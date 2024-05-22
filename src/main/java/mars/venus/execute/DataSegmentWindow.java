package mars.venus.execute;

import mars.Application;
import mars.mips.hardware.*;
import mars.simulator.*;
import mars.util.Binary;
import mars.venus.MonoRightCellRenderer;
import mars.venus.NumberDisplayBaseChooser;
import mars.venus.RepeatButton;
import mars.venus.VenusUI;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;

/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

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
 * Represents the Data Segment window, which is a type of JInternalFrame.
 *
 * @author Sanderson and Bumgarner
 */
public class DataSegmentWindow extends JInternalFrame implements SimulatorListener, Memory.Listener {
    private final VenusUI gui;
    private Object[][] tableData;
    private JTable table;
    private JScrollPane tableScrollPane;
    private final Container contentPane;
    private final JPanel tablePanel;
    private JButton nextButton;
    private JButton prevButton;

    public static final int VALUES_PER_ROW = 8;
    public static final int ROW_COUNT = 16; // with 8 value columns, this shows 512 bytes
    public static final int COLUMN_COUNT = 1 + VALUES_PER_ROW; // 1 for address and 8 for values
    public static final int BYTES_PER_VALUE = Memory.BYTES_PER_WORD;
    public static final int BYTES_PER_ROW = VALUES_PER_ROW * BYTES_PER_VALUE;
    public static final int MEMORY_CHUNK_SIZE = ROW_COUNT * BYTES_PER_ROW;
    // PREV_NEXT_CHUNK_SIZE determines how many rows will be scrolled when Prev or Next buttons fire.
    // MEMORY_CHUNK_SIZE / 2 means scroll half a table up or down.  Easier to view series that flows off the edge.
    // MEMORY_CHUNK_SIZE means scroll a full table's worth.  Scrolls through memory faster.  DPS 26-Jan-09
    public static final int PREV_NEXT_CHUNK_SIZE = MEMORY_CHUNK_SIZE / 2;
    public static final int ADDRESS_COLUMN = 0;
    public static final boolean USER_MODE = false;
    public static final boolean KERNEL_MODE = true;

    private boolean addressHighlighting;
    private boolean asciiDisplay = false;
    private int addressRowFirstAddress;
    private int addressColumn;

    private int firstAddress;
    private int homeAddress;
    private boolean userOrKernelMode;

    // The combo box replaced the row of buttons when number of buttons expanded to 7!
    // We'll keep the button objects however and manually invoke their action listeners
    // when the corresponding combo box item is selected.  DPS 22-Nov-2006
    JComboBox<String> baseAddressSelector;
    private int defaultBaseAddressIndex;
    private final String[] baseAddressChoices;
    private final JButton[] baseAddressButtons;

