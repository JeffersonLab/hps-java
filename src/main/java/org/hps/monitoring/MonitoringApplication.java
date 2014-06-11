package org.hps.monitoring;

import static org.hps.monitoring.MonitoringCommands.AIDA_AUTO_SAVE;
import static org.hps.monitoring.MonitoringCommands.CLEAR_LOG_TABLE;
import static org.hps.monitoring.MonitoringCommands.CONNECT;
import static org.hps.monitoring.MonitoringCommands.DISCONNECT;
import static org.hps.monitoring.MonitoringCommands.EDIT_EVENT_REFRESH;
import static org.hps.monitoring.MonitoringCommands.EXIT;
import static org.hps.monitoring.MonitoringCommands.LOAD_CONNECTION;
import static org.hps.monitoring.MonitoringCommands.LOAD_JOB_SETTINGS;
import static org.hps.monitoring.MonitoringCommands.LOG_TO_FILE;
import static org.hps.monitoring.MonitoringCommands.LOG_TO_TERMINAL;
import static org.hps.monitoring.MonitoringCommands.NEXT;
import static org.hps.monitoring.MonitoringCommands.PAUSE;
import static org.hps.monitoring.MonitoringCommands.RESET_CONNECTION_SETTINGS;
import static org.hps.monitoring.MonitoringCommands.RESET_JOB_SETTINGS;
import static org.hps.monitoring.MonitoringCommands.RESUME;
import static org.hps.monitoring.MonitoringCommands.SAVE_CONNECTION;
import static org.hps.monitoring.MonitoringCommands.SAVE_JOB_SETTINGS;
import static org.hps.monitoring.MonitoringCommands.SAVE_LOG_TABLE;
import static org.hps.monitoring.MonitoringCommands.SAVE_PLOTS;
import static org.hps.monitoring.MonitoringCommands.SCREENSHOT;
import static org.hps.monitoring.MonitoringCommands.SET_EVENT_BUILDER;
import static org.hps.monitoring.MonitoringCommands.SET_LOG_LEVEL;
import static org.hps.monitoring.MonitoringCommands.SET_STEERING_FILE;
import static org.hps.monitoring.MonitoringCommands.SET_STEERING_RESOURCE;

import java.awt.AWTException;
import java.awt.BorderLayout;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.hps.evio.LCSimEventBuilder;
import org.hps.monitoring.record.EventProcessingChain;
import org.hps.monitoring.record.EventProcessingThread;
import org.hps.monitoring.record.etevent.EtConnection;
import org.hps.monitoring.record.etevent.EtConnectionParameters;
import org.hps.monitoring.record.etevent.EtEventSource;
import org.hps.monitoring.subsys.et.EtSystemStripCharts;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.aida.AIDA;

/**
 * Monitoring application for HPS Test Run, which can run LCSim steering files on data converted
 * from the ET ring. This class is accessible to users by calling its main() method.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: MonitoringApplication.java,v 1.61 2013/12/10 07:36:40 jeremy Exp $
 */
// FIXME: The tabs panel isn't filling the full available space even when fill is set to both.
// TODO: Remove minimum GUI component size settings where they are redundant.
// TODO: Refactor this class into multiple classes.
// TODO: Log the messages from all Exceptions.
// TODO: Capture std err and out and redirect to a text panel within the application.
// TODO: Report state of event processing at the end of the job in a new GUI component.
// TODO: Check if the complex logic used to disconnect/cleanup the ET system is necessary anymore.
public class MonitoringApplication {

    // Top-level Swing components.
    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JTabbedPane plotPane;
    private JTabbedPane tabs;
    private ConnectionPanel connectionPanel;
    private ConnectionStatusPanel connectionStatusPanel;
    private EventPanel eventPanel;
    private JobPanel jobPanel;
    private JMenuBar menuBar;
    private EventButtonsPanel buttonsPanel;
    private JFrame frame;

    // References to menu items that will be enabled/disabled depending on application state.
    private JMenuItem connectItem;
    private JMenuItem disconnectItem;
    private JMenuItem resetConnectionItem;
    private JMenuItem connectionLoadItem;
    private JMenuItem savePlotsItem;
    private JMenuItem logItem;
    private JMenuItem terminalItem;
    private JMenuItem steeringItem;
    private JMenuItem aidaAutoSaveItem;
    private JMenuItem saveJobSettingsItem;
    private JMenuItem loadJobSettingsItem;
    private JMenuItem resetJobSettingsItem;

    // Saved references to System.out and System.err in case need to reset.
    private final PrintStream sysOut = System.out;
    private final PrintStream sysErr = System.err;

    // ET connection parameters and state.
    private EtConnectionParameters connectionParameters;
    private EtConnection connection;
    private int connectionStatus = ConnectionStatus.DISCONNECTED;

    // Event processing objects.
    private JobControlManager jobManager;
    private LCSimEventBuilder eventBuilder;
    private EventProcessingThread eventProcessingThread;

    // Job timing.
    private Timer timer;
    private TimerTask updateTimeTask;
    private long jobStartTime;

    // ActionListener for GUI event dispatching.
    private ActionListener actionListener;

    // Logging objects.
    private static Logger logger;
    private Handler logHandler;
    private DefaultTableModel logTableModel;
    //static final String[] logTableColumns = { "Source", "Message", "Date", "Level" };
    static final String[] logTableColumns = { "Message", "Date", "Level" };
    private JTable logTable;
    private Level defaultLogMessageLevel = Level.INFO;

