package org.hps.recon.tracking.lit;

import hep.aida.ITree;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class IntegratedHpsStripDetectorTest extends TestCase
{

    boolean debug = false;
    AIDA aida = AIDA.defaultInstance();
    private ITree _tree = aida.tree();

    public void testAll() throws Exception
    {
        // z positions for detector planes
        // double up the number because each is a 1D measurement.
        //double[] zpos = {10., 10.1, 20., 20.1, 30., 30.1, 40., 40.1, 50., 50.1, 60., 60.1};//, 70., 70.1};
        double[] zpos = {88., 96., 188., 196., 288., 296., 488., 496., 688., 696., 888., 896.};
        // stereo angles for strip detectors, in degrees (will be converted later)
//        double[] stereoAngle = {0., 90., 0., 90., 0., 90., 0., 90., 0., 90., 0., 90., 0., 90.};
//let's try shallow angle stereo
        double hundredmilliradians = java.lang.Math.toDegrees(0.100);
        double stereo = hundredmilliradians;
        double[] stereoAngle = {90., 90. + stereo, 90., 90. - stereo, 90., 90. + stereo, 90., 90. - stereo, 90., 90. + stereo, 90., 90. - stereo, 90., 90. + stereo};

        String[] names = {"L1a", "L1s", "L2a", "L2s", "L3a", "L3s", "L4a", "L4s", "L5a", "L5s", "L6a", "L6s", "L7a", "L7s"};

        // strip measurement resolution
        double sigmaU = .0001;
        // build an Hps detector...
        HpsDetector det = newDetector(zpos, names, stereoAngle);
        // a constant magnetic field...
        ConstantMagneticField field = new ConstantMagneticField(0., -0.24, 0.);

        // a propagator...
        CbmLitTrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);

        // A list of converters to convert spacepoints at a plane
        // into strip hits with stereoAngle as specified above.
        List<PointToStripHitConverter> hitConvList = new ArrayList<PointToStripHitConverter>();
        // convert to a map so I can access it by name, not index
        Map<String, PointToStripHitConverter> hitConvMap = new HashMap<String, PointToStripHitConverter>();
        for (int i = 0; i < zpos.length; ++i) {
            double phiRadians = java.lang.Math.toRadians(stereoAngle[i]);
            hitConvList.add(new PointToStripHitConverter(phiRadians, sigmaU));
            hitConvMap.put(names[i], new PointToStripHitConverter(phiRadians, sigmaU));
        }

        // a class to smear initial track parameters.
        // 
        TrackParameterSmearer smearer = new TrackParameterSmearer(.001, .001, .002, .002, 0.1);

        // A track state to propagate...
        // start at z=0.
        int numTrials = 10000;
        for (int jj = 0; jj < numTrials; ++jj) {
            CbmLitTrackParam mcTrkParamAtStart = new CbmLitTrackParam();
            mcTrkParamAtStart.SetX(0.);
            mcTrkParamAtStart.SetY(0.);
            mcTrkParamAtStart.SetZ(0.);
            mcTrkParamAtStart.SetTx(0.);
            mcTrkParamAtStart.SetTy(0.1);
            mcTrkParamAtStart.SetQp(1.);

            if (debug) {
                System.out.println(mcTrkParamAtStart);
            }

            // a container to hold the propagated track states
            List<CbmLitTrackParam> trackStateList = new ArrayList<CbmLitTrackParam>();
            // a container to hold the hits
            List<CbmLitStripHit> stripHitList = new ArrayList<CbmLitStripHit>();

            //loop over the z planes, extrapolating to each in turn.
            // store the end state
            CbmLitTrackParam mcTrkParamAtEnd = new CbmLitTrackParam();

            for (int i = 0; i < zpos.length; ++i) {
                // the resulting track State
                extrap.Extrapolate(mcTrkParamAtStart, mcTrkParamAtEnd, zpos[i], null);
                if (debug) {
                    System.out.println(mcTrkParamAtEnd);
                }
                trackStateList.add(new CbmLitTrackParam(mcTrkParamAtEnd));
                // convert extrapolated point into a hit and add to list
                stripHitList.add(hitConvList.get(i).generateHit(mcTrkParamAtEnd.GetX(), mcTrkParamAtEnd.GetY(), mcTrkParamAtEnd.GetZ()));
            }

            // repeat this now with the detector planes...
            // a container to hold the propagated track states
            List<CbmLitTrackParam> trackStateList2 = new ArrayList<CbmLitTrackParam>();
            // a container to hold the hits
            List<CbmLitDetPlaneStripHit> stripHitList2 = new ArrayList<CbmLitDetPlaneStripHit>();

            //loop over the z planes, extrapolating to each in turn.
            // store the state at the end
            CbmLitTrackParam trkParamOut2 = new CbmLitTrackParam();
            List<DetectorPlane> planes = det.getPlanes();
            for (DetectorPlane p : planes) {
                // the resulting track State

                extrap.Extrapolate(mcTrkParamAtStart, trkParamOut2, p, null);
                if (debug) {
                    System.out.println(trkParamOut2);
                }
                trackStateList2.add(trkParamOut2);
                // convert extrapolated point into a hit and add to list
                stripHitList2.add(hitConvMap.get(p.name()).generateHit(trkParamOut2.GetX(), trkParamOut2.GetY(), p));
            }
            // end of repeat

            // now to make a track...
            CbmLitTrack track = new CbmLitTrack();
            for (int i = 0; i < zpos.length; ++i) {

                if (debug) {
                    System.out.println("adding hit to track...");
                    System.out.println(stripHitList.get(i));
                }
                //track.AddHit(hitList.get(i));
                track.AddHit(stripHitList.get(i));
                if (debug) {
                    System.out.println(" with this track state: ");
                    System.out.println(trackStateList.get(i));
                }
            }
            track.SetNofHits(zpos.length);

            // repeat
            CbmLitTrack track2 = new CbmLitTrack();
            for (CbmLitHit hit : stripHitList2) {
                track2.AddHit(hit);
            }
            // end repeat

            //compare the two tracks
            List<CbmLitHit> t1Hits = track.GetHits();
            List<CbmLitHit> t2Hits = track2.GetHits();
            if (debug) {
                System.out.println("track1 " + track);
                System.out.println("track2 " + track2);
                for (int i = 0; i < t1Hits.size(); ++i) {
                    System.out.println("t1Hit " + t1Hits.get(i));
                    System.out.println("t2Hit " + t2Hits.get(i));
                }
            }
            //evidently we always need the track parameters at the
            //beginning and end.  See below for a default track state...
            // Here we use the MC generator information.
            // TODO decide how to generate these ab initio.
            // presumably with a circle fit in x,z and a line fit in s,y
            //
            // for now, let's just smear the original track parameters...
            //
//            CbmLitTrackParam smearedParams = new CbmLitTrackParam(trkParam);
//            if (debug) {
//                System.out.println(trkParam);
//            }
//            smearer.SmearTrackParameters(smearedParams);
//            if (debug) {
//                System.out.println("smeared track params ");
//            }
//
//            if (debug) {
//                System.out.println(smearedParams);
//            }
//
//            track.SetParamFirst(smearedParams);
//            track.SetParamLast(trackStateList.get(trackStateList.size() - 1));
//            if (debug) {
//                System.out.println(track);
//                System.out.println("ParamFirst: " + smearedParams);
//                System.out.println("ParamLast: " + trackStateList.get(trackStateList.size() - 1));
//
//            }
//            if (testDefaults) {
            //let's see if we can't start with some default value
//            CbmLitTrackParam defaultStartParams = new CbmLitTrackParam();
//            defaultStartParams.SetZ(zpos[0]);
//            CbmLitTrackParam defaultEndParams = new CbmLitTrackParam();
//            defaultEndParams.SetZ(zpos[zpos.length - 1]);
//            track.SetParamFirst(defaultStartParams);
//            track.SetParamLast(defaultEndParams);
//            //repeat
//            CbmLitTrackParam defaultStartParams2 = new CbmLitTrackParam();
//            CbmLitTrackParam defaultEndParams2 = new CbmLitTrackParam();
//            defaultEndParams2.SetZ(planes.get(planes.size() - 1).GetZpos());
//            track2.SetParamFirst(defaultStartParams2);
//            track2.SetParamLast(defaultEndParams2);
//TODO extract this out into CbmLitTrack maybe?
        // a trackParam to use for intializing the fits
        CbmLitTrackParam trkParamZeroEstimate = new CbmLitTrackParam();
        trkParamZeroEstimate.SetQp(1.0);
        trkParamZeroEstimate.SetCovariance(0, 999.);
        trkParamZeroEstimate.SetCovariance(5, 999.);
        trkParamZeroEstimate.SetCovariance(9, 999.);
        trkParamZeroEstimate.SetCovariance(12, 999.);
        trkParamZeroEstimate.SetCovariance(14, 999.);

        // add start and end states
        track.SetParamFirst(trkParamZeroEstimate);
        track.SetParamLast(trkParamZeroEstimate);
        track2.SetParamFirst(trkParamZeroEstimate);
        track2.SetParamLast(trkParamZeroEstimate);
        
            
            //end repeat

            if (debug) {
                System.out.println(track);
                System.out.println(track2);
            }
            //}

            // a Kalman Filter updater...
            CbmLitTrackUpdate trackUpdate = new CbmLitKalmanFilter();
            // we have a Runge-Kutta extrapolator
            // we need a propagator...
            CbmLitTrackPropagator prop = new SimpleTrackPropagator(extrap);
            CbmLitTrackFitter fitter = new CbmLitTrackFitterImp(prop, trackUpdate);
            CbmLitTrackFitter smoother = new CbmLitKalmanSmoother();
//	//fit downstream...
            boolean downstream = true;
//            fitter.Fit(track, downstream);
//            compare(track, trkParamAtEnd,"downstream", extrap, downstream);    
//            //repeat
//            fitter.Fit(track2, downstream);
//	//fit upstream...
            downstream = false;
            fitter.Fit(track, downstream);
            compare(track, mcTrkParamAtStart, "upstream", extrap, downstream);
            //repeat
            fitter.Fit(track2, downstream);
            compare(track2, mcTrkParamAtStart, "upstream2", extrap, downstream);
            CbmLitFitNode[] fitNodeList = track.GetFitNodes();
            CbmLitFitNode[] fitNodeList2 = track2.GetFitNodes();
            if (debug) {
                System.out.println(" tracks after fit: ");
                System.out.println(track);
                System.out.println(track2);

                System.out.println(" track fit nodes: ");

                for (int i = 0; i < fitNodeList.length; ++i) {
                    System.out.println(fitNodeList[i]);
                    System.out.println(fitNodeList2[i]);
                }
            }

//	// smooth...
//
//            CbmLitTrackFitterIter iterFitter = new CbmLitTrackFitterIter(fitter, smoother);
//            iterFitter.Fit(track);
//            //repeat
//            iterFitter.Fit(track2);
//            if (debug) {
//                System.out.println(" track after refit & smoothing: ");
//                System.out.println(track);
//                System.out.println(track2);
//            }
//            fitNodeList = track.GetFitNodes();
//            fitNodeList2 = track2.GetFitNodes();
//            if (debug) {
//                System.out.println(" track fit nodes: ");
//
//                for (int i = 0; i < fitNodeList.length; ++i) {
//                    System.out.println(fitNodeList[i]);
//                    System.out.println(fitNodeList2[i]);
//                }
//            }
//            System.out.println("track 1 " + jj + " : " + track.GetParamFirst());
//            System.out.println("track 2 " + jj + " : " + track2.GetParamFirst());
//            System.out.println(jj + " : " + fitNodeList[fitNodeList.length - 1].GetSmoothedParam().GetQp());
//            System.out.println(jj + " : " + fitNodeList2[fitNodeList.length - 1].GetSmoothedParam().GetQp());
//
//            // now compare (at the origin)
//            compare(track, trkParamAtStart, "iter", extrap, false);
        } // end of loop over numTrials

        // let's write out the aida file...
        aida.saveAs(this.getClass().getSimpleName() + ".aida");
    }

    HpsDetector newDetector(double[] zees, String[] names, double[] phi)
    {
//        double[] zees = {87.909118946011, 96.0939189018316, 187.97896366133614, 195.97552893546475, 287.9606833184293, 295.9480418112455, 486.50882817838055, 493.9921379568081, 686.3705011120256, 693.9397138855463, 889.457554538664, 896.9125606348301};
//        String[] names = {"L1t_axial", "L1t_stereo", "L2t_axial", "L2t_stereo", "L3t_axial", "L3t_stereo", "L4t_axial", "L4t_stereo", "L5t_axial", "L5t_stereo", "L6t_axial", "L6t_stereo"};
//        double[] phi = {3.141506045566059,
//            -0.09995368832998253,
//            3.141596601360339,
//            -0.0994942520988551,
//            3.141723085567417,
//            -0.09962858546918252,
//            2.518666641859735E-4,
//            3.0916128359226525,
//            3.957986839968619E-4,
//            3.0922364767590538,
//            3.141752209439217,
//            -0.04930764414933564};

        double x0 = .1;
        HpsDetector det = new HpsDetector();

        for (int i = 0; i < zees.length; ++i) {
            CartesianThreeVector pos = new CartesianThreeVector(0., 0., zees[i]);
            CartesianThreeVector eta = new CartesianThreeVector(0., 0., 1.);
            DetectorPlane p = new DetectorPlane(names[i], pos, eta, x0, phi[i]);
            det.addDetectorPlane(p);
        }
        return det;
    }

    private void compare(CbmLitTrack track, CbmLitTrackParam mcp, String folder, CbmLitTrackExtrapolator extrap, boolean downstream)
    {
        double chisq = track.GetChi2();
        if (chisq < 1000) {
            _tree.mkdirs(folder);
            _tree.cd(folder);
            // get the upstream track parameters
            CbmLitTrackParam tp1 = downstream ? track.GetParamFirst() : track.GetParamFirst();
            // output parameters
            CbmLitTrackParam tAtOrigin = new CbmLitTrackParam();
            // find z where we should compare
            double z = mcp.GetZ();
            aida.cloud1D("z of comparison").fill(z);
            // extrapolate our track to this z position
            extrap.Extrapolate(tp1, tAtOrigin, z, null);
            if (debug) {
                System.out.println("MC parameters             : " + mcp);
                System.out.println("track parameters at origin: " + tAtOrigin);
            }
            double[] mcStateVector = mcp.GetStateVector();
            double[] tStateVector = tAtOrigin.GetStateVector();
            String[] label = {"x", "y", "tx", "ty", "qp"};
            double[] covMat = tAtOrigin.GetCovMatrix();
            int[] index = {0, 5, 9, 12, 14};
            for (int i = 0; i < 5; ++i) {
                aida.cloud1D(label[i] + " MC").fill(mcStateVector[i]);
                aida.cloud1D(label[i] + " fit").fill(tStateVector[i]);
                aida.cloud1D(label[i] + " residual").fill(tStateVector[i] - mcStateVector[i]);
                aida.cloud1D(label[i] + " pull").fill((tStateVector[i] - mcStateVector[i]) / sqrt(covMat[index[i]]));

            }

            int ndf = track.GetNDF();
            aida.cloud1D("Chisq").fill(chisq);
            aida.cloud1D("Chisq Probability").fill(ChisqProb.gammq(ndf, chisq));
            aida.cloud1D("Momentum").fill(abs(1. / tStateVector[4]));
//            CbmLitFitNode[] nodes = track.GetFitNodes();
//            for(int i=0; i< nodes.length; ++i)
//            {
//                CbmLitFitNode n = nodes[i];
//            }
            _tree.cd("/");
        }
    }
}
