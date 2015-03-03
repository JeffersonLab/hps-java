package org.hps.monitoring.application;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.hps.monitoring.application.RunPanel.RunModelUpdater;
import org.hps.monitoring.application.model.Configuration;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.RunModel;
import org.hps.monitoring.application.util.ErrorHandler;
import org.hps.monitoring.application.util.EtSystemUtil;
import org.hps.monitoring.subsys.SystemStatusRegistry;
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
import org.lcsim.util.aida.AIDA;

public class MonitoringApplication implements PropertyChangeListener {

    static Logger logger;
    Handler logHandler;
    
    ErrorHandler errorHandler;
   
    MonitoringActionListener actions;
    MonitoringApplicationFrame frame;    
    SettingsDialog settingsDialog = new SettingsDialog();
    
    RunModel runModel = new RunModel();
    ConfigurationModel configurationModel = new ConfigurationModel();
    
    SessionState sessionState;
    
    // The default configuration resource.
    private static final String DEFAULT_CONFIG_RESOURCE = "/org/hps/monitoring/config/default_config.prop";

    // The application's global Configuration settings.
    private Configuration configuration;
        
    class LogHandler extends Handler {

        /**
         * This method inserts a record into the log table.
         */
        public void publish(LogRecord record) {
            frame.logTable.insert(record);
        }

        public void close() throws SecurityException {
        }

        public void flush() {
        }
    }    
             
    MonitoringApplication(Configuration configuration) {
        
        // Setup the error handler.
        this.errorHandler = new ErrorHandler(frame, logger);
        
        // Set the configuration.
        if (configuration != null) {
            // User specified configuration.
            this.configuration = configuration;
        } else {
            // Use the default configuration resource.
            this.configuration = new Configuration(DEFAULT_CONFIG_RESOURCE);
        }
        
        // Setup the action listener.
        actions = new MonitoringActionListener(this);
        
        // Setup the main GUI component.
        frame = new MonitoringApplicationFrame(actions);
        frame.setRunModel(runModel);
        
        // Setup the logger.
        setupLogger();
        
        // Setup the settings dialog box.
        settingsDialog.addActionListener(actions);
        settingsDialog.setConfigurationModel(configurationModel);
        
        // Add this class as a listener on the configuration model.
        configurationModel.addPropertyChangeListener(this);      
        
        // Load the configuration.
        loadConfiguration();
    }
    
    private void setupLogger() {
        logger = Logger.getLogger(MonitoringApplication.class.getSimpleName());
        logHandler = new LogHandler();
        logger.setUseParentHandlers(false);
        logger.addHandler(logHandler);
        logger.setLevel(Level.ALL);
    }
        
    public static MonitoringApplication create(Configuration configuration) {
        return new MonitoringApplication(configuration);
    }    
    
    public static MonitoringApplication create() {
        return create(new Configuration(DEFAULT_CONFIG_RESOURCE));
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // TODO Auto-generated method stub        
    }
    
    void loadConfiguration() {

        // Set the Configuration on the ConfigurationModel which will trigger all the PropertyChangelListeners.
        configurationModel.setConfiguration(configuration);

        // Log that a new configuration was loaded.
        if (configuration.getFile() != null)
            logger.config("Loaded configuration from file: " + configuration.getFile().getPath());
        else
            logger.config("Loaded configuration from resource: " + configuration.getResourcePath());
    }
    
    /**
     * Start a new monitoring session. This method is executed in a separate thread from the EDT
     * within {@link #actionPerformed(ActionEvent)} so GUI updates are not blocked while the session
     * is being setup.
     */
    void startSession() {

        logger.fine("Starting a new monitoring session.");

        // Show a modal window that will block the GUI until connected or an error occurs.
        //JDialog dialog = DialogUtil.showStatusDialog(this, "Info", "Starting new session ...");

        try {
            
            sessionState = new SessionState();

            // Reset the plot panel and global AIDA state.
            resetPlots();

            // The system status registry is cleared here before any event processors
            // which might create a SystemStatus are added to the event processing chain
            // e.g. an LCSim Driver, etc.
            SystemStatusRegistry.getSystemStatusRegistery().clear();

            // Setup the LCSim JobControlManager and event builder.
            setupLCSim();

            // Connect to the ET system.
            connect();

            // Setup the EventProcessingChain object using the EtConnection.
            setupCompositeLoop();
            
            // Start the event processing thread.
            startEventProcessingThread();

            // Setup the system status monitor table.
            //setupSystemStatusMonitor();

            // Start thread which will trigger a disconnect if the event processing finishes.
            //startSessionWatchdogThread();            

            logger.info("successfully started the monitoring session");

        } catch (Exception e) {

            logger.severe("error occurred while setting up the session");

            // Log the error that occurred.
            errorHandler.setError(e).log().printStackTrace();

            // Disconnect from the session.
            //disconnect(ConnectionStatus.ERROR);

        } finally {
            // Close modal window.
            //dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
        }
    }
    
    /**
     * Connect to the ET system using the current connection settings.
     */
    private void connect() throws IOException {

        // Make sure applicable menu items are enabled or disabled.
        // This applies whether or not using an ET server or file source.
        //setConnectedGuiState();

        // Setup the network connection if using an ET server.
        if (usingEtServer()) {

            setConnectionStatus(ConnectionStatus.CONNECTION_REQUESTED);

            // Create a connection to the ET server.
            try {
                createEtConnection();
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            // This is when a direct file source is used and ET is not needed.
            setConnectionStatus(ConnectionStatus.CONNECTED);
        }
    }
    
    boolean usingEtServer() {
        return configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER);
    }    
    
