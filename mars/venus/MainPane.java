package mars.venus;

import mars.Globals;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
 * Creates the tabbed areas in the UI and also creates the internal windows that
 * exist in them.
 *
 * @author Sanderson and Bumgarner
 */
public class MainPane extends JTabbedPane {
    private final EditTabbedPane editTabbedPane;
    private final ExecutePane executeTab;

    /**
     * Constructor for the MainPane class.
     */
    public MainPane(VenusUI appFrame, Editor editor, RegistersWindow regs, Coprocessor1Window cop1Regs, Coprocessor0Window cop0Regs) {
        super();
        editTabbedPane = new EditTabbedPane(appFrame, editor, this);
        executeTab = new ExecutePane(appFrame, regs, cop1Regs, cop0Regs);

        String editTabTitle = "Edit";
        String executeTabTitle = "Execute";
        // TODO: Maybe new icons designed for the top tabs could look decent? -Sean Clarke
        Icon editTabIcon = null; // new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Globals.IMAGES_PATH + "Edit_tab.jpg")));
        Icon executeTabIcon = null; // new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Globals.IMAGES_PATH + "Execute_tab.jpg")));

        this.setTabPlacement(JTabbedPane.TOP);
        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.addTab(editTabTitle, editTabIcon, editTabbedPane, "Text editor for writing MIPS programs.");
        this.addTab(executeTabTitle, executeTabIcon, executeTab, "View and control assembly language program execution. Enabled upon successful assemble.");

        // Listener has one specific purpose: when Execute tab is selected for the
        // * first time, set the bounds of its internal frames by invoking the
        // * setWindowsBounds() method.  Once this occurs, listener removes itself!
        // * We do NOT want to reset bounds each time Execute tab is selected.
        // * See ExecutePane.setWindowsBounds documentation for more details.
        this.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                JTabbedPane tabbedPane = (JTabbedPane) ce.getSource();
                int index = tabbedPane.getSelectedIndex();
                Component c = tabbedPane.getComponentAt(index);
                ExecutePane executePane = MainPane.this.getExecutePane();
                if (c == executePane) {
                    executePane.setWindowBounds();
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
    public EditPane getEditPane() {
        return editTabbedPane.getCurrentEditTab();
    }

    /**
     * Returns component containing editor display
     *
     * @return the editor tabbed pane
     */
    public JComponent getEditTabbedPane() {
        return editTabbedPane;
    }

    /**
     * returns component containing execution-time display
     *
     * @return the execute pane
     */
    public ExecutePane getExecutePane() {
        return executeTab;
    }

    /**
     * returns component containing execution-time display.
     * Same as getExecutePane().
     *
     * @return the execute pane
     */
    public ExecutePane getExecuteTab() {
        return executeTab;
    }
}