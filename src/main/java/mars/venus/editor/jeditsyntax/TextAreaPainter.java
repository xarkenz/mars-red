/*
 * TextAreaPainter.java - Paints the text area
 * Copyright (C) 1999 Slava Pestov
 *
 * 08/05/2002	Cursor (caret) rendering fixed for JDK 1.4 (Anonymous)
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package mars.venus.editor.jeditsyntax;

import mars.venus.editor.jeditsyntax.tokenmarker.Token;
import mars.venus.editor.jeditsyntax.tokenmarker.TokenMarker;

import javax.swing.*;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * The text area repaint manager. It performs double buffering and paints
 * lines of text.
 *
 * @author Slava Pestov
 * @version $Id: TextAreaPainter.java,v 1.24 1999/12/13 03:40:30 sp Exp $
 */
public class TextAreaPainter extends JComponent implements TabExpander {
    /**
     * Creates a new repaint manager. This should be not be called
     * directly.
     */
    public TextAreaPainter(JEditTextArea textArea, TextAreaDefaults defaults) {
        this.textArea = textArea;

        setAutoscrolls(true);
        setDoubleBuffered(true);
        setOpaque(true);

        ToolTipManager.sharedInstance().registerComponent(this);

        currentLine = new Segment();
        currentLineIndex = -1;

        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        setFont(new Font("Courier New", Font.PLAIN, 14));
        setForeground(defaults.foregroundColor);
        setBackground(defaults.backgroundColor);

        tabSizeChars = defaults.tabSize;
        blockCaret = defaults.blockCaret;
        styles = defaults.styles;
        cols = defaults.cols;
        rows = defaults.rows;
        caretForeground = defaults.caretForeground;
        selectionBackground = defaults.selectionBackground;
        lineHighlightColor = defaults.lineHighlightColor;
        lineHighlight = defaults.lineHighlightVisible;
        bracketHighlightColor = defaults.bracketHighlightColor;
        bracketHighlight = defaults.bracketHighlightVisible;
        paintInvalid = defaults.paintInvalid;
        eolMarkerColor = defaults.eolMarkerColor;
        eolMarkers = defaults.eolMarkerVisible;
    }

    /**
     * Fetch the tab size in characters.  DPS 12-May-2010.
     *
     * @return int tab size in characters
     */
    public int getTabSize() {
        return tabSizeChars;
    }

    /**
     * Set the tab size in characters. DPS 12-May-2010.
     * Originally it was fixed at PlainDocument property
     * value (8).
     *
     * @param size tab size in characters
     */
    public void setTabSize(int size) {
        tabSizeChars = size;
    }

    /**
     * Returns the syntax styles used to paint colorized text. Entry <var>n</var>
     * will be used to paint tokens with id = <var>n</var>.
     *
     * @see Token
     */
    public final SyntaxStyle[] getStyles() {
        return styles;
    }

    /**
     * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
     * will be used to paint tokens with id = <i>n</i>.
     *
     * @param styles The syntax styles
     * @see Token
     */
    public final void setStyles(SyntaxStyle[] styles) {
        this.styles = styles;
        repaint();
    }

    /**
     * Returns the caret color.
     */
    public final Color getCaretForeground() {
        return caretForeground;
    }

    /**
     * Sets the caret color.
     *
     * @param caretForeground The caret color
     */
    public final void setCaretForeground(Color caretForeground) {
        this.caretForeground = caretForeground;
        invalidateSelectedLines();
    }

    /**
     * Returns the selection color.
     */
    public final Color getSelectionBackground() {
        return selectionBackground;
    }

    /**
     * Sets the selection color.
     *
     * @param selectionBackground The selection color
     */
    public final void setSelectionBackground(Color selectionBackground) {
        this.selectionBackground = selectionBackground;
        invalidateSelectedLines();
    }

    /**
     * Returns the line highlight color.
     */
    public final Color getLineHighlightColor() {
        return lineHighlightColor;
    }

