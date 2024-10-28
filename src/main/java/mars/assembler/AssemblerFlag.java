package mars.assembler;

import mars.Application;

public enum AssemblerFlag {
    DELAYED_BRANCHING,
    BIG_ENDIAN;

    public boolean isEnabled() {
        return switch (this) {
            case DELAYED_BRANCHING -> Application.getSettings().delayedBranchingEnabled.get();
            case BIG_ENDIAN -> Application.getSettings().useBigEndian.get();
        };
    }

    public static AssemblerFlag fromKey(String key) {
        return switch (key.toLowerCase()) {
            case "db" -> DELAYED_BRANCHING;
            case "be" -> BIG_ENDIAN;
            default -> null;
        };
    }
}