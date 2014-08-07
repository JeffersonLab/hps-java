package org.hps.readout.ecal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.EventHeader;

/**
 * Class <code>FADCVariableTriggerDriver</code> reads reconstructed
 * clusters and makes trigger decisions on them. It is designed to
 * trigger off 2.2 GeV beam A' events. Cuts can either be set manually
 * in a steering file or automatically by specifying a background level.
 * The code for generating trigger pairs and handling the coincidence
 * window comes from <code>FADCTriggerDriver</code>.
 * 
 * @author Kyle McCarty
 * @see FADCTriggerDriver
 */
public class FADCVariableTriggerDriver extends TriggerDriver {
    // ==================================================================
    // ==== Trigger Cut Default Parameters ==============================
    // ==================================================================
    private int minHitCount = 1;								// Minimum required cluster hit count threshold. (Hits)			
    private double seedEnergyHigh = Double.MAX_VALUE;			// Maximum allowed cluster seed energy. (GeV)
    private double seedEnergyLow = Double.MIN_VALUE;			// Minimum required cluster seed energy. (GeV)
    private double clusterEnergyHigh = 1.5 * ECalUtils.GeV;		// Maximum allowed cluster total energy. (GeV)
    private double clusterEnergyLow = .1 * ECalUtils.GeV;		// Minimum required cluster total energy. (GeV)
    private double energySumHigh = 1.9 * ECalUtils.GeV;			// Maximum allowed pair energy sum. (GeV)
    private double energySumLow = 0.0 * ECalUtils.GeV;			// Minimum required pair energy sum. (GeV)
    private double energyDifferenceHigh = 2.2 * ECalUtils.GeV;	// Maximum allowed pair energy difference. (GeV)
    private double energySlopeLow = 1.1;						// Minimum required pair energy slope value.
    private double coplanarityHigh = 35;						// Maximum allowed pair coplanarity deviation. (Degrees)
    
    // ==================================================================
    // ==== Trigger General Default Parameters ==========================
    // ==================================================================
    private String clusterCollectionName = "EcalClusters";		// Name for the LCIO cluster collection.
    private int pairCoincidence = 2;							// Maximum allowed time difference between clusters. (4 ns clock-cycles)
    private double energySlopeParamF = 0.005500;				// A parameter value used for the energy slope calculation.
    private double originX = 1393.0 * Math.tan(0.03052);		// ECal mid-plane, defined by photon beam position (30.52 mrad) at ECal face (z=1393 mm)
    private int backgroundLevel = -1;							// Automatically sets the cuts to achieve a predetermined background rate.
    
    // ==================================================================
    // ==== Driver Internal Variables ===================================
    // ==================================================================
    private Queue<List<HPSEcalCluster>> topClusterQueue = null;	// Store clusters on the top half of the calorimeter.
    private Queue<List<HPSEcalCluster>> botClusterQueue = null;	// Store clusters on the bottom half of the calorimeter.
    private int allClusters = 0;								// Track the number of clusters processed.
    private int allPairs = 0;									// Track the number of cluster pairs processed.
    private int clusterTotalEnergyCount = 0;					// Track the clusters which pass the total energy cut.
    private int clusterSeedEnergyCount = 0;						// Track the clusters which pass the seed energy cut.
    private int clusterHitCountCount = 0;						// Track the clusters which pass the hit count cut.
    private int pairEnergySumCount = 0;							// Track the pairs which pass the energy sum cut.
    private int pairEnergyDifferenceCount = 0;					// Track the pairs which pass the energy difference cut.
    private int pairEnergySlopeCount = 0;						// Track the pairs which pass the energy slope cut.
    private int pairCoplanarityCount = 0;						// Track the pairs which pass the coplanarity cut.
    
