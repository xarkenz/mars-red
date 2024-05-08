package mars.venus;

import mars.util.EditorFont;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

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
public abstract class AbstractFontSettingDialog extends JDialog {
    protected VenusUI gui;
    JComboBox<String> fontFamilySelector;
    JComboBox<EditorFont.Style> fontStyleSelector;
    JSlider fontSizeSelector;
    JSpinner fontSizeSpinSelector;
    JLabel fontSample;
    protected Font currentFont;
    protected JButton defaultButton;

    // Used to determine upon OK, whether or not anything has changed.
    String initialFontFamily;
    EditorFont.Style initialFontStyle;
    String initialFontSize;

    /**
     * Create a new font chooser.  Has pertinent JDialog parameters.
     * Will do everything except make it visible.
     */
    public AbstractFontSettingDialog(VenusUI gui, String title, boolean modality, Font currentFont) {
        super(gui, title, modality);
        this.gui = gui;
        this.currentFont = currentFont;
        this.defaultButton = null;
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(buildDialogPanel(), BorderLayout.CENTER);
        content.add(buildControlPanel(), BorderLayout.SOUTH);
        this.setContentPane(content);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeDialog();
            }
        });
        this.pack();
        this.setLocationRelativeTo(this.gui);
        if (this.defaultButton != null) {
            this.defaultButton.getRootPane().setDefaultButton(this.defaultButton);
        }
    }

    /**
     * Build the dialog area, not including control buttons at bottom.
     */
    protected JPanel buildDialogPanel() {
        initialFontFamily = currentFont.getFamily();
        initialFontStyle = EditorFont.getStyle(currentFont.getStyle());
        initialFontSize = EditorFont.sizeIntToSizeString(currentFont.getSize());
        String[] commonFontFamilies = EditorFont.getCommonFamilies();
        String[] allFontFamilies = EditorFont.getAllFamilies();
        // The makeVectorData() method will combine these two into one Vector
        // with a horizontal line separating the two groups.
        String[][] fullList = {commonFontFamilies, allFontFamilies};

        fontFamilySelector = new JComboBox<>(flattenWithNullSeparators(fullList));
        fontFamilySelector.setRenderer(new SeparatedComboBoxRenderer());
        fontFamilySelector.addActionListener(new SeparatedComboBoxListener(fontFamilySelector));
        fontFamilySelector.setSelectedItem(currentFont.getFamily());
        fontFamilySelector.setEditable(false);
        fontFamilySelector.setMaximumRowCount(commonFontFamilies.length);
        fontFamilySelector.setToolTipText("Short list of common font families followed by complete list");

        fontStyleSelector = new JComboBox<>(EditorFont.Style.values());
        fontStyleSelector.setSelectedItem(EditorFont.getStyle(currentFont.getStyle()));
        fontStyleSelector.setEditable(false);
        fontStyleSelector.setToolTipText("List of available font styles");

        fontSizeSelector = new JSlider(EditorFont.MIN_SIZE, EditorFont.MAX_SIZE, currentFont.getSize());
        fontSizeSelector.setToolTipText("Use slider to select font size from " + EditorFont.MIN_SIZE + " to " + EditorFont.MAX_SIZE + ".");
        fontSizeSelector.addChangeListener(event -> {
            fontSizeSpinSelector.setValue(((JSlider) event.getSource()).getValue());
            fontSample.setFont(getFont());
        });
        SpinnerNumberModel fontSizeSpinnerModel = new SpinnerNumberModel(currentFont.getSize(), EditorFont.MIN_SIZE, EditorFont.MAX_SIZE, 1);
        fontSizeSpinSelector = new JSpinner(fontSizeSpinnerModel);
        fontSizeSpinSelector.setToolTipText("Current font size in points");
        fontSizeSpinSelector.addChangeListener(event -> {
            fontSizeSelector.setValue((Integer) ((JSpinner) event.getSource()).getValue());
            fontSample.setFont(getFont());
        });
        // Action listener to update sample when family or style selected
        ActionListener updateSample = (event -> fontSample.setFont(getFont()));
        fontFamilySelector.addActionListener(updateSample);
        fontStyleSelector.addActionListener(updateSample);

        JPanel familyStyleComponents = new JPanel(new GridLayout(2, 2, 12, 6));
        familyStyleComponents.add(new JLabel("Font family:"));
        familyStyleComponents.add(new JLabel("Font style:"));
        familyStyleComponents.add(fontFamilySelector);
        familyStyleComponents.add(fontStyleSelector);

        JPanel sizeComponents = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        sizeComponents.add(new JLabel("Font size:"));
        sizeComponents.add(fontSizeSelector);
        sizeComponents.add(fontSizeSpinSelector);

        fontSample = new JLabel("The quick brown fox jumped over the lazy dog.", SwingConstants.CENTER);
        fontSample.setOpaque(true);
        fontSample.setBackground(UIManager.getColor("Venus.Editor.background"));
        fontSample.setFont(getFont());
        fontSample.setToolTipText("Dynamically updated font sample based on current settings");

        Box fontOptionComponents = Box.createVerticalBox();
        fontOptionComponents.add(familyStyleComponents);
        fontOptionComponents.add(Box.createVerticalStrut(12));
        fontOptionComponents.add(sizeComponents);

        JPanel contents = new JPanel();
        contents.setLayout(new BorderLayout(12, 12));
        contents.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contents.add(fontOptionComponents, BorderLayout.NORTH);
        contents.add(fontSample, BorderLayout.CENTER);
        return contents;
    }

    /**
     * Build component containing the buttons for dialog control
     * such as OK, Cancel, Reset, Apply, etc.  These may vary
     * by application.
     */
    protected abstract Component buildControlPanel();

    @Override
    public Font getFont() {
        return EditorFont.createFontFromStringValues(
            String.valueOf(fontFamilySelector.getSelectedItem()),
            String.valueOf(fontStyleSelector.getSelectedItem()),
            fontSizeSpinSelector.getValue().toString()
        );
    }

    /**
     * User has clicked Apply or OK button.
     */
    protected void performApply() {
        apply(this.getFont());
    }

    /**
     * We're finished with this modal dialog.
     */
    protected void closeDialog() {
        this.setVisible(false);
        this.dispose();
    }

    /**
     * Reset font to its initial setting
     */
    protected void reset() {
        fontFamilySelector.setSelectedItem(initialFontFamily);
        fontStyleSelector.setSelectedItem(initialFontStyle);
        fontSizeSelector.setValue(EditorFont.sizeStringToSizeInt(initialFontSize));
        fontSizeSpinSelector.setValue(EditorFont.sizeStringToSizeInt(initialFontSize));
    }

    /**
     * Apply the given font.  Left for the client to define.
     *
     * @param font a font to be applied by the client.
     */
    protected abstract void apply(Font font);

    // Method and two classes to permit one or more horizontal separators
    // within a combo box list.  I obtained this code on 13 July 2007
    // from http://www.codeguru.com/java/articles/164.shtml.  Author
    // is listed: Nobuo Tamemasa.  Code is old, 1999, but fine for this.
    // I will use it to separate the short list of "common" font
    // families from the very long list of all font families.  No attempt
    // to keep a list of recently-used fonts like Word does.  The list
    // of common font families is static.

    /**
     * Given an array of string arrays, will produce a Vector concatenating
     * the arrays with a separator between each. A separator is represented by a null element.
     */
    private static Vector<String> flattenWithNullSeparators(String[][] arrayData) {
        boolean needSeparator = false;
        Vector<String> vectorData = new Vector<>();

        for (String[] subarray : arrayData) {
            if (needSeparator) {
                vectorData.addElement(null);
            }
            for (String string : subarray) {
                vectorData.addElement(string);
            }
            // If the subarray was empty, adding a separator after it would be redundant
            needSeparator = (subarray.length > 0);
        }

        return vectorData;
    }

    /**
     * Renderer to handle separators (represented by null) in a combo box.
     */
    private static class SeparatedComboBoxRenderer extends JLabel implements ListCellRenderer<String> {
        private final JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);

        public SeparatedComboBoxRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        }

        @Override
        public Component getListCellRendererComponent(JList list, String item, int index, boolean isSelected, boolean cellHasFocus) {
            if (item == null) {
                // A null item represents a separator
                return separator;
            }
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setFont(list.getFont());
            setText(item);
            return this;
        }
    }

    /**
     * Listener to handle separators (represented by null) in a combo box.
     */
    private static class SeparatedComboBoxListener implements ActionListener {
        private final JComboBox<String> comboBox;
        private Object lastValidItem;

        public SeparatedComboBoxListener(JComboBox<String> comboBox) {
            this.comboBox = comboBox;
            comboBox.setSelectedIndex(0);
            lastValidItem = comboBox.getSelectedItem();
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            Object selectedItem = comboBox.getSelectedItem();
            if (selectedItem == null) {
                // The user selected a separator, which is not a valid item, so revert to previous value
                comboBox.setSelectedItem(lastValidItem);
            }
            else {
                lastValidItem = selectedItem;
            }
        }
    }
}