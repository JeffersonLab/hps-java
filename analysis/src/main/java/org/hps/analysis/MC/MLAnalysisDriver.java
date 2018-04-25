package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;

public class MLAnalysisDriver extends Driver {
    private Subdetector tracker = null;
    private FieldMap magneticFieldMap = null;
    private List<HpsSiSensor> sensors = null;
    private String TRACKER_SUBDETECTOR_NAME = "Tracker";
    
    @Override
    public void detectorChanged(Detector detector) {
        magneticFieldMap = detector.getFieldMap();
        tracker = detector.getSubdetector(TRACKER_SUBDETECTOR_NAME);
        sensors = detector.getSubdetector(TRACKER_SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
    }
    
    @Override
    public void process(EventHeader event) {
        System.out.println("\n\n\nEvent " + event.getEventNumber());
        
        List<RawTrackerHit> readoutHits = null;
        if(event.hasCollection(RawTrackerHit.class, "EcalReadoutHits")) {
            readoutHits = event.get(RawTrackerHit.class, "EcalReadoutHits");
            readoutHits.sort(new Comparator<RawTrackerHit>() {
                @Override
                public int compare(RawTrackerHit arg0, RawTrackerHit arg1) {
                    return Long.compare(arg0.getCellID(), arg1.getCellID());
                }
            });
        } else {
            readoutHits = new ArrayList<RawTrackerHit>(0);
        }
        
        List<CalorimeterHit> convertedHits = null;
        if(event.hasCollection(CalorimeterHit.class, "EcalCalHits")) {
            convertedHits = event.get(CalorimeterHit.class, "EcalCalHits");
            convertedHits.sort(new Comparator<CalorimeterHit>() {
                @Override
                public int compare(CalorimeterHit arg0, CalorimeterHit arg1) {
                    return Long.compare(arg0.getCellID(), arg1.getCellID());
                }
            });
        } else {
            convertedHits = new ArrayList<CalorimeterHit>(0);
        }
        
        List<Cluster> gtpClusters = null;
        if(event.hasCollection(Cluster.class, "EcalClustersGTP")) {
            gtpClusters = event.get(Cluster.class, "EcalClustersGTP");
            gtpClusters.sort(new Comparator<Cluster>() {
                @Override
                public int compare(Cluster arg0, Cluster arg1) {
                    return Long.compare(arg0.getCalorimeterHits().get(0).getCellID(), arg0.getCalorimeterHits().get(0).getCellID());
                }
            });
        } else {
            gtpClusters = new ArrayList<Cluster>(0);
        }
        
        // Try and obtain calorimeter clusters.
        List<Cluster> clusters = null;
        if(event.hasCollection(Cluster.class, "EcalClusters")) {
            clusters = event.get(Cluster.class, "EcalClusters");
            clusters.sort(new Comparator<Cluster>() {
                @Override
                public int compare(Cluster arg0, Cluster arg1) {
                    return Long.compare(arg0.getCalorimeterHits().get(0).getCellID(), arg0.getCalorimeterHits().get(0).getCellID());
                }
            });
        } else {
            clusters = new ArrayList<Cluster>(0);
        }
        
        
        
        System.out.println("Raw Hits:");
        for(RawTrackerHit readoutHit : readoutHits) {
            System.out.printf("\tHit at %7d and time %4d ns and position <%7.1f, %7.1f, %7.1f>.%n",
                    readoutHit.getCellID(), readoutHit.getTime(), readoutHit.getPosition()[0], readoutHit.getPosition()[1], readoutHit.getPosition()[2]);
            StringBuffer adcBuffer = new StringBuffer("\t\tADC:");
            for(short adc : readoutHit.getADCValues()) {
                adcBuffer.append("   ");
                adcBuffer.append(adc);
            }
            System.out.println(adcBuffer.toString());
        }
        if(readoutHits.isEmpty()) { System.out.println("\tNone"); }
        
        System.out.println("\nConverted Hits:");
        for(CalorimeterHit hit : convertedHits) {
            System.out.printf("\tHit at %7d with energy %6.3f GeV (%6.3f GeV) at time %6.2f ns and position <%7.1f, %7.1f, %7.1f>.%n", hit.getCellID(),
                    hit.getRawEnergy(), hit.getCorrectedEnergy(), hit.getTime(), hit.getPosition()[0], hit.getPosition()[1], hit.getPosition()[2]);
            System.out.println("\t\tTruth Hits: " + ((SimCalorimeterHit) hit).getMCParticleCount());
        }
        if(convertedHits.isEmpty()) { System.out.println("\tNone"); }
        
        System.out.println("\nGTP Clusters:");
        Set<CalorimeterHit> clusterHits = new HashSet<CalorimeterHit>();
        for(Cluster cluster : gtpClusters) {
            clusterHits.addAll(cluster.getCalorimeterHits());
            System.out.printf("\tCluster at %7d with energy %6.3f GeV and %d hits at time %6.2f ns and position <%7.1f, %7.1f, %7.1f>.%n",
                    cluster.getCalorimeterHits().get(0).getCellID(), cluster.getEnergy(), cluster.getCalorimeterHits().size(),
                    cluster.getCalorimeterHits().get(0).getTime(), cluster.getPosition()[0], cluster.getPosition()[1], cluster.getPosition()[2]);
        }
        if(gtpClusters.isEmpty()) { System.out.println("\tNone"); }
        
        System.out.println("\nGTP Cluster Hits:");
        for(CalorimeterHit hit : clusterHits) {
            System.out.printf("\tHit at %7d with energy %6.3f GeV (%6.3f GeV) at time %6.2f ns and position <%7.1f, %7.1f, %7.1f>.%n", hit.getCellID(),
                    hit.getRawEnergy(), hit.getCorrectedEnergy(), hit.getTime(), hit.getPosition()[0], hit.getPosition()[1], hit.getPosition()[2]);
        }
        if(clusterHits.isEmpty()) { System.out.println("\tNone"); }
        
        System.out.println("\nClusters:");
        for(Cluster cluster : clusters) {
            System.out.printf("\tCluster at %7d with energy %6.3f GeV and %d hits at time %6.2f ns and position <%7.1f, %7.1f, %7.1f>.%n",
                    cluster.getCalorimeterHits().get(0).getCellID(), cluster.getEnergy(), cluster.getCalorimeterHits().size(),
                    cluster.getCalorimeterHits().get(0).getTime(), cluster.getPosition()[0], cluster.getPosition()[1], cluster.getPosition()[2]);
        }
        if(clusters.isEmpty()) { System.out.println("\tNone"); }
        
        //if(true) { return; }
        
        /*
        // Get the individual truth relations.
        List<LCRelation> truthRelations = null;
        if(event.hasCollection(LCRelation.class, "SimHitTruthRelations")) {
            truthRelations = event.get(LCRelation.class, "SimHitTruthRelations");
        } else {
            throw new RuntimeException("Error: SLIC hit relations collection not found.");
        }
        
        // Process the relations into a truth map.
        Map<SimCalorimeterHit, Set<SimCalorimeterHit>> truthMap = EcalRawConverterDriver.getTruthMap(truthRelations, SimCalorimeterHit.class);
        */
        
        // For each cluster, find the particle that contributes
        // the most energy to each hit.
        List<LCRelation> clusterTruthParticles = new ArrayList<LCRelation>();
        List<Pair<Cluster, MCParticle>> pairList = new ArrayList<Pair<Cluster, MCParticle>>();
        for(Cluster cluster : clusters) {
            // Output the cluster debug text.
            System.out.println("\n");
            System.out.println(getClusterString(cluster));
            
            // Process the hits into a proper truth hit collection.
            List<SimCalorimeterHit> truthHits = new ArrayList<SimCalorimeterHit>(cluster.getCalorimeterHits().size());
            for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
                if(SimCalorimeterHit.class.isAssignableFrom(hit.getClass())) {
                    truthHits.add(SimCalorimeterHit.class.cast(hit));
                } else {
                    System.out.println("Error: Truth analysis can not be performed unless truth information is available.");
                    return;
                    //throw new RuntimeException("Error: Truth analysis can not be performed unless truth information is available.");
                }
            }
            
            
            /*
             * Output the total energy contribution each particle
             * contributed to the entire cluster. The primary
             * particle could be considered to be the one that gives
             * the most energy to the cluster.
             */
            //final int headerWidth = 45;
            //String headerBorder = "\t** " + getHeader(headerWidth) + " **";
            //System.out.println(headerBorder);
            //System.out.println("\t** " + getHeader(headerWidth, "Particle Total Contribution to Cluster") + " **");
            //System.out.println(headerBorder);
            
            double ultimatePercent = 0.0;
            double penultimatePercent = 0.0;
            MCParticle ultimateParticle = null;
            double clusterTruthEnergy = getTruthEnergy(cluster);
            Map<MCParticle, Double> particleEnergyMap = getClusterEnergyDistributionByParticle(cluster);
            for(Map.Entry<MCParticle, Double> entry : particleEnergyMap.entrySet()) {
                double percent = 100.0 * entry.getValue().doubleValue() / clusterTruthEnergy;
                if(percent > ultimatePercent) {
                    penultimatePercent = ultimatePercent;
                    ultimatePercent = percent;
                    ultimateParticle = entry.getKey();
                } else if(percent > penultimatePercent) {
                    penultimatePercent = percent;
                }
                
                System.out.printf("\t\t%6.2f%% :: %s%n", percent, getParticleString(entry.getKey()));
            }
            System.out.printf("\tLargest Single Contribution        :: %6.2f%%%n", ultimatePercent);
            System.out.printf("\tSecond-Largest Single Contribution :: %6.2f%%%n", penultimatePercent);
            System.out.printf("\tTop Two Contribution Difference    :: %6.2f%%%n", (ultimatePercent - penultimatePercent));
            if(ultimatePercent > 50.0) {
                System.out.printf("\tDominant Particle                  :: %s%n", getParticleString(ultimateParticle));
                clusterTruthParticles.add(new BaseLCRelation(cluster, ultimateParticle));
                pairList.add(new Pair<Cluster, MCParticle>(cluster, ultimateParticle));
            } else {
                System.out.printf("\tDominant Particle                  :: NONE%n");
            }
        }
        
        event.put("ClusterTruthRelations", clusterTruthParticles, LCRelation.class, 0);
        
        // Create a list of pairs which originate from the trident.
        // One list should include only pairs where the cluster was
        // directly generated by the trident particle, and the other
        // should include any pairs generated from trident particles,
        // even if the cluster came from secondaries.
        System.out.println("\n\nTrident Truth Pairs:");
        for(int i = 0; i < pairList.size() - 1; i++) {
            Pair<Cluster, MCParticle> pair0 = pairList.get(i);
            boolean isTrident = isTrident(pair0.getSecondElement());
            boolean isTridentDescendent = isTridentDescendent(pair0.getSecondElement());
            
            for(int j = i + 1; j < pairList.size(); j++) {
                Pair<Cluster, MCParticle> pair1 = pairList.get(j);
                
                // The same particle can not be a pair.
                if(pair0 == pair1) { continue; }
                
                // If both particles are tridents, they are a valid
                // primary cluster pair.
                if(isTrident(pair1.getSecondElement()) && isTrident) {
                    System.out.println("\tPrimary Trident Pair:");
                    System.out.println("\t\t" + getClusterString(pair0.getFirstElement()));
                    System.out.println("\t\t\t" + getParticleString(pair0.getSecondElement()));
                    System.out.println("\t\t" + getClusterString(pair1.getFirstElement()));
                    System.out.println("\t\t\t" + getParticleString(pair1.getSecondElement()));
                } else if(isTridentDescendent(pair1.getSecondElement()) && isTridentDescendent) {
                    System.out.println("\tSecondary Trident Pair:");
                    System.out.println("\t\t" + getClusterString(pair0.getFirstElement()));
                    System.out.println("\t\t\t" + getParticleString(pair0.getSecondElement()));
                    System.out.println("\t\t" + getClusterString(pair1.getFirstElement()));
                    System.out.println("\t\t\t" + getParticleString(pair1.getSecondElement()));
                }
            }
        }
        
        // Get the tracks for the event and attempt to obtain truth
        // information for them.
        if(event.hasCollection(Track.class, "GBLTracks")) {
            List<Track> gblTracks = event.get(Track.class, "GBLTracks");
            for(Track track : gblTracks) {
                MCFullDetectorTruth trackTruth = new MCFullDetectorTruth(event, track, magneticFieldMap, sensors, tracker);
            }
        } else {
            throw new RuntimeException("Error: Track collection \"GBLTracks\" was not found.");
        }
        
        /*
         * Output the distance of a truth particle from the cluster
         * center as well as its energy. The closest high-energy
         * particle is likely the origin of the cluster.
         */
        if(!clusters.isEmpty()) {
            System.out.println("\n\n\n");
        }
    }
    
