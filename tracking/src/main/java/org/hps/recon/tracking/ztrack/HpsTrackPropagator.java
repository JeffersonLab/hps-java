package org.hps.recon.tracking.ztrack;

import static java.lang.Math.abs;
import org.hps.recon.tracking.lit.DetectorTrackPropagator;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsTrackPropagator implements TrackPropagator {

    private RK4TrackExtrapolator fExtrapolator; // Track extrapolator tool

    public HpsTrackPropagator(RK4TrackExtrapolator extrap) {
        fExtrapolator = extrap;
    }

    public Status Propagate(ZTrackParam par, DetectorPlane p, double[] F) {
        //TODO figure out what to do with F
        //TODO why reset F?
        if (F != null) {
            for (int k = 0; k < 25; ++k) {
                F[k] = 0.;
            }
            F[0] = 1.;
            F[6] = 1.;
            F[12] = 1.;
            F[18] = 1.;
            F[24] = 1.;
        }
        double[] Fnew = null;
        if (F != null) {
            Fnew = new double[25];
        }

        // now extrapolate to this DetectorPlane
        // extrapolate the track parameters to an arbitrarilty oriented plane...
        // propagate to the z of the bounding box of the plane in steps to assure accuracy
        //TODO check that we aren't within bounding box zmin - zmax
        double zPlane = p.position().z();
        double zStart = par.GetZ();
        double zEnd = zStart < zPlane ? p.getZmin() : p.getZmax();
        double dz = zEnd - zStart;
        if (abs(dz) < DefaultSettings.MINIMUM_PROPAGATION_DISTANCE) {
            return Status.SUCCESS;
        }
        boolean downstream = dz > 0;
        int numSteps = (int) (abs(dz) / DetectorTrackPropagator.MAXIMUM_PROPAGATION_STEP_SIZE);
        double stepSize;
        if (numSteps == 0) {
            stepSize = abs(dz);
        } else {
            stepSize = DetectorTrackPropagator.MAXIMUM_PROPAGATION_STEP_SIZE;
        }
        double zCurrent = zStart;
        // Loop over steps + one additional to get to zOut
        ZTrackParam zTrackParamCurrent = new ZTrackParam(par);
        ZTrackParam zTrackParOut = new ZTrackParam();
        for (int iStep = 0; iStep < numSteps + 1; ++iStep) {
            if (zCurrent == zEnd) {
                break;
            }
            // Update current z position
            if (iStep != numSteps) {
                zCurrent = (downstream) ? zCurrent + stepSize : zCurrent - stepSize;
            } else {
                zCurrent = zEnd;
            }
            // extrapolate
            fExtrapolator.Extrapolate(zTrackParamCurrent, zTrackParOut, zCurrent, F);
            //update
            zTrackParamCurrent = new ZTrackParam(zTrackParOut);
        }
        // we are now at the bounding box of this plane...
        if (fExtrapolator.Extrapolate(zTrackParamCurrent, p, Fnew) == Status.ERROR) {
            return Status.ERROR;
        }
        
        // update track parameters
        par.copyFrom(zTrackParamCurrent);
        // Update transport matrix
        if (F != null) {
            UpdateF(F, Fnew);
        }

        return Status.SUCCESS;
    }

    @Override
    public Status Propagate(ZTrackParam parIn, ZTrackParam parOut, double zOut, double[] F) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Status Propagate(ZTrackParam parIn, ZTrackParam parOut, DetectorPlane p, double[] F) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Status Propagate(ZTrackParam par, double zOut, double[] F) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void UpdateF(
            double[] F,
            double[] newF) {
        double[] A = new double[25];
        Math.Mult25(newF, F, A);
        System.arraycopy(A, 0, F, 0, 25);
    }

}
