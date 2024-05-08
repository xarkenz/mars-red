package mars.venus;

import mars.Application;
import mars.settings.Settings;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseEvent;

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
 * JTable subclass to provide custom tool tips for each of the
 * register table column headers and for each register name in the first column. Based on
 * <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/table.html">Sun's JTable tutorial</a>.
 */
public class RegistersTable extends JTable {
    public static final int NAME_COLUMN = 0;

    private final String[] headerTips;
    private final String[] registerTips;
    private boolean isUpdating;
    private int highlightedRow;

    public RegistersTable(TableModel model, String[] headerTips, String[] registerTips) {
        super(model);
        this.headerTips = headerTips;
        this.registerTips = registerTips;
        this.isUpdating = false;
        this.highlightedRow = -1;
    }

    public void setupColumn(int columnIndex, int preferredWidth, int alignment) {
        TableColumn column = this.getColumnModel().getColumn(columnIndex);
        column.setPreferredWidth(preferredWidth);
        column.setCellRenderer(new RegisterCellRenderer(alignment));
    }

    public void setUpdating(boolean isUpdating) {
        this.isUpdating = isUpdating;
    }

    /**
     * Clear highlight background color from any cell currently highlighted.
     */
    public void clearHighlighting() {
        this.isUpdating = false;
        this.highlightedRow = -1; // Assure highlight will not occur upon re-assemble
        this.tableChanged(new TableModelEvent(this.getModel()));
    }

    /**
     * Refresh the table, triggering re-rendering.
     */
    public void refresh() {
        this.tableChanged(new TableModelEvent(this.getModel()));
    }

    /**
     * Highlight the given row, removing highlighting from all other rows.
     *
     * @param row The row to be highlighted.
     */
    public void highlightRow(int row) {
        this.highlightedRow = row;
        this.refresh();
    }

    // Implement table cell tool tips
    @Override
    public String getToolTipText(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int column = columnAtPoint(event.getPoint());
        int modelColumn = convertColumnIndexToModel(column);
        if (modelColumn == NAME_COLUMN) {
            return registerTips[row];
        }
        else {
            return super.getToolTipText(event);
        }
    }

    // Implement table header tool tips
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(MouseEvent event) {
                int column = columnModel.getColumnIndexAtX(event.getPoint().x);
                int modelColumn = columnModel.getColumn(column).getModelIndex();
                return headerTips[modelColumn];
            }
        };
    }

    /**
     * Cell renderer for displaying register entries.
     */
    private class RegisterCellRenderer extends DefaultTableCellRenderer {
        private final int alignment;

        public RegisterCellRenderer(int alignment) {
            super();
            this.alignment = alignment;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Settings settings = Application.getSettings();

            this.setHorizontalAlignment(alignment);
            if (settings.highlightRegisters.get() && isUpdating && row == highlightedRow) {
                this.setBackground(settings.registerHighlightBackground.get());
                this.setForeground(settings.registerHighlightForeground.get());
            }
            else {
                this.setBackground(null);
                this.setForeground(null);
            }

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            this.setFont(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT);

            return this;
        }
    }
}