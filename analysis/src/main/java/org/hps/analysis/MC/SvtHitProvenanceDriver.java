package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;

/**
 * Attributes the hits on each track to their origin, using the provenance relations
 * written by SvtDigitizationWithPulserDataMergingReadoutDriver.
 *
 * Under pulser overlay a large fraction of tracks carry no truth relation at all. Two
 * explanations survive the aggregate hit counts:
 *
 *   1. The tracks are fakes assembled from pulser data hits.
 *   2. The tracks follow a real trajectory but were built from pulser hits on strips
 *      neighbouring the ones the MC particle actually hit. Adjacent strips are ~55 um
 *      apart, which over a metre of lever arm is ~55 urad, so such a track would still
 *      point at the true particle to well inside a milliradian while carrying zero
 *      truth relations.
 *
 * These differ observably in whether the untruthed hits on a track sit next to channels
 * that did receive MC charge. That is what this driver measures, against the baseline
 * rate at which any pulser hit in the event happens to be near an MC channel -- with
 * high occupancy, adjacency alone proves nothing, so the comparison to the baseline is
 * the whole point.
 *
 * Requires writeHitOriginCollections=true on the digitization driver.
 */
public class SvtHitProvenanceDriver extends Driver {

    private String trackCollectionName = "KalmanFullTracks";
    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String truthRelationCollectionName = "SVTTrueHitRelations";
    private String pulserOriginCollectionName = "SVTHitOriginPulser";
    private String mcContribCollectionName = "SVTHitOriginMCContrib";

    /** Neighbour distances, in strips, at which adjacency is reported. */
    private static final int[] DISTANCES = { 1, 2, 3, 5, 10 };
    /** Bins for the minimum strip distance to an MC-contributing channel. */
    private static final int MAX_DIST_BIN = 12;

    private boolean debug = false;
    private int debugMaxPrint = 20;
    private int debugPrinted = 0;

    // Hit categories, matching the digitization driver.
    private static final int NOISE = 0;
    private static final int MC_PURE = 1;
    private static final int MC_PURE_SUBTHRESH = 2;
    private static final int PULSER_PURE = 3;
    private static final int MERGED = 4;
    private static final int MERGED_SUBTHRESH = 5;
    private static final String[] CAT_NAME = {
        "NOISE            ", "MC_PURE          ", "MC_PURE_SUBTHRESH",
        "PULSER_PURE      ", "MERGED           ", "MERGED_SUBTHRESH "
    };

    private long nEvents = 0;
    private long nTracks = 0;
    private long nZeroTruthTracks = 0;
    private boolean warnedMissing = false;

    // [0] = tracks with at least one truthed hit, [1] = tracks with none
    private final long[] nTracksByClass = new long[2];
    private final long[] nHitsByClass = new long[2];
    private final long[][] nHitsByClassAndCat = new long[2][6];

    // Minimum strip distance from a PULSER_PURE hit to an MC-contributing channel on the
    // same sensor. Index MAX_DIST_BIN is the overflow, MAX_DIST_BIN+1 means the sensor had
    // no MC-contributing channel at all.
    private final long[][] distHistByClass = new long[2][MAX_DIST_BIN + 2];
    private final long[] distHistBaseline = new long[MAX_DIST_BIN + 2];
    private long nPulserPureBaseline = 0;

    public void setTrackCollectionName(String val) { this.trackCollectionName = val; }
    public void setRawHitCollectionName(String val) { this.rawHitCollectionName = val; }
    public void setTruthRelationCollectionName(String val) { this.truthRelationCollectionName = val; }
    public void setPulserOriginCollectionName(String val) { this.pulserOriginCollectionName = val; }
    public void setMcContribCollectionName(String val) { this.mcContribCollectionName = val; }
    public void setDebug(boolean val) { this.debug = val; }
    public void setDebugMaxPrint(int val) { this.debugMaxPrint = val; }

    /** Collects the "from" side of a relation collection, tolerating a missing collection. */
    private Set<RawTrackerHit> fromSide(EventHeader event, String name) {
        Set<RawTrackerHit> out = new HashSet<RawTrackerHit>();
        if(!event.hasCollection(LCRelation.class, name)) { return out; }
        for(LCRelation rel : event.get(LCRelation.class, name)) {
            if(rel.getFrom() instanceof RawTrackerHit) {
                out.add((RawTrackerHit) rel.getFrom());
            }
        }
        return out;
    }

    private static String sensorOf(RawTrackerHit hit) {
        return hit.getDetectorElement().getName();
    }

