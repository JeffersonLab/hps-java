package org.hps.analysis.trigger.data;

/**
 * Class <code>RunDiagStats</code> provides a central repository for
 * all diagnostic statistics. It allows access and editing of each of
 * the specific statistical modules and also is able to generate new
 * diagnostic snapshots.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class RunDiagStats extends GeneralStatModule {
	// Store the statistics for trigger matching.
	private TriggerDiagStats triggerStats = new TriggerDiagStats();
	
	// Store the statistics for cluster matching.
	private ClusterEvent clusterStats = new ClusterEvent();
	
	/**
	 * Clears all of the statistical counters in the object.
	 */
	public void clear() {
		super.clear();
		clusterStats.clear();
		triggerStats.clear();
	}
	
	/**
	 * Notes that an event failed to match all clusters.
	 */
	public void failedClusterEvent() {
		failedClusterEvents++;
	}
	
	/**
	 * Notes that an event failed to match all pair triggers.
	 */
	public void failedPairEvent() {
		failedPairEvents++;
	}

	/**
	 * Notes that an event failed to match all singles triggers.
	 */
	public void failedSinglesEvent() {
		failedSinglesEvents++;
	}
	
	/**
	 * Gets the cluster data.
	 * @return Returns the <code>ClusterEvent</code> object that holds
	 * the cluster data.
	 */
	public ClusterEvent getClusterStats() {
		return clusterStats;
	}
	
	/**
	 * Gets a snapshot of the statistical data at the present time. The
	 * snapshot will remain static and unchanged even if the generating
	 * object itself is updated.
	 * @return Returns a snapshot as a <code>DiagnosticSnapshot</code>
	 * object.
	 */
	public DiagnosticSnapshot getSnapshot() {
		return new DiagnosticSnapshot(this);
	}
	
	/**
	 * Gets the trigger data.
	 * @return Returns the <code>TriggerDiagStats</code> object that holds
	 * the cluster data.
	 */
	public TriggerDiagStats getTriggerStats() {
		return triggerStats;
	}
	
	/**
	 * Notes that an event occurred.
	 */
	public void sawEvent(long eventTime) {
		// Increment the event count.
		totalEvents++;
		
		// If the start time is not defined, use this as the start time.
		if(startTime == -1) { startTime = eventTime; }
		
		// The end time should always match the most recent event.
		endTime = eventTime;
	}

	/**
	 * Notes that an event was labeled as noise.
	 */
	public void sawNoiseEvent() {
		noiseEvents++;
	}
}