    /**
     * Sets the line highlight color.
     *
     * @param lineHighlightColor The line highlight color
     */
    public final void setLineHighlightColor(Color lineHighlightColor) {
        this.lineHighlightColor = lineHighlightColor;
        invalidateSelectedLines();
    }

    /**
     * Returns true if line highlight is enabled, false otherwise.
     */
    public final boolean isLineHighlightEnabled() {
        return lineHighlight;
    }

    /**
     * Enables or disables current line highlighting.
     *
     * @param lineHighlight True if current line highlight should be enabled,
     *                      false otherwise
     */
    public final void setLineHighlightEnabled(boolean lineHighlight) {
        this.lineHighlight = lineHighlight;
        invalidateSelectedLines();
    }

    /**
     * Returns the bracket highlight color.
     */
    public final Color getBracketHighlightColor() {
        return bracketHighlightColor;
    }

    /**
     * Sets the bracket highlight color.
     *
     * @param bracketHighlightColor The bracket highlight color
     */
    public final void setBracketHighlightColor(Color bracketHighlightColor) {
        this.bracketHighlightColor = bracketHighlightColor;
        invalidateLine(textArea.getBracketLine());
    }

    /**
     * Returns true if bracket highlighting is enabled, false otherwise.
     * When bracket highlighting is enabled, the bracket matching the
     * one before the caret (if any) is highlighted.
     */
    public final boolean isBracketHighlightEnabled() {
        return bracketHighlight;
    }

    /**
     * Enables or disables bracket highlighting.
     * When bracket highlighting is enabled, the bracket matching the
     * one before the caret (if any) is highlighted.
     *
     * @param bracketHighlight True if bracket highlighting should be
     *                         enabled, false otherwise
     */
    public final void setBracketHighlightEnabled(boolean bracketHighlight) {
        this.bracketHighlight = bracketHighlight;
        invalidateLine(textArea.getBracketLine());
    }

    /**
     * Returns true if the caret should be drawn as a block, false otherwise.
     */
    public final boolean isBlockCaretEnabled() {
        return blockCaret;
    }

    /**
     * Sets if the caret should be drawn as a block, false otherwise.
     *
     * @param blockCaret True if the caret should be drawn as a block,
     *                   false otherwise.
     */
    public final void setBlockCaretEnabled(boolean blockCaret) {
        this.blockCaret = blockCaret;
        invalidateSelectedLines();
    }

    /**
     * Returns the EOL marker color.
     */
    public final Color getEOLMarkerColor() {
        return eolMarkerColor;
    }

    /**
     * Sets the EOL marker color.
     *
     * @param eolMarkerColor The EOL marker color
     */
    public final void setEOLMarkerColor(Color eolMarkerColor) {
        this.eolMarkerColor = eolMarkerColor;
        repaint();
    }

    /**
     * Returns true if EOL markers are drawn, false otherwise.
     */
    public final boolean getEOLMarkersPainted() {
        return eolMarkers;
    }

    /**
     * Sets if EOL markers are to be drawn.
     *
     * @param eolMarkers True if EOL markers should be drawn, false otherwise
     */
    public final void setEOLMarkersPainted(boolean eolMarkers) {
        this.eolMarkers = eolMarkers;
        repaint();
    }

    /**
     * Returns true if invalid lines are painted as red tildes (~),
     * false otherwise.
     */
    public boolean getInvalidLinesPainted() {
        return paintInvalid;
    }

    /**
     * Sets if invalid lines are to be painted as red tildes.
     *
     * @param paintInvalid True if invalid lines should be drawn, false otherwise
     */
    public void setInvalidLinesPainted(boolean paintInvalid) {
        this.paintInvalid = paintInvalid;
    }

    /**
     * Adds a custom highlight painter.
     *
     * @param highlight The highlight
     */
    public void addCustomHighlight(Highlight highlight) {
        highlight.init(textArea, highlights);
        highlights = highlight;
    }

    /**
     * Highlight interface.
     */
    public interface Highlight {
        /**
         * Called after the highlight painter has been added.
         *
         * @param textArea The text area
         * @param next     The painter this one should delegate to
         */
        void init(JEditTextArea textArea, Highlight next);

