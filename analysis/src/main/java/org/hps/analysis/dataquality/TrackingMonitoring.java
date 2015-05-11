package org.hps.analysis.dataquality;

import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.TIData;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.aida.AIDA;

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
    private final String positronDir = "Positrons/";
    private final String electronDir = "Electrons/";
    private final String topDir = "Top/";
    private final String botDir = "Bottom/";
    private final String hthplotDir = "HelicalTrackHits/";
    String[] trackingQuantNames = {"avg_N_tracks", "avg_N_hitsPerTrack", "avg_d0", "avg_z0", "avg_absslope", "avg_chi2"};
    int nmodules = 6;
    IHistogram1D[] hthTop = new IHistogram1D[nmodules];
    IHistogram1D[] hthBot = new IHistogram1D[nmodules];
    IHistogram2D[] xvsyTop = new IHistogram2D[nmodules];
    IHistogram2D[] xvsyBot = new IHistogram2D[nmodules];

    IHistogram1D trkYAtECALTop;
    IHistogram1D trkYAtECALBot;

    IHistogram1D trkChi2Pos;
    IHistogram1D trkChi2Ele;
    IHistogram1D trkChi2Top;
    IHistogram1D trkChi2Bot;

    IHistogram1D nTracksPos;
    IHistogram1D nTracksEle;
    IHistogram1D nTracksTop;
    IHistogram1D nTracksBot;

    IHistogram1D trkd0Pos;
    IHistogram1D trkd0Ele;
    IHistogram1D trkd0Top;
    IHistogram1D trkd0Bot;

    IHistogram1D trkphiPos;
    IHistogram1D trkphiEle;
    IHistogram1D trkphiTop;
    IHistogram1D trkphiBot;

    IHistogram1D trkomegaPos;
    IHistogram1D trkomegaEle;
    IHistogram1D trkomegaTop;
    IHistogram1D trkomegaBot;

    IHistogram1D trklamPos;
    IHistogram1D trklamEle;
    IHistogram1D trklamTop;
    IHistogram1D trklamBot;

    IHistogram1D trkz0Pos;
    IHistogram1D trkz0Ele;
    IHistogram1D trkz0Top;
    IHistogram1D trkz0Bot;

    IHistogram1D nHitsPos;
    IHistogram1D nHitsEle;
    IHistogram1D nHitsTop;
    IHistogram1D nHitsBot;

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

        IHistogram1D trkChi2 = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track Chi2", 25, 0, 25.0);
        IHistogram1D nTracks = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Tracks per Event", 6, 0, 6);
        IHistogram1D trkd0 = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "d0 ", 25, -5.0, 5.0);
        IHistogram1D trkphi = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "sinphi ", 25, -0.2, 0.2);
        IHistogram1D trkomega = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "omega ", 25, -0.0005, 0.0005);
        IHistogram1D trklam = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "tan(lambda) ", 25, -0.1, 0.1);
        IHistogram1D trkz0 = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "z0 ", 25, -1.0, 1.0);
        IHistogram1D nHits = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Hits per Track", 4, 3, 7);
        IHistogram1D trackMeanTime = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Mean time of hits on track", 100, -10., 100.);
        IHistogram1D trackRMSTime = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "RMS time of hits on track", 100, 0., 15.);
        IHistogram2D trackChi2RMSTime = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track chi2 vs. RMS time of hits", 100, 0., 15., 25, 0, 25.0);
        IHistogram1D seedRMSTime = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "RMS time of hits on seed layers", 100, 0., 15.);
        trkYAtECALTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track Y at ECAL: Top", 100, 0, 100);
        trkYAtECALBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track Y at ECAL: Bot", 100, 0, 100);
        for (int i = 1; i <= nmodules; i++) {
            xvsyTop[i - 1] = aida.histogram2D(hthplotDir + "Module " + i + " Top", 50, -100, 100, 50, 0, 40);
            xvsyBot[i - 1] = aida.histogram2D(hthplotDir + "Module " + i + " Bottom", 50, -100, 100, 50, 0, 40);
            hthTop[i - 1] = aida.histogram1D(hthplotDir + "Module " + i + "Top: Track Hits", 25, 0, 25);
            hthBot[i - 1] = aida.histogram1D(hthplotDir + "Module " + i + "Bot: Track Hits", 25, 0, 25);
        }

        trkChi2Pos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "Track Chi2", 25, 0, 25.0);
        nTracksPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "Tracks per Event", 6, 0, 6);
        trkd0Pos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "d0 ", 25, -5.0, 5.0);
        trkphiPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "sinphi ", 25, -0.2, 0.2);
        trkomegaPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "omega ", 25, -0.0005, 0.0005);
        trklamPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "tan(lambda) ", 25, -0.1, 0.1);
        trkz0Pos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "z0 ", 25, -1.0, 1.0);
        nHitsPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "Hits per Track", 4, 3, 7);

        trkChi2Ele = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "Track Chi2", 25, 0, 25.0);
        nTracksEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "Tracks per Event", 6, 0, 6);
        trkd0Ele = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "d0 ", 25, -5.0, 5.0);
        trkphiEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "sinphi ", 25, -0.2, 0.2);
        trkomegaEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "omega ", 25, -0.0005, 0.0005);
        trklamEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "tan(lambda) ", 25, -0.1, 0.1);
        trkz0Ele = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "z0 ", 25, -1.0, 1.0);
        nHitsEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "Hits per Track", 4, 3, 7);

        trkChi2Top = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "Track Chi2", 25, 0, 25.0);
        nTracksTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "Tracks per Event", 6, 0, 6);
        trkd0Top = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "d0 ", 25, -5.0, 5.0);
        trkphiTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "sinphi ", 25, -0.2, 0.2);
        trkomegaTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "omega ", 25, -0.0005, 0.0005);
        trklamTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "tan(lambda) ", 50, 0.0, 0.1);
        trkz0Top = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "z0 ", 25, -1.0, 1.0);
        nHitsTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "Hits per Track", 4, 3, 7);

        trkChi2Bot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "Track Chi2", 25, 0, 25.0);
        nTracksBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "Tracks per Event", 6, 0, 6);
        trkd0Bot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "d0 ", 25, -5.0, 5.0);
        trkphiBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "sinphi ", 25, -0.2, 0.2);
        trkomegaBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "omega ", 25, -0.0005, 0.0005);
        trklamBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "tan(lambda) ", 50, 0, 0.1);
        trkz0Bot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "z0 ", 25, -1.0, 1.0);
        nHitsBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "Hits per Track", 4, 3, 7);

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

        if (!event.hasCollection(LCRelation.class, helicalTrackHitRelationsCollectionName) || !event.hasCollection(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName))
            return;
        RelationalTable hittostrip = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, helicalTrackHitRelationsCollectionName);
        for (LCRelation relation : hitrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittostrip.add(relation.getFrom(), relation.getTo());

        RelationalTable hittorotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> rotaterelations = event.get(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName);
        for (LCRelation relation : rotaterelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittorotated.add(relation.getFrom(), relation.getTo());

        if (!event.hasCollection(TrackerHit.class, helicalTrackHitCollectionName))
            return;

        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            for (GenericObject data : triggerList)
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    TIData triggerData = new TIData(data);
                    if (!matchTriggerType(triggerData))//only process singles0 triggers...

                        return;
                }
        } else if (debug)
            System.out.println(this.getClass().getSimpleName() + ":  No trigger bank found...running over all trigger types");
        /*  This doesn't work on reco'ed files...fix me!
         int[] topHits = {0, 0, 0, 0, 0, 0};
         int[] botHits = {0, 0, 0, 0, 0, 0};
         List<TrackerHit> hth = event.get(TrackerHit.class, helicalTrackHitCollectionName);
         for (TrackerHit hit : hth) {           
         int module = -99;
         int layer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();
         if (layer < 2) {
         module = 1;
         } else if (layer < 4) {
         module = 2;
         } else if (layer < 6) {
         module = 3;
         } else if (layer < 8) {
         module = 4;
         } else if (layer < 10) {
         module = 5;
         } else {
         module = 6;
         }

         if (hit.getPosition()[1] > 0) {
         topHits[module - 1]++;
         xvsyTop[module - 1].fill(hit.getPosition()[0], hit.getPosition()[1]);
         } else {
         botHits[module - 1]++;
         xvsyBot[module - 1].fill(hit.getPosition()[0], Math.abs(hit.getPosition()[1]));
         }
         }

         for (int i = 0; i < nmodules; i++) {
         hthTop[i].fill(topHits[i]);
         hthBot[i].fill(botHits[i]);
         }
         */
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Tracks per Event").fill(0);
            return;
        }
        nEvents++;
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        nTotTracks += tracks.size();
        aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Tracks per Event").fill(tracks.size());
        int cntEle = 0;
        int cntPos = 0;
        int cntTop = 0;
        int cntBot = 0;
        double ecalFace = 1393.0;//mm
        for (Track trk : tracks) {
            Hep3Vector trackPosAtEcalFace = TrackUtils.extrapolateTrack(trk, ecalFace);
            double yAtECal = trackPosAtEcalFace.y();
            if (yAtECal > 0)
                trkYAtECALTop.fill(yAtECal);
            else
                trkYAtECALBot.fill(Math.abs(yAtECal));
            nTotHits += trk.getTrackerHits().size();
            aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track Chi2").fill(trk.getChi2());
            aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Hits per Track").fill(trk.getTrackerHits().size());
            aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "d0 ").fill(trk.getTrackStates().get(0).getD0());
            aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "sinphi ").fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
            aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "omega ").fill(trk.getTrackStates().get(0).getOmega());
            aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "tan(lambda) ").fill(trk.getTrackStates().get(0).getTanLambda());
            aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "z0 ").fill(trk.getTrackStates().get(0).getZ0());
            if (trk.getTrackStates().get(0).getOmega() < 0) {//positrons
                trkChi2Pos.fill(trk.getChi2());
                nHitsPos.fill(trk.getTrackerHits().size());
                trkd0Pos.fill(trk.getTrackStates().get(0).getD0());
                trkphiPos.fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
                trkomegaPos.fill(trk.getTrackStates().get(0).getOmega());
                trklamPos.fill(trk.getTrackStates().get(0).getTanLambda());
                trkz0Pos.fill(trk.getTrackStates().get(0).getZ0());
                cntPos++;
            } else {
                trkChi2Ele.fill(trk.getChi2());
                nHitsEle.fill(trk.getTrackerHits().size());
                trkd0Ele.fill(trk.getTrackStates().get(0).getD0());
                trkphiEle.fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
                trkomegaEle.fill(trk.getTrackStates().get(0).getOmega());
                trklamEle.fill(trk.getTrackStates().get(0).getTanLambda());
                trkz0Ele.fill(trk.getTrackStates().get(0).getZ0());
                cntEle++;
            }

            if (trk.getTrackStates().get(0).getTanLambda() < 0) {
                trkChi2Bot.fill(trk.getChi2());
                nHitsBot.fill(trk.getTrackerHits().size());
                trkd0Bot.fill(trk.getTrackStates().get(0).getD0());
                trkphiBot.fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
                trkomegaBot.fill(trk.getTrackStates().get(0).getOmega());
                trklamBot.fill(Math.abs(trk.getTrackStates().get(0).getTanLambda()));
                trkz0Bot.fill(trk.getTrackStates().get(0).getZ0());
                cntBot++;
            } else {
                trkChi2Top.fill(trk.getChi2());
                nHitsTop.fill(trk.getTrackerHits().size());
                trkd0Top.fill(trk.getTrackStates().get(0).getD0());
                trkphiTop.fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
                trkomegaTop.fill(trk.getTrackStates().get(0).getOmega());
                trklamTop.fill(Math.abs(trk.getTrackStates().get(0).getTanLambda()));
                trkz0Top.fill(trk.getTrackStates().get(0).getZ0());
                cntTop++;
            }

            sumd0 += trk.getTrackStates().get(0).getD0();
            sumz0 += trk.getTrackStates().get(0).getZ0();
            sumslope += Math.abs(trk.getTrackStates().get(0).getTanLambda());
            sumchisq += trk.getChi2();
