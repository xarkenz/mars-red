package mars.mips.hardware;

import mars.Application;
import mars.assembler.SymbolTable;
import mars.mips.instructions.Instruction;
import mars.util.Binary;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Represents the collection of MIPS registers.
 *
 * @author Jason Bumgarner, Jason Shrewsbury
 * @version June 2003
 */
public class RegisterFile {
    public static final int RETURN_VALUE_0 = 2;
    public static final int RETURN_VALUE_1 = 3;
    public static final int ARGUMENT_0 = 4;
    public static final int ARGUMENT_1 = 5;
    public static final int ARGUMENT_2 = 6;
    public static final int ARGUMENT_3 = 7;
    public static final int GLOBAL_POINTER = 28;
    public static final int STACK_POINTER = 29;
    public static final int FRAME_POINTER = 30;
    public static final int RETURN_ADDRESS = 31;
    // These numbers aren't really meaningful, but they are useful in some cases e.g. register display
    public static final int PROGRAM_COUNTER = 32;
    public static final int HIGH_ORDER = 33;
    public static final int LOW_ORDER = 34;

    private static final Register[] REGISTERS = {
        new Register("$zero", 0, 0),
        new Register("$at", 1, 0),
        new Register("$v0", 2, 0),
        new Register("$v1", 3, 0),
        new Register("$a0", 4, 0),
        new Register("$a1", 5, 0),
        new Register("$a2", 6, 0),
        new Register("$a3", 7, 0),
        new Register("$t0", 8, 0),
        new Register("$t1", 9, 0),
        new Register("$t2", 10, 0),
        new Register("$t3", 11, 0),
        new Register("$t4", 12, 0),
        new Register("$t5", 13, 0),
        new Register("$t6", 14, 0),
        new Register("$t7", 15, 0),
        new Register("$s0", 16, 0),
        new Register("$s1", 17, 0),
        new Register("$s2", 18, 0),
        new Register("$s3", 19, 0),
        new Register("$s4", 20, 0),
        new Register("$s5", 21, 0),
        new Register("$s6", 22, 0),
        new Register("$s7", 23, 0),
        new Register("$t8", 24, 0),
        new Register("$t9", 25, 0),
        new Register("$k0", 26, 0),
        new Register("$k1", 27, 0),
        new Register("$gp", GLOBAL_POINTER, 0),
        new Register("$sp", STACK_POINTER, 0),
        new Register("$fp", FRAME_POINTER, 0),
        new Register("$ra", RETURN_ADDRESS, 0),
    };

    private static final Register PROGRAM_COUNTER_REGISTER = new Register("pc", PROGRAM_COUNTER, 0);
    private static final Register HIGH_ORDER_REGISTER = new Register("hi", HIGH_ORDER, 0);
    private static final Register LOW_ORDER_REGISTER = new Register("lo", LOW_ORDER, 0);

    /**
     * Update the register value whose number is given, unless it is <code>$zero</code>.
     * Also handles the internal pc, lo, and hi registers.
     *
     * @param number Register to set the value of.
     * @param value  The desired value for the register.
     * @return The previous value of the register.
     */
    public static int updateRegister(int number, int value) {
        int previousValue;
        // The $zero register cannot be updated
        if (0 < number && number < REGISTERS.length) {
            // Originally, this used a linear search to figure out which register to update.
            // Since all registers 0-31 are present in order, a simple array access should work.
            // - Sean Clarke 03/2024
            previousValue = REGISTERS[number].setValue(value);
        }
        else {
            // $zero or invalid register, do nothing
            return 0;
        }

        if (Application.isBackSteppingEnabled()) {
            Application.program.getBackStepper().addRegisterFileRestore(number, previousValue);
        }

        return previousValue;
    }

    /**
     * Returns the value of the register whose number is given.
     *
     * @param number The register number.
     * @return The value of the given register.
     */
    public static int getValue(int number) {
        return REGISTERS[number].getValue();
    }

    /**
     * Get register number corresponding to given name.
     *
     * @param name The string formatted register name to look for, in $zero format.
     * @return The number of the register represented by the string,
     *     or -1 if no match was found.
     */
    public static int getNumber(String name) {
        // check for register mnemonic $zero thru $ra
        // just do linear search; there aren't that many registers
        for (Register register : REGISTERS) {
            if (register.getName().equals(name)) {
                return register.getNumber();
            }
        }
        return -1;
    }

    /**
     * Get the set of accessible registers, including pc, hi, and lo.
     *
     * @return The set of registers.
     */
    public static Register[] getRegisters() {
        return REGISTERS;
    }

