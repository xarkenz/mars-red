package mars.settings;

/**
 * Class representing a persistent integer setting and its current value.
 */
public class IntegerSetting {
    private final Settings settings;
    private final boolean notifies;
    private final String key;
    private int defaultValue;
    private int value;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // The default value will almost always be overridden immediately, but it serves as
    // a backup in case both Preferences and the defaults file fail to load.
    IntegerSetting(Settings settings, String key, int defaultValue, boolean notifies) {
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
     * @return The integer value for this setting.
     */
    public int get() {
        return this.value;
    }

    /**
     * Set the value of this setting, updating persistent storage.
     *
     * @param value The new integer value for this setting.
     */
    public void set(int value) {
        if (value != this.value) {
            this.value = value;
            // Value has changed, write it to persistent storage
            this.settings.saveIntegerSetting(this.key, this.value, this.notifies);
        }
    }

    /**
     * Get the default value for this setting.
     *
     * @return The default integer value for this setting.
     */
    public int getDefault() {
        return this.defaultValue;
    }

    /**
     * Set the default value for this setting without updating the current value.
     *
     * @param value The new default integer value for this setting.
     */
    public void setDefault(int value) {
        this.defaultValue = value;
    }

    /**
     * Set the value of this setting without updating persistent storage.
     *
     * @param value The new integer value for this setting.
     */
    public void setNonPersistent(int value) {
        this.value = value;
    }
}
