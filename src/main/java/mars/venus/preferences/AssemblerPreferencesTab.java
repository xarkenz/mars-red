package mars.venus.preferences;

import mars.settings.Settings;
import mars.venus.preferences.components.CheckBoxPreference;

public class AssemblerPreferencesTab extends PreferencesTab {
    private final CheckBoxPreference startAtMain;
    private final CheckBoxPreference compatibilityWarnings;
    private final CheckBoxPreference warningsAreErrors;
    private final CheckBoxPreference extendedAssembler;
    private final CheckBoxPreference delayedBranching;

    public AssemblerPreferencesTab(Settings settings) {
        super(settings, "Assembler");

        this.addRow(this.startAtMain = new CheckBoxPreference(
            this.settings.startAtMain,
            "Use global label \"main\" as the program entry point, if defined",
            "When enabled and the assembler finds a global label \"main\", the program counter will be "
            + "initialized to the address of that label. Otherwise, the program counter starts at the beginning "
            + "of the \".text\" segment. Enabled by default."
        ));
        this.addRow(this.compatibilityWarnings = new CheckBoxPreference(
            this.settings.compatibilityWarningsEnabled,
            "Enable compatibility warnings for MARS 4.5",
            "When enabled, the assembler will generate a warning whenever a language feature is used that is not "
            + "compatible with MARS 4.5. Enabled by default."
        ));
        this.addRow(this.warningsAreErrors = new CheckBoxPreference(
            this.settings.warningsAreErrors,
            "Promote assembler warnings to errors",
            "When enabled, assembler warnings will be generated as errors and prevent successful assembly. "
            + "Disabled by default."
        ));
        this.addRow(this.extendedAssembler = new CheckBoxPreference(
            this.settings.extendedAssemblerEnabled,
            "Allow extended instructions (pseudoinstructions)",
            "When enabled, MIPS programs are permitted to use the extended instructions MARS provides, "
            + "which expand to one or more basic (machine) instructions during assembly. Enabled by default."
        ));
        this.addRow(this.delayedBranching = new CheckBoxPreference(
            this.settings.delayedBranchingEnabled,
            "Enable delayed branching (disable automatic delay slot \"nop\"s)",
            "Unless enabled, the assembler automatically fills the delay slot of branches and jumps "
            + "(that is, the instruction immediately following a branch or jump which is always executed, "
            + "regardless of whether the branch or jump is taken) with a \"nop\" instruction. Disabled by default."
        ));
    }

    @Override
    public void applyChanges() {
        this.startAtMain.apply();
        this.compatibilityWarnings.apply();
        this.warningsAreErrors.apply();
        this.extendedAssembler.apply();
        this.delayedBranching.apply();
    }

    @Override
    public void revertChanges() {
        this.startAtMain.revert();
        this.compatibilityWarnings.revert();
        this.warningsAreErrors.revert();
        this.extendedAssembler.revert();
        this.delayedBranching.revert();
    }
}
