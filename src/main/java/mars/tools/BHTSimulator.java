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

package mars.tools;

import mars.ProgramStatement;
import mars.mips.hardware.*;
import mars.tools.bhtsim.BHTSimGUI;
import mars.tools.bhtsim.BHTableModel;
import mars.util.Binary;

import javax.swing.*;
import java.util.Objects;

/**
 * A MARS tool for simulating branch prediction with a Branch History Table (BHT)
 * <p>
 * The simulation is based on observing the access to the instruction memory area (text segment).
 * If a branch instruction is encountered, a prediction based on a BHT is performed.
 * The outcome of the branch is compared with the prediction and the prediction is updated accordingly.
 * Statistics about the correct and incorrect number of predictions can be obtained for each BHT entry.
 * The number of entries in the BHT and the history that is considered for each prediction can be configured interactively.
 * A change of the configuration however causes a re-initialization of the BHT.
 * <p>
 * The tool can be used to show how branch prediction works in case of loops and how effective such simple methods are.
 * In case of nested loops the difference of BHT with 1 or 2 Bit history can be explored and visualized.
 *
 * @author Ingo Kofler (ingo.kofler@itec.uni-klu.ac.at)
 */
public class BHTSimulator extends AbstractMarsTool {
    /**
     * Constant for the default size of the BHT.
     */
    public static final int DEFAULT_ENTRY_COUNT = 16;
    /**
     * Constant for the default history size.
     */
    public static final int DEFAULT_HISTORY_SIZE = 1;
    /**
     * Constant for the default inital value.
     */
    public static final boolean DEFAULT_INITIAL_VALUE = false;
    /**
     * The name of the tool.
     */
    public static final String NAME = "Branch History Table Simulator";
    /**
     * The version of the tool.
     */
    public static final String VERSION = "Version 1.0 (Ingo Kofler)";

    /**
     * The GUI of the BHT simulator.
     */
    private BHTSimGUI gui;
    /**
     * The model for the BHT.
     */
    private BHTableModel tableModel;
    /**
     * State variable that indicates that the last instruction was a branch instruction (if address != 0) or not (address == 0).
     */
    private int pendingBranchInstructionAddress;
    /**
     * State variable that signals if the last branch was taken.
     */
    private boolean lastBranchTaken;

    /**
     * Construct an instance of this tool. This will be used by the {@link mars.venus.ToolManager}.
     */
    @SuppressWarnings("unused")
    public BHTSimulator() {
        super(NAME + ", " + VERSION);
    }

    /**
     * Adds BHTSimulator as observer of the text segment.
     */
    @Override
    protected void startObserving() {
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
        Memory.getInstance().removeListener(this);
    }

    /**
     * Creates a GUI and initialize the GUI with the default values.
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        this.gui = new BHTSimGUI();
        this.tableModel = new BHTableModel(DEFAULT_ENTRY_COUNT, DEFAULT_HISTORY_SIZE, DEFAULT_INITIAL_VALUE);

        this.gui.getTable().setModel(this.tableModel);
        this.gui.getHistorySizeComboBox().setSelectedItem(DEFAULT_HISTORY_SIZE);
        this.gui.getEntryCountComboBox().setSelectedItem(DEFAULT_ENTRY_COUNT);

        this.gui.getEntryCountComboBox().addActionListener(event -> this.reset());
        this.gui.getHistorySizeComboBox().addActionListener(event -> this.reset());
        this.gui.getInitialValueComboBox().addActionListener(event -> this.reset());

        return this.gui;
    }

    /**
     * Returns the name of the tool.
     *
     * @return The tool's name as a string.
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Perform a reset of the simulator.
     * This causes the BHT to be reseted and the log messages to be cleared.
     */
    @Override
    protected void reset() {
        this.gui.getInstructionTextField().setText("");
        this.gui.getInstructionAddressTextField().setText("");
        this.gui.getInstructionIndexTextField().setText("");
        this.gui.getLogTextField().setText("");
        this.tableModel.initialize(
            (Integer) Objects.requireNonNullElse(this.gui.getEntryCountComboBox().getSelectedItem(), DEFAULT_ENTRY_COUNT),
            (Integer) Objects.requireNonNullElse(this.gui.getHistorySizeComboBox().getSelectedItem(), DEFAULT_HISTORY_SIZE),
            BHTSimGUI.TAKE_BRANCH_STRING.equals(this.gui.getInitialValueComboBox().getSelectedItem())
        );

        this.pendingBranchInstructionAddress = 0;
        this.lastBranchTaken = false;
    }

