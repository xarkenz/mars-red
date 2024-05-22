package mars.mips.hardware;

import mars.Application;
import mars.ProgramStatement;
import mars.mips.instructions.Instruction;
import mars.simulator.ExceptionCause;
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
 * @author Pete Sanderson, August 2003
 */
public class Memory {
    public interface Listener {
        default void memoryWritten(int address, int length, int value, int wordAddress, int wordValue) {
            // Do nothing by default
        }

        default void memoryRead(int address, int length, int value, int wordAddress, int wordValue) {
            // Do nothing by default
        }
    }

    /**
     * MIPS word length in bytes.
     */
    // NOTE:  Much of the code is hardwired for 4 byte words.  Refactoring this is low priority.
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
     * Current setting for endianness (default is {@link Endianness#LITTLE_ENDIAN}).
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

    /**
     * Memory will maintain a collection of observables.  Each one is associated
     * with a specific memory address or address range, and each will have at least
     * one observer registered with it.  When memory access is made, make sure only
     * observables associated with that address send notices to their observers.
     * This assures that observers are not bombarded with notices from memory
     * addresses they do not care about.
     * <p>
     * Would like a tree-like implementation, but that is complicated by this fact:
     * key for insertion into the tree would be based on Comparable using both low
     * and high end of address range, but retrieval from the tree has to be based
     * on target address being ANYWHERE IN THE RANGE (not an exact key match).
     */
    private final List<ListenerRange> listenerRanges = new ArrayList<>();

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
     * Initialize the memory tables and other instance variables.
     */
    public void reset() {
        this.configuration = MemoryConfigurations.getCurrentConfiguration();
        this.nextHeapAddress = alignToNext(this.getAddress(MemoryConfigurations.DYNAMIC_LOW), BYTES_PER_WORD);

        this.dataSegmentRegion = new DataRegion(this.getAddress(MemoryConfigurations.DATA_LOW), this.getAddress(MemoryConfigurations.DATA_HIGH));
        this.kernelDataSegmentRegion = new DataRegion(this.getAddress(MemoryConfigurations.KERNEL_DATA_LOW), this.getAddress(MemoryConfigurations.KERNEL_DATA_HIGH));
        this.mmioRegion = new DataRegion(this.getAddress(MemoryConfigurations.MMIO_LOW), this.getAddress(MemoryConfigurations.MMIO_HIGH));
        this.textSegmentRegion = new TextRegion(this.getAddress(MemoryConfigurations.TEXT_LOW), this.getAddress(MemoryConfigurations.TEXT_HIGH));
        this.kernelTextSegmentRegion = new TextRegion(this.getAddress(MemoryConfigurations.KERNEL_TEXT_LOW), this.getAddress(MemoryConfigurations.KERNEL_TEXT_HIGH));

        // Encourage the garbage collector to clean up any region objects now orphaned
        System.gc();
    }

    public int getAddress(int key) {
        return this.configuration.getAddress(key);
    }

    /**
     * Get the current endianness (i.e. byte ordering) in use.
     * Unless {@link #setEndianness} is called, the default setting is {@link Endianness#LITTLE_ENDIAN}.
     *
     * @return Either {@link Endianness#LITTLE_ENDIAN} or {@link Endianness#BIG_ENDIAN}.
     */
    public Endianness getEndianness() {
        return this.endianness;
    }

    /**
     * Set the endianness (i.e. byte ordering) for memory to use.
     * Before this method is called, the default setting is {@link Endianness#LITTLE_ENDIAN}.
     *
     * @param endianness Either {@link Endianness#LITTLE_ENDIAN} or {@link Endianness#BIG_ENDIAN}.
     */
    public void setEndianness(Endianness endianness) {
        this.endianness = endianness;
    }

    /**
     * Get the next address on the heap which will be used for dynamic memory allocation.
     *
     * @return The next heap address.
     */
    public int getNextHeapAddress() {
        return this.nextHeapAddress;
    }

    /**
     * Returns the next available word-aligned heap address.
     * There is nearly 4MB of heap space available, although there is currently no way to deallocate space.
     *
     * @param numBytes Number of bytes requested.
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
     * Determine whether the current memory configuration has a maximum address that can be stored
     * in 16 bits.
     *
     * @return true if maximum address can be stored in 16 bits or less, false otherwise.
     */
    public boolean isUsing16BitAddressSpace() {
        return Integer.compareUnsigned(this.getAddress(MemoryConfigurations.MAPPED_HIGH), 0xFFFF) <= 0;
    }

    /**
     * Handy little utility to find out if given address is in MARS text
     * segment (starts at Memory.textBaseAddress).
     * Note that MARS does not implement the entire MIPS text segment space,
     * but it does implement enough for hundreds of thousands of lines
     * of code.
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined text segment,
     *     false otherwise.
     */
    public boolean isInTextSegment(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.TEXT_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.TEXT_HIGH)) <= 0;
    }

