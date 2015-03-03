package org.hps.monitoring.application;

/**
 * Status of the connection to the ET server from the monitoring client.
 */
// FIXME: Just change to disconnected and connected.
public enum ConnectionStatus {
    DISCONNECTED,
    DISCONNECTING,
    ERROR,
    CONNECTION_REQUESTED,
    CONNECTED,
    PAUSED
}
