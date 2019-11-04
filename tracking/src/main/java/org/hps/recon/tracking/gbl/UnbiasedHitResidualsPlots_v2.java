/*
 * Unbiased Hit Residuals are computed
 */
/**
 * @author mrsolt
 * @author pf
 *
 */
package org.hps.recon.tracking.gbl;

//import static java.lang.Math.sin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


//import static java.lang.Math.sin;
//import static java.lang.Math.sqrt;
import static java.lang.Math.signum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import hep.physics.matrix.SymmetricMatrix;
//import org.hps.recon.tracking.gbl.matrix.Matrix;
//import org.hps.recon.tracking.gbl.matrix.Vector;
//import org.hps.recon.tracking.gbl.matrix.SymMatrix;
//import org.lcsim.event.base.BaseTrackState;
//import org.hps.recon.tracking.HpsHelicalTrackFit;
//import org.lcsim.fit.helicaltrack.HelixUtils;

import java.io.IOException;
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
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.LCRelation;

import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.constants.Constants;
import org.apache.commons.math3.util.Pair;
import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;


public class UnbiasedHitResidualsPlots_v2 extends Driver {

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree; 
    IHistogramFactory histogramFactory; 

    //List of Sensors
    private List<HpsSiSensor> sensors = null;

    HpsGblRefitter gbl_refitter = new HpsGblRefitter();

    
    //Debug flag
    private int debug_level = 0;
    
    //List of Histograms
    //IHistogram1D momentum;
    
    
    Map<String, IHistogram1D> residualY = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram1D> b_residualY = new HashMap<String,IHistogram1D>();
    
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
    
    Map<String, IHistogram2D> residualYvsP   = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> b_residualYvsP = new HashMap<String,IHistogram2D>();
    
    Map<String, IHistogram2D> residualYvsPEle = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> residualYvsPPos = new HashMap<String,IHistogram2D>();
    
    //Histogram Settings
    int nBins = 200;
    double maxYRes = 0.15;
    double minYRes = -maxYRes;

    
    //Collection Strings
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String GBLTrackCollectionName = "GBLTracks";
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    
    //Bfield
    protected static double bfield;
    FieldMap bFieldMap = null;
    protected static double bfac;
   
    private static final String SUBDETECTOR_NAME = "Tracker";

    boolean cleanFEE = false;
    double nSig = 5;
    int chanExtd = 0;
    
    //Daq map
    SvtChannelCollection channelMap;
    SvtDaqMappingCollection daqMap;

    public void setDebug_Level(int val) { 
        debug_level = val;
    }
    
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
        BeamEnergyCollection beamEnergyCollection = this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        ebeam = beamEnergyCollection.get(0).getBeamEnergy();
        
