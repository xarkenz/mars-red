package mars.assembler;

import mars.*;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterFile;
import mars.util.Binary;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

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
 * A tokenizer is capable of tokenizing a complete MIPS program, or a given line from
 * a MIPS program.  Since MIPS is line-oriented, each line defines a complete statement.
 * Tokenizing is the process of analyzing the input MIPS program for the purpose of
 * recognizing each MIPS language element.  The types of language elements are known as "tokens".
 * MIPS tokens are defined in the {@link TokenType} class.
 * <p>
 * Example: The MIPS statement  <code>here:  lw  $t3, 8($t4)   #load third member of array</code><br>
 * generates the following token list<br>
 * IDENTIFIER, COLON, OPERATOR, REGISTER_NAME, COMMA, INTEGER_5, LEFT_PAREN,
 * REGISTER_NAME, RIGHT_PAREN, COMMENT<br>
 *
 * @author Pete Sanderson, August 2003
 */
public class Tokenizer {
    /**
     * COD2, A-51:  "Identifiers are a sequence of alphanumeric characters,
     * underbars (_), and dots (.) that do not begin with a number."
     * <p>
     * DPS 14-Jul-2008: added '$' as valid symbol.  Permits labels to include $.
     * MIPS-target GCC will produce labels that start with $.
     */
    public static boolean isValidIdentifier(String value) {
        if (!(Character.isLetter(value.charAt(0)) || value.charAt(0) == '_' || value.charAt(0) == '.' || value.charAt(0) == '$')) {
            return false;
        }
        for (int index = 1; index < value.length(); index++) {
            if (!(Character.isLetterOrDigit(value.charAt(index)) || value.charAt(index) == '_' || value.charAt(index) == '.' || value.charAt(index) == '$')) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tokenize a complete MIPS program from a file, line by line. Each line of source code is translated into a
     * {@link SourceLine}, which consists of both the original code and its tokenized form.
     * <p>
     * Note: Equivalences, includes, and macros are handled by the {@link Preprocessor} at this stage.
     *
     * @param filename The name of the file containing source code to be tokenized.
     * @param errors   The error list, which will be populated with any tokenizing errors in the given lines.
     * @return The list of tokenized lines.
     */
    public static List<SourceLine> tokenize(String filename, ErrorList errors) {
        return tokenize(filename, errors, new Preprocessor(filename));
    }

    public static List<SourceLine> tokenize(String filename, ErrorList errors, Preprocessor preprocessor) {
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(filename));
            // Gather all lines from the source file into a list of strings
            return tokenize(filename, inputFile.lines().toList(), errors, preprocessor);
        }
        catch (IOException exception) {
            errors.add(new ErrorMessage(
                filename,
                0,
                0,
                "Unable to read file: " + exception
            ));
            return new ArrayList<>();
        }
    }

    /**
     * Tokenize a complete MIPS program, line by line. Each line of source code is translated into a
     * {@link SourceLine}, which consists of both the original code and its tokenized form.
     * <p>
     * Note: Equivalences, includes, and macros are handled by the {@link Preprocessor} at this stage.
     *
     * @param filename The filename indicating where the given source code is from.
     * @param lines    The source code to be tokenized.
     * @param errors   The error list, which will be populated with any tokenizing errors in the given lines.
     * @return The list of tokenized lines.
     */
    public static List<SourceLine> tokenize(String filename, List<String> lines, ErrorList errors) {
        return tokenize(filename, lines, errors, new Preprocessor(filename));
    }

    public static List<SourceLine> tokenize(String filename, List<String> lines, ErrorList errors, Preprocessor preprocessor) {
        if (preprocessor.checkFilename(filename)) {
            errors.add(new ErrorMessage(
                filename,
                0,
                0,
                "This file has already been included (recursive or duplicate include)"
            ));
        }

        // It's reasonable to guess that the number of lines output will equal the number of lines input...
        // the only exception is the .include directive, which pastes all lines from another file
        List<SourceLine> sourceLines = new ArrayList<>(lines.size());

        int lineNumber = 1;
        for (String line : lines) {
            SourceLine sourceLine = tokenizeLine(filename, line, lineNumber++, errors, preprocessor);
            preprocessor.processLine(sourceLines, sourceLine, errors);
        }

        return sourceLines;
    }

