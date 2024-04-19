package mars.venus.actions.settings;

import mars.Application;
import mars.venus.AbstractFontSettingDialog;
import mars.venus.ColorSelectButton;
import mars.venus.PreferencesDialog;
import mars.venus.VenusUI;
import mars.venus.actions.VenusAction;
import mars.venus.editor.Editor;
import mars.venus.editor.jeditsyntax.SyntaxStyle;
import mars.venus.editor.jeditsyntax.SyntaxUtilities;
import mars.venus.editor.jeditsyntax.tokenmarker.MIPSTokenMarker;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.Caret;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

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
 * Action class for the Settings menu item for text editor settings.
 */
public class SettingsPreferencesAction extends VenusAction {
    /**
     * Create a new SettingsEditorAction.
     */
    public SettingsPreferencesAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    /**
     * When this action is triggered, launch a dialog to view and modify
     * editor settings.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        JDialog dialog = new PreferencesDialog(gui, "Text Editor Settings", true);
        dialog.setVisible(true);
    }
}