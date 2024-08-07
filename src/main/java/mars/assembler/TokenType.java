package mars.assembler;

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

/**
 * Enumeration to identify the types of tokens found in MIPS programs.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public enum TokenType {
    ERROR,
    COMMENT,
    DIRECTIVE,
    OPERATOR,
    DELIMITER,
    /*
     * note: REGISTER_NAME is token of form $zero whereas REGISTER_NUMBER is token
     * of form $0.  The former is part of extended assembler, and latter is part
     * of basic assembler.
     */
    REGISTER_NAME,
    REGISTER_NUMBER,
    FP_REGISTER_NAME,
    IDENTIFIER,
    LEFT_PAREN,
    RIGHT_PAREN,
    INTEGER_5,
    INTEGER_16,
    INTEGER_16U,
    INTEGER_32,
    REAL_NUMBER,
    CHARACTER,
    STRING,
    PLUS,
    MINUS,
    COLON,
    MACRO_PARAMETER;

    /**
     * Determine whether this token type is an integer (i.e. {@link #INTEGER_5}, {@link #INTEGER_16},
     * {@link #INTEGER_16U}, or {@link #INTEGER_32}).
     *
     * @return <code>true</code> if this is an integer type, or <code>false</code> otherwise.
     */
    public boolean isInteger() {
        return this == TokenType.INTEGER_5
            || this == TokenType.INTEGER_16
            || this == TokenType.INTEGER_16U
            || this == TokenType.INTEGER_32;
    }

    /**
     * Determine whether this token type is a floating-point number (i.e. {@link #REAL_NUMBER}).
     *
     * @return <code>true</code> if this is a floating-point type, or <code>false</code> otherwise.
     */
    public boolean isFloatingPoint() {
        return this == TokenType.REAL_NUMBER;
    }
}
