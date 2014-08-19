package org.hps.record.etevent;

import org.hps.record.EventProcessor;
import org.jlab.coda.et.EtEvent;

/**
 * This is the basic abstract class that processors of 
 * <tt>EtEvent</tt> objects should implement.
 */
public abstract class EtEventProcessor implements EventProcessor<EtEvent> {
    
    /**
     * Start of ET session.
     */
    @Override
    public void startJob() {
    }
    
    @Override
    public void startRun(EtEvent event) {
        
    }
    
    /**
     * Process one <tt>EtEvent</tt>.
     */
    @Override
    public void processEvent(EtEvent event) {
    }    
    
    @Override
    public void endRun(EtEvent event) {
        
    }
    
    /**
     * End of ET session.
     */
    @Override
    public void endJob() {
    }    
}