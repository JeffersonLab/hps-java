package org.hps.users.kmccarty;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Driver <code>SimpleTriggerPlotsDriver</code> produces distributions
 * for all of the singles and pair trigger cuts from clusters found in
 * a user-defined LCIO collection.
 * <br/><br/>
 * Clusters are filtered to remove those clusters from the collection
 * found in the "pulse-clipping region," defined as [nsb, windowWidth -
 * nsa]. This is done because in data, readout does not include pulses
 * past the start and end of the readout window, so clusters occurring
 * such that their integration window would exceed these bounds will
 * produce lower-than-expected energies. While not relevant for Monte
 * Carlo, the same check is applied to ensure that the data plotted is
 * comparable.
 * <br/><br/>
 * The driver requires the window width and the number of integration
 * samples (both before and after the threshold crossing) to be defined
 * in order to check for pulse-clipping. It also requires that the
 * energy sum conversion parameter be defined for calculating the energy
 * sum cut value. Lastly, it requires an LCIO cluster collection to be
 * defined. If preferred, all of these except the collection name may
 * be passed to the DAQ configuration manager instead of using the
 * user-defined values.
 * <br/><br/>
 * This driver does not filter based on trigger type. For data, it is
 * suggested that a filter be added upstream or that a skim be used to
 * account for this, if only data from a specific trigger is desired.
 */
public class SimpleTriggerPlotsDriver extends Driver {
	private int nsa = Integer.MIN_VALUE;
	private int nsb = Integer.MIN_VALUE;
	private boolean useDAQConfig = false;
	private int windowWidth = Integer.MIN_VALUE;
	private String clusterCollectionName = null;
	private boolean requireLeftRightPair = false;
	private double energySlopeParam = Double.NaN;
	private double pairFEEThreshold = Double.NaN;
	private double pairTimeThreshold = Double.NaN;
	private boolean requireFiducialClusters = false;
	
	// Define cluster type and cut type reference variables.
	private static final int CLUSTER_TOTAL_ENERGY  = 0;
	private static final int CLUSTER_SEED_ENERGY   = 1;
	private static final int CLUSTER_HIT_COUNT     = 2;
	private static final int PAIR_ENERGY_SUM       = 3;
	private static final int PAIR_ENERGY_DIFF      = 4;
	private static final int PAIR_ENERGY_SLOPE     = 5;
	private static final int PAIR_COPLANARITY      = 6;
	private static final int PAIR_TIME_COINCIDENCE = 7;
	private static final String[] TRIGGER_CUT_NAME = {
			"Cluster Total Energy", "Cluster Seed Energy", "Cluster Hit Count",
			"Pair Energy Sum", "Pair Energy Difference", "Pair Energy Slope",
			"Pair Coplanarity", "Pair Time Coincidence"
	};
	
	// Define the bin settings for the efficiency plots.
	private int[] bins = new int[8];
	private double[] xMax = {
			2.200,			// Cluster energy,    xMax = 2.2 GeV
			2.200,			// Seed energy,       xMax = 2.2 GeV
			9.5,			// Hit count,         xMax = 9.5 hits
			2.200,			// Energy sum,        xMax = 2.2 GeV
			2.200,			// Energy difference, xMax = 2.2 GeV
			4.000,			// Energy slope,      xMax = 4.0 GeV
			180.0,			// Coplanarity,       xMax = 180 degrees
			30.0			// Time coincidence,  xMax = 30 ns
	};
	private double[] binSize = {
			0.050,			// Cluster energy,    binSize = 50 MeV
			0.050,			// Seed energy,       binSize = 50 MeV
			1,				// Hit count,         binSize = 1 hit
			0.050,			// Energy sum,        binSize = 50 MeV
			0.050,			// Energy difference, binSize = 50 MeV
			0.050,			// Energy slope,      binSize = 50 MeV
			5,				// Coplanarity,       binSize = 5 degrees
			2				// Time coincidence,  binSize = 2 ns
	};
	
	// Define plotting objects.
	private AIDA aida = AIDA.defaultInstance();
	private static final String BASE_FOLDER_NAME = "Simple Trigger Cut Plots/";
	
