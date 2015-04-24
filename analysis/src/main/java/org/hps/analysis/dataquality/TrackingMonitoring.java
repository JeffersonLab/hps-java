package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;

/**
 * DQM driver for reconstructed track quantities plots things like number of
 * tracks/event, chi^2, track parameters (d0/z0/theta/phi/curvature)
 *
 * @author mgraham on Mar 28, 2014
 */
// TODO:  Add some quantities for DQM monitoring:  e.g. <tracks>, <hits/track>, etc
public class TrackingMonitoring extends DataQualityMonitor {

    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private final String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private final String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private final String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String trackCollectionName = "MatchedTracks";
    private final String trackerName = "Tracker";
    private static final String nameStrip = "Tracker_TestRunModule_";
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    private Detector detector = null;
    private List<HpsSiSensor> sensors;

    IDDecoder dec;
    int nEvents = 0;
    int nTotTracks = 0;
    int nTotHits = 0;
    double sumd0 = 0;
    double sumz0 = 0;
    double sumslope = 0;
    double sumchisq = 0;
    private final String plotDir = "Tracks/";
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

        IHistogram1D trkChi2 = aida.histogram1D(plotDir + "Track Chi2", 25, 0, 25.0);
        IHistogram1D nTracks = aida.histogram1D(plotDir + "Tracks per Event", 6, 0, 6);
        IHistogram1D trkd0 = aida.histogram1D(plotDir + "d0 ", 25, -5.0, 5.0);
        IHistogram1D trkphi = aida.histogram1D(plotDir + "sinphi ", 25, -0.2, 0.2);
        IHistogram1D trkomega = aida.histogram1D(plotDir + "omega ", 25, -0.00025, 0.00025);
        IHistogram1D trklam = aida.histogram1D(plotDir + "tan(lambda) ", 25, -0.1, 0.1);
        IHistogram1D trkz0 = aida.histogram1D(plotDir + "z0 ", 25, -1.0, 1.0);
        IHistogram1D nHits = aida.histogram1D(plotDir + "Hits per Track", 2, 5, 7);
        IHistogram1D trackMeanTime = aida.histogram1D(plotDir + "Mean time of hits on track", 400, -100., 100.);
        IHistogram1D trackRMSTime = aida.histogram1D(plotDir + "RMS time of hits on track", 200, 0., 15.);
        IHistogram2D trackChi2RMSTime = aida.histogram2D(plotDir + "Track chi2 vs. RMS time of hits", 200, 0., 15., 25, 0, 25.0);
        IHistogram1D seedRMSTime = aida.histogram1D(plotDir + "RMS time of hits on seed layers", 200, 0., 15.);

        // Make a list of SiSensors in the SVT.
        sensors = this.detector.getSubdetector(trackerName).getDetectorElement().findDescendants(HpsSiSensor.class);

