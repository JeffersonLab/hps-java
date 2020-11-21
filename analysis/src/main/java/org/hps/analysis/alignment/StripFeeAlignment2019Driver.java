package org.hps.analysis.alignment;

import hep.physics.vec.Hep3Vector;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hps.analysis.alignment.straighttrack.DetectorBuilder;
import org.hps.analysis.alignment.straighttrack.DetectorPlane;
import org.hps.analysis.alignment.straighttrack.StraightTrackUtils;
import org.hps.recon.ecal.cluster.ClusterUtilities;
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
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A Graf
 */
public class StripFeeAlignment2019Driver extends Driver {

    double _minClusterEnergy = 3.5;
    double _minSeedHitEnergy = 2.5;

    private AIDA aida = AIDA.defaultInstance();

    double _targetZ = -7.5;

    int maxHitsInSensor = 30;

    int _maxNClusters = 2;

    int _numberOfEventsSelected;

    int _minNumberOfAxialHits = 4;

    //TODO maybe use layer-dependent cuts on this distance...
    double _maxDist = 10.;

    private int _maxSvtRawTrackerHits = 250;
    private boolean _requireFiducial = false;

    String _holeOrSlot = "Slot";

    // First two layers are striplets, so punt for now
    List<String> topHoleSensorNames = Arrays.asList(
            //            "module_L1t_halfmodule_axial_sensor0",
            //            "module_L2t_halfmodule_axial_sensor0",
            "module_L3t_halfmodule_axial_sensor0",
            "module_L4t_halfmodule_axial_sensor0",
            "module_L5t_halfmodule_axial_hole_sensor0",
            "module_L6t_halfmodule_axial_hole_sensor0",
            "module_L7t_halfmodule_axial_hole_sensor0");

    //runs 10099 and 10101 have all 10 good layers
    List<String> bottomHoleSensorNames = Arrays.asList(
            //            "module_L1b_halfmodule_axial_sensor0",
            //            "module_L2b_halfmodule_axial_sensor0",
            "module_L3b_halfmodule_axial_sensor0",
            "module_L4b_halfmodule_axial_sensor0",
            "module_L5b_halfmodule_axial_hole_sensor0",
            "module_L6b_halfmodule_axial_hole_sensor0",
            "module_L7b_halfmodule_axial_hole_sensor0");

    // top is missing last layer
    List<String> topSlotSensorNames = Arrays.asList(
            //            "module_L1t_halfmodule_axial_sensor0",
            //            "module_L2t_halfmodule_axial_sensor0",
            "module_L3t_halfmodule_axial_sensor0",
            "module_L4t_halfmodule_axial_sensor0",
            "module_L5t_halfmodule_axial_slot_sensor0",
            "module_L6t_halfmodule_axial_slot_sensor0",
            "module_L7t_halfmodule_axial_slot_sensor0");

    //runs 10099 and 10101 have all 10 good layers
    List<String> bottomSlotSensorNames = Arrays.asList(
            //            "module_L1b_halfmodule_axial_sensor0",
            //            "module_L2b_halfmodule_axial_sensor0",
            "module_L3b_halfmodule_axial_sensor0",
            "module_L4b_halfmodule_axial_sensor0",
            "module_L5b_halfmodule_axial_slot_sensor0",
            "module_L6b_halfmodule_axial_slot_sensor0",
            "module_L7b_halfmodule_axial_slot_sensor0");

    DetectorBuilder _defaultDetector;

