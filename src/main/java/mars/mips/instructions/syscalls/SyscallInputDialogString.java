package mars.mips.instructions.syscalls;

import mars.simulator.SimulatorException;
import mars.assembler.BasicStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.Processor;

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
 * Service to input data.
 */
public class SyscallInputDialogString extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallInputDialogString() {
        super(54, "InputDialogString");
    }

    /**
     * System call to input data.
     */
    @Override
    public void simulate(BasicStatement statement) throws SimulatorException {
        this.requireGUI(statement);

        // Input arguments:
        //    $a0 = address of null-terminated string that is the message to user
        //    $a1 = address of input buffer for the input string
        //    $a2 = maximum number of characters to read
        // Outputs:
        //    $a1 contains status value
        //       0: Valid input data
        //       -2: Cancel was chosen
        //       -3: OK was chosen but no data had been input into field
        //       -4: Length of the input string exceeded the specified maximum

        String message;
        try {
            // Read a null-terminated string from memory
            message = Memory.getInstance().fetchNullTerminatedString(Processor.getValue(Processor.ARGUMENT_0));
        }
        catch (AddressErrorException exception) {
            throw new SimulatorException(statement, exception);
        }

        // Values returned by Java's InputDialog:
        // A null return value means that "Cancel" was chosen rather than OK.
        // An empty string returned (that is, inputString.length() of zero)
        // means that OK was chosen but no string was input.
        String inputString = JOptionPane.showInputDialog(message);
        if (inputString == null) {
            // Cancel was chosen
            Processor.setValue(Processor.ARGUMENT_1, -2);  // set $a1 to -2 flag
        }
        else if (inputString.isEmpty()) {
            // OK was chosen but there was no input
            Processor.setValue(Processor.ARGUMENT_1, -3);  // set $a1 to -3 flag
        }
        else {
            try {
                int byteAddress = Processor.getValue(Processor.ARGUMENT_1); // byteAddress of string is in $a1
                int maxLength = Processor.getValue(Processor.ARGUMENT_2); // input buffer size for input string is in $a2

                // The buffer will contain characters, a '\n' character, and the null character
                // Copy the input data to buffer as space permits
                for (int offset = 0; offset < inputString.length() && offset < maxLength - 1; offset++) {
                    Memory.getInstance().storeByte(byteAddress + offset, inputString.charAt(offset), true);
                }
                if (inputString.length() < maxLength - 1) {
                    // Newline at string end
                    Memory.getInstance().storeByte(byteAddress + Math.min(inputString.length(), maxLength - 2), '\n', true);
                }
                // Null terminator to end string
                Memory.getInstance().storeByte(byteAddress + Math.min((inputString.length() + 1), maxLength - 1), 0, true);

                if (inputString.length() > maxLength - 1) {
                    // Length of the input string exceeded the specified maximum
                    Processor.setValue(Processor.ARGUMENT_1, -4);  // set $a1 to -4 flag
                }
                else {
                    Processor.setValue(Processor.ARGUMENT_1, 0);  // set $a1 to 0 flag
                }
            }
            catch (AddressErrorException exception) {
                throw new SimulatorException(statement, exception);
            }
        }
    }
}
