package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterRegion;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.Subsystem;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusImpl;
import org.hps.recon.tracking.SvtPlotUtils;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This Driver makes plots of SVT sensor occupancies across a run.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * <<<<<<< HEAD
 *
 * 6/6/19: modified this to work with SVT upgrade including "L0"; separated all
 * plotters into 2
 * so that we have one page for L1-4 and one for L5-7
 * =======
 * >>>>>>> iss457
 */
public class SensorOccupancyPlotsDriver extends Driver {

    // Logger
    private static Logger LOGGER = Logger.getLogger(SensorOccupancyPlotsDriver.class.getCanonicalName());
    
    // Plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("SVT Occupancy");
    private IHistogramFactory histogramFactory = null;

    // Histogram maps
    private static Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    private static Map<String, IHistogram1D> occupancyPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> positionPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> clusterPositionPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> clusterPositionPlotCounts = new HashMap<String, IHistogram1D>();
    private static Map<String, int[]> occupancyMap = new HashMap<String, int[]>();
    private static Map<String, IHistogram1D> maxSamplePositionPlots = new HashMap<String, IHistogram1D>();

    private List<HpsSiSensor> sensors;
    private Map<HpsSiSensor, Map<Integer, Hep3Vector>> stripPositions = new HashMap<HpsSiSensor, Map<Integer, Hep3Vector>>();

    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String triggerBankCollectionName = "TriggerBank";
    private String stripClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";

    private int maxSamplePosition = -1;
    private int timeWindowWeight = 1;
    private int eventCount = 0;
    private int eventRefreshRate = 1;
    private int runNumber = -1;
    private int resetPeriod = -1;

    private boolean enablePositionPlots = false;
    private boolean enableMaxSamplePlots = false;
    private boolean enableTriggerFilter = false;
    private boolean filterPulserTriggers = false;
    private boolean filterSingle0Triggers = false;
    private boolean filterSingle1Triggers = false;
    private boolean filterPair0Triggers = false;
    private boolean filterPair1Triggers = false;

    SystemStatus maxSampleStatus;
    private int maxSampleMonitorStart = 1000;
    private int maxSampleMonitorPeriod = 100;

    SystemStatus occupancyStatus;
    private int occupancyMonitorStart = 5000;
    private int occupancyMonitorPeriod = 100;
    private double minPeakOccupancy = 0.0001;
    private double maxPeakOccupancy = 0.01;

    private boolean dropSmallHitEvents = false;

    private boolean enableClusterTimeCuts = true;
    private double clusterTimeCutMax = 4.0;
    private double clusterTimeCutMin = -4.0;
       
    private boolean saveRootFile = true;
    
    // Max Y range for occupancy plots.
    private double occupancyYRange1 = 0.03;
    private double occupancyYRange2 = 0.003;

    public SensorOccupancyPlotsDriver() {
        maxSampleStatus = new SystemStatusImpl(Subsystem.SVT, "Checks that SVT is timed in (max sample plot)", true);
        maxSampleStatus.setStatus(StatusCode.UNKNOWN, "Status is unknown.");
        occupancyStatus = new SystemStatusImpl(Subsystem.SVT, "Checks SVT occupancy", true);
        occupancyStatus.setStatus(StatusCode.UNKNOWN, "Status is unknown.");
    }

    public void setDropSmallHitEvents(boolean dropSmallHitEvents) {
        this.dropSmallHitEvents = dropSmallHitEvents;
    }

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

    public void setResetPeriod(int resetPeriod) {
        this.resetPeriod = resetPeriod;
    }

    public void setEnablePositionPlots(boolean enablePositionPlots) {
        this.enablePositionPlots = enablePositionPlots;
    }

    public void setEnableMaxSamplePlots(boolean enableMaxSamplePlots) {
        this.enableMaxSamplePlots = enableMaxSamplePlots;
    }

    public void setEnableTriggerFilter(boolean enableTriggerFilter) {
        this.enableTriggerFilter = enableTriggerFilter;
    }

    public void setFilterPulserTriggers(boolean filterPulserTriggers) {
        this.filterPulserTriggers = filterPulserTriggers;
    }

