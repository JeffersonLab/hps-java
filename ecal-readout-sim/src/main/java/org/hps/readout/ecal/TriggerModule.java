package org.hps.readout.ecal;

import java.util.HashMap;
import java.util.Map;

import org.hps.readout.ecal.triggerbank.SSPCluster;
import org.lcsim.event.Cluster;

/**
 * Class <code>SSPTriggerModule</code> handles trigger cuts. By default,
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
	// ECal mid-plane, defined by photon beam position (30.52 mrad) at ECal face (z=1393 mm)
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
    	double y[] = { getClusterY(clusterPair[0]), getClusterY(clusterPair[1]) };
    	
    	// Return the calculated value.
    	return getValueCoplanarity(x, y);
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
    	double y[] = { getClusterY(clusterPair[0]), getClusterY(clusterPair[1]) };
    	
    	// Perform the calculation.
    	return getValueEnergySlope(energy, x, y, energySlopeParamF);
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
        return (seedEnergy < cuts.get(CLUSTER_SEED_ENERGY_HIGH));
    }
    
    /**
     * Checks whether the argument energy falls above the cluster seed
     * energy lower bound cut.
     * @param seedEnergy - The energy of the cluster seed.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterSeedEnergyCutLow(double seedEnergy) {
        return (seedEnergy > cuts.get(CLUSTER_SEED_ENERGY_LOW));
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
        return (clusterEnergy < cuts.get(CLUSTER_TOTAL_ENERGY_HIGH));
    }
    
    /**
     * Checks whether the argument energy falls above the cluster total
     * energy lower bound cut.
     * @param clusterEnergy - The energy of the entire cluster.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterTotalEnergyCutLow(double clusterEnergy) {
        return (clusterEnergy > cuts.get(CLUSTER_TOTAL_ENERGY_LOW));
    }
    
    /**
     * Calculates the distance between the origin and a cluster.
     * @param x - The cluster's x-position.
     * @param y - The cluster's y-position.
     * @return Returns displacement of the cluster.
     */
    private static double getClusterDistance(double x, double y) {
        return Math.hypot(x - ORIGIN_X, y);
    }
    
    /**
     * Gets the x-position of a cluster.
     * @param cluster - The cluster of which to get the x-position.
     * @return Returns the cluster x-position.
     */
    private static double getClusterX(Cluster cluster) {
    	return cluster.getCalorimeterHits().get(0).getPosition()[0];
    }
    
    /**
     * Gets the y-position of a cluster.
     * @param cluster - The cluster of which to get the y-position.
     * @return Returns the cluster y-position.
     */
    private static double getClusterY(Cluster cluster) {
    	return cluster.getCalorimeterHits().get(0).getPosition()[1];
    }
    
    /**
     * Calculates the value used by the coplanarity cut.
     * @param x - A two-dimensional array consisting of the first and
     * second clusters' x-positions.
     * @param y - A two-dimensional array consisting of the first and
     * second clusters' y-positions.
     * @return Returns the cluster pair's coplanarity.
     */
    private static double getValueCoplanarity(double[] x, double y[]) {
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
    private static double getValueEnergySlope(double energy[], double x[], double y[], double energySlopeParamF) {
    	// Determine which cluster is the lower-energy cluster.
    	int lei = energy[0] < energy[1] ? 0 : 1;
    	
        // E + R*F
        // Get the low energy cluster energy.
        double slopeParamE = energy[lei];
        
        // Get the low energy cluster radial distance.
        double slopeParamR = getClusterDistance(x[lei], y[lei]);
        
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
     * Checks if a coplanarity angle is within threshold.
     * @param coplanarityAngle - The cluster coplanarity angle.
     * @return Returns <code>true</code> if the angle passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairCoplanarityCut(double coplanarityAngle) {
        return (coplanarityAngle < cuts.get(PAIR_COPLANARITY_HIGH));
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
        return (energyDifference < cuts.get(PAIR_ENERGY_DIFFERENCE_HIGH));
    }
    
    /**
     * Checks that the energy slope value is above threshold.
     * @param energySlope - The energy slope value.
     * @return Returns <code>true</code> if the energy slope passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairEnergySlopeCut(double energySlope) {
        return (energySlope > cuts.get(PAIR_ENERGY_SLOPE_LOW));
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
        return (energySum < cuts.get(PAIR_ENERGY_SUM_HIGH));
    }
    
    /**
     * Checks if the sum of the energies of clusters making up a cluster
     * pair is above the pair energy sum lower bound cut.
     * @param energySum - The sum of the cluster energies.
     * @return Returns <code>true</code> if the energy sum passes
     * the cut and <code>false</code> if it does not.
     */
    private boolean pairEnergySumCutLow(double energySum) {
        return (energySum > cuts.get(PAIR_ENERGY_SUM_LOW));
    }
}
