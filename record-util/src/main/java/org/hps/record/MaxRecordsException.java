package org.hps.record;

/**
 * Exception thrown when maximum number of records is reached.
 */
// FIXME: Use loop(nevents) instead of this for controlling number of records run.
public class MaxRecordsException extends RuntimeException {

    long maxRecords;
    
    public MaxRecordsException(String message, long maxRecords) {
        super(message);
        this.maxRecords = maxRecords;
    }
    
    /**
     * Get the maximum number of records.
     * @return The maximum number of records.
     */
    public long getMaxRecords() {
        return maxRecords;
    }
}
