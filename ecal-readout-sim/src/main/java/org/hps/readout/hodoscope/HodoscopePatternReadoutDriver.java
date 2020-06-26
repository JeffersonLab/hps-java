package org.hps.readout.hodoscope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import java.util.Map;

import java.awt.Point;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.hps.readout.util.HodoscopePattern;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;

/**
 * Class <code>HodoscopePatternReadoutDriver</code> produces hodoscope pattern
 * objects for Ecal-hodo matching in the trigger simulation. Persistency of Hodo
 * FADC hits is <code>persistentTime</code>. On the other hand, hodo FADC hits
 * is earlier to enter the trigger system than Ecal by
 * <code>timeEarlierThanEcal</code> Therefore, for each clock-cycle, FADC hits
 * in [localTime - (persistentTime - timeEarlierThanEcal), localTime +
 * timeEarlierThanEcal + 4] are taken into account to generate hodoscope
 * patterns for all layers.
 * 
 * @author tongtongcao <caot@jlab.org>
 *
 */
public class HodoscopePatternReadoutDriver extends ReadoutDriver {

    /** Stores all channel for the hodoscope. */
    private Map<Long, HodoscopeChannel> channelMap = new HashMap<Long, HodoscopeChannel>();

    /**
     * The name of the collection that contains the hodo FADC hits, which raw energy
     * is self-defined. Through the hodo FADC hits, hodoscope pattern is generated.
     */
    private String inputCollectionName = "HodoscopeCorrectedHits";
    /**
     * The name of the collection into which generated hodoscope patterns for all
     * four layers should be output.
     */
    private String outputCollectionName = "HodoscopePatterns";

    /**
     * The local time for the driver.
     */
    private double localTime = 0.0;

    /**
     * Hodoscope FADC hit cut
     */
    private double fADCHitThreshold = 1.0;

    /**
     * Hodoscope tilt/cluster hit cut
     */
    private double hodoHitThreshold = 200.0;

    /**
     * Gain factor for raw energy (self-defined unit) of FADC hits
     */
    private double gainFactor = 1.25 / 2;

    /**
     * Persistent time for hodoscope FADC hit in unit of ns
     */
    private double persistentTime = 60.0;

    /**
     * Time for hodoscope FADC hits earlier to enter the trigger system than Ecal
     * with unit of ns
     */
    private double timeEarlierThanEcal = 20.0;

    /**
     * The length of time by which objects produced by this driver are shifted due
     * to the need to buffer data from later events. This is calculated
     * automatically. Hodo FADC hits enter the trigger system earlier than Ecal hits
     * by <code>timeEarlierThanEcal</code>
     */
    private double localTimeDisplacement = 0.0;

    /**
     * According to setup in database, index for hodoscope layers are expressed as
     * (layer+1)*y
     */
    public static final int TopLayer1 = 1;
    public static final int TopLayer2 = 2;
    public static final int BotLayer1 = -1;
    public static final int BotLayer2 = -2;

    /**
     * List for 4 layers;
     */
    private List<Integer> layerList = new ArrayList<>(4);

    /**
     * List for 8 (x, hole) points of each layer
     */
    private List<Point> xHolePointList = new ArrayList<>(8);

    @Override
    public void process(EventHeader event) {
        // Check the data management driver to determine whether the
        // input collection is available or not.
        if (!ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime + localTimeDisplacement)) {
            return;
        }

        // Hodoscope FADC hits enter the trigger system earlier than Ecal by the time
        // <code>timeEarlierThanEcal</code> .
        // On the other hand, hodoscope FADC hits persist with a range of
        // <code>persistentTime</code>.
        // To build current hodo patterns, FADC hits between localTime - (persistentTime
        // - timeEarlierThanEcal) and localTime + timeEarlierThanEcal + 4 are used.
        Collection<CalorimeterHit> fadcHits = ReadoutDataManager.getData(
                localTime - (persistentTime - timeEarlierThanEcal), localTime + timeEarlierThanEcal + 4.0,
                inputCollectionName, CalorimeterHit.class);

        // Increment the local time.
        localTime += 4.0;

        // All hits over <code>fadcHitThreshold</code> are saved for each hole of each
        // layer
        Map<Integer, Map<Point, List<Double>>> energyListMapForLayerMap = new HashMap<Integer, Map<Point, List<Double>>>();

        for (int layer : layerList) {
            Map<Point, List<Double>> energyListMap = new HashMap<Point, List<Double>>();
            for (Point point : xHolePointList) {
                energyListMap.put(point, new ArrayList<Double>());
            }
            energyListMapForLayerMap.put(layer, energyListMap);
        }

