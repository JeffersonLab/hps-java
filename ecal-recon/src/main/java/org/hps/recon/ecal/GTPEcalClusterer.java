package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.HashMap;
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
 * Class <code>GTPCalorimeterClusterer</code> processes events and converts hits
 * into clusters, where appropriate. It uses the modified 2014 clustering algorithm.<br/>
 * <br/>
 * For a hit to be a cluster center, it is required to have an energy above some
 * tunable minimum threshold. Additionally, the hit must be a local maximum with
 * respect to its neighbors and itself over a tunable (default 2) clock cycles.
 * Hits that pass these checks are then required to additional have a total
 * cluster energy that exceeds another tunable minimum threshold.<br/>
 * <br/>
 * A hit is added to a cluster as a component if it has a non-zero energy and
 * within the aforementioned tunable time buffer used for clustering and is
 * either at the same location as the seed hit or is a neighbor to the seed hit.
 * @author Kyle McCarty
 * @author Sho Uemura
 */
public class GTPEcalClusterer extends Driver {
    /**
     * <b>calorimeter</b><br/><br/>
     * <code>private HPSEcal3 <b>calorimeter</b></code><br/><br/>
     * The sub-detector representing the calorimeter.
     */
    private HPSEcal3 calorimeter;
    /**
     * <b>ecalName</b><br/><br/>
     * <code>private String <b>ecalName</b></code><br/><br/>
     * The name of the calorimeter sub-detector stored in the
     * <code>
     * Detector</code> object for this run.
     */
    private String ecalName;
    /**
     * <b>clusterCollectionName</b><br/><br/>
     * <code>private String <b>clusterCollectionName</b></code><br/><br/>
     * The name of the LCIO collection name in which the clusters will be
     * stored.
     */
    private String clusterCollectionName = "EcalClusters";
    /**
     * <b>clusterWindow</b><br/><br/>
     * <code>private int <b>clusterWindow</b></code><br/><br/>
     * Indicates the number of FADC clock cycles (each cycle is 4 ns) before and
     * after a given cycle that should be considered when checking if a cluster
     * is a local maximum in space-time.
     */
    private int clusterWindow = 2;
    /**
     * <b>hitBuffer</b><br/><br/>
     * <code>private LinkedList<List<CalorimeterHit>> <b>hitBuffer</b></code><br/><br/>
     * Stores a set of all the hits occurring in each clock cycle for the number
     * of clock cycles that should be considered for clustering.
     */
    private LinkedList<Map<Long, CalorimeterHit>> hitBuffer;
    /**
     * <b>ecalCollectionName</b><br/><br/>
     * <code>private String <b>ecalCollectionName</b></code><br/><br/>
     * The name of LCIO collection containing the calorimeter hits that are to
     * be used for clustering.
     */
    private String ecalCollectionName;
    /**
     * <b>neighborMap</b><br/><br/>
     * <code>private NeighborMap <b>neighborMap</b></code><br/><br/>
     * Maps the
     * <code>long</code> crystal ID to the set of crystal IDs of the crystals
     * which are adjacent to the key.
     */
    private NeighborMap neighborMap = null;
    /**
     * <b>seedEnergyThreshold</b><br/><br/>
     * <code>private double <b>seedEnergyThreshold</b></code><br/><br/>
     * The minimum energy required for a hit to be considered as a cluster
     * center. Hits with energy less than this value will be ignored.
     */
    private double seedEnergyThreshold = 0.05;
    /**
     * <b>limitClusterRange</b><br/><br/>
     * <code>private boolean <b>limitClusterRange</b></code><br/><br/>
     * Whether an asymmetric or symmetric window should be used for
     * adding hits to a cluster.
     */
    private boolean limitClusterRange = false;
    
    /**
     * Initializes detector-dependent parameters for clustering. Method is
     * responsible for determining which crystals are valid cluster centers
     * (stored in
     * <code>validCrystals</code>), defining the detector object (stored in
     * <code>calorimeter</code>), defining the detector ID decoder (stored in
     * <code>decoder</code>), and defining the mapping of crystal IDs to the
     * crystal's neighbors (defined in
     * <code>neighborMap</code>).
     *
     * @param detector - The new detector to use.
     */
    @Override
    public void detectorChanged(Detector detector) {
        // Get the calorimeter object.
        calorimeter = (HPSEcal3) detector.getSubdetector(ecalName);
        
        // Get a map to associate crystals with their neighbors.
        neighborMap = calorimeter.getNeighborMap();
    }
    