        /**
         * This should paint the highlight and delegate to the
         * next highlight painter.
         *
         * @param gfx  The graphics context
         * @param line The line number
         * @param y    The y co-ordinate of the line
         */
        void paintHighlight(Graphics gfx, int line, int y);

        /**
         * Returns the tool tip to display at the specified
         * location. If this highlighter doesn't know what to
         * display, it should delegate to the next highlight
         * painter.
         *
         * @param event The mouse event
         */
        String getToolTipText(MouseEvent event);
    }

    /**
     * Returns the tool tip to display at the specified location.
     *
     * @param event The mouse event
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        if (highlights != null) {
            return highlights.getToolTipText(event);
        }
        else if (this.textArea.getTokenMarker() == null) {
            return null;
        }
        else {
            return this.textArea.getSyntaxSensitiveToolTipText(event.getX(), event.getY());
        }
    }

    /**
     * Returns the font metrics used by this component.
     */
    public FontMetrics getFontMetrics() {
        return fontMetrics;
    }

    /**
     * Sets the font for this component. This is overridden to update the
     * cached font metrics and to recalculate which lines are visible.
     *
     * @param font The font
     */
    @Override
    public void setFont(Font font) {
        super.setFont(font);
        fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
        tabSize = fontMetrics.charWidth(' ') * tabSizeChars;
        textArea.recalculateVisibleLines();
    }

    /**
     * Repaints the text.
     *
     * @param graphics The graphics context
     */
    @Override
    public void paint(Graphics graphics) {
        // Added 4/6/10 DPS to set antialiasing for text rendering - smoother letters
        // Second one says choose algorithm for quality over speed
        ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        tabSize = fontMetrics.charWidth(' ') * tabSizeChars;

        Rectangle clipRect = graphics.getClipBounds();

        graphics.setColor(getBackground());
        graphics.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);

        // We don't use yToLine() here because that method doesn't
        // return lines past the end of the document
        int height = fontMetrics.getHeight();
        int firstLine = textArea.getFirstLine();
        int firstInvalid = firstLine + clipRect.y / height;
        // Because the clipRect's height is usually an even multiple
        // of the font height, we subtract 1 from it, otherwise one
        // too many lines will always be painted.
        int lastInvalid = firstLine + (clipRect.y + clipRect.height - 1) / height;

