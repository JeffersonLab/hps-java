package org.hps.recon.tracking.gbl.matrix;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.max;
import java.util.List;

/**
 * @author Norman A. Graf
 * @version $Id$
 */
// / (Symmetric) Bordered Band Matrix.
/**
 * Separate storage of border, mixed and band parts (as vector<double>).
 * \verbatim Example for matrix size=8 with border
 * size and band width of two +- -+ | B11 B12 M13 M14 M15 M16 M17 M18 | | B12
 * B22 M23 M24 M25 M26 M27 M28 | | M13 M23
 * C33 C34 C35 0. 0. 0. | | M14 M24 C34 C44 C45 C46 0. 0. | | M15 M25 C35 C45
 * C55 C56 C57 0. | | M16 M26 0. C46 C56 C66
 * C67 C68 | | M17 M27 0. 0. C57 C67 C77 C78 | | M18 M28 0. 0. 0. C68 C78 C88 |
 * +- -+ Is stored as:: +- -+ +- -+ | B11
 * B12 | | M13 M14 M15 M16 M17 M18 | | B12 B22 | | M23 M24 M25 M26 M27 M28 | +-
 * -+ +- -+ +- -+ | C33 C44 C55 C66 C77 C88
 * | | C34 C45 C56 C67 C78 0. | | C35 C46 C57 C68 0. 0. | +- -+ \endverbatim
 */
public class BorderedBandMatrix {

    private int numSize; // /< Matrix size
    private int numBorder; // /< Border size
    private int numBand; // /< Band width
    private int numCol; // /< Band matrix size
    private VSymMatrix theBorder = new VSymMatrix(0); // /< Border part
    private VMatrix theMixed = new VMatrix(0, 0); // /< Mixed part
    private VMatrix theBand = new VMatrix(0, 0); // /< Band part

    // / Resize bordered band matrix.
    /**
     * \param nSize [in] Size of matrix \param nBorder [in] Size of border (=1
     * for q/p + additional local parameters)
     * \param nBand [in] Band width (usually = 5, for simplified jacobians = 4)
     */
    public void resize(int nSize, int nBorder, int nBand) {
        numSize = nSize;
        numBorder = nBorder;
        numCol = nSize - nBorder;
        numBand = 0;
        theBorder.resize(numBorder);
        theMixed.resize(numBorder, numCol);
        theBand.resize((nBand + 1), numCol);
    }

    // / Add symmetric block matrix.
    /**
     * Add (extended) block matrix defined by 'aVector * aWeight * aVector.T' to
     * bordered band matrix:
     * BBmatrix(anIndex(i),anIndex(j)) += aVector(i) * aWeight * aVector(j).
     * \param aWeight [in] Weight \param anIndex
     * [in] List of rows/colums to be used \param aVector [in] Vector
     */
    public void addBlockMatrix(double aWeight, int[] anIndex, double[] aVector) {
        int nBorder = numBorder;
        for (int i = 0; i < anIndex.length; ++i) {
            int iIndex = (anIndex)[i] - 1; // anIndex has to be sorted
            for (int j = 0; j <= i; ++j) {
                int jIndex = (anIndex)[j] - 1;
                if (iIndex < nBorder)
                    theBorder.addTo(iIndex, jIndex, aVector[i] * aWeight * aVector[j]);
                else if (jIndex < nBorder)
                    theMixed.addTo(jIndex, iIndex - nBorder, aVector[i] * aWeight * aVector[j]);
                else {
                    int nBand = iIndex - jIndex;
                    theBand.addTo(nBand, jIndex - nBorder, aVector[i] * aWeight * aVector[j]);
                    numBand = max(numBand, nBand); // update band width
                }
            }
        }
    }

    // / Retrieve symmetric block matrix.
    /**
     * Get (compressed) block from bordered band matrix: aMatrix(i,j) =
     * BBmatrix(anIndex(i),anIndex(j)). \param anIndex
     * [in] List of rows/colums to be used
     */
    public SymMatrix getBlockMatrix(List<Integer> anIndex) {

        SymMatrix aMatrix = new SymMatrix(anIndex.size());
        int nBorder = numBorder;
        for (int i = 0; i < anIndex.size(); ++i) {
            int iIndex = anIndex.get(i) - 1; // anIndex has to be sorted
            for (int j = 0; j <= i; ++j) {
                int jIndex = anIndex.get(j) - 1;
                if (iIndex < nBorder)
                    aMatrix.set(i, j, theBorder.get(iIndex, jIndex)); // border part of inverse
                else if (jIndex < nBorder)
                    aMatrix.set(i, j, -theMixed.get(jIndex, iIndex - nBorder)); // mixed part of inverse
                else {
                    int nBand = iIndex - jIndex;
                    aMatrix.set(i, j, theBand.get(nBand, jIndex - nBorder)); // band part of inverse
                }
                aMatrix.set(j, i, aMatrix.get(i, j));
            }
        }
        return aMatrix;
    }

