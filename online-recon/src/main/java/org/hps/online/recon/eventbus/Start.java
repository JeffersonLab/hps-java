package org.hps.online.recon.eventbus;

import java.util.Date;

public class Start {

    Date date = null;

    Start(Date date) {
        if (date == null) {
            date = new Date();
        }
    }

    Date getDate() {
        return date;
    }
}
