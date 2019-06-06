package org.hps.recon.tracking.ztrack;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CleverTrackExtrapolator implements TrackExtrapolator {

    TrackExtrapolator fLineExtrapolator; // line track extrapolation tool
    TrackExtrapolator fRK4Extrapolator;  // RK4 track extrapolation tool

    public CleverTrackExtrapolator(Field field) {
        fLineExtrapolator = new LineTrackExtrapolator(); // line track extrapolation tool
        fRK4Extrapolator = new RK4TrackExtrapolator(field); // RK4 track extrapolation tool

    }

    public Status Extrapolate(ZTrackParam parIn, ZTrackParam parOut, double zOut, double[] F) {
        parOut = parIn;
        return Extrapolate(parOut, zOut, F);
    }

    public Status Extrapolate(
            ZTrackParam parIn,
            ZTrackParam parOut,
            DetectorPlane p,
            double[] F) {
        throw new RuntimeException("not yet implemented!");
    }

    public Status Extrapolate(
            ZTrackParam par,
            DetectorPlane p,
            double[] F) {
        throw new RuntimeException("not yet implemented!");
    }

    public Status Extrapolate(ZTrackParam par, double zOut, double[] F) {
        double zIn = par.GetZ();
        double zStart = DefaultSettings.LINE_EXTRAPOLATION_START_Z;

        if (zIn >= zStart && zOut >= zStart) {
            return fLineExtrapolator.Extrapolate(par, zOut, F);
        } else if (zIn < zStart && zOut < zStart) {
            return fRK4Extrapolator.Extrapolate(par, zOut, F);
        } else if (zOut > zIn && zIn < zStart && zOut > zStart) {
            double[] F1 = new double[25];
            double[] F2 = new double[25];
            Status result;
            if (F != null) {
                result = fRK4Extrapolator.Extrapolate(par, zStart, F1);
            } else {
                result = fRK4Extrapolator.Extrapolate(par, zStart, null);
            }
            if (result == Status.ERROR) {
                return result;
            } else {
                Status result1;
                if (F != null) {
                    result1 = fLineExtrapolator.Extrapolate(par, zOut, F2);
                } else {
                    result1 = fLineExtrapolator.Extrapolate(par, zOut, null);
                }
                if (F != null && result1 == Status.SUCCESS) {
                    Math.Mult25(F2, F1, F);
                }
                return result1;
            }
        } else if (zOut < zIn && zIn > zStart && zOut < zStart) {
            double[] F1 = new double[25];
            double[] F2 = new double[25];
            Status result;
            if (F != null) {
                result = fLineExtrapolator.Extrapolate(par, zStart, F1);
            } else {
                result = fLineExtrapolator.Extrapolate(par, zStart, null);
            }
            if (result == Status.ERROR) {
                return result;
            } else {
                Status result1;
                if (F != null) {
                    result1 = fRK4Extrapolator.Extrapolate(par, zOut, F2);
                } else {
                    result1 = fRK4Extrapolator.Extrapolate(par, zOut, null);
                }
                if (F != null && result1 == Status.SUCCESS) {
                    Math.Mult25(F2, F1, F);
                }
                return result1;
            }
        }

        return Status.SUCCESS;
    }

}