    private static final Map<MCParticle, Integer> getParticleEnergyRankings(Cluster cluster) {
        // Get the particle energy contributions.
        Map<MCParticle, Double> particleEnergyMap = getClusterEnergyDistributionByParticle(cluster);
        
        // Dump the values into a list and sort by energy.
        List<Map.Entry<MCParticle, Double>> particleEnergyRankingList = new ArrayList<Map.Entry<MCParticle, Double>>(particleEnergyMap.size());
        particleEnergyRankingList.addAll(particleEnergyMap.entrySet());
        particleEnergyRankingList.sort(new Comparator<Map.Entry<MCParticle, Double>>() {
            @Override
            public int compare(Entry<MCParticle, Double> arg0, Entry<MCParticle, Double> arg1) {
                return Double.compare(arg0.getValue().doubleValue(), arg1.getValue().doubleValue());
            }
        });
        
        // Map the particles to their ranking in the list.
        Map<MCParticle, Integer> rankingMap = new HashMap<MCParticle, Integer>(particleEnergyMap.size());
        for(int i = 0; i < particleEnergyRankingList.size(); i++) {
            rankingMap.put(particleEnergyRankingList.get(i).getKey(), Integer.valueOf(i));
        }
        
        // Return the result.
        return rankingMap;
    }
    
