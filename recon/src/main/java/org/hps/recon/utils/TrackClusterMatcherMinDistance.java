package org.hps.recon.utils;

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.ecal.cluster.ClusterCorrectionUtilities;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackData;
import org.hps.record.StandardCuts;

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
import org.lcsim.event.ReconstructedParticle;


/**
 * Utility used to determine if a Tracka and Ecal Cluster are matched.
 * Class extends AbstractTrackClusterMatcher, which implements
 * TrackClusterMatcherInter
 *
 * @author <a href="mailto:alspellm@ucsc.edu">Alic Spellman</a>
 */

public class TrackClusterMatcherMinDistance extends AbstractTrackClusterMatcher{

    protected HashMap<Cluster, Track> clusterToTrack;
    RelationalTable hitToRotated = null;
    RelationalTable hitToStrips = null;
    RelationalTable trackToData = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
    /*
     * Define and initialize plots
     */
    protected ITree tree;
    protected IHistogramFactory histogramFactory;
    protected Map<String, IHistogram1D> plots1D;
    protected Map<String, IHistogram2D> plots2D;

    /** 
     * Default no-arg contructor
     */
    public TrackClusterMatcherMinDistance() {
        //Do nothing
    }

    @Override
    public void initializeParameterization(String fname){
        //Not used
    }

    /*
     * Return Track Cluster Match quality.
     * Quality value not yet defined. Return junk value
     */
    @Override
    public double getMatchQC(Cluster cluster, ReconstructedParticle particle){
        //match quality not yet defined for this matcher
        return -9999.9;
    }

    @Deprecated
    @Override
    public void setBeamEnergy(double beamEnergy) {
        //          this.beamEnergy = beamEnergy;
    }

    /*
     * Applies Cluster Corrections after matching.
     */

    @Override
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

    /*
     * Calls matching algorithm method, matches Tracks to Clusters, and returns
     * a Map of matched Tracks w Clusters. 
     * If Track is not matched with Cluster, Map value set to null.
     */
    @Override
    public HashMap<Track,Cluster> matchTracksToClusters(EventHeader event, List<List<Track>> trackCollections, List<Cluster> clusters, StandardCuts cuts, int flipSign, boolean useCorrectedClusterPositionsForMatching, boolean isMC, HPSEcal3 ecal, double beamEnergy){

        HashMap<Track,Cluster> trackClusterPairs = this.trackClusterMatcher(trackCollections, event, clusters, cuts, flipSign, useCorrectedClusterPositionsForMatching, isMC, ecal, beamEnergy);
        System.out.println("LOOK" + super.trackCollectionName);
        return trackClusterPairs;
    }

    /*
     * enable Plots and book histograms
     */
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

