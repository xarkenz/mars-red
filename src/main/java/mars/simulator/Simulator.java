package mars.simulator;

import mars.*;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.mips.instructions.BasicInstruction;
import mars.util.Binary;
import mars.venus.execute.RunSpeedPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * @author Pete Sanderson, August 2005
 */
public class Simulator {
    private static Simulator instance = null;

    // Others can set this true to indicate external interrupt.  Initially used
    // to simulate keyboard and display interrupts.  The device is identified
    // by the address of its MMIO control register.  keyboard 0xFFFF0000 and
    // display 0xFFFF0008.  DPS 23 July 2008
    public static final int NO_DEVICE = 0;
    public static volatile int externalInterruptingDevice = NO_DEVICE;

    private final List<SimulatorListener> guiListeners;
    private final List<SimulatorListener> threadListeners;
    private final SystemIO systemIO;
    private SimulatorThread simulatorThread;

    /**
     * Returns the singleton instance of the MIPS simulator.
     *
     * @return The Simulator object in use.
     */
    public static Simulator getInstance() {
        if (Simulator.instance == null) {
            Simulator.instance = new Simulator();
        }
        return Simulator.instance;
    }

    private Simulator() {
        this.guiListeners = new ArrayList<>();
        this.threadListeners = new ArrayList<>();
        this.systemIO = new SystemIO(this);
        this.simulatorThread = null;
    }

    /**
     * Obtain the associated {@link SystemIO} instance, which handles I/O-related syscall functionality.
     *
     * @return The system I/O handler.
     */
    public SystemIO getSystemIO() {
        return this.systemIO;
    }

    /**
     * Add a {@link SimulatorListener} whose callbacks will be executed on the GUI thread.
     *
     * @param listener The listener to add.
     */
    public void addGUIListener(SimulatorListener listener) {
        if (!this.guiListeners.contains(listener)) {
            this.guiListeners.add(listener);
        }
    }

    /**
     * Remove a {@link SimulatorListener} which was added via {@link #addGUIListener(SimulatorListener)}.
     *
     * @param listener The listener to remove.
     */
    public void removeGUIListener(SimulatorListener listener) {
        this.guiListeners.remove(listener);
    }

    /**
     * Add a {@link SimulatorListener} whose callbacks will be executed on the simulator thread.
     *
     * @param listener The listener to add.
     */
    public void addThreadListener(SimulatorListener listener) {
        if (!this.threadListeners.contains(listener)) {
            this.threadListeners.add(listener);
        }
    }