        bfield = TrackUtils.getBField(detector).magnitude();
        bfac = Constants.fieldConversion * bfield;
        bFieldMap = detector.getFieldMap();
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
            .getDetectorElement().findDescendants(HpsSiSensor.class);
   
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }
        
        //***********//
        //Setup Plots//
        //***********//
        
        //momentum = histogramFactory.createHistogram1D("Momentum", nBins, 0, 1.5*ebeam);
        aida.histogram1D("d0_bottom",100,-2.0,2.0);
        aida.histogram1D("z0_bottom",100,-2.0,2.0);
        aida.histogram1D("p_bottom" ,200, 0,5);
        aida.histogram1D("q_over_p_bottom" ,200, 0,2);
        aida.histogram1D("chi2_bottom" ,100, 0,20);
        
        
        aida.histogram1D("d0_top",  100,-2.0,2.0);
        aida.histogram1D("z0_top",  100,-2.0,2.0);
        aida.histogram1D("p_top" ,  200, 0,  5);
        aida.histogram1D("q_over_p_top" ,200, 0,2);
        aida.histogram1D("chi2_top",200, 0,  20);

        //General residual plots
        aida.histogram1D("u_res_svt_",nBins,minYRes,maxYRes);
        aida.histogram2D("u_res_svt_p_",100,0,5,nBins,minYRes,maxYRes);
        aida.histogram1D("b_res_svt_",nBins,minYRes,maxYRes);
        aida.histogram2D("b_res_svt_p_",100,0,5,nBins,minYRes,maxYRes);
        

        for(HpsSiSensor sensor:sensors){
            String sensorName = sensor.getName();
            int nChan = sensor.getNumberOfChannels();
            double readoutPitch = sensor.getReadoutStripPitch();
            double maxU = nChan * readoutPitch / 2;
            double width = getSensorLength(sensor);
            double maxV = width/2.;
            double minV = -maxV;
            
            residualY.put(sensorName,histogramFactory.createHistogram1D("u_res_" + sensorName, nBins, minYRes, maxYRes));
            b_residualY.put(sensorName,histogramFactory.createHistogram1D("b_res_" + sensorName, nBins, 0.5*minYRes, 0.5*maxYRes));
            //residualYSingleHit.put(sensorName,histogramFactory.createHistogram1D("Residual_U_Single_Hit_" + sensorName, nBins, minYRes, maxYRes));
            //residualYMultipleHit.put(sensorName,histogramFactory.createHistogram1D("Residual_U_Multiple_Hit_" + sensorName, nBins, minYRes, maxYRes));
            //residualYSingleHitvsV.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_V_Single_Hit_" + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            //residualYSingleHitvsU.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_U_Single_Hit_" + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            //residualYMultipleHitvsV.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_V_Multiple_Hit_" + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            //residualYMultipleHitvsU.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_U_Multiple_Hit_" + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            //residualYvsV.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_V_" + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            //residualYvsU.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_U_" + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            //residualYEle.put(sensorName,histogramFactory.createHistogram1D("Residual_U_Electron_" + sensorName, nBins, minYRes, maxYRes));
            //residualYvsVEle.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_V_Electron_" + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            //residualYvsUEle.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_U_Electron_" + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            //residualYPos.put(sensorName,histogramFactory.createHistogram1D("Residual_U_Positron_" + sensorName, nBins, minYRes, maxYRes));
            //residualYvsVPos.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_V_Positron_" + sensorName, 2*nBins, minV, maxV, nBins, minYRes, maxYRes));
            //residualYvsUPos.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_U_Positron_" + sensorName, 2*nBins, -maxU, maxU, nBins, minYRes, maxYRes));
            residualYvsP.put(sensorName,histogramFactory.createHistogram2D("u_res_vs_p_" + sensorName, 25, 0, 5, nBins, minYRes, maxYRes));
            b_residualYvsP.put(sensorName,histogramFactory.createHistogram2D("b_res_vs_p_" + sensorName, 25, 0, 5, nBins, 0.5*minYRes, 0.5*maxYRes));
            
            //residualYvsPEle.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_P_Electron_" + sensorName, nBins, 0, 1.3*ebeam, nBins, minYRes, maxYRes));
            //residualYvsPPos.put(sensorName,histogramFactory.createHistogram2D("Residual_U_vs_P_Positron_" + sensorName, nBins, 0, 1.3*ebeam, nBins, minYRes, maxYRes));
        }
    }

    public void process(EventHeader event){
        aida.tree().cd("/");
        
        List<Track>       refittedTracks = new ArrayList<Track>();
        List<LCRelation>  trackRelations = new ArrayList<LCRelation>();

        //Grab all GBL tracks in the event
        List<Track> tracks = event.get(Track.class,GBLTrackCollectionName);
        
        //Grab all the GBLStripClusterData
        List<GBLStripClusterData> gblStripClusterDataList = event.get(GBLStripClusterData.class,"GBLStripClusterData");
        
        //Grab all the GBLStripClusterData Relations
        List<LCRelation>  gblStripClusterDataRelations = null;
        
        if (event.hasCollection(LCRelation.class,"GBLStripClusterDataRelations")) {
            gblStripClusterDataRelations = event.get(LCRelation.class,"GBLStripClusterDataRelations"); 
        }
        else 
            return;
        
        RelationalTable gblStripClustersToTrack = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        
        for (LCRelation gblSCDrel : gblStripClusterDataRelations) {
            if (gblSCDrel != null && gblSCDrel.getFrom()!=null && gblSCDrel.getTo() != null)
                gblStripClustersToTrack.add(gblSCDrel.getFrom(),gblSCDrel.getTo());
        }
        
        for (Track track : tracks) {

           
            //Get track momentum
            TrackState trackState = track.getTrackStates().get(0);
            Hep3Vector trkmom = new BasicHep3Vector(trackState.getMomentum());
            double trackp = trkmom.magnitude();
            
            //fillBasic plots
            doBasicGBLtrack(track);

            //Convert Set to List
            List<GBLStripClusterData> gblStripClustersOnTrack = new ArrayList<GBLStripClusterData>(gblStripClustersToTrack.allTo(track));
            
            if (debug_level > 1) {
                System.out.println("Strip cluster data on track size (to): " + gblStripClustersToTrack.allTo(track).size());
            }
            //Sort the hits - not necessary in principle, but easier.
            Collections.sort(gblStripClustersOnTrack,new MPIDComparator());
            
            //Get the hit you want to refit without
            for (GBLStripClusterData gblSC : gblStripClustersOnTrack) {
                
                //Perfom the refit

                //Do not refit removing the scatters only.
                if (gblSC.getScatterOnly() == 1)
                    continue;
                
                
                boolean IsTop = gblSC.getVolume() == 0 ? true : false;
                //Get the detector element of the hit => for the extrapolation to sensor
                HpsSiSensor gblScatterSensor = null;
                for (HpsSiSensor sensor : sensors) {
                    if (sensor.getMillepedeId() == gblSC.getId() && sensor.isTopLayer() == IsTop) {
                        gblScatterSensor = sensor;
                        break;
                    }
                }
                if (debug_level > 1)  {
                    System.out.println("Will remove the sensor:");
                    System.out.println(gblScatterSensor.toString());
                }
                
                if (debug_level > 0) {
                    int isTop = Math.sin(gblSC.getTrackLambda()) > 0 ? 0 : 1;
                    System.out.println("GBLStripCluster Data: Volume " + gblSC.getVolume() + " MPID: " + gblSC.getId() +" Track Volume: " + isTop);
                }
                
                double unbiased_res = computeRefitAndResiduals(track,gblSC,gblStripClustersOnTrack,gblScatterSensor,false);
                double biased_res   = computeRefitAndResiduals(track,gblSC,gblStripClustersOnTrack,gblScatterSensor,true);
                                
                aida.histogram1D("u_res_svt_").fill(unbiased_res);
                aida.histogram2D("u_res_svt_p_").fill(trackp,unbiased_res);
                aida.histogram1D("b_res_svt_").fill(biased_res);
                aida.histogram2D("b_res_svt_p_").fill(trackp,unbiased_res);
                
                residualY.get(gblScatterSensor.getName()).fill(unbiased_res);
                residualYvsP.get(gblScatterSensor.getName()).fill(trackp,unbiased_res);
                b_residualY.get(gblScatterSensor.getName()).fill(biased_res);
                b_residualYvsP.get(gblScatterSensor.getName()).fill(trackp,biased_res);
                
                //aida.histogram1D("Residual_U_"+gblScatterSensor.).fill(res);
                //aida.histogram1D("Residual_U_vs_P_"+gblScatterSensor.getName()).fill(res,trackp);
                
                
            } //Loop on hits
        }//Loop on tracks
    }
    
    private Track getUnbiasedTrack(Track trk, TrackerHit hit) {
        
        //Get the GBLStripClusterData of the track
        RelationalTable gblStripClusterDataToTrack = null;
        
        
        //Check that the strip is an instance of SiTrackerHitStrip1D
        if (!(hit instanceof SiTrackerHitStrip1D)) {
            System.out.println("UnbiasedHitResidualsPlots::getUnbiasedTrack::Wrong hit class");
            //Is this correct?
            return null;
        }
        SiTrackerHitStrip1D siStripCluster = (SiTrackerHitStrip1D) hit;
        HpsSiSensor hps_ss = (HpsSiSensor)((siStripCluster.getRawHits()).get(0)).getDetectorElement();
        System.out.println("Hit Sensor: " + hps_ss.getLayerNumber()+ " Side" + hps_ss.getSide() + " isTop:" + hps_ss.isTopLayer() + " isAxial:" + hps_ss.isAxial());

        //TODO : Check if there is a hit on the other side of the silicon
        //Get the identifier of the opposite side
        //Loop on the track hits again and check if there is a hit in the opposite side
        //Get the track parameters and the covariance matrix on the opposite side
        //If valid remove the hit on the opposite side 

        //Get the track parameters at sensor:
        TrackState tState = TrackStateUtils.getTrackStateAtSensor(trk,hps_ss.getMillepedeId());
        List<TrackState> tStates = trk.getTrackStates();
        System.out.println("Track States = " + tStates.size());
        System.out.println("Track state at sensor:");
        if (tState != null) {
            System.out.println(tState.getParameters()[0] + " " +tState.getParameters()[1]+" " +tState.getParameters()[2]);
        }
        
        //Make a new GblTrack
        
        return null;
    }

    //TODO Move this in some utilities class!
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
        final List<Polygon3D> faces = ((Box) sensor.getGeometry().getLogicalVolume().getSolid()).getFacesNormalTo(new BasicHep3Vector(0, 0, 1));
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
        try {
            aida.saveAs("test.root");
        } catch (IOException ex) {
            //nothing
        }
    }
    
    private static class MPIDComparator implements Comparator<GBLStripClusterData> {
        
        @Override
            public int compare(GBLStripClusterData x, GBLStripClusterData y) {
            return Integer.compare(x.getId(),y.getId());
        }
    }


    private void doBasicGBLtrack(Track trk) {
    
        //Get track states wrt IP
        TrackState trackState = trk.getTrackStates().get(0);
    
        String volume = "top";
        if (trackState.getTanLambda() < 0) {
            volume = "bottom";
        }
    
        aida.histogram1D("d0_" + volume).fill(trackState.getD0());
        aida.histogram1D("z0_" + volume).fill(trackState.getZ0());
    
        Hep3Vector momentum = new BasicHep3Vector((trackState.getMomentum()));
        double charge = signum(trackState.getOmega());
    
        aida.histogram1D("p_" + volume).fill(momentum.magnitude());
        aida.histogram1D("q_over_p_" + volume).fill(charge/momentum.magnitude());
    
        aida.histogram1D("chi2_" + volume).fill(trk.getChi2());
        //Hep3Vector beamspot = CoordinateTransformations.transformVectorToDetector(TrackUtils.extrapolateHelixToXPlane(trackState, 0));
        //        if (debug)
        //      System.out.printf("beamspot %s transformed %s \n", beamspot.toString());
        //  aida.histogram1D("beamspot_x_" + isTop).fill(beamspot.x());
        //  aida.histogram1D("beamspot_y_" + isTop).fill(beamspot.y());
    }
    
    private double computeRefitAndResiduals(Track track, GBLStripClusterData gblSC, List<GBLStripClusterData> gblStripClustersOnTrack, HpsSiSensor gblScatterSensor, boolean doBiasedResiduals) {
        
        
        //Remove hit and make a new list of StripClusters
        List<GBLStripClusterData> u_gblSCs = new ArrayList<GBLStripClusterData>();
        //Remove using the MPID
        //For the moment no truly unbiased, just one scatter plane removed.
        
        
        for (GBLStripClusterData gblSC_for_refit : gblStripClustersOnTrack) {
            if (!doBiasedResiduals) {
                if (gblSC_for_refit.getId() != gblSC.getId())
                    u_gblSCs.add(gblSC_for_refit);
                else {
                    //Make a copy
                    GBLStripClusterData gblSC_copy = new GBLStripClusterData(gblSC);
                    //Make the hit scatter only
                    gblSC_copy.setScatterOnly(1);
                    //Add it to the list of stripClusters for fitting
                    u_gblSCs.add(gblSC_copy);
                }
            }
            else
                u_gblSCs.add(gblSC_for_refit);
        }
        
        if (debug_level > 1) {
            System.out.println("List for refit:" + u_gblSCs.size());
            for (GBLStripClusterData u_gblSC : u_gblSCs) {
                System.out.println("SC MPID:" + u_gblSC.getId()+ " Vol: " +u_gblSC.getVolume() + " s:" + u_gblSC.getPath3D()+ " scatterOnly:" + u_gblSC.getScatterOnly());
            }
        }
        
        //Fit the GBL trajectory with n-1 hits (TODO::iterate)
        FittedGblTrajectory refit = gbl_refitter.fit(u_gblSCs,bfac,false);
        
        //Return a boolean?
        if (refit == null)
            return -999;
        
        
        //Get the sensorArray in the fittedGblTrajector
        Integer [] sensorsFromMapArray = refit.getSensorMap().keySet().toArray(new Integer[0]);
        
        //Find the label of the removed hit
        int iLabel = -1;
        for (int i = 0; i<sensorsFromMapArray.length; i++) {
            iLabel = sensorsFromMapArray[i];
            int millepedeID = refit.getSensorMap().get(iLabel);
            if (millepedeID == gblSC.getId()) {
                if (debug_level>0) {
                    System.out.println("Found original sensor in the refitted trajectory:");
                    System.out.println("MPID:" + gblSC.getId() + " ilabel " + iLabel);
                }
                break;
            }
        }
        
        //Correct the track parameters using the corrections at that surface
        //Covariance matrix is wrong (?)
        Pair<double[],SymmetricMatrix> unbiasedHelixParams_surface = refit.getCorrectedPerigeeParameters(TrackUtils.getHTF(track),iLabel,bfield);
        
        //Construct the helical track fit with the track parameters corrected at the surface wrt ref point:
        double [] chisq = {0,0};
        int [] ndf   = {0,0};
        HelicalTrackFit unbiased_htf = new HelicalTrackFit(unbiasedHelixParams_surface.getFirst(),
                                                           unbiasedHelixParams_surface.getSecond(),
                                                           chisq,ndf,null,null);

        //In global to the sensor
        Hep3Vector global_extr_pos       = TrackStateUtils.getLocationAtSensor(unbiased_htf,gblScatterSensor,bfield);
        
        //In local to the sensor
        Hep3Vector local_extr_pos        = gblScatterSensor.getGeometry().getGlobalToLocal().transformed(global_extr_pos);
        
        //This gives the same answer of above (apart from w, which is defined with origin in the center of the sensor)
        //Track inside sensor, channel number of position, local position
        Pair<Boolean,Pair<Integer,Hep3Vector>>  local_extr_info = this.sensorContainsTrack(global_extr_pos,gblScatterSensor);
        double res              = gblSC.getMeas() - ((local_extr_info.getSecond()).getSecond()).x();
        
        if (debug_level > 2) {
            System.out.println("unbiasedHelixParams vs old HelixParams ");
            System.out.println(Arrays.toString(unbiasedHelixParams_surface.getFirst()));
            System.out.println(".....");
            System.out.println(Arrays.toString(track.getTrackParameters()));
            System.out.println("Local Position to the sensor - from electrodes:");
            System.out.println(((local_extr_info.getSecond()).getSecond()).x() + " " +((local_extr_info.getSecond()).getSecond()).y()+ " " +((local_extr_info.getSecond()).getSecond()).z());
            System.out.println("Local Position to the sensor - from sensor surface:");
            System.out.println(local_extr_pos.x() + " " +local_extr_pos.y() + " "+local_extr_pos.z());
            System.out.println("umeas= " + gblSC.getMeas() + " u=" + ((local_extr_info.getSecond()).getSecond()).x());
            System.out.println("R     = umeas - u = " + res);
            //They do not perfectly align yet for outer layers.
            //Residuals from GBL
            /*
              for (GblData gblDataPoint : refit.get_traj().getData()) {
              
              double [] results = {-999,-999,-999};
              List<Integer> indLocal = new ArrayList<Integer>();
              List<Double> derLocal = new ArrayList<Double>();
              gblDataPoint.getResidual(results,indLocal,derLocal);
              System.out.println(gblDataPoint.theLabel + " R_gbl = umeas - u = "+results[0]);
              
              }
            */
        }//debug
        
        return res;
    }
}
