package org.hps.record;

public class RecordProcessingException extends RuntimeException {
    
    public RecordProcessingException(String message, Throwable x) {
        super(message, x);
    }
    
}
