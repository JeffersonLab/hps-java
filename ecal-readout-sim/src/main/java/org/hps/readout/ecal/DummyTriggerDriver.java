package org.hps.readout.ecal;

import org.lcsim.event.EventHeader;

/**
 * Free-running trigger; triggers on every Nth event.
 */
public class DummyTriggerDriver extends TriggerDriver {

    int period = 100;

    public void setPeriod(int period) {
        this.period = period;
    }

    @Override
    public boolean triggerDecision(EventHeader event) {
        return (ClockSingleton.getClock() % period == 0);
    }
}
