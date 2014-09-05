package org.hps.record;

/**
 * Generic error type for exceptions that occur during event processing.
 * It extends <code>RuntimeException</code> so that methods need not
 * declare a <code>throws</code> clause in their definitions to use it.
 */
public class RecordProcessingException extends RuntimeException {    
    
    public RecordProcessingException(String message, Throwable x) {
        super(message, x);
    }
    
}
