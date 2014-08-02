package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.Commands.*;
import static org.hps.monitoring.gui.model.ConfigurationModel.*;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
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
import javax.swing.JTextField;

import org.hps.monitoring.enums.SteeringType;
import org.hps.monitoring.gui.model.ConfigurationModel;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * This is the GUI panel for setting job parameters.  It is connected to the global configuration via 
 * a {@link org.hps.monitoring.model.ConfigurationModel} object.
 */
// TODO: Add validity checks for event builder, lcsim steering files, etc. when edited and revert to old
//       values if invalid.
//       http://docs.oracle.com/javase/7/docs/api/javax/swing/JFormattedTextField.html
// TODO: Remove default values in components.  These should all come from the default configuration.
// TODO: Double check that all settings to and from the configuration are working properly.
class JobSettingsPanel extends AbstractFieldsPanel {

    private JTextField aidaSaveFileNameField;
    private JCheckBox aidaAutoSaveCheckbox;
    private JTextField detectorNameField;
    private JCheckBox disconnectOnErrorCheckBox;
    private JTextField eventBuilderField;
    private JTextField logFileNameField;
    private JComboBox<?> logLevelComboBox;
    private JCheckBox logToFileCheckbox;    
    private JTextField steeringFileField;
    private JComboBox<?> steeringResourcesComboBox;
    private JComboBox<?> steeringTypeComboBox;
                         
    // The package where steering resources must be located.
    static final String STEERING_PACKAGE = "org/hps/steering/monitoring/";
    
    // FIXME: This should be in the default global config file rather than hard-coded here.
    static final String DEFAULT_EVENT_BUILDER_CLASS_NAME = "org.hps.evio.LCSimTestRunEventBuilder";
            
    // This will connect this GUI component to the underlying global configuration.
    ConfigurationModel configurationModel;
                
    // The available LogLevel settings as an array of strings.     
    static final String[] LOG_LEVELS = new String[] {            
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
        
        disconnectOnErrorCheckBox = addCheckBox("Disconnect on error", false, true);
        disconnectOnErrorCheckBox.setActionCommand(DISCONNECT_ON_ERROR_CHANGED);
        disconnectOnErrorCheckBox.addActionListener(this);
        
        logLevelComboBox = addComboBox("Log Level", LOG_LEVELS);               
        logLevelComboBox.setActionCommand(Commands.LOG_LEVEL_CHANGED);
        logLevelComboBox.addActionListener(this);
        
        steeringTypeComboBox = addComboBox("Steering Type", 
                new String[] {SteeringType.RESOURCE.name(), SteeringType.FILE.name()});        
        steeringTypeComboBox.setActionCommand(STEERING_TYPE_CHANGED);
        steeringTypeComboBox.addActionListener(this);
        
        steeringFileField = addField("Steering File", 35);
        steeringFileField.addPropertyChangeListener("value", this);        
                     
        JButton steeringFileButton = addButton("Select Steering File");
        steeringFileButton.setActionCommand(Commands.CHOOSE_STEERING_FILE);
        steeringFileButton.addActionListener(this);
        
        steeringResourcesComboBox = addComboBoxMultiline("Steering File Resource", 
                getAvailableSteeringFileResources(STEERING_PACKAGE));
        steeringResourcesComboBox.setActionCommand(STEERING_RESOURCE_CHANGED);
        steeringResourcesComboBox.addActionListener(this);
        
        detectorNameField = addField("Detector Name", 20);
        detectorNameField.addPropertyChangeListener("value", this);
        
        eventBuilderField = addField("Event Builder Class", 30);
        eventBuilderField.setActionCommand(Commands.SET_EVENT_BUILDER);
        eventBuilderField.addPropertyChangeListener("value", this);
        
        logToFileCheckbox = addCheckBox("Log to File", false, false);
        logToFileCheckbox.setEnabled(false);
        logToFileCheckbox.setActionCommand(LOG_TO_FILE_CHANGED);
        logToFileCheckbox.addActionListener(this);
        
        logFileNameField = addField("Log File", "", "Full path to log file.", 30, false);
        logFileNameField.addPropertyChangeListener("value", this);
        
        aidaAutoSaveCheckbox = addCheckBox("Save AIDA at End of Job", false, false);
        aidaAutoSaveCheckbox.addActionListener(this);
        aidaAutoSaveCheckbox.setActionCommand(AIDA_AUTO_SAVE_CHANGED);
                
        aidaSaveFileNameField = addField("AIDA Auto Save File Name", "", 30, false);
        aidaSaveFileNameField.addPropertyChangeListener("value", this);
    }
    
    @Override
    public void setConfigurationModel(ConfigurationModel configModel) {
        
        // Set the ConfigurationModel reference.
        this.configurationModel = configModel;
        
        // This listener pushes GUI values into the configuration.
        this.configurationModel.addPropertyChangeListener(this);
        
        // This listener updates the GUI from changes in the configuration.
        this.configurationModel.addPropertyChangeListener(new JobSettingsChangeListener());
    }
    
    @Override
    public ConfigurationModel getConfigurationModel() {
        return configurationModel;
    }    
       
    /**
     * Enable this component.
     * @param enable Whether to enable or not.
     */
    void enableJobPanel(boolean enable) {
        detectorNameField.setEnabled(enable);
        eventBuilderField.setEnabled(enable);
        steeringTypeComboBox.setEnabled(enable);
        steeringFileField.setEnabled(enable);   
        steeringResourcesComboBox.setEnabled(enable);
    }   
    
