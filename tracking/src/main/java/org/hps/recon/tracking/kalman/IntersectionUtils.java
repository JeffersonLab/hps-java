package org.hps.recon.tracking.kalman;

//import org.lcsim.detector.solids.Plane3D;
import org.hps.recon.tracking.WTrack;
import org.lcsim.geometry.FieldMap;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

public class IntersectionUtils {
    // Coordinates: the beam is ~ in the y direction
    //              the B field is ~ along the +z direction
    //              Si detectors are ~ in the xz plane

    double h = 1.0;
    private FieldMap fM = null;
    private boolean debug = false;

    public IntersectionUtils() {

    }

    public IntersectionUtils(FieldMap input, double tolerance) {
        fM = input;
        h = tolerance;
    }

    public void setFieldmap(FieldMap input) {
        fM = input;
    }

    public void setTolerance(double tol) {
        h = tol;
    }

    public void setDebug(boolean input) {
        debug = input;
    }

    // Runge Kutta integration extrapolation to a plane through a non-uniform field
    // When close to the plane, then a helix is used to find the exact intersection
    public Hep3Vector rkIntersect(Hep3Vector ptOnPlane, Hep3Vector normal, Hep3Vector X0, Hep3Vector P0, double Q) {
        // ptOnPlane, normal: definition of the plane to which to extrapolate
        // X0 3-D starting point for the extrapolation
        // P0 3-momentum at the start of the extrapolation
        // Q sign of the particle charge (+1 or -1)       
        // the function return value is the 3-D intersection point

        // Find the straight-line distance to the plane for an initial guess at the distance
        double distance = Math.abs(VecOp.dot(VecOp.sub(X0, ptOnPlane), normal)) / VecOp.dot(VecOp.unit(P0), normal);
        if (distance < 0.) {
            if (debug)
                System.out.format("HelixPlaneIntersect:rkIntersect: there will be no intersection. distance=%12.5f\n", distance);
            return X0;
        }

        RungeKutta4 rk4 = new RungeKutta4(Q, h, fM);
        double[] d = rk4.integrate(X0, P0, distance);
        Hep3Vector X1 = new BasicHep3Vector(d[0], d[1], d[2]);
        Hep3Vector P1 = new BasicHep3Vector(d[3], d[4], d[5]);

        Hep3Vector B = fM.getField(X1);
        WTrack wtrk = new WTrack(B.y());
        double[] params = { P1.x(), P1.y(), P1.z(), P1.magnitude(), X1.x(), X1.y(), X1.z() };
        wtrk.setTrackParameters(params);
        return wtrk.getHelixAndPlaneIntercept(ptOnPlane, normal, VecOp.unit(B));

    }

}
