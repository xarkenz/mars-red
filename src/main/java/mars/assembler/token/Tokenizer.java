package mars.assembler.token;

import mars.Application;
import mars.assembler.Directive;
import mars.assembler.log.AssemblerLog;
import mars.assembler.log.SourceLocation;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterFile;
import mars.mips.instructions.Instruction;
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
 * Example:
 * <blockquote><pre>here:  lw  $t3, 8($t4)   #load third member of array</pre></blockquote>
 * The above is tokenized as <code>IDENTIFIER, COLON, OPERATOR, REGISTER_NAME, COMMA, INTEGER_5, LEFT_PAREN,
 * REGISTER_NAME, RIGHT_PAREN, COMMENT</code>.
 * <p>
 * The original MARS tokenizer was written by Pete Sanderson in August 2003.
 *
 * @author Pete Sanderson, August 2003; Sean Clarke, July 2024
 */
public class Tokenizer {
    /**
     * Tokenize a complete MIPS program from a file, line by line. Each line of source code is translated into a
     * {@link SourceLine}, which consists of both the original code and its tokenized form.
     * <p>
     * Note: Equivalences, includes, and macros are handled by the {@link Preprocessor} at this stage.
     *
     * @param filename The name of the file containing source code to be tokenized.
     * @param log      The error list, which will be populated with any tokenizing errors in the given lines.
     * @return The tokenized source file.
     */
    public static SourceFile tokenizeFile(String filename, AssemblerLog log) {
        return tokenizeFile(filename, log, new Preprocessor());
    }

