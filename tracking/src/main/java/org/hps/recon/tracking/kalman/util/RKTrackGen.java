package org.hps.recon.tracking.kalman.util;

import java.util.Random;

import org.lcsim.util.aida.AIDA;

/**
 * 
 * A crude track generator.  It emits tracks in the CLEO parameterization.
 *
 *@author $Author: jeremy $
 *@version $Id: RKTrackGen.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

public class RKTrackGen {

    private AIDA aida = AIDA.defaultInstance();

    public RKTrackGen( long seed){
	par= new RKTrackGenParams();
	ran = new Random(seed);
    }

    public RKTrackGen( RKTrackGenParams par, long seed){
	this.par=par;
	ran = new Random(seed);
    }

    public RKTrack newRKTrack (){

	// Generate track parameters in cleo convention.
	double pt    = par.ptmin()  + (par.ptmax()-par.ptmin())*ran.nextDouble();
	double q = 1.;
	if ( par.chargeControl() == -1 ){
	    q = -1;
	}else if ( par.chargeControl() != 1 ){
	    q = ( ran.nextDouble() > 0.5 ) ? 1. : -1.;
	}
	double cz    = par.czmin()  + (par.czmax()-par.czmin())*ran.nextDouble();
	double phi0  = par.phimin() + (par.phimax()-par.phimin())*ran.nextDouble();
	double d0    = par.d0min()  + (par.d0max()-par.d0min())*ran.nextDouble();
	double z0    = par.z0min()  + (par.z0max()-par.z0min())*ran.nextDouble();

	// Use endcap mode for cz.
	if ( par.czEndcapMode ){
	    if ( ran.nextDouble() > 0.5 ) cz = -cz;
	}

	RKTrack t = new RKTrack( q, pt, cz, phi0, d0, z0, par.bz());

	plotGenTrack(t);

	return t;
    }

    // Monitoring histograms for the generated track.
    public void plotGenTrack( RKTrack t ){

	int nbins = 50;

	double d0min = -5.;
	double d0max =  5.;
	double z0min = -15.;
	double z0max =  15.;

	aida.histogram1D("GeneratedParams/charge",5,-2.,2.).fill(t.q());
	aida.histogram1D("GeneratedParams/pt", nbins,0.,25.).fill(t.pt());
	aida.histogram1D("GeneratedParams/cos(theta)", nbins, -1., 1.).fill(t.cz());
	aida.histogram1D("GeneratedParams/phi0",nbins, -Math.PI, Math.PI).fill(t.phi0());
	aida.histogram1D("GeneratedParams/d0", nbins, d0min, d0max).fill(t.d0());
	aida.histogram1D("GeneratedParams/z0", nbins, z0min, z0max).fill(t.z0());
	aida.histogram1D("GeneratedParams/p", nbins, 0., 50.).fill(t.p());
	aida.cloud2D("GeneratedParams/PCA0",-1).fill(t.x0(),t.y0());
	aida.cloud2D("GeneratedParams/D0 vs Z0",-1).fill(t.z0(),t.d0());

    }


    public RKTrackGenParams getParams(){
	return new RKTrackGenParams(par);
    }

    private Random ran = null;
    private RKTrackGenParams par = null;




}
