package org.hps.online.recon.aida;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;

import hep.aida.ICloud1D;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

/**
 * Create example histograms and data points in a remote AIDA tree
 */
public class ExampleDriver extends RemoteAidaDriver {

    /*
     * AIDA paths
     */
    private static final String ECAL_DIR = "/subdet/ecal";
    private static final String TRACKER_DIR = "/subdet/tracker";
    private static final String PERF_DIR = "/perf";

    /*
     * Performance plots
     */
    private IHistogram1D eventCountH1D;
    private IDataPointSet eventRateDPS;
    private IDataPointSet millisPerEventDPS;

    /*
     * Tracker plots
     */
    private IHistogram1D tracksPerEventH1D;
    private IHistogram1D rawHitsPerTrackH1D;
    private ICloud1D rawTrackerHitsPerEventC1D;
    private ICloud1D chi2C1D;
    private ICloud1D d0C1D;
    private ICloud1D omegaC1D;
    private ICloud1D phiC1D;
    private ICloud1D tanLambdaC1D;
    private ICloud1D z0C1D;
    private ICloud1D hthLayerC1D;
    private ICloud1D hthModuleC1D;
    private ICloud1D fittedTrackerHitsPerEventC1D;

    /*
     * Ecal plots
     */
    private IHistogram2D ecalReadoutHitsXYH2D;
    private IHistogram1D ecalReadoutHitsPerEventH1D;
    private IHistogram1D adcValuesH1D;
    private IHistogram1D clustersPerEventH1D;
    private ICloud1D clusEnergyC1D;
    private ICloud1D clusTimeC1D;
    private ICloud1D clusNHitsC1D;

    /*
     * Collection names
     */
    private static final String CLUSTERS = "EcalClusters";
    private static final String RAW_TRACKER_HITS = "SVTRawTrackerHits";
    private static final String FITTED_HITS = "SVTFittedRawTrackerHits";
    private static final String TRACKS = "MatchedTracks";
    private static final String ECAL_READOUT_HITS = "EcalReadoutHits";
    private static final String HELICAL_TRACK_HITS = "HelicalTrackHits";

    // Convert clouds to histograms after this many entries (-1 means do not convert automatically)
    private static final int CLOUD_MAX = -1;

    /*
     * Event timing
     */
    private int eventsProcessed = 0;
    private long start = -1L;
    private Timer timer;

    private ICloud1D createCloud1D(String name) {
        return hf.createCloud1D(name, name, CLOUD_MAX, "autoconvert=false");
    }

    public void startOfData() {

        /*
         * Tracker plots
         */

        tree.mkdirs(TRACKER_DIR);
        tree.cd(TRACKER_DIR);

        tracksPerEventH1D = aida.histogram1D("Tracks Per Event", 20, 0., 20.);
        //tracksPerEventH1D.annotation().addItem("xAxisLabel", "Tracks / Event");
        //tracksPerEventH1D.annotation().addItem("yAxisLabel", "Count");

        rawHitsPerTrackH1D = aida.histogram1D("Raw Hits Per Track", 10, 0., 10);
        //rawHitsPerTrackH1D.annotation().addItem("xAxisLabel", "Hits / Track");
        //rawHitsPerTrackH1D.annotation().addItem("yAxisLabel", "Count");

        rawTrackerHitsPerEventC1D = createCloud1D("Raw Tracker Hits Per Event");
        chi2C1D = createCloud1D("Chi2");
        d0C1D = createCloud1D("D0");
        omegaC1D = createCloud1D("Omega");
        phiC1D = createCloud1D("Phi");
        tanLambdaC1D = createCloud1D("Tan Lambda");
        z0C1D = createCloud1D("Z0");
        fittedTrackerHitsPerEventC1D = createCloud1D("Fitted Tracker Hits Per Event");
        hthLayerC1D = createCloud1D("Helical Track Hit Layer");
        hthModuleC1D = createCloud1D("Helical Track Hit Module");

        /*
         * Ecal plots
         */

        tree.mkdirs(ECAL_DIR);
        tree.cd(ECAL_DIR);

        clusEnergyC1D = createCloud1D("Cluster Energy");
        clusTimeC1D = createCloud1D("Cluster Time");
        clusNHitsC1D = createCloud1D("Cluster N Hits");
        ecalReadoutHitsXYH2D = aida.histogram2D("Readout Hits XY", 47, -23.5, 23.5, 11, -5.5, 5.5);
        //ecalReadoutHitsXYH2D.annotation().addItem("xAxisLabel", "X Index");
        //ecalReadoutHitsXYH2D.annotation().addItem("yAxisLabel", "Y Index");

        ecalReadoutHitsPerEventH1D = aida.histogram1D("Readout Hits Per Event ", 50, 0, 50.);
        clustersPerEventH1D = aida.histogram1D("Clusters Per Event", 10, -0.5, 9.5);
        adcValuesH1D = aida.histogram1D("Readout Hit ADC Values", 300, 0, 300.);

        tree.mkdir(PERF_DIR);
        tree.cd(PERF_DIR);

        /*
         * Performance plots
         */

        eventCountH1D = aida.histogram1D("Event Count", 1, 0., 1.0);
        eventRateDPS = dpsf.create("Event Rate", "Event Rate", 1);
        millisPerEventDPS = dpsf.create("Millis Per Event", 1);

        TimerTask task = new TimerTask() {
            public void run() {
                if (eventsProcessed > 0 && start > 0) {
                    long elapsed = System.currentTimeMillis() - start;
                    double eps = (double) eventsProcessed / ((double) elapsed / 1000L);
                    double mpe = (double) elapsed / (double) eventsProcessed;
                    LOG.fine("Event Timer: " + eps + " Hz");

                    IDataPoint dp = eventRateDPS.addPoint();
                    dp.coordinate(0).setValue(eps);
                    while (eventRateDPS.size() > 10) {
                        eventRateDPS.removePoint(0);
                    }

                    dp = millisPerEventDPS.addPoint();
                    dp.coordinate(0).setValue(mpe);
                    while (millisPerEventDPS.size() > 10) {
                        millisPerEventDPS.removePoint(0);
                    }
                }
                start = System.currentTimeMillis();
                eventsProcessed = 0;
            }
        };
        Timer timer = new Timer("Event Timer");
        timer.scheduleAtFixedRate(task, 0, 5000L);

        super.startOfData();
    }

