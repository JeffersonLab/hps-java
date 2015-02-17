package org.hps.readout.ecal;

import java.awt.Point;
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
    /** The maximum amount of time by which two clusters are allowed
     * to be separated. */
    public static final String PAIR_TIME_COINCIDENCE = "pairTimeCoincidence";
    
    // Trigger cut settings map.
    private final Map<String, Double> cuts = new HashMap<String, Double>(11);
    
    // Crystal x/y-index to location map.
    private static final Map<Point, double[]> locationMap = new HashMap<Point, double[]>(442);
    
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
     * Calculates the value used by the coplanarity cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    public static double getValueCoplanarity(SSPCluster[] clusterPair) {
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
    	return getCrystalPosition(cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
    			cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"))[0];
    }
    
    /**
     * Gets the x-position of a cluster.
     * @param cluster - The cluster of which to get the x-position.
     * @return Returns the cluster x-position.
     */
    private static double getClusterX(SSPCluster cluster) {
    	return getCrystalPosition(cluster.getXIndex(), cluster.getYIndex())[0];
    }
    
    /**
     * Gets the y-position of a cluster.
     * @param cluster - The cluster of which to get the y-position.
     * @return Returns the cluster y-position.
     */
    private static double getClusterY(Cluster cluster) {
      	return getCrystalPosition(cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
    			cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"))[1];
    }
    
    /**
     * Gets the y-position of a cluster.
     * @param cluster - The cluster of which to get the y-position.
     * @return Returns the cluster y-position.
     */
    private static double getClusterY(SSPCluster cluster) {
    	return getCrystalPosition(cluster.getXIndex(), cluster.getYIndex())[1];
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
     * Gets the x/y/z location of a crystal from its x/y-indices.
     * @param ix - The crystal x-index.
     * @param iy - The crystal y-index.
     * @return Returns the crystal's x/y/z position in millimeters as
     * a size-three array of <code>double</code> primitives. Index 0
     * corresponds to x, 1 to y, and 2 to z.
     * @throws IllegalArgumentException Occurs if the values given for
     * <code>ix</code> and <code>iy</code> do not refer to a crystal.
     */
    private static final double[] getCrystalPosition(int ix, int iy) throws IllegalArgumentException {
    	// Make sure that the location map is initialized.
    	if(locationMap.isEmpty()) { initializeLocationMap(); }
    	
    	// Get the mapped location.
    	double[] location = locationMap.get(new Point(ix, iy));
    	
    	// If it is null, produce an error.
    	if(location == null) {
    		throw new IllegalArgumentException(
    				String.format("Crystal indices (%3d, %3d) do not map to a valid location.", ix, iy)
    		);
    	}
    	
    	// Otherwise, return the location.
    	return location;
    }
    
    /**
     * Sets all of the mappings for the crystal index to position map.
     */
    private static final void initializeLocationMap() {
    	locationMap.put(new Point( 23,   5), new double[] { -340.003,   97.065,   87.845 });
    	locationMap.put(new Point( 22,   5), new double[] { -324.283,   97.450,   87.875 });
    	locationMap.put(new Point( 21,   5), new double[] { -308.648,   97.810,   87.900 });
    	locationMap.put(new Point( 20,   5), new double[] { -293.093,   98.150,   87.920 });
    	locationMap.put(new Point( 19,   5), new double[] { -277.618,   98.470,   87.940 });
    	locationMap.put(new Point( 18,   5), new double[] { -262.213,   98.765,   87.965 });
    	locationMap.put(new Point( 17,   5), new double[] { -246.878,   99.040,   87.980 });
    	locationMap.put(new Point( 16,   5), new double[] { -231.603,   99.290,   87.995 });
    	locationMap.put(new Point( 15,   5), new double[] { -216.393,   99.520,   88.010 });
    	locationMap.put(new Point( 14,   5), new double[] { -201.228,   99.725,   88.030 });
    	locationMap.put(new Point( 13,   5), new double[] { -186.118,   99.905,   88.040 });
    	locationMap.put(new Point( 12,   5), new double[] { -171.058,  100.070,   88.050 });
    	locationMap.put(new Point( 11,   5), new double[] { -156.038,  100.205,   88.055 });
    	locationMap.put(new Point( 10,   5), new double[] { -141.058,  100.325,   88.070 });
    	locationMap.put(new Point(  9,   5), new double[] { -126.113,  100.415,   88.075 });
    	locationMap.put(new Point(  8,   5), new double[] { -111.198,  100.485,   88.075 });
    	locationMap.put(new Point(  7,   5), new double[] {  -96.313,  100.530,   88.080 });
    	locationMap.put(new Point(  6,   5), new double[] {  -81.453,  100.555,   88.085 });
    	locationMap.put(new Point(  5,   5), new double[] {  -66.608,  100.560,   88.085 });
    	locationMap.put(new Point(  4,   5), new double[] {  -51.788,  100.540,   88.080 });
    	locationMap.put(new Point(  3,   5), new double[] {  -36.983,  100.490,   88.075 });
    	locationMap.put(new Point(  2,   5), new double[] {  -22.183,  100.425,   88.075 });
    	locationMap.put(new Point(  1,   5), new double[] {   -7.393,  100.335,   88.070 });
    	locationMap.put(new Point( -1,   5), new double[] {    7.393,  100.335,   88.070 });
    	locationMap.put(new Point( -2,   5), new double[] {   22.183,  100.425,   88.075 });
    	locationMap.put(new Point( -3,   5), new double[] {   36.983,  100.490,   88.075 });
    	locationMap.put(new Point( -4,   5), new double[] {   51.793,  100.540,   88.080 });
    	locationMap.put(new Point( -5,   5), new double[] {   66.613,  100.560,   88.085 });
    	locationMap.put(new Point( -6,   5), new double[] {   81.453,  100.555,   88.085 });
    	locationMap.put(new Point( -7,   5), new double[] {   96.313,  100.530,   88.080 });
    	locationMap.put(new Point( -8,   5), new double[] {  111.198,  100.485,   88.075 });
    	locationMap.put(new Point( -9,   5), new double[] {  126.113,  100.415,   88.075 });
    	locationMap.put(new Point(-10,   5), new double[] {  141.053,  100.325,   88.070 });
    	locationMap.put(new Point(-11,   5), new double[] {  156.038,  100.205,   88.055 });
    	locationMap.put(new Point(-12,   5), new double[] {  171.053,  100.070,   88.050 });
    	locationMap.put(new Point(-13,   5), new double[] {  186.118,   99.905,   88.040 });
    	locationMap.put(new Point(-14,   5), new double[] {  201.228,   99.725,   88.030 });
    	locationMap.put(new Point(-15,   5), new double[] {  216.388,   99.520,   88.010 });
    	locationMap.put(new Point(-16,   5), new double[] {  231.608,   99.290,   87.995 });
    	locationMap.put(new Point(-17,   5), new double[] {  246.878,   99.040,   87.980 });
    	locationMap.put(new Point(-18,   5), new double[] {  262.218,   98.765,   87.965 });
    	locationMap.put(new Point(-19,   5), new double[] {  277.623,   98.470,   87.940 });
    	locationMap.put(new Point(-20,   5), new double[] {  293.098,   98.150,   87.920 });
    	locationMap.put(new Point(-21,   5), new double[] {  308.653,   97.810,   87.900 });
    	locationMap.put(new Point(-22,   5), new double[] {  324.288,   97.450,   87.875 });
    	locationMap.put(new Point(-23,   5), new double[] {  340.008,   97.065,   87.845 });
    	locationMap.put(new Point( 23,   4), new double[] { -340.003,   97.040,   72.715 });
    	locationMap.put(new Point( 22,   4), new double[] { -324.283,   97.420,   72.735 });
    	locationMap.put(new Point( 21,   4), new double[] { -308.648,   97.785,   72.750 });
    	locationMap.put(new Point( 20,   4), new double[] { -293.093,   98.125,   72.765 });
    	locationMap.put(new Point( 19,   4), new double[] { -277.618,   98.450,   72.785 });
    	locationMap.put(new Point( 18,   4), new double[] { -262.213,   98.745,   72.800 });
    	locationMap.put(new Point( 17,   4), new double[] { -246.878,   99.015,   72.815 });
    	locationMap.put(new Point( 16,   4), new double[] { -231.603,   99.265,   72.825 });
    	locationMap.put(new Point( 15,   4), new double[] { -216.388,   99.495,   72.840 });
    	locationMap.put(new Point( 14,   4), new double[] { -201.228,   99.700,   72.850 });
    	locationMap.put(new Point( 13,   4), new double[] { -186.118,   99.885,   72.860 });
    	locationMap.put(new Point( 12,   4), new double[] { -171.058,  100.045,   72.865 });
    	locationMap.put(new Point( 11,   4), new double[] { -156.033,  100.185,   72.875 });
    	locationMap.put(new Point( 10,   4), new double[] { -141.053,  100.300,   72.880 });
    	locationMap.put(new Point(  9,   4), new double[] { -126.108,  100.395,   72.880 });
    	locationMap.put(new Point(  8,   4), new double[] { -111.193,  100.460,   72.890 });
    	locationMap.put(new Point(  7,   4), new double[] {  -96.308,  100.510,   72.890 });
    	locationMap.put(new Point(  6,   4), new double[] {  -81.448,  100.535,   72.895 });
    	locationMap.put(new Point(  5,   4), new double[] {  -66.608,  100.535,   72.890 });
    	locationMap.put(new Point(  4,   4), new double[] {  -51.788,  100.510,   72.890 });
    	locationMap.put(new Point(  3,   4), new double[] {  -36.978,  100.470,   72.890 });
    	locationMap.put(new Point(  2,   4), new double[] {  -22.183,  100.405,   72.880 });
    	locationMap.put(new Point(  1,   4), new double[] {   -7.388,  100.310,   72.880 });
    	locationMap.put(new Point( -1,   4), new double[] {    7.393,  100.310,   72.880 });
    	locationMap.put(new Point( -2,   4), new double[] {   22.188,  100.405,   72.885 });
    	locationMap.put(new Point( -3,   4), new double[] {   36.983,  100.470,   72.890 });
    	locationMap.put(new Point( -4,   4), new double[] {   51.793,  100.510,   72.890 });
    	locationMap.put(new Point( -5,   4), new double[] {   66.613,  100.535,   72.890 });
    	locationMap.put(new Point( -6,   4), new double[] {   81.453,  100.535,   72.895 });
    	locationMap.put(new Point( -7,   4), new double[] {   96.313,  100.510,   72.890 });
    	locationMap.put(new Point( -8,   4), new double[] {  111.198,  100.460,   72.890 });
    	locationMap.put(new Point( -9,   4), new double[] {  126.113,  100.395,   72.880 });
    	locationMap.put(new Point(-10,   4), new double[] {  141.063,  100.300,   72.880 });
    	locationMap.put(new Point(-11,   4), new double[] {  156.043,  100.185,   72.875 });
    	locationMap.put(new Point(-12,   4), new double[] {  171.063,  100.045,   72.865 });
    	locationMap.put(new Point(-13,   4), new double[] {  186.123,   99.885,   72.860 });
    	locationMap.put(new Point(-14,   4), new double[] {  201.233,   99.700,   72.850 });
    	locationMap.put(new Point(-15,   4), new double[] {  216.393,   99.495,   72.840 });
    	locationMap.put(new Point(-16,   4), new double[] {  231.608,   99.265,   72.825 });
    	locationMap.put(new Point(-17,   4), new double[] {  246.883,   99.015,   72.815 });
    	locationMap.put(new Point(-18,   4), new double[] {  262.218,   98.745,   72.800 });
    	locationMap.put(new Point(-19,   4), new double[] {  277.623,   98.450,   72.785 });
    	locationMap.put(new Point(-20,   4), new double[] {  293.098,   98.125,   72.765 });
    	locationMap.put(new Point(-21,   4), new double[] {  308.653,   97.785,   72.750 });
    	locationMap.put(new Point(-22,   4), new double[] {  324.288,   97.420,   72.735 });
    	locationMap.put(new Point(-23,   4), new double[] {  340.008,   97.040,   72.715 });
    	locationMap.put(new Point( 23,   3), new double[] { -340.003,   96.990,   57.600 });
    	locationMap.put(new Point( 22,   3), new double[] { -324.283,   97.375,   57.610 });
    	locationMap.put(new Point( 21,   3), new double[] { -308.648,   97.740,   57.625 });
    	locationMap.put(new Point( 20,   3), new double[] { -293.093,   98.080,   57.630 });
    	locationMap.put(new Point( 19,   3), new double[] { -277.618,   98.395,   57.645 });
    	locationMap.put(new Point( 18,   3), new double[] { -262.213,   98.700,   57.655 });
    	locationMap.put(new Point( 17,   3), new double[] { -246.873,   98.970,   57.660 });
    	locationMap.put(new Point( 16,   3), new double[] { -231.603,   99.220,   57.670 });
    	locationMap.put(new Point( 15,   3), new double[] { -216.383,   99.450,   57.680 });
    	locationMap.put(new Point( 14,   3), new double[] { -201.228,   99.660,   57.685 });
    	locationMap.put(new Point( 13,   3), new double[] { -186.113,   99.840,   57.695 });
    	locationMap.put(new Point( 12,   3), new double[] { -171.053,  100.005,   57.695 });
    	locationMap.put(new Point( 11,   3), new double[] { -156.033,  100.140,   57.700 });
    	locationMap.put(new Point( 10,   3), new double[] { -141.053,  100.255,   57.710 });
    	locationMap.put(new Point(  9,   3), new double[] { -126.108,  100.345,   57.710 });
    	locationMap.put(new Point(  8,   3), new double[] { -111.193,  100.420,   57.710 });
    	locationMap.put(new Point(  7,   3), new double[] {  -96.308,  100.465,   57.715 });
    	locationMap.put(new Point(  6,   3), new double[] {  -81.448,  100.490,   57.715 });
    	locationMap.put(new Point(  5,   3), new double[] {  -66.608,  100.490,   57.715 });
    	locationMap.put(new Point(  4,   3), new double[] {  -51.788,  100.470,   57.710 });
    	locationMap.put(new Point(  3,   3), new double[] {  -36.978,  100.425,   57.710 });
    	locationMap.put(new Point(  2,   3), new double[] {  -22.178,  100.355,   57.710 });
    	locationMap.put(new Point(  1,   3), new double[] {   -7.388,  100.265,   57.705 });
    	locationMap.put(new Point( -1,   3), new double[] {    7.398,  100.265,   57.705 });
    	locationMap.put(new Point( -2,   3), new double[] {   22.188,  100.355,   57.710 });
    	locationMap.put(new Point( -3,   3), new double[] {   36.988,  100.425,   57.710 });
    	locationMap.put(new Point( -4,   3), new double[] {   51.793,  100.470,   57.710 });
    	locationMap.put(new Point( -5,   3), new double[] {   66.613,  100.490,   57.715 });
    	locationMap.put(new Point( -6,   3), new double[] {   81.458,  100.490,   57.715 });
    	locationMap.put(new Point( -7,   3), new double[] {   96.318,  100.465,   57.715 });
    	locationMap.put(new Point( -8,   3), new double[] {  111.198,  100.420,   57.710 });
    	locationMap.put(new Point( -9,   3), new double[] {  126.118,  100.345,   57.710 });
    	locationMap.put(new Point(-10,   3), new double[] {  141.063,  100.255,   57.710 });
    	locationMap.put(new Point(-11,   3), new double[] {  156.043,  100.140,   57.700 });
    	locationMap.put(new Point(-12,   3), new double[] {  171.063,  100.005,   57.695 });
    	locationMap.put(new Point(-13,   3), new double[] {  186.123,   99.840,   57.695 });
    	locationMap.put(new Point(-14,   3), new double[] {  201.233,   99.660,   57.685 });
    	locationMap.put(new Point(-15,   3), new double[] {  216.393,   99.450,   57.680 });
    	locationMap.put(new Point(-16,   3), new double[] {  231.608,   99.220,   57.670 });
    	locationMap.put(new Point(-17,   3), new double[] {  246.883,   98.970,   57.660 });
    	locationMap.put(new Point(-18,   3), new double[] {  262.218,   98.700,   57.655 });
    	locationMap.put(new Point(-19,   3), new double[] {  277.623,   98.395,   57.645 });
    	locationMap.put(new Point(-20,   3), new double[] {  293.098,   98.080,   57.630 });
    	locationMap.put(new Point(-21,   3), new double[] {  308.653,   97.740,   57.625 });
    	locationMap.put(new Point(-22,   3), new double[] {  324.288,   97.375,   57.610 });
    	locationMap.put(new Point(-23,   3), new double[] {  340.008,   96.990,   57.600 });
    	locationMap.put(new Point( 23,   2), new double[] { -340.003,   96.925,   42.490 });
    	locationMap.put(new Point( 22,   2), new double[] { -324.283,   97.305,   42.495 });
    	locationMap.put(new Point( 21,   2), new double[] { -308.648,   97.675,   42.505 });
    	locationMap.put(new Point( 20,   2), new double[] { -293.093,   98.010,   42.510 });
    	locationMap.put(new Point( 19,   2), new double[] { -277.618,   98.330,   42.510 });
    	locationMap.put(new Point( 18,   2), new double[] { -262.213,   98.625,   42.515 });
    	locationMap.put(new Point( 17,   2), new double[] { -246.873,   98.900,   42.525 });
    	locationMap.put(new Point( 16,   2), new double[] { -231.603,   99.155,   42.530 });
    	locationMap.put(new Point( 15,   2), new double[] { -216.383,   99.385,   42.535 });
    	locationMap.put(new Point( 14,   2), new double[] { -201.223,   99.590,   42.530 });
    	locationMap.put(new Point( 13,   2), new double[] { -186.113,   99.775,   42.535 });
    	locationMap.put(new Point( 12,   2), new double[] { -171.048,   99.930,   42.540 });
    	locationMap.put(new Point( 11,   2), new double[] { -156.033,  100.070,   42.545 });
    	locationMap.put(new Point( 10,   2), new double[] { -141.048,  100.185,   42.545 });
    	locationMap.put(new Point(  9,   2), new double[] { -126.108,  100.280,   42.550 });
    	locationMap.put(new Point(  8,   2), new double[] { -111.193,  100.350,   42.545 });
    	locationMap.put(new Point(  7,   2), new double[] {  -96.308,  100.400,   42.545 });
    	locationMap.put(new Point(  6,   2), new double[] {  -81.448,  100.420,   42.550 });
    	locationMap.put(new Point(  5,   2), new double[] {  -66.608,  100.425,   42.550 });
    	locationMap.put(new Point(  4,   2), new double[] {  -51.788,  100.405,   42.550 });
    	locationMap.put(new Point(  3,   2), new double[] {  -36.978,  100.355,   42.545 });
    	locationMap.put(new Point(  2,   2), new double[] {  -22.178,  100.290,   42.545 });
    	locationMap.put(new Point(  1,   2), new double[] {   -7.388,  100.200,   42.545 });
    	locationMap.put(new Point( -1,   2), new double[] {    7.398,  100.200,   42.545 });
    	locationMap.put(new Point( -2,   2), new double[] {   22.188,  100.290,   42.545 });
    	locationMap.put(new Point( -3,   2), new double[] {   36.988,  100.355,   42.545 });
    	locationMap.put(new Point( -4,   2), new double[] {   51.793,  100.405,   42.550 });
    	locationMap.put(new Point( -5,   2), new double[] {   66.613,  100.425,   42.550 });
    	locationMap.put(new Point( -6,   2), new double[] {   81.458,  100.420,   42.550 });
    	locationMap.put(new Point( -7,   2), new double[] {   96.318,  100.400,   42.545 });
    	locationMap.put(new Point( -8,   2), new double[] {  111.198,  100.350,   42.545 });
    	locationMap.put(new Point( -9,   2), new double[] {  126.118,  100.280,   42.550 });
    	locationMap.put(new Point(-10,   2), new double[] {  141.063,  100.185,   42.545 });
    	locationMap.put(new Point(-11,   2), new double[] {  156.043,  100.070,   42.545 });
    	locationMap.put(new Point(-12,   2), new double[] {  171.063,   99.930,   42.540 });
    	locationMap.put(new Point(-13,   2), new double[] {  186.123,   99.775,   42.535 });
    	locationMap.put(new Point(-14,   2), new double[] {  201.233,   99.590,   42.530 });
    	locationMap.put(new Point(-15,   2), new double[] {  216.393,   99.385,   42.535 });
    	locationMap.put(new Point(-16,   2), new double[] {  231.608,   99.155,   42.530 });
    	locationMap.put(new Point(-17,   2), new double[] {  246.883,   98.900,   42.525 });
    	locationMap.put(new Point(-18,   2), new double[] {  262.218,   98.625,   42.515 });
    	locationMap.put(new Point(-19,   2), new double[] {  277.628,   98.330,   42.510 });
    	locationMap.put(new Point(-20,   2), new double[] {  293.098,   98.010,   42.510 });
    	locationMap.put(new Point(-21,   2), new double[] {  308.653,   97.675,   42.505 });
    	locationMap.put(new Point(-22,   2), new double[] {  324.288,   97.305,   42.495 });
    	locationMap.put(new Point(-23,   2), new double[] {  340.008,   96.925,   42.490 });
    	locationMap.put(new Point( 23,   1), new double[] { -340.003,   96.830,   27.385 });
    	locationMap.put(new Point( 22,   1), new double[] { -324.278,   97.215,   27.385 });
    	locationMap.put(new Point( 21,   1), new double[] { -308.648,   97.575,   27.385 });
    	locationMap.put(new Point( 20,   1), new double[] { -293.093,   97.915,   27.385 });
    	locationMap.put(new Point( 19,   1), new double[] { -277.613,   98.240,   27.385 });
    	locationMap.put(new Point( 18,   1), new double[] { -262.213,   98.535,   27.385 });
    	locationMap.put(new Point( 17,   1), new double[] { -246.878,   98.810,   27.385 });
    	locationMap.put(new Point( 16,   1), new double[] { -231.603,   99.060,   27.385 });
    	locationMap.put(new Point( 15,   1), new double[] { -216.383,   99.290,   27.385 });
    	locationMap.put(new Point( 14,   1), new double[] { -201.223,   99.495,   27.385 });
    	locationMap.put(new Point( 13,   1), new double[] { -186.113,   99.680,   27.385 });
    	locationMap.put(new Point( 12,   1), new double[] { -171.048,   99.840,   27.385 });
    	locationMap.put(new Point( 11,   1), new double[] { -156.033,   99.980,   27.385 });
    	locationMap.put(new Point( 10,   1), new double[] { -141.048,  100.095,   27.385 });
    	locationMap.put(new Point(  9,   1), new double[] { -126.103,  100.185,   27.385 });
    	locationMap.put(new Point(  8,   1), new double[] { -111.193,  100.255,   27.385 });
    	locationMap.put(new Point(  7,   1), new double[] {  -96.303,  100.305,   27.385 });
    	locationMap.put(new Point(  6,   1), new double[] {  -81.448,  100.330,   27.385 });
    	locationMap.put(new Point(  5,   1), new double[] {  -66.608,  100.330,   27.385 });
    	locationMap.put(new Point(  4,   1), new double[] {  -51.783,  100.310,   27.385 });
    	locationMap.put(new Point(  3,   1), new double[] {  -36.973,  100.265,   27.385 });
    	locationMap.put(new Point(  2,   1), new double[] {  -22.178,  100.200,   27.385 });
    	locationMap.put(new Point(  1,   1), new double[] {   -7.388,  100.105,   27.385 });
    	locationMap.put(new Point( -1,   1), new double[] {    7.403,  100.105,   27.385 });
    	locationMap.put(new Point(-11,   1), new double[] {  156.078,   99.980,   27.385 });
    	locationMap.put(new Point(-12,   1), new double[] {  171.103,   99.840,   27.385 });
    	locationMap.put(new Point(-13,   1), new double[] {  186.168,   99.680,   27.385 });
    	locationMap.put(new Point(-14,   1), new double[] {  201.268,   99.495,   27.385 });
    	locationMap.put(new Point(-15,   1), new double[] {  216.423,   99.290,   27.385 });
    	locationMap.put(new Point(-16,   1), new double[] {  231.638,   99.060,   27.385 });
    	locationMap.put(new Point(-17,   1), new double[] {  246.913,   98.810,   27.385 });
    	locationMap.put(new Point(-18,   1), new double[] {  262.248,   98.535,   27.385 });
    	locationMap.put(new Point(-19,   1), new double[] {  277.658,   98.240,   27.385 });
    	locationMap.put(new Point(-20,   1), new double[] {  293.133,   97.920,   27.385 });
    	locationMap.put(new Point(-21,   1), new double[] {  308.688,   97.575,   27.385 });
    	locationMap.put(new Point(-22,   1), new double[] {  324.323,   97.215,   27.385 });
    	locationMap.put(new Point(-23,   1), new double[] {  340.043,   96.830,   27.385 });
    	locationMap.put(new Point( 23,  -1), new double[] { -339.998,   96.840,  -27.330 });
    	locationMap.put(new Point( 22,  -1), new double[] { -324.278,   97.225,  -27.340 });
    	locationMap.put(new Point( 21,  -1), new double[] { -308.643,   97.585,  -27.345 });
    	locationMap.put(new Point( 20,  -1), new double[] { -293.093,   97.925,  -27.350 });
    	locationMap.put(new Point( 19,  -1), new double[] { -277.613,   98.245,  -27.360 });
    	locationMap.put(new Point( 18,  -1), new double[] { -262.213,   98.545,  -27.365 });
    	locationMap.put(new Point( 17,  -1), new double[] { -246.868,   98.820,  -27.365 });
    	locationMap.put(new Point( 16,  -1), new double[] { -231.598,   99.070,  -27.370 });
    	locationMap.put(new Point( 15,  -1), new double[] { -216.383,   99.300,  -27.375 });
    	locationMap.put(new Point( 14,  -1), new double[] { -201.223,   99.505,  -27.380 });
    	locationMap.put(new Point( 13,  -1), new double[] { -186.113,   99.690,  -27.385 });
    	locationMap.put(new Point( 12,  -1), new double[] { -171.048,   99.850,  -27.380 });
    	locationMap.put(new Point( 11,  -1), new double[] { -156.028,   99.990,  -27.385 });
    	locationMap.put(new Point( 10,  -1), new double[] { -141.048,  100.100,  -27.390 });
    	locationMap.put(new Point(  9,  -1), new double[] { -126.103,  100.195,  -27.390 });
    	locationMap.put(new Point(  8,  -1), new double[] { -111.193,  100.265,  -27.395 });
    	locationMap.put(new Point(  7,  -1), new double[] {  -96.303,  100.315,  -27.395 });
    	locationMap.put(new Point(  6,  -1), new double[] {  -81.443,  100.340,  -27.390 });
    	locationMap.put(new Point(  5,  -1), new double[] {  -66.603,  100.335,  -27.390 });
    	locationMap.put(new Point(  4,  -1), new double[] {  -51.783,  100.315,  -27.390 });
    	locationMap.put(new Point(  3,  -1), new double[] {  -36.973,  100.275,  -27.395 });
    	locationMap.put(new Point(  2,  -1), new double[] {  -22.173,  100.205,  -27.390 });
    	locationMap.put(new Point(  1,  -1), new double[] {   -7.383,  100.115,  -27.385 });
    	locationMap.put(new Point( -1,  -1), new double[] {    7.403,  100.115,  -27.385 });
    	locationMap.put(new Point(-11,  -1), new double[] {  156.088,   99.985,  -27.385 });
    	locationMap.put(new Point(-12,  -1), new double[] {  171.103,   99.845,  -27.380 });
    	locationMap.put(new Point(-13,  -1), new double[] {  186.168,   99.680,  -27.385 });
    	locationMap.put(new Point(-14,  -1), new double[] {  201.268,   99.495,  -27.380 });
    	locationMap.put(new Point(-15,  -1), new double[] {  216.428,   99.290,  -27.375 });
    	locationMap.put(new Point(-16,  -1), new double[] {  231.643,   99.060,  -27.370 });
    	locationMap.put(new Point(-17,  -1), new double[] {  246.913,   98.810,  -27.365 });
    	locationMap.put(new Point(-18,  -1), new double[] {  262.258,   98.535,  -27.365 });
    	locationMap.put(new Point(-19,  -1), new double[] {  277.658,   98.240,  -27.360 });
    	locationMap.put(new Point(-20,  -1), new double[] {  293.138,   97.925,  -27.350 });
    	locationMap.put(new Point(-21,  -1), new double[] {  308.688,   97.580,  -27.345 });
    	locationMap.put(new Point(-22,  -1), new double[] {  324.323,   97.215,  -27.340 });
    	locationMap.put(new Point(-23,  -1), new double[] {  340.043,   96.835,  -27.330 });
    	locationMap.put(new Point( 23,  -2), new double[] { -339.998,   96.930,  -42.435 });
    	locationMap.put(new Point( 22,  -2), new double[] { -324.278,   97.315,  -42.445 });
    	locationMap.put(new Point( 21,  -2), new double[] { -308.648,   97.680,  -42.455 });
    	locationMap.put(new Point( 20,  -2), new double[] { -293.093,   98.015,  -42.470 });
    	locationMap.put(new Point( 19,  -2), new double[] { -277.613,   98.340,  -42.480 });
    	locationMap.put(new Point( 18,  -2), new double[] { -262.208,   98.635,  -42.490 });
    	locationMap.put(new Point( 17,  -2), new double[] { -246.873,   98.910,  -42.500 });
    	locationMap.put(new Point( 16,  -2), new double[] { -231.593,   99.160,  -42.510 });
    	locationMap.put(new Point( 15,  -2), new double[] { -216.383,   99.390,  -42.515 });
    	locationMap.put(new Point( 14,  -2), new double[] { -201.223,   99.595,  -42.525 });
    	locationMap.put(new Point( 13,  -2), new double[] { -186.113,   99.780,  -42.525 });
    	locationMap.put(new Point( 12,  -2), new double[] { -171.048,   99.940,  -42.535 });
    	locationMap.put(new Point( 11,  -2), new double[] { -156.028,  100.080,  -42.540 });
    	locationMap.put(new Point( 10,  -2), new double[] { -141.048,  100.195,  -42.540 });
    	locationMap.put(new Point(  9,  -2), new double[] { -126.103,  100.290,  -42.545 });
    	locationMap.put(new Point(  8,  -2), new double[] { -111.193,  100.355,  -42.550 });
    	locationMap.put(new Point(  7,  -2), new double[] {  -96.303,  100.405,  -42.550 });
    	locationMap.put(new Point(  6,  -2), new double[] {  -81.443,  100.430,  -42.550 });
    	locationMap.put(new Point(  5,  -2), new double[] {  -66.608,  100.430,  -42.550 });
    	locationMap.put(new Point(  4,  -2), new double[] {  -51.783,  100.405,  -42.550 });
    	locationMap.put(new Point(  3,  -2), new double[] {  -36.973,  100.365,  -42.550 });
    	locationMap.put(new Point(  2,  -2), new double[] {  -22.178,  100.295,  -42.545 });
    	locationMap.put(new Point(  1,  -2), new double[] {   -7.388,  100.205,  -42.545 });
    	locationMap.put(new Point( -1,  -2), new double[] {    7.403,  100.205,  -42.545 });
    	locationMap.put(new Point( -2,  -2), new double[] {   22.193,  100.295,  -42.545 });
    	locationMap.put(new Point( -3,  -2), new double[] {   36.988,  100.365,  -42.550 });
    	locationMap.put(new Point( -4,  -2), new double[] {   51.798,  100.405,  -42.550 });
    	locationMap.put(new Point( -5,  -2), new double[] {   66.623,  100.430,  -42.550 });
    	locationMap.put(new Point( -6,  -2), new double[] {   81.458,  100.430,  -42.550 });
    	locationMap.put(new Point( -7,  -2), new double[] {   96.318,  100.405,  -42.550 });
    	locationMap.put(new Point( -8,  -2), new double[] {  111.208,  100.355,  -42.550 });
    	locationMap.put(new Point( -9,  -2), new double[] {  126.118,  100.290,  -42.545 });
    	locationMap.put(new Point(-10,  -2), new double[] {  141.063,  100.195,  -42.540 });
    	locationMap.put(new Point(-11,  -2), new double[] {  156.043,  100.080,  -42.540 });
    	locationMap.put(new Point(-12,  -2), new double[] {  171.063,   99.940,  -42.535 });
    	locationMap.put(new Point(-13,  -2), new double[] {  186.128,   99.780,  -42.525 });
    	locationMap.put(new Point(-14,  -2), new double[] {  201.238,   99.595,  -42.525 });
    	locationMap.put(new Point(-15,  -2), new double[] {  216.398,   99.390,  -42.515 });
    	locationMap.put(new Point(-16,  -2), new double[] {  231.613,   99.160,  -42.510 });
    	locationMap.put(new Point(-17,  -2), new double[] {  246.888,   98.910,  -42.500 });
    	locationMap.put(new Point(-18,  -2), new double[] {  262.223,   98.635,  -42.490 });
    	locationMap.put(new Point(-19,  -2), new double[] {  277.628,   98.340,  -42.480 });
    	locationMap.put(new Point(-20,  -2), new double[] {  293.108,   98.015,  -42.470 });
    	locationMap.put(new Point(-21,  -2), new double[] {  308.663,   97.680,  -42.455 });
    	locationMap.put(new Point(-22,  -2), new double[] {  324.293,   97.315,  -42.445 });
    	locationMap.put(new Point(-23,  -2), new double[] {  340.013,   96.930,  -42.435 });
    	locationMap.put(new Point( 23,  -3), new double[] { -339.998,   97.000,  -57.540 });
    	locationMap.put(new Point( 22,  -3), new double[] { -324.278,   97.385,  -57.560 });
    	locationMap.put(new Point( 21,  -3), new double[] { -308.648,   97.745,  -57.575 });
    	locationMap.put(new Point( 20,  -3), new double[] { -293.093,   98.090,  -57.595 });
    	locationMap.put(new Point( 19,  -3), new double[] { -277.613,   98.410,  -57.610 });
    	locationMap.put(new Point( 18,  -3), new double[] { -262.208,   98.705,  -57.625 });
    	locationMap.put(new Point( 17,  -3), new double[] { -246.873,   98.975,  -57.640 });
    	locationMap.put(new Point( 16,  -3), new double[] { -231.593,   99.225,  -57.655 });
    	locationMap.put(new Point( 15,  -3), new double[] { -216.383,   99.455,  -57.665 });
    	locationMap.put(new Point( 14,  -3), new double[] { -201.223,   99.665,  -57.675 });
    	locationMap.put(new Point( 13,  -3), new double[] { -186.113,   99.845,  -57.685 });
    	locationMap.put(new Point( 12,  -3), new double[] { -171.048,  100.010,  -57.690 });
    	locationMap.put(new Point( 11,  -3), new double[] { -156.028,  100.145,  -57.700 });
    	locationMap.put(new Point( 10,  -3), new double[] { -141.048,  100.265,  -57.705 });
    	locationMap.put(new Point(  9,  -3), new double[] { -126.103,  100.355,  -57.710 });
    	locationMap.put(new Point(  8,  -3), new double[] { -111.193,  100.425,  -57.710 });
    	locationMap.put(new Point(  7,  -3), new double[] {  -96.303,  100.475,  -57.720 });
    	locationMap.put(new Point(  6,  -3), new double[] {  -81.443,  100.495,  -57.715 });
    	locationMap.put(new Point(  5,  -3), new double[] {  -66.608,  100.500,  -57.720 });
    	locationMap.put(new Point(  4,  -3), new double[] {  -51.783,  100.480,  -57.715 });
    	locationMap.put(new Point(  3,  -3), new double[] {  -36.973,  100.430,  -57.710 });
    	locationMap.put(new Point(  2,  -3), new double[] {  -22.178,  100.365,  -57.710 });
    	locationMap.put(new Point(  1,  -3), new double[] {   -7.388,  100.275,  -57.705 });
    	locationMap.put(new Point( -1,  -3), new double[] {    7.403,  100.275,  -57.705 });
    	locationMap.put(new Point( -2,  -3), new double[] {   22.193,  100.365,  -57.710 });
    	locationMap.put(new Point( -3,  -3), new double[] {   36.988,  100.430,  -57.710 });
    	locationMap.put(new Point( -4,  -3), new double[] {   51.798,  100.480,  -57.715 });
    	locationMap.put(new Point( -5,  -3), new double[] {   66.623,  100.500,  -57.720 });
    	locationMap.put(new Point( -6,  -3), new double[] {   81.458,  100.495,  -57.715 });
    	locationMap.put(new Point( -7,  -3), new double[] {   96.318,  100.475,  -57.720 });
    	locationMap.put(new Point( -8,  -3), new double[] {  111.208,  100.425,  -57.710 });
    	locationMap.put(new Point( -9,  -3), new double[] {  126.118,  100.355,  -57.710 });
    	locationMap.put(new Point(-10,  -3), new double[] {  141.063,  100.265,  -57.705 });
    	locationMap.put(new Point(-11,  -3), new double[] {  156.043,  100.145,  -57.700 });
    	locationMap.put(new Point(-12,  -3), new double[] {  171.063,  100.010,  -57.690 });
    	locationMap.put(new Point(-13,  -3), new double[] {  186.128,   99.845,  -57.685 });
    	locationMap.put(new Point(-14,  -3), new double[] {  201.238,   99.665,  -57.675 });
    	locationMap.put(new Point(-15,  -3), new double[] {  216.398,   99.455,  -57.665 });
    	locationMap.put(new Point(-16,  -3), new double[] {  231.613,   99.225,  -57.655 });
    	locationMap.put(new Point(-17,  -3), new double[] {  246.888,   98.975,  -57.640 });
    	locationMap.put(new Point(-18,  -3), new double[] {  262.223,   98.705,  -57.625 });
    	locationMap.put(new Point(-19,  -3), new double[] {  277.628,   98.410,  -57.610 });
    	locationMap.put(new Point(-20,  -3), new double[] {  293.108,   98.090,  -57.595 });
    	locationMap.put(new Point(-21,  -3), new double[] {  308.663,   97.745,  -57.575 });
    	locationMap.put(new Point(-22,  -3), new double[] {  324.293,   97.385,  -57.560 });
    	locationMap.put(new Point(-23,  -3), new double[] {  340.013,   97.000,  -57.540 });
    	locationMap.put(new Point( 23,  -4), new double[] { -339.998,   97.045,  -72.655 });
    	locationMap.put(new Point( 22,  -4), new double[] { -324.278,   97.435,  -72.680 });
    	locationMap.put(new Point( 21,  -4), new double[] { -308.648,   97.795,  -72.710 });
    	locationMap.put(new Point( 20,  -4), new double[] { -293.093,   98.135,  -72.730 });
    	locationMap.put(new Point( 19,  -4), new double[] { -277.613,   98.455,  -72.750 });
    	locationMap.put(new Point( 18,  -4), new double[] { -262.208,   98.750,  -72.775 });
    	locationMap.put(new Point( 17,  -4), new double[] { -246.873,   99.020,  -72.795 });
    	locationMap.put(new Point( 16,  -4), new double[] { -231.593,   99.280,  -72.810 });
    	locationMap.put(new Point( 15,  -4), new double[] { -216.383,   99.505,  -72.820 });
    	locationMap.put(new Point( 14,  -4), new double[] { -201.223,   99.710,  -72.840 });
    	locationMap.put(new Point( 13,  -4), new double[] { -186.113,   99.895,  -72.850 });
    	locationMap.put(new Point( 12,  -4), new double[] { -171.048,  100.055,  -72.860 });
    	locationMap.put(new Point( 11,  -4), new double[] { -156.028,  100.190,  -72.870 });
    	locationMap.put(new Point( 10,  -4), new double[] { -141.048,  100.305,  -72.880 });
    	locationMap.put(new Point(  9,  -4), new double[] { -126.103,  100.400,  -72.885 });
    	locationMap.put(new Point(  8,  -4), new double[] { -111.193,  100.470,  -72.890 });
    	locationMap.put(new Point(  7,  -4), new double[] {  -96.303,  100.520,  -72.890 });
    	locationMap.put(new Point(  6,  -4), new double[] {  -81.443,  100.540,  -72.895 });
    	locationMap.put(new Point(  5,  -4), new double[] {  -66.608,  100.540,  -72.895 });
    	locationMap.put(new Point(  4,  -4), new double[] {  -51.783,  100.520,  -72.895 });
    	locationMap.put(new Point(  3,  -4), new double[] {  -36.973,  100.480,  -72.890 });
    	locationMap.put(new Point(  2,  -4), new double[] {  -22.178,  100.405,  -72.885 });
    	locationMap.put(new Point(  1,  -4), new double[] {   -7.388,  100.320,  -72.880 });
    	locationMap.put(new Point( -1,  -4), new double[] {    7.403,  100.320,  -72.880 });
    	locationMap.put(new Point( -2,  -4), new double[] {   22.193,  100.405,  -72.885 });
    	locationMap.put(new Point( -3,  -4), new double[] {   36.988,  100.480,  -72.890 });
    	locationMap.put(new Point( -4,  -4), new double[] {   51.798,  100.520,  -72.895 });
    	locationMap.put(new Point( -5,  -4), new double[] {   66.623,  100.540,  -72.895 });
    	locationMap.put(new Point( -6,  -4), new double[] {   81.458,  100.540,  -72.895 });
    	locationMap.put(new Point( -7,  -4), new double[] {   96.318,  100.520,  -72.890 });
    	locationMap.put(new Point( -8,  -4), new double[] {  111.208,  100.470,  -72.890 });
    	locationMap.put(new Point( -9,  -4), new double[] {  126.118,  100.400,  -72.885 });
    	locationMap.put(new Point(-10,  -4), new double[] {  141.063,  100.305,  -72.880 });
    	locationMap.put(new Point(-11,  -4), new double[] {  156.043,  100.190,  -72.870 });
    	locationMap.put(new Point(-12,  -4), new double[] {  171.063,  100.055,  -72.860 });
    	locationMap.put(new Point(-13,  -4), new double[] {  186.128,   99.895,  -72.850 });
    	locationMap.put(new Point(-14,  -4), new double[] {  201.238,   99.710,  -72.840 });
    	locationMap.put(new Point(-15,  -4), new double[] {  216.398,   99.505,  -72.820 });
    	locationMap.put(new Point(-16,  -4), new double[] {  231.613,   99.280,  -72.810 });
    	locationMap.put(new Point(-17,  -4), new double[] {  246.888,   99.020,  -72.795 });
    	locationMap.put(new Point(-18,  -4), new double[] {  262.223,   98.750,  -72.775 });
    	locationMap.put(new Point(-19,  -4), new double[] {  277.628,   98.455,  -72.750 });
    	locationMap.put(new Point(-20,  -4), new double[] {  293.108,   98.135,  -72.730 });
    	locationMap.put(new Point(-21,  -4), new double[] {  308.663,   97.795,  -72.710 });
    	locationMap.put(new Point(-22,  -4), new double[] {  324.293,   97.435,  -72.680 });
    	locationMap.put(new Point(-23,  -4), new double[] {  340.013,   97.045,  -72.655 });
    	locationMap.put(new Point( 23,  -5), new double[] { -339.998,   97.070,  -87.790 });
    	locationMap.put(new Point( 22,  -5), new double[] { -324.278,   97.460,  -87.820 });
    	locationMap.put(new Point( 21,  -5), new double[] { -308.648,   97.820,  -87.850 });
    	locationMap.put(new Point( 20,  -5), new double[] { -293.093,   98.160,  -87.885 });
    	locationMap.put(new Point( 19,  -5), new double[] { -277.613,   98.480,  -87.910 });
    	locationMap.put(new Point( 18,  -5), new double[] { -262.208,   98.775,  -87.935 });
    	locationMap.put(new Point( 17,  -5), new double[] { -246.873,   99.050,  -87.960 });
    	locationMap.put(new Point( 16,  -5), new double[] { -231.593,   99.300,  -87.980 });
    	locationMap.put(new Point( 15,  -5), new double[] { -216.383,   99.530,  -88.000 });
    	locationMap.put(new Point( 14,  -5), new double[] { -201.223,   99.735,  -88.015 });
    	locationMap.put(new Point( 13,  -5), new double[] { -186.113,   99.920,  -88.030 });
    	locationMap.put(new Point( 12,  -5), new double[] { -171.048,  100.080,  -88.045 });
    	locationMap.put(new Point( 11,  -5), new double[] { -156.028,  100.215,  -88.055 });
    	locationMap.put(new Point( 10,  -5), new double[] { -141.048,  100.335,  -88.065 });
    	locationMap.put(new Point(  9,  -5), new double[] { -126.103,  100.420,  -88.070 });
    	locationMap.put(new Point(  8,  -5), new double[] { -111.193,  100.490,  -88.075 });
    	locationMap.put(new Point(  7,  -5), new double[] {  -96.303,  100.540,  -88.085 });
    	locationMap.put(new Point(  6,  -5), new double[] {  -81.443,  100.565,  -88.085 });
    	locationMap.put(new Point(  5,  -5), new double[] {  -66.608,  100.560,  -88.085 });
    	locationMap.put(new Point(  4,  -5), new double[] {  -51.783,  100.540,  -88.085 });
    	locationMap.put(new Point(  3,  -5), new double[] {  -36.973,  100.500,  -88.080 });
    	locationMap.put(new Point(  2,  -5), new double[] {  -22.178,  100.430,  -88.075 });
    	locationMap.put(new Point(  1,  -5), new double[] {   -7.388,  100.340,  -88.065 });
    	locationMap.put(new Point( -1,  -5), new double[] {    7.403,  100.340,  -88.070 });
    	locationMap.put(new Point( -2,  -5), new double[] {   22.193,  100.430,  -88.075 });
    	locationMap.put(new Point( -3,  -5), new double[] {   36.988,  100.500,  -88.080 });
    	locationMap.put(new Point( -4,  -5), new double[] {   51.798,  100.540,  -88.085 });
    	locationMap.put(new Point( -5,  -5), new double[] {   66.623,  100.560,  -88.085 });
    	locationMap.put(new Point( -6,  -5), new double[] {   81.458,  100.565,  -88.085 });
    	locationMap.put(new Point( -7,  -5), new double[] {   96.318,  100.540,  -88.085 });
    	locationMap.put(new Point( -8,  -5), new double[] {  111.208,  100.490,  -88.075 });
    	locationMap.put(new Point( -9,  -5), new double[] {  126.118,  100.420,  -88.070 });
    	locationMap.put(new Point(-10,  -5), new double[] {  141.063,  100.335,  -88.065 });
    	locationMap.put(new Point(-11,  -5), new double[] {  156.043,  100.215,  -88.055 });
    	locationMap.put(new Point(-12,  -5), new double[] {  171.063,  100.080,  -88.045 });
    	locationMap.put(new Point(-13,  -5), new double[] {  186.128,   99.915,  -88.030 });
    	locationMap.put(new Point(-14,  -5), new double[] {  201.238,   99.735,  -88.015 });
    	locationMap.put(new Point(-15,  -5), new double[] {  216.398,   99.530,  -88.000 });
    	locationMap.put(new Point(-16,  -5), new double[] {  231.613,   99.300,  -87.980 });
    	locationMap.put(new Point(-17,  -5), new double[] {  246.888,   99.050,  -87.960 });
    	locationMap.put(new Point(-18,  -5), new double[] {  262.223,   98.775,  -87.935 });
    	locationMap.put(new Point(-19,  -5), new double[] {  277.628,   98.480,  -87.910 });
    	locationMap.put(new Point(-20,  -5), new double[] {  293.108,   98.160,  -87.885 });
    	locationMap.put(new Point(-21,  -5), new double[] {  308.663,   97.820,  -87.850 });
    	locationMap.put(new Point(-22,  -5), new double[] {  324.293,   97.460,  -87.820 });
    	locationMap.put(new Point(-23,  -5), new double[] {  340.013,   97.070,  -87.790 });
    }
}
