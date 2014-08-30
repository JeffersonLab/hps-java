package org.hps.record.processing;

import static org.freehep.record.loop.RecordLoop.Command.NEXT;

import org.freehep.record.loop.RecordLoop.Command;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.record.composite.CompositeRecord;
import org.hps.record.composite.CompositeProcessor;
import org.hps.record.et.EtLoop;
import org.jlab.coda.et.EtEvent;

 /**
  * ET processing step to load an <tt>EtEvent</tt> from the ET ring
  * using a {@link org.hps.EtLoop.record.etevent.EtEventLoop}.
  */
class EtProcessingStep extends CompositeProcessor {
 
    EtLoop loop = new EtLoop();
    
    EtProcessingStep() {
    }
    
    EtLoop getLoop() {
        return loop;
    }
    
    public void startJob() {        
        if (loop == null)
            throw new RuntimeException();
    }
    
    public void process(CompositeRecord record) throws Exception {
            
        // Load the next EtEvent, which calls getEvents() on the ET connection
        // and feeds records to any loop listeners like status monitors.
        loop.execute(NEXT);
        
        // Did an error occur while getting ET events from the network?
        if (loop.getErrorState().hasError())
            // Rethrow the error.
            loop.getErrorState().rethrow();
            
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