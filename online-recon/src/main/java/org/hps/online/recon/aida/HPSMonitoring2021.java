package org.hps.online.recon.aida;

import static java.lang.Math.sqrt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;
import org.hps.recon.tracking.SvtPlotUtils;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.Cluster;

import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;

import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.util.ArrayList;
import org.apache.commons.math.stat.StatUtils;
import org.hps.monitoring.ecal.plots.EcalMonitoringUtilities;
import org.hps.recon.ecal.EcalUtils;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.GenericObject;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;

/**
 * Make plots for remote AIDA display based on Kalman Filter track
 * reconstruction
 */
public class HPSMonitoring2021 extends RemoteAidaDriver {

    /*
     * AIDA paths
     */
    private static final String ECAL_HITS_DIR = "/ecalHits";
    private static final String ECAL_CLUSTERS_DIR = "/ecalClusters";
    private static final String ECAL_PAIRS_DIR = "/ecalPairs";
    private static final String TRACKER_DIR = "/trackPars";
    private static final String TRACKTIME_DIR = "/trackTime";
    private static final String SVTHITS_DIR = "/svtHits";
    private static final String SVTRAW_DIR = "/xperSensor/svtHits/counts";
    private static final String SVTT0_DIR = "/xperSensor/svtHits/time";   
    private static final String ELECTRON_DIR = "/electrons";
    private static final String POSITRON_DIR = "/positrons";
    private static final String TRACKTIMEHOT_DIR = "/xperSensor/tracks/trkHitTime";
    private static final String TRACKTIMEDTVSPHASE_DIR = "/xperSensor/tracks/deltaTvsPhase";
    private static final String TRACKTIMEDT_DIR = "/xperSensor/tracks/deltaT";
    private static final String SVTOCC_DIR = "/svtOccupancy";
    private static final String SVTMAX_DIR = "/xperSensor/svtHits/svtMaxSample";
    private static final String V01D_DIR = "/v01DPlots";
    private static final String V02D_DIR = "/v02DPlots";
    private static final String PERF_DIR = "/EventsProcessed";
    private static final String TEMP_DIR = "/tmp";

    /*
     * Performance plots
     */
    private IHistogram1D eventCountH1D;
    private IDataPointSet eventRateDPS;
    private IDataPointSet millisPerEventDPS;

    /*
     * Ecal plots
     */
    private IHistogram2D hitCountFillPlot;
    private IHistogram2D hitCountDrawPlot;

    private IHistogram2D occupancyDrawPlot;
    private double[] occupancyFill = new double[11 * 47];
    private int NoccupancyFill;
    private IHistogram2D clusterCountFillPlot;
    private IHistogram2D clusterCountDrawPlot;
    private IHistogram1D hitCountPlot;
    private IHistogram1D hitTimePlot;
    private IHistogram1D hitEnergyPlot;
    private IHistogram1D hitMaxEnergyPlot;
    private IHistogram1D topTimePlot, botTimePlot, orTimePlot;
    private IHistogram1D topTrigTimePlot, botTrigTimePlot, orTrigTimePlot;
    private IHistogram2D topTimePlot2D, botTimePlot2D, orTimePlot2D;

    private IHistogram1D clusterCountPlot;
    private IHistogram1D clusterSizePlot;
    private IHistogram1D clusterEnergyPlot;
    private IHistogram1D clusterMaxEnergyPlot;
    private IHistogram1D clusterTimes;
    private IHistogram1D clusterTimeSigma;
    private IHistogram2D edgePlot;

    private IHistogram1D pairEnergySum;
    private IHistogram1D pairEnergyDifference;
    private IHistogram1D pairCoplanarity;
    private IHistogram1D pairEnergySlope;
    private IHistogram1D pairEnergyPositionMeanX;
    private IHistogram1D pairEnergyPositionMeanY;
    private IHistogram1D pairMassOppositeHalf;
    private IHistogram1D pairMassSameHalf;
    private IHistogram1D pairMassOppositeHalfFid;
    private IHistogram1D pairMassSameHalfFid;

    int eventn = 0;
    int thisEventN, prevEventN;

    long thisTime, prevTime;
    double thisEventTime, prevEventTime;
    double maxE = 5000 * EcalUtils.MeV;
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
    private static final Map<String, IHistogram1D> trackHitDt = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackHitT0 = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackT0 = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackTimeRange = new HashMap<String, IHistogram1D>();

    private static final Map<String, IHistogram2D> trackTrigTime = new HashMap<String, IHistogram2D>();
    //private static final Map<String, IHistogram2D> trackHitDtChan = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> trackHit2D = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> trackTimeMinMax = new HashMap<String, IHistogram2D>();

    private double minTime = -40;
    private double maxTime = 40;

    private boolean enableTrackTimesPerSensorPlots = false;

    /*
    *  Final state particle and V0 plots
     */
    private IHistogram1D nEle;
    private IHistogram1D elePx;
    private IHistogram1D elePy;
    private IHistogram1D elePz;
    private IHistogram2D eleProjXYEcalMatch;
    private IHistogram2D eleProjXYEcalNoMatch;

