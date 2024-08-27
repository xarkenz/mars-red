package mars.venus.themes;

import com.formdev.flatlaf.FlatLightLaf;

public class SolarizedDarkLaf extends FlatLightLaf {
    public static boolean setup() {
        return setup(new SolarizedDarkLaf());
    }

    @Override
    public String getName() {
        return "SolarizedDarkLaf";
    }
}
