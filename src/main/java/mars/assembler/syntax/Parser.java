package mars.assembler.syntax;

import mars.ErrorList;
import mars.ErrorMessage;
import mars.assembler.Directive;
import mars.assembler.SourceLine;
import mars.assembler.token.Token;
import mars.assembler.token.TokenType;
import mars.mips.instructions.Instruction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Parser {
    private final Iterator<SourceLine> sourceLines;
    private final ErrorList errors;
    private SourceLine sourceLine;
    private Iterator<Token> lineTokens;
    private Token cachedToken;

    public Parser(Iterator<SourceLine> sourceLines, ErrorList errors) {
        this.sourceLines = sourceLines;
        this.errors = errors;
        this.sourceLine = null;
        this.lineTokens = null;
        this.cachedToken = null;
    }

    public Syntax parseNextSyntax() {
        Syntax syntax;
        do {
            syntax = tryParseNextSyntax();
        }
        while (syntax == null && this.nextTokenIfNeeded());

        return syntax;
    }

    public Syntax tryParseNextSyntax() {
        if (!this.nextTokenIfNeeded()) {
            return null;
        }

        switch (this.cachedToken.getType()) {
            case IDENTIFIER -> {
                String labelName = this.cachedToken.getLiteral();

                if (!this.nextTokenInLine() || this.cachedToken.getType() != TokenType.COLON) {
                    this.logError("'" + labelName + "' is not a valid directive or mnemonic");
                    return null;
                }

                this.cachedToken = null;
                return new LabelSyntax(this.sourceLine, labelName);
            }
            case DIRECTIVE -> {
                Directive directive = (Directive) this.cachedToken.getValue();

                List<Token> content = new ArrayList<>();
                while (this.nextTokenInLine()) {
                    content.add(this.cachedToken);
                }

                return new DirectiveSyntax(this.sourceLine, directive, content);
            }
            case OPERATOR -> {
                Token mnemonicToken = this.cachedToken;
                // I know what I'm doing! The tokenizer puts a List<Instruction> in, I can get a List<Instruction> out
                @SuppressWarnings("unchecked")
                List<Instruction> mnemonicMatches = (List<Instruction>) mnemonicToken.getValue();

                List<Token> content = new ArrayList<>();
                if (this.nextTokenInLine()) {
                    // Instruction mnemonics are also valid label names
                    if (this.cachedToken.getType() == TokenType.COLON) {
                        // Morph the token into an identifier
                        mnemonicToken.setType(TokenType.IDENTIFIER);
                        mnemonicToken.setValue(null);

                        this.cachedToken = null;
                        return new LabelSyntax(this.sourceLine, mnemonicToken.getLiteral());
                    }

                    content.add(this.cachedToken);
                }

                while (this.nextTokenInLine()) {
                    content.add(this.cachedToken);
                }

                return new InstructionSyntax(this.sourceLine, mnemonicMatches, content);
            }
            default -> {
                this.logError("Unexpected token: " + this.cachedToken);
                return null;
            }
        }
    }

    private boolean nextTokenInLine() {
        if (this.lineTokens.hasNext()) {
            this.cachedToken = this.lineTokens.next();
            return true;
        }
        else {
            this.cachedToken = null;
            return false;
        }
    }

    private boolean nextTokenIfNeeded() {
        if (this.cachedToken != null) {
            return true;
        }

        while (this.lineTokens == null || !this.lineTokens.hasNext()) {
            if (!this.sourceLines.hasNext()) {
                this.cachedToken = null;
                return false;
            }
            this.sourceLine = this.sourceLines.next();

            this.lineTokens = this.sourceLine.getTokens()
                .stream()
                .filter(token -> token.getType() != TokenType.DELIMITER && token.getType() != TokenType.COMMENT)
                .iterator();
        }

        this.cachedToken = this.lineTokens.next();
        return true;
    }

    private void logError(String message) {
        this.errors.add(new ErrorMessage(
            this.sourceLine.getFilename(),
            this.sourceLine.getLineNumber(),
            (this.cachedToken == null) ? this.sourceLine.getContent().length() : this.cachedToken.getSourceColumn(),
            message
        ));
    }
}
