/*
 * MIPSTokenMarker.java - MIPS Assembly token marker
 * Copyright (C) 1998, 1999 Slava Pestov, 2010 Pete Sanderson
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package mars.venus.editors.jeditsyntax.tokenmarker;

import mars.Application;
import mars.assembler.Directive;
import mars.assembler.TokenType;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterFile;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.Instruction;
import mars.venus.editors.jeditsyntax.KeywordMap;
import mars.venus.editors.jeditsyntax.PopupHelpItem;

import javax.swing.text.Segment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MIPS token marker.
 *
 * @author Pete Sanderson (2010) and Slava Pestov (1999)
 */
public class MIPSTokenMarker extends TokenMarker {
    // private members
    private static KeywordMap cKeywords;
    private static String[] tokenLabels, tokenExamples;
    private final KeywordMap keywords;
    private int lastOffset;
    private int lastKeyword;

    public MIPSTokenMarker() {
        this(getKeywords());
    }

    public MIPSTokenMarker(KeywordMap keywords) {
        this.keywords = keywords;
    }

    public static String[] getMIPSTokenLabels() {
        if (tokenLabels == null) {
            tokenLabels = new String[Token.ID_COUNT];
            tokenLabels[Token.COMMENT1] = "Comment";
            tokenLabels[Token.LITERAL1] = "String literal";
            tokenLabels[Token.LITERAL2] = "Character literal";
            tokenLabels[Token.LABEL] = "Label";
            tokenLabels[Token.KEYWORD1] = "Instruction mnemonic";
            tokenLabels[Token.KEYWORD2] = "Assembler directive";
            tokenLabels[Token.KEYWORD3] = "Register";
            tokenLabels[Token.INVALID] = "Invalid";
            tokenLabels[Token.MACRO_ARG] = "Macro parameter";
        }
        return tokenLabels;
    }

    public static String[] getMIPSTokenExamples() {
        if (tokenExamples == null) {
            tokenExamples = new String[Token.ID_COUNT];
            tokenExamples[Token.COMMENT1] = "# comment";
            tokenExamples[Token.LITERAL1] = "\"string\"";
            tokenExamples[Token.LITERAL2] = "'\\n'";
            tokenExamples[Token.LABEL] = "main:";
            tokenExamples[Token.KEYWORD1] = "lui";
            tokenExamples[Token.KEYWORD2] = ".text";
            tokenExamples[Token.KEYWORD3] = "$zero";
            tokenExamples[Token.INVALID] = "\"bad";
            tokenExamples[Token.MACRO_ARG] = "%arg";
        }
        return tokenExamples;
    }

