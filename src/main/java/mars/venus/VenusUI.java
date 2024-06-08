package mars.venus;

import mars.Application;
import mars.settings.Settings;
import mars.simulator.*;
import mars.util.SVGIcon;
import mars.venus.actions.ToolAction;
import mars.venus.actions.VenusAction;
import mars.venus.actions.edit.*;
import mars.venus.actions.file.*;
import mars.venus.actions.help.*;
import mars.venus.actions.run.*;
import mars.venus.actions.settings.*;
import mars.venus.editor.Editor;
import mars.venus.editor.FileStatus;
import mars.venus.execute.ProgramStatus;
import mars.venus.execute.RunSpeedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
 * Top level container for the Venus GUI.
 *
 * @author Sanderson and Team JSpim
 */
/*
 * Heavily modified by Pete Sanderson, July 2004, to incorporate JSPIMMenu and JSPIMToolbar
 * not as subclasses of JMenuBar and JToolBar, but as instances of them.  They are both
 * here primarily so both can share the Action objects.
 */
public class VenusUI extends JFrame implements SimulatorListener {
    /**
     * Width and height in user pixels of icons in the tool bar and menus.
     */
    // TODO: Make this configurable
    public static final int ICON_SIZE = 24;
    /**
     * Time in milliseconds to show splash screen.
     */
    public static final int SPLASH_DURATION_MILLIS = 5000;
    /**
     * Filename of the icon used in the window title bar and taskbar.
     */
    public static final String WINDOW_ICON_NAME = "MarsPlanet.jpg";

    private final String baseTitle;
    private final Settings settings;
    private final MainPane mainPane;
    private final RegistersPane registersPane;
    private final MessagesPane messagesPane;
    private final Editor editor;

    private FileStatus fileStatus;
    private ProgramStatus programStatus;

    // The "action" objects, which include action listeners.  One of each will be created then
    // shared between a menu item and its corresponding toolbar button.  This is a very cool
    // technique because it relates the button and menu item so closely.

    private final List<VenusAction> actions;

    private FileNewAction fileNewAction;
    private FileOpenAction fileOpenAction;
    private FileCloseAction fileCloseAction;
    private FileCloseAllAction fileCloseAllAction;
    private FileSaveAction fileSaveAction;
    private FileSaveAsAction fileSaveAsAction;
    private FileSaveAllAction fileSaveAllAction;
    private FileDumpMemoryAction fileDumpMemoryAction;
    private FilePrintAction filePrintAction;
    private FileExitAction fileExitAction;

    private EditUndoAction editUndoAction;
    private EditRedoAction editRedoAction;
    private EditCutAction editCutAction;
    private EditCopyAction editCopyAction;
    private EditPasteAction editPasteAction;
    private EditFindReplaceAction editFindReplaceAction;
    private EditSelectAllAction editSelectAllAction;
    private EditCommentAction editCommentAction;

    private RunAssembleAction runAssembleAction;
    private RunStartAction runStartAction;
    private RunStepForwardAction runStepForwardAction;
    private RunStepBackwardAction runStepBackwardAction;
    private RunPauseAction runPauseAction;
    private RunStopAction runStopAction;
    private RunResetAction runResetAction;
    private RunClearBreakpointsAction runClearBreakpointsAction;
    private RunToggleBreakpointsAction runToggleBreakpointsAction;

    private SettingsLabelAction settingsLabelAction;
    private SettingsPopupInputAction settingsPopupInputAction;
    private SettingsValueDisplayBaseAction settingsValueDisplayBaseAction;
    private SettingsAddressDisplayBaseAction settingsAddressDisplayBaseAction;
    private SettingsExtendedAction settingsExtendedAction;
    private SettingsAssembleOnOpenAction settingsAssembleOnOpenAction;
    private SettingsAssembleAllAction settingsAssembleAllAction;
    private SettingsWarningsAreErrorsAction settingsWarningsAreErrorsAction;
    private SettingsStartAtMainAction settingsStartAtMainAction;
    private SettingsProgramArgumentsAction settingsProgramArgumentsAction;
    private SettingsDelayedBranchingAction settingsDelayedBranchingAction;
    private SettingsExceptionHandlerAction settingsExceptionHandlerAction;
    private SettingsDarkThemeAction settingsDarkThemeAction;
    private SettingsEditorAction settingsEditorAction;
    private SettingsHighlightingAction settingsHighlightingAction;
    private SettingsMemoryConfigurationAction settingsMemoryConfigurationAction;
    private SettingsSelfModifyingCodeAction settingsSelfModifyingCodeAction;
    private SettingsEndiannessAction settingsEndiannessAction;
    private SettingsPreferencesAction settingsPreferencesAction;

