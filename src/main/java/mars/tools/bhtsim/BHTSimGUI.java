/*
Copyright (c) 2009,  Ingo Kofler, ITEC, Klagenfurt University, Austria

Developed by Ingo Kofler (ingo.kofler@itec.uni-klu.ac.at)

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

package mars.tools.bhtsim;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * Represents the GUI of the BHT Simulator Tool.
 * <p>
 * The GUI consists mainly of four parts:
 * <ul>
 * <li>A configuration panel to select the number of entries and the history size.
 * <li>A information panel that displays the most recent branch instruction including its address and BHT index.
 * <li>A table representing the BHT with all entries and their internal state and statistics.
 * <li>A log panel that summarizes the predictions in a textual form.
 * </ul>
 *
 * @author Ingo Kofler (ingo.kofler@itec.uni-klu.ac.at)
 */
public class BHTSimGUI extends JPanel {
    /**
     * Constant for the color that highlights the current BHT entry.
     */
    public static final Color COLOR_PREPREDICTION = Color.YELLOW;
    /**
     * Constant for the color to signal a correct prediction.
     */
    public static final Color COLOR_PREDICTION_CORRECT = Color.GREEN;
    /**
     * Constant for the color to signal a misprediction.
     */
    public static final Color COLOR_PREDICTION_INCORRECT = Color.RED;
    /**
     * Constant for the String representing "take the branch."
     */
    public static final String TAKE_BRANCH_STRING = "TAKE";
    /**
     * Constant for the String representing "do not take the branch."
     */
    public static final String NOT_TAKE_BRANCH_STRING = "NOT TAKE";

    /**
     * Text field presenting the most recent branch instruction.
     */
    private JTextField instructionTextField;
    /**
     * Text field representing the address of the most recent branch instruction.
     */
    private JTextField instructionAddressTextField;
    /**
     * Text field representing the resulting BHT index of the branch instruction.
     */
    private JTextField instructionIndexTextField;
    /**
     * Combo box for selecting the number of BHT entries.
     */
    private JComboBox<Integer> entryCountComboBox;
    /**
     * Combo box for selecting the history size.
     */
    private JComboBox<Integer> historySizeComboBox;
    /**
     * Combo box for selecting the initial value.
     */
    private JComboBox<String> initialValueComboBox;
    /**
     * The table representing the BHT.
     */
    private final JTable table;
    /**
     * Text field for log output.
     */
    private JTextArea logTextField;

    /**
     * Creates the GUI components of the BHT Simulator
     * The GUI is a subclass of JPanel which is integrated in the GUI of the MARS tool.
     */
    public BHTSimGUI() {
        this.setLayout(new BorderLayout(10, 10));

        this.table = this.createAndInitializeTable();

        this.add(this.buildConfigPanel(), BorderLayout.NORTH);
        this.add(this.buildInfoPanel(), BorderLayout.WEST);
        this.add(new JScrollPane(this.table), BorderLayout.CENTER);
        this.add(this.buildLogPanel(), BorderLayout.SOUTH);
    }

    /**
     * Creates and initializes the JTable representing the BHT.
     *
     * @return The JTable representing the BHT.
     */
    private JTable createAndInitializeTable() {
        // Create the table
        JTable table = new JTable();

        // Create a default renderer for double values (percentage)
        DefaultTableCellRenderer doubleRenderer = new DefaultTableCellRenderer() {
            private final DecimalFormat formatter = new DecimalFormat("##0.00");

            @Override
            public void setValue(Object value) {
                this.setText((value == null) ? "" : this.formatter.format(value));
            }
        };
        doubleRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // Create a default renderer for all other values with center alignment
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
        defaultRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        table.setDefaultRenderer(Double.class, doubleRenderer);
        table.setDefaultRenderer(Integer.class, defaultRenderer);
        table.setDefaultRenderer(String.class, defaultRenderer);

        table.setSelectionBackground(BHTSimGUI.COLOR_PREPREDICTION);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        return table;
    }

