package org.hps.online.recon.aida;

import java.io.IOException;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogramFactory;
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
    protected IAnalysisFactory af = aida.analysisFactory();
    protected IDevTree tree = (IDevTree) aida.tree();
    protected IHistogramFactory hf = aida.analysisFactory().createHistogramFactory(tree);
    protected IDataPointSetFactory dpsf = aida.analysisFactory().createDataPointSetFactory(tree);

    private String remoteTreeBind = null;
    private boolean runLocal=false; // set the to true for testing; bypasses remote connections

    public RemoteAidaDriver() {
    }

    public void setRemoteTreeBind(String remoteTreeBind) {
        this.remoteTreeBind = remoteTreeBind;
    }

    public void setRunLocal(boolean run){
        this.runLocal=run;
    }
    
    protected void endOfData() {
        disconnect();
    }

    protected void startOfData() {
        initialize();
    }

    protected void initialize() {
        if(runLocal)return;
        // HACK: Fixes exceptions from missing AIDA converters
        final RmiStoreFactory rsf = new RmiStoreFactory();

        try {
            connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized final void disconnect() {
        if(runLocal)return;
        if (rmiTreeServer != null) {
            ((RmiServerImpl) rmiTreeServer).disconnect();
            rmiTreeServer = null;
        }
        if (treeServer != null) {
            treeServer.close();
            treeServer = null;
        }
    }

    synchronized final void connect() throws IOException {
        if(runLocal)return;
        if (rmiTreeServer != null) {
            LOG.warning("Already connected (RMI tree server is not null)");
            return;
        }
        if (remoteTreeBind == null) {
            throw new IllegalStateException("remoteTreeBind is not set");
        }
        LOG.info("Connecting tree server: " + remoteTreeBind);
        boolean serverDuplex = true;
        treeServer = new RemoteServer(tree, serverDuplex);
        rmiTreeServer = new RmiServerImpl(treeServer, remoteTreeBind);
        LOG.info("Done connecting tree server: " + remoteTreeBind);
    }

    protected abstract void process(EventHeader event);
}