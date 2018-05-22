package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hps.recon.tracking.BeamlineConstants;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.TriggerModule;
import org.hps.util.Pair;
import org.hps.util.TruthCalorimeterHit;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

public class TruthModule {
    public static final Map<MCParticle, Integer> preprocessMCParticles(List<SimTrackerHit> trackerHits) {
        // Iterate over the tracker hits and determine how many
        // layers of the SVT a given particle passes through.
        Map<MCParticle, boolean[]> particleLayerMap = new HashMap<MCParticle, boolean[]>();
        for(SimTrackerHit trackerHit : trackerHits) {
            if(particleLayerMap.containsKey(trackerHit.getMCParticle())) {
                boolean[] layerFlags = particleLayerMap.get(trackerHit.getMCParticle());
                layerFlags[TruthModule.getSVTLayerNumber(trackerHit) - 1] = true;
            } else {
                boolean[] layerFlags = new boolean[6];
                layerFlags[TruthModule.getSVTLayerNumber(trackerHit) - 1] = true;
                particleLayerMap.put(trackerHit.getMCParticle(), layerFlags);
            }
        }
        
        // Store the total number of layers traversed for each of the
        // tracker particles.
        Map<MCParticle, Integer> particleTraversalMap = new HashMap<MCParticle, Integer>();
        for(Map.Entry<MCParticle, boolean[]> entry : particleLayerMap.entrySet()) {
            particleTraversalMap.put(entry.getKey(), Integer.valueOf(TruthModule.getLayersTraversed(entry.getValue())));
        }
        
        // Return the map.
        return particleTraversalMap;
    }
    
    public static final List<SimCalorimeterHit> preprocessHodoscopeHits(List<SimCalorimeterHit> hodoscopeHits) {
        // Store all of the hodoscope truth hits on the same crystal
        // in a single set to be combined later.
        IndexMap<List<SimCalorimeterHit>> crystalMap = new IndexMap<List<SimCalorimeterHit>>();
        for(SimCalorimeterHit hodoscopeHit : hodoscopeHits) {
            int[] crystalIndices = TruthModule.getHodoscopeIndices(hodoscopeHit);
            if(crystalMap.containsKey(crystalIndices)) {
                List<SimCalorimeterHit> indexHits = crystalMap.get(crystalIndices);
                indexHits.add(hodoscopeHit);
            } else {
                List<SimCalorimeterHit> indexHits = new ArrayList<SimCalorimeterHit>();
                indexHits.add(hodoscopeHit);
                crystalMap.put(crystalIndices, indexHits);
            }
        }
        
        // Fuse all of the hodoscope hits into one SimCalorimeterHit
        // for each crystal.
        List<SimCalorimeterHit> processedHodoscopeHits = new ArrayList<SimCalorimeterHit>(crystalMap.keySet().size());
        for(List<SimCalorimeterHit> crystalList : crystalMap.values()) {
            processedHodoscopeHits.add(TruthModule.combineHits(crystalList));
        }
        
        // Return the hits.
        return processedHodoscopeHits;
    }
    
    public static final Map<SimCalorimeterHit, Map<MCParticle, Double>> getHodoscopeHitParticleEnergyContributionMap(List<SimCalorimeterHit> hits) {
        Map<SimCalorimeterHit, Map<MCParticle, Double>> incidentParticleContributionMap = new HashMap<SimCalorimeterHit, Map<MCParticle, Double>>(hits.size());
        for(SimCalorimeterHit hit : hits) {
            Map<MCParticle, Double>  percentEnergyMap = TruthModule.getIncidentHodoscopeParticlePercentEnergyContribution(hit);
            incidentParticleContributionMap.put(hit, percentEnergyMap);
        }
        
        return incidentParticleContributionMap;
    }
    
    public static final Map<Cluster, Map<MCParticle, Double>> getClusterParticleEnergyContributionMap(List<Cluster> clusters) {
        Map<Cluster, Map<MCParticle, Double>> clusterParticleContributionMap = new HashMap<Cluster, Map<MCParticle, Double>>(clusters.size());
        for(Cluster reconCluster : clusters) {
            Map<MCParticle, Double> percentEnergyMap = TruthModule.getIncidentClusterParticlePercentEnergyContribution(reconCluster);
            clusterParticleContributionMap.put(reconCluster, percentEnergyMap);
            
            System.out.println("\t" + TruthModule.getClusterString(reconCluster));
            for(Map.Entry<MCParticle, Double> entry : percentEnergyMap.entrySet()) {
                System.out.printf("\t\t%6.2f%% :: %s%n", entry.getValue().doubleValue() * 100.0, TruthModule.getParticleString(entry.getKey()));
            }
        }
        
        return clusterParticleContributionMap;
    }
    
