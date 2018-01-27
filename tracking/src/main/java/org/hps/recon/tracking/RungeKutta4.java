package org.hps.recon.tracking;

import org.lcsim.constants.Constants;
import org.lcsim.geometry.FieldMap;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

public class RungeKutta4 {
    private double h;
    private double h2;
    private int Q;
    private FieldMap fM;

    public RungeKutta4(int inputQ, double dx, FieldMap fM) {
        h = dx; // Step size in mm
        h2 = h / 2.0;
        this.fM = fM; // Magnetic field map
        this.Q = inputQ;
    }

    double[] integrate(Hep3Vector r0, Hep3Vector p0, double s) {
        // r0 is the initial point in mm
        // p0 is the initial momentum in GeV/c
        // s is the distance to propagate (approximate to distance dx)
        double[] r = { r0.x(), r0.y(), r0.z(), p0.x(), p0.y(), p0.z() };
        double[] k1 = new double[6];
        double[] k2 = new double[6];
        double[] k3 = new double[6];
        double[] k4 = new double[6];
        int nStep = (int) (s / h) + 1;
        for (int step = 0; step < nStep; step++) {
            Hep3Vector ri = new BasicHep3Vector(r[0], r[1], r[2]);
            Hep3Vector pi = new BasicHep3Vector(r[3], r[4], r[5]);
            k1 = f(ri, pi);
            Hep3Vector r1 = new BasicHep3Vector(r[0] + h2 * k1[0], r[1] + h2 * k1[1], r[2] + h2 * k1[2]);
            BasicHep3Vector p1 = new BasicHep3Vector(r[3] + h2 * k1[3], r[4] + h2 * k1[4], r[5] + h2 * k1[5]);
            k2 = f(r1, p1);
            Hep3Vector r2 = new BasicHep3Vector(r[0] + h2 * k2[0], r[1] + h2 * k2[1], r[2] + h2 * k2[1]);
            BasicHep3Vector p2 = new BasicHep3Vector(r[3] + h2 * k2[3], r[4] + h2 * k2[4], r[5] + h2 * k2[5]);
            k3 = f(r2, p2);
            Hep3Vector r3 = new BasicHep3Vector(r[0] + h * k3[0], r[1] + h * k3[1], r[2] + h * k3[2]);
            BasicHep3Vector p3 = new BasicHep3Vector(r[3] + h * k3[3], r[4] + h * k3[4], r[5] + h * k3[5]);
            k4 = f(r3, p3);
            for (int i = 0; i < 6; i++) {
                r[i] = r[i] + h * (k1[i] / 6. + k2[i] / 3. + k3[i] / 3. + k4[i] / 6.);
            }
        }
        return r;
    }

    private double[] f(Hep3Vector x, Hep3Vector p) { // Return all the derivatives
        //TODO: ensure field is in correct coord system
        Hep3Vector B = fM.getField(x);

        double[] d = new double[6];
        p = VecOp.unit(p);
        Hep3Vector dp = VecOp.mult(Constants.fieldConversion * Q, VecOp.cross(p, B));
        d[0] = p.x(); // dx/ds (assuming the electron moves at the speed of light)
        d[1] = p.y();
        d[2] = p.z();
        d[3] = dp.x();
        d[4] = dp.y();
        d[5] = dp.z();

        return d;
    }

}