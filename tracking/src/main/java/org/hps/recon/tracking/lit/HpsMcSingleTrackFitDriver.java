package org.hps.recon.tracking.lit;

import hep.aida.ITree;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * A Driver used for development to be run over single particle MC events
 *
 * @author Norman A. Graf
 */
public class HpsMcSingleTrackFitDriver extends Driver {

    private boolean _debug = true;

    // histograms
    AIDA aida = AIDA.defaultInstance();
    private ITree _tree = aida.tree();

    private MaterialSupervisor _materialManager = null;
    private MultipleScattering _scattering = null;
    private Map<Long, Hep3Matrix> rotMap = new HashMap<Long, Hep3Matrix>();
    private Map<Long, Hep3Vector> tranMap = new HashMap<Long, Hep3Vector>();
    private Map<Long, ITransform3D> xformMap = new HashMap<Long, ITransform3D>();
    private Map<Long, String> xformNames = new HashMap<Long, String>();

    private Hep3Vector uHat = new BasicHep3Vector(1, 0, 0);
    private Hep3Vector wHat = new BasicHep3Vector(0, 0, 1);

    private HpsDetector _det;
    // create a map of detector phi 
    // recall that CBM measures x, so phi=0 is effectively vertical
    Map<String, Double> detectorPhi = new HashMap<String, Double>();

    private CbmLitRK4TrackExtrapolator _extrap;
    CbmLitTrackFitter _fitter;
    CbmLitTrackFitter _smoother;
    CbmLitTrackFitterIter _iterFitter;

    Random ran = new Random();

    private Map<String, DetectorPlane> planes = new HashMap<String, DetectorPlane>();

    protected void detectorChanged(Detector detector) {
        _materialManager = new MaterialSupervisor();
        _scattering = new MultipleScattering(_materialManager);
        _materialManager.buildModel(detector);
        // get the rotation and translation quantities for the detectors.
//        setupTransforms(detector);
        // a constant magnetic field...
        ConstantMagneticField bfield = new ConstantMagneticField(0., -0.24, 0.);

        HpsMagField field = new HpsMagField(detector.getFieldMap());
        _extrap = new CbmLitRK4TrackExtrapolator(bfield);

        // a Kalman Filter updater...
        CbmLitTrackUpdate trackUpdate = new CbmLitKalmanFilter();
        CbmLitTrackPropagator prop = new SimpleTrackPropagator(_extrap);
        _fitter = new CbmLitTrackFitterImp(prop, trackUpdate);
        _smoother = new CbmLitKalmanSmoother();
        _iterFitter = new CbmLitTrackFitterIter(_fitter, _smoother);

        _det = new HpsDetector(detector);
        List<DetectorPlane> planeList = _det.getPlanes();
        for (DetectorPlane p : planeList) {
            planes.put(p.name(), p);
        }
        System.out.println(_det);
        populatePhiMap2();
    }

