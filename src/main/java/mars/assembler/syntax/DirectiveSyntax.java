package mars.assembler.syntax;

import mars.assembler.Assembler;
import mars.assembler.Directive;
import mars.assembler.token.SourceLine;
import mars.assembler.token.Token;

import java.util.List;

public class DirectiveSyntax implements Syntax {
    private final SourceLine sourceLine;
    private final Token firstToken;
    private final Directive directive;
    private final List<Token> content;

    public DirectiveSyntax(SourceLine sourceLine, Token firstToken, Directive directive, List<Token> content) {
        this.sourceLine = sourceLine;
        this.firstToken = firstToken;
        this.directive = directive;
        this.content = content;
    }

    @Override
    public SourceLine getSourceLine() {
        return this.sourceLine;
    }

    @Override
    public Token getFirstToken() {
        return this.firstToken;
    }

    public Directive getDirective() {
        return this.directive;
    }

    public List<Token> getContent() {
        return this.content;
    }

    @Override
    public void process(Assembler assembler) {
        this.directive.process(this, assembler);
    }
}
