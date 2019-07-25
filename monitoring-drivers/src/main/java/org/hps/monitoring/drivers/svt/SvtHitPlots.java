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
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;

import org.hps.recon.tracking.SvtPlotUtils;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Monitoring driver that provides information about the number of SVT hits per
 * event.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtHitPlots extends Driver {

    // TODO: Add documentation
    //static {
    //    hep.aida.jfree.AnalysisFactory.register();
    //}
    // Plotting
    private static ITree tree = null;
    private final IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private final IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("SVT Hits");
    private IHistogramFactory histogramFactory = null;
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();

    // Histogram Maps
    private static final Map<String, IHistogram1D> hitsPerSensorPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, int[]> hitsPerSensor = new HashMap<String, int[]>();
    private static final Map<String, IHistogram1D> layersHitPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> hitCountPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> firstSamplePlots = new HashMap<String, IHistogram1D>();
    // private static Map<String, IHistogram1D> firstSamplePlotsNoise = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram2D> firstSamplePlotsNoisePerChannel = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram1D> t0Plots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram2D> t0VsTriggerTime = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> t0VsTriggerBank = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> t0VsChannel = new HashMap<String, IHistogram2D>();
    private List<HpsSiSensor> sensors;
    private SvtTimingConstants timingConstants;
    private static final String SUBDETECTOR_NAME = "Tracker";
    private final String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fittedHitsCollectioName = "SVTFittedRawTrackerHits";
    // Counters
    double eventCount = 0;
    double totalHitCount = 0;
    double totalTopHitCount = 0;
    double totalBotHitCount = 0;

    private boolean dropSmallHitEvents = false;
    private static final boolean debug = false;
    private boolean doPerChannelSamplePlots = false;
    private int maxSampleCutForNoise = -1;
    private boolean saveRootFile = false;
    private String outputRootFilename = "";
    private boolean showPlots = true;

    private boolean cutOutLowChargeHits = false;
    private double hitChargeCut = 400;

    public void setDropSmallHitEvents(boolean dropSmallHitEvents) {
        this.dropSmallHitEvents = dropSmallHitEvents;
    }

    public void setCutOutLowChargeHits(boolean cutOutLowChargeHits) {
        this.cutOutLowChargeHits = cutOutLowChargeHits;
    }

    public void setHitChargeCut(double hitCharge) {
        this.hitChargeCut = hitCharge;
    }

    public void setDoPerChannelsSampleplots(boolean val) {
        doPerChannelSamplePlots = val;
    }

    public void setSaveRootFile(boolean save) {
        saveRootFile = save;
    }

    /**
     * Create a plotter style.
     *
     * @param sensor : HpsSiSensor associated with the plot. This is used to set
     * certain attributes based on the position of the sensor.
     * @param xAxisTitle : Title of the x axis
     * @param yAxisTitle : Title of the y axis
     * @return plotter style
     */
    // TODO: Move this to a utilities class
    IPlotterStyle createStyle(HpsSiSensor sensor, String xAxisTitle, String yAxisTitle) {
        IPlotterStyle style = SvtPlotUtils.createStyle(plotterFactory, xAxisTitle, yAxisTitle);

        if (sensor.isTopLayer()) {
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        } else {
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
        }

        return style;
    }

    private void clearHitMaps() {
        for (HpsSiSensor sensor : sensors)
            hitsPerSensor.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[0] = 0;
    }

    /**
     * Clear all histograms of it's current data.
     */
    private void resetPlots() {

        // Reset all hit maps
        this.clearHitMaps();

        // Since all plots are mapped to the name of a sensor, loop
        // through the sensors, get the corresponding plots and clear them.
        for (HpsSiSensor sensor : sensors) {
            hitsPerSensorPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
            firstSamplePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
            // firstSamplePlotsNoise.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
            if (doPerChannelSamplePlots)
                firstSamplePlotsNoisePerChannel.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
        }

        for (IHistogram1D histogram : layersHitPlots.values())
            histogram.reset();

        for (IHistogram1D histogram : hitCountPlots.values())
            histogram.reset();

    }

    @Override
    protected void detectorChanged(Detector detector) {

        // Get the HpsSiSensor objects from the geometry
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);

        timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
        if (sensors.isEmpty())
            throw new RuntimeException("No sensors were found in this detector.");

        // // If the tree already exist, clear all existing plots of any old data
        // // they might contain.
        // if (tree != null) {
        // this.resetPlots();
        // return;
        // }
        tree = analysisFactory.createTreeFactory().create();
        histogramFactory = analysisFactory.createHistogramFactory(tree);

        plotters.put("Raw hits per sensor: L0-L3", plotterFactory.create("Raw hits per sensor: L0-L3"));
        plotters.get("Raw hits per sensor: L0-L3").createRegions(4, 4);
        plotters.put("Raw hits per sensor: L4-L6", plotterFactory.create("Raw hits per sensor: L4-L6"));
        plotters.get("Raw hits per sensor: L4-L6").createRegions(6, 4);

        for (HpsSiSensor sensor : sensors) {
            hitsPerSensorPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Raw Hits", 25, 0, 25));

            if (sensor.getLayerNumber() < 9)
                plotters.get("Raw hits per sensor: L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(hitsPerSensorPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(sensor, "Number of Raw Hits", ""));
            else
                plotters.get("Raw hits per sensor: L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(hitsPerSensorPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(sensor, "Number of Raw Hits", ""));
            hitsPerSensor.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), new int[1]);
        }

        plotters.put("Number of layers hit", plotterFactory.create("Number of layers hit"));
        plotters.get("Number of layers hit").createRegions(1, 2);

        layersHitPlots.put("Top", histogramFactory.createHistogram1D("Top Layers Hit", 15, 0, 15));
        plotters.get("Number of layers hit")
                .region(0)
                .plot(layersHitPlots.get("Top"),
                        SvtPlotUtils.createStyle(plotterFactory, "Number of Top Layers Hit", ""));
        layersHitPlots.put("Bottom", histogramFactory.createHistogram1D("Bottom Layers Hit", 15, 0, 15));
        plotters.get("Number of layers hit")
                .region(1)
                .plot(layersHitPlots.get("Bottom"),
                        SvtPlotUtils.createStyle(plotterFactory, "Number of Bottom Layers Hit", ""));

        plotters.put("Raw hit counts/Event", plotterFactory.create("Raw hit counts/Event"));
        plotters.get("Raw hit counts/Event").createRegions(2, 2);

        hitCountPlots.put("Raw hit counts/Event", histogramFactory.createHistogram1D("Raw hit counts", 100, 0, 500));
        plotters.get("Raw hit counts/Event")
                .region(0)
                .plot(hitCountPlots.get("Raw hit counts/Event"),
                        SvtPlotUtils.createStyle(plotterFactory, "Number of Raw Hits", ""));
        hitCountPlots.put("SVT top raw hit counts/Event",
                histogramFactory.createHistogram1D("SVT top raw hit counts", 100, 0, 300));
        plotters.get("Raw hit counts/Event")
                .region(2)
                .plot(hitCountPlots.get("SVT top raw hit counts/Event"),
                        SvtPlotUtils.createStyle(plotterFactory, "Number of Raw Hits in Top Volume", ""));
        hitCountPlots.put("SVT bottom raw hit counts/Event",
                histogramFactory.createHistogram1D("SVT bottom raw hit counts", 100, 0, 300));
        plotters.get("Raw hit counts/Event")
                .region(3)
                .plot(hitCountPlots.get("SVT bottom raw hit counts/Event"),
                        SvtPlotUtils.createStyle(plotterFactory, "Number of Raw Bits in the Bottom Volume", ""));

        plotters.put("First sample distributions (pedestal shifts): L0-L3", plotterFactory.create("First sample distributions (pedestal shifts): L0-L3"));
        plotters.get("First sample distributions (pedestal shifts): L0-L3").createRegions(4, 4);
        plotters.put("First sample distributions (pedestal shifts): L4-L6", plotterFactory.create("First sample distributions (pedestal shifts): L4-L6"));
        plotters.get("First sample distributions (pedestal shifts): L4-L6").createRegions(6, 4);

        plotters.put("L0-L3 t0", plotterFactory.create("L0-L3 t0"));
        plotters.get("L0-L3 t0").createRegions(4, 4);
        plotters.put("L4-L6 t0", plotterFactory.create("L4-L6 t0"));
        plotters.get("L4-L6 t0").createRegions(6, 4);

        plotters.put("L0-L3 t0 vs Trigger Phase", plotterFactory.create("L0-L3 t0 vs Trigger Phase"));
        plotters.get("L0-L3 t0 vs Trigger Phase").createRegions(4, 4);
        plotters.put("L4-L6 t0 vs Trigger Phase", plotterFactory.create("L4-L6 t0 vs Trigger Phase"));
        plotters.get("L4-L6 t0 vs Trigger Phase").createRegions(6, 4);

        plotters.put("L0-L3 t0 vs Channel", plotterFactory.create("L0-L3 t0 vs Channel"));
        plotters.get("L0-L3 t0 vs Channel").createRegions(4, 4);
        plotters.put("L4-L6 t0 vs Channel", plotterFactory.create("L4-L6 t0 vs Channel"));
        plotters.get("L4-L6 t0 vs Channel").createRegions(6, 4);

        if (doPerChannelSamplePlots) {
            plotters.put("First sample channel distributions (pedestal shifts): L0-L3", plotterFactory.create("First sample channel distributions (pedestal shifts): L0-L3"));
            plotters.get("First sample channel distributions (pedestal shifts): L0-L3").createRegions(4, 4);
            plotters.put("First sample channel distributions (pedestal shifts): L4-L6", plotterFactory.create("First sample channel distributions (pedestal shifts): L4-L6"));
            plotters.get("First sample channel distributions (pedestal shifts): L4-L6").createRegions(6, 4);

        }

        for (HpsSiSensor sensor : sensors) {
            firstSamplePlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - first sample", 100, -500.0, 2000.0));
            if (sensor.getLayerNumber() < 9)
                plotters.get("First sample distributions (pedestal shifts): L0-L3")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(firstSamplePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "First sample - pedestal [ADC counts]", ""));
            else
                plotters.get("First sample distributions (pedestal shifts): L4-L6")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(firstSamplePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "First sample - pedestal [ADC counts]", ""));

            if (doPerChannelSamplePlots) {
                firstSamplePlotsNoisePerChannel.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram2D(
                        SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " channels - first sample", 640, -0.5, 639.5, 20, -500.0, 500.0));
                if (sensor.getLayerNumber() < 9)
                    plotters.get("First sample channel distributions (pedestal shifts): L0-L3")
                            .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                            .plot(firstSamplePlotsNoisePerChannel.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                    this.createStyle(sensor, "First sample channels - pedestal [ADC counts]", ""));
                else
                    plotters.get("First sample channel distributions (pedestal shifts): L4-L6")
                            .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                            .plot(firstSamplePlotsNoisePerChannel.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                    this.createStyle(sensor, "First sample channels - pedestal [ADC counts]", ""));
            }

            t0Plots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - t0", 100, -100, 100.0));
            t0VsTriggerTime.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + "Hit Time vs. Trigger Phase",
                    120, -60, 60, 30, -15, 15));