    /**
     * Remove a {@link SimulatorListener} which was added via {@link #addThreadListener(SimulatorListener)}.
     *
     * @param listener The listener to remove.
     */
    public void removeThreadListener(SimulatorListener listener) {
        this.threadListeners.remove(listener);
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
     * @throws ProcessingException Throws exception if run-time exception occurs.
     */
    public void simulate(Program program, int programCounter, int maxSteps, int[] breakpoints) throws ProcessingException {
        this.simulatorThread = new SimulatorThread(this, program, programCounter, maxSteps, breakpoints);
        this.simulatorThread.start();

        if (Application.getGUI() == null) {
            // The simulator was run from the command line

            // This is a slightly hacky way to get the exception out of the simulator thread (we love Java)
            final ProcessingException[] exception = new ProcessingException[1];
            SimulatorListener exceptionListener = new SimulatorListener() {
                @Override
                public void simulatorFinished(SimulatorFinishEvent event) {
                    exception[0] = event.getException();
                }
            };
            this.addThreadListener(exceptionListener);

            try {
                // Wait for the simulator thread to finish
                this.simulatorThread.join();
            }
            catch (InterruptedException interruptedException) {
                // This should not happen, as the simulator thread should handle the interrupt
                System.err.println("Error: unhandled simulator interrupt: " + interruptedException);
            }
            this.simulatorThread = null;
            this.removeThreadListener(exceptionListener);

            if (exception[0] != null) {
                throw exception[0];
            }
        }
    }

    /**
     * Flag the simulator to stop due to pausing. Once it has done so,
     * {@link SimulatorListener#simulatorPaused(SimulatorPauseEvent)} will be called for all registered listeners.
     */
    public void pause() {
        if (this.simulatorThread != null) {
            this.simulatorThread.stopForPause();
            this.simulatorThread = null;
        }
    }

    /**
     * Flag the simulator to stop due to termination. Once it has done so,
     * {@link SimulatorListener#simulatorFinished(SimulatorFinishEvent)} will be called for all registered listeners.
     */
    public void terminate() {
        if (this.simulatorThread != null) {
            this.simulatorThread.stopForTermination();
            this.simulatorThread = null;
        }
    }

    /**
     * Called when the simulator has started execution of the current program.
     * Invokes {@link SimulatorListener#simulatorStarted(SimulatorStartEvent)} for all listeners.
     */
    private void dispatchStartEvent(int stepCount, int programCounter) {
        final SimulatorStartEvent event = new SimulatorStartEvent(this, stepCount, programCounter);
        for (SimulatorListener listener : this.threadListeners) {
            listener.simulatorStarted(event);
        }
        if (Application.getGUI() != null) {
            SwingUtilities.invokeLater(() -> {
                for (SimulatorListener listener : this.guiListeners) {
                    listener.simulatorStarted(event);
                }
            });
        }
    }

    /**
     * Called when the simulator has paused execution of the current program.
     * Invokes {@link SimulatorListener#simulatorPaused(SimulatorPauseEvent)} for all listeners.
     */
    public void dispatchPauseEvent(int stepCount, int programCounter, SimulatorPauseEvent.Reason reason) {
        final SimulatorPauseEvent event = new SimulatorPauseEvent(this, stepCount, programCounter, reason);
        for (SimulatorListener listener : this.threadListeners) {
            listener.simulatorPaused(event);
        }
        if (Application.getGUI() != null) {
            SwingUtilities.invokeLater(() -> {
                for (SimulatorListener listener : this.guiListeners) {
                    listener.simulatorPaused(event);
                }
            });
        }
    }

    /**
     * Called when the simulator has finished execution of the current program.
     * Invokes {@link SimulatorListener#simulatorFinished(SimulatorFinishEvent)} for all listeners.
     */
    private void dispatchFinishEvent(int programCounter, SimulatorFinishEvent.Reason reason, ProcessingException exception) {
        final SimulatorFinishEvent event = new SimulatorFinishEvent(this, programCounter, reason, exception);
        for (SimulatorListener listener : this.threadListeners) {
            listener.simulatorFinished(event);
        }
        if (Application.getGUI() != null) {
            SwingUtilities.invokeLater(() -> {
                for (SimulatorListener listener : this.guiListeners) {
                    listener.simulatorFinished(event);
                }
            });
        }
    }

    /**
     * Called when the simulator has finished executing an instruction, but only if the run speed is not unlimited.
     * Invokes {@link SimulatorListener#simulatorStepped()} for all listeners.
     */
    private void dispatchStepEvent() {
        synchronized (this.threadListeners) {
            for (SimulatorListener listener : this.threadListeners) {
                listener.simulatorStepped();
            }
        }
        if (Application.getGUI() != null) {
            SwingUtilities.invokeLater(() -> {
                for (SimulatorListener listener : this.guiListeners) {
                    listener.simulatorStepped();
                }
            });
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
        private Runnable interruptEventDispatcher;

        /**
         * Create a new <code>SimulatorThread</code> without starting it.
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
            this.interruptEventDispatcher = this::dispatchExternalFinishEvent;
        }

        private void dispatchExternalPauseEvent() {
            // Dispatch a pause event once the simulator stops
            this.simulator.dispatchPauseEvent(this.maxSteps, this.programCounter, SimulatorPauseEvent.Reason.EXTERNAL);
        }

        private void dispatchExternalFinishEvent() {
            // Dispatch a pause event once the simulator stops
            this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.EXTERNAL, null);
        }

        /**
         * Flag this thread to stop due to pausing. Once it has done so,
         * {@link SimulatorListener#simulatorPaused(SimulatorPauseEvent)}
         * will be called for all registered listeners.
         */
        public void stopForPause() {
            this.interruptEventDispatcher = this::dispatchExternalPauseEvent;
            this.interrupt();
        }

        /**
         * Flag this thread to stop due to finishing. Once it has done so,
         * {@link SimulatorListener#simulatorFinished(SimulatorFinishEvent)}
         * will be called for all registered listeners.
         */
        public void stopForTermination() {
            this.interruptEventDispatcher = this::dispatchExternalFinishEvent;
            this.interrupt();
        }

        /**
         * Simulate the program given to this thread until a pause or finish condition is reached.
         * Once the program starts, {@link SimulatorListener#simulatorStarted(SimulatorStartEvent)}
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
            catch (ProcessingException exception) {
                this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.EXCEPTION, exception);
            }
            catch (InterruptedException exception) {
                // this.interruptEventDispatcher will be set by the method that caused the interrupt
                this.interruptEventDispatcher.run();
            }
            catch (Exception exception) {
                // Should only happen if there is a bug somewhere
                System.err.println("Error: unhandled exception during simulation:");
                exception.printStackTrace(System.err);
                this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.INTERNAL_ERROR, null);
            }
        }

        /**
         * The main simulation logic. This is run on the simulator thread, and is always called from {@link #run()}.
         */
        private void runSimulation() throws ProcessingException, InterruptedException {
            if (this.breakPoints != null) {
                // Must be pre-sorted for binary search
                Arrays.sort(this.breakPoints);
            }

            this.simulator.dispatchStartEvent(this.maxSteps, this.programCounter);

            RegisterFile.initializeProgramCounter(this.programCounter);

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
            ProgramStatement statement;
            while ((statement = this.getStatement()) != null) {
                this.programCounter = RegisterFile.getProgramCounter(); // Added 7/26/06 (explanation above)
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
                                ExceptionCause.RESERVED_INSTRUCTION_EXCEPTION
                            );
                        }

                        // THIS IS WHERE THE INSTRUCTION EXECUTION IS ACTUALLY SIMULATED!
                        instruction.getSimulationCode().simulate(statement);

                        // IF statement added 7/26/06 (explanation above)
                        if (Application.isBackSteppingEnabled()) {
                            Application.program.getBackStepper().addDoNothing(this.programCounter);
                        }
                    }
                    catch (ProcessingException exception) {
                        if (exception.getErrors() == null) {
                            this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.EXIT_SYSCALL, exception);
                            return;
                        }

                        // See if an exception handler is present.  Assume this is the case
                        // if and only if memory location Memory.exceptionHandlerAddress
                        // (e.g. 0x80000180) contains an instruction.  If so, then set the
                        // program counter there and continue.  Otherwise terminate the
                        // MIPS program with appropriate error message.
                        ProgramStatement exceptionHandler = null;
                        try {
                            exceptionHandler = Memory.getInstance().getStatement(Memory.exceptionHandlerAddress);
                        }
                        catch (AddressErrorException ignored) {
                            // Will not occur with this well-known address
                        }

                        if (exceptionHandler != null) {
                            RegisterFile.setProgramCounter(Memory.exceptionHandlerAddress);
                        }
                        else {
                            throw exception;
                        }
                    }
                    catch (InterruptedException exception) {
                        // The instruction was interrupted in the middle of what it was doing,
                        // and it should have already reverted all of its own changes, so all we need to do
                        // to allow a pause interrupt to work is undo the incrementation of the program counter
                        RegisterFile.setProgramCounter(this.programCounter);
                        // Proceed with interrupt handling as usual
                        throw exception;
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
                    throw new InterruptedException();
                }
                // Check for a breakpoint
                if (this.breakPoints != null && Arrays.binarySearch(this.breakPoints, RegisterFile.getProgramCounter()) >= 0) {
                    this.simulator.dispatchPauseEvent(this.maxSteps, this.programCounter, SimulatorPauseEvent.Reason.BREAKPOINT);
                    return;
                }
                // Check whether the step limit has been reached (if one exists)
                if (this.maxSteps > 0) {
                    stepCount++;
                    if (stepCount >= this.maxSteps) {
                        this.simulator.dispatchPauseEvent(this.maxSteps, this.programCounter, SimulatorPauseEvent.Reason.STEP_LIMIT_REACHED);
                        return;
                    }
                }

                // Update GUI and delay the next step if the program is not running at unlimited speed
                if (Application.getGUI() != null && RunSpeedPanel.getInstance().getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
                    // Schedule a GUI update
                    this.simulator.dispatchStepEvent();

                    // Wait according to the speed setting (division is fine here since it should never be 0)
                    Thread.sleep((int) (1000.0 / RunSpeedPanel.getInstance().getRunSpeed()));
                }
            }
            // End of main simulation loop

