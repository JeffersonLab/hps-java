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
//import hep.physics.vec.BasicHep3Vector;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Set;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;

import org.hps.recon.tracking.SvtPlotUtils;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
//import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
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
public class SvtClusterPlotsWithTrackHits extends Driver {

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
    private static final Map<String, IHistogram1D> deltaYPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram2D> deltaYVsYPlots = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram1D> deltaYHOTPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram2D> deltaYHOTVsYPlots = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram1D> clusterYPlots = new HashMap<String, IHistogram1D>();

    private SvtTimingConstants timingConstants;
    private static final int TOP = 0;
    private static final int BOTTOM = 1;

    private String eCalClusterCollectionName = "EcalClusters";
    private double clusterEnergyCut = 0.2; //GeV
    private double clusterXCut = 250;//mm
    private double deltaStripHit = 10.0;//mm

//    private String trackCollectionName = "GBLTracks";
    private String trackCollectionName = "MatchedTracks";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";

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

    public void setDropSmallHitEvents(boolean dropSmallHitEvents) {
        this.dropSmallHitEvents = dropSmallHitEvents;
    }

    public void setSaveRootFile(boolean saveRootFile) {
        this.saveRootFile = saveRootFile;
    }
//
//    private int computePlotterRegion(HpsSiSensor sensor) {
//
//        if (sensor.getLayerNumber() < 7) {
//            if (sensor.isTopLayer()) {
//                return 6 * (sensor.getLayerNumber() - 1);
//            } else {
//                return 6 * (sensor.getLayerNumber() - 1) + 1;
//            }
//        } else if (sensor.isTopLayer()) {
//            if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
//                return 6 * (sensor.getLayerNumber() - 7) + 2;
//            } else {
//                return 6 * (sensor.getLayerNumber() - 7) + 3;
//            }
//        } else if (sensor.isBottomLayer()) {
//            if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
//                return 6 * (sensor.getLayerNumber() - 7) + 4;
//            } else {
//                return 6 * (sensor.getLayerNumber() - 7) + 5;
//            }
//        }
//        return -1;
//    }

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
            clusterChargePlots.get(sensor.getName()).reset();
            singleHitClusterChargePlots.get(sensor.getName()).reset();
            clusterTimePlots.get(sensor.getName()).reset();
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

//        plotters.put("Cluster Amplitude", plotterFactory.create("Cluster Amplitude"));
//        plotters.get("Cluster Amplitude").createRegions(6, 6);
//
//        plotters.put("Cluster Time", plotterFactory.create("Cluster Time"));
//        plotters.get("Cluster Time").createRegions(6, 6);
        plotters.put("Cluster Amplitude: L1-L4", plotterFactory.create("Cluster Amplitude: L1-L4"));
        plotters.get("Cluster Amplitude: L1-L4").createRegions(4, 4);
        plotters.put("Cluster Amplitude: L5-L7", plotterFactory.create("Cluster Amplitude: L5-L7"));
        plotters.get("Cluster Amplitude: L5-L7").createRegions(6, 4);
        plotters.put("Cluster Time: L1-L4", plotterFactory.create("Cluster Time: L1-L4"));
        plotters.get("Cluster Time: L1-L4").createRegions(4, 4);
        plotters.put("Cluster Time: L5-L7", plotterFactory.create("Cluster Time: L5-L7"));
        plotters.get("Cluster Time: L5-L7").createRegions(6, 4);

        plotters.put("L1-L4 Cluster Y", plotterFactory.create("L1-L4 Cluster Y"));
        plotters.get("L1-L4 Cluster Y").createRegions(4, 4);
        plotters.put("L5-L7 Cluster Y", plotterFactory.create("L5-L7 Cluster Y"));
        plotters.get("L5-L7 Cluster Y").createRegions(6, 4);

        plotters.put("L1-L4 deltaY", plotterFactory.create("L1-L4 deltaY"));
        plotters.get("L1-L4 deltaY").createRegions(4, 4);
        plotters.put("L5-L7 deltaY", plotterFactory.create("L5-L7 deltaY"));
        plotters.get("L5-L7 deltaY").createRegions(6, 4);

        plotters.put("L1-L4 deltaY vs Y", plotterFactory.create("L1-L4 deltaY vs Y"));
        plotters.get("L1-L4 deltaY vs Y").createRegions(4, 4);
        plotters.put("L5-L7 deltaY vs Y", plotterFactory.create("L5-L7 deltaY vs Y"));
        plotters.get("L5-L7 deltaY vs Y").createRegions(6, 4);

