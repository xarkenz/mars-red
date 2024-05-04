package mars.venus.editor.jeditsyntax;

import mars.settings.Settings;
import mars.venus.editor.FileEditorTab;
import mars.venus.editor.MARSTextEditingArea;
import mars.venus.editor.jeditsyntax.tokenmarker.MIPSTokenMarker;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import java.awt.*;

/**
 * Adaptor subclass for {@link JEditTextArea}.
 * <p>
 * Provides those methods required by the MARSTextEditingArea interface
 * that are not defined by JEditTextArea.  This permits JEditTextArea
 * to be used within MARS largely without modification.
 *
 * @author Pete Sanderson 4-20-2010
 * @since MARS 4.0
 */
public class JEditBasedTextArea extends JEditTextArea implements MARSTextEditingArea {
    private final FileEditorTab fileEditorTab;
    private final UndoManager undoManager;
    private boolean isCompoundEdit = false;
    private CompoundEdit compoundEdit;
    private final JEditBasedTextArea sourceCode;

    public JEditBasedTextArea(FileEditorTab fileEditorTab, Settings settings, JComponent lineNumbers) {
        super(settings, lineNumbers);
        this.fileEditorTab = fileEditorTab;
        this.undoManager = new UndoManager();
        this.compoundEdit = new CompoundEdit();
        this.sourceCode = this;

        // Needed to support unlimited undo/redo capability
        this.getDocument().addUndoableEditListener(event -> {
            // Remember the edit and update the menus.
            if (this.isCompoundEdit) {
                this.compoundEdit.addEdit(event.getEdit());
            }
            else {
                this.undoManager.addEdit(event.getEdit());
                this.fileEditorTab.updateUndoRedoActions();
            }
        });

        this.setFont(settings.getEditorFont());
        this.setTokenMarker(new MIPSTokenMarker());

        this.addCaretListener(event -> {
            // Display caret position on the edit pane
            this.fileEditorTab.displayCaretPosition(event.getDot());
        });
    }

    @Override
    public void setFont(Font font) {
        getPainter().setFont(font);
    }

    @Override
    public Font getFont() {
        return getPainter().getFont();
    }

    /**
     * Set whether highlighting of the line currently being edited is enabled.
     *
     * @param highlight true to enable line highlighting, false to disable.
     */
    @Override
    public void setLineHighlightEnabled(boolean highlight) {
        getPainter().setLineHighlightEnabled(highlight);
    }

    /**
     * Set the caret blinking rate in milliseconds.  If rate is 0 or less, blinking is disabled.
     *
     * @param rate Blinking rate in milliseconds.
     */
    @Override
    public void setCaretBlinkRate(int rate) {
        if (rate > 0) {
            caretBlinks = true;
            caretBlinkRate = rate;
            caretTimer.setDelay(rate);
            caretTimer.setInitialDelay(rate);
            caretTimer.restart();
        }
        else {
            caretBlinks = false;
        }
    }

    /**
     * Set the number of characters a tab will expand to.
     *
     * @param chars number of characters
     */
    @Override
    public void setTabSize(int chars) {
        painter.setTabSize(chars);
    }

    /**
     * Update the syntax style table, which is obtained from {@link SyntaxUtilities}.
     */
    @Override
    public void updateSyntaxStyles(Settings settings) {
        painter.setStyles(SyntaxUtilities.getCurrentSyntaxStyles(settings));
    }

    @Override
    public Component getOuterComponent() {
        return this;
    }

    /**
     * Get rid of any accumulated undoable edits.  It is useful to call
     * this method after opening a file into the text area.  The
     * act of setting its text content upon reading the file will generate
     * an undoable edit.  Normally you don't want a freshly-opened file
     * to appear with its Undo action enabled.  But it will unless you
     * call this after setting the text.
     */
    @Override
    public void discardAllUndoableEdits() {
        this.undoManager.discardAllEdits();
    }

    /**
     * Same as {@link #setSelectedText(String)} but named for compatibility with
     * JTextComponent method replaceSelection.
     * DPS, 14 Apr 2010
     *
     * @param replacementText The replacement text for the selection
     */
    @Override
    public void replaceSelection(String replacementText) {
        setSelectedText(replacementText);
    }

    @Override
    public void setSelectionVisible(boolean visible) {
    }

    @Override
    public void setSourceCode(String text, boolean editable) {
        this.setText(text);
        this.setEditable(editable);
        this.setEnabled(editable);
        this.setCaretPosition(0);
        if (editable) {
            this.requestFocusInWindow();
        }
    }

