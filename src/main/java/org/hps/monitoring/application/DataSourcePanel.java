package org.hps.monitoring.application;

import static org.hps.monitoring.application.Commands.OPEN_FILE;
import static org.hps.monitoring.application.Commands.DATA_SOURCE_TYPE_CHANGED;
import static org.hps.monitoring.application.Commands.PROCESSING_STAGE_CHANGED;
import static org.hps.monitoring.application.Commands.VALIDATE_DATA_FILE;
import static org.hps.monitoring.application.model.ConfigurationModel.DATA_SOURCE_PATH_PROPERTY;
import static org.hps.monitoring.application.model.ConfigurationModel.DATA_SOURCE_TYPE_PROPERTY;
import static org.hps.monitoring.application.model.ConfigurationModel.PROCESSING_STAGE_PROPERTY;

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
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;

/**
 * A sub-panel of the settings window for selecting a data source, e.g. an ET server, an LCIO file,
 * or an EVIO file.
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

    DataSourcePanel() {
        
        setLayout(new GridBagLayout());
        
        dataSourceTypeComboBox = addComboBox("Data Source", dataSourceTypes);
        dataSourceTypeComboBox.setSelectedIndex(0);
        dataSourceTypeComboBox.setActionCommand(DATA_SOURCE_TYPE_CHANGED);
        dataSourceTypeComboBox.addActionListener(this);

        dataSourcePathField = addField("Data Source Path", 40);
        dataSourcePathField.addPropertyChangeListener(this);

        fileSourceButton = addButton("Select data file");
        fileSourceButton.setActionCommand(OPEN_FILE);
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

    /**
     * This is a simple file filter that will accept files with
     * ".evio" anywhere in their name. 
     */
    static class EvioFileFilter extends FileFilter {

        public EvioFileFilter() {            
        }
        
        @Override
        public boolean accept(File pathname) {
            if (pathname.getName().contains(".evio") || pathname.isDirectory()) {
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public String getDescription() {
            return "EVIO files";
        }        
    }
    
    private void chooseDataFile() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("LCIO files", "slcio"));
        fc.addChoosableFileFilter(new EvioFileFilter());
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
    
    /**
     * 
     * @param e
     */
    @Override   
    public void actionPerformed(ActionEvent e) {
        try {
            configurationModel.removePropertyChangeListener(this);
            if (DATA_SOURCE_TYPE_CHANGED.equals(e.getActionCommand())) {
                DataSourceType dataSourceType = DataSourceType.values()[dataSourceTypeComboBox.getSelectedIndex()];
                configurationModel.setDataSourceType(dataSourceType);
                validateDataFileButton.setEnabled(dataSourceType.isFile());
            } else if (PROCESSING_STAGE_CHANGED.equals(e.getActionCommand())) {
                ProcessingStage processingStage = ProcessingStage.values()[processingStageComboBox.getSelectedIndex()];
                configurationModel.setProcessingStage(processingStage);
            } else if (OPEN_FILE.equals(e.getActionCommand())) {
                chooseDataFile();
            }
        } finally {
            configurationModel.addPropertyChangeListener(this);
        }
    }

    /**
     * Update the GUI from changes in the underlying configuration. The changes from the
     * configuration are distinguishable by their property name.
     */
    public class DataSourceChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("ancestor"))
                return;
            Object value = evt.getNewValue();
            configurationModel.removePropertyChangeListener(this);
            try {
                if (DATA_SOURCE_TYPE_PROPERTY.equals(evt.getPropertyName())) {
                    dataSourceTypeComboBox.setSelectedIndex(((DataSourceType) evt.getNewValue()).ordinal());
                } else if (DATA_SOURCE_PATH_PROPERTY.equals(evt.getPropertyName())) {
                    dataSourcePathField.setText((String) value);
                } else if (PROCESSING_STAGE_PROPERTY.equals(evt.getPropertyName())) {
                    processingStageComboBox.setSelectedItem(value.toString());
                }
            } finally {
                configurationModel.addPropertyChangeListener(this);
            }
        }
    }

    public void addActionListener(ActionListener listener) {
        // Hook the validate button to the main app where that task actually executes.
        validateDataFileButton.addActionListener(listener);
    }
    
    @Override
    public void setConfigurationModel(ConfigurationModel model) {
        super.setConfigurationModel(model);
        
        model.addPropertyChangeListener(new DataSourceChangeListener());
    }
}