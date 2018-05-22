package org.hps.analysis.MC;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;

public class MCTruthDebugDriver extends Driver {
    private static final double ECAL_INCIDENCE_THRESHOLD = 1300;
    
    @Override
    public void process(EventHeader event) {
        // Print the event number.
        println("\n\n\n\nEvent " + event.getEventNumber());
        
        // Process the tracks for truth information.
        List<Track> tracks = TruthModule.getCollection(event, "GBLTracks", Track.class);
        List<SimTrackerHit> truthTrackerHits = TruthModule.getCollection(event, "TrackerHits", SimTrackerHit.class);
        if(!tracks.isEmpty() && !truthTrackerHits.isEmpty()) {
            println("\n **************************************************************************");
            println(" *** Tracks Analysis ******************************************************");
            println(" **************************************************************************");
            for(Track track : tracks) {
                for(TrackerHit hit : track.getTrackerHits()) {
                    for(Object rawHit : hit.getRawHits()) {
                        if(rawHit instanceof RawTrackerHit) {
                            RawTrackerHit realHit = (RawTrackerHit) rawHit;
                            
                            for(SimTrackerHit truthHit : truthTrackerHits) {
                                if(truthHit.getCellID() == realHit.getCellID()) {
                                    System.out.printf("dt = %f; dx = %f; dy = %f; dz = %f%n", truthHit.getTime() - hit.getTime(), truthHit.getPosition()[0] - hit.getPosition()[0],
                                            truthHit.getPosition()[1] - hit.getPosition()[1], truthHit.getPosition()[2] - hit.getPosition()[2]);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Iterate over the clusters and get their truth particle
        // contributions by both incident and actual particle.
        List<Cluster> clusters = TruthModule.getCollection(event, "EcalClustersCorr", Cluster.class);
        if(!clusters.isEmpty()) {
            println("\n **************************************************************************");
            println(" *** Clusters Analysis ****************************************************");
            println(" **************************************************************************");
            for(Cluster cluster : clusters) {
                // Print the cluster under consideration.
                println(TruthModule.getClusterString(cluster));
                
                // Store the truth contributions for the cluster.
                double totalTruthEnergy = 0.0;
                Map<MCParticle, Double> particleContributionMap = new HashMap<MCParticle, Double>();
                Map<MCParticle, Double> incidentParticleContributionMap = new HashMap<MCParticle, Double>();
                
                // Process the cluster hits and extract their truth data.
                println("\tTruth Hits:");
                for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    // If no truth information exists, analysis can not
                    // be performed.
                    if(!(hit instanceof SimCalorimeterHit)) {
                        throw new RuntimeException("Error: Expected truth information, but none was found.");
                    }
                    
                    // Cast the hit to a truth hit.
                    SimCalorimeterHit truthHit = (SimCalorimeterHit) hit;
                    
                    // Print out the hit and its truth contributions, and
                    // also extract and store the truth data.
                    println("\t\t" + TruthModule.getEcalHitString(truthHit));
                    for(int i = 0; i < truthHit.getMCParticleCount(); i++) {
                        // Get the current particle truth information.
                        double truthEnergy = truthHit.getContributedEnergy(i);
                        MCParticle truthParticle = truthHit.getMCParticle(i);
                        MCParticle incidentParticle = getIncidentParticle(truthParticle, ECAL_INCIDENCE_THRESHOLD);
                        
                        // Print the hit.
                        printf("\t\t\t%5.3f :: %s%n", truthEnergy, TruthModule.getParticleString(truthParticle));
                        
                        // Increment the total truth energy.
                        totalTruthEnergy += truthEnergy;
                        
                        // Process the truth contribution of the real
                        // truth particle.
                        if(particleContributionMap.containsKey(truthParticle)) {
                            double combinedEnergy = truthEnergy + particleContributionMap.get(truthParticle).doubleValue();
                            particleContributionMap.put(truthParticle, Double.valueOf(combinedEnergy));
                        } else {
                            particleContributionMap.put(truthParticle, Double.valueOf(truthEnergy));
                        }
                        
                        // Process the truth contribution of the incident
                        // truth particle.
                        if(incidentParticleContributionMap.containsKey(incidentParticle)) {
                            double combinedEnergy = truthEnergy + incidentParticleContributionMap.get(incidentParticle).doubleValue();
                            incidentParticleContributionMap.put(incidentParticle, Double.valueOf(combinedEnergy));
                        } else {
                            incidentParticleContributionMap.put(incidentParticle, Double.valueOf(truthEnergy));
                        }
                    }
                }
                
                // Output the full cluster truth for real hits.
                println("\tReal Particle Truth Contributions:" );
                for(Entry<MCParticle, Double> entry : particleContributionMap.entrySet()) {
                    printf("\t\t%5.3f GeV (%5.1f%%) :: %s%n", entry.getValue().doubleValue(), entry.getValue().doubleValue() / totalTruthEnergy * 100.0,
                            TruthModule.getParticleString(entry.getKey()));
                }
                
                // Output the full cluster truth for incident hits.
                println("\tIncident Particle Truth Contributions:" );
                for(Entry<MCParticle, Double> entry : incidentParticleContributionMap.entrySet()) {
                    printf("\t\t%5.3f GeV (%5.1f%%) :: %s%n", entry.getValue().doubleValue(), entry.getValue().doubleValue() / totalTruthEnergy * 100.0,
                            TruthModule.getParticleString(entry.getKey()));
                }
            }
        }
        
        // Process particles and determine what contributions they
        // give to what hits.
        List<SimCalorimeterHit> ecalHits = new java.util.ArrayList<SimCalorimeterHit>();
        List<CalorimeterHit> ecalHits2 = TruthModule.getCollection(event, "EcalHits", CalorimeterHit.class);
        for(CalorimeterHit ecalHit2 : ecalHits2) {
            if(ecalHit2 instanceof SimCalorimeterHit) {
                ecalHits.add((SimCalorimeterHit) ecalHit2);
            } else { throw new IllegalArgumentException(); }
        }
        
        println("\n **************************************************************************");
        println(" *** Particle Analysis ****************************************************");
        println(" **************************************************************************");
        
        System.out.println("\"EcalHits\" Size: " + ecalHits.size());
        
        Map<MCParticle, Set<SimCalorimeterHit>> particleContributionMap = new HashMap<MCParticle, Set<SimCalorimeterHit>>();
        for(SimCalorimeterHit hit : ecalHits) {
            for(int i = 0; i < hit.getMCParticleCount(); i++) {
                MCParticle truthParticle = hit.getMCParticle(i);
                if(particleContributionMap.containsKey(truthParticle)) {
                    particleContributionMap.get(truthParticle).add(hit);
                } else {
                    Set<SimCalorimeterHit> hitSet = new HashSet<SimCalorimeterHit>();
                    hitSet.add(hit);
                    particleContributionMap.put(truthParticle, hitSet);
                }
            }
        }
        
        // Output the particle contribution data.
        for(Entry<MCParticle, Set<SimCalorimeterHit>> entry : particleContributionMap.entrySet()) {
            println("\t" + TruthModule.getParticleString(entry.getKey()));
            for(SimCalorimeterHit hit : entry.getValue()) {
                for(int i = 0; i < hit.getMCParticleCount(); i++) {
                    if(hit.getMCParticle(i) == entry.getKey()) {
                        printf("\t\t%5.3f GeV (%5.1f%%) :: %s%n", hit.getContributedEnergy(i),
                                (hit.getContributedEnergy(i) / getTruthEnergy(hit) * 100.0), TruthModule.getEcalHitString(hit));
                    }
                }
            }
        }
        
        // Get the particles.
        List<MCParticle> particles = TruthModule.getCollection(event, "MCParticle", MCParticle.class);
        
        // Output the particle trees.
        println("\n **************************************************************************");
        println(" *** Particle Trees *******************************************************");
        println(" **************************************************************************");
        for(MCParticle particle : particles) {
            if(particle.getParents().isEmpty()) {
                printTree(particle);
            }
        }
    }
    
    private static final void printTree(MCParticle particle) {
        printTree(particle, 0);
    }
    
    private static final void printTree(MCParticle particle, int level) {
        println(getIndent(level) + TruthModule.getParticleString(particle));
        for(MCParticle daughter : particle.getDaughters()) {
            printTree(daughter, level + 1);
        }
    }
    
    private static final String getIndent(int level) {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < level; i++) {
            buffer.append('\t');
        }
        return buffer.toString();
    }
    
    private static final double getTruthEnergy(SimCalorimeterHit hit) {
        double energy = 0.0;
        for(int i = 0; i < hit.getMCParticleCount(); i++) {
            energy += hit.getContributedEnergy(i);
        }
        
        return energy;
    }
    
    private static final MCParticle getIncidentParticle(MCParticle particle, double incidenceThreshold) {
        // Check the position of the particle's production vertex. If
        // it was created after the incidence threshold, get its
        // parent and perform the same test. Repeat until the current
        // particle is produced before the threshold.
        MCParticle curParticle = particle;
        while(true) {
            // Particles are expected to only ever have one parent.
            if(curParticle.getParents().size() != 1) {
                throw new RuntimeException("Error: Particles are expected to have either 0 or 1 parent(s) - saw " + particle.getParents().size() + ".");
            }
            
            // If the particle was created before the incidence
            // threshold, this is the "final" particle.
            if(curParticle.getOriginZ() < incidenceThreshold) {
                break;
            }
            
            // Otherwise, get the particle's parent and repeat the
            // process. Note that the A' should never be returned, so
            // if the parent is the A', the current particle is taken
            // to be an origin particle and should be returned.
            if(curParticle.getParents().get(0).getPDGID() == 622) {
                break;
            } else {
                curParticle = curParticle.getParents().get(0);
            }
        }
        
        // Return the particle
        return curParticle;
    }
    
    private static final void println(String text) {
        System.out.println(text);
    }
    
    private static final void printf(String text, Object... args) {
        System.out.printf(text, args);
    }
}