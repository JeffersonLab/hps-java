package org.hps.monitoring;

import static org.hps.monitoring.MonitoringCommands.aidaAutoSaveCmd;
import static org.hps.monitoring.MonitoringCommands.clearLogTableCmd;
import static org.hps.monitoring.MonitoringCommands.connectCmd;
import static org.hps.monitoring.MonitoringCommands.disconnectCmd;
import static org.hps.monitoring.MonitoringCommands.eventBuilderCmd;
import static org.hps.monitoring.MonitoringCommands.eventRefreshCmd;
import static org.hps.monitoring.MonitoringCommands.exitCmd;
import static org.hps.monitoring.MonitoringCommands.loadConnectionCmd;
import static org.hps.monitoring.MonitoringCommands.loadJobSettingsCmd;
import static org.hps.monitoring.MonitoringCommands.logLevelCmd;
import static org.hps.monitoring.MonitoringCommands.logToFileCmd;
import static org.hps.monitoring.MonitoringCommands.logToTerminalCmd;
import static org.hps.monitoring.MonitoringCommands.nextCmd;
import static org.hps.monitoring.MonitoringCommands.pauseCmd;
import static org.hps.monitoring.MonitoringCommands.resetConnectionSettingsCmd;
import static org.hps.monitoring.MonitoringCommands.resetDriversCmd;
import static org.hps.monitoring.MonitoringCommands.resetEventsCmd;
import static org.hps.monitoring.MonitoringCommands.resetJobSettingsCmd;
import static org.hps.monitoring.MonitoringCommands.resumeCmd;
import static org.hps.monitoring.MonitoringCommands.saveConnectionCmd;
import static org.hps.monitoring.MonitoringCommands.saveJobSettingsCmd;
import static org.hps.monitoring.MonitoringCommands.saveLogTableCmd;
import static org.hps.monitoring.MonitoringCommands.savePlotsCmd;
import static org.hps.monitoring.MonitoringCommands.screenshotCmd;
import static org.hps.monitoring.MonitoringCommands.setMaxEventsCmd;
import static org.hps.monitoring.MonitoringCommands.steeringFileCmd;
import static org.hps.monitoring.MonitoringCommands.steeringResourceCmd;
import static org.hps.monitoring.MonitoringCommands.updateTimeCmd;

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
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.evio.LCSimEventBuilder;
import org.hps.util.Resettable;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Monitoring application for HPS Test Run, which can run LCSim steering files on data
 * converted from the ET ring. This class is accessible to users by calling its main()
 * method.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: MonitoringApplication.java,v 1.61 2013/12/10 07:36:40 jeremy Exp $
 */