    private IHistogram1D nPos;
    private IHistogram1D posPx;
    private IHistogram1D posPy;
    private IHistogram1D posPz;
    private IHistogram2D posProjXYEcalMatch;
    private IHistogram2D posProjXYEcalNoMatch;

    private IHistogram1D nPhot;
    private IHistogram1D photEne;
    private IHistogram2D photXYECal;

    private double ecalXRange = 500;
    private double ecalYRange = 100;

    private double pMax = 7.0;

    private IHistogram1D nV0;
    private IHistogram1D unconMass;
    private IHistogram1D unconVx;
    private IHistogram1D unconVy;
    private IHistogram1D unconVz;
    private IHistogram1D unconChi2;

    private IHistogram2D pEleVspPos;
    private IHistogram2D pyEleVspyPos;
    private IHistogram2D pxEleVspxPos;
    private IHistogram2D massVsVtxZ;

    //The field map for extrapolation
    private FieldMap bFieldMap;
    private double targetZ = -7.5;

    private static final String TRACKER_NAME = "Tracker";
    private boolean isSeedTracker = false;
    /*
     * Collection names
     */
    private static final String RAW_ECAL_HITS = "EcalCalHits";
    private static final String ECAL_CLUSTERS = "EcalClusters";
    private static final String RAW_TRACKER_HITS = "SVTRawTrackerHits";
    private static final String FITTED_HITS = "SVTFittedRawTrackerHits";
    private static final String ECAL_READOUT_HITS = "EcalReadoutHits";
    private static final String STRIP_CLUSTERS = "StripClusterer_SiTrackerHitStrip1D";
    private String trackColName = "KalmanFullTracks";
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

