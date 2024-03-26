package mars.settings;

import mars.util.EditorFont;

import java.awt.*;
import java.util.Objects;

/**
 * Class representing a persistent font setting and its current value.
 */
public class FontSetting {
    private final Settings settings;
    private final String key;
    private String family;
    private String style;
    private String size;
    private final boolean notifies;

    // Package-private visibility; the constructor usage should be restricted to Settings.
    // The initial values will almost always be overridden immediately, but they serve as
    // a backup in case both Preferences and the defaults file fail to load.
    FontSetting(Settings settings, String key, String initialFamily, String initialStyle, String initialSize, boolean notifies) {
        this.settings = settings;
        this.key = key;
        this.family = initialFamily;
        this.style = initialStyle;
        this.size = initialSize;
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
     * @return The value for this setting, as a font.
     */
    public Font get() {
        return EditorFont.createFontFromStringValues(this.family, this.style, this.size);
    }

    /**
     * Set the value of this setting, updating persistent storage.
     *
     * @param font The new font value for this setting.
     */
    public void set(Font font) {
        String family = font.getFamily();
        String style = EditorFont.getStyle(font.getStyle()).getName();
        String size = EditorFont.sizeIntToSizeString(font.getSize());
        this.set(family, style, size);
    }

    /**
     * Set the value of this setting, updating persistent storage.
     *
     * @param family The new font family value for this setting.
     * @param style  The new font style value for this setting.
     * @param size   The new font size value for this setting.
     */
    public void set(String family, String style, String size) {
        if (!Objects.equals(family, this.family) || !Objects.equals(style, this.style) || !Objects.equals(size, this.size)) {
            // Value has changed, write it to persistent storage
            settings.saveFontSetting(this.key, family, style, size, this.notifies);
            this.family = family;
            this.style = style;
            this.size = size;
        }
    }
}
