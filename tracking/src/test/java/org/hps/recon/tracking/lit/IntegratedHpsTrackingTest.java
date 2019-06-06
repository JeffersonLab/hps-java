package org.hps.recon.tracking.lit;

import hep.aida.ITree;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.fourvec.Lorentz4Vector;
import org.lcsim.util.fourvec.Momentum4Vector;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class IntegratedHpsTrackingTest extends TestCase
{

    boolean debug = false;
//    String hpsDetectorName = "HPS-Phantom-fieldmap"; //TODO figure out what is wrong with this detector
    String hpsDetectorName = "HPS-EngRun2015-Nominal-v2-fieldmap";
    CbmLitRK4TrackExtrapolator _extrap;
    CbmLitTrackFitter _fitter;
    HpsDetector _hpsdet;

    AIDA aida = AIDA.defaultInstance();
    private ITree _tree = aida.tree();

    public void testIt() throws Exception
    {
        initialize();
        fee();
        //mollers();
        // save our histograms
        aida.saveAs(this.getClass().getSimpleName() + "_" + hpsDetectorName + ".aida");
        System.out.println("wrote " + this.getClass().getSimpleName() + ".aida");
    }

    public void initialize() throws Exception
    {
        //start with a detector

        DatabaseConditionsManager cm = DatabaseConditionsManager.getInstance();
        cm.setDetector(hpsDetectorName, 0);
        Detector det = cm.getDetectorObject();
        // a constant magnetic field...
        CbmLitField constfield = new ConstantMagneticField(0., -0.24, 0.);
        _extrap = new CbmLitRK4TrackExtrapolator(constfield);
        // a Kalman Filter updater...
        CbmLitTrackUpdate trackUpdate = new CbmLitKalmanFilter();
        CbmLitTrackPropagator prop = new SimpleTrackPropagator(_extrap);
        _fitter = new CbmLitTrackFitterImp(prop, trackUpdate);
        _hpsdet = new HpsDetector(det);
        System.out.println(_hpsdet);
    }

    public void fee() throws Exception
    {
        // simulate a full energy electron
        // the vertex x,y distribution
        double[] means = {0., 0.};
        double[][] cov = {{.02, 0.}, {0., .04}};
        MultivariateNormalDistribution vxd = new MultivariateNormalDistribution(means, cov);
        // lets step through some various momenta and see how well we do
        double[] momenta = {1.056}; //{0.3, 0.4, 0.6, 0.8, 1.0};
        for (int ii = 0; ii < momenta.length; ++ii) {
            int nEvents = 10000;
            for (int event = 0; event < nEvents; ++event) {
                double[] xy = vxd.sample();
                aida.cloud2D("vertex x vs y").fill(xy[0], xy[1]);
                // track state to propagate
                CbmLitTrackParam parIn = new CbmLitTrackParam();
                double[] pars = new double[5];
                //TODO generate a function to return MC track parameters
                pars[0] = xy[0]; //x
                pars[1] = xy[1]; //y
                pars[2] = 0.0; // x' (dx/dz)
                pars[3] = 0.03; // y' (dy/dz)
                pars[4] = -1 / momenta[ii];//1.056; // q/p
                parIn.SetStateVector(pars);
                parIn.SetZ(0.);
                // a trackParam to use for intializing the fits
                //TODO smear this a bit, see how little I can get away with
                CbmLitTrackParam trkParamZeroEstimate = new CbmLitTrackParam();
                trkParamZeroEstimate.SetQp(pars[4]);
                trkParamZeroEstimate.SetCovariance(0, 999.);
                trkParamZeroEstimate.SetCovariance(5, 999.);
                trkParamZeroEstimate.SetCovariance(9, 999.);
                trkParamZeroEstimate.SetCovariance(12, 999.);
                trkParamZeroEstimate.SetCovariance(14, 999.);

                List<CbmLitDetPlaneStripHit> hits = makeHits(parIn);
                if (hits.size() == 12) {
                    boolean[] updown = {true};//false};//, true};
                    for (int k = 0; k < updown.length; ++k) {
                        if (debug) {
                            System.out.println("\n fit " + k);
                        }
                        boolean downstream = updown[k];
                        String folder = "fee/momentum " + momenta[ii] + (downstream ? " downstream" : " upstream");
                        CbmLitTrack fitTrack = fit(hits, trkParamZeroEstimate, downstream);
                        if (fitTrack != null) {
                            compare(fitTrack, parIn, folder, _extrap, downstream);
                        } else {
                            System.out.println("fit failed");
                        }
                    }
                }
            } // end of loop over events
        }//end of loop over momenta
    }

    private CbmLitTrack fit(List<CbmLitDetPlaneStripHit> hits, CbmLitTrackParam trkParamZeroEstimate, boolean downstream)
    {
        // create a track
        CbmLitTrack track = new CbmLitTrack();
        // add the hits
        for (CbmLitHit hit : hits) {
            track.AddHit(hit);
        }
        //TODO extract this out into CbmLitTrack maybe
        // add start and end states
        track.SetParamFirst(trkParamZeroEstimate);
        track.SetParamLast(trkParamZeroEstimate);
        // fit
        LitStatus status = _fitter.Fit(track, downstream);
        if (status == LitStatus.kLITERROR) {
            return null;
        }
        return track;
    }

    public void mollers() throws Exception
    {
        // the vertex x,y distribution
        double[] means = {0., 0.};
        double[][] cov = {{.02, 0.}, {0., .04}};
        MultivariateNormalDistribution vxd = new MultivariateNormalDistribution(means, cov);

        Random rx = new Random();
        double sigmaX = .08;
        Random ry = new Random();
        double sigmaY = .02;
        Random rz = new Random();
        double sigmaZ = .002;

        // generate some decays
        int nEvents = 10000;
        int nTwoGoodMollerTracksEvents = 0;
        for (int event = 0; event < nEvents; ++event) {
            //generate a vertex
            double[] xy = vxd.sample();
            aida.cloud2D("mollers/vertex x vs y").fill(xy[0], xy[1]);
            double zvtx = rz.nextGaussian() * sigmaZ;
            aida.cloud1D("mollers/vertex z").fill(zvtx);
            CartesianThreeVector vtx = new CartesianThreeVector(xy[0], xy[1], zvtx);
            // the input particles
            Lorentz4Vector[] mollers = genMollers();
            //
            // the list of list of hits
            List<List<CbmLitDetPlaneStripHit>> hitLists = new ArrayList<List<CbmLitDetPlaneStripHit>>();
            // the list of fit tracks
            List<CbmLitTrack> fitTracks = new ArrayList<CbmLitTrack>();
            for (int i = 0; i < mollers.length; ++i) {
                int q = -1;
//                System.out.println("starting with track:");
//                System.out.println(mollers[i]);
                // create a physical track
                PhysicalTrack ptrack = makeTrack(mollers[i], vtx, q);
                CartesianThreeVector mom = ptrack.momentum();
                // create a Lit Track State to propagate
                CbmLitTrackParam parIn = new CbmLitTrackParam();

                double[] pars = new double[5];
                pars[0] = vtx.x(); //x
                pars[1] = vtx.y(); //y
                pars[2] = mom.x() / mom.z(); // x' (dx/dz)
                pars[3] = mom.y() / mom.z(); // y' (dy/dz)
                pars[4] = q / mom.magnitude(); // q/p
                parIn.SetStateVector(pars);
                parIn.SetZ(vtx.z());
                // simulate some hits
                List<CbmLitDetPlaneStripHit> hits = makeHits(parIn);
                // a trackParam to use for intializing the fits
                //TODO smear this a bit, see how little I can get away with
                CbmLitTrackParam trkParamZeroEstimate = new CbmLitTrackParam();
                trkParamZeroEstimate.SetQp(pars[4]);
                trkParamZeroEstimate.SetCovariance(0, 999.);
                trkParamZeroEstimate.SetCovariance(5, 999.);
                trkParamZeroEstimate.SetCovariance(9, 999.);
                trkParamZeroEstimate.SetCovariance(12, 999.);
                trkParamZeroEstimate.SetCovariance(14, 999.);
                // now fit the hits
                if (hits.size() == 12) {
                    boolean[] updown = {false};//, true};
                    for (int k = 0; k < updown.length; ++k) {
                        if (debug) {
                            System.out.println("\n fit " + k);
                        }
                        boolean downstream = updown[k];
                        String folder = "mollers" + (downstream ? " downstream" : " upstream");
                        CbmLitTrack fitTrack = fit(hits, trkParamZeroEstimate, downstream);
                        if (fitTrack != null) {
                            compare(fitTrack, parIn, folder, _extrap, downstream);
                            fitTracks.add(fitTrack);
                        } else {
                            System.out.println("fit failed");
                        }
                    }
                } // end of 12-hit tracks
            }// end of loop over mollers
            if(fitTracks.size()==2)
            {
                nTwoGoodMollerTracksEvents++;
            }
        }// end of event loop
        System.out.println("found "+nTwoGoodMollerTracksEvents+" good two-track moller events");

    }

    List<CbmLitDetPlaneStripHit> makeHits(CbmLitTrackParam parIn)
    {
        List<DetectorPlane> planes = _hpsdet.getPlanes();
        Random ru = new Random();
        double sigmaU = .005;
        // the extrapolated state
        CbmLitTrackParam parOut = new CbmLitTrackParam();
        // the simulated hits
        List<CbmLitDetPlaneStripHit> mcPropagatedDetPlaneHits = new ArrayList<CbmLitDetPlaneStripHit>();
        // propagate to each plane in turn
        for (DetectorPlane p : planes) {
            _extrap.Extrapolate(parIn, parOut, p, null);
            if (p.inBounds(parOut.GetX(), parOut.GetY(), parOut.GetZ())) {
                String name = p.name();
                double u = p.u(new CartesianThreeVector(parOut.GetX(), parOut.GetY(), parOut.GetZ()));
                // let's smear this by a gaussian
                double smearedU = u + ru.nextGaussian() * sigmaU;
                if (debug) {
                    System.out.println("intersected " + name);
                    System.out.println("MC propagated  pos " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
                    System.out.println("MC propagated u: " + u + " smeared u " + smearedU + " delta " + (u - smearedU));
                }

                // lets create a strip hit and use this as our first track fit
                CbmLitDetPlaneStripHit mcHit = new CbmLitDetPlaneStripHit(p, smearedU, sigmaU);
                mcPropagatedDetPlaneHits.add(mcHit);
            } else {
//                System.out.println("did not intersect " + p.name());
            }
        } // end of loop over planes
        // check that we have 12 hits
        if (debug) {
            System.out.println("have " + mcPropagatedDetPlaneHits.size() + " hits");
        }
        return mcPropagatedDetPlaneHits;
    }

    PhysicalTrack makeTrack(Lorentz4Vector fourVec, CartesianThreeVector vtx, int q)
    {
        double[] mom = {fourVec.px(), fourVec.py(), fourVec.pz()};
        double[] pos = vtx.vector();
        double E = fourVec.E();
        // create a physical track 
        return new PhysicalTrack(pos, mom, E, q);
    }

    Lorentz4Vector[] genMollers()
    {
        double emass = 0.000511;
        double beamE = 1.056;
        double emom = sqrt(beamE * beamE - emass * emass);

        // create the beam electron
        Momentum4Vector vec = new Momentum4Vector(0., 0., emom, beamE);
        // add on the target electron
        vec.plusEquals(0., 0., 0., emass);
        Lorentz4Vector[] decays = null;
        boolean good = false;
        while (!good) {
            decays = vec.twobodyDecay(emass, emass);
            // check on rough HPS acceptance...
            if (decays[0].E() > 0.3 && decays[0].E() < 0.75) {
                good = true;
            }
        }
        return decays;
    }

    private CbmLitTrack fitIt(List<CbmLitStripHit> hits)
    {
        // create a track
        CbmLitTrack track = new CbmLitTrack();
        // add the hits
        for (CbmLitHit hit : hits) {
            track.AddHit(hit);
        }
        // add start and end states
        CbmLitTrackParam defaultStartParams = new CbmLitTrackParam();
        CbmLitTrackParam defaultEndParams = new CbmLitTrackParam();
        defaultEndParams.SetZ(hits.get(hits.size() - 1).GetZ());
        track.SetParamFirst(defaultStartParams);
        track.SetParamLast(defaultEndParams);
        // fit downstream
        LitStatus status = _fitter.Fit(track, true);
        //      LitStatus status = _iterFitter.Fit(track);
        System.out.println("zPlane fit downstream: " + status);
        System.out.println(track);
        System.out.println(track.GetParamLast());
        status = _fitter.Fit(track, false);
        System.out.println("zPlane fit upstream: " + status);
        System.out.println(track);
        System.out.println(track.GetParamFirst());
        return track;
    }

    private void compare(CbmLitTrack track, CbmLitTrackParam mcp, String folder, CbmLitTrackExtrapolator extrap, boolean downstream)
    {
        double chisq = track.GetChi2();
        if (chisq < 1000) {
            _tree.mkdirs(folder);
            _tree.cd(folder);

            // get the track parameters
            CbmLitTrackParam tp1 = downstream ? track.GetParamLast() : track.GetParamFirst();
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
            aida.cloud1D("z of comparison").fill(z);
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
            _tree.cd("/");
        }
    }
}
