package org.hps.recon.tracking.lit;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.hps.recon.tracking.CoordinateTransformations;
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
import org.lcsim.util.Driver;

/**
 *
 * @author Norman A. Graf
 */
public class HpsLitFitDriver extends Driver {

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

    private HpsDetector _det = new HpsDetector();

    private CbmLitRK4TrackExtrapolator _extrap;

    protected void detectorChanged(Detector detector) {
        _materialManager = new MaterialSupervisor();
        _scattering = new MultipleScattering(_materialManager);
        _materialManager.buildModel(detector);
        // get the rotation and translation quantities for the detectors.
        setupTransforms(detector);
        HpsMagField field = new HpsMagField(detector.getFieldMap());
        _extrap = new CbmLitRK4TrackExtrapolator(field);
        System.out.println(_det);
    }

    @Override
    protected void process(EventHeader event) {
        setupSensors(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        // seems to be a problem with these hits, punt for now
//        List<SimTrackerHit> ecalTrackerHits = event.get(SimTrackerHit.class, "TrackerHitsECal");
//        if(ecalTrackerHits.size()==0) return;
        List<SimTrackerHit> simTrackerHitList = event.get(SimTrackerHit.class, "TrackerHits");
        List<MCParticle> mcParticles = event.get(MCParticle.class, "MCParticle");
        int nSimTrackerHits = simTrackerHitList.size();

        double[] mcmomentum = mcParticles.get(0).getMomentum().v();
//        double[] pAtECal = ecalTrackerHits.get(0).getMomentum();
//        double[] mcpAtECal = ecalTrackerHits.get(0).getPosition();

        List<Track> tracks = event.get(Track.class, "MatchedTracks");
        //System.out.println("found " + tracks.size() + " tracks");
        // create a collection to hold the list of hits on a track
        // use a set to assure no duplicates (although we haven't implemented equals() yet...
        // use TreeSet since it allows us to get a revrese order view of the set, needed for smoothing...
        // could presumably also use an iterator...
        List<TreeSet<HpsStripHit>> stripHitTracks = new ArrayList<TreeSet<HpsStripHit>>();
        for (Track t : tracks) {
            List<TrackerHit> hits = t.getTrackerHits();
            if (hits.size() == 6) {
                // create a set of hits associated to this track
                TreeSet<HpsStripHit> trackHitList = new TreeSet<HpsStripHit>();
                System.out.println("MC particle momentum at origin " + mcmomentum[0] + " " + mcmomentum[1] + " " + mcmomentum[2]);
//                System.out.println("MC particle momentum at ECal z= " + mcpAtECal[2] + " " + pAtECal[0] + " " + pAtECal[1] + " " + pAtECal[2]);
                //System.out.println("found " + ecalTrackerHits.size() + " trackerhits at ECal scoring plane");
                //System.out.println("found " + nSimTrackerHits + " SVT SimTrackerHits");
                // some arrays for the full fitting procedure...
                int nHitsToFit = 0;
                double[] um = new double[12];  // the vector of local measurements u
                double[] sigma2 = new double[12]; // the squared measurement uncertainties
                long[] ids = new long[12]; // the module identifier
                int[] layer = new int[12]; // layer number
                String[] names = new String[12]; // the sensor names
                boolean[] isAxial = new boolean[12]; // axial or stero
                boolean[] isTop = new boolean[12]; // top or bottom
                Hep3Vector[] ru = new Hep3Vector[12]; // uHat in global coords, viz. L2G time (1,0,0)
                Hep3Vector[] rw = new Hep3Vector[12]; // wHat in global coords, viz. L2G times (0, 0, 1)
                Hep3Vector[] r = new Hep3Vector[12]; // translation vector local to global

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
                        Hep3Vector posG = new BasicHep3Vector(strip.getPosition());
                        Hep3Vector posL = sensor.getGeometry().getGlobalToLocal().transformed(posG);
                        // OK, now let's try the explicit rotation and translation...
                        long ID = sensor.getIdentifier().getValue();
                        Hep3Matrix rotMat = rotMap.get(ID);
                        Hep3Vector transVec = tranMap.get(ID);

                        // OK, now for the full fit...
                        names[nHitsToFit] = sensor.getName();
                        ids[nHitsToFit] = sensor.getIdentifier().getValue();
                        isAxial[nHitsToFit] = sensor.isAxial();
                        // get layer number and whether top or bottom...
                        String tmp = xformNames.get(ids[nHitsToFit]);
                        for (int j = 1; j < 7; ++j) {
                            if (tmp.startsWith("module_L" + j)) {
                                layer[nHitsToFit] = j;
                                if (tmp.contains("L" + j + "t")) {
                                    isTop[nHitsToFit] = true;
                                }
                            }
                        }

                        um[nHitsToFit] = posL.x(); // u measurement in local coordinates...
                        SymmetricMatrix covG = new SymmetricMatrix(3, strip.getCovMatrix(), true);
                        SymmetricMatrix covL = sensor.getGeometry().getGlobalToLocal().transformed(covG);
                        sigma2[nHitsToFit] = covL.e(0, 0);
                        ru[nHitsToFit] = VecOp.mult(rotMat, uHat);
                        rw[nHitsToFit] = VecOp.mult(rotMat, wHat);
                        r[nHitsToFit] = transVec;
                        // create a hit and add it to the collection of track hits
                        HpsStripHit trackHit = new HpsStripHit(um[nHitsToFit], sqrt(sigma2[nHitsToFit]), planes.get(names[nHitsToFit]));
                        trackHitList.add(trackHit);
                        if (_debug) {
                            System.out.println("layer[ " + nHitsToFit + " ]= " + layer[nHitsToFit] + (isTop[nHitsToFit] ? " top " : " bottom ") + (isAxial[nHitsToFit] ? " axial " : " stereo "));
                            System.out.println("u_m[ " + nHitsToFit + " ]= " + um[nHitsToFit]);
                            System.out.println("sigma2[ " + nHitsToFit + " ]= " + sigma2[nHitsToFit]);
                        }
                        nHitsToFit++;
                    }
                } // end of loop over hits...
                System.out.println("have " + nHitsToFit + " hits to fit");
                // add this set of hits to our collection of "tracks"
                stripHitTracks.add(trackHitList);
                //OK, should have 12 strip hits to fit...
                //lets try to propagate the final state generator MCParticle
                for (MCParticle mcp : mcParticles) {
                    if (mcp.getGeneratorStatus() == MCParticle.FINAL_STATE) {
                        //TODO abstract out method to create a track given an MCParticle
                        double[] pos = {mcp.getOriginX(), mcp.getOriginY(), mcp.getOriginZ()};
                        double[] mom = {mcp.getPX(), mcp.getPY(), mcp.getPZ()};
                        double E = mcp.getEnergy();
                        double q = mcp.getCharge();
                        // create a physical track 
                        PhysicalTrack ptrack = new PhysicalTrack(pos, mom, E, (int) q);
                        // create a Li Track
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
                        //TODO remove fixed size loop here and replace arrays with collections.
                        for (int i = 0; i < 12; ++i) {

                            DetectorPlane dp = planes.get(names[i]);
                            _extrap.Extrapolate(parIn, dp, null);
                            System.out.println(parIn.GetX() + " " + parIn.GetY() + " " + parIn.GetZ());
                            System.out.println("u: " + dp.u(new CartesianThreeVector(parIn.GetX(), parIn.GetY(), parIn.GetZ())));
                        }
                        // now extrapolate to the ECal
//                        _extrap.Extrapolate(parIn, mcpAtECal[2], null);
//                        System.out.println("mcp propagated to ECal "+parIn.GetX() + " " + parIn.GetY() + " " + parIn.GetZ());
//                        System.out.println("mcp SimTrackerHit at ECal "+mcpAtECal[0] + " " + mcpAtECal[1] + " " + mcpAtECal[2]);

                    }
                } // end of loop over MCParticles
            }  // end of check on six hits
        }  // end of loop over tracks
        // so, did we find any fittable tracks?
        System.out.println("Event " + event.getEventNumber() + " found " + stripHitTracks.size() + " tracks to fit...");
        for (Set<HpsStripHit> hits : stripHitTracks) {
            for (HpsStripHit hit : hits) {
                System.out.println(hit);
            }
        }
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

