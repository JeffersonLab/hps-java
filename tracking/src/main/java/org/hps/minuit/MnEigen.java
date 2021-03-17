package org.hps.minuit;

/**
 * Calculates and the eigenvalues of the user covariance matrix
 * MnUserCovariance.
 *
 */
public class MnEigen {

    /* Calculate eigenvalues of the covariance matrix.
     * Will perform the calculation of the eigenvalues of the covariance matrix
     * and return the result in the form of a double array.
     * The eigenvalues are ordered from the smallest to the largest eigenvalue.
     */
    public static double[] eigenvalues(MnUserCovariance covar) {
        MnAlgebraicSymMatrix cov = new MnAlgebraicSymMatrix(covar.nrow());
        for (int i = 0; i < covar.nrow(); i++) {
            for (int j = i; j < covar.nrow(); j++) {
                cov.set(i, j, covar.get(i, j));
            }
        }

        MnAlgebraicVector eigen = cov.eigenvalues();
        return eigen.data();
    }
}
