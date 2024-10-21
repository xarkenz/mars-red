package mars.assembler.extended;

import mars.assembler.Assembler;
import mars.assembler.BasicStatement;
import mars.assembler.Operand;
import mars.assembler.Statement;
import mars.assembler.syntax.StatementSyntax;
import mars.mips.instructions.ExtendedInstruction;

import java.util.List;

public class ExtendedStatement implements Statement {
    private final StatementSyntax syntax;
    private final ExtendedInstruction instruction;
    private final List<Operand> operands;
    private final List<BasicStatement> expansion;

    public ExtendedStatement(StatementSyntax syntax, ExtendedInstruction instruction, List<Operand> operands, List<BasicStatement> expansion) {
        this.syntax = syntax;
        this.instruction = instruction;
        this.operands = operands;
        this.expansion = expansion;
    }

    @Override
    public StatementSyntax getSyntax() {
        return this.syntax;
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

    @Override
    public void handlePlacement(Assembler assembler, int address) {
        for (BasicStatement basicStatement : this.expansion) {
            basicStatement.handlePlacement(assembler, address);
            address += basicStatement.getInstruction().getSizeBytes();
        }
    }
}
