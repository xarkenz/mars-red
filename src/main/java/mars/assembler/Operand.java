package mars.assembler;

import mars.assembler.extended.TemplateOperand;
import mars.assembler.syntax.SyntaxOperand;

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

    @Override
    public Operand resolve(Assembler assembler) {
        return this;
    }

    @Override
    public TemplateOperand withType(OperandType type) {
        return new Operand(type, this.value);
    }

    @Override
    public Operand resolve(List<Operand> originalOperands, int address) {
        return this;
    }
}