    protected void process(EventHeader event) {
        System.out.println("Event: " + event.getEventNumber());
        setupSensors(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        List<SimTrackerHit> simTrackerHitList = event.get(SimTrackerHit.class, "TrackerHits");
        List<MCParticle> mcParticles = event.get(MCParticle.class, "MCParticle");
        int nSimTrackerHits = simTrackerHitList.size();

        double[] mcmomentum = mcParticles.get(0).getMomentum().v();

        List<org.lcsim.event.Track> tracks = event.get(org.lcsim.event.Track.class, "MatchedTracks");
        if (tracks.size() != 1) {
            return;
        }
        if (mcParticles.size() != 1) {
            return;
        }

        Track t = tracks.get(0);
        List<TrackerHit> hits = t.getTrackerHits();
        if (hits.size() != 6) {
            return;
        }
        MCParticle mcp = mcParticles.get(0);

        CbmLitTrackParam mcparams = mcParams(mcp);
        //lets try to propagate the final state generator MCParticle
        List<CbmLitDetPlaneStripHit> mcPropagatedDetPlaneHits = new ArrayList<CbmLitDetPlaneStripHit>();
        List<CbmLitStripHit> mcPropagatedZPlaneHits = new ArrayList<CbmLitStripHit>();

        System.out.println("found " + tracks.size() + " track with " + hits.size() + " hits and " + mcParticles.size() + " MC Particle");

        List<TreeSet<CbmLitDetPlaneStripHit>> stripHitTrackList = new ArrayList<TreeSet<CbmLitDetPlaneStripHit>>();
        // create a set of hits associated to this track
        List<CbmLitDetPlaneStripHit> trackHitList = new ArrayList<CbmLitDetPlaneStripHit>();

        for (TrackerHit h : hits) {
            Set<TrackerHit> stripList = hitToStrips.allFrom(hitToRotated.from(h));
            for (TrackerHit strip : stripList) {
                List rawHits = strip.getRawHits();
                HpsSiSensor sensor = null;
                for (Object o : rawHits) {
                    RawTrackerHit rth = (RawTrackerHit) o;
                    // TODO figure out why the following collection is always null
                    //List<SimTrackerHit> stipMCHits = rth.getSimTrackerHits();
                    sensor = (HpsSiSensor) rth.getDetectorElement();
                }
                Hep3Vector globalPos = new BasicHep3Vector(strip.getPosition());
                Hep3Vector localPos = sensor.getGeometry().getGlobalToLocal().transformed(globalPos);

                double u = localPos.x(); // u measurement in local coordinates...
                SymmetricMatrix globalCovMatrix = new SymmetricMatrix(3, strip.getCovMatrix(), true);
                SymmetricMatrix localCovMatrix = sensor.getGeometry().getGlobalToLocal().transformed(globalCovMatrix);
                double du = sqrt(localCovMatrix.e(0, 0));
                String name = sensor.getName();
                // create a hit and add it to the collection of track hits
                CbmLitDetPlaneStripHit trackHit = new CbmLitDetPlaneStripHit(planes.get(name), u, du);
                trackHitList.add(trackHit);
            }
        } // end of loop over hits...
        //OK, should have 12 strip hits to fit...
        System.out.println("have " + trackHitList.size() + " strip hits to fit");
        CbmLitTrack zTrack = fitIt(trackHitList);
        System.out.println("fitted zTrack: " + zTrack);
        compare(zTrack, mcparams, "zTrack");

        // let's compare this to the extrapolated MC track...
        List<CbmLitStripHit> mcHitList = mcTrackHitList(mcparams, trackHitList);
        CbmLitTrack mcZtrack = fitItStripHit(mcHitList);
        System.out.println("fitted mcZrack: " + mcZtrack);
        compare(mcZtrack, mcparams, "mcZTrack");
    }

    private List<CbmLitStripHit> mcTrackHitList(CbmLitTrackParam parIn, List<CbmLitDetPlaneStripHit> trackHitList) {
        CbmLitTrackParam parOut = new CbmLitTrackParam();
        List<CbmLitStripHit> mcPropagatedZPlaneHits = new ArrayList<CbmLitStripHit>();
        List<CbmLitDetPlaneStripHit> mcPropagatedDetPlaneHits = new ArrayList<CbmLitDetPlaneStripHit>();

        // let's propagate!
        double sigmaU = .005;
        //TODO remove fixed size loop here and replace arrays with collections.
        for (CbmLitDetPlaneStripHit hit : trackHitList) {
            DetectorPlane dp = hit.GetPlane();

            _extrap.Extrapolate(parIn, parOut, dp, null);
            String name = dp.name();
            double u = dp.u(new CartesianThreeVector(parOut.GetX(), parOut.GetY(), parOut.GetZ()));
            System.out.println(name);
            System.out.println("MC propagated  pos " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
            System.out.println("MC propagated u: " + u);
            System.out.println("hit u: " + hit.GetU());
            // let's smear this by a gaussian
            double smearedU = u + ran.nextGaussian() * sigmaU;

            // lets create a strip hit and use this as our first track fit
            CbmLitDetPlaneStripHit mcHit = new CbmLitDetPlaneStripHit(dp, smearedU, sigmaU);
            mcPropagatedDetPlaneHits.add(mcHit);
            // repeat this for a z plane
            double z = dp.GetZpos();
            _extrap.Extrapolate(parIn, parOut, z, null);
            System.out.println("MC Z propagated  pos " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
            System.out.println("Z Plane origin " + dp.position());
            CbmLitStripHit zhit = new CbmLitStripHit();

            // CBM measures phi from the vertical...
            System.out.println("hit phi " + hit.GetPhi() + " detector map phi" + detectorPhi.get(name));
            double detPhi = detectorPhi.get(name);
            double detPhiU = cos(detPhi) * parOut.GetX() + sin(detPhi) * parOut.GetY();
            double litPhi = hit.GetPhi() + PI / 2.;
            double zu = cos(litPhi) * parOut.GetX() + sin(litPhi) * parOut.GetY();
            System.out.println("u " + u + " detPhiU " + detPhiU);
            zu += ran.nextGaussian() * sigmaU;
            System.out.println("LitStripHit smeared zu: " + zu + "MC phi: " + hit.GetPhi() + " litPhi " + litPhi);

            zhit.SetPhi(litPhi);
            zhit.SetU(zu);
            zhit.SetDu(sigmaU);
            zhit.SetZ(z);
            zhit.SetDz(.0001);
            mcPropagatedZPlaneHits.add(zhit);

            System.out.println("***** " + name + " hitPhi " + hit.GetPhi());
            System.out.println("MC propagated  pos " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
            System.out.println("MC propagated u: " + u);
            System.out.println("hit u: " + hit.GetU());
        }
        return mcPropagatedZPlaneHits;
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = null;
        if (event.hasCollection(RawTrackerHit.class, "SVTRawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        }
        if (event.hasCollection(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
        }
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }

    private CbmLitTrack fitIt(List<CbmLitDetPlaneStripHit> hits) {
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
        if (_debug) {
            System.out.println("zPlane fit downstream: " + status);
            System.out.println(track);
            System.out.println(track.GetParamLast());
            status = _fitter.Fit(track, false);
            System.out.println("zPlane fit upstream: " + status);
            System.out.println(track);
            System.out.println(track.GetParamFirst());
        }
        return track;
    }

    private CbmLitTrack fitItStripHit(List<CbmLitStripHit> hits) {
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
        if (_debug) {
            System.out.println("zPlane fit downstream: " + status);
            System.out.println(track);
            System.out.println(track.GetParamLast());
            status = _fitter.Fit(track, false);
            System.out.println("zPlane fit upstream: " + status);
            System.out.println(track);
            System.out.println(track.GetParamFirst());
        }
        return track;
    }

    // simplify this for testing
    private void populatePhiMap2() {
        detectorPhi.put("module_L1t_halfmodule_axial_sensor0", PI / 2.);
        detectorPhi.put("module_L1t_halfmodule_stereo_sensor0", PI / 2. + 0.100);
        detectorPhi.put("module_L1b_halfmodule_stereo_sensor0", PI / 2. + 0.100);
        detectorPhi.put("module_L1b_halfmodule_axial_sensor0", PI / 2.);
        detectorPhi.put("module_L2t_halfmodule_axial_sensor0", PI / 2.);
        detectorPhi.put("module_L2t_halfmodule_stereo_sensor0", PI / 2. + 0.100);
        detectorPhi.put("module_L2b_halfmodule_stereo_sensor0", PI / 2. + 0.100);
        detectorPhi.put("module_L2b_halfmodule_axial_sensor0", PI / 2.);
        detectorPhi.put("module_L3t_halfmodule_axial_sensor0", PI / 2.);
        detectorPhi.put("module_L3t_halfmodule_stereo_sensor0", PI / 2. + 0.100);
        detectorPhi.put("module_L3b_halfmodule_stereo_sensor0", PI / 2. + 0.100);
        detectorPhi.put("module_L3b_halfmodule_axial_sensor0", PI / 2.);
        detectorPhi.put("module_L4t_halfmodule_axial_slot_sensor0", PI / 2.);
        detectorPhi.put("module_L4t_halfmodule_axial_hole_sensor0", PI / 2.);
        detectorPhi.put("module_L4t_halfmodule_stereo_slot_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L4t_halfmodule_stereo_hole_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L4b_halfmodule_stereo_slot_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L4b_halfmodule_stereo_hole_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L4b_halfmodule_axial_slot_sensor0", PI / 2.);
        detectorPhi.put("module_L4b_halfmodule_axial_hole_sensor0", PI / 2.);
        detectorPhi.put("module_L5t_halfmodule_axial_slot_sensor0", PI / 2.);
        detectorPhi.put("module_L5t_halfmodule_axial_hole_sensor0", PI / 2.);
        detectorPhi.put("module_L5t_halfmodule_stereo_slot_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L5t_halfmodule_stereo_hole_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L5b_halfmodule_stereo_slot_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L5b_halfmodule_stereo_hole_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L5b_halfmodule_axial_slot_sensor0", PI / 2.);
        detectorPhi.put("module_L5b_halfmodule_axial_hole_sensor0", PI / 2.);
        detectorPhi.put("module_L6t_halfmodule_axial_slot_sensor0", PI / 2.);
        detectorPhi.put("module_L6t_halfmodule_axial_hole_sensor0", PI / 2.);
        detectorPhi.put("module_L6t_halfmodule_stereo_slot_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L6t_halfmodule_stereo_hole_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L6b_halfmodule_stereo_slot_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L6b_halfmodule_stereo_hole_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L6b_halfmodule_axial_slot_sensor0", PI / 2.);
        detectorPhi.put("module_L6b_halfmodule_axial_hole_sensor0", PI / 2.);
    }

    private CbmLitTrackParam mcParams(MCParticle mcp) {
        CbmLitTrackParam parIn = new CbmLitTrackParam();
        double[] mom = {mcp.getPX(), mcp.getPY(), mcp.getPZ()};
        double p = sqrt(mom[0] * mom[0] + mom[1] * mom[1] + mom[2] * mom[2]);
        double q = mcp.getCharge();
        double[] pars = new double[5];
        pars[0] = mcp.getOriginX(); //x
        pars[1] = mcp.getOriginY(); //y
        pars[2] = mom[0] / mom[2]; // x' (dx/dz)
        pars[3] = mom[1] / mom[2]; // y' (dy/dz)
        pars[4] = q / p; // q/p
        parIn.SetStateVector(pars);
        parIn.SetZ(mcp.getOriginZ());

        return parIn;
    }

    private void compare(CbmLitTrack track, CbmLitTrackParam mcp, String folder) {
        _tree.mkdirs(folder);
        _tree.cd(folder);
        // get the upstream track parameters
        CbmLitTrackParam tp1 = track.GetParamFirst();
        // output parameters
        CbmLitTrackParam tAtOrigin = new CbmLitTrackParam();
        // find z where we should compare
        double z = mcp.GetZ();
        // extrapolate our track to this z position
        //TODO change to propagate since field is changing here...
        _extrap.Extrapolate(tp1, tAtOrigin, z, null);
        System.out.println("MC parameters             : " + mcp);
        System.out.println("track parameters at origin: " + tAtOrigin);
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
        double chisq = track.GetChi2();
        int ndf = track.GetNDF();
        aida.cloud1D("Chisq").fill(chisq);
        aida.cloud1D("Chisq Probability").fill(ChisqProb.gammq(ndf, chisq));
        aida.cloud1D("Momentum").fill(abs(1. / tStateVector[4]));
        _tree.cd("/");
    }

}
