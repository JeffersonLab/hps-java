package org.hps.monitoring;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * The panel for setting job parameters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: JobPanel.java,v 1.17 2013/11/05 17:15:04 jeremy Exp $
 */
class JobPanel extends FieldsPanel {

    private JTextField detectorNameField;
    private JCheckBox disconnectWarningCheckBox;    
    private JCheckBox disconnectOnErrorCheckBox;
    private JTextField eventBuilderField;
    private JComboBox steeringTypeComboBox;
    private JTextField steeringFileField;
    private JComboBox steeringResourcesComboBox;
    private JCheckBox logCheckBox;
    private JTextField logFileField;
    private JCheckBox remoteAidaCheckBox;
    private JTextField remoteAidaNameField;
    private JCheckBox pauseModeCheckBox;
    private JComboBox logLevelComboBox;
    private JTextField aidaSaveField;
    private JCheckBox aidaSaveCheckBox;
    
    private String steeringPackage = "org/hps/steering/monitoring/";

    private String defaultEventBuilderClassName = (new JobSettings()).eventBuilderClassName;

    private final static String[] steeringTypes = {"RESOURCE", "FILE"};
    final static int RESOURCE = 0;
    final static int FILE = 1;
    
    /**
     * The available LogLevel settings.
     */
    String[] logLevels = new String[] {
        Level.ALL.toString(),
        Level.FINEST.toString(),
        Level.FINER.toString(),
        Level.FINE.toString(),
        Level.CONFIG.toString(),
        Level.INFO.toString(),
        Level.WARNING.toString(),
        Level.SEVERE.toString(),
        Level.OFF.toString()};

    /**
     * Class constructor.
     */
    JobPanel() {

        super(new Insets(4, 2, 2, 4), true);
        setLayout(new GridBagLayout());

        pauseModeCheckBox = addCheckBox("Pause mode", false, true);
        disconnectOnErrorCheckBox = addCheckBox("Disconnect on error", false, true);
        disconnectWarningCheckBox = addCheckBox("Warn before disconnect", true, true);
        logLevelComboBox = addComboBox("Log Level", this.logLevels);
        logLevelComboBox.setActionCommand(MonitoringCommands.logLevelCmd);
        steeringTypeComboBox = addComboBox("Steering Type", steeringTypes);  
        steeringFileField = addField("Steering File", 35);  	      
        steeringResourcesComboBox = addComboBoxMultiline("Steering File Resource", 
                SteeringFileUtil.getAvailableSteeringFileResources(steeringPackage));
        //steeringResourcesComboBox = addComboBox("Steering File Resource", new String[]{});
        steeringResourcesComboBox.setActionCommand(MonitoringCommands.steeringResourceCmd);
        detectorNameField = addField("Detector Name", 20);
        eventBuilderField = addField("Event Builder Class", 30);
        eventBuilderField.setActionCommand(MonitoringCommands.eventBuilderCmd);
        logCheckBox = addCheckBox("Log to File", false, false);
        logFileField = addField("Log File", "", "Full path to log file.", 30, false);
        aidaSaveCheckBox = addCheckBox("Save AIDA at End of Job", false, false);
        aidaSaveField = addField("AIDA Auto Save File Name", "", 30, false);
        remoteAidaCheckBox = addCheckBox("Enable remote AIDA", false, true);
        remoteAidaNameField = addField("Remote AIDA name", "", 15, true);
        
        // Set default job settings.
        setJobSettings(new JobSettings());
    }
    
    /**
     * Enable this component.
     * @param enable Whether to enable or not.
     */
    void enableJobPanel(boolean enable) {
        detectorNameField.setEnabled(enable);
        eventBuilderField.setEnabled(enable);
        pauseModeCheckBox.setEnabled(enable);
        remoteAidaCheckBox.setEnabled(enable);
        remoteAidaNameField.setEnabled(enable);
        steeringTypeComboBox.setEnabled(enable);
        steeringFileField.setEnabled(enable);   
        steeringResourcesComboBox.setEnabled(enable);
    }   
    
