package org.hps.monitoring.record;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.freehep.record.source.RecordSource;
import org.hps.evio.EventConstants;
import org.hps.evio.LCSimEventBuilder;
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
 * This class provides a serial implementation of the 
 * ET to EVIO to LCIO event processing chain.
 * 
 * This is a flexible class for handling the event processing with
 * a number of different configurations and scenarios.  The processing
 * chain can be configured to execute the ET, EVIO event building, or
 * LCIO eventing building stages.  The source can be set to an ET ring,
 * EVIO file source, or LCIO file source.  Any number of event processors
 * can be registered for processing the different record types, in order
 * to plot or otherwise visualize or analyze the events in that format.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class EventProcessingChain {
      
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
    EvioEventQueue evioQueue = new EvioEventQueue();
    LcioEventQueue lcioQueue = new LcioEventQueue();
    LCSimEventBuilder eventBuilder;
    EtEvent currentEtEvent;
    EvioEvent currentEvioEvent;
    int totalEventsProcessed;
    String detectorName;
    boolean stopRequested;
    boolean paused;
    boolean stopOnEndRun;
    
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
    }
    
    void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
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
      
    /**
     * Process one event by executing the processing steps.
     * @throws IOException If some error occurs while processing events.
     */
    void processEvent() throws IOException {
        for (EventProcessingStep step : this.processingSteps) {
            step.execute();
        }
        ++this.totalEventsProcessed;
    }
    
    /**
     * Stop event processing.
     */
    public void stop() {
        this.stopRequested = true;
    }
    
    /**
     * Pause event processing.
     */
    public void pause() {
        this.paused = true;
    }
    
    /**
     * Resume event processing after pausing.
     */
    public void resume() {
        this.paused = false;
    }
    
    /**
     * This exception occurs when the call to the event loop
     * results in a null current record.
     */
    class EventsExhaustedException extends IOException {
        EventsExhaustedException(String message) {
            super(message);
        }
    }
    
    /**
     * This exception occurs when an EVIO end record is encountered
     * in the event stream.
     */
    class EndRunException extends IOException {
        EndRunException(String message) {
            super(message);
        }
    }
    
    /**
     * Primary method for event processing.  This will run events
     * until a stop or pause is requested, the event source is exhausted,
     * or (if this behavior is enabled) the end of run record is reached.
     */
    public synchronized void run() {
        this.stopRequested = false;
        for (;;) {
            try {
                processEvent();
            } catch (EventsExhaustedException e) {
                this.logStream.println(e.getMessage());
                break;
            } catch (EndRunException e) {
                this.logStream.println(e.getMessage());
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            if (stopRequested) {
                this.logStream.println("Stop was requested.  Processing will end!");
                break;
            }
            
            if (this.paused) {
                synchronized (Thread.currentThread()) {
                    for (;;) {
                        try {
                            // Sleep for 1 second.
                            Thread.currentThread().wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // Check if unpaused.
                        if (!this.paused)
                            break;
                    }
                }
            }
        }
    }
    
    /** 
     * Get the total number of events processed.
     * @return The number of events processed.
     */
    public int getTotalEventsProcessed() {
        return this.totalEventsProcessed;
    }
    
    /**
     * Interface for a single processing step which handles one type of record.
     */
    interface EventProcessingStep {
       void execute() throws IOException;
    }

    /**
     * ET processing step to load an <tt>EtEvent</tt> from the ET ring.
     */
    private class EtProcessingStep implements EventProcessingStep {
        
        /**
         * Load the next <tt>EtEvent</tt>.
         */
        public void execute() throws IOException {
            // Load the next EtEvent.
            etLoop.loop(1);
            
            // Get an EtEvent from the loop.
            currentEtEvent = (EtEvent) etLoop.getRecordSource().getCurrentRecord();
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
        public void execute() throws IOException {
            
            if (sourceType == SourceType.ET_EVENT) {
                EvioEvent evioEvent = null;
                try {
                    evioEvent = createEvioEvent(currentEtEvent);
                    setEventNumber(evioEvent);
                } catch (EvioException e) {
                    throw new IOException(e);
                }
            
                // Add EvioEvent to the queue for loop.
                evioQueue.addRecord(evioEvent);
            }

            // Process one EvioEvent.
            evioLoop.loop(1);
            currentEvioEvent = (EvioEvent) evioLoop.getRecordSource().getCurrentRecord();
            
            // The call to loop did not create a current record.
            if (currentEvioEvent == null)
                throw new EventsExhaustedException("No current EVIO event.");
            
            // Encountered an end of run record.
            if (EventConstants.isEndEvent(currentEvioEvent))
                // If stop on end run is enabled then trigger an exception to end processing.
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
         * @param event The <tt>EvioEvent</tt> on which to set the event number.
         */
        private void setEventNumber(EvioEvent event) {
            int eventNumber = -1;
            for (BaseStructure bank : event.getChildren()) {
                if (bank.getHeader().getTag() == EventConstants.EVENTID_BANK_TAG) {
                    eventNumber = bank.getIntData()[0];
                    break;
                }
            }
            if (eventNumber != -1)
                event.setEventNumber(eventNumber);
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
        public void execute() throws IOException {

            // When the loop does not have a direct event source, the events
            // need to be built from EVIO.
            if (sourceType.ordinal() < SourceType.LCIO_FILE.ordinal()) {
            
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
        
            // Process the next LCIO event.
            lcsimLoop.loop(1, null);
            
            // In this case, there are no more records in the file.
            if (sourceType == SourceType.LCIO_FILE) {
                if (!lcsimLoop.getRecordSource().hasNext())
                    throw new EventsExhaustedException("No next LCIO event.");
            }
            
            // The last call to loop did not create a current record.
            if (lcsimLoop.getRecordSource().getCurrentRecord() == null) {
                throw new EventsExhaustedException("No current LCIO event.");
            }
        }
    }
}