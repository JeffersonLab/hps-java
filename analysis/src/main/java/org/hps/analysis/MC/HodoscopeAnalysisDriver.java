package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

public class HodoscopeAnalysisDriver extends Driver {
    private int goodEvents = 0;
    private int totalEvents = 0;
    private int hodoscopeEvents = 0;
    private int hodoscopeBothEvents = 0;
    private int multiLayerEvents = 0;
    private int electronClusterEvents = 0;

    private int hodoMatchEvents = 0;
    private int ecalMatchEvents = 0;
    private int fullCorrelationEvents = 0;
    private int hodoCorrelationEvents = 0;
    private int tier1EcalCorrelationEvents = 0;
    private int tier2EcalCorrelationEvents = 0;
    
    private static final int PDGID_POSITRON = -11;
    
    private final AIDA aida = AIDA.defaultInstance();
    private final IHistogram1D hodoscopeEnergyDistributionAll = aida.histogram1D("Hodoscope/Crystal Energy Distribution (All Particles)", 500, 0.000, 0.005);
    private final IHistogram1D hodoscopeEnergyDistributionPositrons = aida.histogram1D("Hodoscope/Crystal Energy Distribution (Positrons Only)", 500, 0.000, 0.005);
    
    private final IHistogram2D hodoscopeLayerCorrelation = aida.histogram2D("Hodoscope/Layer Correlation", 5, -0.5, 4.5, 5, -0.5, 4.5);
    private final IHistogram2D hodoscopeL1ClusterXCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster X Correlation", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL1ClusterYCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster Y Correlation", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    private final IHistogram2D hodoscopeL2ClusterXCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster X Correlation", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL2ClusterYCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster Y Correlation", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    
    private int totalHodoscopeHits = 0;
    private int positronHodoscopeHits = 0;
    
    private int totalLayer1Hits = 0;
    private int matchedLayer1Hits = 0;
    
    private int minXIndex = Integer.MAX_VALUE;
    private int maxXIndex = Integer.MIN_VALUE;
    private int minYIndex = Integer.MAX_VALUE;
    private int maxYIndex = Integer.MIN_VALUE;
    
    private int[][][] hodoEcalCorrelationTable = { // [LAYER][HODO_X_INDEX][MIN/MAX]
            {
                { 4, 7 }, { 6, 10 }, { 8, 16 }, { 12, 21 }, { 18, 23 }
            },
            {
                { 6, 7 }, { 7, 12 }, { 11, 17 }, { 14, 21 }, { 19, 23 }
            }
    };
    private int[][] hodoHodoCorrelationTable = { // [L1_X_INDEX][L2_MIN/L2_MAX]
            { 0, 1 }, { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 4 }
    };
    
    private List<Integer> weirdEventsList = new ArrayList<Integer>();
    
    @Override
    public void startOfData() {
        
    }
    
    @Override
    public void endOfData() {
        System.out.println("Analysis Debug Summary:");
        System.out.println("\tGeneral Analytic Data:");
        System.out.println("\t\tTotal Events          :: " + totalEvents);
        System.out.println("\t\tGood Events           :: " + goodEvents);
        System.out.println("\t\tHodoscope Events      :: " + hodoscopeEvents);
        System.out.println("\t\tHodoscope Both Events :: " + hodoscopeBothEvents);
        System.out.println("\t\tMulti-Layer Events    :: " + multiLayerEvents);
        System.out.println("\t\tTracked e- Events     :: " + electronClusterEvents);
        
        System.out.println("\tCorrelation Table Data:");
        System.out.println("\t\tHodoscope Matched Events       :: " + hodoMatchEvents);
        System.out.println("\t\tHodoscope Correlation Events   :: " + hodoCorrelationEvents);
        System.out.println("\t\tEcal Matched Events            :: " + ecalMatchEvents);
        System.out.println("\t\tEcal Tier-1 Correlation Events :: " + tier1EcalCorrelationEvents);
        System.out.println("\t\tEcal Tier-2 Correlation Events :: " + tier2EcalCorrelationEvents);
        System.out.println("\t\tFull Correlation Events        :: " + fullCorrelationEvents);
        
        System.out.println("\tHodoscope Hit Energy Distribution:");
        System.out.println("\t\tTotal Hits           :: " + totalHodoscopeHits);
        System.out.println("\t\te+ Hits              :: " + positronHodoscopeHits);
        
        System.out.println("\tHodoscope Layer Correlation:");
        System.out.println("\t\tTotal Layer 1 Hits   :: " + totalLayer1Hits);
        System.out.println("\t\tMatched Layer 1 Hits :: " + matchedLayer1Hits);
        
        System.out.println("\tStrange Hodoscope/Cluster Correlations:");
        Set<Integer> repeatSet = new HashSet<Integer>();
        for(Integer i : weirdEventsList) {
            if(!repeatSet.contains(i)) {
                System.out.println("\t\t" + i.toString());
            }
            repeatSet.add(i);
        }
    }
    
