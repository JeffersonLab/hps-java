package org.hps.monitoring.record;

import static org.freehep.record.loop.RecordLoop.Command.NEXT;

import org.freehep.record.loop.RecordLoop.Command;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.evio.LCSimEventBuilder;
import org.hps.monitoring.record.composite.CompositeRecord;
import org.hps.monitoring.record.composite.CompositeRecordProcessor;
import org.hps.monitoring.record.lcio.LcioEventQueue;
import org.hps.monitoring.record.lcio.LcioLoop;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.lcsim.util.loop.LCSimLoop;

/**
 * Processing step for building LCIO events from EVIO
 * or reading them directly from an input event file.
 */
class LcioProcessingStep extends CompositeRecordProcessor {

    LcioLoop loop = new LcioLoop();
    LCSimEventBuilder builder;
    LcioEventQueue lcioEventQueue;
    
    /**
     * Get the <code>LcioLoop</code> associated with this processing step.
     * @return The <code>LcioLoop</code> associated with this processing step.
     */
    LCSimLoop getLoop() {
        return this.loop;
    }
        
    /**
     * Set the <code>LCSimEventBuilder</code> for converting from EVIO to LCIO.
     * @param builder The converter for EVIO to LCIO.
     */
    void setEventBuilder(LCSimEventBuilder builder) {
        this.builder = builder;
    }
    
    /**
     * Set the <code>LcioEventQueue</code> event source, to be used when there
     * is no direct LCIO record source from a file.
     * @param lcioEventQueue The <code>LcioEventQueue</code> to be used as a record source.
     */
    void setLcioEventQueue(LcioEventQueue lcioEventQueue) {
        this.lcioEventQueue = lcioEventQueue;
        loop.setRecordSource(lcioEventQueue);
    }
    
    /**
     * Start of job hook.
     */
    public void startJob() {
        if (builder == null)
            throw new RuntimeException("No LCSimEventBuilder was setup.");
    }
    
    /**
     * Process a <code>CompositeRecord</code> event by creating an LCIO event
     * and adding it to the record.
     */
    public void processEvent(CompositeRecord record) throws Exception {
        
        // When the loop does not have a direct LCIO file source, 
        // the events need to be built from the EVIO input.
        if (lcioEventQueue != null) {
            
            EvioEvent currentEvioEvent = record.getEvioEvent();
            
            // Set state on LCIO event builder.
            builder.readEvioEvent(currentEvioEvent);
            
            // The LCIO event will be built if processing an EVIO physics event.
            if (builder.isPhysicsEvent(currentEvioEvent)) {
                
                // Use the event builder to create the next LCIO event.
                EventHeader lcioEvent = builder.makeLCSimEvent(currentEvioEvent);
    
                // Add LCIO event to the queue.
                lcioEventQueue.addRecord(lcioEvent);
            } else {
                // The LCIO processing ignores non-physics events coming from EVIO.
                return;
            }
        }
            
        // Is there a next record?
        if (!loop.getRecordSource().hasNext())
            // The next record does not exist.
            throw new NoSuchRecordException("No next LCIO event.");
        
        // Load the next LCIO event, triggering Driver process methods.
        loop.execute(NEXT);
            
        // Is there a current record?
        if (loop.getRecordSource().getCurrentRecord() == null) {
            // The last call to the loop did not create a current record.
            throw new NoSuchRecordException("No current LCIO event.");
        }
        
        // Get the current LCIO event.
        EventHeader lcioEvent = (EventHeader) loop.getRecordSource().getCurrentRecord();
                
        // Update the CompositeRecord with a reference to the LCIO event.
        record.setLcioEvent(lcioEvent);
        // Was there an EVIO event set?
        if (record.getEvioEvent() == null) {
            // Set event number from LCIO if no EVIO event was set.
            record.setEventNumber(lcioEvent.getEventNumber());
        }
    }
    
    /**
     * End of job hook.
     */
    public void endJob() {
        // Execute STOP on the LCIO record loop.
        loop.execute(Command.STOP);
    }
}