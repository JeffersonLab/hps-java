package org.hps.online.recon.example;

import java.util.List;

import org.hps.online.recon.RemoteAidaDriver;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

/**
 * Create example 1D and 2D histograms in a remote AIDA tree
 */
public class HistExampleDriver extends RemoteAidaDriver {

    private static final String ECAL_DIR = "/subdet/ecal";
    private static final String TRACKER_DIR = "/subdet/tracker";
    private IHistogram1D tracks = null;
    private IHistogram2D ecalHits = null;

    public void startOfData() {
        tree.mkdirs(TRACKER_DIR);
        tree.cd(TRACKER_DIR);
        tracks = aida.histogram1D("Tracks", 20, 0., 20.);
        tracks.annotation().addItem("xAxisLabel", "Tracks / Event");
        tracks.annotation().addItem("yAxisLabel", "Count");

        tree.mkdirs(ECAL_DIR);
        tree.cd(ECAL_DIR);
        ecalHits = aida.histogram2D("Ecal Hits", 47, -23.5, 23.5, 11, -5.5, 5.5);
        ecalHits.annotation().addItem("xAxisLabel", "X Index");
        ecalHits.annotation().addItem("yAxisLabel", "Y Index");

        super.startOfData();
    }

    public void process(EventHeader event) {
        tracks.fill(event.get(Track.class, "MatchedTracks").size());

        List<RawTrackerHit> hits = event.get(RawTrackerHit.class, "EcalReadoutHits");
        for (RawTrackerHit hit : hits) {
            int column = hit.getIdentifierFieldValue("ix");
            int row = hit.getIdentifierFieldValue("iy");
            ecalHits.fill(column, row);
        }
    }
}
