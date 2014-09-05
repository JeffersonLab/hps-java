package org.hps.record;

/**
 * Exception thrown when maximum number of records is reached.
 */
// FIXME: Use loop(nevents) instead of this for controlling number of records run.
public class MaxRecordsException extends Exception {

    int maxRecords;
    
    public MaxRecordsException(String message, int maxRecords) {
        super(message);
        this.maxRecords = maxRecords;
    }
    
    /**
     * Get the maximum number of records.
     * @return The maximum number of records.
     */
    public int getMaxRecords() {
        return maxRecords;
    }
}
