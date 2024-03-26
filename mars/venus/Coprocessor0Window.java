package mars.venus;

import mars.Globals;
import mars.settings.Settings;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterAccessNotice;
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
public class Coprocessor0Window extends JPanel implements Observer {
    private static final int NAME_COLUMN = 0;
    private static final int NUMBER_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;

    private final JTable table;
    private int[] rowGivenRegNumber; // translate register number to table row.
    private boolean highlighting;
    private int highlightRow;

    /**
     * Constructor which sets up a fresh window with a table that contains the register values.
     */
    public Coprocessor0Window() {
        Simulator.getInstance().addObserver(this);
        this.highlighting = false;
        table = new MyTippedJTable(new RegTableModel(setupWindow()));
        table.getColumnModel().getColumn(NAME_COLUMN).setPreferredWidth(50);
        table.getColumnModel().getColumn(NUMBER_COLUMN).setPreferredWidth(25);
        table.getColumnModel().getColumn(VALUE_COLUMN).setPreferredWidth(60);
        // Display register values (String-ified) right-justified in mono font
        table.getColumnModel().getColumn(NAME_COLUMN).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.LEFT));
        table.getColumnModel().getColumn(NUMBER_COLUMN).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
        table.getColumnModel().getColumn(VALUE_COLUMN).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
        table.setPreferredScrollableViewportSize(new Dimension(200, 700));
        this.setLayout(new BorderLayout());  // table display will occupy entire width if widened
        this.add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    }

    /**
     * Sets up the data for the window.
     *
     * @return The array object with the data for the window.
     */
    private Object[][] setupWindow() {
        Register[] registers = Coprocessor0.getRegisters();
        Object[][] tableData = new Object[registers.length][3];
        rowGivenRegNumber = new int[32]; // maximum number of registers
        for (int i = 0; i < registers.length; i++) {
            rowGivenRegNumber[registers[i].getNumber()] = i;
            tableData[i][0] = registers[i].getName();
            tableData[i][1] = registers[i].getNumber();
            tableData[i][2] = NumberDisplayBaseChooser.formatNumber(registers[i].getValue(), NumberDisplayBaseChooser.getBase(Globals.getSettings().displayValuesInHex.get()));
        }
        return tableData;
    }

    /**
     * Reset and redisplay registers.
     */
    public void clearWindow() {
        this.clearHighlighting();
        Coprocessor0.resetRegisters();
        this.updateRegisters(Globals.getGUI().getMainPane().getExecutePane().getValueDisplayBase());
    }

    /**
     * Clear highlight background color from any row currently highlighted.
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
     * Update register display using current display base (10 or 16)
     */
    public void updateRegisters() {
        this.updateRegisters(Globals.getGUI().getMainPane().getExecutePane().getValueDisplayBase());
    }

    /**
     * Update register display using specified display base
     *
     * @param base number base for display (10 or 16)
     */
    public void updateRegisters(int base) {
        for (Register register : Coprocessor0.getRegisters()) {
            this.updateRegisterValue(register.getNumber(), register.getValue(), base);
        }
    }

    /**
     * This method handles the updating of the GUI.
     *
     * @param number The number of the register to update.
     * @param value  New value.
     */
    public void updateRegisterValue(int number, int value, int base) {
        ((RegTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(value, base), rowGivenRegNumber[number], 2);
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
                    Coprocessor0.addRegistersObserver(this);
                    this.highlighting = true;
                }
            }
            else {
                // Simulated MIPS execution stops.  Stop responding.
                Coprocessor0.deleteRegistersObserver(this);
            }
        }
        else if (obj instanceof RegisterAccessNotice access) {
            // NOTE: each register is a separate Observable
            if (access.getAccessType() == AccessNotice.WRITE) {
                // For now, use highlighting technique used by Label Window feature to highlight
                // memory cell corresponding to a selected label.  The highlighting is not
                // as visually distinct as changing the background color, but will do for now.
                // Ideally, use the same highlighting technique as for Text Segment -- see
                // AddressCellRenderer class in DataSegmentWindow.java.
                this.highlighting = true;
                this.highlightCellForRegister((Register) observable);
                Globals.getGUI().getRegistersPane().setSelectedComponent(this);
            }
        }
    }

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register Register object corresponding to row to be selected.
     */
    void highlightCellForRegister(Register register) {
        int registerRow = Coprocessor0.getRegisterPosition(register);
        if (registerRow < 0) {
            return; // Not a valid Coprocessor0 register
        }
        this.highlightRow = registerRow;
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
            if (Globals.getSettings().highlightRegisters.get() && highlighting && row == highlightRow) {
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

        private final Object[][] data;

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
         * All register values are editable.
         */
        @Override
        public boolean isCellEditable(int row, int col) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            return col == VALUE_COLUMN;
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
            catch (NumberFormatException e) {
                data[row][col] = "INVALID";
                fireTableCellUpdated(row, col);
                return;
            }
            //  Assures that if changed during MIPS program execution, the update will
            //  occur only between MIPS instructions.
            synchronized (Globals.MEMORY_AND_REGISTERS_LOCK) {
                Coprocessor0.updateRegister(Coprocessor0.getRegisters()[row].getNumber(), intValue);
            }
            int valueBase = Globals.getGUI().getMainPane().getExecutePane().getValueDisplayBase();
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
            this.setRowSelectionAllowed(true); // Highlights background color of entire row
            this.setSelectionBackground(Color.GREEN);
        }

        private static final String[] REGISTER_TOOL_TIPS = {
            "Memory address at which address exception occurred", // $8 (vaddr)
            "Interrupt mask and enable bits", // $12 (status)
            "Exception type and pending interrupt bits", // $13 (cause)
            "Address of instruction that caused exception", // $14 (epc)
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
            "Register number.  In your program, precede it with $", // Number
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