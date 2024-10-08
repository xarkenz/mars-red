package mars.assembler.extended;

import mars.assembler.BasicStatement;
import mars.assembler.Operand;
import mars.assembler.SourceLine;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.ExtendedInstruction;

import java.util.ArrayList;
import java.util.List;

public class ExpansionTemplate {
    public interface Statement {
        BasicStatement resolve(List<Operand> originalOperands, int address, SourceLine sourceLine);

        default boolean isActive() {
            return true;
        }
    }

    private final ExtendedInstruction instruction;
    private final List<Statement> template;

    public ExpansionTemplate(ExtendedInstruction instruction, List<Statement> template) {
        this.instruction = instruction;
        this.template = template;
    }

    public ExtendedStatement resolve(List<Operand> operands, int address, SourceLine sourceLine) {
        List<BasicStatement> expansion = new ArrayList<>(this.template.size());

        for (Statement statement : this.template) {
            BasicStatement resolvedStatement = statement.resolve(operands, address, sourceLine);
            if (resolvedStatement != null) {
                expansion.add(resolvedStatement);
                address += BasicInstruction.BYTES_PER_INSTRUCTION;
            }
        }

        return new ExtendedStatement(sourceLine, this.instruction, operands, expansion);
    }

    public int getSizeBytes() {
        int sizeBytes = 0;

        for (Statement statement : this.template) {
            if (statement.isActive()) {
                sizeBytes += BasicInstruction.BYTES_PER_INSTRUCTION;
            }
        }

        return sizeBytes;
    }

    public ExtendedInstruction getInstruction() {
        return this.instruction;
    }

    public List<Statement> getTemplate() {
        return this.template;
    }
}
