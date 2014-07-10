package org.hps.monitoring.gui;

/**
 * Status of the connection to the ET server from the monitoring client.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: ConnectionStatus.java,v 1.3 2013/11/05 17:15:04 jeremy Exp $
 */
// TODO: Make this class an enum.
final class ConnectionStatus
{
    /**
     * The status codes.
     */
    static final int DISCONNECTED = 0;
    static final int CONNECTED = 1;
    static final int CONNECTING = 2;
    static final int TIMED_OUT = 3;
    static final int SLEEPING = 4;
    static final int DISCONNECTING = 5;
    static final int ERROR = 6;
    static final int CONNECTION_REQUESTED = 7;
    static final int DISCONNECT_REQUESTED = 8;
    
    /**
     * The string descriptions for connection statuses.
     */
    private static final String[] statuses = { 
        "DISCONNECTED", 
        "CONNECTED", 
        "CONNECTING", 
        "TIMED OUT", 
        "SLEEPING", 
        "DISCONNECTING", 
        "ERROR",
        "CONNECTION REQUESTED",
        "DISCONNECT REQUESTED" };
    
    /**
     * Total number of statuses.
     */
    static final int NUMBER_STATUSES = statuses.length;
    
    /**
     * Convert status setting to string.
     * @param status The status setting.
     * @return The status string.
     */
    static String toString(int status) {
        return statuses[status].toUpperCase();
    }
}
