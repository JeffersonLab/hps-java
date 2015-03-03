package org.hps.monitoring.application;

import static org.hps.monitoring.application.Commands.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

public class MonitoringActionListener implements ActionListener {

    MonitoringApplication app;
    Logger logger;
    
    MonitoringActionListener(MonitoringApplication app) {
        this.app = app;
        this.logger = MonitoringApplication.logger;
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
                    app.startSession();
                }
            }.start();
        } else if (DISCONNECT.equals(cmd)) {
            // Run the stop session method on a seperate thread.
            new Thread() {
                public void run() {
                    //stopSession();
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
        } else if (SHOW_SETTINGS.equals(cmd)) {
            //showSettingsDialog();
        } else if (SELECT_CONFIG_FILE.equals(cmd)) {
            //chooseConfigurationFile();
        } else if (SAVE_CONFIG_FILE.equals(cmd)) {
            //updateLayoutConfiguration(); /* Save current GUI layout settings first, if needed. */
            //saveConfigurationFile();
        } else if (LOAD_DEFAULT_CONFIG_FILE.equals(cmd)) {
            //loadDefaultConfigFile();
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
        }
    }
        
    private void disconnect() {
        disconnect(ConnectionStatus.DISCONNECTING);
    }

    /**
     * Disconnect from the current ET session with a particular status.
     * @param status The connection status.
     */
    private void disconnect(ConnectionStatus status) {

        logger.fine("Disconnecting the current session.");

        // Cleanup the ET connection.
        //cleanupEtConnection();

        // Update state of GUI to disconnected.
        //setDisconnectedGuiState();

        // Finally, change application state to fully disconnected.
        //setConnectionStatus(ConnectionStatus.DISCONNECTED);

        // Set the application status from the caller if an error occurred.
        //if (status == ConnectionStatus.ERROR)
        //    setConnectionStatus(status);

        //log(Level.INFO, "Disconnected from the session.");
    }    
}