        plotters.put("L1-L4 deltaYHOT", plotterFactory.create("L1-L4 deltaYHOT"));
        plotters.get("L1-L4 deltaYHOT").createRegions(4, 4);
        plotters.put("L5-L7 deltaYHOT", plotterFactory.create("L5-L7 deltaYHOT"));
        plotters.get("L5-L7 deltaYHOT").createRegions(6, 4);

        plotters.put("L1-L4 deltaYHOT vs Y", plotterFactory.create("L1-L4 deltaYHOT vs Y"));
        plotters.get("L1-L4 deltaYHOT vs Y").createRegions(4, 4);
        plotters.put("L5-L7 deltaYHOT vs Y", plotterFactory.create("L5-L7 deltaYHOT vs Y"));
        plotters.get("L5-L7 deltaYHOT vs Y").createRegions(6, 4);

        for (HpsSiSensor sensor : sensors) {

            clusterChargePlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Charge", 100, 0, 5000));
            singleHitClusterChargePlots
                    .put(sensor.getName(), histogramFactory.createHistogram1D(sensor.getName()
                            + " - Single Hit Cluster Charge", 100, 0, 5000));
            clusterTimePlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Time", 100, -75, 75));

            if (sensor.getLayerNumber() < 9) {
                plotters.get("Cluster Amplitude: L1-L4")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterChargePlots.get(sensor.getName()),
                                this.createStyle(sensor, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Cluster Amplitude: L1-L4")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(singleHitClusterChargePlots.get(sensor.getName()),
                                this.createStyle(null, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Cluster Time: L1-L4").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterTimePlots.get(sensor.getName()), this.createStyle(null, "Cluster Time [ns]", ""));
            } else {
                plotters.get("Cluster Amplitude: L5-L7")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterChargePlots.get(sensor.getName()),
                                this.createStyle(sensor, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Cluster Amplitude: L5-L7")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(singleHitClusterChargePlots.get(sensor.getName()),
                                this.createStyle(null, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Cluster Time: L5-L7").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterTimePlots.get(sensor.getName()), this.createStyle(null, "Cluster Time [ns]", ""));
            }
            clusterYPlots.put(sensor.getName(), histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Y", 250, 0, 50.0));

            deltaYPlots.put(sensor.getName(), histogramFactory.createHistogram1D(sensor.getName() + " - deltaY", 100, -10, 10.0));
            deltaYVsYPlots.put(sensor.getName(), histogramFactory.createHistogram2D(sensor.getName() + " - deltaY vs Y",
                    100, -10, 10, 100, -30, 30));
            deltaYHOTPlots.put(sensor.getName(), histogramFactory.createHistogram1D(sensor.getName() + " - deltaYHOT", 100, -10, 10.0));
            deltaYHOTVsYPlots.put(sensor.getName(), histogramFactory.createHistogram2D(sensor.getName() + " - deltaYHOT vs Y",
                    100, -10, 10, 100, -30, 30));
            if (sensor.getLayerNumber() < 9) {
                plotters.get("L1-L4 Cluster Y")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterYPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit Cluster abs(Y)", ""));
                plotters.get("L1-L4 deltaY")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(deltaYPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit deltaY", ""));
                plotters.get("L1-L4 deltaY vs Y")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(deltaYVsYPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit deltaY vs ProjectionY", ""));
                plotters.get("L1-L4 deltaYHOT")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(deltaYHOTPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit deltaYHOT", ""));
                plotters.get("L1-L4 deltaYHOT vs Y")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(deltaYHOTVsYPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit deltaYHOT vs ProjectionY", ""));
            } else {
                plotters.get("L5-L7 Cluster Y")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterYPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit Cluster abs(Y)", ""));
                plotters.get("L5-L7 deltaY")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(deltaYPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit deltaY", ""));
                plotters.get("L5-L7 deltaY vs Y")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(deltaYVsYPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit deltaY vs ProjectionY", ""));
                plotters.get("L5-L7 deltaYHOT")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(deltaYHOTPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit deltaYHOT", ""));
                plotters.get("L5-L7 deltaYHOT vs Y")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(deltaYHOTVsYPlots.get(sensor.getName()),
                                this.createStyle(sensor, "Hit deltaYHOT vs ProjectionY", ""));
            }
        }

        plotters.put("SVT-trigger timing top-bottom", plotterFactory.create("SVT-trigger timing top-bottom"));
        plotters.get("SVT-trigger timing top-bottom").createRegions(1, 2);

        hitTimeTrigTimePlots.put("Top",
                histogramFactory.createHistogram2D("Top Cluster Time vs. Trigger Phase", 100, -75, 50, 6, -15, 15));
        plotters.get("SVT-trigger timing top-bottom")
                .region(0)
                .plot(hitTimeTrigTimePlots.get("Top"), this.createStyle(null, "Cluster Time [ns]", "Trigger Phase[ns]"));
        hitTimeTrigTimePlots.put("Bottom",
                histogramFactory.createHistogram2D("Bottom Cluster Time vs. Trigger Phase", 100, -75, 50, 6, -15, 15));
        plotters.get("SVT-trigger timing top-bottom")
                .region(1)
                .plot(hitTimeTrigTimePlots.get("Bottom"),
                        this.createStyle(null, "Cluster Time [ns]", "Trigger Phase[ns]"));

        plotters.put("SVT-trigger timing by phase", plotterFactory.create("SVT-trigger timing by phase"));
        plotters.get("SVT-trigger timing by phase").createRegions(2, nmodlayers);

        plotters.put("SVT-trigger timing and amplitude by phase",
                plotterFactory.create("SVT-trigger timing and amplitude by phase"));
        plotters.get("SVT-trigger timing and amplitude by phase").createRegions(2, nmodlayers);

        for (int i = 0; i < nmodlayers; i++)
            for (int j = 0; j < 2; j++) {
                hitTimeTrigTimePlots1D[i][j] = histogramFactory.createHistogram1D(
                        String.format("Cluster Time for Phase %d, %s", i, j == TOP ? "Top" : "Bottom"), 100, -75, 50);
                plotters.get("SVT-trigger timing by phase").region(i + nmodlayers * j)
                        .plot(hitTimeTrigTimePlots1D[i][j], this.createStyle(null, "Cluster Time [ns]", ""));
                hitTimeTrigTimePlots2D[i][j] = histogramFactory.createHistogram2D(
                        String.format("Cluster Amplitude vs. Time for Phase %d, %s", i, j == TOP ? "Top" : "Bottom"),
                        100, -75, 50, 100, 0, 5000.0);
                plotters.get("SVT-trigger timing and amplitude by phase")
                        .region(i + nmodlayers * j)
                        .plot(hitTimeTrigTimePlots2D[i][j],
                                this.createStyle(null, "Cluster Time [ns]", "Cluster Amplitude [ADC Counts]"));
            }

        for (IPlotter plotter : plotters.values())
            plotter.show();
    }

    public void process(EventHeader event) {

        if (runNumber == -1)
            runNumber = event.getRunNumber();

        // // If the event doesn't contain fitted raw hits, skip it
        // if (!event.hasCollection(FittedRawTrackerHit.class, fittedHitsCollectionName)) {
        // return;
        // }
        // Get the list of fitted hits from the event
        // List<FittedRawTrackerHit> fittedHits = event.get(FittedRawTrackerHit.class, fittedHitsCollectionName);
        //
        // // Map the fitted hits to their corresponding raw hits
        // this.mapFittedRawHits(fittedHits);
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
            clusterYPlots.get(sensor.getName()).fill(absClY);
            // Fill all plots
            clusterChargePlots.get(sensor.getName()).fill(cluster.getdEdx() / DopedSilicon.ENERGY_EHPAIR);

            if (cluster.getRawHits().size() == 1)
                singleHitClusterChargePlots.get(sensor.getName()).fill(cluster.getdEdx() / DopedSilicon.ENERGY_EHPAIR);
            double trigPhase = (((event.getTimeStamp() - 4 * timingConstants.getOffsetPhase()) % 24) - 12);
            clusterTimePlots.get(sensor.getName()).fill(cluster.getTime());
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

        List<Cluster> ecalClusters;
        if (event.hasCollection(Cluster.class, eCalClusterCollectionName))
            ecalClusters = event.get(Cluster.class, eCalClusterCollectionName);
        else {
            System.out.println(this.getName() + "::  No ECal Clusters found");
            return;
        }

        // find the positron-side cluster above a certain energy
        // take max energy cluster
        Cluster positronCluster = null;
        double maxEnergy = -999;
        for (Cluster cluster : ecalClusters) {
            double clE = cluster.getEnergy();
            double clX = cluster.getPosition()[0];
            double clY = cluster.getPosition()[1];
//            System.out.println("Cluster y = " + clY + "; Cluster E = " + clE);
            if (clE > clusterEnergyCut && clX > clusterXCut && clE > maxEnergy) {
                maxEnergy = clE;
                positronCluster = cluster;
            }
        }

//        if (positronCluster == null)
//            System.out.println(this.getName() + "::  No Positron Cluster Found"); //            return;
        if (positronCluster != null) {
            //get positron cluster y and make a line from target (0,0,0)...all I care about is Y
            double cluYSlope = positronCluster.getPosition()[1] / positronCluster.getPosition()[2];
            //loop over siClusters and pick out ones that are in delta of projection
            for (SiTrackerHitStrip1D siCl : clusters) {
                double clY = siCl.getPosition()[1];
                double clZ = siCl.getPosition()[2];
                //projection of ecal cluster "track" onto sensor
                double ecalProjY = cluYSlope * clZ;
                double deltaY = clY - ecalProjY;
                // Get the sensor associated with this cluster
                if (clY * ecalProjY > 0) {
                    HpsSiSensor sensor = (HpsSiSensor) siCl.getSensor();
                    deltaYPlots.get(sensor.getName()).fill(deltaY);
                    deltaYVsYPlots.get(sensor.getName()).fill(deltaY, ecalProjY);
                }
            }
            //loop over tracks and match up (roughly) ECal hit, then plot 
            // deltaY for just  hits on this track
            // purpose of this is to see if method makes sense
            if (event.hasCollection(Track.class, trackCollectionName)) {
                List<Track> tracks = event.get(Track.class, trackCollectionName);
                //               System.out.println("Number of Tracks = " + tracks.size());
                for (Track trk : tracks) {
                    if (trk.getTrackStates().get(0).getOmega() > 0)
                        continue;//only pick positrons (-ive omega)
                    double trkSlope = trk.getTrackStates().get(0).getTanLambda();
                    //                  System.out.println("track slope  = " + trkSlope);
                    if (trkSlope * cluYSlope < 0)
                        continue;//make sure it's in the same half
                    double projToEcal = trkSlope * positronCluster.getPosition()[2];
                    //difference in track projection and ECal cluster... maybe use this as a cut at some point...
                    double deltaTrackProjToCluster = projToEcal - positronCluster.getPosition()[1];
                    List<TrackerHit> hitsOnTrack = trk.getTrackerHits();
                    for (TrackerHit hth : hitsOnTrack) {
                        HelicalTrackHit htc = (HelicalTrackHit) hth;
                        HelicalTrackCross cross = (HelicalTrackCross) htc;
                        List<HelicalTrackStrip> clusterlist = cross.getStrips();
                        for (HelicalTrackStrip hts : clusterlist) {
                            SiTrackerHitStrip1D siCl = findSiClusterFromHelicalTrackStrip(hts, clusters);
                            if (siCl != null) {
                                List<RawTrackerHit> rthList = hts.rawhits();
                                HpsSiSensor sensor = (HpsSiSensor) rthList.get(0).getDetectorElement();
                                double siclY = siCl.getPosition()[1];
                                double siclZ = siCl.getPosition()[2];
                                double ecalProjY = cluYSlope * siclZ;
                                double deltaY = siclY - ecalProjY;
                                deltaYHOTPlots.get(sensor.getName()).fill(deltaY);
                                deltaYHOTVsYPlots.get(sensor.getName()).fill(deltaY, ecalProjY);
                            }
                        }
                    }
                }
            }
        }

        return;

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

    private SiTrackerHitStrip1D findSiClusterFromHelicalTrackStrip(HelicalTrackStrip hit, List<SiTrackerHitStrip1D> siStrips) {
        for (SiTrackerHitStrip1D cl : siStrips)
            if (matchHits(hit, cl))
                return cl;
        return null;
    }

    private boolean matchHits(HelicalTrackStrip hts, SiTrackerHitStrip1D siStrip) {
        List<RawTrackerHit> rthHts = hts.rawhits();
        List<RawTrackerHit> rtsSi = siStrip.getRawHits();
        if (rtsSi == rthHts)
//            System.out.println("Found a siStrip that matches a HelicaTrackStrip!!!");
            return true;
        return false;
    }
}