    public void setFilterSingle0Triggers(boolean filterSingle0Triggers) {
        this.filterSingle0Triggers = filterSingle0Triggers;
    }

    public void setFilterSingle1Triggers(boolean filterSingle1Triggers) {
        this.filterSingle1Triggers = filterSingle1Triggers;
    }

    public void setFilterPair0Triggers(boolean filterPair0Triggers) {
        this.filterPair0Triggers = filterPair0Triggers;
    }

    public void setFilterPair1Triggers(boolean filterPair1Triggers) {
        this.filterPair1Triggers = filterPair1Triggers;
    }

    public void setMaxSamplePosition(int maxSamplePosition) {
        this.maxSamplePosition = maxSamplePosition;
    }

    public void setTimeWindowWeight(int timeWindowWeight) {
        this.timeWindowWeight = timeWindowWeight;
    }

    public void setMaxSampleMonitorStart(int maxSampleMonitorStart) {
        this.maxSampleMonitorStart = maxSampleMonitorStart;
    }

    public void setMaxSampleMonitorPeriod(int maxSampleMonitorPeriod) {
        this.maxSampleMonitorPeriod = maxSampleMonitorPeriod;
    }

    public void setOccupancyMonitorStart(int occupancyMonitorStart) {
        this.occupancyMonitorStart = occupancyMonitorStart;
    }

    public void setOccupancyMonitorPeriod(int occupancyMonitorPeriod) {
        this.occupancyMonitorPeriod = occupancyMonitorPeriod;
    }

    public void setMinPeakOccupancy(double minPeakOccupancy) {
        this.minPeakOccupancy = minPeakOccupancy;
    }

    public void setMaxPeakOccupancy(double maxPeakOccupancy) {
        this.maxPeakOccupancy = maxPeakOccupancy;
    }

    public void setSaveRootFile(boolean saveRootFile) {
        this.saveRootFile = saveRootFile;
    }

    public void setOccupancyYRange1(double occupancyYRange1) {
        this.occupancyYRange1 = occupancyYRange1;
    }
    
    public void setOccupancyYRange2(double occupancyYRange2) {
        this.occupancyYRange2 = occupancyYRange2;
    }
    
    /**
     * Get the global strip position of a physical channel number for a given
     * sensor.
     *
     * @param sensor : HpsSiSensor
     * @param physicalChannel : physical channel number
     * @return The strip position (mm) in the global coordinate system
     */
    private Hep3Vector getStripPosition(HpsSiSensor sensor, int physicalChannel) {
        return stripPositions.get(sensor).get(physicalChannel);
    }

    /**
     * For each sensor, create a mapping between a physical channel number and
     * it's global strip position.
     */
    // TODO: Move this to a utility class
    private void createStripPositionMap() {
        for (HpsSiSensor sensor : sensors)
            stripPositions.put(sensor, createStripPositionMap(sensor));
    }

