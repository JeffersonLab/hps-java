package org.hps.readout.ecal.updated;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.TriggerDriver;
import org.lcsim.event.EventHeader;

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
    
    public void setPulserRate(int clockCycles) {
        pulserRate = clockCycles * 2;
    }
}