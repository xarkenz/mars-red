/*
 * TextAreaDefaults.java - Encapsulates default values for various settings
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package mars.venus.editors.jeditsyntax;

import mars.Globals;

import javax.swing.*;
import java.awt.*;

/**
 * Encapsulates default settings for a text area. This can be passed
 * to the constructor once the necessary fields have been filled out.
 * The advantage of doing this over calling lots of set() methods after
 * creating the text area is that this method is faster.
 */
public class TextAreaDefaults {
    public InputHandler inputHandler;
    public SyntaxDocument document;
    public boolean editable;

    public boolean caretVisible;
    public boolean caretBlinks;
    public boolean blockCaret;
    public int caretBlinkRate;
    public int electricScroll;
    public int tabSize;

    public int cols;
    public int rows;
    public SyntaxStyle[] styles;
    public Color backgroundColor;
    public Color foregroundColor;
    public Color caretColor;
    public Color selectionColor;
    public Color lineHighlightColor;
    public boolean lineHighlightVisible;
    public Color bracketHighlightColor;
    public boolean bracketHighlightVisible;
    public Color eolMarkerColor;
    public boolean eolMarkerVisible;
    public boolean paintInvalid;

    public JPopupMenu popup;

    /**
     * Constructs a new TextAreaDefaults object with the default values filled in.
     */
    public TextAreaDefaults() {
        inputHandler = new DefaultInputHandler();
        inputHandler.addDefaultKeyBindings();
        editable = true;

        blockCaret = false;
        caretVisible = true;
        caretBlinks = (Globals.getSettings().caretBlinkRate.get() != 0);
        caretBlinkRate = Globals.getSettings().caretBlinkRate.get();
        tabSize = Globals.getSettings().editorTabSize.get();
        electricScroll = 0; // was 3.  Will begin scrolling when cursor is this many lines from the edge.

        cols = 80;
        rows = 25;
        styles = SyntaxUtilities.getCurrentSyntaxStyles();
        backgroundColor = UIManager.getColor("Venus.Editor.background");
        foregroundColor = UIManager.getColor("Venus.Editor.foreground");
        caretColor = UIManager.getColor("Venus.Editor.caret");
        selectionColor = UIManager.getColor("Venus.Editor.selectionBackground");
        lineHighlightColor = UIManager.getColor("Venus.Editor.highlightedLine");
        lineHighlightVisible = Globals.getSettings().highlightCurrentEditorLine.get();
        bracketHighlightColor = caretColor;
        bracketHighlightVisible = false; // assembly language doesn't need this.
        eolMarkerColor = new Color(0x009999);
        eolMarkerVisible = false;
        paintInvalid = false;
        document = new SyntaxDocument();
    }
}
