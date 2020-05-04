package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitKalmanFilter implements CbmLitTrackUpdate {

    boolean debug = false;

    public LitStatus Update(CbmLitTrackParam parIn, CbmLitTrackParam parOut, CbmLitHit hit, double[] chiSq) {
        parOut = parIn;
        return Update(parOut, hit, chiSq);
    }

    public LitStatus Update(CbmLitTrackParam par, CbmLitHit hit, double[] chiSq) {
        LitStatus result = LitStatus.kLITSUCCESS;
        if (hit.GetType() == LitHitType.kLITSTRIPHIT) {
            if (hit instanceof CbmLitDetPlaneStripHit) {
                result = Update(par, (CbmLitDetPlaneStripHit) hit, chiSq);
            } else {
                result = Update(par, (CbmLitStripHit) hit, chiSq);
            }
        } else if (hit.GetType() == LitHitType.kLITPIXELHIT) {
            result = Update(par, (CbmLitPixelHit) hit, chiSq);
        }
        return result;
    }

    LitStatus Update(
            CbmLitTrackParam par,
            CbmLitPixelHit hit,
            double[] chiSq) {
        double[] cIn = par.GetCovMatrix();

        final double ONE = 1., TWO = 2.;

        double dxx = hit.GetDx() * hit.GetDx();
        double dxy = hit.GetDxy();
        double dyy = hit.GetDy() * hit.GetDy();

        // calculate residuals
        double dx = hit.GetX() - par.GetX();
        double dy = hit.GetY() - par.GetY();

        // Calculate and inverse residual covariance matrix
        double t = ONE / (dxx * dyy + dxx * cIn[5] + dyy * cIn[0] + cIn[0] * cIn[5]
                - dxy * dxy - TWO * dxy * cIn[1] - cIn[1] * cIn[1]);
        double R00 = (dyy + cIn[5]) * t;
        double R01 = -(dxy + cIn[1]) * t;
        double R11 = (dxx + cIn[0]) * t;

        // Calculate Kalman gain matrix
        double K00 = cIn[0] * R00 + cIn[1] * R01;
        double K01 = cIn[0] * R01 + cIn[1] * R11;
        double K10 = cIn[1] * R00 + cIn[5] * R01;
        double K11 = cIn[1] * R01 + cIn[5] * R11;
        double K20 = cIn[2] * R00 + cIn[6] * R01;
        double K21 = cIn[2] * R01 + cIn[6] * R11;
        double K30 = cIn[3] * R00 + cIn[7] * R01;
        double K31 = cIn[3] * R01 + cIn[7] * R11;
        double K40 = cIn[4] * R00 + cIn[8] * R01;
        double K41 = cIn[4] * R01 + cIn[8] * R11;

        // Calculate filtered state vector
        double[] xOut = {par.GetX(), par.GetY(), par.GetTx(), par.GetTy(), par.GetQp()};
        xOut[0] += K00 * dx + K01 * dy;
        xOut[1] += K10 * dx + K11 * dy;
        xOut[2] += K20 * dx + K21 * dy;
        xOut[3] += K30 * dx + K31 * dy;
        xOut[4] += K40 * dx + K41 * dy;

        // Calculate filtered covariance matrix
        double[] cOut = new double[cIn.length];
        System.arraycopy(cIn, 0, cOut, 0, cIn.length);

        cOut[0] += -K00 * cIn[0] - K01 * cIn[1];
        cOut[1] += -K00 * cIn[1] - K01 * cIn[5];
        cOut[2] += -K00 * cIn[2] - K01 * cIn[6];
        cOut[3] += -K00 * cIn[3] - K01 * cIn[7];
        cOut[4] += -K00 * cIn[4] - K01 * cIn[8];

        cOut[5] += -K11 * cIn[5] - K10 * cIn[1];
        cOut[6] += -K11 * cIn[6] - K10 * cIn[2];
        cOut[7] += -K11 * cIn[7] - K10 * cIn[3];
        cOut[8] += -K11 * cIn[8] - K10 * cIn[4];

        cOut[9] += -K20 * cIn[2] - K21 * cIn[6];
        cOut[10] += -K20 * cIn[3] - K21 * cIn[7];
        cOut[11] += -K20 * cIn[4] - K21 * cIn[8];

        cOut[12] += -K30 * cIn[3] - K31 * cIn[7];
        cOut[13] += -K30 * cIn[4] - K31 * cIn[8];

        cOut[14] += -K40 * cIn[4] - K41 * cIn[8];

        // Copy filtered state to output
        par.SetX(xOut[0]);
        par.SetY(xOut[1]);
        par.SetTx(xOut[2]);
        par.SetTy(xOut[3]);
        par.SetQp(xOut[4]);
        par.SetCovMatrix(cOut);

        // Calculate chi-square
        double xmx = hit.GetX() - par.GetX();
        double ymy = hit.GetY() - par.GetY();
        double C0 = cOut[0];
        double C1 = cOut[1];
        double C5 = cOut[5];

        double norm = dxx * dyy - dxx * C5 - dyy * C0 + C0 * C5
                - dxy * dxy + 2 * dxy * C1 - C1 * C1;

        chiSq[0] = ((xmx * (dyy - C5) - ymy * (dxy - C1)) * xmx
                + (-xmx * (dxy - C1) + ymy * (dxx - C0)) * ymy) / norm;

        return LitStatus.kLITSUCCESS;
    }

    LitStatus UpdateWMF(
            CbmLitTrackParam par,
            CbmLitPixelHit hit,
            double[] chiSq) {
        double[] xIn = {par.GetX(), par.GetY(), par.GetTx(), par.GetTy(), par.GetQp()};

        double[] cIn = par.GetCovMatrix();
        double[] cInInv = par.GetCovMatrix();

        double dxx = hit.GetDx() * hit.GetDx();
        double dxy = hit.GetDxy();
        double dyy = hit.GetDy() * hit.GetDy();

        // Inverse predicted cov matrix
        CbmLitMatrixMath.InvSym15(cInInv);
        // Calculate C1
        double[] C1 = cInInv;
        double det = dxx * dyy - dxy * dxy;
        C1[0] += dyy / det;
        C1[1] += -dxy / det;
        C1[5] += dxx / det;
        // Inverse C1 . output updated covariance matrix
        CbmLitMatrixMath.InvSym15(C1);

        double[] t = new double[5];
        t[0] = cInInv[0] * par.GetX() + cInInv[1] * par.GetY() + cInInv[2] * par.GetTx()
                + cInInv[3] * par.GetTy() + cInInv[4] * par.GetQp()
                + (dyy * hit.GetX() - dxy * hit.GetY()) / det;
        t[1] = cInInv[1] * par.GetX() + cInInv[5] * par.GetY() + cInInv[6] * par.GetTx()
                + cInInv[7] * par.GetTy() + cInInv[8] * par.GetQp()
                + (-dxy * hit.GetX() + dxx * hit.GetY()) / det;
        t[2] = cInInv[2] * par.GetX() + cInInv[6] * par.GetY() + cInInv[9] * par.GetTx()
                + cInInv[10] * par.GetTy() + cInInv[11] * par.GetQp();
        t[3] = cInInv[3] * par.GetX() + cInInv[7] * par.GetY() + cInInv[10] * par.GetTx()
                + cInInv[12] * par.GetTy() + cInInv[13] * par.GetQp();
        t[4] = cInInv[4] * par.GetX() + cInInv[8] * par.GetY() + cInInv[11] * par.GetTx()
                + cInInv[13] * par.GetTy() + cInInv[14] * par.GetQp();

        double[] xOut = new double[5];
        CbmLitMatrixMath.Mult15On5(C1, t, xOut);

        // Copy filtered state to output
        par.SetX(xOut[0]);
        par.SetY(xOut[1]);
        par.SetTx(xOut[2]);
        par.SetTy(xOut[3]);
        par.SetQp(xOut[4]);
        par.SetCovMatrix(C1);

        // Calculate chi square
        double dx0 = xOut[0] - xIn[0];
        double dx1 = xOut[1] - xIn[1];
        double dx2 = xOut[2] - xIn[2];
        double dx3 = xOut[3] - xIn[3];
        double dx4 = xOut[4] - xIn[4];
        double xmx = hit.GetX() - par.GetX();
        double ymy = hit.GetY() - par.GetY();
        chiSq[0] = ((xmx * dyy - ymy * dxy) * xmx + (-xmx * dxy + ymy * dxx) * ymy) / det
                + (dx0 * cInInv[0] + dx1 * cInInv[1] + dx2 * cInInv[2] + dx3 * cInInv[3] + dx4 * cInInv[4]) * dx0
                + (dx0 * cInInv[1] + dx1 * cInInv[5] + dx2 * cInInv[6] + dx3 * cInInv[7] + dx4 * cInInv[8]) * dx1
                + (dx0 * cInInv[2] + dx1 * cInInv[6] + dx2 * cInInv[9] + dx3 * cInInv[10] + dx4 * cInInv[11]) * dx2
                + (dx0 * cInInv[3] + dx1 * cInInv[7] + dx2 * cInInv[10] + dx3 * cInInv[12] + dx4 * cInInv[13]) * dx3
                + (dx0 * cInInv[4] + dx1 * cInInv[8] + dx2 * cInInv[11] + dx3 * cInInv[13] + dx4 * cInInv[14]) * dx4;

        return LitStatus.kLITSUCCESS;
    }

    LitStatus Update(
            CbmLitTrackParam par,
            CbmLitStripHit hit,
            double[] chiSq) {
        double[] xIn = {par.GetX(), par.GetY(), par.GetTx(), par.GetTy(), par.GetQp()};
        double[] cIn = par.GetCovMatrix();

        double u = hit.GetU();
        double duu = hit.GetDu() * hit.GetDu();
        double phiCos = hit.GetCosPhi();
        double phiSin = hit.GetSinPhi();
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
        if (debug) {
            System.out.println("**** In update***");
            System.out.println("u meas " + u);
            System.out.println("xIn[0] " + xIn[0] + " xIn[1] " + xIn[1]);
            System.out.println("phiCos " + phiCos + " phiSin " + phiSin);
            System.out.println("u pred " + (xIn[0] * phiCos + xIn[1] * phiSin));
            System.out.println("residual r " + r);
        }
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

        if (debug) {
            System.out.println("filtered residual ru " + ru);
        }
        // Calculate chi-square
        chiSq[0] = (ru * ru) / (duu - phiCosSq * cOut[0] - phi2SinCos * cOut[1] - phiSinSq * cOut[5]);

        return LitStatus.kLITSUCCESS;
    }

    LitStatus UpdateWMF(
            CbmLitTrackParam par,
            CbmLitStripHit hit,
            double[] chiSq) {
        double[] xIn = {par.GetX(), par.GetY(), par.GetTx(), par.GetTy(), par.GetQp()};

        double[] cIn = par.GetCovMatrix();
        double[] cInInv = par.GetCovMatrix();

        double duu = hit.GetDu() * hit.GetDu();
        double phiCos = hit.GetCosPhi();
        double phiSin = hit.GetSinPhi();

        // Inverse predicted cov matrix
        CbmLitMatrixMath.InvSym15(cInInv);
        // Calculate C1
        double[] C1 = cInInv;
        C1[0] += phiCos * phiCos / duu;
        C1[1] += phiCos * phiSin / duu;
        C1[5] += phiSin * phiSin / duu;
        // Inverse C1 . output updated covariance matrix
        CbmLitMatrixMath.InvSym15(C1);

        double[] t = new double[5];
        t[0] = cInInv[0] * par.GetX() + cInInv[1] * par.GetY() + cInInv[2] * par.GetTx()
                + cInInv[3] * par.GetTy() + cInInv[4] * par.GetQp()
                + phiCos * hit.GetU() / duu;
        t[1] = cInInv[1] * par.GetX() + cInInv[5] * par.GetY() + cInInv[6] * par.GetTx()
                + cInInv[7] * par.GetTy() + cInInv[8] * par.GetQp()
                + phiSin * hit.GetU() / duu;
        t[2] = cInInv[2] * par.GetX() + cInInv[6] * par.GetY() + cInInv[9] * par.GetTx()
                + cInInv[10] * par.GetTy() + cInInv[11] * par.GetQp();
        t[3] = cInInv[3] * par.GetX() + cInInv[7] * par.GetY() + cInInv[10] * par.GetTx()
                + cInInv[12] * par.GetTy() + cInInv[13] * par.GetQp();
        t[4] = cInInv[4] * par.GetX() + cInInv[8] * par.GetY() + cInInv[11] * par.GetTx()
                + cInInv[13] * par.GetTy() + cInInv[14] * par.GetQp();

        double[] xOut = new double[5];
        CbmLitMatrixMath.Mult15On5(C1, t, xOut);

        // Copy filtered state to output
        par.SetX(xOut[0]);
        par.SetY(xOut[1]);
        par.SetTx(xOut[2]);
        par.SetTy(xOut[3]);
        par.SetQp(xOut[4]);
        par.SetCovMatrix(C1);

        // Calculate chi square
        double zeta = hit.GetU() - phiCos * xOut[0] - phiSin * xOut[1];
        double dx0 = xOut[0] - xIn[0];
        double dx1 = xOut[1] - xIn[1];
        double dx2 = xOut[2] - xIn[2];
        double dx3 = xOut[3] - xIn[3];
        double dx4 = xOut[4] - xIn[4];
        chiSq[0] = zeta * zeta / duu
                + (dx0 * cInInv[0] + dx1 * cInInv[1] + dx2 * cInInv[2] + dx3 * cInInv[3] + dx4 * cInInv[4]) * dx0
                + (dx0 * cInInv[1] + dx1 * cInInv[5] + dx2 * cInInv[6] + dx3 * cInInv[7] + dx4 * cInInv[8]) * dx1
                + (dx0 * cInInv[2] + dx1 * cInInv[6] + dx2 * cInInv[9] + dx3 * cInInv[10] + dx4 * cInInv[11]) * dx2
                + (dx0 * cInInv[3] + dx1 * cInInv[7] + dx2 * cInInv[10] + dx3 * cInInv[12] + dx4 * cInInv[13]) * dx3
                + (dx0 * cInInv[4] + dx1 * cInInv[8] + dx2 * cInInv[11] + dx3 * cInInv[13] + dx4 * cInInv[14]) * dx4;

        return LitStatus.kLITSUCCESS;
    }

    //TODO see if this needs editing...
    //In principle, would have to convert everything into local coordinates, 
    //but let's see how far I get just transforming  u and phi into global coordinates...
    LitStatus Update(
            CbmLitTrackParam par,
            CbmLitDetPlaneStripHit hit,
            double[] chiSq) {
        double[] xIn = {par.GetX(), par.GetY(), par.GetTx(), par.GetTy(), par.GetQp()};
        double[] cIn = par.GetCovMatrix();

        double u = hit.GetU();
        double duu = hit.GetDu() * hit.GetDu();
        double phiCos = hit.GetCosPhi();
        double phiSin = hit.GetSinPhi();
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
        double pdu = hit.GetPlane().u(new CartesianThreeVector(xIn[0], xIn[1], hit.GetZ()));
        if (debug) {
            System.out.println(hit.GetPlane().name() + " phi " + hit.GetPhi());
            System.out.println("pos: " + xIn[0] + " " + xIn[1] + " " + hit.GetZ());
            System.out.println("u= " + u + " x*cosPhi+y*sinPhi= " + (xIn[0] * phiCos + xIn[1] * phiSin));
            System.out.println("plane predicted u (pdu)" + pdu);
        }
        //cng let's see if this simepl replacement works...
        r = u - pdu;
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

        // Filtered resuduals
        double ru = u - xOut[0] * phiCos - xOut[1] * phiSin;
        double pduFiltered = hit.GetPlane().u(new CartesianThreeVector(xOut[0], xOut[1], hit.GetZ()));
        if (debug) {
            System.out.println("filtered pos: " + xOut[0] + " " + xOut[1] + " " + hit.GetZ());
            System.out.println("u= " + u + " x*cosPhi+y*sinPhi= " + (xOut[0] * phiCos + xOut[1] * phiSin));
            System.out.println("plane predicted u (pdu)" + pduFiltered);
        }
        // let's see if this simple replacement works...
        ru = u - pduFiltered;
//        
        // Calculate chi-square
        chiSq[0] = (ru * ru) / (duu - phiCosSq * cOut[0] - phi2SinCos * cOut[1] - phiSinSq * cOut[5]);

        return LitStatus.kLITSUCCESS;
    }

}
