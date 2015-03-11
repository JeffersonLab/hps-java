package org.hps.monitoring.application.util;

import hep.aida.dev.IDevTree;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.RmiRemoteUtils;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

import java.net.InetAddress;

import org.lcsim.util.aida.AIDA;

/**
 * AIDA RMI server wrapper.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class AIDAServer {
    
    RmiServerImpl server;
    String name;
    boolean connected = false;
   
    /**
     * Class constructor. 
     * @param name The name of the AIDA server.
     */
    public AIDAServer(String name) {
        this.name = name;
    }
    
    /**
     * Set the name that will be used for the path part of the URL.
     * @param name The server's name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Start the remote AIDA server.
     * @return True if server started successfully; false if an error occurred during initialization.
     */
    public boolean start() {
        RemoteServer treeServer = new RemoteServer((IDevTree) AIDA.defaultInstance().tree());
        try {
            server = new RmiServerImpl(treeServer, "/" + name);
            connected = true;
        }
        catch (Exception e) {
            e.printStackTrace();
            connected = false;
        }
        return connected;
    }
    
    /** 
     * Close the server down by disconnecting it.
     */
    public void disconnect() {
        server.disconnect();
        connected = false;
    }
    
    /**
     * True if connected.
     * @return True if connected to server.
     */
    public boolean connected() {
        return connected;
    }    
    
    public String getName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName() + server.getBindName() + ":" + RmiRemoteUtils.port;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}