package org.hps.online.recon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import hep.aida.IAnalysisFactory;
import hep.aida.IAxis;
import hep.aida.IBaseHistogram;
import hep.aida.ICloud;
import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.ICloud3D;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogram3D;
import hep.aida.IHistogramFactory;
import hep.aida.IManagedObject;
import hep.aida.IProfile;
import hep.aida.IProfile1D;
import hep.aida.IProfile2D;
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
 * Creates an AIDA tree that is used to combine histograms from multiple
 * online recon {@link Station} instances by connecting to their remote trees
 * and adding histogram objects with the same paths together
 *
 * The resulting combined plots can then be viewed in a remote AIDA client,
 * such as JAS3 with the Remote AIDA plugin or in a browser by connecting to
 * a webapp running the aidatld library.
 *
 * The station trees are mounted into a server tree and added together
 * into a combined set of histograms in a separate directory structure.
 * The remote plots are read-only, so a station's plots can only be reset
 * by restarting it. The remotely mounted trees are automatically
 * unmounted when a station is deactivated (stopped).
 *
 * This class implements <code>Runnable</code> and is designed to be run
 * periodically using a scheduled thread executor by the {@code Server}.
 *
 * The <code>RemoteTreeBindThread</code> is used to connect asynchronously
 * to a station that has been activated for event processing.
 *
 * When saving plots to ROOT, all combined clouds are automatically converted
 * to histograms, as an error will occur otherwise.
 */
public class PlotAggregator implements Runnable {

    /** Package logger */
    private static Logger LOG = Logger.getLogger(PlotAggregator.class.getPackage().getName());

    /** Directory where remote AIDA trees are mounted */
    private static final String REMOTES_DIR = "/remotes";

    /** Directory where plots are aggregated */
    private static final String COMBINED_DIR = "/combined";

    /** Directory for event performance plots */
    public static String EVENTS_DIR = "/EventsProcessed";

    /** Event rate plot name */
    private static final String EVENT_RATE = "Event Rate";

    /** Event processing timing plot name */
    private static final String EVENT_TIME = "Event Time";

    /** Event count plot name in the remote */
    private static final String EVENT_COUNT = "Event Count";

    /** Combined station event count histogram */
    private static final String STATION_EVENT_COUNTS = "Station Event Counts";

    /** Number of bins to use for converting clouds to histograms */
    private static final int CLOUD_BINS = 100;

    /** Network port for AIDA connection */
    private int port = 3001;

    /** Name of the remote server for RMI */
    private String serverName = "HPSRecon";

    /** Name of the host to bind */
    private String hostName = null;

    /** Interval of plot aggregation in milliseconds */
    private Long updateInterval = 5000L;

    /** URLs of the remote AIDA trees that are currently mounted
     * e.g. <pre>//localhost:4321/MyTree</pre> */
    private Set<String> remotes = new HashSet<String>();

    /**
     * AIDA objects for the primary server tree
     */
    private final IAnalysisFactory af = IAnalysisFactory.create();
    private final ITreeFactory tf = af.createTreeFactory();
    private final IDevTree serverTree = (IDevTree) tf.create();
    private final IDataPointSetFactory dpsf = af.createDataPointSetFactory(serverTree);
    private final IHistogramFactory hf = af.createHistogramFactory(serverTree);

    private IDataPointSet eventRateDPS = null;
    private IDataPointSet eventTimeDPS = null;
    private IHistogram1D statEventCountsH1D = null;

    /**
     * RMI server objects
     */
    private RemoteServer treeServer;
    private RmiServer rmiTreeServer;

    /**
     * Dummy object used for synchronization
     */
    private final String updating = "updating";

    private static final Set<String> DIR_TYPE = new HashSet<String>();
    static {
        DIR_TYPE.add("dir");
    }

    int minStatNum = Integer.MAX_VALUE;
    int maxStatNum = Integer.MIN_VALUE;