    // Arrays used with Base Address combo box chooser.
    // The combo box replaced the row of buttons when number of buttons expanded to 7!
    private static final int EXTERN_BASE_ADDRESS_INDEX = 0;
    private static final int DATA_BASE_ADDRESS_INDEX = 1;
    private static final int HEAP_BASE_ADDRESS_INDEX = 2;
    private static final int GLOBAL_POINTER_BASE_ADDRESS_INDEX = 3;
    private static final int STACK_POINTER_BASE_ADDRESS_INDEX = 4;
    private static final int TEXT_BASE_ADDRESS_INDEX = 5;
    private static final int KERNEL_DATA_BASE_ADDRESS_INDEX = 6;
    private static final int MMIO_BASE_ADDRESS_INDEX = 7;
    // Must agree with above in number and order...
    private final int[] baseAddresses = {
        Memory.getInstance().getAddress(MemoryConfigurations.EXTERN_LOW),
        Memory.getInstance().getAddress(MemoryConfigurations.STATIC_LOW),
        Memory.getInstance().getAddress(MemoryConfigurations.DYNAMIC_LOW),
        -1, // Global pointer placeholder
        -1, // Stack pointer placeholder
        Memory.getInstance().getAddress(MemoryConfigurations.TEXT_LOW),
        Memory.getInstance().getAddress(MemoryConfigurations.KERNEL_DATA_LOW),
        Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW),
    };
    // Must agree with above in number and order...
    private static final String[] LOCATION_DESCRIPTIONS = {
        ".extern",
        ".data",
        "heap",
        "current $gp",
        "current $sp",
        ".text",
        ".kdata",
        "MMIO",
    };

    /**
     * Constructor for the Data Segment window.
     *
     * @param choosers an array of objects used by user to select number display base (10 or 16)
     */
    public DataSegmentWindow(VenusUI gui, NumberDisplayBaseChooser[] choosers) {
        super("Memory Viewer", true, false, true, false);
        this.setFrameIcon(null);

        this.gui = gui;
        this.homeAddress = this.baseAddresses[DATA_BASE_ADDRESS_INDEX];
        this.firstAddress = homeAddress;
        this.userOrKernelMode = USER_MODE;
        this.addressHighlighting = false;
        this.baseAddressChoices = new String[this.baseAddresses.length];
        this.baseAddressButtons = new JButton[this.baseAddresses.length];
        this.contentPane = this.getContentPane();
        this.tablePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel navigationBar = new JPanel();
        try {
            this.prevButton = new PrevButton(new ImageIcon(getToolkit().getImage(getClass().getResource(Application.IMAGES_PATH + "Previous22.png"))));
            this.nextButton = new NextButton(new ImageIcon(getToolkit().getImage(getClass().getResource(Application.IMAGES_PATH + "Next22.png"))));
            // This group of buttons was replaced by a combo box.  Keep the JButton objects for their action listeners.
            for (int index = 0; index < this.baseAddressButtons.length; index++) {
                this.baseAddressButtons[index] = new JButton();
            }
        }
        catch (NullPointerException exception) {
            System.err.println("Error: images folder not found");
            System.exit(0);
        }

        this.initializeBaseAddressChoices();
        this.baseAddressSelector = new JComboBox<>();
        this.baseAddressSelector.setModel(new CustomComboBoxModel(this.baseAddressChoices));
        this.baseAddressSelector.setEditable(false);
        this.baseAddressSelector.setSelectedIndex(this.defaultBaseAddressIndex);
        this.baseAddressSelector.setToolTipText("Base address for data segment display");
        this.baseAddressSelector.addActionListener(event -> {
            // Trigger action listener for associated invisible button
            this.baseAddressButtons[this.baseAddressSelector.getSelectedIndex()].getActionListeners()[0].actionPerformed(null);
        });

        this.initializeBaseAddressButtons();
        JPanel navButtons = new JPanel(new GridLayout(1, 4));
        navButtons.add(prevButton);
        navButtons.add(nextButton);
        navigationBar.add(navButtons);
        navigationBar.add(baseAddressSelector);
        for (NumberDisplayBaseChooser chooser : choosers) {
            navigationBar.add(chooser);
        }
        JCheckBox asciiDisplayCheckBox = new JCheckBox("ASCII", this.asciiDisplay);
        asciiDisplayCheckBox.setToolTipText("Display data segment values in ASCII (overrides Hexadecimal Values setting)");
        asciiDisplayCheckBox.addItemListener(event -> {
            this.asciiDisplay = (event.getStateChange() == ItemEvent.SELECTED);
            this.updateValues();
        });
        navigationBar.add(asciiDisplayCheckBox);

        this.contentPane.add(navigationBar, BorderLayout.SOUTH);

        Simulator.getInstance().addGUIListener(this);
    }

    public void updateBaseAddressComboBox() {
        this.baseAddresses[EXTERN_BASE_ADDRESS_INDEX] = Memory.getInstance().getAddress(MemoryConfigurations.EXTERN_LOW);
        this.baseAddresses[DATA_BASE_ADDRESS_INDEX] = Memory.getInstance().getAddress(MemoryConfigurations.STATIC_LOW);
        this.baseAddresses[HEAP_BASE_ADDRESS_INDEX] = Memory.getInstance().getAddress(MemoryConfigurations.DYNAMIC_LOW);
        this.baseAddresses[GLOBAL_POINTER_BASE_ADDRESS_INDEX] = -1; // Global pointer placeholder
        this.baseAddresses[STACK_POINTER_BASE_ADDRESS_INDEX] = -1; // Stack pointer placeholder
        this.baseAddresses[TEXT_BASE_ADDRESS_INDEX] = Memory.getInstance().getAddress(MemoryConfigurations.TEXT_LOW);
        this.baseAddresses[KERNEL_DATA_BASE_ADDRESS_INDEX] = Memory.getInstance().getAddress(MemoryConfigurations.KERNEL_DATA_LOW);
        this.baseAddresses[MMIO_BASE_ADDRESS_INDEX] = Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW);
        this.updateBaseAddressChoices();
        this.baseAddressSelector.setModel(new CustomComboBoxModel(this.baseAddressChoices));
        this.baseAddressSelector.setSelectedIndex(this.defaultBaseAddressIndex);
    }

    /**
     * Scroll the viewport so the cell at the given data segment address
     * is visible, vertically centered if possible, and selected.
     * Developed July 2007 for new feature that shows source code step where
     * label is defined when that label is clicked on in the Label Window.
     * Note there is a separate method to highlight the cell by setting
     * its background color to a highlighting color.  Thus one cell can be
     * highlighted while a different cell is selected at the same time.
     *
     * @param address data segment address of word to be selected.
     */
    public void selectCellForAddress(int address) {
        Point rowColumn = this.displayCellForAddress(address);
        if (rowColumn == null) {
            return;
        }
        Rectangle addressCell = this.table.getCellRect(rowColumn.x, rowColumn.y, true);
        // Select the memory address cell by generating a fake Mouse Pressed event within its
        // extent and explicitly invoking the table's mouse listener.
        MouseEvent fakeMouseEvent = new MouseEvent(
            this.table,
            MouseEvent.MOUSE_PRESSED,
            new Date().getTime(),
            MouseEvent.BUTTON1_DOWN_MASK,
            (int) addressCell.getX() + 1,
            (int) addressCell.getY() + 1,
            1,
            false
        );
        for (MouseListener mouseListener : this.table.getMouseListeners()) {
            mouseListener.mousePressed(fakeMouseEvent);
        }
    }

    /**
     * Scroll the viewport so the cell at the given data segment address
     * is visible, vertically centered if possible, and highlighted (but not selected).
     *
     * @param address Data segment address of word to be selected.
     */
    public void highlightCellForAddress(int address) {
        Point rowColumn = this.displayCellForAddress(address);
        if (rowColumn == null || rowColumn.x < 0 || rowColumn.y < 0) {
            return;
        }
        this.addressColumn = rowColumn.y;
        this.addressRowFirstAddress = Binary.decodeInteger(this.table.getValueAt(rowColumn.x, ADDRESS_COLUMN).toString());
        // Tell the system that table contents have changed.  This will trigger re-rendering
        // during which cell renderers are obtained.  The cell of interest (identified by
        // instance variables this.addressRow and this.addressColumn) will get a renderer
        // with highlight background color and all others get renderer with default background.
        this.table.tableChanged(new TableModelEvent(this.table.getModel(), 0, this.tableData.length - 1));
    }

    /**
     * Given address, will compute table cell location, adjusting table if necessary to
     * contain this cell, make sure that cell is visible, then return a Point containing
     * row and column position of cell in the table.  This private helper method is called
     * by selectCellForAddress() and highlightCellForAddress().
     * This is the kind of design I tell my students to avoid! The method both translates
     * address to table cell coordinates and adjusts the display to assure the cell is visible.
     * The two operations are related because the address may fall in within address space not
     * currently in the (display) table, including a different MIPS data segment (e.g. in
     * kernel instead of user data segment).
     */
    private Point displayCellForAddress(int address) {
        // This requires a 5-step process.  Each step is described
        // just above the statements that implement it.

        // STEP 1: Determine which data segment contains this address.
        int desiredComboBoxIndex = this.getBaseAddressIndexForAddress(address);
        if (desiredComboBoxIndex < 0) {
            // It is not a data segment address so good bye!
            return null;
        }
        // STEP 2:  Set the combo box appropriately.  This will also display the
        // first chunk of addresses from that segment.
        this.baseAddressSelector.setSelectedIndex(desiredComboBoxIndex);
        ((CustomComboBoxModel) this.baseAddressSelector.getModel()).forceComboBoxUpdate(desiredComboBoxIndex);
        this.baseAddressButtons[desiredComboBoxIndex].getActionListeners()[0].actionPerformed(null);
        // STEP 3:  Display memory chunk containing this address, which may be
        // different than the one just displayed.
        int baseAddress = this.baseAddresses[desiredComboBoxIndex];
        if (baseAddress == -1) {
            if (desiredComboBoxIndex == GLOBAL_POINTER_BASE_ADDRESS_INDEX) {
                baseAddress = Memory.alignToPrevious(RegisterFile.getValue(RegisterFile.GLOBAL_POINTER), BYTES_PER_ROW);
            }
            else if (desiredComboBoxIndex == STACK_POINTER_BASE_ADDRESS_INDEX) {
                baseAddress = Memory.alignToPrevious(RegisterFile.getValue(RegisterFile.STACK_POINTER), BYTES_PER_ROW);
            }
            else {
                // Shouldn't happen since these are the only two
                return null;
            }
        }
        int byteOffset = address - baseAddress;
        int chunkOffset = byteOffset / MEMORY_CHUNK_SIZE;
        int byteOffsetIntoChunk = byteOffset % MEMORY_CHUNK_SIZE;
        // Subtract 1 from chunkOffset because we're gonna call the "next" action
        // listener to get the correct chunk loaded and displayed, and the first
        // thing it does is increment firstAddress by MEMORY_CHUNK_SIZE.  Here
        // we do an offsetting decrement in advance because we don't want the
        // increment but we want the other actions that method provides.
        this.firstAddress += chunkOffset * MEMORY_CHUNK_SIZE - PREV_NEXT_CHUNK_SIZE;
        this.nextButton.getActionListeners()[0].actionPerformed(null);
        // STEP 4:  Find cell containing this address.  Add 1 to column calculation
        // because table column 0 displays address, not memory contents.  The
        // "convertColumnIndexToView()" is not necessary because the columns cannot be
        // reordered, but I included it as a precautionary measure in case that changes.
        int addrRow = byteOffsetIntoChunk / BYTES_PER_ROW;
        int addrColumn = byteOffsetIntoChunk % BYTES_PER_ROW / BYTES_PER_VALUE + 1;
        addrColumn = this.table.convertColumnIndexToView(addrColumn);
        Rectangle addressCell = this.table.getCellRect(addrRow, addrColumn, true);
        // STEP 5:  Center the row containing the cell of interest, to the extent possible.
        double cellHeight = addressCell.getHeight();
        double viewHeight = this.tableScrollPane.getViewport().getExtentSize().getHeight();
        int numberOfVisibleRows = (int) (viewHeight / cellHeight);
        int newViewPositionY = Math.max((int) ((addrRow - (numberOfVisibleRows / 2)) * cellHeight), 0);
        this.tableScrollPane.getViewport().setViewPosition(new Point(0, newViewPositionY));
        return new Point(addrRow, addrColumn);
    }

    private void initializeBaseAddressChoices() {
        this.updateBaseAddressChoices();
        this.defaultBaseAddressIndex = DATA_BASE_ADDRESS_INDEX;
    }

    /**
     * Update String array containing labels for base address combo box.
     */
    private void updateBaseAddressChoices() {
        for (int i = 0; i < this.baseAddressChoices.length; i++) {
            this.baseAddressChoices[i] = LOCATION_DESCRIPTIONS[i] + ((this.baseAddresses[i] >= 0) ? " (" + Binary.intToHexString(this.baseAddresses[i]) + ")" : "");
        }
    }

    /**
     * Given an address, determine which segment it is in and return the corresponding
     * combo box index.  Note there is not a one-to-one correspondence between these
     * indexes and the Memory tables.  For instance, the heap (0x10040000), the
     * global (0x10008000) and the data segment base (0x10000000) are all stored in the
     * same table as the static (0x10010000) so all are "Memory.inDataSegment()".
     */
    private int getBaseAddressIndexForAddress(int address) {
        int desiredComboBoxIndex = -1; // Assume not a data address
        if (Memory.getInstance().isInKernelDataSegment(address)) {
            return KERNEL_DATA_BASE_ADDRESS_INDEX;
        }
        else if (Memory.getInstance().isInMemoryMappedIO(address)) {
            return MMIO_BASE_ADDRESS_INDEX;
        }
        else if (Memory.getInstance().isInTextSegment(address)) { // DPS 8-July-2013
            return TEXT_BASE_ADDRESS_INDEX;
        }

        int shortestDistance = Integer.MAX_VALUE;
        int testDistance;
        // Check distance from .extern base.  Cannot be below it
        testDistance = address - Memory.getInstance().getAddress(MemoryConfigurations.EXTERN_LOW);
        if (testDistance >= 0 && testDistance < shortestDistance) {
            shortestDistance = testDistance;
            desiredComboBoxIndex = EXTERN_BASE_ADDRESS_INDEX;
        }
        // Check distance from .data base.  Cannot be below it
        testDistance = address - Memory.getInstance().getAddress(MemoryConfigurations.STATIC_LOW);
        if (testDistance >= 0 && testDistance < shortestDistance) {
            shortestDistance = testDistance;
            desiredComboBoxIndex = DATA_BASE_ADDRESS_INDEX;
        }
        // Check distance from heap base; cannot be below it
        testDistance = address - Memory.getInstance().getAddress(MemoryConfigurations.DYNAMIC_LOW);
        if (testDistance >= 0 && testDistance < shortestDistance) {
            shortestDistance = testDistance;
            desiredComboBoxIndex = HEAP_BASE_ADDRESS_INDEX;
        }
        // Check distance from global pointer; can be either side of it
        testDistance = Math.abs(address - RegisterFile.getValue(RegisterFile.GLOBAL_POINTER));
        if (testDistance < shortestDistance) {
            shortestDistance = testDistance;
            desiredComboBoxIndex = GLOBAL_POINTER_BASE_ADDRESS_INDEX;
        }
        // Check distance from stack pointer; can be on either side of it
        testDistance = Math.abs(address - RegisterFile.getValue(RegisterFile.STACK_POINTER));
        if (testDistance < shortestDistance) {
            shortestDistance = testDistance;
            desiredComboBoxIndex = STACK_POINTER_BASE_ADDRESS_INDEX;
        }
        // Check distance from .text base; cannot be below it
        testDistance = address - Memory.getInstance().getAddress(MemoryConfigurations.TEXT_LOW);
        if (testDistance >= 0 && testDistance < shortestDistance) {
            shortestDistance = testDistance;
            desiredComboBoxIndex = TEXT_BASE_ADDRESS_INDEX;
        }
        // Check distance from .kdata base; cannot be below it
        testDistance = address - Memory.getInstance().getAddress(MemoryConfigurations.KERNEL_DATA_LOW);
        if (testDistance >= 0 && testDistance < shortestDistance) {
            shortestDistance = testDistance;
            desiredComboBoxIndex = KERNEL_DATA_BASE_ADDRESS_INDEX;
        }
        // Check distance from MMIO base; cannot be below it
        testDistance = address - Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW);
        if (testDistance >= 0 && testDistance < shortestDistance) {
            shortestDistance = testDistance;
            desiredComboBoxIndex = MMIO_BASE_ADDRESS_INDEX;
        }

        return desiredComboBoxIndex;
    }

    /**
     * Generates the Address/Data part of the Data Segment window.
     * Returns the JScrollPane for the Address/Data part of the Data Segment window.
     */
    private JScrollPane generateDataPanel() {
        int valueBase = this.gui.getMainPane().getExecuteTab().getValueDisplayBase();
        int addressBase = this.gui.getMainPane().getExecuteTab().getAddressDisplayBase();

        this.tableData = new Object[ROW_COUNT][COLUMN_COUNT];
        int address = this.homeAddress;
        for (int row = 0; row < ROW_COUNT; row++) {
            this.tableData[row][ADDRESS_COLUMN] = NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase);
            for (int column = 1; column < COLUMN_COUNT; column++) {
                try {
                    this.tableData[row][column] = NumberDisplayBaseChooser.formatNumber(Memory.getInstance().fetchWord(address, false), valueBase);
                }
                catch (AddressErrorException exception) {
                    this.tableData[row][column] = NumberDisplayBaseChooser.formatNumber(0, valueBase);
                }
                address += BYTES_PER_VALUE;
            }
        }

        String[] columnNames = new String[COLUMN_COUNT];
        for (int column = 0; column < COLUMN_COUNT; column++) {
            columnNames[column] = getColumnName(column, addressBase);
        }

        this.table = new MemoryTable(new MemoryTableModel(this.tableData, columnNames));
        // Do not allow user to re-order columns; column order corresponds to MIPS memory order
        this.table.getTableHeader().setReorderingAllowed(false);
        this.table.setRowSelectionAllowed(false);
        // Addresses are column 0, render right-justified in mono font
        MonoRightCellRenderer monoRightCellRenderer = new MonoRightCellRenderer();
        this.table.getColumnModel().getColumn(ADDRESS_COLUMN).setPreferredWidth(60);
        this.table.getColumnModel().getColumn(ADDRESS_COLUMN).setCellRenderer(monoRightCellRenderer);
        // Data cells are columns 1 onward, render right-justified in mono font but highlightable.
        AddressCellRenderer addressCellRenderer = new AddressCellRenderer();
        for (int col = 1; col < COLUMN_COUNT; col++) {
            this.table.getColumnModel().getColumn(col).setPreferredWidth(60);
            this.table.getColumnModel().getColumn(col).setCellRenderer(addressCellRenderer);
        }
        this.tableScrollPane = new JScrollPane(this.table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return this.tableScrollPane;
    }

    /**
     * Little helper.  Is called when headers set up and each time number base changes.
     */
    private static String getColumnName(int column, int base) {
        return (column == ADDRESS_COLUMN) ? "Address" : "Value (+" + Integer.toString((column - 1) * BYTES_PER_VALUE, base) + ")";
    }

    /**
     * Generates and displays fresh table, typically done upon successful assembly.
     */
    public void setupTable() {
        this.tablePanel.removeAll();
        this.tablePanel.add(generateDataPanel());
        this.contentPane.add(this.tablePanel);
        this.setBaseAddressChooserEnabled(true);
    }

    /**
     * Removes the table from its frame, typically done when a file is closed.
     */
    public void clearWindow() {
        this.tablePanel.removeAll();
        this.setBaseAddressChooserEnabled(false);
    }

    /**
     * Clear highlight background color from any cell currently highlighted.
     */
    public void clearHighlighting() {
        this.addressHighlighting = false;
        this.table.tableChanged(new TableModelEvent(this.table.getModel(), 0, this.tableData.length - 1));
        // The below addresses situation in which addressRow and addressColum hold their
        // values across assemble operations.  Whereupon at the first step of the next
        // run the last cells from the previous run are highlighted!  This method is called
        // after each successful assemble (or reset, which just re-assembles).  The
        // assignment below assures the highlighting condition column==addressColumn will be
        // initially false since column>=0.  DPS 23 jan 2009
        this.addressColumn = -1;
    }

    private int getValueDisplayFormat() {
        return (this.asciiDisplay) ? NumberDisplayBaseChooser.ASCII : this.gui.getMainPane().getExecuteTab().getValueDisplayBase();
    }

    /**
     * Update table model with contents of new memory "chunk".  MARS supports megabytes of
     * data segment space so we only plug a "chunk" at a time into the table.
     *
     * @param firstAddress The first address in the memory range to be placed in the model.
     */
    public void updateModelForMemoryRange(int firstAddress) {
        if (this.tablePanel.getComponentCount() == 0) {
            // Ignore if no content to change
            return;
        }
        int valueBase = getValueDisplayFormat();
        int addressBase = this.gui.getMainPane().getExecuteTab().getAddressDisplayBase();
        int address = firstAddress;
        TableModel dataModel = this.table.getModel();
        for (int row = 0; row < ROW_COUNT; row++) {
            ((MemoryTableModel) dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase), row, ADDRESS_COLUMN);
            for (int column = 1; column < COLUMN_COUNT; column++) {
                try {
                    ((MemoryTableModel) dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(Memory.getInstance().fetchWord(address, false), valueBase), row, column);
                }
                catch (AddressErrorException exception) {
                    // Display 0 for values outside of the valid address range
                    ((MemoryTableModel) dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(0, valueBase), row, column);
                }
                address += BYTES_PER_VALUE;
            }
        }
    }

    /**
     * Redisplay the addresses.  This should only be done when address display base is
     * modified (e.g. between base 16, hex, and base 10, dec).
     */
    public void updateDataAddresses() {
        if (this.tablePanel.getComponentCount() == 0) {
            // Ignore if no content to change
            return;
        }
        int addressBase = this.gui.getMainPane().getExecuteTab().getAddressDisplayBase();
        int address = this.firstAddress;
        String formattedAddress;
        for (int row = 0; row < ROW_COUNT; row++) {
            formattedAddress = NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase);
            ((MemoryTableModel) this.table.getModel()).setDisplayAndModelValueAt(formattedAddress, row, 0);
            address += BYTES_PER_ROW;
        }
        // Column headers include address offsets, so update them too
        for (int column = 1; column < COLUMN_COUNT; column++) {
            this.table.getColumnModel().getColumn(column).setHeaderValue(getColumnName(column, addressBase));
        }
        this.table.getTableHeader().repaint();
    }

    /**
     * Update data display to show all values.
     */
    public void updateValues() {
        this.updateModelForMemoryRange(this.firstAddress);
    }

    @Override
    public void simulatorStarted(SimulatorStartEvent event) {
        this.startObservingMemory();
    }

    @Override
    public void simulatorPaused(SimulatorPauseEvent event) {
        this.stopObservingMemory();
        this.updateValues();
    }

    @Override
    public void simulatorFinished(SimulatorFinishEvent event) {
        this.stopObservingMemory();
        this.updateValues();
    }

    @Override
    public void simulatorStepped() {
        this.updateValues();
    }

    /**
     * Do this initially and upon reset.
     */
    private void setBaseAddressChooserEnabled(boolean enabled) {
        this.baseAddressSelector.setEnabled(enabled);
        this.baseAddressButtons[GLOBAL_POINTER_BASE_ADDRESS_INDEX].setEnabled(enabled);
        this.baseAddressButtons[STACK_POINTER_BASE_ADDRESS_INDEX].setEnabled(enabled);
        this.baseAddressButtons[HEAP_BASE_ADDRESS_INDEX].setEnabled(enabled);
        this.baseAddressButtons[EXTERN_BASE_ADDRESS_INDEX].setEnabled(enabled);
        this.baseAddressButtons[MMIO_BASE_ADDRESS_INDEX].setEnabled(enabled);
        this.baseAddressButtons[TEXT_BASE_ADDRESS_INDEX].setEnabled(enabled && this.gui.getSettings().selfModifyingCodeEnabled.get());
        this.baseAddressButtons[KERNEL_DATA_BASE_ADDRESS_INDEX].setEnabled(enabled);
        this.prevButton.setEnabled(enabled);
        this.nextButton.setEnabled(enabled);
        this.baseAddressButtons[DATA_BASE_ADDRESS_INDEX].setEnabled(enabled);
    }

    /**
     * Establish action listeners for the data segment navigation buttons.
     */
    private void initializeBaseAddressButtons() {
        // Set initial states
        setBaseAddressChooserEnabled(false);
        // Add tool tips
        // NOTE: For buttons that are now combo box items, the tool tips are not displayed w/o custom renderer.
        this.baseAddressButtons[GLOBAL_POINTER_BASE_ADDRESS_INDEX].setToolTipText("View memory around global pointer ($gp)");
        this.baseAddressButtons[STACK_POINTER_BASE_ADDRESS_INDEX].setToolTipText("View memory around stack pointer ($sp)");
        this.baseAddressButtons[HEAP_BASE_ADDRESS_INDEX].setToolTipText("View memory around heap starting at " + Binary.intToHexString(Memory.getInstance().getAddress(MemoryConfigurations.DYNAMIC_LOW)));
        this.baseAddressButtons[KERNEL_DATA_BASE_ADDRESS_INDEX].setToolTipText("View memory around kernel data starting at " + Binary.intToHexString(Memory.getInstance().getAddress(MemoryConfigurations.KERNEL_DATA_LOW)));
        this.baseAddressButtons[EXTERN_BASE_ADDRESS_INDEX].setToolTipText("View memory around static globals starting at " + Binary.intToHexString(Memory.getInstance().getAddress(MemoryConfigurations.EXTERN_LOW)));
        this.baseAddressButtons[MMIO_BASE_ADDRESS_INDEX].setToolTipText("View memory around memory-mapped I/O starting at " + Binary.intToHexString(Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW)));
        this.baseAddressButtons[TEXT_BASE_ADDRESS_INDEX].setToolTipText("View memory around program code starting at " + Binary.intToHexString(Memory.getInstance().getAddress(MemoryConfigurations.TEXT_LOW)));
        this.baseAddressButtons[DATA_BASE_ADDRESS_INDEX].setToolTipText("View memory around program data starting at " + Binary.intToHexString(Memory.getInstance().getAddress(MemoryConfigurations.STATIC_LOW)));
        this.prevButton.setToolTipText("View previous/lower memory range (hold down for rapid fire)");
        this.nextButton.setToolTipText("View next/higher memory range (hold down for rapid fire)");

        // Add the action listeners to maintain button state and table contents
        // Currently there is no memory upper bound so next button always enabled.
        this.baseAddressButtons[GLOBAL_POINTER_BASE_ADDRESS_INDEX].addActionListener(event -> {
            this.userOrKernelMode = USER_MODE;
            this.firstAddress = RegisterFile.getValue(RegisterFile.GLOBAL_POINTER);
            // updateModelForMemoryRange requires argument to be multiple of 4,
            // but for cleaner display we'll make it a multiple of 32 (last nibble is 0).
            // This makes it easier to mentally calculate address from row address + column offset.
            this.firstAddress = Memory.alignToPrevious(this.firstAddress, BYTES_PER_ROW);
            this.homeAddress = this.firstAddress;
            this.updateFirstAddress(this.firstAddress);
            this.updateModelForMemoryRange(this.firstAddress);
        });
        this.baseAddressButtons[STACK_POINTER_BASE_ADDRESS_INDEX].addActionListener(event -> {
            this.userOrKernelMode = USER_MODE;
            this.firstAddress = RegisterFile.getValue(RegisterFile.STACK_POINTER);
            // See comment above for baseAddressButtons[GLOBAL_POINTER_BASE_ADDRESS_INDEX]
            this.firstAddress = Memory.alignToPrevious(this.firstAddress, BYTES_PER_ROW);
            this.homeAddress = Memory.getInstance().getAddress(MemoryConfigurations.STACK_POINTER);
            this.updateFirstAddress(this.firstAddress);
            this.updateModelForMemoryRange(this.firstAddress);
        });
        this.baseAddressButtons[HEAP_BASE_ADDRESS_INDEX].addActionListener(event -> {
            this.userOrKernelMode = USER_MODE;
            this.homeAddress = Memory.getInstance().getAddress(MemoryConfigurations.DYNAMIC_LOW);
            this.updateFirstAddress(this.homeAddress);
            this.updateModelForMemoryRange(this.firstAddress);
        });
        this.baseAddressButtons[EXTERN_BASE_ADDRESS_INDEX].addActionListener(event -> {
            this.userOrKernelMode = USER_MODE;
            this.homeAddress = Memory.getInstance().getAddress(MemoryConfigurations.EXTERN_LOW);
            this.updateFirstAddress(this.homeAddress);
            this.updateModelForMemoryRange(this.firstAddress);
        });
        this.baseAddressButtons[KERNEL_DATA_BASE_ADDRESS_INDEX].addActionListener(event -> {
            this.userOrKernelMode = KERNEL_MODE;
            this.homeAddress = Memory.getInstance().getAddress(MemoryConfigurations.KERNEL_DATA_LOW);
            this.updateFirstAddress(this.homeAddress);
            this.updateModelForMemoryRange(this.firstAddress);
        });
        this.baseAddressButtons[MMIO_BASE_ADDRESS_INDEX].addActionListener(event -> {
            this.userOrKernelMode = KERNEL_MODE;
            this.homeAddress = Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW);
            this.updateFirstAddress(this.homeAddress);
            this.updateModelForMemoryRange(this.firstAddress);
        });
        this.baseAddressButtons[TEXT_BASE_ADDRESS_INDEX].addActionListener(event -> {
            this.userOrKernelMode = USER_MODE;
            this.homeAddress = Memory.getInstance().getAddress(MemoryConfigurations.TEXT_LOW);
            this.updateFirstAddress(this.homeAddress);
            this.updateModelForMemoryRange(this.firstAddress);
        });
        baseAddressButtons[DATA_BASE_ADDRESS_INDEX].addActionListener(event -> {
            this.userOrKernelMode = USER_MODE;
            this.homeAddress = Memory.getInstance().getAddress(MemoryConfigurations.STATIC_LOW);
            this.updateFirstAddress(this.homeAddress);
            this.updateModelForMemoryRange(this.firstAddress);
        });

        // NOTE: action listeners for prevButton and nextButton are now in their
        // specialized inner classes at the bottom of this listing.  DPS 20 July 2008
    }

    /**
     * This will assure that user cannot view memory locations outside the data segment
     * for selected mode.  For user mode, this means no lower than data segment base,
     * or higher than user memory boundary.  For kernel mode, this means no lower than
     * kernel data segment base or higher than kernel memory.  It is called by the
     * above action listeners.
     * <p>
     * <code>prevButton</code> and <code>nextButton</code> are also enabled/disabled appropriately.
     */
    private void updateFirstAddress(int address) {
        int lowLimit = Memory.getInstance().getAddress((this.userOrKernelMode == USER_MODE) ? MemoryConfigurations.USER_LOW : MemoryConfigurations.MAPPED_LOW);
        int highLimit = Memory.getInstance().getAddress((this.userOrKernelMode == USER_MODE) ? MemoryConfigurations.USER_HIGH : MemoryConfigurations.MAPPED_HIGH);

        this.firstAddress = address;
        if (this.firstAddress <= lowLimit) {
            this.firstAddress = lowLimit;
            this.prevButton.setEnabled(false);
        }
        else {
            this.prevButton.setEnabled(true);
        }
        if (this.firstAddress >= highLimit - MEMORY_CHUNK_SIZE) {
            this.firstAddress = highLimit - MEMORY_CHUNK_SIZE + 1;
            this.nextButton.setEnabled(false);
        }
        else {
            this.nextButton.setEnabled(true);
        }
    }

    @Override
    public void memoryWritten(int address, int length, int value, int wordAddress, int wordValue) {
        if (RunSpeedPanel.getInstance().getRunSpeed() != RunSpeedPanel.UNLIMITED_SPEED) {
            this.addressHighlighting = true;
            this.highlightCellForAddress(wordAddress);
        }
        else {
            this.addressHighlighting = false;
        }
    }

    /**
     * Convenience method to add this as a listener of all memory.
     */
    public void startObservingMemory() {
        Memory.getInstance().addListener(this);
    }

    /**
     * Convenience method to remove this as a listener of memory.
     */
    public void stopObservingMemory() {
        Memory.getInstance().removeListener(this);
    }

    /**
     * Class defined to address apparent Javax.swing.JComboBox bug: when selection is
     * is set programmatically using setSelectedIndex() rather than by user-initiated
     * event (such as mouse click), the text displayed in the JComboBox is not always
     * updated correctly. Sometimes it is, sometimes updated to incorrect value.
     * No pattern that I can detect.  Google search yielded many forums addressing
     * this problem. One suggested solution, a JComboBox superclass overriding
     * setSelectedIndex to also call selectedItemChanged() did not help.  Only this
     * solution to extend the model class to call the protected
     * "fireContentsChanged()" method worked.
     *
     * @author DPS 25-Jan-2009
     */
    // TODO: Has this bug been fixed?
    private static class CustomComboBoxModel extends DefaultComboBoxModel<String> {
        public CustomComboBoxModel(String[] list) {
            super(list);
        }

        public void forceComboBoxUpdate(int index) {
            super.fireContentsChanged(this, index, index);
        }
    }

    /**
     * Class representing memory data table data.
     */
    private class MemoryTableModel extends AbstractTableModel {
        private final String[] columnNames;
        private final Object[][] data;

        public MemoryTableModel(Object[][] data, String[] columnNames) {
            this.data = data;
            this.columnNames = columnNames;
        }

        @Override
        public int getColumnCount() {
            return this.columnNames.length;
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public String getColumnName(int column) {
            return this.columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            return this.data[row][column];
        }

        /**
         * The cells in the Address column are not editable.
         * Value cells are editable except when displayed
         * in ASCII view - don't want to give the impression
         * that ASCII text can be entered directly because
         * it can't.  It is possible but not worth the
         * effort to implement.
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            return column != ADDRESS_COLUMN && !DataSegmentWindow.this.asciiDisplay;
        }

        /**
         * JTable uses this method to determine the default renderer/editor for each cell.
         */
        @Override
        public Class<?> getColumnClass(int column) {
            return getValueAt(0, column).getClass();
        }

        /**
         * Update cell contents in table model.  This method should be called
         * only when user edits cell, so input validation has to be done.  If
         * value is valid, MIPS memory is updated.
         */
        @Override
        public void setValueAt(Object value, int row, int column) {
            int intValue;
            int address = 0;
            try {
                intValue = Binary.decodeInteger(value.toString());
            }
            catch (NumberFormatException exception) {
                this.data[row][column] = "INVALID";
                this.fireTableCellUpdated(row, column);
                return;
            }

            // Calculate address from row and column
            try {
                address = Binary.decodeInteger(this.data[row][ADDRESS_COLUMN].toString()) + (column - 1) * BYTES_PER_VALUE; // KENV 1/6/05
            }
            catch (NumberFormatException exception) {
                // Can't really happen since memory addresses are completely under
                // the control of the software.
            }
            // Assures that if changed during MIPS program execution, the update will
            // occur only between MIPS instructions.
            synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                try {
                    Memory.getInstance().storeWord(address, intValue, true);
                }
                // Somehow, user was able to display out-of-range address.  Most likely to occur between
                // stack base and kernel.  Also text segment with self-modifying-code setting off.
                catch (AddressErrorException exception) {
                    return;
                }
            }
            int valueBase = DataSegmentWindow.this.gui.getMainPane().getExecuteTab().getValueDisplayBase();
            this.data[row][column] = NumberDisplayBaseChooser.formatNumber(intValue, valueBase);
            this.fireTableCellUpdated(row, column);
        }

        /**
         * Update cell contents in table model.  Does not affect MIPS memory.
         */
        private void setDisplayAndModelValueAt(Object value, int row, int column) {
            this.data[row][column] = value;
            this.fireTableCellUpdated(row, column);
        }
    }

    /**
     * Special renderer capable of highlighting cells by changing background color.
     * Will set background to highlight color if certain conditions met.
     */
    private class AddressCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int rowFirstAddress = Binary.decodeInteger(table.getValueAt(row, ADDRESS_COLUMN).toString());
            boolean isHighlighted = DataSegmentWindow.this.gui.getSettings().highlightDataSegment.get() && DataSegmentWindow.this.addressHighlighting
                && rowFirstAddress == DataSegmentWindow.this.addressRowFirstAddress && column == DataSegmentWindow.this.addressColumn;

            this.setHorizontalAlignment(SwingConstants.CENTER);
            if (isHighlighted) {
                this.setBackground(DataSegmentWindow.this.gui.getSettings().dataSegmentHighlightBackground.get());
                this.setForeground(DataSegmentWindow.this.gui.getSettings().dataSegmentHighlightForeground.get());
            }
            else {
                this.setBackground(null);
                this.setForeground(null);
            }

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isHighlighted) {
                this.setFont(DataSegmentWindow.this.gui.getSettings().tableHighlightFont.get());
            }
            else {
                this.setFont(DataSegmentWindow.this.gui.getSettings().tableFont.get());
            }

            return this;
        }
    }

    /**
     * JTable subclass to provide custom tool tips for each of the
     * register table column headers and for each register name in the first column. From
     * <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/table.html">Sun's JTable tutorial</a>.
     */
    private class MemoryTable extends JTable {
        public MemoryTable(MemoryTableModel model) {
            super(model);
        }

        private static final String[] COLUMN_TOOL_TIPS = {
            "Base MIPS memory address for this row of the table.", // Address
            "32-bit value stored at the base address for its row.", // Value (+0)
            "32-bit value stored %d bytes beyond the base address for its row.", // Value (+n)
        };

        // Implement table header tool tips.
        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(this.columnModel) {
                @Override
                public String getToolTipText(MouseEvent event) {
                    int index = this.columnModel.getColumnIndexAtX(event.getPoint().x);
                    int toolTipIndex = this.columnModel.getColumn(index).getModelIndex();
                    if (toolTipIndex >= 2) {
                        // Value (+n)
                        return COLUMN_TOOL_TIPS[2].formatted((toolTipIndex - 1) * 4);
                    }
                    else {
                        return COLUMN_TOOL_TIPS[toolTipIndex];
                    }
                }
            };
        }
    }

    /**
     * The Prev button (left arrow) scrolls downward through the
     * selected address range.  It is a RepeatButton, which means
     * if the mouse is held down on the button, it will repeatedly
     * fire after an initial delay.  Allows rapid scrolling.
     *
     * @author DPS 20 July 2008
     */
    private class PrevButton extends RepeatButton {
        public PrevButton(Icon icon) {
            super(icon);
            this.setInitialDelay(500); // 500 milliseconds hold-down before firing
            this.setInterval(60); // every 60 milliseconds after that
            this.addActionListener(this);
        }

        /**
         * This occurs when the button is clicked, the repeat timer goes off, or the button is lifted.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            DataSegmentWindow.this.updateFirstAddress(DataSegmentWindow.this.firstAddress - PREV_NEXT_CHUNK_SIZE);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
        }
    }

    /**
     * The Next button (right arrow) scrolls upward through the
     * selected address range.  It is a RepeatButton, which means
     * if the mouse is held down on the button, it will repeatedly
     * fire after an initial delay.  Allows rapid scrolling.
     *
     * @author DPS 20 July 2008
     */
    private class NextButton extends RepeatButton {
        public NextButton(Icon icon) {
            super(icon);
            this.setInitialDelay(500); // 500 milliseconds hold-down before firing
            this.setInterval(60); // every 60 milliseconds after that
            this.addActionListener(this);
        }

        /**
         * This occurs when the button is clicked, the repeat timer goes off, or the button is lifted.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            DataSegmentWindow.this.updateFirstAddress(DataSegmentWindow.this.firstAddress + PREV_NEXT_CHUNK_SIZE);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
        }
    }
}
