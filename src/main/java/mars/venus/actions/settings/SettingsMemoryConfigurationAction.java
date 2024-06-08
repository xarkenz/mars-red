package mars.venus.actions.settings;

import mars.Application;
import mars.mips.hardware.MemoryConfiguration;
import mars.mips.hardware.MemoryConfigurations;
import mars.simulator.Simulator;
import mars.util.Binary;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;
import mars.venus.execute.ProgramStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

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
public class SettingsMemoryConfigurationAction extends VenusAction {
    /**
     * Create a new SettingsEditorAction.  Has all the GuiAction parameters.
     */
    public SettingsMemoryConfigurationAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    /**
     * When this action is triggered, launch a dialog to view and modify
     * editor settings.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        JDialog configDialog = new MemoryConfigurationDialog(this.gui, "MIPS Memory Configuration", true);
        configDialog.setVisible(true);
    }

    private static class MemoryConfigurationDialog extends JDialog implements ActionListener {
        private final VenusUI gui;
        private JTextField[] addressDisplay;
        private JLabel[] nameDisplay;
        private ConfigurationButton selectedConfigurationButton;
        private ConfigurationButton initialConfigurationButton;

        public MemoryConfigurationDialog(VenusUI gui, String title, boolean modality) {
            super(gui, title, modality);
            this.gui = gui;

            this.setContentPane(this.buildDialogPanel());
            this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    MemoryConfigurationDialog.this.performClose();
                }
            });
            this.pack();
            this.setLocationRelativeTo(gui);
        }

        private JPanel buildDialogPanel() {
            JPanel dialogPanel = new JPanel(new BorderLayout());
            dialogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            JPanel configInfo = new JPanel(new FlowLayout());
            configInfo.add(this.buildConfigChooser());
            configInfo.add(this.buildConfigDisplay());
            dialogPanel.add(configInfo);
            dialogPanel.add(this.buildControlPanel(), BorderLayout.SOUTH);
            return dialogPanel;
        }

        private Component buildConfigChooser() {
            JPanel chooserPanel = new JPanel(new GridLayout(4, 1));
            ButtonGroup choices = new ButtonGroup();
            for (MemoryConfiguration config : MemoryConfigurations.getConfigurations()) {
                ConfigurationButton button = new ConfigurationButton(config);
                button.addActionListener(this);
                if (button.isSelected()) {
                    this.selectedConfigurationButton = button;
                    this.initialConfigurationButton = button;
                }
                choices.add(button);
                chooserPanel.add(button);
            }
            chooserPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Configuration"));
            return chooserPanel;
        }

        private Component buildConfigDisplay() {
            JPanel displayPanel = new JPanel();
            MemoryConfiguration config = MemoryConfigurations.getCurrentConfiguration();
            String[] configurationItemNames = config.addressNames();
            int numItems = configurationItemNames.length;
            JPanel namesPanel = new JPanel(new GridLayout(numItems, 1));
            JPanel valuesPanel = new JPanel(new GridLayout(numItems, 1));
            Font monospaced = new Font("Monospaced", Font.PLAIN, 12);
            nameDisplay = new JLabel[numItems];
            addressDisplay = new JTextField[numItems];
            for (int index = 0; index < numItems; index++) {
                nameDisplay[index] = new JLabel();
                addressDisplay[index] = new JTextField();
                addressDisplay[index].setEditable(false);
                addressDisplay[index].setFont(monospaced);
            }
            // Display vertically from high to low memory addresses so
            // add the components in reverse order.
            for (int index = addressDisplay.length - 1; index >= 0; index--) {
                namesPanel.add(nameDisplay[index]);
                valuesPanel.add(addressDisplay[index]);
            }
            setConfigDisplay(config);
            Box columns = Box.createHorizontalBox();
            columns.add(valuesPanel);
            columns.add(Box.createHorizontalStrut(6));
            columns.add(namesPanel);
            displayPanel.add(columns);
            return displayPanel;
        }

        /**
         * Carry out action for the radio buttons.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            ConfigurationButton button = (ConfigurationButton) event.getSource();
            MemoryConfiguration config = button.getConfiguration();
            setConfigDisplay(config);
            this.selectedConfigurationButton = button;
        }

        // Row of control buttons to be placed along the button of the dialog
        private Component buildControlPanel() {
            Box controlPanel = Box.createHorizontalBox();
            JButton okButton = new JButton("OK");
            okButton.setToolTipText(SettingsHighlightingAction.OK_TOOL_TIP_TEXT);
            okButton.addActionListener(event -> {
                this.performApply();
                this.performClose();
            });
            JButton applyButton = new JButton("Apply");
            applyButton.setToolTipText(SettingsHighlightingAction.APPLY_TOOL_TIP_TEXT);
            applyButton.addActionListener(event -> {
                this.performApply();
            });
            JButton cancelButton = new JButton("Cancel");
            cancelButton.setToolTipText(SettingsHighlightingAction.CANCEL_TOOL_TIP_TEXT);
            cancelButton.addActionListener(event -> {
                this.performClose();
            });
            JButton resetButton = new JButton("Reset");
            resetButton.setToolTipText(SettingsHighlightingAction.REVERT_TOOL_TIP_TEXT);
            resetButton.addActionListener(event -> {
                this.performReset();
            });
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(okButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(applyButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(cancelButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(resetButton);
            controlPanel.add(Box.createHorizontalGlue());
            return controlPanel;
        }

        private void performApply() {
            if (MemoryConfigurations.setCurrentConfiguration(this.selectedConfigurationButton.getConfiguration())) {
                Application.getSettings().memoryConfiguration.set(this.selectedConfigurationButton.getConfiguration().identifier());
                gui.getRegistersPane().getRegistersWindow().clearHighlighting();
                gui.getRegistersPane().getRegistersWindow().updateRegisters();
                gui.getMainPane().getExecuteTab().getDataSegmentWindow().updateBaseAddressComboBox();
            }
        }

        private void performClose() {
            this.setVisible(false);
            this.dispose();
        }

        private void performReset() {
            this.selectedConfigurationButton = this.initialConfigurationButton;
            this.selectedConfigurationButton.setSelected(true);
            setConfigDisplay(this.selectedConfigurationButton.getConfiguration());
        }

        // Set name values in JLabels and address values in the JTextFields
        private void setConfigDisplay(MemoryConfiguration config) {
            String[] configurationItemNames = config.addressNames();
            int[] configurationItemValues = config.addresses();
            // Will use TreeMap to extract list of address-name pairs sorted by
            // hex-stringified address. This will correctly handle kernel addresses,
            // whose int values are negative and thus normal sorting yields incorrect
            // results.  There can be duplicate addresses, so I concatenate the name
            // onto the address to make each key unique.  Then slice off the name upon
            // extraction.
            Map<String, String> treeSortedByAddress = new TreeMap<>();
            for (int i = 0; i < configurationItemValues.length; i++) {
                treeSortedByAddress.put(Binary.intToHexString(configurationItemValues[i]) + configurationItemNames[i], configurationItemNames[i]);
            }
            Iterator<Map.Entry<String, String>> setSortedByAddress = treeSortedByAddress.entrySet().iterator();
            int addressStringLength = Binary.intToHexString(configurationItemValues[0]).length();
            for (int i = 0; i < configurationItemValues.length; i++) {
                Map.Entry<String, String> pair = setSortedByAddress.next();
                nameDisplay[i].setText(pair.getValue());
                addressDisplay[i].setText(pair.getKey().substring(0, addressStringLength));
            }
        }
    }

    // Handy class to connect button to its configuration...
    private static class ConfigurationButton extends JRadioButton {
        private final MemoryConfiguration configuration;

        public ConfigurationButton(MemoryConfiguration config) {
            super(config.name(), config == MemoryConfigurations.getCurrentConfiguration());
            this.configuration = config;
        }

        public MemoryConfiguration getConfiguration() {
            return configuration;
        }
    }
}