    /**
     * Tokenize one line of source code. If lexical errors are discovered, they are added to the given error list
     * rather than being thrown as exceptions.
     *
     * @param filename     The filename indicating where the given source code is from.
     * @param line         The content of the line to be tokenized.
     * @param lineNumber   The line number in the source file (for error reporting).
     * @param errors       The error list, which will be populated with any tokenizing errors in the given lines.
     * @param preprocessor The current preprocessor instance, which will process token substitutions.
     * @return The generated tokens for the given line.
     */
    /*
     * Tokenizing is not as easy as it appears at first blush, because the typical
     * delimiters: space, tab, comma, can all appear inside MIPS quoted ASCII strings!
     * Also, spaces are not as necessary as they seem, the following line is accepted
     * and parsed correctly by SPIM:    label:lw,$t4,simple#comment
     * as is this weird variation:      label  :lw  $t4  ,simple ,  ,  , # comment
     *
     * as is this line:  stuff:.asciiz"# ,\n\"","aaaaa"  (interestingly, if you put
     * additional characters after the \", they are ignored!!)
     *
     * I also would like to know the starting character position in the line of each
     * token, for error reporting purposes.  StringTokenizer cannot give you this.
     *
     * Given all the above, it is just as easy to "roll my own" as to use StringTokenizer
     */
    public static SourceLine tokenizeLine(String filename, String line, int lineNumber, ErrorList errors, Preprocessor preprocessor) {
        List<Token> tokens = new ArrayList<>();

        if (line.isBlank()) {
            return new SourceLine(filename, lineNumber, line, tokens);
        }

        int index = 0;
        mainLoop: while (index < line.length()) {
            char startChar = line.charAt(index);

            switch (startChar) {
                // Delimiter (not a token, ignored)
                case ',', ' ', '\t' -> {}
                // Line comment
                case '#' -> {
                    preprocessor.processToken(tokens, new Token(
                        TokenType.COMMENT,
                        null,
                        line.substring(index),
                        filename,
                        lineNumber,
                        index
                    ));
                    // Skip to the end of the line
                    index = line.length();
                }
                // Single-character tokens (excluding plus and minus, which are handled by the default case)
                case ':', '(', ')' -> {
                    preprocessor.processToken(tokens, new Token(
                        switch (startChar) {
                            case ':' -> TokenType.COLON;
                            case '(' -> TokenType.LEFT_PAREN;
                            case ')' -> TokenType.RIGHT_PAREN;
                            // Should not happen, only included to satisfy the compiler
                            default -> TokenType.ERROR;
                        },
                        null,
                        Character.toString(startChar),
                        filename,
                        lineNumber,
                        index
                    ));
                    // Move on to the next character
                    index++;
                }
                // Character literal
                case '\'' -> {
                    // This is handled almost exactly like a string, except the number of characters must be 1
                    int startIndex = index;
                    StringBuilder value = new StringBuilder();
                    // Move on to the content of the literal
                    index++;
                    while (index < line.length()) {
                        char ch = line.charAt(index);
                        switch (ch) {
                            // End of the character literal
                            case '\'' -> {
                                // Move on to the next character after the quote
                                index++;
                                if (value.isEmpty()) {
                                    errors.add(new ErrorMessage(
                                        filename,
                                        lineNumber,
                                        startIndex,
                                        "Empty character literal"
                                    ));
                                    continue mainLoop;
                                }
                                else if (value.length() > 1) {
                                    errors.add(new ErrorMessage(
                                        filename,
                                        lineNumber,
                                        startIndex,
                                        "Too many characters in character literal"
                                    ));
                                }
                                preprocessor.processToken(tokens, new Token(
                                    TokenType.CHARACTER,
                                    value.charAt(0),
                                    line.substring(startIndex, index),
                                    filename,
                                    lineNumber,
                                    startIndex
                                ));
                                continue mainLoop;
                            }
                            // Escape sequence
                            case '\\' -> {
                                index = handleCharEscape(value, filename, lineNumber, ++index, line, errors);
                            }
                            // Literal character
                            default -> {
                                value.append(ch);
                                index++;
                            }
                        }
                    }
                    // If execution reaches this point, the literal was not properly terminated
                    errors.add(new ErrorMessage(
                        filename,
                        lineNumber,
                        startIndex,
                        "Unclosed character literal"
                    ));
                    if (!value.isEmpty()) {
                        preprocessor.processToken(tokens, new Token(
                            TokenType.CHARACTER,
                            (int) value.charAt(0),
                            line.substring(startIndex),
                            filename,
                            lineNumber,
                            startIndex
                        ));
                    }
                }
                // String literal
                case '"' -> {
                    int startIndex = index;
                    StringBuilder value = new StringBuilder();
                    // Move on to the content of the literal
                    index++;
                    while (index < line.length()) {
                        char ch = line.charAt(index);
                        switch (ch) {
                            // End of the string literal
                            case '"' -> {
                                // Move on to the next character after the quote
                                index++;
                                preprocessor.processToken(tokens, new Token(
                                    TokenType.STRING,
                                    value.toString(),
                                    line.substring(startIndex, index),
                                    filename,
                                    lineNumber,
                                    startIndex
                                ));
                                continue mainLoop;
                            }
                            // Escape sequence
                            case '\\' -> {
                                index = handleCharEscape(value, filename, lineNumber, ++index, line, errors);
                            }
                            // Literal character
                            default -> {
                                value.append(ch);
                                index++;
                            }
                        }
                    }
                    // If execution reaches this point, the literal was not properly terminated
                    errors.add(new ErrorMessage(
                        filename,
                        lineNumber,
                        startIndex,
                        "Unclosed string literal"
                    ));
                    preprocessor.processToken(tokens, new Token(
                        TokenType.STRING,
                        value.toString(),
                        line.substring(startIndex),
                        filename,
                        lineNumber,
                        startIndex
                    ));
                }
                // Something else
                default -> {
                    if (startChar == '+' || startChar == '-') {
                        // Either binary (e.g. label+4), which gets its own token, or unary (e.g. -10).
                        // If a plus or minus immediately follows an identifier, it is considered binary.
                        // Hacky, but it will have to do for now.
                        if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).getType() == TokenType.IDENTIFIER) {
                            preprocessor.processToken(tokens, new Token(
                                (startChar == '+') ? TokenType.PLUS : TokenType.MINUS,
                                null,
                                Character.toString(startChar),
                                filename,
                                lineNumber,
                                index
                            ));
                            continue;
                        }
                    }

                    // Check for either a number or identifier of some kind
                    if (Character.isLetterOrDigit(startChar) || startChar == '_' || startChar == '.' || startChar == '$' || startChar == '%') {
                        int startIndex = index;
                        // Assume the first character to be part of whatever this is
                        StringBuilder builder = new StringBuilder();
                        builder.append(startChar);
                        index++;

                        // A number can either start with a digit or a prefix followed by a digit
                        // Decimal point, plus, and minus are all valid prefixes
                        boolean isNumber = Character.isDigit(startChar) || (
                            (startChar == '.' || startChar == '+' || startChar == '-')
                            && index < line.length()
                            && Character.isDigit(line.charAt(index))
                        );

                        // Accumulate characters as long as they fit a number or identifier
                        while (index < line.length()) {
                            char ch = line.charAt(index);
                            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '$') {
                                builder.append(ch);
                            }
                            // Check for the special +/- in exponential form (e.g. 1.2e+34)
                            else if (isNumber && (ch == '+' || ch == '-')) {
                                char prevChar = builder.charAt(builder.length() - 1);
                                if (prevChar == 'e' || prevChar == 'E') {
                                    builder.append(ch);
                                }
                                else {
                                    break;
                                }
                            }
                            else {
                                break;
                            }
                        }

                        String value = builder.toString();

                        // See if it is a macro parameter
                        if (Macro.tokenIsMacroParameter(value, false)) {
                            preprocessor.processToken(tokens, new Token(
                                TokenType.MACRO_PARAMETER,
                                null,
                                value,
                                filename,
                                lineNumber,
                                startIndex
                            ));
                            continue;
                        }

                        // See if it is a register name or number
                        Register register = RegisterFile.getRegister(value);
                        if (register != null) {
                            preprocessor.processToken(tokens, new Token(
                                (register.getName().equals(value))
                                    ? TokenType.REGISTER_NAME
                                    : TokenType.REGISTER_NUMBER,
                                register.getNumber(),
                                value,
                                filename,
                                lineNumber,
                                startIndex
                            ));
                            continue;
                        }

                        // See if it is a floating point register name
                        register = Coprocessor1.getRegister(value);
                        if (register != null) {
                            preprocessor.processToken(tokens, new Token(
                                TokenType.FP_REGISTER_NAME,
                                register.getNumber(),
                                value,
                                filename,
                                lineNumber,
                                startIndex
                            ));
                            break;
                        }

                        // See if it is an immediate (constant) integer value
                        try {
                            int intValue = Binary.decodeInteger(value); // KENV 1/6/05

                            /* MODIFICATION AND COMMENT, DPS 3-July-2008
                             *
                             * The modifications of January 2005 documented below are being rescinded.
                             * All hexadecimal immediate values are considered 32 bits in length and
                             * their classification as INTEGER_5, INTEGER_16, INTEGER_16U (new)
                             * or INTEGER_32 depends on their 32 bit value.  So 0xFFFF will be
                             * equivalent to 0x0000FFFF instead of 0xFFFFFFFF.  This change, along with
                             * the introduction of INTEGER_16U (adopted from Greg Gibeling of Berkeley),
                             * required extensive changes to instruction templates especially for
                             * pseudo-instructions.
                             *
                             * This modification also appears in buildBasicStatementFromBasicInstruction()
                             * in mars.ProgramStatement.
                             *
                             * ///// Begin modification 1/4/05 KENV   ///////////////////////////////////////////
                             * // We have decided to interpret non-signed (no + or -) 16-bit hexadecimal immediate
                             * // operands as signed values in the range -32768 to 32767. So 0xffff will represent
                             * // -1, not 65535 (bit 15 as sign bit), 0x8000 will represent -32768 not 32768.
                             * // NOTE: 32-bit hexadecimal immediate operands whose values fall into this range
                             * // will be likewise affected, but they are used only in pseudo-instructions.  The
                             * // code in ExtendedInstruction.java to split this number into upper 16 bits for "lui"
                             * // and lower 16 bits for "ori" works with the original source code token, so it is
                             * // not affected by this tweak.  32-bit immediates in data segment directives
                             * // are also processed elsewhere so are not affected either.
                             * ////////////////////////////////////////////////////////////////////////////////
                             *
                             * if (Binary.isHex(value) && intValue >= 0x8000 && intValue <= 0xFFFF) {
                             *     // Subtract the 0x10000 bias, because strings in the
                             *     // range 0x8000 to 0xFFFF are used to represent
                             *     // 16-bit negative numbers, not positive numbers.
                             *     intValue -= 0x10000;
                             * }
                             * // ------------- END KENV 1/4/05 MODIFICATIONS --------------
                             *
                             * END DPS 3-July-2008 COMMENTS */

                            // Classify the integer based on the number of bits needed to represent it in binary
                            // Shift operands must fit in 5 bits, thus being limited to 0-31
                            TokenType intType;
                            if (0 <= intValue && intValue <= 31) {
                                intType = TokenType.INTEGER_5;
                            }
                            else if (DataTypes.MIN_UHALF_VALUE <= intValue && intValue <= DataTypes.MAX_UHALF_VALUE) {
                                intType = TokenType.INTEGER_16U;
                            }
                            else if (DataTypes.MIN_HALF_VALUE <= intValue && intValue <= DataTypes.MAX_HALF_VALUE) {
                                intType = TokenType.INTEGER_16;
                            }
                            else {
                                intType = TokenType.INTEGER_32;
                            }
                            preprocessor.processToken(tokens, new Token(
                                intType,
                                intValue,
                                value,
                                filename,
                                lineNumber,
                                startIndex
                            ));
                            break;
                        }
                        catch (NumberFormatException exception) {
                            // Ignore, this simply means the token is not an integer
                        }

                        // See if it is a real (fixed-point or floating-point) number. Note that integers can also
                        // be used as real numbers, but have already been handled above.
                        // NOTE: This also accepts would-be identifiers "Infinity" and "NaN".
                        try {
                            double doubleValue = Double.parseDouble(value);
                            preprocessor.processToken(tokens, new Token(
                                TokenType.REAL_NUMBER,
                                doubleValue,
                                value,
                                filename,
                                lineNumber,
                                startIndex
                            ));
                            break;
                        }
                        catch (NumberFormatException exception) {
                            // Ignore, this simply means the token is not a real number
                        }

                        // See if it is a directive
                        if (value.charAt(0) == '.') {
                            Directive directive = Directive.matchDirective(value);
                            if (directive != null) {
                                preprocessor.processToken(tokens, new Token(
                                    TokenType.DIRECTIVE,
                                    directive,
                                    value,
                                    filename,
                                    lineNumber,
                                    startIndex
                                ));
                                break;
                            }
                        }

                        // See if it is an instruction operator
                        if (Application.instructionSet.matchOperator(value) != null) {
                            preprocessor.processToken(tokens, new Token(
                                TokenType.OPERATOR,
                                null,
                                value,
                                filename,
                                lineNumber,
                                startIndex
                            ));
                            break;
                        }

                        // Test for general identifier goes last because there are defined tokens for various
                        // MIPS constructs (such as operators and directives) that also could fit
                        // the lexical specifications of an identifier, and those need to be recognized first.
                        if (isValidIdentifier(value)) {
                            preprocessor.processToken(tokens, new Token(
                                TokenType.IDENTIFIER,
                                null,
                                value,
                                filename,
                                lineNumber,
                                startIndex
                            ));
                            break;
                        }

                        // Doesn't match any MIPS language token, so produce an error
                        if (isNumber) {
                            errors.add(new ErrorMessage(
                                filename,
                                lineNumber,
                                startIndex,
                                "Invalid number: " + value
                            ));
                        }
                        else {
                            errors.add(new ErrorMessage(
                                filename,
                                lineNumber,
                                startIndex,
                                "Invalid language element: " + value
                            ));
                        }
                    }
                    else {
                        // startChar is some character that isn't recognized as the start of a token
                        errors.add(new ErrorMessage(
                            filename,
                            lineNumber,
                            index,
                            "Unexpected character: " + startChar
                                + " (0x" + Integer.toUnsignedString(startChar, 16) + ")"
                        ));
                    }
                }
            }
        }

        return new SourceLine(filename, lineNumber, line, tokens);
    }

    /**
     * Handle an escape for a character or string literal. It is assumed that <code>index</code> has already been
     * incremented past the initial backslash.
     *
     * @return The new value of <code>index</code>, which corresponds to the next character in the line.
     */
    private static int handleCharEscape(StringBuilder value, String filename, int lineNumber, int index, String line, ErrorList errors) {
        // If the line ends abruptly, leave the index as is... other code will deal with it
        if (index >= line.length()) {
            return index;
        }

        // Get the first character of the escape sequence
        char ch = line.charAt(index);
        switch (ch) {
            // Line feed (newline)
            case 'n' -> value.append('\n');
            // Tab
            case 't' -> value.append('\t');
            // Carriage return
            case 'r' -> value.append('\r');
            // Backspace
            case 'b' -> value.append('\b');
            // Form feed (rarely useful)
            case 'f' -> value.append('\f');
            // Backslash & quote escapes
            case '\\', '\'', '\"' -> value.append(ch);
            // Octal or invalid
            default -> {
                // Check for an octal digit
                if ('0' <= ch && ch <= '7') {
                    // 3 octal digits represent 9 bits, so if the first digit has the most significant bit set
                    // (i.e. 4-7), it can only have one digit follow in order to fit in 8 bits
                    int maxDigits = (ch >= '4') ? 2 : 3;
                    int octalValue = ch - '0';
                    // Start at the second digit because the first has already been handled
                    // For consistency, `index` always represents the index of the last known valid digit
                    for (int digit = 1; digit < maxDigits && index + 1 < line.length(); digit++) {
                        ch = line.charAt(index + 1);
                        if ('0' <= ch && ch <= '7') {
                            // This is a valid octal digit
                            index++;
                            // Insert the new digit as the least significant 3 bits
                            octalValue = (octalValue << 3) | (ch - '0');
                        }
                        else {
                            // Not a valid octal digit, so it is assumed to not be part of this escape sequence
                            break;
                        }
                    }
                    // Convert the octal value to the corresponding character
                    value.append((char) octalValue);
                }
                else {
                    // Invalid escape
                    errors.add(new ErrorMessage(
                        filename,
                        lineNumber,
                        index,
                        "Unrecognized character escape: \\" + ch
                    ));
                }
            }
            // TODO: Not yet implemented (each preceded by a backslash):
            //       xNN = hex character
            //       uNNNN = unicode character (do we even want this feature?)
        }

        // Move to the first character following the escape
        return index + 1;
    }
}
