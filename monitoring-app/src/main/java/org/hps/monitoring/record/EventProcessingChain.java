package org.hps.monitoring.record;

import static org.freehep.record.loop.RecordLoop.Command.NEXT;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.freehep.record.loop.RecordLoop.Command;
import org.freehep.record.source.NoSuchRecordException;
import org.freehep.record.source.RecordSource;
import org.hps.evio.EventConstants;
import org.hps.evio.LCSimEventBuilder;
import org.hps.monitoring.record.composite.CompositeRecord;
import org.hps.monitoring.record.composite.CompositeRecordLoop;
import org.hps.monitoring.record.etevent.EtEventLoop;
import org.hps.monitoring.record.etevent.EtEventProcessor;
import org.hps.monitoring.record.etevent.EtEventSource;
import org.hps.monitoring.record.evio.EvioEventLoop;
import org.hps.monitoring.record.evio.EvioEventProcessor;
import org.hps.monitoring.record.evio.EvioEventQueue;
import org.hps.monitoring.record.evio.EvioFileSource;
import org.hps.monitoring.record.lcio.LcioEventQueue;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCIOEventSource;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This class provides a serial implementation of the monitoring event
 * processing chain.  This is accomplished by chaining together implementations 
 * of FreeHep's <tt>RecordLoop</tt>.
 * 
 * The processing chain can be configured to execute the ET, EVIO event building, 
 * or LCIO eventing building stages.  The source can be set to an ET ring,
 * EVIO file source, or LCIO file source.  Any number of event processors
 * can be registered for processing the different record types, in order
 * to plot, update a GUI component, or analyze the events.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EventProcessingChain {
      
    /**
     * Type of source for events.
     */
    enum SourceType {
        ET_EVENT,
        EVIO_FILE,
        LCIO_FILE
    }
    
    /**
     * Processing stages to execute.
     */
    enum ProcessingStage {
        READ_ET_EVENT,
        BUILD_EVIO_EVENT,
        BUILD_LCIO_EVENT
    }
    
    SourceType sourceType;    
    ProcessingStage processingStage = ProcessingStage.BUILD_LCIO_EVENT;    
    PrintStream logStream = System.out;    
    List<EventProcessingStep> processingSteps = new ArrayList<EventProcessingStep>();    
    RecordSource recordSource;    
    EtEventLoop etLoop = new EtEventLoop();
    EvioEventLoop evioLoop = new EvioEventLoop();
    LCSimLoop lcsimLoop = new LCSimLoop();
    CompositeRecordLoop compositeLoop = new CompositeRecordLoop();            
    EvioEventQueue evioQueue = new EvioEventQueue();
    LcioEventQueue lcioQueue = new LcioEventQueue();
    LCSimEventBuilder eventBuilder;        
    int totalEventsProcessed;
    String detectorName;
    boolean stopOnEndRun;
    boolean continueOnErrors;
    String steeringResource;
    File steeringFile;
    Exception lastException;
    volatile boolean isDone;
    volatile boolean paused;
        
    /**
     * No argument constructor.  
     * The setter methods should be used to setup this class.
     */
    public EventProcessingChain() {  
    }
        
    public void configure() {
        
        if (recordSource == null)
            throw new RuntimeException("No record source was set.");
        
        if (sourceType == SourceType.ET_EVENT) {
            this.processingSteps.add(new EtProcessingStep());            
        }
        if (processingStage.ordinal() >= ProcessingStage.BUILD_EVIO_EVENT.ordinal()) {
            if (sourceType.ordinal() <= SourceType.EVIO_FILE.ordinal()) {
                this.processingSteps.add(new EvioProcessingStep());
            }
            if (sourceType == SourceType.ET_EVENT) {
                this.evioLoop.setRecordSource(evioQueue);   
            }
        }
        if (processingStage.ordinal() >= ProcessingStage.BUILD_LCIO_EVENT.ordinal()) {
            if (this.eventBuilder != null)
                this.eventBuilder.setDetectorName(detectorName);
            this.processingSteps.add(new LcioProcessingStep());
            if (sourceType.ordinal() != SourceType.LCIO_FILE.ordinal()) {
                this.lcsimLoop.setRecordSource(lcioQueue);
            }
        }
        
        // Setup the composite loop.
        compositeLoop.addProcessingSteps(processingSteps);
        compositeLoop.registerRecordLoop(etLoop);
        compositeLoop.registerRecordLoop(evioLoop);
        compositeLoop.registerRecordLoop(lcsimLoop);
    }
    
    void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }
        
    public void setSteeringFile(File steeringFile) {
        this.steeringFile = steeringFile;
    }
    
    public void setSteeringResource(String steeringResource) {
        this.steeringResource = steeringResource;
    }
    
    public void setProcessingStage(ProcessingStage processingStage) {
        this.processingStage = processingStage;
    }
            
    public void setEventBuilder(LCSimEventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
    }
    
    public void setRecordSource(EtEventSource recordSource) {
        this.recordSource = recordSource;
        this.etLoop.setRecordSource(recordSource);
        this.sourceType = SourceType.ET_EVENT;
    }
    
    public void setRecordSource(EvioFileSource recordSource) {
        this.recordSource = recordSource;
        this.evioLoop.setRecordSource(recordSource);
        setSourceType(SourceType.EVIO_FILE);
    }
    
    public void setRecordSource(LCIOEventSource recordSource) {
        this.recordSource = recordSource;
        try {
            this.lcsimLoop.setLCIORecordSource(recordSource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setSourceType(SourceType.LCIO_FILE);
    }
    
    public void add(Driver driver) {
        this.lcsimLoop.add(driver);
    }
    
    public void add(Collection<Driver> drivers) {
        for (Driver driver : drivers) { 
            this.lcsimLoop.add(driver);
        }
    }
    
    public void add(EtEventProcessor processor) {
        this.etLoop.addEtEventProcessor(processor);
    }
    
    public void add(EvioEventProcessor processor) {
        this.evioLoop.addEvioEventProcessor(processor);
    }
    
    public void setDetectorName(String detectorName) {
        this.detectorName = detectorName;
    }
    
    public void setPrintStream(PrintStream logStream) {
        this.logStream = logStream;
    }
    
    public void setStopOnEndRun() {
        this.stopOnEndRun = true;
    }
    
    public void setContinueOnErrors() {
        this.continueOnErrors = true;
    }
    
    public CompositeRecord getCompositeRecord() {
        try {
            return (CompositeRecord) compositeLoop.getRecordSource().getCurrentRecord();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
      
    /**
     * Process one event by executing the processing steps.
     * @throws IOException If some error occurs while processing events.
     */
    void processEvent() throws IOException, NoSuchRecordException {
        for (EventProcessingStep step : this.processingSteps) {
            step.execute();
        }
        ++this.totalEventsProcessed;
    }
        
    /**
     * This exception occurs when an EVIO end record is encountered
     * in the event stream and the processing chain is configured
     * to stop when this occurs.
     */
    class EndRunException extends IOException {
        EndRunException(String message) {
            super(message);
        }
    }
    
    public void resume() {
        this.paused = false;
    }
    
    public void loop() {
        while (!isDone) {
            if (!paused) {
                compositeLoop.execute(Command.GO, false);
            }
        }
    }
    
    public void pause() {
        compositeLoop.execute(Command.PAUSE);
        paused = true;
    }
        
    public void finish() {
        compositeLoop.execute(Command.STOP);
        isDone = true;
    }    
        
    public void next() {
        compositeLoop.execute(Command.GO_N, 1L, true);
    }
            
    /** 
     * Get the total number of events processed.
     * @return The number of events processed.
     */
    public int getTotalEventsProcessed() {
        return this.totalEventsProcessed;
    }
    
    public boolean isDone() {
        return isDone;
    }
        
    /**
     * ET processing step to load an <tt>EtEvent</tt> from the ET ring.
     */
    private class EtProcessingStep implements EventProcessingStep {
        
        /**
         * Load the next <tt>EtEvent</tt>.
         */
        public void execute() throws IOException, NoSuchRecordException {
            // Load the next EtEvent.
            etLoop.execute(NEXT);
            
            // Get an EtEvent from the loop.
            EtEvent nextEtEvent = (EtEvent) etLoop.getRecordSource().getCurrentRecord();
            
            // Failed to read an EtEvent from the ET server.
            if (nextEtEvent == null)
                throw new NoSuchRecordException("No current EtEvent is available.");
            
            getCompositeRecord().setEtEvent(nextEtEvent);
        }
    }
    
    /**
     * EVIO processing step to build an <tt>EvioEvent</tt> from the <tt>EtEvent</tt>
     * or load the next <tt>EvioEvent</tt> from a file if using an EVIO file source.
     */
    private class EvioProcessingStep implements EventProcessingStep {
        
        /**
         * Load the next <tt>EvioEvent</tt>, either from a record source
         * or from the <tt>EtEvent</tt> data.
         */
        public void execute() throws IOException, NoSuchRecordException {
            
            if (sourceType == SourceType.ET_EVENT) {
                EvioEvent evioEvent = null;
                try {
                    evioEvent = createEvioEvent(getCompositeRecord().getEtEvent());                    
                    if (evioEvent == null)
                        throw new IOException("Failed to create EvioEvent from current EtEvent.");
                    setEventNumber(evioEvent);
                } catch (EvioException e) {
                    throw new IOException(e);
                }
            
                // Add EvioEvent to the queue for loop.
                evioQueue.addRecord(evioEvent);
            }

            // Process one EvioEvent.
            evioLoop.execute(NEXT);         
            EvioEvent nextEvioEvent = (EvioEvent) evioLoop.getRecordSource().getCurrentRecord();
            
            // The call to loop did not create a current record.
            if (nextEvioEvent == null)
                throw new NoSuchRecordException("No current EVIO event.");
            
            getCompositeRecord().setEvioEvent(nextEvioEvent);
            
            // Encountered an end of run record.
            if (EventConstants.isEndEvent(nextEvioEvent))
                // If stop on end run is enabled, then trigger an exception to end processing.
                if (stopOnEndRun)
                    throw new EndRunException("EVIO end event received, and stop on end run is enabled.");
        }
        
        /**
         * Create an <tt>EvioEvent</tt> from the current <tt>EtEvent</tt>.
         * @param etEvent
         * @return
         * @throws IOException
         * @throws EvioException
         * @throws BufferUnderflowException
         */
        private EvioEvent createEvioEvent(EtEvent etEvent) 
                throws IOException, EvioException, BufferUnderflowException {
            return (new EvioReader(etEvent.getDataBuffer())).parseNextEvent();
        }
        
        /**
         * When reading from ET data, the EVIO event number needs to be set manually
         * from the event ID bank.
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
    
    /**
     * Processing step to build an LCIO event from an <tt>EvioEvent</tt>
     * or load the next event if using a file source.
     */
    private class LcioProcessingStep implements EventProcessingStep {
        
        /**
         * Create the next LCIO event either from the EVIO record
         * or from a direct LCIO file source.
         */
        public void execute() throws IOException, NoSuchRecordException {

            // When the loop does not have a direct LCIO file source, 
            // the events need to be built from the EVIO input.
            if (sourceType.ordinal() < SourceType.LCIO_FILE.ordinal()) {
            
                EvioEvent currentEvioEvent = getCompositeRecord().getEvioEvent();
                
                // Set state on LCIO event builder.
                eventBuilder.readEvioEvent(currentEvioEvent);
                
                // The LCIO event will be built if processing an EVIO physics event.
                if (eventBuilder.isPhysicsEvent(currentEvioEvent)) {

                    // Use the event builder to create the next LCIO event.
                    EventHeader lcioEvent = eventBuilder.makeLCSimEvent(currentEvioEvent);
        
                    // Add LCIO event to the queue.
                    lcioQueue.addRecord(lcioEvent);
                } else {
                    // The LCIO processing ignores non-physics events.
                    return;
                }
            }
        
            // If using an LCIO file source, check for EOF.
            if (sourceType == SourceType.LCIO_FILE) {
                if (!lcsimLoop.getRecordSource().hasNext())
                    throw new NoSuchRecordException("No next LCIO event.");
            }
            
            // Process the next LCIO event.
            lcsimLoop.execute(NEXT);
                                   
            // The last call to the loop did not create a current record for some reason.
            if (lcsimLoop.getRecordSource().getCurrentRecord() == null) {
                throw new NoSuchRecordException("No current LCIO event.");
            }
            
            EventHeader lcioEvent = (EventHeader) lcsimLoop.getRecordSource().getCurrentRecord();
            getCompositeRecord().setLcioEvent(lcioEvent);
        }
    }
}