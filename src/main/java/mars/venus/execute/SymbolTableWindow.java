package mars.venus.execute;

import mars.Application;
import mars.assembler.Symbol;
import mars.assembler.SymbolTable;
import mars.mips.hardware.Memory;
import mars.util.Binary;
import mars.venus.SimpleCellRenderer;
import mars.venus.NumberDisplayBaseChooser;
import mars.venus.VenusUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
   
/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Represents the symbol table window, which is a type of JInternalFrame.  Venus user
 * can view MIPS program labels and their addresses.
 *
 * @author Sanderson and Team JSpim
 */
public class SymbolTableWindow extends JInternalFrame {
    private final VenusUI gui;
    private final JPanel labelPanel;
    private final JCheckBox dataLabels;
    private final JCheckBox textLabels;
    private final List<SymbolTableDisplay> symbolTableDisplays;

    private static final int LABEL_COLUMN = 0;
    private static final int ADDRESS_COLUMN = 1;
    private static final String[] COLUMN_NAMES = {
        "Label",
        "Address",
    };
    private static final String[] COLUMN_TOOL_TIPS = {
        "Programmer-defined label (identifier).", // Label
        "Text or data segment address the label corresponds to.", // Address
    };
    private final Action[] columnSortActions = {
        new ColumnSortAction(LABEL_COLUMN),
        new ColumnSortAction(ADDRESS_COLUMN),
    };

    // Use 8-state machine to track sort status for displaying tables
    // State  Sort Column  Name sort order  Address sort order  Click Label  Click Address
    // 0      Addr         ascending        ascending           4            1
    // 1      Addr         ascending        descending          5            0
    // 2      Addr         descending       ascending           6            3
    // 3      Addr         descending       descending          7            2
    // 4      Name         ascending        ascending           6            0
    // 5      Name         ascending        descending          7            1
    // 6      Name         descending       ascending           4            2
    // 7      Name         descending       descending          5            3
    // "Click Label" column shows which state to go to when Label column is clicked.
    // "Click Address" column shows which state to go to when Address column is clicked.

    // The array of comparators; index corresponds to state in table above.
    private static final List<Comparator<Symbol>> TABLE_SORTING_COMPARATORS = List.of(
        /* 0 */ new LabelAddressAscendingComparator(),
        /* 1 */ new LabelAddressAscendingComparator().reversed(),
        /* 2 */ new LabelAddressAscendingComparator(),
        /* 3 */ new LabelAddressAscendingComparator().reversed(),
        /* 4 */ new LabelNameAscendingComparator(),
        /* 5 */ new LabelNameAscendingComparator(),
        /* 6 */ new LabelNameAscendingComparator().reversed(),
        /* 7 */ new LabelNameAscendingComparator().reversed()
    );
    // The array of state transitions; primary index corresponds to state in table above,
    // secondary index corresponds to table columns (0==label name, 1==address).
    private static final int[][] SORT_STATE_TRANSITIONS = {
        /* 0 */ { 4, 1 },
        /* 1 */ { 5, 0 },
        /* 2 */ { 6, 3 },
        /* 3 */ { 7, 2 },
        /* 4 */ { 6, 0 },
        /* 5 */ { 7, 1 },
        /* 6 */ { 4, 2 },
        /* 7 */ { 5, 3 },
    };
    // The array of column headings; index corresponds to state in table above.
    private static final String SORT_SYMBOL_SEPARATOR = "  ";
    private static final char ASCENDING_SYMBOL = '∧'; // "points" up to indicate ascending sort
    private static final char DESCENDING_SYMBOL = '∨'; // "points" down to indicate descending sort
    private static final String[][] COLUMN_SORT_BUTTON_LABELS = {
        /* 0 */ { COLUMN_NAMES[0], COLUMN_NAMES[1] + SORT_SYMBOL_SEPARATOR + ASCENDING_SYMBOL },
        /* 1 */ { COLUMN_NAMES[0], COLUMN_NAMES[1] + SORT_SYMBOL_SEPARATOR + DESCENDING_SYMBOL },
        /* 2 */ { COLUMN_NAMES[0], COLUMN_NAMES[1] + SORT_SYMBOL_SEPARATOR + ASCENDING_SYMBOL },
        /* 3 */ { COLUMN_NAMES[0], COLUMN_NAMES[1] + SORT_SYMBOL_SEPARATOR + DESCENDING_SYMBOL },
        /* 4 */ { COLUMN_NAMES[0] + SORT_SYMBOL_SEPARATOR + ASCENDING_SYMBOL, COLUMN_NAMES[1] },
        /* 5 */ { COLUMN_NAMES[0] + SORT_SYMBOL_SEPARATOR + ASCENDING_SYMBOL, COLUMN_NAMES[1] },
        /* 6 */ { COLUMN_NAMES[0] + SORT_SYMBOL_SEPARATOR + DESCENDING_SYMBOL, COLUMN_NAMES[1] },
        /* 7 */ { COLUMN_NAMES[0] + SORT_SYMBOL_SEPARATOR + DESCENDING_SYMBOL, COLUMN_NAMES[1] },
    };

