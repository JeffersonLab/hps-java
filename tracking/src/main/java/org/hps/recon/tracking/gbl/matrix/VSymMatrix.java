package org.hps.recon.tracking.gbl.matrix;

import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.abs;
import java.util.ArrayList;

/**
 * Simple symmetric Matrix based on ArrayList
 *
 * @author Norman A. Graf
 * @version $Id$
 */
public class VSymMatrix {

    private int numRows;
    // TODO replace with a simple double[]
    private ArrayList theVec;

    public VSymMatrix(int nRows) {
        numRows = nRows;
        theVec = new ArrayList((nRows * nRows + nRows) / 2);
        for (int i = 0; i < (nRows * nRows + nRows) / 2; ++i) {
            theVec.add(i, 0.);
        }
    }

    public VSymMatrix copy() {
        VSymMatrix aResult = new VSymMatrix(numRows);
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j < numRows; ++j) {
                {
                    aResult.set(i, j, get(i, j));
                }
            }
        }
        return aResult;
    }

    // / Resize symmetric matrix.
    /**
     * \param [in] nRows Number of rows.
     */
    // TODO understand what this method is supposed to do.
    // may not need it as ArrayList already automatically resizes...
    void resize(int nRows) {
        numRows = nRows;
        theVec = new ArrayList(nRows * nRows + nRows);
        for (int i = 0; i < nRows * nRows + nRows; ++i) {
            theVec.add(0.);
        }
    }

    // / Get number of rows (= number of colums).
    /**
     * \return Number of rows.
     */
    int getNumRows() {
        return numRows;
    }

    public void set(int row, int col, double val) {
        theVec.set((row * row + row) / 2 + col, val);
    }

    public void addTo(int row, int col, double val) {
        theVec.set((row * row + row) / 2 + col, (double) theVec.get((row * row + row) / 2 + col) + val);
    }

    public void subFrom(int row, int col, double val) {
        theVec.set((row * row + row) / 2 + col, (double) theVec.get((row * row + row) / 2 + col) - val);
    }

    public double get(int row, int col) {
        return (double) theVec.get((row * row + row) / 2 + col);
    }

    // / Print matrix.
    void print() {
        System.out.println(" VSymMatrix: " + numRows + "*" + numRows);
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j <= i; ++j) {
                if (j % 5 == 0) {
                    System.out.format("%n%4d " + "," + "%4d" + " - " + "%4d" + " : ", i, j, min(j + 4, i));
                }
                System.out.format("%13f", theVec.get((i * i + i) / 2 + j));
            }
        }
        System.out.print("\n\n\n");
    }

    // / Subtraction SymMatrix-(sym)Matrix.
    VSymMatrix minus(VMatrix aMatrix) {
        VSymMatrix aResult = new VSymMatrix(numRows);
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j <= i; ++j) {
                aResult.set(i, j, (double) theVec.get((i * i + i) / 2 + j) - (double) aMatrix.get(i, j));
            }
        }
        return aResult;
    }

    // / Multiplication SymMatrix*Vector.
    VVector times(VVector aVector) {
        VVector aResult = new VVector(numRows);
        for (int i = 0; i < numRows; ++i) {
            aResult.set(i, (double) theVec.get((i * i + i) / 2 + i) * (double) aVector.get(i));
            for (int j = 0; j < i; ++j) {
                aResult.set(j, aResult.get(j) + (double) theVec.get((i * i + i) / 2 + j) * aVector.get(i));
                aResult.set(i, aResult.get(i) + (double) theVec.get((i * i + i) / 2 + j) * aVector.get(j));
            }
        }
        return aResult;
    }

    // / Multiplication SymMatrix*Matrix.
    VMatrix times(VMatrix aMatrix) {
        int nCol = aMatrix.getNumCols();
        VMatrix aResult = new VMatrix(numRows, nCol);
        for (int l = 0; l < nCol; ++l) {
            for (int i = 0; i < numRows; ++i) {
                aResult.set(i, l, (double) theVec.get((i * i + i) / 2 + i) * (double) aMatrix.get(i, l));
                for (int j = 0; j < i; ++j) {
                    aResult.set(j, l, aResult.get(j, l) + (double) theVec.get((i * i + i) / 2 + j) * aMatrix.get(i, l));
                    aResult.set(i, l, aResult.get(i, l) + (double) theVec.get((i * i + i) / 2 + j) * aMatrix.get(j, l));
                }
            }
        }
        return aResult;
    }

    /*
     * ============================================================================ from mpnum.F (MillePede-II by V.
     * Blobel, Univ. Hamburg) ============================================================================
     */
    // / Matrix inversion.
    /**
     * Invert symmetric N-by-N matrix V in symmetric storage mode V(1) = V11, V(2) = V12, V(3) = V22, V(4) = V13, . . .
     * replaced by inverse matrix Method of solution is by elimination selecting the pivot on the diagonal each stage.
     * The rank of the matrix is returned in NRANK. For NRANK ne N, all remaining rows and cols of the resulting matrix
     * V are set to zero. \exception 1 : matrix is singular. \return Rank of matrix.
     */
    public int invert() {

        final double eps = 1.0E-10;
        int aSize = numRows;
        int[] next = new int[aSize];
        double[] diag = new double[aSize];
        int nSize = aSize;

        int first = 1;
        for (int i = 1; i <= nSize; ++i) {
            next[i - 1] = i + 1; // set "next" pointer
            diag[i - 1] = abs((double) theVec.get((i * i + i) / 2 - 1)); // save abs of diagonal elements
        }
        next[aSize - 1] = -1; // end flag

        int nrank = 0;
        for (int i = 1; i <= nSize; ++i) { // start of loop
            int k1 = 0;
            double vkk = 0.0;

            int j1 = first;
            int previous = 0;
            int last = previous;
            // look for pivot
            while (j1 > 0) {
                int jj = (j1 * j1 + j1) / 2 - 1;
                if (abs((double) theVec.get(jj)) > max(abs(vkk), eps * diag[j1 - 1])) {
                    vkk = (double) theVec.get(jj);
                    k1 = j1;
                    last = previous;
                }
                previous = j1;
                j1 = next[j1 - 1];
            }
            // pivot found
            if (k1 > 0) {
                int kk = (k1 * k1 + k1) / 2 - 1;
                if (last <= 0) {
                    first = next[k1 - 1];
                } else {
                    next[last - 1] = next[k1 - 1];
                }
                next[k1 - 1] = 0; // index is used, reset
                nrank++; // increase rank and ...

                vkk = 1.0 / vkk;
                theVec.set(kk, -vkk);
                int jk = kk - k1;
                int jl = -1;
                for (int j = 1; j <= nSize; ++j) { // elimination
                    if (j == k1) {
                        jk = kk;
                        jl += j;
                    } else {
                        if (j < k1) {
                            ++jk;
                        } else {
                            jk += j - 1;
                        }

                        double vjk = (double) theVec.get(jk);
                        theVec.set(jk, vkk * vjk);
                        int lk = kk - k1;
                        if (j >= k1) {
                            for (int l = 1; l <= k1 - 1; ++l) {
                                ++jl;
                                ++lk;
                                theVec.set(jl, (double) theVec.get(jl) - (double) theVec.get(lk) * vjk);
                            }
                            ++jl;
                            lk = kk;
                            for (int l = k1 + 1; l <= j; ++l) {
                                ++jl;
                                lk += l - 1;
                                theVec.set(jl, (double) theVec.get(jl) - (double) theVec.get(lk) * vjk);
                            }
                        } else {
                            for (int l = 1; l <= j; ++l) {
                                ++jl;
                                ++lk;
                                theVec.set(jl, (double) theVec.get(jl) - (double) theVec.get(lk) * vjk);
                            }
                        }
                    }
                }

            } else {
                for (int k = 1; k <= nSize; ++k) {
                    if (next[k - 1] >= 0) {
                        int kk = (k * k - k) / 2 - 1;
                        for (int j = 1; j <= nSize; ++j) {
                            if (next[j - 1] >= 0) {
                                theVec.set(kk + j, 0.0); // clear matrix row/col
                            }
                        }
                    }
                }
                throw new RuntimeException("Symmetric matrix inversion is singular");// 1; // singular
            }
        }
        for (int ij = 0; ij < (nSize * nSize + nSize) / 2; ++ij) {
            theVec.set(ij, -(double) theVec.get(ij)); // finally reverse sign of all matrix elements
        }
        return nrank;
    }

}
