package org.hps.readout.trigger;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.TriggerDriver;
import org.lcsim.event.EventHeader;

/**
 * Class <code>PulserReadoutDriver</code> automatically sends a
 * trigger on every <code>n</code>th event, where <code>n</code> is a
 * definable value.<br/><br/>
 * <code>PulserReadoutDriver</code> does not support method {@link
 * org.hps.readout.TriggerDriver#setDeadTime(int) setDeadTime(int)},
 * as its rate is defined entirely via the pulse rate. Thus, its dead
 * time is always, effectively, zero.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class PulserReadoutDriver extends TriggerDriver {
    private int events = 0;
    private int pulserRate = 200;
    
    @Override
    public void startOfData() {
        // Register the trigger.
        ReadoutDataManager.registerTrigger(this);
        
        // Run the superclass method.
        super.startOfData();
    }
    
    @Override
    public void process(EventHeader event) {
        events++;
        if(events == pulserRate) {
            events = 0;
            sendTrigger();
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
    
    /**
     * Sets the rate of the pulser. It will trigger every
     * <code>clockCycles</code> events, where two events is a clock cycle.
     * @param clockCycles - The rate of the pulser in events.
     */
    public void setPulserRate(int clockCycles) {
        pulserRate = clockCycles * 2;
    }
}
