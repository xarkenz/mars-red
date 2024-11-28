package mars.simulator;

import mars.Application;
import mars.assembler.BasicStatement;
import mars.mips.hardware.*;

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
 * Used to "step backward" through execution, undoing each instruction.
 *
 * @author Pete Sanderson, February 2006; Sean Clarke, November 2024
 */
public class BackStepper {
    private boolean enabled;
    private final BackStepStack stack;
    private int currentStepID;

    /**
     * Create a fresh BackStepper.  It is enabled, which means all
     * subsequent instruction executions will have their "undo" action
     * recorded here.
     */
    public BackStepper() {
        this.enabled = true;
        this.stack = new BackStepStack(Application.MAXIMUM_BACKSTEPS);
        this.currentStepID = 0;
    }

    public void reset() {
        this.stack.clear();
    }

    /**
     * Determine whether execution "undo" steps are currently being recorded.
     *
     * @return true if undo steps being recorded, false if not.
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set enable status.
     *
     * @param state If true, will begin (or continue) recoding "undo" steps.  If false, will stop.
     */
    public void setEnabled(boolean state) {
        this.enabled = state;
    }

    /**
     * Test whether there are steps that can be undone.
     *
     * @return true if there are no steps to be undone, false otherwise.
     */
    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    public void finishStep() {
        this.currentStepID++;
    }

    /**
     * Carry out a "back step", which will undo the latest execution step.
     * Does nothing if backstepping not enabled or if there are no steps to undo.
     */
    /*
     * Note that there may be more than one action in an instruction execution (one "step"); for
     * instance the multiply, divide, and double-precision floating point operations
     * all store their result in register pairs which results in two store operations.
     * Both must be undone transparently, so we need to detect that multiple action happen
     * together and carry out all of them here. Use a do-while loop based on the step ID.
     */
    public void backStep() {
        if (!this.isEnabled() || this.stack.isEmpty()) {
            return;
        }

        this.setEnabled(false); // MUST DO THIS SO METHOD CALL IN SWITCH WILL NOT RESULT IN NEW ACTION ON STACK!

        int stepID = this.stack.peek().stepID;
        do {
            Action action = this.stack.pop();

            try {
                switch (action.target) {
                    case REGISTER_VALUE -> {
                        action.register.setValue(action.restoreValue);
                    }
                    case MEMORY_WORD -> {
                        Memory.getInstance().storeWord(action.address, action.restoreValue, true);
                    }
                    case MEMORY_STATEMENT -> {
                        Memory.getInstance().storeStatement(action.address, action.restoreStatement, true);
                    }
                }
            }
            catch (AddressErrorException exception) {
                // If the original action did not cause an exception this will not either, but just in case
                throw new RuntimeException("accessed invalid memory address while backstepping", exception);
            }
        }
        while (!this.stack.isEmpty() && this.stack.peek().stepID == stepID);

        this.setEnabled(true); // RESET IT (was disabled at top of loop -- see comment)
    }

    /**
     * Add a new "back step" (the undo action) to the stack.  The action here
     * is to restore a register file register value.
     *
     * @param register     The affected register number.
     * @param restoreValue The "restore" value to be stored there.
     */
    public void registerChanged(Register register, int restoreValue) {
        if (this.isEnabled()) {
            Action action = this.stack.push();
            action.target = RestoreTarget.REGISTER_VALUE;
            action.stepID = this.currentStepID;
            action.register = register;
            action.restoreValue = restoreValue;
        }
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory word value.
     *
     * @param address      The affected memory address.
     * @param restoreValue The "restore" value to be stored there.
     */
    public void wordWritten(int address, int restoreValue) {
        if (this.isEnabled()) {
            Action action = this.stack.push();
            action.target = RestoreTarget.MEMORY_WORD;
            action.stepID = this.currentStepID;
            action.address = address;
            action.restoreValue = restoreValue;
        }
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory word value.
     *
     * @param address          The affected memory address.
     * @param restoreStatement The "restore" value to be stored there.
     */
    public void statementWritten(int address, BasicStatement restoreStatement) {
        if (this.isEnabled()) {
            Action action = this.stack.push();
            action.target = RestoreTarget.MEMORY_STATEMENT;
            action.stepID = this.currentStepID;
            action.address = address;
            action.restoreStatement = restoreStatement;
        }
    }

    /**
     * The types of "undo" actions.
     */
    private enum RestoreTarget {
        REGISTER_VALUE,
        MEMORY_WORD,
        MEMORY_STATEMENT,
    }

    /**
     * Represents a "back step" (undo action) on the stack.
     */
    private static class Action {
        public RestoreTarget target; // what "undo" action to perform
        public int stepID; // Integer used to group adjacent actions into the same step
        public Register register;
        public int address;
        public int restoreValue;
        public BasicStatement restoreStatement;
    }

    /**
     * Special purpose stack class for backstepping.  You've heard of circular queues
     * implemented with an array, right?  This is a circular stack!  When full, the
     * newly-pushed item overwrites the oldest item, with circular top!  All operations
     * are constant time.  It's synchronized too, to be safe (is used by both the
     * simulation thread and the GUI thread for the back-step button).
     * Upon construction, it is filled with newly-created empty BackStep objects which
     * will exist for the life of the stack.  Push does not create a BackStep object
     * but instead overwrites the contents of the existing one.  Thus during MIPS
     * program (simulated) execution, BackStep objects are never created or junked
     * regardless of how many steps are executed.  This will speed things up a bit
     * and make life easier for the garbage collector.
     */
    private static class BackStepStack {
        private int size;
        private int top;
        private final Action[] entries;

        /**
         * Stack is created once during initialization of the simulator. The one-time overhead of
         * creating all the BackStep objects will not be noticed by the user, and enhances
         * runtime performance by not having to create or recycle them during MIPS
         * program execution.
         */
        public BackStepStack(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("invalid backstep capacity " + capacity);
            }
            this.size = 0;
            this.top = capacity - 1;
            this.entries = new Action[capacity];
            for (int index = 0; index < capacity; index++) {
                this.entries[index] = new Action();
            }
        }

        public synchronized void clear() {
            this.size = 0;
        }

        public synchronized boolean isEmpty() {
            return this.size == 0;
        }

        public synchronized Action push() {
            // Allocate space in the stack by incrementing the top
            this.top = (this.top + 1) % this.entries.length;
            // Increment the size to match unless stack is full, in which case the oldest entry is overwritten
            if (this.size < this.entries.length) {
                this.size++;
            }

            // Reuse existing entry object to form "new" entry
            return this.entries[this.top];
        }

        public synchronized Action pop() {
            // Save the current top of the stack
            Action action = this.peek();
            // Decrement the size and deallocate the space by decrementing the top
            this.size--;
            // Add this.entries.length (capacity) to ensure result of modulo operation is positive
            this.top = (this.top + this.entries.length - 1) % this.entries.length;

            return action;
        }

        public synchronized Action peek() {
            if (this.isEmpty()) {
                throw new IllegalStateException("backstep stack is empty");
            }
            return this.entries[this.top];
        }
    }
}