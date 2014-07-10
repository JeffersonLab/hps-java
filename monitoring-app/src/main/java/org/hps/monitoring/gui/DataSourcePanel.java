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

import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.lcio.LCIOReader;

/**
 * A sub-panel of the settings window for selecting a data source, 
 * e.g. an ET server, an LCIO file, or an EVIO file.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class DataSourcePanel extends FieldsPanel implements ActionListener {
    
    enum DataSourceType {
        ET_SERVER("ET Server"),
        EVIO_FILE("EVIO File"),
        LCIO_FILE("LCIO File");
        
        String name;
        DataSourceType(String name) {
            this.name = name;
        }
        
        public String toString() {
            return name;
        }
        
        static DataSourceType fromString(String dataSourceString) {
            if (dataSourceString == ET_SERVER.toString()) {
                return ET_SERVER;
            } else if (dataSourceString == EVIO_FILE.toString()) {
                return EVIO_FILE;
            } else if (dataSourceString == LCIO_FILE.toString()) {
                return LCIO_FILE;
            } else {
                throw new IllegalArgumentException("Unknown data source type: " + dataSourceString);
            }
        }
        
        static DataSourceType fromIndex(int index) {
            if (index == ET_SERVER.ordinal()) {
                return ET_SERVER;
            } else if (index == EVIO_FILE.ordinal()) {
                return EVIO_FILE;
            } else if (index == LCIO_FILE.ordinal()) {
                return LCIO_FILE;
            } else {
                throw new IllegalArgumentException("Invalid data source index: " + index);
            }
        }
        
        boolean isFile() {
            return this.ordinal() > ET_SERVER.ordinal();
        }
    }
    
    static String[] dataSourceTypes = { 
        DataSourceType.ET_SERVER.toString(), 
        DataSourceType.EVIO_FILE.toString(),
        DataSourceType.LCIO_FILE.toString()};
    
    JComboBox dataSourceCombo;
    JTextField fileField;    
    String DATA_SOURCE_COMMAND = "dataSourceChanged";
    ErrorHandler errorHandler;
    
    DataSourcePanel() {
        setLayout(new GridBagLayout());        
        dataSourceCombo = addComboBox("Data Source", dataSourceTypes);
        dataSourceCombo.setSelectedIndex(0);
        dataSourceCombo.setActionCommand(DATA_SOURCE_COMMAND);
        dataSourceCombo.addActionListener(this);
        fileField = addField("File Path", 40);
        fileField.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(DATA_SOURCE_COMMAND)) {
            int selectedIndex = dataSourceCombo.getSelectedIndex();
            DataSourceType dataSourceType = DataSourceType.fromIndex(selectedIndex);
            if (dataSourceType.isFile()) {
                fileField.setEnabled(true); 
                chooseFile();                 
            } else {
                fileField.setEnabled(false);
            }
        }
    }
    
    private void chooseFile() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setDialogTitle("Choose Data Source");        
        int r = fc.showDialog(this, "SELECT ...");
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
        DataSourceType dataSourceType = DataSourceType.fromIndex(this.dataSourceCombo.getSelectedIndex());
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
        return DataSourceType.fromIndex(this.dataSourceCombo.getSelectedIndex());
    } 
}