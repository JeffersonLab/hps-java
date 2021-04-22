package org.hps.recon.tracking.gbl.matrix;

import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.min;

/**
 * Simple Vector based on ArrayList
 */
public class VVector {

    private int numRows;
    private ArrayList theVec;

    public VVector(int nRows) {
        numRows = nRows;
        theVec = new ArrayList(nRows);
        for (int i = 0; i < numRows; ++i) {
            theVec.add(0.);
        }
    }

    public VVector(VVector old) {
        numRows = old.numRows;
        theVec = new ArrayList(old.theVec);
    }

    public VVector(List list) {
        theVec = new ArrayList(list);
        theVec.trimToSize();
        numRows = theVec.size();
    }

    public void set(int row, double val) {
        theVec.set(row, val);
    }

    public void addTo(int row, double val) {
        theVec.set(row, (double) theVec.get(row) + val);
    }

    public void subFrom(int row, double val) {
        theVec.set(row, (double) theVec.get(row) - val);
    }

    public double get(int row) {
        return (double) theVec.get(row);
    }

    // Get part of vector.
    /**
     * \param [in] len Length of part. \param [in] start Offset of part. \return Part of vector.
     */
    public VVector getVec(int len, int start) {
        return new VVector(theVec.subList(start, start + len));

    }

    // / Put part of vector.
    /**
     * \param [in] aVector Vector with part. \param [in] start Offset of part.
     */
    public void putVec(VVector aVector, int start) {
        theVec.addAll(start, aVector.theVec);
    }

    // / Get number of rows.
    /**
     * \return Number of rows.
     */
    int getNumRows() {
        return numRows;
    }

    // / Print vector.
    public void print() {
        System.out.println(theVec.size());
        for (int i = 0; i < numRows; ++i) {
            if (i % 5 == 0) {
                System.out.format("%n%4d " + " - " + "%4d" + " : ", i, min(i + 4, numRows));
            }
            System.out.format("%13f", theVec.get(i));
        }
        System.out.print("\n\n\n");
    }

    // / Subtraction Vector-Vector.
    VVector minus(VVector aVector) {
        VVector aResult = new VVector(numRows);
        for (int i = 0; i < numRows; ++i) {
            aResult.set(i, (double) theVec.get(i) - aVector.get(i));
        }
        return aResult;
    }

    // / Assignment Vector=Vector.
    public VVector copy() {
        VVector aResult = new VVector(numRows);
        for (int i = 0; i < numRows; ++i) {
            aResult.set(i, get(i));
        }
        return aResult;
    }

}
