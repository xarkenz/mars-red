package mars.assembler;

import mars.assembler.syntax.DirectiveSyntax;
import mars.assembler.token.Token;
import mars.assembler.token.TokenType;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.util.Binary;
import mars.util.StringTrie;

import java.util.*;

/*
Copyright (c) 2003-2012,  Pete Sanderson and Kenneth Vollmar

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
 * Class representing MIPS assembler directives.
 * The directive name is indicative of the directive it represents.  For example, DATA
 * represents the MIPS .data directive.
 *
 * @author Pete Sanderson, August 2003
 */
public enum Directive {
    DATA(
        ".data",
        "Values following this directive are stored in the static data region of memory. "
            + "To store values, use storage directives such as \".word\" and \".space\". "
            + "An optional operand can specify the starting address, but this feature should be used with caution.",
        false,
        (syntax, assembler) -> {
            assembler.setSegment(assembler.dataSegment);
            if (!syntax.getContent().isEmpty() && syntax.getContent().get(0).getType().isInteger()) {
                assembler.getSegment().setAddress((Integer) syntax.getContent().get(0).getValue()); // KENV 1/6/05
            }
        }
    ),
    TEXT(
        ".text",
        "Instructions following this directive are stored in the text region of memory. "
            + "An optional operand can specify the starting address, but this feature should be used with caution.",
        false,
        (syntax, assembler) -> {
            assembler.setSegment(assembler.textSegment);
            if (!syntax.getContent().isEmpty() && syntax.getContent().get(0).getType().isInteger()) {
                assembler.getSegment().setAddress((Integer) syntax.getContent().get(0).getValue()); // KENV 1/6/05
            }
        }
    ),
    KDATA(
        ".kdata",
        "Values following this directive are stored in the kernel data region of memory. "
            + "To store values, use storage directives such as \".word\" and \".space\". "
            + "An optional operand can specify the starting address.",
        false,
        (syntax, assembler) -> {
            assembler.setSegment(assembler.kernelDataSegment);
            if (!syntax.getContent().isEmpty() && syntax.getContent().get(0).getType().isInteger()) {
                assembler.getSegment().setAddress((Integer) syntax.getContent().get(0).getValue()); // KENV 1/6/05
            }
        }
    ),
    KTEXT(
        ".ktext",
        "Instructions following this directive are stored in the kernel text region of memory. "
            + "An optional operand can specify the starting address (to create an exception handler, for instance).",
        false,
        (syntax, assembler) -> {
            assembler.setSegment(assembler.kernelTextSegment);
            if (!syntax.getContent().isEmpty() && syntax.getContent().get(0).getType().isInteger()) {
                assembler.getSegment().setAddress((Integer) syntax.getContent().get(0).getValue()); // KENV 1/6/05
            }
        }
    ),
    BYTE(
        ".byte",
        "Store 8-bit byte(s) specified as operands. Operands can take the form of an integer value, or "
            + "\"value : count\" to repeat a value.",
        true,
        new NumericStorageFunction(false, 1)
    ),
    HALF(
        ".half",
        "Store 16-bit halfword(s) specified as operands. Operands can take the form of an integer value, or "
            + "\"value : count\" to repeat a value. Aligns values to halfword boundaries by default.",
        true,
        new NumericStorageFunction(false, Memory.BYTES_PER_HALFWORD)
    ),
    WORD(
        ".word",
        "Store 32-bit word(s) specified as operands. Operands can take the form of an integer value, "
        + "a label, or \"value : count\" to repeat a value. Aligns values to word boundaries by default.",
        true,
        new NumericStorageFunction(false, Memory.BYTES_PER_WORD)
    ),
    FLOAT(
        ".float",
        "Store single-precision floating-point number(s) specified as operands. Operands can take the form "
            + "of a numeric value, \"Infinity\", \"NaN\", or \"value : count\" to repeat a value. "
            + "Aligns values to word boundaries by default.",
        true,
        new NumericStorageFunction(true, Memory.BYTES_PER_WORD)
    ),
    DOUBLE(
        ".double",
        "Store double-precision floating-point number(s) specified as operands. Operands can take the form "
            + "of a numeric value, \"Infinity\", \"NaN\", or \"value : count\" to repeat a value. "
            + "Aligns values to word (not doubleword) boundaries by default.",
        true,
        new NumericStorageFunction(true, Memory.BYTES_PER_DOUBLEWORD)
    ),
    ASCII(
        ".ascii",
        "Store one or more strings specified as operands without appending a null terminator.",
        true,
        new StringStorageFunction(false)
    ),
    ASCIIZ(
        ".asciiz",
        "Store one or more strings specified as operands, appending a null terminator (byte value zero).",
        true,
        new StringStorageFunction(true)
    ),
    SPACE(
        ".space",
        "Reserve a number of bytes specified as an operand. The reserved memory is not guaranteed to "
            + "be initialized, though it will likely be zeroed due to simulator limitations.",
        false,
        (syntax, assembler) -> {
            if (!assembler.getSegment().isData()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' only applies to data segments");
                return;
            }
            else if (syntax.getContent().size() != 1) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' requires one operand: an integer size");
                return;
            }
            Token sizeToken = syntax.getContent().get(0);
            if (!sizeToken.getType().isInteger()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' expected an integer size, got: " + sizeToken);
                return;
            }
            int size = (Integer) sizeToken.getValue();
            if (size < 0) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' expected a non-negative integer, got: " + size);
                return;
            }

