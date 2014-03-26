package org.hps.recon.vertexing;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.matrix.MutableMatrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.StraightLineTrack;

/**
 *  Fit a set of straight line tracks to a common vertex
 *
 * @author Richard Partridge
 */
public class VertexFitter {

    private double _xref;
    private double _tol = 1.0e-9;
    private int _maxIterations = 100;
    private VertexFit _vfit;

    /**
     * Instantiate the vertex fitting class for the Heavy Photon Search simulation.
     * The tracks are assumed to travel in the +x direction in a field-free region
     * for x < xref.  For x > xref, the particles are assumed to travel in a helical
     * trajectory.  HelixConverter should be called before VertexFitter to transform
     * the helices to StraightLineTracks at the x = xref boundary.
     *
     * @param xref x coordinate of the magnetic field boundary
     */
    public VertexFitter(double xref) {

        _xref = xref;
    }

    /**
     * Perform a vertex fit of the specified StraightLineTracks.  At least two
     * tracks must be provided to locate the vertex position.  The fitter performs
     * a chi^2 minimization using the correlated track errors using the method of
     * Lagrange Multipliers to impose the constraint that the track passes through
     * the vertex position.  The vertex position is assumed to be unmeasured.
     *
     * The algorithm follows the fitting formalism used by SQUAW, with the
     * vertex position treated as a "poorly measured variable" with infinite error.
     * Matrix names largely follow the SQUAW writeup where defined.
     * 
     * A writeup of the SQUAW fitting procedure can be found in the cvs directory
     * containing this package.
     *
     * @param sltlist list of StraightLineTracks to be fit
     * @return fit success flag - true if the fit converged
     */
    public boolean VertexFit(List<StraightLineTrack> sltlist) {

        //  Dump any previous vertex fit
        _vfit = null;

        //  Find the number of tracks and make sure we have at least 2
        int ntrks = sltlist.size();
        if (ntrks < 2) return false;

        //  Extract the measured values of the measured variables
        Matrix mm = FillMeasured(sltlist);

        //  Find the starting point for the unmeasured vertex position
        Matrix mstar = ApproximateIntersection(sltlist.get(0), sltlist.get(1));

        //  Extract the error matrix and it's inverse for the measured variables
        Matrix GInv = FillCovariance(sltlist);
        Matrix GG = MatrixOp.inverse(GInv);

        //  The inverse error matrix for the unmeasured variables is all 0's
        Matrix GStar = new BasicMatrix(3, 3);

        //  Initialize the differences between the fitted and measured quantities to 0
        Matrix cc = new BasicMatrix(mm.getNRows(), 1);
        Matrix cstar = new BasicMatrix(mstar.getNRows(), 1);

        for (int iter = 0; iter < _maxIterations; iter++) {

            //  Update the fitted and unmeasured variables
            Matrix xx = MatrixOp.add(mm, cc);
            Matrix xstar = MatrixOp.add(mstar, cstar);

            //  Calculate the errors in the constraint equations
            Matrix FF = CalculateFF(ntrks, xx, xstar);

            //  Calculate the derivative matrices BB and BStar and their transposes
            Matrix BB = CalculateBB(ntrks, xstar);
            Matrix BStar = CalculateBStar(ntrks, xx);

            //  Calculate the matrix AA that provides the coefficients for the
            //  system of linear equations to be solved: AA * XX = YY
            Matrix AA = CalculateAA(ntrks, BB, BStar, GInv, GStar);

            //  Calculate the constant terms in the system of linear equations
            MutableMatrix YY = new BasicMatrix(AA.getNRows(), 1);
            Matrix RR = MatrixOp.sub(MatrixOp.add(MatrixOp.mult(MatrixOp.transposed(BStar), cstar), MatrixOp.mult(MatrixOp.transposed(BB), cc)), FF);
            MatrixOp.setSubMatrix(YY, RR, 0, 0);

            //  Solve the system of linear equations
            Matrix AAInv = MatrixOp.inverse(AA);
            Matrix XX = MatrixOp.mult(AAInv, YY);

            //  Get the Lagrange multipliers
            Matrix alpha = MatrixOp.getSubMatrix(XX, 0, 0, FF.getNRows(), 1);

            if (iter > 0 && Converged(FF)) {

                //  Fit converged - extract the fit quantities
                //  Calculate the fit chi square
                double chisq = MatrixOp.mult(MatrixOp.transposed(cc), MatrixOp.mult(GG, cc)).e(0, 0);

                //  Extract the covariance matrix for the vertex position from the matrix AAInv
                Matrix vtxcov = MatrixOp.getSubMatrix(AAInv, FF.getNRows(), FF.getNRows(), GStar.getNRows(), GStar.getNColumns());
                
                //  Find the fitted track directions and save them in a map
                Map<StraightLineTrack, Hep3Vector> dirmap = new HashMap<StraightLineTrack, Hep3Vector>();
                for (int i=0; i<ntrks; i++) {
                   Hep3Vector u = VecOp.unit(new BasicHep3Vector(1, xx.e(4*i + 1, 0), xx.e(4*i + 3, 0)));
                   dirmap.put(sltlist.get(i), u);
                }

                //  Create a new VertexFit and return with a success flag
                _vfit = new VertexFit(MatrixOp.as3Vector(xstar), new SymmetricMatrix(vtxcov), chisq, dirmap);
                return true;
            }

            //  Update the change in the fitted and unmeasured variables
            cc = MatrixOp.mult(-1., MatrixOp.mult(GInv, MatrixOp.mult(BB, alpha)));
            cstar = MatrixOp.getSubMatrix(XX, FF.getNRows(), 0, GStar.getNRows(), 1);
        }

        return false;
    }

