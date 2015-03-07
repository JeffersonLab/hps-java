package org.hps.monitoring.application;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.SteeringType;
import org.hps.monitoring.application.util.EtSystemUtil;
import org.hps.monitoring.subsys.et.EtSystemMonitor;
import org.hps.monitoring.subsys.et.EtSystemStripCharts;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.composite.EventProcessingThread;
import org.hps.record.enums.DataSourceType;
import org.hps.record.et.EtConnection;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.util.Driver;

/**
 * This class encapsulates all of the logic involved with processing events 
 * and managing the related state and objects within the monitoring application.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class EventProcessing {
    
    MonitoringApplication application;
    Logger logger;
    SessionState sessionState;
    List<CompositeRecordProcessor> processors;
    List<Driver> drivers;
    
    /**
     * This class is used to organize the objects for an event processing session.
     */
    class SessionState {
        JobManager jobManager;
        LCSimEventBuilder eventBuilder;
        CompositeLoop loop;
        EventProcessingThread processingThread;
        Thread sessionWatchdogThread;
        EtConnection connection;
    }
    
    /**
     * Initialize with reference to the current monitoring application
     * and a list of extra processors to add to the loop after 
     * configuration.
     * @param application The current monitoring application.
     * @param processors A list of processors to add after configuration is performed.
     */
    EventProcessing(
            MonitoringApplication application, 
            List<CompositeRecordProcessor> processors,
            List<Driver> drivers) {
        this.application = application;
        this.sessionState = new SessionState();        
        this.logger = MonitoringApplication.logger;
        this.processors = processors;
        this.drivers = drivers;
    }
    
    /**
     * Setup this class from the global configuration.
     * @param configurationModel The global configuration.
     */
    void setup(ConfigurationModel configurationModel) {
        MonitoringApplication.logger.info("setting up LCSim");

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

        MonitoringApplication.logger.config("Set steering to " + steering + " with type " + (steeringType == SteeringType.RESOURCE ? "RESOURCE" : "FILE"));

        try {
            // Create and the job manager.  The conditions manager is instantiated from this call but not configured.
            sessionState.jobManager = new JobManager();
            
            if (configurationModel.hasValidProperty(ConfigurationModel.DETECTOR_ALIAS_PROPERTY)) {
                // Set a detector alias.                
                ConditionsReader.addAlias(configurationModel.getDetectorName(), "file://" + configurationModel.getDetectorAlias());
                logger.config("using detector alias " + configurationModel.getDetectorAlias());
            }
                        
            // Setup the event builder to translate from EVIO to LCIO.
            // This must happen before Driver setup so the builder's listeners are activated first!
            createEventBuilder(configurationModel);
            
            // Configure the job manager for the XML steering.
            sessionState.jobManager.setPerformDryRun(true);
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

            logger.info("lcsim setup was successful");

        } catch (Throwable t) {
            // Catch all errors and rethrow them as RuntimeExceptions.
            application.errorHandler.setError(t).setMessage("Error setting up LCSim.").printStackTrace().raiseException();
        }
        
        // Now setup the CompositeLoop.
        setupLoop(configurationModel);
    }
    
    /**
     * Create the event builder for converting EVIO events to LCSim.
     */
    void createEventBuilder(ConfigurationModel configurationModel) {

        // Get the class for the event builder.
        String eventBuilderClassName = configurationModel.getEventBuilderClassName();

        try {
            // Create a new instance of the builder class.
            sessionState.eventBuilder = (LCSimEventBuilder) Class.forName(eventBuilderClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LCSimEventBuilder.", e);
        }

        // Add the builder as a listener so it is notified when conditions change.
        ConditionsManager.defaultInstance().addConditionsListener(sessionState.eventBuilder);
    }
    
    /**
     * Setup the loop from the global configuration.
     * @param configurationModel The global configuration.
     */
    void setupLoop(ConfigurationModel configurationModel) {

        CompositeLoopConfiguration loopConfig = new CompositeLoopConfiguration()
            .setStopOnEndRun(configurationModel.getDisconnectOnEndRun())
            .setStopOnErrors(configurationModel.getDisconnectOnError())
            .setDataSourceType(configurationModel.getDataSourceType())
            .setProcessingStage(configurationModel.getProcessingStage())
            .setEtConnection(sessionState.connection)
            .setFilePath(configurationModel.getDataSourcePath())
            .setLCSimEventBuilder(sessionState.eventBuilder);

        if (configurationModel.hasValidProperty(ConfigurationModel.MAX_EVENTS_PROPERTY)) {
            long maxEvents = configurationModel.getMaxEvents();
            if (maxEvents > 0L) {
                //logger.config("processing will stop after max events: " + maxEvents);
                loopConfig.setMaxRecords(maxEvents);
            }
        }
        
        // Add all Drivers from the JobManager.
        for (Driver driver : sessionState.jobManager.getDriverExecList()) {
            loopConfig.add(driver);
        }

        // Using ET server?
        if (configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER)) {

            // ET system monitor.
            loopConfig.add(new EtSystemMonitor());

            // ET system strip charts.
            loopConfig.add(new EtSystemStripCharts());
        }

        // Add extra CompositeRecordProcessors to the loop config.
        for (CompositeRecordProcessor processor : processors) {
            loopConfig.add(processor);   
        }
        
        // Add extra Drivers to the loop config.
        for (Driver driver : drivers) {
            loopConfig.add(driver);
        }
                
        // Enable conditions system activation from EVIO event information.
        loopConfig.add(new EvioDetectorConditionsProcessor(configurationModel.getDetectorName()));

        // Create the CompositeLoop with the configuration.
        sessionState.loop = new CompositeLoop(loopConfig);        
    }    
    
    /**
     * Setup a steering file on disk.
     * @param steering The steering file.
     */
    void setupSteeringFile(String steering) {
        sessionState.jobManager.setup(new File(steering));
    }

    /**
     * Setup a steering resource.
     * @param steering The steering resource.
     * @throws IOException if there is a problem setting up or accessing the resource.
     */
    void setupSteeringResource(String steering) throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(steering);
        if (is == null)
            throw new IOException("Steering resource is not accessible or does not exist.");
        sessionState.jobManager.setup(is);
        is.close();
    }
    
    /**
     * Stop the event processing by executing a <code>STOP</code> command on the record loop and
     * killing the event processing thread. This is executed after the ET system is disconnected so
     * that the event processing does not potentially hang in a call to
     * <code>EtSystem.getEvents()</code> forever.
     */
    synchronized void stop() {

        logger.info("event processing is stopping");
        
        // Disconnect from ET system.
        disconnect();
        
        // Is the event processing thread not null?
        if (sessionState.processingThread != null) {

            // Is the event processing thread actually still alive?
            if (sessionState.processingThread.isAlive()) {

                // Request the event processing loop to execute stop.
                sessionState.loop.execute(Command.STOP);

                try {
                    logger.info("waiting for event processing thread to finish");
                    // This should always work, because the ET system is disconnected before this.
                    sessionState.processingThread.join();
                    logger.info("event processing thread finished");
                } catch (InterruptedException e) {
                    // Don't know when this would ever happen.
                    e.printStackTrace();
                }
            }

            // Notify of last error that occurred in event processing.
            if (sessionState.loop.getLastError() != null) {
                application.errorHandler.setError(sessionState.loop.getLastError()).log().printStackTrace();
            }

            // Set the event processing thread to null as it is unusable now.
            sessionState.processingThread = null;
        }

        // Set the loop to null as a new one will be created for next session.
        sessionState.loop = null;
        
        logger.info("event processing stopped");
    }    
           
    /**
     * Start event processing on the event processing thread
     * and start the watchdog thread.
     */
    synchronized void start() {
        
        // Start the event processing thread.
        sessionState.processingThread = new EventProcessingThread(sessionState.loop);
        sessionState.processingThread.start();
        
        // Start the watchdog thread which will auto-disconnect when event processing is done.
        sessionState.sessionWatchdogThread = new SessionWatchdogThread(sessionState.processingThread);
        sessionState.sessionWatchdogThread.start();        
    }
    
    /**
     * Notify the event processor to pause processing.
     */
    synchronized void pause() {
        if (!application.connectionModel.getPaused()) {
            sessionState.loop.pause();
            application.connectionModel.setPaused(true);
        }
    }
    
    /**
     * Get next event if in pause mode.
     */
    synchronized void next() {
        if (application.connectionModel.getPaused()) {
            application.connectionModel.setPaused(false);
            sessionState.loop.execute(Command.GO_N, 1L, true);
            application.connectionModel.setPaused(true);
        }
    }
    
    /**
     * Resume processing events from pause mode.
     */
    synchronized void resume() {
        if (application.connectionModel.getPaused()) {
            // Notify event processor to continue.
            sessionState.loop.resume();        
            application.connectionModel.setPaused(false);
        }
    }
    
    /**
     * Interrupt and join to the processing watchdog thread.
     */
    synchronized void killWatchdogThread() {
        // Is the session watchdog thread not null?
        if (sessionState.sessionWatchdogThread != null) {
            // Is the thread still alive?
            if (sessionState.sessionWatchdogThread.isAlive()) {
                // Interrupt the thread which should cause it to stop.
                sessionState.sessionWatchdogThread.interrupt();
                try {
                    // This should always work once the thread is interrupted.
                    sessionState.sessionWatchdogThread.join();
                } catch (InterruptedException e) {
                    // This should never happen.
                    e.printStackTrace();
                }
            }
            // Set the thread object to null.
            sessionState.sessionWatchdogThread = null;
        }
    }
    
    /**
     * Cleanup the ET connection.
     */
    synchronized void closeEtConnection() {
        if (sessionState.connection != null) {
            if (sessionState.connection.getEtSystem().alive()) {
                sessionState.connection.cleanup();
            }
            sessionState.connection = null;
        }        
    }
    
    /**
     * True if the processing thread is active.
     * @return True if processing thread is active.
     */
    boolean isActive() {
        return sessionState.processingThread != null && sessionState.processingThread.isAlive();
    }
    
    /**
     * Connect to the ET system using the current connection settings.
     */
    synchronized void connect() throws IOException {

        // Setup the network connection if using an ET server.
        if (usingEtServer()) {
            // Create a connection to the ET server.
            try {
                createEtConnection();
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            // This is when a direct file source is used and ET is not needed.
            application.connectionModel.setConnectionStatus(ConnectionStatus.CONNECTED);
        }
    }
    
    /**
     * True if using an ET server.
     * @return True if using an ET server.
     */
    boolean usingEtServer() {
        return application.configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER);
    }    
    
    /**
     * Create a connection to an ET system using current parameters from the GUI. If successful, the
     * application's ConnectionStatus is changed to CONNECTED.
     */
    void createEtConnection() {
        // Setup connection to ET system.
        sessionState.connection = EtSystemUtil.createEtConnection(application.configurationModel);

        if (sessionState.connection != null) {
            // Set status to connected as there is now a live ET connection.
            application.connectionModel.setConnectionStatus(ConnectionStatus.CONNECTED);
            //logger.info("successfully connected to ET system");
        } else {
            application.errorHandler.setError(new RuntimeException("Failed to create ET connection.")).log().printStackTrace().raiseException();
        }
    }
    
    /**
     * Disconnect from the current ET session with a particular status.
     * @param status The connection status.
     */
    synchronized void disconnect() {
        
        // Kill the session watch dog thread.
        killWatchdogThread();

        // Cleanup the ET connection.
        closeEtConnection();
                              
        // Change application state to disconnected.
        application.connectionModel.setConnectionStatus(ConnectionStatus.DISCONNECTED);
    }    
               
    /**
     * This class notifies the application to disconnect if the event processing thread completes.     
     */
    class SessionWatchdogThread extends Thread {

        Thread processingThread;

        SessionWatchdogThread(Thread processingThread) {
            this.processingThread = processingThread;
        }
        
        public void run() {
            try {
                // When the event processing thread finishes, the session should be stopped and a
                // disconnect should occur.
                processingThread.join();
                                
                // Activate a disconnect using the ActionEvent which is used by the disconnect button.
                application.actionPerformed(new ActionEvent(Thread.currentThread(), 0, Commands.DISCONNECT));
                               
            } catch (InterruptedException e) {
                // This happens when the thread is interrupted by the user pressing the disconnect button.
            }            
        }
    }
}
