package mars.venus.preferences;

import mars.settings.Settings;
import mars.venus.preferences.components.CheckBoxPreference;

public class MemoryPreferencesTab extends PreferencesTab {
    private final CheckBoxPreference useBigEndian;

    public MemoryPreferencesTab(Settings settings) {
        super(settings, "Memory");

        this.addRow(this.useBigEndian = new CheckBoxPreference(
            this.settings.useBigEndian,
            "Use big-endian byte ordering",
            "When enabled, the bytes in a word will be ordered from most to least significant. "
            + "By default, the bytes in a word are ordered from least to most significant (little-endian)."
        ));
    }

    @Override
    public void applyChanges() {
        this.useBigEndian.apply();
    }

    @Override
    public void revertChanges() {
        this.useBigEndian.revert();
    }
}