        System.out.println("[TrackClusterMatcherMinDistance] Saving Histogram for " + super.trackCollectionName);
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void bookHistograms() {

        plots1D = new HashMap<String, IHistogram1D>();
        plots2D = new HashMap<String, IHistogram2D>();

        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        //Timing Plots
        plots1D.put(String.format("%s_ele_track_cluster_dt",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_cluster_dt",super.trackCollectionName),  600, -150, 150));

        plots1D.put(String.format("%s_pos_track_cluster_dt",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_cluster_dt",super.trackCollectionName),  600, -150, 150));
        
        plots1D.put(String.format("%s_pos_track_time",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_time",super.trackCollectionName),  600, -150, 150));
        
        plots1D.put(String.format("%s_ele_track_time",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_time",super.trackCollectionName),  600, -150, 150));
        
        plots1D.put(String.format("%s_cluster_time_wOffset",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_cluster_time_wOffset",super.trackCollectionName),  600, -150, 150));

        //Track position at ecal
        plots1D.put(String.format("%s_pos_track_x",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_x",super.trackCollectionName),  500, -200, 800));
        
        plots1D.put(String.format("%s_pos_track_y",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_y",super.trackCollectionName),  300, -300, 300));
        
        plots1D.put(String.format("%s_pos_track_z",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_z",super.trackCollectionName),  750, 0, 1500));

        plots1D.put(String.format("%s_ele_track_x",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_x",super.trackCollectionName),  500, -800, 200));
        
        plots1D.put(String.format("%s_ele_track_y",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_y",super.trackCollectionName),  600, -300, 300));
        
        plots1D.put(String.format("%s_ele_track_z",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_z",super.trackCollectionName),  750, 0, 1500));

        //Track Cluster Residuals
        plots1D.put(String.format("%s_pos_track_cluster_dx",super.trackCollectionName),histogramFactory.createHistogram1D(String.format("%s_pos_track_cluster_dx",super.trackCollectionName),800, -200, 200));
        
        plots1D.put(String.format("%s_pos_track_cluster_dy",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_cluster_dy",super.trackCollectionName),800, -200, 200));
        
        plots1D.put(String.format("%s_pos_track_cluster_dz",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_cluster_dz",super.trackCollectionName),100, -50,50));
        
        plots1D.put(String.format("%s_pos_track_cluster_dr",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_cluster_dr",super.trackCollectionName),100, -50, 150));
        
        plots1D.put(String.format("%s_ele_track_cluster_dx",super.trackCollectionName),histogramFactory.createHistogram1D(String.format("%s_ele_track_cluster_dx",super.trackCollectionName),800, -200, 200));
        
        plots1D.put(String.format("%s_ele_track_cluster_dy",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_cluster_dy",super.trackCollectionName),800, -200, 200));
        
        plots1D.put(String.format("%s_ele_track_cluster_dz",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_cluster_dz",super.trackCollectionName),100, -50,50));
        
        plots1D.put(String.format("%s_ele_track_cluster_dr",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_cluster_dr",super.trackCollectionName),100, -50,150));

        //Matched Pair Plots
        plots1D.put(String.format("%s_pos_track_cluster_matched_pair_dx",super.trackCollectionName),histogramFactory.createHistogram1D(String.format("%s_pos_track_cluster_matched_pair_dx",super.trackCollectionName),800, -200, 200));
        
        plots1D.put(String.format("%s_pos_track_cluster_matched_pair_dy",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_cluster_matched_pair_dy",super.trackCollectionName),800, -200, 200));
        
        plots1D.put(String.format("%s_pos_track_cluster_matched_pair_dz",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_cluster_matched_pair_dz",super.trackCollectionName),100, -50,50));
        
        plots1D.put(String.format("%s_pos_track_cluster_matched_pair_dr",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_pos_track_cluster_matched_pair_dr",super.trackCollectionName),100, -50, 150));

        plots1D.put(String.format("%s_pos_track_cluster_matched_pair_dt",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_cluster_matched_pair_dt",super.trackCollectionName),  600, -150, 150));

        plots2D.put(String.format("%s_pos_track_cluster_matched_pair_p_v_E",super.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_track_cluster_matched_pair_p_v_E",super.trackCollectionName), 1000, 0, 5, 1000, 0, 5));

        
        plots1D.put(String.format("%s_ele_track_cluster_matched_pair_dx",super.trackCollectionName),histogramFactory.createHistogram1D(String.format("%s_ele_track_cluster_matched_pair_dx",super.trackCollectionName),800, -200, 200));
        
        plots1D.put(String.format("%s_ele_track_cluster_matched_pair_dy",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_cluster_matched_pair_dy",super.trackCollectionName),800, -200, 200));
        
        plots1D.put(String.format("%s_ele_track_cluster_matched_pair_dz",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_cluster_matched_pair_dz",super.trackCollectionName),100, -50,50));
        
        plots1D.put(String.format("%s_ele_track_cluster_matched_pair_dr",super.trackCollectionName),histogramFactory.createHistogram1D(String.format( "%s_ele_track_cluster_matched_pair_dr",super.trackCollectionName),100, -50,150));
        
        plots1D.put(String.format("%s_ele_track_cluster_matched_pair_dt",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_cluster_matched_pair_dt",super.trackCollectionName),  600, -150, 150));

        plots2D.put(String.format("%s_ele_track_cluster_matched_pair_p_v_E",super.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_track_cluster_matched_pair_p_v_E",super.trackCollectionName), 1000, 0, 5, 1000, 0, 5));

        //Unmatched Pair Plots
        plots1D.put(String.format("%s_ele_track_unmatched_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_unmatched_momentum",super.trackCollectionName),  1000, 0, 5));
        
        plots1D.put(String.format("%s_ele_track_unmatched_outsideEcal_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_unmatched_outsideEcal_momentum",super.trackCollectionName),  1000, 0, 5));
        
        plots1D.put(String.format("%s_ele_track_unmatched_insideEcal_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_unmatched_insideEcal_momentum",super.trackCollectionName),  1000, 0, 5));

        plots1D.put(String.format("%s_pos_track_unmatched_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_unmatched_momentum",super.trackCollectionName),  1000, 0, 5));

        plots1D.put(String.format("%s_pos_track_unmatched_outsideEcal_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_unmatched_outsideEcal_momentum",super.trackCollectionName),  1000, 0, 5));
        
        plots1D.put(String.format("%s_pos_track_unmatched_insideEcal_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_unmatched_insideEcal_momentum",super.trackCollectionName),  1000, 0, 5));

        // Energy/Momentum plots
        plots1D.put(String.format("%s_ele_track_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_momentum",super.trackCollectionName),  1000, 0, 5));

        
        plots1D.put(String.format("%s_pos_track_momentum",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_momentum",super.trackCollectionName),  1000, 0, 5));
        
        plots1D.put(String.format("%s_cluster_energy",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_cluster_energy",super.trackCollectionName),  1000, 0, 5));
        
        plots1D.put(String.format("%s_corrected_cluster_energy",super.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_corrected_cluster_energy",super.trackCollectionName),  1000, 0, 5));
    }

    public void plotClusterValues(List<Cluster> clusters, double trackClusterTimeOffset){
        for(Cluster cluster : clusters) {
            double clusterEnergy = cluster.getEnergy();
            double cluster_time = ClusterUtilities.getSeedHitTime(cluster);
            plots1D.get(String.format("%s_cluster_energy",super.trackCollectionName)).fill(clusterEnergy);
            plots1D.get(String.format("%s_cluster_time_wOffset",super.trackCollectionName)).fill(cluster_time - trackClusterTimeOffset);
        }
    }

    
    public List<Double> getTrackPositionAtEcal(Track track){

        double trackx;
        double tracky;
        double trackz;

        if (this.trackCollectionName.contains("GBLTracks")){
            trackx = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[1];
            tracky = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[2];
            trackz = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[0];
        }
        else {
            TrackData trackdata = (TrackData) trackToData.from(track);
            TrackState ts_ecal = track.getTrackStates().get(track.getTrackStates().size()-1);
            double[] ts_ecalPos = ts_ecal.getReferencePoint();
            trackx = ts_ecalPos[1];
            tracky = ts_ecalPos[2];
            trackz = ts_ecalPos[0];
        }
        List<Double> trackxyz = new ArrayList<Double>();
        trackxyz.add(trackx);
        trackxyz.add(tracky);
        trackxyz.add(trackz);
        return trackxyz;
    }

    private double getTrackTime(Track track){

        //GBL
        double trackt = 99999;
        if (super.trackCollectionName.contains("GBLTracks")){
            trackt = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
        }

        //KF
        else{
            TrackData trackdata = (TrackData) trackToData.from(track);
            trackt = trackdata.getTrackTime();
        }
        return trackt;
    }

    public HashMap<Track,Cluster> trackClusterMatcher(List<List<Track>> trackCollections, EventHeader event, List<Cluster> clusters, StandardCuts cuts, int flipSign, boolean useCorrectedClusterPositionsForMatching, boolean isMC, HPSEcal3 ecal, double beamEnergy)  {

        /*
         * Matching algorithm uses a closest distance match between Track and
         * Cluster.
         * This matcher works for both GBL and KF Tracks.
         * Returns Map of Tracks matched do unique Clusters
         */

        //Initialize clusterToTrack map, used for applying cluster corrections
        this.clusterToTrack = new HashMap<Cluster, Track>();

        // Map stores all Track Cluster pairs in Event.
        HashMap<Track, Cluster> alltrackClusterPairs = new HashMap<Track, Cluster>();

        //GBL Tracks require rotation tables
        if(super.trackCollectionName.contains("GBLTracks")){
            hitToRotated = TrackUtils.getHitToRotatedTable(event);
            hitToStrips = TrackUtils.getHitToStripsTable(event);
        }

        //For KF
        //Relational Table is used to get KF track time from
        //KFTrackDataRelations
        if(super.trackCollectionName.contains("KalmanFullTracks")){
            List<TrackData> TrackData;
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
        }

        if(enablePlots){
            plotClusterValues(clusters,cuts.getTrackClusterTimeOffset());
        }

        //For all Track lists in trackCollections, match Tracks to Clusters
        for (List<Track> tracks : trackCollections) {

            if(tracks == null || tracks.isEmpty() || clusters == null || clusters.isEmpty()){
                continue;
            }

            //Map relating Tracks to potential Cluster matches, and their
            //position residuals
            Map<Track, Map<Cluster, Double>> trackClusterResidualsMap = new HashMap<Track, Map<Cluster, Double>>(); 

            for(Track track : tracks) {

                 // Derive the charge of the particle from the track.
                int charge = (int) Math.signum(track.getTrackStates().get(0).getOmega())*flipSign;

                //Track time includes a hard-coded offset
                double trackt = getTrackTime(track);
                double tracktOffset = 3.5; //Track time distribution is centered on -4ns, added offset to center ~0

                //Track positions depend on GBL or KF
                List<Double> trackPos = this.getTrackPositionAtEcal(track);
                double trackx = trackPos.get(0);
                double tracky = trackPos.get(1);
                double trackz = trackPos.get(2);

                //Charge dependent offset in Track-Cluster dx residual peak
                //must be accounted for to match by closest distance.
                //Hard-coded for now...
                double dxoffset; 
                if(super.trackCollectionName.contains("GBLTracks")){
                    if(charge < 0)
                        dxoffset = -6.6;
                    else
                        dxoffset = 1.75;
                }
                else {
                    if(charge < 0)
                        dxoffset = -4.8; //KF ele tracks have x-position bias of -3.3 mm, hardcode offset + 3.3
                    else
                        dxoffset = 1.85; //Similar case as above 
                }

                //Defines Track top or bottom
                double tanlambda = track.getTrackParameter(4);

                //Track momentum magnitude
                double[] trackP = TrackUtils.getTrackStateAtLocation(track,TrackState.AtIP).getMomentum();
                double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));

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
                double tcut = 8.0;
                double xcut = 20.0;
                double ycut = 20.0;

                //Loop over all clusters, looking for best match to current track
                double smallestdt = Double.MAX_VALUE;
                double smallestdr = Double.MAX_VALUE;
                for(Cluster cluster : clusters) {
                    //if the option to use corrected cluster positions is selected, then
                    //create a copy of the current cluster, and apply corrections to it
                    //before calculating position residual.  Default is don't use corrections.  
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

                    double clusterx = cluster.getPosition()[0];
                    double clustery = cluster.getPosition()[1];
                    double clusterz = cluster.getPosition()[2];

                    //Calculate position residuals
                    double dx = clusterx - trackx + dxoffset;
                    double dy = clustery - tracky;
                    double dz = clusterz - trackz;
                    double dr = Math.sqrt(Math.pow(clusterx-trackx,2) + Math.pow(clustery-tracky,2));

                    //Check that Top/Bottom Track is only matched to
                    //Top/Bottom Clusters
                    if(clustery > 0 && tanlambda < 0)
                        continue;
                    if(clustery < 0 && tanlambda > 0)
                        continue;

                    if(enablePlots) {
                        if(charge > 0) {
                            //Time residual plot
                            plots1D.get(String.format("%s_pos_track_cluster_dt",super.trackCollectionName)).fill(dt);
                            //Track@Ecal Cluster Residuals
                            plots1D.get(String.format("%s_pos_track_cluster_dx",super.trackCollectionName)).fill(dx);
                            plots1D.get(String.format("%s_pos_track_cluster_dy",super.trackCollectionName)).fill(dy);
                            plots1D.get(String.format("%s_pos_track_cluster_dz",super.trackCollectionName)).fill(dz);
                            plots1D.get(String.format("%s_pos_track_cluster_dr",super.trackCollectionName)).fill(dr);
                        }
                        else {
                            //Time residual plot
                            plots1D.get(String.format("%s_ele_track_cluster_dt",super.trackCollectionName)).fill(dt);
                            //Track@Ecal Cluster Residuals
                            plots1D.get(String.format("%s_ele_track_cluster_dx",super.trackCollectionName)).fill(dx);
                            plots1D.get(String.format("%s_ele_track_cluster_dy",super.trackCollectionName)).fill(dy);
                            plots1D.get(String.format("%s_ele_track_cluster_dz",super.trackCollectionName)).fill(dz);
                            plots1D.get(String.format("%s_ele_track_cluster_dr",super.trackCollectionName)).fill(dr);
                        }
                    }

                    //If position and time residual cuts are passed, build map of
                    //all cluster position residuals with this track
                    if((Math.abs(dt) < tcut) && (Math.abs(dx) < xcut) && (Math.abs(dy) < ycut) ) {
                        cluster_dr_Map.put(originalcluster, dr);
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

                if(enablePlots){
                    Cluster cluster = entry.getValue();

                    Track track = entry.getKey();
                    int charge = (int) Math.signum(track.getTrackStates().get(0).getOmega())*flipSign;
                    double trackt = getTrackTime(track);
                    //Track momentum magnitude
                    double[] trackP = TrackUtils.getTrackStateAtLocation(track,TrackState.AtIP).getMomentum();
                    double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
                    //Track position
                    List<Double> trackPos = this.getTrackPositionAtEcal(track);
                    double trackx = trackPos.get(0);
                    double tracky = trackPos.get(1);
                    double trackz = trackPos.get(2);

                    if(cluster == null){
                        if(charge < 0){
                            plots1D.get(String.format("%s_ele_track_unmatched_momentum",super.trackCollectionName)).fill(trackPmag);
                            if(!isTrackInEcal(trackx, tracky)){
                                plots1D.get(String.format("%s_ele_track_unmatched_outsideEcal_momentum",super.trackCollectionName)).fill(trackPmag);
                            }
                            else{
                                plots1D.get(String.format("%s_ele_track_unmatched_insideEcal_momentum",super.trackCollectionName)).fill(trackPmag);
                            }
                        }
                        else{
                            plots1D.get(String.format("%s_pos_track_unmatched_momentum",super.trackCollectionName)).fill(trackPmag);
                            if(!isTrackInEcal(trackx, tracky)){
                                plots1D.get(String.format("%s_pos_track_unmatched_outsideEcal_momentum",super.trackCollectionName)).fill(trackPmag);
                            }
                            else{
                                plots1D.get(String.format("%s_pos_track_unmatched_insideEcal_momentum",super.trackCollectionName)).fill(trackPmag);
                            }
                        }
                        continue;
                    }

                    //Cluster Position
                    double cluster_time = ClusterUtilities.getSeedHitTime(cluster);
                    double clusterx = cluster.getPosition()[0];
                    double clustery = cluster.getPosition()[1];
                    double clusterz = cluster.getPosition()[2];

                    //Calculate position residuals
                    double dx = clusterx - trackx;
                    double dy = clustery - tracky;
                    double dz = clusterz - trackz;
                    double dr = Math.sqrt(Math.pow(clusterx-trackx,2) + Math.pow(clustery-tracky,2));
                    double dt = cluster_time - cuts.getTrackClusterTimeOffset() - trackt;
                    if(charge > 0) {
                        //Time residual plot
                        plots1D.get(String.format("%s_pos_track_cluster_matched_pair_dt",super.trackCollectionName)).fill(dt);
                        //Track@Ecal Cluster Residuals
                        plots1D.get(String.format("%s_pos_track_cluster_matched_pair_dx",super.trackCollectionName)).fill(dx);
                        plots1D.get(String.format("%s_pos_track_cluster_matched_pair_dy",super.trackCollectionName)).fill(dy);
                        plots1D.get(String.format("%s_pos_track_cluster_matched_pair_dz",super.trackCollectionName)).fill(dz);
                        plots1D.get(String.format("%s_pos_track_cluster_matched_pair_dr",super.trackCollectionName)).fill(dr);
                        plots2D.get(String.format("%s_pos_track_cluster_matched_pair_p_v_E",super.trackCollectionName)).fill(trackPmag,cluster.getEnergy());
                    }
                    else {
                        //Time residual plot
                        plots1D.get(String.format("%s_ele_track_cluster_matched_pair_dt",super.trackCollectionName)).fill(dt);
                        //Track@Ecal Cluster Residuals
                        plots1D.get(String.format("%s_ele_track_cluster_matched_pair_dx",super.trackCollectionName)).fill(dx);
                        plots1D.get(String.format("%s_ele_track_cluster_matched_pair_dy",super.trackCollectionName)).fill(dy);
                        plots1D.get(String.format("%s_ele_track_cluster_matched_pair_dz",super.trackCollectionName)).fill(dz);
                        plots1D.get(String.format("%s_ele_track_cluster_matched_pair_dr",super.trackCollectionName)).fill(dr);
                        plots2D.get(String.format("%s_ele_track_cluster_matched_pair_p_v_E",super.trackCollectionName)).fill(trackPmag,cluster.getEnergy());
                    }
                }
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

    public boolean isTrackInEcal(double trackx, double tracky){

        //Define first order Ecal geometry --> assumed square here,
        //smaller than actual beamgap. Improve x geometry 
        double ecalx1 = -276.0; //mm
        double ecalx2 = 361.0;
        double ecaly1 = 91.0;
        double ecaly2 = -91.0;
        double bgapup = 22;
        double bgapdown = -22;

        double eholex11 = -93.0;
        double eholex12 = -70.0;

        double eholex22 = 15.0;
        double eholex21 = 29;

        double eholey12 = 36.0;
        double eholey11 = 22.4;

        double eholey22 = -36.0;
        double eholey21 = -22.3;

        boolean inEcalAccept = true;

        if(trackx < ecalx1 || trackx > ecalx2)
            inEcalAccept = false;
        if(tracky > ecaly1 || tracky < ecaly2)
            inEcalAccept = false;
        if(tracky < bgapup && tracky > bgapdown)
            inEcalAccept = false;
        if((trackx > eholex12 && trackx < eholex22) && ( (tracky < eholey12) && (tracky > eholey11)))
            inEcalAccept = false;
        if((trackx > eholex12 && trackx < eholex22) && ( (tracky < eholey21) && (tracky > eholey22)))
            inEcalAccept = false;
        return inEcalAccept;
    }
}
