package org.hps.monitoring.application.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This model updates listeners when the connection status changes from disconnected to connected or vice versa. It will
 * also notify listeners when the event processing has been paused.
 */
public final class ConnectionStatusModel extends AbstractModel {

    /**
     * The connection status property.
     */
    public static final String CONNECTION_STATUS_PROPERTY = "ConnectionStatus";

    /**
     * The paused property.
     */
    public static final String PAUSED_PROPERTY = "Paused";

    /**
     * The property names of this class.
     */
    private static final String[] PROPERTY_NAMES = new String[] {CONNECTION_STATUS_PROPERTY, PAUSED_PROPERTY};

    /**
     * The current connection status.
     */
    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;

    /**
     * Flag which is <code>true</code> when event processing is in the paused state.
     */
    private boolean paused = false;

    /**
     * Get the current connection status.
     *
     * @return the current connection status
     */
    public ConnectionStatus getConnectionStatus() {
        return this.connectionStatus;
    }

    /**
     * Return <code>true</code> if the event processing is currently paused.
     *
     * @return <code>true</code> if event processing is currently paused
     */
    public boolean getPaused() {
        return this.paused;
    }

    /**
     * Get the property names for this class.
     *
     * @return the property names for this class
     */
    @Override
    public String[] getPropertyNames() {
        return PROPERTY_NAMES;
    }

    /**
     * Return <code>true</code> if the status is <code>CONNECTED</code>.
     *
     * @return <code>true</code> if status is <code>CONNECTED</code>
     */
    public boolean isConnected() {
        return this.connectionStatus == ConnectionStatus.CONNECTED;
    }

    /**
     * Return <code>true</code> if the status is <code>DISCONNECTED</code>.
     *
     * @return <code>true</code> if the status is <code>DISCONNECTED</code>
     */
    public boolean isDisconnected() {
        return this.connectionStatus == ConnectionStatus.DISCONNECTED;
    }

    /**
     * Return <code>true</code> if the status is <code>DISCONNECTING</code>.
     *
     * @return <code>true</code> if the status is <code>DISCONNECTING</code>
     */
    public boolean isDisconnecting() {
        return this.connectionStatus == ConnectionStatus.DISCONNECTING;
    }

    /**
     * Set the connection status.
     *
     * @param connectionStatus the new connection status
     */
    public void setConnectionStatus(final ConnectionStatus connectionStatus) {
        final ConnectionStatus oldValue = connectionStatus;
        this.connectionStatus = connectionStatus;
        for (final PropertyChangeListener listener : this.getPropertyChangeSupport().getPropertyChangeListeners()) {
            listener.propertyChange(new PropertyChangeEvent(this, CONNECTION_STATUS_PROPERTY, oldValue,
                    this.connectionStatus));
        }
    }

    /**
     * Set to <code>true</code> if status is paused.
     *
     * @param paused <code>true</code> if status is paused
     */
    public void setPaused(final boolean paused) {
        final boolean oldValue = this.paused;
        this.paused = paused;
        for (final PropertyChangeListener listener : this.getPropertyChangeSupport().getPropertyChangeListeners()) {
            listener.propertyChange(new PropertyChangeEvent(this, PAUSED_PROPERTY, oldValue, this.paused));
        }
    }
}
