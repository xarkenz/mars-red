package mars.mips.hardware;

import mars.Application;
import mars.assembler.BasicStatement;
import mars.mips.instructions.Instruction;
import mars.simulator.ExceptionCause;
import mars.simulator.Simulator;
import mars.util.Binary;

import java.util.*;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Representation of MIPS memory.
 *
 * @author Pete Sanderson, August 2003; Sean Clarke, May 2024
 */
public class Memory {
    /**
     * Interface representing a listener of memory read/write operations.
     * When attached to memory via {@link Memory#addListener(Listener, int, int)} or an overload,
     * it is bound to a range of memory addresses. Any operations on memory outside of that range will <i>not</i>
     * notify the listener. Note that a listener can be bound to multiple memory ranges simultaneously.
     */
    public interface Listener extends EventListener {
        /**
         * Invoked upon any write operation which is relevant to this listener's active range.
         * Does nothing by default.
         *
         * @param address     The address which was written to.
         * @param length      The number of bytes written. Will always be 1 (single byte), {@link #BYTES_PER_HALFWORD},
         *                    or {@link #BYTES_PER_WORD}.
         * @param value       The value which was written.
         * @param wordAddress The address of the word affected by the operation.
         * @param wordValue   The resulting value of the word.
         */
        default void memoryWritten(int address, int length, int value, int wordAddress, int wordValue) {
            // Do nothing by default
        }

        /**
         * Invoked upon any read operation which is relevant to this listener's active range.
         * Does nothing by default.
         *
         * @param address     The address which was read from.
         * @param length      The number of bytes read. Will always be 1 (single byte), {@link #BYTES_PER_HALFWORD},
         *                    or {@link #BYTES_PER_WORD}.
         * @param value       The value which was read.
         * @param wordAddress The address of the word affected by the operation.
         * @param wordValue   The full value of the word.
         */
        default void memoryRead(int address, int length, int value, int wordAddress, int wordValue) {
            // Do nothing by default
        }

        /**
         * Invoked when memory is reset via a call to {@link #reset()}, regardless of this listener's active range.
         * Does nothing by default.
         */
        default void memoryReset() {
            // Do nothing by default
        }
    }

    /**
     * MIPS word length in bytes.
     */
    public static final int BYTES_PER_WORD = 4;
    /**
     * MIPS halfword length in bytes.
     */
    public static final int BYTES_PER_HALFWORD = BYTES_PER_WORD / 2;
    /**
     * MIPS doubleword length in bytes.
     */
    public static final int BYTES_PER_DOUBLEWORD = BYTES_PER_WORD * 2;

    /**
     * Determine whether a given address is aligned on a word boundary.
     *
     * @param address The address to check.
     * @return True if address is word-aligned, false otherwise.
     */
    public static boolean isWordAligned(int address) {
        return (address & (BYTES_PER_WORD - 1)) == 0;
    }

    /**
     * Ensure that a given address is aligned on a word boundary, throwing an exception otherwise.
     *
     * @param address        The address to check.
     * @param exceptionCause The cause given if an exception is thrown
     *                       (usually either {@link ExceptionCause#ADDRESS_STORE}
     *                       or {@link ExceptionCause#ADDRESS_FETCH}).
     * @throws AddressErrorException Thrown if the address is not word-aligned.
     */
    public static void enforceWordAlignment(int address, int exceptionCause) throws AddressErrorException {
        if (!isWordAligned(address)) {
            throw new AddressErrorException("address not aligned on word boundary", exceptionCause, address);
        }
    }

    /**
     * Determine whether a given address is aligned on a halfword boundary.
     *
     * @param address The address to check.
     * @return True if address is halfword-aligned, false otherwise.
     */
    public static boolean isHalfwordAligned(int address) {
        return (address & (BYTES_PER_HALFWORD - 1)) == 0;
    }

    /**
     * Ensure that a given address is aligned on a halfword boundary, throwing an exception otherwise.
     *
     * @param address        The address to check.
     * @param exceptionCause The cause given if an exception is thrown
     *                       (usually either {@link ExceptionCause#ADDRESS_STORE}
     *                       or {@link ExceptionCause#ADDRESS_FETCH}).
     * @throws AddressErrorException Thrown if the address is not halfword-aligned.
     */
    public static void enforceHalfwordAlignment(int address, int exceptionCause) throws AddressErrorException {
        if (!isHalfwordAligned(address)) {
            throw new AddressErrorException("address not aligned on halfword boundary", exceptionCause, address);
        }
    }

    /**
     * Determine whether a given address is aligned on a doubleword boundary.
     *
     * @param address The address to check.
     * @return True if address is doubleword-aligned, false otherwise.
     */
    public static boolean isDoublewordAligned(int address) {
        return (address & (BYTES_PER_DOUBLEWORD - 1)) == 0;
    }

    /**
     * Ensure that a given address is aligned on a doubleword boundary, throwing an exception otherwise.
     *
     * @param address        The address to check.
     * @param exceptionCause The cause given if an exception is thrown
     *                       (usually either {@link ExceptionCause#ADDRESS_STORE}
     *                       or {@link ExceptionCause#ADDRESS_FETCH}).
     * @throws AddressErrorException Thrown if the address is not doubleword-aligned.
     */
    public static void enforceDoublewordAlignment(int address, int exceptionCause) throws AddressErrorException {
        if (!isDoublewordAligned(address)) {
            throw new AddressErrorException("address not aligned on doubleword boundary", exceptionCause, address);
        }
    }

    /**
     * Align the given address to the next alignment boundary.
     * If the address is already aligned, it is left unchanged.
     *
     * @param address   The memory address to align.
     * @param alignment The alignment required in bytes, which must be a power of 2.
     * @return The next address divisible by <code>alignment</code>.
     */
    public static int alignToNext(int address, int alignment) {
        int excess = address & (alignment - 1);
        if (excess == 0) {
            return address;
        }
        else {
            return address + alignment - excess;
        }
    }

    /**
     * Align the given address to the previous alignment boundary.
     * If the address is already aligned, it is left unchanged.
     *
     * @param address   The memory address to align.
     * @param alignment The alignment required in bytes, which must be a power of 2.
     * @return The previous address divisible by <code>alignment</code>.
     */
    public static int alignToPrevious(int address, int alignment) {
        return address & -alignment; // Simplified from "address & ~(alignment - 1)"
    }

