package mars.venus.actions.settings;

import mars.Application;
import mars.venus.*;
import mars.venus.actions.VenusAction;
import mars.venus.editor.Editor;
import mars.venus.editor.jeditsyntax.SyntaxStyle;
import mars.venus.editor.jeditsyntax.SyntaxUtilities;
import mars.venus.editor.jeditsyntax.tokenmarker.MIPSTokenMarker;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.Caret;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

/*
Copyright (c) 2003-2011,  Pete Sanderson and Kenneth Vollmar

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
 * Action class for the Settings menu item for text editor settings.
 */
public class SettingsEditorAction extends VenusAction {
    /**
     * Create a new SettingsEditorAction.  Has all the GuiAction parameters.
     */
    public SettingsEditorAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    /**
     * When this action is triggered, launch a dialog to view and modify
     * editor settings.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        JDialog editorDialog = new EditorFontDialog(gui, "Text Editor Settings", true, Application.getSettings().getEditorFont());
        editorDialog.setVisible(true);
    }

    private static final String SAMPLE_TOOL_TIP_TEXT = "Current setting; modify using buttons to the right";
    private static final String FOREGROUND_TOOL_TIP_TEXT = "Click to select text color.";
    private static final String BOLD_TOOL_TIP_TEXT = "Toggle text bold style.";
    private static final String ITALIC_TOOL_TIP_TEXT = "Toggle text italic style.";
    private static final String DEFAULT_TOOL_TIP_TEXT = "When enabled, configuration is disabled and the editor defaults are used.";

    private static final String TAB_SIZE_TOOL_TIP_TEXT = "Current tab size in spaces.";
    private static final String BLINK_SPINNER_TOOL_TIP_TEXT = "Current blinking rate in milliseconds.";
    private static final String BLINK_SAMPLE_TOOL_TIP_TEXT = "Displays current blinking rate.";
    private static final String CURRENT_LINE_HIGHLIGHT_TOOL_TIP_TEXT = "When enabled, the current line being edited is highlighted.";
    private static final String AUTO_INDENT_TOOL_TIP_TEXT = "When enabled, the current indentation level is preserved when starting a new line.";
    private static final String[] POPUP_GUIDANCE_TOOL_TIP_TEXT = {
        "Turns off instruction and directive guide popup while typing.",
        "Generates instruction guide popup after first letter of potential instruction is typed.",
        "Generates instruction guide popup after second letter of potential instruction is typed.",
    };

    private static class EditorFontDialog extends AbstractFontSettingDialog {
        private JButton[] foregroundButtons;
        private JLabel[] syntaxSamples;
        private JToggleButton[] useBold;
        private JToggleButton[] useItalic;
        private JCheckBox[] useDefault;

        private int[] syntaxStyleIndices;
        private SyntaxStyle[] defaultStyles, initialStyles, currentStyles;
        private Font previewFont;

        private JSlider tabSizeSelector;
        private JSpinner tabSizeSpinSelector;
        private JSpinner blinkRateSpinSelector;
        private JCheckBox lineHighlightCheck;
        private JCheckBox autoIndentCheck;
        private Caret blinkCaret;
        private JTextField blinkSample;
        private JRadioButton[] popupGuidanceOptions;
        // Flag to indicate whether any syntax style buttons have been clicked
        // since dialog created or most recent "apply".
        private boolean syntaxStylesHaveChanged = false;

        private int initialEditorTabSize;
        private int initialCaretBlinkRate;
        private int initialPopupGuidance;
        private boolean initialLineHighlighting;
        private boolean initialAutoIndent;

        public EditorFontDialog(Frame owner, String title, boolean modality, Font font) {
            super(owner, title, modality, font);
        }

        // Build the main dialog here
        @Override
        protected JPanel buildDialogPanel() {
            JPanel dialog = new JPanel(new BorderLayout(6, 6));
            JPanel fontDialogPanel = super.buildDialogPanel();
            JPanel syntaxStylePanel = buildSyntaxStylePanel();
            JPanel otherSettingsPanel = buildOtherSettingsPanel();
            fontDialogPanel.setBorder(createTitledBorder("Editor Font"));
            syntaxStylePanel.setBorder(createTitledBorder("Syntax Highlighting"));
            otherSettingsPanel.setBorder(createTitledBorder("Other Editor Settings"));
            dialog.add(fontDialogPanel, BorderLayout.WEST);
            dialog.add(syntaxStylePanel, BorderLayout.CENTER);
            dialog.add(otherSettingsPanel, BorderLayout.SOUTH);
            return dialog;
        }

        // Row of control buttons to be placed along the button of the dialog
        @Override
        protected Component buildControlPanel() {
            JButton revertButton = new JButton("Revert");
            revertButton.setToolTipText(SettingsHighlightingAction.REVERT_TOOL_TIP_TEXT);
            revertButton.addActionListener(event -> reset());

            JButton okButton = new JButton("OK");
            okButton.setToolTipText(SettingsHighlightingAction.OK_TOOL_TIP_TEXT);
            okButton.addActionListener(event -> {
                performApply();
                closeDialog();
            });
            defaultButton = okButton;

            JButton cancelButton = new JButton("Cancel");
            cancelButton.setToolTipText(SettingsHighlightingAction.CANCEL_TOOL_TIP_TEXT);
            cancelButton.addActionListener(event -> closeDialog());

            JButton applyButton = new JButton("Apply");
            applyButton.setToolTipText(SettingsHighlightingAction.APPLY_TOOL_TIP_TEXT);
            applyButton.addActionListener(event -> performApply());

            Box controlPanel = Box.createHorizontalBox();
            controlPanel.setBorder(BorderFactory.createEmptyBorder(12, 6, 6, 6));
            controlPanel.add(revertButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(okButton);
            controlPanel.add(Box.createHorizontalStrut(12));
            controlPanel.add(cancelButton);
            controlPanel.add(Box.createHorizontalStrut(12));
            controlPanel.add(applyButton);
            return controlPanel;
        }

        // User has clicked "Apply" or "OK" button.
        @Override
        protected void apply(Font font) {
            Application.getSettings().highlightCurrentEditorLine.set(lineHighlightCheck.isSelected());
            Application.getSettings().autoIndentEnabled.set(autoIndentCheck.isSelected());
            Application.getSettings().caretBlinkRate.set((Integer) blinkRateSpinSelector.getValue());
            Application.getSettings().editorTabSize.set(tabSizeSelector.getValue());

            if (syntaxStylesHaveChanged) {
                for (int row = 0; row < syntaxStyleIndices.length; row++) {
                    Application.getSettings().setEditorSyntaxStyleByPosition(
                        syntaxStyleIndices[row],
                        new SyntaxStyle(syntaxSamples[row].getForeground(), useItalic[row].isSelected(), useBold[row].isSelected())
                    );
                }
                syntaxStylesHaveChanged = false;
            }

            Application.getSettings().setEditorFont(font);

            for (int item = 0; item < popupGuidanceOptions.length; item++) {
                if (popupGuidanceOptions[item].isSelected()) {
                    Application.getSettings().editorPopupPrefixLength.set(item);
                    Application.getSettings().popupInstructionGuidance.set(item > 0);
                    break;
                }
            }
        }

        // User has clicked the "Revert" button.  Put everything back to initial state.
        @Override
        protected void reset() {
            super.reset();
            initializeSyntaxStyleChangeables();
            resetOtherSettings();
            syntaxStylesHaveChanged = true;
        }

        // Perform reset on miscellaneous editor settings
        private void resetOtherSettings() {
            tabSizeSelector.setValue(initialEditorTabSize);
            tabSizeSpinSelector.setValue(initialEditorTabSize);
            lineHighlightCheck.setSelected(initialLineHighlighting);
            autoIndentCheck.setSelected(initialAutoIndent);
            blinkRateSpinSelector.setValue(initialCaretBlinkRate);
            blinkCaret.setBlinkRate(initialCaretBlinkRate);
            popupGuidanceOptions[initialPopupGuidance].setSelected(true);
        }

        // Miscellaneous editor settings (cursor blinking, line highlighting, tab size, etc)
        private JPanel buildOtherSettingsPanel() {
            JPanel otherSettingsPanel = new JPanel();

            // Tab size
            initialEditorTabSize = Application.getSettings().editorTabSize.get();
            tabSizeSelector = new JSlider(Editor.MIN_TAB_SIZE, Editor.MAX_TAB_SIZE, initialEditorTabSize);
            tabSizeSelector.setToolTipText("Use slider to select tab size in spaces from " + Editor.MIN_TAB_SIZE + " to " + Editor.MAX_TAB_SIZE + ".");
            tabSizeSelector.addChangeListener(event -> tabSizeSpinSelector.setValue(tabSizeSelector.getValue()));
            SpinnerNumberModel tabSizeSpinnerModel = new SpinnerNumberModel(initialEditorTabSize, Editor.MIN_TAB_SIZE, Editor.MAX_TAB_SIZE, 1);
            tabSizeSpinSelector = new JSpinner(tabSizeSpinnerModel);
            tabSizeSpinSelector.setToolTipText(TAB_SIZE_TOOL_TIP_TEXT);
            tabSizeSpinSelector.addChangeListener(event -> tabSizeSelector.setValue((Integer) tabSizeSpinSelector.getValue()));

            // Highlighting of current line
            initialLineHighlighting = Application.getSettings().highlightCurrentEditorLine.get();
            lineHighlightCheck = new JCheckBox("Highlight the line currently being edited");
            lineHighlightCheck.setSelected(initialLineHighlighting);
            lineHighlightCheck.setToolTipText(CURRENT_LINE_HIGHLIGHT_TOOL_TIP_TEXT);

            // Auto-indent
            initialAutoIndent = Application.getSettings().autoIndentEnabled.get();
            autoIndentCheck = new JCheckBox("Enable automatic indentation");
            autoIndentCheck.setSelected(initialAutoIndent);
            autoIndentCheck.setToolTipText(AUTO_INDENT_TOOL_TIP_TEXT);

            // Cursor blink rate
            initialCaretBlinkRate = Application.getSettings().caretBlinkRate.get();
            blinkSample = new JTextField();
            blinkSample.setCaretPosition(0);
            blinkSample.setToolTipText(BLINK_SAMPLE_TOOL_TIP_TEXT);
            blinkSample.setEnabled(false);
            blinkCaret = blinkSample.getCaret();
            blinkCaret.setBlinkRate(initialCaretBlinkRate);
            blinkCaret.setVisible(true);
            SpinnerNumberModel blinkRateSpinnerModel = new SpinnerNumberModel(initialCaretBlinkRate, Editor.MIN_BLINK_RATE, Editor.MAX_BLINK_RATE, 100);
            blinkRateSpinSelector = new JSpinner(blinkRateSpinnerModel);
            blinkRateSpinSelector.setToolTipText(BLINK_SPINNER_TOOL_TIP_TEXT);
            blinkRateSpinSelector.addChangeListener(event -> {
                blinkCaret.setBlinkRate((Integer) blinkRateSpinSelector.getValue());
                blinkSample.requestFocus();
                blinkCaret.setVisible(true);
            });

            JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            tabPanel.add(new JLabel("Tab size (spaces):"));
            tabPanel.add(tabSizeSelector);
            tabPanel.add(tabSizeSpinSelector);

            JPanel blinkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            blinkPanel.add(new JLabel("Cursor blink rate (ms):"));
            blinkPanel.add(blinkRateSpinSelector);
            blinkPanel.add(blinkSample);

            otherSettingsPanel.setLayout(new GridLayout(1, 2));
            JPanel leftColumnSettingsPanel = new JPanel(new GridLayout(4, 1));
            leftColumnSettingsPanel.add(tabPanel);
            leftColumnSettingsPanel.add(blinkPanel);
            leftColumnSettingsPanel.add(lineHighlightCheck);
            leftColumnSettingsPanel.add(autoIndentCheck);

            // Combine instruction guide off/on and instruction prefix length into radio buttons
            popupGuidanceOptions = new JRadioButton[]{new JRadioButton("Do not display instruction guidance"), new JRadioButton("Display instruction guidance after 1 letter typed"), new JRadioButton("Display instruction guidance after 2 letters typed"),};
            JPanel rightColumnSettingsPanel = new JPanel(new GridLayout(popupGuidanceOptions.length, 1));
            rightColumnSettingsPanel.setBorder(createTitledBorder("Instruction Guidance"));
            ButtonGroup popupGuidanceButtons = new ButtonGroup();
            for (int index = 0; index < popupGuidanceOptions.length; index++) {
                popupGuidanceOptions[index].setSelected(false);
                popupGuidanceOptions[index].setToolTipText(POPUP_GUIDANCE_TOOL_TIP_TEXT[index]);
                popupGuidanceButtons.add(popupGuidanceOptions[index]);
                rightColumnSettingsPanel.add(popupGuidanceOptions[index]);
            }
            initialPopupGuidance = Application.getSettings().popupInstructionGuidance.get() ? Application.getSettings().editorPopupPrefixLength.get() : 0;
            popupGuidanceOptions[initialPopupGuidance].setSelected(true);

            otherSettingsPanel.add(leftColumnSettingsPanel);
            otherSettingsPanel.add(rightColumnSettingsPanel);
            return otherSettingsPanel;
        }

        // Control style (color, plain/italic/bold) for syntax highlighting
        private JPanel buildSyntaxStylePanel() {
            JPanel syntaxStylePanel = new JPanel();
            defaultStyles = SyntaxUtilities.getDefaultSyntaxStyles();
            initialStyles = SyntaxUtilities.getCurrentSyntaxStyles(Application.getSettings());
            String[] descriptions = MIPSTokenMarker.getTokenDescriptions();
            String[] examples = MIPSTokenMarker.getTokenExamples();
            syntaxStylesHaveChanged = false;

            // Create the mapping from syntax style rows to indices
            syntaxStyleIndices = new int[descriptions.length];
            int count = 0;
            for (int index = 0; index < descriptions.length; index++) {
                if (descriptions[index] != null) {
                    syntaxStyleIndices[count++] = index;
                }
            }
            syntaxStyleIndices = Arrays.copyOfRange(syntaxStyleIndices, 0, count);

            // Create new arrays (no gaps) for grid display, refer to original index
            currentStyles = new SyntaxStyle[count];
            syntaxSamples = new JLabel[count];
            foregroundButtons = new JButton[count];
            useBold = new JToggleButton[count];
            useItalic = new JToggleButton[count];
            useDefault = new JCheckBox[count];
            Font genericFont = new JLabel().getFont();
            previewFont = new Font(Application.getSettings().getEditorFont().getName(), Font.PLAIN, genericFont.getSize());
            Font boldFont = new Font(Font.SERIF, Font.BOLD, genericFont.getSize());
            Font italicFont = new Font(Font.SERIF, Font.ITALIC, genericFont.getSize());
            // Set all the fixed features.  Changeable features set/reset in initializeSyntaxStyleChangeables
            for (int row = 0; row < syntaxStyleIndices.length; row++) {
                syntaxSamples[row] = new JLabel();
                syntaxSamples[row].setOpaque(true);
                syntaxSamples[row].setHorizontalAlignment(SwingConstants.CENTER);
                syntaxSamples[row].setBackground(UIManager.getColor("Venus.Editor.background"));
                syntaxSamples[row].setToolTipText(SAMPLE_TOOL_TIP_TEXT);
                foregroundButtons[row] = new ColorSelectButton();
                foregroundButtons[row].addActionListener(new ForegroundChanger(row));
                foregroundButtons[row].setToolTipText(FOREGROUND_TOOL_TIP_TEXT);
                BoldItalicChanger boldItalicChanger = new BoldItalicChanger(row);
                useBold[row] = new JToggleButton("B", false);
                useBold[row].setFont(boldFont);
                useBold[row].addActionListener(boldItalicChanger);
                useBold[row].setToolTipText(BOLD_TOOL_TIP_TEXT);
                useItalic[row] = new JToggleButton("I", false);
                useItalic[row].setFont(italicFont);
                useItalic[row].addActionListener(boldItalicChanger);
                useItalic[row].setToolTipText(ITALIC_TOOL_TIP_TEXT);
                useDefault[row] = new JCheckBox();
                useDefault[row].addItemListener(new DefaultChanger(row));
                useDefault[row].setToolTipText(DEFAULT_TOOL_TIP_TEXT);
            }
            initializeSyntaxStyleChangeables();
            // Build a grid
            syntaxStylePanel.setLayout(new BorderLayout(6, 6));
            JPanel labelPreviewPanel = new JPanel(new GridLayout(syntaxStyleIndices.length, 2, 6, 6));
            JPanel buttonsPanel = new JPanel(new GridLayout(syntaxStyleIndices.length, 4, 6, 6));
            // 1: label, 2: preview, 3: foreground chooser, 4: bold, 5: italic, 6: default
            for (int row = 0; row < syntaxStyleIndices.length; row++) {
                labelPreviewPanel.add(new JLabel(descriptions[syntaxStyleIndices[row]], SwingConstants.LEFT));
                syntaxSamples[row].setText(examples[syntaxStyleIndices[row]]);
                labelPreviewPanel.add(syntaxSamples[row]);
                buttonsPanel.add(foregroundButtons[row]);
                buttonsPanel.add(useBold[row]);
                buttonsPanel.add(useItalic[row]);
                buttonsPanel.add(useDefault[row]);
            }
            JPanel instructions = new JPanel(new FlowLayout(FlowLayout.CENTER));
            // Create fake checkbox for illustration purposes
            JCheckBox checkBoxIllustration = new JCheckBox() {
                @Override
                protected void processMouseEvent(MouseEvent event) {}

                @Override
                protected void processKeyEvent(KeyEvent event) {}
            };
            checkBoxIllustration.setSelected(true);
            instructions.add(checkBoxIllustration);
            instructions.add(new JLabel("= use default color and style"));
            syntaxStylePanel.add(labelPreviewPanel, BorderLayout.WEST);
            syntaxStylePanel.add(buttonsPanel, BorderLayout.CENTER);
            syntaxStylePanel.add(instructions, BorderLayout.SOUTH);
            return syntaxStylePanel;
        }

        // Set or reset the changeable features of component for syntax style
        private void initializeSyntaxStyleChangeables() {
            for (int count = 0; count < syntaxSamples.length; count++) {
                int index = syntaxStyleIndices[count];
                syntaxSamples[count].setFont(previewFont);
                syntaxSamples[count].setForeground(initialStyles[index].getColor());
                foregroundButtons[count].setBackground(initialStyles[index].getColor());
                foregroundButtons[count].setEnabled(true);
                currentStyles[count] = initialStyles[index];
                useBold[count].setSelected(initialStyles[index].isBold());
                if (useBold[count].isSelected()) {
                    Font font = syntaxSamples[count].getFont();
                    syntaxSamples[count].setFont(font.deriveFont(font.getStyle() ^ Font.BOLD));
                }
                useItalic[count].setSelected(initialStyles[index].isItalic());
                if (useItalic[count].isSelected()) {
                    Font font = syntaxSamples[count].getFont();
                    syntaxSamples[count].setFont(font.deriveFont(font.getStyle() ^ Font.ITALIC));
                }
                useDefault[count].setSelected(initialStyles[index].toString().equals(defaultStyles[index].toString()));
                if (useDefault[count].isSelected()) {
                    foregroundButtons[count].setEnabled(false);
                    useBold[count].setEnabled(false);
                    useItalic[count].setEnabled(false);
                }
            }
        }

        // Set the foreground color, bold and italic of sample (a JLabel)
        private void setSampleStyles(JLabel sample, SyntaxStyle style) {
            Font font = previewFont;
            if (style.isBold()) {
                font = font.deriveFont(font.getStyle() ^ Font.BOLD);
            }
            if (style.isItalic()) {
                font = font.deriveFont(font.getStyle() ^ Font.ITALIC);
            }
            sample.setFont(font);
            sample.setForeground(style.getColor());
        }

        private static Border createTitledBorder(String title) {
            return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
            );
        }

        /**
         * Toggle bold or italic style on preview button when bold or italic button clicked.
         */
        private class BoldItalicChanger implements ActionListener {
            private final int row;

