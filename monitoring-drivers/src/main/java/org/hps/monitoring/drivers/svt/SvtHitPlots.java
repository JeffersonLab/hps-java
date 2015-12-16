package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.jfree.plotter.Plotter;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.ref.rootwriter.RootFileStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.SvtPlotUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
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
    static {
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("SVT Hits");
    private IHistogramFactory histogramFactory = null;
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();

    // Histogram Maps
    private static Map<String, IHistogram1D> hitsPerSensorPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, int[]> hitsPerSensor = new HashMap<String, int[]>();
    private static Map<String, IHistogram1D> layersHitPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> hitCountPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> firstSamplePlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> firstSamplePlotsNoise = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram2D> firstSamplePlotsNoisePerChannel = new HashMap<String, IHistogram2D>();

    private List<HpsSiSensor> sensors;

    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    
    // Counters
    double eventCount = 0;
    double totalHitCount = 0;
    double totalTopHitCount = 0;
    double totalBotHitCount = 0;

    private boolean dropSmallHitEvents = true;
    private static final boolean debug = false;
    private boolean doPerChannelSamplePlots = false;
    private int maxSampleCutForNoise = -1;
    private boolean saveRootFile = false;
    private String outputRootFilename = "";
    private boolean showPlots = true;

    public void setDropSmallHitEvents(boolean dropSmallHitEvents) {
        this.dropSmallHitEvents = dropSmallHitEvents;
    }

    public void setDoPerChannelsSampleplots(boolean val) {
        doPerChannelSamplePlots = val;
    }

    public void setSaveRootFile(boolean save) {
        saveRootFile = save;
    }


    private int computePlotterRegion(HpsSiSensor sensor) {

        if (sensor.getLayerNumber() < 7) {
            if (sensor.isTopLayer()) {
                return 6 * (sensor.getLayerNumber() - 1);
            } else {
                return 6 * (sensor.getLayerNumber() - 1) + 1;
            }
        } else {

            if (sensor.isTopLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6 * (sensor.getLayerNumber() - 7) + 2;
                } else {
                    return 6 * (sensor.getLayerNumber() - 7) + 3;
                }
            } else if (sensor.isBottomLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6 * (sensor.getLayerNumber() - 7) + 4;
                } else {
                    return 6 * (sensor.getLayerNumber() - 7) + 5;
                }
            }
        }
        return -1;
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
        for (HpsSiSensor sensor : sensors) {
            hitsPerSensor.get(sensor.getName())[0] = 0;
        }
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
            hitsPerSensorPlots.get(sensor.getName()).reset();
            firstSamplePlots.get(sensor.getName()).reset();
            firstSamplePlotsNoise.get(sensor.getName()).reset();
            if(doPerChannelSamplePlots)
                firstSamplePlotsNoisePerChannel.get(sensor.getName()).reset();
        }

        for (IHistogram1D histogram : layersHitPlots.values()) {
            histogram.reset();
        }

        for (IHistogram1D histogram : hitCountPlots.values()) {
            histogram.reset();
        }

    }

    protected void detectorChanged(Detector detector) {

        // Get the HpsSiSensor objects from the geometry
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);

        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }

//        // If the tree already exist, clear all existing plots of any old data
//        // they might contain.
//        if (tree != null) {
//            this.resetPlots();
//            return;
//        }
        tree = analysisFactory.createTreeFactory().create();
        histogramFactory = analysisFactory.createHistogramFactory(tree);

        plotters.put("Raw hits per sensor", plotterFactory.create("Raw hits per sensor"));
        plotters.get("Raw hits per sensor").createRegions(6, 6);

        for (HpsSiSensor sensor : sensors) {
            hitsPerSensorPlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + " - Raw Hits", 25, 0, 25));
            plotters.get("Raw hits per sensor").region(SvtPlotUtils.computePlotterRegion(sensor))
                    .plot(hitsPerSensorPlots.get(sensor.getName()), this.createStyle(sensor, "Number of Raw Hits", ""));
            hitsPerSensor.put(sensor.getName(), new int[1]);
        }

        plotters.put("Number of layers hit", plotterFactory.create("Number of layers hit"));
        plotters.get("Number of layers hit").createRegions(1, 2);

        layersHitPlots.put("Top",
                histogramFactory.createHistogram1D("Top Layers Hit", 12, 0, 12));
        plotters.get("Number of layers hit").region(0).plot(layersHitPlots.get("Top"), SvtPlotUtils.createStyle(plotterFactory, "Number of Top Layers Hit", ""));
        layersHitPlots.put("Bottom",
                histogramFactory.createHistogram1D("Bottom Layers Hit", 12, 0, 12));
        plotters.get("Number of layers hit").region(1).plot(layersHitPlots.get("Bottom"), SvtPlotUtils.createStyle(plotterFactory, "Number of Bottom Layers Hit", ""));

        plotters.put("Raw hit counts/Event", plotterFactory.create("Raw hit counts/Event"));
        plotters.get("Raw hit counts/Event").createRegions(2, 2);

        hitCountPlots.put("Raw hit counts/Event",
                histogramFactory.createHistogram1D("Raw hit counts", 100, 0, 100));
        plotters.get("Raw hit counts/Event").region(0).plot(hitCountPlots.get("Raw hit counts/Event"), SvtPlotUtils.createStyle(plotterFactory, "Number of Raw Hits", ""));
        hitCountPlots.put("SVT top raw hit counts/Event",
                histogramFactory.createHistogram1D("SVT top raw hit counts", 100, 0, 100));
        plotters.get("Raw hit counts/Event").region(2).plot(hitCountPlots.get("SVT top raw hit counts/Event"), SvtPlotUtils.createStyle(plotterFactory, "Number of Raw Hits in Top Volume", ""));
        hitCountPlots.put("SVT bottom raw hit counts/Event",
                histogramFactory.createHistogram1D("SVT bottom raw hit counts", 100, 0, 100));
        plotters.get("Raw hit counts/Event").region(3).plot(hitCountPlots.get("SVT bottom raw hit counts/Event"), SvtPlotUtils.createStyle(plotterFactory, "Number of Raw Bits in the Bottom Volume", ""));

        plotters.put("First sample distributions (pedestal shifts)", plotterFactory.create("First sample distributions (pedestal shifts)"));
        plotters.get("First sample distributions (pedestal shifts)").createRegions(6, 6);

        plotters.put("First sample distributions (pedestal shifts, MAX_SAMPLE>=4)", plotterFactory.create("First sample distributions (pedestal shifts, MAX_SAMPLE>=4)"));
        plotters.get("First sample distributions (pedestal shifts, MAX_SAMPLE>=4)").createRegions(6, 6);
        
        if(doPerChannelSamplePlots) {
            plotters.put("First sample channel distributions (pedestal shifts, MAX_SAMPLE>=4)", plotterFactory.create("First sample channel distributions (pedestal shifts, MAX_SAMPLE>=4)"));
            plotters.get("First sample channel distributions (pedestal shifts, MAX_SAMPLE>=4)").createRegions(6, 6);
        }

        
        for (HpsSiSensor sensor : sensors) {
            firstSamplePlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + " - first sample", 100, -500.0, 2000.0));
            plotters.get("First sample distributions (pedestal shifts)").region(this.computePlotterRegion(sensor))
                    .plot(firstSamplePlots.get(sensor.getName()), this.createStyle(sensor, "First sample - pedestal [ADC counts]", ""));
            firstSamplePlotsNoise.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + " - first sample (MAX_SAMPLE>=4)", 100, -500.0, 2000.0));
            plotters.get("First sample distributions (pedestal shifts, MAX_SAMPLE>=4)").region(this.computePlotterRegion(sensor))
                    .plot(firstSamplePlotsNoise.get(sensor.getName()), this.createStyle(sensor, "First sample - pedestal (MAX_SAMPLE>=4) [ADC counts]", ""));

            if( doPerChannelSamplePlots ) {
                firstSamplePlotsNoisePerChannel.put(sensor.getName(),
                        histogramFactory.createHistogram2D(sensor.getName() + " channels - first sample (MAX_SAMPLE>=4)", 640, -0.5,639.5, 20, -500.0, 500.0));
                plotters.get("First sample channel distributions (pedestal shifts, MAX_SAMPLE>=4)").region(this.computePlotterRegion(sensor))
                .plot(firstSamplePlotsNoisePerChannel.get(sensor.getName()), this.createStyle(sensor, "First sample channels - pedestal (MAX_SAMPLE>=4) [ADC counts]", ""));
            }
        
        
        }

        for (IPlotter plotter : plotters.values()) {
            for (int regionN = 0; regionN < plotter.numberOfRegions(); regionN++) {
                PlotterRegion region = ((PlotterRegion) ((Plotter) plotter).region(regionN));
                if (region.getPlottedObjects().size() == 0) {
                    continue;
                }
                region.getPanel().addMouseListener(new PopupPlotterListener(region));
            }
            if(showPlots)
                plotter.show();
        }
    }

    public void process(EventHeader event) {

        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            return;
        }

        if(debug && ((int) eventCount % 100 == 0) )
            System.out.println(this.getClass().getSimpleName() + ": processed " + String.valueOf(eventCount) + " events");
        
        eventCount++;
        

        if(outputRootFilename.isEmpty())
            outputRootFilename = "run" + String.valueOf( event.getRunNumber());
        
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

        if (dropSmallHitEvents && SvtPlotUtils.countSmallHits(rawHits) > 3) {
            return;
        }

        this.clearHitMaps();
        for (RawTrackerHit rawHit : rawHits) {
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
            int channel = rawHit.getIdentifierFieldValue("strip");
            double pedestal = sensor.getPedestal(channel,0);
            // Find the sample with maximum ADC count
            int maxSample = 0;
            double maxSampleValue = 0;
            for (int s = 0; s < 6; ++s) {
                if (((double) rawHit.getADCValues()[s] - pedestal) > maxSampleValue) {
                    maxSample = s;
                    maxSampleValue = ((double) rawHit.getADCValues()[s]) - pedestal;
                }
            }

            hitsPerSensor.get(sensor.getName())[0]++;
            firstSamplePlots.get(sensor.getName()).fill(rawHit.getADCValues()[0] - pedestal);
            if (maxSampleCutForNoise >=0 && maxSample >= maxSampleCutForNoise) {
                    firstSamplePlotsNoise.get(sensor.getName()).fill(rawHit.getADCValues()[0] - pedestal);
                    if( doPerChannelSamplePlots ) 
                        firstSamplePlotsNoisePerChannel.get(sensor.getName()).fill(channel, rawHit.getADCValues()[0] - pedestal);
            } else {
                firstSamplePlotsNoise.get(sensor.getName()).fill(rawHit.getADCValues()[0] - pedestal);
                if( doPerChannelSamplePlots ) 
                    firstSamplePlotsNoisePerChannel.get(sensor.getName()).fill(channel, rawHit.getADCValues()[0] - pedestal);
            }
        }

        int[] topLayersHit = new int[12];
        int[] botLayersHit = new int[12];
        int eventHitCount = 0;
        int topEventHitCount = 0;
        int botEventHitCount = 0;
        for (HpsSiSensor sensor : sensors) {
            int hitCount = hitsPerSensor.get(sensor.getName())[0];
            hitsPerSensorPlots.get(sensor.getName()).fill(hitCount);

            eventHitCount += hitCount;

            if (hitsPerSensor.get(sensor.getName())[0] > 0) {
                if (sensor.isTopLayer()) {
                    topLayersHit[sensor.getLayerNumber() - 1]++;
                    topEventHitCount += hitCount;
                } else {
                    botLayersHit[sensor.getLayerNumber() - 1]++;
                    botEventHitCount += hitCount;
                }
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
        for (int layerN = 0; layerN < 12; layerN++) {
            if (topLayersHit[layerN] > 0) {
                totalTopLayersHit++;
            }
            if (botLayersHit[layerN] > 0) {
                totalBotLayersHit++;
            }
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
        
        if(saveRootFile) {
            String rootFileName = outputRootFilename.isEmpty() ? "svthitplots.root" : outputRootFilename + "_svthitplots.root";
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
