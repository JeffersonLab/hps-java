package org.hps.online.recon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
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
import hep.aida.ref.remote.RemoteConnectionException;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

/**
 * Connect to remote AIDA trees and aggregate the plots
 * by adding histograms.
 *
 * Config file example:<br/>
 * <pre>
 * port = 3001
 * name = RmiAidaAgg
 * interval = 5000
 * remotes = //host01:2001/RmiAidaServer,//host01:2002/RmiAidaServer
 * </pre>
 *
 * Currently handles only 1D and 2D histograms.
 *
 * NOTE: This component is kept here as an example of a standalone program
 * for plot aggregation, but its functionality has been integrated into the
 * {@link Server}, so it should only be used in special circumstances.
 *
 * @deprecated Use {@link Server} with the included {@link InlineAggregator}
 */
@Deprecated
public class Aggregator {

    // Application return codes
    private static final int OKAY = 0; // exit without error
    private static final int UNKNOWN_ERROR = 1; // generic error
    private static final int PARSE_ERROR = 2; // error parsing CL opts
    private static final int CONNECTION_ERROR = 3; // error connecting to a remote tree or RMI service

    //private static final String FAIL_ON_CONNECTION_ERROR_PROPERTY = "failOnConnectionError";
    private static final String HOST_PROPERTY = "host";
    private static final String REMOTES_PROPERTY = "remotes";
    private static final String INTERVAL_PROPERTY = "interval";
    private static final String RMI_NAME_PROPERTY = "name";
    private static final String PORT_PROPERTY = "port";

    private static Logger LOG = Logger.getLogger(Aggregator.class.getPackage().getName());

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

    /**
     * List of remote AIDA instances; set with "remotes" property, which should be a
     * comma-delimited list of valid RMI tree bind names e.g. <pre>//localhost:2001/HPSRecon</pre>
     */
    private LinkedHashSet<String> remoteTreeBinds = new LinkedHashSet<String>();

    private IAnalysisFactory af = IAnalysisFactory.create();
    private ITreeFactory tf = af.createTreeFactory();
    private IDevTree serverTree = (IDevTree) tf.create();

    private RemoteServer treeServer;
    private RmiServer rmiTreeServer;

    public Aggregator() {
    }

