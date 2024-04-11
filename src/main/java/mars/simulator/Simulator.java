package mars.simulator;

import mars.*;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.mips.instructions.BasicInstruction;
import mars.util.Binary;
import mars.venus.*;
import mars.venus.actions.VenusAction;
import mars.venus.actions.run.RunStartAction;
import mars.venus.actions.run.RunPauseAction;
import mars.venus.actions.run.RunStepForwardAction;
import mars.venus.actions.run.RunStopAction;
import mars.venus.execute.ExecutePane;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;

/*
Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

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
 * Used to simulate the execution of an assembled MIPS program.
 *
 * @author Pete Sanderson
 * @version August 2005
 */
public class Simulator extends Observable {
    public static final String NAME = "Simulator";

    private static Simulator instance = null;
    private static Runnable interactiveGUIUpdater = null;
    // Others can set this true to indicate external interrupt.  Initially used
    // to simulate keyboard and display interrupts.  The device is identified
    // by the address of its MMIO control register.  keyboard 0xFFFF0000 and
    // display 0xFFFF0008.  DPS 23 July 2008.
    public static final int NO_DEVICE = 0;
    public static volatile int externalInterruptingDevice = NO_DEVICE;

    private SimulatorThread simulatorThread;
    private SystemIO systemIO;

    /**
     * Enumeration of reasons for the simulation to stop. "Stop" in this context
     * could mean either terminate or pause.
     */
    public enum StopReason {
        /**
         * A breakpoint was reached, causing execution to pause.
         */
        BREAKPOINT,
        /**
         * An exception occurred, causing the program to terminate.
         */
        EXCEPTION,
        /**
         * The requested number of steps have completed successfully, causing execution to pause.
         * This is also used when stepping one step at a time.
         */
        STEP_LIMIT_REACHED,
        /**
         * One of the exit syscalls was invoked, causing the program to terminate.
         */
        EXIT_SYSCALL,
        /**
         * A null instruction was reached (the program counter ran off the bottom),
         * causing the program to terminate.
         */
        RAN_OFF_BOTTOM,
        /**
         * Execution was stopped by something outside of the Simulator.
         * This is usually caused by the Pause/Stop actions.
         */
        EXTERNAL,
    }

    /**
     * Returns the singleton instance of the MIPS simulator.
     *
     * @return The Simulator object in use.
     */
    public static Simulator getInstance() {
        // Do NOT change this to create the Simulator at load time (in declaration above)!
        // Its constructor looks for the GUI, which at load time is not created yet,
        // and incorrectly leaves interactiveGUIUpdater null!  This causes runtime
        // exceptions while running in timed mode.
        if (instance == null) {
            instance = new Simulator();
        }
        return instance;
    }

    private Simulator() {
        simulatorThread = null;
        systemIO = new SystemIO();
        if (Application.getGUI() != null) {
            interactiveGUIUpdater = new UpdateGUI();
        }
    }

    public SystemIO getSystemIO() {
        return systemIO;
    }

    /**
     * Determine whether or not the next instruction to be executed is in a
     * "delay slot".  This means delayed branching is enabled, the branch
     * condition has evaluated true, and the next instruction executed will
     * be the one following the branch.  It is said to occupy the "delay slot."
     * Normally programmers put a nop instruction here but it can be anything.
     *
     * @return true if next instruction is in delay slot, false otherwise.
     */
    public static boolean isInDelaySlot() {
        return DelayedBranch.isTriggered();
    }

    /**
     * Simulate execution of given MIPS program.  It must have already been assembled.
     *
     * @param program        The program to be simulated.
     * @param programCounter Address of first instruction to simulate; this goes into program counter
     * @param maxSteps       Maximum number of steps to perform before returning false (0 or less means no max)
     * @param breakpoints    Array of breakpoint program counter values, use null if none
     * @param actor          The GUI component responsible for this call, usually GO or STEP.  null if none.
     * @return true if execution completed, false otherwise
     * @throws ProcessingException Throws exception if run-time exception occurs.
     */
    public boolean simulate(Program program, int programCounter, int maxSteps, int[] breakpoints, VenusAction actor) throws ProcessingException {
        simulatorThread = new SimulatorThread(this, program, programCounter, maxSteps, breakpoints, actor);
        simulatorThread.start();

        if (actor == null) {
            // The simulator was run from command-line instead of GUI.
            // So, just stick around until execution thread is finished.
            simulatorThread.get(); // This should emulate join()
            ProcessingException exception = simulatorThread.exception;
            boolean isFinished = simulatorThread.isFinished;
            simulatorThread = null;
            if (exception != null) {
                throw exception;
            }
            return isFinished;
        }
        else {
            return true;
        }
    }

