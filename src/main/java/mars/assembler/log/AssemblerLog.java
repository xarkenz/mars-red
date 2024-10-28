package mars.assembler.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
 * Maintains list of generated error messages, regardless of source (tokenizing, parsing, assembly, execution).
 *
 * @author Pete Sanderson, August 2003; Sean Clarke, October 2024
 */
public class AssemblerLog {
    private final List<LogMessage> messages;
    // Basically an EnumMap<LogLevel, int>
    private final int[] messageCounts;
    private int maxErrorCount;
    private Consumer<LogMessage> output;

    /**
     * Create a new <code>AssemblerLog</code>.
     */
    public AssemblerLog() {
        this.messages = new ArrayList<>();
        this.messageCounts = new int[LogLevel.values().length];
        Arrays.fill(this.messageCounts, 0);
        this.maxErrorCount = -1;
        this.output = null;
    }

    /**
     * Clear all messages from the log.
     */
    public void clear() {
        this.messages.clear();
        Arrays.fill(this.messageCounts, 0);
    }

    /**
     * Get the list of all logged messages, regardless of level, in chronological order.
     *
     * @return The list of logged messages.
     */
    public List<LogMessage> getMessages() {
        return this.messages;
    }

    /**
     * Count the number of error messages in the list.
     *
     * @return Number of error messages in the list.
     */
    public int getMessageCount() {
        return this.messages.size();
    }

    /**
     * Count the number of error messages in the list.
     *
     * @return Number of error messages in the list.
     */
    public int getMessageCount(LogLevel level) {
        return this.messageCounts[level.ordinal()];
    }

    /**
     * Determine whether at least one error has occurred (excluding warnings).
     *
     * @return <code>true</code> if at least one error has occurred, or <code>false</code> otherwise.
     */
    public boolean hasMessages(LogLevel level) {
        return this.getMessageCount(level) > 0;
    }

    /**
     * Get the maximum number of errors which can be produced by a single assembler run.
     *
     * @return The error limit.
     */
    public int getMaxErrorCount() {
        return this.maxErrorCount;
    }

    public void setMaxErrorCount(int maxErrorCount) {
        this.maxErrorCount = maxErrorCount;
    }

    /**
     * Check to see if the error limit has been exceeded.
     *
     * @return <code>true</code> if the error count exceeds the limit, or <code>false</code> otherwise.
     * @see #getMaxErrorCount()
     */
    public boolean hasExceededMaxErrorCount() {
        return this.maxErrorCount >= 0 && this.getMessageCount(LogLevel.ERROR) > this.maxErrorCount;
    }

    public void setOutput(Consumer<LogMessage> output) {
        this.output = output;
    }

    /**
     * Add a new error message to the end of the error list.
     *
     * @param message Message to be added to the end of the error list.
     */
    public void log(LogMessage message) {
        int errorCount = this.getMessageCount(LogLevel.ERROR);
        if (this.maxErrorCount >= 0 && errorCount >= this.maxErrorCount) {
            if (errorCount == this.maxErrorCount) {
                this.messages.add(LogMessage.error(null, "Maximum error count exceeded; halting assembly"));
                this.messageCounts[LogLevel.ERROR.ordinal()]++;
            }

            if (this.output != null) {
                this.output.accept(message);
            }
            return;
        }

        this.messages.add(message);
        this.messageCounts[message.getLevel().ordinal()]++;

        if (this.output != null) {
            this.output.accept(message);
        }
    }

    public void log(LogLevel level, SourceLocation location, String content) {
        this.log(new LogMessage(level, location, content));
    }

    public void logInfo(SourceLocation location, String content) {
        this.log(LogMessage.info(location, content));
    }

    public void logWarning(SourceLocation location, String content) {
        this.log(LogMessage.warning(location, content));
    }

    public void logError(SourceLocation location, String content) {
        this.log(LogMessage.error(location, content));
    }
}
