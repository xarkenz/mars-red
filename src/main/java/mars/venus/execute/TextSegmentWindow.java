package mars.venus.execute;

import mars.Application;
import mars.assembler.BasicStatement;
import mars.assembler.syntax.StatementSyntax;
import mars.mips.hardware.*;
import mars.simulator.*;
import mars.util.Binary;
import mars.venus.EditorFont;
import mars.venus.NumberDisplayBaseChooser;
import mars.venus.VenusUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;

/*
Copyright (c) 2003-2007,  Pete Sanderson and Kenneth Vollmar

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
 * Creates the Text Segment window in the Execute tab of the UI.
 *
 * @author Team JSpim
 */
public class TextSegmentWindow extends JInternalFrame implements SimulatorListener, Memory.Listener {
    private static final int BREAKPOINT_COLUMN = 0;
    private static final int ADDRESS_COLUMN = 1;
    private static final int CODE_COLUMN = 2;
    private static final int BASIC_COLUMN = 3;
    private static final int SOURCE_COLUMN = 4;
    private static final String[] COLUMN_NAMES = {
        "Bkpt",
        "Address",
        "Code",
        "Basic",
        "Source",
    };

    /**
     * Displayed in the Basic and Source columns if existing code is overwritten
     * using the self-modifying code feature.
     */
    private static final String MODIFIED_CODE_MARKER = " ------ ";

    private final VenusUI gui;
    private final JPanel programArgumentsPanel; // DPS 17-July-2008
    private final JTextField programArgumentsTextField; // DPS 17-July-2008
    private static final int PROGRAM_ARGUMENT_TEXTFIELD_COLUMNS = 40;
    private TextSegmentTable table;
    private JScrollPane tableScroller;
    private Object[][] data;
    /*
     * Maintain an int array of code addresses in parallel with ADDRESS_COLUMN,
     * to speed model-row -> text-address mapping.  Maintain a Hashtable of
     * (text-address, model-row) pairs to speed text-address -> model-row mapping.
     * The former is used for breakpoints and changing display base (e.g. base 10
     * to 16); the latter is used for highlighting.  Both structures will remain
     * consistent once set up, since address column is not editable.
     */
    private int[] rowAddresses; // Index is table model row, value is text address
    private Hashtable<Integer, Integer> addressRows; // Key is text address, value is table model row
    private Hashtable<Integer, ModifiedCode> executeMods; // Key is table model row, value is original code, basic, source.
    private TextTableModel tableModel;
    private boolean breakpointsEnabled; // Added 31 Dec 2009
    private int fetchAddress;
    private int executeAddress;
    private TableModelListener tableModelListener;

    /**
     * Constructor, sets up a new JInternalFrame.
     */
    public TextSegmentWindow(VenusUI gui) {
        super("Text Segment", true, false, true, false);
        this.setFrameIcon(null);

        this.gui = gui;
        this.breakpointsEnabled = true;
        this.programArgumentsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.programArgumentsPanel.add(new JLabel("Program Arguments: "));
        this.programArgumentsTextField = new JTextField(PROGRAM_ARGUMENT_TEXTFIELD_COLUMNS);
        this.programArgumentsTextField.setToolTipText("Arguments provided to program at runtime via $a0 (argc) and $a1 (argv)");
        this.programArgumentsPanel.add(this.programArgumentsTextField);

        Simulator.getInstance().addGUIListener(this);
        this.gui.getSettings().addListener(this::updateListeningStatus);
    }

