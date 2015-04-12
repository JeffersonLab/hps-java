package org.hps.readout.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.hps.recon.ecal.EcalUtils;
import org.hps.recon.ecal.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.aida.AIDA;

/**
 * Class <code>FADCPrimaryTriggerDriver</code> reads reconstructed
 * clusters and makes trigger decisions on them. It is designed to
 * trigger off 2.2 GeV beam A' events. Cuts can either be set manually
 * in a steering file or automatically by specifying a background level.
 * The code for generating trigger pairs and handling the coincidence
 * window comes from <code>FADCTriggerDriver</code>.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @see FADCTriggerDriver
 */
public class FADCPrimaryTriggerDriver extends TriggerDriver {
    // ==================================================================
    // ==== Trigger General Default Parameters ==========================
    // ==================================================================
    private String clusterCollectionName = "EcalClusters";        // Name for the LCIO cluster collection.
    private int pairCoincidence = 2;                              // Maximum allowed time difference between clusters. (4 ns clock-cycles)
    private int backgroundLevel = -1;                             // Automatically sets the cuts to achieve a predetermined background rate.
    private TriggerModule triggerModule = new TriggerModule(1.0, 0.050,
    		6.600, 0.010, 6.600, 0.000, 13.200, 6.600, 0.0, 360, 0.0055);
    
    // ==================================================================
    // ==== Driver Internal Variables ===================================
    // ==================================================================
    private Queue<List<Cluster>> topClusterQueue = null;           // Store clusters on the top half of the calorimeter.
    private Queue<List<Cluster>> botClusterQueue = null;           // Store clusters on the bottom half of the calorimeter.
    private int allClusters = 0;                                   // Track the number of clusters processed.
    private int allPairs = 0;                                      // Track the number of cluster pairs processed.
    private int clusterTotalEnergyCount = 0;                       // Track the clusters which pass the total energy cut.
    private int clusterSeedEnergyCount = 0;                        // Track the clusters which pass the seed energy cut.
    private int clusterHitCountCount = 0;                          // Track the clusters which pass the hit count cut.
    private int pairEnergySumCount = 0;                            // Track the pairs which pass the energy sum cut.
    private int pairEnergyDifferenceCount = 0;                     // Track the pairs which pass the energy difference cut.
    private int pairEnergySlopeCount = 0;                          // Track the pairs which pass the energy slope cut.
    private int pairCoplanarityCount = 0;                          // Track the pairs which pass the coplanarity cut.
    private boolean verbose = false;                               // Stores whether debug text should be output.
    
    // ==================================================================
    // ==== Trigger Distribution Plots ==================================
    // ==================================================================
    private static final int NO_CUTS       = 0;
    private static final int ALL_CUTS      = 1;
    private static final int OVER_1HIT     = 2;
    private static final int OVER_2HIT     = 3;
    private static final int SINGLES_CUTS  = 4;
    private static final int PLOT_COUNT    = 5;
    private AIDA aida = AIDA.defaultInstance();
    private IHistogram1D[] clusterSeedEnergy;
    //private IHistogram1D clusterSeedEnergy = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution", 176, 0.0, 2.2);
    //private IHistogram1D clusterSeedEnergySingle = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution (Passed Single Cuts)", 176, 0.0, 2.2);
    //private IHistogram1D clusterSeedEnergyAll = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution (Passed All Cuts)", 176, 0.0, 2.2);
    private IHistogram1D[] clusterHitCount;
    //private IHistogram1D clusterHitCount = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution", 9, 1, 10);
    //private IHistogram1D clusterHitCountSingle = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution (Passed Single Cuts)", 9, 1, 10);
    //private IHistogram1D clusterHitCountAll = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution (Passed All Cuts)", 9, 1, 10);
    private IHistogram1D[] clusterTotalEnergy;
    //private IHistogram1D clusterTotalEnergy = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution", 176, 0.0, 2.2);
    //private IHistogram1D clusterTotalEnergySingle = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution (Passed Single Cuts)", 176, 0.0, 2.2);
    //private IHistogram1D clusterTotalEnergyAll = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution (Passed All Cuts)", 176, 0.0, 2.2);
    
