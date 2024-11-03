package mars.assembler.token;

import mars.assembler.log.AssemblerLog;
import mars.assembler.log.SourceLocation;

import java.util.*;

/*
Copyright (c) 2013-2014.

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
 * Stores information about a macro definition for use by {@link MacroHandler}.
 *
 * @author M.H.Sekhavat (sekhavat17@gmail.com), 2013; Sean Clarke, October 2024
 */
public class Macro {
    private final SourceLocation location;
    private final String name;
    private final List<Token> parameters;
    private final List<SourceLine> lines;
    private final Set<String> labels;

    /**
     * Create a new macro with the given signature.
     *
     * @param name       The name of the macro.
     * @param parameters The list of parameters for the macro, each of which must have type
     *                   {@link TokenType#MACRO_PARAMETER}.
     */
    public Macro(SourceLocation location, String name, List<Token> parameters) {
        this.location = location;
        this.name = name;
        this.parameters = parameters;
        this.lines = new ArrayList<>();
        this.labels = new HashSet<>();
    }

    /**
     * Get the location of the <code>.macro</code> directive which started this macro's definition.
     *
     * @return The location of the macro definition.
     */
    public SourceLocation getLocation() {
        return this.location;
    }

    /**
     * Get the name of this macro.
     *
     * @return The macro name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the list of parameters for this macro, each of which is a token whose type is
     * {@link TokenType#MACRO_PARAMETER}.
     *
     * @return The parameter list.
     */
    public List<Token> getParameters() {
        return this.parameters;
    }

    /**
     * Get the list of lines in the macro definition body.
     *
     * @return The definition lines.
     */
    public List<SourceLine> getLines() {
        return this.lines;
    }

    /**
     * Register a label which has been defined within the macro body. The label will be modified upon macro expansion
     * to avoid conflicts between instances of the same label.
     *
     * @param label The label defined within the macro body.
     * @return <code>false</code> if this label has been previously defined in this macro, or <code>true</code>
     *         if this label has no conflicts.
     */
    public boolean addLabel(String label) {
        return this.labels.add(label);
    }

    /**
     * Determine whether a given label matches one defined in the macro definition.
     *
     * @param label The label to search for.
     * @return <code>true</code> if the label is defined in this macro, or <code>false</code> otherwise.
     */
    public boolean hasLabel(String label) {
        return this.labels.contains(label);
    }

    /**
     * Check a token and determine whether it matches one of the parameters for this macro.
     *
     * @param token The token to check.
     * @param log   The log, which an error will be written to if <code>token</code> has type
     *              {@link TokenType#MACRO_PARAMETER} (i.e. starts with <code>%</code>) but does not correspond
     *              to any parameter of this macro.
     * @return The index in the parameter list of the matched parameter, or <code>-1</code> if <code>token</code>
     *         does not represent a parameter for this macro.
     */
    public int checkForParameter(Token token, AssemblerLog log) {
        // If this token could not possibly be a parameter, we won't bother checking
        if (token.getType() == TokenType.MACRO_PARAMETER || token.isSPIMStyleMacroParameter()) {
            // Do a linear search for the parameter to obtain the index. Linear search isn't great, but most
            // macros won't have nearly enough parameters to warrant a more sophisticated solution
            for (int index = 0; index < this.parameters.size(); index++) {
                if (this.parameters.get(index).getLiteral().equals(token.getLiteral())) {
                    return index;
                }
            }

            // If no SPIM-style parameter was found, it could just be a symbol instead. But if no MARS-style
            // parameter was found, it cannot be a symbol due to starting with %, so give an error
            if (token.getType() == TokenType.MACRO_PARAMETER) {
                log.logError(
                    token.getLocation(),
                    "Undefined macro parameter '" + token + "'"
                );
            }
        }

        return -1;
    }

    /**
     * Convert this macro to a string containing the name and parameter count. For example, a macro
     * <code>my_macro</code> defined with 3 parameters will result in the string <code>my_macro/3</code>.
     *
     * @return The signature of this macro in string form.
     */
    @Override
    public String toString() {
        return this.name + "/" + this.parameters.size();
    }
}
