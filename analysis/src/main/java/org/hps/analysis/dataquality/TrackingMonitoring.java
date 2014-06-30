package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import java.util.List;
import java.util.Map;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCIOParameters;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;

/**
 * DQM driver for reconstructed track quantities
 * plots things like number of tracks/event, chi^2, track parameters
 * (d0/z0/theta/phi/curvature)
 *
 * @author mgraham on Mar 28, 2014
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
    int nEvents = 0;
    int nTotTracks = 0;
    int nTotHits = 0;
    double sumd0 = 0;
    double sumz0 = 0;
    double sumslope = 0;
    double sumchisq = 0;
     private String plotDir = "Tracks/";
    String[] trackingQuantNames = {"avg_N_tracks", "avg_N_hitsPerTrack", "avg_d0", "avg_z0", "avg_absslope", "avg_chi2"};

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

        IHistogram1D trkChi2 = aida.histogram1D(plotDir+"Track Chi2", 25, 0, 25.0);
        IHistogram1D nTracks = aida.histogram1D(plotDir+"Tracks per Event", 6, 0, 6);
        IHistogram1D trkd0 = aida.histogram1D(plotDir+"d0 ", 25, -5.0, 5.0);
        IHistogram1D trkphi = aida.histogram1D(plotDir+"sinphi ", 25, -0.2, 0.2);
        IHistogram1D trkomega = aida.histogram1D(plotDir+"omega ", 25, -0.00025, 0.00025);
        IHistogram1D trklam = aida.histogram1D(plotDir+"tan(lambda) ", 25, -0.1, 0.1);
        IHistogram1D trkz0 = aida.histogram1D(plotDir+"z0 ", 25, -1.0, 1.0);
        IHistogram1D nHits = aida.histogram1D(plotDir+"Hits per Track", 2, 5, 7);

    }

    @Override
    public void process(EventHeader event) {

        aida.tree().cd("/");
                   
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            aida.histogram1D(plotDir+"Tracks per Event").fill(0);
            return;
        }
        nEvents++;
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        nTotTracks += tracks.size();
        aida.histogram1D(plotDir+"Tracks per Event").fill(tracks.size());
        for (Track trk : tracks) {
            nTotHits += trk.getTrackerHits().size();
            aida.histogram1D(plotDir+"Track Chi2").fill(trk.getChi2());
            aida.histogram1D(plotDir+"Hits per Track").fill(trk.getTrackerHits().size());
            //why is getTrackParameter depricated?  How am  I supposed to get this? 
            aida.histogram1D(plotDir+"d0 ").fill(trk.getTrackParameter(LCIOParameters.ParameterName.d0.ordinal()));
            aida.histogram1D(plotDir+"sinphi ").fill(Math.sin(trk.getTrackParameter(LCIOParameters.ParameterName.phi0.ordinal())));
            aida.histogram1D(plotDir+"omega ").fill(trk.getTrackParameter(LCIOParameters.ParameterName.omega.ordinal()));
            aida.histogram1D(plotDir+"tan(lambda) ").fill(trk.getTrackParameter(LCIOParameters.ParameterName.tanLambda.ordinal()));
            aida.histogram1D(plotDir+"z0 ").fill(trk.getTrackParameter(LCIOParameters.ParameterName.z0.ordinal()));
            sumd0 += trk.getTrackParameter(LCIOParameters.ParameterName.d0.ordinal());
            sumz0 += trk.getTrackParameter(LCIOParameters.ParameterName.z0.ordinal());
            sumslope += Math.abs(trk.getTrackParameter(LCIOParameters.ParameterName.tanLambda.ordinal()));
            sumchisq += trk.getChi2();
        }
    }

    @Override
    public void calculateEndOfRunQuantities() {
        monitoredQuantityMap.put(trackingQuantNames[0], (double) nTotTracks / nEvents);
        monitoredQuantityMap.put(trackingQuantNames[1], (double) nTotHits / nTotTracks);
        monitoredQuantityMap.put(trackingQuantNames[2], sumd0 / nTotTracks);
        monitoredQuantityMap.put(trackingQuantNames[3], sumz0 / nTotTracks);
        monitoredQuantityMap.put(trackingQuantNames[4], sumslope / nTotTracks);
        monitoredQuantityMap.put(trackingQuantNames[5], sumchisq / nTotTracks);
    }


    @Override
    public void printDQMData() {
        System.out.println("ReconMonitoring::printDQMData");
        for (Map.Entry<String, Double> entry : monitoredQuantityMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        System.out.println("*******************************");
    }

    @Override
    public void printDQMStrings() {
         for (Map.Entry<String, Double> entry : monitoredQuantityMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD "+entry.getKey()+" double;");

    }

}
