package org.hps.analysis.trigger.data;

/**
 * Class <code>GeneralStatModule</code> stores statistical data that
 * is not specific to any one part of the diagnostics.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class GeneralStatModule {
    // Store general run statistics.
    protected long endTime = -1;
    protected long startTime = -1;
    protected int totalEvents = 0;
    protected int noiseEvents = 0;
    protected int failedPairEvents = 0;
    protected int failedClusterEvents = 0;
    protected int failedSinglesEvents = 0;
    
    /**
     * Clears all of the statistical counters in the object.
     */
    void clear() {
        endTime = -1;
        startTime = -1;
        totalEvents = 0;
        noiseEvents = 0;
        failedPairEvents = 0;
        failedClusterEvents = 0;
        failedSinglesEvents = 0;
    }
    
    @Override
    public GeneralStatModule clone() {
        // Create the a cloned object.
        GeneralStatModule clone = new GeneralStatModule();
        
        // Copy the tracked statistical data to the clone.
        clone.endTime             = endTime;
        clone.startTime           = startTime;
        clone.totalEvents         = totalEvents;
        clone.noiseEvents         = noiseEvents;
        clone.failedPairEvents    = failedPairEvents;
        clone.failedClusterEvents = failedClusterEvents;
        clone.failedSinglesEvents = failedSinglesEvents;
        
        // Return the clone.
        return clone;
    }
    
    /**
     * Gets the length of time, in nanoseconds, over which the events
     * represented by this object occurred.
     * @return Returns the length of time as a <code>long</code>.
     */
    public long getDuration() {
        return endTime - startTime;
    }
    
    /**
     * Gets the number of events seen.
     * @return Returns the number of events as an <code>int</code>.
     */
    public int getEventCount() {
        return totalEvents;
    }
    
    /**
     * Gets the number of events in which at least one cluster was
     * not matched.
     * @return Returns the number of events as an <code>int</code>.
     */
    public int getFailedClusterEventCount() {
        return failedClusterEvents;
    }
    
    /**
     * Gets the number of events in which at least one pair trigger
     * was not matched.
     * @return Returns the number of events as an <code>int</code>.
     */
    public int getFailedPairEventCount() {
        return failedPairEvents;
    }

    /**
     * Gets the number of events in which at least one singles trigger
     * was not matched.
     * @return Returns the number of events as an <code>int</code>.
     */
    public int getFailedSinglesEventCount() {
        return failedSinglesEvents;
    }
    
    /**
     * Gets the number of events which were ignored due to having too
     * many hits in them.
     * @return Returns the number of events as an <code>int</code>.
     */
    public int getNoiseEvents() {
        return noiseEvents;
    }

}
