package mars.settings;

import mars.Application;
import mars.util.Binary;
import mars.util.EditorFont;
import mars.util.PropertiesFile;
import mars.venus.editors.jeditsyntax.SyntaxStyle;
import mars.venus.editors.jeditsyntax.SyntaxUtilities;

import java.awt.*;
import java.util.Observable;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

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
 * Contains various IDE settings.  Persistent settings are maintained for the
 * current user and on the current machine using Java's Preference objects.
 * Failing that, default setting values come from Settings.properties file.
 * If both of those fail, default values are defined in this class.
 * <p>
 * NOTE: If the Preference objects fail due to security exceptions, changes to
 * settings will not carry over from one MARS session to the next.
 * <p>
 * Actual implementation of the Preference objects is platform-dependent.
 * For Windows, they are stored in Registry.  To see, run regedit and browse to:
 * HKEY_CURRENT_USER\Software\JavaSoft\Prefs\mars
 *
 * @author Pete Sanderson
 */
public class Settings extends Observable {
    /* Properties file used to hold default settings. */
    private static final String SETTINGS_FILENAME = "Settings";

    // BOOLEAN SETTINGS

    /**
     * Flag to determine whether or not program being assembled is limited to
     * basic MIPS instructions and formats.
     */
    public final BooleanSetting extendedAssemblerEnabled = new BooleanSetting(
        this,
        "ExtendedAssembler",
        true,
        true
    );
    /**
     * Flag to determine whether or not program being assembled is limited to
     * using register numbers instead of names. NOTE: Its default value is
     * false and the IDE provides no means to change it!
     */
    public final BooleanSetting bareMachineEnabled = new BooleanSetting(
        this,
        "BareMachine",
        false,
        true
    );
    /**
     * Flag to determine whether or not a file is immediately and automatically assembled
     * upon opening. Handy when using an external editor.
     */
    public final BooleanSetting assembleOnOpenEnabled = new BooleanSetting(
        this,
        "AssembleOnOpen",
        false,
        true
    );
    /**
     * Flag to determine whether only the current editor source file (enabled false) or
     * all files in its directory (enabled true) will be assembled when assembly is selected.
     */
    public final BooleanSetting assembleAllEnabled = new BooleanSetting(
        this,
        "AssembleAll",
        false,
        true
    );
    /**
     * Flag for visibility of the label window (symbol table).  Default only, dynamic status
     * maintained by ExecutePane.
     */
    public final BooleanSetting labelWindowVisible = new BooleanSetting(
        this,
        "LabelWindowVisibility",
        false,
        true
    );
    /**
     * Flag for displaying addresses in hexadecimal or decimal in the Execute pane.
     */
    public final BooleanSetting displayAddressesInHex = new BooleanSetting(
        this,
        "DisplayAddressesInHex",
        true,
        true
    );
    /**
     * Flag for displaying values in hexadecimal or decimal in the Execute pane.
     */
    public final BooleanSetting displayValuesInHex = new BooleanSetting(
        this,
        "DisplayValuesInHex",
        true,
        true
    );
    /**
     * Flag to determine whether the currently selected exception handler source file will
     * be included in each assembly operation.
     */
    public final BooleanSetting exceptionHandlerEnabled = new BooleanSetting(
        this,
        "LoadExceptionHandler",
        false,
        true
    );
    /**
     * Flag to determine whether or not delayed branching is in effect at MIPS execution.
     * This means we simulate the pipeline and statement FOLLOWING a successful branch
     * is executed before branch is taken. DPS 14 June 2007.
     */
    public final BooleanSetting delayedBranchingEnabled = new BooleanSetting(
        this,
        "DelayedBranching",
        false,
        true
    );
    /**
     * Flag to determine whether or not the editor will display line numbers.
     */
    public final BooleanSetting displayEditorLineNumbers = new BooleanSetting(
        this,
        "EditorLineNumbersDisplayed",
        true,
        true
    );
    /**
     * Flag to determine whether or not assembler warnings are considered errors.
     */
    public final BooleanSetting warningsAreErrors = new BooleanSetting(
        this,
        "WarningsAreErrors",
        false,
        true
    );
    /**
     * Flag to determine whether or not to display and use program arguments
     */
    public final BooleanSetting useProgramArguments = new BooleanSetting(
        this,
        "ProgramArguments",
        false,
        true
    );
    /**
     * Flag to control whether or not highlighting is applied to data segment window
     */
    public final BooleanSetting highlightDataSegment = new BooleanSetting(
        this,
        "DataSegmentHighlighting",
        true,
        true
    );
    /**
     * Flag to control whether or not highlighting is applied to register windows
     */
    public final BooleanSetting highlightRegisters = new BooleanSetting(
        this,
        "RegistersHighlighting",
        true,
        true
    );
    /**
     * Flag to control whether or not assembler automatically initializes program counter to 'main's address
     */
    public final BooleanSetting startAtMain = new BooleanSetting(
        this,
        "StartAtMain",
        true,
        true
    );
    /**
     * Flag to control whether or not editor will highlight the line currently being edited
     */
    public final BooleanSetting highlightCurrentEditorLine = new BooleanSetting(
        this,
        "EditorCurrentLineHighlighting",
        true,
        true
    );
    /**
     * Flag to control whether or not editor will provide popup instruction guidance while typing
     */
    public final BooleanSetting popupInstructionGuidance = new BooleanSetting(
        this,
        "PopupInstructionGuidance",
        true,
        true
    );
    /**
     * Flag to control whether or not simulator will use popup dialog for input syscalls
     */
    public final BooleanSetting popupSyscallInput = new BooleanSetting(
        this,
        "PopupSyscallInput",
        false,
        true
    );
    /**
     * Flag to control whether or not to use generic text editor instead of language-aware styled editor
     */
    public final BooleanSetting useGenericTextEditor = new BooleanSetting(
        this,
        "GenericTextEditor",
        false,
        true
    );
    /**
     * Flag to control whether or not language-aware editor will use auto-indent feature
     */
    public final BooleanSetting autoIndentEnabled = new BooleanSetting(
        this,
        "AutoIndent",
        true,
        true
    );
    /**
     * Flag to determine whether a program can write binary code to the text or data segment and
     * execute that code.
     */
    public final BooleanSetting selfModifyingCodeEnabled = new BooleanSetting(
        this,
        "SelfModifyingCode",
        false,
        true
    );

