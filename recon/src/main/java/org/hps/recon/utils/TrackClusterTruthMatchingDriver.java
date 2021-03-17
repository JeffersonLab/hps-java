package org.hps.recon.utils;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.HashMap;
import java.util.List; 
import java.util.ArrayList; 
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackData;
import org.hps.record.StandardCuts;
import org.hps.recon.ecal.cluster.ClusterUtilities;

import org.hps.util.Pair;
import org.hps.util.RK4integrator;

import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.util.Driver;
import org.lcsim.geometry.Detector;
import org.lcsim.event.TrackState;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.geometry.subdetector.HPSEcal3;


/** 
 * Driver used to truth match Tracks and Clusters, and then check the
 * performance of Track-to-Cluster Matcher algorithm.
 **/

public class TrackClusterTruthMatchingDriver extends Driver {

    private org.lcsim.geometry.FieldMap fM;
    private ITree tree;
    private IHistogramFactory histogramFactory;
    private Map<String, IHistogram1D> plots1D;
    private Map<String, IHistogram2D> plots2D;
    String[] identifiers = {"truth_matched"};

    RelationalTable hitToRotated = null;
    RelationalTable hitToStrips = null;
    RelationalTable TrktoData = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
    HPSEcal3 ecal;
    double beamEnergy = 2.3;
    protected StandardCuts cuts = new StandardCuts();
    TrackClusterMatcher matcher;

    //030112021
    //Charged Track matcher Evaluation
    Map<MCParticle,Pair<Track,Cluster>> goodMatches = new HashMap<MCParticle,Pair<Track,Cluster>>();
    Map<MCParticle,Pair<Track,Cluster>> badMatches = new HashMap<MCParticle,Pair<Track,Cluster>>();
    Map<MCParticle,Pair<Track,Cluster>> missedMatches = new HashMap<MCParticle,Pair<Track,Cluster>>();
    Map<MCParticle,Pair<Track,Cluster>> photonsMatchedToTrack = new HashMap<MCParticle,Pair<Track,Cluster>>();
    Map<MCParticle,Pair<Track,Cluster>> truthPhotonClusters = new HashMap<MCParticle,Pair<Track,Cluster>>();
    Map<MCParticle,Pair<Track,Cluster>> unknownMatches_mcpToRogueTrack = new HashMap<MCParticle,Pair<Track,Cluster>>();
    Map<MCParticle,Pair<Track,Cluster>> unknownMatches_mcpToRogueCluster = new HashMap<MCParticle,Pair<Track,Cluster>>();
    Map<MCParticle, Pair<Track,Cluster>> unmatchedUntrackableMCPs = new HashMap<MCParticle, Pair<Track,Cluster>>();
    Map<Track, Cluster> rogueMatches = new HashMap<Track,Cluster>();
    List<Track> rogueTracks = new ArrayList<Track>();
    List<Cluster> iddPhotons = new ArrayList<Cluster>();
    List<MCParticle> mcparticlesEvaluated = new ArrayList<MCParticle>();

    //INITIALIZE TERMS TO EVALUATE MATCHING PERFORMANCE
    int ntruthpairs_ele = 0;
    int ntruthpairs_pos = 0;
    int ntruthTracks_ele = 0;
    int ntruthTracks_pos = 0;
    int nrecoTracks_ele = 0;
    int nrecoTracks_pos = 0;
    int ntruthClusters = 0;
    int nClusters = 0;

    double goodMatch_ele=0;
    double fakeMatch_ele=0;
    double noMatch_ele=0;
    double missedMatch_ele=0;
    double unknownMatch_ele=0;
    double goodMatch_pos=0;
    double fakeMatch_pos=0;
    double noMatch_pos=0;
    double missedMatch_pos=0;
    double unknownMatch_pos=0;

    double noMatchTruthTrack_ele=0;
    double unknownMatchTruthTrack_ele=0;
    double unknownMatchTruthCluster_ele=0;
    double truthTrackOutsideEcal_ele=0; 
    double trackOutsideEcal_ele=0;
    double noMatchTruthTrack_pos=0;
    double unknownMatchTruthTrack_pos=0;
    double unknownMatchTruthCluster_pos=0;
    double truthTrackOutsideEcal_pos=0; 
    double trackOutsideEcal_pos=0;

    double efficiency_ele;
    double efficiency_pos;
    double fakerate_ele;
    double fakerate_pos;


    //Counting fake rate
    boolean verbose = true;
    double NtruthEleClustPairs = 0;
    double NtruthPosClustPairs = 0;

    //Check track efficiency 
    double NpossibleTracks = 0;
    double NrecoTruthTracks = 0;
    double trackEfficiency = 0;

    //check number of reco tracks that are matched to the same MCP "duplicates"
    double nDuplicates = 0;

    //time difference for track and cluster
    double trackClusterTimeOffset;
    boolean truthComparisons = true;

    //Collection Names
    String ecalScoringPlaneHitsCollectionName = "TrackerHitsECal";
    String trackCollectionName = "KalmanFullTracks";
    String trackToScoringPlaneHitRelationsName = "TrackToEcalScoringPlaneHitRelations";

    String ecalClustersCollectionName = "EcalClusters";
    String ecalReadoutHitsCollectionName = "EcalReadoutHits";
    String ecalTruthRelationsName = "EcalTruthRelations";
    //matcher
    String trackClusterMatcherAlgo = "TrackClusterMatcherMinDistance";

    private Set<SimTrackerHit> simhitsontrack = new HashSet<SimTrackerHit>();


    public void setTrackClusterTimeOffset(double input) {
        trackClusterTimeOffset = input;
    }
    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }
    public void setTrackClusterMatcherAlgo(String algoName) {
        this.trackClusterMatcherAlgo = algoName;
    }

    public void saveHistograms() {
        System.out.println("[TrackClusterTruthMatchingDriver] Saving Histogram for " + this.trackCollectionName);
        String rootFile = String.format("%s_truthTrackClusterMatching.root",this.trackCollectionName);
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

        String trackCollectionName = this.trackCollectionName;
        plots1D = new HashMap<String, IHistogram1D>();
        plots2D = new HashMap<String, IHistogram2D>();
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);


