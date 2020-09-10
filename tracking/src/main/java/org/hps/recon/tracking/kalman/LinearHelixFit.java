package org.hps.recon.tracking.kalman;

//package kalman;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

class LinearHelixFit { // Simultaneous fit of axial and stereo measurements to a line in the non-bending plane
    // and a parabola in the bending plane

    private DMatrixRMaj  C; // Covariance matrix of solution
    private DMatrixRMaj  a; // Solution vector (line coefficients followed by parabola coefficients
    private double chi2;
    private double[] vpred;

    LinearHelixFit(int N, double[] y, double[] v, double[] s, double[][] delta, double[][] R2, boolean verbose) {
        // N = number of measurement points (detector layers) to fit. This must be no less than 5, with at least 2 axial and 2 stereo
        // y = nominal location of each measurement plane along the beam axis
        // v = measurement value in the detector coordinate system, perpendicular to the strips
        // s = one sigma error estimate on each measurement
        // delta = offset of the detector coordinate system from the global system (minus the nominal y value along the beam axis)
        // R2 = 2nd row of the general rotation from the global system to the local detector system
        // verbose: set true to get lots of debug printout
        double [][] A = new double[5][5];
        double [] B = new double[5];

        for (int i = 0; i < N; i++) {
            double R10 = R2[i][0];
            double R11 = R2[i][1];
            double R12 = R2[i][2];
            double w = 1.0 / (s[i] * s[i]);

            A[0][0] += w * R12 * R12;
            A[0][1] += w * y[i] * R12 * R12;
            A[0][2] += w * R12 * R10;
            A[0][3] += w * y[i] * R12 * R10;
            A[0][4] += w * y[i] * y[i] * R12 * R10;
            A[1][1] += w * y[i] * y[i] * R12 * R12;
            A[1][4] += w * y[i] * y[i] * y[i] * R12 * R10;
            A[2][2] += w * R10 * R10;
            A[2][3] += w * y[i] * R10 * R10;
            A[2][4] += w * y[i] * y[i] * R10 * R10;
            A[3][4] += w * y[i] * y[i] * y[i] * R10 * R10;
            A[4][4] += w * y[i] * y[i] * y[i] * y[i] * R10 * R10;
            double vcorr = (v[i] + R10 * delta[i][0] + R12 * delta[i][2] + R11 * (delta[i][1] - y[i]));
            B[0] += vcorr * R12 * w;
            B[1] += vcorr * y[i] * R12 * w;
            B[2] += vcorr * R10 * w;
            B[3] += vcorr * y[i] * R10 * w;
            B[4] += vcorr * y[i] * y[i] * R10 * w;
        }
        A[1][0] = A[0][1];
        A[1][2] = A[0][3];
        A[1][3] = A[0][4];
        A[2][0] = A[0][2];
        A[2][1] = A[1][2];
        A[3][0] = A[0][3];
        A[3][1] = A[1][3];
        A[3][2] = A[2][3];
        A[3][3] = A[2][4];
        A[4][0] = A[0][4];
        A[4][1] = A[1][4];
        A[4][2] = A[2][4];
        A[4][3] = A[3][4];

        C = new DMatrixRMaj(A);
        DMatrixRMaj V = new DMatrixRMaj(B);
        
        if (verbose) C.print();

        // Do the calculation
        CommonOps_DDRM.invert(C);
        a = new DMatrixRMaj(5);
        CommonOps_DDRM.mult(C,V,a);

        // Add up the chi-squared
        vpred = new double[N];
        chi2 = 0.;
        for (int i = 0; i < N; i++) {
            double R10 = R2[i][0];
            double R11 = R2[i][1];
            double R12 = R2[i][2];
            vpred[i] = R10 * (evaluateParabola(y[i]) - delta[i][0]) + R12 * (evaluateLine(y[i]) - delta[i][2]) + R11 * (y[i] - delta[i][1]);
            //   R10 -0.000146  ePyi -4.094654  deltai0 -22.794523  R12 -0.999999 eLyi 22.174849  deltai2 32.839151  R11 -0.001407  yi 912.877681  deltai1 912.892707
            //System.out.printf("  R10 %f  ePyi %f  deltai0 %f  R12 %f eLyi %f  deltai2 %f  R11 %f  yi %f  deltai1 %f \n", R10, evaluateParabola(y[i]), delta[i][0], R12, evaluateLine(y[i]), delta[i][2], R11, y[i], delta[i][1]);
            double err = (v[i] - vpred[i]) / s[i];
            chi2 += err * err;
        }
    }

    void print(int N, double[] x, double[] y, double[] z, double[] v, double[] s) {
        System.out.format("LinearHelixFit2: parabola a=%10.7f   b=%10.7f   c=%10.7f\n", a.get(2,0), a.get(3,0), a.get(4,0));
        System.out.format("LinearHelixFit2:     line a=%10.7f   b=%10.7f\n", a.get(0,0), a.get(1,0));
        System.out.println("LinearHelixFit2 covariance:");
        C.print();
        System.out.format(
                "LinearHelixFit2: i  xMC   xpred       y           zMC       zpred        v    v predicted   residual   sigmas       chi^2=%8.3f\n",
                chi2);
        for (int i = 0; i < N; i++) {
            double xpred = evaluateParabola(y[i]);
            double zpred = evaluateLine(y[i]);
            System.out.format("        %d   %10.7f %10.7f %10.7f %10.7f %10.7f %10.7f %10.7f %10.7f %10.7f\n", i, x[i], xpred, y[i], z[i],
                    zpred, v[i], vpred[i], v[i] - vpred[i], (v[i] - vpred[i]) / s[i]);
        }
    }

    DMatrixRMaj covariance() {
        return C;
    }

    Vec solution() {
        Vec aV = new Vec(a.unsafe_get(0,0),a.unsafe_get(1,0),a.unsafe_get(2,0),a.unsafe_get(3,0),a.unsafe_get(4,0));
        return aV;
    }

    double evaluateLine(double y) {
        return a.unsafe_get(0,0) + a.unsafe_get(1,0) * y;
    }

    double evaluateParabola(double y) {
        return a.unsafe_get(2,0) + (a.unsafe_get(3,0) + a.unsafe_get(4,0) * y) * y;
    }

    double chiSquared() {
        return chi2;
    }
}
