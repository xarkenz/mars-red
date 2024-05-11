package mars.tools;

import mars.Application;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.util.Binary;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Observable;

/*
Copyright (c) 2010-2011,  Pete Sanderson and Kenneth Vollmar

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
 * Bitmap display simulator.  It can be run either as a stand-alone Java application having
 * access to the mars package, or through MARS as an item in its Tools menu.  It makes
 * maximum use of methods inherited from its abstract superclass AbstractMarsToolAndApplication.
 *
 * @author Pete Sanderson, 23 December 2010
 * @version 1.0
 */
public class BitmapDisplay extends AbstractMarsToolAndApplication {
    private static final String NAME = "Bitmap Display";
    private static final String VERSION = "Version 1.0";

    // Values for Combo Boxes
    private static final Integer[] UNIT_SIZE_CHOICES = { 1, 2, 4, 8, 16, 32 };
    private static final int DEFAULT_UNIT_WIDTH_INDEX = 0;
    private static final int DEFAULT_UNIT_HEIGHT_INDEX = 0;
    private static final Integer[] BITMAP_SIZE_CHOICES = { 64, 128, 256, 512, 1024, 2048 };
    private static final int DEFAULT_BITMAP_WIDTH_INDEX = 3;
    private static final int DEFAULT_BITMAP_HEIGHT_INDEX = 3;

    // Values for display canvas.  Note their initialization uses the identifiers just above.
    private int unitWidth = UNIT_SIZE_CHOICES[DEFAULT_UNIT_WIDTH_INDEX];
    private int unitHeight = UNIT_SIZE_CHOICES[DEFAULT_UNIT_HEIGHT_INDEX];
    private int bitmapWidth = BITMAP_SIZE_CHOICES[DEFAULT_BITMAP_WIDTH_INDEX];
    private int bitmapHeight = BITMAP_SIZE_CHOICES[DEFAULT_BITMAP_HEIGHT_INDEX];

    // The next four are initialized dynamically in initializeDisplayBaseChoices()
    private static final String[] BASE_ADDRESS_NAMES = { "global data", "$gp", "static data", "heap", "MMIO" };
    private static final int DEFAULT_BASE_ADDRESS_INDEX = 2; // Static data
    private int[] baseAddresses;
    private int baseAddress;
    private String[] baseAddressChoices;

    // Major GUI components
    private JComboBox<Integer> unitWidthSelector;
    private JComboBox<Integer> unitHeightSelector;
    private JComboBox<Integer> bitmapWidthSelector;
    private JComboBox<Integer> bitmapHeightSelector;
    private JComboBox<String> baseAddressSelector;
    private BitmapCanvas canvas;
    private BufferedImage image;

    // Ideally, we could synchronize on the image itself, but it can't be final, so a separate object is used instead.
    private final Object imageLock = new Object();

    /**
     * Main provided for pure stand-alone use.  Recommended stand-alone use is to write a
     * driver program that instantiates a Bitmap object then invokes its go() method.
     * "stand-alone" means it is not invoked from the MARS Tools menu.  "Pure" means there
     * is no driver program to invoke the application.
     */
    public static void main(String[] args) {
        new BitmapDisplay().go();
    }

    /**
     * Simple constructor, used to run a stand-alone bitmap display tool.
     *
     * @param title   String containing title for title bar.
     * @param heading String containing text for heading shown in upper part of window.
     */
    public BitmapDisplay(String title, String heading) {
        super(title, heading);
    }

    /**
     * Simple constructor, used by the MARS Tools menu mechanism.
     */
    public BitmapDisplay() {
        this(NAME + ", " + VERSION, NAME);
    }

    /**
     * Required MarsTool method to return Tool name.
     *
     * @return Tool name.  MARS will display this in menu item.
     */
    @Override
    public String getName() {
        return "Bitmap Display";
    }

