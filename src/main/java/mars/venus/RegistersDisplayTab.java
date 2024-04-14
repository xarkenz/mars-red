package mars.venus;

import mars.Application;

public interface RegistersDisplayTab {
    /**
     * Update register display using current number base (10 or 16).
     */
    default void updateRegisters() {
        updateRegisters(Application.getGUI().getMainPane().getExecuteTab().getValueDisplayBase());
    }

    /**
     * Update register display using specified number base (10 or 16).
     *
     * @param base Desired number base.
     */
    void updateRegisters(int base);
}
