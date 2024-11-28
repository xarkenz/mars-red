package mars.settings;

import mars.venus.editor.jeditsyntax.SyntaxStyle;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class representing a persistent syntax style setting and its current value.
 */
public class SyntaxStyleSetting {
    private final Settings settings;
    private final String key;
    private final String uiKey;
    private SyntaxStyle value;
    private final boolean notifies;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // This setting works differently from most; the default value is derived from UI properties, so the base UI
    // key is stored rather than the default value itself.
    SyntaxStyleSetting(Settings settings, String key, String uiKey, boolean notifies) {
        this.settings = settings;
        this.key = key;
        this.uiKey = uiKey;
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
     * @return The syntax style value for this setting.
     */
    public SyntaxStyle get() {
        return this.value;
    }

    /**
     * Set the value of this setting, updating persistent storage.
     *
     * @param value The new syntax style value for this setting.
     */
    public void set(SyntaxStyle value) {
        if (!Objects.equals(value, this.value)) {
            this.value = value;
            // Value has changed, write it to persistent storage
            this.settings.saveStringSetting(this.key, SyntaxStyleSetting.encode(this.value), this.notifies);
        }
    }

    /**
     * Get the theme default value for this setting.
     *
     * @return The theme default syntax style value for this setting.
     */
    public SyntaxStyle getDefault() {
        Color foreground = UIManager.getColor(this.uiKey + ".foreground");
        boolean italic = UIManager.getBoolean(this.uiKey + ".italic");
        boolean bold = UIManager.getBoolean(this.uiKey + ".bold");
        return new SyntaxStyle(foreground, italic, bold);
    }

    /**
     * Get the value currently stored in this setting if it is not <code>null</code>, or the theme default
     * returned by {@link #getDefault()} otherwise.
     *
     * @return The syntax style value for this setting, or the theme default if not set.
     */
    public SyntaxStyle getOrDefault() {
        return (this.value == null) ? this.getDefault() : this.value;
    }

    /**
     * Set the value of this setting without updating persistent storage.
     *
     * @param value The new syntax style value for this setting.
     */
    public void setNonPersistent(SyntaxStyle value) {
        this.value = value;
    }

    /**
     * Encode a {@link SyntaxStyle} object as a string of attributes separated by commas:
     * <ul>
     * <li><code>Color=RGB</code>: Specifies the color as an integer <code>RGB</code>.
     * <li><code>Bold</code>: Will be present if the bold style flag is set.
     * <li><code>Italic</code>: Will be present if the italic style flag is set.
     * </ul>
     *
     * @param style The syntax style to encode as a string.
     * @return The encoded form of the given syntax style, or null if the style is null.
     * @see ColorSetting#encode(Color)
     * @see SyntaxStyleSetting#decode(String)
     */
    public static String encode(SyntaxStyle style) {
        if (style == null) {
            return null;
        }

        List<String> attributes = new ArrayList<>();
        attributes.add("Color=" + ColorSetting.encode(style.getForeground()));
        if (style.isBold()) {
            attributes.add("Bold");
        }
        if (style.isItalic()) {
            attributes.add("Italic");
        }

        return String.join(",", attributes);
    }

    /**
     * Decode a {@link SyntaxStyle} object from a string of attributes separated by commas:
     * <ul>
     * <li><code>Color=RGB</code>: Sets the color to integer <code>RGB</code>. If not present, defaults to null.
     * <li><code>Bold</code>: If present, sets the bold style flag.
     * <li><code>Italic</code>: If present, sets the italic style flag.
     * </ul>
     *
     * @param string The string to decode as a syntax style.
     * @return The syntax style decoded from the given string, or null if <code>string</code> is null.
     * @see ColorSetting#decode(String)
     * @see SyntaxStyleSetting#encode(SyntaxStyle)
     */
    public static SyntaxStyle decode(String string) {
        if (string == null) {
            return null;
        }

        Color color = null;
        boolean bold = false;
        boolean italic = false;

        for (String attribute : string.split(",")) {
            attribute = attribute.strip().toLowerCase();

            if (attribute.startsWith("color=")) {
                color = ColorSetting.decode(attribute.substring(6).strip());
            }
            else if (attribute.equals("bold")) {
                bold = true;
            }
            else if (attribute.equals("italic")) {
                italic = true;
            }
        }

        return new SyntaxStyle(color, italic, bold);
    }
}
