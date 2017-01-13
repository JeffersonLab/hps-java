package org.hps.recon.ecal.cluster;

import java.util.List;
import java.util.Random;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.event.base.BaseCalorimeterHit;

/**
 * This smears the MC hit time energy according to the resolution as found in the 2016 data.
 * 
 * @author holly
 *
 */
public class HitTMCSmearDriver extends Driver {
    
    /*
     * This is the default class when used in readout. Recon MC requires EcalCalHits. 
     */
    private String inputHitCollection = "EcalCorrectedHits";
    
    public void setInputHitCollection(String inputHitCollection) {
        this.inputHitCollection = inputHitCollection;
    }
    
    
   
    // Time resolution as derived for 2016 data
    private static double calcSmear(double energy){
        Random r = new Random();
        double sigT = r.nextGaussian()*Math.sqrt(Math.pow(0.188/energy, 2) + Math.pow(0.152, 2));
        return sigT;
    }
            
    //Call the offset, correct the time, and set the time
    public void process(EventHeader event) {
            
        //Get the hits in the event       
        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputHitCollection);
            
        for (CalorimeterHit iHit : hits){
            double oldT = iHit.getTime();
            double energy = iHit.getRawEnergy();
                            
            double sigT = calcSmear(energy);
            ((BaseCalorimeterHit) iHit).setTime(oldT+sigT);

        }    
    }    
}