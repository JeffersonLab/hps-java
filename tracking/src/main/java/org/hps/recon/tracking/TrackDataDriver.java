package org.hps.recon.tracking;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseRelationalTable;

/**
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 *
 */
public class TrackDataDriver extends Driver {

    // Collection Names
    String trackCollectionName = "MatchedTracks";
    // TODO: Change this to match whatever track name is decided on
    String trackTimeDataCollectionName = "TrackTimeData";
    String trackResidualsCollectionName = "TrackResiduals";
    String rotatedHthRelationsColName = "RotatedHelicalTrackHitRelations";
    String rotatedHthCollectionName = "RotatedHelicalTrackHits";
    String trackTimeDataRelationsColName = "TrackTimeDataRelations";
    String trackResidualsRelationsColName = "TrackResidualsRelations";

    public TrackDataDriver() {
    }
    
    public void setTrackCollectionName(String name){
        this.trackCollectionName=name;
    }

    protected void detectorChanged(Detector detector) {

        // TODO: Add plots of all track data variables
    }

    protected void process(EventHeader event) {

        // If the event doesn't contain a collection of tracks, skip it.
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            return;
        }

        // Get the collection of tracks from the event
        List<Track> tracks = event.get(Track.class, trackCollectionName);

        // Get the collection of LCRelations relating RotatedHelicalTrackHits to
        // HelicalTrackHits
        List<LCRelation> rotatedHthToHthRelations = event.get(LCRelation.class, rotatedHthRelationsColName);
        BaseRelationalTable hthToRotatedHth = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        hthToRotatedHth.addRelations(rotatedHthToHthRelations);

        List<HelicalTrackHit> rotatedHths = event.get(HelicalTrackHit.class, rotatedHthCollectionName);

        // Create a collection to hold the track time and t0 residual data
        List<TrackTimeData> timeDataCollection = new ArrayList<TrackTimeData>();

        // Create a collection of LCRelations between a track and the t0 residual data
        List<LCRelation> trackToTrackTimeDataRelations = new ArrayList<LCRelation>();

        // Create a collection to hold the track residuals
        List<TrackResidualsData> trackResidualsCollection = new ArrayList<TrackResidualsData>();

        // Create a collection of LCRelations between a track and the track residuals
        List<LCRelation> trackToTrackResidualsRelations = new ArrayList<LCRelation>();

        // Create a collection to hold the track residuals
        List<TrackQualityData> trackQualityCollection = new ArrayList<TrackQualityData>();

        // Create a collection of LCRelations between a track and the track residuals
        List<LCRelation> trackQualityDataToTrackRelations = new ArrayList<LCRelation>();

        double totalT0 = 0;
        double totalHits = 0;
        double trackTime = 0;
        double t0Residual = 0;
        double xResidual = 0;
        double yResidual = 0;
        float trackerVolume = -1;

        boolean isFirstHit = true;

        HpsSiSensor sensor = null;
        Hep3Vector stereoHitPosition = null;
        Hep3Vector trackPosition = null;
        HelicalTrackHit helicalTrackHit = null;

        List<Double> t0Residuals = new ArrayList<Double>();
        List<Double> trackResidualsX = new ArrayList<Double>();
        List<Float> trackResidualsY = new ArrayList<Float>();
        List<Integer> sensorLayers = new ArrayList<Integer>();
        List<Integer> stereoLayers = new ArrayList<Integer>();

