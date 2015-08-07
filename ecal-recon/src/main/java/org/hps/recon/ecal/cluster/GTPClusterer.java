package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
 * Class <code>GTPClusterer</code> is an implementation of the abstract
 * class <code>AbstractClusterer</code> that is responsible for producing
 * clusters using the GTP algorithm employed by the hardware.<br/>
 * <br/>
 * The GTP algorithm produces clusters by finding hits representing
 * local spatiotemporal energy maxima and forming a cluster from the
 * hits within the aforementioned spatiotemporal window. A given hit
 * is first checked to see if it exceeds some minimum energy threshold
 * (referred to as the "seed energy threshold"). If this is the case,
 * the algorithm looks at all hits that occurred in the same crystal as
 * the comparison hit, or any crystal directly adjacent to it, within
 * a programmable time window. If the hit exceeds all hits meeting these
 * criteria in energy, the hit is considered the “seed hit” of a cluster.
 * Then, all hits within the 3x3 spatial window which occur in the time
 * window are added to a <code>Cluster</code> object.<br/>
 * <br/>
 * Note that the algorithm employs two distinct temporal windows. The
 * first is the “verification” window. This is used to check that the
 * potential seed hit is a local maximum in energy, and is required to
 * be symmetric (i.e. as long before the seed time as after it) to ensure
 * consistency. The second temporal window is the “inclusion” window,
 * which determines which hits are included in the cluster. The inclusion
 * window can be asymmetric, but can not exceed the verification window
 * in length. As an example, one could choose a 12 ns verification window,
 * meaning that the algorithm would 12 ns before and after the seed hit
 * to check that it has the highest energy, but use a 4 ns/12 ns inclusion
 * window, meaning that the algorithm would only include hits in 3x3
 * spatial window up to 4 ns before and up to 12 ns after the seed hit
 * in the cluster. Due to the way the hardware processes hits, the higher
 * energy parts of a cluster always occur first in time, so it is not
 * necessarily desirable to include hits significantly before the seed.
 * It is however, necessary to verify a hit’s status as a maximum across
 * the full time window to ensure consistency in cluster formation.
 * <code>GTPClusterer</code> automatically defines the inclusion window
 * in terms of the verification window.<br/>
 * <br/>
 * <code>GTPClusterer</code> requires as input a collection of
 * <code>CalorimeterHit</code> objects representing the event hits. It
 * will then produce a collection of <code>Cluster</code> objects
 * representing the GTP algorithm output. It is designed to be run on
 * Monte Carlo events where each event represents a 2 ns beam bunch.
 * If the input data is formatted in the style of hardware readout, the
 * sister class <code>GTPOnlineClusterer</code> should be used instead.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @see Cluster
 * @see CalorimeterHit
 * @see AbstractClusterer
 * @see GTPOnlineClusterer
 */
public class GTPClusterer extends AbstractClusterer {
    /**
     * The minimum energy required for a hit to be considered as a
     * cluster center. Hits with energy less than this value will be
     * ignored. This is the seed energy lower bound cut.
     */
    private double seedEnergyThreshold;
    
    /**
     * Indicates the number of FADC clock cycles (each cycle is 4 ns)
     * before and after a given cycle that should be considered when
     * checking if a cluster is a local maximum in space-time. This
     * is the hit verification temporal window.
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
     * adding hits to a cluster. This defines the hit inclusion temporal
     * window with respect to the verification window.
     */
    private boolean limitClusterRange = false;
    
    /**
     * Sets whether debug text should be written.
     */
    private boolean verbose = false;
    
    /**
     * Sets whether the clusterer should store the collection of hits
     * that are part of clusters. This needs to be true if the clusters
     * are to be written out to LCIO.
     */
    private boolean writeHitCollection = true;
    
    /**
     * Instantiates a new instance of a Monte Carlo GTP clustering
     * algorithm. It will use the default seed energy threshold of
     * 50 MeV and a default hit inclusion window of +/- 2 ns. By
     * default the cluster inclusion and verification windows are
     * identical.
     */
    GTPClusterer() {
        super(new String[] { "seedEnergyThreshold", "clusterWindow" }, new double[] { 0.050, 2 });
    }
    
