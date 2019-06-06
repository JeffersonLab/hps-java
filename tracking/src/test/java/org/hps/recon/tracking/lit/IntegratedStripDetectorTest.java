package org.hps.recon.tracking.lit;

import hep.aida.ITree;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class IntegratedStripDetectorTest extends TestCase
{

    boolean debug = false;
    boolean testDefaults = false;
    AIDA aida = AIDA.defaultInstance();
    private ITree _tree = aida.tree();

    public void testAll() throws Exception
    {
        // z positions for detector planes
        // double up the number because each is a 1D measurement.
        double[] zpos = {10., 10.1, 20., 20.1, 30., 30.1, 40., 40.1, 50., 50.1, 60., 60.1, 70., 70.1};
        // stereo angles for strip detectors, in degrees (will be converted later)
//        double[] stereoAngle = {0., 90., 0., 90., 0., 90., 0., 90., 0., 90., 0., 90., 0., 90.};
//let's try shallow angle stereo
        double hundredmilliradians = java.lang.Math.toDegrees(0.100);
        double stereo = hundredmilliradians;
        double[] stereoAngle = {90., 90. + stereo, 90., 90. - stereo, 90., 90. + stereo, 90., 90. - stereo, 90., 90. + stereo, 90., 90. - stereo, 90., 90. + stereo};

        // a constant magnetic field...
        ConstantMagneticField field = new ConstantMagneticField(0., -0.24, 0.);

        // a propagator...
        CbmLitTrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);

        // A list of converters to convert spacepoints at a plane
        // into strip hits with stereoAngle as specified above.
        List<PointToStripHitConverter> hitConvList = new ArrayList<PointToStripHitConverter>();
        for (int i = 0; i < zpos.length; ++i) {
            double phiRadians = java.lang.Math.toRadians(stereoAngle[i]);
            hitConvList.add(new PointToStripHitConverter(phiRadians, .0001));
        }
        //PointToPixelHitConverter hitConv = new PointToPixelHitConverter(.0001, .0001, 0.0);

        // a class to smear initial track parameters.
        // 
        //TrackParameterSmearer smearer = new TrackParameterSmearer(.001, .001, .002, .002, 0.1);
// a trackParam to use for intializing the fits
        CbmLitTrackParam trkParamZeroEstimate = new CbmLitTrackParam();
        trkParamZeroEstimate.SetQp(0.1);
        trkParamZeroEstimate.SetCovariance(0, 999.);
        trkParamZeroEstimate.SetCovariance(5, 999.);
        trkParamZeroEstimate.SetCovariance(9, 999.);
        trkParamZeroEstimate.SetCovariance(12, 999.);
        trkParamZeroEstimate.SetCovariance(14, 999.);
        // A track state to propagate...
        // start at z=0.
        int numTrials = 10000;
        for (int jj = 0; jj < numTrials; ++jj) {
            CbmLitTrackParam trkParam = new CbmLitTrackParam();
            trkParam.SetX(0.);
            trkParam.SetY(0.);
            trkParam.SetZ(0.);
            trkParam.SetTx(0.);
            trkParam.SetTy(0.0);
            trkParam.SetQp(1.);

            if (debug) {
                System.out.println(trkParam);
            }

            // a container to hold the propagated track states
            List<CbmLitTrackParam> trackStateList = new ArrayList<CbmLitTrackParam>();
            // a container to hold the hits
            // List<CbmLitPixelHit> hitList = new ArrayList<CbmLitPixelHit>();
            List<CbmLitStripHit> stripHitList = new ArrayList<CbmLitStripHit>();

            //loop over the z planes, extrapolating to each in turn.
            for (int i = 0; i < zpos.length; ++i) {
                // the resulting track State
                CbmLitTrackParam trkParamOut = new CbmLitTrackParam();
                extrap.Extrapolate(trkParam, trkParamOut, zpos[i], null);
                if (debug) {
                    System.out.println(trkParamOut);
                }
                trackStateList.add(trkParamOut);
                // convert extrapolated point into a hit and add to list
                // hitList.add(hitConv.generateHit(trkParamOut.GetX(), trkParamOut.GetY(), trkParamOut.GetZ()));
                stripHitList.add(hitConvList.get(i).generateHit(trkParamOut.GetX(), trkParamOut.GetY(), trkParamOut.GetZ()));
            }

            // now to make a track...
            CbmLitTrack track = new CbmLitTrack();
            for (int i = 0; i < zpos.length; ++i) {

                if (debug) {
                    System.out.println("adding hit to track...");
                }
                if (debug) {
                    //System.out.println(hitList.get(i));
                    System.out.println(stripHitList.get(i));
                }
                //track.AddHit(hitList.get(i));
                track.AddHit(stripHitList.get(i));
                if (debug) {
                    System.out.println(" with this track state: ");
                }
                if (debug) {
                    System.out.println(trackStateList.get(i));
                }
            }
            track.SetNofHits(zpos.length);

            //evidently we always need the track parameters at the
            //beginning and end.  See below for a default track state...
            // Here we use the MC generator information.
            // TODO decide how to generate these ab initio.
            // presumably with a circle fit in x,z and a line fit in s,y
            //
            // for now, let's just smear the original track parameters...
            //
//            CbmLitTrackParam smearedParams = new CbmLitTrackParam(trkParam);
            if (debug) {
                System.out.println(trkParam);
            }
//            smearer.SmearTrackParameters(smearedParams);
            if (debug) {
                System.out.println("smeared track params ");
            }

            if (debug) {
                System.out.println(trkParamZeroEstimate);
            }

            track.SetParamFirst(trkParamZeroEstimate);
            track.SetParamLast(trkParamZeroEstimate);
            if (debug) {
                System.out.println(track);
                System.out.println("ParamFirst: " + track.GetParamFirst());
                System.out.println("ParamLast: " + track.GetParamLast());

            }
//            if (testDefaults) {
//                //let's see if we can't start with some default value
//                System.out.println("let's see if we can use a default set of parameters...");
//                CbmLitTrackParam defaultStartParams = new CbmLitTrackParam();
//                CbmLitTrackParam defaultEndParams = new CbmLitTrackParam();
//                defaultEndParams.SetZ(zpos[zpos.length - 1]);
//                track.SetParamFirst(defaultStartParams);
//                track.SetParamLast(defaultEndParams);
//                if (debug) {
//                    System.out.println(track);
//                }
//            }

            // a Kalman Filter updater...
            CbmLitTrackUpdate trackUpdate = new CbmLitKalmanFilter();
            // we have a Runge-Kutta extrapolator
            // we need a propagator...
            CbmLitTrackPropagator prop = new SimpleTrackPropagator(extrap);
            CbmLitTrackFitter fitter = new CbmLitTrackFitterImp(prop, trackUpdate);
            CbmLitTrackFitter smoother = new CbmLitKalmanSmoother();
// can't fit both sequentially (pulls will be sigma=1.4 instead of 1.0)
            // choose one or the other                
//	//fit downstream...
//            fitter.Fit(track, true);
//	//fit upstream...
            boolean downstream = false;
            String dir = downstream ? "downstream" : "upstream";
            fitter.Fit(track, downstream);
            compare(track, trkParam, dir, extrap, downstream);
//
            if (debug) {
                System.out.println(" track after fit: ");
            }
            if (debug) {
                System.out.println(track);
            }
            CbmLitFitNode[] fitNodeList = track.GetFitNodes();
            if (debug) {
                System.out.println(" track fit nodes: ");

                for (int i = 0; i < fitNodeList.length; ++i) {
                    System.out.println(fitNodeList[i]);
                }
            }

//	// smooth...
//
//            CbmLitTrackFitterIter iterFitter = new CbmLitTrackFitterIter(fitter, smoother);
//            iterFitter.Fit(track);
//
//            if (debug) {
//                System.out.println(" track after refit & smoothing: ");
//            }
//            if (debug) {
//                System.out.println(track);
//            }
//            fitNodeList = track.GetFitNodes();
//            if (debug) {
//                System.out.println(" track fit nodes: ");
//
//                for (int i = 0; i < fitNodeList.length; ++i) {
//                    System.out.println(fitNodeList[i]);
//                }
//            }
//            System.out.println(jj + " : " + fitNodeList[fitNodeList.length - 1].GetSmoothedParam().GetQp());
        } // end of loop over numTrials
// let's write out the aida file...
        aida.saveAs(this.getClass().getSimpleName() + ".aida");

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
                aida.cloud1D(label[i] + " residual").fill(tStateVector[i] - mcStateVector[i]);
                aida.cloud1D(label[i] + " pull").fill((tStateVector[i] - mcStateVector[i]) / sqrt(covMat[index[i]]));

            }

            int ndf = track.GetNDF();
            aida.cloud1D("Chisq").fill(chisq);
            aida.cloud1D("Chisq Probability").fill(ChisqProb.gammq(ndf, chisq));
            aida.cloud1D("Momentum").fill(abs(1. / tStateVector[4]));
            _tree.cd("/");
        }
    }
}
