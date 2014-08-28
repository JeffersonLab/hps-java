package org.hps.record;

import java.io.IOException;

/**
 * An Exception thrown when an end run occurs.
 */
// TODO: Add run number to this class.
public class EndRunException extends IOException {
    
    int runNumber;
    
    public EndRunException(String message, int runNumber) {
        super(message);
        this.runNumber = runNumber;
    }
    
    public int getRunNumber() {
        return runNumber;
    }
    
}
