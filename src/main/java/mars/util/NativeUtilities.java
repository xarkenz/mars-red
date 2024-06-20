package mars.util;

import java.awt.*;
import java.lang.reflect.Field;

public class NativeUtilities {
    private static Boolean isUsingX11 = null;

    // Prevent instances of this class
    private NativeUtilities() {}

    public static boolean isUsingX11() {
        if (isUsingX11 == null) {
            // Look for a class name like sun.awt.X11.XToolkit
            isUsingX11 = Toolkit.getDefaultToolkit().getClass().getPackageName().contains("X11");
        }

        return isUsingX11;
    }

    public static void setApplicationName(String name) {
        if (isUsingX11()) {
            try {
                // Hacky! You would think there would be a better way to do this...
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Field classNameField = toolkit.getClass().getDeclaredField("awtAppClassName");
                classNameField.setAccessible(true);
                classNameField.set(toolkit, name);
            }
            catch (Exception exception) {
                // We don't have access to change it
                exception.printStackTrace(System.err);
                System.err.println("Failed to change the application name (see above error message).");
                System.err.println("Try adding '--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED' as a Java command argument.");
            }
        }
    }
}
