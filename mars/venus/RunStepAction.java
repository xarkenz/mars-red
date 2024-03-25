package mars.venus;

import mars.Globals;
import mars.ProcessingException;
import mars.Settings;
import mars.mips.hardware.RegisterFile;
import mars.simulator.ProgramArgumentList;
import mars.simulator.Simulator;

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
 * Action for the Run -> Step menu item
 */
public class RunStepAction extends GuiAction {
    private String name;
    private ExecutePane executePane;

    public RunStepAction(String name, Icon icon, String description, Integer mnemonic, KeyStroke accel, VenusUI gui) {
        super(name, icon, description, mnemonic, accel, gui);
    }

    /**
     * Perform next simulated instruction step.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        name = this.getValue(Action.NAME).toString();
        executePane = gui.getMainPane().getExecutePane();
        if (FileStatus.isAssembled()) {
            if (!VenusUI.getStarted()) {  // DPS 17-July-2008
                processProgramArgumentsIfAny();
            }
            VenusUI.setStarted(true);
            gui.messagesPane.setSelectedComponent(gui.messagesPane.runTab);
            executePane.getTextSegmentWindow().setCodeHighlighting(true);
            try {
                Globals.program.simulateStepAtPC(this);
            }
            catch (ProcessingException ev) {
                // Ignore
            }
        }
        else {
            // note: this should never occur since "Step" is only enabled after successful assembly.
            JOptionPane.showMessageDialog(gui, "The program must be assembled before it can be run.");
        }
    }

    /**
     * When step is completed, control returns here (from execution thread, indirectly)
     * to update the GUI.
     */
    public void stepped(boolean done, int reason, ProcessingException pe) {
        executePane.getRegistersWindow().updateRegisters();
        executePane.getCoprocessor1Window().updateRegisters();
        executePane.getCoprocessor0Window().updateRegisters();
        executePane.getDataSegmentWindow().updateValues();
        if (!done) {
            executePane.getTextSegmentWindow().highlightStepAtPC();
            FileStatus.set(FileStatus.RUNNABLE);
        }
        if (done) {
            RunGoAction.resetMaxSteps();
            executePane.getTextSegmentWindow().unhighlightAllSteps();
            FileStatus.set(FileStatus.TERMINATED);
        }
        if (done && pe == null) {
            gui.getMessagesPane().postMarsMessage("\n" + name + ": execution " + ((reason == Simulator.CLIFF_TERMINATION) ? "terminated due to null instruction." : "completed successfully.") + "\n\n");
            gui.getMessagesPane().postRunMessage("\n-- Program finished " + ((reason == Simulator.CLIFF_TERMINATION) ? "(exited without syscall)" : "") + " --\n\n");
            gui.getMessagesPane().selectRunMessageTab();
            if (reason == Simulator.CLIFF_TERMINATION) {
                executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
            }
        }
        if (pe != null) {
            RunGoAction.resetMaxSteps();
            gui.getMessagesPane().postMarsMessage(pe.errors().generateErrorReport());
            gui.getMessagesPane().postMarsMessage("\n" + name + ": execution terminated with errors.\n\n");
            gui.getRegistersPane().setSelectedComponent(executePane.getCoprocessor0Window());
            FileStatus.set(FileStatus.TERMINATED); // Should be redundant
            executePane.getTextSegmentWindow().setCodeHighlighting(true);
            executePane.getTextSegmentWindow().unhighlightAllSteps();
            executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
        }
        VenusUI.setReset(false);
    }

    /**
     * Method to store any program arguments into MIPS memory and registers before
     * execution begins. Arguments go into the gap between $sp and kernel memory.
     * Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
     * $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
     */
    private void processProgramArgumentsIfAny() {
        String programArguments = executePane.getTextSegmentWindow().getProgramArguments();
        if (programArguments == null || programArguments.isEmpty() || !Globals.getSettings().getBoolean(Settings.PROGRAM_ARGUMENTS)) {
            return;
        }
        new ProgramArgumentList(programArguments).storeProgramArguments();
    }
}