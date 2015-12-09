/**
 * 
 */
package org.hps.recon.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
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

    Logger logger = Logger.getLogger(SvtHitMultiplicityFilter.class.getSimpleName());
    private final static String stripClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private int hitsPerSensor = 1;
    private int minHitsPerHalf = 3;
        
    public SvtHitMultiplicityFilter() {
       logger.setLevel(Level.WARNING);
    }
    
    @Override
    protected void process(EventHeader event) {
     
        incrementEventProcessed();
        
        // Make sure there are any strips in the event
        if(!event.hasCollection(SiTrackerHitStrip1D.class, stripClusterCollectionName))
            skipEvent();
        
        // Find all strip clusters in the events
        List<SiTrackerHitStrip1D> stripClusters = event.get(SiTrackerHitStrip1D.class, stripClusterCollectionName);
        
        
        Map<String, List<SiTrackerHitStrip1D> > sensorHitMap= new HashMap< String, List<SiTrackerHitStrip1D> >();
        
        int nhits[] = {0,0};

        for(SiTrackerHitStrip1D cluster : stripClusters) {
            String sensorName = cluster.getRawHits().get(0).getDetectorElement().getName();
            boolean isTop = ((HpsSiSensor) cluster.getRawHits().get(0).getDetectorElement()).isTopLayer();
            if(isTop)
                nhits[0]++;
            else
                nhits[1]++;
            
            List<SiTrackerHitStrip1D> hits;
            if(sensorHitMap.containsKey(sensorName))
                hits = sensorHitMap.get(sensorName);
            else {
                hits = new ArrayList<SiTrackerHitStrip1D>();
                sensorHitMap.put(sensorName, hits);
            }
            hits.add(cluster);
        }
        
        // if none of the halves contains the required nr of hits, skip the event
        if( nhits[0] < minHitsPerHalf && nhits[1] < minHitsPerHalf) 
            skipEvent();
        
        // go through and check that the number of hits for each layer is what's required
        for(Map.Entry<String, List<SiTrackerHitStrip1D>> entry : sensorHitMap.entrySet()) {
            if( entry.getValue().size() != hitsPerSensor) 
                skipEvent();
        }


        
        StringBuffer sb = new StringBuffer();
        sb.append("Event with " + stripClusters.size() + " hits passed:\n");
        for(SiTrackerHitStrip1D hit : stripClusters) {
            sb.append(hit.getPositionAsVector().toString() + " " + hit.getRawHits().get(0).getDetectorElement().getName() + "\n");
        }
        logger.info(sb.toString());
        
        
        incrementEventPassed();
        
    }
    
    public void setHitsPerSensor(int hitsPerSensor) {
        this.hitsPerSensor = hitsPerSensor;
    }


       
}
