package org.hps.recon.tracking.kalman.util;

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trffit.HTrack;


/**
 * 
 * A utility used to debug precision problems with
 * inwards going fits.  It resets the z dependent parts
 * of the covariance matrix of a track to the values 
 * that it had at the start of the fit.
 * 
 * This is used to effectively do a 2-D circle fit instead
 * of a 3D helix fit but throwing out the z information
 * after each hit has been added.
 *
 * For this to work properly the track must start
 * at the SurfCyl surface but I never put a check in the
 * code for this.
 *
 * Usage:
 * - at the start of a fit, instantiate a new RKZot object.
 * - whenever you feel like throwing out the z information
 *   call the Zot method.
 *
 *@author $Author: jeremy $
 *@version $Id: RKZot.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 *
 */


public class  RKZot{

    static final int IPHI = SurfCylinder.IPHI;
    static final int IZ   = SurfCylinder.IZ;
    static final int IALF = SurfCylinder.IALF;
    static final int ITLM = SurfCylinder.ITLM;
    static final int IQPT = SurfCylinder.IQPT;

    TrackError e0;

    public RKZot( HTrack h0 ){
	ETrack et =h0.newTrack();

	// Do I want to check that this is a SurfCyl?
	
	e0 = et.error();

    }
    
    public void Zot( HTrack h  ){
	ETrack et = h.newTrack();

	if ( !(et.surface() instanceof SurfCylinder) ){
	    System.out.println ("Error RKZot2: Not a cylinder ... " );
	    return;
	}

	TrackError err = et.error();

	err.set(   IZ,   IZ, e0.get(  IZ,  IZ) );
	err.set( ITLM, ITLM, e0.get(ITLM,ITLM) );

	err.set(   IZ, IPHI, 0. );
	err.set(   IZ, IALF, 0. );
	err.set(   IZ, ITLM, 0. );
	err.set(   IZ, IQPT, 0. );

	err.set( IPHI,   IZ, 0. );
	err.set( IALF,   IZ, 0. );
	err.set( ITLM,   IZ, 0. );
	err.set( IQPT,   IZ, 0. );

	err.set( ITLM, IPHI, 0. );
	err.set( ITLM, IALF, 0. );
	err.set( ITLM, IQPT, 0. );

	err.set( IPHI, ITLM, 0. );
	err.set( IALF, ITLM, 0. );
	err.set( IQPT, ITLM, 0. );

	et.setError( err ); 
	h.setFit( et, h.chisquared() ); 
    }

}
           
