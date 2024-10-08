package mars.mips.instructions;

/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

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

import mars.assembler.OperandType;

import java.util.List;

/**
 * Class to represent a basic instruction in the MIPS instruction set.
 * Basic instruction means it translates directly to a 32-bit binary machine
 * instruction.
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003
 */
public class BasicInstruction extends Instruction {
    /**
     * These are the MIPS-defined formats of basic machine instructions.  The R-format indicates
     * the instruction works only with registers.  The I-format indicates the instruction
     * works with an immediate value (e.g. constant).  The J-format indicates this is a Jump
     * instruction.  The I-branch-format is defined by me, not MIPS, to to indicate this is
     * a Branch instruction, specifically to distinguish immediate
     * values used as target addresses.
     *
     * @author Pete Sanderson
     * @version August 2003
     */
    public enum Format {
        R_FORMAT,
        I_FORMAT,
        I_BRANCH_FORMAT,
        J_FORMAT,
    }

    private final Format format;
    private final SimulationFunction function;

    private final String operationMask;
    private final int opcodeMask;  // integer with 1's where constants required (0/1 become 1, f/s/t become 0)
    private final int opcodeMatch; // integer matching constants required (0/1 become 0/1, f/s/t become 0)

    /**
     * BasicInstruction constructor.
     *
     * @param example     An example usage of the instruction, as a String.
     * @param format The format is R, I, I-branch or J.
     * @param operMask    The opcode mask is a 32 character string that contains the opcode in binary in the appropriate bit positions and codes for operand positions ('f', 's', 't') in the remainding positions.
     * @param function     The inline definition of an object and class which anonymously implements the SimulationCode interface.
     */
    /* codes for operand positions are:
     * f == First operand
     * s == Second operand
     * t == Third operand
     * example: "add rd,rs,rt" is R format with fields in this order: opcode, rs, rt, rd, shamt, funct.
     *          Its opcode is 0, shamt is 0, funct is 0x40.  Based on operand order, its mask is
     *          "000000ssssstttttfffff00000100000", split into
     *          opcode |  rs   |  rt   |  rd   | shamt | funct
     *          000000 | sssss | ttttt | fffff | 00000 | 100000
     * This mask can be used at code generation time to map the assembly component to its
     * correct bit positions in the binary machine instruction.
     * It can also be used at runtime to match a binary machine instruction to the correct
     * instruction simulator -- it needs to match all and only the 0's and 1's.
     */
    public BasicInstruction(String mnemonic, List<OperandType> operandTypes, Format format, String title, String description, String operMask, SimulationFunction function) {
        super(mnemonic, operandTypes, title, description);
        this.format = format;
        this.function = function;

        this.operationMask = operMask.replaceAll(" ", ""); // squeeze out any/all spaces
        if (operationMask.length() != Instruction.INSTRUCTION_LENGTH_BITS) {
            System.out.println("mask not " + Instruction.INSTRUCTION_LENGTH_BITS + " bits!");
        }

        this.opcodeMask = Integer.parseUnsignedInt(this.operationMask.replaceAll("0", "1").replaceAll("[^1]", "0"), 2);
        this.opcodeMatch = Integer.parseUnsignedInt(this.operationMask.replaceAll("[^1]", "0"), 2);
    }

    /**
     * Get the operand format of the instruction.  MIPS defines 3 of these
     * R-format, I-format, and J-format.  R-format is all registers.  I-format
     * is address formed from register base with immediate offset.  J-format
     * is for jump destination addresses.  I have added one more:
     * I-branch-format, for branch destination addresses.  These are a variation
     * of the I-format in that the computed value is address relative to the
     * Program Counter.  All four formats are represented by static objects.
     *
     * @return The machine instruction format, R, I, J or I-branch.
     */
    public Format getFormat() {
        return this.format;
    }

    public SimulationFunction getFunction() {
        return this.function;
    }

    /**
     * Get the 32-character operation mask.  Each mask position represents a
     * bit position in the 32-bit machine instruction.  Operation codes and
     * unused bits are represented in the mask by 1's and 0's.  Operand codes
     * are represented by 'f', 's', and 't' for bits occupied by first, secon
     * and third operand, respectively.
     *
     * @return The 32 bit mask, as a String
     */
    public String getOperationMask() {
        return this.operationMask;
    }

    public int getOpcodeMask() {
        return this.opcodeMask;
    }

    public int getOpcodeMatch() {
        return this.opcodeMatch;
    }
}
