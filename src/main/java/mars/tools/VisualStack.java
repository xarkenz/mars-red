package mars.tools;

import mars.Application;
import mars.ProgramStatement;
import mars.mips.hardware.*;
import mars.mips.instructions.Instruction;
import mars.util.Binary;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Observable;

public class VisualStack extends AbstractMarsTool {
    private static final String NAME = "Visual Stack";
    private static final String VERSION = "Version 1.0";
    private static final int STACK_VIEWER_WIDTH = 400;
    private static final int STACK_VIEWER_HEIGHT = 400;

    private StackViewer stackViewer;
    private JLabel statusLabel;
    private int oldStackPtrValue;
    private boolean stackOK = true;

    /**
     * Construct an instance of this tool. This will be used by the {@link mars.venus.ToolManager}.
     */
    @SuppressWarnings("unused")
    public VisualStack() {
        super(NAME + ", " + VERSION);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initializePreGUI() {
        this.startObserving(Memory.stackLimitAddress, Memory.stackBaseAddress);
        this.startObserving(RegisterFile.getProgramCounterRegister());
        this.oldStackPtrValue = Memory.stackPointer;
    }

    @Override
    protected JComponent buildMainDisplayArea() {
        JPanel displayArea = new JPanel();
        displayArea.setLayout(new BoxLayout(displayArea, BoxLayout.PAGE_AXIS));
        this.stackViewer = new StackViewer(STACK_VIEWER_WIDTH, STACK_VIEWER_HEIGHT);
        this.statusLabel = new JLabel("Stack status: OK");
        JScrollPane displayAreaScrollPane = new JScrollPane(this.stackViewer, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        displayArea.add(displayAreaScrollPane);
        displayArea.add(this.statusLabel);
        this.statusLabel.setAlignmentX(0.5f);
        return displayArea;
    }

    @Override
    public void initializePostGUI() {
        this.dialog.setResizable(false);
    }

    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = NAME + ", " + VERSION + """

            This tool provides a graphical visualization of the MIPS stack. \
            When data is pushed to the stack, a colored rectangle representing the data appears in the appropriate position, along with information about which register the data came from. \
            Visual Stack was written to aid developers of recursive functions in debugging and to help them avoid common stack pitfalls; as such, it distinguishes between return addresses and other types of data. \
            (To do this, it uses a heuristic: if the data is the address of a jal command plus 4, it is probably a return address. This properly identifies return addresses contained in registers other than $ra.) \
            Aside from showing data on the stack, Visual Stack represents the position of the stack pointer both graphically (a green arrow points to the data at $sp) and textually (the value of $sp is displayed.)

            Visual Stack can also detect certain error conditions, such as $sp not being aligned on a word boundary. Should such a condition arise, the screen will freeze and the error will be briefly described. \
            The stack will not be updated again until the Reset button is pressed.

            This tool was written by James Hester, a student at the University of Texas at Dallas, in November 2014.""";

        JButton help = new JButton("Help");
        help.addActionListener(event -> {
            JTextArea textArea = new JTextArea(helpContent);
            textArea.setRows(15);
            textArea.setColumns(30);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            JOptionPane.showMessageDialog(this.dialog, new JScrollPane(textArea), "Visual Stack", JOptionPane.INFORMATION_MESSAGE);
        });
        return help;
    }

    @Override
    protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
        if (this.stackOK) {
            synchronized (Application.MEMORY_AND_REGISTERS_LOCK) {
                if (notice.getAccessType() == AccessNotice.WRITE) {
                    if (notice.accessIsFromGUI()) {
                        return;
                    }

                    if (notice instanceof MemoryAccessNotice memoryAccessNotice) {
                        this.processMemoryUpdate(memoryAccessNotice);
                    }
                    else {
                        this.processStackPtrUpdate();
                    }
                }
            }
        }
    }

    private void processMemoryUpdate(MemoryAccessNotice notice) {
        int address = notice.getAddress();
        int valueWritten;
        int registerDataCameFrom;
        if (address <= Memory.stackBaseAddress && address >= Memory.stackLimitAddress) {
            try {
                valueWritten = Memory.getInstance().getWord(address);
                ProgramStatement statement = Memory.getInstance().getStatement(RegisterFile.getProgramCounter() - Instruction.INSTRUCTION_LENGTH_BYTES);
                registerDataCameFrom = statement.getBinaryStatement();
                registerDataCameFrom &= 0x1F0000;
                registerDataCameFrom >>= 16;
            }
            catch (AddressErrorException exception) {
                exception.printStackTrace(System.err);
                return;
            }

            int position = (Memory.stackPointer - address) / Memory.WORD_LENGTH_BYTES;
            if (position < 0) {
                this.handleError("Data was pushed to an address greater than stack base!");
            }
            else {
                boolean dataIsReturnAddress = this.isReturnAddress(valueWritten);
                String description = (dataIsReturnAddress ? "Return address" : "Data")
                    + " from register " + RegisterFile.getRegisters()[registerDataCameFrom].getName();

                if (position > 0) {
                    this.stackViewer.insertStackElement(position, dataIsReturnAddress, description);
                }
            }
        }
    }

