package mars.simulator;

/**
 * Event for when the simulator begins execution of a program.
 *
 * @param maxSteps       The maximum number of steps the simulator will take (-1 if not applicable).
 * @param programCounter The initial value of the program counter.
 */
public record SimulatorStartEvent(int maxSteps, int programCounter) {
}
