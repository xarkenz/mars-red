package mars.mips.instructions;

import mars.mips.instructions.syscalls.Syscall;
import mars.util.FilenameFinder;
import mars.util.PropertiesFile;

import java.util.*;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * This class provides functionality to bring external Syscall definitions
 * into MARS.  This permits anyone with knowledge of the Mars public interfaces,
 * in particular of the Memory and Register classes, to write custom MIPS syscall
 * functions. This is adapted from the {@link mars.venus.ToolLoader} class, which is in turn adapted
 * from Bret Barker's GameServer class from the book "Developing Games In Java".
 */
public class SyscallManager {
    private static final String SYSCALL_OVERRIDES_PATH = "Syscalls";
    private static final String CLASS_PREFIX = "mars.mips.instructions.syscalls.";
    private static final String SYSCALLS_DIRECTORY_PATH = "mars/mips/instructions/syscalls";
    private static final String SYSCALL_INTERFACE = "Syscall.class";
    private static final String SYSCALL_ABSTRACT = "AbstractSyscall.class";
    private static final String CLASS_EXTENSION = "class";

    private Map<Integer, Syscall> syscallTable = null;

    /**
     * Dynamically load syscall classes from the {@code mars.mips.instructions.syscalls} package
     * so they can be accessed via {@link #findSyscall(int)}.
     * Syscall overrides specified in the application preferences are also processed at this time.
     */
    public void loadSyscalls() {
        this.syscallTable = new HashMap<>();
        // Grab all class files in the same directory as Syscall
        List<String> filenames = FilenameFinder.getFilenameList(this.getClass().getClassLoader(), SYSCALLS_DIRECTORY_PATH, CLASS_EXTENSION);
        Set<String> knownFilenames = new HashSet<>();
        Map<String, Integer> overrides = getSyscallOverrides();

        for (String filename : filenames) {
            // Do not process class if already encountered (happens if run in MARS development directory)
            if (knownFilenames.add(filename) && !filename.equals(SYSCALL_INTERFACE) && !filename.equals(SYSCALL_ABSTRACT)) {
                try {
                    // Obtain the class, ensuring it implements the Syscall interface
                    String className = CLASS_PREFIX + filename.substring(0, filename.lastIndexOf(CLASS_EXTENSION) - 1);
                    Class<?> loadedClass = Class.forName(className);
                    if (!Syscall.class.isAssignableFrom(loadedClass)) {
                        continue;
                    }
                    // Obtain an instance of the class
                    Syscall syscall = (Syscall) loadedClass.getDeclaredConstructor().newInstance();

                    // Check for a service number override, consuming the override if it exists
                    Integer overrideNumber = overrides.remove(syscall.getName());
                    if (overrideNumber != null) {
                        // The service number has been overridden by syscall configuration
                        syscall.setNumber(overrideNumber);
                    }

                    // Add the syscall to the table
                    Syscall existingSyscall = this.syscallTable.put(syscall.getNumber(), syscall);
                    if (existingSyscall != null) {
                        throw new Exception("syscall service number " + syscall.getNumber() + " is assigned to both '" + existingSyscall.getName() + "' and '" + syscall.getName() + "'");
                    }
                }
                catch (Exception exception) {
                    System.err.println(this.getClass().getSimpleName() + ": " + exception);
                }
            }
        }

        // If there are any entries left in the overrides map, they did not correspond to any syscall
        for (String unknownName : overrides.keySet()) {
            System.err.println(SYSCALL_OVERRIDES_PATH + ".properties: unrecognized syscall '" + unknownName + "'");
        }
    }

    /**
     * Find the syscall corresponding a given service number, if one exists.
     *
     * @param number The service number of the desired syscall.
     * @return The syscall object, or null if no syscall was found.
     */
    public Syscall findSyscall(int number) {
        if (this.syscallTable == null) {
            this.loadSyscalls();
        }

        return this.syscallTable.get(number);
    }

    /**
     * Read all syscall number overrides from the syscall configuration file.
     *
     * @return Mapping from syscall names to service numbers.
     */
    private static Map<String, Integer> getSyscallOverrides() {
        Properties properties = PropertiesFile.loadPropertiesFromFile(SYSCALL_OVERRIDES_PATH);

        Map<String, Integer> overrides = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = entry.getKey().toString();
            try {
                Integer number = Integer.decode(entry.getValue().toString());
                if (overrides.put(name, number) != null) {
                    System.err.println(SYSCALL_OVERRIDES_PATH + ".properties: duplicate entries for syscall '" + name + "'");
                }
            }
            catch (NumberFormatException exception) {
                System.err.println(SYSCALL_OVERRIDES_PATH + ".properties: invalid service number for syscall '" + name + "': " + entry.getValue());
            }
        }

        return overrides;
    }
}
