package mars.venus;

import mars.tools.MarsTool;
import mars.util.FilenameFinder;
import mars.venus.actions.ToolAction;

import java.lang.reflect.Modifier;
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
 * This class provides functionality to bring external MARS tools into the MARS
 * system by adding them to its Tools menu.  This permits anyone with knowledge
 * of the MARS public interfaces, in particular of the Memory and Register
 * classes, to write applications which can interact with a MIPS program
 * executing under MARS.  The execution is of course simulated.  The
 * private method for loading tool classes is adapted from Bret Barker's
 * GameServer class from the book "Developing Games In Java".
 *
 * @author Pete Sanderson with help from Bret Barker
 * @version August 2005
 */
public class ToolManager {
    private static final String TOOLS_PACKAGE_PREFIX = "mars.tools.";
    private static final String TOOLS_DIRECTORY_PATH = "mars/tools";
    private static final String CLASS_EXTENSION = "class";

    private static List<ToolAction> toolActions = null;

    private ToolManager() {}

    /**
     * Get the list of actions to fill the Tools menu, which may be empty. The loader
     * searches for all classes in the <code>mars.tools</code> package that implement {@link MarsTool}.
     *
     * @return An array of actions, one for each <code>MarsTool</code> discovered.
     */
    /*
     * This method is adapted from the loadGameControllers() method in Bret Barker's
     * GameServer class.  Barker (bret@hypefiend.com) is co-author of the book
     * "Developing Games in Java".  It was demo'ed to me by Otterbein student
     * Chris Dieterle as part of his Spring 2005 independent study of implementing a
     * networked multi-player game playing system.  Thanks Bret and Chris!
     *
     * Bug Fix 25 Feb 06, DPS: method did not recognize tools folder if its
     * absolute pathname contained one or more spaces (e.g. C:\Program Files\mars\tools).
     * Problem was, class loader's getResource method returns a URL, in which spaces
     * are replaced with "%20".  So I added a replaceAll() to change them back.
     *
     * Enhanced 3 Oct 06, DPS: method did not work if running MARS from a JAR file.
     * The array of files returned is null, but the File object contains the name
     * of the JAR file (using toString, not getName).  Extract that name, open it
     * as a ZipFile, get the ZipEntry enumeration, find the class files in the tools
     * folder, then continue as before.
     */
    public static ToolAction[] getToolActions() {
        if (toolActions == null) {
            toolActions = new ArrayList<>();

            // Add any tools stored externally, as listed in Config.properties file.
            // This needs some work, because mars.Globals.getExternalTools() returns
            // whatever is in the properties file entry.  Since the class file will
            // not be located in the mars.tools folder, the loop below will not process
            // it correctly.  Not sure how to create a Class object given an absolute
            // pathname.

            // Grab all class files in the same directory as the tools package
            List<String> filenames = FilenameFinder.findFilenames(ToolManager.class.getClassLoader(), TOOLS_DIRECTORY_PATH, CLASS_EXTENSION);
            Set<String> knownFilenames = new HashSet<>();

            for (String filename : filenames) {
                if (!knownFilenames.add(filename)) {
                    // We've already encountered this file (happens if run in MARS development directory)
                    continue;
                }
                try {
                    // Obtain the class corresponding to the filename
                    String className = TOOLS_PACKAGE_PREFIX + filename.substring(0, filename.length() - CLASS_EXTENSION.length() - 1);
                    Class<?> loadedClass = Class.forName(className);
                    // Ensure it is a concrete class implementing the MarsTool interface
                    if (Modifier.isAbstract(loadedClass.getModifiers()) || Modifier.isInterface(loadedClass.getModifiers()) || !MarsTool.class.isAssignableFrom(loadedClass)) {
                        continue;
                    }
                    // Obtain an instance of the class
                    MarsTool tool = (MarsTool) loadedClass.getDeclaredConstructor().newInstance();
                    // Create an action for the tool and add it to the list
                    toolActions.add(new ToolAction(tool));
                }
                catch (Exception exception) {
                    System.err.println(ToolManager.class.getSimpleName() + ": " + exception);
                }
            }
        }

        return toolActions.toArray(ToolAction[]::new);
    }
}