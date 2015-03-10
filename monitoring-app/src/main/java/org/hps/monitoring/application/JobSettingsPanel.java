package org.hps.monitoring.application;

import static org.hps.monitoring.application.Commands.*;
import static org.hps.monitoring.application.model.ConfigurationModel.*;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.SteeringType;
import org.hps.monitoring.application.util.ResourceUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * This is the GUI panel for setting job parameters. It is connected to the global configuration via
 * a {@link org.hps.monitoring.model.ConfigurationModel} object.
 */
class JobSettingsPanel extends AbstractFieldsPanel {

    private JComboBox<?> steeringResourcesComboBox;
    private JTextField steeringFileField;
    private JComboBox<?> steeringTypeComboBox;
    private JComboBox<String> detectorNameComboBox;
    private JTextField detectorAliasField;
    private JComboBox<String> conditionsTagComboBox;
    private JComboBox<String> eventBuilderComboBox;
    private JTextField userRunNumberField;
    private JCheckBox freezeConditionsCheckBox;    
    private JTextField maxEventsField;
    private JCheckBox disconnectOnErrorCheckBox;
    private JCheckBox disconnectOnEndRunCheckBox;    
    private JComboBox<?> logLevelComboBox;
    private JCheckBox logToFileCheckbox;
    private JTextField logFileNameField;
           
    // The package where steering resources must be located.
    static final String STEERING_PACKAGE = "org/hps/steering/monitoring/";

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
    JobSettingsPanel(ConfigurationModel model) {

        super(new Insets(5, 3, 3, 5), true);
        
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        model.addPropertyChangeListener(this);
        
        setLayout(new GridBagLayout());

        steeringResourcesComboBox = addComboBoxMultiline("Steering File Resource", ResourceUtil.findSteeringResources(STEERING_PACKAGE));
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
        
        detectorNameComboBox = addComboBox("Detector Name", ResourceUtil.findDetectorNames());
        detectorNameComboBox.setActionCommand(DETECTOR_NAME_CHANGED);
        detectorNameComboBox.addActionListener(this);
        
        detectorAliasField = addField("Detector Resources Directory", "", 35, true);
        detectorAliasField.setActionCommand(DETECTOR_ALIAS_CHANGED);
        detectorAliasField.addPropertyChangeListener("value", this);
        detectorAliasField.addActionListener(this);
        
        JButton compactXmlButton = addButton("Select Compact Xml File");
        compactXmlButton.setActionCommand(Commands.CHOOSE_COMPACT_FILE);
        compactXmlButton.addActionListener(this);

        userRunNumberField = addField("User Run Number", "", 10, true);
        userRunNumberField.addPropertyChangeListener("value", this);
        userRunNumberField.setActionCommand(USER_RUN_NUMBER_CHANGED);
        userRunNumberField.setEnabled(true);
        userRunNumberField.setEditable(true);
        
        
        conditionsTagComboBox = addComboBox("Conditions Tag", ResourceUtil.getConditionsTags());
        conditionsTagComboBox.addItem("");
        conditionsTagComboBox.setSelectedItem("");
        conditionsTagComboBox.setActionCommand(CONDITIONS_TAG_CHANGED);
        conditionsTagComboBox.addActionListener(this);
        conditionsTagComboBox.setEditable(false);
        conditionsTagComboBox.setEnabled(true);
                
        freezeConditionsCheckBox = addCheckBox("Freeze detector conditions", false, true);
        freezeConditionsCheckBox.addActionListener(this);
        freezeConditionsCheckBox.setActionCommand(FREEZE_CONDITIONS_CHANGED);
        
        maxEventsField = addField("Max Events", "-1", 10, false);
        maxEventsField.addPropertyChangeListener("value", this);
        maxEventsField.setEnabled(true);
        maxEventsField.setEditable(true);
        
        eventBuilderComboBox = addComboBox("LCSim Event Builder", ResourceUtil.findEventBuilderClassNames());
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

        logFileNameField = addField("Log File Name", "", "Full path to log file.", 50, false);
        logFileNameField.setEditable(false);
    }

    @Override
    public ConfigurationModel getConfigurationModel() {
        return configurationModel;
    }

