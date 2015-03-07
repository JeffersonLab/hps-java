package org.hps.monitoring.application;

import static org.hps.monitoring.application.Commands.EXIT;
import static org.hps.monitoring.application.Commands.CLOSE_FILE;
import static org.hps.monitoring.application.Commands.OPEN_FILE;
import static org.hps.monitoring.application.Commands.CLEAR_PLOTS;
import static org.hps.monitoring.application.Commands.SAVE_PLOTS;
import static org.hps.monitoring.application.Commands.LOAD_SETTINGS;
import static org.hps.monitoring.application.Commands.LOAD_DEFAULT_SETTINGS;
import static org.hps.monitoring.application.Commands.SAVE_SCREENSHOT;
import static org.hps.monitoring.application.Commands.SAVE_SETTINGS;
import static org.hps.monitoring.application.Commands.SHOW_SETTINGS;
import static org.hps.monitoring.application.Commands.DEFAULT_WINDOW;
import static org.hps.monitoring.application.Commands.MAXIMIZE_WINDOW;
import static org.hps.monitoring.application.Commands.MINIMIZE_WINDOW;

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
        openFileItem.setActionCommand(OPEN_FILE);
        openFileItem.addActionListener(listener);
        openFileItem.setToolTipText("Open an EVIO or LCIO data file");
        fileMenu.add(openFileItem);
        
        closeFileItem = new JMenuItem("Close File");
        closeFileItem.setMnemonic(KeyEvent.VK_C);
        closeFileItem.setActionCommand(CLOSE_FILE);
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
        settingsItem.setActionCommand(SHOW_SETTINGS);
        settingsItem.addActionListener(listener);
        settingsItem.setToolTipText("Show settings dialog");
        settingsMenu.add(settingsItem);
        
        JMenuItem loadConfigItem = new JMenuItem("Load Settings ...");
        loadConfigItem.addActionListener(listener);
        loadConfigItem.setMnemonic(KeyEvent.VK_L);
        loadConfigItem.setActionCommand(LOAD_SETTINGS);
        loadConfigItem.setToolTipText("Load settings from a properties file");
        settingsMenu.add(loadConfigItem);

        JMenuItem saveConfigItem = new JMenuItem("Save Settings ...");
        saveConfigItem.addActionListener(listener);
        saveConfigItem.setMnemonic(KeyEvent.VK_S);
        saveConfigItem.setActionCommand(SAVE_SETTINGS);
        saveConfigItem.setToolTipText("Save configuration to a properties file");
        settingsMenu.add(saveConfigItem);
        
        JMenuItem defaultSettingsItem = new JMenuItem("Load Default Settings");
        defaultSettingsItem.addActionListener(listener);
        defaultSettingsItem.setMnemonic(KeyEvent.VK_D);
        defaultSettingsItem.setActionCommand(LOAD_DEFAULT_SETTINGS);
        defaultSettingsItem.setToolTipText("Load the default settings");
        settingsMenu.add(defaultSettingsItem);
        
        JMenu plotsMenu = new JMenu("Plots");
        plotsMenu.setMnemonic(KeyEvent.VK_P);
        add(plotsMenu);
        
        JMenuItem savePlotsItem = new JMenuItem("Save Plots ...");
        savePlotsItem.setMnemonic(KeyEvent.VK_S);
        savePlotsItem.setActionCommand(SAVE_PLOTS);
        savePlotsItem.addActionListener(listener);
        savePlotsItem.setEnabled(true);
        savePlotsItem.setToolTipText("Save plots to AIDA file");
        plotsMenu.add(savePlotsItem);

        JMenuItem clearPlotsItem = new JMenuItem("Clear plots");
        clearPlotsItem.setMnemonic(KeyEvent.VK_C);
        clearPlotsItem.setActionCommand(CLEAR_PLOTS);
        clearPlotsItem.addActionListener(listener);
        clearPlotsItem.setEnabled(true);
        clearPlotsItem.setToolTipText("Clear the AIDA plots");
        plotsMenu.add(clearPlotsItem);
        
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        add(toolsMenu);
        
        JMenuItem screenshotItem = new JMenuItem("Save Screenshot ...");
        screenshotItem.setMnemonic(KeyEvent.VK_S);
        screenshotItem.setActionCommand(SAVE_SCREENSHOT);
        screenshotItem.addActionListener(listener);
        screenshotItem.setEnabled(true);
        screenshotItem.setToolTipText("Save a screenshot to a graphics file");
        toolsMenu.add(screenshotItem);
        
        JMenu windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        add(windowMenu);
        
        JMenuItem maximizeItem = new JMenuItem("Maximize");
        maximizeItem.setMnemonic(KeyEvent.VK_M);
        maximizeItem.setActionCommand(MAXIMIZE_WINDOW);
        maximizeItem.addActionListener(listener);
        maximizeItem.setEnabled(true);
        maximizeItem.setToolTipText("Maximize the application window");
        windowMenu.add(maximizeItem);
        
        JMenuItem minimizeItem = new JMenuItem("Minimize");
        minimizeItem.setMnemonic(KeyEvent.VK_I);
        minimizeItem.setActionCommand(MINIMIZE_WINDOW);
        minimizeItem.addActionListener(listener);
        minimizeItem.setEnabled(true);
        minimizeItem.setToolTipText("Minimize the application window");
        windowMenu.add(minimizeItem);
        
        JMenuItem defaultsItem = new JMenuItem("Restore Defaults");
        defaultsItem.setMnemonic(KeyEvent.VK_D);
        defaultsItem.setActionCommand(DEFAULT_WINDOW);
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