    // Current sort state (0-7, see table above).  Will be set from saved Settings in constructor.
    private int sortState;

    /**
     * Constructor for the symbol table window.
     */
    public SymbolTableWindow(VenusUI gui) {
        super("Symbol Table", true, false, true, false);
        this.setFrameIcon(null);

        this.gui = gui;
        this.sortState = this.gui.getSettings().symbolTableSortState.get();
        if (this.sortState < 0 || this.sortState >= SORT_STATE_TRANSITIONS.length) {
            this.sortState = 0;
        }
        this.symbolTableDisplays = new ArrayList<>();

        this.labelPanel = new JPanel(new BorderLayout());
        this.getContentPane().add(this.labelPanel, BorderLayout.CENTER);

        this.dataLabels = new JCheckBox("Data Segment", true);
        this.textLabels = new JCheckBox("Text Segment", true);
        ItemListener labelListener = (event) -> {
            for (SymbolTableDisplay labelsForSymbolTable : this.symbolTableDisplays) {
                labelsForSymbolTable.generateLabelTable();
            }
        };
        this.dataLabels.addItemListener(labelListener);
        this.textLabels.addItemListener(labelListener);
        this.dataLabels.setToolTipText("If checked, will display labels defined in the data segment.");
        this.textLabels.setToolTipText("If checked, will display labels defined in the text segment.");

        JPanel optionsPane = new JPanel();
        optionsPane.add(this.dataLabels);
        optionsPane.add(this.textLabels);
        optionsPane.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Table.gridColor"), 1));
        this.getContentPane().add(optionsPane, BorderLayout.SOUTH);
    }

    /**
     * Initialize table of labels (symbol table).
     */
    public void setupTable() {
        this.clearWindow();
        this.labelPanel.add(this.generateLabelScrollPane(), BorderLayout.CENTER);
    }

    /**
     * Clear the window.
     */
    public void clearWindow() {
        this.labelPanel.removeAll();
    }

