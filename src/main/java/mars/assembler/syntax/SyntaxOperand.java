package mars.assembler.syntax;

import mars.assembler.Assembler;
import mars.assembler.Operand;
import mars.assembler.OperandType;

public interface SyntaxOperand {
    OperandType getType();

    Operand resolve(Assembler assembler);
}
