/*
Copyright (c) 2009,  Ingo Kofler, ITEC, Klagenfurt University, Austria

Developed by Ingo Kofler (ingo.kofler@itec.uni-klu.ac.at)

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

package mars.tools.bhtsim;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;

/**
 * Simulates the actual functionality of a Branch History Table (BHT).
 * <p>
 * The BHT consists of a number of BHT entries which are used to perform branch prediction.
 * The entries of the BHT are stored as a Vector of BHTEntry objects.
 * The number of entries is configurable but has to be a power of 2.
 * The history kept by each BHT entry is also configurable during run-time.
 * A change of the configuration however causes a complete reset of the BHT.
 * <p>
 * The typical interaction is as follows:
 * <ul>
 * <li>Construction of a BHT with a certain number of entries with a given history size.</li>
 * <li>When encountering a branch instruction the index of the relevant BHT entry is calculated via the {@link BHTableModel#getIndexForAddress(int)} method.</li>
 * <li>The current prediction of the BHT entry at the calculated index is obtained via the {@link BHTableModel#getPrediction(int)} method.</li>
 * <li>After detecting if the branch was really taken or not, this feedback is provided to the BHT by the {@link BHTableModel#updatePrediction(int, boolean)} method.</li>
 * </ul>
 * <p>
 * Additionally it serves as TableModel that can be directly used to render the state of the BHT in a JTable.
 * Feedback provided to the BHT causes a change of the internal state and a repaint of the table(s) associated to this model.
 *
 * @author Ingo Kofler (ingo.kofler@itec.uni-klu.ac.at)
 */
public class BHTableModel extends AbstractTableModel {
    /**
     * Names of the table columns.
     */
    private static final String[] COLUMN_NAMES = {
        "Index",
        "History",
        "Prediction",
        "Correct",
        "Incorrect",
        "Precision",
    };
    /**
     * Types of the table columns.
     */
    private static final Class<?>[] COLUMN_CLASSES = {
        Integer.class,
        String.class,
        String.class,
        Integer.class,
        Integer.class,
        Double.class,
    };

    /**
     * Vector holding the entries of the BHT.
     */
    private Vector<BHTEntry> entries;

    /**
     * Constructs a new BHT with given number of entries and history size.
     *
     * @param numEntries  number of entries in the BHT
     * @param historySize Size of the history (in bits/number of past branches).
     */
    public BHTableModel(int numEntries, int historySize, boolean initVal) {
        this.initialize(numEntries, historySize, initVal);
    }

    /**
     * Returns the name of the given column of the table.
     *
     * @param column The index of the column.
     * @return Name of the given column.
     */
	@Override
    public String getColumnName(int column) {
		if (column < 0 || column > COLUMN_NAMES.length) {
			throw new IllegalArgumentException("Illegal column index " + column
                + " (must be in range 0.." + (COLUMN_NAMES.length - 1) + ")");
		}

        return COLUMN_NAMES[column];
    }

    /**
     * Returns the class/type of the given column of the table.
     * Required by the TableModel interface.
     *
     * @param column the index of the column
     * @return Class representing the type of the given column.
     */
	@Override
    public Class<?> getColumnClass(int column) {
		if (column < 0 || column > COLUMN_CLASSES.length) {
			throw new IllegalArgumentException("Illegal column index " + column
                + " (must be in range 0.." + (COLUMN_CLASSES.length - 1) + ")");
		}

        return COLUMN_CLASSES[column];
    }

    /**
     * Returns the number of columns.
     *
     * @return The number of columns.
     */
    @Override
    public int getColumnCount() {
        return COLUMN_CLASSES.length;
    }

    /**
     * Returns the number of entries of the BHT.
     *
     * @return Number of rows (i.e. entries) in the BHT.
     */
    @Override
    public int getRowCount() {
        return this.entries.size();
    }

    /**
     * Returns the value of the cell at the given row and column.
     *
     * @param row    The row index.
     * @param column The column index.
     * @return The value of the cell.
     */
    @Override
    public Object getValueAt(int row, int column) {
        BHTEntry entry = this.entries.elementAt(row);
		if (entry == null) {
			return "";
		}

		if (column == 0) {
			return row;
		}
		if (column == 1) {
			return entry.getHistoryAsString();
		}
		if (column == 2) {
			return entry.getPredictionAsStr();
		}
		if (column == 3) {
			return entry.getCorrectCount();
		}
		if (column == 4) {
			return entry.getIncorrectCount();
		}
		if (column == 5) {
			return entry.getPrecision();
		}

        return "";
    }

    /**
     * Initializes the BHT with the given size and history.
     * All previous data like the BHT entries' history and statistics will get lost.
     * A refresh of the table that use this BHT as model will be triggered.
     *
     * @param entryCount        Number of entries in the BHT (has to be a power of 2).
     * @param historySize       Size of the history to consider.
     * @param initialPrediction Initial value for each entry (true means take branch, false means do not take branch).
     */
    public void initialize(int entryCount, int historySize, boolean initialPrediction) {
		if (entryCount <= 0 || (entryCount & (entryCount - 1)) != 0) {
			throw new IllegalArgumentException("Number of entries must be a positive power of 2.");
		}
		if (historySize < 1 || historySize > 2) {
			throw new IllegalArgumentException("Only history sizes of 1 or 2 supported.");
		}

        this.entries = new Vector<>();
        for (int index = 0; index < entryCount; index++) {
            this.entries.add(new BHTEntry(historySize, initialPrediction));
        }

        // Refresh the table(s)
        this.fireTableStructureChanged();
    }

    /**
     * Returns the index into the BHT for a given branch instruction address.
     * A simple direct mapping is used.
     *
     * @param address The address of the branch instruction.
     * @return The index into the BHT.
     */
    public int getIndexForAddress(int address) {
		if (address < 0) {
			throw new IllegalArgumentException("No negative addresses supported");
		}

        return (address >> 2) % this.entries.size();
    }

    /**
     * Retrieve the prediction for the i-th BHT entry.
     *
     * @param index The index of the entry in the BHT.
     * @return The prediction to take (true) or do not take (false) the branch.
     */
    public boolean getPrediction(int index) {
		if (index < 0 || index > this.entries.size()) {
			throw new IllegalArgumentException("Only indexes in the range 0 to " + (this.entries.size() - 1) + " allowed");
		}

        return this.entries.elementAt(index).getCurrentPrediction();
    }

    /**
     * Updates the BHT entry with the outcome of the branch instruction.
     * This causes a change in the model and signals to update the connected table(s).
     *
     * @param index       The index of the entry in the BHT.
     * @param branchTaken Whether the branch was taken.
     */
    public void updatePrediction(int index, boolean branchTaken) {
		if (index < 0 || index > this.entries.size()) {
			throw new IllegalArgumentException("Only indexes in the range 0 to " + (this.entries.size() - 1) + " allowed");
		}

        this.entries.elementAt(index).updatePrediction(branchTaken);
        this.fireTableRowsUpdated(index, index);
    }
}