    private IHistogram1D[] pairEnergySum;
    //private IHistogram1D pairEnergySum = aida.histogram1D("Trigger Plots :: Pair Energy Sum Distribution", 176, 0.0, 4.4);
    //private IHistogram1D pairEnergySumAll = aida.histogram1D("Trigger Plots :: Pair Energy Sum Distribution (Passed All Cuts)", 176, 0.0, 4.4); 
    private IHistogram1D[] pairEnergyDifference;
    //private IHistogram1D pairEnergyDifference = aida.histogram1D("Trigger Plots :: Pair Energy Difference Distribution", 176, 0.0, 2.2);
    //private IHistogram1D pairEnergyDifferenceAll = aida.histogram1D("Trigger Plots :: Pair Energy Difference Distribution (Passed All Cuts)", 176, 0.0, 2.2);
    private IHistogram1D[] pairCoplanarity;
    //private IHistogram1D pairCoplanarity = aida.histogram1D("Trigger Plots :: Pair Coplanarity Distribution", 180, 0.0, 180.0);
    //private IHistogram1D pairCoplanarityAll = aida.histogram1D("Trigger Plots :: Pair Coplanarity Distribution (Passed All Cuts)", 180, 0.0, 180.0);
    private IHistogram1D[] pairEnergySlope;
    //private IHistogram1D pairEnergySlope = aida.histogram1D("Trigger Plots :: Pair Energy Slope Distribution", 100, 0.0, 4.0);
    //private IHistogram1D pairEnergySlopeAll = aida.histogram1D("Trigger Plots :: Pair Energy Slope Distribution (Passed All Cuts)", 100, 0.0, 4.0);
    
    private IHistogram2D[] clusterDistribution;
    //private IHistogram2D clusterDistribution = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution", 46, -23, 23, 11, -5.5, 5.5);
    //private IHistogram2D clusterDistributionSingle = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution (Passed Single Cuts)", 46, -23, 23, 11, -5.5, 5.5);
    //private IHistogram2D clusterDistributionAll = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution (Passed All Cuts)", 46, -23, 23, 11, -5.5, 5.5);
    
