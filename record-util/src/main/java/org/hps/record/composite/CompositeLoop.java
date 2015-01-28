package org.hps.record.composite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.NoSuchRecordException;
import org.freehep.record.source.RecordSource;
import org.hps.record.EndRunException;
import org.hps.record.MaxRecordsException;
import org.hps.record.RecordProcessingException;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.hps.record.et.EtEventProcessor;
import org.hps.record.et.EtEventSource;
import org.hps.record.et.EtEventSource.EtSourceException;
import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.evio.EvioFileSource;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCIOEventSource;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;

/**
 * Implementation of a composite record loop for processing
 * ET, EVIO and LCIO events using a single record source.
 */
public final class CompositeLoop extends DefaultRecordLoop {

    CompositeRecordSource recordSource = new CompositeRecordSource();
    List<CompositeLoopAdapter> adapters = new ArrayList<CompositeLoopAdapter>();
    
    boolean paused = false;
    boolean stopOnErrors = true;
    
    CompositeLoopConfiguration config = null;
                            
    /**
     * No argument constructor.  
     * The {@link #setCompositeLoopConfiguration(CompositeLoopConfiguration)} method must be
     * called on the loop manually.
     */
    public CompositeLoop() {
        setRecordSource(recordSource);
    }
    
    /**
     * Create the loop with the given configuration.
     * @param config The configuration parameters of the loop.
     */
    public CompositeLoop(CompositeLoopConfiguration config) {
        setRecordSource(recordSource);
        setCompositeLoopConfiguration(config);
    }
    
    /**
     * Set to true in order to have this loop stop on all
     * event processing errors.  Certain types of fatal errors
     * will never be ignored.
     * @param stopOnErrors True for this loop to stop on errors.
     */
    public void setStopOnErrors(boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
    }
    
    /**
     * Add a {@link CompositeLoopAdapter} which will process 
     * {@link CompositeRecord} objects.
     * @param adapter The CompositeLoopAdapter object.
     */
    public void addAdapter(CompositeLoopAdapter adapter) {
        addLoopListener(adapter);
        addRecordListener(adapter);        
    }
        
    /**
     * Set the <code>RecordSource</code> which provides <code>CompositeRecord</code> objects.
     */
    public final void setRecordSource(RecordSource source) {
        if (!source.getRecordClass().isAssignableFrom(CompositeRecord.class)) {
            throw new IllegalArgumentException("The RecordSource has the wrong class.");
        }        
        super.setRecordSource(source);
    }
                
    /**
     * Handle errors from the client such as registered adapters.
     * If the loop is setup to try and continue on errors, 
     * only non-fatal record processing exceptions are ignored.
     */
    protected void handleClientError(Throwable x) {      
        
        x.printStackTrace();
        
        // Is the error ignorable?
        if (isIgnorable(x)) {
            // Ignore the error!
            return;
        }
                
        // Set the exception on the super class.
        this._exception = x;
        
        // Stop the event processing.
        this.execute(Command.STOP);
    }

    /**
     * Handle errors thrown by the <code>RecordSource</code>.
     */
    protected void handleSourceError(Throwable x) {

        x.printStackTrace();
        
        // Is the error ignorable?
        if (isIgnorable(x)) {
            // Ignore the error!
            return;
        }
        
        // Set the exception on the super class.
        this._exception = x;
        
        // Stop the event processing.
        this.execute(Command.STOP);
    }        
    
    /**
     * <p>
     * True if an error is ignore-able.  If <code>stopOnErrors</code>
     * is true, then this method always returns false.  Otherwise,
     * the error cause determines whether the loop can continue 
     * processing.
     * <p>
     * The assumption here is that errors coming from event processing
     * of the composite records are caught in the adapters and then wrapped
     * in a {@link org.hps.record.RecordProcessingException}.  Certain
     * errors which should never be ignored are also wrapped in a 
     * similar way, so we need to check for these error types before 
     * assuming that event processing can continue.
     * 
     * @param x The error that occurred.
     * @return True if the error can be ignored.
     */
    private boolean isIgnorable(Throwable x) {        
        if (!stopOnErrors) {        
            if (x instanceof RecordProcessingException) {
                Throwable cause = x.getCause();
                if (cause instanceof MaxRecordsException || 
                        cause instanceof EndRunException || 
                        cause instanceof EtSourceException || 
                        cause instanceof NoSuchRecordException) {
                    // These types of exceptions are never ignored.
                    return false;
                } else {
                    // Other types of record processing exceptions are considered non-fatal.
                    return true;
                }
            } 
        }
        return false;               
    }
            
    /**
     * Get the last error that occurred.
     * @return The last error that occurred.
     */
    public Throwable getLastError() {
        return _exception;     
    }
    
    /**
     * Pause the event processing.
     */
    public void pause() {   
        execute(Command.PAUSE);
        paused = true;
    }
    
    /**
     * Resume event processing from pause mode.
     */
    public void resume() {
        paused = false;
    }
    
    /**
     * True if loop is paused.
     * @return True if loop is current paused.
     */
    public boolean isPaused() {
        return paused;
    }
        
    /**
     * Loop over events from the source.
     * @param number The number of events to process or -1 for unlimited.
     * @return The number of records that were processed.
     */
    public long loop(long number) {
        if (number < 0L) {
            execute(Command.GO, true);
        } else {
            execute(Command.GO_N, number, true);
            execute(Command.STOP); 
        }
        return getSupplied();
    }
    
