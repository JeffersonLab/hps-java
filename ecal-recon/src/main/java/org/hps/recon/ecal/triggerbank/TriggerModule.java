package org.hps.recon.ecal.triggerbank;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.hps.recon.ecal.daqconfig.PairTriggerConfig;
import org.hps.recon.ecal.daqconfig.SinglesTriggerConfig;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

/**
 * Class <code>TriggerModule</code> handles trigger cuts. By default,
 * it sets all cuts such that any cluster or cluster pair will pass.
 * Cuts can be set after initialization via the <code>setCutValue</code>
 * method using a cut identifier. All valid cut identifiers are static
 * class variables for easy reference.<br/>
 * <br/>
 * All cut value calculations are static methods, and can thusly be
 * called without initializing an instance of the module.<br/>
 * <br/>
 * Both <code>Cluster</code> objects and <code>SSPCluster</code> objects
 * are supported.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see Cluster
 * @see SSPCluster
 */
public final class TriggerModule {
	// The calorimeter mid-plane, defined by the photon beam position
	// (30.52 mrad) at the calorimeter face (z = 1393 mm).
    private static final double ORIGIN_X = 1393.0 * Math.tan(0.03052);
    
    // Trigger module property names.
    /** The value of the parameter "F" in the energy slope equation
     * <code>E_low GeV + R_min mm * F GeV/mm</code>. */
    public static final String PAIR_ENERGY_SLOPE_F = "pairEnergySlopeF";
    /** The lower bound for the pair energy sum cut. The sum of the
     * energies of two clusters must exceed this value to pass the cut. */
    public static final String PAIR_ENERGY_SUM_LOW = "pairEnergySumLow";
    /** The upper bound for the pair energy sum cut. The sum of the
     * energies of two clusters must be below this value to pass the cut. */
    public static final String PAIR_ENERGY_SUM_HIGH = "pairEnergySumHigh";
    /** The threshold for the cluster hit count cut. Clusters must have
     * this many hits or more to pass the cut. */
    public static final String CLUSTER_HIT_COUNT_LOW = "clusterHitCountLow";
    /** The threshold for the energy slope cut. The value of the energy
     * slope equation must exceed this value to pass the cut. */
    public static final String PAIR_ENERGY_SLOPE_LOW = "pairEnergySlopeLow";
    /** The bound for the coplanarity cut. The coplanarity angle for
     * the cluster pair must be below this value to pass the cut. */
    public static final String PAIR_COPLANARITY_HIGH = "pairCoplanarityHigh";
    /** The lower bound for the cluster seed energy cut. The seed energy
     * of a cluster must exceed this value to pass the cut. */
    public static final String CLUSTER_SEED_ENERGY_LOW = "clusterSeedEnergyLow";
    /** The upper bound for the cluster seed energy cut. The seed energy
     * of a cluster must be below this value to pass the cut. */
    public static final String CLUSTER_SEED_ENERGY_HIGH = "clusterSeedEnergyHigh";
    /** The lower bound for the cluster total energy cut. The energy
     * of a cluster must exceed this value to pass the cut. */
    public static final String CLUSTER_TOTAL_ENERGY_LOW = "clusterTotalEnergyLow";
    /** The upper bound for the cluster total energy cut. The energy
     * of a cluster must be below this value to pass the cut. */
    public static final String CLUSTER_TOTAL_ENERGY_HIGH = "clusterTotalEnergyHigh";
    /** The bound for the pair energy difference cut. The absolute value
     * of the difference between the energies of the cluster pair must
     * be below this value to pass the cut. */
    public static final String PAIR_ENERGY_DIFFERENCE_HIGH = "pairEnergyDifferenceHigh";
    /** The maximum amount of time by which two clusters are allowed
     * to be separated. */
    public static final String PAIR_TIME_COINCIDENCE = "pairTimeCoincidence";
    
    // Trigger cut settings map.
    private final Map<String, Double> cuts = new HashMap<String, Double>(11);
    
    /**
     * Creates an <code>SSPTriggerModule</code> that accepts all single
     * cluster and cluster pair events.
     */
    public TriggerModule() {
    	// Set the cluster singles cuts to accept all values by default.
    	cuts.put(CLUSTER_HIT_COUNT_LOW, 0.0);
    	cuts.put(CLUSTER_SEED_ENERGY_LOW, 0.0);
    	cuts.put(CLUSTER_SEED_ENERGY_HIGH, Double.MAX_VALUE);
    	cuts.put(CLUSTER_TOTAL_ENERGY_LOW, 0.0);
    	cuts.put(CLUSTER_TOTAL_ENERGY_HIGH, Double.MAX_VALUE);
    	
    	// Set the cluster pair cuts to accept all values by default.
    	cuts.put(PAIR_COPLANARITY_HIGH, 180.0);
    	cuts.put(PAIR_ENERGY_DIFFERENCE_HIGH, Double.MAX_VALUE);
    	cuts.put(PAIR_ENERGY_SLOPE_LOW, 0.0);
    	cuts.put(PAIR_ENERGY_SUM_LOW, 0.0);
    	cuts.put(PAIR_ENERGY_SUM_HIGH, Double.MAX_VALUE);
    	cuts.put(PAIR_TIME_COINCIDENCE, Double.MAX_VALUE);
    	
    	// Set the default value of the energy slope parameter F.
    	cuts.put(PAIR_ENERGY_SLOPE_F, 0.0055);
    }
    
    /**
     * Creates an <code>SSPTriggerModule</code> that uses the default
     * cut values specified by the argument array. The array should be
     * of size 11. Values are applied in the order:
     * <ul>
     * <li>Cluster Hit Count Lower Bound</li>
     * <li>Cluster Seed Energy Lower Bound</li>
     * <li>Cluster Seed Energy Upper Bound</li>
     * <li>Cluster Seed Total Lower Bound</li>
     * <li>Cluster Seed Total Upper Bound</li>
     * <li>Pair Energy Sum Lower Bound</li>
     * <li>Pair Energy Sum Upper Bound</li>
     * <li>Pair Energy Difference Upper Bound</li>
     * <li>Pair Energy Slope Lower Bound</li>
     * <li>Pair Coplanarity Upper Bound</li>
     * <li>Pair Energy Slope Parameter F</li>
     * </ul>
     */
    public TriggerModule(double... cutValues) {
    	// Set the cuts to the default values.
    	this();
    	
    	// Define the cuts in the order that they correspond to the
    	// value arguments.
    	String[] cutID = { CLUSTER_HIT_COUNT_LOW, CLUSTER_SEED_ENERGY_LOW, CLUSTER_SEED_ENERGY_HIGH,
    			CLUSTER_TOTAL_ENERGY_LOW, CLUSTER_TOTAL_ENERGY_HIGH, PAIR_ENERGY_SUM_LOW, PAIR_ENERGY_SUM_HIGH,
    			PAIR_ENERGY_DIFFERENCE_HIGH, PAIR_ENERGY_SLOPE_LOW, PAIR_COPLANARITY_HIGH, PAIR_ENERGY_SLOPE_F };
    	
    	// Iterate over the value arguments and assign them to the
    	// appropriate cut.
    	for(int i = 0; i < cutValues.length; i++) {
    		// If more values were given then cuts exist, break from
    		// the loop.
    		if(i == 11) { break; }
    		
    		// Set the current cut to its corresponding value.
    		cuts.put(cutID[i], cutValues[i]);
    	}
    }
    
    /**
     * Gets the value of the requested cut, if it exists.
     * @param cut - The identifier of the cut.
     * @return Returns the cut value as a <code>double</code>.
     * @throws IllegalArgumentException Occurs if the cut does not exist.
     */
    public double getCutValue(String cut) throws IllegalArgumentException {
    	// Try to get the indicated cut.
    	Double value = cuts.get(cut);
    	
    	// If the cut is valid, return it.
    	if(value != null) { return value.doubleValue(); }
    	
    	// Otherwise, produce an exception.
    	else { throw new IllegalArgumentException(String.format("Cut \"%s\" does not exist.", cut)); }
    }
    