        for (CalorimeterHit hit : fadcHits) {
            double energy = hit.getRawEnergy();
            if (energy > fADCHitThreshold) {
                Long cellID = hit.getCellID();
                int layer = channelMap.get(cellID).getLayer();
                int y = channelMap.get(cellID).getIY();
                int x = channelMap.get(cellID).getIX();
                int hole = channelMap.get(cellID).getHole();

                Point point = new Point(x, hole);
                energyListMapForLayerMap.get((layer + 1) * y).get(point).add(energy * gainFactor);
            }
        }

        //Get maximum of energy in lists for each hole of each layer
        Map<Integer, Map<Point, Double>> maxEnergyMapForLayerMap = new HashMap<Integer, Map<Point, Double>>();
        for (int layer : layerList) {
            Map<Point, Double> maxEnergyMap = new HashMap<>();
            for (Point point : xHolePointList) {
                if(energyListMapForLayerMap.get(layer).get(point).size() != 0)
                    maxEnergyMap.put(point, Collections.max(energyListMapForLayerMap.get(layer).get(point)));
                else
                    maxEnergyMap.put(point, 0.);
                    
            }
            maxEnergyMapForLayerMap.put(layer, maxEnergyMap);
        }

        //Hodoscope patterns for all layers   
        //Order of list: TopLayer1, TopLayer2, BotLayer1, BotLayer2
        List<HodoscopePattern> hodoPatterns = new ArrayList<>(4);
        
        // Flag to determine if a pattern list at the current clock-cycle is added into data manager
        boolean flag = false;

        for (int i = 0; i < 4; i++) {
            HodoscopePattern pattern = new HodoscopePattern();

            Map<Point, Double> maxEnergyMap = maxEnergyMapForLayerMap.get(layerList.get(i));

            if (maxEnergyMap.get(xHolePointList.get(0)) > hodoHitThreshold) {
                pattern.setHitStatus(HodoscopePattern.HODO_LX_1, true);
                flag = true;
            }
            if (maxEnergyMap.get(xHolePointList.get(1)) + maxEnergyMap.get(xHolePointList.get(2)) > hodoHitThreshold) {
                pattern.setHitStatus(HodoscopePattern.HODO_LX_2, true);
                flag = true;
            }
            if (maxEnergyMap.get(xHolePointList.get(3)) + maxEnergyMap.get(xHolePointList.get(4)) > hodoHitThreshold) {
                pattern.setHitStatus(HodoscopePattern.HODO_LX_3, true);
                flag = true;
            }
            if (maxEnergyMap.get(xHolePointList.get(5)) + maxEnergyMap.get(xHolePointList.get(6)) > hodoHitThreshold) {
                pattern.setHitStatus(HodoscopePattern.HODO_LX_4, true);
                flag = true;
            }
            if (maxEnergyMap.get(xHolePointList.get(7)) > hodoHitThreshold) {
                pattern.setHitStatus(HodoscopePattern.HODO_LX_5, true);
                flag = true;
            }
            if (maxEnergyMap.get(xHolePointList.get(0)) + maxEnergyMap.get(xHolePointList.get(1))
                    + maxEnergyMap.get(xHolePointList.get(2)) > hodoHitThreshold) {
                pattern.setHitStatus(HodoscopePattern.HODO_LX_CL_12, true);
                flag = true;
            }
            if (maxEnergyMap.get(xHolePointList.get(1)) + maxEnergyMap.get(xHolePointList.get(2)) + maxEnergyMap.get(xHolePointList.get(3))
                    + maxEnergyMap.get(xHolePointList.get(4)) > hodoHitThreshold) {
                pattern.setHitStatus(HodoscopePattern.HODO_LX_CL_23, true);
                flag = true;
            }
            if (maxEnergyMap.get(xHolePointList.get(3)) + maxEnergyMap.get(xHolePointList.get(4)) + maxEnergyMap.get(xHolePointList.get(5))
                    + maxEnergyMap.get(xHolePointList.get(6)) > hodoHitThreshold) {
                pattern.setHitStatus(HodoscopePattern.HODO_LX_CL_34, true);
                flag = true;
            }
            if (maxEnergyMap.get(xHolePointList.get(5)) + maxEnergyMap.get(xHolePointList.get(6))
                    + maxEnergyMap.get(xHolePointList.get(7)) > hodoHitThreshold) {
                pattern.setHitStatus(HodoscopePattern.HODO_LX_CL_45, true);
                flag = true;
            }

            hodoPatterns.add(pattern);
        }
        
