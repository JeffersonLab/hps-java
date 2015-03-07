package org.hps.monitoring.application;

import hep.aida.jfree.AnalysisFactory;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.jfree.plotter.PlotterRegionListener;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
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
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.monitoring.application.model.RunModel;
import org.hps.monitoring.application.util.DialogUtil;
import org.hps.monitoring.application.util.ErrorHandler;
import org.hps.monitoring.application.util.EvioFileFilter;
import org.hps.monitoring.application.util.TableExporter;
import org.hps.monitoring.plotting.MonitoringAnalysisFactory;
import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusListener;
import org.hps.monitoring.subsys.SystemStatusRegistry;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.enums.DataSourceType;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.DefaultLogFormatter;

/**
 * This is the primary class that implements the monitoring GUI application.
 * It should not be used directly.  Instead the {@link Main} class should be
 * used from the command line or via the supplied script built automatically 
 * by Maven.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
final class MonitoringApplication implements ActionListener, PropertyChangeListener, SystemStatusListener {

    // Statically initialize logging, which will be fully setup later.
    static final Logger logger;
    static {
        logger = Logger.getLogger(MonitoringApplication.class.getSimpleName());
    }
    static final Level DEFAULT_LEVEL = Level.ALL;

    // Default log stream.
    PrintStream logStream = System.out;
    
    // Application error handling.
    final ErrorHandler errorHandler;
   
    // The main GUI components inside a JFrame.
    final MonitoringApplicationFrame frame;    
    
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
             
    /**
     * Instantiate and show the monitoring application with the given configuration.
     * @param configuration The Configuration object containing application settings.
     */
    MonitoringApplication(Configuration configuration) {
                
        // Setup the main GUI component.
        frame = new MonitoringApplicationFrame(this);
        
        // Setup the error handler.
        errorHandler = new ErrorHandler(frame, logger);
                       
        // Add this class as a listener on the configuration model.
        configurationModel.addPropertyChangeListener(this);
        
        // Setup the logger.
        setupLogger();
               
        // Setup AIDA plotting and connect it to the GUI.
        setupAida();
        
        // Set the configuration.
        if (configuration != null) {
            // There was a user specified configuration.
            this.configuration = configuration;
        } else {
            // Use the default configuration.
            this.configuration = new Configuration(DEFAULT_CONFIGURATION);
        }
                                      
        // Load the configuration.
        loadConfiguration(this.configuration);
        
        // Setup the data source combo box.
        frame.dataSourceComboBox.initialize();
        
        logger.info("application initialized successfully");
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
        // TODO: Handle log level configuration change here.
    }
    
    /**
     * The primary action handler for the application.
     * @param e The ActionEvent to handle.
     */
    public void actionPerformed(ActionEvent e) {

        String cmd = e.getActionCommand();
        if (Commands.CONNECT.equals(cmd)) {
            // Run the start session method on a separate thread.
            new Thread() {
                public void run() {
                    startSession();
                }
            }.start();
        } else if (Commands.DISCONNECT.equals(cmd)) {
            // Run the stop session method on a separate thread.
            new Thread() {
                public void run() {
                    stopSession();
                }
            }.start();
        } else if (Commands.SAVE_PLOTS.equals(cmd)) {
            savePlots();
        } else if (Commands.EXIT.equals(cmd)) {
            exit();
        } else if (Commands.PAUSE.equals(cmd)) { 
            processing.pause();
        } else if (Commands.NEXT.equals(cmd)) {
            processing.next();
        } else if (Commands.RESUME.equals(cmd)) {
            processing.resume();
        } else if (Commands.SHOW_SETTINGS.equals(cmd)) {
            showSettingsDialog();
        } else if (Commands.LOAD_SETTINGS.equals(cmd)) {
            loadSettings();
        } else if (Commands.SAVE_SETTINGS.equals(cmd)) {
            saveSettings();
        }  else if (Commands.CLEAR_PLOTS.equals(cmd)) {
            clearPlots();
        } else if (Commands.LOAD_DEFAULT_SETTINGS.equals(cmd)) {
            loadDefaultSettings();
        } else if (Commands.OPEN_FILE.equals(cmd)) {
            openFile();
        } else if (Commands.DEFAULT_WINDOW.equals(cmd)) {
            restoreDefaultWindow();
        } else if (Commands.MAXIMIZE_WINDOW.equals(cmd)) {
            maximizeWindow();
        } else if (Commands.MINIMIZE_WINDOW.equals(cmd)) {
            minimizeWindow();
        } else if (Commands.CLOSE_FILE.equals(cmd)) {
            closeFile();
        } else if (Commands.SAVE_SCREENSHOT.equals(cmd)) {
            saveScreenshot();
        } else if (Commands.LOG_LEVEL_CHANGED.equals(cmd)) {
            setLogLevel();
        } else if (Commands.SAVE_LOG_TABLE.equals(cmd)) {
            saveLogTable();
        } else if (Commands.CLEAR_LOG_TABLE.equals(cmd)) {
            getLogRecordModel().clear();
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
     * Setup the logger.
     */
    void setupLogger() {
        logger.setUseParentHandlers(false);
        logger.addHandler(new LogHandler());
        logger.addHandler(new StreamHandler(logStream, new DefaultLogFormatter()) {
            public void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        });
        for (Handler handler : logger.getHandlers()) {
            handler.setLevel(DEFAULT_LEVEL);
        }
        logger.setLevel(DEFAULT_LEVEL);
        logger.info("logging initialized");
    }
            
    /**
     * This method sets the configuration on the model, which fires a change for every property.
     * @param configuration The new configuration.
     */
    void loadConfiguration(Configuration configuration) {
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

        // Clear the static AIDA tree in case plots are hanging around from previous sessions.
        AIDA.defaultInstance().clearAll();

        // Reset plot panel which removes all tabs.
        frame.plotPanel.reset();
        
        logger.info("plots were cleared");
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
        
        logger.info("system status monitor initialized successfully");
    }
    
    /**
     * Hook for logging all status changes from the system status monitor.
     */
    @Override
    public void statusChanged(SystemStatus status) {

        // Choose the appropriate log level.
        Level level = Level.FINE;
        if (status.getStatusCode().equals(Level.WARNING)) {
            level = Level.WARNING;
        } else if (status.getStatusCode().ordinal() >= StatusCode.ERROR.ordinal()) {
            level = Level.SEVERE;
        }
        
        // Log all status changes.
        logger.log(level, "STATUS, " + "subsys: " + status.getSubsystem() + ", " 
                + "code: " + status.getStatusCode().name() 
                + ", " + "descr: " + status.getDescription() 
                + ", " + "mesg: " + status.getMessage());
    }
    
    /**
     * <p>
     * Start a new monitoring session.
     * <p> 
     * This method is executed in a separate thread from the EDT within {@link #actionPerformed(ActionEvent)} 
     * so that GUI updates are not blocked while the session is being setup.
     */
    void startSession() {
        
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
            processors.add(frame.runPanel.new RunPanelUpdater());
            
            // Initialize event processing with the list of processors and reference to the application.
            processing = new EventProcessing(this, processors);
            
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
     * Stop the session by disconnecting from the ET system and stopping the event processing.
     */
    void stopSession() {
        
        logger.info("stopping the session");
        
        // Disconnect from ET system, if using the ET server, and set the proper disconnected GUI state.
        processing.disconnect();

        // Stop the event processing, which is called after the ET system goes down to avoid hanging in calls to ET system.
        processing.stop(); 
        
        logger.info("session was stopped");
    }
    
    /**
     * Exit from the application.
     */
    void exit() {        
        // Cleanup ET system if necessary.
        if (processing != null && processing.isActive()) {
            logger.info("killing active ET connection");
            processing.closeEtConnection();
        }
        frame.setVisible(false);
        logger.info("exiting the application");
        logger.getHandlers()[0].flush();
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
            writeScreenshot(fileName, format);
            DialogUtil.showInfoDialog(frame, "Screenshot Saved", "Screenshot was saved to file.");
            logger.info("saved screenshot to " + fileName);
        }
    }

    /**
     * Save a screenshot to an output file.
     * @param fileName The name of the output file.
     */
    void writeScreenshot(String fileName, String format) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle screenRectangle = new Rectangle(screenSize);
        try {
            Robot robot = new Robot();
            BufferedImage image = robot.createScreenCapture(screenRectangle);
            ImageIO.write(image, format, new File(fileName));
        } catch (Exception e) {
            errorHandler.setError(e).setMessage("Failed to take screenshot.").printStackTrace().log().showErrorDialog();
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
}