package org.hps.recon.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackData;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Applies the hpstr PreselectAndCategorize2021 V0 preselection cuts (timing, chi2,
 * momentum, hit count, vertex quality) to Kalman V0 candidates, then runs
 * TrackTruthMatcher on the surviving electron/positron tracks. This lets the
 * ele/pos truth-match rate be compared to hpstr's post-preselection numbers on an
 * equivalent basis, since raw (unpreselected) track samples are dominated by
 * background/ghost tracks that hpstr never sees.
 *
 * Cut values are taken verbatim from the MC branch of
 * PreselectAndCategorize2021::determine_time_cuts and the vertex cutflow in
 * PreselectAndCategorize2021::process (hpstr, 2021 analysis).
 */
public class Preselection2021 extends Driver {

    private AIDA aida;
    private String v0CollectionName = "UnconstrainedV0Candidates_KF";
    // Full Kalman track collection (not just the V0 daughters), used to check whether
    // other tracks in the event are picking up ("stealing") the A' electron's hits.
    private String trackCollectionName = "KalmanFullTracks";
    // Truth-only Kalman refit track collection (see AprimeElectronHitFilterDriver): a second
    // Kalman pass run using ONLY the Si clusters truth-matched to the true A' electron, when
    // there are enough of them. Empty/absent in events where too few layers matched.
    private String apEleOnlyTrackCollectionName = "KalmanFullTracks_ApEleOnly";
    private double purityCut = 0.5;
    private boolean enablePlots = true;
    // Loose kinematic-compatibility cut used only for the "_kinmatch" sub-categories
    // (see isKinMatch()): max fractional difference between the electron track's total
    // momentum and the true A' electron's, once both are in the same (tracking) frame.
    private double kinMatchMomentumFrac = 0.15;
    // Minimum number of distinct layers with a SimTrackerHit belonging to the true A'
    // electron for it to be considered "findable" - required by isKinMatch() so the
    // "_kinmatch" sub-categories only flag tracks whose truth electron could plausibly
    // have been reconstructed in the first place.
    private static final int MIN_FINDABLE_LAYERS = 10;

    private static final double POS_CLUSTER_E_MIN = 0.2;
    private static final double ELE_P_MIN = 0.2;
    private static final double ELE_P_MAX = 2.9;
    private static final double POS_P_MIN = 0.4;
    private static final double CHI2_NDF_MAX = 20.0;
    private static final int MIN_HITS = 10;
    private static final double ELE_CLUSTER_TIME_MAX = 6.0;
    private static final double POS_CLUSTER_TIME_MAX = 5.7;
    private static final double ELE_POS_TRACK_TIME_MAX = 9.2;
    // Kalman track time and ECal cluster time are on different absolute clocks
    // (raw offset ~38-40 ns observed in this MC sample; StandardCuts' 55 ns
    // constant is calibrated for GBL track time, not Kalman, so it doesn't apply
    // directly here). Calibrated empirically from raw_{ele,pos}_cluster_dt below.
    private static final double TRACK_CLUSTER_TIME_OFFSET = -37.7;
    private static final double VERTEX_CHI2_MAX = 30.0;
    private static final double VTX_MAX_P = 4.0;
    private static final double VTX_Z_MIN = -20.0;
    private static final double VTX_Z_MAX = 150.0;
    private static final double ELECTRON_MASS = 0.000511;
    private static final int APRIME_PDGID = 622;

    // truth_category histogram bin values
    private static final int CAT_UNMATCHED = 0;
    private static final int CAT_APRIME_DAUGHTER = 1;
    private static final int CAT_OTHER = 2;
    private static final int CAT_NOHITS = 3;

    // V0/track category split, keyed off the electron's truth_category:
    // "aprime" = both ele+pos matched to A' daughters (signal); "recoil" = electron
    // matched to a non-A'-daughter (e.g. the beam recoil electron); "unmatched" = electron
    // truth match failed the purity cut; "nohits" = none of the electron's hits matched
    // any MC particle at all (purity exactly 0, e.g. a pulser-overlay/background track).
    private static final String[] CATEGORIES = {"aprime", "recoil", "unmatched", "nohits"};

    // Sub-categories of "unmatched"/"nohits": the electron track is nonetheless
    // kinematically compatible with the true A' electron MCParticle (same sign of
    // py/pz, total momentum within KINMATCH_MOMENTUM_FRAC) - i.e. the truth-hit chain
    // likely broke even though the track itself is probably genuine. See isKinMatch().
    private static final String[] KINMATCH_SUBCATEGORIES = {"unmatched_kinmatch", "nohits_kinmatch"};
    // Union used for booking every category-suffixed histogram; CATEGORIES alone still
    // drives categoryIndex()/ele_pos_category, which is unaffected by the kinmatch flag.
    private static final String[] ALL_CATEGORIES = {
        "aprime", "recoil", "unmatched", "nohits", "unmatched_kinmatch", "nohits_kinmatch"
    };

    private static final int NLAYERS = 14;

    private static final String[] CUT_NAMES = {
        "seed", "posClusterE", "eleP", "posP", "eleChi2Ndf", "posChi2Ndf",
        "eleNHits", "posNHits", "eleClusterTime", "posClusterTime",
        "elePosTrackTime", "vertexChi2", "vtxMaxP"
    };
    private int[] nPassCut = new int[CUT_NAMES.length];
    private int nV0Candidates = 0;
    // Running count of events seen by this driver, in file order (0-based), for use with
    // "dumpevent <file> <n>" (the n-th-event form), since run/event numbers don't reliably
    // dereference into dumpevent.
    private int eventCount = 0;

    private Map<String, IHistogram1D> plots1D = new HashMap<String, IHistogram1D>();
    private Map<String, IHistogram2D> plots2D = new HashMap<String, IHistogram2D>();

    public void setV0CollectionName(String name) {
        this.v0CollectionName = name;
    }

    public void setTrackCollectionName(String name) {
        this.trackCollectionName = name;
    }

    public void setApEleOnlyTrackCollectionName(String name) {
        this.apEleOnlyTrackCollectionName = name;
    }

    public void setPurityCut(double cut) {
        this.purityCut = cut;
    }

    public void setKinMatchMomentumFrac(double frac) {
        this.kinMatchMomentumFrac = frac;
    }

    public void setEnablePlots(boolean val) {
        this.enablePlots = val;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        if (aida == null)
            aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        aida.tree().mkdirs("TrkTruthPresel");
        aida.tree().cd("TrkTruthPresel");
        if (enablePlots)
            bookHistograms();
    }

