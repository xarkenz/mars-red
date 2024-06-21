package mars.venus.actions.edit;

import mars.venus.editor.FileEditorTab;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;

import javax.swing.*;
import java.awt.event.ActionEvent;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Edit -> Undo menu item.
 */
public class EditUndoAction extends VenusAction {
    public EditUndoAction(VenusUI gui, Integer mnemonic, KeyStroke accel) {
        super(gui, "Undo", VenusUI.getSVGActionIcon("undo.svg"), "Undo last edit", mnemonic, accel);
        this.setEnabled(false);
    }

    /**
     * Adapted from TextComponentDemo.java in the
     * Java Tutorial "Text Component Features".
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        FileEditorTab tab = this.gui.getMainPane().getCurrentEditorTab();
        if (tab != null) {
            tab.undo();
            this.gui.updateUndoRedoActions();
        }
    }

    /**
     * Automatically update whether this action is enabled or disabled
     * based on the status of the {@link javax.swing.undo.UndoManager}.
     */
    @Override
    public void update() {
        FileEditorTab tab = this.gui.getMainPane().getCurrentEditorTab();
        this.setEnabled(tab != null && tab.getUndoManager().canUndo());
    }
}