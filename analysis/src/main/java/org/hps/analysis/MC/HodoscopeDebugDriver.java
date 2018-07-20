package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hps.record.triggerbank.TriggerModule;
import org.hps.util.Pair;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

public class HodoscopeDebugDriver extends Driver {
    private int clusterEvents = 0;
    private int analyzableEvents = 0;
    private int noMatchedPositronEvents = 0;
    private int multiPositronEvents = 0;
    
    
    private final AIDA aida = AIDA.defaultInstance();
    private final IHistogram1D trackMomentumSum = aida.histogram1D("Track/Analyzable Event Momentum Sum (All)", 250, 0.000, 2.500);
    private final IHistogram1D matchedTrackMomentumSum = aida.histogram1D("Track/Analyzable Event Momentum Sum (Matched Positron)", 250, 0.000, 2.500);
    private final IHistogram2D clusterEnergyVsXIndex = aida.histogram2D("Matching/Cluster Energy vs. Cluster x-Index (All Matched Pairs)", 47, -23.5, 23.5, 250, 0.000, 2.500);
    private final IHistogram2D positronClusterEnergyVsXIndex = aida.histogram2D("Matching/Cluster Energy vs. Cluster x-Index (Best Matched Positrons)", 47, -23.5, 23.5, 250, 0.000, 2.500);
    private final IHistogram2D unmatchedClusterEnergyVsXIndex = aida.histogram2D("Matching/Cluster Energy vs. Cluster x-Index (Unmatched Clusters)", 47, -23.5, 23.5, 250, 0.000, 2.500);
    
    private static final int TABLE_ANALYZABLE_EVENTS         = 0;
    private static final int TABLE_MATCHED_ANALYZABLE_EVENTS = 1;
    private static final int TABLE_BUMP_HUNT_EVENTS          = 2;
    private static final int TABLE_MATCHED_BUMP_HUNT_EVENTS  = 3;
    
    private static final int TABLE_ALL = 0;
    private static final int TABLE_NO_LAYER = 1;
    private static final int TABLE_ONE_LAYER = 2;
    private static final int TABLE_ONE_LAYER_ADJ = 3;
    private static final int TABLE_TWO_LAYER = 4;
    
    private final int[][] tableData = new int[4][5];
    
    private final IHistogram1D layer1HitCount = aida.histogram1D("Hodoscope/Layer 1 (All) Hit Multiplicity", 6, -0.5, 5.5);
    private final IHistogram1D layer2HitCount = aida.histogram1D("Hodoscope/Layer 2 (All) Hit Multiplicity", 6, -0.5, 5.5);
    private final IHistogram1D layer1TopHitCount = aida.histogram1D("Hodoscope/Layer 1 (Top) Hit Multiplicity", 6, -0.5, 5.5);
    private final IHistogram1D layer1BotHitCount = aida.histogram1D("Hodoscope/Layer 1 (Bottom) Hit Multiplicity", 6, -0.5, 5.5);
    private final IHistogram1D layer2TopHitCount = aida.histogram1D("Hodoscope/Layer 2 (Top) Hit Multiplicity", 6, -0.5, 5.5);
    private final IHistogram1D layer2BotHitCount = aida.histogram1D("Hodoscope/Layer 2 (Bottom) Hit Multiplicity", 6, -0.5, 5.5);
    private final IHistogram2D layer1MultiHit = aida.histogram2D("Hodoscope/Layer 1 (All) Hit x-Index Relations", 6, -0.5, 5.5, 6, -0.5, 5.5);
    private final IHistogram2D layer2MultiHit = aida.histogram2D("Hodoscope/Layer 2 (All) Hit x-Index Relations", 6, -0.5, 5.5, 6, -0.5, 5.5);
    private final IHistogram2D layer1TopMultiHit = aida.histogram2D("Hodoscope/Layer 1 (Top) Hit x-Index Relations", 6, -0.5, 5.5, 6, -0.5, 5.5);
    private final IHistogram2D layer1BotMultiHit = aida.histogram2D("Hodoscope/Layer 1 (Bottom) Hit x-Index Relations", 6, -0.5, 5.5, 6, -0.5, 5.5);
    private final IHistogram2D layer2TopMultiHit = aida.histogram2D("Hodoscope/Layer 2 (Top) Hit x-Index Relations", 6, -0.5, 5.5, 6, -0.5, 5.5);
    private final IHistogram2D layer2BotMultiHit = aida.histogram2D("Hodoscope/Layer 2 (Bottom) Hit x-Index Relations", 6, -0.5, 5.5, 6, -0.5, 5.5);
    