    /**
     * Method to be called once the user compiles the program.
     * Should convert the lines of code over to the table rows and columns.
     */
    public void setupTable() {
        int addressBase = this.gui.getMainPane().getExecuteTab().getAddressDisplayBase();
        this.breakpointsEnabled = true;
        SortedMap<Integer, BasicStatement> statements = Application.assembler.getAssembledStatements();
        this.data = new Object[statements.size()][COLUMN_NAMES.length];
        this.rowAddresses = new int[this.data.length];
        this.addressRows = new Hashtable<>(this.data.length);
        this.executeMods = new Hashtable<>(this.data.length);
        // Get highest source line number to determine # of leading spaces so line numbers will vertically align.
        // In multi-file situation, this will not necessarily be the last line b/c statements contains
        // source lines from all files.  DPS 03-Oct-2010
        int maxSourceLineNumber = 0;
        for (BasicStatement statement : statements.values()) {
            if (statement.getSyntax() != null) {
                int lineNumber = statement.getSyntax().getSourceLine().getLocation().getLineIndex() + 1;
                if (lineNumber > maxSourceLineNumber) {
                    maxSourceLineNumber = lineNumber;
                }
            }
        }
        int maxSourceLineDigits = Integer.toUnsignedString(maxSourceLineNumber).length();
        int leadingSpaces;
        StatementSyntax currentSyntax = null;
        int row = 0;
        for (var entry : statements.entrySet()) {
            int address = entry.getKey();
            BasicStatement statement = entry.getValue();

            this.rowAddresses[row] = address;
            this.addressRows.put(address, row);
            this.data[row][BREAKPOINT_COLUMN] = false;
            this.data[row][ADDRESS_COLUMN] = NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase);
            this.data[row][CODE_COLUMN] = NumberDisplayBaseChooser.formatNumber(statement.getBinaryEncoding(), 16);
            this.data[row][BASIC_COLUMN] = statement.toString();

            String sourceString = "";
            if (statement.getSyntax() != null && statement.getSyntax() != currentSyntax) {
                currentSyntax = statement.getSyntax();
                int lineNumber = currentSyntax.getSourceLine().getLocation().getLineIndex() + 1;
                String lineNumberString = Integer.toUnsignedString(lineNumber);
                leadingSpaces = maxSourceLineDigits - lineNumberString.length();
                String linePrefix = " ".repeat(leadingSpaces) + lineNumberString + ": ";
                String line = EditorFont.substituteSpacesForTabs(currentSyntax.getSourceLine().getContent(), this.gui.getSettings().editorTabSize.get());
                sourceString = linePrefix + line;
            }
            this.data[row][SOURCE_COLUMN] = sourceString;
            row++;
        }
        this.getContentPane().removeAll();
        this.tableModel = new TextTableModel(this.data);
        if (this.tableModelListener != null) {
            this.tableModel.addTableModelListener(this.tableModelListener);
            // Initialize listener
            this.tableModel.fireTableDataChanged();
        }
        this.table = new TextSegmentTable(this.tableModel);

        this.table.getColumnModel().getColumn(BREAKPOINT_COLUMN).setMinWidth(40);
        this.table.getColumnModel().getColumn(BREAKPOINT_COLUMN).setMaxWidth(50);
        this.table.getColumnModel().getColumn(BREAKPOINT_COLUMN).setPreferredWidth(40);
        this.table.getColumnModel().getColumn(BREAKPOINT_COLUMN).setCellRenderer(new CheckBoxTableCellRenderer());

        CodeCellRenderer codeCellRenderer = new CodeCellRenderer();

        this.table.getColumnModel().getColumn(ADDRESS_COLUMN).setMinWidth(80);
        this.table.getColumnModel().getColumn(ADDRESS_COLUMN).setMaxWidth(100);
        this.table.getColumnModel().getColumn(ADDRESS_COLUMN).setPreferredWidth(90);
        this.table.getColumnModel().getColumn(ADDRESS_COLUMN).setCellRenderer(codeCellRenderer);

        this.table.getColumnModel().getColumn(CODE_COLUMN).setMinWidth(80);
        this.table.getColumnModel().getColumn(CODE_COLUMN).setMaxWidth(100);
        this.table.getColumnModel().getColumn(CODE_COLUMN).setPreferredWidth(90);
        this.table.getColumnModel().getColumn(CODE_COLUMN).setCellRenderer(codeCellRenderer);

        this.table.getColumnModel().getColumn(BASIC_COLUMN).setMinWidth(120);
        this.table.getColumnModel().getColumn(BASIC_COLUMN).setPreferredWidth(120);
        this.table.getColumnModel().getColumn(BASIC_COLUMN).setCellRenderer(codeCellRenderer);

        this.table.getColumnModel().getColumn(SOURCE_COLUMN).setMinWidth(120);
        this.table.getColumnModel().getColumn(SOURCE_COLUMN).setPreferredWidth(400);
        this.table.getColumnModel().getColumn(SOURCE_COLUMN).setCellRenderer(codeCellRenderer);

        // Re-order columns according to current preference...
        this.reorderColumns();
        // Add listener to catch column re-ordering for updating settings.
        this.table.getColumnModel().addColumnModelListener(new ColumnOrderListener());

        this.tableScroller = new JScrollPane(this.table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.getContentPane().add(this.tableScroller);
        if (this.gui.getSettings().useProgramArguments.get()) {
            this.addProgramArgumentsPanel();
        }

        this.updateListeningStatus();
        this.getContentPane().revalidate();
        this.updateHighlighting();
    }