    /**
     * Processes the argument <code>CalorimeterHit</code> collection and
     * forms a collection of <code>Cluster</code> objects according to
     * the GTP clustering algorithm.
     * @param event - The object containing event data.
     * @param hitList - A list of <code>CalorimeterHit</code> objects
     * from which clusters should be formed.
     */
    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hits) {
        // Store each hit in a set by its cell ID so that it may be
        // easily acquired later.
        HashMap<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
        for(CalorimeterHit hit : hits) {
            hitMap.put(hit.getCellID(), hit);
        }
        
        // Remove the last event from the hit buffer and add the new one.
        hitBuffer.removeLast();
        hitBuffer.addFirst(hitMap);
        
        // Run the clustering algorithm on the buffer.
        List<Cluster> clusterList = getClusters();
        
        // The MC GTP algorithm collects hits from across events; to be
        // stored in LCIO format properly, it needs to separately store
        // its clusters' hits in a collection.
        if(writeHitCollection) {
        	// Create a set to store the hits so that each one may be
        	// stored only once.
	        Set<CalorimeterHit> hitSet = new HashSet<CalorimeterHit>();
	        
	        // Loop over all clusters and add their hits to the set.
	        for(Cluster cluster : clusterList) {
	        	for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
	        		hitSet.add(hit);
	        	}
	        }
	        
	        // Convert the set into a List object so that it can be stored
	        // in LCIO.
	        List<CalorimeterHit> clusterHits = new ArrayList<CalorimeterHit>(hitSet.size());
	        clusterHits.addAll(hitSet);
	        
	        // Place the list of hits into the event stream.
	        event.put("GTPHits", hits, CalorimeterHit.class, 0);
        }
        
        // Return the clusters.
        return clusterList;
    }
    
    /**
     * Indicates the type of cluster that is generated by this algorithm.
     * @return Returns the type of cluster as a <code>ClusterType</code>
     * object; specifically, returns <code>ClusterType.GTP</code>.
     */
    @Override
    public ClusterType getClusterType() {
        return ClusterType.GTP;
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
     * Sets the number of clock cycles before and after a given cycle
     * that will be used when checking whether a given hit is a local
     * maximum in both time and space. Note that a value of <code>N</code>
     * indicates that <code>N</code> clock cycles before and
     * <code>N</code> clock cycles after will be considered. Thusly, a
     * total of <code>2N + 1</code> clock cycles will be used. This
     * defines the size of the hit verification window. The inclusion
     * window is defined as a function of this, as discussed in the
     * method <code>setLimitClusterRange</code>.
     * @param clusterWindow - The number of additional clock cycles to
     * include in the clustering checks. A negative value will be treated
     * as zero.
     */
    void setClusterWindow(int clusterWindow) {
        // The cluster window of must always be at least zero.
        if (clusterWindow < 0) { this.clusterWindow = 0; }
        
        // If the cluster window is non-zero, then store it.
        else { this.clusterWindow = clusterWindow; }
    }
    
    /**
     * Sets the behavior of the hit inclusion and verification temporal
     * windows. If set to <code>true</code>, the hit inclusion window
     * will be defined as one clock-cycle before the seed hit and the
     * regular length of the verification window after the seed hit. If
     * <code>false</code>, the inclusion window and the verification
     * window are set to be identical.
     * @param limitClusterRange - <code>true</code> indicates that the
     * asymmetric clustering window should be used and <code>false</code>
     * that the symmetric window should be used.
     */
    void setAsymmetricWindow(boolean limitClusterRange) {
        this.limitClusterRange = limitClusterRange;
    }
    
    /**
     * Sets the minimum energy a hit must have before it will be
     * considered for cluster formation.
     * @param seedThreshold - The seed threshold in GeV.
     */
    void setSeedEnergyThreshold(double seedEnergyThreshold) {
        // A negative energy threshold is non-physical. All thresholds
        // be at least zero.
        if (seedEnergyThreshold < 0.0) { this.seedEnergyThreshold = 0.0; }
        
        // If the energy threshold is valid, then use it.
        else { this.seedEnergyThreshold = seedEnergyThreshold; }
    }
    
    /**
     * Sets whether the clusterer should output diagnostic text or not.
     * @param verbose - <code>true</code> indicates that the clusterer
     * should output diagnostic text and <code>false</code> that it
     * should not.
     */
    void setVerbose(boolean verbose) {
    	this.verbose = verbose;
    }
    
    /**
     * Sets whether the set of hits associated with an event's clusters
     * should be written to the data stream and persisted in LCIO. This
     * must be true if the clusters are to be persisted.
     * @param state - <code>true</code> indicates that the hits will be
     * persisted and <code>false</code> that they will not.
     */
    void setWriteHitCollection(boolean state) {
    	writeHitCollection = state;
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
}
