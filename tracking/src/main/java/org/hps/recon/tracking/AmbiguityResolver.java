package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

/**
 * Chooses between potentially-redundant tracks. Keeps lists of: operable
 * tracks, tracks discarded because they're duplicates of operable tracks,
 * tracks discarded because they're sub-tracks of operable tracks, tracks
 * discarded because they share too many hits with operable tracks, tracks that
 * fail scoring criteria, original versions of tracks that required cleaning to
 * become operable.
 *
 * @author Miriam Diamond <mdiamond@slac.stanford.edu> * @version $id: v1
 *         05/30/2017$
 */

public abstract class AmbiguityResolver {

    List<Track> tracks;
    List<Track> partials;
    List<Track> duplicates;
    List<Track> shared;
    List<Track> wereCleaned;
    List<Track> poorScore;

    protected Map<List<TrackerHit>, List<Track>> hitsToTracksMap;
    protected Map<Track, List<Track>> sharedTracksMap;
    protected Map<Track, double[]> trackScoreMap;

    protected AmbiguityResolverUtils utils = new AmbiguityResolverUtils();

    /**
     * Default constructor
     */
    public AmbiguityResolver() {
        this.tracks = new ArrayList<Track>();
        this.partials = new ArrayList<Track>();
        this.duplicates = new ArrayList<Track>();
        this.shared = new ArrayList<Track>();
        this.wereCleaned = new ArrayList<Track>();
        this.poorScore = new ArrayList<Track>();

        hitsToTracksMap = new HashMap<List<TrackerHit>, List<Track>>();
        sharedTracksMap = new HashMap<Track, List<Track>>();
        trackScoreMap = new HashMap<Track, double[]>();
    }

    // getters

    /**
     * @return current operable tracks
     */
    public List<Track> getTracks() {
        return this.tracks;
    }

    /**
     * @return tracks discarded because partial versions of operable tracks
     */
    public List<Track> getPartialTracks() {
        return this.partials;
    }

    /**
     * @return tracks discarded because duplicates of operable tracks
     */
    public List<Track> getDuplicateTracks() {
        return this.duplicates;
    }

    /**
     * @return tracks discarded because shared too many hits with operable
     *         tracks
     */
    public List<Track> getSharedTracks() {
        return this.shared;
    }

    /**
     * @return tracks discarded because of poor score
     */
    public List<Track> getPoorScoreTracks() {
        return this.poorScore;
    }

    // setters

    /**
     * Resets internal states.
     */
    public void resetResolver() {
        this.tracks.clear();
        this.partials.clear();
        this.duplicates.clear();
        this.shared.clear();
        this.wereCleaned.clear();
        this.poorScore.clear();

        hitsToTracksMap.clear();
        sharedTracksMap.clear();
        trackScoreMap.clear();
    }

    /**
     * Makes internal state maps.
     * 
     * @param inputTracks
     *            collection of track lists
     */
    public void initializeFromCollection(List<List<Track>> inputTracks) {
        for (List<Track> tracklist : inputTracks) {
            initializeFromList(tracklist);
        }
    }

    /**
     * Makes internal state maps.
     * 
     * @param tracklist
     */
    public void initializeFromList(List<Track> tracklist) {
        utils.makeTrackHitMaps(tracklist);
        utils.makeSharedTrackMap(tracklist);
        utils.makeTrackScoreMap(tracklist);
    }

    /**
     * Calculate a score for operable track
     * 
     * @param track
     *
     */
    public double scoreTrack(Track track) {
        return 0;
    }

    /**
     * Determines whether given track has holes in any layers
     * 
     * @param track
     * @returns arrayOfIntegers: one int per layer, 0=hit 1=hole
     *          2=outside-acceptance
     *
     */
    protected int[] holesOnTrack(Track trk) {
        return null;
    }

    /**
     * In current mode, perform ambiguity resolution on operable tracks. Updates
     * all track lists.
     */
    public void resolve() {

    }

    /**
     * 
     * @param track
     * @return score if applicable
     */
    public double getScore(Track trk) {
        return trackScoreMap.get(trk)[0];
    }

    /**
     * whether two tracks are considered shared
     * 
     * @param track1
     * @param track2
     * @return yes/no
     *
     */
    protected boolean areShared(Track trk1, Track trk2) {
        return false;
    }

    public class compareScore implements Comparator<Track> {
        public int compare(Track t1, Track t2) {
            double t1Score = getScore(t1);
            double t2Score = getScore(t2);

            if (t1Score > t2Score)
                return -1;
            if (t2Score > t1Score)
                return 1;
            return 0;
        }
    }

    protected class AmbiguityResolverUtils {

        /**
         * Makes track-score internal state map.
         * 
         * @param inputTracks
         *            collection of track lists
         */
        protected void makeTrackScoreMap(List<Track> tracklist) {
            for (Track trk : tracklist) {
                double[] score = new double[1];
                score[0] = scoreTrack(trk);
                trackScoreMap.put(trk, score);
            }

        }

