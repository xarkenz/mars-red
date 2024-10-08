package mars.assembler;

import mars.assembler.extended.TemplateOperand;

import java.util.List;

public class Operand implements TemplateOperand {
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
    public TemplateOperand withType(OperandType type) {
        return new Operand(type, this.value);
    }

    @Override
    public Operand resolve(List<Operand> originalOperands, int address) {
        return this;
    }
}
