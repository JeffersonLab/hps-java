package org.hps.analysis.trigger.data;

/**
 * Class <code>DiagnosticSnapshot</code> creates a snapshot of the
 * trigger diagnostics at a specific time that can be passed to other
 * classes. It is entirely static and will not change after creation.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class DiagnosticSnapshot {
    // Store the TI trigger information.
    private int[] tiSeenAll = new int[6];
    private int[] tiSeenHierarchical = new int[6];
    
    // Store the statistical modules.
    private final GeneralStatModule generalStats;
    private final ClusterStatModule clusterStats;
    private final TriggerStatModule[] triggerStats = new TriggerStatModule[4];
    
    /**
     * Creates a snapshot of the trigger diagnostic results.
     * @param stats - The run statistical object.
     */
    DiagnosticSnapshot(RunDiagStats stats) {
        // Store the statistical modules.
        generalStats = stats.clone();
        clusterStats = stats.getClusterStats().clone();
        triggerStats[0] = stats.getTriggerStats().getSingles0Stats().clone();
        triggerStats[1] = stats.getTriggerStats().getSingles1Stats().clone();
        triggerStats[2] = stats.getTriggerStats().getPair0Stats().clone();
        triggerStats[3] = stats.getTriggerStats().getPair1Stats().clone();
        
        // Copy the TI trigger data.
        for(int triggerType = 0; triggerType < 6; triggerType++) {
            tiSeenAll[triggerType] = stats.getTriggerStats().getTITriggers(triggerType, false);
            tiSeenHierarchical[triggerType] = stats.getTriggerStats().getTITriggers(triggerType, true);
        }
    }
    
    /**
     * Gets the general run statistics.
     * @return Returns a <code>GeneralStatModule</code> object that
     * contains the statistics.
     */
    public GeneralStatModule getGeneralStats() {
        return generalStats;
    }
    
    /**
     * Gets the cluster statistics.
     * @return Returns a <code>ClusterStatModule</code> object that
     * contains the statistics.
     */
    public ClusterStatModule getClusterStats() {
        return clusterStats;
    }
    
    /**
     * Gets the singles 0 trigger statistics.
     * @return Returns a <code>TriggerStatModule</code> object that
     * contains the statistics.
     */
    public TriggerStatModule getSingles0Stats() {
        return triggerStats[0];
    }
    
    /**
     * Gets the singles 1 trigger statistics.
     * @return Returns a <code>TriggerStatModule</code> object that
     * contains the statistics.
     */
    public TriggerStatModule getSingles1Stats() {
        return triggerStats[1];
    }
    
    /**
     * Gets the pair 0 trigger statistics.
     * @return Returns a <code>TriggerStatModule</code> object that
     * contains the statistics.
     */
    public TriggerStatModule getPair0Stats() {
        return triggerStats[2];
    }
    
    /**
     * Gets the pair 1 trigger statistics.
     * @return Returns a <code>TriggerStatModule</code> object that
     * contains the statistics.
     */
    public TriggerStatModule getPair1Stats() {
        return triggerStats[3];
    }
    
    /**
     * Gets the total number of events where the TI reported a trigger
     * of the specified type.
     * @param triggerID - The identifier for the type of trigger.
     * @param hierarchical - <code>true</code> returns only the number of
     * events where this trigger type was the <i>only</i> type seen by
     * the TI while <code>false</code> returns the number of events
     * that saw this trigger type without regards for other trigger
     * flags.
     * @return Returns the count as an <code>int</code>.
     */
    public int getTITriggers(int triggerID, boolean hierarchical) {
        // Verify the trigger type.
        validateTriggerType(triggerID);
        
        // Increment the counters.
        if(hierarchical) { return tiSeenHierarchical[triggerID]; }
        else { return tiSeenAll[triggerID]; }
    }
    
    /**
     * Produces an exception if the argument trigger type is not of a
     * supported type.
     * @param triggerType - The trigger type to verify.
     */
    private static final void validateTriggerType(int triggerType) {
        if(triggerType < 0 || triggerType > 5) {
            throw new IndexOutOfBoundsException(String.format("Trigger type \"%d\" is not supported.", triggerType));
        }
    }
}
