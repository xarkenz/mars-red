package mars.mips.hardware;

import mars.util.Binary;

import java.util.Properties;

/**
 * A specification for how MIPS memory should be laid out in the address space. The layout consists of a number of
 * address ranges as well as a number of special locations.
 * <p>
 * This class is typically constructed using {@link #fromProperties(Properties)}. Many of the public fields in this
 * class indicate which property in the <code>Properties</code> object they are derived from.
 */
public class MemoryLayout {
    /**
     * Representation of an address range defined by a minimum address and a maximum address.
     *
     * @param minAddress The minimum address in the range.
     * @param maxAddress The maximum address in the range.
     */
    public record Range(int minAddress, int maxAddress) {
        /**
         * Construct a new <code>Range</code> given a minimum address and maximum address.
         *
         * @param minAddress The minimum address in the range.
         * @param maxAddress The maximum address in the range.
         * @throws IllegalArgumentException Thrown if <code>minAddress</code> &gt; <code>maxAddress</code>
         *         (using unsigned comparison).
         */
        public Range {
            if (Integer.compareUnsigned(minAddress, maxAddress) > 0) {
                throw new IllegalArgumentException("address range has min address > max address");
            }
        }

        /**
         * Decode a string representation of a range, which should be two integers separated by a comma. Whitespace is
         * ignored everywhere except within the integers themselves.
         *
         * @param string The string to decode.
         * @return The range represented by the string.
         * @throws IllegalArgumentException Thrown if the string is improperly formatted, or if
         *         <code>minAddress</code> &gt; <code>maxAddress</code> (using unsigned comparison).
         */
        public static Range decode(String string) throws IllegalArgumentException {
            int commaIndex = string.indexOf(',');
            if (commaIndex < 0) {
                throw new IllegalArgumentException("expected a comma separating min address and max address of range");
            }
            return new Range(
                Binary.decodeInteger(string.substring(0, commaIndex).strip()),
                Binary.decodeInteger(string.substring(commaIndex + 1).strip())
            );
        }

        /**
         * Determine whether a given address lies within this range.
         *
         * @param address The address to check.
         * @return <code>true</code> if this range contains <code>address</code>, or <code>false</code> otherwise.
         */
        public boolean contains(int address) {
            return Integer.compareUnsigned(this.minAddress, address) <= 0
                && Integer.compareUnsigned(address, this.maxAddress) <= 0;
        }
    }

    /**
     * Array of string descriptions for all memory ranges. Ordering is consistent with {@link #ranges}.
     */
    public static final String[] RANGE_DESCRIPTIONS = {
        "Mapped address space",
        "User space",
        "Text segment (.text)",
        "Data segment",
        "Global data (.extern)",
        "Static data (.data)",
        "Heap/stack data",
        "Kernel text segment (.ktext)",
        "Kernel data segment (.kdata)",
        "Memory-mapped I/O",
    };
    /**
     * Array of string descriptions for all memory locations. Ordering is consistent with {@link #locations}.
     */
    public static final String[] LOCATION_DESCRIPTIONS = {
        "Exception handler address",
        "Initial global pointer ($gp)",
        "Initial stack pointer ($sp)",
    };

    /**
     * The name displayed in the Preferences dialog for this layout.
     * (Derived from the <code>DisplayName</code> property.)
     */
    public final String displayName;
    /**
     * The description displayed in the Preferences dialog for this layout.
     * (Derived from the <code>Description</code> property.)
     */
    public final String description;

    /**
     * The range of mapped address space. All other ranges should be fully contained by this one.
     * (Derived from the <code>MappedSpace</code> property.)
     */
    public final Range mappedRange;
    /**
     * The range of user space. {@link #textRange} and {@link #dataRange} should be fully contained by this range.
     * Note that "kernel space" is considered to be the space in {@link #mappedRange} not covered by this range.
     * (Derived from the <code>UserSpace</code> property.)
     */
    public final Range userRange;
    /**
     * The range covered by the user text segment (<code>.text</code>). Should be contained by {@link #userRange}.
     * (Derived from the <code>TextSegment</code> property.)
     */
    public final Range textRange;
    /**
     * The range covered by the user data segment. Should be contained by {@link #userRange}. {@link #externRange},
     * {@link #staticRange}, and {@link #dynamicRange} should all be fully contained by this range.
     * (Derived from the <code>DataSegment</code> property.)
     */
    public final Range dataRange;
    /**
     * The range containing global data (generated via <code>.extern</code>). Should be contained by {@link #dataRange}.
     * (Derived from the <code>ExternData</code> property.)
     */
    public final Range externRange;
    /**
     * The range containing static data (<code>.data</code>). Should be contained by {@link #dataRange}.
     * (Derived from the <code>StaticData</code> property.)
     */
    public final Range staticRange;
    /**
     * The range containing dynamic data (runtime heap/stack). Should be contained by {@link #dataRange}.
     * Note that the heap grows upward from the minimum address and the stack grows downward from the maximum address
     * &mdash; more specifically, from {@link #initialStackPointer}.
     * (Derived from the <code>HeapStackData</code> property.)
     */
    public final Range dynamicRange;
    /**
     * The range covered by the kernel text segment (<code>.ktext</code>). Should <i>not</i> be contained by
     * {@link #userRange}.
     * (Derived from the <code>KernelTextSegment</code> property.)
     */
    public final Range kernelTextRange;
    /**
     * The range covered by the kernel data segment (<code>.kdata</code>). Should <i>not</i> be contained by
     * {@link #userRange}.
     * (Derived from the <code>KernelDataSegment</code> property.)
     */
    public final Range kernelDataRange;
    /**
     * The range dedicated for performing memory-mapped I/O.
     * (Derived from the <code>MemoryMappedIO</code> property.)
     */
    public final Range mmioRange;

