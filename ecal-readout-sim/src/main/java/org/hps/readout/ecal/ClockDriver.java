package org.hps.readout.ecal;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Driver to run the clock in ClockSingleton. Run this driver last.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: ClockDriver.java,v 1.2 2011/10/07 23:14:55 meeg Exp $
 */
public class ClockDriver extends Driver {
    public void setDt(double dt) {
        ClockSingleton.setDt(dt);
    }

    public void process(EventHeader event) {
        ClockSingleton.step();
    }

    public void startOfData() {
        ClockSingleton.init();
    }
}
