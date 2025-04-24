package mars.settings;

import mars.util.Binary;
import mars.venus.editor.jeditsyntax.SyntaxStyle;
import mars.venus.editor.jeditsyntax.tokenmarker.Token;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
 * Failing that, default setting values come from default_settings.properties file.
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
     * Path to the properties file which contains default settings.
     */
    private static final String DEFAULT_SETTINGS_PATH = "/config/default_settings.properties";

    public static String encodeFileList(List<File> files) {
        StringBuilder output = new StringBuilder();
        for (File file : files) {
            output.append(file.getPath()).append(';');
        }
        return output.toString();
    }

    public static List<File> decodeFileList(String string) {
        String[] paths = string.split(";");
        List<File> files = new ArrayList<>(paths.length);
        for (String path : paths) {
            path = path.strip();
            if (!path.isEmpty()) {
                files.add(new File(path));
            }
        }
        return files;
    }

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

    // This determines where the values are actually stored.  Actual implementation
    // is platform-dependent.  For Windows, they are stored in Registry.  To see,
    // run regedit and browse to: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\mars
    private Preferences preferences;

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
    /**
     * Flag to control whether the assembler outputs a warning whenever it assembles a program which uses features
     * exclusive to MARS Red, and would not assemble properly in MARS 4.5. This is useful to students, for example,
     * whose assignments may be assembled in MARS 4.5 for grading.
     */
    public final BooleanSetting compatibilityWarningsEnabled = new BooleanSetting(
        this,
        "CompatibilityWarnings",
        true,
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
        this.compatibilityWarningsEnabled,
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
     * Maximum number of recent files to be kept in the "Recent Files" menu.
     */
    public final IntegerSetting maxRecentFiles = new IntegerSetting(
        this,
        "MaxRecentFiles",
        10,
        false
    );
    /**
     * Maximum number of lines that can be kept in a console at once.
     */
    public final IntegerSetting consoleMaxLines = new IntegerSetting(
        this,
        "ConsoleMaxLines",
        1000,
        false
    );
    /**
     * Number of extra lines to be trimmed when the number of lines in a console exceeds the maximum.
     * This allows the console to be trimmed less frequently for performance reasons.
     */
    public final IntegerSetting consoleTrimLines = new IntegerSetting(
        this,
        "ConsoleTrimLines",
        20,
        false
    );
    /**
     * Maximum number of errors that a single assembler run can produce.
     */
    public final IntegerSetting assemblerMaxErrors = new IntegerSetting(
        this,
        "AssemblerMaxErrors",
        200,
        false
    );
    /**
     * Maximum number of "backstep" operations that can be taken. An instruction
     * may produce more than one (e.g. <code>trap</code> instruction may set several registers).
     */
    public final IntegerSetting maxBacksteps = new IntegerSetting(
        this,
        "MaxBacksteps",
        2000,
        false
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
     * Number of letters to be matched by editor's instruction guide before popup generated (if popup enabled).
     */
    public final IntegerSetting editorPopupPrefixLength = new IntegerSetting(
        this,
        "EditorPopupPrefixLength",
        1,
        false
    );

    public final IntegerSetting bitmapDisplayUnitWidth = new IntegerSetting(
        this,
        "BitmapDisplayUnitWidth",
        3,
        false
    );

    public final IntegerSetting bitmapDisplayUnitHeight = new IntegerSetting(
        this,
        "BitmapDisplayUnitHeight",
        3,
        false
    );

    public final IntegerSetting bitmapDisplayWidth = new IntegerSetting(
        this,
        "BitmapDisplayWidth",
        3,
        false
    );

    public final IntegerSetting bitmapDisplayHeight = new IntegerSetting(
        this,
        "BitmapDisplayHeight",
        3,
        false
    );

    public final IntegerSetting bitmapDisplayBaseAddress = new IntegerSetting(
        this,
        "BitmapDisplayBaseAddress",
        2,
        false
    );

    public final IntegerSetting[] integerSettings = {
        this.uiScale,
        this.maxRecentFiles,
        this.consoleMaxLines,
        this.consoleTrimLines,
        this.assemblerMaxErrors,
        this.maxBacksteps,
        this.symbolTableSortState,
        this.caretBlinkRate,
        this.editorTabSize,
        this.editorPopupPrefixLength,
        this.bitmapDisplayUnitWidth,
        this.bitmapDisplayUnitHeight,
        this.bitmapDisplayWidth,
        this.bitmapDisplayHeight,
        this.bitmapDisplayBaseAddress
    };

    // STRING SETTINGS

    /**
     * Acceptable file extensions for MIPS assembly files.
     * Stored as the extensions (e.g. <code>asm</code>) separated by semicolons.
     */
    public final StringSetting mipsFileExtensions = new StringSetting(
        this,
        "MIPSFileExtensions",
        "asm;s",
        false
    );
    /**
     * The special label treated as the entry point for a program.
     */
    public final StringSetting entryPointLabel = new StringSetting(
        this,
        "EntryPointLabel",
        "main",
        false
    );
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
     * The list of files which have been opened recently, ordered from most to least recent.
     * Stored as the full file paths separated by semicolons.
     */
    public final StringSetting recentFiles = new StringSetting(
        this,
        "RecentFiles",
        "",
        false
    );
    /**
     * The name of the look and feel to use for the GUI.
     */
    public final StringSetting lookAndFeelName = new StringSetting(
        this,
        "LookAndFeel",
        "FlatLightLaf",
        false
    );

    public final StringSetting[] stringSettings = {
        this.mipsFileExtensions,
        this.entryPointLabel,
        this.exceptionHandlerPath,
        this.memoryConfiguration,
        this.textSegmentColumnOrder,
        this.previouslyOpenFiles,
        this.recentFiles,
        this.lookAndFeelName,
    };

    // COLOR SETTINGS

    /**
     * RGB color for register highlighted foreground
     */
    public final ColorSetting registerHighlightForeground = new ColorSetting(
        this,
        "RegisterHighlightForeground",
        "Venus.RegistersPane.registerHighlight.foreground",
        false
    );
    /**
     * RGB color for register highlighted background
     */
    public final ColorSetting registerHighlightBackground = new ColorSetting(
        this,
        "RegisterHighlightBackground",
        "Venus.RegistersPane.registerHighlight.background",
        false
    );
    /**
     * RGB color for text segment highlighted foreground
     */
    public final ColorSetting textSegmentExecuteHighlightForeground = new ColorSetting(
        this,
        "TextSegmentHighlightForeground",
        "Venus.TextSegmentWindow.executeHighlight.foreground",
        false
    );
    /**
     * RGB color for text segment highlighted background
     */
    public final ColorSetting textSegmentExecuteHighlightBackground = new ColorSetting(
        this,
        "TextSegmentHighlightBackground",
        "Venus.TextSegmentWindow.executeHighlight.background",
        false
    );
    /**
     * RGB color for text segment fetch highlighted foreground
     */
    public final ColorSetting textSegmentFetchHighlightForeground = new ColorSetting(
        this,
        "TextSegmentFetchHighlightForeground",
        "Venus.TextSegmentWindow.fetchHighlight.foreground",
        false
    );
    /**
     * RGB color for text segment fetch highlighted background
     */
    public final ColorSetting textSegmentFetchHighlightBackground = new ColorSetting(
        this,
        "TextSegmentFetchHighlightBackground",
        "Venus.TextSegmentWindow.fetchHighlight.background",
        false
    );
    /**
     * RGB color for data segment highlighted foreground
     */
    public final ColorSetting dataSegmentHighlightForeground = new ColorSetting(
        this,
        "DataSegmentHighlightForeground",
        "Venus.MemoryViewWindow.wordHighlight.foreground",
        false
    );
    /**
     * RGB color for data segment highlighted background
     */
    public final ColorSetting dataSegmentHighlightBackground = new ColorSetting(
        this,
        "DataSegmentHighlightBackground",
        "Venus.MemoryViewWindow.wordHighlight.background",
        false
    );

    public final ColorSetting[] colorSettings = {
        this.registerHighlightForeground,
        this.registerHighlightBackground,
        this.textSegmentExecuteHighlightForeground,
        this.textSegmentExecuteHighlightBackground,
        this.textSegmentFetchHighlightForeground,
        this.textSegmentFetchHighlightBackground,
        this.dataSegmentHighlightForeground,
        this.dataSegmentHighlightBackground,
    };

    // FONT SETTINGS

    /**
     * Font for the text editor.
     */
    public final FontSetting editorFont = new FontSetting(
        this,
        "EditorFont",
        new Font(Font.MONOSPACED, Font.PLAIN, 12),
        false
    );
    /**
     * Font for the various table displays (registers, text segment, memory viewer, symbol table).
     */
    public final FontSetting tableFont = new FontSetting(
        this,
        "TableFont",
        new Font(Font.MONOSPACED, Font.PLAIN, 12),
        false
    );
    /**
     * Font for highlighted text in table displays (overrides {@link #tableFont} in those cells).
     */
    public final FontSetting tableHighlightFont = new FontSetting(
        this,
        "TableHighlightFont",
        new Font(Font.MONOSPACED, Font.BOLD, 12),
        false
    );
    /**
     * Font for the console displays in the Messages and Console tabs.
     */
    public final FontSetting consoleFont = new FontSetting(
        this,
        "ConsoleFont",
        new Font(Font.MONOSPACED, Font.PLAIN, 12),
        false
    );

    public final FontSetting[] fontSettings = {
        this.editorFont,
        this.tableFont,
        this.tableHighlightFont,
        this.consoleFont,
    };

    // SYNTAX STYLE SETTINGS

    public final SyntaxStyleSetting syntaxStyleDefault = new SyntaxStyleSetting(
        this,
        "SyntaxStyleDefault",
        "Venus.SyntaxStyle.default",
        false
    );
    public final SyntaxStyleSetting syntaxStyleComment = new SyntaxStyleSetting(
        this,
        "SyntaxStyleComment",
        "Venus.SyntaxStyle.comment",
        false
    );
    public final SyntaxStyleSetting syntaxStyleInstruction = new SyntaxStyleSetting(
        this,
        "SyntaxStyleInstruction",
        "Venus.SyntaxStyle.instruction",
        false
    );
    public final SyntaxStyleSetting syntaxStyleDirective = new SyntaxStyleSetting(
        this,
        "SyntaxStyleDirective",
        "Venus.SyntaxStyle.directive",
        false
    );
    public final SyntaxStyleSetting syntaxStyleRegister = new SyntaxStyleSetting(
        this,
        "SyntaxStyleRegister",
        "Venus.SyntaxStyle.register",
        false
    );
    public final SyntaxStyleSetting syntaxStyleStringLiteral = new SyntaxStyleSetting(
        this,
        "SyntaxStyleStringLiteral",
        "Venus.SyntaxStyle.stringLiteral",
        false
    );
    public final SyntaxStyleSetting syntaxStyleCharLiteral = new SyntaxStyleSetting(
        this,
        "SyntaxStyleCharLiteral",
        "Venus.SyntaxStyle.charLiteral",
        false
    );
    public final SyntaxStyleSetting syntaxStyleLabel = new SyntaxStyleSetting(
        this,
        "SyntaxStyleLabel",
        "Venus.SyntaxStyle.label",
        false
    );
    public final SyntaxStyleSetting syntaxStyleInvalid = new SyntaxStyleSetting(
        this,
        "SyntaxStyleInvalid",
        "Venus.SyntaxStyle.invalid",
        false
    );
    public final SyntaxStyleSetting syntaxStyleMacroArgument = new SyntaxStyleSetting(
        this,
        "SyntaxStyleMacroArgument",
        "Venus.SyntaxStyle.macroArgument",
        false
    );

    public final SyntaxStyleSetting[] syntaxStyleSettings = this.getStyleSettingsArray();

    private SyntaxStyleSetting[] getStyleSettingsArray() {
        SyntaxStyleSetting[] styles = new SyntaxStyleSetting[Token.ID_COUNT];
        styles[Token.NULL] = this.syntaxStyleDefault;
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
     * Create and initialize an instance of <code>Settings</code> based on configuration defaults and
     * user-defined values from permanent storage.
     */
    public Settings() {
        this.loadValues();
    }

    /**
     * Obtain the user-defined <code>SyntaxStyle</code>s in the form of an array.
     * The indices of elements in the array correspond to constants from the {@link Token} class.
     * A <code>null</code> element indicates that the syntax style defers to the theme default.
     *
     * @return The syntax styles.
     * @see SyntaxStyleSetting#get()
     */
    public SyntaxStyle[] getSyntaxStyles() {
        SyntaxStyle[] styles = new SyntaxStyle[this.syntaxStyleSettings.length];

        for (int index = 0; index < styles.length; index++) {
            SyntaxStyleSetting setting = this.syntaxStyleSettings[index];
            styles[index] = (setting == null) ? null : setting.get();
        }

        return styles;
    }

    /**
     * Obtain the current <code>SyntaxStyle</code>s in the form of an array.
     * The indices of elements in the array correspond to constants from the {@link Token} class.
     * Each element is the user-defined style, if set, or the theme default otherwise.
     * A <code>null</code> element indicates that the theme default does not assign a syntax style to the token,
     * and no user-defined value is set.
     *
     * @return The syntax styles.
     * @see SyntaxStyleSetting#getOrDefault()
     */
    public SyntaxStyle[] getSyntaxStylesOrDefault() {
        SyntaxStyle[] styles = new SyntaxStyle[this.syntaxStyleSettings.length];

        for (int index = 0; index < styles.length; index++) {
            SyntaxStyleSetting setting = this.syntaxStyleSettings[index];
            styles[index] = (setting == null) ? null : setting.getOrDefault();
        }

        return styles;
    }

    /**
     * Obtain the user-defined <code>SyntaxStyle</code> corresponding to a given token type.
     * If the syntax style defers to the theme default, <code>null</code> is returned.
     *
     * @param index The index of the desired syntax style, which should be one of the token type constants
     *              in {@link Token}.
     * @return The syntax style, or <code>null</code> if there is no syntax style assigned to the token type.
     * @see SyntaxStyleSetting#get()
     */
    public SyntaxStyle getSyntaxStyle(int index) {
        if (index < 0 || index >= this.syntaxStyleSettings.length) {
            return null;
        }

        SyntaxStyleSetting setting = this.syntaxStyleSettings[index];
        return (setting == null) ? null : setting.get();
    }

    /**
     * Obtain the current <code>SyntaxStyle</code> corresponding to a given token type. If the user has defined a custom
     * syntax style, it is returned. Otherwise, the theme default syntax style is returned.
     *
     * @param index The index of the desired syntax style, which should be one of the token type constants
     *              in {@link Token}.
     * @return The syntax style, or <code>null</code> if there is no syntax style assigned to the token type.
     * @see SyntaxStyleSetting#getOrDefault()
     */
    public SyntaxStyle getSyntaxStyleOrDefault(int index) {
        if (index < 0 || index >= this.syntaxStyleSettings.length) {
            return null;
        }

        SyntaxStyleSetting setting = this.syntaxStyleSettings[index];
        return (setting == null) ? null : setting.getOrDefault();
    }

    /**
     * Obtain the default <code>SyntaxStyle</code> corresponding to a given token type. This default value
     * derives from theme-specific defaults.
     *
     * @param index The index of the desired syntax style, which should be one of the token type constants
     *              in {@link Token}.
     * @return The syntax style, or <code>null</code> if there is no syntax style assigned to the token type.
     * @see SyntaxStyleSetting#getDefault()
     */
    public SyntaxStyle getDefaultSyntaxStyle(int index) {
        if (index < 0 || index >= this.syntaxStyleSettings.length) {
            return null;
        }

        SyntaxStyleSetting setting = this.syntaxStyleSettings[index];
        return (setting == null) ? null : setting.getDefault();
    }

    /**
     * Assign a <code>SyntaxStyle</code> to a given token type.
     *
     * @param index The index of the desired syntax style, which should be one of the token type constants
     *              in {@link Token}.
     * @param style The syntax style to be associated with the token type, or <code>null</code> to assume the default.
     * @see SyntaxStyleSetting#set(SyntaxStyle)
     */
    public void setSyntaxStyle(int index, SyntaxStyle style) {
        if (index < 0 || index >= this.syntaxStyleSettings.length) {
            return;
        }
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
            if (value != null) {
                this.preferences.put(key, value);
            }
            else {
                this.preferences.remove(key);
            }
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
     * Load application-wide settings. For each setting, the saved user preference is used if it exists.
     * Otherwise, the default value stored in <code>/config/default_settings.properties</code> is used.
     * If the defaults file can't be read, or if the setting is not present in the defaults file,
     * the built-in defaults set by this class are used.
     */
    public void loadValues() {
        this.preferences = Objects.requireNonNull(
            Preferences.userNodeForPackage(Settings.class),
            "failed to access persistent settings storage"
        );

        Properties defaults = new Properties();
        try {
            InputStream input = Settings.class.getResourceAsStream(DEFAULT_SETTINGS_PATH);
            defaults.load(input);
        }
        catch (IOException | NullPointerException exception) {
            // Built-in defaults will be used instead
        }

        // Load boolean settings
        for (BooleanSetting setting : this.booleanSettings) {
            String property = defaults.getProperty(setting.getKey());
            if (property != null) {
                setting.setDefault(Boolean.parseBoolean(property));
                setting.setNonPersistent(setting.getDefault());
            }
            setting.setNonPersistent(this.preferences.getBoolean(setting.getKey(), setting.getDefault()));
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
            setting.setNonPersistent(this.preferences.getInt(setting.getKey(), setting.getDefault()));
        }
        // Load string settings
        for (StringSetting setting : this.stringSettings) {
            String property = defaults.getProperty(setting.getKey());
            if (property != null) {
                setting.setDefault(property);
                setting.setNonPersistent(setting.getDefault());
            }
            setting.setNonPersistent(this.preferences.get(setting.getKey(), setting.getDefault()));
        }
        // Load color settings
        for (ColorSetting setting : this.colorSettings) {
            // Default value comes from theme-specific UI values, so don't bother checking for a default here
            Color color = ColorSetting.decode(this.preferences.get(setting.getKey(), null));
            if (color != null) {
                setting.setNonPersistent(color);
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
                setting.setNonPersistent(FontSetting.decode(fontString));
            }
        }
        // Load syntax style settings
        for (SyntaxStyleSetting setting : this.syntaxStyleSettings) {
            if (setting != null) {
                // Default value comes from theme-specific UI values, so don't bother checking for a default here
                SyntaxStyle style = SyntaxStyleSetting.decode(this.preferences.get(setting.getKey(), null));
                if (style != null) {
                    setting.setNonPersistent(style);
                }
            }
        }
    }

    /**
     * Update any default values that are derived from the UI theme. This should be called anytime the theme changes,
     * and exists to reduce the overhead it would induce to constantly request the same theme default when it
     * hasn't changed.
     */
    public void updateThemeDefaults() {
        for (ColorSetting setting : this.colorSettings) {
            setting.updateDefault();
        }
    }
}