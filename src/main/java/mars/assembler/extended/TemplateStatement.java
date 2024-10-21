package mars.assembler.extended;

import mars.assembler.BasicStatement;
import mars.assembler.Operand;
import mars.assembler.syntax.StatementSyntax;
import mars.mips.instructions.BasicInstruction;

import java.util.ArrayList;
import java.util.List;

public class TemplateStatement implements ExpansionTemplate.Statement {
    private final BasicInstruction instruction;
    private final List<TemplateOperand> operands;

    public TemplateStatement(BasicInstruction instruction, List<TemplateOperand> operands) {
        this.instruction = instruction;
        this.operands = operands;
    }

    public BasicInstruction getInstruction() {
        return this.instruction;
    }

    public List<TemplateOperand> getOperands() {
        return this.operands;
    }

    @Override
    public BasicStatement resolve(List<Operand> originalOperands, int address, StatementSyntax syntax) {
        List<Operand> expansionOperands = new ArrayList<>(this.operands.size());

        for (TemplateOperand operand : this.operands) {
            expansionOperands.add(operand.resolve(originalOperands, address));
        }

        return new BasicStatement(syntax, this.instruction, expansionOperands);
    }
}
