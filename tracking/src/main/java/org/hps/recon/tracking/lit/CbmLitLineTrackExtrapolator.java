package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitLineTrackExtrapolator implements CbmLitTrackExtrapolator {

    public LitStatus Extrapolate(
            CbmLitTrackParam parIn,
            CbmLitTrackParam parOut,
            double zOut,
            double[] F) {
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
            CbmLitTrackParam parIn,
            DetectorPlane p,
            double[] F) {
        throw new RuntimeException("not yet implemented!");
    }

    public LitStatus Extrapolate(
            CbmLitTrackParam par,
            double zOut,
            double[] F) {
        double[] X = {par.GetX(), par.GetY(),
            par.GetTx(), par.GetTy(),
            par.GetQp()
        };

        double dz = zOut - par.GetZ();

        //transport state vector F*X*F.T()
        X[0] = X[0] + dz * X[2];
        X[1] = X[1] + dz * X[3];

        par.SetX(X[0]);
        par.SetY(X[1]);

        double[] C = par.GetCovMatrix();
        //transport covariance matrix F*C*F.T()
        double t3 = C[2] + dz * C[9];
        double t7 = dz * C[10];
        double t8 = C[3] + t7;
        double t19 = C[7] + dz * C[12];
        C[0] = C[0] + dz * C[2] + t3 * dz;
        C[1] = C[1] + dz * C[6] + t8 * dz;
        C[2] = t3;
        C[3] = t8;
        C[4] = C[4] + dz * C[11];
        C[5] = C[5] + dz * C[7] + t19 * dz;
        C[6] = C[6] + t7;
        C[7] = t19;
        C[8] = C[8] + dz * C[13];

        par.SetCovMatrix(C);
        par.SetZ(zOut);

        // Transport matrix calculation
        if (F != null) {
            //     F.assign(25, 0.);
            F[0] = 1.;
            F[6] = 1.;
            F[12] = 1.;
            F[18] = 1.;
            F[24] = 1.;
            F[2] = dz;
            F[8] = dz;
        }

        return LitStatus.kLITSUCCESS;
    }
}
