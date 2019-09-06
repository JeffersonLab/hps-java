package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static org.hps.analysis.alignment.straighttrack.FitTracks.GET_IMPACT;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;

/**
 *
 * @author ngraf
 */
public class StraightTrackGeometryDriver extends Driver {

    boolean debug = true;
    DetectorBuilder _db;

    Map<String, SimTrackerHit> simTrackerHitByModule = new TreeMap<String, SimTrackerHit>();
    Map<String, SiTrackerHitStrip1D> stripTrackerHitByModule = new TreeMap<String, SiTrackerHitStrip1D>();
    Map<String, Hit> stripHitByModule = new TreeMap<String, Hit>();

    protected void detectorChanged(Detector detector) {
        _db = new DetectorBuilder(detector);
        _db.drawDetector();
    }

    protected void process(EventHeader event) {
        setupSensors(event);

//        List<MCParticle> mcparts = event.getMCParticles();
//        MCParticle mcp = mcparts.get(0);
//        Hep3Vector o = mcp.getOrigin();
//        Hep3Vector p = mcp.getMomentum();
//        System.out.println("mcp origin " + o);
//        System.out.println("mcp momentum " + p);
//        double pxpz = p.x() / p.z();
//        double pypz = p.y() / p.z();
//        System.out.println("pxpz " + pxpz + " pypz " + pypz);
//
//        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, "TrackerHits");
//        for (SimTrackerHit hit : simHits) {
//            // get the hit's position in global coordinates..
//            Hep3Vector globalPos = hit.getPositionVec();
//            String sensorName = hit.getDetectorElement().getName();
//            //           System.out.println(sensorName + " " + globalPos);
//            simTrackerHitByModule.put(sensorName, hit);
//        }
        List<TrackerHit> stripClusters = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");
        List<DetectorPlane> planes = new ArrayList<DetectorPlane>();
        List<Hit> hits = new ArrayList<Hit>();
        for (TrackerHit hit : stripClusters) {
            SiTrackerHitStrip1D stripHit = new SiTrackerHitStrip1D(hit);
            List rthList = hit.getRawHits();
            RawTrackerHit rth = (RawTrackerHit) rthList.get(0);
            String moduleName = rth.getDetectorElement().getName();
            double[] pos = stripHit.getPosition();
            int layer = TrackUtils.getLayer(hit);
            DetectorPlane dp = _db.planeMap.get(moduleName);
//            System.out.println("layer " + layer);
//            System.out.println(" " + dp);
//            System.out.println(" " + Arrays.toString(pos));
//            System.out.println(moduleName + " " + Arrays.toString(pos));
            stripTrackerHitByModule.put(moduleName, stripHit);
            Hit h = makeHit(dp, pos);
            stripHitByModule.put(moduleName, h);
//            System.out.println(h);
            planes.add(dp);
//            hits.add(h);
        }

//        Set<String> keys = simTrackerHitByModule.keySet();
//        for (String key : keys) {
//            System.out.println(key);
//            System.out.println(Arrays.toString(simTrackerHitByModule.get(key).getPosition()));
//            System.out.println(Arrays.toString(stripTrackerHitByModule.get(key).getPosition()));
//            System.out.println(Arrays.toString(stripHitByModule.get(key).uvm()));
//        }
//
//        double[] A0 = o.v();//{-63., 0., -2338.}; // initial guess for (x,y,z) of track
//        double[] B0 = {0., 0., 1.}; // initial guess for the track direction
//        // test some things here  
//        double[] par = {A0[0], A0[1], pxpz, pypz};
//        List<Hit> myHits = GENER_EVT(planes, par);
//        for (Hit h : myHits) {
//            System.out.println(h);
//        }
    }

    private List<Hit> GENER_EVT(List<DetectorPlane> dets, double[] par) {
        List<Hit> hitlist = new ArrayList<Hit>();
        int N = dets.size();
        double[] W = {0., 0., 1.};
        //initial guess is along zhat
        double[] A = {par[0], par[1], -2338.};
        double[] B = {par[2], par[3], 1.0000000};
        Matrix w = new Matrix(W, 3);
        Matrix a = new Matrix(A, 3);
        Matrix b = new Matrix(B, 3);
        for (int i = 0; i < N; ++i) {
            Hit hit;
            DetectorPlane dp = dets.get(i);
            double[] sigs = dp.sigs();
            Matrix ROT = dp.rot();
            double[] R0 = dp.r0();
            if (debug) {
                System.out.println("CALLING VMATR(W, ROT(1," + (i + 1) + "), WG, 3, 3) ");
            }
            Matrix wg = ROT.times(w);
            if (debug) {
                System.out.println(" WG " + wg.get(0, 0) + " " + wg.get(1, 0) + " " + wg.get(2, 0));
            }
            Matrix bwg = b.transpose().times(wg);
            if (debug) {
                System.out.println(" BWG " + bwg.get(0, 0));
            }
            ImpactPoint ip = GET_IMPACT(A, B, ROT, R0, wg, bwg.get(0, 0));
            System.out.println("impact " + dp.id() + " " + Arrays.toString(ip.r()));
            //TODO will have to modify when I restrict code to 1D strips
            double[] u = new double[2];  // 2dim for u and v measurement 
            double[] wt = new double[3]; // lower diag cov matrix
            for (int j = 0; j < 2; ++j) // do fluctuations
            {
                double coor = ip.q()[j]; // local u coordinate of impact point
//                double smear = sigs[j] * ran.nextGaussian(); // fluctuation
//                coor += smear;
                u[j] = coor;
                int k = j * (j + 1); // should be 0 and 2
                wt[1] = 0.; // explicitly list here in case we ever have cov between u and v
                if (sigs[j] > 0.) {
                    wt[k] = 1 / (sigs[j] * sigs[j]);
                }
                if (debug) {
                    System.out.println("MEASUREMENT Q(" + (j + 1) + ") " + ip.q()[j]);
                }
                if (debug) {
                    System.out.println("SMEARED MEASUREMENT " + (j + 1) + ") " + coor);
                }
            }
            hit = new Hit(u, wt);
//            if (use_fixed_hits) {
//                hit = hitlist.get(i);
//            }
            if (debug) {
                System.out.println("SMEARED UVM(JJ," + (i + 1) + ") " + hit.uvm()[0] + " " + hit.uvm()[1]);
            }
            if (debug) {
                System.out.println("WT(JJ, " + (i + 1) + ") " + hit.wt()[0] + " " + hit.wt()[1] + " " + hit.wt()[2]);
            }
            hitlist.add(hit);
        }
        return hitlist;
    }

    /**
     * Given a DetectorPlane and a global position, return a hit in local
     * coordinates
     *
     * @param p
     * @param pos
     * @return
     */
    public Hit makeHit(DetectorPlane p, double[] pos) {
        Matrix R = p.rot();
        double[] r0 = p.r0();
        Matrix diff = new Matrix(3, 1);
        for (int i = 0; i < 3; ++i) {
            diff.set(i, 0, pos[i] - r0[i]);
        }
//        diff.print(6, 4);
        System.out.println("pos " + Arrays.toString(pos));
        System.out.println("r0  " + Arrays.toString(r0));
        Matrix local = R.times(diff);
//        local.print(6, 4);
        double[] u = new double[2];  // 2dim for u and v measurement 
        double[] wt = new double[3]; // lower diag cov matrix
        double[] sigs = p.sigs();
        u[0] = local.get(0, 0);
        wt[0] = 1 / (sigs[0] * sigs[0]);

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
