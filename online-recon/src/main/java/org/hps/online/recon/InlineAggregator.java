package org.hps.online.recon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.online.recon.properties.Property;

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
import hep.aida.ref.rootwriter.RootFileStore;
import hep.aida.ref.xml.AidaXMLStore;

/**
 * Version of the plot {@link Aggregator} for running inside the {@link Server}
 * on a scheduled thread executor
 */
public class InlineAggregator implements Runnable {

    private static Logger LOG = Logger.getLogger(InlineAggregator.class.getPackage().getName());

    /** Data where remote AIDA trees are mounted. */
    private static final String REMOTES_DIR = "/remotes";

    /** Directory where plots are aggregated. */
    private static final String AGG_DIR = "/combined";

    /** Network port; set with "port" property */
    private int port = 3001;

    /** Name of the remote server; set with "name" property */
    private String serverName = "HPSRecon";

    /** Name of the host to bind; set with "host" property */
    private String hostName = null;

    /** Interval between aggregation in milliseconds; set with "interval" property */
    private Long updateInterval = 2000L;

    /** URLs of the remote AIDA trees that are currently mounted
     * e.g. <pre>//localhost:4321/MyTree</pre>. */
    // TODO: Make this a map of tree bind names to their ITree objects
    private Set<String> remotes = new HashSet<String>();

    /**
     * AIDA objects for the primary tree
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

    /**
     * Create an instance of the plot aggregator
     */
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

    /**
     * Open the main AIDA remote tree
     * @throws IOException If there is an error when opening the tree
     */
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

    /**
     * Get the name of the directory for combining plots from a
     * tree bind name (URL)
     * @param remoteName The tree bind name
     * @return The AIDA directory for combining plots
     */
    private static String toAggregateName(String remoteName) {
        String[] sp = remoteName.split("/");
        if (sp.length < 3) {
            throw new IllegalArgumentException("Bad remote name: " + remoteName);
        }
        return remoteName.replace(REMOTES_DIR + "/" + sp[2], AGG_DIR);
    }

    /**
     * Convert tree bind name (URL) to an AIDA directory
     * @param treeBindName The tree bind name
     * @return The AIDA directory
     */
    static String toMountName(String treeBindName) {
        return REMOTES_DIR + "/" + treeBindName.replace("//", "")
                .replace("/", "_")
                .replace(":", "_")
                .replace(".", "_");
    }

    /**
     * List object names at the path in the AIDA tree
     * @param path The path in the tree
     * @param recursive Whether objects should be listed recursively
     * @param type Filter by object type (use <code>null</code> for all)
     * @return A list of the full object paths in the tree
     */
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

    /**
     * Clear the aggregated plots in the tree (does not do anything
     * to the remote mounted trees)
     */
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

    /**
     * Update the aggregated plots by adding all the histograms from the remote
     * trees together
     */
    private synchronized void update() {
        LOG.finer("Aggregator is updating plots...");
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
        LOG.finer("Aggregator is done updating plots");
    }

    /**
     * Add an AIDA object to another
     * @param srcName The source AIDA object
     * @param target The target AIDAobject
     */
    private void add(String srcName, IManagedObject target) {
        if (target instanceof IBaseHistogram) {
            LOG.finer("Adding: " + srcName);
            IBaseHistogram srcHisto = (IBaseHistogram) serverTree.find(srcName);
            LOG.finer("src entries: " + srcHisto.entries());
            IBaseHistogram targetHisto = (IBaseHistogram) target;
            LOG.finer("target entries before: " + targetHisto.entries());
            if (target instanceof IHistogram1D) {
                ((IHistogram1D) target).add((IHistogram1D) srcHisto);
            } else if (target instanceof IHistogram2D) {
                ((IHistogram2D) target).add((IHistogram2D) srcHisto);
            }
            LOG.finer("target entries after: " + targetHisto.entries());
        }
    }

    /**
     * Disconnect the aggregator
     *
     * The object is unusable after this unless {@link #connect()}
     * is called again.
     */
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

    /**
     * Add a new remote AIDA tree
     * @param remoteTreeBind The URL of the remote tree
     * @throws IOException If there is an exception opening the remote tree
     */
    synchronized void addRemote(String remoteTreeBind) throws IOException {
        if (remoteTreeBind == null) {
            LOG.warning("remoteTreeBind is null");
            return;
        }
        if (remotes.contains(remoteTreeBind)) {
            LOG.warning("remote already exists");
            return;
        }
        try {
            updatable = false;

            boolean clientDuplex = true;
            boolean hurry = false;
            String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+remoteTreeBind+"\",hurry=\""+hurry+"\"";
            ITree remoteTree = null;

            LOG.info("Creating remote tree: " + remoteTreeBind);
            remoteTree = tf.create(remoteTreeBind, RmiStoreFactory.storeType, true, false, options);
            LOG.info("Done creating remote tree");
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
            LOG.info("Done adding remote tree: " + remoteTreeBind);
            LOG.info("Number of remotes after add: " + remotes.size());
        } finally {
            updatable = true;
        }
    }

