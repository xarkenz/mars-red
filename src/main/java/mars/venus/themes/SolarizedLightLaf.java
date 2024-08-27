package mars.venus.themes;

import com.formdev.flatlaf.FlatLightLaf;

public class SolarizedLightLaf extends FlatLightLaf {
    public static boolean setup() {
        return setup(new SolarizedLightLaf());
    }

    @Override
    public String getName() {
        return "SolarizedLightLaf";
    }
}
