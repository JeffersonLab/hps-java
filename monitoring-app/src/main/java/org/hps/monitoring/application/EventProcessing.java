package org.hps.monitoring.application;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.SteeringType;
import org.hps.monitoring.application.util.EtSystemUtil;
import org.hps.monitoring.application.util.PhysicsSyncEventStation;
import org.hps.monitoring.application.util.PreStartEtStation;
import org.hps.monitoring.application.util.RunnableEtStation;
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
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.exception.EtClosedException;
import org.jlab.coda.et.exception.EtException;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.util.Driver;

/**
 * This class encapsulates all of the logic involved with processing events and managing the related
 * state and objects within the monitoring application.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class EventProcessing {

    MonitoringApplication application;
    Logger logger;
    SessionState sessionState;
    List<CompositeRecordProcessor> processors;
    List<Driver> drivers;
    List<ConditionsListener> conditionsListeners;
    int stationPosition;

    /**
     * This class is used to organize the objects for an event processing session.
     */
    class SessionState {
        JobManager jobManager;
        LCSimEventBuilder eventBuilder;
        CompositeLoop loop;
        EventProcessingThread processingThread;
        Thread sessionWatchdogThread;
        ThreadGroup stationThreadGroup = new ThreadGroup("Station Threads");
        List<RunnableEtStation> stations = new ArrayList<RunnableEtStation>();
        EtConnection connection;
    }

    /**
     * Initialize with reference to the current monitoring application and a list of extra
     * processors to add to the loop after configuration.
     * @param application The current monitoring application.
     * @param processors A list of processors to add after configuration is performed.
     */
    EventProcessing(
            MonitoringApplication application, 
            List<CompositeRecordProcessor> processors, 
            List<Driver> drivers, 
            List<ConditionsListener> conditionsListeners) {
        this.application = application;
        this.sessionState = new SessionState();
        this.logger = MonitoringApplication.logger;
        this.processors = processors;
        this.drivers = drivers;
        this.conditionsListeners = conditionsListeners;
        this.stationPosition = application.configurationModel.getStationPosition();
    }
    
    int getNextStationPosition() {
        this.stationPosition += 1;
        return this.stationPosition;        
    }

    /**
     * Setup this class from the global configuration.
     * @param configurationModel The global configuration.
     */
    void setup(ConfigurationModel configurationModel) {
        
        // Setup LCSim from the configuration.
        setupLcsim(configurationModel);

        // Now setup the CompositeLoop.
        setupLoop(configurationModel);
    }

    /**
     * @param configurationModel
     */
    private void setupLcsim(ConfigurationModel configurationModel) {
        MonitoringApplication.logger.info("setting up lcsim");

        // Get steering resource or file as a String parameter.
        String steering = null;
        SteeringType steeringType = configurationModel.getSteeringType();
        if (steeringType.equals(SteeringType.FILE)) {
            steering = configurationModel.getSteeringFile();
        } else {
            steering = configurationModel.getSteeringResource();
        }

        MonitoringApplication.logger.config("set steering " + steering + " with type " + (steeringType == SteeringType.RESOURCE ? "RESOURCE" : "FILE"));

        try {
            // Create and the job manager. The conditions manager is instantiated from this call but
            // not configured.
            sessionState.jobManager = new JobManager();

            // Add conditions listeners after new database conditions manager is initialized from
            // job manager.
            DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
            for (ConditionsListener conditionsListener : conditionsListeners) {
                logger.config("adding conditions listener " + conditionsListener.getClass().getName());
                conditionsManager.addConditionsListener(conditionsListener);
            }

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

            // Set conditions tag.
            if (configurationModel.hasValidProperty(ConfigurationModel.CONDITIONS_TAG_PROPERTY) && !configurationModel.getConditionsTag().equals("")) {
                logger.config("conditions tag is set to " + configurationModel.getConditionsTag());
            } else {
                logger.config("conditions NOT using a tag");
            }

            // Is there a user specified run number from the JobPanel?
            if (configurationModel.hasValidProperty(ConfigurationModel.USER_RUN_NUMBER_PROPERTY)) {
                int userRunNumber = configurationModel.getUserRunNumber();
                String detectorName = configurationModel.getDetectorName();
                logger.config("setting user run number " + userRunNumber + " with detector " + detectorName);
                conditionsManager.setDetector(configurationModel.getDetectorName(), userRunNumber);
                if (configurationModel.hasPropertyKey(ConfigurationModel.FREEZE_CONDITIONS_PROPERTY)) {
                    // Freeze the conditions system to ignore run numbers from the events.
                    logger.config("user configured to freeze conditions system");
                    conditionsManager.freeze();
                } else {
                    // Allow run numbers to be picked up from the events.
                    logger.config("user run number provided but conditions system is NOT frozen");
                    conditionsManager.unfreeze();
                }
            }

            logger.info("lcsim setup was successful");

        } catch (Throwable t) {
            // Catch all errors and re-throw them as RuntimeExceptions.
            application.errorHandler.setError(t).setMessage("Error setting up LCSim.").printStackTrace().raiseException();
        }
    }

    /**
     * Create the event builder for converting EVIO events to LCSim.
     */
    private void createEventBuilder(ConfigurationModel configurationModel) {

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
    private void setupLoop(ConfigurationModel configurationModel) {

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
                loopConfig.setMaxRecords(maxEvents);
            }
        }

        // Add all Drivers from the JobManager.
        for (Driver driver : sessionState.jobManager.getDriverExecList()) {
            loopConfig.add(driver);
            logger.config("added Driver " + driver.getName() + " to job");
        }

        // Using ET server?
        if (configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER)) {

            // ET system monitor.
            logger.config("added EtSystemMonitor to job");
            loopConfig.add(new EtSystemMonitor());

            // ET system strip charts.
            logger.config("added EtSystemStripCharts to job");
            loopConfig.add(new EtSystemStripCharts());
        }

        // Add extra CompositeRecordProcessors to the loop config.
        for (CompositeRecordProcessor processor : processors) {
            loopConfig.add(processor);
            logger.config("added extra processor " + processor.getClass().getSimpleName() + " to job");
        }

        // Add extra Drivers to the loop config.
        for (Driver driver : drivers) {
            loopConfig.add(driver);
            logger.config("added extra Driver " + driver.getName() + " to job");
        }

        // Enable conditions system activation from EVIO event data in case the PRESTART is missed.
        logger.config("added EvioDetectorConditionsProcessor to job with detector " + configurationModel.getDetectorName());
        loopConfig.add(new EvioDetectorConditionsProcessor(configurationModel.getDetectorName()));

        // Create the CompositeLoop with the configuration.
        sessionState.loop = new CompositeLoop(loopConfig);
    }

    /**
     * Setup a steering file on disk.
     * @param steering The steering file.
     */
    private void setupSteeringFile(String steering) {
        sessionState.jobManager.setup(new File(steering));
    }

    /**
     * Setup a steering resource.
     * @param steering The steering resource.
     * @throws IOException if there is a problem setting up or accessing the resource.
     */
    private void setupSteeringResource(String steering) throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(steering);
        if (is == null)
            throw new IOException("Steering resource is not accessible or does not exist.");
        sessionState.jobManager.setup(is);
        is.close();
    }

    synchronized void stop() {

        // Kill session watchdog thread.
        killWatchdogThread();

        // Wake up all ET stations to unblock the system and make sure secondary stations are detached.
        //wakeUpEtStations()
        // Wake up the primary ET station doing the event processing.
        logger.finest("waking up event processing station ...");
        try {
            if (sessionState.connection != null) {
                if (sessionState.connection.getEtSystem() != null) {
                    sessionState.connection.getEtSystem().wakeUpAll(sessionState.connection.getEtStation());
                    logger.finest("event processing station woken up");
                }
            }
        } catch (IOException | EtException | EtClosedException e) {
            e.printStackTrace();
        }
        
        // Stop the event processing now that ET system is unblocked.
        logger.fine("sending STOP command to loop ...");
        sessionState.loop.execute(Command.STOP);
        logger.fine("loop got command STOP");

        // Cleanup the event processing thread since it was told to stop now.
        try {
            logger.fine("waiting for event processing thread to end ...");
            sessionState.processingThread.join();
            logger.fine("event processing thread ended");   
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Notify of last error that occurred in event processing.
        if (sessionState.loop.getLastError() != null) {
            // Log the error.
            application.errorHandler.setError(sessionState.loop.getLastError()).log();
        }

        // Invalidate the loop.
        sessionState.loop = null;

        // Disconnect from the ET system.
        disconnect();
        
        // Invalidate the event processing object so it is unusable now.
        invalidate();
    }

    /**
     * Wake up all ET stations associated with event processing.
     */
    private void wakeUpEtStations() {
        if (sessionState.connection != null) {
            logger.fine("waking up ET stations ...");

            // Wake up secondary ET stations.
            for (RunnableEtStation station : sessionState.stations) {
                if (station.getEtStation().isUsable()) {
                    // Wake up the station which will automatically trigger a detach.
                    try {
                        logger.finest("waking up " + station.getEtStation().getName() + " ...");
                        sessionState.connection.getEtSystem().wakeUpAll(station.getEtStation());
                        logger.finest(station.getEtStation().getName() + " woken up");
                    } catch (IOException | EtException | EtClosedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Wait for station threads to die after being woken up.
            while (sessionState.stationThreadGroup.activeCount() != 0) {
                logger.finest("waiting for station threads to die ...");
                Object lock = new Object();
                synchronized (lock) {
                    try {
                        lock.wait(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            logger.finest("destroying station thread group");
            sessionState.stationThreadGroup.destroy();
            logger.finest("station thread group destroyed");

            // Wake up the primary ET station doing the event processing.
            logger.finest("waking up event processing station ...");
            try {
                sessionState.connection.getEtSystem().wakeUpAll(sessionState.connection.getEtStation());
                logger.finest("event processing station woken up");
            } catch (IOException | EtException | EtClosedException e) {
                e.printStackTrace();
            }

            logger.finest("ET stations woken up");
        }
    }

    /**
     * Start event processing on the event processing thread and start the watchdog thread.
     */
    synchronized void start() {

        logger.fine("event processing threads are starting");

        // Start the event processing thread.
        sessionState.processingThread = new EventProcessingThread(sessionState.loop);
        sessionState.processingThread.start();

        // Start the watchdog thread which will auto-disconnect when event processing is done.
        sessionState.sessionWatchdogThread = new SessionWatchdogThread(sessionState.processingThread);
        sessionState.sessionWatchdogThread.start();

        logger.fine("started event processing threads");
    }

    /**
     * Notify the event processor to pause processing.
     */
    synchronized void pause() {
        logger.finest("pausing");
        if (!application.connectionModel.getPaused()) {
            sessionState.loop.pause();
            application.connectionModel.setPaused(true);
        }
        logger.finest("paused");
    }

    /**
     * Get next event if in pause mode.
     */
    synchronized void next() {
        logger.finest("getting next event");
        if (application.connectionModel.getPaused()) {
            application.connectionModel.setPaused(false);
            sessionState.loop.execute(Command.GO_N, 1L, true);
            application.connectionModel.setPaused(true);
        }
        logger.finest("got next event");
    }

    /**
     * Resume processing events from pause mode.
     */
    synchronized void resume() {
        logger.finest("resuming");
        if (application.connectionModel.getPaused()) {
            // Notify event processor to continue.
            sessionState.loop.resume();
            application.connectionModel.setPaused(false);
        }
        logger.finest("resumed");
    }

    /**
     * Interrupt and join to the processing watchdog thread.
     */
    synchronized void killWatchdogThread() {
        // Is the session watchdog thread not null?
        if (sessionState.sessionWatchdogThread != null) {
            logger.finest("killing watchdog thread ...");
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
            logger.finest("watchdog thread killed");
        }
    }

    /**
     * Cleanup the ET connection.
     */
    synchronized void closeEtConnection() {
        if (sessionState.connection != null) {
            logger.fine("closing ET connection");
            if (sessionState.connection.getEtSystem().alive()) {
                logger.finest("cleaning up the connection ...");
                sessionState.connection.cleanup();
                logger.finest("connection cleanup successful");
            }
            sessionState.connection = null;
            logger.fine("ET connection closed");
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
                logger.fine("connecting to ET system ...");
                
                // Create the main ET system connection.
                createEtConnection();

                // Add an attachment that listens for DAQ configuration changes via physics SYNC events.
                //createPhysicsSyncStation();
                
                // Add an attachment that listens for PRESTART events.
                //createPreStartStation();
                
            } catch (Exception e) {
                throw new IOException(e);
            }
            
            logger.fine("ET system is connected");
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
     * Create a connection to an ET system using current parameters from the GUI. 
     */
    synchronized void createEtConnection() {
        // Setup connection to ET system.
        sessionState.connection = EtSystemUtil.createEtConnection(application.configurationModel);

        if (sessionState.connection != null) {
            // Set status to connected as there is now a live ET connection.
            application.connectionModel.setConnectionStatus(ConnectionStatus.CONNECTED);
        } else {
            application.errorHandler.setError(new RuntimeException("Failed to create ET connection.")).log().printStackTrace().raiseException();
        }
    }

    /**
     * Create the ET that listens for DAQ configuration change via SYNC events.
     */
    private void createPhysicsSyncStation() {
        logger.fine("creating physics SYNC station ...");       
        PhysicsSyncEventStation configStation = new PhysicsSyncEventStation(
                this.sessionState.connection.getEtSystem(),
                this.sessionState.connection.getEtStation().getName() + "_PhysicsSync",
                getNextStationPosition());
        sessionState.stations.add(configStation);
        new Thread(sessionState.stationThreadGroup, configStation).start();
        logger.fine("physics SYNC station created");
    }
    
    /**
     * Create the ET station that listens for GO events in order to initialize the conditions system.
     */
    private void createPreStartStation() {
        logger.fine("creating PRESTART station ...");
        String detectorName = this.application.configurationModel.getDetectorName();
        EtSystem system = this.sessionState.connection.getEtSystem();
        String stationName = this.sessionState.connection.getEtStation().getName() + "_PreStart";
        int order = getNextStationPosition();
        PreStartEtStation preStartStation = new PreStartEtStation(
                detectorName, 
                system, 
                stationName, 
                order);
        sessionState.stations.add(preStartStation);
        new Thread(sessionState.stationThreadGroup, preStartStation).start();
        logger.fine("PRESTART station created");
    }

    /**
     * Disconnect from the current ET session.
     * @param status The connection status.
     */
    synchronized void disconnect() {
                
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
                // This thread waits on the event processing thread to die.
                processingThread.join();

                // Activate a disconnect using the ActionEvent which is used by the disconnect button.
                logger.finest("processing thread ended so automatic disconnect is happening");
                application.actionPerformed(new ActionEvent(Thread.currentThread(), 0, Commands.DISCONNECT));

            } catch (InterruptedException e) {
                logger.finest("SessionWatchdogThread got interrupted");
                // This happens when the thread is interrupted by the user pressing the disconnect button.
            }
        }
    }
    
    void invalidate() {

        this.application = null;
        this.conditionsListeners = null;
        this.drivers = null;
        this.logger = null;
        this.processors = null;        

        this.sessionState.jobManager = null;
        this.sessionState.eventBuilder = null;
        this.sessionState.loop = null;
        this.sessionState.processingThread = null;
        this.sessionState.sessionWatchdogThread = null;
        this.sessionState.stationThreadGroup = null;
        this.sessionState.stations = null;
        this.sessionState.connection = null;
        this.sessionState = null;
    }
}
