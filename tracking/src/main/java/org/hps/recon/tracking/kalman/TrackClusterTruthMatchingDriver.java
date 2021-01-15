//package org.hps.recon.test.ecalScoringPlane;
package org.hps.recon.tracking.kalman;


import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import org.hps.recon.ecal.cluster.ClusterUtilities;


import java.util.HashMap;
import java.util.List; 
import java.util.ArrayList; 
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackData;
import org.hps.util.Pair;
import org.hps.util.RK4integrator;
//import org.lcsim.event.GenericObject;


import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.RawTrackerHit;
//import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.util.Driver;
import org.lcsim.geometry.Detector;
import org.lcsim.event.TrackState;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.CalorimeterHit;
//import org.lcsim.event.RawCalorimeterHit;
//import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
/** 
 * Driver stolen from Omar to relate a Track to an Ecal scoring plane hit
 *
 **/

public class TrackClusterTruthMatchingDriver extends Driver {

    private org.lcsim.geometry.FieldMap fM;
    private ITree tree;
    private IHistogramFactory histogramFactory;
    private Map<String, IHistogram1D> plots1D;
    private Map<String, IHistogram2D> plots2D;
    //Histogram special identifiers
    //String[] identifiers = {"positive_match", "negative_match", "truth_matched", "no_match"};
    String[] identifiers = {"truth_matched"};

