package mars.venus;

import mars.Globals;
import mars.Settings;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterAccessNotice;
import mars.mips.hardware.RegisterFile;
import mars.simulator.Simulator;
import mars.simulator.SimulatorNotice;
import mars.util.Binary;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

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
 * Sets up a window to display registers in the UI.
 *
 * @author Sanderson, Bumgarner
 */
public class RegistersWindow extends JPanel implements Observer {
    private static final int NAME_COLUMN = 0;
    private static final int NUMBER_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;

    private final JTable table;
    private boolean highlighting;
    private int highlightRow;

    /**
     * Constructor which sets up a fresh window with a table that contains the register values.
     */
    public RegistersWindow() {
        Simulator.getInstance().addObserver(this);
        this.highlighting = false;
        table = new MyTippedJTable(new RegTableModel(setupWindow()));
        table.getColumnModel().getColumn(NAME_COLUMN).setPreferredWidth(25);
        table.getColumnModel().getColumn(NUMBER_COLUMN).setPreferredWidth(25);
        table.getColumnModel().getColumn(VALUE_COLUMN).setPreferredWidth(60);
        // Display register values (String-ified) right-justified in mono font
        table.getColumnModel().getColumn(NAME_COLUMN).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.LEFT));
        table.getColumnModel().getColumn(NUMBER_COLUMN).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
        table.getColumnModel().getColumn(VALUE_COLUMN).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
        table.setPreferredScrollableViewportSize(new Dimension(200, 700));
        this.setLayout(new BorderLayout()); // table display will occupy entire width if widened
        this.add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
    }

    /**
     * Sets up the data for the window.
     *
     * @return The array object with the data for the window.
     */
    public Object[][] setupWindow() {
        int valueBase = NumberDisplayBaseChooser.getBase(Globals.getSettings().getBoolean(Settings.DISPLAY_VALUES_IN_HEX));
        Object[][] tableData = new Object[35][3];
        for (Register register : RegisterFile.getRegisters()) {
            tableData[register.getNumber()][0] = register.getName();
            tableData[register.getNumber()][1] = register.getNumber();
            tableData[register.getNumber()][2] = NumberDisplayBaseChooser.formatNumber(register.getValue(), valueBase);
        }
        tableData[32][0] = "pc";
        tableData[32][1] = "";
        tableData[32][2] = NumberDisplayBaseChooser.formatUnsignedInteger(RegisterFile.getProgramCounter(), valueBase);

        tableData[33][0] = "hi";
        tableData[33][1] = "";
        tableData[33][2] = NumberDisplayBaseChooser.formatNumber(RegisterFile.getValue(33), valueBase);

        tableData[34][0] = "lo";
        tableData[34][1] = "";
        tableData[34][2] = NumberDisplayBaseChooser.formatNumber(RegisterFile.getValue(34), valueBase);

        return tableData;
    }

    /**
     * Reset and redisplay registers.
     */
    public void clearWindow() {
        this.clearHighlighting();
        RegisterFile.resetRegisters();
        this.updateRegisters(Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase());
    }

    /**
     * Clear highlight background color from any cell currently highlighted.
     */
    public void clearHighlighting() {
        highlighting = false;
        if (table != null) {
            table.tableChanged(new TableModelEvent(table.getModel()));
        }
        highlightRow = -1; // assure highlight will not occur upon re-assemble.
    }

    /**
     * Refresh the table, triggering re-rendering.
     */
    public void refresh() {
        if (table != null) {
            table.tableChanged(new TableModelEvent(table.getModel()));
        }
    }

    /**
     * update register display using current number base (10 or 16)
     */
    public void updateRegisters() {
        updateRegisters(Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase());
    }

    /**
     * update register display using specified number base (10 or 16)
     *
     * @param base desired number base
     */
    public void updateRegisters(int base) {
        for (Register register : RegisterFile.getRegisters()) {
            updateRegisterValue(register.getNumber(), register.getValue(), base);
        }
        updateRegisterUnsignedValue(32, RegisterFile.getProgramCounter(), base);
        updateRegisterValue(33, RegisterFile.getValue(33), base);
        updateRegisterValue(34, RegisterFile.getValue(34), base);
    }

    /**
     * This method handles the updating of the GUI.
     *
     * @param number The number of the register to update.
     * @param val    New value.
     */
    public void updateRegisterValue(int number, int val, int base) {
        ((RegTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(val, base), number, 2);
    }

    private void updateRegisterUnsignedValue(int number, int val, int base) {
        ((RegTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatUnsignedInteger(val, base), number, 2);
    }

    /**
     * Required by Observer interface.  Called when notified by an Observable that we are registered with.
     * Observables include:
     * The Simulator object, which lets us know when it starts and stops running
     * A register object, which lets us know of register operations
     * The Simulator keeps us informed of when simulated MIPS execution is active.
     * This is the only time we care about register operations.
     *
     * @param observable The Observable object who is notifying us
     * @param obj        Auxiliary object with additional information.
     */
    @Override
    public void update(Observable observable, Object obj) {
        if (observable == mars.simulator.Simulator.getInstance()) {
            SimulatorNotice notice = (SimulatorNotice) obj;
            if (notice.action() == SimulatorNotice.SIMULATOR_START) {
                // Simulated MIPS execution starts.  Respond to memory changes if running in timed
                // or stepped mode.
                if (notice.runSpeed() != RunSpeedPanel.UNLIMITED_SPEED || notice.maxSteps() == 1) {
                    RegisterFile.addRegistersObserver(this);
                    this.highlighting = true;
                }
            }
            else {
                // Simulated MIPS execution stops.  Stop responding.
                RegisterFile.deleteRegistersObserver(this);
            }
        }
        else if (obj instanceof RegisterAccessNotice access) {
            // NOTE: each register is a separate Observable
            if (access.getAccessType() == AccessNotice.WRITE) {
                // Uses the same highlighting technique as for Text Segment -- see
                // AddressCellRenderer class in DataSegmentWindow.java.
                this.highlighting = true;
                this.highlightCellForRegister((Register) observable);
                Globals.getGui().getRegistersPane().setSelectedComponent(this);
            }
        }
    }

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register Register object corresponding to row to be selected.
     */
    void highlightCellForRegister(Register register) {
        this.highlightRow = register.getNumber();
        table.tableChanged(new TableModelEvent(table.getModel()));
    }

    /**
     * Cell renderer for displaying register entries.  This does highlighting, so if you
     * don't want highlighting for a given column, don't use this.  Currently we highlight
     * all columns.
     */
    private class RegisterCellRenderer extends DefaultTableCellRenderer {
        private final Font font;
        private final int alignment;

        public RegisterCellRenderer(Font font, int alignment) {
            super();
            this.font = font;
            this.alignment = alignment;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setFont(font);
            cell.setHorizontalAlignment(alignment);
            if (Globals.getSettings().getBoolean(Settings.REGISTERS_HIGHLIGHTING) && highlighting && row == highlightRow) {
                cell.setBackground(Globals.getSettings().getColorSettingByPosition(Settings.REGISTER_HIGHLIGHT_BACKGROUND));
                cell.setForeground(Globals.getSettings().getColorSettingByPosition(Settings.REGISTER_HIGHLIGHT_FOREGROUND));
                cell.setFont(Globals.getSettings().getFontByPosition(Settings.REGISTER_HIGHLIGHT_FONT));
            }
            else if (row % 2 == 0) {
                cell.setBackground(Globals.getSettings().getColorSettingByPosition(Settings.EVEN_ROW_BACKGROUND));
                cell.setForeground(Globals.getSettings().getColorSettingByPosition(Settings.EVEN_ROW_FOREGROUND));
                cell.setFont(Globals.getSettings().getFontByPosition(Settings.EVEN_ROW_FONT));
            }
            else {
                cell.setBackground(Globals.getSettings().getColorSettingByPosition(Settings.ODD_ROW_BACKGROUND));
                cell.setForeground(Globals.getSettings().getColorSettingByPosition(Settings.ODD_ROW_FOREGROUND));
                cell.setFont(Globals.getSettings().getFontByPosition(Settings.ODD_ROW_FONT));
            }
            return cell;
        }
    }

    private static class RegTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Name", "Number", "Value"};

        Object[][] data;

        public RegTableModel(Object[][] data) {
            this.data = data;
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMN_NAMES[col];
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

        /**
         * All register values are editable except $zero (0), $pc (32), $ra (31).
         */
        @Override
        public boolean isCellEditable(int row, int col) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            // these registers are not editable: $zero (0), $pc (32), $ra (31)
            return col == VALUE_COLUMN && row != 0 && row != 32 && row != 31;
        }

        /**
         * Update cell contents in table model.  This method should be called
         * only when user edits cell, so input validation has to be done.  If
         * value is valid, MIPS register is updated.
         */
        @Override
        public void setValueAt(Object value, int row, int col) {
            int intValue;
            try {
                intValue = Binary.stringToInt(value.toString());
            }
            catch (NumberFormatException nfe) {
                data[row][col] = "INVALID";
                fireTableCellUpdated(row, col);
                return;
            }
            //  Assures that if changed during MIPS program execution, the update will
            //  occur only between MIPS instructions.
            synchronized (Globals.MEMORY_AND_REGISTERS_LOCK) {
                RegisterFile.updateRegister(row, intValue);
            }
            int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
            data[row][col] = NumberDisplayBaseChooser.formatNumber(intValue, valueBase);
            fireTableCellUpdated(row, col);
        }

        /**
         * Update cell contents in table model.  Does not affect MIPS register.
         */
        private void setDisplayAndModelValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }

    ///////////////////////////////////////////////////////////////////
    //
    // JTable subclass to provide custom tool tips for each of the
    // register table column headers and for each register name in
    // the first column. From Sun's JTable tutorial.
    // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
    //
    private static class MyTippedJTable extends JTable {
        MyTippedJTable(RegTableModel model) {
            super(model);
            this.setRowSelectionAllowed(true); // highlights background color of entire row
            this.setSelectionBackground(Color.GREEN);
        }

        private static final String[] REGISTER_TOOL_TIPS = {
            "Constant value 0 (cannot be modified)", // $zero
            "Reserved for assembler use", // $at
            "Expression evaluation and results of a function", // $v0
            "Expression evaluation and results of a function", // $v1
            "Argument 1", // $a0
            "Argument 2", // $a1
            "Argument 3", // $a2
            "Argument 4", // $a3
            "Temporary (not preserved across call)", // $t0
            "Temporary (not preserved across call)", // $t1
            "Temporary (not preserved across call)", // $t2
            "Temporary (not preserved across call)", // $t3
            "Temporary (not preserved across call)", // $t4
            "Temporary (not preserved across call)", // $t5
            "Temporary (not preserved across call)", // $t6
            "Temporary (not preserved across call)", // $t7
            "Saved temporary (preserved across call)", // $s0
            "Saved temporary (preserved across call)", // $s1
            "Saved temporary (preserved across call)", // $s2
            "Saved temporary (preserved across call)", // $s3
            "Saved temporary (preserved across call)", // $s4
            "Saved temporary (preserved across call)", // $s5
            "Saved temporary (preserved across call)", // $s6
            "Saved temporary (preserved across call)", // $s7
            "Temporary (not preserved across call)", // $t8
            "Temporary (not preserved across call)", // $t9
            "Reserved for OS kernel", // $k0
            "Reserved for OS kernel", // $k1
            "Pointer to global area", // $gp
            "Stack pointer", // $sp
            "Frame pointer", // $fp
            "Return address (used by function call)", // $ra
            "Program counter", // pc
            "High-order word of multiply product or divide remainder", // hi
            "Low-order word of multiply product or divide quotient", // lo
        };

        // Implement table cell tool tips.
        @Override
        public String getToolTipText(MouseEvent e) {
            java.awt.Point p = e.getPoint();
            int rowIndex = rowAtPoint(p);
            int colIndex = columnAtPoint(p);
            int realColumnIndex = convertColumnIndexToModel(colIndex);
            if (realColumnIndex == NAME_COLUMN) {
                return REGISTER_TOOL_TIPS[rowIndex];
            }
            else {
                return super.getToolTipText(e);
            }
        }

        private static final String[] COLUMN_TOOL_TIPS = {
            "Each register has a tool tip describing its usage convention", // Name
            "Corresponding register number", // Number
            "Current 32 bit value", // Value
        };

        // Implement table header tool tips.
        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(columnModel) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    java.awt.Point p = e.getPoint();
                    int index = columnModel.getColumnIndexAtX(p.x);
                    int realIndex = columnModel.getColumn(index).getModelIndex();
                    return COLUMN_TOOL_TIPS[realIndex];
                }
            };
        }
    }
}