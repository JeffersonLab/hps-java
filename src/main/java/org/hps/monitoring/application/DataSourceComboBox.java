package org.hps.monitoring.application;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.hps.monitoring.application.DataSourceComboBox.DataSourceItem;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.record.enums.DataSourceType;

/**
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class DataSourceComboBox extends JComboBox<DataSourceItem> implements PropertyChangeListener, ActionListener {

    ConnectionStatusModel connectionModel;
    ConfigurationModel configurationModel;
    
    DataSourceComboBox(
            ConfigurationModel configurationModel, 
            ConnectionStatusModel connectionModel, 
            ActionListener listener) {
        addActionListener(listener);
        setActionCommand(Commands.DATA_SOURCE_CHANGED);
        setPreferredSize(new Dimension(400, this.getPreferredSize().height));
        setEditable(false);
        connectionModel.addPropertyChangeListener(this);
        configurationModel.addPropertyChangeListener(this);
        this.configurationModel = configurationModel;
    }
    
    public void addItem(DataSourceItem item) {
        // Do not add duplicates.
        if (!contains(item)) {
            super.addItem(item);
        }
    }
    
    boolean contains(DataSourceItem item) {
        return ((DefaultComboBoxModel<DataSourceItem>)getModel()).getIndexOf(item) != -1;
    }
    
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
            if (this.name == otherItem.name && this.type == otherItem.type) return true;
            return false;
        }
    }

    /**
     * 
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
       if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
           ConnectionStatus status = (ConnectionStatus) evt.getNewValue();
           if (status.equals(ConnectionStatus.DISCONNECTED)) {
               setEnabled(true);
           } else {
               setEnabled(false);
           }
       } else if (evt.getPropertyName().equals(ConfigurationModel.DATA_SOURCE_PATH_PROPERTY)) {     
           System.out.println("data source path property changed");
           addDataSourceItem();
       } 
    }         
    
    public void actionPerformed(ActionEvent evt) {
        if (evt.getActionCommand().equals(Commands.DATA_SOURCE_CHANGED)) {
            try {
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
    
    void addDataSourceItem() {
                        
        DataSourceType type = configurationModel.getDataSourceType();
        String path = configurationModel.getDataSourcePath();
                
        // Remove an existing ET item in case the settings have changed.
        if (hasEtItem()) {            
            removeEtItem();
        }
        
        // Always make sure there is an ET server available as a data source.
        addItem(new DataSourceItem(getEtName(), DataSourceType.ET_SERVER));
        
        // Add a data source for a file.
        if (!type.equals(DataSourceType.ET_SERVER)) {
            DataSourceItem newItem = new DataSourceItem(path, type);
            if (!contains(newItem)) {
                addItem(newItem);
                setSelectedItem(newItem);
            }
        }       
    }
    
    void removeEtItem() {
        for (int i=0; i<this.getItemCount(); i++) {
            DataSourceItem item = this.getItemAt(i);
            if (item.type == DataSourceType.ET_SERVER) {
                this.removeItem(item);
                break;
            }
        }
    }
    
    boolean hasEtItem() {
        for (int i=0; i<this.getItemCount(); i++) {
            DataSourceItem item = this.getItemAt(i);
            if (item.type == DataSourceType.ET_SERVER) {
                return true;
            }
        }
        return false;
    }
    
    String getEtName() {
        return configurationModel.getEtName() + "@" + configurationModel.getHost() + ":" + configurationModel.getPort();
    }
}