package mars.venus.preferences;

import mars.Application;
import mars.mips.hardware.MemoryConfiguration;
import mars.mips.hardware.MemoryConfigurations;
import mars.settings.Settings;
import mars.util.Binary;
import mars.venus.SimpleCellRenderer;
import mars.venus.StaticTableModel;
import mars.venus.preferences.components.CheckBoxPreference;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

public class MemoryPreferencesTab extends PreferencesTab {
    private final CheckBoxPreference useBigEndian;
    private ConfigurationButton selectedConfigurationButton;
    private ConfigurationButton initialConfigurationButton;
    private JTable memoryRangeTable;
    private Object[][] memoryRangeData;
    private JTable memoryLocationTable;
    private Object[][] memoryLocationData;

    public MemoryPreferencesTab(Settings settings) {
        super(settings, "Memory");

        this.addRow(this.useBigEndian = new CheckBoxPreference(
            this.settings.useBigEndian,
            "Use big-endian byte ordering",
            "When enabled, the bytes in a word will be ordered from most to least significant. "
            + "By default, the bytes in a word are ordered from least to most significant (little-endian)."
        ));
        this.addRow(this.buildConfigChooser(MemoryConfigurations.getConfigurations()));
    }

    @Override
    public void applyChanges() {
        this.useBigEndian.apply();
        if (MemoryConfigurations.setCurrentConfiguration(this.selectedConfigurationButton.getConfiguration())) {
            this.settings.memoryConfiguration.set(this.selectedConfigurationButton.getConfiguration().identifier());
            Application.getGUI().getRegistersPane().getProcessorTab().clearHighlighting();
            Application.getGUI().getRegistersPane().getProcessorTab().updateRegisters();
            Application.getGUI().getMainPane().getExecuteTab().getDataSegmentWindow().updateBaseAddressComboBox();
        }
    }

    @Override
    public void revertChanges() {
        this.useBigEndian.revert();
        this.selectedConfigurationButton = this.initialConfigurationButton;
        this.selectedConfigurationButton.setSelected(true);
        this.setConfigDisplay(this.selectedConfigurationButton.getConfiguration());
    }

    private JComponent buildConfigChooser(List<MemoryConfiguration> configurations) {
        JPanel choicesPanel = new JPanel(new GridLayout(configurations.size(), 1));
        ButtonGroup choicesGroup = new ButtonGroup();
        for (MemoryConfiguration configuration : configurations) {
            ConfigurationButton button = new ConfigurationButton(configuration);
            button.addActionListener(event -> {
                this.setConfigDisplay(button.getConfiguration());
                this.selectedConfigurationButton = button;
            });
            if (button.isSelected()) {
                this.selectedConfigurationButton = button;
                this.initialConfigurationButton = button;
            }
            choicesGroup.add(button);
            choicesPanel.add(button);
        }

        SimpleCellRenderer addressRenderer = new SimpleCellRenderer(SwingConstants.CENTER);

        String[] memoryRangeColumns = { "Description", "Lowest Address", "Highest Address" };
        this.memoryRangeData = new Object[10][3];
        this.memoryRangeData[0][0] = "Mapped address space";
        this.memoryRangeData[1][0] = "User space";
        this.memoryRangeData[2][0] = "Text segment (.text)";
        this.memoryRangeData[3][0] = "Data segment";
        this.memoryRangeData[4][0] = "Global data (.extern)";
        this.memoryRangeData[5][0] = "Static data (.data)";
        this.memoryRangeData[6][0] = "Heap/stack data";
        this.memoryRangeData[7][0] = "Kernel text segment (.ktext)";
        this.memoryRangeData[8][0] = "Kernel data segment (.kdata)";
        this.memoryRangeData[9][0] = "Memory-mapped I/O";

        this.memoryRangeTable = new JTable(new StaticTableModel(this.memoryRangeData, memoryRangeColumns));
        this.memoryRangeTable.setCellSelectionEnabled(false);
        this.memoryRangeTable.putClientProperty("FlatLaf.style", "showVerticalLines: false");
        this.memoryRangeTable.getTableHeader().setReorderingAllowed(false);

        this.memoryRangeTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        this.memoryRangeTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        this.memoryRangeTable.getColumnModel().getColumn(1).setCellRenderer(addressRenderer);
        this.memoryRangeTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        this.memoryRangeTable.getColumnModel().getColumn(2).setCellRenderer(addressRenderer);

        JPanel memoryRangePanel = new JPanel(new BorderLayout());
        memoryRangePanel.add(this.memoryRangeTable, BorderLayout.CENTER);
        memoryRangePanel.add(this.memoryRangeTable.getTableHeader(), BorderLayout.NORTH);
        memoryRangePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(20, 6, 6, 6),
                "Memory Ranges",
                TitledBorder.CENTER,
                TitledBorder.TOP
            ),
            BorderFactory.createLineBorder(UIManager.getColor("Table.gridColor"))
        ));

        String[] memoryLocationColumns = { "Description", "Address" };
        this.memoryLocationData = new Object[3][2];

        this.memoryLocationTable = new JTable(new StaticTableModel(this.memoryLocationData, memoryLocationColumns));
        this.memoryLocationTable.setCellSelectionEnabled(false);
        this.memoryLocationTable.putClientProperty("FlatLaf.style", "showVerticalLines: false");
        this.memoryLocationTable.getTableHeader().setReorderingAllowed(false);

        this.memoryLocationTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        this.memoryLocationTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        this.memoryLocationTable.getColumnModel().getColumn(1).setCellRenderer(addressRenderer);

        JPanel memoryLocationPanel = new JPanel(new BorderLayout());
        memoryLocationPanel.add(this.memoryLocationTable, BorderLayout.CENTER);
        memoryLocationPanel.add(this.memoryLocationTable.getTableHeader(), BorderLayout.NORTH);
        memoryLocationPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(20, 6, 6, 6),
                "Memory Locations",
                TitledBorder.CENTER,
                TitledBorder.TOP
            ),
            BorderFactory.createLineBorder(UIManager.getColor("Table.gridColor"))
        ));

        this.setConfigDisplay(MemoryConfigurations.getCurrentConfiguration());

        Box chooserPanels = Box.createVerticalBox();
        chooserPanels.add(choicesPanel);
        chooserPanels.add(memoryRangePanel);
        chooserPanels.add(memoryLocationPanel);
        chooserPanels.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                "Memory Layout",
                TitledBorder.LEADING,
                TitledBorder.TOP
            ),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        return chooserPanels;
    }

    private void setConfigDisplay(MemoryConfiguration configuration) {
        String[] addressNames = configuration.addressNames();
        int[] addresses = configuration.addresses();

        for (int i = 0; i < 20; i++) {
            this.memoryRangeData[i / 2][1 + i % 2] = Binary.intToHexString(addresses[i]);
        }

        ((StaticTableModel) this.memoryRangeTable.getModel()).fireTableDataChanged();

        for (int i = 20; i < addresses.length; i++) {
            int row = i - 20;
            this.memoryLocationData[row][0] = addressNames[i];
            this.memoryLocationData[row][1] = Binary.intToHexString(addresses[i]);
        }

        ((StaticTableModel) this.memoryLocationTable.getModel()).fireTableDataChanged();
    }

    private static class ConfigurationButton extends JRadioButton {
        private final MemoryConfiguration configuration;

        public ConfigurationButton(MemoryConfiguration configuration) {
            super(configuration.name(), configuration == MemoryConfigurations.getCurrentConfiguration());
            this.configuration = configuration;
        }

        public MemoryConfiguration getConfiguration() {
            return this.configuration;
        }
    }
}
