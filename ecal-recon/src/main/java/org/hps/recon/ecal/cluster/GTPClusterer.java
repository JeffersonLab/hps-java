package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;

/**
 * Class <code>GTPCalorimeterClusterer</code> processes events and
 * converts hits into clusters, where appropriate. It uses the modified
 * 2014 clustering algorithm.<br/>
 * <br/>
 * For a hit to be a cluster center, it is required to have an energy
 * above some tunable minimum threshold. Additionally, the hit must be
 * a local maximum with respect to its neighbors and itself over a tunable
 * (default 2) clock cycles. Hits that pass these checks are then required
 * to additional have a total cluster energy that exceeds another tunable
 * minimum threshold.<br/>
 * <br/>
 * A hit is added to a cluster as a component if it has a non-zero energy
 * and within the aforementioned tunable time buffer used for clustering
 * and is either at the same location as the seed hit or is a neighbor
 * to the seed hit.
 * @author Kyle McCarty
 * @author Sho Uemura
 */
public class GTPClusterer extends AbstractClusterer {
        
    /**
     * The minimum energy required for a hit to be considered as a
     * cluster center. Hits with energy less than this value will be
     * ignored.
     */
    private double seedEnergyThreshold;
    
    /**
     * Indicates the number of FADC clock cycles (each cycle is 4 ns)
     * before and after a given cycle that should be considered when
     * checking if a cluster is a local maximum in space-time.
     */
    private int clusterWindow;
    
    /**
     * Stores a set of all the hits occurring in each clock cycle for
     * the number of clock cycles that should be considered for
     * clustering.
     */
    private LinkedList<Map<Long, CalorimeterHit>> hitBuffer;

    /**
     * Whether an asymmetric or symmetric window should be used for
     * adding hits to a cluster.
     */
    private boolean limitClusterRange = false;
    
    /**
     * Sets whether debug text should be written.
     */
    private boolean verbose = false;
    
    GTPClusterer() {
        super(new String[] { "seedEnergyThreshold", "clusterWindow" }, new double[] { 0.00, 2.});
    }    
    
    /**
     * Sets the clustering algorithm parameters.
     */
    @Override
    public void initialize() {
        // Set cuts.
        setSeedEnergyThreshold(getCuts().getValue("seedEnergyThreshold"));
        setClusterWindow((int) getCuts().getValue("clusterWindow"));
        
        // Initiate the hit buffer.
        hitBuffer = new LinkedList<Map<Long, CalorimeterHit>>();
        
        // Populate the event buffer with (2 * clusterWindow + 1)
        // empty events. These empty events represent the fact that
        // the first few events will not have any events in the past
        // portion of the buffer.
        int bufferSize = (2 * clusterWindow) + 1;
        for (int i = 0; i < bufferSize; i++) {
            hitBuffer.add(new HashMap<Long, CalorimeterHit>(0));
        }
    }
        
