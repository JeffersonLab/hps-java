package org.hps.monitoring.application;

import hep.aida.jfree.AnalysisFactory;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.jfree.plotter.PlotterRegionListener;

import java.awt.Frame;
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
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
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
import org.hps.monitoring.plotting.ExportPdf;
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
 * This is the primary class that implements the data monitoring GUI application.
 * <p>
 * It should not be used directly. Instead the {@link Main} class should be used from the command line.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class MonitoringApplication implements ActionListener, PropertyChangeListener {

    /**
     * The default log handler which puts records into the table GUI component.
     */
    class LogHandler extends Handler {

        /**
         * Close the handler.
         *
         * @throws SecurityException never
         */
        @Override
        public void close() throws SecurityException {
            // Does nothing.
        }

        /**
         * Flush the handler.
         */
        @Override
        public void flush() {
            // Does nothing.
        }

        /**
         * This method inserts a record into the log table.
         */
        @Override
        public void publish(final LogRecord record) {
            // Add the record to the table's model.
            getLogRecordModel().add(record);
        }
    }

    /**
     * Log handler which publishes messages to a stream (console or file in this case).
     */
    class MonitoringApplicationStreamHandler extends StreamHandler {

        /**
         * Class constructor.
         *
         * @param ps the output stream
         */
        MonitoringApplicationStreamHandler(final PrintStream ps) {
            super(ps, new DefaultLogFormatter());
        }

        /**
         * Publish a record which will automatically flush the handler.
         *
         * @param record the <code>LogRecord</code> to publish
         */
        @Override
        public void publish(final LogRecord record) {
            super.publish(record);

            // FIXME: Is this efficient? Should this always happen here?
            flush();
        }

        /**
         * Set the output stream.
         *
         * @param out the output stream
         */
        @Override
        public void setOutputStream(final OutputStream out) {
            super.setOutputStream(out);
        }
    }

    /**
     * The default configuration resource from the jar.
     */
    private static final String DEFAULT_CONFIGURATION = "/org/hps/monitoring/config/default_config.prop";

    /**
     * The default log level (shows all messages).
     */
    private static final Level DEFAULT_LEVEL = Level.ALL;

    /**
     * A filter for selecting EVIO files.
     */
    private static final EvioFileFilter EVIO_FILTER = new EvioFileFilter();

    /**
     * A filter for selecting LCIO files.
     */
    private static final FileFilter LCIO_FILTER = new FileNameExtensionFilter("LCIO files", "slcio");

    /**
     * Global logging object.
     */
    private static final Logger LOGGER;

    /**
     * Saved reference to <code>System.err</code> for convenience.
     */
    private static final PrintStream SYS_ERR = System.err;

    /**
     * Saved reference to <code>System.out</code> for convenience.
     */
    private static final PrintStream SYS_OUT = System.out;

    /**
     * Initialize logging which will be fully configured later.
     */
    static {
        LOGGER = Logger.getLogger(MonitoringApplication.class.getSimpleName());
    }

    /**
     * Static utility method for creating new instance.
     *
     * @param configuration the application settings
     * @return the new monitoring application instance
     */
    static MonitoringApplication create(final Configuration configuration) {
        return new MonitoringApplication(configuration);
    }

    /**
     * The global configuration model.
     */
    private final ConfigurationModel configurationModel = new ConfigurationModel();

    /**
     * The global connection status model.
     */
    private final ConnectionStatusModel connectionModel = new ConnectionStatusModel();

    /**
     * The error handling object.
     */
    private ErrorHandler errorHandler;

    /**
     * The primary GUI component which is a <code>JFrame</code>.
     */
    private MonitoringApplicationFrame frame;

    /**
     * The current log handler.
     */
    private LogHandler logHandler;

    /**
     * Event processing wrapper.
     */
    private EventProcessing processing;

    /**
     * The model which has information about the current run and events being processed.
     */
    private final RunModel runModel = new RunModel();

    /**
     * A remote AIDA server instance.
     */
    private final AIDAServer server = new AIDAServer("hps-monitoring-app");

    /**
     * The handler for putting messages into the log table.
     */
    private MonitoringApplicationStreamHandler streamHandler;

    /**
     * Instantiate and show the monitoring application with the given configuration.
     *
     * @param userConfiguration the Configuration object containing application settings
     */
    MonitoringApplication(final Configuration userConfiguration) {

        try {

            // Setup the main GUI component.
            this.frame = new MonitoringApplicationFrame(this);

            // Add window listener to perform clean shutdown.
            this.frame.addWindowListener(new WindowListener() {

                /**
                 * Not used.
                 *
                 * @param e
                 */
                @Override
                public void windowActivated(final WindowEvent e) {
                }

                /**
                 * Activate cleanup when window closes.
                 *
                 * @param e the window event
                 */
                @Override
                public void windowClosed(final WindowEvent e) {
                    exit();
                }

                /**
                 * Not used.
                 *
                 * @param e
                 */
                @Override
                public void windowClosing(final WindowEvent e) {
                }

                /**
                 * Not used.
                 *
                 * @param e
                 */
                @Override
                public void windowDeactivated(final WindowEvent e) {
                }

                /**
                 * Not used.
                 *
                 * @param e
                 */
                @Override
                public void windowDeiconified(final WindowEvent e) {
                }

                /**
                 * Not used.
                 *
                 * @param e
                 */
                @Override
                public void windowIconified(final WindowEvent e) {
                }

                /**
                 * Not used.
                 *
                 * @param e
                 */
                @Override
                public void windowOpened(final WindowEvent e) {
                }
            });

            // Setup the error handler.
            this.errorHandler = new ErrorHandler(this.frame, LOGGER);

            // Add this class as a listener on the configuration model.
            this.configurationModel.addPropertyChangeListener(this);

            // Setup the logger.
            setupLogger();

            // Setup AIDA plotting and connect it to the GUI.
            setupAida();

            // Load the default configuration.
            loadConfiguration(new Configuration(DEFAULT_CONFIGURATION), false);

            if (userConfiguration != null) {
                // Load user configuration.
                loadConfiguration(userConfiguration, true);
            }

            // Enable the GUI now that initialization is complete.
            this.frame.setEnabled(true);

            LOGGER.info("application initialized successfully");

        } catch (final Exception e) {
            // Don't use the ErrorHandler here because we don't know that it initialized successfully.
            System.err.println("MonitoringApplication failed to initialize without errors!");
            DialogUtil.showErrorDialog(null, "Error Starting Monitoring Application",
                    "Monitoring application failed to initialize.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * The primary action handler for the application.
     *
     * @param e the {@link java.awt.ActionEvent} to handle
     */
    @Override
    public void actionPerformed(final ActionEvent e) {

        // logger.finest("actionPerformed - " + e.getActionCommand());

        final String command = e.getActionCommand();
        if (Commands.CONNECT.equals(command)) {
            startSession();
        } else if (Commands.DISCONNECT.equals(command)) {
            runDisconnectThread();
        } else if (Commands.SAVE_PLOTS.equals(command)) {
            savePlots();
        } else if (Commands.EXIT.equals(command)) {
            // This will trigger the window closing action that cleans everything up.
            this.frame.dispose();
        } else if (Commands.PAUSE.equals(command)) {
            this.processing.pause();
        } else if (Commands.NEXT.equals(command)) {
            this.processing.next();
        } else if (Commands.RESUME.equals(command)) {
            this.processing.resume();
        } else if (Commands.SHOW_SETTINGS.equals(command)) {
            showSettingsDialog();
        } else if (Commands.LOAD_SETTINGS.equals(command)) {
            loadSettings();
        } else if (Commands.SAVE_SETTINGS.equals(command)) {
            saveSettings();
        } else if (Commands.CLEAR_PLOTS.equals(command)) {
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
     * Redirect <code>System.out</code> and <code>System.err</code> to file chosen by a file chooser.
     */
    private void chooseLogFile() {
        final JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Save Log Messages to File");
        fc.setCurrentDirectory(new File("."));
        final int r = fc.showSaveDialog(this.frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            final String fileName = fc.getSelectedFile().getPath();
            if (new File(fileName).exists()) {
                DialogUtil.showErrorDialog(this.frame, "File Exists", "File already exists.");
            } else {
                logToFile(new File(fileName));
            }
        }
    }

    /**
     * Clear the current set of AIDA plots in the default data tree.
     */
    private void clearPlots() {
        final int confirmation = DialogUtil.showConfirmationDialog(this.frame,
                "Are you sure you want to clear the plots", "Clear Plots Confirmation");
        if (confirmation == JOptionPane.YES_OPTION) {
            AIDA.defaultInstance().clearAll();
            DialogUtil.showInfoDialog(this.frame, "Plots Clear", "The AIDA plots were cleared.");
        }
        LOGGER.info("plots were cleared");
    }

    /**
     * Remove the currently selected file from the data source list.
     */
    private void closeFile() {
        if (!this.configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER)) {
            final DataSourceItem item = (DataSourceItem) this.frame.getToolbarPanel().getDataSourceComboBox()
                    .getSelectedItem();
            if (item.getPath().equals(this.configurationModel.getDataSourcePath())) {
                this.frame.getToolbarPanel().getDataSourceComboBox()
                        .removeItem(this.frame.getToolbarPanel().getDataSourceComboBox().getSelectedItem());
            }
        }
    }

    /**
     * Exit from the application from exit menu item or hitting close window button.
     */
    private void exit() {
        if (this.connectionModel.isConnected()) {
            this.processing.stop();
        }
        this.logHandler.setLevel(Level.OFF);
        LOGGER.info("exiting the application");
        this.streamHandler.flush();
        System.exit(0);
    }

    /**
     * Get the current configuration model.
     *
     * @return the current configuration model
     */
    ConfigurationModel getConfigurationModel() {
        return this.configurationModel;
    }

    /**
     * Get the current connection status model.
     *
     * @return the current connections status model
     */
    ConnectionStatusModel getConnectionModel() {
        return this.connectionModel;
    }

    /**
     * Get the application's error handling object.
     *
     * @return the error handling object
     */
    ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    /**
     * Get the logger.
     *
     * @return the logger
     */
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * Get the table model for log records.
     *
     * @return the table model for log records
     */
    LogRecordModel getLogRecordModel() {
        return this.frame.getLogPanel().getLogTable().getLogRecordModel();
    }

    /**
     * Get the log table.
     *
     * @return the log table
     */
    LogTable getLogTable() {
        return this.frame.getLogPanel().getLogTable();
    }

    /**
     * Get a list of relevant run data from the model for writing to a PDF.
     *
     * @return the list of run data from the model
     */
    private List<String> getRunData() {
        final List<String> data = new ArrayList<String>();
        data.add("Created: " + new Date());
        data.add("Run Number: " + this.runModel.getRunNumber());
        data.add("Started: " + this.runModel.getStartDate());
        data.add("Ended: " + this.runModel.getEndDate());
        data.add("Length: " + this.runModel.getRunLength() + " seconds");
        data.add("Total Events: " + this.runModel.getTotalEvents());
        data.add("Elapsed Time: " + this.runModel.getElapsedTime());
        data.add("Events Processed: " + this.runModel.getEventsReceived());
        return data;
    }

    /**
     * Get the run model with information about the run and event(s) currently being processed.
     *
     * @return the run model
     */
    RunModel getRunModel() {
        return this.runModel;
    }

    /**
     * This method sets the configuration on the model, which fires a change for every property.
     *
     * @param configuration The new configuration.
     * @param merge True to merge the configuration into the current one rather than replace it.
     */
    private void loadConfiguration(final Configuration configuration, final boolean merge) {

        if (merge) {
            // This will merge in additional properties so that default or current settings are preserved.
            this.configurationModel.merge(configuration);
        } else {
            // HACK: Clear data source combo box for clean configuration.
            this.frame.getToolbarPanel().getDataSourceComboBox().removeAllItems();

            // This will reset all configuration properties.
            this.configurationModel.setConfiguration(configuration);
        }

        if (configuration.getFile() != null) {
            LOGGER.config("loaded config from file " + configuration.getFile().getPath());
        } else {
            LOGGER.config("loaded config from resource " + configuration.getResourcePath());
        }
    }

    /**
     * Load default application settings.
     */
    private void loadDefaultSettings() {
        loadConfiguration(new Configuration(MonitoringApplication.DEFAULT_CONFIGURATION), false);
        DialogUtil.showInfoDialog(this.frame, "Default Configuration Loaded", "The default configuration was loaded.");
        LOGGER.config("default settings loaded");
    }

    /**
     * Load settings from a properties file using a file chooser.
     */
    private void loadSettings() {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Settings");
        fc.setCurrentDirectory(new File("."));
        final int r = fc.showDialog(this.frame, "Load ...");
        if (r == JFileChooser.APPROVE_OPTION) {
            final File f = fc.getSelectedFile();
            loadConfiguration(new Configuration(f), true);
            LOGGER.info("loaded configuration from file: " + f.getPath());
            DialogUtil.showInfoDialog(this.frame, "Settings Loaded", "Settings were loaded successfully.");
        }
    }

    /**
     * Redirect <code>System.out</code> and <code>System.err</code> to a file.
     *
     * @param file The output log file.
     * @throws FileNotFoundException if the file does not exist.
     */
    private void logToFile(final File file) {
        try {

            // Create the output file stream.
            final PrintStream fileStream = new PrintStream(new FileOutputStream(file.getPath()));
            System.setOut(fileStream);
            System.setErr(fileStream);

            // Flush the current handler, but do NOT close here or System.out gets clobbered!
            this.streamHandler.flush();

            // Replace the current handler with one using the file stream.
            LOGGER.removeHandler(this.streamHandler);
            this.streamHandler = new MonitoringApplicationStreamHandler(fileStream);
            this.streamHandler.setLevel(LOGGER.getLevel());
            LOGGER.addHandler(this.streamHandler);

            // Set the properties on the model.
            this.configurationModel.setLogFileName(file.getPath());
            this.configurationModel.setLogToFile(true);

            LOGGER.info("Saving log messages to " + this.configurationModel.getLogFileName());
            DialogUtil.showInfoDialog(this.frame, "Logging to File", "Log messages redirected to file" + '\n'
                    + this.configurationModel.getLogFileName());

        } catch (final FileNotFoundException e) {
            this.errorHandler.setError(e).log().showErrorDialog();
        }
    }

    /**
     * Send <code>System.out</code> and <code>System.err</code> back to the terminal, e.g. if they were previously sent
     * to a file.
     */
    private void logToTerminal() {

        // Reset System.out and err back to original streams.
        System.setOut(MonitoringApplication.SYS_OUT);
        System.setErr(MonitoringApplication.SYS_ERR);

        // Flush and close the current handler, which is using a file stream.
        this.streamHandler.flush();
        this.streamHandler.close();

        // Replace the handler with the one printing to the terminal.
        LOGGER.removeHandler(this.streamHandler);
        this.streamHandler = new MonitoringApplicationStreamHandler(System.out);
        this.streamHandler.setLevel(LOGGER.getLevel());
        LOGGER.addHandler(this.streamHandler);

        LOGGER.log(Level.INFO, "log messages redirected to terminal");

        // Update the model to indicate logging to file has been disabled.
        this.configurationModel.setLogToFile(false);

        DialogUtil.showInfoDialog(this.frame, "Log to Terminal", "Log messages will be sent to the terminal.");
    }

    /**
     * Maximize the application window.
     */
    private void maximizeWindow() {
        this.frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    /**
     * Minimize the application window.
     */
    private void minimizeWindow() {
        this.frame.setExtendedState(Frame.ICONIFIED);
    }

    /**
     * Open a file data source using a <code>JFileChooser</code>.
     */
    private void openFile() {
        final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(LCIO_FILTER);
        fc.addChoosableFileFilter(EVIO_FILTER);
        fc.setDialogTitle("Select Data File");
        final int r = fc.showDialog(this.frame, "Select ...");
        if (r == JFileChooser.APPROVE_OPTION) {

            // Set data source path.
            final String filePath = fc.getSelectedFile().getPath();

            // Set data source type.
            final FileFilter filter = fc.getFileFilter();
            DataSourceType type = null;
            if (filter == LCIO_FILTER) {
                type = DataSourceType.LCIO_FILE;
            } else if (filter == EVIO_FILTER) {
                type = DataSourceType.EVIO_FILE;
            } else {
                // This should never happen.
                throw new RuntimeException();
            }

            this.configurationModel.setDataSourcePath(filePath);
            this.configurationModel.setDataSourceType(type);

            this.configurationModel.addRecentFile(filePath);

            LOGGER.config("set new data source " + filePath + " with type " + type);
        }
    }

    /**
     * Handle property changes.
     *
     * @param evt The property change event.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConfigurationModel.LOG_LEVEL_PROPERTY)) {
            setLogLevel();
        }
    }

    /**
     * Reset the plots and clear the tabs in the plot window.
     */
    private void resetPlots() {

        // Clear global list of registered plotters.
        MonitoringPlotFactory.getPlotterRegistry().clear();

        // Clear the static AIDA tree in case plots are hanging around from previous sessions.
        AIDA.defaultInstance().clearAll();

        // Reset plot panel which removes all its tabs.
        this.frame.getPlotPanel().reset();

        LOGGER.info("plots were cleared");
    }

    /**
     * Restore the default GUI layout.
     */
    private void restoreDefaultWindow() {
        maximizeWindow();
        this.frame.restoreDefaults();
    }

    /**
     * Run the disconnection on a separate thread.
     */
    private void runDisconnectThread() {
        new Thread() {
            @Override
            public void run() {
                LOGGER.fine("disconnect thread is running ...");
                MonitoringApplication.this.connectionModel.setConnectionStatus(ConnectionStatus.DISCONNECTING);
                MonitoringApplication.this.processing.stop();
                LOGGER.fine("disconnect thread finished!");
            }
        }.run();
    }

    /**
     * Save the log table to a file using a file chooser.
     */
    private void saveLogTable() {
        saveTable(this.frame.getLogPanel().getLogTable());
    }

    /**
     * Save plots to an AIDA, ROOT or PDF file using a file chooser.
     */
    private void savePlots() {
        final JFileChooser fc = new JFileChooser();
        fc.addChoosableFileFilter(new FileNameExtensionFilter("ROOT file", "root"));
        final FileFilter filter = new FileNameExtensionFilter("AIDA file", "aida");
        fc.addChoosableFileFilter(filter);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("PDF file", "pdf"));
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(filter);
        final int r = fc.showSaveDialog(this.frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fc.getSelectedFile();
            if (!selectedFile.exists()) {
                String fileName = fc.getSelectedFile().getAbsolutePath();
                final String extension = ((FileNameExtensionFilter) fc.getFileFilter()).getExtensions()[0];
                if (!fileName.endsWith(".aida") && !fileName.endsWith(".root") && !fileName.endsWith(".pdf")) {
                    fileName += "." + extension;
                }
                try {
                    if ("pdf".equals(extension)) {
                        // Write to a single PDF file.
                        ExportPdf.write(MonitoringPlotFactory.getPlotterRegistry().getPlotters(), fileName,
                                getRunData());
                    } else {
                        // Save plot object data to AIDA or ROOT file.
                        AIDA.defaultInstance().saveAs(fileName);
                    }
                    LOGGER.info("saved plots to " + fileName);
                    DialogUtil.showInfoDialog(this.frame, "Plots Saved", "Plots were successfully saved to " + '\n'
                            + fileName);
                } catch (final IOException e) {
                    this.errorHandler.setError(e).setMessage("Error Saving Plots").printStackTrace().log()
                            .showErrorDialog();
                }
            } else {
                DialogUtil.showErrorDialog(this.frame, "File Exists", "Selected file already exists.");
            }
        }
    }

    /**
     * Save a screenshot to a file using a file chooser.
     */
    private void saveScreenshot() {
        final JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Save Screenshot");
        final FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("png file (*.png)", "png");
        final String format = pngFilter.getExtensions()[0];
        fc.addChoosableFileFilter(pngFilter);
        fc.setCurrentDirectory(new File("."));
        final int r = fc.showSaveDialog(this.frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            String fileName = fc.getSelectedFile().getPath();
            if (!fileName.endsWith("." + format)) {
                fileName += "." + format;
            }
            /*
             * final Object lock = new Object(); synchronized (lock) { try { lock.wait(500); } catch (final
             * InterruptedException e) { e.printStackTrace(); } }
             */
            writeScreenshot(fileName, format);
            DialogUtil.showInfoDialog(this.frame, "Screenshot Saved", "Screenshot was saved to file" + '\n' + fileName);
            LOGGER.info("saved screenshot to " + fileName);
        }
    }

    /**
     * Save current settings to a file using a file chooser.
     */
    private void saveSettings() {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Configuration");
        fc.setCurrentDirectory(new File("."));
        final int r = fc.showSaveDialog(this.frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            final File f = fc.getSelectedFile();
            this.configurationModel.getConfiguration().writeToFile(f);
            LOGGER.info("saved configuration to file: " + f.getPath());
            DialogUtil.showInfoDialog(this.frame, "Settings Saved", "Settings were saved successfully.");
        }
    }

    /**
     * Export a JTable's data to a comma-delimited text file using a file chooser.
     *
     * @param table the table to export
     */
    private void saveTable(final JTable table) {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Table to Text File");
        fc.setCurrentDirectory(new File("."));
        final int r = fc.showSaveDialog(this.frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            final String fileName = fc.getSelectedFile().getPath();
            try {
                TableExporter.export(table, fileName, ',');
                LOGGER.info("saved table data to " + fileName);
                DialogUtil.showInfoDialog(this.frame, "Table Data Saved", "The table was exported successfully.");
            } catch (final IOException e) {
                DialogUtil.showErrorDialog(this.frame, "Table Export Error", "The table export failed.");
                LOGGER.warning("failed to save table data to " + fileName);
            }
        }
    }

    /**
     * Set the log level from the configuration model.
     */
    private void setLogLevel() {
        final Level newLevel = this.configurationModel.getLogLevel();
        if (LOGGER.getLevel() != newLevel) {
            LOGGER.setLevel(newLevel);
            LOGGER.log(Level.INFO, "Log Level was changed to <" + this.configurationModel.getLogLevel().toString()
                    + ">");
        }
    }

    /**
     * Setup AIDA plotting into the GUI components.
     */
    private void setupAida() {
        // Register the factory for displaying plots in tabs.
        MonitoringAnalysisFactory.register();

        // Set the root tab pane for displaying plots.
        MonitoringPlotFactory.setRootPane(this.frame.getPlotPanel().getPlotPane());

        // Setup the region listener to connect the plot info window.
        MonitoringPlotFactory.setPlotterRegionListener(new PlotterRegionListener() {
            @Override
            public void regionSelected(final PlotterRegion region) {
                if (region != null) {
                    MonitoringApplication.this.frame.getPlotInfoPanel().setCurrentRegion(region);
                }
            }
        });

        // Perform global configuration of the JFreeChart back end.
        AnalysisFactory.configure();
    }

    /**
     * Setup the logger.
     */
    private void setupLogger() {
        LOGGER.setUseParentHandlers(false);
        this.logHandler = new LogHandler();
        LOGGER.addHandler(this.logHandler);
        this.streamHandler = new MonitoringApplicationStreamHandler(System.out);
        LOGGER.addHandler(this.streamHandler);
        for (final Handler handler : LOGGER.getHandlers()) {
            handler.setLevel(DEFAULT_LEVEL);
        }
        LOGGER.setLevel(DEFAULT_LEVEL);
        LOGGER.info("logging initialized");
    }

    /**
     * Configure the system status monitor panel for a new job.
     */
    private void setupSystemStatusMonitor() {

        // Clear the system status monitor table.
        this.frame.getSystemStatusPanel().clear();

        // Get the global registry of SystemStatus objects.
        final SystemStatusRegistry registry = SystemStatusRegistry.getSystemStatusRegistery();

        // Process the SystemStatus objects.
        for (final SystemStatus systemStatus : registry.getSystemStatuses()) {
            // This will add the status to the two tables.
            this.frame.getSystemStatusPanel().addSystemStatus(systemStatus);
        }

        LOGGER.info("system status monitor initialized successfully");
    }

    /**
     * Show the settings dialog window.
     */
    private void showSettingsDialog() {
        this.frame.getSettingsDialog().setVisible(true);
    }

    /**
     * Start the AIDA server instance.
     */
    private void startAIDAServer() {
        if (this.configurationModel.hasValidProperty(ConfigurationModel.AIDA_SERVER_NAME_PROPERTY)) {
            this.server.setName(this.configurationModel.getAIDAServerName());
        }
        final boolean started = this.server.start();
        if (started) {
            this.frame.getApplicationMenu().startAIDAServer();
            LOGGER.info("AIDA server started at " + this.server.getName());
            DialogUtil
                    .showInfoDialog(this.frame, "AIDA Server Started", "The remote AIDA server started successfully.");
        } else {
            LOGGER.warning("AIDA server failed to start");
            DialogUtil.showErrorDialog(this.frame, "Failed to Start AIDA Server",
                    "The remote AIDA server failed to start.");
        }
    }

    /**
     * Start a new monitoring session.
     */
    private synchronized void startSession() {

        LOGGER.info("starting new session");

        try {

            // Reset the plot panel and global AIDA state.
            resetPlots();

            // The system status registry is cleared here before any event processors
            // which might create a SystemStatus are added to the event processing chain
            // e.g. an LCSim Driver, etc.
            SystemStatusRegistry.getSystemStatusRegistery().clear();

            // List of extra composite record processors including the updater for the RunPanel.
            final List<CompositeRecordProcessor> processors = new ArrayList<CompositeRecordProcessor>();
            processors.add(this.frame.getEventDashboard().new EventDashboardUpdater());

            // Add Driver to update the trigger diagnostics tables.
            final List<Driver> drivers = new ArrayList<Driver>();
            drivers.add(this.frame.getTriggerPanel().new TriggerDiagnosticGUIDriver());

            // Add listener to push conditions changes to conditions panel.
            final List<ConditionsListener> conditionsListeners = new ArrayList<ConditionsListener>();
            conditionsListeners.add(this.frame.getConditionsPanel().new ConditionsPanelListener());

            // Instantiate the event processing wrapper.
            this.processing = new EventProcessing(this, processors, drivers, conditionsListeners);

            // Connect to the ET system, if applicable.
            this.processing.connect();

            // Configure event processing from the global application settings, including setup of record loop.
            LOGGER.info("setting up event processing on source " + this.configurationModel.getDataSourcePath()
                    + " with type " + this.configurationModel.getDataSourceType());
            this.processing.setup(this.configurationModel);

            // Setup the system status monitor table.
            setupSystemStatusMonitor();

            // Start the event processing thread.
            this.processing.start();

            LOGGER.info("new session successfully initialized");

        } catch (final Exception e) {

            // Disconnect from the ET system.
            this.processing.disconnect();

            // Log the error that occurred and show a pop up dialog.
            this.errorHandler
                    .setError(e)
                    .log()
                    .printStackTrace()
                    .showErrorDialog(
                            "There was an error while starting the session." + '\n' + "See the log for details.",
                            "Session Error");

            LOGGER.severe("failed to start new session");
        }
    }

    /**
     * Stop the AIDA server instance.
     */
    private void stopAIDAServer() {
        this.server.disconnect();
        this.frame.getApplicationMenu().stopAIDAServer();
        LOGGER.info("AIDA server was stopped");
        DialogUtil.showInfoDialog(this.frame, "AIDA Server Stopped", "The AIDA server was stopped.");
    }

    /**
     * Save a screenshot to an output file.
     *
     * @param fileName the name of the output file
     * @param format the output file format (must be accepted by <code>ImageIO</code>)
     */
    private void writeScreenshot(final String fileName, final String format) {
        this.frame.repaint();
        final BufferedImage image = new BufferedImage(this.frame.getWidth(), this.frame.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        this.frame.paint(image.getGraphics());
        try {
            ImageIO.write(image, format, new File(fileName));
        } catch (final IOException e) {
            this.errorHandler.setError(e).setMessage("Failed to save screenshot.").printStackTrace().log()
                    .showErrorDialog();
        }
    }
}
