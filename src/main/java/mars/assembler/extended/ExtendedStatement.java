package mars.assembler.extended;

import mars.Application;
import mars.assembler.*;
import mars.assembler.syntax.StatementSyntax;
import mars.mips.instructions.ExtendedInstruction;
import mars.mips.instructions.BasicInstruction;

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
        BasicInstruction lastInstruction = null;
        for (BasicStatement basicStatement : this.expansion) {
            assembler.placeStatement(basicStatement, address);
            address += BasicInstruction.BYTES_PER_INSTRUCTION;
            lastInstruction = basicStatement.getInstruction();
        }

        if (lastInstruction != null
            && lastInstruction.isControlTransferInstruction() && !AssemblerFlag.DELAYED_BRANCHING.isEnabled()
        ) {
            // Insert an extra NOP after this statement to avoid pipelining conflicts
            BasicStatement nop = Application.instructionSet.getDecoder().decodeStatement(0);
            assembler.placeStatement(nop, address);
        }
    }
}
