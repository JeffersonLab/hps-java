package org.hps.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.physics.vec.Hep3Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Based off of the monitoring plots driver
 * These will be used for the NIM paper
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Matt Solt <mrsolt@slac.stanford.edu>
 */
public class OccupancyPlots extends Driver {

    //plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private IHistogramFactory histogramFactory = null;
    protected AIDA aida = AIDA.defaultInstance();

    // Histogram maps
    private static Map<String, IHistogram1D> occupancyPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> clusterOccupancyPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> positionPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> clusterPositionPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, int[]> occupancyMap = new HashMap<String, int[]>();
    private static Map<String, int[]> clusterOccupancyMap = new HashMap<String, int[]>();
    private static Map<String, IHistogram1D> maxSamplePositionPlots = new HashMap<String, IHistogram1D>();

    private List<HpsSiSensor> sensors;
    private Map<HpsSiSensor, Map<Integer, Hep3Vector>> stripPositions = new HashMap<HpsSiSensor, Map<Integer, Hep3Vector>>();

    //collection names
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

    private boolean enableClusterTimeCuts = true;
    private double clusterTimeCutMax = 4.0;
    private double clusterTimeCutMin = -4.0;

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
        for (HpsSiSensor sensor : sensors) {
            stripPositions.put(sensor, createStripPositionMap(sensor));
        }
    }

    public static Map<Integer, Hep3Vector> createStripPositionMap(HpsSiSensor sensor) {
        Map<Integer, Hep3Vector> positionMap = new HashMap<Integer, Hep3Vector>();
        for (ChargeCarrier carrier : ChargeCarrier.values()) {
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
        }
        return positionMap;
    }
    
    //Grab the channel associated with the cluster based on position.
    private int getClusterChan(SiTrackerHitStrip1D h, HpsSiSensor sensor){
        List<RawTrackerHit> rawhits = h.getRawHits();
        if(rawhits.size() == 1)
            return rawhits.get(0).getIdentifierFieldValue("strip");
        SiTrackerHitStrip1D global = h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);
        Hep3Vector pos_global = global.getPositionAsVector();
        double clusterHitPos = pos_global.y();
        RawTrackerHit hit = rawhits.get(0);
        int chan = hit.getIdentifierFieldValue("strip");
        double diffmin = Math.abs(getStripPosition(sensor,chan).y() - clusterHitPos);
        for(RawTrackerHit rawhit : rawhits){
            int chanhit = rawhit.getIdentifierFieldValue("strip");
            double diff = Math.abs(getStripPosition(sensor,chanhit).y() - clusterHitPos);
            if(diff < diffmin){
                diffmin = diff;
                hit = rawhit;
            }
        }
        return hit.getIdentifierFieldValue("strip");
    }

    /**
     * Clear all histograms of it's current data.
     */
    private void resetPlots() {

        // Clear the hit counter map of all previously stored data.
        occupancyMap.clear();
        clusterOccupancyMap.clear();

        // Since all plots are mapped to the name of a sensor, loop
        // through the sensors, get the corresponding plots and clear them.
        for (HpsSiSensor sensor : sensors) {

            // Clear the occupancy plots.
            occupancyPlots.get(sensor.getName()).reset();
            clusterOccupancyPlots.get(sensor.getName()).reset();

            if (enablePositionPlots) {
                positionPlots.get(sensor.getName()).reset();
                clusterPositionPlots.get(sensor.getName()).reset();
                //clusterPositionPlotCounts.get(sensor.getName()).reset();
            }

            if (enableMaxSamplePlots) {
                maxSamplePositionPlots.get(sensor.getName()).reset();
            }

            // Reset the hit counters.
            occupancyMap.put(sensor.getName(), new int[640]);
            clusterOccupancyMap.put(sensor.getName(), new int[640]);
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
        if (sensors.isEmpty()) {
            throw new RuntimeException("There are no sensors associated with this detector");
        }

        // For each sensor, create a mapping between a physical channel number
        // and the global strip position
        this.createStripPositionMap();
        
        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = analysisFactory.createHistogramFactory(tree);
        
        for(HpsSiSensor sensor : sensors){
            int nChan = sensor.getNumberOfChannels();
            occupancyPlots.put(sensor.getName(),histogramFactory.createHistogram1D(sensor.getName() + " - Occupancy", nChan, 0, nChan));
            clusterOccupancyPlots.put(sensor.getName(),histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Occupancy", nChan, 0, nChan));
            if (enablePositionPlots){
                positionPlots.put(sensor.getName(),histogramFactory.createHistogram1D(sensor.getName() + " - Occupancy vs Position", 1000, -60, 60));
                clusterPositionPlots.put(sensor.getName(),histogramFactory.createHistogram1D(sensor.getName() + " - Cluster occupancy vs Position", 1000, -60, 60));
            }
            occupancyMap.put(sensor.getName(), new int[640]);
            clusterOccupancyMap.put(sensor.getName(), new int[640]);
            if (enableMaxSamplePlots)
                maxSamplePositionPlots.put(sensor.getName(),histogramFactory.createHistogram1D(sensor.getName() + " - Max Sample Number", 6, -0.5, 5.5));
        }
    }

    private boolean passTriggerFilter(List<GenericObject> triggerBanks) {

        // Loop through the collection of banks and get the TI banks.
        for (GenericObject triggerBank : triggerBanks) {

            // If the bank contains TI data, process it
            if (AbstractIntData.getTag(triggerBank) == TIData.BANK_TAG) {

                TIData tiData = new TIData(triggerBank);

                if (filterPulserTriggers && tiData.isPulserTrigger()) {
                    return false;
                } else if (filterSingle0Triggers && tiData.isSingle0Trigger()) {
                    return false;
                } else if (filterSingle1Triggers && tiData.isSingle1Trigger()) {
                    return false;
                } else if (filterPair0Triggers && tiData.isPair0Trigger()) {
                    return false;
                } else if (filterPair1Triggers && tiData.isPair1Trigger()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void process(EventHeader event) {

        // Get the run number from the event and store it. This will be used
        // when writing the plots out to a ROOT file
        if (runNumber == -1) {
            runNumber = event.getRunNumber();
        }

        if (enableTriggerFilter && event.hasCollection(GenericObject.class, triggerBankCollectionName)) {

            // Get the list of trigger banks from the event
            List<GenericObject> triggerBanks = event.get(GenericObject.class, triggerBankCollectionName);

            // Apply the trigger filter
            if (!passTriggerFilter(triggerBanks)) {
                return;
            }
        }

        // If the event doesn't have a collection of RawTrackerHit's, skip it.
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            return;
        }
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

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
            for (int sampleN = 0; sampleN < 6; sampleN++) {
                if (adcValues[sampleN] > maxAmplitude) {
                    maxAmplitude = adcValues[sampleN];
                    maxSamplePositionFound = sampleN;
                }
            }

            if (maxSamplePosition == -1 || maxSamplePosition == maxSamplePositionFound) {
                occupancyMap.get(((HpsSiSensor) rawHit.getDetectorElement()).getName())[rawHit
                        .getIdentifierFieldValue("strip")]++;
            }

            if (enableMaxSamplePlots) {
                maxSamplePositionPlots.get(((HpsSiSensor) rawHit.getDetectorElement()).getName()).fill(
                        maxSamplePositionFound);
            }
        }

        // Fill the strip cluster counts if available
        if (event.hasCollection(SiTrackerHitStrip1D.class, stripClusterCollectionName)) {
            List<SiTrackerHitStrip1D> stripHits1D = event.get(SiTrackerHitStrip1D.class, stripClusterCollectionName);
            for (SiTrackerHitStrip1D h : stripHits1D) {
                int chan = getClusterChan(h,((HpsSiSensor) h.getRawHits().get(0).getDetectorElement()));
                
                if (enableClusterTimeCuts) {
                    if (h.getTime() < clusterTimeCutMax && h.getTime() > clusterTimeCutMin) {
                        clusterOccupancyMap.get(((HpsSiSensor) h.getRawHits().get(0).getDetectorElement()).getName())[chan]++;
                    }
                } else {
                    clusterOccupancyMap.get(((HpsSiSensor) h.getRawHits().get(0).getDetectorElement()).getName())[chan]++;
                }
            }
        }

        // Plot strip occupancies.
        if (eventCount % eventRefreshRate == 0) {
            for (HpsSiSensor sensor : sensors) {
                int[] strips = occupancyMap.get(sensor.getName());
                int[] clusterStrips = clusterOccupancyMap.get(sensor.getName());
                occupancyPlots.get(sensor.getName()).reset();
                clusterOccupancyPlots.get(sensor.getName()).reset();
                if (enablePositionPlots) {
                    positionPlots.get(sensor.getName()).reset();
                }
                for (int channel = 0; channel < strips.length; channel++) {
                    double stripOccupancy = (double) strips[channel] / (double) eventCount;
                    stripOccupancy /= this.timeWindowWeight;
                    occupancyPlots.get(sensor.getName()).fill(channel, stripOccupancy);

                    if (enablePositionPlots) {
                        double stripPosition = this.getStripPosition(sensor, channel).y();
                        positionPlots.get(sensor.getName()).fill(stripPosition, stripOccupancy);
                    }
                }
                for (int channel = 0; channel < clusterStrips.length; channel++) {
                    double clusterOccupancy = (double) clusterStrips[channel] / (double) eventCount;
                    clusterOccupancy /= this.timeWindowWeight;
                    clusterOccupancyPlots.get(sensor.getName()).fill(channel, clusterOccupancy);

                    if (enablePositionPlots) {
                        double clusterPosition = this.getStripPosition(sensor, channel).y();
                        clusterPositionPlots.get(sensor.getName()).fill(clusterPosition, clusterOccupancy);
                    }
                }
            }
        }
    }

    @Override
    public void endOfData() {

        System.out.println("%===============================================================================%");
        System.out.println("%======================== Active Edge Sensor Occupancies =======================%");
        System.out.println("%===============================================================================%");
        System.out.println("% Total Events: " + eventCount);
        // Calculate the occupancies at the sensor edge
        int[] topActiveEdgeStripOccupancy = new int[6];
        int[] bottomActiveEdgeStripOccupancy = new int[6];
        for (HpsSiSensor sensor : sensors) {
            if (sensor.isTopLayer() && sensor.isAxial()) {
                if (sensor.getSide().equals(HpsSiSensor.ELECTRON_SIDE)) {
                    System.out.println("% Top Layer " + getLayerNumber(sensor) + " Hit Counts: "
                            + occupancyMap.get(sensor.getName())[1]);
                    topActiveEdgeStripOccupancy[getLayerNumber(sensor) - 1] += occupancyMap.get(sensor.getName())[1];
                } else {
                    System.out.println("% Top Layer " + getLayerNumber(sensor) + " Hit Counts: "
                            + occupancyMap.get(sensor.getName())[638]);
                    topActiveEdgeStripOccupancy[getLayerNumber(sensor) - 1] += occupancyMap.get(sensor.getName())[638];
                }
            } else if (sensor.isBottomLayer() && sensor.isAxial()) {
                if (sensor.getSide().equals(HpsSiSensor.ELECTRON_SIDE)) {
                    System.out.println("% Bottom Layer " + getLayerNumber(sensor) + " Hit Counts: "
                            + occupancyMap.get(sensor.getName())[1]);
                    bottomActiveEdgeStripOccupancy[getLayerNumber(sensor) - 1] += occupancyMap.get(sensor.getName())[1];
                } else {
                    System.out.println("% Bottom Layer " + getLayerNumber(sensor) + " Hit Counts: "
                            + occupancyMap.get(sensor.getName())[638]);
                    bottomActiveEdgeStripOccupancy[getLayerNumber(sensor) - 1] += occupancyMap.get(sensor.getName())[638];
                }
            }
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
