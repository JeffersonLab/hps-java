package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ref.plotter.style.registry.IStyleStore;
import hep.aida.ref.plotter.style.registry.StyleRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.SvtPlotUtils;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TSData2019;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class KFTrackTimePlots extends Driver {

    // private AIDAFrame plotterFrame;
    private String hitCollection = "StripClusterer_SiTrackerHitStrip1D";
    private String trackCollectionName = "KalmanFullTracks";
    IPlotter plotter, plotter2, plotter3, plotter4, plotter5, plotter6, plotter7;
//    private IHistogram1D[][] t0 = new IHistogram1D[4][14];
//    private IHistogram1D[][] trackHitT0 = new IHistogram1D[4][14];
//    private IHistogram1D[][] trackHitDt = new IHistogram1D[4][14];
//    private IHistogram2D[] trackHit2D = new IHistogram2D[124];
//    private IHistogram1D[] trackT0 = new IHistogram1D[4];
//    private IHistogram2D[] trackTrigTime = new IHistogram2D[4];
//    private IHistogram2D[] trackHitDtChan = new IHistogram2D[14];
//    private IHistogram1D[] trackTimeRange = new IHistogram1D[4];
//    private IHistogram2D[] trackTimeMinMax = new IHistogram2D[4];

    private static ITree tree = AIDA.defaultInstance().tree();
    private final IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private final IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("Track Timing");
    private IHistogramFactory histogramFactory = null;
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();

    // Histogram Maps   
    private static final Map<String, IHistogram1D> trackHitDt = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackHitT0 = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> clusterChargePlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> singleHitClusterChargePlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackT0 = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> trackTimeRange = new HashMap<String, IHistogram1D>();

    private static final Map<String, IHistogram2D> trackTrigTime = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> trackHit2D = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> trackTimeMinMax = new HashMap<String, IHistogram2D>();

    private static final String subdetectorName = "Tracker";
    double minTime = -40;
    double maxTime = 40;
    private boolean removeRandomEvents = true;

    private boolean useStyles=false;

    public void setRemoveRandomEvents(boolean doit) {
        this.removeRandomEvents = doit;
    }

    public void setTrackCollectionName(String name) {
        this.trackCollectionName = name;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        tree.cd("/");
        boolean dirExists = false;
        String dirName = "/KFTrackTime";
        for (String st : tree.listObjectNames()) {
            System.out.println(st);
            if (st.contains(dirName)) {
                dirExists = true;
            }
        }
        tree.setOverwrite(true);
        if (!dirExists) {
            tree.mkdir(dirName);
        }
        tree.cd(dirName);

        histogramFactory = analysisFactory.createHistogramFactory(tree);

        List<HpsSiSensor> sensors = detector.getSubdetector(subdetectorName).getDetectorElement()
                .findDescendants(HpsSiSensor.class);

        if (useStyles){
            StyleRegistry styleRegistry = StyleRegistry.getStyleRegistry();
            IStyleStore store = styleRegistry.getStore("DefaultStyleStore");
            IPlotterStyle style2d = store.getStyle("DefaultColorMapStyle");
            style2d.setParameter("hist2DStyle", "colorMap");
            style2d.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            // style2d.zAxisStyle().setParameter("scale", "log");
            style2d.zAxisStyle().setVisible(false);
            style2d.dataBoxStyle().setVisible(false);
            
            IPlotterStyle styleOverlay = store.getStyle("DefaultHistogram1DStyle");
            styleOverlay.dataStyle().errorBarStyle().setVisible(true);
            styleOverlay.dataStyle().fillStyle().setVisible(false);
            styleOverlay.legendBoxStyle().setVisible(false);
            styleOverlay.dataStyle().outlineStyle().setVisible(false);
        }
        plotters.put("Track Hit Charge: L0-L3", plotterFactory.create("2a Hit Charge: L0-L3"));
        plotters.get("Track Hit Charge: L0-L3").createRegions(4, 4);
        plotters.put("Track Hit Charge: L4-L6", plotterFactory.create("2b Hit Charge: L4-L6"));
        plotters.get("Track Hit Charge: L4-L6").createRegions(6, 4);

        plotters.put("Track Hit Times: L0-L3", plotterFactory.create("2c Track Hit Times: L0-L3"));
        plotters.get("Track Hit Times: L0-L3").createRegions(4, 4);
        plotters.put("Track Hit Times: L4-L6", plotterFactory.create("2d Track Hit Times: L4-L6"));
        plotters.get("Track Hit Times: L4-L6").createRegions(6, 4);

        plotters.put("Track Hit dt: L0-L3", plotterFactory.create("2e Track Hit dt: L0-L3"));
        plotters.get("Track Hit dt: L0-L3").createRegions(4, 4);
        plotters.put("Track Hit dt: L4-L6", plotterFactory.create("2f Track Hit dt: L4-L6"));
        plotters.get("Track Hit dt: L4-L6").createRegions(6, 4);

        plotters.put("Track Time vs. dt: L0-L3", plotterFactory.create("2gTrack Time vs. dt: L0-L3"));
        plotters.get("Track Time vs. dt: L0-L3").createRegions(4, 4);
        plotters.put("Track Time vs. dt: L4-L6", plotterFactory.create("2h Track Time vs. dt: L4-L6"));
        plotters.get("Track Time vs. dt: L4-L6").createRegions(6, 4);

        plotters.put("Track Time", plotterFactory.create("2i Track Time"));
        plotters.get("Track Time").createRegions(2, 2);

//        plotters.put("Track Trig Time", plotterFactory.create("Track Trig Time"));
//        plotters.get("Track Trig Time").createRegions(2, 2);
        plotters.put("Track Hit Time Range", plotterFactory.create("2j Track Hit Time Range"));
        plotters.get("Track Hit Time Range").createRegions(2, 2);

        for (HpsSiSensor sensor : sensors) {
            clusterChargePlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Cluster Charge", 100, 0, 2000));
            singleHitClusterChargePlots
                    .put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())
                            + " - Single Hit Cluster Charge", 100, 0, 2000));

            trackHitT0.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - track hit t0", 100, minTime, maxTime));
            trackHitDt.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - track hit dt", 100, minTime, maxTime));

            trackHit2D.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram2D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - trigger phase vs dt", 80, -20, 20.0, 6, 0, 24.0));
            if (sensor.getLayerNumber() < 9) {
                plotters.get("Track Hit Charge: L0-L3")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Track Hit Charge: L0-L3")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(singleHitClusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(null, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Track Hit Times: L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(trackHitT0.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(sensor, "track hit t0 (ns)", ""));
                plotters.get("Track Hit dt: L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(trackHitDt.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(sensor, "track hit dt (ns)", ""));
                plotters.get("Track Time vs. dt: L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(trackHit2D.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(sensor, "track hit dt (ns)", "event time%24 (ns)"));
            } else {
                plotters.get("Track Hit Charge: L4-L6")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(sensor, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Track Hit Charge: L4-L6")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(singleHitClusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createStyle(null, "Cluster Amplitude [ADC Counts]", ""));
                plotters.get("Track Hit Times: L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(trackHitT0.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(sensor, "track hit t0 (ns)", ""));
                plotters.get("Track Hit dt: L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(trackHitDt.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(sensor, "track hit dt (ns)", ""));
                plotters.get("Track Time vs. dt: L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(trackHit2D.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())), this.createStyle(sensor, "track hit dt (ns)", "event time%24 (ns)"));
            }
        }

//        for (int i = 0; i < nlayers; i++) {
//            int region = computePlotterRegion(i);
//            trackHit2D[i] = aida.histogram2D("Layer " + i + " trigger phase vs dt", 80, -20, 20.0, 6, 0, 24.0);
//            plot(plotter5, trackHit2D[i], style2d, region);
//            trackHitDtChan[i] = aida.histogram2D("Layer " + i + " dt vs position", 200, -20, 20, 50, -20, 20.0);
//            plot(plotter6, trackHitDtChan[i], style2d, region);
//        }
        trackT0.put("Top",
                histogramFactory.createHistogram1D("Top Track Time", 80, -40, 40.0));
        plotters.get("Track Time")
                .region(0)
                .plot(trackT0.get("Top"), SvtPlotUtils.createStyle(plotterFactory, "Track Time [ns]", ""));
        trackT0.put("Bottom",
                histogramFactory.createHistogram1D("Bottom Track Time", 80, -40, 40.0));
        plotters.get("Track Time")
                .region(1)
                .plot(trackT0.get("Bottom"), SvtPlotUtils.createStyle(plotterFactory, "Track Time [ns]", ""));
        trackTrigTime.put("Top",
                histogramFactory.createHistogram2D("Top Track Time vs. Trig Time", 80, -40, 40.0, 6, 0, 24));
        plotters.get("Track Time")
                .region(2)
                .plot(trackTrigTime.get("Top"), SvtPlotUtils.createStyle(plotterFactory, "Track Time [ns]", "event time%24 (ns)"));
        trackTrigTime.put("Bottom",
                histogramFactory.createHistogram2D("Bottom Track Time vs. Trig Time", 80, -40, 40.0, 6, 0, 24));
        plotters.get("Track Time")
                .region(3)
                .plot(trackTrigTime.get("Bottom"), SvtPlotUtils.createStyle(plotterFactory, "Track Time [ns]", "event time%24 (ns)"));

        trackTimeRange.put("Top",
                histogramFactory.createHistogram1D("Top Track Time Range", 50, 0, 50.0));
        plotters.get("Track Hit Time Range")
                .region(0)
                .plot(trackTimeRange.get("Top"), SvtPlotUtils.createStyle(plotterFactory, "Track Time Range [ns]", ""));
        trackTimeRange.put("Bottom",
                histogramFactory.createHistogram1D("Bottom Track Time Range", 50, 0, 50.0));
        plotters.get("Track Hit Time Range")
                .region(1)
                .plot(trackTimeRange.get("Bottom"), SvtPlotUtils.createStyle(plotterFactory, "Track Time Range [ns]", ""));
        trackTimeMinMax.put("Top",
                histogramFactory.createHistogram2D("Top Earliest vs Latest Track Hit Times", 80, -40, 40.0, 80, -40, 40.0));
        plotters.get("Track Hit Time Range")
                .region(2)
                .plot(trackTimeMinMax.get("Top"), SvtPlotUtils.createStyle(plotterFactory, "Earliest Time (ns)", "Latest Time (ns)"));
        trackTimeMinMax.put("Bottom",
                histogramFactory.createHistogram2D("Bottom Earliest vs Latest Track Hit Times", 80, -40, 40.0, 80, -40, 40.0));
        plotters.get("Track Hit Time Range")
                .region(3)
                .plot(trackTimeMinMax.get("Bottom"), SvtPlotUtils.createStyle(plotterFactory, "Earliest Time (ns)", "Latest Time (ns)"));

        for (IPlotter plotter : plotters.values()) {
            plotter.show();
        }

    }

    public void setHitCollection(String hitCollection) {
        this.hitCollection = hitCollection;
    }

    @Override
    public void process(EventHeader event) {
        if (removeRandomEvents && event.hasCollection(GenericObject.class, "TSBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TSBank");
            for (GenericObject data : triggerList) {
                if (AbstractIntData.getTag(data) == TSData2019.BANK_TAG) {
                    TSData2019 triggerData = new TSData2019(data);
                    if (triggerData.isPulserTrigger() || triggerData.isFaradayCupTrigger()) {
                        return;
                    }
                }
            }
        }
        int trigTime = (int) (event.getTimeStamp() % 24);

        //
        List<Track> tracks = event.get(Track.class, trackCollectionName);
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
            for (TrackerHit hitTH : track.getTrackerHits()) {
                SiTrackerHitStrip1D hit = (SiTrackerHitStrip1D) hitTH;
                SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                trackHitT0.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime());
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

            for (TrackerHit hitTH : track.getTrackerHits()) {
                SiTrackerHitStrip1D hit = (SiTrackerHitStrip1D) hitTH;
                int layer = ((HpsSiSensor) hit.getSensor()).getLayerNumber();
                int module = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("module");
                SiSensor sensor = (SiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                trackHitDt.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime() - trackTime);
                trackHit2D.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getTime() - trackTime, event.getTimeStamp() % 24);
                clusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getdEdx() / DopedSilicon.ENERGY_EHPAIR);
                if (hit.getRawHits().size() == 1) {
                    singleHitClusterChargePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(hit.getdEdx() / DopedSilicon.ENERGY_EHPAIR);
                }

            }
        }
    }

    @Override
    public void endOfData() {
        // plotterFrame.dispose();
    }

    // private int computePlotterRegion(int layer, int module) {
    // int iy = (layer) / 2;
    // int ix = 0;
    // if (module > 0)
    // ix += 2;
    // if (layer % 2 == 0)
    // ix += 1;
    // int region = ix * 5 + iy;
    // return region;
    // }
    // layer 1-12
    // module 0-4...0,2 = top; 1,3=bottom
    // this computePlotterRegion puts top&bottom modules on same region
    // and assume plotter is split in 3 columns, 4 rows...L0-5 on top 2 rows; L6-11 on bottom 2
    private int computePlotterRegion(int layer) {

        if (layer == 0) {
            return 0;
        }
        if (layer == 1) {
            return 1;
        }
        if (layer == 2) {
            return 4;
        }
        if (layer == 3) {
            return 5;
        }
        if (layer == 4) {
            return 8;
        }
        if (layer == 5) {
            return 9;
        }

        if (layer == 6) {
            return 2;
        }
        if (layer == 7) {
            return 3;
        }
        if (layer == 8) {
            return 6;
        }
        if (layer == 9) {
            return 7;
        }
        if (layer == 10) {
            return 10;
        }
        if (layer == 11) {
            return 11;
        }
        return -1;
    }

    private String getColor(int module) {
        String color = "Black";
        if (module == 1) {
            color = "Green";
        }
        if (module == 2) {
            color = "Blue";
        }
        if (module == 3) {
            color = "Red";
        }
        return color;
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
        if (sensor != null && sensor.isAxial()) {
            style.regionBoxStyle().backgroundStyle().setColor("246, 246, 34, 1");
        }

        // Turn off the legend
        style.legendBoxStyle().setVisible(false);

        return style;
    }

}