    public static final Map<Cluster, Track> getClusterTrackMatchedPairs(List<Track> tracks, List<Cluster> clusters) {
        Map<Cluster, Track> clusterTrackMap = new HashMap<Cluster, Track>();
        for(Track track : tracks) {
            double[] r = TrackUtils.extrapolateTrack(track, BeamlineConstants.ECAL_FACE).v();
            
            // Search for a nearby cluster.
            Cluster bestCluster = null;
            double bestDeltaR = Double.MAX_VALUE;
            for(Cluster cluster : clusters) {
                double deltaX = cluster.getPosition()[0] - r[0];
                double deltaY = cluster.getPosition()[1] - r[1];
                double deltaR = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                boolean meetsXCondition = Math.abs(cluster.getPosition()[0] - r[0]) < 6.0 + 3.9 / (magnitude(track.getTrackStates().get(TrackState.AtCalorimeter).getMomentum()) - 0.3);
                boolean meetsYCondition = Math.abs(cluster.getPosition()[1] - r[1]) < 15.0;
                if(meetsXCondition && meetsYCondition) {
                    if(deltaR < bestDeltaR) {
                        bestDeltaR = deltaR;
                        bestCluster = cluster;
                    }
                }
            }
            
            if(bestDeltaR < 10.0) {
                clusterTrackMap.put(bestCluster, track);
            }
        }
        
        return clusterTrackMap;
    }
    
    public static final boolean isTopTrack(Track track) {
        return track.getTrackStates().get(0).getTanLambda() > 0;
    }
    
    public static final boolean isBottomTrack(Track track) {
        return !isTopTrack(track);
    }
    
    public static final double magnitude(double[] v) {
        double sum = 0.0;
        for(double c : v) {
            sum += Math.pow(c, 2);
        }
        return Math.sqrt(sum);
    }
    
    public static final double[] getTrackPositionAtCalorimeterFace(Track track) {
        return TrackUtils.extrapolateTrack(track, BeamlineConstants.ECAL_FACE).v();
    }
    
    public static final Map<Cluster, Cluster> mapReconToGTP(Collection<Cluster> reconClusters, Collection<Cluster> gtpClusters) {
        Map<Cluster, Cluster> clusterMap = new HashMap<Cluster, Cluster>();
        Set<Cluster> matchedGTPClusters = new HashSet<Cluster>();
        for(Cluster reconCluster : reconClusters) {
            Set<Cluster> channelClusters = new HashSet<Cluster>();
            for(Cluster gtpCluster : gtpClusters) {
                if(gtpCluster.getCalorimeterHits().get(0).getCellID() == reconCluster.getCalorimeterHits().get(0).getCellID()) {
                    channelClusters.add(gtpCluster);
                }
            }
            
            double deltaE = Double.MAX_VALUE;
            Cluster bestCluster = null;
            for(Cluster channelCluster : channelClusters) {
                if(matchedGTPClusters.contains(channelCluster)) { continue; }
                double energyDifference = reconCluster.getEnergy() - channelCluster.getEnergy();
                if(energyDifference < deltaE) {
                    deltaE = energyDifference;
                    bestCluster = channelCluster;
                }
            }
            
            if(bestCluster != null) {
                clusterMap.put(reconCluster, bestCluster);
                matchedGTPClusters.add(bestCluster);
            }
        }
        
        return clusterMap;
    }
    
    public static final MCParticle getHighestContributingParticle(Map<MCParticle, Double> percentEnergyMap) {
        if(percentEnergyMap == null || percentEnergyMap.isEmpty()) {
            return null;
        }
        
        double highestEnergy = Double.MIN_VALUE;
        MCParticle highestParticle = null;
        for(Map.Entry<MCParticle, Double> entry : percentEnergyMap.entrySet()) {
            if(entry.getValue().doubleValue() > highestEnergy) {
                highestEnergy = entry.getValue().doubleValue();
                highestParticle = entry.getKey();
            }
        }
        
        return highestParticle;
    }
    
