package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hps.record.triggerbank.TriggerModule;
import org.hps.util.Pair;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

public class HodoscopeSimpleCorrelationDriver extends Driver {
    private int goodClusterEvents = 0;
    private int goodTrackPairEvents = 0;
    private int matchedPositronEvents = 0;
    private int twoHodoscopeHitEvents = 0;
    private int correlatedHodoscopeHitEvents = 0;
    private int hodoscopeHitEvents = 0;
    private int correlatedHodoscopeCalorimeterEvents = 0;
    
    private final AIDA aida = AIDA.defaultInstance();
    private final IHistogram1D hodoscopeEnergyDistributionAll = aida.histogram1D("Hodoscope/Crystal Energy Distribution", 500, 0.000, 0.005);
    
    private final IHistogram2D hodoscopeLayerCorrelation = aida.histogram2D("Hodoscope/Layer Correlation", 5, -0.5, 4.5, 5, -0.5, 4.5);
    private final IHistogram2D hodoscopeL1ClusterXAllCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster X Correlation (All)", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL1ClusterYAllCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster Y Correlation (All)", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    private final IHistogram2D hodoscopeL2ClusterXAllCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster X Correlation (All)", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL2ClusterYAllCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster Y Correlation (All)", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    
    private final IHistogram2D hodoscopeL1ClusterXPairedCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster X Correlation (Paired)", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL1ClusterYPairedCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster Y Correlation (Paired)", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    private final IHistogram2D hodoscopeL2ClusterXPairedCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster X Correlation (Paired)", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL2ClusterYPairedCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster Y Correlation (Paired)", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    
    private final IHistogram2D hodoscopeL1ClusterXTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster X Correlation (Table)", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL1ClusterYTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster Y Correlation (Table)", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    private final IHistogram2D hodoscopeL2ClusterXTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster X Correlation (Table)", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL2ClusterYTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster Y Correlation (Table)", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    
    private final IHistogram2D hodoscopeL1ClusterXFailTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster X Correlation (Failed)", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL1ClusterYFailTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster Y Correlation (Failed)", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    private final IHistogram2D hodoscopeL2ClusterXFailTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster X Correlation (Failed)", 5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL2ClusterYFailTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster Y Correlation (Failed)", 3, -1.5, 1.5, 11,  -5.5,  5.5);
    
    private final IHistogram2D hodoscopeL1ClusterXTotalFailTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster X Correlation (Total Failed)",
            5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL1ClusterYTotalFailTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster Y Correlation (Total Failed)",
            3, -1.5, 1.5, 11,  -5.5,  5.5);
    private final IHistogram2D hodoscopeL2ClusterXTotalFailTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster X Correlation (Total Failed)",
            5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL2ClusterYTotalFailTableCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster Y Correlation (Total Failed)",
            3, -1.5, 1.5, 11,  -5.5,  5.5);
    
    private final IHistogram2D hodoscopeL1ClusterXTotalFailTableECutCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster X Correlation (Total Failed, E > 200 MeV)",
            5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL1ClusterYTotalFailTableECutCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 1 - Cluster Y Correlation (Total Failed, E > 200 MeV)",
            3, -1.5, 1.5, 11,  -5.5,  5.5);
    private final IHistogram2D hodoscopeL2ClusterXTotalFailTableECutCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster X Correlation (Total Failed, E > 200 MeV)",
            5, -0.5, 4.5, 47, -23.5, 23.5);
    private final IHistogram2D hodoscopeL2ClusterYTotalFailTableECutCorrelation = aida.histogram2D("Hodoscope/Hodoscope Layer 2 - Cluster Y Correlation (Total Failed, E > 200 MeV)",
            3, -1.5, 1.5, 11,  -5.5,  5.5);
    
    private final IHistogram2D clusterEnergyIndexCorrelation
            = aida.histogram2D("Calorimeter/Cluster Energy vs. Position Index (Matched with Hodoscope)", 47, -23.5, 23.5, 75, 0.000, 2.500);
    private final IHistogram2D clusterEnergyPositionCorrelation
            = aida.histogram2D("Calorimeter/Cluster Energy vs. Position (Matched with Hodoscope)", 400, -400, 400, 75, 0.000, 2.500);
    
    private final IHistogram2D clusterEnergyIndexCorrelationNoHodo = aida.histogram2D("Calorimeter/Cluster Energy vs. Position Index (Matched no Hodoscope)", 47, -23.5, 23.5, 75, 0.000, 2.500);
    private final IHistogram2D clusterEnergyPositionCorrelationNoHodo = aida.histogram2D("Calorimeter/Cluster Energy vs. Position (Matched no Hodoscope)", 400, -400, 400, 75, 0.000, 2.500);
    
    private final IHistogram2D clusterEnergyIndexCorrelationAll = aida.histogram2D("Calorimeter/Cluster Energy vs. Position Index (All)", 47, -23.5, 23.5, 75, 0.000, 2.500);
    private final IHistogram2D clusterEnergyPositionCorrelationAll = aida.histogram2D("Calorimeter/Cluster Energy vs. Position (All)", 400, -400, 400, 75, 0.000, 2.500);
    
    private final IHistogram2D reconClusterEnergyIndexCorrelation = aida.histogram2D("Calorimeter/Recon Cluster Energy vs. Position Index (Matched with Hodoscope)", 47, -23.5, 23.5, 75, 0.000, 2.500);
    private final IHistogram2D reconClusterEnergyPositionCorrelation = aida.histogram2D("Calorimeter/Recon Cluster Energy vs. Position (Matched with Hodoscope)", 400, -400, 400, 75, 0.000, 2.500);
    
    private final IHistogram2D reconClusterEnergyIndexCorrelationNoHodo = aida.histogram2D("Calorimeter/Recon Cluster Energy vs. Position Index (Matched no Hodoscope)", 47, -23.5, 23.5, 75, 0.000, 2.500);
    private final IHistogram2D reconClusterEnergyPositionCorrelationNoHodo = aida.histogram2D("Calorimeter/Recon Cluster Energy vs. Position (Matched no Hodoscope)", 400, -400, 400, 75, 0.000, 2.500);
    
    private final IHistogram2D reconClusterEnergyIndexCorrelationAll = aida.histogram2D("Calorimeter/Recon Cluster Energy vs. Position Index (All)", 47, -23.5, 23.5, 75, 0.000, 2.500);
    private final IHistogram2D reconClusterEnergyPositionCorrelationAll = aida.histogram2D("Calorimeter/Recon Cluster Energy vs. Position (All)", 400, -400, 400, 75, 0.000, 2.500);
    
    private final IHistogram1D mcParticleMomentum = aida.histogram1D("Debug/MC Particle Momentum", 250, 0.000, 2.500);
    private final IHistogram1D mcParticleXMomentum = aida.histogram1D("Debug/MC Particle x-Momentum", 200, -0.100, 0.100);
    private final IHistogram1D mcParticleYMomentum = aida.histogram1D("Debug/MC Particle y-Momentum", 200, -0.100, 0.100);
    private final IHistogram1D mcParticleZMomentum = aida.histogram1D("Debug/MC Particle z-Momentum", 500, -2.500, 2.500);
    
    private final IHistogram2D[][] deltaXMomentum = {
            {
                aida.histogram2D("Matching/Momentum vs. #Delta x (Positive Top Track)", 250, 0.000, 2.500, 160, -40, 40),
                aida.histogram2D("Matching/Momentum vs. #Delta x (Positive Bottom Track)", 250, 0.000, 2.500, 160, -40, 40)
            },
            {
                aida.histogram2D("Matching/Momentum vs. #Delta x (Negative Top Track)", 250, 0.000, 2.500, 160, -40, 40),
                aida.histogram2D("Matching/Momentum vs. #Delta x (Negative Bottom Track)", 250, 0.000, 2.500, 160, -40, 40)
            }
    };
    private final IHistogram2D[][] deltaYMomentum = {
            {
                aida.histogram2D("Matching/Momentum vs. #Delta y (Positive Top Track)", 250, 0.000, 2.500, 160, -40, 40),
                aida.histogram2D("Matching/Momentum vs. #Delta y (Positive Bottom Track)", 250, 0.000, 2.500, 160, -40, 40)
            },
            {
                aida.histogram2D("Matching/Momentum vs. #Delta y (Negative Top Track)", 250, 0.000, 2.500, 160, -40, 40),
                aida.histogram2D("Matching/Momentum vs. #Delta y (Negative Bottom Track)", 250, 0.000, 2.500, 160, -40, 40)
            }
    };
    private final IHistogram2D[][] deltaRMomentum = {
            {
                aida.histogram2D("Matching/Momentum vs. #Delta r (Positive Top Track)", 250, 0.000, 2.500, 80, 0, 40),
                aida.histogram2D("Matching/Momentum vs. #Delta r (Positive Bottom Track)", 250, 0.000, 2.500, 80, 0, 40)
            },
            {
                aida.histogram2D("Matching/Momentum vs. #Delta r (Negative Top Track)", 250, 0.000, 2.500, 80, 0, 40),
                aida.histogram2D("Matching/Momentum vs. #Delta r (Negative Bottom Track)", 250, 0.000, 2.500, 80, 0, 40)
            }
    };
    
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
    
    @Override
    public void detectorChanged(Detector detector) {
        System.out.println("Field Map Values:");
        for(int y = 1400; y >= -1400; y -= 25) {
            double[] field = detector.getFieldMap().getField(new double[] { 0, y, 0 });
            System.out.printf("\t(0, %4d, 0) >> (%7.3f, %7.3f, %7.3f)%n", y, field[0], field[1], field[2]);
        }
    }
    
    @Override
    public void startOfData() {
        String[] topBot = { "Top", "Bottom" };
        String[] posNeg = { "Positive", "Negative" };
        for(int i = 0; i < 2; i++) {
            for(int j = 0; j < 2; j++) {
                aida.histogram1D("Debug/Particle x-Momentum (" + posNeg[i] + " " + topBot[j] + " Track)", 500, -2.500, 2.500);
                aida.histogram1D("Debug/Particle y-Momentum (" + posNeg[i] + " " + topBot[j] + " Track)", 200, -0.100, 0.100);
                aida.histogram1D("Debug/Particle z-Momentum (" + posNeg[i] + " " + topBot[j] + " Track)", 200, -0.100, 0.100);
            }
        }
    }
    
    @Override
    public void endOfData() {
        System.out.println("Analysis Results");
        System.out.println("\tPositron Side Cluster Events               :: " + goodClusterEvents);
        System.out.println("\tAnalyzable Track Pair Events               :: " + goodTrackPairEvents);
        System.out.println("\t\tMatched Positron Events                  :: " + matchedPositronEvents);
        System.out.println("\tA Hodoscope Hit Events                     :: " + hodoscopeHitEvents);
        System.out.println("\tA Hodoscope Hit per Layer Events           :: " + twoHodoscopeHitEvents);
        System.out.println("\tA Correlated Hodoscope Hit Pair Events     :: " + correlatedHodoscopeHitEvents);
        System.out.println("\tA Correlated Hodoscope/Cluster Pair Events :: " + correlatedHodoscopeCalorimeterEvents);
        
        IHistogram2D deltaXMomentumAll = aida.histogram2D("Matching/Momentum vs. #Delta x (All)", 250, 0.000, 2.500, 160, -40, 40);
        IHistogram2D deltaYMomentumAll = aida.histogram2D("Matching/Momentum vs. #Delta y (All)", 250, 0.000, 2.500, 160, -40, 40);
        IHistogram2D deltaRMomentumAll = aida.histogram2D("Matching/Momentum vs. #Delta r (All)", 250, 0.000, 2.500, 80, 0, 40);
        
        String[] topBot = { "Top", "Bottom" };
        String[] posNeg = { "Positive", "Negative" };
        IHistogram1D xMomentum = aida.histogram1D("Debug/Particle x-Momentum (All)", 500, -2.500, 2.500);
        IHistogram1D yMomentum = aida.histogram1D("Debug/Particle y-Momentum (All)", 200, -0.100, 0.100);
        IHistogram1D zMomentum = aida.histogram1D("Debug/Particle z-Momentum (All)", 200, -0.100, 0.100);
        for(int i = 0; i < 2; i++) {
            for(int j = 0; j < 2; j++) {
                deltaXMomentumAll.add(deltaXMomentum[i][j]);
                deltaYMomentumAll.add(deltaYMomentum[i][j]);
                deltaRMomentumAll.add(deltaRMomentum[i][j]);
                xMomentum.add(aida.histogram1D("Debug/Particle x-Momentum (" + posNeg[i] + " " + topBot[j] + " Track)"));
                yMomentum.add(aida.histogram1D("Debug/Particle y-Momentum (" + posNeg[i] + " " + topBot[j] + " Track)"));
                zMomentum.add(aida.histogram1D("Debug/Particle z-Momentum (" + posNeg[i] + " " + topBot[j] + " Track)"));
            }
        }
    }
    
    @Override
    public void process(EventHeader event) {
        goodClusterEvents++;
        
        /*
         * Import the necessary collections from the event.
         */
        
        // Get the GBL tracks.
        List<Track> gblTracks = null;
        if(event.hasCollection(Track.class, "GBLTracks")) {
            gblTracks = event.get(Track.class, "GBLTracks");
        } else {
            gblTracks = new ArrayList<Track>(0);
        }
        
        // Get the hodoscope hits.
        List<SimCalorimeterHit> hodoscopeHits = null;
        if(event.hasCollection(SimCalorimeterHit.class, "HodoscopeHits")) {
            hodoscopeHits = event.get(SimCalorimeterHit.class, "HodoscopeHits");
        } else {
            hodoscopeHits = new ArrayList<SimCalorimeterHit>(0);
        }
        List<SimCalorimeterHit> processedHodoscopeHits = TruthModule.preprocessHodoscopeHits(hodoscopeHits);
        
        // Get GTP clusters.
        List<Cluster> gtpClusters = null;
        if(event.hasCollection(Cluster.class, "EcalClustersGTP")) {
            gtpClusters = event.get(Cluster.class, "EcalClustersGTP");
        } else {
            gtpClusters = new ArrayList<Cluster>();
        }
        
        
        
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
        boolean isGoodEvent = (sawBotPositron && sawTopElectron) || (sawTopPositron && sawBotElectron);
        if(!isGoodEvent) { return; }
        goodTrackPairEvents++;
        
        
        
        /*
         * Plot the hodoscope hit layer correlation and distribution
         * of energy.
         */
        
        // Track the momenta of initial particles in this event.
        List<MCParticle> particles = TruthModule.getCollection(event, "MCParticle", MCParticle.class);
        for(MCParticle particle : particles) {
            if((particle.getPDGID() == -11 || particle.getPDGID() == 11) && (particle.getParents().isEmpty() || particle.getParents().get(0).getPDGID() > 600)) {
                mcParticleMomentum.fill(particle.getMomentum().magnitude());
                mcParticleXMomentum.fill(particle.getMomentum().x());
                mcParticleYMomentum.fill(particle.getMomentum().y());
                mcParticleZMomentum.fill(particle.getMomentum().z());
            }
        }
        
        // Fill the hodoscope hit energy distribution plot.
        for(SimCalorimeterHit hit : processedHodoscopeHits) {
            hodoscopeEnergyDistributionAll.fill(hit.getRawEnergy());
        }
        
        // Fill the simple hodoscope layer correlation plot.
        for(SimCalorimeterHit layer1Hit : processedHodoscopeHits) {
            // Only consider layer-1 hits.
            if(TruthModule.getHodoscopeZIndex(layer1Hit) != 0) { continue; }
            
            // Only consider hits with greater than 1 MeV energy.
            if(layer1Hit.getRawEnergy() < 0.001) { continue; }
            
            // Track the y-index. Only correlate hits with the same
            // y-index, as particles should not hit the first layer
            // of the top half of the hodoscope and the second layer
            // of the bottom half of the hodoscope.
            int l1iy = TruthModule.getHodoscopeYIndex(layer1Hit);
            
            // Iterate over the hodoscope hits and look for layer 2
            // hits with which to correlate the layer 1 hit.
            for(SimCalorimeterHit layer2Hit : processedHodoscopeHits) {
                // Only consider layer-2 hits.
                if(TruthModule.getHodoscopeZIndex(layer2Hit) != 1) { continue; }
                
                // Only consider hits with greater than 1 MeV energy.
                if(layer2Hit.getRawEnergy() < 0.001) { continue; }
                
                // Make sure that both hits have the same y-index.
                int l2iy = TruthModule.getHodoscopeYIndex(layer2Hit);
                if(l1iy != l2iy) { continue; }
                
                // Fill the correlation plot.
                hodoscopeLayerCorrelation.fill(TruthModule.getHodoscopeXIndex(layer1Hit), TruthModule.getHodoscopeXIndex(layer2Hit));
            }
        }
        
        
        
        /*
         * Track the number of events that have hodoscope hits which
         * meet varying parameters.
         */
        
        List<Cluster> reconClusters = TruthModule.getCollection(event, "EcalClustersCorr", Cluster.class);
        
        Map<Cluster, Track> matchedPairs = TruthModule.getClusterTrackMatchedPairs(gblTracks, gtpClusters);
        boolean sawMatchedPositron = false;
        for(Track track : matchedPairs.values()) {
            if(TruthModule.getCharge(track) > 0) {
                sawMatchedPositron = true;
                break;
            }
        }
        if(sawMatchedPositron) { matchedPositronEvents++; }
        
        for(Entry<Cluster, Track> entry : matchedPairs.entrySet()) {
            if(TruthModule.getCharge(entry.getValue()) > 0) {
                clusterEnergyIndexCorrelationNoHodo.fill(TriggerModule.getClusterXIndex(entry.getKey()), entry.getKey().getEnergy());
                clusterEnergyPositionCorrelationNoHodo.fill(entry.getKey().getPosition()[0], entry.getKey().getEnergy());
            }
        }
        
        Map<Cluster, Track> matchedReconPairs = TruthModule.getClusterTrackMatchedPairs(gblTracks, reconClusters);
        
        for(Entry<Cluster, Track> entry : matchedReconPairs.entrySet()) {
            if(TruthModule.getCharge(entry.getValue()) > 0) {
                reconClusterEnergyIndexCorrelationNoHodo.fill(TriggerModule.getClusterXIndex(entry.getKey()), entry.getKey().getEnergy());
                reconClusterEnergyPositionCorrelationNoHodo.fill(entry.getKey().getPosition()[0], entry.getKey().getEnergy());
            }
        }
        
        boolean sawLayer1Hit = false;
        boolean sawLayer2Hit = false;
        List<Pair<SimCalorimeterHit, SimCalorimeterHit>> correlatedHitPairs = new ArrayList<Pair<SimCalorimeterHit, SimCalorimeterHit>>();
        for(SimCalorimeterHit layer1Hit : processedHodoscopeHits) {
            if(TruthModule.getHodoscopeZIndex(layer1Hit) == 0) {
                sawLayer1Hit = true;
            } else {
                sawLayer2Hit = true;
                continue;
            }
            
            int l1ix = TruthModule.getHodoscopeXIndex(layer1Hit);
            
            for(SimCalorimeterHit layer2Hit : processedHodoscopeHits) {
                if(TruthModule.getHodoscopeZIndex(layer2Hit) != 1) { continue; }
                
                int l2ix = TruthModule.getHodoscopeXIndex(layer2Hit);
                
                if(hodoHodoCorrelationTable[l1ix][0] <= l2ix && hodoHodoCorrelationTable[l1ix][1] >= l2ix
                        && layer1Hit.getRawEnergy() >= 0.001 && layer2Hit.getRawEnergy() >= 0.001) {
                    correlatedHitPairs.add(new Pair<SimCalorimeterHit, SimCalorimeterHit>(layer1Hit, layer2Hit));
                }
            }
        }
        
        if(sawLayer1Hit || sawLayer2Hit) { hodoscopeHitEvents++; }
        if(sawLayer1Hit && sawLayer2Hit) { twoHodoscopeHitEvents++; }
        if(!correlatedHitPairs.isEmpty()) {
            correlatedHodoscopeHitEvents++;
            
            //Map<Cluster, Track> matchedPairs = TruthModule.getClusterTrackMatchedPairs(gblTracks, gtpClusters);
            
            for(Entry<Cluster, Track> entry : matchedPairs.entrySet()) {
                if(TruthModule.getCharge(entry.getValue()) > 0) {
                    clusterEnergyIndexCorrelation.fill(TriggerModule.getClusterXIndex(entry.getKey()), entry.getKey().getEnergy());
                    clusterEnergyPositionCorrelation.fill(entry.getKey().getPosition()[0], entry.getKey().getEnergy());
                }
            }
            
            for(Entry<Cluster, Track> entry : matchedReconPairs.entrySet()) {
                if(TruthModule.getCharge(entry.getValue()) > 0) {
                    reconClusterEnergyIndexCorrelation.fill(TriggerModule.getClusterXIndex(entry.getKey()), entry.getKey().getEnergy());
                    reconClusterEnergyPositionCorrelation.fill(entry.getKey().getPosition()[0], entry.getKey().getEnergy());
                }
            }
        }
        
        for(Cluster cluster : gtpClusters) {
            clusterEnergyIndexCorrelationAll.fill(TriggerModule.getClusterXIndex(cluster), cluster.getEnergy());
            clusterEnergyPositionCorrelationAll.fill(cluster.getPosition()[0], cluster.getEnergy());
        }
        
        for(Cluster cluster : reconClusters) {
            reconClusterEnergyIndexCorrelationAll.fill(TriggerModule.getClusterXIndex(cluster), cluster.getEnergy());
            reconClusterEnergyPositionCorrelationAll.fill(cluster.getPosition()[0], cluster.getEnergy());
        }
        
        String[] topBotString = { "Top", "Bottom" };
        String[] posNegString = { "Positive", "Negative" };
        for(Track track : gblTracks) {
            double[] tr = TruthModule.getTrackPositionAtCalorimeterFace(track);
            int topBot = tr[1] > 0 ? 0 : 1;
            int posNeg = TruthModule.getCharge(track) > 0 ? 0 : 1;
            
            final double aa = 3.e-4;
            final double B_PSpec = 0.5242301;
            final double a_Bz = aa*B_PSpec;
            
            double phi = track.getTrackStates().get(0).getPhi();
            double omega = track.getTrackStates().get(0).getOmega();
            double tanLambda = track.getTrackStates().get(0).getTanLambda();
            
            double magP = a_Bz / Math.abs(omega);
            
            double px = magP * Math.cos(phi);
            double py = magP * Math.sin(phi);
            double pz = magP * tanLambda;
            
            boolean isTopTrack = TruthModule.isTopTrack(track);
            
            for(Cluster cluster : gtpClusters) {
                double[] cr = cluster.getPosition();
                
                if((cr[1] > 0 && !isTopTrack) || (cr[1] < 0 && isTopTrack)) { continue; }
                
                double deltaX = cr[0] - tr[0];
                double deltaY = cr[1] - tr[1];
                double deltaR = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                
                deltaXMomentum[posNeg][topBot].fill(magP, deltaX);
                deltaYMomentum[posNeg][topBot].fill(magP, deltaY);
                deltaRMomentum[posNeg][topBot].fill(magP, deltaR);
            }
            
            aida.histogram1D("Debug/Particle x-Momentum (" + posNegString[posNeg] + " " + topBotString[topBot] + " Track)").fill(px);
            aida.histogram1D("Debug/Particle y-Momentum (" + posNegString[posNeg] + " " + topBotString[topBot] + " Track)").fill(py);
            aida.histogram1D("Debug/Particle z-Momentum (" + posNegString[posNeg] + " " + topBotString[topBot] + " Track)").fill(pz);
        }
        
        /*
         * Track the number of events where at least one pair of
         * correlated hodoscope hits also meets the correlation table
         * conditions for a calorimeter cluster.
         */
        
        // Plot the correlation between all permutations of hodoscope
        // hits and clusters.
        for(SimCalorimeterHit hit : processedHodoscopeHits) {
            int ix = TruthModule.getHodoscopeXIndex(hit);
            int iy = TruthModule.getHodoscopeYIndex(hit);
            int iz = TruthModule.getHodoscopeZIndex(hit);
            
            if(hit.getRawEnergy() < 0.001) { continue; }
            
            for(Cluster cluster : gtpClusters) {
                if(cluster.getEnergy() < 0.200) { continue; }
                
                if(iz == 0) {
                    hodoscopeL1ClusterXAllCorrelation.fill(ix, TriggerModule.getClusterXIndex(cluster));
                    hodoscopeL1ClusterYAllCorrelation.fill(iy, TriggerModule.getClusterYIndex(cluster));
                } else {
                    hodoscopeL2ClusterXAllCorrelation.fill(ix, TriggerModule.getClusterXIndex(cluster));
                    hodoscopeL2ClusterYAllCorrelation.fill(iy, TriggerModule.getClusterYIndex(cluster));
                }
            }
        }
        
        // Plot the correlation between all paired hodoscope hits and
        // calorimeter clusters.
        Map<SimCalorimeterHit, Set<Cluster>> plotMap = new HashMap<SimCalorimeterHit, Set<Cluster>>();
        for(Pair<SimCalorimeterHit, SimCalorimeterHit> hodoPair : correlatedHitPairs) {
            for(Cluster cluster : gtpClusters) {
                if(cluster.getEnergy() < 0.200) { continue; }
                
                storeHitCluster(plotMap, hodoPair.getFirstElement(), cluster);
                storeHitCluster(plotMap, hodoPair.getSecondElement(), cluster);
            }
        }
        
        for(Entry<SimCalorimeterHit, Set<Cluster>> entry : plotMap.entrySet()) {
            for(Cluster cluster : entry.getValue()) {
                if(TruthModule.getHodoscopeZIndex(entry.getKey()) == 0) {
                    hodoscopeL1ClusterXPairedCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                    hodoscopeL1ClusterYPairedCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                } else {
                    hodoscopeL2ClusterXPairedCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                    hodoscopeL2ClusterYPairedCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                }
            }
        }
        
        // Plot the correlation between paired hodoscope hits and all
        // clusters which pass the table relations for both paired of
        // the paired hits.
        plotMap.clear();
        Map<SimCalorimeterHit, Set<Cluster>> badPlotMap = new HashMap<SimCalorimeterHit, Set<Cluster>>();
        boolean sawCorrelatedCluster = false;
        for(Pair<SimCalorimeterHit, SimCalorimeterHit> hodoPair : correlatedHitPairs) {
            int l1ix = TruthModule.getHodoscopeXIndex(hodoPair.getFirstElement());
            int l2ix = TruthModule.getHodoscopeXIndex(hodoPair.getSecondElement());
            
            for(Cluster cluster : gtpClusters) {
                if(cluster.getEnergy() < 0.200) { continue; }
                
                int clix = TriggerModule.getClusterXIndex(cluster);
                
                boolean layer1Correlates = hodoEcalCorrelationTable[0][l1ix][0] <= clix && hodoEcalCorrelationTable[0][l1ix][1] >= clix;
                boolean layer2Correlates = hodoEcalCorrelationTable[1][l2ix][0] <= clix && hodoEcalCorrelationTable[1][l2ix][1] >= clix;
                if(layer1Correlates && layer2Correlates) {
                    sawCorrelatedCluster = true;
                    storeHitCluster(plotMap, hodoPair.getFirstElement(), cluster);
                    storeHitCluster(plotMap, hodoPair.getSecondElement(), cluster);
                } else {
                    storeHitCluster(badPlotMap, hodoPair.getFirstElement(), cluster);
                    storeHitCluster(badPlotMap, hodoPair.getSecondElement(), cluster);
                }
            }
        }
        
        if(sawCorrelatedCluster) {
            correlatedHodoscopeCalorimeterEvents++;
        } else {
            Map<SimCalorimeterHit, Set<Cluster>> totalBadPlotMap = new HashMap<SimCalorimeterHit, Set<Cluster>>();
            Map<SimCalorimeterHit, Set<Cluster>> totalBadECutPlotMap = new HashMap<SimCalorimeterHit, Set<Cluster>>();
            for(Pair<SimCalorimeterHit, SimCalorimeterHit> hodoPair : correlatedHitPairs) {
                int l1ix = TruthModule.getHodoscopeXIndex(hodoPair.getFirstElement());
                int l2ix = TruthModule.getHodoscopeXIndex(hodoPair.getSecondElement());
                
                for(Cluster cluster : gtpClusters) {
                    int clix = TriggerModule.getClusterXIndex(cluster);
                    
                    boolean layer1Correlates = hodoEcalCorrelationTable[0][l1ix][0] <= clix && hodoEcalCorrelationTable[0][l1ix][1] >= clix;
                    boolean layer2Correlates = hodoEcalCorrelationTable[1][l2ix][0] <= clix && hodoEcalCorrelationTable[1][l2ix][1] >= clix;
                    if(!layer1Correlates || !layer2Correlates) {
                        storeHitCluster(totalBadPlotMap, hodoPair.getFirstElement(), cluster);
                        storeHitCluster(totalBadPlotMap, hodoPair.getSecondElement(), cluster);
                        
                        if(cluster.getEnergy() >= 0.200) {
                            storeHitCluster(totalBadECutPlotMap, hodoPair.getFirstElement(), cluster);
                            storeHitCluster(totalBadECutPlotMap, hodoPair.getSecondElement(), cluster);
                        }
                    }
                }
            }
            
            for(Entry<SimCalorimeterHit, Set<Cluster>> entry : totalBadPlotMap.entrySet()) {
                for(Cluster cluster : entry.getValue()) {
                    if(TruthModule.getHodoscopeZIndex(entry.getKey()) == 0) {
                        hodoscopeL1ClusterXTotalFailTableCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                        hodoscopeL1ClusterYTotalFailTableCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                    } else {
                        hodoscopeL2ClusterXTotalFailTableCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                        hodoscopeL2ClusterYTotalFailTableCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                    }
                }
            }
            
            for(Entry<SimCalorimeterHit, Set<Cluster>> entry : totalBadECutPlotMap.entrySet()) {
                for(Cluster cluster : entry.getValue()) {
                    if(TruthModule.getHodoscopeZIndex(entry.getKey()) == 0) {
                        hodoscopeL1ClusterXTotalFailTableECutCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                        hodoscopeL1ClusterYTotalFailTableECutCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                    } else {
                        hodoscopeL2ClusterXTotalFailTableECutCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                        hodoscopeL2ClusterYTotalFailTableECutCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                    }
                }
            }
        }
        
        for(Entry<SimCalorimeterHit, Set<Cluster>> entry : plotMap.entrySet()) {
            for(Cluster cluster : entry.getValue()) {
                if(TruthModule.getHodoscopeZIndex(entry.getKey()) == 0) {
                    hodoscopeL1ClusterXTableCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                    hodoscopeL1ClusterYTableCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                } else {
                    hodoscopeL2ClusterXTableCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                    hodoscopeL2ClusterYTableCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                }
            }
        }
        
        for(Entry<SimCalorimeterHit, Set<Cluster>> entry : badPlotMap.entrySet()) {
            for(Cluster cluster : entry.getValue()) {
                if(TruthModule.getHodoscopeZIndex(entry.getKey()) == 0) {
                    hodoscopeL1ClusterXFailTableCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                    hodoscopeL1ClusterYFailTableCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                } else {
                    hodoscopeL2ClusterXFailTableCorrelation.fill(TruthModule.getHodoscopeXIndex(entry.getKey()), TriggerModule.getClusterXIndex(cluster));
                    hodoscopeL2ClusterYFailTableCorrelation.fill(TruthModule.getHodoscopeYIndex(entry.getKey()), TriggerModule.getClusterYIndex(cluster));
                }
            }
        }
    }
    
    private static final <T, V> void storeHitCluster(Map<T, Set<V>> map, T key, V valueMember) {
        if(map.containsKey(key)) {
            map.get(key).add(valueMember);
        } else {
            Set<V> valueSet = new HashSet<V>();
            valueSet.add(valueMember);
            map.put(key, valueSet);
        }
    }
}