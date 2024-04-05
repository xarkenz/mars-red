package mars.venus;

import mars.Globals;
import mars.venus.actions.edit.*;
import mars.venus.actions.file.*;
import mars.venus.actions.help.*;
import mars.venus.actions.run.*;
import mars.venus.actions.settings.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

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
 * Top level container for Venus GUI.
 *
 * @author Sanderson and Team JSpim
 */
/*
 * Heavily modified by Pete Sanderson, July 2004, to incorporate JSPIMMenu and JSPIMToolbar
 * not as subclasses of JMenuBar and JToolBar, but as instances of them.  They are both
 * here primarily so both can share the Action objects.
 */
public class VenusUI extends JFrame {
    private final JMenuBar menu;
    private final MainPane mainPane;
    private final RegistersPane registersPane;
    private final MessagesPane messagesPane;
    private final Editor editor;

    private final String baseTitle;
    private int menuState = FileStatus.NO_FILE;

    // PLEASE PUT THESE TWO (& THEIR METHODS) SOMEWHERE THEY BELONG, NOT HERE
    private static boolean reset = true; // registers/memory reset for execution
    private static boolean started = false; // started execution

    // Components of the menubar
    private JMenu file, run, help, edit, settings;
    private JMenuItem fileNew, fileOpen, fileClose, fileCloseAll, fileSave, fileSaveAs, fileSaveAll, fileDumpMemory, filePrint, fileExit;
    private JMenuItem editUndo, editRedo, editCut, editCopy, editPaste, editFindReplace, editSelectAll;
    private JMenuItem runGo, runStep, runBackstep, runReset, runAssemble, runStop, runPause, runClearBreakpoints, runToggleBreakpoints;
    private JCheckBoxMenuItem settingsLabel, settingsPopupInput, settingsValueDisplayBase, settingsAddressDisplayBase, settingsExtended, settingsAssembleOnOpen, settingsAssembleAll, settingsWarningsAreErrors, settingsStartAtMain, settingsDelayedBranching, settingsProgramArguments, settingsSelfModifyingCode;
    private JMenuItem settingsExceptionHandler, settingsEditor, settingsHighlighting, settingsMemoryConfiguration;
    private JMenuItem helpHelp, helpAbout;

    // The "action" objects, which include action listeners.  One of each will be created then
    // shared between a menu item and its corresponding toolbar button.  This is a very cool
    // technique because it relates the button and menu item so closely
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
    private RunAssembleAction runAssembleAction;
    private RunStartAction runStartAction;
    private RunStepAction runStepAction;
    private RunBackstepAction runBackstepAction;
    private RunResetAction runResetAction;
    private RunStopAction runStopAction;
    private RunPauseAction runPauseAction;
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
    private SettingsEditorAction settingsEditorAction;
    private SettingsHighlightingAction settingsHighlightingAction;
    private SettingsMemoryConfigurationAction settingsMemoryConfigurationAction;
    private SettingsSelfModifyingCodeAction settingsSelfModifyingCodeAction;
    private HelpHelpAction helpHelpAction;
    private HelpAboutAction helpAboutAction;

