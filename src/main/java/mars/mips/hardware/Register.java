package mars.mips.hardware;

import java.util.Observable;

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
public class Register extends Observable {
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
        return name;
    }

    /**
     * Returns the number of the register.
     *
     * @return The number of the register.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns the default (initial) value of the register.
     *
     * @return The default value of the register.
     */
    public int getDefaultValue() {
        return defaultValue;
    }

    /**
     * Change the register's default value, the value to which it will be
     * set when {@link #resetValueToDefault()} is called.
     */
    public synchronized void setDefaultValue(int defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the value of the register.  Observers are notified
     * of the {@link AccessNotice#READ} operation.
     *
     * @return The value of the register.
     */
    public synchronized int getValue() {
        notifyAnyObservers(AccessNotice.READ);
        return value;
    }

    /**
     * Returns the value of the register.  Observers are not notified.
     * Added for release 3.8.
     *
     * @return The value of the register.
     */
    public synchronized int getValueNoNotify() {
        return value;
    }

    /**
     * Sets the value of the register.
     * Observers are notified of the {@link AccessNotice#WRITE} operation.
     *
     * @param value Value to set the register to.
     * @return Previous value of the register.
     */
    public synchronized int setValue(int value) {
        int previousValue = this.value;
        this.value = value;
        notifyAnyObservers(AccessNotice.WRITE);
        return previousValue;
    }

    /**
     * Resets the value of the register to the value it was constructed with.
     * Observers are not notified.
     */
    public synchronized void resetValueToDefault() {
        value = defaultValue;
    }

    /**
     * Notify any observers of the register operation that has just occurred.
     */
    private void notifyAnyObservers(int accessType) {
        if (this.countObservers() > 0) {
            this.setChanged();
            this.notifyObservers(new RegisterAccessNotice(accessType, this.name));
        }
    }
}
