package mars.venus;

import mars.Application;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Register;
import mars.simulator.Simulator;
import mars.util.Binary;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

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
public class Coprocessor0Tab extends RegistersDisplayTab {
    private static final int NUMBER_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;

    private static final String[] HEADER_TIPS = {
        "5-bit register address", // Address
        "Register name corresponding to role", // Name
        "32-bit register value", // Value
    };
    private static final String[] REGISTER_TIPS = {
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "Bad Virtual Address (memory address at which address exception occurred)", // $8 (BadVAddr)
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "Status (interrupt mask and enable bits)", // $12 (Status)
        "Cause (exception type and pending interrupt bits)", // $13 (Cause)
        "Exception Program Counter (address of instruction that caused exception)", // $14 (EPC)
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
        "(not implemented)",
    };

    private final RegistersTable table;

    /**
     * Constructor which sets up a fresh window with a table that contains the register values.
     */
    public Coprocessor0Tab(VenusUI gui) {
        super(gui);
        this.table = new RegistersTable(new RegisterTableModel(this.setupWindow()), HEADER_TIPS, REGISTER_TIPS);
        this.table.setupColumn(NUMBER_COLUMN, 25, SwingConstants.LEFT);
        this.table.setupColumn(NAME_COLUMN, 50, SwingConstants.LEFT);
        this.table.setupColumn(VALUE_COLUMN, 60, SwingConstants.LEFT);
        this.table.setPreferredScrollableViewportSize(new Dimension(200, 700));
        this.setLayout(new BorderLayout()); // Table display will occupy entire width if widened
        this.add(new JScrollPane(this.table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    }

    @Override
    protected RegistersTable getTable() {
        return this.table;
    }

    /**
     * Sets up the data for the window.
     *
     * @return The array object with the data for the window.
     */
    private Object[][] setupWindow() {
        int valueBase = NumberDisplayBaseChooser.getBase(Application.getSettings().displayValuesInHex.get());
        Object[][] tableData = new Object[32][3];

        int number = 0;
        for (Register register : Coprocessor0.getRegisters()) {
            if (register != null) {
                tableData[register.getNumber()][NUMBER_COLUMN] = "$" + register.getNumber();
                tableData[register.getNumber()][NAME_COLUMN] = register.getName();
                tableData[register.getNumber()][VALUE_COLUMN] = NumberDisplayBaseChooser.formatNumber(register.getValue(), valueBase);
            }
            else {
                tableData[number][NUMBER_COLUMN] = "$" + number;
                tableData[number][NAME_COLUMN] = "-";
                tableData[number][VALUE_COLUMN] = "";
            }
            number++;
        }

        return tableData;
    }

    /**
     * Reset and redisplay registers.
     */
    public void resetDisplay() {
        this.clearHighlighting();
        this.updateRegisters(this.gui.getMainPane().getExecuteTab().getValueDisplayBase());
    }

    /**
     * Update register display using specified number base (10 or 16).
     *
     * @param base Desired number base.
     */
    @Override
    public void updateRegisters(int base) {
        for (Register register : Coprocessor0.getRegisters()) {
            if (register != null) {
                this.updateRegisterValue(register.getNumber(), register.getValue(), base);
            }
        }
    }

    /**
     * This method handles the updating of the GUI.
     *
     * @param number The number of the register to update.
     * @param value  The new value.
     */
    public void updateRegisterValue(int number, int value, int base) {
        ((RegisterTableModel) this.table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(value, base), number, VALUE_COLUMN);
    }

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register Register object corresponding to row to be selected.
     */
    @Override
    public void highlightRegister(Register register) {
        if (register != null) {
            this.table.highlightRow(register.getNumber());
        }
    }

    @Override
    public void startObservingRegisters() {
        for (Register register : Coprocessor0.getRegisters()) {
            if (register != null) {
                register.addListener(this);
            }
        }
    }

    @Override
    public void stopObservingRegisters() {
        for (Register register : Coprocessor0.getRegisters()) {
            if (register != null) {
                register.removeListener(this);
            }
        }
    }

    private class RegisterTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Number", "Name", "Value"};

        private final Object[][] data;

        public RegisterTableModel(Object[][] data) {
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
         * JTable uses this method to determine the default renderer/editor for each cell.
         */
        @Override
        public Class<?> getColumnClass(int column) {
            return this.getValueAt(0, column).getClass();
        }

        /**
         * Values of implemented registers are editable.
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            // Note that the data/cell address is constant, no matter where the cell appears onscreen.
            return column == VALUE_COLUMN && Coprocessor0.getRegisters()[row] != null;
        }

        /**
         * Update cell contents in table model.  This method should be called
         * only when user edits cell, so input validation has to be done.  If
         * value is valid, MIPS register is updated.
         */
        @Override
        public void setValueAt(Object value, int row, int column) {
            int intValue;
            try {
                intValue = Binary.decodeInteger(value.toString());
            }
            catch (NumberFormatException exception) {
                this.fireTableCellUpdated(row, column);
                return;
            }

            // Update the cell to the proper number format
            int valueBase = Coprocessor0Tab.this.gui.getMainPane().getExecuteTab().getValueDisplayBase();
            this.setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(intValue, valueBase), row, column);

            // Assures that if changed during MIPS program execution, the update will
            // occur only between MIPS instructions.
            Simulator.getInstance().changeState(() -> Coprocessor0.updateRegister(Coprocessor0.getRegisters()[row].getNumber(), intValue));
        }

        /**
         * Update cell contents in table model.  Does not affect MIPS register.
         */
        public void setDisplayAndModelValueAt(Object value, int row, int column) {
            this.data[row][column] = value;
            this.fireTableCellUpdated(row, column);
        }
    }
}