package mars.venus.actions.run;

import mars.Globals;
import mars.ProcessingException;
import mars.settings.Settings;
import mars.mips.hardware.RegisterFile;
import mars.simulator.ProgramArgumentList;
import mars.simulator.Simulator;
import mars.util.SystemIO;
import mars.venus.ExecutePane;
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
public class RunGoAction extends VenusAction {
    public static final int DEFAULT_MAX_STEPS = -1; // "forever"
    public static int maxSteps = DEFAULT_MAX_STEPS;

    private String name;
    private ExecutePane executePane;

    public RunGoAction(String name, Icon icon, String description, Integer mnemonic, KeyStroke accel, VenusUI gui) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    /**
     * Run the MIPS program.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        name = this.getValue(Action.NAME).toString();
        executePane = gui.getMainPane().getExecutePane();
        if (FileStatus.isAssembled()) {
            if (!VenusUI.getStarted()) {
                processProgramArgumentsIfAny(); // DPS 17-July-2008
            }
            if (VenusUI.getReset() || VenusUI.getStarted()) {
                VenusUI.setStarted(true); // added 8/27/05

                gui.getMessagesPane().postMarsMessage(name + ": running " + FileStatus.getFile().getName() + "\n\n");
                gui.getMessagesPane().selectRunMessageTab();
                executePane.getTextSegmentWindow().setCodeHighlighting(false);
                executePane.getTextSegmentWindow().unhighlightAllSteps();
                gui.setMenuState(FileStatus.RUNNING);
                try {
                    int[] breakPoints = executePane.getTextSegmentWindow().getSortedBreakPointsArray();
                    Globals.program.simulateFromPC(breakPoints, maxSteps, this);
                }
                catch (ProcessingException pe) {
                    // Ignore
                }
            }
            else {
                // This should never occur because at termination the Go and Step buttons are disabled.
                JOptionPane.showMessageDialog(gui, "reset " + VenusUI.getReset() + " started " + VenusUI.getStarted());
            }
        }
        else {
            // note: this should never occur since "Go" is only enabled after successful assembly.
            JOptionPane.showMessageDialog(gui, "The program must be assembled before it can be run.");
        }
    }

    /**
     * Method to be called when Pause is selected through menu/toolbar/shortcut.  This should only
     * happen when MIPS program is running (FileStatus.RUNNING).  See VenusUI.java for enabled
     * status of menu items based on FileStatus.  Set GUI as if at breakpoint or executing
     * step by step.
     */
    public void paused(boolean done, int pauseReason, ProcessingException pe) {
        // I doubt this can happen (pause when execution finished), but if so treat it as stopped.
        if (done) {
            stopped(pe, Simulator.NORMAL_TERMINATION);
            return;
        }
        if (pauseReason == Simulator.BREAKPOINT) {
            gui.getMessagesPane().postMarsMessage(name + ": execution paused at breakpoint: " + FileStatus.getFile().getName() + "\n\n");
        }
        else {
            gui.getMessagesPane().postMarsMessage(name + ": execution paused by user: " + FileStatus.getFile().getName() + "\n\n");
        }
        gui.getMessagesPane().selectMarsMessageTab();
        executePane.getTextSegmentWindow().setCodeHighlighting(true);
        executePane.getTextSegmentWindow().highlightStepAtPC();
        executePane.getRegistersWindow().updateRegisters();
        executePane.getCoprocessor1Window().updateRegisters();
        executePane.getCoprocessor0Window().updateRegisters();
        executePane.getDataSegmentWindow().updateValues();
        FileStatus.set(FileStatus.RUNNABLE);
        VenusUI.setReset(false);
    }

    /**
     * Method to be called when Stop is selected through menu/toolbar/shortcut.  This should only
     * happen when MIPS program is running (FileStatus.RUNNING).  See VenusUI.java for enabled
     * status of menu items based on FileStatus.  Display finalized values as if execution
     * terminated due to completion or exception.
     */
    public void stopped(ProcessingException pe, int reason) {
        // Show final register and data segment values
        executePane.getRegistersWindow().updateRegisters();
        executePane.getCoprocessor1Window().updateRegisters();
        executePane.getCoprocessor0Window().updateRegisters();
        executePane.getDataSegmentWindow().updateValues();
        FileStatus.set(FileStatus.TERMINATED);
        SystemIO.resetFiles(); // close any files opened in MIPS program
        // Bring coprocessor 0 to the front if terminated due to exception.
        if (pe != null) {
            gui.getRegistersPane().setSelectedComponent(executePane.getCoprocessor0Window());
            executePane.getTextSegmentWindow().setCodeHighlighting(true);
            executePane.getTextSegmentWindow().unhighlightAllSteps();
            executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
        }
        switch (reason) {
            case Simulator.NORMAL_TERMINATION:
                gui.getMessagesPane().postMarsMessage("\n" + name + ": execution completed successfully.\n\n");
                gui.getMessagesPane().postRunMessage("\n-- program is finished running --\n\n");
                gui.getMessagesPane().selectRunMessageTab();
                break;
            case Simulator.CLIFF_TERMINATION:
                gui.getMessagesPane().postMarsMessage("\n" + name + ": execution terminated by null instruction.\n\n");
                gui.getMessagesPane().postRunMessage("\n-- program is finished running (dropped off bottom) --\n\n");
                gui.getMessagesPane().selectRunMessageTab();
                break;
            case Simulator.EXCEPTION:
                gui.getMessagesPane().postMarsMessage(pe.errors().generateErrorReport());
                gui.getMessagesPane().postMarsMessage("\n" + name + ": execution terminated with errors.\n\n");
                break;
            case Simulator.PAUSE_OR_STOP:
                gui.getMessagesPane().postMarsMessage("\n" + name + ": execution terminated by user.\n\n");
                gui.getMessagesPane().selectMarsMessageTab();
                break;
            case Simulator.MAX_STEPS:
                gui.getMessagesPane().postMarsMessage("\n" + name + ": execution step limit of " + maxSteps + " exceeded.\n\n");
                gui.getMessagesPane().selectMarsMessageTab();
                break;
            case Simulator.BREAKPOINT: // should never get here
                break;
        }
        RunGoAction.resetMaxSteps();
        VenusUI.setReset(false);
    }

    /**
     * Reset max steps limit to default value at termination of a simulated execution.
     */
    public static void resetMaxSteps() {
        maxSteps = DEFAULT_MAX_STEPS;
    }

    /**
     * Method to store any program arguments into MIPS memory and registers before
     * execution begins. Arguments go into the gap between $sp and kernel memory.
     * Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
     * $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
     */
    private void processProgramArgumentsIfAny() {
        String programArguments = executePane.getTextSegmentWindow().getProgramArguments();
        if (programArguments == null || programArguments.isEmpty() || !Globals.getSettings().useProgramArguments.get()) {
            return;
        }
        new ProgramArgumentList(programArguments).storeProgramArguments();
    }
}