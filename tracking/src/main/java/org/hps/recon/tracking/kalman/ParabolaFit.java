package org.hps.recon.tracking.kalman;

// This simple parabola fit is no longer used.  See LinearHelixFit instead.
class ParabolaFit {
    private SquareMatrix C;
    private Vec a;
    private double chi2;

    ParabolaFit(int N, double[] x, double[] y, double[] s) {
        SquareMatrix A = new SquareMatrix(3);
        Vec B = new Vec(3);
        for (int i = 0; i < N; i++) {
            double w = 1.0 / (s[i] * s[i]);
            A.M[0][0] += w;
            A.M[0][1] += w * x[i];
            A.M[0][2] += w * x[i] * x[i];
            A.M[1][2] += w * x[i] * x[i] * x[i];
            A.M[2][2] += w * x[i] * x[i] * x[i] * x[i];
            B.v[0] += w * y[i];
            B.v[1] += w * x[i] * y[i];
            B.v[2] += w * x[i] * x[i] * y[i];
        }
        A.M[1][0] = A.M[0][1];
        A.M[1][1] = A.M[0][2];
        A.M[2][0] = A.M[0][2];
        A.M[2][1] = A.M[1][2];

        C = A.invert();
        a = B.leftMultiply(C);

        chi2 = 0.;
        for (int i = 0; i < N; i++) {
            double err = (y[i] - evaluate(x[i])) / s[i];
            chi2 += err * err;
        }
    }

    void print() {
        System.out.format("ParabolaFit: chi2=%12.4f, a=%10.7f   b=%10.7f   c=%10.7f\n", chi2, a.v[0], a.v[1], a.v[2]);
        C.print("covariance");
    }

    SquareMatrix covariance() {
        return C;
    }

    Vec solution() {
        return a;
    }

    double evaluate(double x) {
        return a.v[0] + (a.v[1] + a.v[2] * x) * x;
    }

    double chiSquared() {
        return chi2;
    }
}
