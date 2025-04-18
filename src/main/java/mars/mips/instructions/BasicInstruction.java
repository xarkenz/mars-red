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

import mars.assembler.AssemblerFlag;
import mars.assembler.Operand;
import mars.assembler.OperandType;

import java.util.ArrayList;
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
    private final InstructionFormat format;
    private final SimulationFunction function;
    private final boolean controlTransferInstruction;
    private final String encodingDescriptor;
    private final int operationMask; // integer with 1's where constants required (0/1 become 1, f/s/t become 0)
    private final int operationKey; // integer matching constants required (0/1 unchanged, f/s/t become 0)
    private final int[] operandMasks;
    private final int[] operandShifts;

    /**
     * Create a new <code>BasicInstruction</code>.
     *
     * @param mnemonic     The instruction mnemonic used in assembly code (case-insensitive).
     * @param operandTypes The list of operand types for this instruction, which is used to select a specific
     *                     instruction from the group of instructions sharing a mnemonic.
     * @param format       The general organization of this instruction's binary encoding, as interpreted by hardware.
     * @param isCTI        <code>true</code> if this is a Control Transfer Instruction (CTI), that is, a branch,
     *                     jump, or similar instruction that alters the Program Counter; <code>false</code> otherwise.
     * @param title        The "long name" of this instruction, which should relate to the mnemonic.
     * @param description  A short human-readable description of what this instruction does when executed.
     * @param encoding     A string describing the binary encoding for this instruction, with each character
     *                     representing one bit (although spaces may be used as visual separators). The characters
     *                     <code>0</code> and <code>1</code> indicate bit values that are part of the opcode, funct,
     *                     etc. used to identify the instruction; the characters <code>f</code> (first),
     *                     <code>s</code> (second), and <code>t</code> (third) are used to indicate bits derived from
     *                     the respective operands.
     * @param function The implementation of the instruction logic for simulation purposes.
     * @throws IllegalArgumentException Thrown if <code>encoding</code> is not a valid encoding descriptor string.
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
    public BasicInstruction(
        String mnemonic,
        List<OperandType> operandTypes,
        InstructionFormat format,
        boolean isCTI,
        String title,
        String description,
        String encoding,
        SimulationFunction function
    ) {
        super(mnemonic, operandTypes, title, description);
        this.format = format;
        this.controlTransferInstruction = isCTI;
        this.function = function;

        StringBuilder encodingDescriptor = new StringBuilder();
        int operationMask = 0;
        int operationKey = 0;
        this.operandMasks = new int[operandTypes.size()];
        this.operandShifts = new int[operandTypes.size()];

        for (char ch : encoding.toCharArray()) {
            // Ignore any spaces in the encoding string
            if (ch == ' ') {
                continue;
            }
            encodingDescriptor.append(ch);
            operationMask <<= 1;
            operationKey <<= 1;
            for (int index = 0; index < operandTypes.size(); index++) {
                this.operandMasks[index] <<= 1;
            }
            switch (ch) {
                case '0' -> {
                    operationMask |= 1;
                }
                case '1' -> {
                    operationMask |= 1;
                    operationKey |= 1;
                }
                case 'f' -> {
                    if (operandTypes.isEmpty()) {
                        throw new IllegalArgumentException("encoding for instruction " + mnemonic + operandTypes + " contains 'f' but has no first operand");
                    }
                    this.operandMasks[0] |= 1;
                }
                case 's' -> {
                    if (operandTypes.size() < 2) {
                        throw new IllegalArgumentException("encoding for instruction " + mnemonic + operandTypes + " contains 's' but has no second operand");
                    }
                    this.operandMasks[1] |= 1;
                }
                case 't' -> {
                    if (operandTypes.size() < 3) {
                        throw new IllegalArgumentException("encoding for instruction " + mnemonic + operandTypes + " contains 't' but has no third operand");
                    }
                    this.operandMasks[2] |= 1;
                }
                default -> {
                    throw new IllegalArgumentException("unexpected character '" + ch + "' in encoding for instruction " + mnemonic + operandTypes);
                }
            }
        }

        if (encodingDescriptor.length() != Instruction.INSTRUCTION_LENGTH_BITS) {
            throw new IllegalArgumentException("invalid " + encodingDescriptor.length() + "-bit encoding for instruction " + mnemonic + operandTypes);
        }

        // Determine the amount each operand is shifted from the rightmost position
        for (int index = 0; index < operandTypes.size(); index++) {
            int maskShiftTest = this.operandMasks[index];
            // Shift right until reaching a 1 bit, incrementing the shift amount each time
            while (maskShiftTest != 0 && (maskShiftTest & 1) == 0) {
                this.operandShifts[index]++;
                maskShiftTest >>>= 1;
            }
        }

        this.encodingDescriptor = encodingDescriptor.toString();
        this.operationMask = operationMask;
        this.operationKey = operationKey;
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
    public InstructionFormat getFormat() {
        return this.format;
    }

    public boolean isControlTransferInstruction() {
        return this.controlTransferInstruction;
    }

    /**
     * @return
     */
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
    public String getEncodingDescriptor() {
        return this.encodingDescriptor;
    }

    /**
     * @return
     */
    public int getOperationMask() {
        return this.operationMask;
    }

    /**
     * @return
     */
    public int getOperationKey() {
        return this.operationKey;
    }

    /**
     * Get length in bytes that this instruction requires in its binary form.
     *
     * @return int length in bytes of corresponding binary instruction(s).
     */
    @Override
    public int getSizeBytes() {
        int sizeBytes = BYTES_PER_INSTRUCTION;

        if (this.isControlTransferInstruction() && !AssemblerFlag.DELAYED_BRANCHING.isEnabled()) {
            // Extra NOP instruction will be inserted after this instruction
            sizeBytes += BYTES_PER_INSTRUCTION;
        }

        return sizeBytes;
    }

    /**
     * @param operands
     * @return
     */
    public int encodeOperands(List<Operand> operands) {
        int binary = this.operationKey;
        for (int operandIndex = 0; operandIndex < this.getOperandTypes().size(); operandIndex++) {
            int value = operands.get(operandIndex).getValue();
            binary |= (value << this.operandShifts[operandIndex]) & this.operandMasks[operandIndex];
        }
        return binary;
    }

    /**
     * @param binary
     * @return
     */
    public List<Operand> decodeOperands(int binary) {
        List<Operand> operands = new ArrayList<>(this.getOperandTypes().size());
        for (int operandIndex = 0; operandIndex < this.getOperandTypes().size(); operandIndex++) {
            OperandType type = this.getOperandTypes().get(operandIndex);
            int value = (binary & this.operandMasks[operandIndex]) >>> this.operandShifts[operandIndex];
            operands.add(new Operand(type, value));
        }
        return operands;
    }
}