    public static Map<Integer, Hep3Vector> createStripPositionMap(HpsSiSensor sensor) {
        Map<Integer, Hep3Vector> positionMap = new HashMap<Integer, Hep3Vector>();
        for (ChargeCarrier carrier : ChargeCarrier.values())
            if (sensor.hasElectrodesOnSide(carrier)) {
                SiStrips strips = (SiStrips) sensor.getReadoutElectrodes(carrier);
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

    /**
     * Create a plotter style.
     *
     * @param xAxisTitle : Title of the x axis
     * @param sensor : HpsSiSensor associated with the plot. This is used to set
     * certain attributes based on the position of the sensor.
     * @return plotter style
     */
    // TODO: Move this to a utilities class
    IPlotterStyle createOccupancyPlotStyle(String xAxisTitle, HpsSiSensor sensor, boolean isAlarming) {
        // Create a default style
        IPlotterStyle style = this.plotterFactory.createPlotterStyle();

        // Set the style of the X axis
        style.xAxisStyle().setLabel(xAxisTitle);
        style.xAxisStyle().labelStyle().setFontSize(14);
        style.xAxisStyle().setVisible(true);

        // Set the style of the Y axis
        style.yAxisStyle().setLabel("Occupancy");
        style.yAxisStyle().labelStyle().setFontSize(14);
        style.yAxisStyle().setVisible(true);

        // Turn off the histogram grid
        style.gridStyle().setVisible(false);

        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(3);
        style.dataStyle().fillStyle().setVisible(true);
        style.dataStyle().fillStyle().setOpacity(.30);
        if (sensor.isTopLayer()) {
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        } else {
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
        }
        style.dataStyle().errorBarStyle().setVisible(false);

        // Turn off the legend
        style.legendBoxStyle().setVisible(false);

        style.regionBoxStyle().backgroundStyle().setOpacity(.20);
        setBackgroundColor(style, sensor.isAxial(), isAlarming);

        return style;
    }

    private void setBackgroundColor(IPlotterStyle style, boolean isAxial, boolean isAlarming) {
        if (isAlarming) {
            style.regionBoxStyle().backgroundStyle().setColor("246, 34, 34, 1");
            return;
        }
        if (isAxial)
            style.regionBoxStyle().backgroundStyle().setColor("246, 246, 34, 1");
    }

    /**
     * Clear all histograms of it's current data.
     */
    private void resetPlots() {

        // Clear the hit counter map of all previously stored data.
        occupancyMap.clear();

        // Since all plots are mapped to the name of a sensor, loop
        // through the sensors, get the corresponding plots and clear them.
        for (HpsSiSensor sensor : sensors) {

            // Clear the occupancy plots.
            occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();

            if (enablePositionPlots) {
                positionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
                clusterPositionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
                clusterPositionPlotCounts.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
            }

            if (enableMaxSamplePlots)
                maxSamplePositionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();

            // Reset the hit counters.
            occupancyMap.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), new int[640]);
        }
    }

    private static int getLayerNumber(HpsSiSensor sensor) {
        return (int) Math.ceil(((double) sensor.getLayerNumber()) / 2);
    }

    @Override
    protected void detectorChanged(Detector detector) {

        // Get the HpsSiSensor objects from the geometry
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);

        // If there were no sensors found, throw an exception
        if (sensors.isEmpty())
            throw new RuntimeException("There are no sensors associated with this detector");

        // For each sensor, create a mapping between a physical channel number
        // and the global strip position
        this.createStripPositionMap();

        // // If the tree already exist, clear all existing plots of any old data
        // // they might contain.
        // if (tree != null) {
        // this.resetPlots();
        // return;
        // }
        // tree = analysisFactory.createTreeFactory().create();
        tree = AIDA.defaultInstance().tree();
        tree.cd("/");
        histogramFactory = analysisFactory.createHistogramFactory(tree);

        plotters.put("Occupancy: L0-L3", plotterFactory.create("Occupancy: L0-L3"));
        plotters.get("Occupancy: L0-L3").createRegions(4, 4);
        plotters.put("Occupancy: L4-L6", plotterFactory.create("Occupancy: L4-L6"));
        plotters.get("Occupancy: L4-L6").createRegions(6, 4);

        occupancyStatus.setStatus(StatusCode.UNKNOWN, "Not enough statistics yet.");

        if (enablePositionPlots) {
            plotters.put("Occupancy vs Position", plotterFactory.create("Occupancy vs Position"));
            plotters.get("Occupancy vs Position").createRegions(6, 6);
            plotters.put("Cluster occupancy vs Position", plotterFactory.create("Cluster occupancy vs Position"));
            plotters.get("Cluster occupancy vs Position").createRegions(6, 6);
        }

        if (enableMaxSamplePlots) {
            plotters.put("Max Sample Number: L0-L3", plotterFactory.create("Max Sample Number: L0-L3"));
            plotters.get("Max Sample Number: L0-L3").createRegions(4, 4);
            plotters.put("Max Sample Number: L4-L6", plotterFactory.create("Max Sample Number: L4-L6"));
            plotters.get("Max Sample Number: L4-L6").createRegions(6, 4);
            maxSampleStatus.setStatus(StatusCode.UNKNOWN, "Not enough statistics yet.");
        } else
            maxSampleStatus.setStatus(StatusCode.UNKNOWN, "Monitor disabled in steering file.");

