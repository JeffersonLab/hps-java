package org.hps.readout.ecal.updated;

import org.hps.readout.TriggerDriver;
import org.lcsim.event.EventHeader;

/**
 * Class <code>PulserTriggerDriver</code> automatically sends a
 * trigger on every <code>n</code>th event, where <code>n</code> is a
 * definable value.<br/><br/>
 * <code>PulserTriggerDriver</code> does not support method {@link
 * org.hps.readout.TriggerDriver#setDeadTime(int) setDeadTime(int)},
 * as its rate is defined entirely via the pulse rate. Thus, its dead
 * time is always, effectively, zero.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class PulserTriggerDriver extends TriggerDriver {
    private int eventCount = 0;
    private int pulseRate = 100;
    
    @Override
    public void process(EventHeader event) {
        if(++eventCount == pulseRate) {
            sendTrigger();
            eventCount = 0;
        }
    }
    
    @Override
    protected double getTimeDisplacement() {
        return 0;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }
    
    @Override
    public void setDeadTime(int value) {
        throw new UnsupportedOperationException("Error: Pulser triggers do not support dead time.");
    }
    
    /**
     * Sets the rate of the pulser. It will trigger every
     * <code>events</code> events.
     * @param events - The rate of the pulser in events.
     */
    public void setRate(int events) {
        pulseRate = events;
    }
}