    /**
     * Get program arguments from text field at the bottom of the text segment window.
     *
     * @return The list of program arguments.
     * @author DPS 17-July-2008
     */
    public List<String> getProgramArguments() {
        String text = this.programArgumentsTextField.getText();
        return (text.isBlank()) ? List.of() : List.of(text.split("\\s+"));
    }

    /**
     * Allow the user to enter program arguments in a panel at the bottom of the text segment window.
     *
     * @author DPS 17-July-2008
     */
    public void addProgramArgumentsPanel() {
        // Don't add it if text segment window blank (file closed or no assemble yet)
        if (this.getContentPane() != null && this.getContentPane().getComponentCount() > 0) {
            this.getContentPane().add(this.programArgumentsPanel, BorderLayout.NORTH);
            this.getContentPane().validate();
        }
    }

    /**
     * Hide the panel for entering program arguments.
     *
     * @author DPS 17-July-2008
     */
    public void removeProgramArgumentsPanel() {
        if (this.getContentPane() != null) {
            this.getContentPane().remove(this.programArgumentsPanel);
            this.getContentPane().validate();
        }
    }

    /**
     * Remove all components.
     */
    public void clearWindow() {
        this.getContentPane().removeAll();
    }

    /**
     * Assign listener to Table model.  Used for breakpoints, since that is the only editable
     * column in the table.  Since table model objects are transient (get a new one with each
     * successful assemble), this method will simply keep the identity of the listener then
     * add it as a listener each time a new table model object is created.  Limit 1 listener.
     */
    public void registerTableModelListener(TableModelListener listener) {
        this.tableModelListener = listener;
    }

    /**
     * Redisplay the addresses.  This should only be done when address display base is
     * modified (e.g. between base 16 hex and base 10 dec).
     */
    public void updateCodeAddresses() {
        if (this.getContentPane().getComponentCount() == 0) {
            // No content to change
            return;
        }
        int addressBase = this.gui.getMainPane().getExecuteTab().getAddressDisplayBase();
        for (int row = 0; row < this.rowAddresses.length; row++) {
            String formattedAddress = NumberDisplayBaseChooser.formatUnsignedInteger(this.rowAddresses[row], addressBase);
            this.table.getModel().setValueAt(formattedAddress, row, ADDRESS_COLUMN);
        }
    }

    /**
     * Redisplay the basic statements.  This should only be done when address or value display base is
     * modified (e.g. between base 16 hex and base 10 dec).
     */
    public void updateBasicStatements() {
        if (this.getContentPane().getComponentCount() == 0) {
            // No content to change
            return;
        }
        SortedMap<Integer, BasicStatement> statements = Application.assembler.getAssembledStatements();
        int row = 0;
        for (var entry : statements.entrySet()) {
            BasicStatement statement = entry.getValue();
            // Loop has been extended to cover self-modifying code.  If code at this memory location has been
            // modified at runtime, construct a BasicStatement from the current address and binary code
            // then display its basic code.  DPS 11-July-2013
            if (this.executeMods.get(row) == null) {
                // Not modified, so use original logic
                this.table.getModel().setValueAt(statement.toString(), row, BASIC_COLUMN);
            }
            else {
                try {
                    statement = Application.instructionSet.getDecoder().decodeStatement(
                        Binary.decodeInteger(this.table.getModel().getValueAt(row, CODE_COLUMN).toString())
                    );
                    this.table.getModel().setValueAt(statement.toString(), row, BASIC_COLUMN);
                }
                catch (NumberFormatException exception) {
                    // Should never happen, but just in case...
                    this.table.getModel().setValueAt("", row, BASIC_COLUMN);
                }
            }
            row++;
        }
    }

