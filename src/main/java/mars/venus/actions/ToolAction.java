package mars.venus.actions;

import mars.tools.MarsTool;

import javax.swing.*;
import java.awt.event.ActionEvent;

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
 * Connects a MarsTool class (class that implements MarsTool interface) to
 * the Mars menu system by supplying the response to that tool's menu item
 * selection.
 *
 * @author Pete Sanderson
 * @version August 2005
 */
public class ToolAction extends AbstractAction {
    private final MarsTool tool;

    /**
     * Create a new <code>ToolAction</code> for the specified {@link MarsTool}.
     *
     * @param tool The tool this action will open when performed.
     */
    public ToolAction(MarsTool tool) {
        super(tool.getDisplayName(), tool.getIcon());
        this.tool = tool;
    }

    /**
     * Get the {@link MarsTool} this action corresponds to.
     *
     * @return The tool this action will open when performed.
     */
    public MarsTool getTool() {
        return this.tool;
    }

    /**
     * Opens this action's corresponding tool by invoking its {@link MarsTool#launch()} method.
     *
     * @param event The event that triggered this call.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            this.tool.launch();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}