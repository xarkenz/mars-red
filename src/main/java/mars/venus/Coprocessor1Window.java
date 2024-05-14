package mars.venus;

import mars.Application;
import mars.mips.hardware.*;
import mars.util.Binary;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
 * Sets up a window to display Coprocessor 1 registers in the Registers pane of the UI.
 *
 * @author Pete Sanderson 2005
 */
public class Coprocessor1Window extends RegistersDisplayTab {
    private static final int NAME_COLUMN = 0;
    private static final int FLOAT_COLUMN = 1;
    private static final int DOUBLE_COLUMN = 2;

    private static final String[] HEADER_TIPS = {
        "Each register has a tool tip describing its usage convention", // Name
        "32-bit single precision IEEE 754 floating point value", // Single
        "64-bit double precision IEEE 754 floating point value (uses a pair of 32-bit registers)", // Double
    };
    private static final String[] REGISTER_TIPS = {
        "Floating point subprogram return value", // $f0
        "Should not be referenced explicitly in your program", // $f1
        "Floating point subprogram return value", // $f2
        "Should not be referenced explicitly in your program", // $f3
        "Temporary (not preserved across call)", // $f4
        "Should not be referenced explicitly in your program", // $f5
        "Temporary (not preserved across call)", // $f6
        "Should not be referenced explicitly in your program", // $f7
        "Temporary (not preserved across call)", // $f8
        "Should not be referenced explicitly in your program", // $f9
        "Temporary (not preserved across call)", // $f10
        "Should not be referenced explicitly in your program", // $f11
        "Floating point subprogram argument 1", // $f12
        "Should not be referenced explicitly in your program", // $f13
        "Floating point subprogram argument 2", // $f14
        "Should not be referenced explicitly in your program", // $f15
        "Temporary (not preserved across call)", // $f16
        "Should not be referenced explicitly in your program", // $f17
        "Temporary (not preserved across call)", // $f18
        "Should not be referenced explicitly in your program", // $f19
        "Saved temporary (preserved across call)", // $f20
        "Should not be referenced explicitly in your program", // $f21
        "Saved temporary (preserved across call)", // $f22
        "Should not be referenced explicitly in your program", // $f23
        "Saved temporary (preserved across call)", // $f24
        "Should not be referenced explicitly in your program", // $f25
        "Saved temporary (preserved across call)", // $f26
        "Should not be referenced explicitly in your program", // $f27
        "Saved temporary (preserved across call)", // $f28
        "Should not be referenced explicitly in your program", // $f29
        "Saved temporary (preserved across call)", // $f30
        "Should not be referenced explicitly in your program", // $f31
    };

    private final RegistersTable table;
    private final JCheckBox[] conditionFlagCheckBoxes;