    @Override
    public void memoryWritten(int address, int length, int value, int wordAddress, int wordValue) {
        // Translate the address into table model row and modify the values in that row accordingly.
        int row;
        try {
            row = this.findRowForAddress(wordAddress);
        }
        catch (IndexOutOfBoundsException exception) {
            // Address modified is outside the range of original program, ignore
            return;
        }

        String strValue = Binary.intToHexString(wordValue);
        String strBasic;
        String strSource;
        ModifiedCode modification = this.executeMods.get(row);
        if (modification == null) {
            // Not already modified and new code is same as original, so do nothing
            if (this.tableModel.getValueAt(row, CODE_COLUMN).equals(strValue)) {
                return;
            }
            modification = new ModifiedCode(
                row,
                this.tableModel.getValueAt(row, CODE_COLUMN),
                this.tableModel.getValueAt(row, BASIC_COLUMN),
                this.tableModel.getValueAt(row, SOURCE_COLUMN)
            );
            this.executeMods.put(row, modification);
            // Make a BasicStatement and get basic code to display in BASIC_COLUMN
            strBasic = Application.instructionSet.getDecoder().decodeStatement(wordValue).toString();
            strSource = MODIFIED_CODE_MARKER;
        }
        else {
            // If restored to original value, restore the basic and source
            // (this will be the case upon backstepping)
            if (modification.code.equals(strValue)) {
                strBasic = modification.basic.toString();
                strSource = modification.source.toString();
                // Remove from executeMods since we are back to original
                this.executeMods.remove(row);
            }
            else {
                // Make a BasicStatement and get basic code to display in BASIC_COLUMN
                strBasic = Application.instructionSet.getDecoder().decodeStatement(wordValue).toString();
                strSource = MODIFIED_CODE_MARKER;
            }
        }

        // For the code column, we don't want to do the following:
        //       tableModel.setValueAt(strValue, row, CODE_COLUMN)
        // because that method will write to memory using Memory.setRawWord() which will
        // trigger notification to observers, which brings us back to here!!!  Infinite
        // indirect recursion results.  Neither fun nor productive.  So what happens is
        // this: (1) change to memory cell causes setValueAt() to be automatically be
        // called.  (2) it updates the memory cell which in turn notifies us which invokes
        // the update() method - the method we're in right now.  All we need to do here is
        // update the table model then notify the controller/view to update its display.
        this.tableModel.setDisplayAndModelValueAt(strValue, row, CODE_COLUMN);
        this.tableModel.setDisplayAndModelValueAt(strBasic, row, BASIC_COLUMN);
        this.tableModel.setDisplayAndModelValueAt(strSource, row, SOURCE_COLUMN);

        // Let's update the value displayed in the DataSegmentWindow too.  But it only observes memory while
        // the MIPS program is running, and even then only in timed or step mode.  There are good reasons
        // for that.  So we'll send it an artificial memory write event.
        this.gui.getMainPane().getExecuteTab().getDataSegmentWindow().memoryWritten(address, length, value, wordAddress, wordValue);
    }

    @Override
    public void simulatorStarted(SimulatorStartEvent event) {
        this.updateListeningStatus();
    }

    @Override
    public void simulatorPaused(SimulatorPauseEvent event) {
        this.stopObservingMemory();
        this.updateHighlighting();
    }

    @Override
    public void simulatorFinished(SimulatorFinishEvent event) {
        this.stopObservingMemory();
        this.updateHighlighting();
    }

    @Override
    public void simulatorStepped() {
        this.updateHighlighting();
    }

    /**
     * Called by RunResetAction to restore display of any table rows that were
     * overwritten due to self-modifying code feature.
     */
    public void resetModifiedSourceCode() {
        if (this.executeMods != null && !this.executeMods.isEmpty()) {
            for (ModifiedCode modifiedCode : this.executeMods.values()) {
                this.tableModel.setValueAt(modifiedCode.code, modifiedCode.row, CODE_COLUMN);
                this.tableModel.setValueAt(modifiedCode.basic, modifiedCode.row, BASIC_COLUMN);
                this.tableModel.setValueAt(modifiedCode.source, modifiedCode.row, SOURCE_COLUMN);
            }
            this.executeMods.clear();
        }
    }

    /**
     * Returns number of breakpoints currently set.
     *
     * @return number of current breakpoints
     */
    public int getBreakpointCount() {
        int breakpointCount = 0;
        if (this.data != null) {
            for (Object[] rowData : this.data) {
                if (rowData[BREAKPOINT_COLUMN] == Boolean.TRUE) {
                    breakpointCount++;
                }
            }
        }
        return breakpointCount;
    }

    /**
     * Returns array of current breakpoints, each represented by a MIPS program counter address.
     * These are stored in the BREAK_COLUMN of the table model.
     *
     * @return int array of breakpoints, sorted by PC address, or null if there are none.
     */
    public int[] getSortedBreakPointsArray() {
        int breakpointCount = this.getBreakpointCount();
        // Added second condition.  DPS 31-Dec-2009
        if (breakpointCount == 0 || !this.breakpointsEnabled) {
            return null;
        }
        int[] breakpoints = new int[breakpointCount];
        breakpointCount = 0;
        for (int row = 0; row < this.data.length; row++) {
            if (this.data[row][BREAKPOINT_COLUMN] == Boolean.TRUE) {
                breakpoints[breakpointCount++] = this.rowAddresses[row];
            }
        }
        Arrays.sort(breakpoints);
        return breakpoints;
    }

