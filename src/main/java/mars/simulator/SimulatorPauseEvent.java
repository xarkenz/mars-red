package mars.simulator;

import java.util.EventObject;

/**
 * Event for when the simulator stops execution of a program due to pausing.
 * Note that the return value of {@link #getSource()} will always be a {@link Simulator}.
 */
public class SimulatorPauseEvent extends EventObject {
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

    private final int stepCount;
    private final Reason reason;

    /**
     * Construct a new event with the given parameters.
     *
     * @param simulator      The source of this event.
     * @param stepCount      The number of steps the simulator took (-1 if not applicable).
     * @param reason         The reason why execution paused.
     */
    public SimulatorPauseEvent(Simulator simulator, int stepCount, Reason reason) {
        super(simulator);
        this.stepCount = stepCount;
        this.reason = reason;
    }

    /**
     * The number of steps the simulator took (-1 if not applicable).
     */
    public int getStepCount() {
        return this.stepCount;
    }

    /**
     * The reason why execution paused.
     */
    public Reason getReason() {
        return this.reason;
    }
}
