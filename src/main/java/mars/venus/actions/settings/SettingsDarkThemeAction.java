package mars.venus.actions.settings;

import mars.Application;
import mars.venus.VenusUI;
import mars.venus.actions.VenusAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Action class for the Settings menu item to control whether to use
 * the light theme or dark theme. Temporary until a new settings menu
 * is implemented.
 */
public class SettingsDarkThemeAction extends VenusAction {
    public SettingsDarkThemeAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (((JCheckBoxMenuItem) event.getSource()).isSelected()) {
            Application.getSettings().lookAndFeelName.set("FlatDarkLaf");
        }
        else {
            Application.getSettings().lookAndFeelName.set("FlatLightLaf");
        }

        Application.setupLookAndFeel();

        // For now, the UI seems to update properly, so this message isn't really necessary
        // JOptionPane.showMessageDialog(gui, "For best results, you may want to restart " + Application.NAME + ".", "Restart Recommended", JOptionPane.INFORMATION_MESSAGE);
    }
}