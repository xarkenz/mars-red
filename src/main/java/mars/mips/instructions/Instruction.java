package mars.mips.instructions;

import mars.assembler.OperandType;
import mars.mips.hardware.Memory;

import java.util.List;

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
 * Base class to represent member of MIPS instruction set.
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003
 */
public abstract class Instruction {
    /**
     * Length in bytes of a machine instruction.
     */
    public static final int BYTES_PER_INSTRUCTION = Memory.BYTES_PER_WORD;
    public static final int INSTRUCTION_LENGTH_BITS = BYTES_PER_INSTRUCTION * Byte.SIZE;
    /**
     * Characters used in instruction mask to indicate bit positions
     * for 'f'irst, 's'econd, and 't'hird operands.
     */
    public static final char[] OPERAND_MASK = {'f', 's', 't'};

    protected final String mnemonic;
    protected final List<OperandType> operandTypes;
    protected final String title;
    protected final String description;

    protected Instruction(String mnemonic, List<OperandType> operandTypes, String title, String description) {
        this.mnemonic = mnemonic;
        this.operandTypes = operandTypes;
        this.title = title;
        this.description = description;
    }

    /**
     * Get operation mnemonic
     *
     * @return operation mnemonic (e.g. addi, sw)
     */
    public String getMnemonic() {
        return this.mnemonic;
    }

    public List<OperandType> getOperandTypes() {
        return this.operandTypes;
    }

    public String getTitle() {
        return this.title;
    }

    /**
     * Get string describing the instruction.  This is not used internally by
     * MARS, but is for display to the user.
     *
     * @return String describing the instruction.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Get length in bytes that this instruction requires in its binary form.
     *
     * @return int length in bytes of corresponding binary instruction(s).
     */
    public int getSizeBytes() {
        return BYTES_PER_INSTRUCTION;
    }

    public boolean acceptsOperands(List<OperandType> givenTypes) {
        if (givenTypes.size() != this.operandTypes.size()) {
            return false;
        }

        for (int index = 0; index < this.operandTypes.size(); index++) {
            if (!this.operandTypes.get(index).accepts(givenTypes.get(index))) {
                return false;
            }
        }

        return true;
    }

    public String generateExample() {
        // TODO
        return this.mnemonic + this.operandTypes;
    }
}
