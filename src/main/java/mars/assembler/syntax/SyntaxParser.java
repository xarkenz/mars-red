package mars.assembler.syntax;

import mars.assembler.Directive;
import mars.assembler.Operand;
import mars.assembler.OperandType;
import mars.assembler.log.AssemblerLog;
import mars.assembler.log.SourceLocation;
import mars.assembler.token.SourceLine;
import mars.assembler.token.Token;
import mars.assembler.token.TokenType;
import mars.mips.instructions.Instruction;
import mars.mips.instructions.InstructionSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SyntaxParser {
    private final Iterator<SourceLine> sourceLines;
    private final AssemblerLog log;
    private SourceLine sourceLine;
    private Iterator<Token> lineTokens;
    private Token cachedToken;

    public SyntaxParser(Iterator<SourceLine> sourceLines, AssemblerLog log) {
        this.sourceLines = sourceLines;
        this.log = log;
        this.sourceLine = null;
        this.lineTokens = null;
        this.cachedToken = null;
    }

    public SourceLocation getCurrentLocation() {
        if (this.cachedToken != null) {
            return this.cachedToken.getLocation();
        }
        else if (this.sourceLine != null) {
            return this.sourceLine.getLocation().toColumnLocation(this.sourceLine.getContent().length());
        }
        else {
            return null;
        }
    }

    public Syntax parseNextSyntax() {
        Syntax syntax;
        do {
            syntax = this.tryParseNextSyntax();
        }
        while (syntax == null && (this.cachedToken != null || this.nextToken()));

        return syntax;
    }

    public Syntax tryParseNextSyntax() {
        // Cache the next token if necessary
        if (this.cachedToken == null && !this.nextToken()) {
            return null;
        }

        Token firstToken = this.cachedToken;
        switch (firstToken.getType()) {
            case IDENTIFIER -> {
                String labelName = firstToken.getLiteral();

                if (!this.nextTokenInLine() || this.cachedToken.getType() != TokenType.COLON) {
                    // DPS 14-July-2008
                    // Yet Another Hack: detect unrecognized directive. MARS recognizes the same directives
                    // as SPIM but other MIPS assemblers recognize additional directives. Compilers such
                    // as MIPS-directed GCC generate assembly code containing these directives. We'd like
                    // the opportunity to ignore them and continue. Tokenizer would categorize an unrecognized
                    // directive as an TokenType.IDENTIFIER because it would not be matched as a directive and
                    // MIPS labels can start with '.' NOTE: this can also be handled by including the
                    // ignored directive in the Directive list. There is already a mechanism in place
                    // for generating a warning there. But I cannot anticipate the names of all directives
                    // so this will catch anything, including a misspelling of a valid directive (which is
                    // a nice thing to do).
                    if (labelName.startsWith(".")) {
                        this.logWarning("Directive '" + labelName + "' is not supported by MARS; ignored");
                    }
                    else {
                        this.logError("Mnemonic '" + labelName + "' does not correspond to any known instruction");
                    }
                    return null;
                }

                this.cachedToken = null;
                return new LabelSyntax(this.sourceLine, firstToken, labelName);
            }
            case DIRECTIVE -> {
                Directive directive = (Directive) firstToken.getValue();

                List<Token> content = new ArrayList<>();
                this.cachedToken = null;
                do {
                    if (this.cachedToken != null) {
                        content.add(this.cachedToken);
                    }
                    while (this.nextTokenInLine()) {
                        content.add(this.cachedToken);
                    }
                }
                while (directive.allowsContinuation() && this.nextToken() && this.cachedToken.getType().isDirectiveContinuation());

                return new DirectiveSyntax(this.sourceLine, firstToken, directive, content);
            }
            case OPERATOR -> {
                List<SyntaxOperand> operands = new ArrayList<>();
                // Needed to determine the specific instruction variant later
                List<OperandType> operandTypes = new ArrayList<>();

                if (this.nextTokenInLine()) {
                    // Instruction mnemonics are also valid label names, so check if this should be a label instead
                    if (this.cachedToken.getType() == TokenType.COLON) {
                        // Morph the token into an identifier
                        firstToken.setType(TokenType.IDENTIFIER);
                        firstToken.setValue(null);

                        this.cachedToken = null;
                        return new LabelSyntax(this.sourceLine, firstToken, firstToken.getLiteral());
                    }

                    // At this point, we know we're parsing an instruction, so gather operands until end of line
                    do {
                        SyntaxOperand operand = this.parseNextOperand();
                        if (operand == null) {
                            // An error occurred while parsing the operand, so skip the rest of the line
                            this.cachedToken = null;
                            this.lineTokens = null;
                            return null;
                        }
                        operands.add(operand);
                        operandTypes.add(operand.getType());
                    }
                    while (this.cachedToken != null || this.nextTokenInLine());
                }

                // I know what I'm doing! The tokenizer puts a List<Instruction> in, I can get a List<Instruction> out
                @SuppressWarnings("unchecked")
                List<Instruction> mnemonicMatches = (List<Instruction>) firstToken.getValue();

                Instruction instruction = InstructionSet.matchInstruction(mnemonicMatches, operandTypes);
                if (instruction == null) {
                    this.log.logError(firstToken.getLocation(), "No instruction '" + firstToken + "' found matching operands " + operandTypes);
                    return null;
                }

                return new StatementSyntax(this.sourceLine, firstToken, instruction, operands);
            }
            default -> {
                this.logError("Unexpected token: " + firstToken);
                this.cachedToken = null;
                return null;
            }
        }
    }

    private SyntaxOperand parseNextOperand() {
        boolean parenthesized = false;
        if (this.cachedToken.getType() == TokenType.LEFT_PAREN) {
            parenthesized = true;
            if (!this.nextTokenInLine()) {
                this.logError("Unclosed '('");
                return null;
            }
        }

        Token operandToken = this.cachedToken;
        SyntaxOperand operand = operandToken.toOperand();

        if (operand == null) {
            // When expecting an operand, an operator is just treated as a plain identifier
            if (operandToken.getType() == TokenType.OPERATOR) {
                // Morph the token into an identifier
                operandToken.setType(TokenType.IDENTIFIER);
                operandToken.setValue(null);
            }
            else if (operandToken.getType() != TokenType.IDENTIFIER) {
                this.logError("Unexpected instruction operand: " + operandToken);
                return null;
            }

            // Check for a label offset
            if (this.nextTokenInLine() && (
                this.cachedToken.getType() == TokenType.PLUS || this.cachedToken.getType() == TokenType.MINUS
            )) {
                int offsetSign = (this.cachedToken.getType() == TokenType.MINUS) ? -1 : 1;

                if (!this.nextTokenInLine() || !this.cachedToken.getType().isInteger()) {
                    this.logError("Expected an integer offset following '" + ((offsetSign < 0) ? '-' : '+') + "'");
                    return null;
                }

                int offset = offsetSign * (Integer) this.cachedToken.getValue();
                this.cachedToken = null;

                operand = new LabelOperand(operandToken, offset);
            }
            else {
                operand = new LabelOperand(operandToken);
            }
        }
        else {
            this.cachedToken = null;
        }

        if (parenthesized) {
            if (!this.nextTokenInLine() || this.cachedToken.getType() != TokenType.RIGHT_PAREN) {
                this.logError("Unclosed '('");
                this.cachedToken = null;
                return null;
            }
            this.cachedToken = null;

            if (operand instanceof Operand basicOperand && operand.getType() == OperandType.REGISTER) {
                operand = new Operand(OperandType.PAREN_REGISTER, basicOperand.getValue());
            }
            else {
                this.logError("Parentheses can only contain CPU registers, not " + operandToken);
                return null;
            }
        }

        return operand;
    }

    private void logWarning(String message) {
        this.log.logWarning(
            this.getCurrentLocation(),
            message
        );
    }

    private void logError(String message) {
        this.log.logError(
            this.getCurrentLocation(),
            message
        );
    }

    private boolean nextTokenInLine() {
        if (this.lineTokens != null && this.lineTokens.hasNext()) {
            this.cachedToken = this.lineTokens.next();
            return true;
        }
        else {
            this.cachedToken = null;
            return false;
        }
    }

    private boolean nextToken() {
        while (this.lineTokens == null || !this.lineTokens.hasNext()) {
            if (!this.sourceLines.hasNext()) {
                this.cachedToken = null;
                return false;
            }
            this.sourceLine = this.sourceLines.next();

            this.lineTokens = createIterator(this.sourceLine.getTokens());
        }

        this.cachedToken = this.lineTokens.next();
        return true;
    }

    private static Iterator<Token> createIterator(List<Token> tokens) {
        return tokens.stream()
            .filter(token -> token.getType() != TokenType.DELIMITER && token.getType() != TokenType.COMMENT)
            .iterator();
    }
}
