package org.lcsim.hps.recon.tracking.kalman.util;

import java.util.List;
import java.util.Random;
import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfcyl.ThinCylMs;
import org.lcsim.recon.tracking.trfcyl.ThinCylMsSim;
import org.lcsim.util.aida.AIDA;

/**
 * Propagate a track through a detector and generate hits.
 * Include multiple scattering and eloss, or not, as controlled by
 * the configuration file.
 *
 *@author $Author: jeremy $
 *@version $Id: RKMakeHits.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */


public class RKMakeHits {
    
    // Copies of arguments to c'tor.
    // The geometry summary extracted from the GeomConverter classes.
    RKGeom rkgeom = null;

    // A random number generator to use.
    Random random;

    private AIDA aida = AIDA.defaultInstance();

    // TRF wants distances in cm, not mm.
    private double mmTocm = 0.1;

    // Thicknesses in radiation lengths of the 3 classes of cylindrical surfaces.
    private double scatThick0 = 0.006136;
    private double scatThick1 = 0.000916;
    private double scatThick2 = 0.013747;

    // Simulators for scattering at the 3 classes of cylindrical surfaces
    // and 2 classes of z surfaces.
    private ThinCylMsSim scatSim0     = null;
    private ThinCylMsSim scatSim1     = null;
    private ThinCylMsSim scatSim2     = null;
    private ThinZPlaneMsSim zscatSim1 = null;
    private ThinZPlaneMsSim zscatSim2 = null;

    // Simulators for energy loss at the 3 classes of cylindrical surfaces
    // and 2 classes of z surfaces.
    private CylElossSim celoss0 = null;
    private CylElossSim celoss1 = null;
    private CylElossSim celoss2 = null;
    private CylElossSim zeloss1 = null;
    private CylElossSim zeloss2 = null;

    // The run time configuration system.
    ToyConfig config;

    // Quantities taken from the run time configuration system.
    private boolean doms       = false;
    private boolean doeloss    = false;
    private double  msfac      = 1.0;
    private double  dedxscale  = 7.5;
    private double  dedxsigma  = 0.00;
    private boolean doHitCheck = false;

    public RKMakeHits( RKGeom rkgeom, Random random ){
	this.rkgeom = rkgeom;
	this.random = random;

	// Copy information from the run time configuration system.
	try{
	    ToyConfig config = ToyConfig.getInstance();
	    doms                 = config.getBoolean("DoMS");
	    doeloss              = config.getBoolean("DoELoss");
	    msfac                = config.getDouble("MsFac");
	    dedxscale            = config.getDouble("dEdXScale");
	    dedxsigma            = config.getDouble("dEdXSigma");
	    doHitCheck           = config.getBoolean("doHitCheck");

	} catch (ToyConfigException e){
            System.out.println (e.getMessage() );
            System.out.println ("Stopping now." );
            System.exit(-1);
        }
	System.out.println ("RKMakeHits dedxscale: " + dedxscale );

	// Seed for track random number generator.
	long seed          = -398783512;

	// An increment used to generate independent streams of random
	// numbers - I have no proof that this really works.
	long seedIncrement = 876633217;

	CylEloss.SetNewModel(true);

	scatSim0 = new ThinCylMsSim(scatThick0*msfac);
	scatSim1 = new ThinCylMsSim(scatThick1*msfac);
	scatSim2 = new ThinCylMsSim(scatThick2*msfac);

	zscatSim1 = new ThinZPlaneMsSim(scatThick1*msfac);
	zscatSim2 = new ThinZPlaneMsSim(scatThick2*msfac);
	
	// Seed the random number generators in the Siminteractors.
	seed += seedIncrement;
	scatSim0.setSeed(seed);
	seed += seedIncrement;
	scatSim1.setSeed(seed);
	seed += seedIncrement;
	scatSim2.setSeed(seed);
	seed += seedIncrement;
	zscatSim1.setSeed(seed);
	seed += seedIncrement;
	zscatSim2.setSeed(seed);

	double densityBe = rkgeom.getCylinders().get(0).density;
	double densitySi = rkgeom.getCylinders().get(1).density;
	RKDeDxFixed dedxBe = new RKDeDxFixed(densityBe,dedxscale, dedxsigma);
	RKDeDxFixed dedxSi = new RKDeDxFixed(densitySi,dedxscale, dedxsigma);

	double thickbeampipe = rkgeom.getCylinders().get(0).thick;
	double thickpixel    = rkgeom.getCylinders().get(1).thick;
	double thickstrip    = rkgeom.getCylinders().get(6).thick;

	celoss0 = new CylElossSim( thickbeampipe, dedxBe, random );
	celoss1 = new CylElossSim( thickpixel, dedxSi, random  );
	celoss2 = new CylElossSim( thickstrip, dedxSi, random );

	zeloss1 = new CylElossSim( thickpixel, dedxSi, random );
	zeloss2 = new CylElossSim( thickstrip, dedxSi, random );
	System.out.println( "ELoss thicknesses: " + thickbeampipe + " " + thickpixel + " " + thickstrip );
	System.out.println( "Densities: " + densityBe + " " + densitySi );


	// This is probably not necessary - check later.
	seed += seedIncrement;
	celoss0.setSeed(seed);
	seed += seedIncrement;
	celoss1.setSeed(seed);
	seed += seedIncrement;
	celoss2.setSeed(seed);
	seed += seedIncrement;
	zeloss1.setSeed(seed);
	seed += seedIncrement;
	zeloss2.setSeed(seed);

    }

