package mars.assembler;

import mars.ProcessingException;
import mars.assembler.syntax.Syntax;
import mars.mips.instructions.BasicInstruction;

import java.util.List;

public class BasicStatement implements Syntax {
    private final SourceLine sourceLine;
    private final BasicInstruction instruction;
    private final List<Operand> operands;
    private final int[] operandValues;

    public BasicStatement(SourceLine sourceLine, BasicInstruction instruction, List<Operand> operands) {
        this.sourceLine = sourceLine;
        this.instruction = instruction;
        this.operands = operands;
        this.operandValues = new int[operands.size()];
        for (int index = 0; index < operands.size(); index++) {
            this.operandValues[index] = operands.get(index).getValue();
        }
    }

    @Override
    public SourceLine getSourceLine() {
        return this.sourceLine;
    }

    public BasicInstruction getInstruction() {
        return this.instruction;
    }

    public List<Operand> getOperands() {
        return this.operands;
    }

    public int[] getOperandValues() {
        return this.operandValues;
    }

    /**
     * Simulate the execution of a specific MIPS basic instruction.
     *
     * @throws ProcessingException  Thrown if a runtime exception was generated during execution.
     * @throws InterruptedException Thrown if the simulator was stopped during execution.
     */
    public void simulate() throws ProcessingException, InterruptedException {
        this.instruction.getFunction().simulate(this.operandValues);
    }
}
