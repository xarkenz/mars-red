package mars.venus.preferences;

import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryLayout;
import mars.settings.Settings;
import mars.util.Binary;
import mars.venus.SimpleCellRenderer;
import mars.venus.StaticTableModel;
import mars.venus.preferences.components.CheckBoxPreference;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

public class MemoryPreferencesTab extends PreferencesTab {
    private final CheckBoxPreference useBigEndian;
    private MemoryLayoutButton selectedLayoutButton;
    private MemoryLayoutButton initialLayoutButton;
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
        this.addRow(this.buildLayoutChooser());
    }

    @Override
    public void applyChanges() {
        this.useBigEndian.apply();
        this.settings.memoryLayout.set(this.selectedLayoutButton.getKey());
    }

    @Override
    public void revertChanges() {
        this.useBigEndian.revert();
        this.selectedLayoutButton = this.initialLayoutButton;
        this.selectedLayoutButton.setSelected(true);
        this.setLayoutDisplay(this.selectedLayoutButton.getKey());
    }

    private JComponent buildLayoutChooser() {
        JPanel choicesPanel = new JPanel(new GridLayout(Memory.getLayouts().size(), 1));
        ButtonGroup choicesGroup = new ButtonGroup();
        String currentLayoutKey = this.settings.memoryLayout.get();
        for (Map.Entry<String, MemoryLayout> entry : Memory.getLayouts().entrySet()) {
            MemoryLayoutButton button = new MemoryLayoutButton(
                entry.getKey(),
                entry.getValue().displayName,
                entry.getKey().equals(currentLayoutKey)
            );
            button.addActionListener(event -> {
                this.setLayoutDisplay(button.getKey());
                this.selectedLayoutButton = button;
            });
            if (button.isSelected()) {
                this.selectedLayoutButton = button;
                this.initialLayoutButton = button;
            }
            choicesGroup.add(button);
            choicesPanel.add(button);
        }

        SimpleCellRenderer addressRenderer = new SimpleCellRenderer(SwingConstants.CENTER);

        String[] memoryRangeColumns = { "Description", "Lowest Address", "Highest Address" };
        this.memoryRangeData = new Object[MemoryLayout.RANGE_DESCRIPTIONS.length][3];
        for (int row = 0; row < MemoryLayout.RANGE_DESCRIPTIONS.length; row++) {
            this.memoryRangeData[row][0] = MemoryLayout.RANGE_DESCRIPTIONS[row];
        }

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
        this.memoryLocationData = new Object[MemoryLayout.LOCATION_DESCRIPTIONS.length][2];
        for (int row = 0; row < MemoryLayout.LOCATION_DESCRIPTIONS.length; row++) {
            this.memoryLocationData[row][0] = MemoryLayout.LOCATION_DESCRIPTIONS[row];
        }

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

        this.setLayoutDisplay(this.settings.memoryLayout.get());

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

    private void setLayoutDisplay(String layoutKey) {
        MemoryLayout layout = Memory.getLayoutOrDefault(layoutKey);

        for (int row = 0; row < layout.ranges.length; row++) {
            this.memoryRangeData[row][1] = Binary.intToHexString(layout.ranges[row].minAddress());
            this.memoryRangeData[row][2] = Binary.intToHexString(layout.ranges[row].maxAddress());
        }

        ((StaticTableModel) this.memoryRangeTable.getModel()).fireTableDataChanged();

        for (int row = 0; row < layout.locations.length; row++) {
            this.memoryLocationData[row][1] = Binary.intToHexString(layout.locations[row]);
        }

        ((StaticTableModel) this.memoryLocationTable.getModel()).fireTableDataChanged();
    }

    private static class MemoryLayoutButton extends JRadioButton {
        private final String key;

        public MemoryLayoutButton(String key, String displayName, boolean selected) {
            super(displayName, selected);
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }
}
