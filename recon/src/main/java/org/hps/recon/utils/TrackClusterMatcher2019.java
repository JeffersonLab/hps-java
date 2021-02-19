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
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.subdetector.HPSEcal3;



import org.hps.record.StandardCuts;
import org.lcsim.event.ReconstructedParticle;

public class TrackClusterMatcher2019 extends AbstractTrackClusterMatcher{

    public TrackClusterMatcher2019() {
    }

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

        HashMap<Track,Cluster> trackClusterPairs = this.trackClusterMatcher(trackCollections, event, clusters, cuts, flipSign, useCorrectedClusterPositionsForMatching, isMC, ecal, beamEnergy);
        return trackClusterPairs;
    }


    boolean enablePlots = false;
    String rootFile = String.format("%s_TrackClusterMatching.root",super.trackCollectionName);
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

        System.out.println("BOOKING HISTOGRAMS FOR " +  super.trackCollectionName);

        //Timing Plots
        plots1D.put(String.format("%s_ElectronTrack-Cluster_dt",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ElectronTrack-Cluster_dt",super.trackCollectionName),  200, -200, 200));
        plots1D.put(String.format("%s_PositronTrack-Cluster_dt",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_PositronTrack-Cluster_dt",super.trackCollectionName),  200, -200, 200));
        plots1D.put(String.format("%s_pos_track_time",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_time",super.trackCollectionName),  200, -200, 200));
        plots1D.put(String.format("%s_ele_track_time",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_time",super.trackCollectionName),  200, -200, 200));
        plots1D.put(String.format("%s_cluster_time_wOffset",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_cluster_time_wOffset",super.trackCollectionName),  200, -200, 200));

        //Track position at ecal
        plots1D.put(String.format("%s_pos_track_x",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_x",super.trackCollectionName),  1000, -500, 500));
        plots1D.put(String.format("%s_pos_track_y",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_y",super.trackCollectionName),  1000, -500, 500));
        plots1D.put(String.format("%s_pos_track_z",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_z",super.trackCollectionName),  1600, -100, 1500));

        plots1D.put(String.format("%s_ele_track_x",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_x",super.trackCollectionName),  1000, -500, 500));
        plots1D.put(String.format("%s_ele_track_y",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_y",super.trackCollectionName),  1000, -500, 500));
        plots1D.put(String.format("%s_ele_track_z",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_z",super.trackCollectionName),  1600, -100, 1500));

        //Track to Ecal Extrapolation
        plots1D.put(String.format("%s_pos_track_at_ecal_cluster_dx",super.trackCollectionName),histogramFactory.createHistogram1D(String.format("%s_pos_track_at_ecal_cluster_dx",super.trackCollectionName),400, -200, 200));
        plots1D.put(String.format("%s_pos_track_at_ecal_cluster_dy",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_at_ecal_cluster_dy",super.trackCollectionName),400, -200, 200));
        plots1D.put(String.format("%s_pos_track_at_ecal_cluster_dz",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_at_ecal_cluster_dz",super.trackCollectionName),100, -50,50));
        plots1D.put(String.format("%s_pos_track_at_ecal_cluster_dr",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_at_ecal_cluster_dr",super.trackCollectionName),100, -50, 150));
        plots1D.put(String.format("%s_ele_track_at_ecal_cluster_dx",super.trackCollectionName),histogramFactory.createHistogram1D(String.format("%s_ele_track_at_ecal_cluster_dx",super.trackCollectionName),400, -200, 200));
        plots1D.put(String.format("%s_ele_track_at_ecal_cluster_dy",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_at_ecal_cluster_dy",super.trackCollectionName),400, -200, 200));
        plots1D.put(String.format("%s_ele_track_at_ecal_cluster_dz",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_at_ecal_cluster_dz",super.trackCollectionName),100, -50,50));
        plots1D.put(String.format("%s_ele_track_at_ecal_cluster_dr",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_at_ecal_cluster_dr",super.trackCollectionName),100, -50,150));

        // Energy/Momentum plots
        plots1D.put(String.format("%s_ele_Track_Cluster_EdivP",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_Track_Cluster_EdivP",super.trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_pos_Track_Cluster_EdivP",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_Track_Cluster_EdivP",super.trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_ele_track_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_momentum",super.trackCollectionName),  100, 0, 5));
        plots1D.put(String.format("%s_pos_track_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_momentum",super.trackCollectionName),  100, 0, 5));
        plots1D.put(String.format("%s_cluster_energy",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_cluster_energy",super.trackCollectionName),  100, 0, 5));
        plots1D.put(String.format("%s_corrected_cluster_energy",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_corrected_cluster_energy",super.trackCollectionName),  100, 0, 5));
    }

    public void plotClusterValues(List<Cluster> clusters, double trackClusterTimeOffset){
        for(Cluster cluster : clusters) {
            double clusterEnergy = cluster.getEnergy();
            double cluster_time = ClusterUtilities.getSeedHitTime(cluster);
            plots1D.get(String.format("%s_cluster_energy",super.trackCollectionName)).fill(clusterEnergy);
            plots1D.get(String.format("%s_cluster_time_wOffset",super.trackCollectionName)).fill(cluster_time - trackClusterTimeOffset);
        }
    }

    public HashMap<Track,Cluster> trackClusterMatcher(List<List<Track>> trackCollections, EventHeader event, List<Cluster> clusters, StandardCuts cuts, int flipSign, boolean useCorrectedClusterPositionsForMatching, boolean isMC, HPSEcal3 ecal, double beamEnergy)  {

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
        //
        // Create a mapping of matched clusters to corresponding tracks.
        this.clusterToTrack = new HashMap<Cluster, Track>();

        // Create a mapping of Tracks and their corresponding Clusters.
        HashMap<Track, Cluster> alltrackClusterPairs = new HashMap<Track, Cluster>();
        System.out.println("TrackClusterTimeOffset: " + cuts.getTrackClusterTimeOffset());

        //If Kalman Tracks
        //Relational Table is used to get KF track time from
        //KFTrackDataRelations
        hitToRotated = TrackUtils.getHitToRotatedTable(event);
        hitToStrips = TrackUtils.getHitToStripsTable(event);
        List<TrackData> TrackData;
        RelationalTable trackToData = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trackRelations;
        TrackData trackdata;
        if (super.trackCollectionName.contains("KalmanFullTracks")) {
            TrackData = event.get(TrackData.class, "KFTrackData");
            trackRelations = event.get(LCRelation.class, "KFTrackDataRelations");
            for (LCRelation relation : trackRelations) {
                if (relation != null && relation.getTo() != null){
                    trackToData.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        System.out.println("Size of tracks in ecal: " + trackCollections.get(0).size());


        //Map of position residuals between all track+cluster combinations
        for (List<Track> tracks : trackCollections) {


            //Map of position residuals between all track+cluster combinations
            //if(tracks == null || tracks.isEmpty() || clusters == null || clusters.isEmpty()){
              //  continue;
            //}

            Map<Track, Map<Cluster, Double>> trackClusterResidualsMap = new HashMap<Track, Map<Cluster, Double>>(); 

            //Loop over Tracks
            for(Track track : tracks) {

                 // Derive the charge of the particle from the track.
                int charge = (int) Math.signum(track.getTrackStates().get(0).getOmega())*flipSign;

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
                if (super.trackCollectionName.contains("GBLTracks")){
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
                        plots1D.get(String.format("%s_pos_track_time",super.trackCollectionName)).fill(trackt);
                        plots1D.get(String.format("%s_pos_track_momentum",super.trackCollectionName)).fill(trackPmag);
                        plots1D.get(String.format("%s_pos_track_x",super.trackCollectionName)).fill(trackx);
                        plots1D.get(String.format("%s_pos_track_y",super.trackCollectionName)).fill(tracky);
                        plots1D.get(String.format("%s_pos_track_z",super.trackCollectionName)).fill(trackz);
                    }
                    else {
                        plots1D.get(String.format("%s_ele_track_time",super.trackCollectionName)).fill(trackt);
                        plots1D.get(String.format("%s_ele_track_momentum",super.trackCollectionName)).fill(trackPmag);
                        plots1D.get(String.format("%s_ele_track_x",super.trackCollectionName)).fill(trackx);
                        plots1D.get(String.format("%s_ele_track_y",super.trackCollectionName)).fill(tracky);
                        plots1D.get(String.format("%s_ele_track_z",super.trackCollectionName)).fill(trackz);
                    }
                }

                //Begin Cluster Matching Algorithm
                Map<Cluster, Double> cluster_dr_Map = new HashMap<Cluster, Double>();
                Cluster matchedCluster = null;

                //define time and position cuts for Track-Cluster matching,
                double tcut = 4.0;
                double xcut = 15.0;
                double ycut = 15.0;

                //Loop over all clusters, looking for best match to current track
                double smallestdt = Double.MAX_VALUE;
                double smallestdr = Double.MAX_VALUE;
                for(Cluster cluster : clusters) {
                    //if the option to use corrected cluster positions is selected, then
                    //create a copy of the current cluster, and apply corrections to it
                    //before calculating nsigma.  Default is don't use corrections.  
                    Cluster originalcluster = cluster;
                    if (useCorrectedClusterPositionsForMatching) {
                        BaseCluster clusterBase = new BaseCluster(cluster);
                        clusterBase.setNeedsPropertyCalculation(false);
                        cluster = clusterBase;
                        double ypos = tracky;
                        ClusterCorrectionUtilities.applyCorrections(beamEnergy, ecal, cluster, ypos, isMC);
                    }

                    double clusterEnergy = cluster.getEnergy();
                    double cluster_time = ClusterUtilities.getSeedHitTime(cluster);
                    double dt = cluster_time - cuts.getTrackClusterTimeOffset() - trackt + tracktOffset;
                    System.out.println("dt: " + dt);

                    double dist = getDistanceR(cluster, track);

                    double clusterx = cluster.getPosition()[0];
                    double clustery = cluster.getPosition()[1];
                    double clusterz = cluster.getPosition()[2];
                    double dx = clusterx - trackx + dxoffset;
                    double dy = clustery - tracky;
                    double dz = clusterz - trackz;
                    double dr = Math.sqrt(Math.pow(clusterx-trackx,2) + Math.pow(clustery-tracky,2));
                    System.out.println("dx: " + dx);
                    System.out.println("dy: " + dy);

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
                        System.out.println("Track paired with Cluster");
                        cluster_dr_Map.put(originalcluster, dr);
                    }

                    if(enablePlots) {
                        if((Math.abs(dt) < tcut) && (Math.abs(dx) < xcut) && (Math.abs(dy) < ycut) ) {
                            if(charge > 0) {
                                //Time residual plot
                                plots1D.get(String.format("%s_PositronTrack-Cluster_dt",super.trackCollectionName)).fill(dt);

                                //Kalman Extrapolated Residuals
                                plots1D.get(String.format("%s_pos_track_at_ecal_cluster_dx",super.trackCollectionName)).fill(dx);
                                plots1D.get(String.format("%s_pos_track_at_ecal_cluster_dy",super.trackCollectionName)).fill(dy);
                                plots1D.get(String.format("%s_pos_track_at_ecal_cluster_dz",super.trackCollectionName)).fill(dz);
                                plots1D.get(String.format("%s_pos_track_at_ecal_cluster_dr",super.trackCollectionName)).fill(dr);
                            }
                            else {

                                //Time residual plot
                                plots1D.get(String.format("%s_ElectronTrack-Cluster_dt",super.trackCollectionName)).fill(dt);

                                //Kalman Extrapolated Residuals
                                plots1D.get(String.format("%s_ele_track_at_ecal_cluster_dx",super.trackCollectionName)).fill(dx);
                                plots1D.get(String.format("%s_ele_track_at_ecal_cluster_dy",super.trackCollectionName)).fill(dy);
                                plots1D.get(String.format("%s_ele_track_at_ecal_cluster_dz",super.trackCollectionName)).fill(dz);
                                plots1D.get(String.format("%s_ele_track_at_ecal_cluster_dr",super.trackCollectionName)).fill(dr);
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

            //trackClusterPairs maps Track to the Cluster with the smallest dr
            Map<Track,Cluster> trackClusterPairs = new HashMap<Track, Cluster>();

            for(int i=0; i < clusters.size(); i++){
                trackClusterPairs = getMinTrackClusterResidualMap(trackClusterResidualsMap);
                trackClusterResidualsMap = checkDuplicateClusterMatching(trackClusterResidualsMap,trackClusterPairs);
            }
            trackClusterPairs = getMinTrackClusterResidualMap(trackClusterResidualsMap);
            //add Track Cluster pairs for this trackCollection to map for all trackCollections
            for(Map.Entry<Track, Cluster> entry : trackClusterPairs.entrySet()){
                alltrackClusterPairs.put(entry.getKey(), entry.getValue());
            }

        }
        return alltrackClusterPairs;
    }


    public Map<Track, Map<Cluster,Double>> checkDuplicateClusterMatching(Map<Track, Map<Cluster,Double>> trackClusterResidualsMap, Map<Track, Cluster> trackClusterPairs){
        
        boolean duplicateCluster = false;
        List<Track> sharedClusterTracks = new ArrayList<Track>();
        List<Track> skipTracks = new ArrayList<Track>();
        
        for(Track track : trackClusterPairs.keySet()){
            Map<Track, Cluster> trackswDuplicateClusters = new HashMap<Track, Cluster>();
            if(skipTracks.contains(track))
                continue;
            Cluster smallestdrCluster = trackClusterPairs.get(track);
            if(smallestdrCluster == null)
                continue;
            for(Track otherTrack : trackClusterPairs.keySet()){
                if(skipTracks.contains(track))
                    continue;
                if(otherTrack == track)
                    continue;
                Cluster othersmallestdrCluster = trackClusterPairs.get(otherTrack);
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
                    // prefer using GBL tracks to correct (later) the clusters, for some consistency:
                    if (track.getType() >= 32 || !this.clusterToTrack.containsKey(smallestdrCluster)) {
                        this.clusterToTrack.put(smallestdrCluster, track);
                    }

                    smallestdr = dr;
                    smallestdrCluster = c;
                }
            }

            

            Map.put(track, smallestdrCluster);
        }
        return Map;
        
    }


}
