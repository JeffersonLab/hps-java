package org.hps.record;

/**
 * An Exception thrown when an end run occurs.
 */
public class EndRunException extends RuntimeException {
    
    int runNumber;
    
    public EndRunException(String message, int runNumber) {
        super(message);
        this.runNumber = runNumber;
    }
    
    public int getRunNumber() {
        return runNumber;
    }
    
}
