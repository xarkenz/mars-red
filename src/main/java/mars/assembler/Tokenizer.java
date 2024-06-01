package mars.assembler;

import mars.*;

import java.io.BufferedReader;
import java.io.File;
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
 * MIPS tokens are defined in the TokenTypes class.
 * <p>
 * Example: The MIPS statement  <code>here:  lw  $t3, 8($t4)   #load third member of array</code><br>
 * generates the following token list<br>
 * IDENTIFIER, COLON, OPERATOR, REGISTER_NAME, COMMA, INTEGER_5, LEFT_PAREN,
 * REGISTER_NAME, RIGHT_PAREN, COMMENT<br>
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public class Tokenizer {
    private final Map<String, String> equivalences; // DPS 11-July-2012
    private final Set<String> knownFilenames;

    /**
     * Simple constructor.
     */
    public Tokenizer() {
        this.equivalences = new HashMap<>(); // DPS 11-July-2012
        this.knownFilenames = new HashSet<>();
    }

    public List<SourceLine> tokenize(String filename, ErrorList errors) {
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(filename));
            // Gather all lines from the source file into a list of strings
            return this.tokenize(inputFile.lines().toList(), errors);
        }
        catch (IOException exception) {
            errors.add(new ErrorMessage((Program) null, 0, 0, exception.toString()));
            return new ArrayList<>();
        }
    }

    /**
     * Will tokenize a complete MIPS program.  MIPS is line oriented (not free format),
     * so we will be line-oriented too.
     *
     * @param lines The source code to be tokenized.
     * @return An ArrayList representing the tokenized program.  Each list member is a TokenList
     *         that represents a tokenized source statement from the MIPS program.
     */
    public List<SourceLine> tokenize(List<String> lines, ErrorList errors) {
        List<SourceLine> sourceLines = new ArrayList<>(lines.size());

        int lineNumber = 1;
        for (String line : lines) {
            sourceLines.add(tokenizeLine(line, lineNumber++, errors));
            // DPS 03-Jan-2013. Related to 11-July-2012.
            // This statement will replace original source with source modified by .eqv substitution.
            // Not needed by assembler, but looks better in the Text Segment Display.
        }

        return sourceLines;
    }

    /**
     * Pre-pre-processing pass through source code to process any ".include" directives.
     * When one is encountered, the contents of the included file are inserted at that
     * point.  If no .include statements, the return value is a new array list but
     * with the same lines of source code.  Uses recursion to correctly process included
     * files that themselves have .include.  Plus it will detect and report recursive
     * includes both direct and indirect.
     *
     * @author DPS 11-Jan-2013
     */
    private ArrayList<SourceLine> processIncludes(Program program, Map<String, String> includeFiles) throws ProcessingException {
        List<String> source = program.getSourceList();
        ArrayList<SourceLine> result = new ArrayList<>(source.size());
        for (int sourceIndex = 0; sourceIndex < source.size(); sourceIndex++) {
            String line = source.get(sourceIndex);
            TokenList lineTokens = tokenizeLine(program, sourceIndex + 1, line, false);
            boolean hasInclude = false;
            for (int index = 0; index < lineTokens.size(); index++) {
                if (lineTokens.get(index).getLiteral().equalsIgnoreCase(Directive.INCLUDE.getName()) && (lineTokens.size() > index + 1) && lineTokens.get(index + 1).getType() == TokenType.QUOTED_STRING) {
                    String filename = lineTokens.get(index + 1).getLiteral();
                    filename = filename.substring(1, filename.length() - 1); // get rid of quotes
                    // Handle either absolute or relative pathname for .include file
                    if (!new File(filename).isAbsolute()) {
                        filename = new File(program.getFilename()).getParent() + File.separator + filename;
                    }
                    if (includeFiles.containsKey(filename)) {
                        // This is a recursive include.  Generate error message and return immediately.
                        Token t = lineTokens.get(index + 1);
                        errors.add(new ErrorMessage(program, t.getSourceLine(), t.getSourceColumn(), "Recursive include of file " + filename));
                        throw new ProcessingException(errors);
                    }
                    includeFiles.put(filename, filename);
                    Program incl = new Program();
                    try {
                        incl.readSource(filename);
                    }
                    catch (ProcessingException p) {
                        Token t = lineTokens.get(index + 1);
                        errors.add(new ErrorMessage(program, t.getSourceLine(), t.getSourceColumn(), "Error reading include file " + filename));
                        throw new ProcessingException(errors);
                    }
                    ArrayList<SourceLine> allLines = processIncludes(incl, includeFiles);
                    result.addAll(allLines);
                    hasInclude = true;
                    break;
                }
            }
            if (!hasInclude) {
                result.add(new SourceLine(line, program, sourceIndex + 1));
            }
        }
        return result;
    }

    /**
     * Will tokenize one line of source code.  If lexical errors are discovered,
     * they are noted in an ErrorMessage object which is added to the provided ErrorList
     * instead of the Tokenizer's error list. Will NOT throw an exception.
     *
     * @param line       String containing source code
     * @param lineNumber Line number from source code (used in error message)
     * @param errors
     * @return the generated token list for that line
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
    public static SourceLine tokenizeLine(String line, int lineNumber, ErrorList errors) {
        List<Token> tokens = new ArrayList<>();

        if (line.isBlank()) {
            return new SourceLine(line, lineNumber, tokens);
        }

        StringBuilder currentLiteral = new StringBuilder();
        int tokenStartIndex = 0;

        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);

            switch (ch) {
                case '#' -> {
                    // # denotes comment that takes remainder of line
                    if (tokenPos > 0) {
                        this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                    }
                    tokenStartIndex = index;
                    tokenPos = line.length() - index;
                    System.arraycopy(line.toCharArray(), index, token, 0, tokenPos);
                    this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                    index = line.length();
                    tokenPos = 0;
                }
                case ' ', '\t', ',' -> {
                    // Space, tab or comma is delimiter
                    if (tokenPos > 0) {
                        this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                        tokenPos = 0;
                    }
                }
                // These two guys are special.  Will be recognized as unary if and only if two conditions hold:
                // 1. Immediately followed by a digit (will use look-ahead for this).
                // 2. Previous token, if any, is not an IDENTIFIER
                // Otherwise considered binary and thus a separate token.  This is a slight hack but reasonable.
                case '+', '-' -> {
                    // Here's the REAL hack: recognizing signed exponent in E-notation floating point!
                    // (e.g. 1.2e-5) Add the + or - to the token and keep going.  DPS 17 Aug 2005
                    if (tokenPos > 0 && line.length() >= index + 2 && Character.isDigit(line[index + 1]) && (line[index - 1] == 'e' || line[index - 1] == 'E')) {
                        token[tokenPos++] = ch;
                        break;
                    }
                    // End of REAL hack.
                    if (tokenPos > 0) {
                        this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                        tokenPos = 0;
                    }
                    tokenStartIndex = index;
                    token[tokenPos++] = ch;
                    if (!((tokens.isEmpty() || tokens.get(tokens.size() - 1).getType() != TokenType.IDENTIFIER) && (line.length >= index + 2 && Character.isDigit(line[index + 1])))) {
                        // Treat it as binary
                        this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                        tokenPos = 0;
                    }
                }
                case ':', '(', ')' -> {
                    // These are other single-character tokens
                    if (tokenPos > 0) {
                        this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                        tokenPos = 0;
                    }
                    tokenStartIndex = index;
                    token[tokenPos++] = ch;
                    this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                    tokenPos = 0;
                }
                case '"' -> {
                    if (tokenPos > 0) {
                        this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                        tokenPos = 0;
                    }
                    tokenStartIndex = index;
                    token[tokenPos++] = ch;
                    insideQuotedString = true; // we're not inside a quoted string, so start a new token...
                }
                case '\'' -> {
                    if (tokenPos > 0) {
                        this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                        tokenPos = 0;
                    }
                    // Our strategy is to process the whole thing right now...
                    tokenStartIndex = index;
                    token[tokenPos++] = ch; // Put the quote in token[0]
                    int lookaheadChars = line.length() - index - 1;
                    // need minimum 2 more characters, 1 for char and 1 for ending quote
                    if (lookaheadChars < 2) {
                        break;  // gonna be an error
                    }
                    ch = line[++index];
                    token[tokenPos++] = ch; // grab second character, put it in token[1]
                    if (ch == '\'') {
                        break; // gonna be an error: nothing between the quotes
                    }
                    ch = line[++index];
                    token[tokenPos++] = ch; // grab third character, put it in token[2]
                    // Process if we've either reached second, non-escaped, quote or end of line.
                    if (ch == '\'' && token[1] != '\\' || lookaheadChars == 2) {
                        this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                        tokenPos = 0;
                        tokenStartIndex = index;
                        break;
                    }
                    // At this point, there is at least one more character on this line. If we're
                    // still here after seeing a second quote, it was escaped.  Not done yet;
                    // we either have an escape code, an octal code (also escaped) or invalid.
                    ch = line[++index];
                    token[tokenPos++] = ch; // grab fourth character, put it in token[3]
                    // Process if this is ending quote for escaped character or if at end of line
                    if (ch == '\'' || lookaheadChars == 3) {
                        this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                        tokenPos = 0;
                        tokenStartIndex = index;
                        break;
                    }
                    // At this point, we've handled all legal possibilities except octal, e.g. '\377'
                    // Proceed if enough characters remain to finish off octal.
                    if (lookaheadChars >= 5) {
                        ch = line[++index];
                        token[tokenPos++] = ch;  // grab fifth character, put it in token[4]
                        if (ch != '\'') {
                            // still haven't reached end, last chance for validity!
                            ch = line[++index];
                            token[tokenPos++] = ch;  // grab sixth character, put it in token[5]
                        }
                    }
                    // Process no matter what...we either have a valid character by now or not
                    this.processLiteral(token, line, lineNumber, tokenStartIndex, tokens);
                    tokenPos = 0;
                    tokenStartIndex = index; // start of character constant (single quote).
                }
                default -> {
                    // The character will be part of some literal
                    if (currentLiteral.isEmpty()) {
                        tokenStartIndex = index;
                    }
                    currentLiteral.append(ch);
                }
            }
        }
        if (tokenPos > 0) {
            processLiteral(currentLiteral.toString(), line, lineNumber, tokenStartIndex, tokens);
        }

        return new SourceLine(line, lineNumber, tokens);
    }

    /**
     * Given candidate token and its position, will classify and record it.
     */
    private static void processLiteral(StringBuilder literal, String line, int lineNumber, int tokenStartIndex, TokenList tokenList) {
        TokenType tokenType = TokenType.detectLiteralType(literal.toString());
        if (tokenType == TokenType.ERROR) {
            errors.add(new ErrorMessage((Program) null, lineNumber, tokenStartIndex, line + "\nInvalid language element: " + literal));
        }
        tokenList.add(new Token(tokenType, literal, program, lineNumber, tokenStartIndex));
    }

    /**
     * Process the .eqv directive, which needs to be applied prior to tokenizing of subsequent statements.
     * This handles detecting that theLine contains a .eqv directive, in which case it needs
     * to be added to the HashMap of equivalents.  It also handles detecting that theLine
     * contains a symbol that was previously defined in an .eqv directive, in which case
     * the substitution needs to be made.
     * DPS 11-July-2012
     */
    private TokenList processEqv(Program program, int lineNum, String theLine, TokenList tokens) {
        // See if it is .eqv directive.  If so, record it...
        // Have to assure it is a well-formed statement right now (can't wait for assembler).
        if (tokens.size() > 2 && (tokens.get(0).getType() == TokenType.DIRECTIVE || tokens.get(2).getType() == TokenType.DIRECTIVE)) {
            // There should not be a label but if there is, the directive is in token position 2 (ident, colon, directive).
            int dirPos = (tokens.get(0).getType() == TokenType.DIRECTIVE) ? 0 : 2;
            if (Directive.matchDirective(tokens.get(dirPos).getLiteral()) == Directive.EQV) {
                // Get position in token list of last non-comment token
                int tokenPosLastOperand = tokens.size() - ((tokens.get(tokens.size() - 1).getType() == TokenType.COMMENT) ? 2 : 1);
                // There have to be at least two non-comment tokens beyond the directive
                if (tokenPosLastOperand < dirPos + 2) {
                    errors.add(new ErrorMessage(program, lineNum, tokens.get(dirPos).getSourceColumn(), "Too few operands for " + Directive.EQV.getName() + " directive"));
                    return tokens;
                }
                // Token following the directive has to be IDENTIFIER
                if (tokens.get(dirPos + 1).getType() != TokenType.IDENTIFIER) {
                    errors.add(new ErrorMessage(program, lineNum, tokens.get(dirPos).getSourceColumn(), "Malformed " + Directive.EQV.getName() + " directive"));
                    return tokens;
                }
                String symbol = tokens.get(dirPos + 1).getLiteral();
                // Make sure the symbol is not contained in the expression.  Not likely to occur but if left
                // undetected it will result in infinite recursion.  e.g.  .eqv ONE, (ONE)
                for (int i = dirPos + 2; i < tokens.size(); i++) {
                    if (tokens.get(i).getLiteral().equals(symbol)) {
                        errors.add(new ErrorMessage(program, lineNum, tokens.get(dirPos).getSourceColumn(), "Cannot substitute " + symbol + " for itself in " + Directive.EQV.getName() + " directive"));
                        return tokens;
                    }
                }
                // Expected syntax is symbol, expression.  I'm allowing the expression to comprise
                // multiple tokens, so I want to get everything from the IDENTIFIER to either the
                // COMMENT or to the end.
                int startExpression = tokens.get(dirPos + 2).getSourceColumn();
                int endExpression = tokens.get(tokenPosLastOperand).getSourceColumn() + tokens.get(tokenPosLastOperand).getLiteral().length();
                String expression = theLine.substring(startExpression - 1, endExpression - 1);
                // Symbol cannot be redefined - the only reason for this is to act like the Gnu .eqv
                if (equivalences.containsKey(symbol) && !equivalences.get(symbol).equals(expression)) {
                    errors.add(new ErrorMessage(program, lineNum, tokens.get(dirPos + 1).getSourceColumn(), "\"" + symbol + "\" is already defined"));
                    return tokens;
                }
                equivalences.put(symbol, expression);
                return tokens;
            }
        }
        // Check if a substitution from defined .eqv is to be made.  If so, make one.
        boolean substitutionMade = false;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == TokenType.IDENTIFIER && equivalences.containsKey(token.getLiteral())) {
                // Do the substitution
                String sub = equivalences.get(token.getLiteral());
                int startPos = token.getSourceColumn();
                theLine = theLine.substring(0, startPos - 1) + sub + theLine.substring(startPos + token.getLiteral().length() - 1);
                substitutionMade = true; // one substitution per call.  If there are multiple, will catch next one on the recursion
                break;
            }
        }
        tokens.setProcessedLine(theLine); // DPS 03-Jan-2013. Related to changes of 11-July-2012.

        return (substitutionMade) ? tokenizeLine(lineNum, theLine) : tokens;
    }


    /**
     * If passed a candidate character literal, attempt to translate it into integer constant.
     * If the translation fails, return original value.
     */
    private String preprocessCharacterLiteral(String literal) {
        // Must start and end with single quotes and have something in between
        if (literal.length() < 3 || literal.charAt(0) != '\'' || literal.charAt(literal.length() - 1) != '\'') {
            return literal;
        }
        String quotesRemoved = literal.substring(1, literal.length() - 1);
        // If it is an escape sequence, get the ASCII value of the character
        if (quotesRemoved.charAt(0) != '\\') {
            // There must be only one character in the literal
            return (quotesRemoved.length() == 1) ? Integer.toString(quotesRemoved.charAt(0)) : literal;
        }
        // It is a simple escape sequence, so translate it to an integer constant with the ASCII value
        if (quotesRemoved.length() == 2) {
            return switch (quotesRemoved.charAt(1)) {
                case '\'' -> "39"; // Single quote
                case '\"' -> "34"; // Double quote
                case '\\' -> "92"; // Backslash
                case 'n' -> "10"; // Newline (line feed)
                case 't' -> "9"; // Tab
                case 'b' -> "8"; // Backspace
                case 'r' -> "13"; // Return
                case 'f' -> "12"; // Form feed
                case '0' -> "0"; // Null
                default -> literal; // (Invalid escape)
            };
        }
        // It should be a 3 digit octal code (000 through 377)
        if (quotesRemoved.length() == 4) {
            try {
                int intValue = Integer.parseInt(quotesRemoved.substring(1), 8);
                if (intValue >= 0x00 && intValue <= 0xFF) {
                    return Integer.toString(intValue);
                }
            } catch (NumberFormatException ignored) {
                // Invalid octal escape, fall through
            }
        }
        // Not a valid literal
        return literal;
    }
}
