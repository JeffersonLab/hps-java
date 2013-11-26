package org.hps.monitoring;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.enums.Mode;

/**
 * Connection parameters for ET system consumer.
 */
class ConnectionParameters {

    /**
     * Parameters that are externally settable from within the package.
     */
    String etName = "ETBuffer";
    String host = null;
    int port = EtConstants.serverPort;
    boolean blocking = false;
    boolean verbose = false;
    String statName = "MY_STATION";
    int chunk = 1;
    int qSize = 0;
    int position = 1;
    int pposition = 0;
    int flowMode = EtConstants.stationSerial;
    Mode waitMode = Mode.TIMED; 
    int waitTime = 10000000; // wait time in microseconds
    int prescale = 1;
                   
    /**
     * Class constructor.
     */
    public ConnectionParameters() {
        // Set the default host to this machine.
        try {
            InetAddress addr = InetAddress.getLocalHost();
            host = addr.getHostName();
        } catch (UnknownHostException e) {
            throw new ConnectionParametersException("Unable to assign default host.");
        }        
    }
    
    /**
     * This is thrown from the constructor if there a problem setting up the default host.
     */
    public class ConnectionParametersException extends RuntimeException {
        ConnectionParametersException(String msg) {
            super(msg);
        }
    }
               
    /**
     * Convert this class to a readable string (properties format).
     */
    public String toString() {
    	StringBuffer buf = new StringBuffer();
    	buf.append("etName: " + etName + '\n');
    	buf.append("host: " + host + '\n');
    	buf.append("port: " + port + '\n');
    	buf.append("blocking: " + blocking + '\n');
    	buf.append("verbose: " + verbose + '\n');
    	buf.append("statName: " + statName + '\n');
    	buf.append("chunk: " + chunk + '\n');
    	buf.append("qSize: " + qSize + '\n');
    	buf.append("position: " + position + '\n');
    	buf.append("pposition: " + pposition + '\n');
    	buf.append("flowMode: " + flowMode + '\n');
    	buf.append("waitMode: " + waitMode + '\n');
    	buf.append("waitTime: " + waitTime + '\n');
    	buf.append("prescale: " + prescale + '\n');
    	return buf.toString();
    }
}