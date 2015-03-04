package org.hps.monitoring.application;

import static org.hps.monitoring.application.Commands.*;
import hep.aida.jfree.AnalysisFactory;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.jfree.plotter.PlotterRegionListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.hps.monitoring.application.model.Configuration;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.monitoring.application.model.RunModel;
import org.hps.monitoring.application.util.DialogUtil;
import org.hps.monitoring.application.util.ErrorHandler;
import org.hps.monitoring.application.util.EtSystemUtil;
import org.hps.monitoring.plotting.MonitoringAnalysisFactory;
import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusListener;
import org.hps.monitoring.subsys.SystemStatusRegistry;
import org.hps.record.enums.DataSourceType;
import org.lcsim.util.aida.AIDA;

public class MonitoringApplication implements PropertyChangeListener, ActionListener, SystemStatusListener {

    static Logger logger;
    static {
        logger = Logger.getLogger(MonitoringApplication.class.getSimpleName());
    }
    Handler logHandler;
    
    ErrorHandler errorHandler;
   
    MonitoringApplicationFrame frame;    
    
    RunModel runModel = new RunModel();
    ConfigurationModel configurationModel = new ConfigurationModel();
    ConnectionStatusModel connectionModel = new ConnectionStatusModel();
    
    SessionState sessionState;
    EventProcessing processing;
    
    // The default configuration resource.
    private static final String DEFAULT_CONFIGURATION = "/org/hps/monitoring/config/default_config.prop";

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
        
        // Setup the main GUI component, passing it the data models and this object as the primary ActionListener.
        frame = new MonitoringApplicationFrame(configurationModel, runModel, connectionModel, this);
                        
        // Add this class as a listener on the configuration model.
        configurationModel.addPropertyChangeListener(this);
        
        // Setup the logger.
        setupLogger();
        
        // Setup plotting backend and connect to the GUI.
        setupAida();
        
        // Set the configuration.
        if (configuration != null) {
            // User specified configuration.
            this.configuration = configuration;
        } else {
            // Use the default configuration resource.
            this.configuration = new Configuration(DEFAULT_CONFIGURATION);
        }
                                      