// FIXME: Review minimum size settings to see which are actually being respected. Remove
// where they are not needed.
// FIXME: Since this class is almost 2k lines, might want to refactor it into multiple
// classes.
// TODO: Capture std err and out and redirect to a text panel within the application.
// TODO: Review use of Resettable and Redrawable to see if they can be removed and the
//       standard Driver API used instead.  Resettable can maybe be replaced by startOfData().
//       Not sure about Redrawable; maybe it isn't needed at all.
// FIXME: Tracebacks from errors should be caught and written into the log table.
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

    // References to menu items that will be enabled/disabled depending on application
    // state.
    private JMenuItem connectItem;
    private JMenuItem disconnectItem;
    private JMenuItem resetConnectionItem;
    private JMenuItem connectionLoadItem;
    private JMenuItem savePlotsItem;
    private JMenuItem resetDriversItem;
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
    private ConnectionParameters connectionParameters;
    private EtConnection connection;
    private int connectionStatus = ConnectionStatus.DISCONNECTED;

    // Event processing objects.
    private JobControlManager jobManager;
    private LCSimEventBuilder eventBuilder;
    private EtEventProcessor eventProcessor;
    private Thread eventProcessingThread;

    // Job timing.
    private Timer timer;
    private long jobStartTime;

    // ActionListener for GUI event dispatching.
    private ActionListener actionListener;

    // Logging objects.
    private static Logger logger;
    private Handler logHandler;
    private DefaultTableModel logTableModel;
    static final String[] logTableColumns = { "Source", "Message", "Date", "Level" };
    private JTable logTable;
    private Level defaultLogMessageLevel = Level.INFO;

    // Some default GUI size parameters.
    private final int logTableWidth = 700;
    private final int logTableHeight = 270;

    // Format for screenshots. Hard-coded to PNG.
    private static final String screenshotFormat = "png";

    // Listener for processing EtEvents.
    private EtEventListener etListener = new MonitoringApplicationEtListener();

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

    /**
     * Constructor for the monitoring application. Users cannot access this. Call the main
     * method instead.
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
        c = new GridBagConstraints();
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

        // Tab panels.
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1.0;
        JPanel tabsPanel = new JPanel();
        tabs = new JTabbedPane();
        tabs.addTab("Connection Settings", connectionPanel);
        tabs.addTab("Event Monitor", eventPanel);
        tabs.addTab("Job Settings", jobPanel);
        tabsPanel.add(tabs, c);

        // Add tabs to main panel.
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1.0;
        c.insets = new Insets(0, 0, 0, 10);
        leftPanel.add(tabsPanel, c);

        // Layout attributes for left panel.
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

        connectItem = addMenuItem("Connect", KeyEvent.VK_C, connectCmd, true, "Connect to ET system using parameters from connection panel.", connectionMenu);
        disconnectItem = addMenuItem("Disconnect", KeyEvent.VK_D, disconnectCmd, false, "Disconnect from the current ET session.", connectionMenu);
        resetConnectionItem = addMenuItem("Reset Connection Settings", KeyEvent.VK_R, resetConnectionSettingsCmd, true, "Reset connection settings to defaults.", connectionMenu);
        connectionLoadItem = addMenuItem("Load Connection...", KeyEvent.VK_L, loadConnectionCmd, true, "Load connection settings from a saved properties file.", connectionMenu);
        addMenuItem("Save Connection...", KeyEvent.VK_S, saveConnectionCmd, true, "Save connection settings to a properties file.", connectionMenu);
        addMenuItem("Exit", KeyEvent.VK_X, exitCmd, true, "Exit from the application.", connectionMenu);

        JMenu eventMenu = new JMenu("Event");
        eventMenu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(eventMenu);

        addMenuItem("Reset Event Monitor", KeyEvent.VK_E, resetEventsCmd, true, "Reset timer and counters in the event monitor tab.", eventMenu);

        /**
         * FIXME: Rest of these should be converted to use the addMenuItem() helper
         * method...
         */

        JMenuItem eventRefreshItem = new JMenuItem("Set Event Refresh...");
        eventRefreshItem.setMnemonic(KeyEvent.VK_V);
        eventRefreshItem.setActionCommand(eventRefreshCmd);
        eventRefreshItem.addActionListener(actionListener);
        eventRefreshItem.setToolTipText("Set the number of events between GUI updates.");
        eventMenu.add(eventRefreshItem);

        JMenuItem maxEventsItem = new JMenuItem("Set Max Events...");
        maxEventsItem.setMnemonic(KeyEvent.VK_M);
        maxEventsItem.setActionCommand(setMaxEventsCmd);
        maxEventsItem.addActionListener(actionListener);
        maxEventsItem.setToolTipText("Set the maximum number of events to process in one session.");
        eventMenu.add(maxEventsItem);

        JMenu jobMenu = new JMenu("Job");
        jobMenu.setMnemonic(KeyEvent.VK_J);
        menuBar.add(jobMenu);

        saveJobSettingsItem = new JMenuItem("Save Job Settings...");
        saveJobSettingsItem.setMnemonic(KeyEvent.VK_J);
        saveJobSettingsItem.setActionCommand(saveJobSettingsCmd);
        saveJobSettingsItem.addActionListener(actionListener);
        saveJobSettingsItem.setToolTipText("Save Job Settings configuration to a properties file.");
        jobMenu.add(saveJobSettingsItem);

        loadJobSettingsItem = new JMenuItem("Load Job Settings...");
        loadJobSettingsItem.setMnemonic(KeyEvent.VK_L);
        loadJobSettingsItem.setActionCommand(loadJobSettingsCmd);
        loadJobSettingsItem.addActionListener(actionListener);
        loadJobSettingsItem.setToolTipText("Load Job Settings from a properties file.");
        jobMenu.add(loadJobSettingsItem);

        resetJobSettingsItem = new JMenuItem("Reset Job Settings");
        resetJobSettingsItem.setMnemonic(KeyEvent.VK_R);
        resetJobSettingsItem.setActionCommand(resetJobSettingsCmd);
        resetJobSettingsItem.addActionListener(actionListener);
        resetJobSettingsItem.setToolTipText("Reset Job Settings to the defaults.");
        jobMenu.add(resetJobSettingsItem);

        steeringItem = new JMenuItem("Set Steering File...");
        steeringItem.setMnemonic(KeyEvent.VK_S);
        steeringItem.setActionCommand(steeringFileCmd);
        steeringItem.addActionListener(actionListener);
        steeringItem.setToolTipText("Set the job's LCSim steering file.");
        jobMenu.add(steeringItem);

        aidaAutoSaveItem = new JMenuItem("AIDA Auto Save File...");
        aidaAutoSaveItem.setMnemonic(KeyEvent.VK_A);
        aidaAutoSaveItem.setActionCommand(aidaAutoSaveCmd);
        aidaAutoSaveItem.addActionListener(actionListener);
        aidaAutoSaveItem.setToolTipText("Select name of file to auto save AIDA plots at end of job.");
        jobMenu.add(aidaAutoSaveItem);

        savePlotsItem = new JMenuItem("Save Plots to AIDA File...");
        savePlotsItem.setMnemonic(KeyEvent.VK_P);
        savePlotsItem.setActionCommand(savePlotsCmd);
        savePlotsItem.addActionListener(actionListener);
        savePlotsItem.setEnabled(false);
        savePlotsItem.setToolTipText("Save plots from default AIDA tree to an output file.");
        jobMenu.add(savePlotsItem);

        resetDriversItem = new JMenuItem("Reset LCSim Drivers");
        resetDriversItem.setMnemonic(KeyEvent.VK_D);
        resetDriversItem.setActionCommand(resetDriversCmd);
        resetDriversItem.addActionListener(actionListener);
        resetDriversItem.setEnabled(false);
        resetDriversItem.setToolTipText("Reset Drivers that implement the Resettable interface.");
        jobMenu.add(resetDriversItem);

        logItem = new JMenuItem("Redirect to File...");
        logItem.setMnemonic(KeyEvent.VK_F);
        logItem.setActionCommand(logToFileCmd);
        logItem.addActionListener(actionListener);
        logItem.setEnabled(true);
        logItem.setToolTipText("Redirect job's standard out and err to a file.");
        jobMenu.add(logItem);

        terminalItem = new JMenuItem("Redirect to Terminal");
        terminalItem.setMnemonic(KeyEvent.VK_T);
        terminalItem.setActionCommand(logToTerminalCmd);
        terminalItem.addActionListener(actionListener);
        terminalItem.setEnabled(false);
        terminalItem.setToolTipText("Redirect job's standard out and err back to the terminal.");
        jobMenu.add(terminalItem);

        JMenuItem screenshotItem = new JMenuItem("Take a screenshot...");
        screenshotItem.setMnemonic(KeyEvent.VK_N);
        screenshotItem.setActionCommand(screenshotCmd);
        screenshotItem.addActionListener(actionListener);
        screenshotItem.setToolTipText("Save a full screenshot to a " + screenshotFormat + " file.");
        jobMenu.add(screenshotItem);

        JMenu logMenu = new JMenu("Log");
        jobMenu.setMnemonic(KeyEvent.VK_L);
        menuBar.add(logMenu);

        JMenuItem saveLogItem = new JMenuItem("Save log to file...");
        saveLogItem.setMnemonic(KeyEvent.VK_S);
        saveLogItem.setActionCommand(saveLogTableCmd);
        saveLogItem.addActionListener(actionListener);
        saveLogItem.setToolTipText("Save the log records to a tab delimited text file.");
        logMenu.add(saveLogItem);

        addMenuItem("Clear log", KeyEvent.VK_C, clearLogTableCmd, true, "Clear the log table of all messages.", logMenu);
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
            Object[] row = new Object[] { record.getLoggerName(), // source
                    record.getMessage(), // message
                    dateFormat.format(new Date(record.getMillis())), // date
                    record.getLevel() }; // level
            logTableModel.insertRow(logTable.getRowCount(), row);
        }

        /**
         * Close the handler (no-op).
         */
        public void close() throws SecurityException {
        }

        /**
         * Flush the handler (no-op).
         */
        public void flush() {
        }
    }

    /**
     * Creates the application's log table GUI component, which is a JTable containing
     * messages from the logger.
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
    private static final MonitoringApplication createMonitoringApplication() {
        final MonitoringApplication app = new MonitoringApplication();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                app.createApplicationFrame();
            }
        });
        return app;
    }

    /**
     * Run the monitoring application from the command line.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {

        // Set up command line parsing.
        Options options = new Options();
        options.addOption(new Option("h", false, "Print help."));
        options.addOption(new Option("c", true, "Load properties file with connection settings."));
        options.addOption(new Option("j", true, "Load properties file with job settings."));
        CommandLineParser parser = new PosixParser();

        // Parse command line arguments.
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        // Print help and exit.
        if (cl.hasOption("h")) {
            System.out.println("MonitoringApplication [options]");
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }

        // Create the application class.
        MonitoringApplication app = MonitoringApplication.createMonitoringApplication();

        // Load the connection settings.
        if (cl.hasOption("c")) {
            app.loadConnectionSettings(new File(cl.getOptionValue("c")));
        }

        // Load the job settings.
        if (cl.hasOption("j")) {
            app.loadJobSettings(new File(cl.getOptionValue("j")));
        }
    }

    /**
     * Load connection settings from a file.
     * @param file The properties file.
     */
    private void loadConnectionSettings(File file) {
        connectionPanel.loadPropertiesFile(file);
    }

    /**
     * Load job settings from a file.
     * @param file The properties file.
     */
    private void loadJobSettings(File file) {
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
            if (cmd != MonitoringCommands.updateTimeCmd) {
                // Log actions performed. Catch errors in case logging is not initialized
                // yet.
                try {
                    log(Level.FINEST, "Action performed <" + cmd + ">.");
                } catch (Exception xx) {
                    xx.printStackTrace();
                }
            }
            if (connectCmd.equals(cmd)) {
                startSessionThread();
            } else if (disconnectCmd.equals(cmd)) {
                startDisconnectThread();
            } else if (eventRefreshCmd.equals(cmd)) {
                setEventRefresh();
            } else if (savePlotsCmd.equals(cmd)) {
                savePlots();
            } else if (resetDriversCmd.equals(cmd)) {
                resetDrivers();
            } else if (logToFileCmd.equals(cmd)) {
                logToFile();
            } else if (logToTerminalCmd.equals(cmd)) {
                logToTerminal();
            } else if (screenshotCmd.equals(cmd)) {
                chooseScreenshot();
            } else if (exitCmd.equals(cmd)) {
                exit();
            } else if (updateTimeCmd.equals(cmd)) {
                updateTime();
            } else if (resetEventsCmd.equals(cmd)) {
                resetJob();
            } else if (saveConnectionCmd.equals(cmd)) {
                connectionPanel.save();
            } else if (loadConnectionCmd.equals(cmd)) {
                connectionPanel.load();
            } else if (resetConnectionSettingsCmd.equals(cmd)) {
                connectionPanel.reset();
            } else if (setMaxEventsCmd.equals(cmd)) {
                setMaxEvents();
            } else if (saveLogTableCmd.equals(cmd)) {
                saveLogToFile();
            } else if (clearLogTableCmd.equals(cmd)) {
                clearLog();
            } else if (eventBuilderCmd.equals(cmd)) {
                jobPanel.editEventBuilder();
            } else if (pauseCmd.equals(cmd)) {
                pause();
            } else if (nextCmd.equals(cmd)) {
                next();
            } else if (resumeCmd.equals(cmd)) {
                resume();
            } else if (logLevelCmd.equals(cmd)) {
                setLogLevel();
            } else if (aidaAutoSaveCmd.equals(cmd)) {
                jobPanel.chooseAidaAutoSaveFile();
            } else if (saveJobSettingsCmd.equals(cmd)) {
                saveJobSettings();
            } else if (loadJobSettingsCmd.equals(cmd)) {
                loadJobSettings();
            } else if (resetJobSettingsCmd.equals(cmd)) {
                resetJobSettings();
            } else if (steeringResourceCmd.equals(cmd)) {
                steeringResourceSelected();
            } else if (steeringFileCmd.equals(cmd)) {
                selectSteeringFile();
            }
        }
    }

    /**
     * This fires when a steering resource file is selected from the combo box. The Job
     * Settings are changed to use a resource type.
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
     * This is the primary entry point for starting a monitoring session. The session is
     * started on a new thread so it doesn't block.
     */
    private void startSessionThread() {
        if (getConnectionStatus() != ConnectionStatus.CONNECTED) {
            Runnable r = new Runnable() {
                public void run() {
                    session();
                }
            };
            Thread t = new Thread(r, "Session Thread");
            t.start();
        } else {
            log(Level.SEVERE, "Ignoring connection request.  Already connected!");
        }
    }

    /**
     * Set a new log level for the application and also forward to the event processor.
     */
    private void setLogLevel() {
        Level newLevel = jobPanel.getLogLevel();
        logger.setLevel(newLevel);
        if (eventProcessor != null) {
            eventProcessor.setLogLevel(newLevel);
        }

        log(Level.INFO, "Log Level was changed to <" + jobPanel.getLogLevel().toString() + ">.");
    }

    /**
     * The listener for hooking into the event processor.
     */
    private class MonitoringApplicationEtListener implements EtEventListener {

        /**
         * Beginning of job.
         */
        public void begin() {

            // Reset event GUI.
            eventPanel.reset();

            // This is only reset between different jobs.
            eventPanel.resetSessionSupplied();

            // Start the job timer.
            startTimer();
        }

        /**
         * Start of next event.
         */
        public void startOfEvent() {
            eventPanel.updateEventCount();
        }

        /**
         * End of single event.
         */
        public void endOfEvent() {
            eventPanel.updateAverageEventRate(jobStartTime);
        }

        /**
         * Error on this event.
         */
        public void errorOnEvent() {
            eventPanel.updateBadEventCount();
        }

        /**
         * End of job actions. This cleans up the Monitoring Application to put it into
         * the proper state for subsequent disconnection from the ET ring.
         */
        public void finish() {

            // Show a warning dialog box before disconnecting, if this option is selected.
            // This needs to go here rather than in disconnect() so that the LCSim plots
            // stay up.
            if (warnOnDisconnect()) {
                log(Level.FINEST, "Waiting for user to verify disconnect request.");
                showDialog("You are about to be disconnected.");
                // DisconnectDialog d = new DisconnectDialog();
                // d.waitForConfirm();
            }

            try {

                // Save final AIDA file if option is selected.
                if (jobPanel.isAidaAutoSaveEnabled()) {
                    log(Level.INFO, "Auto saving AIDA file <" + jobPanel.getAidaAutoSaveFileName() + ">.");
                    AIDA.defaultInstance().saveAs(jobPanel.getAidaAutoSaveFileName());
                }

                // Call cleanup methods of Drivers.
                try {
                    log(Level.INFO, "Cleaning up LCSim.");
                    if (jobManager != null) {
                        jobManager.finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.log(Level.WARNING, "Error cleaning up LCSim job.");
                }

                // Stop the job timer.
                log(Level.INFO, "Stopping the job timer.");
                timer.stop();
                timer = null;

                // Push final event counts to GUI.
                eventPanel.endJob();
                
            } catch (Exception e) {
                e.printStackTrace();
                log(Level.WARNING, "Error cleaning up job <" + e.getMessage() + ">.");
            }
        }

        /**
         * Prestart event received.
         */
        public void prestart(int seconds, int runNumber) {
            final long millis = ((long) seconds) * 1000;
            eventPanel.setRunNumber(runNumber);
            eventPanel.setRunStartTime(millis);
            log(Level.INFO, "Set run number <" + runNumber + "> from Pre Start.");
            log(Level.INFO, "Run start time <" + EventPanel.dateFormat.format(new Date(millis)) + "> from Pre Start.");
        }

        /**
         * End run event received.
         */
        public void endRun(int seconds, int events) {
            final long millis = ((long) seconds) * 1000;
            eventPanel.setRunEndTime(millis);
            eventPanel.setRunEventCount(events);
            log(Level.INFO, "Set number of events in run to <" + events + ">.");
            log(Level.INFO, "End run time <" + EventPanel.dateFormat.format(new Date(millis)) + ">.");
        }
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

        // Maximize frame size.
        // final GraphicsConfiguration config = frame.getGraphicsConfiguration();
        // final int left = Toolkit.getDefaultToolkit().getScreenInsets(config).left;
        // final int right = Toolkit.getDefaultToolkit().getScreenInsets(config).right;
        // final int top = Toolkit.getDefaultToolkit().getScreenInsets(config).top;
        // final int bottom = Toolkit.getDefaultToolkit().getScreenInsets(config).bottom;
        // final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        // final int width = screenSize.width - left - right;
        // final int height = screenSize.height - top - bottom;
        // frame.setSize(width,height);

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
     * Select an LCSim steering file from disk.
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
     * Call the reset() method on Drivers which implement {@link Resettable}. They must
     * implement the {@link Resettable} interface for this to work.
     */
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

    /**
     * Redirect System.out and System.err to a file. This is primarily used to capture
     * lengthy debug output from event processing. Messages sent to the Logger are
     * unaffected.
     */
    private void logToFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Log File");
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
     * Redirect <code>System.out</code> and <code>System.err</code> back to the terminal, 
     * e.g. if they were previously sent to a file. This is independent of messages that 
     * are sent to the application's log table.
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
     * Using a modal dialog, set the maximum number of events to process before an
     * automatic disconnect.
     */
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
            if (eventProcessor != null) {
                eventProcessor.setMaxEvents(newMaxEvents);
            }
            log("Max events set to <" + newMaxEvents + ">.");
        } catch (Exception e) {
            e.printStackTrace();
            log(Level.WARNING, "Ignored invalid max events setting <" + inputValue + ">.");
            showDialog("The value " + inputValue + " is not valid for Max Events.");
        }
    }

    /**
     * Set the GUI state to disconnected, which will enable/disable applicable GUI
     * components and menu items.
     */
    private void setDisconnectedGuiState() {

        // Enable or disable appropriate menu items.
        connectItem.setEnabled(true);
        disconnectItem.setEnabled(false);
        resetConnectionItem.setEnabled(true);
        connectionLoadItem.setEnabled(true);
        savePlotsItem.setEnabled(false);
        resetDriversItem.setEnabled(false);
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
     * Set the GUI to connected state, which will enable/disable appropriate components
     * and menu items.
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
        resetDriversItem.setEnabled(true);
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
            cleanupConnection();
        }
        System.exit(0);
    }

    /**
     * Save a screenshot using a file chooser.
     */
    private void chooseScreenshot() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Screenshot");
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
     * Get the fully qualified class name of the current event builder for converting from
     * EVIO to LCIO. 
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
    private int getMaxEvents() {
        return eventPanel.getMaxEvents();
    }

    /**
     * Execute a monitoring session. This is executed in a separate thread so as not to
     * block the GUI or other threads during a monitoring session.
     */
    private void session() {

        log(Level.INFO, "Starting a new monitoring session.");

        int endStatus = ConnectionStatus.DISCONNECTING;

        try {

            // Setup LCSim.
            setupLCSim();

            // Connect to the ET system.
            connect();

            // TODO: Add EtEventCounter thread here for counting all ET events.

            // Create the event processing thread.
            createEventProcessingThread();

            // Wait for the event processing thread to finish.
            try {
                eventProcessingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log("Event processor finished with status <" + ConnectionStatus.toString(eventProcessor.getStatus()) + ">.");
            endStatus = eventProcessor.getStatus();
        } catch (Exception e) {
            e.printStackTrace();
            log(Level.SEVERE, "Fatal error in monitoring session.");
            endStatus = ConnectionStatus.ERROR;
        } finally {
            logHandler.flush();
            // Disconnect if needed.
            if (getConnectionStatus() != ConnectionStatus.DISCONNECTED) {
                disconnect(endStatus);
            }
        }

        log("Finished monitoring session.");
    }

    /**
     * Create the thread that will execute the EtEvent processing chain.
     * @return The thread on which event processing will occur.
     */
    private void createEventProcessingThread() {

        // Create a new event processor.
        eventProcessor = new DefaultEtEventProcessor(this.connection, this.eventBuilder, this.jobManager, getMaxEvents(), disconnectOnError(), this.logHandler);

        // Add the application's listener for callbacks to the GUI components.
        eventProcessor.addListener(this.etListener);

        // Set pause mode from JobPanel, after which it can be toggled using the event
        // buttons.
        eventProcessor.pauseMode(this.jobPanel.pauseMode());

        // Create a new thread for event processing.
        Runnable run = new Runnable() {
            public void run() {
                eventProcessor.process();
            }
        };
        eventProcessingThread = new Thread(run, "Event Processing Thread");

        // Start the event processing thread.
        eventProcessingThread.start();

        log(Level.FINEST, "Started event processing thread.");
        logHandler.flush();
    }

    /**
     * Connect to the ET system specified in the GUI's connection panel settings.
     */
    private void connect() {

        log("Connecting to ET system.");

        setConnectionStatus(ConnectionStatus.CONNECTION_REQUESTED);

        // Make sure applicable menu items are enabled or disabled.
        setConnectedGuiState();

        // Create a connection to the ET server.
        createEtConnection();

        log("Successfully connected to ET system.");
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
    private ConnectionParameters getConnectionParameters() {
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

        log("Disconnecting from ET system with status <" + ConnectionStatus.toString(status) + ">.");

        // Check if disconnected already.
        if (getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
            log(Level.WARNING, "ET system is already disconnected.");
            return;
        }

        // Check if in the process of disconnecting.
        if (getConnectionStatus() == ConnectionStatus.DISCONNECTING) {
            log(Level.WARNING, "ET system is already disconnecting.");
            return;
        }

        // Stop event processing if currently connected.
        if (eventProcessor != null) {
            log(Level.FINE, "Stopping the event processor.");
            eventProcessor.stop();
        }

        // Set the application status from the caller.
        setConnectionStatus(status);

        // Cleanup the ET session.
        cleanupConnection();

        // Update state of GUI to disconnected.
        setDisconnectedGuiState();

        // Finally, change application state to fully disconnected.
        setConnectionStatus(ConnectionStatus.DISCONNECTED);

        log("Disconnected from ET system.");
    }

    /**
     * This is a thread for cleaning up the ET connection. This is executed under a
     * separate thread, because it could potentially block forever. So we need to be able
     * to kill it after waiting for X amount of time.
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
    private void cleanupConnection() {

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
                log(Level.SEVERE, "EtCleanupThread failed to disconnect.  Your station <" + this.connection.stat.getName() + "> is zombified.");
                // Make the cleanup thread yield.
                cleanupThread.stopCleanup();
                // Stop the cleanup thread.
                // FIXME: Should call yield() instead?
                cleanupThread.stop();
                // Join to cleanup thread until it dies.
                log(Level.FINEST, "Waiting for EtCleanupThread to die");
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

        // Clear the static AIDA tree in case plots are hanging around from previous sessions.
        resetAidaTree();

        // Reset the top plots panel so that it is empty.
        plotPane.removeAll();
        //((MonitoringAnalysisFactory)MonitoringAnalysisFactory.create()).clearPlotterFactories();

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
            // Create job manager and configure based on steering type of resource or
            // file.
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

            // Call configure to trigger conditions setup and other initialization.
            jobManager.configure();

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
     * Disconnect from the ET system using a separate thread of execution.
     */
    private void startDisconnectThread() {
        Runnable r = new Runnable() {
            public void run() {
                disconnect();
            }
        };
        Thread t = new Thread(r, "Disconnect Thread");
        t.start();
    }

    /**
     * Create a connection to an ET system using current parameters from the GUI. If
     * successful, the application's ConnectionStatus is changed to CONNECTED.
     */
    private void createEtConnection() {

        // Cache connection parameters from GUI to local variable.
        connectionParameters = getConnectionParameters();

        // Setup connection to ET system.
        connection = EtConnection.createEtConnection(connectionParameters);

        if (connection != null) {

            // Set status to connected as there is now a live ET connection.
            setConnectionStatus(ConnectionStatus.CONNECTED);

            log(Level.CONFIG, "Created ET connection to <" + connectionParameters.etName + ">.");
        } else {
            // Some error occurred and the connection is not valid.
            setConnectionStatus(ConnectionStatus.ERROR);
            log(Level.SEVERE, "Failed to create ET connection to <" + connectionParameters.etName + ">.");
            throw new RuntimeException("Failed to create ET connection.");
        }
    }

    /**
     * Start the job timer.
     */
    private void startTimer() {
        timer = new Timer(1000, actionListener);
        timer.setActionCommand(updateTimeCmd);
        jobStartTime = System.currentTimeMillis();
        timer.start();
        log(Level.FINE, "Job timer started.");
    }

    /**
     * Update the elapsed time in the GUI.
     */
    private void updateTime() {
        final long elapsedTime = (System.currentTimeMillis() - jobStartTime) / 1000;
        eventPanel.setElapsedTime(elapsedTime);
    }

    /**
     * Reset the event panel.
     */
    private void resetJob() {
        // Reset GUI.
        jobStartTime = System.currentTimeMillis();
        eventPanel.reset();
        if (getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
            eventPanel.resetSessionSupplied();
        }
        // Reset event processor.
        eventProcessor.resetNumberOfEventsProcessed();
        log(Level.FINE, "Job was reset.");
    }

    /**
     * Save the accumulated log messages to a tab-delimited text file selected using a
     * file chooser.
     */
    private void saveLogToFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Log File");
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
    private void next() {
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            log(Level.FINER, "Notifying event processor to get next events.");
            eventProcessor.nextEvents();
        } else {
            log(Level.WARNING, "Ignored next events command because app is disconnected.");
        }
    }

    /**
     * Notify the event processor to resume processing events in real-time mode, if
     * paused.
     */
    private void resume() {
        if (connected()) {
            // Notify event processor to continue.
            eventProcessor.pauseMode(false);
            eventProcessor.nextEvents();

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
    private void pause() {
        if (connected()) {
            eventProcessor.pauseMode(true);
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
}
