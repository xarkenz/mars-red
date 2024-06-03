package mars.tools;

import mars.Application;
import mars.mips.hardware.*;
import mars.simulator.ExceptionCause;
import mars.simulator.Simulator;
import mars.util.Binary;
import mars.venus.AbstractFontSettingDialog;
import mars.venus.VenusUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Random;

/*
Copyright (c) 2003-2014,  Pete Sanderson and Kenneth Vollmar

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
 * Keyboard and Display Simulator.  It can be run either as a stand-alone Java application having
 * access to the mars package, or through MARS as an item in its Tools menu.  It makes
 * maximum use of methods inherited from its abstract superclass AbstractMarsToolAndApplication.
 * <p>
 * Version 1.0, 24 July 2008.
 * <p>
 * Version 1.1, 24 November 2008 corrects two omissions: (1) the tool failed to register as an observer
 * of kernel text memory when counting instruction executions for transmitter ready bit
 * reset delay, and (2) the tool failed to test the Status register's Exception Level bit before
 * raising the exception that results in the interrupt (if the Exception Level bit is 1, that
 * means an interrupt is being processed, so disable further interrupts).
 * <p>
 * Version 1.2, August 2009, soft-codes the MMIO register locations for new memory configuration
 * feature of MARS 3.7.  Previously memory segment addresses were fixed and final.  Now they
 * can be modified dynamically so the tool has to get its values dynamically as well.
 * <p>
 * Version 1.3, August 2011, corrects bug to enable Display window to scroll when needed.
 * <p>
 * Version 1.4, August 2014, adds two features: (1) ASCII control character 12 (form feed) when
 * transmitted will clear the Display window.  (2) ASCII control character 7 (bell) when
 * transmitted with properly coded (X,Y) values will reposition the cursor to the specified
 * position of a virtual text-based terminal.  X represents column, Y represents row.
 *
 * @author Pete Sanderson
 */
public class MMIOSimulator extends AbstractMarsTool {
    private static final String NAME = "Memory-Mapped I/O Simulator";
    private static final String VERSION = "Version 1.4";

    private static String displayPanelTitle;
    private static String keyboardPanelTitle;

    /**
     * Fill character for virtual terminal (random access mode).
     */
    private static final char VT_FILL = ' ';
    public static final Dimension PREFERRED_TEXT_AREA_DIMENSION = new Dimension(400, 200);
    private static final Insets TEXT_AREA_INSETS = new Insets(4, 4, 4, 4);
    private static final Font DEFAULT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /**
     * Time delay to process Transmitter Data is simulated by counting instruction executions.
     * After this many executions, the Transmitter Controller Ready bit set to 1.
     */
    private static final TransmitterDelayTechnique[] DELAY_TECHNIQUES = {
        new FixedLengthDelay(),
        new UniformlyDistributedDelay(),
        new NormallyDistributedDelay(),
    };

    public static int receiverControl;    // keyboard Ready in low-order bit
    public static int receiverData;       // keyboard character in low-order byte
    public static int transmitterControl; // display Ready in low-order bit
    public static int transmitterData;    // display character in low-order byte
    // These are used to track instruction counts to simulate driver delay of Transmitter Data
    private boolean countingInstructions;
    private int instructionCount;
    private int transmitDelayInstructionCountLimit;

    // Should the transmitted character be displayed before the transmitter delay period?
    // If not, hold onto it and print at the end of delay period.
    private int intWithCharacterToDisplay;
    private boolean displayAfterDelay = true;

    // Whether or not display position is sequential (JTextArea append)
    // or random access (row, column).  Supports new random access feature. DPS 17-July-2014
    private boolean displayRandomAccessMode = false;
    private int rows, columns;
    private DisplayResizeAdapter updateDisplayBorder;

    private JTextArea display;
    private JPanel displayPanel;
    private JComboBox<TransmitterDelayTechnique> delayTechniqueChooser;
    private DelayLengthPanel delayLengthPanel;
    private JCheckBox displayAfterDelayCheckBox;
    private JTextArea keyEventAccepter;

    /**
     * Construct an instance of this tool. This will be used by the {@link mars.venus.ToolManager}.
     */
    @SuppressWarnings("unused")
    public MMIOSimulator() {
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

    /**
     * Set the MMIO addresses.  Prior to MARS 3.7 these were final because
     * MIPS address space was final as well.  Now we will get MMIO base address
     * each time to reflect possible change in memory configuration. DPS 6-Aug-09
     */
    @Override
    protected void initializePreGUI() {
        int address = Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW);
        receiverControl = address; // keyboard Ready in low-order bit
        address += Memory.BYTES_PER_WORD;
        receiverData = address; // keyboard character in low-order byte
        address += Memory.BYTES_PER_WORD;
        transmitterControl = address; // display Ready in low-order bit
        address += Memory.BYTES_PER_WORD;
        transmitterData = address; // display character in low-order byte
        displayPanelTitle = "DISPLAY: Store to Transmitter Data " + Binary.intToHexString(transmitterData);
        keyboardPanelTitle = "KEYBOARD: Characters typed here are stored to Receiver Data " + Binary.intToHexString(receiverData);
    }

