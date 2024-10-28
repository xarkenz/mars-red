package mars.assembler.extended;

import mars.assembler.Assembler;
import mars.assembler.AssemblerFlag;
import mars.assembler.BasicStatement;
import mars.assembler.Operand;
import mars.assembler.syntax.StatementSyntax;

import java.util.List;

public class FlagSubstitutionStatement implements ExpansionTemplate.Statement {
    private final AssemblerFlag flag;
    private final ExpansionTemplate.Statement enabledValue;
    private final ExpansionTemplate.Statement disabledValue;

    public FlagSubstitutionStatement(AssemblerFlag flag, ExpansionTemplate.Statement enabledValue, ExpansionTemplate.Statement disabledValue) {
        this.flag = flag;
        this.enabledValue = enabledValue;
        this.disabledValue = disabledValue;
    }

    public AssemblerFlag getFlag() {
        return this.flag;
    }

    public ExpansionTemplate.Statement getEnabledValue() {
        return this.enabledValue;
    }

    public ExpansionTemplate.Statement getDisabledValue() {
        return this.disabledValue;
    }

    public ExpansionTemplate.Statement getValue() {
        return (this.flag.isEnabled()) ? this.enabledValue : this.disabledValue;
    }

    @Override
    public BasicStatement resolve(List<Operand> originalOperands, StatementSyntax syntax, Assembler assembler, int address) {
        ExpansionTemplate.Statement statement = this.getValue();
        return (statement == null) ? null : statement.resolve(originalOperands, syntax, assembler, address);
    }

    @Override
    public boolean isActive() {
        return this.getValue() != null;
    }
}
