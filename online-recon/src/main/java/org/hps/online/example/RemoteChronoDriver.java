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

    private long nProc;
    private long nTot;
    private long updateInt = 1000L;

    private int port = 2001;
    private String serverName = "RmiAidaServer";

    private final IAnalysisFactory af = IAnalysisFactory.create();
    private final ITreeFactory tf = af.createTreeFactory();
    private final IDevTree tree = (IDevTree) tf.create();
    private IDataPointSetFactory dpsf = af.createDataPointSetFactory(tree);

    private IDataPointSet evtPerSec = null;
    private IDataPointSet evts = null;
    private IDataPointSet msPerEvt = null;
    private IDataPointSet avgPerEvt = null;

    private RemoteServer treeServer;
    private RmiServer rmiTreeServer;

    private long start = -1L;
    private long totTime = 0L;

    private TimerTask task = null;
    private Timer timer = new Timer();

    private boolean timerStarted = false;

    private String path = "/chrono";

    private boolean quiet = false;

    public void startOfData() {

        // HACK: Fixes exceptions from missing AIDA converters
        final RmiStoreFactory rsf = new RmiStoreFactory();

        tree.mkdir(path);
        tree.cd(path);

        evtPerSec = dpsf.create("Events Per Second", "Events Per second",      2);
        evts      = dpsf.create("Events",            "Total Events processed", 2);
        msPerEvt  = dpsf.create("Millis Per Event",  "Millis Per Event",       2);
        avgPerEvt = dpsf.create("Avg Per Event",     "Avg Millis Per Event",   2);

        // TODO: Avg Hz (can replace total events)

        tree.ls();

        try {
            connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        task = new TimerTask() {
            public void run() {

                long curr = System.currentTimeMillis() / 1000L;

                if (start > 0) {
                    long elapsed = System.currentTimeMillis() - start;
                    totTime += elapsed;

                    double perSec = (double) nProc / (((double) elapsed) / 1000.);

                    IDataPoint point1 = evtPerSec.addPoint();
                    point1.coordinate(0).setValue(curr);
                    point1.coordinate(1).setValue(perSec);

                    double msPer = (double) elapsed / (double) nProc;
                    IDataPoint point3 = msPerEvt.addPoint();
                    point3.coordinate(0).setValue(curr);
                    point3.coordinate(1).setValue(msPer);

                    double avg = (double) totTime / (double) nTot;
                    IDataPoint point4 = avgPerEvt.addPoint();
                    point4.coordinate(0).setValue(curr);
                    point4.coordinate(1).setValue(avg);

                    if (!quiet) {
                        System.out.println("perSec = " + perSec);
                        System.out.println("msPer = " + msPer);
                        System.out.println("avg = " + avg);
                    }
                }

                IDataPoint point2 = evts.addPoint();
                point2.coordinate(0).setValue(curr);
                point2.coordinate(1).setValue(nTot);

                if (!quiet) {
                    System.out.println("nTot = " + nTot);
                    System.out.println();
                }

                nProc = 0L;
                start = System.currentTimeMillis();
            }
        };

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

    public void process(EventHeader event) {
        if (!timerStarted) {
            timer.schedule(task, 0, updateInt);
            timerStarted = true;
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