            public BoldItalicChanger(int row) {
                this.row = row;
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                Font font = syntaxSamples[row].getFont();
                if ("B".equals(event.getActionCommand())) {
                    if (useBold[row].isSelected()) {
                        syntaxSamples[row].setFont(font.deriveFont(font.getStyle() | Font.BOLD));
                    }
                    else {
                        syntaxSamples[row].setFont(font.deriveFont(font.getStyle() ^ Font.BOLD));
                    }
                }
                else {
                    if (useItalic[row].isSelected()) {
                        syntaxSamples[row].setFont(font.deriveFont(font.getStyle() | Font.ITALIC));
                    }
                    else {
                        syntaxSamples[row].setFont(font.deriveFont(font.getStyle() ^ Font.ITALIC));
                    }
                }
                currentStyles[row] = new SyntaxStyle(foregroundButtons[row].getBackground(), useItalic[row].isSelected(), useBold[row].isSelected());
                syntaxStylesHaveChanged = true;
            }
        }

        /**
         * Class that handles click on the foreground color selection button.
         */
        private class ForegroundChanger implements ActionListener {
            private final int row;

            public ForegroundChanger(int row) {
                this.row = row;
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                JButton button = (JButton) event.getSource();
                Color newColor = JColorChooser.showDialog(null, "Set Text Color", button.getBackground());
                if (newColor != null) {
                    button.setBackground(newColor);
                    syntaxSamples[row].setForeground(newColor);
                }
                currentStyles[row] = new SyntaxStyle(button.getBackground(), useItalic[row].isSelected(), useBold[row].isSelected());
                syntaxStylesHaveChanged = true;
            }
        }

