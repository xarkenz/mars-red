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
    /**
     * Lowest mapped address.
     */
    public static final int MAPPED_LOW = 0;
    /**
     * Highest mapped address.
     */
    public static final int MAPPED_HIGH = 1;
    /**
     * Lowest user space address.
     */
    public static final int USER_LOW = 2;
    /**
     * Highest user space address.
     */
    public static final int USER_HIGH = 3;
    /**
     * Lowest text segment address.
     */
    public static final int TEXT_LOW = 4;
    /**
     * Highest text segment address.
     */
    public static final int TEXT_HIGH = 5;
    /**
     * Lowest data segment address.
     */
    public static final int DATA_LOW = 6;
    /**
     * Highest data segment address.
     */
    public static final int DATA_HIGH = 7;
    /**
     * Lowest extern data address.
     */
    public static final int EXTERN_LOW = 8;
    /**
     * Highest extern data address.
     */
    public static final int EXTERN_HIGH = 9;
    /**
     * Lowest static data address.
     */
    public static final int STATIC_LOW = 10;
    /**
     * Highest static data address.
     */
    public static final int STATIC_HIGH = 11;
    /**
     * Lowest heap/stack data address.
     */
    public static final int DYNAMIC_LOW = 12;
    /**
     * Highest heap/stack data address.
     */
    public static final int DYNAMIC_HIGH = 13;
    /**
     * Lowest kernel text segment address.
     */
    public static final int KERNEL_TEXT_LOW = 14;
    /**
     * Highest kernel text segment address.
     */
    public static final int KERNEL_TEXT_HIGH = 15;
    /**
     * Lowest kernel data segment address.
     */
    public static final int KERNEL_DATA_LOW = 16;
    /**
     * Highest kernel data segment address.
     */
    public static final int KERNEL_DATA_HIGH = 17;
    /**
     * Lowest memory-mapped I/O address.
     */
    public static final int MMIO_LOW = 18;
    /**
     * Highest memory-mapped I/O address.
     */
    public static final int MMIO_HIGH = 19;
    /**
     * Exception handler address.
     */
    public static final int EXCEPTION_HANDLER = 20;
    /**
     * Initial global pointer ($gp).
     */
    public static final int GLOBAL_POINTER = 21;
    /**
     * Initial stack pointer ($sp).
     */
    public static final int STACK_POINTER = 22;

    // Be careful, these arrays are parallel and position-sensitive.
    // The getters in this and in MemoryConfiguration depend on this sequence.

    private static final String[] ADDRESS_NAMES = {
        "Lowest mapped address",
        "Highest mapped address",
        "Lowest user space address",
        "Highest user space address",
        "Lowest text segment address",
        "Highest text segment address",
        "Lowest data segment address",
        "Highest data segment address",
        "Lowest extern data address",
        "Highest extern data address",
        "Lowest static data address",
        "Highest static data address",
        "Lowest heap/stack data address",
        "Highest heap/stack data address",
        "Lowest kernel text segment address",
        "Highest kernel text segment address",
        "Lowest kernel data segment address",
        "Highest kernel data segment address",
        "Lowest memory-mapped I/O address",
        "Highest memory-mapped I/O address",
        "Exception handler address",
        "Initial global pointer ($gp)",
        "Initial stack pointer ($sp)",
    };

    /**
     * Default configuration comes from SPIM.
     */
    private static final int[] DEFAULT_ADDRESSES = {
        0x00400000, // Lowest mapped address
        0xFFFFFFFF, // Highest mapped address
        0x00400000, // Lowest user space address
        0x7FFFFFFF, // Highest user space address
        0x00400000, // Lowest text segment address
        0x0FFFFFFF, // Highest text segment address
        0x10000000, // Lowest data segment address
        0x7FFFFFFF, // Highest data segment address
        0x10000000, // Lowest extern data address
        0x1000FFFF, // Highest extern data address
        0x10010000, // Lowest static data address
        0x1003FFFF, // Highest static data address
        0x10040000, // Lowest heap/stack data address
        0x7FFFFFFF, // Highest heap/stack data address
        0x80000000, // Lowest kernel text segment address
        0x8FFFFFFF, // Highest kernel text segment address
        0x90000000, // Lowest kernel data segment address
        0xFFFEFFFF, // Highest kernel data segment address
        0xFFFF0000, // Lowest memory-mapped I/O address
        0xFFFFFFFF, // Highest memory-mapped I/O address
        0x80000180, // Exception handler address
        0x10008000, // Initial global pointer ($gp)
        0x7FFFEFFC, // Initial stack pointer ($sp)
    };
    /**
     * Compact allows 16 bit addressing, data segment starts at 0.
     */
    private static final int[] DATA_BASED_COMPACT_ADDRESSES = {
        0x00000000, // Lowest mapped address
        0x00007FFF, // Highest mapped address
        0x00000000, // Lowest user space address
        0x00003FFF, // Highest user space address
        0x00003000, // Lowest text segment address
        0x00003FFF, // Highest text segment address
        0x00000000, // Lowest data segment address
        0x00002FFF, // Highest data segment address
        0x00001000, // Lowest extern data address
        0x00001FFF, // Highest extern data address
        0x00000000, // Lowest static data address
        0x00000FFF, // Highest static data address
        0x00002000, // Lowest heap/stack data address
        0x00002FFF, // Highest heap/stack data address
        0x00004000, // Lowest kernel text segment address
        0x00004FFF, // Highest kernel text segment address
        0x00005000, // Lowest kernel data segment address
        0x00007EFF, // Highest kernel data segment address
        0x00007F00, // Lowest memory-mapped I/O address
        0x00007FFF, // Highest memory-mapped I/O address
        0x00004180, // Exception handler address
        0x00001800, // Initial global pointer ($gp)
        0x00002FFC, // Initial stack pointer ($sp)
    };
    /**
     * Compact allows 16 bit addressing, text segment starts at 0.
     */
    private static final int[] TEXT_BASED_COMPACT_ADDRESSES = {
        0x00000000, // Lowest mapped address
        0x00007FFF, // Highest mapped address
        0x00000000, // Lowest user space address
        0x00003FFF, // Highest user space address
        0x00000000, // Lowest text segment address
        0x00000FFF, // Highest text segment address
        0x00001000, // Lowest data segment address
        0x00003FFF, // Highest data segment address
        0x00001000, // Lowest extern data address
        0x00001FFF, // Highest extern data address
        0x00002000, // Lowest static data address
        0x00002FFF, // Highest static data address
        0x00003000, // Lowest heap/stack data address
        0x00003FFF, // Highest heap/stack data address
        0x00004000, // Lowest kernel text segment address
        0x00004FFF, // Highest kernel text segment address
        0x00005000, // Lowest kernel data segment address
        0x00007EFF, // Highest kernel data segment address
        0x00007F00, // Lowest memory-mapped I/O address
        0x00007FFF, // Highest memory-mapped I/O address
        0x00004180, // Exception handler address
        0x00001800, // Initial global pointer ($gp)
        0x00003FFC, // Initial stack pointer ($sp)
    };

    private static List<MemoryConfiguration> configurations = null;
    private static MemoryConfiguration defaultConfiguration = null;
    private static MemoryConfiguration currentConfiguration = null;

    public static List<MemoryConfiguration> getConfigurations() {
        if (configurations == null) {
            defaultConfiguration = new MemoryConfiguration("Default", "Default", ADDRESS_NAMES, DEFAULT_ADDRESSES);
            setCurrentConfiguration(defaultConfiguration);

            configurations = new ArrayList<>();
            configurations.add(defaultConfiguration);
            configurations.add(new MemoryConfiguration("CompactDataAtZero", "Compact (static data at address 0)", ADDRESS_NAMES, DATA_BASED_COMPACT_ADDRESSES));
            configurations.add(new MemoryConfiguration("CompactTextAtZero", "Compact (text at address 0)", ADDRESS_NAMES, TEXT_BASED_COMPACT_ADDRESSES));
            // Get current config from settings
            setCurrentConfiguration(getConfiguration(Application.getSettings().memoryConfiguration.get()));
        }
        return configurations;
    }

    public static MemoryConfiguration getConfiguration(String identifier) {
        for (MemoryConfiguration configuration : getConfigurations()) {
            if (configuration.identifier().equals(identifier)) {
                return configuration;
            }
        }
        return null;
    }

    public static MemoryConfiguration getDefaultConfiguration() {
        if (defaultConfiguration == null) {
            getConfigurations();
        }
        return defaultConfiguration;
    }

    public static MemoryConfiguration getCurrentConfiguration() {
        if (currentConfiguration == null) {
            getConfigurations();
        }
        return currentConfiguration;
    }

    public static boolean setCurrentConfiguration(MemoryConfiguration configuration) {
        if (configuration == null) {
            return false;
        }
        if (configuration != currentConfiguration) {
            currentConfiguration = configuration;
            Memory.getInstance().reset();
            RegisterFile.getRegisters()[RegisterFile.GLOBAL_POINTER].setDefaultValue(configuration.getAddress(GLOBAL_POINTER));
            RegisterFile.getRegisters()[RegisterFile.STACK_POINTER].setDefaultValue(configuration.getAddress(STACK_POINTER));
            RegisterFile.getProgramCounterRegister().setDefaultValue(configuration.getAddress(TEXT_LOW));
            RegisterFile.reset();
            Coprocessor1.reset();
            Coprocessor0.reset();
            return true;
        }
        else {
            return false;
        }
    }
}