    /**
     * Handles the execution branch instruction.
     * This method is called each time a branch instruction is executed.
     * Based on the address of the instruction the corresponding index into the BHT is calculated.
     * The prediction is obtained from the BHT at the calculated index and is visualized appropriately.
     *
     * @param statement The branch statement that is executed.
     */
    protected void handlePreBranchInst(ProgramStatement statement) {
        String statementString = statement.getBasicAssemblyStatement();
        int address = statement.getAddress();
        String addressString = Binary.intToHexString(address);
        int index = tableModel.getIndexForAddress(address);

        // Update the GUI
        this.gui.getInstructionTextField().setText(statementString);
        this.gui.getInstructionAddressTextField().setText(addressString);
        this.gui.getInstructionIndexTextField().setText(Integer.toString(index));

        // Mark the affected BHT row
        this.gui.getTable().setSelectionBackground(BHTSimGUI.COLOR_PREPREDICTION);
        this.gui.getTable().addRowSelectionInterval(index, index);

        // Add output to log
        this.gui.getLogTextField().append("instruction " + statementString + " at address " + addressString + ", maps to index " + index + "\n");
        this.gui.getLogTextField().append("branches to address " + Binary.intToHexString(BHTSimulator.extractBranchAddress(statement)) + "\n");
        this.gui.getLogTextField().append("prediction is: " + (this.tableModel.getPrediction(index) ? "take" : "do not take") + "...\n");
        this.gui.getLogTextField().setCaretPosition(this.gui.getLogTextField().getDocument().getLength());
    }

    /**
     * Handles the execution of the branch instruction.
     * The correctness of the prediction is visualized in both the table and the log message area.
     * The BHT is updated based on the information if the branch instruction was taken or not.
     *
     * @param branchInstructionAddress The address of the branch instruction.
     * @param branchTaken              The information if the branch is taken or not (determined in a step before).
     */
    protected void handleExecuteBranchInstruction(int branchInstructionAddress, boolean branchTaken) {
        // determine the index in the BHT for the branch instruction
        int index = this.tableModel.getIndexForAddress(branchInstructionAddress);

        // check if the prediction is correct
        boolean correctPrediction = this.tableModel.getPrediction(index) == branchTaken;

        this.gui.getTable().setSelectionBackground(correctPrediction ? BHTSimGUI.COLOR_PREDICTION_CORRECT : BHTSimGUI.COLOR_PREDICTION_INCORRECT);

        // add some output at the log
        this.gui.getLogTextField().append("branch " + (branchTaken ? "taken" : "not taken") + ", prediction was "
            + (correctPrediction ? "correct" : "incorrect") + "\n\n");
        this.gui.getLogTextField().setCaretPosition(this.gui.getLogTextField().getDocument().getLength());

        // update the BHT -> causes refresh of the table
        this.tableModel.updatePrediction(index, branchTaken);
    }

    /**
     * Determines if the instruction is a branch instruction or not.
     *
     * @param statement the statement to investigate
     * @return true, if statement is a branch instruction, otherwise false
     */
    protected static boolean isBranchInstruction(ProgramStatement statement) {
        // Highest 6 bits
        int opcode = statement.getBinaryStatement() >>> (32 - 6);
        // Lowest 5 bits
        int funct = statement.getBinaryStatement() & 0x1F;

        if (opcode == 0x01) {
            if (0x00 <= funct && funct <= 0x07) {
                return true; // bltz, bgez, bltzl, bgezl
            }
            else {
                return 0x10 <= funct && funct <= 0x13; // bltzal, bgezal, bltzall, bgczall
            }
        }
        else if (0x04 <= opcode && opcode <= 0x07) {
            return true; // beq, bne, blez, bgtz
        }
        else {
            return 0x14 <= opcode && opcode <= 0x17; // beql, bnel, blezl, bgtzl
        }
    }

