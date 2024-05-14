package mars.tools;

import javax.swing.*;
import java.awt.*;

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
 * The "hello world" of MarsTools!
 */
public class IntroToTools extends AbstractMarsTool {
    private static final String NAME = "Introduction to MARS Tools";
    private static final String VERSION = "Version 1.0";

    /**
     * Construct an instance of this tool. This will be used by the {@link mars.venus.ToolManager}.
     */
    @SuppressWarnings("unused")
    public IntroToTools() {
        super(NAME + ", " + VERSION);
    }

    /**
     * Required method to return Tool name.
     *
     * @return Tool name.  MARS will display this in menu item.
     */
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getToolMenuOrder() {
        // Put tool at the top of the tool list
        return -1;
    }

    /**
     * Implementation of the inherited abstract method to build the main
     * display area of the GUI.  It will be placed in the CENTER area of a
     * BorderLayout.  The title is in the NORTH area, and the controls are
     * in the SOUTH area.
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        JTextArea message = new JTextArea();
        message.setEditable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setPreferredSize(new Dimension(500, 400));
        message.setText("""
            Hello!  This Tool does not do anything but you may use its source code as a starting point to build your own MARS Tool.

            A MARS Tool is a program listed in the MARS Tools menu.  It is launched when you select its menu item and typically interacts with executing MIPS programs to do something exciting and informative or at least interesting.

            The basic requirements for building a MARS Tool are:
              1. It must be a class that implements the MarsTool interface.  This has only two methods: 'String getName()' to return the name to be displayed in its Tools menu item, and 'void action()' which is invoked when that menu item is selected by the MARS user.
              2. It must be stored in the mars.tools package (in folder mars/tools)
              3. It must be successfully compiled in that package.  This normally means the MARS distribution needs to be extracted from the JAR file before you can develop your Tool.

            If these requirements are met, MARS will recognize and load your Tool into its Tools menu the next time it runs.

            The frame/dialog you are reading right now is an example of an AbstractMarsTool subclass.

            See the mars.tools.AbstractMarsTool API or the source code of existing tool/apps for further information.
            """);
        message.setCaretPosition(0); // Assure first line is visible and at top of scroll pane
        return new JScrollPane(message);
    }
}