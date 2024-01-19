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
 *
 * mg...this only works for L1 and L6 smearing at the moment
 * ...unfortunately some of these things are hard coded
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
    //  instead of just removing hit at a given channel, remove all hits within Nsigma
    private boolean removeHitsWithinNSig = false;
    //  used sigma-weighted ratios
    private boolean useSigmaWeightedRatios = false;
    double nSig = 5;
    double sigmaUL1 = 0.1; //100 microns...this is roughly the 5-hit track projection error in U for layer 1, plotted in SVTHitLevelPlots
    double sigmaUL6 = 0.25; //250 microns...this is roughly the 5-hit track projection error in U for layer 6*1.5 because pull is too wide, plotted in SVTHitLevelPlots
    //    double sigmaUL6 = 0.05; //50 microns...this is narrow just to check the effect. 
    //    double firstSensorKillFactor=3.0;
    /// double secondSensorKillFactor=2.0;
    double firstSensorKillFactor=1.0;
    double secondSensorKillFactor=1.0;
    //these are just used for debugging...
    int checkHitsChannel = 4;
    int checkHitsTotal = 0;
    int checkHitsPassed = 0;
    double checkHitsRatio = 0;

    double maxLayer=12;
    ///

    private List<TrackerHit> siClusters=new ArrayList<TrackerHit>();

    private Map<TrackerHit, Boolean> _siClustersAcceptMap = new HashMap<TrackerHit, Boolean>();
    private    Map<TrackerHit, Boolean> _finalSiClustersAcceptMap = new HashMap<TrackerHit, Boolean>();

    private Map<Integer, Double> _smearingFractionsL1 = new HashMap<Integer, Double>();
    private Map<Integer, Double> _smearingFractionsL6 = new HashMap<Integer, Double>();

    public void setRatioFiles(String[] ratioNames) {
        System.out.println("Setting ratio files!!!  " + ratioNames[0]);
        this.ratioFiles = new HashSet<String>(Arrays.asList(ratioNames));
    }

    public void setDebug(boolean debug) {
        this._debug = debug;
    }

    public void setRemoveHitsWithinNSig(boolean removeHitsWithinNSig) {
        this.removeHitsWithinNSig = removeHitsWithinNSig;
    }

    public void setUseSigmaWeightedRatios(boolean useSigmaWeightedRatios) {
        this.useSigmaWeightedRatios = useSigmaWeightedRatios;
    }

    public void setL1Sigma(double l1sigma) {
        this.sigmaUL1 = l1sigma;
    }

    public void setL6Sigma(double l6sigma) {
        this.sigmaUL1 = l6sigma;
    }

    public void setNSigma(double nSig) {
        this.nSig = nSig;
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
        System.out.println("filling smearing fractions");
        _smearingFractionsL1 = this.fillSmearedFractions(sigmaUL1, 1);
        _smearingFractionsL6 = this.fillSmearedFractions(sigmaUL6, 12);
    }

    @Override
    public void process(EventHeader event) {
        //    System.out.println("In process of SVTHitKiller");

        int nhitsremoved = 0;
        _siClustersAcceptMap.clear();
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
                 
                    double ratio = 0;
                    if (useSigmaWeightedRatios)
                        ratio = this.getSmearedRatio(chan, sensorToKill);
                    else
                        ratio = sensorToKill.getRatio(chan);
                    double killFactor=(1-ratio)*sensorToKill.getScaleKillFactor();
                    if (_debug)
                        System.out.println("Found a hit on a sensor to kill!!! "+sensor.getName()+"  Layer = " + sensor.getLayerNumber()
                                           + " isTop? " + sensorToKill.getIsTop() + " isStereo? " + sensorToKill.getIsStereo()
                                           + " isSlot? " + sensorToKill.getIsSlot() + " channel = " + chan+" ratio = "+ratio);
                    
                    ratio=1-killFactor;
                    if (ratio != -666) {
                        double random = Math.random(); //throw a random number to see if this hit should be rejected
                        if (random > ratio) {
                            passHit = false;
                            if (_debug)
                                System.out.println("Killing this hit Layer = " + sensor.getLayerNumber()
                                                   + " isTop? " + sensorToKill.getIsTop() + "; isStereo? " + sensorToKill.getIsStereo()
                                                   + " isSlot? " + sensorToKill.getIsSlot() +" scaleKillFactor= "+ sensorToKill.getScaleKillFactor()
                                                   + " channel = " + chan + "  kill ratio = " + ratio+"  random = "+random);
                            nhitsremoved++; 
                        }
                    }
                    if (chan == checkHitsChannel) {
                        checkHitsTotal++;
                        if (passHit)
                            checkHitsPassed++;
                        checkHitsRatio = ratio;
                    }

                }
            _siClustersAcceptMap.put(siCluster, passHit);
        }