    public final BooleanSetting[] booleanSettings = {
        extendedAssemblerEnabled,
        bareMachineEnabled,
        assembleOnOpenEnabled,
        assembleAllEnabled,
        labelWindowVisible,
        displayAddressesInHex,
        displayValuesInHex,
        exceptionHandlerEnabled,
        delayedBranchingEnabled,
        displayEditorLineNumbers,
        warningsAreErrors,
        useProgramArguments,
        highlightDataSegment,
        highlightRegisters,
        startAtMain,
        highlightCurrentEditorLine,
        popupInstructionGuidance,
        popupSyscallInput,
        useGenericTextEditor,
        autoIndentEnabled,
        selfModifyingCodeEnabled,
    };

    // INTEGER SETTINGS

    /**
     * State for sorting label window display.
     */
    public final IntegerSetting symbolTableSortState = new IntegerSetting(
        this,
        "LabelSortState",
        0,
        false
    );
    /**
     * Caret blink rate in milliseconds, 0 means don't blink.
     */
    public final IntegerSetting caretBlinkRate = new IntegerSetting(
        this,
        "CaretBlinkRate",
        500,
        false
    );
    /**
     * Editor tab size in characters.
     */
    public final IntegerSetting editorTabSize = new IntegerSetting(
        this,
        "EditorTabSize",
        8,
        false
    );
    /**
     * Number of letters to be matched by editor's instruction guide before popup generated (if popup enabled)
     */
    public final IntegerSetting editorPopupPrefixLength = new IntegerSetting(
        this,
        "EditorPopupPrefixLength",
        1,
        false
    );

    public final IntegerSetting[] integerSettings = {symbolTableSortState,
        caretBlinkRate,
        editorTabSize,
        editorPopupPrefixLength,
    };

    // STRING SETTINGS

    /**
     * Current specified exception handler file (a MIPS assembly source file).
     */
    public final StringSetting exceptionHandlerPath = new StringSetting(
        this,
        "ExceptionHandler",
        "",
        false
    );
    /**
     * Identifier of current memory configuration.
     */
    public final StringSetting memoryConfiguration = new StringSetting(
        this,
        "MemoryConfiguration",
        "",
        false
    );
    /**
     * Order of text segment table columns.
     */
    public final StringSetting textSegmentColumnOrder = new StringSetting(
        this,
        "TextColumnOrder",
        "0 1 2 3 4",
        false
    );
    public final StringSetting previouslyOpenFiles = new StringSetting(
        this,
        "PreviouslyOpenFiles",
        "",
        false
    );

    public final StringSetting[] stringSettings = {
        exceptionHandlerPath,
        memoryConfiguration,
        textSegmentColumnOrder,
        previouslyOpenFiles,
    };