    /**
     * Override the inherited method, which registers us as an Observer over the static data segment
     * (starting address 0x10010000) only.  This version will register us as observer over the
     * the memory range as selected by the base address combo box and capacity of the visualization display
     * (number of visualization elements times the number of memory words each one represents).
     * It does so by calling the inherited 2-parameter overload of this method.
     * If you use the inherited GUI buttons, this
     * method is invoked when you click "Connect" button on MarsTool or the
     * "Assemble and Run" button on a Mars-based app.
     */
    @Override
    protected void addAsObserver() {
        int highAddress = this.baseAddress + this.image.getWidth() * this.image.getHeight() * Memory.WORD_LENGTH_BYTES;
        // Special case: baseAddress < 0 means we're in kernel memory (0x80000000 and up) and most likely
        // in memory map address space (0xffff0000 and up).  In this case, we need to make sure the high address
        // does not drop off the high end of 32 bit address space.  Highest allowable word address is 0xfffffffc,
        // which is interpreted in Java int as -4.
        if (this.baseAddress < 0 && highAddress > -4) {
            highAddress = -4;
        }
        this.addAsObserver(this.baseAddress, highAddress);
    }

    @Override
    protected JComponent buildContentPane(JComponent headingArea, JComponent mainDisplayArea, JComponent buttonArea) {
        Box contentPane = Box.createVerticalBox();
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contentPane.setOpaque(true);
        contentPane.add(headingArea);
        contentPane.add(Box.createVerticalStrut(12));
        contentPane.add(mainDisplayArea);
        contentPane.add(Box.createVerticalStrut(12));
        contentPane.add(buttonArea);
        return contentPane;
    }

    /**
     * Method that constructs the main display area.  It is organized vertically
     * into two major components: the display configuration which an be modified
     * using combo boxes, and the visualization display which is updated as the
     * attached MIPS program executes.
     *
     * @return the GUI component containing these two areas.
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        this.canvas = new BitmapCanvas();

        Box mainArea = Box.createVerticalBox();
        mainArea.add(new JScrollPane(this.canvas, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        mainArea.add(Box.createVerticalStrut(12));
        mainArea.add(this.buildSettingsArea());
        mainArea.add(Box.createVerticalGlue());
        return mainArea;
    }

    /**
     * Update display when connected MIPS program accesses (data) memory.
     *
     * @param memory       The attached memory.
     * @param accessNotice Information provided by memory in MemoryAccessNotice object.
     */
    @Override
    protected void processMIPSUpdate(Observable memory, AccessNotice accessNotice) {
        if (accessNotice.getAccessType() == AccessNotice.WRITE) {
            MemoryAccessNotice notice = (MemoryAccessNotice) accessNotice;
            int address = notice.getAddress();
            int value = notice.getValue();
            int offset = (address - this.baseAddress) / Memory.WORD_LENGTH_BYTES;
            synchronized (BitmapDisplay.this.imageLock) {
                if (0 <= offset && offset < this.image.getWidth() * this.image.getHeight()) {
                    this.image.setRGB(offset % this.image.getWidth(), offset / this.image.getWidth(), value);
                }
            }
        }
    }

    /**
     * Initialize all JComboBox choice structures not already initialized at declaration.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void initializePreGUI() {
        this.baseAddresses = new int[] {
            Memory.dataSegmentBaseAddress,
            Memory.globalPointer,
            Memory.dataBaseAddress,
            Memory.heapBaseAddress,
            Memory.mmioBaseAddress,
        };
        this.baseAddress = this.baseAddresses[DEFAULT_BASE_ADDRESS_INDEX];
        this.baseAddressChoices = new String[BASE_ADDRESS_NAMES.length];
        for (int choice = 0; choice < BASE_ADDRESS_NAMES.length; choice++) {
            this.baseAddressChoices[choice] = Binary.intToHexString(this.baseAddresses[choice]) + " (" + BASE_ADDRESS_NAMES[choice] + ")";
        }
    }

    /**
     * The post-GUI initialization updates the canvas size, which will also handle generating the initial image.
     */
    @Override
    protected void initializePostGUI() {
        this.canvas.updateSize();
    }

    /**
     * Method to reset counters and display when the Reset button selected.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void reset() {
        this.updateDisplay();
    }

    /**
     * Updates display immediately after each update (AccessNotice) is processed, after
     * display configuration changes as needed, and after each execution step when Mars
     * is running in timed mode.  Overrides inherited method that does nothing.
     */
    @Override
    protected void updateDisplay() {
        this.canvas.repaint();
    }