    // Some default GUI size parameters.
    private final int logTableWidth = 700;
    private final int logTableHeight = 270;

    // Format for screenshots. Hard-coded to PNG.
    private static final String screenshotFormat = "png";

    // Maximum time in millis to wait for the ET system to disconnect.
    // TODO: Make this an option in the JobPanel.
    private int maxCleanupTime = 5000;

    // Format of date field for log.
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");
    private static final String LCSIM_FAIL_MESSAGE = "Failed to setup LCSim.";

    private static final int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
    private static final int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

    private static final int leftPanelWidth = (int) (screenWidth * 0.33);
    private static final int rightPanelWidth = (int) (screenWidth * 0.40);

    private static final int connectionStatusPanelHeight = 50;
    private static final int connectionStatusPanelWidth = 400;

    EventProcessingChain eventProcessing;

    /**
     * Constructor for the monitoring application. 
     */
    private MonitoringApplication() {

        // Create and configure the logger.
        setupLogger();

        // Create the ActionEventListener for event dispatching.
        actionListener = new MonitoringApplicationActionListener();

        // Setup the application menus.
        createMenu();

        // Create the GUI panels.
        createPanels();

        // Create the log table GUI component.
        createLogTable();

        // Setup AIDA.
        setupAida();

        // Log that the application started successfully.
        log("Application initialized successfully.");
    }

    /**
     * Setup and configure the AIDA plotting backend.
     */
    private void setupAida() {
        MonitoringAnalysisFactory.register();
        MonitoringAnalysisFactory.configure();
        MonitoringPlotFactory.setRootPane(plotPane);
    }

