package org.hps.monitoring.application;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.monitoring.application.model.SteeringType;
import org.hps.monitoring.application.util.ErrorHandler;
import org.hps.monitoring.application.util.EtSystemUtil;
import org.hps.monitoring.application.util.SyncEventProcessor;
import org.hps.monitoring.subsys.et.EtSystemMonitor;
import org.hps.monitoring.subsys.et.EtSystemStripCharts;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.composite.EventProcessingThread;
import org.hps.record.enums.DataSourceType;
import org.hps.record.epics.EpicsEtProcessor;
import org.hps.record.et.EtConnection;
import org.hps.record.et.EtEventProcessor;
import org.hps.record.et.EtStationThread;
import org.hps.record.et.PreStartProcessor;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.hps.record.evio.EvioEventConstants;
import org.jlab.coda.et.EtConstants;
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

    SessionState sessionState;
    MonitoringApplication application;
    ConfigurationModel configurationModel;
    ConnectionStatusModel connectionModel;
    ErrorHandler errorHandler;
    Logger logger;
    
    /**
     * This class is used to organize the objects for an event processing session.
     */
    class SessionState {
        
        List<CompositeRecordProcessor> processors;
        List<Driver> drivers;
        List<ConditionsListener> conditionsListeners;
        
        JobManager jobManager;
        LCSimEventBuilder eventBuilder;
        CompositeLoop loop;
        
        boolean usingEtServer;
        
        EventProcessingThread processingThread;
        Thread sessionWatchdogThread;
        ThreadGroup stationThreadGroup = new ThreadGroup("Station Threads");
        List<EtStationThread> stations = new ArrayList<EtStationThread>();
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
        logger = MonitoringApplication.logger;        
        configurationModel = application.configurationModel;
        connectionModel = application.connectionModel;
        errorHandler = application.errorHandler;
        
        sessionState = new SessionState();                        
        sessionState.processors = processors;
        sessionState.drivers = drivers;
        sessionState.conditionsListeners = conditionsListeners;
        sessionState.usingEtServer = application.configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER);
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
    void setupLcsim(ConfigurationModel configurationModel) {
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
            for (ConditionsListener conditionsListener : sessionState.conditionsListeners) {
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
            throw new RuntimeException("Error setting up LCSim.", t);
        }
    }

    /**
     * Create the event builder for converting EVIO events to LCSim.
     */
    void createEventBuilder(ConfigurationModel configurationModel) {

        // Get the class for the event builder.
        String eventBuilderClassName = configurationModel.getEventBuilderClassName();

        try {
            // Create a new instance of the builder class.
            sessionState.eventBuilder = (LCSimEventBuilder) Class.forName(eventBuilderClassName, true, Thread.currentThread().getContextClassLoader()).newInstance();
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
        for (CompositeRecordProcessor processor : sessionState.processors) {
            loopConfig.add(processor);
            logger.config("added extra processor " + processor.getClass().getSimpleName() + " to job");
        }

        // Add extra Drivers to the loop config.
        for (Driver driver : sessionState.drivers) {
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

    synchronized void stop() {

        // Kill session watchdog thread.
        killWatchdogThread();

        // Wake up all ET stations to unblock the system and make sure secondary stations are detached.
        if (usingEtServer()) {
            wakeUpEtStations();   
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
            errorHandler.setError(sessionState.loop.getLastError()).log();
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
    void wakeUpEtStations() {
        if (sessionState.connection != null) {
            logger.fine("waking up ET stations ...");

            // Wake up secondary ET stations.
            for (EtStationThread station : sessionState.stations) {
                
                // First unblock if in ET call.
                station.wakeUp();
                
                // Next interrupt so that it will definitely stop.
                station.interrupt();
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
            
            sessionState.stationThreadGroup.destroy();
            
            logger.finest("station threads destroyed");

            // Wake up the primary ET station doing the event processing.
            logger.finest("waking up event processing station ...");
            try {
                sessionState.connection.getEtSystem().wakeUpAll(sessionState.connection.getEtStation());
                logger.finest("event processing station was woken up");
            } catch (IOException | EtException | EtClosedException e) {
                e.printStackTrace();
            }

            logger.finest("ET stations all woken up");
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

        // Start the watch dog thread which will auto-disconnect when event processing is done.
        sessionState.sessionWatchdogThread = new SessionWatchdogThread(sessionState.processingThread);
        sessionState.sessionWatchdogThread.start();

        logger.fine("started event processing threads");
    }

    /**
     * Notify the event processor to pause processing.
     */
    synchronized void pause() {
        logger.finest("pausing");
        if (!connectionModel.getPaused()) {
            sessionState.loop.pause();
            connectionModel.setPaused(true);
        }
        logger.finest("paused");
    }

    /**
     * Get next event if in pause mode.
     */
    synchronized void next() {
        logger.finest("getting next event");
        if (connectionModel.getPaused()) {
            connectionModel.setPaused(false);
            sessionState.loop.execute(Command.GO_N, 1L, true);
            connectionModel.setPaused(true);
        }
        logger.finest("got next event");
    }

    /**
     * Resume processing events from pause mode.
     */
    synchronized void resume() {
        logger.finest("resuming");
        if (connectionModel.getPaused()) {
            // Notify event processor to continue.
            sessionState.loop.resume();
            connectionModel.setPaused(false);
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
     * True if the processing thread is valid and active.
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
                //createSyncStation();
                
                // Add an attachment which listens for EPICs events with scalar data.
                //createEpicsStation();
                
                // Add an attachment that listens for PRESTART events.
                //createPreStartStation();
                
            } catch (Exception e) {
                throw new IOException(e);
            }
            
            logger.fine("ET system is connected");
        } else {
            // This is when a direct file source is used and ET is not needed.
            connectionModel.setConnectionStatus(ConnectionStatus.CONNECTED);
        }
        
    }

    /**
     * True if using an ET server.
     * @return True if using an ET server.
     */
    boolean usingEtServer() {
        return sessionState.usingEtServer;
    }

    /**
     * Create a connection to an ET system using current parameters from the GUI. 
     */
    synchronized void createEtConnection() {
        // Setup connection to ET system.
        sessionState.connection = EtSystemUtil.createEtConnection(configurationModel);

        if (sessionState.connection != null) {
            // Set status to connected as there is now a live ET connection.
            connectionModel.setConnectionStatus(ConnectionStatus.CONNECTED);
        } else {
            errorHandler.setError(new RuntimeException("Failed to create ET connection.")).log().printStackTrace().raiseException();
        }
    }       
    
    /**
     * Create the select array from event selection in ET stations.
     * @return The select array.
     */
    static int[] createSelectArray() {
        int select[] = new int[EtConstants.stationSelectInts];
        Arrays.fill(select, -1);
        return select;   
    }    
     
    /**
     * Create a station that listens for physics sync events
     * containing DAQ configuration.
     */
    void createSyncStation() {
        
        // Sync events have bits 6 and 7 set.
        int syncEventType = 0;
        syncEventType = syncEventType ^ (1 << 6); 
        syncEventType = syncEventType ^ (1 << 7);
        int select[] = createSelectArray();
        select[1] = syncEventType;
        
        createStationThread(
                new SyncEventProcessor(),
                "SYNC", 
                1,
                select);
    }                                                                                                                                                                                                                                                                               
    
    /**
     * Create a station that listens for PRESTART events
     * to initialize the conditions system.
     */
    void createPreStartStation() {
                
        // Select only PRESTART events.
        int[] select = createSelectArray();
        select[0] = EvioEventConstants.PRESTART_EVENT_TAG;
        
        createStationThread(
                new PreStartProcessor(configurationModel.getDetectorName()),
                "PRESTART",
                1,
                select);
    }
    
    /**
     * Create a station that listens for EPICS control events (currently not activated).     
     */
    void createEpicsStation() {
        
        // Select only EPICS events.
        int[] select = createSelectArray();
        select[0] = EvioEventConstants.EPICS_EVENT_TAG;
        
        createStationThread(
                new EpicsEtProcessor(),
                "EPICS",
                1,
                select);
    }
    
    /**
     * Create an ET station thread.
     * @param processor The event processor to run on the thread.
     * @param nameAppend The string to append for naming this station.
     * @param stationPosition The position of the station.
     * @param select The event selection data array.
     */
    void createStationThread(EtEventProcessor processor, String nameAppend, int stationPosition, int[] select) {
        EtStationThread stationThread = new EtStationThread(
                processor,
                sessionState.connection.getEtSystem(),
                sessionState.connection.getEtStation().getName() + "_" + nameAppend,
                stationPosition,
                select);
        new Thread(sessionState.stationThreadGroup, stationThread).start();
        sessionState.stations.add(stationThread);
        logger.config("started ET station " + nameAppend);
        StringBuffer sb = new StringBuffer();
        for (int word : select) {
            sb.append(word + " ");
        }
        logger.config("station has select array: " + sb.toString());
    }

    /**
     * Disconnect from the current ET session.
     * @param status The connection status.
     */
    synchronized void disconnect() {
                
        // Cleanup the ET connection.
        if (usingEtServer()) {
            closeEtConnection();
        }

        // Change application state to disconnected.
        connectionModel.setConnectionStatus(ConnectionStatus.DISCONNECTED);
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
    
    /**
     * Invalidate all 
     */
    void invalidate() {

        application = null;
        logger = null;
        configurationModel = null;
        connectionModel = null;
        errorHandler = null;
        
        sessionState.conditionsListeners = null;
        sessionState.drivers = null;        
        sessionState.processors = null;        
        sessionState.jobManager = null;
        sessionState.eventBuilder = null;
        sessionState.loop = null;
        sessionState.processingThread = null;
        sessionState.sessionWatchdogThread = null;
        sessionState.stationThreadGroup = null;
        sessionState.stations = null;
        sessionState.connection = null;
        
        sessionState = null;
    }
}
