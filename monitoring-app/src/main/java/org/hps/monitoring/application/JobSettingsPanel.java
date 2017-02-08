package org.hps.monitoring.application;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import org.hps.data.detectors.DetectorDataResources;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.SteeringType;
import org.hps.monitoring.application.util.ResourceUtil;
import org.hps.record.enums.ProcessingStage;
import org.hps.steering.SteeringFileCatalog;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * This is the GUI panel for setting job parameters. It is connected to the global configuration settings via a
 * {@link org.hps.monitoring.model.ConfigurationModel} object.
 */
@SuppressWarnings("serial")
final class JobSettingsPanel extends AbstractFieldsPanel {

    /**
     * This filter will accept only files called compact.xml which should be an LCSim detector description file.
     */
    static class CompactFileFilter extends FileFilter {

        /**
         * Class constructor.
         */
        public CompactFileFilter() {
        }

        /**
         * Returns <code>true</code> if the path passes the filter.
         *
         * @return <code>true</code> if the path passes the filter
         */
        @Override
        public boolean accept(final File pathname) {
            return "compact.xml".equals(pathname.getName());
        }

        /**
         * Get the description of the filter.
         *
         * @return the description of the filter
         */
        @Override
        public String getDescription() {
            return "Compact XML files";
        }
    }

    /**
     * Update the GUI from changes in the underlying model. The changes are distinguishable by their property name.
     */
    private class JobSettingsChangeListener implements PropertyChangeListener {

