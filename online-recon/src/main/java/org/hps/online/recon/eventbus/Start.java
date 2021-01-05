package org.hps.online.recon.eventbus;

import java.util.Date;

public class Start {

    Date date = null;

    Start(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Date argument is null");
        }
        this.date = date;
    }

    Start() {
        this.date = new Date();
    }

    Date getDate() {
        return date;
    }
}
