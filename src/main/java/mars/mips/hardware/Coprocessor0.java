package mars.mips.hardware;

import mars.Application;
import mars.mips.instructions.Instruction;
import mars.simulator.ExceptionCause;
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
 * Represents Coprocessor 0.  We will use only its interrupt/exception registers.
 *
 * @author Pete Sanderson
 * @version August 2005
 */
public class Coprocessor0 {
    public static final int VADDR = 8;
    public static final int STATUS = 12;
    public static final int CAUSE = 13;
    public static final int EPC = 14;

    public static final int EXCEPTION_LEVEL = 1;
    // bit position in STATUS register
    // bits 8-15 (mask for interrupt levels) all set, bit 4 (user mode) set,
    // bit 1 (exception level) not set, bit 0 (interrupt enable) set.
    public static final int DEFAULT_STATUS_VALUE = 0x0000FF11;

    private static final Register[] REGISTERS = {
        new Register("vaddr", 8, 0),
        new Register("status", 12, DEFAULT_STATUS_VALUE),
        new Register("cause", 13, 0),
        new Register("epc", 14, 0),
    };

    /**
     * Sets the value of the register given to the value given.
     *
     * @param number The name of register to set the value of ($number, where number is register number).
     * @param value  The desired value for the register.
     * @return old value in register prior to update
     */
    public static int updateRegister(String number, int value) {
        for (Register register : REGISTERS) {
            if (("$" + register.getNumber()).equals(number) || register.getName().equals(number)) {
                return register.setValue(value);
            }
        }
        return 0;
    }

    /**
     * This method updates the register value whose number is given.
     *
     * @param number Number of register to set the value of.
     * @param value  The desired value for the register.
     * @return old value in register prior to update
     */
    public static int updateRegister(int number, int value) {
        for (Register register : REGISTERS) {
            if (register.getNumber() == number) {
                int previousValue = register.setValue(value);

                if (Application.isBackSteppingEnabled()) {
                    Application.program.getBackStepper().addCoprocessor0Restore(number, previousValue);
                }

                return previousValue;
            }
        }
        return 0;
    }

    /**
     * Returns the value of the register whose number is given.
     *
     * @param number The register number.
     * @return The value of the given register.  0 for non-implemented registers
     */
    public static int getValue(int number) {
        for (Register register : REGISTERS) {
            if (register.getNumber() == number) {
                return register.getValue();
            }
        }
        return 0;
    }

    /**
     * For getting the number representation of the register.
     *
     * @param name The string formatted register name to look for.
     * @return The number of the register represented by the string. -1 if no match.
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
     * Coprocessor0 implements only selected registers, so the register number
     * (8, 12, 13, 14) does not correspond to its position in the list of registers
     * (0, 1, 2, 3).
     *
     * @param register A Coprocessor0 register
     * @return the list position of given register, -1 if not found.
     */
    public static int getRegisterPosition(Register register) {
        for (int index = 0; index < REGISTERS.length; index++) {
            if (REGISTERS[index] == register) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Get register object corresponding to given name.  If no match, return null.
     *
     * @param name The register name, in $0 format.
     * @return The register object, or null if not found.
     */
    public static Register getRegister(String name) {
        for (Register register : REGISTERS) {
            if (("$" + register.getNumber()).equals(name) || register.getName().equals(name)) {
                return register;
            }
        }
        return null;
    }

    /**
     * Method to reinitialize the values of the registers.
     */
    public static void reset() {
        for (Register register : REGISTERS) {
            register.resetValueToDefault();
        }
    }

    /**
     * Given MIPS exception cause code, place that code into
     * coprocessor 0 CAUSE register ($13), set the EPC register ($14) to
     * "current" program counter, and set Exception Level bit in STATUS register ($12).
     *
     * @param cause The cause code (see {@link Coprocessor0} for a list)
     * @author Pete Sanderson, August 2005
     */
    public static void updateRegisters(int cause) {
        // Set CAUSE register bits 2 thru 6 to cause value.  The "& 0xFFFFFC83" will set bits 2-6 and 8-9 to 0 while
        // keeping all the others.  Left-shift by 2 to put cause value into position then OR it in.  Bits 8-9 used to
        // identify devices for External Interrupt (8=keyboard, 9=display).
        updateRegister(CAUSE, (getValue(CAUSE) & 0xFFFFFC83) | (cause << 2));
        // When exception occurred, PC had already been incremented so need to subtract 4 here.
        updateRegister(EPC, RegisterFile.getProgramCounter() - Instruction.BYTES_PER_INSTRUCTION);
        // Set EXL (Exception Level) bit, bit position 1, in STATUS register to 1.
        updateRegister(STATUS, Binary.setBit(getValue(STATUS), EXCEPTION_LEVEL));
    }

    /**
     * Given MIPS exception cause code and bad address, place the bad address into VADDR
     * register ($8), place the cause code into the CAUSE register ($13), set the EPC register ($14) to
     * "current" program counter, and set Exception Level bit in STATUS register ($12).
     *
     * @param cause   The cause code (see {@link Coprocessor0} for a list). In this case, should probably be
     *                {@link ExceptionCause#ADDRESS_EXCEPTION_FETCH} or {@link ExceptionCause#ADDRESS_EXCEPTION_STORE}.
     * @param address The address that caused the exception.
     * @author Pete Sanderson, August 2005
     */
    public static void updateRegisters(int cause, int address) {
        updateRegister(VADDR, address);
        updateRegisters(cause);
    }
}
