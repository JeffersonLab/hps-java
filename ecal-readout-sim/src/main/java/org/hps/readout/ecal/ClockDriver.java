package org.hps.readout.ecal;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Class <code>ClockDriver</code> is used to manage the readout
 * simulation time object {@link org.hps.readout.ecal.ClockSingleton
 * ClockSingleton}. It increments the total simulation every event
 * by an amount specified in the steering file, or 2 ns by default.
 * </br/><br/>
 * Note: This driver must be run <b>last</b> to ensure that all of
 * the drivers in the event chain receive the same simulation time if
 * they access the simulation clock.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class ClockDriver extends Driver {
    @Override
    public void process(EventHeader event) {
        ClockSingleton.step();
        TriggerDriver.resetTriggerBit();
    }
    
    @Override
    public void startOfData() {
        ClockSingleton.init();
    }
    
    /**
     * Sets the step size of the simulation clock. The value is in
     * units of nanoseconds. This must be equal to the size of the
     * beam bunches in the input Monte Carlo. By default, this is set
     * to 2 ns.
     * @param stepSize - The beam bunch size in units of nanoseconds.
     */
    public void setStepSize(double stepSize) {
        ClockSingleton.setStepSize(stepSize);
    }
}