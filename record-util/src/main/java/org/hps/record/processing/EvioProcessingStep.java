package org.hps.record.processing;

import static org.freehep.record.loop.RecordLoop.Command.NEXT;

import java.io.IOException;
import java.nio.BufferUnderflowException;

import org.freehep.record.loop.RecordLoop.Command;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.evio.EventConstants;
import org.hps.record.composite.CompositeRecord;
import org.hps.record.composite.CompositeProcessor;
import org.hps.record.evio.EvioLoop;
import org.hps.record.evio.EvioRecordQueue;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * EVIO processing step to build an <tt>EvioEvent</tt> from the <tt>EtEvent</tt>
 * or load the next <tt>EvioEvent</tt> from a file, if using an EVIO file source.
 */
class EvioProcessingStep extends CompositeProcessor {
   
    EvioLoop loop = new EvioLoop();
    DataSourceType sourceType;
    EvioRecordQueue evioEventQueue;
    boolean stopOnEndRun;
    
    /**
     * Get the <tt>EvioEventLoop</tt> associated with this processing step.
     * @return The <tt>EvioEventLoop</tt> associated with this processing step.
     */
    EvioLoop getLoop() {
        return loop;
    }

    /**
     * Set the EVIO event queue.
     * @param evioEventQueue The EVIO event queue.
     */
    void setEvioEventQueue(EvioRecordQueue evioEventQueue) {
        this.evioEventQueue = evioEventQueue;
        loop.setRecordSource(this.evioEventQueue);
    }
    
    /**
     * Set whether an end of run record will throw a control Exception.
     */
    void setStopOnEndRun() {
        stopOnEndRun = true;
    }
    
    /**
     * Set the <tt>EvioEventLoop</tt> for this processing step.
     * @param loop The <tt>EvioEventLoop</tt> for this processing step.
     */
    void setEvioEventLoop(EvioLoop loop) {
        this.loop = loop;
    }
    
    /**
     * Set the type of event source.
     * @param sourceType The type of event source.
     */
    void setSourceType(DataSourceType sourceType) {
        this.sourceType = sourceType;
    }
    
    /**
     * Load the next <tt>EvioEvent</tt>, either from a record source
     * or from the <tt>EtEvent</tt> data.
     */
    public void process(CompositeRecord record) throws Exception {
                
        if (evioEventQueue != null) {
            EvioEvent evioEvent = null;
            try {
                evioEvent = createEvioEvent(record.getEtEvent());                    
                if (evioEvent == null)
                    throw new IOException("Failed to create EvioEvent from current EtEvent.");
                setEventNumber(evioEvent);
            } catch (EvioException e) {
                throw new IOException(e);
            }
        
            // Add EvioEvent to the queue for loop.
            evioEventQueue.addRecord(evioEvent);
        }

        // Process one EvioEvent.
        loop.execute(NEXT);
        if (loop.getErrorState().hasError())
            loop.getErrorState().rethrow();
        
        EvioEvent nextEvioEvent = (EvioEvent) loop.getRecordSource().getCurrentRecord();
        
        // The call to loop did not create a current record.
        if (nextEvioEvent == null)
            throw new NoSuchRecordException("No current EVIO event.");
        
        // Update the CompositeRecord.
        record.setEvioEvent(nextEvioEvent);
        record.setEventNumber(nextEvioEvent.getEventNumber());
    }
    
    /**
     * Create an <tt>EvioEvent</tt> from the current <tt>EtEvent</tt>.
     * @param etEvent
     * @return The created EVIO event.
     * @throws IOException
     * @throws EvioException
     * @throws BufferUnderflowException
     */
    private EvioEvent createEvioEvent(EtEvent etEvent) 
            throws IOException, EvioException, BufferUnderflowException {
        return (new EvioReader(etEvent.getDataBuffer())).parseNextEvent();
    }
    
    /**
     * Set the EVIO event number manually from the event ID bank.
     * @param evioEvent The <tt>EvioEvent</tt> on which to set the event number.
     */
    private void setEventNumber(EvioEvent evioEvent) {
        int eventNumber = -1;
        if (evioEvent.getChildren() != null) {
            for (BaseStructure bank : evioEvent.getChildren()) {
                if (bank.getHeader().getTag() == EventConstants.EVENTID_BANK_TAG) {
                    eventNumber = bank.getIntData()[0];
                    break;
                }
            }
        }
        if (eventNumber != -1)
            evioEvent.setEventNumber(eventNumber);
    }
    
    /**
     * End the job by calling stop on the EVIO processing loop.
     */
    public void endJob() {
        this.loop.execute(Command.STOP);
    }
}