    private static final String getHeader(int bufferWidth) {
        return getHeader(bufferWidth, null);
    }
    
    private static final String getHeader(int bufferWidth, String text) {
        StringBuffer headerBuffer = new StringBuffer();
        
        if(text == null || text.isEmpty()) {
            for(int i = 0; i < bufferWidth; i++) {
                headerBuffer.append('*');
            }
        } else {
            if((text.length() + 2) >= bufferWidth) {
                return text;
            } else {
                int stars = bufferWidth - text.length() - 2;
                boolean uneven = (stars % 2) == 1;
                int halfStars = uneven ? ((stars - 1) / 2) : (stars / 2);
                
                for(int i = 0; i < halfStars; i++) {
                    headerBuffer.append('*');
                }
                if(uneven) {
                    headerBuffer.append('*');
                }
                
                headerBuffer.append(' ');
                headerBuffer.append(text);
                headerBuffer.append(' ');
                
                for(int i = 0; i < halfStars; i++) {
                    headerBuffer.append('*');
                }
            }
        }
        
        return headerBuffer.toString();
    }
    
    private static final MCParticle getOriginParticle(MCParticle particle) {
        MCParticle curParticle = particle;
        while(curParticle.getParents() != null && !curParticle.getParents().isEmpty()) {
            if(!isSecondary(curParticle)) {
                return curParticle;
            } else if(curParticle.getParents().size() == 1) {
                curParticle = curParticle.getParents().get(0);
            } else {
                throw new RuntimeException("Error: Particle has multiple parents.");
            }
        }
        
        return curParticle;
    }
    
