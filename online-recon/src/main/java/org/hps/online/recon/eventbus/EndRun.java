package org.hps.online.recon.eventbus;

import java.util.Date;

/**
 * Object posted to the event bus indicating
 * end of run occurred.
 */
public class EndRun {

    Integer run = null;
    Date date = null;

    EndRun(Integer run, Date date) {
        if (run == null) {
            throw new IllegalArgumentException("The run argument is null");
        }
        this.run = run;
        this.date = date;
        if (this.date == null) {
            this.date = new Date();
        }
    }

    Integer getRun() {
        return run;
    }

    Date getDate() {
        return date;
    }

}
