package mars.simulator;

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

import java.util.EventListener;

/**
 * This interface is used to detect simulator events. It can be implemented and attached to the {@link Simulator}
 * via {@link Simulator#addGUIListener(SimulatorListener)} or {@link Simulator#addThreadListener(SimulatorListener)}.
 * <p>
 * Note: which method above is used to add the listener will determine which thread the callbacks run on.
 */
public interface SimulatorListener extends EventListener {
    /**
     * Called when the simulator begins execution of a program.
     *
     * @param event The event which occurred.
     */
    default void simulatorStarted(SimulatorStartEvent event) {
        // No action by default
    }

    /**
     * Called when the simulator stops execution of a program due to pausing.
     *
     * @param event The event which occurred.
     */
    default void simulatorPaused(SimulatorPauseEvent event) {
        // No action by default
    }

    /**
     * Called when the simulator stops execution of a program due to termination or finishing.
     *
     * @param event The event which occurred.
     */
    default void simulatorFinished(SimulatorFinishEvent event) {
        // No action by default
    }

    /**
     * Called when the simulator has finished executing an instruction, but only if the run speed is not unlimited.
     */
    default void simulatorStepped() {
        // No action by default
    }
}
