package org.hps.monitoring.application.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.hps.monitoring.application.ConnectionStatus;

/**
 * This model updates listeners when the connection status changes from disconnected
 * to connected or vice versa.  It will also notify when the event processing is 
 * paused.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConnectionStatusModel extends AbstractModel {
    
    public static final String CONNECTION_STATUS_PROPERTY = "ConnectionStatus";
    public static final String PAUSED_PROPERTY = "Paused";
    
    static final String[] propertyNames = new String[] { 
        CONNECTION_STATUS_PROPERTY, 
        PAUSED_PROPERTY 
    };

    ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    boolean paused = false;
    
    public String[] getPropertyNames() {
        return propertyNames;
    }
    
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    
    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        ConnectionStatus oldValue = connectionStatus;
        this.connectionStatus = connectionStatus;
        for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
            listener.propertyChange(new PropertyChangeEvent(this, CONNECTION_STATUS_PROPERTY, oldValue, this.connectionStatus));
        }
    }        
    
    public boolean getPaused() {
        return paused;
    }
    
    public void setPaused(boolean paused) {
        System.out.println("ConnectionStatusModel.setPause - " + paused);
        boolean oldValue = this.paused;
        this.paused = paused;
        for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
            listener.propertyChange(new PropertyChangeEvent(this, PAUSED_PROPERTY, oldValue, this.paused));
        }
    }
}
