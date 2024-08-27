package mars;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import mars.assembler.SymbolTable;
import mars.mips.hardware.*;
import mars.mips.instructions.InstructionSet;
import mars.settings.Settings;
import mars.util.Binary;
import mars.venus.VenusUI;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Collection of globally-available data structures.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public class Application {
    /**
     * The name of the application.
     */
    public static final String NAME = "MARS Red";
    /**
     * The current MARS Red version number.
     */
    public static final String VERSION = "5.0-beta7";
    /**
     * MARS copyright years.
     */
    public static final String COPYRIGHT_YEARS = "2003-2014";
    /**
     * MARS copyright holders.
     */
    public static final String COPYRIGHT_HOLDERS = "Pete Sanderson and Kenneth Vollmar";
    /**
     * Lock variable used at head of synchronized block to guard MIPS memory and registers.
     */
    public static final Object MEMORY_AND_REGISTERS_LOCK = new Object();
    /**
     * Path to the resources folder that contains images.
     */
    public static final String IMAGES_PATH = "/images/";
    /**
     * Path to the resources folder that contains action icons.
     */
    public static final String ACTION_ICONS_PATH = "/icons/actions/";
    /**
     * Path to the resources folder that contains help documentation.
     */
    public static final String HELP_PATH = "/help/";
    /**
     * List of accepted file extensions for MIPS assembly source files.
     */
    public static final ArrayList<String> FILE_EXTENSIONS = getFileExtensions();
    /**
     * Maximum length of scrolled message window (Messages and Console).
     */
    public static final int MAXIMUM_MESSAGE_CHARACTERS = getMessageLimit();
    /**
     * Maximum number of assembler errors produced by one assemble operation.
     */
    public static final int MAXIMUM_ERROR_MESSAGES = getErrorLimit();
    /**
     * Maximum number of back-step operations to buffer.
     */
    public static final int MAXIMUM_BACKSTEPS = getBackstepLimit();
    /**
     * Name of properties file used to hold internal configurations.
     */
    private static final String CONFIG_VALUES_PATH = "/config/values.properties";

    /**
     * MARS exit code -- useful with syscall 17 when running from command line (not GUI).
     */
    public static int exitCode = 0;
    /**
     * Flag to determine whether or not to produce internal debugging information.
     */
    public static boolean debug = false;
    /**
     * Flag that indicates whether instructionSet has been initialized.
     */
    private static boolean initialized = false;
    /**
     * Object that contains various settings that can be accessed modified internally.
     */
    private static Settings settings;
    /**
     * The GUI being used (if any) with this simulator.
     */
    private static VenusUI gui = null;
    /**
     * The set of implemented MIPS instructions.
     */
    public static InstructionSet instructionSet;
    /**
     * the program currently being worked with.  Used by GUI only, not command line.
     */
    public static Program program;
    /**
     * Symbol table for file currently being assembled.
     */
    public static SymbolTable globalSymbolTable;
    /**
     * Storage object for config values loaded from file. The properties are loaded when first used.
     */
    private static Properties configValues = null;
    /**
     * Array of strings to display for ASCII codes in ASCII display of data segment. ASCII code 0-255 is array index.
     */
    private static String[] asciiTable = null;
    /**
     * Placeholder for non-printable ASCII codes.
     */
    private static String asciiNonPrint = null;

    public static VenusUI getGUI() {
        return gui;
    }

    public static void setGUI(VenusUI gui) {
        Application.gui = gui;
    }

    public static Settings getSettings() {
        return settings;
    }

    /**
     * Method called once upon system initialization to create the global data structures.
     */
    public static void initialize() {
        if (!initialized) {
            settings = new Settings();
            instructionSet = new InstructionSet();
            instructionSet.populate();
            globalSymbolTable = new SymbolTable("(global)");
            initialized = true;
            debug = false;

            MemoryConfigurations.getConfigurations();
        }
    }

    /**
     * Configure the look and feel of the GUI according to application settings,
     * refreshing the appearance of the GUI if it is already created.
     */
    public static void setupLookAndFeel() {
        // Set the UI scale
        System.setProperty("flatlaf.uiScale", settings.uiScale.get() + "%");
        // Designate the "themes" folder for theme style overrides
        FlatLaf.registerCustomDefaultsSource("themes");
        // Set up the look and feel
        FlatLaf.setup(switch (settings.lookAndFeelName.get()) {
            case "FlatLightLaf" -> new FlatLightLaf();
            case "FlatDarkLaf" -> new FlatDarkLaf();
            case "FlatDarculaLaf" -> new FlatDarculaLaf();
            case "FlatMacLightLaf" -> new FlatMacLightLaf();
            case "FlatMacDarkLaf" -> new FlatMacDarkLaf();
            default -> new FlatLightLaf();
        });

        // Refresh the GUI as best as possible if is already open
        if (gui != null) {
            SwingUtilities.updateComponentTreeUI(gui);
        }
    }

    /**
     * Read byte limit of Run I/O or MARS Messages text to buffer.
     */
    private static int getMessageLimit() {
        return getConfigInteger("MessageLimit", 1000000);
    }

    /**
     * Read limit on number of error messages produced by one assemble operation.
     */
    private static int getErrorLimit() {
        return getConfigInteger("ErrorLimit", 200);
    }

    /**
     * Read backstep limit (number of operations to buffer) from properties file.
     */
    private static int getBackstepLimit() {
        return getConfigInteger("BackstepLimit", 1000);
    }

    /**
     * Read ASCII default display character for non-printing characters, from properties file.
     */
    public static String getAsciiNonPrint() {
        if (asciiNonPrint == null) {
            asciiNonPrint = getConfigString("AsciiNonPrint", ".");
            if (asciiNonPrint.equalsIgnoreCase("space")) {
                asciiNonPrint = " ";
            }
        }
        return asciiNonPrint;
    }

    /**
     * Read ASCII strings for codes 0-255, from <code>/config/values.properties</code>
     * under <code>AsciiTable</code>. If a string is <code>null</code>, substitute value of <code>AsciiNonPrint</code>.
     * If string is <code>space</code>, substitute string containing one space character.
     */
    public static String[] getAsciiTable() {
        if (asciiTable == null) {
            String asciiTableString = getConfigString("AsciiTable");
            asciiTable = asciiTableString.split("\\s+");
            int maxLength = 0;
            for (int index = 0; index < asciiTable.length; index++) {
                if (asciiTable[index].equalsIgnoreCase("null")) {
                    asciiTable[index] = getAsciiNonPrint();
                }
                else if (asciiTable[index].equalsIgnoreCase("space")) {
                    asciiTable[index] = " ";
                }
                maxLength = Math.max(maxLength, asciiTable[index].length());
            }
            for (int index = 0; index < asciiTable.length; index++) {
                asciiTable[index] = " ".repeat(maxLength - asciiTable[index].length() + 1) + asciiTable[index];
            }
        }
        return asciiTable;
    }

    /**
     * Obtain a configuration integer from <code>/config/values.properties</code>.
     *
     * @param key          The name of the configuration integer to retrieve.
     * @param defaultValue The default value to use if no integer is found.
     * @return The configuration integer requested, or <code>defaultValue</code> if it is invalid or not specified.
     * @see Binary#decodeInteger(String)
     */
    private static int getConfigInteger(String key, int defaultValue) {
        String stringValue = getConfigString(key);
        if (stringValue == null) {
            return defaultValue;
        }
        try {
            return Binary.decodeInteger(stringValue);
        }
        catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    /**
     * Read assembly language file extensions from properties file.  Resulting
     * string is tokenized into array list (assume StringTokenizer default delimiters).
     */
    private static ArrayList<String> getFileExtensions() {
        ArrayList<String> extensionsList = new ArrayList<>();
        String extensions = getConfigString("Extensions");
        if (extensions != null) {
            StringTokenizer st = new StringTokenizer(extensions);
            while (st.hasMoreTokens()) {
                extensionsList.add(st.nextToken());
            }
        }
        return extensionsList;
    }

    /**
     * Obtain a configuration string from <code>/config/values.properties</code>.
     *
     * @param key The name of the configuration string to retrieve.
     * @return The configuration string requested, or <code>null</code> if it is not specified.
     */
    public static String getConfigString(String key) {
        return getConfigString(key, null);
    }

    /**
     * Obtain a configuration string from <code>/config/values.properties</code>.
     *
     * @param key          The name of the configuration string to retrieve.
     * @param defaultValue The default value to use if no string is found.
     * @return The configuration string requested, or <code>defaultValue</code> if it is not specified.
     */
    public static String getConfigString(String key, String defaultValue) {
        ensureConfigValuesLoaded();
        return configValues.getProperty(key, defaultValue);
    }

    /**
     * Return whether backstepping is permitted at this time.  Backstepping is ability to undo execution
     * steps one at a time.  Available only in GUI mode.
     *
     * @return true if backstepping is permitted, false otherwise.
     */
    public static boolean isBackSteppingEnabled() {
        return program != null && program.getBackStepper() != null && program.getBackStepper().isEnabled();
    }

    /**
     * Load config values from file if they have not been loaded already.
     */
    private static void ensureConfigValuesLoaded() {
        if (configValues == null) {
            configValues = new Properties();
            try {
                InputStream input = Application.class.getResourceAsStream(CONFIG_VALUES_PATH);
                configValues.load(input);
            }
            catch (IOException | NullPointerException ignored) {
                System.err.println(CONFIG_VALUES_PATH + ": failed to load config values from file");
                // The properties that loaded successfully, if any, will be used
            }
        }
    }
}