    /**
     * The location of the default exception handler. Execution jumps to this address whenever an exception, interrupt,
     * or trap occurs.
     * (Derived from the <code>ExceptionHandler</code> property.)
     */
    public final int exceptionHandlerAddress;
    /**
     * The initial value of the global pointer (<code>$gp</code>).
     * (Derived from the <code>GlobalPointer</code> property.)
     */
    public final int initialGlobalPointer;
    /**
     * The initial value of the stack pointer (<code>$sp</code>). This serves as the starting point for the runtime
     * stack, which grows downward. Typically, this is the last word-aligned address in {@link #dynamicRange}.
     * (Derived from the <code>StackPointer</code> property.)
     */
    public final int initialStackPointer;

    /**
     * Array containing all memory ranges. Ordering is consistent with {@link #RANGE_DESCRIPTIONS}.
     */
    public final Range[] ranges;
    /**
     * Array containing all memory locations. Ordering is consistent with {@link #LOCATION_DESCRIPTIONS}.
     */
    public final int[] locations;

    /**
     * Construct a new <code>MemoryLayout</code> manually. To construct from {@link Properties} instead,
     * see {@link #fromProperties(Properties)}.
     */
    public MemoryLayout(
        String displayName,
        String description,
        Range mappedRange,
        Range userRange,
        Range textRange,
        Range dataRange,
        Range externRange,
        Range staticRange,
        Range dynamicRange,
        Range kernelTextRange,
        Range kernelDataRange,
        Range mmioRange,
        int exceptionHandlerAddress,
        int initialGlobalPointer,
        int initialStackPointer
    ) {
        this.displayName = displayName;
        this.description = description;

        this.mappedRange = mappedRange;
        this.userRange = userRange;
        this.textRange = textRange;
        this.dataRange = dataRange;
        this.externRange = externRange;
        this.staticRange = staticRange;
        this.dynamicRange = dynamicRange;
        this.kernelTextRange = kernelTextRange;
        this.kernelDataRange = kernelDataRange;
        this.mmioRange = mmioRange;

        this.exceptionHandlerAddress = exceptionHandlerAddress;
        this.initialGlobalPointer = initialGlobalPointer;
        this.initialStackPointer = initialStackPointer;

        // The orderings here must remain consistent with the descriptions arrays
        this.ranges = new Range[] {
            this.mappedRange,
            this.userRange,
            this.textRange,
            this.dataRange,
            this.externRange,
            this.staticRange,
            this.dynamicRange,
            this.kernelTextRange,
            this.kernelDataRange,
            this.mmioRange,
        };
        this.locations = new int[] {
            this.exceptionHandlerAddress,
            this.initialGlobalPointer,
            this.initialStackPointer,
        };
    }

    /**
     * Construct a new <code>MemoryLayout</code> using the provided {@link Properties}. A property value is required
     * for every range and location in the layout. See the documentation for the fields in this class for the specific
     * property keys.
     *
     * @param properties The properties to initialize fields with.
     * @return The constructed <code>MemoryLayout</code>.
     * @throws IllegalArgumentException Thrown if one or more required properties is not present.
     */
    public static MemoryLayout fromProperties(Properties properties) throws IllegalArgumentException {
        return new MemoryLayout(
            requireProperty(properties, "DisplayName"),
            requireProperty(properties, "Description"),
            Range.decode(requireProperty(properties, "MappedSpace")),
            Range.decode(requireProperty(properties, "UserSpace")),
            Range.decode(requireProperty(properties, "TextSegment")),
            Range.decode(requireProperty(properties, "DataSegment")),
            Range.decode(requireProperty(properties, "ExternData")),
            Range.decode(requireProperty(properties, "StaticData")),
            Range.decode(requireProperty(properties, "HeapStackData")),
            Range.decode(requireProperty(properties, "KernelTextSegment")),
            Range.decode(requireProperty(properties, "KernelDataSegment")),
            Range.decode(requireProperty(properties, "MemoryMappedIO")),
            Binary.decodeInteger(requireProperty(properties, "ExceptionHandler")),
            Binary.decodeInteger(requireProperty(properties, "GlobalPointer")),
            Binary.decodeInteger(requireProperty(properties, "StackPointer"))
        );
    }

    /**
     * Obtain the property value for a given property key, ensuring that the property exists.
     *
     * @param properties The collection of properties to search within.
     * @param key The property key to search for.
     * @return The corresponding property value.
     * @throws IllegalArgumentException Thrown if the property is not present.
     */
    private static String requireProperty(Properties properties, String key) throws IllegalArgumentException {
        String property = properties.getProperty(key);
        if (property != null) {
            return property;
        }
        else {
            throw new IllegalArgumentException("memory layout file is missing required property '" + key + "'");
        }
    }
}