    private JScrollPane generateLabelScrollPane() {
        this.symbolTableDisplays.clear();
        SymbolTableDisplay globalSymbolTableDisplay = new SymbolTableDisplay(null);
        this.symbolTableDisplays.add(globalSymbolTableDisplay);
        for (String sourceFilename : Application.assembler.getSourceFilenames()) {
            this.symbolTableDisplays.add(new SymbolTableDisplay(sourceFilename));
        }

        Box allSymbolTables = Box.createVerticalBox();
        for (SymbolTableDisplay symbolTableDisplay : this.symbolTableDisplays) {
            if (symbolTableDisplay.hasSymbols()) {
                JTable table = symbolTableDisplay.generateLabelTable();
                // The following is selfish on my part.  Column re-ordering doesn't work correctly when
                // displaying multiple symbol tables; the headers re-order but the columns do not.
                // Given the low perceived benefit of reordering displayed symbol table information
                // versus the perceived effort to make reordering work for multiple symbol tables,
                // I am taking the easy way out here.  PS 19 July 2007.
                table.getTableHeader().setReorderingAllowed(false);
                // Detect click on label/address and scroll Text/Data segment display to it.
                table.addMouseListener(new LabelDisplayMouseListener());
                table.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Table.gridColor"), 2));
                JPanel tablePanel = new JPanel(new BorderLayout());
                tablePanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(12, 12, 12, 12),
                    symbolTableDisplay.getSymbolTableName(),
                    TitledBorder.CENTER,
                    TitledBorder.TOP
                ));
                int height = (int) table.getPreferredSize().getHeight() + tablePanel.getInsets().top + tablePanel.getInsets().bottom;
                tablePanel.setMaximumSize(new Dimension((int) tablePanel.getMaximumSize().getWidth(), height));
                tablePanel.add(table, BorderLayout.CENTER);
                allSymbolTables.add(Box.createVerticalStrut(12));
                allSymbolTables.add(tablePanel);
            }
        }
        allSymbolTables.add(Box.createVerticalGlue());

        JPanel sortPanel = new JPanel(new GridLayout(1, COLUMN_NAMES.length));
        for (int column = 0; column < COLUMN_NAMES.length; column++) {
            JButton columnSortButton = new JButton(this.columnSortActions[column]);
            columnSortButton.setText(COLUMN_SORT_BUTTON_LABELS[this.sortState][column]);
            columnSortButton.setToolTipText(COLUMN_TOOL_TIPS[column]);
            columnSortButton.setBackground(UIManager.getColor("TableHeader.background"));
            columnSortButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Table.gridColor"), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
            ));
            sortPanel.add(columnSortButton);
        }

        JScrollPane scrollPane = new JScrollPane(allSymbolTables, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setColumnHeaderView(sortPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    /**
     * Method to update display of label addresses.  Since label information doesn't change,
     * this should only be done when address base is changed.
     * (e.g. between base 16 hex and base 10 dec).
     */
    public void updateLabelAddresses() {
        for (SymbolTableDisplay symbolTableDisplay : this.symbolTableDisplays) {
            symbolTableDisplay.updateLabelAddresses();
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
     * Action assigned to the sort buttons at the top of the window,
     * handling the process of changing sort state when they are clicked.
     */
    private class ColumnSortAction extends AbstractAction {
        private final int column;

        public ColumnSortAction(int column) {
            this.column = column;
        }

        /**
         * Set the sort state according to which button was pressed.
         *
         * @param event The event to be processed.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            SymbolTableWindow window = SymbolTableWindow.this;
            window.sortState = SORT_STATE_TRANSITIONS[window.sortState][this.column];
            window.gui.getSettings().symbolTableSortState.set(window.sortState);
            // Refresh the window
            window.setupTable();
            window.gui.getMainPane().getExecuteTab().setSymbolTableWindowVisible(false);
            window.gui.getMainPane().getExecuteTab().setSymbolTableWindowVisible(true);
        }
    }

    /**
     * Private listener class to sense clicks on a table entry's
     * Label or Address.  This will trigger action by Text or Data
     * segment to scroll to the corresponding label/address.
     * Suggested by Ken Vollmar, implemented by Pete Sanderson July 2007.
     */
    private class LabelDisplayMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            JTable table = (JTable) event.getSource();
            int row = table.rowAtPoint(event.getPoint());
            int column = table.columnAtPoint(event.getPoint());
            Object data = table.getValueAt(row, column);
            if (column == LABEL_COLUMN) {
                // Selected a label name, so get its address.
                data = table.getModel().getValueAt(row, ADDRESS_COLUMN);
            }
            int address = 0;
            try {
                address = Binary.decodeInteger(data.toString());
            }
            catch (NumberFormatException exception) {
                // Cannot happen because address is generated internally
            }
            // Scroll to this address, either in Text Segment display or Data Segment display
            if (Memory.getInstance().isInTextSegment(address) || Memory.getInstance().isInKernelTextSegment(address)) {
                SymbolTableWindow.this.gui.getMainPane().getExecuteTab().getTextSegmentWindow().selectStepAtAddress(address);
                // If self-modifying code is enabled, scroll to the address in the Data Segment display as well
                if (SymbolTableWindow.this.gui.getSettings().selfModifyingCodeEnabled.get()) {
                    SymbolTableWindow.this.gui.getMainPane().getExecuteTab().getDataSegmentWindow().selectCellForAddress(address);
                }
            }
            else {
                SymbolTableWindow.this.gui.getMainPane().getExecuteTab().getDataSegmentWindow().selectCellForAddress(address);
            }
        }
    }

    /**
     * Represents one symbol table for the display.
     */
    private class SymbolTableDisplay {
        private Object[][] labelData;
        private JTable labelTable;
        private Symbol[] symbols;
        private final SymbolTable symbolTable;
        private final String tableName;

        /**
         * Create a new instance.
         *
         * @param filename Associated <code>Program</code> object.  If null, this represents global symbol table.
         */
        public SymbolTableDisplay(String filename) {
            this.symbolTable = Application.assembler.getSymbolTable(filename);
            this.tableName = (filename == null) ? "Global Symbols" : "From \"" + new File(filename).getName() + "\"";
        }

        /**
         * Returns the name of the symbol table this object represents.
         */
        public String getSymbolTableName() {
            return this.tableName;
        }

        public boolean hasSymbols() {
            return this.symbolTable.getSize() != 0;
        }

        /**
         * Builds the table containing labels and addresses for this symbol table.
         */
        private JTable generateLabelTable() {
            int addressBase = SymbolTableWindow.this.gui.getMainPane().getExecuteTab().getAddressDisplayBase();

            if (SymbolTableWindow.this.textLabels.isSelected() && SymbolTableWindow.this.dataLabels.isSelected()) {
                this.symbols = this.symbolTable.getAllSymbols();
            }
            else if (SymbolTableWindow.this.textLabels.isSelected()) {
                this.symbols = this.symbolTable.getTextSymbols();
            }
            else if (SymbolTableWindow.this.dataLabels.isSelected()) {
                this.symbols = this.symbolTable.getDataSymbols();
            }
            else {
                // Eh, don't wanna deal with null checks
                this.symbols = new Symbol[0];
            }
            Arrays.sort(this.symbols, TABLE_SORTING_COMPARATORS.get(SymbolTableWindow.this.sortState)); // DPS 25 Dec 2008

            // Set up the label table
            this.labelData = new Object[this.symbols.length][COLUMN_NAMES.length];
            for (int row = 0; row < this.symbols.length; row++) {
                this.labelData[row][LABEL_COLUMN] = this.symbols[row].getIdentifier();
                this.labelData[row][ADDRESS_COLUMN] = NumberDisplayBaseChooser.formatNumber(this.symbols[row].getAddress(), addressBase);
            }

            LabelTableModel model = new LabelTableModel(this.labelData, COLUMN_NAMES);
            if (this.labelTable == null) {
                this.labelTable = new LabelTable(model);
            }
            else {
                this.labelTable.setModel(model);
            }

            this.labelTable.setFont(SymbolTableWindow.this.gui.getSettings().tableFont.get());
            this.labelTable.getColumnModel().getColumn(ADDRESS_COLUMN).setCellRenderer(new SimpleCellRenderer(SwingConstants.RIGHT));

            return this.labelTable;
        }

        public void updateLabelAddresses() {
            if (this.labelData == null || SymbolTableWindow.this.labelPanel.getComponentCount() == 0) {
                // No content to change, ignore
                return;
            }

            int addressBase = SymbolTableWindow.this.gui.getMainPane().getExecuteTab().getAddressDisplayBase();
            for (int row = 0; row < this.labelData.length; row++) {
                String formattedAddress = NumberDisplayBaseChooser.formatNumber(this.symbols[row].getAddress(), addressBase);
                this.labelTable.getModel().setValueAt(formattedAddress, row, ADDRESS_COLUMN);
            }
        }
    }

    /**
     * Class representing label table data.
     */
    private static class LabelTableModel extends AbstractTableModel {
        private final String[] columnNames;
        private final Object[][] data;

        public LabelTableModel(Object[][] data, String[] columnNames) {
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

        /**
         * JTable uses this method to determine the default renderer/editor for each cell.
         */
        @Override
        public Class<?> getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return this.data[row][column];
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            this.data[row][column] = value;
            this.fireTableCellUpdated(row, column);
        }
    }

    // JTable subclass to provide custom tool tips for each of the
    // label table column headers. From Sun's JTable tutorial.
    // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
    private class LabelTable extends JTable {
        public LabelTable(LabelTableModel model) {
            super(model);
        }

        // Implement table header tool tips.
        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new LabelTableHeader(this.columnModel);
        }

        // Implement cell tool tips.  All of them are the same (although they could be customized).
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component component = super.prepareRenderer(renderer, row, column);
            if (component instanceof JComponent jComponent) {
                jComponent.setToolTipText("Click on a label or address to view the location in memory.");
            }
            return component;
        }

        /**
         * Customized table header that will both display tool tip when
         * mouse hovers over each column, and also sort the table when
         * mouse is clicked on each column.  The tool tip and sort are
         * customized based on the column under the mouse.
         */
        private class LabelTableHeader extends JTableHeader implements MouseListener {
            public LabelTableHeader(TableColumnModel model) {
                super(model);
                this.addMouseListener(this);
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                int columnIndex = this.columnModel.getColumnIndexAtX(event.getPoint().x);
                if (columnIndex < 0) {
                    return null;
                }
                int modelIndex = this.columnModel.getColumn(columnIndex).getModelIndex();
                return COLUMN_TOOL_TIPS[modelIndex];
            }

            /**
             * When user clicks on table column header, system will sort the
             * table based on that column then redraw it.
             */
            @Override
            public void mouseClicked(MouseEvent event) {
                int columnIndex = this.columnModel.getColumnIndexAtX(event.getPoint().x);
                if (columnIndex < 0) {
                    return;
                }
                int modelIndex = this.columnModel.getColumn(columnIndex).getModelIndex();
                SymbolTableWindow window = SymbolTableWindow.this;
                window.sortState = SORT_STATE_TRANSITIONS[window.sortState][modelIndex];
                window.gui.getSettings().symbolTableSortState.set(window.sortState);
                window.setupTable();
                window.gui.getMainPane().getExecuteTab().setSymbolTableWindowVisible(false);
                window.gui.getMainPane().getExecuteTab().setSymbolTableWindowVisible(true);
            }

            @Override
            public void mouseEntered(MouseEvent event) {
            }

            @Override
            public void mouseExited(MouseEvent event) {
            }

            @Override
            public void mousePressed(MouseEvent event) {
            }

            @Override
            public void mouseReleased(MouseEvent event) {
            }
        }
    }

    /**
     * Comparator class used to sort in ascending order a List of symbols alphabetically by name.
     */
    private static class LabelNameAscendingComparator implements Comparator<Symbol> {
        @Override
        public int compare(Symbol a, Symbol b) {
            return a.getIdentifier().compareToIgnoreCase(b.getIdentifier());
        }
    }

    /**
     * Comparator class used to sort in ascending order a List of symbols numerically by address.
     */
    private static class LabelAddressAscendingComparator implements Comparator<Symbol> {
        @Override
        public int compare(Symbol a, Symbol b) {
            return Integer.compareUnsigned(a.getAddress(), b.getAddress());
        }
    }
}
	   	