    public void setConfiguration(Object object) {
        if (object instanceof CompositeLoopConfiguration) {
            setCompositeLoopConfiguration((CompositeLoopConfiguration)object);
        } else {
            throw new IllegalArgumentException("Wrong type of object to configure CompositeLoop: " + object.getClass().getCanonicalName());
        }
    }
    
    /**
     * Configure the loop using a {@link CompositeLoopConfiguration} object.
     * @param config The CompositeLoopConfiguration object containing the loop configuration parameter values.
     */
    void setCompositeLoopConfiguration(CompositeLoopConfiguration config) {
        
        if (this.config != null)
            throw new RuntimeException("CompositeLoop has already been configured.");
        
        this.config = config;
        
        EtEventAdapter etAdapter = null;
        EvioEventAdapter evioAdapter = null;
        LcioEventAdapter lcioAdapter = null;
        CompositeLoopAdapter compositeAdapter = new CompositeLoopAdapter();
        
        // Was there no RecordSource provided explicitly?
        if (config.recordSource == null) {
            // Using an ET server connection?
            if (config.sourceType.equals(DataSourceType.ET_SERVER)) {
                if (config.connection != null)
                    etAdapter = new EtEventAdapter(new EtEventSource(config.connection));
                else
                    throw new IllegalArgumentException("Configuration is missing a valid ET connection.");
            // Using an EVIO file?
            } else if (config.sourceType.equals(DataSourceType.EVIO_FILE)) {
                if (config.filePath != null) {
                    evioAdapter = new EvioEventAdapter(new EvioFileSource(new File(config.filePath)));
                } else {
                    throw new IllegalArgumentException("Configuration is missing a file path.");
                }
            // Using an LCIO file?
            } else if (config.sourceType.equals(DataSourceType.LCIO_FILE)) {
                if (config.filePath != null)
                    try {
                        lcioAdapter = new LcioEventAdapter(new LCIOEventSource(new File(config.filePath)));
                    } catch (IOException e) {
                        throw new RuntimeException("Error configuring LCIOEventSource.", e);
                    }
                else
                    throw new IllegalArgumentException("Configuration is missing a file path.");
            }
        }
        
        // Configure ET system.
        if (config.sourceType == DataSourceType.ET_SERVER) {
            //System.out.println("compositeLoop.addAdapter(etAdapter)");
            addAdapter(etAdapter);
        }
        
        // Configure EVIO processing.
        if (config.processingStage.ordinal() >= ProcessingStage.EVIO.ordinal()) {
            if (config.sourceType.ordinal() <= DataSourceType.EVIO_FILE.ordinal()) {
                if (evioAdapter == null)
                    evioAdapter = new EvioEventAdapter();
                //System.out.println("compositeLoop.addAdapter(evioAdapter)");
                addAdapter(evioAdapter);
            }
        }
        
        // Configure LCIO processing.
        if (config.processingStage.ordinal() >= ProcessingStage.LCIO.ordinal()) {
            if (lcioAdapter == null)
                lcioAdapter = new LcioEventAdapter();
            //System.out.println("compositeLoop.addAdapter(lcioAdapter)");
            addAdapter(lcioAdapter);
            if (config.eventBuilder != null) {
                if (config.detectorName != null) {
                    // Is LCSim ConditionsManager installed yet?
                    if (!ConditionsManager.isSetup()) {
                        // Setup default LCSim conditions system if not already.
                        LCSimConditionsManagerImplementation.register();
                    }
                    //config.eventBuilder.setDetectorName(config.detectorName);
                } else {
                    throw new IllegalArgumentException("Missing detectorName in configuration.");
                }
                lcioAdapter.setLCSimEventBuilder(config.eventBuilder);
            } else {
                throw new IllegalArgumentException("Missing an LCSimEventBuilder in configuration.");
            }
        }
                                                                                    
        // Set whether to stop on event processing errors.
        setStopOnErrors(config.stopOnErrors);
        
        // Set whether to stop on end run EVIO records.
        if (evioAdapter != null)
        	evioAdapter.setStopOnEndRun(config.stopOnEndRun);
        
        // Add EtEventProcessors to loop.
        if (etAdapter != null) {
            for (EtEventProcessor processor : config.etProcessors) {
                etAdapter.addProcessor(processor);
            }
        }
                
        // Add EvioEventProcessors to loop.
        if (evioAdapter != null) {
            for (EvioEventProcessor processor : config.evioProcessors) {
                evioAdapter.addProcessor(processor);
            }
        }
        
        // Add Drivers to loop.
        if (lcioAdapter != null) {
            for (Driver driver : config.drivers) {
                lcioAdapter.addDriver(driver);
            }
        }
        
        // Add CompositeLoopAdapter which should execute last.
        addAdapter(compositeAdapter);
        
        // Add CompositeRecordProcessors to loop.
        for (CompositeRecordProcessor processor : config.compositeProcessors) {
            compositeAdapter.addProcessor(processor);
        }
        
        if (config.supplyLcioEvents) {
            addAdapter(new LcioEventSupplier(config.timeout, config.maxQueueSize));
        }
        
        // Max records was set?
        if (config.maxRecords != -1) {            
            compositeAdapter.addProcessor(new MaxRecordsProcessor(config.maxRecords));
        }                 
    }    
}