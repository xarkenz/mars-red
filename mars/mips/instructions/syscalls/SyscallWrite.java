package mars.mips.instructions.syscalls;

import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

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
 * and $a2 specifies length.  Number of characters written is returned in $v0
 * (this was changed from $a0 in MARS 3.7 for SPIM compatibility.  The table
 * in COD erroneously shows $a0).
 */
public class SyscallWrite extends AbstractSyscall {
    /**
     * Build an instance of the Write file syscall.  Default service number
     * is 15 and name is "Write".
     */
    public SyscallWrite() {
        super(15, "Write");
    }

    /**
     * Performs syscall function to write to file descriptor given in $a0.  $a1 specifies buffer
     * and $a2 specifies length.  Number of characters written is returned in $v0, starting in MARS 3.7.
     */
    public void simulate(ProgramStatement statement) throws ProcessingException {
        int fd = RegisterFile.getValue(4); // $a0: file descriptor
        int byteAddress = RegisterFile.getValue(5); // $a1: source of characters to write to file
        int maxLength = RegisterFile.getValue(6); // $a2: user-requested length

        byte[] buffer = new byte[maxLength + 1]; // Leave room for a possible null terminator

        try {
            byte byteValue = (byte) Globals.memory.getByte(byteAddress);
            // Stop at requested length, with no special treatment of null bytes
            int index;
            for (index = 0; index < maxLength; index++) {
                buffer[index] = byteValue;
                byteAddress++;
                byteValue = (byte) Globals.memory.getByte(byteAddress);
            }

            buffer[index] = 0; // Add null terminator
        }
        catch (AddressErrorException e) {
            throw new ProcessingException(statement, e);
        }

        int writtenLength = SystemIO.writeToFile(fd, buffer, maxLength);
        RegisterFile.updateRegister(2, writtenLength); // Put return value in $v0
    }
}