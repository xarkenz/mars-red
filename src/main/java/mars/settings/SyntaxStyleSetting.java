package mars.settings;

import mars.venus.editor.jeditsyntax.SyntaxStyle;

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
    private SyntaxStyle defaultValue;
    private SyntaxStyle value;
    private final boolean notifies;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // The default value will almost always be overridden immediately, but it serves as
    // a backup in case both Preferences and the defaults file fail to load.
    SyntaxStyleSetting(Settings settings, String key, SyntaxStyle defaultValue, boolean notifies) {
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
     * Get the default value for this setting.
     *
     * @return The default syntax style value for this setting.
     */
    public SyntaxStyle getDefault() {
        return this.defaultValue;
    }

    /**
     * Set the default value for this setting without updating the current value.
     *
     * @param value The new default syntax style value for this setting.
     */
    public void setDefault(SyntaxStyle value) {
        this.defaultValue = value;
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
        attributes.add("Color=" + ColorSetting.encode(style.getColor()));
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
     * @return The syntax style decoded from the given string.
     * @see ColorSetting#decode(String)
     * @see SyntaxStyleSetting#encode(SyntaxStyle)
     */
    public static SyntaxStyle decode(String string) {
        Color color = null;
        boolean bold = false;
        boolean italic = false;

        if (string != null) {
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
        }

        return new SyntaxStyle(color, italic, bold);
    }
}
