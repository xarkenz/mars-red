package mars.settings;

import mars.util.Binary;

import java.awt.*;
import java.util.Objects;

/**
 * Class representing a persistent color setting and its current value.
 */
public class ColorSetting {
    private final Settings settings;
    private final boolean notifies;
    private final String key;
    private Color defaultValue;
    private Color value;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // The default value will almost always be overridden immediately, but it serves as
    // a backup in case both Preferences and the defaults file fail to load.
    ColorSetting(Settings settings, String key, Color defaultValue, boolean notifies) {
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
     * @return The color value for this setting.
     */
    public Color get() {
        return this.value;
    }

    /**
     * Set the value of this setting, updating persistent storage.
     *
     * @param value The new color value for this setting.
     */
    public void set(Color value) {
        if (!Objects.equals(value, this.value)) {
            this.value = value;
            // Value has changed, write it to persistent storage
            this.settings.saveStringSetting(this.key, ColorSetting.encode(this.value), this.notifies);
        }
    }

    /**
     * Get the default value for this setting.
     *
     * @return The default color value for this setting.
     */
    public Color getDefault() {
        return this.defaultValue;
    }

    /**
     * Set the default value for this setting without updating the current value.
     *
     * @param value The new default color value for this setting.
     */
    public void setDefault(Color value) {
        this.defaultValue = value;
    }

    /**
     * Set the value of this setting without updating persistent storage.
     *
     * @param value The new color value for this setting.
     */
    public void setNonPersistent(Color value) {
        this.value = value;
    }

    /**
     * Encode a {@link Color} object as a string containing a hexadecimal integer with a leading <code>0x</code>.
     *
     * @param color The color to encode as a string.
     * @return The encoded form of the given color.
     * @see ColorSetting#decode(String)
     */
    public static String encode(Color color) {
        return Binary.intToHexString(color.getRGB() & 0xFFFFFF);
    }

    /**
     * Decode a {@link Color} object from a string containing an integer in some format.
     *
     * @param string The string to decode as a color.
     * @return The color decoded from the given string, or null if it could not be decoded.
     * @see Binary#decodeInteger(String)
     * @see ColorSetting#encode(Color)
     */
    public static Color decode(String string) {
        if (string == null) {
            return null;
        }

        try {
            return new Color(Binary.decodeInteger(string));
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }
}
