package mars.venus.preferences;

import mars.Application;
import mars.settings.Settings;
import mars.venus.VenusUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class PreferencesDialog extends JDialog {
    private final VenusUI gui;
    private final List<PreferencesTab> tabs;

    public PreferencesDialog(VenusUI gui, boolean modal) {
        super(gui, "Preferences", modal);
        this.gui = gui;
        this.tabs = new ArrayList<>();

        Settings settings = this.gui.getSettings();
        this.tabs.add(new GeneralPreferencesTab(settings));
        this.tabs.add(new AppearancePreferencesTab(settings));
        this.tabs.add(new EditorPreferencesTab(settings));
        this.tabs.add(new SimulatorPreferencesTab(settings));
        this.tabs.add(new ExceptionPreferencesTab(settings));
        this.tabs.add(new MemoryPreferencesTab(settings));

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.setContentPane(contentPane);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        for (PreferencesTab tab : this.tabs) {
            tabbedPane.add(tab);
        }
        tabbedPane.setSelectedIndex(0);
        contentPane.add(tabbedPane, BorderLayout.CENTER);

        this.buildButtonBar(contentPane);

        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                PreferencesDialog.this.closeDialog();
            }
        });
        this.pack();
        this.setSize(this.getWidth(), 600);
        this.setLocationRelativeTo(this.gui);
    }

    private void buildButtonBar(JPanel contentPane) {
        JButton revertButton = new JButton("Revert");
        revertButton.setToolTipText("Reset to initial settings without applying");
        revertButton.addActionListener(event -> {
            this.revertChanges();
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Apply current settings and close dialog");
        okButton.addActionListener(event -> {
            this.applyChanges();
            this.closeDialog();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close dialog without applying current settings");
        cancelButton.addActionListener(event -> {
            this.closeDialog();
        });

        JButton applyButton = new JButton("Apply");
        applyButton.setToolTipText("Apply current settings now and leave dialog open");
        applyButton.addActionListener(event -> {
            this.applyChanges();
        });

        Box buttonBar = Box.createHorizontalBox();
        buttonBar.setBorder(BorderFactory.createEmptyBorder(12, 6, 6, 6));
        buttonBar.add(revertButton);
        buttonBar.add(Box.createHorizontalGlue());
        buttonBar.add(Box.createHorizontalStrut(12));
        buttonBar.add(okButton);
        buttonBar.add(Box.createHorizontalStrut(12));
        buttonBar.add(cancelButton);
        buttonBar.add(Box.createHorizontalStrut(12));
        buttonBar.add(applyButton);

        contentPane.add(buttonBar, BorderLayout.SOUTH);
        contentPane.getRootPane().setDefaultButton(okButton);
    }

    public void closeDialog() {
        this.setVisible(false);
        this.dispose();
    }

    public void applyChanges() {
        for (PreferencesTab tab : this.tabs) {
            tab.applyChanges();
        }
        // Update, well, everything
        // TODO: maybe find a way to do this on an as-needed basis
        this.gui.getSettings().dispatchChangedEvent();
        Application.setupLookAndFeel();
        SwingUtilities.updateComponentTreeUI(this);
    }

    public void revertChanges() {
        for (PreferencesTab tab : this.tabs) {
            tab.revertChanges();
        }
    }
}
