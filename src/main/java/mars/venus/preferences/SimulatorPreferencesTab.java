package mars.venus.preferences;

import mars.settings.Settings;
import mars.venus.preferences.components.CheckBoxPreference;

public class SimulatorPreferencesTab extends PreferencesTab {
    private final CheckBoxPreference selfModifyingCode;
    private final CheckBoxPreference popupSyscallInput;

    public SimulatorPreferencesTab(Settings settings) {
        super(settings, "Simulator");

        this.addRow(this.selfModifyingCode = new CheckBoxPreference(
            this.settings.selfModifyingCodeEnabled,
            "Allow self-modifying code",
            "When enabled, the MIPS program is allowed to write to text segments and execute instructions "
            + "in data segments. Disabled by default, as these actions are often unintentional."
        ));
        this.addRow(this.popupSyscallInput = new CheckBoxPreference(
            this.settings.popupSyscallInput,
            "Show dialog for user input system calls",
            "When enabled, the input syscalls (5, 6, 7, 8, 12) will display a dialog prompt instead of "
            + "allowing the user to enter input into the console directly."
        ));
    }

    @Override
    public void applyChanges() {
        this.selfModifyingCode.apply();
        this.popupSyscallInput.apply();
    }

    @Override
    public void revertChanges() {
        this.selfModifyingCode.revert();
        this.popupSyscallInput.revert();
    }
}
