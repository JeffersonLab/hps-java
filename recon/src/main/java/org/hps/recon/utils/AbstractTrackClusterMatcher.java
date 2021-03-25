package org.hps.recon.utils;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.event.Cluster;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.LCRelation;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.FieldMap;
import java.util.HashMap;
import java.util.List;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.record.StandardCuts;

/**
 * This is an abstract class that {@link TrackClusterMatcher} classes should
 * implement to match Tracks to Ecal Clusters in reconstruction.
 * The sub-class should implement {@link #matchTracksToClusters(args)} which
 * returns a map of Tracks matched with Ecal Clusters.
 * The sub-class should also implement the following abstract methods, as they
 * are called during reconstruction:
 * enablePlots, bookHistograms,saveHistograms, getMatchQC,
 * applyClusterCorrections, initializeParameterization
 */

/* 
 * @see TrackClusterMatcher
 */

public abstract class AbstractTrackClusterMatcher implements TrackClusterMatcher {

    protected String trackCollectionName = "GBLTracks";
    protected FieldMap bFieldMap = null;
    protected boolean snapToEdge = true;

    /*
     * Default no-arg constructor.
     */
    AbstractTrackClusterMatcher() {
        //Do nothing
    }

    /*Set track collection name. Used to discriminate between KF and GBL tracks
     * in matcher algorithms that extend this class.
     * Default is GBLTracks.
     */
    public void setTrackCollectionName(String trackCollectionName){
        this.trackCollectionName = trackCollectionName;
    }

    /**
     * Abstract methods for producing plots
     */
    public abstract void enablePlots(boolean enablePlots);

    public abstract void bookHistograms();

    public abstract void saveHistograms();

    /**
     * Abstract method for defining a Track Cluster match quality value. Used
     * to give particle id goodness of fit.
     */
    public abstract double getMatchQC(Cluster cluster, ReconstructedParticle particle);

    /**
     * Abstract method to set beam energy
     */
    public abstract void setBeamEnergy(double beamEnergy);

    /**
     * Abstract method to apply Cluster corrections after Track to Cluster matching, if true.
     */
    public abstract void applyClusterCorrections(boolean useTrackPositionClusterCorrection, List<Cluster> clusters, double beamEnergy, HPSEcal3 ecal, boolean isMC);

    /**
     * Abstract method that runs the Track Cluster matching algorithm. Returns
     * Map of Tracks matched to Clusters.
     */
    public abstract HashMap<Track,Cluster> matchTracksToClusters(EventHeader event, List<Track> tracks, List<Cluster> clusters, StandardCuts cuts, int flipSign,boolean useCorrectedClusterPositions, boolean isMC, HPSEcal3 ecal, double beamEnergy);

    /**
     * Abstract method that initializes misc parameterization file used for
     * Cluster parameters. Not always required by matcher extensions/
     */
    public abstract void initializeParameterization(String fname);

    /**
     * Set B field map if used in matching.
     */
    public void setBFieldMap(FieldMap bFieldMap) {
        this.bFieldMap = bFieldMap;
    }

    /**
     * Set snapToEdge if used in matching alg.
     */
    public void setSnapToEdge(boolean val){
        this.snapToEdge = val;
    }

    /**
     * Defined below are generic methods that may be useful for developing
     * matching algorithms
     */

    public boolean isInTime(EventHeader event, StandardCuts cuts, Cluster cluster, Track track){

        double trackt;    
        if(this.trackCollectionName.contains("GBLTracks")){
            RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
            RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
            trackt = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
        }
        else{ //(this.trackCollectionName.contains("KalmanFullTracks")){
            RelationalTable trackToData = getKFTrackDataRelations(event);
            TrackData trackdata = (TrackData) trackToData.from(track);
            trackt = trackdata.getTrackTime();
        }
        double clustert = ClusterUtilities.getSeedHitTime(cluster);
        if (Math.abs(clustert - trackt - cuts.getTrackClusterTimeOffset()) > cuts.getMaxMatchDt())
            return false;
        else
            return true;
    }

