package org.hps.recon.tracking; 

import java.util.List;

import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit; 
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup; 


/**
 * Driver used to load FittedRawTrackerHits onto a sensor readout. 
 */
public class SensorSetup extends RawTrackerHitSensorSetup { 

    /// Name of the collection of fitted hits 
    private String fittedHitColName_ = ""; 

    /// Constructor
    public SensorSetup() { }

    /**
     * Set the name of the FittedRawTrackerHit collection to load onto a sensor
     * readout. 
     *
     * @param fittedHitColName Name of the FittedRawTrackerHit collection to 
     *      load.
     */
    public void setFittedHitCollection(String fittedHitColName) { fittedHitColName_ = fittedHitColName; }

    @Override
    protected void process(EventHeader event) { 
        
        super.process(event);
      
        if (!event.hasCollection(LCRelation.class, fittedHitColName_)) return; 
        
        List< LCRelation > fittedHits = event.get(LCRelation.class, fittedHitColName_);
        
        loadFittedHits(fittedHits);  
    }

    /**
     * Method to process all FittedRawTrackerHits and load them onto a sensor
     * readout. 
     *
     * @param fittedHits The collection of FittedRawTrackerHits to load. 
     */
    public void loadFittedHits(List< LCRelation > fittedHits) {

        for (LCRelation fittedHit : fittedHits) { 
            RawTrackerHit rawHit = FittedRawTrackerHit.getRawTrackerHit(fittedHit);
            ((SiSensor) rawHit.getDetectorElement()).getReadout().addHit(fittedHit); 
        }
    }
}
