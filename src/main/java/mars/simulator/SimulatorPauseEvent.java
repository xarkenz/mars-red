package mars.simulator;

/**
 * Event for when the simulator stops execution of a program due to pausing.
 *
 * @param maxSteps       The maximum number of steps the simulator took (-1 if not applicable).
 * @param programCounter The value of the program counter when the pause occurred.
 * @param reason         The reason why execution paused.
 */
public record SimulatorPauseEvent(int maxSteps, int programCounter, Reason reason) {
    /**
     * Enumeration of reasons for why the simulator might pause.
     */
    public enum Reason {
        /**
         * The requested number of steps have completed successfully, causing execution to pause.
         * This is also used when stepping one step at a time.
         */
        STEP_LIMIT_REACHED,
        /**
         * A breakpoint was reached, causing execution to pause.
         */
        BREAKPOINT,
        /**
         * Execution was paused by something outside of the simulator.
         * This is usually caused by the Pause action.
         */
        EXTERNAL,
    }
}
