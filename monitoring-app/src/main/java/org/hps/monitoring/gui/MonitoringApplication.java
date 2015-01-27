package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.Commands.*;
import static org.hps.monitoring.gui.model.ConfigurationModel.MONITORING_APPLICATION_LAYOUT_PROPERTY;
import static org.hps.monitoring.gui.model.ConfigurationModel.SAVE_LAYOUT_PROPERTY;
import hep.aida.jfree.AnalysisFactory;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.jfree.plotter.PlotterRegionListener;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.hps.monitoring.enums.ConnectionStatus;
import org.hps.monitoring.enums.SteeringType;
import org.hps.monitoring.gui.model.Configuration;
import org.hps.monitoring.gui.model.ConfigurationModel;
import org.hps.monitoring.gui.model.RunModel;
import org.hps.monitoring.plotting.MonitoringAnalysisFactory;
import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusListener;
import org.hps.monitoring.subsys.SystemStatusRegistry;
import org.hps.monitoring.subsys.et.EtSystemMonitor;
import org.hps.monitoring.subsys.et.EtSystemStripCharts;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.composite.EventProcessingThread;
import org.hps.record.enums.DataSourceType;
import org.hps.record.et.EtConnection;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This class is the implementation of the GUI for the Monitoring Application.
 */
// TODO: Move GUI/window functionality to a new class. (This one is too big!)
public final class MonitoringApplication extends ApplicationWindow implements ActionListener, SystemStatusListener, PropertyChangeListener {

    // Top-level Swing components.
    private JPanel mainPanel;
    private EventButtonsPanel buttonsPanel;
    private ConnectionStatusPanel connectionStatusPanel;
    private RunPanel runPanel;
    private SettingsDialog settingsDialog;
    private PlotWindow plotWindow;
    private PlotInfoWindow plotInfoWindow = new PlotInfoWindow();
    private SystemStatusWindow systemStatusWindow;
    private JMenuBar menuBar;

    // References to menu items that will be toggled depending on application state.
    private JMenuItem savePlotsItem;
    private JMenuItem logItem;
    private JMenuItem terminalItem;
    private JMenuItem saveLayoutItem;

    // Saved references to System.out and System.err in case need to reset.
    private final PrintStream sysOut = System.out;
    private final PrintStream sysErr = System.err;

    // Error handling class for the application.
    private ErrorHandler errorHandler;

    // ET connection parameters and state.
    private EtConnection connection;

    // Event processing objects.
    private JobManager jobManager;
    private LCSimEventBuilder eventBuilder;
    private CompositeLoop loop;
    private EventProcessingThread processingThread;
    private Thread sessionWatchdogThread;

    // Logging objects.
    private static Logger logger;
    private Handler logHandler;
    private DefaultTableModel logTableModel;
    static final String[] logTableColumns = { "Date", "Message", "Level" };
    private JTable logTable;
    private static Level DEFAULT_LOG_LEVEL = Level.INFO;

    // Graogucs format for screenshots.
    private static final String SCREENSHOT_FORMAT = "png";

    // Format of date field for log.
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");

    // Some useful GUI size settings.
    private static final int SCREEN_WIDTH = ScreenUtil.getScreenWidth();
    private static final int SCREEN_HEIGHT = ScreenUtil.getScreenHeight();
    private final static int LOG_TABLE_WIDTH = 700; /* FIXME: Should be set from main panel width. */
    private final static int LOG_TABLE_HEIGHT = 270;
    private static final int MAIN_FRAME_HEIGHT = ScreenUtil.getScreenHeight() / 2;
    private static final int MAIN_FRAME_WIDTH = 650;

    // Default config which can be overridden by command line argument.
    private static final String DEFAULT_CONFIG_RESOURCE = "/org/hps/monitoring/config/default_config.prop";

    // The application global Configuration object which is the default configuration unless
    // overridden.
    private Configuration configuration = new Configuration(DEFAULT_CONFIG_RESOURCE);

    // The ConfigurationModel for updating GUI components from the global configuration.
    private ConfigurationModel configurationModel = new ConfigurationModel();

    // The RunModel for updating the RunPanel.
    private RunModel runModel = new RunModel();

    private FileValidationThread fileValidationThread;

    /**
     * Constructor for the monitoring application.
     */
    public MonitoringApplication() {

        super(getApplicationTitle());

        // Add the application as a property change listener on the configuration model.
        configurationModel.addPropertyChangeListener(this);
    }

    /**
     * Initialize GUI components and all other necessary objects to put the application in a usable
     * state.
     */
    public void initialize() {

        // Create and configure the logger.
        setupLogger();

        // Setup the error handling class.
        setupErrorHandler();

        // Setup an uncaught exception handler.
        setupUncaughtExceptionHandler();

        // Create the main GUI panel.
        createMainPanel();

        // Create the log table GUI component.
        createLogTable();

        // Create settings dialog window.
        createSettingsDialog();

        // Setup the application menus.
        createMenuBar();

        // Create the system status window.
        createSystemStatusWindow();

        // Configuration of window for showing plots.
        createPlotWindow();

        // Setup AIDA.
        setupAida();

        // Configure the application's primary JFrame.
        configApplicationFrame();

        // Load the current configuration, which will push values into the GUI.
        loadConfiguration();

        // Log that the application started successfully.
        log(Level.CONFIG, "Application initialized successfully.");
    }

