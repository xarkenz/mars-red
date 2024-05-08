package mars.settings;

import mars.util.Binary;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class representing a persistent font setting and its current value.
 */
public class FontSetting {
    private final Settings settings;
    private final String key;
    private Font defaultValue;
    private Font value;
    private final boolean notifies;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // The default value will almost always be overridden immediately, but it serves as
    // a backup in case both Preferences and the defaults file fail to load.
    FontSetting(Settings settings, String key, Font defaultValue, boolean notifies) {
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
     * @return The font value for this setting.
     */
    public Font get() {
        return this.value;
    }

    /**
     * Set the value of this setting, updating persistent storage.
     *
     * @param value The new font value for this setting.
     */
    public void set(Font value) {
        if (!Objects.equals(value, this.value)) {
            this.value = value;
            // Value has changed, write it to persistent storage
            this.settings.saveStringSetting(this.key, FontSetting.encode(this.value), this.notifies);
        }
    }

    /**
     * Get the default value for this setting.
     *
     * @return The default font value for this setting.
     */
    public Font getDefault() {
        return this.defaultValue;
    }

    /**
     * Set the default value for this setting without updating the current value.
     *
     * @param value The new font string value for this setting.
     */
    public void setDefault(Font value) {
        this.defaultValue = value;
    }

    /**
     * Set the value of this setting without updating persistent storage.
     *
     * @param value The new font value for this setting.
     */
    public void setNonPersistent(Font value) {
        this.value = value;
    }

    /**
     * Encode a {@link Font} object as a string with the form <code>Name</code> or <code>Name;Attributes</code>,
     * where <code>Attributes</code> is a comma-separated list of optional attributes:
     * <ul>
     * <li><code>Bold</code>: Will be present if the bold style flag is set.
     * <li><code>Italic</code>: Will be present if the italic style flag is set.
     * <li><code>Size=N</code>: Specifies the font point size <code>N</code>.
     * </ul>
     *
     * @param font The font to encode as a string.
     * @return The encoded form of the given font, or null if the font is null.
     * @see FontSetting#decode(String)
     */
    public static String encode(Font font) {
        if (font == null) {
            return null;
        }

        List<String> attributes = new ArrayList<>();
        if (font.isBold()) {
            attributes.add("Bold");
        }
        if (font.isItalic()) {
            attributes.add("Italic");
        }
        attributes.add("Size=" + font.getSize());

        return font.getName() + ";" + String.join(",", attributes);
    }

    /**
     * Decode a {@link Font} object from a string with the form <code>Name</code> or <code>Name;Attributes</code>,
     * where <code>Attributes</code> is a comma-separated list of optional attributes:
     * <ul>
     * <li><code>Bold</code>: If present, sets the bold style flag.
     * <li><code>Italic</code>: If present, sets the italic style flag.
     * <li><code>Size=N</code>: Sets the font point size to <code>N</code>. If not present, defaults to 12.
     * </ul>
     *
     * @param string The string to decode as a font.
     * @return The font decoded from the given string.
     * @see FontSetting#encode(Font)
     */
    public static Font decode(String string) {
        String name = Font.DIALOG;
        int style = Font.PLAIN;
        int size = 12;

        if (string == null) {
            return new Font(name, style, size);
        }

        int separator = string.indexOf(';');
        String parsedName = ((separator >= 0) ? string.substring(0, separator) : string).strip();
        if (!parsedName.isEmpty()) {
            name = parsedName;
        }

        if (separator >= 0) {
            for (String attribute : string.substring(separator + 1).split(",")) {
                attribute = attribute.strip().toLowerCase();

                if (attribute.equals("bold")) {
                    style |= Font.BOLD;
                }
                else if (attribute.equals("italic")) {
                    style |= Font.ITALIC;
                }
                else if (attribute.startsWith("size=")) {
                    try {
                        size = Integer.parseUnsignedInt(attribute.substring(5).strip());
                    }
                    catch (NumberFormatException exception) {
                        // Keep size at default
                    }
                }
            }
        }

        return new Font(name, style, size);
    }
}