    @Override
    public void process(EventHeader event) {
        /**
         * ********************************************************** *
         * ***** Pre-Processing ************************************* *
         * ********************************************************** *
         */
        System.out.println("\n\n\nProcessing event " + event.getEventNumber() + "...");
        
        
        
        /*
         * Process tracker hits and MCParticles. By looking at the
         * number of layers a particle traverses, we can estimate if
         * it should produce a useful track or not.
         */
        
        // Get the tracker hits.
        List<SimTrackerHit> trackerHits = null;
        if(event.hasCollection(SimTrackerHit.class, "TrackerHits")) {
            trackerHits = event.get(SimTrackerHit.class, "TrackerHits");
        } else {
            trackerHits = new ArrayList<SimTrackerHit>(0);
        }
        
        // Process the tracker hits to map the number of SVT layers
        // each particle interacted with.
        System.out.println("Particle Pre-Processing Results:");
        Map<MCParticle, Integer> particleTraversalMap = TruthModule.preprocessMCParticles(trackerHits);
        for(Map.Entry<MCParticle, Integer> entry : particleTraversalMap.entrySet()) {
            System.out.println("\t" + entry.getValue().intValue() + " :: " + TruthModule.getParticleString(entry.getKey()));
        }
        
        
        
        /*
         * Process hodoscope hits. Hodoscope detector response is not
         * simulated. We simply combine the truth hits on the same
         * crystal to create a fake crystal hit.
         */
        
        // Get the hodoscope hits.
        List<SimCalorimeterHit> hodoscopeHits = null;
        if(event.hasCollection(SimCalorimeterHit.class, "HodoscopeHits")) {
            hodoscopeHits = event.get(SimCalorimeterHit.class, "HodoscopeHits");
        } else {
            hodoscopeHits = new ArrayList<SimCalorimeterHit>(0);
        }
        
        // Process the hodoscope hits and output the results.
        System.out.println("\nHodoscope Hit Pre-Processing Results:");
        List<SimCalorimeterHit> processedHodoscopeHits = TruthModule.preprocessHodoscopeHits(hodoscopeHits);
        Map<SimCalorimeterHit, Map<MCParticle, Double>> hodoscopeParticleContributionMap = TruthModule.getHodoscopeHitParticleEnergyContributionMap(processedHodoscopeHits);
        for(SimCalorimeterHit hit : processedHodoscopeHits) {
            Map<MCParticle, Double>  percentEnergyMap = hodoscopeParticleContributionMap.get(hit);
            
            System.out.println("\t" + TruthModule.getHodoscopeHitString(hit));
            for(Map.Entry<MCParticle, Double> entry : percentEnergyMap.entrySet()) {
                System.out.printf("\t\t%6.2f%% :: %s%n", entry.getValue().doubleValue() * 100.0, TruthModule.getParticleString(entry.getKey()));
            }
        }
        
        
        
        /*
         * Process clusters. Clusters should have truth information
         * in their hits. This can be used to associate a cluster
         * with a particle for matching to hodoscope hits.
         */
        
        // Get GTP clusters.
        List<Cluster> gtpClusters = null;
        if(event.hasCollection(Cluster.class, "EcalClustersGTP")) {
            gtpClusters = event.get(Cluster.class, "EcalClustersGTP");
        } else {
            gtpClusters = new ArrayList<Cluster>();
        }
        
        // Get reconstructed clusters.
        List<Cluster> reconClusters = null;
        if(event.hasCollection(Cluster.class, "EcalClustersCorr")) {
            reconClusters = event.get(Cluster.class, "EcalClustersCorr");
        } else {
            reconClusters = new ArrayList<Cluster>();
        }
        
        // Map the percentage contribution of each MC particle to the
        // cluster.
        System.out.println("\nCluster Truth Pre-Processing Results:");
        Map<Cluster, Map<MCParticle, Double>> clusterParticleContributionMap = TruthModule.getClusterParticleEnergyContributionMap(reconClusters);
        for(Cluster reconCluster : reconClusters) {
            Map<MCParticle, Double> percentEnergyMap = clusterParticleContributionMap.get(reconCluster);
            
            System.out.println("\t" + TruthModule.getClusterString(reconCluster));
            for(Map.Entry<MCParticle, Double> entry : percentEnergyMap.entrySet()) {
                System.out.printf("\t\t%6.2f%% :: %s%n", entry.getValue().doubleValue() * 100.0, TruthModule.getParticleString(entry.getKey()));
            }
        }
        
        // Try to match reconstructed and GTP clusters together.
        Map<Cluster, Cluster> reconGTPMap = TruthModule.mapReconToGTP(reconClusters, gtpClusters);
        
        System.out.println("\nCluster Matching Pre-Processing Results:");
        System.out.println("\tUnmatched Recon Clusters:");
        if(reconGTPMap.size() == reconClusters.size()) {
            System.out.println("\t\tNone");
        } else {
            for(Cluster reconCluster : reconClusters) {
                if(!reconGTPMap.containsKey(reconCluster)) {
                    System.out.println("\t\t" + TruthModule.getClusterString(reconCluster));
                }
            }
        }
        System.out.println("\tMatched Clusters:");
        if(reconGTPMap.isEmpty()) {
            System.out.println("\t\tNone");
        } else {
            for(Map.Entry<Cluster, Cluster> entry : reconGTPMap.entrySet()) {
                System.out.println("\t\t" + TruthModule.getClusterString(entry.getKey()) + " >>> " + TruthModule.getClusterString(entry.getValue()));
            }
        }
        
        
        
        /*
         * Process tracks. Tracks should be correlated with clusters
         * by extrapolating their position to the calorimeter face
         * and attaching them to nearby clusters.
         */
        
        // Get the GBL tracks.
        System.out.println("\nCluster/Track Matching Pre-Processing Results:");
        List<Track> gblTracks = null;
        if(event.hasCollection(Track.class, "GBLTracks")) {
            gblTracks = event.get(Track.class, "GBLTracks");
        } else {
            gblTracks = new ArrayList<Track>(0);
        }
        
        // Extrapolate the tracks to the calorimeter face and attach
        // them to clusters.
        Map<Cluster, Track> clusterTrackMap = TruthModule.getClusterTrackMatchedPairs(gblTracks, reconClusters);
        for(Cluster cluster : reconClusters) {
            System.out.println("\t" + TruthModule.getClusterString(cluster));
            Track track = clusterTrackMap.get(cluster);
            if(track == null) {
                System.out.println("\t\tMatched track: " + TruthModule.getTrackString(track));
            } else {
                System.out.println("\t\tMatched track: None");
            }
        }
        
        
        
        /**
         * ********************************************************** *
         * ***** Analysis ******************************************* *
         * ********************************************************** *
         */
        System.out.println("\nInitiating analysis...");
        
        
        
        /*
         * Track the total number of triggered events processed.
         */
        
        // Track the total number of events.
        totalEvents++;
        
        
        
        /*
         * Determine if this is a "good" event. Good events should
         * have at least one positive and one negative particle that
         * pass through 5 or more layers of the SVT each. This
         * implies that they will produce tracks and be analyzable.
         */
        
        // Search for an analyzable particle of each type.
        boolean sawTopPositron = false;
        boolean sawBotPositron = false;
        boolean sawTopElectron = false;
        boolean sawBotElectron = false;
        for(Track gblTrack : gblTracks) {
            double[] r = TruthModule.getTrackPositionAtCalorimeterFace(gblTrack);
            if(TruthModule.getCharge(gblTrack) > 0) {
                if(r[1] > 0) {
                    sawTopPositron = true;
                } else {
                    sawBotPositron = true;
                }
            } else if(TruthModule.getCharge(gblTrack) < 0) {
                if(r[1] > 0) {
                    sawTopElectron = true;
                } else {
                    sawBotElectron = true;
                }
            }
        }
        
        // A "good" event has each particle.
        System.out.println("\n\tChecking event quality (GBL method)... ");
        System.out.println("\t\tHas analyzable top electron: " + sawTopElectron);
        System.out.println("\t\tHas analyzable bot electron: " + sawBotElectron);
        System.out.println("\t\tHas analyzable top positron: " + sawTopPositron);
        System.out.println("\t\tHas analyzable bot positron: " + sawBotPositron);
        
        boolean isGoodEvent = (sawBotPositron && sawTopElectron) || (sawTopPositron && sawBotElectron);
        System.out.println("\t\tIs \"good\" event: " + isGoodEvent);
        
        if(!isGoodEvent) { return; }
        goodEvents++;
        
        
        
        /*
         * Check if there were any hodoscope events.
         */
        
        // Track the number of events that have hodoscope hits.
        boolean sawLayer0 = false;
        boolean sawLayer1 = false;
        for(SimCalorimeterHit hit : processedHodoscopeHits) {
            if(hit.getRawEnergy() > 0.001) {
                if(TruthModule.getHodoscopeZIndex(hit) == 0) { sawLayer0 = true; }
                if(TruthModule.getHodoscopeZIndex(hit) == 1) { sawLayer1 = true; }
            }
        }
        if(sawLayer0 || sawLayer1) { hodoscopeEvents++; }
        if(sawLayer0 && sawLayer1) { hodoscopeBothEvents++; }
        System.out.println("\n\tSaw hodoscope hits: " + hodoscopeHits.size());
        System.out.println("\t\tIs hodoscope event: " + (sawLayer0 || sawLayer1));
        System.out.println("\t\tIs hodoscope both-layers event: " + (sawLayer0 && sawLayer1));
        
        
        
        /*
         * Plot the distribution of total energy deposition into
         * hodoscope crystals.
         * NOTE: This should probably use a more advanced method in
         * the future to account for the multiple APD channels in
         * some of the crystals.
         */
        
        System.out.println("\n\tTracking total hodoscope crystal energy deposition... ");
        for(SimCalorimeterHit hit : processedHodoscopeHits) {
            totalHodoscopeHits++;
            
            // Fill the all particles plot.
            hodoscopeEnergyDistributionAll.fill(hit.getRawEnergy());
            System.out.printf("\t\tHit with %5.3f GeV%n", + hit.getRawEnergy());
            
            // A hodoscope hit is considered to have been created by
            // a positron if a positron contributed at least 50% of
            // its total energy.
            Map<MCParticle, Double> incidentEnergyMap = hodoscopeParticleContributionMap.get(hit);
            for(Map.Entry<MCParticle, Double> entry : incidentEnergyMap.entrySet()) {
                if(entry.getKey().getPDGID() == PDGID_POSITRON && entry.getValue().doubleValue() >= 0.50) {
                    positronHodoscopeHits++;
                    hodoscopeEnergyDistributionPositrons.fill(hit.getRawEnergy());
                    break;
                }
            }
        }
        // TODO: Track number in high-energy (85% of beam) region.
        
        
        /*
         * Track how many events contain a particle that contributes
         * to more than three hodoscope hits in the same layer.
         */
        
        Map<MCParticle, int[]> layerHitsMap = new HashMap<MCParticle, int[]>();
        for(SimCalorimeterHit hodoscopeHit : processedHodoscopeHits) {
            Map<MCParticle, Double> percentEnergyMap = hodoscopeParticleContributionMap.get(hodoscopeHit);
            MCParticle hodoscopeParticle = TruthModule.getHighestContributingParticle(percentEnergyMap);
            
            int iz = -1;
            int counts[] = null;
            if(layerHitsMap.containsKey(hodoscopeParticle)) {
                counts = layerHitsMap.get(hodoscopeParticle);
                iz = TruthModule.getHodoscopeZIndex(hodoscopeHit);
                counts[iz]++;
            } else {
                counts = new int[2];
                iz = TruthModule.getHodoscopeZIndex(hodoscopeHit);
                counts[iz]++;
                layerHitsMap.put(hodoscopeParticle, counts);
            }
            
            if(counts[iz] >= 3) {
                multiLayerEvents++;
                break;
            }
        }
        
        
        
        /*
         * Plot the index of the layer 2 hit that corresponds to each
         * layer 1 hit.
         */
        
        System.out.println("\n\tTracking hodoscope layer 1 vs. layer 2 correlation... ");
        boolean metLayerTableCondition = false;
        boolean hodoscopeCorrelationOccurred = false;
        for(SimCalorimeterHit layer1Hit : processedHodoscopeHits) {
            // Skip layer 2 hits.
            if(TruthModule.getHodoscopeZIndex(layer1Hit) == 1) { continue; }
            
            totalLayer1Hits++;
            
            // Check for a particle with the highest percent energy
            // contribution.
            MCParticle layer1Particle = TruthModule.getHighestContributingParticle(hodoscopeParticleContributionMap.get(layer1Hit));
            
            // If there is no dominant particle, skip this hit.
            if(layer1Particle == null) {
                continue;
            }
            
            // For layer 1 hits, look for one or more layer 2 hits
            // that both receive 50% or more energy from the same
            // particle.
            for(SimCalorimeterHit layer2Hit : processedHodoscopeHits) {
                // Skip layer 1 hits.
                if(TruthModule.getHodoscopeZIndex(layer2Hit) == 0) { continue; }
                
                // Check if there is a shared particle with more than
                // 50% energy contribution to the layer 2 hit.
                MCParticle layer2Particle = TruthModule.getHighestContributingParticle(hodoscopeParticleContributionMap.get(layer2Hit));
                
                // If the two particles are the same, plot them.
                if(layer1Particle == layer2Particle) {
                    matchedLayer1Hits++;
                    
                    int l1x = TruthModule.getHodoscopeXIndex(layer1Hit);
                    int l2x = TruthModule.getHodoscopeXIndex(layer2Hit);
                    
                    hodoscopeLayerCorrelation.fill(l1x, l2x);
                    
                    hodoscopeCorrelationOccurred = true;
                    if(hodoHodoCorrelationTable[l1x][0] <= l2x && hodoHodoCorrelationTable[l1x][1] >= l2x) {
                        metLayerTableCondition = true;
                    }
                    
                    System.out.println("\t\t" + TruthModule.getHodoscopeHitString(layer1Hit) + " >>> " + TruthModule.getHodoscopeHitString(layer2Hit));
                }
            }
        }
        System.out.println("\tMatched layer hits    :: " + hodoscopeCorrelationOccurred);
        System.out.println("\tMet table correlation :: " + metLayerTableCondition);
        if(hodoscopeCorrelationOccurred) { hodoMatchEvents++; }
        if(metLayerTableCondition) { hodoCorrelationEvents++; }
        // TODO: Add 1 MeV threshold.
        
        
        
        /*
         * Check if a cluster is attached a particle which should
         * produce a track.
         */
        
        System.out.println("\n\tChecking for cluster with analyzable electron particle... ");
        boolean foundElectron = false;
        for(Cluster reconCluster : reconClusters) {
            if(clusterTrackMap.containsKey(reconCluster)) {
                Track gblTrack = clusterTrackMap.get(reconCluster);
                if(TruthModule.getCharge(gblTrack) < 0) {
                    foundElectron = true;
                    break;
                }
            }
        }
        
        if(foundElectron) {
            electronClusterEvents++;
            System.out.println("\t\tFound");
        } else {
            System.out.println("\t\tNot found");
        }
        
        
        
        /*
         * Plot the position correlation between hodoscope crystals
         * and calorimeter clusters.
         */
        
        System.out.println("\n\tAnalyzing hodoscope/cluster position correlations... ");
        boolean calorimeterCorrelationOccurred = false;
        boolean[] metCalorimeterTableCondition = new boolean[2];
        for(SimCalorimeterHit hodoscopeHit : processedHodoscopeHits) {
            if(hodoscopeHit.getRawEnergy() < 0.001) {
                continue;
            }
            
            System.out.println("\t\t" + TruthModule.getHodoscopeHitString(hodoscopeHit));
            
            Map<MCParticle, Double> percentEnergyMap = hodoscopeParticleContributionMap.get(hodoscopeHit);
            MCParticle hodoscopeParticle = TruthModule.getHighestContributingParticle(percentEnergyMap);
            
            if(hodoscopeParticle.getPDGID() != PDGID_POSITRON) { continue; }
            Integer traversedLayers = particleTraversalMap.get(hodoscopeParticle);
            if(traversedLayers == null || traversedLayers.intValue() < 5) {
                continue;
            }
            
            Cluster bestCluster = null;
            double bestEnergyDeposition = 0.0;
            for(Map.Entry<Cluster, Map<MCParticle, Double>> mapEntry : clusterParticleContributionMap.entrySet()) {
                double truthEnergy = TruthModule.getTruthEnergy(mapEntry.getKey());
                
                for(Map.Entry<MCParticle, Double> particleEntry : mapEntry.getValue().entrySet()) {
                    if(particleEntry.getKey() == hodoscopeParticle) {
                        double energyDeposition = truthEnergy * particleEntry.getValue().doubleValue();
                        if(energyDeposition > bestEnergyDeposition) {
                            bestEnergyDeposition = energyDeposition;
                            bestCluster = mapEntry.getKey();
                        }
                    }
                }
            }
            
            if(bestCluster == null || TriggerModule.getValueClusterTotalEnergy(bestCluster) < 0.200) {
                continue;
            }
            
            double bestEnergyPercent = bestCluster == null ? 0.0 : bestEnergyDeposition / TruthModule.getTruthEnergy(bestCluster);
            if(bestCluster == null) {
                System.out.println("\t\t\tNo matching cluster found.");
            } else if(bestEnergyPercent < 0.10) {
                System.out.println("\t\t\tMatched cluster: " + TruthModule.getClusterString(bestCluster));
                System.out.printf("\t\t\tEnergy Deposition: %5.3f GeV%n", bestEnergyDeposition);
                System.out.printf("\t\t\tPercent Deposition: %5.1f%%%n", 100.0 * bestEnergyPercent);
                System.out.println("\t\t\tIgnoring correlation due to low energy contribution.");
            } else {
                System.out.println("\t\t\tMatched cluster: " + TruthModule.getClusterString(bestCluster));
                System.out.printf("\t\t\tEnergy Deposition: %5.3f GeV%n", bestEnergyDeposition);
                System.out.printf("\t\t\tPercent Deposition: %5.1f%%%n", 100.0 * bestEnergyPercent);
                
                Cluster gtpCluster = reconGTPMap.get(bestCluster);
                int ecalIX = TriggerModule.getClusterXIndex(bestCluster);
                int ecalIY = TriggerModule.getClusterYIndex(bestCluster);
                int hodoIX = TruthModule.getHodoscopeXIndex(hodoscopeHit);
                int hodoIY = TruthModule.getHodoscopeYIndex(hodoscopeHit);
                int hodoIZ = TruthModule.getHodoscopeZIndex(hodoscopeHit);
                maxXIndex = Math.max(ecalIX, maxXIndex);
                minXIndex = Math.min(ecalIX, minXIndex);
                maxYIndex = Math.max(ecalIY, maxYIndex);
                minYIndex = Math.min(ecalIY, minYIndex);
                
                calorimeterCorrelationOccurred = true;
                
                System.out.printf("\t\t\t\t\tConsidering correlation between hodoscope hit at (%1d, %2d, %1d) and cluster at (%3d, %2d)...%n",
                        hodoIX, hodoIY, hodoIZ, ecalIX, ecalIY);
                System.out.printf("\t\t\t\t\t\tTable min/max for L%d ix = %1d  :: [%2d, %2d]%n",
                        hodoIZ + 1, hodoIX, hodoEcalCorrelationTable[hodoIZ][hodoIX][0], hodoEcalCorrelationTable[hodoIZ][hodoIX][1]);
                System.out.printf("\t\t\t\t\t\tCorrelation condition is met :: [ %s ]%n",
                        hodoEcalCorrelationTable[hodoIZ][hodoIX][0] <= ecalIX && hodoEcalCorrelationTable[hodoIZ][hodoIX][1] >= ecalIX);
                
                if(hodoEcalCorrelationTable[hodoIZ][hodoIX][0] <= ecalIX && hodoEcalCorrelationTable[hodoIZ][hodoIX][1] >= ecalIX) {
                    metCalorimeterTableCondition[hodoIZ] = true;
                }
                
                if(ecalIX < -5) {
                    weirdEventsList.add(Integer.valueOf(event.getEventNumber()));
                }
                
                if(TruthModule.getHodoscopeZIndex(hodoscopeHit) == 0) {
                    if(gtpCluster != null) {
                        hodoscopeL1ClusterXCorrelation.fill(TruthModule.getHodoscopeXIndex(hodoscopeHit), TriggerModule.getClusterXIndex(gtpCluster));
                        hodoscopeL1ClusterYCorrelation.fill(TruthModule.getHodoscopeYIndex(hodoscopeHit), TriggerModule.getClusterYIndex(gtpCluster));
                    } else {
                        hodoscopeL1ClusterXCorrelation.fill(TruthModule.getHodoscopeXIndex(hodoscopeHit), TriggerModule.getClusterXIndex(bestCluster));
                        hodoscopeL1ClusterYCorrelation.fill(TruthModule.getHodoscopeYIndex(hodoscopeHit), TriggerModule.getClusterYIndex(bestCluster));
                    }
                } else {
                    if(gtpCluster != null) {
                        hodoscopeL2ClusterXCorrelation.fill(TruthModule.getHodoscopeXIndex(hodoscopeHit), TriggerModule.getClusterXIndex(gtpCluster));
                        hodoscopeL2ClusterYCorrelation.fill(TruthModule.getHodoscopeYIndex(hodoscopeHit), TriggerModule.getClusterYIndex(gtpCluster));
                    } else {
                        hodoscopeL2ClusterXCorrelation.fill(TruthModule.getHodoscopeXIndex(hodoscopeHit), TriggerModule.getClusterXIndex(bestCluster));
                        hodoscopeL2ClusterYCorrelation.fill(TruthModule.getHodoscopeYIndex(hodoscopeHit), TriggerModule.getClusterYIndex(bestCluster));
                    }
                }
            }
        }
        System.out.println("\t\tCorrelation occurred            :: " + calorimeterCorrelationOccurred);
        System.out.println("\t\tMet L1 correlation condition    :: " + metCalorimeterTableCondition[0]);
        System.out.println("\t\tMet L2 correlation condition    :: " + metCalorimeterTableCondition[1]);
        System.out.println("\t\tMet both correlation conditions :: " + (metCalorimeterTableCondition[0] && metCalorimeterTableCondition[1]));
        if(calorimeterCorrelationOccurred) { ecalMatchEvents++; }
        if(metCalorimeterTableCondition[0] || metCalorimeterTableCondition[1]) {
            tier1EcalCorrelationEvents++;
            if(metCalorimeterTableCondition[0] && metCalorimeterTableCondition[1]) {
                tier2EcalCorrelationEvents++;
            }
        }
        
        
        /*
         * Check the correlation table results were met.
         */
        
        if(metLayerTableCondition && metCalorimeterTableCondition[0] && metCalorimeterTableCondition[1]) {
            fullCorrelationEvents++;
        }
    }
}