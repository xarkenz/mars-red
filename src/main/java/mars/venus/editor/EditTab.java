package mars.venus.editor;

import mars.Application;
import mars.Program;
import mars.ProcessingException;
import mars.mips.hardware.RegisterFile;
import mars.util.FilenameFinder;
import mars.venus.VenusUI;
import mars.venus.execute.ProgramStatus;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

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
 * The "Edit" tab in the main tabbed pane, which is itself a tabbed pane.
 * Each of its tabs represents an open file.
 *
 * @author Pete Sanderson
 */
public class EditTab extends JTabbedPane {
    private final VenusUI gui;
    private final Editor editor;
    private boolean isOpeningFiles;
    private boolean workspaceStateSavingEnabled;

    /**
     * Create and initialize the Edit tab with no open files.
     * To restore the previous workspace state, call {@link #loadWorkspaceState()}
     * afterward.
     *
     * @param gui    The parent GUI instance.
     * @param editor The editor instance, which will be used to manage the text editors.
     */
    public EditTab(VenusUI gui, Editor editor) {
        super();
        this.gui = gui;
        this.editor = editor;
        this.isOpeningFiles = false;
        this.workspaceStateSavingEnabled = true;

        this.addChangeListener(event -> {
            FileEditorTab fileEditorTab = (FileEditorTab) this.getSelectedComponent();
            if (fileEditorTab != null) {
                // New IF statement to permit free traversal of edit panes w/o invalidating
                // assembly if assemble-all is selected.  DPS 9-Aug-2011
                if (Application.getSettings().assembleAllEnabled.get()) {
                    this.updateTitle(fileEditorTab);
                }
                else {
                    this.updateTitleAndMenuState(fileEditorTab);
                    this.gui.getMainPane().getExecuteTab().clear();
                }
                fileEditorTab.requestTextAreaFocus();
            }
        });
    }

    /**
     * Get the currently selected editor tab.
     *
     * @return The current editor tab, or null if no files are open.
     */
    public FileEditorTab getCurrentEditorTab() {
        return (FileEditorTab) this.getSelectedComponent();
    }

    /**
     * Select the specified file editor tab to be the current tab.
     *
     * @param tab The tab to become current.
     */
    public void setCurrentEditorTab(FileEditorTab tab) {
        this.setSelectedComponent(tab);
    }

    /**
     * Get the editor tab corresponding to the given pathname, if it is open.
     *
     * @param pathname Pathname to search for.
     * @return The tab with the given pathname, or null if no such tab was found.
     */
    public FileEditorTab getEditorTab(String pathname) {
        for (int tabIndex = 0; tabIndex < this.getTabCount(); tabIndex++) {
            FileEditorTab tab = (FileEditorTab) this.getComponentAt(tabIndex);
            if (tab.getPathname().equals(pathname)) {
                return tab;
            }
        }
        return null;
    }

    /**
     * If the given file is open in the tabbed pane, make it the
     * current tab.  If not opened, open it in a new tab and make
     * it the current tab.  If file is unable to be opened,
     * leave current tab as is.
     *
     * @param file File object for the desired file.
     * @return Tab for the specified file, or null if file is unable to be opened
     */
    public FileEditorTab getCurrentEditorTab(File file) {
        FileEditorTab tab = this.getEditorTab(file.getPath());
        if (tab != null) {
            if (tab != this.getCurrentEditorTab()) {
                this.setCurrentEditorTab(tab);
            }
            return tab;
        }
        // If no return yet, then file is not open.  Try to open it.
        if (this.openFile(file)) {
            return this.getCurrentEditorTab();
        }
        else {
            return null;
        }
    }

    /**
     * Get the list of currently open editor tabs by iterating
     * through the component list.
     *
     * @return The list of editor tabs.
     */
    public List<FileEditorTab> getEditorTabs() {
        List<FileEditorTab> editorTabs = new ArrayList<>(this.getTabCount());
        for (int tabIndex = 0; tabIndex < this.getTabCount(); tabIndex++) {
            editorTabs.add((FileEditorTab) this.getComponentAt(tabIndex));
        }
        return editorTabs;
    }

    /**
     * Determine whether files are currently being opened in new tabs.
     *
     * @return true if files are currently being opened, false otherwise.
     */
    public boolean isOpeningFiles() {
        return this.isOpeningFiles;
    }

