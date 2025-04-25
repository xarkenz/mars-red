package mars.venus.preferences.components;

import mars.settings.BooleanSetting;

import javax.swing.*;

public class CheckBoxPreference extends JCheckBox {
    private final boolean initialValue;
    private final BooleanSetting setting;

    public CheckBoxPreference(BooleanSetting setting, String label, String toolTip) {
        super(label);
        this.initialValue = setting.get();
        this.setting = setting;
        this.setSelected(this.initialValue);
        this.setToolTipText(toolTip);
    }

    public void apply() {
        this.setting.set(this.isSelected());
    }

    public void revert() {
        this.setSelected(this.initialValue);
    }
}