    /**
     * Creates and initializes the panel holding the instruction, address and index text fields.
     *
     * @return The info panel.
     */
    private JPanel buildInfoPanel() {
        this.instructionTextField = new JTextField();
        this.instructionAddressTextField = new JTextField();
        this.instructionIndexTextField = new JTextField();

        this.instructionTextField.setColumns(10);
        this.instructionTextField.setEditable(false);
        this.instructionTextField.setHorizontalAlignment(JTextField.CENTER);
        this.instructionAddressTextField.setColumns(10);
        this.instructionAddressTextField.setEditable(false);
        this.instructionAddressTextField.setHorizontalAlignment(JTextField.CENTER);
        this.instructionIndexTextField.setColumns(10);
        this.instructionIndexTextField.setEditable(false);
        this.instructionIndexTextField.setHorizontalAlignment(JTextField.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.insets = new Insets(5, 5, 2, 5);
        constraints.gridx = 1;
        constraints.gridy = 1;

        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel("Instruction"), constraints);
        constraints.gridy++;
        panel.add(this.instructionTextField, constraints);
        constraints.gridy++;
        panel.add(new JLabel("@ Address"), constraints);
        constraints.gridy++;
        panel.add(this.instructionAddressTextField, constraints);
        constraints.gridy++;
        panel.add(new JLabel("-> Index"), constraints);
        constraints.gridy++;
        panel.add(this.instructionIndexTextField, constraints);

        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.add(panel, BorderLayout.NORTH);
        return outerPanel;
    }

    /**
     * Creates and initializes the panel for the configuration of the tool
     * The panel contains two combo boxes for selecting the number of BHT entries and the history size.
     *
     * @return A panel for the configuration.
     */
    private JPanel buildConfigPanel() {
        Vector<Integer> sizes = new Vector<>();
        sizes.add(8);
        sizes.add(16);
        sizes.add(32);

        Vector<Integer> bits = new Vector<>();
        bits.add(1);
        bits.add(2);

        Vector<String> initialValues = new Vector<>();
        initialValues.add(NOT_TAKE_BRANCH_STRING);
        initialValues.add(TAKE_BRANCH_STRING);

        this.entryCountComboBox = new JComboBox<>(sizes);
        this.historySizeComboBox = new JComboBox<>(bits);
        this.initialValueComboBox = new JComboBox<>(initialValues);

        JPanel panel = new JPanel();
        panel.add(new JLabel("# of BHT entries"));
        panel.add(this.entryCountComboBox);
        panel.add(new JLabel("BHT history size"));
        panel.add(this.historySizeComboBox);
        panel.add(new JLabel("Initial value"));
        panel.add(this.initialValueComboBox);
        return panel;
    }

    /**
     * Creates and initializes the panel containing the log text area.
     *
     * @return The panel for the logging output.
     */
    private JPanel buildLogPanel() {
        this.logTextField = new JTextArea();
        this.logTextField.setRows(6);
        this.logTextField.setEditable(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Log"), BorderLayout.NORTH);
        panel.add(new JScrollPane(this.logTextField), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Returns the combo box for selecting the number of BHT entries.
     *
     * @return The reference to the combo box.
     */
    public JComboBox<Integer> getEntryCountComboBox() {
        return this.entryCountComboBox;
    }

    /**
     * Returns the combo box for selecting the size of the BHT history.
     *
     * @return The reference to the combo box.
     */
    public JComboBox<Integer> getHistorySizeComboBox() {
        return this.historySizeComboBox;
    }

    /**
     * Returns the combo box for selecting the initial value of the BHT
     *
     * @return The reference to the combo box.
     */
    public JComboBox<String> getInitialValueComboBox() {
        return this.initialValueComboBox;
    }

    /**
     * Returns the table representing the BHT.
     *
     * @return The reference to the table.
     */
    public JTable getTable() {
        return this.table;
    }

    /**
     * Returns the text area for logging purposes.
     *
     * @return The reference to the text area.
     */
    public JTextArea getLogTextField() {
        return this.logTextField;
    }

    /**
     * Returns the text field for displaying the most recent branch instruction.
     *
     * @return The reference to the text field.
     */
    public JTextField getInstructionTextField() {
        return this.instructionTextField;
    }

    /**
     * Returns the text field for displaying the address of the most recent branch instruction.
     *
     * @return The reference to the text field.
     */
    public JTextField getInstructionAddressTextField() {
        return this.instructionAddressTextField;
    }

    /**
     * Returns the text field for displaying the corresponding index into the BHT.
     *
     * @return The reference to the text field.
     */
    public JTextField getInstructionIndexTextField() {
        return this.instructionIndexTextField;
    }
}
