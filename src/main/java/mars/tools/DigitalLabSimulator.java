package mars.tools;

import mars.Application;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryConfigurations;
import mars.simulator.Simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Didier Teifreto LIFC Université de franche-Comté www.lifc.univ-fcomte.fr/~teifreto
 * didier.teifreto@univ-fcomte.fr
 */
public class DigitalLabSimulator extends AbstractMarsTool {
    private static final int IN_OFFSET_DISPLAY_1 = 0x10;
    private static final int IN_OFFSET_DISPLAY_2 = 0x11;
    private static final int IN_OFFSET_HEXADECIMAL_KEYBOARD = 0x12;
    private static final int IN_OFFSET_COUNTER = 0x13;
    private static final int OUT_OFFSET_HEXADECIMAL_KEYBOARD = 0x14;

    public static final int EXTERNAL_INTERRUPT_TIMER = 0x00000100;
    public static final int EXTERNAL_INTERRUPT_HEXADECIMAL_KEYBOARD = 0x00000200;

    // Seven segment display
    private SevenSegmentPanel sevenSegmentPanel;
    // Keyboard
    private static int keyboardValueButtonClick = -1; // -1 no button click
    private HexadecimalKeyboard hexadecimalKeyboard;
    private static boolean keyboardInterruptOnOff = false;
    // Counter
    private static final int COUNTER_VALUE_MAX = 30;
    private static int counterValue = COUNTER_VALUE_MAX;
    private static boolean counterInterruptOnOff = false;
    private static OneSecondCounter secondCounter;

    @Override
    public String getIdentifier() {
        return "digitallab";
    }

    /**
     * Required MarsTool method to return Tool name.
     *
     * @return Tool name.  MARS will display this in menu item.
     */
    @Override
    public String getDisplayName() {
        return "Digital Lab Simulator";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    @Override
    protected void startObserving() {
        Memory.getInstance().addListener(this, Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW) + IN_OFFSET_DISPLAY_1);
    }

    @Override
    protected void stopObserving() {
        Memory.getInstance().removeListener(this);
    }