    public static final <T extends Number> List<Pair<MCParticle, T>> asOrderedList(Map<MCParticle, T> map) {
        // Add all of the map entries to a list as pairs.
        List<Pair<MCParticle, T>> list = new ArrayList<Pair<MCParticle, T>>(map.size());
        for(Entry<MCParticle, T> entry : map.entrySet()) {
            list.add(new Pair<MCParticle, T>(entry.getKey(), entry.getValue()));
        }
        
        // Sort the list by the value of the number.
        Collections.sort(list, new Comparator<Pair<MCParticle, T>>() {
            @Override
            public int compare(Pair<MCParticle, T> arg0, Pair<MCParticle, T> arg1) {
                return Double.compare(arg0.getSecondElement().doubleValue(), arg1.getSecondElement().doubleValue());
            }
        });
        
        // Return the sorted list.
        return list;
    }
    
    public static final Map<MCParticle, Double> getIncidentClusterParticleEnergyContribution(Cluster truthCluster) {
        return getIncidentClusterParticleEnergyContribution(truthCluster, false);
    }
    
    public static final Map<MCParticle, Double> getIncidentClusterParticlePercentEnergyContribution(Cluster truthCluster) {
        return getIncidentClusterParticleEnergyContribution(truthCluster, true);
    }
    
    private static final Map<MCParticle, Double> getIncidentClusterParticleEnergyContribution(Cluster truthCluster, boolean usePercent) {
        // Track the total contributed energy.
        double totalEnergy = 0.0;
        
        // Map the incident particle to its contributed energy.
        Map<MCParticle, Double> particleEnergyMap = new HashMap<MCParticle, Double>();
        
        // Combine the truth information of all truth hits that make
        // up the cluster.
        for(CalorimeterHit hit : truthCluster.getCalorimeterHits()) {
            // If truth information is missing, this process can not
            // be performed.
            if(!(hit instanceof SimCalorimeterHit)) {
                throw new RuntimeException("Calorimeter hits are missing truth information.");
            }
            
            // Get the truth hit.
            SimCalorimeterHit truthHit = (SimCalorimeterHit) hit;
            
            // Iterate over the particle contributions.
            for(int i = 0; i < truthHit.getMCParticleCount(); i++) {
                // Add this particle's contribution to the total
                // energy contribution.
                totalEnergy += truthHit.getContributedEnergy(i);
                
                // Get the incident particle and track its specific
                // contribution to the total.
                MCParticle incidentParticle = getIncidentEcalParticle(truthHit.getMCParticle(i));
                if(particleEnergyMap.containsKey(incidentParticle)) {
                    particleEnergyMap.put(incidentParticle, Double.valueOf(particleEnergyMap.get(incidentParticle).doubleValue() + truthHit.getContributedEnergy(i)));
                } else {
                    particleEnergyMap.put(incidentParticle, Double.valueOf(truthHit.getContributedEnergy(i)));
                }
            }
        }
        
        // Scale each particle's contribution by the total amount of
        // contributed energy to get the percent, if this is desired.
        if(usePercent) {
            for(Map.Entry<MCParticle, Double> entry : particleEnergyMap.entrySet()) {
                entry.setValue(Double.valueOf(entry.getValue().doubleValue() / totalEnergy));
            }
        }
        
        // Return the result.
        return particleEnergyMap;
    }
    
    public static final Map<MCParticle, Double> getIncidentHodoscopeParticlePercentEnergyContribution(SimCalorimeterHit hit) {
        // Track the energy.
        double totalEnergy = 0.0;
        
        // Map each incident particle to the total energy it has
        // contributed to the hit.
        Map<MCParticle, Double> particleTotalEnergyMap = new HashMap<MCParticle, Double>();
        
        // Iterate over the hits. Store the total contributed energy
        // across all particles and also store what amount of that is
        // from each incident particle.
        for(int i = 0; i < hit.getMCParticleCount(); i++) {
            totalEnergy += hit.getContributedEnergy(i);
            MCParticle originParticle = getIncidentHodoscopeParticle(hit.getMCParticle(i));
            if(particleTotalEnergyMap.containsKey(originParticle)) {
                double newTotal = hit.getContributedEnergy(i) + particleTotalEnergyMap.get(originParticle).doubleValue();
                particleTotalEnergyMap.put(originParticle, Double.valueOf(newTotal));
            } else {
                particleTotalEnergyMap.put(originParticle, Double.valueOf(hit.getContributedEnergy(i)));
            }
        }
        
        // Weight the energy associated with each incident particle
        // by the total to get a percent.
        for(Map.Entry<MCParticle, Double> entry : particleTotalEnergyMap.entrySet()) {
            entry.setValue(Double.valueOf(entry.getValue().doubleValue() / totalEnergy));
        }
        
        // Return the result.
        return particleTotalEnergyMap;
    }
    
