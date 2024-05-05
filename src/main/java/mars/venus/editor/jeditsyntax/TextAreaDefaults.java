/*
 * TextAreaDefaults.java - Encapsulates default values for various settings
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package mars.venus.editor.jeditsyntax;

import mars.settings.Settings;

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
    public Color caretForeground;
    public Color selectionBackground;
    public boolean lineHighlightVisible;
    public Color lineHighlightColor;
    public boolean bracketHighlightVisible;
    public Color bracketHighlightColor;
    public boolean eolMarkerVisible;
    public Color eolMarkerColor;
    public boolean paintInvalid;

    public JPopupMenu popup;

    /**
     * Constructs a new TextAreaDefaults object with the default values filled in.
     */
    public TextAreaDefaults(Settings settings) {
        inputHandler = new DefaultInputHandler();
        inputHandler.addDefaultKeyBindings();
        editable = true;

        blockCaret = false;
        caretVisible = true;
        caretBlinkRate = settings.caretBlinkRate.get();
        caretBlinks = caretBlinkRate > 0;
        tabSize = settings.editorTabSize.get();
        electricScroll = 0; // Was 3.  Will begin scrolling when cursor is this many lines from the edge.

        cols = 80;
        rows = 25;
        styles = SyntaxUtilities.getCurrentSyntaxStyles(settings);
        backgroundColor = UIManager.getColor("Venus.Editor.background");
        foregroundColor = UIManager.getColor("Venus.Editor.foreground");
        caretForeground = UIManager.getColor("Venus.Editor.caretForeground");
        selectionBackground = UIManager.getColor("Venus.Editor.selectionBackground");
        lineHighlightVisible = settings.highlightCurrentEditorLine.get();
        lineHighlightColor = UIManager.getColor("Venus.Editor.highlightedLine");
        bracketHighlightVisible = false; // assembly language doesn't need this.
        bracketHighlightColor = caretForeground;
        eolMarkerVisible = false;
        eolMarkerColor = new Color(0x009999);
        paintInvalid = false;
        document = new SyntaxDocument();
    }
}