	/**
	 * Runs at the beginning of the run. Instantiates all trigger cut
	 * plots as well as the DAQ configuration manager, if it is enabled.
	 * Also makes sure that any necessary parameters are defined.
	 */
	@Override
	public void startOfData() {
		// A cluster collection name must be defined.
		if(clusterCollectionName == null) {
			throw new IllegalArgumentException("A cluster collection name must be defined.");
		}
		
		// If the DAQ configuration is not to be used, all of the window
		// and trigger parameters must be defined manually.
		if(!useDAQConfig) {
			// Require that each parameter be defined.
			if(nsa <= 0) {
				throw new IllegalArgumentException("Parameter \"NSA\" must be defined and positive.");
			} if(nsb <= 0) {
				throw new IllegalArgumentException("Parameter \"NSB\" must be defined and positive.");
			} if(nsa <= 0) {
				throw new IllegalArgumentException("Parameter \"Window Width\" must be defined and positive.");
			} if(Double.isNaN(energySlopeParam)) {
				throw new IllegalArgumentException("Parameter \"Energy Slope Conversion Parameter\" must be defined.");
			}
			
			// Check that the window width is large enough to contain
			// some time period where pulse-clipping is not an issue.
			if(windowWidth - nsa - nsb <= 0) {
				throw new IllegalArgumentException("Window width is smaller than integration window; no region is not pulse-clipped.");
			}
		}
		
		// Otherwise, wait for the DAQ configuration manager to receive
		// the settings and set the values appropriately.
		else {
	        ConfigurationManager.addActionListener(new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	                // Get the DAQ configuration.
	                DAQConfig daq = ConfigurationManager.getInstance();
	                
	                // Load the DAQ settings from the configuration manager.
	                nsa = daq.getFADCConfig().getNSA();
	                nsb = daq.getFADCConfig().getNSB();
	                windowWidth = daq.getFADCConfig().getWindowWidth();
	                energySlopeParam = daq.getSSPConfig().getPair2Config().getEnergySlopeCutConfig().getParameterF();
	            }
	        });
		}
		
		// Instantiate plots for each trigger cut.
    	for(int triggerCut = CLUSTER_TOTAL_ENERGY; triggerCut <= PAIR_TIME_COINCIDENCE; triggerCut++) {
        	// Make sure that the maximum x-axis values for the efficiency
        	// plots are evenly divisible by the bin size.
        	if(Math.floor(1.0 * xMax[triggerCut] / binSize[triggerCut]) != (xMax[triggerCut] / binSize[triggerCut])) {
        		xMax[triggerCut] = Math.ceil(xMax[triggerCut] / binSize[triggerCut]) * binSize[triggerCut];
        	}
        	
    		// Define the bin counts for each plot.
    		bins[triggerCut] = (int) Math.ceil(xMax[triggerCut] / binSize[triggerCut]);
    		
    		// Define the numerator, denominator, and ratio plots.
    		aida.histogram1D(getTriggerPlotName(triggerCut), bins[triggerCut], 0.0, bins[triggerCut] * binSize[triggerCut]);
    	}
	}
	
	/**
	 * Runs each event. Collects all clusters from the indicated cluster
	 * collection and filters them to select only those within the time
	 * region safe from pulse-clipping. This is defined as [nsb,
	 * windowWidth - nsa]. All trigger cut values are then plotted for
	 * all clusters and cluster pairs within the filtered set.
	 * @param event - The object containing all event data.
	 */
	@Override
	public void process(EventHeader event) {
		// If the DAQ configuration is in use, only process the event
		// if it has been instantiated.
		if(useDAQConfig && !ConfigurationManager.isInitialized()) {
			return;
		}
		
        // Get the event GTP cluster collection.
        if(!event.hasCollection(Cluster.class, clusterCollectionName)) { return; }
        List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
        
        // Filter the clusters to exclude those affected by the pulse
        // clipping region.
        List<Cluster> goodClusters = new ArrayList<Cluster>();
        for(Cluster cluster : clusters) {
        	// First, check if the cluster is outside the pulse-clipping
        	// region.
        	if(isVerifiable(cluster, nsa, nsb, windowWidth)) {
        		// Next, check that it is fiducial if the fiducial
        		// requirement is active. If so, add the cluster to
        		// the list of good clusters. Otherwise, just add the
        		// the list immediately.
        		if(!requireFiducialClusters || (requireFiducialClusters && TriggerModule.inFiducialRegion(cluster))) {
        			goodClusters.add(cluster);
        		}
        	}
        }
        
        // Populate the cluster singles plots.
        for(Cluster cluster : goodClusters) {
        	aida.histogram1D(getTriggerPlotName(CLUSTER_SEED_ENERGY)).fill(TriggerModule.getValueClusterSeedEnergy(cluster));
        	aida.histogram1D(getTriggerPlotName(CLUSTER_TOTAL_ENERGY)).fill(TriggerModule.getValueClusterTotalEnergy(cluster));
        	aida.histogram1D(getTriggerPlotName(CLUSTER_HIT_COUNT)).fill(TriggerModule.getValueClusterHitCount(cluster));
        }
        
        // Get a list of all top/bottom cluster pairs.
        List<Cluster[]> pairs = TriggerModule.getTopBottomPairs(clusters, Cluster.class);
        
        // Populate the cluster pair plots.
        pairPlotsLoop:
        for(Cluster[] pair : pairs) {
        	// If a left-right cluster pair is required, check that the
        	// condition is met.
        	if(requireLeftRightPair) {
        		boolean hasLeft = (TriggerModule.getClusterXIndex(pair[0]) < 0|| TriggerModule.getClusterXIndex(pair[1]) < 0);
        		boolean hasRight = (TriggerModule.getClusterXIndex(pair[0]) > 0|| TriggerModule.getClusterXIndex(pair[1]) > 0);
        		if(!hasLeft || !hasRight) { continue pairPlotsLoop; }
        	}
        	
        	// If FEE clusters are to be excluded, check if any are present.
        	if(!Double.isNaN(pairFEEThreshold)) {
        		for(Cluster cluster : pair) {
        			// The FEE cut only applies to left clusters.
        			if(TriggerModule.getClusterXIndex(cluster) < 0) {
        				// The pair is invalid if the cluster has more
        				// energy than the threshold.
        				if(TriggerModule.getValueClusterTotalEnergy(cluster) > pairFEEThreshold) {
        					continue pairPlotsLoop;
        				}
        			}
        		}
        	}
        	
        	// If the time coincidence threshold is active, check that
        	// the pair meets the requirement.
        	if(!Double.isNaN(pairTimeThreshold)) {
        		if(TriggerModule.getValueTimeCoincidence(pair) > pairTimeThreshold) {
        			continue pairPlotsLoop;
        		}
        	}
        	
        	// Populate the plots.
        	aida.histogram1D(getTriggerPlotName(PAIR_ENERGY_SUM)).fill(TriggerModule.getValueEnergySum(pair));
        	aida.histogram1D(getTriggerPlotName(PAIR_ENERGY_DIFF)).fill(TriggerModule.getValueEnergyDifference(pair));
        	aida.histogram1D(getTriggerPlotName(PAIR_ENERGY_SLOPE)).fill(TriggerModule.getValueEnergySlope(pair, energySlopeParam));
        	aida.histogram1D(getTriggerPlotName(PAIR_COPLANARITY)).fill(TriggerModule.getValueCoplanarity(pair));
        	aida.histogram1D(getTriggerPlotName(PAIR_TIME_COINCIDENCE)).fill(TriggerModule.getValueTimeCoincidence(pair));
        }
	}
	
    /**
     * Checks whether all of the hits in a cluster are within the safe
     * region of the FADC output window.
     * @param reconCluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster is safe and
     * returns <code>false</code> otherwise.
     */
    private static final boolean isVerifiable(Cluster reconCluster, int nsa, int nsb, int windowWidth) {
        // Iterate over the hits in the cluster.
        for(CalorimeterHit hit : reconCluster.getCalorimeterHits()) {
            // Check that none of the hits are within the disallowed
            // region of the FADC readout window.
            if(hit.getTime() <= nsb || hit.getTime() >= (windowWidth - nsa)) {
                return false;
            }
            
            // Also check to make sure that the cluster does not have
            // any negative energy hits. These are, obviously, wrong.
            if(hit.getCorrectedEnergy() < 0.0) {
                return false;
            }
        }
        
        // If all of the cluster hits pass the time cut, the cluster
        // is valid.
        return true;
    }
    
    /**
     * Forms the name for a specific cut's trigger plot. THis will always
     * be the base folder name plus the name of the trigger cut.
     * @param cutIndex - The index corresponding to the trigger cut.
     * This should use one of the predefined cut indices from this class.
     * @return Returns the plot name as a <code>String</code>.
     */
    private static final String getTriggerPlotName(int cutIndex) {
    	return BASE_FOLDER_NAME + TRIGGER_CUT_NAME[cutIndex];
    }
    
    /**
     * Sets the name of LCIO collection containing the clusters that
     * are to be plotted.
     * @param collection - The LCIO collection name.
     */
    public void setClusterCollectionName(String collection) {
    	clusterCollectionName = collection;
    }
    
    /**
     * Specifies whether the parameters NSA, NSB, readout window width,
     * and energy slope conversion parameter should be obtained from
     * the DAQ configuration or if they should be obtained from user
     * definitions. If the DAQ configuration is used, the energy slope
     * conversion parameter is defined using the pair 1 trigger value.
     * @param state - <code>true</code> means that all values will be
     * acquired from the DAQ configuration. <code>false</code> means
     * that user-defined values are expected.
     */
    public void setUseDAQConfig(boolean state) {
    	useDAQConfig = state;
    }
    
    /**
     * Defines the number of integration samples after a threshold
     * crossing used by the FADC. Note that this is only used if the
     * DAQ configuration manager is disabled.
     * @param value - The number of samples.
     */
    public void setNsa(int value) {
    	nsa = value;
    }
    
    /**
     * Defines the number of integration samples before a threshold
     * crossing used by the FADC. Note that this is only used if the
     * DAQ configuration manager is disabled.
     * @param value - The number of samples.
     */
    public void setNsb(int value) {
    	nsb = value;
    }
    
    /**
     * Defines the size of the readout window. Note that this is only
     * used if the DAQ configuration manager is disabled.
     * @param value - The size of the readout window in nanoseconds.
     */
    public void setWindowWidth(int value) {
    	windowWidth = value;
    }
    
    /**
     * Defines the conversion parameter used in the energy slope
     * calculation. Note that this is only used if the DAQ configuration
     * manager is disabled.
     * @param value - The conversion parameter in units of GeV/mm.
     */
    public void setEnergySlopeConversionParameter(double value) {
    	energySlopeParam = value;
    }
    
    public void setRequireFiducialClusters(boolean state) {
    	requireFiducialClusters = state;
    }
    
    /**
     * Sets whether plotted pairs should carry the additional requirement
     * of having one cluster on the left side of the calorimeter and
     * one cluster on the right. This is disabled by default.
     * @param state - <code>true</code> requires one cluster on each
     * and <code>false</code> does not.
     */
    public void setRequireLeftRightPair(boolean state) {
    	requireLeftRightPair = state;
    }
    
    /**
     * Defines a threshold for FEE electrons for plotted pair values.
     * Pairs with a left-side cluster exceeding this value will not be
     * included in the plots. This can be disabled by setting the value
     * to <code>Double.NaN</code>. This is the default behavior.
     * @param value - The threshold above which left-side clusters are
     * excluded.
     */
    public void setPairFEEThreshold(double value) {
    	pairFEEThreshold = value;
    }
    
    /**
     * Defines a time-coincidence threshold in which cluster pairs must
     * fall in order to be plotted. This can be disabled by setting the
     * value to <code>Double.NaN</code>. This is the default behavior.
     * @param value - The absolute difference in time clusters must fall
     * within in order to be plotted in pair plots.
     */
    public void setPairTimeThreshold(double value) {
    	pairTimeThreshold = value;
    }
    
    public void setClusterSeedEnergyXMax(double value)   { xMax[CLUSTER_SEED_ENERGY]   = value; }
    public void setClusterTotalEnergyXMax(double value)  { xMax[CLUSTER_TOTAL_ENERGY]  = value; }
    public void setClusterHitCountXMax(double value)     { xMax[CLUSTER_HIT_COUNT]     = value; }
    public void setPairEnergySumXMax(double value)       { xMax[PAIR_ENERGY_SUM]       = value; }
    public void setPairEnergyDiffXMax(double value)      { xMax[PAIR_ENERGY_DIFF]      = value; }
    public void setPairEnergySlopeXMax(double value)     { xMax[PAIR_ENERGY_SLOPE]     = value; }
    public void setPairCoplanarityXMax(double value)     { xMax[PAIR_COPLANARITY]      = value; }
    public void setPairTimeCoincidenceXMax(double value) { xMax[PAIR_TIME_COINCIDENCE] = value; }
    
    public void setClusterSeedEnergyBinSize(double value)   { binSize[CLUSTER_SEED_ENERGY]   = value; }
    public void setClusterTotalEnergyBinSize(double value)  { binSize[CLUSTER_TOTAL_ENERGY]  = value; }
    public void setClusterHitCountBinSize(double value)     { binSize[CLUSTER_HIT_COUNT]     = value; }
    public void setPairEnergySumBinSize(double value)       { binSize[PAIR_ENERGY_SUM]       = value; }
    public void setPairEnergyDiffBinSize(double value)      { binSize[PAIR_ENERGY_DIFF]      = value; }
    public void setPairEnergySlopeBinSize(double value)     { binSize[PAIR_ENERGY_SLOPE]     = value; }
    public void setPairCoplanarityBinSize(double value)     { binSize[PAIR_COPLANARITY]      = value; }
    public void setPairTimeCoincidenceBinSize(double value) { binSize[PAIR_TIME_COINCIDENCE] = value; }
}
