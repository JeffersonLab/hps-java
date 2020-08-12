/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.apache.commons.math.util.FastMath;

import org.lcsim.constants.Constants;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HitUtils;

//ejml
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.dense.fixed.CommonOps_DDF3;

/**
 * Track representation based on paper
 * Paul Avery, CBX 98-39, June 9, 1998
 *  
 * Used primarily for the algorithm to intersect a helix with a generic plane in space.
 *  
 * @author phansson <phansson@slac.stanford.edu>
 * @author pf <pbutti@slac.stanford.edu> #usage of ejml in internal methods
 */
public class WTrack {

    private double[] _parameters = new double[7];
    public HelicalTrackFit _htf = null;
    private double _bfield;
    private double _a;
    private double _rho;
    private double _p0_mag;
    private boolean _debug = false;
    private final int max_iterations_intercept = 10;
    private final double epsilon_intercept = 1e-4;

    /**
     * Constructor. Assumes that b-field is in detector z direction. 
     * 
     * @param track @ref{HelicalTrackFit} 
     * @param bfield value and sign of magnetic field
     */
    public WTrack(HelicalTrackFit track, double bfield) {
        _htf = track;
        //_bfield = flip ? -1.0 * bfield : bfield; // flip if needed
        _bfield = bfield;
        _a = -1 * Constants.fieldConversion * _bfield * Math.signum(track.R());
        double p = track.p(Math.abs(_bfield));
        double theta = Math.PI / 2.0 - FastMath.atan(track.slope());
        double phi = track.phi0();
        _parameters[0] = p * FastMath.cos(phi) * FastMath.sin(theta);
        _parameters[1] = p * FastMath.sin(phi) * FastMath.sin(theta);
        _parameters[2] = p * FastMath.cos(theta);
        _p0_mag = FastMath.sqrt(_parameters[0]*_parameters[0] + _parameters[1]*_parameters[1] + _parameters[2]*_parameters[2]);
        _rho = _a / _p0_mag;
        _parameters[3] = FastMath.sqrt(_parameters[0] * _parameters[0] + _parameters[1] * _parameters[1] + _parameters[2] * _parameters[2]);
        _parameters[4] = -1 * track.dca() * FastMath.sin(phi); // x0
        _parameters[5] = track.dca() * FastMath.cos(phi); // y0
        _parameters[6] = track.z0(); // z0
        if (_debug) {
            System.out.printf("%s: WTrack initialized (p=%f,bfield=%f,theta=%f,phi=%f) from HelicalTrackFit:\n%s: %s\n", this.getClass().getSimpleName(), p, _bfield, theta, phi, this.getClass().getSimpleName(), this.toString());
        }
    }

    /**
     * Copy constructor
     * 
     * @param trk
     */
    public WTrack(WTrack trk) {
        _bfield = trk._bfield;
        _a = trk._a;
        _parameters = trk._parameters;
        _htf = trk._htf;
        _debug = trk._debug;
    }

    public void setTrackParameters(double[] params) {
        _parameters = params;
    }

    public double[] getParameters() {
        return _parameters;
    }

    private boolean goingForward() {
        // assuming the track should go in the x-direction --> not very general -> FIX THIS!?
        return getP0().x() > 0 ? true : false;
    }

    public double a() {
        return _a;

    }

    private int getCharge() {
        return (int) Math.signum(_htf.R());
    }

    public Hep3Vector getP0() {
        return (new BasicHep3Vector(_parameters[0], _parameters[1], _parameters[2]));
    }
    
    public DMatrix3 getP0_ejml() {
        return (new DMatrix3(_parameters[0], _parameters[1], _parameters[2]));
    }

    public Hep3Vector getX0() {
        return (new BasicHep3Vector(_parameters[4], _parameters[5], _parameters[6]));
    }
    
    public DMatrix3 getX0_ejml() {
        return (new DMatrix3(_parameters[4], _parameters[5], _parameters[6]));
    }
    
    public String paramsToString() {
        String str = "";
        for (int i = 0; i < 7; ++i)
            str += _parameters[i] + ", ";
        return str;
    }
    
    public String toString() {
        
        String str = "WTrack params [" + paramsToString() + "]";
        if (this._htf != null) {
            str += "\n with corresponding HelicalTrackFit:\n";
            str += this._htf.toString();
        }
        return str;
    }
    