    /**
     * Attaches the ActionListener from the main app to specific GUI components in this class.
     */
    public void addActionListener(ActionListener listener) {
        steeringResourcesComboBox.addActionListener(listener);
        freezeConditionsCheckBox.addActionListener(listener);
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
     * This filter will accept only files called compact.xml which
     * should be an LCSim detector description file. 
     */
    static class CompactFileFilter extends FileFilter {

        public CompactFileFilter() {            
        }
        
        @Override
        public boolean accept(File pathname) {
            if (pathname.getName().equals("compact.xml")) {
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public String getDescription() {
            return "Compact XML files";
        }        
    }
    
    
    /**
     * Choose a compact XML file to override the one embedded in the jar as a resource.
     */
    void chooseCompactFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose a Compact XML File");
        fc.setCurrentDirectory(new File("."));
        fc.setFileFilter(new CompactFileFilter());
        int r = fc.showDialog(this, "Select ...");
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            configurationModel.setDetectorAlias(file.getParent());
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
    public void actionPerformed(ActionEvent event) {

        //System.out.println("JobSettingsPanel.actionPerformed - " + event.getActionCommand());
        //System.out.println("  source: " + event.getSource());

        try {
            configurationModel.removePropertyChangeListener(this);
            if (event.getActionCommand().equals(Commands.CHOOSE_STEERING_FILE)) {
                chooseSteeringFile();
            } else if (event.getActionCommand().equals(Commands.CHOOSE_COMPACT_FILE)) {
                chooseCompactFile();
            } else if (DISCONNECT_ON_ERROR_CHANGED.equals(event.getActionCommand())) {
                configurationModel.setDisconnectOnError(disconnectOnErrorCheckBox.isSelected());
            } else if (DISCONNECT_ON_END_RUN_CHANGED.equals(event.getActionCommand())) {
                configurationModel.setDisconnectOnEndRun(disconnectOnEndRunCheckBox.isSelected());
            } else if (STEERING_TYPE_CHANGED.equals(event.getActionCommand())) {
                configurationModel.setSteeringType(SteeringType.valueOf((String) steeringTypeComboBox.getSelectedItem()));
            } else if (STEERING_RESOURCE_CHANGED.equals(event.getActionCommand())) {
                configurationModel.setSteeringResource((String) steeringResourcesComboBox.getSelectedItem());
            } else if (LOG_LEVEL_CHANGED.equals(event.getActionCommand())) {
                configurationModel.setLogLevel(Level.parse((String) logLevelComboBox.getSelectedItem()));
            } else if (EVENT_BUILDER_CHANGED.equals(event.getActionCommand())) {
                configurationModel.setEventBuilderClassName((String) eventBuilderComboBox.getSelectedItem());
            } else if (DETECTOR_NAME_CHANGED.equals(event.getActionCommand())) {
                try {
                    configurationModel.setDetectorName((String) detectorNameComboBox.getSelectedItem());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            } else if (FREEZE_CONDITIONS_CHANGED.equals(event.getActionCommand())) {
                if (configurationModel.hasPropertyKey(USER_RUN_NUMBER_PROPERTY) && configurationModel.getUserRunNumber() != null) {
                    configurationModel.setFreezeConditions(freezeConditionsCheckBox.isSelected());
                } else {
                    throw new IllegalArgumentException("Conditions system may only be frozen if there is a valid user run number.");
                }
            } else if (DETECTOR_ALIAS_CHANGED.equals(event.getActionCommand())) {
                configurationModel.setDetectorName(detectorAliasField.getText());
            } else if (CONDITIONS_TAG_CHANGED.equals(event.getActionCommand())) {
                configurationModel.setConditionsTag((String) conditionsTagComboBox.getSelectedItem());
            }
        } finally {
            configurationModel.addPropertyChangeListener(this);
        }
    }

    /**
     * Updates the configuration with changes from the GUI component values. 
     * The changes from the GUI are distinguishable by their component object.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {                            
        configurationModel.removePropertyChangeListener(this);
        try {
            Object source = evt.getSource();            
            if (source == steeringFileField) {
                configurationModel.setSteeringFile(steeringFileField.getText());
            } else if (source == userRunNumberField) {
                // Is run number being reset to null or empty?
                if (userRunNumberField.getText() == null || userRunNumberField.getText().isEmpty()) {
                    // System.out.println("resetting user run number back to null");
                    // Update the model to null user run number and do not freeze the conditions system.
                    configurationModel.setUserRunNumber(null);
                    configurationModel.setFreezeConditions(false);
                } else {
                    try {
                        // Parse the run number. Need to catch errors because it might be an invalid string.
                        int userRunNumber = Integer.parseInt(userRunNumberField.getText());
                        configurationModel.setUserRunNumber(userRunNumber);
                        configurationModel.setFreezeConditions(true);
                    } catch (NumberFormatException e) {
                        System.out.println("bad number format so ignoring user run number " + evt.getNewValue());
                        userRunNumberField.setText((String) evt.getOldValue());
                        // throw new IllegalArgumentException("The value " + evt.getNewValue() + " is not a valid run number.");
                    }
                }
            } else if (source == maxEventsField) {
                configurationModel.setMaxEvents(Long.parseLong(maxEventsField.getText()));
                //System.out.println("setMaxEvents - " + configurationModel.getMaxEvents());
            } else if (evt.getPropertyName().equals(ConfigurationModel.LOG_TO_FILE_PROPERTY)) {
                // This is getting the log to file prop change from the ConfigurationModel to update a read only component.
                Boolean logToFile = (Boolean) evt.getNewValue();
                if (logToFile != null) {
                    logToFileCheckbox.setSelected(logToFile);
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.LOG_FILE_NAME_PROPERTY)) {
                // This is getting the log file name prop change from the ConfigurationModel to update a read only component.
                String logFileName = (String) evt.getNewValue();
                if (logFileName != null && logFileName.length() > 0) {
                    logFileNameField.setText(logFileName);
                } else {
                    logFileNameField.setText("");
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.CONDITIONS_TAG_PROPERTY)) {
                conditionsTagComboBox.setSelectedItem(evt.getNewValue()); 
            }
        } finally {
            configurationModel.addPropertyChangeListener(this);
        }
    }

    /**
     * Update the GUI from changes in the underlying model. 
     * The changes are distinguishable by their property name.
     */
    private class JobSettingsChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            //System.out.println("JobSettingsChangeListener.propertyChange");
            //System.out.println("  src: " + evt.getSource());
            //System.out.println("  propName: " + evt.getPropertyName());
            //System.out.println("  oldValue: " + evt.getOldValue());
            //System.out.println("  newValue: " + evt.getNewValue());
            //System.out.println("  propId: " + evt.getPropagationId());
            if (evt.getSource() instanceof ConfigurationModel) {
                Object value = evt.getNewValue();
                configurationModel.removePropertyChangeListener(this);
                try {
                    if (evt.getPropertyName().equals(DETECTOR_NAME_PROPERTY)) {
                        detectorNameComboBox.setSelectedItem((String) value);
                    } else if (evt.getPropertyName().equals(DETECTOR_ALIAS_PROPERTY)) {
                        detectorAliasField.setText((String) value);
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
                            steeringFileField.setText(((File) value).getAbsolutePath());
                        } else {
                            // A null value here is actually okay and means this field should be reset to have no value.
                            steeringFileField.setText(null);
                        }
                    } else if (evt.getPropertyName().equals(STEERING_RESOURCE_PROPERTY)) {
                        steeringResourcesComboBox.setSelectedItem(value);
                    } else if (evt.getPropertyName().equals(USER_RUN_NUMBER_PROPERTY)) {
                        if (value != null) {
                            userRunNumberField.setText(Integer.toString((int) value));
                        } else {
                            userRunNumberField.setText(null);
                        }
                    } else if (evt.getPropertyName().equals(FREEZE_CONDITIONS_PROPERTY)) {
                        if (value != null) {
                            freezeConditionsCheckBox.setSelected((Boolean) value);
                        }
                    } else if (evt.getPropertyName().equals(MAX_EVENTS_PROPERTY)) {
                        if (value != null) {
                            maxEventsField.setText(value.toString());
                        }
                    }
                } finally {
                    configurationModel.addPropertyChangeListener(this);
                }
            }
        }
    }
    
    @Override
    public void setConfigurationModel(ConfigurationModel model) {
        super.setConfigurationModel(model);
        model.addPropertyChangeListener(new JobSettingsChangeListener());
    }
}