    public static SourceFile tokenizeFile(String filename, AssemblerLog log, Preprocessor preprocessor) {
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(filename));
            // Gather all lines from the source file into a list of strings
            return tokenizeLines(filename, inputFile.lines().toList(), log, false, preprocessor);
        }
        catch (IOException exception) {
            log.logError(
                new SourceLocation(filename),
                "Unable to read file: " + exception
            );
            return new SourceFile(filename, new ArrayList<>());
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
     * @param log      The error list, which will be populated with any tokenizing errors in the given lines.
     * @return The tokenized source file.
     */
    public static SourceFile tokenizeLines(String filename, List<String> lines, AssemblerLog log) {
        return tokenizeLines(filename, lines, log, false);
    }

    public static SourceFile tokenizeLines(String filename, List<String> lines, AssemblerLog log, boolean isInExpansionTemplate) {
        return tokenizeLines(filename, lines, log, isInExpansionTemplate, new Preprocessor());
    }

    public static SourceFile tokenizeLines(String filename, List<String> lines, AssemblerLog log, boolean isInExpansionTemplate, Preprocessor preprocessor) {
        // It's reasonable to guess that the number of lines output will equal the number of lines input...
        // the only exception is the .include directive, which pastes all lines from another file
        List<SourceLine> sourceLines = new ArrayList<>(lines.size());

        int lineIndex = 0;
        for (String line : lines) {
            SourceLine sourceLine = tokenizeLine(filename, line, lineIndex++, log, preprocessor, isInExpansionTemplate);
            preprocessor.processLine(sourceLines, sourceLine, log);
        }

        return new SourceFile(filename, sourceLines);
    }

    /**
     * Tokenize one line of source code. If lexical errors are discovered, they are added to the given error list
     * rather than being thrown as exceptions.
     *
     * @param filename     The filename indicating where the given source code is from.
     * @param line         The content of the line to be tokenized.
     * @param lineIndex    The line index in the source file (for error reporting).
     * @param log          The error list, which will be populated with any tokenizing errors in the given lines.
     * @param preprocessor The current preprocessor instance, which will process token substitutions.
     * @return The generated tokens for the given line.
     */
    public static SourceLine tokenizeLine(String filename, String line, int lineIndex, AssemblerLog log, Preprocessor preprocessor) {
        return tokenizeLine(filename, line, lineIndex, log, preprocessor, false);
    }

    public static SourceLine tokenizeLine(String filename, String line, int lineIndex, AssemblerLog log, Preprocessor preprocessor, boolean isInExpansionTemplate) {
        SourceLocation lineLocation = new SourceLocation(filename, lineIndex);
        List<Token> tokens = new ArrayList<>();

        if (line.isBlank()) {
            return new SourceLine(lineLocation, line, tokens);
        }

        int columnIndex = 0;
        mainLoop: while (columnIndex < line.length()) {
            char startChar = line.charAt(columnIndex);

            // Check for indentation / invisible delimiter (not a token, ignored)
            if (Character.isWhitespace(startChar)) {
                // Move on to the next character
                columnIndex++;
                continue;
            }

            int startIndex = columnIndex;
            SourceLocation startLocation = lineLocation.toColumnLocation(startIndex);

            switch (startChar) {
                // Visible delimiter (is a token, but only relevant for syntax highlighting)
                case ',' -> {
                    preprocessor.processToken(tokens, new Token(
                        startLocation,
                        Character.toString(startChar),
                        TokenType.DELIMITER,
                        null
                    ));
                    // Move on to the next character
                    columnIndex++;
                }
                // Line comment
                case '#' -> {
                    preprocessor.processToken(tokens, new Token(
                        startLocation,
                        line.substring(columnIndex),
                        TokenType.COMMENT,
                        null
                    ));
                    // Skip to the end of the line
                    columnIndex = line.length();
                }
                // Single-character tokens (excluding plus and minus, which are handled by the default case)
                case ':', '(', ')' -> {
                    preprocessor.processToken(tokens, new Token(
                        startLocation,
                        Character.toString(startChar),
                        switch (startChar) {
                            case ':' -> TokenType.COLON;
                            case '(' -> TokenType.LEFT_PAREN;
                            case ')' -> TokenType.RIGHT_PAREN;
                            // Should not happen, only included to satisfy the compiler
                            default -> TokenType.ERROR;
                        },
                        null
                    ));
                    // Move on to the next character
                    columnIndex++;
                }
                // Character literal
                case '\'' -> {
                    // This is handled almost exactly like a string, except the number of characters must be 1
                    StringBuilder value = new StringBuilder();
                    // Move on to the content of the literal
                    columnIndex++;
                    while (columnIndex < line.length()) {
                        char ch = line.charAt(columnIndex);
                        switch (ch) {
                            // End of the character literal
                            case '\'' -> {
                                // Move on to the next character after the quote
                                columnIndex++;
                                if (value.isEmpty()) {
                                    log.logError(
                                        startLocation,
                                        "Empty character literal"
                                    );
                                    continue mainLoop;
                                }
                                else if (value.length() > 1) {
                                    log.logError(
                                        startLocation,
                                        "Too many characters in character literal"
                                    );
                                }
                                preprocessor.processToken(tokens, new Token(
                                    startLocation,
                                    line.substring(startIndex, columnIndex),
                                    TokenType.CHARACTER,
                                    (int) value.charAt(0)
                                ));
                                continue mainLoop;
                            }
                            // Escape sequence
                            case '\\' -> {
                                columnIndex++;
                                columnIndex = handleCharEscape(value, lineLocation, columnIndex, line, log);
                            }
                            // Literal character
                            default -> {
                                value.append(ch);
                                columnIndex++;
                            }
                        }
                    }
                    // If execution reaches this point, the literal was not properly terminated
                    log.logError(
                        startLocation,
                        "Unclosed character literal"
                    );
                    if (!value.isEmpty()) {
                        preprocessor.processToken(tokens, new Token(
                            startLocation,
                            line.substring(startIndex),
                            TokenType.ERROR,
                            (int) value.charAt(0)
                        ));
                    }
                }
                // String literal
                case '"' -> {
                    StringBuilder value = new StringBuilder();
                    // Move on to the content of the literal
                    columnIndex++;
                    while (columnIndex < line.length()) {
                        char ch = line.charAt(columnIndex);
                        switch (ch) {
                            // End of the string literal
                            case '"' -> {
                                // Move on to the next character after the quote
                                columnIndex++;
                                preprocessor.processToken(tokens, new Token(
                                    startLocation,
                                    line.substring(startIndex, columnIndex),
                                    TokenType.STRING,
                                    value.toString()
                                ));
                                continue mainLoop;
                            }
                            // Escape sequence
                            case '\\' -> {
                                columnIndex++;
                                columnIndex = handleCharEscape(value, lineLocation, columnIndex, line, log);
                            }
                            // Literal character
                            default -> {
                                value.append(ch);
                                columnIndex++;
                            }
                        }
                    }
                    // If execution reaches this point, the literal was not properly terminated
                    log.logError(
                        startLocation,
                        "Unclosed string literal"
                    );
                    preprocessor.processToken(tokens, new Token(
                        startLocation,
                        line.substring(startIndex),
                        TokenType.ERROR,
                        value.toString()
                    ));
                }
                // Something else
                default -> {
                    // Check for either a number or identifier of some kind
                    if (Character.isLetterOrDigit(startChar) || startChar == '+' || startChar == '-'
                        || startChar == '_' || startChar == '.' || startChar == '$' || startChar == '%'
                    ) {
                        // Assume the first character to be part of whatever this is
                        StringBuilder builder = new StringBuilder();
                        builder.append(startChar);
                        columnIndex++;

                        // A number can either start with a digit or a prefix followed by a digit
                        // Decimal point, plus, and minus are all valid prefixes
                        boolean isNumber = Character.isDigit(startChar) || (
                            (startChar == '.' || startChar == '+' || startChar == '-')
                            && columnIndex < line.length()
                            && Character.isDigit(line.charAt(columnIndex))
                        );

                        // Accumulate characters as long as they fit a number or identifier
                        while (columnIndex < line.length()) {
                            char ch = line.charAt(columnIndex);
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
                            columnIndex++;
                        }

                        String literal = builder.toString();

                        // See if it is plus or minus as a binary operator
                        if (literal.equals("+")) {
                            preprocessor.processToken(tokens, new Token(
                                startLocation,
                                literal,
                                TokenType.PLUS,
                                null
                            ));
                            continue;
                        }
                        else if (literal.equals("-")) {
                            preprocessor.processToken(tokens, new Token(
                                startLocation,
                                literal,
                                TokenType.MINUS,
                                null
                            ));
                            continue;
                        }

                        // See if it is a macro parameter
                        if (startChar == '%') {
                            TokenType type = TokenType.MACRO_PARAMETER;
                            if (literal.length() <= 1) {
                                log.logError(
                                    startLocation,
                                    "'%' is not a valid macro parameter name"
                                );
                                type = TokenType.ERROR;
                            }
                            preprocessor.processToken(tokens, new Token(
                                startLocation,
                                literal,
                                type,
                                null
                            ));
                            continue;
                        }

                        // See if it is a register name or number
                        Register register = RegisterFile.getRegister(literal);
                        if (register != null) {
                            preprocessor.processToken(tokens, new Token(
                                startLocation,
                                literal,
                                (register.getName().equals(literal))
                                    ? TokenType.REGISTER_NAME
                                    : TokenType.REGISTER_NUMBER,
                                register.getNumber()
                            ));
                            continue;
                        }

                        // See if it is a floating point register name
                        register = Coprocessor1.getRegister(literal);
                        if (register != null) {
                            preprocessor.processToken(tokens, new Token(
                                startLocation,
                                literal,
                                TokenType.FP_REGISTER_NAME,
                                register.getNumber()
                            ));
                            break;
                        }

                        // See if it is an immediate (constant) integer value
                        try {
                            int intValue = Binary.decodeInteger(literal); // KENV 1/6/05

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
                             * in mars.BasicStatement.
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

                            preprocessor.processToken(tokens, new Token(
                                startLocation,
                                literal,
                                TokenType.fromIntegerValue(intValue),
                                intValue
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
                            double doubleValue = Double.parseDouble(literal);
                            preprocessor.processToken(tokens, new Token(
                                startLocation,
                                literal,
                                TokenType.REAL_NUMBER,
                                doubleValue
                            ));
                            break;
                        }
                        catch (NumberFormatException exception) {
                            // Ignore, this simply means the token is not a real number
                        }

                        // See if it is a directive
                        if (literal.charAt(0) == '.') {
                            Directive directive = Directive.fromName(literal);
                            if (directive != null) {
                                if (isInExpansionTemplate) {
                                    log.logError(
                                        startLocation,
                                        "Directives such as '" + directive + "' are not allowed in expansion templates"
                                    );
                                    break;
                                }

                                preprocessor.processToken(tokens, new Token(
                                    startLocation,
                                    literal,
                                    TokenType.DIRECTIVE,
                                    directive
                                ));
                                break;
                            }
                        }

                        // See if it is an instruction operator
                        List<Instruction> mnemonicMatches = Application.instructionSet.matchMnemonic(literal);
                        if (!mnemonicMatches.isEmpty()) {
                            preprocessor.processToken(tokens, new Token(
                                startLocation,
                                literal,
                                TokenType.OPERATOR,
                                mnemonicMatches
                            ));
                            break;
                        }

                        // Test for general identifier goes last because there are defined tokens for various
                        // MIPS constructs (such as operators and directives) that also could fit
                        // the lexical specifications of an identifier, and those need to be recognized first.
                        if (isValidIdentifier(literal)) {
                            preprocessor.processToken(tokens, new Token(
                                startLocation,
                                literal,
                                TokenType.IDENTIFIER,
                                null
                            ));
                            break;
                        }

                        // Doesn't match any MIPS language token, so produce an error
                        if (isNumber) {
                            log.logError(
                                startLocation,
                                "Invalid number: " + literal
                            );
                        }
                        else {
                            log.logError(
                                startLocation,
                                "Invalid language element: " + literal
                            );
                        }
                        preprocessor.processToken(tokens, new Token(
                            startLocation,
                            literal,
                            TokenType.ERROR,
                            null
                        ));
                    }
                    // Check for some sort of template substitution, but only in an expansion template
                    else if (isInExpansionTemplate && startChar == '{') {
                        columnIndex++;

                        // Find the end of the substitution, accounting for possible nesting of curly braces
                        int nestLevel = 1;
                        while (columnIndex < line.length() && nestLevel > 0) {
                            char ch = line.charAt(columnIndex);
                            if (ch == '{') {
                                nestLevel++;
                            }
                            else if (ch == '}') {
                                nestLevel--;
                            }
                            columnIndex++;
                        }

                        if (nestLevel != 0) {
                            log.logError(
                                startLocation,
                                "Unclosed substitution syntax in expansion template"
                            );
                            break;
                        }

                        String subLine = line.substring(startIndex + 1, columnIndex - 1);
                        SourceLine tokenizedSubLine = tokenizeLine(filename, subLine, lineIndex, log, preprocessor, true);

                        preprocessor.processToken(tokens, new Token(
                            startLocation,
                            line.substring(startIndex, columnIndex),
                            TokenType.TEMPLATE_SUBSTITUTION,
                            tokenizedSubLine.getTokens()
                        ));
                    }
                    else {
                        // startChar is some character that isn't recognized as the start of a token
                        log.logError(
                            startLocation,
                            "Unexpected character: " + startChar
                                + " (0x" + Integer.toUnsignedString(startChar, 16) + ")"
                        );
                        preprocessor.processToken(tokens, new Token(
                            startLocation,
                            Character.toString(startChar),
                            TokenType.ERROR,
                            null
                        ));
                    }
                }
            }
        }

        return new SourceLine(lineLocation, line, tokens);
    }

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
     * Handle an escape for a character or string literal. It is assumed that <code>index</code> has already been
     * incremented past the initial backslash.
     *
     * @param value        The destination where the resulting character value will be appended.
     * @param lineLocation The filename of the current source file.
     * @param columnIndex  The index in <code>line</code> of the character immediately following the initial backslash.
     * @param line         The raw form of the current line of source code.
     * @param log          The error list, which will be added to in case of an invalid character escape.
     * @return The new value of <code>index</code>, which corresponds to the next character in the line.
     */
    public static int handleCharEscape(StringBuilder value, SourceLocation lineLocation, int columnIndex, String line, AssemblerLog log) {
        // If the line ends abruptly, leave the index as is... other code will deal with it
        if (columnIndex >= line.length()) {
            return columnIndex;
        }

        // Get the first character of the escape sequence
        char ch = line.charAt(columnIndex);
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
            // Hexadecimal
            case 'x' -> {
                int hexadecimalValue = 0;
                // Enforce exactly 2 hex digits (technically the C standard allows any number of digits, but we are
                // assuming that a character is stored in 8 bits, and this makes the behavior more predictable anyway)
                // For consistency, `index` always represents the index of the last known valid digit
                for (int digit = 0; digit < 2; digit++) {
                    // If the line ends abruptly, stop here... other code will deal with it
                    if (columnIndex + 1 >= line.length()) {
                        return columnIndex + 1;
                    }
                    ch = line.charAt(columnIndex + 1);
                    int digitValue = hexadecimalDigitValue(ch);
                    if (digitValue >= 0) {
                        // This is a valid hexadecimal digit
                        columnIndex++;
                        // Insert the new digit as the least significant 4 bits
                        hexadecimalValue = (hexadecimalValue << 4) | digitValue;
                    }
                    else {
                        // Not a valid hexadecimal digit, so generate an error
                        log.logError(
                            lineLocation.toColumnLocation(columnIndex + 1),
                            "Expected two hexadecimal digits following \\x, got: " + ch
                        );
                        break;
                    }
                }
                // Convert the hexadecimal value to the corresponding character
                value.append((char) hexadecimalValue);
            }
            // Octal or invalid
            default -> {
                // Check for an octal digit
                if ('0' <= ch && ch <= '7') {
                    // 3 octal digits represent 9 bits, so if the first digit has the most significant bit set
                    // (i.e. 4-7), it can only have one digit follow in order to fit in 8 bits
                    int maxDigits = (ch >= '4') ? 2 : 3;
                    // Start with the assumption that this is the least significant digit
                    int octalValue = ch - '0';
                    // Start by checking for a second digit because the first has already been handled
                    // For consistency, `index` always represents the index of the last known valid digit
                    for (int digit = 1; digit < maxDigits && columnIndex + 1 < line.length(); digit++) {
                        ch = line.charAt(columnIndex + 1);
                        if ('0' <= ch && ch <= '7') {
                            // This is a valid octal digit
                            columnIndex++;
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
                    log.logError(
                        lineLocation.toColumnLocation(columnIndex),
                        "Unrecognized character escape: \\" + ch
                    );
                }
            }
        }

        // Move to the first character following the escape
        return columnIndex + 1;
    }

    /**
     * Interpret the given character as a hexadecimal digit, if possible.
     * For digits <code>A</code> through <code>F</code>, both uppercase and lowercase are accepted.
     *
     * @param ch The character to interpret as a hexadecimal digit.
     * @return The hexadecimal digit value in the range <code>[0, 16)</code>, or <code>-1</code> if <code>ch</code>
     *         is not a valid hexadecimal digit.
     */
    public static int hexadecimalDigitValue(int ch) {
        // Check for a decimal digit first
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        else {
            // Convert to lowercase (has to be done after checking for a decimal digit to avoid false positives)
            ch |= 0b100000;
            if ('a' <= ch && ch <= 'f') {
                return ch - 'a' + 10;
            }
            else {
                // Invalid hexadecimal digit
                return -1;
            }
        }
    }
}