    private void bookHistograms() {
        IHistogramFactory hf = aida.histogramFactory();
        for (String ch : new String[]{"ele", "pos"}) {
            plots1D.put(ch + "_reco_track_momentum", hf.createHistogram1D(ch + "_reco_track_momentum", 100, 0, 4.0));
            plots1D.put(ch + "_purity", hf.createHistogram1D(ch + "_purity", 50, 0, 1.001));
            plots1D.put(ch + "_good_hit_layers", hf.createHistogram1D(ch + "_good_hit_layers", NLAYERS, 0.5, NLAYERS + 0.5));
            plots1D.put(ch + "_bad_hit_layers", hf.createHistogram1D(ch + "_bad_hit_layers", NLAYERS, 0.5, NLAYERS + 0.5));
            plots1D.put(ch + "_wrong_hit_layers", hf.createHistogram1D(ch + "_wrong_hit_layers", NLAYERS, 0.5, NLAYERS + 0.5));
            plots1D.put(ch + "_nonmcp_hit_layers", hf.createHistogram1D(ch + "_nonmcp_hit_layers", NLAYERS, 0.5, NLAYERS + 0.5));
            plots1D.put(ch + "_missed_hit_layers", hf.createHistogram1D(ch + "_missed_hit_layers", NLAYERS, 0.5, NLAYERS + 0.5));
            plots1D.put(ch + "_n_mcps_on_layer", hf.createHistogram1D(ch + "_n_mcps_on_layer", NLAYERS, 0.5, NLAYERS + 0.5));

            // track parameter / quality plots
            plots1D.put(ch + "_chi2Ndf", hf.createHistogram1D(ch + "_chi2Ndf", 100, 0, CHI2_NDF_MAX * 1.2));
            plots1D.put(ch + "_nHits", hf.createHistogram1D(ch + "_nHits", NLAYERS, 0.5, NLAYERS + 0.5));
            plots1D.put(ch + "_d0", hf.createHistogram1D(ch + "_d0", 100, -5, 5));
            plots1D.put(ch + "_z0", hf.createHistogram1D(ch + "_z0", 100, -5, 5));
            plots1D.put(ch + "_phi0", hf.createHistogram1D(ch + "_phi0", 100, -1.0, 1.0));
            plots1D.put(ch + "_omega", hf.createHistogram1D(ch + "_omega", 100, -0.002, 0.002));
            plots1D.put(ch + "_tanLambda", hf.createHistogram1D(ch + "_tanLambda", 100, -0.1, 0.1));
            // momentum-component ratios (tracking frame): px/p is along the beam, py/p is
            // in the bend plane - same quantities used for the kin-match sub-categories.
            plots1D.put(ch + "_pxop", hf.createHistogram1D(ch + "_pxop", 100, -1.0, 1.0));
            plots1D.put(ch + "_pyop", hf.createHistogram1D(ch + "_pyop", 100, -1.0, 1.0));
            // 0=unmatched, 1=matched to A' daughter (signal), 2=matched to non-A'-daughter (e.g. recoil electron), 3=no hits matched any MC particle
            plots1D.put(ch + "_truth_category", hf.createHistogram1D(ch + "_truth_category", 4, -0.5, 3.5));

            // same track quantities, split by the ele-based V0 category, plus the
            // "_kinmatch" sub-categories of "unmatched"/"nohits" (see ALL_CATEGORIES)
            for (String cat : ALL_CATEGORIES) {
                plots1D.put(ch + "_reco_track_momentum_" + cat, hf.createHistogram1D(ch + "_reco_track_momentum_" + cat, 100, 0, 4.0));
                plots1D.put(ch + "_chi2Ndf_" + cat, hf.createHistogram1D(ch + "_chi2Ndf_" + cat, 100, 0, CHI2_NDF_MAX * 1.2));
                plots1D.put(ch + "_nHits_" + cat, hf.createHistogram1D(ch + "_nHits_" + cat, NLAYERS, 0.5, NLAYERS + 0.5));
                plots1D.put(ch + "_d0_" + cat, hf.createHistogram1D(ch + "_d0_" + cat, 100, -5, 5));
                plots1D.put(ch + "_z0_" + cat, hf.createHistogram1D(ch + "_z0_" + cat, 100, -5, 5));
                plots1D.put(ch + "_phi0_" + cat, hf.createHistogram1D(ch + "_phi0_" + cat, 100, -1.0, 1.0));
                plots1D.put(ch + "_omega_" + cat, hf.createHistogram1D(ch + "_omega_" + cat, 100, -0.002, 0.002));
                plots1D.put(ch + "_tanLambda_" + cat, hf.createHistogram1D(ch + "_tanLambda_" + cat, 100, -0.1, 0.1));
                plots1D.put(ch + "_pxop_" + cat, hf.createHistogram1D(ch + "_pxop_" + cat, 100, -1.0, 1.0));
                plots1D.put(ch + "_pyop_" + cat, hf.createHistogram1D(ch + "_pyop_" + cat, 100, -1.0, 1.0));
            }
        }

        // Per-layer (0-13) TrackData isolation for the recon V0 electron track, in
        // its own subfolder. Sign convention (see TrackData.getIsolation()): positive
        // if the nearest other hit on the layer is outwards from the beam plane,
        // negative if inwards; offscale if there's no hit on the layer at all (this
        // track) or no other hit on the layer (nothing to be isolated from).
        aida.tree().mkdirs("EleIsolation");
        aida.tree().cd("EleIsolation");
        for (int layer = 0; layer < NLAYERS; layer++)
            plots1D.put("ele_isolation_layer" + layer, hf.createHistogram1D("ele_isolation_layer" + layer, 100, 0.0, 5.0));
        // Per-layer count of isolation values that are exactly 0.0 (as opposed to
        // merely landing in the first bin of the histograms above), to check whether
        // genuine iso==0 entries (not just small-but-nonzero ones) occur.
        plots1D.put("ele_isolation_zero_count", hf.createHistogram1D("ele_isolation_zero_count", NLAYERS, -0.5, NLAYERS - 0.5));
        aida.tree().cd("..");

        plots1D.put("cutflow", hf.createHistogram1D("cutflow", CUT_NAMES.length, 0, CUT_NAMES.length));
        plots1D.put("n_v0_pass_preselection", hf.createHistogram1D("n_v0_pass_preselection", 10, -0.5, 9.5));
        // Correlation of the electron's and positron's truth categories (see CATEGORIES,
        // indices in that order) for the same V0: tests whether "unmatched"/"nohits" is
        // an event-level truth-relation failure (both legs fail together) or specific to
        // one leg (e.g. only the electron fails while the positron matches cleanly).
        int nCat = CATEGORIES.length;
        plots2D.put("ele_pos_category", hf.createHistogram2D("ele_pos_category", nCat, -0.5, nCat - 0.5, nCat, -0.5, nCat - 0.5));
        // A' electron MCParticle truth vs. V0 reco electron track, split by the ele-based
        // V0 category, plus the "_kinmatch" sub-categories (see ALL_CATEGORIES): x-axis is
        // the truth A' electron, y-axis is the reconstructed electron track from the same
        // event's V0 candidate.
        for (String cat : ALL_CATEGORIES) {
            plots2D.put("aprime_ele_truth_vs_reco_p_" + cat,
                    hf.createHistogram2D("aprime_ele_truth_vs_reco_p_" + cat, 100, 0, 4.0, 100, 0, 4.0));
            plots2D.put("aprime_ele_truth_vs_reco_px_" + cat,
                    hf.createHistogram2D("aprime_ele_truth_vs_reco_px_" + cat, 100, -0.3, 0.3, 100, -0.3, 0.3));
            plots2D.put("aprime_ele_truth_vs_reco_py_" + cat,
                    hf.createHistogram2D("aprime_ele_truth_vs_reco_py_" + cat, 100, -0.3, 0.3, 100, -0.3, 0.3));
        }
        // Are other tracks in the event stealing the A' electron's hits? x-axis is the
        // number of layers with a SimTrackerHit left by the true A' electron (i.e. how
        // many hits are actually available to be stolen); y-axis is the number of
        // layer-clusters on all OTHER tracks in the event (not the V0 electron track
        // itself) that have a contribution from the true A' electron, summed over all of
        // those other tracks. Split by the ele-based V0 category (see ALL_CATEGORIES).
        for (String cat : ALL_CATEGORIES) {
            plots2D.put("aprime_ele_nsimhits_vs_stolen_clusters_" + cat,
                    hf.createHistogram2D("aprime_ele_nsimhits_vs_stolen_clusters_" + cat,
                            NLAYERS + 1, -0.5, NLAYERS + 0.5, 3 * NLAYERS + 1, -0.5, 3 * NLAYERS + 0.5));
        }
        // Truth-only Kalman refit (KalmanFullTracks_ApEleOnly, see
        // AprimeElectronHitFilterDriver): does a fit using ONLY the true A' electron's
        // truth-matched Si clusters find a good track, and how does it compare to MC truth
        // and to the standard V0 electron reco track.
        plots1D.put("apeleonly_nmatched_layers", hf.createHistogram1D("apeleonly_nmatched_layers", NLAYERS + 1, -0.5, NLAYERS + 0.5));
        plots1D.put("apeleonly_refit_found", hf.createHistogram1D("apeleonly_refit_found", 2, -0.5, 1.5));
        plots1D.put("apeleonly_chi2ndf", hf.createHistogram1D("apeleonly_chi2ndf", 100, 0, CHI2_NDF_MAX * 1.2));
        plots2D.put("apeleonly_p_vs_truth_p", hf.createHistogram2D("apeleonly_p_vs_truth_p", 100, 0, 4.0, 100, 0, 4.0));
        plots2D.put("apeleonly_px_vs_truth_px", hf.createHistogram2D("apeleonly_px_vs_truth_px", 100, -0.3, 0.3, 100, -0.3, 0.3));
        plots2D.put("apeleonly_py_vs_truth_py", hf.createHistogram2D("apeleonly_py_vs_truth_py", 100, -0.3, 0.3, 100, -0.3, 0.3));
        // x = truth-only refit track momentum, y = standard V0 electron track momentum,
        // split by the ele-based V0 category (see ALL_CATEGORIES) - lets "does the
        // truth-restricted fit agree with standard reco" be checked not just for the clean
        // "aprime" case but also for recoil/unmatched/nohits, where standard reco's hit
        // assignment for the electron went wrong in some way.
        for (String cat : ALL_CATEGORIES) {
            plots2D.put("apeleonly_p_vs_standard_p_" + cat,
                    hf.createHistogram2D("apeleonly_p_vs_standard_p_" + cat, 100, 0, 4.0, 100, 0, 4.0));
        }

        // PDGID of the parent of the electron matched to a non-A'-daughter MC particle
        // ("recoil"/"other" category); 0 means the matched MCP has no parent (primary).
        plots1D.put("ele_other_parent_pdgid", hf.createHistogram1D("ele_other_parent_pdgid", 5000, -2500.5, 2499.5));
        // For "unmatched" (below-majority-purity) electron tracks: parent PDGID of every
        // MC particle contributing a hit on the track (one entry per contributing MCP per
        // layer), to see what's mixed into the hits that keeps purity below the cut.
        plots1D.put("ele_unmatched_hit_parent_pdgid", hf.createHistogram1D("ele_unmatched_hit_parent_pdgid", 5000, -2500.5, 2499.5));
        // For "unmatched" electron tracks (0 < purity < purityCut) that also pass the loose
        // kin-match cut against the true A' electron (see isKinMatch()): per layer on the
        // track, was the hit actually contributed by the true A' electron ("correct") or by
        // something else / no MCParticle at all ("wrong") - i.e. where does the truth-hit
        // chain break for tracks that otherwise look like genuine A' electrons.
        plots1D.put("ele_correct_hit_layers_unmatched_kinmatch",
                hf.createHistogram1D("ele_correct_hit_layers_unmatched_kinmatch", NLAYERS, 0.5, NLAYERS + 0.5));
        plots1D.put("ele_wrong_hit_layers_unmatched_kinmatch",
                hf.createHistogram1D("ele_wrong_hit_layers_unmatched_kinmatch", NLAYERS, 0.5, NLAYERS + 0.5));
        // All layers with a hit on the track (regardless of correct/wrong), for the same
        // "unmatched"+kin-match tracks as above - the track's overall hit-layer occupancy.
        plots1D.put("ele_hit_layers_unmatched_kinmatch",
                hf.createHistogram1D("ele_hit_layers_unmatched_kinmatch", NLAYERS, 0.5, NLAYERS + 0.5));
        plots1D.put("raw_ele_cluster_dt", hf.createHistogram1D("raw_ele_cluster_dt", 200, -100, 100));
        plots1D.put("raw_pos_cluster_dt", hf.createHistogram1D("raw_pos_cluster_dt", 200, -100, 100));
        plots1D.put("raw_ele_pos_dt", hf.createHistogram1D("raw_ele_pos_dt", 200, -100, 100));

        // V0-level plots
        plots1D.put("v0_chi2", hf.createHistogram1D("v0_chi2", 100, 0, VERTEX_CHI2_MAX * 1.2));
        plots1D.put("v0_psum", hf.createHistogram1D("v0_psum", 100, 0, VTX_MAX_P * 1.2));
        plots1D.put("v0_mass", hf.createHistogram1D("v0_mass", 100, 0, 0.3));
        plots1D.put("v0_vtx_x", hf.createHistogram1D("v0_vtx_x", 100, -10, 10));
        plots1D.put("v0_vtx_y", hf.createHistogram1D("v0_vtx_y", 100, -2, 2));
        plots1D.put("v0_vtx_z", hf.createHistogram1D("v0_vtx_z", 100, VTX_Z_MIN, VTX_Z_MAX));
        plots1D.put("v0_true_vtx_x", hf.createHistogram1D("v0_true_vtx_x", 100, -10, 10));
        plots1D.put("v0_true_vtx_y", hf.createHistogram1D("v0_true_vtx_y", 100, -2, 2));
        plots1D.put("v0_true_vtx_z", hf.createHistogram1D("v0_true_vtx_z", 100, -100, 100));
        plots1D.put("v0_vtx_z_res", hf.createHistogram1D("v0_vtx_z_res", 100, -50, 50));

        // V0-level plots split by the ele-based V0 category, plus the "_kinmatch"
        // sub-categories (see ALL_CATEGORIES)
        for (String cat : ALL_CATEGORIES) {
            plots1D.put("v0_chi2_" + cat, hf.createHistogram1D("v0_chi2_" + cat, 100, 0, VERTEX_CHI2_MAX * 1.2));
            plots1D.put("v0_psum_" + cat, hf.createHistogram1D("v0_psum_" + cat, 100, 0, VTX_MAX_P * 1.2));
            plots1D.put("v0_mass_" + cat, hf.createHistogram1D("v0_mass_" + cat, 100, 0, 0.3));
            plots1D.put("v0_vtx_x_" + cat, hf.createHistogram1D("v0_vtx_x_" + cat, 100, -10, 10));
            plots1D.put("v0_vtx_y_" + cat, hf.createHistogram1D("v0_vtx_y_" + cat, 100, -2, 2));
            plots1D.put("v0_vtx_z_" + cat, hf.createHistogram1D("v0_vtx_z_" + cat, 100, VTX_Z_MIN, VTX_Z_MAX));
            plots1D.put("v0_true_vtx_x_" + cat, hf.createHistogram1D("v0_true_vtx_x_" + cat, 100, -10, 10));
            plots1D.put("v0_true_vtx_y_" + cat, hf.createHistogram1D("v0_true_vtx_y_" + cat, 100, -2, 2));
            plots1D.put("v0_true_vtx_z_" + cat, hf.createHistogram1D("v0_true_vtx_z_" + cat, 100, -100, 100));
            plots1D.put("v0_vtx_z_res_" + cat, hf.createHistogram1D("v0_vtx_z_res_" + cat, 100, -50, 50));
        }
    }