    /**
     * Handy little utility to find out if given address is in MARS kernel
     * text segment (starts at Memory.kernelTextBaseAddress).
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined kernel text segment,
     *     false otherwise.
     */
    public boolean isInKernelTextSegment(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.KERNEL_TEXT_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.KERNEL_TEXT_HIGH)) <= 0;
    }

    /**
     * Handy little utility to find out if given address is in MARS data
     * segment (starts at Memory.dataSegmentBaseAddress).
     * Note that MARS does not implement the entire MIPS data segment space,
     * but it does support at least 4MB.
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined data segment,
     *     false otherwise.
     */
    public boolean isInDataSegment(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.DATA_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.DATA_HIGH)) <= 0;
    }

    /**
     * Handy little utility to find out if given address is in MARS kernel data
     * segment (starts at Memory.kernelDataSegmentBaseAddress).
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined kernel data segment,
     *     false otherwise.
     */
    public boolean isInKernelDataSegment(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.KERNEL_DATA_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.KERNEL_DATA_HIGH)) <= 0;
    }

    /**
     * Handy little utility to find out if given address is in the Memory Map area
     * starts at Memory.memoryMapBaseAddress, range 0xffff0000 to 0xffffffff.
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined memory map (MMIO) area,
     *     false otherwise.
     */
    public boolean isInMemoryMappedIO(int address) {
        return Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.MMIO_LOW)) >= 0
            && Integer.compareUnsigned(address, this.getAddress(MemoryConfigurations.MMIO_HIGH)) <= 0;
    }

    private DataRegion getDataRegionForAddress(int address) {
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

    private TextRegion getTextRegionForAddress(int address) {
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
     * Starting at the given address, write the given value over the given number of bytes.
     * Ignores alignment boundaries, and copies one byte at a time.
     * If <code>length == 1</code>, takes value from low order byte.
     * If <code>length == 2</code>, takes from low order half-word.
     *
     * @param address Starting address where memory will be written.
     * @param value   Value to be stored starting at that address.
     * @param length  Number of bytes to be written.
     */
    public void storeUnaligned(int address, int value, int length) throws AddressErrorException {
        DataRegion dataRegion;
        if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            // Write one byte at a time, w/o regard to alignment boundaries.
            dataRegion.storeBytes(address, length, value);
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        this.dispatchWriteEvent(address, length, value);
    }

    /**
     * Starting at the given word address, write the given value over 4 bytes (a word).
     * It must be written as is, without adjusting for byte order (little vs big endian).
     * Address must be word-aligned.
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address.
     * @param notify  Whether to notify listeners of the write operation.
     * @throws AddressErrorException Thrown if address is not on word boundary.
     */
    public void storeWord(int address, int value, boolean notify) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_EXCEPTION_STORE);
        int oldValue = 0;
        DataRegion dataRegion;
        TextRegion textRegion;
        if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            oldValue = dataRegion.storeWord(address, value);
        }
        else if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            // Burch Mod (Jan 2013): replace throw with call to setStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
                throw new AddressErrorException("cannot write to text segment unless self-modifying code is enabled", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
            }
            ProgramStatement oldStatement = textRegion.storeStatement(address, new ProgramStatement(value, address));
            // TODO: make a separate restore type for program statements in the backstepper
            if (oldStatement != null) {
                oldValue = oldStatement.getBinaryStatement();
            }
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        if (notify) {
            this.dispatchWriteEvent(address, BYTES_PER_WORD, value);
        }
        if (Application.isBackSteppingEnabled()) {
            Application.program.getBackStepper().addMemoryRestoreWord(address, oldValue);
        }
    }

    /**
     * Starting at the given halfword address, write the lower 16 bits of given value
     * into 2 bytes (a halfword).
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address.  Only low order 16 bits used.
     * @throws AddressErrorException Thrown if address is not on halfword boundary.
     */
    public void storeHalfword(int address, int value, boolean notify) throws AddressErrorException {
        value &= 0xFFFF;
        enforceHalfwordAlignment(address, ExceptionCause.ADDRESS_EXCEPTION_STORE);
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
        this.storeWord(wordAddress, wordValue, false);
        if (notify) {
            this.dispatchWriteEvent(address, 1, value, wordAddress, wordValue);
        }
    }

    /**
     * Writes low order 8 bits of given value into specified Memory byte.
     *
     * @param address Address of Memory byte to be set.
     * @param value   Value to be stored at that address.  Only low order 8 bits used.
     */
    public void storeByte(int address, int value, boolean notify) throws AddressErrorException {
        value &= 0xFF;
        int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
        int wordValue = this.fetchWord(wordAddress, false);
        wordValue = Binary.setByte(wordValue, switch (this.endianness) {
            case BIG_ENDIAN -> wordAddress - address + (BYTES_PER_WORD - 1);
            case LITTLE_ENDIAN -> address - wordAddress;
        }, value);
        this.storeWord(wordAddress, wordValue, false);
        if (notify) {
            this.dispatchWriteEvent(address, 1, value, wordAddress, wordValue);
        }
    }

    /**
     * Writes 64 bit double value starting at specified Memory address.  Note that
     * high-order 32 bits are stored in higher (second) memory word regardless
     * of "endianness".
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored at that address.
     * @throws AddressErrorException Thrown if address is not on word boundary.
     */
    public void storeDoubleword(int address, long value) throws AddressErrorException {
        // For some reason, both SPIM and previous versions of MARS enforce only word alignment here,
        // so I'll keep it that way for backwards compatibility.  Sean Clarke 05/2024
        enforceWordAlignment(address, ExceptionCause.ADDRESS_EXCEPTION_STORE);
        switch (this.endianness) {
            case BIG_ENDIAN -> {
                this.storeWord(address, Binary.highOrderLongToInt(value), true);
                this.storeWord(address + BYTES_PER_WORD, Binary.lowOrderLongToInt(value), true);
            }
            case LITTLE_ENDIAN -> {
                this.storeWord(address, Binary.lowOrderLongToInt(value), true);
                this.storeWord(address + BYTES_PER_WORD, Binary.highOrderLongToInt(value), true);
            }
        }
    }

    /**
     * Stores ProgramStatement in Text Segment.
     *
     * @param address   Starting address of Memory address to be set.  Must be word boundary.
     * @param statement Machine code to be stored starting at that address -- for simulation
     *                  purposes, actually stores reference to ProgramStatement instead of 32-bit machine code.
     * @throws AddressErrorException If address is not on word boundary or is outside Text Segment.
     */
    public void storeStatement(int address, ProgramStatement statement, boolean notify) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_EXCEPTION_STORE);
        int binaryStatement = statement.getBinaryStatement();
        TextRegion textRegion;
        DataRegion dataRegion;
        if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            textRegion.storeStatement(address, statement);
        }
        else if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
                throw new AddressErrorException("cannot store code beyond text segment unless self-modifying code is enabled", ExceptionCause.ADDRESS_EXCEPTION_FETCH, address);
            }
            dataRegion.storeWord(address, binaryStatement);
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_FETCH, address);
        }
        if (notify) {
            this.dispatchWriteEvent(address, BYTES_PER_WORD, binaryStatement, address, binaryStatement);
        }
    }

    /**
     * Starting at the given word address, read the given number of bytes (max 4).
     * This one does not check for word boundaries, and copies one byte at a time.
     * If length == 1, puts value in low order byte.  If 2, puts into low order half-word.
     *
     * @param address Starting address of Memory address to be read.
     * @param length  Number of bytes to be read.
     * @return Value stored starting at that address.
     */
    public int fetchUnaligned(int address, int length, boolean notify) throws AddressErrorException {
        int value;
        DataRegion dataRegion;
        if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            // Read one byte at a time, w/o regard to alignment boundaries.
            value = dataRegion.fetchBytes(address, length);
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_FETCH, address);
        }
        if (notify) {
            this.dispatchReadEvent(address, length, value);
        }
        return value;
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int without notifying listeners.
     * It transfers the 32 bit value "raw" as stored in memory, and does not adjust
     * for byte order (big or little endian).  Address must be word-aligned.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int fetchWord(int address, boolean notify) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_EXCEPTION_FETCH);
        int value;
        DataRegion dataRegion;
        TextRegion textRegion;
        if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            value = dataRegion.fetchWord(address);
        }
        else if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify & getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            ProgramStatement statement = textRegion.fetchStatement(address);
            value = statement == null ? 0 : statement.getBinaryStatement();
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_FETCH, address);
        }
        if (notify) {
            this.dispatchReadEvent(address, BYTES_PER_WORD, value, address, value);
        }
        return value;
    }

    /**
     * Starting at the given word address, read a 4 byte word as an <code>Integer</code> without notifying listeners.
     * It transfers the 32 bit value "raw" as stored in memory, and does not adjust
     * for byte order (big or little endian).  Address must be word-aligned.
     * <p>
     * Returns null if reading from text segment and there is no instruction at the
     * requested address. Returns null if reading from data segment and this is the
     * first reference to the MARS 4K memory allocation block (i.e., an array to
     * hold the memory has not been allocated).
     * <p>
     * This method was developed by Greg Giberling of UC Berkeley to support the memory
     * dump feature that he implemented in Fall 2007.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address as an Integer.  Conditions
     *     that cause return value null are described above.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public Integer fetchWordOrNull(int address) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_EXCEPTION_FETCH);
        DataRegion dataRegion;
        TextRegion textRegion;
        if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            return dataRegion.fetchWordOrNull(address);
        }
        else if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify & getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            ProgramStatement statement = textRegion.fetchStatement(address);
            return statement == null ? null : statement.getBinaryStatement();
        }
        else {
            // Falls outside mapped addressing range
            return null;
        }
    }

    /**
     * Look for first "null" memory value in an address range.  For text segment (binary code), this
     * represents a word that does not contain an instruction.  Normally use this to find the end of
     * the program.  For data segment, this represents the first block of simulated memory (block length
     * currently 4K words) that has not been referenced by an assembled/executing program.
     *
     * @param firstAddress First address to be searched; the starting point.
     * @param lastAddress Last address to be searched.
     * @return Lowest address within specified range that contains "null" value as described above.
     * @throws AddressErrorException if the base address is not on a word boundary.
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
     * Starting at the given word address, read a 2 byte word into lower 16 bits of int.
     *
     * @param address Starting address of word to be read.
     * @return Halfword (2-byte value) stored starting at that address, zero-extended to 32 bits.
     * @throws AddressErrorException If address is not on halfword boundary.
     */
    public int fetchHalfword(int address, boolean notify) throws AddressErrorException {
        enforceHalfwordAlignment(address, ExceptionCause.ADDRESS_EXCEPTION_FETCH);
        int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
        int wordValue = this.fetchWord(wordAddress, false);
        int value;
        if ((this.endianness == Endianness.BIG_ENDIAN) == (address == wordAddress)) {
            // Read from high-order halfword
            value = wordValue >>> 16;
        }
        else {
            // Read from low-order halfword
            value = wordValue & 0xFFFF;
        }
        if (notify) {
            this.dispatchReadEvent(address, BYTES_PER_HALFWORD, value, wordAddress, wordValue);
        }
        return value;
    }

    /**
     * Reads specified Memory byte into low order 8 bits of int.
     *
     * @param address Address of Memory byte to be read.
     * @return Value stored at that address.  Only low order 8 bits used.
     */
    public int fetchByte(int address, boolean notify) throws AddressErrorException {
        int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
        int wordValue = this.fetchWord(wordAddress, false);
        int value = Binary.getByte(wordValue, switch (this.endianness) {
            case BIG_ENDIAN -> wordAddress - address + (BYTES_PER_WORD - 1);
            case LITTLE_ENDIAN -> address - wordAddress;
        });
        if (notify) {
            this.dispatchReadEvent(address, 1, value, wordAddress, wordValue);
        }
        return value;
    }

    public long fetchDoubleword(int address, boolean notify) throws AddressErrorException {
        // For some reason, both SPIM and previous versions of MARS enforce only word alignment here,
        // so I'll keep it that way for backwards compatibility.  Sean Clarke 05/2024
        enforceWordAlignment(address, ExceptionCause.ADDRESS_EXCEPTION_FETCH);
        int firstWord = this.fetchWord(address, notify);
        int secondWord = this.fetchWord(address + BYTES_PER_WORD, notify);
        return switch (this.endianness) {
            case BIG_ENDIAN -> Binary.twoIntsToLong(firstWord, secondWord);
            case LITTLE_ENDIAN -> Binary.twoIntsToLong(secondWord, firstWord);
        };
    }

    /**
     * Gets ProgramStatement from Text Segment.
     *
     * @param address Starting address of Memory address to be read.  Must be word boundary.
     * @return Reference to ProgramStatement object associated with that address, or null if none.
     * @throws AddressErrorException If address is not on word boundary or is outside Text Segment.
     * @see ProgramStatement
     */
    public ProgramStatement fetchStatement(int address, boolean notify) throws AddressErrorException {
        enforceWordAlignment(address, ExceptionCause.ADDRESS_EXCEPTION_FETCH);
        ProgramStatement statement;
        TextRegion textRegion;
        DataRegion dataRegion;
        if ((textRegion = this.getTextRegionForAddress(address)) != null) {
            statement = textRegion.fetchStatement(address);
        }
        else if ((dataRegion = this.getDataRegionForAddress(address)) != null) {
            if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
                throw new AddressErrorException("cannot execute beyond text segment unless self-modifying code is enabled", ExceptionCause.ADDRESS_EXCEPTION_FETCH, address);
            }
            Integer binaryStatement = dataRegion.fetchWordOrNull(address);
            statement = (binaryStatement == null) ? null : new ProgramStatement(binaryStatement, address);
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_FETCH, address);
        }
        if (notify) {
            int binaryStatement = (statement == null) ? 0 : statement.getBinaryStatement();
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

        char ch = (char) this.fetchByte(address, false);
        while (ch != 0) {
            content.append(ch);
            ch = (char) this.fetchByte(++address, false);
        }

        return content.toString();
    }

    // The data segment is allocated in blocks of 1024 ints (4096 bytes).  Each block is
    // referenced by a "block table" entry, and the table has 1024 entries.  The capacity
    // is thus 1024 entries * 4096 bytes = 4 MB.  Should be enough to cover most
    // programs!!  Beyond that it would go to an "indirect" block (similar to Unix i-nodes),
    // which is not implemented.
    //
    // Although this scheme is an array of arrays, it is relatively space-efficient since
    // only the table is created initially. A 4096-byte block is not allocated until a value
    // is written to an address within it.  Thus most small programs will use only 8K bytes
    // of space (the table plus one block).  The index into both arrays is easily computed
    // from the address; access time is constant.
    //
    // SPIM stores statically allocated data (following first .data directive) starting
    // at location 0x10010000.  This is the first Data Segment word beyond the reach of $gp
    // used in conjunction with signed 16 bit immediate offset.  $gp has value 0x10008000
    // and with the signed 16 bit offset can reach from 0x10008000 - 0xFFFF = 0x10000000
    // (Data Segment base) to 0x10008000 + 0x7FFF = 0x1000FFFF (the byte preceding 0x10010000).
    //
    // Using my scheme, 0x10010000 falls at the beginning of the 17th block -- table entry 16.
    // SPIM uses a heap base address of 0x10040000 which is not part of the MIPS specification.
    // (I don't have a reference for that offhand...)  Using my scheme, 0x10040000 falls at
    // the start of the 65th block -- table entry 64.  That leaves (1024-64) * 4096 = 3,932,160
    // bytes of space available without going indirect.
    private class DataRegion {
        private static final int WORDS_PER_BLOCK = 1024; // allocated blocksize 1024 words == 4K bytes
        private static final int BLOCKS_PER_TABLE = 1024; // Each entry of table points to a block
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

        public DataRegion(int firstAddress, int lastAddress) {
            this.baseAddress = alignToPrevious(firstAddress, BYTES_PER_TABLE);
            int tableCount = (lastAddress - this.baseAddress) / BYTES_PER_TABLE + 1;
            this.tables = new int[tableCount][][];
        }

        /**
         * Helper method to store 1, 2 or 4 byte value in table that represents MIPS
         * memory.
         * <p>
         * Modified 29 Dec 2005 to return old value of replaced bytes.
         */
        public int storeBytes(int address, int length, int value) {
            return this.storeOrFetchBytes(address, length, value, true);
        }

        /**
         * Helper method to fetch 1, 2 or 4 byte value from table that represents MIPS
         * memory.
         */
        public int fetchBytes(int address, int length) {
            return this.storeOrFetchBytes(address, length, 0, false);
        }

        /**
         * The helper's helper.  Works for either storing or fetching, little or big endian.
         * When storing/fetching bytes, most of the work is calculating the correct array element(s)
         * and element byte(s).  This method performs either store or fetch, as directed by its
         * client using STORE or FETCH in last arg.
         * <p>
         * Modified 29 Dec 2005 to return old value of replaced bytes, for STORE.
         */
        private synchronized int storeOrFetchBytes(int address, int length, int value, boolean doStore) {
            int oldValue = 0; // for STORE, return old values of replaced bytes

            int byteOffset = address - this.baseAddress;
            for (int bytePositionInValue = BYTES_PER_WORD - 1; bytePositionInValue >= BYTES_PER_WORD - length; bytePositionInValue--) {
                int bytePositionInMemory = byteOffset % BYTES_PER_WORD;
                if (Memory.this.endianness == Endianness.BIG_ENDIAN) {
                    bytePositionInMemory = (BYTES_PER_WORD - 1) - bytePositionInMemory;
                }

                int wordOffset = byteOffset >>> 2;
                int wordIndex = getWordIndex(wordOffset);
                int blockIndex = getBlockIndex(wordOffset);
                int tableIndex = getTableIndex(wordOffset);

                if (this.tables[tableIndex] == null) {
                    if (doStore) {
                        this.tables[tableIndex] = new int[BLOCKS_PER_TABLE][];
                    }
                    else {
                        byteOffset++;
                        continue;
                    }
                }

                if (this.tables[tableIndex][blockIndex] == null) {
                    if (doStore) {
                        this.tables[tableIndex][blockIndex] = new int[WORDS_PER_BLOCK];
                    }
                    else {
                        byteOffset++;
                        continue;
                    }
                }

                int[] block = this.tables[tableIndex][blockIndex];
                if (doStore) {
                    oldValue = replaceByte(block[wordIndex], bytePositionInMemory, oldValue, bytePositionInValue);
                    block[wordIndex] = replaceByte(value, bytePositionInValue, block[wordIndex], bytePositionInMemory);
                }
                else {
                    value = replaceByte(block[wordIndex], bytePositionInMemory, value, bytePositionInValue);
                }

                byteOffset++;
            }

            return (doStore) ? oldValue : value;
        }

        /**
         * Helper method to store 4 byte value in table that represents MIPS memory.
         * Assumes address is word aligned, no endian processing.
         * <p>
         * Modified 29 Dec 2005 to return overwritten value.
         */
        public synchronized int storeWord(int address, int value) {
            int wordOffset = (address - this.baseAddress) >>> 2;
            int wordIndex = getWordIndex(wordOffset);
            int blockIndex = getBlockIndex(wordOffset);
            int tableIndex = getTableIndex(wordOffset);

            if (this.tables[tableIndex] == null) {
                this.tables[tableIndex] = new int[BLOCKS_PER_TABLE][];
            }
            if (this.tables[tableIndex][blockIndex] == null) {
                this.tables[tableIndex][blockIndex] = new int[WORDS_PER_BLOCK];
            }

            int oldValue = this.tables[tableIndex][blockIndex][wordIndex];
            this.tables[tableIndex][blockIndex][wordIndex] = value;
            return oldValue;
        }

        /**
         * Helper method to fetch 4 byte value from table that represents MIPS memory.
         * Assumes word alignment, no endian processing.
         */
        public synchronized int fetchWord(int address) {
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
         * Helper method to fetch 4 byte value from table that represents MIPS memory.
         * Originally used just for data segment, but now also used for stack.
         * Both use different tables but same storage method and same table size
         * and block size.  Assumes word alignment, no endian processing.
         * <p>
         * This differs from "fetchWordFromTable()" in that it returns an Integer and
         * returns null instead of 0 if the 4K table has not been allocated.  Developed
         * by Greg Gibeling of UC Berkeley, fall 2007.
         */
        public synchronized Integer fetchWordOrNull(int address) {
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

    // I use a similar scheme for storing instructions.  MIPS text segment ranges from
    // 0x00400000 all the way to data segment (0x10000000) a range of about 250 MB!  So
    // I'll provide table of blocks with similar capacity.  This differs from data segment
    // somewhat in that the block entries do not contain int's, but instead contain
    // references to ProgramStatement objects.
    private class TextRegion {
        private static final int WORDS_PER_BLOCK = 1024; // allocated blocksize 1024 words == 4K bytes (likely 8K bytes in reality)
        private static final int BLOCKS_PER_TABLE = 1024; // Each entry of table points to a block
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

        private final ProgramStatement[][][] tables;
        private final int baseAddress;

        public TextRegion(int firstAddress, int lastAddress) {
            this.baseAddress = alignToPrevious(firstAddress, BYTES_PER_TABLE);
            int tableCount = (lastAddress - this.baseAddress) / BYTES_PER_TABLE + 1;
            this.tables = new ProgramStatement[tableCount][][];
        }

        /**
         * Store a program statement at the given address.  Address has already been verified
         * as valid.  It may be either in user or kernel text segment, as specified by arguments.
         */
        public synchronized ProgramStatement storeStatement(int address, ProgramStatement statement) {
            int wordOffset = (address - this.baseAddress) >>> 2;
            int wordIndex = getWordIndex(wordOffset);
            int blockIndex = getBlockIndex(wordOffset);
            int tableIndex = getTableIndex(wordOffset);

            if (this.tables[tableIndex] == null) {
                this.tables[tableIndex] = new ProgramStatement[BLOCKS_PER_TABLE][];
            }
            if (this.tables[tableIndex][blockIndex] == null) {
                this.tables[tableIndex][blockIndex] = new ProgramStatement[WORDS_PER_BLOCK];
            }

            ProgramStatement oldStatement = this.tables[tableIndex][blockIndex][wordIndex];
            this.tables[tableIndex][blockIndex][wordIndex] = statement;
            return oldStatement;
        }

        /**
         * Read a program statement from the given address.  Address has already been verified
         * as valid.  It may be either in user or kernel text segment, as specified by arguments.
         * Returns associated ProgramStatement or null if none.
         */
        public synchronized ProgramStatement fetchStatement(int address) {
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
     * Returns result of substituting specified byte of source value into specified byte
     * of destination value. Byte positions are 0-1-2-3, listed from most to least
     * significant.  No endian issues.  This is a private helper method used by get() & set().
     */
    private static int replaceByte(int sourceValue, int bytePosInSource, int destValue, int bytePosInDest) {
        return
            // Set source byte value into destination byte position; set other 24 bits to 0's...
            ((sourceValue >>> ((3 - bytePosInSource) << 3) & 0xFF) << ((3 - bytePosInDest) << 3))
                // and bitwise-OR it with...
                |
                // Set 8 bits in destination byte position to 0's, other 24 bits are unchanged.
                (destValue & ~(0xFF << ((3 - bytePosInDest) << 3)));
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
        synchronized (this.listenerRanges) {
            return this.listenerRanges.stream()
                .filter(range -> range.contains(address, length))
                .toList();
        }
    }

    private void dispatchReadEvent(int address, int length, int value) {
        try {
            int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
            int wordValue = this.fetchWord(wordAddress, false);
            this.dispatchReadEvent(address, length, value, wordAddress, wordValue);
        }
        catch (AddressErrorException exception) {
            // This should not happen since wordAddress is word-aligned
        }
    }

    private void dispatchReadEvent(int address, int length, int value, int wordAddress, int wordValue) {
        if (Application.program != null || Application.getGUI() == null) {
            for (ListenerRange range : this.getListeners(address, length)) {
                range.listener.memoryRead(address, length, value, wordAddress, wordValue);
            }
        }
    }

    private void dispatchWriteEvent(int address, int length, int value) {
        try {
            int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
            int wordValue = this.fetchWord(wordAddress, false);
            this.dispatchWriteEvent(address, length, value, wordAddress, wordValue);
        }
        catch (AddressErrorException exception) {
            // This should not happen since wordAddress is word-aligned
        }
    }

    private void dispatchWriteEvent(int address, int length, int value, int wordAddress, int wordValue) {
        if (Application.program != null || Application.getGUI() == null) {
            for (ListenerRange range : this.getListeners(address, length)) {
                range.listener.memoryWritten(address, length, value, wordAddress, wordValue);
            }
        }
    }
}