    /**
     * Override the inherited method, which registers us as an Observer over the static data segment
     * (starting address 0x10010000) only.
     * <p>
     * When user enters keystroke, set RECEIVER_CONTROL and RECEIVER_DATA using the action listener.
     * When user loads word (lw) from RECEIVER_DATA (we are notified of the read), then clear RECEIVER_CONTROL.
     * When user stores word (sw) to TRANSMITTER_DATA (we are notified of the write), then clear TRANSMITTER_CONTROL, read TRANSMITTER_DATA,
     * echo the character to display, wait for delay period, then set TRANSMITTER_CONTROL.
     * <p>
     * If you use the inherited GUI buttons, this method is invoked when you click "Connect" button on MarsTool or the
     * "Assemble and Run" button on a Mars-based app.
     */
    @Override
    protected void startObserving() {
        // Set transmitter Control ready bit to 1, means we're ready to accept display character.
        updateMMIOControl(transmitterControl, readyBitSet(transmitterControl));
        // We want to be an observer only of MIPS reads from RECEIVER_DATA and writes to TRANSMITTER_DATA.
        Memory.getInstance().addListener(this.receiverListener, receiverData);
        Memory.getInstance().addListener(this.transmitterListener, transmitterData);
        // We want to be notified of each instruction execution, because instruction count is the
        // basis for delay in re-setting (literally) the TRANSMITTER_CONTROL register.  SPIM does
        // this too.  This simulates the time required for the display unit to process the TRANSMITTER_DATA.
        Memory.getInstance().addListener(
            this,
            Memory.getInstance().getAddress(MemoryConfigurations.TEXT_LOW),
            Memory.getInstance().getAddress(MemoryConfigurations.TEXT_HIGH)
        );
        Memory.getInstance().addListener(
            this,
            Memory.getInstance().getAddress(MemoryConfigurations.KERNEL_TEXT_LOW),
            Memory.getInstance().getAddress(MemoryConfigurations.KERNEL_TEXT_HIGH)
        );
    }

    @Override
    protected void stopObserving() {
        Memory.getInstance().removeListener(this.receiverListener);
        Memory.getInstance().removeListener(this.transmitterListener);
        Memory.getInstance().removeListener(this);
    }

    private final Memory.Listener receiverListener = new Memory.Listener() {
        @Override
        public void memoryRead(int address, int length, int value, int wordAddress, int wordValue) {
            // If MIPS program has just read (loaded) the receiver (keyboard) data register,
            // then clear the Ready bit to indicate there is no longer a keystroke available.
            // If Ready bit was initially clear, they'll get the old keystroke -- serves 'em right
            // for not checking!
            updateMMIOControl(receiverControl, readyBitCleared(receiverControl));
        }
    };

    private final Memory.Listener transmitterListener = new Memory.Listener() {
        @Override
        public void memoryWritten(int address, int length, int value, int wordAddress, int wordValue) {
            // MIPS program has just written (stored) the transmitter (display) data register.  If transmitter
            // Ready bit is clear, device is not ready yet so ignore this event -- serves 'em right for not checking!
            // If transmitter Ready bit is set, then clear it to indicate the display device is processing the character.
            // Also start an intruction counter that will simulate the delay of the slower
            // display device processing the character.
            if (isReadyBitSet(transmitterControl)) {
                updateMMIOControl(transmitterControl, readyBitCleared(transmitterControl));
                intWithCharacterToDisplay = value;
                if (!displayAfterDelay) {
                    displayCharacter(intWithCharacterToDisplay);
                }
                countingInstructions = true;
                instructionCount = 0;
                transmitDelayInstructionCountLimit = generateDelay();
            }
        }
    };

    /**
     * Update display when connected MIPS program reads an instruction from the text or kernel text segments.
     */
    @Override
    public void memoryRead(int address, int length, int value, int wordAddress, int wordValue) {
        // We have been notified of a MIPS instruction execution.
        // If we are in transmit delay period, increment instruction count and if limit
        // has been reached, set the transmitter Ready flag to indicate the MIPS program
        // can write another character to the transmitter data register.  If the Interrupt-Enabled
        // bit had been set by the MIPS program, generate an interrupt!
        if (this.countingInstructions) {
            this.instructionCount++;
            if (this.instructionCount >= this.transmitDelayInstructionCountLimit) {
                if (displayAfterDelay) {
                    displayCharacter(intWithCharacterToDisplay);
                }
                this.countingInstructions = false;
                int updatedTransmitterControl = readyBitSet(transmitterControl);
                updateMMIOControl(transmitterControl, updatedTransmitterControl);
                if (updatedTransmitterControl != 1 && (Coprocessor0.getValue(Coprocessor0.STATUS) & 2) == 0  // Added by Carl Hauser Nov 2008
                    && (Coprocessor0.getValue(Coprocessor0.STATUS) & 1) == 1) {
                    // interrupt-enabled bit is set in both Tranmitter Control and in
                    // Coprocessor0 Status register, and Interrupt Level Bit is 0, so trigger external interrupt.
                    Simulator.getInstance().raiseExternalInterrupt(ExceptionCause.EXTERNAL_INTERRUPT_DISPLAY);
                }
            }
        }
    }

