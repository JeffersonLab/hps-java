package org.lcsim.hps.recon.tracking.kalman.util;


/**
 * 
 * Find the interesection of a track, specified by the cleo paramters 
 * with a plane normal to the z axis and at the specfied z.
 *
 *@author $Author: jeremy $
 *@version $Id: zplane_int.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */


public class zplane_int{

    public double x;
    public double y;
    public double z;
    public double psi;

    // Track parameters referenced to PCA (0,0,0);
    public double k;
    public double d0;
    public double phi;
    public double z0;
    public double t;

    public double q;
    public double rho;

    public zplane_int( double k,
		       double d0,
		       double phi,
		       double z0,
		       double t,
		       double zp
		       ){

	this.k   = k;
	this.d0  = d0;
	this.phi = phi;
	this.z0  = z0;
	this.t   = t;

	// Some other quantities derived from the track parameters.

	q   =  (k > 0 ) ? 1.0 : -1.0;
	rho = Math.abs( 0.5/k );

	double rhopsi = (zp-z0)/t;
	psi = rhopsi/rho;

	double u0  =  Math.cos(phi);
	double v0  =  Math.sin(phi);
	double x0  = -d0 * v0;
	double y0  =  d0 * u0;

	double xc = x0-q*rho*v0;
	double yc = y0+q*rho*u0;
	
	x = xc + q*rho*Math.sin(phi+q*psi);
	y = yc - q*rho*Math.cos(phi+q*psi);
	z = zp;
    }
    
    public double getX(){
	return x;
    }

    public double getY(){
	return y;
    }

    public double getZ(){
	return z;
    }


    public double getPsi(){
	return psi;
    }


}