    public static final MCParticle getIncidentEcalParticle(MCParticle particle) {
        // The calorimeter face occurs at approximately 1318 mm. We
        // allow a little extra distance for safety.
        final int ecalFace = 1300;
        
        // Check the position of the particle's production vertex. If
        // it is within the calorimeter, get its parent and perform
        // the same test. Repeat until the current particle is not
        // produced within the calorimeter.
        MCParticle curParticle = particle;
        while(true) {
            // Particles are expected to only ever have one parent.
            if(curParticle.getParents().size() != 1) {
                throw new RuntimeException("Error: Particles are expected to have either 0 or 1 parent(s) - saw "
                        + particle.getParents().size() + ".");
            }
            
            // If the particle was created before the calorimeter
            // face, this is the "final" particle.
            if(curParticle.getOriginZ() < ecalFace) {
                break;
            }
            
            // Otherwise, get the particle's parent and return that.
            // Note that the A' should never be returned, so if the
            // parent is the A', just return the current particle.
            if(curParticle.getParents().get(0).getPDGID() == 622) {
                break;
            } else {
                curParticle = curParticle.getParents().get(0);
            }
        }
        
        // Return the particle
        return curParticle;
    }
    
    public static final MCParticle getIncidentHodoscopeParticle(MCParticle particle) {
        // The incident hodoscope particle is argument particle, if
        // that particle originated at a position upstream of the
        // hodoscope. Otherwise, it is the first parent particle that
        // meets this condition.
        if(particle.getOriginZ() < 1095) { return particle; }
        
        else {
            MCParticle curParticle = particle;
            while(curParticle.getOriginZ() >= 1095) {
                // Particles are expected to only have one parent.
                if(curParticle.getParents().size() > 1) {
                    throw new RuntimeException("Particles are expected to have either one or no parent(s).");
                }
                
                // A particle with no parents is the origin particle
                // and should be upstream of the hodoscope.
                else if(curParticle.getParents().size() == 0) {
                    return curParticle;
                }
                
                // Otherwise, get the parent and check its origin.
                else {
                    curParticle = curParticle.getParents().get(0);
                }
            }
            
            // Return the particle.
            return curParticle;
        }
    }
    
    public static final SimCalorimeterHit combineHits(Collection<SimCalorimeterHit> hits) {
        // If the list is empty or null, return null.
        if(hits == null || hits.isEmpty()) { return null; }
        
        // The ID and type are not really used. Just define them as
        // the same value as whatever hit is first in the list. The
        // meta data should be the same for all the hits.
        SimCalorimeterHit firstHit = null;
        
        // Track combined values for the new hit.
        double rawEnergy = 0.0;
        double correctedEnergy = 0.0;
        
        // Track the truth contributions of each particle.
        List<TruthContribution> truthContributions = new ArrayList<TruthContribution>();
        
        // Iterate over the hits and extract the information.
        for(SimCalorimeterHit hit : hits) {
            rawEnergy += hit.getRawEnergy();
            correctedEnergy += hit.getCorrectedEnergy();
            
            for(int i = 0; i < hit.getMCParticleCount(); i++) {
                truthContributions.add(new TruthContribution(hit.getMCParticle(i), (float) hit.getContributedEnergy(i),
                        (float) hit.getContributedTime(i), hit.getMCParticle(i).getPDGID()));
            }
            
            // The basic hit parameters should copy those of the
            // first hit. Aside from possibly ID, these should really
            // all be the same for all hits.
            if(firstHit == null) { firstHit = hit; }
        }
        
        // Sort the truth contributions by energy.
        Collections.sort(truthContributions);
        
        // Create the truth contribution arrays.
        int[] pdgs = new int[truthContributions.size()];
        float[] times = new float[truthContributions.size()];
        float[] energies = new float[truthContributions.size()];
        Object[] mcParticles = new Object[truthContributions.size()];
        for(int i = 0; i < truthContributions.size(); i++) {
            pdgs[i] = truthContributions.get(i).getPDGID();
            times[i] = truthContributions.get(i).getTime();
            energies[i] = truthContributions.get(i).getEnergy();
            mcParticles[i] = truthContributions.get(i).getParticle();
        }
        
        // Make a new particle.
        SimCalorimeterHit newHit = new TruthCalorimeterHit(rawEnergy, correctedEnergy, 0, firstHit.getTime(), firstHit.getCellID(),
                firstHit.getPositionVec(), firstHit.getType(), mcParticles, energies, times, pdgs, firstHit.getMetaData());
        
        // Return it.
        return newHit;
    }

