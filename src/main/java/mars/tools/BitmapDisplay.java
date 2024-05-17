package mars.tools;

import mars.Application;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.util.Binary;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

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
 * Bitmap display simulator.
 *
 * @author Pete Sanderson, 23 December 2010
 * @version 1.0
 */
public class BitmapDisplay extends AbstractMarsTool {
    private static final String NAME = "Bitmap Display";
    private static final String VERSION = "Version 1.0";

    // Values for Combo Boxes
    private static final Integer[] UNIT_SIZE_CHOICES = { 1, 2, 4, 8, 16, 32 };
    private static final int DEFAULT_UNIT_WIDTH_INDEX = 0;
    private static final int DEFAULT_UNIT_HEIGHT_INDEX = 0;
    private static final Integer[] DISPLAY_SIZE_CHOICES = { 64, 128, 256, 512, 1024, 2048 };
    private static final int DEFAULT_DISPLAY_WIDTH_INDEX = 3;
    private static final int DEFAULT_DISPLAY_HEIGHT_INDEX = 3;

    // The base addresses are lazy-initialized
    private static final String[] BASE_ADDRESS_NAMES = { "global data", "$gp", "static data", "heap", "MMIO" };
    private static final int DEFAULT_BASE_ADDRESS_INDEX = 2; // Static data
    private int[] baseAddresses;
    private String[] baseAddressChoices;

    // Major GUI components
    private JComboBox<Integer> unitWidthSelector;
    private JComboBox<Integer> unitHeightSelector;
    private JComboBox<Integer> displayWidthSelector;
    private JComboBox<Integer> displayHeightSelector;
    private JComboBox<String> baseAddressSelector;
    private BitmapCanvas canvas;

    /**
     * Construct an instance of this tool. This will be used by the {@link mars.venus.ToolManager}.
     */
    @SuppressWarnings("unused")
    public BitmapDisplay() {
        super(NAME + ", " + VERSION);
    }

    /**
     * Required MarsTool method to return Tool name.
     *
     * @return Tool name.  MARS will display this in menu item.
     */
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void stopObserving() {
        this.canvas.stopObserving();
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
        this.baseAddressChoices = new String[BASE_ADDRESS_NAMES.length];
        for (int choice = 0; choice < BASE_ADDRESS_NAMES.length; choice++) {
            this.baseAddressChoices[choice] = Binary.intToHexString(this.baseAddresses[choice]) + " (" + BASE_ADDRESS_NAMES[choice] + ")";
        }
    }

