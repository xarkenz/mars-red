package mars.venus.themes;

import com.formdev.flatlaf.FlatDarkLaf;

public class SolarizedDarkLaf extends FlatDarkLaf {
    public static boolean setup() {
        return setup(new SolarizedDarkLaf());
    }

    @Override
    public String getName() {
        return "SolarizedDarkLaf";
    }
}
