package org.hps.online.recon.eventbus;

public class EventProcessingError {

    Exception e;
    boolean fatal;

    public EventProcessingError(Exception e, boolean fatal) {
        this.e = e;
    }

    Exception getException() {
        return this.e;
    }
}
