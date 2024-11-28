package mars.simulator;

import java.util.EventObject;

/**
 * Event for when the simulator stops execution of a program due to termination or finishing.
 * Note that the return value of {@link #getSource()} will always be a {@link Simulator}.
 */
public class SimulatorFinishEvent extends EventObject {
    /**
     * Enumeration of reasons for why the simulator might finish or terminate.
     */
    public enum Reason {
        /**
         * An exception occurred, causing the program to terminate.
         */
        EXCEPTION,
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
         * An unhandled internal error occurred during execution,
         * causing the simulator thread to terminate prematurely.
         */
        INTERNAL_ERROR,
        /**
         * The program was terminated by something outside of the simulator.
         * This is usually caused by the Stop action.
         */
        EXTERNAL,
    }

    private final Reason reason;
    private final SimulatorException exception;

    /**
     * Construct a new event with the given parameters.
     *
     * @param simulator      The source of this event.
     * @param reason         The reason why the program finished or terminated.
     * @param exception      The exception causing the program to terminate (only applies to {@link Reason#EXCEPTION}).
     */
    public SimulatorFinishEvent(Simulator simulator, Reason reason, SimulatorException exception) {
        super(simulator);
        this.reason = reason;
        this.exception = exception;
    }

    /**
     * The reason why the program finished or terminated.
     */
    public Reason getReason() {
        return this.reason;
    }

    /**
     * The exception causing the program to terminate (only applies to {@link Reason#EXCEPTION}).
     */
    public SimulatorException getException() {
        return this.exception;
    }
}
