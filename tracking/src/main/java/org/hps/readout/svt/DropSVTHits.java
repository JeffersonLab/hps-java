package org.hps.readout.svt;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Removes raw tracker hits based on SVT Inefficiencies.
 *
 * @author Matt Solt <mrsolt@slac.stanford.edu>
 */

public class DropSVTHits extends Driver {

    //Hit Efficiencies
    private boolean enableHitEfficiency = false;
    private boolean enableHitEfficiencyDistribution = false;
    private double layer1HitEfficiency = 1.0;
    
    //Collection Names
    private String removedOutputCollection = "SVTRawTrackerHitsRemoved";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    
    public void setEnableHitEfficiency(boolean enableHitEfficiency) {
        this.enableHitEfficiency = enableHitEfficiency;
    }
    
    public void setEnableHitEfficiencyDistribution(boolean enableHitEfficiencyDistribution) {
        this.enableHitEfficiencyDistribution = enableHitEfficiencyDistribution;
    }
    
    public void setLayer1HitEfficiency(double layer1HitEfficiency) {
        this.layer1HitEfficiency = layer1HitEfficiency;
    }
    
    public void process(EventHeader event) {
        
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
        List<RawTrackerHit> removedRawHits = new ArrayList<RawTrackerHit>();

        Iterator<RawTrackerHit> iter = rawHits.iterator();
        while (iter.hasNext()) {
            RawTrackerHit hit = iter.next();
            if(!KeepHit(hit,enableHitEfficiencyDistribution,layer1HitEfficiency) && enableHitEfficiency){
                removedRawHits.add(hit);
                iter.remove();
            }
        }
        
        int flags = 1 << LCIOConstants.TRAWBIT_ID1;
        event.put(removedOutputCollection, removedRawHits, RawTrackerHit.class, flags);
    }
    
    private boolean KeepHit(RawTrackerHit hit, boolean enableHitEfficiencyDistribution, double layer1HitEfficiency) {
        boolean keepHit = true;
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        int layer = sensor.getLayerNumber();
        double random = Math.random();
        if(!enableHitEfficiencyDistribution){
            if (random > layer1HitEfficiency && (layer == 1 || layer == 2)){
                keepHit = false;
            }
            return keepHit;
        }
        else{
            double eff = computeEfficiency(hit,sensor);
            if(random > eff){
                keepHit = false;
            }
            return keepHit;
        }
    }
    
    private double computeEfficiency(RawTrackerHit hit, HpsSiSensor sensor){
        double eff = 1.0;
        return eff;
    }
}
