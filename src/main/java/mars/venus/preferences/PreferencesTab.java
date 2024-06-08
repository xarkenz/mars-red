package mars.venus.preferences;

import mars.settings.Settings;

import javax.swing.*;

public abstract class PreferencesTab extends JPanel {
    protected final Settings settings;
    protected final String title;

    public PreferencesTab(Settings settings, String title) {
        this.settings = settings;
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public abstract void applyChanges();

    public abstract void revertChanges();
}
