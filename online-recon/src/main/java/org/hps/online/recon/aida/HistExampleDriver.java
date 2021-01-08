package org.hps.online.recon.aida;

import java.util.List;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

import hep.aida.ICloud1D;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

/**
 * Create example 1D and 2D histograms in a remote AIDA tree
 */
public class HistExampleDriver extends RemoteAidaDriver {

    private static final String ECAL_DIR = "/subdet/ecal";
    private static final String TRACKER_DIR = "/subdet/tracker";
    private static final String PERF_DIR = "/perf";

    private IHistogram1D tracksPerEventH1D;
    private IHistogram1D rawHitsPerTrackH1D;
    private IHistogram2D ecalReadoutHitsXYH2D;
    private IHistogram1D ecalReadoutHitsPerEventH1D;
    private IHistogram1D adcValuesH1D;
    private IHistogram1D clustersPerEventH1D;
    private IHistogram1D eventCountH1D;
    private ICloud1D rawTrackerHitsPerEventC1D;
    private ICloud1D chi2C1D;
    private ICloud1D pxC1D;
    private ICloud1D pyC1D;
    private ICloud1D pzC1D;
    private ICloud1D fittedTrackerHitsPerEventC1D;

    private static final String CLUSTER_COLLECTION_NAME = "EcalClusters";
    private static final String RAW_TRACKER_HIT_COLLECTION_NAME = "SVTRawTrackerHits";
    private static final String FITTED_HITS_COLLECTION_NAME = "SVTFittedRawTrackerHits";
    private static final String TRACK_COLLECTION_NAME = "MatchedTracks";
    private static final String ECAL_READOUT_HITS_COLLECTION_NAME = "EcalReadoutHits";

    // Convert clouds to histograms after this many entries (-1 means do not convert automatically)
    private static final int CLOUD_MAX = -1;

    private ICloud1D createCloud1D(String name) {
        return hf.createCloud1D(name, name, CLOUD_MAX, "autoconvert=false");
    }

    public void startOfData() {

        tree.mkdirs(TRACKER_DIR);
        tree.cd(TRACKER_DIR);

        tracksPerEventH1D = aida.histogram1D("Tracks Per Event", 20, 0., 20.);
        tracksPerEventH1D.annotation().addItem("xAxisLabel", "Tracks / Event");
        tracksPerEventH1D.annotation().addItem("yAxisLabel", "Count");

        rawHitsPerTrackH1D = aida.histogram1D("Raw Hits Per Track", 10, 0., 10);
        rawHitsPerTrackH1D.annotation().addItem("xAxisLabel", "Hits / Track");
        rawHitsPerTrackH1D.annotation().addItem("yAxisLabel", "Count");

        rawTrackerHitsPerEventC1D = hf.createCloud1D("Raw Tracker Hits Per Event");
        chi2C1D = createCloud1D("chi2");
        pxC1D = createCloud1D("PX");
        pyC1D = createCloud1D("PY");
        pzC1D = createCloud1D("PZ");
        fittedTrackerHitsPerEventC1D = createCloud1D("Fitted Tracker Hits Per Event");

        tree.mkdirs(ECAL_DIR);
        tree.cd(ECAL_DIR);

        ecalReadoutHitsXYH2D = aida.histogram2D("Readout Hits XY", 47, -23.5, 23.5, 11, -5.5, 5.5);
        ecalReadoutHitsXYH2D.annotation().addItem("xAxisLabel", "X Index");
        ecalReadoutHitsXYH2D.annotation().addItem("yAxisLabel", "Y Index");

        ecalReadoutHitsPerEventH1D = aida.histogram1D("Readout Hits Per Event ", 50, 0, 50.);

        clustersPerEventH1D = aida.histogram1D("Clusters Per Event", 10, -0.5, 9.5);

        adcValuesH1D = aida.histogram1D("Readout Hit ADC Values", 300, 0, 300.);

        tree.mkdir(PERF_DIR);
        tree.cd(PERF_DIR);
        eventCountH1D = aida.histogram1D("Event Count", 1, 0., 1.0);

        super.startOfData();
    }

    public void process(EventHeader event) {

        eventCountH1D.fill(0.5);

        List<Track> tracks = event.get(Track.class, TRACK_COLLECTION_NAME);

        aida.tree().cd(TRACKER_DIR);

        tracksPerEventH1D.fill(tracks.size());

        for (Track track : tracks) {
            chi2C1D.fill(track.getChi2());
            List<TrackState> trackStates = track.getTrackStates();
            TrackState ts = trackStates.get(0);
            if (ts != null) {
                final double p[] = ts.getMomentum();
                pxC1D.fill(p[0]);
                pyC1D.fill(p[1]);
                pzC1D.fill(p[2]);
            }
            this.rawHitsPerTrackH1D.fill(track.getTrackerHits().size());
        }

        rawTrackerHitsPerEventC1D.fill(
                event.get(RawTrackerHit.class, RAW_TRACKER_HIT_COLLECTION_NAME).size());

        fittedTrackerHitsPerEventC1D.fill(
                event.get(LCRelation.class, FITTED_HITS_COLLECTION_NAME).size());

        List<RawTrackerHit> ecalHits = event.get(RawTrackerHit.class, ECAL_READOUT_HITS_COLLECTION_NAME);
        ecalReadoutHitsPerEventH1D.fill(ecalHits.size());
        for (RawTrackerHit hit : ecalHits) {
            int column = hit.getIdentifierFieldValue("ix");
            int row = hit.getIdentifierFieldValue("iy");
            ecalReadoutHitsXYH2D.fill(column, row);
            for (short adcValue : hit.getADCValues()) {
                adcValuesH1D.fill(adcValue);
            }
        }

        List<Cluster> clusterList = event.get(Cluster.class, CLUSTER_COLLECTION_NAME);
        clustersPerEventH1D.fill(clusterList.size());
    }
}
