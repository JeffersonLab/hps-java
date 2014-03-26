/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.lcsim.constants.Constants;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;

/**
 *
 * @author phansson
 */
public class WTrack {

    private boolean _debug = false;
    public enum PARAM{TEST;}
    private double[] _parameters = new double[7];
    public HelicalTrackFit _htf = null;
    private double _bfield;
    private double _a;
    
    static int max_iterations_intercept = 10;
    static double epsilon_intercept = 1e-4;
    
    public WTrack(WTrack trk) {
        _bfield = trk._bfield;
        _a = trk._a;
        _parameters = trk._parameters;
        _htf = trk._htf;
        _debug = trk._debug;
    }
    
    public WTrack(HelicalTrackFit track, double bfield) {
        initWithTrack(track, bfield, false);
    }
    
    public WTrack(HelicalTrackFit track, double bfield, boolean flip) {
        initWithTrack(track, bfield, flip);
    }
    public void initWithTrack(HelicalTrackFit track, double bfield, boolean flip) {
        _htf = track;
        _bfield = flip ? -1.0*bfield : bfield; // flip if needed
        _a = -1*Constants.fieldConversion*_bfield*Math.signum(track.R());
        double p = track.p(Math.abs(_bfield));
        double theta = Math.PI/2.0 - Math.atan(track.slope());
        double phi = track.phi0();
        _parameters[0] = p*Math.cos(phi)*Math.sin(theta);
        _parameters[1] = p*Math.sin(phi)*Math.sin(theta);
        _parameters[2] = p*Math.cos(theta); 
        _parameters[3] = Math.sqrt(_parameters[0]*_parameters[0]+_parameters[1]*_parameters[1]+_parameters[2]*_parameters[2]);
        _parameters[4] = -1*track.dca()*Math.sin(phi); //x0
        _parameters[5] = track.dca()*Math.cos(phi); //y0
        _parameters[6] = track.z0(); //z0
        if(_debug) {
            System.out.printf("%s: WTrack initialized (p=%f,bfield=%f,theta=%f,phi=%f) from HelicalTrackFit:\n%s:%s\n",this.getClass().getSimpleName(),
                    p,_bfield,theta,phi,
                    this.getClass().getSimpleName(),this.toString());
        }
    }
    public void setTrackParameters(double [] params) {
        _parameters = params;
    }
    
    public double[] getParameters() {
        return _parameters;
    }
    
    private boolean goingForward() {
        // assuming the track should go in the x-direction --> not very general -> FIX THIS!?
        return getP0().x()>0 ? true : false;
    }
    
    
    public double a() {
        return _a;

    }
    
    private int getCharge() {
        return (int) Math.signum(_htf.R());
    }
    
    public Hep3Vector getP0() {
        return ( new BasicHep3Vector(_parameters[0],_parameters[1],_parameters[2]));
    }
    
    public Hep3Vector getX0() {
        return ( new BasicHep3Vector(_parameters[4],_parameters[5],_parameters[6]));
    }
        