    /**
     * Loads triggers settings from the DAQ configuration for a singles
     * trigger. Pair trigger settings will be set to accept all possible
     * values, while singles trigger settings will
     * @param config - The DAQ configuration settings.
     */
    public void loadDAQConfiguration(SinglesTriggerConfig config) {
    	// Set the trigger values.
    	setCutValue(CLUSTER_TOTAL_ENERGY_LOW,  config.getEnergyMinCutConfig().getLowerBound());
    	setCutValue(CLUSTER_TOTAL_ENERGY_HIGH, config.getEnergyMaxCutConfig().getUpperBound());
    	setCutValue(CLUSTER_HIT_COUNT_LOW,     config.getHitCountCutConfig().getLowerBound());
    	
    	// The remaining triggers should be set to their default values.
    	// These settings effectively accept all possible clusters.
    	cuts.put(PAIR_COPLANARITY_HIGH, 180.0);
    	cuts.put(PAIR_ENERGY_DIFFERENCE_HIGH, Double.MAX_VALUE);
    	cuts.put(PAIR_ENERGY_SLOPE_LOW, 0.0);
    	cuts.put(PAIR_ENERGY_SUM_LOW, 0.0);
    	cuts.put(PAIR_ENERGY_SUM_HIGH, Double.MAX_VALUE);
    	cuts.put(PAIR_TIME_COINCIDENCE, Double.MAX_VALUE);
    	
    	// Set the default value of the energy slope parameter F.
    	cuts.put(PAIR_ENERGY_SLOPE_F, 0.0055);
    }
    
    /**
     * Loads triggers settings from the DAQ configuration for a pair
     * trigger. All trigger settings will be loaded directly from the
     * DAQ configuration and set appropriately.
     * @param config - The DAQ configuration settings.
     */
    public void loadDAQConfiguration(PairTriggerConfig config) {
    	// Set the trigger values.
    	setCutValue(CLUSTER_TOTAL_ENERGY_LOW,  config.getEnergyMinCutConfig().getLowerBound());
    	setCutValue(CLUSTER_TOTAL_ENERGY_HIGH, config.getEnergyMaxCutConfig().getUpperBound());
    	setCutValue(CLUSTER_HIT_COUNT_LOW,     config.getHitCountCutConfig().getLowerBound());
    	
    	// The remaining triggers should be set to their default values.
    	// These settings effectively accept all possible clusters.
    	cuts.put(PAIR_COPLANARITY_HIGH, config.getCoplanarityCutConfig().getUpperBound());
    	cuts.put(PAIR_ENERGY_DIFFERENCE_HIGH, config.getEnergyDifferenceCutConfig().getUpperBound());
    	cuts.put(PAIR_ENERGY_SLOPE_LOW, config.getEnergySlopeCutConfig().getLowerBound());
    	cuts.put(PAIR_ENERGY_SUM_LOW, config.getEnergySumCutConfig().getLowerBound());
    	cuts.put(PAIR_ENERGY_SUM_HIGH, config.getEnergySumCutConfig().getUpperBound());
    	cuts.put(PAIR_TIME_COINCIDENCE, config.getTimeDifferenceCutConfig().getUpperBound() * 4.0);
    	
    	// Set the default value of the energy slope parameter F.
    	cuts.put(PAIR_ENERGY_SLOPE_F, config.getEnergySlopeCutConfig().getParameterF());
    }
    
    /**
     * Sets the value of the indicated cut to a new value.
     * @param cut - The identifier of the cut to which the new value
     * should be assigned.
     * @param value - The new cut value.
     * @throws IllegalArgumentException Occurs if the cut does not exist.
     */
    public void setCutValue(String cut, double value) throws IllegalArgumentException {
    	// Make sure that the cut exists. If it does, change it to the
    	// new cut value.
    	if(cuts.containsKey(cut)) {
    		cuts.put(cut, value);
    	}
    	
    	// Otherwise, throw an exception.
    	else { throw new IllegalArgumentException(String.format("Cut \"%s\" does not exist.", cut)); }
    }
    
