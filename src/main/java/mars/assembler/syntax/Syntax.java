package mars.assembler.syntax;

import mars.assembler.Assembler;
import mars.assembler.token.SourceLine;
import mars.assembler.token.Token;

public interface Syntax {
    SourceLine getSourceLine();

    Token getFirstToken();

    void process(Assembler assembler);
}
