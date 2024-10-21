package mars.assembler;

import mars.assembler.syntax.StatementSyntax;

public interface Statement {
    StatementSyntax getSyntax();

    void handlePlacement(Assembler assembler, int address);
}
