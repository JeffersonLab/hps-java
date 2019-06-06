package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitCleverTrackExtrapolator implements CbmLitTrackExtrapolator {

    CbmLitTrackExtrapolator fLineExtrapolator; // line track extrapolation tool
    CbmLitTrackExtrapolator fRK4Extrapolator;  // RK4 track extrapolation tool

    public CbmLitCleverTrackExtrapolator(CbmLitField field) {
        fLineExtrapolator = new CbmLitLineTrackExtrapolator(); // line track extrapolation tool
        fRK4Extrapolator = new CbmLitRK4TrackExtrapolator(field); // RK4 track extrapolation tool

    }

    public LitStatus Extrapolate(CbmLitTrackParam parIn, CbmLitTrackParam parOut, double zOut, double[] F) {
        parOut = parIn;
        return Extrapolate(parOut, zOut, F);
    }

    public LitStatus Extrapolate(
            CbmLitTrackParam parIn,
            CbmLitTrackParam parOut,
            DetectorPlane p,
            double[] F) {
        throw new RuntimeException("not yet implemented!");
    }

    public LitStatus Extrapolate(
            CbmLitTrackParam par,
            DetectorPlane p,
            double[] F) {
        throw new RuntimeException("not yet implemented!");
    }

    public LitStatus Extrapolate(CbmLitTrackParam par, double zOut, double[] F) {
        double zIn = par.GetZ();
        double zStart = CbmLitDefaultSettings.LINE_EXTRAPOLATION_START_Z;

        if (zIn >= zStart && zOut >= zStart) {
            return fLineExtrapolator.Extrapolate(par, zOut, F);
        } else if (zIn < zStart && zOut < zStart) {
            return fRK4Extrapolator.Extrapolate(par, zOut, F);
        } else if (zOut > zIn && zIn < zStart && zOut > zStart) {
            double[] F1 = new double[25];
            double[] F2 = new double[25];
            LitStatus result;
            if (F != null) {
                result = fRK4Extrapolator.Extrapolate(par, zStart, F1);
            } else {
                result = fRK4Extrapolator.Extrapolate(par, zStart, null);
            }
            if (result == LitStatus.kLITERROR) {
                return result;
            } else {
                LitStatus result1;
                if (F != null) {
                    result1 = fLineExtrapolator.Extrapolate(par, zOut, F2);
                } else {
                    result1 = fLineExtrapolator.Extrapolate(par, zOut, null);
                }
                if (F != null && result1 == LitStatus.kLITSUCCESS) {
                    CbmLitMath.Mult25(F2, F1, F);
                }
                return result1;
            }
        } else if (zOut < zIn && zIn > zStart && zOut < zStart) {
            double[] F1 = new double[25];
            double[] F2 = new double[25];
            LitStatus result;
            if (F != null) {
                result = fLineExtrapolator.Extrapolate(par, zStart, F1);
            } else {
                result = fLineExtrapolator.Extrapolate(par, zStart, null);
            }
            if (result == LitStatus.kLITERROR) {
                return result;
            } else {
                LitStatus result1;
                if (F != null) {
                    result1 = fRK4Extrapolator.Extrapolate(par, zOut, F2);
                } else {
                    result1 = fRK4Extrapolator.Extrapolate(par, zOut, null);
                }
                if (F != null && result1 == LitStatus.kLITSUCCESS) {
                    CbmLitMath.Mult25(F2, F1, F);
                }
                return result1;
            }
        }

        return LitStatus.kLITSUCCESS;
    }

}
