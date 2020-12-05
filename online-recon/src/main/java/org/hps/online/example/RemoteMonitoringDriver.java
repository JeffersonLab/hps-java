package org.hps.online.example;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.ITreeFactory;
import hep.aida.dev.IDevTree;
import hep.aida.ref.BatchAnalysisFactory;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

/**
 * Time event processing and make plots in a remote AIDA tree
 * that can be accessed using an RMI client.
 */
public class RemoteMonitoringDriver extends Driver {

    static {
        System.setProperty("hep.aida.IAnalysisFactory", BatchAnalysisFactory.class.getName());
        System.setProperty("java.awt.headless", "true");
    }

    private Map<String, Integer> collections =
            new HashMap<String, Integer>();

    private long nProc;
    private long nTot;
    private long updateInt = 1000L;

    private int port = 2001;
    private String serverName = "RmiAidaServer";

    private final int NDIM = 2;

    private final IAnalysisFactory af = IAnalysisFactory.create();
    private final ITreeFactory tf = af.createTreeFactory();
    private final IDevTree tree = (IDevTree) tf.create();
    private IDataPointSetFactory dpsf = af.createDataPointSetFactory(tree);

    private IDataPointSet evtPerSec = null;
    private IDataPointSet evts = null;
    private IDataPointSet msPerEvt = null;
    private IDataPointSet mem = null;

    private RemoteServer treeServer;
    private RmiServer rmiTreeServer;

    private long start = -1L;
    //private long totTime = 0L;

    private TimerTask task = null;
    private Timer timer = new Timer();

    private String perfPath = "/perf";
    private String subdetPath = "/subdet";

    private boolean quiet = false;

    private static final Runtime RUNTIME = Runtime.getRuntime();

    private IDataPointSet createDataPointSet(String name, String title, String label) {
        IDataPointSet dps = dpsf.create(name, title, NDIM);
        dps.annotation().addItem("label", label);
        return dps;
    }

    public void startOfData() {

        // HACK: Fixes exceptions from missing AIDA converters
        final RmiStoreFactory rsf = new RmiStoreFactory();

        tree.mkdir(perfPath);
        tree.cd(perfPath);
        evtPerSec = createDataPointSet("Events Per Second", "Events Per second",       "Hz");
        evts      = createDataPointSet("Events",            "Total Events processed",  "Events");
        msPerEvt  = createDataPointSet("Millis Per Event",  "Millis Per Event",        "Millis");
        mem       = createDataPointSet("Memory Usage",      "Memory usage",            "MB");

        tree.mkdir(subdetPath);
        tree.cd(subdetPath);
        for (String name : collections.keySet()) {
            createDataPointSet(name, name + " Count", "Count");
        }

        // TODO: overlay rolling averages (client needs to plot into the same region)

        if (!quiet) {
            tree.ls("/", true);
        }

        try {
            connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        task = new TimerTask() {
            public void run() {

                long curr = System.currentTimeMillis() / 1000L;

                if (start > 0) {

                    System.out.println("curr: " + curr);
                    long elapsed = System.currentTimeMillis() - start;
                    //totTime += elapsed;

                    //double perSec = (double) nProc / (((double) elapsed) / 1000.);

                    IDataPoint point1 = evtPerSec.addPoint();
                    point1.coordinate(0).setValue(curr);
                    point1.coordinate(1).setValue(nProc);

                    double msPer = (double) elapsed / (double) nProc;
                    IDataPoint point3 = msPerEvt.addPoint();
                    point3.coordinate(0).setValue(curr);
                    point3.coordinate(1).setValue(msPer);

                    //double avg = (double) totTime / (double) nTot;
                    //IDataPoint point4 = avgPerEvt.addPoint();
                    //point4.coordinate(0).setValue(curr);
                    //point4.coordinate(1).setValue(avg);
                    double kb = (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 1000000L;
                    IDataPoint point4 = mem.addPoint();
                    point4.coordinate(0).setValue(curr);
                    point4.coordinate(1).setValue(kb);

                    if (!quiet) {
                        System.out.println("perSec: " + nProc);
                        System.out.println("msPer: " + msPer);
                        System.out.println("mem: " + kb);
                        //System.out.println("avg: " + avg);
                    }

                    IDataPoint point2 = evts.addPoint();
                    point2.coordinate(0).setValue(curr);
                    point2.coordinate(1).setValue(nTot);

                    if (!quiet) {
                        System.out.println("nTot: " + nTot);
                    }

                    for (Entry<String, Integer> entry : collections.entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                        IDataPointSet dps = (IDataPointSet) tree.find(subdetPath + "/" + entry.getKey());
                        IDataPoint p = dps.addPoint();
                        p.coordinate(0).setValue(curr);
                        p.coordinate(1).setValue(entry.getValue());
                    }
                    for (String name : collections.keySet()) {
                        collections.put(name, 0);
                    }

                    if (!quiet) {
                        System.out.println();
                    }
                }

                nProc = 0L;
                start = System.currentTimeMillis();
            }
        };

        timer.schedule(task, 0, updateInt);
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setUpdateIntervalMillis(Long updateInterval) {
        this.updateInt = updateInterval;
    }

    public void setCollections(String[] collections) {
        for (String collection : collections) {
            this.collections.put(collection, 0);
        }
    }

    public void process(EventHeader event) {
        for (String name : this.collections.keySet()) {
            List list = (List) event.get(name);
            Integer count = this.collections.get(name);
            count += list.size();
            this.collections.put(name, count);
        }
        ++nProc;
        ++nTot;
    }

    public void endOfData() {
        timer.cancel();
        disconnect();
    }

    private void disconnect() {
        ((RmiServerImpl) rmiTreeServer).disconnect();
        treeServer.close();
        try {
            tree.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect() throws IOException {
        String localHost = null;
        try {
            localHost = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String treeBindName = "//"+localHost+":"+port+"/"+serverName;
        System.out.println("Server tree: " + treeBindName);
        try {
            boolean serverDuplex = true;
            treeServer = new RemoteServer(tree, serverDuplex);
            rmiTreeServer = new RmiServerImpl(treeServer, treeBindName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