    /**
     * Create an instance of the plot aggregator
     */
    public PlotAggregator() {
    }

    /**
     * Set the network port
     * @param port The network port
     */
    void setPort(int port) {
        this.port = port;
    }

    /**
     * Set the server name
     * @param serverName The server name
     */
    void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Set the host name
     * @param hostName The host name
     */
    void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Set the update interval in millis
     * @param updateInterval The update interval in millis
     */
    void setUpdateInterval(Long updateInterval) {
        if (updateInterval < 1000L) {
            throw new IllegalArgumentException("Update interval must be >= 1 second");
        }
        if (updateInterval > 30000L) {
            throw new IllegalArgumentException("Update interval must be <= 30 seconds");
        }
        this.updateInterval = updateInterval;
    }

    /**
     * Get the update interval
     * @return The update interval
     */
    Long getUpdateInterval() {
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
        LOG.info("Creating server tree: " + treeBindName);
        boolean serverDuplex = true;
        treeServer = new RemoteServer(serverTree, serverDuplex);
        rmiTreeServer = new RmiServerImpl(treeServer, treeBindName);

        serverTree.setOverwrite(true);

        serverTree.mkdir(COMBINED_DIR);
        serverTree.mkdir(REMOTES_DIR);

        // Create event performance plots
        serverTree.mkdirs(COMBINED_DIR + EVENTS_DIR);
        serverTree.cd(COMBINED_DIR + EVENTS_DIR);
        eventRateDPS = dpsf.create(EVENT_RATE, EVENT_RATE, 2);
        eventTimeDPS = dpsf.create(EVENT_TIME, EVENT_TIME, 2);
        statEventCountsH1D = hf.createHistogram1D(STATION_EVENT_COUNTS, 4, 1, 5);
    }

    /**
     * Get the base path for aggregating plots for a remote tree
     * @param remoteName The tree bind name
     * @return The AIDA path for combining plots
     */
    private static String toAggregateName(String remoteName) {
        String[] sp = remoteName.split("/");
        if (sp.length < 3) {
            throw new IllegalArgumentException("Bad remote name: " + remoteName);
        }
        return remoteName.replace(REMOTES_DIR + "/" + sp[2], COMBINED_DIR);
    }

