package org.hps.record.composite;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordEvent;
import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.evio.EventConstants;
import org.hps.record.EndRunException;
import org.hps.record.RecordProcessingException;
import org.hps.record.evio.EvioProcessor;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * An adapter for directly using the CompositeLoop to supply and process EvioEvents.
 */
public class CompositeEvioAdapter extends CompositeLoopAdapter {
    
    AbstractRecordSource source;
    List<EvioProcessor> processors = new ArrayList<EvioProcessor>();
    boolean stopOnEndRun = true;
       
    public CompositeEvioAdapter(AbstractRecordSource source) {
        this.source = source;
    }
    
    public CompositeEvioAdapter() {
    }
    
    public void addProcessor(EvioProcessor processor) {
        processors.add(processor);
    }
    
    public void setStopOnEndRun(boolean stopOnEndRun) {
        this.stopOnEndRun = stopOnEndRun;
    }
    
    public void recordSupplied(RecordEvent record) {
        //System.out.println("CompositeEvioAdapter.recordSupplied");
        System.out.flush();
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
                throw new NoSuchRecordException("Failed to get next EVIO record.");
            }
            
            // Set event number on the EvioEvent.
            setEventNumber(evioEvent);
            
            // Is pre start event?
            if (EventConstants.isPreStartEvent(evioEvent)) {
                // Activate start of run hook on processors.
                startRun(evioEvent);
            // Is end run event?
            } else if (EventConstants.isEndEvent(evioEvent)) {
                // Activate end of run hook on processors.
                endRun(evioEvent);
                
                // Stop on end run enabled?
                if (stopOnEndRun) {
                    throw new EndRunException("EVIO end event received.", evioEvent.getIntData()[1]);
                }             
            // Is physics event?
            } else if (EventConstants.isPhysicsEvent(evioEvent)) {
                // Process a single physics EvioEvent.
                process(evioEvent);
            }
            
            // Set EvioEvent on CompositeRecord.
            compositeRecord.setEvioEvent(evioEvent);
        } catch (IOException | NoSuchRecordException e) {
            throw new RecordProcessingException("No next EVIO record available from source.", e);
        }  
    }
    
    void process(EvioEvent event) {
        for (EvioProcessor processor : processors) {
            processor.process(event);
        }
    }
    
    void startRun(EvioEvent event) {
        for (EvioProcessor processor : processors) {
            processor.startRun(event);
        }
    }
    
    void endRun(EvioEvent event) {
        for (EvioProcessor processor : processors) {
            processor.endRun(event);
        }
    }     
        
    public void finish(LoopEvent event) {
        for (EvioProcessor processor : processors) {
            processor.endJob();
        }
    }
    
    public void start(LoopEvent event) {
        for (EvioProcessor processor : processors) {
            processor.startJob();
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
                if (bank.getHeader().getTag() == EventConstants.EVENTID_BANK_TAG) {
                    eventNumber = bank.getIntData()[0];
                    break;
                }
            }
        }
        if (eventNumber != -1)
            evioEvent.setEventNumber(eventNumber);
    }   
}
