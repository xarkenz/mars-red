package mars.tools;

import mars.Application;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryConfigurations;
import mars.mips.hardware.Processor;
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
 * @version 1.1
 */
public class BitmapDisplay extends AbstractMarsTool {
    public static final int BASE_ADDRESS_KEY = 1;
    public static final int BITMAP_WIDTH_KEY = 2;
    public static final int BITMAP_HEIGHT_KEY = 3;
    public static final int HORIZONTAL_SCALE_KEY = 4;
    public static final int VERTICAL_SCALE_KEY = 5;
    // The base addresses are lazy-initialized
    private static final String[] BASE_ADDRESS_NAMES = { "Custom", "Global data", "Default $gp", "Static data", "Heap", "MMIO" };
    private int[] baseAddressChoices;
    private String[] baseAddressNames;

    // Major GUI components
    private JComboBox<String> baseAddressSelector;
    private JSpinner bitmapWidthSelector;
    private JSpinner bitmapHeightSelector;
    private JSpinner horizontalScaleSelector;
    private JSpinner verticalScaleSelector;
    private BitmapCanvas canvas;

    private int baseAddressIndex = 3; // Static data
    private int baseAddress;
    private int bitmapWidth = 64;
    private int bitmapHeight = 64;
    private int horizontalScale = 8;
    private int verticalScale = 8;

    /**
     * Construct an instance of this tool. This will be used by the {@link mars.venus.ToolManager}.
     */
    @SuppressWarnings("unused")
    public BitmapDisplay() {
        this.addNotifyHandler(BASE_ADDRESS_KEY, () -> this.setBaseAddress(Processor.getValue(Processor.ARGUMENT_2)));
        this.addNotifyHandler(BITMAP_WIDTH_KEY, () -> this.setBitmapWidth(Processor.getValue(Processor.ARGUMENT_2)));
        this.addNotifyHandler(BITMAP_HEIGHT_KEY, () -> this.setBitmapHeight(Processor.getValue(Processor.ARGUMENT_2)));
        this.addNotifyHandler(HORIZONTAL_SCALE_KEY, () -> this.setHorizontalScale(Processor.getValue(Processor.ARGUMENT_2)));
        this.addNotifyHandler(VERTICAL_SCALE_KEY, () -> this.setVerticalScale(Processor.getValue(Processor.ARGUMENT_2)));

        this.addQueryHandler(BASE_ADDRESS_KEY, () -> Processor.setValue(Processor.VALUE_0, this.baseAddress));
        this.addQueryHandler(BITMAP_WIDTH_KEY, () -> Processor.setValue(Processor.VALUE_0, this.bitmapWidth));
        this.addQueryHandler(BITMAP_HEIGHT_KEY, () -> Processor.setValue(Processor.VALUE_0, this.bitmapHeight));
        this.addQueryHandler(HORIZONTAL_SCALE_KEY, () -> Processor.setValue(Processor.VALUE_0, this.horizontalScale));
        this.addQueryHandler(VERTICAL_SCALE_KEY, () -> Processor.setValue(Processor.VALUE_0, this.verticalScale));
    }

    @Override
    public String getIdentifier() {
        return "bitmap";
    }