    /**
     * Method that constructs the main display area.  It is organized vertically
     * into two major components: the display and the keyboard.  The display itself
     * is a JTextArea and it echoes characters placed into the low order byte of
     * the Transmitter Data location, 0xffff000c.  They keyboard is also a JTextArea
     * places each typed character into the Receive Data location 0xffff0004.
     *
     * @return the GUI component containing these two areas
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        // Changed arrangement of the display and keyboard panels from GridLayout(2,1)
        // to BorderLayout to hold a JSplitPane containing both panels.  This permits user
        // to apportion the relative sizes of the display and keyboard panels within
        // the overall frame.  Will be convenient for use with the new random-access
        // display positioning feature.  Previously, both the display and the keyboard
        // text areas were equal in size and there was no way for the user to change that.
        // DPS 17-July-2014
        // Major GUI components
        JPanel keyboardAndDisplay = new JPanel(new BorderLayout());
        JSplitPane both = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildDisplay(), buildKeyboard());
        both.setResizeWeight(0.5);
        keyboardAndDisplay.add(both);
        return keyboardAndDisplay;
    }

    private static final char CLEAR_SCREEN = 12; // ASCII Form Feed
    private static final char SET_CURSOR_X_Y = 7; // ASCII Bell (ding ding!)

    // Method to display the character stored in the low-order byte of
    // the parameter.  We also recognize two non-printing characters:
    //  Decimal 12 (Ascii Form Feed) to clear the display
    //  Decimal  7 (Ascii Bell) to place the cursor at a specified (X,Y) position.
    //             of a virtual text terminal.  The position is specified in the high
    //             order 24 bits of the transmitter word (X in 20-31, Y in 8-19).
    //             Thus the parameter is the entire word, not just the low-order byte.
    // Once the latter is performed, the display mode changes to random
    // access, which has repercussions for the implementation of character display.
    private void displayCharacter(int intWithCharacterToDisplay) {
        char characterToDisplay = (char) (intWithCharacterToDisplay & 0x000000FF);
        if (characterToDisplay == CLEAR_SCREEN) {
            initializeDisplay();
        }
        else if (characterToDisplay == SET_CURSOR_X_Y) {
            // First call will activate random access mode.
            // We're using JTextArea, where caret has to be within text.
            // So initialize text to all spaces to fill the JTextArea to its
            // current capacity.  Then set caret.  Subsequent character
            // displays will replace, not append, in the text.
            if (!displayRandomAccessMode) {
                displayRandomAccessMode = true;
                initializeDisplay();
            }
            // For SET_CURSOR_X_Y, we need data from the rest of the word.
            // High order 3 bytes are split in half to store (X,Y) value.
            // High 12 bits contain X value, next 12 bits contain Y value.
            int x = (intWithCharacterToDisplay & 0xFFF00000) >>> 20;
            int y = (intWithCharacterToDisplay & 0x000FFF00) >>> 8;
            // If X or Y values are outside current range, set to range limit.
            if (x >= columns) {
                x = columns - 1;
            }
            if (y >= rows) {
                y = rows - 1;
            }
            // display is a JTextArea whose character positioning in the text is linear.
            // Converting (row,column) to linear position requires knowing how many columns
            // are in each row.  I add one because each row except the last ends with '\n' that
            // does not count as a column but occupies a position in the text string.
            // The values of rows and columns is set in initializeDisplay().
            display.setCaretPosition(y * (columns + 1) + x);
        }
        else {
            if (displayRandomAccessMode) {
                try {
                    int caretPosition = display.getCaretPosition();
                    // If caret is positioned at the end of a line (at the '\n'), skip over the '\n'
                    if ((caretPosition + 1) % (columns + 1) == 0) {
                        caretPosition++;
                        display.setCaretPosition(caretPosition);
                    }
                    display.replaceRange(Character.toString(characterToDisplay), caretPosition, caretPosition + 1);
                }
                catch (IllegalArgumentException e) {
                    // tried to write off the end of the defined grid.
                    display.setCaretPosition(display.getCaretPosition() - 1);
                    display.replaceRange(Character.toString(characterToDisplay), display.getCaretPosition(), display.getCaretPosition() + 1);
                }
            }
            else {
                display.append(Character.toString(characterToDisplay));
            }
        }
    }

    /**
     * Initialization code to be executed after the GUI is configured.  Overrides inherited default.
     */
    @Override
    protected void initializePostGUI() {
        initializeTransmitDelaySimulator();
        keyEventAccepter.requestFocusInWindow();
    }

