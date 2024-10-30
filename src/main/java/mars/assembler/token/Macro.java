package mars.assembler.token;

import mars.assembler.log.AssemblerLog;

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
 * Stores information of a macro definition.
 *
 * @author M.H.Sekhavat (sekhavat17@gmail.com)
 */
public class Macro {
    private final String name;
    /**
     * Arguments like <code>%arg</code> will be substituted by macro expansion.
     */
    private final List<Token> parameters;
    private final List<SourceLine> lines;
    private final Set<String> labels;

    public Macro(String name, List<Token> parameters) {
        this.name = name;
        this.parameters = parameters;
        this.lines = new ArrayList<>();
        this.labels = new HashSet<>();
    }

    public String getName() {
        return this.name;
    }

    public List<Token> getParameters() {
        return this.parameters;
    }

    public List<SourceLine> getLines() {
        return this.lines;
    }

    public boolean addLabel(String label) {
        return this.labels.add(label);
    }

    public boolean hasLabel(String label) {
        return this.labels.contains(label);
    }

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

    @Override
    public String toString() {
        // For debug purposes
        return this.name + this.parameters;
    }
}
