package org.hps.recon.tracking.kalman.util;

import org.lcsim.constants.Constants;
import org.lcsim.recon.tracking.trfutil.TRFMath;

/**
 * 
 * One generated track.  The constructor uses a parameterization that
 * is useful for thinking about the range of generated parameters.
 * Other information is available.  Distances are in mm and momentum in GeV/c.
 * 
 *  pt  = transverse momentum (GeV)
 *  cz  = cos(theta)
 *  phi = azimuth of momentum (radians)
 *  d0  = signed DCA wrt z axis (mm)
 *  z0  = z at PCA to z axis (mm)
 * 
 * Parameters are defined in the CLEO convention.
 * 
 *@author $Author: jeremy $
 *@version $Id: RKTrack.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */


public class RKTrack {


    public RKTrack( double q, 
		    double pt, 
		    double cz, 
		    double phi0, 
		    double d0,
		    double z0,
		    double bz){
	
	this.q    = q;
	this.pt   = pt;
	this.cz   = cz;
	this.phi0 = phi0;
	this.d0   = d0;
	this.z0   = z0;
	this.bz   = bz;
	
	update();
    }

    public RKTrack ( RKTrack t ){
	q    = t.q;
	pt   = t.pt;
	cz   = t.cz;
	phi0 = t.phi0;
	d0   = t.d0;
	z0   = t.z0;
	bz   = t.bz;
	
	update();
    }

    
    public double q()     { return     q;}
    public double pt()    { return    pt;}
    public double cz()    { return    cz;}
    public double phi0()  { return  phi0;}
    public double d0()    { return    d0;}
    public double z0()    { return    z0;}
    public double cu()    { return    cu;}
    public double p()     { return     p;}
    public double cotth() { return cz/sz;}
    public double x0()    { return    x0;}
    public double y0()    { return    y0;}
    public double sz()    { return    sz;}
    public double rho()   { return   rho;}

    public double e() {
	return e(m_pi);
    }

    public double e( double m ){
	return Math.sqrt( p*p + m*m);
    }
    
    // Synonym for cottan(theta);
    public double tanlam()  { return cz/sz;}

    public double momentum(){ return     p;}
    public double charge()  { return     q;}

    public String toString(){
	return String.format("CLEO Style track: q=%4.1f pt=%10.4f cz=%10.4f phi0=%10.4f d0=%10.4f z0=%10.4f\n",
			   q, pt, cz, phi0, d0, z0  );
    }

    public double phi_pos(){
	return TRFMath.fmod2(Math.atan2(y0,x0),TRFMath.TWOPI);
    }

    private double bz=5.0;

    private double  q;       // Charge
    private double pt;       // Transverse momentum wrt z axis
    private double cz;       // Cos(theta) of momentum
    private double phi0;     // Azimuth of momentum
    private double d0;       // Signed DCA to z axis
    private double z0;       // z0 at PCA to z axis


    // Derived quantities:
    private double p;        // momentum
    private double rho;      // Radius of curvature
    private double cu;       // Curvature in cleo convention ( = q/2/rho).

    private double sz;       // sin(theta)
    private double cotth;    // cot(theta)

    private double sphi0;    // sin(phi0)
    private double cphi0;    // cos(phi0)

    private double x0;       // x at PCA to z axis.
    private double y0;       // y at PCA to z axis.

    // Matches precicision in TRF.
    private double m_pi = 0.13957;

    private void update(){
	rho   = pt/Constants.fieldConversion/bz;
	cu    = 0.5*q/rho;
	sphi0 = Math.sin(phi0);
	cphi0 = Math.cos(phi0);
	sz    = Math.sqrt( 1. - cz*cz);
	cotth = cz/sz;
	p     = pt/sz;
	x0    = -d0*sphi0;
	y0    =  d0*cphi0;

    }

    // Some other possibly useful things.
    //double rc = d0 + 0.5/kappa;
    //double xc = -rc*sphi0;
    //double yc =  rc*cphi0;


    //double cx = cphi0*sinth;
    //double cy = sphi0*sinth;
    //double cz = costh;

}
