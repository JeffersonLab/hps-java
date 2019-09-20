package org.hps.analysis.fieldOff;

import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
public class StraightTrackAnalysis2019_Bottom extends Driver {

    private AIDA aida = AIDA.defaultInstance();
//    private double[] H02Wire = {0., 0., -(672.71 - 551.64) * 25.4};
    private double[] H02Wire = {0., 0., -(672.71 - 583.44) * 25.4};

    private boolean _debug = false;

    protected void process(EventHeader event) {

        // lets start with bottom clusters above threshold...
        boolean isBottomCluster = false;
        double minClusterEnergy = 3.0;
        double lineSlope = 0.;
        List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
        aida.histogram1D("number of clusters", 10, 0., 10.).fill(clusters.size());
        if (clusters.size() != 1) {
            return;
        }
        for (Cluster cluster : clusters) {
            if (cluster.getEnergy() > minClusterEnergy && TriggerModule.inFiducialRegion(cluster)) {
                if (cluster.getPosition()[1] < 0.) {
                    isBottomCluster = true;
                    aida.histogram2D("Cal Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                    // calculate some slopes and intercepts...
                    aida.histogram1D("Cluster z", 10, 1443., 1445.).fill(cluster.getPosition()[2]);
                    lineSlope = cluster.getPosition()[1] / (cluster.getPosition()[2] - H02Wire[2]);
                }
            }
        }
        if (!isBottomCluster) {
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
        String[] layerNames = {"module_L3b_halfmodule_axial_sensor0",
            "module_L4b_halfmodule_axial_sensor0",
            "module_L6b_halfmodule_axial_hole_sensor0",
            "module_L7b_halfmodule_axial_hole_sensor0"};
        // lets accumulate the hits in the wire-cluster track window
        Map<String, List<TrackerHit>> hitsInWindow = new HashMap();
        Map<String, Double> singleHitInWindowResidual = new HashMap();
        Map<String, Integer> singleHitInWindowClusterSize = new HashMap();
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

//        if(_debug) System.out.println(hm);
        for (TrackerHit hit : stripClusters) {
            double[] stripClusterPos = hit.getPosition();
//            if(_debug) System.out.println("strip cluster position " + Arrays.toString(stripClusterPos));
            List rthList = hit.getRawHits();
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
//            if(_debug) System.out.println("module " + moduleName);
            if (moduleName.contains("axial") && moduleName.contains("b_")) {
                String layer = moduleName.substring(7, 10);
                int layerInt = Integer.parseInt(moduleName.substring(8, 9));
                if (layerInt > 2) {
                    double predicted = lineSlope * (stripClusterPos[2] - H02Wire[2]);
                    aida.histogram1D(layer + " measured", 50, -50., 0.).fill(stripClusterPos[1]);
                    aida.histogram1D(layer + " predicted", 50, -50., 0.).fill(predicted);
                    double dPos = stripClusterPos[1] - predicted;
                    aida.histogram1D(layer + " measured - predicted", 50, -10., 40.).fill(stripClusterPos[1] - predicted);

                    if (hm.get(moduleName) < nMaxHits) {
                        aida.histogram1D(layer + " < " + nMaxHits + " hits hit measured", 50, -50., 0.).fill(stripClusterPos[1]);
                        aida.histogram1D(layer + " < " + nMaxHits + " hits hit predicted", 50, -50., 0.).fill(predicted);
                        aida.histogram1D(layer + " < " + nMaxHits + " hits hit measured - predicted", 50, -10., 40.).fill(stripClusterPos[1] - predicted);
//                    aida.cloud1D(layer + " " + hm.get(moduleName) + " hit measured").fill(stripClusterPos[1]);
//                    aida.cloud1D(layer + " " + hm.get(moduleName) + " hit predicted").fill(predicted);
//                    aida.cloud1D(layer + " " + hm.get(moduleName) + " hit measured - predicted").fill(stripClusterPos[1] - predicted);
                    }
                    // if hit is within the prediction window (+/- dPosMax) store it...
                    if (layerNamesList.contains(moduleName) && abs(dPos) < dPosMax) {
                        aida.histogram1D(layer + " hit in windows measured - predicted", 100, -5., 5.).fill(stripClusterPos[1] - predicted);
                        hitsInWindow.get(moduleName).add(hit);
                        singleHitInWindowResidual.put(moduleName, dPos);
                        singleHitInWindowClusterSize.put(moduleName, rthList.size());
                    }
                }
            }
        }
        // lets check how many hits we have in each layer window
        // require one and only one hit in each of the windows
        boolean oneHitInEachLayer = true;
        boolean allDoubleHits = true;
        boolean allSingleHits = true;
        for (String s : layerNames) {
            if (hitsInWindow.get(s).size() != 1) {
                oneHitInEachLayer = false;
            }
            if (oneHitInEachLayer) {
                if (singleHitInWindowClusterSize.get(s) != 2) {
                    allDoubleHits = false;
                }
                if (singleHitInWindowClusterSize.get(s) != 1) {
                    allSingleHits = false;
                }
                aida.histogram1D("Layer " + s + " single hit in window measured - predicted", 100, -5., 5.).fill(singleHitInWindowResidual.get(s));
            }
            aida.histogram1D("Number of hits in track window in layer " + s, 5, 0., 5.).fill(hitsInWindow.get(s).size());
        }

        //lets calculate and compare the slope for hits in 3-4 with 6-7
        // field-off tracks don't hit layer 1&2
        // bottom layer 5 axial is dead
        if (oneHitInEachLayer) {
            double[] hit3 = hitsInWindow.get(layerNames[0]).get(0).getPosition();
            double[] hit4 = hitsInWindow.get(layerNames[1]).get(0).getPosition();
            double[] hit6 = hitsInWindow.get(layerNames[2]).get(0).getPosition();
            double[] hit7 = hitsInWindow.get(layerNames[3]).get(0).getPosition();
            TwoPointLine tpl34 = new TwoPointLine(hit3[2], hit3[1], hit4[2], hit4[1]);
            TwoPointLine tpl67 = new TwoPointLine(hit6[2], hit6[1], hit7[2], hit7[1]);
            aida.histogram1D("slope34", 100, -0.03, 0.03).fill(tpl34.slope());
            aida.histogram1D("slope67", 100, -0.03, 0.03).fill(tpl67.slope());
            aida.histogram1D("zIntercept34", 100, -4000., -1000.).fill(tpl34.xAxisIntercept());
            aida.histogram1D("zIntercept67", 100, -4000., -1000.).fill(tpl67.xAxisIntercept());
            aida.histogram1D("slope67 - slope34", 200, -0.01, 0.01).fill(tpl67.slope() - tpl34.slope());
            // compare y value at z=400
            double zPos = 400.;
            double y34at400 = tpl34.predict(zPos);
            double y67at400 = tpl67.predict(zPos);
            aida.histogram1D("y67at400 - y34at400", 100, -2., 2.).fill(y67at400 - y34at400);

            if (allSingleHits) {
                aida.histogram1D("slope67 - slope34 All Single Hits", 200, -0.01, 0.01).fill(tpl67.slope() - tpl34.slope());
                aida.histogram1D("y67at400 - y34at400 All Single Hits", 100, -2., 2.).fill(y67at400 - y34at400);
            }

            if (allDoubleHits) {
                aida.histogram1D("slope67 - slope34 All Double Hits", 200, -0.01, 0.01).fill(tpl67.slope() - tpl34.slope());
                aida.histogram1D("y67at400 - y34at400 All Double Hits", 100, -2., 2.).fill(y67at400 - y34at400);
            }

            //let's try to get any stereo hits associated with these axial hits...
            // it's complicated, so hang on...
            List<TrackerHit> helicalTrackHits = event.get(TrackerHit.class, "HelicalTrackHits");
            //loop over all of the 3D Helical track hits and get the strip hits which constitute it...
            //create a map relating the two strips
            Map<TrackerHit, TrackerHit> axialToStereoMap = new HashMap<>();
            // for track fitting wil also want to know the sensor
            //create a map keyed on strip which can provide the sensor
            Map<TrackerHit, String> stripToSensorMap = new HashMap<>();
            for (TrackerHit hth : helicalTrackHits) {
                Collection<TrackerHit> htsList = hitToStrips.allFrom(hth);
                TrackerHit[] htsArray = htsList.toArray(new TrackerHit[2]);
                // could be smarter and try to identify the axial strip, but this is quicker...
                axialToStereoMap.put(htsArray[0], htsArray[1]);
                axialToStereoMap.put(htsArray[1], htsArray[0]);
                stripToSensorMap.put(htsArray[0], ((RawTrackerHit) htsArray[0].getRawHits().get(0)).getDetectorElement().getName());
                stripToSensorMap.put(htsArray[1], ((RawTrackerHit) htsArray[1].getRawHits().get(0)).getDetectorElement().getName());
            }
            //OK should now have a map that contains all of the axial strips which have a stereo strip paired with it
            // should also have a map of sensor names keyed on strip hit
            List<TrackerHit> hitsToFit = new ArrayList<>();
            Map<TrackerHit, String> stripSensorMap = new HashMap<>();
            for (int i = 0; i < 4; ++i) {
                // since we know we only have one hit in the tracking window...
                TrackerHit axialTrackerHit = hitsInWindow.get(layerNames[i]).get(0);
                // now get its partner (if it has one...
                if (axialToStereoMap.containsKey(axialTrackerHit)) {
                    TrackerHit stereoTrackerHit = axialToStereoMap.get(axialTrackerHit);
                    hitsToFit.add(stereoTrackerHit);
                    stripSensorMap.put(stereoTrackerHit, stripToSensorMap.get(stereoTrackerHit));
                    // recall that in the bottom, stereo comes before axial, so this puts the hits in ascending z order...
                    hitsToFit.add(axialTrackerHit);
                    stripSensorMap.put(axialTrackerHit, stripToSensorMap.get(axialTrackerHit));
                } else {
                    hitsToFit.add(axialTrackerHit);
                    stripSensorMap.put(axialTrackerHit, layerNames[i]);
                }
            }
            // should now have (at most) eight 1D strip hits to fit...
            if (_debug) {
                System.out.println("found " + hitsToFit.size() + " hits to fit");
                for (TrackerHit hit : hitsToFit) {
                    System.out.println(stripSensorMap.get(hit));
                }
            }
            // now to fit these hits...
        } // end of oneHitInEachLayer

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
