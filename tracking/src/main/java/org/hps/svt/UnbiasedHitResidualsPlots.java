/*
 * Unbiased Hit Residuals are computed
 */
/**
 * @author mrsolt
 *
 */
package org.hps.svt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.apache.commons.math3.util.Pair;
import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;

public class UnbiasedHitResidualsPlots extends Driver {

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree; 
    IHistogramFactory histogramFactory; 

    //List of Sensors
    private List<HpsSiSensor> sensors = null;
    
    //List of Histograms
    IHistogram1D momentum;
    
    Map<String, IHistogram1D> residualY = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram1D> residualYSingleHit = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram1D> residualYMultipleHit = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram2D> residualYvsV = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYvsU = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYSingleHitvsU = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYMultipleHitvsU = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYSingleHitvsV = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYMultipleHitvsV = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram1D> residualYEle = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram2D> residualYvsVEle = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYvsUEle = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram1D> residualYPos = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram2D> residualYvsVPos = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYvsUPos = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYvsP = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYvsPEle = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYvsPPos = new HashMap<String,IHistogram2D>();
    
    //Histogram Settings
    int nBins = 100;
    double maxYRes = 0.5;
    double minYRes = -maxYRes;

    
    //Collection Strings
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String GBLTrackCollectionName = "GBLTracks";
    
    //Bfield
    protected static double bfield;
    FieldMap bFieldMap = null;
   
    private static final String SUBDETECTOR_NAME = "Tracker";

    boolean cleanFEE = false;
    double nSig = 5;
    int chanExtd = 0;
    
    //Daq map
    SvtChannelCollection channelMap;
    SvtDaqMappingCollection daqMap;
    
    public void setCleanFEE(boolean cleanFEE) { 
        this.cleanFEE = cleanFEE;
    }
    
    public void setSig(double nSig) { 
        this.nSig = nSig;
    }
    
    public void setChanExtd(int chanExtd) { 
        this.chanExtd = chanExtd;
    }
    
    //Beam Energy
    double ebeam;
    
    public void detectorChanged(Detector detector){
    
        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
        //Grab channel map and daq map from conditions database
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        SvtConditions svtConditions = mgr.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
        
        channelMap = svtConditions.getChannelMap();
        daqMap = svtConditions.getDaqMap();
    
        //Set Beam Energy
        BeamEnergyCollection beamEnergyCollection = 
                this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        ebeam = beamEnergyCollection.get(0).getBeamEnergy();
        
        bfield = TrackUtils.getBField(detector).magnitude();
        bFieldMap = detector.getFieldMap();
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
   
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }
        