    /**
     * Required MarsTool method to return Tool name.
     *
     * @return Tool name.  MARS will display this in menu item.
     */
    @Override
    public String getDisplayName() {
        return "Bitmap Display";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    public void setBaseAddress(int baseAddress) {
        if (baseAddress == this.baseAddress) {
            return;
        }
        this.validateAddress(baseAddress);
        this.baseAddress = baseAddress;
        this.baseAddressIndex = 0;
        if (this.canvas != null) {
            this.canvas.setFirstAddress(baseAddress);
            this.baseAddressChoices[0] = baseAddress;
            this.baseAddressNames[0] = BASE_ADDRESS_NAMES[0] + " (" + Binary.intToHexString(baseAddress) + ")";
            this.baseAddressSelector.setSelectedIndex(0);
        }
    }

    public void setBitmapWidth(int bitmapWidth) {
        if (bitmapWidth == this.bitmapWidth) {
            return;
        }
        this.validateBitmapSize(bitmapWidth);
        this.bitmapWidth = bitmapWidth;
        if (this.canvas != null) {
            this.canvas.setBitmapWidth(bitmapWidth);
            this.bitmapWidthSelector.setValue(bitmapWidth);
        }
    }

    public void setBitmapHeight(int bitmapHeight) {
        if (bitmapHeight == this.bitmapHeight) {
            return;
        }
        this.validateBitmapSize(bitmapHeight);
        this.bitmapHeight = bitmapHeight;
        if (this.canvas != null) {
            this.canvas.setBitmapHeight(bitmapHeight);
            this.bitmapHeightSelector.setValue(bitmapHeight);
        }
    }

    public void setHorizontalScale(int horizontalScale) {
        if (horizontalScale == this.horizontalScale) {
            return;
        }
        this.validateScale(horizontalScale);
        this.horizontalScale = horizontalScale;
        if (this.canvas != null) {
            this.canvas.setHorizontalScale(horizontalScale);
            this.horizontalScaleSelector.setValue(horizontalScale);
        }
    }

    public void setVerticalScale(int verticalScale) {
        if (verticalScale == this.verticalScale) {
            return;
        }
        this.validateScale(verticalScale);
        this.verticalScale = verticalScale;
        if (this.canvas != null) {
            this.canvas.setVerticalScale(verticalScale);
            this.verticalScaleSelector.setValue(verticalScale);
        }
    }

    @Override
    protected void stopObserving() {
        this.canvas.stopObserving();
    }

    @Override
    protected void reset() {
        this.canvas.resetImage();
    }

    /**
     * Initialize all JComboBox choice structures not already initialized at declaration.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void initializePreGUI() {
        this.baseAddressChoices = new int[] {
            this.baseAddress,
            Memory.getInstance().getAddress(MemoryConfigurations.DATA_LOW),
            Memory.getInstance().getAddress(MemoryConfigurations.GLOBAL_POINTER),
            Memory.getInstance().getAddress(MemoryConfigurations.STATIC_LOW),
            Memory.getInstance().getAddress(MemoryConfigurations.DYNAMIC_LOW),
            Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW),
        };
        if (this.baseAddressIndex != 0) {
            this.baseAddress = this.baseAddressChoices[this.baseAddressIndex];
            this.baseAddressChoices[0] = this.baseAddress;
        }
        this.baseAddressNames = new String[BASE_ADDRESS_NAMES.length];
        for (int choice = 0; choice < BASE_ADDRESS_NAMES.length; choice++) {
            this.baseAddressNames[choice] = BASE_ADDRESS_NAMES[choice] + " (" + Binary.intToHexString(this.baseAddressChoices[choice]) + ")";
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
            this.baseAddress,
            this.bitmapWidth,
            this.bitmapHeight,
            this.horizontalScale,
            this.verticalScale
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
            This tool interprets a region of memory as a bitmap image, where each 32-bit word represents a single \
            pixel. The display is updated in real-time as values in memory change.

            The first word of the region corresponds to the upper-left corner of the bitmap, and the pixel data is \
            obtained in a row-major format (that is, the rows of pixels in the bitmap are laid end-to-end in memory). \
            In other words, the bitmap is read left to right, then top to bottom.
            
            Each word value in the memory region is interpreted as a 24-bit RGB color in the form 0xRRGGBB \
            (bits 23-16 for red, bits 15-8 for green, bits 7-0 for blue). Bits 31-24 are currently ignored. \
            Note that the whole word is read for each pixel, not the individual bytes. As such, the preferred method \
            for reading and writing pixel data is loading and storing the entire word, using bitwise operations \
            to modify specific components. Loading and storing the bytes for each color component will work, but \
            doing so makes the behavior of the program reliant on the current endianness (i.e. byte ordering) setting.
            
            To automate this tool with syscalls, the following notify/query keys can be used. \
            (Notify syscalls retrieve their value from $a2, and query syscalls place their result in $v0.)
            - Base address (1): The starting address for the memory region to display.
            - Bitmap width (2): The number of pixels / words in each row of the bitmap.
            - Bitmap height (3): The number of rows in the bitmap.
            - Horizontal scale (4): Factor to scale the displayed image by in the X direction.
            - Vertical scale (5): Factor to scale the displayed image by in the Y direction.
            """;
        JButton help = new JButton("Help");
        help.putClientProperty("JButton.buttonType", "help");
        help.addActionListener(event -> JOptionPane.showMessageDialog(
            this.dialog,
            helpContent,
            this.getDisplayName() + " - Help",
            JOptionPane.PLAIN_MESSAGE
        ));
        return help;
    }

    /**
     * Build the layout for the part of the GUI where the settings are located.
     */
    private JComponent buildSettingsArea() {
        JPanel organization = new JPanel(new GridLayout(5, 1, 6, 6));

        this.baseAddressSelector = new JComboBox<>(this.baseAddressNames);
        this.baseAddressSelector.setEditable(false);
        this.baseAddressSelector.setSelectedIndex(this.baseAddressIndex);
        this.baseAddressSelector.setToolTipText("Address of the upper-left corner unit of the bitmap");
        this.baseAddressSelector.addActionListener(event -> {
            this.baseAddressIndex = this.baseAddressSelector.getSelectedIndex();
            this.baseAddress = this.baseAddressChoices[this.baseAddressIndex];
            this.canvas.setFirstAddress(this.baseAddress);
        });
        organization.add(this.createSettingsRow("Base memory address:", this.baseAddressSelector));

        this.bitmapWidthSelector = new JSpinner(new SpinnerNumberModel(this.bitmapWidth, 1, 2048, 1));
        this.bitmapWidthSelector.setToolTipText("Total width, in pixels, of the underlying bitmap");
        this.bitmapWidthSelector.addChangeListener(event -> {
            this.bitmapWidth = ((SpinnerNumberModel) this.bitmapWidthSelector.getModel()).getNumber().intValue();
            this.canvas.setBitmapWidth(this.bitmapWidth);
            this.ensureCanvasVisible();
        });
        organization.add(this.createSettingsRow("Bitmap width:", this.bitmapWidthSelector));

        this.bitmapHeightSelector = new JSpinner(new SpinnerNumberModel(this.bitmapHeight, 1, 2048, 1));
        this.bitmapHeightSelector.setToolTipText("Total height, in pixels, of the underlying bitmap");
        this.bitmapHeightSelector.addChangeListener(event -> {
            this.bitmapHeight = ((SpinnerNumberModel) this.bitmapHeightSelector.getModel()).getNumber().intValue();
            this.canvas.setBitmapHeight(this.bitmapHeight);
            this.ensureCanvasVisible();
        });
        organization.add(this.createSettingsRow("Bitmap height:", this.bitmapHeightSelector));

        this.horizontalScaleSelector = new JSpinner(new SpinnerNumberModel(this.horizontalScale, 1, 32, 1));
        this.horizontalScaleSelector.setToolTipText("Factor to scale the displayed bitmap by in the X direction");
        this.horizontalScaleSelector.addChangeListener(event -> {
            this.horizontalScale = ((SpinnerNumberModel) this.horizontalScaleSelector.getModel()).getNumber().intValue();
            this.canvas.setHorizontalScale(this.horizontalScale);
            this.ensureCanvasVisible();
        });
        organization.add(this.createSettingsRow("Horizontal scale:", this.horizontalScaleSelector));

        this.verticalScaleSelector = new JSpinner(new SpinnerNumberModel(this.verticalScale, 1, 32, 1));
        this.verticalScaleSelector.setToolTipText("Factor to scale the displayed bitmap by in the Y direction");
        this.verticalScaleSelector.addChangeListener(event -> {
            this.verticalScale = ((SpinnerNumberModel) this.verticalScaleSelector.getModel()).getNumber().intValue();
            this.canvas.setVerticalScale(this.verticalScale);
            this.ensureCanvasVisible();
        });
        organization.add(this.createSettingsRow("Vertical scale:", this.verticalScaleSelector));

        return organization;
    }

    private JComponent createSettingsRow(String label, JComponent field) {
        Box settingsRow = Box.createHorizontalBox();
        settingsRow.add(new JLabel(label));
        settingsRow.add(Box.createHorizontalGlue());
        settingsRow.add(field);
        return settingsRow;
    }

    private void validateAddress(int address) {
        if (!Memory.isWordAligned(address)) {
            throw new RuntimeException("base address " + Binary.intToHexString(address) + " is not word-aligned");
        }
    }

    private void validateScale(int scale) {
        if (scale < 1 || scale > 32) {
            throw new RuntimeException("unsupported scale factor " + scale);
        }
    }

    private void validateBitmapSize(int size) {
        if (size < 1 || size > 2048) {
            throw new RuntimeException("unsupported bitmap size " + size);
        }
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
        private boolean imageIsDirty;

        private int firstAddress;
        private int bitmapWidth;
        private int bitmapHeight;
        private int horizontalScale;
        private int verticalScale;

        public BitmapCanvas(int firstAddress, int bitmapWidth, int bitmapHeight, int horizontalScale, int verticalScale) {
            this.firstAddress = firstAddress;
            this.bitmapWidth = bitmapWidth;
            this.bitmapHeight = bitmapHeight;
            this.horizontalScale = horizontalScale;
            this.verticalScale = verticalScale;
            this.imageIsDirty = false;
            // Ensure the background is drawn correctly
            this.setOpaque(false);
            this.setToolTipText("Live display of the bitmap based on values currently in memory");
            // Set the component size and initialize the image (this also starts listening to memory)
            this.updateImageSize();
        }

        public int getBitmapMemorySize() {
            return Memory.BYTES_PER_WORD * this.bitmapWidth * this.bitmapHeight;
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
                int x = offset % this.bitmapWidth;
                int y = offset / this.bitmapWidth;
                if (0 <= offset && offset < this.bitmapWidth * this.bitmapHeight) {
                    this.image.setRGB(x, y, wordValue);
                }
                // Schedule repaint with the updated image if there is not already a repaint scheduled
                if (!this.imageIsDirty) {
                    this.imageIsDirty = true;
                    this.repaint();
                }
            }
        }

        @Override
        public void memoryReset() {
            this.resetImage();
        }

        public void setFirstAddress(int firstAddress) {
            this.firstAddress = firstAddress;
            // Update the active memory range and reinitialize the image contents
            this.stopObserving();
            this.startObserving();
        }

        public void setBitmapWidth(int bitmapWidth) {
            this.bitmapWidth = bitmapWidth;
            this.updateImageSize();
        }

        public void setBitmapHeight(int bitmapHeight) {
            this.bitmapHeight = bitmapHeight;
            this.updateImageSize();
        }

        public void setHorizontalScale(int horizontalScale) {
            this.horizontalScale = horizontalScale;
            this.updateDisplaySize();
        }

        public void setVerticalScale(int verticalScale) {
            this.verticalScale = verticalScale;
            this.updateDisplaySize();
        }

        private void updateDisplaySize() {
            Dimension size = new Dimension(
                this.bitmapWidth * this.horizontalScale,
                this.bitmapHeight * this.verticalScale
            );
            // Just, like, make it very clear that we want the canvas to be this size (still not guaranteed!)
            this.setSize(size);
            this.setPreferredSize(size);
            this.setMinimumSize(size);
            this.setMaximumSize(size);

            // Update the component since its size changed
            this.revalidate();
        }

        private void updateImageSize() {
            // Regenerate the contents of the image from scratch
            synchronized (this.imageLock) {
                this.image = new BufferedImage(
                    this.bitmapWidth,
                    this.bitmapHeight,
                    BufferedImage.TYPE_INT_RGB
                );
            }
            // Update the active memory range and initialize the image contents
            this.stopObserving();
            this.startObserving();
            // The display size depends on the bitmap size, so update that as well
            this.updateDisplaySize();
        }

        private void resetImage() {
            try {
                synchronized (this.imageLock) {
                    int address = this.firstAddress;
                    for (int y = 0; y < this.bitmapHeight; y++) {
                        for (int x = 0; x < this.bitmapWidth; x++) {
                            this.image.setRGB(x, y, Memory.getInstance().fetchWord(address, false));
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
        public void paint(Graphics graphics) {
            // Draw the image in the center of the component bounds
            int displayWidth = this.bitmapWidth * this.horizontalScale;
            int displayHeight = this.bitmapHeight * this.verticalScale;
            int x = Math.max(0, this.getWidth() - displayWidth) / 2;
            int y = Math.max(0, this.getHeight() - displayHeight) / 2;
            synchronized (this.imageLock) {
                graphics.drawImage(this.image, x, y, displayWidth, displayHeight, null);
                this.imageIsDirty = false;
            }
        }
    }
}