    /**
     * Prints out the results of the trigger at the end of the run.
     */
    @Override
    public void endOfData() {
    	// Print out the results of the trigger cuts.
    	System.out.printf("Trigger Processing Results%n");
    	System.out.printf("\tSingle-Cluster Cuts%n");
    	System.out.printf("\t\tTotal Clusters Processed     :: %d%n", allClusters);
    	System.out.printf("\t\tPassed Seed Energy Cut       :: %d%n", clusterSeedEnergyCount);
    	System.out.printf("\t\tPassed Hit Count Cut         :: %d%n", clusterHitCountCount);
    	System.out.printf("\t\tPassed Total Energy Cut      :: %d%n", clusterTotalEnergyCount);
    	System.out.printf("%n");
    	System.out.printf("\tCluster Pair Cuts%n");
    	System.out.printf("\t\tTotal Pairs Processed        :: %d%n", allPairs);
    	System.out.printf("\t\tPassed Energy Sum Cut        :: %d%n", pairEnergySumCount);
    	System.out.printf("\t\tPassed Energy Difference Cut :: %d%n", pairEnergyDifferenceCount);
    	System.out.printf("\t\tPassed Energy Slope Cut      :: %d%n", pairEnergySlopeCount);
    	System.out.printf("\t\tPassed Coplanarity Cut       :: %d%n", pairCoplanarityCount);
    	System.out.printf("%n");
    	System.out.printf("\tTrigger Count :: %d%n", numTriggers);
    	
    	// Run the superclass method.
        super.endOfData();
    }
    
    /**
     * Performs single cluster cuts for the event and passes any clusters
     * which survive to be formed into cluster pairs for the trigger.
     */
    @Override
    public void process(EventHeader event) {
    	// Process the list of clusters for the event, if it exists.
        if (event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
        	// Get the collection of clusters.
        	List<HPSEcalCluster> clusterList = event.get(HPSEcalCluster.class, clusterCollectionName);
        	
        	// Create a list to hold clusters which pass the single
        	// cluster cuts.
        	List<HPSEcalCluster> goodClusterList = new ArrayList<HPSEcalCluster>(clusterList.size());
        	
        	// Sort through the cluster list and add clusters that pass
        	// the single cluster cuts to the good list.
        	clusterLoop:
        	for(HPSEcalCluster cluster : clusterList) {
        		// Increment the number of processed clusters.
        		allClusters++;
        		
        		// ==== Seed Hit Energy Cut ====================================
        		// =============================================================
        		// If the cluster fails the cut, skip to the next cluster.
        		if(!clusterSeedEnergyCut(cluster)) { continue clusterLoop; }
        		
        		// Otherwise, note that it passed the cut.
        		clusterSeedEnergyCount++;
        		
        		// ==== Cluster Hit Count Cut ==================================
        		// =============================================================
        		// If the cluster fails the cut, skip to the next cluster.
        		if(!clusterHitCountCut(cluster)) { continue clusterLoop; }
        		
        		// Otherwise, note that it passed the cut.
        		clusterHitCountCount++;
        		
        		// ==== Cluster Total Energy Cut ===============================
        		// =============================================================
        		// If the cluster fails the cut, skip to the next cluster.
        		if(!clusterTotalEnergyCut(cluster)) { continue clusterLoop; }
        		
        		// Otherwise, note that it passed the cut.
        		clusterTotalEnergyCount++;
        		
        		// A cluster that passes all of the single-cluster cuts
        		// can be used in cluster pairs.
        		goodClusterList.add(cluster);
        	}
        	
        	// Put the good clusters into the cluster queue.
        	updateClusterQueues(goodClusterList);
        }
        
        // Perform the superclass event processing.
        super.process(event);
    }
    
    /**
     * Sets the trigger cuts automatically to a given background level.
     * 
     * @param backgroundLevel - The level to which the background should
     * be set. Actual background rates equal about (5 * backgroundLevel) kHz.
     */
    public void setBackgroundLevel(int backgroundLevel) {
    	this.backgroundLevel = backgroundLevel;
    }
    
    /**
     * Sets the name of the LCIO collection that contains the clusters.
     * 
     * @param clusterCollectionName - The cluster LCIO collection name.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * Sets the highest allowed energy a cluster may have and still
     * pass the cluster total energy single cluster cut. Value uses
     * units of GeV.
     *
     * @param clusterEnergyHigh - The parameter value.
     */
    public void setClusterEnergyHigh(double clusterEnergyHigh) {
        this.clusterEnergyHigh = clusterEnergyHigh * ECalUtils.GeV;
    }
    