    // FONT SETTINGS.  Each array position has associated name.
    /**
     * Font for the text editor
     */
    public static final int EDITOR_FONT = 0;
    /**
     * Font for table even row background (text, data, register displays)
     */
    public static final int EVEN_ROW_FONT = 1;
    /**
     * Font for table odd row background (text, data, register displays)
     */
    public static final int ODD_ROW_FONT = 2;
    /**
     * Font for table odd row foreground (text, data, register displays)
     */
    public static final int TEXTSEGMENT_HIGHLIGHT_FONT = 3;
    /**
     * Font for text segment delay slot highlighted background
     */
    public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FONT = 4;
    /**
     * Font for text segment highlighted background
     */
    public static final int DATASEGMENT_HIGHLIGHT_FONT = 5;
    /**
     * Font for register highlighted background
     */
    public static final int REGISTER_HIGHLIGHT_FONT = 6;

    private static final String FONT_FAMILY_SUFFIX = "Family";
    private static final String FONT_STYLE_SUFFIX = "Style";
    private static final String FONT_SIZE_SUFFIX = "Size";

    private static final String[] fontFamilySettingsKeys = {"EditorFontFamily", "EvenRowFontFamily", "OddRowFontFamily", " TextSegmentHighlightFontFamily", "TextSegmentDelayslotHighightFontFamily", "DataSegmentHighlightFontFamily", "RegisterHighlightFontFamily"};
    private static final String[] fontStyleSettingsKeys = {"EditorFontStyle", "EvenRowFontStyle", "OddRowFontStyle", " TextSegmentHighlightFontStyle", "TextSegmentDelayslotHighightFontStyle", "DataSegmentHighlightFontStyle", "RegisterHighlightFontStyle"};
    private static final String[] fontSizeSettingsKeys = {"EditorFontSize", "EvenRowFontSize", "OddRowFontSize", " TextSegmentHighlightFontSize", "TextSegmentDelayslotHighightFontSize", "DataSegmentHighlightFontSize", "RegisterHighlightFontSize"};

    /**
     * Last resort default values for Font settings;
     * will use only if neither the Preferences nor the properties file work.
     * If you wish to change, do so before instantiating the Settings object.
     * Must match key by list position shown above.
     */

    // DPS 3-Oct-2012
    // Changed default font family from "Courier New" to "Monospaced" after receiving reports that Mac were not
    // correctly rendering the left parenthesis character in the editor or text segment display.
    // See http://www.mirthcorp.com/community/issues/browse/MIRTH-1921?page=com.atlassian.jira.plugin.system.issuetabpanels:all-tabpanel
    private static final String[] defaultFontFamilySettingsValues = {"Monospaced", "Monospaced", "Monospaced", "Monospaced", "Monospaced", "Monospaced", "Monospaced"};
    private static final String[] defaultFontStyleSettingsValues = {"Plain", "Plain", "Plain", "Plain", "Plain", "Plain", "Plain"};
    private static final String[] defaultFontSizeSettingsValues = {"12", "12", "12", "12", "12", "12", "12",};

    // COLOR SETTINGS.  Each array position has associated name.
    /**
     * RGB color for table even row background (text, data, register displays)
     */
    public static final int EVEN_ROW_BACKGROUND = 0;
    /**
     * RGB color for table even row foreground (text, data, register displays)
     */
    public static final int EVEN_ROW_FOREGROUND = 1;
    /**
     * RGB color for table odd row background (text, data, register displays)
     */
    public static final int ODD_ROW_BACKGROUND = 2;
    /**
     * RGB color for table odd row foreground (text, data, register displays)
     */
    public static final int ODD_ROW_FOREGROUND = 3;
    /**
     * RGB color for text segment highlighted background
     */
    public static final int TEXTSEGMENT_HIGHLIGHT_BACKGROUND = 4;
    /**
     * RGB color for text segment highlighted foreground
     */
    public static final int TEXTSEGMENT_HIGHLIGHT_FOREGROUND = 5;
    /**
     * RGB color for text segment delay slot highlighted background
     */
    public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_BACKGROUND = 6;
    /**
     * RGB color for text segment delay slot highlighted foreground
     */
    public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FOREGROUND = 7;
    /**
     * RGB color for text segment highlighted background
     */
    public static final int DATASEGMENT_HIGHLIGHT_BACKGROUND = 8;
    /**
     * RGB color for text segment highlighted foreground
     */
    public static final int DATASEGMENT_HIGHLIGHT_FOREGROUND = 9;
    /**
     * RGB color for register highlighted background
     */
    public static final int REGISTER_HIGHLIGHT_BACKGROUND = 10;
    /**
     * RGB color for register highlighted foreground
     */
    public static final int REGISTER_HIGHLIGHT_FOREGROUND = 11;
    // Match the above by position.
    private static final String[] colorSettingsKeys = {"EvenRowBackground", "EvenRowForeground", "OddRowBackground", "OddRowForeground", "TextSegmentHighlightBackground", "TextSegmentHighlightForeground", "TextSegmentDelaySlotHighlightBackground", "TextSegmentDelaySlotHighlightForeground", "DataSegmentHighlightBackground", "DataSegmentHighlightForeground", "RegisterHighlightBackground", "RegisterHighlightForeground"};
    /**
     * Last resort default values for color settings;
     * will use only if neither the Preferences nor the properties file work.
     * If you wish to change, do so before instantiating the Settings object.
     * Must match key by list position.
     */
    private static final String[] defaultColorSettingsValues = {"0x00e0e0e0", "0", "0x00ffffff", "0", "0x00ffff99", "0", "0x0033ff00", "0", "0x0099ccff", "0", "0x0099cc55", "0"};

