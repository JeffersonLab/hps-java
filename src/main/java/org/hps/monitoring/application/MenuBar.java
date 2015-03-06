package org.hps.monitoring.application;

import static org.hps.monitoring.application.Commands.EXIT;
import static org.hps.monitoring.application.Commands.FILE_CLOSE;
import static org.hps.monitoring.application.Commands.FILE_OPEN;
import static org.hps.monitoring.application.Commands.PLOTS_CLEAR;
import static org.hps.monitoring.application.Commands.PLOTS_SAVE;
import static org.hps.monitoring.application.Commands.SETTINGS_LOAD;
import static org.hps.monitoring.application.Commands.SETTINGS_LOAD_DEFAULT;
import static org.hps.monitoring.application.Commands.SETTINGS_SAVE;
import static org.hps.monitoring.application.Commands.SETTINGS_SHOW;
import static org.hps.monitoring.application.Commands.WINDOW_DEFAULTS;
import static org.hps.monitoring.application.Commands.WINDOW_MAXIMIZE;
import static org.hps.monitoring.application.Commands.WINDOW_MINIMIZE;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.record.enums.DataSourceType;

/**
 * This is the primary menu bar for the monitoring application.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class MenuBar extends JMenuBar implements PropertyChangeListener, ActionListener {
    
    JMenuItem closeFileItem;
    JMenuItem openFileItem;    
    JMenu settingsMenu;
    ConfigurationModel configurationModel;
    
    MenuBar(ConfigurationModel configurationModel, ConnectionStatusModel connectionModel, ActionListener listener) {
        
        // Do not need to listen for changes on this model.
        this.configurationModel = configurationModel;
        
        // Need to listen for connection status changes.
        connectionModel.addPropertyChangeListener(this);                

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        add(fileMenu);
        
        openFileItem = new JMenuItem("Open File ...");
        openFileItem.setMnemonic(KeyEvent.VK_P);
        openFileItem.setActionCommand(FILE_OPEN);
        openFileItem.addActionListener(listener);
        openFileItem.setToolTipText("Open an EVIO or LCIO data file");
        fileMenu.add(openFileItem);
        
        closeFileItem = new JMenuItem("Close File");
        closeFileItem.setMnemonic(KeyEvent.VK_C);
        closeFileItem.setActionCommand(FILE_CLOSE);
        closeFileItem.addActionListener(listener);
        closeFileItem.setToolTipText("Close the current file data source");
        fileMenu.add(closeFileItem);
              
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setActionCommand(EXIT);
        exitItem.addActionListener(listener);
        exitItem.setToolTipText("Exit from the application");
        fileMenu.add(exitItem);
                
        settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic(KeyEvent.VK_S);
        add(settingsMenu);
        
        JMenuItem settingsItem = new JMenuItem("Open Settings Window ...");
        settingsItem.setMnemonic(KeyEvent.VK_O);
        settingsItem.setActionCommand(SETTINGS_SHOW);
        settingsItem.addActionListener(listener);
        settingsItem.setToolTipText("Show settings dialog");
        settingsMenu.add(settingsItem);
        
        JMenuItem loadConfigItem = new JMenuItem("Load Settings ...");
        loadConfigItem.addActionListener(listener);
        loadConfigItem.setMnemonic(KeyEvent.VK_L);
        loadConfigItem.setActionCommand(SETTINGS_LOAD);
        loadConfigItem.setToolTipText("Load settings from a properties file");
        settingsMenu.add(loadConfigItem);

        JMenuItem saveConfigItem = new JMenuItem("Save Settings ...");
        saveConfigItem.addActionListener(listener);
        saveConfigItem.setMnemonic(KeyEvent.VK_S);
        saveConfigItem.setActionCommand(SETTINGS_SAVE);
        saveConfigItem.setToolTipText("Save configuration to a properties file");
        settingsMenu.add(saveConfigItem);
        
        JMenuItem defaultSettingsItem = new JMenuItem("Load Default Settings");
        defaultSettingsItem.addActionListener(listener);
        defaultSettingsItem.setMnemonic(KeyEvent.VK_D);
        defaultSettingsItem.setActionCommand(SETTINGS_LOAD_DEFAULT);
        defaultSettingsItem.setToolTipText("Load the default settings");
        settingsMenu.add(defaultSettingsItem);
        
        JMenu plotsMenu = new JMenu("Plots");
        plotsMenu.setMnemonic(KeyEvent.VK_P);
        add(plotsMenu);
        
        JMenuItem savePlotsItem = new JMenuItem("Save Plots ...");
        savePlotsItem.setMnemonic(KeyEvent.VK_S);
        savePlotsItem.setActionCommand(PLOTS_SAVE);
        savePlotsItem.addActionListener(listener);
        savePlotsItem.setEnabled(true);
        savePlotsItem.setToolTipText("Save plots to AIDA file");
        plotsMenu.add(savePlotsItem);

        JMenuItem clearPlotsItem = new JMenuItem("Clear plots");
        clearPlotsItem.setMnemonic(KeyEvent.VK_C);
        clearPlotsItem.setActionCommand(PLOTS_CLEAR);
        clearPlotsItem.addActionListener(listener);
        clearPlotsItem.setEnabled(true);
        clearPlotsItem.setToolTipText("Clear the AIDA plots");
        plotsMenu.add(clearPlotsItem);
        
        JMenu windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        add(windowMenu);
        
        JMenuItem maximizeItem = new JMenuItem("Maximize");
        maximizeItem.setMnemonic(KeyEvent.VK_M);
        maximizeItem.setActionCommand(WINDOW_MAXIMIZE);
        maximizeItem.addActionListener(listener);
        maximizeItem.setEnabled(true);
        maximizeItem.setToolTipText("Maximize the application window");
        windowMenu.add(maximizeItem);
        
        JMenuItem minimizeItem = new JMenuItem("Minimize");
        minimizeItem.setMnemonic(KeyEvent.VK_I);
        minimizeItem.setActionCommand(WINDOW_MINIMIZE);
        minimizeItem.addActionListener(listener);
        minimizeItem.setEnabled(true);
        minimizeItem.setToolTipText("Minimize the application window");
        windowMenu.add(minimizeItem);
        
        JMenuItem defaultsItem = new JMenuItem("Restore Defaults");
        defaultsItem.setMnemonic(KeyEvent.VK_D);
        defaultsItem.setActionCommand(WINDOW_DEFAULTS);
        defaultsItem.addActionListener(listener);
        defaultsItem.setEnabled(true);
        defaultsItem.setToolTipText("Restore the window defaults");
        windowMenu.add(defaultsItem);        
        
        /*                       

        JMenu logMenu = new JMenu("Log");
        logMenu.setMnemonic(KeyEvent.VK_L);
        add(logMenu);

        logItem = new JMenuItem("Redirect to File ...");
        logItem.setMnemonic(KeyEvent.VK_F);
        logItem.setActionCommand(CHOOSE_LOG_FILE);
        //logItem.addActionListener(this);
        logItem.setEnabled(true);
        logItem.setToolTipText("Redirect std out and err to a file.");
        logMenu.add(logItem);

        terminalItem = new JMenuItem("Redirect to Terminal");
        terminalItem.setMnemonic(KeyEvent.VK_T);
        terminalItem.setActionCommand(LOG_TO_TERMINAL);
        //terminalItem.addActionListener(this);
        terminalItem.setEnabled(false);
        terminalItem.setToolTipText("Redirect std out and err back to the terminal.");
        logMenu.add(terminalItem);

        JMenuItem saveLogItem = new JMenuItem("Save Log Table to File ...");
        saveLogItem.setMnemonic(KeyEvent.VK_S);
        saveLogItem.setActionCommand(SAVE_LOG_TABLE);
        //saveLogItem.addActionListener(this);
        saveLogItem.setToolTipText("Save the log records to a tab delimited text file.");
        logMenu.add(saveLogItem);

        JMenuItem clearLogItem = new JMenuItem("Clear Log Table");
        //clearLogItem.addActionListener(this);
        clearLogItem.setMnemonic(KeyEvent.VK_C);
        clearLogItem.setActionCommand(CLEAR_LOG_TABLE);
        clearLogItem.setToolTipText("Clear the log table of all messages.");
        logMenu.add(clearLogItem);

        JMenu utilMenu = new JMenu("Util");
        plotsMenu.setMnemonic(KeyEvent.VK_U);
        add(utilMenu);

        JMenuItem screenshotItem = new JMenuItem("Take a Screenshot ...");
        screenshotItem.setMnemonic(KeyEvent.VK_N);
        screenshotItem.setActionCommand(SCREENSHOT);
        //screenshotItem.addActionListener(this);
        screenshotItem.setToolTipText("Save a screenshot to file");
        utilMenu.add(screenshotItem);
        */
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
            ConnectionStatus status = (ConnectionStatus) evt.getNewValue();
            boolean connected = status.equals(ConnectionStatus.CONNECTED);            
            closeFileItem.setEnabled(!connected);
            openFileItem.setEnabled(!connected);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(Commands.DATA_SOURCE_CHANGED)) {
            if (!configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER)) {
                closeFileItem.setEnabled(true);
            } else {
                closeFileItem.setEnabled(false);
            }
        }        
    }
    
}