    public byte markTokensImpl(byte token, Segment line, int lineIndex) {
        char[] array = line.array;
        int offset = line.offset;
        lastOffset = offset;
        lastKeyword = offset;
        int length = line.count + offset;
        boolean backslash = false;
        boolean validLabelPosition = true;

        loop:
        for (int i = offset; i < length; i++) {
            int i1 = (i + 1);

            char c = array[i];
            if (validLabelPosition && !Character.isWhitespace(c)) {
                validLabelPosition = false;
            }
            if (c == '\\') {
                backslash = !backslash;
                continue;
            }

            switch (token) {
                case Token.NULL -> {
                    switch (c) {
                        case '"' -> {
                            doKeyword(line, i);
                            if (backslash) {
                                backslash = false;
                            }
                            else {
                                addToken(i - lastOffset, token);
                                token = Token.LITERAL1;
                                lastOffset = lastKeyword = i;
                            }
                        }
                        case '\'' -> {
                            doKeyword(line, i);
                            if (backslash) {
                                backslash = false;
                            }
                            else {
                                addToken(i - lastOffset, token);
                                token = Token.LITERAL2;
                                lastOffset = lastKeyword = i;
                            }
                        }
                        case ':' -> {
                            // 3 Aug 2010.  Will recognize label definitions when:
                            // (1) Label is same as instruction name.
                            // (2) Label begins after column 1.
                            // (3) There are spaces between label name and colon.
                            // (4) label is valid MIPS identifier (otherwise would catch something like "42:").
                            // (5) [03/2024] Label is the first token in the line (otherwise would catch
                            //     "notLabel:" in something like ".eqv notLabel 0 ; .byte notLabel:10"
                            backslash = false;
                            boolean validIdentifier;
                            try {
                                validIdentifier = TokenType.isValidIdentifier(new String(array, lastOffset, i1 - lastOffset - 1).trim());
                            }
                            catch (StringIndexOutOfBoundsException e) {
                                validIdentifier = false;
                            }
                            if (validIdentifier && lastToken == null) {
                                addToken(i1 - lastOffset, Token.LABEL);
                                lastOffset = lastKeyword = i1;
                            }
                        }
                        case '#' -> {
                            backslash = false;
                            doKeyword(line, i);
                            if (length - i >= 1) {
                                addToken(i - lastOffset, token);
                                addToken(length - i, Token.COMMENT1);
                                lastOffset = lastKeyword = length;
                                break loop;
                            }
                        }
                        default -> {
                            backslash = false;
                            // . and $ added 4/6/10 DPS; % added 12/12 M.Sekhavat
                            if (!Character.isLetterOrDigit(c) && c != '_' && c != '.' && c != '$' && c != '%') {
                                doKeyword(line, i);
                            }
                        }
                    }
                }
                case Token.LITERAL1 -> {
                    if (backslash) {
                        backslash = false;
                    }
                    else if (c == '"') {
                        addToken(i1 - lastOffset, token);
                        token = Token.NULL;
                        lastOffset = lastKeyword = i1;
                    }
                }
                case Token.LITERAL2 -> {
                    if (backslash) {
                        backslash = false;
                    }
                    else if (c == '\'') {
                        addToken(i1 - lastOffset, Token.LITERAL1);
                        token = Token.NULL;
                        lastOffset = lastKeyword = i1;
                    }
                }
                default -> {
                    throw new InternalError("Invalid state: " + token);
                }
            }
        }

        if (token == Token.NULL) {
            doKeyword(line, length);
        }

        switch (token) {
            case Token.LITERAL1, Token.LITERAL2 -> {
                addToken(length - lastOffset, Token.INVALID);
                token = Token.NULL;
            }
            case Token.KEYWORD2 -> {
                addToken(length - lastOffset, token);
                if (!backslash) {
                    token = Token.NULL;
                }
            }
            default -> {
                addToken(length - lastOffset, token);
            }
        }

        return token;
    }

    /**
     * Construct and return any appropriate help information for
     * the given token.
     *
     * @param token     the pertinent Token object
     * @param tokenText the source String that matched to the token
     * @return ArrayList of PopupHelpItem objects, one per match.
     */
    public ArrayList<PopupHelpItem> getTokenExactMatchHelp(Token token, String tokenText) {
        ArrayList<PopupHelpItem> helpItems = null;
        if (token != null && token.id == Token.KEYWORD1) {
            ArrayList<Instruction> instructionMatches = Application.instructionSet.matchOperator(tokenText);
            if (!instructionMatches.isEmpty()) {
                int realMatches = 0;
                helpItems = new ArrayList<>();
                for (Instruction instruction : instructionMatches) {
                    if (Application.getSettings().extendedAssemblerEnabled.get() || instruction instanceof BasicInstruction) {
                        helpItems.add(new PopupHelpItem(tokenText, instruction.getExampleFormat(), instruction.getDescription()));
                        realMatches++;
                    }
                }
                if (realMatches == 0) {
                    helpItems.add(new PopupHelpItem(tokenText, tokenText, "(is not a basic instruction)"));
                }
            }
        }
        if (token != null && token.id == Token.KEYWORD2) {
            Directive dir = Directive.matchDirective(tokenText);
            if (dir != null) {
                helpItems = new ArrayList<>();
                helpItems.add(new PopupHelpItem(tokenText, dir.getName(), dir.getDescription()));
            }
        }
        return helpItems;
    }

    /**
     * Construct and return any appropriate help information for
     * prefix match based on current line's token list.
     *
     * @param line      String containing current line
     * @param tokenList first Token on current line (head of linked list)
     * @param token     the pertinent Token object
     * @param tokenText the source String that matched to the token in previous parameter
     * @return ArrayList of PopupHelpItem objects, one per match.
     */

