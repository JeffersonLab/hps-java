package org.hps.monitoring.drivers.monitoring2021.headless;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.recon.tracking.SvtPlotUtils;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

/**
 * Monitoring driver that looks at the SVT cluster charge.
 */
public class SvtClustersHeadless extends Driver {

    // Plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory(); 
    private IHistogramFactory histogramFactory = null;  

    private static int nmodlayers = 7;

    // Histogram Maps
    private static Map<String, IHistogram1D> clusterChargePlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> singleHitClusterChargePlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> clusterTimePlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram2D> hitTimeTrigTimePlots = new HashMap<String, IHistogram2D>();

    private static IHistogram1D[][] hitTimeTrigTimePlots1D = new IHistogram1D[nmodlayers][2];
    private static IHistogram2D[][] hitTimeTrigTimePlots2D = new IHistogram2D[nmodlayers][2];
    private static final Map<String, IHistogram1D> clusterYPlots = new HashMap<String, IHistogram1D>();
    private SvtTimingConstants timingConstants;

    private static final int TOP = 0;
    private static final int BOTTOM = 1;

    private List<HpsSiSensor> sensors;
    // private Map<RawTrackerHit, FittedRawTrackerHit> fittedRawTrackerHitMap
    // = new HashMap<RawTrackerHit, FittedRawTrackerHit>();

    // Detector name
    private static final String SUBDETECTOR_NAME = "Tracker";

    // Collections
    private String clusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";

    private int runNumber = -1;

    private boolean saveRootFile = true;

    private boolean dropSmallHitEvents = true;

    private boolean cutOutLowChargeClusters = false;
    private double clusterChargeCut = 400;

    public void setDropSmallHitEvents(boolean dropSmallHitEvents) {
        this.dropSmallHitEvents = dropSmallHitEvents;
    }

    public void setClusterChargeCut(double clusterCharge) {
        this.clusterChargeCut = clusterCharge;
    }

    public void setCutOutLowChargeClusters(boolean cutOutLowChargeClusters) {
        this.cutOutLowChargeClusters = cutOutLowChargeClusters;
    }

    public void setSaveRootFile(boolean saveRootFile) {
        this.saveRootFile = saveRootFile;
    }

    protected void detectorChanged(Detector detector) {

        // Get the HpsSiSensor objects from the geometry
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);

        timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
        if (sensors.size() == 0)
            throw new RuntimeException("No sensors were found in this detector.");

        // // If the tree already exist, clear all existing plots of any old data
        // // they might contain.
        // if (tree != null) {
        // this.resetPlots();
        // return;
        // }
        tree = analysisFactory.createTreeFactory().create();
        histogramFactory = analysisFactory.createHistogramFactory(tree);

        for (HpsSiSensor sensor : sensors) {

            clusterChargePlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Cluster Charge", 100, 0, 5000));
            singleHitClusterChargePlots
                    .put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())
                            + " - Single Hit Cluster Charge", 100, 0, 5000));
            clusterTimePlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Cluster Time", 100, -75, 75));

            clusterYPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Cluster Y", 100, 0, 25.0));         
        }

        hitTimeTrigTimePlots.put("Top",
                histogramFactory.createHistogram2D("Top Cluster Time vs. Trigger Phase", 100, -75, 50, 6, -12, 12));
        
        hitTimeTrigTimePlots.put("Bottom",
                histogramFactory.createHistogram2D("Bottom Cluster Time vs. Trigger Phase", 100, -75, 50, 6, -12, 12));

        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 2; j++) {
                hitTimeTrigTimePlots1D[i][j] = histogramFactory.createHistogram1D(
                        String.format("Cluster Time for Phase %d, %s", i, j == TOP ? "Top" : "Bottom"), 100, -75, 50);              
                hitTimeTrigTimePlots2D[i][j] = histogramFactory.createHistogram2D(
                        String.format("Cluster Amplitude vs. Time for Phase %d, %s", i, j == TOP ? "Top" : "Bottom"),
                        100, -75, 50, 100, 0, 5000.0);              
            }

    }

    public void process(EventHeader event) {

        if (runNumber == -1)
            runNumber = event.getRunNumber();

        // If the event doesn't contain any clusters, skip it
        if (!event.hasCollection(SiTrackerHitStrip1D.class, clusterCollectionName))
            return;

        if (event.hasCollection(RawTrackerHit.class, "SVTRawTrackerHits")) {
            // Get RawTrackerHit collection from event.
            List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");

            if (dropSmallHitEvents && SvtPlotUtils.countSmallHits(rawHits) > 3)
                return;
        }

        // Get the list of clusters in the event
        List<SiTrackerHitStrip1D> clusters = event.get(SiTrackerHitStrip1D.class, clusterCollectionName);

        for (SiTrackerHitStrip1D cluster : clusters) {

            // Get the sensor associated with this cluster
            HpsSiSensor sensor = (HpsSiSensor) cluster.getSensor();
            double absClY = Math.abs(cluster.getPosition()[1]);
            // Fill all plots
            clusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(cluster.getdEdx() / DopedSilicon.ENERGY_EHPAIR);

            clusterYPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(absClY);

            if (cluster.getRawHits().size() == 1)
                singleHitClusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(cluster.getdEdx() / DopedSilicon.ENERGY_EHPAIR);
            double trigPhase = (((event.getTimeStamp() - 4 * timingConstants.getOffsetPhase()) % 24) - 12);
            if (cutOutLowChargeClusters)
                if (cluster.getdEdx() / DopedSilicon.ENERGY_EHPAIR < clusterChargeCut)
                    continue;
            clusterTimePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(cluster.getTime());
            if (sensor.isTopLayer()) {
                hitTimeTrigTimePlots1D[(int) ((event.getTimeStamp() / 4) % 6)][TOP].fill(cluster.getTime());
                hitTimeTrigTimePlots2D[(int) ((event.getTimeStamp() / 4) % 6)][TOP].fill(cluster.getTime(),
                        cluster.getdEdx() / DopedSilicon.ENERGY_EHPAIR);
                hitTimeTrigTimePlots.get("Top").fill(cluster.getTime(), trigPhase);
            } else {
                hitTimeTrigTimePlots1D[(int) ((event.getTimeStamp() / 4) % 6)][BOTTOM].fill(cluster.getTime());
                hitTimeTrigTimePlots2D[(int) ((event.getTimeStamp() / 4) % 6)][BOTTOM].fill(cluster.getTime(),
                        cluster.getdEdx() / DopedSilicon.ENERGY_EHPAIR);
                hitTimeTrigTimePlots.get("Bottom").fill(cluster.getTime(), trigPhase);
            }
        }
    }

    public void endOfData() {
        if (saveRootFile) {
            String rootFile = "run" + runNumber + "_cluster_analysis.root";
            RootFileStore store = new RootFileStore(rootFile);
            try {
                store.open();
                store.add(tree);
                store.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
