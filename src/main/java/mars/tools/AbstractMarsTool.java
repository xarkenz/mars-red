package mars.tools;

import mars.Application;
import mars.mips.hardware.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;

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
public abstract class AbstractMarsTool extends JFrame implements MarsTool, Observer {
    protected JDialog dialog;

    /**
     * Descriptive title for title bar provided to constructor.
     */
    private final String title;

    private final int firstMemoryAddress = Memory.dataSegmentBaseAddress;
    private final int lastMemoryAddress = Memory.stackBaseAddress;

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
     * Required {@link MarsTool} method.  It is invoked when the user selects this tool from the Tools menu.
     * This default implementation provides generic definitions for interactively controlling the tool.
     * The generic controls for MarsTools are 3 buttons:  connect/disconnect to MIPS resource (memory and/or
     * registers), reset, and close (exit).  This calls 3 methods that can be defined/overriden in the subclass:
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
        synchronized (Application.MEMORY_AND_REGISTERS_LOCK) { // DPS 23 July 2008
            this.startObserving();
        }
        this.dialog.setContentPane(this.buildContentPane(this.buildMainDisplayArea(), this.buildButtonArea()));
        this.dialog.pack();
        this.dialog.setLocationRelativeTo(Application.getGUI());
        this.dialog.setVisible(true);
        this.initializePostGUI();
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
     * Called when receiving notice of access to MIPS memory or registers.  Default
     * implementation of method required by Observer interface.  This method will filter out
     * notices originating from the MARS GUI or from direct user editing of memory or register
     * displays.  Only notices arising from MIPS program access are allowed in.
     * It then calls two methods to be overridden by the subclass (since they do
     * nothing by default): {@link #processMIPSUpdate(Observable, AccessNotice)}, then {@link #updateDisplay()}.
     *
     * @param resource     The attached MIPS resource.
     * @param accessNotice Information provided by the resource about the access.
     */
    @Override
    public void update(Observable resource, Object accessNotice) {
        if (((AccessNotice) accessNotice).accessIsFromMIPS()) {
            this.processMIPSUpdate(resource, (AccessNotice) accessNotice);
            this.updateDisplay();
        }
    }

    /**
     * Override this method to process a received notice from MIPS Observable (memory or register)
     * It will only be called if the notice was generated as the result of MIPS instruction execution.
     * After this method is complete, the {@link #updateDisplay()} method will be invoked automatically.
     * Does nothing by default.
     */
    protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
        // Do nothing by default
    }

    /**
     * This method is called when tool/app is exited either through the "Close" button or the window's X button.
     * Override it to perform any special cleaning needed.  Does nothing by default.
     */
    protected void handleClose() {
        // Do nothing by default
    }

    /**
     * Add this tool as an Observer of desired MIPS observables (memory and registers).
     * This method is called when the tool is opened by the user.
     * <p>
     * By default, will add as an observer of the entire Data Segment in memory.
     * Override if you want something different.  Note that the Memory methods to add an
     * Observer to memory are flexible (you can register for a range of addresses) but
     * may throw an {@link AddressErrorException} that you need to catch.
     * <p>
     * NOTE: if you do not want to register as an Observer of the entire data segment
     * (starts at address 0x10000000) then override this to either do some alternative
     * or nothing at all.  This method is also overloaded to allow arbitrary memory
     * subrange.
     */
    protected void startObserving() {
        this.startObserving(this.firstMemoryAddress, this.lastMemoryAddress);
    }

    /**
     * Add this tool as an Observer of the specified subrange of MIPS memory.  Note
     * that this method is not invoked automatically like the no-argument version, but
     * if you use this method, you can still take advantage of provided default {@link #stopObserving()}
     * since it will remove the app as a memory observer regardless of the subrange
     * or number of subranges it is registered for.
     *
     * @param firstAddress The first memory address in the range to observe.
     * @param lastAddress  The last memory address in the range to observe, which must be greater than
     *                     or equal to <code>firstAddress</code>.
     */
    protected void startObserving(int firstAddress, int lastAddress) {
        try {
            Memory.getInstance().addObserver(this, firstAddress, lastAddress);
        }
        catch (AddressErrorException exception) {
            JOptionPane.showMessageDialog(this.dialog, "Error connecting to MIPS memory.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Delete this tool as an observer of MIPS observables (memory and registers).
     * This method is called when the tool is closed by the user.
     * <p>
     * By default, will delete as an observer of memory. Override if you want something different.
     */
    protected void stopObserving() {
        Memory.getInstance().deleteObserver(this);
    }

    /**
     * Override this method to implement updating of GUI after each MIPS instruction is executed,
     * while running in "timed" mode (user specifies execution speed on the slider control). Does nothing by default.
     */
    protected void updateDisplay() {
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
     * Close the tool's dialog and disconnect the tool.
     */
    public void closeTool() {
        this.handleClose();
        synchronized (Application.MEMORY_AND_REGISTERS_LOCK) { // DPS 23 July 2008
            this.stopObserving();
        }
        this.dialog.setVisible(false);
        this.dialog.dispose();
    }
}