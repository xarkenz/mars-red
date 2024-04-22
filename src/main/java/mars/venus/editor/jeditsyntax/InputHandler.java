/*
 * InputHandler.java - Manages key bindings and executes actions
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package mars.venus.editor.jeditsyntax;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * An input handler converts the user's key strokes into concrete actions.
 * It also takes care of macro recording and action repetition.
 * <p>
 * This class provides all the necessary support code for an input
 * handler, but doesn't actually do any key binding logic. It is up
 * to the implementations of this class to do so.
 * <p>
 * 08/12/2002: Clipboard actions (Oliver Henning)
 *
 * @author Slava Pestov
 * @version $Id: InputHandler.java,v 1.14 1999/12/13 03:40:30 sp Exp $
 * @see DefaultInputHandler
 */
public abstract class InputHandler extends KeyAdapter {
    /**
     * If this client property is set to Boolean.TRUE on the text area,
     * the home/end keys will support 'smart' BRIEF-like behaviour
     * (one press = start/end of line, two presses = start/end of
     * view screen, three presses = start/end of document). By default,
     * this property is not set.
     */
    public static final String SMART_HOME_END_PROPERTY = "InputHandler.homeEnd";

    public static final ActionListener BACKSPACE = new BackspaceAction();
    public static final ActionListener BACKSPACE_WORD = new BackspaceWordAction();
    public static final ActionListener DELETE = new DeleteAction();
    public static final ActionListener DELETE_WORD = new DeleteWordAction();
    public static final ActionListener END = new EndAction(false);
    public static final ActionListener DOCUMENT_END = new DocumentEndAction(false);
    public static final ActionListener SELECT_ALL = new SelectAllAction();
    public static final ActionListener SELECT_END = new EndAction(true);
    public static final ActionListener SELECT_DOC_END = new DocumentEndAction(true);
    public static final ActionListener INSERT_BREAK = new InsertNewlineAction();
    public static final ActionListener INSERT_TAB = new InsertTabAction();
    public static final ActionListener HOME = new HomeAction(false);
    public static final ActionListener DOCUMENT_HOME = new DocumentHomeAction(false);
    public static final ActionListener SELECT_HOME = new HomeAction(true);
    public static final ActionListener SELECT_DOC_HOME = new DocumentHomeAction(true);
    public static final ActionListener NEXT_CHAR = new NextCharAction(false);
    public static final ActionListener NEXT_LINE = new NextLineAction(false);
    public static final ActionListener NEXT_PAGE = new NextPageAction(false);
    public static final ActionListener NEXT_WORD = new NextWordAction(false);
    public static final ActionListener SELECT_NEXT_CHAR = new NextCharAction(true);
    public static final ActionListener SELECT_NEXT_LINE = new NextLineAction(true);
    public static final ActionListener SELECT_NEXT_PAGE = new NextPageAction(true);
    public static final ActionListener SELECT_NEXT_WORD = new NextWordAction(true);
    public static final ActionListener OVERWRITE = new ToggleOverwriteAction();
    public static final ActionListener PREV_CHAR = new PrevCharAction(false);
    public static final ActionListener PREV_LINE = new PrevLineAction(false);
    public static final ActionListener PREV_PAGE = new PrevPageAction(false);
    public static final ActionListener PREV_WORD = new PrevWordAction(false);
    public static final ActionListener SELECT_PREV_CHAR = new PrevCharAction(true);
    public static final ActionListener SELECT_PREV_LINE = new PrevLineAction(true);
    public static final ActionListener SELECT_PREV_PAGE = new PrevPageAction(true);
    public static final ActionListener SELECT_PREV_WORD = new PrevWordAction(true);
    public static final ActionListener REPEAT = new RepeatAction();
    public static final ActionListener TOGGLE_RECT = new ToggleRectangularAction();
    // Clipboard
    public static final ActionListener CLIP_COPY = new ClipboardCopyAction();
    public static final ActionListener CLIP_PASTE = new ClipboardPasteAction();
    public static final ActionListener CLIP_CUT = new ClipboardCutAction();
    // Default action
    public static final ActionListener INSERT_CHAR = new InsertCharAction();

    private static final Hashtable<String, ActionListener> ACTIONS;

