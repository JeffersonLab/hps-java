package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.MonitoringCommands.AIDA_AUTO_SAVE;
import static org.hps.monitoring.gui.MonitoringCommands.CLEAR_LOG_TABLE;
import static org.hps.monitoring.gui.MonitoringCommands.CONNECT;
import static org.hps.monitoring.gui.MonitoringCommands.DISCONNECT;
import static org.hps.monitoring.gui.MonitoringCommands.EXIT;
import static org.hps.monitoring.gui.MonitoringCommands.LOAD_CONNECTION;
import static org.hps.monitoring.gui.MonitoringCommands.LOAD_JOB_SETTINGS;
import static org.hps.monitoring.gui.MonitoringCommands.LOG_TO_FILE;
import static org.hps.monitoring.gui.MonitoringCommands.LOG_TO_TERMINAL;
import static org.hps.monitoring.gui.MonitoringCommands.NEXT;
import static org.hps.monitoring.gui.MonitoringCommands.PAUSE;
import static org.hps.monitoring.gui.MonitoringCommands.RESET_CONNECTION_SETTINGS;
import static org.hps.monitoring.gui.MonitoringCommands.RESET_JOB_SETTINGS;
import static org.hps.monitoring.gui.MonitoringCommands.RESUME;
import static org.hps.monitoring.gui.MonitoringCommands.SAVE_CONNECTION;
import static org.hps.monitoring.gui.MonitoringCommands.SAVE_JOB_SETTINGS;
import static org.hps.monitoring.gui.MonitoringCommands.SAVE_LOG_TABLE;
import static org.hps.monitoring.gui.MonitoringCommands.SAVE_PLOTS;
import static org.hps.monitoring.gui.MonitoringCommands.SCREENSHOT;
import static org.hps.monitoring.gui.MonitoringCommands.SET_EVENT_BUILDER;
import static org.hps.monitoring.gui.MonitoringCommands.SET_LOG_LEVEL;
import static org.hps.monitoring.gui.MonitoringCommands.SET_STEERING_FILE;
import static org.hps.monitoring.gui.MonitoringCommands.SET_STEERING_RESOURCE;
import static org.hps.monitoring.gui.MonitoringCommands.SHOW_SETTINGS;

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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
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

import org.hps.evio.LCSimEventBuilder;
import org.hps.monitoring.gui.DataSourcePanel.DataSourceType;
import org.hps.monitoring.plotting.MonitoringAnalysisFactory;
import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.record.EventProcessingChain;
import org.hps.monitoring.record.EventProcessingThread;
import org.hps.monitoring.record.etevent.EtConnection;
import org.hps.monitoring.record.etevent.EtConnectionParameters;
import org.hps.monitoring.record.etevent.EtEventSource;
import org.hps.monitoring.record.evio.EvioFileSource;
import org.hps.monitoring.subsys.et.EtSystemStripCharts;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.loop.LCIOEventSource;

/**
 * Monitoring application for HPS, which can run LCSim steering files on data converted
 * from the ET server.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: MonitoringApplication.java,v 1.61 2013/12/10 07:36:40 jeremy Exp $
 */
// TODO: Test all application error handling!
// TODO: Review GUI size settings.
// TODO: Add handler for uncaught exceptions.
// TODO: Add back option to continue if event processing errors occur.  
//       Fatal errors like the ET system being down should still cause an automatic disconnect.
// FIXME: Review use of the watchdog thread for automatic disconnect.  It may be overcomplicated.
public class MonitoringApplication extends JFrame implements ActionListener {

    // Top-level Swing components.
    private JPanel mainPanel;
    private EventButtonsPanel buttonsPanel;
    private ConnectionStatusPanel connectionStatusPanel;
    private RunPanel runPanel;
    private JMenuBar menuBar;
    private SettingsDialog settingsDialog;
    private PlotFrame plotFrame;

    // References to menu items that will be toggled depending on application state.
    private JMenuItem connectItem;
    private JMenuItem disconnectItem;
    private JMenuItem resetConnectionItem;
    private JMenuItem connectionLoadItem;
    private JMenuItem savePlotsItem;
    private JMenuItem logItem;
    private JMenuItem terminalItem;
    private JMenuItem steeringItem;
    private JMenuItem aidaAutoSaveItem;
    private JMenuItem loadJobSettingsItem;
    private JMenuItem resetJobSettingsItem;

    // Saved references to System.out and System.err in case need to reset.
    private final PrintStream sysOut = System.out;
    private final PrintStream sysErr = System.err;
    
    // Error handling class for the application.
    ErrorHandler errorHandler;

    // ET connection parameters and state.
    private EtConnectionParameters connectionParameters;
    private EtConnection connection;
    private int connectionStatus = ConnectionStatus.DISCONNECTED;

    // Event processing objects.
    private JobControlManager jobManager;
    private LCSimEventBuilder eventBuilder;
    private EventProcessingChain eventProcessing;
    private EventProcessingThread eventProcessingThread;
    private Thread sessionWatchdogThread;

    // Logging objects.
    private static Logger logger;
    private Handler logHandler;
    private DefaultTableModel logTableModel;
    static final String[] logTableColumns = { "Message", "Date", "Level" };
    private JTable logTable;
    private static Level DEFAULT_LOG_LEVEL = Level.INFO;

    // Format for screenshots.  
    // FIXME: This is hard-coded to PNG format.
    private static final String screenshotFormat = "png";

    // Format of date field for log.
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");

