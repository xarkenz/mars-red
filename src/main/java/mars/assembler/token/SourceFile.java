package mars.assembler.token;

import java.util.List;

public class SourceFile {
    private final String filename;
    private final List<SourceLine> lines;

    public SourceFile(String filename, List<SourceLine> lines) {
        this.filename = filename;
        this.lines = lines;
    }

    public String getFilename() {
        return this.filename;
    }

    public List<SourceLine> getLines() {
        return this.lines;
    }
}
