package mars.assembler.extended;

import mars.assembler.Assembler;
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

    @Override
    public BasicInstruction getInstruction() {
        return this.instruction;
    }

    public List<TemplateOperand> getOperands() {
        return this.operands;
    }

    @Override
    public BasicStatement resolve(List<Operand> originalOperands, StatementSyntax syntax, Assembler assembler, int address) {
        List<Operand> expansionOperands = new ArrayList<>(this.operands.size());

        for (int index = 0; index < this.operands.size(); index++) {
            Operand operand = this.operands.get(index).resolve(originalOperands, address);
            // Perform type conversion if needed
            operand = operand.convertToType(this.instruction.getOperandTypes().get(index), syntax, assembler, address);
            expansionOperands.add(operand);
        }

        return new BasicStatement(syntax, this.instruction, expansionOperands);
    }
}
