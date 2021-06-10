package org.hps.readout.ecal;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Driver to run the clock in ClockSingleton. Run this driver last.
 */
public class ClockDriver extends Driver {
    public void setDt(double dt) {
        ClockSingleton.setDt(dt);
    }

    public void process(EventHeader event) {
        ClockSingleton.step();
        TriggerDriver.resetTrigger();
    }

    public void startOfData() {
        ClockSingleton.init();
    }
}
