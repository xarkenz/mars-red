package mars.venus;

import mars.ErrorList;
import mars.Application;
import mars.simulator.*;
import mars.venus.editor.EditTab;
import mars.venus.editor.FileEditorTab;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
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
 * Creates the message window at the bottom of the UI.
 *
 * @author Team JSpim
 */
public class MessagesPane extends JTabbedPane implements SimulatorListener {
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

    private final StringBuffer consoleOutputBuffer;

    private final JTextArea messagesTextArea;
    private final JTextArea consoleTextArea;
    private final JPanel messagesTab;
    private final JPanel consoleTab;

    /**
     * Constructor for the class, sets up two fresh tabbed text areas for program feedback.
     */
    public MessagesPane() {
        super();
        this.setMinimumSize(new Dimension(0, 0));

        consoleOutputBuffer = new StringBuffer();

        messagesTextArea = new JTextArea();
        messagesTextArea.setEditable(false);
        messagesTextArea.setBackground(UIManager.getColor("Venus.MessagesPane.background"));
        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setBackground(UIManager.getColor("Venus.MessagesPane.background"));
        // Set both text areas to mono font.  For assemble
        // pane, will make messages more readable.  For run
        // pane, will allow properly aligned "text graphics"
        // DPS 15 Dec 2008
        Font monoFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        messagesTextArea.setFont(monoFont);
        consoleTextArea.setFont(monoFont);

        JButton messagesTabClearButton = new JButton("Clear");
        messagesTabClearButton.setToolTipText("Clear the Messages area.");
        messagesTabClearButton.addActionListener(event -> messagesTextArea.setText(""));
        messagesTab = new JPanel(new BorderLayout());
        messagesTab.setBorder(new EmptyBorder(6, 6, 6, 6));
        messagesTab.add(createBoxForButton(messagesTabClearButton), BorderLayout.WEST);
        messagesTab.add(new JScrollPane(messagesTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        messagesTextArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                String text;
                int lineStart = 0;
                int lineEnd = 0;
                try {
                    int line = messagesTextArea.getLineOfOffset(messagesTextArea.viewToModel2D(event.getPoint()));
                    lineStart = messagesTextArea.getLineStartOffset(line);
                    lineEnd = messagesTextArea.getLineEndOffset(line);
                    text = messagesTextArea.getText(lineStart, lineEnd - lineStart);
                }
                catch (BadLocationException exception) {
                    text = "";
                }
                if (!text.isBlank()) {
                    // If error or warning, parse out the line and column number.
                    if (text.startsWith(ErrorList.ERROR_MESSAGE_PREFIX) || text.startsWith(ErrorList.WARNING_MESSAGE_PREFIX)) {
                        messagesTextArea.select(lineStart, lineEnd);
                        messagesTextArea.setSelectionColor(Color.YELLOW);
                        messagesTextArea.repaint();
                        int separatorPosition = text.indexOf(ErrorList.MESSAGE_SEPARATOR);
                        if (separatorPosition >= 0) {
                            text = text.substring(0, separatorPosition);
                        }
                        String[] stringTokens = text.split("\\s"); // tokenize with whitespace delimiter
                        String lineToken = ErrorList.LINE_PREFIX.trim();
                        String columnToken = ErrorList.POSITION_PREFIX.trim();
                        String lineString = "";
                        String columnString = "";
                        for (int i = 0; i < stringTokens.length; i++) {
                            if (stringTokens[i].equals(lineToken) && i < stringTokens.length - 1) {
                                lineString = stringTokens[i + 1];
                            }
                            if (stringTokens[i].equals(columnToken) && i < stringTokens.length - 1) {
                                columnString = stringTokens[i + 1];
                            }
                        }
                        int line;
                        int column;
                        try {
                            line = Integer.parseInt(lineString);
                        }
                        catch (NumberFormatException nfe) {
                            line = 0;
                        }
                        try {
                            column = Integer.parseInt(columnString);
                        }
                        catch (NumberFormatException nfe) {
                            column = 0;
                        }
                        // everything between FILENAME_PREFIX and LINE_PREFIX is filename.
                        int fileNameStart = text.indexOf(ErrorList.FILENAME_PREFIX) + ErrorList.FILENAME_PREFIX.length();
                        int fileNameEnd = text.indexOf(ErrorList.LINE_PREFIX);
                        String fileName = "";
                        if (fileNameStart < fileNameEnd && fileNameStart >= ErrorList.FILENAME_PREFIX.length()) {
                            fileName = text.substring(fileNameStart, fileNameEnd).trim();
                        }
                        if (!fileName.isEmpty()) {
                            selectEditorTextLine(fileName, line, column);
                            selectErrorMessage(fileName, line, column);
                        }
                    }
                }
            }
        });

