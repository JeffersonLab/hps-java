package org.hps.recon.tracking.lit;

import static java.lang.Math.abs;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class SimpleTrackPropagator implements CbmLitTrackPropagator {

    private CbmLitTrackExtrapolator fExtrapolator; // Track extrapolator tool

    public SimpleTrackPropagator(CbmLitTrackExtrapolator extrap) {
        fExtrapolator = extrap;
    }

    public LitStatus Propagate(CbmLitTrackParam parIn, CbmLitTrackParam parOut, double zOut, int pdg, double[] F, double[] length) {
        parOut = parIn;
        return Propagate(parOut, zOut, pdg, F, length);
    }

    public LitStatus Propagate(CbmLitTrackParam par, CbmLitTrackParam parOut, DetectorPlane p, int pdg, double[] F, double[] length) {
        throw new RuntimeException("not yet implemented!");
    }

    public LitStatus Propagate(CbmLitTrackParam par, DetectorPlane p, int pdg, double[] F, double[] length) {
//                double zIn = par.GetZ();
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

        // now simply extrapolate to this z
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

    public LitStatus Propagate(CbmLitTrackParam par, double zOut, int pdg, double[] F, double[] length) {
        double zIn = par.GetZ();
        double dz = zOut - zIn;
        if (abs(dz) < 1E-6) {
            return LitStatus.kLITSUCCESS;
        }
        boolean downstream = dz > 0;
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

        // now simply extrapolate to this z
        double[] Fnew = null;
        if (F != null) {
            Fnew = new double[25];
        }
        // Extrapolate to the next boundary
        if (fExtrapolator.Extrapolate(par, zOut, Fnew) == LitStatus.kLITERROR) {
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
