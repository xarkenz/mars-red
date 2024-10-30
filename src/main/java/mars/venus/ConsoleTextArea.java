package mars.venus;

import mars.Application;
import mars.simulator.*;

import javax.swing.*;
import javax.swing.text.*;
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
    // These constants are designed to keep scrolled contents of the
    // two message areas from becoming overwhelmingly large (which
    // seems to slow things down as new text is appended).  Once it
    // reaches MAXIMUM_SCROLLED_CHARACTERS in length then cut off
    // the first NUMBER_OF_CHARACTERS_TO_CUT characters.  The latter
    // must obviously be smaller than the former.
    public static final int MAXIMUM_SCROLLED_CHARACTERS = Application.MAXIMUM_MESSAGE_CHARACTERS;
    public static final int NUMBER_OF_CHARACTERS_TO_CUT = Application.MAXIMUM_MESSAGE_CHARACTERS / 10; // 10%
    public static final int MAXIMUM_LINE_COUNT = 1000;
    public static final int EXTRA_TRIM_LINE_COUNT = 20;

    private final StringBuffer outputBuffer;
    private final ArrayBlockingQueue<String> inputResultQueue;
    private int inputStartPosition;
    private int inputMaxLength;

    /**
     * Create a new <code>ConsoleTextArea</code>.
     */
    public ConsoleTextArea() {
        super();
        this.outputBuffer = new StringBuffer();
        this.inputResultQueue = new ArrayBlockingQueue<>(1);
        // These values don't really matter because they will be reinitialized when input begins
        this.inputStartPosition = 0;
        this.inputMaxLength = 0;

        this.setEditable(false);
        this.setBackground(UIManager.getColor("Venus.ConsoleTextArea.background"));
        this.setFont(Application.getSettings().consoleFont.get());
    }

    /**
     * Clear all text from the text area, and clear any buffered output.
     */
    public void clear() {
        this.outputBuffer.setLength(0);
        this.setText("");
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
        // Buffering the output allows one flush to handle several writes, meaning the event queue
        // doesn't fill up with console text area updates and effectively block the GUI thread.
        // (This is what happened previously in case of e.g. infinite print loops.)
        // Sean Clarke 04/2024

        boolean isFirstWriteSinceFlush;
        synchronized (this.outputBuffer) {
            isFirstWriteSinceFlush = this.outputBuffer.isEmpty();
            // Add the text to the console output buffer
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
     * then trim the content to have at most {@link #MAXIMUM_LINE_COUNT} lines if necessary.
     * <p>
     * <b>This method must be called from the GUI thread.</b>
     */
    public void flushOutput() {
        synchronized (this.outputBuffer) {
            // Flush the output buffer
            this.append(this.outputBuffer.toString());
            this.outputBuffer.setLength(0);
        }

        // Do some crude trimming to save memory.  If the number of lines exceeds the maximum,
        // trim off the excess old lines, plus some extra lines so we only have to do this occasionally.
        // This will limit scrolling, but the maximum line count can be set reasonably high.
        // FIXME: doing the trimming AFTER the buffer is flushed to the GUI is a significant oversight
        int lineCount = this.getLineCount();
        if (lineCount > MAXIMUM_LINE_COUNT) {
            try {
                int lastLineToTrim = lineCount - MAXIMUM_LINE_COUNT + EXTRA_TRIM_LINE_COUNT;
                this.getDocument().remove(0, this.getLineEndOffset(lastLineToTrim));
            }
            catch (BadLocationException exception) {
                // Only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
                // (which shouldn't happen unless the constants are changed!)
            }
        }

        this.setCaretPosition(this.getDocument().getLength());
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
}
