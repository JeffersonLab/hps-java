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

    List<EvioEventProcessor> processors = new ArrayList<EvioEventProcessor>();
    
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
        for (EvioEventProcessor processor : processors) {
            processor.startJob();
        }
    }
    
    @Override
    public void finish(LoopEvent event) {
        //System.out.println("EvioAdapter.finish");
        for (EvioEventProcessor processor : processors) {
            //System.out.println(processor.getClass().getCanonicalName() + ".endJob");
            processor.endJob();
        }
    }    
    
    // NOTE: This is called between every execution of the GO_N command!!!
    public void suspend(LoopEvent event) {
        //System.out.println("EvioAdapter.suspend");        
        if (event.getException() != null) {
            //System.out.println("current error: " + event.getException().getMessage());
            //System.out.println("ending job from suspend");
            for (EvioEventProcessor processor : processors) {
                processor.endJob();
            }
        }
    }
    
    void addEvioEventProcessor(EvioEventProcessor processor) {
        processors.add(processor);
    }
    
    private void processEvent(EvioEvent event) {
        for (EvioEventProcessor processor : processors) {
            processor.processEvent(event);
        }
    }
    
    private void startRun(EvioEvent event) {
        for (EvioEventProcessor processor : processors) {
            processor.startRun(event);
        }
    }
    
    private void endRun(EvioEvent event) {
        for (EvioEventProcessor processor : processors) {
            processor.endRun(event);
        }
    }    
}
