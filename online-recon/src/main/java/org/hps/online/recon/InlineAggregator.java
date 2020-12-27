package org.hps.online.recon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import hep.aida.IAnalysisFactory;
import hep.aida.IBaseHistogram;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IManagedObject;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.dev.IDevTree;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

/**
 * Version of the plot {@link Aggregator} for running inside the {@link Server}
 * on a scheduled thread executor
 */
public class InlineAggregator implements Runnable {

    private static Logger LOG = Logger.getLogger(InlineAggregator.class.getPackage().getName());

    private static final String REMOTES_DIR = "/remotes";
    private static final String AGG_DIR = "/combined";

    /** Network port; set with "port" property */
    private int port = 3001;

    /** Name of the remote server; set with "name" property */
    private String serverName = "HPSRecon";

    /** Name of the host to bind; set with "host" property */
    private String hostName = null;

    /** Interval between aggregation; set with "interval" property */
    private Long updateInterval = 5000L;

    private Set<String> remotes = new HashSet<String>();

    /**
     * AIDA objects
     */
    private IAnalysisFactory af = IAnalysisFactory.create();
    private ITreeFactory tf = af.createTreeFactory();
    private IDevTree serverTree = (IDevTree) tf.create();

    /**
     * RMI server objects
     */
    private RemoteServer treeServer;
    private RmiServer rmiTreeServer;

    /**
     * Flag indicating that plots can be updated
     */
    private volatile boolean updatable = false;

