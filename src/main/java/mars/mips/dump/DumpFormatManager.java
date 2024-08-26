package mars.mips.dump;

import mars.util.FilenameFinder;
import mars.venus.ToolManager;

import java.lang.reflect.Modifier;
import java.util.*;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * This class provides functionality to bring external memory dump format definitions
 * into MARS.  This is adapted from the {@link ToolManager} class, which is in turn adapted
 * from Bret Barker's GameServer class from the book "Developing Games In Java".
 */
public class DumpFormatManager {
    private static final String DUMP_PACKAGE_PREFIX = "mars.mips.dump.";
    private static final String DUMP_DIRECTORY_PATH = "mars/mips/dump";
    private static final String CLASS_EXTENSION = "class";

    private static Map<String, DumpFormat> dumpFormats = null;

    // Prevent instances
    private DumpFormatManager() {}

    /**
     * Get the list of dump formats to use, loading them if necessary. The loader searches for all classes
     * in the <code>mars.mips.dump</code> package that implement {@link DumpFormat}.
     *
     * @return An array of <code>DumpFormat</code> instances.
     */
    public static DumpFormat[] getDumpFormats() {
        // The map will be populated only the first time this method is called
        if (dumpFormats == null) {
            dumpFormats = new HashMap<>();

            // Grab all class files in the dump package
            List<String> filenames = FilenameFinder.findFilenames(DumpFormatManager.class.getClassLoader(), DUMP_DIRECTORY_PATH, CLASS_EXTENSION);
            Set<String> knownFilenames = new HashSet<>();

            for (String filename : filenames) {
                if (!knownFilenames.add(filename)) {
                    // We've already encountered this file (happens if run in MARS development directory)
                    continue;
                }
                try {
                    // Obtain the class corresponding to the filename
                    String className = DUMP_PACKAGE_PREFIX + filename.substring(0, filename.length() - CLASS_EXTENSION.length() - 1);
                    Class<?> loadedClass = Class.forName(className);
                    // Ensure it is a concrete class implementing the DumpFormat interface
                    if (Modifier.isAbstract(loadedClass.getModifiers()) || Modifier.isInterface(loadedClass.getModifiers()) || !DumpFormat.class.isAssignableFrom(loadedClass)) {
                        continue;
                    }
                    // Obtain an instance of the class
                    DumpFormat format = (DumpFormat) loadedClass.getDeclaredConstructor().newInstance();
                    // Register the dump format with its command descriptor
                    dumpFormats.put(format.getCommandDescriptor(), format);
                }
                catch (Exception exception) {
                    System.err.println(DumpFormatManager.class.getSimpleName() + ": " + exception);
                }
            }
        }

        return dumpFormats.values().toArray(DumpFormat[]::new);
    }

    /**
     * Find the dump format corresponding a given command descriptor, if one exists.
     *
     * @param commandDescriptor The command descriptor of the desired format.
     * @return The dump format object, or null if no format was found.
     */
    public static DumpFormat getDumpFormat(String commandDescriptor) {
        getDumpFormats();
        return dumpFormats.get(commandDescriptor);
    }
}
