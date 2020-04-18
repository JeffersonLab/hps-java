package org.hps.recon.tracking; 

import java.util.List;

import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit; 
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup; 


/**
 *
 */
public class SensorSetup extends RawTrackerHitSensorSetup { 

    /// Name of the collection of fitted hits 
    private String fittedHitColName_ = ""; 

    public SensorSetup() { }

    public void setFittedHitCollection(String fittedHitColName) { fittedHitColName_ = fittedHitColName; }

    @Override
    protected void process(EventHeader event) { 
        
        super.process(event);
      
        if (!event.hasCollection(LCRelation.class, fittedHitColName_)) return; 
        
        List< LCRelation > fittedHits = event.get(LCRelation.class, fittedHitColName_); 
        System.out.println("SensorSetup::process : Collection has " + fittedHits.size() + " fitted hits."); 
        
        loadFittedHits(fittedHits);  
    }

    public void loadFittedHits(List< LCRelation > fittedHits) {

        for (LCRelation fittedHit : fittedHits) { 
            RawTrackerHit rawHit = FittedRawTrackerHit.getRawTrackerHit(fittedHit);
            ((SiSensor) rawHit.getDetectorElement()).getReadout().addHit((FittedRawTrackerHit) fittedHit); 
        }
    }
}