        for (HpsSiSensor sensor : sensors) {

            occupancyPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Occupancy", 640, 0, 640));

            if (sensor.getLayerNumber() < 9)
                plotters.get("Occupancy: L0-L3")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createOccupancyPlotStyle("Physical Channel", sensor, false));
            else
                plotters.get("Occupancy: L4-L6")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createOccupancyPlotStyle("Physical Channel", sensor, false));

            if (enablePositionPlots) {
                if (sensor.isTopLayer()) {
                    positionPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())
                            + " - Occupancy vs Position", 1000, 0, 60));
                    clusterPositionPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())
                            + " - Cluster occupancy vs Position", 1000, 0, 60));
                    clusterPositionPlotCounts.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())
                            + " - Cluster count vs Position", 1000, 0, 60));
                } else {
                    positionPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())
                            + " - Occupancy vs Position", 1000, -60, 0));
                    clusterPositionPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())
                            + " - Cluster occupancy vs Position", 1000, -60, 0));
                    clusterPositionPlotCounts.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())
                            + " - Cluster count vs Position", 1000, -60, 0));
                }

                plotters.get("Occupancy vs Position")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(positionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createOccupancyPlotStyle("Distance from Beam [mm]", sensor, false));
                plotters.get("Cluster occupancy vs Position")
                        .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                        .plot(clusterPositionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                this.createOccupancyPlotStyle("Distance from Beam [mm]", sensor, false));
            }
            occupancyMap.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), new int[640]);

            if (enableMaxSamplePlots) {
                maxSamplePositionPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                        histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Max Sample Number", 6, -0.5, 5.5));
                if (sensor.getLayerNumber() < 9)
                    plotters.get("Max Sample Number: L0-L3")
                            .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                            .plot(maxSamplePositionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                    this.createOccupancyPlotStyle("Max Sample Number", sensor, false));
                else
                    plotters.get("Max Sample Number: L4-L6")
                            .region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                            .plot(maxSamplePositionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                    this.createOccupancyPlotStyle("Max Sample Number", sensor, false));
            }
        }

        IPlotter plotter = plotters.get("Occupancy: L0-L3");
        for (int i = 0; i < plotter.numberOfRegions(); i++) {
            plotter.region(i).setYLimits(occupancyYRange1);
        }

        plotter = plotters.get("Occupancy: L4-L6");
        for (int i = 0; i < plotter.numberOfRegions(); i++) {
            plotter.region(i).setYLimits(occupancyYRange2);
        }
                
        for (IPlotter plotterShow : plotters.values())
            plotterShow.show();
    }

    private boolean passTriggerFilter(List<GenericObject> triggerBanks) {

        // Loop through the collection of banks and get the TI banks.
        for (GenericObject triggerBank : triggerBanks)

            // If the bank contains TI data, process it
            if (AbstractIntData.getTag(triggerBank) == TIData.BANK_TAG) {

                TIData tiData = new TIData(triggerBank);

                if (filterPulserTriggers && tiData.isPulserTrigger())
                    return false;
                else if (filterSingle0Triggers && tiData.isSingle0Trigger())
                    return false;
                else if (filterSingle1Triggers && tiData.isSingle1Trigger())
                    return false;
                else if (filterPair0Triggers && tiData.isPair0Trigger())
                    return false;
                else if (filterPair1Triggers && tiData.isPair1Trigger())
                    return false;
            }
        return true;
    }

    @Override
    public void process(EventHeader event) {

        // Get the run number from the event and store it. This will be used
        // when writing the plots out to a ROOT file
        if (runNumber == -1)
            runNumber = event.getRunNumber();
        if (enableTriggerFilter && event.hasCollection(GenericObject.class, triggerBankCollectionName)) {
            LOGGER.info("Filtering Event");
            // Get the list of trigger banks from the event
            List<GenericObject> triggerBanks = event.get(GenericObject.class, triggerBankCollectionName);

            // Apply the trigger filter
            if (!passTriggerFilter(triggerBanks))
                return;
        }

        // If the event doesn't have a collection of RawTrackerHit's, skip it.
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            LOGGER.warning("No SVT RawTrackerHits in this event.");
            return;
        }
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

