package mars.venus.preferences;

import mars.settings.Settings;

import javax.swing.*;
import java.awt.*;

public abstract class PreferencesTab extends JScrollPane {
    protected final Settings settings;
    protected final Box content;

    public PreferencesTab(Settings settings, String title) {
        this.settings = settings;
        this.setName(title);
        this.content = Box.createVerticalBox();
        Box container = Box.createVerticalBox();
        container.add(this.content);
        container.add(Box.createVerticalGlue());
        container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        this.setViewportView(container);
        this.setBorder(BorderFactory.createEmptyBorder());
    }

    public abstract void applyChanges();

    public abstract void revertChanges();

    public void addRow(JComponent value) {
        Box row = Box.createHorizontalBox();
        row.add(value);
        row.add(Box.createHorizontalGlue());
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        row.setMaximumSize(new Dimension(row.getMaximumSize().width, row.getPreferredSize().height));
        this.content.add(row);
    }

    public void addRow(String label, String toolTip, JComponent value) {
        JLabel jLabel = new JLabel(label);
        jLabel.setToolTipText(toolTip);
        jLabel.setVerticalAlignment(JLabel.TOP);
        Box row = Box.createHorizontalBox();
        row.add(jLabel);
        row.add(Box.createHorizontalStrut(12));
        row.add(value);
        row.add(Box.createHorizontalGlue());
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        row.setMaximumSize(new Dimension(row.getMaximumSize().width, row.getPreferredSize().height));
        this.content.add(row);
    }
}
