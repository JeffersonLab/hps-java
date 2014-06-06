package org.hps.monitoring.record.evio;

import org.hps.monitoring.record.EventProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is the basic abstract class that processors of 
 * <tt>EvioEvent</tt> objects should implement.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class EvioEventProcessor implements EventProcessor<EvioEvent> {
        
    @Override
    public void processEvent(EvioEvent event) {
    }
    
    public void startRun(EvioEvent event) { 
    }
    
    public void endRun(EvioEvent event) {
    }
}