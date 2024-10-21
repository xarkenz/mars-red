package mars.assembler.token;

import mars.ErrorList;
import mars.Program;
import mars.assembler.SourceLine;

import java.util.*;

/*
Copyright (c) 2013.

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
 * Stores information of macros defined by now.
 * <p>
 * Will be used in first pass of assembling MIPS source code.
 * <p>
 * Each {@link Program} will have one {@link MacroHandler}.
 * <p>
 * NOTE: Forward referencing macros (macro expansion before its definition in
 * source code) and nested macro definitions (defining a macro inside another macro
 * definition) are not supported.
 *
 * @author M.H.Sekhavat (sekhavat17@gmail.com)
 */
public class MacroHandler {
    private record MacroSignature(String name, int parameterCount) {}

    /**
     * List of macros defined by now.
     */
    private final Map<MacroSignature, Macro> macros;
    private final Set<String> macroNames;
    private int nextInstanceID;

    /**
     * Create an empty MacroPool for given program.
     */
    public MacroHandler() {
        this.macros = new HashMap<>();
        this.macroNames = new HashSet<>();
        this.nextInstanceID = 0;
    }

    public Macro defineMacro(Macro macro) {
        this.macroNames.add(macro.getName());
        return this.macros.put(new MacroSignature(macro.getName(), macro.getParameters().size()), macro);
    }

    /**
     * Will be called by parser when reaches a macro expansion call
     *
     * @param name
     * @param arguments tokens passed to macro expansion call
     * @return {@link Macro} object matching the name and argument count of tokens passed
     */
    public Macro findMatchingMacro(String name, List<Token> arguments) {
        // If there are no macros with this name whatsoever, don't bother with further checks
        if (!this.hasMacroName(name)) {
            return null;
        }

        return this.macros.get(new MacroSignature(name, arguments.size()));
    }

    /**
     * @param name
     * @return true if any macros have been defined with name <code>name</code>
     *     by now, regardless of argument count.
     */
    public boolean hasMacroName(String name) {
        return this.macroNames.contains(name);
    }

    /**
     * {@link #nextInstanceID} will be set to 0 on construction of this class and will
     * be incremented by each call. Parser calls this method once for every
     * expansion. it will be a unique ID for each expansion of macro in a file.
     *
     * @return counter value
     */
    public int getNextInstanceID() {
        return this.nextInstanceID++;
    }

    public List<Token> getCallArguments(List<Token> callTokens) {
        // Strip off an end-of-line comment if one is present
        if (!callTokens.isEmpty() && callTokens.get(callTokens.size() - 1).getType() == TokenType.COMMENT) {
            callTokens = callTokens.subList(0, callTokens.size() - 1);
        }
        // Check for SPIM-style macro call where arguments are contained in parentheses
        if (callTokens.size() >= 2
            && callTokens.get(0).getType() == TokenType.LEFT_PAREN
            && callTokens.get(callTokens.size() - 1).getType() == TokenType.RIGHT_PAREN
        ) {
            // Simply exclude the parentheses and proceed as usual
            callTokens = callTokens.subList(1, callTokens.size() - 1);
        }

        // Remove any delimiter tokens
        List<Token> arguments = new ArrayList<>(callTokens);
        arguments.removeIf(token -> token.getType() == TokenType.DELIMITER);

        return arguments;
    }

    /**
     * Substitutes macro arguments in a line of source code inside macro
     * definition to be parsed after macro expansion.
     * <p>
     * Also appends <code>_M<var>id</var></code> to all labels defined inside macro body,
     * where <code><var>id</var></code> is the value of <code>instanceID</code>.
     *
     * @param arguments    List of tokens to expand to.
     * @param callerLine
     * @param errors  Destination for any errors from within this method.
     * @return The line of source code, with substituted arguments.
     */
    public List<SourceLine> instantiate(Macro macro, List<Token> arguments, SourceLine callerLine, ErrorList errors) {
        List<SourceLine> callStack = new ArrayList<>();
        callStack.add(callerLine);
        return this.instantiate(macro, arguments, callStack, errors);
    }