    @Override
    protected void process(EventHeader event) {
        int thisEventNum = eventCount++;

        if (!event.hasCollection(ReconstructedParticle.class, v0CollectionName))
            return;
        List<ReconstructedParticle> v0Candidates = event.get(ReconstructedParticle.class, v0CollectionName);
        if (v0Candidates.isEmpty())
            return;

        if (!event.hasCollection(LCRelation.class, "SVTTrueHitRelations"))
            return;
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
        for (LCRelation relation : trueHitRelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                rawtomc.add(relation.getFrom(), relation.getTo());

        List<SimTrackerHit> allsimhits = event.get(SimTrackerHit.class, "TrackerHits");

        // Full track collection (not just this V0's daughters) - used to check whether
        // other tracks in the event are picking up hits left by the true A' electron.
        List<Track> allTracks = event.hasCollection(Track.class, trackCollectionName)
                ? event.get(Track.class, trackCollectionName) : new ArrayList<Track>();

        // Production reconstruction's own track-to-MC-truth relation (built by
        // TrackToMCParticleRelationsDriver/TrackTruthMatching with a much looser "any hit at
        // all" rule, no purity requirement) - used below only as a cross-check on "nohits"
        // tracks, to see whether the full production chain also loses the match or finds one.
        List<LCRelation> kalmanTrackToMCPRelations = event.hasCollection(LCRelation.class, "KalmanFullTracksToMCParticleRelations")
                ? event.get(LCRelation.class, "KalmanFullTracksToMCParticleRelations") : null;

        // Truth-only Kalman refit track (see AprimeElectronHitFilterDriver/setter above):
        // at most one is expected per event, since the filter driver only ever keeps clusters
        // truth-matched to the single true A' electron; if more than one somehow shows up,
        // fall back to the one with the most hits and warn.
        Track apEleOnlyTrack = null;
        if (event.hasCollection(Track.class, apEleOnlyTrackCollectionName)) {
            List<Track> apEleOnlyTracks = event.get(Track.class, apEleOnlyTrackCollectionName);
            for (Track t : apEleOnlyTracks)
                if (apEleOnlyTrack == null || t.getTrackerHits().size() > apEleOnlyTrack.getTrackerHits().size())
                    apEleOnlyTrack = t;
            if (apEleOnlyTracks.size() > 1)
                System.out.println("warning: " + apEleOnlyTrackCollectionName + " has " + apEleOnlyTracks.size()
                        + " tracks in event " + event.getEventNumber() + "; using the one with the most hits");
        }
        if (enablePlots) {
            MCParticle aprimeEleForRefit = findAprimeElectron(event.getMCParticles());
            if (aprimeEleForRefit != null) {
                int nMatchedLayers = countSimHitLayers(aprimeEleForRefit, allsimhits);
                plots1D.get("apeleonly_nmatched_layers").fill(nMatchedLayers);
                if (apEleOnlyTrack != null) {
                    plots1D.get("apeleonly_refit_found").fill(1);
                    double apEleOnlyP = trackMomentumMag(apEleOnlyTrack);
                    Hep3Vector truthP = aprimeEleForRefit.getMomentum();
                    Hep3Vector recoPDet = CoordinateTransformations.transformVectorToDetector(
                            new BasicHep3Vector(apEleOnlyTrack.getTrackStates().get(0).getMomentum()));
                    plots1D.get("apeleonly_chi2ndf").fill(apEleOnlyTrack.getChi2() / apEleOnlyTrack.getNDF());
                    plots2D.get("apeleonly_p_vs_truth_p").fill(apEleOnlyP, truthP.magnitude());
                    plots2D.get("apeleonly_px_vs_truth_px").fill(recoPDet.x(), truthP.x());
                    plots2D.get("apeleonly_py_vs_truth_py").fill(recoPDet.y(), truthP.y());
                } else if (nMatchedLayers >= MIN_FINDABLE_LAYERS)
                    plots1D.get("apeleonly_refit_found").fill(0);
            }
        }

        int nV0Pass = 0;
        for (ReconstructedParticle v0 : v0Candidates) {
            nV0Candidates++;

            ReconstructedParticle eleParticle = null;
            ReconstructedParticle posParticle = null;
            for (ReconstructedParticle daughter : v0.getParticles()) {
                if (daughter.getCharge() < 0)
                    eleParticle = daughter;
                else if (daughter.getCharge() > 0)
                    posParticle = daughter;
            }
            if (eleParticle == null || posParticle == null)
                continue;

            boolean posHasCluster = !posParticle.getClusters().isEmpty();
            boolean posHasTrack = !posParticle.getTracks().isEmpty();
            boolean eleHasTrack = !eleParticle.getTracks().isEmpty();
            if (!(posHasTrack && posHasCluster && eleHasTrack))
                continue;
            passCut(0);

            Track eleTrack = eleParticle.getTracks().get(0);
            Track posTrack = posParticle.getTracks().get(0);
            Cluster posCluster = posParticle.getClusters().get(0);

            double posClusterE = posCluster.getEnergy();
            if (!(posClusterE >= POS_CLUSTER_E_MIN))
                continue;
            passCut(1);

            double eleP = trackMomentumMag(eleTrack);
            if (!(eleP >= ELE_P_MIN && eleP <= ELE_P_MAX))
                continue;
            passCut(2);

            double posP = trackMomentumMag(posTrack);
            if (!(posP >= POS_P_MIN))
                continue;
            passCut(3);

            double eleChi2Ndf = eleTrack.getChi2() / eleTrack.getNDF();
            if (!(eleChi2Ndf <= CHI2_NDF_MAX))
                continue;
            passCut(4);

            double posChi2Ndf = posTrack.getChi2() / posTrack.getNDF();
            if (!(posChi2Ndf <= CHI2_NDF_MAX))
                continue;
            passCut(5);

            int eleNHits = eleTrack.getTrackerHits().size();
            if (!(eleNHits >= MIN_HITS))
                continue;
            passCut(6);

            int posNHits = posTrack.getTrackerHits().size();
            if (!(posNHits >= MIN_HITS))
                continue;
            passCut(7);

            double posClusterTime = ClusterUtilities.getSeedHitTime(posCluster);
            double eleTrackTime = TrackData.getTrackTime(TrackData.getTrackData(event, eleTrack));
            double posTrackTime = TrackData.getTrackTime(TrackData.getTrackData(event, posTrack));

            if (enablePlots) {
                plots1D.get("raw_ele_cluster_dt").fill(eleTrackTime - posClusterTime);
                plots1D.get("raw_pos_cluster_dt").fill(posTrackTime - posClusterTime);
                plots1D.get("raw_ele_pos_dt").fill(eleTrackTime - posTrackTime);
            }

            if (!(Math.abs(eleTrackTime - posClusterTime - TRACK_CLUSTER_TIME_OFFSET) <= ELE_CLUSTER_TIME_MAX))
                continue;
            passCut(8);

            if (!(Math.abs(posTrackTime - posClusterTime - TRACK_CLUSTER_TIME_OFFSET) <= POS_CLUSTER_TIME_MAX))
                continue;
            passCut(9);

            if (!(Math.abs(eleTrackTime - posTrackTime) <= ELE_POS_TRACK_TIME_MAX))
                continue;
            passCut(10);

            Vertex vtx = v0.getStartVertex();
            if (vtx == null || !(vtx.getChi2() <= VERTEX_CHI2_MAX))
                continue;
            passCut(11);

            double[] pe = eleTrack.getTrackStates().get(0).getMomentum();
            double[] pp = posTrack.getTrackStates().get(0).getMomentum();
            double psum = Math.sqrt(Math.pow(pe[0] + pp[0], 2) + Math.pow(pe[1] + pp[1], 2) + Math.pow(pe[2] + pp[2], 2));
            if (!(psum <= VTX_MAX_P))
                continue;
            passCut(12);
            nV0Pass++;

            double eEle = Math.sqrt(pe[0] * pe[0] + pe[1] * pe[1] + pe[2] * pe[2] + ELECTRON_MASS * ELECTRON_MASS);
            double ePos = Math.sqrt(pp[0] * pp[0] + pp[1] * pp[1] + pp[2] * pp[2] + ELECTRON_MASS * ELECTRON_MASS);
            double esum = eEle + ePos;
            double v0Mass2 = esum * esum - psum * psum;
            double v0Mass = v0Mass2 > 0 ? Math.sqrt(v0Mass2) : -1;

            // Preselection passed - run truth matching on both daughter tracks
            TrackTruthMatcher eleTT = new TrackTruthMatcher(eleTrack, rawtomc, 0.0, 0);
            TrackTruthMatcher posTT = new TrackTruthMatcher(posTrack, rawtomc, 0.0, 0);
            String v0Cat = catLabel(eleTT);
            String posCat = catLabel(posTT);
            List<String> eleCats = new ArrayList<String>();
            eleCats.add(v0Cat);
            if ((v0Cat.equals("unmatched") || v0Cat.equals("nohits"))
                    && isKinMatch(eleTrack, eleTT, allsimhits, event.getMCParticles()))
                eleCats.add(v0Cat + "_kinmatch");
            matchAndFill("ele", eleTrack, eleTT, allsimhits, eleCats, thisEventNum, event.getEventNumber(), kalmanTrackToMCPRelations, v0Mass, event.getMCParticles(), allTracks, rawtomc, event);
            matchAndFill("pos", posTrack, posTT, allsimhits, eleCats, thisEventNum, event.getEventNumber(), kalmanTrackToMCPRelations, v0Mass, event.getMCParticles(), allTracks, rawtomc, event);

            if (enablePlots) {
                plots2D.get("ele_pos_category").fill(categoryIndex(v0Cat), categoryIndex(posCat));
                fillV0Plots(vtx, pe, pp, eleTT, posTT, eleCats);
                if (apEleOnlyTrack != null)
                    for (String cat : eleCats)
                        plots2D.get("apeleonly_p_vs_standard_p_" + cat).fill(trackMomentumMag(apEleOnlyTrack), trackMomentumMag(eleTrack));
            }

            if (v0Cat.equals("unmatched") || v0Cat.equals("nohits"))
                System.out.println("[" + v0Cat + "] eventNum=" + thisEventNum + " lcioEventNumber=" + event.getEventNumber()
                        + " ele category=" + v0Cat + " pos category=" + posCat);
        }

        if (enablePlots)
            plots1D.get("n_v0_pass_preselection").fill(nV0Pass);
    }

    // Category label ("aprime"/"recoil"/"unmatched"/"nohits") used to split the V0
    // and per-track histograms, based on the electron's truth match.
    private String catLabel(TrackTruthMatcher tt) {
        if (tt.getPurity() == 0.0)
            return "nohits";
        MCParticle mcp = tt.getMCParticle();
        if (mcp == null || tt.getPurity() < purityCut)
            return "unmatched";
        return isAprimeDaughter(mcp) ? "aprime" : "recoil";
    }

    // Index of a category label within CATEGORIES, for use as a histogram bin.
    private int categoryIndex(String cat) {
        for (int i = 0; i < CATEGORIES.length; i++)
            if (CATEGORIES[i].equals(cat))
                return i;
        return -1;
    }

    private void fillV0Plots(Vertex vtx, double[] pe, double[] pp, TrackTruthMatcher eleTT, TrackTruthMatcher posTT, List<String> cats) {
        double eEle = Math.sqrt(pe[0] * pe[0] + pe[1] * pe[1] + pe[2] * pe[2] + ELECTRON_MASS * ELECTRON_MASS);
        double ePos = Math.sqrt(pp[0] * pp[0] + pp[1] * pp[1] + pp[2] * pp[2] + ELECTRON_MASS * ELECTRON_MASS);
        double esum = eEle + ePos;
        double psum2 = Math.pow(pe[0] + pp[0], 2) + Math.pow(pe[1] + pp[1], 2) + Math.pow(pe[2] + pp[2], 2);
        double mass2 = esum * esum - psum2;
        double mass = mass2 > 0 ? Math.sqrt(mass2) : -1;

        plots1D.get("v0_chi2").fill(vtx.getChi2());
        plots1D.get("v0_psum").fill(Math.sqrt(psum2));
        if (mass >= 0)
            plots1D.get("v0_mass").fill(mass);

        Hep3Vector vtxPos = vtx.getPosition();
        plots1D.get("v0_vtx_x").fill(vtxPos.x());
        plots1D.get("v0_vtx_y").fill(vtxPos.y());
        plots1D.get("v0_vtx_z").fill(vtxPos.z());

        MCParticle eleMCP = eleTT.getMCParticle();
        MCParticle posMCP = posTT.getMCParticle();
        MCParticle truthMCP = eleMCP != null ? eleMCP : posMCP;
        Hep3Vector truePos = truthMCP != null ? truthMCP.getOrigin() : null;
        if (truePos != null) {
            plots1D.get("v0_true_vtx_x").fill(truePos.x());
            plots1D.get("v0_true_vtx_y").fill(truePos.y());
            plots1D.get("v0_true_vtx_z").fill(truePos.z());
            plots1D.get("v0_vtx_z_res").fill(vtxPos.z() - truePos.z());
        }

        for (String cat : cats) {
            plots1D.get("v0_chi2_" + cat).fill(vtx.getChi2());
            plots1D.get("v0_psum_" + cat).fill(Math.sqrt(psum2));
            if (mass >= 0)
                plots1D.get("v0_mass_" + cat).fill(mass);
            plots1D.get("v0_vtx_x_" + cat).fill(vtxPos.x());
            plots1D.get("v0_vtx_y_" + cat).fill(vtxPos.y());
            plots1D.get("v0_vtx_z_" + cat).fill(vtxPos.z());
            if (truePos != null) {
                plots1D.get("v0_true_vtx_x_" + cat).fill(truePos.x());
                plots1D.get("v0_true_vtx_y_" + cat).fill(truePos.y());
                plots1D.get("v0_true_vtx_z_" + cat).fill(truePos.z());
                plots1D.get("v0_vtx_z_res_" + cat).fill(vtxPos.z() - truePos.z());
            }
        }
    }

    private void matchAndFill(String chTag, Track track, TrackTruthMatcher tt, List<SimTrackerHit> allsimhits, List<String> cats, int eventNum, int lcioEventNumber, List<LCRelation> kalmanTrackToMCPRelations, double v0Mass, List<MCParticle> mcParticles, List<Track> allTracks, RelationalTable rawtomc, EventHeader event) {
        if (!enablePlots)
            return;

        double pMag = trackMomentumMag(track);
        plots1D.get(chTag + "_reco_track_momentum").fill(pMag);
        plots1D.get(chTag + "_purity").fill(tt.getPurity());

        double chi2Ndf = track.getChi2() / track.getNDF();
        int nHits = track.getTrackerHits().size();
        org.lcsim.event.TrackState ts = track.getTrackStates().get(0);
        double d0 = ts.getParameter(ParameterName.d0.ordinal());
        double z0 = ts.getParameter(ParameterName.z0.ordinal());
        double phi0 = ts.getParameter(ParameterName.phi0.ordinal());
        double omega = ts.getParameter(ParameterName.omega.ordinal());
        double tanLambda = ts.getParameter(ParameterName.tanLambda.ordinal());
        // TrackState.getMomentum() is in tracking frame (x=beam, y=bend-plane,
        // z=vertical); rotate to the global/detector frame (z=beam) so px/py and
        // px/p, py/p are reported in the same convention as MCParticle.getMomentum().
        Hep3Vector recoPDet = CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(ts.getMomentum()));
        double pxop = pMag > 0 ? recoPDet.x() / pMag : 0.0;
        double pyop = pMag > 0 ? recoPDet.y() / pMag : 0.0;

        plots1D.get(chTag + "_chi2Ndf").fill(chi2Ndf);
        plots1D.get(chTag + "_nHits").fill(nHits);
        plots1D.get(chTag + "_d0").fill(d0);
        plots1D.get(chTag + "_z0").fill(z0);
        plots1D.get(chTag + "_phi0").fill(phi0);
        plots1D.get(chTag + "_omega").fill(omega);
        plots1D.get(chTag + "_tanLambda").fill(tanLambda);
        plots1D.get(chTag + "_pxop").fill(pxop);
        plots1D.get(chTag + "_pyop").fill(pyop);

        for (String cat : cats) {
            plots1D.get(chTag + "_reco_track_momentum_" + cat).fill(pMag);
            plots1D.get(chTag + "_chi2Ndf_" + cat).fill(chi2Ndf);
            plots1D.get(chTag + "_nHits_" + cat).fill(nHits);
            plots1D.get(chTag + "_d0_" + cat).fill(d0);
            plots1D.get(chTag + "_z0_" + cat).fill(z0);
            plots1D.get(chTag + "_phi0_" + cat).fill(phi0);
            plots1D.get(chTag + "_omega_" + cat).fill(omega);
            plots1D.get(chTag + "_tanLambda_" + cat).fill(tanLambda);
            plots1D.get(chTag + "_pxop_" + cat).fill(pxop);
            plots1D.get(chTag + "_pyop_" + cat).fill(pyop);
        }

        if (chTag.equals("ele")) {
            GenericObject eleTrackData = TrackData.getTrackData(event, track);
            if (eleTrackData != null)
                for (int layer = 0; layer < NLAYERS; layer++) {
                    double iso = TrackData.getIsolation(eleTrackData, layer);
                    plots1D.get("ele_isolation_layer" + layer).fill(iso);
                    if (iso == 0.0)
                        plots1D.get("ele_isolation_zero_count").fill(layer);
                }

            MCParticle aprimeEle = findAprimeElectron(mcParticles);
            if (aprimeEle != null) {
                // MCParticle.getMomentum() is already in the global/detector frame
                // (z=beam), matching recoPDet computed above - no rotation needed here.
                Hep3Vector truthP = aprimeEle.getMomentum();
                int nAprimeEleSimHits = tt.getLayersHitByMCP(aprimeEle, allsimhits).size();
                int nStolenClusters = countStolenAprimeEleClusters(track, aprimeEle, allTracks, rawtomc);
                for (String cat : cats) {
                    plots2D.get("aprime_ele_truth_vs_reco_p_" + cat).fill(truthP.magnitude(), pMag);
                    plots2D.get("aprime_ele_truth_vs_reco_px_" + cat).fill(truthP.x(), recoPDet.x());
                    plots2D.get("aprime_ele_truth_vs_reco_py_" + cat).fill(truthP.y(), recoPDet.y());
                    plots2D.get("aprime_ele_nsimhits_vs_stolen_clusters_" + cat).fill(nAprimeEleSimHits, nStolenClusters);
                }
            }
        }

        MCParticle matchedMCP = tt.getMCParticle();
        int truthCategory;
        if (tt.getPurity() == 0.0)
            truthCategory = CAT_NOHITS;
        else if (matchedMCP == null || tt.getPurity() < purityCut)
            truthCategory = CAT_UNMATCHED;
        else if (isAprimeDaughter(matchedMCP))
            truthCategory = CAT_APRIME_DAUGHTER;
        else
            truthCategory = CAT_OTHER;
        plots1D.get(chTag + "_truth_category").fill(truthCategory);

        if (chTag.equals("ele") && truthCategory == CAT_OTHER)
            plots1D.get("ele_other_parent_pdgid").fill(getParentPDGID(matchedMCP));

        if (chTag.equals("ele") && (truthCategory == CAT_UNMATCHED || truthCategory == CAT_NOHITS)) {
            String catTag = (truthCategory == CAT_NOHITS ? "nohits" : "unmatched");
            java.util.Set<Integer> trackLayers = tt.getLayersOnTrack();
            System.out.println("[" + catTag + "] eventNum=" + eventNum + " lcioEventNumber=" + lcioEventNumber
                    + " ele track with " + trackLayers.size() + " hits on track, p=" + trackMomentumMag(track)
                    + " GeV, v0Mass=" + v0Mass + " GeV:");
            MCParticle aprimeEle = findAprimeElectron(mcParticles);
            // Fill layer-by-layer correct/wrong hit plots for "unmatched" (0 < purity <
            // purityCut) tracks that are also kinematically consistent with the true A'
            // electron (see isKinMatch()/cats.contains("unmatched_kinmatch")): "correct" if
            // the true A' electron contributed a hit on that layer, "wrong" otherwise - i.e.
            // did the truth-hit chain actually break, and on which layers.
            boolean fillKinMatchLayers = cats.contains("unmatched_kinmatch") && aprimeEle != null;
            if (cats.contains("unmatched_kinmatch"))
                for (Integer layer : trackLayers)
                    plots1D.get("ele_hit_layers_unmatched_kinmatch").fill(layer);
            int nNoMCP = 0;
            for (Integer layer : trackLayers) {
                java.util.Set<MCParticle> layerMcps = tt.getMCPsOnLayer(layer);
                if (layerMcps == null || layerMcps.isEmpty()) {
                    nNoMCP++;
                    System.out.println("    layer=" + layer + " (no MCP assigned)");
                    if (fillKinMatchLayers)
                        plots1D.get("ele_wrong_hit_layers_unmatched_kinmatch").fill(layer);
                    continue;
                }
                if (fillKinMatchLayers) {
                    if (layerMcps.contains(aprimeEle))
                        plots1D.get("ele_correct_hit_layers_unmatched_kinmatch").fill(layer);
                    else
                        plots1D.get("ele_wrong_hit_layers_unmatched_kinmatch").fill(layer);
                }
                for (MCParticle layerMcp : layerMcps) {
                    int parentPDG = getParentPDGID(layerMcp);
                    if (truthCategory == CAT_UNMATCHED)
                        plots1D.get("ele_unmatched_hit_parent_pdgid").fill(parentPDG);
                    System.out.println("    layer=" + layer + " hitMCP_pdgid=" + layerMcp.getPDGID()
                            + " parentPDGID=" + parentPDG);
                }
            }
            System.out.println("[" + catTag + "] " + nNoMCP + " of " + trackLayers.size()
                    + " hits on track had no MCP assigned");

            if (truthCategory == CAT_NOHITS) {
                MCParticle prodMatch = findProductionMatch(track, kalmanTrackToMCPRelations);
                if (prodMatch != null)
                    System.out.println("[nohits] eventNum=" + eventNum + " lcioEventNumber=" + lcioEventNumber
                            + " KalmanFullTracksToMCParticleRelations DOES have a match: pdgid=" + prodMatch.getPDGID()
                            + " parentPDGID=" + getParentPDGID(prodMatch));
                else
                    System.out.println("[nohits] eventNum=" + eventNum + " lcioEventNumber=" + lcioEventNumber
                            + " KalmanFullTracksToMCParticleRelations agrees: no match found either");

                if (aprimeEle != null) {
                    // Rotate into tracking frame (x=beam, y=bend-plane, z=vertical) so these
                    // components are directly comparable to TrackState.getMomentum().
                    hep.physics.vec.Hep3Vector p = CoordinateTransformations.transformVectorToTracking(aprimeEle.getMomentum());
                    System.out.println("[nohits] eventNum=" + eventNum + " lcioEventNumber=" + lcioEventNumber
                            + " A' electron truth momentum (tracking frame): px=" + p.x() + " py=" + p.y() + " pz=" + p.z() + " GeV");

                    int nAprimeEleLayers = tt.getLayersHitByMCP(aprimeEle, allsimhits).size();
                    boolean findable = nAprimeEleLayers >= MIN_FINDABLE_LAYERS;
                    System.out.println("[nohits] eventNum=" + eventNum + " lcioEventNumber=" + lcioEventNumber
                            + " A' electron is " + (findable ? "FINDABLE" : "NOT FINDABLE")
                            + " (has SimTrackerHits in " + nAprimeEleLayers + " layers)");
                } else
                    System.out.println("[nohits] eventNum=" + eventNum + " lcioEventNumber=" + lcioEventNumber
                            + " no A' electron found in MCParticle list");
            }
        }

        if (tt.getPurity() >= purityCut) {
            for (Integer layer : tt.getGoodHitLayers())
                plots1D.get(chTag + "_good_hit_layers").fill(layer);
            for (Integer layer : tt.getBadHitLayers())
                plots1D.get(chTag + "_bad_hit_layers").fill(layer);
            for (Integer layer : tt.getWrongHitLayers(allsimhits))
                plots1D.get(chTag + "_wrong_hit_layers").fill(layer);
            for (Integer layer : tt.getNonMCPHitLayers(allsimhits))
                plots1D.get(chTag + "_nonmcp_hit_layers").fill(layer);
            for (Integer layer : tt.getMissedHitLayers(allsimhits))
                plots1D.get(chTag + "_missed_hit_layers").fill(layer);
        }

        for (Integer layer : tt.getLayersOnTrack()) {
            java.util.Set<MCParticle> mcps = tt.getMCPsOnLayer(layer);
            if (mcps == null)
                continue;
            for (int k = 0; k < mcps.size(); k++)
                plots1D.get(chTag + "_n_mcps_on_layer").fill(layer);
        }
    }

