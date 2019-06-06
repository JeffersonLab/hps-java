package org.hps.recon.tracking.lit;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Norman A Graf
 * 
 *  @version $Id:
 */
public class FixedHitsFitTest extends TestCase
{

    public void testAll()
    {
        double[] zpos = {10., 20., 30., 40., 50., 60., 70.};
        // a constant magnetic field...
        ConstantMagneticField field = new ConstantMagneticField(0., 1., 0.);

        // a propagator...
        CbmLitTrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);

        // a class to convert spacepoints at a plane
        // into either strip or pixel hits.
        PointToPixelHitConverter hitConv = new PointToPixelHitConverter(.0001, .0001, 0.0);

        // a class to smear initial track parameters.
        TrackParameterSmearer smearer = new TrackParameterSmearer(.001, .001, .002, .002, .002);

        // A track state to propagate...
        // start at z=0.

        CbmLitTrackParam trkParam = new CbmLitTrackParam();
        trkParam.SetX(0.);
        trkParam.SetY(0.);
        trkParam.SetZ(0.);
        trkParam.SetTx(0.);
        trkParam.SetTy(0.1);
        trkParam.SetQp(2.);

        System.out.println(trkParam);

        // a container to hold the propagated track states
        List<CbmLitTrackParam> trackStateList = new ArrayList<CbmLitTrackParam>();
        // a container to hold the hits
        List<CbmLitPixelHit> hitList = new ArrayList<CbmLitPixelHit>();

        //loop over the z planes, extrapolating to each in turn.
        for (int i = 0; i < zpos.length; ++i) {
            // the resulting track State
            CbmLitTrackParam trkParamOut = new CbmLitTrackParam();
            extrap.Extrapolate(trkParam, trkParamOut, zpos[i], null);
            System.out.println(trkParamOut);
            trackStateList.add(trkParamOut);
            // convert extrapolated point into a hit and add to list
            hitList.add(hitConv.generateHit(trkParamOut.GetX(), trkParamOut.GetY(), trkParamOut.GetZ()));
        }
        
        // following output is from the external simulation program

        List<CbmLitPixelHit> fixedHitList = new ArrayList<CbmLitPixelHit>();

        fixedHitList.add(hitConv.createHit(-0.0301037,1.00001,10));
        fixedHitList.add(hitConv.createHit(-0.120549,2.00004,20));
        fixedHitList.add(hitConv.createHit(-0.271082,3.00007,30));
        fixedHitList.add(hitConv.createHit(-0.482274,4.0004,40));
        fixedHitList.add(hitConv.createHit(-0.75345,5.00076,50));
        fixedHitList.add(hitConv.createHit(-1.08507,6.00133,60));
        fixedHitList.add(hitConv.createHit(-1.47704,7.00207,70));
        // now to make a track...
        CbmLitTrack track = new CbmLitTrack();
        for (int i = 0; i < hitList.size(); ++i) {
            System.out.println("adding hit to track...");
            System.out.println(fixedHitList.get(i));
            track.AddHit(fixedHitList.get(i));
            System.out.println(" with this track state: ");
            System.out.println(trackStateList.get(i));
        }
        track.SetNofHits(hitList.size());
        
	//evidently we always need the track parameters at the
	//beginning and end. Here we use the MC generator information.
	// TODO decide how to generate these ab initio.
	// presumably with a circle fit in x,z and a line fit in s,y
	//
	// for now, let's just smear the original track parameters...
	//
	CbmLitTrackParam smearedParams = new CbmLitTrackParam();
        smearedParams.SetX(0.000253161);
        smearedParams.SetY(8.45901e-005);
        smearedParams.SetTx(0.00198466);
        smearedParams.SetTy(0.0981787);
        smearedParams.SetQp(1.998);
        // also set the diagonal elements of the covariance matrix to a large number...
        smearedParams.SetCovariance(0, 9999.);
        smearedParams.SetCovariance(5, 9999.);
        smearedParams.SetCovariance(9, 9999.);
        smearedParams.SetCovariance(12, 9999.);
        smearedParams.SetCovariance(14, 9999.);        
	       
	       System.out.println("fixed track params ");
	
	System.out.println( smearedParams );
	track.SetParamFirst(smearedParams);
	track.SetParamLast(trackStateList.get(trackStateList.size()-1));
	System.out.println( track );        
        
        
	// a Kalman Filter updater...
	CbmLitTrackUpdate trackUpdate = new CbmLitKalmanFilter();
	// we have a Runge-Kutta extrapolator
	// we need a propagator...
	CbmLitTrackPropagator prop = new SimpleTrackPropagator(extrap);
	CbmLitTrackFitter fitter = new CbmLitTrackFitterImp(prop, trackUpdate);
	CbmLitTrackFitter smoother = new CbmLitKalmanSmoother();
//	//fit downstream...
	fitter.Fit(track, true);
//	//fit upstream...
	fitter.Fit(track,false);
//

	CbmLitFitNode[] fitNodeList = track.GetFitNodes();
	System.out.println( " track fit nodes: " );
	for(int i=0; i< fitNodeList.length; ++i)
	{
		System.out.println( fitNodeList[i] );
	}
	System.out.println( " track after fit: " );
	System.out.println( track );        


	// smooth...

	CbmLitTrackFitterIter iterFitter = new CbmLitTrackFitterIter(fitter, smoother);
	iterFitter.Fit(track);

	
	fitNodeList = track.GetFitNodes();
	System.out.println( " track fit nodes: " );
	for(int i=0; i< fitNodeList.length; ++i)
	{
		System.out.println( fitNodeList[i] );
	}  
        
        System.out.println( " track after refit & smoothing: " );
	System.out.println( track );


    }
}