    /**
     * Unmount a remote tree e.g. when a station goes inactive
     * @param remoteTreeBind The URL of the remote tree
     */
    synchronized void unmount(String remoteTreeBind) {
        LOG.info("Unmounting remote tree: " + remoteTreeBind);
        if (!remotes.contains(remoteTreeBind)) {
            LOG.warning("no remote called: " + remoteTreeBind);
            return;
        }
        try {
            updatable = false;
            String path = toMountName(remoteTreeBind);
            LOG.info("Unmounting: " + path);
            this.serverTree.unmount(path);
            remotes.remove(remoteTreeBind);
            LOG.info("Number of remotes after remove: " + remotes.size());
            LOG.info("Done unmounting remote tree: " + remoteTreeBind);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error unmounting tree", e);
        } finally {
            updatable = true;
        }
    }

    /**
     * Clear the aggregated plots and then add all the remote plots together
     * (run periodically on a scheduled thread executor)
     */
    public void run() {
        if (updatable && remotes.size() > 0) {
            updatable = false;
            try {
                clearTree();
                update();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error updating plots", e);
            } finally {
                updatable = true;
            }
        } else {
            // This can happen if there are no remote trees, a tree is being mounted/dismounted,
            // or the main tree has been disconnected.
            LOG.finest("Skipping aggregation because not updatable or no remotes were added");
        }
    }

    /**
     * Save an AIDA file with the current tree contents
     * @param file The output AIDA or ROOT file
     * @throws IOException If there is a problem saving the tree
     */
    void save(File file) throws IOException {
        try {
            this.updatable = false;
            String path = file.getCanonicalPath();

            // Modified code from AIDA class in lcsim
            if (path.endsWith(".root")) {
                if (file.exists()) {
                    LOG.info("Deleting old ROOT file: " + path);
                    file.delete();
                }
                RootFileStore store = new RootFileStore(path);
                store.open();
                store.add(serverTree);
                store.close();
                LOG.info("Saved ROOT file: " + path);
            } else {
                if (!path.endsWith(".aida")) {
                    path = path + ".aida";
                    file = new File(path);
                }
                if (file.exists()) {
                    LOG.info("Deleting old AIDA file: " + path);
                    file.delete();
                }
                AidaXMLStore store = new AidaXMLStore();
                de.schlichtherle.io.File newFile = new de.schlichtherle.io.File(path);
                store.commit(serverTree, newFile, null, false, false, false);
                //store.commit(serverTree, newFile, null, true /*useZip*/, false, false);
                LOG.info("Saved AIDA file: " + file.getPath());
            }
        } finally {
            updatable = true;
        }
    }

    /**
     * Thread for mounting a remote AIDA tree asynchronously, e.g. for a running {@link Station}
     * after it has been started
     *
     * Multiple attempts are made until <code>maxAttempts</code> are reached. If no connection is made
     * then the {@link StationProcess} will be deactivated automatically.
     */
    class RemoteTreeBindThread extends Thread {

        StationProcess station;
        String remoteTreeBind;
        Integer maxAttempts = 5;

        RemoteTreeBindThread(StationProcess station, Integer maxAttempts) {
            if (station == null) {
                throw new IllegalArgumentException("station is null");
            }
            this.station = station;
            Property<String> rtbProp = this.station.getProperties().get("lcsim.remoteTreeBind");
            if (!rtbProp.valid()) {
                throw new IllegalArgumentException("Remote tree bind for station is not valid: " + station.stationName);
            }
            this.remoteTreeBind = rtbProp.value();
            if (maxAttempts != null) {
                if (maxAttempts <= 0) {
                    throw new IllegalArgumentException("Bad value for max attempts: " + maxAttempts);
                }
                this.maxAttempts = maxAttempts;
            }
        }

        public void run() {
            for (long attempt = 1; attempt <= this.maxAttempts; attempt++) {
                try {
                    try {
                        Thread.sleep(attempt*5000L);
                    } catch (InterruptedException e) {
                        LOG.log(Level.WARNING, "Interrupted", e);
                        // TODO: Should disconnect station here???
                        break;
                    }
                    LOG.info("Remote tree connection attempt: " + attempt);
                    LOG.info("Adding remote tree: " + remoteTreeBind);
                    addRemote(remoteTreeBind);
                    LOG.info("Done adding remote tree: " + remoteTreeBind);
                    break;
                } catch (Exception e) {
                    LOG.warning("Could not connect to: " + remoteTreeBind);
                    // If all attempts failed then automatically deactivate the station
                    if (attempt == this.maxAttempts) {
                        LOG.warning("Deactivating station because remote tree connection failed: " + station.getStationName());
                        station.deactivate();
                    }
                }
            }
        }
    }
}
