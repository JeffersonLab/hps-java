/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.filtering;

import java.util.List;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.recon.tracking.HPSFittedRawTrackerHit;
import org.lcsim.hps.recon.tracking.SvtUtils;

/**

 @author mgraham
 @version $Id:
 */
public class InTimeHitsFilter extends EventReconFilter{
   
    private int minSiLayers=10;
    private String hitCollectionName="SVTFittedRawTrackerHits";
    private double hitTimeCut=999;

    public void setClusterTimeCut(double dtCut){
        this.hitTimeCut=dtCut;
    }


    @Override
    public void process(EventHeader event){
       incrementEventProcessed();
        if(!event.hasCollection(HPSFittedRawTrackerHit.class, hitCollectionName))
            skipEvent(); 
        if(hitTimeCut<0)  //why are you even doing this???
            return;

        List<HPSFittedRawTrackerHit> hits=event.get(HPSFittedRawTrackerHit.class, hitCollectionName);

        int totalTopHit=0;
        int totalBotHit=0;

        int nlayersTopHit=0;
        int nlayersBotHit=0;
        int[] layersTop={0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] layersBot={0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        for(HPSFittedRawTrackerHit hit : hits){
            boolean timeCut=inTime(hit.getT0());
//            System.out.println("t0 =" + hit.getT0());
            if(timeCut){
                RawTrackerHit rth=hit.getRawTrackerHit();
                int layerNumber=rth.getLayerNumber();
                boolean isTop=isHitOnTop(rth);
                if(isTop){
                    totalTopHit++;
                    layersTop[layerNumber-1]++;
                } else{
                    totalBotHit++;
                    layersBot[layerNumber-1]++;
                }
            }
        }
        //require hits in the first minSiLayers for both top and bottom planes
        for(int i=0; i<minSiLayers; i++){
            if(layersTop[i]==0)
                skipEvent(); 
            if(layersBot[i]==0)
                skipEvent(); 
        }

        incrementEventPassed();
    }

 

    private boolean inTime(double t0){
        return Math.abs(t0)<hitTimeCut;

    }

    private boolean isHitOnTop(RawTrackerHit hit){
        SiSensor sensor=(SiSensor) hit.getDetectorElement();
        IIdentifier id=hit.getIdentifier();
        SiTrackerIdentifierHelper _sid_helper=(SiTrackerIdentifierHelper) sensor.getIdentifierHelper();

        ChargeCarrier carrier=ChargeCarrier.getCarrier(_sid_helper.getSideValue(id));
        SiSensorElectrodes electrodes=((SiSensor) hit.getDetectorElement()).getReadoutElectrodes(carrier);
        if(!SvtUtils.getInstance().isTopLayer(sensor))
            return false;
        return true;
    }
}
