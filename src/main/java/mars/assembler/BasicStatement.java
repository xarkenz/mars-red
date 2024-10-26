package mars.assembler;

import mars.ProcessingException;
import mars.assembler.syntax.StatementSyntax;
import mars.mips.instructions.BasicInstruction;

import java.util.List;

public class BasicStatement implements Statement {
    private final StatementSyntax syntax;
    private final BasicInstruction instruction;
    private final List<Operand> operands;
    private final int binaryEncoding;

    public BasicStatement(StatementSyntax syntax, BasicInstruction instruction, List<Operand> operands) {
        this(syntax, instruction, operands, instruction.encodeOperands(operands));
    }

    public BasicStatement(StatementSyntax syntax, BasicInstruction instruction, List<Operand> operands, int binaryEncoding) {
        this.syntax = syntax;
        this.instruction = instruction;
        this.operands = operands;
        this.binaryEncoding = binaryEncoding;
    }

    @Override
    public StatementSyntax getSyntax() {
        return this.syntax;
    }

    public BasicInstruction getInstruction() {
        return this.instruction;
    }

    public List<Operand> getOperands() {
        return this.operands;
    }

    public int getOperand(int index) {
        return this.operands.get(index).getValue();
    }

    public int getBinaryEncoding() {
        return this.binaryEncoding;
    }

    @Override
    public void handlePlacement(Assembler assembler, int address) {
        assembler.placeStatement(this, address);
    }

    /**
     * Simulate the execution of a specific MIPS basic instruction.
     *
     * @throws ProcessingException  Thrown if a runtime exception was generated during execution.
     * @throws InterruptedException Thrown if the simulator was stopped during execution.
     */
    public void simulate() throws ProcessingException, InterruptedException {
        this.instruction.getFunction().simulate(this);
    }
}