    /**
     * Generates a list of clusters from the current hit buffer. The "present"
     * event is taken to be the list of hits occurring at index
     * <code>clusterWindow</code>, which is the middle of the buffer.
     *
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
        seedLoop:
        for (Long currentID : currentHits.keySet()) {
            // Get the actual hit object.
            CalorimeterHit currentHit = currentHits.get(currentID);
            
            // Store the energy of the current hit.
            double currentEnergy = currentHit.getRawEnergy();
            
            // If the hit energy is lower than the minimum threshold,
            // then we immediately reject this hit as a possible cluster.
            if (currentEnergy < seedEnergyThreshold) {
                continue seedLoop;
            }
            
            // Store the crystals that are part of this potential cluster, 
            // starting with the cluster seed candidate.
            HPSEcalCluster cluster = new HPSEcalCluster(currentHit);
            cluster.addHit(currentHit);
            
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
        }
        
        // Return the generated list of clusters.
        return clusters;
    }
    
    /**
     * Places hits from the current event into the event hit buffer and
     * processes the buffer to extract clusters. Clusters are then stored in the
     * event object.
     *
     * @param event - The event to process.
     */
    @Override
    public void process(EventHeader event) {
        // Only run the clusterer if this event has the calorimeter hit collection. 
        // This ensures this driver makes clusters in sync with the FADC readout cycle.
        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            // Get the list of calorimeter hits from the event.
            List<CalorimeterHit> hitList = event.get(CalorimeterHit.class, ecalCollectionName);
            
            // Store each hit in a set by its cell ID so that it may be
            // easily acquired later.
            HashMap<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
            for (CalorimeterHit hit : hitList) {
                hitMap.put(hit.getCellID(), hit);
            }
            
            // Remove the last event from the hit buffer and add the new one.
            hitBuffer.removeLast();
            hitBuffer.addFirst(hitMap);
            
            // Run the clustering algorithm on the buffer.
            List<HPSEcalCluster> clusterList = getClusters();
            
            // Store the cluster list in the LCIO collection.
            int flag = 1 << LCIOConstants.CLBIT_HITS;
            event.put(clusterCollectionName, clusterList, HPSEcalCluster.class, flag);
        }
    }
    
    /**
     * Sets the name of the LCIO collection containing the calorimeter hits
     * which should be used for clustering.
     *
     * @param ecalCollectionName - The appropriate collection name.
     */
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
    
    /**
     * Sets the name of the LCIO collection in which the clusters should be
     * stored.
     *
     * @param clusterCollectionName - The desired collection name.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * Sets the name of the calorimeter sub-detector stored in the
     * <code>Detector</code> object that is to be used for this run.
     *
     * @param ecalName - The calorimeter's name.
     */
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }
    
    /**
     * Sets the number of clock cycles before and after a given cycle that will
     * be used when checking whether a given hit is a local maximum in both time
     * and space. Note that a value of
     * <code>N
     * </code> indicates that
     * <code>N</code> clock cycles before and
     * <code>N</code> clock cycles after will be considered. Thusly, a total of
     * <code>2N + 1</code> clock cycles will be used total.
     *
     * @param clusterWindow - The number of additional clock cycles to include
     * in the clustering checks. A negative value will be treated as zero.
     */
    public void setClusterWindow(int clusterWindow) {
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
     * 
     * @param limitClusterRange - <code>true</code> indicates that
     * the asymmetric clustering window should be used and <code>
     * false</code> that the symmetric window should be used.
     */
    public void setLimitClusterRange(boolean limitClusterRange) {
    	this.limitClusterRange = limitClusterRange;
    }
    
    /**
     * Sets the minimum energy threshold below which hits will not be considered
     * as cluster centers.
     *
     * @param seedEnergyThreshold - The minimum energy for a cluster center.
     */
    public void setSeedEnergyThreshold(double seedEnergyThreshold) {
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
     * Initializes the clusterer. This ensures that the collection name for the
     * calorimeter hits from which clusters are to be generated, along with the
     * calorimeter name, have been defined. Method also initializes the event
     * hit buffer.
     */
    @Override
    public void startOfData() {
        // Make sure that there is a cluster collection name into
        // which clusters may be placed.
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
        
        // Make sure that there is a calorimeter detector.
        if (ecalName == null) {
            throw new RuntimeException("The parameter ecalName was not set!");
        }
        
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
}
