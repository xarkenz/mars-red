package mars.venus;

import mars.mips.hardware.Register;
import mars.simulator.*;

import javax.swing.*;

public abstract class RegistersDisplayTab extends JPanel implements SimulatorListener, Register.Listener {
    protected final VenusUI gui;

    public RegistersDisplayTab(VenusUI gui) {
        this.gui = gui;

        Simulator.getInstance().addGUIListener(this);
    }

    protected abstract RegistersTable getTable();

    /**
     * Clear highlight background color from any row currently highlighted.
     */
    public void clearHighlighting() {
        if (this.getTable() != null) {
            this.getTable().clearHighlighting();
        }
    }

    /**
     * Refresh the table, triggering re-rendering.
     */
    public void refresh() {
        if (this.getTable() != null) {
            this.getTable().refresh();
        }
    }

    /**
     * Update register display using current number base (10 or 16).
     */
    public void updateRegisters() {
        this.updateRegisters(this.gui.getMainPane().getExecuteTab().getValueDisplayBase());
    }

    /**
     * Update register display using specified number base (10 or 16).
     *
     * @param base Desired number base.
     */
    public abstract void updateRegisters(int base);

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register Register object corresponding to row to be selected.
     */
    public abstract void highlightRegister(Register register);

    public abstract void startObservingRegisters();

    public abstract void stopObservingRegisters();

    @Override
    public void simulatorStarted(SimulatorStartEvent event) {
        this.startObservingRegisters();
    }

    @Override
    public void simulatorPaused(SimulatorPauseEvent event) {
        this.stopObservingRegisters();
        this.updateRegisters();
        this.refresh();
    }

    @Override
    public void simulatorFinished(SimulatorFinishEvent event) {
        this.stopObservingRegisters();
        this.updateRegisters();
        this.refresh();
    }

    @Override
    public void simulatorStepped() {
        this.updateRegisters();
    }

    @Override
    public void registerWritten(Register register) {
        if (Simulator.getInstance().isLimitingRunSpeed()) {
            this.getTable().setUpdating(true);
            this.highlightRegister(register);
            this.gui.getRegistersPane().setSelectedComponent(this);
        }
        else {
            this.getTable().setUpdating(false);
        }
    }
}
