package org.hps.online.recon.eventbus;

/**
 * Object posted to the event bus to indicate
 * that event processing should be stopped.
 */
public class Stop {

    String reason;

    Stop(String reason) {
        if (reason == null) {
            throw new IllegalArgumentException("The reason argument is null");
        }
        this.reason = reason;
    }

    String getReason() {
        return reason;
    }
}
