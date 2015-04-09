package org.hps.monitoring.application;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.jlab.coda.et.enums.Mode;

/**
 * Connection settings panel.
 */
class ConnectionSettingsPanel extends AbstractFieldsPanel {

    /**
     * Updates the GUI from changes in the ConfigurationModel.
     */
    public class ConnectionSettingsChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            ConnectionSettingsPanel.this.getConfigurationModel().removePropertyChangeListener(this);
            try {
                final Object value = evt.getNewValue();
                if (evt.getPropertyName().equals(ConfigurationModel.ET_NAME_PROPERTY)) {
                    ConnectionSettingsPanel.this.etNameField.setText((String) value);
                } else if (evt.getPropertyName().equals(ConfigurationModel.HOST_PROPERTY)) {
                    ConnectionSettingsPanel.this.hostField.setText((String) value);
                } else if (evt.getPropertyName().equals(ConfigurationModel.PORT_PROPERTY)) {
                    ConnectionSettingsPanel.this.portField.setText(value.toString());
                } else if (evt.getPropertyName().equals(ConfigurationModel.BLOCKING_PROPERTY)) {
                    ConnectionSettingsPanel.this.blockingCheckBox.setSelected((Boolean) value);
                } else if (evt.getPropertyName().equals(ConfigurationModel.VERBOSE_PROPERTY)) {
                    ConnectionSettingsPanel.this.verboseCheckBox.setSelected((Boolean) value);
                } else if (evt.getPropertyName().equals(ConfigurationModel.STATION_NAME_PROPERTY)) {
                    ConnectionSettingsPanel.this.stationNameField.setText((String) value);
                } else if (evt.getPropertyName().equals(ConfigurationModel.CHUNK_SIZE_PROPERTY)) {
                    ConnectionSettingsPanel.this.chunkSizeField.setText(value.toString());
                } else if (evt.getPropertyName().equals(ConfigurationModel.QUEUE_SIZE_PROPERTY)) {
                    ConnectionSettingsPanel.this.queueSizeField.setText(value.toString());
                } else if (evt.getPropertyName().equals(ConfigurationModel.STATION_POSITION_PROPERTY)) {
                    ConnectionSettingsPanel.this.stationPositionField.setText(value.toString());
                } else if (evt.getPropertyName().equals(ConfigurationModel.WAIT_MODE_PROPERTY)) {
                    ConnectionSettingsPanel.this.waitModeComboBox.setSelectedItem(((Mode) value).name());
                } else if (evt.getPropertyName().equals(ConfigurationModel.WAIT_TIME_PROPERTY)) {
                    ConnectionSettingsPanel.this.waitTimeField.setText(value.toString());
                } else if (evt.getPropertyName().equals(ConfigurationModel.PRESCALE_PROPERTY)) {
                    ConnectionSettingsPanel.this.prescaleField.setText(value.toString());
                }
            } finally {
                ConnectionSettingsPanel.this.getConfigurationModel().addPropertyChangeListener(this);
            }
        }
    }

    static final String[] waitModes = { Mode.SLEEP.name(), Mode.TIMED.name(), Mode.ASYNC.name() };
    private final JCheckBox blockingCheckBox;
    private final JTextField chunkSizeField;
    private final JTextField etNameField;
    private final JTextField hostField;
    private final JTextField portField;
    private final JTextField prescaleField;
    private final JTextField queueSizeField;
    private final JTextField stationNameField;
    private final JTextField stationPositionField;
    private final JCheckBox verboseCheckBox;

    private final JComboBox<?> waitModeComboBox;

    private final JTextField waitTimeField;

    /**
     * Class constructor.
     */
    ConnectionSettingsPanel() {

        super(new Insets(5, 5, 5, 5), true);

        this.setLayout(new GridBagLayout());

        this.etNameField = this.addField("ET Name", "", 20);
        this.etNameField.addPropertyChangeListener("value", this);

        this.hostField = this.addField("Host", 20);
        this.hostField.addPropertyChangeListener("value", this);

        this.portField = this.addField("Port", 5);
        this.portField.addPropertyChangeListener("value", this);

        this.blockingCheckBox = this.addCheckBox("Blocking", false, true);
        this.blockingCheckBox.setActionCommand(Commands.BLOCKING_CHANGED);
        this.blockingCheckBox.addActionListener(this);

        this.verboseCheckBox = this.addCheckBox("Verbose", false, true);
        this.verboseCheckBox.setActionCommand(Commands.VERBOSE_CHANGED);
        this.verboseCheckBox.addActionListener(this);

        this.stationNameField = this.addField("Station Name", 10);
        this.stationNameField.addPropertyChangeListener("value", this);

        this.chunkSizeField = this.addField("Chunk Size", 3);
        this.chunkSizeField.addPropertyChangeListener("value", this);

        this.queueSizeField = this.addField("Queue Size", 3);
        this.queueSizeField.addPropertyChangeListener("value", this);

        this.stationPositionField = this.addField("Station Position", 3);
        this.stationPositionField.addPropertyChangeListener("value", this);

        this.waitModeComboBox = this.addComboBox("Wait Mode", waitModes);
        this.waitModeComboBox.setActionCommand(Commands.WAIT_MODE_CHANGED);
        this.waitModeComboBox.addActionListener(this);

        this.waitTimeField = this.addField("Wait Time [microseconds]", 8);
        this.waitTimeField.addPropertyChangeListener(this);

        this.prescaleField = this.addField("Prescale", 8);
        this.prescaleField.addPropertyChangeListener(this);
    }

    /**
     * Used to update the ConfigurationModel from GUI components.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (Commands.WAIT_MODE_CHANGED.equals(e.getActionCommand())) {
            this.getConfigurationModel().setWaitMode(Mode.valueOf((String) this.waitModeComboBox.getSelectedItem()));
        } else if (Commands.BLOCKING_CHANGED.equals(e.getActionCommand())) {
            this.getConfigurationModel().setBlocking(this.blockingCheckBox.isSelected());
        } else if (Commands.VERBOSE_CHANGED.equals(e.getActionCommand())) {
            this.getConfigurationModel().setVerbose(this.verboseCheckBox.isSelected());
        }
    }

    /**
     * Enable or disable the connection panel GUI elements.
     * 
     * @param e Set to true for enabled; false to disable.
     */
    void enableConnectionPanel(final boolean e) {
        this.etNameField.setEnabled(e);
        this.hostField.setEnabled(e);
        this.portField.setEnabled(e);
        this.blockingCheckBox.setEnabled(e);
        this.verboseCheckBox.setEnabled(e);
        this.stationNameField.setEnabled(e);
        this.chunkSizeField.setEnabled(e);
        this.queueSizeField.setEnabled(e);
        this.stationPositionField.setEnabled(e);
        this.waitModeComboBox.setEnabled(e);
        this.waitTimeField.setEnabled(e);
        this.prescaleField.setEnabled(e);
    }

    /**
     * Updates ConfigurationModel from changes in the GUI components.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (!this.accept(evt)) {
            return;
        }
        final Object source = evt.getSource();
        this.getConfigurationModel().removePropertyChangeListener(this);
        try {
            if (source.equals(this.etNameField)) {
                this.getConfigurationModel().setEtName(this.etNameField.getText());
            } else if (source.equals(this.hostField)) {
                this.getConfigurationModel().setHost(this.hostField.getText());
            } else if (source.equals(this.portField)) {
                this.getConfigurationModel().setPort(Integer.parseInt(this.portField.getText()));
            } else if (source.equals(this.stationNameField)) {
                this.getConfigurationModel().setStationName(this.stationNameField.getText());
            } else if (source.equals(this.chunkSizeField)) {
                this.getConfigurationModel().setChunkSize(Integer.parseInt(this.chunkSizeField.getText()));
            } else if (source.equals(this.queueSizeField)) {
                this.getConfigurationModel().setQueueSize(Integer.parseInt(this.queueSizeField.getText()));
            } else if (source.equals(this.stationPositionField)) {
                this.getConfigurationModel().setStationPosition(Integer.parseInt(this.stationPositionField.getText()));
            } else if (source.equals(this.waitTimeField)) {
                this.getConfigurationModel().setWaitTime(Integer.parseInt(this.waitTimeField.getText()));
            } else if (source.equals(this.prescaleField)) {
                this.getConfigurationModel().setPrescale(Integer.parseInt(this.prescaleField.getText()));
            }
        } finally {
            this.getConfigurationModel().addPropertyChangeListener(this);
        }
    }

    @Override
    public void setConfigurationModel(final ConfigurationModel model) {
        super.setConfigurationModel(model);

        // This listener updates the GUI from changes in the configuration.
        this.getConfigurationModel().addPropertyChangeListener(new ConnectionSettingsChangeListener());
    }
}