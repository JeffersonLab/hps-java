package org.hps.online.recon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
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
 * The property <code>remoteAidaPort</code> can be used to set the
 * network port, or it can be set using {{@link #setPort(int)}
 * from within lcsim xml. If they are both set, then the XML
 * will override the system setting.
 */
public abstract class RemoteAidaDriver extends Driver {

    private static final String PORT_PROPERTY = "remoteAidaPort";

    static final Logger LOG = Logger.getLogger(RemoteAidaDriver.class.getPackage().getName());

    static {
        System.setProperty("hep.aida.IAnalysisFactory", BatchAnalysisFactory.class.getName());
        System.setProperty("java.awt.headless", "true");
    }

    protected RemoteServer treeServer;
    protected RmiServer rmiTreeServer;

    protected AIDA aida = AIDA.defaultInstance();
    protected IDevTree tree = (IDevTree) aida.tree();

    static private final Integer DEFAULT_PORT = 2001;
    protected Integer port = DEFAULT_PORT;

    static private final String DEFAULT_NAME = "ReconStation";
    protected String serverName = DEFAULT_NAME;

    protected String hostName = null;

    private String remoteTreeFileName = null;

    public RemoteAidaDriver() {
        if (System.getProperties().containsKey(PORT_PROPERTY)) {
            this.setPort(Integer.parseInt(System.getProperties().getProperty(PORT_PROPERTY)));
            LOG.config("Set remote AIDA port from system property: " + port);
        }
    }

    public void setPort(int port) {
        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException("Bad port number: " + port);
        }
        this.port = port;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setRemoteTreeFileName(String remoteTreeFileName) {
        this.remoteTreeFileName = remoteTreeFileName;
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

    private final void disconnect() {
        ((RmiServerImpl) rmiTreeServer).disconnect();
        treeServer.close();
    }

    private final void connect() throws IOException {
        if (hostName == null) {
            hostName = InetAddress.getLocalHost().getHostName();
        }
        String treeBindName = "//"+hostName+":"+port+"/"+serverName;
        LOG.info("Connecting tree server: " + treeBindName);
        try {
            boolean serverDuplex = true;
            treeServer = new RemoteServer(tree, serverDuplex);
            rmiTreeServer = new RmiServerImpl(treeServer, treeBindName);
            if (this.remoteTreeFileName != null) {
                LOG.info("Writing remote tree info to: " + this.remoteTreeFileName);
                File remoteTreeFile = new File(this.remoteTreeFileName);
                FileOutputStream fos = new FileOutputStream(remoteTreeFile);
                fos.write(treeBindName.getBytes());
                fos.close();
                LOG.info("Done writing remote tree info");
            }
            LOG.info("Connection successful!");
        } catch (Exception e) {
            LOG.severe("Connection failed!");
            throw new RuntimeException(e);
        }
    }
}
