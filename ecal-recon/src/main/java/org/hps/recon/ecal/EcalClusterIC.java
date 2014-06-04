package org.hps.recon.ecal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import org.hps.recon.ecal.HPSEcalClusterIC;


/**
 * This Driver creates clusters from the CalorimeterHits of an
 * {@link org.lcsim.geometry.subdetectur.HPSEcal3} detector.
 * 
 * This clustering logic is based on that from the CLAS-Note-2005-001. 
 * This sorts hits from highest to lowest energy and build clusters around 
 * each local maximum/seed hit. Common hits are distributed between clusters 
 * when minimum between two clusters. There is a threshold cut for minimum
 * hit energy, minimum cluster energy, and minimum seed hit energy. There is 
 * also a timing threshold with respect to the seed hit. All of these parameters
 * are tunable and should be refined with more analysis. 
 *
 *
 * @author Holly Szumila-Vance <hvanc001@odu.edu>
 * @author Kyle McCarty <mccaky@gmail.com>
 *
 */
public class EcalClusterIC extends Driver {
	// File writer to output cluster results.
    FileWriter writeHits;
    // LCIO collection name for calorimeter hits.
    String ecalCollectionName;
    // Name of the calorimeter detector object.
    String ecalName = "Ecal";
    // LCIO cluster collection name to which to write.
    String clusterCollectionName = "EcalClusters";
    // File path to which to write event display output.
    String outfile = "cluster-hit-IC.txt";
    // Map of crystals to their neighbors.
    NeighborMap neighborMap = null;
    // Minimum energy threshold for hits; lower energy hits will be
    // excluded from clustering. Units in GeV.
    double hitEnergyThreshold = 0.0075;
    // Minimum energy threshold for seed hits; if seed hit is below
    // cluster is excluded from output. Units in GeV.
    double seedEnergyThreshold = 0.2;
    // Minimum energy threshold for cluster hits; if total cluster
    // energy is below, the cluster is excluded. Units in GeV.
    double clusterEnergyThreshold = 0.4;  
    // A Comparator that sorts CalorimeterHit objects from highest to
    // lowest energy.
    private static final EnergyComparator ENERGY_COMP = new EnergyComparator();
    // Track the event number for the purpose of outputting to event
    // display format.
    private int eventNum = -1;
    // Apply time cut to hits
    boolean timeCut = false;
    // Minimum time cut window range. Units in ns.
    double minTime = 0.0;
    // Maximum time cut window range. Units in ns.
    double timeWindow = 20.0;
    
       
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
    
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }
    
    /**
     * Minimum energy for a hit to be used in a cluster. Default of 0.0075 GeV
     *
     * @param hitEnergyThreshold
     */
    public void sethitEnergyThreshold(double hitEnergyThreshold) {
        this.hitEnergyThreshold = hitEnergyThreshold;
    }

    /**
     * Minimum energy for a seed hit. Default of 0.2 GeV
     *
     * @param seedEnergyThreshold
     */
    public void setseedEnergyThreshold(double seedEnergyThreshold) {
        this.seedEnergyThreshold = seedEnergyThreshold;
    }
    
    /**
     * Minimum energy for a cluster. Default of 0.4 GeV
     *
     * @param clusterEnergyThreshold
     */
    public void setclusterEnergyThreshold(double clusterEnergyThreshold) {
        this.clusterEnergyThreshold = clusterEnergyThreshold;
    }
    
    /**
     * Apply time cuts to hits. Defaults to false.
     *
     * @param timeCut
     */
    public void setTimeCut(boolean timeCut) {
        this.timeCut = timeCut;
    }

    /**
     * Minimum hit time, if timeCut is true. Default of 0 ns.
     *
     * @param minTime
     */
    public void setMinTime(double minTime) {
        this.minTime = minTime;
    }

    /**
     * Width of time window, if timeCut is true. Default of 20 ns.
     *
     * @param timeWindow
     */
    public void setTimeWindow(double timeWindow) {
        this.timeWindow = timeWindow;
    }
    
    
    public void startOfData() {
    	// Make sure that the calorimeter hit collection name is defined.
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
        
        // Make sure the name of calorimeter detector is defined.
        if (ecalName == null) {
            throw new RuntimeException("The parameter ecalName was not set!");
        }
        
        // Create a file writer and clear the output file, if it exists.
        try {
            writeHits = new FileWriter(outfile);
            writeHits.write("");
        }
        catch(IOException e) { }
    }

    public void detectorChanged(Detector detector) {
        // Get the calorimeter.
    	HPSEcal3 ecal = (HPSEcal3) detector.getSubdetector(ecalName);
    	
        // Store the map of neighbor crystals for the current calorimeter set-up.
        neighborMap = ecal.getNeighborMap();
    }

    public void process(EventHeader event) {
    	// Make sure the current event contains calorimeter hits.
        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            // Get the list of raw calorimeter hits.
            List<CalorimeterHit> hitList = event.get(CalorimeterHit.class, ecalCollectionName);
            
            // Generate clusters from the calorimeter hits.
            List<HPSEcalClusterIC> clusterList = null;
            try { clusterList = createClusters(hitList); }
            catch(IOException e) { }
            
            // If clusters were successfully created, put them in the event.
            if(clusterList != null) {
	            int flag = 1 << LCIOConstants.CLBIT_HITS;
	            event.put(clusterCollectionName, clusterList, HPSEcalClusterIC.class, flag);
            }
        }
    }

    public List<HPSEcalClusterIC> createClusters(List<CalorimeterHit> hitList) throws IOException {
        // Create a list to store the newly created clusters in.
        ArrayList<HPSEcalClusterIC> clusterList = new ArrayList<HPSEcalClusterIC>();
        
        // Sort the list of hits by energy.
        Collections.sort(hitList, ENERGY_COMP);
        
        // Filter the hit list of any hits that fail to pass the
        // designated threshold.
        filterLoop:
        for(int index = hitList.size() - 1; index >= 0; index--) {
        	// If the hit is below threshold or outside of time window, kill it.
        	if((hitList.get(index).getCorrectedEnergy() < hitEnergyThreshold)||
        			(timeCut && (hitList.get(index).getTime() < minTime || hitList.get(index).getTime() > minTime + timeWindow))) {
        		hitList.remove(index);
        	}
        	
        	// Since the hits are sorted by energy from highest to
        	// lowest, any hit that is above threshold means that all
        	// subsequent hits will also be above threshold. Continue through
        	// list to check in time window. 
        	else { continue; }
        }
        
    	// Create a map to connect the cell ID of a calorimeter crystal
        // to the hit which occurred in that crystal.
    	HashMap<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
        for (CalorimeterHit hit : hitList) { hitMap.put(hit.getCellID(), hit); }
        
        // Map a crystal to a list of all clusters in which it is a member.
        Map<CalorimeterHit, List<CalorimeterHit>> commonHits = new HashMap<CalorimeterHit, List<CalorimeterHit>>();
        
        // Map a crystal to the seed of the cluster of which it is a member.
        HashMap<CalorimeterHit, CalorimeterHit> hitSeedMap = new HashMap<CalorimeterHit, CalorimeterHit>();
        
      	// Set containing hits immediately around a seed hit.
      	HashSet<CalorimeterHit> surrSeedSet = new HashSet<CalorimeterHit>();
        
        // Loop through all calorimeter hits to locate seeds and perform
        // first pass calculations for component and common hits.
        for (CalorimeterHit hit : hitList) {
        	// Get the set of all neighboring crystals to the current hit.
            Set<Long> neighbors = neighborMap.get(hit.getCellID());
            
            // Generate a list to store any neighboring hits in.
            ArrayList<CalorimeterHit> neighborHits = new ArrayList<CalorimeterHit>();
            
            // Sort through the set of neighbors and, if a hit exists
            // which corresponds to a neighbor, add it to the list of
            // neighboring hits.
            for (Long neighbor : neighbors) {
            	// Get the neighboring hit.
            	CalorimeterHit neighborHit = hitMap.get(neighbor);
            	
            	// If it exists, add it to the list.
            	if(neighborHit != null) { neighborHits.add(neighborHit); }
            }
            
            // Track whether the current hit is a seed hit or not.
            boolean isSeed = true;
            
            // Loops through all the neighboring hits to determine if
            // the current hit is the local maximum within its set of
            // neighboring hits.
            seedHitLoop:
            for(CalorimeterHit neighbor : neighborHits) {
            	if(hit.getCorrectedEnergy() <= neighbor.getCorrectedEnergy()) {
            		isSeed = false;
            		break seedHitLoop;
            	}
            }
            
            // If this hit is a seed hit, just map it to itself.
            if (isSeed) { hitSeedMap.put(hit, hit); }
            
            // If this hit is not a seed hit, see if it should be
            // attached to any neighboring seed hits.
            else {
                // Sort through the list of neighboring hits.
                for (CalorimeterHit neighborHit : neighborHits) {
                	// Check whether the neighboring hit is a seed.
                	if(hitSeedMap.get(neighborHit) == neighborHit) {
                        // If the neighboring hit is a seed hit and the
                        // current hit has been associated with a cluster,
                        // then it is a common hit between its previous
                        // seed and the neighboring seed.
                        if (hitSeedMap.containsKey(hit)) {
                        	// Check and see if a list of common seeds
                        	// for this hit already exists or not.
                        	List<CalorimeterHit> commonHitList = commonHits.get(hit);
                        	
                        	// If it does not, make a new one.
                        	if(commonHitList == null) { commonHitList = new ArrayList<CalorimeterHit>(); }
                        	
                        	// Add the neighbors to the seeds to set of
                        	// common seeds.
                            commonHitList.add(neighborHit);
                            commonHitList.add(hitSeedMap.get(hit));
                            
                            // Put the common seed list back into the set.
                            commonHits.put(hit, commonHitList);
                        }
                        
                        // If the neighboring hit is a seed hit and the
                    	// current hit has not been added to a cluster yet
                    	// associate it with the neighboring seed and note
                        // that it has been clustered.
                        else {
                        	hitSeedMap.put(hit, neighborHit);
                        	surrSeedSet.add(hit);
                        }
                	}
                }
            }
        } // End primary seed loop.
        
        // Performs second pass calculations for component hits.
        secondaryHitsLoop:
        for (CalorimeterHit secondaryHit : hitList) {
        	// If the secondary hit is not associated with a seed, then
        	// the rest of there is nothing further to be done.
        	if(!hitSeedMap.containsKey(secondaryHit)) { continue secondaryHitsLoop; }
        	
        	// Get the secondary hit's neighboring crystals.
            Set<Long> secondaryNeighbors = neighborMap.get(secondaryHit.getCellID());
            
            // Make a list to store the hits associated with the
            // neighboring crystals.
            List<CalorimeterHit> secondaryNeighborHits = new ArrayList<CalorimeterHit>();
            
            // Loop through the neighboring crystals.
            for (Long secondaryNeighbor : secondaryNeighbors) {
            	// Get the hit associated with the neighboring crystal.
            	CalorimeterHit secondaryNeighborHit = hitMap.get(secondaryNeighbor);
            	
            	// If the neighboring crystal exists and is not already
            	// in a cluster, add it to the list of neighboring hits.
                if (secondaryNeighborHit != null && !hitSeedMap.containsKey(secondaryNeighborHit)) { //!clusteredHitSet.contains(secondaryNeighborHit)) {
                	secondaryNeighborHits.add(secondaryNeighborHit);
                }
            }
            
            // Loop over the secondary neighbor hits.
            for (CalorimeterHit secondaryNeighborHit : secondaryNeighborHits) {
            	// If the neighboring hit is of lower energy than the
            	// current secondary hit, then associate the neighboring
            	// hit with the current secondary hit's seed.
                if (secondaryNeighborHit.getCorrectedEnergy() < secondaryHit.getCorrectedEnergy()) {
                	hitSeedMap.put(secondaryNeighborHit, hitSeedMap.get(secondaryHit));
                }
            }
        } // End component hits loop.
        
        // Performs second pass calculations for common hits.
        commonHitsLoop:
        for (CalorimeterHit clusteredHit : hitSeedMap.keySet()) {
        	// Seed hits are never common hits and can be skipped.
        	if(hitSeedMap.get(clusteredHit) == clusteredHit || surrSeedSet.contains(clusteredHit)) { continue commonHitsLoop; }
        	
    		// Get the current clustered hit's neighboring crystals.
            Set<Long> clusteredNeighbors = neighborMap.get(clusteredHit.getCellID());
            
            // Store a list of all the clustered hits neighboring
            // crystals which themselves contain hits.
            List<CalorimeterHit> clusteredNeighborHits = new ArrayList<CalorimeterHit>();
            
            // Loop through the neighbors and see if they have hits.
            for (Long neighbor : clusteredNeighbors) {
            	// Get the hit associated with the neighbor.
            	CalorimeterHit clusteredNeighborHit = hitMap.get(neighbor);
            	
            	// If it exists, add it to the neighboring hit list.
                if (clusteredNeighborHit != null) {
                	clusteredNeighborHits.add(clusteredNeighborHit);
                }
            }
            
            // Get the seed hit associated with this clustered hit.
            CalorimeterHit clusteredHitSeed = hitSeedMap.get(clusteredHit);
            
            // Loop over the clustered neighbor hits.
            for (CalorimeterHit clusteredNeighborHit : clusteredNeighborHits) {
            	// Check to make sure that the clustered neighbor hit
            	// is not already associated with the current clustered
            	// hit's seed.
                if (hitSeedMap.get(clusteredNeighborHit) != clusteredHitSeed) {
                    if (clusteredHit.getCorrectedEnergy() < clusteredNeighborHit.getCorrectedEnergy()) {
                    	// Check and see if a list of common seeds
                    	// for this hit already exists or not.
                    	List<CalorimeterHit> commonHitList = commonHits.get(clusteredHit);
                    	
                    	// If it does not, make a new one.
                    	if(commonHitList == null) { commonHitList = new ArrayList<CalorimeterHit>(); }
                    	
                    	// Add the neighbors to the seeds to set of
                    	// common seeds.
                        commonHitList.add(clusteredHitSeed);
                        commonHitList.add(hitSeedMap.get(clusteredNeighborHit));
                        
                        // Put the common seed list back into the set.
                        commonHits.put(clusteredHit, commonHitList);
                    }
                }
            }
        } // End common hits loop.
        
        // Remove any common hits from the clustered hits collection.
        for(CalorimeterHit commonHit : commonHits.keySet()) {
        	hitSeedMap.remove(commonHit);
        }
        
        
        
        /*
         * All hits are sorted from above. The next part of the code is for calculating energies. Still 
         * needs implementation into new cluster collection so as to preserve shared hit energy 
         * distribution within clusters.
         */
                
        //Create map to contain the total energy of each cluster
        Map<CalorimeterHit, Double> seedEnergy = new HashMap<CalorimeterHit, Double>();
        
        // Get energy of each cluster, excluding common hits
        for (CalorimeterHit iSeed : hitList) {
            if(hitSeedMap.get(iSeed) == iSeed) {
            	seedEnergy.put(iSeed, 0.0);
            }
        }
        
        //Putting total cluster energies excluding common hit energies into map with seed keys    
        for (Map.Entry<CalorimeterHit, CalorimeterHit> entry : hitSeedMap.entrySet()) {
            CalorimeterHit eSeed = entry.getValue();
            double eEnergy = seedEnergy.get(eSeed);
            eEnergy += entry.getKey().getRawEnergy();
            seedEnergy.put(eSeed, eEnergy);
        }
        
        //Distribute common hit energies with clusters
        Map<CalorimeterHit, Double> seedEnergyTot = seedEnergy;
        for (Map.Entry<CalorimeterHit, List<CalorimeterHit>> entry1 : commonHits.entrySet()) {
            CalorimeterHit commonCell = entry1.getKey();
            List<CalorimeterHit> commSeedList = entry1.getValue();
            CalorimeterHit seedA = commSeedList.get(0);
            CalorimeterHit seedB = commSeedList.get(1);
            double eFractionA = seedEnergy.get(seedA)/(seedEnergy.get(seedA)+seedEnergy.get(seedB));
        	double eFractionB = seedEnergy.get(seedB)/(seedEnergy.get(seedA)+seedEnergy.get(seedB));
            double currEnergyA = seedEnergyTot.get(seedA);
            double currEnergyB = seedEnergyTot.get(seedB);
            currEnergyA += eFractionA * commonCell.getCorrectedEnergy();
            currEnergyB += eFractionB * commonCell.getCorrectedEnergy();

            seedEnergyTot.put(seedA, currEnergyA);
            seedEnergyTot.put(seedB, currEnergyB);
        }
        
        
        
        
        /*
         * Prints the results in event display format. Not analyzed
         * for efficiency, as this will ultimately not be a part of
         * the driver and should be handled by the event display output
         * driver instead.
         */
        // Only write to the output file is something actually exists.
        if (hitMap.size() != 0) {
        	// Increment the event number.
        	eventNum++;
        	
        	// Write the event header.
//        	writeHits.append(String.format("Event\t%d%n", eventNum));
        	
        	// Write the calorimeter hits that passed the energy cut.
            for (CalorimeterHit n : hitList) {
            	int hix = n.getIdentifierFieldValue("ix");
            	int hiy = n.getIdentifierFieldValue("iy");
            	double energy = n.getCorrectedEnergy();
//            	writeHits.append(String.format("EcalHit\t%d\t%d\t%f%n", hix, hiy, energy));
            }
            
            
            for (Map.Entry<CalorimeterHit, CalorimeterHit> entry2 : hitSeedMap.entrySet()) {
                if ((entry2.getKey() == entry2.getValue())&&(entry2.getKey().getCorrectedEnergy()>=seedEnergyThreshold)
                		&&(seedEnergyTot.get(entry2.getKey())>=clusterEnergyThreshold)) {//seed and passes all thresholds
                	int six = entry2.getKey().getIdentifierFieldValue("ix");
                	int siy = entry2.getKey().getIdentifierFieldValue("iy");
                	double energy = seedEnergyTot.get(entry2.getKey());
//                	writeHits.append(String.format("Cluster\t%d\t%d\t%f%n", six, siy, energy));
                	
                    HPSEcalClusterIC cluster = new HPSEcalClusterIC(entry2.getKey());
                    cluster.addHit(entry2.getKey());
                    cluster.setEnergy(energy);

                    for (Map.Entry<CalorimeterHit, CalorimeterHit> entry3 : hitSeedMap.entrySet()) {
                        if (entry3.getValue() == entry2.getValue()) {
                        	int ix = entry3.getKey().getIdentifierFieldValue("ix");
                        	int iy = entry3.getKey().getIdentifierFieldValue("iy");
//                       	writeHits.append(String.format("CompHit\t%d\t%d%n", ix, iy));
                        	
                            cluster.addHit(entry3.getKey());
                        }
                    }
                    for (Map.Entry<CalorimeterHit, List<CalorimeterHit>> entry4 : commonHits.entrySet()) {
                        if (entry4.getValue().contains(entry2.getKey())) {
                        	int ix = entry4.getKey().getIdentifierFieldValue("ix");
                        	int iy = entry4.getKey().getIdentifierFieldValue("iy");
//                        	writeHits.append(String.format("SharHit\t%d\t%d%n", ix, iy));
                        	
                        	// Added in shared hits for energy distribution between clusters, changed by HS 02JUN14
//                            cluster.addHit(entry4.getKey());
                            cluster.addSharedHit(entry4.getKey());
                        }
                    }

                    clusterList.add(cluster);
                }
            }
            
            // Write the event termination header.
//            writeHits.append("EndEvent\n");
        } //End event display output loop.
        
        
        
        
        
        // Return the resulting cluster list.
        return clusterList;
    }
    
    public void endOfData() {
    	// Close the event display output writer.
        try { writeHits.close(); }
        catch (IOException e) { }
    }
    
    private static class EnergyComparator implements Comparator<CalorimeterHit> {
        public int compare(CalorimeterHit o1, CalorimeterHit o2) {
        	// If the energies are equivalent, the same, the two hits
        	// are considered equivalent.
        	if(o1.getCorrectedEnergy() == o2.getCorrectedEnergy()) { return 0; }
        	
        	// Higher energy hits are ranked higher than lower energy hits.
        	else if(o1.getCorrectedEnergy() > o2.getCorrectedEnergy()) { return -1; }
        	
        	// Lower energy hits are ranked lower than higher energy hits.
        	else { return 1; }
        }
    }
}
