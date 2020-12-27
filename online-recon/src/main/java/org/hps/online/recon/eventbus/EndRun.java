package org.hps.online.recon.eventbus;

/**
 * Object posted to the event bus indicating
 * end of run occurred.
 */
public class EndRun {

    Integer run = null;

    EndRun(Integer run) {
        this.run = run;
    }

    Integer getRun() {
        return run;
    }

}
