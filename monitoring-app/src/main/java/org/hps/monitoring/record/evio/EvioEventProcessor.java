package org.hps.monitoring.record.evio;

import org.hps.monitoring.record.EventProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is the basic abstract class that processors of 
 * <tt>EvioEvent</tt> objects should implement.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Add handling for all event types (see EtEventListener).
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