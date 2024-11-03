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
import mars.assembler.log.SourceLocation;

/**
 * Represents one token in the input MIPS program.  Each Token carries, along with its
 * type and value, the position (line, column) in which its source appears in the MIPS program.
 *
 * @author Pete Sanderson, August 2003; Sean Clarke, June 2024
 */
public class Token {
    private TokenType type;
    private Object value;
    private final String literal;
    private final SourceLocation location;

    /**
     * Create a new <code>Token</code> with the given information.
     *
     * @param location The location of this token in the source code.
     * @param literal  The underlying fragment of source code.
     * @param type     The type of this token.
     * @param value    The "value" of this token, dependent on <code>type</code>.
     * @see TokenType
     */
    public Token(SourceLocation location, String literal, TokenType type, Object value) {
        this.type = type;
        this.value = value;
        this.literal = literal;
        this.location = location;
    }

    /**
     * Get the type of this token.
     *
     * @return The type of this token.
     */
    public TokenType getType() {
        return this.type;
    }

    /**
     * Modify the type of this token. This is typically used when the parsing stage of assembly reinterprets
     * the token type, e.g. using an operator mnemonic as a label identifier.
     * <p>
     * Note: {@link #setValue(Object)} should be used alongside this method to ensure the token value remains valid.
     *
     * @param type The new type for this token.
     */
    public void setType(TokenType type) {
        this.type = type;
    }

    /**
     * Get the "value" this token represents, if any. The object returned by this method is dependent on this token's
     * {@link #getType() type}; the documentation for each variant of {@link TokenType} indicates what to expect
     * from this method.
     *
     * @return The token type-dependent value for this token.
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * Modify the "value" this token represents. The new value must follow the guidelines of this token's
     * {@link #getType() type}.
     *
     * @param value The new value for this token.
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Get the underlying fragment of source code this token was created from.
     *
     * @return The literal form of this token.
     */
    public String getLiteral() {
        return this.literal;
    }

    /**
     * Get the location of this token in the source code.
     *
     * @return Name of file associated with this token.
     */
    public SourceLocation getLocation() {
        return this.location;
    }

    /**
     * Get the string representation of this token. This method is equivalent to {@link #getLiteral()}.
     *
     * @return The literal form of this token.
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
        return this.type == TokenType.IDENTIFIER && this.literal.length() > 1 && this.literal.charAt(0) == '$';
    }

    /**
     * Convert this token to an {@link Operand}, if possible. Only registers, integers, and characters
     * can be converted using this method.
     *
     * @return An operand based on the value of this token, or <code>null</code> if the conversion fails.
     */
    public Operand toOperand() {
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

    /**
     * Create a copy of this token.
     *
     * @return A new instance with identical data.
     */
    public Token copy() {
        return new Token(
            this.location,
            this.literal,
            this.type,
            this.value
        );
    }
}