    @Override
    public void memoryWritten(int address, int length, int value, int wordAddress, int wordValue) {
        int mmioBaseAddress = Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW);
        if (address == mmioBaseAddress + IN_OFFSET_DISPLAY_1) {
            sevenSegmentPanel.modifyDisplay(1, (byte) value);
        }
        else if (address == mmioBaseAddress + IN_OFFSET_DISPLAY_2) {
            sevenSegmentPanel.modifyDisplay(0, (byte) value);
        }
        else if (address == mmioBaseAddress + IN_OFFSET_HEXADECIMAL_KEYBOARD) {
            updateHexadecimalKeyboard(value);
        }
        else if (address == mmioBaseAddress + IN_OFFSET_COUNTER) {
            updateOneSecondCounter(value);
        }
        if (counterInterruptOnOff) {
            if (counterValue > 0) {
                counterValue--;
            }
            else {
                counterValue = COUNTER_VALUE_MAX;
                if ((Coprocessor0.getValue(Coprocessor0.STATUS) & 2) == 0) {
                    Simulator.getInstance().raiseExternalInterrupt(EXTERNAL_INTERRUPT_TIMER);
                }
            }
        }
    }

    @Override
    protected void reset() {
        sevenSegmentPanel.resetSevenSegment();
        hexadecimalKeyboard.resetHexaKeyboard();
        secondCounter.resetOneSecondCounter();
    }

    @Override
    protected JComponent buildMainDisplayArea() {
        JPanel panelTools = new JPanel(new GridLayout(1, 2));
        sevenSegmentPanel = new SevenSegmentPanel(2);
        panelTools.add(sevenSegmentPanel);
        hexadecimalKeyboard = new HexadecimalKeyboard();
        panelTools.add(hexadecimalKeyboard);
        secondCounter = new OneSecondCounter();
        return panelTools;
    }

    private synchronized void updateMMIOControlAndData(int value) {
        Simulator.getInstance().changeState(() -> {
            try {
                Memory.getInstance().storeWord(Memory.getInstance().getAddress(MemoryConfigurations.MMIO_LOW) + OUT_OFFSET_HEXADECIMAL_KEYBOARD, value, true);
            }
            catch (AddressErrorException exception) {
                System.err.println("Tool author specified incorrect MMIO address!");
                exception.printStackTrace(System.err);
                System.exit(0);
            }
        });
    }

    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = """
            This tool is composed of 3 parts : two seven-segment displays, an hexadecimal keyboard and counter
            Seven segment display
             Byte value at address 0xFFFF0010 : command right seven segment display
             Byte value at address 0xFFFF0011 : command left seven segment display
             Each bit of these two bytes are connected to segments (bit 0 for a segment, 1 for b segment and 7 for point
            
            Hexadecimal keyboard
             Byte value at address 0xFFFF0012 : command row number of hexadecimal keyboard (bit 0 to 3) and enable keyboard interrupt (bit 7)
             Byte value at address 0xFFFF0014 : receive row and column of the key pressed, 0 if not key pressed
             The mips program have to scan, one by one, each row (send 1,2,4,8...) and then observe if a key is pressed (that mean byte value at adresse 0xFFFF0014 is different from zero).  This byte value is composed of row number (4 left bits) and column number (4 right bits) Here you'll find the code for each key : 0x11,0x21,0x41,0x81,0x12,0x22,0x42,0x82,0x14,0x24,0x44,0x84,0x18,0x28,0x48,0x88.
             For exemple key number 2 return 0x41, that mean the key is on column 3 and row 1.
             If keyboard interruption is enable, an exception is started, with cause register bit number 11 set.
            
            Counter
             Byte value at address 0xFFFF0013 : If one bit of this byte is set, the counter interruption is enable.
             If counter interruption is enable, every 30 instructions, an exception is started with cause register bit number 10.
             (contributed by Didier Teifreto, dteifreto@lifc.univ-fcomte.fr)""";
        JButton help = new JButton("Help");
        help.putClientProperty("JButton.buttonType", "help");
        help.addActionListener(e -> {
            JTextArea ja = new JTextArea(helpContent);
            ja.setRows(20);
            ja.setColumns(60);
            ja.setLineWrap(true);
            ja.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(dialog, new JScrollPane(ja), "Simulating the Hexa Keyboard and Seven segment display", JOptionPane.INFORMATION_MESSAGE);
        });
        return help;
    }

    public static class SevenSegmentDisplay extends JComponent {
        public byte segments;

        public SevenSegmentDisplay(byte segments) {
            this.segments = segments;
            this.setPreferredSize(new Dimension(60, 80));
        }

        public void modifyDisplay(byte segments) {
            this.segments = segments;
            this.repaint();
        }

        public void drawSegment(Graphics graphics, char segment) {
            switch (segment) {
                case 'a' -> { // a segment
                    int[] pxa1 = { 12, 9, 12 };
                    int[] pxa2 = { 36, 39, 36 };
                    int[] pya = { 5, 8, 11 };
                    graphics.fillPolygon(pxa1, pya, 3);
                    graphics.fillPolygon(pxa2, pya, 3);
                    graphics.fillRect(12, 5, 24, 6);
                }
                case 'b' -> { // b segment
                    int[] pxb = { 37, 40, 43 };
                    int[] pyb1 = { 12, 9, 12 };
                    int[] pyb2 = { 36, 39, 36 };
                    graphics.fillPolygon(pxb, pyb1, 3);
                    graphics.fillPolygon(pxb, pyb2, 3);
                    graphics.fillRect(37, 12, 6, 24);
                }
                case 'c' -> { // c segment
                    int[] pxc = { 37, 40, 43 };
                    int[] pyc1 = { 44, 41, 44 };
                    int[] pyc2 = { 68, 71, 68 };
                    graphics.fillPolygon(pxc, pyc1, 3);
                    graphics.fillPolygon(pxc, pyc2, 3);
                    graphics.fillRect(37, 44, 6, 24);
                }
                case 'd' -> { // d segment
                    int[] pxd1 = { 12, 9, 12 };
                    int[] pxd2 = { 36, 39, 36 };
                    int[] pyd = { 69, 72, 75 };
                    graphics.fillPolygon(pxd1, pyd, 3);
                    graphics.fillPolygon(pxd2, pyd, 3);
                    graphics.fillRect(12, 69, 24, 6);
                }
                case 'e' -> { // e segment
                    int[] pxe = { 5, 8, 11 };
                    int[] pye1 = { 44, 41, 44 };
                    int[] pye2 = { 68, 71, 68 };
                    graphics.fillPolygon(pxe, pye1, 3);
                    graphics.fillPolygon(pxe, pye2, 3);
                    graphics.fillRect(5, 44, 6, 24);
                }
                case 'f' -> { // f segment
                    int[] pxf = { 5, 8, 11 };
                    int[] pyf1 = { 12, 9, 12 };
                    int[] pyf2 = { 36, 39, 36 };
                    graphics.fillPolygon(pxf, pyf1, 3);
                    graphics.fillPolygon(pxf, pyf2, 3);
                    graphics.fillRect(5, 12, 6, 24);
                }
                case 'g' -> { // g segment
                    int[] pxg1 = { 12, 9, 12 };
                    int[] pxg2 = { 36, 39, 36 };
                    int[] pyg = { 37, 40, 43 };
                    graphics.fillPolygon(pxg1, pyg, 3);
                    graphics.fillPolygon(pxg2, pyg, 3);
                    graphics.fillRect(12, 37, 24, 6);
                }
                case 'h' -> { // decimal point
                    graphics.fillOval(49, 68, 8, 8);
                }
            }
        }

        @Override
        public void paint(Graphics graphics) {
            for (char segment = 'a'; segment <= 'h'; segment++) {
                if ((segments & 0x1) == 1) {
                    graphics.setColor(Color.RED);
                }
                else {
                    graphics.setColor(Color.LIGHT_GRAY);
                }
                drawSegment(graphics, segment);
                segments = (byte) (segments >>> 1);
            }
        }
    }

    public static class SevenSegmentPanel extends JPanel {
        public SevenSegmentDisplay[] displays;

        public SevenSegmentPanel(int displayCount) {
            this.setLayout(new FlowLayout());
            displays = new SevenSegmentDisplay[displayCount];
            for (int index = 0; index < displays.length; index++) {
                displays[index] = new SevenSegmentDisplay((byte) 0);
                this.add(displays[index]);
            }
        }

        public void modifyDisplay(int index, byte segments) {
            displays[index].modifyDisplay(segments);
            displays[index].repaint();
        }

        public void resetSevenSegment() {
            for (SevenSegmentDisplay display : displays) {
                display.modifyDisplay((byte) 0);
                display.repaint();
            }
        }
    }

    public void updateHexadecimalKeyboard(int row) {
        int key = keyboardValueButtonClick;
        if ((key != -1) && ((1 << (key / 4)) == (row & 0xF))) {
            updateMMIOControlAndData((1 << (key / 4)) | (1 << (4 + (key % 4))));
        }
        else {
            updateMMIOControlAndData(0);
        }
        keyboardInterruptOnOff = (row & 0xF0) != 0;
    }

    public class HexadecimalKeyboard extends JPanel {
        public JButton[] buttons;

        public HexadecimalKeyboard() {
            GridLayout layout = new GridLayout(4, 4, 4, 4);
            this.setLayout(layout);
            this.buttons = new JButton[16];
            for (int index = 0; index < this.buttons.length; index++) {
                this.buttons[index] = new JButton(Integer.toHexString(index).toUpperCase());
                this.buttons[index].setMargin(new Insets(10, 10, 10, 10));
                this.buttons[index].addMouseListener(new ClickListener(index));
                this.buttons[index].setBackground(null);
                this.add(this.buttons[index]);
            }
        }

        public void resetHexaKeyboard() {
            keyboardValueButtonClick = -1;
            for (JButton button : buttons) {
                button.setBackground(null);
            }
        }

        public class ClickListener extends MouseAdapter {
            private final int buttonValue;

            public ClickListener(int val) {
                buttonValue = val;
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (keyboardValueButtonClick != -1) {
                    // Button already pressed -> now realease
                    keyboardValueButtonClick = -1;
                    updateMMIOControlAndData(0);
                    for (JButton button : buttons) {
                        button.setBackground(null);
                    }
                }
                else {
                    // New button pressed
                    keyboardValueButtonClick = buttonValue;
                    buttons[keyboardValueButtonClick].setBackground(new Color(0x118844));
                    if (keyboardInterruptOnOff && (Coprocessor0.getValue(Coprocessor0.STATUS) & 2) == 0) {
                        Simulator.getInstance().raiseExternalInterrupt(EXTERNAL_INTERRUPT_HEXADECIMAL_KEYBOARD);
                    }
                }
            }
        }
    }

    public void updateOneSecondCounter(int value) {
        if (value != 0) {
            counterInterruptOnOff = true;
            counterValue = COUNTER_VALUE_MAX;
        }
        else {
            counterInterruptOnOff = false;
        }
    }

    public static class OneSecondCounter {
        public OneSecondCounter() {
            counterInterruptOnOff = false;
        }

        public void resetOneSecondCounter() {
            counterInterruptOnOff = false;
            counterValue = COUNTER_VALUE_MAX;
        }
    }
}