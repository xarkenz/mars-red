package mars.simulator;

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
 * Represents the cause of an error/interrupt that occurs during execution (simulation).
 *
 * @author Pete Sanderson
 * @version August 2005
 */
public class ExceptionCause {
    /*
     * The exception number is stored in coprocessor 0 cause register ($13)
     * Note: the codes for External Interrupts have been modified from MIPS
     * specs in order to encode two pieces of information.  According
     * to spec, there is one External Interrupt code, 0.  But then
     * how to distinguish keyboard interrupt from display interrupt?
     * The Cause register has Interrupt Pending bits that can be set.
     * Bit 8 represents keyboard, bit 9 represents display.  Those
     * bits are included into this code, but shifted right two positions
     * since the interrupt code will be shifted left two positions
     * for inserting cause code into bit positions 2-6 in Cause register.
     * DPS 23 July 2008.
     */
    public static final int EXTERNAL_INTERRUPT_KEYBOARD = 0x00000040; // see comment above.
    public static final int EXTERNAL_INTERRUPT_DISPLAY = 0x00000080; // see comment above.
    public static final int ADDRESS_EXCEPTION_FETCH = 4;
    public static final int ADDRESS_EXCEPTION_STORE = 5;
    public static final int SYSCALL_EXCEPTION = 8;
    public static final int BREAKPOINT_EXCEPTION = 9;
    public static final int RESERVED_INSTRUCTION_EXCEPTION = 10;
    public static final int ARITHMETIC_OVERFLOW_EXCEPTION = 12;
    public static final int TRAP_EXCEPTION = 13;
    // The following are from SPIM.
    public static final int DIVIDE_BY_ZERO_EXCEPTION = 15;
    public static final int FLOATING_POINT_OVERFLOW = 16;
    public static final int FLOATING_POINT_UNDERFLOW = 17;
}