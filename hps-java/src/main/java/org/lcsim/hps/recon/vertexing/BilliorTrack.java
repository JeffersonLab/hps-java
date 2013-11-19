package org.lcsim.hps.recon.vertexing;
/*
 * BilliorTrack.java
 *
 *
 * $Id: BilliorTrack.java,v 1.1 2011/06/01 17:10:13 jeremy Exp $
 */


import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.matrix.MutableMatrix;
import hep.physics.matrix.SymmetricMatrix;
import java.util.Map;
import org.lcsim.constants.Constants;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.MultipleScatter;

/**
 * Converts from HelicalTrackFit formalism to formalism in Billior paper
 * See (e.g.) Billior, Qian NIM A311, 1992
 * @author Matt Graham
 * @version 1.0
 */
public class BilliorTrack {

    /**
     * Index of DCA element in parameter array and covariance matrix.
     */
    public static int epsIndex = 0;
    /**
     * Index of the z0 coordinate in the parameter array and covariance matrix.
     */
    public static int z0Index = 1;
    /**
     * Index of the angle wrt the Z-axis
     */
    public static int thetaIndex = 2;
    /**
     * Index of phi0 element in parameter array and covariance matrix.
     */
    public static int phi0Index = 3;
    /**
     * Index of curvature element in the parameter array and covariance matrix.
     */
    public static int curvatureIndex = 4;
    // fit independently to a circle in x-y and a line in s-z
    // first is circle, second is line
    private double[] _chisq = new double[2];
    private double _nhchisq;
    private int[] _ndf = new int[2];
    private double[] _parameters;
    private Matrix _covmatrix;
    private Map<HelicalTrackHit, Double> _smap;
    private Map<HelicalTrackHit, MultipleScatter> _msmap;
    /**
     * Doubles used for error variables
     */
    //Omega error
    private double curveerror;
    //tanl(lambda) error
    private double thetaerror;
    //distance of closest approach error
    private double epserror;
    //azimuthal angle at DCA for momentum error
    private double phi0error;
    //z position when the particle is at the dca error
    private double z0error;

    public BilliorTrack() {
    }

    public BilliorTrack(HelicalTrackFit helix) {
        double[] helixparameters = helix.parameters();
        _parameters = convertParsToBillior(helixparameters);
        SymmetricMatrix helixcovmatrix = helix.covariance();
        _covmatrix = convertCovarianceToBillior(helixcovmatrix, helixparameters);
        _chisq = helix.chisq();
        _nhchisq = 0.;
        _ndf = helix.ndf();
        _smap = helix.PathMap();
        _msmap = helix.ScatterMap();
    }

    public double[] convertParsToBillior(double[] helixpars) {
        double[] billior = {0, 0, 0, 0, 0};
        billior[0] = -helixpars[0];
        billior[1] = helixpars[3];
        billior[2] = Math.PI / 2 - Math.atan(helixpars[4]);
        billior[3] = helixpars[1];
        billior[4] = helixpars[2];
        return billior;

    }

    public Matrix convertCovarianceToBillior(SymmetricMatrix helixcov, double[] helixpars) {
        BasicMatrix deriv = new BasicMatrix(5, 5);

        deriv.setElement(epsIndex, HelicalTrackFit.dcaIndex, -1);
        deriv.setElement(z0Index, HelicalTrackFit.z0Index, 1);
        deriv.setElement(thetaIndex, HelicalTrackFit.slopeIndex, -1 / (1 + helixpars[HelicalTrackFit.slopeIndex] * helixpars[HelicalTrackFit.slopeIndex]));
        deriv.setElement(phi0Index, HelicalTrackFit.phi0Index, 1);
        deriv.setElement(curvatureIndex, HelicalTrackFit.curvatureIndex, 1);

        Matrix derivT = MatrixTranspose(deriv);
        Matrix newcov = MatrixOp.mult(deriv, MatrixOp.mult(helixcov, derivT));

        return newcov;
    }

    /**
     * Return the helix parameters as an array.
     * @return helix parameters
     */
    public double[] parameters() {
        return _parameters;
    }

    /**
     * Return the signed helix DCA.
     * @return DCA
     */
    public double eps() {
        return _parameters[epsIndex];
    }

    /**
     * Return the azimuthal direction at the DCA
     * @return azimuthal direction
     */
    public double phi0() {
        return _parameters[phi0Index];
    }

    /**
     * Return the signed helix curvature.
     * @return helix curvature
     */
    public double curvature() {
        return _parameters[curvatureIndex];
    }

    /**
     * Return the z coordinate for the DCA.
     * @return z coordinate
     */
    public double z0() {
        return _parameters[z0Index];
    }

    /**
     * Return the helix slope tan(lambda).
     * @return slope
     */
    public double theta() {
        return _parameters[thetaIndex];
    }

    /**
     * Return the helix covariance matrix.
     * @return covariance matrix
     */
    public Matrix covariance() {
        return _covmatrix;
    }

    /**
     * Return the helix fit chisqs.  chisq[0] is for the circle fit, chisq[1] is
     * for the s-z fit.
     * @return chisq array
     */
    public double[] chisq() {
        return _chisq;
    }