    /**
     * Set whether calls to {@link #saveWorkspaceState()} have any effect.
     * This is useful for when the state of the workspace needs to change without
     * updating the saved workspace state (for example, when closing all files before
     * the application exits).
     *
     * @param enabled Whether to allow the workspace state to be saved.
     */
    public void setWorkspaceStateSavingEnabled(boolean enabled) {
        this.workspaceStateSavingEnabled = enabled;

        if (enabled) {
            this.saveWorkspaceState();
        }
    }

    /**
     * Save the state of the current workspace to permanent storage, which includes
     * the paths of the files that are open.
     * <p>
     * Most file-related operations trigger this automatically unless the
     * {@code workspaceStateSavingEnabled} flag is set to {@code false}.
     *
     * @see #setWorkspaceStateSavingEnabled(boolean)
     */
    public void saveWorkspaceState() {
        if (!this.workspaceStateSavingEnabled) {
            // Things are changing, but we don't want to save the workspace state.
            // Usually this happens when closing all files before exiting.
            return;
        }

        StringBuilder filesString = new StringBuilder();
        for (FileEditorTab tab : this.getEditorTabs()) {
            filesString.append(tab.getPathname()).append(';');
        }
        Application.getSettings().previouslyOpenFiles.set(filesString.toString());
    }

    /**
     * Load the state of the previous workspace from permanent storage, which includes
     * the paths of the files that were open.
     */
    public void loadWorkspaceState() {
        // Restore previous session
        String filesString = Application.getSettings().previouslyOpenFiles.get();
        File[] files = Arrays.stream(filesString.split(";"))
            .map(String::trim)
            .map(File::new)
            .toArray(File[]::new);
        this.openFiles(files);
    }

    /**
     * Carries out all necessary operations to implement
     * the New operation from the File menu.
     */
    public void newFile() {
        FileEditorTab fileEditorTab = new FileEditorTab(gui, this);
        fileEditorTab.setSourceCode("", true);
        fileEditorTab.setShowLineNumbersEnabled(true);
        fileEditorTab.setFileStatus(FileStatus.NEW_NOT_EDITED);
        String name = editor.getNextDefaultFilename();
        fileEditorTab.setPathname(name);
        this.addTab(name, fileEditorTab);

        RegisterFile.resetRegisters();
        VenusUI.setReset(true);
        gui.setProgramStatus(ProgramStatus.NOT_ASSEMBLED);
        gui.getMainPane().getExecuteTab().clear();

        gui.getMainPane().setSelectedComponent(this);
        fileEditorTab.displayCaretPosition(new Point(1, 1));
        this.setSelectedComponent(fileEditorTab);
        updateTitleAndMenuState(fileEditorTab);
        fileEditorTab.requestTextAreaFocus();
    }

    /**
     * Launch a file chooser for the user to select one or more files,
     * attempting to open them in new editor tabs afterward.
     *
     * @return The list of files which could not be opened due to an error. (Can be empty.)
     */
    public ArrayList<File> openFiles() {
        return new FileOpener().openFiles();
    }

    /**
     * Open the specified file in a new editor tab, switching focus to that tab.
     *
     * @return False if the file could not be opened due to an error, true otherwise.
     */
    public boolean openFile(File file) {
        return openFiles(new File[] { file }).isEmpty();
    }

    /**
     * Open the specified files in new editor tabs, switching focus to
     * the first newly opened tab.
     *
     * @return The list of files which could not be opened due to an error.
     *     (Can be empty.)
     */
    public ArrayList<File> openFiles(File[] files) {
        return new FileOpener().openFiles(files);
    }