        try {
            TokenMarker tokenMarker = ((SyntaxDocument) textArea.getDocument()).getTokenMarker();
            int x = textArea.getHorizontalOffset();

            for (int line = firstInvalid; line <= lastInvalid; line++) {
                paintLine(graphics, tokenMarker, line, x);
            }

            if (tokenMarker != null && tokenMarker.isNextLineRequested()) {
                int h = clipRect.y + clipRect.height;
                repaint(0, h, getWidth(), getHeight() - h);
            }
        }
        catch (Exception exception) {
            System.err.println("Error repainting line" + " range {" + firstInvalid + "," + lastInvalid + "}:");
            exception.printStackTrace(System.err);
        }
    }

    /**
     * Marks a line as needing a repaint.
     *
     * @param line The line to invalidate
     */
    public final void invalidateLine(int line) {
        repaint(0, textArea.lineToY(line) + fontMetrics.getMaxDescent() + fontMetrics.getLeading(), getWidth(), fontMetrics.getHeight());
    }

    /**
     * Marks a range of lines as needing a repaint.
     *
     * @param firstLine The first line to invalidate
     * @param lastLine  The last line to invalidate
     */
    public final void invalidateLineRange(int firstLine, int lastLine) {
        repaint(0, textArea.lineToY(firstLine) + fontMetrics.getMaxDescent() + fontMetrics.getLeading(), getWidth(), (lastLine - firstLine + 1) * fontMetrics.getHeight());
    }

    /**
     * Repaints the lines containing the selection.
     */
    public final void invalidateSelectedLines() {
        invalidateLineRange(textArea.getSelectionStartLine(), textArea.getSelectionEndLine());
    }

    /**
     * Implementation of TabExpander interface. Returns next tab stop after
     * a specified point.
     *
     * @param x         The x coordinate.
     * @param tabOffset Ignored.
     * @return The next tab stop after <code>x</code>.
     */
    @Override
    public float nextTabStop(float x, int tabOffset) {
        int offset = textArea.getHorizontalOffset();
        int numTabs = ((int) x - offset) / tabSize;
        return (numTabs + 1) * tabSize + offset;
    }

    /**
     * Returns the painter's preferred size.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension dim = new Dimension();
        dim.width = fontMetrics.charWidth('w') * cols;
        dim.height = fontMetrics.getHeight() * rows;
        return dim;
    }

    /**
     * Returns the painter's minimum size.
     */
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    // package-private members
    int currentLineIndex;
    Token currentLineTokens;
    Segment currentLine;

    // protected members
    protected JEditTextArea textArea;

    protected SyntaxStyle[] styles;
    protected Color caretForeground;
    protected Color selectionBackground;
    protected Color lineHighlightColor;
    protected Color bracketHighlightColor;
    protected Color eolMarkerColor;

    protected boolean blockCaret;
    protected boolean lineHighlight;
    protected boolean bracketHighlight;
    protected boolean paintInvalid;
    protected boolean eolMarkers;
    protected int cols;
    protected int rows;

    protected int tabSize, tabSizeChars;
    protected FontMetrics fontMetrics;

    protected Highlight highlights;

    protected void paintLine(Graphics graphics, TokenMarker tokenMarker, int line, int x) {
        Font defaultFont = getFont();
        Color defaultColor = getForeground();

        currentLineIndex = line;
        int y = textArea.lineToY(line);

        if (line < 0 || line >= textArea.getLineCount()) {
            if (paintInvalid) {
                paintHighlight(graphics, line, y);
                styles[Token.INVALID].setGraphicsFlags(graphics, defaultFont);
                graphics.drawString("~", 0, y + fontMetrics.getHeight());
            }
        }
        else if (tokenMarker == null) {
            paintPlainLine(graphics, line, defaultFont, defaultColor, x, y);
        }
        else {
            paintSyntaxLine(graphics, tokenMarker, line, defaultFont, defaultColor, x, y);
        }
    }

    protected void paintPlainLine(Graphics graphics, int line, Font defaultFont, Color defaultColor, int x, int y) {
        paintHighlight(graphics, line, y);
        textArea.getLineText(line, currentLine);

        graphics.setFont(defaultFont);
        graphics.setColor(defaultColor);

        y += fontMetrics.getHeight();
        x = Math.round(Utilities.drawTabbedText(currentLine, (float) x, (float) y, (Graphics2D) graphics, this, 0));

        if (eolMarkers) {
            graphics.setColor(eolMarkerColor);
            graphics.drawString(".", x, y);
        }
    }

    protected void paintSyntaxLine(Graphics graphics, TokenMarker tokenMarker, int line, Font defaultFont, Color defaultColor, int x, int y) {
        textArea.getLineText(currentLineIndex, currentLine);
        currentLineTokens = tokenMarker.markTokens(currentLine, currentLineIndex);

        paintHighlight(graphics, line, y);

        graphics.setFont(defaultFont);
        graphics.setColor(defaultColor);
        y += fontMetrics.getHeight();
        x = SyntaxUtilities.paintSyntaxLine(currentLine, currentLineTokens, styles, this, graphics, x, y);
        if (eolMarkers) {
            graphics.setColor(eolMarkerColor);
            graphics.drawString(".", x, y);
        }
    }

    protected void paintHighlight(Graphics graphics, int line, int y) {
        if (line >= textArea.getSelectionStartLine() && line <= textArea.getSelectionEndLine()) {
            paintLineHighlight(graphics, line, y);
        }

        if (highlights != null) {
            highlights.paintHighlight(graphics, line, y);
        }

        if (bracketHighlight && line == textArea.getBracketLine()) {
            paintBracketHighlight(graphics, line, y);
        }

        if (line == textArea.getCaretLine()) {
            paintCaret(graphics, line, y);
        }
    }

    protected void paintLineHighlight(Graphics graphics, int line, int y) {
        int height = fontMetrics.getHeight();
        y += fontMetrics.getLeading() + fontMetrics.getMaxDescent();

        int selectionStart = textArea.getSelectionStart();
        int selectionEnd = textArea.getSelectionEnd();

        if (selectionStart == selectionEnd) {
            if (lineHighlight) {
                graphics.setColor(lineHighlightColor);
                graphics.fillRect(0, y, getWidth(), height);
            }
        }
        else {
            graphics.setColor(selectionBackground);

            int selectionStartLine = textArea.getSelectionStartLine();
            int selectionEndLine = textArea.getSelectionEndLine();
            int lineStart = textArea.getLineStartOffset(line);

            int x1;
            int x2;
            if (textArea.isSelectionRectangular()) {
                int lineLen = textArea.getLineLength(line);
                x1 = textArea._offsetToX(line, Math.min(lineLen, selectionStart - textArea.getLineStartOffset(selectionStartLine)));
                x2 = textArea._offsetToX(line, Math.min(lineLen, selectionEnd - textArea.getLineStartOffset(selectionEndLine)));
                if (x1 == x2) {
                    x2++;
                }
            }
            else if (selectionStartLine == selectionEndLine) {
                x1 = textArea._offsetToX(line, selectionStart - lineStart);
                x2 = textArea._offsetToX(line, selectionEnd - lineStart);
            }
            else if (line == selectionStartLine) {
                x1 = textArea._offsetToX(line, selectionStart - lineStart);
                x2 = getWidth();
            }
            else if (line == selectionEndLine) {
                x1 = 0;
                x2 = textArea._offsetToX(line, selectionEnd - lineStart);
            }
            else {
                x1 = 0;
                x2 = getWidth();
            }

            graphics.fillRect(Math.min(x1, x2), y, Math.abs(x1 - x2), height);
        }
    }

    protected void paintBracketHighlight(Graphics graphics, int line, int y) {
        int position = textArea.getBracketPosition();
        if (position < 0) {
            return;
        }
        y += fontMetrics.getLeading() + fontMetrics.getMaxDescent();
        int x = textArea._offsetToX(line, position);
        graphics.setColor(bracketHighlightColor);
        // Hack!!! Since there is no fast way to get the character
        // from the bracket matching routine, we use ( since all
        // brackets probably have the same width anyway
        graphics.drawRect(x, y, fontMetrics.charWidth('(') - 1, fontMetrics.getHeight() - 1);
    }

    protected void paintCaret(Graphics graphics, int line, int y) {
        if (textArea.isCaretVisible()) {
            int offset = textArea.getCaretPosition() - textArea.getLineStartOffset(line);
            int caretX = textArea._offsetToX(line, offset);
            int caretWidth = ((blockCaret || textArea.isOverwriteEnabled()) ? fontMetrics.charWidth('w') : 1);
            y += fontMetrics.getLeading() + fontMetrics.getMaxDescent();
            int height = fontMetrics.getHeight();

            graphics.setColor(caretForeground);

            if (textArea.isOverwriteEnabled()) {
                graphics.fillRect(caretX, y + height - 1, caretWidth, 1);
            }
            else {
                graphics.drawRect(caretX, y, caretWidth, height - 1);
            }
        }
    }

    @Override
    public void updateUI() {
        // TODO: handle null
        setBackground(UIManager.getColor("Venus.Editor.background"));
        setForeground(UIManager.getColor("Venus.Editor.foreground"));
        caretForeground = UIManager.getColor("Venus.Editor.caretForeground");
        selectionBackground = UIManager.getColor("Venus.Editor.selectionBackground");
        lineHighlightColor = UIManager.getColor("Venus.Editor.highlightedLine");
    }
}
