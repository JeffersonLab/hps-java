package org.hps.online.recon.aida;

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
// TODO: Move to plotting package
public class StripChartDriver extends Driver {

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

    private volatile long start = -1L;
    private volatile long curr = 0L;
    private volatile long tot = 0L;

    private TimerTask task = null;
    private Timer timer = new Timer();

    private static final String PERF_PATH = "/perf";
    private static final String SUBDET_PATH = "/subdet";
    private static final String AVG_DIR = "avg";

    private Map<IDataPointSet, IDataPointSet> averages =
            new HashMap<IDataPointSet, IDataPointSet>();

    private Map<IDataPointSet, Double> totals =
            new HashMap<IDataPointSet, Double>();

    private static final Runtime RUNTIME = Runtime.getRuntime();

    private IDataPointSet createDataPointSet(String name, String title, String label) {
        IDataPointSet dps = dpsf.create(name, title, NDIM);
        dps.annotation().addItem("label", label);
        return dps;
    }

    private boolean timerStarted = false;

    public void startOfData() {

        // HACK: Fixes exceptions from missing AIDA converters
        final RmiStoreFactory rsf = new RmiStoreFactory();

        tree.mkdir(PERF_PATH);
        tree.cd(PERF_PATH);
        evtPerSec = createDataPointSet("Events Per Second", "Events Per second",  "Hz");
        evts      = createDataPointSet("Total Events",      "Total Events",       "Events");
        msPerEvt  = createDataPointSet("Millis Per Event",  "Millis Per Event",   "Millis");
        mem       = createDataPointSet("Memory Usage",      "Memory Usage",       "MB");

        tree.mkdir(SUBDET_PATH);
        tree.cd(SUBDET_PATH);
        for (String name : collections.keySet()) {
            createDataPointSet(name, name + " Count", "Count");
        }

        String[] names = tree.listObjectNames("/", true);
        String[] types = tree.listObjectTypes("/", true);
        tree.mkdir(PERF_PATH + "/" + AVG_DIR);
        tree.mkdir(SUBDET_PATH + "/" + AVG_DIR);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (types[i].equals("IDataPointSet") && !name.contains("Total Events")) {
                String[] spl = name.split("/");
                String dir = spl[1];
                String plotName = spl[2];
                String avgPath = "/" + dir + "/" + AVG_DIR;
                if (!tree.pwd().equals(avgPath)) {
                    tree.cd(avgPath);
                }
                IDataPointSet dps = createDataPointSet(plotName, plotName, "");
                averages.put((IDataPointSet) tree.find(name), dps);
            }
        }

        System.out.println("Created remote tree objects: ");
        tree.ls("/", true);
        System.out.println();

        try {
            connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        task = new TimerTask() {
            public void run() {

                if (start > 0) {

                    curr = System.currentTimeMillis() / 1000L;

                    long elapsed = System.currentTimeMillis() - start;
                    tot += elapsed;

                    IDataPoint point1 = evtPerSec.addPoint();
                    point1.coordinate(0).setValue(curr);
                    point1.coordinate(1).setValue(nProc);
                    updateAverage(evtPerSec, nProc);

                    if (nProc > 0) {
                        double msPer = (double) elapsed / (double) nProc;
                        IDataPoint point3 = msPerEvt.addPoint();
                        point3.coordinate(0).setValue(curr);
                        point3.coordinate(1).setValue(msPer);
                        updateAverage(msPerEvt, msPer);
                    }

                    double kb = (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 1000000L;
                    IDataPoint point4 = mem.addPoint();
                    point4.coordinate(0).setValue(curr);
                    point4.coordinate(1).setValue(kb);
                    updateAverage(mem, kb);

                    IDataPoint point2 = evts.addPoint();
                    point2.coordinate(0).setValue(curr);
                    point2.coordinate(1).setValue(nTot);

                    for (Entry<String, Integer> entry : collections.entrySet()) {
                        IDataPointSet dps = (IDataPointSet) tree.find(SUBDET_PATH + "/" + entry.getKey());
                        IDataPoint p = dps.addPoint();
                        p.coordinate(0).setValue(curr);
                        p.coordinate(1).setValue(entry.getValue());
                        updateAverage(dps, entry.getValue());
                    }
                    for (String name : collections.keySet()) {
                        collections.put(name, 0);
                    }
                }

                nProc = 0L;
                start = System.currentTimeMillis();
            }
        };
    }

    private void updateAverage(IDataPointSet dps, double val) {
        if (!totals.containsKey(dps)) {
            totals.put(dps, 0.);
        }
        Double oldValue = totals.get(dps);
        Double newValue = oldValue + val;
        totals.put(dps, newValue);
        IDataPoint p = averages.get(dps).addPoint();
        p.coordinate(0).setValue(curr);
        Double newAvg = newValue / ((double) tot / 1000.);
        p.coordinate(1).setValue(newAvg);
    }

    // TODO: Use a system property instead?
    public void setPort(int port) {
        this.port = port;
    }

    // TODO: Use a system property instead?
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
        if (!timerStarted) {
            timer.schedule(task, 0, updateInt);
            timerStarted = true;
        }
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
        // TODO: option to force binding to "localhost" instead of host name
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
