package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitMath {

    public static double ChiSq(
            CbmLitTrackParam par,
            CbmLitHit hit) {
        double chisq = 0.;
        if (hit.GetType() == LitHitType.kLITSTRIPHIT) {
            if (hit instanceof CbmLitDetPlaneStripHit) {
                chisq = ChiSq(par, (CbmLitDetPlaneStripHit) hit);
            } else {
                chisq = ChiSq(par, (CbmLitStripHit) hit);
            }
        } else if (hit.GetType() == LitHitType.kLITPIXELHIT) {
            chisq = ChiSq(par, (CbmLitPixelHit) hit);
        }
        return chisq;
    }

    public static double ChiSq(
            CbmLitTrackParam par,
            CbmLitStripHit hit) {
        double duu = hit.GetDu() * hit.GetDu();
        double phiCos = hit.GetCosPhi();
        double phiSin = hit.GetSinPhi();
        double phiCosSq = phiCos * phiCos;
        double phiSinSq = phiSin * phiSin;
        double phi2SinCos = 2 * phiCos * phiSin;
        double C0 = par.GetCovariance(0);
        double C1 = par.GetCovariance(1);
        double C5 = par.GetCovariance(5);

        double ru = hit.GetU() - par.GetX() * phiCos - par.GetY() * phiSin;

        return (ru * ru) / (duu - phiCosSq * C0 - phi2SinCos * C1 - phiSinSq * C5);
    }

    //TODO do we need to modify this code
    public static double ChiSq(
            CbmLitTrackParam par,
            CbmLitDetPlaneStripHit hit) {
        double duu = hit.GetDu() * hit.GetDu();
        double phiCos = hit.GetCosPhi();
        double phiSin = hit.GetSinPhi();
        double phiCosSq = phiCos * phiCos;
        double phiSinSq = phiSin * phiSin;
        double phi2SinCos = 2 * phiCos * phiSin;
        double C0 = par.GetCovariance(0);
        double C1 = par.GetCovariance(1);
        double C5 = par.GetCovariance(5);

        double ru = hit.GetU() - par.GetX() * phiCos - par.GetY() * phiSin;

        return (ru * ru) / (duu - phiCosSq * C0 - phi2SinCos * C1 - phiSinSq * C5);
    }

    public static double ChiSq(
            CbmLitTrackParam par,
            CbmLitPixelHit hit) {
        double dxx = hit.GetDx() * hit.GetDx();
        double dxy = hit.GetDxy();
        double dyy = hit.GetDy() * hit.GetDy();
        double xmx = hit.GetX() - par.GetX();
        double ymy = hit.GetY() - par.GetY();
        double C0 = par.GetCovariance(0);
        double C1 = par.GetCovariance(1);
        double C5 = par.GetCovariance(5);

        double norm = dxx * dyy - dxx * C5 - dyy * C0 + C0 * C5
                - dxy * dxy + 2 * dxy * C1 - C1 * C1;
        if (norm == 0.) {
            norm = 1e-10;
        }
        return ((xmx * (dyy - C5) - ymy * (dxy - C1)) * xmx
                + (-xmx * (dxy - C1) + ymy * (dxx - C0)) * ymy) / norm;
    }

    public static boolean Mult25(
            double[] a,
            double[] b,
            double[] c) {
        c[0] = a[0] * b[0] + a[1] * b[5] + a[2] * b[10] + a[3] * b[15] + a[4] * b[20];
        c[1] = a[0] * b[1] + a[1] * b[6] + a[2] * b[11] + a[3] * b[16] + a[4] * b[21];
        c[2] = a[0] * b[2] + a[1] * b[7] + a[2] * b[12] + a[3] * b[17] + a[4] * b[22];
        c[3] = a[0] * b[3] + a[1] * b[8] + a[2] * b[13] + a[3] * b[18] + a[4] * b[23];
        c[4] = a[0] * b[4] + a[1] * b[9] + a[2] * b[14] + a[3] * b[19] + a[4] * b[24];
        c[5] = a[5] * b[0] + a[6] * b[5] + a[7] * b[10] + a[8] * b[15] + a[9] * b[20];
        c[6] = a[5] * b[1] + a[6] * b[6] + a[7] * b[11] + a[8] * b[16] + a[9] * b[21];
        c[7] = a[5] * b[2] + a[6] * b[7] + a[7] * b[12] + a[8] * b[17] + a[9] * b[22];
        c[8] = a[5] * b[3] + a[6] * b[8] + a[7] * b[13] + a[8] * b[18] + a[9] * b[23];
        c[9] = a[5] * b[4] + a[6] * b[9] + a[7] * b[14] + a[8] * b[19] + a[9] * b[24];
        c[10] = a[10] * b[0] + a[11] * b[5] + a[12] * b[10] + a[13] * b[15] + a[14] * b[20];
        c[11] = a[10] * b[1] + a[11] * b[6] + a[12] * b[11] + a[13] * b[16] + a[14] * b[21];
        c[12] = a[10] * b[2] + a[11] * b[7] + a[12] * b[12] + a[13] * b[17] + a[14] * b[22];
        c[13] = a[10] * b[3] + a[11] * b[8] + a[12] * b[13] + a[13] * b[18] + a[14] * b[23];
        c[14] = a[10] * b[4] + a[11] * b[9] + a[12] * b[14] + a[13] * b[19] + a[14] * b[24];
        c[15] = a[15] * b[0] + a[16] * b[5] + a[17] * b[10] + a[18] * b[15] + a[19] * b[20];
        c[16] = a[15] * b[1] + a[16] * b[6] + a[17] * b[11] + a[18] * b[16] + a[19] * b[21];
        c[17] = a[15] * b[2] + a[16] * b[7] + a[17] * b[12] + a[18] * b[17] + a[19] * b[22];
        c[18] = a[15] * b[3] + a[16] * b[8] + a[17] * b[13] + a[18] * b[18] + a[19] * b[23];
        c[19] = a[15] * b[4] + a[16] * b[9] + a[17] * b[14] + a[18] * b[19] + a[19] * b[24];
        c[20] = a[20] * b[0] + a[21] * b[5] + a[22] * b[10] + a[23] * b[15] + a[24] * b[20];
        c[21] = a[20] * b[1] + a[21] * b[6] + a[22] * b[11] + a[23] * b[16] + a[24] * b[21];
        c[22] = a[20] * b[2] + a[21] * b[7] + a[22] * b[12] + a[23] * b[17] + a[24] * b[22];
        c[23] = a[20] * b[3] + a[21] * b[8] + a[22] * b[13] + a[23] * b[18] + a[24] * b[23];
        c[24] = a[20] * b[4] + a[21] * b[9] + a[22] * b[14] + a[23] * b[19] + a[24] * b[24];

        return true;
    }

    public static int NDF(
            CbmLitTrack track) {
        int ndf = 0;
        for (int i = 0; i < track.GetNofHits(); i++) {
            if (track.GetHit(i).GetType() == LitHitType.kLITPIXELHIT) {
                ndf += 2;
            } else if (track.GetHit(i).GetType() == LitHitType.kLITSTRIPHIT) {
                ndf++;
            }
        }
        ndf -= 5;
        if (ndf > 0) {
            return ndf;
        } else {
            return 1;
        }
    }

    public static int NDF(HpsLitTrack track) {
        int ndf = 0;
        for (int i = 0; i < track.GetNofHits(); i++) {
            ndf++;
        }
        ndf -= 5;
        if (ndf > 0) {
            return ndf;
        } else {
            return 1;
        }
    }
}