        // Setup the occupancy plots.
        aida.tree().cd("/");
        for (HpsSiSensor sensor : sensors) {
            //IHistogram1D occupancyPlot = aida.histogram1D(sensor.getName().replaceAll("Tracker_TestRunModule_", ""), 640, 0, 639);
            IHistogram1D hitTimeResidual = createSensorPlot(plotDir + "hitTimeResidual_", sensor, 100, -20, 20);
        }

    }

    @Override
    public void process(EventHeader event) {

        aida.tree().cd("/");

        RelationalTable hittostrip = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, helicalTrackHitRelationsCollectionName);
        for (LCRelation relation : hitrelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hittostrip.add(relation.getFrom(), relation.getTo());
            }
        }

        RelationalTable hittorotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> rotaterelations = event.get(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName);
        for (LCRelation relation : rotaterelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hittorotated.add(relation.getFrom(), relation.getTo());
            }
        }

        if (!event.hasCollection(Track.class, trackCollectionName)) {
            aida.histogram1D(plotDir + "Tracks per Event").fill(0);
            return;
        }
        nEvents++;
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        nTotTracks += tracks.size();
        aida.histogram1D(plotDir + "Tracks per Event").fill(tracks.size());
        for (Track trk : tracks) {
            nTotHits += trk.getTrackerHits().size();
            aida.histogram1D(plotDir + "Track Chi2").fill(trk.getChi2());
            aida.histogram1D(plotDir + "Hits per Track").fill(trk.getTrackerHits().size());
            aida.histogram1D(plotDir + "d0 ").fill(trk.getTrackStates().get(0).getD0());
            aida.histogram1D(plotDir + "sinphi ").fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
            aida.histogram1D(plotDir + "omega ").fill(trk.getTrackStates().get(0).getOmega());
            aida.histogram1D(plotDir + "tan(lambda) ").fill(trk.getTrackStates().get(0).getTanLambda());
            aida.histogram1D(plotDir + "z0 ").fill(trk.getTrackStates().get(0).getZ0());
            sumd0 += trk.getTrackStates().get(0).getD0();
            sumz0 += trk.getTrackStates().get(0).getZ0();
            sumslope += Math.abs(trk.getTrackStates().get(0).getTanLambda());
            sumchisq += trk.getChi2();

            int nStrips = 0;
            int nSeedStrips = 0;
            double meanTime = 0;
            double meanSeedTime = 0;
            for (TrackerHit hit : trk.getTrackerHits()) {
                Collection<TrackerHit> htsList = hittostrip.allFrom(hittorotated.from(hit));
                for (TrackerHit hts : htsList) {
                    nStrips++;
                    meanTime += hts.getTime();
                    int layer = ((HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
                    if (layer <= 6) {
                        nSeedStrips++;
                        meanSeedTime += hts.getTime();
                    }
                }
            }
            meanTime /= nStrips;
            meanSeedTime /= nSeedStrips;

            double rmsTime = 0;
            double rmsSeedTime = 0;
            for (TrackerHit hit : trk.getTrackerHits()) {
                Collection<TrackerHit> htsList = hittostrip.allFrom(hittorotated.from(hit));
                for (TrackerHit hts : htsList) {
                    rmsTime += Math.pow(hts.getTime() - meanTime, 2);
                    HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement();
                    int layer = sensor.getLayerNumber();
                    if (layer <= 6) {
                        rmsSeedTime += Math.pow(hts.getTime() - meanSeedTime, 2);
                    }
                    String sensorName = getNiceSensorName(sensor);
                    getSensorPlot(plotDir + "hitTimeResidual_", sensorName).fill(hts.getTime() - meanTime);
                }
            }
            rmsTime = Math.sqrt(rmsTime / nStrips);
            aida.histogram1D(plotDir + "Mean time of hits on track").fill(meanTime);
            aida.histogram1D(plotDir + "RMS time of hits on track").fill(rmsTime);
            aida.histogram2D(plotDir + "Track chi2 vs. RMS time of hits").fill(rmsTime, trk.getChi2());

            rmsSeedTime = Math.sqrt(rmsSeedTime / nSeedStrips);
            aida.histogram1D(plotDir + "RMS time of hits on seed layers").fill(rmsSeedTime);
//            System.out.format("%d seed strips, RMS time %f\n", nSeedStrips, rmsSeedTime);

//            System.out.format("%d strips, mean time %f, RMS time %f\n", nStrips, meanTime, rmsTime);
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
        for (Map.Entry<String, Double> entry : monitoredQuantityMap.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        System.out.println("*******************************");
    }

    @Override
    public void printDQMStrings() {
        for (Map.Entry<String, Double> entry : monitoredQuantityMap.entrySet()) {
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        }
    }

    private IHistogram1D getSensorPlot(String prefix, HpsSiSensor sensor) {
        String hname = prefix + getNiceSensorName(sensor);
        return aida.histogram1D(hname);
    }

    private IHistogram1D getSensorPlot(String prefix, String sensorName) {
        return aida.histogram1D(prefix + sensorName);
    }

    private IHistogram1D createSensorPlot(String prefix, HpsSiSensor sensor, int nchan, double min, double max) {
        String hname = prefix + getNiceSensorName(sensor);
        IHistogram1D hist = aida.histogram1D(hname, nchan, min, max);
        hist.setTitle(sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens"));

        return hist;
    }

    private String getNiceSensorName(HpsSiSensor sensor) {
        return sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens");
    }

}
