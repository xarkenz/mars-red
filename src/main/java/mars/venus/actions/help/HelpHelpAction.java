package mars.venus.actions.help;

import mars.Application;
import mars.assembler.Directive;
import mars.mips.instructions.Instruction;
import mars.venus.StaticTableModel;
import mars.venus.VenusUI;
import mars.venus.WrappingCellRenderer;
import mars.venus.actions.VenusAction;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Help -> Help menu item.
 */
public class HelpHelpAction extends VenusAction {
    public HelpHelpAction(VenusUI gui, Integer mnemonic, KeyStroke accel) {
        super(gui, "Help...", VenusUI.getSVGActionIcon("help.svg"), "View help information", mnemonic, accel);
    }

    // Ideally read or computed from config file...
    private Dimension getSize() {
        return new Dimension(800, 600);
    }

    /**
     * Separates Instruction name descriptor from detailed (operation) description
     * in help string.
     */
    public static final String DESCRIPTION_DETAIL_SEPARATOR = ":";

    /**
     * Displays tabs with categories of information.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("MIPS", createMipsHelpInfoPanel());
        tabbedPane.addTab("MARS", createMarsHelpInfoPanel());
        tabbedPane.addTab("License", createLicensePanel());
        tabbedPane.addTab("Bugs/Comments", createHTMLHelpPanel("BugReportingHelp.html"));
        tabbedPane.addTab("Acknowledgements", createHTMLHelpPanel("Acknowledgements.html"));
        tabbedPane.addTab("Instruction Set Song", createHTMLHelpPanel("MIPSInstructionSetSong.html"));
        // Create non-modal dialog. Based on java.sun.com "How to Make Dialogs", DialogDemo.java
        final JDialog dialog = new JDialog(this.gui, Application.NAME + " " + Application.VERSION + " Help");
        // Ensure the dialog goes away if user clicks the X
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(closeEvent -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
        JPanel closePanel = new JPanel();
        closePanel.setLayout(new BoxLayout(closePanel, BoxLayout.LINE_AXIS));
        closePanel.add(Box.createHorizontalGlue());
        closePanel.add(closeButton);
        closePanel.add(Box.createHorizontalGlue());
        closePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.add(tabbedPane);
        contentPane.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPane.add(closePanel);
        contentPane.setOpaque(true);

        // Show the dialog
        dialog.setContentPane(contentPane);
        dialog.setSize(this.getSize());
        dialog.setLocationRelativeTo(this.gui);
        dialog.setVisible(true);
    }

    /**
     * Create panel containing Help Info read from html document.
     */
    private JPanel createHTMLHelpPanel(String filename) {
        JPanel helpPanel = new JPanel(new BorderLayout());
        JScrollPane helpScrollPane;
        JEditorPane helpDisplay;
        try {
            InputStream input = this.getClass().getResourceAsStream(Application.HELP_PATH + filename);
            assert input != null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            StringBuilder text = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
            reader.close();
            helpDisplay = new JEditorPane("text/html", text.toString());
            helpDisplay.setEditable(false);
            helpDisplay.setCaretPosition(0); // Assure top of document displayed
            helpScrollPane = new JScrollPane(helpDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            helpDisplay.addHyperlinkListener(event -> {
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    JEditorPane pane = (JEditorPane) event.getSource();
                    if (event instanceof HTMLFrameHyperlinkEvent frameHyperlinkEvent) {
                        HTMLDocument document = (HTMLDocument) pane.getDocument();
                        document.processHTMLFrameHyperlinkEvent(frameHyperlinkEvent);
                    }
                    else {
                        try {
                            Desktop.getDesktop().browse(event.getURL().toURI());
                        }
                        catch (IOException | URISyntaxException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                }
            });
        }
        catch (Exception exception) {
            helpScrollPane = new JScrollPane(new JLabel("Error (" + exception + "): " + filename + " contents could not be loaded."));
        }
        helpPanel.add(helpScrollPane);
        return helpPanel;
    }

    /**
     * Set up the license notice for display.
     */
    private JPanel createLicensePanel() {
        JPanel licensePanel = new JPanel(new BorderLayout());
        JScrollPane licenseScrollPane;
        JEditorPane licenseDisplay;
        try {
            InputStream input = this.getClass().getResourceAsStream("/mars_license.txt");
            assert input != null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            StringBuilder text = new StringBuilder("<pre>");
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
            reader.close();
            text.append("</pre>");
            licenseDisplay = new JEditorPane("text/html", text.toString());
            licenseDisplay.setEditable(false);
            licenseDisplay.setCaretPosition(0); // Assure top of document displayed
            licenseScrollPane = new JScrollPane(licenseDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }
        catch (Exception exception) {
            licenseScrollPane = new JScrollPane(new JLabel("Error: license contents could not be loaded.", JLabel.CENTER));
        }
        licensePanel.add(licenseScrollPane);
        return licensePanel;
    }

    /**
     * Set up MARS help tab.  Subtabs get their contents from HTML files.
     */
    private JPanel createMarsHelpInfoPanel() {
        JPanel marsHelpInfo = new JPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Intro", createHTMLHelpPanel("MarsHelpIntro.html"));
        tabbedPane.addTab("IDE", createHTMLHelpPanel("MarsHelpIDE.html"));
        tabbedPane.addTab("Debugging", createHTMLHelpPanel("MarsHelpDebugging.html"));
        tabbedPane.addTab("Settings", createHTMLHelpPanel("MarsHelpSettings.html"));
        tabbedPane.addTab("Tools", createHTMLHelpPanel("MarsHelpTools.html"));
        tabbedPane.addTab("Command", createHTMLHelpPanel("MarsHelpCommand.html"));
        tabbedPane.addTab("Limits", createHTMLHelpPanel("MarsHelpLimits.html"));
        tabbedPane.addTab("History", createHTMLHelpPanel("MarsHelpHistory.html"));
        marsHelpInfo.add(tabbedPane);
        return marsHelpInfo;
    }

    /**
     * Set up MIPS help tab.  Most contents are generated from instruction set info.
     */
    private JPanel createMipsHelpInfoPanel() {
        JPanel mipsHelpInfoPanel = new JPanel(new BorderLayout());
        // Introductory remarks go at the top as a label
        String helpRemarks = """
            <html><center><table border=0 cellpadding=0>
                <tr><th colspan=2><b><i><font size=+1>&nbsp;&nbsp;Operand Key for Example Instructions&nbsp;&nbsp;</font></i></b></th></tr>
                <tr><td><code>label, target</code></td><td>any textual label</td></tr>
                <tr><td><code>$t1, $t2, $t3</code></td><td>any integer register</td></tr>
                <tr><td><code>$f2, $f4, $f6</code></td><td><i>even-numbered</i> floating point register</td></tr>
                <tr><td><code>$f0, $f1, $f3</code></td><td><i>any</i> floating point register</td></tr>
                <tr><td><code>$8</code></td><td>any Coprocessor 0 register</td></tr>
                <tr><td><code>1</code></td><td>unsigned 3-bit integer (0 to 7)</td></tr>
                <tr><td><code>10</code></td><td>unsigned 5-bit integer (0 to 31)</td></tr>
                <tr><td><code>-100</code></td><td>signed 16-bit integer (-32768 to 32767)</td></tr>
                <tr><td><code>100</code></td><td>unsigned 16-bit integer (0 to 65535)</td></tr>
                <tr><td><code>100000</code></td><td>any 32-bit integer (-2147483648 to 4294967295)</td></tr>
                <tr></tr>
                <tr><td colspan=2><b><i><font size=+1>Load & Store addressing mode, basic instructions</font></i></b></td></tr>
                <tr><td><code>-100($t2)</code></td><td>sign-extended 16-bit integer added to contents of $t2</td></tr>
                <tr></tr>
                <tr><td colspan=2><b><i><font size=+1>Load & Store addressing modes, pseudo instructions</font></i></b></td></tr>
                <tr><td><code>($t2)</code></td><td>contents of $t2</td></tr>
                <tr><td><code>-100</code></td><td>signed 16-bit integer</td></tr>
                <tr><td><code>100</code></td><td>unsigned 16-bit integer</td></tr>
                <tr><td><code>100000</code></td><td>signed 32-bit integer</td></tr>
                <tr><td><code>100($t2)</code></td><td>zero-extended unsigned 16-bit integer added to contents of $t2</td></tr>
                <tr><td><code>100000($t2)</code></td><td>signed 32-bit integer added to contents of $t2</td></tr>
                <tr><td><code>label</code></td><td>32-bit address of label</td></tr>
                <tr><td><code>label($t2)</code></td><td>32-bit address of label added to contents of $t2</td></tr>
                <tr><td><code>label+100000</code></td><td>32-bit integer added to label's address</td></tr>
                <tr><td><code>label+100000($t2)&nbsp;&nbsp;&nbsp;</code></td><td>sum of 32-bit integer, label's address, and contents of $t2</td></tr>
            </table></center></html>
            """;
        JLabel helpRemarksLabel = new JLabel(helpRemarks, JLabel.CENTER);
        helpRemarksLabel.setOpaque(true);
        JScrollPane operandsScrollPane = new JScrollPane(helpRemarksLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        mipsHelpInfoPanel.add(operandsScrollPane, BorderLayout.NORTH);
        // Below the label is a tabbed pane with categories of MIPS help
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Basic Instructions", createMipsInstructionHelpPane(Application.instructionSet.getBasicInstructions()));
        tabbedPane.addTab("Extended (Pseudo) Instructions", createMipsInstructionHelpPane(Application.instructionSet.getExtendedInstructions()));
        tabbedPane.addTab("Directives", createMipsDirectivesHelpPane());
        tabbedPane.addTab("Syscalls", createHTMLHelpPanel("SyscallHelp.html"));
        tabbedPane.addTab("Exceptions", createHTMLHelpPanel("ExceptionsHelp.html"));
        tabbedPane.addTab("Macros", createHTMLHelpPanel("MacrosHelp.html"));
        operandsScrollPane.setPreferredSize(new Dimension((int) this.getSize().getWidth(), (int) (this.getSize().getHeight() * .2)));
        operandsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        tabbedPane.setPreferredSize(new Dimension((int) this.getSize().getWidth(), (int) (this.getSize().getHeight() * .6)));
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, operandsScrollPane, tabbedPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.resetToPreferredSizes();
        mipsHelpInfoPanel.add(splitPane);
        return mipsHelpInfoPanel;
    }

    private JScrollPane createMipsDirectivesHelpPane() {
        String[] columnNames = { "Name", "Description" };
        Object[][] rowData = new Object[Directive.values().length][2];
        int row = 0;
        for (Directive directive : Directive.values()) {
            rowData[row][0] = directive.getName();
            rowData[row][1] = directive.getDescription();
            row++;
        }

        JTable table = new JTable(new StaticTableModel(rowData, columnNames));
        table.setFont(this.gui.getSettings().tableFont.get());
        table.setCellSelectionEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(500);

        WrappingCellRenderer renderer = new WrappingCellRenderer(1.25f);
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }

        return new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    private JScrollPane createMipsInstructionHelpPane(List<? extends Instruction> instructions) {
        String[] columnNames = { "Mnemonic", "Operands", "Description" };
        Object[][] rowData = new Object[instructions.size()][3];
        int row = 0;
        for (Instruction instruction : instructions) {
            rowData[row][0] = instruction.getMnemonic() + "\n" + instruction.getTitle();
            rowData[row][1] = Instruction.formatOperands(instruction.getOperandTypes(), instruction.getExampleOperands());
            rowData[row][2] = instruction.getDescription();
            row++;
        }

        JTable table = new JTable(new StaticTableModel(rowData, columnNames));
        table.setFont(this.gui.getSettings().tableFont.get());
        table.setCellSelectionEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);

        WrappingCellRenderer renderer = new WrappingCellRenderer(1.25f);
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }

        return new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
}