    /**
     * Returns the undo manager for this editing area.
     *
     * @return The undo manager.
     */
    @Override
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Undo previous edit.
     */
    @Override
    public void undo() {
        // "unredoing" is mode used by DocumentHandler's insertUpdate() and removeUpdate()
        // to pleasingly mark the text and location of the undo.
        unredoing = true;
        try {
            this.undoManager.undo();
        }
        catch (CannotUndoException ex) {
            System.err.println("Unable to undo: " + ex);
            ex.printStackTrace(System.err);
        }
        unredoing = false;
        this.setCaretVisible(true);
    }

    /**
     * Redo previous edit.
     */
    @Override
    public void redo() {
        // "unredoing" is mode used by DocumentHandler's insertUpdate() and removeUpdate()
        // to pleasingly mark the text and location of the redo.
        unredoing = true;
        try {
            this.undoManager.redo();
        }
        catch (CannotRedoException ex) {
            System.err.println("Unable to redo: " + ex);
            ex.printStackTrace(System.err);
        }
        unredoing = false;
        this.setCaretVisible(true);
    }

    /**
     * Comment or uncomment highlighted lines or line at cursor.
     */
    @Override
    public void commentLines() {
        int startLine, endLine;
        if (sourceCode.getSelectedText() == null) {
            startLine = endLine = sourceCode.getCaretLine();
        }
        else {
            startLine = sourceCode.getSelectionStartLine();
            endLine = sourceCode.getSelectionEndLine();
        }
        boolean areAllLinesCommented = true;
        for (int i = startLine; i <= endLine; i++) {
            String text = sourceCode.getLineText(i);
            if (!text.isBlank() && !text.trim().startsWith("#")) {
                areAllLinesCommented = false;
                break;
            }
        }
        if (areAllLinesCommented) {
            // Uncomment selection
            for (int i = startLine; i <= endLine; i++) {
                String commentedLine = sourceCode.getLineText(i);
                String uncommentedLine = commentedLine.replaceFirst("# ?", "");
                replaceLine(i, uncommentedLine);
            }
        }
        else {
            // Comment selection
            for (int i = startLine; i <= endLine; i++) {
                String uncommentedLine = sourceCode.getLineText(i);
                String commentedLine;
                if (uncommentedLine.isBlank())
                    commentedLine = uncommentedLine;
                else
                    commentedLine = uncommentedLine.replaceFirst("(\\S)", "# $1");
                replaceLine(i, commentedLine);
            }
        }
    }

