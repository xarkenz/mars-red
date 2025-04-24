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
    private final Settings settings;
    private final UndoManager undoManager;
    private boolean isCompoundEdit;
    private CompoundEdit compoundEdit;

    public JEditBasedTextArea(FileEditorTab fileEditorTab, Settings settings, JComponent lineNumbers) {
        super(settings, lineNumbers);
        this.fileEditorTab = fileEditorTab;
        this.settings = settings;
        this.undoManager = new UndoManager();
        this.isCompoundEdit = false;
        this.compoundEdit = new CompoundEdit();

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

        this.setFont(this.settings.editorFont.get());
        this.setTokenMarker(new MIPSTokenMarker());

        // Enable smart Home/End key functionality
        this.putClientProperty(InputHandler.SMART_HOME_END_PROPERTY, true);

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
            this.caretBlinks = true;
            this.caretBlinkRate = rate;
            caretTimer.setDelay(rate);
            caretTimer.setInitialDelay(rate);
            caretTimer.restart();
        }
        else {
            this.caretBlinks = false;
        }
    }

    /**
     * Set the number of characters a tab will expand to.
     *
     * @param chars number of characters
     */
    @Override
    public void setTabSize(int chars) {
        this.painter.setTabSize(chars);
    }

    /**
     * Update the syntax style table, which is obtained from {@link SyntaxUtilities}.
     */
    @Override
    public void updateSyntaxStyles() {
        this.painter.setStyles(SyntaxUtilities.getCurrentSyntaxStyles(this.settings));
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
        this.setSelectedText(replacementText);
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
        return this.undoManager;
    }

    /**
     * Undo previous edit.
     */
    @Override
    public void undo() {
        // "unredoing" is mode used by DocumentHandler's insertUpdate() and removeUpdate()
        // to pleasingly mark the text and location of the undo.
        this.unredoing = true;
        try {
            this.undoManager.undo();
        }
        catch (CannotUndoException exception) {
            System.err.println("Unable to undo: " + exception);
            exception.printStackTrace(System.err);
        }
        this.unredoing = false;
        this.setCaretVisible(true);
    }

    /**
     * Redo previous edit.
     */
    @Override
    public void redo() {
        // "unredoing" is mode used by DocumentHandler's insertUpdate() and removeUpdate()
        // to pleasingly mark the text and location of the redo.
        this.unredoing = true;
        try {
            this.undoManager.redo();
        }
        catch (CannotRedoException exception) {
            System.err.println("Unable to redo: " + exception);
            exception.printStackTrace(System.err);
        }
        this.unredoing = false;
        this.setCaretVisible(true);
    }

    /**
     * Comment or uncomment highlighted lines or line at cursor.
     */
    @Override
    public void commentLines() {
        int startLine, endLine, caretLine;
        boolean caretAtSelectionStart = this.biasLeft;
        boolean hasSelection = this.getSelectedText() != null;
        int startSelectionOffset, endSelectionOffset, caretPositionOffset;
        if (hasSelection) {
            startLine = this.getSelectionStartLine();
            endLine = this.getSelectionEndLine();
            caretLine = this.getCaretLine();
            startSelectionOffset = this.getSelectionStart() - this.getLineStartOffset(startLine);
            endSelectionOffset = this.getSelectionEnd() - this.getLineStartOffset(endLine);
            caretPositionOffset = this.getCaretPosition() - this.getLineStartOffset(caretLine);
        }
        else {
            startLine = endLine = caretLine = this.getCaretLine();
            startSelectionOffset = endSelectionOffset = caretPositionOffset = this.getCaretPosition() - this.getLineStartOffset(startLine);
        }

        // Decide whether to comment or uncomment
        boolean areAllLinesCommented = true;
        for (int i = startLine; i <= endLine; i++) {
            String text = this.getLineText(i);
            if (!text.isBlank() && !text.strip().startsWith("#")) {
                areAllLinesCommented = false;
                break;
            }
        }

        // Perform the action
        int startLineTextDiff = 0, endLineTextDiff = 0, caretLineTextDiff = 0;
        if (areAllLinesCommented) {
            // Uncomment selection
            for (int i = startLine; i <= endLine; i++) {
                String commentedLine = this.getLineText(i);
                String uncommentedLine = commentedLine.replaceFirst("# ?", "");
                this.replaceLine(i, uncommentedLine);
                
                int textDiff = uncommentedLine.length() - commentedLine.length();
                if (i == startLine) {
                    startLineTextDiff = textDiff;
                }
                if (i == endLine) {
                    endLineTextDiff = textDiff;
                }
                if (i == caretLine) {
                    caretLineTextDiff = textDiff;
                }
            }
        }
        else {
            // Comment selection
            for (int i = startLine; i <= endLine; i++) {
                String uncommentedLine = this.getLineText(i);
                String commentedLine;
                if (uncommentedLine.isBlank()) {
                    commentedLine = uncommentedLine;
                }
                else {
                    commentedLine = uncommentedLine.replaceFirst("(\\S)", "# $1");
                }
                this.replaceLine(i, commentedLine);

                int textDiff = commentedLine.length() - uncommentedLine.length();
                if (i == startLine) {
                    startLineTextDiff = textDiff;
                }
                if (i == endLine) {
                    endLineTextDiff = textDiff;
                }
                if (i == caretLine) {
                    caretLineTextDiff = textDiff;
                }
            }
        }

        // Replace selection or caret position
        int finalStartPos = this.getLineStartOffset(startLine) + startSelectionOffset + startLineTextDiff;
        int finalEndPos = this.getLineStartOffset(endLine) + endSelectionOffset + endLineTextDiff;
        int finalCaretPos = this.getLineStartOffset(caretLine) + caretPositionOffset + caretLineTextDiff;
        if (hasSelection) {
            if (caretAtSelectionStart) {
                this.select(finalEndPos, finalStartPos);
            }
            else {
                this.select(finalStartPos, finalEndPos);
            }
        }
        else {
            this.setCaretPosition(finalCaretPos);
        }
    }

    /**
     * Replace a line in the document with text.
     *
     * @param lineNumber line number to replace
     * @param text       text to replace the line with
     */
    public void replaceLine(int lineNumber, String text) {
        int startOffset = this.getLineStartOffset(lineNumber);
        int endOffset = this.getLineEndOffset(lineNumber);
        int lineLength = endOffset - startOffset - 1;
        try {
            this.getDocument().remove(startOffset, lineLength);
            this.getDocument().insertString(startOffset, text, null);
        }
        catch (BadLocationException exception) {
            throw new RuntimeException(exception);
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
        int foundIndex = this.nextIndex(this.getText(), text, this.getCaretPosition(), caseSensitive);
        if (foundIndex >= 0) {
            // Ensure the found text will appear highlighted once selected
            this.requestFocusInWindow();
            // Select the found text
            this.setSelectionStart(foundIndex);
            this.setSelectionEnd(foundIndex + text.length());
            // Need to repeat start due to quirk in JEditTextArea implementation of setSelectionStart
            this.setSelectionStart(foundIndex);
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
        if (text == null || !text.equals(this.getSelectedText()) || this.getSelectionEnd() != this.getCaretPosition()) {
            return this.doFindText(text, caseSensitive);
        }
        // We are positioned at end of selected "find".  Replace and find next.
        int foundIndex = this.getSelectionStart();
        // Ensure the found text will appear highlighted once selected
        this.requestFocusInWindow();
        // Select the found text
        this.setSelectionStart(foundIndex);
        this.setSelectionEnd(foundIndex + text.length());
        // Need to repeat start due to quirk in JEditTextArea implementation of setSelectionStart
        this.setSelectionStart(foundIndex);

        this.isCompoundEdit = true;
        this.compoundEdit = new CompoundEdit();
        this.replaceSelection(replace);
        this.compoundEdit.end();
        this.undoManager.addEdit(this.compoundEdit);
        this.fileEditorTab.updateUndoRedoActions();
        this.isCompoundEdit = false;
        this.setCaretPosition(foundIndex + replace.length());

        if (this.doFindText(text, caseSensitive) == TEXT_NOT_FOUND) {
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
        this.compoundEdit = null; // new one will be created upon first replacement
        this.isCompoundEdit = true; // undo manager's action listener needs this
        while (nextIndex >= 0) {
            nextIndex = this.nextIndex(this.getText(), find, findIndex, caseSensitive);
            if (nextIndex >= 0) {
                // nextIndex() will wrap around, which causes infinite loop if
                // find string is a substring of replacement string.  This
                // statement will prevent that.
                if (nextIndex < findIndex) {
                    break;
                }
                this.grabFocus();
                this.setSelectionStart(nextIndex); // Position cursor at word start
                this.setSelectionEnd(nextIndex + find.length()); //select found text
                // Need to repeat start due to quirk in JEditTextArea implementation of setSelectionStart.
                this.setSelectionStart(nextIndex);
                if (this.compoundEdit == null) {
                    this.compoundEdit = new CompoundEdit();
                }
                this.replaceSelection(replace);
                findIndex = nextIndex + replace.length(); // set for next search
                replaceCount++;
            }
        }
        this.isCompoundEdit = false;
        // Will be true if any replacements were performed
        if (this.compoundEdit != null) {
            this.compoundEdit.end();
            this.undoManager.addEdit(this.compoundEdit);
            this.fileEditorTab.updateUndoRedoActions();
        }
        return replaceCount;
    }

    /**
     * Ensure that syntax styles are updated properly when the application theme is changed.
     */
    @Override
    public void updateUI() {
        super.updateUI();

        this.updateSyntaxStyles();
    }
}