    public void endOfData() {

        timer.cancel();

        super.endOfData();
    }

    public void process(EventHeader event) {

        eventCountH1D.fill(0.5);

        List<Track> tracks = event.get(Track.class, TRACKS);

        aida.tree().cd(TRACKER_DIR);

        tracksPerEventH1D.fill(tracks.size());

        for (Track track : tracks) {
            chi2C1D.fill(track.getChi2());
            List<TrackState> trackStates = track.getTrackStates();
            TrackState ts = trackStates.get(0);
            if (ts != null) {
                d0C1D.fill(ts.getD0());
                omegaC1D.fill(ts.getOmega());
                phiC1D.fill(ts.getPhi());
                tanLambdaC1D.fill(ts.getTanLambda());
                z0C1D.fill(ts.getZ0());
            }
            this.rawHitsPerTrackH1D.fill(track.getTrackerHits().size());
        }

        List<TrackerHit> helicalTrackHits = event.get(TrackerHit.class, HELICAL_TRACK_HITS);
        for (TrackerHit hit : helicalTrackHits) {
            int layer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();
            layer = (int) Math.ceil((double) layer / 2);
            hthLayerC1D.fill(layer);
            int module = layer / 2 + 1;
            hthModuleC1D.fill(module);
        }

        rawTrackerHitsPerEventC1D.fill(
                event.get(RawTrackerHit.class, RAW_TRACKER_HITS).size());

        fittedTrackerHitsPerEventC1D.fill(
                event.get(LCRelation.class, FITTED_HITS).size());

        List<RawTrackerHit> ecalHits = event.get(RawTrackerHit.class, ECAL_READOUT_HITS);
        ecalReadoutHitsPerEventH1D.fill(ecalHits.size());
        for (RawTrackerHit hit : ecalHits) {
            int column = hit.getIdentifierFieldValue("ix");
            int row = hit.getIdentifierFieldValue("iy");
            ecalReadoutHitsXYH2D.fill(column, row);
            for (short adcValue : hit.getADCValues()) {
                adcValuesH1D.fill(adcValue);
            }
        }

        List<Cluster> clusterList = event.get(Cluster.class, CLUSTERS);
        for (Cluster clus : clusterList) {
            clusEnergyC1D.fill(clus.getEnergy());
            clusTimeC1D.fill(clus.getCalorimeterHits().get(0).getTime());
            clusNHitsC1D.fill(clus.getCalorimeterHits().size());
        }
        clustersPerEventH1D.fill(clusterList.size());

        ++eventsProcessed;
    }
}
