package org.hps.monitoring.application;

import hep.aida.jfree.AnalysisFactory;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.jfree.plotter.PlotterRegionListener;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.hps.monitoring.application.DataSourceComboBox.DataSourceItem;
import org.hps.monitoring.application.LogTable.LogRecordModel;
import org.hps.monitoring.application.model.Configuration;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.monitoring.application.model.RunModel;
import org.hps.monitoring.application.util.AIDAServer;
import org.hps.monitoring.application.util.DialogUtil;
import org.hps.monitoring.application.util.ErrorHandler;
import org.hps.monitoring.application.util.EvioFileFilter;
import org.hps.monitoring.application.util.TableExporter;
import org.hps.monitoring.plotting.MonitoringAnalysisFactory;
import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusRegistry;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.enums.DataSourceType;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.DefaultLogFormatter;

/**
 * This is the primary class that implements the monitoring GUI application.
 * It should not be used directly.  Instead the {@link Main} class should be
 * used from the command line.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
final class MonitoringApplication implements ActionListener, PropertyChangeListener {

    // Statically initialize logging, which will be fully setup later.
    static final Logger logger;
    static {
        logger = Logger.getLogger(MonitoringApplication.class.getSimpleName());
    }
    static final Level DEFAULT_LEVEL = Level.ALL;

    // Default log stream.
    MonitoringApplicationStreamHandler streamHandler;
    LogHandler logHandler;
    PrintStream sysOut = System.out;
    PrintStream sysErr = System.err;
    
    // Application error handling.
    ErrorHandler errorHandler;
   
    // The main GUI components inside a JFrame.
    MonitoringApplicationFrame frame;    
    
    // The primary data models.
    final RunModel runModel = new RunModel();
    final ConfigurationModel configurationModel = new ConfigurationModel();
    final ConnectionStatusModel connectionModel = new ConnectionStatusModel();
    
    // The global configuration settings.
    Configuration configuration;
    
    // The default configuration resource embedded in the jar.
    static final String DEFAULT_CONFIGURATION = "/org/hps/monitoring/config/default_config.prop";

    // Encapsulation of ET connection and event processing.
    EventProcessing processing;
        
    // Filters for opening files.
    static final FileFilter lcioFilter = new FileNameExtensionFilter("LCIO files", "slcio");
    static final EvioFileFilter evioFilter = new EvioFileFilter();
    
    AIDAServer server = new AIDAServer("hps-monitoring-app");
    static final RmiStoreFactory rsf = new RmiStoreFactory();
            
    /**
     * Default log handler.
     */
    class LogHandler extends Handler {

        /**
         * This method inserts a record into the log table.
         */
        public void publish(LogRecord record) {
            getLogRecordModel().add(record);
        }

        public void close() throws SecurityException {
        }

        public void flush() {
        }
    }    
    
    LogRecordModel getLogRecordModel() {
        return frame.logPanel.logTable.model;
    }
    
    LogTable getLogTable() {
        return frame.logPanel.logTable;
    }
    
    class MonitoringApplicationStreamHandler extends StreamHandler {
        
        MonitoringApplicationStreamHandler(PrintStream ps) {
            super(ps, new DefaultLogFormatter());
        }
        
        public void publish(LogRecord record) {
            super.publish(record);
            flush();
        }
        
        public void setOutputStream(OutputStream out) {
            super.setOutputStream(out);
        }        
    }
                 
    /**
     * Instantiate and show the monitoring application with the given configuration.
     * @param configuration The Configuration object containing application settings.
     */
    MonitoringApplication(Configuration configuration) {
        
        try {
        
            // Setup the main GUI component.
            frame = new MonitoringApplicationFrame(this);
            
            // Add window listener to perform clean shutdown.
            frame.addWindowListener(new WindowListener() {

                @Override
                public void windowOpened(WindowEvent e) {
                }

                @Override
                public void windowClosing(WindowEvent e) {
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    exit();
                }

                @Override
                public void windowIconified(WindowEvent e) {
                }

                @Override
                public void windowDeiconified(WindowEvent e) {
                }

                @Override
                public void windowActivated(WindowEvent e) {
                }

                @Override
                public void windowDeactivated(WindowEvent e) {
                }
            });
        
            // Setup the error handler.
            errorHandler = new ErrorHandler(frame, logger);
                       
            // Add this class as a listener on the configuration model.
            configurationModel.addPropertyChangeListener(this);
        
            // Setup the logger.
            setupLogger();
               
            // Setup AIDA plotting and connect it to the GUI.
            setupAida();
        
            // Always load the default configuration first.
            loadConfiguration(new Configuration(DEFAULT_CONFIGURATION));
            
            // Overlay the user configuration if one was specified.
            if (configuration != null) {
                this.configuration = configuration;
                loadConfiguration(this.configuration);
            }
        
            // Enable the GUI now that initialization is complete.
            frame.setEnabled(true);
        
            logger.info("application initialized successfully");
        
        } catch (Exception e) {
            // Don't use the ErrorHandler here because we don't know that it initialized successfully.
            System.err.println("MonitoringApplication failed to initialize without errors!");
            DialogUtil.showErrorDialog(null, "Error Starting Monitoring Application", "Monitoring application failed to initialize.");
            e.printStackTrace();
            System.exit(1);
        }        
    }
    
    /**
     * Setup the logger.
     */
    void setupLogger() {
        logger.setUseParentHandlers(false);        
        logHandler = new LogHandler();
        logger.addHandler(logHandler);
        streamHandler = new MonitoringApplicationStreamHandler(System.out);
        logger.addHandler(streamHandler);
        for (Handler handler : logger.getHandlers()) {
            handler.setLevel(DEFAULT_LEVEL);
        }
        logger.setLevel(DEFAULT_LEVEL);
        logger.info("logging initialized");
    }
        
    /**
     * Static utility method for creating new instance.
     * @param configuration The application settings.
     * @return The new monitoring application instance.
     */
    static MonitoringApplication create(Configuration configuration) {
        return new MonitoringApplication(configuration);
    }    
        
    /**
     * Handle property changes.
     * @param evt The property change event.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConfigurationModel.LOG_LEVEL_PROPERTY)) {
            setLogLevel();
        }
    }
    
    /**
     * The primary action handler for the application.
     * @param e The ActionEvent to handle.
     */
    public void actionPerformed(ActionEvent e) {

        logger.finest("actionPerformed - " + e.getActionCommand());
        
        String command = e.getActionCommand();
        if (Commands.CONNECT.equals(command)) {
            startSession();
        } else if (Commands.DISCONNECT.equals(command)) {
            runDisconnectThread();
        } else if (Commands.SAVE_PLOTS.equals(command)) {
            savePlots();
        } else if (Commands.EXIT.equals(command)) {
            // This will trigger the window closing action that cleans everything up.
            frame.dispose();
        } else if (Commands.PAUSE.equals(command)) { 
            processing.pause();
        } else if (Commands.NEXT.equals(command)) {
            processing.next();
        } else if (Commands.RESUME.equals(command)) {
            processing.resume();
        } else if (Commands.SHOW_SETTINGS.equals(command)) {
            showSettingsDialog();
        } else if (Commands.LOAD_SETTINGS.equals(command)) {
            loadSettings();
        } else if (Commands.SAVE_SETTINGS.equals(command)) {
            saveSettings();
        }  else if (Commands.CLEAR_PLOTS.equals(command)) {
            clearPlots();
        } else if (Commands.LOAD_DEFAULT_SETTINGS.equals(command)) {
            loadDefaultSettings();
        } else if (Commands.OPEN_FILE.equals(command)) {
            openFile();
        } else if (Commands.DEFAULT_WINDOW.equals(command)) {
            restoreDefaultWindow();
        } else if (Commands.MAXIMIZE_WINDOW.equals(command)) {
            maximizeWindow();
        } else if (Commands.MINIMIZE_WINDOW.equals(command)) {
            minimizeWindow();
        } else if (Commands.CLOSE_FILE.equals(command)) {
            closeFile();
        } else if (Commands.SAVE_SCREENSHOT.equals(command)) {
            saveScreenshot();
        } else if (Commands.SAVE_LOG_TABLE.equals(command)) {
            saveLogTable();
        } else if (Commands.CLEAR_LOG_TABLE.equals(command)) {
            getLogRecordModel().clear();
        } else if (Commands.LOG_TO_FILE.equals(command)) {
            chooseLogFile();
        } else if (Commands.LOG_TO_TERMINAL.equals(command)) {
            logToTerminal();
        } else if (Commands.START_AIDA_SERVER.equals(command)) {
            startAIDAServer();
        } else if (Commands.STOP_AIDA_SERVER.equals(command)) {
            stopAIDAServer();
        }
    }    
    
    /**
     * Setup AIDA plotting into the GUI components.
     */
    void setupAida() {
        // Register the factory for display plots in tabs.
        MonitoringAnalysisFactory.register();
        
        // Set the root tab pane for displaying plots.
        MonitoringPlotFactory.setRootPane(frame.plotPanel.getPlotPane());
        
        // Setup the region listener to connect the plot info window.
        MonitoringPlotFactory.setPlotterRegionListener(new PlotterRegionListener() {
            @Override
            public void regionSelected(PlotterRegion region) {
                if (region != null) {
                    frame.plotInfoPanel.setCurrentRegion(region);
                }
            }
        });
        
        // Perform global configuration of the JFreeChart back end.
        AnalysisFactory.configure();
    }
                
    /**
     * This method sets the configuration on the model, which fires a change for every property.
     * @param configuration The new configuration.
     */
    void loadConfiguration(Configuration configuration) {
        
        // HACK: Clear data source combo box for new config.
        frame.dataSourceComboBox.removeAllItems();
        
        // Set the Configuration on the ConfigurationModel which will trigger all the PropertyChangelListeners.
        configurationModel.setConfiguration(configuration);
        if (configuration.getFile() != null)
            logger.config("loaded config from file " + configuration.getFile().getPath());
        else
            logger.config("loaded config from resource " + configuration.getResourcePath());
    }
              
    /**
     * Reset the plots and clear the tabs in the plot window.
     */
    void resetPlots() {
        
        // Clear global list of registered plotters.
        MonitoringPlotFactory.getPlotterRegistry().clear();  
        
        // Clear the static AIDA tree in case plots are hanging around from previous sessions.
        AIDA.defaultInstance().clearAll();

        // Reset plot panel which removes all its tabs.
        frame.plotPanel.reset();
        
        logger.info("plots were cleared");
    }                                    
                   
    /**
     * Configure the system status monitor panel for a new job.
     */
    void setupSystemStatusMonitor() {
        
        // Clear the system status monitor table.
        frame.systemStatusPanel.clear();

        // Get the global registry of SystemStatus objects.
        SystemStatusRegistry registry = SystemStatusRegistry.getSystemStatusRegistery();

        // Process the SystemStatus objects.
        for (SystemStatus systemStatus : registry.getSystemStatuses()) {
            // This will add the status to the two tables.
            frame.systemStatusPanel.addSystemStatus(systemStatus);
        }
        
        logger.info("system status monitor initialized successfully");
    }
    
    /**
     * Start a new monitoring session.
     */
    synchronized void startSession() {
        
        logger.info("starting new session");

        try {
                        
            // Reset the plot panel and global AIDA state.
            resetPlots();

            // The system status registry is cleared here before any event processors
            // which might create a SystemStatus are added to the event processing chain
            // e.g. an LCSim Driver, etc.
            SystemStatusRegistry.getSystemStatusRegistery().clear();

            // List of extra composite record processors including the updater for the RunPanel.
            List<CompositeRecordProcessor> processors = new ArrayList<CompositeRecordProcessor>();
            processors.add(frame.dashboardPanel.new EventDashboardUpdater());
            
            // Add Driver to update the trigger diagnostics tables.
            List<Driver> drivers = new ArrayList<Driver>();
            drivers.add(frame.triggerPanel.new TriggerDiagnosticGUIDriver());

            // Add listener to push conditions changes to conditions panel.
            List<ConditionsListener> conditionsListeners = new ArrayList<ConditionsListener>();
            conditionsListeners.add(frame.conditionsPanel.new ConditionsPanelListener());
            
            // Instantiate the event processing wrapper.
            processing = new EventProcessing(this, processors, drivers, conditionsListeners);
            
            // Connect to the ET system, if applicable.
            processing.connect();
            
            // Configure event processing from the global application settings, including setup of record loop.
            logger.info("setting up event processing on source " + configurationModel.getDataSourcePath() 
                    + " with type " + configurationModel.getDataSourceType());
            processing.setup(configurationModel);
                                  
            // Setup the system status monitor table.
            setupSystemStatusMonitor();
                                            
            // Start the event processing thread.            
            processing.start();            
            
            logger.info("new session successfully initialized");

        } catch (Exception e) {

            // Disconnect from the ET system.
            processing.disconnect();
            
            // Log the error that occurred and show a pop up dialog.
            errorHandler.setError(e).log().printStackTrace().showErrorDialog("There was an error while starting the session." 
                    + '\n' + "See the log for details.", "Session Error");
            
            logger.severe("failed to start new session");
        }
    }
           
    /**
     * Exit from the application from exit menu item or hitting close window button.
     */
    void exit() {        
        if (connectionModel.isConnected()) {
            processing.stop();
        }
        logHandler.setLevel(Level.OFF);
        logger.info("exiting the application");
        streamHandler.flush();
        System.exit(0);
    }
            
    /**
     * Save AIDA plots to a file using a file chooser.
     */
    void savePlots() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showSaveDialog(frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            File fileName = fc.getSelectedFile();
            try {
                AIDA.defaultInstance().saveAs(fileName);
                logger.info("saved plots to " + fileName);
                DialogUtil.showInfoDialog(frame, "Plots Saved",  "Plots were successfully saved to AIDA file.");
            } catch (IOException e) {
                errorHandler.setError(e).setMessage("Error Saving Plots").printStackTrace().log().showErrorDialog();
            }
        }
    }
    
    /**
     * Clear the current set of AIDA plots in the default data tree.
     */
    void clearPlots() {
        int confirmation = DialogUtil.showConfirmationDialog(frame, 
                "Are you sure you want to clear the plots", "Clear Plots Confirmation");
        if (confirmation == JOptionPane.YES_OPTION) {
            AIDA.defaultInstance().clearAll();
            DialogUtil.showInfoDialog(frame, "Plots Clear", "The AIDA plots were cleared.");
        }
        logger.info("plots were cleared");
    }
    
    /**
     * Load default application settings.
     */
    void loadDefaultSettings() {
        configuration = new Configuration(MonitoringApplication.DEFAULT_CONFIGURATION);
        configurationModel.setConfiguration(configuration);
        DialogUtil.showInfoDialog(frame, "Default Configuration Loaded", "The default configuration was loaded.");
        logger.config("default settings loaded");
    }
    
    /**
     * Show the settings dialog window.
     */
    void showSettingsDialog() {
        frame.settingsDialog.setVisible(true);        
    }
        
    /**
     * Open a file data source using a <code>JFileChooser</code>.
     */
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
            
            // Set data source type.
            FileFilter filter = fc.getFileFilter();
            DataSourceType type = null;
            if (filter == lcioFilter) {
                type = DataSourceType.LCIO_FILE;
            } else if (filter == evioFilter) {
                type = DataSourceType.EVIO_FILE;
            } else {
                // This should never happen.
                throw new RuntimeException();
            }
                        
            configurationModel.setDataSourcePath(filePath);
            configurationModel.setDataSourceType(type);
            
            logger.config("set new data source " + filePath + " with type " + type);
        }
    }    
    
    /**
     * Save current settings to a file using a file chooser.
     */
    void saveSettings() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Configuration");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            configuration.writeToFile(f);
            logger.info("saved configuration to file: " + f.getPath());
            DialogUtil.showInfoDialog(frame, "Settings Saved", "Settings were saved successfully.");
        }
    }
    
    /**
     * Load settings from a properties file using a file chooser.
     */
    void loadSettings() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Settings");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showDialog(frame, "Load ...");
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            configuration = new Configuration(f);
            loadConfiguration(configuration);
            logger.info("loaded configuration from file: " + f.getPath());
            DialogUtil.showInfoDialog(frame, "Settings Loaded", "Settings were loaded successfully.");
        }
    }
    
    /**
     * Maximize the application window.
     */
    void maximizeWindow() {
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }   
    
    /**
     * Minimize the application window.
     */
    void minimizeWindow() {
        frame.setExtendedState(JFrame.ICONIFIED);
    }    
    
    /**
     * Restore the default GUI layout.
     */
    void restoreDefaultWindow() {
        maximizeWindow();
        frame.restoreDefaults();
    }    
    
    /**
     * Remove the currently selected file from the data source list.
     */
    void closeFile() {
        if (!configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER)) {
            DataSourceItem item = (DataSourceItem) frame.dataSourceComboBox.getSelectedItem();
            if (item.name.equals(configurationModel.getDataSourcePath())) {
                frame.dataSourceComboBox.removeItem(frame.dataSourceComboBox.getSelectedItem());    
            }            
        }
    }
    
    /**
     * Save a screenshot to a file using a file chooser.
     */
    // FIXME: This might need to be on a new thread to allow the GUI to redraw w/o chooser visible.
    void saveScreenshot() {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Save Screenshot");
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("png file (*.png)", "png");
        String format = pngFilter.getExtensions()[0];
        fc.addChoosableFileFilter(pngFilter);
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(frame);
        if (r == JFileChooser.APPROVE_OPTION) {            
            String fileName = fc.getSelectedFile().getPath();
            if (!fileName.endsWith("." + format)) {
                fileName += "." + format;
            }
            frame.repaint();
            Object lock = new Object();
            synchronized (lock) {
                try {
                    lock.wait(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            writeScreenshot(fileName, format);
            DialogUtil.showInfoDialog(frame, "Screenshot Saved", "Screenshot was saved to file" + '\n' + fileName);
            logger.info("saved screenshot to " + fileName);
        }
    }

    /**
     * Save a screenshot to an output file.
     * @param fileName The name of the output file.
     */
    void writeScreenshot(String fileName, String format) {
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        frame.paint(image.getGraphics()); 
        try {
            ImageIO.write(image, format, new File(fileName));
        } catch (IOException e) {
            errorHandler.setError(e).setMessage("Failed to save screenshot.").printStackTrace().log().showErrorDialog();
        }        
    }            
    
    /**
     * Set the log level from the configuration model.
     */
    void setLogLevel() {
        Level newLevel = configurationModel.getLogLevel();
        if (logger.getLevel() != newLevel) {
            logger.setLevel(newLevel);
            logger.log(Level.INFO, "Log Level was changed to <" + configurationModel.getLogLevel().toString() + ">");
        }
    }      
    
    /**
     * Export a JTable's data to a comma-delimited text file using a file chooser.
     */
    void saveTable(JTable table) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Table to Text File");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(frame);
        if (r == JFileChooser.APPROVE_OPTION) {            
            String fileName = fc.getSelectedFile().getPath();
            try {
                TableExporter.export(table, fileName, ',');
                logger.info("saved table data to " + fileName);
                DialogUtil.showInfoDialog(frame, "Table Data Saved", "The table was exported successfully.");
            } catch (IOException e) {
                DialogUtil.showErrorDialog(frame, "Table Export Error", "The table export failed.");
                logger.warning("failed to save table data to " + fileName);
            }                        
        }
    }
    
    /**
     * Save the log table to a file using a file chooser.
     */
    void saveLogTable() {
        saveTable(frame.logPanel.logTable);
    }
        
    /**
     * Redirect <code>System.out</code> and <code>System.err</code> to file chosen
     * by a file chooser.
     */
    void chooseLogFile() {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Save Log Messages to File");       
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(frame);
        if (r == JFileChooser.APPROVE_OPTION) {            
            String fileName = fc.getSelectedFile().getPath();
            if (new File(fileName).exists()) {
                DialogUtil.showErrorDialog(frame, "File Exists", "File already exists.");
            } else {
                logToFile(new File(fileName));
            }
        }        
    }
    
    /**
     * Redirect <code>System.out</code> and <code>System.err</code> to a file.
     * @param file The output log file.
     * @throws FileNotFoundException if the file does not exist.
     */
    void logToFile(File file) {
        try {
            
            // Create the output file stream.
            PrintStream fileStream = new PrintStream(new FileOutputStream(file.getPath()));
            System.setOut(fileStream);
            System.setErr(fileStream);
            
            // Flush the current handler, but do NOT close here or System.out gets clobbered!
            streamHandler.flush();
            
            // Replace the current handler with one using the file stream.
            logger.removeHandler(streamHandler);
            streamHandler = new MonitoringApplicationStreamHandler(fileStream);
            streamHandler.setLevel(logger.getLevel());
            logger.addHandler(streamHandler);
            
            // Set the properties on the model.
            configurationModel.setLogFileName(file.getPath());
            configurationModel.setLogToFile(true);
            
            logger.info("Saving log messages to " + configurationModel.getLogFileName());
            DialogUtil.showInfoDialog(frame, "Logging to File", 
                    "Log messages redirected to file" + '\n' + configurationModel.getLogFileName());
            
        } catch (FileNotFoundException e) {
            errorHandler.setError(e).log().showErrorDialog();
        }
    }      
    
    /**
     * Send <code>System.out</code> and <code>System.err</code> back to the terminal, 
     * e.g. if they were previously sent to a file.
     */
    void logToTerminal() {
        
        // Reset System.out and err back to original streams.
        System.setOut(sysOut);
        System.setErr(sysErr);
        
        // Flush and close the current handler, which is using a file stream.
        streamHandler.flush();
        streamHandler.close();
        
        // Replace the handler with the one printing to the terminal.
        logger.removeHandler(streamHandler);               
        streamHandler = new MonitoringApplicationStreamHandler(System.out);
        streamHandler.setLevel(logger.getLevel());
        logger.addHandler(streamHandler);
        
        logger.log(Level.INFO, "log messages redirected to terminal");
        
        // Update the model to indicate logging to file has been disabled.
        configurationModel.setLogToFile(false);
        
        DialogUtil.showInfoDialog(frame, "Log to Terminal", "Log messages will be sent to the terminal.");
    }    
    
    /**
     * Start the AIDA server instance.
     */
    void startAIDAServer() {
        if (configurationModel.hasValidProperty(ConfigurationModel.AIDA_SERVER_NAME_PROPERTY)) {
            server.setName(configurationModel.getAIDAServerName());
        }
        boolean started = server.start();
        if (started) {
            frame.menu.startAIDAServer();
            logger.info("AIDA server started at " + server.getName());
            DialogUtil.showInfoDialog(frame, "AIDA Server Started", "The remote AIDA server started successfully.");
        } else {
            logger.warning("AIDA server failed to start");
            DialogUtil.showErrorDialog(frame, "Failed to Start AIDA Server", "The remote AIDA server failed to start.");
        }
    }
    
    /**
     * Stop the AIDA server instance.
     */
    void stopAIDAServer() {
        server.disconnect();
        frame.menu.stopAIDAServer();
        logger.info("AIDA server was stopped");
        DialogUtil.showInfoDialog(frame, "AIDA Server Stopped", "The AIDA server was stopped.");
    }    
    
    /**
     * Run the disconnection on a separate thread.
     */
    void runDisconnectThread() {
        new Thread() {
            public void run() {
                logger.fine("disconnect thread is running ...");
                connectionModel.setConnectionStatus(ConnectionStatus.DISCONNECTING);
                MonitoringApplication.this.processing.stop();
                logger.fine("disconnect thread finished!");
            }
        }.run();
    }
}