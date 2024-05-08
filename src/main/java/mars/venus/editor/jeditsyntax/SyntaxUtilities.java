/*
 * SyntaxUtilities.java - Utility functions used by syntax colorizing
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package mars.venus.editor.jeditsyntax;

import mars.settings.Settings;
import mars.venus.editor.jeditsyntax.tokenmarker.Token;

import javax.swing.*;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import java.awt.*;
import java.util.Objects;

/**
 * Class with several utility functions used by jEdit's syntax colorizing
 * subsystem.
 *
 * @author Slava Pestov
 * @version $Id: SyntaxUtilities.java,v 1.9 1999/12/13 03:40:30 sp Exp $
 */
public class SyntaxUtilities {
    // Prevent any instances from being created
    private SyntaxUtilities() {}

    /**
     * Checks if a subregion of a <code>Segment</code> is equal to a
     * string.
     *
     * @param ignoreCase True if case should be ignored, false otherwise
     * @param text       The segment
     * @param offset     The offset into the segment
     * @param match      The string to match
     */
    public static boolean regionMatches(boolean ignoreCase, Segment text, int offset, String match) {
        int length = offset + match.length();
        char[] textArray = text.array;
        if (length > text.offset + text.count) {
            return false;
        }
        for (int i = offset, j = 0; i < length; i++, j++) {
            char c1 = textArray[i];
            char c2 = match.charAt(j);
            if (ignoreCase) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
            }
            if (c1 != c2) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a subregion of a <code>Segment</code> is equal to a
     * character array.
     *
     * @param ignoreCase True if case should be ignored, false otherwise
     * @param text       The segment
     * @param offset     The offset into the segment
     * @param match      The character array to match
     */
    public static boolean regionMatches(boolean ignoreCase, Segment text, int offset, char[] match) {
        int length = offset + match.length;
        char[] textArray = text.array;
        if (length > text.offset + text.count) {
            return false;
        }
        for (int i = offset, j = 0; i < length; i++, j++) {
            char c1 = textArray[i];
            char c2 = match[j];
            if (ignoreCase) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
            }
            if (c1 != c2) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the default style table. This can be passed to the
     * <code>setStyles()</code> method of <code>SyntaxDocument</code>
     * to use the default syntax styles.
     */
    public static SyntaxStyle[] getDefaultSyntaxStyles() {
        SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

        // All need to be assigned even if not used by language (no gaps in array)
        styles[Token.NULL] = new SyntaxStyle(Objects.requireNonNullElse(UIManager.getColor("Venus.Editor.foreground"), Color.BLACK), false, false);
        styles[Token.COMMENT] = new SyntaxStyle(new Color(0x666666), false, false);
        styles[Token.COMMENT2] = new SyntaxStyle(new Color(0x666666), false, false);
        styles[Token.INSTRUCTION] = new SyntaxStyle(new Color(0xF27541), false, false);
        styles[Token.DIRECTIVE] = new SyntaxStyle(new Color(0x5A81FD), false, false);
        styles[Token.REGISTER] = new SyntaxStyle(new Color(0xE9AA4B), false, false);
        styles[Token.STRING_LITERAL] = new SyntaxStyle(new Color(0x3C9862), false, false);
        styles[Token.CHAR_LITERAL] = new SyntaxStyle(new Color(0x3C9862), false, false);
        styles[Token.LABEL] = new SyntaxStyle(new Color(0x2DB7AE), false, true);
        styles[Token.OPERATOR] = new SyntaxStyle(Color.black, false, true);
        styles[Token.INVALID] = new SyntaxStyle(new Color(0xFF3F3F), false, false);
        styles[Token.MACRO_ARGUMENT] = new SyntaxStyle(new Color(0xDE8ACA), false, false);

        return styles;
    }

    /**
     * Returns the CURRENT style table. This can be passed to the
     * <code>setStyles()</code> method of <code>SyntaxDocument</code>
     * to use the current syntax styles.  If changes have been made
     * via the Settings menu, the current settings will not be the
     * same as the default settings.
     */
    public static SyntaxStyle[] getCurrentSyntaxStyles(Settings settings) {
        SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

        styles[Token.NULL] = settings.getSyntaxStyle(Token.NULL);
        styles[Token.COMMENT] = settings.getSyntaxStyle(Token.COMMENT);
        styles[Token.COMMENT2] = settings.getSyntaxStyle(Token.COMMENT2);
        styles[Token.INSTRUCTION] = settings.getSyntaxStyle(Token.INSTRUCTION);
        styles[Token.DIRECTIVE] = settings.getSyntaxStyle(Token.DIRECTIVE);
        styles[Token.REGISTER] = settings.getSyntaxStyle(Token.REGISTER);
        styles[Token.STRING_LITERAL] = settings.getSyntaxStyle(Token.STRING_LITERAL);
        styles[Token.CHAR_LITERAL] = settings.getSyntaxStyle(Token.CHAR_LITERAL);
        styles[Token.LABEL] = settings.getSyntaxStyle(Token.LABEL);
        styles[Token.OPERATOR] = settings.getSyntaxStyle(Token.OPERATOR);
        styles[Token.INVALID] = settings.getSyntaxStyle(Token.INVALID);
        styles[Token.MACRO_ARGUMENT] = settings.getSyntaxStyle(Token.MACRO_ARGUMENT);

        return styles;
    }

    /**
     * Paints the specified line onto the graphics context. Note that this
     * method modifies the offset and count values of the segment.
     *
     * @param line     The line segment.
     * @param tokens   The token list for the line.
     * @param styles   The syntax style list.
     * @param expander The tab expander used to determine tab stops. May be null.
     * @param graphics The graphics context.
     * @param x        The x coordinate.
     * @param y        The y coordinate.
     * @return The x coordinate, plus the width of the painted string.
     */
    public static int paintSyntaxLine(Segment line, Token tokens, SyntaxStyle[] styles, TabExpander expander, Graphics graphics, int x, int y) {
        Font defaultFont = graphics.getFont();
        Color defaultColor = graphics.getColor();

        while (tokens.id != Token.END) {
            if (tokens.id == Token.NULL) {
                if (!defaultColor.equals(graphics.getColor())) {
                    graphics.setColor(defaultColor);
                }
                if (!defaultFont.equals(graphics.getFont())) {
                    graphics.setFont(defaultFont);
                }
            }
            else {
                styles[tokens.id].setGraphicsFlags(graphics, defaultFont);
            }
            line.count = tokens.length;

            x = Math.round(Utilities.drawTabbedText(line, (float) x, (float) y, (Graphics2D) graphics, expander, 0));
            line.offset += tokens.length;

            tokens = tokens.next;
        }

        return x;
    }
}
