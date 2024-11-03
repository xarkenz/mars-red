package mars.assembler.token;

import mars.assembler.log.AssemblerLog;

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
 * A class used by the {@link Preprocessor} to handle macro definition, lookup, and expansion.
 * <p>
 * Macros can be overloaded since they are identified by both macro name and number of parameters. Defining
 * another macro with the same name and parameter count as a previous macro overrides it.
 * <p>
 * Note: Forward references (calling a macro before it is defined) and nested definitions (defining a macro inside
 * the definition of another macro) are not supported.
 *
 * @author M.H.Sekhavat (sekhavat17@gmail.com), 2013; Sean Clarke, October 2024
 */
public class MacroHandler {
    /**
     * A record used to determine which macro is referenced by a macro call. Since macros can be overloaded,
     * the name alone is not enough to match a specific macro.
     *
     * @param name           The macro name.
     * @param parameterCount The number of parameters the macro is defined to have.
     */
    private record MacroSignature(String name, int parameterCount) {}

    /**
     * The macro lookup, which finds a macro based on its signature.
     */
    private final Map<MacroSignature, Macro> macros;
    /**
     * The set of all macro names used in {@link #macros}. The redundancy here makes it quicker to determine
     * whether some identifier <i>could</i> represent the beginning of a macro call.
     */
    private final Set<String> macroNames;
    private int nextInstanceID;

    /**
     * Create a new <code>MacroHandler</code> with no defined macros.
     */
    public MacroHandler() {
        this.macros = new HashMap<>();
        this.macroNames = new HashSet<>();
        this.nextInstanceID = 0;
    }

    /**
     * Define a macro, allowing it to be called later. If a macro already exists with the same name and parameter
     * count, it can no longer be used in expansions after this call.
     *
     * @param macro The macro to define.
     * @return The previous macro with the same name and parameter count, if applicable, or <code>null</code>
     *         if no such macro was defined.
     */
    public Macro defineMacro(Macro macro) {
        this.macroNames.add(macro.getName());
        return this.macros.put(new MacroSignature(macro.getName(), macro.getParameters().size()), macro);
    }

    /**
     * Find the macro defined with the given name whose signature matches the given list of call arguments.
     *
     * @param name      The name of the macro to find.
     * @param arguments The list of arguments given in the macro call.
     * @return The matching macro, if one exists, or <code>null</code> otherwise.
     */
    public Macro findMatchingMacro(String name, List<Token> arguments) {
        // If there are no macros with this name whatsoever, don't bother with further checks
        if (!this.hasMacroName(name)) {
            return null;
        }

        return this.macros.get(new MacroSignature(name, arguments.size()));
    }

    /**
     * Determine whether the given name matches any defined macros. This can be used to avoid unnecessary
     * parsing of macro arguments if the name does not match any defined macro.
     *
     * @param name The macro name to search for.
     * @return <code>true</code> if any macros have been defined with name <code>name</code>, regardless of
     *         parameter count, or <code>false</code> otherwise.
     */
    public boolean hasMacroName(String name) {
        return this.macroNames.contains(name);
    }

    /**
     * Find all macros defined with the given name, regardless of parameter count.
     *
     * @param name The macro name to search for.
     * @return The matching macros, which may be an empty list if none match.
     */
    public List<Macro> findMatchingMacros(String name) {
        List<Macro> matches = new ArrayList<>();
        for (Map.Entry<MacroSignature, Macro> entry : this.macros.entrySet()) {
            if (entry.getKey().name.equals(name)) {
                matches.add(entry.getValue());
            }
        }
        return matches;
    }

    /**
     * Filter the tokens after a possible macro name to obtain the relevant tokens for a macro call.
     *
     * @param callTokens The list of tokens following the macro name token.
     * @return The list of tokens with comments, delimiters, and possible surrounding parentheses removed.
     */
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
     * Generate a macro call expansion using the given arguments. Nested macro calls are also expanded by this method.
     * <p>
     * Also appends <code>_M<var>id</var></code> to all labels defined inside the macro body, where
     * <code><var>id</var></code> is an integer uniquely identifying the specific macro instance. This enables macros
     * to contain labels without conflicting between different instances.
     *
     * @param arguments  The macro arguments used in the call syntax.
     * @param callerLine The line containing the macro call.
     * @param log        The log to be populated with any errors produced.
     * @return The expanded form of the macro, including the expansions of any nested macro calls.
     */
    public List<SourceLine> instantiate(Macro macro, List<Token> arguments, SourceLine callerLine, AssemblerLog log) {
        return this.instantiate(macro, arguments, callerLine, log, new ArrayList<>());
    }

