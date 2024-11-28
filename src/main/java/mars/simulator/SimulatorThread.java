package mars.simulator;

import mars.assembler.BasicStatement;
import mars.mips.hardware.*;
import mars.mips.instructions.Instruction;

import java.util.Arrays;

/**
 * {@link Thread} subclass to perform MIPS simulation in the background. The thread can be interrupted using either
 * {@link Simulator#pause()} or {@link Simulator#terminate()}.
 *
 * @author Pete Sanderson, August 2005; Sean Clarke, April 2024
 */
public class SimulatorThread extends Thread {
    private final Simulator simulator;
    private final int maxSteps;
    private final int[] breakPoints;
    private volatile Runnable stopEventDispatcher;
    private volatile int nextFetchPC;

    /**
     * Create a new <code>SimulatorThread</code> without starting it.
     *
     * @param maxSteps       Maximum number of instruction steps to simulate.  Default of -1 means no maximum.
     * @param breakPoints    Array of breakpoints (instruction addresses) specified by user.
     */
    public SimulatorThread(Simulator simulator, int maxSteps, int[] breakPoints) {
        super("MIPS");
        this.simulator = simulator;
        this.maxSteps = maxSteps;
        this.breakPoints = breakPoints;
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

    public void processJump(int nextFetchPC) {
        this.nextFetchPC = nextFetchPC;
    }

    private void dispatchExternalPauseEvent() {
        // Dispatch a pause event once the simulator stops
        this.simulator.dispatchPauseEvent(this.maxSteps, SimulatorPauseEvent.Reason.EXTERNAL);
    }

    private void dispatchExternalFinishEvent() {
        // Dispatch a finish event once the simulator stops
        this.simulator.dispatchFinishEvent(SimulatorFinishEvent.Reason.EXTERNAL, null);
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
            this.simulator.dispatchFinishEvent(SimulatorFinishEvent.Reason.EXCEPTION, exception);
        }
        catch (InterruptedException exception) {
            // The event dispatcher runnable will be set by the method that caused the interrupt
            this.stopEventDispatcher.run();
        }
        catch (Exception exception) {
            // Should only happen if there is a bug somewhere
            System.err.println("Error: internal exception during simulation (this is a bug!):");
            exception.printStackTrace(System.err);
            this.simulator.dispatchFinishEvent(SimulatorFinishEvent.Reason.INTERNAL_ERROR, null);
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

        this.simulator.dispatchStartEvent(this.maxSteps);

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
            // Prepare the next value for the program counter. If overridden by the instruction being executed,
            // that value will be used instead. This is set up to act like a multiplexer in hardware.
            this.nextFetchPC = Processor.getProgramCounter() + Instruction.BYTES_PER_INSTRUCTION;

            // Fetch the statement to execute from memory
            BasicStatement statement = this.fetchStatement();
            if (statement == null) {
                // A null statement indicates that execution "ran off the bottom" of the program.
                // While a real MIPS device would keep chugging along and executing garbage data as instructions,
                // it's probably safe to say the user did not intend that to happen, so we'll just stop instead.
                this.simulator.dispatchFinishEvent(SimulatorFinishEvent.Reason.RAN_OFF_BOTTOM, null);
                return;
            }

            try {
                // Handle external interrupt if necessary
                Integer externalInterruptDevice = this.simulator.checkExternalInterruptDevice();
                if (externalInterruptDevice != null) {
                    throw new SimulatorException(statement, "external interrupt", externalInterruptDevice);
                }

                // Simulate the statement execution
                statement.simulate();
            }
            catch (SimulatorException exception) {
                if (exception.getExitCode() != null) {
                    // There are no errors attached, so this was caused by an exit syscall
                    this.simulator.dispatchFinishEvent(SimulatorFinishEvent.Reason.EXIT_SYSCALL, exception);
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
                    // Found an exception handler, so invalidate the pipeline and jump to the handler address.
                    // To simulate invalidating the pipeline, skip past the step that would have happened
                    // in a typical jump where an extra instruction would be executed in the delay slot.
                    this.nextFetchPC = exceptionHandlerAddress + Instruction.BYTES_PER_INSTRUCTION;
                    Processor.setProgramCounter(exceptionHandlerAddress);
                }
                else {
                    // Did not find an exception handler, so terminate the program
                    throw exception;
                }
            }

            // Update both the fetch program counter and execute program counter to simulate a pipeline
            Processor.incrementProgramCounter(this.nextFetchPC);

            // Carry out any state changes meant to happen between instructions
            this.simulator.flushStateChanges();

            // Mark the end of a step in the backstepper so all actions from this iteration are considered one step
            this.simulator.getBackStepper().finishStep();

            // END OF SIMULATOR STEP

            // Check for a thread interrupt (either a pause or termination)
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            // Check whether the step limit has been reached (if it is set)
            if (this.maxSteps > 0) {
                stepCount++;
                if (stepCount >= this.maxSteps) {
                    this.simulator.dispatchPauseEvent(this.maxSteps, SimulatorPauseEvent.Reason.STEP_LIMIT_REACHED);
                    return;
                }
            }
            // Check for a breakpoint
            if (this.breakPoints != null && Arrays.binarySearch(this.breakPoints, Processor.getExecuteProgramCounter()) >= 0) {
                this.simulator.dispatchPauseEvent(this.maxSteps, SimulatorPauseEvent.Reason.BREAKPOINT);
                return;
            }

            // Update the GUI and delay the next step if the program is not running at unlimited speed
            if (this.simulator.isLimitingRunSpeed()) {
                // Schedule a GUI update if one is not already scheduled
                this.simulator.dispatchStepEvent();

                // Wait according to the speed setting (division is fine here since it should never be 0)
                Thread.sleep(Math.round(1000.0 / this.simulator.getRunSpeed()));
            }
        }
    }

    private BasicStatement fetchStatement() throws SimulatorException {
        try {
            return Memory.getInstance().fetchStatement(Processor.getExecuteProgramCounter(), true);
        }
        catch (AddressErrorException exception) {
            Coprocessor0.updateRegister(Coprocessor0.EPC, Processor.getExecuteProgramCounter());
            throw new SimulatorException(exception);
        }
    }
}
