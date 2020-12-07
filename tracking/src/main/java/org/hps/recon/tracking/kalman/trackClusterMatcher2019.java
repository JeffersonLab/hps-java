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

public class trackClusterMatcher2019 {


    //Used for plots
    boolean enablePlots = false;
    private ITree tree;
    private IHistogramFactory histogramFactory;
    private Map<String, IHistogram1D> plots1D;
    private String trackType;

    RelationalTable hitToRotated = null;
    RelationalTable hitToStrips = null;

    public trackClusterMatcher2019(String trackCollectionName) {
        trackType = trackCollectionName;
        System.out.println("TRACK TYPE IS " + trackType);
    }

    public void enablePlots(boolean enablePlots) {
        this.enablePlots = enablePlots;
        if (enablePlots ==true) {
            this.bookHistograms();
        }
    }

    public void saveHistograms() {
        System.out.println("Saving Histogram for " + this.trackType);
        String rootFile = String.format("%s_TrackClusterMatching.root",this.trackType);
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

        System.out.println("BOOKING HISTOGRAMS FOR " +  this.trackType);
        String trackType = this.trackType;
        plots1D = new HashMap<String, IHistogram1D>();
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        //Timing Plots
        plots1D.put(String.format("%s_ElectronTrack-Cluster_dt",trackType), histogramFactory.createHistogram1D(String.format("%s_ElectronTrack-Cluster_dt",trackType),  200, -200, 200));
        plots1D.put(String.format("%s_PositronTrack-Cluster_dt",trackType), histogramFactory.createHistogram1D(String.format("%s_PositronTrack-Cluster_dt",trackType),  200, -200, 200));
        plots1D.put(String.format("%s_PositronTrackTime",trackType), histogramFactory.createHistogram1D(String.format("%s_PositronTrackTime",trackType),  200, -200, 200));
        plots1D.put(String.format("%s_ElectronTrackTime",trackType), histogramFactory.createHistogram1D(String.format("%s_ElectronTrackTime",trackType),  200, -200, 200));
        plots1D.put(String.format("%s_Cluster_Timing_(woffset)",trackType), histogramFactory.createHistogram1D(String.format("%s_Cluster_Timing_(woffset)",trackType),  200, -200, 200));

        //Track to Ecal Extrapolation
        plots1D.put(String.format("%s_PositronTrack@ECal_ECalCluster_dx",trackType),histogramFactory.createHistogram1D(String.format("%s_PositronTrack@ECal_ECalCluster_dx",trackType),400, -200, 200));
        plots1D.put(String.format("%s_PositronTrack@ECal_ECalCluster_dy",trackType),histogramFactory.createHistogram1D(String.format( "%s_PositronTrack@ECal_ECalCluster_dy",trackType),400, -200, 200));
        plots1D.put(String.format("%s_PositronTrack@ECal_ECalCluster_dz",trackType),histogramFactory.createHistogram1D(String.format( "%s_PositronTrack@ECal_ECalCluster_dz",trackType),100, -50,50));
        plots1D.put(String.format("%s_PositronTrack@ECal_ECalCluster_dr",trackType),histogramFactory.createHistogram1D(String.format( "%s_PositronTrack@ECal_ECalCluster_dr",trackType),100, -50, 150));
        plots1D.put(String.format("%s_ElectronTrack@ECal_ECalCluster_dx",trackType),histogramFactory.createHistogram1D(String.format("%s_ElectronTrack@ECal_ECalCluster_dx",trackType),400, -200, 200));
        plots1D.put(String.format("%s_ElectronTrack@ECal_ECalCluster_dy",trackType),histogramFactory.createHistogram1D(String.format( "%s_ElectronTrack@ECal_ECalCluster_dy",trackType),400, -200, 200));
        plots1D.put(String.format("%s_ElectronTrack@ECal_ECalCluster_dz",trackType),histogramFactory.createHistogram1D(String.format( "%s_ElectronTrack@ECal_ECalCluster_dz",trackType),100, -50,50));
        plots1D.put(String.format("%s_ElectronTrack@ECal_ECalCluster_dr",trackType),histogramFactory.createHistogram1D(String.format( "%s_ElectronTrack@ECal_ECalCluster_dr",trackType),100, -50,150));

        // Energy/Momentum plots
        plots1D.put(String.format("%s_ele_Track_Cluster_EdivP",trackType), histogramFactory.createHistogram1D(String.format("%s_ele_Track_Cluster_EdivP",trackType),  200, -10, 10));
        plots1D.put(String.format("%s_pos_Track_Cluster_EdivP",trackType), histogramFactory.createHistogram1D(String.format("%s_pos_Track_Cluster_EdivP",trackType),  200, -10, 10));

       /* 
        //RK Extrapolated Residuals
        plots1D.put(String.format("RK_PositronTrack@ECal_ECalCluster_dx",histogramFactory.createHistogram1D(String.format("RK_PositronTrack@ECal_ECalCluster_dx",400, -200, 200));
        plots1D.put(String.format("RK_PositronTrack@ECal_ECalCluster_dy",histogramFactory.createHistogram1D(String.format( "RK_PositronTrack@ECal_ECalCluster_dy",400, -200, 200));
        plots1D.put(String.format("RK_PositronTrack@ECal_ECalCluster_dz",histogramFactory.createHistogram1D(String.format( "RK_PositronTrack@ECal_ECalCluster_dz",100, -50,50));
        plots1D.put(String.format("RK_ElectronTrack@ECal_ECalCluster_dx",histogramFactory.createHistogram1D(String.format("RK_ElectronTrack@ECal_ECalCluster_dx",400, -200, 200));
        plots1D.put(String.format("RK_ElectronTrack@ECal_ECalCluster_dy",histogramFactory.createHistogram1D(String.format( "RK_ElectronTrack@ECal_ECalCluster_dy",400, -200, 200));
        plots1D.put(String.format("RK_ElectronTrack@ECal_ECalCluster_dz",histogramFactory.createHistogram1D(String.format( "RK_ElectronTrack@ECal_ECalCluster_dz",100, -50,50));
    */
    }
    public Cluster oldtrackClusterMatcher(Track TrackHPS, String trackCollectionName, int Charge, List<Cluster> Clusters, double trackTime, double trackClusterTimeOffset)  {

        //HPSTrack
        Track track = TrackHPS;
        int charge = Charge;
        double tanlambda = track.getTrackParameter(4);
        String trackType = trackCollectionName;
        double tracktOffset = 4; //track time distributions show mean at -4 ns
        double trackt = trackTime;
        double trackx;
        double tracky;
        double trackz;
        double dxoffset;
        List<Cluster> clusters = Clusters;

        if(trackType.contains("GBLTracks")) {
            trackx = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[1]; 
            tracky = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[2];
            trackz = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[0];
            if(charge < 0)
                dxoffset = -5.5;
            else
                dxoffset = 0.0;
        }

        else {
            TrackState ts_ecal = track.getTrackStates().get(track.getTrackStates().size()-1);
            double[] ts_ecalPos = ts_ecal.getReferencePoint();
            trackx = ts_ecalPos[0];
            tracky = ts_ecalPos[1];
            trackz = ts_ecalPos[2];
            if(charge < 0)
                dxoffset = 3.3;
            else
                dxoffset = -3.6;
        }

        if(enablePlots){
            if (charge > 0) {
                plots1D.get(String.format("%s_PositronTrackTime",trackType)).fill(trackt);
            }
            else {
                plots1D.get(String.format("%s_ElectronTrackTime",trackType)).fill(trackt);
            }
        }


        /*
        //Track state at ecal via RK extrap
        TrackState ts_ecalRK = track.getTrackStates().get(track.getTrackStates().size()-2);
        Hep3Vector ts_ecalPos_RK = new BasicHep3Vector(ts_ecalRK.getReferencePoint());
        ts_ecalPos_RK = CoordinateTransformations.transformVectorToDetector(ts_ecalPos_RK);
        */


        Cluster matchedCluster = null;
        double smallestdt = Double.MAX_VALUE;
        double smallestdr = Double.MAX_VALUE;
        double offset = trackClusterTimeOffset;
        double tcut = 4.0;
        double xcut = 15.0;
        double ycut = 15.0;

        for(Cluster cluster : clusters) {
            double clusTime = ClusterUtilities.getSeedHitTime(cluster);
            double dt = clusTime - trackClusterTimeOffset - trackt + tracktOffset;

            double clusxpos = cluster.getPosition()[0];
            double clusypos = cluster.getPosition()[1];
            double cluszpos = cluster.getPosition()[2];
            double dx = clusxpos - trackx + dxoffset;
            double dy = clusypos - tracky;
            double dz = cluszpos - trackz;
            double dr = Math.sqrt(Math.pow(clusxpos-trackx,2) + Math.pow(clusypos-tracky,2));
            if(clusxpos < 0 && charge > 0)
                continue;
            if(clusxpos > 0 && charge < 0)
                continue;
            if(clusypos > 0 && tanlambda < 0)
                continue;
            if(clusypos < 0 && tanlambda > 0)
                continue;

            /*
            //via RK Extrap
            double dxRK = clusPos[0]-ts_ecalPos_RK.x();
            double dyRK = clusPos[1]-ts_ecalPos_RK.y();
            double dzRK = clusPos[2]-ts_ecalPos_RK.z();
            */


            //Extremely simplified track cluster matching. Cluster that passes
            //position cuts and has closest time is matched. This needs to be
            //updated to a real algorithm.
            if((Math.abs(dt) < tcut) && (Math.abs(dx) < xcut) && (Math.abs(dy) < ycut) ) {
                System.out.println("KF cluster passing selection found");
                if(Math.abs(dr) < smallestdr) {
                    smallestdr = Math.abs(dr);
                    matchedCluster = cluster;
                }
            }

            
            if(enablePlots) {
                System.out.println("Filling Histograms for " + trackType);
                plots1D.get(String.format("%s_Cluster_Timing_(woffset)",trackType)).fill(clusTime-offset);
                if((Math.abs(dt) < tcut) && (Math.abs(dx) < xcut) && (Math.abs(dy) < ycut) ) {
                    if(charge > 0) {
                        //Time residual plot
                        plots1D.get(String.format("%s_PositronTrack-Cluster_dt",trackType)).fill(dt);

                        //Kalman Extrapolated Residuals
                        plots1D.get(String.format("%s_PositronTrack@ECal_ECalCluster_dx",trackType)).fill(dx);
                        plots1D.get(String.format("%s_PositronTrack@ECal_ECalCluster_dy",trackType)).fill(dy);
                        plots1D.get(String.format("%s_PositronTrack@ECal_ECalCluster_dz",trackType)).fill(dz);
                        plots1D.get(String.format("%s_PositronTrack@ECal_ECalCluster_dr",trackType)).fill(dr);

                        /*
                        //RK Extrapolated Residuals
                        plots1D.get(String.format("RK_PositronTrack@ECal_ECalCluster_dx").fill(dxRK);
                        plots1D.get(String.format("RK_PositronTrack@ECal_ECalCluster_dy").fill(dyRK);
                        plots1D.get(String.format("RK_PositronTrack@ECal_ECalCluster_dz").fill(dzRK);
                        */
                    }
                    else {

                        //Time residual plot
                        plots1D.get(String.format("%s_ElectronTrack-Cluster_dt",trackType)).fill(dt);

                        //Kalman Extrapolated Residuals
                        plots1D.get(String.format("%s_ElectronTrack@ECal_ECalCluster_dx",trackType)).fill(dx);
                        plots1D.get(String.format("%s_ElectronTrack@ECal_ECalCluster_dy",trackType)).fill(dy);
                        plots1D.get(String.format("%s_ElectronTrack@ECal_ECalCluster_dz",trackType)).fill(dz);
                        plots1D.get(String.format("%s_ElectronTrack@ECal_ECalCluster_dr",trackType)).fill(dr);
                        /*
                        //RK Extrapolated Residuals
                        plots1D.get(String.format("RK_ElectronTrack@ECal_ECalCluster_dx").fill(dxRK);
                        plots1D.get(String.format("RK_ElectronTrack@ECal_ECalCluster_dy").fill(dyRK);
                        plots1D.get(String.format("RK_ElectronTrack@ECal_ECalCluster_dz").fill(dzRK);
                        */
                    }
                }
            }
            
        }

        if(matchedCluster == null){
            System.out.println("No matching cluster found for KF track at ECal");
            return null;
        }
        else {
            return matchedCluster;
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
        if(tracks == null || tracks.isEmpty()){
            System.out.println("Track list given to KFTrackEcalClusterMatcher is Empty!");
            return null;
        }

        Map<Track, Map<Cluster, Double>> trackClusterResMap = new HashMap<Track, Map<Cluster, Double>>(); 

        //First, gather all necessary Track information
        String trackType = trackCollectionName;

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

        for(Track track : tracks) {

            //charge sign must be flipped by factor of -1 (WHY!?)
            int charge = -1* (int) Math.signum(track.getTrackStates().get(0).getOmega());
            double trackt;
            //The mean of track time distribution, for GBL and KF, is at -4ns.
            //Offset is hardcoded below
            double tracktOffset = 4; 
            double tanlambda = track.getTrackParameter(4);
            double[] trackP;
            double trackPmag;
            double trackx;
            double tracky;
            double trackz;
            double dxoffset;

            if (trackType.contains("GBLTracks")){
                trackt = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
                trackx = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[1]; 
                tracky = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[2];
                trackz = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[0];
                trackP = track.getTrackStates().get(0).getMomentum(); 

                //electron GBLTracks show x position bias at +5.5 mm. Offset
                //accounted for below (IS THIS OKAY TO HARDCODE?)
                if(charge < 0)
                    dxoffset = -5.5;
                else
                    dxoffset = 0.0;
            }

            //KFTracks
            else {
                trackdata = (TrackData) trackToData.from(track);
                trackt = trackdata.getTrackTime();
                //KF TrackState at ecal stored as the last TrackState in
                //KalmanInterface.java
                TrackState ts_ecal = track.getTrackStates().get(track.getTrackStates().size()-1);
                trackP = track.getTrackStates().get(0).getMomentum(); 
                double[] ts_ecalPos = ts_ecal.getReferencePoint();
                trackx = ts_ecalPos[0];
                tracky = ts_ecalPos[1];
                trackz = ts_ecalPos[2];
                if(charge < 0)
                    dxoffset = 3.3;
                else
                    dxoffset = - 3.6;
            }

            //Plot ele and pos track times
            if(enablePlots){
                if (charge > 0) {
                    plots1D.get(String.format("%s_PositronTrackTime",trackType)).fill(trackt);
                }
                else {
                    plots1D.get(String.format("%s_ElectronTrackTime",trackType)).fill(trackt);
                }
            }

            trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));

            /*
            //Track state at ecal via RK extrap
            TrackState ts_ecalRK = track.getTrackStates().get(track.getTrackStates().size()-2);
            Hep3Vector ts_ecalPos_RK = new BasicHep3Vector(ts_ecalRK.getReferencePoint());
            ts_ecalPos_RK = CoordinateTransformations.transformVectorToDetector(ts_ecalPos_RK);
            */

            //Begin Cluster Matching Algorithm
            Map<Cluster, Double> clusterResMap = new HashMap<Cluster, Double>();
            Cluster matchedCluster = null;

            double smallestdt = Double.MAX_VALUE;
            double smallestdr = Double.MAX_VALUE;
            //define time and position cuts for Track-Cluster matching
            double tcut = 4.0;
            double xcut = 10.0;
            double ycut = 10.0;

            for(Cluster cluster : clusters) {
                double clusterEnergy = cluster.getEnergy();
                double clusTime = ClusterUtilities.getSeedHitTime(cluster);
                double dt = clusTime - trackClusterTimeOffset - trackt + tracktOffset;

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

                //Plot of cluster energy / track momentum
                if(enablePlots){
                    if(charge < 0)
                        plots1D.get(String.format("%s_ele_Track_Cluster_EdivP",trackType)).fill(clusterEnergy/trackPmag);
                    else
                        plots1D.get(String.format("%s_pos_Track_Cluster_EdivP",trackType)).fill(clusterEnergy/trackPmag);
                }

                //If position and time residual cuts are passed, build map of
                //all cluster position residuals with this track
                if((Math.abs(dt) < tcut) && (Math.abs(dx) < xcut) && (Math.abs(dy) < ycut) ) {
                    clusterResMap.put(cluster, dr);
                }

                if(enablePlots) {
                    plots1D.get(String.format("%s_Cluster_Timing_(woffset)",trackType)).fill(clusTime - trackClusterTimeOffset);
                    if((Math.abs(dt) < tcut) && (Math.abs(dx) < xcut) && (Math.abs(dy) < ycut) ) {
                        if(charge > 0) {
                            //Time residual plot
                            plots1D.get(String.format("%s_PositronTrack-Cluster_dt",trackType)).fill(dt);
                            //Energy/Momentum plot

                            //Kalman Extrapolated Residuals
                            plots1D.get(String.format("%s_PositronTrack@ECal_ECalCluster_dx",trackType)).fill(dx);
                            plots1D.get(String.format("%s_PositronTrack@ECal_ECalCluster_dy",trackType)).fill(dy);
                            plots1D.get(String.format("%s_PositronTrack@ECal_ECalCluster_dz",trackType)).fill(dz);
                            plots1D.get(String.format("%s_PositronTrack@ECal_ECalCluster_dr",trackType)).fill(dr);

                            /*
                            //RK Extrapolated Residuals
                            plots1D.get(String.format("RK_PositronTrack@ECal_ECalCluster_dx").fill(dxRK);
                            plots1D.get(String.format("RK_PositronTrack@ECal_ECalCluster_dy").fill(dyRK);
                            plots1D.get(String.format("RK_PositronTrack@ECal_ECalCluster_dz").fill(dzRK);
                            */
                        }
                        else {

                            //Time residual plot
                            plots1D.get(String.format("%s_ElectronTrack-Cluster_dt",trackType)).fill(dt);
                            //Energy/Momentum plot

                            //Kalman Extrapolated Residuals
                            plots1D.get(String.format("%s_ElectronTrack@ECal_ECalCluster_dx",trackType)).fill(dx);
                            plots1D.get(String.format("%s_ElectronTrack@ECal_ECalCluster_dy",trackType)).fill(dy);
                            plots1D.get(String.format("%s_ElectronTrack@ECal_ECalCluster_dz",trackType)).fill(dz);
                            plots1D.get(String.format("%s_ElectronTrack@ECal_ECalCluster_dr",trackType)).fill(dr);
                            /*
                            //RK Extrapolated Residuals
                            plots1D.get(String.format("RK_ElectronTrack@ECal_ECalCluster_dx").fill(dxRK);
                            plots1D.get(String.format("RK_ElectronTrack@ECal_ECalCluster_dy").fill(dyRK);
                            plots1D.get(String.format("RK_ElectronTrack@ECal_ECalCluster_dz").fill(dzRK);
                            */
                        }
                    }
                }

                /*
                //via RK Extrap
                double dxRK = clusPos[0]-ts_ecalPos_RK.x();
                double dyRK = clusPos[1]-ts_ecalPos_RK.y();
                double dzRK = clusPos[2]-ts_ecalPos_RK.z();
                */
            }

            //Every Track is mapped to the map of cluster position residuals
            //for that track. i.e (Track; clusterA_dr, clusterB_dr,
            //clusterG_dr, clusterZ_dr)
            trackClusterResMap.put(track, clusterResMap);
        }

