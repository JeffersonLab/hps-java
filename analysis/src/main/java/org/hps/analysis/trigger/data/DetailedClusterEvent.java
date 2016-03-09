package org.hps.analysis.trigger.data;

import java.util.ArrayList;
import java.util.List;

import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.record.triggerbank.SSPCluster;
import org.lcsim.event.Cluster;

public class DetailedClusterEvent extends ClusterEvent {
    // Store all of the pairs.
    private List<ClusterMatchedPair> pairList = new ArrayList<ClusterMatchedPair>();
    
    /**
     * Fuses another <code>ClusterEvent</code> with this object. The
     * other event's cluster pairs and states will be added to those
     * already in this event.
     * @param event - The event to fuse.
     */
    public void addEvent(ClusterEvent event) {
        // Run the superclass method.
        super.addEvent(event);
        
        // If the event is null, do nothing.
        if(event == null) { return; }
        
        // Merge the list of cluster pairs, if applicable.
        if(event instanceof DetailedClusterEvent) {
            pairList.addAll(((DetailedClusterEvent) event).pairList);
        }
    }
    
    /**
     * Adds a reconstructed/SSP cluster pair and marks it as having an
     * energy fail state.
     * @param reconCluster - The reconstructed cluster.
     * @param sspCluster - The SSP cluster.
     */
    public void pairFailEnergy(Cluster reconCluster, SSPCluster sspCluster) {
        pairFailEnergy();
        pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_ENERGY));
    }
    
    /**
     * Adds a reconstructed/SSP cluster pair and marks it as having a
     * hit count fail state.
     * @param reconCluster - The reconstructed cluster.
     * @param sspCluster - The SSP cluster.
     */
    public void pairFailHitCount(Cluster reconCluster, SSPCluster sspCluster) {
        pairFailHitCount();
        pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_HIT_COUNT));
    }
    
    /**
     * Adds a reconstructed/SSP cluster pair and marks it as having a
     * position fail state.
     * @param reconCluster - The reconstructed cluster.
     * @param sspCluster - The SSP cluster.
     */
    public void pairFailPosition(Cluster reconCluster, SSPCluster sspCluster) {
        pairFailPosition();
        pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_POSITION));
    }
    
    /**
     * Adds a reconstructed/SSP cluster pair and marks it as having a
     * time fail state.
     * @param reconCluster - The reconstructed cluster.
     * @param sspCluster - The SSP cluster.
     */
    public void pairFailTime(Cluster reconCluster, SSPCluster sspCluster) {
        pairFailTime();
        pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_TIME));
    }
    
    /**
     * Adds a reconstructed/SSP cluster pair and marks it as having a
     * match state.
     * @param reconCluster - The reconstructed cluster.
     * @param sspCluster - The SSP cluster.
     */
    public void pairMatch(Cluster reconCluster, SSPCluster sspCluster) {
        pairMatch();
        pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_MATCHED));
    }
    
    /**
     * Gets a list of all matched cluster pairs and their match states.
     * @return Returns the matched cluster pairs as a <code>List</code>
     * of <code>ClusterMatchedPair</code> objects.
     */
    public List<ClusterMatchedPair> getClusterPairs() {
        return pairList;
    }
}
