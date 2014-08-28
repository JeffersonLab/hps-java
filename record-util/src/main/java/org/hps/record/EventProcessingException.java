package org.hps.record;

public class EventProcessingException extends RuntimeException {
    
    public EventProcessingException(String message, Throwable x) {
        super(message, x);
    }
    
}
