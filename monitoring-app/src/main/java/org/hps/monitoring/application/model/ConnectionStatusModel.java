package org.hps.monitoring.application.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This model updates listeners when the connection status changes from disconnected to connected or vice versa. It will
 * also notify when the event processing is paused.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ConnectionStatusModel extends AbstractModel {

    public static final String CONNECTION_STATUS_PROPERTY = "ConnectionStatus";
    public static final String PAUSED_PROPERTY = "Paused";

    static final String[] propertyNames = new String[] { CONNECTION_STATUS_PROPERTY, PAUSED_PROPERTY };

    ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    boolean paused = false;

    public ConnectionStatus getConnectionStatus() {
        return this.connectionStatus;
    }

    public boolean getPaused() {
        return this.paused;
    }

    @Override
    public String[] getPropertyNames() {
        return propertyNames;
    }

    public boolean isConnected() {
        return this.connectionStatus == ConnectionStatus.CONNECTED;
    }

    public boolean isDisconnected() {
        return this.connectionStatus == ConnectionStatus.DISCONNECTED;
    }

    public boolean isDisconnecting() {
        return this.connectionStatus == ConnectionStatus.DISCONNECTING;
    }

    public void setConnectionStatus(final ConnectionStatus connectionStatus) {
        final ConnectionStatus oldValue = connectionStatus;
        this.connectionStatus = connectionStatus;
        for (final PropertyChangeListener listener : this.propertyChangeSupport.getPropertyChangeListeners()) {
            listener.propertyChange(new PropertyChangeEvent(this, CONNECTION_STATUS_PROPERTY, oldValue,
                    this.connectionStatus));
        }
    }

    public void setPaused(final boolean paused) {
        final boolean oldValue = this.paused;
        this.paused = paused;
        for (final PropertyChangeListener listener : this.propertyChangeSupport.getPropertyChangeListeners()) {
            listener.propertyChange(new PropertyChangeEvent(this, PAUSED_PROPERTY, oldValue, this.paused));
        }
    }
}
