package org.hps.recon.utils;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
//import hep.aida.ref.rootwriter.RootFileStore;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

import org.hps.recon.tracking.TrackData;
import org.hps.recon.ecal.cluster.ClusterUtilities;
//import org.hps.recon.particle.SimpleParticleID;



import org.lcsim.event.Cluster;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.LCRelation;
import org.lcsim.event.ReconstructedParticle;
//import org.lcsim.event.base.BaseReconstructedParticle;

import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.FieldMap;




//import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.CoordinateTransformations;

import org.hps.record.StandardCuts;

public abstract class AbstractTrackClusterMatcher implements TrackClusterMatcherInter {

    protected Map<String,Double> cutsMap = new HashMap<String,Double>();

    /**
     * Flag used to determine if plots are enabled/disabled
     */

    protected boolean useCorrectedClusterPositionsForMatching = false;
    protected boolean isMC = false;
    HPSEcal3 ecal;

    /**
     * The B field map
     */
    FieldMap bFieldMap = null;



    AbstractTrackClusterMatcher() {
    }

    public abstract void bookHistograms();

    // Plotting
    protected ITree tree = IAnalysisFactory.create().createTreeFactory().create();
    protected IHistogramFactory histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
    protected Map<String, IHistogram1D> plots1D = new HashMap<String, IHistogram1D>();
    protected Map<String, IHistogram2D> plots2D = new HashMap<String, IHistogram2D>();

    public abstract void saveHistograms();

    public abstract double getMatchQC(Cluster cluster, ReconstructedParticle particle);

    public abstract HashMap<Track,Cluster> matchTracksToClusters(EventHeader event, List<List<Track>> trackCollections, List<Cluster> clusters, StandardCuts cuts, int flipSign,boolean useCorrectedClusterPositions, HPSEcal3 ecal, boolean isMC);

    public abstract void initializeParameterization(String fname);

    public void setBFieldMap(FieldMap bFieldMap) {
        this.bFieldMap = bFieldMap;
    }

    private boolean snapToEdge = true;
    public void setSnapToEdge(boolean val){
        this.snapToEdge = val;
    }

    public abstract void enablePlots(boolean enablePlots);

    public boolean isInTime(EventHeader event, double trackClusterTimeOffset, Cluster cluster, Track track){

        double dt = getdt(event, trackClusterTimeOffset, cluster, track);
        if(cutsMap.isEmpty())
            return true;
        if(dt > cutsMap.get("T"))
            return false;
        else
            return true;

    }

    protected String trackCollectionName = "GBLTracks";
    public void setTrackCollectionName(String trackCollectionName){
        this.trackCollectionName = trackCollectionName;
    }



    public boolean isMatch(Cluster cluster, Track track){
    
        // Check that the track and cluster are in the same detector volume.
        // If not, there is no way they can be a match.
        if ((track.getTrackStates().get(0).getTanLambda() > 0 && cluster.getPosition()[1] < 0)
                || (track.getTrackStates().get(0).getTanLambda() < 0 && cluster.getPosition()[1] > 0)) {
            return false;
        }

        double [] delta = getDistanceXYZ(cluster, track);
        double deltaX = delta[0];
        double deltaY = delta[1];

        if(cutsMap.isEmpty())
            return true;
        if(deltaX > cutsMap.get("X"))
            return false;
        if(deltaY > cutsMap.get("Y"))
            return false;

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
        if(trackCollectionName.contains("GBLTracks")){
            trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);
        }       

        if(trackCollectionName.contains("KalmanFullTracks")){
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
        if(trackCollectionName.contains("GBLTracks")){
            trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);
        }       

        if(trackCollectionName.contains("KalmanFullTracks")){
            trackStateAtEcal = track.getTrackStates().get(track.getTrackStates().size()-1);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);

        }

        distance[0] = cPos.x() - tPos.x();
        distance[1] = cPos.y() - tPos.y();
        distance[2] = cPos.z() - tPos.z();
        
        return distance;
        
        

    }

    public double getdt(EventHeader event, double trackClusterTimeOffset, Cluster cluster, Track track){
        
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
        double dt = clustert - trackClusterTimeOffset - trackt;

        return dt;



    }

    public RelationalTable getKFTrackDataRelations(EventHeader event){
        
        List<TrackData> TrackData;
        RelationalTable trackToData = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trackRelations;
        TrackData trackdata;
        if (trackCollectionName.contains("KalmanFullTracks")) {
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