            assembler.getSegment().incrementAddress(size);
        }
    ),
    ALIGN(
        ".align",
        "Align the next stored value to a multiple of 2^n bytes, where n is specified as an operand, "
            + "or use n = 0 disable automatic alignment until the next segment directive.",
        false,
        (syntax, assembler) -> {
            if (!assembler.getSegment().isData()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' only applies to data segments");
                return;
            }
            else if (syntax.getContent().size() != 1) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' requires one operand: an integer alignment");
                return;
            }
            Token alignmentToken = syntax.getContent().get(0);
            if (!alignmentToken.getType().isInteger()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' expected an integer alignment, got: " + alignmentToken);
                return;
            }
            int alignmentPower = (Integer) alignmentToken.getValue();
            if (alignmentPower < 0) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' expected a non-negative integer, got: " + alignmentPower);
                return;
            }

            if (alignmentPower == 0) {
                assembler.setAutoAlignmentEnabled(false);
            }
            else {
                assembler.alignSegmentAddress(1 << alignmentPower);
            }
        }
    ),
    EXTERN(
        ".extern",
        "Declare a global data field with a label and size specified as operands. This field is stored "
            + "in the global data segment.",
        false,
        (syntax, assembler) -> {
            if (syntax.getContent().size() != 2) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' requires two operands: a label and an integer size");
                return;
            }
            Token identifier = syntax.getContent().get(0);
            if (identifier.getType() != TokenType.IDENTIFIER && identifier.getType() != TokenType.OPERATOR) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' expected an identifier, got: " + identifier);
                return;
            }
            Token sizeToken = syntax.getContent().get(1);
            if (!sizeToken.getType().isInteger()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' expected an integer size, got: " + sizeToken);
                return;
            }
            int size = (Integer) sizeToken.getValue();
            if (size < 0) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' expected a non-negative integer, got: " + size);
                return;
            }

            assembler.defineExtern(identifier, size);
        }
    ),
    GLOBL(
        ".globl",
        "Declare one or more labels specified as operands to be global symbols. Global symbols can be "
            + "referenced from any source file currently being assembled.",
        true,
        (syntax, assembler) -> {
            if (syntax.getContent().isEmpty()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' requires one or more identifiers");
                return;
            }
            // SPIM limits .globl to one label, why not extend it to a list?
            for (Token token : syntax.getContent()) {
                if (token.getType() != TokenType.IDENTIFIER && token.getType() != TokenType.OPERATOR) {
                    logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' expected an identifier, got: " + token);
                    return;
                }

                assembler.makeSymbolGlobal(token);
            }
        }
    ),
    /**
     * Added by DPS on 11 July 2012.
     */
    EQV(
        ".eqv",
        "Create an equivalence whose identifier is specified as the first operand, and whose content "
            + "follows. When the identifier is used after this point in the source file, it is replaced with this "
            + "directive's content. This is very similar to \"#define\" in the C language.",
        false,
        (syntax, assembler) -> {} // Handled by the preprocessor
    ),
    /**
     * Added by Mohammad Sekhavat in Oct 2012.
     */
    MACRO(
        ".macro",
        "Begin the definition of a new macro whose identifier is specified as the first operand, and "
            + "whose parameter list follows. Instructions following this directive are added to the macro definition "
            + "until a \".end_macro\" directive is reached. For more information about macros, see Macro Help.",
        false,
        (syntax, assembler) -> {} // Handled by the preprocessor
    ),
    /**
     * Added by Mohammad Sekhavat in Oct 2012.
     */
    END_MACRO(
        ".end_macro",
        "End the definition of a macro started by \".macro\".",
        false,
        (syntax, assembler) -> {} // Handled by the preprocessor
    ),
    /**
     * Added by DPS on 11 Jan 2013.
     */
    INCLUDE(
        ".include",
        "Given a string filename specified as an operand, include the file's content in the current file. "
            + "The behavior of this directive is almost as if the specified file is copied and pasted directly into "
            + "the current file, and as such, it will share the same local symbol table.",
        false,
        (syntax, assembler) -> {} // Handled by the preprocessor
    ),
    SET(
        ".set",
        "Set the value of an assembler flag according to the given operand. Currently not implemented.",
        false,
        (syntax, assembler) -> {
            logWarning(syntax, assembler, "Directive '" + syntax.getDirective() + "' is not currently supported; ignored");
        }
    );

    public static final Map<String, Directive> ALL_DIRECTIVES = new HashMap<>();
    public static final StringTrie<Directive> ALL_DIRECTIVES_TRIE = new StringTrie<>();

    // All directives are loaded statically, so we can put them in their respective lookups statically as well
    static {
        for (Directive directive : Directive.values()) {
            ALL_DIRECTIVES.put(directive.getName(), directive);
            ALL_DIRECTIVES_TRIE.put(directive.getName(), directive);
        }
    }

    private final String name;
    private final String description;
    private final boolean allowsContinuation;
    private final Function function;

    Directive(String name, String description, boolean allowsContinuation, Function function) {
        this.name = name;
        this.description = description;
        this.allowsContinuation = allowsContinuation;
        this.function = function;
    }

    /**
     * Find the directive which corresponds to the given name, if one exists.
     *
     * @param name The directive name to search for, e.g. <code>.word</code>. The leading period is included.
     * @return The matching directive, or <code>null</code> if not found.
     */
    public static Directive fromName(String name) {
        return ALL_DIRECTIVES.get(name.toLowerCase());
    }

    /**
     * Find the directives, if any, which start with the given prefix. For example, the prefix <code>.a</code>
     * would match <code>.ascii</code>, <code>.asciiz</code>, and <code>.align</code>.
     *
     * @param prefix The prefix to match.
     * @return List of matching directives, which may be empty if none match.
     */
    public static List<Directive> matchNamePrefix(String prefix) {
        StringTrie<Directive> subTrie = ALL_DIRECTIVES_TRIE.getSubTrieIfPresent(prefix.toLowerCase());
        return (subTrie == null) ? List.of() : List.copyOf(subTrie.values());
    }

    /**
     * Get the literal name of this directive, including the leading period.
     *
     * @return The name of this directive in string form, e.g. <code>.word</code>.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get description of this directive (for help purposes).
     *
     * @return Description of this MIPS directive.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Determine whether this directive accepts a list of values which can continue onto the next line(s).
     * This is primarily used by the {@link mars.assembler.syntax.SyntaxParser SyntaxParser} to only check for
     * more tokens on the next line if the directive allows it.
     *
     * @return <code>true</code> if the directive's content may continue onto the next line,
     *         or <code>false</code> otherwise.
     */
    public boolean allowsContinuation() {
        return this.allowsContinuation;
    }

    /**
     * Parse and execute this directive using the given directive syntax and assembler. Any errors that occur
     * during this process are added to the assembler log.
     *
     * @param syntax    The syntax applied to this directive in the source code.
     * @param assembler The assembler whose state should be updated by this directive if needed.
     * @see Assembler#getLog()
     */
    public void process(DirectiveSyntax syntax, Assembler assembler) {
        this.function.apply(syntax, assembler);
    }

    /**
     * Obtain the string representation of this directive. This method is equivalent to {@link #getName()}.
     *
     * @return The name of this directive in string form, e.g. <code>.word</code>.
     */
    @Override
    public String toString() {
        return this.name;
    }

    private interface Function {
        void apply(DirectiveSyntax syntax, Assembler assembler);
    }

    private static void logWarning(DirectiveSyntax syntax, Assembler assembler, String message) {
        logWarning(syntax.getFirstToken(), assembler, message);
    }

    private static void logWarning(Token token, Assembler assembler, String message) {
        assembler.logWarning(token.getLocation(), message);
    }

    private static void logError(DirectiveSyntax syntax, Assembler assembler, String message) {
        logError(syntax.getFirstToken(), assembler, message);
    }

    private static void logError(Token token, Assembler assembler, String message) {
        assembler.logError(token.getLocation(), message);
    }

    private static void logError(Token token, Assembler assembler, AddressErrorException exception) {
        logError(token, assembler, "Cannot write to " + Binary.intToHexString(assembler.getSegment().getAddress()) + ": " + exception.getMessage());
    }

    private record NumericStorageFunction(boolean isFloatingPoint, int numBytes) implements Function {
        @Override
        public void apply(DirectiveSyntax syntax, Assembler assembler) {
            // DPS 11/20/06, added text segment prohibition
            if (!assembler.getSegment().isData()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' only applies to data segments");
                return;
            }
            if (syntax.getContent().isEmpty()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' requires one or more values");
                return;
            }

            // Automatically align the current address, if needed
            if (assembler.isAutoAlignmentEnabled()) {
                assembler.alignSegmentAddress(this.numBytes);
            }

            // DPS 15/07/08, allow ":" for repetition for all numeric
            // directives (originally just .word)
            // Conditions for correctly-formed replication:
            // (integer directive AND integer value OR floating directive AND
            // (integer value OR floating value))
            // AND integer repetition value
            int initialAddress = assembler.getSegment().getAddress();
            Token repeatColon = null;
            Token previousNeedsPatch = null;
            for (Token token : syntax.getContent()) {
                try {
                    // Integers (including characters) may be used as values in integer or floating-point directives,
                    // or as a repetition count
                    if (token.getType().isInteger() || token.getType() == TokenType.CHARACTER) {
                        // Extract the value from the token
                        int value = (Integer) token.getValue();

                        // Handle the "value : n" format, which replicates the value "n" times.
                        if (repeatColon != null) {
                            if (assembler.getSegment().getAddress() == initialAddress) {
                                logError(repeatColon, assembler, "Missing value to repeat before ':'");
                                return;
                            }
                            else if (value <= 0) {
                                logError(token, assembler, "Expected a positive integer repetition count, got: " + token);
                                return;
                            }

                            // Replicate the last stored value (n - 1) additional times
                            int address = assembler.getSegment().getAddress();
                            if (this.numBytes == Memory.BYTES_PER_DOUBLEWORD) {
                                long repeatValue = Memory.getInstance().fetchDoubleword(address - this.numBytes, false);
                                for (int repetition = 1; repetition < value; repetition++) {
                                    Memory.getInstance().storeDoubleword(address, repeatValue, true);
                                    address += this.numBytes;
                                }
                            }
                            else {
                                int repeatValue = Memory.getInstance().fetch(address - this.numBytes, this.numBytes, false);
                                for (int repetition = 1; repetition < value; repetition++) {
                                    Memory.getInstance().store(address, repeatValue, this.numBytes, true);
                                    if (previousNeedsPatch != null) {
                                        assembler.createForwardReferencePatch(address, this.numBytes, previousNeedsPatch);
                                    }
                                    address += this.numBytes;
                                }
                            }
                            assembler.getSegment().setAddress(address);

                            // The repetition syntax is now complete
                            repeatColon = null;
                            previousNeedsPatch = null;
                        }
                        else if (this.isFloatingPoint) {
                            this.storeFloatingPoint(syntax, assembler, token, value);
                        }
                        else {
                            this.storeInteger(syntax, assembler, token, value);
                            previousNeedsPatch = null;
                        }
                    }
                    // Only integers may be used as a repetition count
                    else if (repeatColon != null) {
                        // We can just reuse the same check after the main loop to generate an error
                        break;
                    }
                    // Real numbers may be used as values in floating-point directives only
                    else if (this.isFloatingPoint && token.getType() == TokenType.REAL_NUMBER) {
                        this.storeFloatingPoint(syntax, assembler, token, (Double) token.getValue());
                    }
                    // A colon indicates that the previous value should be repeated a number of times specified by
                    // the next token (repetition count)
                    else if (token.getType() == TokenType.COLON) {
                        repeatColon = token;
                    }
                    // Identifiers may be used as values in integer directives only, but require the actual value to
                    // be patched later by the assembler since symbol addresses aren't fully resolved yet
                    else if (token.getType() == TokenType.IDENTIFIER) {
                        if (this.isFloatingPoint) {
                            logError(token, assembler, "Directive '" + syntax.getDirective() + "' does not support label addresses as values");
                            return;
                        }

                        int value;
                        // Only check the local symbol table, not the global symbol table. Local symbols always take
                        // precedence over global symbols with the same identifier, and we can't guarantee
                        // at this point that the identifier doesn't refer to a local symbol later in the file.
                        Symbol symbol = assembler.getLocalSymbolTable().getSymbol(token.getLiteral());
                        if (symbol != null) {
                            // This label has a resolved address which we can simply use as the value
                            value = symbol.getAddress();
                            previousNeedsPatch = null;
                        }
                        else {
                            // Since this label hasn't been resolved yet, we'll just store a placeholder value of
                            // 0xDEADBEEF (truncated as needed) and tell the assembler to patch the value later
                            value = 0xDEADBEEF;
                            assembler.createForwardReferencePatch(assembler.getSegment().getAddress(), this.numBytes, token);
                            // If repeated, more patches will need to be applied
                            previousNeedsPatch = token;
                        }

                        // Store either the actual value or the temporary placeholder
                        Memory.getInstance().store(assembler.getSegment().getAddress(), value, this.numBytes, false);
                        assembler.getSegment().incrementAddress(this.numBytes);
                    }
                    else {
                        logError(token, assembler, "Directive '" + syntax.getDirective() + "' expected a numeric value, got: " + token);
                        return;
                    }
                }
                catch (AddressErrorException exception) {
                    logError(token, assembler, exception);
                    return;
                }
            }

            // If there's still a repeat colon, the repetition count is invalid or missing
            if (repeatColon != null) {
                logError(repeatColon, assembler, "Expected an integer repetition count following ':'");
            }
        }

        private void storeFloatingPoint(DirectiveSyntax syntax, Assembler assembler, Token token, double value) throws AddressErrorException {
            if (this.numBytes == Memory.BYTES_PER_DOUBLEWORD) {
                Memory.getInstance().storeDoubleword(assembler.getSegment().getAddress(), Double.doubleToRawLongBits(value), true);
            }
            else {
                // Issue a warning if the exponent of the value is too large to be stored in a float
                if (Double.isFinite(value) && Math.abs(value) > Float.MAX_VALUE) {
                    logWarning(token, assembler, "Floating-point value '" + token + "' is out of range for directive '" + syntax.getDirective() + "' and will be truncated to fit");
                }
                Memory.getInstance().storeWord(assembler.getSegment().getAddress(), Float.floatToRawIntBits((float) value), true);
            }
            assembler.getSegment().incrementAddress(this.numBytes);
        }

        private void storeInteger(DirectiveSyntax syntax, Assembler assembler, Token token, int value) throws AddressErrorException {
            // DPS 4-Jan-2013. Overriding 6-Jan-2005 KENV changes.
            // If value is out of range for the directive, will simply truncate
            // the leading bits (includes sign bits). This is what SPIM does.
            // But will issue a warning (not error) which SPIM does not do.
            if (
                // Out of range for byte?
                this.numBytes == 1 && (value < -0x80 || value > 0xFF)
                // Out of range for halfword?
                || this.numBytes == Memory.BYTES_PER_HALFWORD && (value < -0x8000 || value > 0xFFFF)
            ) {
                logWarning(token, assembler, "Integer value '" + token + "' is out of range for directive '" + syntax.getDirective() + "' and will be truncated to fit");
            }
            Memory.getInstance().store(assembler.getSegment().getAddress(), value, this.numBytes, true);
            assembler.getSegment().incrementAddress(this.numBytes);
        }
    }

    private record StringStorageFunction(boolean addNullTerminator) implements Function {
        @Override
        public void apply(DirectiveSyntax syntax, Assembler assembler) {
            if (!assembler.getSegment().isData()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' only applies to data segments");
                return;
            }
            if (syntax.getContent().isEmpty()) {
                logError(syntax, assembler, "Directive '" + syntax.getDirective() + "' requires one or more strings");
                return;
            }

            for (Token token : syntax.getContent()) {
                if (token.getType() != TokenType.STRING) {
                    logError(token, assembler, "Directive '" + syntax.getDirective() + "' expected a string");
                    continue;
                }

                for (byte charValue : ((String) token.getValue()).getBytes()) {
                    try {
                        Memory.getInstance().storeByte(assembler.getSegment().getAddress(), charValue, true);
                    }
                    catch (AddressErrorException exception) {
                        logError(token, assembler, exception);
                        return;
                    }
                    assembler.getSegment().incrementAddress(1);
                }

                if (this.addNullTerminator) {
                    try {
                        Memory.getInstance().storeByte(assembler.getSegment().getAddress(), 0, true);
                    }
                    catch (AddressErrorException exception) {
                        logError(token, assembler, exception);
                        return;
                    }
                    assembler.getSegment().incrementAddress(1);
                }
            }
        }
    }
}