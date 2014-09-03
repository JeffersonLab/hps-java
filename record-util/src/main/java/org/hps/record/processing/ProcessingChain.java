package org.hps.record.processing;

import java.io.File;
import java.io.IOException;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeProcessor;
import org.hps.record.et.EtProcessor;
import org.hps.record.et.EtSource;
import org.hps.record.evio.EvioFileSource;
import org.hps.record.evio.EvioProcessor;
import org.hps.record.evio.EvioRecordQueue;
import org.hps.record.lcio.LcioRecordQueue;
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
public class ProcessingChain {
                    
    protected boolean paused;
    protected int maxRecords = -1;
    
    protected EtProcessingStep etStep = new EtProcessingStep();
    protected EvioProcessingStep evioStep = new EvioProcessingStep();
    protected LcioProcessingStep lcioStep = new LcioProcessingStep();
    protected CompositeLoop compositeLoop = new CompositeLoop();
                  
    /**
     * A configuration object must be supplied to use this class.
     * @param configuration The configuration of the event processing.
     */
    public ProcessingChain(ProcessingConfiguration configuration) {                
        configure(configuration);
    }

    private void configure(ProcessingConfiguration configuration) {
                
        // Was there no RecordSource provided explicitly?
        if (configuration.recordSource == null) {
            // Using an ET server connection?
            if (configuration.sourceType.equals(DataSourceType.ET_SERVER)) {
                if (configuration.connection != null)
                    etStep.getLoop().setRecordSource(new EtSource(configuration.connection));
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
            if (configuration.recordSource instanceof EtSource) {
                etStep.getLoop().setRecordSource((EtSource) configuration.recordSource);
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
                    evioStep.setEvioEventQueue(new EvioRecordQueue());
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
                lcioStep.setLcioEventQueue(new LcioRecordQueue());
            }
            // Set event builder.
            lcioStep.setEventBuilder(configuration.eventBuilder);
            
            // Add LCIO processing step.
            compositeLoop.addProcessor(lcioStep);
        }
        
        // Set whether to stop on event processing errors.
        compositeLoop.setStopOnErrors(configuration.stopOnErrors);
        
        // Add EtEventProcessors to loop.
        for (EtProcessor processor : configuration.etProcessors) {
            etStep.getLoop().addEtEventProcessor(processor);
        }
        
        // Add EvioEventProcessors to loop.
        for (EvioProcessor processor : configuration.evioProcessors) {
            evioStep.getLoop().addEvioEventProcessor(processor);
        }
        
        // Add Drivers to loop.
        for (Driver driver : configuration.drivers) {
            lcioStep.getLoop().add(driver);
        }
        
        // Add CompositeRecordProcessors to loop.
        for (CompositeProcessor processor : configuration.compositeProcessors) {
            compositeLoop.addProcessor(processor);
        }

        // Stop on end run?
        if (configuration.stopOnEndRun) {
            // Add the CompositeRecordProcessor that will throw the EndRunException.
            compositeLoop.addProcessor(new EvioEndEventProcessor());
        }
        
        // Max records was set?
        if (configuration.maxRecords != -1) {            
            compositeLoop.addProcessor(new MaxRecordsProcessor(configuration.maxRecords));
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
                // Loop until done, error occurs, or pause is requested.
                compositeLoop.execute(Command.GO, true);
                
                // Is loop done?
                if (compositeLoop.isDone()) {
                    // Stop record processing.
                    break;
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Stop the event processing by halting the loop.
     */
    public void stop() {
        compositeLoop.execute(Command.STOP);
    }
         
    /**
     * Get the last error that occurred.
     * @return The last error that occurred.
     */
    public Throwable getLastError() {
        return compositeLoop.getLastError();
    }

    /**
     * Pause the event processing.
     */
    public void pause() {   
        compositeLoop.execute(Command.PAUSE);
        paused = true;
    }
    
    /**
     * Resume event processing from pause mode.
     */
    public void resume() {
        this.paused = false;
    }
                  
    /**
     * Get the next event e.g. while in pause mode.
     */
    public void next() {
        compositeLoop.execute(Command.GO_N, 1L, true);
    }                      
}