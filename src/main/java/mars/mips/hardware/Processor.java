package mars.mips.hardware;

import mars.Application;
import mars.assembler.Symbol;
import mars.assembler.SymbolTable;
import mars.simulator.Simulator;
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
public class Processor {
    public static final int ZERO_CONSTANT = 0;
    public static final int ASSEMBLER_TEMPORARY = 1;
    public static final int VALUE_0 = 2;
    public static final int VALUE_1 = 3;
    public static final int ARGUMENT_0 = 4;
    public static final int ARGUMENT_1 = 5;
    public static final int ARGUMENT_2 = 6;
    public static final int ARGUMENT_3 = 7;
    public static final int TEMPORARY_0 = 8;
    public static final int TEMPORARY_1 = 9;
    public static final int TEMPORARY_2 = 10;
    public static final int TEMPORARY_3 = 11;
    public static final int TEMPORARY_4 = 12;
    public static final int TEMPORARY_5 = 13;
    public static final int TEMPORARY_6 = 14;
    public static final int TEMPORARY_7 = 15;
    public static final int SAVED_0 = 16;
    public static final int SAVED_1 = 17;
    public static final int SAVED_2 = 18;
    public static final int SAVED_3 = 19;
    public static final int SAVED_4 = 20;
    public static final int SAVED_5 = 21;
    public static final int SAVED_6 = 22;
    public static final int SAVED_7 = 23;
    public static final int TEMPORARY_8 = 24;
    public static final int TEMPORARY_9 = 25;
    public static final int KERNEL_0 = 26;
    public static final int KERNEL_1 = 27;
    public static final int GLOBAL_POINTER = 28;
    public static final int STACK_POINTER = 29;
    public static final int FRAME_POINTER = 30;
    public static final int RETURN_ADDRESS = 31;
    // These numbers aren't really meaningful, but they are useful in some cases e.g. register display
    public static final int HIGH_ORDER = 32;
    public static final int LOW_ORDER = 33;
    public static final int PROGRAM_COUNTER = 34;

    private static final Register[] REGISTERS = {
        new Register("$zero", ZERO_CONSTANT, 0),
        new Register("$at", ASSEMBLER_TEMPORARY, 0),
        new Register("$v0", VALUE_0, 0),
        new Register("$v1", VALUE_1, 0),
        new Register("$a0", ARGUMENT_0, 0),
        new Register("$a1", ARGUMENT_1, 0),
        new Register("$a2", ARGUMENT_2, 0),
        new Register("$a3", ARGUMENT_3, 0),
        new Register("$t0", TEMPORARY_0, 0),
        new Register("$t1", TEMPORARY_1, 0),
        new Register("$t2", TEMPORARY_2, 0),
        new Register("$t3", TEMPORARY_3, 0),
        new Register("$t4", TEMPORARY_4, 0),
        new Register("$t5", TEMPORARY_5, 0),
        new Register("$t6", TEMPORARY_6, 0),
        new Register("$t7", TEMPORARY_7, 0),
        new Register("$s0", SAVED_0, 0),
        new Register("$s1", SAVED_1, 0),
        new Register("$s2", SAVED_2, 0),
        new Register("$s3", SAVED_3, 0),
        new Register("$s4", SAVED_4, 0),
        new Register("$s5", SAVED_5, 0),
        new Register("$s6", SAVED_6, 0),
        new Register("$s7", SAVED_7, 0),
        new Register("$t8", TEMPORARY_8, 0),
        new Register("$t9", TEMPORARY_9, 0),
        new Register("$k0", KERNEL_0, 0),
        new Register("$k1", KERNEL_1, 0),
        new Register("$gp", GLOBAL_POINTER, 0),
        new Register("$sp", STACK_POINTER, 0),
        new Register("$fp", FRAME_POINTER, 0),
        new Register("$ra", RETURN_ADDRESS, 0),
    };

    private static final Register HIGH_ORDER_REGISTER = new Register("hi", HIGH_ORDER, 0);
    private static final Register LOW_ORDER_REGISTER = new Register("lo", LOW_ORDER, 0);
    private static final Register PROGRAM_COUNTER_REGISTER = new Register("PC", PROGRAM_COUNTER, 0);

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
        if (number == 0) {
            // $zero cannot be modified
            return 0;
        }
        else {
            // Originally, this used a linear search to figure out which register to update.
            // Since all registers 0-31 are present in order, a simple array access should work.
            // - Sean Clarke 03/2024
            previousValue = REGISTERS[number].setValue(value);
        }