//        System.out.println("Number of SVT RawTrackerHts = " + rawHits.size());
        if (dropSmallHitEvents && SvtPlotUtils.countSmallHits(rawHits) > 3)
            return;

        if (resetPeriod > 0 && eventCount > resetPeriod) { // reset occupancy numbers after resetPeriod events
            eventCount = 0;
            resetPlots();
        }

        eventCount++;

        // Increment strip hit count.
        for (RawTrackerHit rawHit : rawHits) {
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
                occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(((HpsSiSensor) rawHit.getDetectorElement()).getName()))[rawHit
                        .getIdentifierFieldValue("strip")]++; //                System.out.println("Filling occupancy");

            if (enableMaxSamplePlots)
                maxSamplePositionPlots.get(SvtPlotUtils.fixSensorNumberLabel(((HpsSiSensor) rawHit.getDetectorElement()).getName())).fill(
                        maxSamplePositionFound);
        }

        // Fill the strip cluster counts if available
        if (event.hasCollection(SiTrackerHitStrip1D.class, stripClusterCollectionName)) {
            List<SiTrackerHitStrip1D> stripHits1D = event.get(SiTrackerHitStrip1D.class, stripClusterCollectionName);
            for (SiTrackerHitStrip1D h : stripHits1D) {
                SiTrackerHitStrip1D global = h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);
                Hep3Vector pos_global = global.getPositionAsVector();
                if (enableClusterTimeCuts) {
                    if (h.getTime() < clusterTimeCutMax && h.getTime() > clusterTimeCutMin)
                        clusterPositionPlotCounts.get(
                                SvtPlotUtils.fixSensorNumberLabel(((HpsSiSensor) h.getRawHits().get(0).getDetectorElement()).getName())).fill(
                                pos_global.y());
                } else
                    clusterPositionPlotCounts.get(SvtPlotUtils.fixSensorNumberLabel(((HpsSiSensor) h.getRawHits().get(0).getDetectorElement()).getName()))
                            .fill(pos_global.y());
            }
        }

        if (enableMaxSamplePlots && eventCount > maxSampleMonitorStart && eventCount % maxSampleMonitorPeriod == 0)
            checkMaxSample();

        if (eventCount > occupancyMonitorStart && eventCount % occupancyMonitorPeriod == 0)
            checkOccupancy();

        // Plot strip occupancies.
        if (eventCount % eventRefreshRate == 0)
            for (HpsSiSensor sensor : sensors) {
                int[] strips = occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()));
                occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
                if (enablePositionPlots)
                    positionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
                for (int channel = 0; channel < strips.length; channel++) {
                    double stripOccupancy = (double) strips[channel] / (double) eventCount;

                    stripOccupancy /= this.timeWindowWeight;
                    //                  System.out.println("channel " + channel + " occupancy = " + stripOccupancy);
                    occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(channel, stripOccupancy);

                    if (enablePositionPlots) {
                        double stripPosition = this.getStripPosition(sensor, channel).y();
                        positionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(stripPosition, stripOccupancy);
                    }
                }
                if (enablePositionPlots) {
                    clusterPositionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();
                    IHistogram1D h = clusterPositionPlotCounts.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()));
                    for (int bin = 0; bin < h.axis().bins(); ++bin) {
                        int y = h.binEntries(bin);
                        double stripClusterOccupancy = (double) y / (double) eventCount;
                        double x = h.axis().binCenter(bin);
                        clusterPositionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(x, stripClusterOccupancy);
                    }
                }

            }

        if (plotters.get("Occupancy") != null)
            plotters.get("Occupancy").refresh();

    }

    private void checkMaxSample() {
        StatusCode oldStatus = maxSampleStatus.getStatusCode();
        boolean isSystemOK = true;

        String label = "foo";
        for (HpsSiSensor sensor : sensors) {

            if (sensor.getLayerNumber() < 9)
                label = "Max Sample Number: L0-L3";
            else
                label = "Max Sample Number: L4-L6";
            IHistogram1D maxSamplePlot = maxSamplePositionPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()));