    private final String[] fontFamilySettingsValues;
    private final String[] fontStyleSettingsValues;
    private final String[] fontSizeSettingsValues;
    private final String[] colorSettingsValues;

    private final Preferences preferences;

    /**
     * Create Settings object and set to saved values.  If saved values not found, will set
     * based on defaults stored in Settings.properties file.  If file problems, will set based
     * on defaults stored in this class.
     */
    public Settings() {
        fontFamilySettingsValues = new String[fontFamilySettingsKeys.length];
        fontStyleSettingsValues = new String[fontStyleSettingsKeys.length];
        fontSizeSettingsValues = new String[fontSizeSettingsKeys.length];
        colorSettingsValues = new String[colorSettingsKeys.length];
        // This determines where the values are actually stored.  Actual implementation
        // is platform-dependent.  For Windows, they are stored in Registry.  To see,
        // run regedit and browse to: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\mars
        preferences = Preferences.userNodeForPackage(this.getClass());
        // The gui parameter, formerly passed to initialize(), is no longer needed
        // because I removed (1/21/09) the call to generate the Font object for the text editor.
        // Font objects are now generated only on demand so the "if (gui)" guard
        // is no longer necessary.  Originally added by Berkeley b/c they were running it on a 
        // headless server and running in command mode.  The Font constructor resulted in Swing 
        // initialization which caused problems.  Now this will only occur on demand from
        // Venus, which happens only when running as GUI.
        initialize();
    }

    /**
     * Return whether backstepping is permitted at this time.  Backstepping is ability to undo execution
     * steps one at a time.  Available only in the IDE.  This is not a persistent setting and is not under
     * MARS user control.
     *
     * @return true if backstepping is permitted, false otherwise.
     */
    public boolean getBackSteppingEnabled() {
        return (Application.program != null && Application.program.getBackStepper() != null && Application.program.getBackStepper().isEnabled());
    }

    /**
     * Reset settings to default values, as described in the constructor comments.
     */
    public void reset() {
        initialize();
    }

    /*
     * This section contains all code related to syntax highlighting styles settings.
     * A style includes 3 components: color, bold (t/f), italic (t/f)
     *
     * The fallback defaults will come not from an array here, but from the
     * existing static method SyntaxUtilities.getDefaultSyntaxStyles()
     * in the mars.venus.editors.jeditsyntax package.  It returns an array
     * of SyntaxStyle objects.
     */
    private String[] syntaxStyleColorSettingsValues;
    private boolean[] syntaxStyleBoldSettingsValues;
    private boolean[] syntaxStyleItalicSettingsValues;

    private static final String SYNTAX_STYLE_COLOR_PREFIX = "SyntaxStyleColor_";
    private static final String SYNTAX_STYLE_BOLD_PREFIX = "SyntaxStyleBold_";
    private static final String SYNTAX_STYLE_ITALIC_PREFIX = "SyntaxStyleItalic_";

    private static String[] syntaxStyleColorSettingsKeys, syntaxStyleBoldSettingsKeys, syntaxStyleItalicSettingsKeys;
    private static String[] defaultSyntaxStyleColorSettingsValues;
    private static boolean[] defaultSyntaxStyleBoldSettingsValues;
    private static boolean[] defaultSyntaxStyleItalicSettingsValues;

    public void setEditorSyntaxStyleByPosition(int index, SyntaxStyle syntaxStyle) {
        syntaxStyleColorSettingsValues[index] = syntaxStyle.getColorAsHexString();
        syntaxStyleItalicSettingsValues[index] = syntaxStyle.isItalic();
        syntaxStyleBoldSettingsValues[index] = syntaxStyle.isBold();
        saveEditorSyntaxStyle(index);
    }

    public SyntaxStyle getEditorSyntaxStyleByPosition(int index) {
        return new SyntaxStyle(getColorValueByPosition(index, syntaxStyleColorSettingsValues), syntaxStyleItalicSettingsValues[index], syntaxStyleBoldSettingsValues[index]);
    }

    public SyntaxStyle getDefaultEditorSyntaxStyleByPosition(int index) {
        return new SyntaxStyle(getColorValueByPosition(index, defaultSyntaxStyleColorSettingsValues), defaultSyntaxStyleItalicSettingsValues[index], defaultSyntaxStyleBoldSettingsValues[index]);
    }

