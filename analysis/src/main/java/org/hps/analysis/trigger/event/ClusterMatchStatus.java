package org.hps.analysis.trigger.event;

import java.util.List;

import org.hps.readout.ecal.triggerbank.SSPCluster;
import org.lcsim.event.Cluster;

/**
 * Tracks the status of cluster matching for the purposes of trigger
 * verification.
 * 
 * @author Kyle McCarty
 */
public class ClusterMatchStatus extends ClusterStatModule {
    public void addEvent(ClusterMatchEvent event, List<Cluster> reconClusters, List<SSPCluster> sspClusters) {
    	// Update the number of reconstructed and SSP clusters
		// that have been seen so far.
		int sspCount = sspClusters == null ? 0 : sspClusters.size();
		int reconCount = reconClusters == null ? 0 : reconClusters.size();
		this.sspClusters   += sspCount;
		this.reconClusters += reconCount;
		
		// Update the pair state information.
		matches      += event.getMatches();
		failEnergy   += event.getEnergyFailures();
		failHitCount += event.getHitCountFailures();
		failPosition += event.getPositionFailures();
		
		// In the special case that there are no SSP clusters, no pairs
		// will be listed. All possible fails are known to have failed
		// due to position.
		if(sspClusters == null || sspClusters.isEmpty()) {
			failPosition += (reconClusters == null ? 0 : reconClusters.size());
		}
    }
    
	/**
	 * Clears all statistical information and resets the object of its
	 * default, empty state.
	 */
    @Override
	public void clear() {
		super.clear();
	}
    
    /**
     * Gets a copy of the statistical data stored in the object.
     * @return Returns the data in a <code>ClusterStatModule</code>
     * object.
     */
    public ClusterStatModule cloneStatModule() {
    	return new ClusterStatModule(this);
    }
}