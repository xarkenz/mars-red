package mars.simulator;

import mars.assembler.BasicStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Processor;
import mars.mips.instructions.Instruction;
import mars.util.Binary;

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
 * Class to represent error that occurs while assembling or running a MIPS program.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public class SimulatorException extends Exception {
    private Integer exitCode = null;

    /**
     * Constructor for ProcessingException.
     *
     * @param message
     */
    public SimulatorException(String message) {
        super(message);
    }

    /**
     * Constructor for ProcessingException.
     *
     * @param exception AddressErrorException object containing specialized error message, cause, address
     */
    public SimulatorException(AddressErrorException exception) {
        super(exception);
        Coprocessor0.updateRegisters(exception.getType(), exception.getAddress());
    }

    /**
     * Constructor for ProcessingException to handle runtime exceptions
     *
     * @param statement BasicStatement of statement causing runtime exception
     * @param message   String containing specialized error message
     */
    public SimulatorException(BasicStatement statement, String message) {
        super(
            "Runtime exception at " + Binary.intToHexString(
                Processor.getProgramCounter() - Instruction.BYTES_PER_INSTRUCTION
            ) + ": " + message
        );
    }

    /**
     * Constructor for ProcessingException to handle runtime exceptions
     *
     * @param statement BasicStatement of statement causing runtime exception
     * @param message   String containing specialized error message
     * @param cause     exception cause (see Exceptions class for list)
     */
    public SimulatorException(BasicStatement statement, String message, int cause) {
        this(statement, message);
        Coprocessor0.updateRegisters(cause);
    }

    /**
     * Constructor for ProcessingException to handle address runtime exceptions
     *
     * @param statement a BasicStatement of statement causing runtime exception
     * @param exception AddressErrorException object containing specialized error message, cause, address
     */
    public SimulatorException(BasicStatement statement, AddressErrorException exception) {
        this(statement, exception.getMessage());
        Coprocessor0.updateRegisters(exception.getType(), exception.getAddress());
    }

    /**
     * Constructor for ProcessingException.
     * <p>
     * No error list, but allows passing of exit code (i.e., syscall 17
     * for exiting with exit code).
     *
     * @param exitCode exit code
     */
    public SimulatorException(int exitCode) {
        super();
        this.exitCode = exitCode;
    }

    /**
     * Constructor for ProcessingException.
     * <p>
     * No parameter and thus no error list.  Use this for normal MIPS
     * program termination (e.g. syscall 10 for exit).
     */
    public SimulatorException() {
        super();
        this.exitCode = 0;
    }

    /**
     * Produce the error code passed by the program.
     *
     * @return Returns the error code.
     */
    public Integer getExitCode() {
        return this.exitCode;
    }
}
