package mars.venus.actions;

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
 * Parent class for Action subclasses representing every menu/toolbar option.
 */
public abstract class VenusAction extends AbstractAction {
    protected VenusUI gui;
    protected KeyStroke shortcut;

    protected VenusAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(name, icon);
        this.gui = gui;
        this.shortcut = accel;
        putValue(SHORT_DESCRIPTION, description);
        putValue(MNEMONIC_KEY, mnemonic);
        putValue(ACCELERATOR_KEY, accel);
    }

    /**
     * Should be overridden by subclasses to carry out the action they represent.
     */
    @Override
    public abstract void actionPerformed(ActionEvent event);

    /**
     * Register this action as a key shortcut when the component is in focus.
     *
     * @param component the component to register the key shortcut on.
     */
    public void registerShortcut(JComponent component) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(this.getShortcut(), this.getName());
        component.getActionMap().put(this.getName(), this);
    }

    /**
     * @return The GUI instance this action was created for.
     */
    public VenusUI getGUI() {
        return this.gui;
    }

    /**
     * @return The key shortcut to execute this action.
     */
    public KeyStroke getShortcut() {
        return this.shortcut;
    }

    /**
     * @return The name assigned to the action.
     */
    public String getName() {
        return this.getValue(Action.NAME).toString();
    }

    /**
     * Update this action according to the current GUI state.
     */
    public void update() {
        // Do nothing by default
    }
}