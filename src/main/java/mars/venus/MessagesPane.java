package mars.venus;

import mars.Application;
import mars.assembler.log.LogMessage;
import mars.assembler.log.SourceLocation;
import mars.simulator.*;
import mars.venus.editor.EditTab;
import mars.venus.editor.FileEditorTab;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

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
    private final ConsoleTextArea messages;
    private final ConsoleTextArea console;
    private final JPanel messagesTab;
    private final JPanel consoleTab;

    /**
     * Constructor for the class, sets up two fresh tabbed text areas for program feedback.
     */
    public MessagesPane() {
        super();
        this.setMinimumSize(new Dimension(0, 0));
        this.messages = new ConsoleTextArea();
        this.console = new ConsoleTextArea();

        JButton messagesTabClearButton = new JButton("Clear");
        messagesTabClearButton.setToolTipText("Clear the Messages area.");
        messagesTabClearButton.addActionListener(event -> this.messages.clear());
        this.messagesTab = new JPanel(new BorderLayout());
        this.messagesTab.setBorder(new EmptyBorder(6, 6, 6, 6));
        this.messagesTab.add(this.createBoxForButton(messagesTabClearButton), BorderLayout.WEST);
        this.messagesTab.add(new JScrollPane(this.messages, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        JButton consoleClearButton = new JButton("Clear");
        consoleClearButton.setToolTipText("Clear the Console area.");
        consoleClearButton.addActionListener(event -> this.console.clear());
        this.consoleTab = new JPanel(new BorderLayout());
        this.consoleTab.setBorder(new EmptyBorder(6, 6, 6, 6));
        this.consoleTab.add(this.createBoxForButton(consoleClearButton), BorderLayout.WEST);
        JScrollPane consoleScrollPane = new JScrollPane(this.console, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.consoleTab.add(consoleScrollPane, BorderLayout.CENTER);
        this.addTab("Messages", null, this.messagesTab, "Information, warnings and errors. Click on an error message to jump to the error source.");
        this.addTab("Console", null, this.consoleTab, "Simulated MIPS console input and output.");

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
     * Will select the specified line in an editor tab.  If the file is open
     * but not current, its tab will be made current.  If the file is not open,
     * it will be opened in a new tab and made current, however the line will
     * not be selected (apparent apparent problem with JEditTextArea).
     *
     * @param message The message whose source should be highlighted.
     */
    // TODO: this should not be here in my opinion
    public void highlightMessageSource(LogMessage message) {
        SourceLocation location = message.getLocation();
        if (location == null || location.getFilename() == null || location.getLineIndex() < 0) {
            return;
        }

        File file = new File(location.getFilename());
        EditTab editTab = Application.getGUI().getMainPane().getEditTab();
        FileEditorTab tab = editTab.getEditorTab(file);
        if (tab != null) {
            if (tab != editTab.getCurrentEditorTab()) {
                editTab.setCurrentEditorTab(tab);
            }
        }
        else {
            // File is not open.  Try to open it.
            if (editTab.openFile(file)) {
                tab = editTab.getCurrentEditorTab();
                Application.getGUI().addRecentFile(file);
            }
        }

        // If tab is null, it means the desired file was not open.  Line selection
        // does not properly with the JEditTextArea editor in this situation (it works
        // fine for the original generic editor).  So we just won't do it. DPS 9-Aug-2010
        if (tab != null) {
            tab.selectLine(location.getLineIndex(), location.getColumnIndex());
        }
    }

    /**
     * Get the text area used to display application messages to the user.
     *
     * @return The console contained in the Messages tab.
     */
    public ConsoleTextArea getMessages() {
        return this.messages;
    }

    /**
     * Get the text area used to display runtime output to the user.
     *
     * @return The console contained in the Console tab.
     */
    public ConsoleTextArea getConsole() {
        return this.console;
    }

    /**
     * Ensure that the Messages tab is visible.
     * <p>
     * This method may be called from any thread.
     */
    public void selectMessagesTab() {
        SwingUtilities.invokeLater(() -> this.setSelectedComponent(this.messagesTab));
    }

    /**
     * Ensure that the Console tab is visible.
     * <p>
     * This method may be called from any thread.
     */
    public void selectConsoleTab() {
        SwingUtilities.invokeLater(() -> this.setSelectedComponent(this.consoleTab));
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
        this.console.writeOutput(input + '\n');
        return input;
    }

    /**
     * Called when the simulator begins execution of a program.
     *
     * @param event The event which occurred.
     */
    @Override
    public void simulatorStarted(SimulatorStartEvent event) {
        this.messages.writeOutput(Simulator.class.getSimpleName() + ": started simulation.\n");
        this.setSelectedComponent(this.consoleTab);
    }

    /**
     * Called when the simulator stops execution of a program due to pausing.
     *
     * @param event The event which occurred.
     */
    @Override
    public void simulatorPaused(SimulatorPauseEvent event) {
        switch (event.getReason()) {
            case BREAKPOINT -> {
                this.messages.writeOutput(Simulator.class.getSimpleName() + ": paused simulation at breakpoint.\n");
            }
            case STEP_LIMIT_REACHED -> {
                this.messages.writeOutput(Simulator.class.getSimpleName() + ": paused simulation after " + event.getStepCount() + " step(s).\n");
            }
            case EXTERNAL -> {
                this.messages.writeOutput(Simulator.class.getSimpleName() + ": paused simulation.\n");
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
        switch (event.getReason()) {
            case EXIT_SYSCALL -> {
                int exitCode = event.getException().getExitCode();
                if (exitCode == 0) {
                    this.messages.writeOutput(Simulator.class.getSimpleName() + ": finished simulation successfully.\n");
                }
                else {
                    this.messages.writeOutput(Simulator.class.getSimpleName() + ": finished simulation with exit code " + exitCode + ".\n");
                }
                this.console.writeOutput("\n--- program finished with exit code " + exitCode + " ---\n\n");
                this.setSelectedComponent(this.consoleTab);
            }
            case RAN_OFF_BOTTOM -> {
                this.messages.writeOutput(Simulator.class.getSimpleName() + ": finished simulation due to null instruction.\n");
                this.console.writeOutput("\n--- program automatically terminated (ran off bottom) ---\n\n");
                this.setSelectedComponent(this.consoleTab);
            }
            case EXCEPTION -> {
                this.messages.writeOutput(Simulator.class.getSimpleName() + ": finished simulation with errors.\n");
                if (event.getException() == null) {
                    this.console.writeOutput("\n--- program terminated due to error(s) ---\n\n");
                }
                else {
                    this.console.writeOutput("\n--- program terminated due to error(s): ---\n");
                    this.console.writeOutput(event.getException().getMessage());
                    this.console.writeOutput("\n--- end of error report ---\n\n");
                }
                this.setSelectedComponent(this.consoleTab);
            }
            case EXTERNAL -> {
                this.messages.writeOutput(Simulator.class.getSimpleName() + ": stopped simulation.\n");
                this.console.writeOutput("\n--- program terminated by user ---\n\n");
            }
            case INTERNAL_ERROR -> {
                this.messages.writeOutput(Simulator.class.getSimpleName() + ": stopped simulation after encountering an internal error.\n");
                this.console.writeOutput("\n--- program terminated due to internal error ---\n\n");
            }
        }
    }
}
