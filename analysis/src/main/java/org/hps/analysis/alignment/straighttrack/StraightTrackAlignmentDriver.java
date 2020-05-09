package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import hep.physics.vec.Hep3Vector;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.Math.PI;
//import java.io.BufferedWriter;
//import java.io.FileOutputStream;
//import java.io.OutputStreamWriter;
//import java.io.Writer;
import static java.lang.Math.abs;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hps.analysis.alignment.straighttrack.FitTracks.GEN_ROTMAT;
import org.hps.analysis.alignment.straighttrack.vertex.StraightLineVertexFitter;
import org.hps.analysis.alignment.straighttrack.vertex.Track;
import org.hps.analysis.alignment.straighttrack.vertex.Vertex;
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
    boolean _debug = false;
    DetectorBuilder _db;
    String _detectorName;
    double _minClusterEnergy = 3.5;

    private AIDA aida = AIDA.defaultInstance();
    // z from blueprints
    // x from fit (was -63? in 2016)
    private double[] H02Wire = {-68.0, 0., -(672.71 - 583.44) * 25.4};
    // initial guess for (x,y,z) of track origin
    // TODO get estimate for x of beam on wire. Was x=-63 in 2016
    double[] A0 = {0., 0., -2267.};
    // initial guess for the track direction
    double[] B0 = {0., 0., 1.};

    Map<String, SimTrackerHit> simTrackerHitByModule = new TreeMap<String, SimTrackerHit>();
    Map<String, SiTrackerHitStrip1D> stripTrackerHitByModule = new TreeMap<String, SiTrackerHitStrip1D>();
    Map<String, Hit> stripHitByModule = new TreeMap<String, Hit>();

