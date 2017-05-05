package org.hps.recon.ecal.cluster;

import java.util.List;
import java.util.Random;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.event.base.BaseCalorimeterHit;

/**
 * This smears the MC hit time energy according to the resolution as found in the 2016 data.
 */
public class HitTMCSmearDriver extends Driver {
    
    /*
     * This is the default class when used in readout. Recon MC requires EcalCalHits. 
     */
    private String inputHitCollection = "EcalCalHits";
    //not EcalRawHits or GTPHits
    //EcalCorrectedHits keeps increasing in time
    
    public void setInputHitCollection(String inputHitCollection) {
        this.inputHitCollection = inputHitCollection;
    }
    
    
   
    // Time resolution as derived for 2016 data
    //Factor 1.98 derived after running over MC. Probably due to
    //lack of trigger jitter in simulation.
    private static double calcSmear(double energy, double time){
        Random r = new Random();
        double sigT = r.nextGaussian()*Math.sqrt(Math.pow(0.188/energy, 2) + Math.pow(0.152, 2))/1.98;
        return time + sigT;
    }
            
    //Call the offset, correct the time, and set the time
    public void process(EventHeader event) {
        
        if (event.hasCollection(CalorimeterHit.class, inputHitCollection)){
            //Get the hits in the event       
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputHitCollection);
            
            for (CalorimeterHit iHit : hits){
                double oldT = iHit.getTime();
                double energy = iHit.getRawEnergy();          
                double newT = calcSmear(energy, oldT);
                
                ((BaseCalorimeterHit) iHit).setTime(newT);

            }    
        } 
    }
}