            // If we got here, it was due to a null statement, which means the program counter
            // "fell off the end" of the program.
            // The "while" loop above should not contain any "break" statements for this reason, only "return"s.

            // DPS July 2007.  This "if" statement is needed for correct program
            // termination if delayed branching on and last statement in
            // program is a branch/jump.  Program will terminate rather than branch,
            // because that's what MARS does when execution drops off the bottom.
            if (DelayedBranch.isTriggered() || DelayedBranch.isRegistered()) {
                DelayedBranch.clear();
            }

            this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.RAN_OFF_BOTTOM, null);
        }

        private ProgramStatement getStatement() throws ProcessingException {
            try {
                return Memory.getInstance().getStatement(RegisterFile.getProgramCounter());
            }
            catch (AddressErrorException exception) {
                // Next statement is a hack.  Previous statement sets EPC register to (PC - 4)
                // because it assumes the bad address comes from an operand so the program counter has already been
                // incremented.  In this case, bad address is the instruction fetch itself so program counter has
                // not yet been incremented.  We'll set the EPC directly here.  DPS 8-July-2013
                Coprocessor0.updateRegister(Coprocessor0.EPC, RegisterFile.getProgramCounter());

                ErrorList errors = new ErrorList();
                errors.add(new ErrorMessage(
                    this.program,
                    0, 0,
                    "invalid program counter value: " + Binary.intToHexString(RegisterFile.getProgramCounter())
                ));

                throw new ProcessingException(errors, exception);
            }
        }
    }
}
