package org.hps.recon.tracking;

import java.util.Iterator;
import java.util.List;

import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;

/**
 * 
 * @author Miriam Diamond <mdiamond@slac.stanford.edu> $Id:
 *         ClassicAmbiguityResolver.java, v1 05/30/2017$ Performs: removal of
 *         duplicates, removal of partials, removal of shared tracks based on
 *         number of shared-hits, removal of poor scoring tracks. Track scoring
 *         considers holes, bad channels, layer acceptances. No track cleaning.
 */

public class ClassicAmbiguityResolver extends AmbiguityResolver {
    private int _shareThreshold;
    private double _scoreThreshold;
    private double[] sharedHitScore = { 10, 10, 10, 10, 10, 10 };
    private double[] unsharedHitScore = { 20, 20, 20, 20, 20, 20 };
    private double[] holePenalty = { 10, 10, 10, 10, 10, 10 };
    private double[] outsideAcceptancePenalty = { 5, 0, 0, 0, 0, 0 };
    private double badChisqPenalty;
    private double ChisqScoring;
    private double cumProbThreshold;
    private boolean doChargeCheck;
    private AcceptanceHelper acc;

    /**
     * Constructor with AcceptanceHelper (used in track scoring). Defaults:
     * score threshold 50, share threshold 0, cumProb threshold 0.95, chi2
     * scoring factor 2.0, bad chi2 penalty 30, doChargeCheck false
     * 
     * @param AcceptanceHelper
     */
    public ClassicAmbiguityResolver(AcceptanceHelper helper) {
        super();
        acc = helper;
        setScoreThreshold(50);
        setShareThreshold(0);
        setCumProbThreshold(0.95);
        setChi2Scoring(2.0);
        setBadChi2Penalty(30);
        setDoChargeCheck(false);
    }

    /**
     * @param cumProbThreshold
     *            : max cumulative probability threshold for chi2 in scoring,
     *            default 0.95
     */
    void setCumProbThreshold(double threshold) {
        cumProbThreshold = threshold;
    }

    /**
     * @param factor
     *            : weighting factor for chi2 in track scoring, default 2.0
     */
    void setChi2Scoring(double factor) {
        ChisqScoring = factor;
    }

    /**
     * @param scoreThreshold
     *            : min acceptable score for a track
     */
    public void setScoreThreshold(double score) {
        _scoreThreshold = score;
    }

    /**
     * @param hitScores
     *            : for track-scoring. Entry i of array = value added to score
     *            for having a shared hit in layer i+1
     */
    public void setSharedHitScores(double[] scores) {
        if (scores.length == 6)
            sharedHitScore = scores;
    }

    /**
     * @param hitScores
     *            : for track-scoring. Entry i of array = value added to score
     *            for having an unshared hit in layer i+1
     */
    public void setUnsharedHitScores(double[] scores) {
        if (scores.length == 6)
            unsharedHitScore = scores;
    }

    /**
     * @param penalties
     *            : for track-scoring. Entry i of array = value subtracted from
     *            score for missing the acceptance of layer i+1
     */
    public void setOutsideAcceptancePenalties(double[] penalties) {
        if (penalties.length == 6)
            outsideAcceptancePenalty = penalties;
    }

    /**
     * @param penalties
     *            : for track-scoring. Entry i of array = value subtracted from
     *            score for having a hole in layer i+1
     */
    public void setHolePenalties(double[] penalties) {
        if (penalties.length == 6)
            holePenalty = penalties;
    }

    /**
     * @param penalty
     *            : for track-scoring. Value subtracted from score for having a
     *            bad overall chi2/dof
     */
    public void setBadChi2Penalty(double penalty) {
        badChisqPenalty = penalty;
    }

    /**
     * @param shareThreshold
     *            : max number of shared hits that any operable tracks can share
     *            with each other
     */
    public void setShareThreshold(int value) {
        _shareThreshold = value;
    }

    /**
     * 
     * @param yes
     *            /no : only consider tracks shared if they are same charge
     */
    public void setDoChargeCheck(boolean value) {
        doChargeCheck = value;
    }

    private boolean areSameCharge(Track trk1, Track trk2) {
        return (TrackUtils.getCharge(trk1) == TrackUtils.getCharge(trk2));
    }

    /**
     * @override
     *
     */
    protected boolean areShared(Track trk1, Track trk2) {
        if (TrackUtils.numberOfSharedHits(trk1, trk2) > _shareThreshold) {
            if ((!doChargeCheck) || (areSameCharge(trk1, trk2))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @override
     */
    public void resolve() {
        utils.RemoveDuplicates();
        // System.out.printf("Number of tracks after duplicate removal %d \n",
        // _tracks.size());
        utils.RemoveShared();
        // System.out.printf("Number of tracks after shared removal %d \n",
        // _tracks.size());
        utils.RemovePartials();
        // System.out.printf("Number of tracks after partials removal %d \n",
        // _tracks.size());
        RemovePoorScores();
        // System.out.printf("Number of tracks after poor-score removal %d \n",
        // _tracks.size());
    }

    /**
     * Remove tracks with poor scores from operable tracks list, put them in
     * poorScores list
     */
    protected void RemovePoorScores() {

        for (Iterator<Track> iterator = _tracks.iterator(); iterator.hasNext();) {
            Track trk = iterator.next();
            if (trackScoreMap.get(trk)[0] < _scoreThreshold) {
                iterator.remove();
                _poorScore.add(trk);
            }
        }

    }

    /**
     * @override
     *
     */
    public double scoreTrack(Track track) {
        /*
         * bonuses for hits (unshared and shared), penalties for holes and
         * missing acceptances of layers
         * 
         * define p = 1.0 - CumulativeChiSquare(chisquared/dof) if (p>0): add
         * log10(p) to score. else: apply penalty to score
         */
        double score = 0;
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();

        int[] holes = holesOnTrack(track);

        for (TrackerHit hit : hitsOnTrack) {
            int layer = ((RawTrackerHit) hit.getRawHits().get(0))
                    .getLayerNumber();
            layer = (layer + 1) / 2;
            if (TrackUtils.isSharedHit(hit, _tracks))
                score += sharedHitScore[layer - 1];
            else
                score += unsharedHitScore[layer - 1];
        }

        for (int i = 0; i < holes.length; i++) {
            if (holes[i] == 1)
                score -= holePenalty[i];
            else if (holes[i] == 2)
                score -= outsideAcceptancePenalty[i];
        }

        double cumProb = ChisqProb.gammp(track.getNDF(), track.getChi2());
        // System.out.printf("cumProb %f log %f \n", cumProb,
        // Math.log10(cumProb));
        if (cumProb < cumProbThreshold)
            score -= ChisqScoring * Math.log10(cumProb);
        else
            score -= badChisqPenalty;

        return score;
    }

    /**
     * @override
     */
    public int[] holesOnTrack(Track trk) {
        int[] holes = { 1, 1, 1, 1, 1, 1 };
        // 0=hit, 1=hole, 2=outside acceptance

        List<TrackerHit> stereoHits = trk.getTrackerHits();

        for (TrackerHit stereoHit : stereoHits) {
            int layer = ((RawTrackerHit) stereoHit.getRawHits().get(0))
                    .getLayerNumber();
            layer = (layer + 1) / 2;
            holes[layer - 1] = 0;
        }

        // for layers with no hits: holes or outside-acceptance?
        for (int i = 0; i < holes.length; i++) {
            if (holes[i] == 0)
                continue;

            if (!acc.isWithinAcceptance(trk, i + 1))
                holes[i] = 2;
        }

        return holes;
    }
}
