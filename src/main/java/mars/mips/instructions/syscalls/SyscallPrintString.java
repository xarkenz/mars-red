package mars.mips.instructions.syscalls;

import mars.simulator.SimulatorException;
import mars.assembler.BasicStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
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
 * Service to display string stored starting at address in $a0 onto the console.
 */
public class SyscallPrintString extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallPrintString() {
        super(4, "PrintString");
    }

    /**
     * Performs syscall function to print string stored starting at address in $a0.
     */
    @Override
    public void simulate(BasicStatement statement) throws SimulatorException {
        try {
            // Read a null-terminated string from memory
            String stringValue = Memory.getInstance().fetchNullTerminatedString(RegisterFile.getValue(4));
            Simulator.getInstance().getSystemIO().printString(stringValue);
        }
        catch (AddressErrorException exception) {
            throw new SimulatorException(statement, exception);
        }
    }
}