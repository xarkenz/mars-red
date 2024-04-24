/*
 * HardcopyWriter class comes from the book "Java Examples in a Nutshell,
 * 3rd Edition" by David Flanagan.  Publisher is O'Reilly, ISBN is
 * 0-596-00620-9.  Published Jan 2004.  Web page for the book is:
 *  http://www.oreilly.com/catalog/jenut3/
 *
 * Concerning my use of their code, the O'Reilly policy is described at:
 *  http://www.oreilly.com/pub/a/oreilly/ask_tim/2001/codepolicy.html
 *
 * Here is a quote copied from that web page:
 *
 *    "What is our policy with regard to programmers incorporating code
 *    examples from books into their work? I get asked this all the time."
 *    The short answer is this:
 *    You can use and redistribute example code from our books for any
 *    non-commercial purpose (and most commercial purposes) as long as
 *    you acknowledge their source and authorship. The source of the code
 *    should be noted in any documentation as well as in the program code
 *    itself (as a comment). The attribution should include author, title,
 *    publisher, and ISBN.
 *
 * I have made some minor adjustments to the code to fit my needs in
 * regards to font sizing and spacing.  This code will be used to print
 * multi-page plain text documents, specifically listings of MIPS assembler
 * source code.  I am extremely grateful for this generous code use policy!
 *
 * Pete Sanderson, August 2004
 */

package mars.util;

import java.awt.*;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * <code>HardcopyWriter</code> class from the book "Java Examples in a Nutshell,
 * 3rd Edition" by David Flanagan.  Publisher is O'Reilly, ISBN is
 * 0-596-00620-9.  Published Jan 2004.  Web page for the book is
 * <a href="http://www.oreilly.com/catalog/jenut3/">here</a>.
 * <p>
 * A character output stream that sends output to a printer.  I made only
 * a couple minor changes. -- Pete Sanderson
 */
public class HardcopyWriter extends Writer {
    protected PrintJob job; // The PrintJob object in use
    protected Graphics page; // Graphics object for current page
    protected String jobName; // The name of the print job
    protected int fontSize; // Point size of the font
    protected String time; // Current time (appears in header)
    protected Dimension pageSize; // Size of the page (in dots)
    protected int pageResolutionDPI; // Page resolution in dots per inch
    protected Font font, headerFont; // Body font and header font
    protected FontMetrics bodyMetrics; // Metrics for the body font
    protected FontMetrics headerMetrics; // Metrics for the header font
    protected int leftX, topY; // Upper-left corner inside margin
    protected int width, height; // Size (in dots) inside margins
    protected int headerY; // Baseline of the page header
    protected int charWidth; // The width of each character
    protected int lineHeight; // The height of each line
    protected int lineAscent; // Offset of font baseline
    protected int charsPerLine; // Number of characters per line
    protected int linesPerPage; // Number of lines per page
    protected int charsPerTab = 4; // Added by Pete Sanderson 8-17-04
    protected int charNum = 0, lineNum = 0; // Current column and line position
    protected int pageNum = 0; // Current page number
    // A field to save state between invocations of the write( ) method
    private boolean lastCharWasReturn = false;
    // The lock to ensure one print dialog is open at a time
    protected static final Object PRINT_DIALOG_LOCK = new Object();

