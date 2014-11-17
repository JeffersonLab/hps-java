package org.hps.record.composite;

import java.io.IOException;
import java.nio.BufferUnderflowException;

import org.freehep.record.loop.RecordEvent;
import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.record.EndRunException;
import org.hps.record.RecordProcessingException;
import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * An adapter for directly using the CompositeLoop to supply and process EvioEvents.
 */
public class EvioEventAdapter extends RecordProcessorAdapter<EvioEvent> {
    
    AbstractRecordSource source;
    boolean stopOnEndRun = true;
       
    /**
     * Constructor that takes a record source. 
     * @param source The record source.
     */
    public EvioEventAdapter(AbstractRecordSource source) {
        this.source = source;
    }
    
    /**
     * No argument constructor for when ET will be converted to EVIO.
     */
    public EvioEventAdapter() {
    }
        
    /**
     * Set whether to stop when end run records are received.
     * @param stopOnEndRun True to stop on end run EVIO records.
     */
    public void setStopOnEndRun(boolean stopOnEndRun) {
        this.stopOnEndRun = stopOnEndRun;
    }
    
    /**
     * Process one record which will create an <code>EvioEvent</code> or
     * get it from the source and set a reference to it on the {@link CompositeRecord}.
     */
    public void recordSupplied(RecordEvent record) {
        CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        try {
            EvioEvent evioEvent;
            // Using ET system?
            if (compositeRecord.getEtEvent() != null) {
                try {
                    // Create EVIO from ET byte buffer.
                    evioEvent = createEvioEvent(compositeRecord.getEtEvent());
                } catch (BufferUnderflowException | EvioException e) {
                    // There was a problem creating EVIO from ET.
                    throw new RecordProcessingException("Failed to create EvioEvent from EtEvent.", e);
                }
            } else {
                // Load the next record from the EVIO record source. 
                source.next();                
                evioEvent = (EvioEvent)source.getCurrentRecord();
            }
            // Failed to create an EvioEvent?
            if (evioEvent == null) {
                // Throw an error because EvioEvent was not created.
                throw new NoSuchRecordException("Failed to get next EVIO record.");
            }
            
            // Set event number on the EvioEvent.
            setEventNumber(evioEvent);
            
            // Is pre start event?
            if (EvioEventUtilities.isPreStartEvent(evioEvent)) {
                // Activate start of run hook on processors.
                startRun(evioEvent);
            // Is end run event?
            } else if (EvioEventUtilities.isEndEvent(evioEvent)) {
                // Activate end of run hook on processors.
                endRun(evioEvent);
                
                // Stop on end run enabled?
                if (stopOnEndRun) {
                    // Throw exception to stop processing from end run.
                    throw new EndRunException("EVIO end event received.", evioEvent.getIntData()[1]);
                }             
            // Is physics event?
            } else if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                // Process a single physics EvioEvent.
                process(evioEvent);
            }
            
            // Set EvioEvent on CompositeRecord.
            compositeRecord.setEvioEvent(evioEvent);
        } catch (IOException | NoSuchRecordException e) {
            throw new RecordProcessingException("No next EVIO record available from source.", e);
        }  
    }
          
    /**
     * Create an EvioEvent from an EtEvent byte buffer.
     * @param etEvent The input EtEvent.
     * @return The EvioEvent created from the EtEvent.
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
                if (bank.getHeader().getTag() == EvioEventConstants.EVENTID_BANK_TAG) {
                    eventNumber = bank.getIntData()[0];
                    break;
                }
            }
        }
        if (eventNumber != -1)
            evioEvent.setEventNumber(eventNumber);
    }   
}
