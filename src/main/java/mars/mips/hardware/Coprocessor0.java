package mars.mips.hardware;

import mars.simulator.ExceptionCause;
import mars.simulator.Simulator;
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
 * @author Pete Sanderson, August 2005
 */
public class Coprocessor0 {
    public static final int BAD_V_ADDR = 8;
    public static final int STATUS = 12;
    public static final int CAUSE = 13;
    public static final int EPC = 14;

    public static final int EXL_BIT = 1;
    // bit position in STATUS register
    // bits 8-15 (mask for interrupt levels) all set, bit 4 (user mode) set,
    // bit 1 (exception level) not set, bit 0 (interrupt enable) set.
    public static final int DEFAULT_STATUS_VALUE = 0b00000000_00000000_11111111_00010001;

    private static final Register[] REGISTERS = {
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        new Register("BadVAddr", BAD_V_ADDR, 0),
        null,
        null,
        null,
        new Register("Status", STATUS, DEFAULT_STATUS_VALUE),
        new Register("Cause", CAUSE, 0),
        new Register("EPC", EPC, 0),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
    };

    /**
     * This method updates the register value whose number is given.
     *
     * @param number Number of register to set the value of.
     * @param value  The desired value for the register.
     * @return old value in register prior to update
     */
    public static int updateRegister(int number, int value) {
        if (REGISTERS[number] != null) {
            int previousValue = REGISTERS[number].setValue(value);

            Simulator.getInstance().getBackStepper().registerChanged(REGISTERS[number], previousValue);

            return previousValue;
        }
        else {
            return 0;
        }
    }

    /**
     * Returns the value of the register whose number is given.
     *
     * @param number The register number.
     * @return The value of the given register.  0 for non-implemented registers
     */
    public static int getValue(int number) {
        if (REGISTERS[number] != null) {
            return REGISTERS[number].getValue();
        }
        else {
            return 0;
        }
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
     * Method to reinitialize the values of the registers.
     */
    public static void reset() {
        for (Register register : REGISTERS) {
            if (register != null) {
                register.resetValueToDefault();
            }
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
        // Set EPC (Exception Program Counter) to the address of the instruction that caused the error
        updateRegister(EPC, Processor.getExecuteProgramCounter());
        // Set EXL (Exception Level) bit, bit position 1, in STATUS register to 1.
        updateRegister(STATUS, Binary.setBit(getValue(STATUS), EXL_BIT));
    }

    /**
     * Given MIPS exception cause code and bad address, place the bad address into VADDR
     * register ($8), place the cause code into the CAUSE register ($13), set the EPC register ($14) to
     * "current" program counter, and set Exception Level bit in STATUS register ($12).
     *
     * @param cause   The cause code (see {@link Coprocessor0} for a list). In this case, should probably be
     *                {@link ExceptionCause#ADDRESS_FETCH} or {@link ExceptionCause#ADDRESS_STORE}.
     * @param address The address that caused the exception.
     * @author Pete Sanderson, August 2005
     */
    public static void updateRegisters(int cause, int address) {
        updateRegister(BAD_V_ADDR, address);
        updateRegisters(cause);
    }
}