    /**
     * Clears all breakpoints that have been set since last assemble, and
     * updates the display of the breakpoint column.
     */
    public void clearAllBreakpoints() {
        for (int row = 0; row < this.tableModel.getRowCount(); row++) {
            if (this.data[row][BREAKPOINT_COLUMN] == Boolean.TRUE) {
                // Must use this method to assure display updated and listener notified
                this.tableModel.setValueAt(Boolean.FALSE, row, BREAKPOINT_COLUMN);
            }
        }
        // Handles an obscure situation: if you click to set some breakpoints then "immediately" clear them
        // all using the shortcut (CTRL-K), the last checkmark set is not removed even though the breakpoint
        // is removed (tableModel.setValueAt(Boolean.FALSE, i, BREAK_COLUMN)) and all the other checkmarks
        // are removed.  The checkmark remains although if you subsequently run the program it will blow
        // through because the data model cell really has been cleared (contains false).  Occurs only when
        // the last checked breakpoint check box still has the "focus".  There is but one renderer and editor
        // per column.  Getting the renderer and setting it "setSelected(false)" will not work.  You have
        // to get the editor instead.  DPS 7-Aug-2006
        ((JCheckBox) ((DefaultCellEditor) this.table.getCellEditor(0, BREAKPOINT_COLUMN)).getComponent()).setSelected(false);
    }

    public void scrollToRow(int row) {
        Rectangle rect = this.table.getCellRect(row, 0, true);
        // Make the next row visible as well
        rect.height *= 2;
        this.table.scrollRectToVisible(rect);
    }

    /**
     * Highlights the source code line whose address matches the current
     * program counter value.  This is used for stepping through code
     * execution and when reaching breakpoints.
     */
    public void updateHighlighting() {
        this.fetchAddress = Processor.getProgramCounter();
        this.executeAddress = Processor.getExecuteProgramCounter();

        // Make sure executing instruction is visible on screen
        try {
            int row = this.findRowForAddress(this.executeAddress);
            this.scrollToRow(row);
        }
        catch (IndexOutOfBoundsException exception) {
            // Out of bounds address, no scrolling will occur
        }

        // Repaint with the updated highlighting
        this.table.repaint();
    }

    /**
     * Scroll the viewport so the step (table row) at the given text segment address
     * is visible, vertically centered if possible, and selected.
     * Developed July 2007 for new feature that shows source code step where
     * label is defined when that label is clicked on in the Label Window.
     *
     * @param address text segment address of source code step.
     */
    public void selectStepAtAddress(int address) {
        int addressRow;
        try {
            addressRow = this.findRowForAddress(address);
        }
        catch (IndexOutOfBoundsException exception) {
            return;
        }
        // Scroll to assure desired row is centered in view port.
        int addressSourceColumn = this.table.convertColumnIndexToView(SOURCE_COLUMN);
        Rectangle sourceCell = this.table.getCellRect(addressRow, addressSourceColumn, true);
        double cellHeight = sourceCell.getHeight();
        double viewHeight = this.tableScroller.getViewport().getExtentSize().getHeight();
        int numberOfVisibleRows = (int) (viewHeight / cellHeight);
        int newViewPositionY = Math.max((int) ((double) (addressRow - (numberOfVisibleRows / 2)) * cellHeight), 0);
        this.tableScroller.getViewport().setViewPosition(new Point(0, newViewPositionY));
        // Select the source code cell for this row by generating a fake Mouse Pressed event
        // and explicitly invoking the table's mouse listener.
        MouseEvent fakeMouseEvent = new MouseEvent(
            this.table,
            MouseEvent.MOUSE_PRESSED,
            new Date().getTime(),
            MouseEvent.BUTTON1_DOWN_MASK,
            (int) sourceCell.getX() + 1,
            (int) sourceCell.getY() + 1,
            1,
            false
        );
        for (MouseListener mouseListener : this.table.getMouseListeners()) {
            mouseListener.mousePressed(fakeMouseEvent);
        }
    }

    /**
     * Enable or disable all items in the Breakpoints column.
     */
    public void toggleBreakpoints() {
        // Already programmed to toggle by clicking on column header, so we'll create
        // a fake mouse event with coordinates on that header then generate the fake
        // event on its mouse listener.
        Rectangle rect = this.table.getHeaderRect(BREAKPOINT_COLUMN);
        MouseEvent fakeMouseEvent = new MouseEvent(
            this.table,
            MouseEvent.MOUSE_CLICKED,
            new Date().getTime(),
            MouseEvent.BUTTON1_DOWN_MASK,
            (int) rect.getX(),
            (int) rect.getY(),
            1,
            false
        );
        MouseListener[] mouseListeners = this.table.tableHeader.getMouseListeners();
        for (MouseListener mouseListener : mouseListeners) {
            mouseListener.mouseClicked(fakeMouseEvent);
        }
    }