    // Generate perfect hits on the outward going arc only.
    // Do not apply energy loss or multiple scattering.
    public RKHitList GenerateOneArcHits( RKTrack rktrk ){

	// Convert to TRF style track.
	toTRF trftrk = new toTRF(rktrk);
	VTrack vtdca = trftrk.atDcaZaxis();
	VTrack vtz   = trftrk.atZPlane(rktrk.z0());

	// This code only looks at the first arc of a track.
	double pathlimit = trftrk.halfarc();
	int nmeas  = 0;
	int nzmeas = 0;
	int ncmeas = 0;
	RKHitList rkl = new RKHitList();
	Propagator prop = rkgeom.newPropagator();
	double sum = 0.;
	for ( RKSurf s: rkgeom.getCylinders() ){
	    PropStat ps = prop.vecDirProp( vtdca, s.getCylinder(), PropDir.FORWARD);
	    if ( ps.success() ){
		sum += ps.pathDistance();
		if ( sum > pathlimit ) break;
		if ( s.inBounds(vtdca.vector(1)) ){
		    rkl.addHit( new RKHit( rktrk, s, vtdca, ps, sum ) );
		    aida.cloud2D( "HitGenNoScat/C r vs z",-1).fill( vtdca.vector(1), s.radius*mmTocm );
		    aida.cloud2D( "HitGenNoScat/Both r vs z",-1).fill( vtdca.vector(1), s.radius*mmTocm );
		    double r = s.radius*mmTocm;
		    /*
		    System.out.printf ("Adding Cyl: %10.4f %10.4f %10.4f %10.4f %10.4f %10.4f\n",
				       r*Math.cos(vtdca.vector(0)),
				       r*Math.sin(vtdca.vector(0)),
				       r,
				       vtdca.vector(1),
				       sum,
				       vtdca.vector(3)
				       );
		    */
		    nmeas  += s.rodim;
		    ncmeas += s.rodim;
		}
	    }
	}
	aida.cloud2D("HitGenNoScat/Path length vs cz",-1).fill(rktrk.cz(),sum);
	aida.cloud1D("HitGenNoScat/Sum").fill(sum);
	aida.cloud2D("HitGenNoScat/Hits vs cz",-1).fill( rktrk.cz(), ncmeas );

	sum = 0;
	for ( RKSurf s: rkgeom.getZ( rktrk.z0(), rktrk.cz() ) ){
	    PropStat ps = prop.vecDirProp( vtz, s.getZPlane(), PropDir.FORWARD );
	    if ( ps.success() ){
		sum += ps.pathDistance();
		if ( sum > pathlimit ) break;
		double r = Math.sqrt(vtz.vector(0)*vtz.vector(0) + vtz.vector(1)*vtz.vector(1) );
		if ( s.inBounds(r) ){
		    rkl.addHit( new RKHit( rktrk, s, vtz, ps, sum ) );
		    aida.cloud2D( "HitGenNoScat/Z r vs z",-1).fill( s.zc*mmTocm, r );
		    aida.cloud2D( "HitGenNoScat/Both r vs z",-1).fill( s.zc*mmTocm, r );
		    nmeas += s.rodim;
		    nzmeas += s.rodim;
		    /*
		    System.out.printf ("Adding Z:   %10.4f %10.4f %10.4f %10.4f %10.4f\n",
				       vtz.vector(0),
				       vtz.vector(1),
				       r,
				       s.zc*0.1,
				       sum );
		    */
		}
	    }	    
	}
	aida.cloud2D("HitGenNoScat/Z Hits vs cz",-1).fill( rktrk.cz(), nzmeas );
	aida.cloud2D("HitGenNoScat/Nmeas vs cz",-1).fill( rktrk.cz(), nmeas );
	aida.cloud2D("HitGenNoScat/Nz vs Nc",-1).fill( ncmeas, nzmeas );

	return rkl;
    }

