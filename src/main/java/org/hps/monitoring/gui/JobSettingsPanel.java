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
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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
import org.hps.record.LCSimEventBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.reflections.Reflections;

/**
 * This is the GUI panel for setting job parameters. It is connected to the global configuration via
 * a {@link org.hps.monitoring.model.ConfigurationModel} object.
 */
class JobSettingsPanel extends AbstractFieldsPanel {

    private JComboBox<?> steeringResourcesComboBox;
    private JTextField steeringFileField;
    private JComboBox<?> steeringTypeComboBox;
    private JComboBox<String> detectorNameComboBox;
    private JComboBox<String> eventBuilderComboBox;
    private JTextField userRunNumberField;
    private JCheckBox freezeConditionsCheckBox;    
    private JCheckBox disconnectOnErrorCheckBox;
    private JCheckBox disconnectOnEndRunCheckBox;
    private JTextField aidaSaveFileNameField;
    private JCheckBox aidaAutoSaveCheckbox;        
    private JTextField logFileNameField;
    private JComboBox<?> logLevelComboBox;
    private JCheckBox logToFileCheckbox;
           
    // The package where steering resources must be located.
    static final String STEERING_PACKAGE = "org/hps/steering/monitoring/";

    // This connects the GUI to the global configuration model.
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

        steeringResourcesComboBox = addComboBoxMultiline("Steering File Resource", findSteeringResources(STEERING_PACKAGE));
        steeringResourcesComboBox.setActionCommand(STEERING_RESOURCE_CHANGED);
        steeringResourcesComboBox.addActionListener(this);
        
        steeringFileField = addField("Steering File", 35);
        steeringFileField.addPropertyChangeListener("value", this);
        
        JButton steeringFileButton = addButton("Select Steering File");
        steeringFileButton.setActionCommand(Commands.CHOOSE_STEERING_FILE);
        steeringFileButton.addActionListener(this);
        
        steeringTypeComboBox = addComboBox("Steering Type", new String[] { SteeringType.RESOURCE.name(), SteeringType.FILE.name() });
        steeringTypeComboBox.setActionCommand(STEERING_TYPE_CHANGED);
        steeringTypeComboBox.addActionListener(this);
        
        detectorNameComboBox = addComboBox("Detector Name", this.findDetectorNames());
        detectorNameComboBox.setActionCommand(DETECTOR_NAME_CHANGED);
        detectorNameComboBox.addActionListener(this);

        userRunNumberField = addField("User Run Number", "", 10, false);
        userRunNumberField.addPropertyChangeListener("value", this);
        userRunNumberField.setActionCommand(USER_RUN_NUMBER_CHANGED);
        userRunNumberField.setEnabled(true);
        userRunNumberField.setEditable(true);
        
        freezeConditionsCheckBox = addCheckBox("Freeze detector conditions", false, true);
        freezeConditionsCheckBox.addActionListener(this);
        freezeConditionsCheckBox.setActionCommand(FREEZE_CONDITIONS_CHANGED);
        
        eventBuilderComboBox = addComboBox("LCSim Event Builder", this.findEventBuilderClassNames());
        eventBuilderComboBox.setSize(24, eventBuilderComboBox.getPreferredSize().height);
        eventBuilderComboBox.setActionCommand(EVENT_BUILDER_CHANGED);
        eventBuilderComboBox.addActionListener(this);
        
        disconnectOnErrorCheckBox = addCheckBox("Disconnect on error", false, true);
        disconnectOnErrorCheckBox.setActionCommand(DISCONNECT_ON_ERROR_CHANGED);
        disconnectOnErrorCheckBox.addActionListener(this);

        disconnectOnEndRunCheckBox = addCheckBox("Disconnect on end run", false, true);
        disconnectOnEndRunCheckBox.setActionCommand(DISCONNECT_ON_END_RUN_CHANGED);
        disconnectOnEndRunCheckBox.addActionListener(this);

        logLevelComboBox = addComboBox("Log Level", LOG_LEVELS);
        logLevelComboBox.setActionCommand(Commands.LOG_LEVEL_CHANGED);
        logLevelComboBox.addActionListener(this);
                                            
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
    /*
     * void enableJobPanel(boolean enable) { detectorNameField.setEnabled(enable);
     * eventBuilderField.setEnabled(enable); steeringTypeComboBox.setEnabled(enable);
     * steeringFileField.setEnabled(enable); steeringResourcesComboBox.setEnabled(enable); }
     */