    /**
     * Convenience method to add this as a listener of the text segment in memory.
     */
    public void startObservingMemory() {
        Memory.getInstance().addListener(
            this,
            Memory.getInstance().getLayout().textRange.minAddress(),
            Memory.getInstance().getLayout().textRange.maxAddress()
        );
    }

    /**
     * Convenience method to remove this as a listener of memory.
     */
    public void stopObservingMemory() {
        Memory.getInstance().removeListener(this);
    }

    /**
     * Convenience method to remove this as a listener of memory, then add again if the self-modifying code
     * feature is enabled in the settings.
     */
    public void updateListeningStatus() {
        this.stopObservingMemory();
        if (this.gui.getSettings().selfModifyingCodeEnabled.get()) {
            this.startObservingMemory();
        }
    }

    /**
     * Re-order the Text segment columns according to saved preferences.
     */
    private void reorderColumns() {
        TableColumnModel oldModel = this.table.getColumnModel();
        TableColumnModel newModel = new DefaultTableColumnModel();
        // Apply ordering only if correct number of columns.
        int[] savedColumnOrder = this.getSavedColumnOrder();
        if (savedColumnOrder.length == this.table.getColumnCount()) {
            for (int column : savedColumnOrder) {
                newModel.addColumn(oldModel.getColumn(column));
            }
            this.table.setColumnModel(newModel);
        }
    }

    private int[] getSavedColumnOrder() {
        String columnOrder = this.gui.getSettings().textSegmentColumnOrder.get();
        return Arrays.stream(columnOrder.split("\\s+"))
            .mapToInt(Integer::parseInt)
            .toArray();
    }

    /**
     * Helper method to find the table row corresponding to the given
     * text segment address.  This method is called by
     * a couple different public methods.
     *
     * @param address The address to find the row for.
     * @return The table row corresponding to this address.
     * @throws IndexOutOfBoundsException Thrown if the address does not correspond to any row in the table.
     */
    public int findRowForAddress(int address) throws IndexOutOfBoundsException {
        Integer row = this.addressRows.get(address);
        if (row != null) {
            return row;
        }
        else {
            // Address not found in map
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * For whatever reason, FlatLaf seems to forget what the frame icon is set to after this method is called.
     * So, this method has been overwritten to set the frame icon back to <code>null</code> as desired.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        this.setFrameIcon(null);
    }

    /**
     * Inner class to implement the Table model for this JTable.
     */
    private class TextTableModel extends AbstractTableModel {
        private final Object[][] data;

        public TextTableModel(Object[][] data) {
            this.data = data;
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            return this.data[row][column];
        }

        /**
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the break column would contain text ("true"/"false"),
         * rather than a check box.
         */
        @Override
        public Class<?> getColumnClass(int column) {
            return this.getValueAt(0, column).getClass();
        }

        /**
         * Only Column #1, the Breakpoint, can be edited.
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            // Note that the data/cell address is constant, no matter where the cell appears onscreen.
            return column == BREAKPOINT_COLUMN
                || (column == CODE_COLUMN && TextSegmentWindow.this.gui.getSettings().selfModifyingCodeEnabled.get());
        }

        /**
         * Set cell contents in the table model. Overrides inherited empty method.
         * Straightforward process except for the Code column.
         */
        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column != CODE_COLUMN) {
                this.setDisplayAndModelValueAt(value, row, column);
                return;
            }

            // Handle changes in the Code column
            if (value.equals(this.getValueAt(row, column))) {
                return;
            }

            int intValue;
            try {
                intValue = Binary.decodeInteger(value.toString());
            }
            catch (NumberFormatException exception) {
                return;
            }

            // Retrieve address based on row
            int address = TextSegmentWindow.this.rowAddresses[row];
            // Assures that if changed during MIPS program execution, the update will
            // occur only between MIPS instructions.
            Simulator.getInstance().changeState(() -> {
                try {
                    Memory.getInstance().storeWord(address, intValue, true);
                }
                catch (AddressErrorException exception) {
                    // Somehow, user was able to display out-of-range address.  Most likely to occur between
                    // stack base and kernel.
                }
            });
        }