//            t0VsTriggerBank.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + "Hit Time vs. Trigger Bank",
//                    100, -100, 100, 10, 5,12));
            t0VsChannel.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + "Hit Time vs. Channel",
                    100, -100, 100, 640, 0, 639));
            if (sensor.getLayerNumber() < 9) {
                plotters.get("L0-L3 t0")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(t0Plots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Hit t0", ""));
                plotters.get("L0-L3 t0 vs Trigger Phase")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(t0VsTriggerTime.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Hit t0 vs Trigger Phase", ""));
//                plotters.get("L0-L3 t0 vs Trigger Bank")
//                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
//                        .plot(t0VsTriggerBank.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
//                                this.createStyle(sensor, "Hit t0 vs Trigger Bank", ""));
                plotters.get("L0-L3 t0 vs Channel")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(t0VsChannel.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Hit t0 vs Channel", ""));
            } else {
                plotters.get("L4-L6 t0")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(t0Plots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Hit t0", ""));
                plotters.get("L4-L6 t0 vs Trigger Phase")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(t0VsTriggerTime.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Hit t0 vs Trigger Phase", ""));
//                 plotters.get("L4-L6 t0 vs Trigger Bank")
//                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
//                        .plot(t0VsTriggerBank.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
//                                this.createStyle(sensor, "Hit t0 vs Trigger Bank", ""));
                plotters.get("L4-L6 t0 vs Channel")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(t0VsChannel.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Hit t0 vs Channel", ""));
            }
        }

        for (IPlotter plotter : plotters.values())
            if (showPlots)
                plotter.show();
    }

    @Override
    public void process(EventHeader event
    ) {

        TIData triggerData = null;
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName))
            return;

        if (!event.hasCollection(LCRelation.class, fittedHitsCollectioName))
            return;
        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            for (GenericObject data : triggerList)
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG)
                    triggerData = new TIData(data); //                    System.out.println(triggerData.getIntVal(1) + "   "
            //                            + triggerData.getIntVal(2)                  );
        }
        if (debug && ((int) eventCount % 100 == 0))
            System.out.println(this.getClass().getSimpleName() + ": processed " + String.valueOf(eventCount)
                    + " events");

        eventCount++;

        if (outputRootFilename.isEmpty())
            outputRootFilename = "run" + String.valueOf(event.getRunNumber());

        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
        List<LCRelation> fittedHits = event.get(LCRelation.class, fittedHitsCollectioName);

        if (dropSmallHitEvents && SvtPlotUtils.countSmallHits(rawHits) > 3)
            return;

        this.clearHitMaps();
        for (RawTrackerHit rawHit : rawHits) {
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
            int channel = (int) rawHit.getIdentifierFieldValue("strip");
            double pedestal = sensor.getPedestal(channel, 0);
            // Find the sample with maximum ADC count
            int maxSample = 0;
            double maxSampleValue = 0;
            for (int s = 0; s < 6; ++s)
                if (((double) rawHit.getADCValues()[s] - pedestal) > maxSampleValue) {
                    maxSample = s;
                    maxSampleValue = ((double) rawHit.getADCValues()[s]) - pedestal;
                }

            hitsPerSensor.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[0]++;
            firstSamplePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(rawHit.getADCValues()[0] - pedestal);
            // if (maxSampleCutForNoise >= 0 && maxSample >= maxSampleCutForNoise) {
            // firstSamplePlotsNoise.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(rawHit.getADCValues()[0] - pedestal);
            if (doPerChannelSamplePlots)
                firstSamplePlotsNoisePerChannel.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))
                        .fill(channel, rawHit.getADCValues()[0] - pedestal);
            // } else {
            // firstSamplePlotsNoise.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(rawHit.getADCValues()[0] - pedestal);
            // if (doPerChannelSamplePlots) {
            // firstSamplePlotsNoisePerChannel.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(channel, rawHit.getADCValues()[0] - pedestal);
            // }
            // }
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
            t0VsTriggerTime.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(t0, trigPhase);
