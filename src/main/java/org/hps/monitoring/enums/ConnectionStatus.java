package org.hps.monitoring.enums;

/**
 * Status of the connection to the ET server from the monitoring client.
 */
public enum ConnectionStatus {
    
    DISCONNECTED,
    CONNECTED,
    CONNECTING,
    TIMED_OUT,
    SLEEPING,
    DISCONNECTING,
    ERROR,
    CONNECTION_REQUESTED,
    DISCONNECT_REQUESTED;               
}
