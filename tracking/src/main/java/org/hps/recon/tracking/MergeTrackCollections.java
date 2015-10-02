package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lcsim.event.EventHeader;
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
    private String partialTrackCollectionName = "PartialTracks";
    private boolean removeCollections = true;
    private double badHitChisq = 10.0;

    /**
     * Name of the LCIO collection containing all good tracks.
     *
     * @param outputCollectionName Defaults to MatchedTracks.
     */
    public void setOutputCollectionName(String outputCollectionName) {
        this.outputCollectionName = outputCollectionName;
    }

    /**
     * Name of the LCIO collection containing partial tracks (tracks whose hit
     * content is a strict subset of another track).
     *
     * @param partialTrackCollectionName Defaults to PartialTracks.
     */
    public void setPartialTrackCollectionName(String partialTrackCollectionName) {
        this.partialTrackCollectionName = partialTrackCollectionName;
    }

    /**
     * Remove existing track collections after merging them.
     *
     * @param removeCollections Default to true.
     */
    public void setRemoveCollections(boolean removeCollections) {
        this.removeCollections = removeCollections;
    }

    /**
     * Set chisq threshold for partial tracks.
     *
     * @param badHitChisq Chisq threshold, default 10.
     */
    public void setBadHitChisq(double badHitChisq) {
        this.badHitChisq = badHitChisq;
    }

    @Override
    public void process(EventHeader event) {
        List<List<Track>> trackCollections = event.get(Track.class);

        // Loop over each of the track collections retrieved from the event
        Map<Set<TrackerHit>, List<Track>> hitsToTracksMap = new HashMap<Set<TrackerHit>, List<Track>>();
        for (List<Track> tracklist : trackCollections) {
            for (Track trk : tracklist) {
                Set<TrackerHit> trackHits = new HashSet<TrackerHit>(trk.getTrackerHits());
                List<Track> matchingTracks = hitsToTracksMap.get(trackHits);
                if (matchingTracks == null) {
                    matchingTracks = new ArrayList<Track>();
                    hitsToTracksMap.put(trackHits, matchingTracks);
                }
                matchingTracks.add(trk);

            }
        }

        List<Track> deduplicatedTracks = new ArrayList<Track>();
        Map<Track, Set<TrackerHit>> trackToHitsMap = new HashMap<Track, Set<TrackerHit>>();

        for (Map.Entry<Set<TrackerHit>, List<Track>> mapEntry : hitsToTracksMap.entrySet()) {
            List<Track> matchingTracks = mapEntry.getValue();
            Track trk = matchingTracks.get(0);// pick lowest-chisq track
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
            trackToHitsMap.put(trk, mapEntry.getKey());
        }

        List<Track> partialTracks = new ArrayList<Track>();
        for (Track track : deduplicatedTracks) {
            Set<TrackerHit> trackHits = trackToHitsMap.get(track);
            for (Track otherTrack : deduplicatedTracks) {
                Set<TrackerHit> otherTrackHits = trackToHitsMap.get(otherTrack);
                if (otherTrackHits.size() > trackHits.size() && otherTrackHits.containsAll(trackHits) && otherTrack.getChi2() < track.getChi2() + badHitChisq) {
//                    System.out.format("%f %d %f %d\n", track.getChi2(), trackHits.size(), otherTrack.getChi2(), otherTrackHits.size());
                    partialTracks.add(track);
                    break;
                }
            }
        }
        deduplicatedTracks.removeAll(partialTracks);

        if (removeCollections) {
            for (List<Track> tracklist : trackCollections) {
                event.remove(event.getMetaData(tracklist).getName());
            }
        }

        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputCollectionName, deduplicatedTracks, Track.class, flag);
        event.put(partialTrackCollectionName, partialTracks, Track.class, flag);
    }
}
