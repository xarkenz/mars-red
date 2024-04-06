package mars.settings;

import mars.util.Binary;

import java.awt.*;
import java.util.Objects;

/**
 * Class representing a persistent color setting and its current value.
 */
public class ColorSetting {
    private final Settings settings;
    private final String key;
    private Color color;
    private final boolean notifies;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // The initialValue will almost always be overridden immediately, but it serves as
    // a backup in case both Preferences and the defaults file fail to load.
    ColorSetting(Settings settings, String key, Color initialColor, boolean notifies) {
        this.settings = settings;
        this.key = key;
        this.color = initialColor;
        this.notifies = notifies;
    }

    /**
     * Get the string key used to identify this setting in {@link java.util.prefs.Preferences}.
     *
     * @return The key for this setting.
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Get the value currently stored in this setting.
     *
     * @return The value for this setting, as a color.
     */
    public Color get() {
        return this.color;
    }

    /**
     * Set the value of this setting, updating persistent storage.
     *
     * @param color The new color value for this setting.
     */
    public void set(Color color) {
        if (!Objects.equals(color, this.color)) {
            // Value has changed, write it to persistent storage
            String value = Binary.intToHexString(color.getRed() << 16 | color.getGreen() << 8 | color.getBlue());
            settings.saveStringSetting(this.key, value, this.notifies);
            this.color = color;
        }
    }
}