    /**
     * Determine whether two <i>inclusive</i> ranges of <i>unsigned</i> integers intersect.
     * In order for the ranges to intersect, they must share at least one integer between them;
     * thus, ranges that "touch" but do not overlap are not considered to intersect.
     * <p>
     * For meaningful results, it should hold that <code>start1</code> &le; <code>end1</code> and
     * <code>start2</code> &le; <code>end2</code> (when treated as unsigned integers).
     *
     * @param start1 The first unsigned integer specified by the first range.
     * @param end1 The last unsigned integer specified by the first range.
     * @param start2 The first unsigned integer specified by the second range.
     * @param end2 The last unsigned integer specified by the second range.
     * @return <code>true</code> if the ranges <code>[start1, end1]</code> and <code>[start2, end2]</code>
     *         share any elements, or <code>false</code> otherwise.
     * @see #rangesMergeable(int, int, int, int)
     */
    public static boolean rangesIntersect(int start1, int end1, int start2, int end2) {
        return Integer.compareUnsigned(end1, start2) >= 0
            && Integer.compareUnsigned(start1, end2) <= 0;
    }

    /**
     * Determine whether two <i>inclusive</i> ranges of <i>unsigned</i> integers can be merged into a single range.
     * In order for the ranges to be mergeable, they must either intersect or be adjacent.
     * This is different than the condition for {@link #rangesIntersect(int, int, int, int)} since ranges
     * that only "touch" are also considered mergeable.
     * <p>
     * For meaningful results, it should hold that <code>start1</code> &le; <code>end1</code> and
     * <code>start2</code> &le; <code>end2</code> (when treated as unsigned integers).
     *
     * @param start1 The first unsigned integer specified by the first range.
     * @param end1 The last unsigned integer specified by the first range.
     * @param start2 The first unsigned integer specified by the second range.
     * @param end2 The last unsigned integer specified by the second range.
     * @return <code>true</code> if the ranges <code>[start1, end1]</code> and <code>[start2, end2]</code>
     *         can be merged into a single range, or <code>false</code> otherwise.
     * @see #rangesIntersect(int, int, int, int)
     */
    public static boolean rangesMergeable(int start1, int end1, int start2, int end2) {
        return rangesIntersect(start1, end1, start2, end2) || end1 + 1 == start2 || end2 + 1 == start1;
    }

    /**
     * The list of attached listeners along with the range of addresses each is bound to.
     */
    private final List<ListenerRange> listenerRanges = new ArrayList<>();
    /**
     * Current setting for endianness.
     */
    private Endianness endianness = Endianness.LITTLE_ENDIAN;
    /**
     * Current memory configuration, updated upon memory reset.
     */
    private MemoryConfiguration configuration;
    /**
     * The next address on the heap which will be used for dynamic memory allocation.
     */
    private int nextHeapAddress;

    private DataRegion dataSegmentRegion;
    private DataRegion kernelDataSegmentRegion;
    private DataRegion mmioRegion;
    private TextRegion textSegmentRegion;
    private TextRegion kernelTextSegmentRegion;

    private static Memory instance = null;

    /**
     * Get the singleton memory instance.
     */
    public static Memory getInstance() {
        if (instance == null) {
            instance = new Memory();
        }
        return instance;
    }

    /**
     * Reinitialize the memory contents with the current memory configuration
     * ({@link MemoryConfigurations#getCurrentConfiguration()}) and reset the heap address to its initial state.
     */
    public void reset() {
        // Update the memory configuration and endianness
        this.configuration = MemoryConfigurations.getCurrentConfiguration();
        this.endianness = (Application.getSettings().useBigEndian.get()) ? Endianness.BIG_ENDIAN : Endianness.LITTLE_ENDIAN;

        // Initialize the heap address at the bottom of the dynamic data range
        this.nextHeapAddress = alignToNext(this.getAddress(MemoryConfigurations.DYNAMIC_LOW), BYTES_PER_WORD);

        // Allocate new memory regions, which will be filled in as needed.
        // MMIO is separate because it isn't really considered part of the kernel data segment,
        // though they could have been combined in this case. Probably better to assume they aren't adjacent anyway.
        this.dataSegmentRegion = new DataRegion(this.getAddress(MemoryConfigurations.DATA_LOW), this.getAddress(MemoryConfigurations.DATA_HIGH));
        this.kernelDataSegmentRegion = new DataRegion(this.getAddress(MemoryConfigurations.KERNEL_DATA_LOW), this.getAddress(MemoryConfigurations.KERNEL_DATA_HIGH));
        this.mmioRegion = new DataRegion(this.getAddress(MemoryConfigurations.MMIO_LOW), this.getAddress(MemoryConfigurations.MMIO_HIGH));
        this.textSegmentRegion = new TextRegion(this.getAddress(MemoryConfigurations.TEXT_LOW), this.getAddress(MemoryConfigurations.TEXT_HIGH));
        this.kernelTextSegmentRegion = new TextRegion(this.getAddress(MemoryConfigurations.KERNEL_TEXT_LOW), this.getAddress(MemoryConfigurations.KERNEL_TEXT_HIGH));

        // Encourage the garbage collector to clean up any region objects now orphaned
        System.gc();

        // Notify listeners of the memory reset
        synchronized (this.listenerRanges) {
            for (ListenerRange range : this.listenerRanges) {
                range.listener.memoryReset();
            }
        }
    }

    public int getAddress(int key) {
        return this.configuration.getAddress(key);
    }

    /**
     * Get the current endianness (i.e. byte ordering) in use.
     * This reflects the value of {@link mars.settings.Settings#useBigEndian};
     * however, it is only updated when {@link #reset()} is called.
     *
     * @return Either {@link Endianness#LITTLE_ENDIAN} or {@link Endianness#BIG_ENDIAN}.
     */
    public Endianness getEndianness() {
        return this.endianness;
    }

    /**
     * Get the next address on the heap which will be used for dynamic memory allocation.
     *
     * @return The next heap address, which is always word-aligned.
     */
    public int getNextHeapAddress() {
        return this.nextHeapAddress;
    }

