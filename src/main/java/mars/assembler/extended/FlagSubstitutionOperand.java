package mars.assembler.extended;

import mars.assembler.AssemblerFlag;
import mars.assembler.Operand;
import mars.assembler.OperandType;

import java.util.List;

public class FlagSubstitutionOperand implements TemplateOperand {
    private final AssemblerFlag flag;
    private final TemplateOperand enabledValue;
    private final TemplateOperand disabledValue;
    private final OperandType type;

    public FlagSubstitutionOperand(AssemblerFlag flag, TemplateOperand enabledValue, TemplateOperand disabledValue) {
        this(flag, enabledValue, disabledValue, OperandType.union(enabledValue.getType(), disabledValue.getType()));
    }

    public FlagSubstitutionOperand(AssemblerFlag flag, TemplateOperand enabledValue, TemplateOperand disabledValue, OperandType type) {
        this.flag = flag;
        this.enabledValue = enabledValue;
        this.disabledValue = disabledValue;
        this.type = type;
    }

    public AssemblerFlag getFlag() {
        return this.flag;
    }

    public TemplateOperand getEnabledValue() {
        return this.enabledValue;
    }

    public TemplateOperand getDisabledValue() {
        return this.disabledValue;
    }

    public TemplateOperand getValue() {
        return (this.flag.isEnabled()) ? this.enabledValue : this.disabledValue;
    }

    @Override
    public OperandType getType() {
        return this.type;
    }

    @Override
    public TemplateOperand withType(OperandType type) {
        return new FlagSubstitutionOperand(this.flag, this.enabledValue, this.disabledValue, type);
    }

    @Override
    public Operand resolve(List<Operand> originalOperands, int address) {
        return this.getValue().resolve(originalOperands, address);
    }
}