	/**
	 * Sets the cluster singles cuts to the values parsed from an
	 * argument string.
	 * @param isSingles - Indicates whether the parser should expect
	 * 10 cut values (for pairs) or 3 (for singles).
	 * @param cutValues - A string representing the cuts values. This
	 * must be formatted in the style of "Emin Emax Nmin ...".
	 */
	public void setCutValues(boolean isSingles, String cutValues) {
		// Make sure that the string is not null.
		if(cutValues == null) {
			throw new NullPointerException(String.format("Cut arguments for trigger are null!"));
		}
		
		// Tokenize the argument string.
		StringTokenizer tokens = new StringTokenizer(cutValues);
		
		// Store the cut values. Entry format is:
		// clusterEnergyMin clusterEnergyMax hitCountMin
		// clusterEnergyMin clusterEnergyMax hitCountMin pairSumMin pairSumMax pairDiffMax pairSlopeMin pairSlopeF pairCoplanarityMax pairTimeCoincidence
		double cuts[];
		if(isSingles) { cuts = new double[] { 0.0, 8.191, 0 }; }
		else { cuts = new double[] { 0.0, 8.191, 0, 0, 8.191, 8.191, 0, 0.0055, 180, Double.MAX_VALUE }; }
		String[] cutNames = { "clusterEnergyMin", "clusterEnergyMax", "hitCountMin",
				"pairSumMin", "pairSumMax", "pairDiffMax", "pairSlopeMin", "pairSlopeF",
				"pairCoplanarityMax", "pairTimeCoincidence" };
		
		// Iterate over the number of cuts and extract that many values
		// from the cut value string.
		for(int cutNum = 0; cutNum < cuts.length; cutNum++) {
			// If there are no more tokens left, the argument string
			// is missing some values. Throw an exception!
			if(tokens.hasMoreTokens()) {
				// Get the next token from the string.
				String arg = tokens.nextToken();
				
				// Try to parse the token as a double. All cut values
				// should be rendered as doubles (or integers, which
				// can be parsed as doubles). If it is not, the string
				// is improperly formatted.
				try { cuts[cutNum] = Double.parseDouble(arg); }
				catch(NumberFormatException e) {
					throw new NumberFormatException(String.format("Argument for \"%s\" improperly formatted: %s", cutNames[cutNum], arg));
				}
			}
		}
		
		// Store the cuts in the trigger.
		setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW,    cuts[0]);
		setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH,   cuts[1]);
		setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW,       cuts[2]);
		setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW,         cuts[3]);
		setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH,        cuts[4]);
		setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, cuts[5]);
		setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       cuts[6]);
		setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F,         cuts[7]);
		setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH,       cuts[8]);
		setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE,       cuts[9]);
	}
    
    /**
     * Checks whether the argument cluster possesses the minimum
     * allowed hits.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterHitCountCut(Cluster cluster) {
        return clusterHitCountCut(getValueClusterHitCount(cluster));
    }
    
    /**
     * Checks whether the argument cluster possesses the minimum
     * allowed hits.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterHitCountCut(SSPCluster cluster) {
        return clusterHitCountCut(getValueClusterHitCount(cluster));
    }
    
    /**
     * Checks whether the argument cluster seed hit falls within the
     * allowed seed hit energy range.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterSeedEnergyCut(Cluster cluster) {
    	return clusterSeedEnergyCut(getValueClusterSeedEnergy(cluster));
    }
    
    /**
     * Checks whether the argument cluster seed hit falls below the
     * allowed seed hit energy upper bound.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterSeedEnergyCutHigh(Cluster cluster) {
    	return clusterSeedEnergyCutHigh(getValueClusterSeedEnergy(cluster));
    }
    
    /**
     * Checks whether the argument cluster seed hit falls above the
     * allowed seed hit energy lower bound.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterSeedEnergyCutLow(Cluster cluster) {
    	return clusterSeedEnergyCutLow(getValueClusterSeedEnergy(cluster));
    }
    
    /**
     * Checks whether the argument cluster falls within the allowed
     * cluster total energy range.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterTotalEnergyCut(Cluster cluster) {
    	return clusterTotalEnergyCut(getValueClusterTotalEnergy(cluster));
    }
    
    /**
     * Checks whether the argument cluster falls below the allowed
     * cluster total energy upper bound.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterTotalEnergyCutHigh(Cluster cluster) {
    	return clusterTotalEnergyCutHigh(getValueClusterTotalEnergy(cluster));
    }
    
    /**
     * Checks whether the argument cluster falls above the allowed
     * cluster total energy lower bound.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterTotalEnergyCutLow(Cluster cluster) {
    	return clusterTotalEnergyCutLow(getValueClusterTotalEnergy(cluster));
    }
    
    /**
     * Checks whether the argument cluster falls within the allowed
     * cluster total energy range.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterTotalEnergyCut(SSPCluster cluster) {
    	return clusterTotalEnergyCut(getValueClusterTotalEnergy(cluster));
    }
    
    /**
     * Checks whether the argument cluster falls below the allowed
     * cluster total energy upper bound.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterTotalEnergyCutHigh(SSPCluster cluster) {
    	return clusterTotalEnergyCutHigh(getValueClusterTotalEnergy(cluster));
    }
    
    /**
     * Checks whether the argument cluster falls above the allowed
     * cluster total energy lower bound.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterTotalEnergyCutLow(SSPCluster cluster) {
    	return clusterTotalEnergyCutLow(getValueClusterTotalEnergy(cluster));
    }
    
    /**
     * Calculates the distance between the origin and a cluster.
     * @param cluster - The cluster pair from which the value should
     * be calculated.
     * @return Returns displacement of the cluster.
     */
    public static double getClusterDistance(Cluster cluster) {
    	// Get the variables from the cluster.
    	double x = getClusterX(cluster);
    	double y = getClusterY(cluster);
    	
    	// Perform the calculation.
    	return getClusterDistance(x, y);
    }
    
    /**
     * Gets the size of a cluster.
     * @param cluster - The cluster for which to obtain the size.
     * @return Returns the size as an <code>int</code>.
     */
    public static final double getClusterHitCount(Cluster cluster) {
    	return cluster.getCalorimeterHits().size();
    }
    
    /**
     * Gets the seed hit from a <code>Cluster</code> object.
     * @param cluster - The cluster.
     * @return Returns the seed hit as a <code>CalorimeterHit</code>
     * object.
     */
    public static final CalorimeterHit getClusterSeedHit(Cluster cluster) {
    	if(getClusterHitCount(cluster) > 0) {
    		return cluster.getCalorimeterHits().get(0);
    	} else {
    		throw new NullPointerException("Cluster does not define hits!");
    	}
    }
    
    /**
     * Gets the time-stamp of a cluster.
     * @param cluster - The cluster for which to obtain the time.
     * @return Returns the time as a <code>double</code>.
     */
    public static final double getClusterTime(Cluster cluster) {
    	return getClusterSeedHit(cluster).getTime();
    }
    
    /**
     * Gets the x-position of a cluster in millimeters in the hardware
     * coordinate system.
     * @param cluster - The cluster of which to get the x-position.
     * @return Returns the cluster x-position.
     */
    public static double getClusterX(Cluster cluster) {
    	return getCrystalPosition(getClusterXIndex(cluster), getClusterYIndex(cluster))[0];
    }
    
    /**
     * Gets the x-position of a cluster in millimeters in the hardware
     * coordinate system.
     * @param cluster - The cluster of which to get the x-position.
     * @return Returns the cluster x-position.
     */
    public static double getClusterX(SSPCluster cluster) {
    	return getCrystalPosition(cluster.getXIndex(), cluster.getYIndex())[0];
    }
    
    /**
     * Gets the x-index of a cluster.
     * @param cluster - The cluster for which to obtain the index.
     * @return Returns the index as an <code>int</code>.
     */
    public static final int getClusterXIndex(Cluster cluster) {
    	return getClusterSeedHit(cluster).getIdentifierFieldValue("ix");
    }
    
    /**
     * Gets the y-position of a cluster in millimeters in the hardware
     * coordinate system.
     * @param cluster - The cluster of which to get the y-position.
     * @return Returns the cluster y-position.
     */
    public static double getClusterY(Cluster cluster) {
    	return getCrystalPosition(getClusterXIndex(cluster), getClusterYIndex(cluster))[1];
    }
    
    /**
     * Gets the y-position of a cluster in millimeters in the hardware
     * coordinate system.
     * @param cluster - The cluster of which to get the y-position.
     * @return Returns the cluster y-position.
     */
    public static double getClusterY(SSPCluster cluster) {
    	return getCrystalPosition(cluster.getXIndex(), cluster.getYIndex())[1];
    }
    
    /**
     * Gets the y-index of a cluster.
     * @param cluster - The cluster for which to obtain the index.
     * @return Returns the index as an <code>int</code>.
     */
    public static final int getClusterYIndex(Cluster cluster) {
    	return getClusterSeedHit(cluster).getIdentifierFieldValue("iy");
    }
    
    /**
     * Gets the z-position of a cluster in millimeters in the hardware
     * coordinate system.
     * @param cluster - The cluster of which to get the z-position.
     * @return Returns the cluster z-position.
     */
    public static double getClusterZ(Cluster cluster) {
    	return getCrystalPosition(getClusterXIndex(cluster), getClusterYIndex(cluster))[2];
    }
    
    /**
     * Gets the z-position of a cluster in millimeters in the hardware
     * coordinate system.
     * @param cluster - The cluster of which to get the z-position.
     * @return Returns the cluster z-position.
     */
    public static double getClusterZ(SSPCluster cluster) {
    	return getCrystalPosition(cluster.getXIndex(), cluster.getYIndex())[2];
    }
    
    /**
     * Gets the value used for the cluster total energy cut.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the energy of the entire cluster in GeV.
     */
    public static double getValueClusterTotalEnergy(Cluster cluster) {
        return cluster.getEnergy();
    }
    
    /**
     * Gets the value used for the cluster total energy cut.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the energy of the entire cluster in GeV.
     */
    public static double getValueClusterTotalEnergy(SSPCluster cluster) {
        return cluster.getEnergy();
    }
    
    /**
     * Gets the value used for the cluster hit count cut.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the number of hits in the cluster.
     */
    public static int getValueClusterHitCount(Cluster cluster) {
        return cluster.getCalorimeterHits().size();
    }
    
    /**
     * Gets the value used for the cluster hit count cut.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the number of hits in the cluster.
     */
    public static int getValueClusterHitCount(SSPCluster cluster) {
        return cluster.getHitCount();
    }
    
    /**
     * Gets the value used for the seed hit energy cut.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the cluster seed energy in GeV.
     */
    public static double getValueClusterSeedEnergy(Cluster cluster) {
        return cluster.getCalorimeterHits().get(0).getCorrectedEnergy();
    }
    
    /**
     * Calculates the value used by the coplanarity cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    public static double getValueCoplanarity(Cluster[] clusterPair) {
    	// Get the variables used by the calculation.
    	double x[] = { getClusterX(clusterPair[0]), getClusterX(clusterPair[1]) };
    	double z[] = { getClusterZ(clusterPair[0]), getClusterZ(clusterPair[1]) };
    	
    	// Return the calculated value.
    	return getValueCoplanarity(x, z);
    }
    
    /**
     * Calculates the value used by the coplanarity cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    public static double getValueCoplanarity(SSPCluster[] clusterPair) {
    	// Get the variables used by the calculation.
    	double x[] = { getClusterX(clusterPair[0]), getClusterX(clusterPair[1]) };
    	double z[] = { getClusterZ(clusterPair[0]), getClusterZ(clusterPair[1]) };
    	
    	// Return the calculated value.
    	return getValueCoplanarity(x, z);
    }
    
    /**
     * Calculates the value used by the coplanarity cut using the LCSim
     * coordinate system. This method will not match the hardware results.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    @Deprecated
    public static double getValueCoplanarityLegacy(Cluster[] clusterPair) {
    	// Get the variables used by the calculation.
    	double x[] = { getClusterSeedHit(clusterPair[0]).getIdentifierFieldValue("ix"),
    			getClusterSeedHit(clusterPair[1]).getIdentifierFieldValue("ix") };
    	double y[] = { getClusterSeedHit(clusterPair[0]).getIdentifierFieldValue("iy"),
    			getClusterSeedHit(clusterPair[1]).getIdentifierFieldValue("iy") };
    	
    	// Return the calculated value.
    	return getValueCoplanarityLegacy(x, y);
    }
    
    /**
     * Calculates the value used by the energy difference cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the difference between the cluster energies.
     */
    public static double getValueEnergyDifference(Cluster[] clusterPair) {
    	// Get the variables used by the calculation.
    	double[] energy = { clusterPair[0].getEnergy(), clusterPair[1].getEnergy() };
    	
    	// Perform the calculation.
        return getValueEnergyDifference(energy);
    }
    
    /**
     * Calculates the value used by the energy difference cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the difference between the cluster energies.
     */
    public static double getValueEnergyDifference(SSPCluster[] clusterPair) {
    	// Get the variables used by the calculation.
    	double[] energy = { clusterPair[0].getEnergy(), clusterPair[1].getEnergy() };
    	
    	// Perform the calculation.
        return getValueEnergyDifference(energy);
    }
    
    /**
     * Calculates the value used by the energy slope cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @param energySlopeParamF - The value of the variable F in the
     * energy slope equation E_low + R_min * F.
     * @return Returns the energy slope value.
     */
    public static double getValueEnergySlope(Cluster[] clusterPair, double energySlopeParamF) {
    	// Get the variables used by the calculation.
    	double[] energy = { clusterPair[0].getEnergy(), clusterPair[1].getEnergy() };
    	double x[] = { getClusterX(clusterPair[0]), getClusterX(clusterPair[1]) };
    	double z[] = { getClusterZ(clusterPair[0]), getClusterZ(clusterPair[1]) };
    	
    	// Perform the calculation.
    	return getValueEnergySlope(energy, x, z, energySlopeParamF);
    }
    
    /**
     * Calculates the value used by the energy slope cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @param energySlopeParamF - The value of the variable F in the
     * energy slope equation E_low + R_min * F.
     * @return Returns the energy slope value.
     */
    public static double getValueEnergySlope(SSPCluster[] clusterPair, double energySlopeParamF) {
    	// Get the variables used by the calculation.
    	double[] energy = { clusterPair[0].getEnergy(), clusterPair[1].getEnergy() };
    	double x[] = { getClusterX(clusterPair[0]), getClusterX(clusterPair[1]) };
    	double z[] = { getClusterZ(clusterPair[0]), getClusterZ(clusterPair[1]) };
    	
    	// Perform the calculation.
    	return getValueEnergySlope(energy, x, z, energySlopeParamF);
    }
    
    /**
     * Calculates the value used by the energy slope cut the LCSim
     * coordinate system.  This method will not match the hardware
     * results.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @param energySlopeParamF - The value of the variable F in the
     * energy slope equation E_low + R_min * F.
     * @return Returns the energy slope value.
     */
    @Deprecated
    public static double getValueEnergySlopeLegacy(Cluster[] clusterPair, double energySlopeParamF) {
    	// Get the variables used by the calculation.
    	double[] energy = { clusterPair[0].getEnergy(), clusterPair[1].getEnergy() };
    	double x[] = { getClusterSeedHit(clusterPair[0]).getIdentifierFieldValue("ix"),
    			getClusterSeedHit(clusterPair[1]).getIdentifierFieldValue("ix") };
    	double y[] = { getClusterSeedHit(clusterPair[0]).getIdentifierFieldValue("iy"),
    			getClusterSeedHit(clusterPair[1]).getIdentifierFieldValue("iy") };
    	
    	// Perform the calculation.
    	return getValueEnergySlopeLegacy(energy, x, y, energySlopeParamF);
    }
    
    /**
     * Calculates the value used by the energy sum cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the sum of the cluster energies.
     */
    public static double getValueEnergySum(Cluster[] clusterPair) {
    	// Get the variables used by the calculation.
    	double[] energy = { clusterPair[0].getEnergy(), clusterPair[1].getEnergy() };
    	
    	// Perform the calculation.
    	return getValueEnergySum(energy);
    }
    
    /**
     * Calculates the value used by the energy sum cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the sum of the cluster energies.
     */
    public static double getValueEnergySum(SSPCluster[] clusterPair) {
    	// Get the variables used by the calculation.
    	double[] energy = { clusterPair[0].getEnergy(), clusterPair[1].getEnergy() };
    	
    	// Perform the calculation.
    	return getValueEnergySum(energy);
    }
    
    /**
     * Calculates the value used by the time coincidence cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the absolute difference in the cluster times..
     */
    public static double getValueTimeCoincidence(Cluster[] clusterPair) {
    	// Get the variables used by the calculation.
    	double[] time = { clusterPair[0].getCalorimeterHits().get(0).getTime(),
    			clusterPair[1].getCalorimeterHits().get(0).getTime() };
    	
    	// Perform the calculation.
    	return getValueTimeCoincidence(time);
    }
    
    /**
     * Calculates the value used by the time coincidence cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the absolute difference in the cluster times..
     */
    public static double getValueTimeCoincidence(SSPCluster[] clusterPair) {
    	// Get the variables used by the calculation.
    	double[] time = { clusterPair[0].getTime(), clusterPair[1].getTime() };
    	
    	// Perform the calculation.
    	return getValueTimeCoincidence(time);
    }
    
    /**
     * Checks if a cluster pair is coplanar to the beam within a given
     * angle.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairCoplanarityCut(Cluster[] clusterPair) {
        return pairCoplanarityCut(getValueCoplanarity(clusterPair));
    }
    
    /**
     * Checks if a cluster pair is coplanar to the beam within a given
     * angle.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairCoplanarityCut(SSPCluster[] clusterPair) {
        return pairCoplanarityCut(getValueCoplanarity(clusterPair));
    }
    
    /**
     * Checks if the energy difference between the clusters making up
     * a cluster pair is below an energy difference threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergyDifferenceCut(Cluster[] clusterPair) {
        return pairEnergyDifferenceCut(getValueEnergyDifference(clusterPair));
    }
    
    /**
     * Checks if the energy difference between the clusters making up
     * a cluster pair is below an energy difference threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergyDifferenceCut(SSPCluster[] clusterPair) {
        return pairEnergyDifferenceCut(getValueEnergyDifference(clusterPair));
    }
    
    /**
     * Requires that the distance from the beam of the lowest energy
     * cluster in a cluster pair satisfies the following:<br/>
     * <code>E_low + R_min * F < [ Threshold ]</code>
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergySlopeCut(Cluster[] clusterPair) {
        return pairEnergySlopeCut(getValueEnergySlope(clusterPair, cuts.get(PAIR_ENERGY_SLOPE_F)));
    }
    
    /**
     * Requires that the distance from the beam of the lowest energy
     * cluster in a cluster pair satisfies the following:<br/>
     * <code>E_low + R_min * F < [ Threshold ]</code>
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergySlopeCut(SSPCluster[] clusterPair) {
        return pairEnergySlopeCut(getValueEnergySlope(clusterPair, cuts.get(PAIR_ENERGY_SLOPE_F)));
    }
    
    /**
     * Checks if the sum of the energies of the clusters making up a
     * cluster pair is within an energy sum threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergySumCut(Cluster[] clusterPair) {
    	return pairEnergySumCut(getValueEnergySum(clusterPair));
    }
    
    /**
     * Checks if the sum of the energies of the clusters making up a
     * cluster pair is below the energy sum upper bound threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergySumCutHigh(Cluster[] clusterPair) {
    	return pairEnergySumCutHigh(getValueEnergySum(clusterPair));
    }
    
    /**
     * Checks if the sum of the energies of the clusters making up a
     * cluster pair is above the energy sum lower bound threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergySumCutLow(Cluster[] clusterPair) {
    	return pairEnergySumCutLow(getValueEnergySum(clusterPair));
    }
    
    /**
     * Checks if the sum of the energies of clusters making up a cluster
     * pair is below an energy sum threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergySumCut(SSPCluster[] clusterPair) {
    	return pairEnergySumCut(getValueEnergySum(clusterPair));
    }
    
    /**
     * Checks if the sum of the energies of the clusters making up a
     * cluster pair is below the energy sum upper bound threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergySumCutHigh(SSPCluster[] clusterPair) {
    	return pairEnergySumCutHigh(getValueEnergySum(clusterPair));
    }
    
    /**
     * Checks if the sum of the energies of the clusters making up a
     * cluster pair is above the energy sum lower bound threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairEnergySumCutLow(SSPCluster[] clusterPair) {
    	return pairEnergySumCutLow(getValueEnergySum(clusterPair));
    }
    
    /**
     * Checks if the absolute difference between the times between
     * two clusters is below the time coincidence cut.
     * @param clusterPair - The cluster pair to check.
     * @return <code>true</code> if the energy sum passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairTimeCoincidenceCut(Cluster[] clusterPair) {
    	return pairTimeCoincidenceCut(getValueTimeCoincidence(clusterPair));
    }
    
    /**
     * Checks if the absolute difference between the times between
     * two clusters is below the time coincidence cut.
     * @param clusterPair - The cluster pair to check.
     * @return <code>true</code> if the energy sum passes
     * the cut and <code>false</code> if it does not.
     */
    public boolean pairTimeCoincidenceCut(SSPCluster[] clusterPair) {
    	return pairTimeCoincidenceCut(getValueTimeCoincidence(clusterPair));
    }
    
    /**
     * Checks whether the argument hit count meets the minimum required
     * hit count.
     * @param hitCount - The number of hits in the cluster.
     * @return Returns <code>true</code> if the count passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterHitCountCut(int hitCount) {
        return (hitCount >= cuts.get(CLUSTER_HIT_COUNT_LOW));
    }
    
    /**
     * Checks whether the argument energy seed hit falls within the
     * allowed seed hit energy range.
     * @param seedEnergy - The energy of the cluster seed.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterSeedEnergyCut(double seedEnergy) {
        return clusterSeedEnergyCutHigh(seedEnergy) && clusterSeedEnergyCutLow(seedEnergy);
    }
    
    /**
     * Checks whether the argument energy falls below the cluster seed
     * energy upper bound cut.
     * @param seedEnergy - The energy of the cluster seed.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterSeedEnergyCutHigh(double seedEnergy) {
        return (seedEnergy <= cuts.get(CLUSTER_SEED_ENERGY_HIGH));
    }
    
    /**
     * Checks whether the argument energy falls above the cluster seed
     * energy lower bound cut.
     * @param seedEnergy - The energy of the cluster seed.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterSeedEnergyCutLow(double seedEnergy) {
        return (seedEnergy >= cuts.get(CLUSTER_SEED_ENERGY_LOW));
    }
    
    /**
     * Checks whether the argument energy falls within the allowed
     * cluster total energy range.
     * @param clusterEnergy - The energy of the entire cluster.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterTotalEnergyCut(double clusterEnergy) {
        return clusterTotalEnergyCutHigh(clusterEnergy) && clusterTotalEnergyCutLow(clusterEnergy);
    }
    
    /**
     * Checks whether the argument energy falls below the cluster total
     * energy upper bound cut.
     * @param clusterEnergy - The energy of the entire cluster.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterTotalEnergyCutHigh(double clusterEnergy) {
        return (clusterEnergy <= cuts.get(CLUSTER_TOTAL_ENERGY_HIGH));
    }
    
    /**
     * Checks whether the argument energy falls above the cluster total
     * energy lower bound cut.
     * @param clusterEnergy - The energy of the entire cluster.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterTotalEnergyCutLow(double clusterEnergy) {
        return (clusterEnergy >= cuts.get(CLUSTER_TOTAL_ENERGY_LOW));
    }
    
    /**
     * Calculates the distance between the origin and a cluster.
     * @param x - The cluster's x-position.
     * @param z - The cluster's z-position.
     * @return Returns displacement of the cluster.
     */
    private static double getClusterDistance(double x, double z) {
        return Math.hypot(x, z);
    }
    
    /**
     * Gets the mapped position used by the SSP for a specific crystal.
     * @param ix - The crystal x-index.
     * @param iy - The crystal y-index.
     * @return Returns the crystal position as a <double</code> array
     * where the coordinates are ordered from the lowest to highest
     * index as x, y, z.
     * @throws IndexOutOfBoundsException Occurs if in either of the
     * cases where <code>ix == 0</code> or <code>|ix| > 23</code> for
     * the x-index and either of the cases where <code>iy == 0</code>
     * or <code>|iy| > 5</code> for the y-index.
     */
	private static double[] getCrystalPosition(int ix, int iy) throws IndexOutOfBoundsException {
		// Make sure that the requested crystal is a valid crystal.
		if(ix == 0 || ix < -23 || ix > 23) {
			throw new IndexOutOfBoundsException(String.format("Value \"%d\" is invalid for field x-index.", ix));
		} if(iy == 0 || iy < -5 || iy > 5) {
			throw new IndexOutOfBoundsException(String.format("Value \"%d\" is invalid for field y-index.", iy));
		}
		
		// Return the mapped position.
		if(ix < 1) { return position[5 - iy][22 - ix]; }
		else { return position[5 - iy][23 - ix]; }
	}
    
    /**
     * Calculates the value used by the coplanarity cut.
     * @param x - A two-dimensional array consisting of the first and
     * second clusters' x-positions.
     * @param y - A two-dimensional array consisting of the first and
     * second clusters' y-positions.
     * @return Returns the cluster pair's coplanarity.
     */
    private static double getValueCoplanarity(double[] x, double z[]) {
        // Get the cluster angles.
        int[] clusterAngle = new int[2];
        for(int i = 0; i < 2; i++) {
        	clusterAngle[i] = (int) Math.round(Math.atan(x[i] / z[i]) * 180.0 / Math.PI);
        }
        
        // Calculate the coplanarity cut value.
        return Math.abs(clusterAngle[1] - clusterAngle[0]);
    }
    
    /**
     * Calculates the value used by the coplanarity cut. This method
     * is not accurate to the hardware is retained only for legacy
     * purposes at this time.
     * @param x - A two-dimensional array consisting of the first and
     * second clusters' x-positions.
     * @param y - A two-dimensional array consisting of the first and
     * second clusters' y-positions.
     * @return Returns the cluster pair's coplanarity.
     */
    @Deprecated
    private static double getValueCoplanarityLegacy(double[] x, double y[]) {
        // Get the cluster angles.
        double[] clusterAngle = new double[2];
        for(int i = 0; i < 2; i++) {
            clusterAngle[i] = (Math.toDegrees(Math.atan2(y[i], x[i] - ORIGIN_X)) + 180.0) % 180.0;
        }
        
        // Calculate the coplanarity cut value.
        return Math.abs(clusterAngle[1] - clusterAngle[0]);
    }
    
    /**
     * Calculates the value used by the energy difference cut.
     * @param energy - A two-dimensional array consisting of the first
     * and second clusters' energies.
     * @return Returns the difference of the cluster energies.
     */
    private static double getValueEnergyDifference(double[] energy) {
        return Math.abs(energy[0] - energy[1]);
    }
    
    /**
     * Calculates the value used by the energy slope cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @param energySlopeParamF - The value of the variable F in the
     * energy slope equation E_low + R_min * F.
     * @return Returns the cut value.
     */
    private static double getValueEnergySlope(double energy[], double x[], double z[], double energySlopeParamF) {
    	// Determine which cluster is the lower-energy cluster.
    	int lei = energy[0] < energy[1] ? 0 : 1;
    	
        // E + R*F
        // Get the low energy cluster energy.
        double slopeParamE = energy[lei];
        
        // Get the low energy cluster radial distance.
        double slopeParamR = Math.sqrt((x[lei] * x[lei]) + (z[lei] * z[lei]));
        
        // Calculate the energy slope.
        return slopeParamE + slopeParamR * energySlopeParamF;
    }
    
    /**
     * Calculates the value used by the energy slope cut. This version
     * is superseded by the <code>getValueEnergySlope</code>, which
     * more accurately mirrors the hardware behavior.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @param energySlopeParamF - The value of the variable F in the
     * energy slope equation E_low + R_min * F.
     * @return Returns the cut value.
     */
    @Deprecated
    private static double getValueEnergySlopeLegacy(double energy[], double x[], double y[], double energySlopeParamF) {
    	// Determine which cluster is the lower-energy cluster.
    	int lei = energy[0] < energy[1] ? 0 : 1;
    	
        // E + R*F
        // Get the low energy cluster energy.
        double slopeParamE = energy[lei];
        
        // Get the low energy cluster radial distance.
        double slopeParamR = Math.sqrt((x[lei] * x[lei]) + (y[lei] * y[lei]));
        
        // Calculate the energy slope.
        return slopeParamE + slopeParamR * energySlopeParamF;
    }
    
    /**
     * Calculates the value used by the energy sum cut.
     * @param energy - A two-dimensional array consisting of the first
     * and second clusters' energies.
     * @return Returns the sum of the cluster energies.
     */
    private static double getValueEnergySum(double[] energy) {
        return energy[0] + energy[1];
    }
    
    /**
     * Calculates the value used by the time coincidence cut.
     * @param time - A two-dimensional array consisting of the first
     * and second clusters' times.
     * @return Returns the absolute difference between the times of
     * the two clusters.
     */
    private static double getValueTimeCoincidence(double[] time) {
    	return Math.abs(time[0] - time[1]);
    }
    
    /**
     * Checks if a coplanarity angle is within threshold.
     * @param coplanarityAngle - The cluster coplanarity angle.
     * @return Returns <code>true</code> if the angle passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairCoplanarityCut(double coplanarityAngle) {
        return (coplanarityAngle <= cuts.get(PAIR_COPLANARITY_HIGH));
    }
    
    /**
     * Checks if the energy difference between the clusters making up
     * a cluster pair is below an energy difference threshold.
     * @param energyDifference - The absolute value of the difference
     * of the energies of the cluster pair.
     * @return Returns <code>true</code> if the energy difference passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairEnergyDifferenceCut(double energyDifference) {
        return (energyDifference <= cuts.get(PAIR_ENERGY_DIFFERENCE_HIGH));
    }
    
    /**
     * Checks that the energy slope value is above threshold.
     * @param energySlope - The energy slope value.
     * @return Returns <code>true</code> if the energy slope passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairEnergySlopeCut(double energySlope) {
        return (energySlope >= cuts.get(PAIR_ENERGY_SLOPE_LOW));
    }
    
    /**
     * Checks if the sum of the energies of clusters making up a cluster
     * pair is within an energy sum threshold.
     * @param energySum - The sum of the cluster energies.
     * @return Returns <code>true</code> if the energy sum passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairEnergySumCut(double energySum) {
        return pairEnergySumCutHigh(energySum) && pairEnergySumCutLow(energySum);
    }
    
    /**
     * Checks if the sum of the energies of clusters making up a cluster
     * pair is below the pair energy sum upper bound cut.
     * @param energySum - The sum of the cluster energies.
     * @return Returns <code>true</code> if the energy sum passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairEnergySumCutHigh(double energySum) {
        return (energySum <= cuts.get(PAIR_ENERGY_SUM_HIGH));
    }
    
    /**
     * Checks if the sum of the energies of clusters making up a cluster
     * pair is above the pair energy sum lower bound cut.
     * @param energySum - The sum of the cluster energies.
     * @return Returns <code>true</code> if the energy sum passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairEnergySumCutLow(double energySum) {
        return (energySum >= cuts.get(PAIR_ENERGY_SUM_LOW));
    }
    
    /**
     * Checks if the absolute difference between the times between
     * two clusters is below the time coincidence cut.
     * @param timeDifference - The absolute difference in time between
     * the clusters.
     * @return <code>true</code> if the energy sum passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairTimeCoincidenceCut(double timeDifference) {
    	return (timeDifference <= cuts.get(PAIR_TIME_COINCIDENCE));
    }
    
    /**
     * An array of the form <code>position[iy][ix]</code> that contains
     * the hardware SSP position mappings for each crystal. Note that
     * ix in the array goes from -22 (representing ix = 23) up to 25
     * (representing ix = 23) and uses array index x = 0 as a valid
     * parameter, while ix skips zero.
     */
	private static final double[][][] position = {
		{	{ -340.003,   97.065,   87.845 }, { -324.283,   97.450,   87.875 }, { -308.648,   97.810,   87.900 },
			{ -293.093,   98.150,   87.920 }, { -277.618,   98.470,   87.940 }, { -262.213,   98.765,   87.965 },
			{ -246.878,   99.040,   87.980 }, { -231.603,   99.290,   87.995 }, { -216.393,   99.520,   88.010 },
			{ -201.228,   99.725,   88.030 }, { -186.118,   99.905,   88.040 }, { -171.058,  100.070,   88.050 },
			{ -156.038,  100.205,   88.055 }, { -141.058,  100.325,   88.070 }, { -126.113,  100.415,   88.075 },
			{ -111.198,  100.485,   88.075 }, {  -96.313,  100.530,   88.080 }, {  -81.453,  100.555,   88.085 },
			{  -66.608,  100.560,   88.085 }, {  -51.788,  100.540,   88.080 }, {  -36.983,  100.490,   88.075 },
			{  -22.183,  100.425,   88.075 }, {   -7.393,  100.335,   88.070 }, {    7.393,  100.335,   88.070 },
			{   22.183,  100.425,   88.075 }, {   36.983,  100.490,   88.075 }, {   51.793,  100.540,   88.080 },
			{   66.613,  100.560,   88.085 }, {   81.453,  100.555,   88.085 }, {   96.313,  100.530,   88.080 },
			{  111.198,  100.485,   88.075 }, {  126.113,  100.415,   88.075 }, {  141.053,  100.325,   88.070 },
			{  156.038,  100.205,   88.055 }, {  171.053,  100.070,   88.050 }, {  186.118,   99.905,   88.040 },
			{  201.228,   99.725,   88.030 }, {  216.388,   99.520,   88.010 }, {  231.608,   99.290,   87.995 },
			{  246.878,   99.040,   87.980 }, {  262.218,   98.765,   87.965 }, {  277.623,   98.470,   87.940 },
			{  293.098,   98.150,   87.920 }, {  308.653,   97.810,   87.900 }, {  324.288,   97.450,   87.875 },
			{  340.008,   97.065,   87.845 }
		},
		{	{ -340.003,   97.040,   72.715 }, { -324.283,   97.420,   72.735 }, { -308.648,   97.785,   72.750 },
			{ -293.093,   98.125,   72.765 }, { -277.618,   98.450,   72.785 }, { -262.213,   98.745,   72.800 },
			{ -246.878,   99.015,   72.815 }, { -231.603,   99.265,   72.825 }, { -216.388,   99.495,   72.840 },
			{ -201.228,   99.700,   72.850 }, { -186.118,   99.885,   72.860 }, { -171.058,  100.045,   72.865 },
			{ -156.033,  100.185,   72.875 }, { -141.053,  100.300,   72.880 }, { -126.108,  100.395,   72.880 },
			{ -111.193,  100.460,   72.890 }, {  -96.308,  100.510,   72.890 }, {  -81.448,  100.535,   72.895 },
			{  -66.608,  100.535,   72.890 }, {  -51.788,  100.510,   72.890 }, {  -36.978,  100.470,   72.890 },
			{  -22.183,  100.405,   72.880 }, {   -7.388,  100.310,   72.880 }, {    7.393,  100.310,   72.880 },
			{   22.188,  100.405,   72.885 }, {   36.983,  100.470,   72.890 }, {   51.793,  100.510,   72.890 },
			{   66.613,  100.535,   72.890 }, {   81.453,  100.535,   72.895 }, {   96.313,  100.510,   72.890 },
			{  111.198,  100.460,   72.890 }, {  126.113,  100.395,   72.880 }, {  141.063,  100.300,   72.880 },
			{  156.043,  100.185,   72.875 }, {  171.063,  100.045,   72.865 }, {  186.123,   99.885,   72.860 },
			{  201.233,   99.700,   72.850 }, {  216.393,   99.495,   72.840 }, {  231.608,   99.265,   72.825 },
			{  246.883,   99.015,   72.815 }, {  262.218,   98.745,   72.800 }, {  277.623,   98.450,   72.785 },
			{  293.098,   98.125,   72.765 }, {  308.653,   97.785,   72.750 }, {  324.288,   97.420,   72.735 },
			{  340.008,   97.040,   72.715 }
		},
		{	{ -340.003,   96.990,   57.600 }, { -324.283,   97.375,   57.610 }, { -308.648,   97.740,   57.625 },
			{ -293.093,   98.080,   57.630 }, { -277.618,   98.395,   57.645 }, { -262.213,   98.700,   57.655 },
			{ -246.873,   98.970,   57.660 }, { -231.603,   99.220,   57.670 }, { -216.383,   99.450,   57.680 },
			{ -201.228,   99.660,   57.685 }, { -186.113,   99.840,   57.695 }, { -171.053,  100.005,   57.695 },
			{ -156.033,  100.140,   57.700 }, { -141.053,  100.255,   57.710 }, { -126.108,  100.345,   57.710 },
			{ -111.193,  100.420,   57.710 }, {  -96.308,  100.465,   57.715 }, {  -81.448,  100.490,   57.715 },
			{  -66.608,  100.490,   57.715 }, {  -51.788,  100.470,   57.710 }, {  -36.978,  100.425,   57.710 },
			{  -22.178,  100.355,   57.710 }, {   -7.388,  100.265,   57.705 }, {    7.398,  100.265,   57.705 },
			{   22.188,  100.355,   57.710 }, {   36.988,  100.425,   57.710 }, {   51.793,  100.470,   57.710 },
			{   66.613,  100.490,   57.715 }, {   81.458,  100.490,   57.715 }, {   96.318,  100.465,   57.715 },
			{  111.198,  100.420,   57.710 }, {  126.118,  100.345,   57.710 }, {  141.063,  100.255,   57.710 },
			{  156.043,  100.140,   57.700 }, {  171.063,  100.005,   57.695 }, {  186.123,   99.840,   57.695 },
			{  201.233,   99.660,   57.685 }, {  216.393,   99.450,   57.680 }, {  231.608,   99.220,   57.670 },
			{  246.883,   98.970,   57.660 }, {  262.218,   98.700,   57.655 }, {  277.623,   98.395,   57.645 },
			{  293.098,   98.080,   57.630 }, {  308.653,   97.740,   57.625 }, {  324.288,   97.375,   57.610 },
			{  340.008,   96.990,   57.600 }
		},
		{	{ -340.003,   96.925,   42.490 }, { -324.283,   97.305,   42.495 }, { -308.648,   97.675,   42.505 },
			{ -293.093,   98.010,   42.510 }, { -277.618,   98.330,   42.510 }, { -262.213,   98.625,   42.515 },
			{ -246.873,   98.900,   42.525 }, { -231.603,   99.155,   42.530 }, { -216.383,   99.385,   42.535 },
			{ -201.223,   99.590,   42.530 }, { -186.113,   99.775,   42.535 }, { -171.048,   99.930,   42.540 },
			{ -156.033,  100.070,   42.545 }, { -141.048,  100.185,   42.545 }, { -126.108,  100.280,   42.550 },
			{ -111.193,  100.350,   42.545 }, {  -96.308,  100.400,   42.545 }, {  -81.448,  100.420,   42.550 },
			{  -66.608,  100.425,   42.550 }, {  -51.788,  100.405,   42.550 }, {  -36.978,  100.355,   42.545 },
			{  -22.178,  100.290,   42.545 }, {   -7.388,  100.200,   42.545 }, {    7.398,  100.200,   42.545 },
			{   22.188,  100.290,   42.545 }, {   36.988,  100.355,   42.545 }, {   51.793,  100.405,   42.550 },
			{   66.613,  100.425,   42.550 }, {   81.458,  100.420,   42.550 }, {   96.318,  100.400,   42.545 },
			{  111.198,  100.350,   42.545 }, {  126.118,  100.280,   42.550 }, {  141.063,  100.185,   42.545 },
			{  156.043,  100.070,   42.545 }, {  171.063,   99.930,   42.540 }, {  186.123,   99.775,   42.535 },
			{  201.233,   99.590,   42.530 }, {  216.393,   99.385,   42.535 }, {  231.608,   99.155,   42.530 },
			{  246.883,   98.900,   42.525 }, {  262.218,   98.625,   42.515 }, {  277.628,   98.330,   42.510 },
			{  293.098,   98.010,   42.510 }, {  308.653,   97.675,   42.505 }, {  324.288,   97.305,   42.495 },
			{  340.008,   96.925,   42.490 }
		},
		{	{ -340.003,   96.830,   27.385 }, { -324.278,   97.215,   27.385 }, { -308.648,   97.575,   27.385 },
			{ -293.093,   97.915,   27.385 }, { -277.613,   98.240,   27.385 }, { -262.213,   98.535,   27.385 },
			{ -246.878,   98.810,   27.385 }, { -231.603,   99.060,   27.385 }, { -216.383,   99.290,   27.385 },
			{ -201.223,   99.495,   27.385 }, { -186.113,   99.680,   27.385 }, { -171.048,   99.840,   27.385 },
			{ -156.033,   99.980,   27.385 }, { -141.048,  100.095,   27.385 }, { -126.103,  100.185,   27.385 },
			{ -111.193,  100.255,   27.385 }, {  -96.303,  100.305,   27.385 }, {  -81.448,  100.330,   27.385 },
			{  -66.608,  100.330,   27.385 }, {  -51.783,  100.310,   27.385 }, {  -36.973,  100.265,   27.385 },
			{  -22.178,  100.200,   27.385 }, {   -7.388,  100.105,   27.385 }, {    7.403,  100.105,   27.385 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{  156.078,   99.980,   27.385 }, {  171.103,   99.840,   27.385 }, {  186.168,   99.680,   27.385 },
			{  201.268,   99.495,   27.385 }, {  216.423,   99.290,   27.385 }, {  231.638,   99.060,   27.385 },
			{  246.913,   98.810,   27.385 }, {  262.248,   98.535,   27.385 }, {  277.658,   98.240,   27.385 },
			{  293.133,   97.920,   27.385 }, {  308.688,   97.575,   27.385 }, {  324.323,   97.215,   27.385 },
			{  340.043,   96.830,   27.385 }
		},
		{	{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }
		},
		{	{ -339.998,   96.840,  -27.330 }, { -324.278,   97.225,  -27.340 }, { -308.643,   97.585,  -27.345 },
			{ -293.093,   97.925,  -27.350 }, { -277.613,   98.245,  -27.360 }, { -262.213,   98.545,  -27.365 },
			{ -246.868,   98.820,  -27.365 }, { -231.598,   99.070,  -27.370 }, { -216.383,   99.300,  -27.375 },
			{ -201.223,   99.505,  -27.380 }, { -186.113,   99.690,  -27.385 }, { -171.048,   99.850,  -27.380 },
			{ -156.028,   99.990,  -27.385 }, { -141.048,  100.100,  -27.390 }, { -126.103,  100.195,  -27.390 },
			{ -111.193,  100.265,  -27.395 }, {  -96.303,  100.315,  -27.395 }, {  -81.443,  100.340,  -27.390 },
			{  -66.603,  100.335,  -27.390 }, {  -51.783,  100.315,  -27.390 }, {  -36.973,  100.275,  -27.395 },
			{  -22.173,  100.205,  -27.390 }, {   -7.383,  100.115,  -27.385 }, {    7.403,  100.115,  -27.385 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 }, {    0.000,    0.000,    0.000 },
			{  156.088,   99.985,  -27.385 }, {  171.103,   99.845,  -27.380 }, {  186.168,   99.680,  -27.385 },
			{  201.268,   99.495,  -27.380 }, {  216.428,   99.290,  -27.375 }, {  231.643,   99.060,  -27.370 },
			{  246.913,   98.810,  -27.365 }, {  262.258,   98.535,  -27.365 }, {  277.658,   98.240,  -27.360 },
			{  293.138,   97.925,  -27.350 }, {  308.688,   97.580,  -27.345 }, {  324.323,   97.215,  -27.340 },
			{  340.043,   96.835,  -27.330 }
		},
		{	{ -339.998,   96.930,  -42.435 }, { -324.278,   97.315,  -42.445 }, { -308.648,   97.680,  -42.455 },
			{ -293.093,   98.015,  -42.470 }, { -277.613,   98.340,  -42.480 }, { -262.208,   98.635,  -42.490 },
			{ -246.873,   98.910,  -42.500 }, { -231.593,   99.160,  -42.510 }, { -216.383,   99.390,  -42.515 },
			{ -201.223,   99.595,  -42.525 }, { -186.113,   99.780,  -42.525 }, { -171.048,   99.940,  -42.535 },
			{ -156.028,  100.080,  -42.540 }, { -141.048,  100.195,  -42.540 }, { -126.103,  100.290,  -42.545 },
			{ -111.193,  100.355,  -42.550 }, {  -96.303,  100.405,  -42.550 }, {  -81.443,  100.430,  -42.550 },
			{  -66.608,  100.430,  -42.550 }, {  -51.783,  100.405,  -42.550 }, {  -36.973,  100.365,  -42.550 },
			{  -22.178,  100.295,  -42.545 }, {   -7.388,  100.205,  -42.545 }, {    7.403,  100.205,  -42.545 },
			{   22.193,  100.295,  -42.545 }, {   36.988,  100.365,  -42.550 }, {   51.798,  100.405,  -42.550 },
			{   66.623,  100.430,  -42.550 }, {   81.458,  100.430,  -42.550 }, {   96.318,  100.405,  -42.550 },
			{  111.208,  100.355,  -42.550 }, {  126.118,  100.290,  -42.545 }, {  141.063,  100.195,  -42.540 },
			{  156.043,  100.080,  -42.540 }, {  171.063,   99.940,  -42.535 }, {  186.128,   99.780,  -42.525 },
			{  201.238,   99.595,  -42.525 }, {  216.398,   99.390,  -42.515 }, {  231.613,   99.160,  -42.510 },
			{  246.888,   98.910,  -42.500 }, {  262.223,   98.635,  -42.490 }, {  277.628,   98.340,  -42.480 },
			{  293.108,   98.015,  -42.470 }, {  308.663,   97.680,  -42.455 }, {  324.293,   97.315,  -42.445 },
			{  340.013,   96.930,  -42.435 }
		},
		{	{ -339.998,   97.000,  -57.540 }, { -324.278,   97.385,  -57.560 }, { -308.648,   97.745,  -57.575 },
			{ -293.093,   98.090,  -57.595 }, { -277.613,   98.410,  -57.610 }, { -262.208,   98.705,  -57.625 },
			{ -246.873,   98.975,  -57.640 }, { -231.593,   99.225,  -57.655 }, { -216.383,   99.455,  -57.665 },
			{ -201.223,   99.665,  -57.675 }, { -186.113,   99.845,  -57.685 }, { -171.048,  100.010,  -57.690 },
			{ -156.028,  100.145,  -57.700 }, { -141.048,  100.265,  -57.705 }, { -126.103,  100.355,  -57.710 },
			{ -111.193,  100.425,  -57.710 }, {  -96.303,  100.475,  -57.720 }, {  -81.443,  100.495,  -57.715 },
			{  -66.608,  100.500,  -57.720 }, {  -51.783,  100.480,  -57.715 }, {  -36.973,  100.430,  -57.710 },
			{  -22.178,  100.365,  -57.710 }, {   -7.388,  100.275,  -57.705 }, {    7.403,  100.275,  -57.705 },
			{   22.193,  100.365,  -57.710 }, {   36.988,  100.430,  -57.710 }, {   51.798,  100.480,  -57.715 },
			{   66.623,  100.500,  -57.720 }, {   81.458,  100.495,  -57.715 }, {   96.318,  100.475,  -57.720 },
			{  111.208,  100.425,  -57.710 }, {  126.118,  100.355,  -57.710 }, {  141.063,  100.265,  -57.705 },
			{  156.043,  100.145,  -57.700 }, {  171.063,  100.010,  -57.690 }, {  186.128,   99.845,  -57.685 },
			{  201.238,   99.665,  -57.675 }, {  216.398,   99.455,  -57.665 }, {  231.613,   99.225,  -57.655 },
			{  246.888,   98.975,  -57.640 }, {  262.223,   98.705,  -57.625 }, {  277.628,   98.410,  -57.610 },
			{  293.108,   98.090,  -57.595 }, {  308.663,   97.745,  -57.575 }, {  324.293,   97.385,  -57.560 },
			{  340.013,   97.000,  -57.540 }
		},
		{	{ -339.998,   97.045,  -72.655 }, { -324.278,   97.435,  -72.680 }, { -308.648,   97.795,  -72.710 },
			{ -293.093,   98.135,  -72.730 }, { -277.613,   98.455,  -72.750 }, { -262.208,   98.750,  -72.775 },
			{ -246.873,   99.020,  -72.795 }, { -231.593,   99.280,  -72.810 }, { -216.383,   99.505,  -72.820 },
			{ -201.223,   99.710,  -72.840 }, { -186.113,   99.895,  -72.850 }, { -171.048,  100.055,  -72.860 },
			{ -156.028,  100.190,  -72.870 }, { -141.048,  100.305,  -72.880 }, { -126.103,  100.400,  -72.885 },
			{ -111.193,  100.470,  -72.890 }, {  -96.303,  100.520,  -72.890 }, {  -81.443,  100.540,  -72.895 },
			{  -66.608,  100.540,  -72.895 }, {  -51.783,  100.520,  -72.895 }, {  -36.973,  100.480,  -72.890 },
			{  -22.178,  100.405,  -72.885 }, {   -7.388,  100.320,  -72.880 }, {    7.403,  100.320,  -72.880 },
			{   22.193,  100.405,  -72.885 }, {   36.988,  100.480,  -72.890 }, {   51.798,  100.520,  -72.895 },
			{   66.623,  100.540,  -72.895 }, {   81.458,  100.540,  -72.895 }, {   96.318,  100.520,  -72.890 },
			{  111.208,  100.470,  -72.890 }, {  126.118,  100.400,  -72.885 }, {  141.063,  100.305,  -72.880 },
			{  156.043,  100.190,  -72.870 }, {  171.063,  100.055,  -72.860 }, {  186.128,   99.895,  -72.850 },
			{  201.238,   99.710,  -72.840 }, {  216.398,   99.505,  -72.820 }, {  231.613,   99.280,  -72.810 },
			{  246.888,   99.020,  -72.795 }, {  262.223,   98.750,  -72.775 }, {  277.628,   98.455,  -72.750 },
			{  293.108,   98.135,  -72.730 }, {  308.663,   97.795,  -72.710 }, {  324.293,   97.435,  -72.680 },
			{  340.013,   97.045,  -72.655 }
		},
		{	{ -339.998,   97.070,  -87.790 }, { -324.278,   97.460,  -87.820 }, { -308.648,   97.820,  -87.850 },
			{ -293.093,   98.160,  -87.885 }, { -277.613,   98.480,  -87.910 }, { -262.208,   98.775,  -87.935 },
			{ -246.873,   99.050,  -87.960 }, { -231.593,   99.300,  -87.980 }, { -216.383,   99.530,  -88.000 },
			{ -201.223,   99.735,  -88.015 }, { -186.113,   99.920,  -88.030 }, { -171.048,  100.080,  -88.045 },
			{ -156.028,  100.215,  -88.055 }, { -141.048,  100.335,  -88.065 }, { -126.103,  100.420,  -88.070 },
			{ -111.193,  100.490,  -88.075 }, {  -96.303,  100.540,  -88.085 }, {  -81.443,  100.565,  -88.085 },
			{  -66.608,  100.560,  -88.085 }, {  -51.783,  100.540,  -88.085 }, {  -36.973,  100.500,  -88.080 },
			{  -22.178,  100.430,  -88.075 }, {   -7.388,  100.340,  -88.065 }, {    7.403,  100.340,  -88.070 },
			{   22.193,  100.430,  -88.075 }, {   36.988,  100.500,  -88.080 }, {   51.798,  100.540,  -88.085 },
			{   66.623,  100.560,  -88.085 }, {   81.458,  100.565,  -88.085 }, {   96.318,  100.540,  -88.085 },
			{  111.208,  100.490,  -88.075 }, {  126.118,  100.420,  -88.070 }, {  141.063,  100.335,  -88.065 },
			{  156.043,  100.215,  -88.055 }, {  171.063,  100.080,  -88.045 }, {  186.128,   99.915,  -88.030 },
			{  201.238,   99.735,  -88.015 }, {  216.398,   99.530,  -88.000 }, {  231.613,   99.300,  -87.980 },
			{  246.888,   99.050,  -87.960 }, {  262.223,   98.775,  -87.935 }, {  277.628,   98.480,  -87.910 },
			{  293.108,   98.160,  -87.885 }, {  308.663,   97.820,  -87.850 }, {  324.293,   97.460,  -87.820 },
			{  340.013,   97.070,  -87.790 }
		}
	};
}
