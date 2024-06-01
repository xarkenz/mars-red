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
    private final int sourceLine;
    private final int sourceColumn;
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
        this.sourceLine = line;
        this.sourceColumn = column;
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
     * @param type New TokenTypes for this token.
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
     * Produces name of file associated with this token.
     *
     * @return Name of file associated with this token.
     */
    public String getSourceFilename() {
        return this.sourceFilename;
    }

    /**
     * Produces line number of MIPS program of this token.
     *
     * @return Line number in source program of this token.
     */
    public int getSourceLine() {
        return this.sourceLine;
    }

    /**
     * Produces position within source line of this token.
     *
     * @return First character position within source program line of this token.
     */
    public int getSourceColumn() {
        return this.sourceColumn;
    }

    /**
     * Set original program and line number for this token.
     * Line number or both may change during pre-assembly as a result
     * of the ".include" directive, and we need to keep the original
     * for later reference (error messages, text segment display).
     *
     * @param token
     */
    public void setOriginalToken(Token token) {
        this.originalToken = token;
    }

    /**
     * Get the original form of this token. Returns <code>this</code> unless the token:
     * <ul>
     * <li>Has been included from another file (<code>.include</code>).
     * <li>Has been substituted as a result of an equivalence (<code>.eqv</code>).
     * <li>Has been generated from an instance of a macro (<code>.macro</code>).
     * </ul>
     *
     * @return Original form of this token.
     */
    public Token getOriginalToken() {
        return this.originalToken;
    }
}

