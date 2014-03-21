package org.lcsim.hps.recon.ecal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * Class <code>CTPCalorimeterClusterer</code> processes events and
 * converts hits into clusters, where appropriate. It uses the modified
 * 2014 clustering algorithm.
 * 
 * For a hit to be a cluster center, it is required to have an energy
 * above some tunable minimum threshold. Additionally, the hit must be
 * a local maximum with respect to its neighbors and itself over a
 * tunable (default 4) clock cycles. Hits that pass these checks are
 * then required to additional have a total cluster energy that exceeds
 * another tunable minimum threshold.
 * 
 * A hit is added to a cluster as a component if it has a non-zero
 * energy and within the aforementioned tunable time buffer used for
 * clustering and is either at the same location as the seed hit or
 * is a neighbor to the seed hit.
 * 
 * @author Kyle McCarty
 * @author Sho Uemura
 */
public class CTPCalorimeterClusterer extends Driver {
    /**
     * <b>calorimeter</b><br/><br/>
     * <code>private HPSEcal3 <b>calorimeter</b></code><br/><br/>
     * The sub-detector representing the calorimeter.
     */
    private HPSEcal3 calorimeter;
    
    /**
     * <b>calorimeterName</b><br/><br/>
     * <code>private String <b>calorimeterName</b></code><br/><br/>
     * The name of the calorimeter sub-detector stored in the <code>
     * Detector</code> object for this run.
     */
    String calorimeterName;
    
    /**
     * <b>clusterCollectionName</b><br/><br/>
     * <code>private String <b>clusterCollectionName</b></code><br/><br/>
     * The name of the LCIO collection name in which the clusters
     * will be stored.
     */
    String clusterCollectionName = "EcalClusters";
    
    /**
     * <b>clusterEnergyThreshold</b><br/><br/>
     * <code>private double <b>clusterEnergyThreshold</b></code><br/><br/>
     * The minimum total cluster energy required for a cluster to be
     * accepted. Clusters with less energy will not be reported even
     * if they are a local maximum.
     */
    double clusterEnergyThreshold = 0.0;
    
    /**
     * <b>clusterWindow</b><br/><br/>
     * <code>private int <b>clusterWindow</b></code><br/><br/>
     * Indicates the number of clock cycles before and after a given
     * cycle that should be considered when checking if a cluster is
     * a local maximum in space-time.
     */
    int clusterWindow = 2;
    
    /**
     * <b>eventBuffer</b><br/><br/>
     * <code>private LinkedList<EventHeader> <b>eventBuffer</b></code><br/><br/>
     * Stores the <code>EventHeader</code> objects that occurred within
     * the cluster window so that clusters may be attached to the event
     * in which the seed hit occurred.
     */
    private LinkedList<EventHeader> eventBuffer;
    
    /**
     * <b>hitBuffer</b><br/><br/>
     * <code>private LinkedList<List<CalorimeterHit>> <b>hitBuffer</b></code><br/><br/>
     * Stores a set of all the hits occurring in each clock cycle for
     * the number of clock cycles that should be considered for
     * clustering.
     */
    private LinkedList<Map<Long, CalorimeterHit>> hitBuffer;
    
    /**
     * <b>hitCollectionName</b><br/><br/>
     * <code>private String <b>hitCollectionName</b></code><br/><br/>
     * The name of LCIO collection containing the calorimeter hits
     * that are to be used for clustering. 
     */
    String hitCollectionName;
    
    /**
     * <b>neighborMap</b><br/><br/>
     * <code>private NeighborMap <b>neighborMap</b></code><br/><br/>
     * Maps the <code>long</code> crystal ID to the set of crystal IDs
     * of the crystals which are adjacent to the key.
     */
    private NeighborMap neighborMap = null;
    
    /**
     * <b>seedEnergyThreshold</b><br/><br/>
     * <code>private double <b>seedEnergyThreshold</b></code><br/><br/>
     * The minimum energy required for a hit to be considered as a
     * cluster center. Hits with energy less than this value will
     * be ignored.
     */
    double seedEnergyThreshold = 0.0;
    
