package mars.assembler;

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
 * Information about MIPS data types.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class DataTypes {
    /**
     * Number of bytes occupied by MIPS double.
     */
    public static final int DOUBLE_SIZE = 8;
    /**
     * Number of bytes occupied by MIPS float.
     */
    public static final int FLOAT_SIZE = 4;
    /**
     * Number of bytes occupied by MIPS word.
     */
    public static final int WORD_SIZE = 4;
    /**
     * Number of bytes occupied by MIPS halfword.
     */
    public static final int HALF_SIZE = 2;
    /**
     * Number of bytes occupied by MIPS byte.
     */
    public static final int BYTE_SIZE = 1;
    /**
     * Number of bytes occupied by MIPS character.
     */
    public static final int CHAR_SIZE = 1;
    /**
     * Maximum value that can be stored in a MIPS word.
     */
    public static final int MAX_WORD_VALUE = 0x7FFFFFFF;
    /**
     * Minimum value that can be stored in a MIPS word.
     */
    public static final int MIN_WORD_VALUE = 0x80000000;
    /**
     * Maximum value that can be stored in a MIPS halfword.
     */
    public static final int MAX_HALF_VALUE = 0x00007FFF;
    /**
     * Minimum value that can be stored in a MIPS halfword.
     */
    public static final int MIN_HALF_VALUE = 0xFFFF8000;
    /**
     * Maximum value that can be stored in an unsigned MIPS halfword.
     */
    public static final int MAX_UHALF_VALUE = 0x0000FFFF;
    /**
     * Minimum value that can be stored in na unsigned MIPS halfword.
     */
    public static final int MIN_UHALF_VALUE = 0x00000000;
    /**
     * Maximum value that can be stored in a MIPS byte.
     */
    public static final int MAX_BYTE_VALUE = 0x0000007F;
    /**
     * Minimum value that can be stored in a MIPS byte.
     */
    public static final int MIN_BYTE_VALUE = 0xFFFFFF80;
    /**
     * Maximum positive finite value that can be stored in a MIPS float is same as Java Float
     */
    public static final double MAX_FLOAT_VALUE = Float.MAX_VALUE;
    /**
     * Minimum magnitude negative value that can be stored in a MIPS float (negative of the max)
     */
    public static final double LOW_FLOAT_VALUE = -Float.MAX_VALUE;
    /**
     * Maximum positive finite value that can be stored in a MIPS double is same as Java Double
     */
    public static final double MAX_DOUBLE_VALUE = Double.MAX_VALUE;
    /**
     * Largest magnitude negative value that can be stored in a MIPS double (negative of the max)
     */
    public static final double LOW_DOUBLE_VALUE = -Double.MAX_VALUE;

    /**
     * Get length in bytes for numeric MIPS directives.
     *
     * @param direct Directive to be measured.
     * @return Returns length in bytes for values of that type.  If type is not numeric
     *     (or not implemented yet), returns 0.
     */
    public static int getLengthInBytes(Directive direct) {
        if (direct == Directive.FLOAT) {
            return FLOAT_SIZE;
        }
        else if (direct == Directive.DOUBLE) {
            return DOUBLE_SIZE;
        }
        else if (direct == Directive.WORD) {
            return WORD_SIZE;
        }
        else if (direct == Directive.HALF) {
            return HALF_SIZE;
        }
        else if (direct == Directive.BYTE) {
            return BYTE_SIZE;
        }
        else {
            return 0;
        }
    }

    /**
     * Determines whether given integer value falls within value range for given directive.
     *
     * @param direct Directive that controls storage allocation for value.
     * @param value  The value to be stored.
     * @return Returns {@code true} if value can be stored in the number of bytes allowed
     *     by the given directive (.word, .half, .byte), {@code false} otherwise.
     */
    public static boolean outOfRange(Directive direct, int value) {
        if (direct == Directive.HALF && (value < MIN_HALF_VALUE || value > MAX_HALF_VALUE)) {
            return true;
        }
        else {
            return direct == Directive.BYTE && (value < MIN_BYTE_VALUE || value > MAX_BYTE_VALUE);
        }
    }

    /**
     * Determines whether given floating point value falls within value range for given directive.
     * For float, this refers to range of the data type, not precision.  Example: 1.23456789012345
     * be stored in a float with loss of precision.  It's within the range.  But 1.23e500 cannot be
     * stored in a float because the exponent 500 is too large (float allows 8 bits for exponent).
     *
     * @param direct Directive that controls storage allocation for value.
     * @param value  The value to be stored.
     * @return Returns {@code true} if value is within range of
     *     the given directive (.float, .double), {@code false} otherwise.
     */
    public static boolean outOfRange(Directive direct, double value) {
        return direct == Directive.FLOAT && (value < LOW_FLOAT_VALUE || value > MAX_FLOAT_VALUE);
    }
}