    /**
     * Returns the next available word-aligned heap address. This is roughly equivalent to <code>malloc</code> in C.
     * Unfortunately, there is currently no way to deallocate space. (Might be added in the future.)
     *
     * @param numBytes Number of contiguous bytes to allocate.
     * @return Address of the allocated heap storage.
     * @throws IllegalArgumentException Thrown if the number of requested bytes is negative
     *         or exceeds available heap storage.
     */
    public int allocateHeapSpace(int numBytes) throws IllegalArgumentException {
        int result = this.nextHeapAddress;
        if (numBytes < 0) {
            throw new IllegalArgumentException("invalid heap allocation of " + numBytes + " bytes requested");
        }
        int newHeapAddress = alignToNext(this.nextHeapAddress + numBytes, BYTES_PER_WORD);
        if (Integer.compareUnsigned(newHeapAddress, this.getAddress(MemoryConfigurations.DYNAMIC_HIGH)) > 0) {
            throw new IllegalArgumentException("heap allocation of " + numBytes + " bytes failed due to insufficient heap space");
        }
        this.nextHeapAddress = newHeapAddress;
        return result;
    }

    /**
     * Determine whether the current memory configuration fits a 16-bit address space.
     *
     * @return True if the highest mapped address can be stored in 16 bits, false otherwise.
     */
    public boolean isUsingCompactAddressSpace() {
        return Integer.compareUnsigned(this.getAddress(MemoryConfigurations.MAPPED_HIGH), 0xFFFF) <= 0;
    }

