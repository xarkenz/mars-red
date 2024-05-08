package mars.venus;

import mars.FileDrop;
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
    public MainPane(VenusUI gui, Editor editor) {
        super();
        this.editTab = new EditTab(gui, editor);
        this.executeTab = new ExecuteTab(gui);

        this.setTabPlacement(JTabbedPane.TOP);
        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.addTab("Edit", null, this.editTab, "Text editor for writing MIPS programs.");
        this.addTab("Execute", null, this.executeTab, "View and control assembly language program execution. Enabled upon successful assemble.");

        // Listener has one specific purpose: when Execute tab is selected for the
        // first time, set the bounds of its internal frames by invoking the
        // setWindowsBounds() method.  Once this occurs, listener removes itself!
        // We do NOT want to reset bounds each time Execute tab is selected.
        // See ExecutePane.setWindowsBounds documentation for more details.
        this.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                if (MainPane.this.getSelectedComponent() == MainPane.this.executeTab) {
                    MainPane.this.executeTab.setWindowBounds();
                    MainPane.this.removeChangeListener(this);
                }
            }
        });

        // Enable file drag and drop functionality
        new FileDrop(this, this.editTab::openFiles);
    }

    /**
     * Returns the current editor tab selected in the "Edit" tab.
     *
     * @return The current editor tab.
     */
    public FileEditorTab getCurrentEditorTab() {
        return this.editTab.getCurrentEditorTab();
    }

    /**
     * Returns component containing editor display.
     *
     * @return The component for the "Edit" tab.
     */
    public EditTab getEditTab() {
        return this.editTab;
    }

    /**
     * Returns component containing execution-time display.
     *
     * @return The component for the "Execute" tab.
     */
    public ExecuteTab getExecuteTab() {
        return this.executeTab;
    }
}