package mars.venus;

import mars.Application;
import mars.assembler.log.LogMessage;
import mars.simulator.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/*
Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject
to the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Special text area used by {@link MessagesPane} to imitate console input/output. Output written using
 * {@link #writeOutput(String)} is buffered to reduce the load on the GUI and prevent rapid-fire writes from clogging
 * the event queue, which would be the case if {@link #append(String)} was used instead. User input is not
 * entirely accurate to the behavior of a real console, as it prevents the user from entering any input while no
 * system calls are requesting it, and limits the user from exceeding character limits while typing. These differences
 * should be beneficial in the majority of cases, though.
 * <p>
 * The user input code was originally written by Ricardo Fernández Pascual (rfernandez@ditec.um.es) in December 2009.
 */
public class ConsoleTextArea extends JTextArea {
    // These constants are designed to keep the contents of the console from becoming overwhelmingly large
    // (which slows things down over time and can hog and/or exhaust memory)
    public static final int MAXIMUM_LINE_COUNT = 1000;
    public static final int TRIM_LINES_EXTRA_COUNT = MAXIMUM_LINE_COUNT / 20; // trim 5% extra
    public static final int MAXIMUM_CHARACTER_COUNT = MAXIMUM_LINE_COUNT * 500; // Average 500 characters per line
    public static final int TRIM_CHARACTERS_EXTRA_COUNT = MAXIMUM_CHARACTER_COUNT / 10; // trim 10% extra

    private final StringBuffer outputBuffer;
    private int outputBufferLineCount;
    private boolean outputBufferTrimmed;
    private final List<TextRange<LogMessage>> outputMessages;
    private final ArrayBlockingQueue<String> inputResultQueue;
    private int inputStartPosition;
    private int inputMaxLength;

