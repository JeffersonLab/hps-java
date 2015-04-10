package org.hps.monitoring.application.model;

import java.awt.Color;

/**
 * This is the status of the connection to the ET server from the monitoring client, and it includes a color that should
 * be displayed in the GUI for the associated text.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public enum ConnectionStatus {

    CONNECTED(Color.GREEN), DISCONNECTED(Color.RED), DISCONNECTING(Color.YELLOW);

    Color color;

    ConnectionStatus(final Color color) {
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }
}