    public double getDistanceR(Cluster cluster, Track track){

        // Get the cluster position
        Hep3Vector cPos = new BasicHep3Vector(cluster.getPosition());

        // Extrapolate the track to the Ecal cluster position
        Hep3Vector tPos = null;
        TrackState trackStateAtEcal = null;
        //if (this.useAnalyticExtrapolator) {
            //tPos = TrackUtils.extrapolateTrack(track, cPos.z());
        //} 
        if(this.trackCollectionName.contains("GBLTracks")){
            trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);
        }       

        if(this.trackCollectionName.contains("KalmanFullTracks")){
            trackStateAtEcal = track.getTrackStates().get(track.getTrackStates().size()-1);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);

        }

        return Math.sqrt(Math.pow(cPos.x()-tPos.x(),2)+Math.pow(cPos.y()-tPos.y(),2));
    }

    public double[] getDistanceXYZ(Cluster cluster, Track track){

        //init distance array
        double[] distance = null;

        // Get the cluster position
        Hep3Vector cPos = new BasicHep3Vector(cluster.getPosition());

        // Extrapolate the track to the Ecal cluster position
        Hep3Vector tPos = null;
        TrackState trackStateAtEcal = null;
        if(this.trackCollectionName.contains("GBLTracks")){
            trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);
        }       

        if(this.trackCollectionName.contains("KalmanFullTracks")){
            trackStateAtEcal = track.getTrackStates().get(track.getTrackStates().size()-1);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);

        }

        distance[0] = cPos.x() - tPos.x();
        distance[1] = cPos.y() - tPos.y();
        distance[2] = cPos.z() - tPos.z();
        
        return distance;
    }

    public double getdt(EventHeader event, StandardCuts cuts, Cluster cluster, Track track){
        
        double trackt;    
        if(this.trackCollectionName.contains("GBLTracks")){
            RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
            RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
            trackt = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
        }
        else{ //(this.trackCollectionName.contains("KalmanFullTracks")){
            RelationalTable trackToData = getKFTrackDataRelations(event);
            TrackData trackdata = (TrackData) trackToData.from(track);
            trackt = trackdata.getTrackTime();
        }

        double clustert = ClusterUtilities.getSeedHitTime(cluster);
        double dt = clustert - cuts.getTrackClusterTimeOffset() - trackt;

        return dt;
    }

    public double getTrackTime(Track track, EventHeader event){

        double trackt;    
        if(this.trackCollectionName.contains("GBLTracks")){
            RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
            RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
            trackt = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
        }
        else{ //(this.trackCollectionName.contains("KalmanFullTracks")){
            RelationalTable trackToData = getKFTrackDataRelations(event);
            TrackData trackdata = (TrackData) trackToData.from(track);
            trackt = trackdata.getTrackTime();
        }

        return trackt;
    }

    public double[] getTrackPositionAtEcal(Track track){

        // Extrapolate the track to the Ecal cluster position
        Hep3Vector tPos = null;
        TrackState trackStateAtEcal = null;
        if(this.trackCollectionName.contains("GBLTracks")){
            trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);
        }       

        if(this.trackCollectionName.contains("KalmanFullTracks")){
            trackStateAtEcal = track.getTrackStates().get(track.getTrackStates().size()-1);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);

        }

        double[] trackPosition = {tPos.x(),tPos.y(),tPos.z()};
        return trackPosition;
    }

    public RelationalTable getKFTrackDataRelations(EventHeader event){
        
        List<TrackData> TrackData;
        RelationalTable trackToData = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trackRelations;
        TrackData trackdata;
        if (this.trackCollectionName.contains("KalmanFullTracks")) {
            TrackData = event.get(TrackData.class, "KFTrackData");
            trackRelations = event.get(LCRelation.class, "KFTrackDataRelations");
            for (LCRelation relation : trackRelations) {
                if (relation != null && relation.getTo() != null){
                    trackToData.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        return trackToData;
    }
}
