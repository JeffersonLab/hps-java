/**
 * This driver removes a helical track hit for each layer
 * and then puts hits into a new collection of rotated helical
 * track hits with the missing hit.
 */
/**
 * @author mrsolt
 *
 */
package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

public class RemoveHelicalTrackHit extends Driver {
    
    //New Track Collection Strings
    private String tracksMissingL0CollectionName = "RotatedHelicalTrackHitsMissingL0";
    private String tracksMissingL1CollectionName = "RotatedHelicalTrackHitsMissingL1";
    private String tracksMissingL2CollectionName = "RotatedHelicalTrackHitsMissingL2";
    private String tracksMissingL3CollectionName = "RotatedHelicalTrackHitsMissingL3";
    private String tracksMissingL4CollectionName = "RotatedHelicalTrackHitsMissingL4";
    private String tracksMissingL5CollectionName = "RotatedHelicalTrackHitsMissingL5";
    private String tracksMissingL6CollectionName = "RotatedHelicalTrackHitsMissingL6";
    
    private String rotatedHelicalTrackHitCollectionName = "RotatedHelicalTrackHits";

    int nLay = 6; //number of layers
    
    public void setNLay(int nLay) { 
        this.nLay = nLay;
    }

    public void process(EventHeader event){
         
        //Loop over rotated helical track hits and fill new collections with missing hits at a given layer
        List<HelicalTrackHit> hits = event.get(HelicalTrackHit.class, rotatedHelicalTrackHitCollectionName);
        
        List<HelicalTrackHit> missingL0TrackerHits = new ArrayList<HelicalTrackHit>();
        List<HelicalTrackHit> missingL1TrackerHits = new ArrayList<HelicalTrackHit>();
        List<HelicalTrackHit> missingL2TrackerHits = new ArrayList<HelicalTrackHit>();
        List<HelicalTrackHit> missingL3TrackerHits = new ArrayList<HelicalTrackHit>();
        List<HelicalTrackHit> missingL4TrackerHits = new ArrayList<HelicalTrackHit>();
        List<HelicalTrackHit> missingL5TrackerHits = new ArrayList<HelicalTrackHit>();
        List<HelicalTrackHit> missingL6TrackerHits = new ArrayList<HelicalTrackHit>();

        for(HelicalTrackHit hit:hits){
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
            int layer = (sensor.getLayerNumber() + 1)/2;
            if(nLay == 7) layer = layer - 1;
            if(layer == 0){
                if(!missingL1TrackerHits.contains(hit)) missingL1TrackerHits.add(hit);
                if(!missingL2TrackerHits.contains(hit)) missingL2TrackerHits.add(hit);
                if(!missingL3TrackerHits.contains(hit)) missingL3TrackerHits.add(hit);
                if(!missingL4TrackerHits.contains(hit)) missingL4TrackerHits.add(hit);
                if(!missingL5TrackerHits.contains(hit)) missingL5TrackerHits.add(hit);
                if(!missingL6TrackerHits.contains(hit)) missingL6TrackerHits.add(hit);
            }
            if(layer == 1){
                if(!missingL0TrackerHits.contains(hit)) missingL0TrackerHits.add(hit);
                if(!missingL2TrackerHits.contains(hit)) missingL2TrackerHits.add(hit);
                if(!missingL3TrackerHits.contains(hit)) missingL3TrackerHits.add(hit);
                if(!missingL4TrackerHits.contains(hit)) missingL4TrackerHits.add(hit);
                if(!missingL5TrackerHits.contains(hit)) missingL5TrackerHits.add(hit);
                if(!missingL6TrackerHits.contains(hit)) missingL6TrackerHits.add(hit);
            }
            if(layer == 2){
                if(!missingL0TrackerHits.contains(hit)) missingL0TrackerHits.add(hit);
                if(!missingL1TrackerHits.contains(hit)) missingL1TrackerHits.add(hit);
                if(!missingL3TrackerHits.contains(hit)) missingL3TrackerHits.add(hit);
                if(!missingL4TrackerHits.contains(hit)) missingL4TrackerHits.add(hit);
                if(!missingL5TrackerHits.contains(hit)) missingL5TrackerHits.add(hit);
                if(!missingL6TrackerHits.contains(hit)) missingL6TrackerHits.add(hit);
            }
            if(layer == 3){
                if(!missingL0TrackerHits.contains(hit)) missingL0TrackerHits.add(hit);
                if(!missingL1TrackerHits.contains(hit)) missingL1TrackerHits.add(hit);
                if(!missingL2TrackerHits.contains(hit)) missingL2TrackerHits.add(hit);
                if(!missingL4TrackerHits.contains(hit)) missingL4TrackerHits.add(hit);
                if(!missingL5TrackerHits.contains(hit)) missingL5TrackerHits.add(hit);
                if(!missingL6TrackerHits.contains(hit)) missingL6TrackerHits.add(hit);
            }
            if(layer == 4){
                if(!missingL0TrackerHits.contains(hit)) missingL0TrackerHits.add(hit);
                if(!missingL1TrackerHits.contains(hit)) missingL1TrackerHits.add(hit);
                if(!missingL2TrackerHits.contains(hit)) missingL2TrackerHits.add(hit);
                if(!missingL3TrackerHits.contains(hit)) missingL3TrackerHits.add(hit);
                if(!missingL5TrackerHits.contains(hit)) missingL5TrackerHits.add(hit);
                if(!missingL6TrackerHits.contains(hit)) missingL6TrackerHits.add(hit);
            }
            if(layer == 5){
                if(!missingL0TrackerHits.contains(hit)) missingL0TrackerHits.add(hit);
                if(!missingL1TrackerHits.contains(hit)) missingL1TrackerHits.add(hit);
                if(!missingL2TrackerHits.contains(hit)) missingL2TrackerHits.add(hit);
                if(!missingL3TrackerHits.contains(hit)) missingL3TrackerHits.add(hit);
                if(!missingL4TrackerHits.contains(hit)) missingL4TrackerHits.add(hit);
                if(!missingL6TrackerHits.contains(hit)) missingL6TrackerHits.add(hit);
            }
            if(layer == 6){
                if(!missingL0TrackerHits.contains(hit)) missingL0TrackerHits.add(hit);
                if(!missingL1TrackerHits.contains(hit)) missingL1TrackerHits.add(hit);
                if(!missingL2TrackerHits.contains(hit)) missingL2TrackerHits.add(hit);
                if(!missingL3TrackerHits.contains(hit)) missingL3TrackerHits.add(hit);
                if(!missingL4TrackerHits.contains(hit)) missingL4TrackerHits.add(hit);
                if(!missingL5TrackerHits.contains(hit)) missingL5TrackerHits.add(hit);
            }
        }
        int flags = 1 << LCIOConstants.TRAWBIT_ID1;
        event.put(tracksMissingL0CollectionName, missingL1TrackerHits, HelicalTrackHit.class, flags);
        event.put(tracksMissingL1CollectionName, missingL1TrackerHits, HelicalTrackHit.class, flags);
        event.put(tracksMissingL2CollectionName, missingL2TrackerHits, HelicalTrackHit.class, flags);
        event.put(tracksMissingL3CollectionName, missingL3TrackerHits, HelicalTrackHit.class, flags);
        event.put(tracksMissingL4CollectionName, missingL4TrackerHits, HelicalTrackHit.class, flags);
        event.put(tracksMissingL5CollectionName, missingL5TrackerHits, HelicalTrackHit.class, flags);
        event.put(tracksMissingL6CollectionName, missingL6TrackerHits, HelicalTrackHit.class, flags);
    }
}