    public InlineAggregator() {
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setUpdateInterval(Long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public Long getUpdateInterval() {
        return this.updateInterval;
    }

    synchronized void connect() throws IOException {
        if (this.hostName == null) {
            this.hostName = InetAddress.getLocalHost().getHostName();
        }
        String treeBindName = "//"+this.hostName+":"+port+"/"+serverName;
        LOG.config("Creating aggregator server tree: " + treeBindName);
        boolean serverDuplex = true;
        treeServer = new RemoteServer(serverTree, serverDuplex);
        rmiTreeServer = new RmiServerImpl(treeServer, treeBindName);

        serverTree.mkdir(AGG_DIR);
        serverTree.mkdir(REMOTES_DIR);

        // Flag as good for updates
        updatable = true;

        LOG.config("Done creating aggregator server tree!");
    }

    private static String toAggregateName(String remoteName) {
        String[] sp = remoteName.split("/");
        return remoteName.replace(REMOTES_DIR + "/" + sp[2], AGG_DIR);
    }

    static String toMountName(String treeBindName) {
        return REMOTES_DIR + "/" + treeBindName.replace("//", "").replace("/", "_").replace(":", "_").replace(".", "_");
    }

    private String[] listObjectNames(String path, boolean recursive, String type) {
        String[] names = null;
        if (type != null) {
            List<String> filtNames = new ArrayList<String>();
            String[] objectNames = serverTree.listObjectNames(path, recursive);
            String[] objectTypes = serverTree.listObjectTypes(path, recursive);
            for (int i = 0; i < objectNames.length; i++) {
                if (objectTypes[i].equals(type)) {
                    filtNames.add(objectNames[i]);
                }
            }
            names = filtNames.toArray(new String[] {});
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serverTree.ls(path, recursive, baos);
            try {
                names = baos.toString("UTF-8").split("\\r?\\n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return names;
    }

    private synchronized void clearTree() {
        LOG.fine("Clearing tree...");
        String[] objectNames = listObjectNames(AGG_DIR, true, null);
        for (String name : objectNames) {
            try {
                IManagedObject obj = serverTree.find(name);
                if (obj instanceof IHistogram) {
                    ((IBaseHistogram) obj).reset();
                }
            } catch (IllegalArgumentException e) {
            }
        }
        LOG.fine("Done clearing tree");
    }

    private synchronized void update() {
        LOG.fine("Aggregator is updating plots...");
        try {
            String[] dirs = this.listObjectNames(REMOTES_DIR, false, null);
            for (String dir : dirs) {
                String[] remoteObjects = serverTree.listObjectNames(dir, true);
                for (String remoteName : remoteObjects) {
                    String aggName = toAggregateName(remoteName);
                    IManagedObject obj = null;
                    try {
                        obj = serverTree.find(aggName);
                        if (obj instanceof IHistogram) {
                            add(remoteName, obj);
                        }
                    } catch (IllegalArgumentException e) {
                    }
                    if (obj == null) {
                        try {
                            obj = serverTree.find(remoteName);
                            LOG.info("Copying: " + remoteName + " -> " + aggName);
                            serverTree.cp(remoteName, aggName, false);
                        } catch (IllegalArgumentException e1) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error updating plots", e);
        }
        LOG.fine("Aggregator is done updating plots");
    }

    private void add(String srcName, IManagedObject obj) {
        if (obj instanceof IBaseHistogram) {
            LOG.fine("Adding: " + srcName);
            IBaseHistogram srcBase = (IBaseHistogram) serverTree.find(srcName);
            LOG.fine("src entries: " + srcBase.entries());
            IBaseHistogram tgtBase = (IBaseHistogram) obj;
            LOG.fine("target entries before: " + tgtBase.entries());
            if (obj instanceof IHistogram1D) {
                ((IHistogram1D) obj).add((IHistogram1D) serverTree.find(srcName));
            } else if (obj instanceof IHistogram2D) {
                ((IHistogram2D) obj).add((IHistogram2D) serverTree.find(srcName));
            }
            LOG.fine("target entries after: " + tgtBase.entries());
        }
    }

    synchronized void disconnect() {

        // Flag as invalid for updates
        updatable = false;

        LOG.info("Disconnecting aggregator ...");
        if (rmiTreeServer != null) {
            try {
                ((RmiServerImpl) rmiTreeServer).disconnect();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error disconnecting RMI tree server", e);
            }
        }
        if (treeServer != null) {
            try {
                treeServer.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error disconnecting tree server", e);
            }
        }
        if (serverTree != null) {
            try {
                serverTree.close();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error disconnecting server tree", e);
            }
        }
        LOG.info("Done disconnecting aggregator");
    }

    synchronized void addRemote(String remoteTreeBind) throws IOException {
        try {
            updatable = false;

            boolean clientDuplex = true;
            boolean hurry = false;
            String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+remoteTreeBind+"\",hurry=\""+hurry+"\"";
            ITree remoteTree = null;

            LOG.info("Creating remote tree: " + remoteTreeBind);
            remoteTree = tf.create(remoteTreeBind, RmiStoreFactory.storeType, true, false, options);
            LOG.info("Done creating remote tree!");
            String mountName = toMountName(remoteTreeBind);
            LOG.info("Mounting remote tree to: " + mountName);
            serverTree.mount(mountName, remoteTree, "/");

            String remoteDir = toMountName(remoteTreeBind);
            LOG.info("Adding dirs for: " + remoteDir);
            String[] dirNames = listObjectNames(remoteDir, true, "dir");
            for (String dirName : dirNames) {
                String aggName = toAggregateName(dirName);
                LOG.info("Making aggregation dir: " + aggName);
                this.serverTree.mkdirs(aggName);
            }

            remotes.add(remoteTreeBind);
            LOG.info("Number of remotes after add: " + remotes.size());
            LOG.info("Done adding remote");
        } finally {
            updatable = true;
        }
    }

    synchronized void unmount(String remoteTreeBind) {
        try {
            updatable = false;
            String path = toMountName(remoteTreeBind);
            LOG.info("Unmounting: " + path);
            this.serverTree.unmount(path);
            remotes.remove(remoteTreeBind); // TODO: Check that this works okay
            LOG.info("Number of remotes after remove: " + remotes.size());
            LOG.info("Done unmounting");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error unmounting tree", e);
        } finally {
            updatable = true;
        }
    }

    public void run() {
        if (updatable && remotes.size() > 0) {
            clearTree();
            update();
        } else {
            LOG.warning("Skipping aggregation because not updatable or no remotes were added");
        }
    }
}
