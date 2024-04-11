package mars.venus.actions.run;

import mars.Application;
import mars.ProcessingException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.venus.execute.ExecutePane;
import mars.venus.FileStatus;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;

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
 * Action for the Run -> Reset menu item
 */
public class RunResetAction extends VenusAction {
    public RunResetAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
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
            Application.program.assemble(RunAssembleAction.getProgramsToAssemble(), Application.getSettings().extendedAssemblerEnabled.get(), Application.getSettings().warningsAreErrors.get());
        }
        catch (ProcessingException pe) {
            gui.getMessagesPane().writeToMessages(getName() + ": unable to reset.  Please close file then re-open and re-assemble.\n");
            return;
        }

        RegisterFile.resetRegisters();
        Coprocessor1.resetRegisters();
        Coprocessor0.resetRegisters();

        ExecutePane executePane = gui.getMainPane().getExecutePane();
        executePane.getRegistersWindow().clearHighlighting();
        executePane.getRegistersWindow().updateRegisters();
        executePane.getCoprocessor1Window().clearHighlighting();
        executePane.getCoprocessor1Window().updateRegisters();
        executePane.getCoprocessor0Window().clearHighlighting();
        executePane.getCoprocessor0Window().updateRegisters();
        executePane.getDataSegmentWindow().highlightCellForAddress(Memory.dataBaseAddress);
        executePane.getDataSegmentWindow().clearHighlighting();
        executePane.getTextSegmentWindow().resetModifiedSourceCode();
        executePane.getTextSegmentWindow().setCodeHighlighting(true);
        executePane.getTextSegmentWindow().highlightStepAtPC();

        gui.getRegistersPane().setSelectedComponent(executePane.getRegistersWindow());

        FileStatus.set(FileStatus.RUNNABLE);
        VenusUI.setReset(true);
        VenusUI.setStarted(false);

        gui.getMessagesPane().writeToConsole(getName() + ": reset completed.\n\n");
    }
}