    /**
     * Method to reset counters and display when the Reset button selected.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void reset() {
        displayRandomAccessMode = false;
        initializeTransmitDelaySimulator();
        initializeDisplay();
        keyEventAccepter.setText("");
        ((TitledBorder) displayPanel.getBorder()).setTitle(displayPanelTitle);
        displayPanel.repaint();
        keyEventAccepter.requestFocusInWindow();
        updateMMIOControl(transmitterControl, readyBitSet(transmitterControl));
    }

    // The display JTextArea (top half) is initialized either to the empty
    // string, or to a string filled with lines of spaces. It will do the
    // latter only if the MIPS program has sent the BELL character (Ascii 7) to
    // the transmitter.  This sets the caret (cursor) to a specific (x,y) position
    // on a text-based virtual display.  The lines of spaces is necessary because
    // the caret can only be placed at a position within the current text string.
    private void initializeDisplay() {
        String initialText = "";
        if (displayRandomAccessMode) {
            Dimension textDimensions = getDisplayPanelTextDimensions();
            columns = (int) textDimensions.getWidth();
            rows = (int) textDimensions.getHeight();
            repaintDisplayPanelBorder();
            char[] charArray = new char[columns];
            Arrays.fill(charArray, VT_FILL);
            String row = new String(charArray);
            initialText = row + ("\n" + row).repeat(Math.max(0, rows - 1));
        }
        display.setText(initialText);
        display.setCaretPosition(0);
    }

    // Update display window title with current text display capacity (columns and rows)
    // This will be called when window resized or font changed.
    private void repaintDisplayPanelBorder() {
        Dimension size = this.getDisplayPanelTextDimensions();
        int cols = (int) size.getWidth();
        int rows = (int) size.getHeight();
        int caretPosition = display.getCaretPosition();
        String stringCaretPosition;
        // Display position as stream or 2D depending on random access
        if (displayRandomAccessMode) {
            if (((caretPosition + 1) % (columns + 1) != 0)) {
                stringCaretPosition = "(" + (caretPosition % (columns + 1)) + "," + (caretPosition / (columns + 1)) + ")";
            }
            else if (((caretPosition + 1) % (columns + 1) == 0) && ((caretPosition / (columns + 1)) + 1 == rows)) {
                stringCaretPosition = "(" + (caretPosition % (columns + 1) - 1) + "," + (caretPosition / (columns + 1)) + ")";
            }
            else {
                stringCaretPosition = "(0," + ((caretPosition / (columns + 1)) + 1) + ")";
            }
        }
        else {
            stringCaretPosition = Integer.toString(caretPosition);
        }
        String title = displayPanelTitle + ", cursor " + stringCaretPosition + ", area " + cols + " x " + rows;
        ((TitledBorder) displayPanel.getBorder()).setTitle(title);
        displayPanel.repaint();
    }

    // Calculate text display capacity of display window. Text dimensions are based
    // on pixel dimensions of window divided by font size properties.
    private Dimension getDisplayPanelTextDimensions() {
        Dimension areaSize = display.getSize();
        int widthInPixels = (int) areaSize.getWidth();
        int heightInPixels = (int) areaSize.getHeight();
        FontMetrics metrics = getFontMetrics(display.getFont());
        int rowHeight = metrics.getHeight();
        int charWidth = metrics.charWidth('m');
        // Estimate number of columns/rows of text that will fit in current window with current font.
        // I subtract 1 because initial tests showed slight scroll otherwise.
        return new Dimension(widthInPixels / charWidth - 1, heightInPixels / rowHeight - 1);
    }

    // Trigger recalculation and update of display text dimensions when window resized.
    private class DisplayResizeAdapter extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent event) {
            getDisplayPanelTextDimensions();
            repaintDisplayPanelBorder();
        }
    }

    /**
     * Overrides default method, to provide a Help button for this tool/app.
     */
    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = "Keyboard And Display MMIO Simulator\n\n" + "Use this program to simulate Memory-Mapped I/O (MMIO) for a keyboard input device and character " + "display output device.  It may be run either from MARS' Tools menu or as a stand-alone application. " + "For the latter, simply write a driver to instantiate a mars.tools.KeyboardAndDisplaySimulator object " + "and invoke its go() method.\n\n" + "While the tool is connected to MIPS, each keystroke in the text area causes the corresponding ASCII " + "code to be placed in the Receiver Data register (low-order byte of memory word " + Binary.intToHexString(receiverData) + "), and the " + "Ready bit to be set to 1 in the Receiver Control register (low-order bit of " + Binary.intToHexString(receiverControl) + ").  The Ready " + "bit is automatically reset to 0 when the MIPS program reads the Receiver Data using an 'lw' instruction.\n\n" + "A program may write to the display area by detecting the Ready bit set (1) in the Transmitter Control " + "register (low-order bit of memory word " + Binary.intToHexString(transmitterControl) + "), then storing the ASCII code of the character to be " + "displayed in the Transmitter Data register (low-order byte of " + Binary.intToHexString(transmitterData) + ") using a 'sw' instruction.  This " + "triggers the simulated display to clear the Ready bit to 0, delay awhile to simulate processing the data, " + "then set the Ready bit back to 1.  The delay is based on a count of executed MIPS instructions.\n\n" + "In a polled approach to I/O, a MIPS program idles in a loop, testing the device's Ready bit on each " + "iteration until it is set to 1 before proceeding.  This tool also supports an interrupt-driven approach " + "which requires the program to provide an interrupt handler but allows it to perform useful processing " + "instead of idly looping.  When the device is ready, it signals an interrupt and the MARS simuator will " + "transfer control to the interrupt handler.  Note: in MARS, the interrupt handler has to co-exist with the " + "exception handler in kernel memory, both having the same entry address.  Interrupt-driven I/O is enabled " + "when the MIPS program sets the Interrupt-Enable bit in the device's control register.  Details below.\n\n" + "Upon setting the Receiver Controller's Ready bit to 1, its Interrupt-Enable bit (bit position 1) is tested. " + "If 1, then an External Interrupt will be generated.  Before executing the next MIPS instruction, the runtime " + "simulator will detect the interrupt, place the interrupt code (0) into bits 2-6 of Coprocessor 0's Cause " + "register ($13), set bit 8 to 1 to identify the source as keyboard, place the program counter value (address " + "of the NEXT instruction to be executed) into its EPC register ($14), and check to see if an interrupt/trap " + "handler is present (looks for instruction code at address 0x80000180).  If so, the program counter is set to " + "that address.  If not, program execution is terminated with a message to the Run I/O tab.  The Interrupt-Enable " + "bit is 0 by default and has to be set by the MIPS program if interrupt-driven input is desired.  Interrupt-driven " + "input permits the program to perform useful tasks instead of idling in a loop polling the Receiver Ready bit!  " + "Very event-oriented.  The Ready bit is supposed to be read-only but in MARS it is not.\n\n" + "A similar test and potential response occurs when the Transmitter Controller's Ready bit is set to 1.  This " + "occurs after the simulated delay described above.  The only difference is the Cause register bit to identify " + "the (simulated) display as external interrupt source is bit position 9 rather than 8.  This permits you to " + "write programs that perform interrupt-driven output - the program can perform useful tasks while the " + "output device is processing its data.  Much better than idling in a loop polling the Transmitter Ready bit! " + "The Ready bit is supposed to be read-only but in MARS it is not.\n\n" + "IMPORTANT NOTE: The Transmitter Controller Ready bit is set to its initial value of 1 only when you click the tool's " + "'Connect to MIPS' button ('Assemble and Run' in the stand-alone version) or the tool's Reset button!  If you run a " + "MIPS program and reset it in MARS, the controller's Ready bit is cleared to 0!  Configure the Data Segment Window to " + "display the MMIO address range so you can directly observe values stored in the MMIO addresses given above.\n\n" + "COOL NEW FEATURE (MARS 4.5, AUGUST 2014): Clear the display window from MIPS program\n\n" + "When ASCII 12 (form feed) is stored in the Transmitter Data register, the tool's Display window will be cleared " + "following the specified transmission delay.\n\n" + "COOL NEW FEATURE (MARS 4.5, AUGUST 2014): Simulate a text-based virtual terminal with (x,y) positioning\n\n" + "When ASCII 7 (bell) is stored in the Transmitter Data register, the cursor in the tool's Display window will " + "be positioned at the (X,Y) coordinate specified by its high-order 3 bytes, following the specfied transmission delay. " + "Place the X position (column) in bit positions 20-31 of the " + "Transmitter Data register and place the Y position (row) in bit positions 8-19.  The cursor is not displayed " + "but subsequent transmitted characters will be displayed starting at that position. Position (0,0) is at upper left. " + "Why did I select the ASCII Bell character?  Just for fun!\n\n" + "The dimensions (number of columns and rows) of the virtual text-based terminal are calculated based on the display " + "window size and font specifications.  This calculation occurs during program execution upon first use of the ASCII 7 code. " + "It will not change until the Reset button is clicked, even if the window is resized.  The window dimensions are included in " + "its title, which will be updated upon window resize or font change.  No attempt is made to reposition data characters already " + "transmitted by the program.  To change the dimensions of the virtual terminal, resize the Display window as desired (note there " + "is an adjustible splitter between the Display and Keyboard windows) then click the tool's Reset button.  " + "Implementation detail: the window is implemented by a JTextArea to which text is written as a string. " + "Its caret (cursor) position is required to be a position within the string.  I simulated a text terminal with random positioning " + "by pre-allocating a string of spaces with one space per (X,Y) position and an embedded newline where each line ends. Each character " + "transmitted to the window thus replaces an existing character in the string.\n\n" + "Thanks to Eric Wang at Washington State University, who requested these features to enable use of this display as the target " + "for programming MMIO text-based games.\n\n" + "Contact Pete Sanderson at psanderson@otterbein.edu with questions or comments.\n";
        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(event -> {
            JTextArea textArea = new JTextArea(helpContent);
            textArea.setRows(30);
            textArea.setColumns(60);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            JOptionPane optionPane = new JOptionPane(
                new JScrollPane(textArea),
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new String[] { "Close" }
            );
            JDialog helpDialog = optionPane.createDialog(dialog, "Simulating the Keyboard and Display");
            // Make the Help dialog modeless (can remain visible while working with other components)
            helpDialog.setModal(false);
            helpDialog.setVisible(true);
        });
        return helpButton;
    }

    /**
     * UI components and layout for upper part of GUI, where simulated display is located.
     */
    private JComponent buildDisplay() {
        displayPanel = new JPanel(new BorderLayout());
        TitledBorder tb = new TitledBorder(displayPanelTitle);
        tb.setTitleJustification(TitledBorder.CENTER);
        displayPanel.setBorder(tb);
        display = new JTextArea();
        display.setFont(DEFAULT_FONT);
        display.setEditable(false);
        display.setMargin(TEXT_AREA_INSETS);
        updateDisplayBorder = new DisplayResizeAdapter();
        // To update display of size in the Display text area when window or font size changes.
        display.addComponentListener(updateDisplayBorder);
        // To update display of caret position in the Display text area when caret position changes.
        display.addCaretListener(event -> repaintDisplayPanelBorder());

        // 2011-07-29: Patrik Lundin, patrik@lundin.info
        // Added code so display autoscrolls.
        DefaultCaret caret = (DefaultCaret) display.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        // end added autoscrolling

        JScrollPane displayScrollPane = new JScrollPane(display);
        displayScrollPane.setPreferredSize(PREFERRED_TEXT_AREA_DIMENSION);

        displayPanel.add(displayScrollPane);
        JPanel displayOptions = new JPanel();
        delayTechniqueChooser = new JComboBox<>(DELAY_TECHNIQUES);
        delayTechniqueChooser.setToolTipText("Technique for determining simulated transmitter device processing delay");
        delayTechniqueChooser.addActionListener(event -> transmitDelayInstructionCountLimit = generateDelay());
        delayLengthPanel = new DelayLengthPanel();
        displayAfterDelayCheckBox = new JCheckBox("DAD", true);
        displayAfterDelayCheckBox.setToolTipText("Display After Delay: if checked, transmitter data not displayed until after delay");
        displayAfterDelayCheckBox.addActionListener(event -> displayAfterDelay = displayAfterDelayCheckBox.isSelected());

        JButton fontButton = new JButton("Font");
        fontButton.setToolTipText("Select the font for the display panel");
        fontButton.addActionListener(new FontChanger());
        displayOptions.add(fontButton);
        displayOptions.add(displayAfterDelayCheckBox);
        displayOptions.add(delayTechniqueChooser);
        displayOptions.add(delayLengthPanel);
        displayPanel.add(displayOptions, BorderLayout.SOUTH);
        return displayPanel;
    }

    /**
     * UI components and layout for lower part of GUI, where simulated keyboard is located.
     */
    private JComponent buildKeyboard() {
        JPanel keyboardPanel = new JPanel(new BorderLayout());
        keyEventAccepter = new JTextArea();
        keyEventAccepter.setEditable(true);
        keyEventAccepter.setFont(DEFAULT_FONT);
        keyEventAccepter.setMargin(TEXT_AREA_INSETS);
        JScrollPane keyAccepterScrollPane = new JScrollPane(keyEventAccepter);
        keyAccepterScrollPane.setPreferredSize(PREFERRED_TEXT_AREA_DIMENSION);
        keyEventAccepter.addKeyListener(new KeyboardKeyListener());
        keyboardPanel.add(keyAccepterScrollPane);
        TitledBorder tb = new TitledBorder(keyboardPanelTitle);
        tb.setTitleJustification(TitledBorder.CENTER);
        keyboardPanel.setBorder(tb);
        return keyboardPanel;
    }

    /**
     * Update the MMIO Control register memory cell. We will delegate.
     */
    private void updateMMIOControl(int addr, int intValue) {
        updateMMIOControlAndData(addr, intValue, 0, 0, true);
    }

    /**
     * Update the MMIO Control and Data register pair -- 2 memory cells. We will delegate.
     */
    private void updateMMIOControlAndData(int controlAddr, int controlValue, int dataAddr, int dataValue) {
        updateMMIOControlAndData(controlAddr, controlValue, dataAddr, dataValue, false);
    }

    /**
     * This one does the work: update the MMIO Control and optionally the Data register as well.
     * NOTE: last argument TRUE means update only the MMIO Control register; FALSE means update both Control and Data.
     */
    private synchronized void updateMMIOControlAndData(int controlAddr, int controlValue, int dataAddr, int dataValue, boolean controlOnly) {
        synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
            try {
                Memory.getInstance().storeWord(controlAddr, controlValue, true);
                if (!controlOnly) {
                    Memory.getInstance().storeWord(dataAddr, dataValue, true);
                }
            }
            catch (AddressErrorException exception) {
                System.err.println("Tool author specified incorrect MMIO address!\n" + exception);
                System.exit(0);
            }
        }
        // HERE'S A HACK!!  Want to immediately display the updated memory value in MARS
        // but that code was not written for event-driven update (e.g. Observer) --
        // it was written to poll the memory cells for their values.  So we force it to do so.

        if (Application.getGUI() != null && Application.getGUI().getMainPane().getExecuteTab().getTextSegmentWindow().getCodeHighlighting()) {
            Application.getGUI().getMainPane().getExecuteTab().getDataSegmentWindow().updateValues();
        }
    }

    /**
     * Return value of the given MMIO control register after ready (low order) bit set (to 1).
     * Have to preserve the value of Interrupt Enable bit (bit 1)
     */
    private static boolean isReadyBitSet(int mmioControlRegister) {
        try {
            return (Memory.getInstance().fetchWord(mmioControlRegister, true) & 1) == 1;
        }
        catch (AddressErrorException exception) {
            System.err.println("Tool author specified incorrect MMIO address!\n" + exception);
            System.exit(0);
        }
        return false; // to satisfy the compiler -- this will never happen.
    }

    /**
     * Return value of the given MMIO control register after ready (low order) bit set (to 1).
     * Have to preserve the value of Interrupt Enable bit (bit 1).
     */
    private static int readyBitSet(int mmioControlRegister) {
        try {
            return Memory.getInstance().fetchWord(mmioControlRegister, false) | 1;
        }
        catch (AddressErrorException exception) {
            System.err.println("Tool author specified incorrect MMIO address!\n" + exception);
            System.exit(0);
        }
        return 1; // to satisfy the compiler -- this will never happen.
    }

    /**
     * Return value of the given MMIO control register after ready (low order) bit cleared (to 0).
     * Have to preserve the value of Interrupt Enable bit (bit 1). Bits 2 and higher don't matter.
     */
    private static int readyBitCleared(int mmioControlRegister) {
        try {
            return Memory.getInstance().fetchWord(mmioControlRegister, false) & 2;
        }
        catch (AddressErrorException exception) {
            System.err.println("Tool author specified incorrect MMIO address!\n" + exception);
            System.exit(0);
        }
        return 0; // to satisfy the compiler -- this will never happen.
    }

    /**
     * Transmit delay is simulated by counting instruction executions.
     * Here we simly initialize (or reset) the variables.
     */
    private void initializeTransmitDelaySimulator() {
        this.countingInstructions = false;
        this.instructionCount = 0;
        this.transmitDelayInstructionCountLimit = this.generateDelay();
    }

    /**
     * Calculate transmitter delay (# instruction executions) based on
     * current combo box and slider settings.
     */
    private int generateDelay() {
        double sliderValue = delayLengthPanel.getDelayLength();
        TransmitterDelayTechnique technique = (TransmitterDelayTechnique) delayTechniqueChooser.getSelectedItem();
        return technique == null ? 0 : technique.generateDelay(sliderValue);
    }

    /**
     * Class to grab keystrokes going to keyboard echo area and send them to MMIO area
     */
    private class KeyboardKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent event) {
            int updatedReceiverControl = readyBitSet(receiverControl);
            updateMMIOControlAndData(receiverControl, updatedReceiverControl, receiverData, event.getKeyChar() & 0x00000ff);
            if (updatedReceiverControl != 1 && (Coprocessor0.getValue(Coprocessor0.STATUS) & 2) == 0   // Added by Carl Hauser Nov 2008
                && (Coprocessor0.getValue(Coprocessor0.STATUS) & 1) == 1) {
                // interrupt-enabled bit is set in both Receiver Control and in
                // Coprocessor0 Status register, and Interrupt Level Bit is 0, so trigger external interrupt.
                Simulator.getInstance().raiseExternalInterrupt(ExceptionCause.EXTERNAL_INTERRUPT_KEYBOARD);
            }
        }

        @Override
        public void keyPressed(KeyEvent event) {
        }

        @Override
        public void keyReleased(KeyEvent event) {
        }
    }

    /**
     * Class for selecting transmitter delay lengths (# of MIPS instruction executions).
     */
    private class DelayLengthPanel extends JPanel {
        private final static int DELAY_INDEX_MIN = 0;
        private final static int DELAY_INDEX_MAX = 40;
        private final static int DELAY_INDEX_INIT = 4;
        private final double[] delayTable = {1, 2, 3, 4, 5, 10, 20, 30, 40, 50, 100, // 0-10
            150, 200, 300, 400, 500, 600, 700, 800, 900, 1000, // 11-20
            1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, // 21-30
            20000, 40000, 60000, 80000, 100000, 200000, 400000, 600000, 800000, 1000000, // 31-40
        };
        private JLabel sliderLabel = null;
        private volatile int delayLengthIndex = DELAY_INDEX_INIT;

        public DelayLengthPanel() {
            super(new BorderLayout());
            JSlider delayLengthSlider = new JSlider(JSlider.HORIZONTAL, DELAY_INDEX_MIN, DELAY_INDEX_MAX, DELAY_INDEX_INIT);
            delayLengthSlider.setSize(new Dimension(100, (int) delayLengthSlider.getSize().getHeight()));
            delayLengthSlider.setMaximumSize(delayLengthSlider.getSize());
            delayLengthSlider.addChangeListener(new DelayLengthListener());
            sliderLabel = new JLabel(setLabel(delayLengthIndex));
            sliderLabel.setHorizontalAlignment(JLabel.CENTER);
            sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            this.add(sliderLabel, BorderLayout.NORTH);
            this.add(delayLengthSlider, BorderLayout.CENTER);
            this.setToolTipText("Parameter for simulated delay length (MIPS instruction execution count)");
        }

        // returns current delay length setting, in instructions.
        public double getDelayLength() {
            return delayTable[delayLengthIndex];
        }

        // set label wording depending on current speed setting
        private String setLabel(int index) {
            return "Delay length: " + ((int) delayTable[index]) + " instruction executions";
        }

        // Both revises label as user slides and updates current index when sliding stops.
        private class DelayLengthListener implements ChangeListener {
            @Override
            public void stateChanged(ChangeEvent event) {
                JSlider source = (JSlider) event.getSource();
                if (!source.getValueIsAdjusting()) {
                    delayLengthIndex = source.getValue();
                    transmitDelayInstructionCountLimit = generateDelay();
                }
                else {
                    sliderLabel.setText(setLabel(source.getValue()));
                }
            }
        }
    }

    /**
     * Interface for Transmitter Delay-generating techniques.
     */
    private interface TransmitterDelayTechnique {
        int generateDelay(double parameter);
    }

    // Delay value is fixed, and equal to slider value.
    private static class FixedLengthDelay implements TransmitterDelayTechnique {
        @Override
        public String toString() {
            return "Fixed transmitter delay, select using slider";
        }

        @Override
        public int generateDelay(double fixedDelay) {
            return (int) fixedDelay;
        }
    }

    // Randomly pick value from range 1 to slider setting, uniform distribution
    // (each value has equal probability of being chosen).
    private static class UniformlyDistributedDelay implements TransmitterDelayTechnique {
        private final Random random = new Random();

        @Override
        public String toString() {
            return "Uniformly distributed delay, min=1, max=slider";
        }

        @Override
        public int generateDelay(double max) {
            return random.nextInt((int) max) + 1;
        }
    }

    // Pretty badly-hacked normal distribution, but is more realistic than uniform!
    // Get sample from Normal(0,1) -- mean=0, s.d.=1 -- multiply it by slider
    // value, take absolute value to make sure we don't get negative,
    // add 1 to make sure we don't get 0.
    private static class NormallyDistributedDelay implements TransmitterDelayTechnique {
        private final Random random = new Random();

        @Override
        public String toString() {
            return "'Normally' distributed delay: floor(abs(N(0,1)*slider)+1)";
        }

        @Override
        public int generateDelay(double mult) {
            return (int) (Math.abs(random.nextGaussian() * mult) + 1);
        }
    }

    /**
     * Font dialog for the display panel
     * Almost all of the code is used from the SettingsHighlightingAction
     * class.
     */
    private class FontSettingDialog extends AbstractFontSettingDialog {
        public FontSettingDialog(VenusUI gui, String title, Font currentFont) {
            super(gui, title, true, currentFont);
        }

        private void showDialog() {
            // Because dialog is modal, this blocks until user terminates the dialog.
            this.setVisible(true);
        }

        @Override
        protected void closeDialog() {
            this.setVisible(false);
            // Update display text dimensions based on current font and size. DPS 22-July-2014
            updateDisplayBorder.componentResized(null);
        }

        // Control buttons for the dialog.
        @Override
        protected Component buildControlPanel() {
            Box controlPanel = Box.createHorizontalBox();
            JButton okButton = new JButton("OK");
            okButton.addActionListener(event -> {
                apply(getFont());
                closeDialog();
            });
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(event -> {
                closeDialog();
            });
            JButton resetButton = new JButton("Reset");
            resetButton.addActionListener(event -> reset());
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(okButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(cancelButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(resetButton);
            controlPanel.add(Box.createHorizontalGlue());
            return controlPanel;
        }

        // Change the font for the keyboard and display
        @Override
        protected void apply(Font font) {
            display.setFont(font);
            keyEventAccepter.setFont(font);
        }
    }

    private class FontChanger implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            FontSettingDialog fontDialog = new FontSettingDialog(null, "Select Text Font", display.getFont());
            fontDialog.showDialog();
        }
    }
}