    /**
     * Replace a line in the document with text.
     *
     * @param lineNumber line number to replace
     * @param text       text to replace the line with
     */
    public void replaceLine(int lineNumber, String text) {
        int startOffset = sourceCode.getLineStartOffset(lineNumber);
        int endOffset = sourceCode.getLineEndOffset(lineNumber);
        int lineLength = endOffset - startOffset - 1;
        try {
            sourceCode.getDocument().remove(startOffset, lineLength);
            sourceCode.getDocument().insertString(startOffset, text, null);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Methods to support Find / Replace feature
     *
     * Basis for this Find / Replace solution is:
     * http://java.ittoolbox.com/groups/technical-functional/java-l/search-and-replace-using-jtextpane-630964
     * as written by Chris Dickenson in 2005
     */

    /**
     * Finds next occurrence of text in a forward search of a string. Search begins
     * at the current cursor location, and wraps around when the end of the string
     * is reached.
     *
     * @param text          The text to locate in the string.
     * @param caseSensitive true if search is to be case-sensitive, false otherwise.
     * @return TEXT_FOUND or TEXT_NOT_FOUND, depending on the result.
     */
    public int doFindText(String text, boolean caseSensitive) {
        int foundIndex = nextIndex(sourceCode.getText(), text, sourceCode.getCaretPosition(), caseSensitive);
        if (foundIndex >= 0) {
            // Ensure the found text will appear highlighted once selected
            sourceCode.requestFocusInWindow();
            // Select the found text
            sourceCode.setSelectionStart(foundIndex);
            sourceCode.setSelectionEnd(foundIndex + text.length());
            // Need to repeat start due to quirk in JEditTextArea implementation of setSelectionStart
            sourceCode.setSelectionStart(foundIndex);
            return TEXT_FOUND;
        }
        else {
            return TEXT_NOT_FOUND;
        }
    }

    /**
     * Returns next index of word in text - forward search.  If end of string is
     * reached during the search, will wrap around to the beginning one time.
     *
     * @param input         the string to search
     * @param find          the string to find
     * @param start         the character index to start the search
     * @param caseSensitive true for case sensitive. false to ignore case
     * @return next indexed position of found text or -1 if not found
     */
    public int nextIndex(String input, String find, int start, boolean caseSensitive) {
        int index = -1;
        if (input != null && find != null && start < input.length()) {
            if (caseSensitive) { // indexOf() returns -1 if not found
                index = input.indexOf(find, start);
                // If not found from non-starting cursor position, wrap around
                if (start > 0 && index < 0) {
                    index = input.indexOf(find);
                }
            }
            else {
                String lowerCaseText = input.toLowerCase();
                index = lowerCaseText.indexOf(find.toLowerCase(), start);
                // If not found from non-starting cursor position, wrap around
                if (start > 0 && index < 0) {
                    index = lowerCaseText.indexOf(find.toLowerCase());
                }
            }
        }
        return index;
    }

    /**
     * Finds and replaces next occurrence of text in a string in a forward search.
     * If cursor is initially at end
     * of matching selection, will immediately replace then find and select the
     * next occurrence if any.  Otherwise it performs a find operation.  The replace
     * can be undone with one undo operation.
     *
     * @param text          the text to locate in the string
     * @param replace       the text to replace the find text with - if the find text exists
     * @param caseSensitive true for case sensitive. false to ignore case
     * @return Returns TEXT_FOUND if not initially at end of selected match and matching
     *     occurrence is found.  Returns TEXT_NOT_FOUND if the text is not matched.
     *     Returns TEXT_REPLACED_NOT_FOUND_NEXT if replacement is successful but there are
     *     no additional matches.  Returns TEXT_REPLACED_FOUND_NEXT if replacement is
     *     successful and there is at least one additional match.
     */
    public int doReplace(String text, String replace, boolean caseSensitive) {
        // Will perform a "find" and return, unless positioned at the end of
        // a selected "find" result.
        if (text == null || !text.equals(sourceCode.getSelectedText()) || sourceCode.getSelectionEnd() != sourceCode.getCaretPosition()) {
            return doFindText(text, caseSensitive);
        }
        // We are positioned at end of selected "find".  Replace and find next.
        int foundIndex = sourceCode.getSelectionStart();
        // Ensure the found text will appear highlighted once selected
        sourceCode.requestFocusInWindow();
        // Select the found text
        sourceCode.setSelectionStart(foundIndex);
        sourceCode.setSelectionEnd(foundIndex + text.length());
        // Need to repeat start due to quirk in JEditTextArea implementation of setSelectionStart
        sourceCode.setSelectionStart(foundIndex);

        isCompoundEdit = true;
        compoundEdit = new CompoundEdit();
        sourceCode.replaceSelection(replace);
        compoundEdit.end();
        undoManager.addEdit(compoundEdit);
        fileEditorTab.updateUndoRedoActions();
        isCompoundEdit = false;
        sourceCode.setCaretPosition(foundIndex + replace.length());

        if (doFindText(text, caseSensitive) == TEXT_NOT_FOUND) {
            return TEXT_REPLACED_NOT_FOUND_NEXT;
        }
        else {
            return TEXT_REPLACED_FOUND_NEXT;
        }
    }

    /**
     * Finds and replaces <b>ALL</b> occurrences of text in a string in a forward search.
     * All replacements are bundled into one CompoundEdit, so one Undo operation will
     * undo all of them.
     *
     * @param find          the text to locate in the string
     * @param replace       the text to replace the find text with - if the find text exists
     * @param caseSensitive true for case sensitive. false to ignore case
     * @return the number of occurrences that were matched and replaced.
     */
    public int doReplaceAll(String find, String replace, boolean caseSensitive) {
        int nextIndex = 0;
        int findIndex = 0; // Begin at start of text
        int replaceCount = 0;
        compoundEdit = null; // new one will be created upon first replacement
        isCompoundEdit = true; // undo manager's action listener needs this
        while (nextIndex >= 0) {
            nextIndex = nextIndex(sourceCode.getText(), find, findIndex, caseSensitive);
            if (nextIndex >= 0) {
                // nextIndex() will wrap around, which causes infinite loop if
                // find string is a substring of replacement string.  This
                // statement will prevent that.
                if (nextIndex < findIndex) {
                    break;
                }
                sourceCode.grabFocus();
                sourceCode.setSelectionStart(nextIndex); // Position cursor at word start
                sourceCode.setSelectionEnd(nextIndex + find.length()); //select found text
                // Need to repeat start due to quirk in JEditTextArea implementation of setSelectionStart.
                sourceCode.setSelectionStart(nextIndex);
                if (compoundEdit == null) {
                    compoundEdit = new CompoundEdit();
                }
                sourceCode.replaceSelection(replace);
                findIndex = nextIndex + replace.length(); // set for next search
                replaceCount++;
            }
        }
        isCompoundEdit = false;
        // Will be true if any replacements were performed
        if (compoundEdit != null) {
            compoundEdit.end();
            undoManager.addEdit(compoundEdit);
            fileEditorTab.updateUndoRedoActions();
        }
        return replaceCount;
    }
}