    /**
     * Attaches the ActionListener from the main app to GUI components in this class.
     */
    void addActionListener(ActionListener listener) {
        steeringResourcesComboBox.addActionListener(listener);
        logLevelComboBox.addActionListener(listener);
        eventBuilderField.addActionListener(listener);
    }
        
    /**
     * Choose a file name for the automatic AIDA save file.
     */
    void chooseAidaAutoSaveFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose AIDA Auto Save File");
        int r = fc.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String fileName = file.getPath();
            int extIndex = fileName.lastIndexOf(".");
            if ((extIndex == -1) || !(fileName.substring(extIndex + 1, fileName.length())).toLowerCase().equals("aida")) {
                fileName = fileName + ".aida";
            }
            final String fileName2 = fileName;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    aidaSaveCheckBox.setSelected(true);
                    aidaSaveField.setText(fileName2);
                }
            });
        }
    }

    /**
     * Check that the steering file or resource is valid.
     * @return True if steering is valid; false if not.
     */
    boolean checkSteering() {
        String steering = steeringFileField.getText();
        int steeringType = steeringTypeComboBox.getSelectedIndex();		
        if (RESOURCE == steeringType) {
            // Check that steering resource exists.
            InputStream is = getClass().getResourceAsStream(steering);
            if (is == null) {
                return false;
            } else {
                return true;
            }
        } else if (FILE == steeringType) {
            // Check that steering file exists.
            File f = new File(steering);
            if (!f.exists()) {
                return false;
            } else {
                return true;
            }
        } else {
            throw new IllegalArgumentException("The steeringType is invalid: " + steeringType);
        }               
    }
     
    /**
     * Setup the event builder from the field setting.
     * @return True if builder is setup successfully; false if not.
     */
    void editEventBuilder() {
        String eventBuilderClassName = eventBuilderField.getText();
        boolean okay = true;
        try {
            // Test that the event builder can be created without throwing any exceptions.
            Class eventBuilderClass = Class.forName(eventBuilderClassName);
            eventBuilderClass.newInstance();
        } 
        catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "The event builder class does not exist.");
            okay = false;
        } 
        catch (InstantiationException e) {
            JOptionPane.showMessageDialog(this, "Failed to instantiate instance of event builder class.");
            okay = false;
        } 
        catch (IllegalAccessException e) {
            JOptionPane.showMessageDialog(this, "Couldn't access event builder class.");
            okay = false;
        }
        
        if (!okay)
            resetEventBuilder();
    }

    /**
     * Reset the event builder to the default.
     */
    private void resetEventBuilder() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                eventBuilderField.setText(defaultEventBuilderClassName);
            }
        });
    }

    /**
     * Get the event builder class name.
     * @return The event builder class name.
     */
    String getEventBuilderClassName() {
        return eventBuilderField.getText();
    }

    /**
     * Set the steering file field.
     * @param steeringFile The path to the file.
     */
    void setSteeringFile(final String steeringFile) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                steeringFileField.setText(steeringFile);
            }
        });
    }
    
    /**
     * Set the steering file resource.
     * @param s The resource path.
     */
    void setSteeringResource(final String s) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                steeringResourcesComboBox.setSelectedItem(s);
            }
        });
    }

    /**
     * Set the name of the detector.
     * @param detectorName The name of the detector.
     */
    void setDetectorName(final String detectorName) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                detectorNameField.setText(detectorName);
            }
        });
    }

    /**
     * Get the steering file or resource path from the field setting.
     * @return The steering file or resource path.
     */
    String getSteering() {
        if (getSteeringType() == FILE) {
            return steeringFileField.getText();
        }
        else if (getSteeringType() == RESOURCE) {
            return (String) steeringResourcesComboBox.getSelectedItem();
        }
        else {
            return null;
        }
    }

    /**
     * Get the type of steering, file or resource.
     * @return The type of steering.
     */
    int getSteeringType() {
        return steeringTypeComboBox.getSelectedIndex();
    }

    /**
     * Get the name of the detector.
     * @return The name of the detector.
     */
    String getDetectorName() {
        return detectorNameField.getText();
    }

    /**
     * 
     * @param defaultEventBuilderClassName
     */
    void setDefaultEventBuilder(final String defaultEventBuilderClassName) {
        this.defaultEventBuilderClassName = defaultEventBuilderClassName;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                eventBuilderField.setText(defaultEventBuilderClassName);
            }
        });
    }

    /**
     * Check if pause mode is selected.
     * @return True if pause mode is enabled; false if not.
     */
    boolean pauseMode() {
        return this.pauseModeCheckBox.isSelected();
    }
    
    /**
     * Set the pause mode.
     * @param p The pause mode; true for on; false for off.
     */
    void enablePauseMode(final boolean p) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pauseModeCheckBox.setSelected(p);
            }
        });
    }

    /**
     * Get the log level from the combo box. 
     * @return The log level.
     */
    Level getLogLevel() {
        return Level.parse((String) logLevelComboBox.getSelectedItem());
    }
    
    /**
     * Get the disconnect on error setting from the check box.
     * @return The disconnect on error setting.
     */
    boolean disconnectOnError() {
        return disconnectOnErrorCheckBox.isSelected();
    }
    
    /**
     * Get the warn on disconnect setting.
     * @return The warn on disconnect setting.
     */
    boolean warnOnDisconnect() {
        return disconnectWarningCheckBox.isSelected();
    }
    
    /**
     * Get the log to file setting.
     * @return The log to file setting.
     */
    boolean logToFile() {
        return logCheckBox.isSelected();
    }
    
    /**
     * Get the log file name.
     * @return The log file name.
     */
    String getLogFileName() {
        return logFileField.getText();
    }    
    
    /**
     * Get whether the AIDA server is enabled.
     * @return True if the AIDA server is enabled; false if not.
     */
    boolean isAidaServerEnabled() {
        return remoteAidaCheckBox.isSelected();
    }
    
    /**
     * Get whether AIDA autosave is enabled.
     * @return True if AIDA autosave is enabled; false if not.
     */
    boolean isAidaAutoSaveEnabled() {
        return aidaSaveCheckBox.isSelected();
    }
    
    /**
     * Get the AIDA autosave file name.
     * @return The AIDA autosave file name.
     */
    String getAidaAutoSaveFileName() {
        return aidaSaveField.getText();
    }
    
    /**
     * Get the name of the remote AIDA server.
     * @return The remote AIDA server name.
     */
    String getRemoteAidaName() {
        return remoteAidaNameField.getText();
    }
                  
    /**
     * Set whether to disconnect if errors occur.
     * @param b The disconnect on error setting.
     */
    private void setDisconnectOnError(final boolean b) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                disconnectOnErrorCheckBox.setSelected(b);
            }
        });        
    }
    
    /**
     * Set whether to warn before a disconnect occurs.
     * @param b The warn before disconnect setting.
     */
    private void setWarnBeforeDisconnect(final boolean b) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                disconnectWarningCheckBox.setSelected(b);
            }
        });            
    }
    
    /**
     * Set the steering type.
     * @param t The steering type.
     */
    void setSteeringType(final int t) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                steeringTypeComboBox.setSelectedIndex(t);
            }
        });        
    }
    
    /**
     * Set the log level.
     * @param level The log level.
     */
    private void setLogLevel(final Level level) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logLevelComboBox.setSelectedItem(level.toString());
            }
        });               
    }
    
    /**
     * Set the fully qualified class name of the event builder.
     * @param c The class name of the event builder.
     */
    private void setEventBuilder(final String c) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                eventBuilderField.setText(c);
            }
        });        
    }
    
    /**
     * Set whether to log to a file.
     * @param b The log to file setting.
     */
    void setLogToFile(final boolean b) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logCheckBox.setSelected(b);
            }
        });        
    }
    
    /**
     * Set the log file name.
     * @param s The log file name.
     */
    void setLogFile(final String s) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logFileField.setText(s);
            }
        });
    }
    
    /**
     * Set AIDA autosave.
     * @param b The AIDA autosave setting.
     */
    private void enableAidaAutoSave(final boolean b) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                aidaSaveCheckBox.setSelected(b);
            }
        });
    }
    
    /**
     * Set the AIDA autosave file name.
     * @param s The AIDA autosave file name.
     */
    private void setAidaAutoSaveFileName(final String s) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                aidaSaveField.setText(s);
            }
        });
    }
    
    /**
     * Enable remote AIDA.
     * @param b The remote AIDA setting; true to enable; false to disable.
     */
    private void enableRemoteAida(final boolean b) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                remoteAidaCheckBox.setSelected(b);
            }
        });
    }
    
    /**
     * Set the name of the remote AIDA server.
     * @param s The name of the remote AIDA server.
     */
    private void setRemoteAidaName(final String s) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                remoteAidaNameField.setText(s);
            }
        });
    }
    
    /**
     * Get the resource path for the steering file.
     * @return The resource path for the steering file.
     */
    private String getSelectedSteeringResource() {
        return (String) steeringResourcesComboBox.getSelectedItem();
    }
    
    /**
     * Get the path to the steering file path.
     * @return The steering file path.
     */
    private String getSteeringFile() {
        return steeringFileField.getText();
    }
            
    /**
     * Gather {@link JobSettings} parameters from GUI and return a JobSettings object.
     * @return The JobSettings from the JobPanel.
     */
    JobSettings getJobSettings() {
        JobSettings settings = new JobSettings();
        settings.pauseMode = pauseMode();
        settings.disconnectOnError = disconnectOnError();
        settings.warnBeforeDisconnect = warnOnDisconnect();
        settings.logLevel = getLogLevel();
        settings.steeringType = getSteeringType();
        settings.steeringFile = getSteeringFile();
        settings.steeringResource = getSelectedSteeringResource();
        settings.detectorName = getDetectorName();
        settings.eventBuilderClassName = getEventBuilderClassName();
        settings.logToFile = logToFile();
        settings.logFileName = getLogFileName();
        settings.autoSaveAida = isAidaAutoSaveEnabled();
        settings.autoSaveAidaFileName = getAidaAutoSaveFileName();
        settings.remoteAidaName = getRemoteAidaName();
        settings.enableRemoteAida = remoteAidaCheckBox.isSelected();
        return settings;
    }
               
    /**
     * Set the JobPanel parameters from a JobSettings object.
     * @param settings The JobSettings to load.
     */
    void setJobSettings(JobSettings settings) {
        enablePauseMode(settings.pauseMode);
        setDisconnectOnError(settings.disconnectOnError);
        setWarnBeforeDisconnect(settings.warnBeforeDisconnect);
        setLogLevel(settings.logLevel);
        setSteeringType(settings.steeringType);
        setSteeringFile(settings.steeringFile);
        setSteeringResource(settings.steeringResource);
        setDetectorName(settings.detectorName);
        setEventBuilder(settings.eventBuilderClassName);
        setLogToFile(settings.logToFile);        
        setLogFile(settings.logFileName);
        enableAidaAutoSave(settings.autoSaveAida);
        setAidaAutoSaveFileName(settings.autoSaveAidaFileName);
        enableRemoteAida(settings.enableRemoteAida);
        setRemoteAidaName(settings.remoteAidaName);
    }
    
    /**
     * Reset the JobPanel to its defaults.
     */
    void resetJobSettings() {
        setJobSettings(new JobSettings());
    }    
}