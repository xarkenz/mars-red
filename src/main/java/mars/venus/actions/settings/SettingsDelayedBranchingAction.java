package mars.venus.actions.settings;

import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;

import javax.swing.*;
import java.awt.event.ActionEvent;

/*
Copyright (c) 2003-2007,  Pete Sanderson and Kenneth Vollmar

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
 * Action class for the Settings menu item to control delayed branching.
 * Note: Changing this setting while the current program is runnable
 * (assembled, or stepped execution) or terminated triggers a re-assembly.
 * This is necessary to maintain consistency because the machine
 * code assembled for branch instructions differs depending on
 * this setting -- would branch to incorrect address if setting
 * were changed between assembly and execution.
 * Note: This action is disabled while the MIPS program is running.
 * The user need only pause or stop execution to re-enable it.
 */
public class SettingsDelayedBranchingAction extends VenusAction {
    public SettingsDelayedBranchingAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        this.gui.getSettings().delayedBranchingEnabled.set(((JCheckBoxMenuItem) event.getSource()).isSelected());
    }
}