        /**
         * Class that handles action (check, uncheck) on the Default checkbox.
         */
        private class DefaultChanger implements ItemListener {
            private final int row;

            public DefaultChanger(int row) {
                this.row = row;
            }

            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    // Disable buttons
                    foregroundButtons[row].setEnabled(false);
                    useBold[row].setEnabled(false);
                    useItalic[row].setEnabled(false);
                    // Save current settings
                    currentStyles[row] = new SyntaxStyle(foregroundButtons[row].getBackground(), useItalic[row].isSelected(), useBold[row].isSelected());
                    // Set to defaults
                    SyntaxStyle defaultStyle = defaultStyles[syntaxStyleIndices[row]];
                    setSampleStyles(syntaxSamples[row], defaultStyle);
                    foregroundButtons[row].setBackground(defaultStyle.getColor());
                    useBold[row].setSelected(defaultStyle.isBold());
                    useItalic[row].setSelected(defaultStyle.isItalic());
                }
                else {
                    // Restore current settings
                    setSampleStyles(syntaxSamples[row], currentStyles[row]);
                    foregroundButtons[row].setBackground(currentStyles[row].getColor());
                    useBold[row].setSelected(currentStyles[row].isBold());
                    useItalic[row].setSelected(currentStyles[row].isItalic());
                    // Enable buttons
                    foregroundButtons[row].setEnabled(true);
                    useBold[row].setEnabled(true);
                    useItalic[row].setEnabled(true);
                }
                syntaxStylesHaveChanged = true;
            }
        }
    }
}