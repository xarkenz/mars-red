package mars.venus.actions.settings;

import mars.Application;
import mars.settings.ColorSetting;
import mars.settings.Settings;
import mars.venus.*;
import mars.venus.actions.VenusAction;
import mars.venus.execute.ExecuteTab;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;

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
 * Action class for the Settings menu item for text editor settings.
 */
public class SettingsHighlightingAction extends VenusAction {
    // NOTE: These must follow same sequence and buttons must follow this sequence too!
    private final ColorSetting[] backgroundSettings = {
        Application.getSettings().textSegmentHighlightBackground,
        Application.getSettings().textSegmentDelaySlotHighlightBackground,
        Application.getSettings().dataSegmentHighlightBackground,
        Application.getSettings().registerHighlightBackground,
    };
    private final ColorSetting[] foregroundSettings = {
        Application.getSettings().textSegmentHighlightForeground,
        Application.getSettings().textSegmentDelaySlotHighlightForeground,
        Application.getSettings().dataSegmentHighlightForeground,
        Application.getSettings().registerHighlightForeground,
    };

    private Color[] initialSettingsBackground;
    private Color[] currentNondefaultBackground;
    private Color[] initialSettingsForeground;
    private Color[] currentNondefaultForeground;
    private boolean initialDataHighlightSetting;
    private boolean currentDataHighlightSetting;
    private boolean initialRegisterHighlightSetting;
    private boolean currentRegisterHighlightSetting;

    private JDialog highlightDialog;
    private JButton[] backgroundButtons;
    private JButton[] foregroundButtons;
    private JCheckBox[] defaultCheckBoxes;
    private JLabel[] samples;
    private JButton dataHighlightButton;
    private JButton registerHighlightButton;

    // Tool tips for color buttons
    private static final String SAMPLE_TOOL_TIP_TEXT = "Preview based on background and text color settings";
    private static final String BACKGROUND_TOOL_TIP_TEXT = "Click, to select background color";
    private static final String FOREGROUND_TOOL_TIP_TEXT = "Click, to select text color";
    private static final String DEFAULT_TOOL_TIP_TEXT = "Check, to select default color (disables color select buttons)";
    // Tool tips for the control buttons along the bottom
    public static final String OK_TOOL_TIP_TEXT = "Apply current settings and close dialog";
    public static final String APPLY_TOOL_TIP_TEXT = "Apply current settings now and leave dialog open";
    public static final String REVERT_TOOL_TIP_TEXT = "Reset to initial settings without applying";
    public static final String CANCEL_TOOL_TIP_TEXT = "Close dialog without applying current settings";
    // Tool tips for the data and register highlighting enable/disable controls
    private static final String DATA_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT = "Click, to enable or disable highlighting in Data Segment window";
    private static final String REGISTER_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT = "Click, to enable or disable highlighting in Register windows";