    private void saveEditorSyntaxStyle(int index) {
        try {
            preferences.put(syntaxStyleColorSettingsKeys[index], syntaxStyleColorSettingsValues[index]);
            preferences.putBoolean(syntaxStyleBoldSettingsKeys[index], syntaxStyleBoldSettingsValues[index]);
            preferences.putBoolean(syntaxStyleItalicSettingsKeys[index], syntaxStyleItalicSettingsValues[index]);
            preferences.flush();
        }
        catch (SecurityException se) {
            // cannot write to persistent storage for security reasons
        }
        catch (BackingStoreException bse) {
            // unable to communicate with persistent storage (strange days)
        }
    }

    // For syntax styles, need to initialize from SyntaxUtilities defaults.
    // Taking care not to explicitly create a Color object, since it may trigger
    // Swing initialization (that caused problems for UC Berkeley when we
    // created Font objects here).  It shouldn't, but then again Font shouldn't
    // either but they said it did.  (see HeadlessException)   
    // On the other hand, the first statement of this method causes Color objects
    // to be created!  It is possible but a real pain in the rear to avoid using 
    // Color objects totally.  Requires new methods for the SyntaxUtilities class.
    private void initializeEditorSyntaxStyles() {
        SyntaxStyle[] syntaxStyle = SyntaxUtilities.getDefaultSyntaxStyles();
        int tokens = syntaxStyle.length;
        syntaxStyleColorSettingsKeys = new String[tokens];
        syntaxStyleBoldSettingsKeys = new String[tokens];
        syntaxStyleItalicSettingsKeys = new String[tokens];
        defaultSyntaxStyleColorSettingsValues = new String[tokens];
        defaultSyntaxStyleBoldSettingsValues = new boolean[tokens];
        defaultSyntaxStyleItalicSettingsValues = new boolean[tokens];
        syntaxStyleColorSettingsValues = new String[tokens];
        syntaxStyleBoldSettingsValues = new boolean[tokens];
        syntaxStyleItalicSettingsValues = new boolean[tokens];
        for (int i = 0; i < tokens; i++) {
            syntaxStyleColorSettingsKeys[i] = SYNTAX_STYLE_COLOR_PREFIX + i;
            syntaxStyleBoldSettingsKeys[i] = SYNTAX_STYLE_BOLD_PREFIX + i;
            syntaxStyleItalicSettingsKeys[i] = SYNTAX_STYLE_ITALIC_PREFIX + i;
            syntaxStyleColorSettingsValues[i] = defaultSyntaxStyleColorSettingsValues[i] = syntaxStyle[i].getColorAsHexString();
            syntaxStyleBoldSettingsValues[i] = defaultSyntaxStyleBoldSettingsValues[i] = syntaxStyle[i].isBold();
            syntaxStyleItalicSettingsValues[i] = defaultSyntaxStyleItalicSettingsValues[i] = syntaxStyle[i].isItalic();
        }
    }

    private void getEditorSyntaxStyleSettingsFromPreferences() {
        for (int i = 0; i < syntaxStyleColorSettingsKeys.length; i++) {
            syntaxStyleColorSettingsValues[i] = preferences.get(syntaxStyleColorSettingsKeys[i], syntaxStyleColorSettingsValues[i]);
            syntaxStyleBoldSettingsValues[i] = preferences.getBoolean(syntaxStyleBoldSettingsKeys[i], syntaxStyleBoldSettingsValues[i]);
            syntaxStyleItalicSettingsValues[i] = preferences.getBoolean(syntaxStyleItalicSettingsKeys[i], syntaxStyleItalicSettingsValues[i]);
        }
    }

    /**
     * Current editor font.  Retained for compatibility but replaced
     * by: getFontByPosition(Settings.EDITOR_FONT)
     *
     * @return Font object for current editor font.
     */
    public Font getEditorFont() {
        return getFontByPosition(EDITOR_FONT);
    }

    /**
     * Retrieve a Font setting
     *
     * @param fontSettingPosition constant that identifies which item
     * @return Font object for given item
     */
    public Font getFontByPosition(int fontSettingPosition) {
        if (fontSettingPosition >= 0 && fontSettingPosition < fontFamilySettingsValues.length) {
            return EditorFont.createFontFromStringValues(fontFamilySettingsValues[fontSettingPosition], fontStyleSettingsValues[fontSettingPosition], fontSizeSettingsValues[fontSettingPosition]);
        }
        else {
            return null;
        }
    }

    /**
     * Retrieve a default Font setting
     *
     * @param fontSettingPosition Constant that identifies which item.
     * @return Font object for given item
     */
    public Font getDefaultFontByPosition(int fontSettingPosition) {
        if (fontSettingPosition >= 0 && fontSettingPosition < defaultFontFamilySettingsValues.length) {
            return EditorFont.createFontFromStringValues(defaultFontFamilySettingsValues[fontSettingPosition], defaultFontStyleSettingsValues[fontSettingPosition], defaultFontSizeSettingsValues[fontSettingPosition]);
        }
        else {
            return null;
        }
    }