        /**
         * Makes track-hits internal state maps.
         * 
         * @param inputTracks
         *            collection of track lists
         */
        protected void makeTrackHitMaps(List<Track> tracklist) {
            for (Track trk : tracklist) {
                List<TrackerHit> trackHits = trk.getTrackerHits();
                List<Track> matchingTracks = hitsToTracksMap.get(trackHits);
                if (matchingTracks == null) {
                    matchingTracks = new ArrayList<Track>();
                    hitsToTracksMap.put(trackHits, matchingTracks);
                }
                matchingTracks.add(trk);
                this.tracks.add(trk);
            }
        }

        /**
         * Makes shared-tracks internal state maps.
         * 
         * @param inputTracks
         *            collection of track lists
         */
        protected void makeSharedTrackMap(List<Track> tracklist) {
            for (Track trk : tracklist) {
                // tracks with shared hits
                List<Track> sharedTracks = new ArrayList<Track>();
                for (Track otherTrack : tracklist) {
                    if (otherTrack == trk)
                        continue;
                    if (areShared(otherTrack, trk)) {
                        sharedTracks.add(otherTrack);
                    }
                }
                sharedTracksMap.put(trk, sharedTracks);
            }
        }

        /**
         * Finds all partial tracks corresponding to a given track
         * 
         * @param track
         * @return ListOfPartialTracks
         */
        protected List<Track> PartialsForTrack(Track trk) {
            List<Track> partialTracks = new ArrayList<Track>();

            List<TrackerHit> trackHits = trk.getTrackerHits();
            for (Track otherTrack : this.tracks) {
                List<TrackerHit> otherTrackHits = otherTrack.getTrackerHits();
                if (otherTrackHits.size() < trackHits.size() && trackHits.containsAll(otherTrackHits)) {
                    partialTracks.add(otherTrack);
                }
            }

            return partialTracks;
        }

        /**
         * Looks through operable tracks collection for shared tracks. Keeps
         * only the best in operable tracks collection. Puts the rest in shared
         * tracks collection.
         */
        protected void RemoveShared() {
            List<Track> sorted = new ArrayList<Track>(this.tracks);

            Collections.sort(sorted, new compareScore());

            // System.out.println("REMOVING SHARED");
            for (Track trk : sorted) {
                // System.out.printf("track score %f \n", getScore(trk));
                if (this.tracks.contains(trk)) {
                    // System.out.println("    examining this track...");
                    List<Track> shared = sharedTracksMap.get(trk);
                    // System.out.printf("     removing %d shared \n",
                    // shared.size());
                    this.tracks.removeAll(shared);
                    for (Track s : shared) {
                        // System.out.printf(
                        // "           removed track with score %f \n",
                        // getScore(s));
                        if (!this.shared.contains(s))
                            this.shared.add(s);
                    }
                }
            }

        }

        /**
         * Looks through operable tracks collection for duplicate tracks. Keeps
         * only one in operable tracks collection. Puts the rest in duplicate
         * tracks collection.
         */
        protected void RemoveDuplicates() {
            List<Track> temp = new ArrayList<Track>();
            for (Map.Entry<List<TrackerHit>, List<Track>> mapEntry : hitsToTracksMap.entrySet()) {
                List<Track> matchingTracks = mapEntry.getValue();
                if (matchingTracks.isEmpty())
                    continue;

                Track tempTrack = utils.findBestScore(matchingTracks);
                int trackType = 0;
                for (Track matchingTrack : matchingTracks) {
                    // Get the name of the strategy used to find this track
                    SeedTrack seedTrack;
                    try {
                        seedTrack = (SeedTrack) matchingTrack;
                    } catch (ClassCastException ex) {
                        continue;
                    }
                    String strategyName = seedTrack.getStrategy().getName();
                    StrategyType strategyType = StrategyType.getType(strategyName);
                    if (strategyType != null) {
                        trackType = TrackType.addStrategy(trackType, strategyType);
                    }
                }
                try {
                    ((SeedTrack) tempTrack).setTrackType(trackType);
                } catch (ClassCastException ex) {
                }

                temp.add(tempTrack);
            }
            this.duplicates.addAll(this.tracks);
            this.duplicates.removeAll(temp);
            this.tracks = temp;
        }

        /**
         * Looks through operable tracks collection for tracks that are partial
         * versions of others. Removes the partials from the operable tracks
         * collection, and puts them in partial tracks collection
         */
        protected void RemovePartials() {
            for (Track track : this.tracks) {
                if (!this.partials.contains(track)) {
                    List<Track> temp = PartialsForTrack(track);
                    for (Track track2 : temp) {
                        if (!this.partials.contains(track2))
                            this.partials.add(track2);
                    }
                }
            }
            this.tracks.removeAll(this.partials);
        }

        /**
         * Finds the track with the best score from amongst a list
         * 
         * @param trackList
         * @return bestTrack
         */
        protected Track findBestScore(List<Track> tracks) {
            if (tracks.isEmpty())
                return null;

            Track trk = tracks.get(0);
            double best = trackScoreMap.get(trk)[0];

            // pick best-scoring track
            for (Track currentTrack : tracks) {
                double temp = trackScoreMap.get(currentTrack)[0];
                if (temp > best) {
                    trk = currentTrack;
                    best = temp;
                }
            }

            return trk;
        }
    }
}
