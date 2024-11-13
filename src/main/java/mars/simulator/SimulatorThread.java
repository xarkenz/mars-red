package mars.simulator;

import mars.*;
import mars.assembler.BasicStatement;
import mars.mips.hardware.*;
import mars.mips.instructions.Instruction;
import mars.util.Binary;

import java.util.Arrays;

/**
 * {@link Thread} subclass to perform MIPS simulation in the background. The thread can be interrupted using either
 * {@link Simulator#pause()} or {@link Simulator#terminate()}.
 */
public class SimulatorThread extends Thread {
    private final Simulator simulator;
    private final int maxSteps;
    private final int[] breakPoints;
    private int programCounter;
    private volatile Runnable stopEventDispatcher;

    /**
     * Create a new <code>SimulatorThread</code> without starting it.
     *
     * @param programCounter Address in text segment of first instruction to simulate.
     * @param maxSteps       Maximum number of instruction steps to simulate.  Default of -1 means no maximum.
     * @param breakPoints    Array of breakpoints (instruction addresses) specified by user.
     */
    public SimulatorThread(Simulator simulator, int programCounter, int maxSteps, int[] breakPoints) {
        super("MIPS");
        this.simulator = simulator;
        this.maxSteps = maxSteps;
        this.breakPoints = breakPoints;
        this.programCounter = programCounter;
        this.stopEventDispatcher = this::dispatchExternalFinishEvent;
    }

    /**
     * Flag this thread to stop due to pausing. Once it has done so,
     * {@link SimulatorListener#simulatorPaused(SimulatorPauseEvent)} will be called for all registered listeners.
     * <p>
     * This method may be called from any thread.
     */
    public void stopForPause() {
        this.stopEventDispatcher = this::dispatchExternalPauseEvent;
        this.interrupt();
    }

    /**
     * Flag this thread to stop due to termination. Once it has done so,
     * {@link SimulatorListener#simulatorFinished(SimulatorFinishEvent)} will be called for all registered listeners.
     * <p>
     * This method may be called from any thread.
     */
    public void stopForTermination() {
        this.stopEventDispatcher = this::dispatchExternalFinishEvent;
        this.interrupt();
    }

    private void dispatchExternalPauseEvent() {
        // Dispatch a pause event once the simulator stops
        this.simulator.dispatchPauseEvent(this.maxSteps, this.programCounter, SimulatorPauseEvent.Reason.EXTERNAL);
    }

