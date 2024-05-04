package mars.venus.execute;

import mars.Application;
import mars.venus.*;

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
 * The "Execute" tab in the main tabbed pane, which contains execution-related windows.
 *
 * @author Pete Sanderson and Team JSpim
 */
public class ExecuteTab extends JDesktopPane {
    private final VenusUI gui;
    private final DataSegmentWindow dataSegment;
    private final TextSegmentWindow textSegment;
    private final SymbolTableWindow labelValues;
    private final NumberDisplayBaseChooser valueDisplayBase;
    private final NumberDisplayBaseChooser addressDisplayBase;
    private ProgramStatus programStatus;
    private boolean labelWindowVisible;

    /**
     * Initialize the Execute pane.
     *
     * @param gui The parent GUI instance.
     */
    public ExecuteTab(VenusUI gui) {
        this.gui = gui;
        // Although these are displayed in Data Segment, they apply to all three internal
        // windows within the Execute pane.  So they will be housed here.
        addressDisplayBase = new NumberDisplayBaseChooser("Hexadecimal Addresses", Application.getSettings().displayAddressesInHex.get());
        valueDisplayBase = new NumberDisplayBaseChooser("Hexadecimal Values", Application.getSettings().displayValuesInHex.get());
        addressDisplayBase.setToolTipText("If checked, displays all memory addresses in hexadecimal.  Otherwise, decimal.");
        valueDisplayBase.setToolTipText("If checked, displays all memory and register contents in hexadecimal.  Otherwise, decimal.");
        textSegment = new TextSegmentWindow();
        labelValues = new SymbolTableWindow();
        dataSegment = new DataSegmentWindow(new NumberDisplayBaseChooser[] { addressDisplayBase, valueDisplayBase });
        labelWindowVisible = Application.getSettings().labelWindowVisible.get();
        programStatus = ProgramStatus.NOT_ASSEMBLED;

        this.add(textSegment); // these 3 LOC moved up.  DPS 3-Sept-2014
        this.add(dataSegment);
        this.add(labelValues);
        textSegment.pack(); // these 3 LOC added.  DPS 3-Sept-2014
        dataSegment.pack();
        labelValues.pack();
        textSegment.setVisible(true);
        dataSegment.setVisible(true);
        labelValues.setVisible(labelWindowVisible);
    }

    /**
     * This method will set the bounds of this JDesktopPane's internal windows
     * relative to the current size of this JDesktopPane.  Such an operation
     * cannot be adequately done at constructor time because the actual
     * size of the desktop pane window is not yet established.  Layout manager
     * is not a good option here because JDesktopPane does not work well with
     * them (the whole idea of using JDesktopPane with internal frames is to
     * have mini-frames that you can resize, move around, minimize, etc).  This
     * method should be invoked only once: the first time the Execute tab is
     * selected (a change listener invokes it).  We do not want it invoked
     * on subsequent tab selections; otherwise, user manipulations of the
     * internal frames would be lost the next time execute tab is selected.
     */
    public void setWindowBounds() {
        int fullWidth = this.getSize().width - this.getInsets().left - this.getInsets().right;
        int fullHeight = this.getSize().height - this.getInsets().top - this.getInsets().bottom;
        int halfHeight = fullHeight / 2;
        Dimension textDim = new Dimension((int) (fullWidth * .75), halfHeight);
        Dimension dataDim = new Dimension(fullWidth, halfHeight);
        Dimension labelDim = new Dimension((int) (fullWidth * .25), halfHeight);
        Dimension textFullDim = new Dimension(fullWidth, halfHeight);
        dataSegment.setBounds(0, textDim.height + 1, dataDim.width, dataDim.height);
        if (labelWindowVisible) {
            textSegment.setBounds(0, 0, textDim.width, textDim.height);
            labelValues.setBounds(textDim.width + 1, 0, labelDim.width, labelDim.height);
        }
        else {
            textSegment.setBounds(0, 0, textFullDim.width, textFullDim.height);
            labelValues.setBounds(0, 0, 0, 0);
        }
    }

