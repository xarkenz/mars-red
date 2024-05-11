package mars.mips.instructions.syscalls;

import mars.Application;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.*;
import mars.simulator.ExceptionCause;

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
public class SyscallMessageDialogDouble extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    public SyscallMessageDialogDouble() {
        super(58, "MessageDialogDouble");
    }

    /**
     * System call to display a message to user.
     */
    @Override
    public void simulate(ProgramStatement statement) throws ProcessingException {
        // Input arguments:
        //   $a0 = address of null-terminated string that is an information-type message to user
        //   $f12 = double value to display in string form after the first message
        // Output: none

        String message;
        try {
            // Read a null-terminated string from memory
            message = Application.memory.getNullTerminatedString(RegisterFile.getValue(4));
        }
        catch (AddressErrorException exception) {
            throw new ProcessingException(statement, exception);
        }

        // Display the dialog
        try {
            JOptionPane.showMessageDialog(null, message + Coprocessor1.getDoubleFromRegisterPair("$f12"), null, JOptionPane.INFORMATION_MESSAGE);
        }
        catch (InvalidRegisterAccessException exception) {
            RegisterFile.updateRegister(5, -1);  // set $a1 to -1 flag
            throw new ProcessingException(statement, "Invalid register access during double input (syscall " + this.getNumber() + ")", ExceptionCause.SYSCALL_EXCEPTION);
        }
    }
}
