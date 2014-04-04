package org.hps.recon.tracking;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.matrix.MutableMatrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.util.swim.Helix;

/**
 * Convert a helix to a straight line track at a specified reference plane normal to the x axis.
 * This code was developed for simulating the Heavy Photon Search experiment where the target is
 * located outside the magnetic field volume.
 * 
 * @author Richard Partridge
 */
// FIXME: If this is copy-pasted from lcsim, what behavior was changed here?  Is it needed? --JM
public class HelixConverter {

    private double _xref;

    /**
     * Constructor for the HelixConverter used to convert helices to StraightLineTracks at the
     * magnetic field boundary. The reference point xref identifies the x coordinate that specifies
     * the magnetic field boundary. The StraightLineTracks produced by this class will use xref as
     * their reference point.
     * 
     * @param xref x coordinate that specifies the magnetic field boundary
     */
    public HelixConverter(double xref) {
        _xref = xref;
    }

    /**
     * Convert a helix to a StraightLineTrack.
     * 
     * @param helix helix to be converted
     * @return resulting StraightLineTrack
     */
    public StraightLineTrack Convert(HelicalTrackFit helix) {

        // Get helix parameters used in this calculation
        double RC = helix.R();
        double xc = helix.xc();
        double yc = helix.yc();
        double phi0 = helix.phi0();
        double d0 = helix.dca();
        double slope = helix.slope();
        double z0 = helix.z0();

        // First find path length to reference point
        double arg = (xc - _xref) / RC;
        if (Math.abs(arg) > 1.0)
            return null;
        double phi = Math.asin(arg);
        double dphi = phi0 - phi;
        if (dphi > Math.PI)
            dphi -= 2. * Math.PI;
        if (dphi < -Math.PI)
            dphi += 2. * Math.PI;
        double s = RC * dphi;
        double cphi = Math.cos(phi);
        double sphi = Math.sin(phi);
        double cphi0 = Math.cos(phi0);
        double sphi0 = Math.sin(phi0);

        // Get the track position at the reference point
        double xref = xc - RC * sphi;
        if (Math.abs(xref - _xref) > 1.e-10)
            System.out.println("Bad path length - x0: " + xref + " xref: " + _xref);
        double yref = yc + RC * cphi;
        double zref = z0 + s * slope;

        // Get dy/dx and dz/dx for the straight-line track
        double dydx = sphi / cphi;
        double dzdx = slope / cphi;

        // Calculate the Jacobian between the straight line track parameters and the helix
        // parameters
        MutableMatrix deriv = new BasicMatrix(4, 5);
        double dydcurv = (cphi0 + s / RC * sphi - cphi) * RC * RC;
        deriv.setElement(StraightLineTrack.y0Index, HelicalTrackFit.curvatureIndex, dydcurv);
        deriv.setElement(StraightLineTrack.y0Index, HelicalTrackFit.dcaIndex, cphi0);
        double dydphi0 = (RC - d0) * sphi0 - RC * sphi;
        deriv.setElement(StraightLineTrack.y0Index, HelicalTrackFit.phi0Index, dydphi0);
        deriv.setElement(StraightLineTrack.z0Index, HelicalTrackFit.z0Index, 1.);
        deriv.setElement(StraightLineTrack.z0Index, HelicalTrackFit.slopeIndex, s);
        deriv.setElement(StraightLineTrack.dydxIndex, HelicalTrackFit.curvatureIndex, -s / (cphi * cphi));
        deriv.setElement(StraightLineTrack.dydxIndex, HelicalTrackFit.phi0Index, 1. / (cphi * cphi));
        double dzslopedphi0 = sphi * slope / (cphi * cphi);
        deriv.setElement(StraightLineTrack.dzdxIndex, HelicalTrackFit.curvatureIndex, -s * dzslopedphi0);
        deriv.setElement(StraightLineTrack.dzdxIndex, HelicalTrackFit.phi0Index, dzslopedphi0);
        deriv.setElement(StraightLineTrack.dzdxIndex, HelicalTrackFit.slopeIndex, 1. / cphi);

        // Calculate the covariance matrix
        Matrix derivT = MatrixTranspose(deriv);
        SymmetricMatrix hcov = helix.covariance();
        Matrix cov = MatrixOp.mult(deriv, MatrixOp.mult(hcov, derivT));
        SymmetricMatrix scov = new SymmetricMatrix(cov);

        return new StraightLineTrack(xref, yref, zref, dydx, dzdx, scov);
    }

    /**
     * Convert a helix to a StraightLineTrack.
     * 
     * @param helix helix to be converted
     * @return resulting StraightLineTrack
     */
    public StraightLineTrack Convert(Helix helix) {

        Hep3Vector unitVec = helix.getUnitTangentAtLength(0);
        Hep3Vector posVec = helix.getPointAtDistance(0);
        double dzdx = unitVec.z() / unitVec.x();
        double dydx = unitVec.y() / unitVec.x();
        double zref = posVec.z() - dzdx * (posVec.x() - _xref);
        double yref = posVec.y() - dydx * (posVec.x() - _xref);
        SymmetricMatrix scov = null;
        StraightLineTrack slt = new StraightLineTrack(_xref, yref, zref, dydx, dzdx, scov);
        // System.out.printf("%s: unitVec %s posVec %s\n",this.getClass().getSimpleName(),unitVec.toString(),posVec.toString());
        // System.out.printf("%s: dzdx=%f dydx=%s\n",this.getClass().getSimpleName(),dzdx,dydx);
        // System.out.printf("%s: ref = %f,%f,%f\n",this.getClass().getSimpleName(),_xref,yref,zref);
        return slt;
    }

    /**
     * Returns the transpose of the matrix (inexplicably not handled by the matrix package for
     * non-square matrices).
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
}