    /**
     * Create a connection to an ET system using current parameters from the GUI. If successful, the
     * application's ConnectionStatus is changed to CONNECTED.
     */
    private void createEtConnection() {

        // Setup connection to ET system.
        sessionState.connection = EtSystemUtil.createEtConnection(configurationModel);

        if (sessionState.connection != null) {

            // Set status to connected as there is now a live ET connection.
            setConnectionStatus(ConnectionStatus.CONNECTED);

            logger.info("successfully connected to ET system");

        } else {
            // Some error occurred and the connection was not created.
            setConnectionStatus(ConnectionStatus.ERROR);

            errorHandler.setError(new RuntimeException("Failed to create ET connection.")).log().printStackTrace().raiseException();
        }
    }    
    
    /**
     * Set the connection status.
     * @param status The connection status.
     */
    void setConnectionStatus(ConnectionStatus status) {
        // FIXME
        //frame.connectionStatusPanel.setConnectionStatus(status);
        logger.info("connection status changed to: " + status.name());
        logHandler.flush();
    }
    
    void resetPlots() {

        // Clear the static AIDA tree in case plots are hanging around from previous sessions.
        AIDA.defaultInstance().clearAll();

        // Reset plots.
        frame.plotPanel.reset();
    }           
    
    /**
     * Setup the LCSim job manager and the event builder.
     */
    void setupLCSim() {

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
            sessionState.jobManager = new JobManager();
            
            if (configurationModel.hasValidProperty(ConfigurationModel.DETECTOR_ALIAS_PROPERTY)) {
                // Set a detector alias.                
                ConditionsReader.addAlias(configurationModel.getDetectorName(), "file://" + configurationModel.getDetectorAlias());
                logger.config("using detector alias " + configurationModel.getDetectorAlias());
            }
                        
            // Setup the event builder to translate from EVIO to LCIO.
            // This must happen before Driver setup so the builder's listeners are activated first!
            createEventBuilder();
            
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

            logger.info("LCSim setup was successful.");

        } catch (Throwable t) {
            // Catch all errors and rethrow them as RuntimeExceptions.
            errorHandler.setError(t).setMessage("Error setting up LCSim.").printStackTrace().raiseException();
        }
    }

    void setupSteeringFile(String steering) {
        logger.config("setting up steering file: " + steering);
        sessionState.jobManager.setup(new File(steering));
    }

    void setupSteeringResource(String steering) throws IOException {
        logger.config("setting up steering resource: " + steering);
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(steering);
        if (is == null)
            throw new IOException("Steering resource is not accessible or does not exist.");
        sessionState.jobManager.setup(is);
        is.close();
    }
        
    /**
     * Create the event builder for converting EVIO events to LCSim.
     */
    void createEventBuilder() {

        // Get the class for the event builder.
        String eventBuilderClassName = configurationModel.getEventBuilderClassName();

        logger.config("initializing event builder: " + eventBuilderClassName);

        try {
            // Create a new instance of the builder class.
            sessionState.eventBuilder = (LCSimEventBuilder) Class.forName(eventBuilderClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LCSimEventBuilder.", e);
        }

        // Add the builder as a listener so it is notified when conditions change.
        ConditionsManager.defaultInstance().addConditionsListener(sessionState.eventBuilder);

        logger.config("successfully initialized event builder: " + eventBuilderClassName);
    }
    
    void setupCompositeLoop() {

        CompositeLoopConfiguration loopConfig = new CompositeLoopConfiguration()
            .setStopOnEndRun(configurationModel.getDisconnectOnEndRun())
            .setStopOnErrors(configurationModel.getDisconnectOnError())
            .setDataSourceType(configurationModel.getDataSourceType())
            .setProcessingStage(configurationModel.getProcessingStage())
            .setEtConnection(sessionState.connection)
            .setFilePath(configurationModel.getDataSourcePath())
            .setLCSimEventBuilder(sessionState.eventBuilder)
            .setDetectorName(configurationModel.getDetectorName());

        if (configurationModel.hasValidProperty(ConfigurationModel.MAX_EVENTS_PROPERTY)) {
            long maxEvents = configurationModel.getMaxEvents();
            if (maxEvents > 0L) {
                logger.config("processing will stop after max events: " + maxEvents);
                loopConfig.setMaxRecords(maxEvents);
            }
        }
        
        // Add all Drivers from the JobManager.
        for (Driver driver : sessionState.jobManager.getDriverExecList()) {
            loopConfig.add(driver);
        }

        // Using ET server?
        if (usingEtServer()) {

            // ET system monitor.
            loopConfig.add(new EtSystemMonitor());

            // ET system strip charts.
            loopConfig.add(new EtSystemStripCharts());
        }

        // RunPanel updater.
        loopConfig.add(frame.runPanel.new RunModelUpdater());
                
        // Setup for conditions activation via EVIO events.
        loopConfig.add(new EvioDetectorConditionsProcessor(configurationModel.getDetectorName()));

        // Create the CompositeLoop with the configuration.
        sessionState.loop = new CompositeLoop(loopConfig);        
    }    
    
    void startEventProcessingThread() {
        
        // Create the processing thread.
        sessionState.processingThread = new EventProcessingThread(sessionState.loop);

        // Start the processing thread.
        sessionState.processingThread.start();
    }
}