    /**
     * Retrieve the VertexFit class containing the results of the fit
     *
     * @return fit results
     */
    public VertexFit getFit() {
        return _vfit;
    }

    /**
     * Set the convergence criteria for the fit (default is 1E-9).  Units are mm.
     *
     * @param tol convergence criteria
     */
    public void setTolerance(double tol) {
        _tol = tol;
    }

    /**
     * Set the maximum number of iterations to try before the fit fails for
     * lack of convergence (default is 100).
     *
     * @param maxIterations maximum number of fit iterations
     */
    public void setMaxIterations(int maxIterations) {
        _maxIterations = maxIterations;
    }

    /**
     * Extract the measured variables from the list of straight line tracks.
     * Measurement variables are saved as four consecutive values (y0, y', z0, z')
     * for each track, with the measurement variables for track 0 coming first.
     *
     * @param sltlist list of StraightLineTracks
     * @return column matrix containing the measured variables
     */
    private Matrix FillMeasured(List<StraightLineTrack> sltlist) {

        int ntrks = sltlist.size();
        MutableMatrix mm = new BasicMatrix(4 * ntrks, 1);
        for (int i = 0; i < ntrks; i++) {
            StraightLineTrack slt = sltlist.get(i);
            mm.setElement(4 * i, 0, slt.y0());
            mm.setElement(4 * i + 1, 0, slt.dydx());
            mm.setElement(4 * i + 2, 0, slt.z0());
            mm.setElement(4 * i + 3, 0, slt.dzdx());
        }
        return mm;
    }

