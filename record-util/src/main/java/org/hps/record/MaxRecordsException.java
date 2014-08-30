package org.hps.record;

/**
 * Exception to be thrown when maximum number of records is reached.
 */
public class MaxRecordsException extends Exception {

    int maxRecords;
    
    public MaxRecordsException(String message, int maxRecords) {
        super(message);
        this.maxRecords = maxRecords;
    }
    
    public int getMaxRecords() {
        return maxRecords;
    }
}
