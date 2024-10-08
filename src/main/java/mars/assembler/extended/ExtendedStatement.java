package mars.assembler.extended;

import mars.assembler.BasicStatement;
import mars.assembler.Operand;
import mars.assembler.SourceLine;
import mars.assembler.syntax.Syntax;
import mars.mips.instructions.ExtendedInstruction;

import java.util.List;

public class ExtendedStatement implements Syntax {
    private final SourceLine sourceLine;
    private final ExtendedInstruction instruction;
    private final List<Operand> operands;
    private final List<BasicStatement> expansion;

    public ExtendedStatement(SourceLine sourceLine, ExtendedInstruction instruction, List<Operand> operands, List<BasicStatement> expansion) {
        this.sourceLine = sourceLine;
        this.instruction = instruction;
        this.operands = operands;
        this.expansion = expansion;
    }

    @Override
    public SourceLine getSourceLine() {
        return this.sourceLine;
    }

    public ExtendedInstruction getInstruction() {
        return this.instruction;
    }

    public List<Operand> getOperands() {
        return this.operands;
    }

    public List<BasicStatement> getExpansion() {
        return this.expansion;
    }
}
