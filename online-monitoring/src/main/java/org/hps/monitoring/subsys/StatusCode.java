package org.hps.monitoring.subsys;

import java.awt.Color;

/**
 * Code that represents a sub-system status.
 */
public enum StatusCode {
    
    OKAY(Color.GREEN),
    UNKNOWN(Color.GRAY),
    CLEARED(Color.LIGHT_GRAY),
    OFFLINE(Color.ORANGE),
    INFO(Color.WHITE),
    WARNING(Color.YELLOW),
    ERROR(Color.RED),
    ALARM(Color.RED),
    HALT(Color.RED);
    
    Color color;
    
    StatusCode(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }    
}
