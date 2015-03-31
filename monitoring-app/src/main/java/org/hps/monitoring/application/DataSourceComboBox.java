package org.hps.monitoring.application;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.hps.monitoring.application.DataSourceComboBox.DataSourceItem;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.record.enums.DataSourceType;

/**
 * This is a combo box that shows the current data source such as an LCIO file, EVIO file or ET ring.
 * It can also be used to select a new data source for the new session. 
 * <p>
 * The way this works is kind of odd because it is not directly connected to an event loop, so it must 
 * catch changes to the configuration and update its items accordingly.
 * <p>
 * A single ET item is kept in the list and updated as changes are made to the global configuration.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class DataSourceComboBox extends JComboBox<DataSourceItem> implements PropertyChangeListener, ActionListener{

    ConnectionStatusModel connectionModel;
    ConfigurationModel configurationModel;

    static class DataSourceItem {

        private String name;
        private String path;
        private DataSourceType type;

        DataSourceItem(String path, String name, DataSourceType type) {
            if (path == null) {
                throw new IllegalArgumentException("path is null");
            }
            if (name == null) {
                throw new IllegalArgumentException("name is null");
            }
            if (type == null) {
                throw new IllegalArgumentException("type is null");
            }
            this.type = type;
            this.name = name;
            this.path = path;
        }

        public String toString() {
            return name;
        }
        
        public String getPath() {
            return path;
        }
        
        public String getName() {
            return name;
        }

        public boolean equals(Object object) {
            if (!(object instanceof DataSourceItem)) {
                return false;
            }
            DataSourceItem otherItem = (DataSourceItem) object;
            if (this.name == otherItem.name && this.path == otherItem.path && this.type == otherItem.type) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    @SuppressWarnings({ "rawtypes", "serial", "unchecked" })
    DataSourceComboBox(ConfigurationModel configurationModel, ConnectionStatusModel connectionModel) {
        addActionListener(this);
        setActionCommand(Commands.DATA_SOURCE_CHANGED);
        setPreferredSize(new Dimension(510, this.getPreferredSize().height));
        setEditable(false);
        this.configurationModel = configurationModel;
        connectionModel.addPropertyChangeListener(this);                
        configurationModel.addPropertyChangeListener(this);
        
        ListCellRenderer renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                if (value instanceof DataSourceItem) {
                    setToolTipText(((DataSourceItem)value).getPath());
                } else {
                    setToolTipText(null);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

        };
        this.setRenderer(renderer);
    }
            
    boolean containsItem(DataSourceItem item) {
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
                if (configurationModel.hasValidProperty(ConfigurationModel.DATA_SOURCE_TYPE_PROPERTY)) {                    
                    String path = configurationModel.getDataSourcePath();
                    DataSourceType type = DataSourceType.getDataSourceType(path);
                    if (type.isFile()) {
                        DataSourceItem item = findItem(path);
                        if (item == null) {
                            item = addDataSourceItem(path, type);
                        }
                        if (configurationModel.getDataSourceType().isFile()) {
                            setSelectedItem(item);
                        }
                    }
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.DATA_SOURCE_TYPE_PROPERTY)) {
                if (configurationModel.getDataSourceType() == DataSourceType.ET_SERVER) {
                    DataSourceItem item = findEtItem();
                    if (item == null) {
                        item = new DataSourceItem(configurationModel.getEtPath(), configurationModel.getEtPath(), DataSourceType.ET_SERVER);
                    }
                    setSelectedItem(item);
                } else {
                    if (configurationModel.hasValidProperty(ConfigurationModel.DATA_SOURCE_PATH_PROPERTY)) {
                        DataSourceItem item = findItem(configurationModel.getDataSourcePath());
                        if (item == null) {
                            item = addDataSourceItem(configurationModel.getDataSourcePath(), configurationModel.getDataSourceType());
                        }
                        setSelectedItem(item);
                    }
                }
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
    
    @Override
    public void setSelectedItem(Object object) {
        super.setSelectedItem(object);
        this.setToolTipText(((DataSourceItem)object).getPath());
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getActionCommand().equals(Commands.DATA_SOURCE_CHANGED)) {
            try {
                // Update the model with data source settings.
                configurationModel.removePropertyChangeListener(this);
                DataSourceItem item = (DataSourceItem) getSelectedItem();
                if (item != null) {
                    configurationModel.setDataSourceType(item.type);
                    if (item.type != DataSourceType.ET_SERVER) {
                        configurationModel.setDataSourcePath(item.getPath());
                    }
                }
            } finally {
                configurationModel.addPropertyChangeListener(this);
            }
        } 
    }
       
    public void addItem(DataSourceItem item) {
        if (containsItem(item)) {
            return;
        }
        if (findItem(item.getPath()) == null) {
            super.addItem(item);
        }
    }

    DataSourceItem findItem(String path) {
        for (int i = 0; i < this.getItemCount(); i++) {
            DataSourceItem item = this.getItemAt(i);
            if (item.getPath().equals(path)) {
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

    DataSourceItem addDataSourceItem(String path, DataSourceType type) {
        DataSourceItem newItem = new DataSourceItem(path, new File(path).getName(), type);
        addItem(newItem);
        return newItem;
    }
    
    void updateEtItem() {
        DataSourceItem item = findEtItem();
        if (item == null) {
            item = new DataSourceItem(configurationModel.getEtPath(), configurationModel.getEtPath(), DataSourceType.ET_SERVER);
            addItem(item);
        } else {
            item.name = configurationModel.getEtPath();
        }
    }
}