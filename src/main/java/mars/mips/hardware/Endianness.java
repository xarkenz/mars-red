package mars.mips.hardware;

import java.nio.ByteOrder;

/**
 * An enumeration of the different "endianness" states. "Endianness" refers to the order in which bytes
 * are stored in memory.
 * <p>
 * For now, this enumeration is essentially a wrapper around an existing enumeration, {@link ByteOrder}.
 * The reason for this is primarily that <code>ByteOrder</code> is not actually an <code>enum</code>,
 * but rather a <code>final class</code> with two constant instances as static members since <code>enum</code>s
 * were introduced in Java 1.5, after <code>ByteOrder</code> was created. The benefit of an <code>enum</code>
 * is that the compiler understands that no more instances of this class will ever be created, allowing for
 * more convenient logic in many cases (particularly when using <code>switch</code> expressions).
 */
public enum Endianness {
    /**
     * The bytes of a multibyte value are ordered from most significant to least significant.
     */
    BIG_ENDIAN(ByteOrder.BIG_ENDIAN),
    /**
     * The bytes of a multibyte value are ordered from least significant to most significant.
     */
    LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN);

    private final ByteOrder byteOrder;

    Endianness(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    /**
     * Get the Java standard library representation of this endianness value.
     *
     * @return The <code>ByteOrder</code> variant corresponding to this value.
     */
    public ByteOrder getByteOrder() {
        return this.byteOrder;
    }
}