    private Hep3Vector getMomentumOnHelix(double s) {
        WTrack track = this;
        double a = track.a();
        Hep3Vector p0 = track.getP0();
        double rho = a / p0.magnitude();
        double px = p0.x() * FastMath.cos(rho * s) - p0.y() * FastMath.sin(rho * s);
        double py = p0.y() * FastMath.cos(rho * s) + p0.x() * FastMath.sin(rho * s);
        double pz = p0.z();
        return (new BasicHep3Vector(px, py, pz));
    }
    
    private DMatrix3 getMomentumOnHelix_ejml(double s) {
        double p0_mag = FastMath.sqrt(_parameters[0]*_parameters[0] + _parameters[1]*_parameters[1] + _parameters[2]*_parameters[2]);
        double rho = _a / p0_mag;
        double csrho = FastMath.cos(rho * s);
        double ssrho = FastMath.sqrt(1-csrho*csrho);
        double px = _parameters[0] * csrho - _parameters[1] * ssrho;
        double py = _parameters[1] * csrho + _parameters[0] * ssrho;
        double pz = _parameters[2];
        return (new DMatrix3(px,py,pz));
    }

    private Hep3Vector getPointOnHelix(double s) {
        WTrack track = this;
        double a = track.a();
        Hep3Vector p0 = track.getP0();
        Hep3Vector x0 = track.getX0();
        double rho = a / p0.magnitude();
        double x = x0.x() + p0.x() / a * Math.sin(rho * s) - p0.y() / a * (1 - Math.cos(rho * s));
        double y = x0.y() + p0.y() / a * Math.sin(rho * s) + p0.x() / a * (1 - Math.cos(rho * s));
        double z = x0.z() + p0.z() / p0.magnitude() * s;
        return (new BasicHep3Vector(x, y, z));
    }

    private DMatrix3 getPointOnHelix_ejml(double s) {
        
        
        double ssrho = FastMath.sin(_rho * s);
        double csrho = FastMath.sqrt(1-ssrho*ssrho);
        double x = _parameters[4] + _parameters[0] / _a * ssrho - _parameters[1] / _a * (1 - csrho);
        double y = _parameters[5] + _parameters[1] / _a * ssrho + _parameters[0] / _a * (1 - csrho);
        double z = _parameters[6] + _parameters[2] / _p0_mag * s;
        
        return (new DMatrix3(x, y, z));
    }
        

    private double getPathLengthToPlaneApprox(Hep3Vector xp, Hep3Vector eta, Hep3Vector h) {
        /*
         * Find the approximate path length to the point xp in arbitrary oriented, constant
         * magnetic field with unit vector h
         */
        WTrack track = this;
        double a = track.a();
        Hep3Vector p0 = track.getP0();
        Hep3Vector x0 = track.getX0();
        double p = p0.magnitude();
        double rho = a / p;
        double A = VecOp.dot(eta, VecOp.cross(p0, h)) / p * 0.5 * rho;
        double B = VecOp.dot(p0, eta) / p;
        double C = VecOp.dot(VecOp.sub(x0, xp), eta);
        double t = B * B - 4 * A * C;
        if (t < 0) {
            if (_debug) {
                System.out.println(" getPathLengthToPlaneApprox ERROR t is negative (" + t + ")!");
                System.out.println(" p " + p + " rho " + rho + " a " + a + " A " + A + " B " + B + " C " + C);
                System.out.println(" track params: " + track.paramsToString());
                System.out.println(" xp " + xp.toString());
                System.out.println(" eta " + eta.toString());
                System.out.println(" h " + h.toString());
            }
            return Double.NaN;

        }
        double root1 = (-B + Math.sqrt(t)) / (2 * A);
        double root2 = (-B - Math.sqrt(t)) / (2 * A);

        // choose the smallest positive solution
        double root = Math.abs(root1) <= Math.abs(root2) ? root1 : root2;

        if (_debug) {
            System.out.println(" getPathLengthToPlaneApprox ");
            System.out.println(" " + track.paramsToString());
            System.out.println(" xp " + xp.toString());
            System.out.println(" eta " + eta.toString());
            System.out.println(" h " + h.toString());
            System.out.println(" p " + p + " rho " + rho + " t " + t + " A " + A + " B " + B + " C " + C);
            System.out.println(" root1 " + root1 + " root2 " + root2 + " -> root " + root);
        }
        return root;

    }
    
