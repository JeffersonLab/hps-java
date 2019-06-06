package org.hps.recon.tracking.ztrack;

import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static java.lang.Math.sqrt;

/**
 * A Track representation using physical properties: position four-momentum
 * charge
 *
 * Intersection code taken from Paul Avery's writeup
 * http://www.phys.ufl.edu/~avery/fitting/transport_2007feb.doc
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class PhysicalTrack {
//TODO supply enums for the track parameters...
//FIXME do I even need the parameters array?
// position in cm
// energy in GeV
// magnetic field in Tesla
//    

    private double[] _parameters = new double[7];
    private CartesianThreeVector _pos;
    private CartesianThreeVector _mom;
    private int _charge;
    private boolean _debug = false;

    static int max_iterations = 10;
    static double tolerance = 1e-4;
//x, y, z, px, py, pz, E, q

    public PhysicalTrack(double[] pos, double[] mom, double E, int q) {
        _parameters[0] = pos[0]; //x
        _parameters[1] = pos[1]; //y
        _parameters[2] = pos[2]; //z
        _pos = new CartesianThreeVector(pos);
        _parameters[3] = mom[0]; //px
        _parameters[4] = mom[1]; //py
        _parameters[5] = mom[2]; //pz
        _mom = new CartesianThreeVector(mom);
        _parameters[6] = E; // Energy
        _charge = q; // charge
    }

    public PhysicalTrack(CartesianThreeVector pos, CartesianThreeVector mom, double E, int q) {
        _pos = new CartesianThreeVector(pos);
        _mom = new CartesianThreeVector(mom);
        _parameters[0] = pos.x(); //x
        _parameters[1] = pos.y(); //y
        _parameters[2] = pos.z(); //z
        _parameters[3] = mom.x(); //px
        _parameters[4] = mom.y(); //py
        _parameters[5] = mom.z(); //pz
        _parameters[6] = E; // Energy
        _charge = q; // charge
    }

    public PhysicalTrack(PhysicalTrack t) {
        _pos = new CartesianThreeVector(t._pos);
        _mom = new CartesianThreeVector(t._mom);
        _parameters[0] = t._pos.x(); //x
        _parameters[1] = t._pos.y(); //y
        _parameters[2] = t._pos.z(); //z
        _parameters[3] = t._mom.x(); //px
        _parameters[4] = t._mom.y(); //py
        _parameters[5] = t._mom.z(); //pz
        _parameters[6] = t.energy(); // Energy
        _charge = t.charge(); // charge 
    }

    public CartesianThreeVector position() {
        return new CartesianThreeVector(_pos);
    }

    public CartesianThreeVector momentum() {
        return new CartesianThreeVector(_mom);
    }

    public int charge() {
        return _charge;
    }

    public double energy() {
        return _parameters[6];
    }

    //TODO flesh out setters and getters
    public CartesianThreeVector pointOnPath(double s, CartesianThreeVector B) {
        /*
         * Get point on the trajectory at path length s in an arbitrarily oriented
         * but constant magnetic field h
         */
        double bMag = B.magnitude();
        if (B.magnitude() != 0) {  // along the helix
            double a = BFAC * bMag * _charge;
            double p = _mom.magnitude();
            double rho = a / p;
            double rhoS = rho * s;
            CartesianThreeVector h = B.unitVector();
            CartesianThreeVector a1 = _mom.times(sin(rhoS) / a);
            CartesianThreeVector a2 = (_mom.cross(h)).times((1 - cos(rhoS)) / a);
            CartesianThreeVector a3 = h.times((_mom.dot(h) / p) * (s - sin(rhoS) / rho));
            CartesianThreeVector x = _pos.plus(a1);
            x.minusEquals(a2);
            x.plusEquals(a3);
            return x;
        } else // along a straight line
        {
            return new CartesianThreeVector(_pos.plus(_mom.unitVector().times(s)));
        }
    }

    public CartesianThreeVector momentumOnPath(double s, CartesianThreeVector B) {
        /*
         * Get the momentum along the trajectory at path length s 
         * in an arbitrarily oriented but constant magnetic field
         */
        double bMag = B.magnitude();
        if (B.magnitude() != 0) { // along the helix
            double a = BFAC * bMag * _charge;
            double p = _mom.magnitude();
            double rho = a / p;
            double rhoS = rho * s;
            CartesianThreeVector h = B.unitVector();
            CartesianThreeVector momentum = _mom.times(cos(rhoS));
            CartesianThreeVector a2 = _mom.cross(h.times(sin(rhoS)));
            CartesianThreeVector a3 = h.times(_mom.dot(h) * (1 - cos(rhoS)));
            momentum.minusEquals(a2);
            momentum.plusEquals(a3);
            return momentum;
        } else { // along a straight line
            return new CartesianThreeVector(_mom);
        }
    }

    public void moveTo(double s, CartesianThreeVector B) {
        _pos = pointOnPath(s, B);
        _mom = momentumOnPath(s, B);
        _parameters[0] = _pos.x(); //x
        _parameters[1] = _pos.y(); //y
        _parameters[2] = _pos.z(); //z
        _parameters[3] = _mom.x(); //px
        _parameters[4] = _mom.y(); //py
        _parameters[5] = _mom.z(); //pz
    }

    public double approximatePathLengthToPlane(CartesianThreeVector xp, CartesianThreeVector eta, CartesianThreeVector H) {
        /*
         * Find the approximate path length to the plane defined by
         * xp: point on the plane
         * eta: unit normal vector of the plane
         * in an arbitrarily oriented but constant magnetic field H.
         * Over a sufficiently short path the equations can be accurately 
         * represented to second order in s 
         */
        //TODO handle straight track (i.e. B=0)
        //TODO handle two cases where the indicial equation can be solved analytically
        // This occurs when B is either perpendicular or parallel to eta.
        //
        double bMag = H.magnitude();
        CartesianThreeVector delta = _pos.minus(xp);
        double p = _mom.magnitude();
        if (H.magnitude() != 0) { // along the helix
            CartesianThreeVector h = H.unitVector();
            double a = BFAC * bMag * _charge;

            double rho = a / p;
            double A = eta.dot(_mom.cross(h)) * (rho / (2 * p));
            double B = eta.dot(_mom) / p;
            double C = eta.dot(delta);
            double t = B * B - 4 * A * C;
            if (t < 0) {
                System.out.println(" getPathLengthToPlaneApprox ERROR t is negative (" + t + ")!");
                System.out.println(" p " + p + " rho " + rho + " a " + a + " A " + A + " B " + B + " C " + C);
                System.out.println(" track params: " + toString());
                System.out.println(" xp " + xp.toString());
                System.out.println(" eta " + eta.toString());
                System.out.println(" h " + h.toString());
                //throw new RuntimeException("negative radical");
                return -99999.;
            }
            double var = (-B - signum(B) * sqrt(t));
            double root1 = var / (2 * A);
            double root2 = 2 * C / var;

            // choose the shortest path
            // TODO implement functionality to allow only positive steps
            double root = abs(root1) <= abs(root2) ? root1 : root2;
            if (_debug) {
                System.out.println(" getPathLengthToPlaneApprox ");
                System.out.println(" " + toString());
                System.out.println(" xp " + xp.toString());
                System.out.println(" eta " + eta.toString());
                System.out.println(" h " + h.toString());
                System.out.println(" p " + p + " rho " + rho + " t " + t + " A " + A + " B " + B + " C " + C);
                System.out.println(" root1 " + root1 + " root2 " + root2 + " -> root " + root);
            }
            return root;
        } else // along a straight line
        {
            return -p * (delta.dot(eta)) / _mom.dot(eta);
        }

    }

    public IntersectionStatus planeIntersection(CartesianThreeVector xp, CartesianThreeVector eta, CartesianThreeVector H) {
        /*
         * Find the point of intersection of this track with the plane defined by
         * xp: point on the plane
         * eta: unit vector of the plane
         * in an arbitrarily oriented but constant magnetic field H.
         * Note that this requires one to be in the vicinity of the plane, 
         * i.e. close enough that the parabolic approximation
         * is valid.
         */
        //TODO implement closed solutions in case of H=0 (i.e. straight line) or H perpendicular or parallel to the plane
        int iteration = 0;
        double s_total = 0.;
        double step = 9999999.9;
        double oldstep = step;
        // create a new track to propagate...
        PhysicalTrack t = new PhysicalTrack(this);
        while (iteration <= max_iterations && abs(step) > tolerance) {
            ++iteration;
            if (_debug) {
                System.out.printf("%s: Iteration %d\n", this.getClass().getSimpleName(), iteration);
                System.out.printf("%s: s_total %f prev_step %.3f current trk params: %s \n",
                        this.getClass().getSimpleName(), s_total, step, t.toString());
            }

//            // check that the track is not looping
//            if (trk.goingForward()) {
            // Start by approximating the path length to the point
            step = t.approximatePathLengthToPlane(xp, eta, H);
            //TODO figure out how to determine whether the plane is not reachable
            if (step == -99999.) {
                return new IntersectionStatus(false, null, 0., iteration);
            }
            if (_debug) {
                System.out.printf("%s: path length step s=%.3f\n", this.getClass().getSimpleName(), step);
            }

            // propagate the track
            t.moveTo(step, H);

            if (_debug) {
                System.out.printf("%s: updated track params: [%s]\n", this.getClass().getSimpleName(), t.toString());
            }
            s_total += step;
        }

        if (_debug) {
            System.out.printf("%s: final total_s=%f with final step %f after %d iterations gave track params: %s\n",
                    this.getClass().getSimpleName(), s_total, step, iteration, t.toString());
        }
        if (iteration >= max_iterations) {
            return new IntersectionStatus(false, null, 0., iteration);
        } else {
            return new IntersectionStatus(true, t, s_total, iteration);
        }
    }

    // Physical constants 
    // Note that for LCIO
    // distance in mm
    // energy in GeV
    // field in Tesla
    /**
     * Speed of light. 2.99792458e11 mm/sec.
     */
    public static double CLIGHT = 2.99792458e11;

    /**
     * Factor connecting curvature to momentum. C = 1/Rc = BFAC * B * q/pT where
     * B = magnetic field in Tesla, q = charge in natural units (electron has
     * -1) and pT = transverse momentum in GeV/c Numerical value is 1.0e-13 *
     * CLIGHT .
     */
    public static double BFAC = 1.0e-13 * CLIGHT;

    public String toString() {
        return "PhysicalTrack: \n position: " + _pos.toString() + " \n momentum: " + _mom.toString() + " \n Energy: " + _parameters[6] + " charge: " + _charge;
    }
}