    public void setEnableTrackTimesPerSensorPlots(boolean enable) {
        this.enableTrackTimesPerSensorPlots = enable;
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

    public void setFinalStateParticlesColName(String name) {
        this.finalStateParticlesColName = name;
    }

    public void setUnconstrainedV0CandidatesColName(String name) {
        this.unconstrainedV0CandidatesColName = name;
    }

    public void setTrackColName(String name) {
        this.trackColName = name;
    }

    public void setIsSeedTracker(boolean isST) {
        this.isSeedTracker = isST;
    }

    public void setTargetZ(double z) {
        this.targetZ = z;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        // Get the HpsSiSensor objects from the geometry
        sensors = detector.getSubdetector(TRACKER_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
        // If there were no sensors found, throw an exception
        if (sensors.isEmpty()) {
            throw new RuntimeException("There are no sensors associated with this detector");
        }

        /*
         * Ecal plots
         */
        tree.mkdirs(TEMP_DIR);
        tree.mkdirs(ECAL_HITS_DIR);
        tree.mkdirs(ECAL_CLUSTERS_DIR);
        tree.mkdirs(ECAL_PAIRS_DIR);
        tree.cd(ECAL_HITS_DIR);

        String hitCountDrawPlotTitle;
        hitCountDrawPlotTitle = RAW_ECAL_HITS + " : Hit Rate KHz";

        hitCountDrawPlot = aida.histogram2D(hitCountDrawPlotTitle, 47, -23.5, 23.5, 11, -5.5, 5.5);
        occupancyDrawPlot = aida.histogram2D(RAW_ECAL_HITS + " : Occupancy", 47,
                -23.5, 23.5, 11, -5.5, 5.5);

        NoccupancyFill = 1; // to avoid a "NaN" at beginning
        for (int ii = 0; ii < (11 * 47); ii++) {
            int row = EcalMonitoringUtilities.getRowFromHistoID(ii);
            int column = EcalMonitoringUtilities.getColumnFromHistoID(ii);
            occupancyFill[ii] = 0.;
        }
        prevTime = 0; // init the time
        thisTime = 0; // init the time

        thisEventN = 0;
        prevEventN = 0;

        thisEventTime = 0;
        prevEventTime = 0;

        hitCountPlot = aida.histogram1D(RAW_ECAL_HITS + " : Hit Count In Event",
                30, -0.5, 29.5);

        topTimePlot = aida.histogram1D(RAW_ECAL_HITS + " : First Hit Time, Top",
                100, 0, 100 * 4.0);
        botTimePlot = aida.histogram1D(RAW_ECAL_HITS
                + " : First Hit Time, Bottom", 100, 0, 100 * 4.0);
        orTimePlot = aida.histogram1D(RAW_ECAL_HITS + " : First Hit Time, Or",
                100, 0, 100 * 4.0);

        hitTimePlot = aida.histogram1D(RAW_ECAL_HITS + " : Hit Time", 100,
                0 * 4.0, 100 * 4.0);
        topTrigTimePlot = aida.histogram1D(RAW_ECAL_HITS
                + " : Trigger Time, Top", 101, -2, 402);
        botTrigTimePlot = aida.histogram1D(RAW_ECAL_HITS
                + " : Trigger Time, Bottom", 101, -2, 402);
        orTrigTimePlot = aida.histogram1D(RAW_ECAL_HITS + " : Trigger Time, Or",
                1024, 0, 4096);

        topTimePlot2D = aida.histogram2D(RAW_ECAL_HITS
                + " : Hit Time vs. Trig Time, Top", 100, 0, 100 * 4.0, 101, -2, 402);
        botTimePlot2D = aida.histogram2D(RAW_ECAL_HITS
                + " : Hit Time vs. Trig Time, Bottom", 100, 0, 100 * 4.0, 101, -2, 402);
        orTimePlot2D = aida.histogram2D(RAW_ECAL_HITS
                + " : Hit Time vs. Trig Time, Or", 100, 0, 100 * 4.0, 101, -2, 402);

        hitEnergyPlot = aida.histogram1D(RAW_ECAL_HITS + " : Hit Energy", 100,
                -0.1, maxE);
        hitMaxEnergyPlot = aida.histogram1D(RAW_ECAL_HITS
                + " : Maximum Hit Energy In Event", 100, -0.1, maxE);

        tree.cd(ECAL_CLUSTERS_DIR);
        clusterCountDrawPlot = aida.histogram2D(ECAL_CLUSTERS
                + " : Cluster Rate KHz", 47, -23.5, 23.5, 11, -5.5, 5.5);
        clusterCountPlot = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Count per Event", 10, -0.5, 9.5);
        clusterSizePlot = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Size", 15, -0.5, 15.5);
        clusterEnergyPlot = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Energy", 100, -0.1, maxE);
        clusterMaxEnergyPlot = aida.histogram1D(ECAL_CLUSTERS
                + " : Maximum Cluster Energy in Event", 100, -0.1, maxE);
        edgePlot = aida.histogram2D(ECAL_CLUSTERS + " : Weighted Cluster",
                47, -23.25, 23.25, 11, -5.25, 5.25);
        clusterTimes = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Time Mean", 400, 0, 4.0 * 100);
        clusterTimeSigma = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Time Sigma", 100, 0, 40);

        tree.cd(ECAL_PAIRS_DIR);
        pairEnergySum = aida.histogram1D("Pair Energy Sum Distribution", 176, 0.0, maxE);
        pairEnergyDifference = aida.histogram1D("Pair Energy Difference Distribution", 176, 0.0, 2.2);
        pairCoplanarity = aida.histogram1D("Pair Coplanarity Distribution", 90, 0.0, 180.0);
        pairEnergySlope = aida.histogram1D("Pair Energy Slope Distribution", 150, 0.0, 3.0);
        pairEnergyPositionMeanX = aida.histogram1D("Cluster Pair Weighted Energy Position (x-Index)", 100, -250, 250);
        pairEnergyPositionMeanY = aida.histogram1D("Cluster Pair Weighted Energy Position (y-Index)", 100, -100, 100);
        pairMassSameHalf = aida.histogram1D("Cluster Pair Mass: Same Half", 100, 0.0, 0.35);
        pairMassOppositeHalf = aida.histogram1D("Cluster Pair Mass: Top Bottom", 100, 0.0, 0.35);
        pairMassSameHalfFid = aida.histogram1D("Cluster Pair Mass: Same Half Fiducial", 100, 0.0, 0.35);
        pairMassOppositeHalfFid = aida.histogram1D("Cluster Pair Mass: Top Bottom Fiducial", 100, 0.0, 0.35);
        tree.cd(TEMP_DIR);
        hitCountFillPlot = makeCopy(hitCountDrawPlot);
        clusterCountFillPlot = makeCopy(clusterCountDrawPlot);
        /**
         * Turn off aggregation for occupancy/rate plots (JM)
         */
        clusterCountFillPlot.annotation().addItem("aggregate", "false");
        clusterCountDrawPlot.annotation().addItem("aggregate", "false");
        occupancyDrawPlot.annotation().addItem("aggregate", "false");
        hitCountFillPlot.annotation().addItem("aggregate", "false");
        hitCountDrawPlot.annotation().addItem("aggregate", "false");

        /*
         * SVT RAW Occupancy plots
         */
        tree.mkdirs(SVTOCC_DIR);
        tree.cd(SVTOCC_DIR);
        tree.mkdirs(SVTMAX_DIR);
        for (HpsSiSensor sensor : sensors) {
            tree.cd(SVTOCC_DIR);
            occupancyMap.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), new int[640]);
            occupancyPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), aida
                    .histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Occupancy", 640, 0, 640));
            if (this.enableMaxSamplePlots) {               
                tree.cd(SVTMAX_DIR);
                maxSamplePositionPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), aida.histogram1D(
                        SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Max Sample Number", 6, -0.5, 5.5));
            }
        }
        /**
         * Turn off aggregation for occupancy plots (JM)
         */
        for (Entry<String, IHistogram1D> entry : occupancyPlots.entrySet()) {
            entry.getValue().annotation().addItem("aggregate", "false");
        }

        /*
         * SVT Hits
         */
        tree.mkdirs(SVTRAW_DIR);
        tree.mkdirs(SVTT0_DIR);

