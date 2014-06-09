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
    public void start() {
    }
    
    /**
     * Process one <tt>EtEvent</tt>.
     */
    @Override
    public void processEvent(EtEvent event) {
    }    
    
    /**
     * End of ET session.
     */
    public void stop() {        
    }
    
    // from EtEventListener
    //
    // void begin();    
    // void startOfEvent();
    // void endOfEvent();
    // void errorOnEvent();
    // void finish();
    // void prestart(int seconds, int runNumber);    
    // void endRun(int seconds, int nevents);
}