    /**
     * <b>validClusterCrystals</b><br/><br/>
     * <code>private Set<Long> <b>validClusterCrystals</b></code><br/><br/>
     * Contains the <code>long</code> crystal ID of every crystal that
     * is a valid cluster center.
     */
    private Set<Long> validClusterCrystals = null;
    
    /**
     * <b>detectorChanged</b><br/><br/>
     * <code>public void <b>detectorChanged</b>(Detector detector)</code><br/><br/>
     * Initializes detector-dependent parameters for clustering. Method
     * is responsible for determining which crystals are valid cluster
     * centers (stored in <code>validCrystals</code>), defining the
     * detector object (stored in <code>calorimeter</code>), defining
     * the detector ID decoder (stored in <code>decoder</code>), and
     * defining the mapping of crystal IDs to the crystal's neighbors
     * (defined in <code>neighborMap</code>).
     * @param detector - The new detector to use.
     */
    public void detectorChanged(Detector detector) {
        // Get the calorimeter object.
    	calorimeter = (HPSEcal3) detector.getSubdetector(calorimeterName);
        
        // Get a map to associate crystals with their neighbors.
        neighborMap = calorimeter.getNeighborMap();
        
        // Store the crystals that are allowed to be cluster centers.
        validClusterCrystals = new HashSet<Long>();
        
        // Populate the list of valid cluster centers.
        for (Long cellID : neighborMap.keySet()) {
        	// Store whether the current crystal is a valid cluster center.
            boolean isValidCenter = true;
            
            // Get the set of neighbors for the current crystal.
            Set<Long> neighbors = neighborMap.get(cellID);
            
            // Sort over the list of neighbors.
            for (Long neighborID : neighbors) {
            	// Get the neighbor's list of neighbors as well as itself.
                Set<Long> neighborNeighbors = new HashSet<Long>();
                neighborNeighbors.addAll(neighborMap.get(neighborID));
                neighborNeighbors.add(neighborID);
                
                // If the current crystal's neighbors include all of
                // one of its neighbor's neighbors, then it is an edge
                // crystal and is not a valid center for clusters.
                if (neighborNeighbors.containsAll(neighbors)) {
                    isValidCenter = false;
                    break;
                }
            }
            
            // If the current crystal survived the above test, it is
            // a valid cluster center. Add it to the list.
            if (isValidCenter) { validClusterCrystals.add(cellID); }
        }
    }
    
    /**
     * <b>endOfData</b><br/><br/>
     * <code>public void <b>endOfData</b>()</code><br/><br/>
     * Empties the remaining events from the event buffer and processes
     * any remaining clusters.
     */
    public void endOfData() {
    	// At the end of the data run, not all filled events in the hit
    	// buffer will have been processed. We add additional empty
    	// events to the end of the event buffer and continue to process
    	// events until all the remaining legitimate events have been
    	// processed.
    	for(int i = 0; i < clusterWindow; i++) {
        	// Remove the last event from the hit buffer a blank one.
        	hitBuffer.removeLast();
        	hitBuffer.addFirst(new HashMap<Long, CalorimeterHit>(0));
        	eventBuffer.removeLast();
        	eventBuffer.addFirst(null);
        	
        	// Process the event.
        	processEvent();
    	}
    }
    
