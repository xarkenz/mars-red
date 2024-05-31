package mars.util;

import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryConfigurations;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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

public class MemoryDump {
    private static final String[] segmentNames = {".text", ".data"};
    private static final int[] baseAddresses = new int[2];
    private static final int[] limitAddresses = new int[2];

    /**
     * Return array with segment address bounds for specified segment.
     *
     * @param segment String with segment name (initially ".text" and ".data")
     * @return array of two Integer, the base and limit address for that segment.  Null if parameter
     *     name does not match a known segment name.
     */
    public static Integer[] getSegmentBounds(String segment) {
        for (int i = 0; i < segmentNames.length; i++) {
            if (segmentNames[i].equals(segment)) {
                Integer[] bounds = new Integer[2];
                bounds[0] = getBaseAddresses()[i];
                bounds[1] = getLimitAddresses()[i];
                return bounds;
            }
        }
        return null;
    }

    /**
     * Get the names of segments available for memory dump.
     *
     * @return array of Strings, each string is segment name (e.g. ".text", ".data")
     */
    public static String[] getSegmentNames() {
        return segmentNames;
    }

    /**
     * Get the MIPS memory base address(es) of the specified segment name(s).
     * If invalid segment name is provided, will throw NullPointerException, so
     * I recommend getting segment names from getSegmentNames().
     *
     * @return Array of int containing corresponding base addresses.
     */
    public static int[] getBaseAddresses() {
        baseAddresses[0] = Memory.getInstance().getAddress(MemoryConfigurations.TEXT_LOW);
        baseAddresses[1] = Memory.getInstance().getAddress(MemoryConfigurations.DATA_LOW);
        return baseAddresses;
    }

    /**
     * Get the MIPS memory limit address(es) of the specified segment name(s).
     * If invalid segment name is provided, will throw NullPointerException, so
     * I recommend getting segment names from getSegmentNames().
     *
     * @return Array of int containing corresponding limit addresses.
     */
    public static int[] getLimitAddresses() {
        limitAddresses[0] = Memory.getInstance().getAddress(MemoryConfigurations.TEXT_HIGH);
        limitAddresses[1] = Memory.getInstance().getAddress(MemoryConfigurations.DATA_HIGH);
        return limitAddresses;
    }
}