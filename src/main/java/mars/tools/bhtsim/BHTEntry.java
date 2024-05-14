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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a single entry of the Branch History Table.
 * <p>
 * The entry holds the information about former branch predictions and outcomes.
 * The number of past branch outcomes can be configured and is called the history.
 * The semantics of the history of size <var>n</var> is as follows.
 * The entry will change its prediction, if it mispredicts the branch <var>n</var> times in series.
 * The prediction of the entry can be obtained by the {@link BHTEntry#getCurrentPrediction()} method.
 * Feedback of taken or not taken branches is provided to the entry via the {@link BHTEntry#updatePrediction(boolean)} method.
 * This causes the history and the prediction to be updated.
 * <p>
 * Additionally the entry keeps track about how many times the prediction was correct or incorrect.
 * The statistics can be obtained by the methods {@link BHTEntry#getCorrectCount()},
 * {@link BHTEntry#getIncorrectCount()} and {@link BHTEntry#getPrecision()}.
 *
 * @author Ingo Kofler (ingo.kofler@itec.uni-klu.ac.at)
 */
public class BHTEntry {
    /**
     * The history of the BHT entry. Each boolean value signals if the branch was taken or not.
     * The value at index n-1 represents the most recent branch outcome.
     */
    private final boolean[] branchHistory;
    /**
     * The current prediction.
     */
    private boolean currentPrediction;
    /**
     * Absolute number of incorrect predictions.
     */
    private int incorrectCount;
    /**
     * Absolute number of correct predictions.
     */
    private int correctCount;

    /**
     * Constructs a BHT entry with a given history size.
     * <p>
     * The size of the history can only be set via the constructor and cannot be changed afterwards.
     *
     * @param historySize       Number of past branch outcomes to remember.
     * @param initialPrediction The initial value of the entry (true means take branch, false means do not take branch).
     */
    public BHTEntry(int historySize, boolean initialPrediction) {
        this.currentPrediction = initialPrediction;
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.branchHistory = new boolean[historySize];
        Arrays.fill(this.branchHistory, initialPrediction);
    }

    /**
     * Returns the branch prediction based on the history.
     *
     * @return true if prediction is to take the branch, false otherwise.
     */
    public boolean getCurrentPrediction() {
        return this.currentPrediction;
    }

    /**
     * Updates the entry's history and prediction.
     * This method provides feedback for a prediction.
     * The history and the statistics are updated accordingly.
     * Based on the updated history, a new prediction is calculated.
     *
     * @param branchTaken Signals if the branch was taken (true) or not (false).
     */
    public void updatePrediction(boolean branchTaken) {
        // Update history
        for (int index = 0; index < this.branchHistory.length - 1; index++) {
            this.branchHistory[index] = this.branchHistory[index + 1];
        }
        this.branchHistory[this.branchHistory.length - 1] = branchTaken;

        // If the prediction was correct, update stats and keep prediction
        if (branchTaken == this.currentPrediction) {
            this.correctCount++;
        }
        else {
            this.incorrectCount++;

            // Check if the prediction should change
            boolean changePrediction = true;

            for (boolean previousBranchTaken : this.branchHistory) {
                if (previousBranchTaken != branchTaken) {
                    changePrediction = false;
                    break;
                }
            }

			if (changePrediction) {
				this.currentPrediction = !this.currentPrediction;
			}
        }
    }

    /**
     * Get the absolute number of mispredictions.
     *
     * @return Number of incorrect predictions (mispredictions).
     */
    public int getIncorrectCount() {
        return incorrectCount;
    }

    /**
     * Get the absolute number of correct predictions.
     *
     * @return Number of correct predictions.
     */
    public int getCorrectCount() {
        return correctCount;
    }

    /**
     * Get the percentage of correct predictions.
     *
     * @return The percentage of correct predictions.
     */
    public double getPrecision() {
        int totalPredictions = this.incorrectCount + this.correctCount;
        return (totalPredictions <= 0) ? 0.0 : this.correctCount * 100.0 / totalPredictions;
    }

    /**
     * Builds a string representation of the BHT entry's history.
     * The history is a sequence of flags that signal if the branch was taken (T) or not taken (NT).
     *
     * @return A string representation of the BHT entry's history.
     */
    public String getHistoryAsString() {
        List<String> historyStrings = new ArrayList<>(this.branchHistory.length);
        for (boolean branchTaken : this.branchHistory) {
            historyStrings.add(branchTaken ? "T" : "NT");
        }
        return String.join(", ", historyStrings);
    }

    /**
     * Returns a string representation of the BHT entry's current prediction.
     * The prediction can be either to TAKE or NOT TAKE the branch.
     *
     * @return A string representation of the BHT entry's current prediction.
     */
    public String getPredictionAsStr() {
        return this.currentPrediction ? "TAKE" : "NOT TAKE";
    }
}
