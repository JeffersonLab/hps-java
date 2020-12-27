package org.hps.online.recon.eventbus;

public class StopProcessing {

    String reason;

    StopProcessing(String reason) {
        this.reason = reason;
    }

}