    /**
     * Creates all the JPanels for the monitoring GUI.
     */
    private void createPanels() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        createLeftPanel();
        createRightPanel();
    }

    /**
     * Create the left panel.
     */
    private void createLeftPanel() {

        // Lefthand panel containing the three application tabs.
        leftPanel = new JPanel();
        // set border ex.
        // leftPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        // leftPanel.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.setMinimumSize(new Dimension(leftPanelWidth, screenHeight - 30));
        leftPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        // Event processing buttons.
        //c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 0, 0, 10);
        buttonsPanel = new EventButtonsPanel();
        buttonsPanel.addActionListener(actionListener);
        leftPanel.add(buttonsPanel, c);

        // Connection status panel.
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = c.weighty = 1.0;
        c.insets = new Insets(10, 0, 5, 0);
        connectionStatusPanel = new ConnectionStatusPanel();
        connectionStatusPanel.setMinimumSize(new Dimension(connectionStatusPanelWidth, connectionStatusPanelHeight));
        leftPanel.add(connectionStatusPanel, c);

        // Contents of the tabs panel.
        connectionPanel = new ConnectionPanel();
        eventPanel = new EventPanel();
        jobPanel = new JobPanel();
        jobPanel.addActionListener(actionListener);
        
        // Create the container for the tabbed pane.
        //JPanel tabsPanel = new JPanel();
        //c = new GridBagConstraints();
        //c.gridx = 0;
        //c.gridy = 2;
        //c.fill = GridBagConstraints.BOTH;
        //c.weightx = c.weighty = 1.0;
        //c.insets = new Insets(0, 0, 0, 10);
        //leftPanel.add(tabsPanel, c);
        
        // Tab panels.
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 2;
        tabs = new JTabbedPane();
        tabs.addTab("Connection Settings", connectionPanel);
        tabs.addTab("Event Monitor", eventPanel);
        tabs.addTab("Job Settings", jobPanel);
        //tabsPanel.add(tabs, c);
        leftPanel.add(tabs, c);
                
        // Layout attributes for the entire left panel.
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 5, 0, 0);
        mainPanel.add(leftPanel, c);
    }

    /**
     * Create the right panel.
     */
    private void createRightPanel() {

        // Create right-hand panel.
        rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(rightPanelWidth, screenHeight - 30));
        rightPanel.setMinimumSize(new Dimension(rightPanelWidth, screenHeight - 30));
        rightPanel.setLayout(new BorderLayout());

        // Create plot pane with empty tabs.
        plotPane = new JTabbedPane();
        rightPanel.add(plotPane, BorderLayout.CENTER);

        // Set layout of right panel.
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1.0;
        c.weighty = 1.0;
        mainPanel.add(rightPanel, c);
    }

    /**
     * Create the menu items.
     */
    private void createMenu() {

        menuBar = new JMenuBar();

        JMenu connectionMenu = new JMenu("Connection");
        connectionMenu.setMnemonic(KeyEvent.VK_C);
        menuBar.add(connectionMenu);

        connectItem = addMenuItem("Connect", KeyEvent.VK_C, CONNECT, true, "Connect to ET system using parameters from connection panel.", connectionMenu);
        disconnectItem = addMenuItem("Disconnect", KeyEvent.VK_D, DISCONNECT, false, "Disconnect from the current ET session.", connectionMenu);
        resetConnectionItem = addMenuItem("Reset Connection Settings", KeyEvent.VK_R, RESET_CONNECTION_SETTINGS, true, "Reset connection settings to defaults.", connectionMenu);
        connectionLoadItem = addMenuItem("Load Connection...", KeyEvent.VK_L, LOAD_CONNECTION, true, "Load connection settings from a saved properties file.", connectionMenu);
        addMenuItem("Save Connection...", KeyEvent.VK_S, SAVE_CONNECTION, true, "Save connection settings to a properties file.", connectionMenu);
        addMenuItem("Exit", KeyEvent.VK_X, EXIT, true, "Exit from the application.", connectionMenu);

        JMenu eventMenu = new JMenu("Event");
        eventMenu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(eventMenu);

        //addMenuItem("Reset Event Monitor", KeyEvent.VK_E, RESET_EVENTS, true, "Reset timer and counters in the event monitor tab.", eventMenu);

        /**
         * FIXME: Rest of these should be converted to use the addMenuItem() helper method...
         */

        JMenuItem eventRefreshItem = new JMenuItem("Set Event Refresh...");
        eventRefreshItem.setMnemonic(KeyEvent.VK_V);
        eventRefreshItem.setActionCommand(EDIT_EVENT_REFRESH);
        eventRefreshItem.addActionListener(actionListener);
        eventRefreshItem.setToolTipText("Set the number of events between GUI updates.");
        eventMenu.add(eventRefreshItem);

        /*
        JMenuItem maxEventsItem = new JMenuItem("Set Max Events...");
        maxEventsItem.setMnemonic(KeyEvent.VK_M);
        maxEventsItem.setActionCommand(SET_MAX_EVENTS);
        maxEventsItem.addActionListener(actionListener);
        maxEventsItem.setToolTipText("Set the maximum number of events to process in one session.");
        eventMenu.add(maxEventsItem);
        */

        JMenu jobMenu = new JMenu("Job");
        jobMenu.setMnemonic(KeyEvent.VK_J);
        menuBar.add(jobMenu);

        saveJobSettingsItem = new JMenuItem("Save Job Settings...");
        saveJobSettingsItem.setMnemonic(KeyEvent.VK_J);
        saveJobSettingsItem.setActionCommand(SAVE_JOB_SETTINGS);
        saveJobSettingsItem.addActionListener(actionListener);
        saveJobSettingsItem.setToolTipText("Save Job Settings configuration to a properties file.");
        jobMenu.add(saveJobSettingsItem);

        loadJobSettingsItem = new JMenuItem("Load Job Settings...");
        loadJobSettingsItem.setMnemonic(KeyEvent.VK_L);
        loadJobSettingsItem.setActionCommand(LOAD_JOB_SETTINGS);
        loadJobSettingsItem.addActionListener(actionListener);
        loadJobSettingsItem.setToolTipText("Load Job Settings from a properties file.");
        jobMenu.add(loadJobSettingsItem);

        resetJobSettingsItem = new JMenuItem("Reset Job Settings");
        resetJobSettingsItem.setMnemonic(KeyEvent.VK_R);
        resetJobSettingsItem.setActionCommand(RESET_JOB_SETTINGS);
        resetJobSettingsItem.addActionListener(actionListener);
        resetJobSettingsItem.setToolTipText("Reset Job Settings to the defaults.");
        jobMenu.add(resetJobSettingsItem);

        steeringItem = new JMenuItem("Set Steering File...");
        steeringItem.setMnemonic(KeyEvent.VK_S);
        steeringItem.setActionCommand(SET_STEERING_FILE);
        steeringItem.addActionListener(actionListener);
        steeringItem.setToolTipText("Set the job's LCSim steering file.");
        jobMenu.add(steeringItem);

        aidaAutoSaveItem = new JMenuItem("AIDA Auto Save File...");
        aidaAutoSaveItem.setMnemonic(KeyEvent.VK_A);
        aidaAutoSaveItem.setActionCommand(AIDA_AUTO_SAVE);
        aidaAutoSaveItem.addActionListener(actionListener);
        aidaAutoSaveItem.setToolTipText("Select name of file to auto save AIDA plots at end of job.");
        jobMenu.add(aidaAutoSaveItem);

        savePlotsItem = new JMenuItem("Save Plots to AIDA File...");
        savePlotsItem.setMnemonic(KeyEvent.VK_P);
        savePlotsItem.setActionCommand(SAVE_PLOTS);
        savePlotsItem.addActionListener(actionListener);
        savePlotsItem.setEnabled(false);
        savePlotsItem.setToolTipText("Save plots from default AIDA tree to an output file.");
        jobMenu.add(savePlotsItem);

        logItem = new JMenuItem("Redirect to File...");
        logItem.setMnemonic(KeyEvent.VK_F);
        logItem.setActionCommand(LOG_TO_FILE);
        logItem.addActionListener(actionListener);
        logItem.setEnabled(true);
        logItem.setToolTipText("Redirect job's standard out and err to a file.");
        jobMenu.add(logItem);

        terminalItem = new JMenuItem("Redirect to Terminal");
        terminalItem.setMnemonic(KeyEvent.VK_T);
        terminalItem.setActionCommand(LOG_TO_TERMINAL);
        terminalItem.addActionListener(actionListener);
        terminalItem.setEnabled(false);
        terminalItem.setToolTipText("Redirect job's standard out and err back to the terminal.");
        jobMenu.add(terminalItem);

        JMenuItem screenshotItem = new JMenuItem("Take a screenshot...");
        screenshotItem.setMnemonic(KeyEvent.VK_N);
        screenshotItem.setActionCommand(SCREENSHOT);
        screenshotItem.addActionListener(actionListener);
        screenshotItem.setToolTipText("Save a full screenshot to a " + screenshotFormat + " file.");
        jobMenu.add(screenshotItem);

        JMenu logMenu = new JMenu("Log");
        jobMenu.setMnemonic(KeyEvent.VK_L);
        menuBar.add(logMenu);

        JMenuItem saveLogItem = new JMenuItem("Save log to file...");
        saveLogItem.setMnemonic(KeyEvent.VK_S);
        saveLogItem.setActionCommand(SAVE_LOG_TABLE);
        saveLogItem.addActionListener(actionListener);
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
     * @param menu The menu to which it should be added.
     * @return
     */
    private JMenuItem addMenuItem(String label, int mnemonic, String cmd, boolean enabled, String tooltip, JMenu menu) {
        JMenuItem item = new JMenuItem(label);
        item.setMnemonic(mnemonic);
        item.setActionCommand(cmd);
        item.setEnabled(enabled);
        item.setToolTipText(tooltip);
        item.addActionListener(actionListener);
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
            Object[] row = new Object[] { /*record.getLoggerName(),*/ // source
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
        logPane.setPreferredSize(new Dimension(logTableWidth, logTableHeight));
        logPane.setMinimumSize(new Dimension(logTableWidth, logTableHeight));
        leftPanel.add(logPane, c);
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
     * Create the monitoring application frame and run it on a separate thread.
     * @return Reference to the created application.
     */
    static final MonitoringApplication createMonitoringApplication() {
        final MonitoringApplication app = new MonitoringApplication();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                app.createApplicationFrame();
            }
        });
        return app;
    }

    /**
     * Load connection settings from a file.
     * @param file The properties file.
     */
    void loadConnectionSettings(File file) {
        connectionPanel.loadPropertiesFile(file);
    }

    /**
     * Load job settings from a file.
     * @param file The properties file.
     */
    void loadJobSettings(File file) {
        try {
            jobPanel.setJobSettings(new JobSettings(file));
            // Need to check here if System.out and err have been redirected.
            if (jobPanel.logToFile()) {
                redirectStdOutAndErrToFile(new File(jobPanel.getLogFileName()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The ActionListener implementation for handling all GUI events.
     */
    private class MonitoringApplicationActionListener implements ActionListener {

        /**
         * Action handler method for the app.
         * @param e The event to handle.
         */
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();            
            if (CONNECT.equals(cmd)) {
                //startSessionThread();
                startSession();
            } else if (DISCONNECT.equals(cmd)) {
                //startDisconnectThread();
                stopSession();
            } else if (EDIT_EVENT_REFRESH.equals(cmd)) {
                setEventRefresh();
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
                connectionPanel.save();
            } else if (LOAD_CONNECTION.equals(cmd)) {
                connectionPanel.load();
            } else if (RESET_CONNECTION_SETTINGS.equals(cmd)) {
                connectionPanel.reset();
            } /*else if (SET_MAX_EVENTS.equals(cmd)) {
                setMaxEvents();
            }*/ else if (SAVE_LOG_TABLE.equals(cmd)) {
                saveLogToFile();
            } else if (CLEAR_LOG_TABLE.equals(cmd)) {
                clearLog();
            } else if (SET_EVENT_BUILDER.equals(cmd)) {
                jobPanel.editEventBuilder();
            } else if (PAUSE.equals(cmd)) {
                pauseEventProcessing();
            } else if (NEXT.equals(cmd)) {
                nextEvent();
            } else if (RESUME.equals(cmd)) {
                resumeEventProcessing();
            } else if (SET_LOG_LEVEL.equals(cmd)) {
                setLogLevel();
            } else if (AIDA_AUTO_SAVE.equals(cmd)) {
                jobPanel.chooseAidaAutoSaveFile();
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
            }
        }
    }

    /**
     * This fires when a steering resource file is selected from the combo box. The Job Settings
     * are changed to use a resource type.
     */
    private void steeringResourceSelected() {
        jobPanel.setSteeringType(JobPanel.RESOURCE);
    }

    /**
     * Save the job settings to a selected file.
     */
    private void saveJobSettings() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Job Settings");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(leftPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            JobSettings settings = jobPanel.getJobSettings();
            try {
                settings.save(f);
                log(Level.INFO, "Saved Job Settings to properties file <" + f.getPath() + ">.");
            } catch (IOException e) {
                e.printStackTrace();
                log(Level.SEVERE, "Error saving Job Settings to properties file <" + f.getPath() + ">.");
                showDialog("Error saving Job Settings to properties file.");
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
        int r = fc.showOpenDialog(leftPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                jobPanel.setJobSettings(new JobSettings(f));
                log(Level.INFO, "Loaded Job Settings from properties file <" + f.getPath() + ">.");
            } catch (IOException e) {
                e.printStackTrace();
                log(Level.SEVERE, "Error loading Job Settings from properties file <" + f.getPath() + ">.");
                showDialog("Error loading Job Settings from properties file.");
            }
        }
    }

    /**
     * Reset the job settings to the defaults.
     */
    private void resetJobSettings() {
        jobPanel.resetJobSettings();
        // Redirect System.out and err back to the terminal.
        logToTerminal();
    }
   
    /**
     * Set a new log level for the application and also forward to the event processor.
     */
    private void setLogLevel() {
        Level newLevel = jobPanel.getLogLevel();
        logger.setLevel(newLevel);
        //if (eventProcessor != null) {
        //    eventProcessor.setLogLevel(newLevel);
        //}

        log(Level.INFO, "Log Level was changed to <" + jobPanel.getLogLevel().toString() + ">.");
    }

    /**
     * Set the connection status.
     * @param status The connection status.
     */
    private void setConnectionStatus(int status) {
        connectionStatus = status;
        connectionStatusPanel.setStatus(status);
        log(Level.FINE, "Connection status changed to <" + ConnectionStatus.toString(status) + ">.");
        logHandler.flush();
    }

    /**
     * Get the current connection status.
     * @return The connection status.
     */
    private int getConnectionStatus() {
        return connectionStatus;
    }

    /**
     * Pop-up a modal dialog.
     * @param m The message to display in the dialog box.
     */
    private void showDialog(String m) {
        JOptionPane.showMessageDialog(leftPanel, m);
    }

    /**
     * Setup the frame to run the application.
     */
    private void createApplicationFrame() {
        mainPanel.setOpaque(true);
        frame = new JFrame(getApplicationTitle());
        frame.setContentPane(mainPanel);
        frame.setJMenuBar(menuBar);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // frame.setMinimumSize(new Dimension(600, 850));
        frame.setResizable(true);
        frame.pack();    
        frame.setVisible(true);
    }

    /**
     * Save plots to a selected output file.
     */
    private void savePlots() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showSaveDialog(mainPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File fileName = fc.getSelectedFile();
            try {
                AIDA.defaultInstance().saveAs(fileName);
                logger.log(Level.INFO, "Plots saved to <" + fileName + ">.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Select an LCSim steering file.
     */
    private void selectSteeringFile() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showOpenDialog(mainPanel);
        if (r == JFileChooser.APPROVE_OPTION) {
            File fileName = fc.getSelectedFile();
            try {
                (new JobControlManager()).setup(fileName);
                jobPanel.setSteeringFile(fileName.getPath());
                jobPanel.setSteeringType(JobPanel.FILE);
                log("Steering file set to <" + fileName.getPath() + ">.");
            } catch (Exception e) {
                e.printStackTrace();
                log(Level.SEVERE, "Failed to read steering file <" + fileName.getPath() + ">.");
                showDialog("Failed to read the LCSim XML file.");
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
     * Call the reset() method on Drivers which implement {@link Resettable}. They must implement
     * the {@link Resettable} interface for this to work.
     */
    /*
    private synchronized void resetDrivers() {
        if (jobManager != null) {
            for (Driver driver : jobManager.getDriverExecList()) {
                if (driver instanceof Resettable) {
                    try {
                        ((Resettable) driver).reset();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        log(Level.INFO, "LCSim drivers were reset.");
    }
    */

    /**
     * Redirect System.out and System.err to a file. This is primarily used to capture lengthy
     * debug output from event processing. Messages sent to the Logger are unaffected.
     */
    private void logToFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Log File");
        fc.setCurrentDirectory(new File("."));
        int fcs = fc.showSaveDialog(mainPanel);
        if (fcs == JFileChooser.APPROVE_OPTION) {
            final File logFile = fc.getSelectedFile();
            if (logFile.exists()) {
                showDialog("Log file already exists.");
            } else {
                try {
                    if (!logFile.createNewFile()) {
                        throw new IOException();
                    }

                    redirectStdOutAndErrToFile(logFile);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            jobPanel.setLogToFile(true);
                            jobPanel.setLogFile(logFile.getPath());

                            terminalItem.setEnabled(true);
                            logItem.setEnabled(false);
                        }
                    });

                    log("Redirected System output to file <" + logFile.getPath() + ">.");
                } catch (IOException e) {
                    e.printStackTrace();
                    log(Level.SEVERE, "Error redirecting System output to file <" + logFile.getPath() + ">.");
                    showDialog("Error redirecting System output to log file.");
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
                jobPanel.setLogFile("");
                jobPanel.setLogToFile(false);
                terminalItem.setEnabled(false);
                logItem.setEnabled(true);
            }
        });
        log("Redirected print output to terminal.");
    }

    /**
     * Set the event refresh rate using a modal dialog.
     */
    private void setEventRefresh() {
        String inputValue = JOptionPane.showInputDialog("Event Refresh Rate:", eventPanel.getEventRefresh());
        try {
            int newEventRefresh = Integer.parseInt(inputValue);
            if (newEventRefresh < 1) {
                throw new RuntimeException("Event Refresh must be > 0.");
            }
            eventPanel.setEventRefresh(newEventRefresh);
            log("Event refresh set to <" + newEventRefresh + ">.");
        } catch (Exception e) {
            e.printStackTrace();
            log(Level.WARNING, "Ignored invalid event refresh setting.");
            showDialog("The value " + inputValue + " is not valid for Event Refresh Rate.");
        }
    }

    /**
     * Using a modal dialog, set the maximum number of events to process before an automatic
     * disconnect.
     */
    /*
    private void setMaxEvents() {
        String inputValue = JOptionPane.showInputDialog("Max Events:", eventPanel.getMaxEvents());
        try {
            int newMaxEvents = Integer.parseInt(inputValue);
            if (newMaxEvents < 0) {
                showDialog("Max Events is set to unlimited.");
                newMaxEvents = -1;
            }
            // Set max events in panel.
            eventPanel.setMaxEvents(newMaxEvents);
            // Set max events in event processor.
            //if (eventProcessor != null) {
            //    eventProcessor.setMaxEvents(newMaxEvents);
            //}
            log("Max events set to <" + newMaxEvents + ">.");
        } catch (Exception e) {
            e.printStackTrace();
            log(Level.WARNING, "Ignored invalid max events setting <" + inputValue + ">.");
            showDialog("The value " + inputValue + " is not valid for Max Events.");
        }
    }
    */

    /**
     * Set the GUI state to disconnected, which will enable/disable applicable GUI components and
     * menu items.
     */
    private void setDisconnectedGuiState() {

        // Enable or disable appropriate menu items.
        connectItem.setEnabled(true);
        disconnectItem.setEnabled(false);
        resetConnectionItem.setEnabled(true);
        connectionLoadItem.setEnabled(true);
        savePlotsItem.setEnabled(false);
        //resetDriversItem.setEnabled(false);
        logItem.setEnabled(true);
        terminalItem.setEnabled(true);
        steeringItem.setEnabled(true);

        // Re-enable the ConnectionPanel.
        connectionPanel.enableConnectionPanel(true);

        // Re-enable the JobPanel.
        jobPanel.enableJobPanel(true);

        // Set relevant event panel buttons to disabled.
        buttonsPanel.enablePauseButton(false);
        buttonsPanel.enableNextEventsButton(false);

        // Toggle connection button to proper setting.
        buttonsPanel.toggleConnectButton();
    }

    /**
     * Set the GUI to connected state, which will enable/disable appropriate components and menu
     * items.
     */
    private void setConnectedGuiState() {

        // Disable connection panel.
        connectionPanel.enableConnectionPanel(false);

        // Disable JobPanel.
        jobPanel.enableJobPanel(false);

        // Enable or disable appropriate menu items.
        connectItem.setEnabled(false);
        disconnectItem.setEnabled(true);
        resetConnectionItem.setEnabled(false);
        connectionLoadItem.setEnabled(false);
        savePlotsItem.setEnabled(true);
        //resetDriversItem.setEnabled(true);
        logItem.setEnabled(false);
        terminalItem.setEnabled(false);
        steeringItem.setEnabled(false);

        // Enable relevant event panel buttons.
        buttonsPanel.enablePauseButton(true);
        buttonsPanel.setPauseModeState(jobPanel.pauseMode());

        // Toggle connection button to proper settings.
        buttonsPanel.toggleConnectButton();
    }

    /**
     * Exit from the application.
     */
    private void exit() {
        if (connection != null) {
            cleanupEtConnection();
        }
        System.exit(0);
    }

    /**
     * Save a screenshot using a file chooser.
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
            log("Screenshot saved to <" + fileName + ">.");
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
        } catch (AWTException e) {
            e.printStackTrace();
            showDialog(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            showDialog(e.getMessage());
        }
    }

    /**
     * Get the fully qualified class name of the current event builder for converting from EVIO to
     * LCIO.
     * @return The class name of the event builder.
     */
    private String getEventBuilderClassName() {
        return jobPanel.getEventBuilderClassName();
    }

    /**
     * Get the type of steering file being used.
     * @return The type of the steering file.
     */
    private int getSteeringType() {
        return jobPanel.getSteeringType();
    }

    /**
     * Get the current max events setting.
     * @return The maximum number of events to process before disconnect.
     */
    /*
    private int getMaxEvents() {
        return eventPanel.getMaxEvents();
    }
    */

    /**
     * Execute a monitoring session. This should be executed in a separate thread so as not to block the
     * GUI or other threads during a monitoring session.
     */
    private void startSession() {

        log(Level.INFO, "Starting a new monitoring session.");

        try {

            // Reset the plot panel and global AIDA state.
            resetPlots();

            // Setup the LCSim JobControlManager and event builder.
            setupLCSim();

            // Connect to the ET system, which will setup a valid EtConnection object.
            connect();
            
            // Setup the EventProcessingChain object using the EtConnection.
            setupEventProcessingChain();

            // Start the event processing thread.
            eventProcessingThread.start();
            
            // Start the session timer.
            startSessionTimer();
           
            log(Level.INFO, "Successfully started the monitoring session.");
                                                      
        } catch (Exception e) {
            e.printStackTrace();
            log(Level.SEVERE, e.getMessage());
            disconnect(ConnectionStatus.ERROR);
        }         
    }

    /**
     * Connect to the ET system specified in the GUI's connection panel settings.
     */
    private void connect() throws IOException {

        log("Connecting to ET system.");

        setConnectionStatus(ConnectionStatus.CONNECTION_REQUESTED);

        // Make sure applicable menu items are enabled or disabled.
        setConnectedGuiState();

        // Create a connection to the ET server.
        try {
            createEtConnection();          
            log("Successfully connected to ET system.");
        } catch (Exception e) {
            log(e.getMessage());
            throw new IOException(e);
        }       
    }

    /**
     * Get the steering parameter, which is either a file path or resource string.
     * @return The steering parameter.
     */
    private String getSteering() {
        return jobPanel.getSteering();
    }

    /**
     * Get the name of the detector for conditions data.
     * @return The name of the detector.
     */
    private String getDetectorName() {
        return jobPanel.getDetectorName();
    }

    /**
     * Get the connection parameter settings from the connection panel.
     * @return The connection parameters.
     */
    private EtConnectionParameters getConnectionParameters() {
        return connectionPanel.getConnectionParameters();
    }

    /**
     * Get whether a warning dialog will open before disconnect.
     * @return True if warning will occur before disconnect; false if no.
     */
    private boolean warnOnDisconnect() {
        return jobPanel.warnOnDisconnect();
    }

    /**
     * Get whether errors in event processing will cause automatic disconnect.
     * @return True if disconnect on event processing error; false to continue.
     */
    private boolean disconnectOnError() {
        return jobPanel.disconnectOnError();
    }

    private void disconnect() {
        disconnect(ConnectionStatus.DISCONNECTING);
    }

    /**
     * Disconnect from the current ET session with a particular status.
     * @param status The connection status.
     */
    synchronized private void disconnect(int status) {

        log("Disconnecting from the ET system.");
     
        // Cleanup the ET connection.
        cleanupEtConnection();

        // Update state of GUI to disconnected.
        setDisconnectedGuiState();

        // Finally, change application state to fully disconnected.
        setConnectionStatus(ConnectionStatus.DISCONNECTED);
        
        // Set the application status from the caller if an error had occurred.
        if (status == ConnectionStatus.ERROR)
            setConnectionStatus(status);

        log("Successfully disconnected from ET system.");
    }

    /**
     * This is a thread for cleaning up the ET connection. 
     * It is executed under a separate thread, because it could potentially block forever. 
     * So we need to be able to kill it after waiting for X amount of time.
     */
    private class EtCleanupThread extends Thread {

        boolean succeeded;

        EtCleanupThread() {
            super("ET Cleanup Thread");
        }

        public void run() {
            connection.cleanup();
            connection = null;
            succeeded = true;
        }

        public boolean succeeded() {
            return succeeded;
        }

        public void stopCleanup() {
            Thread.yield();
        }
    }

    /**
     * Cleanup the ET connection.
     */
    private void cleanupEtConnection() {

        if (connection != null) {
           
            // Execute the connection cleanup thread.
            EtCleanupThread cleanupThread = new EtCleanupThread();
            log(Level.FINE, "Starting EtCleanupThread to disconnect from ET system.");
            logHandler.flush();
            cleanupThread.start();
            try {
                // Wait X seconds for cleanup thread to finish.
                cleanupThread.join(this.maxCleanupTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (cleanupThread.succeeded()) {
                log(Level.FINE, "EtCleanupThread succeeded in disconnecting from ET system.");
            } else {
                log(Level.SEVERE, "EtCleanupThread failed to disconnect.  Your station <" + this.connection.getEtStation().getName() + "> is zombified!");
                // Make the cleanup thread yield.
                cleanupThread.stopCleanup();
                // Stop the cleanup thread.
                // FIXME: Should call yield() instead?
                cleanupThread.stop();
                // Join to cleanup thread until it dies.                
                log(Level.FINEST, "EtCleanupThread was killed.");
                // The ET connection is now unusable so set it to null.
                this.connection = null;
            }            
        }
    }

    /**
     * Setup the LCSim job manager and the event builder.
     */
    private void setupLCSim() {

        log(Level.INFO, "Setting up LCSim.");

        // Get steering resource or file as a String parameter.
        String steering = getSteering();
        int steeringType = jobPanel.getSteeringType();
        log(Level.CONFIG, "LCSim steering <" + steering + "> of type <" + (steeringType == JobPanel.RESOURCE ? "RESOURCE" : "FILE") + ">.");

        // Check if the LCSim steering file looks valid.
        if (jobPanel.checkSteering() == false) {
            log(Level.SEVERE, "Steering file <" + steering + "> is not valid.");
            throw new RuntimeException("Invalid LCSim steering file < " + steering + ">.");
        }

        try {
            // Create job manager and configure.
            jobManager = new JobControlManager();
            jobManager.setPerformDryRun(true);
            if (steeringType == JobPanel.RESOURCE) {
                log(Level.CONFIG, "Setting up steering resource <" + steering + ">.");
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(steering);
                jobManager.setup(is);
                is.close();
            } else if (getSteeringType() == JobPanel.FILE) {
                log(Level.CONFIG, "Setting up steering file <" + steering + ">.");
                jobManager.setup(new File(steering));
            }

            // Setup the event builder to translate from EVIO to LCIO.
            createEventBuilder();

            // Catch all other setup exceptions and re-throw them as RuntimeExceptions.
        } catch (Exception e) {
            e.printStackTrace();
            log(Level.SEVERE, e.getMessage());
            log(Level.SEVERE, LCSIM_FAIL_MESSAGE);
            throw new RuntimeException(LCSIM_FAIL_MESSAGE, e);
        }

        log(Level.INFO, "LCSim setup was successful.");
    }

    /**
     * Create the event builder for converting EVIO events to LCSim.
     */
    private void createEventBuilder() {

        // Setup the EventBuilder class.
        String eventBuilderClassName = getEventBuilderClassName();

        log(Level.CONFIG, "Initializing event builder <" + eventBuilderClassName + ">.");

        try {
            eventBuilder = (LCSimEventBuilder) Class.forName(eventBuilderClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LCSimEventBuilder class.", e);
        }

        // Set the detector name on the event builder so it can find conditions data.
        eventBuilder.setDetectorName(getDetectorName());

        log(Level.INFO, "Successfully initialized event builder <" + eventBuilderClassName + ">.");
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

            log(Level.CONFIG, "Created ET connection to <" + connectionParameters.getBufferName() + ">.");
        } else {
            // Some error occurred and the connection is not valid.
            setConnectionStatus(ConnectionStatus.ERROR);
            log(Level.SEVERE, "Failed to create ET connection to <" + connectionParameters.getBufferName() + ">.");
            throw new RuntimeException("Failed to create ET connection.");
        }
    }

    /**
     * Start the job timer.
     */
    private void startSessionTimer() {
        timer = new Timer("UpdateTime");
        jobStartTime = System.currentTimeMillis();
        
        updateTimeTask = new TimerTask() {                       
            public void run() {
                final long elapsedTime = (System.currentTimeMillis() - jobStartTime) / 1000;
                eventPanel.setElapsedTime(elapsedTime);
            }            
        };        
        timer.scheduleAtFixedRate(updateTimeTask, 0, 1000);
        log(Level.FINE, "Job timer started.");
    }
    
    private void stopSessionTimer() {
        updateTimeTask.cancel();
        timer.purge();
    }

    /**
     * Update the elapsed time in the GUI.
     */
    private void updateTime() {
        final long elapsedTime = (System.currentTimeMillis() - jobStartTime) / 1000;
        eventPanel.setElapsedTime(elapsedTime);
    }

    /**
     * Save the accumulated log messages to a tab-delimited text file selected using a file
     * chooser.
     */
    private void saveLogToFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Log File");
        fc.setCurrentDirectory(new File("."));
        int fcs = fc.showSaveDialog(mainPanel);
        if (fcs == JFileChooser.APPROVE_OPTION) {
            final File logFile = fc.getSelectedFile();
            if (logFile.exists()) {
                showDialog("The log file already exists.");
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
                    log("Saved log to file <" + logFile.getPath() + ">.");
                } catch (IOException e) {
                    e.printStackTrace();
                    log(Level.SEVERE, "Failed to save log to file <" + logFile.getPath() + ">.");
                    showDialog("Failed to save log file.");
                }
            }
        }
    }

    /**
     * Clear all data from the log table.
     */
    private void clearLog() {
        logTableModel.setRowCount(0);
        log(Level.FINE, "Log table was cleared.");
    }

    /**
     * Get whether or not connected to the ET system with an active monitoring session.
     * @return True if connected to ET system; false if not.
     */
    private boolean connected() {
        return connectionStatus == ConnectionStatus.CONNECTED;
    }

    /**
     * Notify event processor to get next set of events, if in pause mode.
     */
    private void nextEvent() {
        if (connected()) {
            log(Level.FINER, "Notifying event processor to get next events.");
            eventProcessing.next();
        } else {
            log(Level.WARNING, "Ignored next events command because app is disconnected.");
        }
    }

    /**
     * Notify the event processor to resume processing events in real-time mode, if paused.
     */
    private void resumeEventProcessing() {
        
        if (connected()) {
            
            // Notify event processor to continue.
            eventProcessing.resume();

            // Set state of event buttons.
            buttonsPanel.setPauseModeState(false);

            // Toggle job panel setting.
            jobPanel.enablePauseMode(false);

            log(Level.FINEST, "Disabled pause mode and will now process in real-time.");
        }
    }

    /**
     * Notify the event processor to start pause mode, which will pause between events.
     */
    private void pauseEventProcessing() {
        if (connected()) {
            // Pause event processing.
            eventProcessing.pause();
            
            // Set GUI state.
            buttonsPanel.setPauseModeState(true);
            jobPanel.enablePauseMode(false);
            
            log(Level.FINER, "Enabled pause mode.");
        }
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
        log(defaultLogMessageLevel, m);
    }

    /**
     * Setup the <tt>EventProcessingChain</tt> object and create a <tt>Thread</tt> for running it.
     * The processing is not started by this method.
     */
    private void setupEventProcessingChain() {
        eventProcessing = new EventProcessingChain();
        eventProcessing.setRecordSource(new EtEventSource(this.connection));
        eventProcessing.setEventBuilder(this.eventBuilder);
        eventProcessing.setDetectorName(this.getDetectorName());
        eventProcessing.add(this.jobManager.getDriverExecList());
        eventProcessing.add(new EventPanelUpdater(eventPanel));
        
        // TEST
        eventProcessing.add(new EtSystemStripCharts());
        
        eventProcessing.setStopOnEndRun();
        if (!this.disconnectOnError())
            eventProcessing.setContinueOnErrors();
        eventProcessing.configure();
        eventProcessingThread = new EventProcessingThread(eventProcessing);
    }

    /**
     * Clear state of plot panel and AIDA for a new session.
     */
    private void resetPlots() {

        // Clear the static AIDA tree in case plots are hanging around from previous sessions.
        resetAidaTree();

        // Reset the plots panel so that it is empty.
        plotPane.removeAll();

    }
    
    /**
     * End the current job.
     */
    private void endJob() {
        
        // Save final AIDA file if option is selected.
        if (jobPanel.isAidaAutoSaveEnabled()) {
            log(Level.INFO, "Auto saving AIDA file <" + jobPanel.getAidaAutoSaveFileName() + ">.");
            try {
                AIDA.defaultInstance().saveAs(jobPanel.getAidaAutoSaveFileName());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error saving AIDA file.");
                e.printStackTrace();
            }
        }        
    }
    
    /**
     * Stop the session by stopping the event processing thread, ending the job,
     * and disconnecting from the ET system.
     */
    private void stopSession() {
        
        Runnable runnable = new Runnable() {

            public void run() {

                logger.log(Level.INFO, "Stopping session.");

                // Request event processing to stop.
                eventProcessing.finish();

                // Wait for the event processing thread to finish.
                try {
                    logger.log(Level.FINER, "Waiting for event processing to finish before disconnecting.");
                    eventProcessingThread.join();
                    logger.log(Level.FINER, "Event processing finished.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Reset event processing objects.
                eventProcessing = null;
                eventProcessingThread = null;

                // Perform various end of job cleanup.
                endJob();

                // Disconnect from the ET server.
                disconnect();

                // Stop the session timer.
                stopSessionTimer();

                logger.log(Level.INFO, "Session done.");
            }
        };
        
        Thread thread = new Thread(runnable);
        thread.start();
    }
}