    // Generate perfect hits on the outward going arc only.
    // Do not apply energy loss or multiple scattering.
    public RKHitList GenerateMixedOneArcHits( RKTrack rktrk ){

	// Convert to TRF style track.
	toTRF trftrk = new toTRF(rktrk);
	VTrack vtdca = trftrk.atDcaZaxis();

	// This code only looks at the first arc of a track.
	double pathlimit = trftrk.halfarc();

	int nmeas  = 0;
	int nzmeas = 0;
	int ncmeas = 0;
	double sum = 0.;
	RKHitList rkl = new RKHitList();
	Propagator prop = rkgeom.newPropagator();

	List<RKSurf> cyls = rkgeom.getCylinders();
	List<RKSurf> zps  = rkgeom.getZ( rktrk.z0(), rktrk.cz() );

	int next_cyl = 0;
	int next_zp  = 0;
	int max_cyl  = cyls.size();
	int max_zp   = zps.size();

	double sumde = 0;

	// At top of the loop, vtdca contains the track parameters at the 
	// departure point for this step.
	while ( (next_cyl<max_cyl) || ( next_zp < max_zp ) ){

	    VTrack vt_cyl = null;
	    VTrack vt_zp  = null;
	    
	    double path_cyl = 0.;
	    double path_zp  = 0.;

	    int status_cyl  = 0;
	    int status_zp   = 0;

	    RKSurf rks_cyl  = null;	    
	    RKSurf rks_zp   = null;	    

	    PropStat ps_cyl = null;
	    PropStat ps_zp  = null;

	    double r_cyl = 0.;
	    double z_cyl = 0.;
	    double r_zp  = 0.;
	    double z_zp  = 0.;

	    int next_cyl_sav = next_cyl;
	    int next_zp_sav  = next_zp;

	    // Check cylindrical surfaces until the next good one is found.
	    //while ( next_cyl < -1 ){
	    while ( next_cyl < max_cyl ){

		// Default status is failure.
		status_cyl = 0;
		
		// Parameters at the starting surface for this step.
		vt_cyl  = new VTrack(vtdca);

		// Move to the next surface.
		rks_cyl = cyls.get(next_cyl);
		ps_cyl  = prop.vecDirProp( vt_cyl, rks_cyl.getCylinder(), PropDir.FORWARD);

		// If we do not get to this cylinder, try the next one.
		// This can happen if starting point is outside this one but inside a later one.
		if ( !ps_cyl.success() ) {
		    ++next_cyl;
		    continue;
		}

		// If we moved backwards, try the next cylinder outward.
		path_cyl = ps_cyl.pathDistance();
		if ( path_cyl < 0. ) {
		    //System.out.println ("Negative path at cyl..." );
		    status_cyl = 3;
		    ++next_cyl;
		    continue;
		}
		status_cyl = 1;

		// Needed for downstream diagnostics.
		r_cyl = rks_cyl.radius*mmTocm;
		z_cyl = vt_cyl.vector(1);

		if ( rks_cyl.inBounds(z_cyl) ){

		    // We have the next good surface from this list.
		    status_cyl = 2;
		    break;

		} else{

		    // If not in bounds, try the next surface.
		    ++next_cyl;
		    continue;
		}
		
	    }
	    
	    // Check the next z surface.
	    while ( next_zp < max_zp ){

		// Default status is failure.
		status_zp = 0;

		// Parameters at the start of this step.
		vt_zp  = new VTrack(vtdca);

		// Move to the next surface.
		rks_zp = zps.get(next_zp);
	        ps_zp  = prop.vecDirProp( vt_zp, rks_zp.getZPlane(), PropDir.FORWARD );


		// This happens when the z surface being tested is behind the starting point.
		// Try the next surface.
		if ( !ps_zp.success() ) {
		    ++next_zp;
		    continue;
		}

		// Normally this will not happen.  If it does, try the next surface.
		path_zp = ps_zp.pathDistance();
		if ( path_zp < 0. ) {
		    status_zp = 3;
		    ++next_zp;
		    continue;
		}
		status_zp = 1;

		// Needed for downstream diagnostics
		r_zp = Math.sqrt(vt_zp.vector(0)*vt_zp.vector(0) + 
				 vt_zp.vector(1)*vt_zp.vector(1) );
		z_zp = rks_zp.zc*mmTocm;

		if ( rks_zp.inBounds(r_zp) ){

		    // We have the next good surface from this list.
		    status_zp = 2;
		    break;
		}else{

		    // Out of bounds so try the next surface.
		    ++next_zp;
		    continue;
		}

	    }

	    // At this point we have zero, one or two good solutions.
	    // If two, choose the one with the shortest step length.
	    boolean addcyl = false;
	    boolean addzp  = false;
	    if ( status_cyl == 2 && status_zp == 2 ){
		if (  path_cyl < path_zp ) {
		    addcyl = true;
		} else {
		    addzp = true;
		}
	    } else if ( status_cyl == 2 ){
		addcyl = true;

	    } else if ( status_zp == 2 ){
		addzp = true;
	    } else {
		
		// This should only happen when both lists are exhausted.
		// Or when one of the hit types is turned off.
		if ( next_cyl<max_cyl || next_zp < max_zp ){
		    System.out.println ("Don't know how I got here ... " 
					+ next_cyl + " "
					+ max_cyl  + " "
					+ next_zp  + " "
					+ max_zp
					);
		}
	    }
	    

	    // Add the chosen hit.
	    if ( addcyl ){

		// Advance counter to next surface.
		++next_cyl;

		// Restore the z index to it's position at the start of this step.
		next_zp = next_zp_sav;
		
		// Did we exceed the half-arc limit?
		sum += path_cyl;
		if ( sum > pathlimit ) break;

		// Track parameters at this step.
		vtdca = new VTrack(vt_cyl);

		// Some diagnostics
		aida.cloud2D( "HitGen/C r vs z",-1).fill( z_cyl, r_cyl );
		aida.cloud2D( "HitGen/Both r vs z",-1).fill( z_cyl, r_cyl);

		// Bookkeeping.
		nmeas  += rks_cyl.rodim;
		ncmeas += rks_cyl.rodim;

		if ( doms ){

		    // Interact.
		    // Should modify this to not interact if this is the last point on the outward trace.
		    VTrack test = new VTrack(vtdca);
		    if ( r_cyl < 1.3 ){
			scatSim0.interact(vtdca);
		    } else if ( r_cyl < 10. ){
			scatSim1.interact(vtdca);
		    }else{
			scatSim2.interact(vtdca);
		    }


		    // Diagnostics.
		    double scatThick = 0.;
		    if ( r_cyl < 1.3 ){
			scatThick = scatThick0;
		    }else if ( r_cyl < 10. ){
			scatThick = scatThick1;
		    } else{
			scatThick = scatThick2;
		    }
		    ThinCylMs scat1  = new ThinCylMs(scatThick);
		    TrackError vtmp  = new TrackError();
		    ETrack et  = new ETrack( test.surface().newPureSurface(), test.vector(), vtmp);
		    ETrack ets = new ETrack(et);
		    scat1.interact(et);

		    double dd = vtdca.vector().get(2)-test.vector().get(2);
		    double ee = et.error().get(2,2);
		    double r = dd/Math.sqrt(ee);
		    aida.cloud1D( "HitGen/Scat pull").fill(r);
		    aida.histogram1D("HitGen/Gen scat radius", 300, 0., 150.).fill(r_cyl);

		}

		// Add the hit.
		rkl.addHit( new RKHit( rktrk, rks_cyl, vtdca, ps_cyl, sum ) );


	    } else if ( addzp ) {

		// Advance counter to next surface.
		++next_zp;

		// Restore the cylinder index to it's position at the start of this step.
		next_cyl = next_cyl_sav;

		// Did we exceed the half-arc limit?
		sum += path_zp;
		if ( sum > pathlimit ) break;

		// Track parameters at this step.
		vtdca = vt_zp;


		// Some diagnostics
		aida.cloud2D( "HitGen/Z r vs z",-1).fill( z_zp, r_zp );
		aida.cloud2D( "HitGen/Both r vs z",-1).fill( z_zp, r_zp );

		// Bookkeeping.
		nmeas += rks_zp.rodim;
		nzmeas += rks_zp.rodim;

		if ( doms ){

		    // Interact.
		    // Should modify this to not interact if this is the last point on the outward trace.
		    VTrack test = new VTrack(vtdca);
		    if ( Math.abs(z_zp) < 25. ){
			zscatSim1.interact(vtdca);
		    }else{
			zscatSim2.interact(vtdca);
		    }
		    aida.histogram1D("HitGen/Gen scat z", 340, -170., 170.).fill(z_zp);

		}

		// Add the hit.
		rkl.addHit( new RKHit( rktrk, rks_zp, vtdca, ps_zp, sum ) );
		

	    } else {

		// Neither intersected but try next surfaces for both lists.
		++next_cyl;
		++next_zp;
	    }

	}

	// More diagnostics.
	aida.cloud2D("HitGen/Path length vs cz",-1).fill(rktrk.cz(),sum);
	aida.cloud2D("HitGen/C Hits vs cz",-1).fill( rktrk.cz(), ncmeas );
	aida.cloud2D("HitGen/Z Hits vs cz",-1).fill( rktrk.cz(), nzmeas );
	aida.cloud2D("HitGen/Nmeas vs cz",-1).fill( rktrk.cz(), nmeas );
	aida.cloud2D("HitGen/Nz vs Nc",-1).fill( ncmeas, nzmeas );

	aida.cloud1D("HitGen/Generated sumde:",-1).fill(sumde);
	aida.histogram1D("HitGen/Generated sumde hist", 100, 0., 20.).fill(sumde*1000.);
	RKDebug.Instance().setDeGen(sumde);

	// If requested, also generate hits using the old algorithm and check that they
	// agree with those from the new algorithm.  This only makes sense if ms and eloss
	// are disabled.
	if ( doHitCheck && !( doeloss || doms ) ){
	    CheckHitList( rkl, rktrk );
	}

	return rkl;
    }


