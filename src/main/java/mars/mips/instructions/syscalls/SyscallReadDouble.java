package mars.mips.instructions.syscalls;

import mars.simulator.SimulatorException;
import mars.assembler.BasicStatement;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.InvalidRegisterAccessException;
import mars.simulator.ExceptionCause;
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
 * Service to read the bits of console input double into $f0 and $f1.
 * $f1 contains high order word of the double.
 */
public class SyscallReadDouble extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallReadDouble() {
        super(7, "ReadDouble");
    }

    /**
     * Performs syscall function to read the bits of input double into $f0 and $f1.
     */
    @Override
    public void simulate(BasicStatement statement) throws SimulatorException, InterruptedException {
        try {
            double doubleValue = Simulator.getInstance().getSystemIO().readDouble();

            Coprocessor1.setRegisterPairToDouble(0, doubleValue);
        }
        catch (NumberFormatException exception) {
            throw new SimulatorException(statement, "invalid double input (syscall " + this.getNumber() + ")", ExceptionCause.SYSCALL_EXCEPTION);
        }
        catch (InvalidRegisterAccessException exception) {
            // This should not occur because $f0 is always a valid double target
            throw new SimulatorException(statement, "internal error writing double to register (syscall " + this.getNumber() + ")", ExceptionCause.SYSCALL_EXCEPTION);
        }
    }
}