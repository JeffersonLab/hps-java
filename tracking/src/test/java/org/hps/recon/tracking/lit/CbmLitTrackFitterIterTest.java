package org.hps.recon.tracking.lit;

import hep.aida.ITree;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class CbmLitTrackFitterIterTest extends TestCase
{
    AIDA aida = AIDA.defaultInstance();
    private ITree _tree = aida.tree();
    boolean debug = false;

    /**
     * Test of Fit method, of class CbmLitTrackFitterIter.
     */
    public void testCbmLitTrackFitterIter() throws Exception
    {
        // create a simple detector
        SimpleDetector det = makeDetector();
        double[] zPlanes = det.getZPositions();
        // create a simple magnetic field
        CbmLitField field = new ConstantMagneticField(0., -0.5, 0.);
        // create an extrapolator...
        CbmLitRK4TrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);
        // temporary track parameter for reuse
        CbmLitTrackParam parOut = new CbmLitTrackParam();

        // the Track Fitter
        CbmLitTrackFitter fitter = createFitter(det, field);

        double sigmax = .002;
        double sigmay = .002;
        double sigmaxy = 0.;
        Random ran = new Random();
        // loop over lots of tracks
        int numTracks = 10;
        for (int i = 0; i < numTracks; ++i) {
            // generate an input track
            CbmLitTrackParam par = new CbmLitTrackParam();
            //TODO replace this with a state generator
            double[] pars = new double[5];
            pars[0] = 0.; //x
            pars[1] = 0.; //y
            pars[2] = 0.1; // x'
            pars[3] = 0.1; // y'
            pars[4] = -1.0 / 1.056; // q/p
            par.SetStateVector(pars);
            par.SetZ(0.);
//            // also need a starting covariance matrix...
//            // upperdiagonal
//            double[] cov = new double[15];
//            cov[0] = sigmax;
//            cov[5] = sigmay;
//            //TODO resolve what to use for starting covariance matrix...
//            cov[9] = 1. / 99999.;
//            cov[12] = 1. / 99999.;
//            cov[14] = 1. / 99999.;
//            CbmLitTrackParam parFirst = new CbmLitTrackParam();
//            CbmLitTrackParam parLast = new CbmLitTrackParam();
            List<CbmLitHit> hitList = new ArrayList<CbmLitHit>();
            // swim the track to each detector station
            if(debug) System.out.println(zPlanes.length);
            for (int j = 0; j < zPlanes.length; ++j) {
                //              if(debug) System.out.println(zPlanes[j]);
                LitStatus stat = extrap.Extrapolate(par, parOut, zPlanes[j], null);
                if(debug) System.out.println(j + " : par    " + par);
                if(debug) System.out.println(j + " : parOut " + parOut);
                if(debug) System.out.println(j + " : " + "x= " + parOut.GetX() + " y= " + parOut.GetY() + " z= " + parOut.GetZ());
                // generate hits by smearing the positions
                // start with pixel hits
                CbmLitPixelHit hit = new CbmLitPixelHit();
                hit.SetDx(sigmax);
                hit.SetDxy(sigmaxy);
                hit.SetDy(sigmay);
                hit.SetDz(0.);
                // smear x and y independently...
                hit.SetX(parOut.GetX() + sigmax * ran.nextGaussian());
                hit.SetY(parOut.GetY() + sigmay * ran.nextGaussian());
                hit.SetZ(parOut.GetZ());
                hitList.add(hit);

//                // need a first and last track state
//                if (j == 0) {
//                    pars[0] = hit.GetX(); //x
//                    pars[1] = hit.GetY(); //y
//                    pars[2] = 0.;//parOut.GetTx(); // x'
//                    pars[3] = 0.;//parOut.GetTy(); // y'
//                    pars[4] = parOut.GetQp(); // q/p
//                    parFirst.SetStateVector(pars);
//                    parFirst.SetZ(0.);
//                    // also need an estimate for the covariance matrix...
//                    parFirst.SetCovMatrix(cov);
//                }
//                if (j == zPlanes.length - 1) {
//                    pars[0] = hit.GetX(); //x
//                    pars[1] = hit.GetY(); //y
//                    pars[2] = 0.;//parOut.GetTx(); // x'
//                    pars[3] = 0.;//parOut.GetTy(); // y'
//                    pars[4] = parOut.GetQp(); // q/p
//                    parLast.SetStateVector(pars);
//                    parLast.SetZ(hit.GetZ() + 1.);
//                    parLast.SetCovMatrix(cov);
//                }
            }
            if (debug) {
                System.out.println("hitList has " + hitList.size() + " hits");
            }

            // create a track with these hits
            CbmLitTrack track = new CbmLitTrack();
            //seems we need approximations for the track state at the beginning and end
            // let's see how little we can get away with...
            // add start and end states
            CbmLitTrackParam defaultStartParams = new CbmLitTrackParam();
            CbmLitTrackParam defaultEndParams = new CbmLitTrackParam();
            defaultStartParams.SetZ(hitList.get(0).GetZ());
            defaultEndParams.SetZ(hitList.get(hitList.size() - 1).GetZ());
            track.SetParamFirst(defaultStartParams);
            track.SetParamLast(defaultEndParams);
//            track.SetParamFirst(parFirst);
//            track.SetParamLast(parLast);

//            System.out.println("track: "+track);
            track.AddHits(hitList);
//            System.out.println("track: "+track);

            // let's try just fitting this list of hits...
            fit(hitList, det, field);

            // fit this track
            boolean downstream = true;
            LitStatus stat = fitter.Fit(track, downstream);
            if (debug) {
                System.out.println(stat + " : " + track);
            }
            CbmLitTrackParam first = track.GetParamFirst();
            if (debug) {
                System.out.println("first: " + first);
            }
            CbmLitTrackParam last = track.GetParamLast();
            if (debug) {
                System.out.println("last: " + last);
            }
            // extrapolate to the origin
            CbmLitTrackParam fitAtZero = new CbmLitTrackParam(last);
            System.out.println("fitAtZero: "+fitAtZero);
            extrap.Extrapolate(fitAtZero, 0., null);
            System.out.println("fitAtZero: "+fitAtZero);
            // compare
            compare(track,par,"test",extrap,downstream);
            
        }
        // end of loop over lots of tracks
        aida.saveAs(this.getClass().getSimpleName() + ".aida");
    }

    SimpleDetector makeDetector()
    {
        SimpleDetector det = new SimpleDetector();
        //create a few CbmLitMaterialInfo objects...
        double[] zees = {20., 10., 5., 11., 37., 52., 23., 1., 44., 40.};
        int numDet = zees.length;
        for (int i = 0; i < numDet; ++i) {
            CbmLitMaterialInfo m = new CbmLitMaterialInfo();
            double z = zees[i];
            m.SetA(28.08);
            m.SetLength(.03);
            m.SetName("Silicon");
            m.SetRL(9.37);
            m.SetRho(2.33);
            m.SetZ(14);
            m.SetZpos(z);
            det.addDetectorPlane(m);
        }
        return det;
    }

    CbmLitTrackFitter createFitter(SimpleDetector det, CbmLitField field)
    {
        CbmLitTrackPropagator prop = new DetectorTrackPropagator(det, field);
        CbmLitTrackUpdate update = new CbmLitKalmanFilter();
        CbmLitTrackFitter fitter = new CbmLitTrackFitterImp(prop, update);
        CbmLitTrackFitter smoother = new CbmLitKalmanSmoother();
        int maxIter = 1;
        int minHits = 5;
        double chiSqCut = 20.;
        CbmLitTrackFitter iterfitter = new CbmLitTrackFitterIter(fitter, smoother, maxIter, minHits, chiSqCut);
        return iterfitter;
    }

    void fit(List<CbmLitHit> hitList, SimpleDetector det, CbmLitField field)
    {
        CbmLitTrackPropagator prop = new DetectorTrackPropagator(det, field);
        CbmLitTrackUpdate update = new CbmLitKalmanFilter();
        CbmLitTrackFitter fitter = new CbmLitTrackFitterImp(prop, update);
        // create a track with these hits
        CbmLitTrack track = new CbmLitTrack();
        //seems we need approximations for the track state at the beginning and end
        // let's see how little we can get away with...
        // add start and end states
        CbmLitTrackParam defaultStartParams = new CbmLitTrackParam();
        CbmLitTrackParam defaultEndParams = new CbmLitTrackParam();
        defaultStartParams.SetZ(hitList.get(0).GetZ());
        defaultEndParams.SetZ(hitList.get(hitList.size() - 1).GetZ());
        track.SetParamFirst(defaultStartParams);
        track.SetParamLast(defaultEndParams);
        track.AddHits(hitList);
        // fit downstream...
        LitStatus stat = fitter.Fit(track, true);
        CbmLitTrackParam first = track.GetParamFirst();
        CbmLitTrackParam last = track.GetParamLast();
        if (debug) {
            System.out.println(stat + " : " + track);
            System.out.println("first: " + first);
            System.out.println("last: " + last);
        }
        // now fit upstream...
        stat = fitter.Fit(track, false);
        first = track.GetParamFirst();
        last = track.GetParamLast();
        if (debug) {
            System.out.println(stat + " : " + track);
            System.out.println("first: " + first);
            System.out.println("last: " + last);
        }
    }
    
    private void compare(CbmLitTrack track, CbmLitTrackParam mcp, String folder, CbmLitTrackExtrapolator extrap, boolean downstream)
    {
        boolean localdebug = true;
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
            if (localdebug) {
                System.out.println("MC parameters             : " + mcp);
                System.out.println("trackFirst                : "+track.GetParamFirst());
                System.out.println("trackLast                 : "+track.GetParamLast());
                System.out.println("track parameters at origin: " + tAtOrigin);
            }
            double[] mcStateVector = mcp.GetStateVector();
            double[] tStateVector = tAtOrigin.GetStateVector();
            String[] label = {"x", "y", "tx", "ty", "qp"};
            double[] covMat = tAtOrigin.GetCovMatrix();
            int[] index = {0, 5, 9, 12, 14};
            for (int i = 0; i < 5; ++i) {
                aida.cloud1D(label[i] + " MC").fill(mcStateVector[i]);
                aida.cloud1D(label[i] + " track").fill(tStateVector[i]);
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