    // Looks up this track's match in production's own track-to-MCParticle relation
    // (e.g. KalmanFullTracksToMCParticleRelations), by object identity, or null if
    // that collection is absent or has no entry for this track.
    private MCParticle findProductionMatch(Track track, List<LCRelation> relations) {
        if (relations == null)
            return null;
        for (LCRelation rel : relations)
            if (rel != null && rel.getFrom() == track)
                return (MCParticle) rel.getTo();
        return null;
    }

    // PDGID of the MCParticle's immediate parent, or 0 (not a real PDGID) if it
    // has no parent (i.e. it's a primary/generator-level particle).
    private int getParentPDGID(MCParticle mcp) {
        List<MCParticle> parents = mcp.getParents();
        if (parents.isEmpty())
            return 0;
        return parents.get(0).getPDGID();
    }

    // Walks the MCParticle's parent chain to see if any ancestor is the A' (622).
    // Distinguishes an A'-decay-daughter electron/positron (or a delta-ray/secondary
    // descended from one) from an unrelated electron, e.g. the beam recoil electron.
    private boolean isAprimeDaughter(MCParticle mcp) {
        MCParticle cur = mcp;
        while (!cur.getParents().isEmpty()) {
            MCParticle parent = cur.getParents().get(0);
            if (parent.getPDGID() == APRIME_PDGID)
                return true;
            cur = parent;
        }
        return false;
    }

