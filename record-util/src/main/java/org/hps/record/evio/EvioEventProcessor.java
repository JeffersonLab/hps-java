package org.hps.record.evio;

import org.hps.record.EventProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is the basic abstract class that processors of 
 * <tt>EvioEvent</tt> objects should implement.
 */
public abstract class EvioEventProcessor implements EventProcessor<EvioEvent> {
    
    @Override
    public void startJob() {
    }
    
    @Override
    public void startRun(EvioEvent event) { 
    }
    
    @Override
    public void processEvent(EvioEvent event) {
    }
            
    @Override
    public void endRun(EvioEvent event) {
    }
    
    @Override
    public void endJob() {        
    }
}