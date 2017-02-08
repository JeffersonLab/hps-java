package org.hps.analysis.MC;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.io.IOException;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.analysis.examples.DetailedAnalysisDriver;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Simple Driver to compare positions of the SimTrackerHits with the TrackerHits
 * created by the full SVT simulation. This analyzes SimTrackerHits and
 * TrackerHits separately, so should only be run on MC events with single
 * tracks. TODO follow the link back from the TrackerHit to the SimTrackerHit to
 * get a 1-to-1 correspondence between the hits.
 */
public class MCTrackerHitResidualAnalysisDriver extends Driver {

    private boolean debug = false;
    AIDA aida = AIDA.defaultInstance();
    String aidaFileName = "MCTrackerHitResidualAnalysisDriverPlots";
    String aidaFileType = "aida";

    boolean _debug = false;

    private String siClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";

    @Override
    protected void process(EventHeader event) {

        // cng
        // make relational table for strip clusters to mc particle
        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY,
                RelationalTable.Weighting.UNWEIGHTED);
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY,
                RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    rawtomc.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        List<TrackerHit> siClusters = event.get(TrackerHit.class, siClusterCollectionName);
        RelationalTable clustertosimhit = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY,
                RelationalTable.Weighting.UNWEIGHTED);
        for (TrackerHit cluster : siClusters) {
            List<RawTrackerHit> rawHits = cluster.getRawHits();
            for (RawTrackerHit rth : rawHits) {
                Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(rth);
                if (simTrackerHits != null) {
                    for (SimTrackerHit simhit : simTrackerHits) {
                        clustertosimhit.add(cluster, simhit);
                    }
                }
            }
        }
        // cng
        // a map of MC hit positions keyed on sensor name
        Map<String, List<Double>> mcSensorHitPositionMap = new HashMap<String, List<Double>>();
        // a map of Tracker hit positions keyed on sensor name
        Map<String, List<Double>> trackSensorHitPositionMap = new HashMap<String, List<Double>>();

        // First step is to get the SimTrackerHits and determine their location
        // in local coordinates.
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, "TrackerHits");
        if (_debug)
            System.out.println("found " + simHits.size() + " SimTrackerHits");
        // loop over each hit
        for (SimTrackerHit hit : simHits) {
            Hep3Vector stripPos = null;
            SymmetricMatrix covG = null;
            // did we correctly map clusters to this simhit?
            Set<TrackerHit> clusters = clustertosimhit.allTo(hit);
            if (_debug)
                System.out.println("found " + clusters.size() + " clusters associated to this SimTrackerHit");
            int clusterSize = 0;
            if (clusters != null) {
                for (TrackerHit clust : clusters) {
                    clusterSize = clust.getRawHits().size();
                    double[] clusPos = clust.getPosition();
                    stripPos = new BasicHep3Vector(clusPos);
                    // now for the uncertainty in u
                    covG = new SymmetricMatrix(3, clust.getCovMatrix(), true);
                }
            }

            // get the hit's position in global coordinates..
            Hep3Vector globalPos = hit.getPositionVec();
            // get the transformation from global to local
            ITransform3D g2lXform = hit.getDetectorElement().getGeometry().getGlobalToLocal();
            // System.out.println("transform matrix: " + g2lXform);
            IRotation3D rotMat = g2lXform.getRotation();
            // System.out.println("rotation matrix: " + rotMat);
            ITranslation3D transMat = g2lXform.getTranslation();
            // System.out.println("translation vector: " + transMat);
            // check that we can reproduce the local origin
            ITransform3D l2gXform = hit.getDetectorElement().getGeometry().getLocalToGlobal();
            Hep3Vector o = new BasicHep3Vector();
            // System.out.println("origin: " + o);
            // tranform the local origin into global position
            Hep3Vector localOriginInglobal = l2gXform.transformed(o);
            // System.out.println("transformed local to global: " + localOriginInglobal);
            // and now back...
            // System.out.println("and back: " + g2lXform.transformed(localOriginInglobal));
            // hmmm, so why is this not the same as the translation vector of the transform?
            // Note:
            // u is the measurement direction perpendicular to the strip
            // v is along the strip
            // w is normal to the wafer plane

            Hep3Vector localPos = g2lXform.transformed(globalPos);
            // System.out.println("Layer: " + hit.getLayer() + " Layer Number: " + hit.getLayerNumber() + " ID: " +
            // hit.getCellID() + " " + hit.getDetectorElement().getName());
            // System.out.println("global position " + globalPos);
            // System.out.println("local  position " + localPos);
            String sensorName = hit.getDetectorElement().getName();
            double u = localPos.x();
            if (stripPos != null) {
                Hep3Vector clusLocalPos = g2lXform.transformed(stripPos);
                double clusU = clusLocalPos.x();
                aida.cloud1D(sensorName + " " + clusterSize + " strip cluster u-MC_u").fill(clusU - u);
                SymmetricMatrix covL = g2lXform.transformed(covG);
                double sigmaU = sqrt(covL.e(0, 0));
                aida.cloud1D(sensorName + " " + clusterSize + " strip cluster u-MC_u pull").fill((clusU - u) / sigmaU);
            }
            if (_debug)
                System.out.println("MC " + hit.getDetectorElement().getName() + " u= " + localPos.x());
            if (mcSensorHitPositionMap.containsKey(sensorName)) {
                List<Double> vals = mcSensorHitPositionMap.get(sensorName);
                vals.add(u);
            } else {
                List<Double> vals = new ArrayList<Double>();
                vals.add(u);
                mcSensorHitPositionMap.put(sensorName, vals);
            }
        } // end of loop over SimTrackerHits

        // // let's look at Tracker hits
        // setupSensors(event);
        // RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        // RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        // List<Track> tracks = event.get(Track.class, "MatchedTracks");
        // System.out.println("found " + tracks.size() + " tracks");
        // for (Track t : tracks) {
        // List<TrackerHit> hits = t.getTrackerHits();
        // System.out.println("track has " + hits.size() + " hits");
        // if (hits.size() == 6) {
        // for (TrackerHit h : hits) {
        // Set<TrackerHit> stripList = hitToStrips.allFrom(hitToRotated.from(h));
        // for (TrackerHit strip : stripList) {
        // List rawHits = strip.getRawHits();
        // HpsSiSensor sensor = null;
        // for (Object o : rawHits) {
        // RawTrackerHit rth = (RawTrackerHit) o;
        // // TODO figure out why the following collection is always null
        // List<SimTrackerHit> stipMCHits = rth.getSimTrackerHits();
        // sensor = (HpsSiSensor) rth.getDetectorElement();
        // }
        // int nHitsInCluster = rawHits.size();
        // String sensorName = sensor.getName();
        // Hep3Vector posG = new BasicHep3Vector(strip.getPosition());
        // Hep3Vector posL = sensor.getGeometry().getGlobalToLocal().transformed(posG);
        // double u = posL.x();
        // double mcU = mcSensorHitPositionMap.get(sensorName).get(0);
        //
        // aida.cloud1D(sensorName + "_" + nHitsInCluster + "_hitCluster meas - MC u position").fill(u - mcU);
        // // now for the uncertainty in u
        // SymmetricMatrix covG = new SymmetricMatrix(3, strip.getCovMatrix(), true);
        // SymmetricMatrix covL = sensor.getGeometry().getGlobalToLocal().transformed(covG);
        // double sigmaU = sqrt(covL.e(0, 0));
        // aida.cloud1D(sensorName + "_" + nHitsInCluster + "_hitCluster meas - MC u position pull").fill((u - mcU) /
        // sigmaU);
        // aida.cloud1D(sensorName + " number of hits in cluster").fill(nHitsInCluster);
        // System.out.println(" Track Hit: " + nHitsInCluster + " " + sensorName + " u " + u + " mcU " + mcU +
        // " sigmaU " + sigmaU);
        // }
        // }
        // } // end of loop over six-hit tracks
        // } // end of loop over tracks
    }

    protected void endOfData() {
        try {
            aida.saveAs(aidaFileName + "." + aidaFileType);
        } catch (IOException ex) {
            Logger.getLogger(DetailedAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setAidaFileName(String s) {
        aidaFileName = s;
    }

    public void setAidaFileType(String s) {
        aidaFileType = s;
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
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper()
                .getIdentifierDictionary();
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
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x"
                        + Long.toHexString(strippedId.getValue()) + ">.");
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
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x"
                        + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }

}
