package mars.settings;

import mars.util.Binary;
import mars.util.PropertiesFile;
import mars.venus.editor.jeditsyntax.SyntaxStyle;
import mars.venus.editor.jeditsyntax.tokenmarker.Token;

import java.awt.*;
import java.util.*;
import java.util.List;
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
 * Failing that, default setting values come from DefaultSettings.properties file.
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
public class Settings {
    /**
     * Name of properties file used to hold default settings.
     */
    private static final String DEFAULT_SETTINGS_PROPERTIES = "DefaultSettings";

    // This determines where the values are actually stored.  Actual implementation
    // is platform-dependent.  For Windows, they are stored in Registry.  To see,
    // run regedit and browse to: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\mars
    private final Preferences preferences = Preferences.userNodeForPackage(Settings.class);

    public interface Listener extends EventListener {
        void settingsChanged();
    }

    private final List<Listener> listeners = new ArrayList<>();

    public void addListener(Listener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public void dispatchChangedEvent() {
        for (Listener listener : this.listeners) {
            listener.settingsChanged();
        }
    }

    // BOOLEAN SETTINGS

    /**
     * Flag to control whether the assembler will be limited to basic MIPS instructions and formats.
     */
    public final BooleanSetting extendedAssemblerEnabled = new BooleanSetting(
        this,
        "ExtendedAssembler",
        true,
        false
    );
    /**
     * Flag to control whether a file is immediately and automatically assembled upon opening.
     * Handy when using an external editor, for example.
     */
    public final BooleanSetting assembleOnOpenEnabled = new BooleanSetting(
        this,
        "AssembleOnOpen",
        false,
        false
    );
    /**
     * Flag to control whether the symbol table window will be visible upon successful assembly in the Execute tab.
     */
    public final BooleanSetting labelWindowVisible = new BooleanSetting(
        this,
        "LabelWindowVisibility",
        false,
        false
    );
    /**
     * Flag to control whether memory addresses are displayed in hexadecimal.
     * If this flag is set to false, they are displayed in decimal.
     */
    public final BooleanSetting displayAddressesInHex = new BooleanSetting(
        this,
        "DisplayAddressesInHex",
        true,
        true
    );
    /**
     * Flag to control whether operand, memory, and register values are displayed in hexadecimal.
     * If this flag is set to false, they are displayed in decimal.
     */
    public final BooleanSetting displayValuesInHex = new BooleanSetting(
        this,
        "DisplayValuesInHex",
        true,
        true
    );
    /**
     * Flag to control whether the currently selected exception handler source file will
     * be included in each assembly operation.
     */
    public final BooleanSetting exceptionHandlerEnabled = new BooleanSetting(
        this,
        "LoadExceptionHandler",
        false,
        false
    );
    /**
     * Flag to control whether delayed branching is simulated. Delayed branching is a feature of actual hardware
     * where the instruction immediately following a branch instruction is <i>always</i> executed, even if the
     * branch is taken, due to CPU pipelining. Thus, with delayed branching disabled, there is effectively
     * a hidden <code>nop</code> after every branch instruction.
     * <p>
     * DPS 14 June 2007
     */
    public final BooleanSetting delayedBranchingEnabled = new BooleanSetting(
        this,
        "DelayedBranching",
        false,
        true
    );
    /**
     * Flag to control whether the editor will display line numbers on the side.
     */
    public final BooleanSetting displayEditorLineNumbers = new BooleanSetting(
        this,
        "EditorLineNumbersDisplayed",
        true,
        true
    );
    /**
     * Flag to control whether assembler warnings are automatically promoted to errors.
     */
    public final BooleanSetting warningsAreErrors = new BooleanSetting(
        this,
        "WarningsAreErrors",
        false,
        false
    );
    /**
     * Flag to control whether to accept and use program arguments.
     */
    public final BooleanSetting useProgramArguments = new BooleanSetting(
        this,
        "ProgramArguments",
        false,
        true
    );
    /**
     * Flag to control whether values in the memory viewer are highlighted during stepped execution.
     */
    public final BooleanSetting memoryHighlightingEnabled = new BooleanSetting(
        this,
        "DataSegmentHighlighting",
        true,
        true
    );
    /**
     * Flag to control whether registers in the register window are highlighted during stepped execution.
     */
    public final BooleanSetting registerHighlightingEnabled = new BooleanSetting(
        this,
        "RegistersHighlighting",
        true,
        true
    );
    /**
     * Flag to control whether the assembler automatically initializes program counter to the address
     * of global label <code>main</code>, if defined.
     */
    public final BooleanSetting startAtMain = new BooleanSetting(
        this,
        "StartAtMain",
        true,
        false
    );
    /**
     * Flag to control whether the editor will highlight the line currently being edited.
     */
    public final BooleanSetting highlightCurrentEditorLine = new BooleanSetting(
        this,
        "EditorCurrentLineHighlighting",
        true,
        true
    );
    /**
     * Flag to control whether the editor will provide popup instruction guidance while typing.
     */
    public final BooleanSetting popupInstructionGuidance = new BooleanSetting(
        this,
        "PopupInstructionGuidance",
        true,
        true
    );
    /**
     * Flag to control whether the simulator will use popup dialogs for input syscalls.
     */
    public final BooleanSetting popupSyscallInput = new BooleanSetting(
        this,
        "PopupSyscallInput",
        false,
        false
    );
    /**
     * Flag to control whether the editor will use the auto-indent feature.
     */
    public final BooleanSetting autoIndentEnabled = new BooleanSetting(
        this,
        "AutoIndent",
        true,
        false
    );
    /**
     * Flag to control whether a program can write binary code to the text or data segment and execute that code.
     */
    public final BooleanSetting selfModifyingCodeEnabled = new BooleanSetting(
        this,
        "SelfModifyingCode",
        false,
        true
    );
    /**
     * Flag to control whether the big-endian byte ordering is used in memory and register addressing.
     * If true, big-endian is used, meaning bytes of values are stored from most to least significant.
     * If false, little-endian is used instead, meaning bytes are stored from least to most significant.
     */
    public final BooleanSetting useBigEndian = new BooleanSetting(
        this,
        "BigEndian",
        false,
        false
    );

    public final BooleanSetting[] booleanSettings = {
        this.extendedAssemblerEnabled,
        this.assembleOnOpenEnabled,
        this.labelWindowVisible,
        this.displayAddressesInHex,
        this.displayValuesInHex,
        this.exceptionHandlerEnabled,
        this.delayedBranchingEnabled,
        this.displayEditorLineNumbers,
        this.warningsAreErrors,
        this.useProgramArguments,
        this.memoryHighlightingEnabled,
        this.registerHighlightingEnabled,
        this.startAtMain,
        this.highlightCurrentEditorLine,
        this.popupInstructionGuidance,
        this.popupSyscallInput,
        this.autoIndentEnabled,
        this.selfModifyingCodeEnabled,
        this.useBigEndian,
    };

    // INTEGER SETTINGS

    /**
     * UI scale multiplier as a percentage. 100 is at 1:1 scale.
     */
    public final IntegerSetting uiScale = new IntegerSetting(
        this,
        "UIScale",
        100,
        true
    );
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
     * Caret blink rate in milliseconds. 0 means don't blink.
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

    public final IntegerSetting[] integerSettings = {
        this.uiScale,
        this.symbolTableSortState,
        this.caretBlinkRate,
        this.editorTabSize,
        this.editorPopupPrefixLength,
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
     * Stored as the ordered column indices separated by spaces.
     */
    public final StringSetting textSegmentColumnOrder = new StringSetting(
        this,
        "TextColumnOrder",
        "0 1 2 3 4",
        false
    );
    /**
     * The list of files which were open in tabs in the previous session.
     * Stored as the full file paths separated by semicolons.
     */
    public final StringSetting previouslyOpenFiles = new StringSetting(
        this,
        "PreviouslyOpenFiles",
        "",
        false
    );
    /**
     * The name of the look and feel to use for the GUI.
     */
    public final StringSetting lookAndFeelName = new StringSetting(
        this,
        "LookAndFeel",
        "FlatDarkLaf",
        false
    );

    public final StringSetting[] stringSettings = {
        this.exceptionHandlerPath,
        this.memoryConfiguration,
        this.textSegmentColumnOrder,
        this.previouslyOpenFiles,
        this.lookAndFeelName,
    };

    // COLOR SETTINGS

    /**
     * RGB color for text segment highlighted background
     */
    public final ColorSetting textSegmentHighlightBackground = new ColorSetting(
        this,
        "TextSegmentHighlightBackground",
        new Color(0xE9AA4B),
        false
    );
    /**
     * RGB color for text segment highlighted foreground
     */
    public final ColorSetting textSegmentHighlightForeground = new ColorSetting(
        this,
        "TextSegmentHighlightForeground",
        new Color(0x000000),
        false
    );
    /**
     * RGB color for text segment delay slot highlighted background
     */
    public final ColorSetting textSegmentDelaySlotHighlightBackground = new ColorSetting(
        this,
        "TextSegmentDelaySlotHighlightBackground",
        new Color(0x99CC55),
        false
    );
    /**
     * RGB color for text segment delay slot highlighted foreground
     */
    public final ColorSetting textSegmentDelaySlotHighlightForeground = new ColorSetting(
        this,
        "TextSegmentDelaySlotHighlightForeground",
        new Color(0x000000),
        false
    );
    /**
     * RGB color for text segment highlighted background
     */
    public final ColorSetting dataSegmentHighlightBackground = new ColorSetting(
        this,
        "DataSegmentHighlightBackground",
        new Color(0x5A81FD),
        false
    );
    /**
     * RGB color for text segment highlighted foreground
     */
    public final ColorSetting dataSegmentHighlightForeground = new ColorSetting(
        this,
        "DataSegmentHighlightForeground",
        new Color(0x000000),
        false
    );
    /**
     * RGB color for register highlighted background
     */
    public final ColorSetting registerHighlightBackground = new ColorSetting(
        this,
        "RegisterHighlightBackground",
        new Color(0x3C9862),
        false
    );
    /**
     * RGB color for register highlighted foreground
     */
    public final ColorSetting registerHighlightForeground = new ColorSetting(
        this,
        "RegisterHighlightForeground",
        new Color(0x000000),
        false
    );

    public final ColorSetting[] colorSettings = {
        this.textSegmentHighlightBackground,
        this.textSegmentHighlightForeground,
        this.textSegmentDelaySlotHighlightBackground,
        this.textSegmentDelaySlotHighlightForeground,
        this.dataSegmentHighlightBackground,
        this.dataSegmentHighlightForeground,
        this.registerHighlightBackground,
        this.registerHighlightForeground,
    };

    // FONT SETTINGS

    /**
     * Font for the text editor
     */
    public final FontSetting editorFont = new FontSetting(
        this,
        "EditorFont",
        new Font(Font.MONOSPACED, Font.PLAIN, 12),
        false
    );
    /**
     * Font for table even row background (text, data, register displays)
     */
    public final FontSetting tableFont = new FontSetting(
        this,
        "TableFont",
        new Font(Font.MONOSPACED, Font.PLAIN, 12),
        false
    );
    /**
     * Font for table odd row background (text, data, register displays)
     */
    public final FontSetting tableHighlightFont = new FontSetting(
        this,
        "TableHighlightFont",
        new Font(Font.MONOSPACED, Font.BOLD, 12),
        false
    );

    public final FontSetting[] fontSettings = {
        this.editorFont,
        this.tableFont,
        this.tableHighlightFont,
    };

    // SYNTAX STYLE SETTINGS

    public final SyntaxStyleSetting syntaxStyleComment = new SyntaxStyleSetting(
        this,
        "SyntaxStyleComment",
        new SyntaxStyle(new Color(0x666666), false, false),
        false
    );
    public final SyntaxStyleSetting syntaxStyleInstruction = new SyntaxStyleSetting(
        this,
        "SyntaxStyleInstruction",
        new SyntaxStyle(new Color(0xF27541), false, false),
        false
    );
    public final SyntaxStyleSetting syntaxStyleDirective = new SyntaxStyleSetting(
        this,
        "SyntaxStyleDirective",
        new SyntaxStyle(new Color(0x5A81FD), false, false),
        false
    );
    public final SyntaxStyleSetting syntaxStyleRegister = new SyntaxStyleSetting(
        this,
        "SyntaxStyleRegister",
        new SyntaxStyle(new Color(0xE9AA4B), false, false),
        false
    );
    public final SyntaxStyleSetting syntaxStyleStringLiteral = new SyntaxStyleSetting(
        this,
        "SyntaxStyleStringLiteral",
        new SyntaxStyle(new Color(0x3C9862), false, false),
        false
    );
    public final SyntaxStyleSetting syntaxStyleCharLiteral = new SyntaxStyleSetting(
        this,
        "SyntaxStyleCharLiteral",
        new SyntaxStyle(new Color(0x3C9862), false, false),
        false
    );
    public final SyntaxStyleSetting syntaxStyleLabel = new SyntaxStyleSetting(
        this,
        "SyntaxStyleLabel",
        new SyntaxStyle(new Color(0x2DB7AE), false, true),
        false
    );
    public final SyntaxStyleSetting syntaxStyleInvalid = new SyntaxStyleSetting(
        this,
        "SyntaxStyleInvalid",
        new SyntaxStyle(new Color(0xFF3F3F), false, false),
        false
    );
    public final SyntaxStyleSetting syntaxStyleMacroArgument = new SyntaxStyleSetting(
        this,
        "SyntaxStyleMacroArgument",
        new SyntaxStyle(new Color(0xDE8ACA), false, false),
        false
    );

    public final SyntaxStyleSetting[] syntaxStyleSettings = this.getStyleSettingsArray();

    private SyntaxStyleSetting[] getStyleSettingsArray() {
        SyntaxStyleSetting[] styles = new SyntaxStyleSetting[Token.ID_COUNT];
        styles[Token.COMMENT] = this.syntaxStyleComment;
        styles[Token.INSTRUCTION] = this.syntaxStyleInstruction;
        styles[Token.DIRECTIVE] = this.syntaxStyleDirective;
        styles[Token.REGISTER] = this.syntaxStyleRegister;
        styles[Token.STRING_LITERAL] = this.syntaxStyleStringLiteral;
        styles[Token.CHAR_LITERAL] = this.syntaxStyleCharLiteral;
        styles[Token.LABEL] = this.syntaxStyleLabel;
        styles[Token.INVALID] = this.syntaxStyleInvalid;
        styles[Token.MACRO_ARGUMENT] = this.syntaxStyleMacroArgument;
        return styles;
    }

    /**
     * Create Settings object and set to saved values.  If saved values not found, will set
     * based on defaults stored in DefaultSettings.properties file.  If file problems, will set based
     * on defaults stored in this class.
     */
    public Settings() {
        this.loadValues();
    }

    public SyntaxStyle getSyntaxStyle(int index) {
        SyntaxStyleSetting setting = this.syntaxStyleSettings[index];
        return (setting != null) ? setting.get() : null;
    }

    public void setSyntaxStyle(int index, SyntaxStyle style) {
        SyntaxStyleSetting setting = this.syntaxStyleSettings[index];
        if (setting != null) {
            setting.set(style);
        }
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
        catch (SecurityException | BackingStoreException exception) {
            System.err.println("Error: failed to write settings to persistent storage:");
            exception.printStackTrace(System.err);
        }
        if (notify) {
            this.dispatchChangedEvent();
        }
    }

    public void saveIntegerSetting(String key, int value, boolean notify) {
        try {
            this.preferences.putInt(key, value);
            this.preferences.flush();
        }
        catch (SecurityException | BackingStoreException exception) {
            System.err.println("Error: failed to write settings to persistent storage:");
            exception.printStackTrace(System.err);
        }
        if (notify) {
            this.dispatchChangedEvent();
        }
    }

    /**
     * Save the key-value pair in the {@link Preferences} object and ensure it is written to persistent storage.
     * In most cases, the {@link StringSetting#set(String)} method should be used on an existing
     * setting instead of calling this method directly.
     */
    public void saveStringSetting(String key, String value, boolean notify) {
        try {
            this.preferences.put(key, value);
            this.preferences.flush();
        }
        catch (SecurityException | BackingStoreException exception) {
            System.err.println("Error: failed to write settings to persistent storage:");
            exception.printStackTrace(System.err);
        }
        if (notify) {
            this.dispatchChangedEvent();
        }
    }

    /**
     * Establish the settings from the given properties file.  Return true if it worked,
     * false if it didn't.  Note the properties file exists only to provide default values
     * in case the Preferences fail or have not been recorded yet.
     * <p>
     * Then, get settings values from Preferences object.  A key-value pair will only be written
     * to Preferences if/when the value is modified.  If it has not been modified, the default value
     * will be returned here.
     */
    public void loadValues() {
        Properties defaults = PropertiesFile.loadPropertiesFromFile(DEFAULT_SETTINGS_PROPERTIES);

        // Load boolean settings
        for (BooleanSetting setting : this.booleanSettings) {
            String property = defaults.getProperty(setting.getKey());
            if (property != null) {
                setting.setDefault(Boolean.parseBoolean(property));
                setting.setNonPersistent(setting.getDefault());
            }
            setting.set(this.preferences.getBoolean(setting.getKey(), setting.getDefault()));
        }
        // Load integer settings
        for (IntegerSetting setting : this.integerSettings) {
            String property = defaults.getProperty(setting.getKey());
            if (property != null) {
                try {
                    setting.setDefault(Binary.decodeInteger(property));
                    setting.setNonPersistent(setting.getDefault());
                }
                catch (NumberFormatException exception) {
                    // Keep the default value
                }
            }
            setting.set(this.preferences.getInt(setting.getKey(), setting.getDefault()));
        }
        // Load string settings
        for (StringSetting setting : this.stringSettings) {
            String property = defaults.getProperty(setting.getKey());
            if (property != null) {
                setting.setDefault(property);
                setting.setNonPersistent(setting.getDefault());
            }
            setting.set(this.preferences.get(setting.getKey(), setting.getDefault()));
        }
        // Load color settings
        for (ColorSetting setting : this.colorSettings) {
            String property = defaults.getProperty(setting.getKey());
            if (property != null) {
                Color color = ColorSetting.decode(property);
                if (color != null) {
                    setting.setDefault(color);
                    setting.setNonPersistent(color);
                }
            }
            Color color = ColorSetting.decode(this.preferences.get(setting.getKey(), null));
            if (color != null) {
                setting.set(color);
            }
        }
        // Load font settings
        for (FontSetting setting : this.fontSettings) {
            String property = defaults.getProperty(setting.getKey());
            if (property != null) {
                Font font = FontSetting.decode(property);
                setting.setDefault(font);
                setting.setNonPersistent(font);
            }
            String fontString = this.preferences.get(setting.getKey(), null);
            if (fontString != null) {
                setting.set(FontSetting.decode(fontString));
            }
        }
        // Load syntax style settings
        for (SyntaxStyleSetting setting : this.syntaxStyleSettings) {
            if (setting != null) {
                String property = defaults.getProperty(setting.getKey());
                if (property != null) {
                    SyntaxStyle style = SyntaxStyleSetting.decode(property);
                    setting.setDefault(style);
                    setting.setNonPersistent(style);
                }
                String styleString = this.preferences.get(setting.getKey(), null);
                if (styleString != null) {
                    setting.set(SyntaxStyleSetting.decode(styleString));
                }
            }
        }
    }
}