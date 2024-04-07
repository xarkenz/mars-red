package mars.venus.editors.generic;

import mars.Application;
import mars.venus.EditPane;
import mars.venus.editors.MARSTextEditingArea;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import java.awt.*;

/*
Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

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

public class GenericTextArea extends JTextArea implements MARSTextEditingArea {
    private final EditPane editPane;
    private final UndoManager undoManager;
    private final UndoableEditListener undoableEditListener;
    private final JTextArea sourceCode;
    private final JScrollPane editAreaScrollPane;

    private boolean isCompoundEdit = false;
    private CompoundEdit compoundEdit;

    public GenericTextArea(EditPane editPane, JComponent lineNumbers) {
        this.editPane = editPane;
        this.sourceCode = this;
        this.setFont(Application.getSettings().getEditorFont());
        this.setTabSize(Application.getSettings().editorTabSize.get());
        this.setMargin(new Insets(0, 3, 3, 3));
        this.setCaretBlinkRate(Application.getSettings().caretBlinkRate.get());

        JPanel source = new JPanel(new BorderLayout());
        source.add(lineNumbers, BorderLayout.WEST);
        source.add(this, BorderLayout.CENTER);
        editAreaScrollPane = new JScrollPane(source, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        editAreaScrollPane.getVerticalScrollBar().setUnitIncrement(sourceCode.getFontMetrics(this.sourceCode.getFont()).getHeight());

        this.undoManager = new UndoManager();

        this.getCaret().addChangeListener(event -> this.editPane.displayCaretPosition(getCaretPosition()));

        // Needed to support unlimited undo/redo capability
        undoableEditListener = event -> {
            // Remember the edit and update the menus.
            if (isCompoundEdit) {
                compoundEdit.addEdit(event.getEdit());
            }
            else {
                undoManager.addEdit(event.getEdit());
                this.editPane.updateUndoRedoActions();
            }
        };
        this.getDocument().addUndoableEditListener(undoableEditListener);
    }

    /**
     * Does nothing, but required by the interface.  This editor does not support
     * highlighting of the line currently being edited.
     */
    @Override
    public void setLineHighlightEnabled(boolean highlight) {
    }

    /**
     * Does nothing, but required by the interface.  This editor does not support
     * syntax styling (colors, bold/italic).
     */
    @Override
    public void updateSyntaxStyles() {
    }

    /**
     * Set the caret blinking rate in milliseconds.  If rate is 0
     * it will not blink.  If negative, do nothing.
     *
     * @param rate blinking rate in milliseconds
     */
    @Override
    public void setCaretBlinkRate(int rate) {
        if (rate >= 0) {
            getCaret().setBlinkRate(rate);
        }
    }

    @Override
    public Component getOuterComponent() {
        return editAreaScrollPane;
    }

    /**
     * For initalizing the source code when opening an ASM file
     *
     * @param text     String containing text
     * @param editable Set true if code is editable else false
     */
    @Override
    public void setSourceCode(String text, boolean editable) {
        this.setText(text);
        this.setBackground((editable) ? Color.WHITE : Color.GRAY);
        this.setEditable(editable);
        this.setEnabled(editable);
        this.getCaret().setVisible(editable);
        this.setCaretPosition(0);
        if (editable) {
            this.requestFocusInWindow();
        }
    }

    /**
     * Tell UndoManager to discard all its collected undoable edits.
     */
    @Override
    public void discardAllUndoableEdits() {
        undoManager.discardAllEdits();
    }

    /**
     * Override inherited setText to temporarily remove UndoableEditListener because this
     * operation is not undoable.
     *
     * @param text String with new contents for the editing area. Replaces current content.
     */
    @Override
    public void setText(String text) {
        this.getDocument().removeUndoableEditListener(undoableEditListener);
        super.setText(text);
        this.getDocument().addUndoableEditListener(undoableEditListener);
    }

    /**
     * Control caret visibility
     *
     * @param visible true to display caret, false to hide it
     */
    @Override
    public void setCaretVisible(boolean visible) {
        this.getCaret().setVisible(visible);
    }

    /**
     * Control selection visibility
     *
     * @param visible true to display selection, false to hide it
     */
    @Override
    public void setSelectionVisible(boolean visible) {
        this.getCaret().setSelectionVisible(visible);
    }

    /**
     * Returns the undo manager for this editing area
     *
     * @return the undo manager
     */
    @Override
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Undo previous edit
     */
    @Override
    public void undo() {
        try {
            this.undoManager.undo();
        }
        catch (CannotUndoException ex) {
            System.out.println("Unable to undo: " + ex);
            ex.printStackTrace();
        }
        this.setCaretVisible(true);
    }

    /**
     * Redo previous edit
     */
    @Override
    public void redo() {
        try {
            this.undoManager.redo();
        }
        catch (CannotRedoException ex) {
            System.out.println("Unable to redo: " + ex);
            ex.printStackTrace();
        }
        this.setCaretVisible(true);
    }

    // Methods to support Find/Replace feature
    //
    // Basis for this Find/Replace solution is:
    // http://java.ittoolbox.com/groups/technical-functional/java-l/search-and-replace-using-jtextpane-630964
    // as written by Chris Dickenson in 2005
    //
    // sourceCode is implemented as JTextArea rather than JTextPane but the necessary methods are inherited
    // by both from JTextComponent.

    /**
     * Finds next occurrence of text in a forward search of a string. Search begins
     * at the current cursor location, and wraps around when the end of the string
     * is reached.
     *
     * @param find          the text to locate in the string
     * @param caseSensitive true if search is to be case-sensitive, false otherwise
     * @return TEXT_FOUND or TEXT_NOT_FOUND, depending on the result.
     */
    @Override
    public int doFindText(String find, boolean caseSensitive) {
        int findIndex = sourceCode.getCaretPosition();
        int nextIndex = this.nextIndex(sourceCode.getText(), find, findIndex, caseSensitive);
        if (nextIndex >= 0) {
            sourceCode.requestFocus(); // guarantees visibility of the blue highlight 
            sourceCode.setSelectionStart(nextIndex); // position cursor at word start
            sourceCode.setSelectionEnd(nextIndex + find.length());
            sourceCode.setSelectionStart(nextIndex); // position cursor at word start
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
     * @param start         the character index to start the search at
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
     * @param find          the text to locate in the string
     * @param replace       the text to replace the find text with - if the find text exists
     * @param caseSensitive true for case sensitive. false to ignore case
     * @return Returns TEXT_FOUND if not initially at end of selected match and matching
     *     occurrence is found.  Returns TEXT_NOT_FOUND if the text is not matched.
     *     Returns TEXT_REPLACED_NOT_FOUND_NEXT if replacement is successful but there are
     *     no additional matches.  Returns TEXT_REPLACED_FOUND_NEXT if replacement is
     *     successful and there is at least one additional match.
     */
    @Override
    public int doReplace(String find, String replace, boolean caseSensitive) {
        // Will perform a "find" and return, unless positioned at the end of
        // a selected "find" result.
        if (find == null || !find.equals(sourceCode.getSelectedText()) || sourceCode.getSelectionEnd() != sourceCode.getCaretPosition()) {
            return doFindText(find, caseSensitive);
        }
        // We are positioned at end of selected "find".  Replace and find next.
        int nextPosition = sourceCode.getSelectionStart();
        sourceCode.grabFocus();
        sourceCode.setSelectionStart(nextPosition); // Position cursor at word start
        sourceCode.setSelectionEnd(nextPosition + find.length()); // Select found text
        isCompoundEdit = true;
        compoundEdit = new CompoundEdit();
        sourceCode.replaceSelection(replace);
        compoundEdit.end();
        undoManager.addEdit(compoundEdit);
        editPane.updateUndoRedoActions();
        isCompoundEdit = false;
        sourceCode.setCaretPosition(nextPosition + replace.length());
        if (this.doFindText(find, caseSensitive) == TEXT_NOT_FOUND) {
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
    @Override
    public int doReplaceAll(String find, String replace, boolean caseSensitive) {
        int nextIndex = 0;
        int findIndex = 0; // Begin at start of text
        int replaceCount = 0;
        compoundEdit = null; // new one will be created upon first replacement
        isCompoundEdit = true; // undo manager's action listener needs this
        while (nextIndex >= 0) {
            nextIndex = this.nextIndex(sourceCode.getText(), find, findIndex, caseSensitive);
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
            editPane.updateUndoRedoActions();
        }
        return replaceCount;
    }
}