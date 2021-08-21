package org.hps.online.recon.aida;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import static java.lang.Math.sqrt;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.recon.tracking.SvtPlotUtils;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.conditions.svt.SvtTimingConstants;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.geometry.Detector;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.dev.IDevTree;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import org.hps.recon.tracking.TrackUtils;

import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.FieldMap;

/**
 * Create example histograms and data points in a remote AIDA tree
 */
//public class HPSMonitoringSeedTracker2021 extends RemoteAidaDriver {
/* this is just for testing...change to extends RemoteAidaDriver for online */
public class HPSMonitoringKFTracker2021 extends Driver {

    /*
     * AIDA paths
     */
    private static final String ECAL_DIR = "/ecal";
    private static final String TRACKER_DIR = "/tracks";
    private static final String TRACKTIME_DIR = "/tracks/trkTime";
    private static final String SVTOCC_DIR = "/svtOccupancy";
    private static final String SVTMAX_DIR = "/svtMaxSample";
    private static final String SVTHITS_DIR = "/svtHits";
    private static final String FINALSTATE_DIR = "/finalState";
    private static final String V0_DIR = "/V0";
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
     * Ecal plots
     */
    private IHistogram2D ecalReadoutHitsXYH2D;
    private IHistogram1D ecalReadoutHitsPerEventH1D;
    private IHistogram1D adcValuesH1D;
    private IHistogram1D clustersPerEventH1D;
    private IHistogram1D clusEnergyH1D;
    private IHistogram1D clusTimeH1D;
    private IHistogram1D clusNHitsH1D;

    /*
     * SVT Raw hit plots
     */
    private int maxSamplePosition = -1;
    private int timeWindowWeight = 1;
    private int eventCount = 0;
    private int eventRefreshRate = 10;
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
    private static final Map<String, IHistogram1D> layersHitPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> hitCountPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> t0Plots = new HashMap<String, IHistogram1D>();

    /*
     * Tracker plots
     */
    private IHistogram1D tracksPerEventH1D;
    private IHistogram1D rawHitsPerTrackH1D;

    private IHistogram1D chi2H1D;
    private IHistogram1D d0H1D;
    private IHistogram1D omegaH1D;
    private IHistogram1D phiH1D;
    private IHistogram1D tanLambdaH1D;
    private IHistogram1D z0H1D;   
    private IHistogram1D fittedTrackerHitsPerEventH1D;

    /* 
     *   Track Time plots
     */
    private static final Map<String, IHistogram1D> t0 = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackHitDt = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackHitT0 = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackT0 = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackTimeRange = new HashMap<String, IHistogram1D>();

    private static final Map<String, IHistogram2D> trackTrigTime = new HashMap<String, IHistogram2D>();
    //private static final Map<String, IHistogram2D> trackHitDtChan = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> trackHit2D = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> trackTimeMinMax = new HashMap<String, IHistogram2D>();

    double minTime = -40;
    double maxTime = 40;

    /*
    *  Final state particle and V0 plots
     */
    IHistogram1D nEle;
    IHistogram1D elePx;
    IHistogram1D elePy;
    IHistogram1D elePz;
    IHistogram2D eleProjXYEcalMatch;
    IHistogram2D eleProjXYEcalNoMatch;

    IHistogram1D nPos;
    IHistogram1D posPx;
    IHistogram1D posPy;
    IHistogram1D posPz;
    IHistogram2D posProjXYEcalMatch;
    IHistogram2D posProjXYEcalNoMatch;

    IHistogram1D nPhot;
    IHistogram1D photEne;
    IHistogram2D photXYECal;
    IHistogram1D pi0Ene;
    IHistogram1D pi0Diff;
    IHistogram1D pi0Mass;

    double ecalXRange = 500;
    double ecalYRange = 100;

    double pMax = 7.0;
    double pi0EsumCut = 3.0;//GeV
    double pi0EdifCut = 2.0;//GeV

    IHistogram1D nV0;
    IHistogram1D unconMass;
    IHistogram1D unconVx;
    IHistogram1D unconVy;
    IHistogram1D unconVz;
    IHistogram1D unconChi2;

