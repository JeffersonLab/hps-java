package org.hps.recon.utils;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.matrix.SymmetricMatrix;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
//import java.util.HashSet;
import java.io.IOException;
import java.util.Set;

import org.hps.recon.tracking.TrackTruthInfo;

import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRelationalTable;

import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;
import org.hps.recon.tracking.TrackUtils;
//import org.hps.recon.utils.TrackTruthMatcher;
//import org.hps.recon.utils.TrackTruthMatching;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;

import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.geometry.Detector;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;

import org.lcsim.lcio.LCIOConstants;

/**
 * This driver creates an MCParticle relation to be persisted for each track collection
 * It also saves a TruthTrack
 */
public class TrackTruthRelationsDriver extends Driver {

    //Write output to LCIO
    protected boolean writeToLCIO = false; 

    //Collection Names
    private String trackCollectionName = "";
    
    private double bfield;
    private double bfield_y;

    private boolean debug = false;
    private boolean saveTruthTracks = true;

    //Single MCP must leave n hits on Track to be matched to Track
    private int nGoodHitsRequired = 10;
    //Number of hits required on Track
    private int nHitsRequired = 10;

    //(bestMCP_Nhits/Total_Nhits) must be > purity cut    
    //Where "hit" is counted only once per layer
    private double purityCut = 0.8;

    //Define Plot tools
    private ITree tree;
    private IHistogramFactory histogramFactory;
    private Map<String, IHistogram1D> plots1D;
    private Map<String, IHistogram2D> plots2D;
    boolean enablePlots = false;    
    protected int NLAYERS = 14;

    public void setNLAYERS(int input){
        this.NLAYERS = input;
    }

    public void setWriteToLCIO(boolean input){
        this.writeToLCIO = input;
    }

    public void setPurityCut(double input){
        this.purityCut = input;
    }

    public void setNHitsRequired(int input){
        this.nHitsRequired = input;
    }

    public void setNGoodHitsRequired(int input){
        this.nGoodHitsRequired = input;
    }

    public void setEnablePlots(boolean input){
        this.enablePlots = input;
        if(enablePlots)
            this.bookHistograms();
    }

    public void setTrackCollectionName(String input) {
        this.trackCollectionName = input;
    }

