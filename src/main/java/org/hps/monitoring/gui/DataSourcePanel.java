package org.hps.monitoring.gui;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.hps.monitoring.config.Configurable;
import org.hps.monitoring.config.Configuration;
import org.hps.monitoring.enums.DataSourceType;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.lcio.LCIOReader;

/**
 * A sub-panel of the settings window for selecting a data source, 
 * e.g. an ET server, an LCIO file, or an EVIO file.
 */
public class DataSourcePanel extends AbstractFieldsPanel implements ActionListener, Configurable {
           
    static String[] dataSourceTypes = { 
        DataSourceType.ET_SERVER.description(), 
        DataSourceType.EVIO_FILE.description(),
        DataSourceType.LCIO_FILE.description()
    };
    
    JComboBox dataSourceCombo;
    JTextField fileField;
    String DATA_SOURCE_COMMAND = "dataSourceChanged";
    ErrorHandler errorHandler;
    
    Configuration config;
    
    DataSourcePanel() {
        setLayout(new GridBagLayout());        
        dataSourceCombo = addComboBox("Data Source", dataSourceTypes);
        dataSourceCombo.setSelectedIndex(0);
        dataSourceCombo.setActionCommand(DATA_SOURCE_COMMAND);
        dataSourceCombo.addActionListener(this);
        fileField = addField("File Path", 40);
        fileField.setEditable(false);
    }

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
                    fileField.setText(filePath);
                }
            });
        }
    }
    
    void checkFile() throws IOException {
        DataSourceType dataSourceType = DataSourceType.values()[this.dataSourceCombo.getSelectedIndex()];
        if (!dataSourceType.isFile())
            return;
        File file = new File(fileField.getText());
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
    
    
    String getFilePath() {
        if (getDataSourceType().isFile()) {
            return this.fileField.getText();
        } else {
            return null;
        }
    }
    
    DataSourceType getDataSourceType() {
        return DataSourceType.values()[this.dataSourceCombo.getSelectedIndex()];
    }
    
    void setDataSourceType(DataSourceType dataSourceType) {
        this.dataSourceCombo.setSelectedIndex(dataSourceType.ordinal());        
    }
    
    void setFilePath(String filePath) {
        this.fileField.setText(filePath);
    }

    @Override
    public void load(Configuration config) {
        this.dataSourceCombo.removeActionListener(this);
        this.setDataSourceType(DataSourceType.valueOf(config.get("dataSourceType")));
        this.dataSourceCombo.addActionListener(this);
        this.setFilePath(config.get("dataSourcePath"));
    }

    @Override
    public void save(Configuration config) {
        config.set("dataSourceType", getDataSourceType().name());
        config.set("dataSourcePath", getFilePath());
    }

    @Override
    public Configuration getConfiguration() {
        return config;
    }

    @Override
    public void save() {
        save(config);        
    }

    @Override
    public void set(Configuration config) {
        load(config);
        this.config = config;
    }

    @Override
    public void reset() {
        set(config);
    } 
}