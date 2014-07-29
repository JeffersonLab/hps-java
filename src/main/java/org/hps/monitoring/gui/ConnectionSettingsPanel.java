package org.hps.monitoring.gui;

import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.hps.monitoring.config.Configurable;
import org.hps.monitoring.config.Configuration;
import org.jlab.coda.et.enums.Mode;

/**
 * Connection settings panel.
 */
class ConnectionSettingsPanel extends AbstractFieldsPanel implements Configurable {

    private JTextField etNameField;
    private JTextField hostField;
    private JTextField portField;
    private JCheckBox blockingCheckBox;
    private JCheckBox verboseCheckBox;
    private JTextField statNameField;
    private JTextField chunkField;
    private JTextField qSizeField;
    private JTextField positionField;
    private JComboBox<?> waitComboBox;
    private JTextField waitTimeField;
    private JTextField prescaleField;
    
    static final String[] waitModes = {
        Mode.SLEEP.name(),
        Mode.TIMED.name(),
        Mode.ASYNC.name()
    };
    
    Configuration config;

    /**
     * Class constructor.
     */
    ConnectionSettingsPanel() {

        super(new Insets(5, 5, 5, 5), true);

        setLayout(new GridBagLayout());

        // Define fields.
        etNameField = addField("ET Name", "", 20);
        hostField = addField("Host", 20);
        portField = addField("Port", 5);
        blockingCheckBox = addCheckBox("Blocking", false, true);
        verboseCheckBox = addCheckBox("Verbose", false, true);
        statNameField = addField("Station Name", 10);
        chunkField = addField("Chunk Size", 3);
        qSizeField = addField("Queue Size", 3);
        positionField = addField("Station Position", 3);
        waitComboBox = addComboBox("Wait Mode", waitModes);
        waitTimeField = addField("Wait Time [microseconds]", 8);
        prescaleField = addField("Prescale", 8);
    }
     
    /**
     * Get the current wait mode from the GUI selection.
     * @return The wait mode.
     */
    private Mode getWaitMode() {
        return Mode.valueOf((String) waitComboBox.getSelectedItem());
    }

    /**
     * Set the wait mode and push to the GUI.
     * @param waitMode The wait mode.
     */
    private void setWaitMode(Mode waitMode) {
        waitComboBox.setSelectedIndex(waitMode.ordinal());
    }
    
    private void setWaitMode(String s) {
        Mode mode = null;
        if (s.equals(Mode.SLEEP.name()))
            mode = Mode.SLEEP;
        else if (s.equals(Mode.TIMED.name()))
            mode = Mode.TIMED;
        else if (s.equals(Mode.ASYNC.name()))
            mode = Mode.ASYNC;
        else
            throw new IllegalArgumentException("Invalid mode string.");
        setWaitMode(mode);
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
        statNameField.setEnabled(e);
        chunkField.setEnabled(e);
        qSizeField.setEnabled(e);
        positionField.setEnabled(e);
        waitComboBox.setEnabled(e);
        waitTimeField.setEnabled(e);
        prescaleField.setEnabled(e);
    }
        
    @Override
    public void load(Configuration config) {        
        etNameField.setText(config.get("bufferName"));
        hostField.setText(config.get("host"));
        portField.setText(config.get("port"));
        blockingCheckBox.setSelected(config.getBoolean("blocking"));                       
        verboseCheckBox.setSelected(config.getBoolean("verbose"));            
        statNameField.setText(config.get("statName"));
        chunkField.setText(config.get("chunk"));                                   
        qSizeField.setText(config.get("qSize"));                      
        positionField.setText(config.get("position"));
        setWaitMode(config.get("waitMode"));
        waitTimeField.setText(config.get("waitTime"));
        prescaleField.setText(config.get("prescale"));
    }

    @Override
    public void save(Configuration config) {
        config.set("bufferName", etNameField.getText());
        config.set("host", hostField.getText());
        config.set("port", portField.getText());
        config.set("blocking", blockingCheckBox.isSelected());
        config.set("verbose", verboseCheckBox.isSelected());
        config.set("statName", statNameField.getText());
        config.set("chunk", chunkField.getText());
        config.set("qSize", qSizeField.getText());
        config.set("position", positionField.getText());
        config.set("waitMode", (String) waitComboBox.getSelectedItem());
        config.set("waitTime", waitTimeField.getText());
        config.set("prescale", prescaleField.getText());        
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
    
    @Override
    public void save() {
        save(config);
    }

    @Override
    public Configuration getConfiguration() {
        return config;
    }
}