    /**
     * Provide a Help button for this tool/app.
     */
    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = """
            Use this program to simulate a basic bitmap display where
            each memory word in a specified address space corresponds to
            one display pixel in row-major order starting at the upper left
            corner of the display.  This tool may be run either from the
            MARS Tools menu or as a stand-alone application.

            You can easily learn to use this small program by playing with
            it!  Each rectangular unit on the display represents one memory
            word in a contiguous address space starting with the specified
            base address.  The value stored in that word will be interpreted
            as a 24-bit RGB color value with the red component in bits 16-23,
            the green component in bits 8-15, and the blue component in bits 0-7.
            Each time a memory word within the display address space is written
            by the MIPS program, its position in the display will be rendered
            in the color that its value represents.

            Version 1.0 is very basic and was constructed from the Memory
            Reference Visualization tool's code.  Feel free to improve it and
            send me your code for consideration in the next MARS release.

            Contact Pete Sanderson at psanderson@otterbein.edu with
            questions or comments.
            """;
        JButton help = new JButton("Help");
        help.addActionListener(event -> JOptionPane.showMessageDialog(this.window, helpContent));
        return help;
    }

    /**
     * UI components and layout for left half of GUI, where the settings are located.
     */
    private JComponent buildSettingsArea() {
        JPanel organization = new JPanel(new GridLayout(5, 1, 6, 6));

        this.unitWidthSelector = new JComboBox<>(UNIT_SIZE_CHOICES);
        this.unitWidthSelector.setEditable(false);
        this.unitWidthSelector.setSelectedIndex(DEFAULT_UNIT_WIDTH_INDEX);
        this.unitWidthSelector.setToolTipText("Width, in pixels, of each rectangle representing a word in memory");
        this.unitWidthSelector.addActionListener(event -> {
            this.unitWidth = UNIT_SIZE_CHOICES[this.unitWidthSelector.getSelectedIndex()];
            this.canvas.resetImage();
        });
        organization.add(this.createSettingsRow("Unit width (px):", this.unitWidthSelector));

        this.unitHeightSelector = new JComboBox<>(UNIT_SIZE_CHOICES);
        this.unitHeightSelector.setEditable(false);
        this.unitHeightSelector.setSelectedIndex(DEFAULT_UNIT_HEIGHT_INDEX);
        this.unitHeightSelector.setToolTipText("Height, in pixels, of each rectangle representing a word in memory");
        this.unitHeightSelector.addActionListener(event -> {
            this.unitHeight = UNIT_SIZE_CHOICES[this.unitHeightSelector.getSelectedIndex()];
            this.canvas.resetImage();
        });
        organization.add(this.createSettingsRow("Unit height (px):", this.unitHeightSelector));

        this.bitmapWidthSelector = new JComboBox<>(BITMAP_SIZE_CHOICES);
        this.bitmapWidthSelector.setEditable(false);
        this.bitmapWidthSelector.setSelectedIndex(DEFAULT_BITMAP_WIDTH_INDEX);
        this.bitmapWidthSelector.setToolTipText("Total width, in pixels, of the displayed bitmap");
        this.bitmapWidthSelector.addActionListener(event -> {
            this.bitmapWidth = BITMAP_SIZE_CHOICES[this.bitmapWidthSelector.getSelectedIndex()];
            this.canvas.updateSize();
        });
        organization.add(this.createSettingsRow("Bitmap width (px):", this.bitmapWidthSelector));

        this.bitmapHeightSelector = new JComboBox<>(BITMAP_SIZE_CHOICES);
        this.bitmapHeightSelector.setEditable(false);
        this.bitmapHeightSelector.setSelectedIndex(DEFAULT_BITMAP_HEIGHT_INDEX);
        this.bitmapHeightSelector.setToolTipText("Total height, in pixels, of the displayed bitmap");
        this.bitmapHeightSelector.addActionListener(event -> {
            this.bitmapHeight = BITMAP_SIZE_CHOICES[this.bitmapHeightSelector.getSelectedIndex()];
            this.canvas.updateSize();
        });
        organization.add(this.createSettingsRow("Bitmap height (px):", this.bitmapHeightSelector));

        this.baseAddressSelector = new JComboBox<>(this.baseAddressChoices);
        this.baseAddressSelector.setEditable(false);
        this.baseAddressSelector.setSelectedIndex(DEFAULT_BASE_ADDRESS_INDEX);
        this.baseAddressSelector.setToolTipText("Address of the top-left corner pixel of the bitmap");
        this.baseAddressSelector.addActionListener(event -> {
            // This may also affect what address range we should be registered as an Observer
            // for.  The default (inherited) address range is the MIPS static data segment
            // starting at 0x10010000. To change this requires override of
            // AbstractMarsToolAndApplication.addAsObserver().  The no-argument version of
            // that method is called automatically  when "Connect" button is clicked for MarsTool
            // and when "Assemble and Run" button is clicked for Mars application.
            this.baseAddress = this.baseAddresses[this.baseAddressSelector.getSelectedIndex()];
            this.canvas.resetImage();
        });
        organization.add(this.createSettingsRow("Base memory address:", this.baseAddressSelector));

        return organization;
    }

    private JComponent createSettingsRow(String label, JComponent field) {
        Box settingsRow = Box.createHorizontalBox();
        settingsRow.add(new JLabel(label));
        settingsRow.add(Box.createHorizontalGlue());
        settingsRow.add(field);
        return settingsRow;
    }

    /**
     * Custom component class for the central bitmap display canvas.
     */
    private class BitmapCanvas extends JPanel {
        public BitmapCanvas() {
            // Ensure the background is drawn correctly
            this.setOpaque(false);
            this.setToolTipText("Live display of the bitmap based on values currently in memory");
            this.resetImage();
        }

        @Override
        public void paintComponent(Graphics graphics) {
            // Draw the image in the center of the component bounds
            int x = Math.max(0, this.getWidth() - BitmapDisplay.this.bitmapWidth) / 2;
            int y = Math.max(0, this.getHeight() - BitmapDisplay.this.bitmapHeight) / 2;
            synchronized (BitmapDisplay.this.imageLock) {
                graphics.drawImage(BitmapDisplay.this.image, x, y, BitmapDisplay.this.bitmapWidth, BitmapDisplay.this.bitmapHeight, null);
            }
        }

        public void updateSize() {
            Dimension size = new Dimension(BitmapDisplay.this.bitmapWidth, BitmapDisplay.this.bitmapHeight);
            // Just, like, make it very clear that we want the canvas to be this size
            this.setSize(size);
            this.setPreferredSize(size);
            this.setMinimumSize(size);
            this.setMaximumSize(size);

            // Reset the image since the size has changed
            this.resetImage();

            // Reshape the window to fit the new size
            BitmapDisplay.this.window.pack();
            BitmapDisplay.this.window.setLocationRelativeTo(Application.getGUI());
        }

        public void resetImage() {
            // If something has changed, we can delete and add ourselves as an observer to refresh the memory range
            if (BitmapDisplay.this.connectButton != null && BitmapDisplay.this.connectButton.isConnected()) {
                BitmapDisplay.this.deleteAsObserver();
                BitmapDisplay.this.addAsObserver();
            }

            synchronized (BitmapDisplay.this.imageLock) {
                // Regenerate the contents of the image from scratch
                BitmapDisplay.this.image = new BufferedImage(
                    BitmapDisplay.this.bitmapWidth / BitmapDisplay.this.unitWidth,
                    BitmapDisplay.this.bitmapHeight / BitmapDisplay.this.unitHeight,
                    BufferedImage.TYPE_INT_RGB
                );
                try {
                    int offset = 0;
                    for (int y = 0; y < BitmapDisplay.this.image.getHeight(); y++) {
                        for (int x = 0; x < BitmapDisplay.this.image.getWidth(); x++) {
                            BitmapDisplay.this.image.setRGB(x, y, Memory.getInstance().getWordNoNotify(BitmapDisplay.this.baseAddress + offset));
                            offset += Memory.WORD_LENGTH_BYTES;
                        }
                    }
                }
                catch (AddressErrorException exception) {
                    // The base address must not be aligned properly, so keep the image in its default state
                }
            }

            // Update the canvas component, which will repaint the image
            this.revalidate();
        }
    }
}