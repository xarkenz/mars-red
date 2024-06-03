package mars.mips.hardware;

import mars.Application;
import mars.util.Binary;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Represents Coprocessor 1, the Floating Point Unit (FPU)
 *
 * @author Pete Sanderson
 * @version July 2005
 */
/*
 * Adapted from RegisterFile class developed by Bumgarner et al in 2003.
 * The FPU registers will be implemented by Register objects.  Such objects
 * can only hold int values, but we can use Float.floatToIntBits() to translate
 * a 32 bit float value into its equivalent 32-bit int representation, and
 * Float.intBitsToFloat() to bring it back.  More importantly, there are
 * similar methods Double.doubleToLongBits() and Double.LongBitsToDouble()
 * which can be used to extend a double value over 2 registers.  The resulting
 * long is split into 2 int values (high order 32 bits, low order 32 bits) for
 * storing into registers, and reassembled upon retrieval.
 */
public class Coprocessor1 {
    private static final Register[] REGISTERS = {
        new Register("$f0", 0, 0),
        new Register("$f1", 1, 0),
        new Register("$f2", 2, 0),
        new Register("$f3", 3, 0),
        new Register("$f4", 4, 0),
        new Register("$f5", 5, 0),
        new Register("$f6", 6, 0),
        new Register("$f7", 7, 0),
        new Register("$f8", 8, 0),
        new Register("$f9", 9, 0),
        new Register("$f10", 10, 0),
        new Register("$f11", 11, 0),
        new Register("$f12", 12, 0),
        new Register("$f13", 13, 0),
        new Register("$f14", 14, 0),
        new Register("$f15", 15, 0),
        new Register("$f16", 16, 0),
        new Register("$f17", 17, 0),
        new Register("$f18", 18, 0),
        new Register("$f19", 19, 0),
        new Register("$f20", 20, 0),
        new Register("$f21", 21, 0),
        new Register("$f22", 22, 0),
        new Register("$f23", 23, 0),
        new Register("$f24", 24, 0),
        new Register("$f25", 25, 0),
        new Register("$f26", 26, 0),
        new Register("$f27", 27, 0),
        new Register("$f28", 28, 0),
        new Register("$f29", 29, 0),
        new Register("$f30", 30, 0),
        new Register("$f31", 31, 0),
    };

    // The 8 condition flags will be stored in bits 0-7 for flags 0-7.
    private static final Register CONDITION_FLAGS = new Register("cf", 32, 0);
    private static final int CONDITION_FLAG_COUNT = 8;

    /**
     * Sets the value of the FPU register given to the value given.
     *
     * @param reg Register to set the value of.
     * @param val The desired float value for the register.
     */
    public static void setRegisterToFloat(int reg, float val) {
        if (0 <= reg && reg < REGISTERS.length) {
            REGISTERS[reg].setValue(Float.floatToRawIntBits(val));
        }
    }

    /**
     * Sets the value of the FPU register given to the 32-bit
     * pattern given by the int parameter.
     *
     * @param reg Register to set the value of.
     * @param val The desired int bit pattern for the register.
     */
    public static void setRegisterToInt(int reg, int val) {
        if (0 <= reg && reg < REGISTERS.length) {
            REGISTERS[reg].setValue(val);
        }
    }

    /**
     * Sets the value of the FPU register given to the double value given.  The register
     * must be even-numbered, and the low order 32 bits are placed in it.  The high order
     * 32 bits are placed in the (odd numbered) register that follows it.
     *
     * @param reg Register to set the value of.
     * @param val The desired double value for the register.
     * @throws InvalidRegisterAccessException if register ID is invalid or odd-numbered.
     */
    public static void setRegisterPairToDouble(int reg, double val) throws InvalidRegisterAccessException {
        setRegisterPairToLong(reg, Double.doubleToRawLongBits(val));
    }

    /**
     * Sets the value of the FPU register pair given to the long value containing 64 bit pattern
     * given.  The register
     * must be even-numbered, and the low order 32 bits from the long are placed in it.  The high order
     * 32 bits from the long are placed in the (odd numbered) register that follows it.
     *
     * @param reg Register to set the value of.  Must be even register of even/odd pair.
     * @param val The desired long value for the register.
     * @throws InvalidRegisterAccessException if register ID is invalid or odd-numbered.
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
     * Gets the float value stored in the given FPU register.
     *
     * @param reg Register to get the value of.
     * @return The float value stored by that register.
     */
    public static float getFloatFromRegister(int reg) {
        if (0 <= reg && reg < REGISTERS.length) {
            return Float.intBitsToFloat(REGISTERS[reg].getValue());
        }
        else {
            return 0.0f;
        }
    }

    /**
     * Gets the 32-bit int bit pattern stored in the given FPU register.
     *
     * @param reg Register to get the value of.
     * @return The int bit pattern stored by that register.
     */
    public static int getIntFromRegister(int reg) {
        if (0 <= reg && reg < REGISTERS.length) {
            return REGISTERS[reg].getValue();
        }
        else {
            return 0;
        }
    }

    /**
     * Gets the double value stored in the given FPU register.  The register
     * must be even-numbered.
     *
     * @param reg Register to get the value of. Must be even number of even/odd pair.
     * @throws InvalidRegisterAccessException if register ID is invalid or odd-numbered.
     */
    public static double getDoubleFromRegisterPair(int reg) throws InvalidRegisterAccessException {
        return Double.longBitsToDouble(getLongFromRegisterPair(reg));
    }