    /**
     * Initializes the cluster pair queues and other variables.
     */
    @Override
    public void startOfData() {
    	// Define plot type names.
    	String[] plotType = new String[PLOT_COUNT];
    	plotType[NO_CUTS] = "";
    	plotType[ALL_CUTS] = " (Passed Single Cuts)";
    	plotType[OVER_1HIT] = " (More than 1 Hit)";
    	plotType[OVER_2HIT] = " (More than 2 Hits)";
    	plotType[SINGLES_CUTS] = " (Passed Single Cuts)";
    	
    	// Define plot type directories.
    	String[] plotDir = new String[PLOT_COUNT];
    	plotDir[NO_CUTS] = "NoCuts/";
    	plotDir[ALL_CUTS] = "PassedAll/";
    	plotDir[OVER_1HIT] = "2PlusHits/";
    	plotDir[OVER_2HIT] = "3PlusHits";
    	plotDir[SINGLES_CUTS] = "PassedSingles/";
    	
    	// Instantiate the singles plot arrays.
    	clusterSeedEnergy = new IHistogram1D[PLOT_COUNT];
    	clusterHitCount = new IHistogram1D[PLOT_COUNT];
    	clusterTotalEnergy = new IHistogram1D[PLOT_COUNT];
    	clusterDistribution = new IHistogram2D[PLOT_COUNT];
    	
    	// Instantiate the pair plot arrays. Note that the pair cuts
    	// only ever see clusters that pass the singles cuts, so the
    	// "passed singles cuts" plots are meaningless. Thusly, the
    	// pair plots have one fewer plot than the singles.
    	pairEnergySum = new IHistogram1D[PLOT_COUNT - 1];
    	pairEnergyDifference = new IHistogram1D[PLOT_COUNT - 1];
    	pairCoplanarity = new IHistogram1D[PLOT_COUNT - 1];
    	pairEnergySlope = new IHistogram1D[PLOT_COUNT - 1];
    	
    	// Instantiate the plots.
    	for(int i = 0; i < PLOT_COUNT; i++) {
    		System.out.println(plotDir[i] + "Cluster Seed Energy" + plotType[i]);
    		clusterSeedEnergy[i] = aida.histogram1D(plotDir[i] + "Cluster Seed Energy" + plotType[i], 176, 0.0, 2.2);
    		clusterHitCount[i] = aida.histogram1D(plotDir[i] + "Cluster Hit Count Distribution" + plotType[i], 9, 0.5, 9.5);
    		clusterTotalEnergy[i] = aida.histogram1D(plotDir[i] + "Cluster Total Energy Distribution" + plotType[i], 176, 0.0, 2.2);
    		clusterDistribution[i] = aida.histogram2D(plotDir[i] + "Cluster Seed Distribution" + plotType[i], 46, -23, 23, 11, -5.5, 5.5);
    		
    		if(i != PLOT_COUNT - 1) {
    			pairEnergySum[i] = aida.histogram1D(plotDir[i] + "Pair Energy Sum Distribution" + plotType[i], 176, 0.0, 4.4);
    			pairEnergyDifference[i] = aida.histogram1D(plotDir[i] + "Pair Energy Difference Distribution" + plotType[i], 176, 0.0, 2.2);
    			pairCoplanarity[i] = aida.histogram1D(plotDir[i] + "Pair Coplanarity Distribution" + plotType[i], 180, 0.0, 180.0);
    			pairEnergySlope[i] = aida.histogram1D(plotDir[i] + "Pair Energy Slope Distribution" + plotType[i], 100, 0.0, 4.0);
    		}
    	}
    	
        // Make sure that a valid cluster collection name has been
        // defined. If it has not, throw an exception.
        if (clusterCollectionName == null) {
            throw new RuntimeException("The parameter clusterCollectionName was not set!");
        }
        
        // Initialize the top and bottom cluster queues.
        topClusterQueue = new LinkedList<List<Cluster>>();
        botClusterQueue = new LinkedList<List<Cluster>>();
        
        // Populate the top cluster queue. It should be populated with
        // a number of empty lists equal to (2 * pairCoincidence + 1).
        for (int i = 0; i < 2 * pairCoincidence + 1; i++) {
            topClusterQueue.add(new ArrayList<Cluster>());
        }
        
        // Populate the bottom cluster queue. It should be populated with
        // a number of empty lists equal to (2 * pairCoincidence + 1).
        for (int i = 0; i < pairCoincidence + 1; i++) {
            botClusterQueue.add(new ArrayList<Cluster>());
        }
        
        // If a background level has been set, pick the correct cuts.
        if(backgroundLevel != -1) { setBackgroundCuts(backgroundLevel); }
        
        // Run the superclass method.
        super.startOfData();
    }
    
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
        
