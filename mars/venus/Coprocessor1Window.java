package mars.venus;

import mars.Globals;
import mars.settings.Settings;
import mars.mips.hardware.*;
import mars.simulator.Simulator;
import mars.simulator.SimulatorNotice;
import mars.util.Binary;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
 * Sets up a window to display Coprocessor 1 registers in the Registers pane of the UI.
 *
 * @author Pete Sanderson 2005
 */
public class Coprocessor1Window extends JPanel implements ActionListener, Observer {
    private static final int NAME_COLUMN = 0;
    private static final int FLOAT_COLUMN = 1;
    private static final int DOUBLE_COLUMN = 2;

    private final JTable table;
    private final JCheckBox[] conditionFlagCheckBoxes;
    private boolean highlighting;
    private int highlightRow;

    /**
     * Constructor which sets up a fresh window with a table that contains the register values.
     */
    public Coprocessor1Window() {
        Simulator.getInstance().addObserver(this);
        // Display registers in table contained in scroll pane.
        this.setLayout(new BorderLayout()); // table display will occupy entire width if widened
        table = new MyTippedJTable(new RegTableModel(setupWindow()));
        table.getColumnModel().getColumn(NAME_COLUMN).setPreferredWidth(20);
        table.getColumnModel().getColumn(FLOAT_COLUMN).setPreferredWidth(70);
        table.getColumnModel().getColumn(DOUBLE_COLUMN).setPreferredWidth(130);
        // Display register values (String-ified) right-justified in mono font
        table.getColumnModel().getColumn(NAME_COLUMN).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.LEFT));
        table.getColumnModel().getColumn(FLOAT_COLUMN).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
        table.getColumnModel().getColumn(DOUBLE_COLUMN).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
        this.add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        // Display condition flags in panel below the registers
        JPanel flagsPane = new JPanel(new BorderLayout());
        flagsPane.setToolTipText("Flags are used by certain floating point instructions, default flag is 0");
        flagsPane.add(new JLabel("Condition Flags", JLabel.CENTER), BorderLayout.NORTH);
        int numFlags = Coprocessor1.getConditionFlagCount();
        conditionFlagCheckBoxes = new JCheckBox[numFlags];
        JPanel checksPane = new JPanel(new GridLayout(2, numFlags / 2));
        for (int i = 0; i < numFlags; i++) {
            conditionFlagCheckBoxes[i] = new JCheckBox(Integer.toString(i));
            conditionFlagCheckBoxes[i].addActionListener(this);
            conditionFlagCheckBoxes[i].setBackground(Color.WHITE);
            conditionFlagCheckBoxes[i].setToolTipText("Flag " + i);
            checksPane.add(conditionFlagCheckBoxes[i]);
        }
        flagsPane.add(checksPane, BorderLayout.CENTER);
        this.add(flagsPane, BorderLayout.SOUTH);
    }

    /**
     * Called when user clicks on a condition flag checkbox.
     * Updates both the display and the underlying Coprocessor 1 flag.
     *
     * @param e component that triggered this call
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        JCheckBox checkBox = (JCheckBox) e.getSource();
        int i = Integer.parseInt(checkBox.getText());
        if (checkBox.isSelected()) {
            checkBox.setSelected(true);
            Coprocessor1.setConditionFlag(i);
        }
        else {
            checkBox.setSelected(false);
            Coprocessor1.clearConditionFlag(i);
        }
    }

    /**
     * Sets up the data for the window.
     *
     * @return The array object with the data for the window.
     */
    public Object[][] setupWindow() {
        Register[] registers = Coprocessor1.getRegisters();
        this.highlighting = false;
        Object[][] tableData = new Object[registers.length][3];
        for (int i = 0; i < registers.length; i++) {
            tableData[i][0] = registers[i].getName();
            tableData[i][1] = NumberDisplayBaseChooser.formatFloatNumber(registers[i].getValue(), NumberDisplayBaseChooser.getBase(Globals.getSettings().displayValuesInHex.get()));
            if (i % 2 == 0) {
                // even numbered double registers
                long longValue = 0;
                try {
                    longValue = Coprocessor1.getLongFromRegisterPair(registers[i].getName());
                }
                catch (InvalidRegisterAccessException e) {
                    // cannot happen since i must be even
                }
                tableData[i][2] = NumberDisplayBaseChooser.formatDoubleNumber(longValue, NumberDisplayBaseChooser.getBase(Globals.getSettings().displayValuesInHex.get()));
            }
            else {
                tableData[i][2] = "";
            }
        }
        return tableData;
    }

    /**
     * Reset and redisplay registers.
     */
    public void clearWindow() {
        this.clearHighlighting();
        Coprocessor1.resetRegisters();
        this.updateRegisters(Globals.getGUI().getMainPane().getExecutePane().getValueDisplayBase());
        Coprocessor1.clearConditionFlags();
        this.updateConditionFlagDisplay();
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
     * Redisplay registers using current display number base (10 or 16)
     */
    public void updateRegisters() {
        updateRegisters(Globals.getGUI().getMainPane().getExecutePane().getValueDisplayBase());
    }

    /**
     * Redisplay registers using specified display number base (10 or 16)
     *
     * @param base number base for display (10 or 16)
     */
    public void updateRegisters(int base) {
        for (Register register : Coprocessor1.getRegisters()) {
            updateFloatRegisterValue(register.getNumber(), register.getValue(), base);
            if (register.getNumber() % 2 == 0) {
                try {
                    long value = Coprocessor1.getLongFromRegisterPair(register.getNumber());
                    updateDoubleRegisterValue(register.getNumber(), value, base);
                }
                catch (InvalidRegisterAccessException e) {
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
        ((RegTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatFloatNumber(value, base), number, FLOAT_COLUMN);
    }

    /**
     * This method handles the updating of the GUI.  Does not affect actual register.
     *
     * @param number The number of the double register to update. (Must be even.)
     * @param value  New value, as a long.
     * @param base   The number base for display (e.g. 10, 16)
     */
    public void updateDoubleRegisterValue(int number, long value, int base) {
        ((RegTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatDoubleNumber(value, base), number, DOUBLE_COLUMN);
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
                    Coprocessor1.addRegistersObserver(this);
                    this.highlighting = true;
                }
            }
            else {
                // Simulated MIPS execution stops.  Stop responding.
                Coprocessor1.deleteRegistersObserver(this);
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
        private static final String[] COLUMN_NAMES = {"Name", "Float", "Double"};

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
            int valueBase = Globals.getGUI().getMainPane().getExecutePane().getValueDisplayBase();
            String stringValue = value.toString();
            try {
                if (col == FLOAT_COLUMN) {
                    if (Binary.isHex(stringValue)) {
                        // Avoid using Float.intBitsToFloat() b/c it may not preserve NaN value.
                        int intValue = Binary.stringToInt(stringValue);
                        //  Assures that if changed during MIPS program execution, the update will
                        //  occur only between MIPS instructions.
                        synchronized (Globals.MEMORY_AND_REGISTERS_LOCK) {
                            Coprocessor1.updateRegister(row, intValue);
                        }
                        data[row][col] = NumberDisplayBaseChooser.formatFloatNumber(intValue, valueBase);
                    }
                    else {
                        float floatValue = Float.parseFloat(stringValue);
                        //  Assures that if changed during MIPS program execution, the update will
                        //  occur only between MIPS instructions.
                        synchronized (Globals.MEMORY_AND_REGISTERS_LOCK) {
                            Coprocessor1.setRegisterToFloat(row, floatValue);
                        }
                        data[row][col] = NumberDisplayBaseChooser.formatNumber(floatValue, valueBase);
                    }
                    // have to update corresponding double display
                    int dReg = row - (row % 2);
                    setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatDoubleNumber(Coprocessor1.getLongFromRegisterPair(dReg), valueBase), dReg, DOUBLE_COLUMN);
                }
                else if (col == DOUBLE_COLUMN) {
                    if (Binary.isHex(stringValue)) {
                        long longValue = Binary.stringToLong(stringValue);
                        //  Assures that if changed during MIPS program execution, the update will
                        //  occur only between MIPS instructions.
                        synchronized (Globals.MEMORY_AND_REGISTERS_LOCK) {
                            Coprocessor1.setRegisterPairToLong(row, longValue);
                        }
                        setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatDoubleNumber(longValue, valueBase), row, col);
                    }
                    else {
                        // is not hex, so must be decimal
                        double doubleValue = Double.parseDouble(stringValue);
                        //  Assures that if changed during MIPS program execution, the update will
                        //  occur only between MIPS instructions.
                        synchronized (Globals.MEMORY_AND_REGISTERS_LOCK) {
                            Coprocessor1.setRegisterPairToDouble(row, doubleValue);
                        }
                        setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(doubleValue, valueBase), row, col);
                    }
                    // have to update corresponding float display
                    setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(Coprocessor1.getValue(row), valueBase), row, FLOAT_COLUMN);
                    setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(Coprocessor1.getValue(row + 1), valueBase), row + 1, FLOAT_COLUMN);
                }
            }
            catch (NumberFormatException nfe) {
                data[row][col] = "INVALID";
                fireTableCellUpdated(row, col);
            }
            catch (InvalidRegisterAccessException e) {
                // Should not occur; code below will re-display original value
                fireTableCellUpdated(row, col);
            }
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

        private final String[] COLUMN_TOOL_TIPS = {
            "Each register has a tool tip describing its usage convention", // Name
            "32-bit single precision IEEE 754 floating point register", // Float
            "64-bit double precision IEEE 754 floating point register (uses a pair of 32-bit registers)", // Double
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