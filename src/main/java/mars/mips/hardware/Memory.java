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
 * Represents MIPS memory.  Different segments are represented by different data structs.
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
     * base address for (user) text segment: 0x00400000
     */
    public static int textBaseAddress = MemoryConfigurations.getDefaultTextBaseAddress(); //0x00400000;
    /**
     * base address for (user) data segment: 0x10000000
     */
    public static int dataSegmentBaseAddress = MemoryConfigurations.getDefaultDataSegmentBaseAddress(); //0x10000000;
    /**
     * base address for .extern directive: 0x10000000
     */
    public static int externBaseAddress = MemoryConfigurations.getDefaultExternBaseAddress(); //0x10000000;
    /**
     * base address for storing globals
     */
    public static int globalPointer = MemoryConfigurations.getDefaultGlobalPointer(); //0x10008000;
    /**
     * base address for storage of non-global static data in data segment: 0x10010000 (from SPIM)
     */
    public static int dataBaseAddress = MemoryConfigurations.getDefaultDataBaseAddress(); //0x10010000; // from SPIM not MIPS
    /**
     * base address for heap: 0x10040000 (I think from SPIM not MIPS)
     */
    public static int heapBaseAddress = MemoryConfigurations.getDefaultHeapBaseAddress(); //0x10040000; // I think from SPIM not MIPS
    /**
     * starting address for stack: 0x7fffeffc (this is from SPIM not MIPS)
     */
    public static int stackPointer = MemoryConfigurations.getDefaultStackPointer(); //0x7fffeffc;
    /**
     * base address for stack: 0x7ffffffc (this is mine - start of highest word below kernel space)
     */
    public static int stackBaseAddress = MemoryConfigurations.getDefaultStackBaseAddress(); //0x7ffffffc;
    /**
     * highest address accessible in user (not kernel) mode.
     */
    public static int userHighAddress = MemoryConfigurations.getDefaultUserHighAddress(); //0x7fffffff;
    /**
     * kernel boundary.  Only OS can access this or higher address
     */
    public static int kernelBaseAddress = MemoryConfigurations.getDefaultKernelBaseAddress(); //0x80000000;
    /**
     * base address for kernel text segment: 0x80000000
     */
    public static int kernelTextBaseAddress = MemoryConfigurations.getDefaultKernelTextBaseAddress(); //0x80000000;
    /**
     * starting address for exception handlers: 0x80000180
     */
    public static int exceptionHandlerAddress = MemoryConfigurations.getDefaultExceptionHandlerAddress(); //0x80000180;
    /**
     * base address for kernel data segment: 0x90000000
     */
    public static int kernelDataBaseAddress = MemoryConfigurations.getDefaultKernelDataBaseAddress(); //0x90000000;
    /**
     * starting address for memory mapped I/O: 0xffff0000 (-65536)
     */
    public static int mmioBaseAddress = MemoryConfigurations.getDefaultMemoryMapBaseAddress(); //0xffff0000;
    /**
     * highest address acessible in kernel mode.
     */
    public static int kernelHighAddress = MemoryConfigurations.getDefaultKernelHighAddress(); //0xffffffff;

    /**
     * MIPS word length in bytes.
     */
    // NOTE:  Much of the code is hardwired for 4 byte words.  Refactoring this is low priority.
    public static final int BYTES_PER_WORD = 4;
    /**
     * MIPS doubleword length in bytes.
     */
    public static final int BYTES_PER_DOUBLEWORD = BYTES_PER_WORD * 2;
    /**
     * MIPS halfword length in bytes.
     */
    public static final int BYTES_PER_HALFWORD = BYTES_PER_WORD / 2;
    /**
     * Constant representing byte order of each memory word.  Little-endian means lowest
     * numbered byte is right most [3][2][1][0].
     */
    public static final boolean LITTLE_ENDIAN = true;
    /**
     * Constant representing byte order of each memory word.  Big-endian means lowest
     * numbered byte is left most [0][1][2][3].
     */
    public static final boolean BIG_ENDIAN = false;
    /**
     * Current setting for endian (default LITTLE_ENDIAN)
     */
    private static boolean byteOrder = LITTLE_ENDIAN;

    public int heapAddress;

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
    // the start of the 65'th block -- table entry 64.  That leaves (1024-64) * 4096 = 3,932,160
    // bytes of space available without going indirect.
    private static final int BLOCK_LENGTH_WORDS = 1024; // allocated blocksize 1024 ints == 4K bytes
    private static final int BLOCK_TABLE_LENGTH = 1024; // Each entry of table points to a block.
    private int[][] dataBlockTable;
    private int[][] kernelDataBlockTable;

    // The stack is modeled similarly to the data segment.  It cannot share the same
    // data structure because the stack base address is very large.  To store it in the
    // same data structure would require implementation of indirect blocks, which has not
    // been realized.  So the stack gets its own table of blocks using the same dimensions
    // and allocation scheme used for data segment.
    //
    // The other major difference is the stack grows DOWNWARD from its base address, not
    // upward.  I.e., the stack base is the largest stack address. This turns the whole
    // scheme for translating memory address to block-offset on its head!  The simplest
    // solution is to calculate relative address (offset from base) by subtracting the
    // desired address from the stack base address (rather than subtracting base address
    // from desired address).  Thus as the address gets smaller the offset gets larger.
    // Everything else works the same, so it shares some private helper methods with
    // data segment algorithms.
    private int[][] stackBlockTable;

    // Memory mapped I/O is simulated with a separate table using the same structure and
    // logic as data segment.  Memory is allocated in 4K byte blocks.  But since MMIO
    // address range is limited to 0xffff0000 to 0xfffffffc, there are only 64K bytes 
    // total.  Thus there will be a maximum of 16 blocks, and I suspect never more than
    // one since only the first few addresses are typically used.  The only exception
    // may be a rogue program generating such addresses in a loop.  Note that the
    // MMIO addresses are interpreted by Java as negative numbers since it does not 
    // have unsigned types.  As long as the absolute address is correctly translated
    // into a table offset, this is of no concern.
    private static final int MMIO_TABLE_LENGTH = 16; // Each entry of table points to a 4K block.
    private int[][] memoryMapBlockTable;

    // I use a similar scheme for storing instructions.  MIPS text segment ranges from
    // 0x00400000 all the way to data segment (0x10000000) a range of about 250 MB!  So
    // I'll provide table of blocks with similar capacity.  This differs from data segment
    // somewhat in that the block entries do not contain int's, but instead contain
    // references to ProgramStatement objects.
    private static final int TEXT_BLOCK_LENGTH_WORDS = 1024; // allocated blocksize 1024 ints == 4K bytes
    private static final int TEXT_BLOCK_TABLE_LENGTH = 1024; // Each entry of table points to a block.
    private ProgramStatement[][] textBlockTable;
    private ProgramStatement[][] kernelTextBlockTable;

    // Set "top" address boundary to go with each "base" address.  This determines permissable
    // address range for user program.  Currently limit is 4MB, or 1024 * 1024 * 4 bytes based
    // on the table structures described above (except memory mapped IO, limited to 64KB by range).
    public static int dataSegmentLimitAddress = dataSegmentBaseAddress + BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * BYTES_PER_WORD;
    public static int textLimitAddress = textBaseAddress + TEXT_BLOCK_LENGTH_WORDS * TEXT_BLOCK_TABLE_LENGTH * BYTES_PER_WORD;
    public static int kernelDataSegmentLimitAddress = kernelDataBaseAddress + BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * BYTES_PER_WORD;
    public static int kernelTextLimitAddress = kernelTextBaseAddress + TEXT_BLOCK_LENGTH_WORDS * TEXT_BLOCK_TABLE_LENGTH * BYTES_PER_WORD;
    public static int stackLimitAddress = stackBaseAddress - BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * BYTES_PER_WORD;
    public static int mmioLimitAddress = mmioBaseAddress + BLOCK_LENGTH_WORDS * MMIO_TABLE_LENGTH * BYTES_PER_WORD;

    // This will be a Singleton class, only one instance is ever created.  Since I know the 
    // Memory object is always needed, I'll go ahead and create it at the time of class loading.
    // (greedy rather than lazy instantiation).  The constructor is private and getInstance()
    // always returns this instance.
    private static final Memory instance = new Memory();

    /**
     * Private constructor for Memory.  Separate data structures for text and data segments.
     */
    private Memory() {
        initialize();
    }

    /**
     * Returns the unique Memory instance, which becomes in essence global.
     */
    public static Memory getInstance() {
        return instance;
    }

    /**
     * Explicitly clear the contents of memory.  Typically done at start of assembly.
     */
    public void clear() {
        setConfiguration();
        initialize();
    }

    /**
     * Sets current memory configuration for simulated MIPS.  Configuration is
     * collection of memory segment addresses. e.g. text segment starting at
     * address 0x00400000.  Configuration can be modified starting with MARS 3.7.
     */
    public static void setConfiguration() {
        MemoryConfiguration config = MemoryConfigurations.getCurrentConfiguration();

        textBaseAddress = config.getTextBaseAddress(); //0x00400000;
        dataSegmentBaseAddress = config.getDataSegmentBaseAddress(); //0x10000000;
        externBaseAddress = config.getExternBaseAddress(); //0x10000000;
        globalPointer = config.getGlobalPointer(); //0x10008000;
        dataBaseAddress = config.getDataBaseAddress(); //0x10010000; // from SPIM not MIPS
        heapBaseAddress = config.getHeapBaseAddress(); //0x10040000; // I think from SPIM not MIPS
        stackPointer = config.getStackPointer(); //0x7fffeffc;
        stackBaseAddress = config.getStackBaseAddress(); //0x7ffffffc;
        userHighAddress = config.getUserHighAddress(); //0x7fffffff;
        kernelBaseAddress = config.getKernelBaseAddress(); //0x80000000;
        kernelTextBaseAddress = config.getKernelTextBaseAddress(); //0x80000000;
        exceptionHandlerAddress = config.getExceptionHandlerAddress(); //0x80000180;
        kernelDataBaseAddress = config.getKernelDataBaseAddress(); //0x90000000;
        mmioBaseAddress = config.getMemoryMapBaseAddress(); //0xffff0000;
        kernelHighAddress = config.getKernelHighAddress(); //0xffffffff;
        dataSegmentLimitAddress = Math.min(config.getDataSegmentLimitAddress(), dataSegmentBaseAddress + BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * BYTES_PER_WORD);
        textLimitAddress = Math.min(config.getTextLimitAddress(), textBaseAddress + TEXT_BLOCK_LENGTH_WORDS * TEXT_BLOCK_TABLE_LENGTH * BYTES_PER_WORD);
        kernelDataSegmentLimitAddress = Math.min(config.getKernelDataSegmentLimitAddress(), kernelDataBaseAddress + BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * BYTES_PER_WORD);
        kernelTextLimitAddress = Math.min(config.getKernelTextLimitAddress(), kernelTextBaseAddress + TEXT_BLOCK_LENGTH_WORDS * TEXT_BLOCK_TABLE_LENGTH * BYTES_PER_WORD);
        stackLimitAddress = Math.max(config.getStackLimitAddress(), stackBaseAddress - BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * BYTES_PER_WORD);
        mmioLimitAddress = Math.min(config.getMemoryMapLimitAddress(), mmioBaseAddress + BLOCK_LENGTH_WORDS * MMIO_TABLE_LENGTH * BYTES_PER_WORD);
    }

    /**
     * Determine whether the current memory configuration has a maximum address that can be stored
     * in 16 bits.
     *
     * @return true if maximum address can be stored in 16 bits or less, false otherwise
     */
    public static boolean usingCompactMemoryConfiguration() {
        return (kernelHighAddress & 0x00007fff) == kernelHighAddress;
    }

    private void initialize() {
        this.heapAddress = heapBaseAddress;
        this.textBlockTable = new ProgramStatement[TEXT_BLOCK_TABLE_LENGTH][];
        this.dataBlockTable = new int[BLOCK_TABLE_LENGTH][];
        this.kernelTextBlockTable = new ProgramStatement[TEXT_BLOCK_TABLE_LENGTH][];
        this.kernelDataBlockTable = new int[BLOCK_TABLE_LENGTH][];
        this.stackBlockTable = new int[BLOCK_TABLE_LENGTH][];
        this.memoryMapBlockTable = new int[MMIO_TABLE_LENGTH][];
        // Encourage the garbage collector to clean up any arrays we just threw away
        System.gc();
    }

    /**
     * Returns the next available word-aligned heap address.  There is no recycling and
     * no heap management!  There is however nearly 4MB of heap space available in Mars.
     *
     * @param numBytes Number of bytes requested.
     * @return Address of the allocated heap storage.
     * @throws IllegalArgumentException Thrown if the number of requested bytes is negative
     *         or exceeds available heap storage.
     */
    public int allocateHeapSpace(int numBytes) throws IllegalArgumentException {
        int result = this.heapAddress;
        if (numBytes < 0) {
            throw new IllegalArgumentException("invalid heap allocation of " + numBytes + " bytes requested");
        }
        int newHeapAddress = alignToNext(this.heapAddress + numBytes, BYTES_PER_WORD);
        if (newHeapAddress > dataSegmentLimitAddress) {
            throw new IllegalArgumentException("heap allocation of " + numBytes + " bytes failed due to insufficient heap space");
        }
        this.heapAddress = newHeapAddress;
        return result;
    }

    /**
     * Set byte order to either LITTLE_ENDIAN or BIG_ENDIAN.  Default is {@link #LITTLE_ENDIAN}.
     *
     * @param order either {@link #LITTLE_ENDIAN} or {@link #BIG_ENDIAN}
     */
    public void setByteOrder(boolean order) {
        byteOrder = order;
    }

    /**
     * Retrieve memory byte order.  Default is {@link #LITTLE_ENDIAN} (like PCs).
     *
     * @return either {@link #LITTLE_ENDIAN} or {@link #BIG_ENDIAN}
     */
    public boolean getByteOrder() {
        return byteOrder;
    }

    /**
     * Starting at the given address, write the given value over the given number of bytes.
     * This one does not check for word boundaries, and copies one byte at a time.
     * If length == 1, takes value from low order byte.  If 2, takes from low order half-word.
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address.
     * @param length  Number of bytes to be written.
     * @return old value that was replaced by the set operation
     */
    // Allocates blocks if necessary.
    public int set(int address, int value, int length) throws AddressErrorException {
        int oldValue = 0;
        int relativeByteAddress;
        if (isInDataSegment(address)) {
            // In data segment.  Will write one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - dataSegmentBaseAddress; // relative to data segment start, in bytes
            oldValue = this.storeBytesInTable(dataBlockTable, relativeByteAddress, length, value);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // In stack.  Handle similarly to data segment write, except relative byte
            // address calculated "backward" because stack addresses grow down from base.
            relativeByteAddress = stackBaseAddress - address;
            oldValue = this.storeBytesInTable(stackBlockTable, relativeByteAddress, length, value);
        }
        else if (isInTextSegment(address) || isInKernelTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with call to setStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            ProgramStatement oldStatement = this.setStatement(address, new ProgramStatement(value, address));
            if (oldStatement != null) {
                oldValue = oldStatement.getBinaryStatement();
            }
        }
        else if (address >= mmioBaseAddress && address < mmioLimitAddress) {
            // Memory-mapped I/O
            relativeByteAddress = address - mmioBaseAddress;
            oldValue = this.storeBytesInTable(memoryMapBlockTable, relativeByteAddress, length, value);
        }
        else if (isInKernelDataSegment(address)) {
            // In kernel data segment.  Will write one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - kernelDataBaseAddress; // relative to data segment start, in bytes
            oldValue = this.storeBytesInTable(kernelDataBlockTable, relativeByteAddress, length, value);
        }
        else {
            // Falls outside MARS addressing range
            throw new AddressErrorException("address out of range ", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
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
     * @throws AddressErrorException If address is not on word boundary.
     */
    public void setRawWord(int address, int value) throws AddressErrorException {
        if (!isWordAligned(address)) {
            throw new AddressErrorException("store address not aligned on word boundary ", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        int oldValue = 0;
        int relative;
        if (isInDataSegment(address)) {
            // In data segment
            relative = (address - dataSegmentBaseAddress) >> 2; // convert byte address to words
            oldValue = this.storeWordInTable(dataBlockTable, relative, value);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // In stack.  Handle similarly to data segment write, except relative
            // address calculated "backward" because stack addresses grow down from base.
            relative = (stackBaseAddress - address) >> 2; // convert byte address to words
            oldValue = this.storeWordInTable(stackBlockTable, relative, value);
        }
        else if (isInTextSegment(address) | isInKernelTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with call to setStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            ProgramStatement oldStatement = this.setStatement(address, new ProgramStatement(value, address));
            if (oldStatement != null) {
                oldValue = oldStatement.getBinaryStatement();
            }
        }
        else if (address >= mmioBaseAddress && address < mmioLimitAddress) {
            // Memory-mapped I/O
            relative = (address - mmioBaseAddress) >> 2; // convert byte address to word
            oldValue = this.storeWordInTable(memoryMapBlockTable, relative, value);
        }
        else if (isInKernelDataSegment(address)) {
            // In kernel data segment
            relative = (address - kernelDataBaseAddress) >> 2; // convert byte address to words
            oldValue = this.storeWordInTable(kernelDataBlockTable, relative, value);
        }
        else {
            // Falls outside MARS addressing range
            throw new AddressErrorException("store address out of range ", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
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
     * @throws AddressErrorException If address is not on word boundary.
     */
    public void setWord(int address, int value) throws AddressErrorException {
        if (!isWordAligned(address)) {
            throw new AddressErrorException("store address not aligned on word boundary ", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        int oldValue = this.set(address, value, BYTES_PER_WORD);
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
     * @throws AddressErrorException If address is not on halfword boundary.
     */
    public void setHalfword(int address, int value) throws AddressErrorException {
        if (!isHalfwordAligned(address)) {
            throw new AddressErrorException("store address not aligned on halfword boundary ", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
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
     */
    public void setDoubleword(int address, double value) throws AddressErrorException {
        long longValue = Double.doubleToLongBits(value);
        this.set(address + 4, Binary.highOrderLongToInt(longValue), 4);
        this.set(address, Binary.lowOrderLongToInt(longValue), 4);
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
        ProgramStatement oldValue;
        if (!isWordAligned(address)) {
            throw new AddressErrorException("store address not aligned on word boundary ", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
        }
        if (isInTextSegment(address)) {
            oldValue = this.storeProgramStatement(address, statement, textBaseAddress, textBlockTable);
        }
        else if (isInKernelTextSegment(address)) {
            oldValue = this.storeProgramStatement(address, statement, kernelTextBaseAddress, kernelTextBlockTable);
        }
        else {
            throw new AddressErrorException("store address out of range ", ExceptionCause.ADDRESS_EXCEPTION_STORE, address);
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
    public int get(int address, int length) throws AddressErrorException {
        return this.get(address, length, true);
    }

    // Does the real work, but includes option to NOT notify observers.
    private int get(int address, int length, boolean notify) throws AddressErrorException {
        int value;
        int relativeByteAddress;
        if (isInDataSegment(address)) {
            // In data segment.  Will read one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - dataSegmentBaseAddress; // relative to data segment start, in bytes
            value = this.fetchBytesFromTable(dataBlockTable, relativeByteAddress, length);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // In stack. Similar to data, except relative address computed "backward"
            relativeByteAddress = stackBaseAddress - address;
            value = this.fetchBytesFromTable(stackBlockTable, relativeByteAddress, length);
        }

        else if (address >= mmioBaseAddress && address < mmioLimitAddress) {
            // Memory-mapped I/O
            relativeByteAddress = address - mmioBaseAddress;
            value = this.fetchBytesFromTable(memoryMapBlockTable, relativeByteAddress, length);
        }
        else if (isInTextSegment(address) || isInKernelTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify & getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            ProgramStatement statement = this.getStatementNoNotify(address);
            value = statement == null ? 0 : statement.getBinaryStatement();
        }
        else if (isInKernelDataSegment(address)) {
            // In kernel data segment.  Will read one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - kernelDataBaseAddress; // relative to data segment start, in bytes
            value = this.fetchBytesFromTable(kernelDataBlockTable, relativeByteAddress, length);
        }
        else {
            // Falls outside MARS addressing range
            throw new AddressErrorException("address out of range ", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
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
        if (isInDataSegment(address)) {
            // In data segment
            relative = (address - dataSegmentBaseAddress) >> 2; // convert byte address to words
            value = this.fetchWordFromTable(dataBlockTable, relative);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // In stack. Similar to data, except relative address computed "backward"
            relative = (stackBaseAddress - address) >> 2; // convert byte address to words
            value = this.fetchWordFromTable(stackBlockTable, relative);
        }
        else if (address >= mmioBaseAddress && address < mmioLimitAddress) {
            // Memory-mapped I/O
            relative = (address - mmioBaseAddress) >> 2;
            value = this.fetchWordFromTable(memoryMapBlockTable, relative);
        }
        else if (isInTextSegment(address) || isInKernelTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify & getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            // Sean Clarke (05/2024): don't throw, reading should be fine regardless of self-modifying code setting
            ProgramStatement statement = this.getStatementNoNotify(address);
            value = statement == null ? 0 : statement.getBinaryStatement();
        }
        else if (isInKernelDataSegment(address)) {
            // In kernel data segment
            relative = (address - kernelDataBaseAddress) >> 2; // convert byte address to words
            value = this.fetchWordFromTable(kernelDataBlockTable, relative);
        }
        else {
            // Falls outside MARS addressing range
            throw new AddressErrorException("address out of range", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
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
        if (isInDataSegment(address)) {
            // In data segment
            relative = (address - dataSegmentBaseAddress) >> 2; // convert byte address to words
            value = this.fetchWordOrNullFromTable(dataBlockTable, relative);
        }
        else if (address > stackLimitAddress && address <= stackBaseAddress) {
            // In stack. Similar to data, except relative address computed "backward"
            relative = (stackBaseAddress - address) >> 2; // convert byte address to words
            value = this.fetchWordOrNullFromTable(stackBlockTable, relative);
        }
        else if (isInTextSegment(address) || isInKernelTextSegment(address)) {
            ProgramStatement statement = this.getStatementNoNotify(address);
            if (statement != null) {
                value = statement.getBinaryStatement();
            }
        }
        else if (isInKernelDataSegment(address)) {
            // In kernel data segment
            relative = (address - kernelDataBaseAddress) >> 2; // convert byte address to words
            value = this.fetchWordOrNullFromTable(kernelDataBlockTable, relative);
        }
        else {
            // Falls outside Mars addressing range
            throw new AddressErrorException("address out of range", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
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
        if (!isWordAligned(address)) {
            throw new AddressErrorException("fetch address not aligned on word boundary ", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        return this.get(address, BYTES_PER_WORD, true);
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int. Observers are NOT notified.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int getWordNoNotify(int address) throws AddressErrorException {
        if (!isWordAligned(address)) {
            throw new AddressErrorException("fetch address not aligned on word boundary ", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        return this.get(address, BYTES_PER_WORD, false);
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
            throw new AddressErrorException("fetch address not aligned on halfword boundary ", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        return this.get(address, BYTES_PER_HALFWORD);
    }

    /**
     * Reads specified Memory byte into low order 8 bits of int.
     *
     * @param address Address of Memory byte to be read.
     * @return Value stored at that address.  Only low order 8 bits used.
     */
    public int getByte(int address) throws AddressErrorException {
        return this.get(address, 1);
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
            throw new AddressErrorException("fetch address for text segment not aligned to word boundary ", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        if (isInTextSegment(address)) {
            return this.readProgramStatement(address, textBaseAddress, textBlockTable, notify);
        }
        else if (isInKernelTextSegment(address)) {
            return this.readProgramStatement(address, kernelTextBaseAddress, kernelTextBlockTable, notify);
        }
        else if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
            throw new AddressErrorException("fetch address for text segment out of range ", ExceptionCause.ADDRESS_EXCEPTION_LOAD, address);
        }
        else {
            return new ProgramStatement(this.get(address, BYTES_PER_WORD, notify), address);
        }
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
        return address % BYTES_PER_WORD == 0;
    }

    /**
     * Utility to determine if given address is halfword-aligned.
     *
     * @param address the address to check
     * @return true if address is halfword-aligned, false otherwise
     */
    public static boolean isHalfwordAligned(int address) {
        return address % BYTES_PER_HALFWORD == 0;
    }

    /**
     * Utility to determine if given address is doubleword-aligned.
     *
     * @param address the address to check
     * @return true if address is doubleword-aligned, false otherwise
     */
    public static boolean isDoublewordAligned(int address) {
        return address % BYTES_PER_DOUBLEWORD == 0;
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
        int excess = address % alignment;
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
        return address - address % alignment;
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
    public static boolean isInTextSegment(int address) {
        return address >= textBaseAddress && address < textLimitAddress;
    }

    /**
     * Handy little utility to find out if given address is in MARS kernel
     * text segment (starts at Memory.kernelTextBaseAddress).
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined kernel text segment,
     *     false otherwise.
     */
    public static boolean isInKernelTextSegment(int address) {
        return address >= kernelTextBaseAddress && address < kernelTextLimitAddress;
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
    public static boolean isInDataSegment(int address) {
        return address >= dataSegmentBaseAddress && address < dataSegmentLimitAddress;
    }

    /**
     * Handy little utility to find out if given address is in MARS kernel data
     * segment (starts at Memory.kernelDataSegmentBaseAddress).
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined kernel data segment,
     *     false otherwise.
     */
    public static boolean isInKernelDataSegment(int address) {
        return address >= kernelDataBaseAddress && address < kernelDataSegmentLimitAddress;
    }

    /**
     * Handy little utility to find out if given address is in the Memory Map area
     * starts at Memory.memoryMapBaseAddress, range 0xffff0000 to 0xffffffff.
     *
     * @param address integer memory address
     * @return true if that address is within MARS-defined memory map (MMIO) area,
     *     false otherwise.
     */
    public static boolean isInMemoryMapSegment(int address) {
        return address >= mmioBaseAddress && address < kernelHighAddress;
    }

    // ALL THE OBSERVABLE STUFF GOES HERE.  FOR COMPATIBILITY, Memory IS STILL
    // EXTENDING OBSERVABLE, BUT WILL NOT USE INHERITED METHODS.  WILL INSTEAD
    // USE A COLLECTION OF MemoryObserver OBJECTS, EACH OF WHICH IS COMBINATION
    // OF AN OBSERVER WITH AN ADDRESS RANGE.

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
     *
     * @param listener The listener to add.
     * @implNote This is equivalent to calling
     *           {@link #addListener(Listener, int, int) addListener(listener, 0x00000000, 0xFFFFFFFF)}.
     */
    public void addListener(Listener listener) {
        this.addListener(listener, 0x00000000, 0xFFFFFFFF);
    }

    /**
     * Register the given listener to a single address.
     * The listener will be notified when any read/write operations occur on the byte at that address.
     *
     * @param listener The listener to add.
     * @param address  The memory address of the byte to attach the listener to.
     * @implNote This is equivalent to calling
     *           {@link #addListener(Listener, int, int) addListener(listener, address, address)}.
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
            if (Application.program != null || Application.getGUI() == null) {
                int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
                int wordValue = this.getRawWord(wordAddress);
                for (ListenerRange range : this.getListeners(address, length)) {
                    range.listener.memoryRead(address, length, value, wordAddress, wordValue);
                }
            }
        }
        catch (AddressErrorException exception) {
            // This should not happen since wordAddress is word-aligned
        }
    }

    private void dispatchWriteEvent(int address, int length, int value) {
        try {
            if (Application.program != null || Application.getGUI() == null) {
                int wordAddress = alignToPrevious(address, BYTES_PER_WORD);
                int wordValue = this.getRawWord(wordAddress);
                for (ListenerRange range : this.getListeners(address, length)) {
                    range.listener.memoryWritten(address, length, value, wordAddress, wordValue);
                }
            }
        }
        catch (AddressErrorException exception) {
            // This should not happen since wordAddress is word-aligned
        }
    }

    /**
     * Helper method to store 1, 2 or 4 byte value in table that represents MIPS
     * memory. Originally used just for data segment, but now also used for stack.
     * Both use different tables but same storage method and same table size
     * and block size.
     * <p>
     * Modified 29 Dec 2005 to return old value of replaced bytes.
     */
    private int storeBytesInTable(int[][] blockTable, int relativeByteAddress, int length, int value) {
        return this.storeOrFetchBytesInTable(blockTable, relativeByteAddress, length, value, true);
    }

    /**
     * Helper method to fetch 1, 2 or 4 byte value from table that represents MIPS
     * memory.  Originally used just for data segment, but now also used for stack.
     * Both use different tables but same storage method and same table size
     * and block size.
     */
    private int fetchBytesFromTable(int[][] blockTable, int relativeByteAddress, int length) {
        return this.storeOrFetchBytesInTable(blockTable, relativeByteAddress, length, 0, false);
    }

    /**
     * The helper's helper.  Works for either storing or fetching, little or big endian.
     * When storing/fetching bytes, most of the work is calculating the correct array element(s)
     * and element byte(s).  This method performs either store or fetch, as directed by its
     * client using STORE or FETCH in last arg.
     * <p>
     * Modified 29 Dec 2005 to return old value of replaced bytes, for STORE.
     */
    private synchronized int storeOrFetchBytesInTable(int[][] blockTable, int relativeByteAddress, int length, int value, boolean doStore) {
        int relativeWordAddress, block, offset, bytePositionInMemory, bytePositionInValue;
        int oldValue = 0; // for STORE, return old values of replaced bytes
        int loopStopper = 3 - length;
        // IF added DPS 22-Dec-2008. NOTE: has NOT been tested with Big-Endian.
        // Fix provided by Saul Spatz; comments that follow are his.
        // If address in stack segment is 4k + m, with 0 < m < 4, then the
        // relativeByteAddress we want is stackBaseAddress - 4k + m, but the
        // address actually passed in is stackBaseAddress - (4k + m), so we
        // need to add 2m.  Because of the change in sign, we get the
        // expression 4-delta below in place of m.
        if (blockTable == stackBlockTable) {
            int delta = relativeByteAddress % 4;
            if (delta != 0) {
                relativeByteAddress += (4 - delta) << 1;
            }
        }
        for (bytePositionInValue = 3; bytePositionInValue > loopStopper; bytePositionInValue--) {
            bytePositionInMemory = relativeByteAddress % 4;
            relativeWordAddress = relativeByteAddress >> 2;
            block = relativeWordAddress / BLOCK_LENGTH_WORDS; // Block number
            offset = relativeWordAddress % BLOCK_LENGTH_WORDS; // Word within that block
            if (blockTable[block] == null) {
                if (doStore) {
                    blockTable[block] = new int[BLOCK_LENGTH_WORDS];
                }
                else {
                    return 0;
                }
            }
            if (byteOrder == LITTLE_ENDIAN) {
                bytePositionInMemory = 3 - bytePositionInMemory;
            }
            if (doStore) {
                oldValue = replaceByte(blockTable[block][offset], bytePositionInMemory, oldValue, bytePositionInValue);
                blockTable[block][offset] = replaceByte(value, bytePositionInValue, blockTable[block][offset], bytePositionInMemory);
            }
            else {
                value = replaceByte(blockTable[block][offset], bytePositionInMemory, value, bytePositionInValue);
            }
            relativeByteAddress++;
        }
        return (doStore) ? oldValue : value;
    }

    /**
     * Helper method to store 4 byte value in table that represents MIPS memory.
     * Originally used just for data segment, but now also used for stack.
     * Both use different tables but same storage method and same table size
     * and block size.  Assumes address is word aligned, no endian processing.
     * Modified 29 Dec 2005 to return overwritten value.
     */
    private synchronized int storeWordInTable(int[][] blockTable, int relative, int value) {
        int block, offset, oldValue;
        block = relative / BLOCK_LENGTH_WORDS;
        offset = relative % BLOCK_LENGTH_WORDS;
        if (blockTable[block] == null) {
            // First time writing to this block, so allocate the space.
            blockTable[block] = new int[BLOCK_LENGTH_WORDS];
        }
        oldValue = blockTable[block][offset];
        blockTable[block][offset] = value;
        return oldValue;
    }

    /**
     * Helper method to fetch 4 byte value from table that represents MIPS memory.
     * Originally used just for data segment, but now also used for stack.
     * Both use different tables but same storage method and same table size
     * and block size.  Assumes word alignment, no endian processing.
     */
    private synchronized int fetchWordFromTable(int[][] blockTable, int relative) {
        int block = relative / BLOCK_LENGTH_WORDS;
        int offset = relative % BLOCK_LENGTH_WORDS;
        if (blockTable[block] == null) {
            // This block has not been allocated, so assume it is 0 by default
            return 0;
        }
        else {
            return blockTable[block][offset];
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
    private synchronized Integer fetchWordOrNullFromTable(int[][] blockTable, int relative) {
        int block = relative / BLOCK_LENGTH_WORDS;
        int offset = relative % BLOCK_LENGTH_WORDS;
        if (blockTable[block] == null) {
            // This block has not been allocated, so return null
            return null;
        }
        else {
            return blockTable[block][offset];
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
            ((sourceValue >> (24 - (bytePosInSource << 3)) & 0xFF) << (24 - (bytePosInDest << 3)))
                // and bitwise-OR it with...
                |
                // Set 8 bits in destination byte position to 0's, other 24 bits are unchanged.
                (destValue & ~(0xFF << (24 - (bytePosInDest << 3))));
    }

    /**
     * Store a program statement at the given address.  Address has already been verified
     * as valid.  It may be either in user or kernel text segment, as specified by arguments.
     */
    private ProgramStatement storeProgramStatement(int address, ProgramStatement statement, int baseAddress, ProgramStatement[][] blockTable) {
        int relative = (address - baseAddress) >> 2; // Convert byte address to words
        int block = relative / BLOCK_LENGTH_WORDS;
        int offset = relative % BLOCK_LENGTH_WORDS;
        ProgramStatement oldValue = null;
        if (block < TEXT_BLOCK_TABLE_LENGTH) {
            if (blockTable[block] == null) {
                // No instructions are stored in this block yet, so allocate the block
                blockTable[block] = new ProgramStatement[BLOCK_LENGTH_WORDS];
            }
            else {
                oldValue = blockTable[block][offset];
            }
            blockTable[block][offset] = statement;
        }
        return oldValue;
    }

    /**
     * Read a program statement from the given address.  Address has already been verified
     * as valid.  It may be either in user or kernel text segment, as specified by arguments.
     * Returns associated ProgramStatement or null if none.
     * Last parameter controls whether or not observers will be notified.
     */
    private ProgramStatement readProgramStatement(int address, int baseAddress, ProgramStatement[][] blockTable, boolean notify) {
        int relative = (address - baseAddress) >> 2; // convert byte address to words
        int block = relative / TEXT_BLOCK_LENGTH_WORDS;
        int offset = relative % TEXT_BLOCK_LENGTH_WORDS;
        if (block < TEXT_BLOCK_TABLE_LENGTH) {
            if (blockTable[block] == null || blockTable[block][offset] == null) {
                // No instructions are stored in this block
                if (notify) {
                    this.dispatchReadEvent(address, Instruction.BYTES_PER_INSTRUCTION, 0);
                }
                return null;
            }
            else {
                ProgramStatement statement = blockTable[block][offset];
                if (notify) {
                    this.dispatchReadEvent(address, Instruction.BYTES_PER_INSTRUCTION, statement.getBinaryStatement());
                }
                return statement;
            }
        }
        if (notify) {
            this.dispatchReadEvent(address, Instruction.BYTES_PER_INSTRUCTION, 0);
        }
        return null;
    }
}