    /**
     * Show or hide the label window (symbol table).  If visible, it is displayed
     * to the right of the text segment and the latter is shrunk accordingly.
     *
     * @param visibility set to true or false
     */
    public void setSymbolTableWindowVisibility(boolean visibility) {
        if (!visibility && labelWindowVisible) {
            labelWindowVisible = false;
            textSegment.setVisible(false);
            labelValues.setVisible(false);
            this.setWindowBounds();
            textSegment.setVisible(true);
        }
        else if (visibility && !labelWindowVisible) {
            labelWindowVisible = true;
            textSegment.setVisible(false);
            this.setWindowBounds();
            textSegment.setVisible(true);
            labelValues.setVisible(true);
        }
    }

    /**
     * Clears out all components of the Execute tab: text segment
     * display, data segment display, label display and register display.
     * This will typically be done upon File->Close, Open, New.
     */
    public void clear() {
        this.getTextSegmentWindow().clearWindow();
        this.getDataSegmentWindow().clearWindow();
        this.getLabelsWindow().clearWindow();
        gui.getRegistersPane().getRegistersWindow().clearWindow();
        gui.getRegistersPane().getCoprocessor1Window().clearWindow();
        gui.getRegistersPane().getCoprocessor0Window().clearWindow();
        // Seems to be required in order to display cleared Execute tab contents...
        if (gui.getMainPane().getSelectedComponent() == this) {
            gui.getMainPane().setSelectedComponent(gui.getMainPane().getEditTab());
            gui.getMainPane().setSelectedComponent(this);
        }
    }

    /**
     * Get the current status of the overall program.
     *
     * @return The current program status.
     */
    public ProgramStatus getProgramStatus() {
        return this.programStatus;
    }

    /**
     * Set the status of the overall program, and update the main GUI menu state.
     *
     * @param status The new program status.
     */
    public void setProgramStatus(ProgramStatus status) {
        this.programStatus = status;
        this.gui.updateMenuState(status);
    }

    /**
     * Access the text segment window.
     */
    public TextSegmentWindow getTextSegmentWindow() {
        return textSegment;
    }

    /**
     * Access the data segment window.
     */
    public DataSegmentWindow getDataSegmentWindow() {
        return dataSegment;
    }

    /**
     * Access the label values window.
     */
    public SymbolTableWindow getLabelsWindow() {
        return labelValues;
    }

    /**
     * Retrieve the number system base for displaying values (mem/register contents)
     */
    public int getValueDisplayBase() {
        return valueDisplayBase.getBase();
    }

    /**
     * Retrieve the number system base for displaying memory addresses
     */
    public int getAddressDisplayBase() {
        return addressDisplayBase.getBase();
    }

    /**
     * Retrieve component used to set numerical base (10 or 16) of data value display.
     *
     * @return the chooser
     */
    public NumberDisplayBaseChooser getValueDisplayBaseChooser() {
        return valueDisplayBase;
    }

    /**
     * Retrieve component used to set numerical base (10 or 16) of address display.
     *
     * @return the chooser
     */
    public NumberDisplayBaseChooser getAddressDisplayBaseChooser() {
        return addressDisplayBase;
    }

    /**
     * Update display of columns based on state of given chooser.  Normally
     * called only by the chooser's ItemListener.
     *
     * @param chooser the GUI object manipulated by the user to change number base
     */
    public void numberDisplayBaseChanged(NumberDisplayBaseChooser chooser) {
        if (chooser == valueDisplayBase) {
            // Have all internal windows update their value columns
            gui.getRegistersPane().getRegistersWindow().updateRegisters();
            gui.getRegistersPane().getCoprocessor0Window().updateRegisters();
            gui.getRegistersPane().getCoprocessor1Window().updateRegisters();
            dataSegment.updateValues();
            textSegment.updateBasicStatements();
        }
        else { // chooser == addressDisplayBase
            // Have all internal windows update their address columns
            dataSegment.updateDataAddresses();
            labelValues.updateLabelAddresses();
            textSegment.updateCodeAddresses();
            textSegment.updateBasicStatements();
        }
    }
}