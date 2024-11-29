package mars.assembler.extended;

import mars.assembler.Assembler;
import mars.assembler.AssemblerFlag;
import mars.assembler.BasicStatement;
import mars.assembler.Operand;
import mars.assembler.syntax.StatementSyntax;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.ExtendedInstruction;

import java.util.ArrayList;
import java.util.List;

public class ExpansionTemplate {
    public interface Statement {
        BasicInstruction getInstruction();

        BasicStatement resolve(List<Operand> originalOperands, StatementSyntax syntax, Assembler assembler, int address);
    }

    private final ExtendedInstruction instruction;
    private final List<Statement> statements;

    public ExpansionTemplate(ExtendedInstruction instruction, List<Statement> statements) {
        this.instruction = instruction;
        this.statements = statements;
    }

    public ExtendedInstruction getInstruction() {
        return this.instruction;
    }

    public List<Statement> getStatements() {
        return this.statements;
    }

    public int getSizeBytes() {
        int sizeBytes = 0;

        BasicInstruction lastInstruction = null;
        for (Statement statement : this.statements) {
            BasicInstruction instruction = statement.getInstruction();
            if (instruction != null) {
                sizeBytes += BasicInstruction.BYTES_PER_INSTRUCTION;
                lastInstruction = instruction;
            }
        }

        if (lastInstruction != null
            && lastInstruction.isControlTransferInstruction() && !AssemblerFlag.DELAYED_BRANCHING.isEnabled()
        ) {
            // Extra NOP instruction will be inserted after this instruction
            sizeBytes += BasicInstruction.BYTES_PER_INSTRUCTION;
        }

        return sizeBytes;
    }

    public ExtendedStatement resolve(List<Operand> operands, StatementSyntax syntax, Assembler assembler, int address) {
        List<BasicStatement> expansion = new ArrayList<>(this.statements.size());

        for (Statement statement : this.statements) {
            BasicStatement resolvedStatement = statement.resolve(operands, syntax, assembler, address);
            if (resolvedStatement != null) {
                expansion.add(resolvedStatement);
                address += BasicInstruction.BYTES_PER_INSTRUCTION;
            }
        }

        return new ExtendedStatement(syntax, this.instruction, operands, expansion);
    }
}
