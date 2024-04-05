package mars.venus.actions.run;

import mars.Globals;
import mars.ProcessingException;
import mars.simulator.ProgramArgumentList;
import mars.venus.FileStatus;
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
 * Action for the Run -> Go menu item
 */
public class RunStartAction extends VenusAction {
    public RunStartAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    /**
     * Run the MIPS program.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (FileStatus.isAssembled()) {
            if (!VenusUI.getStarted()) {
                // DPS 17-July-2008
                // Store any program arguments into MIPS memory and registers before
                // execution begins. Arguments go into the gap between $sp and kernel memory.
                // Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
                // $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
                String programArguments = gui.getMainPane().getExecutePane().getTextSegmentWindow().getProgramArguments();
                if (programArguments != null && !programArguments.isEmpty() && Globals.getSettings().useProgramArguments.get()) {
                    new ProgramArgumentList(programArguments).storeProgramArguments();
                }
            }

            Globals.getGUI().getMainPane().getExecutePane().getTextSegmentWindow().setCodeHighlighting(false);
            Globals.getGUI().getMainPane().getExecutePane().getTextSegmentWindow().unhighlightAllSteps();

            try {
                int[] breakPoints = gui.getMainPane().getExecutePane().getTextSegmentWindow().getSortedBreakPointsArray();
                Globals.program.simulateFromPC(breakPoints, -1, this);
            }
            catch (ProcessingException e) {
                // Ignore
            }
        }
        else {
            // Note: this should never occur since the Start action is only enabled after successful assembly.
            JOptionPane.showMessageDialog(gui, "The program must be assembled before it can be run.");
        }
    }
}