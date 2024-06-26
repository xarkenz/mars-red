package mars.venus.execute;

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

import mars.venus.editor.FileStatus;

/**
 * Enumeration to indicate the status of the current program (which may involve more than one file).
 * These status values were originally part of {@link FileStatus}, but were split off to eliminate
 * unnecessary global state in the class.
 */
public enum ProgramStatus {
    /**
     * The program has yet to be assembled.
     */
    NOT_ASSEMBLED,
    /**
     * The program has just been assembled successfully, and is runnable.
     */
    NOT_STARTED,
    /**
     * The program is currently being executed by the simulator.
     */
    RUNNING,
    /**
     * The program stopped execution after starting, but is still runnable.
     */
    PAUSED,
    /**
     * The program stopped execution after starting, and is no longer runnable.
     */
    TERMINATED;

    /**
     * Determine whether the program has been previously started, but is not currently running.
     *
     * @return <code>true</code> if the program is {@link #PAUSED} or {@link #TERMINATED},
     *         or <code>false</code> otherwise.
     */
    public boolean hasStarted() {
        return this == PAUSED || this == TERMINATED;
    }

    /**
     * Determine whether the program is able to start, but is not currently running.
     *
     * @return <code>true</code> if the program is {@link #NOT_STARTED} or {@link #PAUSED},
     *         or <code>false</code> otherwise.
     */
    public boolean canStart() {
        return this == NOT_STARTED || this == PAUSED;
    }

    /**
     * Determine whether the program is able to run, but is not currently running.
     *
     * @return <code>true</code> if the program is {@link #NOT_STARTED}, {@link #PAUSED}, or {@link #TERMINATED},
     *         or <code>false</code> otherwise.
     */
    public boolean isRunnable() {
        return this == NOT_STARTED || this == PAUSED || this == TERMINATED;
    }
}
