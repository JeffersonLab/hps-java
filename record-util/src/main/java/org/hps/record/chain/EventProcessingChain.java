package org.hps.record.chain;

import java.io.File;
import java.io.IOException;

import org.freehep.record.loop.AbstractLoopListener;
import org.freehep.record.loop.LoopEvent;
import org.freehep.record.loop.RecordLoop.Command;
import org.hps.record.DataSourceType;
import org.hps.record.chain.EventProcessingConfiguration.ProcessingStage;
import org.hps.record.composite.CompositeRecordLoop;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.etevent.EtEventProcessor;
import org.hps.record.etevent.EtEventSource;
import org.hps.record.evio.EndRunProcessor;
import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.evio.EvioEventQueue;
import org.hps.record.evio.EvioFileSource;
import org.hps.record.lcio.LcioEventQueue;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCIOEventSource;

/**
 * This class provides a serial implementation of the event processing chain
 * for the monitoring app.  Implementations of FreeHep's <tt>RecordLoop</tt> 
 * are chained together via a <tt>CompositeRecordLoop</tt>.  The processing for 
 * each record type is done by activating registered processors on their
 * individual loop implementations.  Essentially, the class is a facade that
 * hides the complexity of configuring all the different record loops.
 * 
 * The processing chain can be configured to execute the ET, EVIO event building,
 * or LCIO event building stages.  The source can be set to an ET ring,
 * EVIO file source, or LCIO file source.  Any number of event processors
 * can be registered with the three different loops for processing the different 
 * record types, in order to plot, update a GUI component, or analyze the events.
 */
public class EventProcessingChain extends AbstractLoopListener {
                    
    protected int totalEventsProcessed;
    protected Throwable lastError;
    protected boolean done;
    protected boolean paused;
    
    protected EtProcessingStep etStep = new EtProcessingStep();
    protected EvioProcessingStep evioStep = new EvioProcessingStep();
    protected LcioProcessingStep lcioStep = new LcioProcessingStep();
    protected CompositeRecordLoop compositeLoop = new CompositeRecordLoop();
                  
    /**
     * A configuration object must be supplied to use this class.
     * @param configuration The configuration of the event processing.
     */
    public EventProcessingChain(EventProcessingConfiguration configuration) {                
        configure(configuration);
    }

