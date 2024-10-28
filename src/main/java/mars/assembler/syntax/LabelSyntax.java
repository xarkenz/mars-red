package mars.assembler.syntax;

import mars.assembler.Assembler;
import mars.assembler.token.SourceLine;
import mars.assembler.Symbol;
import mars.assembler.token.Token;

public class LabelSyntax implements Syntax {
    private final SourceLine sourceLine;
    private final Token firstToken;
    private final String labelName;

    public LabelSyntax(SourceLine sourceLine, Token firstToken, String labelName) {
        this.sourceLine = sourceLine;
        this.firstToken = firstToken;
        this.labelName = labelName;
    }

    @Override
    public SourceLine getSourceLine() {
        return this.sourceLine;
    }

    @Override
    public Token getFirstToken() {
        return this.firstToken;
    }

    public String getLabelName() {
        return this.labelName;
    }

    @Override
    public void process(Assembler assembler) {
        Symbol labelSymbol = new Symbol(
            this.labelName,
            assembler.getSegment().getAddress(),
            assembler.getSegment().isData()
        );
        Symbol replacedSymbol = assembler.getLocalSymbolTable().defineSymbol(labelSymbol);
        if (replacedSymbol != null) {
            assembler.logError(
                this.firstToken.getLocation(),
                "Label '" + this.labelName + "' has already been defined"
            );
        }
    }
}
