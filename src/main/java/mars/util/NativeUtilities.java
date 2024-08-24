package mars.util;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;

public class NativeUtilities {
    private static Boolean isUsingX11 = null;

    // Prevent instances of this class
    private NativeUtilities() {}

    /**
     * Determine whether AWT is currently running on the X11 windowing system.
     * If so, most implementations of AWT classes are located in the internal package <code>sun.awt.X11</code>.
     *
     * @return <code>true</code> if the default toolkit is <code>sun.awt.X11.XToolkit</code>,
     *         or <code>false</code> otherwise.
     */
    public static boolean isUsingX11() {
        if (isUsingX11 == null) {
            // Look for a class name like `sun.awt.X11.XToolkit`
            isUsingX11 = Toolkit.getDefaultToolkit().getClass().getPackageName().contains("X11");
        }

        return isUsingX11;
    }

    /**
     * Set the name of the AWT application as a whole, if applicable. It is recommended that this method is called
     * before any windows are shown, as the platform may not recognize the change otherwise.
     * <p>
     * This does nothing for most platforms, but for some (namely X11), this method does the extra work to ensure
     * that the name is properly set as desired.
     *
     * @param name The general application name, which appears on the taskbar and/or status bar on some platforms.
     */
    public static void setApplicationName(String name) {
        if (isUsingX11()) {
            // The name of the whole application in X11 (at least in GNOME) is not based on the window title,
            // and it can't be changed through `java.awt.Taskbar` either. Instead, it is hardcoded to be the
            // name of the main class, replacing dots with hyphens. (For example, `mars-MarsLauncher`.)
            // Currently, the only way to change this name is to use reflection to change a private field in
            // `sun.awt.X11.XToolkit` called `awtAppClassName` to the name we want.
            // This is frustrating, but we have no other choice unless an official API is added to AWT in the future.
            // The relevant JDK bug report can be found at https://bugs.openjdk.org/browse/JDK-6528430.

            Toolkit toolkit = Toolkit.getDefaultToolkit();

            // NOTE: The following try/catch code does not work in Java 17 because the trick has been patched.
            // I figure I'll just leave it here because it can't hurt. Thank you Java for being so helpful.
            // Sean Clarke 08/2024
            try {
                Module thisModule = NativeUtilities.class.getModule();
                if (thisModule == NativeUtilities.class.getClassLoader().getUnnamedModule()) {
                    // We are in the unnamed module, so we can utilize a trick allowing us to `--add-opens` at runtime.
                    // This is needed since the module system will not allow us to modify field visibility
                    // or other properties of internal libraries (such as `sun.awt.X11`) otherwise.
                    toolkit.getClass().getModule().addOpens("sun.awt.X11", thisModule);
                }
            }
            catch (Exception exception) {
                // Oh well, try the below code anyway; if it doesn't work, the user will be notified then
            }

            try {
                // Attempt to modify the desired private field mentioned above via reflection
                Field classNameField = toolkit.getClass().getDeclaredField("awtAppClassName");
                classNameField.setAccessible(true);
                classNameField.set(toolkit, name);
            }
            catch (Exception exception) {
                // We don't have access to change it, but the application will still work... notify the user, though
                exception.printStackTrace(System.err);
                System.err.println("Error: failed to set the taskbar application name (see above error message).");
                if (exception instanceof InaccessibleObjectException) {
                    System.err.println("Try adding '--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED' as a Java command argument.");
                }
            }
        }
    }

    /**
     * Update the application icon displayed on the taskbar to the given image, if possible.
     * <p>
     * This method does nothing for platforms which do not support the {@link Taskbar} API.
     * Note that this is not a replacement for {@link Window#setIconImage(Image)}, which sets the icon assigned
     * for a particular window (also setting the application icon on some platforms).
     *
     * @param iconImage The new application icon, which will appear on the taskbar on some platforms.
     */
    public static void setApplicationIconImage(Image iconImage) {
        // Thank goodness this has an official API unlike the above!
        try {
            Taskbar.getTaskbar().setIconImage(iconImage);
        }
        catch (UnsupportedOperationException exception) {
            // The platform doesn't support setting the icon image through this method
        }
        catch (SecurityException exception) {
            System.err.println("Error: failed to set the taskbar icon image due to lack of permission.");
        }
    }
}
