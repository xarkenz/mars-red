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
    /**
     * The level of this message.
     */
    private final LogLevel level;
    /**
     * The name of source file.
     */
    private final SourceLocation location;
    /**
     * The message content.
     */
    private final String content;

    /**
     * Create a new <code>LogMessage</code>.
     *
     * @param level    Set to WARNING if message is a warning, else set to ERROR or omit.
     * @param location String containing name of source file in which this error appears.
     * @param content  String containing appropriate error message.
     */
    public LogMessage(LogLevel level, SourceLocation location, String content) {
        this.level = level;
        this.location = location;
        this.content = content;
    }

    public static LogMessage info(SourceLocation location, String content) {
        return new LogMessage(LogLevel.INFO, location, content);
    }

    public static LogMessage warning(SourceLocation location, String content) {
        return new LogMessage(LogLevel.WARNING, location, content);
    }

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
     * Produce name of file containing error.
     *
     * @return Returns String containing name of source file containing the error.
     */
    public SourceLocation getLocation() {
        return this.location;
    }

    /**
     * Produce error message.
     *
     * @return Returns String containing textual error message.
     */
    public String getContent() {
        return this.content;
    }

    @Override
    public String toString() {
        return this.level.getDisplayName() + ' ' + this.location + ":\n    " + this.content;
    }
}