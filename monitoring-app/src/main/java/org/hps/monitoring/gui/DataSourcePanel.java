package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.Commands.DATA_SOURCE_TYPE_CHANGED;
import static org.hps.monitoring.gui.Commands.PROCESSING_STAGE_CHANGED;
import static org.hps.monitoring.gui.model.ConfigurationModel.DATA_SOURCE_PATH_PROPERTY;
import static org.hps.monitoring.gui.model.ConfigurationModel.DATA_SOURCE_TYPE_PROPERTY;
import static org.hps.monitoring.gui.model.ConfigurationModel.PROCESSING_STAGE_PROPERTY;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

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
        
        processingStageComboBox = addComboBox("Processing Stage", processingStages);
        processingStageComboBox.setSelectedIndex(2);
        processingStageComboBox.setActionCommand(PROCESSING_STAGE_CHANGED);
        processingStageComboBox.addActionListener(this);        
    }
       
    private void chooseFile() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setDialogTitle("Select Data Source");        
        int r = fc.showDialog(this, "Select ...");
        File file = null;
        if (r == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            final String filePath = file.getPath();
            
            // This will cause the GUI to be updated via a PropertyChangeListener.
            configurationModel.setDataSourcePath(filePath);                      
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
            if (dataSourceType.isFile()) { 
                chooseFile();
            }
        } else if (PROCESSING_STAGE_CHANGED.equals(e.getActionCommand())) {
            ProcessingStage processingStage = ProcessingStage.values()[processingStageComboBox.getSelectedIndex()];
            configurationModel.setProcessingStage(processingStage);
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
            
            // FIXME: Anyway to make sure this is not needed?
            if (evt.getPropertyName().equals("ancestor"))
                return;
            
            //System.out.println("DataSourceChangeListener.propertyChange");
            //System.out.println("  source: " + evt.getSource());
            //System.out.println("  name: " + evt.getPropertyName());
            //System.out.println("  value: " + evt.getNewValue());
            Object value = evt.getNewValue();            
            if (DATA_SOURCE_TYPE_PROPERTY.equals(evt.getPropertyName())) {
                dataSourceTypeComboBox.setSelectedItem(value.toString());
            } else if (DATA_SOURCE_PATH_PROPERTY.equals(evt.getPropertyName())) {
                dataSourcePathField.setText((String) value); 
            } else if (PROCESSING_STAGE_PROPERTY.equals(evt.getPropertyName())) {
                processingStageComboBox.setSelectedItem(value.toString());
            }
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
}