    /**
     * Get Color object for specified settings key.
     * Returns null if key is not found or its value is not a valid color encoding.
     *
     * @param key The Setting key
     * @return Corresponding Color, or null if key not found or value not valid color.
     */
    public Color getColorSettingByKey(String key) {
        return getColorValueByKey(key, colorSettingsValues);
    }

    /**
     * Get default Color value for specified settings key.
     * Returns null if key is not found or its value is not a valid color encoding.
     *
     * @param key the Setting key
     * @return corresponding default Color, or null if key not found or value not valid color
     */
    public Color getDefaultColorSettingByKey(String key) {
        return getColorValueByKey(key, defaultColorSettingsValues);
    }

    /**
     * Get Color object for specified settings name (a static constant).
     * Returns null if argument invalid or its value is not a valid color encoding.
     *
     * @param position the Setting name (see list of static constants)
     * @return corresponding Color, or null if argument invalid or value not valid color
     */
    public Color getColorSettingByPosition(int position) {
        return getColorValueByPosition(position, colorSettingsValues);
    }

    /**
     * Get default Color object for specified settings name (a static constant).
     * Returns null if argument invalid or its value is not a valid color encoding.
     *
     * @param position the Setting name (see list of static constants)
     * @return corresponding default Color, or null if argument invalid or value not valid color
     */
    public Color getDefaultColorSettingByPosition(int position) {
        return getColorValueByPosition(position, defaultColorSettingsValues);
    }

    ////////////////////////////////////////////////////////////////////////
    //  Setting Setters
    ////////////////////////////////////////////////////////////////////////

    /**
     * Set editor font to the specified Font object and write it to persistent storage.
     * This method retained for compatibility but replaced by:
     * setFontByPosition(Settings.EDITOR_FONT, font)
     *
     * @param font Font object to be used by text editor.
     */
    public void setEditorFont(Font font) {
        setFontByPosition(EDITOR_FONT, font);
    }

    /**
     * Store a Font setting
     *
     * @param fontSettingPosition Constant that identifies the item the font goes with
     * @param font                The font to set that item to
     */
    public void setFontByPosition(int fontSettingPosition, Font font) {
        if (fontSettingPosition >= 0 && fontSettingPosition < fontFamilySettingsValues.length) {
            fontFamilySettingsValues[fontSettingPosition] = font.getFamily();
            fontStyleSettingsValues[fontSettingPosition] = EditorFont.getStyle(font.getStyle()).getName();
            fontSizeSettingsValues[fontSettingPosition] = EditorFont.sizeIntToSizeString(font.getSize());
            saveFontSetting(fontSettingPosition, fontFamilySettingsKeys, fontFamilySettingsValues);
            saveFontSetting(fontSettingPosition, fontStyleSettingsKeys, fontStyleSettingsValues);
            saveFontSetting(fontSettingPosition, fontSizeSettingsKeys, fontSizeSettingsValues);
        }
        if (fontSettingPosition == EDITOR_FONT) {
            setChanged();
            notifyObservers();
        }
    }

    /**
     * Set Color object for specified settings key.  Has no effect if key is invalid.
     *
     * @param key   the Setting key
     * @param color the Color to save
     */
    public void setColorSettingByKey(String key, Color color) {
        for (int i = 0; i < colorSettingsKeys.length; i++) {
            if (key.equals(colorSettingsKeys[i])) {
                setColorSettingByPosition(i, color);
                return;
            }
        }
    }

    /**
     * Set Color object for specified settings name (a static constant). Has no effect if invalid.
     *
     * @param position the Setting name (see list of static constants)
     * @param color    the Color to save
     */
    public void setColorSettingByPosition(int position, Color color) {
        if (position >= 0 && position < colorSettingsKeys.length) {
            setColorSetting(position, color);
        }
    }

    /////////////////////////////////////////////////////////////////////////
    //
    //     PRIVATE HELPER METHODS TO DO THE REAL WORK
    //
    /////////////////////////////////////////////////////////////////////////

    // Initialize settings to default values.
    // Strategy: First set from properties file.  
    //           If that fails, set from array.
    //           In either case, use these values as defaults in call to Preferences.

    private void initialize() {
        applyDefaultSettings();
        if (!readSettingsFromPropertiesFile()) {
            System.out.println("MARS System error: unable to read Settings.properties defaults. Using built-in defaults.");
        }
        getSettingsFromPreferences();
    }