//below does not work on recon'ed files            
//
//            int nStrips = 0;
//            int nSeedStrips = 0;
//            double meanTime = 0;
//            double meanSeedTime = 0;
//            for (TrackerHit hit : trk.getTrackerHits()) {
//                Collection<TrackerHit> htsList = hittostrip.allFrom(hittorotated.from(hit));
//                for (TrackerHit hts : htsList) {
//                    nStrips++;
//                    meanTime += hts.getTime();
//                    int layer = ((HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
//                    if (layer <= 6) {
//                        nSeedStrips++;
//                        meanSeedTime += hts.getTime();
//                    }
//                }
//            }
//            meanTime /= nStrips;
//            meanSeedTime /= nSeedStrips;
//
//            double rmsTime = 0;
//            double rmsSeedTime = 0;
//            for (TrackerHit hit : trk.getTrackerHits()) {
//                Collection<TrackerHit> htsList = hittostrip.allFrom(hittorotated.from(hit));
//                for (TrackerHit hts : htsList) {
//                    rmsTime += Math.pow(hts.getTime() - meanTime, 2);
//                    HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement();
//                    int layer = sensor.getLayerNumber();
//                    if (layer <= 6) {
//                        rmsSeedTime += Math.pow(hts.getTime() - meanSeedTime, 2);
//                    }
//                    String sensorName = getNiceSensorName(sensor);
//                    getSensorPlot(plotDir + "hitTimeResidual_", sensorName).fill((hts.getTime() - meanTime) * nStrips / (nStrips - 1)); //correct residual for bias
//                }
//            }
//            rmsTime = Math.sqrt(rmsTime / nStrips);
//            aida.histogram1D(plotDir + trackCollectionName+ "/"+ triggerType + "/" + "Mean time of hits on track").fill(meanTime);
//            aida.histogram1D(plotDir + trackCollectionName+ "/"+ triggerType + "/" + "RMS time of hits on track").fill(rmsTime);
//            aida.histogram2D(plotDir + trackCollectionName+ "/"+ triggerType + "/" + "Track chi2 vs. RMS time of hits").fill(rmsTime, trk.getChi2());
//
//            rmsSeedTime = Math.sqrt(rmsSeedTime / nSeedStrips);
//            aida.histogram1D(plotDir + trackCollectionName+ "/"+ triggerType + "/" + "RMS time of hits on seed layers").fill(rmsSeedTime);