    /**
     * Get the register object corresponding to a given name.
     *
     * @param name The register name, either in $0 or $zero format.
     * @return The register object, or null if not found.
     */
    public static Register getUserRegister(String name) {
        if (name.isEmpty() || name.charAt(0) != '$') {
            return null;
        }
        try {
            // Check for register number 0-31
            return REGISTERS[Binary.decodeInteger(name.substring(1))]; // KENV 1/6/05
        }
        catch (NumberFormatException | ArrayIndexOutOfBoundsException exception) {
            // Handles both NumberFormat and ArrayIndexOutOfBounds
            // Check for register mnemonic $zero thru $ra
            // Just do linear search; there aren't that many registers
            for (Register register : REGISTERS) {
                if (register.getName().equals(name)) {
                    return register;
                }
            }
            return null;
        }
    }

    /**
     * Initialize the Program Counter.  Do not use this to implement jumps and
     * branches, as it will NOT record a backstep entry with the restore value.
     * If you need backstepping capability, use {@link #setProgramCounter(int)} instead.
     *
     * @param value The value to set the Program Counter to.
     */
    public static void initializeProgramCounter(int value) {
        PROGRAM_COUNTER_REGISTER.setValue(value);
    }

    /**
     * Will initialize the Program Counter to either the default reset value, or the address
     * associated with source program global label "main", if it exists as a text segment label
     * and the global setting is set.
     *
     * @param startAtMain If true, will set program counter to address of statement labeled
     *                    'main' (or other defined start label) if defined.  If not defined, or if parameter false,
     *                    will set program counter to default reset value.
     */
    public static void initializeProgramCounter(boolean startAtMain) {
        int mainAddress = Application.globalSymbolTable.getAddress(SymbolTable.getStartLabel());
        if (startAtMain && mainAddress != SymbolTable.NOT_FOUND && (Memory.getInstance().isInTextSegment(mainAddress) || Memory.getInstance().isInKernelTextSegment(mainAddress))) {
            initializeProgramCounter(mainAddress);
        }
        else {
            initializeProgramCounter(PROGRAM_COUNTER_REGISTER.getDefaultValue());
        }
    }

    /**
     * Get the current program counter value.
     *
     * @return The program counter value as an int.
     */
    public static int getProgramCounter() {
        return PROGRAM_COUNTER_REGISTER.getValue();
    }

    /**
     * Set the value of the program counter.  Note that an ordinary PC update should be done using
     * the {@link #incrementPC()} method; use this only when processing jumps and branches.
     *
     * @param value The value to set the Program Counter to.
     * @return The previous program counter value.
     */
    public static int setProgramCounter(int value) {
        int previousValue = PROGRAM_COUNTER_REGISTER.setValue(value);
        if (Application.isBackSteppingEnabled()) {
            Application.program.getBackStepper().addPCRestore(previousValue);
        }
        return previousValue;
    }

    /**
     * Get the Register object for program counter.  Use with caution.
     *
     * @return The program counter register.
     */
    public static Register getProgramCounterRegister() {
        return PROGRAM_COUNTER_REGISTER;
    }

    /**
     * Get the program counter's initial (reset) value.
     *
     * @return The program counter's initial value.
     */
    public static int getInitialProgramCounter() {
        return PROGRAM_COUNTER_REGISTER.getDefaultValue();
    }

    /**
     * Increment the Program counter in the general case (not a jump or branch).
     */
    public static void incrementPC() {
        PROGRAM_COUNTER_REGISTER.setValue(PROGRAM_COUNTER_REGISTER.getValueNoNotify() + Instruction.BYTES_PER_INSTRUCTION);
    }

    public static int getHighOrder() {
        return HIGH_ORDER_REGISTER.getValue();
    }

    public static void setHighOrder(int value) {
        HIGH_ORDER_REGISTER.setValue(value);
    }

    public static Register getHighOrderRegister() {
        return HIGH_ORDER_REGISTER;
    }

    public static int getLowOrder() {
        return LOW_ORDER_REGISTER.getValue();
    }

    public static void setLowOrder(int value) {
        LOW_ORDER_REGISTER.setValue(value);
    }

    public static Register getLowOrderRegister() {
        return LOW_ORDER_REGISTER;
    }

    /**
     * Reinitialize the values of the registers.
     * <p>
     * <b>NOTE:</b> Should <i>not</i> be called from command-mode MARS because this
     * this method uses global settings from the registry.  Command-mode must operate
     * using only the command switches, not registry settings.
     */
    public static void resetRegisters() {
        for (Register register : REGISTERS) {
            register.resetValueToDefault();
        }
        HIGH_ORDER_REGISTER.resetValueToDefault();
        LOW_ORDER_REGISTER.resetValueToDefault();
        // Replaces "programCounter.resetValue()", DPS 3/3/09
        initializeProgramCounter(Application.getSettings().startAtMain.get());
    }
}
