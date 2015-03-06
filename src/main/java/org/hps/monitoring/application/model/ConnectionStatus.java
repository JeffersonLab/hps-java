package org.hps.monitoring.application.model;

import java.awt.Color;

/**
 * This is the status of the connection to the ET server from the monitoring client,
 * and it includes a color that should be displayed in the GUI for the associated
 * text.
 */
public enum ConnectionStatus {

    DISCONNECTED(Color.RED),
    CONNECTED(Color.GREEN);
    
    Color color;    
    
    ConnectionStatus(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }
}
