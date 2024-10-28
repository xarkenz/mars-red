package mars.assembler.log;

public class AssemblyError extends Exception {
    private final AssemblerLog log;

    public AssemblyError(AssemblerLog log) {
        this.log = log;
    }

    public AssemblerLog getLog() {
        return this.log;
    }
}
