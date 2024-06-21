package mars.venus.actions.file;

import mars.mips.dump.DumpFormat;
import mars.mips.dump.DumpFormatManager;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.util.Binary;
import mars.util.MemoryDump;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;
import mars.venus.execute.ProgramStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

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
 * Action for the File -> Dump Memory menu item.
 */
public class FileDumpMemoryAction extends VenusAction {
    private static final String TITLE = "Dump Memory To File";

    private JDialog dumpDialog;
    private JComboBox<String> segmentSelector;
    private JComboBox<DumpFormat> dumpFormatSelector;

    public FileDumpMemoryAction(VenusUI gui, Integer mnemonic, KeyStroke accel) {
        super(gui, "Dump Memory...", VenusUI.getSVGActionIcon("dump_memory.svg"), "Dump machine code or data in an available format", mnemonic, accel);
    }

    /**
     * Save the memory segment in a supported format.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        this.dumpDialog = this.createDumpDialog();
        this.dumpDialog.pack();
        this.dumpDialog.setLocationRelativeTo(this.gui);
        this.dumpDialog.setVisible(true);
    }

    @Override
    public void update() {
        this.setEnabled(this.gui.getProgramStatus() != ProgramStatus.NOT_ASSEMBLED);
    }

    /**
     * Create the dump dialog that appears when menu item is selected.
     */
    private JDialog createDumpDialog() {
        JDialog dumpDialog = new JDialog(this.gui, TITLE, true);
        this.buildDialogContentPane(dumpDialog);
        dumpDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dumpDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                FileDumpMemoryAction.this.closeDialog();
            }
        });
        return dumpDialog;
    }

    /**
     * Set contents of dump dialog.
     */
    private void buildDialogContentPane(JDialog dumpDialog) {
        JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        dumpDialog.setContentPane(contents);

        // A series of parallel arrays representing the memory segments that can be dumped.
        String[] segmentNames = MemoryDump.getSegmentNames();
        int[] baseAddresses = MemoryDump.getBaseAddresses();
        int[] limitAddresses = MemoryDump.getLimitAddresses();
        int[] highAddresses = new int[segmentNames.length];

        // These three are allocated and filled by buildDialogPanel() and used by action listeners.
        String[] segmentLabels = new String[segmentNames.length];
        int[] actualBaseAddresses = new int[segmentNames.length];
        int[] actualHighAddresses = new int[segmentNames.length];

        // Calculate the actual highest address to be dumped.  For text segment, this depends on the
        // program length (number of machine code instructions).  For data segment, this depends on
        // how many MARS 4K word blocks have been referenced during assembly and/or execution.
        // Then generate label from concatenation of segmentNames[segment], baseAddresses[segment]
        // and highAddresses[segment].  This lets user know exactly what range will be dumped.  Initially not
        // editable but maybe add this later.
        // If there is nothing to dump (e.g. address of first null == base address), then
        // the segment will not be listed.
        int segmentCount = 0;

        for (int segment = 0; segment < segmentNames.length; segment++) {
            try {
                highAddresses[segment] = Memory.getInstance().getAddressOfFirstNullWord(baseAddresses[segment], limitAddresses[segment]) - Memory.BYTES_PER_WORD;
            }
            catch (AddressErrorException exception) {
                // Exception will not happen since the Memory base and limit addresses are on word boundaries!
                highAddresses[segment] = baseAddresses[segment] - Memory.BYTES_PER_WORD;
            }
            if (highAddresses[segment] >= baseAddresses[segment]) {
                actualBaseAddresses[segmentCount] = baseAddresses[segment];
                actualHighAddresses[segmentCount] = highAddresses[segment];
                segmentLabels[segmentCount] = segmentNames[segment] + " (" + Binary.intToHexString(baseAddresses[segment]) + " - " + Binary.intToHexString(highAddresses[segment]) + ")";
                segmentCount++;
            }
        }

        // It is highly unlikely that no segments remain after the null check, since
        // there will always be at least one instruction (.text segment has one non-null).
        // But just in case...
        if (segmentCount == 0) {
            contents.add(new Label("There is nothing to dump!"), BorderLayout.NORTH);
            JButton okButton = new JButton("OK");
            okButton.addActionListener(event -> this.closeDialog());
            contents.add(okButton, BorderLayout.SOUTH);
            return;
        }

        // This is needed to assure no null array elements in ComboBox list.
        if (segmentCount < segmentLabels.length) {
            String[] trimmedArray = new String[segmentCount];
            System.arraycopy(segmentLabels, 0, trimmedArray, 0, segmentCount);
            segmentLabels = trimmedArray;
        }

        // Create segment selector.  First element selected by default.
        this.segmentSelector = new JComboBox<>(segmentLabels);
        this.segmentSelector.setSelectedIndex(0);
        JPanel segmentPanel = new JPanel(new BorderLayout());
        segmentPanel.add(new JLabel("Memory Segment"), BorderLayout.NORTH);
        segmentPanel.add(this.segmentSelector);
        contents.add(segmentPanel, BorderLayout.WEST);

        // Next, create list of all available dump formats.
        DumpFormat[] dumpFormats = DumpFormatManager.getDumpFormats();
        this.dumpFormatSelector = new JComboBox<>(dumpFormats);
        this.dumpFormatSelector.setRenderer(new DumpFormatComboBoxRenderer(this.dumpFormatSelector));
        this.dumpFormatSelector.setSelectedIndex(0);
        JPanel formatPanel = new JPanel(new BorderLayout());
        formatPanel.add(new JLabel("Dump Format"), BorderLayout.NORTH);
        formatPanel.add(this.dumpFormatSelector);
        contents.add(formatPanel, BorderLayout.EAST);

        // Bottom row - the control buttons for Next and Cancel
        Box controlPanel = Box.createHorizontalBox();
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(event -> {
            int firstAddress = actualBaseAddresses[this.segmentSelector.getSelectedIndex()];
            int lastAddress = actualHighAddresses[this.segmentSelector.getSelectedIndex()];
            if (performDump(firstAddress, lastAddress, (DumpFormat) this.dumpFormatSelector.getSelectedItem())) {
                this.closeDialog();
            }
        });
        dumpDialog.getRootPane().setDefaultButton(nextButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> this.closeDialog());
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(nextButton);
        controlPanel.add(Box.createHorizontalStrut(12));
        controlPanel.add(cancelButton);
        contents.add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * User has clicked "Next" button, so launch a file chooser then get
     * segment (memory range) and format selections and save to the file.
     */
    private boolean performDump(int firstAddress, int lastAddress, DumpFormat format) {
        File file;
        JFileChooser saveDialog = new JFileChooser(this.gui.getEditor().getCurrentSaveDirectory());
        saveDialog.setDialogTitle(TITLE);
        while (true) {
            int decision = saveDialog.showSaveDialog(this.gui);
            if (decision != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            file = saveDialog.getSelectedFile();
            if (file.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(
                    this.gui,
                    "File \"" + file.getName() + "\" already exists.  Do you wish to overwrite it?",
                    "Overwrite existing file?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                switch (overwrite) {
                    case JOptionPane.YES_OPTION -> {
                        // Pass through to the break statement below
                    }
                    case JOptionPane.NO_OPTION -> {
                        continue;
                    }
                    default -> {
                        return false;
                    }
                }
            }
            // At this point, either the file with the selected name does not exist
            // or the user wanted to overwrite it, so go ahead and save it!
            break;
        }

        try {
            format.dumpMemoryRange(file, firstAddress, lastAddress);
        }
        catch (AddressErrorException | IOException exception) {
            JOptionPane.showMessageDialog(this.gui, exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return true;
    }

    /**
     * We're finished with this modal dialog, so close it.
     */
    private void closeDialog() {
        this.dumpDialog.setVisible(false);
        this.dumpDialog.dispose();
    }

    /**
     * Display tool tip for dump format list items.  Got the technique from
     * <a href="http://forum.java.sun.com/thread.jspa?threadID=488762&messageID=2292482">a forum post</a>.
     */
    private static class DumpFormatComboBoxRenderer extends BasicComboBoxRenderer {
        private final JComboBox<DumpFormat> comboBox;

        public DumpFormatComboBoxRenderer(JComboBox<DumpFormat> comboBox) {
            super();
            this.comboBox = comboBox;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            this.setToolTipText(value.toString());
            if (index >= 0 && this.comboBox.getItemAt(index).getDescription() != null) {
                this.setToolTipText(this.comboBox.getItemAt(index).getDescription());
            }
            return this;
        }
    }
}