    private double getPathLengthToPlaneApprox_ejml(DMatrix3 xp, DMatrix3 eta, DMatrix3 h) {
        /*
         * Find the approximate path length to the point xp in arbitrary oriented, constant
         * magnetic field with unit vector h
         */
        
        DMatrix3 p0 = getP0_ejml();
        DMatrix3 x0 = getX0_ejml();
                
        //double A = VecOp.dot(eta, VecOp.cross(p0, h)) / _p0_mag * 0.5 * _rho;
        //double B = VecOp.dot(p0, eta) / _p0_mag;
        //double C = VecOp.dot(VecOp.sub(x0, xp), eta);
        
        DMatrix3x3 p0x = new DMatrix3x3();
        p0x.set(0, -p0.get(2,0), p0.get(1,0), p0.get(2,0),0., -p0.get(0,0), -p0.get(1,0),p0.get(0,0),0.);

        DMatrix3 p0xh = new DMatrix3();
        CommonOps_DDF3.mult(p0x,h,p0xh);
        
        double A = CommonOps_DDF3.dot(eta,p0xh) / _p0_mag * 0.5 * _rho;
        double B = CommonOps_DDF3.dot(p0,eta) / _p0_mag;
        
        //I can safely modify X0 since it's a copy
        CommonOps_DDF3.subtractEquals(x0,xp);
        double C = CommonOps_DDF3.dot(x0,eta);
                
        double t = B * B - 4 * A * C;
        if (t < 0) {
            if (_debug) {
                System.out.println(" getPathLengthToPlaneApprox ERROR t is negative (" + t + ")!");
                System.out.println(" p " + _p0_mag + " rho " + _rho + " a " + _a + " A " + A + " B " + B + " C " + C);
                System.out.println(" track params: " + paramsToString());
                System.out.println(" xp ");
                xp.print();
                System.out.println(" eta ");
                eta.print();
                System.out.println(" h ");
                h.print();
            }
            return Double.NaN;

        }
        double root1 = (-B + Math.sqrt(t)) / (2 * A);
        double root2 = (-B - Math.sqrt(t)) / (2 * A);
        
        // choose the smallest positive solution
        double root = Math.abs(root1) <= Math.abs(root2) ? root1 : root2;

        if (_debug) {
            System.out.println(" getPathLengthToPlaneApprox ");
            System.out.println(" " + paramsToString());
            //System.out.println(" xp " + xp.toString());
            //System.out.println(" eta " + eta.toString());
            //System.out.println(" h " + h.toString());
            System.out.println(" p " + _p0_mag + " rho " + _rho + " t " + t + " A " + A + " B " + B + " C " + C);
            System.out.println(" root1 " + root1 + " root2 " + root2 + " -> root " + root);
        }
        return root;

    }



    /**
     * Get point on helix at path length s in arbitrary oriented, constant magnetic field with unit vector h
     * @param s - path length
     * @param h - magnetic field unit vector
     * @return get a 3D point along the helix
     */
    private Hep3Vector getPointOnHelix(double s, Hep3Vector h) {
        WTrack track = this;
        double a = track.a();
        Hep3Vector p0 = track.getP0();
        double p = p0.magnitude();
        Hep3Vector x0 = track.getX0();
        double rho = a / p0.magnitude();
        double srho = s * rho;
        Hep3Vector a1 = VecOp.mult(1 / a * Math.sin(srho), p0);
        Hep3Vector a2 = VecOp.mult(1 / a * (1 - Math.cos(srho)), VecOp.cross(p0, h));
        Hep3Vector a3 = VecOp.mult(VecOp.dot(p0, h) / p, h);
        Hep3Vector a4 = VecOp.mult(s - Math.sin(srho) / rho, a3);
        Hep3Vector x = VecOp.add(x0, a1);
        x = VecOp.sub(x, a2);
        x = VecOp.add(x, a4);
        return x;
    }