        for (HpsSiSensor sensor : sensors) {
            tree.cd(SVTRAW_DIR);
            hitsPerSensorPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    aida.histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Raw Hits", 50, 0, 50));
            hitsPerSensor.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), new int[1]);
            tree.cd(SVTT0_DIR);
            t0Plots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    aida.histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - t0", 100, -100, 100.0));
        }
        tree.mkdirs(SVTHITS_DIR);
        tree.cd(SVTHITS_DIR);
        hitCountPlots.put("Raw hit counts", aida.histogram1D("Raw hit counts", 100, 0, 500));
        hitCountPlots.put("SVT top raw hit counts", aida.histogram1D("SVT top raw hit counts", 100, 0, 300));
        hitCountPlots.put("SVT bottom raw hit counts", aida.histogram1D("SVT bottom raw hit counts", 100, 0, 300));
        /*
         * Tracking plots
         */

        tree.mkdirs(TRACKER_DIR);
        tree.cd(TRACKER_DIR);

        tracksPerEventH1D = aida.histogram1D("Tracks Per Event", 20, 0., 10.);
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
        if (enableTrackTimesPerSensorPlots) {
            tree.mkdirs(TRACKTIMEHOT_DIR);
            tree.mkdirs(TRACKTIMEDT_DIR);
            tree.mkdirs(TRACKTIMEDTVSPHASE_DIR);

            for (HpsSiSensor sensor : sensors) {
                tree.cd(TRACKTIMEHOT_DIR);
                trackHitT0.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                        aida.histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - track hit t0", 100, minTime, maxTime));
                tree.cd(TRACKTIMEDT_DIR);
                trackHitDt.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                        aida.histogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - track hit dt", 100, minTime, maxTime));
                tree.cd(TRACKTIMEDTVSPHASE_DIR);
                trackHit2D.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                        aida.histogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - trigger phase vs dt", 80, -20, 20.0, 6, 0, 24.0));
                //trackHitDtChan.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                //        aida.histogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - dt vs position", 200, -20, 20, 50, -20, 20.0));
            }
        }
        tree.cd(TRACKTIME_DIR);
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
        tree.mkdirs(ELECTRON_DIR);
        tree.mkdirs(POSITRON_DIR);
        tree.mkdirs(V01D_DIR);
        tree.mkdirs(V02D_DIR);
        tree.cd(ELECTRON_DIR);
        nEle = aida.histogram1D("Number of Electrons per event", 5, 0, 5);
        elePx = aida.histogram1D("Electron Px (GeV)", 50, -0.2, 0.2);
        elePy = aida.histogram1D("Electron Py (GeV)", 50, -0.2, 0.2);
        elePz = aida.histogram1D("Electron Pz (GeV)", 50, 0.0, pMax);
        eleProjXYEcalMatch = aida.histogram2D("Electron ECal Projection: Matched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        eleProjXYEcalNoMatch = aida.histogram2D("Electron ECal Projection: Unmatched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        tree.cd(POSITRON_DIR);
        nPos = aida.histogram1D("Number of Positrons per event", 5, 0, 5);
        posPx = aida.histogram1D("Positron Px (GeV)", 50, -0.2, 0.2);
        posPy = aida.histogram1D("Positron Py (GeV)", 50, -0.2, 0.2);
        posPz = aida.histogram1D("Positron Pz (GeV)", 50, 0.0, pMax);
        posProjXYEcalMatch = aida.histogram2D("Positron ECal Projection: Matched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        posProjXYEcalNoMatch = aida.histogram2D("Positron ECal Projection: Unmatched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);

//        nPhot = aida.histogram1D("Number of Photons per event", 5, 0, 5);
//        photEne = aida.histogram1D("Photon Energy (GeV)", 50, 0.0, pMax);
//        photXYECal = aida.histogram2D("ECal Position", 50, -300, 400, 50, -ecalYRange, ecalYRange);
        /* V0 Quantities */
 /* Mass, vertex, chi^2 of fit */
 /* unconstrained  */
        tree.cd(V01D_DIR);
        nV0 = aida.histogram1D("Number of V0 per event", 5, 0, 5);
        unconMass = aida.histogram1D("Unconstrained Mass (GeV)", 100, 0, 0.200);
        unconVx = aida.histogram1D("Unconstrained Vx (mm)", 50, -5, 5);
        unconVy = aida.histogram1D("Unconstrained Vy (mm)", 50, -1.0, 1.0);
        unconVz = aida.histogram1D("Unconstrained Vz (mm)", 50, -15, 10);
        unconChi2 = aida.histogram1D("Unconstrained Chi2", 50, 0, 25);
        tree.cd(V02D_DIR);
        pEleVspPos = aida.histogram2D("P(e) vs P(p)", 50, 0, 2.5, 50, 0, 2.5);
        pyEleVspyPos = aida.histogram2D("Py(e) vs Py(p)", 50, -0.1, 0.1, 50, -0.1, 0.1);
        pxEleVspxPos = aida.histogram2D("Px(e) vs Px(p)", 50, -0.1, 0.1, 50, -0.1, 0.1);
        massVsVtxZ = aida.histogram2D("Mass vs Vz", 50, 0, 0.15, 50, -10, 10);

        tree.mkdirs(PERF_DIR);
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

        int nhits = 0;
        int chits[] = new int[11 * 47];

        int orTrigTime = 4097;
        int topTrigTime = 4097;
        int botTrigTime = 4097;

        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            if (!triggerList.isEmpty()) {
                GenericObject triggerData = triggerList.get(0);

                if (triggerData instanceof SSPData) {
                    // TODO: TOP, BOTTOM, OR, and AND triggers are test
                    // run specific parameters and are not supported
                    // by SSPData.
                    orTrigTime = 0; // ((SSPData)triggerData).getOrTrig();
                    topTrigTime = 0; // ((SSPData)triggerData).getTopTrig();
                    botTrigTime = 0; // ((SSPData)triggerData).getBotTrig();

                    orTrigTimePlot.fill(orTrigTime);
                    topTrigTimePlot.fill(topTrigTime);
                    botTrigTimePlot.fill(botTrigTime);

                }

            }// end if triggerList isEmpty
        }

        if (event.hasCollection(CalorimeterHit.class, RAW_ECAL_HITS)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, RAW_ECAL_HITS);
            hitCountPlot.fill(hits.size());

            double maxEnergy = 0;
            double topTime = Double.POSITIVE_INFINITY;
            double botTime = Double.POSITIVE_INFINITY;
            double orTime = Double.POSITIVE_INFINITY;
            for (CalorimeterHit hit : hits) {

                int column = hit.getIdentifierFieldValue("ix");
                int row = hit.getIdentifierFieldValue("iy");
                int id = EcalMonitoringUtilities.getHistoIDFromRowColumn(row, column);
                hitCountFillPlot.fill(column, row);
                {
                    chits[id]++;
                    nhits++;
                }
                hitEnergyPlot.fill(hit.getRawEnergy());
                hitTimePlot.fill(hit.getTime());

                if (hit.getTime() < orTime) {
                    orTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") > 0 && hit.getTime() < topTime) {
                    topTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") < 0 && hit.getTime() < botTime) {
                    botTime = hit.getTime();
                }
                if (hit.getRawEnergy() > maxEnergy) {
                    maxEnergy = hit.getRawEnergy();
                }
            }

            if (orTime != Double.POSITIVE_INFINITY) {
                orTimePlot.fill(orTime);
                orTimePlot2D.fill(orTime, orTrigTime);
            }
            if (topTime != Double.POSITIVE_INFINITY) {
                topTimePlot.fill(topTime);
                topTimePlot2D.fill(topTime, topTrigTime);
            }
            if (botTime != Double.POSITIVE_INFINITY) {
                botTimePlot.fill(botTime);
                botTimePlot2D.fill(botTime, botTrigTime);
            }
            hitMaxEnergyPlot.fill(maxEnergy);
        }

        if (nhits > 0) {
            for (int ii = 0;
                    ii < (11 * 47); ii++) {
                occupancyFill[ii] += (1. * chits[ii]) / nhits;
            }
        }

        // calorimeter and the bottom.
        List<Cluster> topList = new ArrayList<Cluster>();
        List<Cluster> bottomList = new ArrayList<Cluster>();

        // Track the highest energy cluster in the event.
        double maxEnergy = 0.0;
        if (event.hasCollection(Cluster.class, ECAL_CLUSTERS)) {
            List<Cluster> clusters = event.get(Cluster.class, ECAL_CLUSTERS);
            for (Cluster cluster : clusters) {
                clusterCountFillPlot.fill(cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"), cluster
                        .getCalorimeterHits().get(0).getIdentifierFieldValue("iy"));

                if (cluster.getEnergy() > maxEnergy) {
                    maxEnergy = cluster.getEnergy();
                }

                // Get the list of calorimeter hits and its size.
                List<CalorimeterHit> hitList = cluster.getCalorimeterHits();
                int hitCount = hitList.size();

                // Track cluster statistics.
                double xEnergyWeight = 0.0;
                double yEnergyWeight = 0.0;
                double[] hitTimes = new double[hitCount];
                double totalHitEnergy = 0.0;

                // Iterate over the hits and extract statistics from them.
                for (int hitIndex = 0; hitIndex < hitCount; hitIndex++) {
                    hitTimes[hitIndex] = hitList.get(hitIndex).getTime();
                    totalHitEnergy += hitList.get(hitIndex).getRawEnergy();
                    xEnergyWeight += (hitList.get(hitIndex).getRawEnergy() * hitList.get(hitIndex)
                            .getIdentifierFieldValue("ix"));
                    yEnergyWeight += (hitList.get(hitIndex).getRawEnergy() * hitList.get(hitIndex)
                            .getIdentifierFieldValue("iy"));
                }

                // If the cluster energy exceeds zero, plot the cluster
                // statistics.
                if (cluster.getEnergy() > 0) {
                    clusterSizePlot.fill(hitCount);
                    clusterTimes.fill(StatUtils.mean(hitTimes, 0, hitCount));
                    clusterTimeSigma.fill(Math.sqrt(StatUtils.variance(hitTimes, 0, hitCount)));
                    edgePlot.fill(xEnergyWeight / totalHitEnergy, yEnergyWeight / totalHitEnergy);
                }

                // Fill the single cluster plots.
                clusterEnergyPlot.fill(cluster.getEnergy());

                // Cluster pairs are formed from all top/bottom cluster
                // combinations. To create these pairs, separate the
                // clusters into two lists based on their y-indices.
                if (cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix") > 0) {
                    topList.add(cluster);
                } else {
                    bottomList.add(cluster);
                }
            }
            // Populate the event plots.
            clusterCountPlot.fill(clusters.size());
            if (maxEnergy > 0) {
                clusterMaxEnergyPlot.fill(maxEnergy);
            }

            // Create a list to store cluster pairs.
            List<Cluster[]> pairList = new ArrayList<Cluster[]>(topList.size() * bottomList.size());

            // Form pairs from all possible combinations of clusters
            // from the top and bottom lists.
            for (Cluster topCluster : topList) {
                for (Cluster bottomCluster : bottomList) {
                    // Make a cluster pair array.
                    Cluster[] pair = new Cluster[2];

                    // The lower energy cluster goes in the second slot.
                    if (topCluster.getEnergy() > bottomCluster.getEnergy()) {
                        pair[0] = topCluster;
                        pair[1] = bottomCluster;
                    } else {
                        pair[0] = bottomCluster;
                        pair[1] = topCluster;
                    }

                    // Add the pair to the pair list.
                    pairList.add(pair);
                }
            }

            // Iterate over each pair and calculate the pair cut values.
            for (Cluster[] pair : pairList) {
                // Get the energy slope value.
                double energySumValue = TriggerModule.getValueEnergySum(pair);
                double energyDifferenceValue = TriggerModule.getValueEnergyDifference(pair);
                double energySlopeValue = TriggerModule.getValueEnergySlope(pair, 0.005500);
                double coplanarityValue = TriggerModule.getValueCoplanarity(pair);
                double xMean = ((pair[0].getEnergy() * pair[0].getPosition()[0]) + (pair[1].getEnergy() * pair[1]
                        .getPosition()[0])) / energySumValue;
                double yMean = ((pair[0].getEnergy() * pair[0].getPosition()[1]) + (pair[1].getEnergy() * pair[1]
                        .getPosition()[1])) / energySumValue;

                // Populate the cluster pair plots.
                pairEnergySum.fill(energySumValue, 1);
                ;
                pairEnergyDifference.fill(energyDifferenceValue, 1);
                pairEnergySlope.fill(energySlopeValue, 1);
                pairCoplanarity.fill(coplanarityValue, 1);
                pairEnergyPositionMeanX.fill(xMean);
                pairEnergyPositionMeanY.fill(yMean);
                pairMassOppositeHalf.fill(getClusterPairMass(pair));

                int idx0 = pair[0].getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                int idx1 = pair[1].getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                if (Math.abs(idx0) > 2 && Math.abs(idx1) > 2) {
                    pairMassOppositeHalfFid.fill(getClusterPairMass(pair));
                }

            }
            // Create a list to store cluster pairs.
            List<Cluster[]> pairListSameHalf = new ArrayList<Cluster[]>();

            // Form pairs from all possible combinations of clusters
            // from the top and bottom lists.
            for (Cluster topCluster : topList) {
                for (Cluster topCluster2 : topList) {
                    // Make a cluster pair array.
                    Cluster[] pair = new Cluster[2];

                    // The lower energy cluster goes in the second slot.
                    if (topCluster.getEnergy() > topCluster2.getEnergy()) {
                        pair[0] = topCluster;
                        pair[1] = topCluster2;
                    } else {
                        pair[0] = topCluster2;
                        pair[1] = topCluster;
                    }

                    // Add the pair to the pair list.
                    pairListSameHalf.add(pair);
                }
            }

            for (Cluster cluster : bottomList) {
                for (Cluster cluster2 : bottomList) {
                    // Make a cluster pair array.
                    Cluster[] pair = new Cluster[2];

                    // The lower energy cluster goes in the second slot.
                    if (cluster.getEnergy() > cluster2.getEnergy()) {
                        pair[0] = cluster;
                        pair[1] = cluster2;
                    } else {
                        pair[0] = cluster2;
                        pair[1] = cluster;
                    }

                    // Add the pair to the pair list.
                    pairListSameHalf.add(pair);
                }
            }
            // Iterate over each pair and calculate the pair cut values.
            for (Cluster[] pair : pairListSameHalf) {
                // Get the energy slope value.
                pairMassSameHalf.fill(getClusterPairMass(pair));
                int idx0 = pair[0].getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                int idx1 = pair[1].getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                if (Math.abs(idx0) > 2 && Math.abs(idx1) > 2) {
                    pairMassSameHalfFid.fill(getClusterPairMass(pair));
                }
            }

        } else {
            clusterCountPlot.fill(0);
        }

        thisTime = System.currentTimeMillis() / 1000;
        thisEventN = event.getEventNumber();
        thisEventTime = event.getTimeStamp() / 1E9;
        if ((thisTime - prevTime) > eventRefreshRate) {
            double scale = 1.;

            if (NoccupancyFill > 0) {
                scale = (thisEventN - prevEventN) / NoccupancyFill;
                scale = scale / (thisEventTime - prevEventTime);
                scale /= 1000.; // do KHz
            }
            // System.out.println("Event: "+thisEventN+" "+prevEventN);
            // System.out.println("Time: "+thisEventTime+" "+prevEventTime);
            // System.out.println("Monitor: "+thisTime+" "+prevTime+" "+NoccupancyFill);
            if (scale > 0) {
                hitCountFillPlot.scale(scale);
                clusterCountFillPlot.scale(scale);
                redraw();
            }
            prevTime = thisTime;
            prevEventN = thisEventN;
            prevEventTime = thisEventTime;
            NoccupancyFill = 0;
        } else {
            NoccupancyFill++;
        }

        // If the event doesn't have a collection of RawTrackerHit's, skip it.
        if (!event.hasCollection(RawTrackerHit.class,
                RAW_TRACKER_HITS
        )) {
            return;
        }
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, RAW_TRACKER_HITS);
        //System.out.println(rawHits.size());
        List<LCRelation> fittedHits = event.get(LCRelation.class, FITTED_HITS);

        fittedTrackerHitsPerEventH1D.fill(event.get(LCRelation.class,
                FITTED_HITS
        ).size()
        );

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
            for (int sampleN = 0; sampleN < 6; sampleN++) {
                if (adcValues[sampleN] > maxAmplitude) {
                    maxAmplitude = adcValues[sampleN];
                    maxSamplePositionFound = sampleN;
                }
            }
            if (maxSamplePosition == -1 || maxSamplePosition == maxSamplePositionFound) {
                occupancyMap.get(
                        SvtPlotUtils.fixSensorNumberLabel(((HpsSiSensor) rawHit.getDetectorElement()).getName()))[rawHit
                        .getIdentifierFieldValue("strip")]++; // System.out.println("Filling occupancy");
            }
            if (this.enableMaxSamplePlots) {
                maxSamplePositionPlots
                        .get(SvtPlotUtils.fixSensorNumberLabel(((HpsSiSensor) rawHit.getDetectorElement()).getName()))
                        .fill(maxSamplePositionFound);
            }
        }
        // Plot strip occupancies.
        if (eventCount % eventRefreshRate == 0) {
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
            if (cutOutLowChargeHits) {
                if (amplitude / DopedSilicon.ENERGY_EHPAIR < hitChargeCut) {
                    continue;
                }
            }
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

            if (hitsPerSensor.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[0] > 0) {
                if (sensor.isTopLayer()) {
                    topLayersHit[sensor.getLayerNumber() - 1]++;
                    topEventHitCount += hitCount;
                } else {
                    botLayersHit[sensor.getLayerNumber() - 1]++;
                    botEventHitCount += hitCount;
                }
            }
        }

        hitCountPlots.get("Raw hit counts").fill(eventHitCount);
        hitCountPlots.get("SVT top raw hit counts").fill(topEventHitCount);
        hitCountPlots.get("SVT bottom raw hit counts").fill(botEventHitCount);

        List<Track> tracks = event.get(Track.class, trackColName);

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

        for (Track track : tracks) {
            int trackModule;
            String moduleName = "Top";
            if (track.getTrackerHits().get(0).getPosition()[1] > 0) {
                trackModule = 0;
            } else {
                moduleName = "Bottom";
                trackModule = 1;
            }
            double minTime = Double.POSITIVE_INFINITY;
            double maxTime = Double.NEGATIVE_INFINITY;
            int hitCount = 0;
            double trackTime = 0;

            if (isSeedTracker) {
                for (TrackerHit hitCross : track.getTrackerHits()) {
                    for (HelicalTrackStrip hit : ((HelicalTrackCross) hitCross).getStrips()) {
                        SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.rawhits().get(0)).getDetectorElement();
                        if (enableTrackTimesPerSensorPlots) {
                            trackHitT0.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.time());
                        }
                        trackTime += hit.time();
                        hitCount++;
                        if (hit.time() > maxTime) {
                            maxTime = hit.time();
                        }
                        if (hit.time() < minTime) {
                            minTime = hit.time();
                        }
                    }
                }
                trackTimeMinMax.get(moduleName).fill(minTime, maxTime);
                trackTimeRange.get(moduleName).fill(maxTime - minTime);
                trackTime /= hitCount;
                trackT0.get(moduleName).fill(trackTime);
                trackTrigTime.get(moduleName).fill(trackTime, trigTime);
                if (enableTrackTimesPerSensorPlots) {
                    for (TrackerHit hitCross : track.getTrackerHits()) {
                        for (HelicalTrackStrip hit : ((HelicalTrackCross) hitCross).getStrips()) {
                            int layer = hit.layer();
                            int module = ((RawTrackerHit) hit.rawhits().get(0)).getIdentifierFieldValue("module");
                            SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.rawhits().get(0)).getDetectorElement();
                            trackHitDt.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.time() - trackTime);
                            trackHit2D.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.time() - trackTime, event.getTimeStamp() % 24);
                        }
                    }
                }
            } else {
                for (TrackerHit hitTH : track.getTrackerHits()) {

                    SiTrackerHitStrip1D hit = (SiTrackerHitStrip1D) hitTH;
                    SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                    if (enableTrackTimesPerSensorPlots) {
                        trackHitT0.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime());
                    }
                    trackTime += hit.getTime();
                    hitCount++;
                    if (hit.getTime() > maxTime) {
                        maxTime = hit.getTime();
                    }
                    if (hit.getTime() < minTime) {
                        minTime = hit.getTime();
                    }
                }
                trackTimeMinMax.get(moduleName).fill(minTime, maxTime);
                trackTimeRange.get(moduleName).fill(maxTime - minTime);
                trackTime /= hitCount;
                trackT0.get(moduleName).fill(trackTime);
                trackTrigTime.get(moduleName).fill(trackTime, trigTime);
                if (enableTrackTimesPerSensorPlots) {
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
                if (fsp.getClusters().size() != 0) {
                    eleProjXYEcalMatch.fill(tPos.y(), tPos.z());
                } else {
                    eleProjXYEcalNoMatch.fill(tPos.y(), tPos.z());
                }
            } else if (charge > 0) {
                posCnt++;
                Hep3Vector mom = fsp.getMomentum();
                posPx.fill(mom.x());
                posPy.fill(mom.y());
                posPz.fill(mom.z());
                TrackState stateAtEcal = TrackUtils.getTrackStateAtECal((fsp.getTracks().get(0)));
                Hep3Vector tPos = new BasicHep3Vector(stateAtEcal.getReferencePoint());
                if (fsp.getClusters().size() != 0) {
                    posProjXYEcalMatch.fill(tPos.y(), tPos.z());// tracking frame!
                } else {
                    posProjXYEcalNoMatch.fill(tPos.y(), tPos.z());
                }
//            } else if (fsp.getClusters().size() != 0) {
//                photCnt++;
//                Cluster clu = fsp.getClusters().get(0);
//                photEne.fill(clu.getEnergy());
//                photXYECal.fill(clu.getPosition()[0], clu.getPosition()[1]);
            } else {
                System.out.println("This FSP had no tracks or clusters???");
            }
        }

        nEle.fill(eleCnt);

        nPos.fill(posCnt);

