package org.hps.online.recon.aida;

import java.io.IOException;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.dev.IDevTree;
import hep.aida.ref.BatchAnalysisFactory;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

/**
 * Abstract driver for providing remote AIDA functionality
 *
 * The property <code>remoteTreeBind</code> can be used to set the
 * RMI binding name.
 */
public abstract class RemoteAidaDriver extends Driver {

    static final Logger LOG = Logger.getLogger(RemoteAidaDriver.class.getPackage().getName());

    static {
        System.setProperty("hep.aida.IAnalysisFactory", BatchAnalysisFactory.class.getName());
        System.setProperty("java.awt.headless", "true");
    }

    protected RemoteServer treeServer;
    protected RmiServer rmiTreeServer;

    protected AIDA aida = AIDA.defaultInstance();
    protected IDevTree tree = (IDevTree) aida.tree();

    private String remoteTreeBind = null;

    public RemoteAidaDriver() {
    }

    public void setRemoteTreeBind(String remoteTreeBind) {
        this.remoteTreeBind = remoteTreeBind;
    }

    protected void endOfData() {
        disconnect();
    }

    protected void startOfData() {

        // HACK: Fixes exceptions from missing AIDA converters
        final RmiStoreFactory rsf = new RmiStoreFactory();

        try {
            connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void process(EventHeader event);

    synchronized final void disconnect() {
        ((RmiServerImpl) rmiTreeServer).disconnect();
        treeServer.close();
        rmiTreeServer = null;
        treeServer = null;
    }

    synchronized  final void connect() throws IOException {
        if (remoteTreeBind == null) {
            throw new IllegalStateException("remoteTreeBind is not set");
        }
        LOG.info("Connecting tree server: " + remoteTreeBind);
        //try {
        boolean serverDuplex = true;
        treeServer = new RemoteServer(tree, serverDuplex);
        rmiTreeServer = new RmiServerImpl(treeServer, remoteTreeBind);
        LOG.info("Done connecting tree server: " + remoteTreeBind);
        //} catch (Exception e) {
        //    LOG.log(Level.SEVERE, "Failed to setup remote tree server", e);
        //    throw new RuntimeException(e);
        //}
    }
}