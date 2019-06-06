package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsTrackPropagator {

    private CbmLitRK4TrackExtrapolator fExtrapolator; // Track extrapolator tool

    public HpsTrackPropagator(CbmLitRK4TrackExtrapolator extrap) {
        fExtrapolator = extrap;
    }

    public LitStatus Propagate(CbmLitTrackParam par, DetectorPlane p, int pdg, double[] F, double[] length) {
//        double zIn = par.GetZ();
//        double dz = zOut - zIn;
//        if (abs(dz) < 1E-6) {
//            return LitStatus.kLITSUCCESS;
//        }
//        boolean downstream = dz > 0;
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

        // now simply extrapolate to this DetectorPlane
        double[] Fnew = null;
        if (F != null) {
            Fnew = new double[25];
        }
        // Extrapolate to the next boundary
        if (fExtrapolator.Extrapolate(par, p, Fnew) == LitStatus.kLITERROR) {
            return LitStatus.kLITERROR;
        }

        // Update transport matrix
        if (F != null) {
            UpdateF(F, Fnew);
        }

        return LitStatus.kLITSUCCESS;
    }

    void UpdateF(
            double[] F,
            double[] newF) {
        double[] A = new double[25];
        CbmLitMath.Mult25(newF, F, A);
        System.arraycopy(A, 0, F, 0, 25);
    }

}