    /**
     * Constructor which sets up a fresh window with a table that contains the register values.
     */
    public Coprocessor1Window(VenusUI gui) {
        super(gui);
        // Display registers in table contained in scroll pane.
        this.setLayout(new BorderLayout()); // table display will occupy entire width if widened
        table = new RegistersTable(new RegisterTableModel(setupWindow()), HEADER_TIPS, REGISTER_TIPS);
        table.setupColumn(NAME_COLUMN, 20, SwingConstants.LEFT);
        table.setupColumn(FLOAT_COLUMN, 70, SwingConstants.LEFT);
        table.setupColumn(DOUBLE_COLUMN, 130, SwingConstants.LEFT);
        this.add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        // Display condition flags in panel below the registers
        JPanel flagsPane = new JPanel(new BorderLayout(0, 6));
        flagsPane.setBorder(new EmptyBorder(6, 6, 6, 6));
        flagsPane.setToolTipText("Flags are used by certain floating point instructions, default flag is 0");
        flagsPane.add(new JLabel("Condition Flags", JLabel.CENTER), BorderLayout.NORTH);
        int flagCount = Coprocessor1.getConditionFlagCount();
        conditionFlagCheckBoxes = new JCheckBox[flagCount];
        JPanel checksPane = new JPanel(new GridLayout(2, flagCount / 2));
        for (int flag = 0; flag < flagCount; flag++) {
            conditionFlagCheckBoxes[flag] = new JCheckBox(Integer.toString(flag));
            final int finalFlag = flag;
            conditionFlagCheckBoxes[flag].addActionListener(event -> {
                JCheckBox checkBox = (JCheckBox) event.getSource();
                if (checkBox.isSelected()) {
                    checkBox.setSelected(true);
                    Coprocessor1.setConditionFlag(finalFlag);
                }
                else {
                    checkBox.setSelected(false);
                    Coprocessor1.clearConditionFlag(finalFlag);
                }
            });
            conditionFlagCheckBoxes[flag].setToolTipText("Flag " + flag);
            checksPane.add(conditionFlagCheckBoxes[flag]);
        }
        flagsPane.add(checksPane, BorderLayout.CENTER);
        this.add(flagsPane, BorderLayout.SOUTH);
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
    public Object[][] setupWindow() {
        Object[][] tableData = new Object[32][3];
        for (Register register : Coprocessor1.getRegisters()) {
            tableData[register.getNumber()][0] = register.getName();
            tableData[register.getNumber()][1] = NumberDisplayBaseChooser.formatFloatNumber(register.getValue(), NumberDisplayBaseChooser.getBase(Application.getSettings().displayValuesInHex.get()));
            if (register.getNumber() % 2 == 0) {
                // Even numbered double registers
                long longValue = 0;
                try {
                    longValue = Coprocessor1.getLongFromRegisterPair(register.getName());
                }
                catch (InvalidRegisterAccessException exception) {
                    // Cannot happen since row must be even
                }
                tableData[register.getNumber()][2] = NumberDisplayBaseChooser.formatDoubleNumber(longValue, NumberDisplayBaseChooser.getBase(Application.getSettings().displayValuesInHex.get()));
            }
            else {
                tableData[register.getNumber()][2] = "";
            }
        }
        return tableData;
    }

    /**
     * Reset and redisplay registers.
     */
    public void clearWindow() {
        clearHighlighting();
        Coprocessor1.resetRegisters();
        updateRegisters(gui.getMainPane().getExecuteTab().getValueDisplayBase());
        Coprocessor1.clearConditionFlags();
        updateConditionFlagDisplay();
    }

    /**
     * Update register display using specified number base (10 or 16).
     *
     * @param base Desired number base.
     */
    @Override
    public void updateRegisters(int base) {
        for (Register register : Coprocessor1.getRegisters()) {
            updateFloatRegisterValue(register.getNumber(), register.getValue(), base);
            if (register.getNumber() % 2 == 0) {
                try {
                    long value = Coprocessor1.getLongFromRegisterPair(register.getNumber());
                    updateDoubleRegisterValue(register.getNumber(), value, base);
                }
                catch (InvalidRegisterAccessException exception) {
                    // Should not happen because the register number is always even
                }
            }
        }
        updateConditionFlagDisplay();
    }

    private void updateConditionFlagDisplay() {
        for (int flag = 0; flag < conditionFlagCheckBoxes.length; flag++) {
            conditionFlagCheckBoxes[flag].setSelected(Coprocessor1.getConditionFlag(flag) != 0);
        }
    }

    /**
     * This method handles the updating of the GUI.  Does not affect actual register.
     *
     * @param number The number of the float register whose display to update.
     * @param value  New value, as an int.
     * @param base   The number base for display (e.g. 10, 16)
     */
    public void updateFloatRegisterValue(int number, int value, int base) {
        ((RegisterTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatFloatNumber(value, base), number, FLOAT_COLUMN);
    }

    /**
     * This method handles the updating of the GUI.  Does not affect actual register.
     *
     * @param number The number of the double register to update. (Must be even.)
     * @param value  New value, as a long.
     * @param base   The number base for display (e.g. 10, 16)
     */
    public void updateDoubleRegisterValue(int number, long value, int base) {
        ((RegisterTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatDoubleNumber(value, base), number, DOUBLE_COLUMN);
    }

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register Register object corresponding to row to be selected.
     */
    @Override
    public void highlightRegister(Register register) {
        table.highlightRow(register.getNumber());
    }

    @Override
    public void startObservingRegisters() {
        for (Register register : Coprocessor1.getRegisters()) {
            register.addListener(this);
        }
    }

    @Override
    public void stopObservingRegisters() {
        for (Register register : Coprocessor1.getRegisters()) {
            register.addListener(this);
        }
    }

    private class RegisterTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Name", "Single", "Double"};

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
         * Float column and even-numbered rows of double column are editable.
         */
        @Override
        public boolean isCellEditable(int row, int col) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            return col == FLOAT_COLUMN || (col == DOUBLE_COLUMN && row % 2 == 0);
        }

        /**
         * Update cell contents in table model.  This method should be called
         * only when user edits cell, so input validation has to be done.  If
         * value is valid, MIPS register is updated.
         */
        @Override
        public void setValueAt(Object value, int row, int col) {
            int valueBase = gui.getMainPane().getExecuteTab().getValueDisplayBase();
            String stringValue = value.toString();
            try {
                if (col == FLOAT_COLUMN) {
                    if (Binary.isHex(stringValue)) {
                        // Avoid using Float.intBitsToFloat() b/c it may not preserve NaN value.
                        int intValue = Binary.decodeInteger(stringValue);
                        // Assures that if changed during MIPS program execution, the update will
                        // occur only between MIPS instructions.
                        synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                            Coprocessor1.updateRegister(row, intValue);
                        }
                        data[row][col] = NumberDisplayBaseChooser.formatFloatNumber(intValue, valueBase);
                    }
                    else {
                        // Is not hex, so must be decimal
                        float floatValue = Float.parseFloat(stringValue);
                        // Assures that if changed during MIPS program execution, the update will
                        // occur only between MIPS instructions.
                        synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                            Coprocessor1.setRegisterToFloat(row, floatValue);
                        }
                        data[row][col] = NumberDisplayBaseChooser.formatNumber(floatValue, valueBase);
                    }
                    // Update corresponding double display
                    int register = row - (row % 2);
                    setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatDoubleNumber(Coprocessor1.getLongFromRegisterPair(register), valueBase), register, DOUBLE_COLUMN);
                }
                else if (col == DOUBLE_COLUMN) {
                    if (Binary.isHex(stringValue)) {
                        long longValue = Binary.decodeLong(stringValue);
                        // Assures that if changed during MIPS program execution, the update will
                        // occur only between MIPS instructions.
                        synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                            Coprocessor1.setRegisterPairToLong(row, longValue);
                        }
                        setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatDoubleNumber(longValue, valueBase), row, col);
                    }
                    else {
                        // Is not hex, so must be decimal
                        double doubleValue = Double.parseDouble(stringValue);
                        // Assures that if changed during MIPS program execution, the update will
                        // occur only between MIPS instructions.
                        synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                            Coprocessor1.setRegisterPairToDouble(row, doubleValue);
                        }
                        setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(doubleValue, valueBase), row, col);
                    }
                    // Update corresponding float display
                    setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(Coprocessor1.getValue(row), valueBase), row, FLOAT_COLUMN);
                    setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(Coprocessor1.getValue(row + 1), valueBase), row + 1, FLOAT_COLUMN);
                }
            }
            catch (NumberFormatException exception) {
                data[row][col] = "INVALID";
                fireTableCellUpdated(row, col);
            }
            catch (InvalidRegisterAccessException exception) {
                // Should not occur; code below will re-display original value
                fireTableCellUpdated(row, col);
            }
        }

        /**
         * Update cell contents in table model.  Does not affect MIPS register.
         */
        public void setDisplayAndModelValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }
}