package org.hps.recon.tracking.kalman.util;


/**
 * 
 * Find the interesection of a track, specified by the CLEO paramters
 * with the a cylinder of radius r, centered on the origin.
 *
 *@author $Author: jeremy $
 *@version $Id: cyl_int.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

public class cyl_int{

    public double x;
    public double y;
    public double psi;

    public double k;
    public double d0;
    public double phi;
    public double z;
    public double t;

    public double q;
    public double rho;

    public cyl_int( double k, 
		    double d0, 
		    double phi, 
		    double z, 
		    double t, 
		    double r
		    ){

	this.k   = k;
	this.d0  = d0;
	this.phi = phi;
	this.z   = z;
	this.t   = t;

	// Some other quantities derived from the track parameters.

	q   =  (k > 0 ) ? 1.0 : -1.0;
	rho = Math.abs( 0.5/k );


	double u0   =  Math.cos(phi);
	double v0   =  Math.sin(phi);
	double x0   = -d0 * v0;
	double y0   =  d0 * u0;

	double x0t  = x0-q*rho*v0;
	double y0t  = y0+q*rho*u0;

	double dpsi = Math.atan2(-y0t,x0t);
	
	double sang = (r*r-x0t*x0t-y0t*y0t-rho*rho)/
	    (2.*q*rho*Math.sqrt(x0t*x0t+y0t*y0t));
	if ( sang > 1.0 ){
	    sang = 1.0; 
	}else if ( sang < -1.0 ){
	    sang = -1.0;
	}

	double psi1=(Math.asin(sang)-phi-dpsi);
	double psi2=(Math.PI-Math.asin(sang)-phi-dpsi);

	double twopi = 2.*Math.PI;

	// Put angles in the correct quadrant.
	if (psi1 > twopi){
	    psi1 -= twopi;
	}
	if (psi1 < 0.0) {
	    psi1 += twopi;
	}
	if (psi1 < 0.0) {
	    psi1 += twopi;
	}
	
	if (psi2 > twopi) {
	    psi2 -= twopi;
	}
	if (psi2 < 0.0) {
	    psi2 += twopi;
	}
	if (psi2 < 0.0) {
	    psi2 += twopi;
	}

	psi = ( psi2 < psi1) ? psi2 : psi1;
	
	x = x0t + q*rho*Math.sin(phi+q*psi);
	y = y0t - q*rho*Math.cos(phi+q*psi);
    }
    
    public double getX(){
	return x;
    }

    public double getY(){
	return y;
    }

    public double getZ(){
	double cz = t / Math.sqrt( 1. + t*t );
	return z + rho*psi*t;
    }


    public double getPsi(){
	return psi;
    }


}





