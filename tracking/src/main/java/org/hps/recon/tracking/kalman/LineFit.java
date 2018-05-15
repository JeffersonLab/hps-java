package org.hps.recon.tracking.kalman;

// This simple line fit is no longer used.  See LinearHelixFit instead.
class LineFit {
    private double[][] C;
    private double inter;
    private double slp;
    private double chi2;

    LineFit(int N, double[] x, double[] y, double[] s) {
        double sum = 0.;
        double sumx = 0.;
        double sumx2 = 0.;
        double sumxy = 0.;
        double sumy = 0.;
        for (int i = 0; i < N; i++) {
            double w = 1.0 / (s[i] * s[i]);
            sum += w;
            sumx += w * x[i];
            sumx2 += w * x[i] * x[i];
            sumxy += w * x[i] * y[i];
            sumy += w * y[i];
        }
        double D = sum * sumx2 - sumx * sumx;
        C = new double[2][2];
        if (D != 0.) {
            C[0][0] = sumx2 / D;
            C[0][1] = -sumx / D;
            C[1][0] = C[0][1];
            C[1][1] = sum / D;
        } else {
            System.out.format("LineFit: singular matrix. N=%d\n", N);
        }
        inter = C[0][0] * sumy + C[0][1] * sumxy;
        slp = C[1][0] * sumy + C[1][1] * sumxy;

        chi2 = 0.;
        for (int i = 0; i < N; i++) {
            double err = (y[i] - evaluate(x[i])) / s[i];
            chi2 += err * err;
        }
    }

    void print() {
        System.out.format("LineFit: intercept=%10.6f, slope=%10.6f, chi**2=%12.4e\n", inter, slp, chi2);
        System.out.format("       Covariance=  %10.6f   %10.6f\n", C[0][0], C[0][1]);
        System.out.format("                    %10.6f   %10.6f\n", C[1][0], C[1][1]);
    }

    double[][] covariance() {
        return C;
    }

    double slope() {
        return slp;
    }

    double intercept() {
        return inter;
    }

    double evaluate(double x) {
        return inter + slp * x;
    }

    double chiSquared() {
        return chi2;
    }
}
