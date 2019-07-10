/**
 * Driver for analyzing hits in SVT
 */
/**
 * @author mrsolt
 *
 */
package org.hps.analysis.dataquality;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;

public class SvtHitPlots extends Driver {

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree; 
    IHistogramFactory histogramFactory; 

    //List of Sensors
    private List<HpsSiSensor> sensors = null;
    
    Map<String, IHistogram1D> fittedRawHitTime = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram1D> clusterTime = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram1D> helicalTrackHitTime = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram2D> fittedRawHitPosition = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram1D> fittedRawHitPositionZ = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram2D> clusterPosition = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> clusterPositionSensorCoord = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram1D> clusterPositionZ = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram1D> clusterDiff = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram2D> clusterDiff2D = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> helicalTrackHitPosition = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram1D> helicalTrackHitPositionZ = new HashMap<String,IHistogram1D>();
    Map<Integer, IHistogram2D> trackerHitPositionTop = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram1D> trackerHitPositionTopZ = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram2D> trackerHitPositionBot = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram1D> trackerHitPositionBotZ = new HashMap<Integer,IHistogram1D>();
    Map<String, IHistogram2D> xPosvsChan = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> yPosvsChan = new HashMap<String,IHistogram2D>();
    IHistogram1D trackP;
    IHistogram1D trackTopP;
    IHistogram1D trackBotP;
    IHistogram1D trackEleP;
    IHistogram1D trackEleTopP;
    IHistogram1D trackEleBotP;
    IHistogram1D trackPosP;
    IHistogram1D trackPosTopP;
    IHistogram1D trackPosBotP;
    
    //Histogram Settings
    int nBins = 120;
    
    int nLay = 14;
    
    //Collection Strings
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String stripClusterHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String helicalTrackerHitCollectionName = "HelicalTrackHits";
    private String trackerHitCollectionName = "TrackerHits";
    private String trackCollectionName = "GBLTracks";
   
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    //Beam Energy
    double ebeam;
    
    public void detectorChanged(Detector detector){

        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
    
        //Set Beam Energy
        BeamEnergyCollection beamEnergyCollection = 
                this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        ebeam = beamEnergyCollection.get(0).getBeamEnergy();      
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
   
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }

        trackP = aida.histogram1D("Track Momentum", 100, 0, 5.0);
        trackTopP = aida.histogram1D("Track Top Momentum", 100, 0, 5.0);
        trackBotP = aida.histogram1D("Track Bot Momentum", 100, 0, 5.0);
        trackEleP = aida.histogram1D("Track Ele Momentum", 100, 0, 5.0);
        trackEleTopP = aida.histogram1D("Track Ele Top Momentum", 100, 0, 5.0);
        trackEleBotP = aida.histogram1D("Track Ele Bot Momentum", 100, 0, 5.0);
        trackPosP = aida.histogram1D("Track Pos Momentum", 100, 0, 5.0);
        trackPosTopP = aida.histogram1D("Track Pos Top Momentum", 100, 0, 5.0);
        trackPosBotP = aida.histogram1D("Track Pos Bot Momentum", 100, 0, 5.0);
        
        for(int i = 0; i < nLay; i++){
            int layer = i + 1;
            trackerHitPositionTopZ.put(layer, histogramFactory.createHistogram1D("Tracker Hit Z Top Layer " + layer, 10000, 0, 1000));
            trackerHitPositionTop.put(layer, histogramFactory.createHistogram2D("Tracker Hit X-Y Top Layer " + layer, 100, -20, 20, 100,0,20));
            trackerHitPositionBotZ.put(layer, histogramFactory.createHistogram1D("Tracker Hit Z Bot Layer " + layer, 10000, 0, 1000));
            trackerHitPositionBot.put(layer, histogramFactory.createHistogram2D("Tracker Hit X-Y Bot Layer " + layer, 100, -20, 20, 100,-20,0));
        }
        
