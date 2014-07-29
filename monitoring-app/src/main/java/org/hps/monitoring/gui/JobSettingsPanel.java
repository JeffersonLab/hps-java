package org.hps.monitoring.gui;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.hps.monitoring.config.Configurable;
import org.hps.monitoring.config.Configuration;
import org.hps.monitoring.enums.SteeringType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * The panel for setting job parameters.
 */
class JobSettingsPanel extends AbstractFieldsPanel implements Configurable, ActionListener {

    private JTextField detectorNameField;
    //private JCheckBox disconnectOnErrorCheckBox;
    private JTextField eventBuilderField;
    private JComboBox<?> steeringTypeComboBox;
    private JTextField steeringFileField;
    private JComboBox<?> steeringResourcesComboBox;
    private JCheckBox logCheckBox;
    private JTextField logFileField;
    private JCheckBox pauseModeCheckBox;
    private JComboBox<?> logLevelComboBox;
    private JTextField aidaSaveField;
    private JCheckBox aidaSaveCheckBox;
    
    private String steeringPackage = "org/hps/steering/monitoring/";

    // FIXME: This should probably be in some kind of global config file.
    private String DEFAULT_EVENT_BUILDER_CLASS_NAME = "org.hps.evio.LCSimTestRunEventBuilder";

    private final static String[] steeringTypes = { SteeringType.RESOURCE.name(), SteeringType.FILE.name()};
    
    Configuration config;
    
    /**
     * The available LogLevel settings as an array of strings.
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
        Level.OFF.toString()
    };

    /**
     * Class constructor.
     */
    JobSettingsPanel() {

        super(new Insets(4, 2, 2, 4), true);
        setLayout(new GridBagLayout());

        pauseModeCheckBox = addCheckBox("Pause mode", false, true);
        //disconnectOnErrorCheckBox = addCheckBox("Disconnect on error", false, true);
        
        logLevelComboBox = addComboBox("Log Level", this.logLevels);
        logLevelComboBox.setActionCommand(MonitoringCommands.SET_LOG_LEVEL);
        
        steeringTypeComboBox = addComboBox("Steering Type", steeringTypes);  
        steeringFileField = addField("Steering File", 35);
                     
        JButton steeringFileButton = addButton("Select Steering File");
        steeringFileButton.setActionCommand(MonitoringCommands.CHOOSE_STEERING_FILE);
        steeringFileButton.addActionListener(this);
        
        steeringResourcesComboBox = addComboBoxMultiline("Steering File Resource", 
                getAvailableSteeringFileResources(steeringPackage));
        steeringResourcesComboBox.setActionCommand(MonitoringCommands.SET_STEERING_RESOURCE);
        
        detectorNameField = addField("Detector Name", 20);
        
        eventBuilderField = addField("Event Builder Class", 30);
        eventBuilderField.setActionCommand(MonitoringCommands.SET_EVENT_BUILDER);
        
        logCheckBox = addCheckBox("Log to File", false, false);
        logCheckBox.setEnabled(false);
        
        logFileField = addField("Log File", "", "Full path to log file.", 30, false);
        
        aidaSaveCheckBox = addCheckBox("Save AIDA at End of Job", false, false);
        aidaSaveField = addField("AIDA Auto Save File Name", "", 30, false);        
    }
    
    /**
     * Enable this component.
     * @param enable Whether to enable or not.
     */
    void enableJobPanel(boolean enable) {
        detectorNameField.setEnabled(enable);
        eventBuilderField.setEnabled(enable);
        pauseModeCheckBox.setEnabled(enable);
        steeringTypeComboBox.setEnabled(enable);
        steeringFileField.setEnabled(enable);   
        steeringResourcesComboBox.setEnabled(enable);
    }   
    
