package org.hps.online.example;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import hep.aida.IAnalysisFactory;
import hep.aida.IBaseHistogram;
import hep.aida.IDataPointSet;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IManagedObject;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.dev.IDevTree;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

/**
 * Connect to remote AIDA trees and aggregate the plots
 *
 * Config file example:<br/>
 * <pre>
 * port = 3001
 * name = RmiAidaAgg
 * interval = 5000
 * remotes = //host01:2001/RmiAidaServer,//host01:2002/RmiAidaServer
 * </pre>
 */
public class RemoteAggregator {

    private static final String REMOTES_DIR = "/remotes";

    private static final String AGG_DIR = "/agg";

    /** Network port; set with "port" property */
    private int port = 3001;

    /** Name of the remote server; set with "name" property */
    private String serverName = "RmiAidaServer";

    /** Update interval between aggregation; set with "interval" property */
    private Long updateInterval = 5000L;

    /**
     * List of remote AIDA instances; set with "remotes" property, which should be a
     * comma-delimited list
     */
    private LinkedHashSet<String> remoteTreeBinds = new LinkedHashSet<String>();

    private IAnalysisFactory af = IAnalysisFactory.create();
    private ITreeFactory tf = af.createTreeFactory();
    private IDevTree serverTree = (IDevTree) tf.create();

    private RemoteServer treeServer;
    private RmiServer rmiTreeServer;

    private Map<String, ITree> remoteTrees = new HashMap<String, ITree>();

    public RemoteAggregator() {
    }

    private void configure(String path) {
        Properties prop = new Properties();
        try {
            prop.load(new FileReader(new File(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (prop.containsKey("port")) {
            port = Integer.parseInt(prop.getProperty("port"));
        }
        if (prop.containsKey("server")) {
            serverName = prop.getProperty("server");
        }
        if (prop.containsKey("interval")) {
            updateInterval = Long.parseLong(prop.getProperty("interval"));
        }
        if (prop.containsKey("remotes")) {
            for (String remote : prop.getProperty("remotes").split(",")) {
                this.remoteTreeBinds.add(remote.trim());
            }
        } else {
            throw new RuntimeException("Missing required configuration \"remotes\" with list of remote servers");
        }
    }

    private void connect() throws IOException {
        String localHost = null;
        try {
            localHost = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String treeBindName = "//"+localHost+":"+port+"/"+serverName;
        System.out.println("Server tree: " + treeBindName);
        try {
            boolean serverDuplex = true;
            treeServer = new RemoteServer(serverTree, serverDuplex);
            rmiTreeServer = new RmiServerImpl(treeServer, treeBindName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                remoteTree = tf.create(remoteTreeBind, RmiStoreFactory.storeType, true, false, options);
                //remoteTree.ls("/", true);
                remoteTrees.put(remoteTreeBind, remoteTree);

                String mountName = toMountName(remoteTreeBind);
                System.out.println("Mounting remote tree to: " + mountName);
                serverTree.mount(mountName, remoteTree, "/");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String remoteDir = toMountName(this.remoteTreeBinds.iterator().next());
        String[] dirNames = listObjectNames(remoteDir, true, "dir");
        for (String dirName : dirNames) {
            String aggName = toAggregateName(dirName);
            System.out.println("Making agg dir: " + aggName);
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
                } else if (obj instanceof IDataPointSet) {
                    ((IDataPointSet)obj).clear();
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
                            System.out.println("Copying: " + remoteName + " -> " + aggName);
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
        if (obj instanceof IHistogram1D) {
            System.out.println("Adding: " + srcName + " -> " + obj.name());
            IHistogram1D target = (IHistogram1D) obj;
            IHistogram1D src = (IHistogram1D) serverTree.find(srcName);
            System.out.println("src entries: " + src.entries());

            System.out.println("target entries before: " + target.entries());
            target.add(src);
            System.out.println("target entries after: " + target.entries());

            System.out.println();
        }
    }

    private void disconnect() {
        System.out.println("Disconnecting ...");
        ((RmiServerImpl) rmiTreeServer).disconnect();
        treeServer.close();
        try {
            serverTree.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Bye!");
    }

    private void loop() {
        while (true) {
            clearTree();
            update();
            if (updateInterval > 0) {
                try {
                    Thread.sleep(updateInterval);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    static public void main(String[] args) {
        final RemoteAggregator agg = new RemoteAggregator();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                agg.disconnect();
            }
        });

        if (args.length < 1) {;
            System.out.println("Usage: RemoteAggregator [config_file]");
            System.exit(1);
        }
        agg.configure(args[0]);
        try {
            agg.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        agg.mount();
        agg.loop();
    }
}
