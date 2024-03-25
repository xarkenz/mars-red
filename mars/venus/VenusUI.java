package mars.venus;

import mars.Globals;
import mars.Settings;

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
    JMenuBar menu;
    JToolBar toolbar;
    MainPane mainPane;
    RegistersPane registersPane;
    RegistersWindow registersTab;
    Coprocessor1Window coprocessor1Tab;
    Coprocessor0Window coprocessor0Tab;
    MessagesPane messagesPane;
    JSplitPane splitter, horizonSplitter;

    private final String baseTitle;
    private int menuState = FileStatus.NO_FILE;

    // PLEASE PUT THESE TWO (& THEIR METHODS) SOMEWHERE THEY BELONG, NOT HERE
    private static boolean reset = true; // registers/memory reset for execution
    private static boolean started = false; // started execution
    Editor editor;

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
    private Action fileNewAction;
    private Action fileOpenAction;
    private Action fileCloseAction;
    private Action fileCloseAllAction;
    private Action fileSaveAction;
    private Action fileSaveAsAction;
    private Action fileSaveAllAction;
    private Action fileDumpMemoryAction;
    private Action filePrintAction;
    private Action fileExitAction;
    EditUndoAction editUndoAction;
    EditRedoAction editRedoAction;
    private Action editCutAction;
    private Action editCopyAction;
    private Action editPasteAction;
    private Action editFindReplaceAction;
    private Action editSelectAllAction;
    private Action runAssembleAction;
    private Action runGoAction;
    private Action runStepAction;
    private Action runBackstepAction;
    private Action runResetAction;
    private Action runStopAction;
    private Action runPauseAction;
    private Action runClearBreakpointsAction;
    private Action runToggleBreakpointsAction;
    private Action settingsLabelAction;
    private Action settingsPopupInputAction;
    private Action settingsValueDisplayBaseAction;
    private Action settingsAddressDisplayBaseAction;
    private Action settingsExtendedAction;
    private Action settingsAssembleOnOpenAction;
    private Action settingsAssembleAllAction;
    private Action settingsWarningsAreErrorsAction;
    private Action settingsStartAtMainAction;
    private Action settingsProgramArgumentsAction;
    private Action settingsDelayedBranchingAction;
    private Action settingsExceptionHandlerAction;
    private Action settingsEditorAction;
    private Action settingsHighlightingAction;
    private Action settingsMemoryConfigurationAction;
    private Action settingsSelfModifyingCodeAction;
    private Action helpHelpAction;
    private Action helpAboutAction;

    /**
     * Constructor for the Class. Sets up a window object for the UI
     *
     * @param title Name of the window to be created.
     */
    public VenusUI(String title) {
        super(title);
        this.baseTitle = title;
        this.editor = new Editor(this);
        Globals.setGui(this);

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

        // the "restore" size (window control button that toggles with maximize)
        // I want to keep it large, with enough room for user to get handles
        //this.setSize((int)(screenWidth*.8),(int)(screenHeight*.8));

        Globals.initialize(true);

        // Image courtesy of NASA/JPL.
        URL im = this.getClass().getResource(Globals.IMAGES_PATH + "RedMars16.gif");
        if (im == null) {
            System.out.println("Internal Error: images folder or file not found");
            System.exit(0);
        }
        Image mars = Toolkit.getDefaultToolkit().getImage(im);
        this.setIconImage(mars);
        // Everything in frame will be arranged on JPanel "center", which is only frame component.
        // "center" has BorderLayout and 2 major components:
        //   -- panel (jp) on North with 2 components
        //      1. toolbar
        //      2. run speed slider.
        //   -- split pane (horizonSplitter) in center with 2 components side-by-side
        //      1. split pane (splitter) with 2 components stacked
        //         a. main pane, with 2 tabs (edit, execute)
        //         b. messages pane with 2 tabs (mars, run I/O)
        //      2. registers pane with 3 tabs (register file, coproc 0, coproc 1)
        // I should probably run this breakdown out to full detail.  The components are created
        // roughly in bottom-up order; some are created in component constructors and thus are
        // not visible here.

        registersTab = new RegistersWindow();
        coprocessor1Tab = new Coprocessor1Window();
        coprocessor0Tab = new Coprocessor0Window();
        registersPane = new RegistersPane(this, registersTab, coprocessor1Tab, coprocessor0Tab);
        registersPane.setPreferredSize(registersPanePreferredSize);

        //Insets defaultTabInsets = (Insets)UIManager.get("TabbedPane.tabInsets");
        //UIManager.put("TabbedPane.tabInsets", new Insets(1, 1, 1, 1));
        mainPane = new MainPane(this, editor, registersTab, coprocessor1Tab, coprocessor0Tab);
        //UIManager.put("TabbedPane.tabInsets", defaultTabInsets);

        mainPane.setPreferredSize(mainPanePreferredSize);
        messagesPane = new MessagesPane();
        messagesPane.setPreferredSize(messagesPanePreferredSize);
        splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPane, messagesPane);
        splitter.setOneTouchExpandable(true);
        splitter.resetToPreferredSizes();
        horizonSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitter, registersPane);
        horizonSplitter.setOneTouchExpandable(true);
        horizonSplitter.resetToPreferredSizes();

        // due to dependencies, do not set up menu/toolbar until now.
        this.createActionObjects();
        menu = this.setUpMenuBar();
        this.setJMenuBar(menu);

        toolbar = this.setUpToolBar();

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

        // This is invoked when opening the app.  It will set the app to
        // appear at full screen size.
        this.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                VenusUI.this.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });

        // This is invoked when exiting the app through the X icon.  It will in turn
        // check for unsaved edits before exiting.
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (VenusUI.this.editor.closeAll()) {
                    System.exit(0);
                }
            }
        });

        // The following will handle the windowClosing event properly in the
        // situation where user Cancels out of "save edits?" dialog.  By default,
        // the GUI frame will be hidden but I want it to do nothing.
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        this.pack();
        this.setVisible(true);
    }

    /*
     * Action objects are used instead of action listeners because one can be easily shared between
     * a menu item and a toolbar button.  Does nice things like disable both if the action is
     * disabled, etc.
     */
    private void createActionObjects() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Class<?> cs = this.getClass();

        try {
            fileNewAction = new FileNewAction("New", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "New22.png"))), "Create a new file for editing", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, tk.getMenuShortcutKeyMaskEx()), this);
            fileOpenAction = new FileOpenAction("Open ...", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Open22.png"))), "Open a file for editing", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, tk.getMenuShortcutKeyMaskEx()), this);
            fileCloseAction = new FileCloseAction("Close", null, "Close the current file", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMaskEx()), this);
            fileCloseAllAction = new FileCloseAllAction("Close All", null, "Close all open files", KeyEvent.VK_L, null, this);
            fileSaveAction = new FileSaveAction("Save", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Save22.png"))), "Save the current file", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, tk.getMenuShortcutKeyMaskEx()), this);
            fileSaveAsAction = new FileSaveAsAction("Save as ...", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "SaveAs22.png"))), "Save current file with different name", KeyEvent.VK_A, null, this);
            fileSaveAllAction = new FileSaveAllAction("Save All", null, "Save all open files", KeyEvent.VK_V, null, this);
            fileDumpMemoryAction = new FileDumpMemoryAction("Dump Memory ...", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Dump22.png"))), "Dump machine code or data in an available format", KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, tk.getMenuShortcutKeyMaskEx()), this);
            filePrintAction = new FilePrintAction("Print ...", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Print22.gif"))), "Print current file", KeyEvent.VK_P, null, this);
            fileExitAction = new FileExitAction("Exit", null, "Exit Mars", KeyEvent.VK_X, null, this);
            editUndoAction = new EditUndoAction("Undo", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Undo22.png"))), "Undo last edit", KeyEvent.VK_U, KeyStroke.getKeyStroke(KeyEvent.VK_Z, tk.getMenuShortcutKeyMaskEx()), this);
            editRedoAction = new EditRedoAction("Redo", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Redo22.png"))), "Redo last edit", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_Y, tk.getMenuShortcutKeyMaskEx()), this);
            editCutAction = new EditCutAction("Cut", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Cut22.gif"))), "Cut", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_X, tk.getMenuShortcutKeyMaskEx()), this);
            editCopyAction = new EditCopyAction("Copy", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Copy22.png"))), "Copy", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_C, tk.getMenuShortcutKeyMaskEx()), this);
            editPasteAction = new EditPasteAction("Paste", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Paste22.png"))), "Paste", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_V, tk.getMenuShortcutKeyMaskEx()), this);
            editFindReplaceAction = new EditFindReplaceAction("Find/Replace", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Find22.png"))), "Find/Replace", KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F, tk.getMenuShortcutKeyMaskEx()), this);
            editSelectAllAction = new EditSelectAllAction("Select All", null, "Select All", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_A, tk.getMenuShortcutKeyMaskEx()), this);
            runAssembleAction = new RunAssembleAction("Assemble", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Assemble22.png"))), "Assemble the current file and clear breakpoints", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), this);
            runGoAction = new RunGoAction("Go", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Play22.png"))), "Run the current program", KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), this);
            runStepAction = new RunStepAction("Step", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "StepForward22.png"))), "Run one step at a time", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), this);
            runBackstepAction = new RunBackstepAction("Backstep", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "StepBack22.png"))), "Undo the last step", KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), this);
            runPauseAction = new RunPauseAction("Pause", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Pause22.png"))), "Pause the currently running program", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), this);
            runStopAction = new RunStopAction("Stop", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Stop22.png"))), "Stop the currently running program", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), this);
            runResetAction = new RunResetAction("Reset", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Reset22.png"))), "Reset MIPS memory and registers", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), this);
            runClearBreakpointsAction = new RunClearBreakpointsAction("Clear all breakpoints", null, "Clears all execution breakpoints set since the last assemble.", KeyEvent.VK_K, KeyStroke.getKeyStroke(KeyEvent.VK_K, tk.getMenuShortcutKeyMaskEx()), this);
            runToggleBreakpointsAction = new RunToggleBreakpointsAction("Toggle all breakpoints", null, "Disable/enable all breakpoints without clearing (can also click Bkpt column header)", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_T, tk.getMenuShortcutKeyMaskEx()), this);
            settingsLabelAction = new SettingsLabelAction("Show Labels Window (symbol table)", null, "Toggle visibility of Labels window (symbol table) in the Execute tab", null, null, this);
            settingsPopupInputAction = new SettingsPopupInputAction("Popup dialog for input syscalls (5,6,7,8,12)", null, "If set, use popup dialog for input syscalls (5,6,7,8,12) instead of cursor in Run I/O window", null, null, this);

            settingsValueDisplayBaseAction = new SettingsValueDisplayBaseAction("Values displayed in hexadecimal", null, "Toggle between hexadecimal and decimal display of memory/register values", null, null, this);
            settingsAddressDisplayBaseAction = new SettingsAddressDisplayBaseAction("Addresses displayed in hexadecimal", null, "Toggle between hexadecimal and decimal display of memory addresses", null, null, this);
            settingsExtendedAction = new SettingsExtendedAction("Permit extended (pseudo) instructions and formats", null, "If set, MIPS extended (pseudo) instructions are formats are permitted.", null, null, this);
            settingsAssembleOnOpenAction = new SettingsAssembleOnOpenAction("Assemble file upon opening", null, "If set, a file will be automatically assembled as soon as it is opened.  File Open dialog will show most recently opened file.", null, null, this);
            settingsAssembleAllAction = new SettingsAssembleAllAction("Assemble all files in directory", null, "If set, all files in current directory will be assembled when Assemble operation is selected.", null, null, this);
            settingsWarningsAreErrorsAction = new SettingsWarningsAreErrorsAction("Assembler warnings are considered errors", null, "If set, assembler warnings will be interpreted as errors and prevent successful assembly.", null, null, this);
            settingsStartAtMainAction = new SettingsStartAtMainAction("Initialize Program Counter to global 'main' if defined", null, "If set, assembler will initialize Program Counter to text address globally labeled 'main', if defined.", null, null, this);
            settingsProgramArgumentsAction = new SettingsProgramArgumentsAction("Program arguments provided to MIPS program", null, "If set, program arguments for MIPS program can be entered in border of Text Segment window.", null, null, this);
            settingsDelayedBranchingAction = new SettingsDelayedBranchingAction("Delayed branching", null, "If set, delayed branching will occur during MIPS execution.", null, null, this);
            settingsSelfModifyingCodeAction = new SettingsSelfModifyingCodeAction("Self-modifying code", null, "If set, the MIPS program can write and branch to both text and data segments.", null, null, this);
            settingsEditorAction = new SettingsEditorAction("Editor...", null, "View and modify text editor settings.", null, null, this);
            settingsHighlightingAction = new SettingsHighlightingAction("Highlighting...", null, "View and modify Execute Tab highlighting colors", null, null, this);
            settingsExceptionHandlerAction = new SettingsExceptionHandlerAction("Exception Handler...", null, "If set, the specified exception handler file will be included in all Assemble operations.", null, null, this);
            settingsMemoryConfigurationAction = new SettingsMemoryConfigurationAction("Memory Configuration...", null, "View and modify memory segment base addresses for simulated MIPS.", null, null, this);
            helpHelpAction = new HelpHelpAction("Help", new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Help22.png"))), "Help", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), this);
            helpAboutAction = new HelpAboutAction("About ...", null, "Information about Mars", null, null, this);
        }
        catch (NullPointerException e) {
            System.out.println("Internal Error: images folder not found, or other null pointer exception while creating Action objects");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /*
     * build the menus and connect them to action objects (which serve as action listeners
     * shared between menu item and corresponding toolbar icon).
     */
    private JMenuBar setUpMenuBar() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Class<?> cs = this.getClass();

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
        help.setMnemonic(KeyEvent.VK_H);
        // slight bug: user typing alt-H activates help menu item directly, not help menu

        fileNew = new JMenuItem(fileNewAction);
        fileNew.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "New16.png"))));
        fileOpen = new JMenuItem(fileOpenAction);
        fileOpen.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Open16.png"))));
        fileClose = new JMenuItem(fileCloseAction);
        fileClose.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "MyBlank16.gif"))));
        fileCloseAll = new JMenuItem(fileCloseAllAction);
        fileCloseAll.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "MyBlank16.gif"))));
        fileSave = new JMenuItem(fileSaveAction);
        fileSave.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Save16.png"))));
        fileSaveAs = new JMenuItem(fileSaveAsAction);
        fileSaveAs.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "SaveAs16.png"))));
        fileSaveAll = new JMenuItem(fileSaveAllAction);
        fileSaveAll.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "MyBlank16.gif"))));
        fileDumpMemory = new JMenuItem(fileDumpMemoryAction);
        fileDumpMemory.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Dump16.png"))));
        filePrint = new JMenuItem(filePrintAction);
        filePrint.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Print16.gif"))));
        fileExit = new JMenuItem(fileExitAction);
        fileExit.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "MyBlank16.gif"))));
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
        editUndo.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Undo16.png"))));//"Undo16.gif"))));
        editRedo = new JMenuItem(editRedoAction);
        editRedo.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Redo16.png"))));//"Redo16.gif"))));
        editCut = new JMenuItem(editCutAction);
        editCut.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Cut16.gif"))));
        editCopy = new JMenuItem(editCopyAction);
        editCopy.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Copy16.png"))));//"Copy16.gif"))));
        editPaste = new JMenuItem(editPasteAction);
        editPaste.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Paste16.png"))));//"Paste16.gif"))));
        editFindReplace = new JMenuItem(editFindReplaceAction);
        editFindReplace.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Find16.png"))));//"Paste16.gif"))));
        editSelectAll = new JMenuItem(editSelectAllAction);
        editSelectAll.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "MyBlank16.gif"))));
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
        runAssemble.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Assemble16.png"))));//"MyAssemble16.gif"))));
        runGo = new JMenuItem(runGoAction);
        runGo.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Play16.png"))));//"Play16.gif"))));
        runStep = new JMenuItem(runStepAction);
        runStep.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "StepForward16.png"))));//"MyStepForward16.gif"))));
        runBackstep = new JMenuItem(runBackstepAction);
        runBackstep.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "StepBack16.png"))));//"MyStepBack16.gif"))));
        runReset = new JMenuItem(runResetAction);
        runReset.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Reset16.png"))));//"MyReset16.gif"))));
        runStop = new JMenuItem(runStopAction);
        runStop.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Stop16.png"))));//"Stop16.gif"))));
        runPause = new JMenuItem(runPauseAction);
        runPause.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Pause16.png"))));//"Pause16.gif"))));
        runClearBreakpoints = new JMenuItem(runClearBreakpointsAction);
        runClearBreakpoints.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "MyBlank16.gif"))));
        runToggleBreakpoints = new JMenuItem(runToggleBreakpointsAction);
        runToggleBreakpoints.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "MyBlank16.gif"))));

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
        settingsLabel.setSelected(Globals.getSettings().getBoolean(Settings.LABEL_WINDOW_VISIBILITY));
        settingsPopupInput = new JCheckBoxMenuItem(settingsPopupInputAction);
        settingsPopupInput.setSelected(Globals.getSettings().getBoolean(Settings.POPUP_SYSCALL_INPUT));
        settingsValueDisplayBase = new JCheckBoxMenuItem(settingsValueDisplayBaseAction);
        settingsValueDisplayBase.setSelected(Globals.getSettings().getBoolean(Settings.DISPLAY_VALUES_IN_HEX));
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has already been created.
        mainPane.getExecutePane().getValueDisplayBaseChooser().setSettingsMenuItem(settingsValueDisplayBase);
        settingsAddressDisplayBase = new JCheckBoxMenuItem(settingsAddressDisplayBaseAction);
        settingsAddressDisplayBase.setSelected(Globals.getSettings().getBoolean(Settings.DISPLAY_ADDRESSES_IN_HEX));
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has already been created.
        mainPane.getExecutePane().getAddressDisplayBaseChooser().setSettingsMenuItem(settingsAddressDisplayBase);
        settingsExtended = new JCheckBoxMenuItem(settingsExtendedAction);
        settingsExtended.setSelected(Globals.getSettings().getBoolean(Settings.EXTENDED_ASSEMBLER_ENABLED));
        settingsDelayedBranching = new JCheckBoxMenuItem(settingsDelayedBranchingAction);
        settingsDelayedBranching.setSelected(Globals.getSettings().getBoolean(Settings.DELAYED_BRANCHING_ENABLED));
        settingsSelfModifyingCode = new JCheckBoxMenuItem(settingsSelfModifyingCodeAction);
        settingsSelfModifyingCode.setSelected(Globals.getSettings().getBoolean(Settings.SELF_MODIFYING_CODE_ENABLED));
        settingsAssembleOnOpen = new JCheckBoxMenuItem(settingsAssembleOnOpenAction);
        settingsAssembleOnOpen.setSelected(Globals.getSettings().getBoolean(Settings.ASSEMBLE_ON_OPEN_ENABLED));
        settingsAssembleAll = new JCheckBoxMenuItem(settingsAssembleAllAction);
        settingsAssembleAll.setSelected(Globals.getSettings().getBoolean(Settings.ASSEMBLE_ALL_ENABLED));
        settingsWarningsAreErrors = new JCheckBoxMenuItem(settingsWarningsAreErrorsAction);
        settingsWarningsAreErrors.setSelected(Globals.getSettings().getBoolean(Settings.WARNINGS_ARE_ERRORS));
        settingsStartAtMain = new JCheckBoxMenuItem(settingsStartAtMainAction);
        settingsStartAtMain.setSelected(Globals.getSettings().getBoolean(Settings.START_AT_MAIN));
        settingsProgramArguments = new JCheckBoxMenuItem(settingsProgramArgumentsAction);
        settingsProgramArguments.setSelected(Globals.getSettings().getBoolean(Settings.PROGRAM_ARGUMENTS));
        settingsEditor = new JMenuItem(settingsEditorAction);
        settingsHighlighting = new JMenuItem(settingsHighlightingAction);
        settingsExceptionHandler = new JMenuItem(settingsExceptionHandlerAction);
        settingsMemoryConfiguration = new JMenuItem(settingsMemoryConfigurationAction);

        settings.add(settingsLabel);
        settings.add(settingsProgramArguments);
        settings.add(settingsPopupInput);
        settings.add(settingsAddressDisplayBase);
        settings.add(settingsValueDisplayBase);
        settings.addSeparator();
        settings.add(settingsAssembleOnOpen);
        settings.add(settingsAssembleAll);
        settings.add(settingsWarningsAreErrors);
        settings.add(settingsStartAtMain);
        settings.addSeparator();
        settings.add(settingsExtended);
        settings.add(settingsDelayedBranching);
        settings.add(settingsSelfModifyingCode);
        settings.addSeparator();
        settings.add(settingsEditor);
        settings.add(settingsHighlighting);
        settings.add(settingsExceptionHandler);
        settings.add(settingsMemoryConfiguration);

        helpHelp = new JMenuItem(helpHelpAction);
        helpHelp.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "Help16.png"))));//"Help16.gif"))));
        helpAbout = new JMenuItem(helpAboutAction);
        helpAbout.setIcon(new ImageIcon(tk.getImage(cs.getResource(Globals.IMAGES_PATH + "MyBlank16.gif"))));
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

    /*
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

        JButton runButton = new JButton(runGoAction);
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

    /*
     * Determine from FileStatus what the menu state (enabled/disabled) should
     * be then call the appropriate method to set it.  Current states are:
     *
     * setMenuStateInitial: set upon startup and after File->Close
     * setMenuStateEditingNew: set upon File->New
     * setMenuStateEditing: set upon File->Open or File->Save or erroneous Run->Assemble
     * setMenuStateRunnable: set upon successful Run->Assemble
     * setMenuStateRunning: set upon Run->Go
     * setMenuStateTerminated: set upon completion of simulated execution
     */
    void setMenuState(int status) {
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
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        editUndoAction.updateUndoState();
        editRedoAction.updateRedoState();
    }

    /* Added DPS 9-Aug-2011, for newly-opened files.  Retain
        existing Run menu state (except Assemble, which is always true).
         Thus if there was a valid assembly it is retained. */
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
        if (!Globals.getSettings().getBoolean(Settings.ASSEMBLE_ALL_ENABLED)) {
            runGoAction.setEnabled(false);
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
        editUndoAction.updateUndoState();
        editRedoAction.updateRedoState();
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
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(true);
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        editUndoAction.updateUndoState();
        editRedoAction.updateRedoState();
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
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(false);
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        editUndoAction.updateUndoState();
        editRedoAction.updateRedoState();
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
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(true);
        runGoAction.setEnabled(true);
        runStepAction.setEnabled(true);
        runBackstepAction.setEnabled(Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().isEmpty());
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(true);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        editUndoAction.updateUndoState();
        editRedoAction.updateRedoState();
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
        editCutAction.setEnabled(false);
        editCopyAction.setEnabled(false);
        editPasteAction.setEnabled(false);
        editFindReplaceAction.setEnabled(false);
        editSelectAllAction.setEnabled(false);
        settingsDelayedBranchingAction.setEnabled(false); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(false); // added 21 July 2009
        runAssembleAction.setEnabled(false);
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(true);
        runPauseAction.setEnabled(true);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        editUndoAction.setEnabled(false);//updateUndoState(); // DPS 10 Jan 2008
        editRedoAction.setEnabled(false);//updateRedoState(); // DPS 10 Jan 2008
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
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(true);
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().isEmpty());
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(true);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        editUndoAction.updateUndoState();
        editRedoAction.updateRedoState();
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
     * @return current menu state.
     */
    public int getMenuState() {
        return menuState;
    }

    /**
     * To set whether the register values are reset.
     *
     * @param reset Boolean true if the register values have been reset.
     */
    public static void setReset(boolean reset) {
        VenusUI.reset = reset;
    }

    /**
     * To set whether MIPS program execution has started.
     *
     * @param started true if the MIPS program execution has started.
     */
    public static void setStarted(boolean started) {
        VenusUI.started = started;
    }

    /**
     * To find out whether the register values are reset.
     *
     * @return Boolean true if the register values have been reset.
     */
    public static boolean getReset() {
        return reset;
    }

    /**
     * To find out whether MIPS program is currently executing.
     *
     * @return true if MIPS program is currently executing.
     */
    public static boolean getStarted() {
        return started;
    }

    /**
     * Get reference to Editor object associated with this GUI.
     *
     * @return Editor for the GUI.
     */
    public Editor getEditor() {
        return editor;
    }

    /**
     * Get reference to messages pane associated with this GUI.
     *
     * @return MessagesPane object associated with the GUI.
     */
    public MainPane getMainPane() {
        return mainPane;
    }

    /**
     * Get reference to messages pane associated with this GUI.
     *
     * @return MessagesPane object associated with the GUI.
     */
    public MessagesPane getMessagesPane() {
        return messagesPane;
    }

    /**
     * Get reference to registers pane associated with this GUI.
     *
     * @return RegistersPane object associated with the GUI.
     */
    public RegistersPane getRegistersPane() {
        return registersPane;
    }

    /**
     * Get reference to settings menu item for display base of memory/register values.
     *
     * @return the menu item
     */
    public JCheckBoxMenuItem getValueDisplayBaseMenuItem() {
        return settingsValueDisplayBase;
    }

    /**
     * Get reference to settings menu item for display base of memory/register values.
     *
     * @return the menu item
     */
    public JCheckBoxMenuItem getAddressDisplayBaseMenuItem() {
        return settingsAddressDisplayBase;
    }

    /**
     * Return reference to the Run->Assemble item's action.  Needed by File->Open in case
     * assemble-upon-open flag is set.
     *
     * @return the Action object for the Run->Assemble operation.
     */
    public Action getRunAssembleAction() {
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