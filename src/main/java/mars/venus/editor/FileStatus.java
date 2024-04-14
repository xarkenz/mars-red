package mars.venus.editor;

import java.io.File;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Used to store and return information on the status of the current ASM file that
 * is being edited in the program.
 *
 * @author Team JSpim
 */
public class FileStatus {
    /**
     * initial state or after close
     */
    public static final int NO_FILE = 0;
    /**
     * New edit window with no edits
     */
    public static final int NEW_NOT_EDITED = 1;
    /**
     * New edit window with unsaved edits
     */
    public static final int NEW_EDITED = 2;
    /**
     * open/saved edit window with no edits
     */
    public static final int NOT_EDITED = 3;
    /**
     * open/saved edit window with unsaved edits
     */
    public static final int EDITED = 4;
    /**
     * successful assembly
     */
    public static final int RUNNABLE = 5;
    /**
     * execution is under way
     */
    public static final int RUNNING = 6;
    /**
     * execution terminated
     */
    public static final int TERMINATED = 7;
    /**
     * file is being opened.  DPS 9-Aug-2011
     */
    public static final int OPENING = 8;

    private int status;
    private File file;

    /**
     * Create a FileStatus object with {@link #NO_FILE} for status and null for current file.
     */
    public FileStatus() {
        this(FileStatus.NO_FILE, null);
    }

    /**
     * Create a FileStatus object with given status and file pathname.
     *
     * @param status   Initial file status.  See FileStatus static constants.
     * @param pathname Full file pathname.
     * @see #setPathname(String)
     */
    public FileStatus(int status, String pathname) {
        this.status = status;
        if (pathname == null) {
            this.file = null;
        }
        else {
            this.setPathname(pathname);
        }
    }

    /**
     * Set editing status of this file.  See FileStatus static constants.
     *
     * @param newStatus the new status
     */
    public void setFileStatus(int newStatus) {
        this.status = newStatus;
    }

    /**
     * Get editing status of this file.
     *
     * @return The current editing status.  See FileStatus static constants.
     */
    public int getFileStatus() {
        return this.status;
    }

    /**
     * Determine if file is "new", which means created using New but not yet saved.
     * If created using Open, it is not new.
     *
     * @return true if file was created using New and has not yet been saved, false otherwise.
     */
    public boolean isNew() {
        return status == FileStatus.NEW_NOT_EDITED || status == FileStatus.NEW_EDITED;
    }

    /**
     * Determine if file has been modified since last save or, if not yet saved, since
     * being created using New or Open.
     *
     * @return true if file has been modified since save or creation, false otherwise.
     */
    public boolean hasUnsavedEdits() {
        return status == FileStatus.NEW_EDITED || status == FileStatus.EDITED;
    }

    /**
     * Set full file pathname. See java.io.File(String pathname) for parameter specs.
     *
     * @param newPath the new pathname. If no directory path, getParent() will return null.
     */
    public void setPathname(String newPath) {
        this.file = new File(newPath);
    }

    /**
     * Set full file pathname. See java.io.File(String parent, String child) for parameter specs.
     *
     * @param parent The parent directory of the file.  Can be null.
     * @param name   The name of the file (no directory path).
     */
    public void setPathname(String parent, String name) {
        this.file = new File(parent, name);
    }

    /**
     * Get full file pathname.
     *
     * @return The full pathname as a string.  Null if the current file is null.
     * @see File#getPath()
     */
    public String getPathname() {
        return (this.file == null) ? null : this.file.getPath();
    }

    /**
     * Get file name with no path information.
     *
     * @return The filename as a string.  Null if the current file is null.
     * @see File#getName()
     */
    public String getFilename() {
        return (this.file == null) ? null : this.file.getName();
    }

    /**
     * Get file parent pathname.
     *
     * @return The full parent pathname as a string.  Null if the current file is null.
     * @see File#getParent()
     */
    public String getParent() {
        return (this.file == null) ? null : this.file.getParent();
    }
}