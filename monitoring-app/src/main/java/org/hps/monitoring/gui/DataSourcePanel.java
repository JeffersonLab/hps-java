package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.Commands.CHOOSE_FILE_SOURCE;
import static org.hps.monitoring.gui.Commands.DATA_SOURCE_TYPE_CHANGED;
import static org.hps.monitoring.gui.Commands.PROCESSING_STAGE_CHANGED;
import static org.hps.monitoring.gui.Commands.VALIDATE_DATA_FILE;
import static org.hps.monitoring.gui.model.ConfigurationModel.DATA_SOURCE_PATH_PROPERTY;
import static org.hps.monitoring.gui.model.ConfigurationModel.DATA_SOURCE_TYPE_PROPERTY;
import static org.hps.monitoring.gui.model.ConfigurationModel.PROCESSING_STAGE_PROPERTY;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.hps.monitoring.gui.model.ConfigurationModel;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;

/**
 * A sub-panel of the settings window for selecting a data source, 
 * e.g. an ET server, an LCIO file, or an EVIO file.
 */
class DataSourcePanel extends AbstractFieldsPanel {
           
    static final String[] dataSourceTypes = { 
        DataSourceType.ET_SERVER.description(), 
        DataSourceType.EVIO_FILE.description(),
        DataSourceType.LCIO_FILE.description()
    };
    
    static final String[] processingStages = {
        ProcessingStage.ET.name(),
        ProcessingStage.EVIO.name(),
        ProcessingStage.LCIO.name()
    };
    
    JComboBox<?> dataSourceTypeComboBox;
    JTextField dataSourcePathField;
    JButton fileSourceButton;
    JButton validateDataFileButton;
    JComboBox<?> processingStageComboBox;
    
    ConfigurationModel configurationModel;
    
    DataSourcePanel() {
        setLayout(new GridBagLayout());        
        
        dataSourceTypeComboBox = addComboBox("Data Source", dataSourceTypes);
        dataSourceTypeComboBox.setSelectedIndex(0);
        dataSourceTypeComboBox.setActionCommand(DATA_SOURCE_TYPE_CHANGED);
        dataSourceTypeComboBox.addActionListener(this);
        
        dataSourcePathField = addField("Data Source Path", 40);
        dataSourcePathField.addPropertyChangeListener(this);
        
        fileSourceButton = addButton("Select data file");
        fileSourceButton.setActionCommand(CHOOSE_FILE_SOURCE);
        fileSourceButton.addActionListener(this);
        
        validateDataFileButton = addButton("Validate data file");
        validateDataFileButton.setActionCommand(VALIDATE_DATA_FILE);
                
        processingStageComboBox = addComboBox("Processing Stage", processingStages);
        processingStageComboBox.setSelectedIndex(2);
        processingStageComboBox.setActionCommand(PROCESSING_STAGE_CHANGED);
        processingStageComboBox.addActionListener(this);        
    }
    
    private String getFileExtension(String path) {
        return path.substring(path.lastIndexOf(".") + 1);
    }
       
    private void chooseDataFile() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("LCIO files", "slcio"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("EVIO files", "evio"));        
        fc.setDialogTitle("Select Data File");
        int r = fc.showDialog(this, "Select ...");
        File file = null;
        if (r == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            final String filePath = file.getPath();
            final String extension = getFileExtension(filePath);
            
            // This will cause the GUI to be updated via a PropertyChangeListener.
            configurationModel.setDataSourcePath(filePath);
                                    
            // This will set the combo box in the GUI to the correct state, which will then
            // update the model.
            if (extension.equals("slcio")) { 
                dataSourceTypeComboBox.setSelectedIndex(DataSourceType.LCIO_FILE.ordinal());
            } else if (extension.equals("evio")) {
                dataSourceTypeComboBox.setSelectedIndex(DataSourceType.EVIO_FILE.ordinal());
            } 
        }
    }
        
    @Override
    public void setConfigurationModel(ConfigurationModel configurationModel) {
        this.configurationModel = configurationModel;
        
        // This listener pushes GUI values into the configuration.
        this.configurationModel.addPropertyChangeListener(this);
        
        // This listener updates the GUI from changes in the configuration.
        this.configurationModel.addPropertyChangeListener(new DataSourceChangeListener());
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
            validateDataFileButton.setEnabled(dataSourceType.isFile());
        } else if (PROCESSING_STAGE_CHANGED.equals(e.getActionCommand())) {
            ProcessingStage processingStage = ProcessingStage.values()[processingStageComboBox.getSelectedIndex()];
            configurationModel.setProcessingStage(processingStage);
        } else if (CHOOSE_FILE_SOURCE.equals(e.getActionCommand())) { 
            chooseDataFile();
        } 
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
    }    
    
    /**
     * Update the GUI from changes in the underlying configuration.
     * The changes from the configuration are distinguishable by their 
     * property name.
     */
    public class DataSourceChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("ancestor"))
                return;            
            Object value = evt.getNewValue();
            if (DATA_SOURCE_TYPE_PROPERTY.equals(evt.getPropertyName())) {
                dataSourceTypeComboBox.setSelectedIndex(((DataSourceType)evt.getNewValue()).ordinal());
            } else if (DATA_SOURCE_PATH_PROPERTY.equals(evt.getPropertyName())) {
                dataSourcePathField.setText((String) value); 
            } else if (PROCESSING_STAGE_PROPERTY.equals(evt.getPropertyName())) {
                processingStageComboBox.setSelectedItem(value.toString());
            }
        }
    }
    
    public void addActionListener(ActionListener listener) {
        // Hook the validate button to the main app where that task actually executes.
        validateDataFileButton.addActionListener(listener);
    }
}