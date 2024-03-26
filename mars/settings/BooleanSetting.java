package mars.settings;

/**
 * Class representing a persistent boolean setting and its current value.
 */
public class BooleanSetting {
    private final Settings settings;
    private final boolean notifies;
    private final String key;
    private boolean defaultValue;
    private boolean value;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // The default value will almost always be overridden immediately, but it serves as
    // a backup in case both Preferences and the defaults file fail to load.
    BooleanSetting(Settings settings, String key, boolean defaultValue, boolean notifies) {
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
     * @return The boolean value for this setting.
     */
    public boolean get() {
        return this.value;
    }

    /**
     * Set the value of this setting, updating persistent storage.
     *
     * @param value The new boolean value for this setting.
     */
    public void set(boolean value) {
        if (value != this.value) {
            // Value has changed, write it to persistent storage
            settings.saveBooleanSetting(this.key, this.value, this.notifies);
        }
        this.value = value;
    }

    /**
     * Get the default value for this setting.
     *
     * @return The default boolean value for this setting.
     */
    public boolean getDefault() {
        return this.defaultValue;
    }

    /**
     * Set the default value for this setting without updating the current value.
     *
     * @param value The new default boolean value for this setting.
     */
    public void setDefault(boolean value) {
        this.defaultValue = value;
    }

    /**
     * Temporarily set the value of this setting. The new value will NOT be written to persistent
     * storage. Currently, this is used only when running MARS from the command line.
     *
     * @param value The new boolean value for this setting.
     */
    public void setNonPersistent(boolean value) {
        this.value = value;
    }
}