    /**
     * Set the chisq for non-holonomic constraints (e.g., pT > xx).
     * @param nhchisq non-holonomic constraint chisq
     */
    public void setnhchisq(double nhchisq) {
        _nhchisq = nhchisq;
        return;
    }

    /**
     * Return the non-holenomic constraint chisq.
     * @return non-holenomic constraint chisq
     */
    public double nhchisq() {
        return _nhchisq;
    }

    /**
     * Return the total chisq: chisq[0] + chisq[1] + nhchisq.
     * @return total chisq
     */
    public double chisqtot() {
        return _chisq[0] + _chisq[1] + _nhchisq;
    }

    /**
     * Return the degrees of freedom for the fits.  ndf[0] is for the circle fit
     * and ndf[1] is for the s-z fit.
     * @return dof array
     */
    public int[] ndf() {
        return _ndf;
    }

    /**
     * Return cos(theta).
     * @return cos(theta)
     */
    public double cth() {
        return Math.cos(theta());
    }

    /**
     * Return sin(theta).
     * @return sin(theta)
     */
    public double sth() {
        return Math.sin(theta());
    }

    /**
     * Return transverse momentum pT for the helix.
     * @param bfield magnetic field
     * @return pT
     */
    public double pT(double bfield) {
        return Constants.fieldConversion * bfield * Math.abs(R());
    }

    /**
     * Return the momentum.
     * @param bfield magnetic field
     * @return momentum
     */
    public double p(double bfield) {
        return pT(bfield) / sth();
    }

    /**
     * Return the radius of curvature for the helix.
     * @return radius of curvature
     */
    public double R() {
        return 1. / curvature();
    }

    /**
     * Return the x coordinate of the helix center/axis.
     * @return x coordinate of the helix axis
     */
    public double xc() {
        return (R() + eps()) * Math.sin(phi0());
    }

    /**
     * Return the y coordinate of the helix center/axis.
     * @return y coordinate of the helix axis
     */
    public double yc() {
        return -(R() + eps()) * Math.cos(phi0());
    }

    public double x0() {
        return eps() * Math.sin(phi0());
    }

    public double y0() {
        return -eps() * Math.cos(phi0());
    }

    /**
     * Return a map of x-y path lengths for the hits used in the helix fit.
     * @return path length map
     */
    public Map<HelicalTrackHit, Double> PathMap() {
        return _smap;
    }

    /**
     * Return a map of the MultipleScatter objects supplied for the fit.
     * @return map of multiple scattering uncertainties
     */
    public Map<HelicalTrackHit, MultipleScatter> ScatterMap() {
        return _msmap;
    }

    /**
     * Return the error for curvature, omega
     * @return a double curveerror
     */
    public double getCurveError() {
        curveerror = Math.sqrt(_covmatrix.e(BilliorTrack.curvatureIndex, BilliorTrack.curvatureIndex));
        return curveerror;
    }

    /**
     * Return the error for slope dz/ds, tan(lambda)
     * @return double a slopeerror
     */
    public double getThetaError() {
        thetaerror = Math.sqrt(_covmatrix.e(BilliorTrack.thetaIndex, BilliorTrack.thetaIndex));
        return thetaerror;
    }

    /**
     * Return the error for distance of closest approach, dca
     * @return a double dcaerror
     */
    public double getEpsError() {
        epserror = Math.sqrt(_covmatrix.e(BilliorTrack.epsIndex, BilliorTrack.epsIndex));
        return epserror;
    }

    /**
     * Return the error for phi0, azimuthal angle of the momentum at the DCA ref. point
     * @return a double phi0error
     */
    public double getPhi0Error() {
        phi0error = Math.sqrt(_covmatrix.e(BilliorTrack.phi0Index, BilliorTrack.phi0Index));
        return phi0error;
    }

    /**
     * Return the error for z0, the z position of the particle at the DCA
     * @return a double z0error
     */
    public double getZ0Error() {
        z0error = Math.sqrt(_covmatrix.e(BilliorTrack.z0Index, BilliorTrack.z0Index));
        return z0error;
    }

        /**
     * Returns the transpose of the matrix (inexplicably not handled by
     * the matrix package for non-square matrices).
     *
     * @param m matrix to be transposed
     * @return transposed matrix
     */
    private Matrix MatrixTranspose(Matrix m) {
        MutableMatrix mt = new BasicMatrix(m.getNColumns(), m.getNRows());
        for (int i = 0; i < m.getNRows(); i++) {
            for (int j = 0; j < m.getNColumns(); j++) {
                mt.setElement(j, i, m.e(i, j));
            }
        }
        return mt;
    }

    /**
     * Create a string with the helix parameters.
     * @return string containing the helix parameters
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("HelicalTrackFit: \n");
        sb.append("eps= " + eps() + "\n");
        sb.append("phi0= " + phi0() + "\n");
        sb.append("curvature: " + curvature() + "\n");
        sb.append("z0= " + z0() + "\n");
        sb.append("theta= " + theta() + "\n");
        return sb.toString();
    }
}