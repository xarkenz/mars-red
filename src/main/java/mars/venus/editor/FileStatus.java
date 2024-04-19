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
public enum FileStatus {
    /**
     * No files are open (this only applies to the menu state in {@link mars.venus.VenusUI}).
     */
    NO_FILE,
    /**
     * Newly created file with no edits.
     */
    NEW_NOT_EDITED,
    /**
     * Newly created file with unsaved edits.
     */
    NEW_EDITED,
    /**
     * File from disk with no edits.
     */
    NOT_EDITED,
    /**
     * File from disk with unsaved edits.
     */
    EDITED;

    /**
     * Determine whether file is "new", which means created using New but not yet saved.
     * If created using Open, it is not new.
     *
     * @return true if file was created using New and has not yet been saved, false otherwise.
     */
    public boolean isNew() {
        return switch (this) {
            case NEW_NOT_EDITED, NEW_EDITED -> true;
            default -> false;
        };
    }

    /**
     * Determine whether file has been modified since last save or, if not yet saved, since
     * being created using New or Open.
     *
     * @return true if file has been modified since save or creation, false otherwise.
     */
    public boolean hasUnsavedEdits() {
        return switch (this) {
            case NEW_EDITED, EDITED -> true;
            default -> false;
        };
    }
}