    /**
     * Attaches the ActionListener from the main app to specific GUI components in this class.
     */
    void addActionListener(ActionListener listener) {
        logFileNameField.addActionListener(listener);
        logToFileCheckbox.addActionListener(listener);
        steeringResourcesComboBox.addActionListener(listener);
        freezeConditionsCheckBox.addActionListener(listener);
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
            configurationModel.setAidaAutoSave(true);
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
            throw new IOException("Not an LCSim XML file: " + file.getPath());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(Commands.CHOOSE_STEERING_FILE)) {
            this.chooseSteeringFile();
        } else if (DISCONNECT_ON_ERROR_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setDisconnectOnError(disconnectOnErrorCheckBox.isSelected());
        } else if (DISCONNECT_ON_END_RUN_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setDisconnectOnEndRun(disconnectOnEndRunCheckBox.isSelected());
        } else if (STEERING_TYPE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setSteeringType(SteeringType.valueOf((String) steeringTypeComboBox.getSelectedItem()));
        } else if (STEERING_RESOURCE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setSteeringResource((String) steeringResourcesComboBox.getSelectedItem());
        } else if (LOG_TO_FILE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setLogToFile(logToFileCheckbox.isSelected());
        } else if (LOG_LEVEL_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setLogLevel(Level.parse((String) logLevelComboBox.getSelectedItem()));
        } else if (AIDA_AUTO_SAVE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setAidaAutoSave(aidaAutoSaveCheckbox.isSelected());
        } else if (EVENT_BUILDER_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setEventBuilderClassName((String) eventBuilderComboBox.getSelectedItem());
        } else if (DETECTOR_NAME_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setDetectorName((String) detectorNameComboBox.getSelectedItem());
        } else if (FREEZE_CONDITIONS_CHANGED.equals(e.getActionCommand())) {
            if (configurationModel.hasPropertyValue(USER_RUN_NUMBER_PROPERTY) && configurationModel.getUserRunNumber() != null) {
                configurationModel.setFreezeConditions(freezeConditionsCheckBox.isSelected());
            } else {
                throw new IllegalArgumentException("Conditions system may only be frozen if there is a valid user run number.");
            }
        }
    }

    /**
     * Updates the configuration with changes from the GUI component values. The changes from the
     * GUI are distinguishable by their component object.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        // FIXME: Anyway to make sure this is not needed?
        if (evt.getPropertyName().equals("ancestor"))
            return;

        Object source = evt.getSource();

        if (source == steeringFileField) {
            configurationModel.setSteeringFile(steeringFileField.getText());
        } else if (source == logFileNameField) {
            configurationModel.setLogFileName(logFileNameField.getText());
        } else if (source == aidaSaveFileNameField) {
            configurationModel.setAidaFileName(aidaSaveFileNameField.getText());
        } else if (source == aidaAutoSaveCheckbox) {
            configurationModel.setAidaAutoSave(aidaAutoSaveCheckbox.isSelected());
        } else if (source == userRunNumberField) {
            // Is run number being reset to null or empty?
            if (userRunNumberField.getText() == null || userRunNumberField.getText().isEmpty()) {
                System.out.println("resetting user run number back to null");
                // Update the model to null user run number and do not freeze the conditions system.
                configurationModel.setUserRunNumber(null);
                configurationModel.setFreezeConditions(false);
            } else {
                try {
                    System.out.println("setting new user run number " + evt.getNewValue());
                    // Parse the run number.  Need to catch errors because it might be an invalid string.
                    int userRunNumber = Integer.parseInt(userRunNumberField.getText());
                    configurationModel.setUserRunNumber(userRunNumber);
                    configurationModel.setFreezeConditions(true);
                    System.out.println("successfully set run number to userRunNumber");
                } catch (NumberFormatException e) {
                    System.out.println("bad number format so ignoring user run number " + evt.getNewValue());
                    userRunNumberField.setText((String) evt.getOldValue());
                    throw new IllegalArgumentException("The value " + evt.getNewValue() + " is not a valid run number.");
                }                            
            }
        }
    }

    /**
     * Update the GUI from changes in the underlying configuration. The changes from the
     * configuration are distinguishable by their property name.
     */
    private class JobSettingsChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {

            // FIXME: Anyway to make sure this is not needed?
            if (evt.getPropertyName().equals("ancestor"))
                return;

            Object value = evt.getNewValue();

            if (evt.getPropertyName().equals(DETECTOR_NAME_PROPERTY)) {
                detectorNameComboBox.setSelectedItem((String) value);
            } else if (evt.getPropertyName().equals(AIDA_AUTO_SAVE_PROPERTY)) {
                aidaAutoSaveCheckbox.setSelected((Boolean) value);
            } else if (evt.getPropertyName().equals(AIDA_FILE_NAME_PROPERTY)) {
                aidaSaveFileNameField.setText((String) value);
            } else if (evt.getPropertyName().equals(DISCONNECT_ON_ERROR_PROPERTY)) {
                disconnectOnErrorCheckBox.setSelected((Boolean) value);
            } else if (evt.getPropertyName().equals(DISCONNECT_ON_END_RUN_PROPERTY)) {
                disconnectOnEndRunCheckBox.setSelected((Boolean) value);
            } else if (evt.getPropertyName().equals(EVENT_BUILDER_PROPERTY)) {
                eventBuilderComboBox.setSelectedItem((String) value);
            } else if (evt.getPropertyName().equals(LOG_FILE_NAME_PROPERTY)) {
                logFileNameField.setText((String) value);
            } else if (evt.getPropertyName().equals(LOG_LEVEL_PROPERTY)) {
                logLevelComboBox.setSelectedItem(value.toString());
            } else if (evt.getPropertyName().equals(LOG_TO_FILE_PROPERTY)) {
                logToFileCheckbox.setSelected((Boolean) value);
            } else if (evt.getPropertyName().equals(STEERING_TYPE_PROPERTY)) {
                steeringTypeComboBox.setSelectedIndex(((SteeringType) value).ordinal());
            } else if (evt.getPropertyName().equals(STEERING_FILE_PROPERTY)) {
                if (value != null) {                    
                    steeringFileField.setText((String) value);
                } else {
                    // A null value here is actually okay and means this field should be reset to have no value.
                    steeringFileField.setText(null);
                }
            } else if (evt.getPropertyName().equals(STEERING_RESOURCE_PROPERTY)) {
                steeringResourcesComboBox.setSelectedItem(value);
            } else if (evt.getPropertyName().equals(USER_RUN_NUMBER_PROPERTY)) {
                if (value != null) {
                    userRunNumberField.setText(Integer.toString((int)value));
                } else {
                    userRunNumberField.setText(null);
                }
            } else if (evt.getPropertyName().equals(FREEZE_CONDITIONS_PROPERTY)) {
                if (value != null) {
                    freezeConditionsCheckBox.setSelected((Boolean) value);
                }
            }
        }
    }
    
    /**
     * Get the files with extension 'lcsim' from all loaded jar files.
     * @param packageName The package name for filtering the resources.
     * @return A list of embedded steering file resources.
     */
    private static String[] findSteeringResources(String packageName) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        java.util.Collections.sort(resources);
        String[] arr = new String[resources.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = resources.get(i);
        }
        return arr;
    }
    
    /**
     * Find all classes that implement {@link org.hps.record.LCSimEventBuilder} and return
     * a list of their canonical names.
     * @return The list of classes implementing LCSimEventBuilder.
     */
    private static String[] findEventBuilderClassNames() {
        Reflections reflections = new Reflections("org.hps");
        Set<Class<? extends LCSimEventBuilder>> subTypes = reflections.getSubTypesOf(LCSimEventBuilder.class);
        Set<String> classNames = new HashSet<String>();
        for (Class<? extends LCSimEventBuilder> type : subTypes) {
            classNames.add(type.getCanonicalName());
        }
        return classNames.toArray(new String[classNames.size()]);        
    }
 
    /**
     * Find a list of available detector names.
     * Only those detectors that have names starting with "HPS" in their
     * detector.properties files will be returned.
     * @return The list of available detector names.
     */
    private static String[] findDetectorNames() {
        ClassLoader classLoader = JobSettingsPanel.class.getClassLoader();
        List<String> detectorNames = new ArrayList<String>();
        URL url = JobSettingsPanel.class.getResource("MonitoringApplication.class");
        String protocol = url.getProtocol();
        if (!"jar".equals(protocol)) {
            throw new RuntimeException("Unsupported URL protocol: " + url.getProtocol());
        }
        try {
            JarURLConnection con = (JarURLConnection) url.openConnection();
            JarFile archive = con.getJarFile();
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith("detector.properties")) {
                    InputStream inputStream = classLoader.getResourceAsStream(entry.getName());
                    if (inputStream == null) {
                        throw new RuntimeException("Failed to load jar entry: " + entry.getName());
                    }
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    String detectorName = properties.getProperty("name");
                    if (detectorName.startsWith("HPS")) {
                        detectorNames.add(detectorName);
                    }
                }
            }
            archive.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Collections.sort(detectorNames);
        return detectorNames.toArray(new String[detectorNames.size()]);
    }
}