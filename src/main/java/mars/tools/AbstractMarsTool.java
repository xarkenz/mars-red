package mars.tools;

import mars.Application;
import mars.mips.hardware.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
 * An abstract class that provides generic components to facilitate implementation of
 * a MarsTool and/or stand-alone Mars-based application.  Provides default definitions
 * of both the action() method required to implement MarsTool and the go() method
 * conventionally used to launch a Mars-based stand-alone application. It also provides
 * generic definitions for interactively controlling the application.  The generic controls
 * for MarsTools are 3 buttons:  connect/disconnect to MIPS resource (memory and/or
 * registers), reset, and close (exit).  The generic controls for stand-alone Mars apps
 * include: button that triggers a file open dialog, a text field to display status
 * messages, the run-speed slider to control execution rate when running a MIPS program,
 * a button that assembles and runs the current MIPS program, a button to interrupt
 * the running MIPS program, a reset button, and an exit button.
 *
 * @author Pete Sanderson, 14 November 2006
 */
public abstract class AbstractMarsTool extends JFrame implements MarsTool, Memory.Listener {
    protected JDialog dialog;

    /**
     * Descriptive title for title bar provided to constructor.
     */
    private final String title;

    /**
     * Simple constructor
     *
     * @param title String containing title bar text
     */
    protected AbstractMarsTool(String title) {
        this.title = title;
    }

    /**
     * Required MarsTool method to return Tool name.  Must be defined by subclass.
     *
     * @return Tool name.  MARS will display this in menu item.
     */
    @Override
    public abstract String getName();

    /**
     * Abstract method that must be instantiated by subclass to build the main display area
     * of the GUI.  It will be placed in the CENTER area of a BorderLayout.  The title
     * is in the NORTH area, and the controls are in the SOUTH area.
     */
    protected abstract JComponent buildMainDisplayArea();

    /**
     * This is invoked when the user selects this tool from the Tools menu.
     * This default implementation provides generic definitions for interactively controlling the tool.
     * The generic controls for MarsTools are 3 buttons:  reset, help, and close.
     * <p>
     * This calls 3 methods that can be defined/overriden in the subclass:
     * {@link #initializePreGUI()} for any special initialization that must be completed before building the user
     * interface (e.g. data structures whose properties determine default GUI settings),
     * {@link #initializePostGUI()} for any special initialization that cannot be completed until after
     * building the user interface (e.g. data structure whose properties are determined by default GUI settings),
     * and {@link #buildMainDisplayArea()} to contain application-specific displays of parameters and results.
     */
    @Override
    public void action() {
        this.dialog = new JDialog(Application.getGUI(), this.title);
        // Ensure the dialog goes away if the user clicks the X
        this.dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                AbstractMarsTool.this.closeTool();
            }
        });
        this.initializePreGUI();
        this.dialog.setContentPane(this.buildContentPane(this.buildMainDisplayArea(), this.buildButtonArea()));
        this.dialog.pack();
        this.dialog.setLocationRelativeTo(Application.getGUI());
        this.dialog.setVisible(true);
        this.initializePostGUI();
        this.startObserving();
    }

    /**
     * Method that will be called once just before the GUI is constructed in the go() and action()
     * methods.  Use it to initialize any data structures needed for the application whose values
     * will be needed to determine the initial state of GUI components.  Does nothing by default.
     */
    protected void initializePreGUI() {
        // Do nothing by default
    }

    /**
     * Method that will be called once just after the GUI is constructed in the go() and action()
     * methods.  Use it to initialize data structures needed for the application whose values
     * may depend on the initial state of GUI components.  Does nothing by default.
     */
    protected void initializePostGUI() {
        // Do nothing by default
    }

    /**
     * Method that will be called each time the default Reset button is clicked.
     * Use it to reset any data structures and/or GUI components.  Does nothing by default.
     */
    protected void reset() {
        // Do nothing by default
    }

    protected JComponent buildContentPane(JComponent mainDisplayArea, JComponent buttonArea) {
        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contentPane.setOpaque(true);
        contentPane.add(mainDisplayArea, BorderLayout.CENTER);
        contentPane.add(buttonArea, BorderLayout.SOUTH);
        return contentPane;
    }

    /**
     * The MarsTool default set of controls has one row of 3 buttons.  It includes a dual-purpose button to
     * attach or detach simulator to MIPS memory, a button to reset the cache, and one to close the tool.
     */
    protected JComponent buildButtonArea() {
        JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText("Reset all counters and other structures");
        resetButton.addActionListener(event -> this.reset());

        JButton closeButton = new JButton("Close");
        closeButton.setToolTipText("Close this tool");
        closeButton.addActionListener(event -> this.closeTool());

        // Add all the buttons
        Box buttonArea = Box.createHorizontalBox();
        buttonArea.add(resetButton);
        buttonArea.add(Box.createHorizontalGlue());
        JComponent helpComponent = this.getHelpComponent();
        if (helpComponent != null) {
            buttonArea.add(helpComponent);
            buttonArea.add(Box.createHorizontalGlue());
        }
        buttonArea.add(closeButton);
        return buttonArea;
    }

    /**
     * This method is called when tool/app is exited either through the "Close" button or the window's X button.
     * Override it to perform any special cleaning needed.  Does nothing by default.
     */
    protected void handleClose() {
        // Do nothing by default
    }

    /**
     * Register this tool as a listener of memory and/or registers, if applicable.
     * This method is called when the tool is opened by the user. Does nothing by default.
     *
     * @see Register#addListener(Register.Listener)
     * @see Memory#addListener(Memory.Listener)
     * @see Memory#addListener(Memory.Listener, int)
     * @see Memory#addListener(Memory.Listener, int, int)
     */
    protected void startObserving() {
        // Do nothing by default
    }

    /**
     * Unregister this tool as a listener of memory and/or registers, if applicable.
     * This method is called when the tool is closed by the user. Does nothing by default.
     *
     * @see Register#removeListener(Register.Listener)
     * @see Memory#removeListener(Memory.Listener)
     */
    protected void stopObserving() {
        // Do nothing by default
    }

    /**
     * Override this method to provide a JComponent (probably a JButton) of your choice
     * to be placed just left of the Close/Exit button.  Its anticipated use is for a
     * "help" button that launches a help message or dialog.  But it can be any valid
     * JComponent that doesn't mind co-existing among a bunch of JButtons.
     */
    protected JComponent getHelpComponent() {
        return null;
    }

    /**
     * Disconnect the tool from registers/memory and close the tool's dialog.
     * This calls {@link #stopObserving()}, then {@link #handleClose()}, then closes the actual dialog.
     */
    public void closeTool() {
        this.stopObserving();
        this.handleClose();
        this.dialog.setVisible(false);
        this.dialog.dispose();
    }
}