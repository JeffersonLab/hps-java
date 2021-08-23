package org.hps.online.recon.aida;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.recon.tracking.SvtPlotUtils;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;
import org.hps.conditions.svt.SvtTimingConstants;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.dev.IDevTree;
import hep.physics.vec.Hep3Vector;

/**
 * Create example histograms and data points in a remote AIDA tree
 */

//public class HPSMonitoringSeedTracker2021 extends RemoteAidaDriver {
/* this is just for testing...change to extends RemoteAidaDriver for online */
public class HPSMonitoringSeedTracker2021 extends Driver {
    /*
     * AIDA paths
     */
    private static final String ECAL_DIR = "/subdet/ecal";
    private static final String TRACKER_DIR = "/subdet/tracker";
    private static final String SVTOCC_DIR = "/subdet/svtOccupancy";
    private static final String SVTMAX_DIR = "/subdet/svtMaxSample";
    private static final String SVTHITS_DIR = "/subdet/svtHits";
    private static final String PERF_DIR = "/perf";
    /* this is just for testing...remove when running online */
    protected AIDA aida = AIDA.defaultInstance();
    protected IAnalysisFactory af = aida.analysisFactory();
    protected IDevTree tree = (IDevTree) aida.tree();
    protected IHistogramFactory hf = aida.analysisFactory().createHistogramFactory(tree);
    protected IDataPointSetFactory dpsf = aida.analysisFactory().createDataPointSetFactory(tree);

    /*
     * Performance plots
     */
    private IHistogram1D eventCountH1D;
    private IDataPointSet eventRateDPS;
    private IDataPointSet millisPerEventDPS;
    /*
     * SVT Raw hit plots
     */
    private int maxSamplePosition = -1;
    private int timeWindowWeight = 1;
    private int eventCount = 0;
    private int eventRefreshRate = 1;
    private boolean enableMaxSamplePlots = false;
    private static Map<String, IHistogram1D> occupancyPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, int[]> occupancyMap = new HashMap<String, int[]>();
    private static Map<String, IHistogram1D> maxSamplePositionPlots = new HashMap<String, IHistogram1D>();
    private List<HpsSiSensor> sensors;

    /*
     * SVT Hits plots
     */
    private SvtTimingConstants timingConstants;
    private boolean cutOutLowChargeHits = false;
    private double hitChargeCut = 400;
    private boolean dropSmallHitEvents = false;
    private static final Map<String, IHistogram1D> hitsPerSensorPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, int[]> hitsPerSensor = new HashMap<String, int[]>();
    private static final Map<String, IHistogram1D> hitCountPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> t0Plots = new HashMap<String, IHistogram1D>();

    /*
     * Tracker plots
     */
    private IHistogram1D tracksPerEventH1D;
    private IHistogram1D rawHitsPerTrackH1D;
    private IHistogram1D rawTrackerHitsPerEventH1D;
    private IHistogram1D chi2H1D;
    private IHistogram1D d0H1D;
    private IHistogram1D omegaH1D;
    private IHistogram1D phiH1D;
    private IHistogram1D tanLambdaH1D;
    private IHistogram1D z0H1D;
    private IHistogram1D hthLayerH1D;
    private IHistogram1D fittedTrackerHitsPerEventH1D;

    /*
     * Ecal plots
     */
    private IHistogram2D ecalReadoutHitsXYH2D;
    private IHistogram1D ecalReadoutHitsPerEventH1D;
    private IHistogram1D adcValuesH1D;
    private IHistogram1D clustersPerEventH1D;
    private IHistogram1D clusEnergyH1D;
    private IHistogram1D clusTimeH1D;
    private IHistogram1D clusNHitsH1D;

    private static final String TRACKER_NAME = "Tracker";
    /*
     * Collection names
     */
    private static final String CLUSTERS = "EcalClusters";
    private static final String RAW_TRACKER_HITS = "SVTRawTrackerHits";
    private static final String FITTED_HITS = "SVTFittedRawTrackerHits";
    private static final String TRACKS = "MatchedTracks";
    private static final String ECAL_READOUT_HITS = "EcalReadoutHits";
    private static final String HELICAL_TRACK_HITS = "HelicalTrackHits";
    private static final String triggerBankCollectionName = "TriggerBank";
    private static final String stripClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
  