//        nPhot.fill(photCnt);

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

    private static Map<Integer, Hep3Vector> createStripPositionMap(HpsSiSensor sensor) {
        Map<Integer, Hep3Vector> positionMap = new HashMap<Integer, Hep3Vector>();
        for (ChargeCarrier carrier : ChargeCarrier.values()) {
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
        }
        return positionMap;
    }

    private void clearHitMaps() {
        for (HpsSiSensor sensor : sensors) {
            hitsPerSensor.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[0] = 0;
        }
    }

    private double getClusterPairMass(Cluster clu1, Cluster clu2) {
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

        if (evtmass > 0) {
            return Math.sqrt(evtmass);
        } else {
            return -99;
        }
    }

    private double getMomentum(Track trk) {

        double px = trk.getTrackStates().get(0).getMomentum()[0];
        double py = trk.getTrackStates().get(0).getMomentum()[1];
        double pz = trk.getTrackStates().get(0).getMomentum()[2];
        return Math.sqrt(px * px + py * py + pz * pz);
    }

    private IHistogram2D makeCopy(IHistogram2D hist) {
        return aida.histogram2D(hist.title() + "_copy", hist.xAxis().bins(), hist.xAxis().lowerEdge(), hist.xAxis()
                .upperEdge(), hist.yAxis().bins(), hist.yAxis().lowerEdge(), hist.yAxis().upperEdge());
    }

    void redraw() {
        hitCountDrawPlot.reset();
        hitCountDrawPlot.add(hitCountFillPlot);

        hitCountFillPlot.reset();

        clusterCountDrawPlot.reset();
        clusterCountDrawPlot.add(clusterCountFillPlot);

        clusterCountFillPlot.reset();

        occupancyDrawPlot.reset();
        for (int id = 0; id < (47 * 11); id++) {
            int row = EcalMonitoringUtilities.getRowFromHistoID(id);
            int column = EcalMonitoringUtilities.getColumnFromHistoID(id);
            double mean = occupancyFill[id] / NoccupancyFill;

            occupancyFill[id] = 0;
            if ((row != 0) && (column != 0) && (!EcalMonitoringUtilities.isInHole(row, column))) {
                occupancyDrawPlot.fill(column, row, mean);
            }
        }

    }

    public double getClusterPairMass(Cluster[] pair) {
        double x0 = pair[0].getPosition()[0];
        double y0 = pair[0].getPosition()[1];
        double z0 = pair[0].getPosition()[2];
        double x1 = pair[1].getPosition()[0];
        double y1 = pair[1].getPosition()[1];
        double z1 = pair[1].getPosition()[2];
        double e0 = pair[0].getEnergy();
        double e1 = pair[1].getEnergy();
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

        if (evtmass > 0) {
            return Math.sqrt(evtmass);
        } else {
            return -99;
        }
    }
}
