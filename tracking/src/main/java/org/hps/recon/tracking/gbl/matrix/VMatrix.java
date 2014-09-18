package org.hps.recon.tracking.gbl.matrix;

import static java.lang.Math.min;
import java.util.ArrayList;

/**
 * Simple Vector based on ArrayList
 *
 * @author Norman A. Graf
 *
 * @version $Id$
 */
public class VMatrix
{

    private int numRows;
    private int numCols;
    //TODO replace with a simple double[]
    private ArrayList theVec;

    public VMatrix(int nRows, int nCols)
    {
        numRows = nRows;
        numCols = nCols;
        theVec = new ArrayList(nRows * nCols);
        for (int i = 0; i < numRows * nCols; ++i) {
            theVec.add(0.);
        }
    }

    public VMatrix(VMatrix m)
    {
        numRows = m.numRows;
        numCols = m.numCols;
        theVec = new ArrayList(numRows * numCols);
        for (int i = 0; i < numRows * numCols; ++i) {
            theVec.add(0.);
        }
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j < numCols; ++j) {
                theVec.set(numCols * i + j, m.get(i, j));
            }
        }
    }

    public VMatrix copy()
    {
        VMatrix aResult = new VMatrix(numRows, numCols);
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j < numCols; ++j) {
                {
                    aResult.set(i, j, get(i, j));
                }
            }
        }
        return aResult;
    }

    /// Resize Matrix.
    /**
     * \param [in] nRows Number of rows. \param [in] nCols Number of columns.
     */
    // TODO understand what this method is supposed to do.
    // may not need it as ArrayList already automatically resizes...
    public void resize(int nRows, int nCols)
    {
        numRows = nRows;
        numCols = nCols;
        theVec = new ArrayList(numRows * nCols);
        for (int i = 0; i < numRows * nCols; ++i) {
            theVec.add(0.);
        }
    }

/// Get transposed matrix.
    /**
     * \return Transposed matrix.
     */
    public VMatrix transpose()
    {
        VMatrix aResult = new VMatrix(numCols, numRows);
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j < numCols; ++j) {
                //System.out.println("row: "+i+" col: "+j+" val: "+theVec.get(numCols * i + j));
                aResult.set(j, i, (double) theVec.get(numCols * i + j));
            }
        }
        return aResult;
    }

    public void set(int row, int col, double val)
    {
        theVec.set(numCols * row + col, val);
    }

    public void addTo(int row, int col, double val)
    {
        theVec.set(numCols * row + col, (double) theVec.get(numCols * row + col) + val);
    }

    public void subFrom(int row, int col, double val)
    {
        theVec.set(numCols * row + col, (double) theVec.get(numCols * row + col) - val);
    }

    public double get(int row, int col)
    {
        return (double) theVec.get(numCols * row + col);
    }

/// Get number of rows.
    /**
     * \return Number of rows.
     */
    int getNumRows()
    {
        return numRows;
    }

/// Get number of columns.
    /**
     * \return Number of columns.
     */
    int getNumCols()
    {
        return numCols;
    }

/// Multiplication Matrix*Vector.
    VVector times(VVector aVector)
    {
        VVector aResult = new VVector(numRows);
        for (int i = 0; i < numRows; ++i) {
            double sum = 0.0;
            for (int j = 0; j < numCols; ++j) {
                sum += (double) theVec.get(numCols * i + j) * aVector.get(j);
            }
            aResult.set(i, sum);
        }
        return aResult;
    }

// Multiplication Matrix*Matrix.
    VMatrix times(VMatrix aMatrix)
    {

        VMatrix aResult = new VMatrix(numRows, aMatrix.numCols);
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j < aMatrix.numCols; ++j) {
                double sum = 0.0;
                for (int k = 0; k < numCols; ++k) {
                    sum += (double) theVec.get(numCols * i + k) * aMatrix.get(k, j);
                }
                aResult.set(i, j, sum);
            }
        }
        return aResult;
    }

/// Addition Matrix+Matrix.
    VMatrix plus(VMatrix aMatrix)
    {
        VMatrix aResult = new VMatrix(numRows, numCols);
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j < numCols; ++j) {
                aResult.set(i, j, (double) theVec.get(numCols * i + j) + (double) aMatrix.get(i, j));
            }
        }
        return aResult;
    }

/// Print matrix.
    public void print()
    {
        System.out.println(" VMatrix: " + numRows + "*" + numCols);
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j < numCols; ++j) {
                if (j % 5 == 0) {
                    System.out.format("%n%4d " + "," + "%4d" + " - " + "%4d" + " : ", i, j, min(j + 4, numCols));
                }
                System.out.format("%13f", theVec.get(numCols * i + j));
            }
        }
        System.out.print("\n\n\n");
    }
}