    /**
     * Convert the remote tree bind (URL) of a remote to a valid AIDA directory name
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
     * List object names at a path in the server AIDA tree
     * @param path The path in the tree
     * @param recursive Whether objects should be listed recursively
     * @param type Filter by a set of object types (use <code>null</code> for all)
     * @return A list of the full object paths in the tree
     */
    private String[] listObjectNames(String path, boolean recursive, Set<String> types) {
        String[] names = null;
        if (types != null) {
            if (types.size() == 0) {
                throw new IllegalArgumentException("Set of types for filtering is empty");
            }
            List<String> filtNames = new ArrayList<String>();
            String[] objectNames = serverTree.listObjectNames(path, recursive);
            String[] objectTypes = serverTree.listObjectTypes(path, recursive);
            for (int i = 0; i < objectNames.length; i++) {
                if (types.contains(objectTypes[i])) {
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
     * Clear the aggregated plots in the tree
     *
     * This does nothing to the remote trees that have been mounted,
     * since they are read-only.
     */
    private synchronized void clearTree() {
        LOG.fine("Clearing tree...");
        String[] objectNames = listObjectNames(COMBINED_DIR, true, null);
        for (String name : objectNames) {
            try {
                IManagedObject obj = serverTree.find(name);
                if (obj instanceof IBaseHistogram) {
                    IBaseHistogram hist = (IBaseHistogram) obj;
                    if (hist.entries() > 0) {
                        ((IBaseHistogram) obj).reset();
                    }
                } else if (obj instanceof IDataPointSet) {
                    ((IDataPointSet) obj).clear();
                }
            } catch (IllegalArgumentException e) {
            }
        }
        LOG.fine("Done clearing tree");
    }

    /**
     * Create the combined set of directories from the remote ones
     */
    private void makeCombinedDirs() {
        String[] remoteDirNames = listObjectNames(REMOTES_DIR, true, DIR_TYPE);
        String[] combinedDirNames = listObjectNames(COMBINED_DIR, true, DIR_TYPE);
        HashSet<String> combinedDirs = new HashSet<String>();
        combinedDirs.addAll(Arrays.asList(combinedDirNames));
        for (String dirName : remoteDirNames) {
            String combinedName = toAggregateName(dirName);
            if (!combinedDirs.contains(combinedName)) {
                LOG.fine("Making missing aggregation dir: " + combinedName);
                serverTree.mkdirs(combinedName);
                combinedDirs.add(combinedName);
            }
        }
    }

    /**
     * Update the aggregated plots by adding all the histograms from the remote
     * trees together
     */
    private void update() {

        LOG.fine("Plot aggregator is updating...");

        // Create combined directories in case any are missing
        makeCombinedDirs();

        // Get the directories for the remote trees
        String[] dirs = null;
        try {
            dirs = listObjectNames(REMOTES_DIR, false, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get remote object dirs from server tree", e);
        }

        // Loop over each remote tree directory
        for (String dir : dirs) {

            LOG.finer("Updating remote: " + dir);

            try {
                // Loop over all the objects in the remote tree
                String[] remoteObjects = serverTree.listObjectNames(dir, true);
                for (String remoteName : remoteObjects) {

                    // Get the source object
                    if (!objectExists(remoteName)) {

                        // The path is a directory and should not be aggregated
                        continue;
                    }

                    // Get the target object
                    String targetPath = toAggregateName(remoteName);

                    // Try to aggregate the remote object
                    try {
                        IManagedObject srcObject = serverTree.find(remoteName);

                        // Check flag whether object should be aggregated
                        if (shouldAggregate(srcObject)) {

                            IManagedObject targetObject = null;

                            // Only histograms are aggregated generically.
                            if (srcObject instanceof IBaseHistogram) {
                                try {
                                    // Get object from the tree
                                    targetObject = serverTree.find(targetPath);

                                    // Add source to target
                                    add((IBaseHistogram) srcObject, (IBaseHistogram) targetObject);
                                } catch (IllegalArgumentException e) {

                                    // Create a new target histogram by copying one of the remote objects
                                    serverTree.cp(remoteName, targetPath, false);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Error updating aggregate plot: " + targetPath, e);
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error aggregating remote dir: " + dir, e);
            }
        }

        LOG.fine("Plot aggregator is done updating");
    }

    /**
     * Check if an object exists at the given path in the server tree
     * @param path The object path
     * @return True if the object exists
     */
    private boolean objectExists(String path) {
        try {
            serverTree.find(path);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Add an AIDA object to another
     * @param srcName The source AIDA object path (from the remote)
     * @param target The target AIDA object (the combined histogram)
     */
    private static void add(IBaseHistogram src, IBaseHistogram target) {
        if (src instanceof IHistogram) {
            // Add two histograms
            add((IHistogram) src, (IHistogram) target);
        } else if (src instanceof ICloud) {
            // Add two clouds
            add((ICloud) src, (ICloud) target);
        } else if (src instanceof IProfile) {
            // Add two profile histograms
            add((IProfile) src, (IProfile) target);
        }
    }

    /**
     * Check whether plot should be aggregated by looking at an annotation flag
     * @param obj The object to check
     * @return True if plot should be aggregated
     */
    static private boolean shouldAggregate(IManagedObject obj) {
        if (obj instanceof IBaseHistogram) {
            IBaseHistogram hist = (IBaseHistogram) obj;
            if (hist.annotation().hasKey("aggregate")) {
                if (hist.annotation().value("aggregate").toLowerCase().equals("false")) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Add two histograms together
     *
     * Entries in <code>src</code> will be added to those in <code>target</code>
     *
     * @param src The source histogram
     * @param target The target histogram
     */
    private static void add(IHistogram src, IHistogram target) {
        if (src.entries() == 0) {
            return;
        }
        if (target instanceof IHistogram1D) {
            ((IHistogram1D) target).add((IHistogram1D) src);
        } else if (target instanceof IHistogram2D) {
            ((IHistogram2D) target).add((IHistogram2D) src);
        } else if (target instanceof IHistogram3D) {
            ((IHistogram3D) target).add((IHistogram3D) src);
        }
    }

    /**
     * Add two profile histograms together
     *
     * Entries in <code>src</code> will be added to those in <code>target</code>
     *
     * @param src The source profile histogram
     * @param target The target profile histogram
     */
    private static void add(IProfile src, IProfile target) {
        if (src.entries() == 0) {
            return;
        }
        if (target instanceof IProfile1D) {
            ((IProfile1D) target).add((IProfile1D) src);
        } else if (target instanceof IHistogram2D) {
            ((IProfile2D) target).add((IProfile2D) src);
        }
    }

    /**
     * Add two unconverted clouds together
     * @param src The source cloud
     * @param target The target cloud
     */
    private static void add(ICloud src, ICloud target) {
        if (src.isConverted()) {
            LOG.warning("Skipping add of converted src cloud: " + src.title());
            return;
        }
        if (src instanceof ICloud1D) {
            for (int i=0; i<src.entries(); i++) {
                ((ICloud1D)target).fill(
                        ((ICloud1D) src).value(i),
                        ((ICloud1D) src).weight(i));
            }
        }
    }

    private List<IDataPointSet> getDataPointSets(String dir, String name) {
        List<IDataPointSet> objects = new ArrayList<IDataPointSet>();
        for (String remoteName : this.remotes) {
            String dpsPath = toMountName(remoteName) +
                    dir + "/" + name;
            try {
                IManagedObject obj = serverTree.find(dpsPath);
                objects.add((IDataPointSet) obj);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to find: " + dpsPath);
            }
        }
        return objects;
    }

    private void addEventRatePlots() {
        List<IDataPointSet> srcObjects = getDataPointSets(EVENTS_DIR, EVENT_RATE);
        if (srcObjects.size() == 0) {
            return;
        }
        add(srcObjects, this.eventRateDPS, false);
    }

    private static void add(List<IDataPointSet> sources, IDataPointSet target, boolean average) {
        for (int i = 0;; i++) {
            double xVal = 0;
            double yVal = 0;
            for (IDataPointSet src : sources) {
                try {
                    if (xVal == 0) {
                        xVal = src.point(i).coordinate(0).value();
                    }
                    yVal += src.point(i).coordinate(1).value();
                } catch (Exception e) {
                    break;
                }
            }
            if (xVal == 0) {
                break;
            }
            if (average) {
                yVal = yVal / sources.size();
            }
            IDataPoint targetPoint = target.addPoint();
            targetPoint.coordinate(0).setValue(xVal);
            targetPoint.coordinate(1).setValue(yVal);
        }
    }

    private void addEventTimePlots() {
        List<IDataPointSet> srcObjects = getDataPointSets(EVENTS_DIR, EVENT_TIME);
        if (srcObjects.size() == 0) {
            return;
        }
        add(srcObjects, this.eventTimeDPS, true);
    }

    private void updateStationNumbers() {
        for (String remote: this.remotes) {
            String mountName = toMountName(remote);
            Integer statNum = Integer.valueOf(mountName.substring(mountName.lastIndexOf("_") + 1));
            if (statNum < minStatNum) {
                minStatNum = statNum;
                LOG.info("Min station num set to: " + minStatNum);
            }
            if (statNum > maxStatNum) {
                maxStatNum = statNum;
                LOG.info("Max station num set to: " + maxStatNum);
            }
        }
    }

    private void addStationEventCounts() {

        if (this.remotes.size() == 0) {
            return;
        }

        // Check if the plot should be recreated
        int nbins = (maxStatNum - minStatNum) + 1;
        int lower = minStatNum;
        int upper = maxStatNum + 1;
        IAxis ax = statEventCountsH1D.axis();
        if (ax.bins() != nbins || ax.binLowerEdge(0) != lower || ax.binUpperEdge(ax.bins() - 1) != upper) {
            LOG.info("Recreating stat event counts H1D");
            serverTree.cd(COMBINED_DIR + EVENTS_DIR);
            statEventCountsH1D = hf.createHistogram1D("Station Event Counts", nbins, lower, upper);
        }

        statEventCountsH1D.reset();

        // Update the combined plot with each station's event count
        for (String remote : this.remotes) {
            String mountName = toMountName(remote);
            Integer statNum = Integer.valueOf(mountName.substring(mountName.lastIndexOf("_") + 1));
            String eventCountPath = mountName + EVENTS_DIR + "/" + EVENT_COUNT;
            try {
                IHistogram1D eventCount = (IHistogram1D) serverTree.find(eventCountPath);
                int nevents = eventCount.entries();
                statEventCountsH1D.fill(statNum, nevents);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to find object: " + eventCountPath, e);
            }
        }
    }

    /**
     * Convert all AIDA clouds to histograms within a tree
     *
     * @param tree The AIDA tree
     * @throws IOException If there is an error converting the clouds
     */
    private static void convertClouds(ITree tree) throws IOException {
        if (tree.isReadOnly()) {
            throw new IOException("Tree is read only");
        }
        String[] objects = tree.listObjectNames("/", true);
        String[] types = tree.listObjectTypes("/", true);
        for (int i=0; i<objects.length; i++) {
            String path = objects[i];
            String type = types[i];
            if (type.contains("Cloud")) {
                IManagedObject obj = tree.find(path);
                ICloud cloud = (ICloud) obj;
                if (cloud.isConverted()) {
                    continue;
                }
                LOG.finest("Converting cloud to hist: " + cloud.title());
                try {
                    if (obj instanceof ICloud1D) {
                        ICloud1D c1d = (ICloud1D) obj;
                        c1d.setConversionParameters(CLOUD_BINS, c1d.lowerEdge(), c1d.upperEdge());
                    } else if (obj instanceof ICloud2D) {
                        ICloud2D c2d = (ICloud2D) obj;
                        c2d.setConversionParameters(CLOUD_BINS, c2d.lowerEdgeX(), c2d.upperEdgeX(),
                                CLOUD_BINS, c2d.lowerEdgeY(), c2d.upperEdgeY());
                    } else if (obj instanceof ICloud3D) {
                        ICloud3D c3d = (ICloud3D) obj;
                        c3d.setConversionParameters(CLOUD_BINS, c3d.lowerEdgeX(), c3d.upperEdgeX(),
                                CLOUD_BINS, c3d.lowerEdgeY(), c3d.upperEdgeY(),
                                CLOUD_BINS, c3d.lowerEdgeZ(), c3d.upperEdgeZ());
                    }
                    cloud.convertToHistogram();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error converting cloud to histogram: " + cloud.title());
                }
            }
        }
    }

    /**
     * Copy objects and directories from one tree into another
     * @param srcTree The source tree
     * @param targetTree The target tree
     * @param srcPath The path in the source tree from which to copy
     */
    private static void copy(ITree srcTree, IDevTree targetTree, String srcPath) {
        if (srcPath == null) {
            srcPath = "/";
        }
        String[] objects = srcTree.listObjectNames(srcPath, true);
        String[] types = srcTree.listObjectTypes(srcPath, true);
        for (int i=0; i<objects.length; i++) {
            String path = objects[i];
            String type = types[i];
            if (type.equals("dir")) {
                LOG.finest("Creating dir in output tree: " + path);
                targetTree.mkdirs(path);
            } else {
                LOG.finest("Copying object to target tree: " + path);
                IManagedObject object = srcTree.find(path);
                List<String> spl = new ArrayList<String>(Arrays.asList(path.split("/")));
                spl.remove(spl.size() - 1);
                String dir = String.join("/", spl);
                targetTree.add(dir, object);
            }
        }
    }

    /**
     * Disconnect the object's remote tree server
     *
     * The object is unusable after this unless {@link #connect()}
     * is called again.
     */
    synchronized void disconnect() {

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
     * Mount a new remote AIDA tree from a station
     * @param remoteTreeBind The URL of the remote tree
     * @throws IOException If there is an exception opening the remote tree
     */
    void mount(String remoteTreeBind) throws IOException {
        if (remoteTreeBind == null) {
            throw new IOException("The remoteTreeBind is null");
        }
        if (remotes.contains(remoteTreeBind)) {
            LOG.warning("Remote already exists: " + remoteTreeBind);
            return;
        }

        boolean clientDuplex = true;
        boolean hurry = false;
        String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+remoteTreeBind+"\",hurry=\""+hurry+"\"";
        ITree remoteTree = null;

        LOG.info("Creating remote tree: " + remoteTreeBind);
        remoteTree = tf.create(remoteTreeBind, RmiStoreFactory.storeType, true, false, options);
        String mountName = toMountName(remoteTreeBind);

        LOG.fine("Mounting remote tree to: " + mountName);
        serverTree.mount(mountName, remoteTree, "/");

        String remoteDir = toMountName(remoteTreeBind);
        LOG.fine("Adding dirs for: " + remoteDir);

        String[] dirNames = listObjectNames(remoteDir, true, DIR_TYPE);
        for (String dirName : dirNames) {
            String aggName = toAggregateName(dirName);
            LOG.fine("Making aggregation dir: " + aggName);
            this.serverTree.mkdirs(aggName);
        }

        remotes.add(remoteTreeBind);

        LOG.info("Done creating remote tree: " + remoteTreeBind);
        LOG.info("Number of remotes after add: " + remotes.size());
    }

    /**
     * Unmount a remote tree e.g. when a station goes inactive
     * @param remoteTreeBind The URL of the remote tree
     */
    void unmount(String remoteTreeBind) {
        LOG.info("Unmounting remote tree: " + remoteTreeBind);
        if (remoteTreeBind == null) {
            LOG.warning("Remote tree bind is null");
            return;
        }
        if (!remotes.contains(remoteTreeBind)) {
            LOG.warning("No registered remote with name: " + remoteTreeBind);
            return;
        }
        try {
            String path = toMountName(remoteTreeBind);
            LOG.fine("Unmounting: " + path);
            this.serverTree.unmount(path);
            remotes.remove(remoteTreeBind);
            updateStationNumbers();
            LOG.info("Number of remotes after remove: " + remotes.size());
            LOG.info("Done unmounting remote tree: " + remoteTreeBind);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error unmounting remote tree: " + remoteTreeBind, e);
        }
    }

    /**
     * Clear the aggregated plots and then add all the remote plots together
     * into combined plots
     *
     * Also update event processing plots
     */
    @Override
    public void run() {
        if (remotes.size() == 0) {
            return;
        }
        try {
            double start = (double) System.currentTimeMillis();
            synchronized (updating) {

                // Clear the combined tree
                clearTree();

                // Add event rate plots
                try {
                    addEventRatePlots();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to add event rate plots", e);
                }

                // Add event time plots
                try {
                    addEventTimePlots();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to add event time plots", e);
                }

                // Update the combined plots
                update();

                // Add the station event counts plot
                try {
                    addStationEventCounts();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to add station event count plot", e);
                }
            }

            // Print how long the update took
            double elapsed = ((double) System.currentTimeMillis()) - start;
            LOG.info("Plot update took: " + elapsed/1000. + " sec");

            // Broadcast a message to clients (e.g. the web application) that an update occurred
            PlotNotifier.instance().broadcast("updated at " + new Date().toString());

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error updating plots", e);
        }
    }

    /**
     * Save an AIDA file with the current tree contents
     *
     * Only the combined plots are saved to the output file, by creating an
     * output tree.
     *
     * @param file The output AIDA or ROOT file (use '.aida' or '.root' to specify)
     * @throws IOException If there is a problem saving the tree
     */
    void save(File file) throws IOException {

        String path = file.getCanonicalPath();

        synchronized (updating) {
            /*
             * Copy server objects to a new AIDA tree containing
             * only the combined outputs.
             */
            IDevTree outputTree = (IDevTree) tf.create();
            synchronized (this.serverTree) {
                copy(serverTree, outputTree, PlotAggregator.COMBINED_DIR);
            }

            // Modified code from AIDA class in lcsim
            if (path.endsWith(".root")) {

                /*
                 * The combined clouds must be converted to histograms
                 * before writing to a ROOT file or an error will occur.
                 */
                convertClouds(outputTree);

                if (file.exists()) {
                    LOG.info("Deleting old ROOT file: " + path);
                    file.delete();
                }
                RootFileStore store = new RootFileStore(path);
                store.open();
                store.add(outputTree);
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
                store.commit(outputTree, newFile, null, false, false, false);
                // store.commit(serverTree, newFile, null, true /*gzip*/, false, false);
                LOG.info("Saved AIDA file: " + file.getPath());
            }

            outputTree.close();
            outputTree = null;
        }
    }

    /**
     * Add a remote tree binding for aggregation
     * @param remoteTreeBindStr The URL of the remote tree
     * @param maxAttempts The max number of connection attempts
     * @param initialWaitMillis The initial wait in milliseconds before trying to connect
     * @param backoffMillis The back-off wait multiplier in milliseconds between attempts
     * @return True if adding the remote tree was successful
     * @throws InterruptedException If the method is interrupted
     * @throw IOException If there is an error adding the remote tree
     */
    boolean addRemoteTree(String remoteTreeBindStr, int maxAttempts, long initialWaitMillis, long backoffMillis)
            throws InterruptedException, IOException {
        boolean mounted = false;

        LOG.info("Mounting remote tree: " + remoteTreeBindStr);
        if (remoteTreeBindStr == null) {
            throw new IllegalArgumentException("The remoteTreeBindStr is null");
        }
        if (initialWaitMillis > 0) {
            LOG.info("Waiting for station initialization...");
            Thread.sleep(initialWaitMillis);
        }

        for (long attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                long waitMillis = attempt * backoffMillis;
                LOG.info("Waiting before connection attempt...");
                Thread.sleep(waitMillis);
                LOG.info("Connection attempt: " + attempt + "/" + maxAttempts);
                mount(remoteTreeBindStr);
                LOG.info("Successfully connected: " + remoteTreeBindStr);
                mounted = true;
                break;
            } catch (InterruptedException e) {
                LOG.warning("Remote connection attempt was interrupted");
                break;
            } catch (Exception e) {
                LOG.warning("Connection attempt failed -- will retry");
            }
        }
        if (mounted == false) {
            LOG.severe("Failed to connect: " + remoteTreeBindStr);
        } else {
            // Keep track of station numbers for plotting
            updateStationNumbers();
        }

        return mounted;
    }

    /**
     * Mount tree with default arguments
     * @param remoteTreeBindStr The URL of the remote tree
     * @return True if adding the remote tree was successful
     * @throws InterruptedException If the method is interrupted
     * @throw IOException If there is a problem adding the remote tree
     */
    boolean addRemoteTree(String remoteTreeBindStr)
        throws InterruptedException, IOException {
        return addRemoteTree(remoteTreeBindStr, 5, 5000, 2000);
    }
}
