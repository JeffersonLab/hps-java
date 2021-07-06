package org.hps.monitoring.drivers.monitoring2021.headless;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.SvtPlotUtils;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;
import hep.physics.vec.Hep3Vector;

/**
 * This Driver makes plots of SVT sensor occupancies across a run.
 *
 * 6/6/19: modified this to work with SVT upgrade including "L0"; separated all
 * plotters into 2
 * so that we have one page for L1-4 and one for L5-7
 */
public class SvtClusterOccHeadless extends Driver {
  
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();   
    private IHistogramFactory histogramFactory = null;

    // Histogram maps  
    private static Map<String, IHistogram1D> occupancyPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, int[]> occupancyMap = new HashMap<String, int[]>();

    private List<HpsSiSensor> sensors;
    private Map<HpsSiSensor, Map<Integer, Hep3Vector>> stripPositions = new HashMap<HpsSiSensor, Map<Integer, Hep3Vector>>();

    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String triggerBankCollectionName = "TriggerBank";
    private String stripClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";

    private int timeWindowWeight = 1;
    private int eventCount = 0;
    private int eventRefreshRate = 1;
    private int runNumber = -1;

    private boolean enableTriggerFilter = false;
    private boolean filterPulserTriggers = false;
    private boolean filterSingle0Triggers = false;
    private boolean filterSingle1Triggers = false;
    private boolean filterPair0Triggers = false;
    private boolean filterPair1Triggers = false;

    private boolean dropSmallHitEvents = false;

    private boolean enableClusterTimeCuts = true;
    private double clusterTimeCutMax = 20.0;
    private double clusterTimeCutMin = 0.0;

    private boolean enableClusterChargeCut = true;
    private double clusterChargeCut = 400;

    private boolean saveRootFile = true;

    public SvtClusterOccHeadless() {
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

    public void setEnableTriggerFilter(boolean enableTriggerFilter) {
        this.enableTriggerFilter = enableTriggerFilter;
    }

    public void setEnableClusterTimeCuts(boolean enableClusterTimeCuts) {
        this.enableClusterTimeCuts = enableClusterTimeCuts;
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

    public void setSaveRootFile(boolean saveRootFile) {
        this.saveRootFile = saveRootFile;
    }

    /**
     * For each sensor, create a mapping between a physical channel number and
     * it's global strip position.
     */
    private void createStripPositionMap() {
        for (HpsSiSensor sensor : sensors)
            stripPositions.put(sensor, createStripPositionMap(sensor));
    }

    public static Map<Integer, Hep3Vector> createStripPositionMap(HpsSiSensor sensor) {
        Map<Integer, Hep3Vector> positionMap = new HashMap<Integer, Hep3Vector>();
        for (ChargeCarrier carrier : ChargeCarrier.values())
            if (sensor.hasElectrodesOnSide(carrier)) {
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
      
        tree = AIDA.defaultInstance().tree();
        tree.cd("/");// aida.tree().cd("/");
        histogramFactory = analysisFactory.createHistogramFactory(tree);  

        for (HpsSiSensor sensor : sensors) {

            occupancyPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                    histogramFactory.createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " - Occupancy", 640, 0, 640));       

            occupancyMap.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), new int[640]);

        }
      
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
            System.out.println("SensorOccupancyPlotsDriver::  Filtering Event");
            // Get the list of trigger banks from the event
            List<GenericObject> triggerBanks = event.get(GenericObject.class, triggerBankCollectionName);

            // Apply the trigger filter
            if (!passTriggerFilter(triggerBanks))
                return;
        }

        // If the event doesn't have a collection of RawTrackerHit's, skip it.
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            System.out.println("No SVT RawTrackerHits in this event???");
            return;
        }
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

//        System.out.println("Number of SVT RawTrackerHts = " + rawHits.size());
        if (dropSmallHitEvents && SvtPlotUtils.countSmallHits(rawHits) > 3)
            return;
   
        eventCount++;

        // Fill the strip cluster counts if available
        if (event.hasCollection(SiTrackerHitStrip1D.class, stripClusterCollectionName)) {
            List<SiTrackerHitStrip1D> stripHits1D = event.get(SiTrackerHitStrip1D.class, stripClusterCollectionName);
            for (SiTrackerHitStrip1D h : stripHits1D) {
                if (enableClusterChargeCut && h.getdEdx() / DopedSilicon.ENERGY_EHPAIR < clusterChargeCut)
                    continue;
                RawTrackerHit rth = h.getRawHits().get(0);
                if ((h.getTime() < clusterTimeCutMax && h.getTime() > clusterTimeCutMin) || !enableClusterTimeCuts)
                    occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(((HpsSiSensor) rth.getDetectorElement()).getName()))[rth
                            .getIdentifierFieldValue("strip")]++; //   
            }
        }

        // Plot strip occupancies.
        if (eventCount % eventRefreshRate == 0)
            for (HpsSiSensor sensor : sensors) {
                int[] strips = occupancyMap.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()));
                occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).reset();

                for (int channel = 0; channel < strips.length; channel++) {
                    double stripOccupancy = (double) strips[channel] / (double) eventCount;

                    stripOccupancy /= this.timeWindowWeight;
                    //                  System.out.println("channel " + channel + " occupancy = " + stripOccupancy);
                    occupancyPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(channel, stripOccupancy);
                }
            }    
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
