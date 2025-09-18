package mars.venus.actions.run;

import mars.Application;
import mars.assembler.log.AssemblyError;
import mars.assembler.token.SourceFile;
import mars.mips.hardware.*;
import mars.simulator.Simulator;
import mars.venus.RegistersPane;
import mars.venus.execute.ExecuteTab;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;
import mars.venus.execute.ProgramStatus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

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
 * Action for the Run -> Reset menu item.
 */
public class RunResetAction extends VenusAction {
    public RunResetAction(VenusUI gui, Integer mnemonic, KeyStroke accel) {
        super(gui, "Reset", VenusUI.getSVGActionIcon("reset.svg"), "Reset MIPS memory and registers", mnemonic, accel);
    }

    /**
     * Reset GUI components and MIPS resources.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        // The difficult part here is resetting the data segment.  Two approaches are:
        // 1. After each assembly, get a deep copy of the Globals.memory array
        //    containing data segment.  Then replace it upon reset.
        // 2. Simply re-assemble the program upon reset, and the assembler will
        //    build a new data segment.  Reset can only be done after a successful
        //    assembly, so there is "no" chance of assembler error.
        // I am choosing the second approach although it will slow down the reset
        // operation.  The first approach requires additional Memory class methods.
        try {
            // Must get a copy of the tokenized files since getTokenizedFiles() returns the underlying list
            // (which is cleared when restarting the assembly process)
            List<SourceFile> tokenizedFiles = List.copyOf(Application.assembler.getTokenizedFiles());
            Application.assembler.assembleFiles(tokenizedFiles);
        }
        catch (AssemblyError error) {
            this.gui.getMessagesPane().getMessages().writeOutput(this.getName() + ": unable to reset.  Please close file then re-open and re-assemble.\n");
            return;
        }

        Simulator.getInstance().reset();

        RegistersPane registersPane = this.gui.getRegistersPane();
        registersPane.getProcessorTab().clearHighlighting();
        registersPane.getProcessorTab().updateRegisters();
        registersPane.getCoprocessor1Tab().clearHighlighting();
        registersPane.getCoprocessor1Tab().updateRegisters();
        registersPane.getCoprocessor0Tab().clearHighlighting();
        registersPane.getCoprocessor0Tab().updateRegisters();
        registersPane.setSelectedComponent(registersPane.getProcessorTab());

        ExecuteTab executeTab = this.gui.getMainPane().getExecuteTab();
        executeTab.getDataSegmentWindow().highlightCellForAddress(Memory.getInstance().getLayout().staticRange.minAddress());
        executeTab.getDataSegmentWindow().clearHighlighting();
        executeTab.getTextSegmentWindow().resetModifiedSourceCode();
        executeTab.getTextSegmentWindow().updateHighlighting();

        this.gui.getMessagesPane().getMessages().writeOutput(this.getName() + ": reset completed.\n");
        if (this.gui.getProgramStatus() == ProgramStatus.PAUSED) {
            this.gui.getMessagesPane().getMessages().writeOutput("\n--- program terminated by user ---\n\n");
        }

        this.gui.setProgramStatus(ProgramStatus.NOT_STARTED);
    }

    @Override
    public void update() {
        this.setEnabled(this.gui.getProgramStatus().hasStarted());
    }
}