//            System.out.format("%d seed strips, RMS time %f\n", nSeedStrips, rmsSeedTime);
//            System.out.format("%d strips, mean time %f, RMS time %f\n", nStrips, meanTime, rmsTime);
        }
        nTracksTop.fill(cntTop);
        nTracksBot.fill(cntBot);
        nTracksPos.fill(cntPos);
        nTracksEle.fill(cntEle);
    }

    @Override
    public void calculateEndOfRunQuantities() {
        IFitFactory fitFactory = AIDA.defaultInstance().analysisFactory().createFitFactory();
        IFitter fitter = fitFactory.createFitter("chi2");

        for (HpsSiSensor sensor : sensors) {
            //IHistogram1D occupancyPlot = aida.histogram1D(sensor.getName().replaceAll("Tracker_TestRunModule_", ""), 640, 0, 639);
            IHistogram1D hitTimeResidual = getSensorPlot(plotDir + "hitTimeResidual_", getNiceSensorName(sensor));
            IFitResult result = fitGaussian(hitTimeResidual, fitter, "range=\"(-20.0,20.0)\"");
            System.out.format("%s\t%f\t%f\t%d\t%d\n", getNiceSensorName(sensor), result.fittedParameters()[1], result.fittedParameters()[2], sensor.getFebID(), sensor.getFebHybridID());
        }

        monitoredQuantityMap.put(trackingQuantNames[0], (double) nTotTracks / nEvents);
        monitoredQuantityMap.put(trackingQuantNames[1], (double) nTotHits / nTotTracks);
        monitoredQuantityMap.put(trackingQuantNames[2], sumd0 / nTotTracks);
        monitoredQuantityMap.put(trackingQuantNames[3], sumz0 / nTotTracks);
        monitoredQuantityMap.put(trackingQuantNames[4], sumslope / nTotTracks);
        monitoredQuantityMap.put(trackingQuantNames[5], sumchisq / nTotTracks);
    }

    IFitResult fitGaussian(IHistogram1D h1d, IFitter fitter, String range) {
        double[] init = {h1d.maxBinHeight(), h1d.mean(), h1d.rms()};
        IFitResult ifr = null;
        try {
            ifr = fitter.fit(h1d, "g", init, range);
        } catch (RuntimeException ex) {
            System.out.println(this.getClass().getSimpleName() + ":  caught exception in fitGaussian");
        }
        return ifr;
//        double[] init = {20.0, 0.0, 1.0, 20, -1};
//        return fitter.fit(h1d, "g+p1", init, range);
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
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
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
