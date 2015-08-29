package org.hps.recon.tracking.gbl;

import hep.physics.matrix.BasicMatrix;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.recon.tracking.seedtracker.ScatterAngle;

/**
 * A class providing various utilities related to GBL
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class GblUtils {

    private static GblUtils INSTANCE = null;

    private GblUtils() {
    }

    public static GblUtils getInstance() {
        if (INSTANCE == null) {
            return new GblUtils();
        } else {
            return INSTANCE;
        }
    }

    public BasicMatrix gblSimpleJacobianLambdaPhi(double ds, double cosl, double bfac) {
        /*
         Simple jacobian: quadratic in arc length difference.
         using lambda phi as directions
	    
         @param ds: arc length difference
         @type ds: float
         @param cosl: cos(lambda)
         @type cosl: float
         @param bfac: Bz*c
         @type bfac: float
         @return: jacobian to move by 'ds' on trajectory
         @rtype: matrix(float)
         ajac(1,1)= 1.0D0
         ajac(2,2)= 1.0D0
         ajac(3,1)=-DBLE(bfac*ds)
         ajac(3,3)= 1.0D0
         ajac(4,1)=-DBLE(0.5*bfac*ds*ds*cosl)
         ajac(4,3)= DBLE(ds*cosl)
         ajac(4,4)= 1.0D0
         ajac(5,2)= DBLE(ds)
         ajac(5,5)= 1.0D0
         '''
         jac = np.eye(5)
         jac[2, 0] = -bfac * ds
         jac[3, 0] = -0.5 * bfac * ds * ds * cosl
         jac[3, 2] = ds * cosl
         jac[4, 1] = ds  
         return jac
         */
        BasicMatrix mat = unitMatrix(5, 5);
        mat.setElement(2, 0, -bfac * ds);
        mat.setElement(3, 0, -0.5 * bfac * ds * ds * cosl);
        mat.setElement(3, 2, ds * cosl);
        mat.setElement(4, 1, ds);
        return mat;
    }

    public BasicMatrix unitMatrix(int rows, int cols) {
        BasicMatrix mat = new BasicMatrix(rows, cols);
        for (int row = 0; row != mat.getNRows(); row++) {
            for (int col = 0; col != mat.getNColumns(); col++) {
                if (row != col) {
                    mat.setElement(row, col, 0);
                } else {
                    mat.setElement(row, col, 1);
                }
            }
        }
        return mat;
    }

    public BasicMatrix zeroMatrix(int rows, int cols) {
        BasicMatrix mat = new BasicMatrix(rows, cols);
        for (int row = 0; row != mat.getNRows(); row++) {
            for (int col = 0; col != mat.getNColumns(); col++) {
                mat.setElement(row, col, 0.);
            }
        }
        return mat;
    }

    public static double estimateScatter(HelicalTrackStripGbl strip, HelicalTrackFit htf, MultipleScattering scattering, double _B) {
        //can be edge case where helix is outside, but close to sensor, so make a new scatter point assuming the helix does pass through the sensor
        MaterialSupervisor.DetectorPlane hitPlane = null;
        if (MaterialSupervisor.class.isInstance(scattering.getMaterialManager())) {
            MaterialSupervisor matSup = (MaterialSupervisor) scattering.getMaterialManager();
            IDetectorElement hitElement = ((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement();
            for (MaterialSupervisor.ScatteringDetectorVolume vol : matSup.getMaterialVolumes()) {
                if (vol.getDetectorElement() == hitElement) {
                    hitPlane = (MaterialSupervisor.DetectorPlane) vol;
                    break;
                }
            }
            if (hitPlane == null) {
                throw new RuntimeException("cannot find plane for hit!");
            } else {
                // find scatterlength
                double s_closest = HelixUtils.PathToXPlane(htf, hitPlane.origin().x(), 0., 0).get(0);
                double X0 = hitPlane.getMaterialTraversedInRL(HelixUtils.Direction(htf, s_closest));
                ScatterAngle scatterAngle = new ScatterAngle(s_closest, scattering.msangle(htf.p(Math.abs(_B)), X0));
                return scatterAngle.Angle();
            }
        } else {
            throw new UnsupportedOperationException("Should not happen. This problem is only solved with the MaterialSupervisor.");
        }
    }
}
