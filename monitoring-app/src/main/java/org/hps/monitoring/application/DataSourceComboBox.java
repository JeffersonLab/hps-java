package org.hps.monitoring.application;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.hps.monitoring.application.DataSourceComboBox.DataSourceItem;
import org.hps.monitoring.application.model.ConfigurationListener;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.record.enums.DataSourceType;

/**
 * <p>
 * This is a combo box that shows and can be used to select the current data source
 * such as an LCIO file, EVIO file or ET ring.
 * <p>
 * The way this works is kind of funky because it is not directly connected to an
 * event loop, so it must catch changes to the configuration and update its 
 * items accordingly.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class DataSourceComboBox extends JComboBox<DataSourceItem> implements PropertyChangeListener, ActionListener, ConfigurationListener {

    ConnectionStatusModel connectionModel;
    ConfigurationModel configurationModel;

    static class DataSourceItem {

        String name;
        DataSourceType type;

        DataSourceItem(String name, DataSourceType type) {
            this.type = type;
            this.name = name;
        }

        public String toString() {
            return name;
        }

        public boolean equals(Object object) {
            if (!(object instanceof DataSourceItem)) {
                return false;
            }
            DataSourceItem otherItem = (DataSourceItem) object;
            if (this.name == otherItem.name && this.type == otherItem.type)
                return true;
            return false;
        }
    }
    
    DataSourceComboBox(ConfigurationModel configurationModel, ConnectionStatusModel connectionModel) {
        addActionListener(this);
        setActionCommand(Commands.DATA_SOURCE_CHANGED);
        setPreferredSize(new Dimension(400, this.getPreferredSize().height));
        setEditable(false);
        this.configurationModel = configurationModel;
        connectionModel.addPropertyChangeListener(this);                
        configurationModel.addConfigurationListener(this);
    }
            
    void setSelectedItem() {
        DataSourceItem item = findItem(configurationModel.getDataSourcePath(), getDataSourceType(configurationModel.getDataSourcePath()));
        if (item != null) {
            setSelectedItem(item);
        }
    }

    boolean contains(DataSourceItem item) {
        return ((DefaultComboBoxModel<DataSourceItem>) getModel()).getIndexOf(item) != -1;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        configurationModel.removePropertyChangeListener(this);
        try {
            if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
                ConnectionStatus status = (ConnectionStatus) evt.getNewValue();
                if (status.equals(ConnectionStatus.DISCONNECTED)) {
                    setEnabled(true);
                } else {
                    setEnabled(false);
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.DATA_SOURCE_PATH_PROPERTY)) {
                String path = configurationModel.getDataSourcePath();
                DataSourceType type = getDataSourceType(path);
                addDataSourceItem(path, type);
                setSelectedItem();
            } else if (evt.getPropertyName().equals(ConfigurationModel.DATA_SOURCE_TYPE_PROPERTY)) {
                setSelectedItem();
            } else if (evt.getPropertyName().equals(ConfigurationModel.HOST_PROPERTY)) {
                updateEtItem();
            } else if (evt.getPropertyName().equals(ConfigurationModel.ET_NAME_PROPERTY)) {
                updateEtItem();
            } else if (evt.getPropertyName().equals(ConfigurationModel.PORT_PROPERTY)) {
                updateEtItem();
            }
        } finally {
            configurationModel.addPropertyChangeListener(this);
        }
    }
    
    static DataSourceType getDataSourceType(String path) {
        if (path.endsWith(".slcio")) {
            return DataSourceType.LCIO_FILE;
        } else if (new File(path).getName().contains(".evio")) {
            return DataSourceType.EVIO_FILE;
        } else {
            return DataSourceType.ET_SERVER;
        }
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getActionCommand().equals(Commands.DATA_SOURCE_CHANGED)) {
            try {
                // Update the model with data source settings.
                configurationModel.removePropertyChangeListener(this);
                DataSourceItem item = (DataSourceItem) getSelectedItem();
                configurationModel.setDataSourceType(item.type);
                if (item.type != DataSourceType.ET_SERVER) {
                    configurationModel.setDataSourcePath(item.name);
                }
            } finally {
                configurationModel.addPropertyChangeListener(this);
            }
        } 
    }
    
    public void configurationChanged(ConfigurationModel configurationModel) {
               
        // Clear the data source list.
        removeAllItems();
        
        // Add the default ET item.
        this.removeActionListener(this);
        try {
            addItem(new DataSourceItem(configurationModel.getEtPath(), DataSourceType.ET_SERVER));
                
            // Add a file source if one has been provided.
            if (configurationModel.getDataSourcePath() != null) {
                // Add an item for this data source.
                DataSourceItem newItem = new DataSourceItem(configurationModel.getDataSourcePath(), configurationModel.getDataSourceType());
                //System.out.println("adding new item " + newItem.name + " " + newItem.type);
                addItem(newItem);            
                if (configurationModel.getDataSourceType().isFile()) {
                    //System.out.println("setting selected");
                    setSelectedItem(newItem);
                }
            }
        } finally {
            this.addActionListener(this);    
        }
        
        // Don't add as property change listener until after configuration has been initialized.
        configurationModel.removePropertyChangeListener(this);
        configurationModel.addPropertyChangeListener(this);
    }

    public void addItem(DataSourceItem item) {
        // Do not add invalid looking items.
        if (item.name == null || item.name.length() == 0) { 
            return;
        }
        // Do not add duplicates.
        if (!contains(item)) {
            super.addItem(item);
        }
    }

    DataSourceItem findItem(String path, DataSourceType type) {
        for (int i = 0; i < this.getItemCount(); i++) {
            DataSourceItem item = this.getItemAt(i);
            if (item.type == type && item.name == path) {
                return item;
            }            
        }
        return null;
    }
    
    DataSourceItem findEtItem() {
        for (int i = 0; i < this.getItemCount(); i++) {
            DataSourceItem item = this.getItemAt(i);
            if (item.type == DataSourceType.ET_SERVER) {
                return item;
            }
        }
        return null;
    }

    void addDataSourceItem(String path, DataSourceType type) {
        DataSourceItem newItem = new DataSourceItem(path, type);
        if (!contains(newItem)) {
            addItem(newItem);
        }
    }
    
    void updateEtItem() {
        DataSourceItem item = findEtItem();
        item.name = configurationModel.getEtPath();
    }
}