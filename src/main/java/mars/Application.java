package mars;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import mars.assembler.SymbolTable;
import mars.mips.hardware.*;
import mars.mips.instructions.InstructionSet;
import mars.settings.Settings;
import mars.util.PropertiesFile;
import mars.venus.VenusUI;

import javax.swing.*;
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
    public static final String VERSION = "5.0-beta5";
    /**
     * MARS copyright years.
     */
    public static final String COPYRIGHT_YEARS = "2003-2014";
    /**
     * MARS copyright holders.
     */
    public static final String COPYRIGHT_HOLDERS = "Pete Sanderson and Kenneth Vollmar";
    /**
     * Name of properties file used to hold internal configurations.
     */
    private static final String CONFIG_PROPERTIES = "Config";
    /**
     * Lock variable used at head of synchronized block to guard MIPS memory and registers.
     */
    public static final Object MEMORY_AND_REGISTERS_LOCK = new Object();
    /**
     * Prefix to print to console when echoing user input from pop-up dialog.
     */
    public static final String USER_INPUT_PREFIX = "**** user input : ";
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
     * Placeholder for non-printable ASCII codes
     */
    public static final String ASCII_NON_PRINT = getAsciiNonPrint();
    /**
     * Array of strings to display for ASCII codes in ASCII display of data segment. ASCII code 0-255 is array index.
     */
    public static final String[] ASCII_TABLE = getAsciiStrings();

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
            case "FlatDarkLaf" -> new FlatDarkLaf();
            case "FlatLightLaf" -> new FlatLightLaf();
            default -> new FlatDarkLaf();
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
        return getIntegerProperty("MessageLimit", 1000000);
    }

    /**
     * Read limit on number of error messages produced by one assemble operation.
     */
    private static int getErrorLimit() {
        return getIntegerProperty("ErrorLimit", 200);
    }

    /**
     * Read backstep limit (number of operations to buffer) from properties file.
     */
    private static int getBackstepLimit() {
        return getIntegerProperty("BackstepLimit", 1000);
    }

    /**
     * Read ASCII default display character for non-printing characters, from properties file.
     */
    public static String getAsciiNonPrint() {
        String asciiNonPrint = getPropertyEntry(CONFIG_PROPERTIES, "AsciiNonPrint");
        return (asciiNonPrint == null) ? "." : (asciiNonPrint.equals("space")) ? " " : asciiNonPrint;
    }

    /**
     * Read ASCII strings for codes 0-255, from properties file. If string
     * value is "null", substitute value of ASCII_NON_PRINT.  If string is
     * "space", substitute string containing one space character.
     */
    private static String[] getAsciiStrings() {
        String asciiTable = getPropertyEntry(CONFIG_PROPERTIES, "AsciiTable");
        String placeHolder = getAsciiNonPrint();
        String[] asciiStrings = asciiTable.split("\\s+");
        int maxLength = 0;
        for (int index = 0; index < asciiStrings.length; index++) {
            if (asciiStrings[index].equals("null")) {
                asciiStrings[index] = placeHolder;
            }
            else if (asciiStrings[index].equals("space")) {
                asciiStrings[index] = " ";
            }
            maxLength = Math.max(maxLength, asciiStrings[index].length());
        }
        for (int index = 0; index < asciiStrings.length; index++) {
            asciiStrings[index] = " ".repeat(maxLength - asciiStrings[index].length() + 1) + asciiStrings[index];
        }
        return asciiStrings;
    }

    /**
     * Read and return integer property value for given file and property name.
     * Default value is returned if property file or name not found.
     */
    private static int getIntegerProperty(String propertyName, int defaultValue) {
        Properties properties = PropertiesFile.loadPropertiesFromFile(Application.CONFIG_PROPERTIES);
        try {
            return Integer.parseInt(properties.getProperty(propertyName, Integer.toString(defaultValue)));
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
        String extensions = getPropertyEntry(CONFIG_PROPERTIES, "Extensions");
        if (extensions != null) {
            StringTokenizer st = new StringTokenizer(extensions);
            while (st.hasMoreTokens()) {
                extensionsList.add(st.nextToken());
            }
        }
        return extensionsList;
    }

    /**
     * Read and return property file value (if any) for requested property.
     *
     * @param propertiesFile name of properties file (do NOT include filename extension,
     *                       which is assumed to be ".properties")
     * @param propertyName   String containing desired property name
     * @return String containing associated value; null if property not found
     */
    public static String getPropertyEntry(String propertiesFile, String propertyName) {
        return PropertiesFile.loadPropertiesFromFile(propertiesFile).getProperty(propertyName);
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
}