    /**
     * Construct a new HardcopyWriter.
     *
     * @param frame The parent frame, required for all printing in Java.
     * @param jobName The name of the print job, which appears left justified at the top of each printed page.
     * @param fontSize The font size in points.
     * @param leftMargin The left margin in inches (or fractions of inches).
     * @param rightMargin The right margin in inches (or fractions of inches).
     * @param topMargin The top margin in inches (or fractions of inches).
     * @param bottomMargin The bottom margin in inches (or fractions of inches).
     */
    public HardcopyWriter(Frame frame, String jobName, int fontSize, double leftMargin, double rightMargin, double topMargin, double bottomMargin) throws HardcopyWriter.PrintCanceledException {
        // Get the PrintJob object with which we'll do all the printing.
        // The call is synchronized on the static printprops object, which
        // means that only one print dialog can be popped up at a time.
        // If the user clicks Cancel in the print dialog, throw an exception.
        Toolkit toolkit = frame.getToolkit(); // get Toolkit from Frame
        synchronized (PRINT_DIALOG_LOCK) {
            //job = toolkit.getPrintJob(frame, jobname, printprops);
            //*******************************************
            // SANDERSON MOD 8-17-2004:
            // Currently we will ignore user specifications from Print dialog
            // such as page ranges and number of copies.  But ja and pa can be
            // queried to get and act upon them (in future release).
            JobAttributes ja = new JobAttributes();
            PageAttributes pa = new PageAttributes();
            job = toolkit.getPrintJob(frame, jobName, ja, pa);
            //*******************************************
        }
        if (job == null) {
            throw new PrintCanceledException("User cancelled print request");
        }
        /* ******************************************************
         SANDERSON OVERRIDE 8-17-2004:
         I didn't like the results produced by the code below, so am commenting
         it out and just setting pagedpi to 72.  This assures, among other things,
         that the client asking for 10 point font will really get 10 point font!

         pagesize = job.getPageDimension( ); // query the page size
         pagedpi = job.getPageResolution( ); // query the page resolution
         // Bug Workaround:
         // On Windows, getPageDimension( ) and getPageResolution don't work, so
         // we've got to fake them.
         if (System.getProperty("os.name").regionMatches(true,0,"windows",0,7)){
         // Use screen dpi, which is what the PrintJob tries to emulate
         pagedpi = toolkit.getScreenResolution( );
         // Assume a 8.5" x 11" page size. A4 paper users must change this.
         pagesize = new Dimension((int)(8.5 * pagedpi), 11*pagedpi);
         // We also have to adjust the fontsize. It is specified in points,
         // (1 point = 1/72 of an inch) but Windows measures it in pixels.
         fontsize = fontsize * pagedpi / 72;
         }
         ***********************************/

        pageResolutionDPI = 72;
        pageSize = new Dimension((int) (8.5 * pageResolutionDPI), 11 * pageResolutionDPI);
        fontSize = fontSize * pageResolutionDPI / 72;

        // Compute coordinates of the upper-left corner of the page.
        // I.e. the coordinates of (leftMargin, topMargin). Also compute
        // the width and height inside of the margins.
        leftX = (int) (leftMargin * pageResolutionDPI);
        topY = (int) (topMargin * pageResolutionDPI);
        width = pageSize.width - (int) ((leftMargin + rightMargin) * pageResolutionDPI);
        height = pageSize.height - (int) ((topMargin + bottomMargin) * pageResolutionDPI);
        // Get body font and font size
        font = new Font("Monospaced", Font.PLAIN, fontSize);
        bodyMetrics = frame.getFontMetrics(font);
        lineHeight = bodyMetrics.getHeight();
        lineAscent = bodyMetrics.getAscent();
        charWidth = bodyMetrics.charWidth('0'); // Assumes a monospaced font!
        // Now compute the number of columns and lines
        // that will fit inside the margins
        charsPerLine = width / charWidth;
        linesPerPage = height / lineHeight;
        // Get header font information
        // And compute baseline of page header: 1/8" above the top margin
        headerFont = new Font("SansSerif", Font.ITALIC, fontSize);
        headerMetrics = frame.getFontMetrics(headerFont);
        headerY = topY - (int) (0.125 * pageResolutionDPI) - headerMetrics.getHeight() + headerMetrics.getAscent();
        // Compute the date/time string to display in the page header
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
        df.setTimeZone(TimeZone.getDefault());
        time = df.format(new Date());
        this.jobName = jobName; // save name
        this.fontSize = fontSize; // save font size
    }