    private List<SourceLine> instantiate(Macro macro, List<Token> arguments, SourceLine callerLine, AssemblerLog log, List<Macro> callStack) {
        // Detect a recursive macro call, which happens if the call stack already contains this macro
        if (callStack.contains(macro)) {
            StringBuilder message = new StringBuilder("Recursive macro call detected (");
            for (Macro previousMacro : callStack) {
                message.append(previousMacro).append(" → ");
            }
            message.append(macro).append(')');
            log.logError(callerLine.getLocation(), message.toString());
            return new ArrayList<>();
        }
        callStack.add(macro);

        int instanceID = this.nextInstanceID++;
        List<SourceLine> instanceLines = new ArrayList<>(macro.getLines().size());

        lineLoop: for (SourceLine macroLine : macro.getLines()) {
            List<Token> tokens = macroLine.getTokens();
            List<Token> instanceTokens = new ArrayList<>(macroLine.getTokens().size());

            // First, substitute parameter tokens with the provided arguments
            for (Token token : tokens) {
                Token instanceToken;

                // Check for a macro parameter
                int parameterIndex = macro.checkForParameter(token, log);
                if (parameterIndex >= 0) {
                    // Substitute the argument passed by the caller
                    instanceToken = arguments.get(parameterIndex).copy();
                }
                // Check for a label defined in the original macro
                else if ((token.getType() == TokenType.IDENTIFIER || token.getType() == TokenType.OPERATOR)
                         && macro.hasLabel(token.getLiteral())
                ) {
                    // Add a suffix to the label based on this macro instance ID
                    String instanceLabel = token.getLiteral() + "_M" + instanceID;
                    instanceToken = new Token(
                        token.getLocation(),
                        instanceLabel,
                        TokenType.IDENTIFIER,
                        null
                    );
                }
                else {
                    // Use the token without any changes
                    instanceToken = token.copy();
                }

                instanceTokens.add(instanceToken);
            }

            // Check for a macro call in the instantiated line
            for (int index = 0; index < instanceTokens.size(); index++) {
                // Check for a potential macro name
                if (instanceTokens.get(index).getType() != TokenType.IDENTIFIER) {
                    continue;
                }
                // If this identifier doesn't match any macro name, don't bother with further checks
                else if (!this.hasMacroName(instanceTokens.get(index).getLiteral())) {
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
                    // No need to check this token further, but since we know this name is used for other macros,
                    // we can utilize the token value to pass that information along to the parser if needed
                    instanceTokens.get(index).setValue(this.findMatchingMacros(calleeName));
                    continue;
                }

                // Expand the macro with the current arguments
                List<SourceLine> innerInstanceLines = this.instantiate(calleeMacro, calleeArguments, macroLine, log, callStack);

                if (index != 0) {
                    // Remove the macro call from the original line, but still keep the beginning of the line
                    // in case it has a label or something before the call
                    instanceTokens.subList(index, instanceTokens.size()).clear();

                    // Add the modified line
                    instanceLines.add(new SourceLine(
                        callerLine.getLocation(),
                        macroLine.getContent(),
                        instanceTokens,
                        macroLine
                    ));
                }

                // Paste in the expansion
                for (SourceLine innerInstanceLine : innerInstanceLines) {
                    instanceLines.add(new SourceLine(
                        callerLine.getLocation(),
                        innerInstanceLine.getContent(),
                        innerInstanceLine.getTokens(),
                        new SourceLine(
                            macroLine.getLocation(),
                            innerInstanceLine.getContent(),
                            innerInstanceLine.getTokens(),
                            innerInstanceLine
                        )
                    ));
                }

                continue lineLoop;
            }

            // Finished processing the line, so add it to the expansion
            instanceLines.add(new SourceLine(
                callerLine.getLocation(),
                macroLine.getContent(),
                instanceTokens,
                macroLine
            ));
        }

        callStack.remove(callStack.size() - 1);
        return instanceLines;
    }
}