    /**
     * The action handler method for the application.
     * @param e The event to handle.
     */
    public void actionPerformed(ActionEvent e) {

        // System.out.println("MonitoringApplication. actionPerformed: " + e.getActionCommand());

        String cmd = e.getActionCommand();
        if (CONNECT.equals(cmd)) {
            // Run the start session method on a seperate thread.
            new Thread() {
                public void run() {
                    startSession();
                }
            }.start();
        } else if (DISCONNECT.equals(cmd)) {
            // Run the stop session method on a seperate thread.
            new Thread() {
                public void run() {
                    stopSession();
                }
            }.start();
        } else if (SAVE_PLOTS.equals(cmd)) {
            savePlots();
        } else if (CHOOSE_LOG_FILE.equals(cmd)) {
            chooseLogFile();
        } else if (LOG_TO_TERMINAL.equals(cmd)) {
            logToTerminal();
        } else if (SCREENSHOT.equals(cmd)) {
            chooseScreenshot();
        } else if (EXIT.equals(cmd)) {
            exit();
        } else if (SAVE_LOG_TABLE.equals(cmd)) {
            saveLogTableToFile();
        } else if (CLEAR_LOG_TABLE.equals(cmd)) {
            clearLogTable();
        } else if (PAUSE.equals(cmd)) {
            pauseEventProcessing();
        } else if (NEXT.equals(cmd)) {
            nextEvent();
        } else if (RESUME.equals(cmd)) {
            resumeEventProcessing();
        } else if (LOG_LEVEL_CHANGED.equals(cmd)) {
            setLogLevel();
        } else if (AIDA_AUTO_SAVE.equals(cmd)) {
            getJobSettingsPanel().chooseAidaAutoSaveFile();
        } else if (SHOW_SETTINGS.equals(cmd)) {
            showSettingsDialog();
        } else if (SELECT_CONFIG_FILE.equals(cmd)) {
            chooseConfigurationFile();
        } else if (SAVE_CONFIG_FILE.equals(cmd)) {
            updateLayoutConfiguration(); /* Save current GUI layout settings first, if needed. */
            saveConfigurationFile();
        } else if (LOAD_DEFAULT_CONFIG_FILE.equals(cmd)) {
            loadDefaultConfigFile();
        } else if (SAVE_LAYOUT.equals(cmd)) {
            setSaveLayout();
        } else if (RESTORE_DEFAULT_GUI_LAYOUT.equals(cmd)) {
            restoreDefaultLayout();
        } else if (VALIDATE_DATA_FILE.equals(cmd)) {
            if (fileValidationThread == null) {
                new FileValidationThread().start();
            }
        } else if (RESET_PLOTS.equals(cmd)) {
            resetAidaTree();
        }
    }

    /**
     * Set the GUI to visible.
     */
    public void setVisible(boolean visible) {

        super.setVisible(true);

        this.systemStatusWindow.setVisible(true);

        // FIXME: If this is done earlier before app is visible, then the GUI will fail to show!
        this.connectionStatusPanel.setConnectionStatus(ConnectionStatus.DISCONNECTED);
    }

    /**
     * Set the Configuration but don't update the ConfigurationModel.
     * @param configuration
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Handle a property change event.
     * @param evt The property change event.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getPropertyName().equals("ancestor"))
            return;
        Object value = evt.getNewValue();
        if (evt.getPropertyName().equals(SAVE_LAYOUT_PROPERTY)) {
            if (value != null) {
                saveLayoutItem.setSelected((Boolean) value);
            } 
        } else if (evt.getPropertyName().equals(MONITORING_APPLICATION_LAYOUT_PROPERTY)) {
            updateWindowConfiguration(new WindowConfiguration((String) value));
        } else if (evt.getPropertyName().equals(ConfigurationModel.SYSTEM_STATUS_FRAME_LAYOUT_PROPERTY)) {
            if (systemStatusWindow != null) {
                systemStatusWindow.updateWindowConfiguration(new WindowConfiguration((String) value));
            } else {
                System.err.println("ERROR: The systemStatusFrame is null!");
            }
        } else if (evt.getPropertyName().equals(ConfigurationModel.PLOT_FRAME_LAYOUT_PROPERTY)) {
            if (plotWindow != null) {
                plotWindow.updateWindowConfiguration(new WindowConfiguration((String) value));
            } else {
                System.err.println("ERROR: The plotWindow is null!");
            }
        } else if (evt.getPropertyName().equals(ConfigurationModel.LOG_TO_FILE_PROPERTY)) {
            if ((Boolean) value == true) {
                logToFile(new File(configurationModel.getLogFileName()));
            } else {
                logToTerminal();
            }
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
        log(level, "STATUS, " + "subsys: " + status.getSubsystem() + ", " + "code: " + status.getStatusCode().name() + ", " + "descr: " + status.getDescription() + ", " + "mesg: " + status.getMessage());
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        plotWindow.setEnabled(enabled);
        systemStatusWindow.setEnabled(enabled);
        // settingsDialog.setEnabled(false);

        // this.setFocusable(enabled);
        // plotWindow.setFocusable(enabled);
        // systemStatusWindow.setFocusable(enabled);
        // settingsDialog.setFocusable(false);
    }

    /* -------------------------- private methods ----------------------------- */

    /**
     * Setup the error handler.
     */
    private void setupErrorHandler() {
        errorHandler = new ErrorHandler(this, logger);
    }

