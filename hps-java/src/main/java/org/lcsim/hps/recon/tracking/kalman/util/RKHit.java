package org.lcsim.hps.recon.tracking.kalman.util;

import java.util.List;
import java.util.Random;

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Hit;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfcyl.ClusCylPhi;
import org.lcsim.recon.tracking.trfcyl.ClusCylPhiZ2D;
import org.lcsim.recon.tracking.trfcyl.HitCylPhi;
import org.lcsim.recon.tracking.trfcyl.HitCylPhiZ2D;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfzp.ClusZPlane1;
import org.lcsim.recon.tracking.trfzp.ClusZPlane2;
import org.lcsim.recon.tracking.trfzp.HitZPlane1;
import org.lcsim.recon.tracking.trfzp.HitZPlane2;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;
import org.lcsim.util.aida.AIDA;
/**
 * 
 * Interface between RKGeom and the TRF hit classes.
 * The main purpose of this class is to produce the 
 * various sorts of TRF hits.
 *
 *@author $Author: jeremy $
 *@version $Id: RKHit.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

public class RKHit{

    static private Random ran  = new Random();
    static private Random ranz = new Random();

    private AIDA aida = AIDA.defaultInstance();


    // Resolutions in cm ( ie TRF units ).
    //static private double respixel   =  0.0025;
    //static private double resstrip   =  0.0050;
    //static private double zres_strip =  2.89;    // 10 cm /sqrt(12).
    //static private double zres_strip =  1.00;    // see if a smaller number helps.

    private RKTrack  track;
    private RKSurf   surf;
    private VTrack   vt;
    private PropStat ps;
    private double   path;
    private double   psi;
    private Hit ghit = null;

    // Dimension of the readout 
    private int rodim = 0;

    // Number of stereo measurements in this device.
    private int zdim  = 0;

    public RKHit( RKTrack track, RKSurf surf, VTrack vt, PropStat ps, double path){
	this.track = track;
	this.surf  = surf;
	this.vt    = new VTrack(vt);
	this.ps    = new PropStat(ps);
	this.path  = path;
	psi = path*track.sz()/track.rho();

	rodim = surf.rodim;
	if ( surf.getType() ==  RKSurf.type_zdisc ){
	    zdim = 1;
	} else if (  surf.getType() ==  RKSurf.type_tube ){
	    if ( rodim == 2 ) {
		zdim=1;
	    }
	}

    }


    public RKTrack  getRKTrack() { return track; }
    public RKSurf   getSurface() { return surf;  }
    public VTrack   getVTrack()  { return vt;    }
    public PropStat getPropStat(){ return ps;    }
    public double   getPath()    { return path;  }
    public double   getPsi()     { return psi;   }
    public int      getRodim()   { return rodim; }
    public int      getZdim()    { return zdim;  }

    static public void setSeed( long seed ){
	ran.setSeed(seed);
	ranz.setSeed(seed+1234578);
    }

    public Hit MakeHit(){
	if ( ghit != null ) return ghit;

	if ( surf.getType() == RKSurf.type_tube ){
	    if ( surf.rodim == 0 ){
		return null;
	    } else if ( surf.rodim == 1 ){
		return MakeCylPhi( surf.getResolution0() );
	    } else if ( surf.rodim == 2 ){
		return MakeCylPhiZ2D( surf.getResolution0(), surf.getResolution1() );
	    }
	} else if ( surf.getType() == RKSurf.type_zdisc ){
	    if ( surf.rodim == 0 ){
		return null;
	    } else if ( surf.rodim == 1 ){
		return MakeZPlane1D( surf.getResolution0() );
	    } else if ( surf.rodim == 2 ){
		return MakeZPlane2D( surf.getResolution0(), surf.getResolution1() );
	    }
	}
	return null;
    }
    
    private Hit MakeCylPhi( double res){
	SurfCylinder s = (SurfCylinder)vt.surface();
	double r = s.radius();

	double sig0 = res/r;

	double m0 = vt.vector(0) + sig0*ran.nextGaussian();
	//double m0 = vt.vector(0);

	double stereo = 0.;
	int mcid = 0;
	ClusCylPhi cluster = new ClusCylPhi( r, m0, sig0, mcid );
	TrackError  verr = new TrackError();
	verr.set(0,0,10.);
	verr.set(1,1,100.);
	verr.set(2,2,2.);
	verr.set(3,3,10.);
	verr.set(4,4,10.);

	ETrack et =  new ETrack(s.newPureSurface(), vt.vector(), verr );
	List ghits = cluster.predict(et,cluster);
	ghit = (HitCylPhi)ghits.get(0);
	return ghit;
    }

    private Hit MakeCylPhiZ2D( double sigpixel, double sigz ){

	SurfCylinder s = (SurfCylinder)vt.surface();
	double r = s.radius();

	double sig0 = sigpixel/r;
	double sig1 = sigz;

	double m0 = vt.vector(0) + sig0*ran.nextGaussian();

	//double m1 = vt.vector(1) + sig1*ran.nextGaussian();

	// Hack to decouple r-phi and z fits.
	double m1;
	if ( r > 100 ){
	    m1 = vt.vector(1) + sig1*ranz.nextGaussian();
	}else{
	    m1 = vt.vector(1) + sig1*ran.nextGaussian();
	}

	//double m0 = vt.vector(0);
	//double m1 = vt.vector(1);

	double stereo = 0.;
	int mcid = 0;
	ClusCylPhiZ2D cluster = new ClusCylPhiZ2D( r, m0, sig0, m1, sig1, stereo, mcid );
	TrackError  verr = new TrackError();
	verr.set(0,0,10.);
	verr.set(1,1,100.);
	verr.set(2,2,2.);
	verr.set(3,3,10.);
	verr.set(4,4,10.);

	ETrack et =  new ETrack(s.newPureSurface(), vt.vector(), verr );
	List ghits = cluster.predict(et,cluster);
	ghit = (HitCylPhiZ2D)ghits.get(0);
	return ghit;
    }

    private Hit MakeZPlane1D( double res){
	SurfZPlane s = (SurfZPlane)vt.surface();
	double z     =  s.z();

	// Measurement error.
	double sig0 = res;

	// Measurement direction.
	double wx  = 1.;
	double wy  = 0.;
	if ( surf.ixy == RKSurf.ixy_y ){
	    wx = 0.;
	    wy = 1.;
	}
	double m0  = vt.vector(0)*wx + vt.vector(1)*wy;
	m0 += sig0*ran.nextGaussian();
 
	int mcid = 0;
	ClusZPlane1 cluster = new ClusZPlane1( z, wx, wy, m0, sig0, mcid );
	TrackError  verr = new TrackError();
	verr.set(0,0,10.);
	verr.set(1,1,10.);
	verr.set(2,2,10.);
	verr.set(3,3,10.);
	verr.set(4,4,10.);

	ETrack et =  new ETrack(s.newPureSurface(), vt.vector(), verr );
	List ghits = cluster.predict(et,cluster);
	ghit = (HitZPlane1)ghits.get(0);
	return ghit;

    }

    private Hit MakeZPlane2D( double res0, double res1){
	SurfZPlane s = (SurfZPlane)vt.surface();
	double z     =  s.z();

	double sig0 = res0;
	double sig1 = res1;

	double m0 = vt.vector(0) + sig0*ran.nextGaussian();
	double m1 = vt.vector(1) + sig1*ran.nextGaussian();
	//double m0 = vt.vector(0);
	//double m1 = vt.vector(1);

	double dxdy = 0.;
	int mcid = 0;
	ClusZPlane2 cluster = new ClusZPlane2( z, m0, m1, sig0*sig0, sig1*sig1, dxdy, mcid );
	TrackError  verr = new TrackError();
	verr.set(0,0,10.);
	verr.set(1,1,10.);
	verr.set(2,2,10.);
	verr.set(3,3,10.);
	verr.set(4,4,10.);

	ETrack et =  new ETrack(s.newPureSurface(), vt.vector(), verr );
	List ghits = cluster.predict(et,cluster);
	ghit = (HitZPlane2)ghits.get(0);
	return ghit;

    }

    private Hit MakeNullCyl( ){
	SurfCylinder s = (SurfCylinder)vt.surface();
	double r = s.radius();

	ghit = new HitNull();

	return ghit;
    }



}
