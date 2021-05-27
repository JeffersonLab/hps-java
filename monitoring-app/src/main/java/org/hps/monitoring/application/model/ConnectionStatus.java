package org.hps.monitoring.application.model;

import java.awt.Color;

/**
 * This is the status of the connection to the ET server from the monitoring client, and it includes a color that should
 * be displayed in the GUI for the associated text.
 */
public enum ConnectionStatus {

    /**
     * This is the state when the session is connected to event processing.
     */
    CONNECTED(Color.GREEN),
    /**
     * This is the disconnected state when event processing is not occurring.
     */
    DISCONNECTED(Color.RED),
    /**
     * This is the state when the session is being torn down.
     */
    DISCONNECTING(Color.YELLOW);

    /**
     * The color that should be displayed in the GUI component for this state.
     */
    private Color color;

    /**
     * Class constructor.
     *
     * @param color the color to display for this state
     */
    private ConnectionStatus(final Color color) {
        this.color = color;
    }

    /**
     * Get the color to display for this state.
     *
     * @return the color to display for this state
     */
    public Color getColor() {
        return this.color;
    }
}