    private void handleError(String errorText) {
        this.statusLabel.setForeground(Color.RED);
        this.statusLabel.setText(errorText);
        this.stackViewer.errorOverlay();
        Toolkit.getDefaultToolkit().beep();
        this.stackOK = false;
        this.repaint();
    }

    private void processStackPtrUpdate() {
        int newStackPtrValue = RegisterFile.getValue(RegisterFile.STACK_POINTER);
        int stackPtrDelta = this.oldStackPtrValue - newStackPtrValue;
        if (stackPtrDelta % Memory.WORD_LENGTH_BYTES != 0) {
            this.handleError("$sp set to " + Binary.intToHexString(newStackPtrValue) + "; not word-aligned!");
        }
        else {
            stackPtrDelta /= 4;
            this.stackViewer.advanceStackPointer(stackPtrDelta);
            this.oldStackPtrValue = newStackPtrValue;
        }
    }

    private synchronized boolean isReturnAddress(int address) {
        if (Memory.inTextSegment(address)) {
            try {
                ProgramStatement statement = Memory.getInstance().getStatement(address - Instruction.INSTRUCTION_LENGTH_BYTES);
                return statement.getBasicAssemblyStatement().startsWith("jal");
            }
            catch (AddressErrorException exception) {
                return false;
            }
        }
        else {
            return false;
        }
    }

    @Override
    public void reset() {
        this.stackViewer.reset(STACK_VIEWER_WIDTH, STACK_VIEWER_HEIGHT);
        this.statusLabel.setForeground(null);
        this.statusLabel.setText("Stack status: OK");
        this.stackViewer.advanceStackPointer(0);
        this.oldStackPtrValue = Memory.stackPointer;
        this.stackOK = true;
        this.repaint();
    }

    private static class StackViewer extends JComponent {
        private static final Color GARBAGE_DARK = new Color(0x801515);
        private static final Color GARBAGE_LIGHT = new Color(0xFFAAAA);
        private static final Color DATA_DARK = new Color(0x0D4D4D);
        private static final Color DATA_LIGHT = new Color(0x669999);
        private static final Color RET_DARK = new Color(0x567714);
        private static final Color RET_LIGHT = new Color(0xD4EE9F);
        private static final Color NULL_DARK = new Color(0x424242);
        private static final Color NULL_LIGHT = new Color(0xD2D2D2);
        private static final Color ARROW_COLOR = new Color(0x00FF00);

        private BufferedImage screen;
        private Graphics2D imageWriter;
        private int stackPtrPosition = 0;
        private int highestOccupiedPosition;

        public StackViewer(int width, int height) {
            this.reset(width, height);
        }

        public void reset(int width, int height) {
            super.setSize(width, height);
            super.setPreferredSize(new Dimension(width, height));
            this.screen = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            this.imageWriter = this.screen.createGraphics();
            this.imageWriter.setFont(new Font("SansSerif", Font.BOLD, 14));
            this.imageWriter.setColor(Color.BLACK);
            this.imageWriter.fillRect(0, 0, width, height);
            this.imageWriter.setColor(NULL_LIGHT);
            this.imageWriter.drawLine(0, height / 10, width, height / 10);
            this.imageWriter.drawLine(40, height / 10, 40, height);
            this.stackPtrPosition = 0;
            this.highestOccupiedPosition = 0;
            this.advanceStackPointer(0);
        }

        public void printStackPointerAddress(int position) {
            this.imageWriter.setColor(Color.BLACK);
            this.imageWriter.fillRect(0, 0, this.getWidth(), 39);
            this.imageWriter.setColor(Color.WHITE);
            this.imageWriter.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
            this.printStringCentered("$sp: " + Binary.intToHexString(Memory.stackPointer - position * 4), 116, 0, 24);
        }