    protected void detectorChanged(Detector detector) {

        _defaultDetector = new DetectorBuilder(detector);
        System.out.println("Selecting tracks on the " + _holeOrSlot + " side");
    }

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        if (event.get(RawTrackerHit.class, "SVTRawTrackerHits").size() < _maxSvtRawTrackerHits) {
            List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
            aida.histogram1D("Number of clusters", 100, 0., 7.).fill(clusters.size());
            for (Cluster cluster : clusters) {
                aida.histogram1D("All Cluster energy", 100, 0., 5.5).fill(cluster.getEnergy());
                double seedEnergy = ClusterUtilities.findSeedHit(cluster).getCorrectedEnergy();
                if (seedEnergy > _minSeedHitEnergy) {
                    aida.histogram1D("All Cluster energy seed energy > " + _minSeedHitEnergy, 100, 0., 5.5).fill(cluster.getEnergy());
                }
                aida.histogram2D("All Cluster energy vs seed energy", 100, 2.5, 5.5, 100, 0.5, 3.5).fill(cluster.getEnergy(), seedEnergy);
                if (clusters.size() == 1) {
                    aida.histogram2D("All Cluster energy vs seed energy nClusters = 1", 100, 2.5, 5.5, 100, 0.5, 3.5).fill(cluster.getEnergy(), seedEnergy);
                }
                if (clusters.size() == 2) {
                    aida.histogram2D("All Cluster energy vs seed energy nClusters = 2", 100, 2.5, 5.5, 100, 0.5, 3.5).fill(cluster.getEnergy(), seedEnergy);
                }
            }
            if (clusters.size() <= _maxNClusters) {
                for (Cluster cluster : clusters) {
                    aida.histogram1D("All Cluster energy nClusters <= " + _maxNClusters, 100, 0., 5.5).fill(cluster.getEnergy());
                    double seedEnergy = ClusterUtilities.findSeedHit(cluster).getCorrectedEnergy();
                    aida.histogram2D("All Cluster energy nClusters <= " + _maxNClusters + " vs seed energy", 100, 2.5, 5.5, 100, 0.5, 3.5).fill(cluster.getEnergy(), seedEnergy);
                    if (cluster.getEnergy() > _minClusterEnergy) {
                        if (seedEnergy > _minSeedHitEnergy) {
                            boolean isFiducial = TriggerModule.inFiducialRegion(cluster);
//                            if (isFiducial) {
//                                System.out.println("run " + event.getRunNumber() + " event " + event.getEventNumber() + " passed cuts");
                            setupSensors(event);
                            String fid = isFiducial ? "fiducial" : "";
                            Cluster c = cluster;
                            String topOrBottom = c.getPosition()[1] > 0 ? "top " : "bottom ";
                            boolean isTop = c.getPosition()[1] > 0;
                            List<String> sensorNames = null;
                            if (_holeOrSlot.equals("Hole")) {
                                sensorNames = isTop ? topHoleSensorNames : bottomHoleSensorNames;
//                                System.out.println(sensorNames);
                            }
                            if (_holeOrSlot.equals("Slot")) {
                                sensorNames = isTop ? topSlotSensorNames : bottomSlotSensorNames;
//                                System.out.println(sensorNames);
                            }
                            aida.histogram2D("Cal " + fid + " Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);

//                                aida.histogram1D(topOrBottom + fid + " Cluster z", 10, 1443., 1445.).fill(cluster.getPosition()[2]);
                            aida.histogram1D(topOrBottom + fid + " Cluster energy", 100, 0., 5.5).fill(cluster.getEnergy());

                            //OK, we have a good, high-energy cluster in the calorimeter...
                            // calculate some slopes and intercepts...
                            Point3D P0 = new Point3D(0., 0., _targetZ);
                            double[] cPos = c.getPosition();
                            Point3D P1 = new Point3D(cPos[0], cPos[1], cPos[2]);
                            //let's get some SVT strip clusters...
                            List<SiTrackerHitStrip1D> stripClusters = event.get(SiTrackerHitStrip1D.class,
                                    "StripClusterer_SiTrackerHitStrip1D");
//                            System.out.println("found " + stripClusters.size() + " strip clusters");
                            // aida.histogram1D(topOrBottom + fid + "number of strip clusters", 100, 0., 50.).fill(nStripHits);
                            // lets partition the strip clusters into each module
                            Map<String, List<SiTrackerHitStrip1D>> hitsPerModuleMap = new HashMap<>();
                            for (TrackerHit hit : stripClusters) {
                                List rthList = hit.getRawHits();
                                String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
//                                System.out.println(moduleName);
                                if (sensorNames.contains(moduleName)) {
//                                    System.out.println(moduleName + " contains hit " + hit);
                                    if (!hitsPerModuleMap.containsKey(moduleName)) {
                                        hitsPerModuleMap.put(moduleName, new ArrayList<SiTrackerHitStrip1D>());
                                        hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
                                    } else {
                                        hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
                                    }
                                }
                            } // end of loop over strip hits
                            for (String s : hitsPerModuleMap.keySet()) {
//                                System.out.println(s + " has " + hitsPerModuleMap.get(s).size() + " strip hit clusters");
                                aida.histogram1D(s + " hits", 100, 0., 50.).fill(hitsPerModuleMap.get(s).size());
                            }
                            Map<String, SiTrackerHitStrip1D> hitsToFit = new LinkedHashMap<>();
                            Map<String, DetectorPlane> detectorPlanesInFit = new LinkedHashMap<>();

                            String trackingDetectorName = cPos[1] > 0 ? "top" + _holeOrSlot : "bottom" + _holeOrSlot;

                            List<DetectorPlane> td = _defaultDetector.getTracker(trackingDetectorName);
                            String[] trackerSensorNames = _defaultDetector.getTrackerSensorNames(trackingDetectorName);

                            for (DetectorPlane dp : td) {
                                String moduleName = trackerSensorNames[dp.id() - 1];
                                if (moduleName.contains("axial")) {
                                    int nHitsInWindow = 0;
                                    if (hitsPerModuleMap.containsKey(moduleName)) {
                                        //System.out.println(moduleName + " has " + hitsPerModuleMap.get(moduleName).size() + " strip hit clusters");

                                        // get the best hit in this layer associated with this cluster                     
                                        SiTrackerHitStrip1D closest = null;
                                        double d = 9999.;
                                        for (SiTrackerHitStrip1D stripHit : hitsPerModuleMap.get(moduleName)) {
                                            // calculate the intercept of the straight track with this sensor...
                                            Hep3Vector intercept = StraightTrackUtils.linePlaneIntersect(P0, P1, dp.origin(), dp.normal());
//                                            System.out.println(moduleName + " intercept :" + intercept);
                                            Hep3Vector point = stripHit.getPositionAsVector();
//                                            System.out.println("point: " + point);
                                            // calculate the distance between this point and the strip
                                            LineSegment3D stripLine = stripHit.getHitSegment();
                                            double dist = stripLine.distanceTo(new Point3D(intercept));
//                                            System.out.println("dist: " + dist);
//                        double d2 = VecOp.cross(stripLine.getDirection(),VecOp.sub(intercept,stripLine.getStartPoint())).magnitude();
//                        System.out.println("dist "+dist+" d2 "+d2);
                                            if (abs(dist) < _maxDist) {
                                                nHitsInWindow++;
                                            }
                                            if (abs(dist) < d) {
                                                d = dist;
                                                closest = stripHit;
                                            }
                                        }
                                        // are we within a reasonable distance?
                                        if (abs(d) < _maxDist) {
                                            hitsToFit.put(moduleName, closest);
                                            detectorPlanesInFit.put(moduleName, dp);
                                            aida.histogram1D(moduleName + fid + " distance to hit", 100, 0., _maxDist).fill(d);
                                            aida.histogram1D(moduleName + fid + " number of hits in " + _maxDist + " window", 10, 0., 10.).fill(nHitsInWindow);
                                        }
                                    }
                                }
//                System.out.println(dp.id() + " " + trackerSensorNames[dp.id()-1]);
//                System.out.println(dp);
                            } // end of loop over detector planes in this detector
                            // did we get hits within the window in each of the sensor planes
                            //System.out.println("found " + hitsToFit.keySet().size() + " sensors with hits in the window for " + trackingDetectorName);
                            aida.histogram1D(trackingDetectorName + " number of sensors with hit in " + _maxDist + "window", 10, 0., 10.).fill(hitsToFit.keySet().size());
                            if (hitsToFit.keySet().size() >= _minNumberOfAxialHits) {
                                aida.histogram2D("Selected Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                                aida.histogram1D("Selected " + topOrBottom + " Cluster energy", 100, 0., 5.5).fill(cluster.getEnergy());
                                skipEvent = false;
                                if (_requireFiducial && !isFiducial) {
                                    skipEvent = true;
                                }
                            } // end of check on number of acceptable hits in axial layers
//                            }// end of check on fiducial cluster
                        }//end of check on cluster seed energy
                    }// end of check on cluster energy
                } // end of loop over clusters
            }//end of check on _maxNClusters
        }//end of check on maxSvtRawTrackerHits
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
//            System.out.println("\n ****BINGO**** \n");
            _numberOfEventsSelected++;
        }
    }

    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsSelected + " events");
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

    public void setHoleOrSlot(String s) {
        _holeOrSlot = s;
    }

    public void setMinNumberOfAxialHits(int i) {
        _minNumberOfAxialHits = i;
    }

    public void setTargetZ(double d) {
        _targetZ = d;
    }

    public void setMinSeedHitEnergy(double d) {
        _minSeedHitEnergy = d;
    }

    public void setMinClusterEnergy(double d) {
        _minClusterEnergy = d;
    }

    public void setRequireFiducial(boolean b) {
        _requireFiducial = b;
    }

    public void setMaxDist(double d) {
        _maxDist = d;
    }
    
    public void setMaxNClusters(int i)
    {
        _maxNClusters = i;
    }
}
