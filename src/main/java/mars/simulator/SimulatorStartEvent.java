package mars.simulator;

import java.util.EventObject;

/**
 * Event for when the simulator begins execution of a program.
 * Note that the return value of {@link #getSource()} will always be a {@link Simulator}.
 */
public class SimulatorStartEvent extends EventObject {
    private final int stepCount;

    /**
     * Construct a new event with the given parameters.
     *
     * @param simulator The source of this event.
     * @param stepCount The maximum number of steps the simulator will take (-1 if not applicable).
     */
    public SimulatorStartEvent(Simulator simulator, int stepCount) {
        super(simulator);
        this.stepCount = stepCount;
    }

    /**
     * The maximum number of steps the simulator will take (-1 if not applicable).
     */
    public int getStepCount() {
        return this.stepCount;
    }
}
