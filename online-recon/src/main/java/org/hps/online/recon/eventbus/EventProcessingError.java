package org.hps.online.recon.eventbus;

/**
 * Error object for posting to the event bus.
 */
public class EventProcessingError {

    private Exception e;
    private boolean fatal;

    public EventProcessingError(Exception e, boolean fatal) {
        this.e = e;
    }

    Exception getException() {
        return this.e;
    }

    boolean fatal() {
        return fatal;
    }
}
