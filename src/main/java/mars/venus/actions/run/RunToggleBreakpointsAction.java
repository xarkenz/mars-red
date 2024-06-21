package mars.venus.actions.run;

import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;

import javax.swing.*;
import java.awt.event.ActionEvent;

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
 * Action for the Run -> Toggle Breakpoints menu item.
 */
public class RunToggleBreakpointsAction extends VenusAction {
    public RunToggleBreakpointsAction(VenusUI gui, Integer mnemonic, KeyStroke accel) {
        super(gui, "Toggle All Breakpoints", null, "Disable/enable all breakpoints without clearing (can also click Bkpt column header)", mnemonic, accel);
    }

    /**
     * When this option is selected, tell text segment window to toggle breakpoints in its table model.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        this.gui.getMainPane().getExecuteTab().getTextSegmentWindow().toggleBreakpoints();
    }
    
    @Override
    public void update() {
        this.setEnabled(this.gui.getProgramStatus().isRunnable());
    }
}