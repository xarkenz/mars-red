package mars.assembler.extended;

import mars.assembler.Operand;
import mars.assembler.OperandType;

import java.util.List;

public class OperandSubstitution implements TemplateOperand {
    private final int operandIndex;
    private final List<OperandModifier> modifiers;
    private final OperandType type;

    public OperandSubstitution(int operandIndex, List<OperandModifier> modifiers, OperandType originalType) {
        this.operandIndex = operandIndex;
        this.modifiers = modifiers;

        OperandType type = originalType;
        for (OperandModifier modifier : modifiers) {
            type = modifier.getType(type);
        }
        this.type = type;
    }

    public int getOperandIndex() {
        return this.operandIndex;
    }

    public List<OperandModifier> getModifiers() {
        return this.modifiers;
    }

    @Override
    public OperandType getType() {
        return this.type;
    }

    @Override
    public TemplateOperand withType(OperandType type) {
        return new OperandSubstitution(this.operandIndex, this.modifiers, type);
    }

    public Operand applyModifiers(Operand operand, int address) {
        for (OperandModifier modifier : this.modifiers) {
            operand = modifier.apply(operand, address);
        }
        return operand;
    }

    @Override
    public Operand resolve(List<Operand> originalOperands, int address) {
        Operand operand = originalOperands.get(this.operandIndex);
        return this.applyModifiers(operand, address);
    }
}