    RelationalTable hitToRotated = null;
    RelationalTable hitToStrips = null;
    RelationalTable TrktoData = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);

    TrackClusterMatcher2019 matcher;

    //Counting fake rate
    boolean verbose = true;
    double eleRecoRate;
    double posRecoRate;
    double eleFakeRate=0;
    double posFakeRate=0;
    double eleEfficiency=0;
    double posEfficiency=0;
    double NgeneratedEle = 0;
    double NgeneratedPos = 0;
    double NrecoEle = 0;
    double NrecoPos = 0;
    double NtruthEleClustPairs = 0;
    double NtruthPosClustPairs = 0;
    double eleNegMatch=0;
    double eleTrueMatch=0;
    double posNegMatch=0;
    double posTrueMatch=0;
    double eleNoMatch = 0;
    double posNoMatch = 0;
    double eletotalCount=0;
    double postotalCount=0;


    //NEW TRACK PERFORMANCE VARS
    double NmatcherPosTrackClusterPairs = 0.0;
    double NmatcherEleTrackClusterPairs = 0.0;
    double NgoodmatcherPosTrackClusterPairs = 0.0;
    double NgoodmatcherEleTrackClusterPairs = 0.0;


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

    String ecalClustersCollectionName = "EcalClustersCorr";
    String ecalReadoutHitsCollectionName = "EcalReadoutHits";
    String ecalTruthRelationsName = "EcalTruthRelations";

    private Set<SimTrackerHit> simhitsontrack = new HashSet<SimTrackerHit>();


    public void setTrackClusterTimeOffset(double input) {
        trackClusterTimeOffset = input;
    }
    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
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
        System.out.println("[TrackClusterTruthMatchingDriver] Booking Histograms for " + trackCollectionName);
        plots1D = new HashMap<String, IHistogram1D>();
        plots2D = new HashMap<String, IHistogram2D>();
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        if(truthComparisons == true){
            //Plots showing residuals between track at Ecal and truth-matched
            //scoring plane hit that has been extrapolated to the Ecal
            plots1D.put(String.format("%s_ele_track_v_truth_atEcal_dx",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_v_truth_atEcal_dx",this.trackCollectionName), 200, -200, 200));
            plots1D.put(String.format("%s_ele_track_v_truth_atEcal_dy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_v_truth_atEcal_dy",this.trackCollectionName), 200, -200, 200));
            plots1D.put(String.format("%s_ele_track_v_truth_atEcal_dz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_v_truth_atEcal_dz",this.trackCollectionName), 200, -200, 200));
            plots1D.put(String.format("%s_ele_track_v_truth_atEcal_dr",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_v_truth_atEcal_dr",this.trackCollectionName), 200, -200, 200));
            plots1D.put(String.format("%s_ele_track_v_truth_atEcal_dt",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_v_truth_atEcal_dt",this.trackCollectionName), 200, -200, 200));

            plots1D.put(String.format("%s_pos_track_v_truth_atEcal_dx",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_v_truth_atEcal_dx",this.trackCollectionName), 200, -200, 200));
            plots1D.put(String.format("%s_pos_track_v_truth_atEcal_dy",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_v_truth_atEcal_dy",this.trackCollectionName), 200, -200, 200));
            plots1D.put(String.format("%s_pos_track_v_truth_atEcal_dz",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_v_truth_atEcal_dz",this.trackCollectionName), 200, -200, 200));
            plots1D.put(String.format("%s_pos_track_v_truth_atEcal_dr",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_v_truth_atEcal_dr",this.trackCollectionName), 200, -200, 200));
            plots1D.put(String.format("%s_pos_track_v_truth_atEcal_dt",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_v_truth_atEcal_dt",this.trackCollectionName), 200, -200, 200));

            //Track extrapolation to Ecal: Momentum vs truth-extrap position
            //residuals
            plots2D.put(String.format("%s_ele_truthMCP_RK4ScoringPlaneToEcal_ZvP",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_truthMCP_RK4ScoringPlaneToEcal_ZvP",this.trackCollectionName), 1500, 0, 1500,300,0,3));

            plots2D.put(String.format("%s_pos_truthMCP_RK4ScoringPlaneToEcal_ZvP",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_truthMCP_RK4ScoringPlaneToEcal_ZvP",this.trackCollectionName), 1500, 0, 1500,300,0,3));


            //Plots showing the track-cluster residuals for the different match
            //identifiers: positive_match, negative_match, no_match
            for(String id: identifiers){
                plots1D.put(String.format("%s_ele_track_cluster_%s_dx",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_ele_track_cluster_%s_dx",this.trackCollectionName,id), 200, -200, 200));
                plots1D.put(String.format("%s_ele_track_cluster_%s_dy",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_ele_track_cluster_%s_dy",this.trackCollectionName,id), 200, -200, 200));
                plots1D.put(String.format("%s_ele_track_cluster_%s_dz",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_ele_track_cluster_%s_dz",this.trackCollectionName,id), 200, -200, 200));
                plots1D.put(String.format("%s_ele_track_cluster_%s_dr",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_ele_track_cluster_%s_dr",this.trackCollectionName,id), 200, -200, 200));
                plots1D.put(String.format("%s_ele_track_cluster_%s_dt",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_ele_track_cluster_%s_dt",this.trackCollectionName,id), 200, -200, 200));

                plots1D.put(String.format("%s_pos_track_cluster_%s_dx",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_pos_track_cluster_%s_dx",this.trackCollectionName,id), 200, -200, 200));
                plots1D.put(String.format("%s_pos_track_cluster_%s_dy",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_pos_track_cluster_%s_dy",this.trackCollectionName,id), 200, -200, 200));
                plots1D.put(String.format("%s_pos_track_cluster_%s_dz",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_pos_track_cluster_%s_dz",this.trackCollectionName,id), 200, -200, 200));
                plots1D.put(String.format("%s_pos_track_cluster_%s_dr",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_pos_track_cluster_%s_dr",this.trackCollectionName,id), 200, -200, 200));
                plots1D.put(String.format("%s_pos_track_cluster_%s_dt",this.trackCollectionName,id), histogramFactory.createHistogram1D(String.format("%s_pos_track_cluster_%s_dt",this.trackCollectionName,id), 200, -200, 200));
            }
            //Track reconstruction efficiency
            plots1D.put(String.format("%s_track_reco_efficiency",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_track_reco_efficiency",this.trackCollectionName), 10, 0, 10));
            //Number of duplicate MCPs matched to reco Tracks
            plots1D.put(String.format("%s_n_duplicate_TrackMCP_matches",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_n_duplicate_TrackMCP_matches",this.trackCollectionName), 10, 0, 10));

            //Checking the track-cluster matching fake rate and matching efficiency
            plots1D.put(String.format("%s_ele_fakeRate",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_fakeRate",this.trackCollectionName), 10, 0, 10));
            plots1D.put(String.format("%s_pos_fakeRate",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_fakeRate",this.trackCollectionName), 10, 0, 10));
            plots1D.put(String.format("%s_ele_Efficiency",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_Efficiency",this.trackCollectionName), 10, 0, 10));
            plots1D.put(String.format("%s_pos_Efficiency",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_Efficiency",this.trackCollectionName), 10, 0, 10));

            //Following plots used to identify quality of track truth matching
            plots1D.put(String.format("%s_ele_track_truth_matched_w_cluster_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_truth_matched_w_cluster_momentum",this.trackCollectionName), 160, 0, 4));
            plots1D.put(String.format("%s_pos_track_truth_matched_w_cluster_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_truth_matched_w_cluster_momentum",this.trackCollectionName), 160, 0, 4));

            //track and truthtrack momentum
            plots1D.put(String.format("%s_ele_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_momentum",this.trackCollectionName), 160, 0, 4));
            plots1D.put(String.format("%s_pos_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_momentum",this.trackCollectionName), 160, 0, 4));


            plots1D.put(String.format("%s_ele_delta_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_delta_momentum",this.trackCollectionName), 400, 0, 4));
            plots1D.put(String.format("%s_pos_delta_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_delta_momentum",this.trackCollectionName), 400, 0, 4));

            plots1D.put(String.format("%s_ele_truth_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_momentum",this.trackCollectionName), 160, 0, 4));
            plots1D.put(String.format("%s_pos_truth_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_momentum",this.trackCollectionName), 160, 0, 4));
            plots1D.put(String.format("%s_pos_truth_track_MCP_momentum_ratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_MCP_momentum_ratio",this.trackCollectionName), 160, 0, 4));
            plots1D.put(String.format("%s_ele_truth_track_MCP_momentum_ratio",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_MCP_momentum_ratio",this.trackCollectionName), 160, 0, 4));


            //duplicate track momentum
            plots1D.put(String.format("%s_ele_duplicate_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_duplicate_track_momentum",this.trackCollectionName), 160, 0, 4));
            plots1D.put(String.format("%s_pos_duplicate_track_momentum",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_duplicate_track_momentum",this.trackCollectionName), 160, 0, 4));
            //Checking E/P for track+cluster pair
            
            plots1D.put(String.format("%s_ele_truth_track_matched_w_cluster_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_matched_w_cluster_EdivP",trackCollectionName),  1000, 0, 10));
            plots1D.put(String.format("%s_pos_truth_track_matched_w_cluster_EdivP",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_matched_w_cluster_EdivP",trackCollectionName),  1000, 0, 10));
            plots1D.put(String.format("%s_cluster_energy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_cluster_energy",trackCollectionName),  100, 0, 5));
            plots1D.put(String.format("%s_truth_cluster_energy",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_truth_cluster_energy",trackCollectionName),  100, 0, 5));

            //Matched track clusters
            plots1D.put(String.format("%s_ele_truthTrack_electron_cluster_E_div_P",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truthTrack_electron_cluster_E_div_P",trackCollectionName),  1000, 0, 10));
            plots1D.put(String.format("%s_ele_truthTrack_positron_cluster_E_div_P",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truthTrack_positron_cluster_E_div_P",trackCollectionName),  1000, 0, 10));
            plots1D.put(String.format("%s_ele_truthTrack_photon_cluster_E_div_P",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truthTrack_photon_cluster_E_div_P",trackCollectionName),  1000, 0, 10));

            plots1D.put(String.format("%s_pos_truthTrack_electron_cluster_E_div_P",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truthTrack_electron_cluster_E_div_P",trackCollectionName),  1000, 0, 10));
            plots1D.put(String.format("%s_pos_truthTrack_positron_cluster_E_div_P",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truthTrack_positron_cluster_E_div_P",trackCollectionName),  1000, 0, 10));
            plots1D.put(String.format("%s_pos_truthTrack_photon_cluster_E_div_P",trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truthTrack_photon_cluster_E_div_P",trackCollectionName),  1000, 0, 10));

            //Plots for tracks in ecal fiducial region

            plots1D.put(String.format("%s_ele_truth_track_momentum_within_ecal",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_truth_track_momentum_within_ecal",this.trackCollectionName), 160, 0, 4));
            plots1D.put(String.format("%s_pos_truth_track_momentum_within_ecal",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_truth_track_momentum_within_ecal",this.trackCollectionName), 160, 0, 4));
            plots1D.put(String.format("%s_ele_track_momentum_within_ecal",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_momentum_within_ecal",this.trackCollectionName), 160, 0, 4));
            plots1D.put(String.format("%s_pos_track_momentum_within_ecal",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_momentum_within_ecal",this.trackCollectionName), 160, 0, 4));


            //Hit multiplicity for truth matching
            plots1D.put(String.format("%s_ele_track_maxMCPmultiplicity",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_maxMCPmultiplicity",this.trackCollectionName),30, 0, 30));
            plots1D.put(String.format("%s_pos_track_maxMCPmultiplicity",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_maxMCPmultiplicity",this.trackCollectionName),30, 0, 30));

            //track positions at Ecal in XY plane
            plots2D.put(String.format("%s_ele_track_atEcal_xyplane_pos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_ele_track_atEcal_xyplane_pos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
            plots2D.put(String.format("%s_pos_track_atEcal_xyplane_pos",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("%s_pos_track_atEcal_xyplane_pos",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
            //Cluster Positions in XY plane
            plots2D.put(String.format("ecal_cluster_positions_xy_plane",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("ecal_cluster_positions_xy_plane",this.trackCollectionName),1000, -500, 500,1000, -500, 500));
            plots2D.put(String.format("truth_ecal_cluster_positions_xy_plane",this.trackCollectionName), histogramFactory.createHistogram2D(String.format("truth_ecal_cluster_positions_xy_plane",this.trackCollectionName),1000, -500, 500,1000, -500, 500));


            //Check track quality

            plots1D.put(String.format("%s_ele_track_chi2divndf",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_ele_track_chi2divndf",this.trackCollectionName), 200, 0, 200));
            plots1D.put(String.format("%s_pos_track_chi2divndf",this.trackCollectionName), histogramFactory.createHistogram1D(String.format("%s_pos_track_chi2divndf",this.trackCollectionName), 200, 0, 200));
        }

       // plots1D.put(String.format("KFvGBL_truthmatchedTrack_dp"), histogramFactory.createHistogram1D(String.format("KFvGBL_truthmatchedTrack_dp"), 400, -4, 4));
        //plots1D.put(String.format("KFvGBL_truthmatchedTrack_dx"), histogramFactory.createHistogram1D(String.format("KFvGBL_truthmatchedTrack_dx"), 500, -500, 500));


    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void startOfData() {
        System.out.println("Starting job");
        bookHistograms();
        matcher = new TrackClusterMatcher2019(this.trackCollectionName);
    }

    public void endOfData() {
        eleFakeRate = eleNegMatch/(eleNegMatch+eleTrueMatch);
        posFakeRate = posNegMatch/(posNegMatch + posTrueMatch);
        eleEfficiency = (eleNegMatch+eleTrueMatch)/NtruthEleClustPairs;
        posEfficiency = (posNegMatch+posTrueMatch)/NtruthPosClustPairs;
        eleRecoRate = NrecoEle/NgeneratedEle;
        posRecoRate = NrecoPos/NgeneratedPos;


        //track reco efficiency
        trackEfficiency = NrecoTruthTracks/NpossibleTracks;
        for(int i=0; i < Math.round(trackEfficiency*100); i++)
            plots1D.get(String.format("%s_track_reco_efficiency",this.trackCollectionName)).fill(1.0);
        System.out.println("LOOK! nDuplicates = " + nDuplicates);
        for(int i = 0; i < Math.round(nDuplicates); i++)
            plots1D.get(String.format("%s_n_duplicate_TrackMCP_matches",this.trackCollectionName)).fill(1.0);

        if(truthComparisons == true){
            for(int i=0; i < Math.round(eleFakeRate*100); i++)
                plots1D.get(String.format("%s_ele_fakeRate",this.trackCollectionName)).fill(1.0);
            for(int i=0; i < Math.round(posFakeRate*100); i++)
                plots1D.get(String.format("%s_pos_fakeRate",this.trackCollectionName)).fill(1.0);
            for(int i=0; i < Math.round(posEfficiency*100); i++)
                plots1D.get(String.format("%s_pos_Efficiency",this.trackCollectionName)).fill(1.0);
            for(int i=0; i < Math.round(eleEfficiency*100); i++)
            plots1D.get(String.format("%s_ele_Efficiency",this.trackCollectionName)).fill(1.0);
        }
        saveHistograms();

        System.out.println("NmatcherPosTrackClusterPairs" + NmatcherPosTrackClusterPairs);
        System.out.println("NmatcherEleTrackClusterPairs" + NmatcherEleTrackClusterPairs);
        System.out.println("NgoodmatcherPosTrackClusterPairs"+ NgoodmatcherPosTrackClusterPairs);
        System.out.println("NgoodmatcherEleTrackClusterPairs"+ NgoodmatcherEleTrackClusterPairs);
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

    public void compareKFtoGBL(EventHeader event) {
    
        if(this.trackCollectionName.contains("GBLTracks"))
                return;
        if(!event.hasCollection(Track.class, "KalmanFullTracks")) return;
        if(!event.hasCollection(Track.class, "GBLTracks")) return;

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




        List<Track> kfFEEs = new ArrayList<Track>();
        List<Track> gblFEEs = new ArrayList<Track>();
        List<Track> kftracks = event.get(Track.class, "KalmanFullTracks");
        List<Track> gbltracks = event.get(Track.class, "GBLTracks");
        List<TrackerHit> rotheltrackhits = event.get(TrackerHit.class, "RotatedHelicalTrackHits");
        Map<Track,MCParticle> gblmcps = new HashMap<Track, MCParticle>();
        Map<Track,MCParticle> kfmcps = new HashMap<Track, MCParticle>();
        Map<Track, Track> KFtoGBLmcpMap = new HashMap<Track,Track>();


    }
    protected void process(EventHeader event) {


        if(truthComparisons == true){
            //Get collection of Ecal clusters
            //If event has no collection of tracks, skip
            if(!event.hasCollection(Track.class, trackCollectionName)) return;
            //If even doesnt have collection of Ecal scoring plane hits, skip
            if(!event.hasCollection(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName)) return;

            //Get EcalClusters from event
            List<Cluster> clusters = event.get(Cluster.class, ecalClustersCollectionName);
            for(Cluster cluster : clusters) {
            }
            //Truth Match Clusters in LCIO to MCParticles    
            List<Cluster> truthClusters = new ArrayList<Cluster>();
            Map<Cluster,MCParticle> truthClustersMap = new HashMap<Cluster,MCParticle>();
            for(Cluster cluster : clusters){
                double clustx = cluster.getPosition()[0];
                double clusty = cluster.getPosition()[1];
                double clustz = cluster.getPosition()[2];
                double clusterEnergy = cluster.getEnergy();

                MCParticle clusterMCP = getClusterMCP(cluster,event);

                plots2D.get(String.format("ecal_cluster_positions_xy_plane")).fill(clustx,clusty);
                plots1D.get(String.format("%s_cluster_energy",trackCollectionName)).fill(clusterEnergy);

                if(clusterMCP == null)
                    continue;
                truthClusters.add(cluster);
                truthClustersMap.put(cluster, clusterMCP);

                plots2D.get(String.format("truth_ecal_cluster_positions_xy_plane")).fill(clustx,clusty);
                plots1D.get(String.format("%s_truth_cluster_energy",trackCollectionName)).fill(clusterEnergy);

            }

            // Get collection of tracks from event
            List<Track> tracks = event.get(Track.class, trackCollectionName);
            List<Track> truthTracks = new ArrayList<Track>();
            Map<Track,MCParticle> truthTracksMap = new HashMap<Track,MCParticle>();
            List<Track> truthTracks_w_truthClusters = new ArrayList<Track>();
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

            System.out.println("[TruthMatcher] Looping over tracks: " + tracks.size());
            //Looping Over all Tracks
            for(Track track : tracks){

                double trackT;
                if (this.trackCollectionName.contains("GBLTracks")){
                    trackT = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
                }
                else {
                    TrackData trackdata = (TrackData) TrktoData.from(track);
                    trackT = trackdata.getTrackTime();
                }
                if(Math.abs(trackT) > 10.0)
                    continue;
                //For GBL Tracks, there is a |10|ns track time cut applied in
                //reconstruciton, that is not applied for Kalman Tracks. I am enforcing
                //this cut here, for a fair comparison of GBL v KF

                //check track quality
                int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());
                double chi2 = track.getChi2();
                int ndf = track.getNDF();

                double trackx;
                double tracky;
                double trackz;
                if(this.trackCollectionName.contains("GBLTracks")) {
                    trackx = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[1];
                    tracky = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[2];
                    trackz = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[0];
                }
                else {
                    TrackState ts_ecal = track.getTrackStates().get(track.getTrackStates().size()-1);
                    double[] ts_ecalPos = ts_ecal.getReferencePoint();
                    trackx = ts_ecalPos[1];
                    tracky = ts_ecalPos[2];
                    trackz = ts_ecalPos[0];
                }

                //Define first order Ecal geometry --> assumed square here,
                //smaller than actual beamgap. Improve x geometry 
                double ecalx1 = -270.0; //mm
                double ecalx2 = 355.0;
                double ecaly1 = 85.0;
                double ecaly2 = -85.0;
                double bgapup = 29.0;
                double bgapdown = -29.0;
                double eholex1 = -87.5;
                double eholex2 = 12.5;
                double eholey1 = 42.0;
                double eholey2 = -42.0;

                boolean fiducial = true;

                if(trackx < ecalx1 || trackx > ecalx2)
                    fiducial = false;
                if(tracky > ecaly1 || tracky < ecaly2)
                    fiducial = false;;
                if(tracky < bgapup && tracky > bgapdown)
                    fiducial = false;;
                if((trackx > eholex1 && trackx < eholex2) && ( (tracky < eholey1) && (tracky > eholey2)))
                    fiducial = false;;

                if (fiducial == false)
                    continue;
                //be careful about this -1...Is it always going to be correct?
                //Check "flipsign" in ReconParticleDriver.java
                double[] trackP = track.getMomentum();
                double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
                double[] trackPfinal = track.getTrackStates().get(track.getTrackStates().size()-1).getMomentum();
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
                    if (fiducial == true) 
                        plots1D.get(String.format("%s_ele_track_momentum_within_ecal",this.trackCollectionName)).fill(trackPmag);
                }
                else{
                    plots1D.get(String.format("%s_pos_track_momentum",this.trackCollectionName)).fill(trackPmag);
                    plots1D.get(String.format("%s_pos_delta_momentum",this.trackCollectionName)).fill(deltaPmag);
                    plots1D.get(String.format("%s_pos_track_chi2divndf",this.trackCollectionName)).fill(chi2/((double) ndf));
                    if (fiducial == true) 
                        plots1D.get(String.format("%s_pos_track_momentum_within_ecal",this.trackCollectionName)).fill(trackPmag);
                }

                //truth match track to mcparticle
                MCParticle trackMCP =  newgetTrackMCP(event,track,this.trackCollectionName, true);
                //If no MCP is found for Track, continue to next Track
                if(trackMCP == null)
                    continue;
                truthTracks.add(track);
                truthTracksMap.put(track, trackMCP);
                //Get scoring plane hit associated with this track
                SimTrackerHit matchedScoringplaneHit = getTrackScoringPlaneHit(event, track, trackMCP, ecalScoringPlaneHitsCollectionName);
                if(matchedScoringplaneHit != null)
                    this.trackScoringPlanePlots(event, track, matchedScoringplaneHit);

                double mcpPmag = trackMCP.getMomentum().magnitude(); 
                double trackMCPratio = trackPmag/mcpPmag;
                if(charge < 0){
                    plots1D.get(String.format("%s_ele_truth_track_momentum",this.trackCollectionName)).fill(trackPmag);
                    plots1D.get(String.format("%s_ele_truth_track_MCP_momentum_ratio",this.trackCollectionName)).fill(trackMCPratio);
                    if(fiducial == true)
                        plots1D.get(String.format("%s_ele_truth_track_momentum_within_ecal",this.trackCollectionName)).fill(trackPmag);
                }
                else{
                    plots1D.get(String.format("%s_pos_truth_track_momentum",this.trackCollectionName)).fill(trackPmag);
                    plots1D.get(String.format("%s_pos_truth_track_MCP_momentum_ratio",this.trackCollectionName)).fill(trackMCPratio);
                    if(fiducial == true)
                        plots1D.get(String.format("%s_pos_truth_track_momentum_within_ecal",this.trackCollectionName)).fill(trackPmag);
                }

                //Truth match track to cluster using MCParticles of each
                Cluster truthTrackCluster = null;
                for(Map.Entry<Cluster,MCParticle> entry : truthClustersMap.entrySet()){
                    MCParticle clusterMCP = entry.getValue();
                    if(clusterMCP == trackMCP) {
                        truthTrackCluster = entry.getKey();                    
                        break;
                    }
                }

                if(truthTrackCluster != null){
                    double clusterEnergy = truthTrackCluster.getEnergy();
                    truthTracks_w_truthClusters.add(track);
                    truthTracktruthClusterMap.put(track, truthTrackCluster);

                    double truthTrackPmag = trackMCP.getMomentum().magnitude(); 
                    double truthClusterE = truthTrackCluster.getEnergy();

                    trackClusterAnalysis(track,truthTrackCluster,"truth_matched");

                    //Plot E/P for truth-matched tracks+clusters
                    if(charge < 0){
                        plots1D.get(String.format("%s_ele_track_truth_matched_w_cluster_momentum",this.trackCollectionName)).fill(trackPmag);
                        plots1D.get(String.format("%s_ele_truth_track_matched_w_cluster_EdivP",trackCollectionName)).fill(clusterEnergy/trackPmag);
                        NtruthEleClustPairs = NtruthEleClustPairs + 1;
                        System.out.println("Number of electron tracks truth matched to a cluster: " + NtruthEleClustPairs);
                    }
                    else{
                        plots1D.get(String.format("%s_pos_track_truth_matched_w_cluster_momentum",this.trackCollectionName)).fill(trackPmag);
                        plots1D.get(String.format("%s_pos_truth_track_matched_w_cluster_EdivP",trackCollectionName)).fill(clusterEnergy/trackPmag);
                        NtruthPosClustPairs = NtruthPosClustPairs + 1;
                        System.out.println("Number of positron tracks truth matched to a cluster: " + NtruthPosClustPairs);
                    }
                }
            }
            System.out.println("[TruthMatcher] Tracks truth matched: " + truthTracks.size());
            System.out.println("[TruthMatcher] Tracks truth matched with truth cluster: " + truthTracks_w_truthClusters.size());
            
            //Use matching algorithm to match tracks to clusters (without truth
            //info)
            Map<Track, Cluster> matchedTrackClusterMap = new HashMap<Track,Cluster>();
            //To check the performance of the Track+Cluster matching algorithm,
            //feed the algorithm the set of truth matched Tracks and truth
            //matched Clusters, so that comparison can be done.
            //matchedTrackClusterMap = matcher.trackClusterMatcher(truthTracks_w_truthClusters,event, this.trackCollectionName, truthClusters, this.trackClusterTimeOffset);
            matchedTrackClusterMap = matcher.trackClusterMatcher(truthTracks_w_truthClusters,event, this.trackCollectionName, truthClusters, this.trackClusterTimeOffset);
            
            System.out.println("Matching set of Truth Tracks to Clusters using algorithm");
            if(matchedTrackClusterMap != null){
                System.out.println("Number of Tracks matched using algorithm " + matchedTrackClusterMap.size());
                for (Map.Entry<Track,Cluster> entry : matchedTrackClusterMap.entrySet()){
                    Track track = entry.getKey();
                    int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());
                    double[] trackP = track.getMomentum();
                    double trackPmag = Math.sqrt(Math.pow(trackP[0],2) + Math.pow(trackP[1],2) + Math.pow(trackP[2],2));
                    //Function below checks if trackMCP == clusterMCP, or if
                    //cluster == null, and calculates terms for efficiency and fake
                    //rate

                    checkTrackClusterMatch(track, matchedTrackClusterMap.get(track), truthTracktruthClusterMap.get(track));

                    //Skip further analysis if no cluster is matched
                    if(entry.getValue() == null)
                        continue;


                    //Check the truth cluster id for each truth track to see if cluster
                    //id belongs to correct particle
                    MCParticle matchedClusterMCP = truthClustersMap.get(entry.getValue());
                    MCParticle matchedTrackMCP = truthTracksMap.get(track);
                    int clusterid = matchedClusterMCP.getPDGID();


                    String clusterParticle = null;
                    if(clusterid == -11)
                        clusterParticle = "positron";
                    if(clusterid == 11)
                        clusterParticle = "electron";
                    if(clusterid == 22)
                        clusterParticle = "photon";


                    //if(clusterParticle == null){
                      //  continue;
                   // }
                    if(charge < 0)
                        plots1D.get(String.format("%s_ele_truthTrack_%s_cluster_E_div_P",trackCollectionName, clusterParticle)).fill(matchedTrackClusterMap.get(track).getEnergy()/trackPmag);
                        NmatcherEleTrackClusterPairs = NmatcherEleTrackClusterPairs + 1.0; 
                        System.out.println("Number of ele tracks matched to a track by algorithm: " + NmatcherEleTrackClusterPairs);
                        if(matchedClusterMCP == matchedTrackMCP){
                            NgoodmatcherEleTrackClusterPairs = NgoodmatcherEleTrackClusterPairs + 1; 
                        }

                    else{
                        plots1D.get(String.format("%s_pos_truthTrack_%s_cluster_E_div_P",trackCollectionName, clusterParticle)).fill(matchedTrackClusterMap.get(track).getEnergy()/trackPmag);
                        NmatcherPosTrackClusterPairs = NmatcherPosTrackClusterPairs + 1.0; 
                        System.out.println("Number of ele tracks matched to a track by algorithm: " + NmatcherPosTrackClusterPairs);
                        if(matchedClusterMCP == matchedTrackMCP){
                            NgoodmatcherPosTrackClusterPairs = NgoodmatcherPosTrackClusterPairs + 1; 
                        }
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

        }
    }

    public Cluster truthMatchTracktoCluster(EventHeader event, Track track, MCParticle trackMCP, List<Cluster> clusters){

        /**
         * Get the MC particle associated with a track.
         * Fill mape with Track -> MCParticle
        **/
  

        if(trackMCP == null) 
            return null;

        /**
         * Truth matching Tracks with Clusters via track_MCParticle ==
         * cluster_MCParticle.
         * */

        Cluster truthTrackCluster = null;
        for (Cluster cluster : clusters) {
            MCParticle clusterMCP = getClusterMCP(cluster, event);
            //if MCParticles of cluster and track match, map the two.
            if(clusterMCP == trackMCP) {
                truthTrackCluster = cluster;                    
                break;
            }
        }
        return truthTrackCluster;
    }

    public SimTrackerHit getTrackScoringPlaneHit(EventHeader event, Track track, MCParticle trackMCP, String ecalScoringPlaneHitsCollectionName) {

        List<SimTrackerHit> scoringPlaneHits = event.get(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName);


        //Check for simtrackerhit MCP that matches trackMCP
        if(trackMCP == null)
            return null;
        SimTrackerHit matchedScoringPlaneHit = null;
        for(SimTrackerHit scoringPlaneHit : scoringPlaneHits){
            // If the MC particles don't match, move on to the next particle
            if(!(scoringPlaneHit.getMCParticle() == trackMCP)) continue;
            matchedScoringPlaneHit = scoringPlaneHit;
            // Once a match is found, there is no need to loop through the rest of the list
            break;
        }
        return matchedScoringPlaneHit;
    }

    public void checkTrackClusterMatch(Track track,Cluster matchedCluster, Cluster truthTrackCluster){
        
        //Get track parameters
        int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());

        if(matchedCluster == null){
            //trackClusterAnalysis(track,matchedCluster,"no_match");
            if(charge > 0) 
                posNoMatch = posNoMatch + 1.0;
            else
                eleNoMatch = eleNoMatch + 1.0;
            return;
        }

        if(matchedCluster == truthTrackCluster){
            //trackClusterAnalysis(track, matchedCluster,"positive_match");
            if(charge > 0){
                posTrueMatch = posTrueMatch + 1.0;
            }
            else{
                eleTrueMatch = eleTrueMatch + 1.0;
            }
        }
        else{
            //trackClusterAnalysis(track, matchedCluster,"negative_match");
            if(charge > 0){ 
                posNegMatch = posNegMatch + 1.0;
            }
            else{
                eleNegMatch = eleNegMatch + 1.0;
            }
        }

        eletotalCount = eleTrueMatch + eleNegMatch + eleNoMatch;
        postotalCount = posTrueMatch + posNegMatch + posNoMatch;
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

        double[] trackP = track.getMomentum();
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
            plots2D.get(String.format("%s_ele_track_atEcal_xyplane_pos",this.trackCollectionName)).fill(trackx,tracky);
            //Extrapolated Track momentum vs truth position residuals
            plots2D.get(String.format("%s_ele_truthMCP_RK4ScoringPlaneToEcal_ZvP",this.trackCollectionName)).fill(truthzpos, trackPmag);
            //Residuals between track extrapolated to Ecal Face, and truth hit
            //extrapolated to Ecal Face
            plots1D.get(String.format("%s_ele_track_v_truth_atEcal_dx",this.trackCollectionName)).fill(dx);
            plots1D.get(String.format("%s_ele_track_v_truth_atEcal_dy",this.trackCollectionName)).fill(dy);
            plots1D.get(String.format("%s_ele_track_v_truth_atEcal_dz",this.trackCollectionName)).fill(dz);
            plots1D.get(String.format("%s_ele_track_v_truth_atEcal_dr",this.trackCollectionName)).fill(dr);
            plots1D.get(String.format("%s_ele_track_v_truth_atEcal_dt",this.trackCollectionName)).fill(dt);
        }
        else {
            //track momentum. Truth and reco

            //Track X,Y position at Ecal
            plots2D.get(String.format("%s_pos_track_atEcal_xyplane_pos",this.trackCollectionName)).fill(trackx,tracky);
            //Extrapolated Track P vs truth position residuals
            plots2D.get(String.format("%s_pos_truthMCP_RK4ScoringPlaneToEcal_ZvP",this.trackCollectionName)).fill(truthzpos, trackPmag);
            //Track vs Cluster residuals at Ecal
            plots1D.get(String.format("%s_pos_track_v_truth_atEcal_dx",this.trackCollectionName)).fill(dx);
            plots1D.get(String.format("%s_pos_track_v_truth_atEcal_dy",this.trackCollectionName)).fill(dy);
            plots1D.get(String.format("%s_pos_track_v_truth_atEcal_dz",this.trackCollectionName)).fill(dz);
            plots1D.get(String.format("%s_pos_track_v_truth_atEcal_dr",this.trackCollectionName)).fill(dr);
            plots1D.get(String.format("%s_pos_track_v_truth_atEcal_dt",this.trackCollectionName)).fill(dt);
        }

    }

    public void trackClusterAnalysis(Track track, Cluster cluster,  String identifier) {
        //For comparing extrapolated track position  with position of
        //truth-matched cluster
        int charge = -1* (int) Math.signum(track.getTrackStates().get(0).getOmega());
        double trackx;
        double tracky;
        double trackz;
        double clustx;
        double clusty;
        double clustz;
        double dxoffset;
        double clusTime;
        double dt;
        double dx;
        double dy;
        double dz;
        double dr;
        double clusterEnergy; 
        String id = identifier; //positive_match, negative_match, truth_match
        double trackT;
        if (this.trackCollectionName.contains("GBLTracks")){
            trackT = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);
            trackx = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[1];
            tracky = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[2];
            trackz = TrackUtils.getTrackStateAtECal(track).getReferencePoint()[0];
          //  dxoffset = -5.5;
            dxoffset = 0.0;
        }
        else {
            TrackData trackdata = (TrackData) TrktoData.from(track);
            trackT = trackdata.getTrackTime();
            TrackState ts_ecal = track.getTrackStates().get(track.getTrackStates().size()-1);
            double[] ts_ecalPos = ts_ecal.getReferencePoint();
            trackx = ts_ecalPos[1];
            tracky = ts_ecalPos[2];
            trackz = ts_ecalPos[0];
            dxoffset = 0.0;
        }
        //double tracktOffset = 4.0;
        //double[] trackP = new double[3];
        //double[] trackP = track.getMomentum();
        
        if(cluster == null){
            clustx = 0;
            clusty = 0;
            clustz = 0;
            clusTime = 0;
            clusterEnergy = 0.0;
            dt = trackT;
        }
        else {
            clustx = cluster.getPosition()[0];
            clusty = cluster.getPosition()[1];
            clustz = cluster.getPosition()[2];
            clusterEnergy = cluster.getEnergy();
            clusTime = ClusterUtilities.getSeedHitTime(cluster);
            dt = clusTime - trackClusterTimeOffset - trackT;
        }

        dx = clustx - trackx;
        dy = clusty - tracky;
        dz = clustz - trackz;
        dr = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2) + Math.pow(dz,2));

        //Make plots
        if(charge < 0) {
            plots1D.get(String.format("%s_ele_track_cluster_%s_dx",this.trackCollectionName,id)).fill(dx);
            plots1D.get(String.format("%s_ele_track_cluster_%s_dy",this.trackCollectionName,id)).fill(dy);
            plots1D.get(String.format("%s_ele_track_cluster_%s_dz",this.trackCollectionName,id)).fill(dz);
            plots1D.get(String.format("%s_ele_track_cluster_%s_dr",this.trackCollectionName,id)).fill(dr);
            plots1D.get(String.format("%s_ele_track_cluster_%s_dt",this.trackCollectionName,id)).fill(dt);
        }
        else {
            plots1D.get(String.format("%s_pos_track_cluster_%s_dx",this.trackCollectionName,id)).fill(dx);
            plots1D.get(String.format("%s_pos_track_cluster_%s_dy",this.trackCollectionName,id)).fill(dy);
            plots1D.get(String.format("%s_pos_track_cluster_%s_dz",this.trackCollectionName,id)).fill(dz);
            plots1D.get(String.format("%s_pos_track_cluster_%s_dr",this.trackCollectionName,id)).fill(dr);
            plots1D.get(String.format("%s_pos_track_cluster_%s_dt",this.trackCollectionName,id)).fill(dt);
        }

    }

/**
     * Get the MC particle associated with a track.
     * 
     * @param track : Track to get the MC particle for
     * @return The MC particle associated with the track
     */
    private MCParticle getClusterMCP(Cluster cluster, EventHeader event) {
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

        Set<SimCalorimeterHit> simcalhits = rawtomc.allFrom(readoutMatchHit);
        double maxMCPEnergy = 0.0;
        MCParticle largestEnergyMCP = null;
        for(SimCalorimeterHit simcalhit : simcalhits) {
            for(int i=0; i < simcalhit.getMCParticleCount(); i++){
                if(simcalhit.getMCParticle(i).getEnergy() > maxMCPEnergy) {
                    maxMCPEnergy = simcalhit.getMCParticle(i).getEnergy();
                    largestEnergyMCP = simcalhit.getMCParticle(i);
                }
            }
        }

        return largestEnergyMCP;
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
        if(charge < 0){
            plots1D.get(String.format("%s_ele_track_maxMCPmultiplicity",this.trackCollectionName)).fill(maxValue);
        }
        else{
            plots1D.get(String.format("%s_pos_track_maxMCPmultiplicity",this.trackCollectionName)).fill(maxValue);
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