        /**
         * Handle {@link java.beans.PropertyChangeEvent} by updating the GUI from changes to the model.
         *
         * @param evt the {@link java.beans.PropertyChangeEvent} to handle
         */
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if (evt.getSource() instanceof ConfigurationModel) {
                final Object value = evt.getNewValue();
                final String property = evt.getPropertyName();
                JobSettingsPanel.this.getConfigurationModel().removePropertyChangeListener(this);
                try {
                    if (property.equals(ConfigurationModel.DETECTOR_NAME_PROPERTY)) {
                        JobSettingsPanel.this.detectorNameComboBox.setSelectedItem(value);
                    } else if (property.equals(ConfigurationModel.DETECTOR_ALIAS_PROPERTY)) {
                        JobSettingsPanel.this.detectorAliasField.setText((String) value);
                    } else if (property.equals(ConfigurationModel.DISCONNECT_ON_ERROR_PROPERTY)) {
                        JobSettingsPanel.this.disconnectOnErrorCheckBox.setSelected((Boolean) value);
                    } else if (property.equals(ConfigurationModel.DISCONNECT_ON_END_RUN_PROPERTY)) {
                        JobSettingsPanel.this.disconnectOnEndRunCheckBox.setSelected((Boolean) value);
                    } else if (property.equals(ConfigurationModel.EVENT_BUILDER_PROPERTY)) {
                        JobSettingsPanel.this.eventBuilderComboBox.setSelectedItem(value);
                    } else if (property.equals(ConfigurationModel.LOG_FILE_NAME_PROPERTY)) {
                        JobSettingsPanel.this.logFileNameField.setText((String) value);
                    } else if (property.equals(ConfigurationModel.LOG_LEVEL_PROPERTY)) {
                        JobSettingsPanel.this.logLevelComboBox.setSelectedItem(value.toString());
                    } else if (property.equals(ConfigurationModel.LOG_TO_FILE_PROPERTY)) {
                        JobSettingsPanel.this.logToFileCheckbox.setSelected((Boolean) value);
                    } else if (property.equals(ConfigurationModel.STEERING_TYPE_PROPERTY)) {
                        JobSettingsPanel.this.steeringTypeComboBox.setSelectedItem(value);
                    } else if (property.equals(ConfigurationModel.STEERING_FILE_PROPERTY)) {
                        if (value != null) {
                            JobSettingsPanel.this.steeringFileField.setText((String) evt.getNewValue());
                        } else {
                            // A null value here is actually okay and means this field should be reset to have no value.
                            JobSettingsPanel.this.steeringFileField.setText(null);
                        }
                    } else if (property.equals(ConfigurationModel.STEERING_RESOURCE_PROPERTY)) {
                        JobSettingsPanel.this.steeringResourcesComboBox.setSelectedItem(value);
                    } else if (property.equals(ConfigurationModel.USER_RUN_NUMBER_PROPERTY)) {
                        if (value != null) {
                            JobSettingsPanel.this.userRunNumberField.setText(Integer.toString((int) value));
                        } else {
                            JobSettingsPanel.this.userRunNumberField.setText(null);
                        }
                    } else if (property.equals(ConfigurationModel.FREEZE_CONDITIONS_PROPERTY)) {
                        if (value != null) {
                            JobSettingsPanel.this.freezeConditionsCheckBox.setSelected((Boolean) value);
                        }
                    } else if (property.equals(ConfigurationModel.MAX_EVENTS_PROPERTY)) {
                        if (value != null) {
                            JobSettingsPanel.this.maxEventsField.setText(value.toString());
                        }
                    } else if (property.equals(ConfigurationModel.PROCESSING_STAGE_PROPERTY)) {
                        JobSettingsPanel.this.processingStageComboBox.setSelectedItem(evt.getNewValue());
                    } 
                } finally {
                    JobSettingsPanel.this.getConfigurationModel().addPropertyChangeListener(this);
                }
            }
        }
    }

    /**
     * The available LogLevel settings as an array of strings.
     */
    static final String[] LOG_LEVELS = new String[] {Level.ALL.toString(), Level.FINEST.toString(),
            Level.FINER.toString(), Level.FINE.toString(), Level.CONFIG.toString(), Level.INFO.toString(),
            Level.WARNING.toString(), Level.SEVERE.toString(), Level.OFF.toString()};

    /**
     * The package where steering resources must be located.
     */
    private static final String STEERING_PACKAGE = "org/hps/steering/monitoring/";

    /**
     * Combo box for selecting a conditions system tag.
     */
    private final JComboBox<String> conditionsTagComboBox;

    /**
     * Field setting a detector alias to a local file.
     */
    private final JTextField detectorAliasField;

    /**
     * Combo box for selecting a detector model.
     */
    private final JComboBox<String> detectorNameComboBox;

    /**
     * Check box for enabling disconnect on end run.
     */
    private final JCheckBox disconnectOnEndRunCheckBox;

    /**
     * Check box for enabling disconnect on event processing errors.
     */
    private final JCheckBox disconnectOnErrorCheckBox;

    /**
     * Combo box for selecting the event builder class.
     */
    private final JComboBox<String> eventBuilderComboBox;

    /**
     * Check box for freezing conditions system provided there is a user selected run number and detector.
     */
    private final JCheckBox freezeConditionsCheckBox;

    /**
     * Field for specifying an output log file.
     */
    private final JTextField logFileNameField;

    /**
     * Field for setting the log level.
     */
    private final JComboBox<?> logLevelComboBox;

    /**
     * Check box for enabling logging to a file.
     */
    private final JCheckBox logToFileCheckbox;

    /**
     * Field for setting the maximum number of events to process in the session.
     */
    private final JTextField maxEventsField;

    /**
     * Combo box for selecting the processing stage(s) to execute (ET, EVIO or LCIO).
     */
    private final JComboBox<ProcessingStage> processingStageComboBox;

    /**
     * Field for setting an XML steering file.
     */
    private final JTextField steeringFileField;

    /**
     * Combo box for selecting a steering file resource.
     */
    private final JComboBox<?> steeringResourcesComboBox;

    /**
     * Combo box for selecting between current file or resource for XML steering.
     */
    private final JComboBox<SteeringType> steeringTypeComboBox;

    /**
     * Field for setting a user run number for conditions system activation.
     */
    private final JTextField userRunNumberField;

    /**
     * Class constructor.
     *
     * @param model the current {@link org.hps.monitoring.application.model.ConfigurationModel} with global
     *            configuration
     */
    @SuppressWarnings("unchecked")
    JobSettingsPanel(final ConfigurationModel model) {

        super(new Insets(5, 3, 3, 5), true);

        this.setBorder(new EmptyBorder(10, 10, 10, 10));

        this.setLayout(new GridBagLayout());

        // Listen on changes to the configuration which will then be automatically pushed to the GUI.
        model.addPropertyChangeListener(this);

        Set<String> steeringFileResources = SteeringFileCatalog.getSteeringResources();        
        this.steeringResourcesComboBox = this.addComboBoxMultiline("Steering File Resource",
                steeringFileResources.toArray(new String[steeringFileResources.size()]));
        this.steeringResourcesComboBox.setActionCommand(Commands.STEERING_RESOURCE_CHANGED);
        this.steeringResourcesComboBox.addActionListener(this);

        this.steeringFileField = this.addField("Steering File", 50);
        this.steeringFileField.addPropertyChangeListener("value", this);

        final JButton steeringFileButton = this.addButton("Select Steering File");
        steeringFileButton.setActionCommand(Commands.CHOOSE_STEERING_FILE);
        steeringFileButton.addActionListener(this);

        this.steeringTypeComboBox = this.addComboBox("Steering Type", SteeringType.values());
        this.steeringTypeComboBox.setActionCommand(Commands.STEERING_TYPE_CHANGED);
        this.steeringTypeComboBox.addActionListener(this);

        this.processingStageComboBox = new JComboBox<ProcessingStage>(ProcessingStage.values());
        this.addComponent("Processing Stage", this.processingStageComboBox);
        this.processingStageComboBox.setActionCommand(Commands.PROCESSING_STAGE_CHANGED);
        this.processingStageComboBox.addActionListener(this);
        
        Set<String> detectorNames = DetectorDataResources.getDetectorNames();
        this.detectorNameComboBox = this.addComboBox("Detector Name", detectorNames.toArray(new String[detectorNames.size()]));
        this.detectorNameComboBox.setActionCommand(Commands.DETECTOR_NAME_CHANGED);
        this.detectorNameComboBox.addActionListener(this);

        this.detectorAliasField = this.addField("Detector Resources Directory", "", 35, true);
        this.detectorAliasField.setActionCommand(Commands.DETECTOR_ALIAS_CHANGED);
        this.detectorAliasField.addPropertyChangeListener("value", this);
        this.detectorAliasField.addActionListener(this);

        final JButton compactXmlButton = this.addButton("Select Compact Xml File");
        compactXmlButton.setActionCommand(Commands.CHOOSE_COMPACT_FILE);
        compactXmlButton.addActionListener(this);

        this.userRunNumberField = this.addField("User Run Number", "", 10, true);
        this.userRunNumberField.addPropertyChangeListener("value", this);
        this.userRunNumberField.setActionCommand(Commands.USER_RUN_NUMBER_CHANGED);
        this.userRunNumberField.setEnabled(true);
        this.userRunNumberField.setEditable(true);

        this.conditionsTagComboBox = this.addComboBox("Conditions Tag", ResourceUtil.getConditionsTags());
        this.conditionsTagComboBox.addItem("");
        this.conditionsTagComboBox.setSelectedItem("");
        this.conditionsTagComboBox.setActionCommand(Commands.CONDITIONS_TAG_CHANGED);
        this.conditionsTagComboBox.addActionListener(this);
        this.conditionsTagComboBox.setEditable(false);
        this.conditionsTagComboBox.setEnabled(true);

        this.freezeConditionsCheckBox = this.addCheckBox("Freeze detector conditions", false, true);
        this.freezeConditionsCheckBox.addActionListener(this);
        this.freezeConditionsCheckBox.setActionCommand(Commands.FREEZE_CONDITIONS_CHANGED);

        this.maxEventsField = this.addField("Max Events", "-1", 10, false);
        this.maxEventsField.addPropertyChangeListener("value", this);
        this.maxEventsField.setEnabled(true);
        this.maxEventsField.setEditable(true);

        this.eventBuilderComboBox = this.addComboBox("LCSim Event Builder", ResourceUtil.findEventBuilderClassNames());
        this.eventBuilderComboBox.setSize(24, this.eventBuilderComboBox.getPreferredSize().height);
        this.eventBuilderComboBox.setActionCommand(Commands.EVENT_BUILDER_CHANGED);
        this.eventBuilderComboBox.addActionListener(this);

        this.disconnectOnErrorCheckBox = this.addCheckBox("Disconnect on error", false, true);
        this.disconnectOnErrorCheckBox.setActionCommand(Commands.DISCONNECT_ON_ERROR_CHANGED);
        this.disconnectOnErrorCheckBox.addActionListener(this);

        this.disconnectOnEndRunCheckBox = this.addCheckBox("Disconnect on end run", false, true);
        this.disconnectOnEndRunCheckBox.setActionCommand(Commands.DISCONNECT_ON_END_RUN_CHANGED);
        this.disconnectOnEndRunCheckBox.addActionListener(this);

        this.logLevelComboBox = this.addComboBox("Log Level", LOG_LEVELS);
        this.logLevelComboBox.setActionCommand(Commands.LOG_LEVEL_CHANGED);
        this.logLevelComboBox.addActionListener(this);

        this.logToFileCheckbox = this.addCheckBox("Log to File", false, false);
        this.logToFileCheckbox.setEnabled(false);

        this.logFileNameField = this.addField("Log File Name", "", "Full path to log file", 50, false);
        this.logFileNameField.setEditable(false);
    }

    /**
     * Handle {@link java.awt.event.ActionEvent} objects by pushing changes from the GUI into the model.
     *
     * @param event the {@link java.awt.event.ActionEvent} to handle
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        try {
            this.getConfigurationModel().removePropertyChangeListener(this);
            final String command = event.getActionCommand();
            if (event.getActionCommand().equals(Commands.CHOOSE_STEERING_FILE)) {
                this.chooseSteeringFile();
            } else if (event.getActionCommand().equals(Commands.CHOOSE_COMPACT_FILE)) {
                this.chooseCompactFile();
            } else if (Commands.DISCONNECT_ON_ERROR_CHANGED.equals(command)) {
                this.getConfigurationModel().setDisconnectOnError(this.disconnectOnErrorCheckBox.isSelected());
            } else if (Commands.DISCONNECT_ON_END_RUN_CHANGED.equals(command)) {
                this.getConfigurationModel().setDisconnectOnEndRun(this.disconnectOnEndRunCheckBox.isSelected());
            } else if (Commands.STEERING_TYPE_CHANGED.equals(command)) {
                this.getConfigurationModel().setSteeringType(this.steeringTypeComboBox.getSelectedItem().toString());
            } else if (Commands.STEERING_RESOURCE_CHANGED.equals(command)) {
                this.getConfigurationModel().setSteeringResource(
                        this.steeringResourcesComboBox.getSelectedItem().toString());
            } else if (Commands.LOG_LEVEL_CHANGED.equals(command)) {
                this.getConfigurationModel().setLogLevel(Level.parse((String) this.logLevelComboBox.getSelectedItem()));
            } else if (Commands.EVENT_BUILDER_CHANGED.equals(command)) {
                this.getConfigurationModel().setEventBuilderClassName(
                        (String) this.eventBuilderComboBox.getSelectedItem());
            } else if (Commands.DETECTOR_NAME_CHANGED.equals(command)) {
                try {
                    this.getConfigurationModel().setDetectorName((String) this.detectorNameComboBox.getSelectedItem());
                } catch (final Exception exception) {
                    exception.printStackTrace();
                }
            } else if (Commands.FREEZE_CONDITIONS_CHANGED.equals(command)) {
                if (this.getConfigurationModel().hasPropertyKey(ConfigurationModel.USER_RUN_NUMBER_PROPERTY)
                        && this.getConfigurationModel().getUserRunNumber() != null) {
                    this.getConfigurationModel().setFreezeConditions(this.freezeConditionsCheckBox.isSelected());
                } else {
                    throw new IllegalArgumentException(
                            "Conditions system may only be frozen if there is a valid user run number.");
                }
            } else if (Commands.DETECTOR_ALIAS_CHANGED.equals(command)) {
                this.getConfigurationModel().setDetectorName(this.detectorAliasField.getText());
            } else if (Commands.CONDITIONS_TAG_CHANGED.equals(command)) {
                this.getConfigurationModel().setConditionsTag((String) this.conditionsTagComboBox.getSelectedItem());
            } else if (Commands.PROCESSING_STAGE_CHANGED.equals(command)) {
                this.getConfigurationModel().setProcessingStage(
                        this.processingStageComboBox.getSelectedItem().toString());
            }
        } finally {
            this.getConfigurationModel().addPropertyChangeListener(this);
        }
    }

    /**
     * Attaches the ActionListener from the main application to specific GUI components in this class.
     *
     * @param the {@link java.awt.event.ActionListener} to register with the relevant components
     */
    @Override
    public void addActionListener(final ActionListener listener) {
        this.steeringResourcesComboBox.addActionListener(listener);
        this.freezeConditionsCheckBox.addActionListener(listener);
    }

    /**
     * Parse the LCSim XML steering file to see if it looks valid.
     *
     * @param file the input steering file
     * @throws IOException if there is a basic IO problem like reading the file
     * @throws JDOMException if the XML is not valid
     */
    private void checkSteeringFile(final File file) throws IOException, JDOMException {
        final SAXBuilder builder = new SAXBuilder();
        final Document document = builder.build(file);
        final Element rootNode = document.getRootElement();
        if (!"lcsim".equals(rootNode.getName())) {
            throw new IOException("not an LCSim XML file: " + file.getPath());
        }
    }

    /**
     * Choose a compact XML file to override the one embedded in the jar as a resource.
     */
    private void chooseCompactFile() {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose a Compact XML File");
        fc.setCurrentDirectory(new File("."));
        fc.setFileFilter(new CompactFileFilter());
        final int r = fc.showDialog(this, "Select ...");
        if (r == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();
            this.getConfigurationModel().setDetectorAlias(file.getParent());
        }
    }

    /**
     * Choose an LCSim steering file.
     */
    private void chooseSteeringFile() {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose an LCSim Steering File");
        fc.setCurrentDirectory(new File("."));
        final int r = fc.showDialog(this, "Select ...");
        if (r == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();
            try {
                this.checkSteeringFile(file);
                this.getConfigurationModel().setSteeringFile(file.getCanonicalPath());
                this.getConfigurationModel().setSteeringType(SteeringType.FILE.toString());
            } catch (IOException | JDOMException e) {
                throw new RuntimeException("Error parsing the selected steering file.", e);
            }
        }
    }

    /**
     * Updates the configuration with changes from the GUI component values. The changes from the GUI are
     * distinguishable by their component object.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        this.getConfigurationModel().removePropertyChangeListener(this);
        try {
            final Object source = evt.getSource();
            if (source == this.steeringFileField) {
                this.getConfigurationModel().setSteeringFile(this.steeringFileField.getText());
            } else if (source == this.userRunNumberField) {
                // Is run number being reset to null or empty?
                if (this.userRunNumberField.getText() == null || this.userRunNumberField.getText().isEmpty()) {
                    // Update the model to null user run number and do not freeze the conditions system.
                    this.getConfigurationModel().setUserRunNumber(null);
                    this.getConfigurationModel().setFreezeConditions(false);
                } else {
                    try {
                        // Parse the run number. Need to catch errors because it might be an invalid string.
                        final int userRunNumber = Integer.parseInt(this.userRunNumberField.getText());
                        this.getConfigurationModel().setUserRunNumber(userRunNumber);
                        this.getConfigurationModel().setFreezeConditions(true);
                    } catch (final NumberFormatException e) {
                        System.out.println("bad number format so ignoring user run number " + evt.getNewValue());
                        this.userRunNumberField.setText((String) evt.getOldValue());
                        // throw new IllegalArgumentException("The value " + evt.getNewValue() +
                        // " is not a valid run number.");
                    }
                }
            } else if (source == this.maxEventsField) {
                this.getConfigurationModel().setMaxEvents(Long.parseLong(this.maxEventsField.getText()));
                // System.out.println("setMaxEvents - " + configurationModel.getMaxEvents());
            } else if (evt.getPropertyName().equals(ConfigurationModel.LOG_TO_FILE_PROPERTY)) {
                // This is getting the log to file prop change from the ConfigurationModel to update a read only
                // component.
                final Boolean logToFile = (Boolean) evt.getNewValue();
                if (logToFile != null) {
                    this.logToFileCheckbox.setSelected(logToFile);
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.LOG_FILE_NAME_PROPERTY)) {
                // This is getting the log file name prop change from the ConfigurationModel to update a read only
                // component.
                final String logFileName = (String) evt.getNewValue();
                if (logFileName != null && logFileName.length() > 0) {
                    this.logFileNameField.setText(logFileName);
                } else {
                    this.logFileNameField.setText("");
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.CONDITIONS_TAG_PROPERTY)) {
                this.conditionsTagComboBox.setSelectedItem(evt.getNewValue());
            }
        } finally {
            this.getConfigurationModel().addPropertyChangeListener(this);
        }
    }

    /**
     * Sets the {@link org.hps.monitoring.application.model.ConfigurationModel} for the component containing global
     * configuration.
     * <p>
     * The job settings will be updated automatically if the model changes.
     *
     * @param the {@link org.hps.monitoring.application.model.ConfigurationModel} for the component
     */
    @Override
    public void setConfigurationModel(final ConfigurationModel model) {
        super.setConfigurationModel(model);
        model.addPropertyChangeListener(new JobSettingsChangeListener());
    }
}
