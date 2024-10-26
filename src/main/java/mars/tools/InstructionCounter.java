/*
Copyright (c) 2008,  Felipe Lessa

Developed by Felipe Lessa (felipe.lessa@gmail.com)

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
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryConfigurations;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.InstructionFormat;

import javax.swing.*;
import java.awt.*;

/**
 * Instruction counter tool. Can be used to know how many instructions
 * were executed to complete a given program.
 * <p>
 * Code slightly based on {@link MemoryReferenceVisualization}.
 *
 * @author Felipe Lessa (felipe.lessa@gmail.com)
 */
public class InstructionCounter extends AbstractMarsTool {
    private static final String NAME = "Instruction Counter";
    private static final String VERSION = "Version 1.0 (Felipe Lessa)";

    /**
     * Number of instructions executed until now.
     */
    protected int counter = 0;
    private JTextField counterField;
    /**
     * Number of instructions of type R.
     */
    protected int counterR = 0;
    private JTextField counterRField;
    private JProgressBar progressbarR;
    /**
     * Number of instructions of type I.
     */
    protected int counterI = 0;
    private JTextField counterIField;
    private JProgressBar progressbarI;
    /**
     * Number of instructions of type J.
     */
    protected int counterJ = 0;
    private JTextField counterJField;
    private JProgressBar progressbarJ;

    /**
     * The last address we saw. We ignore it because the only way for a
     * program to execute twice the same instruction is to enter an infinite
     * loop, which is not insteresting in the POV of counting instructions.
     */
    protected int lastAddress = -1;

    /**
     * Construct an instance of this tool. This will be used by the {@link mars.venus.ToolManager}.
     */
    @SuppressWarnings("unused")
    public InstructionCounter() {
        super(NAME + ", " + VERSION);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected JComponent buildMainDisplayArea() {
        JPanel panel = new JPanel(new GridBagLayout());

        counterField = new JTextField("0", 10);
        counterField.setEditable(false);

        counterRField = new JTextField("0", 10);
        counterRField.setEditable(false);
        progressbarR = new JProgressBar(JProgressBar.HORIZONTAL);
        progressbarR.setStringPainted(true);

        counterIField = new JTextField("0", 10);
        counterIField.setEditable(false);
        progressbarI = new JProgressBar(JProgressBar.HORIZONTAL);
        progressbarI.setStringPainted(true);

        counterJField = new JTextField("0", 10);
        counterJField.setEditable(false);
        progressbarJ = new JProgressBar(JProgressBar.HORIZONTAL);
        progressbarJ.setStringPainted(true);

        // Add them to the panel

        // Fields
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridheight = c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 17, 0);
        panel.add(counterField, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        panel.add(counterRField, c);

        c.gridy++;
        panel.add(counterIField, c);

        c.gridy++;
        panel.add(counterJField, c);

        // Labels
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 1;
        c.gridwidth = 2;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 17, 0);
        panel.add(new JLabel("Instructions so far: "), c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 2;
        c.gridwidth = 1;
        c.gridy++;
        panel.add(new JLabel("R-type: "), c);

        c.gridy++;
        panel.add(new JLabel("I-type: "), c);

        c.gridy++;
        panel.add(new JLabel("J-type: "), c);

        // Progress bars
        c.insets = new Insets(3, 3, 3, 3);
        c.gridx = 4;
        c.gridy = 2;
        panel.add(progressbarR, c);

        c.gridy++;
        panel.add(progressbarI, c);

        c.gridy++;
        panel.add(progressbarJ, c);

        return panel;
    }

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

    @Override
    public void memoryRead(int address, int length, int value, int wordAddress, int wordValue) {
		if (wordAddress == lastAddress) {
			return;
		}
        lastAddress = wordAddress;

        counter++;
        try {
            ProgramStatement stmt = Memory.getInstance().fetchStatement(wordAddress, false);
            BasicInstruction instr = (BasicInstruction) stmt.getInstruction();
            InstructionFormat format = instr.getFormat();
			if (format == InstructionFormat.R_TYPE) {
				counterR++;
			}
			else if (format == InstructionFormat.I_TYPE || format == InstructionFormat.I_TYPE_BRANCH) {
				counterI++;
			}
			else if (format == InstructionFormat.J_TYPE) {
				counterJ++;
			}
        }
        catch (AddressErrorException exception) {
            exception.printStackTrace(System.err);
        }
        updateDisplay();
    }

    @Override
    protected void initializePreGUI() {
        counter = counterR = counterI = counterJ = 0;
        lastAddress = -1;
    }

    @Override
    protected void reset() {
        counter = counterR = counterI = counterJ = 0;
        lastAddress = -1;
        updateDisplay();
    }

    private void updateDisplay() {
        counterField.setText(Integer.toString(counter));

        counterRField.setText(Integer.toString(counterR));
        progressbarR.setMaximum(counter);
        progressbarR.setValue(counterR);

        counterIField.setText(Integer.toString(counterI));
        progressbarI.setMaximum(counter);
        progressbarI.setValue(counterI);

        counterJField.setText(Integer.toString(counterJ));
        progressbarJ.setMaximum(counter);
        progressbarJ.setValue(counterJ);

        if (counter == 0) {
            progressbarR.setString("n/a");
            progressbarI.setString("n/a");
            progressbarJ.setString("n/a");
        }
        else {
            progressbarR.setString((counterR * 100) / counter + "%");
            progressbarI.setString((counterI * 100) / counter + "%");
            progressbarJ.setString((counterJ * 100) / counter + "%");
        }
    }
}
