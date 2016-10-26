package org.hps.analysis.trigger.util;

import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

/**
 * Class <code>TriggerDiagnosticUtil</code> contains a series of
 * utility methods that are used at various points throughout the
 * trigger diagnostic package.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerDiagnosticUtil {
    /**
     * Convenience method that writes the position of a cluster in the
     * form (ix, iy).
     * @param cluster - The cluster.
     * @return Returns the cluster position as a <code>String</code>.
     */
    public static final String clusterPositionString(Cluster cluster) {
        return String.format("(%3d, %3d)", TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster));
    }
    
    /**
     * Convenience method that writes the position of a cluster in the
     * form (ix, iy).
     * @param cluster - The cluster.
     * @return Returns the cluster position as a <code>String</code>.
     */
    public static final String clusterPositionString(SSPCluster cluster) {
        return String.format("(%3d, %3d)", TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster));
    }
    
    /**
     * Convenience method that writes the information in a cluster to
     * a <code>String</code>.
     * @param cluster - The cluster.
     * @return Returns the cluster information as a <code>String</code>.
     */
    public static final String clusterToString(Cluster cluster) {
        return String.format("Cluster at (%3d, %3d) with %.3f GeV and %.0f hits at %4.0f ns.",
                TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster),
                TriggerModule.getValueClusterTotalEnergy(cluster), TriggerModule.getClusterHitCount(cluster),
                TriggerModule.getClusterTime(cluster));
    }
    
    /**
     * Convenience method that writes the information in a cluster to
     * a <code>String</code>.
     * @param cluster - The cluster.
     * @return Returns the cluster information as a <code>String</code>.
     */
    public static final String clusterToString(SSPCluster cluster) {
        return String.format("Cluster at (%3d, %3d) with %.3f GeV and %.0f hits at %4.0f ns.",
                TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster),
                TriggerModule.getValueClusterTotalEnergy(cluster), TriggerModule.getClusterHitCount(cluster),
                TriggerModule.getClusterTime(cluster));
    }
    
    /**
     * Checks whether a cluster is within the safe region of the FADC
     * output window.
     * @param sspCluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster is safe and
     * returns <code>false</code> otherwise.
     */
    public static final boolean isVerifiable(SSPCluster sspCluster, int nsa, int nsb, int windowWidth) {
        // Check that none of the hits are within the disallowed
        // region of the FADC readout window.
        if(TriggerModule.getClusterTime(sspCluster) <= nsb || TriggerModule.getClusterTime(sspCluster) >= (windowWidth - nsa)) {
            return false;
        }
        
        // If all of the cluster hits pass the time cut, the cluster
        // is valid.
        return true;
    }
    
    /**
     * Checks whether all of the hits in a cluster are within the safe
     * region of the FADC output window.
     * @param reconCluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster is safe and
     * returns <code>false</code> otherwise.
     */
    public static final boolean isVerifiable(Cluster reconCluster, int nsa, int nsb, int windowWidth) {
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
}