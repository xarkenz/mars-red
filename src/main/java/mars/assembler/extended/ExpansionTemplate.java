package mars.assembler.extended;

import mars.assembler.BasicStatement;
import mars.assembler.Operand;
import mars.assembler.syntax.StatementSyntax;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.ExtendedInstruction;

import java.util.ArrayList;
import java.util.List;

public class ExpansionTemplate {
    public interface Statement {
        BasicStatement resolve(List<Operand> originalOperands, int address, StatementSyntax syntax);

        default boolean isActive() {
            return true;
        }
    }

    private final ExtendedInstruction instruction;
    private final List<Statement> statements;

    public ExpansionTemplate(ExtendedInstruction instruction, List<Statement> statements) {
        this.instruction = instruction;
        this.statements = statements;
    }

    public ExtendedStatement resolve(List<Operand> operands, int address, StatementSyntax syntax) {
        List<BasicStatement> expansion = new ArrayList<>(this.statements.size());

        for (Statement statement : this.statements) {
            BasicStatement resolvedStatement = statement.resolve(operands, address, syntax);
            if (resolvedStatement != null) {
                expansion.add(resolvedStatement);
                address += resolvedStatement.getInstruction().getSizeBytes();
            }
        }

        return new ExtendedStatement(syntax, this.instruction, operands, expansion);
    }

    public int getSizeBytes() {
        int sizeBytes = 0;

        for (Statement statement : this.statements) {
            if (statement.isActive()) {
                sizeBytes += BasicInstruction.BYTES_PER_INSTRUCTION;
            }
        }

        return sizeBytes;
    }

    public ExtendedInstruction getInstruction() {
        return this.instruction;
    }

    public List<Statement> getStatements() {
        return this.statements;
    }
}
