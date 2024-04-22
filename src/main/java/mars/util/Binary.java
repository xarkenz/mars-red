package mars.util;

import mars.Application;

import java.util.Arrays;

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
    private static final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    // Use this to produce String equivalent of unsigned int value (add it to int value, result is long)
    private static final long UNSIGNED_BASE = 0x7FFFFFFFL + 0x7FFFFFFFL + 2L; //0xFFFFFFFF+1

    /**
     * Translate int value into a String consisting of '1's and '0's.
     *
     * @param value  The int value to convert.
     * @param length The number of bit positions, starting at least significant, to process.
     * @return String consisting of '1' and '0' characters corresponding to the requested binary sequence.
     */
    public static String intToBinaryString(int value, int length) {
        char[] result = new char[length];
        int index = length - 1;
        for (int i = 0; i < length; i++) {
            result[index] = (bitValue(value, i) == 1) ? '1' : '0';
            index--;
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
     * Translate long value into a String consisting of '1's and '0's.
     *
     * @param value  The long value to convert.
     * @param length The number of bit positions, starting at least significant, to process.
     * @return String consisting of '1' and '0' characters corresponding to the requested binary sequence.
     */
    public static String longToBinaryString(long value, int length) {
        char[] result = new char[length];
        int index = length - 1;
        for (int i = 0; i < length; i++) {
            result[index] = (bitValue(value, i) == 1) ? '1' : '0';
            index--;
        }
        return new String(result);
    }

    /**
     * Translate long value into a String consisting of '1's and '0's.  Assumes all 64 bits are
     * to be translated.
     *
     * @param value The long value to convert.
     * @return String consisting of '1' and '0' characters corresponding to the requested binary sequence.
     */
    public static String longToBinaryString(long value) {
        return longToBinaryString(value, Long.SIZE);
    }

    /**
     * Translate String consisting of '1's and '0's into an int value having that binary representation.
     * The String is assumed to be at most 32 characters long.  No error checking is performed.
     * String position 0 has most-significant bit, position length-1 has least-significant.
     *
     * @param value The String value to convert.
     * @return int whose binary value corresponds to decoded String.
     */
    public static int binaryStringToInt(String value) {
        int result = value.charAt(0) - '0';
        for (int i = 1; i < value.length(); i++) {
            result = (result << 1) | (value.charAt(i) - '0');
        }
        return result;
    }

    /**
     * Translate String consisting of '1's and '0's into a long value having that binary representation.
     * The String is assumed to be at most 64 characters long.  No error checking is performed.
     * String position 0 has most-significant bit, position length-1 has least-significant.
     *
     * @param value The String value to convert.
     * @return long whose binary value corresponds to decoded String.
     */
    public static long binaryStringToLong(String value) {
        long result = value.charAt(0) - '0';
        for (int i = 1; i < value.length(); i++) {
            result = (result << 1) | (value.charAt(i) - '0');
        }
        return result;
    }

    /**
     * Translate String consisting of '1's and '0's into String equivalent of the corresponding
     * hexadecimal value.  No length limit.
     * String position 0 has most-significant bit, position length-1 has least-significant.
     *
     * @param value The String value to convert.
     * @return String containing '0', '1', ...'F' characters which form hexadecimal
     *     equivalent of decoded String.
     */
    public static String binaryStringToHexString(String value) {
        int digits = (value.length() + 3) / 4;
        char[] hexChars = new char[digits + 2];
        int position, result, pow, rep;
        hexChars[0] = '0';
        hexChars[1] = 'x';
        position = value.length() - 1;
        for (int digit = 0; digit < digits; digit++) {
            result = 0;
            pow = 1;
            rep = 0;
            while (rep < 4 && position >= 0) {
                if (value.charAt(position) == '1') {
                    result = result + pow;
                }
                pow *= 2;
                position--;
                rep++;
            }
            hexChars[digits - digit + 1] = hexDigits[result];
        }
        return new String(hexChars);
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
     * Translate String consisting of '1's and '0's into char equivalent of the corresponding
     * hexadecimal digit.  String limited to length 4.
     * String position 0 has most-significant bit, position length-1 has least-significant.
     *
     * @param value The String value to convert.
     * @return char '0', '1', ...'F' which form hexadecimal equivalent of decoded String.
     *     If string length > 4, returns '0'.
     */
    public static char binaryStringToHexDigit(String value) {
        if (value.length() > 4) {
            return '0';
        }
        int result = 0;
        int pow = 1;
        for (int i = value.length() - 1; i >= 0; i--) {
            if (value.charAt(i) == '1') {
                result = result + pow;
            }
            pow *= 2;
        }
        return hexDigits[result];
    }

    /**
     * Prefix a hexadecimal-indicating string "0x" to the string which is
     * returned by the method {@link Integer#toHexString(int)}. Prepend leading zeroes
     * to that string as necessary to make it always eight hexadecimal digits.
     *
     * @param value The int value to convert.
     * @return String containing '0', '1', ...'F' which form hexadecimal equivalent of int.
     */
    public static String intToHexString(int value) {
        StringBuilder hexString = new StringBuilder(Integer.toHexString(value));
        while (hexString.length() < 8) {
            hexString.insert(0, '0');
        }

        hexString.insert(0, "0x");
        return hexString.toString();
    }

    /**
     * Returns a 6 character string representing the 16-bit hexadecimal equivalent of the
     * given integer value.  First two characters are "0x".  It assumes value will "fit"
     * in 16 bits.  If non-negative, prepend leading zeroes to that string as necessary
     * to make it always four hexadecimal digits.  If negative, chop off the first
     * four 'f' digits so result is always four hexadecimal digits
     *
     * @param value The int value to convert.
     * @return String containing '0', '1', ...'F' which form hexadecimal equivalent of int.
     */
    public static String intToHalfHexString(int value) {
        StringBuilder hexString = new StringBuilder(Integer.toHexString(value));
        if (hexString.length() > 4) {
            hexString.delete(4, hexString.length());
        }
        while (hexString.length() < 4) {
            hexString.insert(0, '0');
        }

        hexString.insert(0, "0x");
        return hexString.toString();
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
        return binaryStringToHexString(longToBinaryString(value));
    }

    /**
     * Produce String equivalent of integer value interpreting it as an unsigned integer.
     * For instance,  -1 (0xffffffff) produces "4294967295" instead of "-1".
     *
     * @param d The int value to interpret.
     * @return String which forms unsigned 32 bit equivalent of int.
     */
    public static String unsignedIntToIntString(int d) {
        return (d >= 0) ? Integer.toString(d) : Long.toString(UNSIGNED_BASE + d);
    }

    /**
     * Produce ASCII string equivalent of integer value, interpreting it as 4 one-byte
     * characters.  If the value in a given byte does not correspond to a printable
     * character, it will be assigned a default character (defined in config.properties)
     * for a placeholder.
     *
     * @param d The int value to interpret
     * @return String that represents ASCII equivalent
     */
    public static String intToAscii(int d) {
        StringBuilder result = new StringBuilder(8);
        for (int i = 3; i >= 0; i--) {
            int byteValue = getByte(d, i);
            result.append((byteValue < Application.ASCII_TABLE.length) ? Application.ASCII_TABLE[byteValue] : Application.ASCII_NON_PRINT);
        }
        return result.toString();
    }

    /**
     * Attempt to validate given string whose characters represent a 32 bit integer.
     * Integer.decode() is insufficient because it will not allow incorporation of
     * hex two's complement (i.e. 0x80...0 through 0xff...f).  Allows
     * optional negative (-) sign but no embedded spaces.
     *
     * @param string candidate string
     * @return returns int value represented by given string
     * @throws NumberFormatException if string cannot be translated into an int
     */
    public static int stringToInt(String string) throws NumberFormatException {
        int result;
        // First, use Integer.decode().  This will validate most, but it flags
        // valid hex two's complement values as exceptions.  We'll catch those and
        // do our own validation.
        try {
            result = Integer.decode(string);
        }
        catch (NumberFormatException nfe) {
            // Multistep process toward validation of hex two's complement. 3-step test:
            //   (1) exactly 10 characters long,
            //   (2) starts with Ox or 0X,
            //   (3) last 8 characters are valid hex digits.
            string = string.toLowerCase();
            if (string.length() == 10 && string.startsWith("0x")) {
                StringBuilder bitString = new StringBuilder();
                int index;
                // while testing characters, build bit string to set up for binaryStringToInt
                for (int i = 2; i < 10; i++) {
                    index = Arrays.binarySearch(hexDigits, string.charAt(i));
                    if (index < 0) {
                        throw new NumberFormatException();
                    }
                    bitString.append(intToBinaryString(index, 4));
                }
                result = binaryStringToInt(bitString.toString());
            }
            // The following "else" composed by Jose Baiocchi Paredes, Oct 2009.  This new code
            // will correctly translate a string representing an unsigned decimal (not hex)
            // value whose signed value is negative.  This is the decimal equivalent of the
            // "then" case just above.  The method was not used in this context until Release 3.6
            // when background highlighting of the Data Segment was added.  Caused exceptions
            // under certain conditions.
            else if (!string.startsWith("0x")) {
                result = 0;
                for (int i = 0; i < string.length(); i++) {
                    char c = string.charAt(i);
                    if ('0' <= c && c <= '9') {
                        result *= 10;
                        result += c - '0';
                    }
                    else {
                        throw new NumberFormatException();
                    }
                }
            }
            // End of the Jose Paredes code
            else {
                throw new NumberFormatException();
            }
        }
        return result;
    }

    /**
     * Attempt to validate given string whose characters represent a 64 bit long.
     * Long.decode() is insufficient because it will not allow incorporation of
     * hex two's complement (i.e. 0x80...0 through 0xff...f).  Allows
     * optional negative (-) sign but no embedded spaces.
     *
     * @param string candidate string
     * @return returns long value represented by given string
     * @throws NumberFormatException if string cannot be translated into a long
     */
    public static long stringToLong(String string) throws NumberFormatException {
        long result;
        // First, use Long.decode().  This will validate most, but it flags
        // valid hex two's complement values as exceptions.  We'll catch those and
        // do our own validation.
        try {
            result = Long.decode(string);
        }
        catch (NumberFormatException nfe) {
            // Multistep process toward validation of hex two's complement. 3-step test:
            //   (1) exactly 18 characters long,
            //   (2) starts with Ox or 0X,
            //   (3) last 16 characters are valid hex digits.
            string = string.toLowerCase();
            if (string.length() == 18 && string.startsWith("0x")) {
                StringBuilder bitString = new StringBuilder();
                int index;
                // while testing characters, build bit string to set up for binaryStringToInt
                for (int i = 2; i < 18; i++) {
                    index = Arrays.binarySearch(hexDigits, string.charAt(i));
                    if (index < 0) {
                        throw new NumberFormatException();
                    }
                    bitString.append(intToBinaryString(index, 4));
                }
                result = binaryStringToLong(bitString.toString());
            }
            else {
                throw new NumberFormatException();
            }
        }
        return result;
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
     * <li>{@code Signopt "0x" HexDigits}
     * <li>{@code Signopt "0X" HexDigits}
     * <li>{@code Signopt "#" HexDigits} (not allowed here, as {@code "#"} starts a MIPS comment)
     * </ul>
     *
     * @param string String containing numeric digits (could be decimal, octal, or hex)
     * @return Returns {@code true} if string represents a hex number, else returns {@code false}.
     * @author KENV 1/4/05
     */
    public static boolean isHex(String string) {
        try {
            // don't care about return value, just whether it threw exception.
            // If value is EITHER a valid int OR a valid long, continue.
            try {
                Binary.stringToInt(string);
            }
            catch (NumberFormatException nfe) {
                try {
                    Binary.stringToLong(string);
                }
                catch (NumberFormatException e) {
                    return false; // both failed; it is neither valid int nor long
                }
            }

            if ((string.charAt(0) == '-') && // sign is optional but if present can only be -
                (string.charAt(1) == '0') && (Character.toUpperCase(string.charAt(1)) == 'X')) {
                return true; // Form is Sign 0x.... and the entire string is parseable as a number
            }
            else if ((string.charAt(0) == '0') && (Character.toUpperCase(string.charAt(1)) == 'X')) {
                return true; // Form is 0x.... and the entire string is parseable as a number
            }
            else {
                return false;
            }
        }
        catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Parsing method to see if a string represents an octal number.
     * As per {@link Integer#decode(String)},
     * a string represents an octal number if the string is in the forms:
     * <ul>
     * <li>{@code Signopt "0" OctalDigits}
     * </ul>
     *
     * @param string String containing numeric digits (could be decimal, octal, or hex)
     * @return Returns {@code true} if string represents an octal number, else returns {@code false}.
     * @author KENV 1/4/05
     */
    public static boolean isOctal(String string) {
        try {
            // We don't care what value Binary.stringToInt(v) returns, just whether it threw exception
            Binary.stringToInt(string);

            // Don't mistake "0" or a string that starts "0x" for an octal string
            if (isHex(string)) {
                return false; // String starts with "0" but continues "0x", so not octal
            }

            // The condition originally had string.length() > 1, but I think 2 was the intent.
            // - Sean Clarke 03/2024
            if (string.charAt(0) == '-' && // Sign is optional but if present can only be -
                string.charAt(1) == '0' && string.length() > 2) // Has to have more digits than the leading zero
            {
                return true; // Form is Sign 0 OctalDigits
            }
            else {
                // Form is 0 OctalDigits
                // Has to have more digits than the leading zero
                return string.charAt(0) == '0' && string.length() > 1;
            }
        }
        catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            return false;
        }
    }
}