    // Check that the Mixed one arc hit generator matches the simple one.
    // Only valid when scattering and eloss are turned off.
    void CheckHitList( RKHitList hlist, RKTrack rktrk ){


	RKHitList hlist2 = GenerateOneArcHits( rktrk );
	int d1 = hlist.nHits() - hlist2.nHits();
	int d2 = hlist.nCyl() - hlist2.nCyl();
	int d3 = hlist.nZp()  - hlist2.nZp();
	int d4 = hlist.nDof() - hlist2.nDof();

	if ( (d1==0) && (d2==0) && (d3==0) && (d4 ==0) ){
	    List<RKHit> l1 = hlist.getForward();
	    List<RKHit> l2 = hlist2.getForward();
	    for ( int i=0;i<l1.size(); ++i ){
		RKHit h1 = l1.get(i);
		RKHit h2 = l2.get(i);
		double diff = h2.getPath()-h1.getPath();
		aida.cloud1D("HitCheck/Difference in Path length: ").fill(diff);
		
	    }
	}else{
	    System.out.println ( "Diffs: " + d1 + " " +  d2 + " " + d3 + " " + d4 + " " + rktrk.cz() );
	    System.out.println ( "Hits1: " 
				 + hlist2.nHits() + " " 
				 + hlist2.nCyl() + " " 
				 + hlist2.nZp() + " " 
				 + hlist2.nDof() + " " 
				 );
	    System.out.println ( "Hits2: " 
				 + hlist.nHits() + " " 
				 + hlist.nCyl() + " " 
				 + hlist.nZp() + " " 
				 + hlist.nDof() + " " 
				 );
	    int i=0;
	    for ( RKHit h: hlist.getForward() ){
		System.out.printf ( "Hits1: %3d %-20s %10.4f %10.4f\n",
				    i++,
				    h.getSurface().getTypeAsString(),
				    h.getSurface().radius,
				    h.getSurface().zc
				    );
	    }
	    i=0;
	    for ( RKHit h: hlist2.getForward() ){
		System.out.printf ( "Hits2: %3d %-20s %10.4f %10.4f\n",
				    i++,
				    h.getSurface().getTypeAsString(),
				    h.getSurface().radius,
				    h.getSurface().zc
				    );
	    }

	}

    }

}

