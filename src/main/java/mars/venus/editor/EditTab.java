package mars.venus.editor;

import mars.Application;
import mars.venus.DynamicTabbedPane;
import mars.util.FilenameFinder;
import mars.venus.VenusUI;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
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
public class EditTab extends DynamicTabbedPane {
    private final VenusUI gui;
    private final Editor editor;
    private File mostRecentlyOpenedFile;

    /**
     * Create and initialize the Edit tab with no open files.
     *
     * @param gui    The parent GUI instance.
     * @param editor The editor instance, which will be used to manage the text editors.
     */
    public EditTab(VenusUI gui, Editor editor) {
        super();
        this.gui = gui;
        this.editor = editor;
        this.mostRecentlyOpenedFile = null;

        this.addChangeListener(event -> {
            FileEditorTab currentTab = this.getCurrentEditorTab();
            this.updateTitleAndMenuState(currentTab);
            if (currentTab != null) {
                currentTab.requestTextAreaFocus();
            }

            this.gui.saveWorkspaceState();
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
     * Get the editor tab corresponding to the given file, if the file is open.
     *
     * @param file File object for the desired tab.
     * @return The tab for the given file, or null if no such tab was found.
     */
    public FileEditorTab getEditorTab(File file) {
        for (FileEditorTab tab : this.getEditorTabs()) {
            if (tab.getFile().equals(file)) {
                return tab;
            }
        }
        return null;
    }

    /**
     * If the given file is open, make it the current tab.
     * If not opened, open it in a new tab and make it the current tab.
     * If file is unable to be opened, leave current tab as is.
     *
     * @param file File object for the desired tab.
     * @return The tab for the given file, or null if file is unable to be opened.
     */
    public FileEditorTab getCurrentEditorTab(File file) {
        FileEditorTab tab = this.getEditorTab(file);
        if (tab != null) {
            this.setCurrentEditorTab(tab);
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
     * Get the list of currently open editor tabs by iterating through the component list.
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
     * Carries out all necessary operations to implement
     * the New operation from the File menu.
     */
    public void newFile() {
        FileEditorTab newTab = new FileEditorTab(this.gui);
        newTab.setSourceCode("", true);
        newTab.setFileStatus(FileStatus.NEW_NOT_EDITED);
        String name = this.editor.getNextDefaultFilename();
        // This is kind of goofy, but it's fine since a File doesn't have to represent anything.
        newTab.setFile(new File(name));
        this.addTab(name, null, newTab, name);

        this.gui.getMainPane().setSelectedComponent(this);
        newTab.displayCaretPosition(new Point(1, 1));
        this.setSelectedComponent(newTab);
        this.updateTitleAndMenuState(newTab);
        newTab.requestTextAreaFocus();
    }

    /**
     * Launch a file chooser for the user to select one or more files,
     * attempting to open them in new editor tabs afterward.
     *
     * @return The list of files which could not be opened due to an error. (Can be empty.)
     */
    public List<File> openFiles() {
        List<File> unopenedFiles = new FileOpener().openFiles();
        this.gui.getMainPane().setSelectedComponent(this);
        return unopenedFiles;
    }

    /**
     * Open the specified file in a new editor tab, switching focus to that tab.
     *
     * @return False if the file could not be opened due to an error, true otherwise.
     */
    public boolean openFile(File file) {
        return this.openFiles(List.of(file)).isEmpty();
    }

    /**
     * Open the specified files in new editor tabs, switching focus to the first newly opened tab.
     * If any files cannot be opened, an error dialog will be displayed to the user indicating which
     * files failed to open.
     *
     * @return The list of files which could not be opened due to an error. (Can be empty.)
     */
    public List<File> openFiles(List<File> files) {
        List<File> unopenedFiles = new ArrayList<>();
        FileEditorTab firstTabOpened = null;
        // All new tabs will be inserted following the current tab
        int tabIndex = EditTab.this.getSelectedIndex() + 1;

        EditTab.this.gui.setWorkspaceStateSavingEnabled(false); // DPS 9-Aug-2011

        for (File file : files) {
            try {
                file = file.getCanonicalFile();
            }
            catch (IOException exception) {
                // Ignore, the file will stay unchanged
            }

            // Don't bother reopening the file if it's already open
            FileEditorTab existingTab = this.getEditorTab(file);
            if (existingTab != null) {
                if (firstTabOpened == null) {
                    firstTabOpened = existingTab;
                }
                continue;
            }

            FileEditorTab fileEditorTab = new FileEditorTab(this.gui);
            fileEditorTab.setFile(file);

            if (file.canRead()) {
                try {
                    fileEditorTab.setSourceCode(Files.readString(file.toPath()), true);
                }
                catch (IOException exception) {
                    unopenedFiles.add(file);
                    continue;
                }

                // The above operation generates an undoable edit by setting the initial
                // text area contents. That should not be seen as undoable by the Undo
                // action, so let's get rid of it.
                fileEditorTab.discardAllUndoableEdits();
                fileEditorTab.setFileStatus(FileStatus.NOT_EDITED);

                this.insertTab(file.getName(), null, fileEditorTab, file.getPath(), tabIndex++);

                if (firstTabOpened == null) {
                    firstTabOpened = fileEditorTab;
                }
            }
            else {
                unopenedFiles.add(file);
            }
        }

        if (firstTabOpened != null) {
            // At least one file was opened successfully
            // Treat the first opened tab as the "primary" one in the group
            this.setSelectedComponent(firstTabOpened);
            firstTabOpened.requestTextAreaFocus();
            this.mostRecentlyOpenedFile = firstTabOpened.getFile();
        }

        this.gui.getMainPane().setSelectedComponent(this);

        // Save the updated workspace with the newly opened files
        this.gui.setWorkspaceStateSavingEnabled(true);
        this.gui.saveWorkspaceState();

        if (!unopenedFiles.isEmpty()) {
            // Some files failed to open, so show an error dialog
            this.showOpenFileErrorDialog(unopenedFiles);
        }

        return unopenedFiles;
    }

    /**
     * Display an error dialog to the user indicating that one or more files could not be opened.
     *
     * @param unopenedFiles The list of files which could not be opened. (Should not be empty.) Usually obtained
     *                      from a call to {@link #openFiles(List) openFiles}.
     */
    public void showOpenFileErrorDialog(List<File> unopenedFiles) {
        StringBuilder message = new StringBuilder("<html>The following file");
        if (unopenedFiles.size() != 1) {
            message.append('s');
        }
        message.append(" could not be opened:<ul>");
        for (File file : unopenedFiles) {
            // The path name isn't sanitized, but it should be fine...?
            message.append("<li>").append(file).append("</li>");
        }
        message.append("</ul></html>");
        JOptionPane.showMessageDialog(this.gui, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Attempts to remove the tab at <code>index</code>. If the tab has unsaved changes, the user will be prompted
     * to save them. If they select cancel, the tab will <i>not</i> be removed. Otherwise, the tab is removed.
     * After a successful removal, {@link VenusUI#saveWorkspaceState()} is invoked.
     *
     * @param index The index of the tab to be removed.
     * @throws IndexOutOfBoundsException Thrown if the index is out of range
     *                                   (<code>index</code> &lt; 0 or <code>index</code> &ge; {@link #getTabCount()}).
     */
    @Override
    public void removeTabAt(int index) {
        if (this.resolveUnsavedChanges((FileEditorTab) this.getComponentAt(index))) {
            super.removeTabAt(index);
            this.gui.saveWorkspaceState();
        }
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
        return this.closeFile(this.getCurrentEditorTab());
    }

    /**
     * Carries out all necessary operations to implement
     * the Close operation from the File menu.  May return
     * false, for instance when file has unsaved changes
     * and user selects Cancel from the warning dialog.
     *
     * @param tab The tab for the file to close.
     * @return true if file was closed, false otherwise.
     */
    public boolean closeFile(FileEditorTab tab) {
        if (tab == null) {
            return true;
        }
        int index = this.indexOfComponent(tab);
        if (index < 0) {
            return true;
        }
        this.removeTabAt(index);
        return index >= this.getTabCount() || this.getComponentAt(index) != tab;
    }

    /**
     * Carries out all necessary operations to implement
     * the Close All operation from the File menu.
     *
     * @return true if files closed, false otherwise.
     */
    public boolean closeAllFiles() {
        // If the user cancels at any point, we want the tabs to be in their original state
        for (FileEditorTab tab : this.getEditorTabs()) {
            if (!this.resolveUnsavedChanges(tab)) {
                return false;
            }
        }

        // Manually close all files to avoid the override version of removeTabAt()
        this.setSelectedIndex(-1);
        for (int index = this.getTabCount() - 1; index >= 0; index--) {
            super.removeTabAt(index);
        }

        return true;
    }

    /**
     * Saves file under existing name.  If no name, will invoke Save As.
     *
     * @return true if the file was actually saved.
     */
    public boolean saveCurrentFile() {
        return this.saveFile(this.getCurrentEditorTab());
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
            File savedFile = this.saveAsFile(tab);
            if (savedFile != null) {
                this.gui.addRecentFile(savedFile);
                return true;
            }
            return false;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(tab.getFile()));
            writer.write(tab.getSource(), 0, tab.getSource().length());
            writer.close();
        }
        catch (IOException exception) {
            JOptionPane.showMessageDialog(
                null,
                "Failed to save due to an error:\n" + exception,
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        this.editor.setCurrentSaveDirectory(tab.getFile().getParent());

        tab.setFileStatus(FileStatus.NOT_EDITED);
        this.updateTitleAndMenuState(tab);

        return true;
    }

    /**
     * Pops up a dialog box to do the "Save As" operation.
     * The user will be asked to confirm before any file is overwritten.
     *
     * @return The file object if the file was actually saved, null if canceled.
     */
    public File saveAsCurrentFile() {
        return this.saveAsFile(this.getCurrentEditorTab());
    }

    /**
     * Pops up a dialog box to do the "Save As" operation for the given editor tab.
     * The user will be asked to confirm before any file is overwritten.
     *
     * @param tab The tab for the file to save (does nothing if null).
     * @return The file object if the file was actually saved, null if canceled.
     */
    private File saveAsFile(FileEditorTab tab) {
        if (tab == null) {
            return null;
        }

        // Set Save As dialog directory in a logical way.  If file in
        // edit pane had been previously saved, default to its directory.
        // If a new file, default to current save directory.
        // DPS 13-July-2011
        JFileChooser saveDialog;
        if (tab.isNew()) {
            saveDialog = new JFileChooser(this.editor.getCurrentSaveDirectory());
        }
        else {
            saveDialog = new JFileChooser(tab.getFile().getParent());
            saveDialog.setSelectedFile(tab.getFile());
        }
        // End of 13-July-2011 code.
        saveDialog.setDialogTitle("Save As");

        File file;
        while (true) {
            int decision = saveDialog.showSaveDialog(this.gui);
            if (decision != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            file = saveDialog.getSelectedFile();
            if (file.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(
                    this.gui,
                    "File \"" + file.getName() + "\" already exists.  Do you wish to overwrite it?",
                    "Save Conflict",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                switch (overwrite) {
                    case JOptionPane.YES_OPTION -> {
                        // Pass through to the break statement below
                    }
                    case JOptionPane.NO_OPTION -> {
                        continue;
                    }
                    default -> {
                        return null;
                    }
                }
            }
            // At this point, either the file with the selected name does not exist
            // or the user wanted to overwrite it, so go ahead and save it!
            break;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(tab.getSource(), 0, tab.getSource().length());
            writer.close();
        }
        catch (IOException exception) {
            JOptionPane.showMessageDialog(
                this.gui,
                "Failed to save due to an error: " + exception,
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return null;
        }

        this.editor.setCurrentSaveDirectory(file.getParent());
        tab.setFile(file);
        tab.setFileStatus(FileStatus.NOT_EDITED);
        this.updateTitleAndMenuState(tab);
        this.gui.saveWorkspaceState();

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
                    this.updateTitleAndMenuState(tab);
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
     * Update the title on a given tab and possibly the title of the window,
     * as well as the GUI's menu state (i.e. which actions are enabled) if applicable.
     * Should be invoked any time the tab's state might change in any way.
     *
     * @param tab The tab to update, or null if there are no tabs remaining.
     */
    public void updateTitleAndMenuState(FileEditorTab tab) {
        if (tab == null) {
            this.gui.setTitleContent(null);
            this.gui.setFileStatus(FileStatus.NO_FILE);
        }
        else {
            StringBuilder content = new StringBuilder();
            if (tab.getFileStatus().hasUnsavedEdits()) {
                // Add the prefix for unsaved changes
                content.append('*');
            }
            content.append(tab.getFile().getName());
            int tabIndex = this.indexOfComponent(tab);
            this.setTitleAt(tabIndex, content.toString());
            this.setToolTipTextAt(tabIndex, tab.getFile().getPath());

            if (tab == this.getCurrentEditorTab()) {
                this.gui.setTitleContent(content.toString());
                this.gui.setFileStatus(tab.getFileStatus());
            }
        }
    }

    /**
     * Check whether file has unsaved edits and, if so, check with user about saving them.
     *
     * @return true if no unsaved edits or if user chooses to save them or not; false
     *     if there are unsaved edits and user cancels the operation.
     */
    public boolean resolveUnsavedChanges(FileEditorTab tab) {
        if (tab != null && tab.hasUnsavedEdits()) {
            return switch (this.showUnsavedEditsDialog(tab.getFile().getName())) {
                case JOptionPane.YES_OPTION -> this.saveFile(tab);
                case JOptionPane.NO_OPTION -> true;
                default -> false;
            };
        }
        else {
            return true;
        }
    }

    private int showUnsavedEditsDialog(String name) {
        return JOptionPane.showConfirmDialog(
            this.gui,
            "There are unsaved changes in \"" + name + "\". Would you like to save them?",
            "Save unsaved changes?",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
    }

    private class FileOpener {
        private final JFileChooser fileChooser;
        private int fileFilterCount;
        private final ArrayList<FileFilter> fileFilters;
        private final PropertyChangeListener listenForUserAddedFileFilter;

        public FileOpener() {
            this.fileChooser = new JFileChooser();
            this.listenForUserAddedFileFilter = new ChoosableFileFilterChangeListener();
            this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);
            this.fileChooser.setMultiSelectionEnabled(true);

            FileFilter asmFilter = FilenameFinder.getFileFilter(Application.FILE_EXTENSIONS, "Assembly Files", true);
            this.fileFilters = new ArrayList<>();
            // Note: add sequence is significant - last one added becomes default.
            this.fileFilters.add(asmFilter);
            this.fileFilters.add(this.fileChooser.getAcceptAllFileFilter());
            this.fileFilterCount = 0; // This will trigger fileChooser file filter load in next line
            this.setChoosableFileFilters();
            // Note: above note seems to not be true anymore, so force the assembly filter to be default.
            this.fileChooser.setFileFilter(asmFilter);
        }

        /**
         * Launch a file chooser for the user to select one or more files,
         * attempting to open them in new editor tabs afterward.
         *
         * @return The list of files which could not be opened due to an error. (Can be empty.)
         */
        public List<File> openFiles() {
            // The fileChooser's list may be rebuilt from the master ArrayList if a new filter
            // has been added by the user
            this.setChoosableFileFilters();
            // Set the current directory to the parent directory of the current file being edited,
            // or the working directory for MARS if that fails
            FileEditorTab currentTab = EditTab.this.getCurrentEditorTab();
            File currentDirectory = null;
            if (currentTab != null) {
                currentDirectory = currentTab.getFile().getParentFile();
            }
            if (currentDirectory == null) {
                currentDirectory = new File(System.getProperty("user.dir"));
            }
            this.fileChooser.setCurrentDirectory(currentDirectory);
            // Set default to previous file opened, if any.  This is useful in conjunction
            // with option to assemble file automatically upon opening.  File likely to have
            // been edited externally.
            if (EditTab.this.gui.getSettings().assembleOnOpenEnabled.get() && EditTab.this.mostRecentlyOpenedFile != null) {
                this.fileChooser.setSelectedFile(EditTab.this.mostRecentlyOpenedFile);
            }

            if (this.fileChooser.showOpenDialog(EditTab.this.gui) == JFileChooser.APPROVE_OPTION) {
                List<File> unopenedFiles = EditTab.this.openFiles(List.of(this.fileChooser.getSelectedFiles()));

                // Since the user opened these files manually, add them to the recent files menu
                for (File file : this.fileChooser.getSelectedFiles()) {
                    if (!unopenedFiles.contains(file)) {
                        EditTab.this.gui.addRecentFile(file);
                    }
                }

                if (!unopenedFiles.isEmpty()) {
                    return unopenedFiles;
                }

                if (EditTab.this.gui.getSettings().assembleOnOpenEnabled.get()) {
                    // Send this file right through to the assembler by firing Run->Assemble
                    EditTab.this.gui.getRunAssembleAction().actionPerformed(null);
                }
            }

            return new ArrayList<>();
        }

        /**
         * Private method to generate the file chooser's list of choosable file filters.
         * It is called when the file chooser is created, and called again each time the Open
         * dialog is activated.  We do this because the user may have added a new filter
         * during the previous dialog.  This can be done by entering e.g. *.txt in the file
         * name text field.  Java is funny, however, in that if the user does this then
         * cancels the dialog, the new filter will remain in the list BUT if the user does
         * this then ACCEPTS the dialog, the new filter will NOT remain in the list.  However
         * the act of entering it causes a property change event to occur, and we have a
         * handler that will add the new filter to our internal filter list and "restore" it
         * the next time this method is called.  Strangely, if the user then similarly
         * adds yet another new filter, the new one becomes simply a description change
         * to the previous one, the previous object is modified AND NO PROPERTY CHANGE EVENT
         * IS FIRED!  I could obviously deal with this situation if I wanted to, but enough
         * is enough.  The limit will be one alternative filter at a time.
         *
         * @author DPS 9 July 2008
         */
        private void setChoosableFileFilters() {
            // See if a new filter has been added to the master list.  If so,
            // regenerate the fileChooser list from the master list.
            if (this.fileFilterCount < this.fileFilters.size() || this.fileFilters.size() != this.fileChooser.getChoosableFileFilters().length) {
                this.fileFilterCount = this.fileFilters.size();
                // First, "deactivate" the listener, because our addChoosableFileFilter
                // calls would otherwise activate it!  We want it to be triggered only
                // by MARS user action.
                boolean activeListener = false;
                if (this.fileChooser.getPropertyChangeListeners().length > 0) {
                    this.fileChooser.removePropertyChangeListener(this.listenForUserAddedFileFilter);
                    activeListener = true; // We'll note this for re-activation later
                }
                // Clear out the list and populate from our own ArrayList
                // Last one added becomes the default
                this.fileChooser.resetChoosableFileFilters();
                for (FileFilter fileFilter : this.fileFilters) {
                    this.fileChooser.addChoosableFileFilter(fileFilter);
                }
                // Restore listener
                if (activeListener) {
                    this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);
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
                    if (newFilters.length > FileOpener.this.fileFilters.size()) {
                        // New filter added, so add to end of master list
                        FileOpener.this.fileFilters.add(newFilters[newFilters.length - 1]);
                    }
                }
            }
        }
    }
}