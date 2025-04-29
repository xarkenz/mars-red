package mars.venus;

import javax.swing.table.AbstractTableModel;

public class StaticTableModel extends AbstractTableModel {
    private final Object[][] rowData;
    private final Object[] columnNames;

    public StaticTableModel(Object[][] rowData, Object[] columnNames) {
        this.rowData = rowData;
        this.columnNames = columnNames;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return this.columnNames[columnIndex].toString();
    }

    @Override
    public int getRowCount() {
        return this.rowData.length;
    }

    @Override
    public int getColumnCount() {
        return this.columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return this.rowData[rowIndex][columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Literally the entire reason I made this class
        // Why do all the default table models hardcode this to true???
        return false;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        this.rowData[rowIndex][columnIndex] = value;
        this.fireTableCellUpdated(rowIndex, columnIndex);
    }
}
