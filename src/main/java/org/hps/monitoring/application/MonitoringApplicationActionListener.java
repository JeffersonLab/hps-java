/**
 * 
 */
package org.hps.monitoring.application;

import static org.hps.monitoring.application.Commands.CHOOSE_LOG_FILE;
import static org.hps.monitoring.application.Commands.CLEAR_LOG_TABLE;
import static org.hps.monitoring.application.Commands.CONNECT;
import static org.hps.monitoring.application.Commands.DISCONNECT;
import static org.hps.monitoring.application.Commands.EXIT;
import static org.hps.monitoring.application.Commands.LOG_LEVEL_CHANGED;
import static org.hps.monitoring.application.Commands.LOG_TO_TERMINAL;
import static org.hps.monitoring.application.Commands.NEXT;
import static org.hps.monitoring.application.Commands.OPEN_FILE;
import static org.hps.monitoring.application.Commands.PAUSE;
import static org.hps.monitoring.application.Commands.PLOTS_CLEAR;
import static org.hps.monitoring.application.Commands.PLOTS_SAVE;
import static org.hps.monitoring.application.Commands.RESUME;
import static org.hps.monitoring.application.Commands.SAVE_LOG_TABLE;
import static org.hps.monitoring.application.Commands.SCREENSHOT;
import static org.hps.monitoring.application.Commands.SETTINGS_LOAD;
import static org.hps.monitoring.application.Commands.SETTINGS_LOAD_DEFAULT;
import static org.hps.monitoring.application.Commands.SETTINGS_SAVE;
import static org.hps.monitoring.application.Commands.SETTINGS_SHOW;
import static org.hps.monitoring.application.Commands.VALIDATE_DATA_FILE;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.hps.monitoring.application.model.Configuration;
import org.hps.monitoring.application.util.DialogUtil;
import org.hps.monitoring.application.util.EvioFileFilter;
import org.hps.record.enums.DataSourceType;
import org.lcsim.util.aida.AIDA;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class MonitoringApplicationActionListener implements ActionListener {
    
    MonitoringApplication application;
    
    MonitoringApplicationActionListener(MonitoringApplication application) {
        this.application = application;
    }
    
    /**
     * The action handler method for the application.
     * @param e The event to handle.
     */
    public void actionPerformed(ActionEvent e) {

        String cmd = e.getActionCommand();
        if (CONNECT.equals(cmd)) {
            // FIXME: Can the application run this in a separate thread instead?
            // Run the start session method on a separate thread.
            new Thread() {
                public void run() {
                    application.startSession();
                }
            }.start();
        } else if (DISCONNECT.equals(cmd)) {
            // Run the stop session method on a separate thread.
            new Thread() {
                public void run() {
                    application.stopSession();
                }
            }.start();
        } else if (PLOTS_SAVE.equals(cmd)) {
            savePlots();
        } else if (CHOOSE_LOG_FILE.equals(cmd)) {
            //chooseLogFile();
        } else if (LOG_TO_TERMINAL.equals(cmd)) {
            //logToTerminal();
        } else if (SCREENSHOT.equals(cmd)) {
            //chooseScreenshot();
        } else if (EXIT.equals(cmd)) {
            application.exit();
        } else if (SAVE_LOG_TABLE.equals(cmd)) {
            //saveLogTableToFile();
        } else if (CLEAR_LOG_TABLE.equals(cmd)) {
            //clearLogTable();
        } else if (PAUSE.equals(cmd)) { 
            application.processing.pause();
        } else if (NEXT.equals(cmd)) {
            application.processing.next();
        } else if (RESUME.equals(cmd)) {
            application.processing.resume();
        } else if (LOG_LEVEL_CHANGED.equals(cmd)) {
            //setLogLevel();
        } else if (SETTINGS_SHOW.equals(cmd)) {
            showSettingsDialog();
        } else if (SETTINGS_LOAD.equals(cmd)) {
            loadSettings();
        } else if (SETTINGS_SAVE.equals(cmd)) {
            saveSettings();
        } else if (VALIDATE_DATA_FILE.equals(cmd)) {
            //if (fileValidationThread == null) {
            //    new FileValidationThread().start();
            //}
        } else if (PLOTS_CLEAR.equals(cmd)) {
            clearPlots();
        } else if (SETTINGS_LOAD_DEFAULT.equals(cmd)) {
            loadDefaultSettings();
        } else if (OPEN_FILE.equals(cmd)) {
            openFile();
        }
    }
    
    /**
     * 
     */
    void savePlots() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showSaveDialog(application.frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            File fileName = fc.getSelectedFile();
            try {
                AIDA.defaultInstance().saveAs(fileName);
                DialogUtil.showInfoDialog(application.frame,
                        "Plots Saved", 
                        "Plots were successfully saved to AIDA file.");
            } catch (IOException e) {
                application.errorHandler.setError(e).setMessage("Error Saving Plots").printStackTrace().log().showErrorDialog();
            }
        }
    }
    
    /**
     * 
     */
    void clearPlots() {
        int confirmation = DialogUtil.showConfirmationDialog(application.frame, 
                "Are you sure you want to clear the plots", "Clear Plots Confirmation");
        if (confirmation == JOptionPane.YES_OPTION) {
            AIDA.defaultInstance().clearAll();
            DialogUtil.showInfoDialog(application.frame,
                    "Plots Clear", 
                    "The AIDA plots were cleared.");
        }
    }
    
    void loadDefaultSettings() {
        application.configuration = new Configuration(MonitoringApplication.DEFAULT_CONFIGURATION);
        application.configurationModel.setConfiguration(application.configuration);
        DialogUtil.showInfoDialog(application.frame,
                "Default Configuration Loaded", 
                "The default configuration was loaded.");
    }
    
    void showSettingsDialog() {
        application.frame.settingsDialog.setVisible(true);
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
        int r = fc.showDialog(application.frame, "Select ...");        
        if (r == JFileChooser.APPROVE_OPTION) {
                                  
            // Set data source path.            
            final String filePath = fc.getSelectedFile().getPath();
            application.configurationModel.setDataSourcePath(filePath);
            
            // Set data source type.
            FileFilter filter = fc.getFileFilter();
            if (filter == lcioFilter) {
                application.configurationModel.setDataSourceType(DataSourceType.LCIO_FILE);
            } else if (filter == evioFilter) {
                application.configurationModel.setDataSourceType(DataSourceType.EVIO_FILE);
            }
        }
    }    
    
    void saveSettings() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Configuration");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(application.frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            application.configuration.writeToFile(f);
            DialogUtil.showInfoDialog(application.frame,
                    "Settings Saved", 
                    "Settings were saved successfully.");
        }
    }
    
    void loadSettings() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Settings");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showDialog(application.frame, "Load ...");
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            application.configuration = new Configuration(f);
            application.loadConfiguration(application.configuration);
            DialogUtil.showInfoDialog(application.frame,
                    "Settings Loaded", 
                    "Settings were loaded successfully.");
        }
    }
    
}