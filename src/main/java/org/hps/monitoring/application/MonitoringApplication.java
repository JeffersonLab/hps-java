package org.hps.monitoring.application;

import hep.aida.jfree.AnalysisFactory;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.jfree.plotter.PlotterRegionListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.hps.monitoring.application.RunPanel.RunPanelUpdater;
import org.hps.monitoring.application.model.Configuration;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.monitoring.application.model.RunModel;
import org.hps.monitoring.application.util.ErrorHandler;
import org.hps.monitoring.application.util.EtSystemUtil;
import org.hps.monitoring.plotting.MonitoringAnalysisFactory;
import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusListener;
import org.hps.monitoring.subsys.SystemStatusRegistry;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.enums.DataSourceType;
import org.lcsim.util.aida.AIDA;

/**
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class MonitoringApplication implements PropertyChangeListener, SystemStatusListener {

    static Logger logger;
    static {
        logger = Logger.getLogger(MonitoringApplication.class.getSimpleName());
    }
    Handler logHandler;
    
    ErrorHandler errorHandler;
   
    MonitoringApplicationFrame frame;    
    ActionListener actionListener = new MonitoringApplicationActionListener(this);
    
    RunModel runModel = new RunModel();
    ConfigurationModel configurationModel = new ConfigurationModel();
    ConnectionStatusModel connectionModel = new ConnectionStatusModel();
    
    SessionState sessionState;
    EventProcessing processing;
    
    // The default configuration resource.
    static final String DEFAULT_CONFIGURATION = "/org/hps/monitoring/config/default_config.prop";

    // The application's global Configuration settings.
    Configuration configuration;
        
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
        errorHandler = new ErrorHandler(frame, logger);
        
        // Setup the main GUI component.
        frame = new MonitoringApplicationFrame(this);
                        
        // Add this class as a listener on the configuration model.
        configurationModel.addPropertyChangeListener(this);
        
        // Setup the logger.
        setupLogger();
        
        // Setup AIDA plotting and connect it to the GUI.
        setupAida();
        
        // Set the configuration.
        if (configuration != null) {
            // User specified configuration.
            this.configuration = configuration;
        } else {
            // Use the default configuration resource.
            this.configuration = new Configuration(DEFAULT_CONFIGURATION);
        }
                                      
        // Load the configuration.
        loadConfiguration(this.configuration);
    }
    
    void setupAida() {
        MonitoringAnalysisFactory.register();
        MonitoringPlotFactory.setRootPane(frame.plotPanel.getPlotPane());
        MonitoringPlotFactory.setPlotterRegionListener(new PlotterRegionListener() {
            @Override
            public void regionSelected(PlotterRegion region) {
                if (region == null)
                    throw new RuntimeException("The region arg is null!!!");
                frame.plotInfoPanel.setCurrentRegion(region);
            }
        });
        AnalysisFactory.configure();
    }
    
    void setupLogger() {
        logHandler = new LogHandler();
        logger.setUseParentHandlers(false);
        logger.addHandler(logHandler);
        logger.setLevel(Level.ALL);        
    }
        
    public static MonitoringApplication create(Configuration configuration) {
        return new MonitoringApplication(configuration);
    }    
    
    public static MonitoringApplication create() {
        return create(new Configuration(DEFAULT_CONFIGURATION));
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // TODO Auto-generated method stub
    }
    
    void loadConfiguration(Configuration configuration) {

        // Set the Configuration on the ConfigurationModel which will trigger all the PropertyChangelListeners.
        configurationModel.setConfiguration(configuration);

        // Log that a new configuration was loaded.
        //if (configuration.getFile() != null)
            //logger.config("Loaded configuration from file: " + configuration.getFile().getPath());
        //else
            //logger.config("Loaded configuration from resource: " + configuration.getResourcePath());
    }
   
    
    /**
     * Connect to the ET system using the current connection settings.
     */
    void connect() throws IOException {

        // Make sure applicable menu items are enabled or disabled.
        // This applies whether or not using an ET server or file source.
        //setConnectedGuiState();

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
            connectionModel.setConnectionStatus(ConnectionStatus.CONNECTED);
        }
    }
    
    /**
     * 
     * @return
     */
    boolean usingEtServer() {
        return configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER);
    }    
    
    /**
     * Create a connection to an ET system using current parameters from the GUI. If successful, the
     * application's ConnectionStatus is changed to CONNECTED.
     */
    void createEtConnection() {

        // Setup connection to ET system.
        sessionState.connection = EtSystemUtil.createEtConnection(configurationModel);

        if (sessionState.connection != null) {

            // Set status to connected as there is now a live ET connection.
            connectionModel.setConnectionStatus(ConnectionStatus.CONNECTED);

            //logger.info("successfully connected to ET system");

        } else {
            errorHandler.setError(new RuntimeException("Failed to create ET connection.")).log().printStackTrace().raiseException();
        }
    }    
        
    void resetPlots() {

        // Clear the static AIDA tree in case plots are hanging around from previous sessions.
        AIDA.defaultInstance().clearAll();

        // Reset plots.
        frame.plotPanel.reset();
    }           
                         
        
    /**
     * Disconnect from the current ET session with a particular status.
     * @param status The connection status.
     */
    void disconnect() {

        //logger.fine("Disconnecting the current session.");

        // Cleanup the ET connection.
        cleanupEtConnection();

        // Change application state to disconnected.
        connectionModel.setConnectionStatus(ConnectionStatus.DISCONNECTED);

        //logger.info("Disconnected from the session.");
    }    
    
    /**
     * Cleanup the ET connection.
     */
    void cleanupEtConnection() {
        if (sessionState != null) {
            if (sessionState.connection != null) {
                if (sessionState.connection.getEtSystem().alive()) {
                    sessionState.connection.cleanup();
                }
                sessionState.connection = null;
            }
        }
    }
    
    /**
     * Configure the system status monitor panel for a new job.
     */
    void setupSystemStatusMonitor() {
        // Clear the system status monitor table.
        frame.systemStatusTable.getTableModel().clear();

        // Get the global registry of SystemStatus objects.
        SystemStatusRegistry registry = SystemStatusRegistry.getSystemStatusRegistery();

        // Process the SystemStatus objects.
        for (SystemStatus systemStatus : registry.getSystemStatuses()) {
            // Add a row to the table for every SystemStatus.
            frame.systemStatusTable.getTableModel().addSystemStatus(systemStatus);

            // Add this class as a listener so all status changes can be logged.
            systemStatus.addListener(this);
        }
    }
    
    /**
     * Hook for logging all status changes from the system status monitor.
     */
    @Override
    public void statusChanged(SystemStatus status) {

        // Choose the appropriate log level.
        Level level = Level.INFO;
        if (status.getStatusCode().equals(Level.WARNING)) {
            level = Level.WARNING;
        } else if (status.getStatusCode().ordinal() >= StatusCode.ERROR.ordinal()) {
            level = Level.SEVERE;
        }

        // Log all status changes.
        //logger.log(level, "STATUS, " + "subsys: " + status.getSubsystem() + ", " 
        //        + "code: " + status.getStatusCode().name() 
        //        + ", " + "descr: " + status.getDescription() 
        //       + ", " + "mesg: " + status.getMessage());
    }
    
    /**
     * Start a new monitoring session. This method is executed in a separate thread from the EDT
     * within {@link #actionPerformed(ActionEvent)} so GUI updates are not blocked while the session
     * is being setup.
     */
    void startSession() {

        //logger.fine("Starting a new monitoring session.");

        // Show a modal window that will block the GUI until connected or an error occurs.
        //JDialog dialog = DialogUtil.showStatusDialog(this, "Info", "Starting new session ...");

        try {
            
            // Reset the plot panel and global AIDA state.
            resetPlots();

            // The system status registry is cleared here before any event processors
            // which might create a SystemStatus are added to the event processing chain
            // e.g. an LCSim Driver, etc.
            SystemStatusRegistry.getSystemStatusRegistery().clear();

            // Setup event processing.
            sessionState = new SessionState();
            List<CompositeRecordProcessor> processors = new ArrayList<CompositeRecordProcessor>();
            processors.add(frame.runPanel.new RunPanelUpdater());
            processing = new EventProcessing(this, processors);
            processing.setup(configurationModel);
                                  
            // Setup the system status monitor table.
            setupSystemStatusMonitor();
            
            // Connect to the ET system.
            connect();
          
            // Start event processing.
            processing.start();
            
            //logger.info("successfully started the monitoring session");

        } catch (Exception e) {

            //logger.severe("error occurred while setting up the session");

            // Log the error that occurred.
            errorHandler.setError(e).log().printStackTrace();

            // Disconnect from the session.
            // FIXME: This should never be needed as connected should only be set at end w/o errors.
            disconnect();

        } finally {
            // Close modal window.
            //dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
        }
    }
    
    /**
     * Stop the session by killing the event processing thread, ending the job, and disconnecting
     * from the ET system.
     */
    void stopSession() {
        // Show a modal message window while this method executes.
        //JDialog dialog = DialogUtil.showStatusDialog(this, "Info", "Disconnecting from session ...");

        try {
            // Log message.
            //logger.log(Level.FINER, "stopping the session");

            // Kill the watchdog thread which looks for disconnects, if it is active.
            processing.killWatchdogThread();
            
            // Disconnect from ET system, if using the ET server, and set the proper disconnected
            // GUI state.
            disconnect();

            // Stop the event processing, which is called after the ET system goes down to avoid
            // hanging in calls to ET system.
            processing.stop();

            //logger.log(Level.INFO, "session was stopped");

        } finally {
            // Close modal message window.
            //dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
        }
    }
    
    /**
     * Exit from the application.
     */
    void exit() {
        cleanupEtConnection();
        frame.setVisible(false);
        System.exit(0);
    }              
}