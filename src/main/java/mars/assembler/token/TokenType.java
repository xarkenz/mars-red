package mars.assembler.token;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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

import mars.assembler.Directive;

/**
 * Enumeration to identify the types of tokens found in MIPS programs.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public enum TokenType {
    /**
     * A token that is malformed and could not be tokenized properly. This is used primarily for syntax highlighting.
     * The corresponding token value is <code>null</code>.
     */
    ERROR,
    /**
     * A token representing a <i>visible</i> operand delimiter (i.e. comma). This is used primarily for syntax
     * highlighting. The corresponding token value is <code>null</code>.
     */
    DELIMITER,
    /**
     * A token representing an end-of-line comment starting with <code>#</code>. This is used primarily for syntax
     * highlighting. The corresponding token value is <code>null</code>.
     */
    COMMENT,
    /**
     * A token representing either a preprocessor directive (e.g. {@link Directive#INCLUDE .include})
     * or an assembler directive (e.g. {@link Directive#TEXT .text}). The corresponding token value is the variant of
     * {@link Directive} corresponding to the literal.
     */
    DIRECTIVE,
    /**
     * A token representing the mnemonic of an instruction (e.g. <code>lui</code>). The corresponding token value is
     * a {@link java.util.List List} of the {@link mars.mips.instructions.Instruction Instruction}s with that mnemonic.
     */
    OPERATOR,
    /**
     * A token representing a CPU or Coprocessor 0 register referenced by number (e.g. <code>$0</code>).
     * The corresponding token value is the register number as an {@link Integer}.
     */
    REGISTER_NUMBER,
    /**
     * A token representing a CPU register referenced by name (e.g. <code>$zero</code>).
     * The corresponding token value is the register number as an {@link Integer}.
     */
    REGISTER_NAME,
    /**
     * A token representing a Coprocessor 1 (FPU) register referenced by name (e.g. <code>$f0</code>).
     * The corresponding token value is the register number as an {@link Integer}.
     */
    FP_REGISTER_NAME,
    /**
     * A token representing an identifier (e.g. <code>label</code>).
     * The corresponding token value is <code>null</code>.
     */
    IDENTIFIER,
    /**
     * A token representing an unsigned integer which can fit in 3 bits. The corresponding token value is the
     * numeric value, zero-extended, as an {@link Integer}.
     */
    INTEGER_3_UNSIGNED,
    /**
     * A token representing an unsigned integer which can fit in 5 bits. The corresponding token value is the
     * numeric value, zero-extended, as an {@link Integer}.
     */
    INTEGER_5_UNSIGNED,
    /**
     * A token representing an unsigned integer which can fit in 15 bits. The purpose of this type is to identify
     * integers that fit within the ranges of both signed and unsigned 16-bit integers.
     * The corresponding token value is the numeric value, zero-extended, as an {@link Integer}.
     */
    INTEGER_15_UNSIGNED,
    /**
     * A token representing a signed integer which can fit in 16 bits. The corresponding token value is the
     * numeric value, sign-extended, as an {@link Integer}.
     */
    INTEGER_16_SIGNED,
    /**
     * A token representing an unsigned integer which can fit in 16 bits. The corresponding token value is the
     * numeric value, zero-extended, as an {@link Integer}.
     */
    INTEGER_16_UNSIGNED,
    /**
     * A token representing a 32-bit integer, signed or unsigned. The corresponding token value is the
     * numeric value as an {@link Integer}.
     */
    INTEGER_32,
    /**
     * A token representing a double-precision floating-point number. The corresponding token value is the
     * numeric value as a {@link Double}.
     */
    REAL_NUMBER,
    /**
     * A token representing a character literal. The corresponding token value is the
     * numeric value as an {@link Integer}.
     */
    CHARACTER,
    /**
     * A token representing a string literal. The corresponding token value is the
     * value of the string as a {@link String}.
     */
    STRING,
    /**
     * A token representing a plus (i.e. <code>+</code>).
     * The corresponding token value is <code>null</code>.
     */
    PLUS,
    /**
     * A token representing a minus (i.e. <code>-</code>).
     * The corresponding token value is <code>null</code>.
     */
    MINUS,
    /**
     * A token representing a colon (i.e. <code>:</code>).
     * The corresponding token value is <code>null</code>.
     */
    COLON,
    /**
     * A token representing a left parenthesis (i.e. <code>(</code>).
     * The corresponding token value is <code>null</code>.
     */
    LEFT_PAREN,
    /**
     * A token representing a right parenthesis (i.e. <code>)</code>).
     * The corresponding token value is <code>null</code>.
     */
    RIGHT_PAREN,
    /**
     * A token representing a macro parameter starting with <code>%</code> or <code>$</code> (e.g. <code>%arg</code>).
     * The corresponding token value is <code>null</code>.
     */
    MACRO_PARAMETER,
    /**
     * A token representing a template substitution wrapped in curly braces. The corresponding token value is
     * a {@link java.util.List List} of the {@link Token}s contained within the curly braces.
     */
    TEMPLATE_SUBSTITUTION;

    /**
     * Determine whether this token type is an integer (i.e. {@link #INTEGER_3_UNSIGNED}, {@link #INTEGER_5_UNSIGNED},
     * {@link #INTEGER_15_UNSIGNED} {@link #INTEGER_16_SIGNED}, {@link #INTEGER_16_UNSIGNED}, or {@link #INTEGER_32}).
     *
     * @return <code>true</code> if this is an integer type, or <code>false</code> otherwise.
     */
    public boolean isInteger() {
        return this == INTEGER_3_UNSIGNED
            || this == INTEGER_5_UNSIGNED
            || this == INTEGER_15_UNSIGNED
            || this == INTEGER_16_SIGNED
            || this == INTEGER_16_UNSIGNED
            || this == INTEGER_32;
    }

    /**
     * Determine whether this token type is a floating-point number (i.e. {@link #REAL_NUMBER}).
     *
     * @return <code>true</code> if this is a floating-point type, or <code>false</code> otherwise.
     */
    public boolean isFloatingPoint() {
        return this == REAL_NUMBER;
    }

    public boolean isDirectiveContinuation() {
        return this.isInteger() || this.isFloatingPoint() || this == CHARACTER || this == STRING;
    }

    /**
     * Determine the smallest integer {@link TokenType} which fits the given value, taking signedness into account.
     *
     * @param value The value to determine the type of.
     * @return The narrowest possible integer type.
     */
    public static TokenType fromIntegerValue(int value) {
        if (0 <= value && value < (1 << 3)) {
            return INTEGER_3_UNSIGNED;
        }
        else if (0 <= value && value < (1 << 5)) {
            return INTEGER_5_UNSIGNED;
        }
        else if (0 <= value && value < (1 << 15)) {
            return INTEGER_15_UNSIGNED;
        }
        else if (-(1 << 15) <= value && value < (1 << 15)) {
            return INTEGER_16_SIGNED;
        }
        else if (0 <= value && value < (1 << 16)) {
            return INTEGER_16_UNSIGNED;
        }
        else {
            return INTEGER_32;
        }
    }
}
