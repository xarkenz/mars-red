package mars.venus;

import mars.Application;
import mars.mips.hardware.Register;
import mars.mips.hardware.Processor;
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
public class ProcessorTab extends RegistersDisplayTab {
    private static final int NUMBER_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;

    private static final String[] HEADER_TIPS = {
        "5-bit register address", // Number
        "Register name corresponding to usage conventions", // Name
        "32-bit register value", // Value
    };
    private static final String[] REGISTER_TIPS = {
        "Zero constant value (cannot be modified)", // $zero
        "Assembler Temporary (reserved for pseudo-instruction expansions)", // $at
        "Value 0 (return value or syscall service number)", // $v0
        "Value 1 (return value)", // $v1
        "Argument 0 (for calls and syscalls)", // $a0
        "Argument 1 (for calls and syscalls)", // $a1
        "Argument 2 (for calls and syscalls)", // $a2
        "Argument 3 (for calls and syscalls)", // $a3
        "Temporary 0 (value not preserved by callee)", // $t0
        "Temporary 1 (value not preserved by callee)", // $t1
        "Temporary 2 (value not preserved by callee)", // $t2
        "Temporary 3 (value not preserved by callee)", // $t3
        "Temporary 4 (value not preserved by callee)", // $t4
        "Temporary 5 (value not preserved by callee)", // $t5
        "Temporary 6 (value not preserved by callee)", // $t6
        "Temporary 7 (value not preserved by callee)", // $t7
        "Saved 0 (value preserved by callee)", // $s0
        "Saved 1 (value preserved by callee)", // $s1
        "Saved 2 (value preserved by callee)", // $s2
        "Saved 3 (value preserved by callee)", // $s3
        "Saved 4 (value preserved by callee)", // $s4
        "Saved 5 (value preserved by callee)", // $s5
        "Saved 6 (value preserved by callee)", // $s6
        "Saved 7 (value preserved by callee)", // $s7
        "Temporary 8 (value not preserved by callee)", // $t8
        "Temporary 9 (value not preserved by callee)", // $t9
        "Kernel 0 (reserved for kernel use)", // $k0
        "Kernel 1 (reserved for kernel use)", // $k1
        "Global Pointer (pointer to global area)", // $gp
        "Stack Pointer (pointer to top of stack)", // $sp
        "Frame Pointer (pointer to bottom of current stack frame)", // $fp
        "Return Address (jumped to when returning from function)", // $ra
        "Low-order word of multiplication product or division quotient, access via \"mflo\"", // lo
        "High-order word of multiplication product or division remainder, access via \"mfhi\"", // hi
        "Program Counter (address of instruction being fetched), cannot access directly", // pc
    };

    private final RegistersTable table;

    /**
     * Constructor which sets up a fresh window with a table that contains the register values.
     */
    public ProcessorTab(VenusUI gui) {
        super(gui);
        this.table = new RegistersTable(new RegisterTableModel(this.setupWindow()), HEADER_TIPS, REGISTER_TIPS);
        this.table.setupColumn(NUMBER_COLUMN, 25, SwingConstants.LEFT);
        this.table.setupColumn(NAME_COLUMN, 25, SwingConstants.LEFT);
        this.table.setupColumn(VALUE_COLUMN, 60, SwingConstants.LEFT);
        this.table.setPreferredScrollableViewportSize(new Dimension(200, 700));
        this.setLayout(new BorderLayout()); // Table display will occupy entire width if widened
        this.add(new JScrollPane(this.table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
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
    public Object[][] setupWindow() {
        int valueBase = NumberDisplayBaseChooser.getBase(Application.getSettings().displayValuesInHex.get());
        Object[][] tableData = new Object[35][3];

        for (Register register : Processor.getRegisters()) {
            tableData[register.getNumber()][NUMBER_COLUMN] = "$" + register.getNumber();
            tableData[register.getNumber()][NAME_COLUMN] = register.getName();
            tableData[register.getNumber()][VALUE_COLUMN] = NumberDisplayBaseChooser.formatNumber(register.getValue(), valueBase);
        }

        tableData[Processor.HIGH_ORDER][NUMBER_COLUMN] = "";
        tableData[Processor.HIGH_ORDER][NAME_COLUMN] = Processor.getHighOrderRegister().getName();
        tableData[Processor.HIGH_ORDER][VALUE_COLUMN] = NumberDisplayBaseChooser.formatNumber(Processor.getHighOrder(), valueBase);

        tableData[Processor.LOW_ORDER][NUMBER_COLUMN] = "";
        tableData[Processor.LOW_ORDER][NAME_COLUMN] = Processor.getLowOrderRegister().getName();
        tableData[Processor.LOW_ORDER][VALUE_COLUMN] = NumberDisplayBaseChooser.formatNumber(Processor.getLowOrder(), valueBase);

        tableData[Processor.PROGRAM_COUNTER][NUMBER_COLUMN] = "";
        tableData[Processor.PROGRAM_COUNTER][NAME_COLUMN] = Processor.getFetchPCRegister().getName();
        tableData[Processor.PROGRAM_COUNTER][VALUE_COLUMN] = NumberDisplayBaseChooser.formatUnsignedInteger(Processor.getProgramCounter(), valueBase);

        return tableData;
    }

    /**
     * Redisplay registers.
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
        for (Register register : Processor.getRegisters()) {
            this.updateRegisterValue(register.getNumber(), register.getValue(), base);
        }
        this.updateRegisterValue(Processor.HIGH_ORDER, Processor.getHighOrder(), base);
        this.updateRegisterValue(Processor.LOW_ORDER, Processor.getLowOrder(), base);
        this.updateRegisterUnsignedValue(Processor.PROGRAM_COUNTER, Processor.getProgramCounter(), base);
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
     * This method handles the updating of the GUI.
     *
     * @param number The number of the register to update.
     * @param value  The new value.
     */
    public void updateRegisterUnsignedValue(int number, int value, int base) {
        ((RegisterTableModel) this.table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatUnsignedInteger(value, base), number, VALUE_COLUMN);
    }

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register Register object corresponding to row to be selected.
     */
    @Override
    public void highlightRegister(Register register) {
        this.table.highlightRow(register.getNumber());
    }

    @Override
    public void startObservingRegisters() {
        for (Register register : Processor.getRegisters()) {
            register.addListener(this);
        }
    }

    @Override
    public void stopObservingRegisters() {
        for (Register register : Processor.getRegisters()) {
            register.removeListener(this);
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
         * All register values are editable except $zero and Program Counter.
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            // Note that the data/cell address is constant, no matter where the cell appears onscreen.
            // These registers are not editable: $zero, Program Counter
            return column == VALUE_COLUMN && row != Processor.ZERO_CONSTANT && row != Processor.PROGRAM_COUNTER;
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
            int valueBase = ProcessorTab.this.gui.getMainPane().getExecuteTab().getValueDisplayBase();
            this.setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(intValue, valueBase), row, column);

            // Assures that if changed during MIPS program execution, the update will
            // occur only between MIPS instructions.
            Simulator.getInstance().changeState(() -> Processor.setValue(row, intValue));
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