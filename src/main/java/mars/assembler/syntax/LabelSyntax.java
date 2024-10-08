package mars.assembler.syntax;

import mars.assembler.SourceLine;

public class LabelSyntax implements Syntax {
    private final SourceLine sourceLine;
    private final String labelName;

    public LabelSyntax(SourceLine sourceLine, String labelName) {
        this.sourceLine = sourceLine;
        this.labelName = labelName;
    }

    public SourceLine getSourceLine() {
        return this.sourceLine;
    }

    public String getLabelName() {
        return this.labelName;
    }
}