    public ArrayList<PopupHelpItem> getTokenPrefixMatchHelp(String line, Token tokenList, Token token, String tokenText) {
        // CASE:  Unlikely boundary case...
        if (tokenList == null || tokenList.id == Token.END) {
            return null;
        }

        // CASE:  if current token is a comment, turn off the text.
        if (token != null && token.id == Token.COMMENT1) {
            return null;
        }

        // Let's see if the line already contains an instruction or directive.  If so, we need its token
        // text as well so we can do the match.  Also need to distinguish the case where current
        // token is also an instruction/directive (moreThanOneKeyword variable).
        Token tokens = tokenList;
        String keywordTokenText = null;
        byte keywordType = -1;
        int offset = 0;
        boolean moreThanOneKeyword = false;
        while (tokens.id != Token.END) {
            if (tokens.id == Token.KEYWORD1 || tokens.id == Token.KEYWORD2) {
                if (keywordTokenText != null) {
                    moreThanOneKeyword = true;
                    break;
                }
                keywordTokenText = line.substring(offset, offset + tokens.length);
                keywordType = tokens.id;
            }
            offset += tokens.length;
            tokens = tokens.next;
        }

        // CASE:  Current token is valid KEYWORD1 (MIPS instruction).  If this line contains a previous KEYWORD1 or KEYWORD2
        //        token, then we ignore this one and do exact match on the first one.  If it does not, there may be longer
        //        instructions for which this is a prefix, so do a prefix match on current token.
        if (token != null && token.id == Token.KEYWORD1) {
            if (moreThanOneKeyword) {
                return (keywordType == Token.KEYWORD1) ? getTextFromInstructionMatch(keywordTokenText, true) : getTextFromDirectiveMatch(keywordTokenText, true);
            }
            else {
                return getTextFromInstructionMatch(tokenText, false);
            }
        }

        // CASE:  Current token is valid KEYWORD2 (MIPS directive).  If this line contains a previous KEYWORD1 or KEYWORD2
        //        token, then we ignore this one and do exact match on the first one.  If it does not, there may be longer
        //        directives for which this is a prefix, so do a prefix match on current token.
        if (token != null && token.id == Token.KEYWORD2) {
            if (moreThanOneKeyword) {
                return (keywordType == Token.KEYWORD1) ? getTextFromInstructionMatch(keywordTokenText, true) : getTextFromDirectiveMatch(keywordTokenText, true);
            }
            else {
                return getTextFromDirectiveMatch(tokenText, false);
            }
        }

        // CASE: line already contains KEYWORD1 or KEYWORD2 and current token is something other
        //       than KEYWORD1 or KEYWORD2. Generate text based on exact match of that token.
        if (keywordTokenText != null) {
            if (keywordType == Token.KEYWORD1) {
                return getTextFromInstructionMatch(keywordTokenText, true);
            }
            if (keywordType == Token.KEYWORD2) {
                return getTextFromDirectiveMatch(keywordTokenText, true);
            }
        }

        // CASE:  Current token is NULL, which can be any number of things.  Think of it as being either white space
        //        or an in-progress token possibly preceded by white space.  We'll do a trim on the token.  Now there
        //        are two subcases to consider:
        //    SUBCASE: The line does not contain any KEYWORD1 or KEYWORD2 tokens but nothing remains after trimming the
        //             current token's text.  This means it consists only of white space and there is nothing more to do
        //             but return.
        //    SUBCASE: The line does not contain any KEYWORD1 or KEYWORD2 tokens.  This means we do a prefix match of
        //             of the current token to either instruction or directive names.  Easy to distinguish since
        //             directives start with "."

        if (token != null && token.id == Token.NULL) {
            String trimmedTokenText = tokenText.trim();

            // Subcase: no KEYWORD1 or KEYWORD2 but current token contains nothing but white space.  We're done.
            if (keywordTokenText == null && trimmedTokenText.isEmpty()) {
                return null;
            }

            // Subcase: no KEYWORD1 or KEYWORD2.  Generate text based on prefix match of trimmed current token.
            if (keywordTokenText == null && !trimmedTokenText.isEmpty()) {
                if (trimmedTokenText.charAt(0) == '.') {
                    return getTextFromDirectiveMatch(trimmedTokenText, false);
                }
                else if (trimmedTokenText.length() >= Application.getSettings().editorPopupPrefixLength.get()) {
                    return getTextFromInstructionMatch(trimmedTokenText, false);
                }
            }
        }

        // should never get here...
        return null;
    }

