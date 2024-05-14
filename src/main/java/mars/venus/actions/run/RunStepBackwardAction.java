package mars.venus.actions.run;

import mars.Application;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.venus.RegistersPane;
import mars.venus.execute.ExecuteTab;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;
import mars.venus.execute.ProgramStatus;

import javax.swing.*;
import java.awt.event.ActionEvent;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Run -> Backstep menu item
 */
public class RunStepBackwardAction extends VenusAction {
    public RunStepBackwardAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    /**
     * Perform the next simulated instruction step.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        gui.getMessagesPane().selectConsoleTab();
        RegistersPane registersPane = gui.getRegistersPane();
        ExecuteTab executeTab = gui.getMainPane().getExecuteTab();
        executeTab.getTextSegmentWindow().setCodeHighlighting(true);

        if (Application.isBackSteppingEnabled()) {
            boolean inDelaySlot = Application.program.getBackStepper().isInDelaySlot(); // Added 25 June 2007

            Memory.getInstance().addObserver(executeTab.getDataSegmentWindow());
            registersPane.getRegistersWindow().startObservingRegisters();
            registersPane.getCoprocessor0Window().startObservingRegisters();
            registersPane.getCoprocessor1Window().startObservingRegisters();

            Application.program.getBackStepper().backStep();

            Memory.getInstance().deleteObserver(executeTab.getDataSegmentWindow());
            registersPane.getRegistersWindow().stopObservingRegisters();
            registersPane.getCoprocessor0Window().stopObservingRegisters();
            registersPane.getCoprocessor1Window().stopObservingRegisters();
            registersPane.getRegistersWindow().updateRegisters();
            registersPane.getCoprocessor1Window().updateRegisters();
            registersPane.getCoprocessor0Window().updateRegisters();
            executeTab.getDataSegmentWindow().updateValues();
            executeTab.getTextSegmentWindow().highlightStepAtPC(inDelaySlot); // Argument added 25 June 2007

            executeTab.setProgramStatus(ProgramStatus.PAUSED);
        }
    }
}