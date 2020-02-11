package org.hps.analysis.alignment;

import Jama.Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.analysis.alignment.straighttrack.DetectorBuilder;
import org.hps.analysis.alignment.straighttrack.DetectorPlane;
import org.hps.analysis.alignment.straighttrack.Hit;
import org.hps.recon.tracking.DefaultSiliconResolutionModel;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.SiliconResolutionModel;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.detector.tracker.silicon.ThinSiStrips;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class SiTrackerHitStrip1DAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private boolean debug = false;
    //Collection Strings
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
    Map<String, Integer> moduleFlipMap = new HashMap<>();

    DetectorBuilder _alignedDetector;
    DetectorBuilder _defaultDetector;

    protected void detectorChanged(Detector detector) {
        Path path = Paths.get("D:/work/hps/analysis/physrun2019/alignment/fieldOff/20200203/HPS-PhysicsRun2019-v1-4pt5_20200205_topAlignment250000EventsIteration_9.txt");
        _alignedDetector = new DetectorBuilder(path);
        _defaultDetector = new DetectorBuilder(detector);

        moduleFlipMap.put("module_L1b_halfmodule_axial_sensor0", -1);
        moduleFlipMap.put("module_L1b_halfmodule_stereo_sensor0", 1);
        moduleFlipMap.put("module_L1t_halfmodule_axial_sensor0", 1);
        moduleFlipMap.put("module_L1t_halfmodule_stereo_sensor0", -1);
        moduleFlipMap.put("module_L2b_halfmodule_axial_sensor0", -1);
        moduleFlipMap.put("module_L2b_halfmodule_stereo_sensor0", 1);
        moduleFlipMap.put("module_L2t_halfmodule_axial_sensor0", 1);
        moduleFlipMap.put("module_L2t_halfmodule_stereo_sensor0", -1);
        moduleFlipMap.put("module_L3b_halfmodule_axial_sensor0", -1);
        moduleFlipMap.put("module_L3b_halfmodule_stereo_sensor0", 1);
        moduleFlipMap.put("module_L3t_halfmodule_axial_sensor0", 1);
        moduleFlipMap.put("module_L3t_halfmodule_stereo_sensor0", -1);
        moduleFlipMap.put("module_L4b_halfmodule_axial_sensor0", -1);
        moduleFlipMap.put("module_L4b_halfmodule_stereo_sensor0", 1);
        moduleFlipMap.put("module_L4t_halfmodule_axial_sensor0", 1);
        moduleFlipMap.put("module_L4t_halfmodule_stereo_sensor0", -1);
        moduleFlipMap.put("module_L5b_halfmodule_axial_hole_sensor0", -1);
        moduleFlipMap.put("module_L5b_halfmodule_axial_slot_sensor0", -1); //?
        moduleFlipMap.put("module_L5b_halfmodule_stereo_hole_sensor0", 1);
        moduleFlipMap.put("module_L5b_halfmodule_stereo_slot_sensor0", 1);
        moduleFlipMap.put("module_L5t_halfmodule_axial_hole_sensor0", 1);
        moduleFlipMap.put("module_L5t_halfmodule_axial_slot_sensor0", 1);
        moduleFlipMap.put("module_L5t_halfmodule_stereo_hole_sensor0", -1);
        moduleFlipMap.put("module_L5t_halfmodule_stereo_slot_sensor0", -1);
        moduleFlipMap.put("module_L6b_halfmodule_axial_hole_sensor0", -1);
        moduleFlipMap.put("module_L6b_halfmodule_axial_slot_sensor0", -1);
        moduleFlipMap.put("module_L6b_halfmodule_stereo_hole_sensor0", 1);
        moduleFlipMap.put("module_L6b_halfmodule_stereo_slot_sensor0", 1);
        moduleFlipMap.put("module_L6t_halfmodule_axial_hole_sensor0", 1);
        moduleFlipMap.put("module_L6t_halfmodule_axial_slot_sensor0", 1);
        moduleFlipMap.put("module_L6t_halfmodule_stereo_hole_sensor0", -1);
        moduleFlipMap.put("module_L6t_halfmodule_stereo_slot_sensor0", -1);
        moduleFlipMap.put("module_L7b_halfmodule_axial_hole_sensor0", -1);
        moduleFlipMap.put("module_L7b_halfmodule_axial_slot_sensor0", -1);
        moduleFlipMap.put("module_L7b_halfmodule_stereo_hole_sensor0", 1);
        moduleFlipMap.put("module_L7b_halfmodule_stereo_slot_sensor0", 1);
        moduleFlipMap.put("module_L7t_halfmodule_axial_hole_sensor0", 1);
        moduleFlipMap.put("module_L7t_halfmodule_axial_slot_sensor0", 1);
        moduleFlipMap.put("module_L7t_halfmodule_stereo_hole_sensor0", 1);
        moduleFlipMap.put("module_L7t_halfmodule_stereo_slot_sensor0", 1);
    }

    protected void process(EventHeader event) {

        // Get the list of fitted hits from the event
        List<LCRelation> fittedHits = event.get(LCRelation.class, fittedHitsCollectionName);
        fittedRawTrackerHitMap.clear();
        // Map the fitted hits to their corresponding raw hits
        for (LCRelation fittedHit : fittedHits) {
            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
        }

//        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
        setupSensors(event);
        List<SiTrackerHitStrip1D> stripClusters = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
        int nStripHits = stripClusters.size();
        aida.histogram1D("number of strip clusters in the event", 100, 0., 200.).fill(nStripHits);
        // lets partition the strip clusters into each module
        Map<String, List<SiTrackerHitStrip1D>> hitsPerModuleMap = new HashMap<>();
        if (debug) {
            System.out.println("found " + stripClusters.size() + " strip clusters");
        }
        for (TrackerHit thit : stripClusters) {
            double[] stripPos = thit.getPosition();
            List<RawTrackerHit> hits = thit.getRawHits();
            String moduleName = ((RawTrackerHit) hits.get(0)).getDetectorElement().getName();
            // Get the DetectorPlane for this sensor...
            Hit planeHitLocalPos = makeHit(_defaultDetector.planeMap().get(moduleName), stripPos, 0.006);

            //
            // get the transformation from global to local
            ITransform3D g2lXform = ((RawTrackerHit) thit.getRawHits().get(0)).getDetectorElement().getGeometry().getGlobalToLocal();
//            for (RawTrackerHit hit : hits) {
////                SiSensor sensor = (SiSensor) hit.getDetectorElement();
////                SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
////                if (debug) {
//                    System.out.println(Arrays.toString(hit.getPosition()));
//                    System.out.println(hit.getDetectorElement().getGeometry().getPosition());
////                }
//                //g2lXform = hit.getDetectorElement().getGeometry().getGlobalToLocal();
//            }
            Hep3Vector clusterPos = getPosition(hits, ((SiSensor) hits.get(0).getDetectorElement()).getReadoutElectrodes(ChargeCarrier.HOLE));
            if (debug) {
                System.out.println(" Strip Cluster at " + Arrays.toString(thit.getPosition()) + " has " + hits.size() + " strip hits");
                System.out.println("recalculated cluster position " + clusterPos);
            }
            aida.cloud1D(moduleName + " cluster size").fill(hits.size());
            //let's look at local coordinates...
            // get the hit's position in global coordinates..
            Hep3Vector globalPos = new BasicHep3Vector(stripPos);
            Hep3Vector localPos = g2lXform.transformed(globalPos);
            double lupos = ((RawTrackerHit) thit.getRawHits().get(0)).getDetectorElement().getGeometry().getGlobalToLocal().transformed(new BasicHep3Vector(thit.getPosition())).x();
            double u = localPos.x();
            double uPlane = planeHitLocalPos.uvm()[0];
            double du = u - moduleFlipMap.get(moduleName)*uPlane;
            if (hits.size() < 4) {
 //               aida.cloud2D(moduleName + " " + hits.size() + "u vs du").fill(u, du);
                aida.cloud1D(moduleName +  " du").fill(du);
//                aida.cloud1D(moduleName + " cluster dx " + hits.size() + " hits").fill(stripPos[0] - clusterPos.x());
//                aida.cloud1D(moduleName + " cluster dy " + hits.size() + " hits").fill(stripPos[1] - clusterPos.y());
//                aida.cloud1D(moduleName + " cluster dz " + hits.size() + " hits").fill(stripPos[2] - clusterPos.z());
//                aida.histogram1D(moduleName + " u position " + hits.size(), 2560, -19.2, 19.2).fill(u);
//                aida.histogram1D(moduleName + " u position modulo 0.06 " + hits.size(), 100, -0.06, 0.06).fill(u % 0.06);
//                aida.histogram2D(moduleName + " u position modulo 0.06 vs u" + hits.size(), 640, -19.2, 19.2, 100, -0.06, 0.06).fill(u, u % 0.06);
            }

//            
//            if (!hitsPerModuleMap.containsKey(moduleName)) {
//                hitsPerModuleMap.put(moduleName, new ArrayList<SiTrackerHitStrip1D>());
//                hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
//            } else {
//                hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
//            }
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
            if (des == null || des.isEmpty()) {
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

    //from StripMaker
    private Hep3Vector getPosition(List<RawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        boolean _debug = false;
        SiliconResolutionModel _res_model = new DefaultSiliconResolutionModel();
        // Sensor simulation needed to correct for Lorentz drift
        SiSensorSim _simulation = new CDFSiSensorSim();
        SiTrackerIdentifierHelper _sid_helper;

        // Number of strips beyond which charge is averaged on center strips
        int _max_noaverage_nstrips = 4;

        //cng
        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " getPosition for cluster size " + cluster.size());
        }
        List<Double> signals = new ArrayList<Double>();
        List<Hep3Vector> positions = new ArrayList<Hep3Vector>();

        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " Loop of " + cluster.size() + " and add signals and positions to vectors");
        }

        for (RawTrackerHit hit : cluster) {
            _sid_helper = (SiTrackerIdentifierHelper) hit.getDetectorIdentifierHelper();
            signals.add(FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(hit)));
            if (electrodes instanceof ThinSiStrips) {
                positions.add(((ThinSiStrips) electrodes).getStripCenter(_sid_helper.getElectrodeValue(hit.getIdentifier())));
//                if (hit.getRawTrackerHit().getLayerNumber() < 4)
//                    System.out.println("thinStripCenter is at " + ((ThinSiStrips) electrodes).getStripCenter(_strip_map.get(hit)).toString());
            } else {
                positions.add(((SiStrips) electrodes).getStripCenter(_sid_helper.getElectrodeValue(hit.getIdentifier())));
//                if (hit.getRawTrackerHit().getLayerNumber() < 4)
//                    System.out.println("stripCenter is at " + ((SiStrips) electrodes).getStripCenter(_strip_map.get(hit)).toString());
            }
            if (_debug) {
                System.out.println(this.getClass().getSimpleName() + " Added hit with signal " + FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(hit)) + " at strip center posiiton " + ((SiStrips) electrodes).getStripCenter(_sid_helper.getElectrodeValue(hit.getIdentifier())));
            }
        }

        // Average charge on central strips of longer clusters
        if (signals.size() > _max_noaverage_nstrips) {
            int nstrips_center = signals.size() - 2;

            // collect sum of charges on center strips
            double center_charge_sum = 0.0;
            for (int istrip = 1; istrip < signals.size() - 1; istrip++) {
                center_charge_sum += signals.get(istrip);
            }

            // distribute evenly on center strips
            double center_charge_strip = center_charge_sum / nstrips_center;
            for (int istrip = 1; istrip < signals.size() - 1; istrip++) {
                signals.set(istrip, center_charge_strip);
            }
        }

        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " Calculate charge weighted mean for " + signals.size() + " signals");
        }

        Hep3Vector position = _res_model.weightedAveragePosition(signals, positions);

        if (_debug) {
            if (signals.size() == 2) {
                StringBuffer sb = new StringBuffer(" 2 strip cluster: ");
                for (int i = 0; i < signals.size(); ++i) {
                    sb.append(positions.get(i).x() + " " + signals.get(i) + " ");
                }
                System.out.println(sb.toString() + " " + position.x());
            }
        }
        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " charge weighted position " + position.toString() + " (before trans)");
        }
        electrodes.getParentToLocal().inverse().transform(position);
        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " charge weighted position " + position.toString() + " (after trans)");
        }

        // Swim position back through lorentz drift direction to midpoint between bias surfaces
        if (_simulation != null) {
            _simulation.setSensor((SiSensor) electrodes.getDetectorElement());
            _simulation.lorentzCorrect(position, electrodes.getChargeCarrier());
            if (_debug) {
                System.out.println(this.getClass().getSimpleName() + ": Position " + position.toString() + " ( after Lorentz)");
            }
        }

        // return position in global coordinates
        Hep3Vector newpos = ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);
        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " final cluster position " + newpos.toString());
        }

        return ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);
        // return electrodes.getLocalToGlobal().transformed(position);
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

}
