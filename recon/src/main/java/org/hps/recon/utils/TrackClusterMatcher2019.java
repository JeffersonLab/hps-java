package org.hps.recon.utils;


import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.ecal.cluster.ClusterCorrectionUtilities;

import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackData;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;
import java.io.IOException;



import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.Cluster;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.LCRelation;


import org.lcsim.geometry.subdetector.HPSEcal3;
import org.hps.record.StandardCuts;
import org.lcsim.event.ReconstructedParticle;

public class TrackClusterMatcher2019 extends AbstractTrackClusterMatcher{

    public TrackClusterMatcher2019() {
    }

    private String trackCollectionName;
    RelationalTable hitToRotated = null;
    RelationalTable hitToStrips = null;

    public void initializeParameterization(String fname){
        
    }

    public double getMatchQC(Cluster cluster, ReconstructedParticle particle){
        //match quality not yet defined for this matcher
        return -9999.9;
    }

    //clusterToTrack map must be filled inside of matcher, or cluster corrections will not be applied!
    protected HashMap<Cluster, Track> clusterToTrack;
    public void applyClusterCorrections(boolean useTrackPositionForClusterCorrection, List<Cluster> clusters, double beamEnergy, HPSEcal3 ecal, boolean isMC){

        // Apply the corrections to the Ecal clusters using track information, if available
        for (Cluster cluster : clusters) {
            if (cluster.getParticleId() != 0) {
                if (useTrackPositionForClusterCorrection && this.clusterToTrack.containsKey(cluster)) {
                    Track matchedT = clusterToTrack.get(cluster);
                    double ypos = TrackUtils.getTrackStateAtECal(matchedT).getReferencePoint()[2];
                    ClusterCorrectionUtilities.applyCorrections(beamEnergy, ecal, cluster, ypos, isMC);
                } else {
                    ClusterCorrectionUtilities.applyCorrections(beamEnergy, ecal, cluster, isMC);
                }
            }
        }

    }

    public HashMap<Track,Cluster> matchTracksToClusters(EventHeader event, List<List<Track>> trackCollections, List<Cluster> clusters, StandardCuts cuts, int flipSign, boolean useCorrectedClusterPositionsForMatching, boolean isMC, HPSEcal3 ecal, double beamEnergy){

        System.out.println("Under construction");
        return null;
    }


    boolean enablePlots = false;
    String rootFile = String.format("%s_TrackClusterMatching.root",this.trackCollectionName);
    public void enablePlots(boolean enablePlots) {
        this.enablePlots = enablePlots;
        if (enablePlots ==true) {
            this.bookHistograms();
        }
    }