    private static int channelOf(RawTrackerHit hit) {
        return hit.getIdentifierFieldValue("strip");
    }

    private static int categorise(RawTrackerHit hit, Set<RawTrackerHit> pulser,
                                  Set<RawTrackerHit> mcContrib, Set<RawTrackerHit> truthed) {
        boolean p = pulser.contains(hit);
        boolean m = mcContrib.contains(hit);
        boolean t = truthed.contains(hit);
        if(!m) { return p ? PULSER_PURE : NOISE; }
        if(p)  { return t ? MERGED : MERGED_SUBTHRESH; }
        return t ? MC_PURE : MC_PURE_SUBTHRESH;
    }

    /**
     * Minimum distance in strips from this hit to a channel that received MC charge on the
     * same sensor. Returns MAX_DIST_BIN+1 if the sensor had no MC contribution anywhere.
     */
    private int minDistanceToMC(RawTrackerHit hit, Map<String, TreeSet<Integer>> mcChannels) {
        TreeSet<Integer> chans = mcChannels.get(sensorOf(hit));
        if(chans == null || chans.isEmpty()) { return MAX_DIST_BIN + 1; }
        int ch = channelOf(hit);
        Integer lo = chans.floor(ch);
        Integer hi = chans.ceiling(ch);
        int best = Integer.MAX_VALUE;
        if(lo != null) { best = Math.min(best, ch - lo); }
        if(hi != null) { best = Math.min(best, hi - ch); }
        if(best == Integer.MAX_VALUE) { return MAX_DIST_BIN + 1; }
        return Math.min(best, MAX_DIST_BIN);
    }

    @Override
    public void process(EventHeader event) {
        if(!event.hasCollection(Track.class, trackCollectionName)) { return; }

        Set<RawTrackerHit> truthed = fromSide(event, truthRelationCollectionName);
        Set<RawTrackerHit> pulser = fromSide(event, pulserOriginCollectionName);
        Set<RawTrackerHit> mcContrib = fromSide(event, mcContribCollectionName);

        if(!warnedMissing && !event.hasCollection(LCRelation.class, mcContribCollectionName)) {
            warnedMissing = true;
            System.out.println("SvtHitProvenanceDriver: WARNING collection '" + mcContribCollectionName
                    + "' not found. Was writeHitOriginCollections set on the digitization driver?");
        }

        nEvents++;

        // Channels that received MC charge, by sensor. Built from the MC-contribution
        // relation so it is independent of whether the truth gate kept the relation.
        Map<String, TreeSet<Integer>> mcChannels = new HashMap<String, TreeSet<Integer>>();
        for(RawTrackerHit hit : mcContrib) {
            String s = sensorOf(hit);
            TreeSet<Integer> set = mcChannels.get(s);
            if(set == null) { set = new TreeSet<Integer>(); mcChannels.put(s, set); }
            set.add(channelOf(hit));
        }

        // Baseline: how near an MC channel does an arbitrary pulser hit in this event sit?
        // Tracks are compared against this, not against zero.
        if(event.hasCollection(RawTrackerHit.class, rawHitCollectionName)) {
            for(RawTrackerHit hit : event.get(RawTrackerHit.class, rawHitCollectionName)) {
                if(categorise(hit, pulser, mcContrib, truthed) != PULSER_PURE) { continue; }
                nPulserPureBaseline++;
                distHistBaseline[minDistanceToMC(hit, mcChannels)]++;
            }
        }

        for(Track track : event.get(Track.class, trackCollectionName)) {
            nTracks++;

            List<RawTrackerHit> rawHits = new ArrayList<RawTrackerHit>();
            for(TrackerHit th : track.getTrackerHits()) {
                for(Object o : th.getRawHits()) {
                    if(o instanceof RawTrackerHit) { rawHits.add((RawTrackerHit) o); }
                }
            }
            if(rawHits.isEmpty()) { continue; }

            int nTruthedOnTrack = 0;
            for(RawTrackerHit hit : rawHits) {
                if(truthed.contains(hit)) { nTruthedOnTrack++; }
            }
            final int cls = (nTruthedOnTrack == 0) ? 1 : 0;
            if(cls == 1) { nZeroTruthTracks++; }
            nTracksByClass[cls]++;
            nHitsByClass[cls] += rawHits.size();

            for(RawTrackerHit hit : rawHits) {
                int cat = categorise(hit, pulser, mcContrib, truthed);
                nHitsByClassAndCat[cls][cat]++;
                if(cat == PULSER_PURE) {
                    distHistByClass[cls][minDistanceToMC(hit, mcChannels)]++;
                }
            }

            if(debug && cls == 1 && debugPrinted < debugMaxPrint) {
                debugPrinted++;
                StringBuilder sb = new StringBuilder("[SvtProv] zero-truth track, nRawHits="
                        + rawHits.size() + "  hits:");
                for(RawTrackerHit hit : rawHits) {
                    int cat = categorise(hit, pulser, mcContrib, truthed);
                    sb.append("  ").append(CAT_NAME[cat].trim())
                      .append("(").append(sensorOf(hit)).append(":").append(channelOf(hit));
                    if(cat == PULSER_PURE) {
                        int d = minDistanceToMC(hit, mcChannels);
                        sb.append(", dMC=").append(d > MAX_DIST_BIN ? "none" : Integer.toString(d));
                    }
                    sb.append(")");
                }
                System.out.println(sb.toString());
            }
        }
    }