    /**
     * Sets the lowest allowed energy a cluster may have and still
     * pass the cluster total energy single cluster cut. Value uses
     * units of GeV.
     *
     * @param clusterEnergyLow - The parameter value.
     */
    public void setClusterEnergyLow(double clusterEnergyLow) {
        this.clusterEnergyLow = clusterEnergyLow * ECalUtils.GeV;
    }
    
    /**
     * Sets the maximum deviation from coplanarity that a cluster pair
     * may possess and still pass the coplanarity pair cut. Value uses
     * units of degrees.
     *
     * @param maxCoplanarityAngle - The parameter value.
     */
    public void setCoplanarityHigh(double coplanarityHigh) {
        this.coplanarityHigh = coplanarityHigh;
    }
    
    /**
     * Sets the highest allowed energy difference a cluster pair may
     * have and still pass the cluster pair energy difference cut.
     * Value uses units of GeV.
     *
     * @param energyDifferenceHigh - The parameter value.
     */
    public void setEnergyDifferenceHigh(double energyDifferenceHigh) {
        this.energyDifferenceHigh = energyDifferenceHigh * ECalUtils.GeV;
    }
    
    /**
     * Sets the lowest allowed energy slope a cluster pair may
     * have and still pass the cluster pair energy slope cut.
     *
     * @param energySlopeLow - The parameter value.
     */
    public void setEnergySlopeLow(double energySlopeLow) {
    	this.energySlopeLow = energySlopeLow;
    }
    
    /**
     * Sets the highest allowed energy a cluster pair may have and
     * still pass the cluster pair energy sum cluster cut. Value uses
     * units of GeV.
     *
     * @param energySumHigh - The parameter value.
     */
    public void setEnergySumHigh(double energySumHigh) {
        this.energySumHigh = energySumHigh * ECalUtils.GeV;
    }
    
    /**
     * Sets the lowest allowed energy a cluster pair may have and
     * still pass the cluster pair energy sum cluster cut. Value uses
     * units of GeV.
     *
     * @param energySumHigh - The parameter value.
     */
    public void setEnergySumLow(double energySumLow) {
        this.energySumLow = energySumLow * ECalUtils.GeV;
    }
    
    /**
     * Sets the minimum number of hits needed for a cluster to pass
     * the hit count single cluster cut.
     *
     * @param minHitCount - The parameter value.
     */
    public void setMinHitCount(int minHitCount) {
        this.minHitCount = minHitCount;
    }
    
    /**
     * Sets X coordinate used as the origin for cluster coplanarity and
     * slope calculations. This defaults to the calorimeter mid-plane
     * and is in units of millimeters.
     *
     * @param originX - The parameter value.
     */
    public void setOriginX(double originX) {
        this.originX = originX;
    }
    
    /**
     * Sets the time range over which cluster pairs will be formed.
     * Value uses units of clock-cycles. Note that the number of
     * clock-cycles used is calculated as (2 * pairCoincidence) + 1.
     * 
     * @param pairCoincidence - The parameter value.
     */
    public void setPairCoincidence(int pairCoincidence) {
        this.pairCoincidence = pairCoincidence;
    }
    
    /**
     * Sets the highest allowed energy a seed hit may have and still
     * pass the seed hit energy single cluster cut. Value uses units
     * of GeV.
     *
     * @param seedEnergyHigh - The parameter value.
     */
    public void setSeedEnergyHigh(double seedEnergyHigh) {
        this.seedEnergyHigh = seedEnergyHigh * ECalUtils.GeV;
    }
    
    /**
     * Sets the lowest allowed energy a seed hit may have and still
     * pass the seed hit energy single cluster cut. Value uses units
     * of GeV.
     *
     * @param seedEnergyLow - The parameter value.
     */
    public void setSeedEnergyLow(double seedEnergyLow) {
        this.seedEnergyLow = seedEnergyLow * ECalUtils.GeV;
    }
    
