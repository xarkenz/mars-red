package mars.venus.execute;

import mars.Application;

import javax.swing.*;
import java.awt.*;
	
/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Class for the Run speed slider control.  One singleton is created and can be obtained using
 * {@link #getInstance()}.
 *
 * @author Pete Sanderson
 * @version August 2005
 */
public class RunSpeedPanel extends JPanel {
    /**
     * Constant that represents unlimited run speed.  Compare with return value of
     * getRunSpeed() to determine if set to unlimited.  At the unlimited setting, the GUI
     * will not attempt to update register and memory contents as each instruction
     * is executed.  This is the only possible value for command-line use of Mars.
     */
    public static final double UNLIMITED_SPEED = 1000;

    private static final int SPEED_INDEX_MIN = 0;
    private static final int SPEED_INDEX_MAX = 40;
    private static final int SPEED_INDEX_INIT = 40;
    private static final int SPEED_INDEX_INTERACTION_LIMIT = 36;
    private static final double[] RUN_SPEED_TABLE = {
        .05, .1, .2, .3, .4, .5, 1, 2, 3, 4, 5, // 0-10
        6, 7, 8, 9, 10, 12.5, 15, 17.5, 20, 22.5, // 11-20
        25, 27.5, 30, 35, 40, 45, 50, 75, 100, 125, // 21-30
        150, 175, 200, 300, 400, 500, UNLIMITED_SPEED, // 31-37
        UNLIMITED_SPEED, UNLIMITED_SPEED, UNLIMITED_SPEED // 38-40
    };

    private volatile int runSpeedIndex = SPEED_INDEX_INIT;

    private static RunSpeedPanel instance = null;

    /**
     * Retrieve the run speed panel object.
     *
     * @return The run speed panel.
     */
    public static RunSpeedPanel getInstance() {
        if (instance == null) {
            instance = new RunSpeedPanel();
            Application.runSpeedPanelExists = true; // DPS 24 July 2008 (needed for standalone tools)
        }
        return instance;
    }

    /**
     * Private constructor (this is a singleton class).
     */
    private RunSpeedPanel() {
        super(new BorderLayout());

        this.setToolTipText("Simulation speed for program execution. At "
            + ((int) RUN_SPEED_TABLE[SPEED_INDEX_INTERACTION_LIMIT])
            + " instructions per second or less, the interface is updated after each instruction.");

        final JLabel sliderLabel = new JLabel(getLabelForIndex(runSpeedIndex));
        sliderLabel.setHorizontalAlignment(JLabel.CENTER);
        sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(sliderLabel, BorderLayout.NORTH);

        JSlider runSpeedSlider = new JSlider(JSlider.HORIZONTAL, SPEED_INDEX_MIN, SPEED_INDEX_MAX, SPEED_INDEX_INIT);
//        runSpeedSlider.setMajorTickSpacing(5);
//        runSpeedSlider.setPaintTicks(true); // Create the label table
        runSpeedSlider.addChangeListener(event -> {
            // Revise label as user slides and update current index when sliding stops
            JSlider source = (JSlider) event.getSource();
            if (!source.getValueIsAdjusting()) {
                runSpeedIndex = source.getValue();
            }
            else {
                sliderLabel.setText(getLabelForIndex(source.getValue()));
            }
        });
        this.add(runSpeedSlider, BorderLayout.SOUTH);
    }

    /**
     * Returns current run speed setting, in instructions/second.  Unlimited speed
     * setting is equal to {@link #UNLIMITED_SPEED}.
     *
     * @return Run speed setting in instructions/second.
     */
    public double getRunSpeed() {
        return RUN_SPEED_TABLE[runSpeedIndex];
    }

    /**
     * Set label wording depending on current speed setting.
     */
    private String getLabelForIndex(int index) {
        StringBuilder result = new StringBuilder("Run speed: ");
        if (index <= SPEED_INDEX_INTERACTION_LIMIT) {
            if (RUN_SPEED_TABLE[index] < 1) {
                result.append(RUN_SPEED_TABLE[index]).append(" instructions per second");
            }
            else {
                result.append((int) RUN_SPEED_TABLE[index]).append(" instructions per second");
            }
        }
        else {
            result.append("maximum (no interactions)");
        }
        return result.toString();
    }
}