    /** Fraction of entries in a distance histogram at or below d strips. */
    private static double fracWithin(long[] hist, int d) {
        long num = 0, den = 0;
        for(int i = 0; i < hist.length; i++) {
            den += hist[i];
            if(i <= d) { num += hist[i]; }
        }
        return den > 0 ? (double) num / den : 0.0;
    }

    @Override
    public void endOfData() {
        String[] clsName = { "tracks with truth ", "ZERO-truth tracks " };
        System.out.println();
        System.out.println("============== SVT hit provenance by track ==============");
        System.out.println("  events                    : " + nEvents);
        System.out.println("  tracks (" + trackCollectionName + ") : " + nTracks);
        System.out.println("  zero-truth tracks         : " + nZeroTruthTracks
                + (nTracks > 0 ? String.format("  (%.4f)", (double) nZeroTruthTracks / nTracks) : ""));
        System.out.println();

        for(int cls = 0; cls < 2; cls++) {
            System.out.println("  ---- " + clsName[cls] + " ----");
            System.out.println("    tracks : " + nTracksByClass[cls]
                    + "   raw hits/track : "
                    + (nTracksByClass[cls] > 0
                       ? String.format("%.2f", (double) nHitsByClass[cls] / nTracksByClass[cls]) : "-"));
            for(int cat = 0; cat < 6; cat++) {
                long n = nHitsByClassAndCat[cls][cat];
                System.out.println("      " + CAT_NAME[cat] + " : " + n
                        + (nHitsByClass[cls] > 0
                           ? String.format("  (%.4f)", (double) n / nHitsByClass[cls]) : ""));
            }
            System.out.println();
        }

        System.out.println("  ---- are PULSER_PURE hits next to channels that saw MC charge? ----");
        System.out.println("  The baseline is every PULSER_PURE hit in the event, so it already");
        System.out.println("  folds in the occupancy. Only an excess over baseline is meaningful.");
        StringBuilder hdr = new StringBuilder(String.format("    %-26s %10s", "population", "nHits"));
        for(int d : DISTANCES) { hdr.append(String.format("  within%3d", d)); }
        System.out.println(hdr.toString());

        long nZ = 0, nT = 0;
        for(int i = 0; i < distHistByClass[1].length; i++) { nZ += distHistByClass[1][i]; }
        for(int i = 0; i < distHistByClass[0].length; i++) { nT += distHistByClass[0][i]; }

        Object[][] rows = {
            { "baseline (all in event)", distHistBaseline, nPulserPureBaseline },
            { "on tracks with truth",    distHistByClass[0], nT },
            { "on ZERO-truth tracks",    distHistByClass[1], nZ },
        };
        for(Object[] row : rows) {
            StringBuilder sb = new StringBuilder(String.format("    %-26s %10d", row[0], (Long) row[2]));
            for(int d : DISTANCES) {
                sb.append(String.format("  %8.4f", fracWithin((long[]) row[1], d)));
            }
            System.out.println(sb.toString());
        }

        System.out.println();
        System.out.println("    minimum strip distance to an MC channel, ZERO-truth tracks:");
        StringBuilder sb = new StringBuilder("      ");
        for(int i = 0; i <= MAX_DIST_BIN; i++) {
            sb.append(i == MAX_DIST_BIN ? ">=" + MAX_DIST_BIN : Integer.toString(i))
              .append("=").append(distHistByClass[1][i]).append("  ");
        }
        sb.append("noMCOnSensor=").append(distHistByClass[1][MAX_DIST_BIN + 1]);
        System.out.println(sb.toString());
        System.out.println("=========================================================");
        System.out.println();
        super.endOfData();
    }
}
