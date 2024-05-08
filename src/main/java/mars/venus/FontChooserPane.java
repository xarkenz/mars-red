package mars.venus;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Abstract class for a font selection dialog.
 */
public class FontChooserPane extends JPanel {
    public static final int MIN_FONT_SIZE = 6;
    public static final int MAX_FONT_SIZE = 72;

    private final JComboBox<String> fontFamilySelector;
    private final JCheckBox boldCheckBox;
    private final JCheckBox italicCheckBox;
    private final SpinnerNumberModel fontSizeSelectorModel;

    private final List<ChangeListener> changeListeners;
    private final String[] availableFontFamilies;
    private Font currentFont;

    public FontChooserPane(Font initialFont) {
        super(new FlowLayout(FlowLayout.LEFT, 12, 12));
        this.changeListeners = new ArrayList<>();
        this.availableFontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        this.currentFont = initialFont;

        this.fontFamilySelector = new JComboBox<>(this.availableFontFamilies);
        this.fontFamilySelector.setSelectedItem(initialFont.getFamily());
        this.fontFamilySelector.setEditable(false);
        //this.fontFamilySelector.setMaximumRowCount(10);
        this.fontFamilySelector.setToolTipText("Select a font family from the list of font families currently installed");
        this.fontFamilySelector.addActionListener(event -> this.dispatchChangeEvent());

        this.boldCheckBox = new JCheckBox("Bold", initialFont.isBold());
        this.boldCheckBox.setToolTipText("If checked, a bold font weight will be applied");
        this.boldCheckBox.addActionListener(event -> this.dispatchChangeEvent());

        this.italicCheckBox = new JCheckBox("Italic", initialFont.isItalic());
        this.italicCheckBox.setToolTipText("If checked, an italic style will be applied");
        this.italicCheckBox.addActionListener(event -> this.dispatchChangeEvent());

        this.fontSizeSelectorModel = new SpinnerNumberModel(initialFont.getSize(), MIN_FONT_SIZE, MAX_FONT_SIZE, 1);
        this.fontSizeSelectorModel.addChangeListener(event -> this.dispatchChangeEvent());
        JSpinner fontSizeSelector = new JSpinner();
        fontSizeSelector.setToolTipText("Select the font size, in points");
        Box fontSizeSelectorBox = Box.createHorizontalBox();
        fontSizeSelectorBox.add(new JLabel("Size:", JLabel.RIGHT));
        fontSizeSelectorBox.add(Box.createHorizontalStrut(6));
        fontSizeSelectorBox.add(fontSizeSelector);

        this.add(this.fontFamilySelector);
        this.add(fontSizeSelectorBox);
        this.add(this.boldCheckBox);
        this.add(this.italicCheckBox);
    }

    public void addChangeListener(ChangeListener listener) {
        this.changeListeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        this.changeListeners.remove(listener);
    }

    public List<ChangeListener> getChangeListeners() {
        return this.changeListeners;
    }

    public void dispatchChangeEvent() {
        this.currentFont = new Font(this.getFontFamily(), this.getFontStyle(), this.getFontSize());
        for (ChangeListener listener : this.changeListeners) {
            listener.stateChanged(new ChangeEvent(this));
        }
    }

    private String getFontFamily() {
        String family = String.valueOf(this.fontFamilySelector.getSelectedItem());
        return List.of(this.availableFontFamilies).contains(family) ? family : null;
    }

    private int getFontStyle() {
        return (this.boldCheckBox.isSelected() ? Font.BOLD : Font.PLAIN)
            | (this.italicCheckBox.isSelected() ? Font.ITALIC : Font.PLAIN);
    }

    private int getFontSize() {
        return this.fontSizeSelectorModel.getNumber().intValue();
    }

    public Font getValue() {
        return this.currentFont;
    }

    public void setValue(Font font) {
        this.fontFamilySelector.setSelectedItem(font.getFamily());
        this.boldCheckBox.setSelected(font.isBold());
        this.italicCheckBox.setSelected(font.isItalic());
        this.fontSizeSelectorModel.setValue(font.getSize());
        this.currentFont = font;
    }
}