package org.hps.recon.tracking; 

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
    
    private List<String> sensorNames_ = new ArrayList<String>();

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

    public void setSkipSensors(String[] sensorNames) {
        this.sensorNames_ = new ArrayList<String>(Arrays.asList(sensorNames));
    }
    
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

            Boolean skipHit = false;
            RawTrackerHit rawHit = FittedRawTrackerHit.getRawTrackerHit(fittedHit);
            
            SiSensor sensor = (SiSensor) rawHit.getDetectorElement();
            String name = sensor.getName();
            
            if (sensorNames_.size() > 0) {
                
                for (String sensorName : sensorNames_) {
                    if (name.contains(sensorName)) {
                        skipHit = true;
                        break;
                    }
                }
            }
            
            if (skipHit)
                continue;
            
            ((SiSensor) rawHit.getDetectorElement()).getReadout().addHit(fittedHit); 
        }
    }
}
