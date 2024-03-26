package mars.settings;

import java.util.Objects;

/**
 * Class representing a persistent string setting and its current value.
 */
public class StringSetting {
    private final Settings settings;
    private final String key;
    private String defaultValue;
    private String value;
    private final boolean notifies;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // The default value will almost always be overridden immediately, but it serves as
    // a backup in case both Preferences and the defaults file fail to load.
    StringSetting(Settings settings, String key, String defaultValue, boolean notifies) {
        this.settings = settings;
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.notifies = notifies;
    }

    /**
     * Get the key used to identify this setting in {@link java.util.prefs.Preferences}.
     *
     * @return The string key for this setting.
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Get the value currently stored in this setting.
     *
     * @return The string value for this setting.
     */
    public String get() {
        return this.value;
    }

    /**
     * Set the value of this setting, updating persistent storage.
     *
     * @param value The new string value for this setting.
     */
    public void set(String value) {
        if (!Objects.equals(value, this.value)) {
            // Value has changed, write it to persistent storage
            settings.saveStringSetting(this.key, this.value, this.notifies);
        }
        this.value = value;
    }

    /**
     * Get the default value for this setting.
     *
     * @return The default string value for this setting.
     */
    public String getDefault() {
        return this.defaultValue;
    }

    /**
     * Set the default value for this setting without updating the current value.
     *
     * @param value The new default string value for this setting.
     */
    public void setDefault(String value) {
        this.defaultValue = value;
    }

    /**
     * Temporarily set the value of this setting. The new value will NOT be written to persistent
     * storage. Currently, this is used only when running MARS from the command line.
     *
     * @param value The new string value for this setting.
     */
    public void setNonPersistent(String value) {
        this.value = value;
    }
}
