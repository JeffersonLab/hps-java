package org.hps.analysis.trigger.data;

/**
 * Class <code>TriggerStatModule</code> holds all the statistics from
 * a single trigger event. The variables in this class are set by the
 * extending class <code>TriggerEvent</code>, while this object lacks
 * any external means to alter its values so that it can be used in a
 * static fashion.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerStatModule {
    // Store the reference index for SSP simulated triggers and recon
    // simulated triggers.
    protected static final int SSP = 0;
    protected static final int RECON = 1;
    
    // Define TI trigger type identifiers.
    public static final int SINGLES_0 = 0;
    public static final int SINGLES_1 = 1;
    public static final int PAIR_0    = 2;
    public static final int PAIR_1    = 3;
    public static final int PULSER    = 4;
    public static final int COSMIC    = 5;
    
    // Track the number of simulated triggers seen for each source type.
    // SSP simulated triggers from the SSP bank clusters. Reconstructed
    // simulated triggers come from clusters built from FADC data.
    protected int[] simTriggers = new int[2];
    
    // Also track the number of triggers reported by the SSP bank.
    protected int reportedTriggers = 0;
    
    // Track the number of simulated triggers of each type that were
    // successfully matched.
    protected int[] matchedTriggers = new int[2];
    
    // Track the number of simulated triggers that could not be matched
    // at all.
    protected int[] unmatchedTriggers = new int[2];
    
    // Track which cuts succeeded and which cuts failed for each type.
    // Note that this is currently only tracked for SSP cluster triggers.
    protected int[][] failedCuts = new int[2][4];
    
    // Store the number of trigger matches seen over all events that
    // contain a given TI flag.
    protected int[][] tiTriggersSeen = new int[2][6];
    protected int[][] tiTriggersMatched = new int[2][6];
    
    /**
     * Clears all of the statistical counters in the object.
     */
    void clear() {
        // Clear all values.
        for(int sourceType = 0; sourceType < 2; sourceType++) {
            // Clear the general statistics.
            simTriggers[sourceType]       = 0;
            matchedTriggers[sourceType]   = 0;
            unmatchedTriggers[sourceType] = 0;
            
            // Clear the cut failure statistics.
            for(int cutID = 0; cutID < 4; cutID++) {
                failedCuts[sourceType][cutID] = 0;
            }
            
            // Clear the TI flag statistics.
            for(int tiType = 0; tiType < 6; tiType++) {
                tiTriggersSeen[sourceType][tiType]    = 0;
                tiTriggersMatched[sourceType][tiType] = 0;
            }
        }
    }
    
    @Override
    public TriggerStatModule clone() {
        // Make a new statistics module.
        TriggerStatModule clone = new TriggerStatModule();
        
        // Copy the values that do not depend on trigger source type.
        clone.reportedTriggers = reportedTriggers;
        
        // Set each value that depends on the trigger source type.
        for(int sourceType = 0; sourceType < 2; sourceType++) {
            clone.simTriggers[sourceType] = simTriggers[sourceType];
            clone.matchedTriggers[sourceType] = matchedTriggers[sourceType];
            clone.unmatchedTriggers[sourceType] = unmatchedTriggers[sourceType];
            
            // Set the number of times each cut failed.
            for(int cutID = 0; cutID < 4; cutID++) {
                clone.failedCuts[sourceType][cutID] = failedCuts[sourceType][cutID];
            }
            
            // Copy the values for the TI flag trigger counters.
            for(int tiType = 0; tiType < 6; tiType++) {
                clone.tiTriggersSeen[sourceType][tiType] = tiTriggersSeen[sourceType][tiType];
                clone.tiTriggersMatched[sourceType][tiType] = tiTriggersMatched[sourceType][tiType];
            }
        }
        
        // Return the copied clone.
        return clone;
    }
    
    /**
     * Gets the number of simulated triggers from reconstructed clusters
     * that were not matched.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getFailedReconSimulatedTriggers() {
        return simTriggers[RECON] - matchedTriggers[RECON];
    }
    
    /**
     * Gets the number of simulated triggers from SSP bank clusters
     * that were not matched.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getFailedSSPSimulatedTriggers() {
        return simTriggers[SSP] - matchedTriggers[SSP];
    }
    
    /**
     * Gets the number of simulated triggers from reconstructed clusters
     * that were matched.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getMatchedReconSimulatedTriggers() {
        return matchedTriggers[RECON];
    }
    
    /**
     * Gets the number of simulated triggers from reconstructed clusters
     * that were matched for a given type of trigger when a given TI
     * bank flag was active.
     * @param tiTypeID - The identifier for the type of TI bank trigger
     * that should be active.
     * @param triggerTypeID - The identifier for the type of trigger.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getMatchedReconSimulatedTriggers(int tiTypeID) {
        return tiTriggersMatched[RECON][tiTypeID];
    }
    
    /**
     * Gets the number of simulated triggers from SSP bank clusters
     * that were matched.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getMatchedSSPSimulatedTriggers() {
        return matchedTriggers[SSP];
    }
    
    /**
     * Gets the number of simulated triggers from SSP bank clusters
     * that were matched for a given type of trigger when a given TI
     * bank flag was active.
     * @param tiTypeID - The identifier for the type of TI bank trigger
     * that should be active.
     * @param triggerTypeID - The identifier for the type of trigger.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getMatchedSSPSimulatedTriggers(int tiTypeID) {
        return tiTriggersMatched[SSP][tiTypeID];
    }
    
    /**
     * Gets the number of simulated triggers from reconstructed clusters
     * that were matched, but did not see full cut alignment.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getPartiallyMatchedReconSimulatedTriggers() {
        return simTriggers[RECON] - (matchedTriggers[RECON] + unmatchedTriggers[RECON]);
    }
    
    /**
     * Gets the number of simulated triggers from SSP bank clusters
     * that were matched, but did not see full cut alignment.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getPartiallyMatchedSSPSimulatedTriggers() {
        return simTriggers[SSP] - (matchedTriggers[SSP] + unmatchedTriggers[SSP]);
    }
    
    /**
     * Gets the number of times the specified cut failed for triggers
     * that were partially matched for triggers simulated from FADC
     * reconstructed clusters.
     * @param cutIndex - The numerical cut identifier.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getReconCutFailures(int cutIndex) {
        return getCutFailures(RECON, cutIndex);
    }
    
    /**
     * Gets the number of simulated triggers from reconstructed clusters
     * that were seen.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getReconSimulatedTriggers() {
        return simTriggers[RECON];
    }
    
    /**
     * Gets the number of simulated triggers from reconstructed clusters
     * that were seen for a given trigger type when a given TI bank
     * flag was active.
     * @param tiTypeID - The identifier for the type of TI bank trigger
     * that should be active.
     * @param triggerTypeID - The identifier for the type of trigger.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getReconSimulatedTriggers(int tiTypeID) {
        return tiTriggersSeen[RECON][tiTypeID];
    }
    
    /**
     * Gets the number of triggers reported by the SSP bank.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getReportedTriggers() {
        return reportedTriggers;
    }
    
    /**
     * Gets the number of times the specified cut failed for triggers
     * that were partially matched for triggers simulated from SSP
     * bank clusters.
     * @param cutIndex - The numerical cut identifier.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getSSPCutFailures(int cutIndex) {
        return getCutFailures(SSP, cutIndex);
    }
    
    /**
     * Gets the number of simulated triggers from SSP bank clusters
     * that were seen.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getSSPSimulatedTriggers() {
        return simTriggers[SSP];
    }
    
    /**
     * Gets the number of simulated triggers from SSP bank clusters
     * that were seen for a given trigger type when a given TI bank
     * flag was active.
     * @param tiTypeID - The identifier for the type of TI bank trigger
     * that should be active.
     * @param triggerTypeID - The identifier for the type of trigger.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getSSPSimulatedTriggers(int tiTypeID) {
        return tiTriggersSeen[SSP][tiTypeID];
    }
    
    /**
     * Gets the number of simulated triggers from reconstructed clusters
     * that were completely unmatched.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getUnmatchedReconSimulatedTriggers() {
        return unmatchedTriggers[RECON];
    }
    
    /**
     * Gets the number of simulated triggers from SSP bank clusters
     * that were completely unmatched.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    public int getUnmatchedSSPSimulatedTriggers() {
        return unmatchedTriggers[SSP];
    }
    
    /**
     * Gets the number of times the specified cut failed for triggers
     * that were partially matched for triggers simulated from the type
     * of cluster indicated.
     * @param type - Either <code>SSP</code> or <code>RECON</code>.
     * @param cutIndex - The numerical cut identifier.
     * @return Returns the number of triggers as an <code>int</code>.
     */
    private int getCutFailures(int type, int cutIndex) {
        // Ensure that the cut index is valid.
        if(cutIndex < 0 || cutIndex >= 4) {
            throw new IndexOutOfBoundsException(String.format("Cut index \"%d\" is not recognized.", cutIndex));
        }
        
        // Return the cut failures.
        return failedCuts[type][cutIndex];
    }
}