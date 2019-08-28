package org.hps.analysis.fieldOff;

import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.TrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class StraightTrackAnalysis2019_Top extends Driver {

    private AIDA aida = AIDA.defaultInstance();
//    private double[] H02Wire = {0., 0., -(672.71 - 551.64) * 25.4};
    private double[] H02Wire = {0., 0., -(672.71 - 583.44) * 25.4};

    protected void process(EventHeader event) {

        // lets start with top clusters above threshold...
        boolean isTopCluster = false;
        double minClusterEnergy = 3.0;
        double lineSlope = 0.;
        List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
        aida.histogram1D("number of clusters", 10, 0., 10.).fill(clusters.size());
        if (clusters.size() != 1) {
            return;
        }
        for (Cluster cluster : clusters) {
            if (cluster.getEnergy() > minClusterEnergy && TriggerModule.inFiducialRegion(cluster)) {
                if (cluster.getPosition()[1] > 0.) {
                    isTopCluster = true;
                    aida.histogram2D("Cal Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                    // calculate some slopes and intercepts...
                    aida.cloud1D("Cluster z").fill(cluster.getPosition()[2]);
                    lineSlope = cluster.getPosition()[1] / (cluster.getPosition()[2] - H02Wire[2]);
                }
            }
        }
        if (!isTopCluster) {
            return;
        }
        Cluster c = clusters.get(0);
        if (c.getEnergy() < minClusterEnergy) {
            return;
        }
        // lets just look at central clusters
        if (abs(c.getPosition()[0]) > 50.) {
            return;
        }
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
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
        aida.histogram1D("number of strip clusters", 100, 0., 200.).fill(nStripHits);
        // lets count how many strip clusters there are in each module
        Map<String, Integer> hm = new HashMap();
        for (TrackerHit hit : stripClusters) {
            List rthList = hit.getRawHits();
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
            if (!hm.containsKey(moduleName)) {
                hm.put(moduleName, 1);
            } else {
                hm.put(moduleName, hm.get(moduleName) + 1);
            }
        }
        // lets cut on some global occupancies for the layers we care about...
        String[] layerNames = {"module_L3t_halfmodule_axial_sensor0",
            "module_L4t_halfmodule_axial_sensor0",
            "module_L5t_halfmodule_axial_hole_sensor0",
            "module_L6t_halfmodule_axial_hole_sensor0"};
        // lets accumulate the hits in the wire-cluster track window
        Map<String, List<TrackerHit>> hitsInWindow = new HashMap();
        List<String> layerNamesList = Arrays.asList(layerNames);
        boolean goodEvent = true;
        int nMaxHits = 10;
        double dPosMax = 5.; // maximum distance between predicted and measured axial y position
        // require all four layers to have less than nMaxHits hits in them
        for (String s : layerNames) {
            if (hm.containsKey(s)) {
                if (hm.get(s) < 1 || hm.get(s) >= nMaxHits) {
                    goodEvent = false;
                }
            } else {
                goodEvent = false;
            }
            hitsInWindow.put(s, new ArrayList<TrackerHit>());
        }
        if (!goodEvent) {
            return;
        }

//        System.out.println(hm);
        for (TrackerHit hit : stripClusters) {
            double[] stripClusterPos = hit.getPosition();
//            System.out.println("strip cluster position " + Arrays.toString(stripClusterPos));
            List rthList = hit.getRawHits();
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
//            System.out.println("module " + moduleName);
            if (moduleName.contains("axial") && moduleName.contains("t_half")) {
                //System.out.println(moduleName);
                String layer = moduleName.substring(7, 10);
                double predicted = lineSlope * (stripClusterPos[2] - H02Wire[2]);
                aida.cloud1D(layer + " measured").fill(stripClusterPos[1]);
                aida.cloud1D(layer + " predicted").fill(predicted);
                double dPos = stripClusterPos[1] - predicted;
                aida.cloud1D(layer + " measured - predicted").fill(stripClusterPos[1] - predicted);

                if (hm.get(moduleName) < nMaxHits) {
                    aida.cloud1D(layer + " < " + nMaxHits + " hits hit measured").fill(stripClusterPos[1]);
                    aida.cloud1D(layer + " < " + nMaxHits + " hits hit predicted").fill(predicted);
                    aida.cloud1D(layer + " < " + nMaxHits + " hits hit measured - predicted").fill(stripClusterPos[1] - predicted);
//                    aida.cloud1D(layer + " " + hm.get(moduleName) + " hit measured").fill(stripClusterPos[1]);
//                    aida.cloud1D(layer + " " + hm.get(moduleName) + " hit predicted").fill(predicted);
//                    aida.cloud1D(layer + " " + hm.get(moduleName) + " hit measured - predicted").fill(stripClusterPos[1] - predicted);
                }
                // if hit is within the prediction window (+/- dPosMax) store it...
                if (layerNamesList.contains(moduleName) && abs(dPos)<dPosMax) {
                    hitsInWindow.get(moduleName).add(hit);
                }
            }
        }
        // lets check how many hits we have in each layer window
        // require one and only one hit in each of the windows
        boolean oneHitInEachLayer = true;
        for (String s : layerNames) {
            if(hitsInWindow.get(s).size()!=1) oneHitInEachLayer = false;
            aida.histogram1D("Number of hits in track window in layer " + s, 5, 0., 5.).fill(hitsInWindow.get(s).size());
        }
        
        //lets calculate and compare the slope for hits in 3-4 with 6-7
        // field-off tracks don't hit layer 1&2
        // bottom layer 5 axial is dead
        if(oneHitInEachLayer)
        {
            double[] hit3 = hitsInWindow.get(layerNames[0]).get(0).getPosition();
            double[] hit4 = hitsInWindow.get(layerNames[1]).get(0).getPosition();
            double[] hit5 = hitsInWindow.get(layerNames[2]).get(0).getPosition();
            double[] hit6 = hitsInWindow.get(layerNames[3]).get(0).getPosition();
            TwoPointLine tpl34 = new TwoPointLine(hit3[2],hit3[1],hit4[2],hit4[1]);
            TwoPointLine tpl56 = new TwoPointLine(hit5[2],hit5[1],hit6[2],hit6[1]);
            aida.histogram1D("slope34", 100, -0.03, 0.03).fill(tpl34.slope());
            aida.histogram1D("slope56", 100, -0.03, 0.03).fill(tpl56.slope());
            aida.histogram1D("zIntercept34", 100, -5000., -2000.).fill(tpl34.xAxisIntercept());
            aida.histogram1D("zIntercept56", 100, -5000., -2000.).fill(tpl56.xAxisIntercept());
            aida.histogram1D("slope56 - slope34", 100, -0.005, 0.005).fill(tpl56.slope()-tpl34.slope());
        }

        aida.histogram1D(
                "good cluster energy ", 100, 0., 6.).fill(c.getEnergy());
        aida.histogram2D(
                "good cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class,
                "SVTRawTrackerHits");
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
