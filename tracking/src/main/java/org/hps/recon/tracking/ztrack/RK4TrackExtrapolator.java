package org.hps.recon.tracking.ztrack;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class RK4TrackExtrapolator implements TrackExtrapolator {

    Field fField; // magnetic field

    public RK4TrackExtrapolator(Field field) {
        fField = field;
    }

    public Status Extrapolate(ZTrackParam parIn, ZTrackParam parOut, double zOut, double[] F) {
        parOut.copyFrom(parIn);
        Status stat = Extrapolate(parOut, zOut, F);
        //System.out.println("back"+parOut);
        return stat;
    }

    public Status Extrapolate(ZTrackParam par, double zOut, double[] F) {
        double zIn = par.GetZ();
//        System.out.println("here");
//        System.out.println(par);
        double[] xIn = par.GetStateVector();
        double[] xOut = new double[5];
        double[] F1 = new double[25];

        RK4Order(xIn, zIn, xOut, zOut, F1);

        double[] cIn = par.GetCovMatrix();
        double[] cOut = new double[15];
        TransportC(cIn, F1, cOut);

        par.SetStateVector(xOut);
        par.SetCovMatrix(cOut);
        par.SetZ(zOut);

        if (F != null) {
            System.arraycopy(F1, 0, F, 0, 25);
        }
//System.out.println(par);
        return Status.SUCCESS;
    }

    public Status Extrapolate(ZTrackParam parIn, ZTrackParam parOut, DetectorPlane p, double[] F) {
        parOut.copyFrom(parIn);
        Status stat = Extrapolate(parOut, p, F);
        //System.out.println("back"+parOut);
        return stat;
    }

    public Status Extrapolate(ZTrackParam par, DetectorPlane p, double[] F) {
        // extrapolate the track parameters to an arbitrarily oriented plane...
        // note that we should be reasonably close... so propagate to the z of the 
        // bounding box of the plane using the full RK...
        //TODO check that we aren't within bounding box zmin - zmax
        double zPlane = p.position().z();
        double zIn = par.GetZ();
        double zOut = zIn < zPlane ? p.getZmin() : p.getZmax();
        double dz = zOut - zIn;

        double[] xIn = par.GetStateVector();
        double[] xOut = new double[5];
        double[] F1 = new double[25];
        ZTrackParam par1 = new ZTrackParam(par);
        // if we're not at the bounding box, get there...
        if (abs(dz) > DefaultSettings.MINIMUM_PROPAGATION_DISTANCE) {
            RK4Order(xIn, zIn, xOut, zOut, F1);
            par1.SetStateVector(xOut);
            par1.SetZ(zOut);
        }

//        double[] cIn = par.GetCovMatrix();
//        double[] cOut = new double[15];
//        TransportC(cIn, F1, cOut);
//
//        par.SetCovMatrix(cOut);
        // have now propagated using the full field to the zmin/zmax of the plane. 
        // let's now find the track-plane intersection point...
        // create a PhysicalTrack to propagate...
        double[] pos = {par1.GetX(), par1.GetY(), par1.GetZ()};
        double[] mom = new double[3];
        par1.GetMomentum(mom);
        // assume electron here...
        double p2 = mom[0] * mom[0] + mom[1] * mom[1] + mom[2] * mom[2];
        double E = sqrt(p2 + 0.000511 * 0.000511);
        int q = par.GetQp() > 0 ? 1 : -1;
        PhysicalTrack pt = new PhysicalTrack(pos, mom, E, q);
        //propagate it to the plane
        double[] bField = new double[3];
        fField.GetFieldValue(pos[0], pos[1], pos[2], bField);
        CartesianThreeVector field = new CartesianThreeVector(bField);
        // If plane is not too far from being a z plane we are roughly close.
        // this intersection calculation assumes that the field is locally constant
        //TODO iterate the constant field approximation with the RK extrapolation until the z positions agree
        IntersectionStatus is = pt.planeIntersection(p.position(), p.normal(), field);
        if (is.success()) {
            zOut = is.position().z();
            zIn = par.GetZ();
            xIn = par.GetStateVector();
            xOut = new double[5];
            F1 = new double[25];

            RK4Order(xIn, zIn, xOut, zOut, F1);

            double[] cIn = par.GetCovMatrix();
            double[] cOut = new double[15];
            TransportC(cIn, F1, cOut);

            par.SetStateVector(xOut);
            par.SetCovMatrix(cOut);
            par.SetZ(zOut);

            if (F != null) {
                System.arraycopy(F1, 0, F, 0, 25);
            }
        } else {
            System.err.println("unable to find intersection with plane");
            System.out.println("unable to find intersection with plane "+p.name());
            System.out.println(par);
            return Status.ERROR;
        }
        // now check that the global position is within the limits of the bounded detector plane...

        if (!p.inBounds(xOut[0], xOut[1], zOut)) {
            System.err.println(p.name() + " : intersection point not in bounds");
            System.out.println(p.name() + " : intersection point not in bounds");
            System.out.println(par);
            return Status.ERROR;
        }
        return Status.SUCCESS;
    }

    void RK4Order(
            double[] xIn,
            double zIn,
            double[] xOut,
            double zOut,
            double[] derivs) {
        final double fC = 0.000299792458;

        double[] coef = {0.0, 0.5, 0.5, 1.0};

        double[] Ax = new double[4];
        double[] Ay = new double[4];
        double[] dAx_dtx = new double[4];
        double[] dAy_dtx = new double[4];
        double[] dAx_dty = new double[4];
        double[] dAy_dty = new double[4];
        double[][] k = new double[4][4];

        double h = zOut - zIn;
        double hC = h * fC;
        double hCqp = h * fC * xIn[4];
        double[] x0 = new double[4];

        double[] x = {xIn[0], xIn[1], xIn[2], xIn[3]};

        for (int iStep = 0; iStep < 4; iStep++) { // 1
            if (iStep > 0) {
                for (int i = 0; i < 4; i++) {
                    x[i] = xIn[i] + coef[iStep] * k[i][iStep - 1];
                }
            }

            double[] BxByBz = new double[3];
            fField.GetFieldValue(x[0], x[1], zIn + coef[iStep] * h, BxByBz);
            double Bx = BxByBz[0];
            double By = BxByBz[1];
            double Bz = BxByBz[2];

            double tx = x[2];
            double ty = x[3];
            double txtx = tx * tx;
            double tyty = ty * ty;
            double txty = tx * ty;
            double txtxtyty1 = 1.0 + txtx + tyty;
            double t1 = sqrt(txtxtyty1);
            double t2 = 1.0 / txtxtyty1;

            Ax[iStep] = (txty * Bx + ty * Bz - (1.0 + txtx) * By) * t1;
            Ay[iStep] = (-txty * By - tx * Bz + (1.0 + tyty) * Bx) * t1;

            dAx_dtx[iStep] = Ax[iStep] * tx * t2 + (ty * Bx - 2.0 * tx * By) * t1;
            dAx_dty[iStep] = Ax[iStep] * ty * t2 + (tx * Bx + Bz) * t1;
            dAy_dtx[iStep] = Ay[iStep] * tx * t2 + (-ty * By - Bz) * t1;
            dAy_dty[iStep] = Ay[iStep] * ty * t2 + (-tx * By + 2.0 * ty * Bx) * t1;

            k[0][iStep] = tx * h;
            k[1][iStep] = ty * h;
            k[2][iStep] = Ax[iStep] * hCqp;
            k[3][iStep] = Ay[iStep] * hCqp;

        } // 1

        for (int i = 0; i < 4; i++) {
            xOut[i] = CalcOut(xIn[i], k[i]);
        }
        xOut[4] = xIn[4];

        // Calculation of the derivatives
        // derivatives dx/dx and dx/dy
        // dx/dx
        derivs[0] = 1.;
        derivs[5] = 0.;
        derivs[10] = 0.;
        derivs[15] = 0.;
        derivs[20] = 0.;
        // dx/dy
        derivs[1] = 0.;
        derivs[6] = 1.;
        derivs[11] = 0.;
        derivs[16] = 0.;
        derivs[21] = 0.;
        // end of derivatives dx/dx and dx/dy

        // Derivatives dx/tx
        x[0] = x0[0] = 0.0;
        x[1] = x0[1] = 0.0;
        x[2] = x0[2] = 1.0;
        x[3] = x0[3] = 0.0;
        for (int iStep = 0; iStep < 4; iStep++) { // 2
            if (iStep > 0) {
                for (int i = 0; i < 4; i++) {
                    if (i != 2) {
                        x[i] = x0[i] + coef[iStep] * k[i][iStep - 1];
                    }
                }
            }

            k[0][iStep] = x[2] * h;
            k[1][iStep] = x[3] * h;
            //k[2][iStep] = (dAx_dtx[iStep] * x[2] + dAx_dty[iStep] * x[3]) * hCqp;
            k[3][iStep] = (dAy_dtx[iStep] * x[2] + dAy_dty[iStep] * x[3]) * hCqp;
        } // 2

        derivs[2] = CalcOut(x0[0], k[0]);
        derivs[7] = CalcOut(x0[1], k[1]);
        derivs[12] = 1.0;
        derivs[17] = CalcOut(x0[3], k[3]);
        derivs[22] = 0.0;
        // end of derivatives dx/dtx

        // Derivatives    dx/ty
        x[0] = x0[0] = 0.0;
        x[1] = x0[1] = 0.0;
        x[2] = x0[2] = 0.0;
        x[3] = x0[3] = 1.0;
        for (int iStep = 0; iStep < 4; iStep++) { // 4
            if (iStep > 0) {
                for (int i = 0; i < 4; i++) {
                    if (i != 3) {
                        x[i] = x0[i] + coef[iStep] * k[i][iStep - 1];
                    }
                }
            }

            k[0][iStep] = x[2] * h;
            k[1][iStep] = x[3] * h;
            k[2][iStep] = (dAx_dtx[iStep] * x[2] + dAx_dty[iStep] * x[3]) * hCqp;
            //k[3][iStep] = (dAy_dtx[iStep] * x[2] + dAy_dty[iStep] * x[3]) * hCqp;
        }  // 4

        derivs[3] = CalcOut(x0[0], k[0]);
        derivs[8] = CalcOut(x0[1], k[1]);
        derivs[13] = CalcOut(x0[2], k[2]);
        derivs[18] = 1.;
        derivs[23] = 0.;
        // end of derivatives dx/dty

        // Derivatives dx/dqp
        x[0] = x0[0] = 0.0;
        x[1] = x0[1] = 0.0;
        x[2] = x0[2] = 0.0;
        x[3] = x0[3] = 0.0;
        for (int iStep = 0; iStep < 4; iStep++) { // 4
            if (iStep > 0) {
                for (int i = 0; i < 4; i++) {
                    x[i] = x0[i] + coef[iStep] * k[i][iStep - 1];
                }
            }

            k[0][iStep] = x[2] * h;
            k[1][iStep] = x[3] * h;
            k[2][iStep] = Ax[iStep] * hC
                    + hCqp * (dAx_dtx[iStep] * x[2] + dAx_dty[iStep] * x[3]);
            k[3][iStep] = Ay[iStep] * hC
                    + hCqp * (dAy_dtx[iStep] * x[2] + dAy_dty[iStep] * x[3]);
        }  // 4

        derivs[4] = CalcOut(x0[0], k[0]);
        derivs[9] = CalcOut(x0[1], k[1]);
        derivs[14] = CalcOut(x0[2], k[2]);
        derivs[19] = CalcOut(x0[3], k[3]);
        derivs[24] = 1.;
        // end of derivatives dx/dqp

        // end calculation of the derivatives
    }

    double CalcOut(
            double in,
            double[] k) {
        return in + k[0] / 6. + k[1] / 3. + k[2] / 3. + k[3] / 6.;
    }

    void TransportC(
            double[] cIn,
            double[] F,
            double[] cOut) {
        // F*C*Ft
        double A = cIn[2] + F[2] * cIn[9] + F[3] * cIn[10] + F[4] * cIn[11];
        double B = cIn[3] + F[2] * cIn[10] + F[3] * cIn[12] + F[4] * cIn[13];
        double C = cIn[4] + F[2] * cIn[11] + F[3] * cIn[13] + F[4] * cIn[14];

        double D = cIn[6] + F[7] * cIn[9] + F[8] * cIn[10] + F[9] * cIn[11];
        double E = cIn[7] + F[7] * cIn[10] + F[8] * cIn[12] + F[9] * cIn[13];
        double G = cIn[8] + F[7] * cIn[11] + F[8] * cIn[13] + F[9] * cIn[14];

        double H = cIn[9] + F[13] * cIn[10] + F[14] * cIn[11];
        double I = cIn[10] + F[13] * cIn[12] + F[14] * cIn[13];
        double J = cIn[11] + F[13] * cIn[13] + F[14] * cIn[14];

        double K = cIn[13] + F[17] * cIn[11] + F[19] * cIn[14];

        cOut[0] = cIn[0] + F[2] * cIn[2] + F[3] * cIn[3] + F[4] * cIn[4] + A * F[2] + B * F[3] + C * F[4];
        cOut[1] = cIn[1] + F[2] * cIn[6] + F[3] * cIn[7] + F[4] * cIn[8] + A * F[7] + B * F[8] + C * F[9];
        cOut[2] = A + B * F[13] + C * F[14];
        cOut[3] = B + A * F[17] + C * F[19];
        cOut[4] = C;

        cOut[5] = cIn[5] + F[7] * cIn[6] + F[8] * cIn[7] + F[9] * cIn[8] + D * F[7] + E * F[8] + G * F[9];
        cOut[6] = D + E * F[13] + G * F[14];
        cOut[7] = E + D * F[17] + G * F[19];
        cOut[8] = G;

        cOut[9] = H + I * F[13] + J * F[14];
        cOut[10] = I + H * F[17] + J * F[19];
        cOut[11] = J;

        cOut[12] = cIn[12] + F[17] * cIn[10] + F[19] * cIn[13] + (F[17] * cIn[9] + cIn[10] + F[19] * cIn[11]) * F[17] + K * F[19];
        cOut[13] = K;

        cOut[14] = cIn[14];
    }
}
