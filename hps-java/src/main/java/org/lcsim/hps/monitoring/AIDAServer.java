package org.lcsim.hps.monitoring;

import hep.aida.dev.IDevTree;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

import org.lcsim.util.aida.AIDA;

/**
 * Wrapper class for remote AIDA server.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: AIDAServer.java,v 1.2 2013/10/25 22:59:16 jeremy Exp $
 */
class AIDAServer {
    
    RmiServerImpl server;
    String name;
   
    /**
     * Class constructor. 
     * @param name The name of the AIDA server.
     */
    AIDAServer(String name) {
        this.name = name;
    }

    /**
     * Start the remote AIDA server.
     * @return True if server started successfully; false if an error occurred during initialization.
     */
    boolean start() {
        //RmiStoreFactory store = new RmiStoreFactory(); // Is this needed?
        final boolean serverDuplex = false;
        RemoteServer treeServer = new RemoteServer((IDevTree) AIDA.defaultInstance().tree(), serverDuplex);
        try {
            server = new RmiServerImpl(treeServer, "/" + name);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /** 
     * Close the server down by disconnecting it.
     */
    void close() {
        server.disconnect();
    }
}