    // / Solve linear equation system, partially calculate inverse.
    /**
     * Solve linear equation A*x=b system with bordered band matrix A, calculate
     * bordered band part of inverse of A. Use
     * decomposition in border and band part for block matrix algebra: | A Ct |
     * | x1 | | b1 | , A is the border part | |
     * * | | = | | , Ct is the mixed part | C D | | x2 | | b2 | , D is the band
     * part Explicit inversion of D is avoided
     * by using solution X of D*X=C (X=D^-1*C, obtained from Cholesky
     * decomposition and forward/backward substitution) |
     * x1 | | E*b1 - E*Xt*b2 | , E^-1 = A-Ct*D^-1*C = A-Ct*X | | = | | | x2 | |
     * x - X*x1 | , x is solution of D*x=b2
     * (x=D^-1*b2) Inverse matrix is: | E -E*Xt | | | , only band part of (D^-1
     * + X*E*Xt) | -X*E D^-1 + X*E*Xt | is
     * calculated \param [in] aRightHandSide Right hand side (vector) 'b' of
     * A*x=b \param [out] aSolution Solution
     * (vector) x of A*x=b
     */
    public void solveAndInvertBorderedBand(VVector aRightHandSide, VVector aSolution) {

        // decompose band
        decomposeBand();
        // invert band
        VMatrix inverseBand = invertBand();
        if (numBorder > 0) { // need to use block matrix decomposition to solve
            // solve for mixed part
            VMatrix auxMat = solveBand(theMixed); // = Xt
            VMatrix auxMatT = auxMat.transpose(); // = X
            // solve for border part
            VVector auxVec = aRightHandSide.getVec(numBorder, 0).minus(
                    auxMat.times(aRightHandSide.getVec(numCol, numBorder))); // = b1 - Xt*b2
            VSymMatrix inverseBorder = theBorder.minus(theMixed.times(auxMatT));
            inverseBorder.invert(); // = E
            VVector borderSolution = inverseBorder.times(auxVec); // = x1
            // solve for band part
            VVector bandSolution = solveBand(aRightHandSide.getVec(numCol, numBorder)); // = x
            aSolution.putVec(borderSolution, 0);
            aSolution.putVec(bandSolution.minus(auxMatT.times(borderSolution)), numBorder); // = x2
            // parts of inverse
            theBorder = inverseBorder; // E
            theMixed = inverseBorder.times(auxMat); // E*Xt (-mixed part of inverse) !!!
            theBand = inverseBand.plus(bandOfAVAT(auxMatT, inverseBorder)); // band(D^-1 + X*E*Xt)
        } else {
            aSolution.putVec(solveBand(aRightHandSide), 0);
            theBand = inverseBand;
        }
    }

    // / Print bordered band matrix.
    public void printMatrix() {
        System.out.println("Border part ");
        theBorder.print();
        System.out.println("Mixed  part ");
        theMixed.print();
        System.out.println("Band   part ");
        theBand.print();
    }

    /*
     * ============================================================================ from Dbandmatrix.F (MillePede-II by
     * V. Blobel, Univ. Hamburg) ============================================================================
     */
    // / (root free) Cholesky decomposition of band part: C=LDL^T
    /**
     * Decompose band matrix into diagonal matrix D and lower triangular band
     * matrix L (diagonal=1). Overwrite band
     * matrix with D and off-diagonal part of L. \exception 2 : matrix is
     * singular. \exception 3 : matrix is not
     * positive definite.
     */
    private void decomposeBand() {

        int nRow = numBand + 1;
        int nCol = numCol;
        VVector auxVec = new VVector(nCol);
        for (int i = 0; i < nCol; ++i)
            auxVec.set(i, theBand.get(0, i) * 16.0); // save diagonal elements
        for (int i = 0; i < nCol; ++i) {
            if ((theBand.get(0, i) + auxVec.get(i)) != theBand.get(0, i)) {
                theBand.set(0, i, 1.0 / theBand.get(0, i));
                if (theBand.get(0, i) < 0.) {
                    //mg added this theBand.set(0,i,0.0); on 5/14/2018
                    theBand.set(0, i, 0.0);
                    System.out.println("BorderedBandMatrix::decomposeBand not positive definite");
//                    throw new RuntimeException("BorderedBandMatrix decomposeBand not positive definite");
                }
            } else {
                theBand.set(0, i, 0.00001);
                System.out.println("BorderedBandMatrix::decomposeBand singular");
//                throw new RuntimeException("BorderedBandMatrix decomposeBand singular");
            }
            for (int j = 1; j < min(nRow, nCol - i); ++j) {
                double rxw = theBand.get(j, i) * theBand.get(0, i);
                for (int k = 0; k < min(nRow, nCol - i) - j; ++k)
                    theBand.subFrom(k, i + j, theBand.get(k + j, i) * rxw);
                theBand.set(j, i, rxw);
            }
        }
    }

