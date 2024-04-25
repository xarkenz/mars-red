package mars.venus;

import mars.Application;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterAccessNotice;
import mars.simulator.*;
import mars.venus.execute.RunSpeedPanel;

import javax.swing.*;
import java.util.Observable;
import java.util.Observer;

public abstract class RegistersDisplayTab extends JPanel implements SimulatorListener, Observer {
    public RegistersDisplayTab() {
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
        this.updateRegisters(Application.getGUI().getMainPane().getExecuteTab().getValueDisplayBase());
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

    /**
     * Required by Observer interface.  Called when notified by an Observable that we are registered with.
     * Observables include:
     * A register object, which lets us know of register operations
     * The Simulator keeps us informed of when simulated MIPS execution is active.
     * This is the only time we care about register operations.
     *
     * @param observable The Observable object who is notifying us
     * @param obj        Auxiliary object with additional information.
     */
    @Override
    public void update(Observable observable, Object obj) {
        if (obj instanceof RegisterAccessNotice access) {
            // Note: each register is a separate Observable
            if (access.getAccessType() == AccessNotice.WRITE) {
                if (RunSpeedPanel.getInstance().getRunSpeed() != RunSpeedPanel.UNLIMITED_SPEED) {
                    this.getTable().setUpdating(true);
                    this.highlightRegister((Register) observable);
                    Application.getGUI().getRegistersPane().setSelectedComponent(this);
                }
                else {
                    this.getTable().setUpdating(false);
                }
            }
        }
    }
}