    /**
     * Return ArrayList of PopupHelpItem for match of directives.  If second argument
     * true, will do exact match.  If false, will do prefix match.  Returns null
     * if no matches.
     */
    private ArrayList<PopupHelpItem> getTextFromDirectiveMatch(String tokenText, boolean isExact) {
        List<Directive> directiveMatches = null;
        if (isExact) {
            Directive directive = Directive.matchDirective(tokenText);
            if (directive != null) {
                directiveMatches = new ArrayList<>();
                directiveMatches.add(directive);
            }
        }
        else {
            directiveMatches = Directive.prefixMatchDirectives(tokenText);
        }

        ArrayList<PopupHelpItem> results = null;
        if (directiveMatches != null) {
            results = new ArrayList<>();
            for (Directive directive : directiveMatches) {
                results.add(new PopupHelpItem(tokenText, directive.getName(), directive.getDescription(), isExact));
            }
        }

        return results;
    }

    /**
     * Return text for match of instruction mnemonic.  If second argument true, will
     * do exact match.  If false, will do prefix match.   Text is returned as ArrayList
     * of PopupHelpItem objects. If no matches, returns null.
     */
    private ArrayList<PopupHelpItem> getTextFromInstructionMatch(String tokenText, boolean isExact) {
        ArrayList<Instruction> instructionMatches;
        if (isExact) {
            instructionMatches = Application.instructionSet.matchOperator(tokenText);
        }
        else {
            instructionMatches = Application.instructionSet.prefixMatchOperator(tokenText);
        }
        if (instructionMatches == null) {
            return null;
        }

        ArrayList<PopupHelpItem> results = new ArrayList<>();
        int realMatches = 0;
        Map<String, String> mnemonicDescriptions = new HashMap<>();
        for (Instruction instruction : instructionMatches) {
            if (Application.getSettings().extendedAssemblerEnabled.get() || instruction instanceof BasicInstruction) {
                if (isExact) {
                    results.add(new PopupHelpItem(tokenText, instruction.getExampleFormat(), instruction.getDescription(), true));
                }
                else {
                    if (!mnemonicDescriptions.containsKey(instruction.getMnemonic())) {
                        mnemonicDescriptions.put(instruction.getMnemonic(), instruction.getDescription());
                    }
                }
                realMatches++;
            }
        }

        if (realMatches == 0) {
            if (isExact) {
                results.add(new PopupHelpItem(tokenText, tokenText, "(not a basic instruction)", true));
            }
            else {
                return null;
            }
        }
        else if (!isExact) {
            for (Map.Entry<String, String> entry : mnemonicDescriptions.entrySet()) {
                results.add(new PopupHelpItem(tokenText, entry.getKey(), entry.getValue(), false));
            }
        }

        return results;
    }

    /**
     * Get KeywordMap containing all MIPS key words.  This includes all instruction mnemonics,
     * assembler directives, and register names.
     *
     * @return KeywordMap where key is the keyword and associated value is the token type (e.g. Token.KEYWORD1).
     */
    public static KeywordMap getKeywords() {
        if (cKeywords == null) {
            cKeywords = new KeywordMap(false);
            // Add instruction mnemonics
            for (Instruction instruction : Application.instructionSet.getAllInstructions()) {
                cKeywords.add(instruction.getMnemonic(), Token.KEYWORD1);
            }
            // add assembler directives
            for (Directive directive : Directive.values()) {
                cKeywords.add(directive.getName(), Token.KEYWORD2);
            }
            // add integer register file
            for (Register register : RegisterFile.getRegisters()) {
                cKeywords.add(register.getName(), Token.KEYWORD3);
                cKeywords.add("$" + register.getNumber(), Token.KEYWORD3);  // also recognize $0, $1, $2, etc
            }
            // add Coprocessor 1 (floating point) register file
            for (Register register : Coprocessor1.getRegisters()) {
                cKeywords.add(register.getName(), Token.KEYWORD3);
            }
            // Note: Coprocessor 0 registers referenced only by number: $8, $12, $13, $14. These are already in the map
        }
        return cKeywords;
    }

    private void doKeyword(Segment line, int i) {
        int i1 = i + 1;

        int len = i - lastKeyword;
        byte id = keywords.lookup(line, lastKeyword, len);
        if (id != Token.NULL) {
            if (lastKeyword != lastOffset) {
                addToken(lastKeyword - lastOffset, Token.NULL);
            }
            addToken(len, id);
            lastOffset = i;
        }
        lastKeyword = i1;
    }
}
