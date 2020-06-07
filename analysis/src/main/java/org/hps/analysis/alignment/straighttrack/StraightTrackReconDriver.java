package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import hep.physics.vec.Hep3Vector;
import static java.lang.Math.abs;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class StraightTrackReconDriver extends Driver {

    DetectorBuilder _db;
    boolean _useAlignedDetector = true;
    double _minClusterEnergy = 3.5;
    double maxChisq = 200.;

    private double[] H02Wire = {-68.0, 0., -(672.71 - 583.44) * 25.4};
    // initial guess for (x,y,z) of track origin
    // TODO get estimate for x of beam on wire. Was x=-63 in 2016
    double[] A0 = {0., 0., -2267.};
    // initial guess for the track direction
    double[] B0 = {0., 0., 1.};

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

    private AIDA aida = AIDA.defaultInstance();

    protected void detectorChanged(Detector detector) {
        if (_useAlignedDetector) {
            Path resourcePath = null;
//            String resourceName = "org/hps/analysis/alignment/HPS-PhysicsRun2019-v1-4pt5_20200205_topAlignment250000EventsIteration_9.txt";
            String resourceName = "org/hps/analysis/alignment/HPS-PhysicsRun2019-v1-4pt5_20200531_topAlignment50000EventsIteration_14.txt";
            
            try {
                resourcePath = Paths.get(getClass().getClassLoader().getResource(resourceName).toURI());
            } catch (URISyntaxException ex) {
                Logger.getLogger(StraightTrackAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(resourcePath);
            _db = new DetectorBuilder(resourcePath);
            System.out.println("Using aligned detector " + resourceName);

        } else {
            System.out.println("using default detector " + detector.getName());
            _db = new DetectorBuilder(detector);
        }
        System.out.println("Using Detector: \n"+_db);
    }

    protected void process(EventHeader event) {
        setupSensors(event);
        List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
        for (Cluster cluster : clusters) {
            if (cluster.getEnergy() > _minClusterEnergy) {
                boolean isFiducial = TriggerModule.inFiducialRegion(cluster);
                String fid = isFiducial ? "fiducial" : "";
                Cluster c = cluster;
                String topOrBottom = c.getPosition()[1] > 0 ? "top " : "bottom ";
                boolean isTop = c.getPosition()[1] > 0;
                List<String> sensorNames = isTop ? topHoleSensorNamesToFit : bottomHoleSensorNamesToFit;

                //OK, we have a good, high-energy cluster in tthe calorimeter...
                Point3D P0 = new Point3D(H02Wire[0], H02Wire[1], H02Wire[2]);
                double[] cPos = c.getPosition();
                Point3D P1 = new Point3D(cPos[0], cPos[1], cPos[2]);
                //let's get some SVT strip clusters... 
                List<SiTrackerHitStrip1D> stripClusters = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
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
//                    Hit h = makeHit(detectorPlanesInFit.get(s), pos, du);
                    int mySign = 1;
                    // I flip my coordinate system to have +ive u always up, i.e. aligned with +ive y.
                    if (detectorPlanesInFit.get(s).name().contains("t_halfmodule_stereo")) {
                        mySign = -1;
                    }
                    if (detectorPlanesInFit.get(s).name().contains("b_halfmodule_axial")) {
                        mySign = -1;
                    }

                    double[] u = {mySign * stripHit.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR).getPositionAsVector().x(), 0.};
                    double[] wt = {1 / (du * du), 0., 0.};
                    Hit myHit = new Hit(u, wt);

//                    System.out.println(detectorPlanesInFit.get(s).name() + " myHit " + myHit);
                    hits.add(myHit);
                    //cng
//                    SiTrackerHitStrip1D local = stripHit.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
//                    SiTrackerHitStrip1D global = stripHit.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);
//                    System.out.println("original hit " + stripHit.getPositionAsVector());
//                    System.out.println("global hit " + global.getPositionAsVector());
//                    System.out.println("local hit " + local.getPositionAsVector());
//                    System.out.println("local u " + local.getPositionAsVector().x() + " my hit u " + myHit.uvm()[0]);
//                    double sign = signum(myHit.uvm()[0] * local.getPositionAsVector().x());
//                    System.out.println(stripHit.getSensor().getName() + " " + sign);
//                    aida.histogram1D(stripHit.getSensor().getName() + "stripHit u - my u", 100, -0.001, 0.001).fill(sign * local.getPositionAsVector().x() - myHit.uvm()[0]);
                    //cng
                    // if we have an aligned plane, use it
                    //20200117 not sure what's going on here...
                    // let's simplify things.
//                    if (alignedDetectorPlanesInFit.containsKey(s)) {
//                        planes.add(alignedDetectorPlanesInFit.get(s));
//                    } else {
                    planes.add(detectorPlanesInFit.get(s));
//                    }
                }
                System.out.println(hits.size()+ "hits to fit");
                // require at least 8 hits for fit in top, 10 in bottom (for early runs), 9 in bottom in later runs due to missing layer 4
                int minHitsToFit = isTop ? 8 : 10;  // only 10101 has a working layer 4...
//                if(!isTop && event.getRunNumber()>10101) minHitsToFit=8
                if (hits.size() >= minHitsToFit) {
                    aida.histogram1D(topOrBottom + fid + " number of hits in fit", 20, 0., 20.).fill(hits.size());
                    // fit the track!
                    TrackFit fit = FitTracks.STR_LINFIT(planes, hits, A0, B0);
                    System.out.println("chisq "+fit.chisq() +" ndf "+ fit.ndf() + " chisq/ndf "+(fit.chisq() / fit.ndf()));
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
                    }
                }
            }
        }
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
//            System.out.println("hit wt " + Arrays.toString(hits.get(i).wt()) + " " + sqrt(1 / hits.get(i).wt()[0]));
//            double du = sqrt(1 / hits.get(i).wt()[0]);
//            if (du == 0.006 || du == 0.04) {
////                System.out.println(" allSingleHits = false");
//                allSingleHits = false;
//            }
//            if (du == 0.012 || du == 0.04) {
////                System.out.println(" allDoubleHits = false");
//                allDoubleHits = false;
//            }
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
//        double zPivot = 414.0;
//20200524 c_support_kin_L13b origin in trackingVolume : [     -117.33,     -67.996,      417.79]
        double zPivot = 417.79;
        double[] APivot = {0., 0., zPivot}; // midway between layer 3 and 4
        TrackFit fitFront = FitTracks.STR_LINFIT(frontPlanes, frontHits, APivot, B0);
        TrackFit fitBack = FitTracks.STR_LINFIT(backPlanes, backHits, APivot, B0);
        //System.out.println(fitFront + " " + frontPlanes.size() + " " + frontHits.size());
        double[] parsFront = fitFront.pars();
        double[] parsBack = fitBack.pars();
        aida.histogram2D(" X vs Y front at z = " + zPivot, 200, -60., 16., 200, -50., 50.).fill(parsFront[0], parsFront[1]);
        aida.histogram2D(" X vs Y back at z = " + zPivot, 200, -60., 16., 200, -50., 50.).fill(parsBack[0], parsBack[1]);
        aida.histogram1D(topOrBottom + " dX back-front at z = " + zPivot, 100, -2.5, 2.5).fill(parsBack[0] - parsFront[0]);
        aida.histogram2D(topOrBottom + " dY back-front vs X at z = " + zPivot, 200, -40., 15., 100, -0.5, 0.5).fill(parsFront[0], parsBack[1] - parsFront[1]);
        aida.histogram1D(topOrBottom + " dY back-front at z = " + zPivot, 100, -0.5, 0.5).fill(parsBack[1] - parsFront[1]);
        aida.profile1D(topOrBottom + " dY back-front vs X at z = " + zPivot + " profile", 100, -40., 10.).fill(parsFront[0], parsBack[1] - parsFront[1]);
        aida.profile1D(topOrBottom + " dY back-front vs X at z = " + zPivot + " profile tight", 100, -37., -15.).fill(parsFront[0], parsBack[1] - parsFront[1]);
        aida.histogram1D(topOrBottom + " dXdZ back-front at z = " + zPivot, 100, -0.01, 0.01).fill(parsBack[2] - parsFront[2]);
        aida.histogram1D(topOrBottom + " dYdZ back-front at z = " + zPivot, 100, -0.005, 0.005).fill(parsBack[3] - parsFront[3]);

        // project line fits to z of target
        aida.tree().cd("..");
    }
}