    /**
     * Checks if the branch instruction delivered as parameter will branch or not.
     *
     * @param statement The branch instruction to be investigated.
     * @return true if the branch will be taken, otherwise false.
     */
    protected static boolean willBranch(ProgramStatement statement) {
        int opcode = statement.getBinaryStatement() >>> (32 - 6);
        int funct = statement.getBinaryStatement() & 0x1F;
        int rs = statement.getBinaryStatement() >>> (32 - 6 - 5) & 0x1F;
        int rt = statement.getBinaryStatement() >>> (32 - 6 - 5 - 5) & 0x1F;

        int valRS = RegisterFile.getRegisters()[rs].getValue();
        int valRT = RegisterFile.getRegisters()[rt].getValue();

        if (opcode == 0x01) {
            switch (funct) {
                case 0x00:
                    return valRS < 0; // bltz
                case 0x01:
                    return valRS >= 0; // bgez
                case 0x02:
                    return valRS < 0; // bltzl
                case 0x03:
                    return valRS >= 0; // bgezl
            }
        }

        return switch (opcode) {
            case 0x04, 0x14 -> valRS == valRT;
            case 0x05, 0x15 -> valRS != valRT;
            case 0x06, 0x16 -> valRS <= 0;
            case 0x07, 0x17 -> valRS >= 0;
            default -> true;
        };
    }

    /**
     * Extracts the target address of the branch.
     * <p>
     * In MIPS the target address is encoded as 16-bit value.
     * The target address is encoded relative to the address of the instruction after the branch instruction
     *
     * @param statement The branch instruction.
     * @return The address of the instruction that is executed if the branch is taken.
     */
    protected static int extractBranchAddress(ProgramStatement statement) {
        short offset = (short) (statement.getBinaryStatement() & 0xFFFF);
        return statement.getAddress() + (offset << 2) + 4;
    }

    /**
     * Callback for text segment access by the MIPS simulator.
     * <p>
     * The method is called each time the text segment is accessed to fetch the next instruction.
     * If the next instruction to execute was a branch instruction, the branch prediction is performed and visualized.
     * In case the last instruction was a branch instruction, the outcome of the branch prediction is analyzed and visualized.
     */
    @Override
    public void memoryRead(int address, int length, int value, int wordAddress, int wordValue) {
        ProgramStatement statement;
        try {
            // Access the statement in the text segment without causing an infinite loop
            statement = Memory.getInstance().fetchStatement(address, false);
        }
        catch (AddressErrorException exception) {
            // Ignore misaligned reads
            return;
        }

        // Necessary to handle possible null pointers at the end of the program
        // (e.g. if the simulator tries to execute the next instruction after the last instruction in the text segment)
        if (statement != null) {
            boolean clearTextFields = true;

            // First, check if there's a pending branch to handle
            if (this.pendingBranchInstructionAddress != 0) {
                this.handleExecuteBranchInstruction(this.pendingBranchInstructionAddress, this.lastBranchTaken);
                clearTextFields = false;
                this.pendingBranchInstructionAddress = 0;
            }

            // If current instruction is branch instruction
            if (isBranchInstruction(statement)) {
                this.handlePreBranchInst(statement);
                this.lastBranchTaken = willBranch(statement);
                this.pendingBranchInstructionAddress = statement.getAddress();
                clearTextFields = false;
            }

            // Clear text fields and selection
            if (clearTextFields) {
                this.gui.getInstructionTextField().setText("");
                this.gui.getInstructionAddressTextField().setText("");
                this.gui.getInstructionIndexTextField().setText("");
                this.gui.getTable().clearSelection();
            }
        }
        else {
            // Check if there's a pending branch to handle
            if (this.pendingBranchInstructionAddress != 0) {
                this.handleExecuteBranchInstruction(this.pendingBranchInstructionAddress, this.lastBranchTaken);
                this.pendingBranchInstructionAddress = 0;
            }
        }
    }
}
