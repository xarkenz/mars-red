package mars.mips.hardware;

import mars.Application;

import java.util.ArrayList;
import java.util.List;

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
 * Models the collection of MIPS memory configurations.
 * The default configuration is based on SPIM.  Starting with MARS 3.7,
 * the configuration can be changed.
 *
 * @author Pete Sanderson
 * @version August 2009
 */
public class MemoryConfigurations {
    public static final int TEXT_BASE = 0;
    public static final int DATA_SEGMENT_BASE = 1;
    public static final int EXTERN_BASE = 2;
    public static final int GLOBAL_POINTER = 3;
    public static final int DATA_BASE = 4;
    public static final int HEAP_BASE = 5;
    public static final int STACK_POINTER = 6;
    public static final int STACK_BASE = 7;
    public static final int USER_HIGH = 8;
    public static final int KERNEL_BASE = 9;
    public static final int KERNEL_TEXT_BASE = 10;
    public static final int EXCEPTION_HANDLER = 11;
    public static final int KERNEL_DATA_BASE = 12;
    public static final int MMIO_BASE = 13;
    public static final int KERNEL_HIGH = 14;
    public static final int DATA_SEGMENT_LIMIT = 15;
    public static final int TEXT_LIMIT = 16;
    public static final int KERNEL_DATA_LIMIT = 17;
    public static final int KERNEL_TEXT_LIMIT = 18;
    public static final int STACK_LIMIT = 19;
    public static final int MMIO_LIMIT = 20;

    // Be careful, these arrays are parallel and position-sensitive.
    // The getters in this and in MemoryConfiguration depend on this sequence.

    private static final String[] ADDRESS_NAMES = {
        ".text base address",
        "data segment base address",
        ".extern base address",
        "global pointer $gp",
        ".data base address",
        "heap base address",
        "stack pointer $sp",
        "stack base address",
        "user space high address",
        "kernel space base address",
        ".ktext base address",
        "exception handler address",
        ".kdata base address",
        "MMIO base address",
        "kernel space high address",
        "data segment limit address",
        "text limit address",
        "kernel data segment limit address",
        "kernel text limit address",
        "stack limit address",
        "MMIO limit address",
    };

    /**
     * Default configuration comes from SPIM.
     */
    private static final int[] DEFAULT_ADDRESSES = {
        0x00400000, // .text Base Address
        0x10000000, // Data Segment base address
        0x10000000, // .extern Base Address
        0x10008000, // Global Pointer $gp)
        0x10010000, // .data base Address
        0x10040000, // heap base address
        0x7fffeffc, // stack pointer $sp (from SPIM not MIPS)
        0x7ffffffc, // stack base address
        0x7fffffff, // highest address in user space
        0x80000000, // lowest address in kernel space
        0x80000000, // .ktext base address
        0x80000180, // exception handler address
        0x90000000, // .kdata base address
        0xffff0000, // MMIO base address
        0xffffffff, // highest address in kernel (and memory)
        0x7fffffff, // data segment limit address
        0x0ffffffc, // text limit address
        0xfffeffff, // kernel data segment limit address
        0x8ffffffc, // kernel text limit address
        0x10040000, // stack limit address
        0xffffffff, // memory map limit address
    };
    /**
     * Compact allows 16 bit addressing, data segment starts at 0.
     */
    private static final int[] DATA_BASED_COMPACT_ADDRESSES = {
        0x00003000, // .text Base Address
        0x00000000, // Data Segment base address
        0x00001000, // .extern Base Address
        0x00001800, // Global Pointer $gp)
        0x00000000, // .data base Address
        0x00002000, // heap base address
        0x00002ffc, // stack pointer $sp
        0x00002ffc, // stack base address
        0x00003fff, // highest address in user space
        0x00004000, // lowest address in kernel space
        0x00004000, // .ktext base address
        0x00004180, // exception handler address
        0x00005000, // .kdata base address
        0x00007f00, // MMIO base address
        0x00007fff, // highest address in kernel (and memory)
        0x00002fff, // data segment limit address
        0x00003ffc, // text limit address
        0x00007eff, // kernel data segment limit address
        0x00004ffc, // kernel text limit address
        0x00002000, // stack limit address
        0x00007fff, // MMIO limit address
    };
    /**
     * Compact allows 16 bit addressing, text segment starts at 0.
     */
    private static final int[] TEXT_BASED_COMPACT_ADDRESSES = {
        0x00000000, // .text Base Address
        0x00001000, // Data Segment base address
        0x00001000, // .extern Base Address
        0x00001800, // Global Pointer $gp)
        0x00002000, // .data base Address
        0x00003000, // heap base address
        0x00003ffc, // stack pointer $sp
        0x00003ffc, // stack base address
        0x00003fff, // highest address in user space
        0x00004000, // lowest address in kernel space
        0x00004000, // .ktext base address
        0x00004180, // exception handler address
        0x00005000, // .kdata base address
        0x00007f00, // MMIO base address
        0x00007fff, // highest address in kernel (and memory)
        0x00003fff, // data segment limit address
        0x00000ffc, // text limit address
        0x00007eff, // kernel data segment limit address
        0x00004ffc, // kernel text limit address
        0x00003000, // stack limit address
        0x00007fff, // MMIO limit address
    };

    private static List<MemoryConfiguration> configurations = null;
    private static MemoryConfiguration defaultConfiguration;
    private static MemoryConfiguration currentConfiguration;

    public static List<MemoryConfiguration> getConfigurations() {
        if (configurations == null) {
            defaultConfiguration = new MemoryConfiguration("Default", "Default", ADDRESS_NAMES, DEFAULT_ADDRESSES);
            currentConfiguration = defaultConfiguration;

            configurations = new ArrayList<>();
            configurations.add(defaultConfiguration);
            configurations.add(new MemoryConfiguration("CompactDataAtZero", "Compact, Data at Address 0", ADDRESS_NAMES, DATA_BASED_COMPACT_ADDRESSES));
            configurations.add(new MemoryConfiguration("CompactTextAtZero", "Compact, Text at Address 0", ADDRESS_NAMES, TEXT_BASED_COMPACT_ADDRESSES));
            // Get current config from settings
            setCurrentConfiguration(getConfiguration(Application.getSettings().memoryConfiguration.get()));
        }
        return configurations;
    }

    public static MemoryConfiguration getConfiguration(String identifier) {
        for (MemoryConfiguration config : getConfigurations()) {
            if (config.identifier().equals(identifier)) {
                return config;
            }
        }
        return null;
    }

    public static MemoryConfiguration getDefaultConfiguration() {
        getConfigurations();
        return defaultConfiguration;
    }

    public static MemoryConfiguration getCurrentConfiguration() {
        getConfigurations();
        return currentConfiguration;
    }

    public static boolean setCurrentConfiguration(MemoryConfiguration config) {
        if (config == null) {
            return false;
        }
        if (config != currentConfiguration) {
            currentConfiguration = config;
            Application.memory.reset();
            RegisterFile.getRegisters()[RegisterFile.GLOBAL_POINTER].setDefaultValue(config.getAddress(GLOBAL_POINTER));
            RegisterFile.getRegisters()[RegisterFile.STACK_POINTER].setDefaultValue(config.getAddress(STACK_POINTER));
            RegisterFile.getProgramCounterRegister().setDefaultValue(config.getAddress(TEXT_BASE));
            RegisterFile.resetRegisters();
            return true;
        }
        else {
            return false;
        }
    }
}