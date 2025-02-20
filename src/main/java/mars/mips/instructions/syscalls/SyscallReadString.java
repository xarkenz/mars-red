package mars.mips.instructions.syscalls;

import mars.simulator.SimulatorException;
import mars.assembler.BasicStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.Processor;
import mars.simulator.Simulator;

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
 * Service to read console input string into buffer starting at address in $a0.
 */
public class SyscallReadString extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallReadString() {
        super(8, "ReadString");
    }

    /**
     * Performs syscall function to read console input string into buffer starting at address in $a0.
     * Follows semantics of UNIX 'fgets'.  For specified length n,
     * string can be no longer than n-1. If less than that, add
     * newline to end.  In either case, then pad with null byte.
     */
    @Override
    public void simulate(BasicStatement statement) throws SimulatorException, InterruptedException {
        int buf = Processor.getValue(Processor.ARGUMENT_0); // buf addr in $a0
        int maxLength = Processor.getValue(Processor.ARGUMENT_1) - 1; // $a1
        boolean addNullByte = true;
        // Guard against negative maxLength.  DPS 13-July-2011
        if (maxLength < 0) {
            maxLength = 0;
            addNullByte = false;
        }

        String inputString = Simulator.getInstance().getSystemIO().readString(maxLength);

        int stringLength = Math.min(maxLength, inputString.length());
        try {
            for (int index = 0; index < stringLength; index++) {
                Memory.getInstance().storeByte(buf + index, inputString.charAt(index), true);
            }
            if (stringLength < maxLength) {
                Memory.getInstance().storeByte(buf + stringLength, '\n', true);
                stringLength++;
            }
            if (addNullByte) {
                Memory.getInstance().storeByte(buf + stringLength, 0, true);
            }
        }
        catch (AddressErrorException exception) {
            throw new SimulatorException(statement, exception);
        }
    }
}