package mars.mips.instructions.syscalls;

import mars.simulator.SimulatorException;
import mars.assembler.BasicStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.Processor;
import mars.simulator.ExceptionCause;
import mars.simulator.Simulator;

import java.nio.ByteBuffer;

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
 * Service to read from file descriptor given in $a0.  $a1 specifies buffer
 * and $a2 specifies length.  Number of characters read is returned in $v0.
 * (This was changed from $a0 in MARS 3.7 for SPIM compatibility.  The table
 * in the Computer Organization and Design book erroneously shows $a0.)
 */
public class SyscallRead extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallRead() {
        super(14, "Read");
    }

    /**
     * Performs syscall function to read from file descriptor given in $a0.  $a1 specifies buffer
     * and $a2 specifies length.  Number of characters read is returned in $v0 (starting MARS 3.7).
     */
    @Override
    public void simulate(BasicStatement statement) throws SimulatorException, InterruptedException {
        int descriptor = Processor.getValue(Processor.ARGUMENT_0); // $a0: file descriptor
        int byteAddress = Processor.getValue(Processor.ARGUMENT_1); // $a1: destination of characters to read from file
        int maxLength = Processor.getValue(Processor.ARGUMENT_2); // $a2: user-requested length

        if (maxLength < 0) {
            throw new SimulatorException(statement, "Length value in $a2 cannot be negative for " + this.getName() + " (syscall " + this.getNumber() + ")", ExceptionCause.SYSCALL);
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxLength);

        int readLength = Simulator.getInstance().getSystemIO().readFromFile(descriptor, buffer);
        Processor.updateRegister(Processor.VALUE_0, readLength); // Put return value in $v0

        // Flip buffer from put-mode to get-mode
        buffer.flip();

        // Copy bytes from intermediate buffer into MARS memory
        try {
            for (int index = 0; index < readLength; index++) {
                Memory.getInstance().storeByte(byteAddress++, buffer.get(index), true);
            }
        }
        catch (AddressErrorException exception) {
            throw new SimulatorException(statement, exception);
        }
    }
}