    // Finds the generator-level electron (PDGID 11) whose immediate parent is the
    // A' (622) in the event's MCParticle list, or null if there is none.
    private MCParticle findAprimeElectron(List<MCParticle> mcParticles) {
        for (MCParticle mcp : mcParticles)
            if (mcp.getPDGID() == 11 && !mcp.getParents().isEmpty()
                    && mcp.getParents().get(0).getPDGID() == APRIME_PDGID)
                return mcp;
        return null;
    }

    private double trackMomentumMag(Track track) {
        return new BasicHep3Vector(track.getTrackStates().get(0).getMomentum()).magnitude();
    }

    // Number of distinct layers with a SimTrackerHit belonging to the given MCParticle.
    // Equivalent to TrackTruthMatcher.getLayersHitByMCP(mcp, simhits).size(), duplicated
    // here since this needs to be evaluated independent of any particular reco track.
    private int countSimHitLayers(MCParticle mcp, List<SimTrackerHit> simhits) {
        java.util.Set<Integer> layers = new java.util.HashSet<Integer>();
        for (SimTrackerHit simhit : simhits)
            if (simhit.getMCParticle() == mcp)
                layers.add(simhit.getLayer());
        return layers.size();
    }

    // Counts layer-clusters on OTHER tracks in the event (anything but eleTrack itself)
    // that have a contribution from the true A' electron - i.e. hits that "belong" to the
    // A' electron but ended up clustered onto a different track's TrackerHit instead.
    // Summed over every other track, so a single stolen layer shared by two other tracks
    // counts twice (each occurrence is a separate lost hit for the A' electron's track).
    private int countStolenAprimeEleClusters(Track eleTrack, MCParticle aprimeEle, List<Track> allTracks, RelationalTable rawtomc) {
        int nStolen = 0;
        for (Track other : allTracks) {
            if (other == eleTrack)
                continue;
            TrackTruthMatcher otherTT = new TrackTruthMatcher(other, rawtomc);
            java.util.Set<Integer> aprimeLayers = otherTT.getLayerHitsForAllMCPs().get(aprimeEle);
            if (aprimeLayers != null)
                nStolen += aprimeLayers.size();
        }
        return nStolen;
    }

