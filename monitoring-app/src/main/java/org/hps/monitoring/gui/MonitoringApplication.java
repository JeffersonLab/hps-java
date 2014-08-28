package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.Commands.AIDA_AUTO_SAVE;
import static org.hps.monitoring.gui.Commands.CHOOSE_LOG_FILE;
import static org.hps.monitoring.gui.Commands.CLEAR_LOG_TABLE;
import static org.hps.monitoring.gui.Commands.CONNECT;
import static org.hps.monitoring.gui.Commands.DISCONNECT;
import static org.hps.monitoring.gui.Commands.EXIT;
import static org.hps.monitoring.gui.Commands.LOAD_DEFAULT_CONFIG_FILE;
import static org.hps.monitoring.gui.Commands.LOG_LEVEL_CHANGED;
import static org.hps.monitoring.gui.Commands.LOG_TO_TERMINAL;
import static org.hps.monitoring.gui.Commands.NEXT;
import static org.hps.monitoring.gui.Commands.PAUSE;
import static org.hps.monitoring.gui.Commands.RESUME;
import static org.hps.monitoring.gui.Commands.SAVE_CONFIG_FILE;
import static org.hps.monitoring.gui.Commands.SAVE_LOG_TABLE;
import static org.hps.monitoring.gui.Commands.SAVE_PLOTS;
import static org.hps.monitoring.gui.Commands.SCREENSHOT;
import static org.hps.monitoring.gui.Commands.SELECT_CONFIG_FILE;
import static org.hps.monitoring.gui.Commands.SHOW_SETTINGS;

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
import org.hps.record.DataSourceType;
import org.hps.record.chain.EventProcessingChain;
import org.hps.record.chain.EventProcessingConfiguration;
import org.hps.record.chain.EventProcessingThread;
import org.hps.record.etevent.EtConnection;
import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This class is the implementation of the GUI for the Monitoring Application.
 */
public final class MonitoringApplication extends JFrame implements ActionListener, SystemStatusListener {

    // Top-level Swing components.
    private JPanel mainPanel;
    private EventButtonsPanel buttonsPanel;
    private ConnectionStatusPanel connectionStatusPanel;
    private RunPanel runPanel;
    private JMenuBar menuBar;
    private SettingsDialog settingsDialog;
    private PlotFrame plotFrame;
    private SystemStatusFrame systemStatusFrame;

    // References to menu items that will be toggled depending on application state.
    private JMenuItem savePlotsItem;
    private JMenuItem logItem;
    private JMenuItem terminalItem;

    // Saved references to System.out and System.err in case need to reset.
    private final PrintStream sysOut = System.out;
    private final PrintStream sysErr = System.err;
    
    // Error handling class for the application.
    private ErrorHandler errorHandler;

    // ET connection parameters and state.
    private EtConnection connection;
    //private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;

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
    static final String[] logTableColumns = { "Date", "Message", "Level" };
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
    private static final int MAIN_FRAME_HEIGHT = ScreenUtil.getScreenHeight() / 2;
    private static final int MAIN_FRAME_WIDTH = 650;
    
    // Default config which can be overridden by command line argument.
    private static final String DEFAULT_CONFIG_RESOURCE = "/org/hps/monitoring/config/default_config.prop";
    
    // The application global Configuration object which is the default configuration unless overridden.
    private Configuration configuration = new Configuration(DEFAULT_CONFIG_RESOURCE);
    
    // The ConfigurationModel for updating GUI components from the global configuration.
    private ConfigurationModel configurationModel = new ConfigurationModel();
    
    // The RunModel for updating the RunPanel.
    private RunModel runModel = new RunModel();
                   
    /**
     * Constructor for the monitoring application.
     */
    public MonitoringApplication() {
    }
        
    /**
     * Perform all intialization on start up.
     */
    public void initialize() {
        
        // Create and configure the logger.
        setupLogger();
        
        // Setup the error handling class.
        setupErrorHandler();
        
        // Setup an uncaught exception handler.
        setupUncaughtExceptionHandler();

        // Setup the application menus.
        createApplicationMenu();

        // Create the main GUI panel.
        createMainPanel();

        // Create the log table GUI component.
        createLogTable();

        // Configuration of window for showing plots.
        createPlotFrame();
        
        // Create the system status window.
        createSystemStatusFrame();

        // Setup AIDA.
        setupAida();

        // Configure the application's primary JFrame.
        configApplicationFrame();

        // Create settings dialog window.
        createSettingsDialog();
        
        // Register the ConfigurationModel with sub-components.
        setupConfigurationModel();
                
        // Load the current configuration, either the default or from command line arg.
        loadConfiguration();

        // Log that the application started successfully.
        log(Level.CONFIG, "Application initialized successfully.");
    }
         