        //Given the mapping between all Tracks, and all potential cluster
        //matches, match Tracks to the Clusters that are closest in position
        //Algorithm checks for clusters matched to multiple Tracks, and sorts
        //them until only unique matches exist

        //trackMinResClusterMap maps tracks with minimum position residual
        //cluster
        Map<Track,Cluster> trackMinResClusterMap = new HashMap<Track, Cluster>();

        //build a map of track -> closest cluster
        //check this map for repeat cluster matches
        //if repeat matches are found for any tracks, keep best (closest)
        //match, and loop over matching again, for size of clusters
        for(int i=0; i < clusters.size(); i++){
            trackMinResClusterMap = getTrackMinResClusterMap(trackClusterResMap);
            trackClusterResMap = checkDuplicateClusterMatching(trackClusterResMap,trackMinResClusterMap);
        }
        trackMinResClusterMap = getTrackMinResClusterMap(trackClusterResMap);
        return trackMinResClusterMap;
    }


    public Map<Track, Map<Cluster,Double>> checkDuplicateClusterMatching(Map<Track, Map<Cluster,Double>> trackClusterResMap, Map<Track, Cluster> trackMinResClusterMap){
        
        boolean duplicateCluster = false;
        List<Track> sharedClusterTracks = new ArrayList<Track>();
        List<Track> skipTracks = new ArrayList<Track>();
        
        for(Track track : trackMinResClusterMap.keySet()){
            Map<Track, Cluster> trackswDuplicateClusters = new HashMap<Track, Cluster>();
            if(skipTracks.contains(track))
                continue;
            Cluster smallestdrCluster = trackMinResClusterMap.get(track);
            if(smallestdrCluster == null)
                continue;
            for(Track otherTrack : trackMinResClusterMap.keySet()){
                if(skipTracks.contains(track))
                    continue;
                if(otherTrack == track)
                    continue;
                Cluster othersmallestdrCluster = trackMinResClusterMap.get(otherTrack);
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
                return trackClusterResMap;
            for(Track duptrack : trackswDuplicateClusters.keySet()){
                double dr = trackClusterResMap.get(duptrack).get(trackswDuplicateClusters.get(duptrack));
                if(dr < smallestdr){
                    smallestdr = dr;
                    smallestdrTrack = duptrack;
                }
            }
            for(Track duptrack : trackswDuplicateClusters.keySet()){
                skipTracks.add(duptrack);
                if(duptrack != smallestdrTrack){
                    trackClusterResMap.get(duptrack).remove(trackswDuplicateClusters.get(duptrack));
                }
            }
        }
        return trackClusterResMap;
    }

    public Map<Track,Cluster>  getTrackMinResClusterMap(Map<Track, Map<Cluster, Double>> trackClusterResMap){

        //inputs a mapping of tracks with residuals for each possible cluster
        //from all clusters in the map, for each track, match the cluster with
        //the smallest position residual to that track
        //build output map of track -> closest cluster
        Map<Track,Cluster> Map = new HashMap<Track, Cluster>();
        for(Track track : trackClusterResMap.keySet()){
            double smallestdr = 99999.0;
            Cluster smallestdrCluster = null;
            Map<Cluster, Double> clusterResMap = trackClusterResMap.get(track);
            for(Cluster c : clusterResMap.keySet()){
                double dr = clusterResMap.get(c);
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