    /**
     * Setup the uncaught exception handler which will trap unhandled errors.
     */
    private void setupUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable exception) {
                MonitoringApplication.this.errorHandler.setError(exception).log().printStackTrace().showErrorDialog();
                // FIXME: This should probably cause a system.exit after the dialog box is closed!
            }
        });
    }

    /**
     * Create the settings dialog GUI component.
     */
    private void createSettingsDialog() {

        // Create and configure the settings dialog which has sub-panels for application
        // configuration.
        settingsDialog = new SettingsDialog();
        settingsDialog.getSettingsPanel().addActionListener(this);
        getJobSettingsPanel().addActionListener(this);
        settingsDialog.getSettingsPanel().getDataSourcePanel().addActionListener(this);

        // Push the ConfigurationModel to the job settings dialog.
        getJobSettingsPanel().setConfigurationModel(configurationModel);
        getConnectionSettingsPanel().setConfigurationModel(configurationModel);
        settingsDialog.getSettingsPanel().getDataSourcePanel().setConfigurationModel(configurationModel);
    }

    /**
     * Create the plot window.
     */
    private void createPlotWindow() {

        // Create the JFrame.
        plotWindow = new PlotWindow();

        // Set initial size and position which might be overridden later.
        plotWindow.setDefaultWindowConfiguration(new WindowConfiguration(SCREEN_WIDTH - MAIN_FRAME_WIDTH, SCREEN_HEIGHT, (int) (ScreenUtil.getBoundsX(0)) + MAIN_FRAME_WIDTH, plotWindow.getY()));
    }

    private void createSystemStatusWindow() {
        systemStatusWindow = new SystemStatusWindow();
        WindowConfiguration wc = new WindowConfiguration(
                650, /* FIXME: Hard-coded width setting. */
                //ScreenUtil.getScreenHeight() - mainPanel.getHeight(),
                400,
                (int) ScreenUtil.getBoundsX(0), 
                MAIN_FRAME_HEIGHT);
        systemStatusWindow.setMinimumSize(new Dimension(wc.width, wc.height));
        systemStatusWindow.setDefaultWindowConfiguration(wc);
    }

    /**
     * Configure the AIDA plotting backend.
     */
    private void setupAida() {
        MonitoringAnalysisFactory.register();
        MonitoringPlotFactory.setRootPane(this.plotWindow.getPlotPane());
        MonitoringPlotFactory.setPlotterRegionListener(new PlotterRegionListener() {
            @Override
            public void regionSelected(PlotterRegion region) {
                if (region == null)
                    throw new RuntimeException("The region arg is null!!!");
                // System.out.println("MonitoringApplication - regionSelected - " + region.title());
                plotInfoWindow.setCurrentRegion(region);
            }
        });
        AnalysisFactory.configure();
    }

    /**
     * Create the main panel.
     */
    private void createMainPanel() {

        // Main panel setup.
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        // This var is used for layout of each sub-component.
        GridBagConstraints c;

        // Event processing buttons.
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 0, 0, 10);
        buttonsPanel = new EventButtonsPanel();
        buttonsPanel.addActionListener(this);
        mainPanel.add(buttonsPanel, c);

        // Connection status panel.
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 5, 0);
        connectionStatusPanel = new ConnectionStatusPanel();
        mainPanel.add(connectionStatusPanel, c);

        // Run status panel.
        runPanel = new RunPanel(runModel);
        c = new GridBagConstraints();
        c.insets = new Insets(5, 0, 5, 0);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 2;
        mainPanel.add(runPanel, c);
    }

    /**
     * Create the application menu bar and menu items.
     */
    private void createMenuBar() {

        menuBar = new JMenuBar();

        JMenu applicationMenu = new JMenu("Application");
        applicationMenu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(applicationMenu);

        JMenuItem loadConfigItem = new JMenuItem("Load Settings ...");
        loadConfigItem.addActionListener(this);
        loadConfigItem.setMnemonic(KeyEvent.VK_C);
        loadConfigItem.setActionCommand(SELECT_CONFIG_FILE);
        loadConfigItem.setToolTipText("Load application settings from a properties file");
        applicationMenu.add(loadConfigItem);

        JMenuItem saveConfigItem = new JMenuItem("Save Settings ...");
        saveConfigItem.addActionListener(this);
        saveConfigItem.setMnemonic(KeyEvent.VK_S);
        saveConfigItem.setActionCommand(SAVE_CONFIG_FILE);
        saveConfigItem.setToolTipText("Save settings to a properties file");
        applicationMenu.add(saveConfigItem);

        JMenuItem settingsItem = new JMenuItem("Show Settings ...");
        settingsItem.setMnemonic(KeyEvent.VK_P);
        settingsItem.setActionCommand(SHOW_SETTINGS);
        settingsItem.addActionListener(this);
        settingsItem.setToolTipText("Show application settings menu");
        applicationMenu.add(settingsItem);

        applicationMenu.addSeparator();

        saveLayoutItem = new JCheckBoxMenuItem("Save GUI Layout");
        saveLayoutItem.setActionCommand(SAVE_LAYOUT);
        saveLayoutItem.addActionListener(this);
        saveLayoutItem.setToolTipText("Include current GUI layout when saving settings.");
        if (configurationModel.hasPropertyValue(ConfigurationModel.SAVE_LAYOUT_PROPERTY)) {
            saveLayoutItem.setSelected(configurationModel.getSaveLayout());
        }
        saveLayoutItem.addPropertyChangeListener(this); 
        
        applicationMenu.add(saveLayoutItem);

        JMenuItem restoreLayoutItem = new JMenuItem("Restore Default GUI Layout");
        restoreLayoutItem.setActionCommand(RESTORE_DEFAULT_GUI_LAYOUT);
        restoreLayoutItem.addActionListener(this);
        restoreLayoutItem.setToolTipText("Restore the GUI windows to their default positions and sizes");
        applicationMenu.add(restoreLayoutItem);

        applicationMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setActionCommand(EXIT);
        exitItem.addActionListener(this);
        exitItem.setToolTipText("Exit from the application");
        applicationMenu.add(exitItem);

        JMenu plotsMenu = new JMenu("Plots");
        plotsMenu.setMnemonic(KeyEvent.VK_O);
        menuBar.add(plotsMenu);

        JMenuItem aidaAutoSaveItem = new JMenuItem("Set AIDA Auto Save File ...");
        aidaAutoSaveItem.setMnemonic(KeyEvent.VK_A);
        aidaAutoSaveItem.setActionCommand(AIDA_AUTO_SAVE);
        aidaAutoSaveItem.addActionListener(this);
        aidaAutoSaveItem.setToolTipText("Select name of file to auto save AIDA plots at end of job.");
        plotsMenu.add(aidaAutoSaveItem);

        savePlotsItem = new JMenuItem("Save Plots to AIDA File...");
        savePlotsItem.setMnemonic(KeyEvent.VK_P);
        savePlotsItem.setActionCommand(SAVE_PLOTS);
        savePlotsItem.addActionListener(this);
        savePlotsItem.setEnabled(false);
        savePlotsItem.setToolTipText("Save plots from default AIDA tree to an output file.");
        plotsMenu.add(savePlotsItem);
        
        JMenuItem resetPlotsItem = new JMenuItem("Reset Plots");
        resetPlotsItem.setMnemonic(KeyEvent.VK_R);
        resetPlotsItem.setActionCommand(RESET_PLOTS);
        resetPlotsItem.addActionListener(this);
        resetPlotsItem.setEnabled(true);
        resetPlotsItem.setToolTipText("Reset all AIDA plots in the default tree.");
        plotsMenu.add(resetPlotsItem);

        JMenu logMenu = new JMenu("Log");
        logMenu.setMnemonic(KeyEvent.VK_L);
        menuBar.add(logMenu);

        logItem = new JMenuItem("Redirect to File ...");
        logItem.setMnemonic(KeyEvent.VK_F);
        logItem.setActionCommand(CHOOSE_LOG_FILE);
        logItem.addActionListener(this);
        logItem.setEnabled(true);
        logItem.setToolTipText("Redirect std out and err to a file.");
        logMenu.add(logItem);

        terminalItem = new JMenuItem("Redirect to Terminal");
        terminalItem.setMnemonic(KeyEvent.VK_T);
        terminalItem.setActionCommand(LOG_TO_TERMINAL);
        terminalItem.addActionListener(this);
        terminalItem.setEnabled(false);
        terminalItem.setToolTipText("Redirect std out and err back to the terminal.");
        logMenu.add(terminalItem);

        JMenuItem saveLogItem = new JMenuItem("Save Log Table to File ...");
        saveLogItem.setMnemonic(KeyEvent.VK_S);
        saveLogItem.setActionCommand(SAVE_LOG_TABLE);
        saveLogItem.addActionListener(this);
        saveLogItem.setToolTipText("Save the log records to a tab delimited text file.");
        logMenu.add(saveLogItem);

        JMenuItem clearLogItem = new JMenuItem("Clear Log Table");
        clearLogItem.addActionListener(this);
        clearLogItem.setMnemonic(KeyEvent.VK_C);
        clearLogItem.setActionCommand(CLEAR_LOG_TABLE);
        clearLogItem.setToolTipText("Clear the log table of all messages.");
        logMenu.add(clearLogItem);

        JMenu utilMenu = new JMenu("Util");
        plotsMenu.setMnemonic(KeyEvent.VK_U);
        menuBar.add(utilMenu);

        JMenuItem screenshotItem = new JMenuItem("Take a Screenshot ...");
        screenshotItem.setMnemonic(KeyEvent.VK_N);
        screenshotItem.setActionCommand(SCREENSHOT);
        screenshotItem.addActionListener(this);
        screenshotItem.setToolTipText("Save a full screenshot to a " + SCREENSHOT_FORMAT + " file.");
        utilMenu.add(screenshotItem);
    }

    /**
     * Log handler for inserting messages into the log table.
     */
    private class MonitoringApplicationLogHandler extends Handler {

        /**
         * Puts log messages into the log table.
         */
        public void publish(LogRecord record) {
            // Add the row to the log table.
            Object[] row = new Object[] { dateFormat.format(new Date(record.getMillis())), record.getLevel(), record.getMessage() };
            logTableModel.insertRow(logTable.getRowCount(), row);

            // Print all messages to System.out so they show up in the terminal or log file output.
            System.out.println(row[0] + " :: " + MonitoringApplication.class.getSimpleName() + " :: " + row[1] + " :: " + row[2]);
        }

        public void close() throws SecurityException {
        }

        public void flush() {
        }
    }

    /**
     * Creates the log table component, which is a JTable containing messages from the logger.
     */
    private void createLogTable() {

        String data[][] = new String[0][0];
        logTableModel = new DefaultTableModel(data, logTableColumns);
        logTable = new JTable(logTableModel);
        logTable.setEnabled(false);
        logTable.setAutoCreateRowSorter(true);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1.0;
        c.insets = new Insets(0, 0, 5, 3);
        JScrollPane logPane = new JScrollPane(logTable);
        logPane.setPreferredSize(new Dimension(LOG_TABLE_WIDTH, LOG_TABLE_HEIGHT));
        logPane.setMinimumSize(new Dimension(LOG_TABLE_WIDTH, LOG_TABLE_HEIGHT));
        mainPanel.add(logPane, c);
    }

    /**
     * Setup the application's Logger object for writing messages to the log table.
     */
    private void setupLogger() {
        logger = Logger.getLogger(this.getClass().getSimpleName());
        logHandler = new MonitoringApplicationLogHandler();
        logger.setUseParentHandlers(false);
        logger.addHandler(logHandler);
        logger.setLevel(Level.ALL);
    }

    /**
     * Show the settings window.
     */
    private void showSettingsDialog() {
        settingsDialog.setVisible(true);
    }

    /**
     * Set a new log level for the application. If the new log level is the same as the old one, a
     * new log level will NOT be set.
     */
    private void setLogLevel() {
        Level newLevel = configurationModel.getLogLevel();
        if (logger.getLevel() != newLevel) {
            logger.setLevel(newLevel);
            log(Level.INFO, "Log Level was changed to <" + configurationModel.getLogLevel().toString() + ">");
        }
    }

    /**
     * Set the connection status.
     * @param status The connection status.
     */
    private void setConnectionStatus(ConnectionStatus status) {
        connectionStatusPanel.setConnectionStatus(status);
        log(Level.FINE, "Connection status changed to <" + status.name() + ">");
        logHandler.flush();
    }

    /**
     * Setup the primary <code>JFrame</code> for the application.
     */
    private void configApplicationFrame() {

        mainPanel.setOpaque(true);

        // Configure window size and position.
        WindowConfiguration wc = new WindowConfiguration(MAIN_FRAME_WIDTH, MAIN_FRAME_HEIGHT, (int) ScreenUtil.getBoundsX(0), getY());
        setMinimumSize(new Dimension(wc.width, wc.height));
        setPreferredSize(new Dimension(wc.width, wc.height));
        setDefaultWindowConfiguration(wc);

        setResizable(true);
        setContentPane(mainPanel);
        setJMenuBar(menuBar);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        pack();
    }

    /**
     * Save all the plots to a file using a <code>JFileChooser</code>.
     */
    private void savePlots() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showSaveDialog(mainPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File fileName = fc.getSelectedFile();
            try {
                AIDA.defaultInstance().saveAs(fileName);
                logger.log(Level.INFO, "Plots saved to file <" + fileName + ">");
            } catch (IOException e) {
                errorHandler.setError(e).setMessage("Error saving plots to file.").printStackTrace().log().showErrorDialog();
            }
        }
    }

    /**
     * Get the full title of the application.
     * @return The application title.
     */
    private static String getApplicationTitle() {
        return "HPS Monitoring - " + getUserName() + "@" + getHostname();
    }

    /**
     * Get the hostname, which is used in the application title.
     * @return The hostname.
     */
    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN_HOST";
        }
    }

    /**
     * Get the user name, which is used in the application title.
     * @return The user name.
     */
    private static String getUserName() {
        if (System.getProperty("user.name") == null) {
            return "UNKNOWN_USER";
        } else {
            return System.getProperty("user.name");
        }
    }

    /**
     * Redirect <code>System.out</code> and <code>System.err</code> to a file.
     * @param file The output log file.
     * @throws FileNotFoundException if the file does not exist.
     */
    private void redirectStdOutAndErrToFile(File file) {
        try {
            PrintStream ps = new PrintStream(new FileOutputStream(file.getPath()));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception e) {
            errorHandler.setError(e).log().printStackTrace().raiseException();
        }
    }

    /**
     * Redirect <code>System.out</code> and <code>System.err</code> back to the terminal, e.g. if
     * they were previously sent to a file.
     */
    private void logToTerminal() {
        System.setOut(sysOut);
        System.setErr(sysErr);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                configurationModel.setLogToFile(false);

                // FIXME: These should be toggled via a PropertyChangeListener on the
                // ConfigurationModel.
                terminalItem.setEnabled(false);
                logItem.setEnabled(true);
            }
        });
        log(Level.INFO, "Redirected std out and err back to terminal.");
    }

    /**
     * Redirect <code>System.out</code> and <code>System.err</code> to a file.
     */
    private void logToFile(File file) {
        redirectStdOutAndErrToFile(file);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // FIXME: These should be toggled via PropertyChangeListener on the
                // ConfigurationModel.
                terminalItem.setEnabled(true);
                logItem.setEnabled(false);
            }
        });
        log("Redirected System.out and err to file <" + file.getPath() + ">");
    }

    /**
     * Set the GUI state to disconnected, which will enable/disable applicable GUI components and
     * menu items.
     */
    private void setDisconnectedGuiState() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // Enable or disable appropriate menu items.
                savePlotsItem.setEnabled(false);
                logItem.setEnabled(true);
                terminalItem.setEnabled(true);

                // Re-enable the ConnectionPanel.
                getConnectionSettingsPanel().enableConnectionPanel(true);

                // Re-enable the getJobPanel().
                // getJobSettingsPanel().enableJobPanel(true);

                // Set relevant event panel buttons to disabled.
                buttonsPanel.enablePauseButton(false);
                buttonsPanel.enableNextEventsButton(false);

                // Toggle connection button to proper setting.
                buttonsPanel.setConnected(false);
            }
        });
    }

    /**
     * Set the GUI to connected state, which will enable/disable appropriate components and menu
     * items.
     */
    private void setConnectedGuiState() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // Disable connection panel.
                getConnectionSettingsPanel().enableConnectionPanel(false);

                // Disable getJobPanel().
                // getJobSettingsPanel().enableJobPanel(false);

                // Enable or disable appropriate menu items.
                savePlotsItem.setEnabled(true);
                logItem.setEnabled(false);
                terminalItem.setEnabled(false);

                // Enable relevant event panel buttons.
                buttonsPanel.enablePauseButton(true);

                // Toggle connection button to proper settings.
                buttonsPanel.setConnected(true);
            }
        });
    }

    /**
     * Exit from the application.
     */
    private void exit() {
        if (connection != null) {
            cleanupEtConnection();
        }
        setVisible(false);
        System.exit(0);
    }

    /**
     * Save a screenshot to a file using a file chooser.
     */
    private void chooseScreenshot() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Screenshot");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(mainPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            String fileName = fc.getSelectedFile().getPath();
            int extIndex = fileName.lastIndexOf(".");
            if ((extIndex == -1) || !(fileName.substring(extIndex + 1, fileName.length())).toLowerCase().equals(SCREENSHOT_FORMAT)) {
                fileName = fileName + "." + SCREENSHOT_FORMAT;
            }
            takeScreenshot(fileName);
            log(Level.INFO, "Screenshot saved to file <" + fileName + ">");
        }
    }

    /**
     * Save a screenshot to an output file.
     * @param fileName The name of the output file.
     */
    private void takeScreenshot(String fileName) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle screenRectangle = new Rectangle(screenSize);
        try {
            Robot robot = new Robot();
            BufferedImage image = robot.createScreenCapture(screenRectangle);
            ImageIO.write(image, SCREENSHOT_FORMAT, new File(fileName));
        } catch (Exception e) {
            errorHandler.setError(e).setMessage("Failed to take screenshot.").printStackTrace().log().showErrorDialog();
        }
    }

    /**
     * Start a new monitoring session. This method is executed in a separate thread from the EDT
     * within {@link #actionPerformed(ActionEvent)} so GUI updates are not blocked while the session
     * is being setup.
     */
    private void startSession() {

        log(Level.FINE, "Starting a new monitoring session.");

        // Show a modal window that will block the GUI until connected or an error occurs.
        JDialog dialog = DialogUtil.showStatusDialog(this, "Info", "Starting new session ...");

        try {

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

            // Setup the system status monitor table.
            setupSystemStatusMonitor();

            // Start thread which will trigger a disconnect if the event processing finishes.
            startSessionWatchdogThread();            

            // FIXME: Apparently, the visible plots won't draw without this!  (Unless the user clicks directly on the tab.)
            plotWindow.getPlotPane().requestFocusInWindow();            

            log(Level.INFO, "Successfully started the monitoring session.");

        } catch (Exception e) {

            log(Level.SEVERE, "An error occurred while setting up the session.");

            // Log the error that occurred.
            errorHandler.setError(e).log().printStackTrace();

            // Disconnect from the session.
            disconnect(ConnectionStatus.ERROR);

        } finally {
            // Close modal window.
            dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
        }
    }

    /**
     * Start the session watchdog thread, which will kill the session if event processing finishes.
     */
    private void startSessionWatchdogThread() {
        sessionWatchdogThread = new SessionWatchdogThread();
        sessionWatchdogThread.start();
    }

    /**
     * Connect to the ET system using the current connection settings.
     */
    private void connect() throws IOException {

        // Make sure applicable menu items are enabled or disabled.
        // This applies whether or not using an ET server or file source.
        setConnectedGuiState();

        // Setup the network connection if using an ET server.
        if (usingEtServer()) {

            setConnectionStatus(ConnectionStatus.CONNECTION_REQUESTED);

            // Create a connection to the ET server.
            try {
                createEtConnection();
                log(Level.INFO, "Successfully connected to ET system.");
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            // This is when a direct file source is used and ET is not needed.
            this.setConnectionStatus(ConnectionStatus.CONNECTED);
        }
    }

    private ConnectionSettingsPanel getConnectionSettingsPanel() {
        return settingsDialog.getSettingsPanel().getConnectionPanel();
    }

    private JobSettingsPanel getJobSettingsPanel() {
        return settingsDialog.getSettingsPanel().getJobSettingsPanel();
    }

    private void disconnect() {
        disconnect(ConnectionStatus.DISCONNECTING);
    }

    /**
     * Disconnect from the current ET session with a particular status.
     * @param status The connection status.
     */
    private void disconnect(ConnectionStatus status) {

        log(Level.FINE, "Disconnecting the current session.");

        // Cleanup the ET connection.
        cleanupEtConnection();

        // Update state of GUI to disconnected.
        setDisconnectedGuiState();

        // Finally, change application state to fully disconnected.
        setConnectionStatus(ConnectionStatus.DISCONNECTED);

        // Set the application status from the caller if an error occurred.
        if (status == ConnectionStatus.ERROR)
            setConnectionStatus(status);

        log(Level.INFO, "Disconnected from the session.");
    }

    /**
     * Cleanup the ET connection.
     */
    private void cleanupEtConnection() {
        if (connection != null) {
            if (connection.getEtSystem().alive()) {
                log(Level.FINEST, "Cleaning up the ET connection.");
                connection.cleanup();
                log(Level.FINEST, "Done cleaning up the ET connection.");
            }
            connection = null;
        }
    }

    /**
     * Setup the LCSim job manager and the event builder.
     */
    private void setupLCSim() {

        log(Level.INFO, "Setting up LCSim.");

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

        log(Level.CONFIG, "Set steering to <" + steering + "> with type <" + (steeringType == SteeringType.RESOURCE ? "RESOURCE" : "FILE") + ">");

        try {
            // Create and the job manager.  The conditions manager is instantiated from this call but not configured.
            jobManager = new JobManager();
            
            if (configurationModel.hasValidProperty(ConfigurationModel.DETECTOR_ALIAS_PROPERTY)) {
                // Set a detector alias.                
                ConditionsReader.addAlias(configurationModel.getDetectorName(), "file://" + configurationModel.getDetectorAlias());
                logger.config("using detector alias " + configurationModel.getDetectorAlias());
            }
                        
            // Setup the event builder to translate from EVIO to LCIO.
            // This must happen before Driver setup so the builder's listeners are activated first!
            createEventBuilder();
            
            // Configure the job manager for the XML steering.
            jobManager.setPerformDryRun(true);
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
                if (configurationModel.hasPropertyValue(ConfigurationModel.FREEZE_CONDITIONS_PROPERTY)) {
                    // Freeze the conditions system to ignore run numbers from the events.  
                    logger.config("user configured to freeze conditions system from monitoring app");
                    conditionsManager.freeze();
                } else {
                    // Allow run numbers to be picked up from the events.
                    logger.config("user run number specified but conditions system is NOT frozen");
                    conditionsManager.unfreeze();
                }
            }

            log(Level.INFO, "LCSim setup was successful.");

        } catch (Throwable t) {
            // Catch all errors and rethrow them as RuntimeExceptions.
            errorHandler.setError(t).setMessage("Error setting up LCSim.").printStackTrace().raiseException();
        }
    }

    private void setupSteeringFile(String steering) {
        log(Level.CONFIG, "Setting up steering file <" + steering + ">");
        jobManager.setup(new File(steering));
    }

    private void setupSteeringResource(String steering) throws IOException {
        log(Level.CONFIG, "Setting up steering resource <" + steering + ">");
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(steering);
        if (is == null)
            throw new IOException("Steering resource is not accessible or does not exist.");
        jobManager.setup(is);
        is.close();
    }
        
    /**
     * Create the event builder for converting EVIO events to LCSim.
     */
    private void createEventBuilder() {

        // Get the class for the event builder.
        String eventBuilderClassName = configurationModel.getEventBuilderClassName();

        log(Level.FINE, "Initializing event builder <" + eventBuilderClassName + ">");

        try {
            // Create a new instance of the builder class.
            eventBuilder = (LCSimEventBuilder) Class.forName(eventBuilderClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LCSimEventBuilder.", e);
        }

        // Add the builder as a listener so it is notified when conditions change.
        ConditionsManager.defaultInstance().addConditionsListener(eventBuilder);

        log(Level.CONFIG, "Successfully initialized event builder <" + eventBuilderClassName + ">");
    }
    
    /**
     * Create a connection to an ET system using current parameters from the GUI. If successful, the
     * application's ConnectionStatus is changed to CONNECTED.
     */
    private void createEtConnection() {

        // Setup connection to ET system.
        connection = fromConfigurationModel(configurationModel);

        if (connection != null) {

            // Set status to connected as there is now a live ET connection.
            setConnectionStatus(ConnectionStatus.CONNECTED);

            log(Level.INFO, "Successfully connected to ET system.");

        } else {
            // Some error occurred and the connection was not created.
            setConnectionStatus(ConnectionStatus.ERROR);

            errorHandler.setError(new RuntimeException("Failed to create ET connection.")).log().printStackTrace().raiseException();
        }
    }

    /**
     * Save the log table to a tab-delimited text file selected by a <code>JFileChooser</code>.
     */
    private void saveLogTableToFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Log File");
        fc.setCurrentDirectory(new File("."));
        int fcs = fc.showSaveDialog(mainPanel);
        if (fcs == JFileChooser.APPROVE_OPTION) {
            final File logFile = fc.getSelectedFile();
            if (logFile.exists()) {
                JOptionPane.showMessageDialog(this, "The log file already exists.");
            } else {
                StringBuffer buf = new StringBuffer();
                Vector<Vector> rows = logTableModel.getDataVector();
                for (Vector row : (Vector<Vector>) rows) {
                    buf.append(row.get(0).toString() + '\t' + row.get(1).toString() + '\t' + row.get(2).toString() + '\t' + row.get(3).toString() + '\n');
                }
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(logFile.getPath()));
                    out.write(buf.toString());
                    out.close();
                    log("Saved log to file <" + logFile.getPath() + ">");
                } catch (IOException e) {
                    errorHandler.setError(e).setMessage("Error saving log to file.").log().printStackTrace().showErrorDialog();
                }
            }
        }
    }

    /**
     * Clear all data from the log table.
     */
    private void clearLogTable() {
        logTableModel.setRowCount(0);
        log(Level.INFO, "Log table was cleared.");
    }

    /**
     * Notify event processor to get next set of events, if in pause mode.
     */
    private void nextEvent() {
        this.setConnectionStatus(ConnectionStatus.CONNECTED);
        loop.execute(Command.GO_N, 1L, true);
        log(Level.FINEST, "Getting next event.");
        this.setConnectionStatus(ConnectionStatus.PAUSED);
    }

    /**
     * Notify the event processor to resume processing events, if paused.
     */
    private void resumeEventProcessing() {
        // Notify event processor to continue.
        loop.resume();

        // Set state of event buttons.
        buttonsPanel.setPauseModeState(false);

        log(Level.FINEST, "Resuming event processing after pause.");

        this.setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    /**
     * Notify the event processor to start pause mode, which will pause between events.
     */
    private void pauseEventProcessing() {

        loop.pause();

        // Set GUI state.
        buttonsPanel.setPauseModeState(true);

        log(Level.FINEST, "Event processing was paused.");

        this.setConnectionStatus(ConnectionStatus.PAUSED);
    }

    /**
     * Reset the contents of the default AIDA tree.
     */
    private void resetAidaTree() {
        AIDA.defaultInstance().clearAll();
        log("Reset default AIDA tree.");
    }

    /**
     * Send a message to the logger with given level.
     * @param level The log message's level.
     * @param m The message.
     */
    private void log(Level level, String m) {
        if (logger != null && logTable != null)
            logger.log(level, m);
    }

    /**
     * Send a message to the logger with the default level.
     * @param m The message.
     */
    private void log(String m) {
        log(DEFAULT_LOG_LEVEL, m);
    }

    /**
     * Configure the event processing chain.
     */
    private void setupCompositeLoop() {

        CompositeLoopConfiguration loopConfig = new CompositeLoopConfiguration().setStopOnEndRun(configurationModel.getDisconnectOnEndRun()).setStopOnErrors(configurationModel.getDisconnectOnError()).setDataSourceType(configurationModel.getDataSourceType()).setProcessingStage(configurationModel.getProcessingStage()).setEtConnection(connection).setFilePath(configurationModel.getDataSourcePath()).setLCSimEventBuilder(eventBuilder).setDetectorName(configurationModel.getDetectorName());

        // Add all Drivers from the pre-configured JobManager.
        for (Driver driver : jobManager.getDriverExecList()) {
            loopConfig.add(driver);
        }

        // DEBUG: Turn these off while doing other stuff!!!!
        // Using ET server?
        if (usingEtServer()) {

            // ET system monitor.
            // FIXME: Make whether this is run or not configurable through the JobPanel.
            loopConfig.add(new EtSystemMonitor());

            // ET system strip charts.
            // FIXME: Make whether this is run or not configurable through the JobPanel.
            loopConfig.add(new EtSystemStripCharts());
        }

        // RunPanel updater.
        loopConfig.add(runPanel.new RunModelUpdater());
                
        // Setup for conditions activation via EVIO events.
        loopConfig.add(new EvioDetectorConditionsProcessor(configurationModel.getDetectorName()));

        // Create the CompositeLoop with the configuration.
        loop = new CompositeLoop(loopConfig);
                
        // Create the processing thread.
        processingThread = new EventProcessingThread(loop);

        // Start the processing thread.
        processingThread.start();
    }

    /**
     * True if ET server is being used.
     * @return True if using ET server.
     */
    private boolean usingEtServer() {
        return configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER);
    }

    /**
     * Configure the system status monitor panel for a new job.
     */
    private void setupSystemStatusMonitor() {
        // Clear the system status monitor table.
        systemStatusWindow.getTableModel().clear();

        // Get the global registry of SystemStatus objects.
        SystemStatusRegistry registry = SystemStatusRegistry.getSystemStatusRegistery();

        // Process the SystemStatus objects.
        for (SystemStatus systemStatus : registry.getSystemStatuses()) {
            // Add a row to the table for every SystemStatus.
            systemStatusWindow.getTableModel().addSystemStatus(systemStatus);

            // Add this class as a listener so all status changes can be logged.
            systemStatus.addListener(this);
        }
    }

    /**
     * Clear state of plot panel and AIDA for a new session.
     */
    private void resetPlots() {

        // Clear the static AIDA tree in case plots are hanging around from previous sessions.
        resetAidaTree();

        // Plot frame visible?
        if (!plotWindow.isVisible()) {
            // Turn on plot frame if it is off.
            plotWindow.setVisible(true);
            // plotInfoWindow.setVisible(true);
        }

        // Reset plots.
        plotWindow.reset();
    }

    /**
     * Save the plots to an AIDA output file at the end of the job.
     */
    private void saveAidaFile() {
        // Save final AIDA file if option is selected.
        if (configurationModel.getAidaAutoSave()) {
            log(Level.INFO, "Saving AIDA file <" + configurationModel.getAidaFileName() + ">");
            try {
                AIDA.defaultInstance().saveAs(configurationModel.getAidaFileName());
            } catch (IOException e) {
                errorHandler.setError(e).setMessage("Error saving AIDA file.").log().printStackTrace().showErrorDialog();
            }
        }
    }

    /**
     * Stop the session by killing the event processing thread, ending the job, and disconnecting
     * from the ET system.
     */
    private void stopSession() {
        // Show a modal message window while this method executes.
        JDialog dialog = DialogUtil.showStatusDialog(this, "Info", "Disconnecting from session ...");

        try {
            // Log message.
            logger.log(Level.FINER, "Stopping the session.");

            // Kill the watchdog thread which looks for disconnects, if it is active.
            killSessionWatchdogThread();

            // Automatically write AIDA file from job settings.
            saveAidaFile();

            // Disconnect from ET system, if using the ET server, and set the proper disconnected
            // GUI state.
            disconnect();

            // Stop the event processing, which is called after the ET system goes down to avoid
            // hanging in calls to ET system.
            stopEventProcessing();

            logger.log(Level.INFO, "Session was stopped.");

        } finally {
            // Close modal message window.
            dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
        }
    }

    /**
     * Stop the event processing by executing a <code>STOP</code> command on the record loop and
     * killing the event processing thread. This is executed after the ET system is disconnected so
     * that the event processing does not potentially hang in a call to
     * <code>EtSystem.getEvents()</code> forever.
     */
    private void stopEventProcessing() {

        // Is the event processing thread not null?
        if (processingThread != null) {

            // Is the event processing thread actually still alive?
            if (processingThread.isAlive()) {

                // Request the event processing loop to execute stop.
                loop.execute(Command.STOP);

                try {
                    // This should always work, because the ET system is disconnected before this.
                    processingThread.join();
                } catch (InterruptedException e) {
                    // Don't know when this would ever happen.
                    e.printStackTrace();
                }
            }

            // Notify of last error that occurred in event processing.
            if (loop.getLastError() != null) {
                errorHandler.setError(loop.getLastError()).log().printStackTrace();
            }

            // Set the event processing thread to null as it is unusable now.
            processingThread = null;
        }

        // Set the loop to null as a new one will be created for next session.
        loop = null;
    }

    /**
     * Kill the current session watchdog thread.
     */
    private void killSessionWatchdogThread() {
        // Is the session watchdog thread not null?
        if (sessionWatchdogThread != null) {
            // Is the thread still alive?
            if (sessionWatchdogThread.isAlive()) {
                // Interrupt the thread which should cause it to stop.
                sessionWatchdogThread.interrupt();
                try {
                    // This should always work once the thread is interupted.
                    sessionWatchdogThread.join();
                } catch (InterruptedException e) {
                    // This should never happen.
                    e.printStackTrace();
                }
            }
            // Set the thread object to null.
            sessionWatchdogThread = null;
        }
    }

    /**
     * Thread to automatically trigger a disconnect when the event processing chain finishes or
     * throws a fatal error. This thread joins to the event processing thread and automatically
     * requests a disconnect using an ActionEvent when the event processing thread stops.
     */
    private class SessionWatchdogThread extends Thread {

        public void run() {
            try {
                // When the event processing thread finishes, the session should be stopped and
                // disconnect should occur.
                processingThread.join();

                // Activate a disconnect using the ActionEvent which is used by the disconnect
                // button.
                // FIXME: When this happens the event processing object and its thread don't get set
                // to null!
                actionPerformed(new ActionEvent(Thread.currentThread(), 0, DISCONNECT));

            } catch (InterruptedException e) {
                // This probably just means that the disconnect button was pushed, and this thread
                // should
                // no longer monitor the event processing.
                e.printStackTrace();
            }
        }
    }

    /**
     * Choose an output log file using a <code>JFileChooser</code>.
     */
    private void chooseLogFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Create Log File");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showDialog(this, "Create ...");
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file.exists()) {
                throw new RuntimeException("Log file already exists.");
            } else {
                try {
                    configurationModel.setLogFileName(file.getCanonicalPath());
                    configurationModel.setLogToFile(true);
                } catch (IOException e) {
                    errorHandler.setError(e).log().printStackTrace().showErrorDialog();
                }
                logToFile(file);
            }
        }

    }

    /**
     * Choose an input configuration file using a <code>JFileChooser</code>.
     */
    private void chooseConfigurationFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Settings");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showDialog(mainPanel, "Load ...");
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            log(Level.CONFIG, "Loading settings from file <" + f.getPath() + ">");
            Configuration newConfig = new Configuration(f);
            setConfiguration(newConfig);
            loadConfiguration();
        }
    }

    /**
     * Save a configuration file using a <code>JFileChooser</code>.
     */
    private void saveConfigurationFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Configuration");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(mainPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            log(Level.CONFIG, "Saving configuration to file <" + f.getPath() + ">");
            configuration.writeToFile(f);
        }
    }

    private void updateLayoutConfiguration() {
        if (configurationModel.hasPropertyValue(SAVE_LAYOUT_PROPERTY)) {
            // Should the GUI config be saved?
            if (configurationModel.getSaveLayout()) {
                // Push the current GUI settings into the configuration.
                saveLayoutConfiguration();
            } else {
                // Remove any GUI settings from the configuration.
            clearLayoutConfiguration();
        }
        }
    }

    private void saveLayoutConfiguration() {
        configurationModel.setMonitoringApplicationLayout(new WindowConfiguration(this).toString());
        configurationModel.setSystemStatusFrameLayout(new WindowConfiguration(systemStatusWindow).toString());
        configurationModel.setPlotFrameLayout(new WindowConfiguration(plotWindow).toString());
    }

    private void clearLayoutConfiguration() {
        configurationModel.remove(ConfigurationModel.MONITORING_APPLICATION_LAYOUT_PROPERTY);
        configurationModel.remove(ConfigurationModel.SYSTEM_STATUS_FRAME_LAYOUT_PROPERTY);
        configurationModel.remove(ConfigurationModel.PLOT_FRAME_LAYOUT_PROPERTY);
    }

    private void setSaveLayout() {
        configurationModel.setSaveLayout(saveLayoutItem.isSelected());
    }

    private void restoreDefaultLayout() {
        resetWindowConfiguration();
        plotWindow.resetWindowConfiguration();
        systemStatusWindow.resetWindowConfiguration();
    }
    
    /**
     * Load the current Configuration by updating the ConfigurationModel.
     */
    private void loadConfiguration() {

        // Set the Configuration on the ConfigurationModel which will trigger all the PropertyChangelListeners.
        configurationModel.setConfiguration(configuration);

        // Log that a new configuration was loaded.
        if (configuration.getFile() != null)
            log(Level.CONFIG, "Loaded configuration from file <" + configuration.getFile().getPath() + ">");
        else
            log(Level.CONFIG, "Loaded configuration from resource <" + configuration.getResourcePath() + ">");

    }

    /**
     * Load the default configuration file.
     */
    private void loadDefaultConfigFile() {
        setConfiguration(new Configuration(DEFAULT_CONFIG_RESOURCE));
        loadConfiguration();
    }

    /**
     * Validate the current file source by throwing an IOException if there appears to be a problem
     * with it.
     * @throws IOException if there a problem with the current file source.
     */
    private void validateDataFile() throws IOException {
        DataSourceType dataSourceType = configurationModel.getDataSourceType();
        if (dataSourceType.isFile()) {
            try {
                if (configurationModel.getDataSourcePath() == null)
                    throw new IOException("No data file set.");
                if (configurationModel.getDataSourcePath().equals(""))
                    throw new IOException("Data file has empty path.");
                File file = new File(configurationModel.getDataSourcePath());
                if (!file.exists()) {
                    throw new IOException("File does not exist.");
                }
                if (dataSourceType.equals(DataSourceType.EVIO_FILE)) {
                    try {
                        new EvioReader(file, false, false);
                    } catch (EvioException e) {
                        throw new IOException("Error reading EVIO file.", e);
                    }
                } else if (dataSourceType.equals(DataSourceType.LCIO_FILE)) {
                    new LCIOReader(file);
                }
            } catch (IOException e) {
                throw e;
            }
        } else {
            // This shouldn't really ever happen!
            throw new IOException("No file source was selected.");
        }
    }

    /**
     * This is a thread to validate the current input file. This must be done on a seperate thread,
     * because EVIO files may take a long time to be completely read in using the EvioReader. Also,
     * since the request for file validation comes on the EDT thread, the task must be put onto a
     * seperate thread so that actionPerformed() may exit and not block the EDT from updating the
     * GUI.
     */
    class FileValidationThread extends Thread {
        boolean isFileValid;

        public void run() {
            settingsDialog.setEnabled(false);
            JDialog dialog = DialogUtil.showStatusDialog(MonitoringApplication.this, "Validating data file", configurationModel.getDataSourcePath());
            try {
                validateDataFile();
                DialogUtil.showInfoDialog(MonitoringApplication.this, "File is valid", configurationModel.getDataSourcePath());
            } catch (IOException error) {
                DialogUtil.showErrorDialog(MonitoringApplication.this, error, "Error validating file");
            } finally {
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                settingsDialog.setEnabled(true);
                fileValidationThread = null;
            }
        }
    }

    /**
     * Create an ET server connection from a <code>ConfigurationModel</code>.
     * @param config The ConfigurationModel with the connection parameters.
     * @return The EtConnection object.
     */
    private static EtConnection fromConfigurationModel(ConfigurationModel config) {
        return EtConnection.createConnection(config.getEtName(), config.getHost(), config.getPort(), config.getBlocking(), config.getQueueSize(), config.getPrescale(), config.getStationName(), config.getStationPosition(), config.getWaitMode(), config.getWaitTime(), config.getChunkSize());
    }
   
}