    private void setupTransforms(Detector det) {
        List<MaterialSupervisor.ScatteringDetectorVolume> stripPlanes = _materialManager.getMaterialVolumes();
        for (MaterialSupervisor.ScatteringDetectorVolume vol : stripPlanes) {
            MaterialSupervisor.SiStripPlane plane = (MaterialSupervisor.SiStripPlane) vol;

            if (_debug) {
                System.out.println(plane.getName());
            }

            Hep3Vector oprime = CoordinateTransformations.transformVectorToDetector(plane.origin());
            Hep3Vector nprime = CoordinateTransformations.transformVectorToDetector(plane.normal());

            if (_debug) {
                System.out.println(" origin: " + oprime);
            }

            if (_debug) {
                System.out.println(" normal: " + nprime);
            }

            if (_debug) {
                System.out.println(" Plane is: " + plane.getMeasuredDimension() + " x " + plane.getUnmeasuredDimension());
            }

            HpsSiSensor sensor = (HpsSiSensor) plane.getSensor();
            // create a DetectorPlane object
            String name = sensor.getName();
            //TODO fix the number of radiation lengths here
            double x0 = .003;
            ITransform3D l2g = sensor.getGeometry().getLocalToGlobal();
            ITransform3D g2l = sensor.getGeometry().getGlobalToLocal();
            CartesianThreeVector pos = new CartesianThreeVector(oprime.x(), oprime.y(), oprime.z());
            CartesianThreeVector normal = new CartesianThreeVector(nprime.x(), nprime.y(), nprime.z());
            DetectorPlane dPlane = new DetectorPlane(name, pos, normal, l2g, g2l, x0);
            planes.put(name, dPlane);
            _det.addDetectorPlane(dPlane);

            long ID = sensor.getIdentifier().getValue();
            xformMap.put(ID, l2g);
            rotMap.put(ID, l2g.getRotation().getRotationMatrix());
            tranMap.put(ID, l2g.getTranslation().getTranslationVector());
            xformNames.put(ID, sensor.getName());

//            if (debug) {
//                if(_debug) System.out.println(SvtUtils.getInstance().isAxial(sensor) ? "axial" : "stereo");
//            }
            Hep3Vector measDir = CoordinateTransformations.transformVectorToDetector(plane.getMeasuredCoordinate());

            if (_debug) {
                System.out.println("measured coordinate:    " + measDir);
            }

            Hep3Vector unmeasDir = CoordinateTransformations.transformVectorToDetector(plane.getUnmeasuredCoordinate());

            if (_debug) {
                System.out.println("unmeasured coordinate:   " + unmeasDir);
            }

            if (_debug) {
                System.out.println("thickness: " + plane.getThickness() + " in X0: " + plane.getThicknessInRL());
            }

        }
    }
}