    /**
     * Get point on helix at path length s in arbitrary oriented, constant magnetic field with unit vector h
     * @param s - path length
     * @param h - magnetic field unit vector
     * @return get a 3D point along the helix
     */
    private DMatrix3 getPointOnHelix_ejml(double s, DMatrix3 h) {
        DMatrix3 p0 = getP0_ejml();
        DMatrix3 x0 = getX0_ejml();
        double srho = s * _rho;
        double ssrho = FastMath.sin(srho);
        double csrho = FastMath.sqrt(1-ssrho*ssrho);
        //Hep3Vector a1 = VecOp.mult(1 / a * ssrho, p0);

        DMatrix3 a1 = new DMatrix3(p0);
        CommonOps_DDF3.scale(1 / _a * ssrho,a1);
        
        //Hep3Vector a2 = VecOp.mult(1 / a * (1 - csrho), VecOp.cross(p0, h));
        DMatrix3x3 p0x = new DMatrix3x3();
        p0x.set(0, -p0.get(2,0), p0.get(1,0), p0.get(2,0),0., -p0.get(0,0), -p0.get(1,0),p0.get(0,0),0.);
        DMatrix3 a2 = new DMatrix3();
        CommonOps_DDF3.mult(p0x,h,a2);
        CommonOps_DDF3.scale(1 / _a * (1 - csrho),a2);
        
        //Hep3Vector a3 = VecOp.mult(VecOp.dot(p0, h) / _p_mag, h);

        DMatrix3 a4 = new DMatrix3(h);
        double p0doth = CommonOps_DDF3.dot(p0,h);
        CommonOps_DDF3.scale(p0doth/_p0_mag,a4);

        //Hep3Vector a4 = VecOp.mult(s - srho / _rho, a3);
        CommonOps_DDF3.scale(s - srho / _rho,a4);
        
        
        //Hep3Vector x = VecOp.add(x0, a1);
        //x = VecOp.sub(x, a2);
        //x = VecOp.add(x, a4);
        
        DMatrix3 x = new DMatrix3(x0);
        CommonOps_DDF3.addEquals(x,a1);
        CommonOps_DDF3.subtractEquals(x,a2);
        CommonOps_DDF3.addEquals(x,a4);
        
        return x;
    }




    private Hep3Vector getMomentumOnHelix(double s, Hep3Vector h) {
        /*
         * Get point on helix at path lenght s in arbitrary oriented, constant magnetic field with
         * unit vector h
         */
        WTrack track = this;
        double a = track.a();
        Hep3Vector p0 = track.getP0();
        double rho = a / p0.magnitude();
        double srho = s * rho;
        Hep3Vector a1 = VecOp.mult(Math.cos(srho), p0);
        Hep3Vector a2 = VecOp.cross(p0, VecOp.mult(Math.sin(srho), h));
        Hep3Vector a3 = VecOp.mult(VecOp.dot(p0, h), VecOp.mult(1 - Math.cos(srho), h));
        Hep3Vector p = VecOp.sub(a1, a2);
        p = VecOp.add(p, a3);
        return p;
    }

    private DMatrix3 getMomentumOnHelix_ejml(double s, DMatrix3 h) {
        /*
         * Get point on helix at path lenght s in arbitrary oriented, constant magnetic field with
         * unit vector h
         */
        DMatrix3 p0 = getP0_ejml();
        double srho = s * _rho;
        double csrho = FastMath.cos(srho);
        
        
        //Hep3Vector a1 = VecOp.mult(FastMath.cos(srho), p0);
        //a1 
        DMatrix3 a1 = new DMatrix3(p0);
        CommonOps_DDF3.scale(csrho,a1);
        
        //Hep3Vector a2 = VecOp.cross(p0, VecOp.mult(FastMath.sin(srho), h));

        // set(double a11, double a12, double a13, double a21, double a22, double a23, double a31, double a32, double a33)
        // cross product axb = ax * b
        //ax = [0 -a3 a2 ; a3 0 -a1; -a2 a1 0]
        //ax = [a11 a12 a13; a22 a22 a23; a31 a32 a33]
        
        DMatrix3x3 p0x = new DMatrix3x3();
        p0x.set(0, -p0.get(2,0), p0.get(1,0), p0.get(2,0), 0., p0.get(0,0), -p0.get(1,0), p0.get(0,0), 0);
        DMatrix3 a2 = new DMatrix3();
        
        CommonOps_DDF3.mult(p0x,h,a2);
        CommonOps_DDF3.scale(FastMath.sin(srho),a2);

        //Hep3Vector a3 = VecOp.mult(VecOp.dot(p0, h), VecOp.mult(1 - FastMath.cos(srho), h));
        
        DMatrix3 a3 = new DMatrix3(h);
        double p0doth = CommonOps_DDF3.dot(p0,h);
        CommonOps_DDF3.scale(1-csrho,a3);
        CommonOps_DDF3.scale(p0doth,a3);
        
        
        //Hep3Vector p = VecOp.sub(a1, a2);
        //p = VecOp.add(p, a3);
        
        CommonOps_DDF3.subtractEquals(a1,a2);
        CommonOps_DDF3.addEquals(a1,a3);
        
        return a1;
    }

