package mars.venus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PreferencesDialog extends JDialog {
    public PreferencesDialog(Frame owner, String title, boolean modality) {
        super(owner, title, modality);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.setContentPane(contentPane);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addTab("General", new JPanel());
        tabbedPane.addTab("Appearance", new JPanel());
        tabbedPane.addTab("Editor", new JPanel());
        tabbedPane.addTab("Simulator", new JPanel());
        tabbedPane.addTab("Exception Handler", new JPanel());
        tabbedPane.addTab("Memory Layout", new JPanel());
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
        this.setSize(700, 500);
//        this.pack();
        this.setLocationRelativeTo(owner);
    }

    private void buildButtonBar(JPanel contentPane) {
        JButton revertButton = new JButton("Revert");
        revertButton.setToolTipText("Reset to initial settings without applying");
        revertButton.addActionListener(event -> {
            revertChanges();
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Apply current settings and close dialog");
        okButton.addActionListener(event -> {
            applyChanges();
            closeDialog();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close dialog without applying current settings");
        cancelButton.addActionListener(event -> {
            closeDialog();
        });

        JButton applyButton = new JButton("Apply");
        applyButton.setToolTipText("Apply current settings now and leave dialog open");
        applyButton.addActionListener(event -> {
            applyChanges();
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
        //
    }

    public void revertChanges() {
        //
    }
}
