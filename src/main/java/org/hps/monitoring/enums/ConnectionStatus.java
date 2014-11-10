package org.hps.monitoring.enums;

/**
 * Status of the connection to the ET server from the monitoring client.
 */
public enum ConnectionStatus {
    DISCONNECTED,
    DISCONNECTING,
    ERROR,
    CONNECTION_REQUESTED,
    CONNECTED,
    PAUSED
}
