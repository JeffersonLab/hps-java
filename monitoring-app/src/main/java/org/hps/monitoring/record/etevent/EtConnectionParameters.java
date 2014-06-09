package org.hps.monitoring.record.etevent;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.enums.Mode;

/**
 * Connection parameters for ET system consumer.
 */
public class EtConnectionParameters {
    
    /**
     * Parameters that are externally settable from within the package.
     */
    String bufferName = "ETBuffer";
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
    
    public void setBufferName(String etName) {
        this.bufferName = etName;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    public void setStationName(String stationName) {
        this.statName = stationName;
    }
    
    public void setChunkSize(int chunk) {
        this.chunk = chunk;
    }
    
    public void setQueueSize(int qSize) {
        this.qSize = qSize;
    }
    
    public void setStationPosition(int position) {
        this.position = position;
    }
    
    public void setStationsParallelPosition(int pposition) {
        this.pposition = pposition;
    }
    
    public void setWaitMode(Mode waitMode) {
        this.waitMode = waitMode;
    }
    
    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }
    
    public void setPreScale(int prescale) {
        this.prescale = prescale;
    }
    
    public Mode getWaitMode() {
        return waitMode;
    }
    
    public int getWaitTime() {
        return waitTime;
    }
    
    public int getChunkSize() {
        return chunk;
    }
    
    public String getBufferName() {
        return bufferName;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public boolean getBlocking() {
        return blocking;
    }
    
    public boolean getVerbose() {
        return verbose;
    }
    
    public String getStationName() {
        return statName;
    }
    
    public int getPrescale() {
        return prescale;
    }
    
    public int getQueueSize() {
        return qSize;
    }
    
    public int getStationPosition() {
        return position;
    }
    
    public int getStationParallelPosition() {
        return pposition;
    }
                   
    /**
     * Class constructor.
     */
    public EtConnectionParameters() {
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
    	buf.append("bufferName: " + bufferName + '\n');
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