    /**
     * Carries out all necessary operations to implement
     * the Close operation from the File menu.  May return
     * false, for instance when file has unsaved changes
     * and user selects Cancel from the warning dialog.
     *
     * @return true if file was closed, false otherwise.
     */
    public boolean closeCurrentFile() {
        FileEditorTab fileEditorTab = getCurrentEditorTab();
        if (fileEditorTab == null) {
            return true;
        }
        else if (editsSavedOrAbandoned()) {
            this.remove(fileEditorTab);
            gui.getMainPane().getExecuteTab().clear();
            gui.getMainPane().setSelectedComponent(this);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Carries out all necessary operations to implement
     * the Close All operation from the File menu.
     *
     * @return true if files closed, false otherwise.
     */
    public boolean closeAllFiles() {
        boolean closedAll = true;

        int tabCount = this.getTabCount();
        if (tabCount > 0) {
            this.gui.getMainPane().getExecuteTab().clear();
            this.gui.getMainPane().setSelectedComponent(this);

            List<FileEditorTab> tabs = this.getEditorTabs();

            if (tabs.stream().anyMatch(FileEditorTab::hasUnsavedEdits)) {
                switch (this.showUnsavedEditsDialog("one or more files")) {
                    case JOptionPane.YES_OPTION -> {
                        for (FileEditorTab tab : tabs) {
                            if (tab.hasUnsavedEdits()) {
                                this.setSelectedComponent(tab);
                                boolean saved = this.saveCurrentFile();
                                if (saved) {
                                    this.remove(tab);
                                }
                                else {
                                    closedAll = false;
                                }
                            }
                            else {
                                this.remove(tab);
                            }
                        }
                    }
                    case JOptionPane.NO_OPTION -> {
                        for (FileEditorTab tab : tabs) {
                            this.remove(tab);
                        }
                    }
                    default -> {
                        closedAll = false;
                    }
                }
            }
            else {
                for (FileEditorTab tab : tabs) {
                    this.remove(tab);
                }
            }
        }

        this.saveWorkspaceState();
        return closedAll;
    }

    /**
     * Saves file under existing name.  If no name, will invoke Save As.
     *
     * @return true if the file was actually saved.
     */
    public boolean saveCurrentFile() {
        FileEditorTab fileEditorTab = getCurrentEditorTab();
        if (this.saveFile(fileEditorTab)) {
            fileEditorTab.setFileStatus(FileStatus.NOT_EDITED);
            updateTitleAndMenuState(fileEditorTab);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Save file associated with specified editor tab.
     *
     * @param tab The tab for the file to save.
     * @return true if save operation worked, false otherwise.
     */
    private boolean saveFile(FileEditorTab tab) {
        if (tab == null) {
            return false;
        }
        else if (tab.isNew()) {
            File file = this.saveAsFile(tab);
            if (file != null) {
                tab.setPathname(file.getPath());
            }
            return file != null;
        }
        try {
            File file = new File(tab.getPathname());
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(tab.getSource(), 0, tab.getSource().length());
            writer.close();
            return true;
        }
        catch (IOException exception) {
            JOptionPane.showMessageDialog(
                null,
                "Save operation could not be completed due to an error:\n" + exception,
                "Save Operation Failed",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    /**
     * Pops up a dialog box to do the "Save As" operation.
     * The user will be asked to confirm before any file is overwritten.
     *
     * @return true if the file was actually saved, false if canceled.
     */
    public boolean saveAsCurrentFile() {
        FileEditorTab fileEditorTab = getCurrentEditorTab();
        File file = saveAsFile(fileEditorTab);
        if (file != null) {
            editor.setCurrentSaveDirectory(file.getParent());
            fileEditorTab.setPathname(file.getPath());
            fileEditorTab.setFileStatus(FileStatus.NOT_EDITED);
            updateTitleAndMenuState(fileEditorTab);
            return true;
        }
        return false;
    }

    /**
     * Pops up a dialog box to do the "Save As" operation for the given edit pane.
     * The user will be asked to confirm before any file is overwritten.
     *
     * @return The file object if the file was actually saved, null if canceled.
     */
    private File saveAsFile(FileEditorTab fileEditorTab) {
        if (fileEditorTab == null) {
            return null;
        }

        File file;
        JFileChooser saveDialog;
        while (true) {
            // Set Save As dialog directory in a logical way.  If file in
            // edit pane had been previously saved, default to its directory.
            // If a new file, default to current save directory.
            // DPS 13-July-2011
            if (fileEditorTab.isNew()) {
                saveDialog = new JFileChooser(editor.getCurrentSaveDirectory());
            }
            else {
                saveDialog = new JFileChooser(new File(fileEditorTab.getPathname()).getParent());
            }
            String paneFile = fileEditorTab.getFilename();
            if (paneFile != null) {
                saveDialog.setSelectedFile(new File(paneFile));
            }
            // end of 13-July-2011 code.
            saveDialog.setDialogTitle("Save As");

            int decision = saveDialog.showSaveDialog(gui);
            if (decision != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            file = saveDialog.getSelectedFile();
            if (file.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(gui, "File \"" + file.getName() + "\" already exists.  Do you wish to overwrite it?", "Overwrite existing file?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                switch (overwrite) {
                    case JOptionPane.YES_OPTION -> {
                        // Pass through to the break statement below
                    }
                    case JOptionPane.NO_OPTION -> {
                        continue;
                    }
                    case JOptionPane.CANCEL_OPTION -> {
                        return null;
                    }
                    default -> {
                        // should never occur
                        return null;
                    }
                }
            }
            // At this point, either the file with the selected name does not exist
            // or the user wanted to overwrite it, so go ahead and save it!
            break;
        }

        try {
            BufferedWriter outFileStream = new BufferedWriter(new FileWriter(file));
            outFileStream.write(fileEditorTab.getSource(), 0, fileEditorTab.getSource().length());
            outFileStream.close();
        }
        catch (IOException exception) {
            JOptionPane.showMessageDialog(null, "Save As operation could not be completed due to an error:\n" + exception, "Save As Operation Failed", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return file;
    }

    /**
     * Saves all files currently open in the editor.
     *
     * @return true if operation succeeded, false otherwise.
     */
    public boolean saveAllFiles() {
        if (this.getTabCount() <= 0) {
            return false;
        }

        for (FileEditorTab tab : this.getEditorTabs()) {
            if (tab.hasUnsavedEdits()) {
                if (this.saveFile(tab)) {
                    tab.setFileStatus(FileStatus.NOT_EDITED);
                    this.updateTitle(tab);
                }
                else {
                    return false;
                }
            }
        }

        FileEditorTab tab = this.getCurrentEditorTab();
        tab.setFileStatus(FileStatus.NOT_EDITED);
        this.updateTitleAndMenuState(tab);

        return true;
    }

    /**
     * Remove an tab and update the menu status.
     *
     * @param tab The tab to remove.
     */
    public void remove(FileEditorTab tab) {
        super.remove(tab);
        tab = this.getCurrentEditorTab(); // Is now next tab or null
        if (tab == null) {
            this.editor.setTitle("", FileStatus.NO_FILE);
            Application.getGUI().setMenuState(FileStatus.NO_FILE);
        }
        else {
            this.updateTitleAndMenuState(tab);
        }
        // When last file is closed, menu is unable to respond to mnemonics
        // and accelerators.  Let's have it request focus so it may do so.
        if (this.getTabCount() <= 0) {
            this.gui.haveMenuRequestFocus();
        }
    }

    /**
     * Handy little utility to update the title on the current tab and the frame title bar
     * and also to update the MARS menu state (controls which actions are enabled).
     */
    private void updateTitleAndMenuState(FileEditorTab fileEditorTab) {
        this.gui.setMenuState(fileEditorTab.getFileStatus());
        this.updateTitle(fileEditorTab);
    }

    /**
     * Handy little utility to update the title on the current tab and the frame title bar.
     *
     * @author DPS 9-Aug-2011
     */
    private void updateTitle(FileEditorTab fileEditorTab) {
        this.editor.setTitle(fileEditorTab.getFilename(), fileEditorTab.getFileStatus());
        this.saveWorkspaceState();
    }

    /**
     * Check whether file has unsaved edits and, if so, check with user about saving them.
     *
     * @return true if no unsaved edits or if user chooses to save them or not; false
     *     if there are unsaved edits and user cancels the operation.
     */
    public boolean editsSavedOrAbandoned() {
        FileEditorTab currentPane = getCurrentEditorTab();
        if (currentPane != null && currentPane.hasUnsavedEdits()) {
            return switch (showUnsavedEditsDialog(currentPane.getFilename())) {
                case JOptionPane.YES_OPTION -> saveCurrentFile();
                case JOptionPane.NO_OPTION -> true;
                case JOptionPane.CANCEL_OPTION -> false;
                default -> false; // Should never occur
            };
        }
        else {
            return true;
        }
    }

    private int showUnsavedEditsDialog(String name) {
        return JOptionPane.showConfirmDialog(
            gui,
            "There are unsaved changes in " + name + ". Would you like to save them?",
            "Save unsaved changes?",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
    }

    private class FileOpener {
        private File mostRecentlyOpenedFile;
        private final JFileChooser fileChooser;
        private int fileFilterCount;
        private final ArrayList<FileFilter> fileFilters;
        private final PropertyChangeListener listenForUserAddedFileFilter;

        public FileOpener() {
            mostRecentlyOpenedFile = null;
            fileChooser = new JFileChooser();
            listenForUserAddedFileFilter = new ChoosableFileFilterChangeListener();
            fileChooser.addPropertyChangeListener(listenForUserAddedFileFilter);
            fileChooser.setMultiSelectionEnabled(true);

            FileFilter asmFilter = FilenameFinder.getFileFilter(Application.FILE_EXTENSIONS, "Assembly Files", true);
            fileFilters = new ArrayList<>();
            // Note: add sequence is significant - last one added becomes default.
            fileFilters.add(fileChooser.getAcceptAllFileFilter());
            fileFilters.add(asmFilter);
            fileFilterCount = 0; // This will trigger fileChooser file filter load in next line
            setChoosableFileFilters();
            // Note: above note seems to not be true anymore, so force the last filter to be default.
            fileChooser.setFileFilter(asmFilter);
        }

        /**
         * Launch a file chooser for the user to select one or more files,
         * attempting to open them in new editor tabs afterward.
         *
         * @return The list of files which could not be opened due to an error. (Can be empty.)
         */
        public ArrayList<File> openFiles() {
            // The fileChooser's list may be rebuilt from the master ArrayList if a new filter
            // has been added by the user
            setChoosableFileFilters();
            // Set the current directory to the parent directory of the current file being edited,
            // or the working directory for MARS if that fails
            FileEditorTab currentTab = getCurrentEditorTab();
            File currentDirectory = null;
            if (currentTab != null) {
                currentDirectory = new File(currentTab.getPathname()).getParentFile();
            }
            if (currentDirectory == null) {
                currentDirectory = new File(System.getProperty("user.dir"));
            }
            fileChooser.setCurrentDirectory(currentDirectory);
            // Set default to previous file opened, if any.  This is useful in conjunction
            // with option to assemble file automatically upon opening.  File likely to have
            // been edited externally.
            if (Application.getSettings().assembleOnOpenEnabled.get() && mostRecentlyOpenedFile != null) {
                fileChooser.setSelectedFile(mostRecentlyOpenedFile);
            }

            if (fileChooser.showOpenDialog(gui) == JFileChooser.APPROVE_OPTION) {
                ArrayList<File> unopenedFiles = openFiles(fileChooser.getSelectedFiles());
                if (!unopenedFiles.isEmpty()) {
                    return unopenedFiles;
                }

                if (Application.getSettings().assembleOnOpenEnabled.get()) {
                    // Send this file right through to the assembler by firing Run->Assemble
                    gui.getRunAssembleAction().actionPerformed(null);
                }
            }

            return new ArrayList<>();
        }

        /**
         * Open the specified files in new editor tabs, switching focus to
         * the first newly opened tab.
         *
         * @return The list of files which could not be opened due to an error. (Can be empty.)
         */
        public ArrayList<File> openFiles(File[] files) {
            ArrayList<File> unopenedFiles = new ArrayList<>();
            FileEditorTab firstTabOpened = null;
            isOpeningFiles = true; // DPS 9-Aug-2011

            for (File file : files) {
                try {
                    file = file.getCanonicalFile();
                }
                catch (IOException exception) {
                    // Ignore, the file will stay unchanged
                }
                String filePath = file.getPath();
                // If this file is currently already open, then simply select its tab
                FileEditorTab fileEditorTab = getEditorTab(filePath);
                if (fileEditorTab != null) {
                    // Don't bother reopening the file since it's already open
                    continue;
                }
                fileEditorTab = new FileEditorTab(gui, EditTab.this);
                fileEditorTab.setPathname(filePath);
                if (file.canRead()) {
                    Application.program = new Program();
                    try {
                        Application.program.readSource(filePath);
                    }
                    catch (ProcessingException exception) {
                        unopenedFiles.add(file);
                        continue;
                    }
                    // DPS 1 Nov 2006.  Defined a StringBuffer to receive all file contents,
                    // one line at a time, before adding to the Edit pane with one setText.
                    // StringBuffer is preallocated to full file length to eliminate dynamic
                    // expansion as lines are added to it. Previously, each line was appended
                    // to the Edit pane as it was read, way slower due to dynamic string alloc.
                    StringBuilder fileContents = new StringBuilder((int) file.length());
                    int lineNumber = 1;
                    String line = Application.program.getSourceLine(lineNumber++);
                    while (line != null) {
                        fileContents.append(line).append('\n');
                        line = Application.program.getSourceLine(lineNumber++);
                    }
                    fileEditorTab.setSourceCode(fileContents.toString(), true);
                    // The above operation generates an undoable edit by setting the initial
                    // text area contents. That should not be seen as undoable by the Undo
                    // action, so let's get rid of it.
                    fileEditorTab.discardAllUndoableEdits();
                    fileEditorTab.setShowLineNumbersEnabled(true);
                    fileEditorTab.setFileStatus(FileStatus.NOT_EDITED);

                    addTab(fileEditorTab.getFilename(), null, fileEditorTab, fileEditorTab.getPathname());

                    if (firstTabOpened == null) {
                        firstTabOpened = fileEditorTab;
                    }
                }
            }

            isOpeningFiles = false;
            gui.getMainPane().setSelectedComponent(EditTab.this);
            gui.setProgramStatus(ProgramStatus.NOT_ASSEMBLED);

            if (firstTabOpened != null) {
                // At least one file was opened successfully
                // Treat the first opened tab as the "primary" one in the group
                setSelectedComponent(firstTabOpened);
                // If assemble-all, then allow opening of any file w/o invalidating assembly
                // DPS 9-Aug-2011
                if (!Application.getSettings().assembleAllEnabled.get()) {
                    updateTitleAndMenuState(firstTabOpened);
                    gui.getMainPane().getExecuteTab().clear();
                }
                firstTabOpened.requestTextAreaFocus();
                mostRecentlyOpenedFile = new File(firstTabOpened.getPathname());
            }

            // Save the updated workspace with the newly opened files
            saveWorkspaceState();

            return unopenedFiles;
        }

        // Private method to generate the file chooser's list of choosable file filters.
        // It is called when the file chooser is created, and called again each time the Open
        // dialog is activated.  We do this because the user may have added a new filter
        // during the previous dialog.  This can be done by entering e.g. *.txt in the file
        // name text field.  Java is funny, however, in that if the user does this then
        // cancels the dialog, the new filter will remain in the list BUT if the user does
        // this then ACCEPTS the dialog, the new filter will NOT remain in the list.  However
        // the act of entering it causes a property change event to occur, and we have a
        // handler that will add the new filter to our internal filter list and "restore" it
        // the next time this method is called.  Strangely, if the user then similarly
        // adds yet another new filter, the new one becomes simply a description change
        // to the previous one, the previous object is modified AND NO PROPERTY CHANGE EVENT
        // IS FIRED!  I could obviously deal with this situation if I wanted to, but enough
        // is enough.  The limit will be one alternative filter at a time.
        // DPS... 9 July 2008
        private void setChoosableFileFilters() {
            // See if a new filter has been added to the master list.  If so,
            // regenerate the fileChooser list from the master list.
            if (fileFilterCount < fileFilters.size() || fileFilters.size() != fileChooser.getChoosableFileFilters().length) {
                fileFilterCount = fileFilters.size();
                // First, "deactivate" the listener, because our addChoosableFileFilter
                // calls would otherwise activate it!  We want it to be triggered only
                // by MARS user action.
                boolean activeListener = false;
                if (fileChooser.getPropertyChangeListeners().length > 0) {
                    fileChooser.removePropertyChangeListener(listenForUserAddedFileFilter);
                    activeListener = true; // We'll note this for re-activation later
                }
                // Clear out the list and populate from our own ArrayList
                // Last one added becomes the default
                fileChooser.resetChoosableFileFilters();
                for (FileFilter fileFilter : fileFilters) {
                    fileChooser.addChoosableFileFilter(fileFilter);
                }
                // Restore listener
                if (activeListener) {
                    fileChooser.addPropertyChangeListener(listenForUserAddedFileFilter);
                }
            }
        }

        /**
         * Private inner class for special property change listener.
         * If user adds a file filter, e.g. by typing *.txt into the file text field then pressing
         * Enter, then it is automatically added to the array of choosable file filters.  BUT, unless you
         * Cancel out of the Open dialog, it is then REMOVED from the list automatically also. Here
         * we will achieve a sort of persistence at least through the current activation of MARS.
         *
         * @author DPS 9 July 2008
         */
        private class ChoosableFileFilterChangeListener implements PropertyChangeListener {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY.equals(event.getPropertyName())) {
                    FileFilter[] newFilters = (FileFilter[]) event.getNewValue();
                    if (newFilters.length > fileFilters.size()) {
                        // new filter added, so add to end of master list.
                        fileFilters.add(newFilters[newFilters.length - 1]);
                    }
                }
            }
        }
    }
}