    // GUI size settings.
    private static final int SCREEN_WIDTH = ScreenUtil.getScreenWidth();
    private static final int SCREEN_HEIGHT = ScreenUtil.getScreenHeight();
    private final static int LOG_TABLE_WIDTH = 700; // FIXME: Should be set from main panel width.
    private final static int LOG_TABLE_HEIGHT = 270;
    private static final int MAIN_FRAME_HEIGHT = 450;
    private static final int MAIN_FRAME_WIDTH = 650;
               
    /**
     * Constructor for the monitoring application.
     */
    public MonitoringApplication() {
        
        // Create and configure the logger.
        setupLogger();
        
        // Setup the error handling class.
        setupErrorHandler();
        
        // Setup an uncaught exception handler.
        setupExceptionHandler();

        // Setup the application menus.
        createApplicationMenu();

        // Create the main GUI panel.
        createMainPanel();

        // Create the log table GUI component.
        createLogTable();

        // Configuration of window for showing plots.
        createPlotFrame();

        // Setup AIDA.
        setupAida();

        // Configure the application's primary JFrame.
        configApplicationFrame();

        // Create settings window.
        createSettingsDialog();

        // Log that the application started successfully.
        log("Application initialized successfully.");        
    }

    private void setupErrorHandler() {
        errorHandler = new ErrorHandler(this, logger);
    }
    
