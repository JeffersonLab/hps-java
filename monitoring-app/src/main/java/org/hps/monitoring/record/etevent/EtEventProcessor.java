package org.hps.monitoring.record.etevent;

import org.hps.monitoring.record.EventProcessor;
import org.jlab.coda.et.EtEvent;

/**
 * This is the basic abstract class that processors of 
 * <tt>EtEvent</tt> objects should implement.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
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