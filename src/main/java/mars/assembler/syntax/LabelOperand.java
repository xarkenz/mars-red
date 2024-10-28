package mars.assembler.syntax;

import mars.assembler.Assembler;
import mars.assembler.Operand;
import mars.assembler.OperandType;
import mars.assembler.Symbol;
import mars.assembler.token.Token;

public class LabelOperand implements SyntaxOperand {
    private final OperandType type;
    private final Token labelToken;
    private final int offset;

    public LabelOperand(Token labelToken) {
        this.type = OperandType.LABEL;
        this.labelToken = labelToken;
        this.offset = 0;
    }

    public LabelOperand(Token labelToken, int offset) {
        this.type = OperandType.LABEL_OFFSET;
        this.labelToken = labelToken;
        this.offset = offset;
    }

    @Override
    public OperandType getType() {
        return this.type;
    }

    public Token getLabelToken() {
        return this.labelToken;
    }

    public int getOffset() {
        return this.offset;
    }

    @Override
    public Operand resolve(Assembler assembler) {
        // Attempt to retrieve the symbol from either the local or global symbol table
        Symbol symbol = assembler.getSymbol(this.labelToken.getLiteral());
        // Make sure the symbol actually exists
        if (symbol == null) {
            assembler.logError(
                this.labelToken.getLocation(),
                "Undefined symbol '" + this.labelToken + "'"
            );
            // Resolve to a dummy operand
            return new Operand(this.type, 0xDEADBEEF);
        }

        // Resolve to the label's address, with offset if needed
        return new Operand(this.type, symbol.getAddress() + this.offset);
    }
}
