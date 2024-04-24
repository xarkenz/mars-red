package mars.venus.actions.help;

import mars.Application;
import mars.assembler.Directive;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.ExtendedInstruction;
import mars.mips.instructions.Instruction;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
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
import java.util.*;

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
 * Action for the Help -> Help menu item
 */
public class HelpHelpAction extends VenusAction {
    public HelpHelpAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
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
        final JDialog dialog = new JDialog(gui, Application.NAME + " " + Application.VERSION + " Help");
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
        dialog.setLocationRelativeTo(gui);
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
            helpDisplay.addHyperlinkListener(new HelpHyperlinkListener());
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
                <tr><td><code>1</code></td><td>condition flag (0 to 7)</td></tr>
                <tr><td><code>10</code></td><td>unsigned 5-bit integer (0 to 31)</td></tr>
                <tr><td><code>-100</code></td><td>signed 16-bit integer (-32768 to 32767)</td></tr>
                <tr><td><code>100</code></td><td>unsigned 16-bit integer (0 to 65535)</td></tr>
                <tr><td><code>100000</code></td><td>signed 32-bit integer (-2147483648 to 2147483647)</td></tr>
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
        tabbedPane.addTab("Basic Instructions", createMipsInstructionHelpPane(BasicInstruction.class));
        tabbedPane.addTab("Extended (Pseudo) Instructions", createMipsInstructionHelpPane(ExtendedInstruction.class));
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
        Directive[] directives = Directive.values();
        Vector<String> exampleList = new Vector<>(directives.length);
        for (Directive directive : directives) {
            String example = directive.toString();
            exampleList.add(example + " ".repeat(Math.max(0, 24 - example.length())) + directive.getDescription());
        }
        Collections.sort(exampleList);
        JList<String> examples = new JList<>(exampleList);
        examples.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return new JScrollPane(examples, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private JScrollPane createMipsInstructionHelpPane(Class<? extends Instruction> instructionClass) {
        ArrayList<Instruction> instructionList = Application.instructionSet.getAllInstructions();
        Vector<String> exampleList = new Vector<>(instructionList.size());
        for (Instruction instruction : instructionList) {
            if (instructionClass.isInstance(instruction)) {
                String example = instruction.getExampleFormat();
                exampleList.add(example + " ".repeat(Math.max(0, 24 - example.length())) + instruction.getDescription());
            }
        }
        Collections.sort(exampleList);
        JList<String> examples = new JList<>(exampleList);
        examples.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return new JScrollPane(examples, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * Determines MARS response when user click on hyperlink in displayed help page.
     * The response will be to pop up a simple dialog with the page contents.  It
     * will not display URL, no navigation, nothing.  Just display the page and
     * provide a Close button.
     */
    private class HelpHyperlinkListener implements HyperlinkListener {
        private static final String CANNOT_DISPLAY_MESSAGE = "<html><title></title><body><strong>Unable to display requested document.</strong></body></html>";

        private JDialog webpageDisplay;
        private JTextField webpageURL;

        @Override
        public void hyperlinkUpdate(HyperlinkEvent event) {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                JEditorPane pane = (JEditorPane) event.getSource();
                if (event instanceof HTMLFrameHyperlinkEvent frameHyperlinkEvent) {
                    HTMLDocument document = (HTMLDocument) pane.getDocument();
                    document.processHTMLFrameHyperlinkEvent(frameHyperlinkEvent);
                }
                else {
                    webpageDisplay = new JDialog(gui, "Primitive HTML Viewer");
                    webpageDisplay.setLayout(new BorderLayout());
                    webpageDisplay.setLocation(gui.getSize().width / 6, gui.getSize().height / 6);
                    JEditorPane webpagePane;
                    try {
                        webpagePane = new JEditorPane(event.getURL());
                    }
                    catch (IOException exception) {
                        webpagePane = new JEditorPane("text/html", CANNOT_DISPLAY_MESSAGE);
                    }
                    webpagePane.addHyperlinkListener(hyperlinkEvent -> {
                        if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            JEditorPane editorPane = (JEditorPane) hyperlinkEvent.getSource();
                            if (hyperlinkEvent instanceof HTMLFrameHyperlinkEvent frameHyperlinkEvent) {
                                HTMLDocument document = (HTMLDocument) editorPane.getDocument();
                                document.processHTMLFrameHyperlinkEvent(frameHyperlinkEvent);
                            }
                            else {
                                try {
                                    editorPane.setPage(hyperlinkEvent.getURL());
                                }
                                catch (Throwable t) {
                                    editorPane.setText(CANNOT_DISPLAY_MESSAGE);
                                }
                                webpageURL.setText(hyperlinkEvent.getURL().toString());
                            }
                        }
                    });
                    webpagePane.setPreferredSize(new Dimension(gui.getSize().width * 2 / 3, gui.getSize().height * 2 / 3));
                    webpagePane.setEditable(false);
                    webpagePane.setCaretPosition(0);
                    JScrollPane webpageScrollPane = new JScrollPane(webpagePane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    webpageURL = new JTextField(event.getURL().toString(), 50);
                    webpageURL.setEditable(false);
                    webpageURL.setBackground(Color.WHITE);
                    JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
                    urlPanel.add(new JLabel("URL: "));
                    urlPanel.add(webpageURL);
                    webpageDisplay.add(urlPanel, BorderLayout.NORTH);
                    webpageDisplay.add(webpageScrollPane);
                    JButton closeButton = new JButton("Close");
                    closeButton.addActionListener(e1 -> {
                        webpageDisplay.setVisible(false);
                        webpageDisplay.dispose();
                    });
                    JPanel closePanel = new JPanel();
                    closePanel.setLayout(new BoxLayout(closePanel, BoxLayout.LINE_AXIS));
                    closePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
                    closePanel.add(Box.createHorizontalGlue());
                    closePanel.add(closeButton);
                    closePanel.add(Box.createHorizontalGlue());
                    webpageDisplay.add(closePanel, BorderLayout.SOUTH);
                    webpageDisplay.pack();
                    webpageDisplay.setVisible(true);
                }
            }
        }
    }
}