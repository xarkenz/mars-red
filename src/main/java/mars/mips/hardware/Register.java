package mars.mips.hardware;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

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
 * Abstraction to represent a register of a MIPS Assembler.
 *
 * @author Jason Bumgarner, Jason Shrewsbury, Ben Sherman
 * @version June 2003
 */
public class Register {
    public interface Listener extends EventListener {
        default void registerWritten(Register register) {
            // Do nothing by default
        }

        default void registerRead(Register register) {
            // Do nothing by default
        }
    }

    private final List<Listener> listeners;
    private final String name;
    private final int number;
    private int defaultValue;
    // volatile should be enough to allow safe multi-threaded access
    // w/o the use of synchronized methods.  getValue and setValue
    // are the only methods here used by the register collection
    // (RegisterFile, Coprocessor0, Coprocessor1) methods.
    private volatile int value;

    /**
     * Creates a new register with specified name, number, and value.
     *
     * @param name   The name of the register.
     * @param number The number of the register.
     * @param defaultValue  The default (and initial) value of the register.
     */
    public Register(String name, int number, int defaultValue) {
        this.listeners = new ArrayList<>();
        this.name = name;
        this.number = number;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the name of the register.
     *
     * @return The name of the register.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the number of the register.
     *
     * @return The number of the register.
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * Returns the default (initial) value of the register.
     *
     * @return The default value of the register.
     */
    public int getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * Change the register's default value, the value to which it will be
     * set when {@link #resetValueToDefault()} is called.
     */
    public void setDefaultValue(int defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the value of the register, notifying listeners.
     *
     * @return The value of the register.
     */
    public synchronized int getValue() {
        synchronized (this.listeners) {
            for (Listener listener : this.listeners) {
                listener.registerRead(this);
            }
        }
        return this.value;
    }

    /**
     * Gets the value of the register without notifying listeners.
     *
     * @return The value of the register.
     */
    public synchronized int getValueNoNotify() {
        return this.value;
    }

    /**
     * Sets the value of the register, notifying listeners.
     *
     * @param value Value to set the register to.
     * @return Previous value of the register.
     */
    public synchronized int setValue(int value) {
        int previousValue = this.value;
        this.value = value;
        synchronized (this.listeners) {
            for (Listener listener : this.listeners) {
                listener.registerWritten(this);
            }
        }
        return previousValue;
    }

    /**
     * Resets the value of the register to the value it was constructed with.
     * Listeners are not notified.
     */
    public synchronized void resetValueToDefault() {
        this.value = this.defaultValue;
    }

    public void addListener(Listener listener) {
        synchronized (this.listeners) {
            if (!this.listeners.contains(listener)) {
                this.listeners.add(listener);
            }
        }
    }

    public void removeListener(Listener listener) {
        synchronized (this.listeners) {
            this.listeners.remove(listener);
        }
    }

    public Listener[] getListeners() {
        synchronized (this.listeners) {
            return this.listeners.toArray(Listener[]::new);
        }
    }
}
