package org.hps.monitoring.record;

import static org.freehep.record.loop.RecordLoop.Command.NEXT;

import org.freehep.record.loop.RecordLoop.Command;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.monitoring.record.composite.CompositeRecord;
import org.hps.monitoring.record.composite.CompositeRecordProcessor;
import org.hps.monitoring.record.etevent.EtEventLoop;
import org.jlab.coda.et.EtEvent;

 /**
  * ET processing step to load an <tt>EtEvent</tt> from the ET ring.
  */
class EtProcessingStep extends CompositeRecordProcessor {
 
    EtEventLoop loop = new EtEventLoop();
    
    EtProcessingStep() {
    }
    
    EtEventLoop getLoop() {
        return loop;
    }
    
    public void startJob() {        
        if (loop == null)
            throw new RuntimeException();
    }
    
    public void processEvent(CompositeRecord record) throws Exception {
            
        // Load the next EtEvent, which calls getEvents() on the ET connection
        // and feeds records to any loop listeners like status monitors.
        loop.execute(NEXT);
            
        // Get the current EtEvent from the loop, which should have been cached.
        EtEvent nextEtEvent = (EtEvent) loop.getRecordSource().getCurrentRecord();
            
        // Failed to read an EtEvent from the ET server.
        if (nextEtEvent == null)
            throw new NoSuchRecordException("No current EtEvent is available.");
            
        // Update the CompositeRecord with reference to the current EtEvent.
        record.setEtEvent(nextEtEvent);
     }
    
    public void endJob() {
        loop.execute(Command.STOP);
    }
}