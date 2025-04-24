package mars.venus.preferences;

import mars.settings.Settings;

import javax.swing.*;

public class AssemblerPreferencesTab extends PreferencesTab {
    private final boolean initialCompatibilityWarningsEnabled;
    private final JCheckBox compatibilityWarningsEnabled;

    public AssemblerPreferencesTab(Settings settings) {
        super(settings, "Assembler");

        this.initialCompatibilityWarningsEnabled = this.settings.compatibilityWarningsEnabled.get();
        this.addRow(
            "Enable compatibility warnings",
            "When enabled, the assembler will generate a warning whenever a language feature is used that is not "
            + "compatible with MARS 4.5. Enabled by default.",
            this.compatibilityWarningsEnabled = new JCheckBox()
        );
        this.compatibilityWarningsEnabled.setSelected(this.initialCompatibilityWarningsEnabled);
    }

    @Override
    public void applyChanges() {
        if (this.compatibilityWarningsEnabled.isSelected() != this.settings.compatibilityWarningsEnabled.get()) {
            this.settings.compatibilityWarningsEnabled.set(this.compatibilityWarningsEnabled.isSelected());
        }
    }

    @Override
    public void revertChanges() {
        this.compatibilityWarningsEnabled.setSelected(this.initialCompatibilityWarningsEnabled);
    }
}
