package mars.assembler.extended;

import mars.assembler.AssemblerFlag;
import mars.assembler.OperandType;
import mars.assembler.SourceLine;
import mars.assembler.token.Token;
import mars.assembler.token.TokenType;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.InstructionSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TemplateParser {
    private final InstructionSet instructionSet;
    private final List<OperandType> originalTypes;
    private final Iterator<SourceLine> sourceLines;
    private SourceLine sourceLine;
    private Iterator<Token> lineTokens;
    private Token cachedToken;

    public TemplateParser(InstructionSet instructionSet, List<OperandType> originalTypes, Iterator<SourceLine> sourceLines) {
        this.instructionSet = instructionSet;
        this.originalTypes = originalTypes;
        this.sourceLines = sourceLines;
        this.sourceLine = null;
        this.lineTokens = null;
        this.cachedToken = null;
    }

    public ExpansionTemplate.Statement parseNextStatement() throws RuntimeException {
        if (this.cachedToken == null && !this.nextToken()) {
            return null;
        }

        return this.parseNextStatementInLine();
    }

    public ExpansionTemplate.Statement parseNextStatementInLine() throws RuntimeException {
        if (this.cachedToken == null && !this.nextTokenInLine()) {
            return null;
        }

        switch (this.cachedToken.getType()) {
            case OPERATOR -> {
                String mnemonic = this.cachedToken.getLiteral();
                this.cachedToken = null;

                List<OperandType> operandTypes = new ArrayList<>();
                List<TemplateOperand> operands = new ArrayList<>();
                while (this.cachedToken != null || this.nextTokenInLine()) {
                    TemplateOperand operand = this.parseNextOperand();
                    if (operand != null) {
                        operandTypes.add(operand.getType());
                        operands.add(operand);
                    }
                }

                BasicInstruction instruction = this.instructionSet.matchBasicInstruction(mnemonic, operandTypes);
                if (instruction == null) {
                    throw new RuntimeException("No basic instruction found matching operands " + operandTypes);
                }

                return new TemplateStatement(instruction, operands);
            }
            case TEMPLATE_SUBSTITUTION -> {
                if (this.lineTokens.hasNext()) {
                    throw new RuntimeException("Unexpected tokens following statement substitution");
                }

                // I know what I'm doing! The tokenizer puts a List<Token> in, I can get a List<Token> out
                @SuppressWarnings("unchecked")
                List<Token> content = (List<Token>) this.cachedToken.getValue();
                if (content.isEmpty()) {
                    throw new RuntimeException("Empty template substitution");
                }
                this.cachedToken = null;

                AssemblerFlag flag = AssemblerFlag.fromKey(content.get(0).getLiteral());
                if (flag == null) {
                    throw new RuntimeException("Unrecognized assembler flag: " + content.get(0));
                }

                if (content.size() < 3 || content.get(1).getType() != TokenType.COLON) {
                    throw new RuntimeException("Usage: {<flag> : [<enabledValue>] : [<disabledValue>]}");
                }

                int separatorIndex = 2;
                while (content.get(separatorIndex).getType() != TokenType.COLON) {
                    separatorIndex++;
                    if (separatorIndex >= content.size()) {
                        throw new RuntimeException("Usage: {<flag> : [<enabledValue>] : [<disabledValue>]}");
                    }
                }

                Iterator<Token> originalIterator = this.lineTokens;

                ExpansionTemplate.Statement enabledValue = null;
                if (2 < separatorIndex) {
                    this.lineTokens = createIterator(content.subList(2, separatorIndex));

                    enabledValue = this.parseNextStatementInLine();
                }

                ExpansionTemplate.Statement disabledValue = null;
                if (separatorIndex + 1 < content.size()) {
                    this.lineTokens = createIterator(content.subList(separatorIndex + 1, content.size()));

                    disabledValue = this.parseNextStatementInLine();
                }

                this.cachedToken = null;
                this.lineTokens = originalIterator;

                return new FlagSubstitutionStatement(flag, enabledValue, disabledValue);
            }
            default -> throw new RuntimeException("Unexpected token: " + this.cachedToken);
        }
    }

    private TemplateOperand parseNextOperand() throws RuntimeException {
        boolean parenthesized = false;
        if (this.cachedToken.getType() == TokenType.LEFT_PAREN) {
            parenthesized = true;
            if (!this.nextTokenInLine()) {
                throw new RuntimeException("Unclosed '('");
            }
        }

        TemplateOperand operand = this.parseTokenOperand(this.cachedToken);

        if (parenthesized) {
            if (!this.nextTokenInLine() || this.cachedToken.getType() != TokenType.RIGHT_PAREN) {
                throw new RuntimeException("Unclosed '('");
            }

            if (operand.getType() == OperandType.REGISTER) {
                operand = operand.withType(OperandType.PAREN_REGISTER);
            }
            else if (operand.getType() != OperandType.PAREN_REGISTER) {
                throw new RuntimeException("Operand wrapped in () must be of type 'reg' or '(reg)', not '" + operand.getType() + "'");
            }
        }

        this.cachedToken = null;

        return operand;
    }

    private TemplateOperand parseTokenOperand(Token token) throws RuntimeException {
        if (token.getType() == TokenType.TEMPLATE_SUBSTITUTION) {
            // I know what I'm doing! The tokenizer puts a List<Token> in, I can get a List<Token> out
            @SuppressWarnings("unchecked")
            List<Token> content = (List<Token>) token.getValue();

            return this.parseSubstitution(content);
        }
        else {
            return token.asOperand();
        }
    }

    private TemplateOperand parseSubstitution(List<Token> content) throws RuntimeException {
        if (content.isEmpty()) {
            throw new RuntimeException("Empty template substitution");
        }

        if (content.get(0).getType().isInteger()) {
            int operandIndex = (Integer) content.get(0).getValue();
            if (operandIndex < 0 || operandIndex >= this.originalTypes.size()) {
                throw new RuntimeException("Invalid operand index: " + operandIndex);
            }

            List<OperandModifier> modifiers = new ArrayList<>();
            if (content.size() > 1) {
                if (content.get(1).getType() != TokenType.COLON) {
                    throw new RuntimeException("Expected ':', got: " + content.get(1));
                }

                for (Token modifierToken : content.subList(2, content.size())) {
                    if (modifierToken.getType() == TokenType.COMMENT || modifierToken.getType() == TokenType.DELIMITER) {
                        continue;
                    }
                    OperandModifier modifier = OperandModifier.parse(modifierToken.getLiteral());
                    if (modifier == null) {
                        throw new RuntimeException("Unrecognized operand modifier: " + modifierToken);
                    }
                    modifiers.add(modifier);
                }
            }

            return new OperandSubstitution(operandIndex, modifiers, this.originalTypes.get(operandIndex));
        }
        else {
            AssemblerFlag flag = AssemblerFlag.fromKey(content.get(0).getLiteral());
            if (flag == null) {
                throw new RuntimeException("Unrecognized assembler flag: " + content.get(0));
            }

            if (content.size() < 3 || content.get(1).getType() != TokenType.COLON) {
                throw new RuntimeException("Usage: {<flag> : [<enabledValue>] : [<disabledValue>]}");
            }

            int index = 2;
            TemplateOperand enabledValue = null;
            if (content.get(index).getType() != TokenType.COLON) {
                enabledValue = this.parseTokenOperand(content.get(index));
                index++;
                if (index >= content.size() || content.get(index).getType() != TokenType.COLON) {
                    throw new RuntimeException("Usage: {<flag> : [<enabledValue>] : [<disabledValue>]}");
                }
            }

            index++;
            TemplateOperand disabledValue = null;
            if (index < content.size()) {
                disabledValue = this.parseTokenOperand(content.get(index));
                if (index + 1 < content.size()) {
                    throw new RuntimeException("Usage: {<flag> : [<enabledValue>] : [<disabledValue>]}");
                }
            }

            return new FlagSubstitutionOperand(flag, enabledValue, disabledValue);
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