    private final IHistogram2D noLayerPositronPosition = aida.histogram2D("Track/Positron Track Position (No Hodoscope Hits)", 400, -400, 400, 150, -150, 150);
    private final IHistogram2D oneLayerPositronPosition = aida.histogram2D("Track/Positron Track Position (One Hodoscope Hit)", 400, -400, 400, 150, -150, 150);
    private final IHistogram2D oneLayerAdjPositronPosition = aida.histogram2D("Track/Positron Track Position (One Hodoscope Hit (Adjacent))", 400, -400, 400, 150, -150, 150);
    private final IHistogram2D oneLayerNonAdjPositronPosition = aida.histogram2D("Track/Positron Track Position (One Hodoscope Hit (Non-Adjacent))", 400, -400, 400, 150, -150, 150);
    
    @Override
    public void endOfData() {
        System.out.println("Cluster Events             :: " + clusterEvents);
        System.out.println("Analyzable Events          :: " + analyzableEvents);
        System.out.println("No Matched Positron Events :: " + noMatchedPositronEvents);
        System.out.println("Multi-Positron Events      :: " + multiPositronEvents);
        
        String[] eventTypes = { "Analyzable Events", "Analyzable Events (Matched)", "Bump-Hunt Region Events", "Bump-Hunt Region Events (Matched)" };
        String[] hodoStates = { "All Events", "No Layers", "One Layer", "One Layer (Adj)", "Two Layers" };
        
        for(int i = 0; i < eventTypes.length; i++) {
            System.out.println(eventTypes[i] + ":");
            for(int j = 0; j < hodoStates.length; j++) {
                System.out.printf("\t%-15s :: %6d (%7.3f%%)%n", hodoStates[j], tableData[i][j], (100.0 * tableData[i][j] / tableData[i][TABLE_ALL]));
            }
        }
    }
    
