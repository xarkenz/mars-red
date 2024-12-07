package mars.venus;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

// Adapted from https://stackoverflow.com/a/2628985, which is in turn adapted from
// https://docs.oracle.com/javase/tutorial/2d/text/drawmulstring.html
public class WrappingCellRenderer extends DefaultTableCellRenderer {
    private float lineHeight;
    private final TableCache<CellCache> cache = new TableCache<>();
    private String outOfBoundsText = "";

    private JTable table = null;
    private int row = -1;
    private int column = -1;
    private boolean isPainting = false;

    public WrappingCellRenderer() {
        this(1.0f);
    }

    public WrappingCellRenderer(float lineHeight) {
        this.lineHeight = lineHeight;
    }

    public float getLineHeight() {
        return this.lineHeight;
    }

    public void setLineHeight(float lineHeight) {
        this.lineHeight = lineHeight;
    }

    @Override
    public String getText() {
        if (this.cache == null || this.row < 0 || this.column < 0) {
            return this.outOfBoundsText;
        }

        return (this.isPainting) ? "" : this.getCellCache(this.row, this.column).text;
    }

    @Override
    public void setText(String text) {
        if (this.cache == null || this.row < 0 || this.column < 0) {
            this.outOfBoundsText = text;
            return;
        }

        CellCache cell = this.getCellCache(this.row, this.column);

        if (!cell.text.equals(text)) {
            cell.text = text;
            cell.lineMeasurer = null;
            cell.lineLayouts = null;
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.table = table;
        this.row = row;
        this.column = column;

        this.cache.updateDimensions(table.getRowCount(), table.getColumnCount());

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        this.isPainting = true;
        super.paintComponent(graphics);
        this.isPainting = false;

        if (this.row < 0 || this.column < 0) {
            return;
        }

        CellCache cell = this.getCellCache(this.row, this.column);
        if (cell.text.isEmpty()) {
            return;
        }

        graphics.setColor(this.getForeground());

        if (cell.width != this.getWidth()) {
            cell.lineLayouts = null;
            cell.width = this.getWidth();
        }

        Graphics2D graphics2D = (Graphics2D) graphics;

        Object textAntialiasingHint = UIManager.getLookAndFeelDefaults().get(RenderingHints.KEY_TEXT_ANTIALIASING);
        if (textAntialiasingHint != null) {
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAntialiasingHint);
        }

        Insets insets = this.getInsets();
        int contentWidth = cell.width - insets.left - insets.right;

        if (cell.lineMeasurer == null) {
            cell.lineMeasurer = new LineBreakMeasurer(
                new AttributedString(cell.text, graphics2D.getFont().getAttributes()).getIterator(),
                BreakIterator.getLineInstance(),
                graphics2D.getFontRenderContext()
            );
        }
        if (cell.lineLayouts == null) {
            List<TextLayout> lineLayouts = new ArrayList<>();
            // Set position to the index of the first character in the paragraph
            cell.lineMeasurer.setPosition(0);
            // Get lines until the entire paragraph has been displayed
            while (cell.lineMeasurer.getPosition() < cell.text.length()) {
                // Ensure that newline characters are handled properly
                int offsetLimit = cell.lineMeasurer.nextOffset(contentWidth);
                for (int index = cell.lineMeasurer.getPosition(); index < offsetLimit; index++) {
                    if (cell.text.charAt(index) == '\n') {
                        offsetLimit = index + 1;
                        break;
                    }
                }
                // Retrieve the next line layout
                TextLayout layout = cell.lineMeasurer.nextLayout(contentWidth, offsetLimit, false);
                lineLayouts.add(layout);
            }
            cell.lineLayouts = lineLayouts.toArray(new TextLayout[0]);
        }

        float drawPosY = insets.top;
        for (TextLayout layout : cell.lineLayouts) {
            float adjustedLeading = (this.getLineHeight() - 1.0f) * (layout.getAscent() + layout.getDescent())
                + this.getLineHeight() * layout.getLeading();
            // Compute pen x position. If the paragraph is right-to-left we
            // will align the TextLayouts to the right edge of the panel.
            // Note: drawPosX is always where the LEFT of the text is placed.
            float drawPosX = (layout.isLeftToRight())
                ? insets.left
                : cell.width - insets.right - layout.getAdvance();
            // Move y-coordinate by the ascent of the layout
            drawPosY += adjustedLeading * 0.5f + layout.getAscent();
            // Draw the TextLayout at (drawPosX, drawPosY)
            layout.draw(graphics2D, drawPosX, drawPosY);
            // Move y-coordinate in preparation for next layout
            drawPosY += layout.getDescent() + adjustedLeading * 0.5f;
        }
        cell.height = (int) Math.ceil(drawPosY) + insets.bottom;

        this.recalculateRowHeight(this.row);
    }

    private void recalculateRowHeight(int row) {
        int maxHeight = this.table.getRowHeight();

        for (int column = 0; column < this.table.getColumnCount(); column++) {
            maxHeight = Math.max(maxHeight, this.getCellCache(row, column).height);
        }

        if (maxHeight != this.table.getRowHeight(row)) {
            this.table.setRowHeight(row, maxHeight);
        }
    }

    private CellCache getCellCache(int row, int column) {
        CellCache cell = this.cache.get(row, column);
        if (cell == null) {
            cell = new CellCache();
            this.cache.set(row, column, cell);
        }
        return cell;
    }

    private static class CellCache {
        public String text = "";
        public int width = -1;
        public LineBreakMeasurer lineMeasurer = null;
        public TextLayout[] lineLayouts = null;
        public int height = -1;
    }
}