        // At leaset there is a hodo tilt/cluster hit in any layer, then the pattern list is added into data manager
        if(flag == true) ReadoutDataManager.addData(outputCollectionName, hodoPatterns, HodoscopePattern.class);
    }

    @Override
    public void startOfData() {
        // Define the output LCSim collection parameters.
        LCIOCollectionFactory.setCollectionName(outputCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollection<HodoscopePattern> patternCollectionParams = LCIOCollectionFactory
                .produceLCIOCollection(HodoscopePattern.class);

        // Instantiate the GTP cluster collection with the readout
        // data manager.
        localTimeDisplacement = timeEarlierThanEcal + 4.0;
        addDependency(inputCollectionName);
        ReadoutDataManager.registerCollection(patternCollectionParams, false);

        initLists();
    }

    /**
     * Initiate (layer, y) list and (x, hole) list 
     */
    private void initLists() {
        // Add elements for layer list
        layerList.add(TopLayer1);
        layerList.add(TopLayer2);
        layerList.add(BotLayer1);
        layerList.add(BotLayer2);

        // Add elements for (x, hole) point list
        xHolePointList.add(new Point(0, 0));
        xHolePointList.add(new Point(1, -1));
        xHolePointList.add(new Point(1, 1));
        xHolePointList.add(new Point(2, -1));
        xHolePointList.add(new Point(2, 1));
        xHolePointList.add(new Point(3, -1));
        xHolePointList.add(new Point(3, 1));
        xHolePointList.add(new Point(4, 0));
    }

    @Override
    public void detectorChanged(Detector detector) {
        // Populate the channel ID collections.
        populateChannelCollections();
    }

    /**
     * Populates the channel ID set and maps all existing channels to their
     * respective conditions.
     */
    private void populateChannelCollections() {
        // Load the conditions database and get the hodoscope channel
        // collection data.
        final DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        final HodoscopeChannelCollection channels = conditions
                .getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();

        // Store the set of all channel IDs.
        for (HodoscopeChannel channel : channels) {
            channelMap.put(Long.valueOf(channel.getChannelId().intValue()), channel);
        }
    }

    @Override
    protected double getTimeDisplacement() {
        return localTimeDisplacement;
    }

    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }

    /**
     * Sets the name of the input collection containing the objects of type
     * {@link org.lcsim.event.CalorimeterHit CalorimeterHit} that are output by the
     * digitization driver.
     * 
     * @param collection - The name of the input hit collection.
     */
    public void setInputCollectionName(String collection) {
        inputCollectionName = collection;
    }

    /**
     * Sets the name of the output collection containing the objects of type
     * {@link org.hps.readout.hodoscope.HodoscopePattern HodoscopePattern} that are
     * output by this driver.
     * 
     * @param collection - The name of the output hodoscope pattern collection.
     */
    public void setOutputCollectionName(String collection) {
        outputCollectionName = collection;
    }

    /**
     * Sets hodoscope FADC hit threshold
     * 
     * @param FADC hit threshold
     */
    public void setFADCHitThreshold(double fADCHitThreshold) {
        this.fADCHitThreshold = fADCHitThreshold;
    }

    /**
     * Sets hodoscope tilt/cluster hit threshold
     * 
     * @param hodoscope tilt/cluster hit threshold
     */
    public void setHodoHitThreshold(double hodoHitThreshold) {
        this.hodoHitThreshold = hodoHitThreshold;
    }

    /**
     * Set persistency for hodoscope FADC hit in unit of ns
     * 
     * @param persistency for hodoscope FADC hit in unit of ns
     */
    public void setPersistentTime(double persistentTime) {
        this.persistentTime = persistentTime;
    }

    /**
     * Set time for hodoscope FADC hits earlier to enter the trigger system than
     * Ecal with unit of ns
     * 
     * @param time for hodoscope FADC hits earlier to enter the trigger system than
     * Ecal with unit of ns
     */
    public void setTimeEarlierThanEcal(double timeEarlierThanEcal) {
        this.timeEarlierThanEcal = timeEarlierThanEcal;
    }

    /**
     * Set gain factor for raw energy (self-defined unit) of FADC hits
     * 
     * @param gain factor for raw energy (self-defined unit) of FADC hits
     */
    public void setGainFactor(double gainFactor) {
        this.gainFactor = gainFactor;
    }
}