//Total Counts of Interesting Events
    
        plots1D.put(String.format("nMCPsEvaluated",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCPsEvaluated",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nGoodMatches",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nGoodMatches",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nBadMatches",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nBadMatches",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nMissedMatches",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMissedMatches",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nUnknownMatches_mcpToRogueTrack",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nUnknownMatches_mcpToRogueTrack",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nUnknownMatches_mcpToRogueCluster",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nUnknownMatches_mcpToRogueCluster",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nUnmatchedUntrackableMCPs",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nUnmatchedUntrackableMCPs",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nPhotons_matched_to_track",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nPhotons_matched_to_track",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nTruth_photon_clusters",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nTruth_photon_clusters",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nRogueMatches",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nRogueMatches",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nRogueTracks",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nRogueTracks",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nIddPhotons",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nIddPhotons",this.trackCollectionName), 101, -1, 100));

        plots1D.put(String.format("nTracklessMCPClusterMatchedToRogueTrack",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nTracklessMCPClusterMatchedToRogueTrack",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nTracklessMCPClusterNotMatchedToTrack",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nTracklessMCPClusterNotMatchedToTrack",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nClusterlessMCPtrackMatchedToRogueCluster",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nClusterlessMCPtrackMatchedToRogueCluster",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nClusterlessMCPtrackNotMatchedToCluster",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nClusterlessMCPtrackNotMatchedToCluster",this.trackCollectionName), 101, -1, 100));
        plots1D.put(String.format("nMCParticlesEvaluated",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCParticlesEvaluated",this.trackCollectionName), 101, -1, 100));

//Event characterization plots

        plots1D.put(String.format("nEvents",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nEvents",this.trackCollectionName),10000 , 0, 10000));

        plots1D.put(String.format("nMCParticles_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCParticles_per_event",this.trackCollectionName),1000 , 0, 10000));

        plots1D.put(String.format("nClusters_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nClusters_per_event",this.trackCollectionName),100 , 0, 100));

        plots1D.put(String.format("nTracks_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nTracks_per_event",this.trackCollectionName),100 , 0, 100));

        plots2D.put(String.format("nTracks_v_nClusters_per_event",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("nTracks_v_nClusters_per_event",this.trackCollectionName),100, 0, 100, 100, 0, 100));

        plots2D.put(String.format("nMCP_truth_clusters_v_clusters_per_event",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("nMCP_truth_clusters_v_clusters_per_event",this.trackCollectionName),100, 0, 100, 100, 0, 100));

        plots2D.put(String.format("nMCP_w_truth_tracks_v_nMCP_w_truth_clusters_per_event",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("nMCP_w_truth_tracks_v_nMCP_w_truth_clusters_per_event",this.trackCollectionName),100, 0, 100, 100, 0, 100));
        plots1D.put(String.format("mcp_w_truth_cluster_but_no_truth_tracks_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_w_truth_cluster_but_no_truth_tracks_momentum",this.trackCollectionName),500, 0, 5));

        plots1D.put(String.format("mcp_w_truth_cluster_but_no_truth_tracks_nSimTrackerHits",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_w_truth_cluster_but_no_truth_tracks_nSimTrackerHits",this.trackCollectionName),30, 0, 30));

        plots1D.put(String.format("mcp_w_truth_tracks_but_no_truth_cluster_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_w_truth_tracks_but_no_truth_cluster_momentum",this.trackCollectionName),500, 0, 5));

        plots1D.put(String.format("mcp_w_truth_cluster_but_no_truth_tracks_nSimCalHits",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_w_truth_cluster_but_no_truth_tracks_nSimCalHits",this.trackCollectionName), 30, 0, 30));

        plots1D.put(String.format("mcp_w_truth_tracks_AND_truth_cluster_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_w_truth_tracks_AND_truth_cluster_momentum",this.trackCollectionName),500, 0, 5));

        plots2D.put(String.format("mcp_w_truth_track_AND_truth_cluster_track_v_cluster_momentum",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_w_truth_track_AND_truth_cluster_track_v_cluster_momentum",this.trackCollectionName),500, 0, 5, 500, 0, 5));

        plots1D.put(String.format("mcps_all_checked_against_matcher_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcps_all_checked_against_matcher_momentum",this.trackCollectionName),500, 0, 5));

        plots2D.put(String.format("mcps_loop_nSimTrackerHits_v_nSimCalHits",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcps_loop_nSimTrackerHits_v_nSimCalHits",this.trackCollectionName),30, 0, 30, 30, 0, 30));

        plots1D.put(String.format("nMCP_w_truth_tracks_BUT_NO_truth_cluster_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCP_w_truth_tracks_BUT_NO_truth_cluster_per_event",this.trackCollectionName),100 , 0, 100));

        plots1D.put(String.format("nMCP_w_truth_cluster_BUT_NO_truth_tracks_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCP_w_truth_cluster_BUT_NO_truth_tracks_per_event",this.trackCollectionName),100 , 0, 100));

        plots1D.put(String.format("nMCP_w_truth_tracks_AND_truth_cluster_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCP_w_truth_tracks_AND_truth_cluster_per_event",this.trackCollectionName),100 , 0, 100));


//MCP LOOP OVER MATCHE RESULTS

        plots2D.put(String.format("mcp_loop_truth_cluster_matched_to_wrong_track_p_v_p",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_loop_truth_cluster_matched_to_wrong_track_p_v_p",this.trackCollectionName),500, 0, 5, 500, 0, 5));

        plots1D.put(String.format("mcp_loop_trackless_truth_cluster_matched_to_rogue_track_nSimTrackerHits",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_loop_trackless_truth_cluster_matched_to_rogue_track_nSimTrackerHits",this.trackCollectionName),30 , 0, 30));

        plots1D.put(String.format("mcp_loop_no_truth_tracks_truth_cluster_not_matched_to_track_nSimTrackerHits",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_loop_no_truth_tracks_truth_cluster_not_matched_to_track_nSimTrackerHits",this.trackCollectionName),30 , 0, 30));

        plots2D.put(String.format("mcp_loop_truth_track_matched_to_wrong_cluster_E_v_E",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_loop_truth_track_matched_to_wrong_cluster_E_v_E",this.trackCollectionName),500, 0, 5, 500, 0, 5));



        plots1D.put(String.format("mcp_loop_nEle_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_loop_nEle_per_event",this.trackCollectionName),100 , 0, 100));
        
        plots1D.put(String.format("mcp_loop_nPos_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_loop_nPos_per_event",this.trackCollectionName),100 , 0, 100));
        
        plots1D.put(String.format("mcp_loop_nPhotons_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_loop_nPhotons_per_event",this.trackCollectionName),100 , 0, 100));
        
        plots1D.put(String.format("mcp_loop_nCharged_particles_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_loop_nCharged_particles_per_event",this.trackCollectionName),100 , 0, 100));
        
        plots2D.put(String.format("mcp_loop_nCharged_particles_v_nTracks_per_event",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_loop_nCharged_particles_v_nTracks_per_event",this.trackCollectionName),100, 0, 100, 100, 0, 100));
        
        plots2D.put(String.format("mcp_loop_nMCPs_v_nClusters_per_event",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_loop_nMCPs_v_nClusters_per_event",this.trackCollectionName),100, 0, 100, 100, 0, 100));

//MATCHING PLOT

        plots1D.put(String.format("%s_mcp_truth_track_cluster_pair_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_mcp_truth_track_cluster_pair_momentum",this.trackCollectionName), 500, 0, 5));
        plots1D.put(String.format("%s_mcp_truth_track_cluster_pair_energy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_mcp_truth_track_cluster_pair_energy",this.trackCollectionName), 500, 0, 5));

        plots2D.put(String.format("mcp_truth_track_cluster_pair_ntrackersimhits_v_momentum",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_truth_track_cluster_pair_ntrackersimhits_v_momentum",this.trackCollectionName),20, 0, 20, 500, 0, 5));
        plots2D.put(String.format("mcp_truth_track_ntrackersimhits_v_mcp_momentum",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_truth_track_ntrackersimhits_v_mcp_momentum",this.trackCollectionName), 20, 0, 20, 500, 0, 5));

//MCP TRUTH MATCH TO TRACKS

        plots1D.put(String.format("mcp_nHits_in_Tracker",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_nHits_in_Tracker",this.trackCollectionName), 20, 0, 20));
        
        plots1D.put(String.format("mcp_ofInterest_nRawTrackerHits_per_track",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_ofInterest_nRawTrackerHits_per_track",this.trackCollectionName), 30, 0, 30));

        plots2D.put(String.format("mcp_ofInterest_momentum_v_nRawTrackerHits_per_track",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_ofInterest_momentum_v_nRawTrackerHits_per_track",this.trackCollectionName),500, 0, 5, 30, 0, 30));

        plots1D.put(String.format("mcp_mostHitsTrack_nHits",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_mostHitsTrack_nHits",this.trackCollectionName), 20, 0, 20));

        plots1D.put(String.format("mcp_nTracks",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_nTracks",this.trackCollectionName), 20, 0, 20));

        plots2D.put(String.format("mcp_momentum_v_nTracks",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_momentum_v_nTracks",this.trackCollectionName),500, 0, 5, 20, 0, 20));

        plots1D.put(String.format("nMCP_primary_FEEs_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCP_primary_FEEs_per_event",this.trackCollectionName), 500, 0, 500));

        plots1D.put(String.format("nMCP_radiative_FEEs_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCP_radiative_FEEs_per_event",this.trackCollectionName), 500, 0, 500));

        plots1D.put(String.format("nMCP_622_primary_daughters_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCP_622_primary_daughters_per_event",this.trackCollectionName), 500, 0, 500));

        plots1D.put(String.format("nMCP_623_primary_daughters_per_event",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCP_623_primary_daughters_per_event",this.trackCollectionName), 500, 0, 500));

        plots2D.put(String.format("nMCP_622or623_primary_daughters_v_primary_FEEs_per_event",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("nMCP_622or623_primary_daughters_v_primary_FEEs_per_event",this.trackCollectionName),500, 0, 500, 500, 0, 500));

        plots2D.put(String.format("nMCP_622and623_primary_daughters_and_primary_FEEs_v_nTracks_per_event",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("nMCP_622and623_primary_daughters_and_primary_FEEs_v_nTracks_per_event",this.trackCollectionName),500, 0, 500, 500, 0, 500));

        plots1D.put(String.format("nMCPs_on_track",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("nMCPs_on_track",this.trackCollectionName), 20, 0, 20));

        plots1D.put(String.format("tracks_unmatched_to_mcp_ofInterest_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("tracks_unmatched_to_mcp_ofInterest_momentum",this.trackCollectionName), 500, 0, 5));

        plots2D.put(String.format("mcp_nSimTrackerHits_v_sum_nHitsOnTrack",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_nSimTrackerHits_v_sum_nHitsOnTrack",this.trackCollectionName),20, 0, 20, 20, 0, 20));

        plots1D.put(String.format("mcp_FEE_nTracks_disambiguated",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_FEE_nTracks_disambiguated",this.trackCollectionName), 20, 0, 20));

        plots1D.put(String.format("mcp_nonFEE_nTracks_disambiguated",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_nonFEE_nTracks_disambiguated",this.trackCollectionName), 20, 0, 20));

        plots1D.put(String.format("mcp_nTracks_disambiguated",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_nTracks_disambiguated",this.trackCollectionName), 20, 0, 20));

        plots2D.put(String.format("mcp_momentum_v_nTracks_disambiguated",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_momentum_v_nTracks_disambiguated",this.trackCollectionName),500, 0, 5, 20, 0, 20));

        plots1D.put(String.format("mcp_nhits_on_track_disambiguated",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_nhits_on_track_disambiguated",this.trackCollectionName), 20, 0, 20));

        plots1D.put(String.format("mcp_most_hits_on_track_disambiguated",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_most_hits_on_track_disambiguated",this.trackCollectionName), 20, 0, 20));

        plots1D.put(String.format("mcp_FEEs_most_hits_on_track_disambiguated",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_FEEs_most_hits_on_track_disambiguated",this.trackCollectionName), 20, 0, 20));

        plots1D.put(String.format("mcp_NOT_FEEs_most_hits_on_track_disambiguated",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_NOT_FEEs_most_hits_on_track_disambiguated",this.trackCollectionName), 20, 0, 20));

        plots2D.put(String.format("mcp_momentum_v_best_track_momentum_disam",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_momentum_v_best_track_momentum_disam",this.trackCollectionName),500, 0, 5, 500, 0, 5));

        plots1D.put(String.format("mcp_momentum_best_track_momentum_ratio_disam",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_momentum_best_track_momentum_ratio_disam",this.trackCollectionName), 200, 0, 2));

        plots2D.put(String.format("mcp_nSimTrackerHits_most_nHitsOnTrack_disam",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("mcp_nSimTrackerHits_most_nHitsOnTrack_disam",this.trackCollectionName),20, 0, 20, 20, 0, 20));

        plots1D.put(String.format("mcp_FEE_wAtLeast_one_track_per_event_disamb",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_FEE_wAtLeast_one_track_per_event_disamb",this.trackCollectionName), 500, 0, 500));

        plots1D.put(String.format("mcp_NOT_FEE_wAtLeast_one_track_per_event_disamb",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("mcp_NOT_FEE_wAtLeast_one_track_per_event_disamb",this.trackCollectionName), 500, 0, 500));




//FINAL CLUSTER TRUTH MATCHING PLOTS

        //Cluster Positions in XY plane
        plots2D.put(String.format("ecal_cluster_positions_xy_plane",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("ecal_cluster_positions_xy_plane",this.trackCollectionName),1000, -500, 500,1000, -500, 500));

        plots2D.put(String.format("cluster_truthpositions_xy_plane",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("cluster_truthpositions_xy_plane",this.trackCollectionName),50, -280, 370, 18, -110, 124));
        
        plots1D.put(String.format("cluster_truth_stage_0_energy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_0_energy",this.trackCollectionName), 1000, 0, 10));

        plots1D.put(String.format("cluster_truth_stage_1_energy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_1_energy",this.trackCollectionName), 1000, 0, 10));

        plots2D.put(String.format( "cluster_truth_stage_1_mcpEndpointz_v_ds"), histogramFactory.createHistogram2D(String.format("cluster_truth_stage_1_mcpEndpointz_v_ds"),200, -100, 1900,1000, 0, 2000));

        plots1D.put(String.format("cluster_truth_stage_1_energy_ratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_1_energy_ratio",this.trackCollectionName), 1000, 0, 10));
        
        plots2D.put(String.format("cluster_truth_stage_1_cluster_v_mcp_energy",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("cluster_truth_stage_1_cluster_v_mcp_energy",this.trackCollectionName),1000, 0, 5,1000, 0, 5));

        plots1D.put(String.format("cluster_truth_stage_2_energy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_2_energy",this.trackCollectionName), 1000, 0, 10));
        
        plots2D.put(String.format( "cluster_truth_stage_2_mcpEndpointz_v_ds"), histogramFactory.createHistogram2D(String.format("cluster_truth_stage_2_mcpEndpointz_v_ds"),200, -100, 1900,1000, 0, 2000));
        
        plots1D.put(String.format("cluster_truth_stage_2_energy_ratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_2_energy_ratio",this.trackCollectionName), 1000, 0, 10));
        
        plots2D.put(String.format("cluster_truth_stage_2_cluster_v_mcp_energy",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("cluster_truth_stage_2_cluster_v_mcp_energy",this.trackCollectionName),1000, 0, 5,1000, 0, 5));

        plots2D.put(String.format("cluster_truth_stage_2_mcpPy_v_clustery",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("cluster_truth_stage_2_mcpPy_v_clustery",this.trackCollectionName),1000, -5, 5, 1000, -500, 500));

        plots1D.put(String.format("cluster_truth_stage_3_energy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_3_energy",this.trackCollectionName), 1000, 0, 10));
        
        plots2D.put(String.format( "cluster_truth_stage_3_mcpEndpointz_v_ds"), histogramFactory.createHistogram2D(String.format("cluster_truth_stage_3_mcpEndpointz_v_ds"),200, -100, 1900,1000, 0, 2000));
        
        plots1D.put(String.format("cluster_truth_stage_3_energy_ratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_3_energy_ratio",this.trackCollectionName), 1000, 0, 10));
        
        plots2D.put(String.format("cluster_truth_stage_3_cluster_v_mcp_energy",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("cluster_truth_stage_3_cluster_v_mcp_energy",this.trackCollectionName),1000, 0, 5,1000, 0, 5));

        plots1D.put(String.format("clusters_matched_to_n_mcps"), histogramFactory.createHistogram1D(String.format("clusters_matched_to_n_mcps"),  10, 0, 10));

        //Cluster Duplicates
        plots1D.put(String.format("cluster_truth_stage_3_duplicate_mcp_match_dx",trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_3_duplicate_mcp_match_dx",trackCollectionName),  1000, -1000, 1000));
        
        plots1D.put(String.format("cluster_truth_stage_3_duplicate_mcp_match_dy",trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_3_duplicate_mcp_match_dy",trackCollectionName),  1000, -1000, 1000));

        plots1D.put(String.format("cluster_truth_stage_3_cut_remaining_duplicate_mcp_match_dx",trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_3_cut_remaining_duplicate_mcp_match_dx",trackCollectionName),  1000, -1000, 1000));
        
        plots1D.put(String.format("cluster_truth_stage_3_cut_remaining_duplicate_mcp_match_dy",trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_3_cut_remaining_duplicate_mcp_match_dy",trackCollectionName),  1000, -1000, 1000));

        //Final Cut Clusters
        plots1D.put(String.format("cluster_truth_stage_final_energy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_final_energy",this.trackCollectionName), 1000, 0, 10));
        
        plots2D.put(String.format( "cluster_truth_stage_final_mcpEndpointz_v_ds"), histogramFactory.createHistogram2D(String.format("cluster_truth_stage_final_mcpEndpointz_v_ds"),200, -100, 1900,1000, 0, 2000));
        
        plots1D.put(String.format("cluster_truth_stage_final_energy_ratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_stage_final_energy_ratio",this.trackCollectionName), 1000, 0, 10));
        
        plots2D.put(String.format("cluster_truth_stage_final_cluster_v_mcp_energy",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("cluster_truth_stage_final_cluster_v_mcp_energy",this.trackCollectionName),1000, 0, 5,1000, 0, 5));

        plots1D.put(String.format("cluster_truth_energy",trackCollectionName), histogramFactory.createHistogram1D(String.format("cluster_truth_energy",trackCollectionName),  100, 0, 5));



/////////////////////////////

        plots1D.put(String.format("cluster_event_multiplicity"), histogramFactory.createHistogram1D(String.format("cluster_event_multiplicity"),  20, 0, 20));
        plots1D.put(String.format("track_event_multiplicity"), histogramFactory.createHistogram1D(String.format("track_event_multiplicity"),  20, 0, 20));
        plots2D.put(String.format("track_v_cluster_event_multiplicity"), histogramFactory.createHistogram2D(String.format("track_v_cluster_event_multiplicity"), 20, 0, 20, 20, 0, 20));

        //Plot the XY acceptance of the Ecal by adding half the crystal width
        //to the cluster xy positions
        plots2D.put(String.format("ecal_crystal_acceptance_xy"), histogramFactory.createHistogram2D(String.format("ecal_crystal_acceptance_xy"),800, -400, 400, 240, -120, 120));


//SCORING PLANE PLOTS
        //Plots showing residuals between track at Ecal and truth-matched
        //scoring plane hit that has been extrapolated to the Ecal
        plots1D.put(String.format("%s_ele_track_scoringplane_hit_dx",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_scoringplane_hit_dx",this.trackCollectionName), 800, -400, 400));
        plots1D.put(String.format("%s_ele_track_scoringplane_hit_dy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_scoringplane_hit_dy",this.trackCollectionName), 800, -400, 400));
        plots1D.put(String.format("%s_ele_track_scoringplane_hit_dz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_scoringplane_hit_dz",this.trackCollectionName), 800, -400, 400));
        plots1D.put(String.format("%s_ele_track_scoringplane_hit_dr",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_scoringplane_hit_dr",this.trackCollectionName), 800, -400, 400));
        plots1D.put(String.format("%s_ele_track_scoringplane_hit_dt",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_scoringplane_hit_dt",this.trackCollectionName), 800, -400, 400));

        plots1D.put(String.format("%s_pos_track_scoringplane_hit_dx",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_scoringplane_hit_dx",this.trackCollectionName), 800, -400, 400));
        plots1D.put(String.format("%s_pos_track_scoringplane_hit_dy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_scoringplane_hit_dy",this.trackCollectionName), 800, -400, 400));
        plots1D.put(String.format("%s_pos_track_scoringplane_hit_dz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_scoringplane_hit_dz",this.trackCollectionName), 800, -400, 400));
        plots1D.put(String.format("%s_pos_track_scoringplane_hit_dr",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_scoringplane_hit_dr",this.trackCollectionName), 800, -400, 400));
        plots1D.put(String.format("%s_pos_track_scoringplane_hit_dt",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_scoringplane_hit_dt",this.trackCollectionName), 800, -400, 400));

            //scoringplane momentum components
        plots1D.put(String.format("%s_ele_track_scoringplane_hit_px",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_scoringplane_hit_px",this.trackCollectionName), 600, -1, 5));
        plots1D.put(String.format("%s_ele_track_scoringplane_hit_py",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_scoringplane_hit_py",this.trackCollectionName), 600, -1, 5));
        plots1D.put(String.format("%s_ele_track_scoringplane_hit_pz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_scoringplane_hit_pz",this.trackCollectionName), 600, -1, 5));

        plots1D.put(String.format("%s_pos_track_scoringplane_hit_px",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_scoringplane_hit_px",this.trackCollectionName), 600, -1, 5));
        plots1D.put(String.format("%s_pos_track_scoringplane_hit_py",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_scoringplane_hit_py",this.trackCollectionName), 600, -1, 5));
        plots1D.put(String.format("%s_pos_track_scoringplane_hit_pz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_scoringplane_hit_pz",this.trackCollectionName), 600, -1, 5));
            //track v scoringplane momentum
        plots2D.put(String.format("%s_ele_scoringplaneHit_v_truth_track_p",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_scoringplaneHit_v_truth_track_p",this.trackCollectionName),600, -1, 5, 600, -1, 5));
        plots2D.put(String.format("%s_pos_scoringplaneHit_v_truth_track_p",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_scoringplaneHit_v_truth_track_p",this.trackCollectionName),600, -1, 5, 600, -1, 5));


        //Track extrapolation to Ecal: Momentum vs truth-extrap position
        //residuals
        plots2D.put(String.format("%s_ele_RK4_scoringplanehit_to_ecal_ZvP",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_RK4_scoringplanehit_to_ecal_ZvP",this.trackCollectionName), 1500, 0, 1500,300,0,3));

        plots2D.put(String.format("%s_pos_RK4_scoringplanehit_to_ecal_ZvP",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_RK4_scoringplanehit_to_ecal_ZvP",this.trackCollectionName), 1500, 0, 1500,300,0,3));

//TRACK RECONSTRUCTION EFFICIENCY, ETC.

        //Track reconstruction efficiency
        plots1D.put(String.format("%s_track_reco_efficiency",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_track_reco_efficiency",this.trackCollectionName), 10, 0, 10));
        //Number of duplicate MCPs matched to reco Tracks
        plots1D.put(String.format("%s_n_duplicate_TrackMCP_matches",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_n_duplicate_TrackMCP_matches",this.trackCollectionName), 10, 0, 10));

        //Checking the track-cluster matching fake rate and matching efficiency
        plots1D.put(String.format("%s_ele_fakeRate",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_fakeRate",this.trackCollectionName), 10, 0, 10));
        plots1D.put(String.format("%s_pos_fakeRate",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_fakeRate",this.trackCollectionName), 10, 0, 10));
        plots1D.put(String.format("%s_ele_Efficiency",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_Efficiency",this.trackCollectionName), 10, 0, 10));
        plots1D.put(String.format("%s_pos_Efficiency",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_Efficiency",this.trackCollectionName), 10, 0, 10));


//TRUTH MATCHED TRACKS WITH MCPARTICLES

        plots2D.put(String.format("%s_ele_mcp_v_truth_track_momentum",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_mcp_v_truth_track_momentum",this.trackCollectionName),600, -1, 5, 600, -1, 5));
        plots2D.put(String.format("%s_pos_mcp_v_truth_track_momentum",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_mcp_v_truth_track_momentum",this.trackCollectionName),600, -1, 5, 600, -1, 5));

//PLOTS FOR TRUTH TRACK CLUSTER PAIRS WITH DIFFERENT ACCEPTANCES

        //TRUTH TRACK CLUSTER PAIRS
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_momentum",this.trackCollectionName), 1000, 0, 5));
            //momentum
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_momentum",this.trackCollectionName), 1000, 0, 5));

        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_px",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_px",this.trackCollectionName), 600, -1, 5));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_py",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_py",this.trackCollectionName), 600, -1, 5));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_pz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_pz",this.trackCollectionName), 600, -1, 5));

        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_px",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_px",this.trackCollectionName), 600, -1, 5));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_py",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_py",this.trackCollectionName), 600, -1, 5));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_pz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_pz",this.trackCollectionName), 600, -1, 5));

        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_dx",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_dx",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_dy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_dy",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_dx",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_dx",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_dy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_dy",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_dz",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_dz",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_dz",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_dz",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_EdivP",trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_EdivP",trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_clusterMCP_Eratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_clusterMCP_Eratio",this.trackCollectionName), 1000, 0, 10));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_clusterMCP_Eratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_clusterMCP_Eratio",this.trackCollectionName), 1000, 0, 10));
        plots2D.put(String.format("%s_ele_truth_track_cluster_pair_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_truth_track_cluster_pair_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
        plots2D.put(String.format("%s_pos_truth_track_cluster_pair_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_truth_track_cluster_pair_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));


        //TRUTH TRACK CLUSTER PAIRS INSIDE ECAL
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_insideEcal_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_insideEcal_momentum",this.trackCollectionName), 1000, 0, 5));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_insideEcal_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_insideEcal_momentum",this.trackCollectionName), 1000, 0, 5));


        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_insideEcal_dx",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_insideEcal_dx",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_insideEcal_dy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_insideEcal_dy",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_insideEcal_dx",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_insideEcal_dx",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_insideEcal_dy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_insideEcal_dy",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_insideEcal_dz",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_insideEcal_dz",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_insideEcal_dz",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_insideEcal_dz",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_insideEcal_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_insideEcal_EdivP",trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_insideEcal_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_insideEcal_EdivP",trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_insideEcal_clusterMCP_Eratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_insideEcal_clusterMCP_Eratio",this.trackCollectionName), 1000, 0, 10));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_insideEcal_clusterMCP_Eratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_insideEcal_clusterMCP_Eratio",this.trackCollectionName), 1000, 0, 10));

            //cluster and track xy plots
            
        plots2D.put(String.format("%s_ele_truth_track_cluster_pair_insideEcal_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_truth_track_cluster_pair_insideEcal_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
        plots2D.put(String.format("%s_pos_truth_track_cluster_pair_insideEcal_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_truth_track_cluster_pair_insideEcal_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
        plots2D.put(String.format("%s_ele_truth_track_cluster_pair_insideEcal_cluster_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_truth_track_cluster_pair_insideEcal_cluster_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
        plots2D.put(String.format("%s_pos_truth_track_cluster_pair_insideEcal_cluster_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_truth_track_cluster_pair_insideEcal_cluster_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));

        //TRUTH TRACK CLUSTER PAIRS OUTSIDE ECAL
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_momentum",this.trackCollectionName), 1000, 0, 5));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_momentum",this.trackCollectionName), 1000, 0, 5));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_Pz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_Pz",this.trackCollectionName), 600, -1, 5));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_Pz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_Pz",this.trackCollectionName), 600, -1, 5));

        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_dx",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_dx",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_dy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_dy",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_dx",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_dx",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_dy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_dy",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_dz",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_dz",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_dz",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_dz",trackCollectionName),  800, -200, 200));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_EdivP",trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_EdivP",trackCollectionName),  1000, 0, 10));
        plots1D.put(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_clusterMCP_Eratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_clusterMCP_Eratio",this.trackCollectionName), 1000, 0, 10));
        plots1D.put(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_clusterMCP_Eratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_clusterMCP_Eratio",this.trackCollectionName), 1000, 0, 10));
        plots2D.put(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
        plots2D.put(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
        plots2D.put(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_cluster_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_cluster_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
        plots2D.put(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_cluster_xypos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_cluster_xypos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));


        //track and truthtrack momentum
        plots1D.put(String.format("%s_ele_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_momentum",this.trackCollectionName), 1000, 0, 5));
        plots1D.put(String.format("%s_pos_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_momentum",this.trackCollectionName), 1000, 0, 5));

        plots1D.put(String.format("%s_ele_delta_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_delta_momentum",this.trackCollectionName), 400, 0, 4));
        plots1D.put(String.format("%s_pos_delta_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_delta_momentum",this.trackCollectionName), 400, 0, 4));

        plots1D.put(String.format("%s_ele_truth_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_momentum",this.trackCollectionName), 1000, 0, 5));
        plots1D.put(String.format("%s_pos_truth_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_momentum",this.trackCollectionName), 1000, 0, 5));
        plots1D.put(String.format("%s_pos_truth_track_MCP_momentum_ratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_MCP_momentum_ratio",this.trackCollectionName), 1000, 0, 5));
        plots1D.put(String.format("%s_ele_truth_track_MCP_momentum_ratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_MCP_momentum_ratio",this.trackCollectionName), 1000, 0, 5));


        //duplicate track momentum
        plots1D.put(String.format("%s_ele_duplicate_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_duplicate_track_momentum",this.trackCollectionName), 1000, 0, 5));
        plots1D.put(String.format("%s_pos_duplicate_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_duplicate_track_momentum",this.trackCollectionName), 1000, 0, 5));
        //Checking E/P for track+cluster pair
        

        //MCP hits along z
        plots1D.put(String.format("%s_EcalCluster_Simcalhit_MCParticle_origin_z",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_EcalCluster_Simcalhit_MCParticle_origin_z",this.trackCollectionName), 1500, -1000, 2000));

        //Hit multiplicity for truth matching
        plots1D.put(String.format("%s_ele_track_maxMCPmultiplicity",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_maxMCPmultiplicity",this.trackCollectionName),16, 0, 16));
        plots1D.put(String.format("%s_pos_track_maxMCPmultiplicity",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_maxMCPmultiplicity",this.trackCollectionName),16, 0, 16));
        plots2D.put(String.format("%s_ele_track_p_v_maxMCPmultiplicity",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_track_p_v_maxMCPmultiplicity",this.trackCollectionName), 1000, 0, 5, 16, 0, 16));
        plots2D.put(String.format("%s_pos_track_p_v_maxMCPmultiplicity",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_track_p_v_maxMCPmultiplicity",this.trackCollectionName), 1000, 0, 5, 16, 0, 16));
        plots2D.put(String.format("%s_pos_track_p_v_mcp_p",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_track_p_v_mcp_p",this.trackCollectionName), 1000, 0, 5, 1000, 0, 5));
        plots2D.put(String.format("%s_ele_track_p_v_mcp_p",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_track_p_v_mcp_p",this.trackCollectionName), 1000, 0, 5, 1000, 0, 5));


        //track positions at Ecal in XY plane
        plots2D.put(String.format("%s_ele_track_xypos_at_ecal",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_track_xypos_at_ecal",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
        plots2D.put(String.format("%s_pos_track_xypos_at_ecal",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_track_xypos_at_ecal",this.trackCollectionName),1000, -500, 500,1000, -500, 500));

        plots2D.put(String.format("%s_ele_truth_track_xypos_at_ecal",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_truth_track_xypos_at_ecal",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
        plots2D.put(String.format("%s_pos_truth_track_xypos_at_ecal",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_truth_track_xypos_at_ecal",this.trackCollectionName),1000, -500, 500,1000, -500, 500));


        //Check track quality
        plots1D.put(String.format("%s_ele_track_chi2divndf",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_chi2divndf",this.trackCollectionName), 200, 0, 200));
        plots1D.put(String.format("%s_pos_track_chi2divndf",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_chi2divndf",this.trackCollectionName), 200, 0, 200));

    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void startOfData() {
        System.out.println("Starting job");
        bookHistograms();

        //init matcher being tested
        matcher = TrackClusterMatcherFactory.create(trackClusterMatcherAlgo);
        matcher.setTrackCollectionName(this.trackCollectionName);
        matcher.enablePlots(true);
    }

    public void endOfData() {

        matcher.saveHistograms();
        saveHistograms();

        System.out.println("number of MCParticle matches evaluated: " + mcparticlesEvaluated.size());
        System.out.println("number of good matches: " + goodMatches.size());
        System.out.println("number of bad matches: " + badMatches.size());
        System.out.println("number of missed matches: " + missedMatches.size());
        System.out.println("number of unknown matches mcpToRogueTrack: " + unknownMatches_mcpToRogueTrack.size());
        System.out.println("number of unknown matches mcpToRogueCluster: " + unknownMatches_mcpToRogueCluster.size());
        System.out.println("number of not trackable matches: " + unmatchedUntrackableMCPs.size());
        System.out.println("number of photons matched to a track: " + photonsMatchedToTrack.size());
        System.out.println("number of good photons: " + truthPhotonClusters.size());
        System.out.println("number of rogue matches: " + rogueMatches.size());
        System.out.println("number of rogue Tracks: " + rogueTracks.size());
        System.out.println("number of rogue photons: " + iddPhotons.size());

        /*
        System.out.println("number of good matches: " + nGoodMatches);
        System.out.println("number of bad matches: " + nBadMatches);
        System.out.println("number of good matches ele: " + nGoodMatches_ele);
        System.out.println("number of bad matches ele: " + nBadMatches_ele);
        System.out.println("number of good matches pos: " + nGoodMatches_pos);
        System.out.println("number of bad matches pos: " + nBadMatches_pos);
       
        //track reco efficiency
        trackEfficiency = NrecoTruthTracks/NpossibleTracks;
        for(int i=0; i < Math.round(trackEfficiency*100); i++)
            plots1D.get(String.format("%s_track_reco_efficiency",this.trackCollectionName)).fill(1.0);
        System.out.println("LOOK! nDuplicates = " + nDuplicates);
        for(int i = 0; i < Math.round(nDuplicates); i++)
            plots1D.get(String.format("%s_n_duplicate_TrackMCP_matches",this.trackCollectionName)).fill(1.0);

        //
        for(int i=0; i < Math.round(fakerate_ele*100); i++){
            System.out.println("Filling Histogram fakerate");
            plots1D.get(String.format("%s_ele_fakeRate",this.trackCollectionName)).fill(1.0);
        }
        for(int i=0; i < Math.round(fakerate_pos*100); i++)
            plots1D.get(String.format("%s_pos_fakeRate",this.trackCollectionName)).fill(1.0);
        for(int i=0; i < Math.round(efficiency_ele*100); i++)
            plots1D.get(String.format("%s_pos_Efficiency",this.trackCollectionName)).fill(1.0);
        for(int i=0; i < Math.round(efficiency_pos*100); i++)
            plots1D.get(String.format("%s_ele_Efficiency",this.trackCollectionName)).fill(1.0);
            //
        System.out.println("goodMatch_ele: " + goodMatch_ele);
        System.out.println("fakeMatch_ele: " + fakeMatch_ele);
        System.out.println("noMatch_ele: " + noMatch_ele);
        System.out.println("missedMatch_ele: " + missedMatch_ele);
        System.out.println("unknownMatch_ele: " + unknownMatch_ele);
        System.out.println("goodMatch_pos: " + goodMatch_pos);
        System.out.println("fakeMatch_pos: " + fakeMatch_pos);
        System.out.println("noMatch_pos: " + noMatch_pos);
        System.out.println("missedMatch_pos: " + missedMatch_pos);
        System.out.println("unknownMatch_pos: " + unknownMatch_pos);

        System.out.println("noMatchTruthTrack_ele: " + noMatchTruthTrack_ele);
        System.out.println("unknownMatchTruthTrack_ele: " + unknownMatchTruthTrack_ele);
        System.out.println("unknownMatchTruthCluster_ele: " + unknownMatchTruthCluster_ele);
        System.out.println("truthTrackOutsideEcal_ele: " + truthTrackOutsideEcal_ele);
        System.out.println("trackOutsideEcal_ele: " + trackOutsideEcal_ele);
        System.out.println("noMatchTruthTrack_pos: " + noMatchTruthTrack_pos);
        System.out.println("unknownMatchTruthTrack_pos: " + unknownMatchTruthTrack_pos);
        System.out.println("unknownMatchTruthCluster_pos: " + unknownMatchTruthCluster_pos);
        System.out.println("truthTrackOutsideEcal_pos: " + truthTrackOutsideEcal_pos);
        System.out.println("trackOutsideEcal_pos: " + trackOutsideEcal_pos);
        */
    }


    private void drawEcalFace(List<Cluster> clusters){
        //Define line that draws the beam gap Ecal crystal edge
        int nx = 46;
        int ny = 5;
        double crystalface = 13.0; //mm
        
        for(Cluster cluster : clusters){

            double clusterx = cluster.getPosition()[0];
            double clustery = cluster.getPosition()[1];
            double clusterz = cluster.getPosition()[2];
            
            double leftx = clusterx - (crystalface/2);
            double rightx = clusterx + (crystalface/2);
            double upy = clustery + (crystalface/2);
            double downy = clustery - (crystalface/2);

            plots2D.get("ecal_crystal_acceptance_xy").fill(clusterx,clustery);
            plots2D.get("ecal_crystal_acceptance_xy").fill(leftx,clustery);
            plots2D.get("ecal_crystal_acceptance_xy").fill(rightx,clustery);
            plots2D.get("ecal_crystal_acceptance_xy").fill(clusterx,upy);
            plots2D.get("ecal_crystal_acceptance_xy").fill(clusterx,downy);

            plots2D.get("ecal_crystal_acceptance_xy").fill(rightx,upy);
            plots2D.get("ecal_crystal_acceptance_xy").fill(rightx,downy);
            plots2D.get("ecal_crystal_acceptance_xy").fill(leftx,downy);
            plots2D.get("ecal_crystal_acceptance_xy").fill(leftx,upy);
        }
    }

    public List<MCParticle> getPossibleTrackMCPs(EventHeader event, int minHitsOnTrack){
        //Loop over all simhits in the LCIO file and check how many Tracks
        //could possibly be reconstructed...where a possible track is counted
        //a MC particle leaves minHitsOnTrack hits
        List<SimTrackerHit> simhits =  event.get(SimTrackerHit.class, "TrackerHits");
        Map<MCParticle, int[]> nMCPhits = new HashMap<MCParticle, int[]>();
        for(SimTrackerHit simhit : simhits){
            MCParticle mcp = simhit.getMCParticle();
            if(!nMCPhits.containsKey(mcp)){
                nMCPhits.put(mcp, new int[1]);
                nMCPhits.get(mcp)[0] = 0;
            }
            nMCPhits.get(mcp)[0]++;
        }

        List<MCParticle> possibleTracks = new ArrayList<MCParticle>();
        for(Map.Entry<MCParticle, int[]> entry : nMCPhits.entrySet()){
            if(entry.getValue()[0] > minHitsOnTrack ){
                possibleTracks.add(entry.getKey());
            }
        }

        return possibleTracks;
    }

    protected void process(EventHeader event) {


        //count objects event by event
        System.out.println("Counting for Event " + event.getEventNumber());
        plots1D.get("nEvents").fill(1);
        
        //MCPs
        List<MCParticle> allmcps = event.get(MCParticle.class,"MCParticle");
        plots1D.get("nMCParticles_per_event").fill(allmcps.size());
        System.out.println("MCPs for Event " + event.getEventNumber() + ": " + allmcps.size());

        //Get EcalClusters from event
        List<Cluster> clusters = event.get(Cluster.class, ecalClustersCollectionName);
        plots1D.get("nClusters_per_event").fill(clusters.size());
        System.out.println("Clusters for Event " + event.getEventNumber() + ": " + clusters.size());

        // Get collection of tracks from event
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        plots1D.get("nTracks_per_event").fill(tracks.size());
        System.out.println("Tracks for Event " + event.getEventNumber() + ": " + tracks.size());

        plots2D.get("nTracks_v_nClusters_per_event").fill(tracks.size(),clusters.size());

        cuts.setTrackClusterTimeOffset(44.8);

        //Truth Match Clusters to MCParticles    
        Map<MCParticle, Cluster> mcpClustersMap = new HashMap<MCParticle, Cluster>();
        Map<Cluster, MCParticle> clustersMCPMap = new HashMap<Cluster, MCParticle>();
        getClusterMcpMap(clusters, event, false, mcpClustersMap, clustersMCPMap);
        plots2D.get("nMCP_truth_clusters_v_clusters_per_event").fill(mcpClustersMap.size(),clusters.size());

        List<Cluster> truthClusters = new ArrayList<Cluster>();
        for(Map.Entry<Cluster,MCParticle> entry : clustersMCPMap.entrySet()){
            Cluster truthcluster = entry.getKey();
            double clustx = truthcluster.getPosition()[0];
            double clusty = truthcluster.getPosition()[1];
            double clustz = truthcluster.getPosition()[2];
            double clusterEnergy = truthcluster.getEnergy();
            double energyRatio = clusterEnergy/clustersMCPMap.get(truthcluster).getEnergy();

            plots1D.get(String.format("cluster_truth_energy",trackCollectionName)).fill(clusterEnergy);
            plots2D.get(String.format("cluster_truthpositions_xy_plane")).fill(clustx,clusty);
            truthClusters.add(entry.getKey());
        }
        drawEcalFace(truthClusters);


        //Get Kalman Track Data
        hitToRotated = TrackUtils.getHitToRotatedTable(event);
        hitToStrips = TrackUtils.getHitToStripsTable(event);
        List<TrackData> TrackData;
        List<LCRelation> trackRelations;
        if (this.trackCollectionName.contains("KalmanFullTracks")) {
            TrackData = event.get(TrackData.class, "KFTrackData");
            trackRelations = event.get(LCRelation.class, "KFTrackDataRelations");
            for (LCRelation relation : trackRelations) {
                if (relation != null && relation.getTo() != null){
                    TrktoData.add(relation.getFrom(), relation.getTo());
                }
            }
        }

        //Get Map of MCParticles to truth matched Tracks (and nhits on Track)
        Map<MCParticle, Map<Track, Integer>> mcpTracksMap = new HashMap<MCParticle, Map<Track, Integer>>();
        Map<Track, MCParticle> tracksMCPMap = new HashMap<Track, MCParticle>();
        getMCPTracks(event, tracks, mcpTracksMap, tracksMCPMap);
        plots2D.get("nMCP_w_truth_tracks_v_nMCP_w_truth_clusters_per_event").fill(mcpTracksMap.size(),mcpClustersMap.size());


        //Combine Cluster and Track MCParticle Map into one map
        Map<MCParticle, Map<Track, Cluster>> truthPairsMap = new HashMap<MCParticle, Pair<Track, Cluster>>();
        Map<MCParticle, Pair<Cluster, List<Track>>> comboMap = new HashMap<MCParticle, Pair<Cluster, List<Track>>>();
        Set<MCParticle> mcpsOfInterest = new HashSet<MCParticle>();

        for(Map.Entry<MCParticle, Map<Track, Integer>> entry : mcpTracksMap.entrySet()){
            MCParticle mcp = entry.getKey();
            mcpsOfInterest.add(mcp);
        }
        for(Map.Entry<MCParticle, Cluster> entry : mcpClustersMap.entrySet()){
            MCParticle mcp = entry.getKey();
            mcpsOfInterest.add(mcp);
        }

        int nMcp_wo_cluster = 0;
        int nMcp_wo_track = 0;
        int nMcp_w_both = 0;
        //build full map of MCPs to check against matcher results
        for(MCParticle mcp : mcpsOfInterest){
            List<Track> mcpTracks = new ArrayList<Track>();
            Cluster mcpCluster = null;
            //get tracks matched to this mcp
            if(mcpTracksMap.containsKey(mcp)){
                for(Map.Entry<Track, Integer> subentry : mcpTracksMap.get(mcp).entrySet()){
                    mcpTracks.add(subentry.getKey());
                }
            }

            //get cluster matched to this mcp
            if(mcpClustersMap.containsKey(mcp))
                mcpCluster = mcpClustersMap.get(mcp);

            Pair<Cluster, List<Track>> pairs = new Pair<Cluster,List<Track>>(mcpCluster, mcpTracks);
            comboMap.put(mcp, pairs);
            if(mcpTracks.size() == 0 && mcpCluster != null){
                plots1D.get("mcp_w_truth_cluster_but_no_truth_tracks_momentum").fill(mcp.getMomentum().magnitude());
                plots1D.get("mcp_w_truth_cluster_but_no_truth_tracks_nSimTrackerHits").fill(getNSimTrackerHits(event, mcp));
                nMcp_wo_track = nMcp_wo_track + 1;
            }
            if(mcpTracks.size() > 0 && mcpCluster == null){
                plots1D.get("mcp_w_truth_tracks_but_no_truth_cluster_momentum").fill(mcp.getMomentum().magnitude());
                plots1D.get("mcp_w_truth_cluster_but_no_truth_tracks_nSimCalHits").fill(getNSimCalHits(event, "EcalHits", mcp));
                nMcp_wo_cluster = nMcp_wo_cluster + 1;
            }

            if(mcpTracks.size() > 0 && mcpCluster != null){
                Track bestTrack = getMcpBestTrack(mcp,mcpTracksMap);
                truthPairsMap.put(mcp, new Pair<Track,Cluster>(bestTrack, mclCluster));
                int charge = -1* (int)Math.signum(bestTrack.getTrackStates().get(0).getOmega());
                double[] trackP = bestTrack.getTrackStates().get(bestTrack.getTrackStates().size()-1).getMomentum();
                double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
                plots1D.get("mcp_w_truth_tracks_AND_truth_cluster_momentum").fill(mcp.getMomentum().magnitude());
                plots2D.get("mcp_w_truth_track_AND_truth_cluster_track_v_cluster_momentum").fill(trackPmag,mcpCluster.getEnergy());

                List<Double> trackPos = getTrackPositionAtEcal(bestTrack);
                double clusterx = mcpCluster.getPosition()[0];
                double clustery = mcpCluster.getPosition()[1];
                double clusterz = mcpCluster.getPosition()[2];

                if(charge < 0){
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_dx",trackCollectionName)).fill(clusterx - trackPos.get(0));
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_dy",trackCollectionName)).fill(clustery - trackPos.get(1));
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_dz",trackCollectionName)).fill(clusterz - trackPos.get(2));
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_EdivP",trackCollectionName)).fill(mcpCluster.getEnergy()/trackPmag);
                }
                else{
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_dx",trackCollectionName)).fill(clusterx - trackPos.get(0));
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_dy",trackCollectionName)).fill(clustery - trackPos.get(1));
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_dz",trackCollectionName)).fill(clusterz - trackPos.get(2));
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_EdivP",trackCollectionName)).fill(mcpCluster.getEnergy()/trackPmag);

                }

                nMcp_w_both = nMcp_w_both + 1;
            }

            plots1D.get("mcps_all_checked_against_matcher_momentum").fill(mcp.getMomentum().magnitude());
            plots2D.get("mcps_loop_nSimTrackerHits_v_nSimCalHits").fill(getNSimTrackerHits(event, mcp),getNSimCalHits(event,"EcalHits", mcp));
        }
        plots1D.get("nMCP_w_truth_tracks_BUT_NO_truth_cluster_per_event").fill(nMcp_wo_cluster);
        plots1D.get("nMCP_w_truth_cluster_BUT_NO_truth_tracks_per_event").fill(nMcp_wo_track);
        plots1D.get("nMCP_w_truth_tracks_AND_truth_cluster_per_event").fill(nMcp_w_both);



        //Caluclate track+cluster pair position residuals as function of
        //momentum and create residual parameterization file to be used in
        //matcher algorithm class



        //Feed Track and Cluster collections to matcher algorithm to get
        //algorithm matched Tracks and Clusters
        Map<Track, Cluster> matchedTrackClusterMap = new HashMap<Track,Cluster>();
        List<List<Track>> trackCollections = new ArrayList<List<Track>>();
        trackCollections.add(tracks);
        matchedTrackClusterMap = matcher.matchTracksToClusters(event, trackCollections, clusters, cuts, -1, false, true, ecal, beamEnergy);
        /*
        //Charged Track matcher Evaluation
        Map<Track, Cluster> goodMatches = new HashMap<Track,Cluster>();
        Map<Track, Cluster> badMatches = new HashMap<Track,Cluster>();
        Map<Track, Cluster> unknownMatches = new HashMap<Track, Cluster>();
        Map<Track, Cluster> unmatchedUntrackableMCPs = new HashMap<Track, Cluster>();
        Map<Track, Cluster> rogueMatches = new HashMap<Track,Cluster>();
        List<Track> rogueTracks = new ArrayList<Track>();
        List<Cluster> iddPhotons = new ArrayList<Cluster>();
        */

        //Loop over MCParticles
        int nele = 0;
        int npos = 0;
        int nphoton = 0;

        int nGoodMatches = 0;
        int nBadMatches = 0;
        int nMissedMatches = 0;
        int nUnknownMatches_mcpToRogueTrack = 0;
        int nUnknownMatches_mcpToRogueCluster = 0;
        int nUnmatchedUntrackableMCPs = 0;
        int nPhotonsMatchedToTrack = 0;
        int nTruthPhotons = 0;
        int nRogueMatches = 0;
        int nIddPhotons = 0;
        int nRogueTracks = 0;
        int nTracklessMCPClusterMatchedToRogueTrack = 0;
        int nTracklessMCPClusterNotMatchedToTrack = 0;
        int nClusterlessMCPtrackMatchedToRogueCluster = 0;
        int nClusterlessMCPtrackNotMatchedToCluster = 0;
        int nMCParticlesEvaluated = 0;

        for(Map.Entry<MCParticle, Pair<Cluster, List<Track>>> entry : comboMap.entrySet()){
            MCParticle mcp = entry.getKey();
            int nmcpHits = getNSimTrackerHits(event, mcp);
            Pair<Cluster, List<Track>> pair = entry.getValue();
            Cluster mcpCluster = pair.getFirstElement();
            List<Track> mcpTracks = pair.getSecondElement();

            //Check charged MCP's first
            if(Math.abs(mcp.getPDGID()) == 11){

                if(mcp.getPDGID() == 11){
                    nele = nele + 1;
                }
                if(mcp.getPDGID() == -11){
                    npos = npos + 1;
                }

                //If MCP does have a truth cluster, we can check to see if that
                //cluster was matched to any tracks by the macher algorithm
                if(mcpCluster != null){

                    //Check if mcpCluster is matched to track in algorithm matcher
                    if(matchedTrackClusterMap.containsValue(mcpCluster)){
                        //find the track matched to this cluster by algorithm,
                        //if any
                        Track algTrack = null;
                        for(Map.Entry<Track, Cluster> subentry : matchedTrackClusterMap.entrySet()){
                            if(subentry.getValue() == mcpCluster){
                                algTrack = subentry.getKey();
                                break;
                            }
                        }
                        double[] trackP = TrackUtils.getTrackStateAtLocation(algTrack,TrackState.AtIP).getMomentum();
                        double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));

                        //If we know the truth info for this algTrack, we can
                        //determine if the match is correct. 
                        if(tracksMCPMap.containsKey(algTrack)){
                            //if alg track is one of the tracks matched to this
                            //mcp, good match
                            if(mcpTracks.contains(algTrack)){
                                goodMatches.put(mcp, new Pair<Track, Cluster>(algTrack, mcpCluster));
                                nGoodMatches = nGoodMatches + 1;
                            }
                            else{
                                badMatches.put(mcp, new Pair<Track, Cluster>(algTrack, mcpCluster));
                                nBadMatches = nBadMatches + 1;
                                plots2D.get("mcp_loop_truth_cluster_matched_to_wrong_track_p_v_p").fill(mcp.getMomentum().magnitude(),trackPmag);
                            }
                        }

                        //if we dont know the truth info for this algTrack
                        else{
                            //If this algTrack has no truth information, and it
                            //was matched to the cluster of a MCP that does
                            //have truth tracks, then we know this algTrack
                            //match is wrong.
                            if(mcpTracks.size() > 0){
                                badMatches.put(mcp, new Pair<Track, Cluster>(algTrack, mcpCluster));
                                nBadMatches = nBadMatches + 1;
                                plots2D.get("mcp_loop_truth_cluster_matched_to_wrong_track_p_v_p").fill(mcp.getMomentum().magnitude(),trackPmag);
                            }
                            //If MCP has no truth track, we cant be sure that
                            //this algTrack is wrong
                            else{
                                if(getNSimTrackerHits(event,mcp) == 0){
                                    //badMatches.put(mcp, new Pair<Track, Cluster>(algTrack, mcpCluster));
                                    //nBadMatches = nBadMatches + 1;
                                    //tracklessMCPClusterMatchedToRogueTrack.put(mcp, new Pair<Track, Cluster>(algTrack, mcpCluster));
                                    nTracklessMCPClusterMatchedToRogueTrack = nTracklessMCPClusterMatchedToRogueTrack + 1;
                                }
                                else{
                                    unknownMatches_mcpToRogueTrack.put(mcp, new Pair<Track, Cluster>(algTrack, mcpCluster));
                                    nUnknownMatches_mcpToRogueTrack = nUnknownMatches_mcpToRogueTrack + 1; 
                                    plots1D.get("mcp_loop_trackless_truth_cluster_matched_to_rogue_track_nSimTrackerHits").fill(nmcpHits);
                                }
                            }
                        }
                    }

                    else{
                        //mcpCluster was not matched to any Track. 

                        //If this mcp has mcpTracks, then it should have
                        //matched the mcpCluster to one of those mcpTracks.
                        if(mcpTracks.size() > 0){
                            missedMatches.put(mcp, new Pair<Track, Cluster>(null, mcpCluster));
                            nMissedMatches = nMissedMatches + 1;
                        }
                        else{
                            //If MCP has 0 SimTrackerHits, then its cluster
                            //should not be matched to a track...this is a
                            //"good match"
                            if(getNSimTrackerHits(event,mcp) == 0){
                                //tracklessMCPClusterNotMatchedToTrack.put(mcp, new Pair<null, mcpCluster>);
                                nTracklessMCPClusterNotMatchedToTrack = nTracklessMCPClusterNotMatchedToTrack + 1;
                                //goodMatches.put(mcp, new Pair<Track, Cluster>(null, mcpCluster));
                                //nGoodMatches = nGoodMatches + 1;
                            }
                            else{
                                //If mcpCluster unmatched, and we dont know if
                                //thats right or wrong.
                                unmatchedUntrackableMCPs.put(mcp, new Pair<Track, Cluster>(null, mcpCluster));
                                nUnmatchedUntrackableMCPs = nUnmatchedUntrackableMCPs + 1;
                                plots1D.get("mcp_loop_no_truth_tracks_truth_cluster_not_matched_to_track_nSimTrackerHits").fill(nmcpHits);
                                //check Nsimtrackerhits
                            }
                        }
                    }
                }

                //If mcpCluster is null, there is no truth cluster for this
                //MCP. 
                //So instead of looking for the mcpCluster to have been matched
                //to a Track...we now check if the mcpTracks were matched to a
                //cluster.
                else{
                    //Check to see if this MCP leaves any simcalhits.
                    //If it does, then we expect the MCP to leave a cluster.
                    //If it does not, we do not expect the MCP to leave a
                    //cluster.

                    boolean clusterExpected = false;
                    boolean algClusterIsRogue = true;
                    //If this MCP left simcalhits, it should get matched to a
                    //cluster.
                    if(getNSimCalHits(event, "EcalHits", mcp) > 0){
                        clusterExpected = true;
                    }
                    //loop over all mcpTracks
                    Cluster matchedCluster = null;
                    Track matchedTrack = null;
                    Track mcpBestTrack = getMcpBestTrack(mcp, mcpTracksMap);
                    matchedCluster = matchedTrackClusterMap.get(mcpBestTrack);
                    matchedTrack = mcpBestTrack;
                    if(matchedCluster == null){
                        for(Track track : mcpTracks){
                            matchedCluster = matchedTrackClusterMap.get(track);
                            if(matchedCluster != null){
                                matchedTrack = track;
                                break;
                            }
                        }
                    }

                    if(clusterExpected){
                        if(matchedCluster == null){
                            missedMatches.put(mcp, new Pair<Track, Cluster>(matchedTrack, null));
                            nMissedMatches = nMissedMatches + 1;
                        }
                        //if this matchedCluster belongs to a different
                        //MCP, this is a bad match
                        else if(clustersMCPMap.containsKey(matchedCluster)){
                            badMatches.put(mcp, new Pair<Track, Cluster>(matchedTrack,matchedCluster));
                            nBadMatches = nBadMatches + 1;
                            plots2D.get("mcp_loop_truth_track_matched_to_wrong_cluster_E_v_E").fill(mcp.getEnergy(),matchedCluster.getEnergy());
                        }
                        else{
                            unknownMatches_mcpToRogueCluster.put(mcp, new Pair<Track, Cluster>(matchedTrack, matchedCluster));
                            nUnknownMatches_mcpToRogueCluster = nUnknownMatches_mcpToRogueCluster + 1; 
                        }
                    }
                    else{
                        if(matchedCluster == null){
                            //clusterlessMCPtrackNotMatchedToCluster.put(mcp, new Pair<Track, Cluster>(matchedTrack, null));
                            nClusterlessMCPtrackNotMatchedToCluster = nClusterlessMCPtrackNotMatchedToCluster + 1;
                            //goodMatches.put(mcp, new Pair<Track, Cluster>(mcpBestTrack, null)); 
                            //nGoodMatches = nGoodMatches + 1;
                        }

                        else if(clustersMCPMap.containsKey(matchedCluster)){
                            badMatches.put(mcp, new Pair<Track, Cluster>(matchedTrack,matchedCluster));
                            nBadMatches = nBadMatches + 1;
                            plots2D.get("mcp_loop_truth_track_matched_to_wrong_cluster_E_v_E").fill(mcp.getEnergy(),matchedCluster.getEnergy());
                        }

                        else{
                            //badMatches.put(mcp, new Pair<Track, Cluster>(mcpBestTrack,matchedCluster));
                            //nBadMatches = nBadMatches + 1;
                            nClusterlessMCPtrackMatchedToRogueCluster = nClusterlessMCPtrackMatchedToRogueCluster + 1;
                            //plots2D.get("mcp_loop_truth_track_matched_to_wrong_cluster_E_v_E").fill(mcp.getEnergy(),matchedCluster.getEnergy());
                        }
                    }
                }
            }

            //Loop over photon MCPs
            if(Math.abs(mcp.getPDGID()) == 22){
                nphoton = nphoton + 1;
                //If photon is matched to any track            
                Track matchedTrack = null;
                if(matchedTrackClusterMap.containsValue(mcpCluster)){
                    for(Map.Entry<Track, Cluster> subentry : matchedTrackClusterMap.entrySet()){
                        if(subentry.getValue() == mcpCluster){
                            matchedTrack = subentry.getKey();
                            break;
                        }
                    }
                    photonsMatchedToTrack.put(mcp, new Pair<Track, Cluster>(matchedTrack, mcpCluster));
                    nPhotonsMatchedToTrack = nPhotonsMatchedToTrack + 1;
                }
                else{
                    truthPhotonClusters.put(mcp, new Pair<Track, Cluster>(null, mcpCluster));
                    nTruthPhotons = nTruthPhotons + 1;
                }
            }
            mcparticlesEvaluated.add(mcp);
            nMCParticlesEvaluated = nMCParticlesEvaluated + 1;
        }

        plots1D.get("mcp_loop_nEle_per_event").fill(nele);
        plots1D.get("mcp_loop_nPos_per_event").fill(npos);
        plots1D.get("mcp_loop_nPhotons_per_event").fill(nphoton);
        plots1D.get("mcp_loop_nCharged_particles_per_event").fill(nele + npos);
        plots2D.get("mcp_loop_nCharged_particles_v_nTracks_per_event").fill(nele + npos, tracks.size());
        plots2D.get("mcp_loop_nMCPs_v_nClusters_per_event").fill(nele + npos + nphoton,clusters.size());


        //Loop over rogue clusters to find rogue matches and rogue photons
        for(Cluster cluster : clusters){
            if(clustersMCPMap.containsKey(cluster))
                continue;
            if(matchedTrackClusterMap.containsValue(cluster)){
                Track matchedTrack = null;
                for(Map.Entry<Track, Cluster> subentry : matchedTrackClusterMap.entrySet()){
                    if(subentry.getValue() == cluster){
                        matchedTrack = subentry.getKey();
                    }
                }
                if(tracksMCPMap.containsKey(matchedTrack)){
                    continue;
                }
                else{
                    rogueMatches.put(matchedTrack, cluster);
                    nRogueMatches = nRogueMatches + 1;
                }
            }
            else{
                iddPhotons.add(cluster);
                nIddPhotons = nIddPhotons + 1;
            }
        }

        //Loop over rogue tracks to find rogue matches and rogue tracks
        for(Track track : tracks){
            if(tracksMCPMap.containsKey(track))
                continue;
            Cluster matchedCluster = matchedTrackClusterMap.get(track);
            if(matchedCluster == null){
                rogueTracks.add(track);
                nRogueTracks = nRogueTracks + 1;
            }
            if(clustersMCPMap.containsKey(matchedCluster))
                continue;
            else
                if(!rogueMatches.containsKey(track)){
                    rogueMatches.put(track, matchedCluster);
                    nRogueMatches = nRogueMatches + 1;
                }
        }

        //Fill count histograms
        plots1D.get("nGoodMatches").fill(nGoodMatches);
        plots1D.get("nGoodMatches").fill(-1, nGoodMatches);

        plots1D.get("nBadMatches").fill(nBadMatches);
        plots1D.get("nBadMatches").fill(-1, nBadMatches);

        plots1D.get("nMissedMatches").fill(nMissedMatches);
        plots1D.get("nMissedMatches").fill(-1, nMissedMatches);

        plots1D.get("nUnknownMatches_mcpToRogueTrack").fill(nUnknownMatches_mcpToRogueTrack);
        plots1D.get("nUnknownMatches_mcpToRogueTrack").fill(-1, nUnknownMatches_mcpToRogueTrack);

        plots1D.get("nUnknownMatches_mcpToRogueCluster").fill(nUnknownMatches_mcpToRogueCluster);
        plots1D.get("nUnknownMatches_mcpToRogueCluster").fill(-1, nUnknownMatches_mcpToRogueCluster);

        plots1D.get("nUnmatchedUntrackableMCPs").fill(nUnmatchedUntrackableMCPs);
        plots1D.get("nUnmatchedUntrackableMCPs").fill(-1, nUnmatchedUntrackableMCPs);

        plots1D.get("nPhotons_matched_to_track").fill(nPhotonsMatchedToTrack);
        plots1D.get("nPhotons_matched_to_track").fill(-1, nPhotonsMatchedToTrack);

        plots1D.get("nTruth_photon_clusters").fill(nTruthPhotons);
        plots1D.get("nTruth_photon_clusters").fill(-1, nTruthPhotons);

        plots1D.get("nRogueMatches").fill(nRogueMatches);
        plots1D.get("nRogueMatches").fill(-1, nRogueMatches);

        plots1D.get("nRogueTracks").fill(nRogueTracks);
        plots1D.get("nRogueTracks").fill(-1, nRogueTracks);

        plots1D.get("nIddPhotons").fill(nIddPhotons);
        plots1D.get("nIddPhotons").fill(-1, nIddPhotons);

        plots1D.get("nTracklessMCPClusterMatchedToRogueTrack").fill(nTracklessMCPClusterMatchedToRogueTrack);
        plots1D.get("nTracklessMCPClusterMatchedToRogueTrack").fill(-1, nTracklessMCPClusterMatchedToRogueTrack);

        plots1D.get("nTracklessMCPClusterNotMatchedToTrack").fill(nTracklessMCPClusterNotMatchedToTrack);
        plots1D.get("nTracklessMCPClusterNotMatchedToTrack").fill(-1, nTracklessMCPClusterNotMatchedToTrack);

        plots1D.get("nClusterlessMCPtrackMatchedToRogueCluster").fill(nClusterlessMCPtrackMatchedToRogueCluster);
        plots1D.get("nClusterlessMCPtrackMatchedToRogueCluster").fill(-1, nClusterlessMCPtrackMatchedToRogueCluster);

        plots1D.get("nClusterlessMCPtrackNotMatchedToCluster").fill(nClusterlessMCPtrackNotMatchedToCluster);
        plots1D.get("nClusterlessMCPtrackNotMatchedToCluster").fill(-1, nClusterlessMCPtrackNotMatchedToCluster);

        plots1D.get("nMCParticlesEvaluated").fill(nMCParticlesEvaluated);
        plots1D.get("nMCParticlesEvaluated").fill(-1, nMCParticlesEvaluated);

        /*
        //Loop over MCParticle Track map. 
        //Check if MCParticle has Track AND Cluster...If not...skip MCP
        for(Map.Entry<MCParticle,Map<Track,Integer>> entry : mcpTracks.entrySet()){
            MCParticle mcp = entry.getKey();
            int nmcpHits = getNSimTrackerHits(event, mcp);
            plots2D.get("mcp_truth_track_ntrackersimhits_v_mcp_momentum").fill(nmcpHits,mcp.getMomentum().magnitude());
            Map<Track, Integer> mcpTrackMap = entry.getValue();

            //Check if there is a cluster that goes with this MCP
            Cluster truthCluster = null;
            if(truthClustersMap.containsValue(mcp)){
                for(Map.Entry<Cluster, MCParticle> clusterMap : truthClustersMap.entrySet()){
                    if(clusterMap.getValue() == mcp){
                        System.out.println("Found Cluster for MCP with Track");
                        truthCluster = clusterMap.getKey();
                        plots1D.get(String.format("%s_mcp_truth_track_cluster_pair_momentum",this.trackCollectionName)).fill(mcp.getMomentum().magnitude());
                        plots1D.get(String.format("%s_mcp_truth_track_cluster_pair_energy",this.trackCollectionName)).fill(mcp.getEnergy());
                        plots1D.get("mcp_truth_track_cluster_pair_ntrackersimhits").fill(nmcpHits);
                        plots2D.get("mcp_truth_track_cluster_pair_ntrackersimhits_v_momentum").fill(nmcpHits,mcp.getMomentum().magnitude());
                        break;
                    }
                }
            }
            else{
                //MCP with Tracks does not have a cluster...move to next MCP
                System.out.println("MCP with Track does not have truth Cluster");
                continue;
            }

            //Loop over algorithm matched Track+Cluster pairs. Check if
            //algorithm returns a match that can be validated using truth
            //information
            for(Map.Entry<Track,Cluster> matches : matchedTrackClusterMap.entrySet()){
                Track track = matches.getKey();
                int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());
                System.out.println("Algorithm Track with momentum  " + track.getMomentum()[0]);

                Cluster cluster = matches.getValue();
                if(cluster == null)
                    System.out.println("Algorithm Track not matched with cluster");
                else
                    System.out.println("Algorithm Track matched to cluster w energy: " + cluster.getEnergy());
                if(cluster != truthCluster){
                    continue;
                }
                if(cluster == truthCluster)
                    System.out.println("Cluster matches truth cluster");
                for(Map.Entry<Track,Integer> e : mcpTrackMap.entrySet()){
                    System.out.println("Possible Track momentum: " + e.getKey().getMomentum()[0]);
                }

                //check if algorithm Track matches truth Track
                if(mcpTrackMap.containsKey(track)){
                    goodMatchTracks.put(track, cluster); 
                    if(charge < 0)
                        nGoodMatches_ele = nGoodMatches_ele + 1;
                    else
                        nGoodMatches_pos = nGoodMatches_pos + 1;
                    
                    break;
                }
                else{
                    badMatchTracks.put(track, cluster);
                    if(charge < 0)
                        nBadMatches_ele = nBadMatches_ele + 1;
                    else
                        nBadMatches_pos = nBadMatches_pos + 1;
                    break;
                }
            }
        }

        nGoodMatches = nGoodMatches + goodMatchTracks.size();
        nBadMatches = nBadMatches + badMatchTracks.size();
        //Fake fraction
        //fakeFrac_ele = nBadMatches_ele/(nBadMatches_ele + nGoodMatches_ele);
        */

        /*
        //Photon matcher evaluation
        for(Map.Entry<Cluster,MCParticle> entry : truthClustersMap.entrySet()){
            Cluster truthCluster = entry.getKey();
            MCParticle mcp = entry.getValue();
            if(mcp.getPDGID() != 22)
                continue;
            plots2D.get("photon_truth_energy_v_cluster_energy").fill(mcp.getEnergy(),truthCluster.getEnergy());
            if(matchedTrackClusterMap.containsValue(truthCluster)){
                nBadPhotons = nBadPhotons + 1;
                plots2D.get("photon_truth_energy_v_cluster_energy_trackMatched").fill(mcp.getEnergy(),truthCluster.getEnergy());
            }
            else
                nGoodPhotons = nGoodPhotons + 1;
        }

        int nRogueClusters = 0;
        int nRogueClusterMatch = 0;

        for(Cluster cluster : clusters){
            if(truthClustersMap.containsKey(cluster))
                continue;
            boolean isNotRogueMatch = true;
            if(matchedTrackClusterMap.containsValue(cluster)){
                for(Map.Entry<Track, Cluster> entry : matchedTrackClusterMap.entrySet()){
                    if(entry.getValue() == cluster){
                        //rogue match
                        for(Map.Entry<MCParticle,Map<Track,Integer>> subentry : mcpTrackMap.entrySet()){
                            if(subentry.getValue().containsKey(entry.getKey())){
                                nTruthlessClusterMatch = nTruthlessCluserMatch + 1;    
                                isNotRogueMatch = false;
                            }
                        }
                    }
                }
                continue;
            }
            else
                nIddPhotons = nIddPhotons + 1;
            nRogueClusters = nRogueClusters + 1;

        }
        */










        /*

        //Check event multiplicity
        int Ntracks;
        int Nclusters;;
        if(tracks != null)
            Ntracks = tracks.size();
        else
            Ntracks = 0;
        if(clusters != null)
            Nclusters = clusters.size();
        else
            Nclusters = 0;

        plots1D.get("track_event_multiplicity").fill(Ntracks);
        plots1D.get("cluster_event_multiplicity").fill(Nclusters);
        plots2D.get("track_v_cluster_event_multiplicity").fill(Ntracks, Nclusters);

        //tracks in ecal
        List<Track> tracksInEcal = new ArrayList<Track>();
        //truth tracks
        List<Track> truthTracks = new ArrayList<Track>();

        //Map for storing Truth Tracks
        Map<Track,MCParticle> truthTracksMap = new HashMap<Track,MCParticle>();
        //List of Tracks that have been truth matched to a cluster
        List<Track> truthTracks_w_truthClusters = new ArrayList<Track>();
        //Map for storing truth tracks with truth cluster pairs
        Map<Track, Cluster> truthTracktruthClusterMap = new HashMap<Track, Cluster>();

        //Get Kalman Track Data
        hitToRotated = TrackUtils.getHitToRotatedTable(event);
        hitToStrips = TrackUtils.getHitToStripsTable(event);
        List<TrackData> TrackData;
        List<LCRelation> trackRelations;
        if (this.trackCollectionName.contains("KalmanFullTracks")) {
            TrackData = event.get(TrackData.class, "KFTrackData");
            trackRelations = event.get(LCRelation.class, "KFTrackDataRelations");
            for (LCRelation relation : trackRelations) {
                if (relation != null && relation.getTo() != null){
                    TrktoData.add(relation.getFrom(), relation.getTo());
                }
            }
        }

        //Looping Over all Tracks
        int eventNumber = event.getEventNumber();
        for(Track track : tracks){

            double trackT;
            if (this.trackCollectionName.contains("GBLTracks")){
                trackT = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
            }
            else {
                TrackData trackdata = (TrackData) TrktoData.from(track);
                trackT = trackdata.getTrackTime();
            }
            //For GBL Tracks, there is a |10|ns track time cut applied in
            //reconstruciton, that is not applied for Kalman Tracks. I am enforcing
            //this cut here, for a fair comparison of GBL v KF
            if(Math.abs(trackT) > 10.0)
                continue;

            //check track quality
            int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());
            double chi2 = track.getChi2();
            int ndf = track.getNDF();

            List<Double> trackpos = getTrackPositionAtEcal(track);
            double trackx = trackpos.get(0);
            double tracky = trackpos.get(1);
            double trackz = trackpos.get(2);

            //Check if track at Ecal is within Ecal acceptance
            //We only want to feed Tracks that are expected to leave a hit in
            //the Ecal to the matching algorithm.
            boolean inEcalAccept = isTrackInEcal(trackx,tracky);
            if(inEcalAccept == true)
                tracksInEcal.add(track);

            double[] trackP = TrackUtils.getTrackStateAtLocation(track,TrackState.AtLastHit).getMomentum();
            double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
            
            double[] trackPfinal = TrackUtils.getTrackStateAtLocation(algTrack,TrackState.AtIP).getMomentum();
            double[] trackPinitial = track.getTrackStates().get(0).getMomentum();
            double deltaPmag = Math.sqrt(Math.pow(trackPfinal[0]-trackPinitial[0],2) + Math.pow(trackPfinal[1]-trackPinitial[1],2) + Math.pow(trackPfinal[2]-trackPinitial[2],2));

            //If want to select only FEEs for analysis
            boolean selectFEEs = false;
            if(selectFEEs){
                if(trackPmag > 2.5 || trackPmag < 2.0)
                    continue;
            }

            if(charge < 0){
                plots1D.get(String.format("%s_ele_track_momentum",this.trackCollectionName)).fill(trackPmag);
                plots1D.get(String.format("%s_ele_delta_momentum",this.trackCollectionName)).fill(deltaPmag);
                plots1D.get(String.format("%s_ele_track_chi2divndf",this.trackCollectionName)).fill(chi2/((double) ndf));
                plots2D.get(String.format("%s_ele_track_xypos_at_ecal",this.trackCollectionName)).fill(trackx,tracky);
            }
            else{
                plots1D.get(String.format("%s_pos_track_momentum",this.trackCollectionName)).fill(trackPmag);
                plots1D.get(String.format("%s_pos_delta_momentum",this.trackCollectionName)).fill(deltaPmag);
                plots1D.get(String.format("%s_pos_track_chi2divndf",this.trackCollectionName)).fill(chi2/((double) ndf));
                plots2D.get(String.format("%s_pos_track_xypos_at_ecal",this.trackCollectionName)).fill(trackx,tracky);
            }

            //TRUTH MATCH TRACKS TO MCPARTICLE
            MCParticle trackMCP =  newgetTrackMCP(event,track,this.trackCollectionName, false);
            if(trackMCP == null)
                continue;
            SimTrackerHit scoringplanehit = getTrackScoringPlaneHit(event, track, trackMCP, ecalScoringPlaneHitsCollectionName);
            if(scoringplanehit != null)
                trackScoringPlanePlots(event, track, scoringplanehit);

            //ONLY TRACKS THAT ARE TRUTH MATCHED TO MCPARTICLE PERSIST
            //BEYOND THIS POINT
            truthTracks.add(track);
            truthTracksMap.put(track, trackMCP);
            double mcpPmag = trackMCP.getMomentum().magnitude(); 
            double trackMCPratio = trackPmag/mcpPmag;

            if(charge < 0){
                plots1D.get(String.format("%s_ele_truth_track_momentum",this.trackCollectionName)).fill(trackPmag);
                plots1D.get(String.format("%s_ele_truth_track_MCP_momentum_ratio",this.trackCollectionName)).fill(trackMCPratio);
                plots2D.get(String.format("%s_ele_truth_track_xypos_at_ecal",this.trackCollectionName)).fill(trackx,tracky);
                plots2D.get(String.format("%s_ele_mcp_v_truth_track_momentum",this.trackCollectionName)).fill(mcpPmag,trackPmag);
            }
            else{
                plots1D.get(String.format("%s_pos_truth_track_momentum",this.trackCollectionName)).fill(trackPmag);
                plots1D.get(String.format("%s_pos_truth_track_MCP_momentum_ratio",this.trackCollectionName)).fill(trackMCPratio);
                plots2D.get(String.format("%s_pos_truth_track_xypos_at_ecal",this.trackCollectionName)).fill(trackx,tracky);
                plots2D.get(String.format("%s_pos_mcp_v_truth_track_momentum",this.trackCollectionName)).fill(trackPmag,mcpPmag);
            }

            //TRUTH MATCH TRACK TO CLUSTER BY MATCHING MCPARTICLES
            Cluster truthTrackCluster = null;
            List<Cluster> matchedClusters = new ArrayList<Cluster>();
            for(Map.Entry<Cluster,MCParticle> entry : truthClustersMap.entrySet()){
                MCParticle clusterMCP = entry.getValue();
                if(clusterMCP == trackMCP) {
                    truthTrackCluster = entry.getKey();
                    break;
                }
            }

            //ONLY TRACKS THAT HAVE BEEN TRUTH MATCHED TO A CLUSTER PERSIST
            //BEYOND THIS POINT
            if(truthTrackCluster == null)
                continue;

            //Defines Track top or bottom
            double tanlambda = track.getTrackParameter(4);
            //Check that Top/Bottom Tracks is only matched to
            //Top/Bottom Clusters
            //if(truthTrackCluster.getPosition()[1] > 0 && tanlambda < 0)
            //    continue;
            //if(truthTrackCluster.getPosition()[1] < 0 && tanlambda > 0)
            //    continue;

            truthTracks_w_truthClusters.add(track);
            truthTracktruthClusterMap.put(track, truthTrackCluster);

            double clusterEnergy = truthTrackCluster.getEnergy();
            double clusterx = truthTrackCluster.getPosition()[0];
            double clustery = truthTrackCluster.getPosition()[1];
            double clusterz = truthTrackCluster.getPosition()[2];
            double truthTrackPmag = trackMCP.getMomentum().magnitude(); 

            //LOOKING AT ALL TRUTH TRACK CLUSTER PAIRS
            if(charge < 0){
                NtruthEleClustPairs = NtruthEleClustPairs + 1;
                plots1D.get(String.format("%s_ele_truth_track_cluster_pair_momentum",this.trackCollectionName)).fill(trackPmag);
                plots1D.get(String.format("%s_ele_truth_track_cluster_pair_px",this.trackCollectionName)).fill(trackP[1]);
                plots1D.get(String.format("%s_ele_truth_track_cluster_pair_py",this.trackCollectionName)).fill(trackP[2]);
                plots1D.get(String.format("%s_ele_truth_track_cluster_pair_pz",this.trackCollectionName)).fill(trackP[0]);
                plots1D.get(String.format("%s_ele_truth_track_cluster_pair_dx",trackCollectionName)).fill(clusterx - trackx);
                plots1D.get(String.format("%s_ele_truth_track_cluster_pair_dy",trackCollectionName)).fill(clustery - tracky);
                plots1D.get(String.format("%s_ele_truth_track_cluster_pair_dz",trackCollectionName)).fill(clusterz - trackz);
                plots1D.get(String.format("%s_ele_truth_track_cluster_pair_EdivP",trackCollectionName)).fill(clusterEnergy/trackPmag);
                plots1D.get(String.format("%s_ele_truth_track_cluster_pair_clusterMCP_Eratio",this.trackCollectionName)).fill(clusterEnergy/trackMCP.getEnergy());

                plots2D.get(String.format("%s_ele_truth_track_cluster_pair_xypos",this.trackCollectionName)).fill(trackx,tracky);
                
            }

            if(charge > 0){
                NtruthPosClustPairs = NtruthPosClustPairs + 1;
                plots1D.get(String.format("%s_pos_truth_track_cluster_pair_momentum",this.trackCollectionName)).fill(trackPmag);
                plots1D.get(String.format("%s_pos_truth_track_cluster_pair_px",this.trackCollectionName)).fill(trackP[1]);
                plots1D.get(String.format("%s_pos_truth_track_cluster_pair_py",this.trackCollectionName)).fill(trackP[2]);
                plots1D.get(String.format("%s_pos_truth_track_cluster_pair_pz",this.trackCollectionName)).fill(trackP[0]);
                plots1D.get(String.format("%s_pos_truth_track_cluster_pair_dx",trackCollectionName)).fill(clusterx - trackx);
                plots1D.get(String.format("%s_pos_truth_track_cluster_pair_dy",trackCollectionName)).fill(clustery - tracky);
                plots1D.get(String.format("%s_pos_truth_track_cluster_pair_EdivP",trackCollectionName)).fill(clusterEnergy/trackPmag);
                plots1D.get(String.format("%s_pos_truth_track_cluster_pair_dz",trackCollectionName)).fill(clusterz - trackz);
                plots1D.get(String.format("%s_pos_truth_track_cluster_pair_clusterMCP_Eratio",this.trackCollectionName)).fill(clusterEnergy/trackMCP.getEnergy());

                plots2D.get(String.format("%s_pos_truth_track_cluster_pair_xypos",this.trackCollectionName)).fill(trackx,tracky);
            }

            //LOOKING AT TRUTH TRACK CLUSTER PAIRS IN ECAL ACCEPTANCE
            if(inEcalAccept == true){
                if(charge < 0){
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_insideEcal_momentum",this.trackCollectionName)).fill(trackPmag);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_insideEcal_dx",trackCollectionName)).fill(clusterx - trackx);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_insideEcal_dy",trackCollectionName)).fill(clustery - tracky);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_insideEcal_dz",trackCollectionName)).fill(clusterz - trackz);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_insideEcal_EdivP",trackCollectionName)).fill(clusterEnergy/trackPmag);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_insideEcal_clusterMCP_Eratio",this.trackCollectionName)).fill(clusterEnergy/trackMCP.getEnergy());

                    for(int i =0; i < eventNumber; i++){
                        plots2D.get(String.format("%s_ele_truth_track_cluster_pair_insideEcal_xypos",this.trackCollectionName)).fill(trackx,tracky);
                        plots2D.get(String.format("%s_ele_truth_track_cluster_pair_insideEcal_cluster_xypos",this.trackCollectionName)).fill(clusterx,clustery);
                    }
                    
                }

                if(charge > 0){
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_insideEcal_momentum",this.trackCollectionName)).fill(trackPmag);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_insideEcal_dx",trackCollectionName)).fill(clusterx - trackx);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_insideEcal_dy",trackCollectionName)).fill(clustery - tracky);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_insideEcal_EdivP",trackCollectionName)).fill(clusterEnergy/trackPmag);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_insideEcal_dz",trackCollectionName)).fill(clusterz - trackz);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_insideEcal_clusterMCP_Eratio",this.trackCollectionName)).fill(clusterEnergy/trackMCP.getEnergy());

                    for(int i =0; i < eventNumber; i++){
                        plots2D.get(String.format("%s_pos_truth_track_cluster_pair_insideEcal_xypos",this.trackCollectionName)).fill(trackx,tracky);
                        plots2D.get(String.format("%s_pos_truth_track_cluster_pair_insideEcal_cluster_xypos",this.trackCollectionName)).fill(clusterx,clustery);
                    }
                
                }
            }



            //LOOKING AT TRACKS THAT ARE MATCHED TO CLUSTERS, BUT
            //WHERE TRACK IS NOT IN ECAL ACCEPTANCE
            if(inEcalAccept == false){
                int trackClusterTag = eventNumber;
                if(charge < 0){
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_momentum",this.trackCollectionName)).fill(trackPmag);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_Pz",this.trackCollectionName)).fill(trackP[2]);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_dx",trackCollectionName)).fill(clusterx - trackx);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_dy",trackCollectionName)).fill(clustery - tracky);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_dz",trackCollectionName)).fill(clusterz - trackz);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_EdivP",trackCollectionName)).fill(clusterEnergy/trackPmag);
                    plots1D.get(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_clusterMCP_Eratio",this.trackCollectionName)).fill(clusterEnergy/trackMCP.getEnergy());

                    for(int i =0; i < trackClusterTag; i++){
                        plots2D.get(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_xypos",this.trackCollectionName)).fill(trackx,tracky);
                        plots2D.get(String.format("%s_ele_truth_track_cluster_pair_outsideEcal_cluster_xypos",this.trackCollectionName)).fill(clusterx,clustery);
                    }
                    
                }

                if(charge > 0){
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_momentum",this.trackCollectionName)).fill(trackPmag);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_Pz",this.trackCollectionName)).fill(trackP[2]);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_dx",trackCollectionName)).fill(clusterx - trackx);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_dy",trackCollectionName)).fill(clustery - tracky);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_EdivP",trackCollectionName)).fill(clusterEnergy/trackPmag);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_dz",trackCollectionName)).fill(clusterz - trackz);
                    plots1D.get(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_clusterMCP_Eratio",this.trackCollectionName)).fill(clusterEnergy/trackMCP.getEnergy());

                    for(int i =0; i < trackClusterTag; i++){
                        plots2D.get(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_xypos",this.trackCollectionName)).fill(trackx,tracky);
                        plots2D.get(String.format("%s_pos_truth_track_cluster_pair_outsideEcal_cluster_xypos",this.trackCollectionName)).fill(clusterx,clustery);
                    }
                
                }
            }
        }


        //DEFINE VARIOUS METRICS TO EVALUATE PERFORMANCE
        for(int i = 0; i < truthTracks_w_truthClusters.size(); i++){
            int charge = -1* (int)Math.signum(truthTracks_w_truthClusters.get(i).getTrackStates().get(0).getOmega());
            if(charge < 0)
                ntruthpairs_ele = ntruthpairs_ele + 1;
            if(charge > 0)
                ntruthpairs_pos = ntruthpairs_pos + 1;
        }
        for(int i = 0; i < truthTracks.size(); i++){
            int charge = -1* (int)Math.signum(truthTracks.get(i).getTrackStates().get(0).getOmega());
            if(charge < 0)
                ntruthTracks_ele = ntruthTracks_ele + 1;
            if(charge > 0)
                ntruthTracks_pos = ntruthTracks_pos + 1;
        }
        for(int i = 0; i < tracks.size(); i++){
            int charge = -1* (int)Math.signum(tracks.get(i).getTrackStates().get(0).getOmega());
            if(charge < 0)
                nrecoTracks_ele = nrecoTracks_ele + 1;
            if(charge > 0)
                nrecoTracks_pos = nrecoTracks_pos + 1;
        }
        ntruthClusters = ntruthClusters + truthClusters.size();
        nClusters = nClusters + clusters.size();
        
        //Use matching algorithm to match tracks to clusters (without truth
        //info)
        Map<Track, Cluster> matchedTrackClusterMap = new HashMap<Track,Cluster>();
        //To check the performance of the Track+Cluster matching algorithm,
        //feed the algorithm the set of truth matched Tracks and truth
        //matched Clusters, so that comparison can be done.
        List<List<Track>> trackCollections = new ArrayList<List<Track>>();
        //trackCollections.add(tracksInEcal);
        trackCollections.add(tracks);
        matchedTrackClusterMap = matcher.matchTracksToClusters(event, trackCollections, clusters, cuts, -1, false, true, ecal, beamEnergy);
        if(matchedTrackClusterMap != null){

            for (Map.Entry<Track,Cluster> entry : matchedTrackClusterMap.entrySet()){

                Track track = entry.getKey();
                Cluster cluster = entry.getValue();

                int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());
                double[] trackP = TrackUtils.getTrackStateAtLocation(track,TrackState.AtLastHit).getMomentum();
                double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));

                //plot xy positions
                List<Double> trackpos = getTrackPositionAtEcal(track);
                boolean isinEcal = isTrackInEcal(trackpos.get(0),trackpos.get(1)); //if track is outside of ecal, returns false

                boolean truthTrack = false;
                double truthTrackPDGID = -9999;
                MCParticle truthTrackMCP = null;
                boolean trackOutsideEcal = false;

                boolean truthCluster = false;
                double truthClusterPDGID = 9999;
                MCParticle truthClusterMCP = null;

                boolean noCluster = false;
                boolean fakeMatch = false;
                boolean unknownMatch = false;
                boolean goodMatch = false;
                boolean missedMatch = false;
                boolean noMatch = false;
                boolean noMatchExpected = false;


                if(cluster == null)
                    noCluster = true;

                if(!isinEcal)
                    trackOutsideEcal = true;

                if(noCluster == false){
                    if(truthTracksMap.containsKey(track)){
                        truthTrack = true;
                        truthTrackPDGID = truthTracksMap.get(track).getPDGID();
                        truthTrackMCP = truthTracksMap.get(track);
                    }
                    if(truthClustersMap.containsKey(cluster)){
                        truthCluster = true;
                        truthClusterPDGID = truthClustersMap.get(cluster).getPDGID();
                        truthClusterMCP = truthClustersMap.get(cluster);
                    }

                    if(truthTrack & truthCluster){
                        if(truthTrackMCP == truthClusterMCP)
                            goodMatch = true;
                        else
                            fakeMatch = true;
                    }

                    if(truthTrack & !truthCluster){
                        //if MCP has track and cluster, but track is paired to
                        //different cluster, bad match
                        if(truthClustersMap.containsValue(truthTrackMCP))
                            fakeMatch = true;
                        else
                            unknownMatch = true;
                    }

                    if(truthCluster & !truthTrack){
                        if(truthTracksMap.containsValue(truthClusterMCP))
                            fakeMatch = true;
                        else if(truthClusterPDGID == 22 || (truthClusterPDGID == -11 & charge < 0) || (truthClusterPDGID == 11 & charge > 0))                    {
                            fakeMatch = true;
                        }
                        else
                            unknownMatch = true;
                    }

                    if(!truthCluster & !truthTrack){
                        unknownMatch = true;
                    }
                }

                else if(noCluster == true){
                    noMatch = true;
                    if(truthTracksMap.containsKey(track)){
                        if(truthClustersMap.containsValue(truthTrackMCP))
                            missedMatch = true;
                    }
                }

                //add numbers
                
                if(noMatch & truthTrack){
                    if(charge < 0)
                        noMatchTruthTrack_ele = noMatchTruthTrack_ele + 1;
                    else
                        noMatchTruthTrack_pos = noMatchTruthTrack_pos + 1;
                }

                if(unknownMatch & truthTrack){
                    if(charge < 0)
                        unknownMatchTruthTrack_ele = unknownMatchTruthTrack_ele + 1;
                    else
                        unknownMatchTruthTrack_pos = unknownMatchTruthTrack_pos + 1;
                }

                if(unknownMatch & truthCluster){
                    if(charge < 0)
                        unknownMatchTruthCluster_ele = unknownMatchTruthCluster_ele + 1;
                    else
                        unknownMatchTruthCluster_pos = unknownMatchTruthCluster_pos + 1;
                }

                if(truthTrack & trackOutsideEcal){
                    if(charge < 0)
                        truthTrackOutsideEcal_ele = truthTrackOutsideEcal_ele + 1;
                    else
                        truthTrackOutsideEcal_pos = truthTrackOutsideEcal_pos + 1;
                }

                if(trackOutsideEcal){
                    if(charge < 0)
                        trackOutsideEcal_ele = trackOutsideEcal_ele + 1;
                    else
                        trackOutsideEcal_pos = trackOutsideEcal_pos + 1;
                }

                if(goodMatch){
                    if(charge < 0)
                        goodMatch_ele = goodMatch_ele + 1;
                    else
                        goodMatch_pos = goodMatch_pos + 1;
                }

                if(fakeMatch){
                    if(charge < 0)
                        fakeMatch_ele = fakeMatch_ele + 1;
                    else
                        fakeMatch_pos = fakeMatch_pos + 1;
                }

                if(noMatch){
                    if(charge < 0)
                        noMatch_ele = noMatch_ele + 1;
                    else
                        noMatch_pos = noMatch_pos + 1;
                }

                if(unknownMatch){
                    if(charge < 0)
                        unknownMatch_ele = unknownMatch_ele + 1;
                    else
                        unknownMatch_pos = unknownMatch_pos + 1;
                }

                if(missedMatch){
                    if(charge < 0)
                        missedMatch_ele = missedMatch_ele + 1;
                    else
                        missedMatch_pos = missedMatch_pos + 1;
                }
            }
        }

        List<MCParticle> possibleTrackMCPs = getPossibleTrackMCPs(event, 9);
        NpossibleTracks = NpossibleTracks + possibleTrackMCPs.size();
        NrecoTruthTracks = NrecoTruthTracks + truthTracksMap.size();
        Map<MCParticle,int[]> nDuplicateMCPs = new HashMap<MCParticle, int[]>();

        for(Map.Entry<Track, MCParticle> entry : truthTracksMap.entrySet()){
            MCParticle particle = entry.getValue(); 
            if(!nDuplicateMCPs.containsKey(particle)){
                nDuplicateMCPs.put(particle, new int[1]);
                nDuplicateMCPs.get(particle)[0] = 0;
            }

            nDuplicateMCPs.get(particle)[0]++;
        }
        for(Map.Entry<MCParticle,int[]> entry : nDuplicateMCPs.entrySet()){
            int nentries = entry.getValue()[0];
            List<Track> dupTracks = new ArrayList<Track>();
            if(nentries > 1){
                for(Map.Entry<Track, MCParticle> entry2 : truthTracksMap.entrySet())
                    if(entry2.getValue() == entry.getKey())
                        dupTracks.add(entry2.getKey());
                for(Track track : dupTracks){
                    int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());
                    double[] trackP = track.getMomentum();
                    double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
                    if(charge < 0)
                        plots1D.get(String.format("%s_ele_duplicate_track_momentum",this.trackCollectionName)).fill(trackPmag);
                    else
                        plots1D.get(String.format("%s_pos_duplicate_track_momentum",this.trackCollectionName)).fill(trackPmag);
                }
                nDuplicates = nDuplicates + 1.0;
            }
        }
        */

    }

    private void trackClusterResidualParameterization(Map<MCParticle,Pair<Track, Cluster>> truthPairsMap){
        
        for(Map.Entry<MCParticle, Pair<Track, Cluster>> entry : truthPairsMap.entrySet()){

            Track track = entry.getValue().getFirstElement();
            Cluster cluster = entry.getValue().getSecondElement();
            int charge = -1* (int) Math.signum(track.getTrackStates().get(0).getOmega());
            double [] params = mosthitsTrack.getTrackParameters();
            double tanlambda = params[4];
            boolean isTop;
            if(tanlambda > 0 )
                isTop = true;
            else
                isTop = false;



        }




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


    public SimTrackerHit getTrackScoringPlaneHit(EventHeader event, MCParticle mcp, String ecalScoringPlaneHitsCollectionName) {

        
        //If even doesnt have collection of Ecal scoring plane hits, skip
        if(!event.hasCollection(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName)) return null;

        List<SimTrackerHit> scoringPlaneHits = event.get(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName);


        //Check for simtrackerhit MCP that matches trackMCP
        if(mcp == null)
            return null;
        SimTrackerHit matchedScoringPlaneHit = null;
        for(SimTrackerHit scoringPlaneHit : scoringPlaneHits){
            // If the MC particles don't match, move on to the next particle
            if(!(scoringPlaneHit.getMCParticle() == mcp)) continue;
            matchedScoringPlaneHit = scoringPlaneHit;
            // Once a match is found, there is no need to loop through the rest of the list
            break;
        }
        return matchedScoringPlaneHit;
    }
    

    public double[] getExtrapolatedTrackScoringPlaneHit(EventHeader event, Track track, SimTrackerHit scoringplaneHit){
    
        double truthxpos;
        double truthypos;
        double truthzpos;

        double trackT;
        double simTrackT;

        simTrackT = scoringplaneHit.getTime();

        truthxpos = scoringplaneHit.getPoint()[0];
        truthypos = scoringplaneHit.getPoint()[1];
        truthzpos = scoringplaneHit.getPoint()[2];

        //multiply charge by factor of -1 (WHY!?)
        int charge = -1* (int) Math.signum(track.getTrackStates().get(0).getOmega());

        double[] truthP = scoringplaneHit.getMomentum();
        double truthPmag = Math.sqrt(Math.pow(truthP[0],2) + Math.pow(truthP[1],2) + Math.pow(truthP[2],2));

        //In PhysicsRun2016 detector geometry, scoring plane is located
        //upstream of Ecal face by ~30 mm...
        //Need to extrapolate MCP scoring plane hits to the Ecal, so that we
        //can compare track positions at Ecal with MCP hit at Ecal
        //rotate HPS coordinates to Tracking coordinates (XYZ)HPS -> (ZXY)
        Hep3Vector truthPVecTracking = new BasicHep3Vector(truthP[2],truthP[0],truthP[1]);
        Hep3Vector truthPosVecTracking = new BasicHep3Vector(truthzpos,truthxpos,truthypos);

        Pair<Hep3Vector,Hep3Vector> extrapVecsTracking = null;
        Detector detector = event.getDetector();
        fM = detector.getFieldMap();
        RK4integrator RKint = new RK4integrator(charge,1, fM);
        extrapVecsTracking = RKint.integrate(truthPosVecTracking,truthPVecTracking,1393-scoringplaneHit.getPoint()[2]);
        Hep3Vector RKextrapPos = extrapVecsTracking.getFirstElement();
        truthxpos = RKextrapPos.y();
        truthypos = RKextrapPos.z();
        truthzpos = RKextrapPos.x();
        double[] truthpos = {truthxpos, truthypos, truthzpos};

        return truthpos;

    }


    public void trackScoringPlanePlots(EventHeader event, Track track, SimTrackerHit scoringplaneHit) {

        //Comparison plots for Track extrapolated to ECal vs Truth ScoringPlaneHit at Ecal
    
        double trackx;
        double tracky;
        double trackz;
        double truthxpos;
        double truthypos;
        double truthzpos;
        double dxoffset;

        double trackT;
        double simTrackT;

        if (this.trackCollectionName.contains("GBLTracks")){
            trackT = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
        }
        else {
            TrackData trackdata = (TrackData) TrktoData.from(track);
            trackT = trackdata.getTrackTime();
        }
        simTrackT = scoringplaneHit.getTime();

        //Make histograms of truth vs extrapolation
        if(this.trackCollectionName.contains("GBLTracks")) {
            trackx = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[1];
            tracky = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[2];
            trackz = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[0];
            //dxoffset = -4.1;
            dxoffset = 0.0;
        }

        else {
            TrackState ts_ecal = track.getTrackStates().get(track.getTrackStates().size()-1);
            double[] ts_ecalPos = ts_ecal.getReferencePoint();
            trackx = ts_ecalPos[1];
            tracky = ts_ecalPos[2];
            trackz = ts_ecalPos[0];
            dxoffset = 0.0;
        }

        truthxpos = scoringplaneHit.getPoint()[0];
        truthypos = scoringplaneHit.getPoint()[1];
        truthzpos = scoringplaneHit.getPoint()[2];

        //multiply charge by factor of -1 (WHY!?)
        int charge = -1* (int) Math.signum(track.getTrackStates().get(0).getOmega());

        //double[] trackP = track.getMomentum();
        //momentum is rotated coords (x->z, z->y, y->x)
        double[] trackP = TrackUtils.getTrackStateAtLocation(track,TrackState.AtIP).getMomentum();
        double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
        double[] truthP = scoringplaneHit.getMomentum();
        double truthPmag = Math.sqrt(Math.pow(truthP[0],2) + Math.pow(truthP[1],2) + Math.pow(truthP[2],2));


        //In PhysicsRun2016 detector geometry, scoring plane is located
        //upstream of Ecal face by ~30 mm...
        //Need to extrapolate MCP scoring plane hits to the Ecal, so that we
        //can compare track positions at Ecal with MCP hit at Ecal
        //rotate HPS coordinates to Tracking coordinates (XYZ)HPS -> (ZXY)
        Hep3Vector truthPVecTracking = new BasicHep3Vector(truthP[2],truthP[0],truthP[1]);
        Hep3Vector truthPosVecTracking = new BasicHep3Vector(truthzpos,truthxpos,truthypos);

        Pair<Hep3Vector,Hep3Vector> extrapVecsTracking = null;
        Detector detector = event.getDetector();
        fM = detector.getFieldMap();
        RK4integrator RKint = new RK4integrator(charge,1, fM);
        extrapVecsTracking = RKint.integrate(truthPosVecTracking,truthPVecTracking,1393-scoringplaneHit.getPoint()[2]);
        Hep3Vector RKextrapPos = extrapVecsTracking.getFirstElement();
        truthxpos = RKextrapPos.y();
        truthypos = RKextrapPos.z();
        truthzpos = RKextrapPos.x();

        //Take position residuals for track and extrap truth hits at ecal
        double dx = truthxpos - trackx;
        double dy = truthypos - tracky;
        double dz = truthzpos - trackz;
        double dr = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2) + Math.pow(dz,2));
        double dt = simTrackT - trackT;

        //Make plots
        if(charge < 0) {

            //track momentum. Truth and reco

            //Track X,Y position plots at Ecal
            //Extrapolated Track momentum vs truth position residuals
            plots2D.get(String.format("%s_ele_RK4_scoringplanehit_to_ecal_ZvP",this.trackCollectionName)).fill(truthzpos, trackPmag);
            //Residuals between track extrapolated to Ecal Face, and truth hit
            //extrapolated to Ecal Face
            plots1D.get(String.format("%s_ele_track_scoringplane_hit_dx",this.trackCollectionName)).fill(dx);
            plots1D.get(String.format("%s_ele_track_scoringplane_hit_dy",this.trackCollectionName)).fill(dy);
            plots1D.get(String.format("%s_ele_track_scoringplane_hit_dz",this.trackCollectionName)).fill(dz);
            plots1D.get(String.format("%s_ele_track_scoringplane_hit_dr",this.trackCollectionName)).fill(dr);
            plots1D.get(String.format("%s_ele_track_scoringplane_hit_dt",this.trackCollectionName)).fill(dt);

            plots1D.get(String.format("%s_ele_track_scoringplane_hit_px",this.trackCollectionName)).fill(truthP[0]);
            plots1D.get(String.format("%s_ele_track_scoringplane_hit_py",this.trackCollectionName)).fill(truthP[1]);
            plots1D.get(String.format("%s_ele_track_scoringplane_hit_pz",this.trackCollectionName)).fill(truthP[2]);

            plots2D.get(String.format("%s_ele_scoringplaneHit_v_truth_track_p",this.trackCollectionName)).fill(truthPmag,trackPmag);
        }
        else {

            //Track X,Y position at Ecal
            //Extrapolated Track P vs truth position residuals
            plots2D.get(String.format("%s_pos_RK4_scoringplanehit_to_ecal_ZvP",this.trackCollectionName)).fill(truthzpos, trackPmag);
            //Track vs Cluster residuals at Ecal
            plots1D.get(String.format("%s_pos_track_scoringplane_hit_dx",this.trackCollectionName)).fill(dx);
            plots1D.get(String.format("%s_pos_track_scoringplane_hit_dy",this.trackCollectionName)).fill(dy);
            plots1D.get(String.format("%s_pos_track_scoringplane_hit_dz",this.trackCollectionName)).fill(dz);
            plots1D.get(String.format("%s_pos_track_scoringplane_hit_dr",this.trackCollectionName)).fill(dr);
            plots1D.get(String.format("%s_pos_track_scoringplane_hit_dt",this.trackCollectionName)).fill(dt);

            plots1D.get(String.format("%s_pos_track_scoringplane_hit_px",this.trackCollectionName)).fill(truthP[0]);
            plots1D.get(String.format("%s_pos_track_scoringplane_hit_py",this.trackCollectionName)).fill(truthP[1]);
            plots1D.get(String.format("%s_pos_track_scoringplane_hit_pz",this.trackCollectionName)).fill(truthP[2]);

            plots2D.get(String.format("%s_pos_scoringplaneHit_v_truth_track_p",this.trackCollectionName)).fill(truthPmag,trackPmag);
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
            TrackData trackdata = (TrackData) TrktoData.from(track);
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


    public List<Double> extrapTrackToCrystalCenter(Track track){

        List<Double> trackpos = getTrackPositionAtEcal(track);
        double trackx = trackpos.get(0);
        double tracky = trackpos.get(1);
        double trackz = trackpos.get(2);
        double[] trackP = TrackUtils.getTrackStateAtLocation(track,TrackState.AtIP).getMomentum();
        double px = trackP[0];
        double py = trackP[1];
        double pz = trackP[2];
        double crystalDepth = 80.0/1000;

        double deltax;
        double deltay;
        for(int i = 0; i < crystalDepth; i++){
            double iter = (double) i;
            deltax = (px/pz)*iter;
            deltay = (py/pz)*iter;
            trackx = trackx + deltax*1000;
            tracky = tracky + deltay*1000;
            trackz = trackz + iter;
        }

        List<Double> newTrackPos = new ArrayList<Double>();
        newTrackPos.add(trackx);
        newTrackPos.add(tracky);
        newTrackPos.add(trackz);

        return newTrackPos;
    }

/**
     * Get the MC particle associated with a track.
     * 
     * @param track : Track to get the MC particle for
     * @return The MC particle associated with the track
     */

    public void printMCPtree(EventHeader event){
        List<MCParticle> mcps = event.get(MCParticle.class,"MCParticle");

        for(int i = 0; i < mcps.size(); i++){
            MCParticle mcp = mcps.get(i);
            System.out.println("MCParticle " + i + " PDGID: " + mcp.getPDGID());
            System.out.println("MCParticle " + i + " Energy: " + mcp.getEnergy());
            System.out.println("MCParticle " + i + " Momentum: " + mcp.getPX() + "," + mcp.getPY() + "," + mcp.getPZ());
            System.out.println("MCParticle " + i + " Origin: " + mcp.getOriginX() + "," + mcp.getOriginY() + "," + mcp.getOriginZ());
            //if(mcp.getEndPoint())
            try{
                System.out.println("MCParticle " + i + " Endpoint: " + mcp.getEndPoint().x() + "," + mcp.getEndPoint().y() + "," + mcp.getEndPoint().z());
            }
            catch (Throwable t){
                System.out.println("No endpoint available");
            }
            if(mcp.getParents().size() != 0){   
                System.out.println("MCParticle " + i + " Parent PDGID: " + mcp.getParents().get(0).getPDGID());
                System.out.println("MCParticle " + i + " Parent Energy: " + mcp.getParents().get(0).getEnergy());
                System.out.println("MCParticle " + i + " Parent Momentum: " + mcp.getParents().get(0).getPX() + "," + mcp.getParents().get(0).getPY() + "," + mcp.getParents().get(0).getPZ());
            }
            else
                System.out.println("No parents of this MCP");
        }

    }

    private List<MCParticle> getMCParticlesFromLCIO(EventHeader event){
        List<MCParticle> mcparticles = event.get(MCParticle.class, "MCParticle");
        return mcparticles;
    }

    public int getNSimCalHits(EventHeader event, String simcalhitsCollectionName, MCParticle mcp){

        List<SimCalorimeterHit> simcalhits = event.get(SimCalorimeterHit.class, simcalhitsCollectionName);
        int nHits = 0;
        for(SimCalorimeterHit simcalhit : simcalhits){
            for(int i = 0; i < simcalhit.getMCParticleCount(); i++){
                MCParticle particle = simcalhit.getMCParticle(i);           
                if(particle == mcp)
                    nHits = nHits + 1;
            }
        }
        return nHits;
    }

    private void getClusterMcpMap(List<Cluster> clusters, EventHeader event, boolean verbose, Map<MCParticle, Cluster> mcpClustersMapIn, Map<Cluster, MCParticle> clustersMCPMapIn){

        HashMap<MCParticle, HashSet<Cluster>> mcpClusterMap = new HashMap<MCParticle, HashSet<Cluster>>();
        HashMap<Cluster,MCParticle> mcpClusterMapFinal = new HashMap<Cluster, MCParticle>();

        for(Cluster cluster : clusters){

            double clusterx = cluster.getPosition()[0];
            double clustery = cluster.getPosition()[1];
            double clusterz = cluster.getPosition()[2];
            double clusterEnergy = cluster.getEnergy();

            plots2D.get(String.format("ecal_cluster_positions_xy_plane")).fill(clusterx,clustery);
            plots1D.get(String.format("cluster_truth_stage_0_energy",trackCollectionName)).fill(clusterEnergy);

            //Begin truth matching
            List<MCParticle> mcparticles = getMCParticlesFromLCIO(event);

            List <RawTrackerHit> ecalReadoutHits = event.get(RawTrackerHit.class,ecalReadoutHitsCollectionName);
            Map <MCParticle, int[]>mcParticleMultiplicity = new HashMap<MCParticle, int[]>();
            RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            if (event.hasCollection(LCRelation.class, ecalTruthRelationsName )) {
                List<LCRelation> trueHitRelations = event.get(LCRelation.class, ecalTruthRelationsName);
                for (LCRelation relation : trueHitRelations)
                    if (relation != null && relation.getFrom() != null && relation.getTo() != null){
                        rawtomc.add(relation.getFrom(), relation.getTo());
                    }
                    else System.out.println("failed to build cluster mc relation");
            }

            //Match Cluster->CalorimeterHit(s) to corresponding EcalReadoutHits via
            //CellID match
            List<CalorimeterHit> calorimeterhits = cluster.getCalorimeterHits();
            CalorimeterHit seedhit = ClusterUtilities.findSeedHit(cluster);

            RawTrackerHit readoutMatchHit = null;
            long cellID = seedhit.getCellID();
            for(RawTrackerHit ecalReadoutHit : ecalReadoutHits) {
                long recellID = ecalReadoutHit.getCellID();
                if(cellID == recellID) {
                    readoutMatchHit = ecalReadoutHit;
                }
            }
            if(verbose){
                System.out.println("Checking Cluster with Energy: " + cluster.getEnergy());
                System.out.println("Cluster X: " + cluster.getPosition()[0]);
                System.out.println("Cluster Y: " + cluster.getPosition()[1]);
                System.out.println("Cluster seedhit cellID: " + cellID);
                System.out.println("Cluster seedhit Energy: " + seedhit.getRawEnergy());
            }

            if(readoutMatchHit == null)
                continue;
            if(verbose){
                System.out.println("Matching Readout Hit Found: " + readoutMatchHit.getCellID());
                System.out.println("Readout Hit position: x= " + readoutMatchHit.getPosition()[0] + "; y= " + readoutMatchHit.getPosition()[1] + "; z= " + readoutMatchHit.getPosition()[2]);
            }

            Set<SimCalorimeterHit> simcalhits = rawtomc.allFrom(readoutMatchHit);
            double simcalhit_largestEnergy = 0.0;
            SimCalorimeterHit largest_simcalhit = null;
            double maxMCPEnergy = 0.0;
            for(SimCalorimeterHit simcalhit : simcalhits) {
                if(verbose){
                    System.out.println("Simcalhit energy: " + simcalhit.getRawEnergy());
                }
                if(simcalhit.getRawEnergy() > simcalhit_largestEnergy){
                    simcalhit_largestEnergy = simcalhit.getRawEnergy();
                    largest_simcalhit = simcalhit;
                }
            }
            if(verbose && largest_simcalhit != null){
                System.out.println("Simcalhit with energy: " + largest_simcalhit.getRawEnergy() + " selected");
                System.out.println("Simcalhit cellID: " + largest_simcalhit.getCellID());
                System.out.println("Simcalhit position: x= " + largest_simcalhit.getPosition()[0] + "; y= " + largest_simcalhit.getPosition()[1] + "; z= " + largest_simcalhit.getPosition()[2]);
            }

            double bestMCPEnergyContr = 0.0;
            MCParticle bestMCP = null;

            if(largest_simcalhit == null)
                continue;

            for(int i=0; i < largest_simcalhit.getMCParticleCount(); i++){
                MCParticle mcp = largest_simcalhit.getMCParticle(i);
                double originZ = largest_simcalhit.getMCParticle(i).getOriginZ();
                int PDGID = largest_simcalhit.getMCParticle(i).getPDGID();
                double MCPEnergyFrac = largest_simcalhit.getContributedEnergy(i)/cluster.getEnergy();
                if(verbose){
                    System.out.println("Looping over MCParticles for Simcalhit");
                    System.out.println("MCP energy: " + mcp.getEnergy());
                    System.out.println("MCP energy contribution to simcalhit: " + largest_simcalhit.getContributedEnergy(i));
                    System.out.println("mcp PDGID from mcp = " + mcp.getPDGID());
                    // doesnt work System.out.println("mcp PDGID from simcalhit.getPDG(i) = " + largest_simcalhit.getPDG(i));
                    System.out.println("mcp OriginZ: " + mcp.getOriginZ());
                    System.out.println("mcp EndpointX: " + mcp.getEndPoint().x());
                    System.out.println("mcp EndpointY: " + mcp.getEndPoint().y());
                    System.out.println("mcp EndpointZ: " + mcp.getEndPoint().z());
                    if(mcparticles.contains(mcp)){
                        System.out.println("mcp from simcalhit found in LCIO MCParticle collection");
                    }
                    else
                        System.out.println("mcp from simcalhit NOT FOUND in LCIO MCPartice collection");
                }

                if(mcp.getEnergy() > bestMCPEnergyContr){
                    bestMCPEnergyContr = mcp.getEnergy();
                    bestMCP = mcp;
                }
            }

            if(bestMCP == null)
                continue;

            double distance = Math.sqrt( Math.pow(clusterx - bestMCP.getEndPoint().x(),2) + Math.pow(clustery - bestMCP.getEndPoint().y(),2));
            double energyRatio = cluster.getEnergy()/bestMCP.getEnergy();

            //Cluster has been truth matched to some MCParticle
            plots1D.get(String.format("cluster_truth_stage_1_energy",trackCollectionName)).fill(clusterEnergy);
            plots2D.get("cluster_truth_stage_1_mcpEndpointz_v_ds").fill(bestMCP.getEndPoint().z(),distance);
            plots1D.get(String.format("cluster_truth_stage_1_energy_ratio",this.trackCollectionName)).fill(energyRatio);
            plots2D.get("cluster_truth_stage_1_cluster_v_mcp_energy").fill(cluster.getEnergy(),bestMCP.getEnergy());

            //restrict truth matching to only MCPs that originate at target
            if(bestMCP.getOriginZ() > 0)
                continue;

            plots1D.get(String.format("cluster_truth_stage_2_energy",trackCollectionName)).fill(clusterEnergy);
            plots2D.get("cluster_truth_stage_2_mcpEndpointz_v_ds").fill(bestMCP.getEndPoint().z(),distance);
            plots1D.get(String.format("cluster_truth_stage_2_energy_ratio",this.trackCollectionName)).fill(energyRatio);
            plots2D.get("cluster_truth_stage_2_cluster_v_mcp_energy").fill(cluster.getEnergy(),bestMCP.getEnergy());

            plots2D.get("cluster_truth_stage_2_mcpPy_v_clustery").fill(bestMCP.getPY(),clustery);

            //Require Py sign of MCP to correlate to Top/Bottom cluster
            if(bestMCP.getPY() < 0 & clustery > 0)
                continue;
            if(bestMCP.getPY() > 0 & clustery < 0)
                continue;

            plots1D.get(String.format("cluster_truth_stage_3_energy",trackCollectionName)).fill(clusterEnergy);
            plots2D.get("cluster_truth_stage_3_mcpEndpointz_v_ds").fill(bestMCP.getEndPoint().z(),distance);
            plots1D.get(String.format("cluster_truth_stage_3_energy_ratio",this.trackCollectionName)).fill(energyRatio);
            plots2D.get("cluster_truth_stage_3_cluster_v_mcp_energy").fill(cluster.getEnergy(),bestMCP.getEnergy());

            HashSet<Cluster> c = new HashSet<Cluster>();
            if(mcpClusterMap.containsKey(bestMCP)){
                c = mcpClusterMap.get(bestMCP);
                c.add(cluster);
                mcpClusterMap.put(bestMCP, c);
            }
            else{
                c.add(cluster);
                mcpClusterMap.put(bestMCP, c);
            }
        }

        //Loop over mcpCluster map to check for duplicates
        for(Map.Entry<MCParticle, HashSet<Cluster>> entry : mcpClusterMap.entrySet()){
            MCParticle mcp = entry.getKey();
            HashSet<Cluster> tClusters = entry.getValue();
            plots1D.get("clusters_matched_to_n_mcps").fill(tClusters.size());

            if(tClusters.size() < 2){
                for(Cluster tCluster : tClusters){
                    mcpClusterMapFinal.put(tCluster, mcp);
                    mcpClustersMapIn.put(mcp, tCluster);
                    clustersMCPMapIn.put(tCluster, mcp);
                }
            }

            if(tClusters.size() > 1){
                System.out.println("duplicate clusters matched to single MCP");
                List<Cluster> dupClusters = new ArrayList<Cluster>(tClusters);
                for(int i =0; i < dupClusters.size()-1; i++){
                    plots1D.get("cluster_truth_stage_3_duplicate_mcp_match_dx").fill(dupClusters.get(i).getPosition()[0] - dupClusters.get(i+1).getPosition()[0]);
                    plots1D.get("cluster_truth_stage_3_duplicate_mcp_match_dy").fill(dupClusters.get(i).getPosition()[1] - dupClusters.get(i+1).getPosition()[1]);
                }

                List<Cluster> recoveredDup = new ArrayList<Cluster>();
                for(Cluster tCluster : tClusters){
                    //Cut all matches where MCP.getEndpointZ() is < Ecal face
                    double ecalPosZ = 1300.0;
                    if(mcp.getEndPoint().z() < ecalPosZ)
                        continue;
                    if(tCluster.getPosition()[1] > 0 & mcp.getEndPoint().y() < 0)
                        continue;
                    if(tCluster.getPosition()[1] < 0 & mcp.getEndPoint().y() > 0)
                        continue;
                    if(tCluster.getPosition()[0] > 0 & mcp.getEndPoint().x() < 0)
                        continue;
                    if(tCluster.getPosition()[0] < 0 & mcp.getEndPoint().x() > 0)
                        continue;
                    recoveredDup.add(tCluster);
                    break;
                }

                if(recoveredDup.size() > 1){
                    for(int i =0; i < recoveredDup.size()-1; i++){
                        plots1D.get("cluster_truth_stage_3_cut_remaining_duplicate_mcp_match_dx").fill(recoveredDup.get(i).getPosition()[0] - recoveredDup.get(i+1).getPosition()[0]);
                        plots1D.get("cluster_truth_stage_3_cut_remaining_duplicate_mcp_match_dy").fill(recoveredDup.get(i).getPosition()[1] - recoveredDup.get(i+1).getPosition()[1]);

                    }
                }
                else if(recoveredDup.size() > 0 & recoveredDup.size() < 2) {
                    mcpClusterMapFinal.put(recoveredDup.get(0),mcp);
                    mcpClustersMapIn.put(mcp, recoveredDup.get(0));
                    clustersMCPMapIn.put(recoveredDup.get(0),mcp);
                }
            }
        }

        for(Map.Entry<Cluster, MCParticle> entry: mcpClusterMapFinal.entrySet()){
            Cluster cluster = entry.getKey();
            MCParticle mcp = entry.getValue();
            double distance = Math.sqrt( Math.pow(cluster.getPosition()[0] - mcp.getEndPoint().x(),2) + Math.pow(cluster.getPosition()[1] - mcp.getEndPoint().y(),2));
            double energyRatio = cluster.getEnergy()/mcp.getEnergy();
            plots1D.get(String.format("cluster_truth_stage_final_energy",trackCollectionName)).fill(cluster.getEnergy());
            plots2D.get("cluster_truth_stage_final_mcpEndpointz_v_ds").fill(mcp.getEndPoint().z(),distance);
            plots1D.get(String.format("cluster_truth_stage_final_energy_ratio",this.trackCollectionName)).fill(energyRatio);
            plots2D.get("cluster_truth_stage_final_cluster_v_mcp_energy").fill(cluster.getEnergy(),mcp.getEnergy());
        }
        //return mcpClusterMapFinal;
    }

    private MCParticle getClusterMCP(Cluster cluster, EventHeader event, boolean verbose) {

        List<MCParticle> mcparticles = getMCParticlesFromLCIO(event);

        List <RawTrackerHit> ecalReadoutHits = event.get(RawTrackerHit.class,ecalReadoutHitsCollectionName);
        Map <MCParticle, int[]>mcParticleMultiplicity = new HashMap<MCParticle, int[]>();
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, ecalTruthRelationsName )) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, ecalTruthRelationsName);
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null){
                    rawtomc.add(relation.getFrom(), relation.getTo());
                }
                else System.out.println("failed to build cluster mc relation");
        }

        //Match Cluster->CalorimeterHit(s) to corresponding EcalReadoutHits via
        //CellID match
        List<CalorimeterHit> calorimeterhits = cluster.getCalorimeterHits();
        CalorimeterHit seedhit = ClusterUtilities.findSeedHit(cluster);

        RawTrackerHit readoutMatchHit = null;
        long cellID = seedhit.getCellID();
        for(RawTrackerHit ecalReadoutHit : ecalReadoutHits) {
            long recellID = ecalReadoutHit.getCellID();
            if(cellID == recellID) {
                readoutMatchHit = ecalReadoutHit;
            }
        }
        if(verbose){
            System.out.println("Checking Cluster with Energy: " + cluster.getEnergy());
            System.out.println("Cluster X: " + cluster.getPosition()[0]);
            System.out.println("Cluster Y: " + cluster.getPosition()[1]);
            System.out.println("Cluster seedhit cellID: " + cellID);
            System.out.println("Cluster seedhit Energy: " + seedhit.getRawEnergy());
        }
        if(readoutMatchHit == null)
            return null;
        if(verbose){
            System.out.println("Matching Readout Hit Found: " + readoutMatchHit.getCellID());
            System.out.println("Readout Hit position: x= " + readoutMatchHit.getPosition()[0] + "; y= " + readoutMatchHit.getPosition()[1] + "; z= " + readoutMatchHit.getPosition()[2]);
        }

        Set<SimCalorimeterHit> simcalhits = rawtomc.allFrom(readoutMatchHit);
        double simcalhit_largestEnergy = 0.0;
        SimCalorimeterHit largest_simcalhit = null;
        double maxMCPEnergy = 0.0;
        for(SimCalorimeterHit simcalhit : simcalhits) {
            if(verbose){
                System.out.println("Simcalhit energy: " + simcalhit.getRawEnergy());
            }
            if(simcalhit.getRawEnergy() > simcalhit_largestEnergy){
                simcalhit_largestEnergy = simcalhit.getRawEnergy();
                largest_simcalhit = simcalhit;
            }
        }
        if(verbose && largest_simcalhit != null){
            System.out.println("Simcalhit with energy: " + largest_simcalhit.getRawEnergy() + " selected");
            System.out.println("Simcalhit cellID: " + largest_simcalhit.getCellID());
            System.out.println("Simcalhit position: x= " + largest_simcalhit.getPosition()[0] + "; y= " + largest_simcalhit.getPosition()[1] + "; z= " + largest_simcalhit.getPosition()[2]);
        }
        double bestMCPEnergyContr = 0.0;
        MCParticle bestMCP = null;
        double bestMCPContrE = 0.0;
        if(largest_simcalhit == null)
            return null;
        for(int i=0; i < largest_simcalhit.getMCParticleCount(); i++){
            MCParticle mcp = largest_simcalhit.getMCParticle(i);
            double originZ = largest_simcalhit.getMCParticle(i).getOriginZ();
            int PDGID = largest_simcalhit.getMCParticle(i).getPDGID();
            double MCPEnergyFrac = largest_simcalhit.getContributedEnergy(i)/cluster.getEnergy();
            if(verbose){
            
                System.out.println("Looping over MCParticles for Simcalhit");
                System.out.println("MCP energy: " + mcp.getEnergy());
                System.out.println("MCP energy contribution to simcalhit: " + largest_simcalhit.getContributedEnergy(i));
                System.out.println("mcp PDGID from mcp = " + mcp.getPDGID());
                // doesnt work System.out.println("mcp PDGID from simcalhit.getPDG(i) = " + largest_simcalhit.getPDG(i));
                System.out.println("mcp OriginZ: " + mcp.getOriginZ());
                System.out.println("mcp EndpointX: " + mcp.getEndPoint().x());
                System.out.println("mcp EndpointY: " + mcp.getEndPoint().y());
                System.out.println("mcp EndpointZ: " + mcp.getEndPoint().z());
                if(mcparticles.contains(mcp)){
                    System.out.println("mcp from simcalhit found in LCIO MCParticle collection");
                }
                else
                    System.out.println("mcp from simcalhit NOT FOUND in LCIO MCPartice collection");
                double distance = Math.sqrt( Math.pow(largest_simcalhit.getPosition()[0] - mcp.getEndPoint().x(),2) + Math.pow(largest_simcalhit.getPosition()[1] - mcp.getEndPoint().y(),2));
            }

            if(largest_simcalhit.getContributedEnergy(i) > bestMCPEnergyContr){
                bestMCPEnergyContr = largest_simcalhit.getContributedEnergy(i);
                bestMCP = largest_simcalhit.getMCParticle(i);
                bestMCPContrE = largest_simcalhit.getContributedEnergy(i);
            }
        }

        if(bestMCP == null)
            return null;

        double ecalFacePosZ = 1330.0; //mm
        if(bestMCP.getOriginZ() > ecalFacePosZ)
            return null;

        double distance = Math.sqrt( Math.pow(largest_simcalhit.getPosition()[0] - bestMCP.getEndPoint().x(),2) + Math.pow(largest_simcalhit.getPosition()[1] - bestMCP.getEndPoint().y(),2));

        double energyRatio = cluster.getEnergy()/bestMCP.getEnergy();
        plots1D.get(String.format("cluster_truthMCP_energy_ratio",this.trackCollectionName)).fill(energyRatio);

        return bestMCP;
    }

    private SimTrackerHit getBestSimHit(RawTrackerHit rawhit, RelationalTable rawtomc){

        if(rawhit == null)
            return null;
        Set<SimTrackerHit> simhits = rawtomc.allFrom(rawhit);
        SimTrackerHit rawhitSimhit = null;
        double simhitMaxE = 0.0;
        for(SimTrackerHit simhit : simhits){
            if (simhit != null && simhit.getMCParticle() != null) {
                double simhitEnergy = simhit.getdEdx();
                if(simhitEnergy > simhitMaxE){
                    simhitMaxE = simhitEnergy;
                    rawhitSimhit = simhit;
                }
            }
        }
        return rawhitSimhit;
     
    }

    private Set<RawTrackerHit> getRawHitsFromTrackerHit(TrackerHit hit, RelationalTable rawtomc){

        Map<RawTrackerHit, SimTrackerHit> largestHitOnLayerMap = new HashMap<RawTrackerHit, SimTrackerHit>();
        Map<RawTrackerHit, Integer> layerMap = new HashMap<RawTrackerHit, Integer>(); 
        Set<RawTrackerHit> rawhitsPerLayer = new HashSet<RawTrackerHit>();
        Set<Integer> layers = new HashSet<Integer>();
        for(RawTrackerHit rawhit : (List<RawTrackerHit>) hit.getRawHits()){
            int layer = rawhit.getLayerNumber(); 
            layerMap.put(rawhit, layer);
            layers.add(layer);
        }

        for(int layer : layers){
            List<RawTrackerHit> rawhitsonLayer = new ArrayList<RawTrackerHit>();
            for(Map.Entry<RawTrackerHit,Integer> entry : layerMap.entrySet()){
                if(entry.getValue().equals(layer)){
                    rawhitsonLayer.add(entry.getKey());
                }
            
            }   
            double simhitMaxE = 0.0;
            RawTrackerHit bestRawhit = null;
            for(RawTrackerHit rawhit : rawhitsonLayer){
                SimTrackerHit simhit = this.getBestSimHit(rawhit, rawtomc);
                if (simhit != null && simhit.getMCParticle() != null) {
                    double simhitEnergy = simhit.getdEdx();
                    if(simhitEnergy > simhitMaxE){
                        simhitMaxE = simhitEnergy;
                        bestRawhit = rawhit;
                    }
                }
            }
            rawhitsPerLayer.add(bestRawhit);
        }
        return rawhitsPerLayer;
    }   
        
    public int getNSimTrackerHits(EventHeader event, MCParticle mcp){
        //Check how many hits this MCP left in the tracker
        int nmcpHits = 0;
        Set<Integer> layers = new HashSet<Integer>();
        List<SimTrackerHit> simhits = event.get(SimTrackerHit.class, "TrackerHits");
        for(SimTrackerHit simhit : simhits){
            if(layers.contains(simhit.getLayerNumber()))
                continue;
            MCParticle simhitmcp = simhit.getMCParticle();
            if(simhitmcp == mcp){
                layers.add(simhit.getLayerNumber());
                nmcpHits = nmcpHits + 1;
            }
        }
        return nmcpHits;
    }

    public Track getMcpBestTrack(MCParticle mcp, Map<MCParticle, Map<Track, Integer>> mcpTracksMap){
        int mostHits = 0;
        Track mcpBestTrack = null;
        for(Map.Entry<Track, Integer> entry : mcpTracksMap.get(mcp).entrySet()){
            if(entry.getValue() > mostHits){
                mostHits = entry.getValue();
                mcpBestTrack = entry.getKey();
            }
        }
        return mcpBestTrack;
    }

    public void getMCPTracks(EventHeader event, List<Track> tracks, Map<MCParticle,Map<Track,Integer>> mcpTracksMapIn, Map<Track, MCParticle> tracksMCPMapIn){

        //Retrieve rawhits to mc
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
        }

        List<MCParticle> allmcps = event.get(MCParticle.class,"MCParticle");
        Map<MCParticle,Map<Track, int[]>> mcpTracksMap = new HashMap<MCParticle,Map<Track, int[]>>();
        List<MCParticle> radFEEs = new ArrayList<MCParticle>();
        List<MCParticle> FEEs = new ArrayList<MCParticle>();
        List<MCParticle> primaryFEEs = new ArrayList<MCParticle>();
        List<MCParticle> mcp622 = new ArrayList<MCParticle>();
        List<MCParticle> mcp623 = new ArrayList<MCParticle>();

        double fee = 2.25;

        int nMCPs = allmcps.size();

        for(MCParticle mcp : allmcps){
            boolean skip = false;

            int nmcpHits = getNSimTrackerHits(event, mcp);
            plots1D.get("mcp_nHits_in_Tracker").fill(nmcpHits);
            Set<RawTrackerHit> rawhitsMCP = new HashSet<RawTrackerHit>();
            Map <Track, int[]> rawhitMultiplicity = new HashMap<Track, int[]>();

            int pdgid = mcp.getPDGID();
            List<Integer> parent_ids = new ArrayList<Integer>();
            List<MCParticle> parents = mcp.getParents();

            //ignore all photons
            if(Math.abs(pdgid) != 11)
                continue;
            double momentum = mcp.getMomentum().magnitude();


            boolean isRadiative = false;

            if(momentum > fee){
                parents = mcp.getParents();
                if(parents == null)
                    primaryFEEs.add(mcp);
                else{
                    for(MCParticle parent : parents){
                        if(parent.getMomentum().magnitude() > fee){
                            radFEEs.add(mcp);
                            break;
                        }
                    }
                }
            }


            else{
                if(parents == null)
                    continue;
                for(MCParticle parent : parents){
                    parent_ids.add(parent.getPDGID());
                }
            }

            //Skip mcp unless it is daughter of interesting event, or is FEE
            if((!parent_ids.contains(622) && !parent_ids.contains(623) && momentum < fee) || radFEEs.contains(mcp)){
                continue;
            }

            if(parent_ids.contains(622))
                mcp622.add(mcp);
            if(parent_ids.contains(623))
                mcp623.add(mcp);
            if(momentum > fee)
                FEEs.add(mcp);

            List<SimTrackerHit> simhits = event.get(SimTrackerHit.class, "TrackerHits");
            for(SimTrackerHit simhit : simhits){
                MCParticle simhitmcp = simhit.getMCParticle();
                if(simhitmcp == mcp){
                    Set<RawTrackerHit> rawhits = rawtomc.allTo(simhit);
                    for(RawTrackerHit hit : rawhits){
                        rawhitsMCP.add(hit);
                    }
                }
            }

            for(Track track : tracks){
                List<TrackerHit> trackerhits = track.getTrackerHits();
                for(TrackerHit trackerhit : trackerhits){
                    List<RawTrackerHit> trackerRawhits = trackerhit.getRawHits();
                    Set<Integer> layers = new HashSet<Integer>();
                    for(RawTrackerHit trackerRawhit : trackerRawhits){
                        int layer = trackerRawhit.getLayerNumber();
                        if(layers.contains(layer))
                            continue;
                        short [] adcs = trackerRawhit.getADCValues();
                        if(rawhitsMCP.contains(trackerRawhit)){
                            layers.add(layer);
                            if(!rawhitMultiplicity.containsKey(track)){
                                rawhitMultiplicity.put(track, new int[1]);
                                rawhitMultiplicity.get(track)[0] = 0;
                            }
                            rawhitMultiplicity.get(track)[0]++;
                        }
                    }
                }
                
                if(!rawhitMultiplicity.containsKey(track))
                    continue;
                plots1D.get("mcp_ofInterest_nRawTrackerHits_per_track").fill(rawhitMultiplicity.get(track)[0]);
                plots2D.get("mcp_ofInterest_momentum_v_nRawTrackerHits_per_track").fill(momentum,rawhitMultiplicity.get(track)[0]);
            }

            if(rawhitMultiplicity.size() == 0)
                continue;
            
            int mostHits = 0;
            Track mostHitsTrack = null;
            for(Map.Entry<Track, int[]> entry : rawhitMultiplicity.entrySet()){
                if(entry.getValue()[0] > mostHits){
                    mostHits = entry.getValue()[0];
                    mostHitsTrack = entry.getKey();
                }
            }

            if(mostHits > 0)
                plots1D.get("mcp_mostHitsTrack_nHits").fill(mostHits);

            mcpTracksMap.put(mcp,rawhitMultiplicity);
            plots1D.get("mcp_nTracks").fill(mcpTracksMap.get(mcp).size());
            plots2D.get("mcp_momentum_v_nTracks").fill(momentum,mcpTracksMap.get(mcp).size());
        }


        plots1D.get("nMCP_primary_FEEs_per_event").fill(FEEs.size());
        plots1D.get("nMCP_radiative_FEEs_per_event").fill(radFEEs.size());
        plots1D.get("nMCP_622_primary_daughters_per_event").fill(mcp622.size());
        plots1D.get("nMCP_623_primary_daughters_per_event").fill(mcp623.size());
        plots2D.get("nMCP_622or623_primary_daughters_v_primary_FEEs_per_event").fill(mcp622.size()+mcp623.size(),FEEs.size());
        plots2D.get("nMCP_622and623_primary_daughters_and_primary_FEEs_v_nTracks_per_event").fill(mcp622.size()+mcp623.size()+FEEs.size(),tracks.size());

        //Disambiguate MCP Track matches. Tracks must be exclusively matched to
        //a MCP
        Map<Track, Map<MCParticle,Integer>> trackMap = new HashMap<Track, Map<MCParticle, Integer>>();
        for(Track track : tracks){
            Map<MCParticle, Integer> bMap = new HashMap<MCParticle, Integer>();
            for(Map.Entry<MCParticle, Map<Track, int[]>> entry : mcpTracksMap.entrySet()){
                if(entry.getValue().containsKey(track)){
                    bMap.put(entry.getKey(),entry.getValue().get(track)[0]);
                }
            }
            if(bMap.size() > 0)
                trackMap.put(track, bMap);
            else
                trackMap.put(track, null);
        }

        List<Track> unmatchedTracks = new ArrayList<Track>();
        Map<MCParticle, Map<Track, Integer>> mcpTracksMapDisamb = new HashMap<MCParticle, Map<Track, Integer>>();

        for(Map.Entry<Track, Map<MCParticle,Integer>> entry : trackMap.entrySet()){

            double[] trackP = TrackUtils.getTrackStateAtLocation(entry.getKey(),TrackState.AtIP).getMomentum();
            double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
            if(entry.getValue() == null){
                unmatchedTracks.add(entry.getKey());
                plots1D.get("nMCPs_on_track").fill(0);
                plots1D.get("tracks_unmatched_to_mcp_ofInterest_momentum").fill(trackPmag);
                continue;
            }

            int mosthits = 0;
            MCParticle bestMCP = null;
            double bestMCPmomentum = 0.0;
            Map<MCParticle, Integer> bMap = entry.getValue();
            plots1D.get("nMCPs_on_track").fill(bMap.size());

            for(Map.Entry<MCParticle, Integer> subentry : bMap.entrySet()){
                if(subentry.getValue() == mosthits){
                    System.out.println("Tie between two MCPs with " + mosthits + " for this track!");
                    System.out.println("Momentum of previous best mcp: " + bestMCPmomentum);
                    System.out.println("Momentum of current mcp: " + subentry.getKey().getMomentum().magnitude());
                    System.out.println("Momentum of current track: " + trackPmag);
                    if(Math.abs((1-(subentry.getKey().getMomentum().magnitude()/trackPmag))) <= 
                            Math.abs((1-(bestMCPmomentum/trackPmag)))){
                        bestMCP = subentry.getKey();
                        bestMCPmomentum = subentry.getKey().getMomentum().magnitude();
                        mosthits = subentry.getValue();
                        System.out.println("New best MCP momentum " + bestMCPmomentum);
                    }
                }
                if(subentry.getValue() > mosthits){
                    mosthits = subentry.getValue();
                    bestMCP = subentry.getKey();
                    bestMCPmomentum = subentry.getKey().getMomentum().magnitude();
                }
            }

            //Add track and best MCP to map
            tracksMCPMapIn.put(entry.getKey(),bestMCP);

            //Build map of MCP with list of best Tracks
            if(!mcpTracksMapDisamb.containsKey(bestMCP)){
                Map<Track, Integer> tmpMap = new HashMap<Track,Integer>();
                tmpMap.put(entry.getKey(),mosthits);
                mcpTracksMapDisamb.put(bestMCP,tmpMap);
                mcpTracksMapIn.put(bestMCP, tmpMap);
            }
            else{
                Map<Track, Integer> gammaMap = mcpTracksMapDisamb.get(bestMCP);
                gammaMap.put(entry.getKey(),mosthits);
                mcpTracksMapDisamb.put(bestMCP,gammaMap);
                mcpTracksMapIn.put(bestMCP, gammaMap);
            }
        }

        Set<Track> checkTracks = new HashSet<Track>();
        int nNonFeeMCPwithTracks=0;
        int nFEEwithTracks = 0;
        int nMCPwithTracks = 0;
        for(Map.Entry<MCParticle, Map<Track, Integer>> entry : mcpTracksMapDisamb.entrySet()){
            MCParticle mcp = entry.getKey();
            Map<Track, Integer> tmpMap = entry.getValue();

            int sumHitsOnTracks = 0;
            for(Map.Entry<Track, int[]> og : mcpTracksMap.get(mcp).entrySet()){
                sumHitsOnTracks = sumHitsOnTracks + og.getValue()[0];
                plots2D.get("mcp_nSimTrackerHits_v_sum_nHitsOnTrack").fill(getNSimTrackerHits(event,mcp),sumHitsOnTracks);
            }

            boolean isFEE = false;
            if(mcp.getMomentum().magnitude() > fee){
                isFEE = true;
                plots1D.get("mcp_FEE_nTracks_disambiguated").fill(tmpMap.size());
                if(tmpMap.size() > 0)
                    nFEEwithTracks = nFEEwithTracks + 1;
            }
            else{
                plots1D.get("mcp_nonFEE_nTracks_disambiguated").fill(tmpMap.size());
                if(tmpMap.size() > 0){
                    nNonFeeMCPwithTracks = nNonFeeMCPwithTracks + 1;
                }
            }

            if(tmpMap.size() > 0){
                nMCPwithTracks = nMCPwithTracks + 1;
            }
            
            plots1D.get("mcp_nTracks_disambiguated").fill(entry.getValue().size());
            plots2D.get("mcp_momentum_v_nTracks_disambiguated").fill(mcp.getMomentum().magnitude(),tmpMap.size());

            int mosthits = 0;
            Track mosthitsTrack = null;
            for(Map.Entry<Track, Integer> subentry : tmpMap.entrySet()){
                Track track = subentry.getKey();
                int nhits = subentry.getValue();
                plots1D.get("mcp_nhits_on_track_disambiguated").fill(nhits);
                if(nhits > mosthits){
                    mosthits = nhits;
                    mosthitsTrack = track;
                }
                if(checkTracks.contains(track))
                    System.out.println("FAILURE! TRACKS NOT DISAMBIGUATED");
                checkTracks.add(track);
            }
            plots1D.get("mcp_most_hits_on_track_disambiguated").fill(mosthits);

            double[] trackP = TrackUtils.getTrackStateAtLocation(mosthitsTrack,TrackState.AtIP).getMomentum();
            double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
            double [] params = mosthitsTrack.getTrackParameters();

            if(isFEE)
                plots1D.get("mcp_FEEs_most_hits_on_track_disambiguated").fill(mosthits);
            else
                plots1D.get("mcp_NOT_FEEs_most_hits_on_track_disambiguated").fill(mosthits);

            if(mosthits == 1){
                System.out.println("ANALYZE! MCP 'most hits on Track' = 1");
                System.out.println("MCP energy: " + mcp.getEnergy());
                System.out.println("MCP mom: " + mcp.getMomentum().magnitude() + "; energy: " + mcp.getEnergy());
                System.out.println("MCP px: " + mcp.getMomentum().x() + "; py: " + mcp.getMomentum().y() + "; pz: " + mcp.getMomentum().z());
                System.out.println("Track momentum: " + trackPmag);
                System.out.println("Track px: " + trackP[0] + "; py: " + trackP[1] + "; pz: " + trackP[2]);
                System.out.println("Track d0: " + params[0] + "; phi0: " + params[1] + "; omega: " + params[2] + "; z0: " + params[3] + "; tanlambda: " + params[4]);
                System.out.println("Ntracks corresponding to this MCP before disambiguation: " + mcpTracksMap.get(mcp).size());
                System.out.println("Ntracks corresponding to this MCP after disambiguation: " + entry.getValue().size());
                System.out.println("Where are the remaining hits on this Track? Before disambiguation");
                Map<MCParticle, Integer> map = trackMap.get(mosthitsTrack);
                System.out.println("Track matched to " + map.size() + " other MCPs before disamb");
                for(Map.Entry<MCParticle, Integer> e : map.entrySet()){
                    System.out.println("Track includes mcp of energy : " + e.getKey().getEnergy() + "with " + e.getValue() + " hits");
                }
            }

            plots2D.get("mcp_momentum_v_best_track_momentum_disam").fill(mcp.getMomentum().magnitude(),trackPmag);
            plots1D.get("mcp_momentum_best_track_momentum_ratio_disam").fill(mcp.getMomentum().magnitude()/trackPmag);
            plots2D.get("mcp_nSimTrackerHits_most_nHitsOnTrack_disam").fill(getNSimTrackerHits(event,mcp),mosthits);

            //Get scoringplane hit and plot
            SimTrackerHit scoringplanehit = getTrackScoringPlaneHit(event, mcp, ecalScoringPlaneHitsCollectionName);
            if(scoringplanehit != null)
                trackScoringPlanePlots(event, mosthitsTrack, scoringplanehit);
        }

        plots1D.get("mcp_FEE_wAtLeast_one_track_per_event_disamb").fill(nFEEwithTracks);
        plots1D.get("mcp_NOT_FEE_wAtLeast_one_track_per_event_disamb").fill(nNonFeeMCPwithTracks);

        //return mcpTracksMapDisamb;
    }

    public MCParticle newgetTrackMCP(EventHeader event, Track track, String trackCollectionName, Boolean hitRequirement){

        Map <MCParticle, int[]>mcParticleMultiplicity = new HashMap<MCParticle, int[]>();

        //Retrieve rawhits to mc
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
        }

        MCParticle particle;
        for(TrackerHit hit : track.getTrackerHits()){
            Set<RawTrackerHit> rawhits = getRawHitsFromTrackerHit(hit, rawtomc);
            for(RawTrackerHit rawhit : rawhits){
                SimTrackerHit simhit = getBestSimHit(rawhit, rawtomc);
                if(simhit != null && simhit.getMCParticle() != null){
                    particle = simhit.getMCParticle();
                    if(!mcParticleMultiplicity.containsKey(particle)){
                        mcParticleMultiplicity.put(particle, new int[1]);
                        mcParticleMultiplicity.get(particle)[0] = 0;
                    }

                    mcParticleMultiplicity.get(particle)[0]++;
                }
            }

        }

        // Look for the MC particle that occurs the most of the track
        int maxValue = 0;
        particle = null;
        for(Map.Entry<MCParticle, int[]> entry : mcParticleMultiplicity.entrySet()){
            if(maxValue < entry.getValue()[0]){
                particle = entry.getKey();
                maxValue = entry.getValue()[0];
            }
        }

        int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());
        double[] trackP = TrackUtils.getTrackStateAtLocation(track,TrackState.AtLastHit).getMomentum();
        double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
        double truthP = particle.getMomentum().magnitude();

        if(charge > 0 && particle.getPDGID() == 11)
            return null;

        if(charge < 0 && particle.getPDGID() == -11)
            return null;

        if(charge < 0){
            plots1D.get(String.format("%s_ele_track_maxMCPmultiplicity",this.trackCollectionName)).fill(maxValue);
            plots2D.get(String.format("%s_ele_track_p_v_maxMCPmultiplicity",this.trackCollectionName)).fill(trackPmag,maxValue);
            plots2D.get(String.format("%s_ele_track_p_v_mcp_p",this.trackCollectionName)).fill(trackPmag,truthP);
        }
        else{
            plots1D.get(String.format("%s_pos_track_maxMCPmultiplicity",this.trackCollectionName)).fill(maxValue);
            plots2D.get(String.format("%s_pos_track_p_v_maxMCPmultiplicity",this.trackCollectionName)).fill(trackPmag,maxValue);
            plots2D.get(String.format("%s_pos_track_p_v_mcp_p",this.trackCollectionName)).fill(trackPmag,truthP);
        }

        if(hitRequirement == true){

            if(trackCollectionName.contains("GBLTracks")){
                if(maxValue > 9)
                    return particle;
                else
                    return null;
            }
            else if(trackCollectionName.contains("KalmanFullTracks")){
                if(maxValue > 9)
                    return particle;
                else
                    return null;
            }
            else
                return null;
        }
        else
            return particle;
    }

    private MCParticle getTrackMCP(Track track, EventHeader event){
        
        Map <MCParticle, int[]>mcParticleMultiplicity = new HashMap<MCParticle, int[]>();

        //Retrieve rawhits to mc
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
        }

        //Retrieve all simulated hits
        String MCHitInputCollectionName = "TrackerHits";
        List<SimTrackerHit> allsimhits = event.get(SimTrackerHit.class, MCHitInputCollectionName);

        MCParticle particle;
        for(TrackerHit hit : track.getTrackerHits()){
            List<RawTrackerHit> rawhits = hit.getRawHits();
            Set<RawTrackerHit> EmaxRawHitPerLayer = new HashSet<RawTrackerHit>();
            Map<RawTrackerHit,SimTrackerHit> rawToSimMap = new HashMap<RawTrackerHit, SimTrackerHit>();

            for(RawTrackerHit rawhit : rawhits){
                if(rawhit == null)
                    continue;
                Set<SimTrackerHit> simhits = rawtomc.allFrom(rawhit);
                SimTrackerHit rawhitSimhit = null;
                double simhitMaxE = 0.0;
                for(SimTrackerHit simhit : simhits){
                    if (simhit != null && simhit.getMCParticle() != null) {
                        double simhitEnergy = simhit.getdEdx();
                        if(simhitEnergy > simhitMaxE){
                            simhitMaxE = simhitEnergy;
                            rawhitSimhit = simhit;
                        }
                    }
                }
                if(rawhitSimhit != null)
                    rawToSimMap.put(rawhit, rawhitSimhit);
            }

            Map<RawTrackerHit, Integer> layerMap = new HashMap<RawTrackerHit, Integer>(); 
            Set<Integer> layers = new HashSet<Integer>();
            for(RawTrackerHit rawhit : rawToSimMap.keySet()){
                int layer = rawToSimMap.get(rawhit).getLayer(); 
                layerMap.put(rawhit, layer);
                layers.add(layer);
            }

            for(int layer : layers){
                List<RawTrackerHit> rawhitsonLayer = new ArrayList<RawTrackerHit>();
                for(Map.Entry<RawTrackerHit,Integer> entry : layerMap.entrySet()){
                    if(entry.getValue().equals(layer)){
                        rawhitsonLayer.add(entry.getKey());
                    }
                
                }   
                double simhitMaxE = 0.0;
                SimTrackerHit bestSimhit = null;
                for(RawTrackerHit rawhit : rawhitsonLayer){
                    SimTrackerHit simhit = rawToSimMap.get(rawhit);
                    if (simhit != null && simhit.getMCParticle() != null) {
                        double simhitEnergy = simhit.getdEdx();
                        if(simhitEnergy > simhitMaxE){
                            simhitMaxE = simhitEnergy;
                            bestSimhit = simhit;
                        }
                    }
                }

                if(bestSimhit != null && bestSimhit.getMCParticle() != null){
                    particle = bestSimhit.getMCParticle();
                    if(!mcParticleMultiplicity.containsKey(particle)){
                        mcParticleMultiplicity.put(particle, new int[1]);
                        mcParticleMultiplicity.get(particle)[0] = 0;
                    }

                    mcParticleMultiplicity.get(particle)[0]++;
                }
            }
        }

        // Look for the MC particle that occurs the most of the track
        int maxValue = 0;
        int minhits = 9;
        particle = null;
        for(Map.Entry<MCParticle, int[]> entry : mcParticleMultiplicity.entrySet()){
            if(maxValue < entry.getValue()[0]){
                particle = entry.getKey();
                maxValue = entry.getValue()[0];
            }
        }

        int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());
        if(charge < 0){
            plots1D.get(String.format("%s_ele_track_maxMCPmultiplicity",this.trackCollectionName)).fill(maxValue);
        }
        else{
            plots1D.get(String.format("%s_pos_track_maxMCPmultiplicity",this.trackCollectionName)).fill(maxValue);
        }
        if(this.trackCollectionName.contains("GBLTracks")){
            if(maxValue > minhits)
                return particle;
            else
                return null;
        }
        else if(this.trackCollectionName.contains("KalmanFullTracks")){
            if(maxValue > minhits)
                return particle;
            else
                return null;
        }
        else
            return null;
    }

}


