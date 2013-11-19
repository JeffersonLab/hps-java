package org.lcsim.hps.recon.tracking.kalman.util;

import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trfutil.TRFMath;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

/**
 * 
 * Utility routine to convert RKTracks into TRF VTracks.
 *
 *@author $Author: jeremy $
 *@version $Id: toTRF.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */


public class toTRF {

    // TRF wants distances in cm, not mm.
    private double mmTocm = 0.1;

    private RKTrack t;

    public toTRF( RKTrack t ){
	this.t = t;
    }

    public VTrack atDcaZaxis(){

	// Opposite sign convention.
	double r_signed = -t.d0();

	TrackVector tv  = new TrackVector();
	tv.set(0, r_signed * mmTocm );
	tv.set(1, t.z0()   * mmTocm );
	tv.set(2, t.phi0()          );
	tv.set(3, t.cotth()         );
	tv.set(4, t.q()/t.pt()      );
	SurfDCA s = new SurfDCA( 0., 0. );
	VTrack vt = new VTrack(s, tv);
	return vt;
	
    }

    public VTrack atCyl( double r){

	cyl_int c = new cyl_int( t.cu(), t.d0(), t.phi0(), t.z0(), t.cotth(), r );

	double phi_pos = Math.atan2(c.getY(),c.getX());
	double phi_dir = TRFMath.fmod2(t.phi0() + t.charge()*c.getPsi(),TRFMath.TWOPI);
	double alpha   = TRFMath.fmod2(phi_dir-phi_pos,TRFMath.TWOPI);

	TrackVector tv  = new TrackVector();
	tv.set(0, phi_pos           );
	tv.set(1, c.getZ() * mmTocm );
	tv.set(2, alpha             );
	tv.set(3, t.cotth()         );
	tv.set(4, t.q()/t.pt()      );

	SurfCylinder s = new SurfCylinder( r*mmTocm );
	VTrack vt = new VTrack(s, tv);
	return vt;
    }

    public VTrack atZPlane( double z){

	zplane_int zpi = new zplane_int ( t.cu(), t.d0(), t.phi0(), t.z0(), t.cotth(), z );
	
	double phi = t.phi0() + t.q()*zpi.getPsi();
	double dxdz = Math.cos(phi)/t.cotth();
	double dydz = Math.sin(phi)/t.cotth();

	TrackVector tv  = new TrackVector();
	tv.set(0, zpi.getX()*mmTocm );
	tv.set(1, zpi.getY()*mmTocm );
	tv.set(2, dxdz              );
	tv.set(3, dydz              );
	tv.set(4, t.q()/t.p()       );

	SurfZPlane s = new SurfZPlane( z*mmTocm );
	VTrack vt = new VTrack(s, tv);
	if ( t.cz() >= 0. ){
	    vt.setForward();
	}else{
	    vt.setBackward();
	}

	return vt;

    }

    // Path length corresponding to one half of an arc.
    public double halfarc(){
	return Math.PI*t.rho()/t.sz()*mmTocm;
    }


    
}
