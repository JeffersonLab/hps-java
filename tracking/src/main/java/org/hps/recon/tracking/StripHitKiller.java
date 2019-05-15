package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;

/**
 *
 * @author mgraham created 5/14/19
 * This driver will remove 1d strip clusters from the
 * "StripClusterer_SiTrackerHitStrip1D" (default)
 * collection based on a channel-based (data/mc) ratio file.
 *
 * Careful..the names of the ratio files are important! Format
 * should be:
 * <prefix>_LX_top/bottom_stereo/axial_slot/hole.txt
 */
public class StripHitKiller extends Driver {

    //IMPORTANT...the layer, top/bottom/stereo/axial/slot/hole are derived from these names!!!
    Set<String> ratioFiles = new HashSet<String>();
    private List<SensorToKill> _sensorsToKill = new ArrayList<SensorToKill>();
    //List of Sensors
    private List<HpsSiSensor> sensors = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    private static Pattern layerPattern = Pattern.compile("L(\\d+)(t|b)");
    private String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private boolean _debug = false;

    public void setRatioFiles(String[] ratioNames) {
        System.out.println("Setting ratio files!!!  " + ratioNames[0]);
        this.ratioFiles = new HashSet<String>(Arrays.asList(ratioNames));
    }

    public StripHitKiller() {
    }

    @Override
    public void detectorChanged(Detector detector) {
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                .getDetectorElement().findDescendants(HpsSiSensor.class);
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0)
            throw new RuntimeException("No sensors were found in this detector.");