    // Default values.  Will be replaced if available from property file or Preferences object.
    private void applyDefaultSettings() {
        for (int i = 0; i < fontFamilySettingsValues.length; i++) {
            fontFamilySettingsValues[i] = defaultFontFamilySettingsValues[i];
            fontStyleSettingsValues[i] = defaultFontStyleSettingsValues[i];
            fontSizeSettingsValues[i] = defaultFontSizeSettingsValues[i];
        }
        System.arraycopy(defaultColorSettingsValues, 0, colorSettingsValues, 0, colorSettingsValues.length);
        initializeEditorSyntaxStyles();
    }

    /**
     * Save the key-value pair in the {@link Preferences} object and ensure it is written to persistent storage.
     * In most cases, the {@link BooleanSetting#set(boolean)} method should be used on an existing
     * setting instead of calling this method directly.
     */
    public void saveBooleanSetting(String key, boolean value, boolean notify) {
        try {
            this.preferences.putBoolean(key, value);
            this.preferences.flush();
        }
        catch (SecurityException se) {
            // Cannot write to persistent storage for security reasons
        }
        catch (BackingStoreException bse) {
            // Unable to communicate with persistent storage (strange days)
        }
        if (notify) {
            // Signal observers that the setting has changed
            setChanged();
            notifyObservers();
        }
    }

    public void saveIntegerSetting(String key, int value, boolean notify) {
        try {
            this.preferences.putInt(key, value);
            this.preferences.flush();
        }
        catch (SecurityException se) {
            // Cannot write to persistent storage for security reasons
        }
        catch (BackingStoreException bse) {
            // Unable to communicate with persistent storage (strange days)
        }
        if (notify) {
            // Signal observers that the setting has changed
            setChanged();
            notifyObservers();
        }
    }

    /**
     * Save the key-value pair in the {@link Preferences} object and ensure it is written to persistent storage.
     * In most cases, the {@link StringSetting#set(String)} method should be used on an existing
     * setting instead of calling this method directly.
     */
    public void saveStringSetting(String key, String value, boolean notify) {
        try {
            preferences.put(key, value);
            preferences.flush();
        }
        catch (SecurityException se) {
            // Cannot write to persistent storage for security reasons
        }
        catch (BackingStoreException bse) {
            // Unable to communicate with persistent storage (strange days)
        }
        if (notify) {
            // Signal observers that the setting has changed
            setChanged();
            notifyObservers();
        }
    }

    /**
     * Save the key-value pairs in the {@link Preferences} object and ensure they is written to persistent storage.
     * In most cases, the {@link FontSetting#set(Font)} method should be used on an existing
     * setting instead of calling this method directly.
     */
    public void saveFontSetting(String key, String family, String style, String size, boolean notify) {
        try {
            preferences.put(key + FONT_FAMILY_SUFFIX, family);
            preferences.put(key + FONT_STYLE_SUFFIX, style);
            preferences.put(key + FONT_SIZE_SUFFIX, size);
            preferences.flush();
        }
        catch (SecurityException se) {
            // Cannot write to persistent storage for security reasons
        }
        catch (BackingStoreException bse) {
            // Unable to communicate with persistent storage (strange days)
        }
        if (notify) {
            // Signal observers that the setting has changed
            setChanged();
            notifyObservers();
        }
    }

    // Used by setter methods for color-based settings
    private void setColorSetting(int settingIndex, Color color) {
        colorSettingsValues[settingIndex] = Binary.intToHexString(color.getRed() << 16 | color.getGreen() << 8 | color.getBlue());
        saveColorSetting(settingIndex);
    }

    /**
     * Get Color object for this key value.  Get it from values array provided as argument (could be either
     * the current or the default settings array).
     */
    private Color getColorValueByKey(String key, String[] values) {
        for (int i = 0; i < colorSettingsKeys.length; i++) {
            if (key.equals(colorSettingsKeys[i])) {
                return getColorValueByPosition(i, values);
            }
        }
        return null;
    }

    /**
     * Get Color object for this key array position.  Get it from values array provided as argument (could be either
     * the current or the default settings array).
     */
    private Color getColorValueByPosition(int position, String[] values) {
        Color color = null;
        if (position >= 0 && position < colorSettingsKeys.length) {
            try {
                color = Color.decode(values[position]);
            }
            catch (NumberFormatException ignored) {
            }
        }
        return color;
    }

