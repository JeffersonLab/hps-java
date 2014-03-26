package org.hps.recon.tracking.kalman.util;

import java.util.Iterator;
import java.util.List;

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Hit;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfcyl.HitCylPhi;
import org.lcsim.recon.tracking.trfcyl.HitCylPhiZ2D;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfcyl.ThinCylMs;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trffit.FullFitter;
import org.lcsim.recon.tracking.trffit.HTrack;
import org.lcsim.recon.tracking.trfzp.HitZPlane1;
import org.lcsim.recon.tracking.trfzp.HitZPlane2;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;
import org.lcsim.recon.tracking.trfzp.ThinZPlaneMs;
import org.lcsim.util.aida.AIDA;

/**
 *
 * Copied from org.lcsim.recon.tracking.trffit.FullFitKalman
 *   - added fitForward() and fitBackward() methods.
 *   - added debug printout
 *
 * Full track fit using Kalman filter.  The propagator is specified
 * when the fitter is constructed.  The starting surface, vector and
 * error matrix are taken from the input track.  Errors should be
 * increased appropriately if the fitter is applied repeatedly to
 * a single track.
 *
 *@author $Author: jeremy $
 *@version $Id: FullFitKalman.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

public class FullFitKalman extends FullFitter
{

    private AIDA aida = AIDA.defaultInstance();

    // Flags to control: multiple scattering, energy loss and adding the hit.
    private boolean doMs    = true;
    private boolean doEloss = true;
    private boolean doMeas  = true;

    private double dedxscale = 1.;
    private double dedxsigma = 0.0;
    
    // static methods
    
    //
    
    /**
     *Return a String representation of the class' the type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public static String typeName()
    { return "FullFitKalman"; }
    
    //
    
    /**
     *Return a String representation of the class' the type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public static String staticType()
    { return typeName(); }
    
    // The propagator.
    private Propagator _pprop;
    
    // The add fitter.
    private AddFitKalman _addfit;
   
    int AddFitKalmanDebugLevel = 0;
    //
    
    /**
     *Construct an instance specifying a propagator.
     *
     * @param   prop The Propagator to be used during the fit.
     */
    public FullFitKalman(Propagator prop)
    {
        _pprop = prop;
        _addfit = new AddFitKalman();

	try{
	    ToyConfig config = ToyConfig.getInstance();
	    AddFitKalmanDebugLevel = config.getInt( "AddFitKalmanDebugLevel", 
						    AddFitKalmanDebugLevel );
	    dedxscale = config.getDouble("dEdXScale");
	    dedxsigma = config.getDouble("dEdXSigma");
	    

	} catch (ToyConfigException e){
            System.out.println (e.getMessage() );
            System.out.println ("Stopping now." );
            System.exit(-1);
        }
	System.out.println ("FullfitKalman dedxscale: " + dedxscale );

    }
    
    //
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public String type()
    { return staticType(); }
    
    //
    
    /**
     *Return the propagator.
     *
     * @return The Propagator used in the fit.
     */
    public  Propagator propagator()
    { return _pprop; }
    
    //

    public void setDoMs( boolean b){
	doMs = b;
    }

    public void setDoEloss( boolean b){
	doEloss = b;
    }
    
    /**
     *Fit the specified track.
     *
     * @param   trh The HTrack to fit.
     * @return 0 if successful.
     */
    public int fit(HTrack trh)
    {
        // Copy the hits from the track.
        List hits = trh.hits();
        //System.out.println("Hits has "+hits.size()+" elements");
        // Delete the list of hits from the track.
        while ( trh.hits().size()>0 ) trh.dropHit();
        //System.out.println("Hits has "+hits.size()+" elements");
        
        // Set direction to be nearest.
        //PropDir dir = PropDir.NEAREST;
        PropDir dir = PropDir.FORWARD;
	RKDebug.Instance().setPropDir(dir);
        
        // Loop over hits and fit.
        int icount = 0;
        for ( Iterator ihit=hits.iterator(); ihit.hasNext(); )
        {

            // Extract the next hit pointer.
            Hit hit = (Hit)ihit.next();
            //System.out.println("Hit "+icount+" is: \n"+hit);
            // propagate to the surface
	    //System.out.println ("Before prop: " + trh.newTrack() );
            PropStat pstat = trh.propagate(_pprop,hit.surface(),dir);
            
	    //System.out.println ("After prop: " + trh.newTrack() );
            // fit track
            //System.out.println("trh= \n"+trh+", hit= \n"+hit);
            //System.out.println("_addfit= "+_addfit);
            int fstat = _addfit.addHit(trh,hit);
	    //System.out.println ("After addhit: " + fstat + " " 
	    //		+ trh.chisquared() + "\n" + trh.newTrack() );
            if ( fstat>0 ) return 10000 + 1000*fstat + icount;
            
        }
        return 0;
        
    }
    
    public int fitForward(HTrack trh)
    {

        PropDir dir = PropDir.FORWARD;
	RKDebug.Instance().setPropDir(dir);

        // Copy the hits from the track.
        List hits = trh.hits();

        // Delete the list of hits from the track.
        while ( trh.hits().size()>0 ) trh.dropHit();
       
	double sumde = 0.;
        
        // Loop over hits and fit.
        int icount = 0;
        for ( Iterator ihit=hits.iterator(); ihit.hasNext(); )
        {
	    Surface s_save = trh.newTrack().surface().newPureSurface();
	    ETrack e_save = trh.newTrack();

            // Extract the next hit pointer.
            Hit hit = (Hit)ihit.next();

	    int from = (new SurfaceCode(s_save)).getCode();
	    int to   = (new SurfaceCode(hit.surface())).getCode();

	    // Propagate to the next surface.
            PropStat pstat = trh.propagate(_pprop,hit.surface(),dir);
            if ( ! pstat.success() ) {
		if ( AddFitKalmanDebugLevel > 0 ) {
		    System.out.println ("Error:        "  
					+ RKDebug.Instance().getTrack() + " " 
					+ RKDebug.Instance().getPropDir() + " " 
					+ icount
					);
		    System.out.println ("From surface 5: " + s_save );
		    System.out.println ("To surface 5:   " + hit.surface());
		    System.out.println ("Params: " + e_save.vector() );
		}
		aida.histogram1D("/Bugs/Fit/Failed Fwd prop from Surface",5,0,5).fill( from );
		aida.histogram1D("/Bugs/Fit/Failed Fwd prop to Surface",5,0,5).fill( to  );
		aida.cloud2D("/Bugs/Fit/Failed Fwd prop to vs from Surface").fill( from, to  );

		return icount+1;
	    }
	    
	    //if ( icount != 0 ) {
	    //	int istat = interact( trh, hit, dir );
	    //}


	    // Add the hit.
            int fstat = _addfit.addHit(trh,hit);
	    if ( fstat>0 ){
		if ( AddFitKalmanDebugLevel > 0){
		
		    System.out.println ("Error:        "  
					+ RKDebug.Instance().getTrack() + " " 
					+ RKDebug.Instance().getPropDir() + " " 
					);
		    System.out.println ("From surface 4: " + s_save );
		    System.out.println ("To surface 4:   " + hit.surface());		
		}
		aida.histogram1D("/Bugs/Fit/Failed Fwd addhit from Surface",5,5,5).fill( from );
		aida.histogram1D("/Bugs/Fit/Failed Fwd addhit to Surface",5,0,5).fill( to  );
		aida.cloud2D("/Bugs/Fit/Failed Fwd addhit to vs from Surface").fill( from, to  );

	    }
            if ( fstat>0 ) return 10000 + 1000*fstat + icount;

	    VTUtil before = new VTUtil( trh.newTrack() );
	    int istat = interact( trh, hit, dir );
	    VTUtil after = new VTUtil( trh.newTrack() );

	    double de = before.e() - after.e();
	    sumde += de;

	    //SurfCylinder ss = (SurfCylinder)trh.newTrack().surface();
	    //System.out.printf ("Forw dedx: %10.4f %12.8f  %12.8f\n", ss.radius(), de, sumde );


            ++icount;
        }


	//System.out.println ("Forward fit sumde: " + sumde );
	aida.cloud1D("Forward dedx check:").fill(sumde-RKDebug.Instance().getDeGen());

        return 0;
        
    }

    public int fitBackward(HTrack trh)
    {
        PropDir dir = PropDir.BACKWARD;
	RKDebug.Instance().setPropDir(dir);

	//RKPrintSymMatrix psm = new RKPrintSymMatrix();

        // Copy the hits from the track.
        List hits = trh.hits();

        // Delete the list of hits from the track.
        while ( trh.hits().size()>0 ) trh.dropHit();

	double chold = 0.;

	double sumde = 0.;

	RKZot zot = new RKZot(trh);
	boolean zottable = true;

	int nc = 0;
	int nz = 0;
	int nu = 0;
	String thishit;


        // Loop over hits and fit.
        int icount = 0;
        for ( Iterator ihit=hits.iterator(); ihit.hasNext(); )
        {
            // Extract the next hit pointer.
            Hit hit = (Hit)ihit.next();

	    Surface s_save = trh.newTrack().surface().newPureSurface();

	    int from = (new SurfaceCode(s_save)).getCode();
	    int to   = (new SurfaceCode(hit.surface())).getCode();

	    /*
	    Surface s_new  = hit.surface();
	    if ( s_new instanceof SurfCylinder ){
		System.out.printf ("Next: %3d Cylinder\n",icount);
		++nc;
		thishit = "Cylinder";
	    } else if ( s_new instanceof SurfZPlane ){
		System.out.printf ("Next: %3d ZPlane\n",icount);
		++nz;
		thishit = "ZPlane";
		zottable = false;
	    } else {
		System.out.printf ("Next: %3d Unknown\n",icount);
		++nu;
		thishit = "Unknown";
	    }
	    */
	    if ( hit instanceof HitCylPhi ){
		//System.out.printf ("Next: %3d Cylinder 1D\n",icount);
		++nc;
		thishit = "Cylinder1D";
	    } else if ( hit instanceof HitCylPhiZ2D ){
		//System.out.printf ("Next: %3d Cylinder 2D\n",icount);
		++nc;
		++nz;
		zottable = false;
		thishit = "Cylinder2D";
	    } else if ( hit instanceof HitZPlane1 ){
		//System.out.printf ("Next: %3d ZPlane 1D\n",icount);
		++nc;
		++nz;
		zottable = false;
		thishit = "ZPlane1D";
	    } else if ( hit instanceof HitZPlane2 ){
		//System.out.printf ("Next: %3d ZPlane 2D\n",icount);
		++nc;
		++nz;
		zottable = false;
		thishit = "ZPlane2D";
	    } else{
		System.out.printf ("Next: %3d Unknown\n",icount);
		thishit = "Unknown";
	    }
	    //System.out.println("Error before prop: ");
	    //psm.Print(trh.newTrack());

            PropStat pstat = trh.propagate(_pprop,hit.surface(),dir);

            if ( ! pstat.success() ) {
		if ( AddFitKalmanDebugLevel > 0 ){
		    System.out.println ("Error:        "  
					+ RKDebug.Instance().getTrack() + " " 
					+ RKDebug.Instance().getPropDir() + " " 
					+ icount
					);
		    System.out.println ("From surface 1: " + s_save );
		    System.out.println ("To surface 1:   " + hit.surface());
		    System.out.println ("Failed prop: " + nc + " " + nz + " " + nu + " " + thishit );
		}

		aida.histogram1D("/Bugs/Fit/Failed prop from Surface",5,0,5).fill( from );
		aida.histogram1D("/Bugs/Fit/Failed prop to Surface",5,0,5).fill( to  );
		aida.cloud2D("/Bugs/Fit/Failed prop to vs from Surface").fill( from, to  );

		return icount+1;
	    }
	    //System.out.println("Error after prop: ");
	    //psm.Print(trh.newTrack());

	    if ( zottable ){
	    	zot.Zot(trh);
		//System.out.println("Error after zot: ");
		//psm.Print(trh.newTrack());
	    }
	    

	    //if ( icount != 0 ) {
	    //int istat = interact( trh, hit, dir );
	    //}
	    //VTUtil before = new VTUtil( trh.newTrack() );
	    //int istat = interact( trh, hit, dir );
	    //VTUtil after = new VTUtil( trh.newTrack() );


	    // Add the hit.
            int fstat = _addfit.addHit(trh,hit);

	    //System.out.println ("Hit info: " + hit );


	    if ( fstat>0 ){
		if ( AddFitKalmanDebugLevel > 0){
		    System.out.println ("Error:        "  
					+ RKDebug.Instance().getTrack() + " " 
					+ RKDebug.Instance().getPropDir() + " " 
					);
		    System.out.println ("From surface 2: " + s_save );
		    System.out.println ("To surface 2:   " + hit.surface());		
		    System.out.println ("Failed addhit: " + nc + " " + nz + " " + nu + " " + thishit );
		}
		aida.histogram1D("/Bugs/Fit/Failed addhit from Surface",5,0,5).fill( from );
		aida.histogram1D("/Bugs/Fit/Failed addhit to Surface",5,0,5).fill( to  );
		aida.cloud2D("/Bugs/Fit/Failed addhit to vs from Surface").fill( from, to  );

	    }
            if ( fstat>0 ) return 10000 + 1000*fstat + icount;


	    double chnew = trh.chisquared();
	    double dch = chnew - chold;

	    //System.out.printf("Error after addhit: %15.5f\n", dch);
	    //psm.Print(trh.newTrack());

	    /*
	    if( dch < -0.001 ){
		System.out.println ("From surface 3: " + s_save );
		System.out.println ("To surface 3:   " + hit.surface());				
	    }
	    */
	    chold = chnew;

	    // Save track for diagnostics.
	    VTUtil before = new VTUtil( trh.newTrack() );

	    // Apply multiple scattering and energy loss
	    int istat = interact( trh, hit, dir );

	    // Compute change in energy.
	    VTUtil after = new VTUtil( trh.newTrack() );
	    double de = after.e() - before.e();

	    sumde += de;

	    //SurfCylinder ss = (SurfCylinder)trh.newTrack().surface();
	    //System.out.printf ("Back dedx: %10.4f %12.8f  %12.8f\n", ss.radius(), de, sumde );
            ++icount;
        }

	// Propagate to the beampipe and interact.
	if ( doMs ){

	    // Beampipe parameters for sid02
	    double radius = 1.22;
	    double l_over_radl = 0.006136;

	    SurfCylinder sbp = new SurfCylinder(radius);
	    PropStat pstat = trh.propagate( _pprop, sbp, dir );
	    if ( ! pstat.success() ) return -2;

	    int istat = interactonly( trh, radius, l_over_radl );

	}

	// Propagate to the PCAO.
	SurfDCA sdca = new SurfDCA( 0., 0. );
	PropStat pstat = trh.propagate( _pprop, sdca, dir );
	if ( ! pstat.success() ) return -1;

	//System.out.println ("Backwards fit sumde: " + sumde );
	aida.cloud1D("Backward dedx check:").fill(sumde-RKDebug.Instance().getDeGen());

	//System.out.println("Error at PCAO: ");
	//psm.Print(trh.newTrack());

        return 0;
        
    }

    private int interact ( HTrack trh, Hit hit, PropDir dir ){

	if ( hit.surface().pureType().equals(SurfCylinder.staticType()) ){

	    SurfCylinder s = (SurfCylinder) hit.surface();
	    double r = s.radius();
	    if ( doMs ){
		TrackError eold = trh.newTrack().error(); 
		
		aida.histogram1D("/Bugs/Fit/Fit scat radius:",300,0.,150.).fill(r);
		
		double l_over_radl = 0.;
		if ( r < 1.3 ) {
		    l_over_radl = 0.006136;
		}else if ( r < 10. ){
		    l_over_radl = 0.000916;
		}else {
		    l_over_radl = 0.013747;
		}

		ThinCylMs scat = new ThinCylMs(l_over_radl*RKDebug.Instance().getMsFac());
		ETrack et = trh.newTrack();
		ETrack ets = new ETrack(et);
		double chnew = trh.chisquared();
		
		scat.interact(et);
		hit.update(et);
		trh.setFit(et,chnew);

		/*
		  for ( int i=0; i<5; ++i ){
		  double ex1 = et.error().get(i,i);
		  double ex2 = ets.error().get(i,i);
		  double sigsq = ex1-ex2;
		  //double pull = -9.;
		  double sig=-1.;
		  if ( sigsq > 0. ){
		  sig = Math.sqrt(sigsq);
		  //pull = (et.vector(i)-ets.vector(i))/sig;
		  }
		  aida.cloud1D("/Bugs/Fit/Forward Fit Delta param:"+i).fill(et.vector(i)-ets.vector(i));
		  if ( sig > 0. ) aida.cloud1D("/Bugs/Fit/Forward Fit Delta error:"+i).fill(sig);
		  }
		  //System.out.println( "Error after: " + trh.newTrack().error().minus(eold) );
		  */
	    } // end if MS enabled


	} // end CYlinder MS
	
	if ( hit.surface().pureType().equals(SurfZPlane.staticType()) ){
	    
	    SurfZPlane s = (SurfZPlane) hit.surface();
	    double z = s.z();
	    if ( doMs ){
		TrackError eold = trh.newTrack().error(); 
		
		aida.histogram1D("/Bugs/Fit/Fit scat z forward:",300,-150,150.).fill(z);
		
		double l_over_radl = ( Math.abs(z)< 25) ? 0.000916 : 0.013747;
		ThinZPlaneMs scat = new ThinZPlaneMs(l_over_radl*RKDebug.Instance().getMsFac());
		ETrack et = trh.newTrack();
		//		ETrack ets = new ETrack(et);
		double chnew = trh.chisquared();
		
		scat.interact(et);
		hit.update(et);
		trh.setFit(et,chnew);
		
	    } // end if MS enabled.
	    
	} // end ZPlane MS
	
	// Successful return;
	return 0;
    }


    // There is no hit at this site so we only need to do the interaction, not update hits.
    private int interactonly ( HTrack trh, double r, double l_over_radl ){

	ThinCylMs scat = new ThinCylMs(l_over_radl*RKDebug.Instance().getMsFac());
	ETrack et = trh.newTrack();
	double chnew = trh.chisquared();
	
	scat.interact(et);
	trh.setFit(et,chnew);
	
	// Successful return;
	return 0;
    }
 
    /**
     *output stream
     *
     * @return The String representation of this instance.
     */
    public String toString()
    {
        return getClass().getName();
    }
    
}

