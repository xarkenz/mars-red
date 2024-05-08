package mars.util;

import mars.Application;

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
 * Some utility methods for working with binary representations.
 *
 * @author Pete Sanderson, Ken Vollmar, and Jason Bumgarner
 * @version July 2005
 */
public class Binary {
    // Using int value 0-15 as index, yields equivalent hex digit as char.
    private static final String HEX_DIGITS = "0123456789abcdef";

    /**
     * Translate int value into a String consisting of '1's and '0's.
     *
     * @param value  The int value to convert.
     * @param length The number of bit positions, starting at least significant, to process.
     * @return String consisting of '1' and '0' characters corresponding to the requested binary sequence.
     */
    public static String intToBinaryString(int value, int length) {
        char[] result = new char[length];
        for (int index = 0; index < length; index++) {
            result[length - 1 - index] = (bitValue(value, index) == 1) ? '1' : '0';
        }
        return new String(result);
    }

    /**
     * Translate int value into a String consisting of '1's and '0's.  Assumes all 32 bits are
     * to be translated.
     *
     * @param value The int value to convert.
     * @return String consisting of '1' and '0' characters corresponding to the requested binary sequence.
     */
    public static String intToBinaryString(int value) {
        return intToBinaryString(value, Integer.SIZE);
    }

    /**
     * Translate String consisting of '1's and '0's into an int value having that binary representation.
     * The String is assumed to be at most 32 characters long.  No error checking is performed.
     * String position 0 has most-significant bit, position length-1 has least-significant.
     *
     * @param string The String value to convert.
     * @return int whose binary value corresponds to decoded String.
     */
    public static int binaryStringToInt(String string) {
        int result = 0;
        for (int bit = 0; bit < string.length(); bit++) {
            result = (result << 1) | (string.charAt(bit) - '0');
        }
        return result;
    }

    /**
     * Translate String consisting of '1's and '0's into a long value having that binary representation.
     * The String is assumed to be at most 64 characters long.  No error checking is performed.
     * String position 0 has most-significant bit, position length-1 has least-significant.
     *
     * @param string The String value to convert.
     * @return long whose binary value corresponds to decoded String.
     */
    public static long binaryStringToLong(String string) {
        long result = 0;
        for (int bit = 0; bit < string.length(); bit++) {
            result = (result << 1) | (string.charAt(bit) - '0');
        }
        return result;
    }

    /**
     * Translate String consisting of '1's and '0's into String equivalent of the corresponding
     * hexadecimal value.  No length limit.
     * String position 0 has most-significant bit, position length-1 has least-significant.
     *
     * @param value The String value to convert.
     * @return String containing '0', '1', ...'F' characters which form hexadecimal equivalent of decoded String.
     */
    public static String binaryStringToHexString(String value) {
        int digits = (value.length() + 3) >> 2;
        char[] hexString = new char[digits + 2];
        hexString[0] = '0';
        hexString[1] = 'x';
        int index = value.length() - 1;
        for (int digit = 0; digit < digits; digit++) {
            int result = 0;
            int bitValue = 1;
            for (int bit = 0; bit < 4 && index >= 0; bit++) {
                if (value.charAt(index) == '1') {
                    result += bitValue;
                }
                bitValue <<= 1;
                index--;
            }
            hexString[digits - digit + 1] = HEX_DIGITS.charAt(result);
        }
        return new String(hexString);
    }

