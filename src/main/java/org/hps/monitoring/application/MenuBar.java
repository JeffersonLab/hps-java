package org.hps.monitoring.application;

import static org.hps.monitoring.application.Commands.*;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MenuBar extends JMenuBar {

    //private JMenuItem savePlotsItem;
    //private JMenuItem logItem;
    //private JMenuItem terminalItem;
    //private JMenuItem saveLayoutItem;
    
    MenuBar(ActionListener listener) {

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        add(fileMenu);
        
        JMenuItem openFileItem = new JMenuItem("Open File ...");
        openFileItem.setMnemonic(KeyEvent.VK_P);
        openFileItem.setActionCommand(OPEN_FILE);
        openFileItem.addActionListener(listener);
        openFileItem.setToolTipText("Open an EVIO or LCIO data file");
        fileMenu.add(openFileItem);
              
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setActionCommand(EXIT);
        exitItem.addActionListener(listener);
        exitItem.setToolTipText("Exit from the application");
        fileMenu.add(exitItem);
                
        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic(KeyEvent.VK_S);
        add(settingsMenu);
        
        JMenuItem settingsItem = new JMenuItem("Show Settings ...");
        settingsItem.setMnemonic(KeyEvent.VK_P);
        settingsItem.setActionCommand(SETTINGS_SHOW);
        settingsItem.addActionListener(listener);
        settingsItem.setToolTipText("Show settings dialog");
        settingsMenu.add(settingsItem);
        
        JMenuItem loadConfigItem = new JMenuItem("Load Settings ...");
        loadConfigItem.addActionListener(listener);
        loadConfigItem.setMnemonic(KeyEvent.VK_C);
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
                      
        /*               
        JMenu plotsMenu = new JMenu("Plots");
        plotsMenu.setMnemonic(KeyEvent.VK_O);
        add(plotsMenu);

        JMenuItem aidaAutoSaveItem = new JMenuItem("Set AIDA Auto Save File ...");
        aidaAutoSaveItem.setMnemonic(KeyEvent.VK_A);
        aidaAutoSaveItem.setActionCommand(AIDA_AUTO_SAVE);
        //aidaAutoSaveItem.addActionListener(listener);
        aidaAutoSaveItem.setToolTipText("Select name of file to auto save AIDA plots at end of job.");
        plotsMenu.add(aidaAutoSaveItem);

        savePlotsItem = new JMenuItem("Save Plots to AIDA File...");
        savePlotsItem.setMnemonic(KeyEvent.VK_P);
        savePlotsItem.setActionCommand(SAVE_PLOTS);
        //savePlotsItem.addActionListener(listener);
        savePlotsItem.setEnabled(false);
        savePlotsItem.setToolTipText("Save plots from default AIDA tree to an output file.");
        plotsMenu.add(savePlotsItem);
        
        JMenuItem resetPlotsItem = new JMenuItem("Reset Plots");
        resetPlotsItem.setMnemonic(KeyEvent.VK_R);
        resetPlotsItem.setActionCommand(RESET_PLOTS);
        //resetPlotsItem.addActionListener(listener);
        resetPlotsItem.setEnabled(true);
        resetPlotsItem.setToolTipText("Reset all AIDA plots in the default tree.");
        plotsMenu.add(resetPlotsItem);

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
    
}
