package mars.venus.actions.edit;

import mars.venus.editor.FileEditorTab;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;
import mars.venus.editor.FileStatus;
import mars.venus.editor.MARSTextEditingArea;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Edit -> Find/Replace menu item.
 */
public class EditFindReplaceAction extends VenusAction {
    private static final String DIALOG_TITLE = "Find and Replace";

    private String searchString = "";
    private boolean caseSensitivity = true;

    public EditFindReplaceAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        JDialog dialog = new FindReplaceDialog(this.gui, DIALOG_TITLE, false);
        dialog.setVisible(true);
    }

    @Override
    public void update() {
        this.setEnabled(this.gui.getFileStatus() != FileStatus.NO_FILE);
    }

    /**
     * Private class to do all the work!
     */
    private class FindReplaceDialog extends JDialog {
        public static final String FIND_TOOL_TIP_TEXT = "Find next occurrence of given text; wraps around at end";
        public static final String REPLACE_TOOL_TIP_TEXT = "Replace current occurrence of text then find next";
        public static final String REPLACE_ALL_TOOL_TIP_TEXT = "Replace all occurrences of text";
        public static final String CLOSE_TOOL_TIP_TEXT = "Close the dialog";
        public static final String RESULTS_TOOL_TIP_TEXT = "Outcome of latest operation (button click)";

        public static final String RESULTS_TEXT_FOUND = "Text found";
        public static final String RESULTS_TEXT_NOT_FOUND = "Text not found";
        public static final String RESULTS_TEXT_REPLACED = "Text replaced and found next";
        public static final String RESULTS_TEXT_REPLACED_LAST = "Text replaced; last occurrence";
        public static final String RESULTS_TEXT_REPLACED_ALL = "Replaced ";
        public static final String RESULTS_NO_TEXT_TO_FIND = "No text to find";

        private final VenusUI gui;
        private JTextField findInputField;
        private JTextField replaceInputField;
        private JCheckBox caseSensitiveCheckBox;
        private JLabel resultsLabel;

        public FindReplaceDialog(VenusUI gui, String title, boolean modality) {
            super(gui, title, modality);
            this.gui = gui;

            this.setContentPane(this.buildDialogPanel());

            this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    FindReplaceDialog.this.performClose();
                }
            });

            this.pack();
            this.setLocationRelativeTo(gui);
        }

        // Constructs the dialog's main panel.
        private JPanel buildDialogPanel() {
            JPanel dialogPanel = new JPanel(new BorderLayout());
            dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            dialogPanel.add(this.buildInputPanel(), BorderLayout.NORTH);
            dialogPanel.add(this.buildOptionsPanel());
            dialogPanel.add(this.buildControlPanel(), BorderLayout.SOUTH);
            return dialogPanel;
        }

        // Top part of the dialog, to contain the two input text fields.
        private Component buildInputPanel() {
            this.findInputField = new JTextField(30);
            if (!EditFindReplaceAction.this.searchString.isEmpty()) {
                this.findInputField.setText(EditFindReplaceAction.this.searchString);
                this.findInputField.selectAll();
            }
            this.replaceInputField = new JTextField(30);
            JPanel inputPanel = new JPanel();
            JPanel labelsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            JPanel fieldsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            labelsPanel.add(new JLabel("Find:"));
            labelsPanel.add(new JLabel("Replace with:"));
            fieldsPanel.add(this.findInputField);
            fieldsPanel.add(this.replaceInputField);

            Box columns = Box.createHorizontalBox();
            columns.add(labelsPanel);
            columns.add(Box.createHorizontalStrut(6));
            columns.add(fieldsPanel);
            inputPanel.add(columns);
            return inputPanel;
        }

        // Center part of the dialog, which contains the check box
        // for case sensitivity along with a label to display the
        // outcome of each operation.
        private Component buildOptionsPanel() {
            Box optionsPanel = Box.createHorizontalBox();
            this.caseSensitiveCheckBox = new JCheckBox("Case Sensitive", EditFindReplaceAction.this.caseSensitivity);
            JPanel casePanel = new JPanel(new GridLayout(2, 1));
            casePanel.add(this.caseSensitiveCheckBox);
            casePanel.setMaximumSize(casePanel.getPreferredSize());
            optionsPanel.add(casePanel);
            optionsPanel.add(Box.createHorizontalStrut(5));
            JPanel resultsPanel = new JPanel(new GridLayout(1, 1));
            resultsPanel.setBorder(BorderFactory.createTitledBorder("Result"));
            this.resultsLabel = new JLabel("");
            this.resultsLabel.setForeground(Color.RED);
            this.resultsLabel.setToolTipText(RESULTS_TOOL_TIP_TEXT);
            resultsPanel.add(this.resultsLabel);
            optionsPanel.add(resultsPanel);
            return optionsPanel;
        }

        // Row of control buttons to be placed along the button of the dialog
        private Component buildControlPanel() {
            Box controlPanel = Box.createHorizontalBox();
            controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            JButton findButton = new JButton("Find");
            findButton.setToolTipText(FIND_TOOL_TIP_TEXT);
            findButton.addActionListener(event -> this.performFind());
            JButton replaceButton = new JButton("Replace");
            replaceButton.setToolTipText(REPLACE_TOOL_TIP_TEXT);
            replaceButton.addActionListener(event -> this.performReplace());
            JButton replaceAllButton = new JButton("Replace All");
            replaceAllButton.setToolTipText(REPLACE_ALL_TOOL_TIP_TEXT);
            replaceAllButton.addActionListener(event -> this.performReplaceAll());
            JButton closeButton = new JButton("Close");
            closeButton.setToolTipText(CLOSE_TOOL_TIP_TEXT);
            closeButton.addActionListener(event -> this.performClose());
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(findButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(replaceButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(replaceAllButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(closeButton);
            controlPanel.add(Box.createHorizontalGlue());
            return controlPanel;
        }

        /**
         * Performs a find.  The operation starts at the current cursor position
         * which is not known to this object but is maintained by the EditPane
         * object.  The operation will wrap around when it reaches the end of the
         * document.  If found, the matching text is selected.
         */
        private void performFind() {
            this.resultsLabel.setText("");
            if (!this.findInputField.getText().isEmpty()) {
                FileEditorTab tab = this.gui.getMainPane().getCurrentEditorTab();
                // Being cautious. Should not be null because find/replace tool button disabled if no file open
                if (tab != null) {
                    EditFindReplaceAction.this.searchString = this.findInputField.getText();
                    int result = tab.doFindText(EditFindReplaceAction.this.searchString, this.caseSensitiveCheckBox.isSelected());
                    if (result == MARSTextEditingArea.TEXT_NOT_FOUND) {
                        this.resultsLabel.setText(RESULTS_TEXT_NOT_FOUND);
                    }
                    else {
                        this.resultsLabel.setText(RESULTS_TEXT_FOUND);
                    }
                }
            }
            else {
                this.resultsLabel.setText(RESULTS_NO_TEXT_TO_FIND);
            }
        }

        /**
         * Performs a replace-and-find.  If the matched text is current selected with cursor at
         * its end, the replace happens immediately followed by a find for the next occurrence.
         * Otherwise, it performs a find.  This will select the matching text so the next press
         * of Replace will do the replace.  This is apparently common behavior for replace
         * buttons of different apps I've checked.
         */
        private void performReplace() {
            this.resultsLabel.setText("");
            if (!this.findInputField.getText().isEmpty()) {
                FileEditorTab tab = this.gui.getMainPane().getCurrentEditorTab();
                // Being cautious. Should not be null b/c find/replace tool button disabled if no file open
                if (tab != null) {
                    EditFindReplaceAction.this.searchString = this.findInputField.getText();
                    int result = tab.doReplace(EditFindReplaceAction.this.searchString, this.replaceInputField.getText(), this.caseSensitiveCheckBox.isSelected());
                    String resultText = switch (result) {
                        case MARSTextEditingArea.TEXT_NOT_FOUND -> RESULTS_TEXT_NOT_FOUND;
                        case MARSTextEditingArea.TEXT_FOUND -> RESULTS_TEXT_FOUND;
                        case MARSTextEditingArea.TEXT_REPLACED_NOT_FOUND_NEXT -> RESULTS_TEXT_REPLACED_LAST;
                        case MARSTextEditingArea.TEXT_REPLACED_FOUND_NEXT -> RESULTS_TEXT_REPLACED;
                        default -> RESULTS_NO_TEXT_TO_FIND;
                    };
                    this.resultsLabel.setText(resultText);
                }
            }
            else {
                this.resultsLabel.setText(RESULTS_NO_TEXT_TO_FIND);
            }
        }

        /**
         * Performs a replace-all.  Makes one pass through the document starting at position 0.
         */
        private void performReplaceAll() {
            this.resultsLabel.setText("");
            if (!this.findInputField.getText().isEmpty()) {
                FileEditorTab tab = this.gui.getMainPane().getCurrentEditorTab();
                // Being cautious. Should not be null b/c find/replace tool button disabled if no file open
                if (tab != null) {
                    EditFindReplaceAction.this.searchString = this.findInputField.getText();
                    int replaceCount = tab.doReplaceAll(EditFindReplaceAction.this.searchString, this.replaceInputField.getText(), this.caseSensitiveCheckBox.isSelected());
                    if (replaceCount == 0) {
                        this.resultsLabel.setText(RESULTS_TEXT_NOT_FOUND);
                    }
                    else {
                        this.resultsLabel.setText(RESULTS_TEXT_REPLACED_ALL + replaceCount + " occurrence" + (replaceCount == 1 ? "" : "s"));
                    }
                }
            }
            else {
                this.resultsLabel.setText(RESULTS_NO_TEXT_TO_FIND);
            }
        }

        /**
         * Performs the close operation.  Records the current state of the case-sensitivity
         * checkbox into a variable on the action itself so it will be remembered across invocations within
         * the session.  This also happens with the contents of the "find" text field.
         */
        private void performClose() {
            EditFindReplaceAction.this.caseSensitivity = this.caseSensitiveCheckBox.isSelected();
            this.setVisible(false);
            this.dispose();
        }
    }
}