    /*
     * Event timing
     */
    private int eventsProcessed = 0;
    private long start = -1L;
    private Timer timer;
    /* SVT Occupancy setters */
    public void setEnableMaxSamplePlots(boolean enableMaxSamplePlots) {
        this.enableMaxSamplePlots = enableMaxSamplePlots;
    }

    
    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }
    public void setMaxSamplePosition(int maxSamplePosition) {
        this.maxSamplePosition = maxSamplePosition;
    }
    public void setTimeWindowWeight(int timeWindowWeight) {
        this.timeWindowWeight = timeWindowWeight;
    }

    /* SVT Hit Info setters */
    public void setDropSmallHitEvents(boolean dropSmallHitEvents) {
        this.dropSmallHitEvents = dropSmallHitEvents;
    }

    public void setCutOutLowChargeHits(boolean cutOutLowChargeHits) {
        this.cutOutLowChargeHits = cutOutLowChargeHits;
    }

    public void setHitChargeCut(double hitCharge) {
        this.hitChargeCut = hitCharge;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        // Get the HpsSiSensor objects from the geometry
        sensors = detector.getSubdetector(TRACKER_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
        // If there were no sensors found, throw an exception
        if (sensors.isEmpty())
            throw new RuntimeException("There are no sensors associated with this detector");

        /*
         * SVT RAW Occupancy plots
         */
        tree.mkdirs(SVTOCC_DIR);
        tree.cd(SVTOCC_DIR);
        for (HpsSiSensor sensor : sensors) {
            /* this is just for testing...remove when running online */
            occupancyMap.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), new int[640]);
            occupancyPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), aida
                    .histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Occupancy", 640, 0, 640));
            maxSamplePositionPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), aida.histogram1D(
                    SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Max Sample Number", 6, -0.5, 5.5));
        }
        /*
         * SVT Hits
         */
        tree.mkdirs(SVTHITS_DIR);
        tree.cd(SVTHITS_DIR);
        for (HpsSiSensor sensor : sensors) {
            hitsPerSensorPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    aida.histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Raw Hits", 50, 0, 50));
            hitsPerSensor.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), new int[1]);
            t0Plots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    aida.histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - t0", 100, -100, 100.0));
        }
        hitCountPlots.put("Raw hit counts", aida.histogram1D("Raw hit counts", 100, 0, 500));
        hitCountPlots.put("SVT top raw hit counts", aida.histogram1D("SVT top raw hit counts", 100, 0, 300));
        hitCountPlots.put("SVT bottom raw hit counts",
                aida.histogram1D("SVT bottom raw hit counts", 100, 0, 300));
        /*
         * Tracking plots
         */

        tree.mkdirs(TRACKER_DIR);
        tree.cd(TRACKER_DIR);

        tracksPerEventH1D = aida.histogram1D("Tracks Per Event", 20, 0., 20.);
        // tracksPerEventH1D.annotation().addItem("xAxisLabel", "Tracks / Event");
        // tracksPerEventH1D.annotation().addItem("yAxisLabel", "Count");

        rawHitsPerTrackH1D = aida.histogram1D("Raw Hits Per Track", 10, 0., 10);
        // rawHitsPerTrackH1D.annotation().addItem("xAxisLabel", "Hits / Track");
        // rawHitsPerTrackH1D.annotation().addItem("yAxisLabel", "Count");

        rawTrackerHitsPerEventH1D = aida.histogram1D("Raw Tracker Hits Per Event",100,0,500);
        chi2H1D = aida.histogram1D("Chi2", 25, 0., 50.);
        d0H1D = aida.histogram1D("D0", 50, -5.0, 5.0);
        omegaH1D = aida.histogram1D("Omega", 50, -0.0015, 0.0015);
        phiH1D = aida.histogram1D("Phi", 50, -0.2, 0.5);
        tanLambdaH1D = aida.histogram1D("Tan Lambda", 50, -0.08, 0.08);
        z0H1D = aida.histogram1D("Z0", 50, -0.5, 0.5);
        fittedTrackerHitsPerEventH1D = aida.histogram1D("Fitted Tracker Hits Per Event", 100, 0, 500);
        hthLayerH1D = aida.histogram1D("Helical Track Hit Layer", 15, 0, 15);

        /*
         * Ecal plots
         */

        tree.mkdirs(ECAL_DIR);
        tree.cd(ECAL_DIR);

        clusEnergyH1D = aida.histogram1D("Cluster Energy",50,0,4.0);
        clusTimeH1D = aida.histogram1D("Cluster Time",50,30,80);
        clusNHitsH1D = aida.histogram1D("Cluster N Hits",10,0,10);
        ecalReadoutHitsXYH2D = aida.histogram2D("Readout Hits XY", 47, -23.5, 23.5, 11, -5.5, 5.5);
        // ecalReadoutHitsXYH2D.annotation().addItem("xAxisLabel", "X Index");
        // ecalReadoutHitsXYH2D.annotation().addItem("yAxisLabel", "Y Index");

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
//                    LOG.fine("Event Timer: " + eps + " Hz");

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

    }

    public void endOfData() {

//        timer.cancel();

        super.endOfData();
    }

    public void process(EventHeader event) {

        eventCountH1D.fill(0.5);
        eventCount++;
        this.clearHitMaps();
        // If the event doesn't have a collection of RawTrackerHit's, skip it.
        if (!event.hasCollection(RawTrackerHit.class, RAW_TRACKER_HITS)) {
            return;
        }
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, RAW_TRACKER_HITS);
        List<LCRelation> fittedHits = event.get(LCRelation.class, FITTED_HITS);
        rawTrackerHitsPerEventH1D.fill(rawHits.size());
        
        // Increment strip hit count.
        for (RawTrackerHit rawHit : rawHits) {
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement(); 
            hitsPerSensor.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[0]++;
            // Obtain the raw ADC samples for each of the six samples readout
            short[] adcValues = rawHit.getADCValues();

            // Find the sample that has the largest amplitude. This should
            // correspond to the peak of the shaper signal if the SVT is timed
            // in correctly. Otherwise, the maximum sample value will default
            // to 0.
            int maxAmplitude = 0;
            int maxSamplePositionFound = -1;
            for (int sampleN = 0; sampleN < 6; sampleN++)
                if (adcValues[sampleN] > maxAmplitude) {
                    maxAmplitude = adcValues[sampleN];
                    maxSamplePositionFound = sampleN;
                }
            if (maxSamplePosition == -1 || maxSamplePosition == maxSamplePositionFound)
                occupancyMap.get(
                        SvtPlotUtils.fixSensorNumberLabel(((HpsSiSensor) rawHit.getDetectorElement()).getName()))[rawHit
                                .getIdentifierFieldValue("strip")]++; // System.out.println("Filling occupancy");

            maxSamplePositionPlots
                    .get(SvtPlotUtils.fixSensorNumberLabel(((HpsSiSensor) rawHit.getDetectorElement()).getName()))
                    .fill(maxSamplePositionFound);            
        }
        // Plot strip occupancies.
        if (eventCount % eventRefreshRate == 0)
            for (HpsSiSensor sensor : sensors) {
                int[] strips = occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()));
                occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
                for (int channel = 0; channel < strips.length; channel++) {
                    double stripOccupancy = (double) strips[channel] / (double) eventCount;

                    stripOccupancy /= this.timeWindowWeight;
                    System.out.println("channel " + channel + " occupancy = " + stripOccupancy);
                    occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(channel,
                            stripOccupancy);
                }
            }
        
        
        for (LCRelation fittedHit : fittedHits) {
            // Obtain the SVT raw hit associated with this relation
            RawTrackerHit rawHit = (RawTrackerHit) fittedHit.getFrom();
            int channel = (int) rawHit.getIdentifierFieldValue("strip");
            // Obtain the HpsSiSensor associated with the raw hit
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
            double t0 = FittedRawTrackerHit.getT0(fittedHit);
            double amplitude = FittedRawTrackerHit.getAmp(fittedHit);
            double chi2Prob = ShapeFitParameters.getChiProb(FittedRawTrackerHit.getShapeFitParameters(fittedHit));
            if (cutOutLowChargeHits)
                if (amplitude / DopedSilicon.ENERGY_EHPAIR < hitChargeCut)
                    continue;
            t0Plots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(t0);
            double trigPhase = (((event.getTimeStamp() - 4 * timingConstants.getOffsetPhase()) % 24) - 12);
//            t0VsTriggerTime.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(t0, trigPhase);
//            System.out.println( triggerData.getIntVal(1)*0.0000001);
//            t0VsTriggerBank.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(t0, triggerData.getIntVal(1)*0.0000001);
 //            t0VsChannel.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(t0, channel);
//            amplitudePlots.get(sensor).fill(amplitude);
            //           chi2Plots.get(sensor).fill(chi2Prob);
        }
        
        int[] topLayersHit = new int[14];
        int[] botLayersHit = new int[14];
        int eventHitCount = 0;
        int topEventHitCount = 0;
        int botEventHitCount = 0;
        for (HpsSiSensor sensor : sensors) {
            int hitCount = hitsPerSensor.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[0];
            hitsPerSensorPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hitCount);

            eventHitCount += hitCount;

            if (hitsPerSensor.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[0] > 0)
                if (sensor.isTopLayer()) {
                    topLayersHit[sensor.getLayerNumber() - 1]++;
                    topEventHitCount += hitCount;
                } else {
                    botLayersHit[sensor.getLayerNumber() - 1]++;
                    botEventHitCount += hitCount;
                }
        }      

        hitCountPlots.get("Raw hit counts").fill(eventHitCount);
        hitCountPlots.get("SVT top raw hit counts").fill(topEventHitCount);
        hitCountPlots.get("SVT bottom raw hit counts").fill(botEventHitCount);

        
        List<Track> tracks = event.get(Track.class, TRACKS);
        aida.tree().cd(TRACKER_DIR);

        tracksPerEventH1D.fill(tracks.size());

        for (Track track : tracks) {
            chi2H1D.fill(track.getChi2());
            List<TrackState> trackStates = track.getTrackStates();
            TrackState ts = trackStates.get(0);
            if (ts != null) {
                d0H1D.fill(ts.getD0());
                omegaH1D.fill(ts.getOmega());
                phiH1D.fill(ts.getPhi());
                tanLambdaH1D.fill(ts.getTanLambda());
                z0H1D.fill(ts.getZ0());
            }
            this.rawHitsPerTrackH1D.fill(track.getTrackerHits().size());
        }

        List<TrackerHit> helicalTrackHits = event.get(TrackerHit.class, HELICAL_TRACK_HITS);
        for (TrackerHit hit : helicalTrackHits) {
            int layer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();
            hthLayerH1D.fill(layer);
        }

        rawTrackerHitsPerEventH1D.fill(event.get(RawTrackerHit.class, RAW_TRACKER_HITS).size());

        fittedTrackerHitsPerEventH1D.fill(event.get(LCRelation.class, FITTED_HITS).size());

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
            clusEnergyH1D.fill(clus.getEnergy());
            clusTimeH1D.fill(clus.getCalorimeterHits().get(0).getTime());
            clusNHitsH1D.fill(clus.getCalorimeterHits().size());
        }
        clustersPerEventH1D.fill(clusterList.size());

        ++eventsProcessed;
    }

    public static Map<Integer, Hep3Vector> createStripPositionMap(HpsSiSensor sensor) {
        Map<Integer, Hep3Vector> positionMap = new HashMap<Integer, Hep3Vector>();
        for (ChargeCarrier carrier : ChargeCarrier.values())
            if (sensor.hasElectrodesOnSide(carrier)) {
                // SiSensorElectrodes electrodes = sensor.getReadoutElectrodes();
                SiSensorElectrodes strips = (SiSensorElectrodes) sensor.getReadoutElectrodes(carrier);
                ITransform3D parentToLocal = sensor.getReadoutElectrodes(carrier).getParentToLocal();
                ITransform3D localToGlobal = sensor.getReadoutElectrodes(carrier).getLocalToGlobal();
                for (int physicalChannel = 0; physicalChannel < 640; physicalChannel++) {
                    Hep3Vector localStripPosition = strips.getCellPosition(physicalChannel);
                    Hep3Vector stripPosition = parentToLocal.transformed(localStripPosition);
                    Hep3Vector globalStripPosition = localToGlobal.transformed(stripPosition);
                    positionMap.put(physicalChannel, globalStripPosition);
                }
            }
        return positionMap;
    }
    private void clearHitMaps() {
        for (HpsSiSensor sensor : sensors)
            hitsPerSensor.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[0] = 0;
    }

}