    /**
     * <b>getClusters</b><br/><br/>
     * <code>public List<HPSEcalCluster> <b>getClusters</b>()</code><br/><br/>
     * Generates a list of clusters from the current hit buffer. The
     * "present" event is taken to be the list of hits occurring at
     * index <code>clusterWindow</code>, which is the middle of the
     * buffer.
     * @return Returns a <code>List</code> of <code>HPSEcalCluster
     * </code> objects generated from the current event.
     */
    public List<HPSEcalCluster> getClusters() {
    	// Generate a list for storing clusters.
    	List<HPSEcalCluster> clusters = new ArrayList<HPSEcalCluster>();
    	
    	// Get the list of hits at the current time in the event buffer.
    	Map<Long, CalorimeterHit> currentHits = hitBuffer.get(clusterWindow);
    	
    	// For a hit to be a cluster center, it must be a local maximum
    	// both with respect to its neighbors and itself both in the
    	// present time and at all times within the event buffer.
    	for(Long currentID : currentHits.keySet()) {
    		// If this hit is not a valid cluster center, we do not
    		// need to perform any additional checks.
    		if(validClusterCrystals.contains(currentID)) { continue; }
    		
    		// Get the actual hit object.
    		CalorimeterHit currentHit = currentHits.get(currentID);
    		
    		// Store the energy of the current hit.
    		double currentEnergy = currentHit.getRawEnergy();
    		
    		// If the hit energy is lower than the minimum threshold,
    		// then we immediately reject this hit as a possible cluster.
    		if(currentEnergy < seedEnergyThreshold) { continue; }
    		
    		// Store the cluster energy for the potential cluster as we
    		// sort through the buffer.
    		double clusterEnergy = 0.0;
    		
    		// Store the crystals that are part of this cluster.
    		Set<CalorimeterHit> clusterComponentHits = new HashSet<CalorimeterHit>();
    		
    		// Get the set of neighbors for this hit.
    		Set<Long> neighbors = neighborMap.get(currentHit.getCellID());
    		
    		// Store whether the current hit is a cluster center.
    		boolean isCluster = true;
    		
    		// Sort through each event stored in the buffer.
    		for(Map<Long, CalorimeterHit> bufferHits : hitBuffer) {
    			// Get the hit energy at the current hit's position in
    			// the buffer, if it exists.
    			double bufferHitEnergy = 0.0;
    			CalorimeterHit bufferHit = bufferHits.get(currentID);
    			if(bufferHit != null) { bufferHitEnergy = bufferHit.getRawEnergy(); }
    			
    			// Check to see if the hit at this point in the buffer
    			// is larger than then original hit. If it is, we may
    			// stop the comparison because this is not a cluster.
    			if(bufferHitEnergy > currentEnergy) {
    				isCluster = false;
    				break;
    			}
    			
    			// If the buffer hit is smaller, then add its energy
    			// to the cluster total energy.
    			else {
    				clusterEnergy += bufferHitEnergy;
    				if(bufferHit != currentHit) { clusterComponentHits.add(bufferHit); }
    			}
    			
    			// We must also make sure that the original hit is
    			// larger than all of the neighboring hits at this
    			// point in the buffer as well.
    			for(Long neighborID : neighbors) {
    				// Get the neighbor hit energy if it exists.
        			double neighborHitEnergy = 0.0;
        			CalorimeterHit neighborHit = bufferHits.get(neighborID);
        			if(neighborHit != null) { neighborHitEnergy = neighborHit.getRawEnergy(); }
        			
        			// Check to see if the neighbor hit at this point
        			// in the buffer is larger than then original hit.
        			// If it is, we may stop the comparison because this
        			// is not a cluster.
        			if(neighborHitEnergy > currentEnergy) {
        				isCluster = false;
        				break;
        			}
        			
        			// If the buffer neighbor hit is smaller, then
        			// add its energy to the cluster total energy.
        			else {
        				clusterEnergy += neighborHitEnergy;
        				clusterComponentHits.add(neighborHit);
        			}
    			}
    			
    			// If we exit from the neighbor loop and isCluster is
    			// false, then one of the neighboring crystals had a
    			// larger energy and we may break from this iteration.
    			if(!isCluster) { break; }
    		}
    		
    		// If the potential cluster center is still a valid, check
    		// that its cluster energy is above the minimum threshold.
    		if(isCluster && clusterEnergy >= clusterEnergyThreshold) {
    			// Generate a new cluster from this information.
                CalorimeterHit seedHit = new HPSCalorimeterHit(0.0, currentHit.getTime(), currentID, currentHit.getType());
                seedHit.setMetaData(currentHit.getMetaData());
                HPSEcalCluster cluster = new HPSEcalCluster(seedHit);
                
                // Add all of the neighbors to the cluster.
                for(CalorimeterHit neighbor : clusterComponentHits) { cluster.addHit(neighbor); }
                
                // Add the cluster to the list of clusters.
                clusters.add(cluster);
    		}
    		
    	}
    	
    	// Return the generated list of clusters.
    	return clusters;
    }
    
