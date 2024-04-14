package mars.venus.execute;

import mars.Application;
import mars.settings.Settings;
import mars.mips.hardware.*;
import mars.simulator.Simulator;
import mars.simulator.SimulatorNotice;
import mars.util.Binary;
import mars.venus.MonoRightCellRenderer;
import mars.venus.NumberDisplayBaseChooser;
import mars.venus.RepeatButton;
import mars.venus.RunSpeedPanel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

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
public class DataSegmentWindow extends JInternalFrame implements Observer {
    private static Object[][] tableData;

    private static JTable table;
    private JScrollPane tableScrollPane;
    private final Container contentPane;
    private final JPanel tablePanel;
    private JButton nextButton;
    private JButton prevButton;

    public static final int VALUES_PER_ROW = 8;
    public static final int ROW_COUNT = 16; // with 8 value columns, this shows 512 bytes
    public static final int COLUMN_COUNT = 1 + VALUES_PER_ROW; // 1 for address and 8 for values
    public static final int BYTES_PER_VALUE = 4;
    public static final int BYTES_PER_ROW = VALUES_PER_ROW * BYTES_PER_VALUE;
    public static final int MEMORY_CHUNK_SIZE = ROW_COUNT * BYTES_PER_ROW;
    // PREV_NEXT_CHUNK_SIZE determines how many rows will be scrolled when Prev or Next buttons fire.
    // MEMORY_CHUNK_SIZE / 2 means scroll half a table up or down.  Easier to view series that flows off the edge.
    // MEMORY_CHUNK_SIZE means scroll a full table's worth.  Scrolls through memory faster.  DPS 26-Jan-09
    public static final int PREV_NEXT_CHUNK_SIZE = MEMORY_CHUNK_SIZE / 2;
    public static final int ADDRESS_COLUMN = 0;
    public static final boolean USER_MODE = false;
    public static final boolean KERNEL_MODE = true;

    private boolean addressHighlighting = false;
    private boolean asciiDisplay = false;
    private int addressRow;
    private int addressColumn;
    private int addressRowFirstAddress;
    private final Settings settings;

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
        Memory.externBaseAddress,
        Memory.dataBaseAddress,
        Memory.heapBaseAddress,
        -1 /*Memory.globalPointer*/,
        -1 /*Memory.stackPointer*/,
        Memory.textBaseAddress,
        Memory.kernelDataBaseAddress,
        Memory.mmioBaseAddress,
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
    public DataSegmentWindow(NumberDisplayBaseChooser[] choosers) {
        super("Data Segment", true, false, true, true);

        Simulator.getInstance().addObserver(this);
        settings = Application.getSettings();
        settings.addObserver(this);

        homeAddress = Memory.dataBaseAddress;
        firstAddress = homeAddress;
        userOrKernelMode = USER_MODE;
        addressHighlighting = false;
        baseAddressChoices = new String[baseAddresses.length];
        baseAddressButtons = new JButton[baseAddresses.length];
        contentPane = this.getContentPane();
        tablePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel navigationBar = new JPanel();
        Toolkit tk = Toolkit.getDefaultToolkit();
        Class<?> cs = this.getClass();
        try {
            prevButton = new PrevButton(new ImageIcon(tk.getImage(cs.getResource(Application.IMAGES_PATH + "Previous22.png"))));
            nextButton = new NextButton(new ImageIcon(tk.getImage(cs.getResource(Application.IMAGES_PATH + "Next22.png"))));
            // This group of buttons was replaced by a combo box.  Keep the JButton objects for their action listeners.
            for (int index = 0; index < baseAddressButtons.length; index++) {
                baseAddressButtons[index] = new JButton();
            }
        }
        catch (NullPointerException exception) {
            System.out.println("Internal Error: images folder not found");
            System.exit(0);
        }

        initializeBaseAddressChoices();
        baseAddressSelector = new JComboBox<>();
        baseAddressSelector.setModel(new CustomComboBoxModel(baseAddressChoices));
        baseAddressSelector.setEditable(false);
        baseAddressSelector.setSelectedIndex(defaultBaseAddressIndex);
        baseAddressSelector.setToolTipText("Base address for data segment display");
        baseAddressSelector.addActionListener(event -> {
            // Trigger action listener for associated invisible button
            baseAddressButtons[baseAddressSelector.getSelectedIndex()].getActionListeners()[0].actionPerformed(null);
        });

        initializeBaseAddressButtons();
        JPanel navButtons = new JPanel(new GridLayout(1, 4));
        navButtons.add(prevButton);
        navButtons.add(nextButton);
        navigationBar.add(navButtons);
        navigationBar.add(baseAddressSelector);
        for (NumberDisplayBaseChooser chooser : choosers) {
            navigationBar.add(chooser);
        }
        JCheckBox asciiDisplayCheckBox = new JCheckBox("ASCII", asciiDisplay);
        asciiDisplayCheckBox.setToolTipText("Display data segment values in ASCII (overrides Hexadecimal Values setting)");
        asciiDisplayCheckBox.addItemListener(event -> {
            asciiDisplay = (event.getStateChange() == ItemEvent.SELECTED);
            DataSegmentWindow.this.updateValues();
        });
        navigationBar.add(asciiDisplayCheckBox);

        contentPane.add(navigationBar, BorderLayout.SOUTH);
    }

