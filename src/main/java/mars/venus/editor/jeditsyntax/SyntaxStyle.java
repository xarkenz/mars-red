/*
 * SyntaxStyle.java - A simple text style class
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package mars.venus.editor.jeditsyntax;

import java.awt.*;
import java.util.Objects;

/**
 * A simple text style class. It can specify the color, italic flag,
 * and bold flag of a run of text.
 *
 * @author Slava Pestov
 * @version $Id: SyntaxStyle.java,v 1.6 1999/12/13 03:40:30 sp Exp $
 */
public class SyntaxStyle {
    private final Color foreground;
    private final boolean italic;
    private final boolean bold;
    private Font lastFont;
    private Font lastStyledFont;
    private FontMetrics fontMetrics;

    /**
     * Creates a new SyntaxStyle.
     *
     * @param foreground The text color
     * @param italic     True if the text should be italics
     * @param bold       True if the text should be bold
     */
    public SyntaxStyle(Color foreground, boolean italic, boolean bold) {
        this.foreground = foreground;
        this.italic = italic;
        this.bold = bold;
    }

    /**
     * Returns the color specified in this style.
     */
    public Color getForeground() {
        return foreground;
    }

    /**
     * Returns true if no font styles are enabled.
     */
    public boolean isPlain() {
        return !(bold || italic);
    }

    /**
     * Returns true if italics is enabled for this style.
     */
    public boolean isItalic() {
        return italic;
    }

    /**
     * Returns true if boldface is enabled for this style.
     */
    public boolean isBold() {
        return bold;
    }

    /**
     * Returns the specified font, but with the style's bold and
     * italic flags applied.
     */
    public Font getStyledFont(Font font) {
        Objects.requireNonNull(font);
        if (font.equals(lastFont)) {
            return lastStyledFont;
        }
        lastFont = font;
        lastStyledFont = new Font(font.getFamily(), (bold ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN), font.getSize());
        return lastStyledFont;
    }

    /**
     * Returns the font metrics for the styled font.
     */
    public FontMetrics getFontMetrics(Font font) {
        Objects.requireNonNull(font);
        if (font.equals(lastFont) && fontMetrics != null) {
            return fontMetrics;
        }
        lastFont = font;
        lastStyledFont = new Font(font.getFamily(), (bold ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN), font.getSize());
        fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(lastStyledFont);
        return fontMetrics;
    }

    /**
     * Sets the foreground color and font of the specified graphics
     * context to that specified in this style.
     *
     * @param gfx  The graphics context
     * @param font The font to add the styles to
     */
    public void setGraphicsFlags(Graphics gfx, Font font) {
        gfx.setFont(getStyledFont(font));
        gfx.setColor(foreground);
    }

    /**
     * Returns a string representation of this object.
     */
    public String toString() {
        return getClass().getName() + "[color=" + foreground + (italic ? ",italic" : "") + (bold ? ",bold" : "") + "]";
    }
}
