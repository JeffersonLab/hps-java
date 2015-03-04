package org.hps.monitoring.application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.util.ErrorHandler;
import org.hps.monitoring.subsys.et.EtSystemMonitor;
import org.hps.monitoring.subsys.et.EtSystemStripCharts;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.composite.EventProcessingThread;
import org.hps.record.enums.DataSourceType;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.util.Driver;

/**
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class EventProcessing {
    
    SessionState state;
    ErrorHandler errorHandler;
    Logger logger;
    
    EventProcessing(SessionState state, Logger logger, ErrorHandler errorHandler) {
        this.state = state;
        this.logger = logger;        
        this.errorHandler = errorHandler;        
    }

    void setup(ConfigurationModel configurationModel) {
        logger.info("setting up LCSim");

        // Get steering resource or file as a String parameter.
        String steering = null;
        SteeringType steeringType = configurationModel.getSteeringType();
        if (steeringType.equals(SteeringType.FILE))
            try {
                steering = configurationModel.getSteeringFile().getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        else
            steering = configurationModel.getSteeringResource();

        logger.config("Set steering to " + steering + " with type " + (steeringType == SteeringType.RESOURCE ? "RESOURCE" : "FILE"));

        try {
            // Create and the job manager.  The conditions manager is instantiated from this call but not configured.
            state.jobManager = new JobManager();
            
            if (configurationModel.hasValidProperty(ConfigurationModel.DETECTOR_ALIAS_PROPERTY)) {
                // Set a detector alias.                
                ConditionsReader.addAlias(configurationModel.getDetectorName(), "file://" + configurationModel.getDetectorAlias());
                logger.config("using detector alias " + configurationModel.getDetectorAlias());
            }
                        
            // Setup the event builder to translate from EVIO to LCIO.
            // This must happen before Driver setup so the builder's listeners are activated first!
            createEventBuilder(configurationModel);
            
            // Configure the job manager for the XML steering.
            state.jobManager.setPerformDryRun(true);
            if (steeringType == SteeringType.RESOURCE) {
                setupSteeringResource(steering);
            } else if (steeringType.equals(SteeringType.FILE)) {
                setupSteeringFile(steering);
            }
           
            // Is there a user specified run number from the JobPanel?
            if (configurationModel.hasValidProperty(ConfigurationModel.USER_RUN_NUMBER_PROPERTY)) {
                int userRunNumber = configurationModel.getUserRunNumber();
                String detectorName = configurationModel.getDetectorName();
                DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
                logger.config("setting user run number " + userRunNumber + " with detector " + detectorName);
                conditionsManager.setDetector(configurationModel.getDetectorName(), userRunNumber);
                if (configurationModel.hasPropertyKey(ConfigurationModel.FREEZE_CONDITIONS_PROPERTY)) {
                    // Freeze the conditions system to ignore run numbers from the events.  
                    logger.config("user configured to freeze conditions system from monitoring app");
                    conditionsManager.freeze();
                } else {
                    // Allow run numbers to be picked up from the events.
                    logger.config("user run number specified but conditions system is NOT frozen");
                    conditionsManager.unfreeze();
                }
            }

            logger.info("LCSim setup was successful.");

        } catch (Throwable t) {
            // Catch all errors and rethrow them as RuntimeExceptions.
            errorHandler.setError(t).setMessage("Error setting up LCSim.").printStackTrace().raiseException();
        }
        
        // Setup the CompositeLoop.
        setupLoop(configurationModel);
    }
    
    /**
     * Create the event builder for converting EVIO events to LCSim.
     */
    void createEventBuilder(ConfigurationModel configurationModel) {

        // Get the class for the event builder.
        String eventBuilderClassName = configurationModel.getEventBuilderClassName();

        //logger.config("initializing event builder: " + eventBuilderClassName);

        try {
            // Create a new instance of the builder class.
            state.eventBuilder = (LCSimEventBuilder) Class.forName(eventBuilderClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LCSimEventBuilder.", e);
        }

        // Add the builder as a listener so it is notified when conditions change.
        ConditionsManager.defaultInstance().addConditionsListener(state.eventBuilder);

        //logger.config("successfully initialized event builder: " + eventBuilderClassName);
    }
    
    void setupLoop(ConfigurationModel configurationModel) {

        CompositeLoopConfiguration loopConfig = new CompositeLoopConfiguration()
            .setStopOnEndRun(configurationModel.getDisconnectOnEndRun())
            .setStopOnErrors(configurationModel.getDisconnectOnError())
            .setDataSourceType(configurationModel.getDataSourceType())
            .setProcessingStage(configurationModel.getProcessingStage())
            .setEtConnection(state.connection)
            .setFilePath(configurationModel.getDataSourcePath())
            .setLCSimEventBuilder(state.eventBuilder)
            .setDetectorName(configurationModel.getDetectorName());

        if (configurationModel.hasValidProperty(ConfigurationModel.MAX_EVENTS_PROPERTY)) {
            long maxEvents = configurationModel.getMaxEvents();
            if (maxEvents > 0L) {
                //logger.config("processing will stop after max events: " + maxEvents);
                loopConfig.setMaxRecords(maxEvents);
            }
        }
        
        // Add all Drivers from the JobManager.
        for (Driver driver : state.jobManager.getDriverExecList()) {
            loopConfig.add(driver);
        }

        // Using ET server?
        if (configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER)) {

            // ET system monitor.
            loopConfig.add(new EtSystemMonitor());

            // ET system strip charts.
            loopConfig.add(new EtSystemStripCharts());
        }

        // FIXME: Do this externally?
        // RunPanel updater.
        //loopConfig.add(frame.runPanel.new RunModelUpdater());
                
        // Setup for conditions activation via EVIO events.
        loopConfig.add(new EvioDetectorConditionsProcessor(configurationModel.getDetectorName()));

        // Create the CompositeLoop with the configuration.
        state.loop = new CompositeLoop(loopConfig);        
    }    
    
    
    
    /**
     * Stop the event processing by executing a <code>STOP</code> command on the record loop and
     * killing the event processing thread. This is executed after the ET system is disconnected so
     * that the event processing does not potentially hang in a call to
     * <code>EtSystem.getEvents()</code> forever.
     */
    void stop() {

        // Is the event processing thread not null?
        if (state.processingThread != null) {

            // Is the event processing thread actually still alive?
            if (state.processingThread.isAlive()) {

                // Request the event processing loop to execute stop.
                state.loop.execute(Command.STOP);

                try {
                    // This should always work, because the ET system is disconnected before this.
                    state.processingThread.join();
                } catch (InterruptedException e) {
                    // Don't know when this would ever happen.
                    e.printStackTrace();
                }
            }

            // Notify of last error that occurred in event processing.
            if (state.loop.getLastError() != null) {
                errorHandler.setError(state.loop.getLastError()).log().printStackTrace();
            }

            // Set the event processing thread to null as it is unusable now.
            state.processingThread = null;
        }

        // Set the loop to null as a new one will be created for next session.
        state.loop = null;
    }    
    
    void setupSteeringFile(String steering) {
        //logger.config("setting up steering file: " + steering);
        state.jobManager.setup(new File(steering));
    }

    void setupSteeringResource(String steering) throws IOException {
        //logger.config("setting up steering resource: " + steering);
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(steering);
        if (is == null)
            throw new IOException("Steering resource is not accessible or does not exist.");
        state.jobManager.setup(is);
        is.close();
    }
    
    void start() {
        
        // Create the processing thread.
        state.processingThread = new EventProcessingThread(state.loop);

        // Start the processing thread.
        state.processingThread.start();
    }
    

    
}
