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
import org.lcsim.recon.tracking.trfzp.PropZZRK;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class HpsTrfFitDriver extends Driver {

    private boolean _debug = false;

    private MaterialSupervisor _materialManager = null;
    private MultipleScattering _scattering = null;
    private Map<Long, Hep3Matrix> rotMap = new HashMap<Long, Hep3Matrix>();
    private Map<Long, Hep3Vector> tranMap = new HashMap<Long, Hep3Vector>();
    private Map<Long, ITransform3D> xformMap = new HashMap<Long, ITransform3D>();
    private Map<Long, String> xformNames = new HashMap<Long, String>();

    private Map<String, DetectorPlane> planes = new HashMap<String, DetectorPlane>();

    private Hep3Vector uHat = new BasicHep3Vector(1, 0, 0);
    private Hep3Vector wHat = new BasicHep3Vector(0, 0, 1);

    private HpsDetector _det;

    private CbmLitRK4TrackExtrapolator _extrap;
    CbmLitTrackFitter _fitter;
    CbmLitTrackFitter _smoother;
    CbmLitTrackFitterIter _iterFitter;

    Random ran = new Random();

    // histograms
    AIDA aida = AIDA.defaultInstance();
    private ITree _tree = aida.tree();

    // create a map of detector phi 
    // recall that CBM measures x, so phi=0 is effectively vertical
    Map<String, Double> detectorPhi = new HashMap<String, Double>();

    // what about trf?
    PropZZRK trfprop;

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

        populatePhiMap();

        // trf
        org.lcsim.recon.tracking.magfield.ConstantMagneticField cmf = new org.lcsim.recon.tracking.magfield.ConstantMagneticField(0, -0.24, 0);
        trfprop = new PropZZRK(cmf);
        System.out.println("trfprop " + trfprop);

    }

    @Override
    protected void process(EventHeader event) {
        System.out.println("Event: " + event.getEventNumber());
        setupSensors(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        List<SimTrackerHit> simTrackerHitList = event.get(SimTrackerHit.class, "TrackerHits");
        List<MCParticle> mcParticles = event.get(MCParticle.class, "MCParticle");
        int nSimTrackerHits = simTrackerHitList.size();

        double[] mcmomentum = mcParticles.get(0).getMomentum().v();

        List<Track> tracks = event.get(Track.class, "MatchedTracks");
        if (tracks.size() != 1) {
            return;
        }
        if (mcParticles.size() != 1) {
            return;
        }
        // create a collection to hold the list of hits on a track
        // use a set to assure no duplicates (although we haven't implemented equals() yet...
        // use TreeSet since it allows us to get a reverse order view of the set, needed for smoothing...
        // could presumably also use an iterator...
        List<TreeSet<CbmLitDetPlaneStripHit>> stripHitTrackList = new ArrayList<TreeSet<CbmLitDetPlaneStripHit>>();
        for (Track t : tracks) {
            List<TrackerHit> hits = t.getTrackerHits();
            if (hits.size() == 6) {
                // create a set of hits associated to this track
                TreeSet<CbmLitDetPlaneStripHit> trackHitList = new TreeSet<CbmLitDetPlaneStripHit>();
//                System.out.println("MC particle momentum at origin " + mcmomentum[0] + " " + mcmomentum[1] + " " + mcmomentum[2]);
//                System.out.println("MC particle momentum at ECal z= " + mcpAtECal[2] + " " + pAtECal[0] + " " + pAtECal[1] + " " + pAtECal[2]);
                //System.out.println("found " + ecalTrackerHits.size() + " trackerhits at ECal scoring plane");
                //System.out.println("found " + nSimTrackerHits + " SVT SimTrackerHits");
                // some arrays for the full fitting procedure...
//                int nHitsToFit = 0;
//                double[] um = new double[12];  // the vector of local measurements u
//                double[] sigma2 = new double[12]; // the squared measurement uncertainties
//                long[] ids = new long[12]; // the module identifier
//                int[] layer = new int[12]; // layer number
//                String[] names = new String[12]; // the sensor names
//                boolean[] isAxial = new boolean[12]; // axial or stero
//                boolean[] isTop = new boolean[12]; // top or bottom
//                Hep3Vector[] ru = new Hep3Vector[12]; // uHat in global coords, viz. L2G time (1,0,0)
//                Hep3Vector[] rw = new Hep3Vector[12]; // wHat in global coords, viz. L2G times (0, 0, 1)
//                Hep3Vector[] r = new Hep3Vector[12]; // translation vector local to global

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
                System.out.println("have " + trackHitList.size() + " hits to fit");

                //lets try to propagate the final state generator MCParticle
                List<CbmLitDetPlaneStripHit> mcPropagatedDetPlaneHits = new ArrayList<CbmLitDetPlaneStripHit>();
                List<CbmLitStripHit> mcPropagatedZPlaneHits = new ArrayList<CbmLitStripHit>();
                for (MCParticle mcp : mcParticles) {
                    if (mcp.getGeneratorStatus() == MCParticle.FINAL_STATE) {
                        //TODO abstract out method to create a track given an MCParticle
                        double[] pos = {mcp.getOriginX(), mcp.getOriginY(), mcp.getOriginZ()};
                        double[] mom = {mcp.getPX(), mcp.getPY(), mcp.getPZ()};
                        double E = mcp.getEnergy();
                        double q = mcp.getCharge();
                        // create a physical track 
                        PhysicalTrack ptrack = new PhysicalTrack(pos, mom, E, (int) q);
                        // create a Lit Track
                        double p = sqrt(mom[0] * mom[0] + mom[1] * mom[1] + mom[2] * mom[2]);
                        CbmLitTrackParam parIn = new CbmLitTrackParam();

                        double[] pars = new double[5];
                        pars[0] = mcp.getOriginX(); //x
                        pars[1] = mcp.getOriginY(); //y
                        pars[2] = mom[0] / mom[2]; // x' (dx/dz)
                        pars[3] = mom[1] / mom[2]; // y' (dy/dz)
                        pars[4] = q / p; // q/p
                        parIn.SetStateVector(pars);
                        parIn.SetZ(mcp.getOriginZ());

                        CbmLitTrackParam parOut = new CbmLitTrackParam();

                        // let's propagate!
                        System.out.println("Propagating MCParticle " + mcp);
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
                            u += ran.nextGaussian() * sigmaU;
                            // lets create a strip hit and use this as our first track fit
                            CbmLitDetPlaneStripHit mcHit = new CbmLitDetPlaneStripHit(dp, u, sigmaU);
                            mcPropagatedDetPlaneHits.add(mcHit);
                            // repeat this for a z plane
                            double z = dp.GetZpos();
                            _extrap.Extrapolate(parIn, parOut, z, null);
                            System.out.println("MC Z propagated  pos " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
                            System.out.println("Z Plane origin " + dp.position());
                            CbmLitStripHit zhit = new CbmLitStripHit();

                            //TODO understand what's going on here...
                            // it appears that CBM has swapped x and y...
                            //phi+=PI/2.;
                            double phi = hit.GetPhi();
                            double zu = cos(phi) * parOut.GetX() + sin(phi) * parOut.GetY();
                            System.out.println("MC Z propagated u: " + zu);
                            zu += ran.nextGaussian() * sigmaU;
                            zhit.SetPhi(phi);
                            zhit.SetU(zu);
                            zhit.SetDu(sigmaU);
                            zhit.SetZ(z);
                            zhit.SetDz(.0001);
                            mcPropagatedZPlaneHits.add(zhit);
                        }
                        // OK, ready to fit!
                        System.out.println("MC parIn: " + parIn);
                        System.out.println("MC parOut: " + parOut);
//                        CbmLitTrack detTrack = fit(mcPropagatedDetPlaneHits);
//                        compare(detTrack, parIn, "detTrack");
                        CbmLitTrack zTrack = fitIt(mcPropagatedZPlaneHits);
                        compare(zTrack, parIn, "zTrack");
                        //TODO extrapolate to zero, compare to MC, create residuals and pulls
                    }
                } // end of loop over MCParticles
            }  // end of check on six hits
        }  // end of loop over tracks
        // so, did we find any fittable tracks?
//        System.out.println("Event "+event.getEventNumber()+" found "+stripHitTracks.size()+" tracks to fit...");
//        for(Set<HpsStripHit> hits : stripHitTracks)
//        {
//            for(HpsStripHit hit : hits)
//            {
//                System.out.println(hit);
//            }
//        }
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
        // extrapolate our track to the is z position
        _extrap.Extrapolate(tp1, tAtOrigin, z, null);
        System.out.println("MC parameters             : " + mcp);
        System.out.println("track parameters at origin: " + tAtOrigin);
        double[] mcStateVector = mcp.GetStateVector();
        double[] tStateVector = tAtOrigin.GetStateVector();
        String[] label = {"x", "y", "tx", "ty", "qp"};
        double[] covMat = tAtOrigin.GetCovMatrix();
        int[] index = {0, 5, 9, 12, 14};
        for (int i = 0; i < 5; ++i) {
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

    private CbmLitTrack fit(List<CbmLitDetPlaneStripHit> hits) {
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
        System.out.println("detPlane fit downstream: " + status);
        System.out.println(track);
        System.out.println(track.GetParamLast());
        status = _fitter.Fit(track, false);
        System.out.println("detPlane fit upstream: " + status);
        System.out.println(track);
        System.out.println(track.GetParamFirst());
        return track;
    }

    private CbmLitTrack fitIt(List<CbmLitStripHit> hits) {
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

//    private void setupTransforms(Detector det)
//    {
//        List<MaterialSupervisor.ScatteringDetectorVolume> stripPlanes = _materialManager.getMaterialVolumes();
//        for (MaterialSupervisor.ScatteringDetectorVolume vol : stripPlanes) {
//            MaterialSupervisor.SiStripPlane plane = (MaterialSupervisor.SiStripPlane) vol;
//
//            if (_debug) {
//                System.out.println(plane.getName());
//            }
//
//            Hep3Vector oprime = CoordinateTransformations.transformVectorToDetector(plane.origin());
//            Hep3Vector nprime = CoordinateTransformations.transformVectorToDetector(plane.normal());
//
//            if (_debug) {
//                System.out.println(" origin: " + oprime);
//            }
//
//            if (_debug) {
//                System.out.println(" normal: " + nprime);
//            }
//
//            if (_debug) {
//                System.out.println(" Plane is: " + plane.getMeasuredDimension() + " x " + plane.getUnmeasuredDimension());
//            }
//
//            HpsSiSensor sensor = (HpsSiSensor) plane.getSensor();
//            // create a DetectorPlane object
//            String name = sensor.getName();
//            //TODO fix the number of radiation lengths here
//            double x0 = .003;
//            ITransform3D l2g = sensor.getGeometry().getLocalToGlobal();
//            ITransform3D g2l = sensor.getGeometry().getGlobalToLocal();
//            CartesianThreeVector pos = new CartesianThreeVector(oprime.x(), oprime.y(), oprime.z());
//            CartesianThreeVector normal = new CartesianThreeVector(nprime.x(), nprime.y(), nprime.z());
//            DetectorPlane dPlane = new DetectorPlane(name, pos, normal, l2g, g2l, x0);
//            planes.put(name, dPlane);
//            _det.addDetectorPlane(dPlane);
//
//            long ID = sensor.getIdentifier().getValue();
//            xformMap.put(ID, l2g);
//            rotMap.put(ID, l2g.getRotation().getRotationMatrix());
//            tranMap.put(ID, l2g.getTranslation().getTranslationVector());
//            xformNames.put(ID, sensor.getName());
//
////            if (debug) {
////                if(_debug) System.out.println(SvtUtils.getInstance().isAxial(sensor) ? "axial" : "stereo");
////            }
//            Hep3Vector measDir = CoordinateTransformations.transformVectorToDetector(plane.getMeasuredCoordinate());
//
//            if (_debug) {
//                System.out.println("measured coordinate:    " + measDir);
//            }
//
//            Hep3Vector unmeasDir = CoordinateTransformations.transformVectorToDetector(plane.getUnmeasuredCoordinate());
//
//            if (_debug) {
//                System.out.println("unmeasured coordinate:   " + unmeasDir);
//            }
//
//            if (_debug) {
//                System.out.println("thickness: " + plane.getThickness() + " in X0: " + plane.getThicknessInRL());
//            }
//
//        }
//    }
    private void populatePhiMap() {
        detectorPhi.put("module_L1t_halfmodule_axial_sensor0", PI / 2.);
        detectorPhi.put("module_L1t_halfmodule_stereo_sensor0", -PI / 2. - 0.100);
        detectorPhi.put("module_L1b_halfmodule_stereo_sensor0", PI / 2. + 0.100);
        detectorPhi.put("module_L1b_halfmodule_axial_sensor0", -PI / 2.);
        detectorPhi.put("module_L2t_halfmodule_axial_sensor0", PI / 2.);
        detectorPhi.put("module_L2t_halfmodule_stereo_sensor0", -PI / 2. - 0.100);
        detectorPhi.put("module_L2b_halfmodule_stereo_sensor0", PI / 2. + 0.100);
        detectorPhi.put("module_L2b_halfmodule_axial_sensor0", -PI / 2.);
        detectorPhi.put("module_L3t_halfmodule_axial_sensor0", PI / 2.);
        detectorPhi.put("module_L3t_halfmodule_stereo_sensor0", -PI / 2. - 0.100);
        detectorPhi.put("module_L3b_halfmodule_stereo_sensor0", PI / 2. + 0.100);
        detectorPhi.put("module_L3b_halfmodule_axial_sensor0", -PI / 2.);
        detectorPhi.put("module_L4t_halfmodule_axial_slot_sensor0", -PI / 2.);
        detectorPhi.put("module_L4t_halfmodule_axial_hole_sensor0", PI / 2.);
        detectorPhi.put("module_L4t_halfmodule_stereo_slot_sensor0", PI / 2. - 0.050);
        detectorPhi.put("module_L4t_halfmodule_stereo_hole_sensor0", -PI / 2. - 0.050);
        detectorPhi.put("module_L4b_halfmodule_stereo_slot_sensor0", -PI / 2. + 0.050);
        detectorPhi.put("module_L4b_halfmodule_stereo_hole_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L4b_halfmodule_axial_slot_sensor0", PI / 2.);
        detectorPhi.put("module_L4b_halfmodule_axial_hole_sensor0", -PI / 2.);
        detectorPhi.put("module_L5t_halfmodule_axial_slot_sensor0", -PI / 2.);
        detectorPhi.put("module_L5t_halfmodule_axial_hole_sensor0", PI / 2.);
        detectorPhi.put("module_L5t_halfmodule_stereo_slot_sensor0", PI / 2. - 0.050);
        detectorPhi.put("module_L5t_halfmodule_stereo_hole_sensor0", -PI / 2. - 0.050);
        detectorPhi.put("module_L5b_halfmodule_stereo_slot_sensor0", -PI / 2. + 0.050);
        detectorPhi.put("module_L5b_halfmodule_stereo_hole_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L5b_halfmodule_axial_slot_sensor0", PI / 2.);
        detectorPhi.put("module_L5b_halfmodule_axial_hole_sensor0", -PI / 2.);
        detectorPhi.put("module_L6t_halfmodule_axial_slot_sensor0", -PI / 2.);
        detectorPhi.put("module_L6t_halfmodule_axial_hole_sensor0", PI / 2.);
        detectorPhi.put("module_L6t_halfmodule_stereo_slot_sensor0", PI / 2. - 0.050);
        detectorPhi.put("module_L6t_halfmodule_stereo_hole_sensor0", -PI / 2. - 0.050);
        detectorPhi.put("module_L6b_halfmodule_stereo_slot_sensor0", -PI / 2. + 0.050);
        detectorPhi.put("module_L6b_halfmodule_stereo_hole_sensor0", PI / 2. + 0.050);
        detectorPhi.put("module_L6b_halfmodule_axial_slot_sensor0", PI / 2.);
        detectorPhi.put("module_L6b_halfmodule_axial_hole_sensor0", -PI / 2.);
    }
}