    private void setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {            
            public void uncaughtException(Thread thread, Throwable exception) {
               MonitoringApplication.this.errorHandler.setError(exception).log().printStackTrace().showErrorDialog();
            }
        });
    }
            
    private void createSettingsDialog() {
        settingsDialog = new SettingsDialog();
        getJobPanel().addActionListener(this);
    }

    private void createPlotFrame() {
        plotFrame = new PlotFrame();                
        plotFrame.setSize(SCREEN_WIDTH - MAIN_FRAME_WIDTH, SCREEN_HEIGHT);
        plotFrame.setLocation(
                (int)(ScreenUtil.getBoundsX(0)) + MAIN_FRAME_WIDTH,
                plotFrame.getY());
    }
    
    public void setVisible(boolean visible) {
        super.setVisible(true);
        
        // FIXME: If this is done earlier before app is visible, the GUI will fail to show!
        this.connectionStatusPanel.setStatus(ConnectionStatus.DISCONNECTED);
    }

    /**
     * Configure the AIDA plotting backend.
     */
    private void setupAida() {
        MonitoringAnalysisFactory.register();
        MonitoringAnalysisFactory.configure();
        MonitoringPlotFactory.setRootPane(this.plotFrame.getPlotPane());
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
        runPanel = new RunPanel();
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
    private void createApplicationMenu() {

        menuBar = new JMenuBar();

        JMenu applicationMenu = new JMenu("Application");
        applicationMenu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(applicationMenu);
        addMenuItem("Settings...", KeyEvent.VK_S, SHOW_SETTINGS, true, "Monitoring Application settings", applicationMenu);
        addMenuItem("Exit", KeyEvent.VK_X, EXIT, true, "Exit from the application.", applicationMenu);

        JMenu connectionMenu = new JMenu("Connection");
        connectionMenu.setMnemonic(KeyEvent.VK_C);
        menuBar.add(connectionMenu);

        connectItem = addMenuItem("Connect", KeyEvent.VK_C, CONNECT, true, "Connect to ET system using parameters from connection panel.", connectionMenu);
        disconnectItem = addMenuItem("Disconnect", KeyEvent.VK_D, DISCONNECT, false, "Disconnect from the current ET session.", connectionMenu);
        resetConnectionItem = addMenuItem("Reset Connection Settings", KeyEvent.VK_R, RESET_CONNECTION_SETTINGS, true, "Reset connection settings to defaults.", connectionMenu);
        connectionLoadItem = addMenuItem("Load Connection...", KeyEvent.VK_L, LOAD_CONNECTION, true, "Load connection settings from a saved properties file.", connectionMenu);
        addMenuItem("Save Connection...", KeyEvent.VK_S, SAVE_CONNECTION, true, "Save connection settings to a properties file.", connectionMenu);

        JMenu jobMenu = new JMenu("Job");
        jobMenu.setMnemonic(KeyEvent.VK_J);
        menuBar.add(jobMenu);

        addMenuItem("Save Job Settings...", KeyEvent.VK_J, SAVE_JOB_SETTINGS, true, "Save Job Settings configuration to a properties file.", jobMenu);

        // FIXME: Rest of these should be converted to use the addMenuItem() helper method ...

        loadJobSettingsItem = new JMenuItem("Load Job Settings...");
        loadJobSettingsItem.setMnemonic(KeyEvent.VK_L);
        loadJobSettingsItem.setActionCommand(LOAD_JOB_SETTINGS);
        loadJobSettingsItem.addActionListener(this);
        loadJobSettingsItem.setToolTipText("Load Job Settings from a properties file.");
        jobMenu.add(loadJobSettingsItem);

        resetJobSettingsItem = new JMenuItem("Reset Job Settings");
        resetJobSettingsItem.setMnemonic(KeyEvent.VK_R);
        resetJobSettingsItem.setActionCommand(RESET_JOB_SETTINGS);
        resetJobSettingsItem.addActionListener(this);
        resetJobSettingsItem.setToolTipText("Reset Job Settings to the defaults.");
        jobMenu.add(resetJobSettingsItem);

        steeringItem = new JMenuItem("Set Steering File...");
        steeringItem.setMnemonic(KeyEvent.VK_S);
        steeringItem.setActionCommand(SET_STEERING_FILE);
        steeringItem.addActionListener(this);
        steeringItem.setToolTipText("Set the job's LCSim steering file.");
        jobMenu.add(steeringItem);

        aidaAutoSaveItem = new JMenuItem("AIDA Auto Save File...");
        aidaAutoSaveItem.setMnemonic(KeyEvent.VK_A);
        aidaAutoSaveItem.setActionCommand(AIDA_AUTO_SAVE);
        aidaAutoSaveItem.addActionListener(this);
        aidaAutoSaveItem.setToolTipText("Select name of file to auto save AIDA plots at end of job.");
        jobMenu.add(aidaAutoSaveItem);

        savePlotsItem = new JMenuItem("Save Plots to AIDA File...");
        savePlotsItem.setMnemonic(KeyEvent.VK_P);
        savePlotsItem.setActionCommand(SAVE_PLOTS);
        savePlotsItem.addActionListener(this);
        savePlotsItem.setEnabled(false);
        savePlotsItem.setToolTipText("Save plots from default AIDA tree to an output file.");
        jobMenu.add(savePlotsItem);

        logItem = new JMenuItem("Redirect to File...");
        logItem.setMnemonic(KeyEvent.VK_F);
        logItem.setActionCommand(LOG_TO_FILE);
        logItem.addActionListener(this);
        logItem.setEnabled(true);
        logItem.setToolTipText("Redirect job's standard out and err to a file.");
        jobMenu.add(logItem);

        terminalItem = new JMenuItem("Redirect to Terminal");
        terminalItem.setMnemonic(KeyEvent.VK_T);
        terminalItem.setActionCommand(LOG_TO_TERMINAL);
        terminalItem.addActionListener(this);
        terminalItem.setEnabled(false);
        terminalItem.setToolTipText("Redirect job's standard out and err back to the terminal.");
        jobMenu.add(terminalItem);

        JMenuItem screenshotItem = new JMenuItem("Take a screenshot...");
        screenshotItem.setMnemonic(KeyEvent.VK_N);
        screenshotItem.setActionCommand(SCREENSHOT);
        screenshotItem.addActionListener(this);
        screenshotItem.setToolTipText("Save a full screenshot to a " + screenshotFormat + " file.");
        jobMenu.add(screenshotItem);

        JMenu logMenu = new JMenu("Log");
        jobMenu.setMnemonic(KeyEvent.VK_L);
        menuBar.add(logMenu);

        JMenuItem saveLogItem = new JMenuItem("Save log to file...");
        saveLogItem.setMnemonic(KeyEvent.VK_S);
        saveLogItem.setActionCommand(SAVE_LOG_TABLE);
        saveLogItem.addActionListener(this);
        saveLogItem.setToolTipText("Save the log records to a tab delimited text file.");
        logMenu.add(saveLogItem);

        addMenuItem("Clear log", KeyEvent.VK_C, CLEAR_LOG_TABLE, true, "Clear the log table of all messages.", logMenu);
    }

    /**
     * Add a menu item.
     * @param label The label.
     * @param mnemonic The single letter shortcut.
     * @param cmd The command.
     * @param enabled Whether it is enabled.
     * @param tooltip The tooltip text.
     * @param menu The parent menu to which it should be added.
     * @return The created menu item.
     */
    private JMenuItem addMenuItem(String label, int mnemonic, String cmd, boolean enabled, String tooltip, JMenu menu) {
        JMenuItem item = new JMenuItem(label);
        item.setMnemonic(mnemonic);
        item.setActionCommand(cmd);
        item.setEnabled(enabled);
        item.setToolTipText(tooltip);
        item.addActionListener(this);
        menu.add(item);
        return item;
    }

    /**
     * Log handler for inserting messages into the log table.
     */
    private class MonitoringApplicationLogHandler extends Handler {

        /**
         * Puts log messages into the application's log table GUI component.
         */
        public void publish(LogRecord record) {
            Object[] row = new Object[] { /* record.getLoggerName(), */ // source
                    record.getMessage(), // message
                    dateFormat.format(new Date(record.getMillis())), // date
                    record.getLevel() }; // level
            logTableModel.insertRow(logTable.getRowCount(), row);
        }

        public void close() throws SecurityException {
        }

        public void flush() {
        }
    }

    /**
     * Creates the application's log table GUI component, which is a JTable containing messages
     * from the logger.
     */
    private void createLogTable() {

        String data[][] = new String[0][0];
        logTableModel = new DefaultTableModel(data, logTableColumns);
        logTable = new JTable(logTableModel);
        logTable.setEnabled(false);

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
     * Load connection settings from a file.
     * @param file The properties file.
     */
    public void loadConnectionSettings(File file) {
        getConnectionPanel().loadPropertiesFile(file);
    }

    /**
     * Load job settings from a file.
     * @param file The properties file.
     */
    public void loadJobSettings(File file) {
        try {
            getJobPanel().setJobSettings(new JobSettings(file));
            // Need to check here if System.out and err have been redirected.
            if (getJobPanel().logToFile()) {
                redirectStdOutAndErrToFile(new File(getJobPanel().getLogFileName()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The action handler method for the entire application.
     * @param e The event to handle.
     */
    public void actionPerformed(ActionEvent e) {
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
        } else if (LOG_TO_FILE.equals(cmd)) {
            logToFile();
        } else if (LOG_TO_TERMINAL.equals(cmd)) {
            logToTerminal();
        } else if (SCREENSHOT.equals(cmd)) {
            chooseScreenshot();
        } else if (EXIT.equals(cmd)) {
            exit();
        } else if (SAVE_CONNECTION.equals(cmd)) {
            saveConnection();
        } else if (LOAD_CONNECTION.equals(cmd)) {
            getConnectionPanel().load();
        } else if (RESET_CONNECTION_SETTINGS.equals(cmd)) {
            getConnectionPanel().reset();
        } else if (SAVE_LOG_TABLE.equals(cmd)) {
            saveLogToFile();
        } else if (CLEAR_LOG_TABLE.equals(cmd)) {
            clearLog();
        } else if (SET_EVENT_BUILDER.equals(cmd)) {
            getJobPanel().editEventBuilder();
        } else if (PAUSE.equals(cmd)) {
            pauseEventProcessing();
        } else if (NEXT.equals(cmd)) {
            nextEvent();
        } else if (RESUME.equals(cmd)) {
            resumeEventProcessing();
        } else if (SET_LOG_LEVEL.equals(cmd)) {
            setLogLevel();
        } else if (AIDA_AUTO_SAVE.equals(cmd)) {
            getJobPanel().chooseAidaAutoSaveFile();
        } else if (SAVE_JOB_SETTINGS.equals(cmd)) {
            saveJobSettings();
        } else if (LOAD_JOB_SETTINGS.equals(cmd)) {
            loadJobSettings();
        } else if (RESET_JOB_SETTINGS.equals(cmd)) {
            resetJobSettings();
        } else if (SET_STEERING_RESOURCE.equals(cmd)) {
            steeringResourceSelected();
        } else if (SET_STEERING_FILE.equals(cmd)) {
            selectSteeringFile();
        } else if (SHOW_SETTINGS.equals(cmd)) {
            showSettingsWindow();
        }
    }

    /**
     * Show the settings window.
     */
    private void showSettingsWindow() {
        settingsDialog.setVisible(true);
    }

    /**
     * This fires when a steering resource file is selected from the combo box. The Job Settings
     * are changed to use a resource type.
     */
    private void steeringResourceSelected() {
        getJobPanel().setSteeringType(JobPanel.RESOURCE);
    }

    /**
     * Save the job settings to a selected file.
     */
    private void saveJobSettings() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Job Settings");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(mainPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            JobSettings settings = getJobPanel().getJobSettings();
            try {
                settings.save(f);
                log(Level.INFO, "Saved Job Settings to properties file < " + f.getPath() + " >");
            } catch (IOException e) {                
                errorHandler.setError(e)
                    .printStackTrace()
                    .log()
                    .showErrorDialog();
            }
        }
    }

    /**
     * Load job settings from a selected file.
     */
    private void loadJobSettings() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Job Settings");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showOpenDialog(mainPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                getJobPanel().setJobSettings(new JobSettings(f));
                log(Level.INFO, "Loaded Job Settings from file < " + f.getPath() + " >");
            } catch (IOException e) {
                errorHandler.setError(e)
                    .printStackTrace()
                    .log()
                    .showErrorDialog();
            }
        }
    }

    /**
     * Save the connection settings to a properties file using a file chooser.
     */
    void saveConnection() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            getConnectionPanel().writePropertiesFile(file);
            log(Level.INFO, "Saved connection properties to file < " + file.getPath() + " >");
        }
    }

    /**
     * Reset the job settings to the defaults.
     */
    private void resetJobSettings() {
        getJobPanel().resetJobSettings();
        // Redirect System.out and err back to the terminal.
        logToTerminal();
    }

    /**
     * Set a new log level for the application and also forward to the event processor.
     */
    private void setLogLevel() {
        Level newLevel = getJobPanel().getLogLevel();
        logger.setLevel(newLevel);
        log(Level.INFO, "Log Level was changed to < " + getJobPanel().getLogLevel().toString() + " >");
    }

    /**
     * Set the connection status.
     * @param status The connection status.
     */
    private void setConnectionStatus(int status) {
        connectionStatus = status;
        connectionStatusPanel.setStatus(status);
        log(Level.FINE, "Connection status changed to < " + ConnectionStatus.toString(status) + " >");
        logHandler.flush();
    }

    /**
     * Setup the primary <code>JFrame</code> for the application.
     */
    private void configApplicationFrame() {
        mainPanel.setOpaque(true);
        setTitle(getApplicationTitle());
        setContentPane(mainPanel);
        setJMenuBar(menuBar);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(MAIN_FRAME_WIDTH, MAIN_FRAME_HEIGHT));
        setMinimumSize(new Dimension(MAIN_FRAME_WIDTH, MAIN_FRAME_HEIGHT));        
        setResizable(true);        
        setLocation((int)ScreenUtil.getBoundsX(0), getY());
        pack();              
    }
        
    /**
     * Save plots to a selected output file using a file chooser.
     */
    private void savePlots() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showSaveDialog(mainPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File fileName = fc.getSelectedFile();
            try {
                AIDA.defaultInstance().saveAs(fileName);
                logger.log(Level.INFO, "Plots saved to file < " + fileName + " >");
            } catch (IOException e) {
                errorHandler.setError(e)
                    .setMessage("Error saving plots to file.")
                    .printStackTrace()
                    .log()
                    .showErrorDialog();
            } 
        }
    }

    /**
     * Select an LCSim steering file using a file chooser.
     */
    private void selectSteeringFile() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showOpenDialog(mainPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File fileName = fc.getSelectedFile();
            try {
                (new JobControlManager()).setup(fileName);
                getJobPanel().setSteeringFile(fileName.getPath());
                getJobPanel().setSteeringType(JobPanel.FILE);
                log("Steering file set to < " + fileName.getPath() + " >");
            } catch (Exception e) {
                errorHandler.setError(e)
                    .setMessage("Error reading steering file.")
                    .printStackTrace()
                    .log()
                    .showErrorDialog();
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
     * Redirect System.out and System.err to a file. This can be used to capture lengthy
     * debug output from event processing into a file.  Messages sent to the <code>Logger</code> 
     * are unaffected and will still appear in the log table.
     */
    private void logToFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Log File");
        fc.setCurrentDirectory(new File("."));
        int fcs = fc.showSaveDialog(mainPanel);
        if (fcs == JFileChooser.APPROVE_OPTION) {
            final File logFile = fc.getSelectedFile();
            if (logFile.exists()) {
                JOptionPane.showMessageDialog(this, "Log file already exists.");
            } else {
                try {
                    if (!logFile.createNewFile()) {
                        throw new IOException();
                    }
                    redirectStdOutAndErrToFile(logFile);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getJobPanel().setLogToFile(true);
                            getJobPanel().setLogFile(logFile.getPath());
                            terminalItem.setEnabled(true);
                            logItem.setEnabled(false);
                        }
                    });

                    log("Redirected System output to file < " + logFile.getPath() + " >");
                } catch (IOException e) {
                    errorHandler.setError(e)
                        .setMessage("Error redirecting System.out to file.")
                        .log()
                        .printStackTrace()
                        .showErrorDialog();
                }
            }
        }
    }

    /**
     * Redirect <code>System.out</code> and <code>System.err</code> to a file.
     * @param file The output log file.
     * @throws FileNotFoundException if the file does not exist.
     */
    private void redirectStdOutAndErrToFile(File file) throws FileNotFoundException {
        PrintStream ps = new PrintStream(new FileOutputStream(file.getPath()));
        System.setOut(ps);
        System.setErr(ps);
    }

    /**
     * Redirect <code>System.out</code> and <code>System.err</code> back to the terminal, e.g. if
     * they were previously sent to a file. This is independent of messages that are sent to the
     * application's log table.
     */
    private void logToTerminal() {
        System.setOut(sysOut);
        System.setErr(sysErr);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getJobPanel().setLogFile("");
                getJobPanel().setLogToFile(false);
                terminalItem.setEnabled(false);
                logItem.setEnabled(true);
            }
        });
        log(Level.INFO, "Redirected print output to terminal.");
    }

    /**
     * Set the GUI state to disconnected, which will enable/disable applicable GUI components and
     * menu items.
     */
    private void setDisconnectedGuiState() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // Enable or disable appropriate menu items.
                connectItem.setEnabled(true);
                disconnectItem.setEnabled(false);
                resetConnectionItem.setEnabled(true);
                connectionLoadItem.setEnabled(true);
                savePlotsItem.setEnabled(false);
                logItem.setEnabled(true);
                terminalItem.setEnabled(true);
                steeringItem.setEnabled(true);

                // Re-enable the ConnectionPanel.
                getConnectionPanel().enableConnectionPanel(true);

                // Re-enable the getJobPanel().
                getJobPanel().enableJobPanel(true);

                // Set relevant event panel buttons to disabled.
                buttonsPanel.enablePauseButton(false);
                buttonsPanel.enableNextEventsButton(false);

                // Toggle connection button to proper setting.
                buttonsPanel.toggleConnectButton();
            }
        });
    }

    /**
     * Set the GUI to connected state, which will enable/disable appropriate components and menu items.
     */
    private void setConnectedGuiState() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // Disable connection panel.
                getConnectionPanel().enableConnectionPanel(false);

                // Disable getJobPanel().
                getJobPanel().enableJobPanel(false);

                // Enable or disable appropriate menu items.
                connectItem.setEnabled(false);
                disconnectItem.setEnabled(true);
                resetConnectionItem.setEnabled(false);
                connectionLoadItem.setEnabled(false);
                savePlotsItem.setEnabled(true);
                logItem.setEnabled(false);
                terminalItem.setEnabled(false);
                steeringItem.setEnabled(false);

                // Enable relevant event panel buttons.
                buttonsPanel.enablePauseButton(true);
                buttonsPanel.setPauseModeState(getJobPanel().pauseMode());

                // Toggle connection button to proper settings.
                buttonsPanel.toggleConnectButton();
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
        if (plotFrame.isVisible())
            plotFrame.setVisible(false);
        this.setVisible(false);
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
            if ((extIndex == -1) || !(fileName.substring(extIndex + 1, fileName.length())).toLowerCase().equals(screenshotFormat)) {
                fileName = fileName + "." + screenshotFormat;
            }
            takeScreenshot(fileName);
            log(Level.INFO, "Screenshot saved to file < " + fileName + " >");
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
            ImageIO.write(image, screenshotFormat, new File(fileName));
        } catch (Exception e) {
            errorHandler.setError(e)
                .setMessage("Failed to take screenshot.")
                .printStackTrace()
                .log()
                .showErrorDialog();
        }
    }

    /**
     * Get the fully qualified class name of the current event builder 
     * that will be used to convert EVIO to LCIO.
     * @return The class name of the event builder.
     */
    private String getEventBuilderClassName() {
        return getJobPanel().getEventBuilderClassName();
    }

    /**
     * Get the type of steering file being used.
     * @return The type of the steering file.
     */
    private int getSteeringType() {
        return getJobPanel().getSteeringType();
    }

    /**
     * Start a new monitoring session.  This is executed in a separate thread from the EDT
     * in order to not block the GUI from updating.
     */
    private void startSession() {

        log(Level.FINE, "Starting a new monitoring session.");
        
        // Show a modal window that will block the GUI until connected or an error occurs.
        JDialog dialog = showStatusDialog("Info", "Starting new session ...", true);
        
        try {
                        
            // Reset the plot panel and global AIDA state.
            resetPlots();

            // Setup the LCSim JobControlManager and event builder.
            setupLCSim();
            
            // Using an ET server for this session?
            if (usingEtServer())
                // Connect to the ET system, which will create a valid EtConnection object.
                connect();            
            else
                // Using a file source so just set correct GUI state.
                this.setConnectedGuiState();

            // Setup the EventProcessingChain object using the EtConnection.
            setupEventProcessingChain();

            // Start thread which will trigger a disconnect if the event processing thread
            // finishes.
            startSessionWatchdogThread();

            log(Level.INFO, "Successfully started the monitoring session.");

            // Close modal window.
            dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));

        } catch (Exception e) {
            
            // Close modal window.
            dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
            
            // Handle the error that occurred.
            errorHandler.setError(e)
                .printStackTrace()
                .log();
            
            // Disconnect from the session.
            // FIXME: Is this okay when ET is not being used e.g. for direct file streaming?
            disconnect(ConnectionStatus.ERROR);
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

        log(Level.FINE, "Connecting to ET system.");

        setConnectionStatus(ConnectionStatus.CONNECTION_REQUESTED);

        // Make sure applicable menu items are enabled or disabled.
        setConnectedGuiState();

        // Create a connection to the ET server.
        try {
            createEtConnection();
            log(Level.INFO, "Successfully connected to ET system.");
        } catch (Exception e) {
            //log(e.getMessage());
            throw new IOException(e);
        }
    }

    /**
     * Get the steering parameter, which is either a file path or resource string.
     * @return The steering parameter.
     */
    private String getSteering() {
        return getJobPanel().getSteering();
    }

    /**
     * Get the name of the detector for conditions data.
     * @return The name of the detector.
     */
    private String getDetectorName() {
        return getJobPanel().getDetectorName();
    }

    /**
     * Get the connection parameter settings from the connection panel.
     * @return The connection parameters.
     */
    private EtConnectionParameters getConnectionParameters() {
        return getConnectionPanel().getConnectionParameters();
    }
    
    private ConnectionPanel getConnectionPanel() {
        return settingsDialog.getSettingsPanel().getConnectionPanel();
    }

    private JobPanel getJobPanel() {
        return settingsDialog.getSettingsPanel().getJobPanel();
    }
    
    private DataSourcePanel getDataSourcePanel() {
        return settingsDialog.getSettingsPanel().getDataSourcePanel();
    }

    /**
     * Get whether errors in event processing will cause automatic disconnect.
     * @return True if disconnect on event processing error; false to continue.
     */
    //private boolean disconnectOnError() {
    //    return getJobPanel().disconnectOnError();
    //}

    private void disconnect() {
        disconnect(ConnectionStatus.DISCONNECTING);
    }

    /**
     * Disconnect from the current ET session with a particular status.
     * @param status The connection status.
     */
    private void disconnect(int status) {

        log(Level.FINE, "Disconnecting from the ET server.");

        // Cleanup the ET connection.
        cleanupEtConnection();

        // Update state of GUI to disconnected.
        setDisconnectedGuiState();

        // Finally, change application state to fully disconnected.
        setConnectionStatus(ConnectionStatus.DISCONNECTED);

        // Set the application status from the caller if an error had occurred.
        if (status == ConnectionStatus.ERROR)
            setConnectionStatus(status);

        log(Level.INFO, "Disconnected from the ET server.");
    }

    /**
     * Cleanup the ET connection.
     */
    private void cleanupEtConnection() {

        if (connection != null) {
            connection.cleanup();
            connection = null;
        }
    }

    /**
     * Setup the LCSim job manager and the event builder.
     */
    private void setupLCSim() {

        log(Level.INFO, "Setting up LCSim.");

        // Get steering resource or file as a String parameter.
        String steering = getSteering();
        int steeringType = getJobPanel().getSteeringType();
        log(Level.CONFIG, "Set LCSim steering to < " + steering + " > with type < " + (steeringType == JobPanel.RESOURCE ? "RESOURCE" : "FILE") + " >");

        // Check if the LCSim steering file looks valid.
        try {
            getJobPanel().checkSteering();
        } catch (IOException e) {
            errorHandler.setError(e)
                .log()
                .printStackTrace()
                .raiseException();
        }

        try {
            // Create job manager and configure.
            jobManager = new JobControlManager();
            jobManager.setPerformDryRun(true);
            if (steeringType == JobPanel.RESOURCE) {
                log(Level.CONFIG, "Setting up steering resource < " + steering + " >");
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(steering);
                jobManager.setup(is);
                is.close();
            } else if (getSteeringType() == JobPanel.FILE) {
                log(Level.CONFIG, "Setting up steering file < " + steering + " >");
                jobManager.setup(new File(steering));
            }

            // Setup the event builder to translate from EVIO to LCIO.
            createEventBuilder();

        } catch (Exception e) {
            // Catch all other setup exceptions and re-throw them as RuntimeExceptions.
            errorHandler.setError(e)
                .setMessage("Failed to setup LCSim.")
                .log()
                .printStackTrace()
                .raiseException();
        }

        log(Level.INFO, "LCSim setup was successful.");
    }

    /**
     * Create the event builder for converting EVIO events to LCSim.
     */
    private void createEventBuilder() {

        // Setup the EventBuilder class.
        String eventBuilderClassName = getEventBuilderClassName();

        log(Level.FINE, "Initializing event builder < " + eventBuilderClassName + " >");

        try {
            eventBuilder = (LCSimEventBuilder) Class.forName(eventBuilderClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LCSimEventBuilder class.", e);
        }

        // Set the detector name on the event builder so it can find conditions data.
        eventBuilder.setDetectorName(getDetectorName());

        log(Level.INFO, "Successfully initialized event builder < " + eventBuilderClassName + " >");
    }

    /**
     * Create a connection to an ET system using current parameters from the GUI. If successful,
     * the application's ConnectionStatus is changed to CONNECTED.
     */
    private void createEtConnection() {

        // Cache connection parameters from GUI to local variable.
        connectionParameters = getConnectionParameters();

        // Setup connection to ET system.
        connection = EtConnection.createEtConnection(connectionParameters);

        if (connection != null) {

            // Set status to connected as there is now a live ET connection.
            setConnectionStatus(ConnectionStatus.CONNECTED);

            log(Level.CONFIG, "Created ET connection < " + connectionParameters.getBufferName() + " >");
        } else {
            // Some error occurred and the connection is not valid.
            setConnectionStatus(ConnectionStatus.ERROR);
            
            errorHandler.setError(new RuntimeException("Failed to create ET connection"))
                .log()
                .printStackTrace()
                .raiseException();
        }
    }

    /**
     * Save the log table to a tab-delimited text file selected by a file chooser.
     */
    private void saveLogToFile() {
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
                    log("Saved log to file < " + logFile.getPath() + " >");
                } catch (IOException e) {                    
                    errorHandler.setError(e)
                        .setMessage("Error saving log to file.")
                        .log()
                        .printStackTrace()
                        .showErrorDialog();
                }
            }
        }
    }

    /**
     * Clear all data from the log table.
     */
    private void clearLog() {
        logTableModel.setRowCount(0);
        log(Level.INFO, "Log table was cleared.");
    }

    /**
     * True if connected to ET system.
     * @return True if connected to ET system.
     */
    private boolean connected() {
        return connectionStatus == ConnectionStatus.CONNECTED;
    }

    /**
     * Notify event processor to get next set of events, if in pause mode.
     */
    private void nextEvent() {
        eventProcessing.next();
        log(Level.FINEST, "Getting next event.");
    }

    /**
     * Notify the event processor to resume processing events, if paused.
     */
    private void resumeEventProcessing() {
        // Notify event processor to continue.
        eventProcessing.resume();

        // Set state of event buttons.
        buttonsPanel.setPauseModeState(false);

        // Toggle job panel setting.
        getJobPanel().enablePauseMode(false);

        log(Level.FINE, "Resuming event processing after pause.");
    }

    /**
     * Notify the event processor to start pause mode, which will pause between events.
     */
    private void pauseEventProcessing() {
        //if (connected()) {
            // Pause event processing.
        eventProcessing.pause();

        // Set GUI state.
        buttonsPanel.setPauseModeState(true);
        getJobPanel().enablePauseMode(false);

        log(Level.FINE, "Event processing was paused.");
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
     * Setup the <tt>EventProcessingChain</tt> object and create a <tt>Thread</tt> for running it.
     * The processing is not started by this method.
     */
    private void setupEventProcessingChain() {
        
        // Initialize the event processing chain object.
        eventProcessing = new EventProcessingChain();                
        
        // Configure the data source, e.g. ET, EVIO or LCIO.
        configDataSource();        
        
        // Set the event builder.
        eventProcessing.setEventBuilder(eventBuilder);
        
        // Set the detector name for LCSim conditions system.
        eventProcessing.setDetectorName(getDetectorName());
        
        // Get a list of Drivers to execute from the JobManager.
        eventProcessing.add(jobManager.getDriverExecList());
        
        // Using an ET server or an EVIO file?
        if (usingEtServer() || usingEvioFile())
            // ET or EVIO source can be used for updating the RunPanel.
            eventProcessing.add(runPanel.new EvioUpdater());
        else
            // Direct LCIO source so must use a more generic updater for the RunPanel.
            eventProcessing.add(runPanel.new CompositeRecordUpdater());
        
        // Using an ET server?
        if (usingEtServer())
            // Add ET system strip charts.
            eventProcessing.add(new EtSystemStripCharts());
        
        // Setup the event processing based on the above configuration.
        eventProcessing.setup();
        
        // Create the event processing thread.
        eventProcessingThread = new EventProcessingThread(eventProcessing);
        
        // Start the event processing thread.
        eventProcessingThread.start();
    }
    
    private boolean usingEtServer() {
        return this.getDataSourcePanel().getDataSourceType().equals(DataSourceType.ET_SERVER);
    }
    
    private boolean usingEvioFile() {
        return this.getDataSourcePanel().getDataSourceType().equals(DataSourceType.EVIO_FILE);
    }
    
    private void configDataSource() {
        DataSourceType dataSourceType = getDataSourcePanel().getDataSourceType();
        String filePath = getDataSourcePanel().getFilePath();
        log(Level.INFO, "Data source type < " + dataSourceType + " >");
        if (dataSourceType.equals(DataSourceType.ET_SERVER)) {
            eventProcessing.setRecordSource(new EtEventSource(connection));
            log(Level.FINE, "ET event source");
        } else if (dataSourceType.equals(DataSourceType.EVIO_FILE)){
            eventProcessing.setRecordSource(new EvioFileSource(new File(filePath)));
            log(Level.FINE, "EVIO file <" + filePath + " >");
        } else if (dataSourceType.equals(DataSourceType.LCIO_FILE)) {
            try {
                eventProcessing.setRecordSource(new LCIOEventSource(new File(filePath)));
                log(Level.FINE, "LCIO file < " + filePath + " >");
            } catch (IOException e) {
                errorHandler.setError(e)
                    .log()
                    .printStackTrace()
                    .raiseException();
            }
        }
    }

    /**
     * Clear state of plot panel and AIDA for a new session.
     */
    private void resetPlots() {

        // Clear the static AIDA tree in case plots are hanging around from previous sessions.
        resetAidaTree();

        // Plot frame visible?
        if (!plotFrame.isVisible())
            // Turn on plot frame if it is off.
            plotFrame.setVisible(true);
        else
            // Reset plots.
            plotFrame.reset();
    }

    /**
     * End the current job.
     */
    private void saveAidaFile() {

        // Save final AIDA file if option is selected.
        if (getJobPanel().isAidaAutoSaveEnabled()) {
            log(Level.INFO, "Saving AIDA file < " + getJobPanel().getAidaAutoSaveFileName() + " >");
            try {
                AIDA.defaultInstance().saveAs(getJobPanel().getAidaAutoSaveFileName());
            } catch (IOException e) {
                errorHandler.setError(e)
                    .setMessage("Error saving AIDA file.")
                    .log()
                    .printStackTrace()
                    .showErrorDialog();
            }
        }
    }

    /**
     * Stop the session by stopping the event processing thread, ending the job, and disconnecting
     * from the ET system.
     */
    private void stopSession() {
        // Show a modal message window.
        JDialog dialog = showStatusDialog("Info", "Disconnecting from session ...", true);
        
        // Log message.
        logger.log(Level.FINER, "Stopping the ET session.");
        
        // Terminate event processing.
        stopEventProcessing();
                
        // Save AIDA file.
        saveAidaFile();
        
        // Disconnect from the ET system.
        if (usingEtServer())
            disconnect();
        else 
            setDisconnectedGuiState();        
                
        logger.log(Level.INFO, "Session was stopped.");
        
        // Close modal message window.
        dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
    }
                         
    /**
     * Show a dialog which is modal-like but will not block the current thread
     * from executing after <code>isVisible(true)</code> is called.  It does not 
     * have any buttons so must be closed using an action event.
     * @param title The title of the dialog box.
     * @param message The message to display.
     * @param visible Whether it should be immediately visible.
     * @return The JDialog that was created.
     */
    JDialog showStatusDialog(String title, String message, boolean visible) {
        final JOptionPane optionPane = new JOptionPane(
                message, 
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, 
                null, 
                new Object[]{}, 
                null);
        final JDialog dialog = new JDialog();
        dialog.setContentPane(optionPane);        
        dialog.setTitle(title);
        dialog.setAlwaysOnTop(true);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.pack();        
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
                MonitoringApplication.this.setEnabled(true);
                plotFrame.setEnabled(true);
            }
        });
        MonitoringApplication.this.setEnabled(false);
        plotFrame.setEnabled(false);
        dialog.setVisible(visible);
        return dialog;
    }    
            
    /**
     * Finish event processing and stop its thread, first killing the session watchdog 
     * thread, if necessary.  The event processing thread may still be alive after 
     * this method, e.g. if there is a call to <code>EtSystem.getEvents()</code> happening.
     * In this case, event processing will exit later when the ET system goes down.
     */
    private void stopEventProcessing() {
               
        // Is the event processing thread actually alive?
        if (eventProcessingThread.isAlive()) {

            // Interrupt and kill the session thread if necessary.
            // FIXME: Not sure this should happen here!
            killSessionWatchdogThread();

            // Request the event processing to stop.
            eventProcessing.finish();
        }

        // Wait for the event processing thread to finish.  This should just return
        // immediately if it isn't alive so don't bother checking.
        try {
            // In the case where ET is configured for sleep or timed wait, an untimed join could 
            // block forever, so only wait for ~1 second before continuing.  The EventProcessingChain
            // should still cleanup automatically when its thread completes after the ET system goes down.
            eventProcessingThread.join(1000);
        } catch (InterruptedException e) {
            // Don't know when this would ever happen.
        }
       
        // Log last error that occurred in event processing.
        if (eventProcessing.getLastError() != null) {
            logger.log(Level.SEVERE, eventProcessing.getLastError().getMessage());
        }
       
        // Reset event processing objects.
        eventProcessing = null;
        eventProcessingThread = null;
    }

    /**
     * Kill the current session watchdog thread.
     */
    private void killSessionWatchdogThread() {
        if (sessionWatchdogThread.isAlive()) {
            sessionWatchdogThread.interrupt();
            try {
                // This should always work.
                sessionWatchdogThread.join();
            } catch (InterruptedException e) {
            }
        }
        sessionWatchdogThread = null;
    }

    /**
     * Thread to automatically trigger a disconnect when the event processing chain finishes or
     * throws a fatal error.  This thread joins to the event processing thread and automatically 
     * requests a disconnect after it finishes using an action event.
     */
    private class SessionWatchdogThread extends Thread {

        public void run() {
            try {
                // When the event processing thread finishes, the session should be stopped and
                // disconnect should occur.
                eventProcessingThread.join();

                // Activate a disconnect using the ActionEvent which is used by the disconnect button.
                // FIXME: When this happens the event processing object and its thread don't get set to null!
                actionPerformed(new ActionEvent(Thread.currentThread(), 0, MonitoringCommands.DISCONNECT));

            } catch (InterruptedException e) {
                // This probably just means that the disconnect button was pushed, and this thread should
                // no longer wait on event processing to finish.
            }
        }
    }
}