package org.hps.record.composite;

import org.freehep.application.studio.Studio;
import org.freehep.record.loop.RecordEvent;
import org.hps.record.lcio.LcioEventQueue;

/**
 * This adapter can be used to supply LCIO EventHeader objects to JAS3
 * via a DataSource in order to drive Wired, etc.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LcioEventSupplier extends CompositeLoopAdapter {
    
    LcioEventQueue events;
    
    LcioEventSupplier(long timeoutMillis, int maxSize) {
        events = new LcioEventQueue(timeoutMillis, maxSize);
        events.setName(LcioEventSupplier.class.getName());
        Studio studio = (Studio)Studio.getApplication(); 
        if (studio != null) {
            studio.getLookup().add(events);
        }
    }
          
    public void recordSupplied(RecordEvent record) {
        CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        if (compositeRecord.getLcioEvent() != null) {
            System.out.println("LcioEventSupplier - adding event #" + compositeRecord.getLcioEvent().getEventNumber() + " to queue");
            events.addRecord(compositeRecord.getLcioEvent());
        }
    }
    
    public LcioEventQueue getLcioEventQueue() {
        return events;
    }
}