    private void configure(String path) {
        Properties prop = new Properties();
        try {
            prop.load(new FileReader(new File(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (prop.containsKey(PORT_PROPERTY)) {
            port = Integer.parseInt(prop.getProperty(PORT_PROPERTY));
            if (port < 1024 || port > 65535) {
                throw new IllegalArgumentException("Bad port number: " + port);
            }
        }
        if (prop.containsKey(RMI_NAME_PROPERTY)) {
            serverName = prop.getProperty(RMI_NAME_PROPERTY);
        }
        if (prop.containsKey(INTERVAL_PROPERTY)) {
            updateInterval = Long.parseLong(prop.getProperty(INTERVAL_PROPERTY));
            if (updateInterval < 0) {
                throw new IllegalArgumentException("The interval is invalid: " + updateInterval);
            }
        }
        if (prop.containsKey(REMOTES_PROPERTY)) {
            for (String remote : prop.getProperty(REMOTES_PROPERTY).split(",")) {
                this.remoteTreeBinds.add(remote.trim());
            }
        } else {
            throw new RuntimeException("Missing required configuration \"remotes\" with list of remote servers");
        }
        if (prop.containsKey(HOST_PROPERTY)) {
            this.hostName = prop.getProperty(HOST_PROPERTY);
        }
    }

    private void connect() throws IOException {
        if (this.hostName == null) {
            this.hostName = InetAddress.getLocalHost().getHostName();
        }
        String treeBindName = "//"+this.hostName+":"+port+"/"+serverName;
        LOG.config("Creating RMI server tree: " + treeBindName);
        boolean serverDuplex = true;
        treeServer = new RemoteServer(serverTree, serverDuplex);
        rmiTreeServer = new RmiServerImpl(treeServer, treeBindName);
        serverTree.mkdir(AGG_DIR);
    }

    private void mount() {

        this.serverTree.mkdir(REMOTES_DIR);

        boolean clientDuplex = true;
        boolean hurry = false;
        for (String remoteTreeBind : this.remoteTreeBinds) {
            String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+remoteTreeBind+"\",hurry=\""+hurry+"\"";
            ITree remoteTree = null;
            try {
                LOG.info("Creating remote tree...");
                remoteTree = tf.create(remoteTreeBind, RmiStoreFactory.storeType, true, false, options);
                LOG.info("Done creating remote tree!");
                String mountName = toMountName(remoteTreeBind);
                LOG.info("Mounting remote tree to: " + mountName);
                serverTree.mount(mountName, remoteTree, "/");
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to connect to: " + remoteTreeBind, e);
                exit(e, CONNECTION_ERROR, true);
            }
        }

        String remoteDir = toMountName(this.remoteTreeBinds.iterator().next());
        String[] dirNames = listObjectNames(remoteDir, true, "dir");
        for (String dirName : dirNames) {
            String aggName = toAggregateName(dirName);
            LOG.info("Making aggregation dir: " + aggName);
            this.serverTree.mkdirs(aggName);
        }
    }

    private static String toAggregateName(String remoteName) {
        String[] sp = remoteName.split("/");
        return remoteName.replace(REMOTES_DIR + "/" + sp[2], AGG_DIR);
    }

    private static String toMountName(String treeBindName) {
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

    private void clearTree() {
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
    }

    private void update() {
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
        }
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

    private void disconnect() {

        LOG.info("Disconnecting ...");
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
        LOG.info("Bye!");
    }

    private void loop() throws RemoteConnectionException {
        while (true) {
            try {
                clearTree();
                update();
                if (updateInterval > 0L) {
                    try {
                        Thread.sleep(updateInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                // This assumes all errors are fatal.
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The RMI system spawns many threads which do not appear to shutdown cleanly
     * when certain exceptions are thrown. So this method attempts to interrupt
     * all of them so we can exit the program.
     */
    private static void killRmiThreads() {
        LOG.fine("Interrupting RMI threads...");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            if (thread.getName().contains("RMI")) {
                LOG.fine("Interrupting RMI thread: " + thread.getName());
                thread.interrupt();
            }
        }
    }

    private static void exit(Exception e, int returnCode, boolean killRmiThreads) {

        // This is a disgusting hack to try and get the RMI subsystem to shutdown. :-(
        if (killRmiThreads) {
            killRmiThreads();
        }

        if (e != null) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        LOG.severe("Exiting with return code: " + returnCode);
        System.exit(returnCode);
    }

    private static void exit(Exception e, int returnCode) {
        exit(e, returnCode, false);
    }

    static public void main(String[] args) {
        final Aggregator agg = new Aggregator();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                LOG.config("Disconnecting...");
                agg.disconnect();
                LOG.config("Done disconnecting!");
            }
        });

        if (args.length < 1) {;
            System.out.println("Usage: Aggregator [config_file]");
            exit(null, OKAY);
        }
        try {
            LOG.config("Configuring aggregator...");
            agg.configure(args[0]);
            LOG.config("Done configuring aggregator!");
        } catch (Exception e) {
            LOG.severe("Aggregator configuration failed!");
            exit(e, PARSE_ERROR);
        }
        try {
            LOG.config("Connecting remote tree...");
            agg.connect();
            LOG.config("Done connecting remote tree!");
        } catch (IOException e) {
            LOG.severe("Remote tree connection failed!");
            exit(e, CONNECTION_ERROR);
        }

        LOG.config("Mounting station remote trees...");
        try {
            agg.mount();
            LOG.config("Done mounting station remote trees!");
        } catch (Exception e) {
            /* This should not happen normally as the mount() method
             * will invoke system exit directly on connection failure,
             * but it is theoretically possible if some of the other
             * commands fails, such as creating the AIDA directories.
             */
            LOG.severe("Failed to mount remote trees!");
            exit(e, CONNECTION_ERROR);
        }

        try {
            LOG.config("Starting loop...");
            agg.loop();
        } catch (RemoteConnectionException e) {
            LOG.severe("Connection error occurred!");
            exit(e, CONNECTION_ERROR, true);
        } catch (Exception e) {
            LOG.severe("Unknown error occurred!");
            exit(e, UNKNOWN_ERROR);
        }
    }
}