    static {
        ACTIONS = new Hashtable<>();
        ACTIONS.put("backspace", BACKSPACE);
        ACTIONS.put("backspace-word", BACKSPACE_WORD);
        ACTIONS.put("delete", DELETE);
        ACTIONS.put("delete-word", DELETE_WORD);
        ACTIONS.put("end", END);
        ACTIONS.put("select-all", SELECT_ALL);
        ACTIONS.put("select-end", SELECT_END);
        ACTIONS.put("document-end", DOCUMENT_END);
        ACTIONS.put("select-doc-end", SELECT_DOC_END);
        ACTIONS.put("insert-break", INSERT_BREAK);
        ACTIONS.put("insert-tab", INSERT_TAB);
        ACTIONS.put("home", HOME);
        ACTIONS.put("select-home", SELECT_HOME);
        ACTIONS.put("document-home", DOCUMENT_HOME);
        ACTIONS.put("select-doc-home", SELECT_DOC_HOME);
        ACTIONS.put("next-char", NEXT_CHAR);
        ACTIONS.put("next-line", NEXT_LINE);
        ACTIONS.put("next-page", NEXT_PAGE);
        ACTIONS.put("next-word", NEXT_WORD);
        ACTIONS.put("select-next-char", SELECT_NEXT_CHAR);
        ACTIONS.put("select-next-line", SELECT_NEXT_LINE);
        ACTIONS.put("select-next-page", SELECT_NEXT_PAGE);
        ACTIONS.put("select-next-word", SELECT_NEXT_WORD);
        ACTIONS.put("overwrite", OVERWRITE);
        ACTIONS.put("prev-char", PREV_CHAR);
        ACTIONS.put("prev-line", PREV_LINE);
        ACTIONS.put("prev-page", PREV_PAGE);
        ACTIONS.put("prev-word", PREV_WORD);
        ACTIONS.put("select-prev-char", SELECT_PREV_CHAR);
        ACTIONS.put("select-prev-line", SELECT_PREV_LINE);
        ACTIONS.put("select-prev-page", SELECT_PREV_PAGE);
        ACTIONS.put("select-prev-word", SELECT_PREV_WORD);
        ACTIONS.put("repeat", REPEAT);
        ACTIONS.put("toggle-rect", TOGGLE_RECT);
        ACTIONS.put("clipboard-copy", CLIP_COPY);
        ACTIONS.put("clipboard-paste", CLIP_PASTE);
        ACTIONS.put("clipboard-cut", CLIP_CUT);
        ACTIONS.put("insert-char", INSERT_CHAR);
    }

    /**
     * Returns a named text area action.
     *
     * @param name The action name
     */
    public static ActionListener getAction(String name) {
        return ACTIONS.get(name);
    }

