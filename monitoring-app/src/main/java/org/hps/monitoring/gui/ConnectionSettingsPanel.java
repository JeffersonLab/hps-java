package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.Commands.*;
import static org.hps.monitoring.gui.model.ConfigurationModel.*;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.hps.monitoring.gui.model.ConfigurationModel;
import org.jlab.coda.et.enums.Mode;

/**
 * Connection settings panel.
 */
class ConnectionSettingsPanel extends AbstractFieldsPanel {

    private JTextField etNameField;
    private JTextField hostField;
    private JTextField portField;
    private JCheckBox blockingCheckBox;
    private JCheckBox verboseCheckBox;
    private JTextField stationNameField;
    private JTextField chunkSizeField;
    private JTextField queueSizeField;
    private JTextField stationPositionField;
    private JComboBox<?> waitModeComboBox;
    private JTextField waitTimeField;
    private JTextField prescaleField;

    static final String[] waitModes = { Mode.SLEEP.name(), Mode.TIMED.name(), Mode.ASYNC.name() };

    ConfigurationModel configurationModel;

    /**
     * Class constructor.
     */
    ConnectionSettingsPanel() {

        super(new Insets(5, 5, 5, 5), true);

        setLayout(new GridBagLayout());

        etNameField = addField("ET Name", "", 20);
        etNameField.addPropertyChangeListener("value", this);

        hostField = addField("Host", 20);
        hostField.addPropertyChangeListener("value", this);

        portField = addField("Port", 5);
        portField.addPropertyChangeListener("value", this);

        blockingCheckBox = addCheckBox("Blocking", false, true);
        blockingCheckBox.setActionCommand(BLOCKING_CHANGED);
        blockingCheckBox.addActionListener(this);

        verboseCheckBox = addCheckBox("Verbose", false, true);
        verboseCheckBox.setActionCommand(VERBOSE_CHANGED);
        verboseCheckBox.addActionListener(this);

        stationNameField = addField("Station Name", 10);
        stationNameField.addPropertyChangeListener("value", this);

        chunkSizeField = addField("Chunk Size", 3);
        chunkSizeField.addPropertyChangeListener("value", this);

        queueSizeField = addField("Queue Size", 3);
        queueSizeField.addPropertyChangeListener("value", this);

        stationPositionField = addField("Station Position", 3);
        stationPositionField.addPropertyChangeListener("value", this);

        waitModeComboBox = addComboBox("Wait Mode", waitModes);
        waitModeComboBox.setActionCommand(WAIT_MODE_CHANGED);
        waitModeComboBox.addActionListener(this);

        waitTimeField = addField("Wait Time [microseconds]", 8);
        waitTimeField.addPropertyChangeListener(this);

        prescaleField = addField("Prescale", 8);
        prescaleField.addPropertyChangeListener(this);
    }

    /**
     * Enable or disable the connection panel GUI elements.
     * @param e Set to true for enabled; false to disable.
     */
    void enableConnectionPanel(boolean e) {
        etNameField.setEnabled(e);
        hostField.setEnabled(e);
        portField.setEnabled(e);
        blockingCheckBox.setEnabled(e);
        verboseCheckBox.setEnabled(e);
        stationNameField.setEnabled(e);
        chunkSizeField.setEnabled(e);
        queueSizeField.setEnabled(e);
        stationPositionField.setEnabled(e);
        waitModeComboBox.setEnabled(e);
        waitTimeField.setEnabled(e);
        prescaleField.setEnabled(e);
    }

    /**
     * Updates the GUI from changes in the ConfigurationModel.
     */
    public class ConfigurationSettingsChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {

            Object value = evt.getNewValue();

            if (evt.getPropertyName().equals(ET_NAME_PROPERTY)) {
                etNameField.setText((String) value);
            } else if (evt.getPropertyName().equals(HOST_PROPERTY)) {
                hostField.setText((String) value);
            } else if (evt.getPropertyName().equals(PORT_PROPERTY)) {
                portField.setText(value.toString());
            } else if (evt.getPropertyName().equals(BLOCKING_PROPERTY)) {
                blockingCheckBox.setSelected((Boolean) value);
            } else if (evt.getPropertyName().equals(VERBOSE_PROPERTY)) {
                verboseCheckBox.setSelected((Boolean) value);
            } else if (evt.getPropertyName().equals(STATION_NAME_PROPERTY)) {
                stationNameField.setText((String) value);
            } else if (evt.getPropertyName().equals(CHUNK_SIZE_PROPERTY)) {
                chunkSizeField.setText(value.toString());
            } else if (evt.getPropertyName().equals(QUEUE_SIZE_PROPERTY)) {
                queueSizeField.setText(value.toString());
            } else if (evt.getPropertyName().equals(STATION_POSITION_PROPERTY)) {
                stationPositionField.setText(value.toString());
            } else if (evt.getPropertyName().equals(WAIT_MODE_PROPERTY)) {
                waitModeComboBox.setSelectedItem(((Mode) value).name());
            } else if (evt.getPropertyName().equals(WAIT_TIME_PROPERTY)) {
                waitTimeField.setText(value.toString());
            } else if (evt.getPropertyName().equals(PRESCALE_PROPERTY)) {
                prescaleField.setText(value.toString());
            }
        }
    }

    /**
     * Updates ConfigurationModel from changes in the GUI components.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getPropertyName().equals("ancestor"))
            return;

        Object source = evt.getSource();

        if (source.equals(etNameField)) {
            configurationModel.setEtName(etNameField.getText());
        } else if (source.equals(hostField)) {
            configurationModel.setHost(hostField.getText());
        } else if (source.equals(portField)) {
            configurationModel.setPort(Integer.parseInt(portField.getText()));
        } else if (source.equals(stationNameField)) {
            configurationModel.setStationName(stationNameField.getText());
        } else if (source.equals(chunkSizeField)) {
            configurationModel.setChunkSize(Integer.parseInt(chunkSizeField.getText()));
        } else if (source.equals(queueSizeField)) {
            configurationModel.setQueueSize(Integer.parseInt(queueSizeField.getText()));
        } else if (source.equals(stationPositionField)) {
            configurationModel.setStationPosition(Integer.parseInt(stationPositionField.getText()));
        } else if (source.equals(waitTimeField)) {
            configurationModel.setWaitTime(Integer.parseInt(waitTimeField.getText()));
        } else if (source.equals(prescaleField)) {
            configurationModel.setPrescale(Integer.parseInt(prescaleField.getText()));
        }
    }

    /**
     * Used to update the ConfigurationModel from GUI components.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (WAIT_MODE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setWaitMode(Mode.valueOf((String) waitModeComboBox.getSelectedItem()));
        } else if (BLOCKING_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setBlocking(blockingCheckBox.isSelected());
        } else if (VERBOSE_CHANGED.equals(e.getActionCommand())) {
            configurationModel.setVerbose(verboseCheckBox.isSelected());
        }
    }

    @Override
    public void setConfigurationModel(ConfigurationModel configurationModel) {
        // Set the ConfigurationModel reference.
        this.configurationModel = configurationModel;

        // This listener pushes GUI values into the configuration.
        this.configurationModel.addPropertyChangeListener(this);

        // This listener updates the GUI from changes in the configuration.
        this.configurationModel.addPropertyChangeListener(new ConfigurationSettingsChangeListener());
    }

    @Override
    public ConfigurationModel getConfigurationModel() {
        return configurationModel;
    }
}