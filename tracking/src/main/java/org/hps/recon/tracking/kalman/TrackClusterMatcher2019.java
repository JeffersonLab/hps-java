package org.hps.recon.tracking.kalman;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackData;

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

public class TrackClusterMatcher2019 {


    //Used for plots
    boolean enablePlots = false;
    private ITree tree;
    private IHistogramFactory histogramFactory;
    private Map<String, IHistogram1D> plots1D;
    private String trackCollectionName;

    RelationalTable hitToRotated = null;
    RelationalTable hitToStrips = null;

    public TrackClusterMatcher2019(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
        System.out.println("Matching " + trackCollectionName + "to Ecal Clusters");
    }

    public void enablePlots(boolean enablePlots) {
        this.enablePlots = enablePlots;
        if (enablePlots ==true) {
            this.bookHistograms();
        }
    }

    public void saveHistograms() {
        System.out.println("Saving Histogram for " + this.trackCollectionName);
        String rootFile = String.format("%s_TrackClusterMatching.root",this.trackCollectionName);
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void bookHistograms() {

        System.out.println("BOOKING HISTOGRAMS FOR " +  this.trackCollectionName);
        plots1D = new HashMap<String, IHistogram1D>();
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

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

    public void plotCorrectedClusterValues(List<Cluster> clusters, double trackClusterTimeOffset){
        for(Cluster cluster : clusters) {
            double clusterEnergy = cluster.getEnergy();
            double cluster_time = ClusterUtilities.getSeedHitTime(cluster);
            plots1D.get(String.format("%s_corrected_cluster_energy",trackCollectionName)).fill(clusterEnergy);
        }
    }
    public Map<Track,Cluster> trackClusterMatcher(List<Track> tracks, EventHeader event,  String trackCollectionName, List<Cluster> clusters, double trackClusterTimeOffset)  {

        //Input collection of Tracks, with trackCollectionName, and collection
        //of Ecal Clusters
        //Method matches unique Ecal clusters to Tracks based on closest
        //distance, within a specific time window
        //Output is a map between Tracks and matched Cluster
        //If no cluster is matched to a Track, Map contains Track + Null
        //cluster

        //Map of position residuals between all track+cluster combinations
        System.out.println("Tracks passed to matcher: " + tracks.size());
        System.out.println("Clusters passed to matcher: " + clusters.size());
        if(tracks == null || tracks.isEmpty() || clusters == null || clusters.isEmpty()){
            return null;
        }

        Map<Track, Map<Cluster, Double>> trackClusterResidualsMap = new HashMap<Track, Map<Cluster, Double>>(); 

        //Relation Table required to retrieve kalman track time through
        //TrackData class
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

        //Cluster Plots
        if(enablePlots)
            plotClusterValues(clusters, trackClusterTimeOffset);

        for(Track track : tracks) {

            //charge sign must be flipped by factor of -1 (WHY!?)
            int charge = -1* (int) Math.signum(track.getTrackStates().get(0).getOmega());
            double trackt;
            double tracktOffset = 3.5; //Track time distribution is centered on -4ns, added offset to center ~0

            double trackx;
            double tracky;
            double trackz;
            double dxoffset; //Track x-position at Ecal distriution not centered on 0. Offset varies depending on charge

            //Track parameters that are useful for implementing cuts on
            //potential cluster matches
            double tanlambda = track.getTrackParameter(4);

            if (trackCollectionName.contains("GBLTracks")){
                trackt = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
                trackx = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[1]; 
                tracky = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[2];
                trackz = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[0];

                //electron GBLTracks show x position bias at +5.5 mm. Offset
                //accounted for below (IS THIS OKAY TO HARDCODE?)
                if(charge < 0)
                    dxoffset = 0.0; //-6.6;
                else
                    dxoffset = 0.0; //1.75;
            }

            //KFTracks
            else {
                trackdata = (TrackData) trackToData.from(track);
                trackt = trackdata.getTrackTime();
                //KF TrackState at ecal stored as the last TrackState in
                //KalmanInterface.java
                TrackState ts_ecal = track.getTrackStates().get(track.getTrackStates().size()-1); //Be careful about the coordinate frame used for this track state. It is different between current master and pass1-dev-fix branches.
                //trackP = track.getTrackStates().get(0).getMomentum(); 
                double[] ts_ecalPos = ts_ecal.getReferencePoint();
                trackx = ts_ecalPos[1];
                tracky = ts_ecalPos[2];
                trackz = ts_ecalPos[0];
                if(charge < 0)
                    dxoffset = 0.0; // -4.8; //KF ele tracks have x-position bias of -3.3 mm, hardcode offset + 3.3
                else
                    dxoffset = 0.0; //1.85; //Similar case as above 
            }

            //Track momentum magnitude
            double[] trackP = track.getMomentum();
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
            //determined by distributions
            double smallestdt = Double.MAX_VALUE;
            double smallestdr = Double.MAX_VALUE;
            double tcut = 4.0;
            double xcut = 10.0;
            double ycut = 10.0;

            //Loop over all clusters, looking for best match to current track
            for(Cluster cluster : clusters) {
                double clusterEnergy = cluster.getEnergy();
                double cluster_time = ClusterUtilities.getSeedHitTime(cluster);
                double dt = cluster_time - trackClusterTimeOffset - trackt + tracktOffset;

                double clusterx = cluster.getPosition()[0];
                double clustery = cluster.getPosition()[1];
                double clusterz = cluster.getPosition()[2];
                double dx = clusterx - trackx + dxoffset;
                double dy = clustery - tracky;
                double dz = clusterz - trackz;
                double dr = Math.sqrt(Math.pow(clusterx-trackx,2) + Math.pow(clustery-tracky,2));

                //Ecal fiducial cuts
                if(clusterx < 0 && charge > 0)
                    continue;
                if(clusterx > 0 && charge < 0)
                    continue;
                if(clustery > 0 && tanlambda < 0)
                    continue;
                if(clustery < 0 && tanlambda > 0)
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

            //trackClusterResidualsMap is a map of Tracks to the position residual of
            //each potential cluster match
            trackClusterResidualsMap.put(track, cluster_dr_Map);


            /*
            //In order to apply cluster corrections, clusters need to be assigned particle IDs. 
            //For all clusters possibly matched with this track, assign particle ID based on track, and check that E/P ~1
            ReconstructedParticle particle = new BaseReconstructedParticle();
            particle.addTrack(track);
            // Set the type of the particle. This is used to identify
            // the tracking strategy used in finding the track associated with
            // this particle.
            ((BaseReconstructedParticle) particle).setType(track.getType());

            // Derive the charge of the particle from the track.
            ((BaseReconstructedParticle) particle).setCharge(charge);

            // Extrapolate the particle ID from the track. Positively
            // charged particles are assumed to be positrons and those
            // with negative charges are assumed to be electrons.
            if (particle.getCharge() > 0) {
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(-11, 0, 0, 0));
            } else if (particle.getCharge() < 0) {
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(11, 0, 0, 0));
            }

            final int pid = particle.getParticleIDUsed().getPDG();
            // propogate pid to the cluster:
            if (Math.abs(pid) == 11) {
                for(Map.Entry<Track,Map<Cluster,Double>> entry : trackClusterResidualsMap.entrySet()){ 
                    Cluster clust = entry.getKey();
                    ((BaseCluster) clust).setParticleId(pid);
                }
            }
            */


        }

        //Given the mapping between all Tracks, and all potential cluster
        //matches, match Tracks to the Clusters that are closest in position
        //Algorithm checks for clusters matched to multiple Tracks, and sorts
        //them until only unique matches exist

        //minTrackClusterResidualMap maps Track to the Cluster with the smallest dr
        Map<Track,Cluster> minTrackClusterResidualMap = new HashMap<Track, Cluster>();

        //check map for the same Cluster being matched to multiple Tracks
        //If found, keep Track-Cluster match with smallest dr
        //Repeat matching process
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