    /**
     * Attaches the ActionListener from the main app to specific GUI components in this class.
     */
    void addActionListener(ActionListener listener) {
        eventBuilderField.addActionListener(listener);        
        logFileNameField.addActionListener(listener);
        logToFileCheckbox.addActionListener(listener);
        steeringResourcesComboBox.addActionListener(listener);
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
            final String finalFileName = fileName;
            configurationModel.setAutoSaveAida(true);
            configurationModel.setAidaFileName(finalFileName);                       
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
                configurationModel.setSteeringFile(file.getCanonicalPath());
            } catch (IOException | JDOMException e) {
                throw new RuntimeException("Error parsing the selected steering file.", e);
            }
        }        
    }
    
    /**
     * Parse the lcsim steering file to see if it appears to be valid.
     * @param file The input steering file.
     * @throws IOException if there is a basic IO problem.
     * @throws JDOMException if the XML is not valid.
     */
    private void checkSteeringFile(File file) throws IOException, JDOMException {
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
    /*
    void editEventBuilder() {
        String eventBuilderClassName = eventBuilderField.getText();
        boolean okay = true;
        try {
            // Test that the event builder can be created without throwing any exceptions.
            Class<?> eventBuilderClass = Class.forName(eventBuilderClassName);
            eventBuilderClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error setting up event builder.", e);
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
    /*
    // FIXME: Handle this with property change listener and use old value if new one is invalid.    
    private void resetEventBuilder() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                eventBuilderField.setText(DEFAULT_EVENT_BUILDER_CLASS_NAME);
            }
        });
    }
    */
                                     
    /**
     * Get the files with extension "lcsim" from all loaded jar files.
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
        if (e.getActionCommand().equals(Commands.CHOOSE_STEERING_FILE)) {
            this.chooseSteeringFile();
        } else if (DISCONNECT_ON_ERROR_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setDisconnectOnError(disconnectOnErrorCheckBox.isSelected());
        } else if (STEERING_TYPE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setSteeringType(SteeringType.valueOf((String) steeringTypeComboBox.getSelectedItem())); 
        } else if (STEERING_RESOURCE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setSteeringResource((String) steeringResourcesComboBox.getSelectedItem());
        } else if (LOG_TO_FILE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setLogToFile(logToFileCheckbox.isSelected());
        } else if (LOG_LEVEL_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setLogLevel(Level.parse((String) logLevelComboBox.getSelectedItem()));
        } else if (AIDA_AUTO_SAVE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setAutoSaveAida(aidaAutoSaveCheckbox.isSelected());
        }
    }

    /**
     * Updates the configuration with changes from the GUI component values.
     * The changes from the GUI are distinguishable by their component object.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        // FIXME: Anyway to make sure this is not needed?
        if (evt.getPropertyName().equals("ancestor"))
            return;

        Object source = evt.getSource();

        if (source == detectorNameField) {
            configurationModel.setDetectorName(detectorNameField.getText());
        } else if (source == eventBuilderField) {
            configurationModel.setEventBuilderClassName(eventBuilderField.getText());
        } else if (source == steeringFileField) {
            configurationModel.setSteeringFile(steeringFileField.getText());
        } else if (source == logFileNameField) {
            configurationModel.setLogFileName(logFileNameField.getText());
        } else if (source == aidaSaveFileNameField) {
            configurationModel.setAidaFileName(aidaSaveFileNameField.getText());
        } /* TODO */ else if (source == aidaAutoSaveCheckbox) {
            configurationModel.setAutoSaveAida(aidaAutoSaveCheckbox.isSelected());
        }
    }
    
    /**
     * Update the GUI from changes in the underlying configuration.
     * The changes from the configuration are distinguishable by their 
     * property name.
     */
    public class JobSettingsChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            
            if (evt.getPropertyName().equals("ancestor"))
                return;
                        
            Object value = evt.getNewValue();
            
            if (evt.getPropertyName().equals(DETECTOR_NAME_PROPERTY)) {
                detectorNameField.setText((String) value); 
            } if (evt.getPropertyName().equals(AIDA_AUTO_SAVE_PROPERTY)) {
                aidaAutoSaveCheckbox.setSelected((Boolean) value);
            } if (evt.getPropertyName().equals(AIDA_FILE_NAME_PROPERTY)) {
                aidaSaveFileNameField.setText((String) value);
            } if (evt.getPropertyName().equals(DISCONNECT_ON_ERROR_PROPERTY)) {
                disconnectOnErrorCheckBox.setSelected((Boolean) value);
            } if (evt.getPropertyName().equals(EVENT_BUILDER_PROPERTY)) {
                eventBuilderField.setText((String) value);                
            } if (evt.getPropertyName().equals(LOG_FILE_NAME_PROPERTY)) {
                logFileNameField.setText((String) value);                
            } if (evt.getPropertyName().equals(LOG_LEVEL_PROPERTY)) {
                logLevelComboBox.setSelectedItem(value.toString());
            } if (evt.getPropertyName().equals(LOG_TO_FILE_PROPERTY)) {
                logToFileCheckbox.setSelected((Boolean) value);
            } if (evt.getPropertyName().equals(STEERING_TYPE_PROPERTY)) {
                steeringTypeComboBox.setSelectedIndex(((SteeringType)value).ordinal());
            } if (evt.getPropertyName().equals(STEERING_FILE_PROPERTY)) {
                steeringFileField.setText((String) value);
            } if (evt.getPropertyName().equals(STEERING_RESOURCE_PROPERTY)) {
                steeringResourcesComboBox.setSelectedItem(value);
            }                                          
        }
    }   
}