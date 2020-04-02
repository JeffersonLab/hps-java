package org.hps.recon.tracking.kalman;

class Vec { // N-vector for the Kalman filter
    double[] v;
    int N;

    Vec(int N, double[] vin) {
        v = new double[N];
        for (int i = 0; i < N; i++) {
            v[i] = vin[i];
        }
        this.N = N;
    }

    Vec(double v0, double v1, double v2, double v3, double v4) {
        v = new double[5];
        N = 5;
        v[0] = v0;
        v[1] = v1;
        v[2] = v2;
        v[3] = v3;
        v[4] = v4;
    }

    Vec(double x, double y, double z) {
        v = new double[3];
        N = 3;
        v[0] = x;
        v[1] = y;
        v[2] = z;
    }

    Vec(int N) {
        v = new double[N];
        this.N = N;
    }

    double mag() {
        double val = 0.;
        for (int i = 0; i < N; i++) {
            val += v[i] * v[i];
        }
        return Math.sqrt(val);
    }

    void print(String s) {
        System.out.format("    Vector-%d %s: ", N, s);
        for (int i = 0; i < N; i++) {
            System.out.format(" %11.7f", v[i]);
        }
        System.out.format("\n");
    }

    String string() {
        String s = String.format("%10.5f %10.5f %10.5f", v[0], v[1], v[2]);
        return s;
    }
    
    Vec copy() {
        return new Vec(N, v);
    }

    Vec cross(Vec y) { // Warning, this only makes sense for 3-vectors!
        if (N==3) {
            return new Vec(v[1] * y.v[2] - v[2] * y.v[1], v[2] * y.v[0] - v[0] * y.v[2], v[0] * y.v[1] - v[1] * y.v[0]);
        } else return null;
    }

    Vec unitVec() {
        double n = mag();
        double[] t = new double[N];
        for (int i = 0; i < N; i++)
            t[i] = v[i] / n;
        return new Vec(N, t);
    }

    // For this version you supply the magnitude, in case it is already calculated
    Vec unitVec(double n) {
        double[] t = new double[N];
        for (int i = 0; i < N; i++)
            t[i] = v[i] / n;
        return new Vec(N, t);
    }

    Vec leftMultiply(SquareMatrix G) {
        double[] R = new double[N];
        if (G.N != N) {
            System.out.format("Vec.leftMultiply: matrix size mismatch, Vec=%d, Matrix=%d\n", N, G.N);
            return new Vec(N, R);
        }

        // print("Vec5.leftMultiply");
        // G.print("Vec5.leftMultiply");

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                R[i] += G.M[i][j] * v[j];
            }
        }

        return new Vec(N, R);
    }

    SquareMatrix product(Vec v2) {
        SquareMatrix R = new SquareMatrix(N);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                R.M[i][j] = v[i] * v2.v[j];
            }
        }
        return R;
    }

    double dot(Vec v2) {
        double R = 0.;
        for (int i = 0; i < N; i++) {
            R += v2.v[i] * v[i];
        }
        return R;
    }

    Vec sum(Vec v2) {
        Vec R = new Vec(N);
        for (int i = 0; i < N; i++)
            R.v[i] = v[i] + v2.v[i];
        return R;
    }

    Vec dif(Vec v2) {
        Vec R = new Vec(N);
        for (int i = 0; i < N; i++)
            R.v[i] = v[i] - v2.v[i];
        return R;
    }

    Vec scale(double s) {
        double[] R = new double[N];
        for (int i = 0; i < N; i++) {
            R[i] = v[i] * s;
        }
        return new Vec(N, R);
    }

    boolean isNaN() {
        for (int i = 0; i<N; i++) {
            if (Double.isNaN(v[i])) {
                return true;                
            }
        }
        return false;
    }
}
