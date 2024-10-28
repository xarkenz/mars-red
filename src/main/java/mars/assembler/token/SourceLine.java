package mars.assembler.token;

import mars.assembler.log.SourceLocation;

import java.util.List;

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
 * Handy class to represent, for a given line of source code, the code
 * itself, the program containing it, and its line number within that program.
 * This is used to separately keep track of the original file/position of
 * a given line of code.  When <code>.include</code> is used, it will migrate to a different
 * line and possibly different program but the migration should not be visible to the user.
 */
public class SourceLine {
    private final SourceLocation location;
    private String content;
    private final List<Token> tokens;

    /**
     * Create a new <code>SourceLine</code> with the given information.
     *
     * @param location The location of the line in the source code.
     * @param content  The raw source code of the line.
     * @param tokens   The list of tokens that the line contains.
     */
    public SourceLine(SourceLocation location, String content, List<Token> tokens) {
        this.location = location;
        this.content = content;
        this.tokens = tokens;
    }

    /**
     * Get the location of this line in the source code.
     *
     * @return The line location.
     */
    public SourceLocation getLocation() {
        return this.location;
    }

    /**
     * Get the raw source code of the line.
     *
     * @return The line content.
     */
    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Get the list of tokens that the line contains.
     *
     * @return The token list.
     */
    public List<Token> getTokens() {
        return this.tokens;
    }
}