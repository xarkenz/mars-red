package mars.venus;

import mars.Application;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Register;
import mars.simulator.SimulatorFinishEvent;
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
public class Coprocessor0Window extends RegistersDisplayTab {
    private static final int NAME_COLUMN = 0;
    private static final int NUMBER_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;

    private static final String[] HEADER_TIPS = {
        "Each register has a tool tip describing its usage convention", // Name
        "Register number.  In your program, precede it with $", // Number
        "Current 32 bit value", // Value
    };
    private static final String[] REGISTER_TIPS = {
        "Memory address at which address exception occurred", // $8 (vaddr)
        "Interrupt mask and enable bits", // $12 (status)
        "Exception type and pending interrupt bits", // $13 (cause)
        "Address of instruction that caused exception", // $14 (epc)
    };

    private final RegistersTable table;
    private int[] rowGivenRegisterNumber; // Translates register number to table row

    /**
     * Constructor which sets up a fresh window with a table that contains the register values.
     */
    public Coprocessor0Window() {
        super();
        table = new RegistersTable(new RegisterTableModel(setupWindow()), HEADER_TIPS, REGISTER_TIPS);
        table.setupColumn(NAME_COLUMN, 50, SwingConstants.LEFT);
        table.setupColumn(NUMBER_COLUMN, 25, SwingConstants.LEFT);
        table.setupColumn(VALUE_COLUMN, 60, SwingConstants.LEFT);
        table.setPreferredScrollableViewportSize(new Dimension(200, 700));
        setLayout(new BorderLayout()); // Table display will occupy entire width if widened
        add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    }

    @Override
    protected RegistersTable getTable() {
        return table;
    }

    /**
     * Sets up the data for the window.
     *
     * @return The array object with the data for the window.
     */
    private Object[][] setupWindow() {
        Register[] registers = Coprocessor0.getRegisters();
        Object[][] tableData = new Object[registers.length][3];
        rowGivenRegisterNumber = new int[32]; // Maximum number of registers
        for (int row = 0; row < registers.length; row++) {
            rowGivenRegisterNumber[registers[row].getNumber()] = row;
            tableData[row][0] = registers[row].getName();
            tableData[row][1] = "$" + registers[row].getNumber();
            tableData[row][2] = NumberDisplayBaseChooser.formatNumber(registers[row].getValue(), NumberDisplayBaseChooser.getBase(Application.getSettings().displayValuesInHex.get()));
        }
        return tableData;
    }

    /**
     * Reset and redisplay registers.
     */
    public void clearWindow() {
        clearHighlighting();
        Coprocessor0.resetRegisters();
        updateRegisters(Application.getGUI().getMainPane().getExecuteTab().getValueDisplayBase());
    }

    /**
     * Update register display using specified number base (10 or 16).
     *
     * @param base Desired number base.
     */
    @Override
    public void updateRegisters(int base) {
        for (Register register : Coprocessor0.getRegisters()) {
            updateRegisterValue(register.getNumber(), register.getValue(), base);
        }
    }

    /**
     * This method handles the updating of the GUI.
     *
     * @param number The number of the register to update.
     * @param value  The new value.
     */
    public void updateRegisterValue(int number, int value, int base) {
        ((RegisterTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(value, base), rowGivenRegisterNumber[number], 2);
    }

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register Register object corresponding to row to be selected.
     */
    @Override
    public void highlightRegister(Register register) {
        table.highlightRow(Coprocessor0.getRegisterPosition(register));
    }

    @Override
    public void startObservingRegisters() {
        Coprocessor0.addRegistersObserver(this);
    }

    @Override
    public void stopObservingRegisters() {
        Coprocessor0.deleteRegistersObserver(this);
    }

    @Override
    public void simulatorFinished(SimulatorFinishEvent event) {
        super.simulatorFinished(event);
        // Bring Coprocessor 0 to the front if terminated due to exception
        if (event.exception() != null) {
            Application.getGUI().getRegistersPane().setSelectedComponent(this);
        }
    }

    private static class RegisterTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Name", "Number", "Value"};

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
         * JTable uses this method to determine the default renderer/editor for each cell.
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
            catch (NumberFormatException exception) {
                data[row][col] = "INVALID";
                fireTableCellUpdated(row, col);
                return;
            }
            // Assures that if changed during MIPS program execution, the update will
            // occur only between MIPS instructions.
            synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                Coprocessor0.updateRegister(Coprocessor0.getRegisters()[row].getNumber(), intValue);
            }
            int valueBase = Application.getGUI().getMainPane().getExecuteTab().getValueDisplayBase();
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
}