    @Override
    protected JComponent buildContentPane(JComponent mainDisplayArea, JComponent buttonArea) {
        Box contentPane = Box.createVerticalBox();
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contentPane.setOpaque(true);
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
        this.canvas = new BitmapCanvas(
            this.baseAddresses[DEFAULT_BASE_ADDRESS_INDEX],
            UNIT_SIZE_CHOICES[DEFAULT_UNIT_WIDTH_INDEX],
            UNIT_SIZE_CHOICES[DEFAULT_UNIT_HEIGHT_INDEX],
            DISPLAY_SIZE_CHOICES[DEFAULT_DISPLAY_WIDTH_INDEX],
            DISPLAY_SIZE_CHOICES[DEFAULT_DISPLAY_HEIGHT_INDEX]
        );

        Box mainArea = Box.createVerticalBox();
        mainArea.add(new JScrollPane(this.canvas, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        mainArea.add(Box.createVerticalStrut(12));
        mainArea.add(this.buildSettingsArea());
        mainArea.add(Box.createVerticalGlue());
        return mainArea;
    }

    /**
     * Provide a Help button for this tool.
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
        help.putClientProperty("JButton.buttonType", "help");
        help.addActionListener(event -> JOptionPane.showMessageDialog(this.dialog, helpContent));
        return help;
    }

    /**
     * Build the layout for the part of the GUI where the settings are located.
     */
    private JComponent buildSettingsArea() {
        JPanel organization = new JPanel(new GridLayout(5, 1, 6, 6));

        this.unitWidthSelector = new JComboBox<>(UNIT_SIZE_CHOICES);
        this.unitWidthSelector.setEditable(false);
        this.unitWidthSelector.setSelectedIndex(DEFAULT_UNIT_WIDTH_INDEX);
        this.unitWidthSelector.setToolTipText("Width, in pixels, of each rectangle representing a word in memory");
        this.unitWidthSelector.addActionListener(event -> {
            this.canvas.setUnitWidth(UNIT_SIZE_CHOICES[this.unitWidthSelector.getSelectedIndex()]);
        });
        organization.add(this.createSettingsRow("Unit width (px):", this.unitWidthSelector));

        this.unitHeightSelector = new JComboBox<>(UNIT_SIZE_CHOICES);
        this.unitHeightSelector.setEditable(false);
        this.unitHeightSelector.setSelectedIndex(DEFAULT_UNIT_HEIGHT_INDEX);
        this.unitHeightSelector.setToolTipText("Height, in pixels, of each rectangle representing a word in memory");
        this.unitHeightSelector.addActionListener(event -> {
            this.canvas.setUnitHeight(UNIT_SIZE_CHOICES[this.unitHeightSelector.getSelectedIndex()]);
        });
        organization.add(this.createSettingsRow("Unit height (px):", this.unitHeightSelector));

        this.displayWidthSelector = new JComboBox<>(DISPLAY_SIZE_CHOICES);
        this.displayWidthSelector.setEditable(false);
        this.displayWidthSelector.setSelectedIndex(DEFAULT_DISPLAY_WIDTH_INDEX);
        this.displayWidthSelector.setToolTipText("Total width, in pixels, of the bitmap display");
        this.displayWidthSelector.addActionListener(event -> {
            this.canvas.setDisplayWidth(DISPLAY_SIZE_CHOICES[this.displayWidthSelector.getSelectedIndex()]);
            this.ensureCanvasVisible();
        });
        organization.add(this.createSettingsRow("Display width (px):", this.displayWidthSelector));

        this.displayHeightSelector = new JComboBox<>(DISPLAY_SIZE_CHOICES);
        this.displayHeightSelector.setEditable(false);
        this.displayHeightSelector.setSelectedIndex(DEFAULT_DISPLAY_HEIGHT_INDEX);
        this.displayHeightSelector.setToolTipText("Total height, in pixels, of the bitmap display");
        this.displayHeightSelector.addActionListener(event -> {
            this.canvas.setDisplayHeight(DISPLAY_SIZE_CHOICES[this.displayHeightSelector.getSelectedIndex()]);
            this.ensureCanvasVisible();
        });
        organization.add(this.createSettingsRow("Display height (px):", this.displayHeightSelector));

        this.baseAddressSelector = new JComboBox<>(this.baseAddressChoices);
        this.baseAddressSelector.setEditable(false);
        this.baseAddressSelector.setSelectedIndex(DEFAULT_BASE_ADDRESS_INDEX);
        this.baseAddressSelector.setToolTipText("Address of the top-left corner unit of the bitmap");
        this.baseAddressSelector.addActionListener(event -> {
            this.canvas.setFirstAddress(this.baseAddresses[this.baseAddressSelector.getSelectedIndex()]);
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

    private void ensureCanvasVisible() {
        this.dialog.pack();
        this.dialog.setLocationRelativeTo(Application.getGUI());
    }

    /**
     * Custom component class for the central bitmap display canvas.
     */
    private static class BitmapCanvas extends JPanel implements Memory.Listener {
        // Ideally, we could synchronize on the image itself, but it can't be final,
        // so a separate object is used instead.
        private final Object imageLock = new Object();
        private BufferedImage image;

        private int firstAddress;
        private int unitWidth;
        private int unitHeight;
        private int displayWidth;
        private int displayHeight;

        public BitmapCanvas(int firstAddress, int unitWidth, int unitHeight, int displayWidth, int displayHeight) {
            super();

            this.firstAddress = firstAddress;
            this.unitWidth = unitWidth;
            this.unitHeight = unitHeight;
            this.displayWidth = displayWidth;
            this.displayHeight = displayHeight;
            // Ensure the background is drawn correctly
            this.setOpaque(false);
            this.setToolTipText("Live display of the bitmap based on values currently in memory");
            // Set the component size and initialize the image (this also starts listening to memory)
            this.updateDisplaySize();
        }

        public int getBitmapMemorySize() {
            return Memory.BYTES_PER_WORD * this.image.getWidth() * this.image.getHeight();
        }

        public void startObserving() {
            int lastAddress = this.firstAddress + this.getBitmapMemorySize() - 1;
            // If lastAddress < firstAddress, the range probably crosses the unsigned 32-bit integer limit
            if (Integer.compareUnsigned(lastAddress, this.firstAddress) < 0) {
                // Just cap lastAddress at the unsigned 32-bit integer limit
                lastAddress = 0xFFFFFFFF;
            }

            Memory.getInstance().addListener(this, this.firstAddress, lastAddress);

            this.resetImage();
        }

        public void stopObserving() {
            Memory.getInstance().removeListener(this);
        }

        /**
         * When memory pertaining to the bitmap is modified, change the color of the corresponding pixel and update.
         */
        @Override
        public void memoryWritten(int address, int length, int value, int wordAddress, int wordValue) {
            int offset = (wordAddress - this.firstAddress) / Memory.BYTES_PER_WORD;
            synchronized (this.imageLock) {
                if (0 <= offset && offset < this.image.getWidth() * this.image.getHeight()) {
                    this.image.setRGB(offset % this.image.getWidth(), offset / this.image.getWidth(), wordValue);
                }
            }
            // Repaint with the updated image
            this.repaint();
        }

        public void setFirstAddress(int address) {
            this.firstAddress = address;
            // Update the active memory range and reinitialize the image contents
            this.stopObserving();
            this.startObserving();
        }

        public void setUnitWidth(int pixels) {
            this.unitWidth = pixels;
            this.updateImageSize();
        }

        public void setUnitHeight(int pixels) {
            this.unitHeight = pixels;
            this.updateImageSize();
        }

        public void setDisplayWidth(int pixels) {
            this.displayWidth = pixels;
            this.updateDisplaySize();
        }

        public void setDisplayHeight(int pixels) {
            this.displayHeight = pixels;
            this.updateDisplaySize();
        }

        private void updateDisplaySize() {
            Dimension size = new Dimension(this.displayWidth, this.displayHeight);
            // Just, like, make it very clear that we want the canvas to be this size (still not guaranteed!)
            this.setSize(size);
            this.setPreferredSize(size);
            this.setMinimumSize(size);
            this.setMaximumSize(size);

            // Update the component since its size changed
            this.revalidate();
            // The image size depends on the display size, so update that as well
            this.updateImageSize();
        }

        private void updateImageSize() {
            // Regenerate the contents of the image from scratch
            synchronized (this.imageLock) {
                this.image = new BufferedImage(
                    this.displayWidth / this.unitWidth,
                    this.displayHeight / this.unitHeight,
                    BufferedImage.TYPE_INT_RGB
                );
            }
            // Update the active memory range and initialize the image contents
            this.stopObserving();
            this.startObserving();
        }

        private void resetImage() {
            try {
                synchronized (this.imageLock) {
                    int address = this.firstAddress;
                    for (int y = 0; y < this.image.getHeight(); y++) {
                        for (int x = 0; x < this.image.getWidth(); x++) {
                            this.image.setRGB(x, y, Memory.getInstance().getWordNoNotify(address));
                            address += Memory.BYTES_PER_WORD;
                        }
                    }
                }
            }
            catch (AddressErrorException exception) {
                // The base address must not be aligned properly, so keep the image in its default state
            }
            // Repaint the image
            this.repaint();
        }

        @Override
        public void paintComponent(Graphics graphics) {
            // Draw the image in the center of the component bounds
            int x = Math.max(0, this.getWidth() - this.displayWidth) / 2;
            int y = Math.max(0, this.getHeight() - this.displayHeight) / 2;
            synchronized (this.imageLock) {
                graphics.drawImage(this.image, x, y, this.displayWidth, this.displayHeight, null);
            }
        }
    }
}