    /**
     * Initializes the cluster pair queues and other variables.
     */
    @Override
    public void startOfData() {
    	// Make sure that a valid cluster collection name has been
    	// defined. If it has not, throw an exception.
        if (clusterCollectionName == null) {
            throw new RuntimeException("The parameter clusterCollectionName was not set!");
        }
    	
        // Initialize the top and bottom cluster queues.
        topClusterQueue = new LinkedList<List<HPSEcalCluster>>();
        botClusterQueue = new LinkedList<List<HPSEcalCluster>>();
        
        // Populate the top cluster queue. It should be populated with
        // a number of empty lists equal to (2 * pairCoincidence + 1).
        for (int i = 0; i < 2 * pairCoincidence + 1; i++) {
            topClusterQueue.add(new ArrayList<HPSEcalCluster>());
        }
        
        // Populate the bottom cluster queue. It should be populated with
        // a number of empty lists equal to (2 * pairCoincidence + 1).
        for (int i = 0; i < pairCoincidence + 1; i++) {
            botClusterQueue.add(new ArrayList<HPSEcalCluster>());
        }
        
        // If a background level has been set, pick the correct cuts.
        if(backgroundLevel != -1) { setBackgroundCuts(backgroundLevel); }
        
        // Run the superclass method.
        super.startOfData();
    }

    /**
     * Get a list of all unique cluster pairs in the event
     *
     * @param ecalClusters : List of ECal clusters
     * @return list of cluster pairs
     */
    protected List<HPSEcalCluster[]> getClusterPairsTopBot() {
        // Create a list to store cluster pairs. 
        List<HPSEcalCluster[]> clusterPairs = new ArrayList<HPSEcalCluster[]>();
        
        // Loop over all top-bottom pairs of clusters; higher-energy cluster goes first in the pair
        // To apply pair coincidence time, use only bottom clusters from the 
        // readout cycle pairCoincidence readout cycles ago, and top clusters 
        // from all 2*pairCoincidence+1 previous readout cycles
        for (HPSEcalCluster botCluster : botClusterQueue.element()) {
            for (List<HPSEcalCluster> topClusters : topClusterQueue) {
                for (HPSEcalCluster topCluster : topClusters) {
                	// The first cluster in a pair should always be
                	// the higher energy cluster. If the top cluster
                	// is higher energy, it goes first.
                    if (topCluster.getEnergy() > botCluster.getEnergy()) {
                        HPSEcalCluster[] clusterPair = {topCluster, botCluster};
                        clusterPairs.add(clusterPair);
                    }
                    
                    // Otherwise, the bottom cluster goes first.
                    else {
                        HPSEcalCluster[] clusterPair = {botCluster, topCluster};
                        clusterPairs.add(clusterPair);
                    }
                }
            }
        }
        
        // Return the cluster pair lists.
        return clusterPairs;
    }
    
	/**
	 * Determines if the event produces a trigger.
	 * 
	 * @return Returns <code>true</code> if the event produces a trigger
	 * and <code>false</code> if it does not.
	 */
	@Override
	protected boolean triggerDecision(EventHeader event) {
    	// If there is a list of clusters present for this event,
    	// check whether it passes the trigger conditions.
    	if (event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
        	return testTrigger();
        }
        
        // Otherwise, this event can not produce a trigger and should
        // return false automatically.
        else { return false; }
	}
    
    /**
     * Checks whether the argument cluster possesses the minimum
     * allowed hits.
     * 
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterHitCountCut(HPSEcalCluster cluster) {
    	return (getValueClusterHitCount(cluster) >= minHitCount);
    }
    
    /**
     * Checks whether the argument cluster seed hit falls within the
     * allowed seed hit energy range.
     * 
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterSeedEnergyCut(HPSEcalCluster cluster) {
    	// Get the cluster seed energy.
    	double energy = getValueClusterSeedEnergy(cluster);
    	
    	// Check that it is above the minimum threshold and below the
    	// maximum threshold.
    	return (energy < seedEnergyHigh) && (energy > seedEnergyLow);
    }
    
    /**
     * Checks whether the argument cluster falls within the allowed
     * cluster total energy range.
     * 
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterTotalEnergyCut(HPSEcalCluster cluster) {
    	// Get the total cluster energy.
    	double energy = getValueClusterTotalEnergy(cluster);
    	
    	// Check that it is above the minimum threshold and below the
    	// maximum threshold.
    	return (energy < clusterEnergyHigh) && (energy > clusterEnergyLow);
    }
    
    /**
     * Calculates the distance between two clusters.
     * 
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the distance between the clusters.
     */
    private double getClusterDistance(HPSEcalCluster cluster) {
        return Math.hypot(cluster.getSeedHit().getPosition()[0] - originX, cluster.getSeedHit().getPosition()[1]);
    }
    
