package mars.mips.hardware;

import java.nio.ByteOrder;

public enum Endianness {
    BIG_ENDIAN(ByteOrder.BIG_ENDIAN), LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN);

    private final ByteOrder byteOrder;

    Endianness(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public ByteOrder getByteOrder() {
        return this.byteOrder;
    }
}
