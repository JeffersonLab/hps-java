package org.hps.monitoring.application;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.hps.monitoring.application.DataSourceComboBox.DataSourceItem;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.record.enums.DataSourceType;

/**
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class DataSourceComboBox extends JComboBox<DataSourceItem> implements PropertyChangeListener {

    ConnectionStatusModel connectionModel;
    
    DataSourceComboBox(ConnectionStatusModel connectionModel, ActionListener listener) {
        addActionListener(listener);
        setPreferredSize(new Dimension(400, this.getPreferredSize().height));        
        this.connectionModel = connectionModel;
        connectionModel.addPropertyChangeListener(this);
    }
    
    public void addItem(DataSourceItem item) {
        // Do not add duplicates.
        if (((DefaultComboBoxModel<DataSourceItem>)getModel()).getIndexOf(item) == -1) {
            super.addItem(item);
        }
    }
    
    static class DataSourceItem {
        
        String name;
        DataSourceType type;
        
        DataSourceItem(String name, DataSourceType type) {
            this.type = type;
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
       }
    }         
}