    /**
     * Create a new SettingsEditorAction.  Has all the GuiAction parameters.
     */
    public SettingsHighlightingAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    /**
     * When this action is triggered, launch a dialog to view and modify
     * editor settings.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        highlightDialog = new JDialog(Application.getGUI(), "Runtime Table Highlighting Colors and Fonts", true);
        highlightDialog.setContentPane(buildDialogPanel());
        highlightDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        highlightDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                closeDialog();
            }
        });
        highlightDialog.pack();
        highlightDialog.setLocationRelativeTo(Application.getGUI());
        highlightDialog.setVisible(true);
    }

    /**
     * Build the dialog box that appears when menu item is selected.
     */
    private JPanel buildDialogPanel() {
        JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel patchesPanel = new JPanel(new GridLayout(backgroundSettings.length, 3, 2, 2));
        currentNondefaultBackground = new Color[backgroundSettings.length];
        currentNondefaultForeground = new Color[backgroundSettings.length];
        initialSettingsBackground = new Color[backgroundSettings.length];
        initialSettingsForeground = new Color[backgroundSettings.length];

        backgroundButtons = new JButton[backgroundSettings.length];
        foregroundButtons = new JButton[backgroundSettings.length];
        defaultCheckBoxes = new JCheckBox[backgroundSettings.length];
        samples = new JLabel[backgroundSettings.length];
        for (int i = 0; i < backgroundSettings.length; i++) {
            backgroundButtons[i] = new ColorSelectButton();
            foregroundButtons[i] = new ColorSelectButton();
            defaultCheckBoxes[i] = new JCheckBox();
            samples[i] = new JLabel(" preview ");
            backgroundButtons[i].addActionListener(new BackgroundChanger(i));
            foregroundButtons[i].addActionListener(new ForegroundChanger(i));
            defaultCheckBoxes[i].addItemListener(new DefaultChanger(i));
            samples[i].setToolTipText(SAMPLE_TOOL_TIP_TEXT);
            backgroundButtons[i].setToolTipText(BACKGROUND_TOOL_TIP_TEXT);
            foregroundButtons[i].setToolTipText(FOREGROUND_TOOL_TIP_TEXT);
            defaultCheckBoxes[i].setToolTipText(DEFAULT_TOOL_TIP_TEXT);
        }

        initializeButtonColors();

        for (int i = 0; i < backgroundSettings.length; i++) {
            patchesPanel.add(backgroundButtons[i]);
            patchesPanel.add(foregroundButtons[i]);
            patchesPanel.add(defaultCheckBoxes[i]);
        }

        JPanel descriptionsPanel = new JPanel(new GridLayout(backgroundSettings.length, 1, 2, 2));
        // Note the labels have to match buttons by position...
        descriptionsPanel.add(new JLabel("Text Segment highlighting", SwingConstants.RIGHT));
        descriptionsPanel.add(new JLabel("Text Segment Delay Slot highlighting", SwingConstants.RIGHT));
        descriptionsPanel.add(new JLabel("Data Segment highlighting *", SwingConstants.RIGHT));
        descriptionsPanel.add(new JLabel("Register highlighting *", SwingConstants.RIGHT));

        JPanel samplesPanel = new JPanel(new GridLayout(backgroundSettings.length, 1, 2, 2));
        for (JLabel sample : samples) {
            samplesPanel.add(sample);
        }

        JPanel instructions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // Create fake checkbox for illustration purposes
        JCheckBox illustration = new JCheckBox() {
            @Override
            protected void processMouseEvent(MouseEvent event) {}

            @Override
            protected void processKeyEvent(KeyEvent event) {}
        };
        illustration.setSelected(true);
        instructions.add(illustration);
        instructions.add(new JLabel("= use default colors (disables color selection buttons)"));
        int spacer = 10;
        Box mainArea = Box.createHorizontalBox();
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(descriptionsPanel);
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(samplesPanel);
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(patchesPanel);

        contents.add(mainArea, BorderLayout.EAST);
        contents.add(instructions, BorderLayout.NORTH);

        // Control highlighting enable/disable for Data Segment window and Register windows
        JPanel dataRegisterHighlightControl = new JPanel(new GridLayout(2, 1));
        dataHighlightButton = new JButton();
        dataHighlightButton.setText(getHighlightControlText(currentDataHighlightSetting));
        dataHighlightButton.setToolTipText(DATA_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT);
        dataHighlightButton.addActionListener(event -> {
            currentDataHighlightSetting = !currentDataHighlightSetting;
            dataHighlightButton.setText(getHighlightControlText(currentDataHighlightSetting));
        });
        registerHighlightButton = new JButton();
        registerHighlightButton.setText(getHighlightControlText(currentRegisterHighlightSetting));
        registerHighlightButton.setToolTipText(REGISTER_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT);
        registerHighlightButton.addActionListener(event -> {
            currentRegisterHighlightSetting = !currentRegisterHighlightSetting;
            registerHighlightButton.setText(getHighlightControlText(currentRegisterHighlightSetting));
        });
        JPanel dataHighlightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel registerHighlightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataHighlightPanel.add(new JLabel("* Data Segment highlighting is"));
        dataHighlightPanel.add(dataHighlightButton);
        registerHighlightPanel.add(new JLabel("* Register highlighting is"));
        registerHighlightPanel.add(registerHighlightButton);
        dataRegisterHighlightControl.setBorder(new LineBorder(Color.BLACK));
        dataRegisterHighlightControl.add(dataHighlightPanel);
        dataRegisterHighlightControl.add(registerHighlightPanel);

        // Bottom row - the control buttons for OK, Apply, Cancel
        Box controlPanel = Box.createHorizontalBox();
        JButton okButton = new JButton("OK");
        okButton.setToolTipText(OK_TOOL_TIP_TEXT);
        okButton.addActionListener(event -> {
            applySettings();
            closeDialog();
        });
        JButton applyButton = new JButton("Apply");
        applyButton.setToolTipText(APPLY_TOOL_TIP_TEXT);
        applyButton.addActionListener(event -> applySettings());
        JButton resetButton = new JButton("Revert");
        resetButton.setToolTipText(REVERT_TOOL_TIP_TEXT);
        resetButton.addActionListener(event -> revertSettings());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText(CANCEL_TOOL_TIP_TEXT);
        cancelButton.addActionListener(event -> closeDialog());
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(okButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(applyButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(resetButton);
        controlPanel.add(Box.createHorizontalGlue());

        JPanel allControls = new JPanel(new GridLayout(2, 1));
        allControls.add(dataRegisterHighlightControl);
        allControls.add(controlPanel);
        contents.add(allControls, BorderLayout.SOUTH);
        return contents;
    }

    private String getHighlightControlText(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    /**
     * Called once, upon dialog setup.
     */
    private void initializeButtonColors() {
        for (int row = 0; row < backgroundSettings.length; row++) {
            Color background = backgroundSettings[row].get();
            Color foreground = foregroundSettings[row].get();

            backgroundButtons[row].setBackground(background);
            foregroundButtons[row].setBackground(foreground);
            currentNondefaultBackground[row] = background;
            currentNondefaultForeground[row] = foreground;
            initialSettingsBackground[row] = background;
            initialSettingsForeground[row] = foreground;
            samples[row].setOpaque(true); // Otherwise, background color will not be rendered
            samples[row].setBackground(background);
            samples[row].setForeground(foreground);

            boolean usingDefaults = background.equals(backgroundSettings[row].getDefault())
                && foreground.equals(foregroundSettings[row].getDefault());
            defaultCheckBoxes[row].setSelected(usingDefaults);
            backgroundButtons[row].setEnabled(!usingDefaults);
            foregroundButtons[row].setEnabled(!usingDefaults);
        }

        currentDataHighlightSetting = initialDataHighlightSetting = Application.getSettings().highlightDataSegment.get();
        currentRegisterHighlightSetting = initialRegisterHighlightSetting = Application.getSettings().highlightRegisters.get();
    }

    /**
     * Set the color settings according to current button colors.  Occurs when "Apply" selected.
     */
    private void applySettings() {
        Settings settings = Application.getSettings();
        for (int i = 0; i < backgroundSettings.length; i++) {
            backgroundSettings[i].set(backgroundButtons[i].getBackground());
            foregroundSettings[i].set(foregroundButtons[i].getBackground());
        }
        settings.highlightDataSegment.set(currentDataHighlightSetting);
        settings.highlightRegisters.set(currentRegisterHighlightSetting);
        RegistersPane registersPane = Application.getGUI().getRegistersPane();
        ExecuteTab executeTab = Application.getGUI().getMainPane().getExecuteTab();
        registersPane.getRegistersWindow().refresh();
        registersPane.getCoprocessor0Window().refresh();
        registersPane.getCoprocessor1Window().refresh();
        // If a successful assembly has occurred, the various panes will be populated with tables
        // and we want to apply the new settings.  If it has NOT occurred, there are no tables
        // in the Data and Text segment windows so we don't want to disturb them.
        // In the latter case, the component count for the Text segment window is 0 (but is 1
        // for Data segment window).
        if (executeTab.getTextSegmentWindow().getContentPane().getComponentCount() > 0) {
            executeTab.getDataSegmentWindow().updateValues();
            executeTab.getTextSegmentWindow().highlightStepAtPC();
        }
    }

    /**
     * Called when Reset selected.
     */
    private void revertSettings() {
        dataHighlightButton.setText(getHighlightControlText(initialDataHighlightSetting));
        registerHighlightButton.setText(getHighlightControlText(initialRegisterHighlightSetting));
        Color backgroundSetting, foregroundSetting;
        for (int i = 0; i < backgroundSettings.length; i++) {
            backgroundSetting = initialSettingsBackground[i];
            foregroundSetting = initialSettingsForeground[i];
            backgroundButtons[i].setBackground(backgroundSetting);
            foregroundButtons[i].setBackground(foregroundSetting);
            samples[i].setBackground(backgroundSetting);
            samples[i].setForeground(foregroundSetting);
            boolean usingDefaults = backgroundSetting.equals(backgroundSettings[i].getDefault())
                && foregroundSetting.equals(foregroundSettings[i].getDefault());
            defaultCheckBoxes[i].setSelected(usingDefaults);
            backgroundButtons[i].setEnabled(!usingDefaults);
            foregroundButtons[i].setEnabled(!usingDefaults);
        }
    }

    // We're finished with this modal dialog.
    private void closeDialog() {
        highlightDialog.setVisible(false);
        highlightDialog.dispose();
    }

    /////////////////////////////////////////////////////////////////
    //
    //  Class that handles click on the background selection button
    //
    private class BackgroundChanger implements ActionListener {
        private final int position;

        public BackgroundChanger(int pos) {
            position = pos;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            Color newColor = JColorChooser.showDialog(null, "Set Background Color", button.getBackground());
            if (newColor != null) {
                button.setBackground(newColor);
                currentNondefaultBackground[position] = newColor;
                samples[position].setBackground(newColor);
            }
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    //  Class that handles click on the foreground selection button
    //
    private class ForegroundChanger implements ActionListener {
        private final int position;

        public ForegroundChanger(int pos) {
            position = pos;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            Color newColor = JColorChooser.showDialog(null, "Set Text Color", button.getBackground());
            if (newColor != null) {
                button.setBackground(newColor);
                currentNondefaultForeground[position] = newColor;
                samples[position].setForeground(newColor);
            }
        }
    }

    /**
     * Class that handles action (check, uncheck) on the Default checkbox.
     */
    private class DefaultChanger implements ItemListener {
        private final int position;

        public DefaultChanger(int pos) {
            position = pos;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            // If selected: disable buttons, set their bg values from default setting, set sample bg & fg
            // If deselected: enable buttons, set their bg values from current setting, set sample bg & bg
            Color newBackground;
            Color newForeground;
            if (e.getStateChange() == ItemEvent.SELECTED) {
                backgroundButtons[position].setEnabled(false);
                foregroundButtons[position].setEnabled(false);
                newBackground = backgroundSettings[position].getDefault();
                newForeground = foregroundSettings[position].getDefault();
                currentNondefaultBackground[position] = backgroundButtons[position].getBackground();
                currentNondefaultForeground[position] = foregroundButtons[position].getBackground();
            }
            else {
                backgroundButtons[position].setEnabled(true);
                foregroundButtons[position].setEnabled(true);
                newBackground = currentNondefaultBackground[position];
                newForeground = currentNondefaultForeground[position];
            }
            backgroundButtons[position].setBackground(newBackground);
            foregroundButtons[position].setBackground(newForeground);
            samples[position].setBackground(newBackground);
            samples[position].setForeground(newForeground);
        }
    }
}