package mars.assembler.syntax;

import mars.assembler.Directive;
import mars.assembler.SourceLine;
import mars.assembler.token.Token;

import java.util.List;

public class DirectiveSyntax implements Syntax {
    private final SourceLine sourceLine;
    private final Directive directive;
    private final List<Token> content;

    public DirectiveSyntax(SourceLine sourceLine, Directive directive, List<Token> content) {
        this.sourceLine = sourceLine;
        this.directive = directive;
        this.content = content;
    }

    @Override
    public SourceLine getSourceLine() {
        return this.sourceLine;
    }

    public Directive getDirective() {
        return this.directive;
    }

    public List<Token> getContent() {
        return this.content;
    }
}