        //Setup Plots
        momentum = histogramFactory.createHistogram1D("Momentum", nBins, 0, 1.5*ebeam);
        for(HpsSiSensor sensor:sensors){
            String sensorName = sensor.getName();
            int nChan = sensor.getNumberOfChannels();
            double readoutPitch = sensor.getReadoutStripPitch();
            double maxU = nChan * readoutPitch / 2;
            double width = getSensorLength(sensor);
            double maxV = width/2.;
            double minV = -maxV;

            residualY.put(sensorName,histogramFactory.createHistogram1D("Residual U " + sensorName, nBins, minYRes, maxYRes));
            residualYSingleHit.put(sensorName,histogramFactory.createHistogram1D("Residual U Single Hit " + sensorName, nBins, minYRes, maxYRes));
            residualYMultipleHit.put(sensorName,histogramFactory.createHistogram1D("Residual U Multiple Hit " + sensorName, nBins, minYRes, maxYRes));
            residualYSingleHitvsV.put(sensorName,histogramFactory.createHistogram2D("Residual U vs V Single Hit " + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            residualYSingleHitvsU.put(sensorName,histogramFactory.createHistogram2D("Residual U vs U Single Hit " + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            residualYMultipleHitvsV.put(sensorName,histogramFactory.createHistogram2D("Residual U vs V Multiple Hit " + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            residualYMultipleHitvsU.put(sensorName,histogramFactory.createHistogram2D("Residual U vs U Multiple Hit " + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            residualYvsV.put(sensorName,histogramFactory.createHistogram2D("Residual U vs V " + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            residualYvsU.put(sensorName,histogramFactory.createHistogram2D("Residual U vs U " + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            residualYEle.put(sensorName,histogramFactory.createHistogram1D("Residual U Electron " + sensorName, nBins, minYRes, maxYRes));
            residualYvsVEle.put(sensorName,histogramFactory.createHistogram2D("Residual U vs V Electron " + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            residualYvsUEle.put(sensorName,histogramFactory.createHistogram2D("Residual U vs U Electron " + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            residualYPos.put(sensorName,histogramFactory.createHistogram1D("Residual U Positron " + sensorName, nBins, minYRes, maxYRes));
            residualYvsVPos.put(sensorName,histogramFactory.createHistogram2D("Residual U vs V Positron " + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            residualYvsUPos.put(sensorName,histogramFactory.createHistogram2D("Residual U vs U Positron " + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            residualYvsP.put(sensorName,histogramFactory.createHistogram2D("Residual U vs P " + sensorName, nBins, 0, 1.3*ebeam, nBins, minYRes, maxYRes));
            residualYvsPEle.put(sensorName,histogramFactory.createHistogram2D("Residual U vs P Electron " + sensorName, nBins, 0, 1.3*ebeam, nBins, minYRes, maxYRes));
            residualYvsPPos.put(sensorName,histogramFactory.createHistogram2D("Residual U vs P Positron " + sensorName, nBins, 0, 1.3*ebeam, nBins, minYRes, maxYRes));
        }
    }

    public void process(EventHeader event){
        aida.tree().cd("/");
        
        //Grab all GBL tracks in the event
        List<Track> tracks = event.get(Track.class,GBLTrackCollectionName);
        
        //Grab all the clusters in the event
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, stripHitOutputCollectionName);
    
        for(Track track:tracks){
            //Grab the unused layer on the track
            int unusedLay = getUnusedSvtLayer(track.getTrackerHits());   
            if(unusedLay == -1) continue;
            
            TrackState tState = getTrackState(track,unusedLay);
            if(tState == null){
                continue;
            }
        
            Hep3Vector p = toHep3(tState.getMomentum());
            double q = track.getCharge();
            
            if(cleanFEE){
                // Require track to be an electron
                if(q < 0) continue;
                
                // Select around the FEE momentum peak
                if(p.magnitude() < 0.75*ebeam || p.magnitude() > 1.25*ebeam) continue;
            }
        
            //See if track is within acceptance of both the axial and stereo sensors of the unused layer
            Pair<HpsSiSensor,Pair<Integer,Hep3Vector>> axialSensorPair = isWithinSensorAcceptance(track,tState,unusedLay,true, p,bFieldMap);
            Pair<HpsSiSensor,Pair<Integer,Hep3Vector>> stereoSensorPair = isWithinSensorAcceptance(track,tState,unusedLay,false,p,bFieldMap);           
            
            //Skip track if it isn't within acceptance of both axial and stereo pairs of a given unused layer
            if(axialSensorPair == null || stereoSensorPair == null) continue;
            
            //Fill momentum histogram
            momentum.fill(p.magnitude());
           
            //Set axial and stereo sensors of the missing layer
            HpsSiSensor axialSensor = axialSensorPair.getFirst();
            HpsSiSensor stereoSensor = stereoSensorPair.getFirst();
            
            String sensorAxialName = axialSensor.getName();
            String sensorStereoName = stereoSensor.getName();
            
            //Grab the track extrapolations at each sensor
            Hep3Vector axialExtrapPosSensor = axialSensorPair.getSecond().getSecond();
            Hep3Vector stereoExtrapPosSensor = stereoSensorPair.getSecond().getSecond();
            
            double UresidualAxial = 9999;
            double UresidualStereo = 9999;
            
            boolean isSingleHitAxial = true;
            boolean isSingleHitStereo = true;
            
            //Loop over all reconstructed 1D hits on sensor of interest in the events
            for(SiTrackerHitStrip1D hit:stripHits){
                //Get the sensor and position of the hit
                HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                double[] hitPos = hit.getPosition();
                //Change to sensor coordinates
                Hep3Vector hitPosSensor = globalToSensor(toHep3(hitPos),sensor);
                //Check to see if the sensor of this hit is the same sensor you expect to see an axial hit
                if(sensorAxialName == sensor.getName()){
                    //Compute the residual between extrapolated track and hit position
                    //Keep the value of the smallest residual
                    double Uresidual = axialExtrapPosSensor.x() - hitPosSensor.x();
                    if(Math.abs(Uresidual) < Math.abs(UresidualAxial)){
                        UresidualAxial = Uresidual;
                        if(hit.getRawHits().size() > 1) isSingleHitAxial = false;
                    }
                }
              //Check to see if the sensor of this hit is the same sensor you expect to see a stereo hit
                if(sensorStereoName == sensor.getName()){
                    //Compute the residual between extrapolated track and hit position
                    //Keep the value of the smallest residual
                    double Uresidual = stereoExtrapPosSensor.x() - hitPosSensor.x();
                    if(Math.abs(Uresidual) < Math.abs(UresidualStereo)){
                        UresidualStereo = Uresidual;
                        if(hit.getRawHits().size() > 1) isSingleHitAxial = false;
                    }
                }
            }
        
            //Fill histograms for residuals
            residualY.get(sensorAxialName).fill(UresidualAxial);
            residualY.get(sensorStereoName).fill(UresidualStereo);
            residualYvsV.get(sensorAxialName).fill(axialExtrapPosSensor.y(),UresidualAxial);
            residualYvsV.get(sensorStereoName).fill(stereoExtrapPosSensor.y(),UresidualStereo);
            residualYvsU.get(sensorAxialName).fill(axialExtrapPosSensor.x(),UresidualAxial);
            residualYvsU.get(sensorStereoName).fill(stereoExtrapPosSensor.x(),UresidualStereo);
            residualYvsP.get(sensorAxialName).fill(p.magnitude(),UresidualAxial);
            residualYvsP.get(sensorStereoName).fill(p.magnitude(),UresidualAxial);
            
            
            
            //Fill single hit and multiple hit histograms
            if(isSingleHitAxial){
                residualYSingleHit.get(sensorAxialName).fill(UresidualAxial);
                residualYSingleHitvsV.get(sensorAxialName).fill(axialExtrapPosSensor.y(),UresidualAxial);
                residualYSingleHitvsU.get(sensorAxialName).fill(axialExtrapPosSensor.x(),UresidualAxial);
            }
            else{
                residualYMultipleHit.get(sensorAxialName).fill(UresidualAxial);
                residualYMultipleHitvsV.get(sensorAxialName).fill(axialExtrapPosSensor.y(),UresidualAxial);
                residualYMultipleHitvsU.get(sensorAxialName).fill(axialExtrapPosSensor.x(),UresidualAxial);
            }
            
            if(isSingleHitStereo){
                residualYSingleHit.get(sensorStereoName).fill(UresidualStereo);
                residualYSingleHitvsV.get(sensorStereoName).fill(stereoExtrapPosSensor.y(),UresidualStereo);
                residualYSingleHitvsU.get(sensorStereoName).fill(stereoExtrapPosSensor.x(),UresidualStereo);
            }
            else{
                residualYMultipleHit.get(sensorStereoName).fill(UresidualStereo);
                residualYMultipleHitvsV.get(sensorStereoName).fill(stereoExtrapPosSensor.y(),UresidualStereo);
                residualYMultipleHitvsU.get(sensorStereoName).fill(stereoExtrapPosSensor.x(),UresidualStereo);
            }
            
            if(q > 0){
                residualYEle.get(sensorAxialName).fill(UresidualAxial);
                residualYEle.get(sensorStereoName).fill(UresidualStereo);
                residualYvsVEle.get(sensorAxialName).fill(axialExtrapPosSensor.y(),UresidualAxial);
                residualYvsVEle.get(sensorStereoName).fill(stereoExtrapPosSensor.y(),UresidualStereo);
                residualYvsUEle.get(sensorAxialName).fill(axialExtrapPosSensor.x(),UresidualAxial);
                residualYvsUEle.get(sensorStereoName).fill(stereoExtrapPosSensor.x(),UresidualStereo);
                residualYvsPEle.get(sensorAxialName).fill(p.magnitude(),UresidualAxial);
                residualYvsPEle.get(sensorStereoName).fill(p.magnitude(),UresidualAxial);
            }
            else{
                residualYPos.get(sensorAxialName).fill(UresidualAxial);
                residualYPos.get(sensorStereoName).fill(UresidualStereo);
                residualYvsVPos.get(sensorAxialName).fill(axialExtrapPosSensor.y(),UresidualAxial);
                residualYvsVPos.get(sensorStereoName).fill(stereoExtrapPosSensor.y(),UresidualStereo);
                residualYvsUPos.get(sensorAxialName).fill(axialExtrapPosSensor.x(),UresidualAxial);
                residualYvsUPos.get(sensorStereoName).fill(stereoExtrapPosSensor.x(),UresidualStereo);
                residualYvsPPos.get(sensorAxialName).fill(p.magnitude(),UresidualAxial);
                residualYvsPPos.get(sensorStereoName).fill(p.magnitude(),UresidualAxial);
            }
        }
    }
    
    //Converts position into sensor frame
    private Hep3Vector globalToSensor(Hep3Vector trkpos, HpsSiSensor sensor){
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if(electrodes == null){
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.ELECTRON);
            System.out.println("Charge Carrier is NULL");
        }
        return electrodes.getGlobalToLocal().transformed(trkpos);
    }
    
    //Get the track state at the previous sensor
    private TrackState getTrackState(Track track, int unusedLay){
        int layer = -1;
        boolean isTop = track.getTrackStates().get(0).getTanLambda() > 0;
        //If unused layer is L1, then get trackstate at IP
        if(unusedLay == 1){
            return track.getTrackStates().get(0);
        }
        else{
            layer = unusedLay - 1;
        }
        HpsSiSensor sensorHole = getSensor(track,layer,isTop,true);
        HpsSiSensor sensorSlot = getSensor(track,layer,isTop,false);
        TrackState tState = TrackStateUtils.getTrackStateAtSensor(track,sensorHole.getMillepedeId());
        if(tState == null){
            tState = TrackStateUtils.getTrackStateAtSensor(track,sensorSlot.getMillepedeId());
        }
        return tState;
    }
    
    //Returns channel number of a given position in the sensor frame
    private int getChan(Hep3Vector pos, HpsSiSensor sensor){
        double readoutPitch = sensor.getReadoutStripPitch();
        int nChan = sensor.getNumberOfChannels();
        double height = readoutPitch * nChan;
        return (int) ((height/2-pos.x())/readoutPitch);
    }
    
    //Converts double array into Hep3Vector
    private Hep3Vector toHep3(double[] arr) {
        return new BasicHep3Vector(arr[0], arr[1], arr[2]);
    }
    
    //Return the HpsSiSensor for a given top/bottom track, layer, axial/stereo, and slot/hole
    private HpsSiSensor getSensor(Track track, int layer, boolean isAxial, boolean isHole) {
        double tanLambda = track.getTrackStates().get(0).getTanLambda();
        for(HpsSiSensor sensor: sensors){
            int senselayer = (sensor.getLayerNumber() + 1)/2;
            if(senselayer != layer) continue;
            if((tanLambda > 0 && !sensor.isTopLayer()) || (tanLambda < 0 && sensor.isTopLayer())) continue;
            if((isAxial && !sensor.isAxial()) || (!isAxial && sensor.isAxial())) continue;
            if(layer < 4 && layer > 0){
                return sensor;
            }
            else{
                if((!sensor.getSide().matches("ELECTRON") && isHole) || (sensor.getSide().matches("ELECTRON") && !isHole)) continue;
                return sensor;
            }
        }
        return null;
    }

    private int getUnusedSvtLayer(List<TrackerHit> stereoHits) {      
        int[] svtLayer = new int[6];
        
        // Loop over all of the stereo hits associated with the track
        for (TrackerHit stereoHit : stereoHits) {
            
            // Retrieve the sensor associated with one of the hits.  This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();
            
            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1)/2;
           
            // If a hit is associated with that layer, increment its 
            // corresponding counter
            svtLayer[layer - 1]++;
        }
        
        // Loop through the layer counters and find which layer has not been
        // incremented i.e. is unused by the track
        for(int layer = 0; layer < svtLayer.length; layer++){
            if(svtLayer[layer] == 0) { 
                return (layer + 1);
            }
        }
        return -1;
    }
    
    //Checks to see if track is within acceptance of both axial and stereo sensors at a given layer
    //Also returns channel number of the intersection
    private Pair<HpsSiSensor,Pair<Integer,Hep3Vector>> isWithinSensorAcceptance(Track track, TrackState tState, int layer, boolean axial, Hep3Vector p,FieldMap fieldMap) {
     
        HpsSiSensor axialSensorHole = getSensor(track,layer,true,true);
        HpsSiSensor axialSensorSlot = getSensor(track,layer,true,false);
        HpsSiSensor stereoSensorHole = getSensor(track,layer,false,true);
        HpsSiSensor stereoSensorSlot = getSensor(track,layer,false,false);
        
        HelicalTrackFit htf = TrackUtils.getHTF(tState);
        
        Hep3Vector axialTrackHolePos = TrackStateUtils.getLocationAtSensor(htf,axialSensorHole,bfield);
        Hep3Vector axialTrackSlotPos = TrackStateUtils.getLocationAtSensor(htf,axialSensorSlot,bfield);
        Hep3Vector stereoTrackHolePos = TrackStateUtils.getLocationAtSensor(htf,stereoSensorHole,bfield);
        Hep3Vector stereoTrackSlotPos = TrackStateUtils.getLocationAtSensor(htf,stereoSensorSlot,bfield);

        Pair<Boolean,Pair<Integer,Hep3Vector>> axialHolePair = this.sensorContainsTrack(axialTrackHolePos, axialSensorHole);
        Pair<Boolean,Pair<Integer,Hep3Vector>> axialSlotPair = this.sensorContainsTrack(axialTrackSlotPos, axialSensorSlot);
        Pair<Boolean,Pair<Integer,Hep3Vector>> stereoHolePair = this.sensorContainsTrack(stereoTrackHolePos, stereoSensorHole);
        Pair<Boolean,Pair<Integer,Hep3Vector>> stereoSlotPair = this.sensorContainsTrack(stereoTrackSlotPos, stereoSensorSlot);
        
        if(axialHolePair.getFirst() && axial){
            return new Pair<> (axialSensorHole,axialHolePair.getSecond());
        }
    
        if(axialSlotPair.getFirst() && axial){
            return new Pair<> (axialSensorSlot,axialSlotPair.getSecond());
        }
       
        if(stereoHolePair.getFirst() && !axial){
            return new Pair<> (stereoSensorHole,stereoHolePair.getSecond());
        }
        
        if(stereoSlotPair.getFirst() && !axial){
            return new Pair<> (stereoSensorSlot,stereoSlotPair.getSecond());
        }
        
        return null;
    }
    
    //Checks to see if track is in acceptance of sensor. Computes within sensor frame
    //Also return channel number of the position
    public Pair<Boolean,Pair<Integer,Hep3Vector>> sensorContainsTrack(Hep3Vector trackPosition, HpsSiSensor sensor){
        Hep3Vector pos = globalToSensor(trackPosition, sensor);
        int nChan = sensor.getNumberOfChannels();
        int chan = getChan(pos,sensor);
        double width = getSensorLength(sensor);
        Pair<Integer,Hep3Vector> pair = new Pair<>(chan,pos);
        if(chan < -this.chanExtd || chan > (nChan + this.chanExtd)){
            return new Pair<>(false,pair);
        }
        if(Math.abs(pos.y())>width/2){
            return new Pair<>(false,pair);
        }
        return new Pair<>(true,pair);
    }

    //Returns the horizontal length of the sensor
    protected double getSensorLength(HpsSiSensor sensor) {

        double length = 0;

        // Get the faces normal to the sensor
        final List<Polygon3D> faces = ((Box) sensor.getGeometry().getLogicalVolume().getSolid())
                .getFacesNormalTo(new BasicHep3Vector(0, 0, 1));
        for (final Polygon3D face : faces) {

            // Loop through the edges of the sensor face and find the longest one
            final List<LineSegment3D> edges = face.getEdges();
            for (final LineSegment3D edge : edges) {
                if (edge.getLength() > length) {
                    length = edge.getLength();
                }
            }
        }
        return length;
    }
    
    public void endOfData(){
        System.out.println("End of Data.");
        
    }
}