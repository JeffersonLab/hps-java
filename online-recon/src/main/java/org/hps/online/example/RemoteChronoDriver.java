package org.hps.online.example;

import java.io.IOException;
import java.net.InetAddress;
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
 * Time event processing and plots in a remote AIDA tree
 * that can be accessed using an RMI client.
 */
public class RemoteChronoDriver extends Driver {

    static {
        System.setProperty("hep.aida.IAnalysisFactory", BatchAnalysisFactory.class.getName());
        System.setProperty("java.awt.headless", "true");
    }

    private long nEventsProcessed;
    private long nEventsTotal;
    private long updateInterval = 1000L;

    private int port = 2001;
    private String serverName = "RmiAidaServer";

    private final IAnalysisFactory af = IAnalysisFactory.create();
    private final ITreeFactory tf = af.createTreeFactory();
    private final IDevTree tree = (IDevTree) tf.create();
    private IDataPointSetFactory dpsf = af.createDataPointSetFactory(tree);

    private IDataPointSet eventsPerSecondPlot = null;
    private IDataPointSet eventsPlot = null;

    private RemoteServer treeServer;
    private RmiServer rmiTreeServer;

    private long start = -1L;

    private TimerTask task = null;
    private Timer timer = new Timer();

    private boolean timerStarted = false;

    private String path = "/chrono";

    public void startOfData() {

        // HACK: Fixes exceptions about missing AIDA converters
        final RmiStoreFactory rsf = new RmiStoreFactory();

        tree.mkdir(path);
        tree.cd(path);

        eventsPerSecondPlot = dpsf.create("Events Per Second", "Events per second", 2);
        eventsPlot = dpsf.create("Events", "Total events processed", 2);

        tree.ls();

        try {
            connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        task = new TimerTask() {
            public void run() {
                long curr = System.currentTimeMillis() / 1000L;
                long elapsed = System.currentTimeMillis() - start;
                double perSec = (double) nEventsProcessed / (((double) elapsed) / 1000.);
                IDataPoint point1 = eventsPerSecondPlot.addPoint();
                System.out.println("Events per second: " + curr + " = " + perSec);
                point1.coordinate(0).setValue(curr);
                point1.coordinate(1).setValue(perSec);
                IDataPoint point2 = eventsPlot.addPoint();
                System.out.println("Events: " + curr + " = " + nEventsTotal);
                point2.coordinate(0).setValue(curr);
                point2.coordinate(1).setValue(nEventsTotal);
                start = System.currentTimeMillis();
                nEventsProcessed = 0L;
            }
        };

    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setUpdateIntervalMillis(Long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void process(EventHeader event) {
        if (!timerStarted) {
            timer.schedule(task, 0, updateInterval);
            timerStarted = true;
        }
        ++nEventsProcessed;
        ++nEventsTotal;
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
