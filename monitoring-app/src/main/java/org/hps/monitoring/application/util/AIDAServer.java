package org.hps.monitoring.application.util;

import hep.aida.dev.IDevTree;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.RmiRemoteUtils;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

import java.net.InetAddress;

import org.lcsim.util.aida.AIDA;

/**
 * AIDA RMI server wrapper.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class AIDAServer {

    boolean connected = false;
    String name;
    RmiServerImpl server;

    /**
     * Class constructor.
     *
     * @param name The name of the AIDA server.
     */
    public AIDAServer(final String name) {
        this.name = name;
    }

    /**
     * True if connected.
     *
     * @return True if connected to server.
     */
    public boolean connected() {
        return this.connected;
    }

    /**
     * Close the server down by disconnecting it.
     */
    public void disconnect() {
        this.server.disconnect();
        this.connected = false;
    }

    public String getName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName() + this.server.getBindName() + ":"
                    + RmiRemoteUtils.port;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Set the name that will be used for the path part of the URL.
     *
     * @param name The server's name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Start the remote AIDA server.
     *
     * @return True if server started successfully; false if an error occurred during initialization.
     */
    public boolean start() {
        final RemoteServer treeServer = new RemoteServer((IDevTree) AIDA.defaultInstance().tree());
        try {
            this.server = new RmiServerImpl(treeServer, "/" + this.name);
            this.connected = true;
        } catch (final Exception e) {
            e.printStackTrace();
            this.connected = false;
        }
        return this.connected;
    }
}