package org.hps.monitoring;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.jlab.coda.et.enums.Mode;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: ConnectionPanel.java,v 1.24 2013/11/05 17:15:04 jeremy Exp $
 */
class ConnectionPanel extends FieldsPanel {

    private JTextField etNameField;
    private JTextField hostField;
    private JTextField portField;
    private JCheckBox blockingCheckBox;
    private JCheckBox verboseCheckBox;
    private JTextField statNameField;
    private JTextField chunkField;
    private JTextField qSizeField;
    private JTextField positionField;
    private JTextField ppositionField;
    private JComboBox waitComboBox;
    private JTextField waitTimeField;
    private JTextField prescaleField;
    private ConnectionParameters connectionParameters;
    static final String[] waitModes = {
        Mode.SLEEP.toString(),
        Mode.TIMED.toString(),
        Mode.ASYNC.toString()
    };

    /**
     * Class constructor.
     */
    ConnectionPanel() {

        //super(new Insets(1, 1, 1, 1), true);
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
        ppositionField = addField("Station Parallel Position", 3);
        waitComboBox = addComboBox("Wait Mode", waitModes);
        waitTimeField = addField("Wait Time [microseconds]", 8);
        prescaleField = addField("Prescale", 8);

        // Set default connection parameters which are pushed to GUI.
        setConnectionParameters(new ConnectionParameters());
    }

    /**
     * Get the connection parameters.
     * @return The connection parameters.
     */
    ConnectionParameters getConnectionParameters() {
        connectionParameters = new ConnectionParameters();
        connectionParameters.etName = etNameField.getText();
        connectionParameters.host = hostField.getText();
        connectionParameters.port = Integer.parseInt(portField.getText());
        connectionParameters.blocking = blockingCheckBox.isSelected();
        connectionParameters.verbose = verboseCheckBox.isSelected();
        connectionParameters.statName = statNameField.getText();
        connectionParameters.chunk = Integer.parseInt(chunkField.getText());
        connectionParameters.qSize = Integer.parseInt(qSizeField.getText());
        connectionParameters.position = Integer.parseInt(positionField.getText());
        connectionParameters.pposition = Integer.parseInt(ppositionField.getText());
        connectionParameters.waitMode = getWaitMode();
        connectionParameters.waitTime = Integer.parseInt(waitTimeField.getText());
        connectionParameters.prescale = Integer.parseInt(prescaleField.getText());
        return connectionParameters;
    }

    /**
     * Get the current wait mode from the GUI selection.
     * @return The wait mode.
     */
    private Mode getWaitMode() {
        Mode mode = null;
        String sel = (String) waitComboBox.getSelectedItem();
        if (Mode.TIMED.toString().equalsIgnoreCase(sel)) {
            mode = Mode.TIMED;
        } else if (Mode.ASYNC.toString().equalsIgnoreCase(sel)) {
            mode = Mode.ASYNC;
        } else if (Mode.SLEEP.toString().equalsIgnoreCase(sel)) {
            mode = Mode.SLEEP;
        }
        return mode;
    }

    /**
     * Set the wait mode and push to the GUI.
     * @param waitMode The wait mode.
     */
    private void setWaitMode(Mode waitMode) {
        if (waitMode == Mode.SLEEP) {
            waitComboBox.setSelectedIndex(0);
        } else if (waitMode == Mode.TIMED) {
            waitComboBox.setSelectedIndex(1);
        } else if (waitMode == Mode.ASYNC) {
            waitComboBox.setSelectedIndex(2);
        }
    }

    /**
     * Set the connection parameters and push into the GUI.
     * @param cn The connection parameters.
     */
    private void setConnectionParameters(ConnectionParameters cn) {
        etNameField.setText(cn.etName);
        hostField.setText(cn.host);
        portField.setText(Integer.toString(cn.port));
        blockingCheckBox.setSelected(cn.blocking);
        verboseCheckBox.setSelected(cn.verbose);
        statNameField.setText(cn.statName);
        chunkField.setText(Integer.toString(cn.chunk));
        qSizeField.setText(Integer.toString(cn.qSize));
        positionField.setText(Integer.toString(cn.position));
        ppositionField.setText(Integer.toString(cn.pposition));
        setWaitMode(cn.waitMode);
        waitTimeField.setText(Integer.toString(cn.waitTime));
        prescaleField.setText(Integer.toString(cn.prescale));
        this.connectionParameters = cn;
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
        ppositionField.setEnabled(e);
        waitComboBox.setEnabled(e);
        waitTimeField.setEnabled(e);
        prescaleField.setEnabled(e);
    }

