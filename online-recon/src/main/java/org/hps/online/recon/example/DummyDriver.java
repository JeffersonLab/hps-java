package org.hps.online.recon.example;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class DummyDriver extends Driver {

    public void process(EventHeader event) {
        System.out.println("DummyDriver.process: event=" + event.getEventNumber());
    }
}
