package org.hps.monitoring.record;

import java.io.IOException;
import java.util.Collection;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordLoop.Command;
import org.freehep.record.loop.RecordLoop.State;
import org.freehep.record.source.RecordSource;
import org.hps.evio.LCSimEventBuilder;
import org.hps.monitoring.record.composite.CompositeRecordLoop;
import org.hps.monitoring.record.etevent.EtEventProcessor;
import org.hps.monitoring.record.etevent.EtEventSource;
import org.hps.monitoring.record.evio.EvioEventProcessor;
import org.hps.monitoring.record.evio.EvioEventQueue;
import org.hps.monitoring.record.evio.EvioFileSource;
import org.hps.monitoring.record.lcio.LcioEventQueue;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCIOEventSource;

/**
 * This class provides a serial implementation of the monitoring event
 * processing chain.  This is accomplished by chaining together implementations 
 * of FreeHep's <tt>RecordLoop</tt> via a <tt>CompositeRecordLoop</tt>.  The
 * processing for each record type is done from a record listener on the 
 * composite loop.
 * 
 * The processing chain can be configured to execute the ET, EVIO event building,
 * or LCIO eventing building stages.  The source can be set to an ET ring,
 * EVIO file source, or LCIO file source.  Any number of event processors
 * can be registered with the different loops for processing the different 
 * record types, in order to plot, update a GUI component, or analyze the events.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// FIXME: Make sure that end run EVIO events cause the processing to end.
public class EventProcessingChain extends AbstractLoopListener {
      
    /**
     * Type of source for events.
     */
    public enum SourceType {
        ET_EVENT,
        EVIO_FILE,
        LCIO_FILE
    }
    
    /**
     * Processing stages to execute.
     */
    public enum ProcessingStage {
        READ_ET_EVENT,
        BUILD_EVIO_EVENT,
        BUILD_LCIO_EVENT
    }
    
    private SourceType sourceType = SourceType.ET_EVENT;
    private ProcessingStage processingStage = ProcessingStage.BUILD_LCIO_EVENT;    
    private RecordSource recordSource;
    private LCSimEventBuilder eventBuilder;
    private int totalEventsProcessed;
    private String detectorName;
    private Exception lastException;
    private boolean done;
    private boolean paused;
    private boolean wasSetup;
    
    private EtProcessingStep etStep = new EtProcessingStep();
    private EvioProcessingStep evioStep = new EvioProcessingStep();
    private LcioProcessingStep lcioStep = new LcioProcessingStep();
    private CompositeRecordLoop compositeLoop = new CompositeRecordLoop();
        
    /**
     * No argument constructor.  
     * The setter methods should be used to configure this class.
     */
    public EventProcessingChain() {  
    }
        
    /**
     * Setup the event processing chain based on the current configuration.
     */
    public void setup() {
        
        if (wasSetup) {
            throw new RuntimeException("The EventProcessingChain was already setup.");
        }
        
        // Add this class as a loop listener.
        compositeLoop.addLoopListener(this);
        
        // Record source must be set by here.
        if (recordSource == null)
            throw new RuntimeException("No record source was set.");
        
        // Using the ET server for events.
        if (sourceType == SourceType.ET_EVENT) {
            // Add the ET event processing step.
            compositeLoop.addProcessor(etStep);
        }
   
        // Building EVIO events.
        if (processingStage.ordinal() >= ProcessingStage.BUILD_EVIO_EVENT.ordinal()) {
            // Using EVIO event source.
            if (sourceType.ordinal() <= SourceType.EVIO_FILE.ordinal()) {
                // Using ET event source.
                if (sourceType == SourceType.ET_EVENT) {
                    // Use dynamic event queue.
                    evioStep.setEvioEventQueue(new EvioEventQueue());
                }
                // Add EVIO processing step.
                compositeLoop.addProcessor(evioStep);
            }
        }
        
        // Building LCIO events.
        if (processingStage.ordinal() >= ProcessingStage.BUILD_LCIO_EVENT.ordinal()) {
            // Set detector on event builder.
            if (eventBuilder != null) 
                eventBuilder.setDetectorName(detectorName);
            if (sourceType.ordinal() != SourceType.LCIO_FILE.ordinal()) {
                // Use dynamic event queue.
                lcioStep.setLcioEventQueue(new LcioEventQueue());
            }
            // Set event builder.
            lcioStep.setEventBuilder(eventBuilder);
            // Add LCIO processing step.
            compositeLoop.addProcessor(lcioStep);
        }
        
        wasSetup = true;
    }
    
    /**
     * Set the type of source being used.
     * @param sourceType The type of source.
     */
    void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }
        
    /**
     * Set the processing stages to execute.
     * @param processingStage The processing stages to execute.
     */
    public void setProcessingStage(ProcessingStage processingStage) {
        this.processingStage = processingStage;
    }
            
    /**
     * Set the event builder to be used for the EVIO to LCIO conversion.
     * @param eventBuilder The event builder to use for EVIO to LCIO conversion.
     */
    public void setEventBuilder(LCSimEventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
    }
    
    /**
     * Set the record source.
     * @param recordSource The record source.
     */
    public void setRecordSource(EtEventSource recordSource) {
        this.recordSource = recordSource;
        this.etStep.getLoop().setRecordSource(recordSource);
        this.sourceType = SourceType.ET_EVENT;
    }
    
    /**
     * Set an EVIO event source.
     * @param recordSource The EVIO event source.
     */
    public void setRecordSource(EvioFileSource recordSource) {
        this.recordSource = recordSource;
        evioStep.getLoop().setRecordSource(recordSource);
        setSourceType(SourceType.EVIO_FILE);
    }
    
    /**
     * Set an LCIO event source.
     * @param recordSource The LCIO event source.
     */
    public void setRecordSource(LCIOEventSource recordSource) {
        this.recordSource = recordSource;
        try {
            lcioStep.getLoop().setLCIORecordSource(recordSource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setSourceType(SourceType.LCIO_FILE);
    }
    
    /**
     * Add a driver to the LCIO loop.
     * @param driver The Driver to add.
     */
    public void add(Driver driver) {
        lcioStep.getLoop().add(driver);
    }
    
    /**
     * Add a list of <tt>Drivers</tt> to the LCIO loop.
     * @param drivers The list of <tt>Drivers</tt> to add.
     */
    public void add(Collection<Driver> drivers) {
        for (Driver driver : drivers) { 
            lcioStep.getLoop().add(driver);
        }
    }
    
    /**
     * Add a processor of ET events.
     * @param processor The processor of ET events.
     */
    public void add(EtEventProcessor processor) {
        etStep.getLoop().addEtEventProcessor(processor);
    }
    
    /**
     * Add a processor of EVIO events.
     * @param processor The processor of EVIO events.
     */
    public void add(EvioEventProcessor processor) {
        evioStep.getLoop().addEvioEventProcessor(processor);
    }
    
    /**
     * Set the name of the detector model to use.
     * @param detectorName The name of the detector model.
     */
    public void setDetectorName(String detectorName) {
        this.detectorName = detectorName;
    }
                   
    /**
     * Resume event processing from pause mode.
     */
    public void resume() {
        this.paused = false;
    }
    
    /**
     * Suspend event processing e.g. when pausing.
     * @param loopEvent The loop event.
     */
    public void suspend(LoopEvent loopEvent) {
        if (loopEvent.getException() != null) {
            loopEvent.getException().printStackTrace();
            lastException = (Exception) loopEvent.getException();
        }
    }
    
    /**
     * Loop over events until processing ends for some reason.
     */
    public void loop() {
        // Keep looping until the event processing is flagged as done.
        while (!done) {
            // Go into loop mode if the system is not paused.
            if (!paused) {
                try {
                    if (compositeLoop.getState() != State.IDLE)
                        throw new IllegalLoopStateException(compositeLoop.getState());
                    // Execute GO on the composite loop.
                    compositeLoop.execute(Command.GO, true);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Set the exception.
                    this.lastException = e;
                }
                
                // When an exception occurs, which can sometimes just be control flow,
                // the event processing should stop.
                if (lastException != null) {
                    lastException.printStackTrace();
                    if (!done)
                        // Call finish manually here as the loop was suspended.
                        finish(); 
                } 
            }
        }
    }

    /**
     * Pause the event processing.
     */
    public void pause() {
        if (compositeLoop.getState() != State.LOOPING)
            throw new IllegalLoopStateException(compositeLoop.getState());
        compositeLoop.execute(Command.PAUSE);
        paused = true;
    }
        
    /**
     * Finish the event processing.
     */
    public void finish() {
        // TODO: Add check here for correct loop state.
        try {
            compositeLoop.execute(Command.STOP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        done = true;
    }    
        
    /**
     * Get the next event e.g. while in pause mode.
     */
    public void next() {
        if (compositeLoop.getState() != State.READY)
            throw new IllegalLoopStateException(compositeLoop.getState());
        compositeLoop.execute(Command.GO_N, 1L, true);
    }
            
    /** 
     * Get the total number of events processed.
     * @return The number of events processed.
     */
    public int getTotalEventsProcessed() {
        return this.totalEventsProcessed;
    }
    
    /**
     * Exception thrown if the loop is not in the correct state
     * when a certain method is called.
     */
    class IllegalLoopStateException extends RuntimeException {
        IllegalLoopStateException(State state) {
            super("Illegal loop state: " + state);
        }
    }
}