//        if (_debug)
        List<TrackerHit> tmpClusterList = getFinalHits(_siClustersAcceptMap);
        //        if (_debug)
        if (_debug)System.out.println("New Cluster List Has " + tmpClusterList.size() + "; old List had " + oldClusterListSize);
        if (_debug)System.out.println("number of hits removed for this event = "+nhitsremoved);
        int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.        
        event.remove(this.stripHitInputCollectionName);
        event.put(this.stripHitInputCollectionName, tmpClusterList, SiTrackerHitStrip1D.class, 0, toString());

    }

    @Override
    public void endOfData() {
        System.out.println("StripHitKiller::endOfData  channel = " + checkHitsChannel
                + "  ratio = " + checkHitsRatio + "; hits pass = " + checkHitsPassed
                + "  hits total = " + checkHitsTotal + "  passRatio = " + ((double) checkHitsPassed) / checkHitsTotal);
    }

    public void registerSensor(int layer, boolean isTop, boolean isStereo, boolean isSlot, String ratioFile) {
        if (_debug){
            System.out.println("================  Registering New SensorToKill  =============== ");
            System.out.println("  "
                               + "layer = " + layer + "; top = " + isTop + "; stereo = " + isStereo
                               + "; slot = " + isSlot+";  ratio file = "+ratioFile);
        }
        SensorToKill newSensor = new SensorToKill(layer, isTop, isStereo, isSlot, ratioFile);
        System.out.println("newSensor isTop " + newSensor.getIsTop());
        if(newSensor.getIsTop() && !(newSensor.getIsStereo()) && newSensor.getLayer()==1)
            newSensor.setScaleKillFactor(firstSensorKillFactor) ;      
        if(newSensor.getIsTop() && newSensor.getIsStereo() && newSensor.getLayer()==1)
            newSensor.setScaleKillFactor(secondSensorKillFactor);
       
        if(!(newSensor.getIsTop()) && newSensor.getIsStereo() && newSensor.getLayer()==1)
            newSensor.setScaleKillFactor(firstSensorKillFactor);
        if(!(newSensor.getIsTop()) && !(newSensor.getIsStereo()) && newSensor.getLayer()==1)
           newSensor.setScaleKillFactor(secondSensorKillFactor);
        if (_debug){
            System.out.println("      channel 10 ratio for newSensor = "+newSensor.getRatio(10));
            System.out.println("================  Done With New SensorToKill  =============== ");
        }
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
            if (layer < 4 && layer > 0){//only for pre-2019 runs...fix this...
                 if (_debug)System.out.println("Matching with sensor = "+sensor.getName());
                return sensor;
            }
            else {
                if ((!sensor.getSide().matches("ELECTRON") && isHole) || (sensor.getSide().matches("ELECTRON") && !isHole))
                    continue;
                
                 if (_debug)System.out.println("Matching with sensor = "+sensor.getName());
                return sensor;
            }
        }
        return null;
    }

    private List<TrackerHit> getFinalHits(Map<TrackerHit, Boolean> _initialSiClustersAcceptMap) {
        _finalSiClustersAcceptMap.clear();
        List<TrackerHit> tmpClusterList = new ArrayList<TrackerHit>();
        for (TrackerHit hit : _initialSiClustersAcceptMap.keySet()) {
            boolean isAccept = _initialSiClustersAcceptMap.get(hit);
            if (removeHitsWithinNSig && !isAccept) { // this hit got thrown out; lets throw out all hits within N-sigma
                if (_debug)
                    System.out.println("Found a killed hit...now check for hits around it");
                HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                Hep3Vector pos = globalToSensor(toHep3(hit.getPosition()), sensor);
                int chan = getChan(pos, sensor);
                double sigmaU = sigmaUL1;
                if (sensor.getLayerNumber() > 10)  //remember, layer number is sensor number (1-12)
                    sigmaU = sigmaUL6;
                if (_debug)
                    System.out.println("Layer number = " + sensor.getLayerNumber() + " sigma = " + sigmaU);
                Hep3Vector minPos = new BasicHep3Vector(pos.x() - nSig * sigmaU, pos.y(), pos.z());
                Hep3Vector maxPos = new BasicHep3Vector(pos.x() + nSig * sigmaU, pos.y(), pos.z());
                int minChan = getChan(minPos, sensor);
                int maxChan = getChan(maxPos, sensor);
                if (minChan > maxChan) {
                    int tmp = minChan;
                    minChan = maxChan;
                    maxChan = tmp;
                }
                if (_debug)
                    System.out.println("hit channel = " + chan + "; minChan = " + minChan + "; maxChan = " + maxChan);
                //loop through initial clusters again
                for (TrackerHit testhit : _initialSiClustersAcceptMap.keySet()) {
                    HpsSiSensor testsensor = (HpsSiSensor) ((RawTrackerHit) testhit.getRawHits().get(0)).getDetectorElement();
                    if (sensor != testsensor)
                        continue;
                    Hep3Vector testpos = globalToSensor(toHep3(testhit.getPosition()), sensor);
                    int testchan = getChan(testpos, sensor);
                    if (_debug)
                        System.out.println("test channel = " + testchan);
                    if (testchan > maxChan || testchan < minChan)
                        continue;
                    //in range, set accept to false
                    if (_debug)
                        System.out.println("Found a hit within range of killed hit...killing it too!");
                    _finalSiClustersAcceptMap.put(testhit, false);
                }
                _finalSiClustersAcceptMap.put(hit, isAccept); //add the original cluster to map (shouldn't matter since isAccept==false
            } else if (!_finalSiClustersAcceptMap.containsKey(hit))
                _finalSiClustersAcceptMap.put(hit, isAccept);
        }
        //fill tmpClusterList with accepted hits
        for (TrackerHit hit : _finalSiClustersAcceptMap.keySet())
            if (_finalSiClustersAcceptMap.get(hit))
                tmpClusterList.add(hit);
        return tmpClusterList;
    }

    private double getSmearedRatio(int seedChan, SensorToKill sensorToKill) {
        double smearedRatio = 0;
        double integralSum = 0;
        Map<Integer, Double> _smearingFractions = new HashMap<Integer, Double>();
        if (sensorToKill.getLayer() == 1)
            _smearingFractions = _smearingFractionsL1;
        else if (sensorToKill.getLayer() == 6)
            _smearingFractions = _smearingFractionsL6;
        for (Map.Entry<Integer, Double> entry : _smearingFractions.entrySet()) {
            int chan = seedChan + entry.getKey();
            if (chan < 0 || chan > sensorToKill._sensor.getNumberOfChannels() - 1)
                continue;
            double chanIntegral = entry.getValue();
            double ratio = sensorToKill.getRatio(chan);
            if (ratio > 1.0 || ratio == -666)
                ratio = 1.0;
//            System.out.println("channel = " + chan + "  ratio = "
            //                   + ratio + "; smearedRatio = " + ratio * chanIntegral);
            smearedRatio += ratio * chanIntegral;
            integralSum += chanIntegral;
        }
        if (integralSum == 0)
            smearedRatio = 1.0;
        else
            smearedRatio /= integralSum;
        if (_debug)
            System.out.println("getSmearedRatio: seed ratio = "
                    + sensorToKill.getRatio(seedChan) + "; smearedRatio = " + smearedRatio);
        return smearedRatio;
    }

    private Map<Integer, Double> fillSmearedFractions(double sigmaU, int layer) {
        Map<Integer, Double> _smearingFractions = new HashMap<Integer, Double>();
        double pitch = 0;
        for (HpsSiSensor s : sensors)
            if (s.getLayerNumber() == layer)
                pitch = s.getReadoutStripPitch();
        if (pitch == 0) {
            System.out.println("Couldn't find layer = " + layer);
            return _smearingFractions;
        }

        double normPitch = sigmaU / pitch;
        int nStrips = (int) Math.floor(this.nSig * normPitch);
        double totalInt = 0;
        for (int i = -nStrips; i < 1; i++) {//just do negative bins (and 0)..positive side is same
            double minIntLimit = (i - 0.5) / normPitch;
            double maxIntLimit = (i + 0.5) / normPitch;
            double minInt = this.computeGaussInt(minIntLimit, 1000);
            double maxInt = this.computeGaussInt(maxIntLimit, 1000);
            totalInt += maxInt - minInt;
            _smearingFractions.put(i, maxInt - minInt);
            System.out.println("_smearingFractions chan = " + i + " fraction = " + _smearingFractions.get(i));

        }
        for (int i = 1; i < nStrips + 1; i++) {//positive side is same
            totalInt += _smearingFractions.get(-1 * i);
            _smearingFractions.put(i, _smearingFractions.get(-1 * i));
            System.out.println("_smearingFractions chan = " + i + " fraction = " + _smearingFractions.get(i));

        }
        System.out.println("Layer = " + layer + "; totalInt = " + totalInt);
        return _smearingFractions;
    }

    public class SensorToKill {

        int _layer = 1;
        boolean _isStereo = false;
        boolean _isSlot = false;
        boolean _isTop = false;
        String _ratioFile = "foobarTopL1Stereo.txt";
        HpsSiSensor _sensor = null;
        double _scaleKillFactor=1.0;
        Map<Integer, Double> _channelToRatioMap = new HashMap<Integer, Double>();

        public SensorToKill(int layer, boolean isTop, boolean isStereo, boolean isSlot, String ratioFile) {
             if (_debug)System.out.println("Making new SensorToKill layer = " + layer);
            _layer = layer;
            _isTop = isTop;
            _isStereo = isStereo;
            _isSlot = isSlot;
            _ratioFile = ratioFile;
            _sensor = getSensor(layer, isTop, !isStereo, !isSlot);
            readRatioFile();
        }

        void setScaleKillFactor(double scale){
            this._scaleKillFactor=scale;
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

        double getScaleKillFactor(){
            return(this._scaleKillFactor);
        }

        boolean matchSensor(HpsSiSensor sensor) {
            return _sensor == sensor;
        }

        private void readRatioFile() {
            String infile = "/org/hps/recon/tracking/efficiencyCorrections/" + _ratioFile;
            InputStream inRatios = this.getClass().getResourceAsStream(infile);
             if (_debug)System.out.println("StripHitKiller::Reading ratio file "+infile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inRatios));
            String line;
            String delims = "[ ]+";// this will split strings between one or more spaces
            try {
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(delims);
                     if (_debug) System.out.println("channel number = " + tokens[0] + "; ratio = " + tokens[1]);
                    _channelToRatioMap.put(Integer.parseInt(tokens[0]), Double.parseDouble(tokens[1]));
                }
            } catch (IOException ex) {
                Logger.getLogger(StripHitKiller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public double getRatio(int channel) {
             if (_debug)System.out.println("SensorToKill::  getRatio:  layer = "+this.getLayer()+"; channel  "+channel); 
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
    /*
    private int getChan(Hep3Vector pos, HpsSiSensor sensor) {
        double readoutPitch = sensor.getReadoutStripPitch();
        int nChan = sensor.getNumberOfChannels();
        double height = readoutPitch * nChan;
        return (int) ((height / 2 - pos.x()) / readoutPitch);
    }
    */
    
    private int getChan(Hep3Vector pos, HpsSiSensor sensor) {   
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if(maxLayer>12 && sensor.getLayerNumber()<5){//thin sensors
            int row=electrodes.getRowNumber(pos);
            int col=electrodes.getColumnNumber(pos);
            return electrodes.getCellID(row,col);
        }else{
            return electrodes.getCellID(pos);            
        }
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

    //Computes gaussian integral numerically from -inf to nSig
    // this should be replaced by a simple error function but I can't find one 
    // in normal java libraries!!!
    private double computeGaussInt(double nSig, int nSteps) {
        double mean = 0;
        double sigma = 1;
        double dx = sigma * nSig / (double) nSteps;
        double integral = 0;
        for (int i = 0; i < nSteps; i++) {
            double x = dx * (i + 0.5) + mean;
            integral += dx * Gauss(x, mean, sigma);
        }
        return integral + 0.5;
    }

    //Gaussian function
    private double Gauss(double x, double mean, double sigma) {
        return 1 / (Math.sqrt(2 * Math.PI * Math.pow(sigma, 2))) * Math.exp(-Math.pow(x - mean, 2) / (2 * Math.pow(sigma, 2)));
    }

}
