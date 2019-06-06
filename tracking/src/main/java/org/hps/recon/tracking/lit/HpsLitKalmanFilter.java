package org.hps.recon.tracking.lit;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsLitKalmanFilter {

    LitStatus Update(
            CbmLitTrackParam par,
            HpsStripHit hit,
            double[] chiSq) {
        double[] xIn = {par.GetX(), par.GetY(), par.GetTx(), par.GetTy(), par.GetQp()};
        double[] cIn = par.GetCovMatrix();

        double u = hit.u();
        double duu = hit.du() * hit.du();
        double phiCos = cos(hit.phi());
        double phiSin = sin(hit.phi());
        double phiCosSq = phiCos * phiCos;
        double phiSinSq = phiSin * phiSin;
        double phi2SinCos = 2 * phiCos * phiSin;

        // Inverted covariance matrix of predicted residual
        double R = 1. / (duu + cIn[0] * phiCosSq + phi2SinCos * cIn[1] + cIn[5] * phiSinSq);

        // Calculate Kalman gain matrix
        double K0 = cIn[0] * phiCos + cIn[1] * phiSin;
        double K1 = cIn[1] * phiCos + cIn[5] * phiSin;
        double K2 = cIn[2] * phiCos + cIn[6] * phiSin;
        double K3 = cIn[3] * phiCos + cIn[7] * phiSin;
        double K4 = cIn[4] * phiCos + cIn[8] * phiSin;

        double KR0 = K0 * R;
        double KR1 = K1 * R;
        double KR2 = K2 * R;
        double KR3 = K3 * R;
        double KR4 = K4 * R;

        // Residual of predictions
        double r = u - xIn[0] * phiCos - xIn[1] * phiSin;

        // Calculate filtered state vector
        double[] xOut = new double[5];
        xOut[0] = xIn[0] + KR0 * r;
        xOut[1] = xIn[1] + KR1 * r;
        xOut[2] = xIn[2] + KR2 * r;
        xOut[3] = xIn[3] + KR3 * r;
        xOut[4] = xIn[4] + KR4 * r;

        // Calculate filtered covariance matrix
        double[] cOut = new double[15];
        cOut[0] = cIn[0] - KR0 * K0;
        cOut[1] = cIn[1] - KR0 * K1;
        cOut[2] = cIn[2] - KR0 * K2;
        cOut[3] = cIn[3] - KR0 * K3;
        cOut[4] = cIn[4] - KR0 * K4;

        cOut[5] = cIn[5] - KR1 * K1;
        cOut[6] = cIn[6] - KR1 * K2;
        cOut[7] = cIn[7] - KR1 * K3;
        cOut[8] = cIn[8] - KR1 * K4;

        cOut[9] = cIn[9] - KR2 * K2;
        cOut[10] = cIn[10] - KR2 * K3;
        cOut[11] = cIn[11] - KR2 * K4;

        cOut[12] = cIn[12] - KR3 * K3;
        cOut[13] = cIn[13] - KR3 * K4;

        cOut[14] = cIn[14] - KR4 * K4;

        // Copy filtered state to output
        par.SetX(xOut[0]);
        par.SetY(xOut[1]);
        par.SetTx(xOut[2]);
        par.SetTy(xOut[3]);
        par.SetQp(xOut[4]);
        par.SetCovMatrix(cOut);

        // Filtered residuals
        double ru = u - xOut[0] * phiCos - xOut[1] * phiSin;

        // Calculate chi-square
        chiSq[0] = (ru * ru) / (duu - phiCosSq * cOut[0] - phi2SinCos * cOut[1] - phiSinSq * cOut[5]);

        return LitStatus.kLITSUCCESS;
    }
}