    @Override
    public void process(EventHeader event) {
        // Track the number of events with a cluster at x >= 90 mm.
        clusterEvents++;
        
        // Get the necessary collections.
        List<Track> tracks = TruthModule.getCollection(event, "GBLTracks", Track.class);
        List<Cluster> clusters = TruthModule.getCollection(event, "EcalClustersGTP", Cluster.class);
        
        // Get cluster/track matched pairs.
        Map<Cluster, Track> matchedPairs = TruthModule.getClusterTrackMatchedPairs(tracks, clusters);
        
        // Get the hodoscope hits.
        List<SimCalorimeterHit> rawHodoscopeHits = TruthModule.getCollection(event, "HodoscopeHits", SimCalorimeterHit.class);
        List<SimCalorimeterHit> hodoscopeHits = TruthModule.preprocessHodoscopeHits(rawHodoscopeHits);
        
        
        
        /* ********************************************************** *
         * Test hodoscope event performance for trident conditions.   *
         * ********************************************************** */
        
        // Track whether the event has a hodoscope hit in each layer.
        List<SimCalorimeterHit> layer1TopHits = new ArrayList<SimCalorimeterHit>();
        List<SimCalorimeterHit> layer1BotHits = new ArrayList<SimCalorimeterHit>();
        List<SimCalorimeterHit> layer2TopHits = new ArrayList<SimCalorimeterHit>();
        List<SimCalorimeterHit> layer2BotHits = new ArrayList<SimCalorimeterHit>();
        for(SimCalorimeterHit hit : hodoscopeHits) {
            if(hit.getRawEnergy() > 0.001) {
                if(TruthModule.getHodoscopeZIndex(hit) == 0) {
                    if(TruthModule.getHodoscopeYIndex(hit) == 1) { layer1TopHits.add(hit); }
                    else { layer1BotHits.add(hit); }
                }
                if(TruthModule.getHodoscopeZIndex(hit) == 1) {
                    if(TruthModule.getHodoscopeYIndex(hit) == 1) { layer2TopHits.add(hit); }
                    else { layer2BotHits.add(hit); }
                }
            }
        }
        List<SimCalorimeterHit> layer1Hits = new ArrayList<SimCalorimeterHit>(layer1TopHits.size() + layer1BotHits.size());
        layer1Hits.addAll(layer1TopHits);
        layer1Hits.addAll(layer1BotHits);
        List<SimCalorimeterHit> layer2Hits = new ArrayList<SimCalorimeterHit>(layer2TopHits.size() + layer2BotHits.size());
        layer2Hits.addAll(layer2TopHits);
        layer2Hits.addAll(layer2BotHits);
        
        
        
        /* ********************************************************** *
         * Get the best trident pair, both matched and unmatched.     *
         * ********************************************************** */
        
        // Get the best unmatched trident pair. This is simply the
        // combination of two tracks that has the highest momentum.
        double bestPairSum = 0.0;
        Pair<Track, Track> bestTridentPair = null;
        for(int i = 0; i < tracks.size(); i++) {
            double[] pi = TruthModule.getMomentum(tracks.get(i));
            boolean isTop = TruthModule.isTopTrack(tracks.get(i));
            boolean isPositive = TruthModule.getCharge(tracks.get(i)) > 0;
            
            for(int j = i + 1; j < tracks.size(); j++) {
                // Ignore top/top and bottom/bottom tracks.
                if((isTop && TruthModule.isTopTrack(tracks.get(j))) || (!isTop && TruthModule.isBottomTrack(tracks.get(j)))) {
                    continue;
                }
                
                // Ignore like-charged tracks.
                if((isPositive && (TruthModule.getCharge(tracks.get(j)) > 0)) || (!isPositive && (TruthModule.getCharge(tracks.get(j)) < 0))) {
                    continue;
                }
                
                // Get the momentum sum.
                double[] pj = TruthModule.getMomentum(tracks.get(j));
                double[] psumv = { pi[0] + pj[0], pi[1] + pj[1], pi[2] + pj[2] };
                double psumm = TruthModule.magnitude(psumv);
                
                // Keep the track pair with the best momentum sum.
                if(psumm > bestPairSum) {
                    bestPairSum = psumm;
                    if(isPositive) {
                        bestTridentPair = new Pair<Track, Track>(tracks.get(i), tracks.get(j));
                    } else {
                        bestTridentPair = new Pair<Track, Track>(tracks.get(j), tracks.get(i));
                    }
                }
            }
        }
        
        // Get the best combination of matched positron tracks.
        double bestMatchedPairSum = 0.0;
        Pair<Track, Track> bestMatchedTridentPair = null;
        for(Map.Entry<Cluster, Track> matchedPair : matchedPairs.entrySet()) {
            // The positron must be matched.
            if(TruthModule.getCharge(matchedPair.getValue()) < 0) { continue; }
            
            // Track the momentum sum and top/bottom status.
            double[] pp = TruthModule.getMomentum(matchedPair.getValue());
            boolean isTop = TruthModule.isTopTrack(matchedPair.getValue());
            
            for(Track track : tracks) {
                // Don't pair a track with itself.
                if(track == matchedPair.getValue()) { continue; }
                
                // Only allow top/bottom pairs.
                if((isTop && TruthModule.isTopTrack(track)) || (!isTop && TruthModule.isBottomTrack(track))) {
                    continue;
                }
                
                // Get the momentum sum.
                double[] pe = TruthModule.getMomentum(track);
                double[] psumv = { pp[0] + pe[0], pp[1] + pe[1], pp[2] + pe[2] };
                double psumm = TruthModule.magnitude(psumv);
                
                // Keep the track pair with the best momentum sum.
                if(psumm > bestMatchedPairSum) {
                    bestMatchedPairSum = psumm;
                    bestMatchedTridentPair = new Pair<Track, Track>(matchedPair.getValue(), track);
                }
            }
        }
        
        // Check if this is an "analyzable event."
        boolean isAnalyzableEvent = bestTridentPair != null;
        
        // Only consider analyzable events.
        if(!isAnalyzableEvent) { return; }
        else { analyzableEvents++; }
        
        
        
        /* ********************************************************** *
         * Create hodoscope performance plots.                        *
         * ********************************************************** */
        
        // Make hodoscope layer plots.
        layer1TopHitCount.fill(layer1TopHits.size());
        layer1BotHitCount.fill(layer1BotHits.size());
        layer2TopHitCount.fill(layer2TopHits.size());
        layer2BotHitCount.fill(layer2BotHits.size());
        layer1HitCount.fill(Math.max(layer1TopHits.size(), layer1BotHits.size()));
        layer2HitCount.fill(Math.max(layer2TopHits.size(), layer2BotHits.size()));
        for(int i = 0; i < layer1TopHits.size(); i++) {
            int ix = TruthModule.getHodoscopeXIndex(layer1TopHits.get(i));
            for(int j = i + 1; j < layer1TopHits.size(); j++) {
                layer1MultiHit.fill(ix, TruthModule.getHodoscopeXIndex(layer1TopHits.get(j)));
                layer1TopMultiHit.fill(ix, TruthModule.getHodoscopeXIndex(layer1TopHits.get(j)));
            }
        }
        for(int i = 0; i < layer1BotHits.size(); i++) {
            int ix = TruthModule.getHodoscopeXIndex(layer1BotHits.get(i));
            for(int j = i + 1; j < layer1BotHits.size(); j++) {
                layer1MultiHit.fill(ix, TruthModule.getHodoscopeXIndex(layer1BotHits.get(j)));
                layer1BotMultiHit.fill(ix, TruthModule.getHodoscopeXIndex(layer1BotHits.get(j)));
            }
        }
        for(int i = 0; i < layer2TopHits.size(); i++) {
            int ix = TruthModule.getHodoscopeXIndex(layer2TopHits.get(i));
            for(int j = i + 1; j < layer2TopHits.size(); j++) {
                layer2MultiHit.fill(ix, TruthModule.getHodoscopeXIndex(layer2TopHits.get(j)));
                layer2TopMultiHit.fill(ix, TruthModule.getHodoscopeXIndex(layer2TopHits.get(j)));
            }
        }
        for(int i = 0; i < layer2BotHits.size(); i++) {
            int ix = TruthModule.getHodoscopeXIndex(layer2BotHits.get(i));
            for(int j = i + 1; j < layer2BotHits.size(); j++) {
                layer2MultiHit.fill(ix, TruthModule.getHodoscopeXIndex(layer2BotHits.get(j)));
                layer2BotMultiHit.fill(ix, TruthModule.getHodoscopeXIndex(layer2BotHits.get(j)));
            }
        }
        
        // If this is a no hodoscope hits event, plot the positron
        // position at the calorimeter face.
        if(layer1Hits.isEmpty() && layer2Hits.isEmpty()) {
            double[] r = TruthModule.getTrackPositionAtCalorimeterFace(bestTridentPair.getFirstElement());
            noLayerPositronPosition.fill(r[0], r[1]);
        }
        
        // Do the same for cases where there is only one layer of the
        // hodoscope occupied.
        boolean l1t = !layer1TopHits.isEmpty();
        boolean l1b = !layer1BotHits.isEmpty();
        boolean l2t = !layer2TopHits.isEmpty();
        boolean l2b = !layer2BotHits.isEmpty();
        boolean oneLayerTop = (l1t && !l2t) || (!l1t && l2t);
        boolean oneLayerBot = (l1b && !l2b) || (!l1b && l2b);
        boolean twoLayersTop = l1t && l2t;
        boolean twoLayersBot = l1b && l2b;
        if((!twoLayersBot && oneLayerTop) || (!twoLayersTop && oneLayerBot)) {
            double[] r = TruthModule.getTrackPositionAtCalorimeterFace(bestTridentPair.getFirstElement());
            oneLayerPositronPosition.fill(r[0], r[1]);
        }
        
        
        
        /* ********************************************************** *
         * Test cut performance for trident cuts.                     *
         * ********************************************************** */
        
        // Track how many events have a cluster at x >= 90 mm, but no
        // matched cluster/track pair where the cluster is in this
        // same range.
        boolean hasMatchedPositron = false;
        for(Cluster matchedCluster : matchedPairs.keySet()) {
            if(matchedCluster.getPosition()[0] >= 90) {
                hasMatchedPositron = true;
                break;
            }
        }
        if(!hasMatchedPositron) {
            noMatchedPositronEvents++;
            
            double bestEnergy = 0.0;
            Cluster bestCluster = null;
            for(Cluster cluster : clusters) {
                if(cluster.getPosition()[0] >= 90 && TriggerModule.getValueClusterTotalEnergy(cluster) > bestEnergy) {
                    bestCluster = cluster;
                    bestEnergy = TriggerModule.getValueClusterTotalEnergy(cluster);
                }
            }
            if(bestCluster != null) {
                unmatchedClusterEnergyVsXIndex.fill(TriggerModule.getClusterXIndex(bestCluster), TriggerModule.getValueClusterTotalEnergy(bestCluster));
            }
        }
        
        
        
        /* ********************************************************** *
         * Establish "best trident" conditions and test cut losses    *
         * for these conditions.                                      *
         * ********************************************************** */
        
        // Plot the momentum sum for the highest-momentum positive
        // and negative track pair for all events.
        int totalPositiveTracks = 0;
        
        // Track how many events have more than one positron.
        if(totalPositiveTracks > 1) { multiPositronEvents++; }
        
        // Plot the best momentum.
        if(bestPairSum != 0) { trackMomentumSum.fill(bestPairSum); }
        
        // Plot the best momentum.
        if(bestMatchedPairSum != 0) { matchedTrackMomentumSum.fill(bestMatchedPairSum); }
        
        // Plot the cluster energy vs. the cluster x-position for
        // matched pairs.
        for(Map.Entry<Cluster, Track> entry : matchedPairs.entrySet()) {
            clusterEnergyVsXIndex.fill(TriggerModule.getClusterXIndex(entry.getKey()), TriggerModule.getValueClusterTotalEnergy(entry.getKey()));
            if(bestMatchedTridentPair!= null && bestMatchedTridentPair.getFirstElement() == entry.getValue()) {
                positronClusterEnergyVsXIndex.fill(TriggerModule.getClusterXIndex(entry.getKey()), TriggerModule.getValueClusterTotalEnergy(entry.getKey()));
            }
        }
        
        
        
        /* ********************************************************** *
         * Explore one-layer events.                                  *
         * ********************************************************** */
        
        // Track which half of the calorimeter each track is in.
        boolean bestPairIsTop = TruthModule.isTopTrack(bestTridentPair.getFirstElement());
        boolean bestMatchedPairIsTop = false;
        if(bestMatchedPairSum != 0.0) { bestMatchedPairIsTop = TruthModule.isTopTrack(bestMatchedTridentPair.getFirstElement()); }
        
        // For one layer events, check if there are two adjacent, low
        // energy hodoscope hits in the missing layer.
        boolean sawAdjacent = false;
        if(bestPairIsTop && oneLayerTop) {
            if(l1t && !l2t) {
                sawAdjacent = sawAdjacent(hodoscopeHits, 1, 1);
            } else if(!l1t && l2t) {
                sawAdjacent = sawAdjacent(hodoscopeHits, 1, 0);
            }
        } else if(!bestPairIsTop && oneLayerBot) {
            if(l1b && !l2b) {
                sawAdjacent = sawAdjacent(hodoscopeHits, -1, 1);
            } else if(!l1b && l2b) {
                sawAdjacent = sawAdjacent(hodoscopeHits, -1, 0);
            }
        }
        
        // Plot the positions of the tracks for both adjacent and
        // non-adjacent events.
        if(oneLayerTop || oneLayerBot) {
            double[] r = TruthModule.getTrackPositionAtCalorimeterFace(bestTridentPair.getFirstElement());
            if(sawAdjacent) { oneLayerAdjPositronPosition.fill(r[0], r[1]); }
            else { oneLayerNonAdjPositronPosition.fill(r[0], r[1]); }
        }
        
        // Do that same for matched pairs.
        boolean sawMatchedAdjacent = false;
        if(bestMatchedPairIsTop) {
            if(l1t && !l2t) {
                sawMatchedAdjacent = sawAdjacent(hodoscopeHits, 1, 1);
            } else if(!l1t && l2t) {
                sawMatchedAdjacent = sawAdjacent(hodoscopeHits, 1, 0);
            }
        } else if(!bestMatchedPairIsTop && oneLayerBot) {
            if(l1b && !l2b) {
                sawMatchedAdjacent = sawAdjacent(hodoscopeHits, -1, 1);
            } else if(!l1b && l2b) {
                sawMatchedAdjacent = sawAdjacent(hodoscopeHits, -1, 0);
            }
        }
        
        
        
        /* ********************************************************** *
         * Determine how many of the trident criteria are met.        *
         * ********************************************************** */
        
        // Matched trident events have a positive track that is
        // matched to a cluster.
        boolean isMatchedTridentEvent = bestMatchedPairSum != 0.000;
        
        // Bump-hunt trident events have a track pair that has a
        // momentum sum greater than 70% beam energy.
        double bumpHuntRegionThreshold = 0.70 * 2.300;
        boolean isBumpHuntTridentEvent = bestPairSum >= bumpHuntRegionThreshold;
        boolean isMatchedBumpHuntTridentEvent = bestMatchedPairSum >= bumpHuntRegionThreshold;
        
        // All events that reach this point are by definition
        // analyzable. Populate this portion of the table.
        tableData[TABLE_ANALYZABLE_EVENTS][TABLE_ALL]++;
        if((bestPairIsTop && oneLayerTop) || (!bestPairIsTop && oneLayerBot)) {
            tableData[TABLE_ANALYZABLE_EVENTS][TABLE_ONE_LAYER]++;
            if(sawAdjacent) { tableData[TABLE_ANALYZABLE_EVENTS][TABLE_ONE_LAYER_ADJ]++; }
        }
        else if((bestPairIsTop && twoLayersTop) || (!bestPairIsTop && twoLayersBot)) { tableData[TABLE_ANALYZABLE_EVENTS][TABLE_TWO_LAYER]++; }
        else { tableData[TABLE_ANALYZABLE_EVENTS][TABLE_NO_LAYER]++; }
        // Check if there exists a matched analyzable pair.
        if(isMatchedTridentEvent) {
            tableData[TABLE_MATCHED_ANALYZABLE_EVENTS][TABLE_ALL]++;
            if((bestMatchedPairIsTop && oneLayerTop) || (!bestMatchedPairIsTop && oneLayerBot)) {
                tableData[TABLE_MATCHED_ANALYZABLE_EVENTS][TABLE_ONE_LAYER]++;
                if(sawMatchedAdjacent) { tableData[TABLE_MATCHED_ANALYZABLE_EVENTS][TABLE_ONE_LAYER_ADJ]++; }
            }
            else if((bestMatchedPairIsTop && twoLayersTop) || (!bestMatchedPairIsTop && twoLayersBot)) { tableData[TABLE_MATCHED_ANALYZABLE_EVENTS][TABLE_TWO_LAYER]++; }
            else { tableData[TABLE_MATCHED_ANALYZABLE_EVENTS][TABLE_NO_LAYER]++; }
        }
        
        // Check if there exists a bump-hunt region pair.
        if(isBumpHuntTridentEvent) {
            tableData[TABLE_BUMP_HUNT_EVENTS][TABLE_ALL]++;
            if((bestPairIsTop && oneLayerTop) || (!bestPairIsTop && oneLayerBot)) {
                tableData[TABLE_BUMP_HUNT_EVENTS][TABLE_ONE_LAYER]++;
                if(sawAdjacent) { tableData[TABLE_BUMP_HUNT_EVENTS][TABLE_ONE_LAYER_ADJ]++; }
            }
            else if((bestPairIsTop && twoLayersTop) || (!bestPairIsTop && twoLayersBot)) { tableData[TABLE_BUMP_HUNT_EVENTS][TABLE_TWO_LAYER]++; }
            else { tableData[TABLE_BUMP_HUNT_EVENTS][TABLE_NO_LAYER]++; }
        }
        
        // Check if there exists a matched bump-hunt region pair.
        if(isMatchedBumpHuntTridentEvent) {
            tableData[TABLE_MATCHED_BUMP_HUNT_EVENTS][TABLE_ALL]++;
            if((bestMatchedPairIsTop && oneLayerTop) || (!bestMatchedPairIsTop && oneLayerBot)) {
                tableData[TABLE_MATCHED_BUMP_HUNT_EVENTS][TABLE_ONE_LAYER]++;
                if(sawMatchedAdjacent) { tableData[TABLE_MATCHED_BUMP_HUNT_EVENTS][TABLE_ONE_LAYER_ADJ]++; }
            }
            else if((bestMatchedPairIsTop && twoLayersTop) || (!bestMatchedPairIsTop && twoLayersBot)) { tableData[TABLE_MATCHED_BUMP_HUNT_EVENTS][TABLE_TWO_LAYER]++; }
            else { tableData[TABLE_MATCHED_BUMP_HUNT_EVENTS][TABLE_NO_LAYER]++; }
        }
    }
    
