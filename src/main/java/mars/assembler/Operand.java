package mars.assembler;

import mars.assembler.extended.TemplateOperand;
import mars.assembler.syntax.StatementSyntax;
import mars.assembler.syntax.SyntaxOperand;
import mars.mips.instructions.Instruction;
import mars.util.Binary;

import java.util.List;

public class Operand implements SyntaxOperand, TemplateOperand {
    private final OperandType type;
    private final int value;

    public Operand(OperandType type, int value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public OperandType getType() {
        return this.type;
    }

    public int getValue() {
        return this.value;
    }

    @Override // SyntaxOperand
    public Operand resolve(Assembler assembler) {
        return this;
    }

    @Override // TemplateOperand
    public TemplateOperand withType(OperandType type) {
        return new Operand(type, this.value);
    }

    @Override // TemplateOperand
    public Operand resolve(List<Operand> originalOperands, int address) {
        return this;
    }

    public Operand convertToType(OperandType targetType, StatementSyntax syntax, Assembler assembler, int address) {
        // Don't perform any conversion if we already have the type we want
        if (this.type == targetType) {
            return this;
        }

        switch (targetType) {
            case BRANCH_OFFSET -> {
                if (this.type.isInteger()) {
                    // Leave value as-is
                    return new Operand(targetType, this.value);
                }
                else {
                    // Ensure the target address is properly aligned
                    if ((this.value & (Instruction.BYTES_PER_INSTRUCTION - 1)) != 0) {
                        assembler.logError(syntax.getSourceLine().getLocation(), "Cannot branch to improperly aligned address " + Binary.intToHexString(this.value));
                    }
                    // Compute the branch offset in words
                    int wordOffset = (this.value - (address + Instruction.BYTES_PER_INSTRUCTION)) >> 2;
                    // Ensure the branch offset is in range
                    if (wordOffset < -0x8000 || wordOffset > 0x7FFF) {
                        assembler.logError(syntax.getSourceLine().getLocation(), "Cannot branch to address " + Binary.intToHexString(this.value) + " from address " + Binary.intToHexString(address) + "; offset is out of 16-bit range, try a jump instead");
                    }
                    // This word offset is our resulting value
                    return new Operand(targetType, wordOffset);
                }
            }
            case JUMP_LABEL -> {
                // Ensure the target address is properly aligned
                if ((this.value & (Instruction.BYTES_PER_INSTRUCTION - 1)) != 0) {
                    assembler.logError(syntax.getSourceLine().getLocation(), "Cannot jump to improperly aligned address " + Binary.intToHexString(this.value));
                }
                // Ensure the highest 4 bits of the current address and target address match
                if (this.value >>> (Integer.SIZE - 4) != address >>> (Integer.SIZE - 4)) {
                    assembler.logError(syntax.getSourceLine().getLocation(), "Cannot jump to address " + Binary.intToHexString(this.value) + " from address " + Binary.intToHexString(address) + "; highest 4 bits of addresses conflict, try a jump register instead");
                }
                // Strip the lowest 2 bits
                return new Operand(targetType, this.value >> 2);
            }
            default -> {
                // Leave value as-is
                return new Operand(targetType, this.value);
            }
        }
    }
}