    /**
     * Gets the value used for the cluster total energy cut.
     * 
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the cut value.
     */
    private double getValueClusterTotalEnergy(HPSEcalCluster cluster) {
    	return cluster.getEnergy();
    }
    
    /**
     * Gets the value used for the cluster hit count cut.
     * 
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the cut value.
     */
    private int getValueClusterHitCount(HPSEcalCluster cluster) {
    	return cluster.getCalorimeterHits().size();
    }
    
    /**
     * Gets the value used for the seed hit energy cut.
     * 
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the cut value.
     */
    private double getValueClusterSeedEnergy(HPSEcalCluster cluster) {
    	return cluster.getSeedHit().getCorrectedEnergy();
    }
    
    /**
     * Calculates the value used by the coplanarity cut.
     * 
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    private double getValueCoplanarity(HPSEcalCluster[] clusterPair) {
    	// Get the cluster angles.
    	double[] clusterAngle = new double[2];
    	for(int i = 0; i < 2; i++) {
            double position[] = clusterPair[i].getSeedHit().getPosition();
            //clusterAngle[i] = Math.toDegrees(Math.atan2(position[1], position[0] - originX));
            //clusterAngle[i] = (clusterAngle[i] + 180.0) % 180.0;
            clusterAngle[i] = (Math.toDegrees(Math.atan2(position[1], position[0] - originX)) + 180.0) % 180.0;
    	}
    	
    	// Calculate the coplanarity cut value.
        return Math.abs(clusterAngle[1] - clusterAngle[0]);
    }
    
    /**
     * Calculates the value used by the energy difference cut.
     * 
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    private double getValueEnergyDifference(HPSEcalCluster[] clusterPair) {
    	return clusterPair[0].getEnergy() - clusterPair[1].getEnergy();
    }
    
    /**
     * Calculates the value used by the energy slope cut.
     * 
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    private double getValueEnergySlope(HPSEcalCluster[] clusterPair) {
    	// E + R*F
    	// Get the low energy cluster energy.
    	double slopeParamE = clusterPair[1].getEnergy();
    	
    	// Get the low energy cluster radial distance.
    	double slopeParamR = getClusterDistance(clusterPair[1]);
    	
    	// Calculate the energy slope.
    	return slopeParamE + slopeParamR * energySlopeParamF;
    }
    
    /**
     * Calculates the value used by the energy sum cut.
     * 
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    private double getValueEnergySum(HPSEcalCluster[] clusterPair) {
    	return clusterPair[0].getEnergy() + clusterPair[1].getEnergy();
    }
    
    /**
     * Checks if a cluster pair is coplanar to the beam within a given
     * angle.
     *
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairCoplanarityCut(HPSEcalCluster[] clusterPair) {
        return (getValueCoplanarity(clusterPair) < coplanarityHigh);
    }
    
    /**
     * Checks if the energy difference between the clusters making up
     * a cluster pair is below an energy difference threshold.
     *
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairEnergyDifferenceCut(HPSEcalCluster[] clusterPair) {
        return (getValueEnergyDifference(clusterPair) < energyDifferenceHigh);
    }
    
    /**
     * Requires that the distance from the beam of the lowest energy
     * cluster in a cluster pair satisfies the following:
     * E_low + d_b*.0032 GeV/mm < [ Threshold ]
     *
     * @param clusterPair : pair of clusters
     * @return true if pair is found, false otherwise
     */
    private boolean pairEnergySlopeCut(HPSEcalCluster[] clusterPair) {
    	return (getValueEnergySlope(clusterPair) > energySlopeLow);
    }
    
