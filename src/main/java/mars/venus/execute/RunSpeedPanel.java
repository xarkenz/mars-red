package mars.venus.execute;

import mars.simulator.Simulator;

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
 * Class for the Run speed slider control.
 *
 * @author Pete Sanderson, August 2005
 */
public class RunSpeedPanel extends JPanel {
    private static final double[] RUN_SPEED_VALUES = {
        .05,
        .1,
        .2,
        .3,
        .4,
        .5,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        12.5,
        15,
        17.5,
        20,
        22.5,
        25,
        27.5,
        30,
        35,
        40,
        45,
        50,
        75,
        100,
        125,
        150,
        175,
        200,
        300,
        400,
        500,
        Simulator.UNLIMITED_SPEED,
    };
    private static final int MIN_SPEED_INDEX = 0;
    private static final int MAX_SPEED_INDEX = RUN_SPEED_VALUES.length - 1;
    private static final int INITIAL_SPEED_INDEX = MAX_SPEED_INDEX;

    public RunSpeedPanel() {
        super(new BorderLayout());
        this.setToolTipText("The approximate number of instructions executed by the simulator per second.");

        JLabel label = new JLabel(this.getLabelForIndex(INITIAL_SPEED_INDEX));
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(label, BorderLayout.NORTH);

        JSlider slider = new JSlider(JSlider.HORIZONTAL, MIN_SPEED_INDEX, MAX_SPEED_INDEX, INITIAL_SPEED_INDEX);
        slider.setSnapToTicks(true);
        slider.addChangeListener(event -> {
            JSlider source = (JSlider) event.getSource();
            // Revise label as user slides and update current index when sliding stops
            if (!source.getValueIsAdjusting()) {
                Simulator.getInstance().setRunSpeed(RUN_SPEED_VALUES[source.getValue()]);
            }
            label.setText(this.getLabelForIndex(source.getValue()));
        });
        this.add(slider, BorderLayout.CENTER);
    }

    /**
     * Set label wording depending on current speed setting.
     */
    private String getLabelForIndex(int index) {
        StringBuilder result = new StringBuilder("Run speed: ");
        if (MIN_SPEED_INDEX <= index && index <= MAX_SPEED_INDEX) {
            double runSpeed = RUN_SPEED_VALUES[index];
            if (runSpeed == Simulator.UNLIMITED_SPEED) {
                result.append("unlimited");
            }
            else if (runSpeed < 1) {
                result.append(runSpeed).append(" instructions / second");
            }
            else {
                result.append((int) runSpeed).append(" instructions / second");
            }
        }
        else {
            result.append("invalid");
        }
        return result.toString();
    }
}