    // Loose kinematic-compatibility check between the V0 electron's reconstructed track
    // and the true A' electron MCParticle: the true A' electron must be findable (hits in
    // at least MIN_FINDABLE_LAYERS layers), and the reco track must have the same sign of
    // py/pz and total momentum within kinMatchMomentumFrac of the truth momentum. Mirrors
    // trace_track_truth.py's track_aprime_summary() "kinematic_match" (used there to decide
    // which unmatched/nohits tracks are worth a manual per-layer drill-down), plus the
    // findability requirement already used as a diagnostic further down in matchAndFill();
    // here it drives the "_kinmatch" sub-categories.
    private boolean isKinMatch(Track eleTrack, TrackTruthMatcher tt, List<SimTrackerHit> allsimhits, List<MCParticle> mcParticles) {
        MCParticle aprimeEle = findAprimeElectron(mcParticles);
        if (aprimeEle == null)
            return false;
        if (tt.getLayersHitByMCP(aprimeEle, allsimhits).size() < MIN_FINDABLE_LAYERS)
            return false;
        // Rotate into tracking frame (x=beam, y=bend-plane, z=vertical) so components are
        // directly comparable to TrackState.getMomentum(), same as elsewhere in this class.
        Hep3Vector truthP = CoordinateTransformations.transformVectorToTracking(aprimeEle.getMomentum());
        double[] recoP = eleTrack.getTrackStates().get(0).getMomentum();

        if (recoP[2] == 0.0 || truthP.z() == 0.0)
            return false;
        boolean sameSignPyPz = Math.signum(recoP[1] / recoP[2]) == Math.signum(truthP.y() / truthP.z());

        double pTruth = truthP.magnitude();
        if (!(pTruth > 0.0))
            return false;
        boolean momentumClose = Math.abs(trackMomentumMag(eleTrack) - pTruth) / pTruth <= kinMatchMomentumFrac;

        return sameSignPyPz && momentumClose;
    }

    private void passCut(int cutIndex) {
        nPassCut[cutIndex]++;
        if (enablePlots)
            plots1D.get("cutflow").fill(cutIndex);
    }

    @Override
    public void endOfData() {
        System.out.println("=== Preselection2021 summary ===");
        System.out.println("Total V0 candidates seen: " + nV0Candidates);
        for (int i = 0; i < CUT_NAMES.length; i++) {
            System.out.println(String.format("  survive after %-16s : %d", CUT_NAMES[i], nPassCut[i]));
        }
    }
}