    /**
     * Checks if the sum of the energies of clusters making up a cluster
     * pair is below an energy sum threshold.
     *
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairEnergySumCut(HPSEcalCluster[] clusterPair) {
    	// Get the energy sum value.
    	double energySum = getValueEnergySum(clusterPair);
    	
    	// Check that it is within the allowed range.
        return (energySum < energySumHigh) && (energySum > energySumLow);
    }
	
    private void setBackgroundCuts(int backgroundLevel) {
    	// Make sure that the background level is valid.
    	if(backgroundLevel < 1 || backgroundLevel > 10) {
    		throw new RuntimeException(String.format("Trigger cuts are undefined for background level %d.", backgroundLevel));
    	}
    	
    	// Otherwise, set the trigger cuts. Certain cuts are constant
    	// across all background levels.
    	clusterEnergyLow = 0.000;
    	seedEnergyLow = 0.100;
    	
    	// Set the variable values.
    	if(backgroundLevel == 1) {
    		clusterEnergyHigh = 1.700;
    		seedEnergyHigh = 1.300;
    		energySumLow = 0.400;
    		energySumHigh = 2.00;
    		energyDifferenceHigh = 1.500;
    		energySlopeLow = 1.0;
    		coplanarityHigh = 40;
    		minHitCount = 2;
    	} else if(backgroundLevel == 2) {
    		clusterEnergyHigh = 1.600;
    		seedEnergyHigh = 1.200;
    		energySumLow = 0.300;
    		energySumHigh = 2.00;
    		energyDifferenceHigh = 1.400;
    		energySlopeLow = 0.8;
    		coplanarityHigh = 40;
    		minHitCount = 2;
    	} else if(backgroundLevel == 3) {
    		clusterEnergyHigh = 1.600;
    		seedEnergyHigh = 1.200;
    		energySumLow = 0.200;
    		energySumHigh = 2.000;
    		energyDifferenceHigh = 1.400;
    		energySlopeLow = 0.7;
    		coplanarityHigh = 40;
    		minHitCount = 2;
    	} else if(backgroundLevel == 4) {
    		clusterEnergyHigh = 1.500;
    		seedEnergyHigh = 1.200;
    		energySumLow = 0.500;
    		energySumHigh = 1.950;
    		energyDifferenceHigh = 1.400;
    		energySlopeLow = 0.6;
    		coplanarityHigh = 40;
    		minHitCount = 2;
    	} else if(backgroundLevel == 5) {
    		clusterEnergyHigh = 1.500;
    		seedEnergyHigh = 1.200;
    		energySumLow = 0.400;
    		energySumHigh = 2.000;
    		energyDifferenceHigh = 1.400;
    		energySlopeLow = 0.6;
    		coplanarityHigh = 45;
    		minHitCount = 2;
    	} else if(backgroundLevel == 6) {
    		clusterEnergyHigh = 1.500;
    		seedEnergyHigh = 1.200;
    		energySumLow = 0.200;
    		energySumHigh = 1.950;
    		energyDifferenceHigh = 1.400;
    		energySlopeLow = 0.6;
    		coplanarityHigh = 55;
    		minHitCount = 2;
    	} else if(backgroundLevel == 7) {
    		clusterEnergyHigh = 1.700;
    		seedEnergyHigh = 1.200;
    		energySumLow = 0.200;
    		energySumHigh = 2.000;
    		energyDifferenceHigh = 1.500;
    		energySlopeLow = 0.6;
    		coplanarityHigh = 60;
    		minHitCount = 2;
    	} else if(backgroundLevel == 8) {
    		clusterEnergyHigh = 1.700;
    		seedEnergyHigh = 1.300;
    		energySumLow = 0.200;
    		energySumHigh = 2.000;
    		energyDifferenceHigh = 1.500;
    		energySlopeLow = 0.6;
    		coplanarityHigh = 65;
    		minHitCount = 2;
    	} else if(backgroundLevel == 9) {
    		clusterEnergyHigh = 1.500;
    		seedEnergyHigh = 1.200;
    		energySumLow = 0.400;
    		energySumHigh = 1.950;
    		energyDifferenceHigh = 1.400;
    		energySlopeLow = 0.5;
    		coplanarityHigh = 60;
    		minHitCount = 2;
    	} else if(backgroundLevel == 10) {
    		clusterEnergyHigh = 1.500;
    		seedEnergyHigh = 1.200;
    		energySumLow = 0.400;
    		energySumHigh = 2.000;
    		energyDifferenceHigh = 1.400;
    		energySlopeLow = 0.5;
    		coplanarityHigh = 65;
    		minHitCount = 2;
    	}
    }
    
	/**
	 * Tests all of the current cluster pairs for triggers.
	 * 
	 * @return Returns <code>true</code> if one of the cluster pairs
	 * passes all of the cluster cuts and <code>false</code> otherwise.
	 */
    private boolean testTrigger() {
    	// Get the list of cluster pairs.
    	List<HPSEcalCluster[]> clusterPairs = getClusterPairsTopBot();
        
        // Iterate over the cluster pairs and perform each of the cluster
        // pair cuts on them. A cluster pair that passes all of the
        // cuts registers as a trigger.
    	pairLoop:
        for (HPSEcalCluster[] clusterPair : clusterPairs) {
    		// Increment the number of processed cluster pairs.
    		allPairs++;
    		
    		// ==== Pair Energy Sum Cut ====================================
    		// =============================================================
    		// If the cluster fails the cut, skip to the next pair.
    		if(!pairEnergySumCut(clusterPair)) { continue pairLoop; }
    		
    		// Otherwise, note that it passed the cut.
    		pairEnergySumCount++;
        	
    		// ==== Pair Energy Difference Cut =============================
    		// =============================================================
    		// If the cluster fails the cut, skip to the next pair.
    		if(!pairEnergyDifferenceCut(clusterPair)) { continue pairLoop; }
    		
    		// Otherwise, note that it passed the cut.
    		pairEnergyDifferenceCount++;
    		
    		// ==== Pair Energy Slope Cut ==================================
    		// =============================================================
    		// If the cluster fails the cut, skip to the next pair.
    		//if(!energyDistanceCut(clusterPair)) { continue pairLoop; }
    		if(!pairEnergySlopeCut(clusterPair)) { continue pairLoop; }
    		
    		// Otherwise, note that it passed the cut.
    		pairEnergySlopeCount++;
    		
    		// ==== Pair Coplanarity Cut ===================================
    		// =============================================================
    		// If the cluster fails the cut, skip to the next pair.
    		if(!pairCoplanarityCut(clusterPair)) { continue pairLoop; }
    		
    		// Otherwise, note that it passed the cut.
    		pairCoplanarityCount++;
    		
    		// Clusters that pass all of the pair cuts produce a trigger.
    		return true;
        }
        
        // If the loop terminates without producing a trigger, there
    	// are no cluster pairs which meet the trigger conditions.
        return false;
    }
    
    /**
     * Adds clusters from a new event into the top and bottom cluster
     * queues so that they may be formed into pairs.
     * 
     * @param clusterList - The clusters to add to the queues.
     */
    private void updateClusterQueues(List<HPSEcalCluster> clusterList) {
    	// Create lists to store the top and bottom clusters.
        ArrayList<HPSEcalCluster> topClusterList = new ArrayList<HPSEcalCluster>();
        ArrayList<HPSEcalCluster> botClusterList = new ArrayList<HPSEcalCluster>();
        
        // Loop over the clusters in the cluster list.
        for (HPSEcalCluster cluster : clusterList) {
        	// If the cluster is on the top of the calorimeter, it
        	// goes into the top cluster list.
            if (cluster.getSeedHit().getIdentifierFieldValue("iy") > 0) {
                topClusterList.add(cluster);
            }
            
            // Otherwise, it goes into the bottom cluster list.
            else { botClusterList.add(cluster); }
        }
        
        // Add the new cluster lists to the cluster queues.
        topClusterQueue.add(topClusterList);
        botClusterQueue.add(botClusterList);
        
        // Remove the oldest cluster lists from the queues.
        topClusterQueue.remove();
        botClusterQueue.remove();
    }
}