package mars.settings;

import mars.util.Binary;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Class representing a persistent color setting and its current value.
 */
public class ColorSetting {
    private final Settings settings;
    private final String key;
    private final String uiKey;
    private Color defaultValue;
    private Color value;
    private final boolean notifies;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // This setting works differently from most; the default value is derived from UI properties, so the base UI
    // key is stored rather than the default value itself.
    ColorSetting(Settings settings, String key, String uiKey, boolean notifies) {
        this.settings = settings;
        this.key = key;
        this.uiKey = uiKey;
        this.defaultValue = null;
        this.value = null;
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
     * Get the theme default value for this setting.
     *
     * @return The theme default color value for this setting.
     */
    public Color getDefault() {
        return this.defaultValue;
    }

    public void updateDefault() {
        // This is a hack to make the default value of the setting a java.awt.Color, not a
        // javax.swing.plaf.ColorUIResource. For whatever reason, some Swing components treat UIResource objects
        // differently-- for example, if the background color of a table cell is a UIResource, it can be overridden
        // by the alternate even row background color. This is not the desired behavior.
        Color uiDefault = UIManager.getColor(this.uiKey);
        this.defaultValue = (uiDefault == null) ? null : new Color(uiDefault.getRGB(), true);
    }

    /**
     * Get the value currently stored in this setting if it is not <code>null</code>, or the theme default
     * returned by {@link #getDefault()} otherwise.
     *
     * @return The color value for this setting, or the theme default if not set.
     */
    public Color getOrDefault() {
        return (this.value == null) ? this.getDefault() : this.value;
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