    /**
     * Translate String consisting of hexadecimal digits into String consisting of
     * corresponding binary digits ('1's and '0's).  No length limit.
     * String position 0 will have most-significant bit, position length-1 has least-significant.
     *
     * @param value String containing '0', '1', ...'f'
     *              characters which form hexadecimal.  Letters may be either upper or lower case.
     *              Works either with or without leading "Ox".
     * @return String with equivalent value in binary.
     */
    public static String hexStringToBinaryString(String value) {
        StringBuilder result = new StringBuilder();
        // ignore leading 0x or 0X
        int firstDigit = (value.startsWith("0x") || value.startsWith("0X")) ? 2 : 0;
        for (int digit = firstDigit; digit < value.length(); digit++) {
            switch (value.charAt(digit)) {
                case '0' -> result.append("0000");
                case '1' -> result.append("0001");
                case '2' -> result.append("0010");
                case '3' -> result.append("0011");
                case '4' -> result.append("0100");
                case '5' -> result.append("0101");
                case '6' -> result.append("0110");
                case '7' -> result.append("0111");
                case '8' -> result.append("1000");
                case '9' -> result.append("1001");
                case 'a', 'A' -> result.append("1010");
                case 'b', 'B' -> result.append("1011");
                case 'c', 'C' -> result.append("1100");
                case 'd', 'D' -> result.append("1101");
                case 'e', 'E' -> result.append("1110");
                case 'f', 'F' -> result.append("1111");
            }
        }
        return result.toString();
    }

    /**
     * Returns a 6 character string representing the 16-bit hexadecimal equivalent of the
     * given integer value.  First two characters are "0x".  It assumes value will "fit"
     * in 16 bits.  If non-negative, prepend leading zeroes to that string as necessary
     * to make it always four hexadecimal digits.  If negative, chop off the first
     * four 'f' digits so result is always four hexadecimal digits
     *
     * @param value The int value to convert.
     * @return String containing '0', '1', ...'f' which form hexadecimal equivalent of int.
     */
    public static String intToHalfHexString(int value) {
        return "0x%04x".formatted(value & 0xFFFF);
    }

    /**
     * Prefix a hexadecimal-indicating string "0x" to the string which is
     * returned by the method {@link Integer#toHexString(int)}. Prepend leading zeroes
     * to that string as necessary to make it always eight hexadecimal digits.
     *
     * @param value The int value to convert.
     * @return String containing '0', '1', ...'f' which form hexadecimal equivalent of int.
     */
    public static String intToHexString(int value) {
        return "0x%08x".formatted(value);
    }

    /**
     * Prefix a hexadecimal-indicating string "0x" to the string equivalent to the
     * hexadecimal value in the long parameter. Prepend leading zeroes
     * to that string as necessary to make it always sixteen hexadecimal digits.
     *
     * @param value The long value to convert.
     * @return String containing '0', '1', ...'F' which form hexadecimal equivalent of long.
     */
    public static String longToHexString(long value) {
        return "0x%016x".formatted(value);
    }

    /**
     * Produce ASCII string equivalent of integer value, interpreting it as 4 one-byte
     * characters.  If the value in a given byte does not correspond to a printable
     * character, it will be assigned a default character (defined in config.properties)
     * for a placeholder.
     *
     * @param value The int value to interpret
     * @return String that represents ASCII equivalent
     */
    public static String intToAscii(int value) {
        StringBuilder result = new StringBuilder(8);
        for (int byteIndex = 3; byteIndex >= 0; byteIndex--) {
            int byteValue = getByte(value, byteIndex);
            result.append((byteValue < Application.ASCII_TABLE.length) ? Application.ASCII_TABLE[byteValue] : Application.ASCII_NON_PRINT);
        }
        return result.toString();
    }

    /**
     * Convert the given string to a 32 bit integer, accounting for radix prefixes.
     * Allows optional negative (-) sign, but no spaces. May be unsigned.
     *
     * @param string The string to decode.
     * @return Integer value represented by given string.
     * @throws NumberFormatException Thrown if string cannot be translated into an integer.
     * @see Integer#decode(String)
     */
    public static int decodeInteger(String string) throws NumberFormatException {
        try {
            // First, try Integer.decode(), which works for most formats, but gives up
            // if the string represents a signed negative integer in unsigned form
            return Integer.decode(string);
        }
        catch (NumberFormatException exception) {
            // Didn't decode properly, so perform further checks
            if (string.startsWith("0x") || string.startsWith("0X")) {
                // Try parsing it as an unsigned hexadecimal integer
                // If that doesn't work, the NumberFormatException propagates up to the caller
                return Integer.parseUnsignedInt(string.substring(2), 16);
            }
            else {
                // Try parsing it as an unsigned decimal integer
                // If that doesn't work, the NumberFormatException propagates up to the caller
                return Integer.parseUnsignedInt(string, 10);
            }
        }
    }

