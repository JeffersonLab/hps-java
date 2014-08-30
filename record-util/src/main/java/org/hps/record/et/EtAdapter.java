package org.hps.record.et;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.hps.record.RecordProcessingException;
import org.jlab.coda.et.EtEvent;

/**
 * Adapter for processing <tt>EtEvent</tt> objects using a loop.
 */
public final class EtAdapter extends AbstractLoopListener implements RecordListener {

    List<EtProcessor> processors = new ArrayList<EtProcessor>();
    
    void addEtEventProcessor(EtProcessor processor) {
        processors.add(processor);
    }
    
    @Override
    public void recordSupplied(RecordEvent recordEvent) {
        Object object = recordEvent.getRecord();        
        if (object instanceof EtEvent) {
            EtEvent event = (EtEvent)object;
            processEvent(event);            
        }
    }
    
    @Override
    public void suspend(LoopEvent event) {
        if (event.getException() != null)
            throw new RecordProcessingException("ET system error.", event.getException());
    }
    
    @Override
    public void start(LoopEvent event) {
        for (EtProcessor processor : processors) {
            processor.startJob();
        }
    }
    
    @Override
    public void finish(LoopEvent event) {
        for (EtProcessor processor : processors) {
            processor.endJob();
        }            
    }    
        
    private void processEvent(EtEvent event) {
        for (EtProcessor processor : processors) {
            processor.process(event);
        }
    }              
}