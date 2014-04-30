package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.aida.IProfile;
import java.util.List;
import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCIOParameters;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;

/**
 *  DQM driver for the monte carlo for reconstructed track quantities
 *  plots things like number of tracks/event, momentum, chi^2, track parameters (d0/z0/theta/phi/curvature)
 *  @author mgraham on Mar 28, 2014
 */
// TODO:  Add some quantities for DQM monitoring:  e.g. <tracks>, <hits/track>, etc
public class TrackingMonitoring extends DataQualityMonitor {

    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String trackCollectionName = "MatchedTracks";
    private String trackerName = "Tracker";
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    private Detector detector = null;
    IDDecoder dec;

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        this.detector = detector;
        aida.tree().cd("/");

        IProfile avgLayersTopPlot = aida.profile1D("Number of Stereo Hits per layer in Top Half", 6, 1, 13);
        IProfile avgLayersBottomPlot = aida.profile1D("Number of Stereo Hits per layer in Bottom Half", 6, 1, 13);

        IHistogram1D trkPx = aida.histogram1D("Track Momentum (Px)", 25, -0.1, 0.200);
        IHistogram1D trkPy = aida.histogram1D("Track Momentum (Py)", 25, -0.1, 0.1);
        IHistogram1D trkPz = aida.histogram1D("Track Momentum (Pz)", 25, 0, 2.4);
        IHistogram1D trkChi2 = aida.histogram1D("Track Chi2", 25, 0, 25.0);
        IHistogram1D nTracks = aida.histogram1D("Tracks per Event", 6, 0, 6);
        IHistogram1D trkd0 = aida.histogram1D("d0 ", 25, -5.0, 5.0);
        IHistogram1D trkphi = aida.histogram1D("sinphi ", 25, -0.2, 0.2);
        IHistogram1D trkomega = aida.histogram1D("omega ", 25, -0.00025, 0.00025);
        IHistogram1D trklam = aida.histogram1D("tan(lambda) ", 25, -0.1, 0.1);
        IHistogram1D trkz0 = aida.histogram1D("z0 ", 25, -1.0, 1.0);
        IHistogram1D nHits = aida.histogram1D("Hits per Track", 2, 5, 7);

    }

    @Override
    public void process(EventHeader event) {

        aida.tree().cd("/");
        if (!event.hasCollection(HelicalTrackHit.class, helicalTrackHitCollectionName))
            return;
   
        List<HelicalTrackHit> hthList = event.get(HelicalTrackHit.class, helicalTrackHitCollectionName);
        int[] layersTop = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0,0};
        int[] layersBot = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0,0};
        for (HelicalTrackHit hth : hthList) {
            HelicalTrackCross htc = (HelicalTrackCross) hth;
            double x = htc.getPosition()[0];
            double y = htc.getPosition()[1];
            SiSensor sensor = ((SiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement());
            if (SvtUtils.getInstance().isTopLayer(sensor))
                layersTop[htc.Layer() - 1]++;
            else
                layersBot[htc.Layer() - 1]++;
        }
        for (int i = 0; i < 12; i++) {
            aida.profile1D("Number of Stereo Hits per layer in Top Half").fill(i + 1, layersTop[i]);
            aida.profile1D("Number of Stereo Hits per layer in Bottom Half").fill(i + 1, layersBot[i]);
        }

        if (!event.hasCollection(Track.class, trackCollectionName)) {
//            System.out.println(trackCollectionName + " does not exist; skipping event");
            aida.histogram1D("Tracks per Event").fill(0);
            return;
        }

        List<Track> tracks = event.get(Track.class, trackCollectionName);
        aida.histogram1D("Tracks per Event").fill(tracks.size());
        for (Track trk : tracks) {

            aida.histogram1D("Track Momentum (Px)").fill(trk.getPY());
            aida.histogram1D("Track Momentum (Py)").fill(trk.getPZ());
            aida.histogram1D("Track Momentum (Pz)").fill(trk.getPX());
            aida.histogram1D("Track Chi2").fill(trk.getChi2());

            aida.histogram1D("Hits per Track").fill(trk.getTrackerHits().size());

            aida.histogram1D("d0 ").fill(trk.getTrackParameter(LCIOParameters.ParameterName.d0.ordinal()));
            aida.histogram1D("sinphi ").fill(Math.sin(trk.getTrackParameter(LCIOParameters.ParameterName.phi0.ordinal())));
            aida.histogram1D("omega ").fill(trk.getTrackParameter(LCIOParameters.ParameterName.omega.ordinal()));
            aida.histogram1D("tan(lambda) ").fill(trk.getTrackParameter(LCIOParameters.ParameterName.tanLambda.ordinal()));
            aida.histogram1D("z0 ").fill(trk.getTrackParameter(LCIOParameters.ParameterName.z0.ordinal()));
        }
    }

    @Override
    public void fillEndOfRunPlots() {
    }

    @Override
    public void dumpDQMData() {
    }

}
