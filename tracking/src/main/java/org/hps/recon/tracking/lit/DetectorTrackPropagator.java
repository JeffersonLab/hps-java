package org.hps.recon.tracking.lit;

import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class DetectorTrackPropagator implements CbmLitTrackPropagator {

    CbmLitTrackExtrapolator fExtrapolator; // Track extrapolator tool
    CbmLitGeoNavigator fNavigator; // Geometry navigator tool
    CbmLitMaterialEffects fMaterial; // Material effects tool

    public static double MAXIMUM_PROPAGATION_STEP_SIZE = 10.; // maximum step size in the track propagation

    public DetectorTrackPropagator(SimpleDetector det, CbmLitField field) {
        fExtrapolator = new CbmLitCleverTrackExtrapolator(field);
        fNavigator = new SimpleDetectorNavigator(det);
        fMaterial = new CbmLitMaterialEffectsImp();
    }

    public LitStatus Propagate(CbmLitTrackParam parIn, CbmLitTrackParam parOut, double zOut, int pdg, double[] F, double[] length) {
        parOut = parIn;
        return Propagate(parOut, zOut, pdg, F, length);
    }

    public LitStatus Propagate(CbmLitTrackParam parIn, CbmLitTrackParam parOut, DetectorPlane p, int pdg, double[] F, double[] length) {
        throw new RuntimeException("not yet implemented!");
    }

    public LitStatus Propagate(CbmLitTrackParam parIn, DetectorPlane p, int pdg, double[] F, double[] length) {
        throw new RuntimeException("not yet implemented!");
    }

    public LitStatus Propagate(CbmLitTrackParam par, double zOut, int pdg, double[] F, double[] length) {

        if (!IsParCorrect(par)) {
            return LitStatus.kLITERROR;
        }

        double zIn = par.GetZ();
        double dz = zOut - zIn;

        if (abs(dz) < CbmLitDefaultSettings.MINIMUM_PROPAGATION_DISTANCE) {
            return LitStatus.kLITSUCCESS;
        }

        // Check whether upstream or downstream
        // TODO check upstream/downstream
        boolean downstream = dz > 0;

        if (F != null) {
            Arrays.fill(F, 0.);
            F[0] = 1.;
            F[6] = 1.;
            F[12] = 1.;
            F[18] = 1.;
            F[24] = 1.;
        }

        int nofSteps = (int) (abs(dz) / MAXIMUM_PROPAGATION_STEP_SIZE);
        double stepSize;
        if (nofSteps == 0) {
            stepSize = abs(dz);
        } else {
            stepSize = MAXIMUM_PROPAGATION_STEP_SIZE;
        }
        double z = zIn;
//   cout << "zIn=" << zIn << " zOut=" << zOut << " dz=" << dz << " nofSteps=" << nofSteps
//         << " stepSize=" << stepSize << endl;

        if (length != null) {
            length[0] = 0;
        }

        // Loop over steps + additional step to propagate to virtual plane at zOut
        for (int iStep = 0; iStep < nofSteps + 1; iStep++) {
            if (!IsParCorrect(par)) {
                return LitStatus.kLITERROR;
            }
            // Check if already at exit position
            if (z == zOut) {
                break;
            }

            // Update current z position
            if (iStep != nofSteps) {
                z = (downstream) ? z + stepSize : z - stepSize;
            } else {
                z = zOut;
            }

            // Get intersections with materials for this step
            List<CbmLitMaterialInfo> inter = new ArrayList<CbmLitMaterialInfo>();
            if (fNavigator.FindIntersections(par, z, inter) == LitStatus.kLITERROR) {
                return LitStatus.kLITERROR;
            }

//      cout << "iStep=" << iStep << " z=" << z << " inter.size()=" << inter.size() << endl;
            // Loop over material layers
            for (int iMat = 0; iMat < inter.size(); iMat++) {
                CbmLitMaterialInfo mat = inter.get(iMat);

                // Check if track parameters are correct
                if (!IsParCorrect(par)) {
                    return LitStatus.kLITERROR;
                }

                double[] Fnew = null;
                if (F != null) {
                    Fnew = new double[25];
                }
                // Extrapolate to the next boundary
                if (fExtrapolator.Extrapolate(par, mat.GetZpos(), Fnew) == LitStatus.kLITERROR) {
                    return LitStatus.kLITERROR;
                }

                // Update transport matrix
                if (F != null) {
                    UpdateF(F, Fnew);
                }

                // Add material effects
                fMaterial.Update(par, mat, pdg, downstream);

                if (length != null) {
                    length[0] += mat.GetLength();
                }

            }
        }

        if (!IsParCorrect(par)) {
            return LitStatus.kLITERROR;
        } else {
            return LitStatus.kLITSUCCESS;
        }

    }

    void UpdateF(
            double[] F,
            double[] newF) {
        double[] A = new double[25];
        CbmLitMath.Mult25(newF, F, A);
        System.arraycopy(A, 0, F, 0, 25);
    }

    boolean IsParCorrect(
            CbmLitTrackParam par) {
        //TODO move these hardcoded values into CbmLitDefaultSettings.
        double maxSlope = 5.;
        double minSlope = 0.;
        double maxQp = 1000.; // p = 10 MeV
//        System.out.println(par);
        if (abs(par.GetTx()) > maxSlope
                || abs(par.GetTy()) > maxSlope
                || abs(par.GetTx()) < minSlope
                || abs(par.GetTy()) < minSlope
                || abs(par.GetQp()) > maxQp) {
            return false;
        }

        if (Double.isNaN(par.GetX()) || Double.isNaN(par.GetY())
                || Double.isNaN(par.GetTx()) || Double.isNaN(par.GetTy())
                || Double.isNaN(par.GetQp())) {
            return false;
        }

        return true;
    }
}
