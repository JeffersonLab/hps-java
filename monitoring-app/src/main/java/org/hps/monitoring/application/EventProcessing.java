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
import org.hps.monitoring.subsys.et.EtSystemMonitor;
import org.hps.monitoring.subsys.et.EtSystemStripCharts;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.composite.EventProcessingThread;
import org.hps.record.enums.DataSourceType;
import org.hps.record.et.EtConnection;
import org.hps.record.et.EtStationThread;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.hps.steering.SteeringFileCatalog;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.exception.EtClosedException;
import org.jlab.coda.et.exception.EtException;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.util.Driver;

/**
 * This class encapsulates all of the logic involved with processing events and managing the related state and objects
 * within the monitoring application.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class EventProcessing {

    /**
     * This class organizes and encapsulates most of the objects used by an event processing session.
     */
    private final class SessionState {

        /**
         * A list of extra {@link org.lcsim.conditions.ConditionsListener} objects to add to the loop.
         */
        private List<ConditionsListener> conditionsListeners;

        /**
         * An {@link org.hps.record.et.EtConnection} with ET configuration (can be null if using a file source).
         */
        private EtConnection connection;

        /**
         * A list of extra {@link org.lcsim.util.Driver} objects to add to the loop.
         */
        private List<Driver> drivers;

        /**
         * The class for building the LCSim events from EVIO data.
         */
        private LCSimEventBuilder eventBuilder;

        /**
         * The LCSim {@link org.hps.job.JobManager} which handles the <code>Driver</code> setup from XML steering files.
         */
        private JobManager jobManager;

        /**
         * The loop which manages the ET to EVIO to LCIO event building and processing.
         */
        private CompositeLoop loop;

        /**
         * The {@link org.hps.record.composite.EventProcessingThread} on which event processing executes.
         */
        private EventProcessingThread processingThread;

        /**
         * The list of extra {@link org.hps.record.composite.CompositeRecordProcessor} objects to add to the loop.
         */
        private List<CompositeRecordProcessor> processors;

        /**
         * A {@link java.lang.Thread} which is used to monitor the event processing.
         */
        private Thread sessionWatchdogThread;

        /**
         * A list of ET stations on separate threads (currently unused).
         */
        private List<EtStationThread> stations = new ArrayList<EtStationThread>();

        /**
         * The ET station thread group (currently unused).
         */
        private ThreadGroup stationThreadGroup = new ThreadGroup("Station Threads");

        /**
         * This is <code>true</code> if the session will connect to a network ET event server.
         */
        private boolean usingEtServer;
    }

    /**
     * This class will cause the application to disconnect from the current event processing session if the event
     * processing thread completes.
     */
    private final class SessionWatchdogThread extends Thread {

        /**
         * A reference to the current {{@link #EventProcessing(Thread)}.
         */
        private final Thread processingThread;

        /**
         * Class constructor.
         *
         * @param processingThread the current {{@link #EventProcessing(Thread)}
         */
        private SessionWatchdogThread(final Thread processingThread) {
            this.processingThread = processingThread;
        }

        /**
         * Run this thread, which will disconnect from the current session if the event processing ends for any reason.
         */
        @Override
        public void run() {
            try {
                // This thread waits on the event processing thread to die.
                this.processingThread.join();

                // Activate a disconnect using the ActionEvent which is used by the disconnect button.
                EventProcessing.this.logger.finest("processing thread ended so automatic disconnect is happening");
                EventProcessing.this.application.actionPerformed(new ActionEvent(Thread.currentThread(), 0,
                        Commands.DISCONNECT));

            } catch (final InterruptedException e) {
                EventProcessing.this.logger.finest("SessionWatchdogThread got interrupted");
                // This happens when the thread is interrupted by the user pressing the disconnect button.
            }
        }
    }

    /**
     * Create the select array from event selection in ET stations (not currently used).
     *
     * @return The select array.
     */
    static int[] createSelectArray() {
        final int[] select = new int[EtConstants.stationSelectInts];
        Arrays.fill(select, -1);
        return select;
    }

    /**
     * Reference to the current application.
     */
    private MonitoringApplication application;

    /**
     * Reference to the global configuration model.
     */
    private ConfigurationModel configurationModel;

    /**
     * Reference to the global connection model.
     */
    private ConnectionStatusModel connectionModel;

    /**
     * The error handler, which is just a reference to the application's error handler.
     */
    private ErrorHandler errorHandler;

    /**
     * The logger to use for message which is the application's logger.
     */
    private Logger logger;

    /**
     * The current {@link EventProcessing.SessionState} object which has all of the session state for event processing.
     */
    private SessionState sessionState;
   
    /**
     * The current conditions manager.
     */ 
    private DatabaseConditionsManager conditionsManager;

    /**
     * Class constructor, which will initialize with reference to the current monitoring application and lists of extra
     * processors to add to the loop, as well as supplemental conditions listeners that activate when the conditions
     * change.
     *
     * @param application the current monitoring application object
     * @param processors a list of processors to add after configuration is performed
     * @param drivers a list of extra {@link org.lcsim.util.Driver} objects to add to the loop
     * @param conditionsListeners a list of extra {@link org.lcsim.conditions.ConditionsListener} to add to the loop
     */
    EventProcessing(final MonitoringApplication application, final List<CompositeRecordProcessor> processors,
            final List<Driver> drivers, final List<ConditionsListener> conditionsListeners) {

        this.application = application;
        this.logger = application.getLogger();
        this.configurationModel = application.getConfigurationModel();
        this.connectionModel = application.getConnectionModel();
        this.errorHandler = application.getErrorHandler();

        this.sessionState = new SessionState();
        this.sessionState.processors = processors;
        this.sessionState.drivers = drivers;
        this.sessionState.conditionsListeners = conditionsListeners;
        this.sessionState.usingEtServer = application.getConfigurationModel().getDataSourceType()
                .equals(DataSourceType.ET_SERVER);
    }

    /**
     * Close the current ET connection.
     * <p>
     * This method does not need to be <code>synchronized</code>, because it is only called from the
     * {@link #disconnect()} method which is itself <code>synchronized</code>.
     */
    private void closeEtConnection() {
        if (this.sessionState.connection != null) {
            this.logger.fine("closing ET connection");
            if (this.sessionState.connection.getEtSystem().alive()) {
                this.logger.finest("cleaning up the connection ...");
                this.sessionState.connection.cleanup();
                this.logger.finest("connection cleanup successful");
            }
            this.sessionState.connection = null;
            this.logger.fine("ET connection closed");
        }
    }

    /**
     * Connect to the ET system using the current connection settings.
     *
     * @throws IOException if any error occurs while creating the ET connection
     */
    synchronized void connect() throws IOException {
        // Setup the network connection if using an ET server.
        if (this.usingEtServer()) {
            // Create a connection to the ET server.
            try {
                this.logger.fine("connecting to ET system ...");

                // Create the main ET system connection.
                this.createEtConnection();

                // FIXME: Separate event processing ET stations not currently used due to synchronization and ET issues.

                // Add an attachment that listens for DAQ configuration changes via physics SYNC events.
                // createSyncStation();

                // Add an attachment which listens for EPICs events with scalar data.
                // createEpicsStation();

                // Add an attachment that listens for PRESTART events.
                // createPreStartStation();

            } catch (final Exception e) {
                throw new IOException(e);
            }

            this.logger.fine("ET system is connected");
        } else {
            // This is when a direct file source is used and ET is not needed.
            this.connectionModel.setConnectionStatus(ConnectionStatus.CONNECTED);
        }

    }

    /**
     * Create a connection to an ET system using current parameters from the GUI.
     * <p>
     * This method does not need to be <code>synchronized</code>, because it is only called from the {@link #connect()}
     * method which is itself <code>synchronized</code>.
     */
    private void createEtConnection() {
        // Setup connection to ET system.
        this.sessionState.connection = EtSystemUtil.createEtConnection(this.configurationModel);

        if (this.sessionState.connection != null) {
            // Set status to connected as there is now a live ET connection.
            this.connectionModel.setConnectionStatus(ConnectionStatus.CONNECTED);
        } else {
            this.errorHandler.setError(new RuntimeException("Failed to create ET connection.")).log().printStackTrace()
                    .raiseException();
        }
    }

    /**
     * Create the event builder for converting EVIO events to LCSim.
     *
     * @param configurationModel the current global {@link org.hps.monitoring.application.ConfigurationModel} object
     */
    private void createEventBuilder(final ConfigurationModel configurationModel) {

        // Get the class for the event builder.
        final String eventBuilderClassName = configurationModel.getEventBuilderClassName();

        try {
            // Create a new instance of the builder class.
            this.sessionState.eventBuilder = (LCSimEventBuilder) Class.forName(eventBuilderClassName, true,
                    Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create LCSimEventBuilder.", e);
        }

        // Add the builder as a listener so it is notified when conditions change.
        this.conditionsManager.addConditionsListener(this.sessionState.eventBuilder);
    }

    /**
     * Disconnect from the current session, closing the ET connection if necessary.
     */
    synchronized void disconnect() {

        // Cleanup the ET connection.
        if (this.usingEtServer()) {
            this.closeEtConnection();
        }

        // Change application state to disconnected.
        this.connectionModel.setConnectionStatus(ConnectionStatus.DISCONNECTED);
    }

    /**
     * Invalidate all of the local variables and session state so that this object is not usable after a disconnect.
     */
    void invalidate() {

        this.application = null;
        this.logger = null;
        this.configurationModel = null;
        this.connectionModel = null;
        this.errorHandler = null;

        this.sessionState.conditionsListeners = null;
        this.sessionState.drivers = null;
        this.sessionState.processors = null;
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

    /**
     * Return <code>true</code> if the event processing thread is valid (non-null) and active.
     *
     * @return <code>true</code> if event processing thread is active
     */
    boolean isActive() {
        return this.sessionState.processingThread != null && this.sessionState.processingThread.isAlive();
    }

    /**
     * Interrupt and join the processing watchdog thread.
     * <p>
     * This will happen if there is a user requested disconnect from pushing the button in the GUI.
     */
    synchronized void killWatchdogThread() {
        // Is the session watchdog thread not null?
        if (this.sessionState.sessionWatchdogThread != null) {
            this.logger.finest("killing watchdog thread ...");
            // Is the thread still alive?
            if (this.sessionState.sessionWatchdogThread.isAlive()) {
                // Interrupt the thread which should cause it to stop.
                this.sessionState.sessionWatchdogThread.interrupt();
                try {
                    // This should always work once the thread is interrupted.
                    this.sessionState.sessionWatchdogThread.join();
                } catch (final InterruptedException e) {
                }
            }
            // Set the thread object to null.
            this.sessionState.sessionWatchdogThread = null;
            this.logger.finest("watchdog thread killed");
        }
    }

    /**
     * Get the next event from the loop if in pause mode.
     */
    synchronized void next() {
        this.logger.finest("getting next event");
        if (this.connectionModel.getPaused()) {
            this.connectionModel.setPaused(false);
            this.sessionState.loop.execute(Command.GO_N, 1L, true);
            this.connectionModel.setPaused(true);
        }
        this.logger.finest("got next event");
    }

    /**
     * Notify the loop to pause the event processing.
     */
    synchronized void pause() {
        if (!this.connectionModel.getPaused()) {
            this.logger.finest("pausing");
            this.sessionState.loop.pause();
            this.connectionModel.setPaused(true);
            this.logger.finest("paused");
        }
    }

    /**
     * Resume processing events from pause mode by resuming loop event processing.
     */
    synchronized void resume() {
        this.logger.finest("resuming");
        if (this.connectionModel.getPaused()) {
            // Notify event processor to continue.
            this.sessionState.loop.resume();
            this.connectionModel.setPaused(false);
        }
        this.logger.finest("resumed");
    }

    /**
     * Setup this class from the global {@link org.hps.monitoring.model.ConfigurationModel} object.
     *
     * @param configurationModel the global @link org.hps.monitoring.model.ConfigurationModel} object
     */
    synchronized void setup(final ConfigurationModel configurationModel) {

        // Setup LCSim from the configuration.
        this.setupLcsim(configurationModel);

        // Now setup the CompositeLoop.
        this.setupLoop(configurationModel);
    }

    /**
     * Setup LCSim event processing from the global {@link org.hps.monitoring.model.ConfigurationModel} object.
     *
     * @param configurationModel the global @link org.hps.monitoring.model.ConfigurationModel} object
     */
    private void setupLcsim(final ConfigurationModel configurationModel) {
        this.logger.info("setting up lcsim");

        // Get steering resource or file as a String parameter.
        String steering = null;
        final SteeringType steeringType = configurationModel.getSteeringType();
        if (steeringType.equals(SteeringType.FILE)) {
            steering = configurationModel.getSteeringFile();
        } else {
            steering = configurationModel.getSteeringResource();
        }

        this.logger.config("set steering " + steering + " with type "
                + (steeringType == SteeringType.RESOURCE ? "RESOURCE" : "FILE"));

        try {
            // Create the job manager. A new conditions manager is instantiated from this call but not configured.
            this.sessionState.jobManager = new JobManager();

            // Set ref to current conditions manager.
            this.conditionsManager = DatabaseConditionsManager.getInstance();
            
            // Add conditions listeners after new database conditions manager is initialized from the job manager.
            for (final ConditionsListener conditionsListener : this.sessionState.conditionsListeners) {
                this.logger.config("adding conditions listener " + conditionsListener.getClass().getName());
                this.conditionsManager.addConditionsListener(conditionsListener);
            }

            if (configurationModel.hasValidProperty(ConfigurationModel.DETECTOR_ALIAS_PROPERTY)) {
                // Set a detector alias.
                ConditionsReader.addAlias(configurationModel.getDetectorName(),
                        "file://" + configurationModel.getDetectorAlias());
                this.logger.config("using detector alias " + configurationModel.getDetectorAlias());
            }

            // Setup the event builder to translate from EVIO to LCIO.
            // This must happen before Driver setup so the builder's listeners are activated first!
            this.createEventBuilder(configurationModel);

            // Configure the job manager for the XML steering.
            this.sessionState.jobManager.setDryRun(true);
            if (steeringType == SteeringType.RESOURCE) {
                this.setupSteeringResource(steering);
            } else if (steeringType.equals(SteeringType.FILE)) {
                this.setupSteeringFile(steering);
            }

            // Set conditions tag if applicable.
            if (configurationModel.hasValidProperty(ConfigurationModel.CONDITIONS_TAG_PROPERTY)
                    && !configurationModel.getConditionsTag().equals("")) {
                this.logger.config("conditions tag is set to " + configurationModel.getConditionsTag());
            } else {
                this.logger.config("conditions NOT using a tag");
            }

            // Is there a user specified run number from the JobPanel?
            if (configurationModel.hasValidProperty(ConfigurationModel.USER_RUN_NUMBER_PROPERTY)) {
                final int userRunNumber = configurationModel.getUserRunNumber();
                final String detectorName = configurationModel.getDetectorName();
                this.logger.config("setting user run number " + userRunNumber + " with detector " + detectorName);
                conditionsManager.setDetector(detectorName, userRunNumber);
                if (configurationModel.hasPropertyKey(ConfigurationModel.FREEZE_CONDITIONS_PROPERTY)) {
                    // Freeze the conditions system to ignore run numbers from the events.
                    this.logger.config("user configured to freeze conditions system");
                    this.conditionsManager.freeze();
                } else {
                    // Allow run numbers to be picked up from the events.
                    this.logger.config("user run number provided but conditions system is NOT frozen");
                    this.conditionsManager.unfreeze();
                }
            }

            this.logger.info("lcsim setup was successful");

        } catch (final Throwable t) {
            throw new RuntimeException("Error setting up LCSim.", t);
        }
    }

    /**
     * Setup the {@link org.hps.record.composite.CompositeLoop} from the global
     * {@link org.hps.monitoring.model.ConfigurationModel} object.
     *
     * @param configurationModel the global {@link org.hps.monitoring.model.ConfigurationModel} object
     */
    private void setupLoop(final ConfigurationModel configurationModel) {

        this.logger.config("setting up record loop ...");

        // Initialize the loop from the ConfigurationModel.
        final CompositeLoopConfiguration loopConfig = new CompositeLoopConfiguration()
                .setStopOnEndRun(configurationModel.getDisconnectOnEndRun())
                .setStopOnErrors(configurationModel.getDisconnectOnError())
                .setDataSourceType(configurationModel.getDataSourceType())
                .setProcessingStage(configurationModel.getProcessingStage())
                .setEtConnection(this.sessionState.connection).setFilePath(configurationModel.getDataSourcePath())
                .setLCSimEventBuilder(this.sessionState.eventBuilder);

        this.logger.config("data src path " + configurationModel.getDataSourcePath() + " and type " + configurationModel.getDataSourceType());

        // Set the max events.
        if (configurationModel.hasValidProperty(ConfigurationModel.MAX_EVENTS_PROPERTY)) {
            final long maxEvents = configurationModel.getMaxEvents();
            if (maxEvents > 0L) {
                loopConfig.setMaxRecords(maxEvents);
            }
        }

        // Add all Drivers from the JobManager.
        for (final Driver driver : this.sessionState.jobManager.getDriverExecList()) {
            loopConfig.add(driver);
            this.logger.config("added Driver " + driver.getName());
        }

        // Using ET server?
        if (configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER)) {

            // ET system monitor.
            this.logger.config("added EtSystemMonitor");
            loopConfig.add(new EtSystemMonitor());

            // ET system strip charts.
            this.logger.config("added EtSystemStripCharts");
            loopConfig.add(new EtSystemStripCharts());
        }

        // Add extra CompositeRecordProcessors to the loop config.
        for (final CompositeRecordProcessor processor : this.sessionState.processors) {
            loopConfig.add(processor);
            this.logger.config("added extra processor " + processor.getClass().getSimpleName());
        }

        // Add extra Drivers to the loop config.
        for (final Driver driver : this.sessionState.drivers) {
            loopConfig.add(driver);
            this.logger.config("added extra Driver " + driver.getName());
        }

        // Enable conditions system activation from EVIO event data in case the PRESTART is missed.
        loopConfig.add(new EvioDetectorConditionsProcessor(configurationModel.getDetectorName()));
        this.logger.config("added EvioDetectorConditionsProcessor to job with detector "
                + configurationModel.getDetectorName());

        // Create the CompositeLoop with the configuration.
        this.sessionState.loop = new CompositeLoop(loopConfig);

        this.logger.config("record loop is setup");
    }

    /**
     * Setup XML steering from a file from disk.
     *
     * @param steering the steering file path
     */
    private void setupSteeringFile(final String steering) {
        this.sessionState.jobManager.setup(new File(steering));
    }

    /**
     * Setup XML steering from a jar resource.
     *
     * @param steering the steering resource
     * @throws IOException if there is a problem accessing or setting up the resource
     */
    private void setupSteeringResource(final String resource) throws IOException {
        final InputStream is = SteeringFileCatalog.getInputStream(resource);
        if (is == null) {
            throw new IOException("Resource " + resource + " is not accessible or does not exist.");
        }
        this.sessionState.jobManager.setup(is);
        is.close();
    }

    /**
     * Start event processing on a separate thread and also start the watchdog thread.
     * <p>
     * This method is called externally by the app to activate event processing after it is initialized and configured.
     */
    synchronized void start() {

        this.logger.fine("event processing threads are starting");

        // Start the event processing thread.
        this.sessionState.processingThread = new EventProcessingThread(this.sessionState.loop);
        this.sessionState.processingThread.start();

        // Start the watch dog thread which will auto-disconnect when event processing is done.
        this.sessionState.sessionWatchdogThread = new SessionWatchdogThread(this.sessionState.processingThread);
        this.sessionState.sessionWatchdogThread.start();

        this.logger.fine("started event processing threads");
    }

    /**
     * Stop the current session, which will bring down the current ET client connection and activate end-of-job and
     * end-of-run hooks on all registered event processors.
     * <p>
     * This method is called externally by the app to stop an event processing session e.g. from action event handling.
     */
    synchronized void stop() {

        // Kill session watchdog thread.
        this.killWatchdogThread();

        // Wake up all ET stations to unblock the system and make sure stations are detached properly.
        if (this.usingEtServer()) {
            this.wakeUpEtStations();
        }

        // Stop the event processing now that ET system should be unblocked.
        this.logger.finer("sending STOP command to loop ...");
        this.sessionState.loop.execute(Command.STOP);
        this.logger.finer("loop got STOP command");

        this.logger.finer("processing thread is alive: " + this.sessionState.processingThread.isAlive());

        try {
            // Give the event processing thread a chance to end cleanly.
            this.logger.finer("waiting for event processing thread to end ...");
            this.sessionState.processingThread.join(5000);
            this.logger.finer("processing thread is alive: " + this.sessionState.processingThread.isAlive());
            // this.logger.finer("event processing thread ended cleanly");
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        try {
            this.logger.finer("processing thread is alive: " + this.sessionState.processingThread.isAlive());
            // In this case the thread needs to be interrupted and then joined.
            this.logger.finer("interrupting event processing thread");
            this.sessionState.processingThread.interrupt();
            this.sessionState.processingThread.join();
            this.logger.finer("event processing thread ended after interrupt");
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        // Notify of last error that occurred in event processing.
        if (this.sessionState.loop.getLastError() != null) {
            // Log the error.
            this.errorHandler.setError(this.sessionState.loop.getLastError()).log();
        }

        // Invalidate the loop.
        this.sessionState.loop = null;

        // Disconnect from the ET system.
        this.disconnect();

        // Invalidate the event processing object so it is unusable now.
        this.invalidate();
    }

    /**
     * Return <code>true</code> if using an ET server.
     *
     * @return <code>true</code> if using an ET server in the current session
     */
    private boolean usingEtServer() {
        return this.sessionState.usingEtServer;
    }

    /**
     * Wake up all ET stations associated with the event processing.
     */
    private void wakeUpEtStations() {
        if (this.sessionState.connection != null) {
            this.logger.fine("waking up ET stations ...");

            // Wake up secondary ET stations.
            for (final EtStationThread station : this.sessionState.stations) {

                // First unblock if in ET call.
                station.wakeUp();

                // Next interrupt so that it will definitely stop.
                station.interrupt();
            }

            // Wait for station threads to die after being woken up.
            while (this.sessionState.stationThreadGroup.activeCount() != 0) {
                this.logger.finest("waiting for station threads to die ...");
                final Object lock = new Object();
                synchronized (lock) {
                    try {
                        lock.wait(500);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            this.sessionState.stationThreadGroup.destroy();

            this.logger.finest("station threads destroyed");

            // Wake up the primary ET station doing the event processing.
            this.logger.finest("waking up event processing station ...");
            try {
                this.sessionState.connection.getEtSystem().wakeUpAll(this.sessionState.connection.getEtStation());
                this.logger.finest("event processing station was woken up");
            } catch (IOException | EtException | EtClosedException e) {
                e.printStackTrace();
            }

            this.logger.finest("ET stations all woken up");
        }
    }
}
