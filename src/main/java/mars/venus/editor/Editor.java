package mars.venus.editor;

import mars.venus.VenusUI;

import java.io.File;
import java.util.ArrayList;
 
/*
Copyright (c) 2003-2007,  Pete Sanderson and Kenneth Vollmar

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
 * Manage the file being edited.
 * Currently only manages one file at a time, but can be expanded.
 */
public class Editor {
    public static final int MIN_TAB_SIZE = 1;
    public static final int MAX_TAB_SIZE = 32;
    public static final int MIN_BLINK_RATE = 0; // no flashing
    public static final int MAX_BLINK_RATE = 1000; // once per second

    private final VenusUI gui;
    /**
     * Number of times File->New has been selected.  Used to generate
     * default filename until first Save or Save As.
     */
    private int newUsageCount;
    // Current Directory for Save operation
    // Values will mainly be set by the EditTab as Save operations occur.
    private final String defaultSaveDirectory;
    private String currentSaveDirectory;

    /**
     * Create editor.
     *
     * @param gui the GUI that owns this editor
     */
    public Editor(VenusUI gui) {
        this.gui = gui;
        newUsageCount = 0;
        // Directory from which MARS was launched. Guaranteed to have a value.
        defaultSaveDirectory = System.getProperty("user.dir");
        currentSaveDirectory = defaultSaveDirectory;
    }

    /**
     * Get name of current directory for Save or Save As operation.
     *
     * @return String containing directory pathname.  Returns null if there is
     *     no EditTab.  Returns default (the directory MARS is launched from) if
     *     no Save or Save As operations have been performed.
     */
    public String getCurrentSaveDirectory() {
        return currentSaveDirectory;
    }

    /**
     * Set name of current directory for Save operation.  The contents of this directory will
     * be displayed when Save dialog is launched.
     *
     * @param currentSaveDirectory String containing pathname for current Save directory. If
     *                             it does not exist or is not a directory, the default (MARS launch directory) will be used.
     */
    public void setCurrentSaveDirectory(String currentSaveDirectory) {
        File file = new File(currentSaveDirectory);
        if (!file.exists() || !file.isDirectory()) {
            this.currentSaveDirectory = defaultSaveDirectory;
        }
        else {
            this.currentSaveDirectory = currentSaveDirectory;
        }
    }

    /**
     * Generates the next default file name.
     *
     * @return String "untitled-N.asm", where N is a number that increments upon each call.
     */
    public String getNextDefaultFilename() {
        return "untitled-" + (++newUsageCount) + ".asm";
    }

    /**
     * Places name of file currently being edited into its edit tab and
     * the application's title bar.  If file has been modified since created,
     * opened, or saved, as indicated by value of the status parameter, the name
     * will be preceded by an asterisk.
     *
     * @param name   Name of file (last component of path)
     * @param status Edit status of file.  See FileStatus static constants.
     */
    public void setTitle(String name, int status) {
        if (status == FileStatus.NO_FILE || name == null || name.isBlank()) {
            gui.setTitleContent(null);
        }
        else {
            StringBuilder content = new StringBuilder();
            if (status == FileStatus.NEW_EDITED || status == FileStatus.EDITED) {
                // Add the prefix for unsaved changes
                content.append('*');
            }
            content.append(name);
            gui.setTitleContent(content.toString());
            gui.getMainPane().getEditTab().setTitleAt(gui.getMainPane().getEditTab().getSelectedIndex(), content.toString());
        }
    }

    /**
     * Perform "new" operation to create an empty tab.
     */
    public void newFile() {
        gui.getMainPane().getEditTab().newFile();
    }

    /**
     * Perform "close" operation on current tab's file.
     *
     * @return true if succeeded, else false.
     */
    public boolean close() {
        return gui.getMainPane().getEditTab().closeCurrentFile();
    }

    /**
     * Close all currently open files.
     *
     * @return true if succeeded, else false.
     */
    public boolean closeAll() {
        return gui.getMainPane().getEditTab().closeAllFiles();
    }

    /**
     * Perform "save" operation on current tab's file.
     *
     * @return true if succeeded, else false.
     */
    public boolean save() {
        return gui.getMainPane().getEditTab().saveCurrentFile();
    }

    /**
     * Perform "save as" operation on current tab's file.
     *
     * @return true if succeeded, else false.
     */
    public boolean saveAs() {
        return gui.getMainPane().getEditTab().saveAsCurrentFile();
    }

    /**
     * Perform save operation on all open files (tabs).
     *
     * @return true if succeeded, else false.
     */
    public boolean saveAll() {
        return gui.getMainPane().getEditTab().saveAllFiles();
    }

    /**
     * Launch a file chooser for the user to select one or more files,
     * attempting to open them in new editor tabs afterward.
     *
     * @return The list of files which could not be opened due to an error.
     *     (Can be empty.)
     */
    public ArrayList<File> open() {
        return gui.getMainPane().getEditTab().openFiles();
    }

    /**
     * Called by several of the Action objects when there is potential
     * loss of editing changes.  Specifically: if there is a current
     * file open for editing and its modify flag is true, then give user
     * a dialog box with choice to save, discard edits, or cancel and
     * carry out the decision.  This applies to File->New, File->Open,
     * File->Close, and File->Exit.
     *
     * @return false means user selected Cancel so caller should do that.
     *     Return of true means caller can proceed (edits were saved or discarded).
     */
    public boolean editsSavedOrAbandoned() {
        return gui.getMainPane().getEditTab().editsSavedOrAbandoned();
    }
}