    private void dispatchExternalFinishEvent() {
        // Dispatch a finish event once the simulator stops
        this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.EXTERNAL, null);
    }

    /**
     * Simulate the program given to this thread until a pause or finish condition is reached. Once the program starts,
     * {@link SimulatorListener#simulatorStarted(SimulatorStartEvent)} will be called for all registered listeners.
     */
    @Override
    public void run() {
        // TODO: is this actually necessary?
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
        catch (SimulatorException exception) {
            // An unhandled exception occurred during simulation
            this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.EXCEPTION, exception);
        }
        catch (InterruptedException exception) {
            // The event dispatcher runnable will be set by the method that caused the interrupt
            this.stopEventDispatcher.run();
        }
        catch (Exception exception) {
            // Should only happen if there is a bug somewhere
            System.err.println("Error: internal exception during simulation (this is a bug!):");
            exception.printStackTrace(System.err);
            this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.INTERNAL_ERROR, null);
        }
    }

    /**
     * The main simulation logic. This is run on the simulator thread, and is always called from {@link #run()}.
     */
    private void runSimulation() throws SimulatorException, InterruptedException {
        if (this.breakPoints != null) {
            // Must be pre-sorted for binary search
            Arrays.sort(this.breakPoints);
        }

        this.simulator.dispatchStartEvent(this.maxSteps, this.programCounter);

        Processor.initializeProgramCounter(this.programCounter);

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

        // Main simulation loop, repeat until the thread is interrupted or some end condition is reached
        while (true) {
            // Fetch the statement to execute from memory
            BasicStatement statement = this.fetchStatement();
            if (statement == null) {
                // A null statement indicates that execution "ran off the bottom" of the program.
                // While a real MIPS device would keep chugging along and executing garbage data as instructions,
                // it's probably safe to say the user did not intend that to happen, so we'll just stop instead.
                this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.RAN_OFF_BOTTOM, null);
                return;
            }

            if (this.simulator.isInDelaySlot()) {
                // Handle the delayed jump/branch instead of incrementing the program counter
                Processor.setProgramCounter(this.simulator.getDelayedJumpAddress());
                this.simulator.clearDelayedJumpAddress();
            }
            else {
                // Increment the program counter register before doing anything else.
                // The reason for this is that the program counter register will always hold the address of
                // the NEXT instruction to execute, whereas this.programCounter holds the address of the CURRENT
                // instruction being executed. This allows branch/jump instructions to simply write to the register,
                // and no special logic is needed for whether to increment the program counter.
                Processor.setProgramCounter(this.programCounter + Instruction.BYTES_PER_INSTRUCTION);
            }

            try {
                // Handle external interrupt if necessary
                Integer externalInterruptDevice = this.simulator.checkExternalInterruptDevice();
                if (externalInterruptDevice != null) {
                    throw new SimulatorException(statement, "external interrupt", externalInterruptDevice);
                }

                // Simulate the statement execution
                statement.simulate();

                // IF statement added 7/26/06 (explanation above)
                if (this.simulator.getBackStepper().isEnabled()) {
                    this.simulator.getBackStepper().addDoNothing(this.programCounter);
                }
            }
            catch (SimulatorException exception) {
                // If execution were to terminate at this point, we don't want the program counter
                // to appear as if it was incremented past the instruction that caused the termination,
                // so we will just undo the incrementation of the program counter
                Processor.setProgramCounter(this.programCounter);

                if (exception.getExitCode() != null) {
                    // There are no errors attached, so this was caused by an exit syscall
                    this.simulator.dispatchFinishEvent(this.programCounter, SimulatorFinishEvent.Reason.EXIT_SYSCALL, exception);
                    return;
                }

                // Check for an exception handler by attempting to fetch the instruction located at the
                // exception handler address, as determined by the memory configuration
                int exceptionHandlerAddress = Memory.getInstance().getAddress(MemoryConfigurations.EXCEPTION_HANDLER);
                BasicStatement exceptionHandler = null;
                try {
                    exceptionHandler = Memory.getInstance().fetchStatement(exceptionHandlerAddress, true);
                }
                catch (AddressErrorException ignored) {
                    // Will only occur if the exception handler address is improperly configured
                }
                // Whether the fetched instruction was null indicates whether an exception handler exists
                if (exceptionHandler != null) {
                    // Found an exception handler, so jump to the handler address
                    Processor.setProgramCounter(exceptionHandlerAddress);
                }
                else {
                    // Did not find an exception handler, so terminate the program
                    throw exception;
                }
            }
            catch (InterruptedException exception) {
                // The instruction was interrupted in the middle of what it was doing,
                // and it should have already reverted all of its own changes, so all we need to do
                // to allow a pause interrupt to work is undo the incrementation of the program counter
                Processor.setProgramCounter(this.programCounter);
                // Proceed with interrupt handling as usual
                throw exception;
            }

            // Check for a thread interrupt (either a pause or termination)
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            // Check whether the step limit has been reached (if it is set)
            if (this.maxSteps > 0) {
                stepCount++;
                if (stepCount >= this.maxSteps) {
                    this.simulator.dispatchPauseEvent(this.maxSteps, this.programCounter, SimulatorPauseEvent.Reason.STEP_LIMIT_REACHED);
                    return;
                }
            }
            // Check for a breakpoint
            if (this.breakPoints != null && Arrays.binarySearch(this.breakPoints, this.programCounter) >= 0) {
                this.simulator.dispatchPauseEvent(this.maxSteps, this.programCounter, SimulatorPauseEvent.Reason.BREAKPOINT);
                return;
            }

            // Carry out any state changes meant to happen between instructions
            this.simulator.flushStateChanges();

            // Update the actual program counter to reflect the value stored in the register
            this.programCounter = Processor.getProgramCounter();

            // Update the GUI and delay the next step if the program is not running at unlimited speed
            if (this.simulator.isLimitingRunSpeed()) {
                // Schedule a GUI update if one is not already scheduled
                this.simulator.dispatchStepEvent();

                // Wait according to the speed setting (division is fine here since it should never be 0)
                Thread.sleep((long) (1000.0 / this.simulator.getRunSpeed()));
            }
        }
    }

    private BasicStatement fetchStatement() throws SimulatorException {
        try {
            return Memory.getInstance().fetchStatement(Processor.getProgramCounter(), true);
        }
        catch (AddressErrorException exception) {
            // Next statement is a hack.  Previous statement sets EPC register to (PC - 4)
            // because it assumes the bad address comes from an operand so the program counter has already been
            // incremented.  In this case, bad address is the instruction fetch itself so program counter has
            // not yet been incremented.  We'll set the EPC directly here.  DPS 8-July-2013
            Coprocessor0.updateRegister(Coprocessor0.EPC, Processor.getProgramCounter());
            throw new SimulatorException("invalid program counter value: " + Binary.intToHexString(Processor.getProgramCounter()));
        }
    }
}
