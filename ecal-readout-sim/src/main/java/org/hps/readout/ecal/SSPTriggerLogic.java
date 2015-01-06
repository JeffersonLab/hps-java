package org.hps.readout.ecal;

import org.lcsim.event.Cluster;

/**
 * Class <code>SSPLogic</code> implements each of the trigger cuts. It
 * provides a central location for all primary trigger cuts so that they
 * can be used by multiple drivers without reimplementing them and any
 * changes can be easily propagated to all appropriate drivers.
 * 
 * @author Kyle MCCarty <mccarty@jlab.org>
 */
public class SSPTriggerLogic {
	// ECal mid-plane, defined by photon beam position (30.52 mrad) at ECal face (z=1393 mm)
    private static final double ORIGIN_X = 1393.0 * Math.tan(0.03052);
    
    /**
     * Checks whether the argument cluster possesses the minimum
     * allowed hits.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public static final boolean clusterHitCountCut(Cluster cluster, int thresholdLow) {
        return (getValueClusterHitCount(cluster) >= thresholdLow);
    }
    
    /**
     * Checks whether the argument cluster seed hit falls within the
     * allowed seed hit energy range.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public static final boolean clusterSeedEnergyCut(Cluster cluster, double thresholdLow, double thresholdHigh) {
        // Get the cluster seed energy.
        double energy = getValueClusterSeedEnergy(cluster);
        
        // Check that it is above the minimum threshold and below the
        // maximum threshold.
        return (energy < thresholdHigh) && (energy > thresholdLow);
    }
    
    /**
     * Checks whether the argument cluster falls within the allowed
     * cluster total energy range.
     * @param cluster - The cluster to check.
     * @param thresholdLow - The lower bound of the cut.
     * @param thresholdHigh - The upper bound of the cut.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public static final boolean clusterTotalEnergyCut(Cluster cluster, double thresholdLow, double thresholdHigh) {
        // Get the total cluster energy.
        double energy = getValueClusterTotalEnergy(cluster);
        
        // Check that it is above the minimum threshold and below the
        // maximum threshold.
        return (energy < thresholdHigh) && (energy > thresholdLow);
    }
    
    /**
     * Calculates the distance between two clusters.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the distance between the clusters.
     */
    public static final double getClusterDistance(Cluster cluster) {
        return Math.hypot(cluster.getCalorimeterHits().get(0).getPosition()[0] - ORIGIN_X, cluster.getCalorimeterHits().get(0).getPosition()[1]);
    }
    
    /**
     * Gets the value used for the cluster total energy cut.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the cut value.
     */
    public static final double getValueClusterTotalEnergy(Cluster cluster) {
        return cluster.getEnergy();
    }
    
    /**
     * Gets the value used for the cluster hit count cut.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the cut value.
     */
    public static final int getValueClusterHitCount(Cluster cluster) {
        return cluster.getCalorimeterHits().size();
    }
    
    /**
     * Gets the value used for the seed hit energy cut.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the cut value.
     */
    public static final double getValueClusterSeedEnergy(Cluster cluster) {
        return cluster.getCalorimeterHits().get(0).getCorrectedEnergy();
    }
    
    /**
     * Calculates the value used by the coplanarity cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @param originX - The calorimeter center.
     * @return Returns the cut value.
     */
    public static final double getValueCoplanarity(Cluster[] clusterPair) {
        // Get the cluster angles.
        double[] clusterAngle = new double[2];
        for(int i = 0; i < 2; i++) {
            double position[] = clusterPair[i].getCalorimeterHits().get(0).getPosition();
            clusterAngle[i] = (Math.toDegrees(Math.atan2(position[1], position[0] - ORIGIN_X)) + 180.0) % 180.0;
        }
        
        // Calculate the coplanarity cut value.
        return Math.abs(clusterAngle[1] - clusterAngle[0]);
    }
    
    /**
     * Calculates the value used by the energy difference cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    public static final double getValueEnergyDifference(Cluster[] clusterPair) {
        return clusterPair[0].getEnergy() - clusterPair[1].getEnergy();
    }
    
    /**
     * Calculates the value used by the energy slope cut.
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @param energySlopeParamF - The value of the variable F in the
     * energy slope equation E_low + R_min * F.
     * @return Returns the cut value.
     */
    public static final double getValueEnergySlope(Cluster[] clusterPair, double energySlopeParamF) {
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
     * @param clusterPair - The cluster pair from which the value should
     * be calculated.
     * @return Returns the cut value.
     */
    public static final double getValueEnergySum(Cluster[] clusterPair) {
        return clusterPair[0].getEnergy() + clusterPair[1].getEnergy();
    }
    
    /**
     * Checks if a cluster pair is coplanar to the beam within a given
     * angle.
     * @param clusterPair - The cluster pair to check.
     * @param thresholdHigh - The upper bound for the cut.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public static final boolean pairCoplanarityCut(Cluster[] clusterPair, double thresholdHigh) {
        return (getValueCoplanarity(clusterPair) < thresholdHigh);
    }
    
    /**
     * Checks if the energy difference between the clusters making up
     * a cluster pair is below an energy difference threshold.
     * @param clusterPair - The cluster pair to check.
     * @param thresholdHigh - The upper bound for the cut.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public static final boolean pairEnergyDifferenceCut(Cluster[] clusterPair, double thresholdHigh) {
        return (getValueEnergyDifference(clusterPair) < thresholdHigh);
    }
    
    /**
     * Requires that the distance from the beam of the lowest energy
     * cluster in a cluster pair satisfies the following:
     * E_low + d_b * 0.0032 GeV/mm < [ Threshold ]
     * @param clusterPair : pair of clusters
     * @param thresholdLow - The lower bound for the cut.
     * @return true if pair is found, false otherwise
     */
    public static final boolean pairEnergySlopeCut(Cluster[] clusterPair, double thresholdLow, double energySlopeParamF) {
        return (getValueEnergySlope(clusterPair, energySlopeParamF) > thresholdLow);
    }
    
    /**
     * Checks if the sum of the energies of clusters making up a cluster
     * pair is below an energy sum threshold.
     * @param clusterPair - The cluster pair to check.
     * @param thresholdLow - The lower bound for the cut.
     * @param thresholdHigh - The upper bound for the cut.
     * @return Returns <code>true</code> if the cluster pair passes
     * the cut and <code>false</code> if it does not.
     */
    public static final boolean pairEnergySumCut(Cluster[] clusterPair, double thresholdLow, double thresholdHigh) {
        // Get the energy sum value.
        double energySum = getValueEnergySum(clusterPair);
        
        // Check that it is within the allowed range.
        return (energySum < thresholdHigh) && (energySum > thresholdLow);
    }
}