        if (Simulator.getInstance().getBackStepper().isEnabled()) {
            Simulator.getInstance().getBackStepper().addRegisterFileRestore(number, previousValue);
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
    public static int getRegisterNumber(String name) {
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
    public static Register getRegister(String name) {
        if (!name.isEmpty() && name.charAt(0) == '$') {
            // Check for register number 0-31
            try {
                int number = Integer.parseInt(name.substring(1));
                if (0 <= number && number < REGISTERS.length) {
                    return REGISTERS[number];
                }
            }
            catch (NumberFormatException exception) {
                return null;
                // Handles both NumberFormat and ArrayIndexOutOfBounds
            }

            // Check for register mnemonic $zero thru $ra
            // Just do linear search; there aren't that many registers
            for (Register register : REGISTERS) {
                if (register.getName().equals(name)) {
                    return register;
                }
            }
        }

        return null;
    }

    /**
     * Gets a long representing the value stored in the given register as well as the next register.
     * The register must be even-numbered.
     *
     * @param reg Register to get the value of. Must be even number of even/odd pair.
     * @throws InvalidRegisterAccessException Thrown if register ID is invalid or odd-numbered.
     */
    public static long getLongFromRegisterPair(int reg) throws InvalidRegisterAccessException {
        if (reg % 2 != 0) {
            throw new InvalidRegisterAccessException();
        }

        int firstValue = REGISTERS[reg].getValue();
        int secondValue = REGISTERS[reg + 1].getValue();
        return switch (Memory.getInstance().getEndianness()) {
            case BIG_ENDIAN -> Binary.twoIntsToLong(firstValue, secondValue);
            case LITTLE_ENDIAN -> Binary.twoIntsToLong(secondValue, firstValue);
        };
    }

    /**
     * Sets the value of the register pair given to the long value containing 64 bit pattern
     * given.  The register must be even-numbered, and the low order 32 bits from the long are placed in it.
     * The high order 32 bits from the long are placed in the (odd numbered) register that follows it.
     *
     * @param reg Register to set the value of.  Must be even register of even/odd pair.
     * @param val The desired long value for the register.
     * @throws InvalidRegisterAccessException Thrown if register ID is invalid or odd-numbered.
     */
    public static void setRegisterPairToLong(int reg, long val) throws InvalidRegisterAccessException {
        if (reg % 2 != 0) {
            throw new InvalidRegisterAccessException();
        }

        switch (Memory.getInstance().getEndianness()) {
            case BIG_ENDIAN -> {
                REGISTERS[reg].setValue(Binary.highOrderLongToInt(val));
                REGISTERS[reg + 1].setValue(Binary.lowOrderLongToInt(val));
            }
            case LITTLE_ENDIAN -> {
                REGISTERS[reg].setValue(Binary.lowOrderLongToInt(val));
                REGISTERS[reg + 1].setValue(Binary.highOrderLongToInt(val));
            }
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
        if (startAtMain) {
            Symbol symbol = Application.assembler.getGlobalSymbolTable().getSymbol(SymbolTable.getStartLabel());
            if (symbol != null && symbol.isText()) {
                initializeProgramCounter(symbol.getAddress());
            }
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
     * Set the value of the program counter.
     *
     * @param value The value to set the Program Counter to.
     * @return The previous program counter value.
     */
    public static int setProgramCounter(int value) {
        int previousValue = PROGRAM_COUNTER_REGISTER.setValue(value);
        if (Simulator.getInstance().getBackStepper().isEnabled()) {
            Simulator.getInstance().getBackStepper().addPCRestore(previousValue);
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
    public static void reset() {
        for (Register register : REGISTERS) {
            register.resetValueToDefault();
        }
        HIGH_ORDER_REGISTER.resetValueToDefault();
        LOW_ORDER_REGISTER.resetValueToDefault();
        // Replaces "programCounter.resetValue()", DPS 3/3/09
        initializeProgramCounter(Application.getSettings().startAtMain.get());
    }
}