    /*
      Calculate the exact position of the new helix parameters at path length s in an arbitrarily oriented, 
      constant magnetic field point xp is the point h is a unit vector in the direction of the magnetic field.         
     * @param s - path length
     * @param h - magnetic field unit vector
     * @return track parameters
     */
    public double[] getHelixParametersAtPathLength(double s, Hep3Vector h) {

        // Find track parameters at that path length
        Hep3Vector p = getMomentumOnHelix(s, h);
        Hep3Vector x = getPointOnHelix(s, h);
        
        Hep3Vector p_tmp = getMomentumOnHelix(s);
        Hep3Vector x_tmp = getPointOnHelix(s);

        if (_debug) {
            System.out.println(" point on helix at s");
            System.out.println(" p  " + p.toString() + "   p_tmp " + p_tmp.toString());
            System.out.println(" x  " + x.toString() + "   x_tmp " + x_tmp.toString());
        }

        // Create the new parameter array
        double[] pars = new double[7];
        pars[0] = p.x();
        pars[1] = p.y();
        pars[2] = p.z();
        pars[3] = getParameters()[3]; // E is unchanged
        pars[4] = x.x();
        pars[5] = x.y();
        pars[6] = x.z();
        return pars;
    }
    

    /*
      Calculate the exact position of the new helix parameters at path length s in an arbitrarily oriented, 
      constant magnetic field point xp is the point h is a unit vector in the direction of the magnetic field.         
      * @param s - path length
      * @param h - magnetic field unit vector
      * @return track parameters
     */
    public double[] getHelixParametersAtPathLength_ejml(double s, DMatrix3 h) {

        DMatrix3 p_ejml = getMomentumOnHelix_ejml(s,h);
        DMatrix3 x_ejml = getPointOnHelix_ejml(s,h);
        
        // Create the new parameter array
        double[] pars = new double[7];
        pars[0] = p_ejml.get(0,0);
        pars[1] = p_ejml.get(1,0);
        pars[2] = p_ejml.get(2,0);
        pars[3] = getParameters()[3]; // E is unchanged
        pars[4] = x_ejml.get(0,0);
        pars[5] = x_ejml.get(1,0);
        pars[6] = x_ejml.get(2,0);
        return pars;
    }