    /**
     * Determine whether the given memory address is contained within the text segment,
     * as defined by the current memory configuration.
     *
     * @param address The byte address to check.
     * @return True if the address is within the text segment, false otherwise.
     */
    public boolean isInTextSegment(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.TEXT_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.TEXT_HIGH)) <= 0;
    }

    /**
     * Determine whether the given memory address is contained within the kernel text segment,
     * as defined by the current memory configuration.
     *
     * @param address The byte address to check.
     * @return True if the address is within the kernel text segment, false otherwise.
     */
    public boolean isInKernelTextSegment(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.KERNEL_TEXT_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.KERNEL_TEXT_HIGH)) <= 0;
    }

    /**
     * Determine whether the given memory address is contained within the data segment,
     * as defined by the current memory configuration.
     *
     * @param address The byte address to check.
     * @return True if the address is within the data segment, false otherwise.
     */
    public boolean isInDataSegment(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.DATA_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.DATA_HIGH)) <= 0;
    }

    /**
     * Determine whether the given memory address is contained within the kernel data segment,
     * as defined by the current memory configuration.
     *
     * @param address The byte address to check.
     * @return True if the address is within the kernel data segment, false otherwise.
     */
    public boolean isInKernelDataSegment(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.KERNEL_DATA_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.KERNEL_DATA_HIGH)) <= 0;
    }

    /**
     * Determine whether the given memory address is contained within the memory-mapped I/O range,
     * as defined by the current memory configuration.
     *
     * @param address The byte address to check.
     * @return True if the address is within the memory-mapped I/O range, false otherwise.
     */
    public boolean isInMemoryMappedIO(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.MMIO_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.MMIO_HIGH)) <= 0;
    }

    /**
     * Get the data memory region a given address is mapped to, if applicable.
     *
     * @param address The address to find the region for.
     * @return The corresponding data region, or null if no data regions use the given address.
     */
    public DataRegion getDataRegionForAddress(int address) {
        if (this.isInDataSegment(address)) {
            return this.dataSegmentRegion;
        }
        else if (this.isInKernelDataSegment(address)) {
            return this.kernelDataSegmentRegion;
        }
        else if (this.isInMemoryMappedIO(address)) {
            return this.mmioRegion;
        }
        else {
            return null;
        }
    }

    /**
     * Get the text memory region a given address is mapped to, if applicable.
     *
     * @param address The address to find the region for.
     * @return The corresponding text region, or null if no text regions use the given address.
     */
    public TextRegion getTextRegionForAddress(int address) {
        if (this.isInTextSegment(address)) {
            return this.textSegmentRegion;
        }
        else if (this.isInKernelTextSegment(address)) {
            return this.kernelTextSegmentRegion;
        }
        else {
            return null;
        }
    }

    /**
     * Store a byte, halfword, or word in memory at a given address, which must be aligned properly.
     * May write to a memory region containing text, but only if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to true.
     * <p>
     * This method simply delegates to {@link #storeByte}, {@link #storeHalfword}, or {@link #storeWord}.
     *
     * @param address Address where memory will be written.
     * @param value   Value to be stored at that address.
     * @param length  Number of bytes to be written. Must be 1 (single byte), {@link #BYTES_PER_HALFWORD},
     *                or {@link #BYTES_PER_WORD}. No other values are accepted.
     * @param notify  Whether to notify listeners of the write operation.
     * @throws AddressErrorException Thrown if the given address is not word-aligned, is out of range,
     *         or does not allow this operation.
     */
    public void store(int address, int value, int length, boolean notify) throws AddressErrorException {
        switch (length) {
            case 1 -> this.storeByte(address, value, notify);
            case BYTES_PER_HALFWORD -> this.storeHalfword(address, value, notify);
            case BYTES_PER_WORD -> this.storeWord(address, value, notify);
            default -> throw new IllegalArgumentException("invalid number of bytes: " + length);
        }
    }

    /**
     * Store a word in memory at a given address, which must be aligned to a word boundary.
     * May write to a memory region containing text, but only if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to true.
     *
     * @param address Address of the word where memory will be written.
     * @param value   Value to be stored at that address.
     * @param notify  Whether to notify listeners of the write operation.
     * @throws AddressErrorException Thrown if the given address is not word-aligned, is out of range,
     *         or does not allow this operation.
     */
    public void storeWord(int address, int value, boolean notify) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_STORE);

        DataRegion dataRegion;
        TextRegion textRegion;
        if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            // Falls within a region containing data
            int oldValue = dataRegion.storeWord(address, value);
            // Add a corresponding backstep for the write
            Simulator.getInstance().getBackStepper().wordWritten(address, oldValue);
        }
        else if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            // Falls within a region containing text
            // Burch Mod (Jan 2013): replace throw with call to storeStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
                throw new AddressErrorException("cannot write to text segment unless self-modifying code is enabled", ExceptionCause.ADDRESS_STORE, address);
            }
            BasicStatement statement = Application.instructionSet.getDecoder().decodeStatement(value);
            BasicStatement oldStatement = textRegion.storeStatement(address, statement);
            // Add a corresponding backstep for the write
            Simulator.getInstance().getBackStepper().statementWritten(address, oldStatement);
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_STORE, address);
        }

        if (notify) {
            // Notify listeners of the write operation
            this.dispatchWriteEvent(address, BYTES_PER_WORD, value, address, value);
        }
    }

    /**
     * Store a halfword in memory at a given address, which must be aligned to a halfword boundary.
     * May write to a memory region containing text, but only if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to true.
     *
     * @param address Address of the halfword where memory will be written.
     * @param value   Value to be stored at that address. (Only the lowest 16 bits are used.)
     * @param notify  Whether to notify listeners of the write operation.
     * @throws AddressErrorException Thrown if the given address is not halfword-aligned, is out of range,
     *         or does not allow this operation.
     */
    public void storeHalfword(int address, int value, boolean notify) throws AddressErrorException {
        enforceHalfwordAlignment(address, ExceptionCause.ADDRESS_STORE);
        // Discard all but the lowest 16 bits
        value &= 0xFFFF;

        // Fetch the surrounding word from memory
        int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
        int wordValue = this.fetchWord(wordAddress, false);

        if ((this.endianness == Endianness.BIG_ENDIAN) == (address == wordAddress)) {
            // Write to high-order halfword
            wordValue = (wordValue & 0x0000FFFF) | (value << 16);
        }
        else {
            // Write to low-order halfword
            wordValue = (wordValue & 0xFFFF0000) | value;
        }

        // Store the augmented word back into memory
        this.storeWord(wordAddress, wordValue, false);

        if (notify) {
            // Notify listeners of the write operation
            this.dispatchWriteEvent(address, BYTES_PER_HALFWORD, value, wordAddress, wordValue);
        }
    }

    /**
     * Store a byte in memory at a given address. May write to a memory region containing text, but only if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to true.
     *
     * @param address Address of the byte where memory will be written.
     * @param value   Value to be stored at that address. (Only the lowest 8 bits are used.)
     * @param notify  Whether to notify listeners of the write operation.
     * @throws AddressErrorException Thrown if the given address is out of range or does not allow this operation.
     */
    public void storeByte(int address, int value, boolean notify) throws AddressErrorException {
        // Discard all but the lowest 8 bits
        value &= 0xFF;

        // Fetch the surrounding word from memory
        int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
        int wordValue = this.fetchWord(wordAddress, false);

        // Use the endianness setting to write to the correct inner byte
        wordValue = Binary.setByte(wordValue, switch (this.endianness) {
            case BIG_ENDIAN -> wordAddress - address + (BYTES_PER_WORD - 1);
            case LITTLE_ENDIAN -> address - wordAddress;
        }, value);

        // Store the augmented word back into memory
        this.storeWord(wordAddress, wordValue, false);

        if (notify) {
            // Notify listeners of the write operation
            this.dispatchWriteEvent(address, 1, value, wordAddress, wordValue);
        }
    }

    /**
     * Store a doubleword in memory at a given address, which must be aligned to a word boundary.
     * (Note that aligning to a doubleword boundary is not required.)
     * May write to a memory region containing text, but only if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to true.
     *
     * @param address Address of the first word where memory will be written.
     * @param value   Value to be stored at that address.
     * @param notify  Whether to notify listeners of the 2 write operations (1 for each word).
     * @throws AddressErrorException Thrown if the given address is not word-aligned, is out of range,
     *         or does not allow this operation.
     */
    public void storeDoubleword(int address, long value, boolean notify) throws AddressErrorException {
        // For some reason, both SPIM and previous versions of MARS enforce only word alignment here,
        // so I'll keep it that way for backwards compatibility.  Sean Clarke 05/2024
        enforceWordAlignment(address, ExceptionCause.ADDRESS_STORE);

        // Order the words in memory according to the endianness setting
        switch (this.endianness) {
            case BIG_ENDIAN -> {
                this.storeWord(address, Binary.highOrderLongToInt(value), notify);
                this.storeWord(address + BYTES_PER_WORD, Binary.lowOrderLongToInt(value), notify);
            }
            case LITTLE_ENDIAN -> {
                this.storeWord(address, Binary.lowOrderLongToInt(value), notify);
                this.storeWord(address + BYTES_PER_WORD, Binary.highOrderLongToInt(value), notify);
            }
        }
    }

    /**
     * Store a statement in memory at a given address, which must be aligned to a word boundary.
     * May write to a memory region containing data, but only if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to true.
     *
     * @param address   Word-aligned address where memory will be written.
     * @param statement Value to be stored at that address.
     * @param notify    Whether to notify listeners of the write operation.
     * @throws AddressErrorException Thrown if the given address is not word-aligned, is out of range,
     *         or does not allow this operation.
     */
    public void storeStatement(int address, BasicStatement statement, boolean notify) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_STORE);

        // Obtain the binary representation of the statement
        int binaryStatement = (statement == null) ? 0 : statement.getBinaryEncoding();

        TextRegion textRegion;
        DataRegion dataRegion;
        if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            // Falls within a region containing text
            BasicStatement oldStatement = textRegion.storeStatement(address, statement);
            // Add a corresponding backstep for the write
            Simulator.getInstance().getBackStepper().statementWritten(address, oldStatement);
        }
        else if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            // Falls within a region containing data
            if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
                throw new AddressErrorException("cannot store code beyond text segment unless self-modifying code is enabled", ExceptionCause.ADDRESS_FETCH, address);
            }
            dataRegion.storeWord(address, binaryStatement);
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_FETCH, address);
        }

        if (notify) {
            // Notify listeners of the write operation
            this.dispatchWriteEvent(address, BYTES_PER_WORD, binaryStatement, address, binaryStatement);
        }
    }

    /**
     * Fetch a byte, halfword, or word from memory at a given address, which must be aligned properly.
     * May read from a memory region containing text, even if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to false.
     * <p>
     * This method simply delegates to {@link #fetchByte}, {@link #fetchHalfword}, or {@link #fetchWord}.
     *
     * @param address Address where memory will be read.
     * @param length  Number of bytes to read. Must be 1 (single byte), {@link #BYTES_PER_HALFWORD},
     *                or {@link #BYTES_PER_WORD}. No other values are accepted.
     * @param notify  Whether to notify listeners of the read operation.
     * @return The value fetched from memory.
     * @throws AddressErrorException Thrown if the given address is not word-aligned or is out of range.
     */
    public int fetch(int address, int length, boolean notify) throws AddressErrorException {
        return switch (length) {
            case 1 -> this.fetchByte(address, notify);
            case BYTES_PER_HALFWORD -> this.fetchHalfword(address, notify);
            case BYTES_PER_WORD -> this.fetchWord(address, notify);
            default -> throw new IllegalArgumentException("invalid number of bytes: " + length);
        };
    }

    /**
     * Fetch a word from memory at a given address, which must be aligned to a word boundary.
     * May read from a memory region containing text, even if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to false.
     *
     * @param address Address of the word to fetch.
     * @param notify  Whether to notify listeners of the read operation.
     * @return The value fetched from memory.
     * @throws AddressErrorException Thrown if the given address is not word-aligned or is out of range.
     */
    public int fetchWord(int address, boolean notify) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_FETCH);

        int value;
        DataRegion dataRegion;
        TextRegion textRegion;
        if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            // Falls within a region containing data
            value = dataRegion.fetchWord(address);
        }
        else if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            // Falls within a region containing text
            // Burch Mod (Jan 2013): replace throw with calls to fetchStatement & getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            BasicStatement statement = textRegion.fetchStatement(address);
            value = statement == null ? 0 : statement.getBinaryEncoding();
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_FETCH, address);
        }

        if (notify) {
            // Notify listeners of the read operation
            this.dispatchReadEvent(address, BYTES_PER_WORD, value, address, value);
        }
        return value;
    }

    /**
     * Fetch a word from memory at a given address, which must be aligned to a word boundary.
     * May read from a memory region containing text, even if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to false.
     * Listeners are not notified of a read operation.
     * <p>
     * Returns null if one of the following is true:
     * <ul>
     * <li>The address lies in a region containing text and there is no instruction at the given address.
     * <li>The address lies in a region containing data and the 4-KiB block surrounding it
     *     has not been written to since the last call to {@link #reset()}.
     * <li>The address lies in an unmapped portion of memory.
     * </ul>
     * <p>
     * This method was originally developed by Greg Giberling of UC Berkeley to support the memory
     * dump feature that he implemented in Fall 2007.
     *
     * @param address Word-aligned address where memory will be read.
     * @return The value fetched from memory, or null as described above.
     * @throws AddressErrorException Thrown if the given address is not word-aligned.
     */
    public Integer fetchWordOrNull(int address) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_FETCH);

        DataRegion dataRegion;
        TextRegion textRegion;
        if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            // Falls within a region containing data
            return dataRegion.fetchWordOrNull(address);
        }
        else if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            // Falls within a region containing text
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify & getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            BasicStatement statement = textRegion.fetchStatement(address);
            return statement == null ? null : statement.getBinaryEncoding();
        }
        else {
            // Falls outside mapped addressing range
            return null;
        }
    }

    /**
     * Fetch a halfword from memory at a given address, which must be aligned to a halfword boundary.
     * May read from a memory region containing text, even if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to false.
     *
     * @param address Address of the halfword to fetch.
     * @param notify  Whether to notify listeners of the read operation.
     * @return The zero-extended value fetched from memory. (Only the lowest 16 bits are used.)
     * @throws AddressErrorException Thrown if the given address is not halfword-aligned or is out of range.
     */
    public int fetchHalfword(int address, boolean notify) throws AddressErrorException {
        enforceHalfwordAlignment(address, ExceptionCause.ADDRESS_FETCH);

        // Fetch the surrounding word from memory
        int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
        int wordValue = this.fetchWord(wordAddress, false);

        int value;
        if ((this.endianness == Endianness.BIG_ENDIAN) == (address == wordAddress)) {
            // Extract the high-order halfword
            value = wordValue >>> 16;
        }
        else {
            // Extract the low-order halfword
            value = wordValue & 0xFFFF;
        }

        if (notify) {
            // Notify listeners of the read operation
            this.dispatchReadEvent(address, BYTES_PER_HALFWORD, value, wordAddress, wordValue);
        }
        return value;
    }

    /**
     * Fetch a byte from memory at a given address. May read from a memory region containing text, even if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to false.
     *
     * @param address Address of the byte to fetch.
     * @param notify  Whether to notify listeners of the read operation.
     * @return The zero-extended value fetched from memory. (Only the lowest 8 bits are used.)
     * @throws AddressErrorException Thrown if the given address is out of range.
     */
    public int fetchByte(int address, boolean notify) throws AddressErrorException {
        // Fetch the surrounding word from memory
        int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
        int wordValue = this.fetchWord(wordAddress, false);

        // Use the endianness setting to extract the correct inner byte
        int value = Binary.getByte(wordValue, switch (this.endianness) {
            case BIG_ENDIAN -> wordAddress - address + (BYTES_PER_WORD - 1);
            case LITTLE_ENDIAN -> address - wordAddress;
        });

        if (notify) {
            // Notify listeners of the read operation
            this.dispatchReadEvent(address, 1, value, wordAddress, wordValue);
        }
        return value;
    }

    /**
     * Fetch a doubleword from memory at a given address, which must be aligned to a word boundary.
     * (Note that aligning to a doubleword boundary is not required.)
     * May read from a memory region containing text, even if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to false.
     *
     * @param address Address of the first word to fetch.
     * @param notify  Whether to notify listeners of the 2 read operations (1 for each word).
     * @return The value fetched from memory.
     * @throws AddressErrorException Thrown if the given address is not word-aligned or is out of range.
     */
    public long fetchDoubleword(int address, boolean notify) throws AddressErrorException {
        // For some reason, both SPIM and previous versions of MARS enforce only word alignment here,
        // so I'll keep it that way for backwards compatibility.  Sean Clarke 05/2024
        enforceWordAlignment(address, ExceptionCause.ADDRESS_FETCH);

        // Fetch the two relevant words from memory
        int firstWord = this.fetchWord(address, notify);
        int secondWord = this.fetchWord(address + BYTES_PER_WORD, notify);

        // Reconstruct the value according to the endianness setting
        return switch (this.endianness) {
            case BIG_ENDIAN -> Binary.twoIntsToLong(firstWord, secondWord);
            case LITTLE_ENDIAN -> Binary.twoIntsToLong(secondWord, firstWord);
        };
    }

    /**
     * Fetch a statement from memory at a given address, which must be aligned to a word boundary.
     * May read from a memory region containing data, but only if
     * {@link mars.settings.Settings#selfModifyingCodeEnabled} is set to true.
     *
     * @param address Address of the statement to fetch.
     * @param notify  Whether to notify listeners of the read operation.
     * @return The statement fetched from memory, or null if no statement exists at the given address.
     * @throws AddressErrorException Thrown if the given address is not word-aligned, is out of range,
     *         or does not allow this operation.
     */
    public BasicStatement fetchStatement(int address, boolean notify) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_FETCH);

        BasicStatement statement;
        TextRegion textRegion;
        DataRegion dataRegion;
        if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            // Falls within a region containing text
            statement = textRegion.fetchStatement(address);
        }
        else if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            // Falls within a region containing data
            if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
                throw new AddressErrorException("cannot execute beyond text segment unless self-modifying code is enabled", ExceptionCause.ADDRESS_FETCH, address);
            }
            Integer binaryStatement = dataRegion.fetchWordOrNull(address);
            statement = (binaryStatement == null) ? null : Application.instructionSet.getDecoder().decodeStatement(binaryStatement);
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_FETCH, address);
        }

        if (notify) {
            // Notify listeners of the read operation
            int binaryStatement = (statement == null) ? 0 : statement.getBinaryEncoding();
            this.dispatchReadEvent(address, Instruction.BYTES_PER_INSTRUCTION, binaryStatement, address, binaryStatement);
        }
        return statement;
    }

    /**
     * Reads a null-terminated string from memory starting at the given address.
     *
     * @param address The address of the first byte in the string.
     * @return The string from memory, excluding the null terminator.
     * @throws AddressErrorException If a byte cannot be read at any point.
     */
    public String fetchNullTerminatedString(int address) throws AddressErrorException {
        StringBuilder content = new StringBuilder();

        char ch;
        while ((ch = (char) this.fetchByte(address++, false)) != 0) {
            content.append(ch);
        }

        return content.toString();
    }

    /**
     * Search for first "null" memory value in an address range, as indicated by the return value
     * of {@link #fetchWordOrNull}.
     *
     * @param firstAddress First address to be searched; the starting point. Must be word-aligned.
     * @param lastAddress Last address to be searched.
     * @return Address of the first word within the specified range that does not contain a value.
     * @throws AddressErrorException Thrown if the first address is not word-aligned.
     */
    public int getAddressOfFirstNullWord(int firstAddress, int lastAddress) throws AddressErrorException {
        int address;
        for (address = firstAddress; address < lastAddress; address += BYTES_PER_WORD) {
            if (this.fetchWordOrNull(address) == null) {
                break;
            }
        }
        return address;
    }

    /**
     * Class representing an arbitrary contiguous region of memory which contains data.
     * <p>
     * Obviously, allocating one array for the entire region of memory would be extremely wasteful
     * (and perhaps not even possible), so the region is divided into many fixed-size tables,
     * each of which is further subdivided into many blocks, a block being an array of memory words.
     * Currently, each block contains 1024 words, and each table contains 1024 blocks. Thus, each block
     * holds 4 KiB of data, and each table can accommodate up to 4 MiB of data in total.
     * <p>
     * Although this structure uses a three-dimensional array, this makes it relatively space-efficient since
     * only the outermost array of tables is allocated initially. Tables and blocks are only allocated
     * when a write occurs somewhere in their range; otherwise, they are simply left as null references.
     * In addition, the array of tables only has as many elements as is necessary to fully cover the region.
     * A base address is used as the memory offset of the first table in the array.
     * <p>
     * Sean Clarke (05/2024): The note below is interesting, but not really relevant. I'll keep it here for now.
     * <p>
     * The SPIM simulator stores statically allocated data (i.e. data following a <code>.data</code> directive)
     * starting at address <code>0x10010000</code>. This is the first word beyond the reach of <code>$gp</code>
     * used in conjunction with a signed 16-bit immediate offset. <code>$gp</code> has value <code>0x10008000</code>,
     * and with the signed 16-bit offset, it can reach from <code>0x10008000 - 0x8000 = 0x10000000</code>
     * (base of the data segment) to <code>0x10008000 + 0x7FFF = 0x1000FFFF</code>
     * (the byte preceding <code>0x10010000</code>).
     */
    public static class DataRegion {
        private static final int WORDS_PER_BLOCK = 1024;
        private static final int BLOCKS_PER_TABLE = 1024;
        private static final int WORDS_PER_TABLE = WORDS_PER_BLOCK * BLOCKS_PER_TABLE;
        private static final int BYTES_PER_TABLE = BYTES_PER_WORD * WORDS_PER_TABLE;

        private static int getWordIndex(int wordOffset) {
            return wordOffset & (WORDS_PER_BLOCK - 1);
        }

        private static int getBlockIndex(int wordOffset) {
            return (wordOffset / WORDS_PER_BLOCK) & (BLOCKS_PER_TABLE - 1);
        }

        private static int getTableIndex(int wordOffset) {
            return wordOffset / WORDS_PER_TABLE;
        }

        private final int[][][] tables;
        private final int baseAddress;

        /**
         * Allocate a new region of memory containing data.
         *
         * @param firstAddress The lowest address this region is required to contain.
         * @param lastAddress  The highest address this region is required to contain.
         */
        public DataRegion(int firstAddress, int lastAddress) {
            // Get the base address, which must be aligned to a table boundary
            this.baseAddress = alignToPrevious(firstAddress, BYTES_PER_TABLE);
            // Determine how many tables are needed to cover the region
            int tableCount = (lastAddress - this.baseAddress) / BYTES_PER_TABLE + 1;
            // Allocate an array which can hold that many tables
            this.tables = new int[tableCount][][];
        }

        /**
         * Store a word in the region at a given address.
         * The caller is responsible for ensuring that the address is word-aligned and falls within this region,
         * as no checking will be done.
         *
         * @param address The address to store the word at.
         * @param value   The value to store at the given address.
         * @return The previous value which was overwritten (defaults to 0).
         */
        public synchronized int storeWord(int address, int value) {
            // Compute the indices for the address
            int wordOffset = (address - this.baseAddress) >>> 2;
            int wordIndex = getWordIndex(wordOffset);
            int blockIndex = getBlockIndex(wordOffset);
            int tableIndex = getTableIndex(wordOffset);

            // Allocate the table if necessary
            if (this.tables[tableIndex] == null) {
                this.tables[tableIndex] = new int[BLOCKS_PER_TABLE][];
            }
            // Allocate the block if necessary
            if (this.tables[tableIndex][blockIndex] == null) {
                this.tables[tableIndex][blockIndex] = new int[WORDS_PER_BLOCK];
            }

            int oldValue = this.tables[tableIndex][blockIndex][wordIndex];
            this.tables[tableIndex][blockIndex][wordIndex] = value;
            return oldValue;
        }

        /**
         * Fetch a word from the region at a given address.
         * The caller is responsible for ensuring that the address is word-aligned and falls within this region,
         * as no checking will be done.
         *
         * @param address The address of the word to fetch.
         * @return The value stored at the given address (defaults to 0).
         */
        public synchronized int fetchWord(int address) {
            // Compute the indices for the address
            int wordOffset = (address - this.baseAddress) >>> 2;
            int wordIndex = getWordIndex(wordOffset);
            int blockIndex = getBlockIndex(wordOffset);
            int tableIndex = getTableIndex(wordOffset);

            if (this.tables[tableIndex] == null || this.tables[tableIndex][blockIndex] == null) {
                // The table or block has not been allocated, so assume it is 0 by default
                return 0;
            }
            else {
                return this.tables[tableIndex][blockIndex][wordIndex];
            }
        }

        /**
         * Fetch a word from the region at a given address, or null if the word lies in a block
         * which has not been allocated before.
         * The caller is responsible for ensuring that the address is word-aligned and falls within this region,
         * as no checking will be done.
         * <p>
         * Originally developed by Greg Gibeling of UC Berkeley, fall 2007.
         *
         * @param address The address of the word to fetch.
         * @return The value stored at the given address (defaults to 0 within allocated blocks).
         */
        public synchronized Integer fetchWordOrNull(int address) {
            // Compute the indices for the address
            int wordOffset = (address - this.baseAddress) >>> 2;
            int wordIndex = getWordIndex(wordOffset);
            int blockIndex = getBlockIndex(wordOffset);
            int tableIndex = getTableIndex(wordOffset);

            if (this.tables[tableIndex] == null || this.tables[tableIndex][blockIndex] == null) {
                // The table or block has not been allocated, so return null
                return null;
            }
            else {
                return this.tables[tableIndex][blockIndex][wordIndex];
            }
        }
    }

    /**
     * Class representing an arbitrary contiguous region of memory which contains text.
     * <p>
     * This uses a very similar structure to {@link DataRegion}. The primary difference is that <code>DataRegion</code>
     * contains raw memory words at the innermost level, whereas this contains references to
     * {@link BasicStatement} objects instead. While less space-efficient, it is much more time-efficient
     * for the {@link mars.simulator.Simulator} as it reads the code in the text segment.
     * (Especially since the current code to construct a <code>BasicStatement</code> from its binary data
     * isn't too great, which is certainly on the to-do list to improve.)
     */
    public static class TextRegion {
        private static final int WORDS_PER_BLOCK = 1024;
        private static final int BLOCKS_PER_TABLE = 1024;
        private static final int WORDS_PER_TABLE = WORDS_PER_BLOCK * BLOCKS_PER_TABLE;
        private static final int BYTES_PER_TABLE = BYTES_PER_WORD * WORDS_PER_TABLE;

        private static int getWordIndex(int wordOffset) {
            return wordOffset & (WORDS_PER_BLOCK - 1);
        }

        private static int getBlockIndex(int wordOffset) {
            return (wordOffset / WORDS_PER_BLOCK) & (BLOCKS_PER_TABLE - 1);
        }

        private static int getTableIndex(int wordOffset) {
            return wordOffset / WORDS_PER_TABLE;
        }

        private final BasicStatement[][][] tables;
        private final int baseAddress;

        /**
         * Allocate a new region of memory containing text.
         *
         * @param firstAddress The lowest address this region is required to contain.
         * @param lastAddress  The highest address this region is required to contain.
         */
        public TextRegion(int firstAddress, int lastAddress) {
            // Get the base address, which must be aligned to a table boundary
            this.baseAddress = alignToPrevious(firstAddress, BYTES_PER_TABLE);
            // Determine how many tables are needed to cover the region
            int tableCount = (lastAddress - this.baseAddress) / BYTES_PER_TABLE + 1;
            // Allocate an array which can hold that many tables
            this.tables = new BasicStatement[tableCount][][];
        }

        /**
         * Store a statement in the region at a given address.
         * The caller is responsible for ensuring that the address is word-aligned and falls within this region,
         * as no checking will be done.
         *
         * @param address   The address to store the statement at.
         * @param statement The statement to store at the given address.
         * @return The previous statement which was overwritten (defaults to null).
         */
        public synchronized BasicStatement storeStatement(int address, BasicStatement statement) {
            // Compute the indices for the address
            int wordOffset = (address - this.baseAddress) >>> 2;
            int wordIndex = getWordIndex(wordOffset);
            int blockIndex = getBlockIndex(wordOffset);
            int tableIndex = getTableIndex(wordOffset);

            if (this.tables[tableIndex] == null) {
                this.tables[tableIndex] = new BasicStatement[BLOCKS_PER_TABLE][];
            }
            if (this.tables[tableIndex][blockIndex] == null) {
                this.tables[tableIndex][blockIndex] = new BasicStatement[WORDS_PER_BLOCK];
            }

            BasicStatement oldStatement = this.tables[tableIndex][blockIndex][wordIndex];
            this.tables[tableIndex][blockIndex][wordIndex] = statement;
            return oldStatement;
        }

        /**
         * Fetch a statement from the region at a given address.
         * The caller is responsible for ensuring that the address is word-aligned and falls within this region,
         * as no checking will be done.
         *
         * @param address The address of the statement to fetch.
         * @return The statement stored at the given address (defaults to null).
         */
        public synchronized BasicStatement fetchStatement(int address) {
            // Compute the indices for the address
            int wordOffset = (address - this.baseAddress) >>> 2;
            int wordIndex = getWordIndex(wordOffset);
            int blockIndex = getBlockIndex(wordOffset);
            int tableIndex = getTableIndex(wordOffset);

            if (this.tables[tableIndex] == null || this.tables[tableIndex][blockIndex] == null) {
                // The table or block has not been allocated, so return null
                return null;
            }
            else {
                return this.tables[tableIndex][blockIndex][wordIndex];
            }
        }
    }

    /**
     * Record representing a memory listener combined with its range of applicable addresses.
     *
     * @param listener     The memory listener itself.
     * @param firstAddress The first address the listener is registered to.
     * @param lastAddress  The last address the listener is registered to.
     */
    private record ListenerRange(Listener listener, int firstAddress, int lastAddress) {
        /**
         * Determine whether a read/write operation starting at <code>address</code> and affecting <code>length</code>
         * bytes should notify the listener bound to this range.
         *
         * @param address The address of the first byte affected.
         * @param length  The number of bytes affected.
         * @return <code>true</code> if the range formed by the address and length intersects this range,
         *         or <code>false</code> otherwise.
         */
        public boolean contains(int address, int length) {
            return rangesIntersect(address, address + length - 1, this.firstAddress, this.lastAddress);
        }
    }

    /**
     * Register the given listener to all memory addresses (<code>0x00000000</code> through <code>0xFFFFFFFF</code>).
     * The listener will be notified when any read/write operations occur anywhere in memory.
     * <p>
     * This is equivalent to calling {@link #addListener(Listener, int, int) addListener(listener, 0x00000000, 0xFFFFFFFF)}.
     *
     * @param listener The listener to add.
     */
    public void addListener(Listener listener) {
        this.addListener(listener, 0x00000000, 0xFFFFFFFF);
    }

    /**
     * Register the given listener to a single address.
     * The listener will be notified when any read/write operations occur on the byte at that address.
     * <p>
     * This is equivalent to calling {@link #addListener(Listener, int, int) addListener(listener, address, address)}.
     *
     * @param listener The listener to add.
     * @param address  The memory address of the byte to attach the listener to.
     */
    public void addListener(Listener listener, int address) {
        this.addListener(listener, address, address);
    }

    /**
     * Register the given listener to a range of memory addresses. The range is inclusive;
     * that is, all bytes starting at <code>firstAddress</code> up to (and including) <code>lastAddress</code>
     * will notify the listener if a read/write operation occurs.
     * <p>
     * If the <code>listener</code> object has already been attached to an adjacent or overlapping range of memory,
     * those ranges will be merged
     *
     * @param listener     The listener to add.
     * @param firstAddress The memory address of the first byte in the range.
     * @param lastAddress  The memory address of the last byte in the range.
     */
    public void addListener(Listener listener, int firstAddress, int lastAddress) {
        if (Integer.compareUnsigned(firstAddress, lastAddress) > 0) {
            // This is usually due to programmer error
            throw new IllegalArgumentException("firstAddress > lastAddress");
        }
        synchronized (this.listenerRanges) {
            // If possible, merge existing ranges this listener is assigned to
            // This is probably completely unnecessary to do, but might as well
            Iterator<ListenerRange> iterator = this.listenerRanges.iterator();
            while (iterator.hasNext()) {
                ListenerRange range = iterator.next();
                if (listener.equals(range.listener) && rangesMergeable(firstAddress, lastAddress, range.firstAddress, range.lastAddress)) {
                    // There don't seem to be unsigned alternatives for Math.min() and Math.max()...
                    // firstAddress = Integer.minUnsigned(firstAddress, range.firstAddress);
                    if (Integer.compareUnsigned(range.firstAddress, firstAddress) < 0) {
                        firstAddress = range.firstAddress;
                    }
                    // lastAddress = Integer.maxUnsigned(lastAddress, range.lastAddress);
                    if (Integer.compareUnsigned(range.lastAddress, lastAddress) > 0) {
                        lastAddress = range.lastAddress;
                    }
                    iterator.remove();
                }
            }
            // Now that we have the final bounds for the listener, add it to the list
            this.listenerRanges.add(new ListenerRange(listener, firstAddress, lastAddress));
        }
    }

    /**
     * Unregister the given listener from <i>all</i> memory addresses it is registered to.
     * The listener will no longer be notified of any read/write operations unless it is added again.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(Listener listener) {
        synchronized (this.listenerRanges) {
            this.listenerRanges.removeIf(range -> range.listener.equals(listener));
        }
    }

    private List<ListenerRange> getListeners(int address, int length) {
        List<ListenerRange> listeners = new ArrayList<>();
        synchronized (this.listenerRanges) {
            for (ListenerRange range : this.listenerRanges) {
                if (range.contains(address, length)) {
                    listeners.add(range);
                }
            }
        }
        return listeners;
    }

    private void dispatchReadEvent(int address, int length, int value, int wordAddress, int wordValue) {
        for (ListenerRange range : this.getListeners(address, length)) {
            range.listener.memoryRead(address, length, value, wordAddress, wordValue);
        }
    }

    private void dispatchWriteEvent(int address, int length, int value, int wordAddress, int wordValue) {
        for (ListenerRange range : this.getListeners(address, length)) {
            range.listener.memoryWritten(address, length, value, wordAddress, wordValue);
        }
    }
}