    /**
     * Extract the covariance matrix.  It is assumed that there are no
     * correlations between tracks.  Ordering of elements in the covariance
     * matrix follows the ordering of the measured variables.
     *
     * @param sltlist list of StraightLineTracks
     * @return covariance matrix
     */
    private Matrix FillCovariance(List<StraightLineTrack> sltlist) {

        int ntrks = sltlist.size();
        MutableMatrix GInv = new BasicMatrix(4 * ntrks, 4 * ntrks);
        for (int i = 0; i < ntrks; i++) {
            SymmetricMatrix cov = sltlist.get(i).cov();
            GInv.setElement(4*i + 0, 4*i + 0, cov.diagonal(StraightLineTrack.y0Index));
            GInv.setElement(4*i + 1, 4*i + 1, cov.diagonal(StraightLineTrack.dydxIndex));
            GInv.setElement(4*i + 2, 4*i + 2, cov.diagonal(StraightLineTrack.z0Index));
            GInv.setElement(4*i + 3, 4*i + 3, cov.diagonal(StraightLineTrack.dzdxIndex));
            double cov01 = cov.e(StraightLineTrack.y0Index, StraightLineTrack.dydxIndex);
            GInv.setElement(4*i + 0, 4*i + 1, cov01);
            GInv.setElement(4*i + 1, 4*i + 0, cov01);
            double cov02 = cov.e(StraightLineTrack.y0Index, StraightLineTrack.z0Index);
            GInv.setElement(4*i + 0, 4*i + 2, cov02);
            GInv.setElement(4*i + 2, 4*i + 0, cov02);
            double cov03 = cov.e(StraightLineTrack.y0Index, StraightLineTrack.dzdxIndex);
            GInv.setElement(4*i + 0, 4*i + 3, cov03);
            GInv.setElement(4*i + 3, 4*i + 0, cov03);
            double cov12 = cov.e(StraightLineTrack.dydxIndex, StraightLineTrack.z0Index);
            GInv.setElement(4*i + 1, 4*i + 2, cov12);
            GInv.setElement(4*i + 2, 4*i + 1, cov12);
            double cov13 = cov.e(StraightLineTrack.dydxIndex, StraightLineTrack.dzdxIndex);
            GInv.setElement(4*i + 1, 4*i + 3, cov13);
            GInv.setElement(4*i + 3, 4*i + 1, cov13);
            double cov23 = cov.e(StraightLineTrack.z0Index, StraightLineTrack.dzdxIndex);
            GInv.setElement(4*i + 2, 4*i + 3, cov23);
            GInv.setElement(4*i + 3, 4*i + 2, cov23);
        }
        return GInv;
    }

    /**
     * Find a first approximation to the unmeasured variables by finding the
     * track intersections in the x-y and x-z planes.  The intersection points
     * for the two planes are averaged.
     *
     * @param slt1 first StraightLineTrack to use
     * @param slt2 second StraightLineTrack to use
     * @return column matrix containing the first approximation of the vertex position
     */
    private Matrix ApproximateIntersection(StraightLineTrack slt1, StraightLineTrack slt2) {

        MutableMatrix xstar = new BasicMatrix(3, 1);
        double x1 = (slt1.y0() - slt2.y0()) / (slt2.dydx() - slt1.dydx()) + _xref;
        double x2 = (slt1.z0() - slt2.z0()) / (slt2.dzdx() - slt1.dzdx()) + _xref;
        double xv = 0.5 * (x1 + x2);
        double yv = 0.5 * (slt1.y0() + slt2.y0() + (xv - _xref) * (slt1.dydx() + slt2.dydx()));
        double zv = 0.5 * (slt1.z0() + slt2.z0() + (xv - _xref) * (slt1.dzdx() + slt2.dzdx()));
        xstar.setElement(0, 0, xv);
        xstar.setElement(1, 0, yv);
        xstar.setElement(2, 0, zv);
        return xstar;
    }

    /**
     * Calculate derivative of constraints with respect to the measured variables.
     * Each track has two constraint equations:
     *   f_y(i) = y0(i) + y'(i) * (xv - xref) - yv
     *   f_z(i) = z0(i) + z'(i) * (xv - xref) - zv
     * Save the result in the form BB(i,j) = df(j)/dx(i)
     *
     * @param ntrks number of tracks
     * @param xstar current estimate of the vertex position
     * @return constraint derivatives with respect to the measured variables
     */
    private Matrix CalculateBB(int ntrks, Matrix xstar) {

        MutableMatrix BB = new BasicMatrix(4 * ntrks, 2 * ntrks);
        double xv = xstar.e(0, 0);
        for (int i = 0; i < ntrks; i++) {
            BB.setElement(4 * i, 2 * i, 1.);                    // df_y/dy0
            BB.setElement(4 * i + 1, 2 * i, xv - _xref);          // df_y/dy'
            BB.setElement(4 * i + 2, 2 * i + 1, 1.);                // df_z/dz0
            BB.setElement(4 * i + 3, 2 * i + 1, xv - _xref);        // df_z/dz'
        }
        return BB;
    }