    /**
     * Generates a list of clusters from the current hit buffer. The
     * "present" event is taken to be the list of hits occurring at
     * index <code>clusterWindow</code>, which is the middle of the
     * buffer.
     * @return Returns a <code>List</code> of <code>HPSEcalCluster
     * </code> objects generated from the current event.
     */
    private List<Cluster> getClusters() {
        // Generate a list for storing clusters.
        List<Cluster> clusters = new ArrayList<Cluster>();
        
        // Get the list of hits at the current time in the event buffer.
        Map<Long, CalorimeterHit> currentHits = hitBuffer.get(clusterWindow);
        
        // VERBOSE :: Print the cluster window.
        if(verbose) {
        	// Print the event header.
	        System.out.printf("%n%nEvent:%n");
	        
	        // Calculate some constants.
	        int window = (hitBuffer.size() - 1) / 2;
	        int bufferNum = 0;
	        
	        // Print out all of the hits in the event buffer.
	        for(Map<Long, CalorimeterHit> bufferMap : hitBuffer) {
	            System.out.printf("Buffer %d:%n", hitBuffer.size() - bufferNum - window - 1);
	            CalorimeterHit hit = null;
	            
	            for(Entry<Long, CalorimeterHit> entry : bufferMap.entrySet()) {
	            	hit = entry.getValue();
	            	System.out.printf("\t(%3d, %3d) --> %.4f (%.4f)%n", hit.getIdentifierFieldValue("ix"),
	            			hit.getIdentifierFieldValue("iy"), hit.getCorrectedEnergy(), hit.getRawEnergy());
	            }
	            
	            bufferNum++;
	        }
	        
	        // If there are not hits, indicate this.
	        if(currentHits.isEmpty()) { System.out.println("\tNo hits this event!"); }
        }
        
        // For a hit to be a cluster center, it must be a local maximum
        // both with respect to its neighbors and itself both in the
        // present time and at all times within the event buffer.
        seedLoop:
        for (Long currentID : currentHits.keySet()) {
            // Get the actual hit object.
            CalorimeterHit currentHit = currentHits.get(currentID);
            
            // VERBOSE :: Print the current cluster.
            if(verbose) {
	            System.out.printf("Cluster Check:%n");
	        	System.out.printf("\t(%3d, %3d) --> %.4f%n", currentHit.getIdentifierFieldValue("ix"),
	        			currentHit.getIdentifierFieldValue("iy"), currentHit.getCorrectedEnergy());
            }
            
            // Store the energy of the current hit.
            double currentEnergy = currentHit.getCorrectedEnergy();
            
            // If the hit energy is lower than the minimum threshold,
            // then we immediately reject this hit as a possible cluster.
            if (currentEnergy < seedEnergyThreshold) {
            	// VERBOSE :: Note the reason the potential seed was
            	//            rejected.
            	if(verbose) { System.out.printf("\tREJECT :: Does not exceed seed threshold %.4f.%n", seedEnergyThreshold); }
            	
            	// Skip to the next potential seed.
                continue seedLoop;
            }
            
            // Store the crystals that are part of this potential cluster, 
            // starting with the cluster seed candidate.
            BaseCluster cluster = createBasicCluster();            
            cluster.addHit(currentHit);
            cluster.setPosition(currentHit.getDetectorElement().getGeometry().getPosition().v());
            cluster.setNeedsPropertyCalculation(false);
            
            // Get the set of neighbors for this hit.
            Set<Long> neighbors = neighborMap.get(currentHit.getCellID());
            
            // Sort through each event stored in the buffer.
            int bufferIndex = 0;
            for (Map<Long, CalorimeterHit> bufferHits : hitBuffer) {
                // Get the hit energy at the current hit's position in
                // the buffer, if it exists. Ignore the current seed candidate.
                CalorimeterHit bufferHit = bufferHits.get(currentID);
                if (bufferHit != null && bufferHit != currentHit) {
                    double bufferHitEnergy = bufferHit.getRawEnergy();
                    
                    // Check to see if the hit at this point in the buffer
                    // is larger than then original hit. If it is, we may
                    // stop the comparison because this is not a cluster.
                    if (bufferHitEnergy > currentEnergy) {
                    	// VERBOSE :: Output the reason the potential
                    	//            seed was rejected along with the
                    	//            hit that caused it.
                    	if(verbose) {
	                    	System.out.printf("\tREJECT :: Buffer hit surpasses hit energy.");
	                    	System.out.printf("\tBUFFER HIT :: (%3d, %3d) --> %.4f%n", bufferHit.getIdentifierFieldValue("ix"),
	                    			bufferHit.getIdentifierFieldValue("iy"), bufferHit.getCorrectedEnergy(), bufferHit.getRawEnergy());
                    	}
                    	
                    	// Skip to the next potential seed.
                        continue seedLoop;
                    }
                    
                    // If the buffer hit is smaller, then add its energy
                    // to the cluster total energy.
                    else {
                        if(limitClusterRange && bufferIndex <= clusterWindow + 1) { cluster.addHit(bufferHit); }
                        else if(!limitClusterRange) { cluster.addHit(bufferHit); }
                    }
                }
                
                // We must also make sure that the original hit is
                // larger than all of the neighboring hits at this
                // point in the buffer as well.
                for (Long neighborID : neighbors) {
                    // Get the neighbor hit energy if it exists.
                    CalorimeterHit neighborHit = bufferHits.get(neighborID);
                    if (neighborHit != null) {
                        double neighborHitEnergy = neighborHit.getRawEnergy();
                        
                        // Check to see if the neighbor hit at this point
                        // in the buffer is larger than then original hit.
                        // If it is, we may stop the comparison because this
                        // is not a cluster.
                        if (neighborHitEnergy > currentEnergy) {
                        	// VERBOSE :: Output the reason the potential
                        	//            seed was rejected along with the
                        	//            hit that caused it.
                        	if(verbose) {
	                        	System.out.printf("\tREJECT :: Buffer hit surpasses hit energy.%n");
	                        	System.out.printf("\tBUFFER HIT :: (%3d, %3d) --> %.4f%n", neighborHit.getIdentifierFieldValue("ix"),
	                        			neighborHit.getIdentifierFieldValue("iy"), neighborHit.getCorrectedEnergy(), neighborHit.getRawEnergy());
                        	}
                        	
                        	// Skip to the next potential seed.
                            continue seedLoop;
                        }
                        
                        // If the buffer neighbor hit is smaller, then
                        // add its energy to the cluster total energy.
                        else {
                            if(limitClusterRange && bufferIndex <= clusterWindow + 1) { cluster.addHit(neighborHit); }
                            else if(!limitClusterRange) { cluster.addHit(neighborHit); }
                        }
                    }
                }
                
                // Increment the buffer index.
                bufferIndex++;
            }
            
            // Add the cluster to the list of clusters.
            clusters.add(cluster);
            
            // VERBOSE :: Output the clusters generated from this event.
            if(verbose) {
	            System.out.printf("Cluster added.%n");
	            System.out.printf("\t(%3d, %3d) --> %.4f GeV --> %d hits%n", cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
	            		cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"), cluster.getEnergy(), cluster.getCalorimeterHits().size());
	            for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
	            	System.out.printf("\t\tCLUSTER HIT :: (%3d, %3d) --> %.4f%n", hit.getIdentifierFieldValue("ix"),
	            			hit.getIdentifierFieldValue("iy"), hit.getCorrectedEnergy(), hit.getRawEnergy());
	            }
            }
        }
        
        // Return the generated list of clusters.
        return clusters;
    }
    
