package org.hps.monitoring.record.evio;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.loop.RecordListener;
import org.hps.evio.EventConstants;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Adapter to process <tt>EvioEvent</tt> objects using a loop.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EvioAdapter extends AbstractLoopListener implements RecordListener {

    List<EvioEventProcessor> processors = new ArrayList<EvioEventProcessor>();
    
    @Override
    public void recordSupplied(RecordEvent recordEvent) {
        Object object = recordEvent.getRecord();        
        if (object instanceof EvioEvent) {
            EvioEvent event = (EvioEvent)object;
            if (EventConstants.isPreStartEvent(event)) {
                startRun(event);
            } else if (EventConstants.isEndEvent(event)) {
                endRun(event);
            } else if (EventConstants.isPhysicsEvent(event)) {
                processEvent(event);
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