        //parse the ratio names and register sensors to kill
        String delims = "_|\\.";// this will split strings between  one "_" or "."
        for (String ratioFile : ratioFiles) {
            System.out.println("StripHitKiller::Using this ratioFile:  " + ratioFile);
            int layer = -1;
            boolean top = false;
            boolean stereo = false;
            boolean slot = false;
            System.out.println("Parsing ratioFile = " + ratioFile);
            String[] tokens = ratioFile.split(delims);
            Matcher m = layerPattern.matcher(tokens[1]);
            if (m.find()) {
                layer = Integer.parseInt(m.group(1));
                if (m.group(2).matches("t"))
                    top = true;
            } else {
                System.out.println("Couldn't find layer number!!!  " + ratioFile);
                continue;
            }
//            if (tokens[2].matches("top"))
//                top = true;
//            System.out.println(tokens[2]+" "+tokens[3]+" "+tokens[4]);
            if (tokens[2].matches("stereo"))
                stereo = true;
            if (tokens[3].matches("slot"))
                slot = true;
            System.out.println("StripHitKiller::Killing this:  "
                    + "layer = " + layer + "; top = " + top + "; stereo = " + stereo
                    + "; slot = " + slot);
            this.registerSensor(layer, top, stereo, slot, ratioFile);
        }

    }

    @Override
    public void process(EventHeader event) {
        //    System.out.println("In process of SVTHitKiller");
        List<TrackerHit> siClusters;
        List<TrackerHit> tmpClusterList = new ArrayList<TrackerHit>();
        if (event.hasItem(stripHitInputCollectionName))
            siClusters = (List<TrackerHit>) event.get(stripHitInputCollectionName);
        else {
            System.out.println("StripHitKiller::process No Input Collection Found?? " + stripHitInputCollectionName);
            return;
        }
        if (_debug)
            System.out.println("Number of SiClusters Found = " + siClusters.size());
        int oldClusterListSize = siClusters.size();
        for (TrackerHit siCluster : siClusters) {
            boolean passHit = true;
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) siCluster.getRawHits().get(0)).getDetectorElement();
            for (SensorToKill sensorToKill : _sensorsToKill)
                if (sensorToKill.matchSensor(sensor)) {
                    //ok, get hit channel and kill or not
                    Hep3Vector pos = globalToSensor(toHep3(siCluster.getPosition()), sensor);
                    int chan = getChan(pos, sensor);
                    if (_debug)
                        System.out.println("Found a hit on a sensor to kill!!!  Layer = " + sensor.getLayerNumber() + " channel = " + chan);
                    double ratio = sensorToKill.getRatio(chan);
                    if (ratio != -666) {
                        double random = Math.random(); //throw a random number to see if this hit should be rejected
                        if (random > ratio) {
                            passHit = false;
                            if (_debug)
                                System.out.println("Killing this hit layer=" + sensor.getLayerNumber()
                                        + " channel = " + chan + "  ratio = " + ratio);
                        }
                    }

                }
            if (passHit)
                tmpClusterList.add(siCluster);
        }

        if (_debug)
            System.out.println("New Cluster List Has " + tmpClusterList.size() + "; old List had " + oldClusterListSize);
        int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.        
        event.put(this.stripHitInputCollectionName, tmpClusterList, SiTrackerHitStrip1D.class, 0, toString());

    }

    public void registerSensor(int layer, boolean isTop, boolean isStereo, boolean isSlot, String ratioFile) {
        SensorToKill newSensor = new SensorToKill(layer, isTop, isStereo, isSlot, ratioFile);
        System.out.println("newSensor isTop " + newSensor.getIsTop());
        _sensorsToKill.add(newSensor);
    }

    //Return the HpsSiSensor for a given top/bottom track, layer, axial/stereo, and slot/hole
    private HpsSiSensor getSensor(int layer, boolean isTop, boolean isAxial, boolean isHole) {
        for (HpsSiSensor sensor : sensors) {
            int senselayer = (sensor.getLayerNumber() + 1) / 2;
            if (senselayer != layer)
                continue;
            if ((isTop && !sensor.isTopLayer()) || (!isTop && sensor.isTopLayer()))
                continue;
            if ((isAxial && !sensor.isAxial()) || (!isAxial && sensor.isAxial()))
                continue;
            if (layer < 4 && layer > 0)
                return sensor;
            else {
                if ((!sensor.getSide().matches("ELECTRON") && isHole) || (sensor.getSide().matches("ELECTRON") && !isHole))
                    continue;
                return sensor;
            }
        }
        return null;
    }

    public class SensorToKill {

        int _layer = 1;
        boolean _isStereo = false;
        boolean _isSlot = false;
        boolean _isTop = false;
        String _ratioFile = "foobarTopL1Stereo.txt";
        HpsSiSensor _sensor = null;
        Map<Integer, Double> _channelToRatioMap = new HashMap<Integer, Double>();

        public SensorToKill(int layer, boolean isTop, boolean isStereo, boolean isSlot, String ratioFile) {
            System.out.println("Making new SensorToKill layer = " + layer);
            _layer = layer;
            _isTop = isTop;
            _isStereo = isStereo;
            _isSlot = isSlot;
            _ratioFile = ratioFile;
            _sensor = getSensor(layer, isTop, !isStereo, !isSlot);
            readRatioFile();
        }

        int getLayer() {
            return _layer;
        }

        boolean getIsStereo() {
            return _isStereo;
        }

        boolean getIsSlot() {
            return _isSlot;
        }

        boolean getIsTop() {
            return _isTop;
        }

        String getRatioFile() {
            return _ratioFile;
        }

        boolean matchSensor(HpsSiSensor sensor) {
            return _sensor == sensor;
        }

        private void readRatioFile() {
            String infile = "/org/hps/recon/tracking/efficiencyCorrections/" + _ratioFile;
            InputStream inRatios = this.getClass().getResourceAsStream(infile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inRatios));
            String line;
            String delims = "[ ]+";// this will split strings between one or more spaces
            try {
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(delims);
                    System.out.println("channel number = " + tokens[0] + "; ratio = " + tokens[1]);
                    _channelToRatioMap.put(Integer.parseInt(tokens[0]), Double.parseDouble(tokens[1]));
                }
            } catch (IOException ex) {
                Logger.getLogger(StripHitKiller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public double getRatio(int channel) {
            if (_channelToRatioMap.get(channel) == null)
                return -666;
            return _channelToRatioMap.get(channel);
        }

    }

    //Converts double array into Hep3Vector
    private Hep3Vector toHep3(double[] arr) {
        return new BasicHep3Vector(arr[0], arr[1], arr[2]);
    }
    //Returns channel number of a given position in the sensor frame

    private int getChan(Hep3Vector pos, HpsSiSensor sensor) {
        double readoutPitch = sensor.getReadoutStripPitch();
        int nChan = sensor.getNumberOfChannels();
        double height = readoutPitch * nChan;
        return (int) ((height / 2 - pos.x()) / readoutPitch);
    }

    //Converts position into sensor frame
    private Hep3Vector globalToSensor(Hep3Vector trkpos, HpsSiSensor sensor) {
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if (electrodes == null) {
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.ELECTRON);
            System.out.println("Charge Carrier is NULL");
        }
        return electrodes.getGlobalToLocal().transformed(trkpos);
    }

}