    /**
     * Save connection parameters to selected output file.
     */
    void save() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showSaveDialog(ConnectionPanel.this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            writePropertiesFile(file);
        }
    }

    /**
     * Load connection parameters from a selected file.
     */
    void load() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showOpenDialog(ConnectionPanel.this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            loadPropertiesFile(file);
        }
    }

    /**
     * Reset the connection parameters.
     */
    void reset() {
        setConnectionParameters(new ConnectionParameters());
    }

    /**
     * Write connection parameters to a file.
     * @param file The output properties file.
     */
    private void writePropertiesFile(File file) {
        Properties prop = new Properties();
        prop.setProperty("etName", etNameField.getText());
        prop.setProperty("host", hostField.getText());
        prop.setProperty("port", portField.getText());
        prop.setProperty("blocking", Boolean.toString(blockingCheckBox.isSelected()));
        prop.setProperty("verbose", Boolean.toString(verboseCheckBox.isSelected()));
        prop.setProperty("statName", statNameField.getText());
        prop.setProperty("chunk", chunkField.getText());
        prop.setProperty("qSize", qSizeField.getText());
        prop.setProperty("position", positionField.getText());
        prop.setProperty("pposition", ppositionField.getText());
        prop.setProperty("waitMode", (String) waitComboBox.getSelectedItem());
        prop.setProperty("waitTime", waitTimeField.getText());
        prop.setProperty("prescale", prescaleField.getText());
        try {
            prop.store(new FileOutputStream(file), null);
        } catch (Exception e) {
            showErrorDialog(e.getLocalizedMessage());
        }
    }

    /**
     * Show an error dialog.
     * @param mesg The dialog message.
     */
    private void showErrorDialog(String mesg) {
        JOptionPane.showMessageDialog(this, mesg);
    }

    /**
     * Set the wait mode.
     * @param waitMode The wait mode.
     */
    private void setWaitMode(String waitMode) {
        if (Mode.SLEEP.toString().equalsIgnoreCase(waitMode)) {
            waitComboBox.setSelectedIndex(0);
        } else if (Mode.TIMED.toString().equalsIgnoreCase(waitMode)) {
            waitComboBox.setSelectedIndex(1);
        } else if (Mode.ASYNC.toString().equalsIgnoreCase(waitMode)) {
            waitComboBox.setSelectedIndex(2);
        }
    }

    /**
     * Load connection parameters from properties file.
     * @param file The properties file.
     */
    void loadPropertiesFile(File file) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(file));
            etNameField.setText(prop.getProperty("etName"));
            hostField.setText(prop.getProperty("host"));
            portField.setText(prop.getProperty("port"));
            blockingCheckBox.setSelected(Boolean.parseBoolean(prop.getProperty("blocking")));
            verboseCheckBox.setSelected(Boolean.parseBoolean(prop.getProperty("verbose")));
            statNameField.setText(prop.getProperty("statName"));
            chunkField.setText(prop.getProperty("chunk"));
            qSizeField.setText(prop.getProperty("qSize"));
            positionField.setText(prop.getProperty("position"));
            ppositionField.setText(prop.getProperty("pposition"));
            setWaitMode(prop.getProperty("waitMode"));
            waitTimeField.setText(prop.getProperty("waitTime"));
            prescaleField.setText(prop.getProperty("prescale"));
        } catch (FileNotFoundException e) {
            showErrorDialog(e.getLocalizedMessage());
        } catch (IOException e) {
            showErrorDialog(e.getLocalizedMessage());
        }
        this.connectionParameters = getConnectionParameters();
    }
}