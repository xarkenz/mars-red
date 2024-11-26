package mars.simulator;

import mars.Application;
import mars.assembler.BasicStatement;
import mars.mips.hardware.*;
import mars.mips.instructions.Instruction;

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
 * @author Pete Sanderson, February 2006
 */
public class BackStepper {
    private boolean enabled;
    private final BackStepStack stack;

    // One can argue using java.util.Stack, given its clumsy implementation.
    // A homegrown linked implementation will be more streamlined, but
    // I anticipate that backstepping will only be used during timed
    // (currently max 30 instructions/second) or stepped execution, where
    // performance is not an issue.  Its Vector implementation may result
    // in quicker garbage collection than a pure linked list implementation.

    /**
     * Create a fresh BackStepper.  It is enabled, which means all
     * subsequent instruction executions will have their "undo" action
     * recorded here.
     */
    public BackStepper() {
        this.enabled = true;
        this.stack = new BackStepStack(Application.MAXIMUM_BACKSTEPS);
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

    /**
     * Determine whether the next back-step action occurred as the result of
     * an instruction that executed in the "delay slot" of a delayed branch.
     *
     * @return true if next backstep is instruction that executed in delay slot,
     *     false otherwise.
     */
    // Added 25 June 2007
    public boolean isInDelaySlot() {
        return !this.isEmpty() && this.stack.peek().isInDelaySlot;
    }

    /**
     * Carry out a "back step", which will undo the latest execution step.
     * Does nothing if backstepping not enabled or if there are no steps to undo.
     */
    // Note that there may be more than one "step" in an instruction execution; for
    // instance the multiply, divide, and double-precision floating point operations
    // all store their result in register pairs which results in two store operations.
    // Both must be undone transparently, so we need to detect that multiple steps happen
    // together and carry out all of them here.
    // Use a do-while loop based on the backstep's program statement reference.
    public void backStep() {
        if (!this.isEnabled() || this.stack.isEmpty()) {
            return;
        }

        this.setEnabled(false); // MUST DO THIS SO METHOD CALL IN SWITCH WILL NOT RESULT IN NEW ACTION ON STACK!

        BasicStatement statement = this.stack.peek().statement;
        do {
            BackStep backStep = this.stack.pop();

            if (statement != null) {
                Processor.setProgramCounter(backStep.programCounter);
            }
            try {
                switch (backStep.action) {
                    case DO_NOTHING -> {}
                    case MEMORY_WORD -> {
                        Memory.getInstance().storeWord(backStep.targetAddress, backStep.restoreValue, true);
                    }
                    case MEMORY_STATEMENT -> {
                        Memory.getInstance().storeStatement(backStep.targetAddress, backStep.restoreStatement, true);
                    }
                    case REGISTER_VALUE -> {
                        backStep.targetRegister.setValue(backStep.restoreValue);
                    }
                }
            }
            catch (AddressErrorException exception) {
                // If the original action did not cause an exception this will not either.
                throw new RuntimeException("accessed invalid memory address while backstepping");
            }
        }
        while (!this.stack.isEmpty() && this.stack.peek().statement == statement);

        this.setEnabled(true); // RESET IT (was disabled at top of loop -- see comment)
    }


    /**
     * Convenience method called below to get program counter value.  If it needs to be
     * be modified (e.g. to subtract 4) that can be done here in one place.
     */
    private int getProgramCounter() {
        // PC incremented prior to instruction simulation, so need to adjust for that.
        return Processor.getProgramCounter() - Instruction.BYTES_PER_INSTRUCTION;
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory word value.
     *
     * @param address The affected memory address.
     * @param value   The "restore" value to be stored there.
     */
    public void wordWritten(int address, int value) {
        if (this.isEnabled()) {
            BackStep backStep = this.stack.push(Action.MEMORY_WORD, this.getProgramCounter());
            backStep.targetAddress = address;
            backStep.restoreValue = value;
        }
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory word value.
     *
     * @param address The affected memory address.
     * @param statement   The "restore" value to be stored there.
     */
    public void statementWritten(int address, BasicStatement statement) {
        if (this.isEnabled()) {
            BackStep backStep = this.stack.push(Action.MEMORY_STATEMENT, this.getProgramCounter());
            backStep.targetAddress = address;
            backStep.restoreStatement = statement;
        }
    }

    /**
     * Add a new "back step" (the undo action) to the stack.  The action here
     * is to restore a register file register value.
     *
     * @param register The affected register number.
     * @param value    The "restore" value to be stored there.
     */
    public void registerChanged(Register register, int value) {
        if (this.isEnabled()) {
            BackStep backStep = this.stack.push(Action.REGISTER_VALUE, this.getProgramCounter());
            backStep.targetRegister = register;
            backStep.restoreValue = value;
        }
    }

    /**
     * Add a new "back step" (the undo action) to the stack.  The action here
     * is to restore the program counter.
     *
     * @param programCounter The "restore" value to be stored there.
     */
    public void programCounterChanged(int programCounter) {
        if (this.isEnabled()) {
            BackStep backStep = this.stack.push(Action.REGISTER_VALUE, programCounter);
            backStep.targetRegister = Processor.getProgramCounterRegister();
            backStep.restoreValue = programCounter;
        }
    }

    /**
     * Add a new "back step" (the undo action) to the stack.  The action here
     * is to do nothing!  This is just a place holder so when user is backstepping
     * through the program no instructions will be skipped.  Cosmetic. If the top of the
     * stack has the same PC counter, the do-nothing action will not be added.
     *
     * @param programCounter The program counter to check against the top of the stack.
     */
    public void instructionFinished(int programCounter) {
        if (this.isEnabled() && (this.stack.isEmpty() || programCounter != this.stack.peek().programCounter)) {
            this.stack.push(Action.DO_NOTHING, programCounter);
        }
    }

    /**
     * The types of "undo" actions.
     */
    private enum Action {
        DO_NOTHING,
        REGISTER_VALUE,
        MEMORY_WORD,
        MEMORY_STATEMENT,
    }

    /**
     * Represents a "back step" (undo action) on the stack.
     */
    private static class BackStep {
        public Action action; // what "undo" action to perform
        public int programCounter; // program counter value when original step occurred
        public BasicStatement statement; // statement whose action is being "undone" here
        public boolean isInDelaySlot; // true if instruction executed in "delay slot" (delayed branching enabled)
        public Register targetRegister;
        public int targetAddress;
        public int restoreValue;
        public BasicStatement restoreStatement;

        /**
         * It is critical that BackStep object get its values by calling this method
         * rather than assigning to individual members, because of the technique used
         * to set its statement member (and possibly programCounter).
         */
        public BackStep assign(Action action, int programCounter) {
            this.action = action;
            this.programCounter = programCounter;
            try {
                // Client does not have direct access to program statement, and rather than making all
                // of them go through the methods below to obtain it, we will do it here.
                // Want the program statement but do not want observers notified.
                this.statement = Memory.getInstance().fetchStatement(programCounter, false);
            }
            catch (AddressErrorException exception) {
                // The only situation causing this so far: user modifies memory or register
                // contents through direct manipulation on the GUI, after assembling the program but
                // before starting to run it (or after backstepping all the way to the start).
                // The action will not be associated with any instruction, but will be carried out
                // when popped.
                this.statement = null;
            }
            this.isInDelaySlot = Simulator.getInstance().isInDelaySlot(); // ADDED 25 June 2007

            return this;
        }
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
        private final BackStep[] entries;

        /**
         * Stack is created upon successful assembly or reset.  The one-time overhead of
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
            this.entries = new BackStep[capacity];
            for (int index = 0; index < capacity; index++) {
                this.entries[index] = new BackStep();
            }
        }

        public synchronized void clear() {
            this.size = 0;
        }

        public synchronized boolean isEmpty() {
            return this.size == 0;
        }

        public synchronized BackStep push(Action action, int programCounter) {
            // Allocate space in the stack by incrementing the top
            this.top = (this.top + 1) % this.entries.length;
            // Increment the size to match unless stack is full, in which case the oldest entry is overwritten
            if (this.size < this.entries.length) {
                this.size++;
            }

            // Reuse existing entry object to form "new" entry
            return this.entries[this.top].assign(action, programCounter);
        }

        public synchronized BackStep pop() {
            BackStep backStep = this.peek();

            this.size--;
            // Add this.entries.length (capacity) to ensure result of modulo operation is positive
            this.top = (this.top + this.entries.length - 1) % this.entries.length;

            return backStep;
        }

        public synchronized BackStep peek() {
            if (this.isEmpty()) {
                throw new IllegalStateException("backstep stack is empty");
            }
            return this.entries[this.top];
        }
    }
}