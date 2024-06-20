package mars.venus.preferences;

import mars.settings.Settings;

import javax.swing.*;

public abstract class PreferencesTab extends JPanel {
    protected final Settings settings;

    public PreferencesTab(Settings settings, String title) {
        this.settings = settings;
        this.setName(title);
    }

    public abstract void applyChanges();

    public abstract void revertChanges();
}
