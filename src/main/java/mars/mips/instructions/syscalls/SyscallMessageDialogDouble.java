package mars.mips.instructions.syscalls;

import mars.simulator.SimulatorException;
import mars.assembler.BasicStatement;
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
    @SuppressWarnings("unused")
    public SyscallMessageDialogDouble() {
        super(58, "MessageDialogDouble");
    }

    /**
     * System call to display a message to user.
     */
    @Override
    public void simulate(BasicStatement statement) throws SimulatorException {
        // Input arguments:
        //   $a0 = address of null-terminated string that is an information-type message to user
        //   $f12 = double value to display in string form after the first message
        // Output: none

        try {
            // Read a null-terminated string from memory
            String message = Memory.getInstance().fetchNullTerminatedString(Processor.getValue(Processor.ARGUMENT_0));

            double doubleValue = Coprocessor1.getDoubleFloat(12);

            // Display the dialog
            JOptionPane.showMessageDialog(null, message + doubleValue, null, JOptionPane.INFORMATION_MESSAGE);
        }
        catch (AddressErrorException exception) {
            throw new SimulatorException(statement, exception);
        }
        catch (InvalidRegisterAccessException exception) {
            // This should not occur because $f12 is always a valid double target
            throw new SimulatorException(statement, "internal error reading double from register (syscall " + this.getNumber() + ")", ExceptionCause.SYSCALL);
        }
    }
}
