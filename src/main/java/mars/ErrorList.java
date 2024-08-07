package mars;

import java.util.*;
import java.io.*;

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
 * Maintains list of generated error messages, regardless of source (tokenizing, parsing,
 * assembly, execution).
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public class ErrorList {
    public static final String ERROR_MESSAGE_PREFIX = "Error";
    public static final String WARNING_MESSAGE_PREFIX = "Warning";
    public static final String FILENAME_PREFIX = " in file ";
    public static final String LINE_PREFIX = " on line ";
    public static final String POSITION_PREFIX = ", column ";
    public static final String MESSAGE_SEPARATOR = ":\n    ";

    private final List<ErrorMessage> messages;
    private int errorCount;
    private int warningCount;

    /**
     * Constructor for ErrorList.
     */
    public ErrorList() {
        this.messages = new ArrayList<>();
        this.errorCount = 0;
        this.warningCount = 0;
    }

    /**
     * Obtain the list of error messages.
     *
     * @return The raw list of error messages.
     */
    public List<ErrorMessage> getErrorMessages() {
        return this.messages;
    }

    /**
     * Determine whether at least one error has occurred (excluding warnings).
     *
     * @return <code>true</code> if at least one error has occurred, or <code>false</code> otherwise.
     */
    public boolean errorsOccurred() {
        return this.errorCount != 0;
    }

    /**
     * Determine whether at least one warning has occurred.
     *
     * @return <code>true</code> if at least one warning has occurred, or <code>false</code> otherwise.
     */
    public boolean warningsOccurred() {
        return this.warningCount != 0;
    }

    /**
     * Add a new error message to the end of the error list.
     *
     * @param message Message to be added to the end of the error list.
     */
    public void add(ErrorMessage message) {
        this.add(message, this.messages.size());
    }

    /**
     * Add a new error message at the specified index.
     *
     * @param message Message to be added to the error list.
     * @param index   Position in the error list at which to insert the message.
     */
    public void add(ErrorMessage message, int index) {
        if (this.errorCount > this.getErrorLimit()) {
            return;
        }
        else if (this.errorCount == this.getErrorLimit()) {
            this.messages.add(new ErrorMessage(
                (Program) null,
                message.getLine(),
                message.getPosition(),
                "Error Limit of " + getErrorLimit() + " exceeded."
            ));
            this.errorCount++; // Subsequent errors will not be added; see if statement above
            return;
        }

        this.messages.add(index, message);
        if (message.isWarning()) {
            this.warningCount++;
        }
        else {
            this.errorCount++;
        }
    }

    /**
     * Count the number of error messages in the list.
     *
     * @return Number of error messages in the list.
     */
    public int getErrorCount() {
        return this.errorCount;
    }

    /**
     * Count the number of warning messages in the list.
     *
     * @return Number of warning messages in the list.
     */
    public int getWarningCount() {
        return this.warningCount;
    }

    /**
     * Check to see if the error limit has been exceeded.
     *
     * @return <code>true</code> if the error count exceeds the limit, or <code>false</code> otherwise.
     * @see #getErrorCount()
     * @see #getErrorLimit()
     */
    public boolean hasExceededErrorLimit() {
        return this.errorCount > this.getErrorLimit();
    }

    /**
     * Get the maximum number of errors which can be produced by a single assembler run.
     *
     * @return The error limit.
     */
    public int getErrorLimit() {
        return Application.MAXIMUM_ERROR_MESSAGES;
    }

    /**
     * Produce a report containing only errors, excluding warnings.
     *
     * @return String containing the report.
     */
    public String generateErrorReport() {
        return this.generateReport(false);
    }

    /**
     * Produce a report containing only warnings, excluding errors.
     *
     * @return String containing the report.
     */
    public String generateWarningReport() {
        return this.generateReport(true);
    }

    /**
     * Produce a report containing both warnings and errors, with warnings listed first.
     *
     * @return String containing the report.
     */
    public String generateErrorAndWarningReport() {
        return this.generateWarningReport() + this.generateErrorReport();
    }

    /**
     * Produce a report containing either only warnings or only errors.
     *
     * @param reportWarnings <code>true</code> if only warnings should be reported, or <code>false</code>
     *                       if only errors should be reported.
     * @return String containing the report.
     */
    private String generateReport(boolean reportWarnings) {
        StringBuilder report = new StringBuilder();
        for (ErrorMessage message : this.messages) {
            if (message.isWarning() == reportWarnings) {
                report.append((reportWarnings) ? WARNING_MESSAGE_PREFIX : ERROR_MESSAGE_PREFIX);
                if (!message.getFilename().isEmpty()) {
                    report.append(FILENAME_PREFIX).append('"').append(new File(message.getFilename()).getPath()).append('"');
                }
                if (message.getLine() > 0) {
                    report.append(LINE_PREFIX).append(message.getMacroExpansionHistory()).append(message.getLine());
                }
                if (message.getPosition() > 0) {
                    report.append(POSITION_PREFIX).append(message.getPosition());
                }
                report.append(MESSAGE_SEPARATOR).append(message.getMessage()).append('\n');
            }
        }
        return report.toString();
    }
}

