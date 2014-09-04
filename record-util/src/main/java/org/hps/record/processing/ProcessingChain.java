package org.hps.record.processing;

import java.io.File;
import java.io.IOException;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.record.composite.CompositeEtAdapter;
import org.hps.record.composite.CompositeEvioAdapter;
import org.hps.record.composite.CompositeLcioAdapter;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopAdapter;
import org.hps.record.composite.CompositeProcessor;
import org.hps.record.et.EtProcessor;
import org.hps.record.et.EtSource;
import org.hps.record.evio.EvioFileSource;
import org.hps.record.evio.EvioProcessor;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCIOEventSource;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;

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
// TODO: Replace the short loop control methods with direct calls to loop in code using this class.
public class ProcessingChain {
                    
    boolean paused;
    int maxRecords = -1;
    
    CompositeLoop compositeLoop = new CompositeLoop();
    CompositeEtAdapter etAdapter;
    CompositeEvioAdapter evioAdapter;
    CompositeLcioAdapter lcioAdapter;
    CompositeLoopAdapter compositeAdapter;
                                
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
                    etAdapter = new CompositeEtAdapter(new EtSource(configuration.connection));
                else
                    throw new IllegalArgumentException("Configuration is missing a valid ET connection.");
            // Using an EVIO file?
            } else if (configuration.sourceType.equals(DataSourceType.EVIO_FILE)) {
                if (configuration.filePath != null) {
                    evioAdapter = new CompositeEvioAdapter(new EvioFileSource(new File(configuration.filePath)));
                } else {
                    throw new IllegalArgumentException("Configuration is missing a file path.");
                }
            // Using an LCIO file?
            } else if (configuration.sourceType.equals(DataSourceType.LCIO_FILE)) {
                if (configuration.filePath != null)
                    try {
                        lcioAdapter = new CompositeLcioAdapter(new LCIOEventSource(new File(configuration.filePath)));
                    } catch (IOException e) {
                        throw new RuntimeException("Error configuring LCIOEventSource.", e);
                    }
                else
                    throw new IllegalArgumentException("Configuration is missing a file path.");
            }
        }
        
        // Configure ET system.
        if (configuration.sourceType == DataSourceType.ET_SERVER) {
            //System.out.println("compositeLoop.addAdapter(etAdapter)");
            compositeLoop.addAdapter(etAdapter);
        }
        
        // Configure EVIO processing.
        if (configuration.processingStage.ordinal() >= ProcessingStage.EVIO.ordinal()) {
            if (configuration.sourceType.ordinal() <= DataSourceType.EVIO_FILE.ordinal()) {
                if (evioAdapter == null)
                    evioAdapter = new CompositeEvioAdapter();
                //System.out.println("compositeLoop.addAdapter(evioAdapter)");
                compositeLoop.addAdapter(evioAdapter);
            }
        }
        
        // Configure LCIO processing.
        if (configuration.processingStage.ordinal() >= ProcessingStage.LCIO.ordinal()) {
            if (lcioAdapter == null)
                lcioAdapter = new CompositeLcioAdapter();
            //System.out.println("compositeLoop.addAdapter(lcioAdapter)");
            compositeLoop.addAdapter(lcioAdapter);
            if (configuration.eventBuilder != null) {
                if (configuration.detectorName != null) {
                    // Is LCSim ConditionsManager installed yet?
                    if (!ConditionsManager.isSetup())
                        // Setup LCSim conditions system if not already.
                        LCSimConditionsManagerImplementation.register();
                    configuration.eventBuilder.setDetectorName(configuration.detectorName);
                } else {
                    throw new IllegalArgumentException("Missing detectorName in configuration.");
                }
                lcioAdapter.setLCSimEventBuilder(configuration.eventBuilder);
            } else {
                throw new IllegalArgumentException("Missing an LCSimEventBuilder in configuration.");
            }
        }
                                                                                    
        // Set whether to stop on event processing errors.
        compositeLoop.setStopOnErrors(configuration.stopOnErrors);
        
        // Add EtEventProcessors to loop.
        for (EtProcessor processor : configuration.etProcessors) {
            etAdapter.addProcessor(processor);
        }
                
        // Add EvioEventProcessors to loop.
        for (EvioProcessor processor : configuration.evioProcessors) {
            evioAdapter.addProcessor(processor);
        }
        
        // Add Drivers to loop.
        for (Driver driver : configuration.drivers) {
            lcioAdapter.addDriver(driver);
        }
        
        // Add CompositeLoopAdapter which should execute last.
        CompositeLoopAdapter compositeAdapter = new CompositeLoopAdapter();
        //System.out.println("compositeLoop.addAdapter(compositeAdapter)");
        compositeLoop.addAdapter(compositeAdapter);
        
        // Add CompositeRecordProcessors to loop.
        for (CompositeProcessor processor : configuration.compositeProcessors) {
            compositeAdapter.addProcessor(processor);
        }
        
        // Max records was set?
        if (configuration.maxRecords != -1) {            
            compositeAdapter.addProcessor(new MaxRecordsProcessor(configuration.maxRecords));
        }         
    }
                                            
    /**
     * Loop over events until processing ends for some reason.
     */
    public void run() {
        
        if (ConditionsManager.defaultInstance() == null)
            LCSimConditionsManagerImplementation.register();
        
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
            //System.out.println("bottom of run loop");
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
    
    public CompositeLoop getLoop() {
        return compositeLoop;
    }
}