    /**
     * Places hits from the current event into the event hit buffer
     * and processes the buffer to extract clusters. Clusters are then
     * stored in the event object.
     * @param event - The event to process.
     */
    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hits) {
        // Store each hit in a set by its cell ID so that it may be
        // easily acquired later.
        HashMap<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
        for (CalorimeterHit hit : hits) {
            hitMap.put(hit.getCellID(), hit);
        }

        // Remove the last event from the hit buffer and add the new one.
        hitBuffer.removeLast();
        hitBuffer.addFirst(hitMap);

        // Run the clustering algorithm on the buffer.
        List<Cluster> clusterList = getClusters();

        return clusterList;
    }
               
    /**
     * Sets the number of clock cycles before and after a given cycle
     * that will be used when checking whether a given hit is a local
     * maximum in both time and space. Note that a value of <code>N</code>
     * indicates that <code>N</code> clock cycles before and
     * <code>N</code> clock cycles after will be considered. Thusly, a
     * total of <code>2N + 1</code> clock cycles will be used.
     * @param clusterWindow - The number of additional clock cycles to
     * include in the clustering checks. A negative value will be treated
     * as zero.
     */
    void setClusterWindow(int clusterWindow) {
        // The cluster window of must always be at least zero.
        if (clusterWindow < 0) {
            this.clusterWindow = 0;
        }
        
        // If the cluster window is non-zero, then store it.
        else {
            this.clusterWindow = clusterWindow;
        }
    }
    
    /**
     * Sets whether hits should be added to a cluster from the entire
     * cluster window or just the "future" hits, plus one clock-cycle
     * of "past" hits as a safety buffer to account for time uncertainty.
     * @param limitClusterRange - <code>true</code> indicates that the
     * asymmetric clustering window should be used and <code>false</code>
     * that the symmetric window should be used.
     */
    void setLimitClusterRange(boolean limitClusterRange) {
        this.limitClusterRange = limitClusterRange;
    }
    
    /**
     * Sets the minimum energy threshold below which hits will not be
     * considered as cluster centers.
     * @param seedEnergyThreshold - The minimum energy for a cluster center.
     */
    void setSeedEnergyThreshold(double seedEnergyThreshold) {
        // A negative energy threshold is non-physical. All thresholds
        // be at least zero.
        if (seedEnergyThreshold < 0.0) {
            this.seedEnergyThreshold = 0.0;
        } // If the energy threshold is valid, then use it.
        else {
            this.seedEnergyThreshold = seedEnergyThreshold;
        }
    }
    
    /**
     * Sets whether diagnostic text should be written out or not.
     * @param verbose - <code>true</code> indicates that diagnostic
     * text will be written out and <code>false</code> that it will
     * not.
     */
    void setVerbose(boolean verbose) {
    	this.verbose = verbose;
    }
    
    /**
     * Indicates the type of cluster that is generated by this algorithm.
     * @return Returns the type of cluster as a <code>ClusterType</code>
     * object, specifically, <code>ClusterType.GTP</code>.
     */
    @Override
    public ClusterType getClusterType() {
        return ClusterType.GTP;
    }     
}