    IHistogram2D pEleVspPos;
    IHistogram2D pyEleVspyPos;
    IHistogram2D pxEleVspxPos;
    IHistogram2D massVsVtxZ;

    //The field map for extrapolation
    private FieldMap bFieldMap;
    private double targetZ = -7.5;

    private static final String TRACKER_NAME = "Tracker";
    /*
     * Collection names
     */
    private static final String CLUSTERS = "EcalClusters";
    private static final String RAW_TRACKER_HITS = "SVTRawTrackerHits";
    private static final String FITTED_HITS = "SVTFittedRawTrackerHits";
    private static final String TRACKS = "KalmanFullTracks";
    private static final String ECAL_READOUT_HITS = "EcalReadoutHits";
    private static final String triggerBankCollectionName = "TriggerBank";
    private static final String stripClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String finalStateParticlesColName = "FinalStateParticles_KF";
    private String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates_KF";
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

    public void setPMax(double pmax) {
        this.pMax = pmax;
    }

    public void setPi0EsumCut(double cut) {
        this.pi0EsumCut = cut;
    }

    public void setPi0EdifCut(double cut) {
        this.pi0EdifCut = cut;
    }

    public void setFinalStateParticlesColName(String name) {
        this.finalStateParticlesColName = name;
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
         * Ecal plots
         */
        tree.mkdirs(ECAL_DIR);
        tree.cd(ECAL_DIR);

        clusEnergyH1D = aida.histogram1D("Cluster Energy", 50, 0, 4.0);
        clusTimeH1D = aida.histogram1D("Cluster Time", 50, 30, 80);
        clusNHitsH1D = aida.histogram1D("Cluster N Hits", 10, 0, 10);
        ecalReadoutHitsXYH2D = aida.histogram2D("Readout Hits XY", 47, -23.5, 23.5, 11, -5.5, 5.5);
        // ecalReadoutHitsXYH2D.annotation().addItem("xAxisLabel", "X Index");
        // ecalReadoutHitsXYH2D.annotation().addItem("yAxisLabel", "Y Index");

        ecalReadoutHitsPerEventH1D = aida.histogram1D("Readout Hits Per Event ", 50, 0, 50.);
        clustersPerEventH1D = aida.histogram1D("Clusters Per Event", 10, -0.5, 9.5);
        adcValuesH1D = aida.histogram1D("Readout Hit ADC Values", 300, 0, 300.);
        /*
         * SVT RAW Occupancy plots
         */
        tree.mkdirs(SVTOCC_DIR);
        tree.cd(SVTOCC_DIR);
        tree.mkdir(SVTMAX_DIR);
        for (HpsSiSensor sensor : sensors) {
            tree.cd(SVTOCC_DIR);
            occupancyMap.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), new int[640]);
            occupancyPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), aida
                    .histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Occupancy", 640, 0, 640));
            tree.cd(SVTMAX_DIR);
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
        layersHitPlots.put("Top", aida.histogram1D("Top Layers Hit", 15, 0, 15));
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

        rawHitsPerTrackH1D = aida.histogram1D("Raw Hits Per Track", 15, 0., 15);
        // rawHitsPerTrackH1D.annotation().addItem("xAxisLabel", "Hits / Track");
        // rawHitsPerTrackH1D.annotation().addItem("yAxisLabel", "Count");

        chi2H1D = aida.histogram1D("Chi2", 50, 0., 50.);
        d0H1D = aida.histogram1D("D0", 50, -10.0, 10.0);
        omegaH1D = aida.histogram1D("Omega", 50, -0.0015, 0.0015);
        phiH1D = aida.histogram1D("Phi", 50, -0.3, 0.3);
        tanLambdaH1D = aida.histogram1D("Tan Lambda", 50, -0.08, 0.08);
        z0H1D = aida.histogram1D("Z0", 50, -2.5, 2.5);
        fittedTrackerHitsPerEventH1D = aida.histogram1D("Fitted Tracker Hits Per Event", 100, 0, 500);

        /*
         *  Track time plots
         */
        tree.mkdirs(TRACKTIME_DIR);
        tree.cd(TRACKTIME_DIR);
        for (HpsSiSensor sensor : sensors) {
            t0.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    aida.histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - t0", 100, minTime, maxTime));
            trackHitT0.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    aida.histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - track hit t0", 100, minTime, maxTime));
            trackHitDt.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    aida.histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - track hit dt", 100, minTime, maxTime));

            trackHit2D.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    aida.histogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - trigger phase vs dt", 80, -20, 20.0, 6, 0, 24.0));
            //trackHitDtChan.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
            //        aida.histogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - dt vs position", 200, -20, 20, 50, -20, 20.0));
        }

        trackT0.put("Top",
                aida.histogram1D("Top Track Time", 80, -40, 40.0));
        trackT0.put("Bottom",
                aida.histogram1D("Bottom Track Time", 80, -40, 40.0));

        trackTrigTime.put("Top",
                aida.histogram2D("Top Track Time vs. Trig Time", 80, -40, 40.0, 6, 0, 24));
        trackTrigTime.put("Bottom",
                aida.histogram2D("Bottom Track Time vs. Trig Time", 80, -40, 40.0, 6, 0, 24));
        trackTimeRange.put("Top",
                aida.histogram1D("Top Track Time Range", 75, 0, 30.0));
        trackTimeRange.put("Bottom",
                aida.histogram1D("Bottom Track Time Range", 75, 0, 30.0));
        trackTimeMinMax.put("Top",
                aida.histogram2D("Top Earliest vs Latest Track Hit Times", 80, -25, 25.0, 80, -25, 25.0));
        trackTimeMinMax.put("Bottom",
                aida.histogram2D("Bottom Earliest vs Latest Track Hit Times", 80, -25, 25.0, 80, -25, 25.0));

        /*
        *  Final state particle and V0 plots
         */
        tree.mkdirs(FINALSTATE_DIR);
        tree.mkdirs(V0_DIR);
        tree.cd(FINALSTATE_DIR);
        nEle = aida.histogram1D("Number of Electrons per event", 5, 0, 5);
        elePx = aida.histogram1D("Electron Px (GeV)", 50, -0.2, 0.2);
        elePy = aida.histogram1D("Electron Py (GeV)", 50, -0.2, 0.2);
        elePz = aida.histogram1D("Electron Pz (GeV)", 50, 0.0, pMax);
        eleProjXYEcalMatch = aida.histogram2D("Electron ECal Projection: Matched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        eleProjXYEcalNoMatch = aida.histogram2D("Electron ECal Projection: Unmatched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);

        nPos = aida.histogram1D("Number of Positrons per event", 5, 0, 5);
        posPx = aida.histogram1D("Positron Px (GeV)", 50, -0.2, 0.2);
        posPy = aida.histogram1D("Positron Py (GeV)", 50, -0.2, 0.2);
        posPz = aida.histogram1D("Positron Pz (GeV)", 50, 0.0, pMax);
        posProjXYEcalMatch = aida.histogram2D("Positron ECal Projection: Matched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        posProjXYEcalNoMatch = aida.histogram2D("Positron ECal Projection: Unmatched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        tree.cd(V0_DIR);
        nPhot = aida.histogram1D("Number of Photons per event", 5, 0, 5);
        photEne = aida.histogram1D("Photon Energy (GeV)", 50, 0.0, pMax);
        photXYECal = aida.histogram2D("ECal Position", 50, -300, 400, 50, -ecalYRange, ecalYRange);
        pi0Ene = aida.histogram1D("pi0 Energy (GeV)", 50, pi0EsumCut, pMax);
        pi0Diff = aida.histogram1D("pi0 E-Diff (GeV)", 50, 0, pi0EdifCut);
        pi0Mass = aida.histogram1D("pi0 Mass (GeV)", 50, 0.0, 0.3);
        /* V0 Quantities */
 /* Mass, vertex, chi^2 of fit */
 /* unconstrained  */
        tree.cd(V0_DIR);
        nV0 = aida.histogram1D("Number of V0 per event", 5, 0, 5);
        unconMass = aida.histogram1D("Unconstrained Mass (GeV)", 100, 0, 0.200);
        unconVx = aida.histogram1D("Unconstrained Vx (mm)", 50, -1, 1);
        unconVy = aida.histogram1D("Unconstrained Vy (mm)", 50, -0.6, 0.6);
        unconVz = aida.histogram1D("Unconstrained Vz (mm)", 50, -10, 10);
        unconChi2 = aida.histogram1D("Unconstrained Chi2", 25, 0, 25);
        pEleVspPos = aida.histogram2D("P(e) vs P(p)", 50, 0, 2.5, 50, 0, 2.5);
        pyEleVspyPos = aida.histogram2D("Py(e) vs Py(p)", 50, -0.1, 0.1, 50, -0.1, 0.1);
        pxEleVspxPos = aida.histogram2D("Px(e) vs Px(p)", 50, -0.1, 0.1, 50, -0.1, 0.1);
        massVsVtxZ = aida.histogram2D("Mass vs Vz", 50, 0, 0.15, 50, -10, 10);

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
        bFieldMap = detector.getFieldMap();

    }

    public void endOfData() {

//        timer.cancel();
        super.endOfData();
    }

    public void process(EventHeader event) {

        eventCountH1D.fill(0.5);
        eventCount++;
        this.clearHitMaps();

        List<RawTrackerHit> ecalHits = event.get(RawTrackerHit.class, ECAL_READOUT_HITS);
        ecalReadoutHitsPerEventH1D.fill(ecalHits.size());
        for (RawTrackerHit hit : ecalHits) {
            int column = hit.getIdentifierFieldValue("ix");
            int row = hit.getIdentifierFieldValue("iy");
            ecalReadoutHitsXYH2D.fill(column, row);
            for (short adcValue : hit.getADCValues())
                adcValuesH1D.fill(adcValue);
        }

        List<Cluster> clusterList = event.get(Cluster.class, CLUSTERS);
        for (Cluster clus : clusterList) {
            clusEnergyH1D.fill(clus.getEnergy());
            clusTimeH1D.fill(clus.getCalorimeterHits().get(0).getTime());
            clusNHitsH1D.fill(clus.getCalorimeterHits().size());
        }
        clustersPerEventH1D.fill(clusterList.size());

        // If the event doesn't have a collection of RawTrackerHit's, skip it.
        if (!event.hasCollection(RawTrackerHit.class, RAW_TRACKER_HITS))
            return;
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, RAW_TRACKER_HITS);
        //System.out.println(rawHits.size());
        List<LCRelation> fittedHits = event.get(LCRelation.class, FITTED_HITS);
        fittedTrackerHitsPerEventH1D.fill(event.get(LCRelation.class, FITTED_HITS).size());

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
                    //System.out.println("channel " + channel + " occupancy = " + stripOccupancy);
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
            //I don't think this is at the target...need to get @target included as track state
            TrackState ts = TrackStateUtils.getTrackStateAtIP(track);
            BaseTrackState ts_bs = TrackUtils.getTrackExtrapAtVtxSurfRK(ts, bFieldMap, 0., targetZ);

            if (ts_bs != null) {
                d0H1D.fill(ts_bs.getReferencePoint()[1]);//the way the extrapolation/new reference point is done, the d0==0
                omegaH1D.fill(ts_bs.getOmega());
                phiH1D.fill(ts_bs.getPhi());
                tanLambdaH1D.fill(ts_bs.getTanLambda());
                z0H1D.fill(ts_bs.getZ0());               
            }
            this.rawHitsPerTrackH1D.fill(track.getTrackerHits().size());
        }

        /* 
        *  Track and hit times
         */
        int trigTime = (int) (event.getTimeStamp() % 24);

        // ===> IIdentifierHelper helper = SvtUtils.getInstance().getHelper();
        List<SiTrackerHitStrip1D> hits = event.get(SiTrackerHitStrip1D.class, stripClusterCollectionName);
        for (SiTrackerHitStrip1D hit : hits) {
            // ===> IIdentifier id = hit.getSensor().getIdentifier();
            // ===> int layer = helper.getValue(id, "layer");
            int layer = ((HpsSiSensor) hit.getSensor()).getLayerNumber();
            int module = ((HpsSiSensor) hit.getSensor()).getModuleNumber();
            // ===> int module = helper.getValue(id, "module");
            // System.out.format("%d, %d, %d\n",hit.getCellID(),layer,module);
            SiSensor sensor = hit.getSensor();
            t0.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime());
        }

        for (Track track : tracks) {
            int trackModule;
            String moduleName = "Top";
            if (track.getTrackerHits().get(0).getPosition()[1] > 0)
                trackModule = 0;
            else {
                moduleName = "Bottom";
                trackModule = 1;
            }
            double minTime = Double.POSITIVE_INFINITY;
            double maxTime = Double.NEGATIVE_INFINITY;
            int hitCount = 0;
            double trackTime = 0;
            for (TrackerHit hitTH : track.getTrackerHits()) {
                SiTrackerHitStrip1D hit = (SiTrackerHitStrip1D) hitTH;
                SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                trackHitT0.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime());
                trackTime += hit.getTime();
                hitCount++;
                if (hit.getTime() > maxTime)
                    maxTime = hit.getTime();
                if (hit.getTime() < minTime)
                    minTime = hit.getTime();
            }
            trackTimeMinMax.get(moduleName).fill(minTime, maxTime);
            trackTimeRange.get(moduleName).fill(maxTime - minTime);
            trackTime /= hitCount;
            trackT0.get(moduleName).fill(trackTime);
            trackTrigTime.get(moduleName).fill(trackTime, trigTime);

            for (TrackerHit hitTH : track.getTrackerHits()) {
                SiTrackerHitStrip1D hit = (SiTrackerHitStrip1D) hitTH;
                int layer = ((HpsSiSensor) hit.getSensor()).getLayerNumber();
                int module = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("module");
                SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                trackHitDt.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime() - trackTime);
                trackHit2D.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime() - trackTime, event.getTimeStamp() % 24);
                //trackHitDtChan.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getMeasuredCoordinate().x(), hit.getTime() - trackTime);
            }
        }

        /*
        *  Final state particle plots
         */
        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class,
                finalStateParticlesColName);

        int eleCnt = 0;
        int posCnt = 0;
        int photCnt = 0;
        for (ReconstructedParticle fsp : fspList) {
            double charge = fsp.getCharge();
            if (charge < 0) {
                eleCnt++;
                Hep3Vector mom = fsp.getMomentum();
                elePx.fill(mom.x());
                elePy.fill(mom.y());
                elePz.fill(mom.z());
                TrackState stateAtEcal = TrackUtils.getTrackStateAtECal((fsp.getTracks().get(0)));
                Hep3Vector tPos = new BasicHep3Vector(stateAtEcal.getReferencePoint());
                if (fsp.getClusters().size() != 0)
                    eleProjXYEcalMatch.fill(tPos.y(), tPos.z());
                else
                    eleProjXYEcalNoMatch.fill(tPos.y(), tPos.z());
            } else if (charge > 0) {
                posCnt++;
                Hep3Vector mom = fsp.getMomentum();
                posPx.fill(mom.x());
                posPy.fill(mom.y());
                posPz.fill(mom.z());
                TrackState stateAtEcal = TrackUtils.getTrackStateAtECal((fsp.getTracks().get(0)));
                Hep3Vector tPos = new BasicHep3Vector(stateAtEcal.getReferencePoint());
                if (fsp.getClusters().size() != 0)
                    posProjXYEcalMatch.fill(tPos.y(), tPos.z());// tracking frame!
                else
                    posProjXYEcalNoMatch.fill(tPos.y(), tPos.z());
            } else if (fsp.getClusters().size() != 0) {
                photCnt++;
                Cluster clu = fsp.getClusters().get(0);
                photEne.fill(clu.getEnergy());
                photXYECal.fill(clu.getPosition()[0], clu.getPosition()[1]);
            } else
                System.out.println("This FSP had no tracks or clusters???");
        }
        for (ReconstructedParticle fsp1 : fspList) {
            if (fsp1.getCharge() != 0)
                continue;
            for (ReconstructedParticle fsp2 : fspList) {
                if (fsp1 == fsp2)
                    continue;
                if (fsp2.getCharge() != 0)
                    continue;
//                if (fsp1.getClusters().get(0) == null || fsp2.getClusters().get(0) == null)
//                    continue;//this should never happen
                Cluster clu1 = fsp1.getClusters().get(0);
                Cluster clu2 = fsp2.getClusters().get(0);
                double pi0ene = clu1.getEnergy() + clu2.getEnergy();
                double pi0diff = Math.abs(clu1.getEnergy() - clu2.getEnergy());
                double pi0mass = getClusterPairMass(clu1, clu2);
                if (pi0diff > pi0EdifCut)
                    continue;
                if (pi0ene < pi0EsumCut)
                    continue;
                if (clu1.getPosition()[1] * clu2.getPosition()[1] < 0) {//top bottom
                    pi0Ene.fill(pi0ene);
                    pi0Diff.fill(pi0diff);
                    pi0Mass.fill(pi0mass);
                }
            }
        }
        nEle.fill(eleCnt);
        nPos.fill(posCnt);
        nPhot.fill(photCnt);

        /*
        *  V0 plots
         */
        List<ReconstructedParticle> unConstrainedV0List = event.get(ReconstructedParticle.class,
                unconstrainedV0CandidatesColName);
        nV0.fill(unConstrainedV0List.size());
        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
            Vertex uncVert = uncV0.getStartVertex();
            unconVx.fill(uncVert.getPosition().x());
            unconVy.fill(uncVert.getPosition().y());
            unconVz.fill(uncVert.getPosition().z());
            unconMass.fill(uncV0.getMass());
            unconChi2.fill(uncVert.getChi2());
            massVsVtxZ.fill(uncV0.getMass(), uncVert.getPosition().z());
            // this always has 2 tracks.
            List<ReconstructedParticle> trks = uncV0.getParticles();
            Track ele = trks.get(0).getTracks().get(0);
            Track pos = trks.get(1).getTracks().get(0);
            // if track #0 has charge>0 it's the electron! This seems mixed up, but remember the track
            // charge is assigned assuming a positive B-field, while ours is negative
            if (trks.get(0).getCharge() > 0) {
                pos = trks.get(0).getTracks().get(0);
                ele = trks.get(1).getTracks().get(0);
            }
            pEleVspPos.fill(getMomentum(ele), getMomentum(pos));
            pyEleVspyPos.fill(ele.getTrackStates().get(0).getMomentum()[2],
                    pos.getTrackStates().get(0).getMomentum()[2]);
            pxEleVspxPos.fill(ele.getTrackStates().get(0).getMomentum()[1],
                    pos.getTrackStates().get(0).getMomentum()[1]);
        }

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

    public double getClusterPairMass(Cluster clu1, Cluster clu2) {
        double x0 = clu1.getPosition()[0];
        double y0 = clu1.getPosition()[1];
        double z0 = clu1.getPosition()[2];
        double x1 = clu2.getPosition()[0];
        double y1 = clu2.getPosition()[1];
        double z1 = clu2.getPosition()[2];
        double e0 = clu1.getEnergy();
        double e1 = clu2.getEnergy();
        double xlen0 = sqrt(x0 * x0 + y0 * y0 + z0 * z0);
        double xlen1 = sqrt(x1 * x1 + y1 * y1 + z1 * z1);
        Hep3Vector p0 = new BasicHep3Vector(x0 / xlen0 * e0, y0 / xlen0 * e0, z0 / xlen0 * e0);
        Hep3Vector p1 = new BasicHep3Vector(x1 / xlen1 * e1, y1 / xlen1 * e1, z1 / xlen1 * e1);

        double esum = sqrt(p1.magnitudeSquared()) + sqrt(p0.magnitudeSquared());
        double pxsum = p1.x() + p0.x();
        double pysum = p1.y() + p0.y();
        double pzsum = p1.z() + p0.z();

        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0)
            return Math.sqrt(evtmass);
        else
            return -99;
    }

    private double getMomentum(Track trk) {

        double px = trk.getTrackStates().get(0).getMomentum()[0];
        double py = trk.getTrackStates().get(0).getMomentum()[1];
        double pz = trk.getTrackStates().get(0).getMomentum()[2];
        return Math.sqrt(px * px + py * py + pz * pz);
    }

}