     /**
     * Save the histograms to a ROO file
     */
    public void saveHistograms() {

        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Plotting
    protected ITree tree;
    protected IHistogramFactory histogramFactory;
    protected Map<String, IHistogram1D> plots1D;
    protected Map<String, IHistogram2D> plots2D;

    public void bookHistograms() {

        plots1D = new HashMap<String, IHistogram1D>();
        plots2D = new HashMap<String, IHistogram2D>();

        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        System.out.println("BOOKING HISTOGRAMS FOR " +  this.trackCollectionName);

        //Timing Plots
        plots1D.put(String.format("%s_ElectronTrack-Cluster_dt",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ElectronTrack-Cluster_dt",trackCollectionName),  200, -200, 200));
        plots1D.put(String.format("%s_PositronTrack-Cluster_dt",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_PositronTrack-Cluster_dt",trackCollectionName),  200, -200, 200));
        plots1D.put(String.format("%s_pos_track_time",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_time",trackCollectionName),  200, -200, 200));
        plots1D.put(String.format("%s_ele_track_time",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_time",trackCollectionName),  200, -200, 200));
        plots1D.put(String.format("%s_cluster_time_wOffset",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_cluster_time_wOffset",trackCollectionName),  200, -200, 200));

        //Track position at ecal
        plots1D.put(String.format("%s_pos_track_x",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_x",trackCollectionName),  1000, -500, 500));
        plots1D.put(String.format("%s_pos_track_y",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_y",trackCollectionName),  1000, -500, 500));
        plots1D.put(String.format("%s_pos_track_z",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_z",trackCollectionName),  1600, -100, 1500));

        plots1D.put(String.format("%s_ele_track_x",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_x",trackCollectionName),  1000, -500, 500));
        plots1D.put(String.format("%s_ele_track_y",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_y",trackCollectionName),  1000, -500, 500));
        plots1D.put(String.format("%s_ele_track_z",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_z",trackCollectionName),  1600, -100, 1500));

        //Track to Ecal Extrapolation
        plots1D.put(String.format("%s_pos_track_at_ecal_cluster_dx",trackCollectionName),histogramFactory.createHistogram1D(String.format("%s_pos_track_at_ecal_cluster_dx",trackCollectionName),400, -200, 200));
        plots1D.put(String.format("%s_pos_track_at_ecal_cluster_dy",trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_at_ecal_cluster_dy",trackCollectionName),400, -200, 200));
        plots1D.put(String.format("%s_pos_track_at_ecal_cluster_dz",trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_at_ecal_cluster_dz",trackCollectionName),100, -50,50));
        plots1D.put(String.format("%s_pos_track_at_ecal_cluster_dr",trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_at_ecal_cluster_dr",trackCollectionName),100, -50, 150));
        plots1D.put(String.format("%s_ele_track_at_ecal_cluster_dx",trackCollectionName),histogramFactory.createHistogram1D(String.format("%s_ele_track_at_ecal_cluster_dx",trackCollectionName),400, -200, 200));
        plots1D.put(String.format("%s_ele_track_at_ecal_cluster_dy",trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_at_ecal_cluster_dy",trackCollectionName),400, -200, 200));
        plots1D.put(String.format("%s_ele_track_at_ecal_cluster_dz",trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_at_ecal_cluster_dz",trackCollectionName),100, -50,50));
        plots1D.put(String.format("%s_ele_track_at_ecal_cluster_dr",trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_at_ecal_cluster_dr",trackCollectionName),100, -50,150));

        // Energy/Momentum plots
        plots1D.put(String.format("%s_ele_Track_Cluster_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_Track_Cluster_EdivP",trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_pos_Track_Cluster_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_Track_Cluster_EdivP",trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_ele_track_momentum",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_momentum",trackCollectionName),  100, 0, 5));
        plots1D.put(String.format("%s_pos_track_momentum",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_momentum",trackCollectionName),  100, 0, 5));
        plots1D.put(String.format("%s_cluster_energy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_cluster_energy",trackCollectionName),  100, 0, 5));
        plots1D.put(String.format("%s_corrected_cluster_energy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_corrected_cluster_energy",trackCollectionName),  100, 0, 5));
    }

    public void plotClusterValues(List<Cluster> clusters, double trackClusterTimeOffset){
        for(Cluster cluster : clusters) {
            double clusterEnergy = cluster.getEnergy();
            double cluster_time = ClusterUtilities.getSeedHitTime(cluster);
            plots1D.get(String.format("%s_cluster_energy",trackCollectionName)).fill(clusterEnergy);
            plots1D.get(String.format("%s_cluster_time_wOffset",trackCollectionName)).fill(cluster_time - trackClusterTimeOffset);
        }
    }

    public Map<Track,Cluster> trackClusterMatcher(List<Track> tracks, EventHeader event,  String trackCollectionName, List<Cluster> clusters, double trackClusterTimeOffset)  {

        //This method take as input a collection of Tracks, with track type
        //(GBL or KF) specified, EcalClusters, and the track-cluster time
        //offset
        //Tracks will be uniquely matched to the closest Ecal Cluster, if that
        //Track+Cluster pair is in time, and within hard-coded position
        //residual cuts
        //
        //If no cluster is matched to a Track, returns a null match to Track
        //
        //Method returns a Map of Tracks and matched Ecal Clusters

        //Map of position residuals between all track+cluster combinations
        if(tracks == null || tracks.isEmpty() || clusters == null || clusters.isEmpty()){
            return null;
        }

        Map<Track, Map<Cluster, Double>> trackClusterResidualsMap = new HashMap<Track, Map<Cluster, Double>>(); 

        //If Kalman Tracks
        //Relational Table is used to get KF track time from
        //KFTrackDataRelations
        hitToRotated = TrackUtils.getHitToRotatedTable(event);
        hitToStrips = TrackUtils.getHitToStripsTable(event);
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

        //Plot Cluster Energy
        if(enablePlots)
            plotClusterValues(clusters, trackClusterTimeOffset);

        //Loop over Tracks
        for(Track track : tracks) {

            //I've found that I need to multiply the charge by -1, but I don't
            //know why --alic
            int charge = -1* (int) Math.signum(track.getTrackStates().get(0).getOmega());

            //Track time includes a hard-coded offset
            double trackt;
            double tracktOffset = 3.5; //Track time distribution is centered on -4ns, added offset to center ~0

            //Track positions
            double trackx;
            double tracky;
            double trackz;
            double dxoffset; //Track x-position at Ecal distriution not centered on 0. Offset varies depending on charge

            double tanlambda = track.getTrackParameter(4);

            //Get Track position for GBL Tracks
            if (trackCollectionName.contains("GBLTracks")){
                trackt = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
                trackx = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[1]; 
                tracky = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[2];
                trackz = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[0];

                //electron GBLTracks show x position bias at +5.5 mm. Offset
                //accounted for below (IS THIS OKAY TO HARDCODE?)
                if(charge < 0)
                    dxoffset = -6.6;
                else
                    dxoffset = 1.75;
            }

            //Get Track position for KF Tracks
            else {
                trackdata = (TrackData) trackToData.from(track);
                trackt = trackdata.getTrackTime();
                //KF TrackState at ecal stored as the last TrackState in
                //KalmanInterface.java
                TrackState ts_ecal = track.getTrackStates().get(track.getTrackStates().size()-1); 
                double[] ts_ecalPos = ts_ecal.getReferencePoint();
                trackx = ts_ecalPos[1];
                tracky = ts_ecalPos[2];
                trackz = ts_ecalPos[0];
                if(charge < 0)
                    dxoffset = -4.8; //KF ele tracks have x-position bias of -3.3 mm, hardcode offset + 3.3
                else
                    dxoffset = 1.85; //Similar case as above 
            }

            //Track momentum magnitude
            //double[] trackP =  track.getTrackStates().get(track.getTrackStates().size()-1).getMomentum();
            double[] trackP = TrackUtils.getTrackStateAtLocation(track,TrackState.AtLastHit).getMomentum();
            double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
            
            /*
            //FEE's Only Cut
            if(trackPmag < 2.0 || trackPmag > 2.5)
                continue;
             */

            //Plots
            if(enablePlots){
                if (charge > 0) {
                    plots1D.get(String.format("%s_pos_track_time",trackCollectionName)).fill(trackt);
                    plots1D.get(String.format("%s_pos_track_momentum",trackCollectionName)).fill(trackPmag);
                    plots1D.get(String.format("%s_pos_track_x",trackCollectionName)).fill(trackx);
                    plots1D.get(String.format("%s_pos_track_y",trackCollectionName)).fill(tracky);
                    plots1D.get(String.format("%s_pos_track_z",trackCollectionName)).fill(trackz);
                }
                else {
                    plots1D.get(String.format("%s_ele_track_time",trackCollectionName)).fill(trackt);
                    plots1D.get(String.format("%s_ele_track_momentum",trackCollectionName)).fill(trackPmag);
                    plots1D.get(String.format("%s_ele_track_x",trackCollectionName)).fill(trackx);
                    plots1D.get(String.format("%s_ele_track_y",trackCollectionName)).fill(tracky);
                    plots1D.get(String.format("%s_ele_track_z",trackCollectionName)).fill(trackz);
                }
            }

            //Begin Cluster Matching Algorithm
            Map<Cluster, Double> cluster_dr_Map = new HashMap<Cluster, Double>();
            Cluster matchedCluster = null;

            //define time and position cuts for Track-Cluster matching,
            double tcut = 4.0;
            double xcut = 10.0;
            double ycut = 10.0;

            //Loop over all clusters, looking for best match to current track
            double smallestdt = Double.MAX_VALUE;
            double smallestdr = Double.MAX_VALUE;
            for(Cluster cluster : clusters) {
                double clusterEnergy = cluster.getEnergy();
                double cluster_time = ClusterUtilities.getSeedHitTime(cluster);
                double dt = cluster_time - trackClusterTimeOffset - trackt + tracktOffset;

                double dist = getDistanceR(cluster, track);

                double clusterx = cluster.getPosition()[0];
                double clustery = cluster.getPosition()[1];
                double clusterz = cluster.getPosition()[2];
                double dx = clusterx - trackx + dxoffset;
                double dy = clustery - tracky;
                double dz = clusterz - trackz;
                double dr = Math.sqrt(Math.pow(clusterx-trackx,2) + Math.pow(clustery-tracky,2));

                //Ecal fiduciary cuts
                //if(clusterx < 10 && charge > 0)
                //    continue;
                //if(clusterx > -10 && charge < 0)
                //    continue;
                if(clustery > 10 && tanlambda < 0)
                    continue;
                if(clustery < -10 && tanlambda > 0)
                    continue;


                //If position and time residual cuts are passed, build map of
                //all cluster position residuals with this track
                if((Math.abs(dt) < tcut) && (Math.abs(dx) < xcut) && (Math.abs(dy) < ycut) ) {
                    cluster_dr_Map.put(cluster, dr);
                }

                if(enablePlots) {
                    if((Math.abs(dt) < tcut) && (Math.abs(dx) < xcut) && (Math.abs(dy) < ycut) ) {
                        if(charge > 0) {
                            //Time residual plot
                            plots1D.get(String.format("%s_PositronTrack-Cluster_dt",trackCollectionName)).fill(dt);

                            //Kalman Extrapolated Residuals
                            plots1D.get(String.format("%s_pos_track_at_ecal_cluster_dx",trackCollectionName)).fill(dx);
                            plots1D.get(String.format("%s_pos_track_at_ecal_cluster_dy",trackCollectionName)).fill(dy);
                            plots1D.get(String.format("%s_pos_track_at_ecal_cluster_dz",trackCollectionName)).fill(dz);
                            plots1D.get(String.format("%s_pos_track_at_ecal_cluster_dr",trackCollectionName)).fill(dr);
                        }
                        else {

                            //Time residual plot
                            plots1D.get(String.format("%s_ElectronTrack-Cluster_dt",trackCollectionName)).fill(dt);

                            //Kalman Extrapolated Residuals
                            plots1D.get(String.format("%s_ele_track_at_ecal_cluster_dx",trackCollectionName)).fill(dx);
                            plots1D.get(String.format("%s_ele_track_at_ecal_cluster_dy",trackCollectionName)).fill(dy);
                            plots1D.get(String.format("%s_ele_track_at_ecal_cluster_dz",trackCollectionName)).fill(dz);
                            plots1D.get(String.format("%s_ele_track_at_ecal_cluster_dr",trackCollectionName)).fill(dr);
                        }
                    }
                }

            }

            //Position residual between Track and each potential Cluster is
            //stored in a map so that best match can be chosen
            trackClusterResidualsMap.put(track, cluster_dr_Map);
        }

        //Match Track to the closest Ecal Cluster
        //Check for Clusters that are matched to multiple Tracks, and sort
        //until only unique matches remain

        //minTrackClusterResidualMap maps Track to the Cluster with the smallest dr
        Map<Track,Cluster> minTrackClusterResidualMap = new HashMap<Track, Cluster>();

        for(int i=0; i < clusters.size(); i++){
            minTrackClusterResidualMap = getMinTrackClusterResidualMap(trackClusterResidualsMap);
            trackClusterResidualsMap = checkDuplicateClusterMatching(trackClusterResidualsMap,minTrackClusterResidualMap);
        }
        minTrackClusterResidualMap = getMinTrackClusterResidualMap(trackClusterResidualsMap);
        return minTrackClusterResidualMap;
    }


    public Map<Track, Map<Cluster,Double>> checkDuplicateClusterMatching(Map<Track, Map<Cluster,Double>> trackClusterResidualsMap, Map<Track, Cluster> minTrackClusterResidualMap){
        
        boolean duplicateCluster = false;
        List<Track> sharedClusterTracks = new ArrayList<Track>();
        List<Track> skipTracks = new ArrayList<Track>();
        
        for(Track track : minTrackClusterResidualMap.keySet()){
            Map<Track, Cluster> trackswDuplicateClusters = new HashMap<Track, Cluster>();
            if(skipTracks.contains(track))
                continue;
            Cluster smallestdrCluster = minTrackClusterResidualMap.get(track);
            if(smallestdrCluster == null)
                continue;
            for(Track otherTrack : minTrackClusterResidualMap.keySet()){
                if(skipTracks.contains(track))
                    continue;
                if(otherTrack == track)
                    continue;
                Cluster othersmallestdrCluster = minTrackClusterResidualMap.get(otherTrack);
                if(othersmallestdrCluster == smallestdrCluster)
                {
                    duplicateCluster = true;
                    trackswDuplicateClusters.put(track, smallestdrCluster);
                    trackswDuplicateClusters.put(otherTrack, othersmallestdrCluster);
                }
            }

            double smallestdr = 99999.0;
            Track smallestdrTrack = null;
            if(trackswDuplicateClusters == null)
                return trackClusterResidualsMap;
            for(Track duptrack : trackswDuplicateClusters.keySet()){
                double dr = trackClusterResidualsMap.get(duptrack).get(trackswDuplicateClusters.get(duptrack));
                if(dr < smallestdr){
                    smallestdr = dr;
                    smallestdrTrack = duptrack;
                }
            }
            for(Track duptrack : trackswDuplicateClusters.keySet()){
                skipTracks.add(duptrack);
                if(duptrack != smallestdrTrack){
                    trackClusterResidualsMap.get(duptrack).remove(trackswDuplicateClusters.get(duptrack));
                }
            }
        }
        return trackClusterResidualsMap;
    }

    public Map<Track,Cluster>  getMinTrackClusterResidualMap(Map<Track, Map<Cluster, Double>> trackClusterResidualsMap){

        //inputs a mapping of tracks with residuals for each possible cluster
        //from all clusters in the map, for each track, match the cluster with
        //the smallest position residual to that track
        //build output map of track -> closest cluster
        Map<Track,Cluster> Map = new HashMap<Track, Cluster>();
        for(Track track : trackClusterResidualsMap.keySet()){
            double smallestdr = 99999.0;
            Cluster smallestdrCluster = null;
            Map<Cluster, Double> cluster_dr_Map = trackClusterResidualsMap.get(track);
            for(Cluster c : cluster_dr_Map.keySet()){
                double dr = cluster_dr_Map.get(c);
                if(dr < smallestdr){
                    smallestdr = dr;
                    smallestdrCluster = c;
                }
            }

            

            Map.put(track, smallestdrCluster);
        }
        return Map;
        
    }


}
