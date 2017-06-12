package org.hps.recon.tracking;

import java.util.Iterator;
import java.util.List;

import org.lcsim.event.Track;
import org.lcsim.math.chisq.ChisqProb;

/**
 * Only performs: removal of duplicates, removal of partials, removal of shared
 * tracks based on number of shared-hits, removal of poor scoring tracks. No
 * consideration of holes, bad channels, acceptance, nor hit times. No track
 * cleaning. Scoring based on chi-squared/dof.
 *
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 */

public class SimpleAmbiguityResolver extends AmbiguityResolver {
    public enum AmbiMode {
        DUPS, PARTIALS, SHARED, POORSCORE, FULL;
    }

    private AmbiMode mode;
    private int shareThreshold;
    private double scoreThreshold;

    /**
     * Constructor
     *
     * @param inputTracks
     *            : all track collections
     * @param mode
     *            : remove duplicates, remove partials, remove shared, remove
     *            poor scoring tracks, all the above
     * @param shareThreshold
     * @param scoreThreshold
     */
    public SimpleAmbiguityResolver(List<List<Track>> inputTracks, AmbiMode mode, int share, double score) {
        super();
        initializeFromCollection(inputTracks);
        setMode(mode);
        setShareThreshold(share);
        setScoreThreshold(score);
    }

    /**
     * Default constructor
     */
    public SimpleAmbiguityResolver() {
        super();
        setMode(AmbiMode.FULL);
        setShareThreshold(4);
        setScoreThreshold(1.0);
    }

    // Setters, getters
    /**
     * @param shareThreshold
     *            : max number of shared hits that any operable tracks can share
     *            with each other
     */
    public void setShareThreshold(int value) {
        this.shareThreshold = value;
    }

    /**
     * @param scoreThreshold
     *            : min acceptable score for a track
     */
    public void setScoreThreshold(double value) {
        this.scoreThreshold = value;
    }

    /**
     * @return shareThreshold : max number of shared hits that any operable
     *         tracks can share with each other
     */
    public int getShareThreshold() {
        return this.shareThreshold;
    }

    /**
     * @return scoreThreshold : min acceptable score
     */
    public double getScoreThreshold() {
        return this.scoreThreshold;
    }

    /**
     * @param mode
     *            : remove duplicates, remove partials, remove shared, advanced
     *            ATLAS-style
     */
    public void setMode(AmbiMode mode) {
        this.mode = mode;
    }

    /**
     * @return mode : remove duplicates, remove partials, remove shared
     */
    public AmbiMode getMode() {
        return this.mode;
    }

    /**
     * @override
     *
     */
    protected boolean areShared(Track trk1, Track trk2) {
        if (TrackUtils.numberOfSharedHits(trk1, trk2) > this.shareThreshold)
            return true;
        return false;
    }

    /**
     * @override
     */
    public double scoreTrack(Track trk) {
        double cumProb = ChisqProb.gammq(trk.getNDF(), trk.getChi2());

        return cumProb;
    }

    /**
     * Remove tracks with poor scores from operable tracks list, put them in
     * poorScores list
     */
    protected void RemovePoorScores() {

        for (Iterator<Track> iterator = this.tracks.iterator(); iterator.hasNext();) {
            Track trk = iterator.next();
            if (trackScoreMap.get(trk)[0] < this.scoreThreshold) {
                iterator.remove();
                this.poorScore.add(trk);
            }
        }

    }

    /**
     * @override
     */
    public void resolve() {
        switch (this.mode) {
        case FULL:
            utils.RemoveDuplicates();
            utils.RemovePartials();
            utils.RemoveShared();
            RemovePoorScores();
            break;
        case DUPS:
            utils.RemoveDuplicates();
            break;
        case PARTIALS:
            utils.RemovePartials();
            break;
        case SHARED:
            utils.RemoveShared();
            break;
        case POORSCORE:
            RemovePoorScores();
            break;
        }
    }

}