    /**
     * Establish the settings from the given properties file.  Return true if it worked,
     * false if it didn't.  Note the properties file exists only to provide default values
     * in case the Preferences fail or have not been recorded yet.
     * <p>
     * Any settings successfully read will be stored in both the xSettingsValues and
     * defaultXSettingsValues arrays (x=boolean,string,color).  The latter will overwrite the
     * last-resort default values hardcoded into the arrays above.
     * <p>
     * NOTE: If there is NO ENTRY for the specified property, {@link Properties#getProperty(String)} returns
     * null.  This is no cause for alarm.  It will occur during system development or upon the
     * first use of a new MARS release in which new settings have been defined.
     * In that case, this method will NOT make an assignment to the settings array!
     * So consider it a precondition of this method: the settings arrays must already be
     * initialized with last-resort default values.
     */
    private boolean readSettingsFromPropertiesFile() {
        try {
            Properties properties = PropertiesFile.loadPropertiesFromFile(SETTINGS_FILENAME);
            // TODO: put all settings in one array using an interface?
            // Load boolean settings
            for (BooleanSetting setting : booleanSettings) {
                String property = properties.getProperty(setting.getKey());
                if (property != null) {
                    setting.set(Boolean.parseBoolean(property));
                }
            }
            // Load integer settings
            for (IntegerSetting setting : integerSettings) {
                String property = properties.getProperty(setting.getKey());
                if (property != null) {
                    try {
                        setting.set(Integer.parseInt(property));
                    }
                    catch (NumberFormatException e) {
                        // Keep the default value
                    }
                }
            }
            // Load string settings
            for (StringSetting setting : stringSettings) {
                String property = properties.getProperty(setting.getKey());
                if (property != null) {
                    try {
                        setting.set(property);
                    }
                    catch (NumberFormatException e) {
                        // Keep the default value
                    }
                }
            }
            // Load font settings
            for (int i = 0; i < fontFamilySettingsValues.length; i++) {
                String settingValue = properties.getProperty(fontFamilySettingsKeys[i]);
                if (settingValue != null) {
                    fontFamilySettingsValues[i] = defaultFontFamilySettingsValues[i] = settingValue;
                }
                settingValue = properties.getProperty(fontStyleSettingsKeys[i]);
                if (settingValue != null) {
                    fontStyleSettingsValues[i] = defaultFontStyleSettingsValues[i] = settingValue;
                }
                settingValue = properties.getProperty(fontSizeSettingsKeys[i]);
                if (settingValue != null) {
                    fontSizeSettingsValues[i] = defaultFontSizeSettingsValues[i] = settingValue;
                }
            }
            // Load color settings
            for (int i = 0; i < colorSettingsKeys.length; i++) {
                String settingValue = properties.getProperty(colorSettingsKeys[i]);
                if (settingValue != null) {
                    colorSettingsValues[i] = defaultColorSettingsValues[i] = settingValue;
                }
            }
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Get settings values from Preferences object.  A key-value pair will only be written
     * to Preferences if/when the value is modified.  If it has not been modified, the default value
     * will be returned here.
     * <p>
     * PRECONDITION: Values arrays have already been initialized to default values from
     * Settings.properties file or default value arrays above!
     */
    private void getSettingsFromPreferences() {
        for (BooleanSetting setting : booleanSettings) {
            setting.set(preferences.getBoolean(setting.getKey(), setting.getDefault()));
        }
        for (IntegerSetting setting : integerSettings) {
            setting.set(preferences.getInt(setting.getKey(), setting.getDefault()));
        }
        for (StringSetting setting : stringSettings) {
            setting.set(preferences.get(setting.getKey(), setting.getDefault()));
        }
        for (int i = 0; i < fontFamilySettingsKeys.length; i++) {
            fontFamilySettingsValues[i] = preferences.get(fontFamilySettingsKeys[i], fontFamilySettingsValues[i]);
            fontStyleSettingsValues[i] = preferences.get(fontStyleSettingsKeys[i], fontStyleSettingsValues[i]);
            fontSizeSettingsValues[i] = preferences.get(fontSizeSettingsKeys[i], fontSizeSettingsValues[i]);
        }
        for (int i = 0; i < colorSettingsKeys.length; i++) {
            colorSettingsValues[i] = preferences.get(colorSettingsKeys[i], colorSettingsValues[i]);
        }
        getEditorSyntaxStyleSettingsFromPreferences();
    }

    /**
     * Save the key-value pair in the Properties object and ensure it is written to persistent storage.
     */
    private void saveFontSetting(int index, String[] settingsKeys, String[] settingsValues) {
        try {
            preferences.put(settingsKeys[index], settingsValues[index]);
            preferences.flush();
        }
        catch (SecurityException se) {
            // cannot write to persistent storage for security reasons
        }
        catch (BackingStoreException bse) {
            // unable to communicate with persistent storage (strange days)
        }
    }

    /**
     * Save the key-value pair in the Properties object and ensure it is written to persistent storage.
     */
    private void saveColorSetting(int index) {
        try {
            preferences.put(colorSettingsKeys[index], colorSettingsValues[index]);
            preferences.flush();
        }
        catch (SecurityException se) {
            // cannot write to persistent storage for security reasons
        }
        catch (BackingStoreException bse) {
            // unable to communicate with persistent storage (strange days)
        }
    }
}