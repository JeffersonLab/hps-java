package org.hps.recon.tracking.kalman.util;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

/**
 * 
 * Compute various derived quantities from a VTrack or an ETrack.
 *
 *@author $Author: jeremy $
 *@version $Id: VTUtil.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */


public class VTUtil {

    // Matches the precision in the TRF eloss code.
    private double m_pi   = 0.13957;

    public VTUtil( VTrack v){
	this.v    = v;
	Surface s = v.surface();

	if ( s.pureType().equals( SurfCylinder.staticType()) ){
	    DoCylinder();
	} else if ( s.pureType().equals( SurfZPlane.staticType()) ){
	    DoZPlane();
	}
	else{
	    DoNull();
	}
    }

    public VTUtil( ETrack e){
	v = new VTrack( e.surface(), e.vector() );
	Surface s = v.surface();

	if ( s.pureType().equals( SurfCylinder.staticType()) ){
	    DoCylinder();
	} else if ( s.pureType().equals( SurfZPlane.staticType()) ){
	    DoZPlane();
	}
	else{
	    DoNull();
	}
    }


    public double momentum(){ 
	return p;
    }

    public double p(){ 
	return p;
    }

    public double e(){
	return e(m_pi);
    }

    public double e(double m){
	return Math.sqrt( p()*p() +m*m );
    }

    public Hep3Vector asHep3Vector(){
	double px = pt*Math.cos(phi);
	double py = pt*Math.sin(phi);
	double pz = p *costh;
	return new BasicHep3Vector(px,py,pz);
    }

    public double costh(){
	return costh;
    }

    public double sinth(){
	return sinth;
    }
    public double q(){
	return q;
    }

    public double z(){
	return z;
    }

    public double r(){
	return r;
    }

    private VTrack v = null;
    private double costh;
    private double sinth;
    private double cotth;
    private double pt;
    private double p;
    private double phi;
    private double phi0;
    private double q;

    private double r;
    private double z;

    private void DoCylinder(){
	    cotth = v.vector(SurfCylinder.ITLM);
	    sinth = 1./Math.sqrt( 1. + cotth*cotth );
	    costh = cotth*sinth;
	    pt    = 1./Math.abs(v.vector(SurfCylinder.IQPT));
	    p     = pt/sinth;
	    phi   = v.vector(SurfCylinder.IPHI)+v.vector(SurfCylinder.IALF);
	    q = Math.signum(v.vector(SurfCylinder.IQPT));

	    SurfCylinder cyl = (SurfCylinder) v.surface();
	    r = cyl.radius();
	    z = v.vector(SurfCylinder.IZ);
    }

    private void DoNull(){
	cotth = 0.;
	costh = 0.;
	sinth = 0.;
	pt    = 0.;
	p     = 0.;
	phi   = 0.;
	q     = 1.;
	r     = 0.;
	z     = 0.;

    }

    private void DoZPlane(){
	double xpr = v.vector(SurfZPlane.IDXDZ);
	double ypr = v.vector(SurfZPlane.IDYDZ);

	costh = 1./Math.sqrt( 1. + xpr*xpr + ypr*ypr);
	SurfZPlane sz = (SurfZPlane) v.surface();

	// This is a hack and needs to be done more generally.
	if ( sz.z() <  0) costh = -costh;

	sinth = Math.sqrt( 1. - costh*costh);
	cotth = costh/sinth;
	p     = Math.abs(1./v.vector(SurfZPlane.IQP));
	pt    = p*sinth;
	phi   = Math.atan2( ypr, xpr );

	q = Math.signum(v.vector(SurfZPlane.IQP));

	double x = v.vector(SurfZPlane.IX);
	double y = v.vector(SurfZPlane.IY);
	r = Math.sqrt( x*x + y*y );
	z = ((SurfZPlane)v.surface()).z();

    }


}