    /**
     * Convert the given string to a 64 bit integer, accounting for radix prefixes.
     * Allows optional negative (-) sign, but no spaces. May be unsigned.
     *
     * @param string The string to decode.
     * @return Integer value represented by given string.
     * @throws NumberFormatException Thrown if string cannot be translated into an integer.
     * @see Long#decode(String)
     */
    public static long decodeLong(String string) throws NumberFormatException {
        try {
            // First, try Long.decode(), which works for most formats, but gives up
            // if the string represents a signed negative integer in unsigned form
            return Long.decode(string);
        }
        catch (NumberFormatException exception) {
            // Didn't decode properly, so perform further checks
            if (string.startsWith("0x") || string.startsWith("0X")) {
                // Try parsing it as an unsigned hexadecimal integer
                // If that doesn't work, the NumberFormatException propagates up to the caller
                return Long.parseUnsignedLong(string.substring(2), 16);
            }
            else {
                // Try parsing it as an unsigned decimal integer
                // If that doesn't work, the NumberFormatException propagates up to the caller
                return Long.parseUnsignedLong(string, 10);
            }
        }
    }

    /**
     * Returns int representing the bit values of the high order 32 bits of given
     * 64 bit long value.
     *
     * @param longValue The long value from which to extract bits.
     * @return int containing high order 32 bits of argument
     */
    public static int highOrderLongToInt(long longValue) {
        return (int) (longValue >> 32);  // high order 32 bits
    }

    /**
     * Returns int representing the bit values of the low order 32 bits of given
     * 64 bit long value.
     *
     * @param longValue The long value from which to extract bits.
     * @return int containing low order 32 bits of argument
     */
    public static int lowOrderLongToInt(long longValue) {
        return (int) (longValue << 32 >> 32);  // low order 32 bits
    }

    /**
     * Returns long (64 bit integer) combining the bit values of two given 32 bit
     * integer values.
     *
     * @param highOrder Integer to form the high-order 32 bits of result.
     * @param lowOrder  Integer to form the high-order 32 bits of result.
     * @return long containing concatenated 32 bit int values.
     */
    public static long twoIntsToLong(int highOrder, int lowOrder) {
        return (Integer.toUnsignedLong(highOrder) << 32) | Integer.toUnsignedLong(lowOrder);
    }

    /**
     * Returns the bit value of the given bit position of the given int value.
     *
     * @param value The value to read the bit from.
     * @param bit   bit position in range 0 (least significant) to 31 (most)
     * @return 0 if the bit position contains 0, and 1 otherwise.
     */
    public static int bitValue(int value, int bit) {
        return 1 & (value >> bit);
    }

    /**
     * Returns the bit value of the given bit position of the given long value.
     *
     * @param value The value to read the bit from.
     * @param bit   bit position in range 0 (least significant) to 63 (most)
     * @return 0 if the bit position contains 0, and 1 otherwise.
     */
    public static int bitValue(long value, int bit) {
        return (int) (1L & (value >> bit));
    }

    /**
     * Sets the specified bit of the specified value to 1, and returns the result.
     *
     * @param value The value in which the bit is to be set.
     * @param bit   bit position in range 0 (least significant) to 31 (most)
     * @return value possibly modified with given bit set to 1.
     */
    public static int setBit(int value, int bit) {
        return value | (1 << bit);
    }

    /**
     * Sets the specified bit of the specified value to 0, and returns the result.
     *
     * @param value The value in which the bit is to be set.
     * @param bit   bit position in range 0 (least significant) to 31 (most)
     * @return value possibly modified with given bit set to 0.
     */
    public static int clearBit(int value, int bit) {
        return value & ~(1 << bit);
    }