        for(HpsSiSensor sensor:sensors){
            fittedRawHitTime.put(sensor.getName(), histogramFactory.createHistogram1D("Fitted Raw Hit Time " + sensor.getName(), 100, -100, 100));
            clusterTime.put(sensor.getName(), histogramFactory.createHistogram1D("1D Cluster Hit Time " + sensor.getName(), 100, -100, 100));
            helicalTrackHitTime.put(sensor.getName(), histogramFactory.createHistogram1D("Helical Track Hit Time " + sensor.getName(), 100, -100, 100));
            fittedRawHitPositionZ.put(sensor.getName(), histogramFactory.createHistogram1D("Fitted Raw Hit Z " + sensor.getName(), 10000, 0, 1000));
            clusterPositionZ.put(sensor.getName(), histogramFactory.createHistogram1D("1D Cluster Hit Z " + sensor.getName(), 10000, 0, 1000));
            clusterDiff.put(sensor.getName(), histogramFactory.createHistogram1D("1D Cluster Hit Distance " + sensor.getName(), 100, 0, 100));
            clusterDiff2D.put(sensor.getName(), histogramFactory.createHistogram2D("1D Cluster Hit Diff Y vs X " + sensor.getName(), 100, -20, 20, 100, -10, 10));
            helicalTrackHitPositionZ.put(sensor.getName(), histogramFactory.createHistogram1D("Helical Track Hit Z " + sensor.getName(), 10000, 0, 1000));
            fittedRawHitPosition.put(sensor.getName(), histogramFactory.createHistogram2D("Fitted Raw Hit X-Y " + sensor.getName(), 100, -100, 100, 100,-100,100));
            clusterPositionSensorCoord.put(sensor.getName(), histogramFactory.createHistogram2D("1D Cluster Hit U-V " + sensor.getName(), 100, -15, 15, 100,-10,10));
            clusterPosition.put(sensor.getName(), histogramFactory.createHistogram2D("1D Cluster Hit X-Y " + sensor.getName(), 100, -10, 10, 100,-10,10));
            helicalTrackHitPosition.put(sensor.getName(), histogramFactory.createHistogram2D("Helical Track Hit X-Y " + sensor.getName(), 100, -20, 20, 100,-5,5));
            xPosvsChan.put(sensor.getName(), histogramFactory.createHistogram2D("X vs Chan " + sensor.getName(), 640, 0, 640, 100,-20,20));
            yPosvsChan.put(sensor.getName(), histogramFactory.createHistogram2D("Y vs Chan " + sensor.getName(), 640, 0, 640, 100,-20,20));
        }

    }

    public void process(EventHeader event){
        aida.tree().cd("/");
        
        // Get the list of fitted hits from the event
        List<LCRelation> fittedHits = event.get(LCRelation.class, fittedHitsCollectionName);
         
        // Map the fitted hits to their corresponding raw hits
        Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();

        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
        
        List<TrackerHit> clusterHits = event.get(TrackerHit.class, stripClusterHitCollectionName);
        
        List<TrackerHit> helicalTrackHits = event.get(TrackerHit.class, helicalTrackerHitCollectionName);
        
        if(event.hasCollection(SimTrackerHit.class, trackerHitCollectionName)){
            System.out.println("Event has tracker hits!");
            List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class,trackerHitCollectionName);
            for(SimTrackerHit hit : trackerHits){
                //HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement());
                int layer = hit.getLayer();
                if(hit.getPosition()[1] > 0){
                    trackerHitPositionTopZ.get(layer).fill(hit.getPosition()[2]);
                    trackerHitPositionTop.get(layer).fill(hit.getPosition()[0],hit.getPosition()[1]);
                }
                else{
                    trackerHitPositionBotZ.get(layer).fill(hit.getPosition()[2]);
                    trackerHitPositionBot.get(layer).fill(hit.getPosition()[0],hit.getPosition()[1]);
                }
            }
        }
                 
        for (LCRelation fittedHit : fittedHits) {
            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
        }
        
        for (LCRelation hit : fittedHits) {
            RawTrackerHit rth = (RawTrackerHit) hit.getFrom();
            GenericObject pars = (GenericObject) hit.getTo();

            HpsSiSensor sensor = ((HpsSiSensor) rth.getDetectorElement());
            //this is a clever way to get the parameters we want from the generic object
            double t0 = ShapeFitParameters.getT0(pars);
            double amp = ShapeFitParameters.getAmp(pars);
            double chiProb = ShapeFitParameters.getChiProb(pars);
            int channel = rth.getIdentifierFieldValue("strip");
            fittedRawHitTime.get(sensor.getName()).fill(t0);
            fittedRawHitPositionZ.get(sensor.getName()).fill(rth.getPosition()[2]);
            fittedRawHitPosition.get(sensor.getName()).fill(rth.getPosition()[0],rth.getPosition()[1]);
        }
        
        for(TrackerHit hit : clusterHits){
            SiTrackerHitStrip1D local = ((SiTrackerHitStrip1D) hit).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
            SiTrackerHitStrip1D global = ((SiTrackerHitStrip1D) hit).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement());
            clusterPositionSensorCoord.get(sensor.getName()).fill(local.getPosition()[1],local.getPosition()[0]);
            clusterTime.get(sensor.getName()).fill(hit.getTime());
            clusterPositionZ.get(sensor.getName()).fill(hit.getPosition()[2]);
            clusterPosition.get(sensor.getName()).fill(hit.getPosition()[0],hit.getPosition()[1]);
            int channel = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
            xPosvsChan.get(sensor.getName()).fill(channel, hit.getPosition()[0]);
            yPosvsChan.get(sensor.getName()).fill(channel, hit.getPosition()[1]);
            double dist = 9999;
            double [] pos1 = hit.getPosition();
            double [] minpos2 = {9999,9999,9999};
            for(TrackerHit hit2 : clusterHits){
                HpsSiSensor sensor2 = ((HpsSiSensor) ((RawTrackerHit) hit2.getRawHits().get(0)).getDetectorElement());
                if(!(sensor.getLayerNumber() == sensor2.getLayerNumber())){
                    continue;
                }
                if(sensor.isTopLayer() == sensor2.isTopLayer()){
                    continue;
                }
                if(sensor.equals(sensor2)){
                    continue;
                }
                double [] pos2 = hit2.getPosition();
                if(Math.sqrt((pos1[0]-pos2[0])*(pos1[0]-pos2[0])+(pos1[1]-pos2[1])*(pos1[1]-pos2[1])) < dist){
                    dist = Math.sqrt((pos1[0]-pos2[0])*(pos1[0]-pos2[0])+(pos1[1]-pos2[1])*(pos1[1]-pos2[1]));
                    minpos2 = pos2;
                }
                //System.out.println(sensor.getName() + " " + sensor2.getName() + " " + dist);
            }
            //System.out.println("Min Distance " + dist);
            clusterDiff.get(sensor.getName()).fill(dist);
            clusterDiff2D.get(sensor.getName()).fill(pos1[0]-minpos2[0],pos1[1]-minpos2[1]);
        }
        
        for(TrackerHit hit : helicalTrackHits){
            int i = 1;
            HpsSiSensor sensor =  ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement());
            helicalTrackHitTime.get(sensor.getName()).fill(hit.getTime());
            helicalTrackHitPositionZ.get(sensor.getName()).fill(hit.getPosition()[2]);
            helicalTrackHitPosition.get(sensor.getName()).fill(hit.getPosition()[0],hit.getPosition()[1]);
        }
        
        if(event.hasCollection(Track.class, trackCollectionName)){
            List<Track> tracks = event.get(Track.class, trackCollectionName);
            for(Track track : tracks){
                TrackState ts = track.getTrackStates().get(0);
                double[] p = ts.getMomentum();
                double pTot = Math.sqrt(p[0]*p[0] + p[1]*p[1] + p[2]*p[2]);
                trackP.fill(pTot);
                if(ts.getOmega() > 0){
                    trackEleP.fill(pTot);
                }
                else{
                    trackPosP.fill(pTot);
                }
                if(p[1] > 0){
                    trackTopP.fill(pTot);
                    if(ts.getOmega() > 0){
                        trackEleTopP.fill(pTot);
                    }
                    else{
                        trackPosTopP.fill(pTot);
                    }
                }
                else{
                    trackBotP.fill(pTot);
                    if(ts.getOmega() > 0){
                        trackEleBotP.fill(pTot);
                    }
                    else{
                        trackPosBotP.fill(pTot);
                    }
                }
            }
        }
    }
}