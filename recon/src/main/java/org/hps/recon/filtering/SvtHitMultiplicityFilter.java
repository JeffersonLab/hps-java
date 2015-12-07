/**
 * 
 */
package org.hps.recon.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.event.EventHeader;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;

/**
 * 
 * Filter events based on max nr of strip hits
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtHitMultiplicityFilter extends EventReconFilter {

     private final static String stripClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private int hitsPerSensor = 1;
    
    
    @Override
    protected void process(EventHeader event) {
     
        incrementEventProcessed();
        
        // Make sure there are any strips in the event
        if(!event.hasCollection(SiTrackerHitStrip1D.class, stripClusterCollectionName))
            skipEvent();
        
        // Find all strip clusters in the events
        List<SiTrackerHitStrip1D> stripClusters = event.get(SiTrackerHitStrip1D.class, stripClusterCollectionName);
        
        
        Map<String, List<SiTrackerHitStrip1D> > sensorHitMap= new HashMap< String, List<SiTrackerHitStrip1D> >();
        
        for(SiTrackerHitStrip1D cluster : stripClusters) {
            String sensorName = cluster.getRawHits().get(0).getDetectorElement().getName();
            
            List<SiTrackerHitStrip1D> hits = sensorHitMap.get(sensorName);
            if(hits == null) {
                hits = new ArrayList<SiTrackerHitStrip1D>();
            }
            hits.add(cluster);
        }
        
        
        // go through and check that the number of hits for each layer is what's required
        for(Map.Entry<String, List<SiTrackerHitStrip1D>> entry : sensorHitMap.entrySet()) {
            if( entry.getValue().size() != hitsPerSensor) 
                skipEvent();
        }




        
        incrementEventPassed();
        
    }
    
    public void setHitsPerSensor(int hitsPerSensor) {
        this.hitsPerSensor = hitsPerSensor;
    }


       
}