    /**
     * Set the volatile stop boolean variable checked by the execution
     * thread at the end of each MIPS instruction execution.  If variable
     * is found to be true, the execution thread will depart
     * gracefully so the main thread handling the GUI can take over.
     * This is used by both STOP and PAUSE features.
     */
    public void stop(VenusAction actor) {
        if (simulatorThread != null) {
            simulatorThread.stop(actor);
            simulatorThread = null;
        }
    }

    private final ArrayList<SimulatorListener> listeners = new ArrayList<>();

    public void addListener(SimulatorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SimulatorListener listener) {
        listeners.remove(listener);
    }

    // The simulator thread will call these methods when it enters and returns from
    // its construct() method.  These signal start and stop, respectively, of
    // simulation execution.  The observer can then adjust its own state depending
    // on the execution state.  Note that "stop" and "finish" are not the same thing.
    // "stop" just means it is leaving execution state; this could be triggered
    // by Stop button, by Pause button, by Step button, by runtime exception, by
    // instruction count limit, by breakpoint, or by an exit syscall.

    private void dispatchStartedUpdate(int maxSteps, int programCounter) {
        VenusUI.setStarted(true); // added 8/27/05

        Application.getGUI().getMessagesPane().writeToMessages(NAME + ": started simulation.\n\n");
        Application.getGUI().getMessagesPane().selectConsoleTab();
        Application.getGUI().setMenuState(FileStatus.RUNNING);

        this.setChanged();
        this.notifyObservers(new SimulatorNotice(SimulatorNotice.SIMULATOR_START, maxSteps, RunSpeedPanel.getInstance().getRunSpeed(), programCounter));

        for (SimulatorListener listener : listeners) {
            listener.started(maxSteps, programCounter);
        }
    }

    /**
     * Called when simulation stops but is not finished, and updates the GUI.
     * This should only happen when MIPS program is running (FileStatus.RUNNING).
     * See {@link VenusUI} for enabled status of menu items based on FileStatus.
     */
    public void dispatchPausedUpdate(int maxSteps, int programCounter, Simulator.StopReason reason) {
        this.setChanged();
        this.notifyObservers(new SimulatorNotice(SimulatorNotice.SIMULATOR_STOP, maxSteps, RunSpeedPanel.getInstance().getRunSpeed(), programCounter));

        MessagesPane messagesPane = Application.getGUI().getMessagesPane();
        switch (reason) {
            case BREAKPOINT -> {
                messagesPane.writeToMessages(NAME + ": paused simulation at breakpoint.\n\n");
                messagesPane.selectMessagesTab();
            }
            case EXTERNAL -> {
                messagesPane.writeToMessages(NAME + ": paused simulation.\n\n");
            }
        }

        ExecutePane executePane = Application.getGUI().getMainPane().getExecutePane();
        // Update register and data segment values
        executePane.getRegistersWindow().updateRegisters();
        executePane.getCoprocessor1Window().updateRegisters();
        executePane.getCoprocessor0Window().updateRegisters();
        executePane.getDataSegmentWindow().updateValues();
        // Highlight last executed instruction in text segment
        executePane.getTextSegmentWindow().setCodeHighlighting(true);
        executePane.getTextSegmentWindow().unhighlightAllSteps();
        executePane.getTextSegmentWindow().highlightStepAtPC();

        FileStatus.set(FileStatus.RUNNABLE);
        VenusUI.setReset(false);

        for (SimulatorListener listener : listeners) {
            listener.paused(maxSteps, programCounter, reason);
        }
    }

