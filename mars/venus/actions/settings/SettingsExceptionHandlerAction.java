package mars.venus.actions.settings;

import mars.Globals;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Action class for the Settings menu item for optionally loading a MIPS exception handler.
 */
public class SettingsExceptionHandlerAction extends VenusAction {
    JDialog exceptionHandlerDialog;
    JCheckBox exceptionHandlerSetting;
    JButton exceptionHandlerSelectionButton;
    JTextField exceptionHandlerDisplay;

    boolean initialSelected; // state of check box when dialog initiated.
    String initialPathname;  // selected exception handler when dialog initiated.

    public SettingsExceptionHandlerAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    // launch dialog for setting and filename specification
    @Override
    public void actionPerformed(ActionEvent event) {
        initialSelected = Globals.getSettings().exceptionHandlerEnabled.get();
        initialPathname = Globals.getSettings().exceptionHandlerPath.get();
        exceptionHandlerDialog = new JDialog(Globals.getGUI(), "Exception Handler", true);
        exceptionHandlerDialog.setContentPane(buildDialogPanel());
        exceptionHandlerDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        exceptionHandlerDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                closeDialog();
            }
        });
        exceptionHandlerDialog.pack();
        exceptionHandlerDialog.setLocationRelativeTo(Globals.getGUI());
        exceptionHandlerDialog.setVisible(true);
    }

    // The dialog box that appears when menu item is selected.
    private JPanel buildDialogPanel() {
        JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        // Top row - the check box for setting...
        exceptionHandlerSetting = new JCheckBox("Include this exception handler file in all assemble operations");
        exceptionHandlerSetting.setSelected(Globals.getSettings().exceptionHandlerEnabled.get());
        exceptionHandlerSetting.addActionListener(new ExceptionHandlerSettingAction());
        contents.add(exceptionHandlerSetting, BorderLayout.NORTH);
        // Middle row - the button and text field for exception handler file selection
        JPanel specifyHandlerFile = new JPanel();
        exceptionHandlerSelectionButton = new JButton("Browse");
        exceptionHandlerSelectionButton.setEnabled(exceptionHandlerSetting.isSelected());
        exceptionHandlerSelectionButton.addActionListener(new ExceptionHandlerSelectionAction());
        exceptionHandlerDisplay = new JTextField(Globals.getSettings().exceptionHandlerPath.get(), 30);
        exceptionHandlerDisplay.setEditable(false);
        exceptionHandlerDisplay.setEnabled(exceptionHandlerSetting.isSelected());
        specifyHandlerFile.add(exceptionHandlerSelectionButton);
        specifyHandlerFile.add(exceptionHandlerDisplay);
        contents.add(specifyHandlerFile, BorderLayout.CENTER);
        // Bottom row - the control buttons for OK and Cancel
        Box controlPanel = Box.createHorizontalBox();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            performOK();
            closeDialog();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> closeDialog());
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(okButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());
        contents.add(controlPanel, BorderLayout.SOUTH);
        return contents;
    }

    // User has clicked "OK" button, so record status of the checkbox and text field.
    private void performOK() {
        boolean finalSelected = exceptionHandlerSetting.isSelected();
        String finalPathname = exceptionHandlerDisplay.getText();
        // If nothing has changed then don't modify setting variables or properties file.
        if (initialSelected != finalSelected || initialPathname == null && finalPathname != null || initialPathname != null && !initialPathname.equals(finalPathname)) {
            Globals.getSettings().exceptionHandlerEnabled.set(finalSelected);
            if (finalSelected) {
                Globals.getSettings().exceptionHandlerPath.set(finalPathname);
            }
        }
    }

    // We're finished with this modal dialog.
    private void closeDialog() {
        exceptionHandlerDialog.setVisible(false);
        exceptionHandlerDialog.dispose();
    }

    /////////////////////////////////////////////////////////////////////////////////
    // Associated action class: exception handler setting.  Attached to check box.
    private class ExceptionHandlerSettingAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean selected = ((JCheckBox) e.getSource()).isSelected();
            exceptionHandlerSelectionButton.setEnabled(selected);
            exceptionHandlerDisplay.setEnabled(selected);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    // Associated action class: selecting exception handler file.  Attached to handler selector.
    private class ExceptionHandlerSelectionAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            String pathname = Globals.getSettings().exceptionHandlerPath.get();
            if (pathname != null) {
                File file = new File(pathname);
                if (file.exists()) {
                    chooser.setSelectedFile(file);
                }
            }
            int result = chooser.showOpenDialog(Globals.getGUI());
            if (result == JFileChooser.APPROVE_OPTION) {
                pathname = chooser.getSelectedFile().getPath();
                exceptionHandlerDisplay.setText(pathname);
            }
        }
    }
}