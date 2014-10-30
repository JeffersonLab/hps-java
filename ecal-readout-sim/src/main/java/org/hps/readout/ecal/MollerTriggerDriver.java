package org.hps.readout.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.aida.AIDA;

/**
 * Class <code>MollerTriggerDriver</code> simulates a Møller trigger. It
 * executes four cuts, three of which are single cluster cuts and the
 * last of which is a positional cut. The single cluster cuts are on
 * the seed and total energies of the cluster and the energy of the seed
 * hit of the cluster. The positional cut ignores clusters that occur
 * outside of a high-activity region for Møller events and excludes the
 * primary background high-activity region.
 * <br/><br/>
 * At each event, any extant clusters are examined one at a time. If
 * a cluster passes all four of the cuts, a trigger is produced and
 * the remaining clusters are ignored. If either no clusters are present
 * or no clusters pass the trigger cuts, no trigger is produced.
 * <br/><br/>
 * All thresholds can be set through a steering file. The driver also
 * supports a verbose mode where it will output more details with every
 * event to help with diagnostics.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class MollerTriggerDriver extends TriggerDriver {
    
    // ==================================================================
    // ==== Trigger Algorithms ==========================================
    // ==================================================================    
    
    @Override
    public void endOfData() {
        // Print out the results of the trigger cuts.
        System.out.printf("Trigger Processing Results%n");
        System.out.printf("\tTotal Clusters Processed     :: %d%n", allClusters);
        System.out.printf("\tPassed Positional Cut        :: %d%n", clusterPositionCount);
        System.out.printf("\tPassed Seed Energy Cut       :: %d%n", clusterSeedEnergyCount);
        System.out.printf("\tPassed Total Energy Cut      :: %d%n", clusterTotalEnergyCount);
        System.out.printf("\tPassed Hit Count Cut         :: %d%n", clusterHitCountCount);
        System.out.printf("%n");
        System.out.printf("\tTrigger Count :: %d%n", triggers);
        
        // Run the superclass method.
        super.endOfData();
    }
    
    @Override
    public void process(EventHeader event) {
    	// Run the superclass process event.
    	super.process(event);
    }
    
    @Override
    public void startOfData() {
        // Initialize the cluster hit count diagnostic plots.
        clusterHitCount = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution", 9, 1, 10);
        aClusterHitCount = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution (Passed All Cuts)", 9, 1, 10);
        
        // Initialize the cluster total energy diagnostic plots.
        clusterTotalEnergy = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution", 176, 0.0, 2.2);
        aClusterTotalEnergy = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution (Passed All Cuts)", 176, 0.0, 2.2);
        
        // Initialize the cluster seed energy diagnostic plots.
        clusterSeedEnergy = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution", 176, 0.0, 2.2);
        aClusterSeedEnergy = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution (Passed All Cuts)", 176, 0.0, 2.2);
        
        // Initialize the seed distribution diagnostic plots.
        clusterDistribution = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution", 46, -23, 23, 11, -5.5, 5.5);
        aClusterDistribution = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution (Passed All Cuts)", 46, -23, 23, 11, -5.5, 5.5);
        clusterDistribution100 = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution (Over 100 MeV)", 46, -23, 23, 11, -5.5, 5.5);
        
        // Initialize the seed percentage of cluster energy.
        seedPercent = aida.histogram1D("Analysis Plots :: Seed Percentage of Total Energy", 400, 0.0, 1.0);
        
        // Initialize the momentum angle plot.
        momentumAngle = aida.histogram2D("Trigger Plots :: Particle Momentum Distribution (t = 0)", 500, -0.01, 0.06, 500, -0.04, 0.04);
        aMomentumAngle = aida.histogram2D("Trigger Plots :: Particle Momentum Distribution (t = 0, Passed All Cuts)", 500, -0.01, 0.06, 500, -0.04, 0.04);
        
        // Add the allowed seed crystal positions to the seed set.
		if(useVersionOne) {
	        // Add the allowed seed crystal positions to the seed set.
	        // y = +/- 1, x = -11 -> -15
	        for(int ix = -15; ix <= -11; ix++) {
	            allowedSeedSet.add(new Point(ix, 1));
	            allowedSeedSet.add(new Point(ix, -1));
	        } // y = +/- 2, x = -9 -> -15
	        for(int ix = -15; ix <= -9; ix++) {
	            allowedSeedSet.add(new Point(ix, 2));
	            allowedSeedSet.add(new Point(ix, -2));
	        }
		}
		else {
	        // y = +/- 1, x = -11 -> -13
	        for(int ix = -13; ix <= -11; ix++) {
	            allowedSeedSet.add(new Point(ix, 1));
	            allowedSeedSet.add(new Point(ix, -1));
	        } // y = +/- 2, x = -10 -> -14
	        for(int ix = -14; ix <= -10; ix++) {
	            allowedSeedSet.add(new Point(ix, 2));
	            allowedSeedSet.add(new Point(ix, -2));
	        }
		}
    }
    
    @Override
    protected boolean triggerDecision(EventHeader event) {
        // Check if there is a cluster collection. If not, there is no
        // reason to continue; a trigger can not be produced if there
        // are no clusters.
        if(!event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
            // VERBOSE :: Note that no cluster collection exists for
            //            this event.
            if(verbose) { System.out.println("No cluster collection is present for event."); }
            
            // Indicate that there is no trigger.
            return false;
        }
        
        // VERBOSE :: Note that a cluster collection exists for
        //            this event.
        if(verbose) { System.out.println("Cluster collection is present for event."); }
        
        // Get the cluster list from the event.
        List<HPSEcalCluster> eventList = event.get(HPSEcalCluster.class, clusterCollectionName);
        
        // VERBOSE :: Output the number of extant clusters.
        if(verbose) { System.out.printf("%d clusters in event.%n", eventList.size()); }
        
        // Add the clusters from the event into the cluster list
        // if they pass the minimum total cluster energy and seed
        // energy thresholds.
        for(HPSEcalCluster cluster : eventList) {
            // Increment the clusters processed count.
            allClusters++;
            
            // Plot the seed energy / cluster energy histogram.
            seedPercent.fill(cluster.getSeedHit().getCorrectedEnergy() / cluster.getEnergy(), 1);
            
            // Get the cluster position indices.
            int ix = cluster.getSeedHit().getIdentifierFieldValue("ix");
            int iy = cluster.getSeedHit().getIdentifierFieldValue("iy");
            
            // VERBOSE :: Output the current cluster's properties.
            if(verbose) {
                System.out.printf("\tTesting cluster at (%d, %d) with total energy %f and seed energy %f.%n",
                        ix, iy, cluster.getSeedHit().getCorrectedEnergy(), cluster.getEnergy());
            }
            
            // Add the clusters to the uncut histograms.
            clusterHitCount.fill(cluster.getCalorimeterHits().size());
            clusterTotalEnergy.fill(cluster.getEnergy());
            clusterSeedEnergy.fill(cluster.getSeedHit().getCorrectedEnergy());
            clusterDistribution.fill(ix > 0 ? ix - 1 : ix, iy, 1);
            if(cluster.getSeedHit().getCorrectedEnergy() > 0.100) { clusterDistribution100.fill(ix > 0 ? ix - 1 : ix, iy, 1); }
            
            // VERBOSE :: Output the single cluster trigger thresholds.
            if(verbose) {
                System.out.printf("\tCluster seed energy threshold  :: [%f, %f]%n", clusterSeedEnergyThresholdLow, clusterSeedEnergyThresholdHigh);
                System.out.printf("\tCluster total energy threshold :: %f%n%n", clusterTotalEnergyThresholdLow);
            }
            
            // Perform the single cluster cuts.
            boolean totalEnergyCut = clusterTotalEnergyCut(cluster);
            boolean seedEnergyCut = clusterSeedEnergyCut(cluster);
            boolean hitCountCut = clusterHitCountCut(cluster);
            boolean positionCut = clusterPositionCut(cluster);
            
            // Increment the single cut counts.
            if(positionCut) {
                clusterPositionCount++;
                if(seedEnergyCut) {
                    clusterSeedEnergyCount++;
                    if(totalEnergyCut) {
                        clusterTotalEnergyCount++;
                        if(hitCountCut) {
                            clusterHitCountCount++;
                        }
                    }
                }
            }
            
            // VERBOSE :: Note whether the cluster passed the single
            //            cluster cuts.
            if(verbose) {
                System.out.printf("\tPassed seed energy cut    :: %b%n", seedEnergyCut);
                System.out.printf("\tPassed cluster energy cut :: %b%n", totalEnergyCut);
                System.out.printf("\tPassed hit count cut      :: %b%n", hitCountCut);
                System.out.printf("\tWithin allowed region     :: %b%n%n", positionCut);
            }
            
            // Require that the cluster pass each of the cuts in
            // order to qualify for a trigger.
            if(totalEnergyCut && seedEnergyCut && hitCountCut && positionCut) {
            	// Increment the number of events that have passed
            	// the cuts.
            	passedEvents++;
            	
            	// If the number of passed events exceeds the prescaling
            	// threshold, throw a trigger.
            	if(passedEvents >= prescale) {
            		// Reset the number of passed events.
            		passedEvents = 0;
            		
	                // Add the clusters to the cut histograms.
	                aClusterHitCount.fill(cluster.getCalorimeterHits().size());
	                aClusterTotalEnergy.fill(cluster.getEnergy());
	                aClusterSeedEnergy.fill(cluster.getSeedHit().getCorrectedEnergy());
	                aClusterDistribution.fill(ix > 0 ? ix - 1 : ix, iy, 1);
	                
	                // Increment the trigger count.
	                triggers++;
	                
	                // VERBOSE :: Indicate that a trigger occurred.
	                if(verbose) { System.out.printf("\tTriggered!%n%n"); }
	                
	                // Return a trigger.
	                return true;
            	}
            }
        }
        
        // VERBOSE :: Note that the event has failed to trigger.
        if(verbose) { System.out.println("No trigger.\n\n"); }
        
        // If one or more of the pair cuts failed, the we do not trigger.
        return false;
    }
    
    // ==================================================================
    // ==== Trigger Cut Methods =========================================
    // ==================================================================
    
    /**
     * Checks whether the cluster passes the threshold for minimum
     * number of component hits.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes and <code>
     * false</code> if it does not.
     */
    private boolean clusterHitCountCut(HPSEcalCluster cluster) {
        return cluster.getCalorimeterHits().size() >= clusterHitCountThreshold;
    }
    
    /**
     * Checks whether the cluster falls within the allowed range for
     * the seed hit energy cut.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes and <code>
     * false</code> if it does not.
     */
    private boolean clusterSeedEnergyCut(HPSEcalCluster cluster) {
        // Get the seed energy value.
        double seedEnergy = cluster.getSeedHit().getCorrectedEnergy();
        
        // Perform the seed energy cut.
        return seedEnergy >= clusterSeedEnergyThresholdLow && seedEnergy <= clusterSeedEnergyThresholdHigh;
    }
    
    /**
     * Checks whether the cluster passes the threshold for minimum
     * total cluster energy.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes and <code>
     * false</code> if it does not.
     */
    private boolean clusterTotalEnergyCut(HPSEcalCluster cluster) {
        // Get the cluster energy.
        double clusterEnergy = cluster.getEnergy();
        
        // Perform the cut.
        return clusterEnergy >= clusterTotalEnergyThresholdLow && clusterEnergy <= clusterTotalEnergyThresholdHigh;
    }
    
    /**
     * Checks whether the cluster falls into the accepted region on
     * the calorimeter face.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes and <code>
     * false</code> if it does not.
     */
    private boolean clusterPositionCut(HPSEcalCluster cluster) {
        // Get the cluster position.
        Point seedLoc =  new Point(cluster.getSeedHit().getIdentifierFieldValue("ix"),
                cluster.getSeedHit().getIdentifierFieldValue("iy"));
        
        // Check if it is one of the allowed seed crystals.
        return allowedSeedSet.contains(seedLoc);
    }
    
    // ==================================================================
    // ==== Variables Mutator Methods ===================================
    // ==================================================================
    
    /**
     * Sets the LCIO collection name where <code>HPSEcalCluster</code>
     * objects are stored for use in the trigger.
     * @param clusterCollectionName - The name of the LCIO collection.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * Sets the minimum number of hits required for a cluster to be
     * used in triggering.
     * @param clusterHitCountThreshold - The smallest number of hits
     * in a cluster.
     */
    public void setClusterHitCountThreshold(int clusterHitCountThreshold) {
        this.clusterHitCountThreshold = clusterHitCountThreshold;
    }
    
    /**
     * Sets the threshold for the cluster seed energy of individual
     * clusters above which the cluster will be rejected and not used
     * for triggering.
     * @param clusterSeedEnergyThresholdHigh - The cluster seed energy
     * lower bound.
     */
    public void setClusterSeedEnergyThresholdHigh(double clusterSeedEnergyThresholdHigh) {
        this.clusterSeedEnergyThresholdHigh = clusterSeedEnergyThresholdHigh;
    }
    
    /**
     * Sets the threshold for the cluster seed energy of individual
     * clusters under which the cluster will be rejected and not used
     * for triggering.
     * @param clusterSeedEnergyThresholdLow - The cluster seed energy
     * lower bound.
     */
    public void setClusterSeedEnergyThresholdLow(double clusterSeedEnergyThresholdLow) {
        this.clusterSeedEnergyThresholdLow = clusterSeedEnergyThresholdLow;
    }
    
    /**
     * Sets the threshold for the total cluster energy of individual
     * clusters under which the cluster will be rejected and not used
     * for triggering.
     * @param clusterTotalEnergyThresholdLow - The cluster total energy
     * lower bound.
     */
    public void setClusterTotalEnergyThresholdLow(double clusterTotalEnergyThresholdLow) {
        this.clusterTotalEnergyThresholdLow = clusterTotalEnergyThresholdLow;
    }
    
    /**
     * Sets the threshold for the total cluster energy of individual
     * clusters above which the cluster will be rejected and not used
     * for triggering.
     * @param clusterTotalEnergyThresholdHigh - The cluster total energy
     * upper bound.
     */
    public void setClusterTotalEnergyThresholdHigh(double clusterTotalEnergyThresholdHigh) {
        this.clusterTotalEnergyThresholdHigh = clusterTotalEnergyThresholdHigh;
    }
    
    /**
     * Sets the number of events that must pass the trigger before a
     * proper trigger flag is thrown.
     * @param prescale - The number of passing events before a trigger
     * will be thrown.
     */
    public void setPrescale(int prescale) {
    	this.prescale = prescale;
    }
    
    /**
     * Toggles whether the driver will output its actions to the console
     * during run time or not.
     * @param verbose - <code>true</code> indicates that the console
     * will write its actions and <code>false</code> that it will not.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
	
    /**
     * Toggles whether the more inclusive acceptance region version 1
     * is used, or the slightly smaller and more exclusive acceptance
     * region version 2.
     * @param useVersionOne - <code>true</code> indicates that version
     * 1 of the acceptance region should be used and <code>false</code>
     * that version 2 should be used.
     */
	public void setUseVersionOne(boolean useVersionOne) {
		this.useVersionOne = useVersionOne;
	}
    
    // ==================================================================
    // ==== AIDA Plots ==================================================
    // ==================================================================
    IHistogram2D aClusterDistribution;
    IHistogram1D aClusterHitCount;
    IHistogram1D aClusterSeedEnergy;
    IHistogram1D aClusterTotalEnergy;
    IHistogram2D clusterDistribution;
    IHistogram1D clusterHitCount;
    IHistogram1D clusterSeedEnergy;
    IHistogram1D clusterTotalEnergy;
    IHistogram1D seedPercent;
    IHistogram2D momentumAngle;
    IHistogram2D aMomentumAngle;
    IHistogram2D clusterDistribution100;
    
    // ==================================================================
    // ==== Variables ===================================================
    // ==================================================================
    
    /**
     * Factory for generating histograms.
     */
    private AIDA aida = AIDA.defaultInstance();
    
    /**
     * Contains all allowed seed crystal indices. Seeds outside of this
     * set will be rejected and not produce a trigger.
     */
    private Set<Point> allowedSeedSet = new HashSet<Point>();
    
    /**
     * The name of the LCIO collection containing <code>HPSEcalCluster
     * </code> objects.
     */
    private String clusterCollectionName = "EcalClusters";
    
    /**
     * Defines the minimum number of hits required for a cluster to
     * be used in triggering.
     */
    private int clusterHitCountThreshold = 5;
    
    /**
     * Defines the threshold for the cluster seed energy under which
     * a cluster will be rejected.
     */
    private double clusterSeedEnergyThresholdLow = 0.300;
    
    /**
     * Defines the threshold for the cluster seed energy above which
     * a cluster will be rejected.
     */
    private double clusterSeedEnergyThresholdHigh = 0.700;
    
    /**
     * Defines the threshold for the total cluster energy under which
     * a cluster will be rejected.
     */
    private double clusterTotalEnergyThresholdLow = 0.600;
    
    /**
     * Defines the threshold for the total cluster energy above which
     * a cluster will be rejected.
     */
    private double clusterTotalEnergyThresholdHigh = 0.775;
    
    /**
     * The number of events that have passed the trigger cuts. This is
     * used to determine when a trigger should be thrown for prescaling.
     */
    private int passedEvents = 0;
    
    /**
     * Indicates how many events must pass the trigger cuts before a
     * trigger is thrown.
     */
    private int prescale = 100;
    
    /**
     * Sets whether the driver outputs its clustering decisions to the
     * console or not.
     */
    private boolean verbose = false;
    
    /**
     * Indicates whether trigger region version 1 or version 2 should
     * be used.
     */
    private boolean useVersionOne = false;
    
    private int triggers = 0;                                      // Track the number of triggers.
    private int allClusters = 0;                                   // Track the number of clusters processed.
    private int clusterSeedEnergyCount = 0;                        // Track the clusters which pass the seed energy cut.
    private int clusterHitCountCount = 0;                          // Track the clusters which pass the hit count cut.
    private int clusterTotalEnergyCount = 0;                       // Track the clusters which pass the total energy cut.
    private int clusterPositionCount = 0;                          // Track the clusters which pass the positional cut.
}