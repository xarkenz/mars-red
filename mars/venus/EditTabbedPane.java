package mars.venus;

import mars.Globals;
import mars.Program;
import mars.ProcessingException;
import mars.mips.hardware.RegisterFile;
import mars.util.FilenameFinder;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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
 * Tabbed pane for the editor.  Each of its tabs represents an open file.
 *
 * @author Pete Sanderson
 */
public class EditTabbedPane extends JTabbedPane {
    private final VenusUI gui;
    private final MainPane mainPane;
    private final Editor editor;
    private boolean workspaceStateSavingEnabled;

    /**
     * Constructor for the EditTabbedPane class.
     */
    public EditTabbedPane(VenusUI gui, Editor editor, MainPane mainPane) {
        super();
        this.gui = gui;
        this.mainPane = mainPane;
        this.editor = editor;
        this.workspaceStateSavingEnabled = true;
        this.editor.setEditTabbedPane(this);
        this.addChangeListener(event -> {
            EditPane editPane = (EditPane) getSelectedComponent();
            if (editPane != null) {
                // New IF statement to permit free traversal of edit panes w/o invalidating
                // assembly if assemble-all is selected.  DPS 9-Aug-2011
                if (Globals.getSettings().assembleAllEnabled.get()) {
                    this.updateTitles(editPane);
                }
                else {
                    this.updateTitlesAndMenuState(editPane);
                    this.mainPane.getExecutePane().clearPane();
                }
                editPane.tellEditingComponentToRequestFocusInWindow();
            }
        });
    }

    /**
     * The current EditPane representing a file.  Returns null if
     * no files open.
     *
     * @return the current editor pane
     */
    public EditPane getCurrentEditTab() {
        return (EditPane) this.getSelectedComponent();
    }

    /**
     * Select the specified EditPane to be the current tab.
     *
     * @param editPane The EditPane tab to become current.
     */
    public void setCurrentEditTab(EditPane editPane) {
        this.setSelectedComponent(editPane);
    }

    /**
     * If the given file is open in the tabbed pane, make it the
     * current tab.  If not opened, open it in a new tab and make
     * it the current tab.  If file is unable to be opened,
     * leave current tab as is.
     *
     * @param file File object for the desired file.
     * @return EditPane for the specified file, or null if file is unable to be opened in an EditPane
     */
    public EditPane getCurrentEditTabForFile(File file) {
        EditPane tab = getEditPaneForFile(file.getPath());
        if (tab != null) {
            if (tab != getCurrentEditTab()) {
                setCurrentEditTab(tab);
            }
            return tab;
        }
        // If no return yet, then file is not open.  Try to open it.
        if (openFile(file)) {
            return getCurrentEditTab();
        }
        else {
            return null;
        }
    }

    public void setWorkspaceStateSavingEnabled(boolean enabled) {
        workspaceStateSavingEnabled = enabled;

        if (enabled) {
            saveWorkspaceState();
        }
    }

    public void saveWorkspaceState() {
        if (!workspaceStateSavingEnabled) {
            // Things are changing, but we don't want to save the workspace state.
            // Usually this happens when closing all files before exiting.
            return;
        }

        StringBuilder filesString = new StringBuilder();
        for (int index = 0; index < this.getTabCount(); index++) {
            EditPane tab = (EditPane) this.getComponentAt(index);
            filesString.append(tab.getPathname()).append(';');
        }
        Globals.getSettings().previouslyOpenFiles.set(filesString.toString());
    }

    public void loadWorkspaceState() {
        // Restore previous session
        String filesString = Globals.getSettings().previouslyOpenFiles.get();
        File[] files = Arrays.stream(filesString.split(";"))
            .map(String::trim)
            .map(File::new)
            .toArray(File[]::new);
        openFiles(files);
    }

    /**
     * Carries out all necessary operations to implement
     * the New operation from the File menu.
     */
    public void newFile() {
        EditPane editPane = new EditPane(this.gui);
        editPane.setSourceCode("", true);
        editPane.setShowLineNumbersEnabled(true);
        editPane.setFileStatus(FileStatus.NEW_NOT_EDITED);
        String name = editor.getNextDefaultFilename();
        editPane.setPathname(name);
        this.addTab(name, editPane);

        FileStatus.reset();
        FileStatus.setName(name);
        FileStatus.set(FileStatus.NEW_NOT_EDITED);

        RegisterFile.resetRegisters();
        VenusUI.setReset(true);
        mainPane.getExecutePane().clearPane();
        mainPane.setSelectedComponent(this);
        editPane.displayCaretPosition(new Point(1, 1));
        this.setSelectedComponent(editPane);
        updateTitlesAndMenuState(editPane);
        editPane.tellEditingComponentToRequestFocusInWindow();
    }

