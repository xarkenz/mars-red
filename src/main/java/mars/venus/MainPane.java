package mars.venus;

import mars.venus.editor.EditTab;
import mars.venus.editor.Editor;
import mars.venus.editor.FileEditorTab;
import mars.venus.execute.ExecuteTab;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
 * Creates the tabbed areas in the UI and also creates the internal windows that
 * exist in them.
 *
 * @author Sanderson and Bumgarner
 */
public class MainPane extends JTabbedPane {
    private final EditTab editTab;
    private final ExecuteTab executeTab;

    /**
     * Constructor for the MainPane class.
     */
    public MainPane(VenusUI mainUI, Editor editor, RegistersWindow regs, Coprocessor1Window cop1Regs, Coprocessor0Window cop0Regs) {
        super();
        this.editTab = new EditTab(mainUI, editor);
        this.executeTab = new ExecuteTab(mainUI, regs, cop1Regs, cop0Regs);

        // TODO: Maybe new icons designed for the top tabs could look decent? -Sean Clarke
        this.setTabPlacement(JTabbedPane.TOP);
        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.addTab("Edit", null, editTab, "Text editor for writing MIPS programs.");
        this.addTab("Execute", null, executeTab, "View and control assembly language program execution. Enabled upon successful assemble.");

        // Listener has one specific purpose: when Execute tab is selected for the
        // * first time, set the bounds of its internal frames by invoking the
        // * setWindowsBounds() method.  Once this occurs, listener removes itself!
        // * We do NOT want to reset bounds each time Execute tab is selected.
        // * See ExecutePane.setWindowsBounds documentation for more details.
        this.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                if (MainPane.this.getSelectedComponent() == executeTab) {
                    executeTab.setWindowBounds();
                    MainPane.this.removeChangeListener(this);
                }
            }
        });
    }

    /**
     * Returns current edit pane.  Implementation changed for MARS 4.0 support
     * for multiple panes, but specification is same.
     *
     * @return the editor pane
     */
    public FileEditorTab getCurrentEditorTab() {
        return editTab.getCurrentEditorTab();
    }

    /**
     * Returns component containing editor display.
     *
     * @return The component for the "Edit" tab.
     */
    public EditTab getEditTab() {
        return editTab;
    }

    /**
     * Returns component containing execution-time display.
     *
     * @return The component for the "Execute" tab.
     */
    public ExecuteTab getExecuteTab() {
        return executeTab;
    }
}