    private HelpHelpAction helpHelpAction;
    private HelpUpdateAction helpUpdateAction;
    private HelpAboutAction helpAboutAction;

    /**
     * Create a new instance of the Venus GUI and show it once it is created.
     *
     * @param title Name of the window to be created.
     */
    public VenusUI(Settings settings, String title) {
        super(title);
        Application.setGUI(this);

        this.fileStatus = FileStatus.NO_FILE;
        this.programStatus = ProgramStatus.NOT_ASSEMBLED;

        // Launch splash screen, which will show this window after it finishes
        SplashScreen splashScreen = new SplashScreen(this, SPLASH_DURATION_MILLIS);
        splashScreen.showSplash();

        this.settings = settings;
        this.baseTitle = title;
        this.actions = new ArrayList<>();
        this.editor = new Editor(this);

        double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        // basically give up some screen space if running at 800 x 600
        double messageWidthPct = (screenWidth < 1000.0) ? 0.67 : 0.73;
        double messageHeightPct = (screenWidth < 1000.0) ? 0.12 : 0.15;
        double mainWidthPct = (screenWidth < 1000.0) ? 0.67 : 0.73;
        double mainHeightPct = (screenWidth < 1000.0) ? 0.60 : 0.65;
        double registersWidthPct = (screenWidth < 1000.0) ? 0.18 : 0.22;
        double registersHeightPct = (screenWidth < 1000.0) ? 0.72 : 0.80;

        Dimension messagesPanePreferredSize = new Dimension((int) (screenWidth * messageWidthPct), (int) (screenHeight * messageHeightPct));
        Dimension mainPanePreferredSize = new Dimension((int) (screenWidth * mainWidthPct), (int) (screenHeight * mainHeightPct));
        Dimension registersPanePreferredSize = new Dimension((int) (screenWidth * registersWidthPct), (int) (screenHeight * registersHeightPct));

        Application.initialize();

        // Image courtesy of NASA/JPL
        URL iconImageURL = this.getClass().getResource(Application.IMAGES_PATH + WINDOW_ICON_NAME);
        if (iconImageURL == null) {
            System.err.println("Error: images folder or file not found");
        }
        else {
            Image iconImage = Toolkit.getDefaultToolkit().getImage(iconImageURL);
            this.setIconImage(iconImage);

            // MacOS requires a different method to set the taskbar icon
            try {
                Taskbar.getTaskbar().setIconImage(iconImage);
            }
            catch (UnsupportedOperationException exception) {
                // The OS doesn't support setting the icon through this method
            }
            catch (SecurityException exception) {
                System.err.println("Error: unable to set taskbar icon image: no permission");
            }
        }

        // Everything in frame will be arranged on JPanel "center", which is only frame component.
        // "center" has BorderLayout and 2 major components:
        //   -- panel (jp) on North with 2 components
        //      1. toolbar
        //      2. run speed slider.
        //   -- split pane (horizonSplitter) in center with 2 components side-by-side
        //      1. split pane (splitter) with 2 components stacked
        //         a. main pane, with 2 tabs (edit, execute)
        //         b. messages pane with 2 tabs (mars, run I/O)
        //      2. registers pane with 3 tabs (register file, coprocessor 0, coprocessor 1)
        // I should probably run this breakdown out to full detail.  The components are created
        // roughly in bottom-up order; some are created in component constructors and thus are
        // not visible here.

        this.registersPane = new RegistersPane(this);
        this.registersPane.setPreferredSize(registersPanePreferredSize);

        this.mainPane = new MainPane(this, this.editor);
        this.mainPane.setPreferredSize(mainPanePreferredSize);

        this.messagesPane = new MessagesPane();
        this.messagesPane.setPreferredSize(messagesPanePreferredSize);

        JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.mainPane, this.messagesPane);
        splitter.setOneTouchExpandable(true);
        splitter.resetToPreferredSizes();
        JSplitPane horizonSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitter, this.registersPane);
        horizonSplitter.setOneTouchExpandable(true);
        horizonSplitter.resetToPreferredSizes();

        // Due to dependencies, do not set up menu/toolbar until now.
        this.createActionObjects();
        this.createMenuBar();

        JToolBar toolbar = this.setupToolBar();

        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(toolbar);
        jp.add(RunSpeedPanel.getInstance());
        JPanel center = new JPanel(new BorderLayout());
        center.add(jp, BorderLayout.NORTH);
        center.add(horizonSplitter);

        this.getContentPane().add(center);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                // Maximize the application window
                VenusUI.this.setExtendedState(JFrame.MAXIMIZED_BOTH);
                splashScreen.toFront();
            }

            @Override
            public void windowClosing(WindowEvent event) {
                // Don't save the workspace state after closing all files, unless the exit fails
                VenusUI.this.mainPane.getEditTab().setWorkspaceStateSavingEnabled(false);

                // Check for unsaved changes before closing the application
                if (VenusUI.this.editor.closeAll()) {
                    System.exit(0);
                }
                else {
                    VenusUI.this.mainPane.getEditTab().setWorkspaceStateSavingEnabled(true);
                }
            }
        });

        // The following will handle the windowClosing event properly in the
        // situation where user Cancels out of "save edits?" dialog.  By default,
        // the GUI frame will be hidden but I want it to do nothing.
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Simulator.getInstance().addGUIListener(this);

        // Update all actions
        for (VenusAction action : this.actions) {
            action.update();
        }

        // Restore previous session
        this.getMainPane().getEditTab().loadWorkspaceState();
    }

    /**
     * Called by {@link SplashScreen} once it has displayed for the required duration,
     * making this window visible.
     */
    public void showWindow() {
        this.pack();
        this.setVisible(true);
    }

    /**
     * Load an SVG action icon from the action icons folder ({@link Application#ACTION_ICONS_PATH}).
     *
     * @param filename Name of the SVG icon to load.
     * @return The loaded SVG icon, or null if the image does not exist.
     */
    public static Icon getSVGActionIcon(String filename) {
        URL url = VenusUI.class.getResource(Application.ACTION_ICONS_PATH + filename);

        if (url != null) {
            return new SVGIcon(url, ICON_SIZE, ICON_SIZE);
        }
        else {
            System.err.println("Error: unable to load image \"" + Application.ACTION_ICONS_PATH + filename + "\"");
            return null;
        }
    }

    /**
     * Action objects are used instead of action listeners because one can be easily shared between
     * a menu item and a toolbar button.  Does nice things like disable both if the action is
     * disabled, etc.
     */
    private void createActionObjects() {
        int menuShortcutMask = this.getToolkit().getMenuShortcutKeyMaskEx();

        this.actions.add(this.fileNewAction = new FileNewAction(this, "New", getSVGActionIcon("new.svg"), "Create a new file for editing", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutMask)));
        this.actions.add(this.fileOpenAction = new FileOpenAction(this, "Open...", getSVGActionIcon("open.svg"), "Open a file for editing", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutMask)));
        this.actions.add(this.fileCloseAction = new FileCloseAction(this, "Close", getSVGActionIcon("close.svg"), "Close the current file", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_W, menuShortcutMask)));
        this.actions.add(this.fileCloseAllAction = new FileCloseAllAction(this, "Close All", getSVGActionIcon("close_all.svg"), "Close all open files", KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_W, menuShortcutMask | KeyEvent.SHIFT_DOWN_MASK)));
        this.actions.add(this.fileSaveAction = new FileSaveAction(this, "Save", getSVGActionIcon("save.svg"), "Save the current file", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutMask)));
        this.actions.add(this.fileSaveAsAction = new FileSaveAsAction(this, "Save As...", getSVGActionIcon("save_as.svg"), "Save current file with different name", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutMask | KeyEvent.SHIFT_DOWN_MASK)));
        this.actions.add(this.fileSaveAllAction = new FileSaveAllAction(this, "Save All", getSVGActionIcon("save_all.svg"), "Save all open files", KeyEvent.VK_V, null));
        this.actions.add(this.fileDumpMemoryAction = new FileDumpMemoryAction(this, "Dump Memory...", getSVGActionIcon("dump_memory.svg"), "Dump machine code or data in an available format", KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, menuShortcutMask)));
        this.actions.add(this.filePrintAction = new FilePrintAction(this, "Print...", getSVGActionIcon("print.svg"), "Print current file", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_P, menuShortcutMask)));
        this.actions.add(this.fileExitAction = new FileExitAction(this, "Exit", getSVGActionIcon("exit.svg"), "Exit " + Application.NAME, KeyEvent.VK_X, null));

        this.actions.add(this.editUndoAction = new EditUndoAction(this, "Undo", getSVGActionIcon("undo.svg"), "Undo last edit", KeyEvent.VK_U, KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuShortcutMask)));
        this.actions.add(this.editRedoAction = new EditRedoAction(this, "Redo", getSVGActionIcon("redo.svg"), "Redo last edit", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuShortcutMask)));
        this.actions.add(this.editCutAction = new EditCutAction(this, "Cut", getSVGActionIcon("cut.svg"), "Cut", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutMask)));
        this.actions.add(this.editCopyAction = new EditCopyAction(this, "Copy", getSVGActionIcon("copy.svg"), "Copy", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutMask)));
        this.actions.add(this.editPasteAction = new EditPasteAction(this, "Paste", getSVGActionIcon("paste.svg"), "Paste", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutMask)));
        this.actions.add(this.editFindReplaceAction = new EditFindReplaceAction(this, "Find / Replace...", getSVGActionIcon("find.svg"), "Find and/or replace text in the current file", KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F, menuShortcutMask)));
        this.actions.add(this.editSelectAllAction = new EditSelectAllAction(this, "Select All", getSVGActionIcon("select_all.svg"), "Select all text in the current file", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutMask)));
        this.actions.add(this.editCommentAction = new EditCommentAction(this, "Comment Selected Lines", getSVGActionIcon("comment.svg"), "Toggle whether the currently selected lines are commented out", KeyEvent.VK_SLASH, KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, menuShortcutMask)));

        this.actions.add(this.runAssembleAction = new RunAssembleAction(this, "Assemble", getSVGActionIcon("assemble.svg"), "Assemble the current file and clear breakpoints", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)));
        this.actions.add(this.runStartAction = new RunStartAction(this, "Start", getSVGActionIcon("start.svg"), "Run the current program", KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
        this.actions.add(this.runStopAction = new RunStopAction(this, "Stop", getSVGActionIcon("stop.svg"), "Stop the currently running program", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
        this.actions.add(this.runPauseAction = new RunPauseAction(this, "Pause", getSVGActionIcon("pause.svg"), "Pause the currently running program", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
        this.actions.add(this.runStepForwardAction = new RunStepForwardAction(this, "Step Forward", getSVGActionIcon("step_forward.svg"), "Execute the next instruction", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
        this.actions.add(this.runStepBackwardAction = new RunStepBackwardAction(this, "Step Backward", getSVGActionIcon("step_backward.svg"), "Undo the last step", KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
        this.actions.add(this.runResetAction = new RunResetAction(this, "Reset", getSVGActionIcon("reset.svg"), "Reset MIPS memory and registers", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0)));
        this.actions.add(this.runClearBreakpointsAction = new RunClearBreakpointsAction(this, "Clear All Breakpoints", null, "Clear all execution breakpoints set since the last assemble.", KeyEvent.VK_K, KeyStroke.getKeyStroke(KeyEvent.VK_K, menuShortcutMask)));
        this.actions.add(this.runToggleBreakpointsAction = new RunToggleBreakpointsAction(this, "Toggle All Breakpoints", null, "Disable/enable all breakpoints without clearing (can also click Bkpt column header)", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_T, menuShortcutMask)));

        this.actions.add(this.settingsLabelAction = new SettingsLabelAction(this, "Show symbol table", null, "Toggle visibility of Labels window (symbol table) in the Execute tab", null, null));
        this.actions.add(this.settingsPopupInputAction = new SettingsPopupInputAction(this, "Use dialog for user input", null, "If set, use popup dialog for input syscalls (5, 6, 7, 8, 12) instead of console input", null, null));
        this.actions.add(this.settingsValueDisplayBaseAction = new SettingsValueDisplayBaseAction(this, "Hexadecimal values", null, "Toggle between hexadecimal and decimal display of memory/register values", null, null));
        this.actions.add(this.settingsAddressDisplayBaseAction = new SettingsAddressDisplayBaseAction(this, "Hexadecimal addresses", null, "Toggle between hexadecimal and decimal display of memory addresses", null, null));
        this.actions.add(this.settingsExtendedAction = new SettingsExtendedAction(this, "Allow extended (pseudo) instructions", null, "If set, MIPS extended (pseudo) instructions are formats are permitted.", null, null));
        this.actions.add(this.settingsAssembleOnOpenAction = new SettingsAssembleOnOpenAction(this, "Assemble files when opened", null, "If set, a file will be automatically assembled as soon as it is opened.  File Open dialog will show most recently opened file.", null, null));
        this.actions.add(this.settingsAssembleAllAction = new SettingsAssembleAllAction(this, "Assemble all files in current directory", null, "If set, all files in current directory will be assembled when Assemble operation is selected.", null, null));
        this.actions.add(this.settingsWarningsAreErrorsAction = new SettingsWarningsAreErrorsAction(this, "Promote assembler warnings to errors", null, "If set, assembler warnings will be interpreted as errors and prevent successful assembly.", null, null));
        this.actions.add(this.settingsStartAtMainAction = new SettingsStartAtMainAction(this, "Use \"main\" as program entry point", null, "If set, assembler will initialize Program Counter to text address globally labeled 'main', if defined.", null, null));
        this.actions.add(this.settingsProgramArgumentsAction = new SettingsProgramArgumentsAction(this, "Allow program arguments", null, "If set, program arguments for MIPS program can be entered in border of Text Segment window.", null, null));
        this.actions.add(this.settingsDelayedBranchingAction = new SettingsDelayedBranchingAction(this, "Delayed branching", null, "If set, delayed branching will occur during MIPS execution.", null, null));
        this.actions.add(this.settingsSelfModifyingCodeAction = new SettingsSelfModifyingCodeAction(this, "Self-modifying code", null, "If set, the MIPS program can write and branch to both text and data segments.", null, null));
        this.actions.add(this.settingsEndiannessAction = new SettingsEndiannessAction(this, "Use big-endian byte ordering", null, "If set, the bytes in a word will be ordered from most to least significant.", null, null));
        this.actions.add(this.settingsDarkThemeAction = new SettingsDarkThemeAction(this, "Enable dark theme", null, "If set, the application will launch with a dark theme.", null, null));
        this.actions.add(this.settingsEditorAction = new SettingsEditorAction(this, "Editor Settings...", null, "View and modify text editor settings", null, null));
        this.actions.add(this.settingsHighlightingAction = new SettingsHighlightingAction(this, "Highlighting...", null, "View and modify Execute tab highlighting colors", null, null));
        this.actions.add(this.settingsExceptionHandlerAction = new SettingsExceptionHandlerAction(this, "Exception Handler...", null, "If set, the specified exception handler file will be included in all Assemble operations.", null, null));
        this.actions.add(this.settingsMemoryConfigurationAction = new SettingsMemoryConfigurationAction(this, "Memory Configuration...", null, "View and modify memory segment base addresses for simulated MIPS", null, null));
        this.actions.add(this.settingsPreferencesAction = new SettingsPreferencesAction(this, "Preferences...", getSVGActionIcon("preferences.svg"), "Edit the appearance and behavior of the application", null, null));

        this.actions.add(this.helpHelpAction = new HelpHelpAction(this, "Help...", getSVGActionIcon("help.svg"), "View help information", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_H, menuShortcutMask)));
        this.actions.add(this.helpUpdateAction = new HelpUpdateAction(this, "Check for Updates...", getSVGActionIcon("update.svg"), "Check if a newer version is available", null, null));
        this.actions.add(this.helpAboutAction = new HelpAboutAction(this, "About...", getSVGActionIcon("about.svg"), "Information about " + Application.NAME, null, null));
    }

    /**
     * Build the menus and connect them to action objects (which serve as action listeners
     * shared between menu item and corresponding toolbar icon).
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(this.createMenuItem(this.fileNewAction));
        fileMenu.add(this.createMenuItem(this.fileOpenAction));
        fileMenu.add(this.createMenuItem(this.fileCloseAction));
        fileMenu.add(this.createMenuItem(this.fileCloseAllAction));
        fileMenu.addSeparator();
        fileMenu.add(this.createMenuItem(this.fileSaveAction));
        fileMenu.add(this.createMenuItem(this.fileSaveAsAction));
        fileMenu.add(this.createMenuItem(this.fileSaveAllAction));
        fileMenu.addSeparator();
        fileMenu.add(this.createMenuItem(this.fileDumpMemoryAction));
        fileMenu.add(this.createMenuItem(this.filePrintAction));
        fileMenu.addSeparator();
        fileMenu.add(this.createMenuItem(this.fileExitAction));
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(this.createMenuItem(this.editUndoAction));
        editMenu.add(this.createMenuItem(this.editRedoAction));
        editMenu.addSeparator();
        editMenu.add(this.createMenuItem(this.editCutAction));
        editMenu.add(this.createMenuItem(this.editCopyAction));
        editMenu.add(this.createMenuItem(this.editPasteAction));
        editMenu.addSeparator();
        editMenu.add(this.createMenuItem(this.editFindReplaceAction));
        editMenu.addSeparator();
        editMenu.add(this.createMenuItem(this.editSelectAllAction));
        editMenu.add(this.createMenuItem(this.editCommentAction));
        menuBar.add(editMenu);

        JMenu runMenu = new JMenu("Run");
        runMenu.setMnemonic(KeyEvent.VK_R);
        runMenu.add(this.createMenuItem(this.runAssembleAction));
        runMenu.add(this.createMenuItem(this.runStartAction));
        runMenu.add(this.createMenuItem(this.runStopAction));
        runMenu.add(this.createMenuItem(this.runPauseAction));
        runMenu.add(this.createMenuItem(this.runStepForwardAction));
        runMenu.add(this.createMenuItem(this.runStepBackwardAction));
        runMenu.add(this.createMenuItem(this.runResetAction));
        runMenu.addSeparator();
        runMenu.add(this.createMenuItem(this.runClearBreakpointsAction));
        runMenu.add(this.createMenuItem(this.runToggleBreakpointsAction));
        menuBar.add(runMenu);

        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic(KeyEvent.VK_S);
        settingsMenu.add(this.createMenuCheckBox(this.settingsLabelAction, this.settings.labelWindowVisible.get()));
        settingsMenu.add(this.createMenuBaseChooser(this.settingsAddressDisplayBaseAction, this.settings.displayAddressesInHex.get(), this.mainPane.getExecuteTab().getAddressDisplayBaseChooser()));
        settingsMenu.add(this.createMenuBaseChooser(this.settingsValueDisplayBaseAction, this.settings.displayValuesInHex.get(), this.mainPane.getExecuteTab().getValueDisplayBaseChooser()));
        settingsMenu.addSeparator();
        settingsMenu.add(this.createMenuCheckBox(this.settingsAssembleOnOpenAction, this.settings.assembleOnOpenEnabled.get()));
        settingsMenu.add(this.createMenuCheckBox(this.settingsAssembleAllAction, this.settings.assembleAllEnabled.get()));
        settingsMenu.add(this.createMenuCheckBox(this.settingsWarningsAreErrorsAction, this.settings.warningsAreErrors.get()));
        settingsMenu.add(this.createMenuCheckBox(this.settingsStartAtMainAction, this.settings.startAtMain.get()));
        settingsMenu.add(this.createMenuCheckBox(this.settingsExtendedAction, this.settings.extendedAssemblerEnabled.get()));
        settingsMenu.addSeparator();
        settingsMenu.add(this.createMenuCheckBox(this.settingsProgramArgumentsAction, this.settings.useProgramArguments.get()));
        settingsMenu.add(this.createMenuCheckBox(this.settingsPopupInputAction, this.settings.popupSyscallInput.get()));
        settingsMenu.add(this.createMenuCheckBox(this.settingsDelayedBranchingAction, this.settings.delayedBranchingEnabled.get()));
        settingsMenu.add(this.createMenuCheckBox(this.settingsSelfModifyingCodeAction, this.settings.selfModifyingCodeEnabled.get()));
        settingsMenu.add(this.createMenuCheckBox(this.settingsEndiannessAction, this.settings.useBigEndian.get()));
        settingsMenu.addSeparator();
        settingsMenu.add(this.createMenuCheckBox(this.settingsDarkThemeAction, this.settings.lookAndFeelName.get().equals("FlatDarkLaf")));
        settingsMenu.addSeparator();
        settingsMenu.add(this.createMenuItem(this.settingsEditorAction));
        settingsMenu.add(this.createMenuItem(this.settingsHighlightingAction));
        settingsMenu.add(this.createMenuItem(this.settingsExceptionHandlerAction));
        settingsMenu.add(this.createMenuItem(this.settingsMemoryConfigurationAction));
        settingsMenu.add(this.createMenuItem(this.settingsPreferencesAction));
        menuBar.add(settingsMenu);

        List<ToolAction> toolActions = ToolManager.getToolActions();
        if (!toolActions.isEmpty()) {
            JMenu toolsMenu = new JMenu("Tools");
            toolsMenu.setMnemonic(KeyEvent.VK_T);
            int previousToolOrder = toolActions.get(0).getTool().getToolMenuOrder();
            for (ToolAction toolAction : toolActions) {
                // Put separators between actions with differing tool orders
                int toolOrder = toolAction.getTool().getToolMenuOrder();
                if (toolOrder != previousToolOrder) {
                    toolsMenu.addSeparator();
                    previousToolOrder = toolOrder;
                }
                toolsMenu.add(this.createMenuItem(toolAction));
            }
            menuBar.add(toolsMenu);
        }

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(this.createMenuItem(this.helpHelpAction));
        helpMenu.add(this.createMenuItem(this.helpUpdateAction));
        helpMenu.add(this.createMenuItem(this.helpAboutAction));
        menuBar.add(helpMenu);

        this.setJMenuBar(menuBar);
    }

    private JMenuItem createMenuItem(Action action) {
        JMenuItem menuItem = new JMenuItem(action);
        menuItem.setDisabledIcon(menuItem.getIcon());
        return menuItem;
    }

    private JCheckBoxMenuItem createMenuCheckBox(Action action, boolean selected) {
        JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(action);
        checkBox.setSelected(selected);
        return checkBox;
    }

    private JCheckBoxMenuItem createMenuBaseChooser(Action action, boolean selected, NumberDisplayBaseChooser chooser) {
        JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(action);
        checkBox.setSelected(selected);
        chooser.setSettingsMenuItem(checkBox);
        return checkBox;
    }

    /**
     * Build the toolbar and connect items to action objects (which serve as action listeners
     * shared between toolbar icon and corresponding menu item).
     */
    // TODO: Make the toolbar setup customizable -Sean Clarke
    private JToolBar setupToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        toolBar.add(this.createToolBarButton(this.fileNewAction));
        toolBar.add(this.createToolBarButton(this.fileOpenAction));
        toolBar.add(this.createToolBarButton(this.fileSaveAction));
        toolBar.add(this.createToolBarButton(this.fileSaveAllAction));
        toolBar.addSeparator();
        toolBar.add(this.createToolBarButton(this.editUndoAction));
        toolBar.add(this.createToolBarButton(this.editRedoAction));
        toolBar.add(this.createToolBarButton(this.editCutAction));
        toolBar.add(this.createToolBarButton(this.editCopyAction));
        toolBar.add(this.createToolBarButton(this.editPasteAction));
        toolBar.add(this.createToolBarButton(this.editFindReplaceAction));
        toolBar.addSeparator();
        toolBar.add(this.createToolBarButton(this.runAssembleAction));
        toolBar.add(this.createToolBarButton(this.runStartAction));
        toolBar.add(this.createToolBarButton(this.runStopAction));
        toolBar.add(this.createToolBarButton(this.runPauseAction));
        toolBar.add(this.createToolBarButton(this.runStepForwardAction));
        toolBar.add(this.createToolBarButton(this.runStepBackwardAction));
        toolBar.add(this.createToolBarButton(this.runResetAction));
        toolBar.addSeparator();
        toolBar.add(this.createToolBarButton(this.helpHelpAction));

        return toolBar;
    }

    private JButton createToolBarButton(Action action) {
        JButton button = new JButton(action);
        button.setHideActionText(button.getIcon() != null);
        return button;
    }

    @Override
    public void simulatorStarted(SimulatorStartEvent event) {
        this.setProgramStatus(ProgramStatus.RUNNING);
    }

    @Override
    public void simulatorPaused(SimulatorPauseEvent event) {
        this.setProgramStatus(ProgramStatus.PAUSED);
    }

    @Override
    public void simulatorFinished(SimulatorFinishEvent event) {
        this.setProgramStatus(ProgramStatus.TERMINATED);
    }

    /**
     * Automatically update whether the Undo and Redo actions are enabled or disabled
     * based on the status of the {@link javax.swing.undo.UndoManager}.
     */
    public void updateUndoRedoActions() {
        this.editUndoAction.update();
        this.editRedoAction.update();
    }

    /**
     * Set the content to display as part of the window title, if any. (Usually, this is a filename.)
     * The resulting full title will look like "(content) - (base title)" if the content is not null,
     * or simply the base title otherwise.
     *
     * @param content The content to display before the base title.
     */
    public void setTitleContent(String content) {
        if (content != null) {
            this.setTitle(content + " - " + this.baseTitle);
        }
        else {
            this.setTitle(this.baseTitle);
        }
    }

    /**
     * Get the current status of the file being edited in the Edit tab (or {@link FileStatus#NO_FILE} if no files
     * are being edited).
     *
     * @return The current file status.
     */
    public FileStatus getFileStatus() {
        return this.fileStatus;
    }

    /**
     * Set the status of the file being edited, and update the menu state.
     *
     * @param status The new file status.
     */
    public void setFileStatus(FileStatus status) {
        this.fileStatus = status;
        for (VenusAction action : this.actions) {
            action.update();
        }
    }

    /**
     * Get the current status of the overall program as it relates to the Execute tab.
     *
     * @return The current program status.
     */
    public ProgramStatus getProgramStatus() {
        return this.programStatus;
    }

    /**
     * Set the status of the overall program, and update the menu state.
     *
     * @param status The new program status.
     */
    public void setProgramStatus(ProgramStatus status) {
        this.programStatus = status;
        for (VenusAction action : this.actions) {
            action.update();
        }
    }

    public Settings getSettings() {
        return this.settings;
    }

    /**
     * Get a reference to the text editor associated with this GUI.
     *
     * @return Editor for the GUI.
     */
    public Editor getEditor() {
        return this.editor;
    }

    /**
     * Get a reference to the main pane associated with this GUI.
     *
     * @return MainPane for the GUI.
     */
    public MainPane getMainPane() {
        return this.mainPane;
    }

    /**
     * Get a reference to the messages pane associated with this GUI.
     *
     * @return MessagesPane for the GUI.
     */
    public MessagesPane getMessagesPane() {
        return this.messagesPane;
    }

    /**
     * Get reference to registers pane associated with this GUI.
     *
     * @return RegistersPane for the GUI.
     */
    public RegistersPane getRegistersPane() {
        return this.registersPane;
    }

    /**
     * Get a reference to the Run->Assemble action.  Needed by File->Open in case the
     * assemble-upon-open flag is set.
     *
     * @return The object for the Run->Assemble operation.
     */
    public RunAssembleAction getRunAssembleAction() {
        return this.runAssembleAction;
    }
}