    private static final double getTruthEnergy(Cluster cluster) {
        // Iterate over the cluster hits and extract the total truth
        // energy from each of its contributing MCParticles.
        double totalTruthEnergy = 0.0;
        for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
            // Only truth hits can be analyzed in this way.
            if(SimCalorimeterHit.class.isAssignableFrom(hit.getClass())) {
                // Cast the hit to a truth hit.
                SimCalorimeterHit truthHit = SimCalorimeterHit.class.cast(hit);
                
                // Iterate over the truth particles and add their
                // truth energy deposition to the energy counter.
                for(int i = 0; i < truthHit.getMCParticleCount(); i++) {
                    totalTruthEnergy += truthHit.getContributedEnergy(i);
                }
            } else {
                throw new IllegalArgumentException("Error: Only clusters with truth information available can be processed for truth energy.");
            }
        }
        
        // Return the total truth energy from all hits.
        return totalTruthEnergy;
    }
    
    private static final boolean isPrimary(MCParticle particle) {
        return particle.getParents().isEmpty() || particle.getParents().get(0).getPDGID() == 622;
    }
    
    private static final boolean isSecondary(MCParticle particle) {
        return !isPrimary(particle);
    }
    
    private static final boolean isTridentDescendent(MCParticle particle) {
        MCParticle curParticle = particle;
        while(!curParticle.getParents().isEmpty()) {
            if(curParticle.getParents().size() > 1) {
                throw new RuntimeException("Error: Particles are expected to have either no or 1 parent(s).");
            }
            
            if(isTrident(curParticle)) {
                return true;
            } else {
                curParticle = curParticle.getParents().get(0);
            }
        }
        
        return isTrident(curParticle);
    }
    
    private static final boolean isTrident(MCParticle particle) {
        // A particle with no parents is either the A' or an external
        // particle.
        if(particle.getParents().isEmpty()) {
            return false;
        }
        
        // Otherwise, the particle is a trident if it has the A' as
        // a parent and is either an electron or positron.
        else if(particle.getParents().size() == 1) {
            if(particle.getParents().get(0).getPDGID() == 622 && (particle.getPDGID() == 11 || particle.getPDGID() == -11)) {
                return true;
            } else {
                return false;
            }
        } else {
            throw new RuntimeException("Error: Particles are expected to have either no or 1 parent(s).");
        }
    }
    
    private static final MCParticle getOriginEcalParticle(MCParticle particle) {
        // The calorimeter face occurs at approximately 1318 mm. We
        // allow a little extra distance for safety.
        final int ecalFace = 1330;
        
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
    
    private static final Map<MCParticle, Double> getClusterEnergyDistributionByParticle(Cluster cluster) {
        // Iterate over the cluster hits and map the truth energy
        // depositions of their contributing particles to the energy
        // value.
        Map<MCParticle, Double> particleEnergyMap = new HashMap<MCParticle, Double>();
        for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
            // Only truth hits can be analyzed in this way.
            if(SimCalorimeterHit.class.isAssignableFrom(hit.getClass())) {
                // Cast the hit to a truth hit.
                SimCalorimeterHit truthHit = SimCalorimeterHit.class.cast(hit);
                
                // Iterate over the truth particles and add their
                // truth energy deposition to the energy counter.
                for(int i = 0; i < truthHit.getMCParticleCount(); i++) {
                    // Get the originating particle.
                    //MCParticle originParticle = getOriginParticle(truthHit.getMCParticle(i));
                    MCParticle originParticle = getOriginEcalParticle(truthHit.getMCParticle(i));
                    
                    // Get the energy contribution for this particle,
                    // if it already exists.
                    Double energyContribution = particleEnergyMap.get(originParticle);
                    
                    // Associate the new energy contribution with the
                    // particle. Increment the existing value if one
                    // already exists.
                    if(energyContribution == null) {
                        energyContribution = Double.valueOf(truthHit.getContributedEnergy(i));
                    } else {
                        energyContribution = Double.valueOf(energyContribution.doubleValue() + truthHit.getContributedEnergy(i));
                    }
                    
                    // Map the energy contribution to the particle.
                    particleEnergyMap.put(originParticle, energyContribution);
                }
            } else {
                throw new IllegalArgumentException("Error: Only clusters with truth information available can be processed for truth energy.");
            }
        }
        
        // Return the mapping.
        return particleEnergyMap;
    }
    
    private static final String getParticleString(MCParticle particle) {
        if(particle == null) {
            return "Particle with PDGID UNDEFINED produced at time t = NaN ns with charge NaN C and momentum NaN GeV.";
        } else {
            String particleName = null;
            int pid = particle.getPDGID();
            if(pid == 11) { particleName = "e-"; }
            else if(pid == -11) { particleName = "e+"; }
            else { particleName = Integer.toString(pid); }
            
            return String.format("Particle of type %3s produced at time t = %5.1f ns and vertex <%.1f, %.1f, %.1f> with charge %2.0f C and momentum %5.3f GeV.",
                    particleName, particle.getProductionTime(), particle.getOriginX(), particle.getOriginY(), particle.getOriginZ(), particle.getCharge(), particle.getEnergy());
        }
    }
    
    private static final String getClusterString(Cluster cluster) {
        if(cluster == null) {
            return "Cluster at UNDEFINED with energy NaN GeV and size NaN hit(s) at time NaN ns.";
        } else {
            return String.format("Cluster at %d with energy %5.3f GeV and size %d hit(s) at time %.2f ns.", cluster.getCalorimeterHits().get(0).getCellID(),
                    cluster.getEnergy(), cluster.getCalorimeterHits().size(), cluster.getCalorimeterHits().get(0).getTime());
        }
    }
    
    private static final String getHitString(CalorimeterHit hit) {
        if(hit == null) {
            return "Hit at UNDEFINED with energy NaN GeV at time NaN ns.";
        } else {
            return String.format("Hit at %d with energy %5.3f GeV at time %.1f ns.", hit.getCellID(), hit.getRawEnergy(), hit.getTime());
        }
    }
}