    /**
     * Returns the name of the specified text area action.
     *
     * @param action The action
     */
    public static String getActionName(ActionListener action) {
        for (Map.Entry<String, ActionListener> entry : ACTIONS.entrySet()) {
            if (entry.getValue().equals(action)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Adds the default key bindings to this input handler.
     * This should not be called in the constructor of this
     * input handler, because applications might load the
     * key bindings from a file, etc.
     */
    public abstract void addDefaultKeyBindings();

    /**
     * Adds a key binding to this input handler.
     *
     * @param keyBinding The key binding (the format of this is input-handler specific)
     * @param action     The action
     */
    public abstract void addKeyBinding(String keyBinding, ActionListener action);

    /**
     * Removes a key binding from this input handler.
     *
     * @param keyBinding The key binding
     */
    public abstract void removeKeyBinding(String keyBinding);

    /**
     * Removes all key bindings from this input handler.
     */
    public abstract void removeAllKeyBindings();

    /**
     * Grabs the next key typed event and invokes the specified
     * action with the key as a the action command.
     *
     * @param action The action
     */
    public void grabNextKeyStroke(ActionListener action) {
        grabAction = action;
    }

    /**
     * Returns if repeating is enabled. When repeating is enabled,
     * actions will be executed multiple times. This is usually
     * invoked with a special key stroke in the input handler.
     */
    public boolean isRepeatEnabled() {
        return repeat;
    }

    /**
     * Enables repeating. When repeating is enabled, actions will be
     * executed multiple times. Once repeating is enabled, the input
     * handler should read a number from the keyboard.
     */
    public void setRepeatEnabled(boolean repeat) {
        this.repeat = repeat;
    }

    /**
     * Returns the number of times the next action will be repeated.
     */
    public int getRepeatCount() {
        return (repeat ? Math.max(1, repeatCount) : 1);
    }

    /**
     * Sets the number of times the next action will be repeated.
     *
     * @param repeatCount The repeat count
     */
    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    /**
     * Returns the macro recorder. If this is non-null, all executed
     * actions should be forwarded to the recorder.
     */
    public InputHandler.MacroRecorder getMacroRecorder() {
        return recorder;
    }

    /**
     * Sets the macro recorder. If this is non-null, all executed
     * actions should be forwarded to the recorder.
     *
     * @param recorder The macro recorder
     */
    public void setMacroRecorder(InputHandler.MacroRecorder recorder) {
        this.recorder = recorder;
    }

    /**
     * Returns a copy of this input handler that shares the same
     * key bindings. Setting key bindings in the copy will also
     * set them in the original.
     */
    public abstract InputHandler copy();

    /**
     * Executes the specified action, repeating and recording it as
     * necessary.
     *
     * @param action      The action listener
     * @param source        The event source
     * @param actionCommand The action command
     */
    public void executeAction(ActionListener action, Object source, String actionCommand) {
        // create event
        ActionEvent event = new ActionEvent(source, ActionEvent.ACTION_PERFORMED, actionCommand);

        // don't do anything if the action is a wrapper
        // (like EditAction.Wrapper)
        if (action instanceof Wrapper) {
            action.actionPerformed(event);
            return;
        }

        // remember old values, in case action changes them
        boolean repeat = this.repeat;
        int repeatCount = getRepeatCount();

        // execute the action
        if (action instanceof NonRepeatable) {
            action.actionPerformed(event);
        }
        else {
            for (int i = 0; i < Math.max(1, this.repeatCount); i++) {
                action.actionPerformed(event);
            }
        }

        // do recording. Notice that we do no recording whatsoever
        // for actions that grab keys
        if (grabAction == null) {
            if (recorder != null) {
                if (!(action instanceof NonRecordable)) {
                    if (repeatCount != 1) {
                        recorder.actionPerformed(REPEAT, String.valueOf(repeatCount));
                    }

                    recorder.actionPerformed(action, actionCommand);
                }
            }

            // If repeat was true originally, clear it
            // Otherwise it might have been set by the action, etc
            if (repeat) {
                this.repeat = false;
                this.repeatCount = 0;
            }
        }
    }

    /**
     * Returns the text area that fired the specified event.
     *
     * @param event The event
     */
    public static JEditTextArea getTextArea(EventObject event) {
        if (event != null && event.getSource() instanceof Component component) {
            // find the parent text area
            while (component != null) {
                if (component instanceof JEditTextArea textArea) {
                    return textArea;
                }
                else if (component instanceof JPopupMenu popupMenu) {
                    component = popupMenu.getInvoker();
                }
                else {
                    component = component.getParent();
                }
            }
        }

        // this shouldn't happen
        throw new RuntimeException("InputHandler.getTextArea() did not find a valid component");
    }

    /**
     * If a key is being grabbed, this method should be called with
     * the appropriate key event. It executes the grab action with
     * the typed character as the parameter.
     */
    protected void handleGrabAction(KeyEvent event) {
        // Clear it *before* it is executed so that executeAction()
        // resets the repeat count
        ActionListener grabAction = this.grabAction;
        this.grabAction = null;
        executeAction(grabAction, event.getSource(), String.valueOf(event.getKeyChar()));
    }

    protected ActionListener grabAction;
    protected boolean repeat;
    protected int repeatCount;
    protected MacroRecorder recorder;

    /**
     * If an action implements this interface, it should not be repeated.
     * Instead, it will handle the repetition itself.
     */
    public interface NonRepeatable {}

    /**
     * If an action implements this interface, it should not be recorded
     * by the macro recorder. Instead, it will do its own recording.
     */
    public interface NonRecordable {}

    /**
     * For use by EditAction.Wrapper only.
     *
     * @since jEdit 2.2final
     */
    public interface Wrapper {}

    /**
     * Macro recorder.
     */
    public interface MacroRecorder {
        void actionPerformed(ActionListener listener, String actionCommand);
    }

    public static class BackspaceAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);

            if (!textArea.isEditable()) {
                textArea.getToolkit().beep();
                return;
            }

            if (textArea.getSelectionStart() != textArea.getSelectionEnd()) {
                textArea.setSelectedText("");
            }
            else {
                int caret = textArea.getCaretPosition();
                if (caret == 0) {
                    textArea.getToolkit().beep();
                    return;
                }
                try {
                    textArea.getDocument().remove(caret - 1, 1);
                }
                catch (BadLocationException bl) {
                    bl.printStackTrace();
                }
            }
        }
    }

