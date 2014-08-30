package org.hps.record.evio;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.hps.evio.EventConstants;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Adapter to process <tt>EvioEvent</tt> objects using a record loop.
 */
public final class EvioAdapter extends AbstractLoopListener implements RecordListener {

    List<EvioProcessor> processors = new ArrayList<EvioProcessor>();
    
    void addEvioEventProcessor(EvioProcessor processor) {
        processors.add(processor);
    }
   
    @Override
    public void recordSupplied(RecordEvent recordEvent) {
        Object object = recordEvent.getRecord();        
        if (object instanceof EvioEvent) {
            EvioEvent event = (EvioEvent)object;
            if (EventConstants.isPreStartEvent(event)) {
                // Start of run.
                startRun(event);
            } else if (EventConstants.isEndEvent(event)) {
                // End of run.
                endRun(event);
            } else if (EventConstants.isPhysicsEvent(event)) {
                // Process one physics event.
                processEvent(event);
            }
        }
    }
     
    @Override
    public void start(LoopEvent event) {        
        for (EvioProcessor processor : processors) {
            processor.startJob();
        }
    }
    
    @Override
    public void finish(LoopEvent event) {
        for (EvioProcessor processor : processors) {
            processor.endJob();
        }
    }    
    
    @Override
    public void suspend(LoopEvent event) {
        System.out.println("EvioAdapter.suspend");        
        if (event.getException() != null) {
            for (EvioProcessor processor : processors) {
                processor.endJob();
            }
        }
    }
        
    private void processEvent(EvioEvent event) {
        for (EvioProcessor processor : processors) {
            processor.process(event);
        }
    }
    
    private void startRun(EvioEvent event) {
        for (EvioProcessor processor : processors) {
            processor.startRun(event);
        }
    }
    
    private void endRun(EvioEvent event) {
        for (EvioProcessor processor : processors) {
            processor.endRun(event);
        }
    }    
}
