package org.hps.online.recon.eventbus;

public class EndRun {

    Integer run = null;

    EndRun(Integer run) {
        this.run = run;
    }

    Integer getRun() {
        return run;
    }

}