        // Loop over all the tracks in the event
        for (Track track : tracks) {

            totalT0 = 0;
            totalHits = 0;
            t0Residuals.clear();
            sensorLayers.clear();
            trackResidualsX.clear();
            trackResidualsY.clear();
            stereoLayers.clear();

            //
            // Calculate the track time and track residuals. Also, change the 
            // position of a HelicalTrackHit to be the corrected one.
            //
            // Loop over all stereo hits comprising a track
            for (TrackerHit rotatedStereoHit : track.getTrackerHits()) {

                // Add the stereo layer number associated with the track residual
                stereoLayers.add(((HelicalTrackHit) rotatedStereoHit).Layer());

                // Extrapolate the track to the stereo hit position and calculate 
                // track residuals
                stereoHitPosition = ((HelicalTrackHit) rotatedStereoHit).getCorrectedPosition();
                trackPosition = TrackUtils.extrapolateTrack(track, stereoHitPosition.x());
                xResidual = trackPosition.x() - stereoHitPosition.y();
                yResidual = trackPosition.y() - stereoHitPosition.z();
                trackResidualsX.add(xResidual);
                trackResidualsY.add((float) yResidual);

                //
                // Change the persisted position of both RotatedHelicalTrackHits 
                // and HelicalTrackHits to the corrected position.
                //
                // Get the HelicalTrackHit corresponding to the RotatedHelicalTrackHit
                // associated with a track
                helicalTrackHit = (HelicalTrackHit) hthToRotatedHth.from(rotatedStereoHit);
                ((HelicalTrackHit) rotatedStereoHit).setPosition(stereoHitPosition.v());
                stereoHitPosition = CoordinateTransformations.transformVectorToDetector(stereoHitPosition);
                helicalTrackHit.setPosition(stereoHitPosition.v());

                // Loop over the clusters comprising the stereo hit
                for (HelicalTrackStrip cluster : ((HelicalTrackCross) rotatedStereoHit).getStrips()) {

                    totalT0 += cluster.time();
                    totalHits++;
                }
            }

            // The track time is the mean t0 of hits on a track
            trackTime = totalT0 / totalHits;

            //
            // Calculate the t0 residuals
            //
            isFirstHit = true;
            // Loop over all stereo hits comprising a track
            for (TrackerHit stereoHit : track.getTrackerHits()) {
                // Loop over the clusters comprising the stereo hit
                for (HelicalTrackStrip cluster : ((HelicalTrackCross) stereoHit).getStrips()) {

                    if (isFirstHit) {
                        sensor = (HpsSiSensor) ((RawTrackerHit) cluster.rawhits().get(0)).getDetectorElement();
                        if (sensor.isTopLayer()) {
                            trackerVolume = 0;
                        } else if (sensor.isBottomLayer()) {
                            trackerVolume = 1;
                        }
                    }

                    // Add the layer number associated with this residual to the list of layers
                    sensorLayers.add(sensor.getLayerNumber());

                    // Find the t0 residual and add it to the list of residuals
                    t0Residual = trackTime - cluster.time();
                    // Apply correction to t0 residual
                    t0Residual /= Math.sqrt((totalHits - 1) / totalHits);
                    t0Residuals.add(t0Residual);
                }
            }

            double l1Isolation = 99999999.0;
            double l2Isolation = 99999999.0;
            // Loop over all stereo hits comprising a track
            for (TrackerHit stereoHit : track.getTrackerHits()) {
                // Loop over the clusters comprising the stereo hit
                for (HelicalTrackStrip cluster : ((HelicalTrackCross) stereoHit).getStrips()) {
//                    System.out.println(cluster.layer());
                    if (cluster.layer() == 1) {
                        l1Isolation = getNearestDistance(cluster, rotatedHths);
                    } else if (cluster.layer() == 2) {
                        l2Isolation = getNearestDistance(cluster, rotatedHths);
                    }
                }
            }

            TrackTimeData timeData = new TrackTimeData(trackerVolume, trackTime, sensorLayers, t0Residuals);
            timeDataCollection.add(timeData);
            trackToTrackTimeDataRelations.add(new BaseLCRelation(timeData, track));

            TrackResidualsData trackResiduals = new TrackResidualsData((int) trackerVolume, stereoLayers, trackResidualsX, trackResidualsY);
            trackResidualsCollection.add(trackResiduals);
            trackToTrackResidualsRelations.add(new BaseLCRelation(trackResiduals, track));

            double qualityArray[] = {l1Isolation, l2Isolation};
            TrackQualityData qualityData = new TrackQualityData(qualityArray);
            trackQualityCollection.add(qualityData);
            trackQualityDataToTrackRelations.add(new BaseLCRelation(qualityData, track));
        }

        event.put(trackTimeDataCollectionName, timeDataCollection, TrackTimeData.class, 0);
        event.put(trackTimeDataRelationsColName, trackToTrackTimeDataRelations, LCRelation.class, 0);
        event.put(trackResidualsCollectionName, trackResidualsCollection, TrackResidualsData.class, 0);
        event.put(trackResidualsRelationsColName, trackToTrackResidualsRelations, LCRelation.class, 0);
        event.put(TrackQualityData.QUALITY_COLLECTION, trackQualityCollection, TrackResidualsData.class, 0);
        event.put(TrackQualityData.QUALITY_RELATION_COLLECTION, trackQualityDataToTrackRelations, LCRelation.class, 0);

    }

    private static Double getNearestDistance(HelicalTrackStrip cl, List<HelicalTrackHit> toththits) {
        Hep3Vector corigin = cl.origin();
        Hep3Vector uvec = VecOp.mult(cl.umeas(), cl.u());
        Hep3Vector clvec = VecOp.add(corigin, uvec);
        int layer = cl.layer();
//        HelicalTrackStrip nearest = null;
//        Hep3Vector nearestvec = null;
        Double mindist = 99999999.0;
        for (HelicalTrackHit hth : toththits) {
            HelicalTrackCross cross = (HelicalTrackCross) hth;
            for (HelicalTrackStrip str : cross.getStrips()) {
                if (layer == str.layer() && str != cl) {
                    Hep3Vector strorigin = str.origin();
                    Hep3Vector struvec = VecOp.mult(str.umeas(), str.u());
                    Hep3Vector strvec = VecOp.add(strorigin, struvec);
                    double origindist = VecOp.sub(corigin, strorigin).magnitude();
                    double dist = VecOp.sub(clvec, strvec).magnitude();
                    if (origindist == 0.0 && dist != 0.0 && dist < Math.abs(mindist)) {
                        mindist = VecOp.sub(clvec, strvec).magnitude();
                        if (Math.abs(clvec.z()) > Math.abs(strvec.z())) {
                            mindist = -mindist;
                        }
//                        nearest = str;
//                        nearestvec = strvec;
                    }
                }
            }
        }
//        System.out.println(clvec);
//        System.out.println(nearestvec);
        return mindist;
    }
}
