package org.hps.monitoring.record.etevent;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.jlab.coda.et.EtEvent;

/**
 * Adapter for processing <tt>EtEvent</tt> objects using a loop.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtEventAdapter extends AbstractLoopListener implements RecordListener {

    List<EtEventProcessor> processors = new ArrayList<EtEventProcessor>();
    
    @Override
    public void recordSupplied(RecordEvent recordEvent) {
        Object object = recordEvent.getRecord();        
        if (object instanceof EtEvent) {
            EtEvent event = (EtEvent)object;
            processEvent(event);            
        }
    }
    
    @Override
    public void start(LoopEvent event) {
        for (EtEventProcessor processor : processors) {
            processor.start();
        }
    }
    
    @Override
    public void finish(LoopEvent event) {
        for (EtEventProcessor processor : processors) {
            processor.stop();
        }            
    }    
    
    void addEtEventProcessor(EtEventProcessor processor) {
        processors.add(processor);
    }
    
    private void processEvent(EtEvent event) {
        for (EtEventProcessor processor : processors) {
            processor.processEvent(event);
        }
    }              
}