        // Print the trigger cuts.
        System.out.printf("%nTrigger Module Cut Values:%n");
        System.out.printf("\tSeed Energy Low        :: %.3f%n", triggerModule.getCutValue(TriggerModule.CLUSTER_SEED_ENERGY_LOW));
        System.out.printf("\tSeed Energy High       :: %.3f%n", triggerModule.getCutValue(TriggerModule.CLUSTER_SEED_ENERGY_HIGH));
        System.out.printf("\tCluster Energy Low     :: %.3f%n", triggerModule.getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW));
        System.out.printf("\tCluster Energy High    :: %.3f%n", triggerModule.getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH));
        System.out.printf("\tCluster Hit Count      :: %.0f%n", triggerModule.getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW));
        System.out.printf("\tPair Energy Sum Low    :: %.3f%n", triggerModule.getCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW));
        System.out.printf("\tPair Energy Sum High   :: %.3f%n", triggerModule.getCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH));
        System.out.printf("\tPair Energy Difference :: %.3f%n", triggerModule.getCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH));
        System.out.printf("\tPair Energy Slope      :: %.1f%n", triggerModule.getCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW));
        System.out.printf("\tPair Coplanarity       :: %.1f%n", triggerModule.getCutValue(TriggerModule.PAIR_COPLANARITY_HIGH));
        
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
        if (event.hasCollection(Cluster.class, clusterCollectionName)) {
            // Get the collection of clusters.
            List<Cluster> clusterList = event.get(Cluster.class, clusterCollectionName);
            
            // Create a list to hold clusters which pass the single
            // cluster cuts.
            List<Cluster> goodClusterList = new ArrayList<Cluster>(clusterList.size());
            
            // Sort through the cluster list and add clusters that pass
            // the single cluster cuts to the good list.
            clusterLoop:
            for(Cluster cluster : clusterList) {
                // Increment the number of processed clusters.
                allClusters++;
                
                // Get the cluster plot values.
                int hitCount = cluster.getCalorimeterHits().size();
                double seedEnergy = cluster.getCalorimeterHits().get(0).getCorrectedEnergy();
                double clusterEnergy = cluster.getEnergy();
                int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
                
                // VERBOSE :: Note that a cluster is being processed.
                if(verbose) {
                    System.out.printf("%nProcessing cluster at (% 2d, % 2d)%n", ix, iy);
                }
                
                // Correct for "hole" on the x-axis for plotting.
                if(ix > 0) { ix = ix - 1; }
                
                // Fill the general plots.
                clusterSeedEnergy[NO_CUTS].fill(seedEnergy);
                clusterTotalEnergy[NO_CUTS].fill(clusterEnergy);
                clusterHitCount[NO_CUTS].fill(hitCount);
                clusterDistribution[NO_CUTS].fill(ix, iy);
                //clusterSeedEnergy.fill(seedEnergy, 1);
                //clusterTotalEnergy.fill(clusterEnergy, 1);
                //clusterHitCount.fill(hitCount, 1);
                //clusterDistribution.fill(ix, iy, 1);
                
                // Fill the hit count plots for N > 1.
                if(hitCount > 1) {
                	// Populate the plots.
                    clusterSeedEnergy[OVER_1HIT].fill(seedEnergy);
                    clusterTotalEnergy[OVER_1HIT].fill(clusterEnergy);
                    clusterHitCount[OVER_1HIT].fill(hitCount);
                    clusterDistribution[OVER_1HIT].fill(ix, iy);
                	
                    // Fill the hit count plots for N > 2.
                    if(hitCount > 2) {
                    	// Populate the plots.
                        clusterSeedEnergy[OVER_2HIT].fill(seedEnergy);
                        clusterTotalEnergy[OVER_2HIT].fill(clusterEnergy);
                        clusterHitCount[OVER_2HIT].fill(hitCount);
                        clusterDistribution[OVER_2HIT].fill(ix, iy);
                    }
                }
                
                // ==== Seed Hit Energy Cut ====================================
                // =============================================================
                // VERBOSE :: Print the seed energy comparison check.
                if(verbose) {
                    System.out.printf("\tSeed Energy Cut    :: %.3f < %.3f < %.3f --> %b%n",
                    		triggerModule.getCutValue(TriggerModule.CLUSTER_SEED_ENERGY_LOW), seedEnergy,
                    		triggerModule.getCutValue(TriggerModule.CLUSTER_SEED_ENERGY_HIGH),
                    		triggerModule.clusterSeedEnergyCut(cluster));
                }
                
                // If the cluster fails the cut, skip to the next cluster.
                if(!triggerModule.clusterSeedEnergyCut(cluster)) {
                	continue clusterLoop;
                }
                
                // Otherwise, note that it passed the cut.
                clusterSeedEnergyCount++;
                
                // ==== Cluster Hit Count Cut ==================================
                // =============================================================
                // VERBOSE :: Print the hit count comparison check.
                if(verbose) {
                    System.out.printf("\tHit Count Cut      :: %d >= %.0f --> %b%n",
                    		hitCount, triggerModule.getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW),
                    		triggerModule.clusterHitCountCut(cluster));
                }
                
                // If the cluster fails the cut, skip to the next cluster.
                if(!triggerModule.clusterHitCountCut(cluster)) {
                	continue clusterLoop;
                }
                
                // Otherwise, note that it passed the cut.
                clusterHitCountCount++;
                
                // ==== Cluster Total Energy Cut ===============================
                // =============================================================
                // VERBOSE :: Print the cluster energy comparison check.
                if(verbose) {
                    System.out.printf("\tCluster Energy Cut :: %.3f < %.3f < %.3f --> %b%n",
                    		triggerModule.getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW), clusterEnergy,
                    		triggerModule.getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH),
                    		triggerModule.clusterTotalEnergyCut(cluster));
                }
                
                // If the cluster fails the cut, skip to the next cluster.
                if(!triggerModule.clusterTotalEnergyCut(cluster)) {
                	continue clusterLoop;
                }
                
                // Otherwise, note that it passed the cut.
                clusterTotalEnergyCount++;
                
                // Fill the "passed single cuts" plots.
                clusterSeedEnergy[SINGLES_CUTS].fill(seedEnergy);
                clusterTotalEnergy[SINGLES_CUTS].fill(clusterEnergy);
                clusterHitCount[SINGLES_CUTS].fill(hitCount);
                clusterDistribution[SINGLES_CUTS].fill(ix, iy, 1);
                //clusterSeedEnergySingle.fill(seedEnergy, 1);
                //clusterTotalEnergySingle.fill(clusterEnergy, 1);
                //clusterHitCountSingle.fill(hitCount, 1);
                //clusterDistributionSingle.fill(ix, iy, 1);
                
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
     * @param backgroundLevel - The level to which the background should
     * be set. Actual background rates equal about (5 * backgroundLevel) kHz.
     */
    public void setBackgroundLevel(int backgroundLevel) {
        this.backgroundLevel = backgroundLevel;
    }
    
    /**
     * Sets the name of the LCIO collection that contains the clusters.
     * @param clusterCollectionName - The cluster LCIO collection name.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * Sets the highest allowed energy a cluster may have and still
     * pass the cluster total energy single cluster cut. Value uses
     * units of GeV.
     * @param clusterEnergyHigh - The parameter value.
     */
    public void setClusterEnergyHigh(double clusterEnergyHigh) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, clusterEnergyHigh * EcalUtils.GeV);
    }
    
    /**
     * Sets the lowest allowed energy a cluster may have and still
     * pass the cluster total energy single cluster cut. Value uses
     * units of GeV.
     * @param clusterEnergyLow - The parameter value.
     */
    public void setClusterEnergyLow(double clusterEnergyLow) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, clusterEnergyLow * EcalUtils.GeV);
    }
    
    /**
     * Sets the maximum deviation from coplanarity that a cluster pair
     * may possess and still pass the coplanarity pair cut. Value uses
     * units of degrees.
     * @param maxCoplanarityAngle - The parameter value.
     */
    public void setCoplanarityHigh(double coplanarityHigh) {
        triggerModule.setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, coplanarityHigh);
    }
    
    /**
     * Sets the highest allowed energy difference a cluster pair may
     * have and still pass the cluster pair energy difference cut.
     * Value uses units of GeV.
     * @param energyDifferenceHigh - The parameter value.
     */
    public void setEnergyDifferenceHigh(double energyDifferenceHigh) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, energyDifferenceHigh * EcalUtils.GeV);
    }
    
    /**
     * Sets the lowest allowed energy slope a cluster pair may
     * have and still pass the cluster pair energy slope cut.
     * @param energySlopeLow - The parameter value.
     */
    public void setEnergySlopeLow(double energySlopeLow) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, energySlopeLow);
    }
    
    /**
     * Sets the highest allowed energy a cluster pair may have and
     * still pass the cluster pair energy sum cluster cut. Value uses
     * units of GeV.
     * @param energySumHigh - The parameter value.
     */
    public void setEnergySumHigh(double energySumHigh) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, energySumHigh * EcalUtils.GeV);
    }
    
    /**
     * Sets the lowest allowed energy a cluster pair may have and
     * still pass the cluster pair energy sum cluster cut. Value uses
     * units of GeV.
     * @param energySumHigh - The parameter value.
     */
    public void setEnergySumLow(double energySumLow) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, energySumLow * EcalUtils.GeV);
    }
    
    /**
     * Sets the minimum number of hits needed for a cluster to pass
     * the hit count single cluster cut.
     * @param minHitCount - The parameter value.
     */
    public void setMinHitCount(int minHitCount) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, minHitCount);
    }
    
    /**
     * Sets the time range over which cluster pairs will be formed.
     * Value uses units of clock-cycles. Note that the number of
     * clock-cycles used is calculated as (2 * pairCoincidence) + 1.
     * @param pairCoincidence - The parameter value.
     */
    public void setPairCoincidence(int pairCoincidence) {
        this.pairCoincidence = pairCoincidence;
    }
    
    /**
     * Sets the highest allowed energy a seed hit may have and still
     * pass the seed hit energy single cluster cut. Value uses units
     * of GeV.
     * @param seedEnergyHigh - The parameter value.
     */
    public void setSeedEnergyHigh(double seedEnergyHigh) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_HIGH, seedEnergyHigh * EcalUtils.GeV);
    }
    
    /**
     * Sets the lowest allowed energy a seed hit may have and still
     * pass the seed hit energy single cluster cut. Value uses units
     * of GeV.
     * @param seedEnergyLow - The parameter value.
     */
    public void setSeedEnergyLow(double seedEnergyLow) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_LOW, seedEnergyLow * EcalUtils.GeV);
    }
    
    /**
     * Sets the value of F in the energy slope equation <code>E_min +
     * R_min * F</code>.
     * @param f - The new energy slope parameter.
     */
    public void setEnergySlopeParamF(double f) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, f);
    }

    /**
     * Print debug text.
     * @param verbose Defaults to false (no debug).
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Get a list of all unique cluster pairs in the event.
     * @return A <code>List</code> collection of <code>Cluster</code> 
     * objects containing all cluster pairs.
     */
    protected List<Cluster[]> getClusterPairsTopBot() {
        // Create a list to store cluster pairs. 
        List<Cluster[]> clusterPairs = new ArrayList<Cluster[]>();
        
        // Loop over all top-bottom pairs of clusters; higher-energy cluster goes first in the pair
        // To apply pair coincidence time, use only bottom clusters from the 
        // readout cycle pairCoincidence readout cycles ago, and top clusters 
        // from all 2*pairCoincidence+1 previous readout cycles
        for (Cluster botCluster : botClusterQueue.element()) {
            for (List<Cluster> topClusters : topClusterQueue) {
                for (Cluster topCluster : topClusters) {
                    // The first cluster in a pair should always be
                    // the higher energy cluster. If the top cluster
                    // is higher energy, it goes first.
                    if (topCluster.getEnergy() > botCluster.getEnergy()) {
                        Cluster[] clusterPair = {topCluster, botCluster};
                        clusterPairs.add(clusterPair);
                    }
                    
                    // Otherwise, the bottom cluster goes first.
                    else {
                        Cluster[] clusterPair = {botCluster, topCluster};
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
     * @return Returns <code>true</code> if the event produces a trigger
     * and <code>false</code> if it does not.
     */
    @Override
    protected boolean triggerDecision(EventHeader event) {
        // If there is a list of clusters present for this event,
        // check whether it passes the trigger conditions.
        if (event.hasCollection(Cluster.class, clusterCollectionName)) {
            return testTrigger();
        }
        
        // Otherwise, this event can not produce a trigger and should
        // return false automatically.
        else { return false; }
    }
    
    /**
     * Sets the cuts to accept a certain background level. Levels are
     * defined with an integer between 1 and 10, where each step is
     * indicative of a hypothetical background rate of approximately
     * 5 kHz. Thusly, <code>backgroundLevel = 1</code> represents a
     * hypothetical background rate of approximately 5 kHz, <code>
     * backgroundLevel = 2</code> represents 10 kHz, and so on. Using
     * a value of 0 will set the cuts to the original test run values.
     * @param backgroundLevel - The hypothetical background rate value.
     * This must be between 1 and 10, or zero for the original test
     * run cut values.
     */
    private void setBackgroundCuts(int backgroundLevel) {
        // Make sure that the background level is valid.
        if(backgroundLevel < -1 || backgroundLevel > 10) {
            throw new RuntimeException(String.format("Trigger cuts are undefined for background level %d.", backgroundLevel));
        }
        
        // Some cut values are almost always the same thing. Set those
        // here and only overwrite if necessary.
    	triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_LOW,     0.125);
    	triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_HIGH,    1.300);
    	triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW,    0.200);
    	triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH,   1.700);
    	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW,         0.500);
    	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH,        2.000);
    	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 1.200);
    	triggerModule.setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH,       30);
    	triggerModule.setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW,       2);
        
        // Set the variable values.
        if(backgroundLevel == 1) {
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW,         1.000);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       1.2);
        	triggerModule.setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH,       20);
        } else if(backgroundLevel == 2) {
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       1.0);
        	triggerModule.setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH,       20);
        } else if(backgroundLevel == 3) {
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       1.0);
        } else if(backgroundLevel == 4) {
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.8);
        } else if(backgroundLevel == 5) {
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.8);
        } else if(backgroundLevel == 6) {
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.6);
        } else if(backgroundLevel == 7) {
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.6);
        } else if(backgroundLevel == 8) {
        	triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH,   1.500);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.4);
        } else if(backgroundLevel == 9) {
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.4);
        } else if(backgroundLevel == 10) {
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.4);
        } else if(backgroundLevel == 0) {
        	triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_LOW,     0.100);
        	triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_HIGH,    6.600);
        	triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW,    0.100);
        	triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH,   1.500);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW,         0.000);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH,        1.900);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 2.200);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       1.1);
        	triggerModule.setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH,       35);
        	triggerModule.setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW,       1);
        } else if(backgroundLevel == -1) {
        	triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_LOW,     0.050);
        	triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_HIGH,    6.600);
        	triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW,    0.010);
        	triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH,   6.600);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW,         0.000);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH,        13.200);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 6.600);
        	triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.0);
        	triggerModule.setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH,       360);
        	triggerModule.setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW,       1);
        }
    }
    
    /**
     * Tests all of the current cluster pairs for triggers.
     * @return Returns <code>true</code> if one of the cluster pairs
     * passes all of the cluster cuts and <code>false</code> otherwise.
     */
    private boolean testTrigger() {
        // Get the list of cluster pairs.
        List<Cluster[]> clusterPairs = getClusterPairsTopBot();
        
        // Iterate over the cluster pairs and perform each of the cluster
        // pair cuts on them. A cluster pair that passes all of the
        // cuts registers as a trigger.
        pairLoop:
        for (Cluster[] clusterPair : clusterPairs) {
            // Increment the number of processed cluster pairs.
            allPairs++;
            
            // Get the plot values for the pair cuts.
            double energySum = TriggerModule.getValueEnergySum(clusterPair);
            double energyDifference = TriggerModule.getValueEnergyDifference(clusterPair);
            double energySlope = TriggerModule.getValueEnergySlope(clusterPair, triggerModule.getCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F));
            double coplanarity = TriggerModule.getValueCoplanarity(clusterPair);
            
            // Fill the general plots.
            pairEnergySum[NO_CUTS].fill(energySum);
            pairEnergyDifference[NO_CUTS].fill(energyDifference);
            pairEnergySlope[NO_CUTS].fill(energySlope);
            pairCoplanarity[NO_CUTS].fill(coplanarity);
            //pairEnergySum.fill(energySum, 1);
            //pairEnergyDifference.fill(energyDifference, 1);
            //pairEnergySlope.fill(energySlope, 1);
            //pairCoplanarity.fill(coplanarity, 1);
            
            // Fill the hit count plots for N > 1.
            if(clusterPair[0].getCalorimeterHits().size() > 1 && clusterPair[1].getCalorimeterHits().size() > 1) {
            	// Populate the plots.
                pairEnergySum[OVER_1HIT].fill(energySum);
                pairEnergyDifference[OVER_1HIT].fill(energyDifference);
                pairEnergySlope[OVER_1HIT].fill(energySlope);
                pairCoplanarity[OVER_1HIT].fill(coplanarity);
            	
                // Fill the hit count plots for N > 2.
                if(clusterPair[0].getCalorimeterHits().size() > 2 && clusterPair[1].getCalorimeterHits().size() > 2) {
                	// Populate the plots.
                    pairEnergySum[OVER_2HIT].fill(energySum);
                    pairEnergyDifference[OVER_2HIT].fill(energyDifference);
                    pairEnergySlope[OVER_2HIT].fill(energySlope);
                    pairCoplanarity[OVER_2HIT].fill(coplanarity);
                }
            }
            
            // ==== Pair Energy Sum Cut ====================================
            // =============================================================
            // If the cluster fails the cut, skip to the next pair.
            if(!triggerModule.pairEnergySumCut(clusterPair)) {
            	continue pairLoop;
            }
            
            // Otherwise, note that it passed the cut.
            pairEnergySumCount++;
            
            // ==== Pair Energy Difference Cut =============================
            // =============================================================
            // If the cluster fails the cut, skip to the next pair.
            if(!triggerModule.pairEnergyDifferenceCut(clusterPair)) {
            	continue pairLoop;
            }
            
            // Otherwise, note that it passed the cut.
            pairEnergyDifferenceCount++;
            
            // ==== Pair Energy Slope Cut ==================================
            // =============================================================
            // If the cluster fails the cut, skip to the next pair.
            if(!triggerModule.pairEnergySlopeCut(clusterPair)) {
            	continue pairLoop;
            }
            
            // Otherwise, note that it passed the cut.
            pairEnergySlopeCount++;
            
            // ==== Pair Coplanarity Cut ===================================
            // =============================================================
            // If the cluster fails the cut, skip to the next pair.
            if(!triggerModule.pairCoplanarityCut(clusterPair)) {
            	continue pairLoop;
            }
            
            // Otherwise, note that it passed the cut.
            pairCoplanarityCount++;
            
            // Get the cluster plot values.
            int[] hitCount = new int[2];
            double[] seedEnergy = new double[2];
            double[] clusterEnergy = new double[2];
            int[] ix = new int[2];
            int[] iy = new int[2];
            for(int i = 0; i < 2; i++) {
                hitCount[i] = clusterPair[i].getCalorimeterHits().size();
                seedEnergy[i] = clusterPair[i].getCalorimeterHits().get(0).getCorrectedEnergy();
                clusterEnergy[i] = clusterPair[i].getEnergy();
                ix[i] = clusterPair[i].getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                iy[i] = clusterPair[i].getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
                if(ix[i] > 0) { ix[i] = ix[i] - 1; }
            }
            
            // Fill the general plots.
            for(int i = 0; i < 2; i++) {
                clusterSeedEnergy[ALL_CUTS].fill(seedEnergy[i]);
                clusterTotalEnergy[ALL_CUTS].fill(clusterEnergy[i]);
                clusterHitCount[ALL_CUTS].fill(hitCount[i]);
                clusterDistribution[ALL_CUTS].fill(ix[i], iy[i]);
                //clusterSeedEnergyAll.fill(seedEnergy[i], 1);
                //clusterTotalEnergyAll.fill(clusterEnergy[i], 1);
                //clusterHitCountAll.fill(hitCount[i], 1);
                //clusterDistributionAll.fill(ix[i], iy[i], 1);
            }
            
            // Fill the "passed all cuts" plots.
            pairEnergySum[ALL_CUTS].fill(energySum, 1);
            pairEnergyDifference[ALL_CUTS].fill(energyDifference, 1);
            pairEnergySlope[ALL_CUTS].fill(energySlope, 1);
            pairCoplanarity[ALL_CUTS].fill(coplanarity, 1);
            
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
     * @param clusterList - The clusters to add to the queues.
     */
    private void updateClusterQueues(List<Cluster> clusterList) {
        // Create lists to store the top and bottom clusters.
        ArrayList<Cluster> topClusterList = new ArrayList<Cluster>();
        ArrayList<Cluster> botClusterList = new ArrayList<Cluster>();
        
        // Loop over the clusters in the cluster list.
        for (Cluster cluster : clusterList) {
            // If the cluster is on the top of the calorimeter, it
            // goes into the top cluster list.
            if (cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") > 0) {
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
    
    /**
     * Sets all cut values for the trigger using a string argument with
     * the format "clusterMin clusterMax hitMin sumMin sumMax diffMax
     * slopeMin slopeF coplanarMax timeDiffMax".
     * @param cuts - The cut string.
     */
    public void setCuts(String cuts) {
    	triggerModule.setCutValues(false, cuts);
    }
}