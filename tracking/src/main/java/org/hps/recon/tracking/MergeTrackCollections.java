package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;

/**
 * Read all track collections in the event, deduplicate tracks with the same hit
 * content, and put the resulting list of unique tracks in a new collection.
 * Remove the original track collections (this behavior can be disabled).
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class MergeTrackCollections extends Driver {

    private String outputCollectionName = "MatchedTracks";
    private boolean removeCollections = true;

    public void setOutputCollectionName(String outputCollectionName) {
        this.outputCollectionName = outputCollectionName;
    }

    public void setRemoveCollections(boolean removeCollections) {
        this.removeCollections = removeCollections;
    }

    @Override
    public void process(EventHeader event) {
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        List<List<Track>> trackCollections = event.get(Track.class);

        // Loop over each of the track collections retrieved from the event
        Map<Set<TrackerHit>, List<Track>> hitsToTracksMap = new HashMap<Set<TrackerHit>, List<Track>>();
        for (List<Track> tracklist : trackCollections) {
            for (Track trk : tracklist) {
                Set<TrackerHit> trackHits = new HashSet<TrackerHit>(TrackUtils.getStripHits(trk, hitToStrips, hitToRotated));
                List<Track> matchingTracks = hitsToTracksMap.get(trackHits);
                if (matchingTracks == null) {
                    matchingTracks = new ArrayList<Track>();
                    hitsToTracksMap.put(trackHits, matchingTracks);
                }
                matchingTracks.add(trk);

            }
        }

        List<Track> deduplicatedTracks = new ArrayList<Track>();

        for (List<Track> matchingTracks : hitsToTracksMap.values()) {
            Track trk = matchingTracks.get(0);// pick lowest-chisq track (this probably doesn't matter)
            for (Track matchingTrack : matchingTracks) {
                if (matchingTrack.getChi2() < trk.getChi2()) {
                    trk = matchingTrack;
                }
            }

            int trackType = 0;
            for (Track matchingTrack : matchingTracks) {
                // Get the name of the strategy used to find this track
                SeedTrack seedTrack = (SeedTrack) matchingTrack;
                String strategyName = seedTrack.getStrategy().getName();

                // Check if a StrategyType is associated with this strategy. 
                // If it is, set the track type.  Otherwise, just move on 
                // and stick with the default value of zero.
                StrategyType strategyType = StrategyType.getType(strategyName);
                if (strategyType != null) {
                    trackType = TrackType.addStrategy(trackType, strategyType);
                }
            }

            ((SeedTrack) trk).setTrackType(trackType);
            deduplicatedTracks.add(trk);
        }

        if (removeCollections) {
            for (List<Track> tracklist : trackCollections) {
                event.remove(event.getMetaData(tracklist).getName());
            }
        }

        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputCollectionName, deduplicatedTracks, Track.class, flag);
    }
}
