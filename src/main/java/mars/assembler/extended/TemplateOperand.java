package mars.assembler.extended;

import mars.assembler.Operand;
import mars.assembler.OperandType;

import java.util.List;

public interface TemplateOperand {
    OperandType getType();

    TemplateOperand withType(OperandType type);

    Operand resolve(List<Operand> originalOperands, int address);
}