    /**
     * <b>process</b><br/><br/>
     * <code>public void <b>process</b>(EventHeader event)</code><br/><br/>
     * Places hits from the current event into the event hit buffer
     * and processes the buffer to extract clusters. Clusters are
     * then stored in the event object.
     * @param event - The event to process.
     */
    public void process(EventHeader event) {
    	// Get the list calorimeter hits from the event.
    	List<CalorimeterHit> hitList = event.get(CalorimeterHit.class, hitCollectionName);
    	
    	// Store each hit in a set by its cell ID so that it may be
    	// easily acquired later.
    	HashMap<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>(hitList.size());
    	for(CalorimeterHit hit : hitList) { hitMap.put(hit.getCellID(), hit); }
    	
    	// Remove the last event from the hit buffer and the new one.
    	hitBuffer.removeLast();
    	hitBuffer.addFirst(hitMap);
    	eventBuffer.removeLast();
    	eventBuffer.addFirst(event);
    	
    	// Process the current event.
    	processEvent();
    }
    
    /**
     * <b>processEvent</b><br/><br/>
     * <code>private void <b>processEvent</b>()</code><br/><br/>
     * Handles running the clustering algorithm and placing the results
     * in the appropriate event header. Note that this is separate from
     * <code>process</code> so that it can be called from <code>endOfData
     * </code> as well.
     */
    private void processEvent() {
    	// Get the current event.
    	EventHeader event = eventBuffer.get(clusterWindow);
    	
    	// If the current event is null, then it is a blank event we
    	// have inserted to simulate either past events at the start
    	// of a run, or future events at the end of one. It requires
    	// no further action.
    	if(event == null) { return; }
    	
    	// Run the clustering algorithm on the buffer.
    	List<HPSEcalCluster> clusterList = getClusters();
    	
    	// Store the cluster list in the LCIO collection.
    	int flag = 1 << LCIOConstants.CLBIT_HITS;
    	event.put(clusterCollectionName, clusterList, HPSEcalCluster.class, flag);
    }
    
    /**
     * <b>setHitCollectionName</b><br/><br/>
     * <code>public void <b>setHitCollectionName</b>(String hitCollectionName)</code><br/><br/>
     * Sets the name of the LCIO collection containing the calorimeter
     * hits which should be used for clustering.
     * @param hitCollectionName - The appropriate collection name.
     */
    public void setHitCollectionName(String hitCollectionName) {
        this.hitCollectionName = hitCollectionName;
    }
    
    /**
     * <b>setClusterCollectionName</b><br/><br/>
     * <code>public void <b>setClusterCollectionName</b>(String clusterCollectionName)</code><br/><br/>
     * Sets the name of the LCIO collection in which the clusters
     * should be stored.
     * @param clusterCollectionName - The desired collection name.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
    	this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * <b>setClusterEnergyThreshold</b><br/><br/>
     * <code>public void <b>setClusterEnergyThreshold</b>(double clusterEnergyThreshold)</code><br/><br/>
     * Sets the minimum total cluster energy threshold below which a
     * cluster will be rejected.
     * @param clusterEnergyThreshold - Sets the minimum amount of energy
     * required for a cluster to be reported.
     */
    public void setClusterEnergyThreshold(double clusterEnergyThreshold) {
    	// A negative energy threshold is non-physical. All thresholds
    	// be at least zero.
    	if(clusterEnergyThreshold < 0.0) { this.clusterEnergyThreshold = 0.0; }
    	
    	// If the energy threshold is valid, then use it.
    	else { this.clusterEnergyThreshold = clusterEnergyThreshold; }
    }
    
