package mars.venus;

import mars.simulator.Simulator;
import mars.simulator.SimulatorFinishEvent;
import mars.simulator.SimulatorListener;

import javax.swing.*;

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
 * Contains tabbed areas in the UI to display register contents.
 *
 * @author Pete Sanderson, August 2005
 */
public class RegistersPane extends JTabbedPane implements SimulatorListener {
    private final ProcessorTab processorTab;
    private final Coprocessor1Tab coprocessor1Tab;
    private final Coprocessor0Tab coprocessor0Tab;

    /**
     * Constructor for the RegistersPane class.
     */
    public RegistersPane(VenusUI gui) {
        super();
        this.processorTab = new ProcessorTab(gui);
        this.coprocessor1Tab = new Coprocessor1Tab(gui);
        this.coprocessor0Tab = new Coprocessor0Tab(gui);
        this.processorTab.setVisible(true);
        this.coprocessor1Tab.setVisible(true);
        this.coprocessor0Tab.setVisible(true);
        this.addTab("Processor", null, this.processorTab, "Built-in CPU registers");
        this.addTab("Coprocessor 1", null, this.coprocessor1Tab, "CP1: floating-point unit (FPU)");
        this.addTab("Coprocessor 0", null, this.coprocessor0Tab, "CP0: System Control Coprocessor (used for exceptions and interrupts)");

        Simulator.getInstance().addGUIListener(this);
    }

    /**
     * Return component containing integer register set.
     *
     * @return integer register window
     */
    public ProcessorTab getProcessorTab() {
        return this.processorTab;
    }

    /**
     * Return component containing Coprocessor 1 (floating point) register set.
     *
     * @return floating point register window
     */
    public Coprocessor1Tab getCoprocessor1Tab() {
        return this.coprocessor1Tab;
    }

    /**
     * Return component containing Coprocessor 0 (exceptions) register set.
     *
     * @return exceptions register window
     */
    public Coprocessor0Tab getCoprocessor0Tab() {
        return this.coprocessor0Tab;
    }

    @Override
    public void simulatorFinished(SimulatorFinishEvent event) {
        if (event.getReason() == SimulatorFinishEvent.Reason.EXCEPTION) {
            this.setSelectedComponent(this.coprocessor0Tab);
        }
    }
}