        JButton consoleClearButton = new JButton("Clear");
        consoleClearButton.setToolTipText("Clear the Console area.");
        consoleClearButton.addActionListener(event -> {
            consoleTextArea.setText("");
        });
        consoleTab = new JPanel(new BorderLayout());
        consoleTab.setBorder(new EmptyBorder(6, 6, 6, 6));
        consoleTab.add(createBoxForButton(consoleClearButton), BorderLayout.WEST);
        JScrollPane consoleScrollPane = new JScrollPane(consoleTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        consoleScrollPane.getVerticalScrollBar().addAdjustmentListener(event -> {
//            System.out.println(event.getValueIsAdjusting() + " " + event.getAdjustable().getMaximum() + " " + event.getValue());
//        });
        consoleTab.add(consoleScrollPane, BorderLayout.CENTER);
        this.addTab("Messages", null, messagesTab, "Information, warnings and errors. Click on an error message to jump to the error source.");
        this.addTab("Console", null, consoleTab, "Simulated MIPS console input and output.");

        Simulator.getInstance().addGUIListener(this);
    }

    /**
     * Center given button in a box, centered vertically and 6 pixels on left and right.
     */
    private Box createBoxForButton(JButton button) {
        Box buttonRow = Box.createHorizontalBox();
        buttonRow.add(Box.createHorizontalStrut(6));
        buttonRow.add(button);
        buttonRow.add(Box.createHorizontalStrut(6));
        Box buttonBox = Box.createVerticalBox();
        buttonBox.add(Box.createVerticalGlue());
        buttonBox.add(buttonRow);
        buttonBox.add(Box.createVerticalGlue());
        return buttonBox;
    }

    /**
     * Will select the Mars Messages tab error message that matches the given
     * specifications, if it is found. Matching is done by constructing
     * a string using the parameter values and searching the text area for the last
     * occurrence of that string.
     *
     * @param filename A String containing the file path name.
     * @param line     Line number for error message
     * @param column   Column number for error message
     */
    public void selectErrorMessage(String filename, int line, int column) {
        String errorReportSubstring = new File(filename).getName() + ErrorList.LINE_PREFIX + line + ErrorList.POSITION_PREFIX + column;
        int textPosition = messagesTextArea.getText().lastIndexOf(errorReportSubstring);
        if (textPosition >= 0) {
            int textLine;
            int lineStart;
            int lineEnd;
            try {
                textLine = messagesTextArea.getLineOfOffset(textPosition);
                lineStart = messagesTextArea.getLineStartOffset(textLine);
                lineEnd = messagesTextArea.getLineEndOffset(textLine);
                messagesTextArea.setSelectionColor(Color.YELLOW);
                messagesTextArea.select(lineStart, lineEnd);
                messagesTextArea.getCaret().setSelectionVisible(true);
                messagesTextArea.repaint();
            }
            catch (BadLocationException exception) {
                // If there is a problem, simply skip the selection
            }
        }
    }

    /**
     * Will select the specified line in an editor tab.  If the file is open
     * but not current, its tab will be made current.  If the file is not open,
     * it will be opened in a new tab and made current, however the line will
     * not be selected (apparent apparent problem with JEditTextArea).
     *
     * @param fileName A String containing the file path name.
     * @param line     Line number for error message
     * @param column   Column number for error message
     */
    public void selectEditorTextLine(String fileName, int line, int column) {
        EditTab editTab = Application.getGUI().getMainPane().getEditTab();
        FileEditorTab fileEditorTab, currentPane = null;
        fileEditorTab = editTab.getEditorTab(new File(fileName));
        if (fileEditorTab != null) {
            if (fileEditorTab != editTab.getCurrentEditorTab()) {
                editTab.setCurrentEditorTab(fileEditorTab);
            }
            currentPane = fileEditorTab;
        }
        else {
            // File is not open.  Try to open it.
            if (editTab.openFile(new File(fileName))) {
                currentPane = editTab.getCurrentEditorTab();
            }
        }
        // If editPane == null, it means the desired file was not open.  Line selection
        // does not properly with the JEditTextArea editor in this situation (it works
        // fine for the original generic editor).  So we just won't do it. DPS 9-Aug-2010
        if (fileEditorTab != null) {
            currentPane.selectLine(line, column);
        }
    }

    /**
     * Post a message to the assembler display
     *
     * @param message String to append to assembler display text
     */
    public void writeToMessages(final String message) {
        SwingUtilities.invokeLater(() -> {
            messagesTextArea.append(message);
            // Can do some crude cutting here.  If the document gets "very large",
            // let's cut off the oldest text. This will limit scrolling but the limit
            // can be set reasonably high.
            if (messagesTextArea.getDocument().getLength() > MAXIMUM_SCROLLED_CHARACTERS) {
                try {
                    messagesTextArea.getDocument().remove(0, NUMBER_OF_CHARACTERS_TO_CUT);
                }
                catch (BadLocationException exception) {
                    // Only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
                    // (which shouldn't happen unless the constants are changed!)
                }
            }
        });
    }

    /**
     * Post a message to the runtime console.
     *
     * @param text String to append to runtime console text.
     */
    /*
     * The work of this method is done by "invokeLater" because
     * its JTextArea is maintained by the main event thread
     * but also used, via this method, by the execution thread for
     * "print" syscalls. "invokeLater" schedules the code to be
     * run under the event-processing thread no matter what.
     * DPS 23 Aug 2005
     */
    public void writeToConsole(String text) {
        // Buffering the output allows one flush to handle several writes, meaning the event queue
        // doesn't fill up with console text area updates and effectively block the GUI thread.
        // (This is what happened previously in case of e.g. infinite print loops.)
        // Sean Clarke 04/2024

        boolean isFirstWriteSinceFlush;
        synchronized (consoleOutputBuffer) {
            isFirstWriteSinceFlush = consoleOutputBuffer.isEmpty();
            // Add the text to the console output buffer
            consoleOutputBuffer.append(text);
        }

        if (isFirstWriteSinceFlush) {
            // The console output buffer was empty, meaning this text was the first text written
            // since the last flush. Now, another flush is needed, which must happen on the GUI thread.
            SwingUtilities.invokeLater(this::flushConsole);
        }
    }

    /**
     * Flush the console output buffer to the Console tab's text area,
     * then trim the content to have at most {@link #MAXIMUM_LINE_COUNT} lines if necessary.
     * <p>
     * <b>This method must be called from the GUI thread.</b>
     */
    public void flushConsole() {
        synchronized (consoleOutputBuffer) {
            // Flush the output buffer
            consoleTextArea.append(consoleOutputBuffer.toString());
            consoleOutputBuffer.delete(0, consoleOutputBuffer.length());
        }

        // Do some crude trimming to save memory.  If the number of lines exceeds the maximum,
        // trim off the excess old lines, plus some extra lines so we only have to do this occasionally.
        // This will limit scrolling, but the maximum line count can be set reasonably high.
        int lineCount = consoleTextArea.getLineCount();
        if (lineCount > MAXIMUM_LINE_COUNT) {
            try {
                int lastLineToTrim = lineCount - MAXIMUM_LINE_COUNT + EXTRA_TRIM_LINE_COUNT;
                consoleTextArea.getDocument().remove(0, consoleTextArea.getLineEndOffset(lastLineToTrim));
            }
            catch (BadLocationException exception) {
                // Only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
                // (which shouldn't happen unless the constants are changed!)
            }
        }

        consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
    }

    /**
     * Ensure that the Messages tab is visible.
     * <p>
     * This method may be called from any thread.
     */
    public void selectMessagesTab() {
        SwingUtilities.invokeLater(() -> setSelectedComponent(messagesTab));
    }

    /**
     * Ensure that the Console tab is visible.
     * <p>
     * This method may be called from any thread.
     */
    public void selectConsoleTab() {
        SwingUtilities.invokeLater(() -> setSelectedComponent(consoleTab));
    }

    /**
     * Method used by the {@link SystemIO} class to get interactive user input
     * requested by a running MIPS program (e.g. syscall #5 to read an
     * integer).  {@link SystemIO} knows whether simulator is being run at
     * command line by the user, or by the GUI. If run at command line,
     * it gets input from {@link System#in} rather than here.
     * <p>
     * This is an overloaded method.  This version, with the String parameter,
     * is used to get input from a popup dialog.
     *
     * @param prompt Prompt to display to the user.
     * @return User input, as a String.
     */
    // FIXME: This does not mix well with pausing/stopping! Figure out how to pause this process
    public String getInputString(String prompt) {
        JOptionPane pane = new JOptionPane(prompt, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION);
        pane.setWantsInput(true);
        JDialog dialog = pane.createDialog(Application.getGUI(), "Keyboard Input");
        dialog.setVisible(true);
        String input = (String) pane.getInputValue();
        this.writeToConsole(Application.USER_INPUT_PREFIX + input + '\n');
        return input;
    }

    /**
     * Method used by the {@link SystemIO} class to get interactive user input
     * requested by a running MIPS program (e.g. syscall #5 to read an
     * integer).  {@link SystemIO} knows whether simulator is being run at
     * command line by the user, or by the GUI. If run at command line,
     * it gets input from {@link System#in} rather than here.
     * <p>
     * This is an overloaded method.  This version, with the int parameter,
     * is used to get input from the console.
     *
     * @param maxLength Maximum length of input. This method returns when maxLength characters have been read.
     *                  Use -1 for no length restrictions.
     * @return User input, as a String.
     */
    public String getInputString(int maxLength) {
        ConsoleInputContext context = new ConsoleInputContext(consoleTextArea, maxLength);
        return context.awaitUserInput();
    }

    /**
     * Called when the simulator begins execution of a program.
     *
     * @param event The event which occurred.
     */
    @Override
    public void simulatorStarted(SimulatorStartEvent event) {
        this.writeToMessages(Simulator.class.getSimpleName() + ": started simulation.\n");
        this.setSelectedComponent(consoleTab);
    }

    /**
     * Called when the simulator stops execution of a program due to pausing.
     *
     * @param event The event which occurred.
     */
    @Override
    public void simulatorPaused(SimulatorPauseEvent event) {
        switch (event.reason()) {
            case BREAKPOINT -> {
                this.writeToMessages(Simulator.class.getSimpleName() + ": paused simulation at breakpoint.\n");
            }
            case STEP_LIMIT_REACHED -> {
                this.writeToMessages(Simulator.class.getSimpleName() + ": paused simulation after " + event.maxSteps() + " step(s).\n");
            }
            case EXTERNAL -> {
                this.writeToMessages(Simulator.class.getSimpleName() + ": paused simulation.\n");
            }
        }
    }

    /**
     * Called when the simulator stops execution of a program due to termination or finishing.
     *
     * @param event The event which occurred.
     */
    @Override
    public void simulatorFinished(SimulatorFinishEvent event) {
        switch (event.reason()) {
            case EXIT_SYSCALL -> {
                int exitCode = event.exception().exitCode();
                if (exitCode == 0) {
                    this.writeToMessages(Simulator.class.getSimpleName() + ": finished simulation successfully.\n");
                }
                else {
                    this.writeToMessages(Simulator.class.getSimpleName() + ": finished simulation with exit code " + exitCode + ".\n");
                }
                this.writeToConsole("\n--- program finished with exit code " + exitCode + " ---\n\n");
                this.setSelectedComponent(consoleTab);
            }
            case RAN_OFF_BOTTOM -> {
                this.writeToMessages(Simulator.class.getSimpleName() + ": finished simulation due to null instruction.\n");
                this.writeToConsole("\n--- program automatically terminated (ran off bottom) ---\n\n");
                this.setSelectedComponent(consoleTab);
            }
            case EXCEPTION -> {
                this.writeToMessages(Simulator.class.getSimpleName() + ": finished simulation with errors.\n");
                if (event.exception() == null) {
                    this.writeToConsole("\n--- program terminated due to error(s) ---\n\n");
                }
                else {
                    this.writeToConsole("\n--- program terminated due to error(s): ---\n");
                    this.writeToConsole(event.exception().errors().generateErrorReport());
                    this.writeToConsole("--- end of error report ---\n\n");
                }
                this.setSelectedComponent(consoleTab);
            }
            case EXTERNAL -> {
                this.writeToMessages(Simulator.class.getSimpleName() + ": stopped simulation.\n");
                this.writeToConsole("\n--- program terminated by user ---\n\n");
            }
            case INTERNAL_ERROR -> {
                this.writeToMessages(Simulator.class.getSimpleName() + ": stopped simulation after encountering an internal error.\n");
                this.writeToConsole("\n--- program terminated due to internal error ---\n\n");
            }
        }
    }

    /**
     * Thread class for obtaining user input in the Console tab of {@link MessagesPane}.
     * Originally written by Ricardo Fernández Pascual [rfernandez@ditec.um.es] December 2009.
     */
    private static class ConsoleInputContext {
        private final JTextArea textArea;
        private final ArrayBlockingQueue<String> resultQueue = new ArrayBlockingQueue<>(1);
        private int initialPosition;
        private final int maxLength;

        public ConsoleInputContext(JTextArea textArea, int maxLength) {
            this.textArea = textArea;
            this.maxLength = maxLength;
            // initialPosition will be set when input begins
        }

        /**
         * Allow the user to input text into the console, waiting until the input is submitted.
         * <p>
         * <b>This blocks the current thread. Always run this from the simulator thread!</b>
         *
         * @return The input submitted by the user, not including the newline.
         */
        public String awaitUserInput() {
            SwingUtilities.invokeLater(this::beginInput);

            try {
                // Block the current thread until input is submitted
                return resultQueue.take();
            }
            catch (InterruptedException exception) {
                return null;
            }
            finally {
                SwingUtilities.invokeLater(this::endInput);
            }
        }

        private final DocumentFilter documentFilter = new DocumentFilter() {
            @Override
            public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes) throws BadLocationException {
                // Prevent any edits before the initial position
                if (offset < initialPosition) {
                    textArea.getToolkit().beep();
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
                if (maxLength >= 0 && bypass.getDocument().getLength() + text.length() > initialPosition + maxLength) {
                    int trimmedLength = Math.max(0, initialPosition + maxLength - bypass.getDocument().getLength());
                    text = text.substring(0, trimmedLength);
                    textArea.getToolkit().beep();
                }

                bypass.insertString(offset, text, attributes);

                // If there was a newline, submit the input
                if (newlineIndex >= 0) {
                    submitInput();
                }
            }

            @Override
            public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
                // Prevent any edits before the initial position
                if (offset < initialPosition) {
                    textArea.getToolkit().beep();
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
                if (maxLength >= 0 && (bypass.getDocument().getLength() - length) + text.length() > initialPosition + maxLength) {
                    int trimmedLength = Math.max(0, initialPosition + maxLength - (bypass.getDocument().getLength() - length));
                    text = text.substring(0, trimmedLength);
                    textArea.getToolkit().beep();
                }

                bypass.replace(offset, length, text, attributes);

                // If there was a newline, submit the input
                if (newlineIndex >= 0) {
                    submitInput();
                }
            }

            @Override
            public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
                // Prevent any edits before the initial position
                if (offset < initialPosition) {
                    textArea.getToolkit().beep();
                    return;
                }

                bypass.remove(offset, length);
            }
        };

        private final NavigationFilter navigationFilter = new NavigationFilter() {
            @Override
            public void moveDot(FilterBypass bypass, int dot, Position.Bias bias) {
                // Prevent placement of the caret before the initial position
                if (dot < initialPosition) {
                    dot = Math.min(initialPosition, textArea.getDocument().getLength());
                }
                bypass.moveDot(dot, bias);
            }

            @Override
            public void setDot(FilterBypass bypass, int dot, Position.Bias bias) {
                // Prevent placement of the caret before the initial position
                if (dot < initialPosition) {
                    dot = Math.min(initialPosition, textArea.getDocument().getLength());
                }
                bypass.setDot(dot, bias);
            }
        };

        private final SimulatorListener simulatorListener = new SimulatorListener() {
            @Override
            public void simulatorStarted(SimulatorStartEvent event) {
                textArea.setEditable(true);
            }

            @Override
            public void simulatorPaused(SimulatorPauseEvent event) {
                textArea.setEditable(false);
            }

            @Override
            public void simulatorFinished(SimulatorFinishEvent event) {
                submitInput();
            }
        };

        private void beginInput() {
            textArea.setEditable(true);
            textArea.requestFocusInWindow();
            textArea.setCaretPosition(textArea.getDocument().getLength());
            initialPosition = textArea.getCaretPosition();
            textArea.setNavigationFilter(navigationFilter);
            ((AbstractDocument) textArea.getDocument()).setDocumentFilter(documentFilter);
            Simulator.getInstance().addGUIListener(simulatorListener);
        }

        private void submitInput() {
            try {
                int position = Math.min(initialPosition, textArea.getDocument().getLength());
                int length = Math.min(textArea.getDocument().getLength() - position, maxLength >= 0 ? maxLength : Integer.MAX_VALUE);
                resultQueue.offer(textArea.getText(position, length));
            }
            catch (BadLocationException exception) {
                // This should not happen, but if it somehow does, default to an empty string
                resultQueue.offer("");
            }
        }

        private void endInput() {
            textArea.setEditable(false);
            textArea.setNavigationFilter(null);
            ((AbstractDocument) textArea.getDocument()).setDocumentFilter(null);
            textArea.append("\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
            Simulator.getInstance().removeGUIListener(simulatorListener);
        }
    }
}