    /**
     * Launch a file chooser for the user to select one or more files,
     * attempting to open them in new editor tabs afterward.
     *
     * @return The list of files which could not be opened due to an error.
     *     (Can be empty.)
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
        EditPane editPane = getCurrentEditTab();
        if (editPane == null) {
            return true;
        }
        else if (editsSavedOrAbandoned()) {
            this.remove(editPane);
            mainPane.getExecutePane().clearPane();
            mainPane.setSelectedComponent(this);
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

        int tabCount = getTabCount();
        if (tabCount > 0) {
            mainPane.getExecutePane().clearPane();
            mainPane.setSelectedComponent(this);

            boolean unsavedChanges = false;
            EditPane[] tabs = new EditPane[tabCount];
            for (int i = 0; i < tabs.length; i++) {
                tabs[i] = (EditPane) getComponentAt(i);
                if (tabs[i].hasUnsavedEdits()) {
                    unsavedChanges = true;
                }
            }

            if (unsavedChanges) {
                switch (showUnsavedEditsDialog("one or more files")) {
                    case JOptionPane.YES_OPTION:
                        for (EditPane tab : tabs) {
                            if (tab.hasUnsavedEdits()) {
                                setSelectedComponent(tab);
                                boolean saved = saveCurrentFile();
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
                        break;
                    case JOptionPane.NO_OPTION:
                        for (EditPane tab : tabs) {
                            this.remove(tab);
                        }
                        break;
                    case JOptionPane.CANCEL_OPTION:
                    default:
                        closedAll = false;
                        break;
                }
            }
            else {
                for (EditPane tab : tabs) {
                    this.remove(tab);
                }
            }
        }

        saveWorkspaceState();
        return closedAll;
    }

    /**
     * Saves file under existing name.  If no name, will invoke Save As.
     *
     * @return true if the file was actually saved.
     */
    public boolean saveCurrentFile() {
        EditPane editPane = getCurrentEditTab();
        if (saveFile(editPane)) {
            FileStatus.set(FileStatus.NOT_EDITED);
            editPane.setFileStatus(FileStatus.NOT_EDITED);
            updateTitlesAndMenuState(editPane);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Save file associated with specified edit pane.
     *
     * @param editPane The edit pane for the file to save.
     * @return true if save operation worked, false otherwise.
     */
    private boolean saveFile(EditPane editPane) {
        if (editPane == null) {
            return false;
        }
        else if (editPane.isNew()) {
            File file = saveAsFile(editPane);
            if (file != null) {
                editPane.setPathname(file.getPath());
            }
            return file != null;
        }
        try {
            File file = new File(editPane.getPathname());
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(editPane.getSource(), 0, editPane.getSource().length());
            writer.close();
            return true;
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Save operation could not be completed due to an error:\n" + e, "Save Operation Failed", JOptionPane.ERROR_MESSAGE);
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
        EditPane editPane = getCurrentEditTab();
        File file = saveAsFile(editPane);
        if (file != null) {
            FileStatus.setFile(file);
            FileStatus.setName(file.getPath());
            FileStatus.set(FileStatus.NOT_EDITED);
            editor.setCurrentSaveDirectory(file.getParent());
            editPane.setPathname(file.getPath());
            editPane.setFileStatus(FileStatus.NOT_EDITED);
            updateTitlesAndMenuState(editPane);
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
    private File saveAsFile(EditPane editPane) {
        File file = null;
        if (editPane != null) {
            JFileChooser saveDialog;
            while (true) {
                // Set Save As dialog directory in a logical way.  If file in
                // edit pane had been previously saved, default to its directory.
                // If a new file, default to current save directory.
                // DPS 13-July-2011
                if (editPane.isNew()) {
                    saveDialog = new JFileChooser(editor.getCurrentSaveDirectory());
                }
                else {
                    saveDialog = new JFileChooser(new File(editPane.getPathname()).getParent());
                }
                String paneFile = editPane.getFilename();
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
                outFileStream.write(editPane.getSource(), 0, editPane.getSource().length());
                outFileStream.close();
            }
            catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Save As operation could not be completed due to an error:\n" + e, "Save As Operation Failed", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        return file;
    }

    /**
     * Saves all files currently open in the editor.
     *
     * @return true if operation succeeded, false otherwise.
     */
    public boolean saveAllFiles() {
        boolean result = false;
        int tabCount = getTabCount();
        if (tabCount > 0) {
            result = true;
            EditPane[] tabs = new EditPane[tabCount];
            EditPane savedPane = getCurrentEditTab();
            for (int i = 0; i < tabCount; i++) {
                tabs[i] = (EditPane) getComponentAt(i);
                if (tabs[i].hasUnsavedEdits()) {
                    setCurrentEditTab(tabs[i]);
                    if (saveFile(tabs[i])) {
                        tabs[i].setFileStatus(FileStatus.NOT_EDITED);
                        updateTitles(tabs[i]);
                    }
                    else {
                        result = false;
                    }
                }
            }
            setCurrentEditTab(savedPane);
            if (result) {
                EditPane editPane = getCurrentEditTab();
                FileStatus.set(FileStatus.NOT_EDITED);
                editPane.setFileStatus(FileStatus.NOT_EDITED);
                updateTitlesAndMenuState(editPane);
            }
        }
        return result;
    }

    /**
     * Remove an edit pane and update the menu status.
     *
     * @param editPane The pane to remove.
     */
    public void remove(EditPane editPane) {
        super.remove(editPane);
        editPane = getCurrentEditTab(); // is now next tab or null
        if (editPane == null) {
            FileStatus.set(FileStatus.NO_FILE);
            this.editor.setTitle("", "", FileStatus.NO_FILE);
            Globals.getGUI().setMenuState(FileStatus.NO_FILE);
        }
        else {
            FileStatus.set(editPane.getFileStatus());
            updateTitlesAndMenuState(editPane);
        }
        // When last file is closed, menu is unable to respond to mnemonics
        // and accelerators.  Let's have it request focus so it may do so.
        if (getTabCount() == 0) {
            gui.haveMenuRequestFocus();
        }
    }

    /**
     * Handy little utility to update the title on the current tab and the frame title bar
     * and also to update the MARS menu state (controls which actions are enabled).
     */
    private void updateTitlesAndMenuState(EditPane editPane) {
        editPane.updateStaticFileStatus(); //  for legacy code that depends on the static FileStatus (pre 4.0)
        Globals.getGUI().setMenuState(editPane.getFileStatus());
        updateTitles(editPane);
    }

    /**
     * Handy little utility to update the title on the current tab and the frame title bar.
     * DPS 9-Aug-2011
     */
    private void updateTitles(EditPane editPane) {
        editor.setTitle(editPane.getPathname(), editPane.getFilename(), editPane.getFileStatus());
        saveWorkspaceState();
    }

    /**
     * If there is an EditPane for the given file pathname, return it else return null.
     *
     * @param pathname Pathname for desired file
     * @return the EditPane for this file if it is open in the editor, or null if not.
     */
    public EditPane getEditPaneForFile(String pathname) {
        for (int i = 0; i < getTabCount(); i++) {
            EditPane pane = (EditPane) getComponentAt(i);
            if (pane.getPathname().equals(pathname)) {
                return pane;
            }
        }
        return null;
    }

    /**
     * Check whether file has unsaved edits and, if so, check with user about saving them.
     *
     * @return true if no unsaved edits or if user chooses to save them or not; false
     *     if there are unsaved edits and user cancels the operation.
     */
    public boolean editsSavedOrAbandoned() {
        EditPane currentPane = getCurrentEditTab();
        if (currentPane != null && currentPane.hasUnsavedEdits()) {
            return switch (showUnsavedEditsDialog(currentPane.getFilename())) {
                case JOptionPane.YES_OPTION -> saveCurrentFile();
                case JOptionPane.NO_OPTION -> true;
                case JOptionPane.CANCEL_OPTION -> false;
                default -> false; // should never occur
            };
        }
        else {
            return true;
        }
    }

    private int showUnsavedEditsDialog(String name) {
        return JOptionPane.showConfirmDialog(gui, "There are unsaved changes in " + name + ". Would you like to save them?", "Save unsaved changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    }

    private class FileOpener {
        private File mostRecentlyOpenedFile;
        private final JFileChooser fileChooser;
        private int fileFilterCount;
        private final ArrayList<FileFilter> fileFilters;
        private final PropertyChangeListener listenForUserAddedFileFilter;

        public FileOpener() {
            this.mostRecentlyOpenedFile = null;
            this.fileChooser = new JFileChooser();
            this.listenForUserAddedFileFilter = new ChoosableFileFilterChangeListener();
            this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);
            this.fileChooser.setMultiSelectionEnabled(true);

            FileFilter asmFilter = FilenameFinder.getFileFilter(Globals.FILE_EXTENSIONS, "Assembly Files", true);
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
         * @return The list of files which could not be opened due to an error.
         *     (Can be empty.)
         */
        public ArrayList<File> openFiles() {
            // The fileChooser's list may be rebuilt from the master ArrayList if a new filter
            // has been added by the user.
            setChoosableFileFilters();
            // Get name of file to be opened and load contents into text editing area.
            fileChooser.setCurrentDirectory(new File(editor.getCurrentOpenDirectory()));
            // Set default to previous file opened, if any.  This is useful in conjunction
            // with option to assemble file automatically upon opening.  File likely to have
            // been edited externally.
            if (Globals.getSettings().assembleOnOpenEnabled.get() && mostRecentlyOpenedFile != null) {
                fileChooser.setSelectedFile(mostRecentlyOpenedFile);
            }

            if (fileChooser.showOpenDialog(gui) == JFileChooser.APPROVE_OPTION) {
                editor.setCurrentOpenDirectory(fileChooser.getSelectedFile().getParent());
                ArrayList<File> unopenedFiles = openFiles(fileChooser.getSelectedFiles());
                if (!unopenedFiles.isEmpty()) {
                    return unopenedFiles;
                }

                // Possibly send this file right through to the assembler by firing Run->Assemble's
                // actionPerformed() method.
                if (Globals.getSettings().assembleOnOpenEnabled.get()) {
                    gui.getRunAssembleAction().actionPerformed(null);
                }
            }

            return new ArrayList<>();
        }

        /**
         * Open the specified files in new editor tabs, switching focus to
         * the first newly opened tab.
         *
         * @return The list of files which could not be opened due to an error.
         *     (Can be empty.)
         */
        public ArrayList<File> openFiles(File[] files) {
            ArrayList<File> unopenedFiles = new ArrayList<>();
            EditPane firstTabOpened = null;
            FileStatus.set(FileStatus.OPENING); // DPS 9-Aug-2011

            for (File file : files) {
                try {
                    file = file.getCanonicalFile();
                }
                catch (IOException ioe) {
                    // Ignore, the file will stay unchanged
                }
                String filePath = file.getPath();
                // If this file is currently already open, then simply select its tab
                EditPane editPane = getEditPaneForFile(filePath);
                if (editPane != null) {
                    // Don't bother reopening the file since it's already open
                    continue;
                }
                editPane = new EditPane(gui);
                editPane.setPathname(filePath);
                FileStatus.setName(filePath);
                FileStatus.setFile(file);
                if (file.canRead()) {
                    Globals.program = new Program();
                    try {
                        Globals.program.readSource(filePath);
                    }
                    catch (ProcessingException e) {
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
                    String line = Globals.program.getSourceLine(lineNumber++);
                    while (line != null) {
                        fileContents.append(line).append('\n');
                        line = Globals.program.getSourceLine(lineNumber++);
                    }
                    editPane.setSourceCode(fileContents.toString(), true);
                    // The above operation generates an undoable edit by setting the initial
                    // text area contents. That should not be seen as undoable by the Undo
                    // action, so let's get rid of it.
                    editPane.discardAllUndoableEdits();
                    editPane.setShowLineNumbersEnabled(true);
                    editPane.setFileStatus(FileStatus.NOT_EDITED);

                    addTab(editPane.getFilename(), null, editPane, editPane.getPathname());

                    if (firstTabOpened == null) {
                        firstTabOpened = editPane;
                    }
                }
            }

            mainPane.setSelectedComponent(EditTabbedPane.this);
            FileStatus.set(FileStatus.NOT_EDITED);

            if (firstTabOpened != null) {
                // At least one file was opened successfully
                // Treat the first opened tab as the "primary" one in the group
                setSelectedComponent(firstTabOpened);
                // If assemble-all, then allow opening of any file w/o invalidating assembly.
                // DPS 9-Aug-2011
                if (!Globals.getSettings().assembleAllEnabled.get()) {
                    updateTitlesAndMenuState(firstTabOpened);
                    mainPane.getExecutePane().clearPane();
                }
                firstTabOpened.tellEditingComponentToRequestFocusInWindow();
                mostRecentlyOpenedFile = new File(firstTabOpened.getPathname());
            }

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
                    activeListener = true;  // we'll note this, for re-activation later
                }
                // clear out the list and populate from our own ArrayList.
                // Last one added becomes the default.
                fileChooser.resetChoosableFileFilters();
                for (FileFilter fileFilter : fileFilters) {
                    fileChooser.addChoosableFileFilter(fileFilter);
                }
                // Restore listener.
                if (activeListener) {
                    fileChooser.addPropertyChangeListener(listenForUserAddedFileFilter);
                }
            }
        }

        //  Private inner class for special property change listener.  DPS 9 July 2008.
        //  If user adds a file filter, e.g. by typing *.txt into the file text field then pressing
        //  Enter, then it is automatically added to the array of choosable file filters.  BUT, unless you
        //  Cancel out of the Open dialog, it is then REMOVED from the list automatically also. Here
        //  we will achieve a sort of persistence at least through the current activation of MARS.
        private class ChoosableFileFilterChangeListener implements PropertyChangeListener {
            @Override
            public void propertyChange(java.beans.PropertyChangeEvent e) {
                if (JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY.equals(e.getPropertyName())) {
                    FileFilter[] newFilters = (FileFilter[]) e.getNewValue();
                    if (newFilters.length > fileFilters.size()) {
                        // new filter added, so add to end of master list.
                        fileFilters.add(newFilters[newFilters.length - 1]);
                    }
                }
            }
        }
    }
}