    private List<SourceLine> instantiate(Macro macro, List<Token> arguments, List<SourceLine> callStack, ErrorList errors) {
        SourceLine callerLine = callStack.get(callStack.size() - 1);
        int instanceID = this.getNextInstanceID();
        List<SourceLine> instanceLines = new ArrayList<>(macro.getLines().size());

        lineLoop: for (SourceLine sourceLine : macro.getLines()) {
            List<Token> tokens = sourceLine.tokens();
            List<Token> instanceTokens = new ArrayList<>(sourceLine.tokens().size());

            for (Token token : tokens) {
                Token instanceToken;

                // Check for a macro parameter
                int parameterIndex = macro.checkForParameter(token, errors);
                if (parameterIndex >= 0) {
                    // Substitute the argument passed by the caller
                    Token argument = arguments.get(parameterIndex);
                    instanceToken = new Token(
                        argument.getType(),
                        argument.getValue(),
                        argument.getLiteral(),
                        callerLine.filename(),
                        callerLine.lineIndex(),
                        token.getColumnIndex()
                    );
                }
                // Check for a label defined in the original macro
                else if ((token.getType() == TokenType.IDENTIFIER || token.getType() == TokenType.OPERATOR)
                         && macro.hasLabel(token.getLiteral())
                ) {
                    // Add a suffix to the label based on this macro instance ID
                    String instanceLabel = token.getLiteral() + "_M" + instanceID;
                    instanceToken = new Token(
                        TokenType.IDENTIFIER,
                        null,
                        instanceLabel,
                        callerLine.filename(),
                        callerLine.lineIndex(),
                        token.getColumnIndex()
                    );
                }
                else {
                    // Clone the token to the caller line
                    instanceToken = new Token(
                        token.getType(),
                        token.getValue(),
                        token.getLiteral(),
                        callerLine.filename(),
                        callerLine.lineIndex(),
                        token.getColumnIndex()
                    );
                }

                instanceToken.setOriginalToken(token);
                instanceTokens.add(instanceToken);
            }

            // Check for a macro call in the instantiated line
            for (int index = 0; index < instanceTokens.size(); index++) {
                // Check for a potential macro name
                if (instanceTokens.get(index).getType() != TokenType.IDENTIFIER) {
                    continue;
                }
                // The macro name must be the first token in the line aside from a possible label
                else if (index != 0 && instanceTokens.get(index - 1).getType() != TokenType.COLON) {
                    continue;
                }

                String calleeName = instanceTokens.get(index).getLiteral();
                List<Token> calleeArguments = this.getCallArguments(instanceTokens.subList(index + 1, instanceTokens.size()));

                // Obtain the macro which matches the list of arguments, if one exists
                Macro calleeMacro = this.findMatchingMacro(calleeName, calleeArguments);
                if (calleeMacro == null) {
                    continue;
                }

                // Expand the macro with the current arguments
                callStack.add(sourceLine);
                List<SourceLine> innerInstanceLines = this.instantiate(calleeMacro, calleeArguments, sourceLine, errors);
                callStack.remove(callStack.size() - 1);

                // Remove the macro call from the original line, but still keep the beginning of the line
                // in case it has a label or something before the call
                instanceTokens.subList(index, instanceTokens.size()).clear();

                // Add the modified line, then paste in the macro expansion
                instanceLines.add(new SourceLine(
                    callerLine.filename(),
                    callerLine.lineIndex(),
                    callerLine.content(),
                    instanceTokens
                ));
                instanceLines.addAll(innerInstanceLines);
                continue lineLoop;
            }

            instanceLines.add(new SourceLine(
                callerLine.filename(),
                callerLine.lineIndex(),
                callerLine.content(),
                instanceTokens
            ));
        }

        return instanceLines;
    }
}
