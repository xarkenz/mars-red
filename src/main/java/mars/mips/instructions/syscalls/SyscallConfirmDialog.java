package mars.mips.instructions.syscalls;

import mars.SimulatorException;
import mars.assembler.BasicStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;

import javax.swing.*;

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
 * Service to display a message to user.
 */
public class SyscallConfirmDialog extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallConfirmDialog() {
        super(50, "ConfirmDialog");
    }

    /**
     * System call to display a message to user.
     */
    @Override
    public void simulate(BasicStatement statement) throws SimulatorException {
        // Input arguments: $a0 = address of null-terminated string that is the message to user
        // Output: $a0 contains value of user-chosen option
        //   0: Yes
        //   1: No
        //   2: Cancel

        try {
            // Read a null-terminated string from memory
            String message = Memory.getInstance().fetchNullTerminatedString(RegisterFile.getValue(4));

            // Update register $a0 with the value from showConfirmDialog
            RegisterFile.updateRegister(4, JOptionPane.showConfirmDialog(null, message));
        }
        catch (AddressErrorException exception) {
            throw new SimulatorException(statement, exception);
        }
    }
}
