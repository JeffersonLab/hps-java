package org.hps.recon.ecal.cluster;

import java.util.List;
import java.util.Random;

import org.hps.util.TruthCalorimeterHit;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.event.base.BaseCalorimeterHit;

/**
 * Driver <code>HitTMCSmearDriver</code> is  responsible for smearing
 * hit times to better match the time resolution as studied in the
 * 2016 data set.
 * 
 * @author Holly
 */
public class HitTMCSmearDriver extends Driver {
    /**
     * This is the default class when used in readout. Recon MC
     * requires EcalCalHits. 
     */
    private String inputHitCollection = "EcalCalHits";
    //not EcalRawHits or GTPHits
    //EcalCorrectedHits keeps increasing in time
    
    /**
     * Sets the name of the hit collection that is to undergo time
     * correction.
     * @param inputHitCollection - The collection name.
     */
    public void setInputHitCollection(String inputHitCollection) {
        this.inputHitCollection = inputHitCollection;
    }
    
    /**
     * Calculates the time smear for a given hit energy and time.
     * @param energy - The energy of the hit.
     * @param time - The time of the hit.
     * @return Returns a time-corrected value for the hit.
     */
    private static double calcSmear(double energy, double time){
        // Time resolution as derived for 2016 data
        //Factor 1.98 derived after running over MC. Probably due to
        //lack of trigger jitter in simulation.
        Random r = new Random();
        double sigT = r.nextGaussian() * Math.sqrt(Math.pow(0.188 / energy, 2) + Math.pow(0.152, 2)) / 1.98;
        return time + sigT;
    }
    
    /**
     * Calculates the time correction for event hits and modifies the
     * hits to use the corrected time value.
     * @param event - The event containing the hits that need to be
     * modified.
     */
    @Override
    public void process(EventHeader event) {
        if(event.hasCollection(CalorimeterHit.class, inputHitCollection)){
            // Get the hits in the event.
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputHitCollection);
            
            // Smear the times for each hit.
            for(CalorimeterHit iHit : hits) {
                // Calculate the smeared time.
                double oldT = iHit.getTime();
                double energy = iHit.getRawEnergy();
                double newT = calcSmear(energy, oldT);
                
                // If truth information exists, then hits will of the
                // type TruthCalorimeterHit. Otherwise, they will be
                // of type BaseCalorimeterHit. Ascertain the hit type
                // and then modify the hit time.
                if(iHit instanceof TruthCalorimeterHit) {
                    ((TruthCalorimeterHit) iHit).setTime(newT);
                } else {
                    ((BaseCalorimeterHit) iHit).setTime(newT);
                }
            }
        } 
    }
}