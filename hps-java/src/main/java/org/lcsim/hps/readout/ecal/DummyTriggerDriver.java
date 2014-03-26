package org.lcsim.hps.readout.ecal;

import org.lcsim.event.EventHeader;
import org.hps.util.ClockSingleton;

/**
 * Free-running trigger - triggers on every Nth event
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: DummyTriggerDriver.java,v 1.3 2013/04/02 01:11:11 meeg Exp $
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
