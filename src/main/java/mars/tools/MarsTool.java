package mars.tools;

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

import javax.swing.*;

/**
 * Interface for any tool that interacts with an executing MIPS program.
 * A qualifying tool must be a class in the Tools package that
 * implements the MarsTool interface, must be compiled into a .class file,
 * and its .class file must be in the same Tools folder as MarsTool.class.
 * Mars will detect a qualifying tool upon startup, create an instance
 * using its no-argument constructor and add it to its Tools menu.
 * When its menu item is selected, the {@link #action()} method will be invoked.
 * <p>
 * A tool may receive communication from MIPS system resources
 * (registers or memory) by registering as an observer with
 * {@link mars.mips.hardware.Memory} and/or {@link mars.mips.hardware.Register} objects.
 * <p>
 * It may also communicate directly with those resources through their published methods,
 * provided any such communication is done using {@link mars.simulator.Simulator#changeState(Runnable)}.
 */
public interface MarsTool {
    /**
     * Return the name you have chosen for this tool, which will be used for the
     * menu item.  If not implemented, uses the class name.
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Return the icon you have chosen for this tool, which will be used for the
     * menu item.  If not implemented, no icon is displayed.
     */
    default Icon getIcon() {
        return null;
    }

    /**
     * Return the tool menu order number. Tools in the tool menu list are sorted
     * by this number, then alphabetically - to get your tool at the top, choose
     * a lesser number. Likewise, to get your tool at the bottom, choose a
     * greater number.
     */
    default int getToolMenuOrder() {
        return 0;
    }

    /**
     * Performs tool functions.  It will be invoked when the tool is selected
     * from the Tools menu.
     */
    void action();
}