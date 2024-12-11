package mars.mips.instructions;

import mars.assembler.OperandType;
import mars.mips.hardware.Memory;

import java.util.ArrayList;
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

    private final String mnemonic;
    private final List<OperandType> operandTypes;
    private final String title;
    private final String description;
    private final List<String> exampleOperands;
    private final String alignedExampleSyntax;
    private final String exampleSyntax;

    protected Instruction(String mnemonic, List<OperandType> operandTypes, String title, String description) {
        this.mnemonic = mnemonic;
        this.operandTypes = operandTypes;
        this.title = title;
        this.exampleOperands = generateExampleOperands(mnemonic, operandTypes);
        this.alignedExampleSyntax = formatSyntax(mnemonic, operandTypes, this.exampleOperands, true);
        this.exampleSyntax = formatSyntax(mnemonic, operandTypes, this.exampleOperands, false);
        for (int operandIndex = 0; operandIndex < this.exampleOperands.size(); operandIndex++) {
            description = description.replace("{" + operandIndex + "}", this.exampleOperands.get(operandIndex));
        }
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

    public List<String> getExampleOperands() {
        return this.exampleOperands;
    }

    public String getAlignedExampleSyntax() {
        return this.alignedExampleSyntax;
    }

    public String getExampleSyntax() {
        return this.exampleSyntax;
    }

    /**
     * Get length in bytes that this instruction requires in its binary form.
     *
     * @return int length in bytes of corresponding binary instruction(s).
     */
    public abstract int getSizeBytes();

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

    public boolean acceptsOperandsLoosely(List<OperandType> givenTypes) {
        if (givenTypes.size() != this.operandTypes.size()) {
            return false;
        }

        for (int index = 0; index < this.operandTypes.size(); index++) {
            if (!this.operandTypes.get(index).acceptsLoosely(givenTypes.get(index))) {
                return false;
            }
        }

        return true;
    }

    public static String formatSyntax(String mnemonic, List<OperandType> operandTypes, List<String> operands, boolean align) {
        if (operands.isEmpty()) {
            return mnemonic;
        }

        StringBuilder syntax = new StringBuilder(mnemonic);

        syntax.append(' ');
        if (align) {
            while (syntax.length() < 8) {
                syntax.append(' ');
            }
        }

        return syntax + formatOperands(operandTypes, operands);
    }

    public static String formatOperands(List<OperandType> operandTypes, List<String> operands) {
        if (operands.isEmpty()) {
            return "";
        }

        StringBuilder syntax = new StringBuilder();

        if (operandTypes.get(0) == OperandType.PAREN_REGISTER) {
            syntax.append('(').append(operands.get(0)).append(')');
        }
        else {
            syntax.append(operands.get(0));
        }

        for (int index = 1; index < operands.size(); index++) {
            if (operandTypes.get(index) == OperandType.PAREN_REGISTER) {
                syntax.append('(').append(operands.get(index)).append(')');
            }
            else {
                syntax.append(", ").append(operands.get(index));
            }
        }

        return syntax.toString();
    }

    public static List<String> generateExampleOperands(String mnemonic, List<OperandType> operandTypes) {
        List<String> exampleOperands = new ArrayList<>(operandTypes.size());

        for (int index = 0; index < operandTypes.size(); index++) {
            exampleOperands.add(switch (operandTypes.get(index)) {
                case INTEGER_3_UNSIGNED -> "1";
                case INTEGER_5_UNSIGNED -> "10";
                case INTEGER_15_UNSIGNED, INTEGER_16_UNSIGNED, INTEGER_16 -> "100";
                case INTEGER_16_SIGNED -> "-100";
                case INTEGER_32 -> "100000";
                case REGISTER, PAREN_REGISTER -> "$t" + index;
                case FP_REGISTER -> "$f" + (
                    (isDoublePrecision(mnemonic, operandTypes, index))
                        ? 2 * index + 2 // Even-numbered
                        : 2 * index + 1 // Odd-numbered
                );
                case LABEL -> "label";
                case LABEL_OFFSET -> "label+100000";
                case BRANCH_OFFSET, JUMP_LABEL -> "target";
            });
        }

        return exampleOperands;
    }

    private static boolean isDoublePrecision(String mnemonic, List<OperandType> operandTypes, int index) {
        return mnemonic.contains(".d")
            && (!mnemonic.contains(".d.s")
                || (index + 1 < operandTypes.size() && operandTypes.get(index + 1) == OperandType.FP_REGISTER))
            && (!mnemonic.contains(".s.d")
                || (index - 1 >= 0 && operandTypes.get(index - 1) == OperandType.FP_REGISTER));
    }
}