    public static final int getLayersTraversed(boolean[] layerFlags) {
        int layersTraversed = 0;
        for(boolean b : layerFlags) {
            if(b) { layersTraversed++; }
        }
        
        return layersTraversed;
    }
    
    public static final double getTruthEnergy(Cluster cluster) {
        double energy = 0.0;
        
        for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
            if(!(hit instanceof SimCalorimeterHit)) {
                throw new RuntimeException("Cluster does not contain truth information.");
            }
            
            SimCalorimeterHit truthHit = (SimCalorimeterHit) hit;
            energy += getTruthEnergy(truthHit);
        }
        
        return energy;
    }
    
    public static final double getTruthEnergy(SimCalorimeterHit truthHit) {
        double energy = 0.0;
        
        for(int i = 0; i < truthHit.getMCParticleCount(); i++) {
            energy += truthHit.getContributedEnergy(i);
        }
        
        return energy;
    }
    
    public static final int getSVTLayerNumber(SimTrackerHit hit) {
        int layerNumber = hit.getLayerNumber();
        if(layerNumber % 2 == 0) {
            return layerNumber / 2;
        } else {
            return (layerNumber + 1) / 2;
        }
    }
    
    public static final int[] getHodoscopeIndices(SimCalorimeterHit hodoscopeHit) {
        return new int[] { getHodoscopeXIndex(hodoscopeHit), getHodoscopeYIndex(hodoscopeHit), getHodoscopeZIndex(hodoscopeHit) };
    }
    
    public static final int getHodoscopeXIndex(SimCalorimeterHit hodoscopeHit) {
        double x = hodoscopeHit.getPosition()[0];
        int iz = getHodoscopeZIndex(hodoscopeHit);
        if(iz == 0) {
            //if(x < 60.5) { return Integer.MIN_VALUE; }
            if(x >=  60.5 && x <  76.5) { return 0; }
            else if(x >=  76.5 && x < 110.6) { return 1; }
            else if(x >= 110.6 && x < 154.7) { return 2; }
            else if(x >= 154.7 && x < 198.9) { return 3; }
            else if(x >= 198.9 && x < 242.0) { return 4; }
            //else { return Integer.MAX_VALUE; }
        } else {
            //if(x < 69.1) { return Integer.MIN_VALUE; }
            if(x >=  69.1 && x <  88.6) { return 0; }
            else if(x >=  88.6 && x < 132.7) { return 1; }
            else if(x >= 132.7 && x < 176.8) { return 2; }
            else if(x >= 176.8 && x < 220.4) { return 3; }
            else if(x >= 220.4 && x < 251.5) { return 4; }
            //else { return Integer.MAX_VALUE; }
        }
        
        throw new RuntimeException("Unexpected hodoscope hit x-position " + x + ".");
    }
    
    public static final int getHodoscopeYIndex(SimCalorimeterHit hodoscopeHit) {
        if(hodoscopeHit.getPosition()[1] > 0) {
            return 1;
        } else {
            return -1;
        }
    }
    
    public static final int getHodoscopeZIndex(SimCalorimeterHit hodoscopeHit) {
        if(hodoscopeHit.getPosition()[2] == 1095.0) {
            return 0;
        } else if(hodoscopeHit.getPosition()[2] == 1107.0) {
            return 1;
        } else {
            throw new RuntimeException("Unrecognized z-coordinate " + hodoscopeHit.getPosition()[2] + ".");
        }
    }
    
    public static final String getParticleString(MCParticle particle) {
        if(particle == null) {
            return "Particle with PDGID UNDEFINED produced at time t = NaN ns with charge NaN C and momentum NaN GeV.";
        } else {
            String particleName = null;
            int pid = particle.getPDGID();
            if(pid == 11) { particleName = "e-"; }
            else if(pid == -11) { particleName = "e+"; }
            else if(pid == 22) { particleName = "g"; }
            else if(pid == 622) { particleName = "A'"; }
            else { particleName = Integer.toString(pid); }
            
            return String.format("Particle of type %3s produced at time t = %5.1f ns and vertex <%6.1f, %6.1f, %6.1f> with charge %2.0f C and momentum %5.3f GeV.",
                    particleName, particle.getProductionTime(), particle.getOriginX(), particle.getOriginY(), particle.getOriginZ(), particle.getCharge(),
                    particle.getMomentum().magnitude());
        }
    }
    
    public static final String getClusterString(Cluster cluster) {
        if(cluster == null) {
            return "Cluster at UNDEFINED with energy NaN GeV and size NaN hit(s) at time NaN ns.";
        } else {
            if(!cluster.getCalorimeterHits().isEmpty() && cluster.getCalorimeterHits().get(0) instanceof SimCalorimeterHit) {
                return String.format("Cluster at (%3d, %2d) with energy %5.3f GeV (truth energy %5.3f GeV) and size %d hit(s) at time %.2f ns.",
                        TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster),
                        cluster.getEnergy(), getTruthEnergy(cluster), cluster.getCalorimeterHits().size(), cluster.getCalorimeterHits().get(0).getTime());
            } else {
                return String.format("Cluster at (%3d, %2d) with energy %5.3f GeV and size %d hit(s) at time %.2f ns.",
                        TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster),
                        cluster.getEnergy(), cluster.getCalorimeterHits().size(), cluster.getCalorimeterHits().get(0).getTime());
            }
        }
    }
    
    public static final String getEcalHitString(CalorimeterHit hit) {
        if(hit == null) {
            return "Hit at UNDEFINED with energy NaN GeV at time NaN ns.";
        } else {
            if(hit instanceof SimCalorimeterHit) {
                SimCalorimeterHit truthHit = (SimCalorimeterHit) hit;
                return String.format("Hit at (%3d, %3d) with energy %5.3f GeV (truth energy %5.3f GeV) at time %.1f ns.", hit.getIdentifierFieldValue("ix"),
                        hit.getIdentifierFieldValue("iy"), hit.getRawEnergy(), getTruthEnergy(truthHit), hit.getTime());
            } else {
                return String.format("Hit at (%3d, %3d) with energy %5.3f GeV at time %.1f ns.", hit.getIdentifierFieldValue("ix"),
                        hit.getIdentifierFieldValue("iy"), hit.getRawEnergy(), hit.getTime());
            }
        }
    }
    
    public static final String getHodoscopeHitString(SimCalorimeterHit hit) {
        if(hit == null) {
            return "Hit at UNDEFINED with energy NaN GeV at time NaN ns.";
        } else {
            return String.format("Hit at (%3d, %2d, %1d) with energy %5.3f GeV at time %.1f ns.", getHodoscopeXIndex(hit),
                    getHodoscopeYIndex(hit), getHodoscopeZIndex(hit), hit.getRawEnergy(), hit.getTime());
        }
    }
    
    public static final String getTrackString(Track track) {
        if(track == null) {
            return "Track with momentum NaN GeV and charge NaN C with chi2 NaN and nDF = NaN.";
        } else {
            double p[] = track.getTrackStates().get(0).getMomentum();
            String tanLambdaSign = null;
            if(track.getTrackStates().get(0).getTanLambda() > 0) { tanLambdaSign = "+"; }
            else if(track.getTrackStates().get(0).getTanLambda() < 0) { tanLambdaSign = "-"; }
            else { tanLambdaSign = "0"; }
            
            String charge = getCharge(track) > 0 ? "+" : "-";
            
            return String.format("Track with momentum <%6.3f, %6.3f, %6.3f> GeV and %s charge with chi2 %5.2f, nDF = %2d, and tan(L) = %s", p[0],
                    p[1], p[2], charge, track.getChi2(), track.getNDF(), tanLambdaSign);
        }
    }
    
    public static final int getCharge(Track track) {
        return (int) -Math.signum(track.getTrackStates().get(0).getOmega());
    }
    
    public static final <T> List<T> getCollection(EventHeader event, String collectionName, Class<T> objectType) {
        if(event.hasCollection(objectType, collectionName)) {
            return event.get(objectType, collectionName);
        } else {
            return new ArrayList<T>(0);
        }
    }
}