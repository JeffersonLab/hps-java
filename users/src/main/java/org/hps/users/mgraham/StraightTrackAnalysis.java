package org.hps.users.mgraham;

import hep.aida.IHistogram1D;
import java.util.List;
import org.hps.recon.tracking.nobfield.StraightTrack;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Analysis driver for testing straight track (i.e. 0 b-field) development
 *
 * @author mgraham
 */
public class StraightTrackAnalysis extends Driver {

    protected AIDA aida = AIDA.defaultInstance();
    private String mcSvtHitsName = "TrackerHits";
    private String rawHitsName = "RawTrackerHitMaker_RawTrackerHits";
    private String clustersName = "StripClusterer_SiTrackerHitStrip1D";
    private String hthName = "HelicalTrackHits";
    private String tracksName = "StraightTracks";

    int nevents = 0;

    public void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        IHistogram1D nSimHits = aida.histogram1D("Number of SVT Sim Hits", 25, 0, 25.0);
        IHistogram1D nRawHits = aida.histogram1D("Number of Raw Hits", 25, 0, 25.0);
        IHistogram1D nClusters = aida.histogram1D("Number of 1D Clusters", 25, 0, 25.0);
        IHistogram1D nHTH = aida.histogram1D("Number of HelicalTrackHits", 25, 0, 25.0);
        IHistogram1D nLayers = aida.histogram1D("Number of Layers Hit", 7, 0, 7);
        IHistogram1D nTracks = aida.histogram1D("Number of Tracks found", 5, 0, 5);
    }

    public void process(EventHeader event) {
        nevents++;
        if (!event.hasCollection(SimTrackerHit.class, mcSvtHitsName))
            return;
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, mcSvtHitsName);
        aida.histogram1D("Number of SVT Sim Hits").fill(simHits.size());
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawHitsName);
        List<TrackerHit> clusters = event.get(TrackerHit.class, clustersName);
        List<TrackerHit> hths = event.get(TrackerHit.class, hthName);
        aida.histogram1D("Number of Raw Hits").fill(rawHits.size());
        aida.histogram1D("Number of 1D Clusters").fill(clusters.size());
        aida.histogram1D("Number of HelicalTrackHits").fill(hths.size());
        int[] hitInLayer = {0, 0, 0, 0, 0, 0};
        for (TrackerHit hit : hths) {
            int layer = ((RawTrackerHit) (hit.getRawHits().get(0))).getLayerNumber();
            if (layer == 1 || layer == 2)
                hitInLayer[0] = 1;
            if (layer == 3 || layer == 4)
                hitInLayer[1] = 1;
            if (layer == 5 || layer == 6)
                hitInLayer[2] = 1;
            if (layer == 7 || layer == 8)
                hitInLayer[3] = 1;
            if (layer == 9 || layer == 10)
                hitInLayer[4] = 1;
            if (layer == 11 || layer == 12)
                hitInLayer[5] = 1;
        }
        int totLayers = 0;
        for (int i = 0; i < 6; i++)
            totLayers += hitInLayer[i];
        aida.histogram1D("Number of Layers Hit").fill(totLayers);
        List<Track> tracks = event.get(Track.class, tracksName);
        aida.histogram1D("Number of Tracks found").fill(tracks.size());
        for (Track trk : tracks) {
            StraightTrack stght = (StraightTrack) trk;
            aida.histogram1D("x0", 50, -2, 2).fill(stght.getTrackParameters()[0]);
            aida.histogram1D("y0", 50, -2, 2).fill(stght.getTrackParameters()[2]);
            aida.histogram1D("xz slope", 50, -0.2, 0.25).fill(stght.getTrackParameters()[1]);
            aida.histogram1D("yz slope", 50, -0.25, 0.25).fill(stght.getTrackParameters()[3]);
            aida.histogram1D("track chi2 per ndf", 50, 0, 10).fill(stght.getChi2()/stght.getNDF());
            aida.histogram1D("track nhits", 50, 0, 10).fill(stght.getTrackerHits().size());

        }
    }

}