    public void saveHistograms() {
        String rootFile = String.format("TrackToMCParticleRelations.root",this.trackCollectionName);
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Book Histograms
    public void bookHistograms(){
        plots1D = new HashMap<String, IHistogram1D>();
        plots2D = new HashMap<String, IHistogram2D>();
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

//Plots for all Truth Matched Tracks, regradless of purity

        //Reco Tracks
        //ele
        plots1D.put(String.format("ele_reco_track_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_reco_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_reco_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_reco_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_reco_track_d0"),
                histogramFactory.createHistogram1D(String.format("ele_reco_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_reco_track_z0"),
                histogramFactory.createHistogram1D(String.format("ele_reco_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_reco_track_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_reco_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_reco_track_C"),
                histogramFactory.createHistogram1D(String.format("ele_reco_track_C"), 2000, -0.1, 0.1));

        plots2D.put(String.format("ele_reco_track_momentum_v_nhits"),
                histogramFactory.createHistogram2D(String.format("ele_reco_track_momentum_v_nhits"), 800, 0, 8, 15, 0, 15));
        //pos
        plots1D.put(String.format("pos_reco_track_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_reco_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_reco_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_reco_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_reco_track_d0"),
                histogramFactory.createHistogram1D(String.format("pos_reco_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_reco_track_z0"),
                histogramFactory.createHistogram1D(String.format("pos_reco_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_reco_track_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_reco_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_reco_track_C"),
                histogramFactory.createHistogram1D(String.format("pos_reco_track_C"), 2000, -0.10, 0.10));

        plots2D.put(String.format("pos_reco_track_momentum_v_nhits"),
                histogramFactory.createHistogram2D(String.format("pos_reco_track_momentum_v_nhits"), 800, 0, 8, 15, 0, 15));

        //Truth Tracks
        
        //ele
        plots2D.put(String.format("ele_truth_track_v_matched_mcp_momentum"),
                histogramFactory.createHistogram2D(String.format("ele_truth_track_v_matched_mcp_momentum"), 1600, -8, 8, 1600, -8, 8));

        plots1D.put(String.format("ele_truth_track_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_truth_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_truth_track_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_truth_track_d0"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_truth_track_z0"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_truth_track_C"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_C"), 2000, -0.10, 0.10));

        plots1D.put(String.format("ele_truth_track_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_mcp_momentum"), 800, 0, 8));

        plots2D.put(String.format("ele_truth_tracks_trackP_v_nHits_on_track"),
                histogramFactory.createHistogram2D(String.format("ele_truth_tracks_trackP_v_nHits_on_track"), 800, 0, 800, 15, 0, 15));

        plots2D.put(String.format("ele_truth_track_momentum_v_probability"),
                histogramFactory.createHistogram2D(String.format("ele_truth_track_momentum_v_probability"), 800, 0, 8, 40, 0, 2));

        plots2D.put(String.format("ele_truth_track_tanlambda_v_probability"),
                histogramFactory.createHistogram2D(String.format("ele_truth_track_tanlambda_v_probability"), 1000, -1, 1, 40, 0, 2));

        plots2D.put(String.format("ele_truth_track_phi0_v_probability"),
                histogramFactory.createHistogram2D(String.format("ele_truth_track_phi0_v_probability"), 2000, -10, 10, 40, 0, 2));

        plots2D.put(String.format("ele_truth_track_z0_v_probability"),
                histogramFactory.createHistogram2D(String.format("ele_truth_track_z0_v_probability"), 2000, -10, 10, 40, 0, 2));

        plots2D.put(String.format("ele_truth_track_d0_v_probability"),
                histogramFactory.createHistogram2D(String.format("ele_truth_track_d0_v_probability"), 2000, -10, 10, 40, 0, 2));

        plots2D.put(String.format("ele_truth_track_C_v_probability"),
                histogramFactory.createHistogram2D(String.format("ele_truth_track_C_v_probability"), 2000, -0.10, 0.10, 40, 0, 2));

        plots1D.put(String.format("ele_multi_mcp_truth_prob"),
                histogramFactory.createHistogram1D(String.format("ele_multi_mcp_truth_prob"), 40, 0, 2));

        plots1D.put(String.format("ele_mcp_truth_prob"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_truth_prob"), 40, 0, 2));

        plots1D.put(String.format("ele_n_mcps_on_track"), histogramFactory.createHistogram1D(String.format("ele_n_mcps_on_track"), 30, 0, 30));

        plots1D.put(String.format("ele_n_hits"), histogramFactory.createHistogram1D(String.format("ele_n_hits"), 15, 0, 15));

        plots1D.put(String.format("ele_n_goodhits"), histogramFactory.createHistogram1D(String.format("ele_n_goodhits"), 15, 0, 15));

        plots1D.put(String.format("ele_n_badhits"), histogramFactory.createHistogram1D(String.format("ele_n_badhits"), 15, 0, 15));

        plots1D.put(String.format("ele_purity"), histogramFactory.createHistogram1D(String.format("ele_purity"), 40, 0, 2));

        plots2D.put(String.format("ele_purity_v_nMCPs_on_track"), histogramFactory.createHistogram2D(String.format("ele_purity_v_nMCPs_on_track"), 40, 0, 2, 20, 0, 20));

        plots1D.put(String.format("ele_layers_hit"), histogramFactory.createHistogram1D(String.format("ele_layers_hit"), 15, 0, 15));

        plots1D.put(String.format("ele_n_mcps_on_layer"), histogramFactory.createHistogram1D(String.format("ele_n_mcps_on_layer"), 20, 0, 20));

        plots1D.put(String.format("ele_n_striphits_on_layer"), 
                histogramFactory.createHistogram1D(String.format("ele_n_striphits_on_layer"), 20, 0, 20));

        plots1D.put(String.format("ele_n_mcps_on_striphit"), 
                histogramFactory.createHistogram1D(String.format("ele_n_mcps_on_striphit"), 10, 0, 10));

        plots2D.put(String.format("ele_n_mcps_per_layer"), 
                histogramFactory.createHistogram2D(String.format("ele_n_mcps_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("ele_n_striphits_per_layer"),
                histogramFactory.createHistogram2D(String.format("ele_n_striphits_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("ele_n_mcps_on_layer_striphits"),
                histogramFactory.createHistogram2D(String.format("ele_n_mcps_on_layer_striphits"), 20, 0, 20, 15, 0, 15));

        plots1D.put(String.format("ele_truth_track_good_hit_layers"), 
                histogramFactory.createHistogram1D(String.format("ele_truth_track_good_hit_layers"), 15, 0, 15));

        plots1D.put(String.format("ele_truth_track_bad_hit_layers"), 
                histogramFactory.createHistogram1D(String.format("ele_truth_track_good_bad_layers"), 15, 0, 15));


        //pos
        plots1D.put(String.format("pos_truth_track_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_truth_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_truth_track_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_truth_track_d0"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_truth_track_z0"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_truth_track_C"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_C"), 2000, -0.10, 0.10));

        plots2D.put(String.format("pos_truth_track_momentum_v_probability"),
                histogramFactory.createHistogram2D(String.format("pos_truth_track_momentum_v_probability"), 800, 0, 8, 40, 0, 2));

        plots2D.put(String.format("pos_truth_track_tanlambda_v_probability"),
                histogramFactory.createHistogram2D(String.format("pos_truth_track_tanlambda_v_probability"), 1000, -1, 1, 40, 0, 2));

        plots2D.put(String.format("pos_truth_track_phi0_v_probability"),
                histogramFactory.createHistogram2D(String.format("pos_truth_track_phi0_v_probability"), 2000, -10, 10, 40, 0, 2));

        plots2D.put(String.format("pos_truth_track_z0_v_probability"),
                histogramFactory.createHistogram2D(String.format("pos_truth_track_z0_v_probability"), 2000, -10, 10, 40, 0, 2));

        plots2D.put(String.format("pos_truth_track_d0_v_probability"),
                histogramFactory.createHistogram2D(String.format("pos_truth_track_d0_v_probability"), 2000, -10, 10, 40, 0, 2));

        plots2D.put(String.format("pos_truth_track_C_v_probability"),
                histogramFactory.createHistogram2D(String.format("pos_truth_track_C_v_probability"), 2000, -0.10, 0.10, 40, 0, 2));

        plots1D.put(String.format("pos_multi_mcp_truth_prob"),
                histogramFactory.createHistogram1D(String.format("pos_multi_mcp_truth_prob"), 40, 0, 2));

        plots1D.put(String.format("pos_mcp_truth_prob"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_truth_prob"), 40, 0, 2));

        plots1D.put(String.format("pos_n_mcps_on_track"), histogramFactory.createHistogram1D(String.format("pos_n_mcps_on_track"), 30, 0, 30));

        plots1D.put(String.format("pos_n_hits"), histogramFactory.createHistogram1D(String.format("pos_n_hits"), 15, 0, 15));

        plots1D.put(String.format("pos_n_goodhits"), histogramFactory.createHistogram1D(String.format("pos_n_goodhits"), 15, 0, 15));

        plots1D.put(String.format("pos_n_badhits"), histogramFactory.createHistogram1D(String.format("pos_n_badhits"), 15, 0, 15));

        plots1D.put(String.format("pos_purity"), histogramFactory.createHistogram1D(String.format("pos_purity"), 40, 0, 2));

        plots2D.put(String.format("pos_purity_v_nMCPs_on_track"), histogramFactory.createHistogram2D(String.format("pos_purity_v_nMCPs_on_track"), 40, 0, 2, 20, 0, 20));

        plots1D.put(String.format("pos_layers_hit"), histogramFactory.createHistogram1D(String.format("pos_layers_hit"), 15, 0, 15));

        plots1D.put(String.format("pos_n_mcps_on_layer"), histogramFactory.createHistogram1D(String.format("pos_n_mcps_on_layer"), 20, 0, 20));

        plots1D.put(String.format("pos_n_striphits_on_layer"), 
                histogramFactory.createHistogram1D(String.format("pos_n_striphits_on_layer"), 20, 0, 20));

        plots1D.put(String.format("pos_n_mcps_on_striphit"), 
                histogramFactory.createHistogram1D(String.format("pos_n_mcps_on_striphit"), 10, 0, 10));

        plots2D.put(String.format("pos_n_mcps_per_layer"), 
                histogramFactory.createHistogram2D(String.format("pos_n_mcps_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("pos_n_striphits_per_layer"),
                histogramFactory.createHistogram2D(String.format("pos_n_striphits_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("pos_n_mcps_on_layer_striphits"),
                histogramFactory.createHistogram2D(String.format("pos_n_mcps_on_layer_striphits"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("pos_truth_track_v_matched_mcp_momentum"),
                histogramFactory.createHistogram2D(String.format("pos_truth_track_v_matched_mcp_momentum"), 1600, -8, 8, 1600, -8, 8));


        plots1D.put(String.format("pos_truth_track_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_mcp_momentum"), 800, 0, 8));


        plots2D.put(String.format("pos_truth_tracks_trackP_v_nHits_on_track"),
                histogramFactory.createHistogram2D(String.format("pos_truth_tracks_trackP_v_nHits_on_track"), 800, 0, 800, 15, 0, 15));

        plots1D.put(String.format("pos_truth_track_good_hit_layers"), 
                histogramFactory.createHistogram1D(String.format("pos_truth_track_good_hit_layers"), 15, 0, 15));

        plots1D.put(String.format("pos_truth_track_bad_hit_layers"), 
                histogramFactory.createHistogram1D(String.format("pos_truth_track_bad_hit_layers"), 15, 0, 15));

//Purity = 1 Plots

        //ele
        plots1D.put(String.format("ele_truth_track_purity_eq_1_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_purity_eq_1_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_truth_track_purity_eq_1_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_purity_eq_1_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_truth_track_purity_eq_1_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_purity_eq_1_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_truth_track_purity_eq_1_d0"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_purity_eq_1_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_truth_track_purity_eq_1_z0"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_purity_eq_1_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_truth_track_purity_eq_1_C"),
                histogramFactory.createHistogram1D(String.format("ele_truth_track_purity_eq_1_C"), 2000, -10, 10));

        plots2D.put(String.format("ele_truth_track_purity_eq_1_v_matched_mcp_momentum"),
                histogramFactory.createHistogram2D(String.format("ele_truth_track_purity_eq_1_v_matched_mcp_momentum"), 1600, -8, 8, 1600, -8, 8));

        //pos
        plots1D.put(String.format("pos_truth_track_purity_eq_1_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_purity_eq_1_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_truth_track_purity_eq_1_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_purity_eq_1_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_truth_track_purity_eq_1_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_purity_eq_1_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_truth_track_purity_eq_1_d0"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_purity_eq_1_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_truth_track_purity_eq_1_z0"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_purity_eq_1_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_truth_track_purity_eq_1_C"),
                histogramFactory.createHistogram1D(String.format("pos_truth_track_purity_eq_1_C"), 2000, -10, 10));

        plots2D.put(String.format("pos_truth_track_purity_eq_1_v_matched_mcp_momentum"),
                histogramFactory.createHistogram2D(String.format("pos_truth_track_purity_eq_1_v_matched_mcp_momentum"), 1600, -8, 8, 1600, -8, 8));
        
//Plots for "Real" Tracks that pass purity cut

        plots1D.put(String.format("ele_real_track_n_mcps_on_track"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_n_mcps_on_track"), 30, 0, 30));

        plots1D.put(String.format("ele_real_track_n_hits"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_n_hits"), 15, 0, 15));

        plots1D.put(String.format("ele_real_track_n_goodhits"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_n_goodhits"), 15, 0, 15));

        plots1D.put(String.format("ele_real_track_n_badhits"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_n_badhits"), 15, 0, 15));

        plots1D.put(String.format("ele_real_track_purity"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_purity"), 40, 0, 2));

        plots1D.put(String.format("ele_real_track_layers_hit"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_layers_hit"), 15, 0, 15));

        plots1D.put(String.format("ele_real_track_n_mcps_on_layer"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_n_mcps_on_layer"), 20, 0, 20));

        plots1D.put(String.format("ele_real_track_n_striphits_on_layer"), 
                histogramFactory.createHistogram1D(String.format("ele_real_track_n_striphits_on_layer"), 20, 0, 20));

        plots1D.put(String.format("ele_real_track_n_mcps_on_striphit"), 
                histogramFactory.createHistogram1D(String.format("ele_real_track_n_mcps_on_striphit"), 10, 0, 10));

        plots2D.put(String.format("ele_real_track_n_mcps_per_layer"), 
                histogramFactory.createHistogram2D(String.format("ele_real_track_n_mcps_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("ele_real_track_n_striphits_per_layer"),
                histogramFactory.createHistogram2D(String.format("ele_real_track_n_striphits_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("ele_real_track_n_mcps_on_layer_striphits"),
                histogramFactory.createHistogram2D(String.format("ele_real_track_n_mcps_on_layer_striphits"), 20, 0, 20, 15, 0, 15));

        plots1D.put(String.format("ele_real_track_good_hit_layers"), 
                histogramFactory.createHistogram1D(String.format("ele_real_track_good_hit_layers"), 15, 0, 15));

        plots1D.put(String.format("ele_real_track_bad_hit_layers"), 
                histogramFactory.createHistogram1D(String.format("ele_real_track_bad_hit_layers"), 15, 0, 15));

        plots2D.put(String.format("ele_real_track_v_matched_mcp_momentum"),
                histogramFactory.createHistogram2D(String.format("ele_real_track_v_matched_mcp_momentum"), 1600, -8, 8, 1600, -8, 8));

        plots1D.put(String.format("ele_real_track_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_real_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_real_track_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_track_d0"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_track_z0"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_track_C"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_C"), 2000, -0.10, 0.10));

        plots1D.put(String.format("ele_real_track_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_real_track_mcp_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_mcp_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_real_track_mcp_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_mcp_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_track_mcp_d0"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_mcp_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_track_mcp_z0"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_mcp_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_track_mcp_C"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_mcp_C"), 2000, -0.10, 0.10));

        plots1D.put(String.format("ele_real_track_nhits_gteq_12_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_nhits_gteq_12_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_real_duplicate_tracks_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_real_duplicate_tracks_mcp_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_mcp_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_real_duplicate_tracks_mcp_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_mcp_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_duplicate_tracks_mcp_d0"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_mcp_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_duplicate_tracks_mcp_z0"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_mcp_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_duplicate_tracks_mcp_C"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_mcp_C"), 2000, -0.10, 0.10));


            //reco track
        plots1D.put(String.format("ele_real_track_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_real_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_real_track_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_track_d0"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_track_z0"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_track_C"),
                histogramFactory.createHistogram1D(String.format("ele_real_track_C"), 2000, -0.10, 0.10));

        plots1D.put(String.format("ele_real_duplicate_tracks_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_real_duplicate_tracks_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_real_duplicate_tracks_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_duplicate_tracks_d0"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_duplicate_tracks_z0"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_real_duplicate_tracks_C"),
                histogramFactory.createHistogram1D(String.format("ele_real_duplicate_tracks_C"), 2000, -10, 10));
        //pos

        plots1D.put(String.format("pos_real_track_n_mcps_on_track"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_n_mcps_on_track"), 30, 0, 30));

        plots1D.put(String.format("pos_real_track_n_hits"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_n_hits"), 15, 0, 15));

        plots1D.put(String.format("pos_real_track_n_goodhits"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_n_goodhits"), 15, 0, 15));

        plots1D.put(String.format("pos_real_track_n_badhits"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_n_badhits"), 15, 0, 15));

        plots1D.put(String.format("pos_real_track_purity"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_purity"), 40, 0, 2));

        plots1D.put(String.format("pos_real_track_layers_hit"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_layers_hit"), 15, 0, 15));

        plots1D.put(String.format("pos_real_track_n_mcps_on_layer"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_n_mcps_on_layer"), 20, 0, 20));

        plots1D.put(String.format("pos_real_track_n_striphits_on_layer"), 
                histogramFactory.createHistogram1D(String.format("pos_real_track_n_striphits_on_layer"), 20, 0, 20));

        plots1D.put(String.format("pos_real_track_n_mcps_on_striphit"), 
                histogramFactory.createHistogram1D(String.format("pos_real_track_n_mcps_on_striphit"), 10, 0, 10));

        plots2D.put(String.format("pos_real_track_n_mcps_per_layer"), 
                histogramFactory.createHistogram2D(String.format("pos_real_track_n_mcps_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("pos_real_track_n_striphits_per_layer"),
                histogramFactory.createHistogram2D(String.format("pos_real_track_n_striphits_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("pos_real_track_n_mcps_on_layer_striphits"),
                histogramFactory.createHistogram2D(String.format("pos_real_track_n_mcps_on_layer_striphits"), 20, 0, 20, 15, 0, 15));

        plots1D.put(String.format("pos_real_track_good_hit_layers"), 
                histogramFactory.createHistogram1D(String.format("pos_real_track_good_hit_layers"), 15, 0, 15));

        plots1D.put(String.format("pos_real_track_bad_hit_layers"), 
                histogramFactory.createHistogram1D(String.format("pos_real_track_bad_hit_layers"), 15, 0, 15));

        plots2D.put(String.format("pos_real_track_v_matched_mcp_momentum"),
                histogramFactory.createHistogram2D(String.format("pos_real_track_v_matched_mcp_momentum"), 1600, -8, 8, 1600, -8, 8));

        plots1D.put(String.format("pos_real_track_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_real_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_real_track_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_track_d0"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_track_z0"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_track_C"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_C"), 2000, -0.10, 0.10));

        plots1D.put(String.format("pos_real_track_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_real_track_mcp_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_mcp_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_real_track_mcp_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_mcp_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_track_mcp_d0"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_mcp_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_track_mcp_z0"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_mcp_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_track_mcp_C"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_mcp_C"), 2000, -0.10, 0.10));

        plots1D.put(String.format("pos_real_track_nhits_gteq_12_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_nhits_gteq_12_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_real_duplicate_tracks_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_real_duplicate_tracks_mcp_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_mcp_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_real_duplicate_tracks_mcp_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_mcp_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_duplicate_tracks_mcp_d0"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_mcp_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_duplicate_tracks_mcp_z0"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_mcp_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_duplicate_tracks_mcp_C"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_mcp_C"), 2000, -0.10, 0.10));

            //reco track
        plots1D.put(String.format("pos_real_track_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_real_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_real_track_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_track_d0"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_track_z0"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_track_C"),
                histogramFactory.createHistogram1D(String.format("pos_real_track_C"), 2000, -0.10, 0.10));

        plots1D.put(String.format("pos_real_duplicate_tracks_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_real_duplicate_tracks_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_real_duplicate_tracks_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_duplicate_tracks_d0"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_duplicate_tracks_z0"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_real_duplicate_tracks_C"),
                histogramFactory.createHistogram1D(String.format("pos_real_duplicate_tracks_C"), 2000, -10, 10));

//Plots for "Fake" Tracks that fail purity cut

        //ele
        plots1D.put(String.format("ele_fake_track_n_mcps_on_track"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_n_mcps_on_track"), 30, 0, 30));

        plots1D.put(String.format("ele_fake_track_n_hits"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_n_hits"), 15, 0, 15));

        plots1D.put(String.format("ele_fake_track_n_goodhits"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_n_goodhits"), 15, 0, 15));

        plots1D.put(String.format("ele_fake_track_n_badhits"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_n_badhits"), 15, 0, 15));

        plots1D.put(String.format("ele_fake_track_purity"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_purity"), 40, 0, 2));

        plots1D.put(String.format("ele_fake_track_layers_hit"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_layers_hit"), 15, 0, 15));

        plots1D.put(String.format("ele_fake_track_n_mcps_on_layer"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_n_mcps_on_layer"), 20, 0, 20));

        plots1D.put(String.format("ele_fake_track_n_striphits_on_layer"), 
                histogramFactory.createHistogram1D(String.format("ele_fake_track_n_striphits_on_layer"), 20, 0, 20));

        plots1D.put(String.format("ele_fake_track_n_mcps_on_striphit"), 
                histogramFactory.createHistogram1D(String.format("ele_fake_track_n_mcps_on_striphit"), 10, 0, 10));

        plots2D.put(String.format("ele_fake_track_n_mcps_per_layer"), 
                histogramFactory.createHistogram2D(String.format("ele_fake_track_n_mcps_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("ele_fake_track_n_striphits_per_layer"),
                histogramFactory.createHistogram2D(String.format("ele_fake_track_n_striphits_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("ele_fake_track_n_mcps_on_layer_striphits"),
                histogramFactory.createHistogram2D(String.format("ele_fake_track_n_mcps_on_layer_striphits"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("ele_fake_track_v_matched_mcp_momentum"),
                histogramFactory.createHistogram2D(String.format("ele_fake_track_v_matched_mcp_momentum"), 1600, -8, 8, 1600, -8, 8));

        plots1D.put(String.format("ele_fake_track_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_fake_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_fake_track_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_fake_track_d0"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_fake_track_z0"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_fake_track_C"),
                histogramFactory.createHistogram1D(String.format("ele_fake_track_C"), 2000, -0.10, 0.10));

        //plots1D.put(String.format("ele_fake_duplicate_tracks_mcp_momentum"),
        //        histogramFactory.createHistogram1D(String.format("ele_fake_duplicate_tracks_mcp_momentum"), 800, 0, 8));
        //pos
        plots1D.put(String.format("pos_fake_track_n_mcps_on_track"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_n_mcps_on_track"), 30, 0, 30));

        plots1D.put(String.format("pos_fake_track_n_hits"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_n_hits"), 15, 0, 15));

        plots1D.put(String.format("pos_fake_track_n_goodhits"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_n_goodhits"), 15, 0, 15));

        plots1D.put(String.format("pos_fake_track_n_badhits"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_n_badhits"), 15, 0, 15));

        plots1D.put(String.format("pos_fake_track_purity"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_purity"), 40, 0, 2));

        plots1D.put(String.format("pos_fake_track_layers_hit"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_layers_hit"), 15, 0, 15));

        plots1D.put(String.format("pos_fake_track_n_mcps_on_layer"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_n_mcps_on_layer"), 20, 0, 20));

        plots1D.put(String.format("pos_fake_track_n_striphits_on_layer"), 
                histogramFactory.createHistogram1D(String.format("pos_fake_track_n_striphits_on_layer"), 20, 0, 20));

        plots1D.put(String.format("pos_fake_track_n_mcps_on_striphit"), 
                histogramFactory.createHistogram1D(String.format("pos_fake_track_n_mcps_on_striphit"), 10, 0, 10));

        plots2D.put(String.format("pos_fake_track_n_mcps_per_layer"), 
                histogramFactory.createHistogram2D(String.format("pos_fake_track_n_mcps_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("pos_fake_track_n_striphits_per_layer"),
                histogramFactory.createHistogram2D(String.format("pos_fake_track_n_striphits_per_layer"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("pos_fake_track_n_mcps_on_layer_striphits"),
                histogramFactory.createHistogram2D(String.format("pos_fake_track_n_mcps_on_layer_striphits"), 20, 0, 20, 15, 0, 15));

        plots2D.put(String.format("pos_fake_track_v_matched_mcp_momentum"),
                histogramFactory.createHistogram2D(String.format("pos_fake_track_v_matched_mcp_momentum"), 1600, -8, 8, 1600, -8, 8));

        plots1D.put(String.format("pos_fake_track_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_fake_track_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_fake_track_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_fake_track_d0"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_fake_track_z0"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_fake_track_C"),
                histogramFactory.createHistogram1D(String.format("pos_fake_track_C"), 2000, -0.10, 0.10));

        //plots1D.put(String.format("pos_fake_duplicate_tracks_mcp_momentum"),
        //        histogramFactory.createHistogram1D(String.format("pos_fake_duplicate_tracks_mcp_momentum"), 800, 0, 8));

//
        plots2D.put(String.format("old_truth_track_v_matched_mcp_momentum"),
                histogramFactory.createHistogram2D(String.format("old_truth_track_v_matched_mcp_momentum"), 1600, -8, 8, 1600, -8, 8));

        //MCParticle plots

        //DEBUG PLOTS
        
        plots1D.put(String.format("mcp_helicalTrackFit_phi"),
                histogramFactory.createHistogram1D(String.format("mcp_helicalTrackFit_phi"), 2000, -10, 10));
        //
        
        //ele
        plots1D.put(String.format("ele_mcp_px"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_px"), 1600, -8, 8));

        plots1D.put(String.format("ele_mcp_py"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_py"), 1600, -8, 8));

        plots1D.put(String.format("ele_mcp_pz"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_pz"), 1600, -8, 8));

        plots2D.put(String.format("ele_mcp_px_v_py"),
                histogramFactory.createHistogram2D(String.format("ele_mcp_px_v_py"), 1600, -8, 8, 1600, -8, 8));

        plots2D.put(String.format("ele_mcp_px_v_pz"),
                histogramFactory.createHistogram2D(String.format("ele_mcp_px_v_pz"), 1600, -8, 8, 1600, -8, 8));

        plots2D.put(String.format("ele_mcp_py_v_pz"),
                histogramFactory.createHistogram2D(String.format("ele_mcp_py_v_pz"), 1600, -8, 8, 1600, -8, 8));

        plots1D.put(String.format("ele_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_mcp_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_mcp_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_mcp_d0"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_mcp_z0"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_mcp_C"),
                histogramFactory.createHistogram1D(String.format("ele_mcp_C"), 2000, -0.10, 0.10));

        plots2D.put(String.format("ele_mcp_momentum_v_tanlambda"),
                histogramFactory.createHistogram2D(String.format("ele_mcp_momentum_v_tanlambda"), 800, 0, 8, 1000, -1, 1));

        plots2D.put(String.format("ele_mcp_momentum_v_phi0"),
                histogramFactory.createHistogram2D(String.format("ele_mcp_momentum_v_phi0"), 800, 0, 8, 2000, -10, 10));

        plots2D.put(String.format("ele_mcp_momentum_v_d0"),
                histogramFactory.createHistogram2D(String.format("ele_mcp_momentum_v_d0"), 800, 0, 8, 2000, -10, 10));

        plots2D.put(String.format("ele_mcp_momentum_v_z0"),
                histogramFactory.createHistogram2D(String.format("ele_mcp_momentum_v_z0"), 800, 0, 8, 2000, -10, 10));

        plots2D.put(String.format("ele_mcp_momentum_v_C"),
                histogramFactory.createHistogram2D(String.format("ele_mcp_momentum_v_C"), 800, 0, 8, 2000, -10, 10));
        //pos
        plots1D.put(String.format("pos_mcp_px"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_px"), 1600, -8, 8));

        plots1D.put(String.format("pos_mcp_py"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_py"), 1600, -8, 8));

        plots1D.put(String.format("pos_mcp_pz"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_pz"), 1600, -8, 8));

        plots2D.put(String.format("pos_mcp_px_v_py"),
                histogramFactory.createHistogram2D(String.format("pos_mcp_px_v_py"), 1600, -8, 8, 1600, -8, 8));

        plots2D.put(String.format("pos_mcp_px_v_pz"),
                histogramFactory.createHistogram2D(String.format("pos_mcp_px_v_pz"), 1600, -8, 8, 1600, -8, 8));

        plots2D.put(String.format("pos_mcp_py_v_pz"),
                histogramFactory.createHistogram2D(String.format("pos_mcp_py_v_pz"), 1600, -8, 8, 1600, -8, 8));

        plots1D.put(String.format("pos_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_mcp_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_mcp_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_mcp_d0"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_mcp_z0"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_mcp_C"),
                histogramFactory.createHistogram1D(String.format("pos_mcp_C"), 2000, -0.10, 0.10));

        plots2D.put(String.format("pos_mcp_momentum_v_tanlambda"),
                histogramFactory.createHistogram2D(String.format("pos_mcp_momentum_v_tanlambda"), 800, 0, 8, 1000, -1, 1));

        plots2D.put(String.format("pos_mcp_momentum_v_phi0"),
                histogramFactory.createHistogram2D(String.format("pos_mcp_momentum_v_phi0"), 800, 0, 8, 2000, -10, 10));

        plots2D.put(String.format("pos_mcp_momentum_v_d0"),
                histogramFactory.createHistogram2D(String.format("pos_mcp_momentum_v_d0"), 800, 0, 8, 2000, -10, 10));

        plots2D.put(String.format("pos_mcp_momentum_v_z0"),
                histogramFactory.createHistogram2D(String.format("pos_mcp_momentum_v_z0"), 800, 0, 8, 2000, -10, 10));

        plots2D.put(String.format("pos_mcp_momentum_v_C"),
                histogramFactory.createHistogram2D(String.format("pos_mcp_momentum_v_C"), 800, 0, 8, 2000, -10, 10));

        //Trackable MCP Plots
        plots1D.put(String.format("mcp_tanlambda"), histogramFactory.createHistogram1D(String.format("mcp_tanlambda"), 1000, -1, 1));

        plots2D.put(String.format("mcp_momentum_v_tanlambda"),
                histogramFactory.createHistogram2D(String.format("mcp_momentum_v_tanlambda"), 800, 0, 8, 1000, -1, 1));
        
        //ele
        plots1D.put(String.format("ele_trackable_mcp_nSimTrackerHits"),
                histogramFactory.createHistogram1D(String.format("ele_trackable_mcp_nSimTrackerHits"), 15, 0, 15));
        
        plots2D.put(String.format("ele_trackable_mcp_momentum_v_nSimTrackerHits"),
                histogramFactory.createHistogram2D(String.format("ele_trackable_mcp_momentum_v_nSimTrackerHits"), 800, 0, 8, 15, 0, 15));

        plots2D.put(String.format("ele_mcp_momentum_v_nLayersHit"),
                histogramFactory.createHistogram2D(String.format("ele_mcp_momentum_v_nLayersHit"), 800, 0, 8, 15, 0, 15));

        plots1D.put(String.format("ele_trackable_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("ele_trackable_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("ele_trackable_mcp_tanlambda"),
                histogramFactory.createHistogram1D(String.format("ele_trackable_mcp_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("ele_trackable_mcp_phi0"),
                histogramFactory.createHistogram1D(String.format("ele_trackable_mcp_phi0"), 2000, -10, 10));

        plots1D.put(String.format("ele_trackable_mcp_d0"),
                histogramFactory.createHistogram1D(String.format("ele_trackable_mcp_d0"), 2000, -10, 10));

        plots1D.put(String.format("ele_trackable_mcp_z0"),
                histogramFactory.createHistogram1D(String.format("ele_trackable_mcp_z0"), 2000, -10, 10));

        plots1D.put(String.format("ele_trackable_mcp_C"),
                histogramFactory.createHistogram1D(String.format("ele_trackable_mcp_C"), 2000, -0.10, 0.10));
        //pos
        plots2D.put(String.format("pos_mcp_momentum_v_nLayersHit"),
                histogramFactory.createHistogram2D(String.format("pos_mcp_momentum_v_nLayersHit"), 800, 0, 8, 15, 0, 15));

        plots1D.put(String.format("pos_trackable_mcp_nSimTrackerHits"),
                histogramFactory.createHistogram1D(String.format("pos_trackable_mcp_nSimTrackerHits"), 15, 0, 15));
        
        plots2D.put(String.format("pos_trackable_mcp_momentum_v_nSimTrackerHits"),
                histogramFactory.createHistogram2D(String.format("pos_trackable_mcp_momentum_v_nSimTrackerHits"), 800, 0, 8, 15, 0, 15));

        plots1D.put(String.format("pos_trackable_mcp_momentum"),
                histogramFactory.createHistogram1D(String.format("pos_trackable_mcp_momentum"), 800, 0, 8));

        plots1D.put(String.format("pos_trackable_mcp_tanlambda"),
                histogramFactory.createHistogram1D(String.format("pos_trackable_mcp_tanlambda"), 1000, -1, 1));

        plots1D.put(String.format("pos_trackable_mcp_phi0"),
                histogramFactory.createHistogram1D(String.format("pos_trackable_mcp_phi0"), 2000, -10, 10));

        plots1D.put(String.format("pos_trackable_mcp_d0"),
                histogramFactory.createHistogram1D(String.format("pos_trackable_mcp_d0"), 2000, -10, 10));

        plots1D.put(String.format("pos_trackable_mcp_z0"),
                histogramFactory.createHistogram1D(String.format("pos_trackable_mcp_z0"), 2000, -10, 10));

        plots1D.put(String.format("pos_trackable_mcp_C"),
                histogramFactory.createHistogram1D(String.format("pos_trackable_mcp_C"), 2000, -0.10, 0.10));
    }

    public void endOfData() {
        if(enablePlots)
            saveHistograms();
    }


    public void setDebug(boolean val) {
        debug = val;
    }
    
    public void setSaveTruthTracks(boolean val) {
        saveTruthTracks = val;
    }
    
    /*
      public void setTrackCollectionNames(String [] trackCollectionNames) {
      this.trackCollectionNames = trackCollectionNames;
      }
    */
    
    @Override
    protected void detectorChanged(Detector detector) {
        Hep3Vector fieldInTracker = TrackUtils.getBField(detector);
        bfield = Math.abs(fieldInTracker.y());
        bfield_y = fieldInTracker.y();
    }
    
    @Override 
    protected void process(EventHeader event) {
        
        //Retrieve track collection
        List<Track> trackCollection = new ArrayList<Track>();
        if (trackCollectionName != null) {
            if (event.hasCollection(Track.class, trackCollectionName)) {
                trackCollection = event.get(Track.class, trackCollectionName);
            }
        }
        
        //Retrieve rawhits to mc
        
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
        }
        else
            return;

        //Retrieve all simulated hits
        String MCHitInputCollectionName = "TrackerHits";
        List<SimTrackerHit> allsimhits = event.get(SimTrackerHit.class, MCHitInputCollectionName);

        //MCParticleRelations
        List<LCRelation> trackToMCParticleRelations    =  new ArrayList<LCRelation>();

        //Truth Tracks and Relations
        List<LCRelation> trackToTruthTrackRelations    =  new ArrayList<LCRelation>();
        List<Track>      truthTrackCollection          =  new ArrayList<Track>();
        List<TrackTruthInfo> trackTruthInfoCollection = new ArrayList<TrackTruthInfo>();
        List<LCRelation> trackToTruthInfoRelations = new ArrayList<LCRelation>();




        //Retrieve all MCPs
        List<MCParticle> allmcps = event.get(MCParticle.class, "MCParticle");
        //Check for Trackable MCPs
        Map<MCParticle, Map<Integer, List<SimTrackerHit>>> trackableMCPMap = getTrackableMCPs(allmcps, allsimhits, rawtomc,this.nGoodHitsRequired);

        List<MCParticle> mcps = new ArrayList<MCParticle>();
        for (Track track : trackCollection) {
            System.out.println("Number of trackstates = " + track.getTrackStates().size());
            for(TrackState trackstate : track.getTrackStates()){
                System.out.println("location = " + trackstate.getLocation());
            }

            boolean realTrack = true;
            int charge = -1* (int)Math.signum(track.getTrackStates().get(0).getOmega());
            double trackPmag = new BasicHep3Vector(track.getTrackStates().get(0).getMomentum()).magnitude();
            double tanlambda = track.getTrackStates().get(0).getTanLambda();
            double track_d0    = track.getTrackStates().get(0).getD0();
            double track_z0    = track.getTrackStates().get(0).getZ0();
            double track_C     = track.getTrackStates().get(0).getOmega();
            double track_phi   = track.getTrackStates().get(0).getPhi();

            /*
            //Check match quality of old Track->MCP matching
            TrackTruthMatching ttm = new TrackTruthMatching(track, rawtomc, allsimhits, false);
            MCParticle mcp_matt = null;
            if(ttm != null)
                mcp_matt = ttm.getMCParticle();
            if(mcp_matt != null){
                plots2D.get("old_truth_track_v_matched_mcp_momentum").fill(charge*trackPmag,((int)mcp_matt.getCharge())*mcp_matt.getMomentum().magnitude());
            }
            */

            //Use New TrackTruthMatching tool to match Track -> MCP
            TrackTruthMatcher tt = new TrackTruthMatcher(track, rawtomc, 0.0, 0);

            //Check nHits on Track
            //If nHits < required, dont analyze this Track
            double nHits = tt.getNHits();
            if(nHits < this.nHitsRequired)
                continue;

            //Fill plot with all Reco Tracks momentum
            if(charge < 0){
                plots1D.get("ele_reco_track_momentum").fill(trackPmag);
                plots1D.get("ele_reco_track_tanlambda").fill(tanlambda);
                plots1D.get("ele_reco_track_d0").fill(track_d0);
                plots1D.get("ele_reco_track_z0").fill(track_z0);
                plots1D.get("ele_reco_track_C").fill(track_C);
                plots1D.get("ele_reco_track_phi0").fill(track_phi);
                plots2D.get("ele_reco_track_momentum_v_nhits").fill(trackPmag, nHits);
            }
            else{
                plots1D.get("pos_reco_track_momentum").fill(trackPmag);
                plots1D.get("pos_reco_track_tanlambda").fill(tanlambda);
                plots1D.get("pos_reco_track_d0").fill(track_d0);
                plots1D.get("pos_reco_track_z0").fill(track_z0);
                plots1D.get("pos_reco_track_C").fill(track_C);
                plots1D.get("pos_reco_track_phi0").fill(track_phi);
                plots2D.get("pos_reco_track_momentum_v_nhits").fill(trackPmag, nHits);
            }

            //get Track MCP if it has one
            MCParticle mcp = tt.getMCParticle();

            //If Track is matched to a MCP, continue analysis
            if(mcp != null){

                //Fit MCP to helical track to get params
                double[] mcpParams = getMCPTrackParameters(mcp, bfield);
                double mcp_d0 = mcpParams[0];
                double mcp_z0 = mcpParams[1];
                double mcp_C = mcpParams[2];
                double mcp_phi = mcpParams[3];
                double mcp_slope = mcpParams[4];

                //Get purity and nGoodHits to check Track quality
                double purity = tt.getPurity();
                double nGoodHits = tt.getNGoodHits();


                //First fill plots for all cases where a Track is matched to a
                //MCP, regardless of match purity.
                if(charge < 0){
                    plots2D.get("ele_truth_tracks_trackP_v_nHits_on_track").fill(trackPmag, nHits);
                    plots1D.get("ele_truth_track_momentum").fill(trackPmag);
                    plots1D.get("ele_truth_track_tanlambda").fill(tanlambda);
                    plots1D.get("ele_truth_track_phi0").fill(track_phi);
                    plots1D.get("ele_truth_track_d0").fill(track_d0);
                    plots1D.get("ele_truth_track_z0").fill(track_z0);
                    plots1D.get("ele_truth_track_C").fill(track_C);
                    plots2D.get("ele_truth_track_v_matched_mcp_momentum").fill(charge*trackPmag,((int)mcp.getCharge())*mcp.getMomentum().magnitude());
                    //truth probability for tracks
                    plots2D.get("ele_truth_track_momentum_v_probability").fill(trackPmag, tt.getPurity());
                    plots2D.get("ele_truth_track_tanlambda_v_probability").fill(tanlambda, tt.getPurity());
                    plots2D.get("ele_truth_track_d0_v_probability").fill(track_d0, tt.getPurity());
                    plots2D.get("ele_truth_track_z0_v_probability").fill(track_z0, tt.getPurity());
                    plots2D.get("ele_truth_track_phi0_v_probability").fill(track_phi, tt.getPurity());
                    plots2D.get("ele_truth_track_C_v_probability").fill(track_C, tt.getPurity());

                    if(tt.getLayerHitsForAllMCPs().size() > 1){
                        plots1D.get("ele_multi_mcp_truth_prob").fill(tt.getPurity());
                    }
                    if(tt.getPurity() > 0.5){
                        plots1D.get("ele_mcp_truth_prob").fill(tt.getPurity());
                    }
                    plots2D.get("ele_purity_v_nMCPs_on_track").fill(tt.getPurity(), tt.getLayerHitsForAllMCPs().size());
                    plots1D.get("ele_n_mcps_on_track").fill(tt.getLayerHitsForAllMCPs().size());
                    plots1D.get("ele_n_hits").fill(tt.getNHits());
                    plots1D.get("ele_purity").fill(tt.getPurity());
                    plots1D.get("ele_n_goodhits").fill(tt.getNGoodHits());
                    plots1D.get("ele_n_badhits").fill(tt.getNBadHits());
                    for(Integer layer : tt.getLayersOnTrack()){
                        plots1D.get("ele_layers_hit").fill(layer);
                        plots1D.get("ele_n_mcps_on_layer").fill(tt.getMCPsOnLayer(layer).size());
                        plots2D.get("ele_n_mcps_per_layer").fill(layer,tt.getMCPsOnLayer(layer).size());
                        plots1D.get("ele_n_striphits_on_layer").fill(tt.getStripHitsOnLayer(layer).size());
                        plots2D.get("ele_n_striphits_per_layer").fill(layer,tt.getStripHitsOnLayer(layer).size());
                        for(RawTrackerHit rawhit : tt.getStripHitsOnLayer(layer)){
                            plots1D.get("ele_n_mcps_on_striphit").fill(tt.getMCPsOnRawTrackerHit(rawhit).size());
                            plots2D.get("ele_n_mcps_on_layer_striphits").fill(layer,tt.getMCPsOnRawTrackerHit(rawhit).size());
                        }
                    }
                    //plot good and bad hit layers
                    for(Integer layer : tt.getGoodHitLayers()){
                        plots1D.get("ele_truth_track_good_hit_layers").fill(layer);
                    }
                    for(Integer layer : tt.getBadHitLayers()){
                        plots1D.get("ele_truth_track_bad_hit_layers").fill(layer);
                    }
                }

                else{
                    plots2D.get("pos_purity_v_nMCPs_on_track").fill(tt.getPurity(), tt.getLayerHitsForAllMCPs().size());
                    plots2D.get("pos_truth_tracks_trackP_v_nHits_on_track").fill(trackPmag, nHits);
                    plots1D.get("pos_truth_track_momentum").fill(trackPmag);
                    plots1D.get("pos_truth_track_tanlambda").fill(tanlambda);
                    plots1D.get("pos_truth_track_phi0").fill(track_phi);
                    plots1D.get("pos_truth_track_d0").fill(track_d0);
                    plots1D.get("pos_truth_track_z0").fill(track_z0);
                    plots1D.get("pos_truth_track_C").fill(track_C);
                    plots2D.get("pos_truth_track_v_matched_mcp_momentum").fill(charge*trackPmag,((int)mcp.getCharge())*mcp.getMomentum().magnitude());
                    //Truth Tracks Probability
                    plots2D.get("pos_truth_track_momentum_v_probability").fill(trackPmag, tt.getPurity());
                    plots2D.get("pos_truth_track_tanlambda_v_probability").fill(tanlambda, tt.getPurity());
                    plots2D.get("pos_truth_track_d0_v_probability").fill(track_d0, tt.getPurity());
                    plots2D.get("pos_truth_track_z0_v_probability").fill(track_z0, tt.getPurity());
                    plots2D.get("pos_truth_track_phi0_v_probability").fill(track_phi, tt.getPurity());
                    plots2D.get("pos_truth_track_C_v_probability").fill(track_C, tt.getPurity());
                    if(tt.getLayerHitsForAllMCPs().size() > 1){
                        plots1D.get("pos_multi_mcp_truth_prob").fill(tt.getPurity());
                    }
                    if(tt.getPurity() > 0.5){
                        plots1D.get("pos_mcp_truth_prob").fill(tt.getPurity());
                    }
                    plots1D.get("pos_n_mcps_on_track").fill(tt.getLayerHitsForAllMCPs().size());
                    plots1D.get("pos_n_hits").fill(tt.getNHits());
                    plots1D.get("pos_purity").fill(tt.getPurity());
                    plots1D.get("pos_n_goodhits").fill(tt.getNGoodHits());
                    plots1D.get("pos_n_badhits").fill(tt.getNBadHits());
                    for(Integer layer : tt.getLayersOnTrack()){
                        plots1D.get("pos_layers_hit").fill(layer);
                        plots1D.get("pos_n_mcps_on_layer").fill(tt.getMCPsOnLayer(layer).size());
                        plots2D.get("pos_n_mcps_per_layer").fill(layer,tt.getMCPsOnLayer(layer).size());
                        plots1D.get("pos_n_striphits_on_layer").fill(tt.getStripHitsOnLayer(layer).size());
                        plots2D.get("pos_n_striphits_per_layer").fill(layer,tt.getStripHitsOnLayer(layer).size());
                        for(RawTrackerHit rawhit : tt.getStripHitsOnLayer(layer)){
                            plots1D.get("pos_n_mcps_on_striphit").fill(tt.getMCPsOnRawTrackerHit(rawhit).size());
                            plots2D.get("pos_n_mcps_on_layer_striphits").fill(layer,tt.getMCPsOnRawTrackerHit(rawhit).size());
                        }
                    }

                    //plot good and bad hit layers
                    for(Integer layer : tt.getGoodHitLayers()){
                        plots1D.get("pos_truth_track_good_hit_layers").fill(layer);
                    }
                    for(Integer layer : tt.getBadHitLayers()){
                        plots1D.get("pos_truth_track_bad_hit_layers").fill(layer);
                    }
                }

                //Plots for Track->MCP purity = 100%
                if(purity == 1){

                    if(charge < 0){
                        plots1D.get("ele_truth_track_purity_eq_1_momentum").fill(trackPmag);
                        plots1D.get("ele_truth_track_purity_eq_1_tanlambda").fill(tanlambda);
                        plots1D.get("ele_truth_track_purity_eq_1_phi0").fill(track_phi);
                        plots1D.get("ele_truth_track_purity_eq_1_d0").fill(track_d0);
                        plots1D.get("ele_truth_track_purity_eq_1_z0").fill(track_z0);
                        plots1D.get("ele_truth_track_purity_eq_1_C").fill(track_C);
                        plots2D.get("ele_truth_track_purity_eq_1_v_matched_mcp_momentum").fill(charge*trackPmag,((int)mcp.getCharge())*mcp.getMomentum().magnitude());
                    }

                    else{
                        plots1D.get("pos_truth_track_purity_eq_1_momentum").fill(trackPmag);
                        plots1D.get("pos_truth_track_purity_eq_1_tanlambda").fill(tanlambda);
                        plots1D.get("pos_truth_track_purity_eq_1_phi0").fill(track_phi);
                        plots1D.get("pos_truth_track_purity_eq_1_d0").fill(track_d0);
                        plots1D.get("pos_truth_track_purity_eq_1_z0").fill(track_z0);
                        plots1D.get("pos_truth_track_purity_eq_1_C").fill(track_C);
                        plots2D.get("pos_truth_track_purity_eq_1_v_matched_mcp_momentum").fill(charge*trackPmag,((int)mcp.getCharge())*mcp.getMomentum().magnitude());
                    }

                }


                //If Track passes purity cut, identify Track as real
                if(purity < this.purityCut)
                    realTrack = false;

                //If Track fails purity cut, identify Track as fake
                if(!realTrack){
                    if(charge < 0){
                        plots1D.get("ele_fake_track_momentum").fill(trackPmag);
                        plots1D.get("ele_fake_track_tanlambda").fill(tanlambda);
                        plots1D.get("ele_fake_track_phi0").fill(track_phi);
                        plots1D.get("ele_fake_track_d0").fill(track_d0);
                        plots1D.get("ele_fake_track_z0").fill(track_z0);
                        plots1D.get("ele_fake_track_C").fill(track_C);

                        plots2D.get("ele_fake_track_v_matched_mcp_momentum").fill(charge*trackPmag,((int)mcp.getCharge())*mcp.getMomentum().magnitude());
                        plots1D.get("ele_fake_track_n_mcps_on_track").fill(tt.getLayerHitsForAllMCPs().size());
                        plots1D.get("ele_fake_track_n_hits").fill(tt.getNHits());
                        plots1D.get("ele_fake_track_purity").fill(tt.getPurity());
                        plots1D.get("ele_fake_track_n_goodhits").fill(tt.getNGoodHits());
                        plots1D.get("ele_fake_track_n_badhits").fill(tt.getNBadHits());
                        for(Integer layer : tt.getLayersOnTrack()){
                            plots1D.get("ele_fake_track_layers_hit").fill(layer);
                            plots1D.get("ele_fake_track_n_mcps_on_layer").fill(tt.getMCPsOnLayer(layer).size());
                            plots2D.get("ele_fake_track_n_mcps_per_layer").fill(layer,tt.getMCPsOnLayer(layer).size());
                            plots1D.get("ele_fake_track_n_striphits_on_layer").fill(tt.getStripHitsOnLayer(layer).size());
                            plots2D.get("ele_fake_track_n_striphits_per_layer").fill(layer,tt.getStripHitsOnLayer(layer).size());
                            for(RawTrackerHit rawhit : tt.getStripHitsOnLayer(layer)){
                                plots1D.get("ele_fake_track_n_mcps_on_striphit").fill(tt.getMCPsOnRawTrackerHit(rawhit).size());
                                plots2D.get("ele_fake_track_n_mcps_on_layer_striphits").fill(layer,tt.getMCPsOnRawTrackerHit(rawhit).size());
                            }
                        }

                    }

                    else{
                        plots1D.get("pos_fake_track_momentum").fill(trackPmag);
                        plots1D.get("pos_fake_track_tanlambda").fill(tanlambda);
                        plots1D.get("pos_fake_track_phi0").fill(track_phi);
                        plots1D.get("pos_fake_track_d0").fill(track_d0);
                        plots1D.get("pos_fake_track_z0").fill(track_z0);
                        plots1D.get("pos_fake_track_C").fill(track_C);

                        plots2D.get("pos_fake_track_v_matched_mcp_momentum").fill(charge*trackPmag,((int)mcp.getCharge())*mcp.getMomentum().magnitude());
                        plots1D.get("pos_fake_track_n_mcps_on_track").fill(tt.getLayerHitsForAllMCPs().size());
                        plots1D.get("pos_fake_track_n_hits").fill(tt.getNHits());
                        plots1D.get("pos_fake_track_purity").fill(tt.getPurity());
                        plots1D.get("pos_fake_track_n_goodhits").fill(tt.getNGoodHits());
                        plots1D.get("pos_fake_track_n_badhits").fill(tt.getNBadHits());
                        for(Integer layer : tt.getLayersOnTrack()){
                            plots1D.get("pos_fake_track_layers_hit").fill(layer);
                            plots1D.get("pos_fake_track_n_mcps_on_layer").fill(tt.getMCPsOnLayer(layer).size());
                            plots2D.get("pos_fake_track_n_mcps_per_layer").fill(layer,tt.getMCPsOnLayer(layer).size());
                            plots1D.get("pos_fake_track_n_striphits_on_layer").fill(tt.getStripHitsOnLayer(layer).size());
                            plots2D.get("pos_fake_track_n_striphits_per_layer").fill(layer,tt.getStripHitsOnLayer(layer).size());
                            for(RawTrackerHit rawhit : tt.getStripHitsOnLayer(layer)){
                                plots1D.get("pos_fake_track_n_mcps_on_striphit").fill(tt.getMCPsOnRawTrackerHit(rawhit).size());
                                plots2D.get("pos_fake_track_n_mcps_on_layer_striphits").fill(layer,tt.getMCPsOnRawTrackerHit(rawhit).size());
                            }
                        }

                    }
                }

                //If Track is real
                if(realTrack){
                    if(charge < 0){
                        if(!mcps.contains(mcp)){
                            plots1D.get("ele_real_track_momentum").fill(trackPmag);
                            plots1D.get("ele_real_track_tanlambda").fill(tanlambda);
                            plots1D.get("ele_real_track_phi0").fill(track_phi);
                            plots1D.get("ele_real_track_d0").fill(track_d0);
                            plots1D.get("ele_real_track_z0").fill(track_z0);
                            plots1D.get("ele_real_track_C").fill(track_C);
                        }

                        else{
                            plots1D.get("ele_real_duplicate_tracks_momentum").fill(trackPmag);
                            plots1D.get("ele_real_duplicate_tracks_tanlambda").fill(tanlambda);
                            plots1D.get("ele_real_duplicate_tracks_phi0").fill(track_phi);
                            plots1D.get("ele_real_duplicate_tracks_d0").fill(track_d0);
                            plots1D.get("ele_real_duplicate_tracks_z0").fill(track_z0);
                            plots1D.get("ele_real_duplicate_tracks_C").fill(track_C);
                        }

                        plots2D.get("ele_real_track_v_matched_mcp_momentum").fill(charge*trackPmag,((int)mcp.getCharge())*mcp.getMomentum().magnitude());
                        plots1D.get("ele_real_track_n_mcps_on_track").fill(tt.getLayerHitsForAllMCPs().size());
                        plots1D.get("ele_real_track_n_hits").fill(tt.getNHits());
                        plots1D.get("ele_real_track_purity").fill(tt.getPurity());
                        plots1D.get("ele_real_track_n_goodhits").fill(tt.getNGoodHits());
                        plots1D.get("ele_real_track_n_badhits").fill(tt.getNBadHits());
                        for(Integer layer : tt.getLayersOnTrack()){
                            plots1D.get("ele_real_track_layers_hit").fill(layer);
                            plots1D.get("ele_real_track_n_mcps_on_layer").fill(tt.getMCPsOnLayer(layer).size());
                            plots2D.get("ele_real_track_n_mcps_per_layer").fill(layer,tt.getMCPsOnLayer(layer).size());
                            plots1D.get("ele_real_track_n_striphits_on_layer").fill(tt.getStripHitsOnLayer(layer).size());
                            plots2D.get("ele_real_track_n_striphits_per_layer").fill(layer,tt.getStripHitsOnLayer(layer).size());
                            for(RawTrackerHit rawhit : tt.getStripHitsOnLayer(layer)){
                                plots1D.get("ele_real_track_n_mcps_on_striphit").fill(tt.getMCPsOnRawTrackerHit(rawhit).size());
                                plots2D.get("ele_real_track_n_mcps_on_layer_striphits").fill(layer,tt.getMCPsOnRawTrackerHit(rawhit).size());
                            }
                        }

                        //plot good and bad hit layers
                        for(Integer layer : tt.getGoodHitLayers()){
                            plots1D.get("ele_real_track_good_hit_layers").fill(layer);
                        }
                        for(Integer layer : tt.getBadHitLayers()){
                            plots1D.get("ele_real_track_bad_hit_layers").fill(layer);
                        }
                    }

                    else{
                        if(!mcps.contains(mcp)){
                            plots1D.get("pos_real_track_momentum").fill(trackPmag);
                            plots1D.get("pos_real_track_tanlambda").fill(tanlambda);
                            plots1D.get("pos_real_track_phi0").fill(track_phi);
                            plots1D.get("pos_real_track_d0").fill(track_d0);
                            plots1D.get("pos_real_track_z0").fill(track_z0);
                            plots1D.get("pos_real_track_C").fill(track_C);
                        }

                        else{
                            plots1D.get("pos_real_duplicate_tracks_momentum").fill(trackPmag);
                            plots1D.get("pos_real_duplicate_tracks_tanlambda").fill(tanlambda);
                            plots1D.get("pos_real_duplicate_tracks_phi0").fill(track_phi);
                            plots1D.get("pos_real_duplicate_tracks_d0").fill(track_d0);
                            plots1D.get("pos_real_duplicate_tracks_z0").fill(track_z0);
                            plots1D.get("pos_real_duplicate_tracks_C").fill(track_C);
                        }

                        plots2D.get("pos_real_track_v_matched_mcp_momentum").fill(charge*trackPmag,((int)mcp.getCharge())*mcp.getMomentum().magnitude());
                        plots1D.get("pos_real_track_n_mcps_on_track").fill(tt.getLayerHitsForAllMCPs().size());
                        plots1D.get("pos_real_track_n_hits").fill(tt.getNHits());
                        plots1D.get("pos_real_track_purity").fill(tt.getPurity());
                        plots1D.get("pos_real_track_n_goodhits").fill(tt.getNGoodHits());
                        plots1D.get("pos_real_track_n_badhits").fill(tt.getNBadHits());
                        for(Integer layer : tt.getLayersOnTrack()){
                            plots1D.get("pos_real_track_layers_hit").fill(layer);
                            plots1D.get("pos_real_track_n_mcps_on_layer").fill(tt.getMCPsOnLayer(layer).size());
                            plots2D.get("pos_real_track_n_mcps_per_layer").fill(layer,tt.getMCPsOnLayer(layer).size());
                            plots1D.get("pos_real_track_n_striphits_on_layer").fill(tt.getStripHitsOnLayer(layer).size());
                            plots2D.get("pos_real_track_n_striphits_per_layer").fill(layer,tt.getStripHitsOnLayer(layer).size());
                            for(RawTrackerHit rawhit : tt.getStripHitsOnLayer(layer)){
                                plots1D.get("pos_real_track_n_mcps_on_striphit").fill(tt.getMCPsOnRawTrackerHit(rawhit).size());
                                plots2D.get("pos_real_track_n_mcps_on_layer_striphits").fill(layer,tt.getMCPsOnRawTrackerHit(rawhit).size());
                            }
                        }

                        //plot good and bad hit layers
                        for(Integer layer : tt.getGoodHitLayers()){
                            plots1D.get("pos_real_track_good_hit_layers").fill(layer);
                        }
                        for(Integer layer : tt.getBadHitLayers()){
                            plots1D.get("pos_real_track_bad_hit_layers").fill(layer);
                        }
                    }

                    //MCP based plots
                    
                    //Explicity require fill numerator with subset of
                    //denominator
                    if(trackableMCPMap.containsKey(mcp) ){
                        if(mcp.getCharge() < 0){
                            if(!mcps.contains(mcp)){
                                plots1D.get("ele_real_track_mcp_momentum").fill(mcp.getMomentum().magnitude());
                                plots1D.get("ele_real_track_mcp_tanlambda").fill(mcp_slope);
                                plots1D.get("ele_real_track_mcp_phi0").fill(mcp_phi);
                                plots1D.get("ele_real_track_mcp_d0").fill(mcp_d0);
                                plots1D.get("ele_real_track_mcp_z0").fill(mcp_z0);
                                plots1D.get("ele_real_track_mcp_C").fill(mcp_C);
                                if(nHits >= 12)
                                    plots1D.get("ele_real_track_nhits_gteq_12_mcp_momentum").fill(mcp.getMomentum().magnitude());
                            }

                            else{
                                plots1D.get("ele_real_duplicate_tracks_mcp_momentum").fill(mcp.getMomentum().magnitude());
                                plots1D.get("ele_real_duplicate_tracks_mcp_tanlambda").fill(mcp_slope);
                                plots1D.get("ele_real_duplicate_tracks_mcp_phi0").fill(mcp_phi);
                                plots1D.get("ele_real_duplicate_tracks_mcp_d0").fill(mcp_d0);
                                plots1D.get("ele_real_duplicate_tracks_mcp_z0").fill(mcp_z0);
                                plots1D.get("ele_real_duplicate_tracks_mcp_C").fill(mcp_C);
                            }
                        }
                        else{
                            if(!mcps.contains(mcp)){
                                if(nHits >= 12)
                                    plots1D.get("pos_real_track_nhits_gteq_12_mcp_momentum").fill(mcp.getMomentum().magnitude());
                                plots1D.get("pos_real_track_mcp_momentum").fill(mcp.getMomentum().magnitude());
                                plots1D.get("pos_real_track_mcp_tanlambda").fill(mcp_slope);
                                plots1D.get("pos_real_track_mcp_phi0").fill(mcp_phi);
                                plots1D.get("pos_real_track_mcp_d0").fill(mcp_d0);
                                plots1D.get("pos_real_track_mcp_z0").fill(mcp_z0);
                                plots1D.get("pos_real_track_mcp_C").fill(mcp_C);
                            }
                            else{
                                plots1D.get("pos_real_duplicate_tracks_mcp_momentum").fill(mcp.getMomentum().magnitude());
                                plots1D.get("pos_real_duplicate_tracks_mcp_tanlambda").fill(mcp_slope);
                                plots1D.get("pos_real_duplicate_tracks_mcp_phi0").fill(mcp_phi);
                                plots1D.get("pos_real_duplicate_tracks_mcp_d0").fill(mcp_d0);
                                plots1D.get("pos_real_duplicate_tracks_mcp_z0").fill(mcp_z0);
                                plots1D.get("pos_real_duplicate_tracks_mcp_C").fill(mcp_C);
                            }
                        }
                    }
                    //Add this MCP to a list to check for duplicate Track->MCP
                    //matches 
                    mcps.add(mcp);
                }

                //Add track<->truth relations for all Tracks matched to a MCP
                //with purity >= 0.5
                if(purity >= 0.5){
                    //Add track to mcp relations
                    trackToMCParticleRelations.add(new BaseLCRelation(track,mcp));

                    //Transform MCP into helical track
                    HelicalTrackFit mcp_htf  = TrackUtils.getHTF(mcp,bfield);
                    BaseTrack truth_trk  = new BaseTrack();
                    truth_trk.setTrackParameters(mcp_htf.parameters(),bfield);
                    truth_trk.getTrackStates().clear();
                    double[] ref = new double[] { 0., 0., 0. };
                    SymmetricMatrix cov = new SymmetricMatrix(5);
                    TrackState stateIP = new BaseTrackState(mcp_htf.parameters(),ref,cov.asPackedArray(true),TrackState.AtPerigee,bfield);
                    truth_trk.getTrackStates().add(stateIP);
                    truth_trk.setChisq(-1);
                    truth_trk.setNDF(-1);
                    truth_trk.setFitSuccess(false);
                    truth_trk.setRefPointIsDCA(true);
                    truth_trk.setTrackType(-1);
                    truthTrackCollection.add(truth_trk);
                    trackToTruthTrackRelations.add(new BaseLCRelation(track,truth_trk));

                    //Check hit quality by layer
                    int[] nmcps_byLayer = new int[NLAYERS];
                    int[] goodhits_byLayer = new int[NLAYERS];
                    Set<Integer> layers = tt.getLayersOnTrack();
                    for(int layer : layers){
                        Set<MCParticle> particles = tt.getMCPsOnLayer(layer);
                        nmcps_byLayer[layer-1] = particles.size();
                        if(particles.contains(mcp)){
                            goodhits_byLayer[layer-1] = 1;
                        }
                        else{
                            goodhits_byLayer[layer-1] = -1;
                        }
                    }
                    TrackTruthInfo truthinfo = new TrackTruthInfo(NLAYERS);
                    truthinfo.setTrackNMCPs(nmcps_byLayer);
                    truthinfo.setTrackGoodHits(goodhits_byLayer);
                    truthinfo.setTrackPurity(purity);
                    trackTruthInfoCollection.add(truthinfo);
                    trackToTruthInfoRelations.add(new BaseLCRelation(track,truthinfo));


                }
            }
        }

        if(writeToLCIO){
            //end of process, add collections to lcio
            int flag = 1 << LCIOConstants.TRBIT_HITS;
            event.put(trackCollectionName+"TruthTracks", truthTrackCollection, Track.class, flag);
            event.put(trackCollectionName+"ToTruthTrackRelations", trackToTruthTrackRelations, LCRelation.class, 0);
            event.put(trackCollectionName+"ToMCParticleRelations", trackToMCParticleRelations, LCRelation.class, 0);
            event.put(trackCollectionName+"ToTruthInfoRelations", trackToTruthInfoRelations, LCRelation.class, 0);
            event.put(trackCollectionName+"TrackTruthInfo", trackTruthInfoCollection, TrackTruthInfo.class, 0);
        }

    }

    //Fit MCP to helical track and return fit params
    public double[] getMCPTrackParameters(MCParticle mcp, double bfield){

        HelicalTrackFit mcp_htf  = TrackUtils.getHTF(mcp,bfield);
        if(enablePlots)
            plots1D.get("mcp_helicalTrackFit_phi").fill(mcp_htf.phi0());
        BaseTrack truth_trk  = new BaseTrack();
        truth_trk.setTrackParameters(mcp_htf.parameters(),bfield);
        truth_trk.getTrackStates().clear();
        double[] ref = new double[] { 0., 0., 0. };
        SymmetricMatrix cov = new SymmetricMatrix(5);
        TrackState stateIP = new BaseTrackState(mcp_htf.parameters(),ref,cov.asPackedArray(true),TrackState.AtPerigee,bfield);
        truth_trk.getTrackStates().add(stateIP);
        truth_trk.setChisq(-1);
        truth_trk.setNDF(-1);
        truth_trk.setFitSuccess(false);
        truth_trk.setRefPointIsDCA(true);
        truth_trk.setTrackType(-1);
        //Parameter references
        double d0    = truth_trk.getTrackStates().get(0).getD0();
        double z0    = truth_trk.getTrackStates().get(0).getZ0();
        double C     = truth_trk.getTrackStates().get(0).getOmega();
        double phi   = truth_trk.getTrackStates().get(0).getPhi();
        double slope = truth_trk.getTrackStates().get(0).getTanLambda();

        double[] params = {d0, z0, C, phi, slope};
        return params;
    }

    public Map<MCParticle, Map<Integer, List<SimTrackerHit>>> getTrackableMCPs(List<MCParticle> mcparticles, List<SimTrackerHit> simhits, RelationalTable rawtomc, int NhitsRequired){
        Map<MCParticle, Map<Integer, List<SimTrackerHit>>> trackableMCPs = new HashMap<MCParticle, Map<Integer, List<SimTrackerHit>>>();

        for(MCParticle particle : mcparticles){
            if(Math.abs(particle.getPDGID()) != 11)
                continue;
            int charge = (int) particle.getCharge();


            //Fit MCP to helical track to get params
            double[] mcpParams = getMCPTrackParameters(particle, bfield);
            double d0 = mcpParams[0];
            double z0 = mcpParams[1];
            double C = mcpParams[2];
            double phi0 = mcpParams[3];
            double tanlambda = mcpParams[4];
            double momentum = particle.getMomentum().magnitude();
            double px = particle.getPX();
            double py = particle.getPY();
            double pz = particle.getPZ();

            //plots for all charged MCPs
            if(charge < 0){
                plots1D.get("ele_mcp_px").fill(px);
                plots1D.get("ele_mcp_py").fill(py);
                plots1D.get("ele_mcp_pz").fill(pz);
                plots2D.get("ele_mcp_px_v_py").fill(px,py);
                plots2D.get("ele_mcp_px_v_pz").fill(px,pz);
                plots2D.get("ele_mcp_py_v_pz").fill(py,pz);

                plots1D.get("ele_mcp_momentum").fill(momentum);
                plots1D.get("ele_mcp_tanlambda").fill(tanlambda);
                plots2D.get("ele_mcp_momentum_v_tanlambda").fill(momentum, tanlambda);
                plots1D.get("ele_mcp_d0").fill(d0);
                plots2D.get("ele_mcp_momentum_v_d0").fill(momentum, d0);
                plots1D.get("ele_mcp_z0").fill(z0);
                plots2D.get("ele_mcp_momentum_v_z0").fill(momentum, z0);
                plots1D.get("ele_mcp_C").fill(C);
                plots2D.get("ele_mcp_momentum_v_C").fill(momentum, C);
                plots1D.get("ele_mcp_phi0").fill(phi0);
                plots2D.get("ele_mcp_momentum_v_phi0").fill(momentum, phi0);
            }
            else{
                plots1D.get("pos_mcp_px").fill(px);
                plots1D.get("pos_mcp_py").fill(py);
                plots1D.get("pos_mcp_pz").fill(pz);
                plots2D.get("pos_mcp_px_v_py").fill(px,py);
                plots2D.get("pos_mcp_px_v_pz").fill(px,pz);
                plots2D.get("pos_mcp_py_v_pz").fill(py,pz);

                plots1D.get("pos_mcp_momentum").fill(momentum);
                plots1D.get("pos_mcp_tanlambda").fill(tanlambda);
                plots2D.get("pos_mcp_momentum_v_tanlambda").fill(momentum, tanlambda);
                plots1D.get("pos_mcp_d0").fill(d0);
                plots2D.get("pos_mcp_momentum_v_d0").fill(momentum, d0);
                plots1D.get("pos_mcp_z0").fill(z0);
                plots2D.get("pos_mcp_momentum_v_z0").fill(momentum, z0);
                plots1D.get("pos_mcp_C").fill(C);
                plots2D.get("pos_mcp_momentum_v_C").fill(momentum, C);
                plots1D.get("pos_mcp_phi0").fill(phi0);
                plots2D.get("pos_mcp_momentum_v_phi0").fill(momentum, phi0);
            }

            Map<Integer, List<SimTrackerHit>> layerhitsMap = new HashMap<Integer, List<SimTrackerHit>>();
            for(SimTrackerHit simhit : simhits){
                MCParticle simhitmcp = simhit.getMCParticle();
                if(simhitmcp == particle){
                    int layer = simhit.getLayer();
                    Set<RawTrackerHit> rawhits = rawtomc.allTo(simhit);
                    if(!layerhitsMap.containsKey(layer)){
                        List<SimTrackerHit> tmp = new ArrayList<SimTrackerHit>();
                        tmp.add(simhit);
                        layerhitsMap.put(layer, tmp); 
                    }
                    else{
                        List<SimTrackerHit> tmp = layerhitsMap.get(layer);
                        tmp.add(simhit);
                        layerhitsMap.put(layer, tmp);
                    }
                }
            }

            //Check n layers hit
            if(particle.getCharge() < 0)
                plots2D.get("ele_mcp_momentum_v_nLayersHit").fill(particle.getMomentum().magnitude(), layerhitsMap.size());
            else
                plots2D.get("pos_mcp_momentum_v_nLayersHit").fill(particle.getMomentum().magnitude(), layerhitsMap.size());

            //Require minimum number of SimTrackerHits
            if(layerhitsMap.size() < NhitsRequired)
                continue;

            if(particle.getCharge() < 0){
                plots1D.get("ele_trackable_mcp_nSimTrackerHits").fill(layerhitsMap.size());
                plots2D.get("ele_trackable_mcp_momentum_v_nSimTrackerHits").fill(particle.getMomentum().magnitude(), layerhitsMap.size());
                plots1D.get("ele_trackable_mcp_momentum").fill(particle.getMomentum().magnitude());
                plots1D.get("ele_trackable_mcp_tanlambda").fill(tanlambda);
                plots1D.get("ele_trackable_mcp_phi0").fill(phi0);
                plots1D.get("ele_trackable_mcp_d0").fill(d0);
                plots1D.get("ele_trackable_mcp_z0").fill(z0);
                plots1D.get("ele_trackable_mcp_C").fill(C);
            }
            else{
                plots1D.get("pos_trackable_mcp_nSimTrackerHits").fill(layerhitsMap.size());
                plots2D.get("pos_trackable_mcp_momentum_v_nSimTrackerHits").fill(particle.getMomentum().magnitude(), layerhitsMap.size());
                plots1D.get("pos_trackable_mcp_momentum").fill(particle.getMomentum().magnitude());
                plots1D.get("pos_trackable_mcp_tanlambda").fill(tanlambda);
                plots1D.get("pos_trackable_mcp_phi0").fill(phi0);
                plots1D.get("pos_trackable_mcp_d0").fill(d0);
                plots1D.get("pos_trackable_mcp_z0").fill(z0);
                plots1D.get("pos_trackable_mcp_C").fill(C);
            }

            trackableMCPs.put(particle, layerhitsMap);
        }
        return trackableMCPs;
    }

    /*
    public void getTrackStateAtEachLayer(){
        List<HpsSiSensor> sensors = detector.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);
        HpsSiSensor sensor = (HpsSiSensor) tt.getStripHitsOnLayer(layer).get(0).getDetectorElement();
        Hep3Vector posAtSensor = TrackStateUtils.getLocationAtSensor(track, sensor, bfield);
        Hep3Vector getLocationAtSensor(Track track, HpsSiSensor sensor, double bfield)
    }
    */
}
