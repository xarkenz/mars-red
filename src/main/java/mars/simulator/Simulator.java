package mars.simulator;

import mars.*;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.mips.instructions.BasicInstruction;
import mars.util.Binary;
import mars.venus.*;
import mars.venus.execute.ExecuteTab;
import mars.venus.execute.ProgramStatus;
import mars.venus.execute.RunSpeedPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static Simulator instance = null;
    private static Runnable interactiveGUIUpdater = null;
    // Others can set this true to indicate external interrupt.  Initially used
    // to simulate keyboard and display interrupts.  The device is identified
    // by the address of its MMIO control register.  keyboard 0xFFFF0000 and
    // display 0xFFFF0008.  DPS 23 July 2008.
    public static final int NO_DEVICE = 0;
    public static volatile int externalInterruptingDevice = NO_DEVICE;

    private SimulatorThread simulatorThread;
    private final SystemIO systemIO;

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
        /**
         * An unhandled internal error occurred during execution,
         * causing the simulator thread to terminate prematurely.
         */
        INTERNAL_ERROR,
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
        if (Simulator.instance == null) {
            Simulator.instance = new Simulator();
        }
        return Simulator.instance;
    }

    private Simulator() {
        this.simulatorThread = null;
        this.systemIO = new SystemIO();

        if (Application.getGUI() != null) {
            Simulator.interactiveGUIUpdater = () -> {
                ((RegistersDisplayTab) Application.getGUI().getRegistersPane().getSelectedComponent()).updateRegisters();
                Application.getGUI().getMainPane().getExecuteTab().getDataSegmentWindow().updateValues();
                Application.getGUI().getMainPane().getExecuteTab().getTextSegmentWindow().setCodeHighlighting(true);
                Application.getGUI().getMainPane().getExecuteTab().getTextSegmentWindow().highlightStepAtPC();
            };
        }
    }

    public SystemIO getSystemIO() {
        return this.systemIO;
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
     * @param programCounter Address of first instruction to simulate; this is the initial value of the program counter.
     * @param maxSteps       Maximum number of steps to perform before returning false (0 or less means no max).
     * @param breakpoints    Array of breakpoint program counter values. (Can be null.)
     * @return In command-line mode, true if execution completed, false otherwise. If in GUI mode, always true.
     * @throws ProcessingException Throws exception if run-time exception occurs.
     */
    public boolean simulate(Program program, int programCounter, int maxSteps, int[] breakpoints) throws ProcessingException {
        this.simulatorThread = new SimulatorThread(this, program, programCounter, maxSteps, breakpoints);
        this.simulatorThread.start();

        if (Application.getGUI() == null) {
            // The simulator was run from command-line instead of GUI.
            // So, just stick around until execution thread is finished.
            try {
                this.simulatorThread.join();
            }
            catch (InterruptedException exception) {
                // This should not happen, as the simulator thread should handle the interrupt
                System.err.println("Error: unhandled simulator interrupt: " + exception);
            }
            ProcessingException exception = this.simulatorThread.exception;
            boolean isFinished = this.simulatorThread.isFinished;
            this.simulatorThread = null;
            if (exception != null) {
                throw exception;
            }
            return isFinished;
        }
        else {
            // The simulator was run from the GUI, so just return true
            return true;
        }
    }

    /**
     * Flag the simulator to stop due to pausing. Once it has done so,
     * {@link SimulatorListener#paused(int, int, StopReason)}
     * will be called for all registered listeners.
     */
    public void pause() {
        if (simulatorThread != null) {
            simulatorThread.stopForPause();
            simulatorThread = null;
        }
    }

    /**
     * Flag the simulator to stop due to finishing. Once it has done so,
     * {@link SimulatorListener#finished(int, int, StopReason, ProcessingException)}
     * will be called for all registered listeners.
     */
    public void terminate() {
        if (simulatorThread != null) {
            simulatorThread.stopForTermination();
            simulatorThread = null;
        }
    }

    private final List<SimulatorListener> listeners = new ArrayList<>();

    public void addListener(SimulatorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SimulatorListener listener) {
        listeners.remove(listener);
    }

    /**
     * Called when the simulator has started execution of the current program.
     * Invokes {@link SimulatorListener#started(int, int)} for all listeners.
     */
    private void dispatchStartedUpdate(int maxSteps, int programCounter) {
        Application.getGUI().getMessagesPane().writeToMessages(this.getClass().getSimpleName() + ": started simulation.\n\n");
        Application.getGUI().getMessagesPane().selectConsoleTab();
        Application.getGUI().setMenuState(ProgramStatus.RUNNING);

        this.setChanged();
        this.notifyObservers(new SimulatorNotice(SimulatorNotice.SIMULATOR_START, maxSteps, RunSpeedPanel.getInstance().getRunSpeed(), programCounter));

        for (SimulatorListener listener : listeners) {
            listener.started(maxSteps, programCounter);
        }
    }

    /**
     * Called when the simulator has paused execution of the current program.
     * Invokes {@link SimulatorListener#paused(int, int, StopReason)} for all listeners.
     */
    public void dispatchPausedUpdate(int maxSteps, int programCounter, Simulator.StopReason reason) {
        this.setChanged();
        this.notifyObservers(new SimulatorNotice(SimulatorNotice.SIMULATOR_STOP, maxSteps, RunSpeedPanel.getInstance().getRunSpeed(), programCounter));

        MessagesPane messagesPane = Application.getGUI().getMessagesPane();
        switch (reason) {
            case BREAKPOINT -> {
                messagesPane.writeToMessages(this.getClass().getSimpleName() + ": paused simulation at breakpoint.\n\n");
                messagesPane.selectMessagesTab();
            }
            case EXTERNAL -> {
                messagesPane.writeToMessages(this.getClass().getSimpleName() + ": paused simulation.\n\n");
            }
        }

        RegistersPane registersPane = Application.getGUI().getRegistersPane();
        ExecuteTab executeTab = Application.getGUI().getMainPane().getExecuteTab();
        // Update register and data segment values
        registersPane.getRegistersWindow().updateRegisters();
        registersPane.getCoprocessor1Window().updateRegisters();
        registersPane.getCoprocessor0Window().updateRegisters();
        executeTab.getDataSegmentWindow().updateValues();
        // Highlight last executed instruction in text segment
        executeTab.getTextSegmentWindow().setCodeHighlighting(true);
        executeTab.getTextSegmentWindow().unhighlightAllSteps();
        executeTab.getTextSegmentWindow().highlightStepAtPC();

        executeTab.setProgramStatus(ProgramStatus.PAUSED);

        for (SimulatorListener listener : listeners) {
            listener.paused(maxSteps, programCounter, reason);
        }
    }

    /**
     * Called when the simulator has finished execution of the current program.
     * Invokes {@link SimulatorListener#finished(int, int, StopReason, ProcessingException)} for all listeners.
     */
    private void dispatchFinishedUpdate(int maxSteps, int programCounter, StopReason reason, ProcessingException exception) {
        this.setChanged();
        this.notifyObservers(new SimulatorNotice(SimulatorNotice.SIMULATOR_STOP, maxSteps, RunSpeedPanel.getInstance().getRunSpeed(), programCounter));

        RegistersPane registersPane = Application.getGUI().getRegistersPane();
        ExecuteTab executeTab = Application.getGUI().getMainPane().getExecuteTab();
        // Update register and data segment values
        registersPane.getRegistersWindow().updateRegisters();
        registersPane.getCoprocessor1Window().updateRegisters();
        registersPane.getCoprocessor0Window().updateRegisters();
        executeTab.getDataSegmentWindow().updateValues();
        // Highlight last executed instruction in text segment
        executeTab.getTextSegmentWindow().setCodeHighlighting(true);
        executeTab.getTextSegmentWindow().unhighlightAllSteps();
        executeTab.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - BasicInstruction.INSTRUCTION_LENGTH_BYTES);

        // Bring Coprocessor 0 to the front if terminated due to exception
        if (exception != null) {
            registersPane.setSelectedComponent(registersPane.getCoprocessor0Window());
        }

        MessagesPane messagesPane = Application.getGUI().getMessagesPane();
        switch (reason) {
            case EXIT_SYSCALL -> {
                messagesPane.writeToMessages(this.getClass().getSimpleName() + ": finished simulation successfully.\n\n");
                messagesPane.writeToConsole("\n--- program finished ---\n\n");
                messagesPane.selectConsoleTab();
            }
            case RAN_OFF_BOTTOM -> {
                messagesPane.writeToMessages(this.getClass().getSimpleName() + ": finished simulation due to null instruction.\n\n");
                messagesPane.writeToConsole("\n--- program automatically terminated (ran off bottom) ---\n\n");
                messagesPane.selectConsoleTab();
            }
            case EXCEPTION -> {
                messagesPane.writeToMessages(this.getClass().getSimpleName() + ": finished simulation with errors.\n\n");
                if (exception == null) {
                    messagesPane.writeToConsole("\n--- program terminated due to error(s) ---\n\n");
                }
                else {
                    messagesPane.writeToConsole("\n--- program terminated due to error(s): ---\n");
                    messagesPane.writeToConsole(exception.errors().generateErrorReport());
                    messagesPane.writeToConsole("--- end of error report ---\n\n");
                }
                messagesPane.selectConsoleTab();
            }
            case EXTERNAL -> {
                messagesPane.writeToMessages(this.getClass().getSimpleName() + ": stopped simulation.\n\n");
                messagesPane.writeToConsole("\n--- program terminated by user ---\n\n");
            }
            case INTERNAL_ERROR -> {
                messagesPane.writeToMessages(this.getClass().getSimpleName() + ": stopped simulation after encountering an internal error.");
                messagesPane.writeToConsole("\n--- program terminated due to internal error ---\n\n");
            }
        }

        Application.getGUI().setProgramStatus(ProgramStatus.TERMINATED);

        // Close any unclosed file descriptors opened in execution of program
        systemIO.resetFiles();

        for (SimulatorListener listener : listeners) {
            listener.finished(maxSteps, programCounter, reason, exception);
        }
    }

    /**
     * {@link Thread} subclass to perform a MIPS simulation in the background.
     * The thread can be interrupted using either {@link #pause()} or {@link #terminate()}.
     */
    private static class SimulatorThread extends Thread {
        private final Simulator simulator;
        private final Program program;
        private final int maxSteps;
        private final int[] breakPoints;

        private int programCounter;
        private ProcessingException exception;
        private StopReason stopReason;
        private boolean isFinished;

        /**
         * Create a new {@code SimulatorThread} without starting it.
         *
         * @param program        The program to be simulated.
         * @param programCounter Address in text segment of first instruction to simulate.
         * @param maxSteps       Maximum number of instruction steps to simulate.  Default of -1 means no maximum.
         * @param breakPoints    Array of breakpoints (instruction addresses) specified by user.
         */
        public SimulatorThread(Simulator simulator, Program program, int programCounter, int maxSteps, int[] breakPoints) {
            super("MIPS");
            this.simulator = simulator;
            this.program = program;
            this.maxSteps = maxSteps;
            this.breakPoints = breakPoints;
            this.programCounter = programCounter;
            this.exception = null;
            this.stopReason = StopReason.INTERNAL_ERROR; // In case something truly wild happens
            this.isFinished = false;
        }

        /**
         * Flag this thread to stop due to pausing. Once it has done so,
         * {@link SimulatorListener#paused(int, int, StopReason)}
         * will be called for all registered listeners.
         */
        public void stopForPause() {
            this.isFinished = false;
            this.interrupt();
        }

        /**
         * Flag this thread to stop due to finishing. Once it has done so,
         * {@link SimulatorListener#finished(int, int, StopReason, ProcessingException)}
         * will be called for all registered listeners.
         */
        public void stopForTermination() {
            this.isFinished = true;
            this.interrupt();
        }

        /**
         * Simulate the program given to this thread until a pause or finish condition is reached.
         * Once the program starts, {@link SimulatorListener#started(int, int)}
         * will be called for all registered listeners.
         */
        @Override
        public void run() {
            // The next two statements are necessary for GUI to be consistently updated
            // before the simulation gets underway.  Without them, this happens only intermittently,
            // with a consequence that some simulations are interruptable using PAUSE/STOP and others
            // are not (because one or the other or both is not yet enabled).
            this.setPriority(Thread.NORM_PRIORITY - 1);
            // Let the main thread run a bit to finish updating the GUI
            Thread.yield();

            try {
                this.runSimulation();
            }
            catch (Exception exception) {
                // Should only happen if there is a bug somewhere
                System.err.println("Error: unhandled exception during simulation:");
                exception.printStackTrace();
                this.isFinished = true;
                this.stopReason = StopReason.INTERNAL_ERROR;
            }
            finally {
                // If running from the command-line, then there is no GUI to update.
                // TODO: remove this check once all the hardcoded logic is moved into listeners
                if (Application.getGUI() != null) {
                    if (this.isFinished) {
                        this.simulator.dispatchFinishedUpdate(this.maxSteps, this.programCounter, this.stopReason, this.exception);
                    }
                    else {
                        this.simulator.dispatchPausedUpdate(this.maxSteps, this.programCounter, this.stopReason);
                    }
                }
            }
        }

        /**
         * The main simulation logic. This is run on the simulator thread, and is always called from {@link #run()}.
         */
        private void runSimulation() {
            if (this.breakPoints != null) {
                // Must be pre-sorted for binary search
                Arrays.sort(this.breakPoints);
            }

            this.simulator.dispatchStartedUpdate(this.maxSteps, this.programCounter);

            RegisterFile.initializeProgramCounter(this.programCounter);
            ProgramStatement statement;
            try {
                statement = Application.memory.getStatement(RegisterFile.getProgramCounter());
            }
            catch (AddressErrorException exception) {
                ErrorList errors = new ErrorList();
                errors.add(new ErrorMessage(this.program, 0, 0, "invalid program counter value: " + Binary.intToHexString(RegisterFile.getProgramCounter())));
                // Next statement is a hack.  Previous statement sets EPC register to (PC - 4)
                // because it assumes the bad address comes from an operand so the program counter has already been
                // incremented.  In this case, bad address is the instruction fetch itself so program counter has
                // not yet been incremented.  We'll set the EPC directly here.  DPS 8-July-2013
                Coprocessor0.updateRegister(Coprocessor0.EPC, RegisterFile.getProgramCounter());

                this.stopReason = StopReason.EXCEPTION;
                this.isFinished = true;
                this.exception = new ProcessingException(errors, exception);
                return;
            }

            // If there is a step limit, this is used to track the number of steps taken
            int stepCount = 0;

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

            // Main simulation loop
            while (statement != null) {
                this.programCounter = RegisterFile.getProgramCounter(); // added: 7/26/06 (explanation above)
                RegisterFile.incrementPC();
                // Perform the MIPS instruction in synchronized block.  If external threads agree
                // to access MIPS memory and registers only through synchronized blocks on same
                // lock variable, then full (albeit heavy-handed) protection of MIPS memory and
                // registers is assured.  Not as critical for reading from those resources.
                synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                    try {
                        if (Simulator.externalInterruptingDevice != NO_DEVICE) {
                            int deviceInterruptCode = Simulator.externalInterruptingDevice;
                            Simulator.externalInterruptingDevice = NO_DEVICE;
                            throw new ProcessingException(
                                statement,
                                "external interrupt",
                                deviceInterruptCode
                            );
                        }
                        BasicInstruction instruction = (BasicInstruction) statement.getInstruction();
                        if (instruction == null) {
                            throw new ProcessingException(
                                statement,
                                "invalid instruction: " + Binary.intToHexString(statement.getBinaryStatement()),
                                Exceptions.RESERVED_INSTRUCTION_EXCEPTION
                            );
                        }

                        // THIS IS WHERE THE INSTRUCTION EXECUTION IS ACTUALLY SIMULATED!
                        instruction.getSimulationCode().simulate(statement);

                        // IF statement added 7/26/06 (explanation above)
                        if (Application.getSettings().getBackSteppingEnabled()) {
                            Application.program.getBackStepper().addDoNothing(this.programCounter);
                        }
                    }
                    catch (ProcessingException exception) {
                        if (exception.errors() == null) {
                            this.stopReason = StopReason.EXIT_SYSCALL;
                            this.isFinished = true;
                            return;
                        }

                        // See if an exception handler is present.  Assume this is the case
                        // if and only if memory location Memory.exceptionHandlerAddress
                        // (e.g. 0x80000180) contains an instruction.  If so, then set the
                        // program counter there and continue.  Otherwise terminate the
                        // MIPS program with appropriate error message.
                        ProgramStatement exceptionHandler;
                        try {
                            exceptionHandler = Application.memory.getStatement(Memory.exceptionHandlerAddress);
                        }
                        catch (AddressErrorException ignored) {
                            // Will not occur with this well-known address
                            exceptionHandler = null;
                        }
                        if (exceptionHandler != null) {
                            RegisterFile.setProgramCounter(Memory.exceptionHandlerAddress);
                        }
                        else {
                            this.stopReason = StopReason.EXCEPTION;
                            this.isFinished = true;
                            this.exception = exception;
                            return;
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

                // Check for an interrupt (either a pause or termination)
                if (Thread.interrupted()) {
                    // this.isFinished will be set by the method that caused the interrupt
                    this.stopReason = StopReason.EXTERNAL;
                    return;
                }
                // Check for a breakpoint
                if (this.breakPoints != null && Arrays.binarySearch(this.breakPoints, RegisterFile.getProgramCounter()) >= 0) {
                    this.stopReason = StopReason.BREAKPOINT;
                    this.isFinished = false;
                    return;
                }
                // Check whether the step limit has been reached (if one exists)
                if (this.maxSteps > 0) {
                    stepCount++;
                    if (stepCount >= this.maxSteps) {
                        this.stopReason = StopReason.STEP_LIMIT_REACHED;
                        this.isFinished = false;
                        return;
                    }
                }

                // Schedule a GUI update if the program is not running at unlimited speed
                if (Simulator.interactiveGUIUpdater != null && RunSpeedPanel.getInstance().getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
                    SwingUtilities.invokeLater(Simulator.interactiveGUIUpdater);
                }

                // Wait according to the speed setting
                if (Application.getGUI() != null || Application.runSpeedPanelExists) { // OR added by DPS 24 July 2008 to enable speed control by stand-alone tool
                    if (RunSpeedPanel.getInstance().getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
                        try {
                            Thread.sleep((int) (1000 / RunSpeedPanel.getInstance().getRunSpeed())); // make sure it's never zero!
                        }
                        catch (InterruptedException exception) {
                            // this.isFinished will be set by the method that caused the interrupt
                            this.stopReason = StopReason.EXTERNAL;
                            return;
                        }
                    }
                }

                // Get next instruction in preparation for next iteration
                try {
                    statement = Application.memory.getStatement(RegisterFile.getProgramCounter());
                }
                catch (AddressErrorException exception) {
                    // Next statement is a hack.  Previous statement sets EPC register to (ProgramCounter - 4)
                    // because it assumes the bad address comes from an operand so the ProgramCounter has already been
                    // incremented.  In this case, bad address is the instruction fetch itself so Program Counter has
                    // not yet been incremented.  We'll set the EPC directly here.  DPS 8-July-2013
                    Coprocessor0.updateRegister(Coprocessor0.EPC, RegisterFile.getProgramCounter());

                    ErrorList errors = new ErrorList();
                    errors.add(new ErrorMessage(
                        this.program,
                        0, 0,
                        "invalid program counter value: " + Binary.intToHexString(RegisterFile.getProgramCounter())
                    ));
                    this.exception = new ProcessingException(errors, exception);
                    this.stopReason = StopReason.EXCEPTION;
                    this.isFinished = true;
                    return;
                }
            }

            // If we got here, it was due to a null statement, which means the program counter
            // "fell off the end" of the program.
            // Note: The "while" loop should not contain any "break" statements for this reason.

            // DPS July 2007.  This "if" statement is needed for correct program
            // termination if delayed branching on and last statement in
            // program is a branch/jump.  Program will terminate rather than branch,
            // because that's what MARS does when execution drops off the bottom.
            if (DelayedBranch.isTriggered() || DelayedBranch.isRegistered()) {
                DelayedBranch.clear();
            }

            this.stopReason = StopReason.RAN_OFF_BOTTOM;
            this.isFinished = true;
        }
    }
}
