package mars.mips.hardware;

import mars.Application;
import mars.ProgramStatement;
import mars.mips.instructions.Instruction;
import mars.simulator.ExceptionCause;
import mars.util.Binary;

import java.nio.ByteOrder;
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
// NOTE: This implementation is purely big-endian.  MIPS can handle either one.
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
     * Current setting for endianness (default is {@link ByteOrder#LITTLE_ENDIAN}).
     */
    private ByteOrder endianness = ByteOrder.LITTLE_ENDIAN;
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

    private static final int DATA_WORDS_PER_BLOCK = 1024; // allocated blocksize 1024 words == 4K bytes
    private static final int DATA_BLOCKS_PER_TABLE = 1024; // Each entry of table points to a block
    private static final int DATA_BYTES_PER_TABLE = BYTES_PER_WORD * DATA_WORDS_PER_BLOCK * DATA_BLOCKS_PER_TABLE;

    private int dataSegmentBaseAddress;
    private int[][][] dataSegmentTables;
    private int kernelDataSegmentBaseAddress;
    private int[][][] kernelDataSegmentTables;
    private int mmioBaseAddress;
    private int[][][] mmioTables;

    // I use a similar scheme for storing instructions.  MIPS text segment ranges from
    // 0x00400000 all the way to data segment (0x10000000) a range of about 250 MB!  So
    // I'll provide table of blocks with similar capacity.  This differs from data segment
    // somewhat in that the block entries do not contain int's, but instead contain
    // references to ProgramStatement objects.

    private static final int TEXT_WORDS_PER_BLOCK = 1024; // allocated blocksize 1024 words == 4K bytes (likely 8K bytes in reality)
    private static final int TEXT_BLOCKS_PER_TABLE = 1024; // Each entry of table points to a block
    private static final int TEXT_BYTES_PER_TABLE = BYTES_PER_WORD * TEXT_WORDS_PER_BLOCK * TEXT_BLOCKS_PER_TABLE;

    private int textSegmentBaseAddress;
    private ProgramStatement[][][] textSegmentTables;
    private int kernelTextSegmentBaseAddress;
    private ProgramStatement[][][] kernelTextSegmentTables;

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

    public int getAddress(int key) {
        return this.configuration.getAddress(key);
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
     * Initialize the memory tables and other instance variables.
     */
    public void reset() {
        this.configuration = MemoryConfigurations.getCurrentConfiguration();
        this.nextHeapAddress = alignToNext(this.getAddress(MemoryConfigurations.DYNAMIC_LOW), BYTES_PER_WORD);

        this.dataSegmentBaseAddress = alignToPrevious(this.getAddress(MemoryConfigurations.DATA_LOW), DATA_BYTES_PER_TABLE);
        int dataSegmentTableCount = (this.getAddress(MemoryConfigurations.DATA_HIGH) - this.dataSegmentBaseAddress) / DATA_BYTES_PER_TABLE + 1;
        this.dataSegmentTables = new int[dataSegmentTableCount][][];

        this.kernelDataSegmentBaseAddress = alignToPrevious(this.getAddress(MemoryConfigurations.KERNEL_DATA_LOW), DATA_BYTES_PER_TABLE);
        int kernelDataSegmentTableCount = (this.getAddress(MemoryConfigurations.KERNEL_DATA_HIGH) - this.kernelDataSegmentBaseAddress) / DATA_BYTES_PER_TABLE + 1;
        this.kernelDataSegmentTables = new int[kernelDataSegmentTableCount][][];

        this.mmioBaseAddress = alignToPrevious(this.getAddress(MemoryConfigurations.MMIO_LOW), DATA_BYTES_PER_TABLE);
        int mmioTableCount = (this.getAddress(MemoryConfigurations.MMIO_HIGH) - this.mmioBaseAddress) / DATA_BYTES_PER_TABLE + 1;
        this.mmioTables = new int[mmioTableCount][][];

        this.textSegmentBaseAddress = alignToPrevious(this.getAddress(MemoryConfigurations.TEXT_LOW), TEXT_BYTES_PER_TABLE);
        int textSegmentTableCount = (this.getAddress(MemoryConfigurations.TEXT_HIGH) - this.textSegmentBaseAddress) / TEXT_BYTES_PER_TABLE + 1;
        this.textSegmentTables = new ProgramStatement[textSegmentTableCount][][];

        this.kernelTextSegmentBaseAddress = alignToPrevious(this.getAddress(MemoryConfigurations.KERNEL_TEXT_LOW), TEXT_BYTES_PER_TABLE);
        int kernelTextSegmentTableCount = (this.getAddress(MemoryConfigurations.KERNEL_TEXT_HIGH) - this.kernelTextSegmentBaseAddress) / TEXT_BYTES_PER_TABLE + 1;
        this.kernelTextSegmentTables = new ProgramStatement[kernelTextSegmentTableCount][][];

        // Encourage the garbage collector to clean up any arrays we just orphaned
        System.gc();
    }

    /**
     * Get the current endianness (i.e. byte ordering) in use.
     * Unless {@link #setEndianness} is called, the default setting is {@link ByteOrder#LITTLE_ENDIAN}.
     *
     * @return Either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
     */
    public ByteOrder getEndianness() {
        return this.endianness;
    }

    /**
     * Set the endianness (i.e. byte ordering) for memory to use.
     * Before this method is called, the default setting is {@link ByteOrder#LITTLE_ENDIAN}.
     *
     * @param endianness Either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
     */
    public void setEndianness(ByteOrder endianness) {
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
        if (newHeapAddress > this.getAddress(MemoryConfigurations.DYNAMIC_HIGH)) {
            throw new IllegalArgumentException("heap allocation of " + numBytes + " bytes failed due to insufficient heap space");
        }
        this.nextHeapAddress = newHeapAddress;
        return result;
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
     * @return Old value that was replaced by the write operation.
     */
    public int set(int address, int value, int length) throws AddressErrorException {
        int oldValue = 0;
        int relativeByteAddress;
        if (this.isInDataSegment(address)) {
            // In data segment.  Will write one byte at a time, w/o regard to alignment boundaries.
            relativeByteAddress = address - this.dataSegmentBaseAddress;
            oldValue = this.storeBytesInTable(this.dataSegmentTables, relativeByteAddress, length, value);
        }
        else if (this.isInKernelDataSegment(address)) {
            // In kernel data segment.  Will write one byte at a time, w/o regard to alignment boundaries.
            relativeByteAddress = address - this.kernelDataSegmentBaseAddress;
            oldValue = this.storeBytesInTable(this.kernelDataSegmentTables, relativeByteAddress, length, value);
        }
        else if (this.isInMemoryMappedIO(address)) {
            // In memory-mapped I/O.  Will write one byte at a time, w/o regard to alignment boundaries.
            relativeByteAddress = address - this.mmioBaseAddress;
            oldValue = this.storeBytesInTable(this.mmioTables, relativeByteAddress, length, value);
        }
        else if (this.isInTextSegment(address) || this.isInKernelTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with call to setStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
                throw new AddressErrorException("cannot write to text segment unless self-modifying code is enabled", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
            }
            ProgramStatement oldStatement = this.setStatement(address, new ProgramStatement(value, address));
            if (oldStatement != null) {
                oldValue = oldStatement.getBinaryStatement();
            }
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        this.dispatchWriteEvent(address, length, value);
        if (Application.debug) {
            System.out.println("memory[" + address + "] set to " + value + "(" + length + " bytes)");
        }
        return oldValue;
    }

    /**
     * Starting at the given word address, write the given value over 4 bytes (a word).
     * It must be written as is, without adjusting for byte order (little vs big endian).
     * Address must be word-aligned.
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address.
     * @throws AddressErrorException Thrown if address is not on word boundary.
     */
    public void setRawWord(int address, int value) throws AddressErrorException {
        if (!isWordAligned(address)) {
            throw new AddressErrorException("address not aligned on word boundary", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        int oldValue = 0;
        int relative;
        if (this.isInDataSegment(address)) {
            relative = (address - this.dataSegmentBaseAddress) >> 2; // Convert byte address to words
            oldValue = this.storeWordInTable(this.dataSegmentTables, relative, value);
        }
        else if (this.isInKernelDataSegment(address)) {
            relative = (address - this.kernelDataSegmentBaseAddress) >> 2; // Convert byte address to words
            oldValue = this.storeWordInTable(this.kernelDataSegmentTables, relative, value);
        }
        else if (this.isInMemoryMappedIO(address)) {
            relative = (address - this.mmioBaseAddress) >> 2; // Convert byte address to words
            oldValue = this.storeWordInTable(this.mmioTables, relative, value);
        }
        else if (this.isInTextSegment(address) | this.isInKernelTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with call to setStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
                throw new AddressErrorException("cannot write to text segment unless self-modifying code is enabled", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
            }
            ProgramStatement oldStatement = this.setStatement(address, new ProgramStatement(value, address));
            if (oldStatement != null) {
                oldValue = oldStatement.getBinaryStatement();
            }
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        this.dispatchWriteEvent(address, BYTES_PER_WORD, value);
        if (Application.isBackSteppingEnabled()) {
            Application.program.getBackStepper().addMemoryRestoreRawWord(address, oldValue);
        }
    }

    /**
     * Starting at the given word address, write the given value over 4 bytes (a word).
     * The address must be word-aligned.
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address.
     * @throws AddressErrorException Thrown if address is not on word boundary.
     */
    public void setWord(int address, int value) throws AddressErrorException {
        this.setRawWord(address, (this.endianness == ByteOrder.LITTLE_ENDIAN) ? Integer.reverseBytes(value) : value);
    }

    /**
     * Starting at the given halfword address, write the lower 16 bits of given value
     * into 2 bytes (a halfword).
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address.  Only low order 16 bits used.
     * @throws AddressErrorException Thrown if address is not on halfword boundary.
     */
    public void setHalfword(int address, int value) throws AddressErrorException {
        if (!isHalfwordAligned(address)) {
            throw new AddressErrorException("store address not aligned on halfword boundary", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        int oldValue = this.set(address, value, BYTES_PER_HALFWORD);
        if (Application.isBackSteppingEnabled()) {
            Application.program.getBackStepper().addMemoryRestoreHalf(address, oldValue);
        }
    }

    /**
     * Writes low order 8 bits of given value into specified Memory byte.
     *
     * @param address Address of Memory byte to be set.
     * @param value   Value to be stored at that address.  Only low order 8 bits used.
     */
    public void setByte(int address, int value) throws AddressErrorException {
        int oldValue = this.set(address, value, 1);
        if (Application.isBackSteppingEnabled()) {
            Application.program.getBackStepper().addMemoryRestoreByte(address, oldValue);
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
    public void setDoubleword(int address, double value) throws AddressErrorException {
        long longValue = Double.doubleToLongBits(value);
        this.setWord(address + BYTES_PER_WORD, Binary.highOrderLongToInt(longValue));
        this.setWord(address, Binary.lowOrderLongToInt(longValue));
    }

    /**
     * Stores ProgramStatement in Text Segment.
     *
     * @param address   Starting address of Memory address to be set.  Must be word boundary.
     * @param statement Machine code to be stored starting at that address -- for simulation
     *                  purposes, actually stores reference to ProgramStatement instead of 32-bit machine code.
     * @return The previous statement stored at this address.
     * @throws AddressErrorException If address is not on word boundary or is outside Text Segment.
     */
    public ProgramStatement setStatement(int address, ProgramStatement statement) throws AddressErrorException {
        if (!isWordAligned(address)) {
            throw new AddressErrorException("store address not aligned on word boundary", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        ProgramStatement oldValue;
        if (this.isInTextSegment(address)) {
            oldValue = this.storeProgramStatement(address, statement, this.textSegmentBaseAddress, this.textSegmentTables);
        }
        else if (this.isInKernelTextSegment(address)) {
            oldValue = this.storeProgramStatement(address, statement, this.kernelTextSegmentBaseAddress, this.kernelTextSegmentTables);
        }
        else {
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        if (Application.debug) {
            System.out.println("memory[" + address + "] set to " + statement.getBinaryStatement());
        }
        return oldValue;
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
    public int getUnaligned(int address, int length) throws AddressErrorException {
        return this.getUnaligned(address, length, true);
    }

    // Does the real work, but includes option to NOT notify observers.
    private int getUnaligned(int address, int length, boolean notify) throws AddressErrorException {
        int value;
        int relativeByteAddress;
        if (this.isInDataSegment(address)) {
            // In data segment.  Will read one byte at a time, w/o regard to alignment boundaries.
            relativeByteAddress = address - this.dataSegmentBaseAddress;
            value = this.fetchBytesFromTable(this.dataSegmentTables, relativeByteAddress, length);
        }
        else if (this.isInKernelDataSegment(address)) {
            // In kernel data segment.  Will read one byte at a time, w/o regard to alignment boundaries.
            relativeByteAddress = address - this.kernelDataSegmentBaseAddress;
            value = this.fetchBytesFromTable(this.kernelDataSegmentTables, relativeByteAddress, length);
        }
        else if (this.isInMemoryMappedIO(address)) {
            // In memory-mapped I/O.  Will read one byte at a time, w/o regard to alignment boundaries.
            relativeByteAddress = address - this.mmioBaseAddress;
            value = this.fetchBytesFromTable(this.mmioTables, relativeByteAddress, length);
        }
        else if (this.isInTextSegment(address) || this.isInKernelTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify & getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            ProgramStatement statement = this.getStatementNoNotify(address);
            value = statement == null ? 0 : statement.getBinaryStatement();
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
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
    // Note: the logic here is repeated in getRawWordOrNull() below.  Logic is
    // simplified by having this method just call getRawWordOrNull() then 
    // return either the int of its return value, or 0 if it returns null.
    // Doing so would be detrimental to simulation runtime performance, so
    // I decided to keep the duplicate logic.
    public int getRawWord(int address) throws AddressErrorException {
        if (!isWordAligned(address)) {
            throw new AddressErrorException("address for fetch not aligned on word boundary", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        int value;
        int relative;
        if (this.isInDataSegment(address)) {
            relative = (address - this.dataSegmentBaseAddress) >> 2; // Convert byte address to words
            value = this.fetchWordFromTable(this.dataSegmentTables, relative);
        }
        else if (this.isInKernelDataSegment(address)) {
            relative = (address - this.kernelDataSegmentBaseAddress) >> 2; // Convert byte address to words
            value = this.fetchWordFromTable(this.kernelDataSegmentTables, relative);
        }
        else if (this.isInMemoryMappedIO(address)) {
            relative = (address - this.mmioBaseAddress) >> 2; // Convert byte address to words
            value = this.fetchWordFromTable(this.mmioTables, relative);
        }
        else if (this.isInTextSegment(address) || this.isInKernelTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify & getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            ProgramStatement statement = this.getStatementNoNotify(address);
            value = statement == null ? 0 : statement.getBinaryStatement();
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
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
    // See note above, with getRawWord(), concerning duplicated logic.
    public Integer getRawWordOrNull(int address) throws AddressErrorException {
        if (!isWordAligned(address)) {
            throw new AddressErrorException("address for fetch not aligned on word boundary", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        Integer value = null;
        int relative;
        if (this.isInDataSegment(address)) {
            relative = (address - this.dataSegmentBaseAddress) >> 2; // Convert byte address to words
            value = this.fetchWordOrNullFromTable(this.dataSegmentTables, relative);
        }
        else if (this.isInKernelDataSegment(address)) {
            relative = (address - this.kernelDataSegmentBaseAddress) >> 2; // Convert byte address to words
            value = this.fetchWordOrNullFromTable(this.kernelDataSegmentTables, relative);
        }
        else if (this.isInMemoryMappedIO(address)) {
            relative = (address - this.mmioBaseAddress) >> 2; // Convert byte address to words
            value = this.fetchWordOrNullFromTable(this.mmioTables, relative);
        }
        else if (this.isInTextSegment(address) || this.isInKernelTextSegment(address)) {
            ProgramStatement statement = this.getStatementNoNotify(address);
            if (statement != null) {
                value = statement.getBinaryStatement();
            }
        }
        else {
            // Falls outside mapped addressing range
            throw new AddressErrorException("segmentation fault (address out of range)", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        // Do not notify listeners.  This read operation is initiated by the
        // dump feature, not the executing MIPS program.
        return value;
    }

    /**
     * Look for first "null" memory value in an address range.  For text segment (binary code), this
     * represents a word that does not contain an instruction.  Normally use this to find the end of
     * the program.  For data segment, this represents the first block of simulated memory (block length
     * currently 4K words) that has not been referenced by an assembled/executing program.
     *
     * @param baseAddress  lowest MIPS address to be searched; the starting point
     * @param limitAddress highest MIPS address to be searched
     * @return lowest address within specified range that contains "null" value as described above.
     * @throws AddressErrorException if the base address is not on a word boundary
     */
    public int getAddressOfFirstNull(int baseAddress, int limitAddress) throws AddressErrorException {
        int address;
        for (address = baseAddress; address < limitAddress; address += BYTES_PER_WORD) {
            if (this.getRawWordOrNull(address) == null) {
                break;
            }
        }
        return address;
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int. Observers are notified.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int getWord(int address) throws AddressErrorException {
        int value = this.getRawWord(address);
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            value = Integer.reverseBytes(value);
        }
        this.dispatchReadEvent(address, BYTES_PER_WORD, value, address, value);
        return value;
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int. Observers are NOT notified.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int getWordNoNotify(int address) throws AddressErrorException {
        int value = this.getRawWord(address);
        return (this.endianness == ByteOrder.LITTLE_ENDIAN) ? Integer.reverseBytes(value) : value;
    }

    /**
     * Starting at the given word address, read a 2 byte word into lower 16 bits of int.
     *
     * @param address Starting address of word to be read.
     * @return Halfword (2-byte value) stored starting at that address, stored in lower 16 bits.
     * @throws AddressErrorException If address is not on halfword boundary.
     */
    public int getHalfword(int address) throws AddressErrorException {
        if (!isHalfwordAligned(address)) {
            throw new AddressErrorException("fetch address not aligned on halfword boundary", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        return this.getUnaligned(address, BYTES_PER_HALFWORD);
    }

    /**
     * Reads specified Memory byte into low order 8 bits of int.
     *
     * @param address Address of Memory byte to be read.
     * @return Value stored at that address.  Only low order 8 bits used.
     */
    public int getByte(int address) throws AddressErrorException {
        return this.getUnaligned(address, 1);
    }

    /**
     * Gets ProgramStatement from Text Segment.
     *
     * @param address Starting address of Memory address to be read.  Must be word boundary.
     * @return reference to ProgramStatement object associated with that address, or null if none.
     * @throws AddressErrorException If address is not on word boundary or is outside Text Segment.
     * @see ProgramStatement
     */
    public ProgramStatement getStatement(int address) throws AddressErrorException {
        return this.getStatement(address, true);
    }

    /**
     * Gets ProgramStatement from Text Segment without notifying observers.
     *
     * @param address Starting address of Memory address to be read.  Must be word boundary.
     * @return reference to ProgramStatement object associated with that address, or null if none.
     * @throws AddressErrorException If address is not on word boundary or is outside Text Segment.
     * @see ProgramStatement
     */
    public ProgramStatement getStatementNoNotify(int address) throws AddressErrorException {
        return this.getStatement(address, false);
    }

    private ProgramStatement getStatement(int address, boolean notify) throws AddressErrorException {
        if (!isWordAligned(address)) {
            throw new AddressErrorException("fetch address for text segment not aligned to word boundary", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        ProgramStatement statement;
        if (this.isInTextSegment(address)) {
            statement = this.fetchProgramStatement(address, this.textSegmentBaseAddress, this.textSegmentTables);
        }
        else if (this.isInKernelTextSegment(address)) {
            statement = this.fetchProgramStatement(address, this.kernelTextSegmentBaseAddress, this.kernelTextSegmentTables);
        }
        else if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
            throw new AddressErrorException("cannot execute data segment unless self-modifying code is enabled", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        else {
            statement = new ProgramStatement(this.getWordNoNotify(address), address);
        }
        if (notify) {
            this.dispatchReadEvent(address, Instruction.BYTES_PER_INSTRUCTION, statement == null ? 0 : statement.getBinaryStatement());
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
    public String getNullTerminatedString(int address) throws AddressErrorException {
        StringBuilder content = new StringBuilder();

        char ch = (char) this.getByte(address);
        while (ch != 0) {
            content.append(ch);
            ch = (char) this.getByte(++address);
        }

        return content.toString();
    }

    /**
     * Utility to determine if given address is word-aligned.
     *
     * @param address the address to check
     * @return true if address is word-aligned, false otherwise
     */
    public static boolean isWordAligned(int address) {
        return (address & (BYTES_PER_WORD - 1)) == 0;
    }

    /**
     * Utility to determine if given address is halfword-aligned.
     *
     * @param address the address to check
     * @return true if address is halfword-aligned, false otherwise
     */
    public static boolean isHalfwordAligned(int address) {
        return (address & (BYTES_PER_HALFWORD - 1)) == 0;
    }

    /**
     * Utility to determine if given address is doubleword-aligned.
     *
     * @param address the address to check
     * @return true if address is doubleword-aligned, false otherwise
     */
    public static boolean isDoublewordAligned(int address) {
        return (address & (BYTES_PER_DOUBLEWORD - 1)) == 0;
    }

    /**
     * Align the given address to the next alignment boundary.
     * If the address is already aligned, it is left unchanged.
     *
     * @param address   The memory address to align.
     * @param alignment The alignment required in bytes.
     * @return The next address divisible by <code>alignment</code>.
     */
    public static int alignToNext(int address, int alignment) {
        int excess = Integer.remainderUnsigned(address, alignment);
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
     * @param alignment The alignment required in bytes.
     * @return The previous address divisible by <code>alignment</code>.
     */
    public static int alignToPrevious(int address, int alignment) {
        return address - Integer.remainderUnsigned(address, alignment);
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
            int wordValue = this.getWordNoNotify(wordAddress);
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
            int wordValue = this.getWordNoNotify(wordAddress);
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

    /**
     * Helper method to store 1, 2 or 4 byte value in table that represents MIPS
     * memory.
     * <p>
     * Modified 29 Dec 2005 to return old value of replaced bytes.
     */
    private int storeBytesInTable(int[][][] tables, int relativeByteAddress, int length, int value) {
        return this.storeOrFetchBytesInTable(tables, relativeByteAddress, length, value, true);
    }

    /**
     * Helper method to fetch 1, 2 or 4 byte value from table that represents MIPS
     * memory.
     */
    private int fetchBytesFromTable(int[][][] tables, int relativeByteAddress, int length) {
        return this.storeOrFetchBytesInTable(tables, relativeByteAddress, length, 0, false);
    }

    /**
     * The helper's helper.  Works for either storing or fetching, little or big endian.
     * When storing/fetching bytes, most of the work is calculating the correct array element(s)
     * and element byte(s).  This method performs either store or fetch, as directed by its
     * client using STORE or FETCH in last arg.
     * <p>
     * Modified 29 Dec 2005 to return old value of replaced bytes, for STORE.
     */
    private synchronized int storeOrFetchBytesInTable(int[][][] tables, int relativeByteAddress, int length, int value, boolean doStore) {
        int oldValue = 0; // for STORE, return old values of replaced bytes

        for (int bytePositionInValue = BYTES_PER_WORD - 1; bytePositionInValue >= BYTES_PER_WORD - length; bytePositionInValue--) {
            int bytePositionInMemory = relativeByteAddress % BYTES_PER_WORD;
            if (this.endianness == ByteOrder.BIG_ENDIAN) {
                bytePositionInMemory = (BYTES_PER_WORD - 1) - bytePositionInMemory;
            }

            int relativeWordAddress = relativeByteAddress >> 2; // Convert byte address to words
            int wordIndex = relativeWordAddress % DATA_WORDS_PER_BLOCK;
            int blockIndex = (relativeWordAddress / DATA_WORDS_PER_BLOCK) % DATA_BLOCKS_PER_TABLE;
            int tableIndex = relativeWordAddress / (DATA_WORDS_PER_BLOCK * DATA_BLOCKS_PER_TABLE);

            if (tables[tableIndex] == null) {
                if (doStore) {
                    tables[tableIndex] = new int[DATA_BLOCKS_PER_TABLE][];
                }
                else {
                    relativeByteAddress++;
                    continue;
                }
            }

            if (tables[tableIndex][blockIndex] == null) {
                if (doStore) {
                    tables[tableIndex][blockIndex] = new int[DATA_WORDS_PER_BLOCK];
                }
                else {
                    relativeByteAddress++;
                    continue;
                }
            }

            int[] block = tables[tableIndex][blockIndex];
            if (doStore) {
                oldValue = replaceByte(block[wordIndex], bytePositionInMemory, oldValue, bytePositionInValue);
                block[wordIndex] = replaceByte(value, bytePositionInValue, block[wordIndex], bytePositionInMemory);
            }
            else {
                value = replaceByte(block[wordIndex], bytePositionInMemory, value, bytePositionInValue);
            }

            relativeByteAddress++;
        }

        return (doStore) ? oldValue : value;
    }

    /**
     * Helper method to store 4 byte value in table that represents MIPS memory.
     * Assumes address is word aligned, no endian processing.
     * <p>
     * Modified 29 Dec 2005 to return overwritten value.
     */
    private synchronized int storeWordInTable(int[][][] tables, int relative, int value) {
        int wordIndex = relative % DATA_WORDS_PER_BLOCK;
        int blockIndex = (relative / DATA_WORDS_PER_BLOCK) % DATA_BLOCKS_PER_TABLE;
        int tableIndex = relative / (DATA_WORDS_PER_BLOCK * DATA_BLOCKS_PER_TABLE);

        if (tables[tableIndex] == null) {
            tables[tableIndex] = new int[DATA_BLOCKS_PER_TABLE][];
        }
        if (tables[tableIndex][blockIndex] == null) {
            tables[tableIndex][blockIndex] = new int[DATA_WORDS_PER_BLOCK];
        }

        int oldValue = tables[tableIndex][blockIndex][wordIndex];
        tables[tableIndex][blockIndex][wordIndex] = value;
        return oldValue;
    }

    /**
     * Helper method to fetch 4 byte value from table that represents MIPS memory.
     * Assumes word alignment, no endian processing.
     */
    private synchronized int fetchWordFromTable(int[][][] tables, int relative) {
        int wordIndex = relative % DATA_WORDS_PER_BLOCK;
        int blockIndex = (relative / DATA_WORDS_PER_BLOCK) % DATA_BLOCKS_PER_TABLE;
        int tableIndex = relative / (DATA_WORDS_PER_BLOCK * DATA_BLOCKS_PER_TABLE);

        if (tables[tableIndex] == null || tables[tableIndex][blockIndex] == null) {
            // The table or block has not been allocated, so assume it is 0 by default
            return 0;
        }
        else {
            return tables[tableIndex][blockIndex][wordIndex];
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
    private synchronized Integer fetchWordOrNullFromTable(int[][][] tables, int relative) {
        int wordIndex = relative % DATA_WORDS_PER_BLOCK;
        int blockIndex = (relative / DATA_WORDS_PER_BLOCK) % DATA_BLOCKS_PER_TABLE;
        int tableIndex = relative / (DATA_WORDS_PER_BLOCK * DATA_BLOCKS_PER_TABLE);

        if (tables[tableIndex] == null || tables[tableIndex][blockIndex] == null) {
            // The table or block has not been allocated, so return null
            return null;
        }
        else {
            return tables[tableIndex][blockIndex][wordIndex];
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
     * Store a program statement at the given address.  Address has already been verified
     * as valid.  It may be either in user or kernel text segment, as specified by arguments.
     */
    private ProgramStatement storeProgramStatement(int address, ProgramStatement statement, int baseAddress, ProgramStatement[][][] tables) {
        int relative = (address - baseAddress) >> 2; // Convert byte address to words
        int wordIndex = relative % TEXT_WORDS_PER_BLOCK;
        int blockIndex = (relative / TEXT_WORDS_PER_BLOCK) % TEXT_BLOCKS_PER_TABLE;
        int tableIndex = relative / (TEXT_WORDS_PER_BLOCK * TEXT_BLOCKS_PER_TABLE);

        if (tables[tableIndex] == null) {
            tables[tableIndex] = new ProgramStatement[DATA_BLOCKS_PER_TABLE][];
        }
        if (tables[tableIndex][blockIndex] == null) {
            tables[tableIndex][blockIndex] = new ProgramStatement[DATA_WORDS_PER_BLOCK];
        }

        ProgramStatement oldStatement = tables[tableIndex][blockIndex][wordIndex];
        tables[tableIndex][blockIndex][wordIndex] = statement;
        return oldStatement;
    }

    /**
     * Read a program statement from the given address.  Address has already been verified
     * as valid.  It may be either in user or kernel text segment, as specified by arguments.
     * Returns associated ProgramStatement or null if none.
     */
    private ProgramStatement fetchProgramStatement(int address, int baseAddress, ProgramStatement[][][] tables) {
        int relative = (address - baseAddress) >> 2; // Convert byte address to words
        int wordIndex = relative % TEXT_WORDS_PER_BLOCK;
        int blockIndex = (relative / TEXT_WORDS_PER_BLOCK) % TEXT_BLOCKS_PER_TABLE;
        int tableIndex = relative / (TEXT_WORDS_PER_BLOCK * TEXT_BLOCKS_PER_TABLE);

        if (tables[tableIndex] == null || tables[tableIndex][blockIndex] == null) {
            // The table or block has not been allocated, so return null
            return null;
        }
        else {
            return tables[tableIndex][blockIndex][wordIndex];
        }
    }
}