//            IPlotterRegion region = plotters.get("Max Sample Number").region(SvtPlotUtils.computePlotterRegion(sensor));
            IPlotterRegion region = plotters.get(label).region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor));

            boolean isSensorOK = maxSamplePlot.binEntries(maxSamplePosition) > maxSamplePlot
                    .binEntries(maxSamplePosition - 1)
                    && maxSamplePlot.binEntries(maxSamplePosition) > maxSamplePlot.binEntries(maxSamplePosition + 1);
            if (!isSensorOK) {
                isSystemOK = false;
                if (oldStatus != StatusCode.ALARM)
                    maxSampleStatus.setStatus(StatusCode.ALARM, "Sensor " + SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " looks out of time.");
                IPlotterStyle plotterStyle = createOccupancyPlotStyle("Max Sample Number", sensor, true);
                // region.clear();
                // region.plot(maxSamplePlot, plotterStyle);
                region.applyStyle(plotterStyle);
                // region.style().regionBoxStyle().backgroundStyle().setColor("246, 34, 34, 1");
                // setBackgroundColor(region.style(),sensor.isAxial(),true);

            } else {
                IPlotterStyle plotterStyle = createOccupancyPlotStyle("Max Sample Number", sensor, false);
                // region.clear();
                // region.plot(maxSamplePlot, plotterStyle);
                region.applyStyle(plotterStyle);
                // setBackgroundColor(region.style(),sensor.isAxial(),false);
            }
        }
        if (isSystemOK)
            if (oldStatus != StatusCode.OKAY)
                maxSampleStatus.setStatus(StatusCode.OKAY, "All sensors are timed in.");
    }

    private void checkOccupancy() {
        StatusCode oldStatus = occupancyStatus.getStatusCode();
        boolean isSystemOK = true;
        String label = "foo";
        for (HpsSiSensor sensor : sensors) {
            if (sensor.getLayerNumber() < 9)
                label = "Occupancy: L0-L3";
            else
                label = "Occupancy: L4-L6";

            IHistogram1D occupancyPlot = occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()));
            IPlotterRegion region = plotters.get(label).region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor));

            double apvOccupancy[] = new double[5];
            for (int i = 0; i < occupancyPlot.axis().bins(); i++)
                apvOccupancy[i / 128] += occupancyPlot.binHeight(i);
            for (int i = 0; i < 5; i++)
                apvOccupancy[i] /= 128.0;

            boolean isSensorOK = isOccupancyOK(apvOccupancy);
            if (!isSensorOK) {
                LOGGER.info(String.format("%s: %f %f %f %f %f\n",
                        SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), apvOccupancy[0], apvOccupancy[1],
                        apvOccupancy[2], apvOccupancy[3], apvOccupancy[4]));
                isSystemOK = false;
                if (oldStatus != StatusCode.ALARM)
                    occupancyStatus.setStatus(StatusCode.ALARM, "Sensor " + SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " occupancy abnormal.");
                IPlotterStyle plotterStyle = createOccupancyPlotStyle("Max Sample Number", sensor, true);
                // region.clear();
                // region.plot(occupancyPlot, plotterStyle);
                region.applyStyle(plotterStyle);

            } else {
                IPlotterStyle plotterStyle = createOccupancyPlotStyle("Max Sample Number", sensor, false);
                // region.clear();
                // region.plot(occupancyPlot, plotterStyle);
                region.applyStyle(plotterStyle);
            }
        }
        if (isSystemOK)
            if (oldStatus != StatusCode.OKAY)
                occupancyStatus.setStatus(StatusCode.OKAY, "Occupancy looks OK.");
    }

    private boolean isOccupancyOK(double[] apvOccupancy) {
        double peakOccupancy = 0;
        int highestApv = -1;
        for (int i = 0; i < 5; i++)
            if (apvOccupancy[i] > peakOccupancy) {
                peakOccupancy = apvOccupancy[i];
                highestApv = i;
            }
        if (highestApv != 0 && highestApv != 4) {
            LOGGER.warning("peak occupancy not at edge");
            return false;
        }
        if (peakOccupancy > maxPeakOccupancy || peakOccupancy < minPeakOccupancy) {
            LOGGER.warning("peak occupancy out of range");
            return false;
        }
        if (highestApv == 0)
            for (int i = 4; i > 0; i--) {
                if (apvOccupancy[i] < 0.1 * peakOccupancy || apvOccupancy[i] < minPeakOccupancy)
                    continue; // skip through the tail end of the sensor
                if (0.9 * apvOccupancy[i] > apvOccupancy[i - 1]) {
                    LOGGER.warning("occupancy not monotonic");
                    return false;
                }
            }
        else if (highestApv == 4)
            for (int i = 0; i < 4; i++) {
                if (apvOccupancy[i] < 0.1 * peakOccupancy || apvOccupancy[i] < minPeakOccupancy)
                    continue; // skip through the tail end of the sensor
                if (0.9 * apvOccupancy[i] > apvOccupancy[i + 1]) {
                    LOGGER.warning("occupancy not monotonic");
                    return false;
                }
            }

        return true;
    }

    @Override
    public void endOfData() {

        if (saveRootFile) {
            String rootFile = "run" + runNumber + "_occupancy.root";
            RootFileStore store = new RootFileStore(rootFile);
            try {
                store.open();
                store.add(tree);
                store.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("%===============================================================================%");
        System.out.println("%======================== Active Edge Sensor Occupancies =======================%");
        System.out.println("%===============================================================================%");
        System.out.println("% Total Events: " + eventCount);
        // Calculate the occupancies at the sensor edge

        int[] topActiveEdgeStripOccupancy = new int[7];
        int[] bottomActiveEdgeStripOccupancy = new int[7];
        for (HpsSiSensor sensor : sensors)
            if (sensor.isTopLayer() && sensor.isAxial())

                if (sensor.getSide().equals(HpsSiSensor.ELECTRON_SIDE)) {
                    System.out.println("% Top Layer " + getLayerNumber(sensor) + " Hit Counts: "
                            + occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[1]);
                    topActiveEdgeStripOccupancy[getLayerNumber(sensor) - 1] += occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[1];
                } else {
                    System.out.println("% Top Layer " + getLayerNumber(sensor) + " Hit Counts: "
                            + occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[638]);
                    topActiveEdgeStripOccupancy[getLayerNumber(sensor) - 1] += occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[638];
                }
            else if (sensor.isBottomLayer() && sensor.isAxial())
                if (sensor.getSide().equals(HpsSiSensor.ELECTRON_SIDE)) {
                    System.out.println("% Bottom Layer " + getLayerNumber(sensor) + " Hit Counts: "
                            + occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[1]);
                    bottomActiveEdgeStripOccupancy[getLayerNumber(sensor) - 1] += occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[1];
                } else {
                    System.out.println("% Bottom Layer " + getLayerNumber(sensor) + " Hit Counts: "
                            + occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[638]);
                    bottomActiveEdgeStripOccupancy[getLayerNumber(sensor) - 1] += occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))[638];
                }

        for (int layerN = 0; layerN < 6; layerN++) {
            double topStripOccupancy = (double) topActiveEdgeStripOccupancy[layerN] / (double) eventCount;
            topStripOccupancy /= this.timeWindowWeight;
            System.out.println("% Top Layer " + (layerN + 1) + ": Occupancy in " + (24 / this.timeWindowWeight)
                    + " ns window: " + topStripOccupancy);
            double botStripOccupancy = (double) bottomActiveEdgeStripOccupancy[layerN] / (double) eventCount;
            botStripOccupancy /= this.timeWindowWeight;
            System.out.println("% Bottom Layer " + (layerN + 1) + ": Occupancy in " + (24 / this.timeWindowWeight)
                    + " ns window: " + botStripOccupancy);
        }
        System.out.println("%===============================================================================%");
        System.out.println("%===============================================================================%");
    }
}
