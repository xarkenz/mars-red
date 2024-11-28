package mars.venus.actions.run;

import mars.simulator.Simulator;
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
 * Action for the Run -> Step Backward menu item.
 */
public class RunStepBackwardAction extends VenusAction {
    public RunStepBackwardAction(VenusUI gui, Integer mnemonic, KeyStroke accel) {
        super(gui, "Step Backward", VenusUI.getSVGActionIcon("step_backward.svg"), "Undo the last step", mnemonic, accel);
    }

    /**
     * Undo the previous simulation step.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        this.gui.getMessagesPane().selectConsoleTab();

        RegistersPane registersPane = this.gui.getRegistersPane();
        ExecuteTab executeTab = this.gui.getMainPane().getExecuteTab();

        executeTab.getDataSegmentWindow().startObservingMemory();
        registersPane.getProcessorTab().startObservingRegisters();
        registersPane.getCoprocessor0Tab().startObservingRegisters();
        registersPane.getCoprocessor1Tab().startObservingRegisters();

        Simulator.getInstance().getBackStepper().backStep();

        executeTab.getDataSegmentWindow().stopObservingMemory();
        registersPane.getProcessorTab().stopObservingRegisters();
        registersPane.getCoprocessor0Tab().stopObservingRegisters();
        registersPane.getCoprocessor1Tab().stopObservingRegisters();
        registersPane.getProcessorTab().updateRegisters();
        registersPane.getCoprocessor1Tab().updateRegisters();
        registersPane.getCoprocessor0Tab().updateRegisters();
        executeTab.getDataSegmentWindow().updateValues();
        executeTab.getTextSegmentWindow().updateHighlighting();

        this.gui.setProgramStatus(ProgramStatus.PAUSED);
    }

    @Override
    public void update() {
        this.setEnabled(this.gui.getProgramStatus().isRunnable()
            && Simulator.getInstance().getBackStepper().isEnabled()
            && !Simulator.getInstance().getBackStepper().isEmpty());
    }
}