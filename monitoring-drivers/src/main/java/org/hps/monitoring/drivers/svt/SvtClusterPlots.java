package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

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

/**
 * Monitoring driver that looks at the SVT cluster charge.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 */
public class SvtClusterPlots extends Driver {

    // TODO: Add documentation
    //static {
    //    hep.aida.jfree.AnalysisFactory.register();
    //}
    // Plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("SVT Clusters");
    private IHistogramFactory histogramFactory = null;
    private static Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();

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
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";

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

    private int computePlotterRegion(HpsSiSensor sensor) {

        if (sensor.getLayerNumber() < 7)
            if (sensor.isTopLayer())
                return 6 * (sensor.getLayerNumber() - 1);
            else
                return 6 * (sensor.getLayerNumber() - 1) + 1;
        else if (sensor.isTopLayer())
            if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE)
                return 6 * (sensor.getLayerNumber() - 7) + 2;
            else
                return 6 * (sensor.getLayerNumber() - 7) + 3;
        else if (sensor.isBottomLayer())
            if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE)
                return 6 * (sensor.getLayerNumber() - 7) + 4;
            else
                return 6 * (sensor.getLayerNumber() - 7) + 5;
        return -1;
    }

    IPlotterStyle createStyle(HpsSiSensor sensor, String xAxisTitle, String yAxisTitle) {

        // Create a default style
        IPlotterStyle style = this.plotterFactory.createPlotterStyle();

        // Set the style of the X axis
        style.xAxisStyle().setLabel(xAxisTitle);
        style.xAxisStyle().labelStyle().setFontSize(14);
        style.xAxisStyle().setVisible(true);

        // Set the style of the Y axis
        style.yAxisStyle().setLabel(yAxisTitle);
        style.yAxisStyle().labelStyle().setFontSize(14);
        style.yAxisStyle().setVisible(true);

        // Turn off the histogram grid
        style.gridStyle().setVisible(false);

        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(4);
        style.dataStyle().fillStyle().setVisible(true);
        style.dataStyle().fillStyle().setOpacity(.30);

        if (sensor == null) {
            style.dataStyle().fillStyle().setColor("255, 38, 38, 1");
            style.dataStyle().outlineStyle().setColor("255, 38, 38, 1");
            style.dataStyle().fillStyle().setOpacity(.70);
        } else if (sensor.isTopLayer()) {
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        } else if (sensor.isBottomLayer()) {
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
        }
        style.dataStyle().errorBarStyle().setVisible(false);

        style.regionBoxStyle().backgroundStyle().setOpacity(.20);
        if (sensor != null && sensor.isAxial())
            style.regionBoxStyle().backgroundStyle().setColor("246, 246, 34, 1");

        // Turn off the legend
        style.legendBoxStyle().setVisible(false);

        return style;
    }

    /**
     * Clear all histograms of it's current data.
     */
    private void resetPlots() {

        // Clear the fitted raw hit map of old values
        // fittedRawTrackerHitMap.clear();
        // Since all plots are mapped to the name of a sensor, loop
        // through the sensors, get the corresponding plots and clear them.
        for (HpsSiSensor sensor : sensors) {
            clusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
            singleHitClusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
            clusterTimePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
        }

        for (IHistogram2D histogram : hitTimeTrigTimePlots.values())
            histogram.reset();

        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 2; j++) {
                hitTimeTrigTimePlots1D[i][j].reset();
                hitTimeTrigTimePlots2D[i][j].reset();
            }
    }

    // /**
    // * Method that creates a map between a fitted raw hit and it's corresponding
    // * raw fit
    // *
    // * @param fittedHits : List of fitted hits to map
    // */
    // private void mapFittedRawHits(List<FittedRawTrackerHit> fittedHits) {
    //
    // // Clear the fitted raw hit map of old values
    // fittedRawTrackerHitMap.clear();
    //
    // // Loop through all fitted hits and map them to their corresponding raw hits
    // for (FittedRawTrackerHit fittedHit : fittedHits) {
    // fittedRawTrackerHitMap.put(fittedHit.getRawTrackerHit(), fittedHit);
    // }
    // }
    //
    // /**
    // *
    // * @param rawHit
    // * @return
    // */
    // private FittedRawTrackerHit getFittedHit(RawTrackerHit rawHit) {
    // return fittedRawTrackerHitMap.get(rawHit);
    // }
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

        plotters.put("Cluster Amplitude: L0-L3", plotterFactory.create("Cluster Amplitude: L0-L3"));
        plotters.get("Cluster Amplitude: L0-L3").createRegions(4, 4);
        plotters.put("Cluster Amplitude: L4-L6", plotterFactory.create("Cluster Amplitude: L4-L6"));
        plotters.get("Cluster Amplitude: L4-L6").createRegions(6, 4);
        plotters.put("Cluster Time: L0-L3", plotterFactory.create("Cluster Time: L0-L3"));
        plotters.get("Cluster Time: L0-L3").createRegions(4, 4);
        plotters.put("Cluster Time: L4-L6", plotterFactory.create("Cluster Time: L4-L6"));
        plotters.get("Cluster Time: L4-L6").createRegions(6, 4);

        plotters.put("L0-L3 Cluster Y", plotterFactory.create("L0-L3 Cluster Y"));
        plotters.get("L0-L3 Cluster Y").createRegions(4, 4);
        plotters.put("L4-L6 Cluster Y", plotterFactory.create("L4-L6 Cluster Y"));
        plotters.get("L4-L6 Cluster Y").createRegions(6, 4);

        for (HpsSiSensor sensor : sensors) {

            clusterChargePlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Cluster Charge", 100, 0, 5000));
            singleHitClusterChargePlots
                    .put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())
                            + " - Single Hit Cluster Charge", 100, 0, 5000));
            clusterTimePlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Cluster Time", 100, -75, 75));

            if (sensor.getLayerNumber() < 9) {
                plotters.get("Cluster Amplitude: L0-L3")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Cluster Amplitude: L0-L3")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(singleHitClusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(null, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Cluster Time: L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterTimePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(null, "Cluster Time [ns]", ""));
            } else {
                plotters.get("Cluster Amplitude: L4-L6")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Cluster Amplitude: L4-L6")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(singleHitClusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(null, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Cluster Time: L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterTimePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(null, "Cluster Time [ns]", ""));
            }

            clusterYPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Cluster Y", 100, 0, 25.0));
            if (sensor.getLayerNumber() < 9)
                plotters.get("L0-L3 Cluster Y")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterYPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Hit Cluster abs(Y)", ""));
            else
                plotters.get("L4-L6 Cluster Y")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterYPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Hit Cluster abs(Y)", ""));
        }

        plotters.put("SVT-trigger timing top-bottom", plotterFactory.create("SVT-trigger timing top-bottom"));
        plotters.get("SVT-trigger timing top-bottom").createRegions(1, 2);

        hitTimeTrigTimePlots.put("Top",
                histogramFactory.createHistogram2D("Top Cluster Time vs. Trigger Phase", 100, -75, 50, 6, -12, 12));
        plotters.get("SVT-trigger timing top-bottom")
                .region(0)
                .plot(hitTimeTrigTimePlots.get("Top"), this.createStyle(null, "Cluster Time [ns]", "Trigger Phase[ns]"));
        hitTimeTrigTimePlots.put("Bottom",
                histogramFactory.createHistogram2D("Bottom Cluster Time vs. Trigger Phase", 100, -75, 50, 6, -12, 12));
        plotters.get("SVT-trigger timing top-bottom")
                .region(1)
                .plot(hitTimeTrigTimePlots.get("Bottom"),
                        this.createStyle(null, "Cluster Time [ns]", "Trigger Phase[ns]"));

        plotters.put("SVT-trigger timing by phase", plotterFactory.create("SVT-trigger timing by phase"));
        plotters.get("SVT-trigger timing by phase").createRegions(2, 6);

        plotters.put("SVT-trigger timing and amplitude by phase",
                plotterFactory.create("SVT-trigger timing and amplitude by phase"));
        plotters.get("SVT-trigger timing and amplitude by phase").createRegions(2, 6);

        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 2; j++) {
                hitTimeTrigTimePlots1D[i][j] = histogramFactory.createHistogram1D(
                        String.format("Cluster Time for Phase %d, %s", i, j == TOP ? "Top" : "Bottom"), 100, -75, 50);
                plotters.get("SVT-trigger timing by phase").region(i + 6 * j)
                        .plot(hitTimeTrigTimePlots1D[i][j], this.createStyle(null, "Cluster Time [ns]", ""));
                hitTimeTrigTimePlots2D[i][j] = histogramFactory.createHistogram2D(
                        String.format("Cluster Amplitude vs. Time for Phase %d, %s", i, j == TOP ? "Top" : "Bottom"),
                        100, -75, 50, 100, 0, 5000.0);
                plotters.get("SVT-trigger timing and amplitude by phase")
                        .region(i + 6 * j)
                        .plot(hitTimeTrigTimePlots2D[i][j],
                                this.createStyle(null, "Cluster Time [ns]", "Cluster Amplitude [ADC Counts]"));
            }

        for (IPlotter plotter : plotters.values())
            plotter.show();
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