//    Writer topEventsWriter = null;
//    Writer bottomEventsWriter = null;
    // field-off data does not hit the first two layers
    // top is missing last layer
    List<String> topHoleSensorNamesToFit = Arrays.asList(
            "module_L3t_halfmodule_axial_sensor0",
            "module_L3t_halfmodule_stereo_sensor0",
            "module_L4t_halfmodule_axial_sensor0",
            "module_L4t_halfmodule_stereo_sensor0",
            "module_L5t_halfmodule_axial_hole_sensor0",
            "module_L5t_halfmodule_stereo_hole_sensor0",
            "module_L6t_halfmodule_axial_hole_sensor0",
            "module_L6t_halfmodule_stereo_hole_sensor0");

    //runs 10099 and 10101 have all 10 good layers
    List<String> bottomHoleSensorNamesToFit = Arrays.asList(
            "module_L3b_halfmodule_stereo_sensor0",
            "module_L3b_halfmodule_axial_sensor0",
            "module_L4b_halfmodule_stereo_sensor0",
            "module_L4b_halfmodule_axial_sensor0",
            "module_L5b_halfmodule_stereo_hole_sensor0",
            "module_L5b_halfmodule_axial_hole_sensor0",
            "module_L6b_halfmodule_stereo_hole_sensor0",
            "module_L6b_halfmodule_axial_hole_sensor0",
            "module_L7b_halfmodule_stereo_hole_sensor0",
            "module_L7b_halfmodule_axial_hole_sensor0");

    // alignment stuff
    boolean alignit = true;
    // DetectorPlanes at the 2H02 wire to provide a beam-spot constraint.
    DetectorPlane xPlaneAtWire = null;
    DetectorPlane yPlaneAtWire = null;
    Hit beamAtWire = null;

    boolean _isMC = false;

    boolean beamConstrain = true;
    int nEventsToAlign = 250000;
    int bottomIter;
    int topIter;
    int NITER = 7;
    List<List<Hit>> bottomEventsToAlign = new ArrayList<>();
    List<DetectorPlane> bottomPlanes = null;//new ArrayList<DetectorPlane>();
    List<List<Hit>> topEventsToAlign = new ArrayList<>();
    List<DetectorPlane> topPlanes = null;//new ArrayList<DetectorPlane>();
    Map<String, DetectorPlane> alignedDetectorPlanesInFit = new LinkedHashMap<>();
    // container to hold the current best values of the detector planes
    // this will be update after every alignment.
    Map<String, DetectorPlane> planeMap = new HashMap<String, DetectorPlane>();

    // try some vertexing here...
    List<Track> topTracks = new ArrayList<Track>();
    List<Track> bottomTracks = new ArrayList<Track>();
    List<Track> topAndBottomTracks = new ArrayList<Track>();
    int nTracksToVertex = 50;
    double target_x = -68.0;
    double target_y = 0.;
    double target_z = -(672.71 - 583.44) * 25.4; // -2267.458

    double maxChisq = 200.;

    protected void detectorChanged(Detector detector) {
        if (_isMC) {
            target_x = -65.0;
            target_y = 0.;
            target_z = -2267;
        }
        _db = new DetectorBuilder(detector);
        planeMap.putAll(_db.planeMap());
        // set up the DetectorPlanes at the 2H02 wire
        double[] beamSpot = {target_x, target_y, target_z};  // start with this...
        double[] sigs = {0.050, 0.00}; // pick 50um for beam spot at wire
        xPlaneAtWire = new DetectorPlane(15, Matrix.identity(3, 3), beamSpot, sigs);
        yPlaneAtWire = new DetectorPlane(16, GEN_ROTMAT(PI / 2., 2), beamSpot, sigs);
        double[] u = {0., 0.};
        double[] wt = {1. / (sigs[0] * sigs[0]), 0., 0.};
        beamAtWire = new Hit(u, wt);
        _detectorName = detector.getDetectorName();

        //_db.drawDetector();
//        try {
//
//            topEventsWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("topEvents.txt")));
//            bottomEventsWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("bottomEvents.txt")));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        setupSensors(event);
        // Will use calorimeter clusters to define the road in which we search for hits.
        List<Cluster> clusters = null;
        if (event.hasCollection(Cluster.class, "EcalClustersCorr")) {
            clusters = event.get(Cluster.class, "EcalClustersCorr");
        } else if (event.hasCollection(Cluster.class, "EcalClusters")) {
            clusters = event.get(Cluster.class, "EcalClusters");
        }
        aida.histogram1D("number of clusters", 10, 0., 10.).fill(clusters.size());
//        Cluster c = null;
        for (Cluster cluster : clusters) {
            aida.histogram1D("All Cluster energy", 100, 0., 7.).fill(cluster.getEnergy());
            aida.histogram2D("All Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
            boolean isFiducial = TriggerModule.inFiducialRegion(cluster);
            String fid = isFiducial ? "fiducial" : "";
            aida.histogram1D("All Cluster " + fid + " energy", 100, 0., 7.).fill(cluster.getEnergy());
            aida.histogram2D("All Cluster " + fid + " x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
            if (cluster.getPosition()[1] > 0) {
                aida.histogram1D("All Cluster " + fid + " x top", 320, -270.0, 370.0).fill(cluster.getPosition()[0]);
            } else {
                aida.histogram1D("All Cluster " + fid + " x bottom", 320, -270.0, 370.0).fill(cluster.getPosition()[0]);
            }
        }
//        if (clusters.size() != 1) {
//            return;
//        }

        for (Cluster cluster : clusters) {
//            aida.histogram1D("All Cluster energy", 100, 0., 7.).fill(cluster.getEnergy());
            if (cluster.getEnergy() > _minClusterEnergy) {
                boolean isFiducial = TriggerModule.inFiducialRegion(cluster);
                String fid = isFiducial ? "fiducial" : "";
                Cluster c = cluster;
                String topOrBottom = c.getPosition()[1] > 0 ? "top " : "bottom ";
                boolean isTop = c.getPosition()[1] > 0;
                List<String> sensorNames = isTop ? topHoleSensorNamesToFit : bottomHoleSensorNamesToFit;
                aida.histogram2D("Cal " + fid + " Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                // calculate some slopes and intercepts...
                aida.histogram1D(topOrBottom + fid + "Cluster z", 10, 1443., 1445.).fill(cluster.getPosition()[2]);
                aida.histogram1D(topOrBottom + fid + "Cluster energy", 100, 0., 7.).fill(cluster.getEnergy());

//            }
//        }
//        if (c != null) {
                //OK, we have a good, high-energy cluster in tthe calorimeter...
                Point3D P0 = new Point3D(H02Wire[0], H02Wire[1], H02Wire[2]);
                double[] cPos = c.getPosition();
                Point3D P1 = new Point3D(cPos[0], cPos[1], cPos[2]);
                //let's get some SVT strip clusters...

//                // Get the list of fitted hits from the event
//                List<LCRelation> fittedHits = event.get(LCRelation.class,
//                        "SVTFittedRawTrackerHits");
//                // Map the fitted hits to their corresponding raw hits
//                Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
//                for (LCRelation fittedHit : fittedHits) {
//                    fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
//
//                }
                List<SiTrackerHitStrip1D> stripClusters = event.get(SiTrackerHitStrip1D.class,
                        "StripClusterer_SiTrackerHitStrip1D");
                int nStripHits = stripClusters.size();
                aida.histogram1D(topOrBottom + fid + "number of strip clusters", 100, 0., 200.).fill(nStripHits);
                // lets partition the strip clusters into each module
                Map<String, List<SiTrackerHitStrip1D>> hitsPerModuleMap = new HashMap<>();
                for (TrackerHit hit : stripClusters) {
                    List rthList = hit.getRawHits();
                    String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
                    if (sensorNames.contains(moduleName)) {
                        if (!hitsPerModuleMap.containsKey(moduleName)) {
                            hitsPerModuleMap.put(moduleName, new ArrayList<SiTrackerHitStrip1D>());
                            hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
                        } else {
                            hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
                        }
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
                // unbiased residual plots indicate a working resolution of ~40 to 60um
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
                    //TODO don't use the global position encoded in the SiTrackerHitStrip1D
                    //Instead, calculate u directly from the raw hits which make up the strip cluster.
                    //That would allow us to introduce new (read aligned) geometries in  the analysis.
                    //for now, always calculate the local coordinate from the original plane, not the updated, aligned one.
                    Hit h = makeHit(detectorPlanesInFit.get(s), pos, du);
                    hits.add(h);
                    // if we have an aligned plane, use it
                    //20200117 not sure what's going on here...
                    // let's simplify things.
//                    if (alignedDetectorPlanesInFit.containsKey(s)) {
//                        planes.add(alignedDetectorPlanesInFit.get(s));
//                    } else {
                    planes.add(detectorPlanesInFit.get(s));
//                    }
                }
                // require at least 8 hits for fit in top, 10 in bottom (for early runs), 9 in bottom in later runs due to missing layer 4
                int minHitsToFit = isTop ? 8 : 10;  // only 10101 has a working layer 4...
                if (hits.size() >= minHitsToFit) {
                    aida.histogram1D(topOrBottom + fid + " number of hits in fit", 20, 0., 20.).fill(hits.size());
                    // fit the track!
                    TrackFit fit = FitTracks.STR_LINFIT(planes, hits, A0, B0);
                    if (fit.chisq() / fit.ndf() < maxChisq) {
                        // quick check of track predicted impact points...
//                    List<double[]> impactPoints = fit.impactPoints();
//                    for (double[] pos : impactPoints) {
//                        System.out.println(Arrays.toString(pos));
//                    }
                        // calculate unbiased residuals here
                        refitTrack(planes, hits, A0, B0, isTop);
                        //opening angle analysis...
                        openingAngleAnalysis(planes, hits, A0, B0, isTop);
                        // Note that track position parameters x & y are reported at the input z.
                        double[] pars = fit.pars();
                        double[] cov = fit.cov();

                        aida.histogram1D(topOrBottom + fid + " x at z=-2267", 100, -100., 0.).fill(pars[0]);
                        aida.histogram1D(topOrBottom + fid + " y at z=-2267", 100, -20., 20.).fill(pars[1]);
                        aida.histogram1D(topOrBottom + fid + " dXdZ at z=-2267", 100, 0., 0.050).fill(pars[2]);
                        aida.histogram1D(topOrBottom + fid + " dYdZ at z=-2267", 100, -0.050, 0.050).fill(pars[3]);
                        aida.histogram1D(topOrBottom + fid + " track fit chiSquared per ndf", 100, 0., maxChisq).fill(fit.chisq() / fit.ndf());
                        double chisqProb = ChisqProb.gammp(fit.ndf(), fit.chisq());
                        aida.histogram1D(topOrBottom + fid + " track fit chiSquared probability", 100, 0., 1.).fill(chisqProb);
                        aida.histogram2D("Final Cal Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);
                        aida.histogram1D("Final Cluster energy", 100, 0., 7.).fill(c.getEnergy());

                        // let's check the impact point at the calorimeter...
                        double[] tAtEcal = fit.predict(cPos[2]);
                        aida.histogram2D(fid + " Track at Ecal x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(tAtEcal[0], tAtEcal[1]);
                        aida.histogram1D(topOrBottom + fid + " dx at ECal", 100, -20., 20.).fill(cPos[0] - tAtEcal[0]);
                        aida.histogram1D(topOrBottom + fid + " dy at Ecal", 100, -10., 10.).fill(cPos[1] - tAtEcal[1]);
                        if (beamConstrain) {
                            aida.tree().mkdirs("beam-constrained");
                            aida.tree().cd("beam-constrained");
                            planes.add(xPlaneAtWire);
                            hits.add(beamAtWire);
                            planes.add(yPlaneAtWire);
                            hits.add(beamAtWire);

                            aida.histogram1D(topOrBottom + fid + " number of hits in fit", 20, 0., 20.).fill(hits.size());
                            // fit the track!
                            TrackFit fitbc = FitTracks.STR_LINFIT(planes, hits, A0, B0);
                            // calculate unbiased residuals here
                            refitTrack(planes, hits, A0, B0, isTop);
                            // Note that track position parameters x & y are reported at the input z.
                            double[] parsbc = fitbc.pars();
                            double[] covbc = fitbc.cov();

                            aida.histogram1D(topOrBottom + fid + " x at z=-2267", 100, -100., 0.).fill(parsbc[0]);
                            aida.histogram1D(topOrBottom + fid + " y at z=-2267", 100, -20., 20.).fill(parsbc[1]);
                            aida.histogram1D(topOrBottom + fid + " dXdZ at z=-2267", 100, 0., 0.050).fill(parsbc[2]);
                            aida.histogram1D(topOrBottom + fid + " dYdZ at z=-2267", 100, -0.050, 0.050).fill(parsbc[3]);
                            aida.histogram1D(topOrBottom + fid + " track fit chiSquared per ndf", 100, 0., 100.).fill(fitbc.chisq() / fitbc.ndf());
                            double chisqProbbc = ChisqProb.gammp(fitbc.ndf(), fitbc.chisq());
                            aida.histogram1D(topOrBottom + fid + " track fit chiSquared probability", 100, 0., 1.).fill(chisqProbbc);

                            // let's check the impact point at the calorimeter...
                            double[] tAtEcalbc = fitbc.predict(cPos[2]);
                            aida.histogram2D(fid + " Track at Ecal x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(tAtEcalbc[0], tAtEcalbc[1]);
                            aida.histogram1D(topOrBottom + fid + " dx at ECal", 100, -20., 20.).fill(cPos[0] - tAtEcalbc[0]);
                            aida.histogram1D(topOrBottom + fid + " dy at Ecal", 100, -10., 10.).fill(cPos[1] - tAtEcalbc[1]);
                            aida.tree().cd("..");
                        }
                        // let's apply a few cuts here to enable us to skim events...
                        // beam spot x at wire is -63
                        if (abs(pars[0] + 63) < 20) {
                            // beam spot y at wire is 0
                            if (abs(pars[1]) < 15) {
                                // keep this event
                                skipEvent = false;
                                // good clean event let's use it for alignment
                                if (isTop) {
                                    if (topPlanes == null) {
                                        topPlanes = new ArrayList<DetectorPlane>();
                                        topPlanes.addAll(planes);
                                    }
                                    if (alignit) {
                                        topEventsToAlign.add(hits);
                                    }
//                                    topTracks.add(new Track(fit));
                                    topAndBottomTracks.add(new Track(fit));
                                } else {
                                    if (bottomPlanes == null) {
                                        bottomPlanes = new ArrayList<DetectorPlane>();
                                        bottomPlanes.addAll(planes);
                                    }
                                    if (alignit) {
                                        bottomEventsToAlign.add(hits);
                                    }
//                                    bottomTracks.add(new Track(fit));
                                    topAndBottomTracks.add(new Track(fit));
                                }
                            }
                        }
                    }
                }
            } // end of loop over clusters with energy greater that min cluster energy
        } // end of loop over clusters

        // let's try to align things here...
        if (alignit) {
            // try floating some of the sensors...
            // 0-2 are offsets in u,v,w, 3-5 are rotations about u,v,w
            // start with just offets in u (y) 
            int[] mask = {1, 0, 0, 0, 0, 0};
            if (bottomEventsToAlign.size() >= nEventsToAlign) {
                // start with just offets in u (y) in the outermost layers, viz, 0,1 & 8,9
                // Layer 3
                bottomPlanes.get(0).offset().setMask(mask);
                bottomPlanes.get(1).offset().setMask(mask);
                // Layer 7
                bottomPlanes.get(8).offset().setMask(mask);
                bottomPlanes.get(9).offset().setMask(mask);

                // if we are constraining the fit with the beamspot, can float all the layers but one.
                // fix layer 5 (sensors 9&10, indices 4&5) and see what happens...
                if (beamConstrain) {
                    //layer 4
                    bottomPlanes.get(2).offset().setMask(mask);
                    bottomPlanes.get(3).offset().setMask(mask);
                    //layer 6
                    bottomPlanes.get(6).offset().setMask(mask);
                    bottomPlanes.get(7).offset().setMask(mask);
                }

                System.out.println("Aligning the bottom SVT");
                alignit(bottomPlanes, bottomEventsToAlign, false, bottomIter);
                bottomIter++;
                // add the aligned planes to the list for further processing...
                for (DetectorPlane p : bottomPlanes) {
                    alignedDetectorPlanesInFit.put(p.name(), p);
                }
                // clear the list
                bottomEventsToAlign.clear();
            }
            if (topEventsToAlign.size() >= nEventsToAlign) {
                // start with just offets in u (y) in the outermost layers, viz, 0,1 & 8,9
                // Layer 3
                topPlanes.get(0).offset().setMask(mask);
                topPlanes.get(1).offset().setMask(mask);
                // Layer 6
                topPlanes.get(6).offset().setMask(mask);
                topPlanes.get(7).offset().setMask(mask);

                // if we are constraining the fit with the beamspot, can float all the layers but one.
                // fix layer 5 (sensors 9&10, indices 4&5) and see what happens...
                if (beamConstrain) {
                    //layer 4
                    topPlanes.get(2).offset().setMask(mask);
                    topPlanes.get(3).offset().setMask(mask);
                }

                System.out.println("Aligning the top SVT");
                alignit(topPlanes, topEventsToAlign, true, topIter);
                topIter++;
                // add the aligned planes to the list for further processing...
                for (DetectorPlane p : bottomPlanes) {
                    alignedDetectorPlanesInFit.put(p.name(), p);
                }
                // clear the list
                topEventsToAlign.clear();
            }
        }
        //let's try to vertex some tracks...
        // same side tracks aren't really very useful...
//        if (topTracks.size() >= nTracksToVertex) {
//            vertexEm(topTracks, "top");
//            topTracks.clear();
//        }
//        if (bottomTracks.size() >= nTracksToVertex) {
//            vertexEm(bottomTracks, "bottom");
//            bottomTracks.clear();
//        }
        if (topAndBottomTracks.size() >= nTracksToVertex) {
            vertexEm(topAndBottomTracks, "topAndBottom");
            topAndBottomTracks.clear();
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsWritten++;
        }
    }

    protected void endOfData() {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
//        try {
//            topEventsWriter.flush();
//            topEventsWriter.close();
//            bottomEventsWriter.flush();
//            bottomEventsWriter.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    public void vertexEm(List<Track> tracks, String torb) {
        aida.tree().mkdirs(torb + " vertex ");
        aida.tree().cd(torb + " vertex ");

        //first guess of vertex...
        double[] v0 = {-65., 0., -2267.};
        Vertex v = new Vertex();
        StraightLineVertexFitter.fitPrimaryVertex(tracks, v0, v);

        aida.histogram1D("vertex x", 100, -90., -45.).fill(v.x());
        aida.histogram1D("vertex y", 100, -3.5, 1.5).fill(v.y());
        aida.histogram1D("vertex z", 200, -2500., -2100).fill(v.z());
        aida.histogram2D("vertex x vs y", 100, -90., -45., 100, -3.5, 1.5).fill(v.x(), v.y());
        aida.histogram2D("vertex x vs z", 200, -2500., -2100, 100, -90., -45.).fill(v.z(), v.x());
        aida.histogram2D("vertex y vs z", 200, -2500., -2100, 100, -3.5, 1.5).fill(v.z(), v.y());

        aida.histogram1D("vertex number of Tracks", 200, 0., 200.).fill(v.ntracks());
        aida.histogram1D("vertex chisq per dof", 200, 0., 20000).fill(v.chisq() / v.ndf());

        aida.tree().cd("..");
    }

    /**
     * Align the detector represented by the list of DetectorPlanes using the
     * events in the list of list of Hits
     *
     * @param planes The Detector represented as a list of DetectorPlanes
     * @param events The events represented as a list of Hits
     */
    public void alignit(List<DetectorPlane> planes, List<List<Hit>> events, boolean isTop, int iter) {
        boolean debug = false;
        String path = isTop ? "top alignment " : "bottom alignment ";
        aida.tree().mkdirs(path + iter);
        aida.tree().cd(path + iter);

        //print out starting planes
        System.out.println("Before alignment...");
        for (DetectorPlane p : planes) {
            System.out.println(p);
        }

        // let's look at unbiased residuals before alignment...
        aida.tree().mkdirs("before");
        aida.tree().cd("before");
        for (List<Hit> hits : events) {
            // track fit 
            TrackFit fit = FitTracks.STR_LINFIT(planes, hits, A0, B0);
            double[] pars = fit.pars();
            double[] cov = fit.cov();
            aida.histogram1D(path + " x at z=-2267", 100, -100., 0.).fill(pars[0]);
            aida.histogram1D(path + " y at z=-2267", 100, -20., 20.).fill(pars[1]);
            aida.histogram1D(path + " dXdZ at z=-2267", 100, 0., 0.050).fill(pars[2]);
            aida.histogram1D(path + " dYdZ at z=-2267", 100, -0.050, 0.050).fill(pars[3]);
            aida.histogram1D(path + " track fit chiSquared per ndf", 100, 0., 100.).fill(fit.chisq() / fit.ndf());
            double chisqProb = ChisqProb.gammp(fit.ndf(), fit.chisq());
            aida.histogram1D(path + " track fit chiSquared probability", 100, 0., 1.).fill(chisqProb);
            // unbiased residuals
            refitTrack(planes, hits, A0, B0, isTop);
        }
        aida.tree().cd("..");

        int NN = planes.size();
        // the following controls which variables for which planes are allowed to float...
        // this should be set in the calling routine...
        int[] nprs = new int[NN];
        for (int i = 0; i < NN; ++i) {
            DetectorPlane p = planes.get(i);
            Offset o = p.offset();
            System.out.println(" PLANE " + i + " OFFSETS: " + Arrays.toString(o.offsets()) + " TILTS: " + Arrays.toString(o.angles()));
            int[] mask = o.mask();
            int doit = 0;
            for (int k = 0; k < 6; ++k) {
                doit = doit + mask[k];
            }
            nprs[i] = doit;
        }
        // a map of the Alignment objects keyed by detector plane
        Map<Integer, Alignment> alignmentMap = new HashMap<Integer, Alignment>();
        double[] PAR = new double[6]; // local offsets and tilt angles
        double[] COV = new double[21]; // covariance matrix

        for (int ITER = 0; ITER < NITER; ++ITER) { // iterate the alignment
            System.out.println("Iteration " + (ITER + 1));

            int NTIME = events.size();
            for (int i = 0; i < NTIME; ++i) { // loop over events
                double[] parin = new double[4];  // generated track parameters
                List<Hit> hits = events.get(i);
                TrackFit fit = FitTracks.STR_LINFIT(planes, hits, A0, B0);
                List<double[]> rx = fit.impactPoints();
                //find alignment parameters
                double[] B = new double[3];  // track direction at impact point.
                B[0] = fit.pars()[2];  // dx/dz
                B[1] = fit.pars()[3];  // dy/dz
                B[2] = 1.;             // z
                for (int j = 0; j < NN; ++j) { //loop over all the detector planes
                    int NPR = nprs[j];
                    if (NPR > 0) {  // found a detector plane which we want to align
                        if (debug) {
                            System.out.println("J " + (j + 1) + " NPR " + NPR);
                        }
                        DetectorPlane p = planes.get(j);
                        Offset o = p.offset();
                        int[] mask = o.mask();
                        Alignment a = null;
                        if (alignmentMap.containsKey(j)) {
                            a = alignmentMap.get(j);
                        } else {
                            //best guess for plane rotation and offset is ideal position
                            //this will be updated with each iteration
                            a = new Alignment(j, mask, p.rotArray(), p.r0());
                            alignmentMap.put(j, a);
                        }
                        // need to calculate track impact point RR at this plane
                        Hit hit = hits.get(j);
                        double[] QM = hit.uvm();
                        double[] W = hit.wt();
                        double[] RX = rx.get(j);
                        //System.out.println((i+1)+" "+(j+1)+" "+Arrays.toString(RX));
                        a.accumulate(RX, B, QM, W);
                        double[] ROT = new double[9];
                        double[] R0 = new double[3];
                        if (i == NTIME - 1) {
                            a.solve(PAR, COV, ROT, R0);
                            // this updates ROT and R0, which needs to be fed to the accumulator.
//                            System.out.println("Iteration " + (ITER+1));
//                            System.out.println("Update Plane "+j);
//                            System.out.println("R0 " + Arrays.toString(R0));
//                            System.out.println("ROT " + Arrays.toString(ROT));
                            p.setUpdatedPosition(R0);
                            p.setUpdatedRotation(ROT);
                        }
                    }
                } //loop over planes
            } // loop over events
        }// loop over iterations  

        // in principle we are now aligned...
        // print out ending planes
        System.out.println("After alignment...");
        for (DetectorPlane p : planes) {
            System.out.println(p);
            planeMap.replace(p.name(), p);
        }
        // let's archive this...
        // Note that we always write out the full detector...
        System.out.println("archiving " + path + "iteration " + iter);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(_detectorName + "_" + myDate() + "_" + (isTop ? "topAlignment" : "bottomAlignment_") + nEventsToAlign + "EventsIteration_" + iter + ".txt");
            Writer w = new BufferedWriter(new OutputStreamWriter(fos));
            for (DetectorBuilder.Tracker t : DetectorBuilder.Tracker.values()) {
                String[] sensorNames = _db.getTrackerSensorNames(t.trackerName());
                for (String sensor : sensorNames) {
                    DetectorPlane p = planeMap.get(sensor);
                    StringBuffer sb = new StringBuffer(p.id() + " " + p.name() + " ");
                    sb.append("o ( " + p.origin().x() + " " + p.origin().y() + " " + p.origin().z() + " ) ");
                    sb.append("u ( " + p.u().x() + " " + p.u().y() + " " + p.u().z() + " ) ");
                    sb.append("v ( " + p.v().x() + " " + p.v().y() + " " + p.v().z() + " ) ");
                    sb.append("w ( " + p.normal().x() + " " + p.normal().y() + " " + p.normal().z() + " ) ");
                    sb.append("a ( " + p.rotationAngles()[0] + " " + p.rotationAngles()[1] + " " + p.rotationAngles()[2] + " ) ");
                    sb.append(p.width() + " " + p.height() + " " + p.zmin() + " " + p.zmax() + " \n");
                    w.write(sb.toString());
                }
            }
            w.flush();
            w.close();

        } catch (Exception ex) {
            Logger.getLogger(StraightTrackAlignmentDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        // see if the unbiased residuals improve...
        aida.tree().mkdirs("after");
        aida.tree().cd("after");
        for (List<Hit> hits : events) {
            // track fit 
            TrackFit fit = FitTracks.STR_LINFIT(planes, hits, A0, B0);
            double[] pars = fit.pars();
            double[] cov = fit.cov();
            aida.histogram1D(path + " x at z=-2267", 100, -100., 0.).fill(pars[0]);
            aida.histogram1D(path + " y at z=-2267", 100, -20., 20.).fill(pars[1]);
            aida.histogram1D(path + " dXdZ at z=-2267", 100, 0., 0.050).fill(pars[2]);
            aida.histogram1D(path + " dYdZ at z=-2267", 100, -0.050, 0.050).fill(pars[3]);
            aida.histogram1D(path + " track fit chiSquared per ndf", 100, 0., 100.).fill(fit.chisq() / fit.ndf());
            double chisqProb = ChisqProb.gammp(fit.ndf(), fit.chisq());
            aida.histogram1D(path + " track fit chiSquared probability", 100, 0., 1.).fill(chisqProb);
            // unbiased residuals
            refitTrack(planes, hits, A0, B0, isTop);
            if (beamConstrain) {
                // refit the track without the 2 beam constraints
                TrackFit unconstrainedFit = FitTracks.STR_LINFIT(planes.subList(0, planes.size() - 3), hits.subList(0, hits.size() - 3), A0, B0);
                pars = unconstrainedFit.pars();
                aida.histogram1D(path + " unconstrained x at z=-2267", 100, -100., 0.).fill(pars[0]);
                aida.histogram1D(path + " unconstrained y at z=-2267", 100, -20., 20.).fill(pars[1]);
                aida.histogram1D(path + " unconstrained dXdZ at z=-2267", 100, 0., 0.050).fill(pars[2]);
                aida.histogram1D(path + " unconstrained dYdZ at z=-2267", 100, -0.050, 0.050).fill(pars[3]);
                aida.histogram1D(path + " unconstrained track fit chiSquared per ndf", 100, 0., 100.).fill(unconstrainedFit.chisq() / unconstrainedFit.ndf());
                chisqProb = ChisqProb.gammp(unconstrainedFit.ndf(), unconstrainedFit.chisq());
                aida.histogram1D(path + " unconstrained track fit chiSquared probability", 100, 0., 1.).fill(chisqProb);
            }
        }
        aida.tree().cd("..");

        aida.tree().cd("..");
    }

    public Hit makeHit(DetectorPlane p, SiTrackerHitStrip1D stripCluster) {
        List<RawTrackerHit> hits = stripCluster.getRawHits();
        for (RawTrackerHit hit : hits) {

        }

        return null;
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

        if (event.hasCollection(RawTrackerHit.class,
                "SVTRawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class,
                    "SVTRawTrackerHits");

        }
        if (event.hasCollection(RawTrackerHit.class,
                "RawTrackerHitMaker_RawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class,
                    "RawTrackerHitMaker_RawTrackerHits");
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

    /**
     * Refit a track to obtain the unbiased hit residuals one plane at a time
     *
     * @param planes The planes used in the original fit
     * @param hits The hits on the track
     * @param A0 Initial guess for (x,y,z) of track
     * @param B0 Initial guess for the track direction
     * @param isTop true if track is in the top SVT
     */
    public void refitTrack(List<DetectorPlane> planes, List<Hit> hits, double[] A0, double[] B0, boolean isTop) {
        String topOrBottom = isTop ? "top " : "bottom ";
        String path = "Track Refit ";
        aida.tree().mkdirs(path);
        aida.tree().cd(path);
        // refit this track dropping one different hit each time...
        int nHitsOnTrack = planes.size();
        // loop over all the hits
        for (int i = 0; i < nHitsOnTrack; ++i) {
            List<DetectorPlane> newPlanes = new ArrayList<>();
            List<Hit> newHits = new ArrayList<>();
            // remove each hit and refit track without this hit
            for (int j = 0; j < nHitsOnTrack; ++j) {
                if (j != i) {
                    newPlanes.add(planes.get(j));
                    newHits.add(hits.get(j));
                }
            }
            // refit without the one hit
            TrackFit fit = FitTracks.STR_LINFIT(newPlanes, newHits, A0, B0);
            // get the predicted impact point for the missing hit...

            // get the hit...
            DetectorPlane missingPlane = planes.get(i);
            Hit missingHit = hits.get(i);
            // get the unbiased residual for the missing hit
            double[] resid = unbiasedResidual(fit, missingPlane, missingHit, A0, B0);
            aida.histogram1D(topOrBottom + "unbiased residual " + missingPlane.id(), 100, -1.0, 1.0).fill(resid[1]);
//            aida.histogram2D(topOrBottom + "unbiased x vs residual " + missingPlane.id(), 300, -200, 100, 100, -1.0, 1.0).fill(resid[0], resid[1]);
//            aida.histogram1D(topOrBottom + "unbiased x " + missingPlane.id(), 300, -200, 100).fill(resid[0]);
        }
        aida.tree().cd("..");
    }

    public void openingAngleAnalysis(List<DetectorPlane> planes, List<Hit> hits, double[] A0, double[] B0, boolean isTop) {
        String topOrBottom = isTop ? "top " : "bottom ";
        String path = "Opening Angle ";
        aida.tree().mkdirs(path);
        aida.tree().cd(path);
        // refit front and back parts of this track to determine SVT opening angle.
        int nHitsOnTrack = planes.size();
        List<DetectorPlane> frontPlanes = new ArrayList<DetectorPlane>();
        List<Hit> frontHits = new ArrayList<Hit>();
        List<DetectorPlane> backPlanes = new ArrayList<DetectorPlane>();
        List<Hit> backHits = new ArrayList<Hit>();
        // loop over all the hits
        aida.histogram1D(topOrBottom + " number of hits in fit", 20, 0., 20.).fill(nHitsOnTrack);
        for (int i = 0; i < nHitsOnTrack; ++i) {
            int id = planes.get(i).id();
            //System.out.println("opening angle refit i : " + i + " plane id " + planes.get(i).id());
            aida.histogram1D(topOrBottom + " hit id", 20, 0., 20.).fill(planes.get(i).id());
            if (id < 9) {
                frontPlanes.add(planes.get(i));
                frontHits.add(hits.get(i));
            } else {
                backPlanes.add(planes.get(i));
                backHits.add(hits.get(i));
            }
        }
        // fit the full track...
        TrackFit fullFit = FitTracks.STR_LINFIT(planes, hits, A0, B0);
        //System.out.println(fullFit + " " + planes.size() + " " + hits.size());
        
        // define the fit plane halfway between
        double zPivot = 414.0;
        double[] APivot = {0., 0., zPivot}; // midway between layer 3 and 4
        TrackFit fitFront = FitTracks.STR_LINFIT(frontPlanes, frontHits, APivot, B0);
        TrackFit fitBack = FitTracks.STR_LINFIT(backPlanes, backHits, APivot, B0);
        //System.out.println(fitFront + " " + frontPlanes.size() + " " + frontHits.size());
        double[] parsFront = fitFront.pars();
        double[] parsBack = fitBack.pars();
        aida.histogram2D(" X vs Y front at z = " + zPivot, 200, -60., 16., 200, -50., 50.).fill(parsFront[0], parsFront[1]);
        aida.histogram2D(" X vs Y back at z = " + zPivot, 200, -60., 16., 200, -50., 50.).fill(parsBack[0], parsBack[1]);
        aida.histogram1D(topOrBottom + " X back-front at z = " + zPivot, 100, -2.5, 2.5).fill(parsBack[0] - parsFront[0]);
        aida.histogram2D(topOrBottom + " Y back-front vs X at z = " + zPivot, 200, -40., 15., 100, -0.5, 0.5).fill(parsFront[0], parsBack[1] - parsFront[1]);
        aida.histogram1D(topOrBottom + " Y back-front at z = " + zPivot, 100, -0.5, 0.5).fill(parsBack[1] - parsFront[1]);
        aida.profile1D(topOrBottom + " Y back-front vs X at z = " + zPivot + " profile", 100, -40., 10.).fill(parsFront[0], parsBack[1] - parsFront[1]);
        aida.profile1D(topOrBottom + " Y back-front vs X at z = " + zPivot + " profile tight", 100, -37., -15.).fill(parsFront[0], parsBack[1] - parsFront[1]);
        aida.histogram1D(topOrBottom + " dXdZ back-front at z = " + zPivot, 100, -0.01, 0.01).fill(parsBack[2] - parsFront[2]);
        aida.histogram1D(topOrBottom + " dYdZ back-front at z = " + zPivot, 100, -0.005, 0.005).fill(parsBack[3] - parsFront[3]);

        aida.tree().cd("..");
    }

    /**
     * Calculate the unbiased residual for a hit not included in the fit. Note
     * that because our axial/stereo pairs move in concert it might be necessary
     * to remove two hits from the fit...
     *
     * @param fit The track fit excluding a hit
     * @param dp The DetectorPlane for the excluded hit
     * @param h The excluded hit
     * @param A0 The TrackFit position
     * @param B0 The TrackFit direction
     * @return
     */
    public double[] unbiasedResidual(TrackFit fit, DetectorPlane dp, Hit h, double[] A0, double[] B0) {
        double resid = 9999.;
        boolean debug = false;
        Matrix rot = dp.rot();
        Matrix[] uvwg = new Matrix[3];
        double[][] UVW = {{1., 0., 0.}, {0., 1., 0.}, {0., 0., 1.}};
        double[] BUVW = new double[3];
        double[] PAR = fit.pars();
        double[] A = {PAR[0], PAR[1], A0[2]};
        double[] B = {PAR[2], PAR[3], B0[2]};
        Matrix b = new Matrix(B, 1);
        for (int j = 0; j < 3; ++j) {
//                    if (debug) {
//                        System.out.println("  CALLING VMATR");
//                    }
            Matrix uvw = new Matrix(UVW[j], 3);
            if (debug) {
                System.out.println("  UVW(" + (j + 1) + ") " + uvw.get(0, 0) + " " + uvw.get(1, 0) + " " + uvw.get(2, 0));
            }
            uvwg[j] = rot.transpose().times(uvw);
            if (debug) {
                System.out.println("UVWG(" + (j + 1) + ") " + uvwg[j].get(0, 0) + " " + uvwg[j].get(1, 0) + " " + uvwg[j].get(2, 0) + " ");
            }
//                    System.out.println("j "+j);
//                    System.out.println("b");
//                    b.print(6,4);
            BUVW[j] = b.times(uvwg[j]).get(0, 0);
        }
        if (debug) {
            System.out.println("   BUVW " + BUVW[0] + " " + BUVW[1] + " " + BUVW[2]);
        }
        ImpactPoint ip = FitTracks.GET_IMPACT(A, B, rot, dp.r0(), uvwg[2], BUVW[2]);
//        System.out.println(ip);
//        System.out.println(h);
        return new double[]{ip.q()[1], h.uvm()[0] - ip.q()[0]};
    }

    public void setNumberOfTracksToVertex(int i) {
        nTracksToVertex = i;
    }

    public void setIsMC(boolean b) {
        _isMC = b;
    }

    public void setTargetX(double d) {
        target_x = d;
    }

    public void setTargetY(double d) {
        target_y = d;
    }

    public void setTargetZ(double d) {
        target_z = d;
    }

    public void setMaxTrackChisquared(double d) {
        maxChisq = d;
    }

    public void setNumberOfTracksPerAlignment(int i) {
        nEventsToAlign = i;
    }

    public void setNumberOfAlignmentIterations(int i) {
        NITER = i;
    }

    public void setUseBeamConstraintInTrackFit(boolean b) {
        beamConstrain = b;
    }

    public void setDoAlignment(boolean b) {
        alignit = b;
    }

    static private String myDate() {
        Calendar cal = new GregorianCalendar();
        Date date = new Date();
        cal.setTime(date);
        DecimalFormat formatter = new DecimalFormat("00");
        String day = formatter.format(cal.get(Calendar.DAY_OF_MONTH));
        String month = formatter.format(cal.get(Calendar.MONTH) + 1);
        return cal.get(Calendar.YEAR) + month + day;
    }
}