    // / Invert band part.
    /**
     * \return Inverted band
     */
    private VMatrix invertBand() {

        int nRow = numBand + 1;
        int nCol = numCol;
        VMatrix inverseBand = new VMatrix(nRow, nCol);

        for (int i = nCol - 1; i >= 0; i--) {
            double rxw = theBand.get(0, i);
            for (int j = i; j >= max(0, i - nRow + 1); j--) {
                for (int k = j + 1; k < min(nCol, j + nRow); ++k)
                    rxw -= inverseBand.get(abs(i - k), min(i, k)) * theBand.get(k - j, j);
                inverseBand.set(i - j, j, rxw);
                rxw = 0.;
            }
        }
        return inverseBand;
    }

    // / Solve for band part.
    /**
     * Solve C*x=b for band part using decomposition C=LDL^T and forward (L*z=b)
     * and backward substitution
     * (L^T*x=D^-1*z). \param [in] aRightHandSide Right hand side (vector) 'b'
     * of C*x=b \return Solution (vector) 'x' of
     * C*x=b
     */
    private VVector solveBand(VVector aRightHandSide) {

        int nRow = theBand.getNumRows();
        int nCol = theBand.getNumCols();
        VVector aSolution = new VVector(aRightHandSide);
        for (int i = 0; i < nCol; ++i) // forward substitution
        
            for (int j = 1; j < min(nRow, nCol - i); ++j)
                aSolution.subFrom(j + i, theBand.get(j, i) * aSolution.get(i));
        for (int i = nCol - 1; i >= 0; i--) // backward substitution
        {
            double rxw = theBand.get(0, i) * aSolution.get(i);
            for (int j = 1; j < min(nRow, nCol - i); ++j)
                rxw -= theBand.get(j, i) * aSolution.get(j + i);
            aSolution.set(i, rxw);
        }
        return aSolution;
    }

    // / solve band part for mixed part (border rows).
    /**
     * Solve C*X=B for mixed part using decomposition C=LDL^T and forward and
     * backward substitution. \param [in]
     * aRightHandSide Right hand side (matrix) 'B' of C*X=B \return Solution
     * (matrix) 'X' of C*X=B
     */
    private VMatrix solveBand(VMatrix aRightHandSide) {

        int nRow = theBand.getNumRows();
        int nCol = theBand.getNumCols();
        VMatrix aSolution = new VMatrix(aRightHandSide);
        for (int iBorder = 0; iBorder < numBorder; iBorder++) {
            for (int i = 0; i < nCol; ++i) // forward substitution
            
                for (int j = 1; j < min(nRow, nCol - i); ++j)
                    aSolution.subFrom(iBorder, j + i, theBand.get(j, i) * aSolution.get(iBorder, i));
            for (int i = nCol - 1; i >= 0; i--) // backward substitution
            {
                double rxw = theBand.get(0, i) * aSolution.get(iBorder, i);
                for (int j = 1; j < min(nRow, nCol - i); ++j)
                    rxw -= theBand.get(j, i) * aSolution.get(iBorder, j + i);
                aSolution.set(iBorder, i, rxw);
            }
        }
        return aSolution;
    }

    // / Calculate band part of: 'anArray * aSymArray * anArray.T'.
    /**
     * \return Band part of product
     */
    private VMatrix bandOfAVAT(VMatrix anArray, VSymMatrix aSymArray) {
        int nBand = numBand;
        int nCol = numCol;
        int nBorder = numBorder;
        double sum;
        VMatrix aBand = new VMatrix((nBand + 1), nCol);
        for (int i = 0; i < nCol; ++i)
            for (int j = max(0, i - nBand); j <= i; ++j) {
                sum = 0.;
                for (int l = 0; l < nBorder; ++l) { // diagonal
                    sum += anArray.get(i, l) * aSymArray.get(l, l) * anArray.get(j, l);
                    for (int k = 0; k < l; ++k) // off diagonal
                        sum += anArray.get(i, l) * aSymArray.get(l, k) * anArray.get(j, k) + anArray.get(i, k)
                                * aSymArray.get(l, k) * anArray.get(j, l);
                }
                aBand.set(i - j, j, sum);
            }
        return aBand;
    }
}