    private static final boolean sawAdjacent(Collection<SimCalorimeterHit> hits, int iy, int iz) {
        boolean[] sawHit = new boolean[5];
        for(SimCalorimeterHit hit : hits) {
            if(hit.getRawEnergy() > 0.0003) {
                if(TruthModule.getHodoscopeYIndex(hit) == iy && TruthModule.getHodoscopeZIndex(hit) == iz) {
                    sawHit[TruthModule.getHodoscopeXIndex(hit)] = true;
                }
            }
        }
        
        for(int i = 0; i < sawHit.length - 1; i++) {
            if(sawHit[i] && sawHit[i + 1]) { return true; }
        }
        
        return false;
    }
    
    private static final boolean isBestTrident(Track positronTrack, Track electronTrack, Cluster positronCluster, int cutLevel) {
        if(cutLevel < 0 || cutLevel > 2) {
            throw new IllegalArgumentException("Cut level must be 0 (3%), 1 (5%), or 2 (9%).");
        }
        
        final double[][] cutThresholds = {
                { 0, 0, 0, 0, 0.445, 0.415, 0.355, 0.325, 0.285, 0.285, 0.255, 0.275, 0.255, 0.245, 0.225, 0.215, 0.215, 0.215, 0.205, 0.205, 0.195, 0.195, 0.175, 0.065 },
                { 0, 0, 0, 0, 0.515, 0.485, 0.445, 0.435, 0.385, 0.375, 0.355, 0.355, 0.335, 0.315, 0.295, 0.285, 0.265, 0.265, 0.255, 0.255, 0.235, 0.235, 0.215, 0.085 },
                { 0, 0, 0, 0, 0.595, 0.585, 0.555, 0.555, 0.505, 0.495, 0.465, 0.445, 0.425, 0.395, 0.365, 0.355, 0.325, 0.315, 0.305, 0.295, 0.275, 0.275, 0.255, 0.115 }
        };
        
        double momentumSum = TruthModule.getMomentumSumMagnitude(positronTrack, electronTrack);
        return (momentumSum >= cutThresholds[cutLevel][TriggerModule.getClusterXIndex(positronCluster)]);
    }
}