    private void configure(EventProcessingConfiguration configuration) {
        
        // Add this class as a loop listener.
        compositeLoop.addLoopListener(this);
        
        // Was there no RecordSource provided explicitly?
        if (configuration.recordSource == null) {
            // Using an ET server connection?
            if (configuration.sourceType.equals(DataSourceType.ET_SERVER)) {
                if (configuration.connection != null)
                    etStep.getLoop().setRecordSource(new EtEventSource(configuration.connection));
                else
                    throw new IllegalArgumentException("Configuration is missing a valid ET connection.");
            // Using an EVIO file?
            } else if (configuration.sourceType.equals(DataSourceType.EVIO_FILE)) {
                if (configuration.filePath != null)
                    evioStep.getLoop().setRecordSource(new EvioFileSource(new File(configuration.filePath)));
                else
                    throw new IllegalArgumentException("Configuration is missing a file path.");
            // Using an LCIO file?
            } else if (configuration.sourceType.equals(DataSourceType.LCIO_FILE)) {
                if (configuration.filePath != null)
                    try {
                        lcioStep.getLoop().setLCIORecordSource(new LCIOEventSource(new File(configuration.filePath)));
                    } catch (IOException e) {
                        throw new RuntimeException("Error configuring LCIOEventSource.", e);
                    }
                else
                    throw new IllegalArgumentException("Configuration is missing a file path.");
            }
        } else {           
            // User provided an EtEventSource?
            if (configuration.recordSource instanceof EtEventSource) {
                etStep.getLoop().setRecordSource((EtEventSource) configuration.recordSource);
            // User provided an EvioFileSource?
            } else if (configuration.recordSource instanceof EvioFileSource) {
                evioStep.getLoop().setRecordSource((EvioFileSource) configuration.recordSource);
            // User provided an LCIOEventSource?
            } else if (configuration.recordSource instanceof LCIOEventSource) {
                try {
                    lcioStep.getLoop().setLCIORecordSource((LCIOEventSource)configuration.recordSource);
                } catch (IOException e) {
                    throw new RuntimeException("Error setting up LCIORecordSource.", e);
                }
            } else {
                throw new IllegalArgumentException("Unknown RecordSource type was supplied.");
            }
        }
                
        // Using the ET server for events?
        if (configuration.sourceType == DataSourceType.ET_SERVER) {
            // Add the ET event processing step.
            compositeLoop.addProcessor(etStep);
        }
   
        // Building EVIO events?
        if (configuration.processingStage.ordinal() >= ProcessingStage.EVIO.ordinal()) {
            // Using EVIO event source?
            if (configuration.sourceType.ordinal() <= DataSourceType.EVIO_FILE.ordinal()) {
                // Using ET event source?
                if (configuration.sourceType == DataSourceType.ET_SERVER) {
                    // Use dynamic event queue.
                    evioStep.setEvioEventQueue(new EvioEventQueue());
                }
                // Add EVIO processing step.
                compositeLoop.addProcessor(evioStep);
            }
        }
        
        // Building LCIO events?
        if (configuration.processingStage.ordinal() >= ProcessingStage.LCIO.ordinal()) {
            // Set detector on event builder.
            if (configuration.eventBuilder != null) 
                configuration.eventBuilder.setDetectorName(configuration.detectorName);
            else
                throw new IllegalArgumentException("The eventBuilder was not set in the configuration.");
            
            if (configuration.sourceType.ordinal() != DataSourceType.LCIO_FILE.ordinal()) {
                // Use dynamic event queue.
                lcioStep.setLcioEventQueue(new LcioEventQueue());
            }
            // Set event builder.
            lcioStep.setEventBuilder(configuration.eventBuilder);
            
            // Add LCIO processing step.
            compositeLoop.addProcessor(lcioStep);
        }
        
        // Set whether to stop on event processing errors.
        compositeLoop.setStopOnErrors(configuration.stopOnErrors);
        
        // Add EtEventProcessors to loop.
        for (EtEventProcessor processor : configuration.etProcessors) {
            etStep.getLoop().addEtEventProcessor(processor);
        }
        
        // Add EvioEventProcessors to loop.
        for (EvioEventProcessor processor : configuration.evioProcessors) {
            evioStep.getLoop().addEvioEventProcessor(processor);
        }
        
        // Add Drivers to loop.
        for (Driver driver : configuration.drivers) {
            lcioStep.getLoop().add(driver);
        }
        
        // Add CompositeRecordProcessors to loop.
        for (CompositeRecordProcessor processor : configuration.compositeProcessors) {
            compositeLoop.addProcessor(processor);
        }

        // Stop on end run?
        if (configuration.stopOnEndRun) {
            // Add the CompositeRecordProcessor that will throw the EndRunException.
            compositeLoop.addProcessor(new EndRunProcessor());
        }
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
            lastError = (Exception) loopEvent.getException();
        }
    }
    
    /**
     * Loop over events until processing ends for some reason.
     */
    public void run() {
        // Keep looping until the event processing is flagged as done.
        while (true) {
            // Is the processing unpaused?
            if (!paused) {
                try {
                    // Put the RecordLoop into looping mode and process records until exception occurs.
                    System.out.println("loop state: " + compositeLoop.getState().toString());
                    compositeLoop.execute(Command.GO, true);
                } catch (Exception exception) { 
                    // Handle an "error" which might really just be control flow ("end of run" etc.).
                    setLastError(exception);
                    done = true;
                }               
                if (done)
                    break;
            }
        }
    }
    
    public void stop() {
        compositeLoop.execute(Command.STOP);
        done = true;
    }
     
    /**
     * Set the last error that occurred during processing.
     * @param error The last error that occurred.
     */
    void setLastError(Throwable error) {
        this.lastError = error;
    }
    
    /**
     * Get the last error that occurred.
     * @return The last error that occurred.
     */
    public Throwable getLastError() {
        return lastError;
    }

    /**
     * Pause the event processing.
     */
    public void pause() {
        compositeLoop.execute(Command.PAUSE);
        paused = true;
    }
                  
    /**
     * Get the next event e.g. while in pause mode.
     */
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
}