    /**
     * <b>setCalorimeterName</b><br/><br/>
     * <code>public void <b>setCalorimeterName</b>(String calorimeterName)</code><br/><br/>
     * Sets the name of the calorimeter sub-detector stored in the
     * <code>Detector</code> object that is to be used for this run.
     * @param calorimeterName - The calorimeter's name.
     */
    public void setCalorimeterName(String calorimeterName) { this.calorimeterName = calorimeterName; }
    
    /**
     * <b>setClusterWindow</b><br/><br/>
     * <code>public void <b>setClusterWindow</b>(int clusterWindow)</code><br/><br/>
     * Sets the number of clock cycles before and after a given cycle
     * that will be used when checking whether a given hit is a local
     * maximum in both time and space. Note that a value of <code>N
     * </code> indicates that <code>N</code> clock cycles before and
     * <code>N</code> clock cycles after will be considered. Thusly,
     * a total of <code>2N + 1</code> clock cycles will be used total.
     * @param clusterWindow - The number of additional clock cycles to
     * include in the clustering checks. A negative value will be
     * treated as zero.
     */
    public void setClusterWindow(int clusterWindow) {
    	// The cluster window of must always be at least zero.
    	if(clusterWindow < 0) { this.clusterWindow = 0; }
    	
    	// If the cluster window is non-zero, then store it. Note that
    	// we double the size of the window because events are 2 ns,
    	// while clock cycles are 4 ns.
    	else { this.clusterWindow = 2 * clusterWindow; }
    }
    
    /**
     * <b>setSeedEnergyThreshold</b><br/><br/>
     * <code>public void <b>setSeedEnergyThreshold</b>(double seedEnergyThreshold)</code><br/><br/>
     * Sets the minimum energy threshold below which hits will not be
     * considered as cluster centers.
     * @param seedEnergyThreshold - The minimum energy for a cluster
     * center.
     */
    public void setSeedEnergyThreshold(double seedEnergyThreshold) {
    	// A negative energy threshold is non-physical. All thresholds
    	// be at least zero.
    	if(seedEnergyThreshold < 0.0) { this.seedEnergyThreshold = 0.0; }
    	
    	// If the energy threshold is valid, then use it.
    	else { this.seedEnergyThreshold = seedEnergyThreshold; }
    }
    
    /**
     * <b>startOfData</b><br/><br/>
     * <code>public void <b>startOfData</b>()</code><br/><br/>
     * Initializes the clusterer. This ensures that the collection name
     * for the calorimeter hits from which clusters are to be generated,
     * along with the calorimeter name, have been defined. Method also
     * initializes the event hit buffer.
     */
    public void startOfData() {
        // Make sure that there is a cluster collection name into
    	// which clusters may be placed.
        if (hitCollectionName == null) {
            throw new RuntimeException("The parameter hitCollectionName was not set!");
        }
        
        // Make sure that there is a calorimeter detector.
        if (calorimeterName == null) {
            throw new RuntimeException("The parameter calorimeterName was not set!");
        }
        
        // Initiate the hit buffer.
        hitBuffer = new LinkedList<Map<Long, CalorimeterHit>>();
        eventBuffer = new LinkedList<EventHeader>();
        
        // Populate the event buffer with (2 * clusterWindow + 1)
        // empty events. These empty events represent the fact that
        // the first few events will not have any events in the past
        // portion of the buffer.
        int bufferSize = (2 * clusterWindow) + 1;
        for(int i = 0; i < bufferSize; i++) {
        	hitBuffer.add(new HashMap<Long, CalorimeterHit>(0));
        	eventBuffer.add(null);
        }
    }
}
