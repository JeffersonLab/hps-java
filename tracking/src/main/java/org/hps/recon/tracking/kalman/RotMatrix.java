package org.hps.recon.tracking.kalman;

class RotMatrix { // 3 by 3 rotation matrix for the Kalman filter
    double[][] M = null;

    RotMatrix() { // Create a blank matrix
        M = new double[3][3];
    }
    
    // Careful here: a new array is not made
    RotMatrix(double[][] Min) {
        M = Min;
    }

    RotMatrix(Vec u, Vec v, Vec t) { // Transforms from global coordinates to frame with 3-D unit vectors u, v, t
        M = new double[3][3];
        for (int i = 0; i < 3; i++) { // Simply place the vectors u, v, and t along the successive rows of the matrix
            M[0][i] = u.v[i];
            M[1][i] = v.v[i];
            M[2][i] = t.v[i];
        }
    }

    RotMatrix(Vec u1, Vec v1, Vec t1, Vec u2, Vec v2, Vec t2) { // Rotation from frame 1 to frame 2
        M = new double[3][3];
        M[0][0] = u2.dot(u1);
        M[0][1] = u2.dot(v1);
        M[0][2] = u2.dot(t1);
        M[1][0] = M[0][1];
        M[1][1] = v2.dot(v1);
        M[1][2] = v2.dot(t1);
        M[2][0] = M[0][2];
        M[2][1] = M[1][2];
        M[2][2] = t2.dot(t1);
    }

    RotMatrix(double phi, double theta, double psi) {// Create the rotation matrix from Euler angles
        double c1 = Math.cos(phi);
        double c2 = Math.cos(theta);
        double c3 = Math.cos(psi);
        double s1 = Math.sin(phi);
        double s2 = Math.sin(theta);
        double s3 = Math.sin(psi);
        M = new double[3][3];
        M[0][0] = c3 * c1 - c2 * s1 * s3;
        M[0][1] = c3 * s1 + c2 * c1 * s3;
        M[0][2] = s3 * s2;
        M[1][0] = -s3 * c1 - c2 * s1 * c3;
        M[1][1] = -s3 * s1 + c2 * c1 * c3;
        M[1][2] = c3 * s2;
        M[2][0] = s2 * s1;
        M[2][1] = -s2 * c1;
        M[2][2] = c2;
    }

    RotMatrix(double phi) { // Simple rotation only about z
        double c1 = Math.cos(phi);
        double s1 = Math.sin(phi);
        M = new double[3][3];
        M[0][0] = c1;
        M[0][1] = s1;
        M[1][0] = -s1;
        M[1][1] = c1;
        M[2][2] = 1.0;
    }

    RotMatrix(Vec k, double theta) { // Rodrigues' rotation formula
        // k is a unit vector defining the axis of rotation
        // theta is the angle of rotation counterclockwise about that axis
        double[][] K = new double[3][3];
        K[0][0] = 0.;
        K[0][1] = -k.v[2];
        K[0][2] = k.v[1];
        K[1][0] = k.v[2];
        K[1][1] = 0.;
        K[1][2] = -k.v[0];
        K[2][0] = -k.v[1];
        K[2][1] = k.v[0];
        K[2][2] = 0;
        double ct = Math.cos(theta);
        double st = Math.sin(theta);
        M = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double K2 = 0.;
                for (int n = 0; n < 3; n++) { K2 += K[i][n] * K[n][j]; }
                if (i == j) M[i][j] = 1.0;
                else M[i][j] = 0.0;
                M[i][j] += st * K[i][j] + (1 - ct) * K2;
            }
        }
    }

    RotMatrix copy() {
        RotMatrix Rnew = new RotMatrix();
        for (int i = 0; i < 3; i++) { 
            for (int j = 0; j < 3; j++) { 
                Rnew.M[i][j] = M[i][j]; 
            } 
        }
        return Rnew;
    }

    RotMatrix invert() {
        RotMatrix mInv = new RotMatrix();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mInv.M[i][j] = M[j][i]; // The inverse is the transpose
            }
        }
        return mInv;
    }

    RotMatrix multiply(RotMatrix R2) { // Multiply one rotation matrix by another
        RotMatrix R3 = new RotMatrix();
        for (int i = 0; i < 3; i++) { 
            for (int j = 0; j < 3; j++) { 
                for (int k = 0; k < 3; k++) { 
                    R3.M[i][j] += M[i][k] * R2.M[k][j]; 
                }
            } 
        }
        return R3;
    }

    void print(String s) {
        System.out.format("The 3 by 3 rotation matrix %s:\n", s);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) { 
                System.out.format("  %10.8f", M[i][j]); 
            }
            System.out.format("\n");
        }
    }

    Vec rotate(Vec V) { // Use the matrix to rotate a 3-D vector
        Vec Vp = new Vec(0., 0., 0.);
        for (int i = 0; i < 3; i++) { 
            for (int j = 0; j < 3; j++) { 
                Vp.v[i] += M[i][j] * V.v[j]; 
            } 
        }
        return Vp;
    }

    Vec inverseRotate(Vec V) { // Use the matrix to rotate a 3-D vector in the opposite sense, using the
                               // inverse (i.e. transpose) of the rotation matrix
        Vec Vp = new Vec(0., 0., 0.);
        for (int i = 0; i < 3; i++) { 
            for (int j = 0; j < 3; j++) { 
                Vp.v[i] += M[j][i] * V.v[j]; 
            } 
        }
        return Vp;
    }

}