//            System.out.println( triggerData.getIntVal(1)*0.0000001);
//            t0VsTriggerBank.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(t0, triggerData.getIntVal(1)*0.0000001);
            t0VsChannel.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(t0, channel);
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

        totalHitCount += eventHitCount;
        totalTopHitCount += topEventHitCount;
        totalBotHitCount += botEventHitCount;

        hitCountPlots.get("Raw hit counts/Event").fill(eventHitCount);
        hitCountPlots.get("SVT top raw hit counts/Event").fill(topEventHitCount);
        hitCountPlots.get("SVT bottom raw hit counts/Event").fill(botEventHitCount);

        int totalTopLayersHit = 0;
        int totalBotLayersHit = 0;

        for (int layerN = 0; layerN < 14; layerN++) {
            if (topLayersHit[layerN] > 0)
                totalTopLayersHit++;
            if (botLayersHit[layerN] > 0)
                totalBotLayersHit++;
        }

        layersHitPlots.get("Top").fill(totalTopLayersHit);
        layersHitPlots.get("Bottom").fill(totalBotLayersHit);

    }

    @Override
    protected void endOfData() {

        System.out.println("%================================================%");
        System.out.println("%============ SVT Raw Hit Statistics ============%");
        System.out.println("%================================================%\n%");
        System.out.println("% Total Hits/Event: " + totalHitCount / eventCount);
        System.out.println("% Total Top SVT Hits/Event: " + totalTopHitCount / eventCount);
        System.out.println("% Total Bottom SVT Hits/Event: " + totalBotHitCount / eventCount);
        System.out.println("\n%================================================%");

        if (saveRootFile) {
            String rootFileName = outputRootFilename.isEmpty() ? "svthitplots.root" : outputRootFilename
                    + "_svthitplots.root";
            RootFileStore rootFileStore = new RootFileStore(rootFileName);
            try {
                rootFileStore.open();
                rootFileStore.add(tree);
                rootFileStore.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void setShowPlots(boolean showPlots) {
        this.showPlots = showPlots;
    }

}