    /**
     * Constructor for the Class. Sets up a window object for the UI
     *
     * @param title Name of the window to be created.
     */
    public VenusUI(String title) {
        super(title);
        this.baseTitle = title;
        this.editor = new Editor(this);
        Globals.setGUI(this);

        // TODO: Use this logic whenever window is resized. -Sean Clarke
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

        Globals.initialize();

        // Image courtesy of NASA/JPL.
        URL iconImageURL = this.getClass().getResource(Globals.IMAGES_PATH + "RedMars16.gif");
        if (iconImageURL == null) {
            System.out.println("Internal Error: images folder or file not found");
            System.exit(0);
        }
        Image iconImage = Toolkit.getDefaultToolkit().getImage(iconImageURL);
        this.setIconImage(iconImage);

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

        RegistersWindow registersTab = new RegistersWindow();
        Coprocessor1Window coprocessor1Tab = new Coprocessor1Window();
        Coprocessor0Window coprocessor0Tab = new Coprocessor0Window();
        registersPane = new RegistersPane(this, registersTab, coprocessor1Tab, coprocessor0Tab);
        registersPane.setPreferredSize(registersPanePreferredSize);

        mainPane = new MainPane(this, editor, registersTab, coprocessor1Tab, coprocessor0Tab);
        mainPane.setPreferredSize(mainPanePreferredSize);
        messagesPane = new MessagesPane();
        messagesPane.setPreferredSize(messagesPanePreferredSize);
        JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPane, messagesPane);
        splitter.setOneTouchExpandable(true);
        splitter.resetToPreferredSizes();
        JSplitPane horizonSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitter, registersPane);
        horizonSplitter.setOneTouchExpandable(true);
        horizonSplitter.resetToPreferredSizes();

        // due to dependencies, do not set up menu/toolbar until now.
        this.createActionObjects();
        menu = this.setUpMenuBar();
        this.setJMenuBar(menu);

        JToolBar toolbar = this.setUpToolBar();

        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(toolbar);
        jp.add(RunSpeedPanel.getInstance());
        JPanel center = new JPanel(new BorderLayout());
        center.add(jp, BorderLayout.NORTH);
        center.add(horizonSplitter);

        this.getContentPane().add(center);

        FileStatus.reset();
        // The following has side effect of establishing menu state
        FileStatus.set(FileStatus.NO_FILE);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                // Maximize the application window
                VenusUI.this.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }

            @Override
            public void windowClosing(WindowEvent event) {
                // Check for unsaved changes before closing the application
                // Don't save the workspace state after closing all files, unless the exit fails
                editor.getEditTabbedPane().setWorkspaceStateSavingEnabled(false);
                if (editor.closeAll()) {
                    System.exit(0);
                }
                editor.getEditTabbedPane().setWorkspaceStateSavingEnabled(true);
            }
        });

        // The following will handle the windowClosing event properly in the
        // situation where user Cancels out of "save edits?" dialog.  By default,
        // the GUI frame will be hidden but I want it to do nothing.
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // Restore previous session
        this.getMainPane().getEditTabbedPane().loadWorkspaceState();

        this.pack();
        this.setVisible(true);
    }

    /**
     * Action objects are used instead of action listeners because one can be easily shared between
     * a menu item and a toolbar button.  Does nice things like disable both if the action is
     * disabled, etc.
     */
    private void createActionObjects() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Class<?> cs = this.getClass();

        try {
            fileNewAction = new FileNewAction(this, "New", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "New22.png"))), "Create a new file for editing", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, tk.getMenuShortcutKeyMaskEx()));
            fileOpenAction = new FileOpenAction(this, "Open...", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Open22.png"))), "Open a file for editing", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, tk.getMenuShortcutKeyMaskEx()));
            fileCloseAction = new FileCloseAction(this, "Close", null, "Close the current file", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMaskEx()));
            fileCloseAllAction = new FileCloseAllAction(this, "Close All", null, "Close all open files", KeyEvent.VK_L, null);
            fileSaveAction = new FileSaveAction(this, "Save", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Save22.png"))), "Save the current file", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, tk.getMenuShortcutKeyMaskEx()));
            fileSaveAsAction = new FileSaveAsAction(this, "Save as...", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "SaveAs22.png"))), "Save current file with different name", KeyEvent.VK_A, null);
            fileSaveAllAction = new FileSaveAllAction(this, "Save All", null, "Save all open files", KeyEvent.VK_V, null);
            fileDumpMemoryAction = new FileDumpMemoryAction(this, "Dump Memory...", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Dump22.png"))), "Dump machine code or data in an available format", KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, tk.getMenuShortcutKeyMaskEx()));
            filePrintAction = new FilePrintAction(this, "Print...", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Print22.gif"))), "Print current file", KeyEvent.VK_P, null);
            fileExitAction = new FileExitAction(this, "Exit", null, "Exit " + Globals.APPLICATION_NAME, KeyEvent.VK_X, null);

            editUndoAction = new EditUndoAction(this, "Undo", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Undo22.png"))), "Undo last edit", KeyEvent.VK_U, KeyStroke.getKeyStroke(KeyEvent.VK_Z, tk.getMenuShortcutKeyMaskEx()));
            editRedoAction = new EditRedoAction(this, "Redo", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Redo22.png"))), "Redo last edit", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_Y, tk.getMenuShortcutKeyMaskEx()));
            editCutAction = new EditCutAction(this, "Cut", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Cut22.gif"))), "Cut", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_X, tk.getMenuShortcutKeyMaskEx()));
            editCopyAction = new EditCopyAction(this, "Copy", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Copy22.png"))), "Copy", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_C, tk.getMenuShortcutKeyMaskEx()));
            editPasteAction = new EditPasteAction(this, "Paste", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Paste22.png"))), "Paste", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_V, tk.getMenuShortcutKeyMaskEx()));
            editFindReplaceAction = new EditFindReplaceAction(this, "Find/Replace", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Find22.png"))), "Find/Replace", KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F, tk.getMenuShortcutKeyMaskEx()));
            editSelectAllAction = new EditSelectAllAction(this, "Select All", null, "Select All", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_A, tk.getMenuShortcutKeyMaskEx()));

            runAssembleAction = new RunAssembleAction(this, "Assemble", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Assemble22.png"))), "Assemble the current file and clear breakpoints", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
            runStartAction = new RunStartAction(this, "Start", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Play22.png"))), "Run the current program", KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
            runStepAction = new RunStepAction(this, "Step Forward", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "StepForward22.png"))), "Execute the next instruction", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
            runBackstepAction = new RunBackstepAction("Step Backward", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "StepBack22.png"))), "Undo the last step", KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), this);
            runPauseAction = new RunPauseAction(this, "Pause", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Pause22.png"))), "Pause the currently running program", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
            runStopAction = new RunStopAction(this, "Stop", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Stop22.png"))), "Stop the currently running program", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
            runResetAction = new RunResetAction(this, "Reset", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Reset22.png"))), "Reset MIPS memory and registers", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
            runClearBreakpointsAction = new RunClearBreakpointsAction(this, "Clear All Breakpoints", null, "Clears all execution breakpoints set since the last assemble.", KeyEvent.VK_K, KeyStroke.getKeyStroke(KeyEvent.VK_K, tk.getMenuShortcutKeyMaskEx()));
            runToggleBreakpointsAction = new RunToggleBreakpointsAction(this, "Toggle All Breakpoints", null, "Disable/enable all breakpoints without clearing (can also click Bkpt column header)", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_T, tk.getMenuShortcutKeyMaskEx()));

            settingsLabelAction = new SettingsLabelAction(this, "Show symbol table", null, "Toggle visibility of Labels window (symbol table) in the Execute tab", null, null);
            settingsPopupInputAction = new SettingsPopupInputAction(this, "Use dialog for user input", null, "If set, use popup dialog for input syscalls (5, 6, 7, 8, 12) instead of console input", null, null);
            settingsValueDisplayBaseAction = new SettingsValueDisplayBaseAction(this, "Hexadecimal values", null, "Toggle between hexadecimal and decimal display of memory/register values", null, null);
            settingsAddressDisplayBaseAction = new SettingsAddressDisplayBaseAction(this, "Hexadecimal addresses", null, "Toggle between hexadecimal and decimal display of memory addresses", null, null);
            settingsExtendedAction = new SettingsExtendedAction(this, "Allow extended (pseudo) instructions", null, "If set, MIPS extended (pseudo) instructions are formats are permitted.", null, null);
            settingsAssembleOnOpenAction = new SettingsAssembleOnOpenAction(this, "Assemble files when opened", null, "If set, a file will be automatically assembled as soon as it is opened.  File Open dialog will show most recently opened file.", null, null);
            settingsAssembleAllAction = new SettingsAssembleAllAction(this, "Assemble all files in current directory", null, "If set, all files in current directory will be assembled when Assemble operation is selected.", null, null);
            settingsWarningsAreErrorsAction = new SettingsWarningsAreErrorsAction(this, "Promote assembler warnings to errors", null, "If set, assembler warnings will be interpreted as errors and prevent successful assembly.", null, null);
            settingsStartAtMainAction = new SettingsStartAtMainAction(this, "Use \"main\" as program entry point", null, "If set, assembler will initialize Program Counter to text address globally labeled 'main', if defined.", null, null);
            settingsProgramArgumentsAction = new SettingsProgramArgumentsAction(this, "Allow program arguments", null, "If set, program arguments for MIPS program can be entered in border of Text Segment window.", null, null);
            settingsDelayedBranchingAction = new SettingsDelayedBranchingAction(this, "Delayed branching", null, "If set, delayed branching will occur during MIPS execution.", null, null);
            settingsSelfModifyingCodeAction = new SettingsSelfModifyingCodeAction(this, "Self-modifying code", null, "If set, the MIPS program can write and branch to both text and data segments.", null, null);
            settingsEditorAction = new SettingsEditorAction(this, "Editor Settings...", null, "View and modify text editor settings.", null, null);
            settingsHighlightingAction = new SettingsHighlightingAction(this, "Highlighting...", null, "View and modify Execute tab highlighting colors", null, null);
            settingsExceptionHandlerAction = new SettingsExceptionHandlerAction(this, "Exception Handler...", null, "If set, the specified exception handler file will be included in all Assemble operations.", null, null);
            settingsMemoryConfigurationAction = new SettingsMemoryConfigurationAction(this, "Memory Configuration...", null, "View and modify memory segment base addresses for simulated MIPS.", null, null);

            helpHelpAction = new HelpHelpAction(this, "Help...", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Help22.png"))), "Help", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
            helpAboutAction = new HelpAboutAction(this, "About...", null, "Information about " + Globals.APPLICATION_NAME, null, null);
        }
        catch (NullPointerException e) {
            System.out.println("Internal Error: images folder not found, or other null pointer exception while creating Action objects");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Build the menus and connect them to action objects (which serve as action listeners
     * shared between menu item and corresponding toolbar icon).
     */
    private JMenuBar setUpMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        run = new JMenu("Run");
        run.setMnemonic(KeyEvent.VK_R);
        settings = new JMenu("Settings");
        settings.setMnemonic(KeyEvent.VK_S);
        help = new JMenu("Help");
        // slight bug: user typing alt-H activates help menu item directly, not help menu
        help.setMnemonic(KeyEvent.VK_H);

        fileNew = new JMenuItem(fileNewAction);
        fileOpen = new JMenuItem(fileOpenAction);
        fileClose = new JMenuItem(fileCloseAction);
        fileCloseAll = new JMenuItem(fileCloseAllAction);
        fileSave = new JMenuItem(fileSaveAction);
        fileSaveAs = new JMenuItem(fileSaveAsAction);
        fileSaveAll = new JMenuItem(fileSaveAllAction);
        fileDumpMemory = new JMenuItem(fileDumpMemoryAction);
        filePrint = new JMenuItem(filePrintAction);
        fileExit = new JMenuItem(fileExitAction);

        file.add(fileNew);
        file.add(fileOpen);
        file.add(fileClose);
        file.add(fileCloseAll);
        file.addSeparator();
        file.add(fileSave);
        file.add(fileSaveAs);
        file.add(fileSaveAll);
        file.add(fileDumpMemory);
        file.addSeparator();
        file.add(filePrint);
        file.addSeparator();
        file.add(fileExit);

        editUndo = new JMenuItem(editUndoAction);
        editRedo = new JMenuItem(editRedoAction);
        editCut = new JMenuItem(editCutAction);
        editCopy = new JMenuItem(editCopyAction);
        editPaste = new JMenuItem(editPasteAction);
        editFindReplace = new JMenuItem(editFindReplaceAction);
        editSelectAll = new JMenuItem(editSelectAllAction);
        edit.add(editUndo);
        edit.add(editRedo);
        edit.addSeparator();
        edit.add(editCut);
        edit.add(editCopy);
        edit.add(editPaste);
        edit.addSeparator();
        edit.add(editFindReplace);
        edit.add(editSelectAll);

        runAssemble = new JMenuItem(runAssembleAction);
        runGo = new JMenuItem(runStartAction);
        runStep = new JMenuItem(runStepAction);
        runBackstep = new JMenuItem(runBackstepAction);
        runReset = new JMenuItem(runResetAction);
        runStop = new JMenuItem(runStopAction);
        runPause = new JMenuItem(runPauseAction);
        runClearBreakpoints = new JMenuItem(runClearBreakpointsAction);
        runToggleBreakpoints = new JMenuItem(runToggleBreakpointsAction);

        run.add(runAssemble);
        run.add(runGo);
        run.add(runStep);
        run.add(runBackstep);
        run.add(runPause);
        run.add(runStop);
        run.add(runReset);
        run.addSeparator();
        run.add(runClearBreakpoints);
        run.add(runToggleBreakpoints);

        settingsLabel = new JCheckBoxMenuItem(settingsLabelAction);
        settingsLabel.setSelected(Globals.getSettings().labelWindowVisible.get());
        settingsPopupInput = new JCheckBoxMenuItem(settingsPopupInputAction);
        settingsPopupInput.setSelected(Globals.getSettings().popupSyscallInput.get());
        settingsValueDisplayBase = new JCheckBoxMenuItem(settingsValueDisplayBaseAction);
        settingsValueDisplayBase.setSelected(Globals.getSettings().displayValuesInHex.get());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has already been created.
        mainPane.getExecutePane().getValueDisplayBaseChooser().setSettingsMenuItem(settingsValueDisplayBase);
        settingsAddressDisplayBase = new JCheckBoxMenuItem(settingsAddressDisplayBaseAction);
        settingsAddressDisplayBase.setSelected(Globals.getSettings().displayAddressesInHex.get());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has already been created.
        mainPane.getExecutePane().getAddressDisplayBaseChooser().setSettingsMenuItem(settingsAddressDisplayBase);
        settingsExtended = new JCheckBoxMenuItem(settingsExtendedAction);
        settingsExtended.setSelected(Globals.getSettings().extendedAssemblerEnabled.get());
        settingsDelayedBranching = new JCheckBoxMenuItem(settingsDelayedBranchingAction);
        settingsDelayedBranching.setSelected(Globals.getSettings().delayedBranchingEnabled.get());
        settingsSelfModifyingCode = new JCheckBoxMenuItem(settingsSelfModifyingCodeAction);
        settingsSelfModifyingCode.setSelected(Globals.getSettings().selfModifyingCodeEnabled.get());
        settingsAssembleOnOpen = new JCheckBoxMenuItem(settingsAssembleOnOpenAction);
        settingsAssembleOnOpen.setSelected(Globals.getSettings().assembleOnOpenEnabled.get());
        settingsAssembleAll = new JCheckBoxMenuItem(settingsAssembleAllAction);
        settingsAssembleAll.setSelected(Globals.getSettings().assembleAllEnabled.get());
        settingsWarningsAreErrors = new JCheckBoxMenuItem(settingsWarningsAreErrorsAction);
        settingsWarningsAreErrors.setSelected(Globals.getSettings().warningsAreErrors.get());
        settingsStartAtMain = new JCheckBoxMenuItem(settingsStartAtMainAction);
        settingsStartAtMain.setSelected(Globals.getSettings().startAtMain.get());
        settingsProgramArguments = new JCheckBoxMenuItem(settingsProgramArgumentsAction);
        settingsProgramArguments.setSelected(Globals.getSettings().useProgramArguments.get());
        settingsEditor = new JMenuItem(settingsEditorAction);
        settingsHighlighting = new JMenuItem(settingsHighlightingAction);
        settingsExceptionHandler = new JMenuItem(settingsExceptionHandlerAction);
        settingsMemoryConfiguration = new JMenuItem(settingsMemoryConfigurationAction);

        settings.add(settingsLabel);
        settings.add(settingsAddressDisplayBase);
        settings.add(settingsValueDisplayBase);
        settings.addSeparator();
        settings.add(settingsAssembleOnOpen);
        settings.add(settingsAssembleAll);
        settings.add(settingsWarningsAreErrors);
        settings.add(settingsStartAtMain);
        settings.add(settingsExtended);
        settings.addSeparator();
        settings.add(settingsProgramArguments);
        settings.add(settingsPopupInput);
        settings.add(settingsDelayedBranching);
        settings.add(settingsSelfModifyingCode);
        settings.addSeparator();
        settings.add(settingsEditor);
        settings.add(settingsHighlighting);
        settings.add(settingsExceptionHandler);
        settings.add(settingsMemoryConfiguration);

        helpHelp = new JMenuItem(helpHelpAction);
        helpAbout = new JMenuItem(helpAboutAction);
        help.add(helpHelp);
        help.addSeparator();
        help.add(helpAbout);

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(run);
        menuBar.add(settings);
        JMenu toolMenu = new ToolLoader().buildToolsMenu();
        if (toolMenu != null) {
            menuBar.add(toolMenu);
        }
        menuBar.add(help);

        return menuBar;
    }

    /**
     * Build the toolbar and connect items to action objects (which serve as action listeners
     * shared between toolbar icon and corresponding menu item).
     */
    // TODO: Make the toolbar setup customizable -Sean Clarke
    JToolBar setUpToolBar() {
        JToolBar toolBar = new JToolBar();

        JButton newButton = new JButton(fileNewAction);
        newButton.setText("");
        JButton openButton = new JButton(fileOpenAction);
        openButton.setText("");
        JButton saveButton = new JButton(fileSaveAction);
        saveButton.setText("");
        JButton saveAsButton = new JButton(fileSaveAsAction);
        saveAsButton.setText("");
        JButton dumpMemoryButton = new JButton(fileDumpMemoryAction);
        dumpMemoryButton.setText("");
        JButton printButton = new JButton(filePrintAction);
        printButton.setText("");

        JButton undoButton = new JButton(editUndoAction);
        undoButton.setText("");
        JButton redoButton = new JButton(editRedoAction);
        redoButton.setText("");
        JButton cutButton = new JButton(editCutAction);
        cutButton.setText("");
        JButton copyButton = new JButton(editCopyAction);
        copyButton.setText("");
        JButton pasteButton = new JButton(editPasteAction);
        pasteButton.setText("");
        JButton findReplaceButton = new JButton(editFindReplaceAction);
        findReplaceButton.setText("");
        JButton selectAllButton = new JButton(editSelectAllAction);
        selectAllButton.setText("");

        JButton runButton = new JButton(runStartAction);
        runButton.setText("");
        JButton assembleButton = new JButton(runAssembleAction);
        assembleButton.setText("");
        JButton stepButton = new JButton(runStepAction);
        stepButton.setText("");
        JButton backstepButton = new JButton(runBackstepAction);
        backstepButton.setText("");
        JButton resetButton = new JButton(runResetAction);
        resetButton.setText("");
        JButton stopButton = new JButton(runStopAction);
        stopButton.setText("");
        JButton pauseButton = new JButton(runPauseAction);
        pauseButton.setText("");
        JButton helpButton = new JButton(helpHelpAction);
        helpButton.setText("");

        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.add(saveAsButton);
        toolBar.add(dumpMemoryButton);
        toolBar.add(printButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(undoButton);
        toolBar.add(redoButton);
        toolBar.add(cutButton);
        toolBar.add(copyButton);
        toolBar.add(pasteButton);
        toolBar.add(findReplaceButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(assembleButton);
        toolBar.add(runButton);
        toolBar.add(stepButton);
        toolBar.add(backstepButton);
        toolBar.add(pauseButton);
        toolBar.add(stopButton);
        toolBar.add(resetButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(helpButton);
        toolBar.add(new JToolBar.Separator());

        return toolBar;
    }

    /**
     * Determine from FileStatus what the menu state (enabled/disabled) should
     * be then call the appropriate method to set it.  Current states are:
     * <ul>
     * <li>setMenuStateInitial: set upon startup and after File->Close
     * <li>setMenuStateEditingNew: set upon File->New
     * <li>setMenuStateEditing: set upon File->Open or File->Save or erroneous Run->Assemble
     * <li>setMenuStateRunnable: set upon successful Run->Assemble
     * <li>setMenuStateRunning: set upon Run->Go
     * <li>setMenuStateTerminated: set upon completion of simulated execution
     * </ul>
     */
    public void setMenuState(int status) {
        menuState = status;
        switch (status) {
            case FileStatus.NO_FILE -> setMenuStateInitial();
            case FileStatus.NEW_NOT_EDITED -> setMenuStateEditingNew();
            case FileStatus.NEW_EDITED -> setMenuStateEditingNew();
            case FileStatus.NOT_EDITED -> setMenuStateNotEdited(); // was MenuStateEditing. DPS 9-Aug-2011
            case FileStatus.EDITED -> setMenuStateEditing();
            case FileStatus.RUNNABLE -> setMenuStateRunnable();
            case FileStatus.RUNNING -> setMenuStateRunning();
            case FileStatus.TERMINATED -> setMenuStateTerminated();
            case FileStatus.OPENING -> {} // This is a temporary state. DPS 9-Aug-2011
            default -> System.out.println("Invalid File Status: " + status);
        }
    }

    void setMenuStateInitial() {
        // Note: undo and redo are handled separately by the undo manager
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(false);
        fileCloseAllAction.setEnabled(false);
        fileSaveAction.setEnabled(false);
        fileSaveAsAction.setEnabled(false);
        fileSaveAllAction.setEnabled(false);
        fileDumpMemoryAction.setEnabled(false);
        filePrintAction.setEnabled(false);
        fileExitAction.setEnabled(true);
        editUndoAction.setEnabled(false);
        editRedoAction.setEnabled(false);
        editCutAction.setEnabled(false);
        editCopyAction.setEnabled(false);
        editPasteAction.setEnabled(false);
        editFindReplaceAction.setEnabled(false);
        editSelectAllAction.setEnabled(false);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(false);
        runStartAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        updateUndoRedoActions();
    }

    /*
     * Added DPS 9-Aug-2011, for newly-opened files.  Retain
     * existing Run menu state (except Assemble, which is always true).
     * Thus if there was a valid assembly it is retained.
     */
    void setMenuStateNotEdited() {
        // Note: undo and redo are handled separately by the undo manager
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(false);
        filePrintAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        updateUndoRedoActions();
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true);
        settingsMemoryConfigurationAction.setEnabled(true);
        runAssembleAction.setEnabled(true);
        // If assemble-all, allow previous Run menu settings to remain.
        // Otherwise, clear them out.  DPS 9-Aug-2011
        if (!Globals.getSettings().assembleAllEnabled.get()) {
            runStartAction.setEnabled(false);
            runStepAction.setEnabled(false);
            runBackstepAction.setEnabled(false);
            runResetAction.setEnabled(false);
            runStopAction.setEnabled(false);
            runPauseAction.setEnabled(false);
            runClearBreakpointsAction.setEnabled(false);
            runToggleBreakpointsAction.setEnabled(false);
        }
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
    }

    void setMenuStateEditing() {
        // Note: undo and redo are handled separately by the undo manager
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(false);
        filePrintAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        updateUndoRedoActions();
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(true);
        runStartAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
    }

    /**
     * Use this when "File -> New" is used
     */
    void setMenuStateEditingNew() {
        // Note: undo and redo are handled separately by the undo manager
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(false);
        filePrintAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        updateUndoRedoActions();
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(false);
        runStartAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
    }

    /**
     * Use this upon successful assemble or reset
     */
    void setMenuStateRunnable() {
        // Note: undo and redo are handled separately by the undo manager
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(true);
        filePrintAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        updateUndoRedoActions();
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(true);
        runStartAction.setEnabled(true);
        runStepAction.setEnabled(true);
        runBackstepAction.setEnabled(Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().isEmpty());
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(true);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
    }

    /**
     * Use this while program is running
     */
    void setMenuStateRunning() {
        fileNewAction.setEnabled(false);
        fileOpenAction.setEnabled(false);
        fileCloseAction.setEnabled(false);
        fileCloseAllAction.setEnabled(false);
        fileSaveAction.setEnabled(false);
        fileSaveAsAction.setEnabled(false);
        fileSaveAllAction.setEnabled(false);
        fileDumpMemoryAction.setEnabled(false);
        filePrintAction.setEnabled(false);
        fileExitAction.setEnabled(false);
        editUndoAction.setEnabled(false); // DPS 10 Jan 2008
        editRedoAction.setEnabled(false); // DPS 10 Jan 2008
        editCutAction.setEnabled(false);
        editCopyAction.setEnabled(false);
        editPasteAction.setEnabled(false);
        editFindReplaceAction.setEnabled(false);
        editSelectAllAction.setEnabled(false);
        settingsDelayedBranchingAction.setEnabled(false); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(false); // added 21 July 2009
        runAssembleAction.setEnabled(false);
        runStartAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(true);
        runPauseAction.setEnabled(true);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
    }

    /**
     * Use this upon completion of execution
     */
    void setMenuStateTerminated() {
        // Note: undo and redo are handled separately by the undo manager
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(true);
        filePrintAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        updateUndoRedoActions();
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(true);
        runStartAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().isEmpty());
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(true);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
    }

    /**
     * Automatically update whether the Undo and Redo actions are enabled or disabled
     * based on the status of the {@link javax.swing.undo.UndoManager}.
     */
    public void updateUndoRedoActions() {
        editUndoAction.updateEnabledStatus();
        editRedoAction.updateEnabledStatus();
    }

    /**
     * Set the content to display as part of the window title, if any. (Usually, this is a filename.)
     * The resulting full title will look like "(content) - (base title)" if the content is not null,
     * or simply the base title otherwise.
     */
    public void setTitleContent(String content) {
        if (content != null) {
            setTitle(content + " - " + baseTitle);
        }
        else {
            setTitle(baseTitle);
        }
    }

    /**
     * Get current menu state.  State values are constants in FileStatus class.  DPS 23 July 2008
     *
     * @return Current menu state.
     */
    public int getMenuState() {
        return menuState;
    }

    /**
     * Set whether the register values are reset.
     *
     * @param reset Boolean true if the register values have been reset.
     */
    public static void setReset(boolean reset) {
        VenusUI.reset = reset;
    }

    /**
     * Set whether MIPS program execution has started.
     *
     * @param started true if the MIPS program execution has started.
     */
    public static void setStarted(boolean started) {
        VenusUI.started = started;
    }

    /**
     * Get whether the register values are reset.
     *
     * @return Boolean true if the register values have been reset.
     */
    public static boolean getReset() {
        return reset;
    }

    /**
     * Get whether MIPS program is currently executing.
     *
     * @return true if MIPS program is currently executing.
     */
    public static boolean getStarted() {
        return started;
    }

    /**
     * Get a reference to the text editor associated with this GUI.
     *
     * @return Editor for the GUI.
     */
    public Editor getEditor() {
        return editor;
    }

    /**
     * Get a reference to the main pane associated with this GUI.
     *
     * @return MainPane for the GUI.
     */
    public MainPane getMainPane() {
        return mainPane;
    }

    /**
     * Get a reference to the messages pane associated with this GUI.
     *
     * @return MessagesPane for the GUI.
     */
    public MessagesPane getMessagesPane() {
        return messagesPane;
    }

    /**
     * Get reference to registers pane associated with this GUI.
     *
     * @return RegistersPane for the GUI.
     */
    public RegistersPane getRegistersPane() {
        return registersPane;
    }

    /**
     * Get a reference to the settings menu checkbox for the display base of memory/register values.
     *
     * @return The checkbox menu item.
     */
    public JCheckBoxMenuItem getValueDisplayBaseMenuItem() {
        return settingsValueDisplayBase;
    }

    /**
     * Get a reference to the settings menu checkbox for the display base of memory addresses.
     *
     * @return The checkbox menu item.
     */
    public JCheckBoxMenuItem getAddressDisplayBaseMenuItem() {
        return settingsAddressDisplayBase;
    }

    /**
     * Get a reference to the Run->Assemble action.  Needed by File->Open in case the
     * assemble-upon-open flag is set.
     *
     * @return The object for the Run->Assemble operation.
     */
    public RunAssembleAction getRunAssembleAction() {
        return runAssembleAction;
    }

    /**
     * Have the menu request keyboard focus.  DPS 5-4-10
     */
    public void haveMenuRequestFocus() {
        this.menu.requestFocus();
    }

    /**
     * Send keyboard event to menu for possible processing.  DPS 5-4-10
     *
     * @param e KeyEvent for menu component to consider for processing.
     */
    public void dispatchEventToMenu(KeyEvent e) {
        this.menu.dispatchEvent(e);
    }
}