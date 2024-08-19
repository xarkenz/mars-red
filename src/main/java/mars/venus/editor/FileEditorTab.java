package mars.venus.editor;

import mars.Application;
import mars.venus.VenusUI;
import mars.venus.editor.jeditsyntax.JEditBasedTextArea;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

/*
Copyright (c) 2003-2011,  Pete Sanderson and Kenneth Vollmar

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
 * Represents one file opened for editing.  Maintains required internal structures.
 * Before Mars 4.0, there was only one editor pane, a tab, and only one file could
 * be open at a time.  With 4.0 came the multi-file editor, and existing duties were
 * split between EditPane (now FileEditorTab) and the new EditTabbedPane (now EditTab) class.
 *
 * @author Sanderson and Bumgarner
 */
public class FileEditorTab extends JPanel {
    private final VenusUI gui;
    private final MARSTextEditingArea textEditingArea;
    private final JLabel caretPositionLabel;
    private final JLabel lineNumbers;

    private FileStatus fileStatus;
    private File file;

    /**
     * Create a new tab within the "Edit" tab for editing a file.
     */
    public FileEditorTab(VenusUI gui) {
        super(new BorderLayout());
        this.gui = gui;
        this.lineNumbers = new JLabel();
        this.fileStatus = FileStatus.NO_FILE;
        this.file = null;

        this.textEditingArea = new JEditBasedTextArea(this, this.gui.getSettings(), this.lineNumbers);
        // Text editing area is responsible for its own scrolling
        this.add(this.textEditingArea.getOuterComponent(), BorderLayout.CENTER);

        this.textEditingArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                handleEditEvent();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                handleEditEvent();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                handleEditEvent();
            }
        });

        this.setSourceCode("", false);

        this.lineNumbers.setFont(this.textEditingArea.getFont().deriveFont(Font.PLAIN));
        this.lineNumbers.setForeground(UIManager.getColor("Venus.Editor.lineNumbers.foreground"));
        this.lineNumbers.setBackground(UIManager.getColor("Venus.Editor.lineNumbers.background"));
        this.lineNumbers.setVerticalAlignment(JLabel.TOP);
        this.lineNumbers.setText(null);
        this.lineNumbers.setVisible(this.gui.getSettings().displayEditorLineNumbers.get());

        Box statusBar = Box.createHorizontalBox();
        this.caretPositionLabel = new JLabel();
        this.caretPositionLabel.setToolTipText("Tracks the current position of the text editing cursor.");
        displayCaretPosition(new Point(1, 1));
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(this.caretPositionLabel);
        statusBar.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        this.add(statusBar, BorderLayout.SOUTH);

        this.gui.getSettings().addListener(() -> {
            this.textEditingArea.setFont(this.gui.getSettings().editorFont.get());
            this.textEditingArea.setLineHighlightEnabled(this.gui.getSettings().highlightCurrentEditorLine.get());
            this.textEditingArea.setCaretBlinkRate(this.gui.getSettings().caretBlinkRate.get());
            this.textEditingArea.setTabSize(this.gui.getSettings().editorTabSize.get());
            this.textEditingArea.updateSyntaxStyles();
            this.textEditingArea.revalidate();
            // We want line numbers to be displayed same size but always PLAIN style.
            // Easiest way to get same pixel height as source code is to set to same
            // font family as the source code! It can get a bit complicated otherwise
            // because different fonts will render the same font size in different
            // pixel heights.  This is a factor because the line numbers as displayed
            // in the editor form a separate column from the source code and if the
            // pixel height is not the same then the numbers will not line up with
            // the source lines.
            this.lineNumbers.setFont(this.gui.getSettings().editorFont.get().deriveFont(Font.PLAIN));
            if (this.gui.getSettings().displayEditorLineNumbers.get()) {
                this.lineNumbers.setText(getLineNumbersList(this.textEditingArea.getDocument()));
                this.lineNumbers.setVisible(true);
            }
            else {
                this.lineNumbers.setText(null);
                this.lineNumbers.setVisible(false);
            }
            this.lineNumbers.revalidate();
        });
    }

    /**
     * If source code is modified, will set flag to trigger/request file save.
     */
    private void handleEditEvent() {
        // IF statement added DPS 9-Aug-2011
        // This method is triggered when file contents added to document
        // upon opening, even though not edited by user.  The IF
        // statement will sense this situation and immediately return.
        if (!this.gui.isWorkspaceStateSavingEnabled()) {
            this.setFileStatus(FileStatus.NOT_EDITED);
        }
        else {
            // End of 9-Aug-2011 modification.
            if (this.getFileStatus() == FileStatus.NEW_NOT_EDITED) {
                this.setFileStatus(FileStatus.NEW_EDITED);
            }
            else if (this.getFileStatus() == FileStatus.NOT_EDITED) {
                this.setFileStatus(FileStatus.EDITED);
            }
            this.gui.getMainPane().getEditTab().updateTitleAndMenuState(this);
        }

        if (Application.getSettings().displayEditorLineNumbers.get()) {
            this.lineNumbers.setText(getLineNumbersList(this.textEditingArea.getDocument()));
        }
    }

    /**
     * For initializing the source code when opening an ASM file.
     *
     * @param sourceCode String containing text
     * @param editable   Set true if code is editable else false
     */
    public void setSourceCode(String sourceCode, boolean editable) {
        this.textEditingArea.setSourceCode(sourceCode, editable);

        if (Application.getSettings().displayEditorLineNumbers.get()) {
            this.lineNumbers.setText(getLineNumbersList(this.textEditingArea.getDocument()));
        }
    }

    /**
     * Get rid of any accumulated undoable edits.  It is useful to call
     * this method after opening a file into the text area.  The
     * act of setting its text content upon reading the file will generate
     * an undoable edit.  Normally you don't want a freshly-opened file
     * to appear with its Undo action enabled.  But it will unless you
     * call this after setting the text.
     */
    public void discardAllUndoableEdits() {
        textEditingArea.discardAllUndoableEdits();
    }

    /**
     * Form string with source code line numbers.
     * Resulting string is HTML, for which JLabel will happily honor <code>&lt;br&gt;</code> to do
     * multiline label (it ignores <code>\n</code>).  The line number list is a {@link JLabel} with
     * one line number per line.
     */
    public static String getLineNumbersList(Document document) {
        int lineCount = document.getDefaultRootElement().getElementCount();
        int digits = Integer.toString(lineCount).length();

        StringBuilder lineNumberList = new StringBuilder("<html>");
        for (int lineNumber = 1; lineNumber <= lineCount; lineNumber++) {
            String lineNumberStr = Integer.toString(lineNumber);
            int leadingSpaces = digits - lineNumberStr.length() + 1;
            lineNumberList.append("&nbsp;".repeat(Math.max(0, leadingSpaces)))
                .append(lineNumberStr)
                .append("&nbsp;<br>");
        }
        lineNumberList.append("<br></html>");

        return lineNumberList.toString();
    }

    /**
     * Calculate and return number of lines in source code text.
     * Do this by counting newline characters then adding one if last line does
     * not end with newline character.
     */
    /*
     * IMPLEMENTATION NOTE:
     * Tried repeatedly to use StringTokenizer to count lines but got bad results
     * on empty lines (consecutive delimiters) even when returning delimiter as token.
     * BufferedReader on StringReader seems to work better.
     */
    public int getSourceLineCount() {
        BufferedReader reader = new BufferedReader(new StringReader(this.getSource()));
        int lineNums = 0;
        try {
            while (reader.readLine() != null) {
                lineNums++;
            }
        }
        catch (IOException exception) {
            // No action
        }
        return lineNums;
    }

    /**
     * Get the source code text.
     *
     * @return String containing source code.
     */
    public String getSource() {
        return this.textEditingArea.getText();
    }

    /**
     * Get the file status of this tab.
     *
     * @return The file status.
     */
    public FileStatus getFileStatus() {
        return this.fileStatus;
    }

    /**
     * Set the file status of this tab.
     *
     * @param fileStatus The file status.
     */
    public void setFileStatus(FileStatus fileStatus) {
        this.fileStatus = fileStatus;
    }

    /**
     * @see FileStatus#hasUnsavedEdits()
     */
    public boolean hasUnsavedEdits() {
        return this.fileStatus.hasUnsavedEdits();
    }

    /**
     * @see FileStatus#isNew()
     */
    public boolean isNew() {
        return this.fileStatus.isNew();
    }

    /**
     * Get the file represented by this tab.
     *
     * @return The file object.
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Set the file represented by this tab.
     *
     * @param file The file object.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Delegates to text area's requestFocusInWindow method.
     */
    public void requestTextAreaFocus() {
        this.textEditingArea.requestFocusInWindow();
    }

    /**
     * Get the manager in charge of Undo and Redo operations
     *
     * @return The undo manager object.
     */
    public UndoManager getUndoManager() {
        return this.textEditingArea.getUndoManager();
    }

    // Note: these are invoked only when copy/cut/paste are used from the
    // toolbar or menu or the defined menu Alt codes.  When
    // Ctrl-C, Ctrl-X or Ctrl-V are used, this code is NOT invoked
    // but the operation works correctly!
    // The "set visible" operations are used because clicking on the toolbar
    // icon causes both the selection highlighting AND the blinking cursor
    // to disappear!  This does not happen when using menu selection or
    // Ctrl-C/X/V.

    /**
     * Copy the currently selected text to the clipboard.
     */
    public void copyText() {
        this.textEditingArea.copy();
        this.textEditingArea.setCaretVisible(true);
        this.textEditingArea.setSelectionVisible(true);
    }

    /**
     * Cut the currently selected text to the clipboard.
     */
    public void cutText() {
        this.textEditingArea.cut();
        this.textEditingArea.setCaretVisible(true);
    }

    /**
     * Paste the current clipboard contents at the cursor position.
     */
    public void pasteText() {
        this.textEditingArea.paste();
        this.textEditingArea.setCaretVisible(true);
    }

    /**
     * Select all text.
     */
    public void selectAllText() {
        this.textEditingArea.selectAll();
        this.textEditingArea.setCaretVisible(true);
        this.textEditingArea.setSelectionVisible(true);
    }

    /**
     * Undo the previous edit.
     */
    public void undo() {
        this.textEditingArea.undo();
    }

    /**
     * Redo the previously undone edit.
     */
    public void redo() {
        this.textEditingArea.redo();
    }

    /**
     * Comment or uncomment the selection, or line at cursor if nothing is selected.
     */
    public void commentLines() {
        this.textEditingArea.commentLines();
    }

    /**
     * Automatically update whether the Undo and Redo actions are enabled or disabled
     * based on the status of the {@link javax.swing.undo.UndoManager}.
     */
    public void updateUndoRedoActions() {
        this.gui.updateUndoRedoActions();
    }

    /**
     * Update the caret position label on the editor's border to
     * display the current line and column.  The position is given
     * as text stream offset and will be converted into line and column.
     *
     * @param offset Offset into the text stream of caret.
     */
    public void displayCaretPosition(int offset) {
        this.displayCaretPosition(this.convertStreamPositionToLineColumn(offset));
    }

    /**
     * Update the status bar display for the caret position.
     *
     * @param position Position of the caret (x = column, y = line).
     */
    public void displayCaretPosition(Point position) {
        this.caretPositionLabel.setText("Line " + position.y + ":" + position.x);
    }

    /**
     * Given character stream offset in text being edited, calculate its column and line number coordinates.
     *
     * @param offset Position of character.
     * @return The column and line number position (x = column, y = line).
     */
    public Point convertStreamPositionToLineColumn(int offset) {
        String source = this.getSource();
        int line = 1;
        int column = 1;
        for (int index = 0; index < offset; index++) {
            if (index >= source.length()) {
                return new Point(-1, -1);
            }
            if (source.charAt(index) == '\n') {
                line++;
                column = 1;
            }
            else {
                column++;
            }
        }
        return new Point(column, line);
    }

    /**
     * Given line and column (position in the line) numbers, calculate
     * its byte stream position in text being edited.
     *
     * @param line   Line number in file (starts with 1)
     * @param column Position within that line (starts with 1)
     * @return corresponding stream position.  Returns -1 if there is no corresponding position.
     */
    public int convertLineColumnToStreamPosition(int line, int column) {
        String source = this.getSource();
        int textLine = 1;
        int textColumn = 1;
        for (int index = 0; index < source.length(); index++) {
            if (textLine == line && textColumn == column) {
                return index;
            }
            if (source.charAt(index) == '\n') {
                textLine++;
                textColumn = 1;
            }
            else {
                textColumn++;
            }
        }
        return -1;
    }

    /**
     * Select the specified editor text line.  Lines are numbered starting with 1, consistent
     * with line numbers displayed by the editor.
     *
     * @param line The line number to select.  Numbering starts at 1, and
     *             nothing will happen if the parameter value is less than 1
     */
    public void selectLine(int line) {
        this.selectLine(line, -1);
    }

    /**
     * Select the specified editor text line.  Lines are numbered starting with 1, consistent
     * with line numbers displayed by the editor.
     *
     * @param line   The line number to select.  Numbering starts at 1, and
     *               nothing will happen if the parameter value is less than 1
     * @param column Desired column at which to place the cursor.
     */
    public void selectLine(int line, int column) {
        if (line <= 0) {
            return;
        }

        int lineStartPosition = this.convertLineColumnToStreamPosition(line, 1);
        int lineEndPosition = this.convertLineColumnToStreamPosition(line + 1, 1) - 1;

        if (lineEndPosition < 0) { // DPS 19 Sept 2012.  Happens if "line" is last line of file.
            lineEndPosition = this.getSource().length() - 1;
        }
        if (lineStartPosition >= 0) {
            this.textEditingArea.select(lineStartPosition, lineEndPosition);
            this.textEditingArea.setSelectionVisible(true);

            if (column > 0) {
                // Made one attempt at setting cursor; didn't work but here's the attempt
                // (imagine using it in the one-parameter overloaded method above)
                this.textEditingArea.setCaretPosition(lineStartPosition + column - 1);
            }
        }
    }

    /**
     * Finds next occurrence of text in a forward search of a string. Search begins
     * at the current cursor location, and wraps around when the end of the string
     * is reached.
     *
     * @param find          the text to locate in the string
     * @param caseSensitive true if search is to be case-sensitive, false otherwise
     * @return TEXT_FOUND or TEXT_NOT_FOUND, depending on the result.
     */
    public int doFindText(String find, boolean caseSensitive) {
        return this.textEditingArea.doFindText(find, caseSensitive);
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
    public int doReplace(String find, String replace, boolean caseSensitive) {
        return this.textEditingArea.doReplace(find, replace, caseSensitive);
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
        return this.textEditingArea.doReplaceAll(find, replace, caseSensitive);
    }
}