        /**
         * Update cell contents in table model.  Does not affect underlying memory.
         */
        public void setDisplayAndModelValueAt(Object value, int row, int column) {
            this.data[row][column] = value;
            this.fireTableCellUpdated(row, column);
        }
    }

    private record ModifiedCode(int row, Object code, Object basic, Object source) {}

    /**
     * A custom table cell renderer that we'll use to highlight the current line of
     * source code when executing using Step or breakpoint.
     */
    private class CodeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int address = TextSegmentWindow.this.rowAddresses[row];
            Font font;

            this.setHorizontalAlignment(SwingConstants.LEFT);
            if (address == TextSegmentWindow.this.executeAddress) {
                this.setBackground(TextSegmentWindow.this.gui.getSettings().textSegmentExecuteHighlightBackground.getOrDefault());
                this.setForeground(TextSegmentWindow.this.gui.getSettings().textSegmentExecuteHighlightForeground.getOrDefault());
                font = TextSegmentWindow.this.gui.getSettings().tableHighlightFont.get();
            }
            else if (address == TextSegmentWindow.this.fetchAddress) {
                this.setBackground(TextSegmentWindow.this.gui.getSettings().textSegmentFetchHighlightBackground.getOrDefault());
                this.setForeground(TextSegmentWindow.this.gui.getSettings().textSegmentFetchHighlightForeground.getOrDefault());
                font = TextSegmentWindow.this.gui.getSettings().tableHighlightFont.get();
            }
            else {
                this.setBackground(null);
                this.setForeground(null);
                font = TextSegmentWindow.this.gui.getSettings().tableFont.get();
            }

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            this.setFont(font);

            return this;
        }
    }


    /**
     * Cell renderer for Breakpoint column.  We can use this to enable/disable breakpoint checkboxes with
     * a single action.  This class blatantly copied/pasted from
     * <a href="http://www.javakb.com/Uwe/Forum.aspx/java-gui/1451/Java-TableCellRenderer-for-a-boolean-checkbox-field">here</a>.
     * Slightly customized.
     *
     * @author DPS 31-Dec-2009
     */
    private class CheckBoxTableCellRenderer extends JCheckBox implements TableCellRenderer {
        private Border noFocusBorder;
        private Border focusBorder;

        public CheckBoxTableCellRenderer() {
            super();
            this.setContentAreaFilled(true);
            this.setBorderPainted(true);
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setVerticalAlignment(SwingConstants.CENTER);

            /*
             * Use this if you want to add "instant" recognition of breakpoint changes
             * during simulation run.  Currently, the simulator gets array of breakpoints
             * only when "Go" is selected.  Thus the system does not respond to breakpoints
             * added/removed during unlimited/timed execution.  In order for it to do so,
             * we need to be informed of such changes and the ItemListener below will do this.
             * Then the item listener needs to inform the SimThread object so it can request
             * a fresh breakpoint array.  That would make SimThread an observer.  Synchronization
             * will come into play in the SimThread class?  It could get complicated, which
             * is why I'm dropping it for release 3.8.  DPS 31-Dec-2009
             *
             * this.addItemListener(event -> {
             *     String what = "state changed";
             *     if (event.getStateChange() == ItemEvent.SELECTED) what = "selected";
             *     if (event.getStateChange() == ItemEvent.DESELECTED) what = "deselected";
             *     System.out.println("Item " + what);
             * });
             *
             * For a different approach, see RunClearBreakpointsAction.java.  This menu item registers
             * as a TableModelListener by calling the TextSegmentWindow's registerTableModelListener
             * method.  Then it is notified when the table model changes, and this occurs whenever
             * the user clicks on a breakpoint checkbox!  Using this approach, the SimThread registers
             * similarly.  A "GUI guard" is not needed in SimThread because it extends SwingWorker and
             * thus is only invoked when the IDE is present (never when running MARS in command mode).
             */
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (table != null) {
                if (isSelected) {
                    this.setForeground(table.getSelectionForeground());
                    this.setBackground(table.getSelectionBackground());
                }
                else {
                    this.setForeground(table.getForeground());
                    this.setBackground(table.getBackground());
                }

                this.setEnabled(table.isEnabled() && TextSegmentWindow.this.breakpointsEnabled);
                this.setComponentOrientation(table.getComponentOrientation());

                if (hasFocus) {
                    if (this.focusBorder == null) {
                        this.focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
                    }
                    this.setBorder(this.focusBorder);
                }
                else {
                    if (this.noFocusBorder == null) {
                        if (this.focusBorder == null) {
                            this.focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
                        }
                        if (this.focusBorder != null) {
                            this.noFocusBorder = new EmptyBorder(this.focusBorder.getBorderInsets(this));
                        }
                    }
                    this.setBorder(this.noFocusBorder);
                }
                this.setSelected(value == Boolean.TRUE);
            }

            return this;
        }
    }

    // JTable subclass to provide custom tool tips for each of the
    // text table column headers. From Sun's JTable tutorial.
    // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
    private class TextSegmentTable extends JTable {
        private JTableHeader tableHeader;

        public TextSegmentTable(TextTableModel model) {
            super(model);
            // Prevents cells in row from being highlighted when user clicks on breakpoint checkbox
            this.setRowSelectionAllowed(false);
        }

        private static final String[] COLUMN_TOOL_TIPS = {
            "If checked, will set an execution breakpoint. Click header to disable/enable breakpoints", // Break
            "Text segment address of binary instruction code", // Address
            "32-bit binary MIPS instruction", // Code
            "Basic assembler instruction", // Basic
            "Source code line", // Source
        };

        // Implement table header tool tips.
        @Override
        protected JTableHeader createDefaultTableHeader() {
            this.tableHeader = new TextTableHeader(this.columnModel);
            return this.tableHeader;
        }

        /**
         * Given the model index of a column header, will return rectangle
         * rectangle of displayed header (may be in different position due to
         * column re-ordering).
         */
        public Rectangle getHeaderRect(int modelIndex) {
            for (int column = 0; column < this.columnModel.getColumnCount(); column++) {
                if (this.columnModel.getColumn(column).getModelIndex() == modelIndex) {
                    return this.tableHeader.getHeaderRect(column);
                }
            }
            return this.tableHeader.getHeaderRect(modelIndex);
        }

        /**
         * Customized table header that will both display tool tip when
         * mouse hovers over each column, and also enable/disable breakpoints
         * when mouse is clicked on breakpoint column.  Both are
         * customized based on the column under the mouse.
         */
        private class TextTableHeader extends JTableHeader implements MouseListener {
            public TextTableHeader(TableColumnModel model) {
                super(model);
                this.addMouseListener(this);
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                int index = this.columnModel.getColumnIndexAtX(event.getPoint().x);
                int realIndex = this.columnModel.getColumn(index).getModelIndex();
                return COLUMN_TOOL_TIPS[realIndex];
            }

            // When user clicks on breakpoint column header, breakpoints are
            // toggled (enabled/disabled).  DPS 31-Dec-2009
            @Override
            public void mouseClicked(MouseEvent event) {
                int index = this.columnModel.getColumnIndexAtX(event.getPoint().x);
                int realIndex = this.columnModel.getColumn(index).getModelIndex();
                if (realIndex == BREAKPOINT_COLUMN) {
                    JCheckBox check = ((JCheckBox) ((DefaultCellEditor) this.table.getCellEditor(0, index)).getComponent());
                    TextSegmentWindow.this.breakpointsEnabled = !TextSegmentWindow.this.breakpointsEnabled;
                    check.setEnabled(TextSegmentWindow.this.breakpointsEnabled);

                    this.table.tableChanged(new TableModelEvent(
                        TextSegmentWindow.this.tableModel,
                        0,
                        TextSegmentWindow.this.data.length - 1,
                        BREAKPOINT_COLUMN
                    ));
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {}

            @Override
            public void mouseExited(MouseEvent event) {}

            @Override
            public void mousePressed(MouseEvent event) {}

            @Override
            public void mouseReleased(MouseEvent event) {}
        }
    }

    /**
     * Will capture movement of text columns.  This info goes into persistent store.
     */
    private class ColumnOrderListener implements TableColumnModelListener {
        // When column moves, save the new column order.
        @Override
        public void columnMoved(TableColumnModelEvent event) {
            int[] columnOrder = new int[TextSegmentWindow.this.table.getColumnCount()];
            for (int column = 0; column < columnOrder.length; column++) {
                columnOrder[column] = TextSegmentWindow.this.table.getColumnModel().getColumn(column).getModelIndex();
            }
            // If movement is slow, this event may fire multiple times w/o
            // actually changing the column order.  If new column order is
            // same as previous, do not save changes to persistent store.
            if (!Arrays.equals(columnOrder, TextSegmentWindow.this.getSavedColumnOrder())) {
                // Join column numbers into a space-separated string
                String columnOrderString = String.join(" ", Arrays.stream(columnOrder).mapToObj(Integer::toString).toList());
                TextSegmentWindow.this.gui.getSettings().textSegmentColumnOrder.set(columnOrderString);
            }
        }

        @Override
        public void columnAdded(TableColumnModelEvent event) {}

        @Override
        public void columnRemoved(TableColumnModelEvent event) {}

        @Override
        public void columnMarginChanged(ChangeEvent event) {}

        @Override
        public void columnSelectionChanged(ListSelectionEvent event) {}
    }
}