        public void advanceStackPointer(int numPositions) {
            int newPosition = this.stackPtrPosition + numPositions;
            this.printStackPointerAddress(newPosition);
            this.ensureHeight(40 * (newPosition + 1));
            this.imageWriter.setColor(Color.BLACK);
            this.imageWriter.fillRect(0, 40 * (this.stackPtrPosition > 0 ? this.stackPtrPosition : 1) + 1, 35, 39);
            this.drawArrow(newPosition, ARROW_COLOR);
            if (numPositions < 0) {
                for (int index = newPosition + 1; index <= this.stackPtrPosition; ++index) {
                    this.removeStackElement(index);
                }
            }
            else if (numPositions > 0 && newPosition > this.highestOccupiedPosition) {
                for (int index = this.highestOccupiedPosition + 1; index <= newPosition; ++index) {
                    this.insertEmptyElement(index);
                }
            }

            this.repaint();
            this.stackPtrPosition = newPosition;
        }

        public void insertEmptyElement(int index) {
            int y1 = 2 + 40 * index;
            this.imageWriter.setColor(NULL_DARK);
            this.imageWriter.fillRect(42, y1, 320, 36);
            this.imageWriter.setColor(NULL_LIGHT);
            this.imageWriter.fillRect(46, y1 + 4, 312, 28);
            this.imageWriter.setColor(Color.BLACK);
            int stringHeight = (int) this.imageWriter.getFontMetrics().getLineMetrics("Null or invalid data", this.imageWriter).getHeight();
            this.printStringCentered("Null or invalid data", 312, 42, y1 + 28 - (28 - stringHeight) / 2);
        }

        public void removeStackElement(int index) {
            int y1 = 1 + 40 * index;
            this.imageWriter.setColor(Color.BLACK);
            this.imageWriter.fillRect(41, y1, 322, 42);
            this.highestOccupiedPosition = index - 1;
        }

        public void insertStackElement(int index, boolean isReturnAddress, String label) {
            if (index > this.highestOccupiedPosition) {
                this.highestOccupiedPosition = index;
            }

            this.ensureHeight(40 * (index + 1));
            int y1 = 2 + 40 * index;
            if (isReturnAddress) {
                this.imageWriter.setColor(RET_DARK);
            }
            else {
                this.imageWriter.setColor(DATA_DARK);
            }

            this.imageWriter.fillRect(42, y1, 320, 36);
            if (isReturnAddress) {
                this.imageWriter.setColor(RET_LIGHT);
            }
            else {
                this.imageWriter.setColor(DATA_LIGHT);
            }

            this.imageWriter.fillRect(46, y1 + 4, 312, 28);
            this.imageWriter.setColor(Color.BLACK);
            int strHeight = (int) this.imageWriter.getFontMetrics().getLineMetrics(label, this.imageWriter).getHeight();
            this.printStringCentered(label, 312, 42, y1 + 28 - (28 - strHeight) / 2);
            this.repaint();
        }

        public void ensureHeight(int newHeight) {
            if (newHeight > this.screen.getHeight()) {
                BufferedImage newScreen = new BufferedImage(this.screen.getWidth(), newHeight, 6);
                Graphics2D newImageWriter = newScreen.createGraphics();
                newImageWriter.drawImage(this.screen, 0, 0, this);
                newImageWriter.setColor(Color.BLACK);
                newImageWriter.fillRect(0, this.screen.getHeight(), this.screen.getWidth(), newHeight - this.screen.getHeight());
                newImageWriter.setColor(NULL_LIGHT);
                newImageWriter.drawLine(40, this.screen.getHeight(), 40, newHeight);
                newImageWriter.setColor(this.imageWriter.getColor());
                newImageWriter.setFont(this.imageWriter.getFont());
                newImageWriter.setRenderingHints(this.imageWriter.getRenderingHints());
                this.screen = newScreen;
                this.imageWriter = newImageWriter;
                this.setPreferredSize(new Dimension(this.getWidth(), newHeight));
                this.revalidate();
                this.repaint();
            }
        }

        private void printStringCentered(String string, int width, int x, int y) {
            int offset = this.imageWriter.getFontMetrics().stringWidth(string);
            offset = (width - offset) / 2;
            this.imageWriter.setFont(new Font("SansSerif", Font.BOLD, 14));
            this.imageWriter.drawString(string, x + offset, y);
        }

        private void drawArrow(int position, Color color) {
            int[] arrowXPoints = { 12, 24, 12 };
            int[] arrowYPoints = { 10, 20, 30 };

            for (int index = 0; index < arrowYPoints.length; ++index) {
                arrowYPoints[index] += (position > 0 ? position : 1) * 400 / 10;
            }

            this.imageWriter.setColor(color);
            this.imageWriter.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
            this.imageWriter.fillPolygon(arrowXPoints, arrowYPoints, 3);
        }

        public void errorOverlay() {
            this.imageWriter.setColor(GARBAGE_DARK);
            this.imageWriter.fillRect(0, 0, this.getSize().width, this.getSize().height);
            this.repaint();
        }

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (this.screen != null) {
                graphics.drawImage(this.screen, 0, 0, this);
            }
        }
    }
}