    public void updateBaseAddressComboBox() {
        baseAddresses[EXTERN_BASE_ADDRESS_INDEX] = Memory.externBaseAddress;
        baseAddresses[GLOBAL_POINTER_BASE_ADDRESS_INDEX] = -1; /*Memory.globalPointer*/
        baseAddresses[DATA_BASE_ADDRESS_INDEX] = Memory.dataBaseAddress;
        baseAddresses[HEAP_BASE_ADDRESS_INDEX] = Memory.heapBaseAddress;
        baseAddresses[STACK_POINTER_BASE_ADDRESS_INDEX] = -1; /*Memory.stackPointer*/
        baseAddresses[KERNEL_DATA_BASE_ADDRESS_INDEX] = Memory.kernelDataBaseAddress;
        baseAddresses[MMIO_BASE_ADDRESS_INDEX] = Memory.mmioBaseAddress;
        baseAddresses[TEXT_BASE_ADDRESS_INDEX] = Memory.textBaseAddress;
        updateBaseAddressChoices();
        baseAddressSelector.setModel(new CustomComboBoxModel(baseAddressChoices));
        baseAddressSelector.setSelectedIndex(defaultBaseAddressIndex);
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
        Point rowColumn = displayCellForAddress(address);
        if (rowColumn == null) {
            return;
        }
        Rectangle addressCell = table.getCellRect(rowColumn.x, rowColumn.y, true);
        // Select the memory address cell by generating a fake Mouse Pressed event within its
        // extent and explicitly invoking the table's mouse listener.
        MouseEvent fakeMouseEvent = new MouseEvent(table,
            MouseEvent.MOUSE_PRESSED,
            new Date().getTime(),
            MouseEvent.BUTTON1_DOWN_MASK,
            (int) addressCell.getX() + 1,
            (int) addressCell.getY() + 1,
            1,
            false
        );
        for (MouseListener mouseListener : table.getMouseListeners()) {
            mouseListener.mousePressed(fakeMouseEvent);
        }
    }

