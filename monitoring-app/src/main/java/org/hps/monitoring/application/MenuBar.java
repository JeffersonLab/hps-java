package org.hps.monitoring.application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JCheckBox;
//import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.monitoring.application.model.HasConfigurationModel;
import org.hps.record.enums.DataSourceType;

/**
 * This is the primary menu bar for the monitoring application.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class MenuBar extends JMenuBar implements PropertyChangeListener, ActionListener, HasConfigurationModel {

    /**
     * The implementation of {@link javax.swing.JMenuItem} for recent file items.
     */
    private class RecentFileItem extends JMenuItem {

        /**
         * The recent file's path.
         */
        private final String path;

        /**
         * Class constructor with file's path and its numerical mnemonic (0-9).
         *
         * @param path the path
         * @param mnemonic the item's mnemonic shortcut (0-9)
         */
        RecentFileItem(final String path, final int mnemonic) {
            setText(mnemonic - KeyEvent.VK_0 + " " + path);
            setMnemonic(mnemonic);
            this.path = path;
        }

        /**
         * Get the file path of the item.
         *
         * @return the file path
         */
        private String getPath() {
            return this.path;
        }
    }

    /**
     * Starting mnemonic for recent files (this is equivalent to '0').
     */
    private static final int RECENT_FILES_START_INDEX = 48;

    /**
     * Menu item for closing a file source.
     */
    private final JMenuItem closeFileItem;

    /**
     * The application backing model.
     */
    private ConfigurationModel configurationModel;

    /**
     * Menu item for logging to a file.
     */
    private final JMenuItem logItem;

    /**
     * Menu item for opening a file data source.
     */
    private final JMenuItem openFileItem;

    /**
     * Menu with list of recent files (10 max).
     */
    private final JMenu recentFilesMenu;

    /**
     * Menu item for opening the settings dialog window.
     */
    private final JMenu settingsMenu;
    
    /**
     * Checkbox for enabling plot pop-up when regions are clicked.
     */
    private JCheckBox popupItem;

    /**
     * Class constructor.
     *
     * @param configurationModel the {@link org.hps.monitoring.application.model.ConfigurationModel} providing the model
     * @param connectionModel the {@link org.hps.monitoring.application.model.ConnectionStatusModel} providing
     * connection status
     * @param listener an {@link ava.awt.event.ActionListener} which is assigned to certain components
     */
    MenuBar(final ConfigurationModel configurationModel, final ConnectionStatusModel connectionModel,
            final ActionListener listener) {

        this.setConfigurationModel(configurationModel);

        // Need to listen for connection status changes to toggle menu items.
        connectionModel.addPropertyChangeListener(this);

        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        add(fileMenu);

        this.openFileItem = new JMenuItem("Open File ...");
        this.openFileItem.setMnemonic(KeyEvent.VK_P);
        this.openFileItem.setActionCommand(Commands.OPEN_FILE);
        this.openFileItem.addActionListener(listener);
        this.openFileItem.setToolTipText("Open an EVIO or LCIO data file");
        fileMenu.add(this.openFileItem);

        this.closeFileItem = new JMenuItem("Close File");
        this.closeFileItem.setMnemonic(KeyEvent.VK_C);
        this.closeFileItem.setActionCommand(Commands.CLOSE_FILE);
        this.closeFileItem.addActionListener(listener);
        this.closeFileItem.setToolTipText("Close the current file data source");
        fileMenu.add(this.closeFileItem);

        this.recentFilesMenu = new JMenu("Recent Files");
        this.recentFilesMenu.setMnemonic(KeyEvent.VK_R);
        this.recentFilesMenu.setToolTipText("List of recent data files");
        final JMenuItem noRecentFilesItem = new JMenuItem("No recent files");
        noRecentFilesItem.setEnabled(false);
        this.recentFilesMenu.add(noRecentFilesItem);
        fileMenu.add(this.recentFilesMenu);

        fileMenu.addSeparator();

        final JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setActionCommand(Commands.EXIT);
        exitItem.addActionListener(listener);
        exitItem.setToolTipText("Exit from the application");
        fileMenu.add(exitItem);

        this.settingsMenu = new JMenu("Settings");
        this.settingsMenu.setMnemonic(KeyEvent.VK_S);
        add(this.settingsMenu);

        final JMenuItem settingsItem = new JMenuItem("Open Settings Window ...");
        settingsItem.setMnemonic(KeyEvent.VK_O);
        settingsItem.setActionCommand(Commands.SHOW_SETTINGS);
        settingsItem.addActionListener(listener);
        settingsItem.setToolTipText("Show settings dialog");
        this.settingsMenu.add(settingsItem);

        final JMenuItem loadConfigItem = new JMenuItem("Load Settings ...");
        loadConfigItem.addActionListener(listener);
        loadConfigItem.setMnemonic(KeyEvent.VK_L);
        loadConfigItem.setActionCommand(Commands.LOAD_SETTINGS);
        loadConfigItem.setToolTipText("Load settings from a properties file");
        this.settingsMenu.add(loadConfigItem);

        final JMenuItem saveConfigItem = new JMenuItem("Save Settings ...");
        saveConfigItem.addActionListener(listener);
        saveConfigItem.setMnemonic(KeyEvent.VK_S);
        saveConfigItem.setActionCommand(Commands.SAVE_SETTINGS);
        saveConfigItem.setToolTipText("Save configuration to a properties file");
        this.settingsMenu.add(saveConfigItem);

        final JMenuItem defaultSettingsItem = new JMenuItem("Load Default Settings");
        defaultSettingsItem.addActionListener(listener);
        defaultSettingsItem.setMnemonic(KeyEvent.VK_D);
        defaultSettingsItem.setActionCommand(Commands.LOAD_DEFAULT_SETTINGS);
        defaultSettingsItem.setToolTipText("Load the default settings");
        this.settingsMenu.add(defaultSettingsItem);

        final JMenu plotsMenu = new JMenu("Plots");
        plotsMenu.setMnemonic(KeyEvent.VK_P);
        add(plotsMenu);

        final JMenuItem savePlotsItem = new JMenuItem("Save plots ...");
        savePlotsItem.setMnemonic(KeyEvent.VK_S);
        savePlotsItem.setActionCommand(Commands.SAVE_PLOTS);
        savePlotsItem.addActionListener(listener);
        savePlotsItem.setEnabled(true);
        savePlotsItem.setToolTipText("Save all plots to a file");
        plotsMenu.add(savePlotsItem);

        final JMenuItem clearPlotsItem = new JMenuItem("Clear plots");
        clearPlotsItem.setMnemonic(KeyEvent.VK_C);
        clearPlotsItem.setActionCommand(Commands.CLEAR_PLOTS);
        clearPlotsItem.addActionListener(listener);
        clearPlotsItem.setEnabled(true);
        clearPlotsItem.setToolTipText("Clear the AIDA plots");
        plotsMenu.add(clearPlotsItem);
        
        popupItem = new JCheckBox("Plot popup");
        popupItem.setActionCommand(Commands.PLOT_POPUP);
        popupItem.addActionListener(listener);
        popupItem.setEnabled(true);
        popupItem.setToolTipText("Enable plot popup window");
        popupItem.setSelected(true);
        plotsMenu.add(popupItem);
        
        final JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        add(toolsMenu);

        final JMenuItem screenshotItem = new JMenuItem("Save Screenshot ...");
        screenshotItem.setMnemonic(KeyEvent.VK_S);
        screenshotItem.setActionCommand(Commands.SAVE_SCREENSHOT);
        screenshotItem.addActionListener(listener);
        screenshotItem.setEnabled(true);
        screenshotItem.setToolTipText("Save a screenshot to a graphics file");
        toolsMenu.add(screenshotItem);

        this.logItem = new JMenuItem("Log to File ...");
        this.logItem.setMnemonic(KeyEvent.VK_L);
        this.logItem.setActionCommand(Commands.LOG_TO_FILE);
        this.logItem.addActionListener(listener);
        this.logItem.setEnabled(true);
        this.logItem.setToolTipText("Redirect System.out to a file instead of terminal");
        toolsMenu.add(this.logItem);

        final JMenu windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        add(windowMenu);

        final JMenuItem maximizeItem = new JMenuItem("Maximize");
        maximizeItem.setMnemonic(KeyEvent.VK_M);
        maximizeItem.setActionCommand(Commands.MAXIMIZE_WINDOW);
        maximizeItem.addActionListener(listener);
        maximizeItem.setEnabled(true);
        maximizeItem.setToolTipText("Maximize the application window");
        windowMenu.add(maximizeItem);

        final JMenuItem minimizeItem = new JMenuItem("Minimize");
        minimizeItem.setMnemonic(KeyEvent.VK_I);
        minimizeItem.setActionCommand(Commands.MINIMIZE_WINDOW);
        minimizeItem.addActionListener(listener);
        minimizeItem.setEnabled(true);
        minimizeItem.setToolTipText("Minimize the application window");
        windowMenu.add(minimizeItem);

        final JMenuItem defaultsItem = new JMenuItem("Restore Defaults");
        defaultsItem.setMnemonic(KeyEvent.VK_D);
        defaultsItem.setActionCommand(Commands.DEFAULT_WINDOW);
        defaultsItem.addActionListener(listener);
        defaultsItem.setEnabled(true);
        defaultsItem.setToolTipText("Restore the window defaults");
        windowMenu.add(defaultsItem);
    }

    /**
     * The {@link java.awt.event.ActionEvent} handling, which may set values on the configuration
     * model from the GUI.
     *
     * @param the {@link java.awt.event.ActionEvent} to handle
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        try {
            this.getConfigurationModel().removePropertyChangeListener(this);        
            if (e.getActionCommand().equals(Commands.DATA_SOURCE_CHANGED)) {
                if (!this.configurationModel.getDataSourceType().equals(DataSourceType.ET_SERVER)) {
                    this.closeFileItem.setEnabled(true);
                } else {
                    this.closeFileItem.setEnabled(false);
                }
            }            
        } finally {
            this.getConfigurationModel().addPropertyChangeListener(this);
        }
    }

    /**
     * The {@link java.beans.PropertyChangeEvent} handling which sets GUI values from the model.
     *
     * @param the {@link java.beans.PropertyChangeEvent} to handle
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        this.configurationModel.removePropertyChangeListener(this);
        try {
            if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
                final ConnectionStatus status = (ConnectionStatus) evt.getNewValue();
                final boolean connected = status.equals(ConnectionStatus.CONNECTED);
                this.closeFileItem.setEnabled(!connected);
                this.openFileItem.setEnabled(!connected);
            } else if (evt.getPropertyName().equals(ConfigurationModel.LOG_TO_FILE_PROPERTY)) {
                final Boolean logToFile = (Boolean) evt.getNewValue();
                if (logToFile) {
                    // Toggle log item state to send to terminal.
                    this.logItem.setText("Log to Terminal ...");
                    this.logItem.setActionCommand(Commands.LOG_TO_TERMINAL);
                    this.logItem.setToolTipText("Log messages to the terminal");
                } else {
                    // Toggle log item state to send to file.
                    this.logItem.setText("Log to File ...");
                    this.logItem.setActionCommand(Commands.LOG_TO_FILE);
                    this.logItem.setToolTipText("Log messages to a file");
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.RECENT_FILES_PROPERTY)) {
                setRecentFiles(this.configurationModel.getRecentFilesList());
            }
        } finally {
            this.configurationModel.addPropertyChangeListener(this);
        }
    }      

    /**
     * Set the recent file menu items.
     *
     * @param recentFiles the list of recent files from the model
     */
    private void setRecentFiles(final List<String> recentFiles) {
        this.recentFilesMenu.removeAll();
        int fileMnemonic = RECENT_FILES_START_INDEX; /* starts at KeyEvent.VK_0 */
        for (final String recentFile : recentFiles) {
            final RecentFileItem recentFileItem = new RecentFileItem(recentFile, fileMnemonic);
            recentFileItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    final String recentFile = ((RecentFileItem) e.getSource()).getPath();
                    final DataSourceType dst = DataSourceType.getDataSourceType(recentFile);
                    MenuBar.this.configurationModel.setDataSourcePath(recentFile);
                    MenuBar.this.configurationModel.setDataSourceType(dst);
                }
            });
            this.recentFilesMenu.add(recentFileItem);
            ++fileMnemonic;
        }
    }

    @Override
    public ConfigurationModel getConfigurationModel() {
        return this.configurationModel;
    }

    @Override
    public void setConfigurationModel(ConfigurationModel configurationModel) {
        this.configurationModel = configurationModel;
        this.configurationModel.addPropertyChangeListener(this);
    }
}
