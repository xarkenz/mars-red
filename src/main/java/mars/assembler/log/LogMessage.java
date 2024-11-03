package mars.assembler.log;

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
 * Represents occurrence of an error detected during tokenizing, assembly or simulation.
 *
 * @author Pete Sanderson, August 2003; Sean Clarke, October 2024
 */
public class LogMessage {
    private static final String INDENT = "\t";

    /**
     * The level of this message.
     */
    private final LogLevel level;
    /**
     * The location in the source code this message refers to.
     */
    private final SourceLocation location;
    /**
     * The textual content of this message.
     */
    private final String content;

    /**
     * Create a new <code>LogMessage</code>.
     *
     * @param level    The level of the message.
     * @param location The location in the source code the message refers to.
     * @param content  The textual content of the message.
     */
    public LogMessage(LogLevel level, SourceLocation location, String content) {
        this.level = level;
        this.location = location;
        this.content = content;
    }

    /**
     * Create a new <code>LogMessage</code> with level {@link LogLevel#INFO}.
     *
     * @param location The location in the source code the message refers to.
     * @param content  The textual content of the message.
     */
    public static LogMessage info(SourceLocation location, String content) {
        return new LogMessage(LogLevel.INFO, location, content);
    }

    /**
     * Create a new <code>LogMessage</code> with level {@link LogLevel#WARNING}.
     *
     * @param location The location in the source code the message refers to.
     * @param content  The textual content of the message.
     */
    public static LogMessage warning(SourceLocation location, String content) {
        return new LogMessage(LogLevel.WARNING, location, content);
    }

    /**
     * Create a new <code>LogMessage</code> with level {@link LogLevel#ERROR}.
     *
     * @param location The location in the source code the message refers to.
     * @param content  The textual content of the message.
     */
    public static LogMessage error(SourceLocation location, String content) {
        return new LogMessage(LogLevel.ERROR, location, content);
    }

    /**
     * Get the level of this message.
     *
     * @return The level of this message.
     */
    public LogLevel getLevel() {
        return this.level;
    }

    /**
     * Get the location in the source code this message refers to.
     *
     * @return The location in the source code this message refers to, or <code>null</code> if this message does not
     *         reference any particular location in the source code.
     */
    public SourceLocation getLocation() {
        return this.location;
    }

    /**
     * Get the textual content of this message.
     *
     * @return The textual content of this message.
     */
    public String getContent() {
        return this.content;
    }

    /**
     * Convert this message to a string for the purpose of displaying to the user. This string representation may
     * take up multiple lines, but does not end with a newline.
     *
     * @return This message converted to string form.
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder(this.level.getDisplayName());
        if (this.location != null) {
            output.append(' ').append(this.location).append(":\n").append(INDENT);
        }
        else {
            output.append(": ");
        }
        // Indent each additional line of the message content
        int lineStart = 0;
        int lineEnd = this.content.indexOf('\n');
        while (lineEnd >= 0) {
            output.append(this.content, lineStart, lineEnd + 1).append(INDENT);
            lineStart = lineEnd + 1;
            lineEnd = this.content.indexOf('\n', lineStart);
        }
        output.append(this.content, lineStart, this.content.length());
        return output.toString();
    }
}