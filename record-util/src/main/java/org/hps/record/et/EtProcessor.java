package org.hps.record.et;

import org.hps.record.RecordProcessor;
import org.jlab.coda.et.EtEvent;

/**
 * This is the basic abstract class that processors of 
 * <tt>EtEvent</tt> objects should implement.
 */
public abstract class EtProcessor implements RecordProcessor<EtEvent> {
    
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
    public void process(EtEvent event) {
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