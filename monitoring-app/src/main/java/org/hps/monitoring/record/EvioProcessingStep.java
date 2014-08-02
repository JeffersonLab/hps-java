package org.hps.monitoring.record;

import static org.freehep.record.loop.RecordLoop.Command.NEXT;

import java.io.IOException;
import java.nio.BufferUnderflowException;

import org.freehep.record.loop.RecordLoop.Command;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.evio.EventConstants;
import org.hps.monitoring.enums.DataSourceType;
import org.hps.monitoring.record.composite.CompositeRecord;
import org.hps.monitoring.record.composite.CompositeRecordProcessor;
import org.hps.monitoring.record.evio.EvioEventLoop;
import org.hps.monitoring.record.evio.EvioEventQueue;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * EVIO processing step to build an <tt>EvioEvent</tt> from the <tt>EtEvent</tt>
 * or load the next <tt>EvioEvent</tt> from a file, if using an EVIO file source.
 */
class EvioProcessingStep extends CompositeRecordProcessor {
   
    EvioEventLoop loop = new EvioEventLoop();
    DataSourceType sourceType;
    EvioEventQueue evioEventQueue;
    boolean stopOnEndRun;

    // FIXME: Should this really be extending IOException?
    class EndRunException extends IOException {
        EndRunException(String message) {
            super(message);
        }
    }
    
    /**
     * Get the <tt>EvioEventLoop</tt> associated with this processing step.
     * @return The <tt>EvioEventLoop</tt> associated with this processing step.
     */
    EvioEventLoop getLoop() {
        return loop;
    }

    /**
     * Set the EVIO event queue.
     * @param evioEventQueue The EVIO event queue.
     */
    void setEvioEventQueue(EvioEventQueue evioEventQueue) {
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
    void setEvioEventLoop(EvioEventLoop loop) {
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
    public void processEvent(CompositeRecord record) throws Exception {
                
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
        EvioEvent nextEvioEvent = (EvioEvent) loop.getRecordSource().getCurrentRecord();
        
        // The call to loop did not create a current record.
        if (nextEvioEvent == null)
            throw new NoSuchRecordException("No current EVIO event.");
        
        record.setEvioEvent(nextEvioEvent);
        record.setEventNumber(nextEvioEvent.getEventNumber());
        
        // Encountered an end of run record.
        if (EventConstants.isEndEvent(nextEvioEvent))
            // If stop on end run is enabled, then trigger an exception to end processing.
            if (stopOnEndRun)
                throw new EndRunException("EVIO end event received, and stop on end run is enabled.");
        
        //System.out.println("done with EvioProcessingStep.processEvent");
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