    /**
     * Called when simulation stops after finishing, and updates the GUI.
     * This should only happen when MIPS program is running (FileStatus.RUNNING).
     * See {@link VenusUI} for enabled status of menu items based on FileStatus.
     */
    private void dispatchFinishedUpdate(int maxSteps, int programCounter, StopReason reason, ProcessingException exception) {
        this.setChanged();
        this.notifyObservers(new SimulatorNotice(SimulatorNotice.SIMULATOR_STOP, maxSteps, RunSpeedPanel.getInstance().getRunSpeed(), programCounter));

        ExecutePane executePane = Application.getGUI().getMainPane().getExecutePane();
        // Update register and data segment values
        executePane.getRegistersWindow().updateRegisters();
        executePane.getCoprocessor1Window().updateRegisters();
        executePane.getCoprocessor0Window().updateRegisters();
        executePane.getDataSegmentWindow().updateValues();
        // Highlight last executed instruction in text segment
        executePane.getTextSegmentWindow().setCodeHighlighting(true);
        executePane.getTextSegmentWindow().unhighlightAllSteps();
        executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - BasicInstruction.INSTRUCTION_LENGTH_BYTES);

        // Bring Coprocessor 0 to the front if terminated due to exception
        if (exception != null) {
            Application.getGUI().getRegistersPane().setSelectedComponent(executePane.getCoprocessor0Window());
        }

        MessagesPane messagesPane = Application.getGUI().getMessagesPane();
        switch (reason) {
            case EXIT_SYSCALL -> {
                messagesPane.writeToMessages(NAME + ": finished simulation successfully.\n\n");
                messagesPane.writeToConsole("\n--- program finished ---\n\n");
                messagesPane.selectConsoleTab();
            }
            case RAN_OFF_BOTTOM -> {
                messagesPane.writeToMessages(NAME + ": finished simulation due to null instruction.\n\n");
                messagesPane.writeToConsole("\n--- program automatically terminated (ran off bottom) ---\n\n");
                messagesPane.selectConsoleTab();
            }
            case EXCEPTION -> {
                messagesPane.writeToMessages(NAME + ": finished simulation with errors.\n\n");
                messagesPane.writeToConsole("\n--- program terminated due to error(s): ---\n");
                messagesPane.writeToConsole(exception.errors().generateErrorReport());
                messagesPane.writeToConsole("--- end of error report ---\n\n");
                messagesPane.selectConsoleTab();
            }
            case STEP_LIMIT_REACHED -> {
                messagesPane.writeToMessages(NAME + ": paused simulation after " + maxSteps + " step(s).\n\n");
            }
            case EXTERNAL -> {
                messagesPane.writeToMessages(NAME + ": stopped simulation.\n\n");
            }
        }

        FileStatus.set(FileStatus.TERMINATED);
        VenusUI.setReset(false);

        // Close any unclosed file descriptors opened in execution of program
        systemIO.resetFiles();