    /**
     * Gets a long representing the double value stored in the given double
     * precision FPU register.
     * The register must be even-numbered.
     *
     * @param reg Register to get the value of. Must be even number of even/odd pair.
     * @throws InvalidRegisterAccessException if register ID is invalid or odd-numbered.
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
     * This method updates the FPU register value whose number is given.  Note the
     * registers themselves hold an int value.  There are helper methods available
     * to which you can give a float or double to store.
     *
     * @param number FPU register to set the value of.
     * @param value The desired int value for the register.
     * @return The previous value of the register.
     */
    public static int updateRegister(int number, int value) {
        int previousValue;
        if (0 <= number && number < REGISTERS.length) {
            // Originally, this used a linear search to figure out which register to update.
            // Since all registers 0-31 are present in order, a simple array access should work.
            // Sean Clarke 03/2024
            previousValue = REGISTERS[number].setValue(value);
        }
        else {
            // Invalid register, do nothing
            return 0;
        }

        if (Application.isBackSteppingEnabled()) {
            Application.program.getBackStepper().addCoprocessor1Restore(number, previousValue);
        }

        return previousValue;
    }

    /**
     * Returns the value of the FPU register whose number is given.  Returns the
     * raw int value actually stored there.  If you need a float, use
     * Float.intBitsToFloat() to get the equivent float.
     *
     * @param number The FPU register number.
     * @return The int value of the given register.
     */
    public static int getValue(int number) {
        return REGISTERS[number].getValue();
    }

    /**
     * For getting the number representation of the FPU register.
     *
     * @param name The string formatted register name to look for.
     * @return The number of the register represented by the string, or -1 if not found.
     */
    public static int getRegisterNumber(String name) {
        Register register = getRegister(name);
        return (register == null) ? -1 : register.getNumber();
    }

    /**
     * For returning the set of registers.
     *
     * @return The set of registers.
     */
    public static Register[] getRegisters() {
        return REGISTERS;
    }

    /**
     * Get the register object corresponding to a given name.
     *
     * @param name The FPU register name, must be "$f0" through "$f31".
     * @return The register object, or null if not found.
     */
    public static Register getRegister(String name) {
        if (name.length() <= 2 || name.charAt(0) != '$' || name.charAt(1) != 'f') {
            return null;
        }
        try {
            // Check for register number 0-31
            return REGISTERS[Binary.decodeInteger(name.substring(2))]; // KENV 1/6/05
        }
        catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Method to reinitialize the values of the registers.
     */
    public static void reset() {
        for (Register register : REGISTERS) {
            register.resetValueToDefault();
        }
        clearConditionFlags();
    }

    /**
     * Set condition flag to 1 (true).
     *
     * @param flag condition flag number (0-7)
     */
    public static void setConditionFlag(int flag) {
        if (flag >= 0 && flag < CONDITION_FLAG_COUNT) {
            int oldFlagValue = Binary.bitValue(CONDITION_FLAGS.getValueNoNotify(), flag);
            CONDITION_FLAGS.setValue(Binary.setBit(CONDITION_FLAGS.getValueNoNotify(), flag));
            if (Application.isBackSteppingEnabled()) {
                if (oldFlagValue == 0) {
                    Application.program.getBackStepper().addConditionFlagClear(flag);
                }
                else {
                    Application.program.getBackStepper().addConditionFlagSet(flag);
                }
            }
        }
    }

    /**
     * Set condition flag to 0 (false).
     *
     * @param flag condition flag number (0-7)
     */
    public static void clearConditionFlag(int flag) {
        if (flag >= 0 && flag < CONDITION_FLAG_COUNT) {
            int oldFlagValue = Binary.bitValue(CONDITION_FLAGS.getValueNoNotify(), flag);
            CONDITION_FLAGS.setValue(Binary.clearBit(CONDITION_FLAGS.getValueNoNotify(), flag));
            if (Application.isBackSteppingEnabled()) {
                if (oldFlagValue == 0) {
                    Application.program.getBackStepper().addConditionFlagClear(flag);
                }
                else {
                    Application.program.getBackStepper().addConditionFlagSet(flag);
                }
            }
        }
    }

    /**
     * Get value of specified condition flag (0-7).
     *
     * @param flag condition flag number (0-7)
     * @return 0 if condition is false, 1 if condition is true
     */
    public static int getConditionFlag(int flag) {
        if (flag < 0 || flag >= CONDITION_FLAG_COUNT) {
            return 0;
        }
        return Binary.bitValue(CONDITION_FLAGS.getValue(), flag);
    }

    /**
     * Get bitfield of condition flags (0-7).
     *
     * @return Bitfield of condition flags.
     */
    public static int getConditionFlags() {
        return CONDITION_FLAGS.getValue();
    }

    /**
     * Clear all condition flags (0-7).
     */
    public static void clearConditionFlags() {
        // Set lowest 8 bits to 0
        CONDITION_FLAGS.setValue(CONDITION_FLAGS.getValueNoNotify() & 0xFFFFFF00);
    }

    /**
     * Set all condition flags (0-7).
     */
    public static void setConditionFlags() {
        // Set lowest 8 bits to 1
        CONDITION_FLAGS.setValue(CONDITION_FLAGS.getValueNoNotify() | 0x000000FF);
    }

    /**
     * Get count of condition flags.
     *
     * @return number of condition flags
     */
    public static int getConditionFlagCount() {
        return CONDITION_FLAG_COUNT;
    }
}
