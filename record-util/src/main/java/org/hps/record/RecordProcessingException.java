package org.hps.record;

/**
 * Error type for exceptions that occur during event processing.
 */
public class RecordProcessingException extends RuntimeException {    
    public RecordProcessingException(String message, Throwable x) {
        super(message, x);
    }
}
