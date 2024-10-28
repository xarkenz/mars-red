package mars.assembler.log;

public enum LogLevel {
    INFO("Info"),
    WARNING("Warning"),
    ERROR("Error");

    private final String displayName;

    LogLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
