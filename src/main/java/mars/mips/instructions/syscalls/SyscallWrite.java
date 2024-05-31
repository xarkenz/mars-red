package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
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
 * Service to write to file descriptor given in $a0.  $a1 specifies buffer
 * and $a2 specifies length.  Number of characters written is returned in $v0.
 * (This was changed from $a0 in MARS 3.7 for SPIM compatibility.  The table
 * in the Computer Organization and Design book erroneously shows $a0.)
 */
public class SyscallWrite extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallWrite() {
        super(15, "Write");
    }

    /**
     * Performs syscall function to write to file descriptor given in $a0.  $a1 specifies buffer
     * and $a2 specifies length.  Number of characters written is returned in $v0, starting in MARS 3.7.
     */
    @Override
    public void simulate(ProgramStatement statement) throws ProcessingException {
        int descriptor = RegisterFile.getValue(4); // $a0: file descriptor
        int byteAddress = RegisterFile.getValue(5); // $a1: source of characters to write to file
        int maxLength = RegisterFile.getValue(6); // $a2: user-requested length

        if (maxLength < 0) {
            throw new ProcessingException(statement, "Length value in $a2 cannot be negative for " + this.getName() + " (syscall " + this.getNumber() + ")");
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxLength + 1); // Leave room for a possible null terminator

        try {
            // Stop at requested length, with no special treatment of null bytes
            for (int index = 0; index < maxLength; index++, byteAddress++) {
                buffer.put((byte) Memory.getInstance().fetchByte(byteAddress, false));
            }
            // Add null terminator
            // Probably not needed? - CW
            // buffer.put((byte) 0);
        }
        catch (AddressErrorException exception) {
            throw new ProcessingException(statement, exception);
        }

        // Flip buffer from put-mode to get-mode
        buffer.flip();

        int writtenLength = Simulator.getInstance().getSystemIO().writeToFile(descriptor, buffer);
        RegisterFile.updateRegister(2, writtenLength); // Put return value in $v0
    }
}