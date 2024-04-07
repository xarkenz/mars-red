package mars.venus;

import mars.Application;
import mars.venus.editors.MARSTextEditingArea;
import mars.venus.editors.generic.GenericTextArea;
import mars.venus.editors.jeditsyntax.JEditBasedTextArea;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Observable;
import java.util.Observer;

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
 * be open at a time.  With 4.0 came the multi-file (pane, tab) editor, and existing
 * duties were split between EditPane and the new EditTabbedPane class.
 *
 * @author Sanderson and Bumgarner
 */
public class EditPane extends JPanel implements Observer {
    private final MARSTextEditingArea textEditingArea;
    private final VenusUI gui;
    private final JLabel caretPositionLabel;
    private final JCheckBox showLineNumbers;
    private final JLabel lineNumbers;
    private final FileStatus fileStatus;

    /**
     * Constructor for the EditPane class.
     */
    public EditPane(VenusUI gui) {
        super(new BorderLayout());
        this.gui = gui;
        // We want to be notified of editor font changes! See update() below.
        Application.getSettings().addObserver(this);
        this.fileStatus = new FileStatus();
        lineNumbers = new JLabel();

        if (Application.getSettings().useGenericTextEditor.get()) {
            this.textEditingArea = new GenericTextArea(this, lineNumbers);
        }
        else {
            this.textEditingArea = new JEditBasedTextArea(this, lineNumbers);
        }
        // sourceCode is responsible for its own scrolling
        this.add(this.textEditingArea.getOuterComponent(), BorderLayout.CENTER);

        // If source code is modified, will set flag to trigger/request file save.
        textEditingArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                // IF statement added DPS 9-Aug-2011
                // This method is triggered when file contents added to document
                // upon opening, even though not edited by user.  The IF
                // statement will sense this situation and immediately return.
                if (FileStatus.get() == FileStatus.OPENING) {
                    setFileStatus(FileStatus.NOT_EDITED);
                    FileStatus.set(FileStatus.NOT_EDITED);
                }
                else {
                    // End of 9-Aug-2011 modification.
                    if (getFileStatus() == FileStatus.NEW_NOT_EDITED) {
                        setFileStatus(FileStatus.NEW_EDITED);
                    }
                    if (getFileStatus() == FileStatus.NOT_EDITED) {
                        setFileStatus(FileStatus.EDITED);
                    }
                    if (getFileStatus() == FileStatus.NEW_EDITED) {
                        EditPane.this.gui.getEditor().setTitle("", getFilename(), getFileStatus());
                    }
                    else {
                        EditPane.this.gui.getEditor().setTitle(getPathname(), getFilename(), getFileStatus());
                    }

                    if (FileStatus.get() == FileStatus.NEW_NOT_EDITED) {
                        FileStatus.set(FileStatus.NEW_EDITED);
                    }
                    else if (FileStatus.get() != FileStatus.NEW_EDITED) {
                        FileStatus.set(FileStatus.EDITED);
                    }

                    Application.getGUI().getMainPane().getExecutePane().clearPane(); // DPS 9-Aug-2011
                }

                if (showingLineNumbers()) {
                    lineNumbers.setText(getLineNumbersList(textEditingArea.getDocument()));
                }
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                this.insertUpdate(event);
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                this.insertUpdate(event);
            }
        });

        // TODO: This option should be in the editor settings??? -Sean Clarke
        showLineNumbers = new JCheckBox("Show line numbers");
        showLineNumbers.setToolTipText("When checked, line numbers are displayed on the left-hand side of the editor.");
        showLineNumbers.setEnabled(false);
        showLineNumbers.setSelected(Application.getSettings().displayEditorLineNumbers.get());

        this.setSourceCode("", false);

        lineNumbers.setFont(getLineNumberFont(textEditingArea.getFont()));
        lineNumbers.setForeground(UIManager.getColor("Venus.Editor.lineNumbers.foreground"));
        lineNumbers.setBackground(UIManager.getColor("Venus.Editor.lineNumbers.background"));
        lineNumbers.setVerticalAlignment(JLabel.TOP);
        lineNumbers.setText("");
        lineNumbers.setVisible(true);

        // Listener fires when "Show Line Numbers" check box is clicked.
        showLineNumbers.addItemListener(event -> {
            if (showLineNumbers.isSelected()) {
                lineNumbers.setText(getLineNumbersList(textEditingArea.getDocument()));
                lineNumbers.setVisible(true);
            }
            else {
                lineNumbers.setText("");
                lineNumbers.setVisible(false);
            }
            textEditingArea.revalidate(); // added 16 Jan 2012 to assure label redrawn.
            Application.getSettings().displayEditorLineNumbers.set(showLineNumbers.isSelected());
            // Needed because caret disappears when checkbox clicked
            textEditingArea.setCaretVisible(true);
            textEditingArea.requestFocusInWindow();
        });

        JPanel editInfo = new JPanel(new BorderLayout());
        caretPositionLabel = new JLabel();
        caretPositionLabel.setToolTipText("Tracks the current position of the text editing cursor.");
        displayCaretPosition(new Point());
        editInfo.add(caretPositionLabel, BorderLayout.WEST);
        editInfo.add(showLineNumbers, BorderLayout.CENTER);
        this.add(editInfo, BorderLayout.SOUTH);
    }

    /**
     * For initializing the source code when opening an ASM file.
     *
     * @param sourceCode String containing text
     * @param editable   Set true if code is editable else false
     */
    public void setSourceCode(String sourceCode, boolean editable) {
        this.textEditingArea.setSourceCode(sourceCode, editable);

        if (showingLineNumbers()) {
            lineNumbers.setText(getLineNumbersList(this.textEditingArea.getDocument()));
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
     * Resulting string is HTML, for which JLabel will happily honor <br> to do
     * multiline label (it ignores '\n').  The line number list is a JLabel with
     * one line number per line.
     */
    public String getLineNumbersList(Document doc) {
        StringBuilder lineNumberList = new StringBuilder("<html>");
        int lineCount = doc.getDefaultRootElement().getElementCount();
        int digits = Integer.toString(lineCount).length();
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
        BufferedReader bufStringReader = new BufferedReader(new StringReader(textEditingArea.getText()));
        int lineNums = 0;
        try {
            while (bufStringReader.readLine() != null) {
                lineNums++;
            }
        }
        catch (IOException e) {
            // No action
        }
        return lineNums;
    }

    /**
     * Get source code text
     *
     * @return Sting containing source code
     */
    public String getSource() {
        return textEditingArea.getText();
    }

    /**
     * Set the editing status for this EditPane's associated document.
     * For the argument, use one of the constants from class FileStatus.
     *
     * @param fileStatus the status constant from class FileStatus
     */
    public void setFileStatus(int fileStatus) {
        this.fileStatus.setFileStatus(fileStatus);
    }

    /**
     * Get the editing status for this EditPane's associated document.
     * This will be one of the constants from class FileStatus.
     */
    public int getFileStatus() {
        return this.fileStatus.getFileStatus();
    }

    /**
     * @see FileStatus#getFilename()
     */
    public String getFilename() {
        return this.fileStatus.getFilename();
    }

    /**
     * @see FileStatus#getPathname()
     */
    public String getPathname() {
        return this.fileStatus.getPathname();
    }

    /**
     * @see FileStatus#setPathname(String)
     */
    public void setPathname(String pathname) {
        this.fileStatus.setPathname(pathname);
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
     * Delegates to text area's requestFocusInWindow method.
     */
    public void tellEditingComponentToRequestFocusInWindow() {
        this.textEditingArea.requestFocusInWindow();
    }

    /**
     * @see FileStatus#updateStaticFileStatus()
     */
    public void updateStaticFileStatus() {
        fileStatus.updateStaticFileStatus();
    }

    /**
     * Get the manager in charge of Undo and Redo operations
     *
     * @return the Undo manager
     */
    public UndoManager getUndoManager() {
        return textEditingArea.getUndoManager();
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
     * Copy currently-selected text into clipboard.
     */
    public void copyText() {
        textEditingArea.copy();
        textEditingArea.setCaretVisible(true);
        textEditingArea.setSelectionVisible(true);
    }

    /**
     * Cut currently-selected text into clipboard.
     */
    public void cutText() {
        textEditingArea.cut();
        textEditingArea.setCaretVisible(true);
    }

    /**
     * Paste clipboard contents at cursor position.
     */
    public void pasteText() {
        textEditingArea.paste();
        textEditingArea.setCaretVisible(true);
    }

    /**
     * Select all text.
     */
    public void selectAllText() {
        textEditingArea.selectAll();
        textEditingArea.setCaretVisible(true);
        textEditingArea.setSelectionVisible(true);
    }

    /**
     * Undo previous edit.
     */
    public void undo() {
        textEditingArea.undo();
    }

    /**
     * Redo previously undone edit.
     */
    public void redo() {
        textEditingArea.redo();
    }

    /**
     * Automatically update whether the Undo and Redo actions are enabled or disabled
     * based on the status of the {@link javax.swing.undo.UndoManager}.
     */
    public void updateUndoRedoActions() {
        gui.updateUndoRedoActions();
    }

    /**
     * get editor's line number display status
     *
     * @return true if editor is current displaying line numbers, false otherwise.
     */
    public boolean showingLineNumbers() {
        return showLineNumbers.isSelected();
    }

    /**
     * enable or disable checkbox that controls display of line numbers
     *
     * @param enable True to enable box, false to disable.
     */
    public void setShowLineNumbersEnabled(boolean enable) {
        showLineNumbers.setEnabled(enable);
    }

    /**
     * Update the caret position label on the editor's border to
     * display the current line and column.  The position is given
     * as text stream offset and will be converted into line and column.
     *
     * @param pos Offset into the text stream of caret.
     */
    public void displayCaretPosition(int pos) {
        displayCaretPosition(convertStreamPositionToLineColumn(pos));
    }

    /**
     * Display cursor coordinates
     *
     * @param p Point object with x-y (column, line number) coordinates of cursor
     */
    public void displayCaretPosition(Point p) {
        caretPositionLabel.setText("Line: " + p.y + " Column: " + p.x);
    }

    /**
     * Given byte stream position in text being edited, calculate its column and line
     * number coordinates.
     *
     * @param position Position of character.
     * @return The column and line number coordinate as a Point.
     */
    public Point convertStreamPositionToLineColumn(int position) {
        String textStream = textEditingArea.getText();
        int line = 1;
        int column = 1;
        for (int i = 0; i < position; i++) {
            if (textStream.charAt(i) == '\n') {
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
        String textStream = textEditingArea.getText();
        int textLength = textStream.length();
        int textLine = 1;
        int textColumn = 1;
        for (int i = 0; i < textLength; i++) {
            if (textLine == line && textColumn == column) {
                return i;
            }
            if (textStream.charAt(i) == '\n') {
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
     * @param line The desired line number of this TextPane's text.  Numbering starts at 1, and
     *             nothing will happen if the parameter value is less than 1
     */
    public void selectLine(int line) {
        if (line > 0) {
            int lineStartPosition = convertLineColumnToStreamPosition(line, 1);
            int lineEndPosition = convertLineColumnToStreamPosition(line + 1, 1) - 1;
            if (lineEndPosition < 0) { // DPS 19 Sept 2012.  Happens if "line" is last line of file.

                lineEndPosition = textEditingArea.getText().length() - 1;
            }
            if (lineStartPosition >= 0) {
                textEditingArea.select(lineStartPosition, lineEndPosition);
                textEditingArea.setSelectionVisible(true);
            }
        }
    }

    /**
     * Select the specified editor text line.  Lines are numbered starting with 1, consistent
     * with line numbers displayed by the editor.
     *
     * @param line   The desired line number of this TextPane's text.  Numbering starts at 1, and
     *               nothing will happen if the parameter value is less than 1
     * @param column Desired column at which to place the cursor.
     */
    public void selectLine(int line, int column) {
        selectLine(line);
        // Made one attempt at setting cursor; didn't work but here's the attempt
        // (imagine using it in the one-parameter overloaded method above)
        //sourceCode.setCaretPosition(lineStartPosition+column-1);
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
        return textEditingArea.doFindText(find, caseSensitive);
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
        return textEditingArea.doReplace(find, replace, caseSensitive);
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
        return textEditingArea.doReplaceAll(find, replace, caseSensitive);
    }

    /**
     * Update, if source code is visible, when Font setting changes.
     * This method is specified by the Observer interface.
     */
    @Override
    public void update(Observable fontChanger, Object arg) {
        textEditingArea.setFont(Application.getSettings().getEditorFont());
        textEditingArea.setLineHighlightEnabled(Application.getSettings().highlightCurrentEditorLine.get());
        textEditingArea.setCaretBlinkRate(Application.getSettings().caretBlinkRate.get());
        textEditingArea.setTabSize(Application.getSettings().editorTabSize.get());
        textEditingArea.updateSyntaxStyles();
        textEditingArea.revalidate();
        // We want line numbers to be displayed same size but always PLAIN style.
        // Easiest way to get same pixel height as source code is to set to same
        // font family as the source code! It can get a bit complicated otherwise
        // because different fonts will render the same font size in different
        // pixel heights.  This is a factor because the line numbers as displayed
        // in the editor form a separate column from the source code and if the
        // pixel height is not the same then the numbers will not line up with
        // the source lines.
        lineNumbers.setFont(getLineNumberFont(textEditingArea.getFont()));
        lineNumbers.revalidate();
    }

    /**
     * Private helper method.
     * Determine font to use for editor line number display, given current
     * font for source code.
     */
    private Font getLineNumberFont(Font sourceFont) {
        return (textEditingArea.getFont().getStyle() == Font.PLAIN) ? sourceFont : new Font(sourceFont.getFamily(), Font.PLAIN, sourceFont.getSize());
    }
}