    /**
     * Scroll the viewport so the cell at the given data segment address
     * is visible, vertically centered if possible, and highlighted (but not selected).
     *
     * @param address data segment address of word to be selected.
     */
    public void highlightCellForAddress(int address) {
        Point rowColumn = displayCellForAddress(address);
        if (rowColumn == null || rowColumn.x < 0 || rowColumn.y < 0) {
            return;
        }
        this.addressRow = rowColumn.x;
        this.addressColumn = rowColumn.y;
        this.addressRowFirstAddress = Binary.stringToInt(table.getValueAt(this.addressRow, ADDRESS_COLUMN).toString());
        //System.out.println("Address "+Binary.intToHexString(address)+" becomes row "+ addressRow + " column "+addressColumn+
        //" starting addr "+dataTable.getValueAt(this.addressRow,ADDRESS_COLUMN));
        // Tell the system that table contents have changed.  This will trigger re-rendering
        // during which cell renderers are obtained.  The cell of interest (identified by
        // instance variables this.addressRow and this.addressColumn) will get a renderer
        // with highlight background color and all others get renderer with default background.
        table.tableChanged(new TableModelEvent(table.getModel(), 0, tableData.length - 1));
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
        int desiredComboBoxIndex = getBaseAddressIndexForAddress(address);
        if (desiredComboBoxIndex < 0) {
            // It is not a data segment address so good bye!
            return null;
        }
        // STEP 2:  Set the combo box appropriately.  This will also display the
        // first chunk of addresses from that segment.
        baseAddressSelector.setSelectedIndex(desiredComboBoxIndex);
        ((CustomComboBoxModel) baseAddressSelector.getModel()).forceComboBoxUpdate(desiredComboBoxIndex);
        baseAddressButtons[desiredComboBoxIndex].getActionListeners()[0].actionPerformed(null);
        // STEP 3:  Display memory chunk containing this address, which may be
        // different than the one just displayed.
        int baseAddress = baseAddresses[desiredComboBoxIndex];
        if (baseAddress == -1) {
            if (desiredComboBoxIndex == GLOBAL_POINTER_BASE_ADDRESS_INDEX) {
                baseAddress = RegisterFile.getValue(RegisterFile.GLOBAL_POINTER) - (RegisterFile.getValue(RegisterFile.GLOBAL_POINTER) % BYTES_PER_ROW);
            }
            else if (desiredComboBoxIndex == STACK_POINTER_BASE_ADDRESS_INDEX) {
                baseAddress = RegisterFile.getValue(RegisterFile.STACK_POINTER) - (RegisterFile.getValue(RegisterFile.STACK_POINTER) % BYTES_PER_ROW);
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
        firstAddress = firstAddress + chunkOffset * MEMORY_CHUNK_SIZE - PREV_NEXT_CHUNK_SIZE;
        nextButton.getActionListeners()[0].actionPerformed(null);
        // STEP 4:  Find cell containing this address.  Add 1 to column calculation
        // because table column 0 displays address, not memory contents.  The
        // "convertColumnIndexToView()" is not necessary because the columns cannot be
        // reordered, but I included it as a precautionary measure in case that changes.
        int addrRow = byteOffsetIntoChunk / BYTES_PER_ROW;
        int addrColumn = byteOffsetIntoChunk % BYTES_PER_ROW / BYTES_PER_VALUE + 1;
        addrColumn = table.convertColumnIndexToView(addrColumn);
        Rectangle addressCell = table.getCellRect(addrRow, addrColumn, true);
        // STEP 5:  Center the row containing the cell of interest, to the extent possible.
        double cellHeight = addressCell.getHeight();
        double viewHeight = tableScrollPane.getViewport().getExtentSize().getHeight();
        int numberOfVisibleRows = (int) (viewHeight / cellHeight);
        int newViewPositionY = Math.max((int) ((addrRow - (numberOfVisibleRows / 2)) * cellHeight), 0);
        tableScrollPane.getViewport().setViewPosition(new Point(0, newViewPositionY));
        return new Point(addrRow, addrColumn);
    }

    private void initializeBaseAddressChoices() {
        updateBaseAddressChoices();
        defaultBaseAddressIndex = DATA_BASE_ADDRESS_INDEX;
    }

    /**
     * Update String array containing labels for base address combo box.
     */
    private void updateBaseAddressChoices() {
        for (int i = 0; i < baseAddressChoices.length; i++) {
            baseAddressChoices[i] = LOCATION_DESCRIPTIONS[i] + ((baseAddresses[i] >= 0) ? " (" + Binary.intToHexString(baseAddresses[i]) + ")" : "");
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
        if (Memory.inKernelDataSegment(address)) {
            return KERNEL_DATA_BASE_ADDRESS_INDEX;
        }
        else if (Memory.inMemoryMapSegment(address)) {
            return MMIO_BASE_ADDRESS_INDEX;
        }
        else if (Memory.inTextSegment(address)) { // DPS 8-July-2013
            return TEXT_BASE_ADDRESS_INDEX;
        }

        int shortDistance = Integer.MAX_VALUE;
        int testDistance;
        // Check distance from .extern base.  Cannot be below it
        testDistance = address - Memory.externBaseAddress;
        if (testDistance >= 0 && testDistance < shortDistance) {
            shortDistance = testDistance;
            desiredComboBoxIndex = EXTERN_BASE_ADDRESS_INDEX;
        }
        // Check distance from global pointer; can be either side of it...
        testDistance = Math.abs(address - RegisterFile.getValue(RegisterFile.GLOBAL_POINTER));
        if (testDistance < shortDistance) {
            shortDistance = testDistance;
            desiredComboBoxIndex = GLOBAL_POINTER_BASE_ADDRESS_INDEX;
        }
        // Check distance from .data base.  Cannot be below it
        testDistance = address - Memory.dataBaseAddress;
        if (testDistance >= 0 && testDistance < shortDistance) {
            shortDistance = testDistance;
            desiredComboBoxIndex = DATA_BASE_ADDRESS_INDEX;
        }
        // Check distance from heap base.  Cannot be below it
        testDistance = address - Memory.heapBaseAddress;
        if (testDistance >= 0 && testDistance < shortDistance) {
            shortDistance = testDistance;
            desiredComboBoxIndex = HEAP_BASE_ADDRESS_INDEX;
        }
        // Check distance from stack pointer.  Can be on either side of it...
        testDistance = Math.abs(address - RegisterFile.getValue(RegisterFile.STACK_POINTER));
        if (testDistance < shortDistance) {
            shortDistance = testDistance;
            desiredComboBoxIndex = STACK_POINTER_BASE_ADDRESS_INDEX;
        }
        return desiredComboBoxIndex;
    }

    /**
     * Generates the Address/Data part of the Data Segment window.
     * Returns the JScrollPane for the Address/Data part of the Data Segment window.
     */
    private JScrollPane generateDataPanel() {
        int valueBase = Application.getGUI().getMainPane().getExecuteTab().getValueDisplayBase();
        int addressBase = Application.getGUI().getMainPane().getExecuteTab().getAddressDisplayBase();

        tableData = new Object[ROW_COUNT][COLUMN_COUNT];
        int address = this.homeAddress;
        for (int row = 0; row < ROW_COUNT; row++) {
            tableData[row][ADDRESS_COLUMN] = NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase);
            for (int column = 1; column < COLUMN_COUNT; column++) {
                try {
                    tableData[row][column] = NumberDisplayBaseChooser.formatNumber(Application.memory.getRawWord(address), valueBase);
                }
                catch (AddressErrorException exception) {
                    tableData[row][column] = NumberDisplayBaseChooser.formatNumber(0, valueBase);
                }
                address += BYTES_PER_VALUE;
            }
        }

        String[] columnNames = new String[COLUMN_COUNT];
        for (int column = 0; column < COLUMN_COUNT; column++) {
            columnNames[column] = getColumnName(column, addressBase);
        }

        table = new MemoryTable(new MemoryTableModel(tableData, columnNames));
        // Do not allow user to re-order columns; column order corresponds to MIPS memory order
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowSelectionAllowed(false);
        // Addresses are column 0, render right-justified in mono font
        MonoRightCellRenderer monoRightCellRenderer = new MonoRightCellRenderer();
        table.getColumnModel().getColumn(ADDRESS_COLUMN).setPreferredWidth(60);
        table.getColumnModel().getColumn(ADDRESS_COLUMN).setCellRenderer(monoRightCellRenderer);
        // Data cells are columns 1 onward, render right-justified in mono font but highlightable.
        AddressCellRenderer addressCellRenderer = new AddressCellRenderer();
        for (int col = 1; col < COLUMN_COUNT; col++) {
            table.getColumnModel().getColumn(col).setPreferredWidth(60);
            table.getColumnModel().getColumn(col).setCellRenderer(addressCellRenderer);
        }
        tableScrollPane = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        return tableScrollPane;
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
        tablePanel.removeAll();
        tablePanel.add(generateDataPanel());
        contentPane.add(tablePanel);
        setBaseAddressChooserEnabled(true);
    }

    /**
     * Removes the table from its frame, typically done when a file is closed.
     */
    public void clearWindow() {
        tablePanel.removeAll();
        setBaseAddressChooserEnabled(false);
    }

    /**
     * Clear highlight background color from any cell currently highlighted.
     */
    public void clearHighlighting() {
        addressHighlighting = false;
        table.tableChanged(new TableModelEvent(table.getModel(), 0, tableData.length - 1));
        // The below addresses situation in which addressRow and addressColum hold their
        // values across assemble operations.  Whereupon at the first step of the next
        // run the last cells from the previous run are highlighted!  This method is called
        // after each successful assemble (or reset, which just re-assembles).  The
        // assignment below assures the highlighting condition column==addressColumn will be
        // initially false since column>=0.  DPS 23 jan 2009
        addressColumn = -1;
    }

    private int getValueDisplayFormat() {
        return (asciiDisplay) ? NumberDisplayBaseChooser.ASCII : Application.getGUI().getMainPane().getExecuteTab().getValueDisplayBase();
    }

    /**
     * Update table model with contents of new memory "chunk".  Mars supports megabytes of
     * data segment space so we only plug a "chunk" at a time into the table.
     *
     * @param firstAddr the first address in the memory range to be placed in the model.
     */
    public void updateModelForMemoryRange(int firstAddr) {
        if (tablePanel.getComponentCount() == 0) {
            // Ignore if no content to change
            return;
        }
        int valueBase = getValueDisplayFormat();
        int addressBase = Application.getGUI().getMainPane().getExecuteTab().getAddressDisplayBase();
        int address = firstAddr;
        TableModel dataModel = table.getModel();
        for (int row = 0; row < ROW_COUNT; row++) {
            ((MemoryTableModel) dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase), row, ADDRESS_COLUMN);
            for (int column = 1; column < COLUMN_COUNT; column++) {
                try {
                    ((MemoryTableModel) dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(Application.memory.getWordNoNotify(address), valueBase), row, column);
                }
                catch (AddressErrorException e) {
                    // Bit of a hack here.  Memory will throw an exception if you try to read directly from text segment when the
                    // self-modifying code setting is disabled.  This is a good thing if it is the executing MIPS program trying to
                    // read.  But not a good thing if it is the DataSegmentDisplay trying to read.  I'll trick Memory by
                    // temporarily enabling the setting as "non persistent" so it won't write through to the registry.
                    if (Memory.inTextSegment(address)) {
                        int displayValue = 0;
                        if (!Application.getSettings().selfModifyingCodeEnabled.get()) {
                            Application.getSettings().selfModifyingCodeEnabled.setNonPersistent(true);
                            try {
                                displayValue = Application.memory.getWordNoNotify(address);
                            }
                            catch (AddressErrorException e1) {
                                // Still got an exception?  Doesn't seem possible but if we drop through it will write default value 0.
                            }
                            Application.getSettings().selfModifyingCodeEnabled.setNonPersistent(false);
                        }
                        ((MemoryTableModel) dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(displayValue, valueBase), row, column);
                    }
                    // Bug Fix: the following line of code disappeared during the release 4.4 mods, but is essential to
                    // display values of 0 for valid MIPS addresses that are outside the MARS simulated address space.  Such
                    // addresses cause an AddressErrorException.  Prior to 4.4, they performed this line of code unconditionally.
                    // With 4.4, I added the above IF statement to work with the text segment but inadvertently removed this line!
                    // Now it becomes the "else" part, executed when not in text segment.  DPS 8-July-2014.
                    else {
                        ((MemoryTableModel) dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(0, valueBase), row, column);
                    }
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
        if (tablePanel.getComponentCount() == 0) {
            return; // ignore if no content to change
        }
        int addressBase = Application.getGUI().getMainPane().getExecuteTab().getAddressDisplayBase();
        int address = this.firstAddress;
        String formattedAddress;
        for (int row = 0; row < ROW_COUNT; row++) {
            formattedAddress = NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase);
            ((MemoryTableModel) table.getModel()).setDisplayAndModelValueAt(formattedAddress, row, 0);
            address += BYTES_PER_ROW;
        }
        // Column headers include address offsets, so translate them too
        for (int column = 1; column < COLUMN_COUNT; column++) {
            table.getColumnModel().getColumn(column).setHeaderValue(getColumnName(column, addressBase));
        }
        table.getTableHeader().repaint();
    }

    /**
     * Update data display to show all values.
     */
    public void updateValues() {
        updateModelForMemoryRange(this.firstAddress);
    }

    /**
     * Reset range of memory addresses to base address of currently selected segment and update display.
     */
    public void resetMemoryRange() {
        baseAddressSelector.getActionListeners()[0].actionPerformed(null); // previously baseAddressButtons[DATA_BASE_ADDRESS_INDEX]
    }

    /**
     * Reset all data display values to 0
     */
    public void resetValues() {
        int valueBase = Application.getGUI().getMainPane().getExecuteTab().getValueDisplayBase();
        TableModel dataModel = table.getModel();
        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 1; column < COLUMN_COUNT; column++) {
                ((MemoryTableModel) dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(0, valueBase), row, column);
            }
        }
        setBaseAddressChooserEnabled(false);
    }

    /**
     * Do this initially and upon reset.
     */
    private void setBaseAddressChooserEnabled(boolean enabled) {
        baseAddressSelector.setEnabled(enabled);
        baseAddressButtons[GLOBAL_POINTER_BASE_ADDRESS_INDEX].setEnabled(enabled);
        baseAddressButtons[STACK_POINTER_BASE_ADDRESS_INDEX].setEnabled(enabled);
        baseAddressButtons[HEAP_BASE_ADDRESS_INDEX].setEnabled(enabled);
        baseAddressButtons[EXTERN_BASE_ADDRESS_INDEX].setEnabled(enabled);
        baseAddressButtons[MMIO_BASE_ADDRESS_INDEX].setEnabled(enabled);
        baseAddressButtons[TEXT_BASE_ADDRESS_INDEX].setEnabled(enabled && settings.selfModifyingCodeEnabled.get());
        baseAddressButtons[KERNEL_DATA_BASE_ADDRESS_INDEX].setEnabled(enabled);
        prevButton.setEnabled(enabled);
        nextButton.setEnabled(enabled);
        baseAddressButtons[DATA_BASE_ADDRESS_INDEX].setEnabled(enabled);
    }

    /**
     * Establish action listeners for the data segment navigation buttons.
     */
    private void initializeBaseAddressButtons() {
        // Set initial states
        setBaseAddressChooserEnabled(false);
        // Add tool tips
        // NOTE: For buttons that are now combo box items, the tool tips are not displayed w/o custom renderer.
        baseAddressButtons[GLOBAL_POINTER_BASE_ADDRESS_INDEX].setToolTipText("View memory around global pointer ($gp)");
        baseAddressButtons[STACK_POINTER_BASE_ADDRESS_INDEX].setToolTipText("View memory around stack pointer ($sp)");
        baseAddressButtons[HEAP_BASE_ADDRESS_INDEX].setToolTipText("View memory around heap starting at " + Binary.intToHexString(Memory.heapBaseAddress));
        baseAddressButtons[KERNEL_DATA_BASE_ADDRESS_INDEX].setToolTipText("View memory around kernel data starting at " + Binary.intToHexString(Memory.kernelDataBaseAddress));
        baseAddressButtons[EXTERN_BASE_ADDRESS_INDEX].setToolTipText("View memory around static globals starting at " + Binary.intToHexString(Memory.externBaseAddress));
        baseAddressButtons[MMIO_BASE_ADDRESS_INDEX].setToolTipText("View memory around memory-mapped I/O starting at " + Binary.intToHexString(Memory.mmioBaseAddress));
        baseAddressButtons[TEXT_BASE_ADDRESS_INDEX].setToolTipText("View memory around program code starting at " + Binary.intToHexString(Memory.textBaseAddress));
        baseAddressButtons[DATA_BASE_ADDRESS_INDEX].setToolTipText("View memory around program data starting at " + Binary.intToHexString(Memory.dataBaseAddress));
        prevButton.setToolTipText("View the previous (lower) memory range (hold down for rapid fire)");
        nextButton.setToolTipText("View the next (higher) memory range (hold down for rapid fire)");

        // Add the action listeners to maintain button state and table contents
        // Currently there is no memory upper bound so next button always enabled.
        baseAddressButtons[GLOBAL_POINTER_BASE_ADDRESS_INDEX].addActionListener(event -> {
            userOrKernelMode = USER_MODE;
            // Get $gp global pointer, but guard against it having value below data segment
            firstAddress = Math.max(Memory.dataSegmentBaseAddress, RegisterFile.getValue(RegisterFile.GLOBAL_POINTER));
            // updateModelForMemoryRange requires argument to be multiple of 4,
            // but for cleaner display we'll make it a multiple of 32 (last nibble is 0).
            // This makes it easier to mentally calculate address from row address + column offset.
            firstAddress = firstAddress - (firstAddress % BYTES_PER_ROW);
            homeAddress = firstAddress;
            updateFirstAddress(firstAddress);
            updateModelForMemoryRange(firstAddress);
        });
        baseAddressButtons[STACK_POINTER_BASE_ADDRESS_INDEX].addActionListener(event -> {
            userOrKernelMode = USER_MODE;
            // Get $sp stack pointer, but guard against it having value below data segment
            firstAddress = Math.max(Memory.dataSegmentBaseAddress, RegisterFile.getValue(RegisterFile.STACK_POINTER));
            // See comment above for baseAddressButtons[GLOBAL_POINTER_BASE_ADDRESS_INDEX]...
            firstAddress = firstAddress - (firstAddress % BYTES_PER_ROW);
            homeAddress = Memory.stackBaseAddress;
            updateFirstAddress(firstAddress);
            updateModelForMemoryRange(firstAddress);
        });
        baseAddressButtons[HEAP_BASE_ADDRESS_INDEX].addActionListener(event -> {
            userOrKernelMode = USER_MODE;
            homeAddress = Memory.heapBaseAddress;
            updateFirstAddress(homeAddress);
            updateModelForMemoryRange(firstAddress);
        });
        baseAddressButtons[EXTERN_BASE_ADDRESS_INDEX].addActionListener(event -> {
            userOrKernelMode = USER_MODE;
            homeAddress = Memory.externBaseAddress;
            updateFirstAddress(homeAddress);
            updateModelForMemoryRange(firstAddress);
        });
        baseAddressButtons[KERNEL_DATA_BASE_ADDRESS_INDEX].addActionListener(event -> {
            userOrKernelMode = KERNEL_MODE;
            homeAddress = Memory.kernelDataBaseAddress;
            updateFirstAddress(homeAddress);
            updateModelForMemoryRange(firstAddress);
        });
        baseAddressButtons[MMIO_BASE_ADDRESS_INDEX].addActionListener(event -> {
            userOrKernelMode = KERNEL_MODE;
            homeAddress = Memory.mmioBaseAddress;
            updateFirstAddress(homeAddress);
            updateModelForMemoryRange(firstAddress);
        });
        baseAddressButtons[TEXT_BASE_ADDRESS_INDEX].addActionListener(event -> {
            userOrKernelMode = USER_MODE;
            homeAddress = Memory.textBaseAddress;
            updateFirstAddress(homeAddress);
            updateModelForMemoryRange(firstAddress);
        });
        baseAddressButtons[DATA_BASE_ADDRESS_INDEX].addActionListener(event -> {
            userOrKernelMode = USER_MODE;
            homeAddress = Memory.dataBaseAddress;
            updateFirstAddress(homeAddress);
            updateModelForMemoryRange(firstAddress);
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
     * {@code prevButton} and {@code nextButton} are also enabled/disabled appropriately.
     */
    private void updateFirstAddress(int address) {
        int lowLimit = (userOrKernelMode == USER_MODE) ? Math.min(Math.min(Memory.textBaseAddress, Memory.dataSegmentBaseAddress), Memory.dataBaseAddress) : Memory.kernelDataBaseAddress;
        int highLimit = (userOrKernelMode == USER_MODE) ? Memory.userHighAddress : Memory.kernelHighAddress;
        
        firstAddress = address;
        if (firstAddress <= lowLimit) {
            firstAddress = lowLimit;
            prevButton.setEnabled(false);
        }
        else {
            prevButton.setEnabled(true);
        }
        if (firstAddress >= highLimit - MEMORY_CHUNK_SIZE) {
            firstAddress = highLimit - MEMORY_CHUNK_SIZE + 1;
            nextButton.setEnabled(false);
        }
        else {
            nextButton.setEnabled(true);
        }
    }

    /**
     * Required by Observer interface.  Called when notified by an Observable that we are registered with.
     * Observables include:
     * The Simulator object, which lets us know when it starts and stops running
     * A delegate of the Memory object, which lets us know of memory operations
     * The Simulator keeps us informed of when simulated MIPS execution is active.
     * This is the only time we care about memory operations.
     *
     * @param observable The Observable object who is notifying us
     * @param obj        Auxiliary object with additional information.
     */
    @Override
    public void update(Observable observable, Object obj) {
        if (observable == Simulator.getInstance()) {
            SimulatorNotice notice = (SimulatorNotice) obj;
            if (notice.action() == SimulatorNotice.SIMULATOR_START) {
                // Simulated MIPS execution starts.  Respond to memory changes if running in timed
                // or stepped mode.
                if (notice.runSpeed() != RunSpeedPanel.UNLIMITED_SPEED || notice.maxSteps() == 1) {
                    Memory.getInstance().addObserver(this);
                    addressHighlighting = true;
                }
            }
            else {
                // Simulated MIPS execution stops.  Stop responding.
                Memory.getInstance().deleteObserver(this);
            }
        }
        else if (observable == settings) {
            // Suspended work in progress. Intended to disable combobox item for text segment. DPS 9-July-2013.
            //baseAddressSelector.getModel().getElementAt(TEXT_BASE_ADDRESS_INDEX)
            //*.setEnabled(settings.getBooleanSetting(Settings.SELF_MODIFYING_CODE_ENABLED));
        }
        else if (obj instanceof MemoryAccessNotice access) { // NOTE: observable != Memory.getInstance() because Memory class delegates notification duty.
            if (access.getAccessType() == AccessNotice.WRITE) {
                int address = access.getAddress();
                // Use the same highlighting technique as for Text Segment -- see
                // AddressCellRenderer class below.
                this.highlightCellForAddress(address);
            }
        }
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
     * "fireContentsChanged()" method worked. DPS 25-Jan-2009
     */
    // TODO: Has this bug been fixed? -Sean Clarke
    private static class CustomComboBoxModel extends DefaultComboBoxModel<String> {
        public CustomComboBoxModel(String[] list) {
            super(list);
        }

        private void forceComboBoxUpdate(int index) {
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
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return data[row][col];
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
        public boolean isCellEditable(int row, int col) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            return col != ADDRESS_COLUMN && !asciiDisplay;
        }

        /**
         * JTable uses this method to determine the default renderer/
         * editor for each cell.
         */
        @Override
        public Class<?> getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        /**
         * Update cell contents in table model.  This method should be called
         * only when user edits cell, so input validation has to be done.  If
         * value is valid, MIPS memory is updated.
         */
        @Override
        public void setValueAt(Object value, int row, int col) {
            int intValue;
            int address = 0;
            try {
                intValue = Binary.stringToInt((String) value);
            }
            catch (NumberFormatException exception) {
                data[row][col] = "INVALID";
                fireTableCellUpdated(row, col);
                return;
            }

            // Calculate address from row and column
            try {
                address = Binary.stringToInt((String) data[row][ADDRESS_COLUMN]) + (col - 1) * BYTES_PER_VALUE; // KENV 1/6/05
            }
            catch (NumberFormatException exception) {
                // Can't really happen since memory addresses are completely under
                // the control of the software.
            }
            // Assures that if changed during MIPS program execution, the update will
            // occur only between MIPS instructions.
            synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                try {
                    Application.memory.setRawWord(address, intValue);
                }
                // Somehow, user was able to display out-of-range address.  Most likely to occur between
                // stack base and kernel.  Also text segment with self-modifying-code setting off.
                catch (AddressErrorException aee) {
                    return;
                }
            }
            int valueBase = Application.getGUI().getMainPane().getExecuteTab().getValueDisplayBase();
            data[row][col] = NumberDisplayBaseChooser.formatNumber(intValue, valueBase);
            fireTableCellUpdated(row, col);
        }

        /**
         * Update cell contents in table model.  Does not affect MIPS memory.
         */
        private void setDisplayAndModelValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }

    /**
     * Special renderer capable of highlighting cells by changing background color.
     * Will set background to highlight color if certain conditions met.
     */
    private class AddressCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int rowFirstAddress = Binary.stringToInt(table.getValueAt(row, ADDRESS_COLUMN).toString());

            this.setHorizontalAlignment(SwingConstants.CENTER);
            if (settings.highlightDataSegment.get() && addressHighlighting && rowFirstAddress == addressRowFirstAddress && column == addressColumn) {
                this.setBackground(settings.getColorSettingByPosition(Settings.DATASEGMENT_HIGHLIGHT_BACKGROUND));
                this.setForeground(settings.getColorSettingByPosition(Settings.DATASEGMENT_HIGHLIGHT_FOREGROUND));
            }
            else {
                this.setBackground(null);
                this.setForeground(null);
            }

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            this.setFont(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT);

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
            return new JTableHeader(columnModel) {
                @Override
                public String getToolTipText(MouseEvent event) {
                    int index = columnModel.getColumnIndexAtX(event.getPoint().x);
                    int toolTipIndex = columnModel.getColumn(index).getModelIndex();
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
            updateFirstAddress(firstAddress - PREV_NEXT_CHUNK_SIZE);
            updateModelForMemoryRange(firstAddress);
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
            updateFirstAddress(firstAddress + PREV_NEXT_CHUNK_SIZE);
            updateModelForMemoryRange(firstAddress);
        }
    }
}