    /**
     * This is the write() method of the stream. All Writer subclasses
     * implement this. All other versions of write() are variants of this one
     */
    @Override
    public void write(char[] buffer, int index, int len) {
        synchronized (this.lock) { // For thread safety
            // Loop through all the characters passed to us
            for (int i = index; i < index + len; i++) {
                // If we haven't begun a page (or a new page), do that now.
                if (page == null) {
                    beginNewPage();
                }
                // If the character is a line terminator, then begin new line,
                // unless it is a \n immediately after a \r.
                if (buffer[i] == '\n') {
                    if (!lastCharWasReturn) {
                        beginNewLine();
                    }
                    continue;
                }
                if (buffer[i] == '\r') {
                    beginNewLine();
                    lastCharWasReturn = true;
                    continue;
                }
                else {
                    lastCharWasReturn = false;
                }
                // If it's some other non-printing character, ignore it.
                if (Character.isWhitespace(buffer[i]) && !Character.isSpaceChar(buffer[i]) && (buffer[i] != '\t')) {
                    continue;
                }
                // If no more characters will fit on the line, start new line.
                if (charNum >= charsPerLine) {
                    beginNewLine();
                    // Also start a new page, if necessary
                    if (page == null) {
                        beginNewPage();
                    }
                }
                // Now print the character:
                // If it is a space, skip one space, without output.
                // If it is a tab, skip the necessary number of spaces.
                // Otherwise, print the character.
                // It is inefficient to draw only one character at a time, but
                // because our FontMetrics don't match up exactly to what the
                // printer uses, we need to position each character individually
                if (Character.isSpaceChar(buffer[i])) {
                    charNum++;
                }
                else if (buffer[i] == '\t') {
                    charNum += charsPerTab - (charNum % charsPerTab);
                }
                else {
                    page.drawChars(buffer, i, 1, leftX + charNum * charWidth, topY + (lineNum * lineHeight) + lineAscent);
                    charNum++;
                }
            }
        }
    }

    /**
     * This is the flush( ) method that all Writer subclasses must implement.
     * There is no way to flush a PrintJob without prematurely printing the
     * page, so we don't do anything.
     */
    @Override
    public void flush() { /* do nothing */ }

    /**
     * This is the close( ) method that all Writer subclasses must implement.
     * Print the pending page (if any) and terminate the PrintJob.
     */
    @Override
    public void close() {
        synchronized (this.lock) {
            if (page != null) {
                page.dispose(); // Send page to the printer
            }
            job.end(); // Terminate the job
        }
    }

    /**
     * Set the font style. The argument should be one of the font style
     * constants defined by the java.awt.Font class. All subsequent output
     * will be in that style. This method relies on all styles of the
     * Monospaced font having the same metrics.
     */
    public void setFontStyle(int style) {
        synchronized (this.lock) {
            // Try to set a new font, but do nothing if it fails
            try {
                font = new Font("Monospaced", style, fontSize);
            }
            catch (Exception e) {
                // The font will not change
            }
            // If a page is pending, set the new font.
            if (page != null) {
                page.setFont(font);
            }
        }
    }

    /**
     * End the current page. Subsequent output will be on a new page.
     */
    public void pageBreak() {
        synchronized (this.lock) {
            beginNewPage();
        }
    }

    /**
     * Return the number of columns of characters that fit on the page
     */
    public int getCharactersPerLine() {
        return this.charsPerLine;
    }

    /**
     * Return the number of lines that fit on a page
     */
    public int getLinesPerPage() {
        return this.linesPerPage;
    }

    /**
     * This internal method begins a new line
     */
    protected void beginNewLine() {
        charNum = 0; // Reset character number to 0
        lineNum++; // Increment line number
        if (lineNum >= linesPerPage) { // If we've reached the end of page
            page.dispose(); // send page to printer
            page = null; // but don't start a new page yet.
        }
    }

    /**
     * This internal method begins a new page and prints the header.
     */
    protected void beginNewPage() {
        page = job.getGraphics(); // Begin the new page
        lineNum = 0;
        charNum = 0; // Reset line and char number
        pageNum++; // Increment page number
        page.setFont(headerFont); // Set the header font.
        page.drawString(jobName, leftX, headerY); // Print job name left justified
        String s = "- " + pageNum + " -"; // Print the page # centered.
        int w = headerMetrics.stringWidth(s);
        page.drawString(s, leftX + (this.width - w) / 2, headerY);
        w = headerMetrics.stringWidth(time); // Print date right justified
        page.drawString(time, leftX + width - w, headerY);
        // Draw a line beneath the header
        int y = headerY + headerMetrics.getDescent() + 1;
        page.drawLine(leftX, y, leftX + width, y);
        // Set the basic monospaced font for the rest of the page.
        page.setFont(font);
    }

    /**
     * This is the exception class that the HardcopyWriter constructor
     * throws when the user clicks "Cancel" in the print dialog box.
     */
    public static class PrintCanceledException extends Exception {
        public PrintCanceledException(String msg) {
            super(msg);
        }
    }
}