    public static class BackspaceWordAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int start = textArea.getSelectionStart();
            if (start != textArea.getSelectionEnd()) {
                textArea.setSelectedText("");
            }

            int line = textArea.getCaretLine();
            int lineStart = textArea.getLineStartOffset(line);
            int caret = start - lineStart;

            String lineText = textArea.getLineText(textArea.getCaretLine());

            if (caret == 0) {
                if (lineStart == 0) {
                    textArea.getToolkit().beep();
                    return;
                }
                caret--;
            }
            else {
                String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
                caret = TextUtilities.findWordStart(lineText, caret, noWordSep);
            }

            try {
                textArea.getDocument().remove(caret + lineStart, start - (caret + lineStart));
            }
            catch (BadLocationException bl) {
                bl.printStackTrace();
            }
        }
    }

    public static class DeleteAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);

            if (!textArea.isEditable()) {
                textArea.getToolkit().beep();
                return;
            }

            if (textArea.getSelectionStart() != textArea.getSelectionEnd()) {
                textArea.setSelectedText("");
            }
            else {
                int caret = textArea.getCaretPosition();
                if (caret == textArea.getDocumentLength()) {
                    textArea.getToolkit().beep();
                    return;
                }
                try {
                    textArea.getDocument().remove(caret, 1);
                }
                catch (BadLocationException bl) {
                    bl.printStackTrace();
                }
            }
        }
    }

    public static class DeleteWordAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int start = textArea.getSelectionStart();
            if (start != textArea.getSelectionEnd()) {
                textArea.setSelectedText("");
            }

            int line = textArea.getCaretLine();
            int lineStart = textArea.getLineStartOffset(line);
            int caret = start - lineStart;

            String lineText = textArea.getLineText(textArea.getCaretLine());

            if (caret == lineText.length()) {
                if (lineStart + caret == textArea.getDocumentLength()) {
                    textArea.getToolkit().beep();
                    return;
                }
                caret++;
            }
            else {
                String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
                caret = TextUtilities.findWordEnd(lineText, caret, noWordSep);
            }

            try {
                textArea.getDocument().remove(start, (caret + lineStart) - start);
            }
            catch (BadLocationException bl) {
                bl.printStackTrace();
            }
        }
    }

    public static class EndAction implements ActionListener {
        private final boolean select;

        public EndAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);

            int caret = textArea.getCaretPosition();

            int lastOfLine = textArea.getLineEndOffset(textArea.getCaretLine()) - 1;
            int lastVisibleLine = textArea.getFirstLine() + textArea.getVisibleLines();
            if (lastVisibleLine >= textArea.getLineCount()) {
                lastVisibleLine = Math.min(textArea.getLineCount() - 1, lastVisibleLine);
            }
            else {
                lastVisibleLine -= (textArea.getElectricScroll() + 1);
            }

            int lastVisible = textArea.getLineEndOffset(lastVisibleLine) - 1;
            int lastDocument = textArea.getDocumentLength();

            if (caret == lastDocument) {
                textArea.getToolkit().beep();
                return;
            }
            else if (!Boolean.TRUE.equals(textArea.getClientProperty(SMART_HOME_END_PROPERTY))) {
                caret = lastOfLine;
            }
            else if (caret == lastVisible) {
                caret = lastDocument;
            }
            else if (caret == lastOfLine) {
                caret = lastVisible;
            }
            else {
                caret = lastOfLine;
            }

            if (select) {
                textArea.select(textArea.getMarkPosition(), caret);
            }
            else {
                textArea.setCaretPosition(caret);
            }
        }
    }

    public static class SelectAllAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            textArea.selectAll();
        }
    }

    public static class DocumentEndAction implements ActionListener {
        private final boolean select;

        public DocumentEndAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            if (select) {
                textArea.select(textArea.getMarkPosition(), textArea.getDocumentLength());
            }
            else {
                textArea.setCaretPosition(textArea.getDocumentLength());
            }
        }
    }

    public static class HomeAction implements ActionListener {
        private final boolean select;

        public HomeAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);

            int caret = textArea.getCaretPosition();

            int firstLine = textArea.getFirstLine();

            int firstOfLine = textArea.getLineStartOffset(textArea.getCaretLine());
            int firstVisibleLine = (firstLine == 0 ? 0 : firstLine + textArea.getElectricScroll());
            int firstVisible = textArea.getLineStartOffset(firstVisibleLine);

            if (caret == 0) {
                textArea.getToolkit().beep();
                return;
            }
            else if (!Boolean.TRUE.equals(textArea.getClientProperty(SMART_HOME_END_PROPERTY))) {
                caret = firstOfLine;
            }
            else if (caret == firstVisible) {
                caret = 0;
            }
            else if (caret == firstOfLine) {
                caret = firstVisible;
            }
            else {
                caret = firstOfLine;
            }

            if (select) {
                textArea.select(textArea.getMarkPosition(), caret);
            }
            else {
                textArea.setCaretPosition(caret);
            }
        }
    }

    public static class DocumentHomeAction implements ActionListener {
        private final boolean select;

        public DocumentHomeAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            if (select) {
                textArea.select(textArea.getMarkPosition(), 0);
            }
            else {
                textArea.setCaretPosition(0);
            }
        }
    }

    public static class InsertNewlineAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);

            if (!textArea.isEditable()) {
                textArea.getToolkit().beep();
                return;
            }
            // AutoIndent feature added DPS 31-Dec-2010
            textArea.setSelectedText("\n" + textArea.getAutoIndent());
        }
    }

    public static class InsertTabAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);

            if (!textArea.isEditable()) {
                textArea.getToolkit().beep();
                return;
            }

            textArea.overwriteSetSelectedText("\t");
        }
    }

    public static class NextCharAction implements ActionListener {
        private final boolean select;

        public NextCharAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int caret = textArea.getCaretPosition();
            if (caret == textArea.getDocumentLength()) {
                textArea.getToolkit().beep();
                return;
            }

            if (select) {
                textArea.select(textArea.getMarkPosition(), caret + 1);
            }
            else {
                textArea.setCaretPosition(caret + 1);
            }
        }
    }

    public static class NextLineAction implements ActionListener {
        private final boolean select;

        public NextLineAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int caret = textArea.getCaretPosition();
            int line = textArea.getCaretLine();

            if (line == textArea.getLineCount() - 1) {
                textArea.getToolkit().beep();
                return;
            }

            int magic = textArea.getMagicCaretPosition();
            if (magic == -1) {
                magic = textArea.offsetToX(line, caret - textArea.getLineStartOffset(line));
            }

            caret = textArea.getLineStartOffset(line + 1) + textArea.xToOffset(line + 1, magic);
            if (select) {
                textArea.select(textArea.getMarkPosition(), caret);
            }
            else {
                textArea.setCaretPosition(caret);
            }
            textArea.setMagicCaretPosition(magic);
        }
    }

    public static class NextPageAction implements ActionListener {
        private final boolean select;

        public NextPageAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int lineCount = textArea.getLineCount();
            int firstLine = textArea.getFirstLine();
            int visibleLines = textArea.getVisibleLines();
            int line = textArea.getCaretLine();

            firstLine += visibleLines;

            if (firstLine + visibleLines >= lineCount - 1) {
                firstLine = lineCount - visibleLines;
            }

            textArea.setFirstLine(firstLine);

            int caret = textArea.getLineStartOffset(Math.min(textArea.getLineCount() - 1, line + visibleLines));
            if (select) {
                textArea.select(textArea.getMarkPosition(), caret);
            }
            else {
                textArea.setCaretPosition(caret);
            }
        }
    }

    public static class NextWordAction implements ActionListener {
        private final boolean select;

        public NextWordAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int caret = textArea.getCaretPosition();
            int line = textArea.getCaretLine();
            int lineStart = textArea.getLineStartOffset(line);
            caret -= lineStart;

            String lineText = textArea.getLineText(textArea.getCaretLine());

            if (caret == lineText.length()) {
                if (lineStart + caret == textArea.getDocumentLength()) {
                    textArea.getToolkit().beep();
                    return;
                }
                caret++;
            }
            else {
                String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
                caret = TextUtilities.findWordEnd(lineText, caret, noWordSep);
            }

            if (select) {
                textArea.select(textArea.getMarkPosition(), lineStart + caret);
            }
            else {
                textArea.setCaretPosition(lineStart + caret);
            }
        }
    }

    public static class ToggleOverwriteAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            textArea.setOverwriteEnabled(!textArea.isOverwriteEnabled());
        }
    }

    public static class PrevCharAction implements ActionListener {
        private final boolean select;

        public PrevCharAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int caret = textArea.getCaretPosition();
            if (caret == 0) {
                textArea.getToolkit().beep();
                return;
            }

            if (select) {
                textArea.select(textArea.getMarkPosition(), caret - 1);
            }
            else {
                textArea.setCaretPosition(caret - 1);
            }
        }
    }

    public static class PrevLineAction implements ActionListener {
        private final boolean select;

        public PrevLineAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int caret = textArea.getCaretPosition();
            int line = textArea.getCaretLine();

            if (line == 0) {
                textArea.getToolkit().beep();
                return;
            }

            int magic = textArea.getMagicCaretPosition();
            if (magic == -1) {
                magic = textArea.offsetToX(line, caret - textArea.getLineStartOffset(line));
            }

            caret = textArea.getLineStartOffset(line - 1) + textArea.xToOffset(line - 1, magic);
            if (select) {
                textArea.select(textArea.getMarkPosition(), caret);
            }
            else {
                textArea.setCaretPosition(caret);
            }
            textArea.setMagicCaretPosition(magic);
        }
    }

    public static class PrevPageAction implements ActionListener {
        private final boolean select;

        public PrevPageAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int firstLine = textArea.getFirstLine();
            int visibleLines = textArea.getVisibleLines();
            int line = textArea.getCaretLine();

            if (firstLine < visibleLines) {
                firstLine = visibleLines;
            }

            textArea.setFirstLine(firstLine - visibleLines);

            int caret = textArea.getLineStartOffset(Math.max(0, line - visibleLines));
            if (select) {
                textArea.select(textArea.getMarkPosition(), caret);
            }
            else {
                textArea.setCaretPosition(caret);
            }
        }
    }

    public static class PrevWordAction implements ActionListener {
        private final boolean select;

        public PrevWordAction(boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            int caret = textArea.getCaretPosition();
            int line = textArea.getCaretLine();
            int lineStart = textArea.getLineStartOffset(line);
            caret -= lineStart;

            String lineText = textArea.getLineText(textArea.getCaretLine());

            if (caret == 0) {
                if (lineStart == 0) {
                    textArea.getToolkit().beep();
                    return;
                }
                caret--;
            }
            else {
                String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
                caret = TextUtilities.findWordStart(lineText, caret, noWordSep);
            }

            if (select) {
                textArea.select(textArea.getMarkPosition(), lineStart + caret);
            }
            else {
                textArea.setCaretPosition(lineStart + caret);
            }
        }
    }

    public static class RepeatAction implements ActionListener, NonRecordable {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            textArea.getInputHandler().setRepeatEnabled(true);
            String actionCommand = event.getActionCommand();
            if (actionCommand != null) {
                textArea.getInputHandler().setRepeatCount(Integer.parseInt(actionCommand));
            }
        }
    }

    public static class ToggleRectangularAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            textArea.setSelectionRectangular(!textArea.isSelectionRectangular());
        }
    }

    public static class InsertCharAction implements ActionListener, NonRepeatable {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            String str = event.getActionCommand();
            int repeatCount = textArea.getInputHandler().getRepeatCount();

            if (textArea.isEditable()) {
                textArea.overwriteSetSelectedText(str.repeat(Math.max(0, repeatCount)));
            }
            else {
                textArea.getToolkit().beep();
            }
        }
    }

    public static class ClipboardCutAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            textArea.cut();
        }
    }

    public static class ClipboardCopyAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            textArea.copy();
        }

    }

    public static class ClipboardPasteAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JEditTextArea textArea = getTextArea(event);
            textArea.paste();
        }
    }
}