        // Load the current configuration.
        loadConfiguration();
    }
    
    void setupAida() {
        MonitoringAnalysisFactory.register();
        MonitoringPlotFactory.setRootPane(frame.plotPanel.getPlotPane());
        MonitoringPlotFactory.setPlotterRegionListener(new PlotterRegionListener() {
            @Override
            public void regionSelected(PlotterRegion region) {
                if (region == null)
                    throw new RuntimeException("The region arg is null!!!");
                // System.out.println("MonitoringApplication - regionSelected - " + region.title());
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
    
    void loadConfiguration() {

        // Set the Configuration on the ConfigurationModel which will trigger all the PropertyChangelListeners.
        configurationModel.setConfiguration(configuration);

        // Log that a new configuration was loaded.
        //if (configuration.getFile() != null)
            //logger.config("Loaded configuration from file: " + configuration.getFile().getPath());
        //else
            //logger.config("Loaded configuration from resource: " + configuration.getResourcePath());
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
            processing = new EventProcessing(sessionState, logger, errorHandler);
            processing.setup(configurationModel);
                        
            // Add the dashboard updater.
            sessionState.loop.getCompositeLoopAdapters().get(0).addProcessor(frame.runPanel.new RunModelUpdater());
          
            // Setup the system status monitor table.
            setupSystemStatusMonitor();
            
            // Connect to the ET system.
            connect();
          
            // Start event processing.
            processing.start();
            
            // Start thread which will trigger a disconnect if the event processing finishes.
            //startSessionWatchdogThread();            

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
     * The action handler method for the application.
     * @param e The event to handle.
     */
    public void actionPerformed(ActionEvent e) {

        System.out.println("MonitoringApplication.actionPerformed - " + e.getActionCommand());

        String cmd = e.getActionCommand();
        if (CONNECT.equals(cmd)) {
            // Run the start session method on a separate thread.
            new Thread() {
                public void run() {
                    startSession();
                }
            }.start();
        } else if (DISCONNECT.equals(cmd)) {
            // Run the stop session method on a separate thread.
            new Thread() {
                public void run() {
                    stopSession();
                }
            }.start();
        } else if (SAVE_PLOTS.equals(cmd)) {
            //savePlots();
        } else if (CHOOSE_LOG_FILE.equals(cmd)) {
            //chooseLogFile();
        } else if (LOG_TO_TERMINAL.equals(cmd)) {
            //logToTerminal();
        } else if (SCREENSHOT.equals(cmd)) {
            //chooseScreenshot();
        } else if (EXIT.equals(cmd)) {
            //exit();
        } else if (SAVE_LOG_TABLE.equals(cmd)) {
            //saveLogTableToFile();
        } else if (CLEAR_LOG_TABLE.equals(cmd)) {
            //clearLogTable();
        } else if (PAUSE.equals(cmd)) {
            //pauseEventProcessing();
        } else if (NEXT.equals(cmd)) {
            //nextEvent();
        } else if (RESUME.equals(cmd)) {
            //resumeEventProcessing();
        } else if (LOG_LEVEL_CHANGED.equals(cmd)) {
            //setLogLevel();
        } else if (AIDA_AUTO_SAVE.equals(cmd)) {
            //getJobSettingsPanel().chooseAidaAutoSaveFile();
        } else if (SETTINGS_SHOW.equals(cmd)) {
            showConfigurationDialog();
        } else if (SETTINGS_LOAD.equals(cmd)) {
            //chooseConfigurationFile();
        } else if (SETTINGS_SAVE.equals(cmd)) {
            //updateLayoutConfiguration(); /* Save current GUI layout settings first, if needed. */
            //saveConfigurationFile();
        } else if (SAVE_LAYOUT.equals(cmd)) {
            //setSaveLayout();
        } else if (RESTORE_DEFAULT_GUI_LAYOUT.equals(cmd)) {
            //restoreDefaultLayout();
        } else if (VALIDATE_DATA_FILE.equals(cmd)) {
            //if (fileValidationThread == null) {
            //    new FileValidationThread().start();
            //}
        } else if (RESET_PLOTS.equals(cmd)) {
            //resetAidaTree();
        } else if (SETTINGS_LOAD_DEFAULT.equals(cmd)) {
            loadDefaultSettings();
            DialogUtil.showInfoDialog(frame,
                    "Default Configuration Loaded", 
                    "The default configuration was loaded from resource " + '\n' + DEFAULT_CONFIGURATION);
        } else if (OPEN_FILE.equals(cmd)) {
            openFile();
        }
    }
        
    /**
     * Disconnect from the current ET session with a particular status.
     * @param status The connection status.
     */
    void disconnect() {

        //logger.fine("Disconnecting the current session.");

        // Cleanup the ET connection.
        cleanupEtConnection();

        // Update state of GUI to disconnected.
        //setDisconnectedGuiState();

        // Change application state to disconnected.
        connectionModel.setConnectionStatus(ConnectionStatus.DISCONNECTED);

        //logger.info("Disconnected from the session.");
    }    
    
    /**
     * Cleanup the ET connection.
     */
    void cleanupEtConnection() {
        if (sessionState.connection != null) {
            if (sessionState.connection.getEtSystem().alive()) {
                //logger.fine("cleaning up ET connection");
                sessionState.connection.cleanup();
                //logger.fine("done cleaning up tET connection");
            }
            sessionState.connection = null;
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
            //killSessionWatchdogThread();

            // Automatically write AIDA file from job settings.
            //saveAidaFile();

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
    
    void loadDefaultSettings() {
        configuration = new Configuration(DEFAULT_CONFIGURATION);
        configurationModel.setConfiguration(configuration);
    }
    
    void showConfigurationDialog() {
        frame.settingsDialog.setVisible(true);
    }
    
    /**
     * This is a simple file filter that will accept files with ".evio" anywhere in their name. 
     */
    static class EvioFileFilter extends FileFilter {

        public EvioFileFilter() {            
        }
        
        @Override
        public boolean accept(File pathname) {
            if (pathname.getName().contains(".evio") || pathname.isDirectory()) {
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public String getDescription() {
            return "EVIO files";
        }        
    }
    
    /**
     * Open a file data source using a <code>JFileChooser</code>.
     */
    static FileFilter lcioFilter = new FileNameExtensionFilter("LCIO files", "slcio");
    static EvioFileFilter evioFilter = new EvioFileFilter();
    void openFile() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(lcioFilter);
        fc.addChoosableFileFilter(evioFilter);
        fc.setDialogTitle("Select Data File");
        int r = fc.showDialog(frame, "Select ...");        
        if (r == JFileChooser.APPROVE_OPTION) {
                                  
            // Set data source path.            
            final String filePath = fc.getSelectedFile().getPath();
            configurationModel.setDataSourcePath(filePath);
            
            // Set data source type.
            FileFilter filter = fc.getFileFilter();
            if (filter == lcioFilter) {
                configurationModel.setDataSourceType(DataSourceType.LCIO_FILE);
            } else if (filter == evioFilter) {
                configurationModel.setDataSourceType(DataSourceType.EVIO_FILE);
            }
        }
    }    
    
    void saveSettings() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Configuration");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            //log(Level.CONFIG, "Saving configuration to file <" + f.getPath() + ">");
            configuration.writeToFile(f);
        }
    }
    
    
}
