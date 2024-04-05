package mars.venus.actions.settings;

import mars.Globals;
import mars.venus.*;
import mars.venus.actions.VenusAction;
import mars.venus.editors.jeditsyntax.SyntaxStyle;
import mars.venus.editors.jeditsyntax.SyntaxUtilities;
import mars.venus.editors.jeditsyntax.tokenmarker.MIPSTokenMarker;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.Caret;
import java.awt.*;
import java.awt.event.*;

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
        JDialog editorDialog = new EditorFontDialog(Globals.getGUI(), "Text Editor Settings", true, Globals.getSettings().getEditorFont());
        editorDialog.setVisible(true);
    }

    private static final String GENERIC_TOOL_TIP_TEXT = "Use a generic text editor (similar to Notepad) instead of language-aware styled editor.";

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

    // Concrete font chooser class.
    private static class EditorFontDialog extends AbstractFontSettingDialog {
        private JButton[] foregroundButtons;
        private JLabel[] syntaxSamples;
        private JToggleButton[] useBold;
        private JToggleButton[] useItalic;
        private JCheckBox[] useDefault;

        private int[] syntaxStyleIndex;
        private SyntaxStyle[] defaultStyles, initialStyles, currentStyles;
        private Font previewFont;

        private JPanel syntaxStylePanel;
        private JPanel otherSettingsPanel; // 4 Aug 2010

        private JSlider tabSizeSelector;
        private JSpinner tabSizeSpinSelector, blinkRateSpinSelector;
        private JCheckBox lineHighlightCheck, genericEditorCheck, autoIndentCheck;
        private Caret blinkCaret;
        private JTextField blinkSample;
        private JRadioButton[] popupGuidanceOptions;
        // Flag to indicate whether any syntax style buttons have been clicked
        // since dialog created or most recent "apply".
        private boolean syntaxStylesHaveChanged = false;

        private int initialEditorTabSize, initialCaretBlinkRate, initialPopupGuidance;
        private boolean initialLineHighlighting, initialGenericTextEditor, initialAutoIndent;

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
            this.syntaxStylePanel = syntaxStylePanel; // 4 Aug 2010
            this.otherSettingsPanel = otherSettingsPanel; // 4 Aug 2010
            return dialog;
        }

        // Row of control buttons to be placed along the button of the dialog
        @Override
        protected Component buildControlPanel() {
            JButton resetButton = new JButton("Reset to Defaults");
            resetButton.setToolTipText(SettingsHighlightingAction.RESET_TOOL_TIP_TEXT);
            resetButton.addActionListener(event -> reset());

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

            // TODO: Put this option somewhere else! -Sean Clarke
            initialGenericTextEditor = Globals.getSettings().useGenericTextEditor.get();
            genericEditorCheck = new JCheckBox("Use generic text editor", initialGenericTextEditor);
            genericEditorCheck.setToolTipText(GENERIC_TOOL_TIP_TEXT);
            genericEditorCheck.addItemListener(event -> {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    syntaxStylePanel.setVisible(false);
                    otherSettingsPanel.setVisible(false);
                }
                else {
                    syntaxStylePanel.setVisible(true);
                    otherSettingsPanel.setVisible(true);
                }
            });

            Box controlPanel = Box.createHorizontalBox();
            controlPanel.setBorder(BorderFactory.createEmptyBorder(12, 6, 6, 6));
            controlPanel.add(resetButton);
            controlPanel.add(Box.createHorizontalStrut(12));
            controlPanel.add(genericEditorCheck);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(okButton);
            controlPanel.add(Box.createHorizontalStrut(12));
            controlPanel.add(cancelButton);
            controlPanel.add(Box.createHorizontalStrut(12));
            controlPanel.add(applyButton);
            return controlPanel;
        }

        // User has clicked "Apply" or "OK" button.  Required method, is
        // abstract in superclass.
        @Override
        protected void apply(Font font) {
            Globals.getSettings().useGenericTextEditor.set(genericEditorCheck.isSelected());
            Globals.getSettings().highlightCurrentEditorLine.set(lineHighlightCheck.isSelected());
            Globals.getSettings().autoIndentEnabled.set(autoIndentCheck.isSelected());
            Globals.getSettings().caretBlinkRate.set((Integer) blinkRateSpinSelector.getValue());
            Globals.getSettings().editorTabSize.set(tabSizeSelector.getValue());
            if (syntaxStylesHaveChanged) {
                for (int i = 0; i < syntaxStyleIndex.length; i++) {
                    Globals.getSettings().setEditorSyntaxStyleByPosition(syntaxStyleIndex[i], new SyntaxStyle(syntaxSamples[i].getForeground(), useItalic[i].isSelected(), useBold[i].isSelected()));
                }
                syntaxStylesHaveChanged = false; // reset
            }
            Globals.getSettings().setEditorFont(font);
            for (int i = 0; i < popupGuidanceOptions.length; i++) {
                if (popupGuidanceOptions[i].isSelected()) {
                    if (i == 0) {
                        Globals.getSettings().popupInstructionGuidance.set(false);
                    }
                    else {
                        Globals.getSettings().popupInstructionGuidance.set(true);
                        Globals.getSettings().editorPopupPrefixLength.set(i);
                    }
                    break;
                }
            }
        }

        // User has clicked "Reset to Defaults" button.  Put everything back to initial state.
        @Override
        protected void reset() {
            super.reset();
            initializeSyntaxStyleChangeables();
            resetOtherSettings();
            syntaxStylesHaveChanged = true;
            genericEditorCheck.setSelected(initialGenericTextEditor);
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

            // Tab size selector
            initialEditorTabSize = Globals.getSettings().editorTabSize.get();
            tabSizeSelector = new JSlider(Editor.MIN_TAB_SIZE, Editor.MAX_TAB_SIZE, initialEditorTabSize);
            tabSizeSelector.setToolTipText("Use slider to select tab size from " + Editor.MIN_TAB_SIZE + " to " + Editor.MAX_TAB_SIZE + ".");
            tabSizeSelector.addChangeListener(event -> {
                int value = ((JSlider) event.getSource()).getValue();
                tabSizeSpinSelector.setValue(value);
            });
            SpinnerNumberModel tabSizeSpinnerModel = new SpinnerNumberModel(initialEditorTabSize, Editor.MIN_TAB_SIZE, Editor.MAX_TAB_SIZE, 1);
            tabSizeSpinSelector = new JSpinner(tabSizeSpinnerModel);
            tabSizeSpinSelector.setToolTipText(TAB_SIZE_TOOL_TIP_TEXT);
            tabSizeSpinSelector.addChangeListener(event -> {
                Object value = ((JSpinner) event.getSource()).getValue();
                tabSizeSelector.setValue((Integer) value);
            });

            // highlighting of current line
            initialLineHighlighting = Globals.getSettings().highlightCurrentEditorLine.get();
            lineHighlightCheck = new JCheckBox("Highlight the line currently being edited");
            lineHighlightCheck.setSelected(initialLineHighlighting);
            lineHighlightCheck.setToolTipText(CURRENT_LINE_HIGHLIGHT_TOOL_TIP_TEXT);

            // auto-indent
            initialAutoIndent = Globals.getSettings().autoIndentEnabled.get();
            autoIndentCheck = new JCheckBox("Enable automatic indentation");
            autoIndentCheck.setSelected(initialAutoIndent);
            autoIndentCheck.setToolTipText(AUTO_INDENT_TOOL_TIP_TEXT);

            // cursor blink rate selector
            initialCaretBlinkRate = Globals.getSettings().caretBlinkRate.get();
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
                Object value = ((JSpinner) event.getSource()).getValue();
                blinkCaret.setBlinkRate((Integer) value);
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
            popupGuidanceOptions = new JRadioButton[] {
                new JRadioButton("Do not display instruction guidance"),
                new JRadioButton("Display instruction guidance after 1 letter typed"),
                new JRadioButton("Display instruction guidance after 2 letters typed"),
            };
            JPanel rightColumnSettingsPanel = new JPanel(new GridLayout(popupGuidanceOptions.length, 1));
            rightColumnSettingsPanel.setBorder(createTitledBorder("Instruction Guidance"));
            ButtonGroup popupGuidanceButtons = new ButtonGroup();
            for (int i = 0; i < popupGuidanceOptions.length; i++) {
                popupGuidanceOptions[i].setSelected(false);
                popupGuidanceOptions[i].setToolTipText(POPUP_GUIDANCE_TOOL_TIP_TEXT[i]);
                popupGuidanceButtons.add(popupGuidanceOptions[i]);
                rightColumnSettingsPanel.add(popupGuidanceOptions[i]);
            }
            initialPopupGuidance = Globals.getSettings().popupInstructionGuidance.get() ? Globals.getSettings().editorPopupPrefixLength.get() : 0;
            popupGuidanceOptions[initialPopupGuidance].setSelected(true);

            otherSettingsPanel.add(leftColumnSettingsPanel);
            otherSettingsPanel.add(rightColumnSettingsPanel);
            return otherSettingsPanel;
        }

        // Control style (color, plain/italic/bold) for syntax highlighting
        private JPanel buildSyntaxStylePanel() {
            JPanel syntaxStylePanel = new JPanel();
            defaultStyles = SyntaxUtilities.getDefaultSyntaxStyles();
            initialStyles = SyntaxUtilities.getCurrentSyntaxStyles();
            String[] labels = MIPSTokenMarker.getMIPSTokenLabels();
            String[] sampleText = MIPSTokenMarker.getMIPSTokenExamples();
            syntaxStylesHaveChanged = false;
            int count = 0;
            // Count the number of actual styles specified
            for (String label : labels) {
                if (label != null) {
                    count++;
                }
            }
            // create new arrays (no gaps) for grid display, refer to original index
            syntaxStyleIndex = new int[count];
            currentStyles = new SyntaxStyle[count];
            String[] label = new String[count];
            syntaxSamples = new JLabel[count];
            foregroundButtons = new JButton[count];
            useBold = new JToggleButton[count];
            useItalic = new JToggleButton[count];
            useDefault = new JCheckBox[count];
            Font genericFont = new JLabel().getFont();
            previewFont = new Font(Globals.getSettings().getEditorFont().getName(), Font.PLAIN, genericFont.getSize());
            Font boldFont = new Font(Font.SERIF, Font.BOLD, genericFont.getSize());
            Font italicFont = new Font(Font.SERIF, Font.ITALIC, genericFont.getSize());
            count = 0;
            // Set all the fixed features.  Changeable features set/reset in initializeSyntaxStyleChangeables
            for (int index = 0; index < labels.length; index++) {
                if (labels[index] != null) {
                    syntaxStyleIndex[count] = index;
                    syntaxSamples[count] = new JLabel();
                    syntaxSamples[count].setOpaque(true);
                    syntaxSamples[count].setHorizontalAlignment(SwingConstants.CENTER);
                    syntaxSamples[count].setText(sampleText[index]);
                    syntaxSamples[count].setBackground(UIManager.getColor("Venus.Editor.background"));
                    syntaxSamples[count].setToolTipText(SAMPLE_TOOL_TIP_TEXT);
                    foregroundButtons[count] = new ColorSelectButton();
                    foregroundButtons[count].addActionListener(new ForegroundChanger(count));
                    foregroundButtons[count].setToolTipText(FOREGROUND_TOOL_TIP_TEXT);
                    BoldItalicChanger boldItalicChanger = new BoldItalicChanger(count);
                    useBold[count] = new JToggleButton("B", false);
                    useBold[count].setFont(boldFont);
                    useBold[count].addActionListener(boldItalicChanger);
                    useBold[count].setToolTipText(BOLD_TOOL_TIP_TEXT);
                    useItalic[count] = new JToggleButton("I", false);
                    useItalic[count].setFont(italicFont);
                    useItalic[count].addActionListener(boldItalicChanger);
                    useItalic[count].setToolTipText(ITALIC_TOOL_TIP_TEXT);
                    label[count] = labels[index];
                    useDefault[count] = new JCheckBox();
                    useDefault[count].addItemListener(new DefaultChanger(count));
                    useDefault[count].setToolTipText(DEFAULT_TOOL_TIP_TEXT);
                    count++;
                }
            }
            initializeSyntaxStyleChangeables();
            // build a grid
            syntaxStylePanel.setLayout(new BorderLayout(6, 6));
            JPanel labelPreviewPanel = new JPanel(new GridLayout(syntaxStyleIndex.length, 2, 6, 6));
            JPanel buttonsPanel = new JPanel(new GridLayout(syntaxStyleIndex.length, 4, 6, 6));
            // column 1: label,  column 2: preview, column 3: foreground chooser, column 4/5: bold/italic, column 6: default
            for (int i = 0; i < syntaxStyleIndex.length; i++) {
                labelPreviewPanel.add(new JLabel(label[i], SwingConstants.LEFT));
                labelPreviewPanel.add(syntaxSamples[i]);
                buttonsPanel.add(foregroundButtons[i]);
                buttonsPanel.add(useBold[i]);
                buttonsPanel.add(useItalic[i]);
                buttonsPanel.add(useDefault[i]);
            }
            JPanel instructions = new JPanel(new FlowLayout(FlowLayout.CENTER));
            // create fake checkbox for illustration purposes
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
                int index = syntaxStyleIndex[count];
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

        // Toggle bold or italic style on preview button when B or I button clicked
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

        // Class that handles click on the foreground selection button
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

        // Class that handles action (check, uncheck) on the Default checkbox.
        private class DefaultChanger implements ItemListener {
            private final int row;

            public DefaultChanger(int pos) {
                row = pos;
            }

            @Override
            public void itemStateChanged(ItemEvent event) {
                // If selected: disable buttons, save current settings, set to defaults
                // If deselected: restore current settings, enable buttons
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    foregroundButtons[row].setEnabled(false);
                    useBold[row].setEnabled(false);
                    useItalic[row].setEnabled(false);
                    currentStyles[row] = new SyntaxStyle(foregroundButtons[row].getBackground(), useItalic[row].isSelected(), useBold[row].isSelected());
                    SyntaxStyle defaultStyle = defaultStyles[syntaxStyleIndex[row]];
                    setSampleStyles(syntaxSamples[row], defaultStyle);
                    foregroundButtons[row].setBackground(defaultStyle.getColor());
                    useBold[row].setSelected(defaultStyle.isBold());
                    useItalic[row].setSelected(defaultStyle.isItalic());
                }
                else {
                    setSampleStyles(syntaxSamples[row], currentStyles[row]);
                    foregroundButtons[row].setBackground(currentStyles[row].getColor());
                    useBold[row].setSelected(currentStyles[row].isBold());
                    useItalic[row].setSelected(currentStyles[row].isItalic());
                    foregroundButtons[row].setEnabled(true);
                    useBold[row].setEnabled(true);
                    useItalic[row].setEnabled(true);
                }
                syntaxStylesHaveChanged = true;
            }
        }
    }
}