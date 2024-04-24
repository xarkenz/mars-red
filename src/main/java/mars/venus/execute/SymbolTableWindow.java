package mars.venus.execute;

import mars.Application;
import mars.Program;
import mars.assembler.Symbol;
import mars.assembler.SymbolTable;
import mars.mips.hardware.Memory;
import mars.util.Binary;
import mars.venus.MonoRightCellRenderer;
import mars.venus.NumberDisplayBaseChooser;
import mars.venus.actions.run.RunAssembleAction;

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
    private final JPanel labelPanel;
    private final JCheckBox dataLabels;
    private final JCheckBox textLabels;
    private final ArrayList<SymbolTableDisplay> symbolTableDisplays;

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
    public SymbolTableWindow() {
        super("Symbol Table", true, false, true, false);
        this.setFrameIcon(null);

        sortState = Application.getSettings().symbolTableSortState.get();
        if (sortState < 0 || SORT_STATE_TRANSITIONS.length <= sortState) {
            sortState = 0;
        }
        symbolTableDisplays = new ArrayList<>();

        labelPanel = new JPanel(new BorderLayout());
        this.getContentPane().add(labelPanel, BorderLayout.CENTER);
        dataLabels = new JCheckBox("Data Segment", true);
        textLabels = new JCheckBox("Text Segment", true);
        ItemListener labelListener = event -> {
            for (SymbolTableDisplay labelsForSymbolTable : symbolTableDisplays) {
                labelsForSymbolTable.generateLabelTable();
            }
        };
        dataLabels.addItemListener(labelListener);
        textLabels.addItemListener(labelListener);
        dataLabels.setToolTipText("If checked, will display labels defined in the data segment.");
        textLabels.setToolTipText("If checked, will display labels defined in the text segment.");

        JPanel features = new JPanel();
        features.add(dataLabels);
        features.add(textLabels);
        features.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Table.gridColor"), 1));
        this.getContentPane().add(features, BorderLayout.SOUTH);
    }

    /**
     * Initialize table of labels (symbol table).
     */
    public void setupTable() {
        labelPanel.removeAll();
        labelPanel.add(generateLabelScrollPane(), BorderLayout.CENTER);
    }

    /**
     * Clear the window.
     */
    public void clearWindow() {
        labelPanel.removeAll();
    }

    private JScrollPane generateLabelScrollPane() {
        symbolTableDisplays.clear();
        SymbolTableDisplay globalSymbolTableDisplay = new SymbolTableDisplay(null);
        symbolTableDisplays.add(globalSymbolTableDisplay);
        for (Program program : RunAssembleAction.getProgramsToAssemble()) {
            symbolTableDisplays.add(new SymbolTableDisplay(program));
        }

        Box allSymbolTables = Box.createVerticalBox();
        for (SymbolTableDisplay symbolTableDisplay : symbolTableDisplays) {
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
            JButton columnSortButton = new JButton(columnSortActions[column]);
            columnSortButton.setText(COLUMN_SORT_BUTTON_LABELS[sortState][column]);
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
        for (SymbolTableDisplay symbolTableDisplay : symbolTableDisplays) {
            symbolTableDisplay.updateLabelAddresses();
        }
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
         * @param event the event to be processed
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            sortState = SORT_STATE_TRANSITIONS[sortState][this.column];
            Application.getSettings().symbolTableSortState.set(sortState);
            // Refresh the window
            setupTable();
            Application.getGUI().getMainPane().getExecuteTab().setSymbolTableWindowVisibility(false);
            Application.getGUI().getMainPane().getExecuteTab().setSymbolTableWindowVisibility(true);
        }
    }

    /**
     * Private listener class to sense clicks on a table entry's
     * Label or Address.  This will trigger action by Text or Data
     * segment to scroll to the corresponding label/address.
     * Suggested by Ken Vollmar, implemented by Pete Sanderson
     * July 2007.
     */
    private static class LabelDisplayMouseListener extends MouseAdapter {
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
                address = Binary.stringToInt(data.toString());
            }
            catch (NumberFormatException exception) {
                // Cannot happen because address is generated internally
            }
            // Scroll to this address, either in Text Segment display or Data Segment display
            if (Memory.inTextSegment(address) || Memory.inKernelTextSegment(address)) {
                Application.getGUI().getMainPane().getExecuteTab().getTextSegmentWindow().selectStepAtAddress(address);
            }
            else {
                Application.getGUI().getMainPane().getExecuteTab().getDataSegmentWindow().selectCellForAddress(address);
            }
        }
    }

    /**
     * Represents one symbol table for the display.
     */
    private class SymbolTableDisplay {
        private final Program program;
        private Object[][] labelData;
        private JTable labelTable;
        private Symbol[] symbols;
        private final SymbolTable symbolTable;
        private final String tableName;

        /**
         * Create a new instance.
         *
         * @param program Associated <code>Program</code> object.  If null, this represents global symbol table.
         */
        public SymbolTableDisplay(Program program) {
            this.program = program;
            this.symbolTable = (program == null) ? Application.globalSymbolTable : program.getLocalSymbolTable();
            this.tableName = (program == null) ? "Global Symbols" : "From \"" + new File(program.getFilename()).getName() + "\"";
        }

        /**
         * Returns the name of the symbol table this object represents.
         */
        public String getSymbolTableName() {
            return tableName;
        }

        public boolean hasSymbols() {
            return symbolTable.getSize() != 0;
        }

        /**
         * Builds the table containing labels and addresses for this symbol table.
         */
        private JTable generateLabelTable() {
            SymbolTable symbolTable = (program == null) ? Application.globalSymbolTable : program.getLocalSymbolTable();
            int addressBase = Application.getGUI().getMainPane().getExecuteTab().getAddressDisplayBase();
            if (textLabels.isSelected() && dataLabels.isSelected()) {
                symbols = symbolTable.getAllSymbols();
            }
            else if (textLabels.isSelected() && !dataLabels.isSelected()) {
                symbols = symbolTable.getTextSymbols();
            }
            else if (!textLabels.isSelected() && dataLabels.isSelected()) {
                symbols = symbolTable.getDataSymbols();
            }
            else {
                // eh, don't wanna deal with null checks
                symbols = new Symbol[0];
            }
            Arrays.sort(symbols, TABLE_SORTING_COMPARATORS.get(sortState)); // DPS 25 Dec 2008
            labelData = new Object[symbols.length][2];

            // Set up the label table
            for (int row = 0; row < symbols.length; row++) {
                labelData[row][LABEL_COLUMN] = symbols[row].getName();
                labelData[row][ADDRESS_COLUMN] = NumberDisplayBaseChooser.formatNumber(symbols[row].getAddress(), addressBase);
            }
            LabelTableModel model = new LabelTableModel(labelData, COLUMN_NAMES);
            if (labelTable == null) {
                labelTable = new MyTippedJTable(model);
            }
            else {
                labelTable.setModel(model);
            }
            labelTable.setFont(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT);
            labelTable.getColumnModel().getColumn(ADDRESS_COLUMN).setCellRenderer(new MonoRightCellRenderer());
            return labelTable;
        }

        public void updateLabelAddresses() {
            if (labelPanel.getComponentCount() == 0) {
                return; // ignore if no content to change
            }
            int addressBase = Application.getGUI().getMainPane().getExecuteTab().getAddressDisplayBase();
            int address;
            String formattedAddress;
            int numSymbols = (labelData == null) ? 0 : labelData.length;
            for (int i = 0; i < numSymbols; i++) {
                address = symbols[i].getAddress();
                formattedAddress = NumberDisplayBaseChooser.formatNumber(address, addressBase);
                labelTable.getModel().setValueAt(formattedAddress, i, ADDRESS_COLUMN);
            }
        }
    }

    /**
     * Class representing label table data.
     */
    private static class LabelTableModel extends AbstractTableModel {
        private final String[] columns;
        private final Object[][] data;

        public LabelTableModel(Object[][] data, String[] columns) {
            this.data = data;
            this.columns = columns;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /**
         * JTable uses this method to determine the default renderer/
         * editor for each cell.
         */
        @Override
        public Class<?> getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }

    // JTable subclass to provide custom tool tips for each of the
    // label table column headers. From Sun's JTable tutorial.
    // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
    private class MyTippedJTable extends JTable {
        public MyTippedJTable(LabelTableModel model) {
            super(model);
        }

        // Implement table header tool tips.
        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new SymbolTableHeader(columnModel);
        }

        // Implement cell tool tips.  All of them are the same (although they could be customized).
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
            Component component = super.prepareRenderer(renderer, rowIndex, vColIndex);
            if (component instanceof JComponent jComponent) {
                jComponent.setToolTipText("Click on label or address to view it in Text/Data Segment");
            }
            return component;
        }

        /**
         * Customized table header that will both display tool tip when
         * mouse hovers over each column, and also sort the table when
         * mouse is clicked on each column.  The tool tip and sort are
         * customized based on the column under the mouse.
         */
        private class SymbolTableHeader extends JTableHeader {
            public SymbolTableHeader(TableColumnModel model) {
                super(model);
                this.addMouseListener(new SymbolTableHeaderMouseListener());
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                int index = columnModel.getColumnIndexAtX(event.getPoint().x);
                if (index < 0) {
                    return null;
                }
                int realIndex = columnModel.getColumn(index).getModelIndex();
                return COLUMN_TOOL_TIPS[realIndex];
            }

            /**
             * When user clicks on table column header, system will sort the
             * table based on that column then redraw it.
             */
            private class SymbolTableHeaderMouseListener implements MouseListener {
                @Override
                public void mouseClicked(MouseEvent event) {
                    int index = columnModel.getColumnIndexAtX(event.getPoint().x);
                    if (index < 0) {
                        return;
                    }
                    int realIndex = columnModel.getColumn(index).getModelIndex();
                    sortState = SORT_STATE_TRANSITIONS[sortState][realIndex];
                    Application.getSettings().symbolTableSortState.set(sortState);
                    setupTable();
                    Application.getGUI().getMainPane().getExecuteTab().setSymbolTableWindowVisibility(false);
                    Application.getGUI().getMainPane().getExecuteTab().setSymbolTableWindowVisibility(true);
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
    }

    /**
     * Comparator class used to sort in ascending order a List of symbols alphabetically by name.
     */
    private static class LabelNameAscendingComparator implements Comparator<Symbol> {
        @Override
        public int compare(Symbol a, Symbol b) {
            return a.getName().compareToIgnoreCase(b.getName());
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
	   	
