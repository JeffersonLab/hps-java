package org.hps.monitoring.application.model;

import org.hps.monitoring.application.ConnectionStatus;

/**
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class ConnectionStatusModel extends AbstractModel {
    
    public static final String CONNECTION_STATUS_PROPERTY = "ConnectionStatus";
    public static final String PAUSED_PROPERTY = "ConnectionStatus";
    
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
        if (connectionStatus != this.connectionStatus) {
            ConnectionStatus oldValue = connectionStatus;
            this.connectionStatus = connectionStatus;
            firePropertyChange(CONNECTION_STATUS_PROPERTY, oldValue, this.connectionStatus);
        }
    }        
    
    public boolean getPaused() {
        return paused;
    }
    
    public void setPaused(boolean paused) {
        if (paused != this.paused) {
            boolean oldValue = this.paused;
            this.paused = paused;
            firePropertyChange(PAUSED_PROPERTY, oldValue, paused);
        }
    }
}