    private void setupErrorHandler() {
        errorHandler = new ErrorHandler(this, logger);
    }
    
    private void setupUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {            
            public void uncaughtException(Thread thread, Throwable exception) {
               MonitoringApplication.this.errorHandler.setError(exception)
                   .log()
                   .printStackTrace()
                   .showErrorDialog();
            }
        });
    }
            
    private void createSettingsDialog() {
        settingsDialog = new SettingsDialog();
        settingsDialog.getSettingsPanel().addActionListener(this);
        getJobSettingsPanel().addActionListener(this);
    }

    private void createPlotFrame() {
        plotFrame = new PlotFrame();                
        plotFrame.setSize(SCREEN_WIDTH - MAIN_FRAME_WIDTH, SCREEN_HEIGHT);
        plotFrame.setLocation(
                (int)(ScreenUtil.getBoundsX(0)) + MAIN_FRAME_WIDTH,
                plotFrame.getY());
    }
    
    private void createSystemStatusFrame() {
        systemStatusFrame = new SystemStatusFrame();
        systemStatusFrame.setLocation(
                (int)ScreenUtil.getBoundsX(0),
                MAIN_FRAME_HEIGHT);
    }
    
    public void setVisible(boolean visible) {
        
        super.setVisible(true);
        
        this.systemStatusFrame.setVisible(true);
        
        // FIXME: If this is done earlier before app is visible, the GUI will fail to show!
        this.connectionStatusPanel.setConnectionStatus(ConnectionStatus.DISCONNECTED);
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
    private void createApplicationMenu() {

        menuBar = new JMenuBar();

        JMenu applicationMenu = new JMenu("Application");
        applicationMenu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(applicationMenu);               
        
        JMenuItem loadConfigItem = new JMenuItem("Load Settings ...");
        loadConfigItem.addActionListener(this);
        loadConfigItem.setMnemonic(KeyEvent.VK_C);
        loadConfigItem.setActionCommand(SELECT_CONFIG_FILE);
        loadConfigItem.setToolTipText("Load application settings from a properties file.");
        applicationMenu.add(loadConfigItem);
        
        JMenuItem saveConfigItem = new JMenuItem("Save Settings ...");
        saveConfigItem.addActionListener(this);
        saveConfigItem.setMnemonic(KeyEvent.VK_S);
        saveConfigItem.setActionCommand(SAVE_CONFIG_FILE);        
        saveConfigItem.setToolTipText("Save settings to a properties file.");
        applicationMenu.add(saveConfigItem);
        
        JMenuItem settingsItem = new JMenuItem("Show Settings ...");
        settingsItem.setMnemonic(KeyEvent.VK_P);
        settingsItem.setActionCommand(SHOW_SETTINGS);
        settingsItem.addActionListener(this);
        settingsItem.setToolTipText("Show application settings menu.");
        applicationMenu.add(settingsItem);
                
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setActionCommand(EXIT);
        exitItem.addActionListener(this);
        exitItem.setToolTipText("Exit from the application.");
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
        screenshotItem.setToolTipText("Save a full screenshot to a " + screenshotFormat + " file.");
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
            Object[] row = new Object[] { 
                    dateFormat.format(new Date(record.getMillis())), 
                    record.getLevel(),
                    record.getMessage() };
            logTableModel.insertRow(logTable.getRowCount(), row);
            
            // Print all messages to System.out so they show up in the terminal or log file output.
            System.out.println(row[0] + " :: " + row[1] + " :: " + row[2]);
        }

        public void close() throws SecurityException {
        }

        public void flush() {
        }
    }

    /**
     * Creates the log table component, which is a JTable containing messages
     * from the logger.
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
            showSettingsWindow();
        } else if (SELECT_CONFIG_FILE.equals(cmd)) {
            chooseConfigurationFile();
        } else if (SAVE_CONFIG_FILE.equals(cmd)) {
            saveConfigurationFile();
        } else if (LOAD_DEFAULT_CONFIG_FILE.equals(cmd)) {
            loadDefaultConfigFile();
        } 
    }

    /**
     * Show the settings window.
     */
    private void showSettingsWindow() {
        settingsDialog.setVisible(true);
    }
       
    /**
     * Set a new log level for the application.  If the new log level is the same as the old one, 
     * a new log level will NOT be set.
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
        //connectionStatus = status;
        connectionStatusPanel.setConnectionStatus(status);
        log(Level.FINE, "Connection status changed to <" + status.name() + ">");
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
                errorHandler.setError(e)
                    .setMessage("Error saving plots to file.")
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
                
                // FIXME: These should be toggled via a PropertyChangeListener or ActionEvent.
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
                // FIXME: These should be toggled via PropertyChangeListener on the ConfigurationModel.
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
                getJobSettingsPanel().enableJobPanel(true);

                // Set relevant event panel buttons to disabled.
                buttonsPanel.enablePauseButton(false);
                buttonsPanel.enableNextEventsButton(false);

                // Toggle connection button to proper setting.
                buttonsPanel.setConnected(false);
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
                getConnectionSettingsPanel().enableConnectionPanel(false);

                // Disable getJobPanel().
                getJobSettingsPanel().enableJobPanel(false);

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
        if (plotFrame.isVisible())
            plotFrame.setVisible(false);
        if (systemStatusFrame.isVisible())
            systemStatusFrame.setVisible(false);
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
            if ((extIndex == -1) || !(fileName.substring(extIndex + 1, fileName.length())).toLowerCase().equals(screenshotFormat)) {
                fileName = fileName + "." + screenshotFormat;
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
     * Start a new monitoring session.  This method is executed in a separate thread from the EDT
     * within {@link #actionPerformed(ActionEvent)} so GUI updates are not blocked while the session 
     * is being setup.
     */
    private void startSession() {

        log(Level.FINE, "Starting a new monitoring session.");
        
        // Show a modal window that will block the GUI until connected or an error occurs.
        JDialog dialog = showStatusDialog("Info", "Starting new session ...", true);
        
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
            setupEventProcessingChain();
            //setupEventProcessingChainNew();
            
            // Setup the system status monitor table.
            setupSystemStatusMonitor();

            // Start thread which will trigger a disconnect if the event processing finishes.
            startSessionWatchdogThread();

            log(Level.INFO, "Successfully started the monitoring session.");

        } catch (Exception e) {
            
            log(Level.SEVERE, "An error occurred while setting up the session.");
            
            // Log the error that occurred.
            errorHandler.setError(e)
                .log()
                .printStackTrace();
                /*.showErrorDialog("Error setting up the session.");*/
            
            // Disconnect from the session.
            //if (this.connected())
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
            if (connection.getEtSystem().alive()) {
                connection.cleanup();
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
            // Create job manager and configure.
            jobManager = new JobControlManager();
            jobManager.setPerformDryRun(true);
            if (steeringType == SteeringType.RESOURCE) {
                setupSteeringResource(steering);
            } else if (steeringType.equals(SteeringType.FILE)) {
                setupSteeringFile(steering);
            }

            // Setup the event builder to translate from EVIO to LCIO.
            createEventBuilder();
            
            log(Level.INFO, "LCSim setup was successful.");

        } catch (Throwable t) {
            // Catch all errors and rethrow them as RuntimeExceptions.
            errorHandler.setError(t)
                .setMessage("Error setting up LCSim.")
                .printStackTrace()
                .raiseException();
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

        // Set the detector name on the event builder so it can find conditions data.
        eventBuilder.setDetectorName(configurationModel.getDetectorName());

        log(Level.CONFIG, "Successfully initialized event builder <" + eventBuilderClassName + ">");
    }

    /**
     * Create a connection to an ET system using current parameters from the GUI. If successful,
     * the application's ConnectionStatus is changed to CONNECTED.
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
            
            errorHandler.setError(new RuntimeException("Failed to create ET connection."))
                .log()
                .printStackTrace()
                .raiseException();
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
    private void clearLogTable() {
        logTableModel.setRowCount(0);
        log(Level.INFO, "Log table was cleared.");
    }
    
    /**
     * Notify event processor to get next set of events, if in pause mode.
     */
    private void nextEvent() {
        this.setConnectionStatus(ConnectionStatus.CONNECTED);
        eventProcessing.next();
        log(Level.FINEST, "Getting next event.");
        this.setConnectionStatus(ConnectionStatus.PAUSED);
    }

    /**
     * Notify the event processor to resume processing events, if paused.
     */
    private void resumeEventProcessing() {
        // Notify event processor to continue.
        eventProcessing.resume();

        // Set state of event buttons.
        buttonsPanel.setPauseModeState(false);

        log(Level.FINEST, "Resuming event processing after pause.");
        
        this.setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    /**
     * Notify the event processor to start pause mode, which will pause between events.
     */
    private void pauseEventProcessing() {
       
        eventProcessing.pause();

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
    private void setupEventProcessingChain() {
        
        EventProcessingConfiguration configuration = new EventProcessingConfiguration();
        
        configuration.setStopOnEndRun(configurationModel.getDisconnectOnEndRun());        
        // FIXME: This doesn't work properly in the event processing chain right now so hard code to true
        //        until that is fixed.  (Need to talk with Dima about it.)
        //configurationModel.getDisconnectOnError();
        configuration.setStopOnErrors(true);
         
        configuration.setDataSourceType(configurationModel.getDataSourceType());
        configuration.setEtConnection(connection);        
        configuration.setFilePath(configurationModel.getDataSourcePath());
        configuration.setLCSimEventBuild(eventBuilder);
        configuration.setDetectorName(configurationModel.getDetectorName());                
               
        // Add all Drivers from the pre-configured JobManager.
        for (Driver driver : jobManager.getDriverExecList()) {
            configuration.add(driver);
        }        
               
        // ET system monitor processor.
        configuration.add(new EtSystemMonitor());
            
        // ET system strip charts processor.
        configuration.add(new EtSystemStripCharts());
              
        // RunPanel updater processor.
        configuration.add(runPanel.new RunModelUpdater());
        
        // Create the EventProcessingChain object.
        eventProcessing = new EventProcessingChain(configuration);
        
        // Create the event processing thread.
        eventProcessingThread = new EventProcessingThread(eventProcessing);
        
        // Start the event processing thread.
        eventProcessingThread.start();        
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
        systemStatusFrame.getTableModel().clear();
        
        // Get the global registry of SystemStatus objects.
        SystemStatusRegistry registry = SystemStatusRegistry.getSystemStatusRegistery();
        
        // Process the SystemStatus objects.
        for (SystemStatus systemStatus : registry.getSystemStatuses()) {
            // Add a row to the table for every SystemStatus.
            systemStatusFrame.getTableModel().addSystemStatus(systemStatus);
            
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
        if (!plotFrame.isVisible())
            // Turn on plot frame if it is off.
            plotFrame.setVisible(true);
            
        // Reset plots.
        plotFrame.reset(); 
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
                errorHandler.setError(e)
                    .setMessage("Error saving AIDA file.")
                    .log()
                    .printStackTrace()
                    .showErrorDialog();
            }
        }
    }

    /**
     * Stop the session by killing the event processing thread, ending the job, and disconnecting
     * from the ET system.
     */
    private void stopSession() {
        // Show a modal message window while this method executes.
        JDialog dialog = showStatusDialog("Info", "Disconnecting from session ...", true);
        
        try {
            // Log message.
            logger.log(Level.FINER, "Stopping the session.");
        
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
            
        } finally {        
            // Close modal message window.
            dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
        }
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
    private JDialog showStatusDialog(String title, String message, boolean visible) {
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
                       
        if (eventProcessingThread != null) {
            // Is the event processing thread actually still alive?
            if (eventProcessingThread.isAlive()) {

                // Interrupt and kill the event processing watchdog thread if necessary.
                killSessionWatchdogThread();

                // Request the event processing to stop.
                eventProcessing.stop();                
            }

            // Wait for the event processing thread to finish.  This should just return
            // immediately if it isn't alive so don't bother checking if alive is false.
            try {
                // In the case where ET is configured for sleep or timed wait, an untimed join could 
                // block forever, so only wait for ~1 second before continuing.  The EventProcessingChain
                // should still cleanup automatically when its thread completes after the ET system goes down.
                eventProcessingThread.join(1000);
            } catch (InterruptedException e) {
                // Don't know when this would ever happen.
            }
       
            // Handle last error that occurred in event processing.
            if (eventProcessing.getLastError() != null) {
                errorHandler.setError(eventProcessing.getLastError()).log().printStackTrace();
            }
       
            // Reset event processing objects.
            eventProcessing = null;
            eventProcessingThread = null;
        }
    }

    /**
     * Kill the current session watchdog thread.
     */
    private void killSessionWatchdogThread() {
        if (sessionWatchdogThread != null) {
            if (sessionWatchdogThread.isAlive()) {
                // Interrupt the thread which should cause it to stop.
                sessionWatchdogThread.interrupt();
                try {
                    // This should always work once the thread is interupted.
                    sessionWatchdogThread.join();
                } catch (InterruptedException e) {
                }
            }
            sessionWatchdogThread = null;
        }
    }

    /**
     * Thread to automatically trigger a disconnect when the event processing chain finishes or
     * throws a fatal error.  This thread joins to the event processing thread and automatically 
     * requests a disconnect using an ActionEvent when the event processing thread stops.
     */
    private class SessionWatchdogThread extends Thread {

        public void run() {
            try {
                // When the event processing thread finishes, the session should be stopped and
                // disconnect should occur.
                eventProcessingThread.join();

                // Activate a disconnect using the ActionEvent which is used by the disconnect button.
                // FIXME: When this happens the event processing object and its thread don't get set to null!
                actionPerformed(new ActionEvent(Thread.currentThread(), 0, DISCONNECT));

            } catch (InterruptedException e) {
                // This probably just means that the disconnect button was pushed, and this thread should
                // no longer wait on event processing to finish.
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
                configurationModel.setLogToFile(true);
                try {
                    configurationModel.setLogFileName(file.getCanonicalPath());
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
       
    /**
     * Setup the <code>ConfigurationModel</code> by registering it with sub-components.
     */
    private void setupConfigurationModel() {        
        getJobSettingsPanel().setConfigurationModel(configurationModel);
        getConnectionSettingsPanel().setConfigurationModel(configurationModel);
        settingsDialog.getSettingsPanel().getDataSourcePanel().setConfigurationModel(configurationModel);
    }

    /**
     * Set the Configuration but don't update the ConfigurationModel.
     * @param configuration
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
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
        log(level, "STATUS, "
                + "subsys: " + status.getSubsystem() + ", "
                + "code: " + status.getStatusCode().name() + ", "                 
                + "descr: " + status.getDescription() + ", "                 
                + "mesg: " + status.getMessage());
    }
    
    public static EtConnection fromConfigurationModel(ConfigurationModel configurationModel) {
        try {
            
            // make a direct connection to ET system's tcp server            
            EtSystemOpenConfig etConfig = new EtSystemOpenConfig(
                    configurationModel.getEtName(), 
                    configurationModel.getHost(), 
                    configurationModel.getPort());

            // create ET system object with verbose debugging output
            EtSystem sys = new EtSystem(etConfig, EtConstants.debugInfo);
            sys.open();

            // configuration of a new station
            EtStationConfig statConfig = new EtStationConfig();
            //statConfig.setFlowMode(cn.flowMode);
            // FIXME: Flow mode hard-coded.
            statConfig.setFlowMode(EtConstants.stationSerial);
            boolean blocking = configurationModel.getBlocking();
            if (!blocking) {
                statConfig.setBlockMode(EtConstants.stationNonBlocking);
                int qSize = configurationModel.getQueueSize();
                if (qSize > 0) {
                    statConfig.setCue(qSize);
                }
            }
            // Set prescale.
            int prescale = configurationModel.getPrescale();
            if (prescale > 0) {
                //System.out.println("setting prescale to " + cn.prescale);
                statConfig.setPrescale(prescale);
            }

            // Create the station.
            //System.out.println("position="+config.getInteger("position"));
            EtStation stat = sys.createStation(
                    statConfig, 
                    configurationModel.getStationName(),
                    configurationModel.getStationPosition());

            // attach to new station
            EtAttachment att = sys.attach(stat);

            // Return new connection.
            EtConnection connection = new EtConnection(
                    sys, 
                    att, 
                    stat,
                    configurationModel.getWaitMode(),
                    configurationModel.getWaitTime(),
                    configurationModel.getChunkSize()
                    );
            
            return connection;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
}