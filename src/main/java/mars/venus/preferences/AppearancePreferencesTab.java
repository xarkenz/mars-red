package mars.venus.preferences;

import mars.settings.Settings;
import mars.venus.FontChooserPane;

import javax.swing.*;

public class AppearancePreferencesTab extends PreferencesTab {
    public enum Theme {
        DEFAULT_LIGHT("Default Light", "FlatLightLaf"),
        DEFAULT_DARK("Default Dark", "FlatDarkLaf");

        public static Theme fromLookAndFeelName(String lookAndFeelName) {
            return switch (lookAndFeelName) {
                case "FlatLightLaf" -> DEFAULT_LIGHT;
                case "FlatDarkLaf" -> DEFAULT_DARK;
                default -> null;
            };
        }

        private final String displayName;
        private final String lookAndFeelName;

        Theme(String displayName, String lookAndFeelName) {
            this.displayName = displayName;
            this.lookAndFeelName = lookAndFeelName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getLookAndFeelName() {
            return this.lookAndFeelName;
        }

        @Override
        public String toString() {
            return this.getDisplayName();
        }
    }

    private final Theme initialTheme;
    private final JComboBox<Theme> themeChooser;
    private final FontChooserPane editorFontChooser;
    private final FontChooserPane tableFontChooser;
    private final FontChooserPane tableHighlightFontChooser;
    private final FontChooserPane consoleFontChooser;

    public AppearancePreferencesTab(Settings settings) {
        super(settings, "Appearance");

        this.initialTheme = Theme.fromLookAndFeelName(this.settings.lookAndFeelName.get());
        this.addRow(
            "Theme:",
            "Controls the look and feel of the user interface.",
            this.themeChooser = new JComboBox<>(Theme.values())
        );
        this.themeChooser.setSelectedItem(this.initialTheme);

        this.addRow(
            "Editor font:",
            "Font for the text editor.",
            this.editorFontChooser = new FontChooserPane(this.settings.editorFont.get())
        );
        this.addRow(
            "Table font:",
            "Font for the various table displays (registers, text segment, memory viewer, symbol table).",
            this.tableFontChooser = new FontChooserPane(this.settings.tableFont.get())
        );
        this.addRow(
            "Table highlight font:",
            "Font for highlighted text in table displays (overrides the table font in those cells).",
            this.tableHighlightFontChooser = new FontChooserPane(this.settings.tableHighlightFont.get())
        );
        this.addRow(
            "Console font:",
            "Font for the console displays in the Messages and Console tabs.",
            this.consoleFontChooser = new FontChooserPane(this.settings.consoleFont.get())
        );
    }

    @Override
    public void applyChanges() {
        if (this.themeChooser.getSelectedIndex() >= 0) {
            String lookAndFeelName = Theme.values()[this.themeChooser.getSelectedIndex()].getLookAndFeelName();
            if (!lookAndFeelName.equals(this.settings.lookAndFeelName.get())) {
                this.settings.lookAndFeelName.set(lookAndFeelName);
            }
        }

        if (!this.editorFontChooser.getValue().equals(this.settings.editorFont.get())) {
            this.settings.editorFont.set(this.editorFontChooser.getValue());
        }
        if (!this.tableFontChooser.getValue().equals(this.settings.tableFont.get())) {
            this.settings.tableFont.set(this.tableFontChooser.getValue());
        }
        if (!this.tableHighlightFontChooser.getValue().equals(this.settings.tableHighlightFont.get())) {
            this.settings.tableHighlightFont.set(this.tableHighlightFontChooser.getValue());
        }
        if (!this.consoleFontChooser.getValue().equals(this.settings.consoleFont.get())) {
            this.settings.consoleFont.set(this.consoleFontChooser.getValue());
        }
    }

    @Override
    public void revertChanges() {
        this.themeChooser.setSelectedItem(this.initialTheme);
        this.editorFontChooser.revertValue();
        this.tableFontChooser.revertValue();
        this.tableHighlightFontChooser.revertValue();
        this.consoleFontChooser.revertValue();
    }
}
