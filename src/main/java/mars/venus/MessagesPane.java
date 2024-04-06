package mars.venus;

import mars.ErrorList;
import mars.Application;
import mars.ProcessingException;
import mars.simulator.Simulator;
import mars.simulator.SimulatorListener;
import mars.simulator.SystemIO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position.Bias;
import javax.swing.undo.UndoableEdit;
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
public class MessagesPane extends JTabbedPane {
    // These constants are designed to keep scrolled contents of the
    // two message areas from becoming overwhelmingly large (which
    // seems to slow things down as new text is appended).  Once it
    // reaches MAXIMUM_SCROLLED_CHARACTERS in length then cut off
    // the first NUMBER_OF_CHARACTERS_TO_CUT characters.  The latter
    // must obviously be smaller than the former.
    public static final int MAXIMUM_SCROLLED_CHARACTERS = Application.MAXIMUM_MESSAGE_CHARACTERS;
    public static final int NUMBER_OF_CHARACTERS_TO_CUT = Application.MAXIMUM_MESSAGE_CHARACTERS / 10; // 10%

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
            public void mouseClicked(MouseEvent e) {
                String text;
                int lineStart = 0;
                int lineEnd = 0;
                try {
                    int line = messagesTextArea.getLineOfOffset(messagesTextArea.viewToModel2D(e.getPoint()));
                    lineStart = messagesTextArea.getLineStartOffset(line);
                    lineEnd = messagesTextArea.getLineEndOffset(line);
                    text = messagesTextArea.getText(lineStart, lineEnd - lineStart);
                }
                catch (BadLocationException ble) {
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

        JButton consoleTabClearButton = new JButton("Clear");
        consoleTabClearButton.setToolTipText("Clear the Console area.");
        consoleTabClearButton.addActionListener(event -> consoleTextArea.setText(""));
        consoleTab = new JPanel(new BorderLayout());
        consoleTab.setBorder(new EmptyBorder(6, 6, 6, 6));
        consoleTab.add(createBoxForButton(consoleTabClearButton), BorderLayout.WEST);
        consoleTab.add(new JScrollPane(consoleTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        this.addTab("Messages", null, messagesTab, "Information, warnings and errors. Click on an error message to jump to the error source.");
        this.addTab("Console", null, consoleTab, "Simulated MIPS console input and output.");
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
            catch (BadLocationException ble) {
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
        EditTabbedPane editTabbedPane = Application.getGUI().getMainPane().getEditTabbedPane();
        EditPane editPane, currentPane = null;
        editPane = editTabbedPane.getEditPaneForFile(new File(fileName).getPath());
        if (editPane != null) {
            if (editPane != editTabbedPane.getCurrentEditTab()) {
                editTabbedPane.setCurrentEditTab(editPane);
            }
            currentPane = editPane;
        }
        else {
            // File is not open.  Try to open it.
            if (editTabbedPane.openFile(new File(fileName))) {
                currentPane = editTabbedPane.getCurrentEditTab();
            }
        }
        // If editPane == null, it means the desired file was not open.  Line selection
        // does not properly with the JEditTextArea editor in this situation (it works
        // fine for the original generic editor).  So we just won't do it. DPS 9-Aug-2010
        if (editPane != null) {
            currentPane.selectLine(line, column);
        }
    }

    /**
     * Returns component used to display assembler messages
     *
     * @return assembler message text component
     */
    public JTextArea getMessagesTextArea() {
        return messagesTextArea;
    }

    /**
     * Returns component used to display runtime messages
     *
     * @return runtime message text component
     */
    public JTextArea getConsoleTextArea() {
        return consoleTextArea;
    }

    /**
     * Post a message to the assembler display
     *
     * @param message String to append to assembler display text
     */
    public void writeToMessages(final String message) {
        SwingUtilities.invokeLater(() -> {
            setSelectedComponent(messagesTab);
            messagesTextArea.append(message);
            // Can do some crude cutting here.  If the document gets "very large",
            // let's cut off the oldest text. This will limit scrolling but the limit
            // can be set reasonably high.
            if (messagesTextArea.getDocument().getLength() > MAXIMUM_SCROLLED_CHARACTERS) {
                try {
                    messagesTextArea.getDocument().remove(0, NUMBER_OF_CHARACTERS_TO_CUT);
                }
                catch (BadLocationException e) {
                    // Only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
                    // (which shouldn't happen unless the constants are changed!)
                }
            }
        });
    }

    /**
     * Post a message to the runtime display
     *
     * @param message String to append to runtime display text
     */
    // The work of this method is done by "invokeLater" because
    // its JTextArea is maintained by the main event thread
    // but also used, via this method, by the execution thread for
    // "print" syscalls. "invokeLater" schedules the code to be
    // run under the event-processing thread no matter what.
    // DPS, 23 Aug 2005.
    public void writeToConsole(final String message) {
        SwingUtilities.invokeLater(() -> {
            setSelectedComponent(consoleTab);
            consoleTextArea.append(message);
            // Can do some crude cutting here.  If the document gets "very large",
            // let's cut off the oldest text. This will limit scrolling but the limit
            // can be set reasonably high.
            if (consoleTextArea.getDocument().getLength() > MAXIMUM_SCROLLED_CHARACTERS) {
                try {
                    consoleTextArea.getDocument().remove(0, NUMBER_OF_CHARACTERS_TO_CUT);
                }
                catch (BadLocationException ble) {
                    // Only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
                    // (which shouldn't happen unless the constants are changed!)
                }
            }
        });
    }

    /**
     * Make the assembler message tab current (up front)
     */
    public void selectMessagesTab() {
        setSelectedComponent(messagesTab);
    }

    /**
     * Make the runtime message tab current (up front)
     */
    public void selectConsoleTab() {
        setSelectedComponent(consoleTab);
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
        Asker asker = new Asker(maxLength); // Asker defined immediately below.
        return asker.response();
    }

    /**
     * Thread class for obtaining user input in the {@link MessagesPane} console.
     * Written by Ricardo Fernández Pascual [rfernandez@ditec.um.es] December 2009.
     */
    private class Asker implements Runnable {
        private final ArrayBlockingQueue<String> resultQueue = new ArrayBlockingQueue<>(1);
        private int initialPos;
        private final int maxLength;

        public Asker(int maxLength) {
            this.maxLength = maxLength;
            // initialPos will be set in run()
        }

        private final DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent event) {
                EventQueue.invokeLater(() -> {
                    try {
                        String inserted = event.getDocument().getText(event.getOffset(), event.getLength());
                        int newlineIndex = inserted.indexOf('\n');
                        if (newlineIndex >= 0) {
                            int offset = event.getOffset() + newlineIndex;
                            if (offset + 1 == event.getDocument().getLength()) {
                                returnResponse();
                            }
                            else {
                                // remove the '\n' and put it at the end
                                event.getDocument().remove(offset, 1);
                                event.getDocument().insertString(event.getDocument().getLength(), "\n", null);
                                // insertUpdate will be called again, since we have inserted the '\n' at the end
                            }
                        }
                        else if (maxLength >= 0 && event.getDocument().getLength() - initialPos >= maxLength) {
                            returnResponse();
                        }
                    }
                    catch (BadLocationException ex) {
                        returnResponse();
                    }
                });
            }

            @Override
            public void removeUpdate(final DocumentEvent event) {
                EventQueue.invokeLater(() -> {
                    if ((event.getDocument().getLength() < initialPos || event.getOffset() < initialPos) && event instanceof UndoableEdit) {
                        ((UndoableEdit) event).undo();
                        consoleTextArea.setCaretPosition(event.getOffset() + event.getLength());
                    }
                });
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
            }
        };

        private final NavigationFilter navigationFilter = new NavigationFilter() {
            @Override
            public void moveDot(FilterBypass bypass, int dot, Bias bias) {
                if (dot < initialPos) {
                    dot = Math.min(initialPos, consoleTextArea.getDocument().getLength());
                }
                bypass.moveDot(dot, bias);
            }

            @Override
            public void setDot(FilterBypass bypass, int dot, Bias bias) {
                if (dot < initialPos) {
                    dot = Math.min(initialPos, consoleTextArea.getDocument().getLength());
                }
                bypass.setDot(dot, bias);
            }
        };

        private final SimulatorListener simulatorListener = new SimulatorListener() {
            @Override
            public void finished(int maxSteps, int programCounter, Simulator.StopReason reason, ProcessingException exception) {
                returnResponse();
            }
        };

        // Must be invoked from the GUI thread.
        @Override
        public void run() {
            setSelectedComponent(consoleTab);
            consoleTextArea.setEditable(true);
            consoleTextArea.requestFocusInWindow();
            consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
            initialPos = consoleTextArea.getCaretPosition();
            consoleTextArea.setNavigationFilter(navigationFilter);
            consoleTextArea.getDocument().addDocumentListener(listener);
            Simulator.getInstance().addListener(simulatorListener);
        }

        // Not required to be called from the GUI thread.
        private void cleanup() {
            EventQueue.invokeLater(() -> {
                consoleTextArea.getDocument().removeDocumentListener(listener);
                consoleTextArea.setEditable(false);
                consoleTextArea.setNavigationFilter(null);
                consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
                Simulator.getInstance().removeListener(simulatorListener);
            });
        }

        private void returnResponse() {
            try {
                int pos = Math.min(initialPos, consoleTextArea.getDocument().getLength());
                int len = Math.min(consoleTextArea.getDocument().getLength() - pos, maxLength >= 0 ? maxLength : Integer.MAX_VALUE);
                resultQueue.offer(consoleTextArea.getText(pos, len));
            }
            catch (BadLocationException ex) {
                // This should not happen
                resultQueue.offer("");
            }
        }

        private String response() {
            EventQueue.invokeLater(this);
            try {
                return resultQueue.take();
            }
            catch (InterruptedException ex) {
                return null;
            }
            finally {
                cleanup();
            }
        }
    }
}