    /**
     * Calculate derivative of constraints with respect to the unmeasured variables.
     * Each track has two constraint equations:
     *   f_y(i) = y0(i) + y'(i) * (xv - xref) - yv
     *   f_z(i) = z0(i) + z'(i) * (xv - xref) - zv
     * Save the result in the form BStar(i,j) = df(j)/dxv(i)
     *
     * @param ntrks number of tracks
     * @param xx current estimate of the measured variables
     * @return constraint derivatives with respect to the unmeasured variables
     */
    private Matrix CalculateBStar(int ntrks, Matrix xx) {

        MutableMatrix BStar = new BasicMatrix(3, 2 * ntrks);
        for (int i = 0; i < ntrks; i++) {
            double dydx = xx.e(4 * i + 1, 0);
            double dzdx = xx.e(4 * i + 3, 0);
            BStar.setElement(0, 2 * i, dydx);
            BStar.setElement(1, 2 * i, -1.);
            BStar.setElement(0, 2 * i + 1, dzdx);
            BStar.setElement(2, 2 * i + 1, -1.);
        }
        return BStar;
    }

    /**
     * Calculate the matrix AA that provides the coefficients for the
     * system of linear equations to be solved
     *
     * @param ntrks number of tracks
     * @param BB constraint derivatives for the measured variables
     * @param BStar constraint derivatives for the unmeasured variables
     * @param GInv inverse of the covariance matrix for the measured variables
     * @param GStar inverse of the covariance matrix for the unmeasured variables
     * @return matrix containing coefficients for the system of linear equations
     */
    private Matrix CalculateAA(int ntrks, Matrix BB, Matrix BStar, Matrix GInv, Matrix GStar) {

        Matrix HH = MatrixOp.mult(MatrixOp.transposed(BB), MatrixOp.mult(GInv, BB));
        Matrix BStarT = MatrixOp.transposed(BStar);
        MutableMatrix AA = new BasicMatrix(HH.getNRows() + BStar.getNRows(), HH.getNColumns() + BStarT.getNColumns());
        MatrixOp.setSubMatrix(AA, MatrixOp.mult(-1., HH), 0, 0);
        MatrixOp.setSubMatrix(AA, BStarT, 0, HH.getNColumns());
        MatrixOp.setSubMatrix(AA, BStar, HH.getNRows(), 0);
        MatrixOp.setSubMatrix(AA, GStar, HH.getNRows(), HH.getNColumns());
        return AA;
    }

    /**
     * Evaluate the residual values for the constraint equations (the fitter
     * will try to force this vector to 0).
     * Each track has two constraint equations:
     *   f_y(i) = y0(i) + y'(i) * (xv - xref) - yv
     *   f_z(i) = z0(i) + z'(i) * (xv - xref) - zv
     * Save the result in the form FF(2*i) = f_y(i), FF(2*i+) = f_z(i)
     *
     * @param ntrks number of tracks
     * @param xx measured variables
     * @param xstar unmeasured variables (vertex position)
     * @return column matrix containing the residuals of the constaint equations
     */
    private Matrix CalculateFF(int ntrks, Matrix xx, Matrix xstar) {

        MutableMatrix FF = new BasicMatrix(2 * ntrks, 1);
        double xv = xstar.e(0, 0);
        double yv = xstar.e(1, 0);
        double zv = xstar.e(2, 0);
        for (int i = 0; i < ntrks; i++) {
            double y0 = xx.e(4 * i, 0);
            double dydx = xx.e(4 * i + 1, 0);
            double z0 = xx.e(4 * i + 2, 0);
            double dzdx = xx.e(4 * i + 3, 0);
            double fy = y0 + dydx * (xv - _xref) - yv;
            double fz = z0 + dzdx * (xv - _xref) - zv;
            FF.setElement(2 * i, 0, fy);
            FF.setElement(2 * i + 1, 0, fz);
        }
        return FF;
    }

    /**
     * Check to see if all the constraints are satisfied within the tolerance
     * that has been set.
     *
     * @param FF constraint residuals
     * @return true if the fit has converged
     */
    private boolean Converged(Matrix FF) {

        for (int i=0; i<FF.getNRows(); i++) {
            if (Math.abs(FF.e(i, 0)) > _tol) return false;
        }
        return true;
    }
}