    /**
     * Sets the specified byte of the specified value to the low order 8 bits of
     * specified replacement value, and returns the result.
     *
     * @param value   The value in which the byte is to be set.
     * @param bite    Byte position in range 0 (least significant) to 3 (most).
     * @param replace Value to place into that byte position - use low order 8 bits.
     * @return Modified value.
     * @author DPS 12 July 2006
     */
    public static int setByte(int value, int bite, int replace) {
        return value & ~(0xFF << (bite << 3)) | ((replace & 0xFF) << (bite << 3));
    }

    /**
     * Gets the specified byte of the specified value.
     *
     * @param value The value in which the byte is to be retrieved.
     * @param bite  Byte position in range 0 (least significant) to 3 (most)
     * @return Zero-extended byte value in low order byte.
     * @author DPS 12 July 2006
     */
    public static int getByte(int value, int bite) {
        return value << ((3 - bite) << 3) >>> 24;
    }

    /**
     * Parsing method to see if a string represents a hex number.
     * As per {@link Integer#decode(String)},
     * a string represents a hex number if the string is in the forms:
     * <ul>
     * <li><code>Signopt "0x" HexDigits</code>
     * <li><code>Signopt "0X" HexDigits</code>
     * <li><code>Signopt "#" HexDigits</code> (not allowed here, as <code>"#"</code> starts a MIPS comment)
     * </ul>
     *
     * @param string String containing numeric digits (could be decimal, octal, or hex)
     * @return Returns <code>true</code> if string represents a hex number, else returns <code>false</code>.
     * @author KENV 1/4/05
     */
    public static boolean isHex(String string) {
        try {
            // Don't care about return value, just whether it threw exception.
            // If value is EITHER a valid int OR a valid long, continue.
            try {
                Binary.decodeInteger(string);
            }
            catch (NumberFormatException exception) {
                Binary.decodeLong(string);
            }

            // Sign is optional but if present can only be -
            if (string.charAt(0) == '-' && string.charAt(1) == '0' && Character.toUpperCase(string.charAt(1)) == 'X') {
                // Form is Sign 0x HexDigits and the entire string is parseable as a number
                return true;
            }
            else if (string.charAt(0) == '0' && Character.toUpperCase(string.charAt(1)) == 'X') {
                // Form is 0x HexDigits and the entire string is parseable as a number
                return true;
            }
            else {
                return false;
            }
        }
        catch (StringIndexOutOfBoundsException | NumberFormatException exception) {
            return false;
        }
    }

    /**
     * Parsing method to see if a string represents an octal number.
     * As per {@link Integer#decode(String)},
     * a string represents an octal number if the string is in the forms:
     * <ul>
     * <li><code>Signopt "0" OctalDigits</code>
     * </ul>
     *
     * @param string String containing numeric digits (could be decimal, octal, or hex)
     * @return Returns <code>true</code> if string represents an octal number, else returns <code>false</code>.
     * @author KENV 1/4/05
     */
    public static boolean isOctal(String string) {
        try {
            // Don't care about return value, just whether it threw exception.
            // If value is EITHER a valid int OR a valid long, continue.
            try {
                Binary.decodeInteger(string);
            }
            catch (NumberFormatException exception) {
                Binary.decodeLong(string);
            }

            // Don't mistake "0" or a string that starts "0x" for an octal string
            if (isHex(string)) {
                return false; // String starts with "0" but continues "0x", so not octal
            }

            // Sign is optional but if present can only be -
            // Has to have more digits than the leading zero
            // The condition originally had string.length() > 1, but I think 2 was the intent.
            // - Sean Clarke 03/2024
            if (string.charAt(0) == '-' && string.charAt(1) == '0' && string.length() > 2) {
                // Form is Sign 0 OctalDigits
                return true;
            }
            else if (string.charAt(0) == '0' && string.length() > 1) {
                // Form is 0 OctalDigits
                return true;
            }
            else {
                return false;
            }
        }
        catch (StringIndexOutOfBoundsException | NumberFormatException exception) {
            return false;
        }
    }
}