    /**
     * Create a new <code>ConsoleTextArea</code>.
     */
    public ConsoleTextArea() {
        super();
        this.outputBuffer = new StringBuffer();
        this.outputBufferLineCount = 0;
        this.outputBufferTrimmed = false;
        this.outputMessages = new ArrayList<>();
        this.inputResultQueue = new ArrayBlockingQueue<>(1);
        // These values don't really matter because they will be reinitialized when input begins
        this.inputStartPosition = 0;
        this.inputMaxLength = 0;

        this.setEditable(false);
        this.setBackground(UIManager.getColor("Venus.ConsoleTextArea.background"));
        this.setFont(Application.getSettings().consoleFont.get());

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (ConsoleTextArea.this.outputMessages.isEmpty()) {
                    return;
                }
                int offset = ConsoleTextArea.this.viewToModel2D(event.getPoint());
                if (offset < 0) {
                    return;
                }
                // Perform a linear search in reverse chronological order
                for (int index = ConsoleTextArea.this.outputMessages.size() - 1; index >= 0; index--) {
                    TextRange<LogMessage> textRange = ConsoleTextArea.this.outputMessages.get(index);
                    if (textRange.start <= offset) {
                        if (offset - textRange.start < textRange.length) {
                            // User clicked within the text range
                            ConsoleTextArea.this.clickMessage(textRange.value);
                        }
                        // At least for now, we'll just assume text ranges don't overlap, so every range we check after
                        // this point will end before the clicked offset. Thus, we can stop searching here regardless
                        break;
                    }
                }
            }
        });
    }

    /**
     * Clear all text from the text area, and clear any buffered output.
     * <p>
     * <b>This method must be called from the GUI thread.</b>
     */
    public void clear() {
        synchronized (this.outputBuffer) {
            this.outputBuffer.setLength(0);
            this.outputBufferLineCount = 0;
            this.outputBufferTrimmed = false;
        }
        this.outputMessages.clear();
        this.setText("");
    }

    /**
     * Write output text to the console. Console output is buffered so as to lighten the load on the GUI when
     * calls to this method are made in rapid succession.
     * <p>
     * This method may be called from any thread.
     *
     * @param message String to append to the console output.
     */
    public void writeMessage(LogMessage message) {
        String text = message.toString() + '\n';

        this.writeOutput(text, message);
    }

    /**
     * Write output text to the console. Console output is buffered so as to lighten the load on the GUI when
     * calls to this method are made in rapid succession.
     * <p>
     * This method may be called from any thread.
     *
     * @param text String to append to the console output.
     */
    /*
     * The work of this method is done by "invokeLater" because
     * its JTextArea is maintained by the main event thread
     * but also used, via this method, by the execution thread for
     * "print" syscalls. "invokeLater" schedules the code to be
     * run under the event-processing thread no matter what.
     * DPS 23 Aug 2005
     */
    public void writeOutput(String text) {
        this.writeOutput(text, null);
    }

    private void writeOutput(String text, LogMessage message) {
        // Buffering the output allows one flush to handle several writes, meaning the event queue
        // doesn't fill up with console text area updates and effectively block the GUI thread.
        // (This is what happened previously in case of e.g. infinite print loops.)
        // Sean Clarke 04/2024

        boolean isFirstWriteSinceFlush;
        synchronized (this.outputBuffer) {
            isFirstWriteSinceFlush = this.outputBuffer.isEmpty();

            // Do crude trimming by character count if needed to prevent the buffer from becoming larger than necessary
            int projectedLength = this.outputBuffer.length() + text.length();
            if (projectedLength > MAXIMUM_CHARACTER_COUNT) {
                if (text.length() >= MAXIMUM_CHARACTER_COUNT) {
                    // The new text is so large that it will completely replace the buffer
                    this.outputBuffer.setLength(0);
                    this.outputBufferLineCount = 0;
                    // Trim the new text to the maximum size
                    text = text.substring(text.length() - MAXIMUM_CHARACTER_COUNT);
                }
                else {
                    // The new text does not completely replace the buffer (this case is much more likely)
                    int endOfTrim = projectedLength - MAXIMUM_CHARACTER_COUNT;
                    // Adjust the number of lines in the buffer
                    for (int index = 0; index < endOfTrim; index++) {
                        if (this.outputBuffer.charAt(index) == '\n') {
                            this.outputBufferLineCount--;
                        }
                    }
                    // Trim the buffer
                    this.outputBuffer.delete(0, endOfTrim);

                    if (!this.outputMessages.isEmpty()) {
                        // Adjust the output text range start positions
                        int characterCount = endOfTrim;
                        if (!this.outputBufferTrimmed) {
                            // This is the first time the output buffer has been trimmed since a flush
                            characterCount += this.getDocument().getLength();
                        }
                        this.shiftTextRanges(characterCount);
                    }
                }

                // Flag that the output buffer has been trimmed so that when the buffer flushes, we know the buffer
                // content effectively extends beyond its actual size (and thus fully replaces current content)
                this.outputBufferTrimmed = true;
            }

            // Count the number of additional lines the text added to the output buffer
            for (int index = 0; index < text.length(); index++) {
                if (text.charAt(index) == '\n') {
                    this.outputBufferLineCount++;
                }
            }

            // If this is a message, create a new text range for it
            if (message != null) {
                this.outputMessages.add(new TextRange<>(
                    (this.outputBufferTrimmed)
                        ? this.outputBuffer.length()
                        : this.getDocument().getLength() + this.outputBuffer.length(),
                    text.length(),
                    message
                ));
            }

            // Add the text to the output buffer
            this.outputBuffer.append(text);
        }

        if (isFirstWriteSinceFlush) {
            // The output buffer was empty, meaning this text was the first text written
            // since the last flush completed. Now, another flush is needed, which must happen on the GUI thread.
            SwingUtilities.invokeLater(this::flushOutput);
        }
    }

    /**
     * Flush the output buffer to the GUI text area,
     * trimming the content to have at most {@link #MAXIMUM_LINE_COUNT} lines if necessary.
     * <p>
     * <b>This method must be called from the GUI thread.</b>
     */
    public void flushOutput() {
        synchronized (this.outputBuffer) {
            // Trim output if needed to save memory. If the number of lines exceeds the maximum, trim off the excess
            // old lines, plus some extra lines so we only have to do this occasionally. To account for the potential
            // of absurdly long lines, trim using the same approach for characters if needed. Trimming will limit the
            // amount of history kept, but the maximum counts should be set reasonably high.
            int characterCountToTrim = 0;

            int currentLineCount = (this.outputBufferTrimmed) ? 0 : this.getLineCount();
            int currentLength = (this.outputBufferTrimmed) ? 0 : this.getDocument().getLength();

            // First, trim lines if needed
            int projectedLineCount = currentLineCount + this.outputBufferLineCount;
            if (projectedLineCount > MAXIMUM_LINE_COUNT) {
                try {
                    int lastLineToTrim = projectedLineCount - MAXIMUM_LINE_COUNT + TRIM_LINES_EXTRA_COUNT;
                    if (lastLineToTrim >= currentLineCount) {
                        // The output buffer is so large that none of the current content will be retained,
                        // and not all of the output buffer can be added (happens in case of a lot of output at once)
                        lastLineToTrim -= currentLineCount;
                        for (int index = 0; lastLineToTrim > 0; index++) {
                            if (this.outputBuffer.charAt(index) == '\n') {
                                lastLineToTrim--;
                                if (lastLineToTrim == 0) {
                                    characterCountToTrim = currentLength + index + 1;
                                }
                            }
                        }
                    }
                    else {
                        // At least some (probably most) of the current content will be retained
                        characterCountToTrim = this.getLineEndOffset(lastLineToTrim);
                    }
                }
                catch (BadLocationException exception) {
                    throw new IllegalStateException("bad trim constants", exception);
                }

            }

            // Then, trim characters if trimming lines wasn't enough to reduce output
            int projectedLength = currentLength + this.outputBuffer.length() - characterCountToTrim;
            if (projectedLength > MAXIMUM_CHARACTER_COUNT) {
                characterCountToTrim += projectedLength - MAXIMUM_CHARACTER_COUNT + TRIM_CHARACTERS_EXTRA_COUNT;
            }

            if (characterCountToTrim > 0) {
                // Line and/or character limit has been exceeded, so actual trimming must be done
                if (this.outputBufferTrimmed || characterCountToTrim >= currentLength) {
                    // Rather than trim the buffer, clear the document, and append the new content, we can just replace
                    // the current document text with a substring of the buffer
                    this.setText(this.outputBuffer.substring(characterCountToTrim - currentLength));
                }
                else {
                    // Trim the existing content
                    try {
                        this.getDocument().remove(0, characterCountToTrim);
                    }
                    catch (BadLocationException exception) {
                        throw new IllegalStateException("invalid trim length", exception);
                    }

                    // Append the new content
                    this.append(this.outputBuffer.toString());
                }

                // Adjust the output text range start positions if needed
                this.shiftTextRanges(characterCountToTrim);
            }
            else {
                // No trimming to be done, so just append the new content
                this.append(this.outputBuffer.toString());
            }

            // Flush the output buffer
            this.outputBuffer.setLength(0);
            this.outputBufferLineCount = 0;
            this.outputBufferTrimmed = false;

            // Place the caret after the flushed output
            this.setCaretPosition(this.getDocument().getLength());
        }
    }

    private void shiftTextRanges(int characterCount) {
        Iterator<TextRange<LogMessage>> textRanges = this.outputMessages.iterator();
        while (textRanges.hasNext()) {
            TextRange<LogMessage> textRange = textRanges.next();
            textRange.start -= characterCount;
            if (textRange.start < 0) {
                if (textRange.start + textRange.length <= 0) {
                    // The range is now fully trimmed out
                    textRanges.remove();
                }
                else {
                    // The range extends after the trim, so cut off the trimmed part of it
                    textRange.length += textRange.start;
                    textRange.start = 0;
                }
            }
        }
    }

    public void clickMessage(LogMessage message) {
        Application.getGUI().getMessagesPane().highlightMessageSource(message);
    }

    /**
     * Allow the user to input text into the console, waiting until the input is submitted.
     * <p>
     * <b>This blocks the current thread. Always call this method from the simulator thread!</b>
     *
     * @param maxLength Maximum length allowed for user input, or <code>-1</code> if no maximum length should
     *                  be enforced. If set to a non-negative value, the user will be prevented from exceeding
     *                  the limit while typing.
     * @return The input submitted by the user, not including the newline.
     */
    public String awaitUserInput(int maxLength) throws InterruptedException {
        this.inputMaxLength = maxLength;
        SwingUtilities.invokeLater(this::beginInput);

        try {
            // Block the current thread until input is submitted
            return this.inputResultQueue.take();
        }
        catch (InterruptedException exception) {
            // Delete the partial input, as we don't have a good way to save it
            SwingUtilities.invokeLater(() -> {
                if (this.inputStartPosition <= this.getDocument().getLength()) {
                    this.replaceRange("", this.inputStartPosition, this.getDocument().getLength());
                }
            });
            throw exception;
        }
        finally {
            SwingUtilities.invokeLater(this::endInput);
        }
    }

    /**
     * Update the appearance of this text area, overriding the background color and font according to current settings.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        this.setBackground(UIManager.getColor("Venus.ConsoleTextArea.background"));
        this.setFont(Application.getSettings().consoleFont.get());
    }

    private final DocumentFilter documentFilter = new DocumentFilter() {
        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes) throws BadLocationException {
            // Aliases for brevity
            int startPosition = ConsoleTextArea.this.inputStartPosition;
            int maxLength = ConsoleTextArea.this.inputMaxLength;

            // Prevent any edits before the initial position
            if (offset < startPosition) {
                ConsoleTextArea.this.getToolkit().beep();
                return;
            }

            // If there are any newlines, act like the first one ended the input by stripping it
            // and everything past it off. I don't know if this is the best way to handle characters
            // after a newline, but I can't think of a better way.
            int newlineIndex = text.indexOf('\n');
            if (newlineIndex >= 0) {
                text = text.substring(0, newlineIndex);
            }

            // If the character limit would be exceeded, strip the excess off and beep to let the user know
            if (maxLength >= 0 && bypass.getDocument().getLength() + text.length() > startPosition + maxLength) {
                int trimmedLength = Math.max(0, startPosition + maxLength - bypass.getDocument().getLength());
                text = text.substring(0, trimmedLength);
                ConsoleTextArea.this.getToolkit().beep();
            }

            bypass.insertString(offset, text, attributes);

            // If there was a newline, submit the input
            if (newlineIndex >= 0) {
                ConsoleTextArea.this.submitInput();
            }
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
            // Aliases for brevity
            int startPosition = ConsoleTextArea.this.inputStartPosition;
            int maxLength = ConsoleTextArea.this.inputMaxLength;

            // Prevent any edits before the initial position
            if (offset < startPosition) {
                ConsoleTextArea.this.getToolkit().beep();
                return;
            }

            // If there are any newlines, act like the first one ended the input by stripping it
            // and everything past it off. I don't know if this is the best way to handle characters
            // after a newline, but I can't think of a better way.
            int newlineIndex = text.indexOf('\n');
            if (newlineIndex >= 0) {
                text = text.substring(0, newlineIndex);
            }

            // If the character limit would be exceeded, strip the excess off and beep to let the user know
            if (maxLength >= 0 && (bypass.getDocument().getLength() - length) + text.length() > startPosition + maxLength) {
                int trimmedLength = Math.max(0, startPosition + maxLength - (bypass.getDocument().getLength() - length));
                text = text.substring(0, trimmedLength);
                ConsoleTextArea.this.getToolkit().beep();
            }

            bypass.replace(offset, length, text, attributes);

            // If there was a newline, submit the input
            if (newlineIndex >= 0) {
                ConsoleTextArea.this.submitInput();
            }
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
            // Prevent any edits before the initial position
            if (offset < ConsoleTextArea.this.inputStartPosition) {
                ConsoleTextArea.this.getToolkit().beep();
                return;
            }

            bypass.remove(offset, length);
        }
    };

    private final NavigationFilter navigationFilter = new NavigationFilter() {
        @Override
        public void moveDot(FilterBypass bypass, int dot, Position.Bias bias) {
            // Prevent placement of the caret before the initial position
            if (dot < ConsoleTextArea.this.inputStartPosition) {
                dot = Math.min(ConsoleTextArea.this.inputStartPosition, ConsoleTextArea.this.getDocument().getLength());
            }
            bypass.moveDot(dot, bias);
        }

        @Override
        public void setDot(FilterBypass bypass, int dot, Position.Bias bias) {
            // Prevent placement of the caret before the initial position
            if (dot < ConsoleTextArea.this.inputStartPosition) {
                dot = Math.min(ConsoleTextArea.this.inputStartPosition, ConsoleTextArea.this.getDocument().getLength());
            }
            bypass.setDot(dot, bias);
        }
    };

    private final SimulatorListener simulatorListener = new SimulatorListener() {
        /**
         * Called when execution is resumed after pausing during console input.
         *
         * @param event The event which occurred.
         */
        @Override
        public void simulatorStarted(SimulatorStartEvent event) {
            ConsoleTextArea.this.setEditable(true);
        }

        /**
         * Called when execution is paused during console input.
         *
         * @param event The event which occurred.
         */
        @Override
        public void simulatorPaused(SimulatorPauseEvent event) {
            ConsoleTextArea.this.setEditable(false);
        }

        /**
         * Called when execution is terminated during console input.
         *
         * @param event The event which occurred.
         */
        @Override
        public void simulatorFinished(SimulatorFinishEvent event) {
            ConsoleTextArea.this.submitInput();
        }
    };

    private void beginInput() {
        this.setEditable(true);
        this.requestFocusInWindow();
        this.setCaretPosition(this.getDocument().getLength());
        this.inputStartPosition = this.getCaretPosition();
        this.setNavigationFilter(this.navigationFilter);
        ((AbstractDocument) this.getDocument()).setDocumentFilter(this.documentFilter);
        Simulator.getInstance().addGUIListener(this.simulatorListener);
    }

    private void submitInput() {
        try {
            int position = Math.min(this.inputStartPosition, this.getDocument().getLength());
            int length = Math.min(this.getDocument().getLength() - position, this.inputMaxLength >= 0
                ? this.inputMaxLength
                : Integer.MAX_VALUE);
            this.inputResultQueue.offer(this.getText(position, length));
        }
        catch (BadLocationException exception) {
            // This should not happen, but if it somehow does, default to an empty string
            System.err.println(this.getClass().getSimpleName() + ": failed to retrieve user input from text area:");
            exception.printStackTrace(System.err);
            this.inputResultQueue.offer("");
        }
        // Append a newline to account for the newline stripped before submission
        SwingUtilities.invokeLater(() -> this.append("\n"));
    }

    private void endInput() {
        this.setEditable(false);
        this.setNavigationFilter(null);
        ((AbstractDocument) this.getDocument()).setDocumentFilter(null);
        this.setCaretPosition(this.getDocument().getLength());
        Simulator.getInstance().removeGUIListener(this.simulatorListener);
    }

    private static class TextRange<T> {
        public int start;
        public int length;
        public T value;

        public TextRange(int start, int length, T value) {
            this.start = start;
            this.length = length;
            this.value = value;
        }
    }
}
