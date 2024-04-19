package mars;

import mars.assembler.SymbolTable;
import mars.mips.hardware.Memory;
import mars.mips.instructions.InstructionSet;
import mars.settings.Settings;
import mars.util.PropertiesFile;
import mars.venus.VenusUI;

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
     * Name of properties file used to hold internal configurations.
     */
    private static final String CONFIG_PROPERTIES = "Config";

    /**
     * Lock variable used at head of synchronized block to guard MIPS memory and registers
     */
    public static final Object MEMORY_AND_REGISTERS_LOCK = new Object();
    /**
     * Prefix to print to console when echoing user input from pop-up dialog.
     */
    public static final String USER_INPUT_PREFIX = "**** user input : ";
    /**
     * Path to folder that contains images
     */
    public static final String IMAGES_PATH = "/images/";
    /**
     * Path to folder that contains action icons
     */
    public static final String ACTION_ICONS_PATH = "/icons/actions/";
    /**
     * Path to folder that contains help text
     */
    public static final String HELP_PATH = "/help/";
    /**
     * The current MARS Red version number. Can't wait for {@link #initialize()} call to get it.
     */
    public static final String VERSION = "5.0-beta2";
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
     * The name of the application.
     */
    public static final String NAME = "MARS Red";
    /**
     * MARS copyright years.
     */
    public static final String COPYRIGHT_YEARS = "2003-2014";
    /**
     * MARS copyright holders.
     */
    public static final String COPYRIGHT_HOLDERS = "Pete Sanderson and Kenneth Vollmar";
    /**
     * Placeholder for non-printable ASCII codes
     */
    public static final String ASCII_NON_PRINT = getAsciiNonPrint();
    /**
     * Array of strings to display for ASCII codes in ASCII display of data segment. ASCII code 0-255 is array index.
     */
    public static final String[] ASCII_TABLE = getAsciiStrings();

    /**
     * MARS exit code -- useful with SYSCALL 17 when running from command line (not GUI).
     */
    public static int exitCode = 0;
    /**
     * Flag that indicates whether the RunSpeedPanel has been created.
     */
    public static boolean runSpeedPanelExists = false;
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
     * Simulated MIPS memory component.
     */
    public static Memory memory;

    public static void setGUI(VenusUI gui) {
        Application.gui = gui;
    }

    public static VenusUI getGUI() {
        return gui;
    }

    public static Settings getSettings() {
        return settings;
    }

    /**
     * Method called once upon system initialization to create the global data structures.
     */
    public static void initialize() {
        if (!initialized) {
            memory = Memory.getInstance(); // Clients can still use Memory.getInstance instead of Globals.memory
            instructionSet = new InstructionSet();
            instructionSet.populate();
            globalSymbolTable = new SymbolTable("(global)");
            settings = new Settings();
            initialized = true;
            debug = false;
            memory.clear(); // Will establish memory configuration from setting
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
     * Get list of MarsTools that reside outside the MARS distribution.
     * Currently this is done by adding the tool's path name to the list
     * of values for the ExternalTools property. Use ";" as delimiter!
     *
     * @return ArrayList.  Each item is file path to .class file
     * of a class that implements MarsTool.  If none, returns empty list.
     */
    public static ArrayList<String> getExternalTools() {
        ArrayList<String> toolsList = new ArrayList<>();
        String tools = getPropertyEntry(CONFIG_PROPERTIES, "ExternalTools");
        if (tools != null) {
            StringTokenizer st = new StringTokenizer(tools, ";");
            while (st.hasMoreTokens()) {
                toolsList.add(st.nextToken());
            }
        }
        return toolsList;
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
}