        for (SimulatorListener listener : listeners) {
            listener.finished(maxSteps, programCounter, reason, exception);
        }
    }

    /**
     * SwingWorker subclass to perform the simulated execution in background thread.
     * It is "interrupted" when main thread sets the "stop" variable to true.
     * The variable is tested before the next MIPS instruction is simulated.  Thus
     * interruption occurs in a tightly controlled fashion.
     * <p>
     * See {@link SwingWorker} for more details on its functionality and usage.  It is
     * provided by Sun Microsystems for download and is not part of the Swing library.
     */
    private static class SimulatorThread extends SwingWorker {
        private final Simulator simulator;
        private final Program program;
        private final int maxSteps;
        private int[] breakPoints;
        private int programCounter;
        private ProcessingException exception;
        private final VenusAction starter;
        private volatile VenusAction stopper;
        private volatile boolean shouldStop = false;
        private StopReason stopReason;
        private boolean isFinished;

        /**
         * SimulatorThread constructor.  Receives all the information it needs to simulate execution.
         *
         * @param program        The program to be simulated.
         * @param programCounter Address in text segment of first instruction to simulate.
         * @param maxSteps       Maximum number of instruction steps to simulate.  Default of -1 means no maximum.
         * @param breakPoints    Array of breakpoints (instruction addresses) specified by user.
         * @param starter        The GUI component responsible for this call, usually GO or STEP.  null if none.
         */
        public SimulatorThread(Simulator simulator, Program program, int programCounter, int maxSteps, int[] breakPoints, VenusAction starter) {
            super(Application.getGUI() != null);
            this.simulator = simulator;
            this.program = program;
            this.programCounter = programCounter;
            this.maxSteps = maxSteps;
            this.breakPoints = breakPoints;
            this.isFinished = false;
            this.exception = null;
            this.starter = starter;
            this.stopper = null;
        }

        /**
         * Sets to "true" the volatile boolean variable that is tested after each
         * MIPS instruction is executed.  After calling this method, the next test
         * will yield "true" and "construct" will return.
         *
         * @param actor the Swing component responsible for this call.
         */
        public void stop(VenusAction actor) {
            shouldStop = true;
            stopper = actor;
        }

        /**
         * This is comparable to the Runnable "run" method (it is called by
         * SwingWorker's "run" method).  It simulates the program
         * execution in the background.
         *
         * @return Boolean value true if execution is done, false otherwise
         */
        @Override
        public Object construct() {
            // The next two statements are necessary for GUI to be consistently updated
            // before the simulation gets underway.  Without them, this happens only intermittently,
            // with a consequence that some simulations are interruptable using PAUSE/STOP and others
            // are not (because one or the other or both is not yet enabled).
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
            // Let the main thread run a bit to finish updating the GUI
            Thread.yield();

            if (breakPoints == null || breakPoints.length == 0) {
                breakPoints = null;
            }
            else {
                // Must be pre-sorted for binary search
                Arrays.sort(breakPoints);
            }

            simulator.dispatchStartedUpdate(maxSteps, programCounter);

            RegisterFile.initializeProgramCounter(programCounter);
            ProgramStatement statement;
            try {
                statement = Application.memory.getStatement(RegisterFile.getProgramCounter());
            }
            catch (AddressErrorException exception) {
                ErrorList errors = new ErrorList();
                errors.add(new ErrorMessage(program, 0, 0, "invalid program counter value: " + Binary.intToHexString(RegisterFile.getProgramCounter())));
                this.exception = new ProcessingException(errors, exception);
                // Next statement is a hack.  Previous statement sets EPC register to ProgramCounter-4
                // because it assumes the bad address comes from an operand so the ProgramCounter has already been
                // incremented.  In this case, bad address is the instruction fetch itself so Program Counter has
                // not yet been incremented.  We'll set the EPC directly here.  DPS 8-July-2013
                Coprocessor0.updateRegister(Coprocessor0.EPC, RegisterFile.getProgramCounter());
                this.stopReason = StopReason.EXCEPTION;
                return this.isFinished = true;
            }

            int steps = 0;

            // *******************  PS addition 26 July 2006  **********************
            // A couple statements below were added for the purpose of assuring that when
            // "back stepping" is enabled, every instruction will have at least one entry
            // on the back-stepping stack.  Most instructions will because they write either
            // to a register or memory.  But "nop" and branches not taken do not.  When the
            // user is stepping backward through the program, the stack is popped and if
            // an instruction has no entry it will be skipped over in the process.  This has
            // no effect on the correctness of the mechanism but the visual jerkiness when
            // instruction highlighting skips such instructions is disruptive.  Current solution
            // is to add a "do nothing" stack entry for instructions that do no write anything.
            // To keep this invisible to the "simulate()" method writer, we
            // will push such an entry onto the stack here if there is none for this instruction
            // by the time it has completed simulating.  This is done by the IF statement
            // just after the call to the simulate method itself.  The BackStepper method does
            // the aforementioned check and decides whether to push or not.  The result
            // is a a smoother interaction experience.  But it comes at the cost of slowing
            // simulation speed for flat-out runs, for every MIPS instruction executed even
            // though very few will require the "do nothing" stack entry.  For stepped or
            // timed execution the slower execution speed is not noticeable.
            //
            // To avoid this cost I tried a different technique: back-fill with "do nothings"
            // during the backstepping itself when this situation is recognized.  Problem
            // was in recognizing all possible situations in which the stack contained such
            // a "gap".  It became a morass of special cases and it seemed every weird test
            // case revealed another one.  In addition, when a program
            // begins with one or more such instructions ("nop" and branches not taken),
            // the backstep button is not enabled until a "real" instruction is executed.
            // This is noticeable in stepped mode.
            // *********************************************************************

            while (statement != null) {
                programCounter = RegisterFile.getProgramCounter(); // added: 7/26/06 (explanation above)
                RegisterFile.incrementPC();
                // Perform the MIPS instruction in synchronized block.  If external threads agree
                // to access MIPS memory and registers only through synchronized blocks on same
                // lock variable, then full (albeit heavy-handed) protection of MIPS memory and
                // registers is assured.  Not as critical for reading from those resources.
                synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                    try {
                        if (Simulator.externalInterruptingDevice != NO_DEVICE) {
                            int deviceInterruptCode = externalInterruptingDevice;
                            Simulator.externalInterruptingDevice = NO_DEVICE;
                            throw new ProcessingException(statement, "external interrupt", deviceInterruptCode);
                        }
                        BasicInstruction instruction = (BasicInstruction) statement.getInstruction();
                        if (instruction == null) {
                            throw new ProcessingException(statement, "unknown instruction " + Binary.intToHexString(statement.getBinaryStatement()), Exceptions.RESERVED_INSTRUCTION_EXCEPTION);
                        }
                        // THIS IS WHERE THE INSTRUCTION EXECUTION IS ACTUALLY SIMULATED!
                        instruction.getSimulationCode().simulate(statement);

                        // IF statement added 7/26/06 (explanation above)
                        if (Application.getSettings().getBackSteppingEnabled()) {
                            Application.program.getBackStepper().addDoNothing(programCounter);
                        }
                    }
                    catch (ProcessingException exception) {
                        if (exception.errors() == null) {
                            this.stopReason = StopReason.EXIT_SYSCALL;
                            return this.isFinished = true;
                        }
                        else {
                            // See if an exception handler is present.  Assume this is the case
                            // if and only if memory location Memory.exceptionHandlerAddress
                            // (e.g. 0x80000180) contains an instruction.  If so, then set the
                            // program counter there and continue.  Otherwise terminate the
                            // MIPS program with appropriate error message.
                            ProgramStatement exceptionHandler = null;
                            try {
                                exceptionHandler = Application.memory.getStatement(Memory.exceptionHandlerAddress);
                            }
                            catch (AddressErrorException e) {
                                // Will not occur with this well-known address
                            }
                            if (exceptionHandler != null) {
                                RegisterFile.setProgramCounter(Memory.exceptionHandlerAddress);
                            }
                            else {
                                this.stopReason = StopReason.EXCEPTION;
                                this.exception = exception;
                                return this.isFinished = true;
                            }
                        }
                    }
                }

                // DPS 15 June 2007.  Handle delayed branching if it occurs.
                if (DelayedBranch.isTriggered()) {
                    RegisterFile.setProgramCounter(DelayedBranch.getBranchTargetAddress());
                    DelayedBranch.clear();
                }
                else if (DelayedBranch.isRegistered()) {
                    DelayedBranch.trigger();
                }

                // Volatile variable initialized false but can be set true by the main thread.
                // Used to stop or pause a running MIPS program.  See stopSimulation() above.
                if (shouldStop) {
                    this.stopReason = StopReason.EXTERNAL;
                    return this.isFinished = false;
                }
                // Return if we've reached a breakpoint.
                if (breakPoints != null && Arrays.binarySearch(breakPoints, RegisterFile.getProgramCounter()) >= 0) {
                    this.stopReason = StopReason.BREAKPOINT;
                    return this.isFinished = false;
                }
                // Check number of MIPS instructions executed.  Return if at limit (-1 is no limit).
                if (maxSteps > 0) {
                    steps++;
                    if (steps >= maxSteps) {
                        this.stopReason = StopReason.STEP_LIMIT_REACHED;
                        return this.isFinished = false;
                    }
                }

                // schedule GUI update only if: there is in fact a GUI! AND
                //                              using Run,  not Step (maxSteps > 1) AND
                //                              running slowly enough for GUI to keep up
                //if (Globals.getGui() != null && maxSteps != 1 &&
                if (interactiveGUIUpdater != null && maxSteps != 1 && RunSpeedPanel.getInstance().getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
                    SwingUtilities.invokeLater(interactiveGUIUpdater);
                }
                if (Application.getGUI() != null || Application.runSpeedPanelExists) { // OR added by DPS 24 July 2008 to enable speed control by stand-alone tool
                    if (maxSteps != 1 && RunSpeedPanel.getInstance().getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
                        try {
                            Thread.sleep((int) (1000 / RunSpeedPanel.getInstance().getRunSpeed())); // make sure it's never zero!
                        }
                        catch (InterruptedException ignored) {
                        }
                    }
                }

                // Get next instruction in preparation for next iteration
                try {
                    statement = Application.memory.getStatement(RegisterFile.getProgramCounter());
                }
                catch (AddressErrorException exception) {
                    ErrorList errors = new ErrorList();
                    errors.add(new ErrorMessage(program, 0, 0, "invalid program counter value: " + Binary.intToHexString(RegisterFile.getProgramCounter())));
                    this.exception = new ProcessingException(errors, exception);
                    // Next statement is a hack.  Previous statement sets EPC register to (ProgramCounter - 4)
                    // because it assumes the bad address comes from an operand so the ProgramCounter has already been
                    // incremented.  In this case, bad address is the instruction fetch itself so Program Counter has
                    // not yet been incremented.  We'll set the EPC directly here.  DPS 8-July-2013
                    Coprocessor0.updateRegister(Coprocessor0.EPC, RegisterFile.getProgramCounter());
                    this.stopReason = StopReason.EXCEPTION;
                    return this.isFinished = true;
                }
            }
            // DPS July 2007.  This "if" statement is needed for correct program
            // termination if delayed branching on and last statement in
            // program is a branch/jump.  Program will terminate rather than branch,
            // because that's what MARS does when execution drops off the bottom.
            if (DelayedBranch.isTriggered() || DelayedBranch.isRegistered()) {
                DelayedBranch.clear();
            }
            // If we got here it was due to null statement, which means program
            // counter "fell off the end" of the program.  NOTE: Assumes the
            // "while" loop contains no "break;" statements.
            this.stopReason = StopReason.RAN_OFF_BOTTOM;
            return this.isFinished = true;
        }

        /**
         * This method is invoked by the SwingWorker when the {@link #construct()} method returns.
         * It will update the GUI appropriately.  According to Sun's documentation, it
         * is run in the main thread so should work OK with Swing components (which are
         * not thread-safe).
         * <p>
         * Its action depends on what caused the return from {@link #construct()} and what
         * action led to the call of {@link #construct()} in the first place.
         */
        @Override
        public void finished() {
            // If running from the command-line, then there is no GUI to update.
            if (Application.getGUI() == null) {
                return;
            }

            if (starter instanceof RunStepForwardAction) {
                simulator.dispatchPausedUpdate(maxSteps, programCounter, stopReason);
            }
            else if (starter instanceof RunStartAction) {
                if (isFinished) {
                    simulator.dispatchFinishedUpdate(maxSteps, programCounter, stopReason, exception);
                }
                else if (stopReason == StopReason.BREAKPOINT) {
                    simulator.dispatchPausedUpdate(maxSteps, programCounter, stopReason);
                }
                else {
                    if (stopper instanceof RunPauseAction) {
                        simulator.dispatchPausedUpdate(maxSteps, programCounter, stopReason);
                    }
                    else if (stopper instanceof RunStopAction) {
                        simulator.dispatchFinishedUpdate(maxSteps, programCounter, stopReason, exception);
                    }
                }
            }
        }
    }

    private static class UpdateGUI implements Runnable {
        @Override
        public void run() {
            if (Application.getGUI().getRegistersPane().getSelectedComponent() == Application.getGUI().getMainPane().getExecutePane().getRegistersWindow()) {
                Application.getGUI().getMainPane().getExecutePane().getRegistersWindow().updateRegisters();
            }
            else {
                Application.getGUI().getMainPane().getExecutePane().getCoprocessor1Window().updateRegisters();
            }
            Application.getGUI().getMainPane().getExecutePane().getDataSegmentWindow().updateValues();
            Application.getGUI().getMainPane().getExecutePane().getTextSegmentWindow().setCodeHighlighting(true);
            Application.getGUI().getMainPane().getExecutePane().getTextSegmentWindow().highlightStepAtPC();
        }
    }
}
