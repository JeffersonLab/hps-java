package org.hps.recon.tracking.kalman;

// Propagate a charged particle according to the magnetic field map by 4th order Runge Kutta integration.
// Note that the coordinate system is the Kalman-Filter-code system, as that is what the field-map routine
// called here assumes.
public class RungeKutta4 {

    private double h;
    private double h2;
    private double alpha;
    private org.lcsim.geometry.FieldMap fM;

    public RungeKutta4(double Q, double dx, org.lcsim.geometry.FieldMap fM) {
        alpha = Q * 2.99792458e-4; // Q is the charge in units of the proton charge
        h = dx; // Step size in mm
        h2 = h / 2.0;
        this.fM = fM; // Magnetic field map
    }

    double[] integrate(Vec r0, Vec p0, double s) {
        // r0 is the initial point in mm
        // p0 is the initial momentum in GeV/c
        // s is the distance to propagate (approximate to distance dx)
        double[] r = { r0.v[0], r0.v[1], r0.v[2], p0.v[0], p0.v[1], p0.v[2] };
        double[] k1 = new double[6];
        double[] k2 = new double[6];
        double[] k3 = new double[6];
        double[] k4 = new double[6];
        int nStep = (int) (s / h) + 1;
        for (int step = 0; step < nStep; step++) {
            Vec ri = new Vec(r[0], r[1], r[2]);
            double[] pi = { r[3], r[4], r[5] };
            k1 = f(ri, pi);
            Vec r1 = new Vec(r[0] + h2 * k1[0], r[1] + h2 * k1[1], r[2] + h2 * k1[2]);
            double[] p1 = { r[3] + h2 * k1[3], r[4] + h2 * k1[4], r[5] + h2 * k1[5] };
            k2 = f(r1, p1);
            Vec r2 = new Vec(r[0] + h2 * k2[0], r[1] + h2 * k2[1], r[2] + h2 * k2[1]);
            double[] p2 = { r[3] + h2 * k2[3], r[4] + h2 * k2[4], r[5] + h2 * k2[5] };
            k3 = f(r2, p2);
            Vec r3 = new Vec(r[0] + h * k3[0], r[1] + h * k3[1], r[2] + h * k3[2]);
            double[] p3 = { r[3] + h * k3[3], r[4] + h * k3[4], r[5] + h * k3[5] };
            k4 = f(r3, p3);
            for (int i = 0; i < 6; i++) { r[i] = r[i] + h * (k1[i] / 6. + k2[i] / 3. + k3[i] / 3. + k4[i] / 6.); }
        }
        return r;
    }

    private double[] f(Vec x, double[] p) { // Return all the derivatives
        Vec B = KalmanInterface.getField(x, fM);  // This field routine assumes the Kalman-Filter coordinate system.
        double[] d = new double[6];
        double pmag = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
        // System.out.format("P magnitude = %10.7f GeV\n", pmag);
        d[0] = p[0] / pmag; // dx/ds 
        d[1] = p[1] / pmag;
        d[2] = p[2] / pmag;
        d[3] = alpha * (d[1] * B.v[2] - d[2] * B.v[1]); // dp/ds
        d[4] = alpha * (d[2] * B.v[0] - d[0] * B.v[2]);
        d[5] = alpha * (d[0] * B.v[1] - d[1] * B.v[0]);
        // double[] dd = new double[6];
        // for (int i = 0; i < 5; i++) {
        // dd[i] = d[i] * h;
        // }
        // System.out.format(" dr=%10.7f %10.7f %10.7f, dp=%10.7f %10.7f %10.7f\n",
        // dd[0], dd[1], dd[2], dd[3], dd[4], dd[5]);
        return d;
    }

}
