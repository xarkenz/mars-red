package mars.assembler.log;

import java.util.Objects;

/**
 * Class representing some location in a source file given to the assembler. The location can reference either
 * a line as a whole or a specific column within the line.
 *
 * @author Sean Clarke, October 2024
 */
public class SourceLocation implements Comparable<SourceLocation> {
    private final String filename;
    private final int lineIndex;
    private final int columnIndex;

    /**
     * Create a new <code>SourceLocation</code> referencing a file as a whole.
     *
     * @param filename The name of the source file.
     */
    public SourceLocation(String filename) {
        this.filename = filename;
        this.lineIndex = -1;
        this.columnIndex = -1;
    }

    /**
     * Create a new <code>SourceLocation</code> referencing a line as a whole.
     *
     * @param filename  The name of the source file.
     * @param lineIndex The zero-based index of the line.
     */
    public SourceLocation(String filename, int lineIndex) {
        this.filename = filename;
        this.lineIndex = lineIndex;
        this.columnIndex = -1;
    }

    /**
     * Create a new <code>SourceLocation</code> referencing a specific column in a line.
     *
     * @param filename    The name of the source file.
     * @param lineIndex   The zero-based index of the line.
     * @param columnIndex The zero-based index of the column.
     */
    public SourceLocation(String filename, int lineIndex, int columnIndex) {
        this.filename = filename;
        this.lineIndex = lineIndex;
        this.columnIndex = columnIndex;
    }

    /**
     * Get the name of the source file referenced by this location.
     *
     * @return The source filename, or <code>null</code> if not applicable.
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Get the line number referenced by this location, where the first line in the file is line 0.
     *
     * @return The zero-based line index.
     */
    public int getLineIndex() {
        return this.lineIndex;
    }

    /**
     * Get the column number referenced by this location, where the beginning of the line is column 0.
     * If this location represents the entire line, the column number will be negative.
     *
     * @return The zero-based column index, or -1 if not applicable.
     */
    public int getColumnIndex() {
        return this.columnIndex;
    }

    /**
     * Generates a new location referencing the line as a whole rather than a particular column in the line.
     *
     * @return The line location.
     */
    public SourceLocation toLineLocation() {
        return new SourceLocation(this.filename, this.lineIndex);
    }

    /**
     * Generates a new location referencing a column in the line referenced by this location.
     *
     * @param columnIndex The zero-based index of the column.
     * @return The column location.
     */
    public SourceLocation toColumnLocation(int columnIndex) {
        return new SourceLocation(this.filename, this.lineIndex, columnIndex);
    }

    /**
     * Compare two locations. The {@link #getFilename() path} is compared first; if it is the same, the
     * {@link #getLineIndex() lineIndex} is compared; if it is the same, the {@link #getColumnIndex() columnIndex}
     * is compared.
     *
     * @param that The location to compare against.
     * @return An integer whose value is positive, negative, or zero to indicate whether <code>this</code> is
     *         greater than, less than, or equal to <code>that</code>, respectively.
     */
    @Override
    public int compareTo(SourceLocation that) {
        // Order by path, then line index, then column index
        int compare;
        if ((compare = this.filename.compareTo(that.filename)) != 0) {
            return compare;
        }
        else if ((compare = Integer.compare(this.lineIndex, that.lineIndex)) != 0) {
            return compare;
        }
        else {
            return Integer.compare(this.columnIndex, that.columnIndex);
        }
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof SourceLocation that
            && Objects.equals(this.filename, that.filename)
            && this.lineIndex == that.lineIndex
            && this.columnIndex == that.columnIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.filename, this.lineIndex, this.columnIndex);
    }

    /**
     * Get the string representation of this location. For example, a location with a <code>path</code> of
     * <code>path/to/file.asm</code>, a <code>lineIndex</code> of 10, and a <code>columnIndex</code> of 20
     * produces the following string:
     * <blockquote><pre>(path/to/file.asm, line 11, column 21)</pre></blockquote>
     * <p>
     * Fields are omitted from the output if they are not applicable (i.e. <code>null</code> or negative).
     *
     * @return The string representation of this location.
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder().append('(');
        if (this.filename != null) {
            output.append(this.filename);
        }
        if (this.lineIndex >= 0) {
            if (output.length() > 1) {
                output.append(", ");
            }
            output.append("line ").append(this.lineIndex + 1);
        }
        if (this.columnIndex >= 0) {
            if (output.length() > 1) {
                output.append(", ");
            }
            output.append("column ").append(this.columnIndex + 1);
        }
        return output.append(')').toString();
    }
}
