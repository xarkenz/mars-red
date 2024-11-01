package mars.assembler;

import mars.simulator.SimulatorException;
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
     * @throws SimulatorException  Thrown if a runtime exception was generated during execution.
     * @throws InterruptedException Thrown if the simulator was stopped during execution.
     */
    public void simulate() throws SimulatorException, InterruptedException {
        this.instruction.getFunction().simulate(this);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder(this.instruction.getMnemonic());

        for (int index = 0; index < this.operands.size(); index++) {
            Operand operand = this.operands.get(index);
            if (index == 0) {
                while (output.length() < 8) {
                    output.append(' ');
                }
            }
            else if (operand.getType() != OperandType.PAREN_REGISTER) {
                output.append(", ");
            }

            output.append(operand);
        }

        return output.toString();
    }
}
