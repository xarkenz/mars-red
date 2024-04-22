package mars.simulator;

import mars.ProcessingException;

/**
 * Event for when the simulator stops execution of a program due to termination or finishing.
 *
 * @param maxSteps       The maximum number of steps the simulator took (-1 if not applicable).
 * @param programCounter The value of the program counter when the program finished or terminated.
 * @param reason         The reason why the program finished or terminated.
 * @param exception      The exception causing the program to terminate (only applies to {@link Reason#EXCEPTION}).
 */
public record SimulatorFinishEvent(int maxSteps, int programCounter, Reason reason, ProcessingException exception) {
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
}