    /**   Find the interception point between the helix and a plane  
     * @param xp point on the plane
     * @param eta unit vector of the plane 
     * @param h unit vector of magnetic field
     * @return the intersection point of the helix with the plane
     */
    public Hep3Vector getHelixAndPlaneIntercept(Hep3Vector xp, Hep3Vector eta, Hep3Vector h) {

        int iteration = 1;
        double s_total = 0.;
        double step = 9999999.9;
        
        // List<WTrack> tracks = new ArrayList<WTrack>();
        WTrack trk = this;

        DMatrix3 xp_ejml = new DMatrix3();
        DMatrix3 eta_ejml = new DMatrix3();
        DMatrix3 h_ejml = new DMatrix3();
        
        HitUtils.H3VToDM3(xp,xp_ejml);
        HitUtils.H3VToDM3(eta,eta_ejml);
        HitUtils.H3VToDM3(h,h_ejml);

        while (iteration <= max_iterations_intercept && Math.abs(step) > epsilon_intercept) {

            if (_debug) {
                System.out.printf("%s: Iteration %d\n", this.getClass().getSimpleName(), iteration);
                System.out.printf("%s: s_total %f prev_step %.3f current trk params: %s \n", this.getClass().getSimpleName(), s_total, step, trk.paramsToString());
            }

            // check that the track is not looping

            if (trk.goingForward()) {

                step = getPathLengthToPlaneApprox_ejml(xp_ejml, eta_ejml, h_ejml);
                
                System.out.println("PF::WTRACKOPT::step_ejml\n"+step);

                // Start by approximating the path length to the point
                step = getPathLengthToPlaneApprox(xp, eta, h);
                System.out.println("PF::WTRACKOPT::step\n"+step);
                

                if (step == Double.NaN)
                    return null;

                if (_debug)
                    System.out.printf("%s: path length step s=%.3f\n", this.getClass().getSimpleName(), step);

                // Find the track parameters at this point
                
                double[] params = getHelixParametersAtPathLength(step, h);

                System.out.println("PF::params="+params[0]+" "+params[1]+" "+params[2]+" "+params[3]+" "+params[4]+" "+params[5]+" "+params[6]);

                params = getHelixParametersAtPathLength_ejml(step, h_ejml);
                System.out.println("PF::params_ejml="+params[0]+" "+params[1]+" "+params[2]+" "+params[3]+" "+params[4]+" "+params[5]+" "+params[6]);

                // update the track parameters
                trk.setTrackParameters(params);

                if (_debug)
                    System.out.printf("%s: updated track params: [%s]\n", this.getClass().getSimpleName(), trk.paramsToString());

                // tracks.add(trk);
                iteration++;
                s_total += step;

                // Save distance between point and estimate
                // Hep3Vector dpoint = VecOp.sub(xp, trk.getX0());

            } else {
                // if(_debug)
                System.out.printf("%s: this track started to go backwards?! params [%s]\n", this.getClass().getSimpleName(), trk.toString());
                return null;
            }

        }

        if (_debug)
            System.out.printf("%s: final total_s=%f with final step %f after %d iterations gave track params: %s\n", this.getClass().getSimpleName(), s_total, step, iteration, trk.paramsToString());

        return trk.getX0();

    }

    

    
    /**   Find the interception point between the helix and a plane  
     * @param xp point on the plane
     * @param eta unit vector of the plane 
     * @param h unit vector of magnetic field
     * @return the intersection point of the helix with the plane
     */
    public Hep3Vector getHelixAndPlaneIntercept_ejml(DMatrix3 xp, DMatrix3 eta, DMatrix3 h) {
        
        int iteration = 1;
        double s_total = 0.;
        double step = 9999999.9;
        
        // List<WTrack> tracks = new ArrayList<WTrack>();
        WTrack trk = this;

        while (iteration <= max_iterations_intercept && Math.abs(step) > epsilon_intercept) {

            if (_debug) {
                System.out.printf("%s: Iteration %d\n", this.getClass().getSimpleName(), iteration);
                System.out.printf("%s: s_total %f prev_step %.3f current trk params: %s \n", this.getClass().getSimpleName(), s_total, step, trk.paramsToString());
            }

            // check that the track is not looping

            if (trk.goingForward()) {

                step = getPathLengthToPlaneApprox_ejml(xp, eta, h);
                
                if (step == Double.NaN)
                    return null;

                if (_debug)
                    System.out.printf("%s: path length step s=%.3f\n", this.getClass().getSimpleName(), step);

                // Find the track parameters at this point
                
                double[] params = getHelixParametersAtPathLength_ejml(step, h);
                
                // update the track parameters
                trk.setTrackParameters(params);

                if (_debug)
                    System.out.printf("%s: updated track params: [%s]\n", this.getClass().getSimpleName(), trk.paramsToString());

                // tracks.add(trk);
                iteration++;
                s_total += step;

                // Save distance between point and estimate
                // Hep3Vector dpoint = VecOp.sub(xp, trk.getX0());

            } else {
                // if(_debug)
                System.out.printf("%s: this track started to go backwards?! params [%s]\n", this.getClass().getSimpleName(), trk.toString());
                return null;
            }

        }

        if (_debug)
            System.out.printf("%s: final total_s=%f with final step %f after %d iterations gave track params: %s\n", this.getClass().getSimpleName(), s_total, step, iteration, trk.paramsToString());

        return trk.getX0();

    }

}
