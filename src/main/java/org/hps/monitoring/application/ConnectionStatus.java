package org.hps.monitoring.application;

import java.awt.Color;

/**
 * Status of the connection to the ET server from the monitoring client.
 */
public enum ConnectionStatus {

    DISCONNECTED(Color.RED),
    CONNECTED(Color.GREEN);
    
    Color color;    
    
    ConnectionStatus(Color color) {
        this.color = color;
    }
    
    Color getColor() {
        return color;
    }
}
