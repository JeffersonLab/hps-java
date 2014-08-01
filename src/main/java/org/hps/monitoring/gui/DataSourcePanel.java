package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.Commands.*;
import static org.hps.monitoring.gui.model.ConfigurationModel.*;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.hps.monitoring.enums.DataSourceType;
import org.hps.monitoring.gui.model.ConfigurationModel;

/**
 * A sub-panel of the settings window for selecting a data source, 
 * e.g. an ET server, an LCIO file, or an EVIO file.
 */
class DataSourcePanel extends AbstractFieldsPanel {
           
    static String[] dataSourceTypes = { 
        DataSourceType.ET_SERVER.description(), 
        DataSourceType.EVIO_FILE.description(),
        DataSourceType.LCIO_FILE.description()
    };
    
    JComboBox<?> dataSourceTypeComboBox;
    JTextField dataSourcePathField;    
    
    ConfigurationModel configurationModel;
    
    DataSourcePanel() {
        setLayout(new GridBagLayout());        
        dataSourceTypeComboBox = addComboBox("Data Source", dataSourceTypes);
        dataSourceTypeComboBox.setSelectedIndex(0);
        dataSourceTypeComboBox.setActionCommand(DATA_SOURCE_TYPE_CHANGED);
        dataSourceTypeComboBox.addActionListener(this);
        
        dataSourcePathField = addField("Data Source Path", 40);
        dataSourcePathField.setEditable(false);
        dataSourcePathField.addPropertyChangeListener("value", this);
    }

    /*
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(DATA_SOURCE_COMMAND)) {
            int selectedIndex = dataSourceCombo.getSelectedIndex();
            DataSourceType dataSourceType = DataSourceType.values()[selectedIndex];
            if (dataSourceType.isFile()) { 
                chooseFile();
            } else {
                setFilePath("");
            }
        }
    }
    */
    
    private void chooseFile() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setDialogTitle("Select Data Source");        
        int r = fc.showDialog(this, "Select ...");
        File file = null;
        if (r == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            final String filePath = file.getPath();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    dataSourcePathField.setText(filePath);
                }
            });
        }
    }
    
    /*
    void checkFile() throws IOException {
        DataSourceType dataSourceType = DataSourceType.values()[this.dataSourceTypeComboBox.getSelectedIndex()];
        if (!dataSourceType.isFile())
            return;
        File file = new File(dataSourcePathField.getText());
        if (!file.exists()) {
            throw new IOException("File " + file + " does not exist!");
        }
        if (dataSourceType.equals(DataSourceType.EVIO_FILE)) {
            try {
                new EvioReader(file, false, false);
            } catch (EvioException e) {
                throw new IOException("Error with EVIO file.", e);
            }
        } else if (dataSourceType.equals(DataSourceType.LCIO_FILE)) {
            new LCIOReader(file);
        }
    }
    */

    @Override
    public void setConfigurationModel(ConfigurationModel configurationModel) {
        this.configurationModel = configurationModel;
    }

    @Override
    public ConfigurationModel getConfigurationModel() {
        return configurationModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (DATA_SOURCE_TYPE_CHANGED.equals(e.getActionCommand())) {
            DataSourceType dataSourceType = DataSourceType.values()[dataSourceTypeComboBox.getSelectedIndex()];
            configurationModel.setDataSourceType(dataSourceType);
            if (dataSourceType.isFile()) { 
                chooseFile();
            }
        }
    }
    
    /**
     * Updates the configuration with changes from the GUI component values.
     * The changes from the GUI are distinguishable by their component object.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (dataSourcePathField.equals(evt.getSource())) {
            configurationModel.setDataSourcePath(dataSourcePathField.getText());
        }
    }
    
    /**
     * Update the GUI from changes in the underlying configuration.
     * The changes from the configuration are distinguishable by their 
     * property name.
     */
    public class DataSourceChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Object value = evt.getNewValue();            
            if (DATA_SOURCE_TYPE_PROPERTY.equals(evt.getPropertyName())) {
                dataSourceTypeComboBox.setSelectedItem(value.toString());
            } else if (DATA_SOURCE_PATH_PROPERTY.equals(evt.getPropertyName())) {
                dataSourcePathField.setText((String) value); 
            }
        }
    }              
}