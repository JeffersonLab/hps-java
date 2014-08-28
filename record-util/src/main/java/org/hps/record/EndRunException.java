package org.hps.record;

import java.io.IOException;

/**
 * An Exception thrown when an end run occurs.
 */
// TODO: Add run number to this class.
public class EndRunException extends IOException {
    public EndRunException(String message) {
        super(message);
    }
}
