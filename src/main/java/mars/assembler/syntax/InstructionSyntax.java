package mars.assembler.syntax;

import mars.assembler.SourceLine;
import mars.assembler.token.Token;
import mars.mips.instructions.Instruction;

import java.util.List;

public class InstructionSyntax implements Syntax {
    private final SourceLine sourceLine;
    private final List<Instruction> mnemonicMatches;
    private final List<Token> content;

    public InstructionSyntax(SourceLine sourceLine, List<Instruction> mnemonicMatches, List<Token> content) {
        this.sourceLine = sourceLine;
        this.mnemonicMatches = mnemonicMatches;
        this.content = content;
    }

    @Override
    public SourceLine getSourceLine() {
        return this.sourceLine;
    }

    public List<Instruction> getMnemonicMatches() {
        return this.mnemonicMatches;
    }

    public List<Token> getContent() {
        return this.content;
    }
}
