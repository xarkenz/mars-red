package mars.venus;

public class TableCache<T> {
    private int rowCount;
    private int columnCount;
    private Object[][] cache;

    public TableCache() {
        this(0, 0);
    }

    public TableCache(int rowCount, int columnCount) {
        this.checkDimensions(rowCount, columnCount);

        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.cache = new Object[rowCount][columnCount];
    }

    public void updateDimensions(int rowCount, int columnCount) {
        if (rowCount != this.rowCount || columnCount != this.columnCount) {
            this.checkDimensions(rowCount, columnCount);

            Object[][] updatedCache = new Object[rowCount][columnCount];
            int rowsToCopy = Math.min(this.rowCount, rowCount);
            int columnsToCopy = Math.min(this.columnCount, columnCount);
            for (int row = 0; row < rowsToCopy; row++) {
                System.arraycopy(this.cache[row], 0, updatedCache[row], 0, columnsToCopy);
            }

            this.cache = updatedCache;
            this.rowCount = rowCount;
            this.columnCount = columnCount;
        }
    }

    @SuppressWarnings("unchecked")
    public T get(int row, int column) {
        this.checkCoordinates(row, column);

        return (T) this.cache[row][column];
    }

    public void set(int row, int column, T value) {
        this.checkCoordinates(row, column);

        this.cache[row][column] = value;
    }

    private void checkDimensions(int rowCount, int columnCount) {
        if (rowCount < 0 || columnCount < 0) {
            throw new IllegalArgumentException("invalid dimensions: " + rowCount + ", " + columnCount);
        }
    }

    private void checkCoordinates(int row, int column) {
        if (row < 0 || row >= this.rowCount || column < 0 || column >= this.columnCount) {
            throw new IllegalArgumentException("invalid coordinates: " + row + ", " + column);
        }
    }
}
