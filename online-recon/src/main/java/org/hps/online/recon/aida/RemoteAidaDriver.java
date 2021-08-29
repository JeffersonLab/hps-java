package org.hps.online.recon.aida;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.online.recon.Station;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.dev.IDevTree;
import hep.aida.ref.BatchAnalysisFactory;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

/**
 * Abstract driver for providing remote AIDA functionality
 */
public class RemoteAidaDriver extends Driver {

    static final Logger LOG = Logger.getLogger(RemoteAidaDriver.class.getPackage().getName());

    static {
        System.setProperty("hep.aida.IAnalysisFactory", BatchAnalysisFactory.class.getName());
        System.setProperty("java.awt.headless", "true");
    }

    protected RemoteServer treeServer;
    protected RmiServer rmiTreeServer;
    private boolean serverDuplex = true;

    protected final AIDA aida = AIDA.defaultInstance();
    protected final IAnalysisFactory af = aida.analysisFactory();
    protected final IDevTree tree = (IDevTree) aida.tree();
    protected final IHistogramFactory hf = af.createHistogramFactory(tree);
    protected final IDataPointSetFactory dpsf = af.createDataPointSetFactory(tree);

    private String remoteTreeBind = null;
    private String stationName = null;
    private Integer stationNum = null;

    /*
     * Performance plots
     */
    private static String PERF_DIR = "/EventsProcessed";
    private IHistogram1D eventCountH1D;
    private IDataPointSet eventRateDPS;
    private IDataPointSet millisPerEventDPS;

    /*
     * Event timing
     */
    private int eventsProcessed = 0;
    private long start = -1L;
    private Timer timer;
    protected int eventCount = 0;
    private int NPOINTS = 20;

    public RemoteAidaDriver() {

        // Set the station name from the system property
        if (System.getProperties().containsKey(Station.STAT_NAME_KEY)) {
            stationName = System.getProperty(Station.STAT_NAME_KEY);
            LOG.info("Station name set from system prop: " + stationName);
        } else {
            throw new RuntimeException("Station name not set in system props: " + Station.STAT_NAME_KEY);
        }

        // Set the station number
        stationNum = Integer.valueOf(stationName.substring(stationName.lastIndexOf("_") + 1));
        LOG.info("Station num set from name: " + stationNum);

        // Set the RMI URL for the remote AIDA connection
        if (System.getProperties().containsKey(Station.RTB_KEY)) {
            remoteTreeBind = System.getProperty(Station.RTB_KEY);
            LOG.info("Station remote tree binding set from system prop: " + this.remoteTreeBind);
        }

        // Do not allow object overwriting
        tree.setOverwrite(false);
    }

    public String getStationName() {
        return stationName;
    }

    public void setRemoteTreeBind(String remoteTreeBind) {
        this.remoteTreeBind = remoteTreeBind;
    }

    @Override
    protected void endOfData() {

        timer.cancel();

        disconnect();
    }

    @Override
    protected void startOfData() {
        try {
            connect();
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect remote AIDA tree", e);
        }

        tree.mkdir(PERF_DIR);
        tree.cd(PERF_DIR);

        /*
         * Performance plots
         */
        eventCountH1D = aida.histogram1D("Event Count", 1, 0., 1.0);
        eventRateDPS = dpsf.create("Event Rate", "Event Rate", 2);
        millisPerEventDPS = dpsf.create("Millis Per Event", 2);

        startEventTimer();
    }

    private void startEventTimer() {
        TimerTask task = new TimerTask() {
            public void run() {
                if (eventsProcessed > 0 && start > 0) {
                    LOG.info("Event timer is updating -- start=" + start + "; eventsProcessed=" + eventsProcessed);
                    long elapsed = System.currentTimeMillis() - start;
                    double eps = (double) eventsProcessed / ((double) elapsed / 1000L);
                    double mpe = (double) elapsed / (double) eventsProcessed;

                    long currTimeSec = System.currentTimeMillis() / 1000L;

                    IDataPoint dp = eventRateDPS.addPoint();
                    dp.coordinate(0).setValue(currTimeSec);
                    dp.coordinate(1).setValue(eps);
                    while (eventRateDPS.size() > NPOINTS) {
                        eventRateDPS.removePoint(0);
                    }

                    dp = millisPerEventDPS.addPoint();
                    dp.coordinate(0).setValue(currTimeSec);
                    dp.coordinate(1).setValue(mpe);
                    while (millisPerEventDPS.size() > NPOINTS) {
                        millisPerEventDPS.removePoint(0);
                    }
                }
                start = System.currentTimeMillis();
                eventsProcessed = 0;
            }
        };
        timer = new Timer("Event Timer");
        timer.scheduleAtFixedRate(task, 0, 5000L);
    }

    synchronized final void disconnect() {
        try {
            if (rmiTreeServer != null) {
                ((RmiServerImpl) rmiTreeServer).disconnect();
                rmiTreeServer = null;
            }
            if (treeServer != null) {
                treeServer.close();
                treeServer = null;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error disconnecting remote AIDA tree", e);
        }
    }

    synchronized final void connect() throws IOException {

        // HACK: Fixes exceptions from missing AIDA converters
        final RmiStoreFactory rsf = new RmiStoreFactory();

        if (rmiTreeServer != null) {
            LOG.warning("Already connected -- RMI tree server is not null");
            return;
        }
        if (remoteTreeBind == null) {
            throw new IllegalStateException("remoteTreeBind is not set");
        }
        LOG.info("Setting up remote AIDA tree: " + remoteTreeBind);

        treeServer = new RemoteServer(tree, serverDuplex);
        rmiTreeServer = new RmiServerImpl(treeServer, remoteTreeBind);
        LOG.info("Done setting up remote AIDA tree: " + remoteTreeBind);
    }

    public void process(EventHeader event) {
        eventCountH1D.fill(0.5);
        eventCount++;
        eventsProcessed++;
    }
}