    public String paramsToString() {
        String str = "";
        for(int i=0;i<7;++i) str += _parameters[i] + ", ";
        return str;
    }
    public String toString() {
        
        String str = "WTrack params [" + paramsToString() + "]";
        if(this._htf!=null) {
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
        double px = p0.x()*Math.cos(rho*s) - p0.y()*Math.sin(rho*s);
        double py = p0.y()*Math.cos(rho*s) + p0.x()*Math.sin(rho*s);
        double pz = p0.z(); 
        return (new BasicHep3Vector(px,py,pz));
    }
    
    private Hep3Vector getPointOnHelix(double s) {
        WTrack track = this;
        double a = track.a();
        Hep3Vector p0 = track.getP0();
        Hep3Vector x0 = track.getX0();
        double rho = a / p0.magnitude();
        double x = x0.x() + p0.x()/a*Math.sin(rho*s) - p0.y()/a*(1-Math.cos(rho*s));
        double y = x0.y() + p0.y()/a*Math.sin(rho*s) + p0.x()/a*(1-Math.cos(rho*s));
        double z = x0.z() + p0.z()/p0.magnitude()*s;
        return (new BasicHep3Vector(x,y,z));
    }
    
    private double getPathLengthToPlaneApprox(Hep3Vector xp, Hep3Vector eta, Hep3Vector h) {
        /*
         * Find the approximate path length to the point xp 
         * in arbitrary oriented, constant magnetic field with unit vector h
         */
        WTrack track = this;
        double a = track.a();
        Hep3Vector p0 = track.getP0();
        Hep3Vector x0 = track.getX0();
        double p = p0.magnitude();
        double rho = a / p;
        double A = VecOp.dot(eta,VecOp.cross(p0, h))/p*0.5*rho;
        double B = VecOp.dot(p0,eta)/p;
        double C = VecOp.dot(VecOp.sub(x0,xp),eta);
        double t = B*B-4*A*C;
        if(t<0) {
            System.out.println(" getPathLengthToPlaneApprox ERROR t is negative (" + t + ")!" );
            System.out.println(" p " + p + " rho " + rho + " a " + a + " A " + A + " B " + B + " C " + C);
            System.out.println(" track params: " + track.paramsToString());
            System.out.println(" xp " + xp.toString());
            System.out.println(" eta " + eta.toString());
            System.out.println(" h " + h.toString());
            System.exit(1);
        }
        double root1 = (-B + Math.sqrt(t)) /(2*A);
        double root2 = (-B - Math.sqrt(t)) /(2*A);

        // choose the smallest positive solution
        // if both negative choose the smallest negative ???
        //if(root1==0 || root2==0) root=0;
        double root = Math.abs(root1) <= Math.abs(root2) ? root1 : root2;
//        else if(Math.signum(root1)>0 && Math.signum(root2)<0) root = root1;
//        else if(Math.signum(root2)>0 && Math.signum(root1)<0) root = root2;
//        else if(Math.signum(root1)>0 && Math.signum(root2)>0) root =  root1 > root2 ? root2 : root1;
//        else if(Math.signum(root1)<0 && Math.signum(root2)<0) root =  root1 < root2 ? root2 : root1;
//        else {
//            System.out.println(" I should never get here! (root1=" + root1 + " root2=" + root2+")");
//            System.exit(1);
//        }
        if(_debug) {
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
    
    
    private Hep3Vector getPointOnHelix(double s, Hep3Vector h) {
        /*
         * Get point on helix at path lenght s 
         * in arbitrary oriented, constant magnetic field with unit vector h
         */
        WTrack track = this;
        double a = track.a();
        Hep3Vector p0 = track.getP0();
        double p = p0.magnitude();
        Hep3Vector x0 = track.getX0();
        double rho = a / p0.magnitude();
        double srho = s*rho;
        Hep3Vector a1 = VecOp.mult(1/a*Math.sin(srho), p0);
        Hep3Vector a2 = VecOp.mult(1/a*(1-Math.cos(srho)),VecOp.cross(p0,h));
        Hep3Vector a3 = VecOp.mult(VecOp.dot(p0, h)/p,h);
        Hep3Vector a4 = VecOp.mult(s-Math.sin(srho)/rho, a3);
        Hep3Vector x = VecOp.add(x0,a1);
        x = VecOp.sub(x,a2);
        x = VecOp.add(x,a4);
        return x;
    }
    
    private Hep3Vector getMomentumOnHelix(double s, Hep3Vector h) {
        /*
         * Get point on helix at path lenght s 
         * in arbitrary oriented, constant magnetic field with unit vector h
         */
        WTrack track = this;
        double a = track.a();
        Hep3Vector p0 = track.getP0();
        double rho = a / p0.magnitude();
        double srho = s*rho;
        Hep3Vector a1 = VecOp.mult(Math.cos(srho), p0);
        Hep3Vector a2 = VecOp.cross(p0, VecOp.mult(Math.sin(srho),h));
        Hep3Vector a3 = VecOp.mult(VecOp.dot(p0,h),VecOp.mult(1-Math.cos(srho),h));
        Hep3Vector p  = VecOp.sub(a1,a2);
        p = VecOp.add(p,a3);
        return p;
    }
    
    
    private double[] getHelixParametersAtPathLength(double s, Hep3Vector h) {
        /*
         * Calculate the exact position of the new helix parameters at path length s
         * in an arbitrarily oriented, constant magnetic field
         * 
         * point xp is the point 
         * h is a unit vector in the direction of the magnetic field
         */
        
        // Find track parameters at that path length
        Hep3Vector p = getMomentumOnHelix(s, h);
        Hep3Vector x = getPointOnHelix(s, h);
        
        Hep3Vector p_tmp = getMomentumOnHelix(s);
        Hep3Vector x_tmp = getPointOnHelix(s);
        
        if(_debug) {
            System.out.println(" point on helix at s");
            System.out.println(" p  " + p.toString() + "   p_tmp " + p_tmp.toString());
            System.out.println(" x  " + x.toString() + "   x_tmp " + x_tmp.toString());
        }
        
        
        //Create the new parameter array
        double [] pars = new double[7];
        pars[0] = p.x();
        pars[1] = p.y();
        pars[2] = p.z();
        pars[3] = getParameters()[3]; //E is unchanged
        pars[4] = x.x();
        pars[5] = x.y();
        pars[6] = x.z();
        return pars;
    }
    
    
    public Hep3Vector getHelixAndPlaneIntercept(Hep3Vector xp, Hep3Vector eta, Hep3Vector h) {
    
        /*
         * Find the interception point between the helix and plane
         * xp: point on the plane
         * eta: unit vector of the plane
         * h: unit vector of magnetic field
         */
        
        
        int iteration = 1;
        double s_total = 0.;
        double step = 9999999.9;
        //List<WTrack> tracks = new ArrayList<WTrack>();
        WTrack trk = this;
        while(iteration<=max_iterations_intercept && Math.abs(step)>epsilon_intercept) {
            
            if(_debug) {
                System.out.printf("%s: Iteration %d\n", this.getClass().getSimpleName(),iteration);
                System.out.printf("%s: s_total %f prev_step %.3f current trk params: %s \n",
                                        this.getClass().getSimpleName(),s_total,step,trk.paramsToString());
            }
            
            // check that the track is not looping
            
            if(trk.goingForward()) {
                

                // Start by approximating the path length to the point
                step = getPathLengthToPlaneApprox(xp, eta, h);

                if(_debug) System.out.printf("%s: path length step s=%.3f\n",this.getClass().getSimpleName(),step);

                // Find the track parameters at this point
                double[] params = getHelixParametersAtPathLength(step, h);

                // update the track parameters           
                trk.setTrackParameters(params);

                if(_debug) System.out.printf("%s: updated track params: [%s]\n",this.getClass().getSimpleName(),trk.paramsToString());

                //tracks.add(trk);
                iteration++;
                s_total += step;

                //Save distance between point and estimate
                //Hep3Vector dpoint = VecOp.sub(xp, trk.getX0());
            
            } else {
                //if(_debug) 
                    System.out.printf("%s: this track started to go backwards?! params [%s]\n",this.getClass().getSimpleName(),trk.toString());
                return null;
            }
            
            
        }
        
        if(_debug) System.out.printf("%s: final total_s=%f with final step %f after %d iterations gave track params: %s\n",
                                        this.getClass().getSimpleName(),s_total,step,iteration,trk.paramsToString());
        
        return trk.getX0();
    
    }
    
   
    
  
    
    
}