    /**
     * Attaches the ActionListener from the main app to GUI components in this class.
     */
    void addActionListener(ActionListener listener) {
        steeringResourcesComboBox.addActionListener(listener);
        //logLevelComboBox.addActionListener(listener);
        logFileField.addActionListener(listener);
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
     * Choose an lcsim steering file.
     */
    void chooseSteeringFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose an LCSim Steering File");
        fc.setCurrentDirectory(new File("."));
        int r = fc.showDialog(this, "Select ...");
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                checkSteeringFile(file);
                setSteeringFile(file);
            } catch (Exception e) {
                
            }
        }        
    }
    
    void checkSteeringFile(File file) throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(file);
        Element rootNode = document.getRootElement();
        if (!rootNode.getName().equals("lcsim")) {
            throw new IOException("Not an LCSim XML file.");
        }
    }
         
    /**
     * Setup the event builder from the field setting.
     * @return True if builder is setup successfully; false if not.
     */
    // FIXME: This method should throw an exception if an error occurs.
    void editEventBuilder() {
        String eventBuilderClassName = eventBuilderField.getText();
        boolean okay = true;
        try {
            // Test that the event builder can be created without throwing any exceptions.
            Class<?> eventBuilderClass = Class.forName(eventBuilderClassName);
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
                eventBuilderField.setText(DEFAULT_EVENT_BUILDER_CLASS_NAME);
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
    void setSteeringFile(final File steeringFile) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                steeringFileField.setText(steeringFile.getAbsolutePath());
            }
        });
        setSteeringType(SteeringType.FILE);
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
        setSteeringType(SteeringType.RESOURCE);
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
    
    String getSteeringResource() {
        return (String) steeringResourcesComboBox.getSelectedItem();
    }

    /**
     * Get the steering file or resource path from the field setting.
     * @return The steering file or resource path.
     */
    String getSteering() {
        if (getSteeringType().equals(SteeringType.FILE)) {
            return steeringFileField.getText();
        }
        else if (getSteeringType().equals(SteeringType.RESOURCE)) {
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
    SteeringType getSteeringType() {
        return SteeringType.values()[steeringTypeComboBox.getSelectedIndex()];
    }

    /**
     * Get the name of the detector.
     * @return The name of the detector.
     */
    String getDetectorName() {
        return detectorNameField.getText();
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
    //boolean disconnectOnError() {
    //    return disconnectOnErrorCheckBox.isSelected();
    //}
        
    /**
     * Get the log to file setting.
     * @return The log to file setting.
     */
    boolean isLogToFileEnabled() {
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
     * Set whether to disconnect if errors occur.
     * @param b The disconnect on error setting.
     */
    //private void setDisconnectOnError(final boolean b) {
    //    SwingUtilities.invokeLater(new Runnable() {
    //        public void run() {
    //            disconnectOnErrorCheckBox.setSelected(b);
    //        }
    //    });        
    //}
        
    /**
     * Set the steering type.
     * @param t The steering type.
     */
    void setSteeringType(final SteeringType steeringType) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                steeringTypeComboBox.setSelectedIndex(steeringType.ordinal());
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
    void setLogFile(final File file) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logFileField.setText(file.getAbsolutePath());
                setLogToFile(true);
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
     * Get the path to the steering file path.
     * @return The steering file path.
     */
    private String getSteeringFile() {
        return steeringFileField.getText();
    }
                 
    @Override
    public void load(Configuration config) {
        enablePauseMode(config.getBoolean("pauseMode"));
        setLogLevel(Level.parse(config.get("logLevel")));  
        setSteeringType(SteeringType.valueOf(config.get("steeringType")));
        if (config.hasKey("steeringFile"))
            setSteeringFile(new File(config.get("steeringFile")));
        if (config.hasKey("steeringResource")) {
            setSteeringResource(config.get("steeringResource"));
        }
        setDetectorName(config.get("detectorName"));
        setEventBuilder(config.get("eventBuilderClassName"));
        setLogToFile(config.getBoolean("logToFile"));
        setLogFile(new File(config.get("logFileName")));
        enableAidaAutoSave(config.getBoolean("autoSaveAida"));
        setAidaAutoSaveFileName(config.get("autoSaveAidaFileName"));
    }
    
    @Override
    public void save(Configuration config) {
        config.set("pauseMode", pauseMode());
        config.set("logLevel", getLogLevel().getName());
        config.set("steeringType", getSteeringType().name());
        config.set("steeringFile", getSteeringFile());
        config.set("steeringResource", getSteeringResource());
        config.set("detectorName", getDetectorName());
        config.set("eventBuilderClassName", getEventBuilderClassName());
        config.set("logToFile", isLogToFileEnabled());
        config.set("logFileName", getLogFileName());
        config.set("autoSaveAida", isAidaAutoSaveEnabled());
        config.set("autoSaveAidaFileName", getAidaAutoSaveFileName());
    }
    
    public void save() {
        save(config);
    }
    
    @Override
    public void set(Configuration config) {
        load(config);
        this.config = config;
    }

    @Override
    public void reset() {
        load(config);        
    }
    
    public Configuration getConfiguration() {
        return config;
    }
      
    /**
     * Get the files that end in .lcsim from all loaded jar files.
     * @return A list of embedded steering file resources.
     */
    public static String[] getAvailableSteeringFileResources(String packageName) {
        List<String> resources = new ArrayList<String>();
        URL url = JobSettingsPanel.class.getResource("MonitoringApplication.class");
        String scheme = url.getProtocol();
        if (!"jar".equals(scheme)) {
            throw new IllegalArgumentException("Unsupported scheme.  Only jar is allowed.");
        }
        try {
            JarURLConnection con = (JarURLConnection) url.openConnection();
            JarFile archive = con.getJarFile();
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".lcsim") && entry.getName().contains(packageName)) {
                    resources.add(entry.getName());
                }
            }
            archive.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }        
        java.util.Collections.sort(resources);
        String[] arr = new String[resources.size()];
        for (int i=0; i<arr.length; i++) {
            arr[i] = resources.get(i);
        }
        return arr;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(MonitoringCommands.CHOOSE_STEERING_FILE)) {
            this.chooseSteeringFile();
        }
    }
    
}