package org.hps.monitoring.application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

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
@SuppressWarnings("serial")
class MenuBar extends JMenuBar implements PropertyChangeListener, ActionListener {
    
    ConfigurationModel configurationModel;
    
    JMenuItem closeFileItem;
    JMenuItem openFileItem;    
    JMenu settingsMenu;
    JMenuItem logItem;
    JMenuItem serverItem;
    JMenu recentFilesMenu;    
    
    class RecentFileItem extends JMenuItem {
        
        String path;        
        
        RecentFileItem(String path, int mnemonic) {
            setText((mnemonic - KeyEvent.VK_0) + " " + path);
            setMnemonic(mnemonic);
            this.path = path;
        }
        
        String getPath() {
            return path;
        }        
    }
                   
    MenuBar(ConfigurationModel configurationModel, ConnectionStatusModel connectionModel, ActionListener listener) {
         
        this.configurationModel = configurationModel;        
        this.configurationModel.addPropertyChangeListener(this);
        
        // Need to listen for connection status changes.
        connectionModel.addPropertyChangeListener(this);  
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        add(fileMenu);
        
        openFileItem = new JMenuItem("Open File ...");
        openFileItem.setMnemonic(KeyEvent.VK_P);
        openFileItem.setActionCommand(Commands.OPEN_FILE);
        openFileItem.addActionListener(listener);
        openFileItem.setToolTipText("Open an EVIO or LCIO data file");
        fileMenu.add(openFileItem);
        
        closeFileItem = new JMenuItem("Close File");
        closeFileItem.setMnemonic(KeyEvent.VK_C);
        closeFileItem.setActionCommand(Commands.CLOSE_FILE);
        closeFileItem.addActionListener(listener);
        closeFileItem.setToolTipText("Close the current file data source");
        fileMenu.add(closeFileItem);
                                      
        recentFilesMenu = new JMenu("Recent Files");
        recentFilesMenu.setMnemonic(KeyEvent.VK_R);
        recentFilesMenu.setToolTipText("List of recent data files");
        JMenuItem noRecentFilesItem = new JMenuItem("No recent files");
        noRecentFilesItem.setEnabled(false);
        recentFilesMenu.add(noRecentFilesItem);
        fileMenu.add(recentFilesMenu);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setActionCommand(Commands.EXIT);
        exitItem.addActionListener(listener);
        exitItem.setToolTipText("Exit from the application");
        fileMenu.add(exitItem);
                
        settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic(KeyEvent.VK_S);
        add(settingsMenu);
        
        JMenuItem settingsItem = new JMenuItem("Open Settings Window ...");
        settingsItem.setMnemonic(KeyEvent.VK_O);
        settingsItem.setActionCommand(Commands.SHOW_SETTINGS);
        settingsItem.addActionListener(listener);
        settingsItem.setToolTipText("Show settings dialog");
        settingsMenu.add(settingsItem);
        
        JMenuItem loadConfigItem = new JMenuItem("Load Settings ...");
        loadConfigItem.addActionListener(listener);
        loadConfigItem.setMnemonic(KeyEvent.VK_L);
        loadConfigItem.setActionCommand(Commands.LOAD_SETTINGS);
        loadConfigItem.setToolTipText("Load settings from a properties file");
        settingsMenu.add(loadConfigItem);

        JMenuItem saveConfigItem = new JMenuItem("Save Settings ...");
        saveConfigItem.addActionListener(listener);
        saveConfigItem.setMnemonic(KeyEvent.VK_S);
        saveConfigItem.setActionCommand(Commands.SAVE_SETTINGS);
        saveConfigItem.setToolTipText("Save configuration to a properties file");
        settingsMenu.add(saveConfigItem);
        
        JMenuItem defaultSettingsItem = new JMenuItem("Load Default Settings");
        defaultSettingsItem.addActionListener(listener);
        defaultSettingsItem.setMnemonic(KeyEvent.VK_D);
        defaultSettingsItem.setActionCommand(Commands.LOAD_DEFAULT_SETTINGS);
        defaultSettingsItem.setToolTipText("Load the default settings");
        settingsMenu.add(defaultSettingsItem);
        
        JMenu plotsMenu = new JMenu("Plots");
        plotsMenu.setMnemonic(KeyEvent.VK_P);
        add(plotsMenu);
        
        JMenuItem savePlotsItem = new JMenuItem("Save plots ...");
        savePlotsItem.setMnemonic(KeyEvent.VK_S);
        savePlotsItem.setActionCommand(Commands.SAVE_PLOTS);
        savePlotsItem.addActionListener(listener);
        savePlotsItem.setEnabled(true);
        savePlotsItem.setToolTipText("Save all plots to a file");
        plotsMenu.add(savePlotsItem);

        JMenuItem clearPlotsItem = new JMenuItem("Clear plots");
        clearPlotsItem.setMnemonic(KeyEvent.VK_C);
        clearPlotsItem.setActionCommand(Commands.CLEAR_PLOTS);
        clearPlotsItem.addActionListener(listener);
        clearPlotsItem.setEnabled(true);
        clearPlotsItem.setToolTipText("Clear the AIDA plots");
        plotsMenu.add(clearPlotsItem);
        
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        add(toolsMenu);
        
        JMenuItem screenshotItem = new JMenuItem("Save Screenshot ...");
        screenshotItem.setMnemonic(KeyEvent.VK_S);
        screenshotItem.setActionCommand(Commands.SAVE_SCREENSHOT);
        screenshotItem.addActionListener(listener);
        screenshotItem.setEnabled(true);
        screenshotItem.setToolTipText("Save a screenshot to a graphics file");
        toolsMenu.add(screenshotItem);
        
        logItem = new JMenuItem("Log to File ...");
        logItem.setMnemonic(KeyEvent.VK_L);
        logItem.setActionCommand(Commands.LOG_TO_FILE);
        logItem.addActionListener(listener);
        logItem.setEnabled(true);
        logItem.setToolTipText("Redirect System.out to a file instead of terminal");
        toolsMenu.add(logItem);
        
        serverItem = new JMenuItem("Start AIDA Server ...");
        serverItem.setMnemonic(KeyEvent.VK_A);
        serverItem.setActionCommand(Commands.START_AIDA_SERVER);
        serverItem.setEnabled(true);
        serverItem.setToolTipText("Start AIDA RMI Server");
        serverItem.addActionListener(listener);
        toolsMenu.add(serverItem);
        
        JMenu windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        add(windowMenu);
        
        JMenuItem maximizeItem = new JMenuItem("Maximize");
        maximizeItem.setMnemonic(KeyEvent.VK_M);
        maximizeItem.setActionCommand(Commands.MAXIMIZE_WINDOW);
        maximizeItem.addActionListener(listener);
        maximizeItem.setEnabled(true);
        maximizeItem.setToolTipText("Maximize the application window");
        windowMenu.add(maximizeItem);
        
        JMenuItem minimizeItem = new JMenuItem("Minimize");
        minimizeItem.setMnemonic(KeyEvent.VK_I);
        minimizeItem.setActionCommand(Commands.MINIMIZE_WINDOW);
        minimizeItem.addActionListener(listener);
        minimizeItem.setEnabled(true);
        minimizeItem.setToolTipText("Minimize the application window");
        windowMenu.add(minimizeItem);
        
        JMenuItem defaultsItem = new JMenuItem("Restore Defaults");
        defaultsItem.setMnemonic(KeyEvent.VK_D);
        defaultsItem.setActionCommand(Commands.DEFAULT_WINDOW);
        defaultsItem.addActionListener(listener);
        defaultsItem.setEnabled(true);
        defaultsItem.setToolTipText("Restore the window defaults");
        windowMenu.add(defaultsItem);               
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        configurationModel.removePropertyChangeListener(this);        
        try {            
            if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
                ConnectionStatus status = (ConnectionStatus) evt.getNewValue();
                boolean connected = status.equals(ConnectionStatus.CONNECTED);
                closeFileItem.setEnabled(!connected);
                openFileItem.setEnabled(!connected);
            } else if (evt.getPropertyName().equals(ConfigurationModel.LOG_TO_FILE_PROPERTY)) {
                Boolean logToFile = (Boolean) evt.getNewValue();
                if (logToFile == true) {
                    // Toggle log item state to send to terminal.
                    logItem.setText("Log to Terminal ...");
                    logItem.setActionCommand(Commands.LOG_TO_TERMINAL);
                    logItem.setToolTipText("Log messages to the terminal");
                } else {
                    // Toggle log item state to send to file.
                    logItem.setText("Log to File ...");
                    logItem.setActionCommand(Commands.LOG_TO_FILE);
                    logItem.setToolTipText("Log messages to a file");
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.RECENT_FILES_PROPERTY)) {
                setRecentFiles(configurationModel.getRecentFilesList());
            } 
            
        } finally {
            configurationModel.addPropertyChangeListener(this);
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
    
    void setRecentFiles(List<String> recentFiles) {
        recentFilesMenu.removeAll();
        int fileMnemonic = 48; /* starts at KeyEvent.VK_0 */
        for (String recentFile : recentFiles) {
            RecentFileItem recentFileItem = new RecentFileItem(recentFile, fileMnemonic);
            recentFileItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {            
                    String recentFile = ((RecentFileItem) e.getSource()).getPath();
                    DataSourceType dst = DataSourceType.getDataSourceType(recentFile);
                    configurationModel.setDataSourcePath(recentFile); 
                    configurationModel.setDataSourceType(dst);
                }
            });                
            recentFilesMenu.add(recentFileItem);
            ++fileMnemonic;
        }        
    }
    
    void startAIDAServer() {
        serverItem.setActionCommand(Commands.STOP_AIDA_SERVER);
        serverItem.setText("Stop AIDA Server");
        serverItem.setToolTipText("Stop the remote AIDA server");
    }
    
    void stopAIDAServer() {
        serverItem.setActionCommand(Commands.START_AIDA_SERVER);
        serverItem.setText("Start AIDA Server");
        serverItem.setToolTipText("Start the remote AIDA server");
    }
}
