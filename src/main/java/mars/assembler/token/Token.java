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

import mars.assembler.Operand;
import mars.assembler.OperandType;

/**
 * Represents one token in the input MIPS program.  Each Token carries, along with its
 * type and value, the position (line, column) in which its source appears in the MIPS program.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public class Token {
    private TokenType type;
    private Object value;
    private final String literal;
    private final String sourceFilename;
    private final int lineIndex;
    private final int columnIndex;
    // Original program and line will differ from the above if token was defined in an included file
    private Token originalToken;

    /**
     * Constructor for Token class.
     *
     * @param type     The token type that this token has. (e.g. REGISTER_NAME)
     * @param value
     * @param literal  The source literal for this token. (e.g. $t3)
     * @param filename The name of the file containing this token.
     * @param line     The line number in source program in which this token appears.
     * @param column   The starting position in that line number of this token's source value.
     */
    public Token(TokenType type, Object value, String literal, String filename, int line, int column) {
        this.type = type;
        this.value = value;
        this.literal = literal;
        this.sourceFilename = filename;
        this.lineIndex = line;
        this.columnIndex = column;
        this.originalToken = this;
    }

    /**
     * Produces token type of this token.
     *
     * @return TokenType of this token.
     */
    public TokenType getType() {
        return this.type;
    }

    /**
     * Set or modify token type.  Generally used to note that
     * an identifier that matches an instruction name is
     * actually being used as a label.
     *
     * @param type New type for this token.
     */
    public void setType(TokenType type) {
        this.type = type;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Produces source code of this token.
     *
     * @return String containing source code of this token.
     */
    public String getLiteral() {
        return this.literal;
    }

    /**
     * Produces name of file associated with this token.
     *
     * @return Name of file associated with this token.
     */
    public String getFilename() {
        return this.sourceFilename;
    }

    /**
     * Get the line at which this token resides, where the first line in the file is line 0.
     *
     * @return The zero-based line index.
     */
    public int getLineIndex() {
        return this.lineIndex;
    }

    /**
     * Get the column at which this token starts in the source line, where the beginning of the line is column 0.
     *
     * @return The zero-based column index.
     */
    public int getColumnIndex() {
        return this.columnIndex;
    }

    /**
     * Get the original token that this token derives from. Returns <code>this</code> unless the token:
     * <ul>
     * <li>Has been included from another file (<code>.include</code>).
     * <li>Has been substituted as a result of an equivalence (<code>.eqv</code>).
     * <li>Has been generated from an instance of a macro (<code>.macro</code>).
     * </ul>
     *
     * @return The original form of this token.
     */
    public Token getOriginalToken() {
        return this.originalToken;
    }

    /**
     * Set the original token that this token derives from. Tokens may be cloned during preprocessing as a result
     * of macros, <code>.eqv</code>, and <code>.include</code>, and information about that process must be kept
     * for later reference. For example, error messages and the text segment display check this information.
     *
     * @param token The original form of this token.
     */
    public void setOriginalToken(Token token) {
        this.originalToken = token;
    }

    /**
     * Get a String representing the token.  This method is
     * equivalent to {@link #getLiteral()}.
     *
     * @return String version of the token.
     */
    @Override
    public String toString() {
        return this.getLiteral();
    }

    /**
     * Determine whether this token is a valid SPIM-style macro parameter. MARS-style macro parameters start with
     * <code>%</code>, whereas SPIM-style macro parameters start with <code>$</code>. Note that register names
     * such as <code>$zero</code> are not accepted.
     *
     * @return <code>true</code> if this token is an {@link TokenType#IDENTIFIER IDENTIFIER} in the form of
     *         <code>$</code> followed by at least one character, or <code>false</code> otherwise.
     */
    public boolean isSPIMStyleMacroParameter() {
        return this.type == TokenType.IDENTIFIER || this.literal.length() <= 1 || this.literal.charAt(0) != '$';
    }

    /**
     * Convert this token to an {@link Operand}, if possible. Only registers, integers, and characters
     * can be converted using this method.
     *
     * @return An operand based on the value of this token, or <code>null</code> if the conversion fails.
     */
    public Operand asOperand() {
        TokenType type = this.type;

        // Character literals should just be treated as plain integers
        if (type == TokenType.CHARACTER) {
            type = TokenType.fromIntegerValue((Integer) this.value);
        }

        return switch (type) {
            case REGISTER_NUMBER, REGISTER_NAME -> new Operand(OperandType.REGISTER, (Integer) this.value);
            case FP_REGISTER_NAME -> new Operand(OperandType.FP_REGISTER, (Integer) this.value);
            case INTEGER_3_UNSIGNED -> new Operand(OperandType.INTEGER_3_UNSIGNED, (Integer) this.value);
            case INTEGER_5_UNSIGNED -> new Operand(OperandType.INTEGER_5_UNSIGNED, (Integer) this.value);
            case INTEGER_15_UNSIGNED -> new Operand(OperandType.INTEGER_15_UNSIGNED, (Integer) this.value);
            case INTEGER_16_SIGNED -> new Operand(OperandType.INTEGER_16_SIGNED, (Integer) this.value);
            case INTEGER_16_UNSIGNED -> new Operand(OperandType.INTEGER_16_UNSIGNED, (Integer) this.value);
            case INTEGER_32 -> new Operand(OperandType.INTEGER_32, (Integer) this.value);
            default -> null;
        };
    }
}

