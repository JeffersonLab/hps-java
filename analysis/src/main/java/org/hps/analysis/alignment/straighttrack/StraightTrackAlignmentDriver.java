package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import hep.physics.vec.Hep3Vector;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class StraightTrackAlignmentDriver extends Driver {

    private int _numberOfEventsWritten = 0;
    boolean _debug = true;
    DetectorBuilder _db;
    double _minClusterEnergy = 3.5;

    private AIDA aida = AIDA.defaultInstance();
    // z from blueprints
    // x from fit (was -63? in 2016)
    private double[] H02Wire = {-68.0, 0., -(672.71 - 583.44) * 25.4};

    Map<String, SimTrackerHit> simTrackerHitByModule = new TreeMap<String, SimTrackerHit>();
    Map<String, SiTrackerHitStrip1D> stripTrackerHitByModule = new TreeMap<String, SiTrackerHitStrip1D>();
    Map<String, Hit> stripHitByModule = new TreeMap<String, Hit>();

    protected void detectorChanged(Detector detector) {
        _db = new DetectorBuilder(detector);
        //_db.drawDetector();
    }

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        // Will use calorimeter clusters to define the road in which we search for hits.
        List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
        aida.histogram1D("number of clusters", 10, 0., 10.).fill(clusters.size());
//        Cluster c = null;
//        for (Cluster cluster : clusters) {
//            aida.histogram1D("All Cluster energy", 100, 0., 7.).fill(cluster.getEnergy());
//        }
//        if (clusters.size() != 1) {
//            return;
//        }

        for (Cluster cluster : clusters) {
            aida.histogram1D("All Cluster energy", 100, 0., 7.).fill(cluster.getEnergy());
            if (cluster.getEnergy() > _minClusterEnergy) {
                boolean isFiducial = TriggerModule.inFiducialRegion(cluster);
                String fid = isFiducial ? "fiducial" : "";
                Cluster c = cluster;
                String topOrBottom = c.getPosition()[1] > 0 ? "top " : "bottom ";
                aida.histogram2D("Cal " + fid + " Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                // calculate some slopes and intercepts...
                aida.histogram1D(topOrBottom + fid + "Cluster z", 10, 1443., 1445.).fill(cluster.getPosition()[2]);
                aida.histogram1D(topOrBottom + fid + "Cluster energy", 100, 0., 7.).fill(cluster.getEnergy());

//            }
//        }
//        if (c != null) {
                //OK, we have a good, high-energy cluster in the fiducial part of the calorimeter...
                Point3D P0 = new Point3D(H02Wire[0], H02Wire[1], H02Wire[2]);
                double[] cPos = c.getPosition();
                //String topOrBottom = cPos[1] > 0 ? "top " : "bottom ";
                Point3D P1 = new Point3D(cPos[0], cPos[1], cPos[2]);
                //let's get some SVT strip clusters...
                setupSensors(event);
                // Get the list of fitted hits from the event
                List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
                // Map the fitted hits to their corresponding raw hits
                Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
                for (LCRelation fittedHit : fittedHits) {
                    fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
                }
                List<SiTrackerHitStrip1D> stripClusters = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
                int nStripHits = stripClusters.size();
                aida.histogram1D(topOrBottom + fid + "number of strip clusters", 100, 0., 200.).fill(nStripHits);
                // lets partition the strip clusters into each module
                Map<String, List<SiTrackerHitStrip1D>> hitsPerModuleMap = new HashMap<>();
                for (TrackerHit hit : stripClusters) {
                    List rthList = hit.getRawHits();
                    String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
                    if (!hitsPerModuleMap.containsKey(moduleName)) {
                        hitsPerModuleMap.put(moduleName, new ArrayList<SiTrackerHitStrip1D>());
                        hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
                    } else {
                        hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
                    }
                }
//            for (String s : hitsPerModuleMap.keySet()) {
//                System.out.println(s + " has " + hitsPerModuleMap.get(s).size() + " strip hit clusters");
//            }
                Map<String, SiTrackerHitStrip1D> hitsToFit = new LinkedHashMap<>();
                Map<String, DetectorPlane> detectorPlanesInFit = new LinkedHashMap<>();
                String trackingDetectorName = cPos[1] > 0 ? "topHole" : "bottomHole"; // work on slot later
                List<DetectorPlane> td = _db.getTracker(trackingDetectorName);
                String[] trackerSensorNames = _db.getTrackerSensorNames(trackingDetectorName);
                double maxDist = 5.;
                for (DetectorPlane dp : td) {
                    String moduleName = trackerSensorNames[dp.id() - 1];
//                System.out.println(moduleName);
                    if (hitsPerModuleMap.containsKey(moduleName)) {
//                    System.out.println(moduleName + " has " + hitsPerModuleMap.get(moduleName).size() + " strip hit clusters");

                        // get the best hit in this layer associated with this cluster                     
                        SiTrackerHitStrip1D closest = null;
                        double d = 9999.;
                        for (SiTrackerHitStrip1D stripHit : hitsPerModuleMap.get(moduleName)) {
                            // calculate the intercept of the straight track with this sensor...
                            Hep3Vector intercept = StraightTrackUtils.linePlaneIntersect(P0, P1, dp.origin(), dp.normal());
                            // calculate the distance between this point and the strip
                            LineSegment3D stripLine = stripHit.getHitSegment();
                            double dist = stripLine.distanceTo(new Point3D(intercept));
//                        double d2 = VecOp.cross(stripLine.getDirection(),VecOp.sub(intercept,stripLine.getStartPoint())).magnitude();
//                        System.out.println("dist "+dist+" d2 "+d2);
                            if (abs(dist) < d) {
                                d = dist;
                                closest = stripHit;
                            }
                        }
                        // are we within a reasonable distance?
                        if (abs(d) < maxDist) {
                            hitsToFit.put(moduleName, closest);
                            detectorPlanesInFit.put(moduleName, dp);
                            aida.histogram1D(moduleName + fid + " distance to hit", 100, -maxDist, maxDist).fill(d);
                        }
                    }
//                System.out.println(dp.id() + " " + trackerSensorNames[dp.id()-1]);
//                System.out.println(dp);
                }
                // we now have a list of hits to fit.
                List<Hit> hits = new ArrayList<Hit>();
                List<DetectorPlane> planes = new ArrayList<DetectorPlane>();
                //for now, assign an error based on the size of the strip cluster
                double[] fixedDu = {0., .012, .006};
                for (String s : hitsToFit.keySet()) {
                    SiTrackerHitStrip1D stripHit = hitsToFit.get(s);
//                System.out.println(s + " has a hit at " + stripHit.getPositionAsVector());
                    int size = stripHit.getRawHits().size();
                    double du;
                    if (size < 3) {
                        du = fixedDu[size];
                    } else {
                        du = .04;
                    }
                    double[] pos = stripHit.getPosition();
                    Hit h = makeHit(detectorPlanesInFit.get(s), pos, du);
                    hits.add(h);
                    planes.add(detectorPlanesInFit.get(s));
                }
                aida.histogram1D(topOrBottom + fid + " number of hits to fit", 20, 0., 20.).fill(hits.size());
                // require at least 8 hits for fit
                if (hits.size() > 7) {
                    double[] A0 = {0., 0., -2267.}; // initial guess for (x,y,z) of track 
                    // TODO get estimate for x of beam on wire. Was x=-63 in 2016
                    double[] B0 = {0., 0., 1.}; // initial guess for the track direction
                    // fit the track!
                    TrackFit fit = FitTracks.STR_LINFIT(planes, hits, A0, B0);
                    // Note that track position parameters x & y are reported at the input z.
                    double[] pars = fit.pars();
                    double[] cov = fit.cov();

                    aida.histogram1D(topOrBottom + fid + " x at z=-2267", 100, -100., 0.).fill(pars[0]);
                    aida.histogram1D(topOrBottom + fid + " y at z=-2267", 100, -20., 20.).fill(pars[1]);
                    aida.histogram1D(topOrBottom + fid + " dXdZ at z=-2267", 100, 0., 0.050).fill(pars[2]);
                    aida.histogram1D(topOrBottom + fid + " dYdZ at z=-2267", 100, -0.050, 0.050).fill(pars[3]);
                    aida.histogram1D(topOrBottom + fid + " track fit chiSquared per ndf", 100, 0., 100.).fill(fit.chisq() / fit.ndf());
                    double chisqProb = ChisqProb.gammp(fit.ndf(), fit.chisq());
                    aida.histogram1D(topOrBottom + fid + " track fit chiSquared probability", 100, 0., 1.).fill(chisqProb);
                    aida.histogram2D("Final Cal Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);
                    aida.histogram1D("Final Cluster energy", 100, 0., 7.).fill(c.getEnergy());

                    // let's apply a few cuts here to enable us to skim events...
                    // beam spot x at wire is -63
                    if (abs(pars[0] + 63) < 20) {
                        // beam spot y at wire is 0
                        if (abs(pars[1]) < 15) {
                            // keep this event
                            skipEvent = false;
                        }
                    }
                }
            } // end of loop over clusters with energy greater that min cluster energy
        } // end of loop over clusters
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsWritten++;
        }
    }

    protected void endOfData() {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
    }

    /**
     * Given a DetectorPlane and a global position, return a hit in local
     * coordinates
     *
     * @param p
     * @param pos
     * @return
     */
    public Hit makeHit(DetectorPlane p, double[] pos, double du) {
        Matrix R = p.rot();
        double[] r0 = p.r0();
        Matrix diff = new Matrix(3, 1);
        for (int i = 0; i < 3; ++i) {
            diff.set(i, 0, pos[i] - r0[i]);
        }
//        diff.print(6, 4);
//        System.out.println("pos " + Arrays.toString(pos));
//        System.out.println("r0  " + Arrays.toString(r0));
        Matrix local = R.times(diff);
//        local.print(6, 4);
        double[] u = new double[2];  // 2dim for u and v measurement 
        double[] wt = new double[3]; // lower diag cov matrix
        double[] sigs = p.sigs();
        u[0] = local.get(0, 0);
        wt[0] = 1 / (du * du); //(sigs[0] * sigs[0]);

        return new Hit(u, wt);
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

}
