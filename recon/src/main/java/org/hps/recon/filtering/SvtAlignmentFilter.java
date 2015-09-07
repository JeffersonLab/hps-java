/**
 * 
 */
package org.hps.recon.filtering;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.TrackUtils;
import org.hps.util.BasicLogFormatter;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.log.LogUtil;

/**
 * 
 * Filter events to be used for SVT alignment.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtAlignmentFilter extends EventReconFilter {

    
//    private static Logger logger = LogUtil.create(SvtAlignmentFilter.class.getSimpleName(), new BasicLogFormatter(), Level.INFO);
//    private int minRawHits = 12;
//    private int minStripClustersPerSensor = 1;
//    private int maxStripClustersPerSensor = 9999999;
//    private int minStripClustersInHalf = 12;
//    private int maxStripClustersInHalf = 9999999;
//    private int minEcalClustersInHalf = -1;
//    private int maxEcalClustersInHalf = 99999999;
//    private Double minStripIsolation = 0.2;
    
    
    @Override
    protected void process(EventHeader event) {
     
        incrementEventProcessed();
        
        if(!event.hasCollection(Track.class,"MatchedTracks")) skipEvent();
        
        if(!event.hasCollection(Cluster.class, "EcalClusters")) skipEvent();
        
        List<Track> tracks = event.get(Track.class, "MatchedTracks");
        List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
        
        // require track match
        List<Track> selectedTracks = new ArrayList<Track>();

        for(Track track : tracks) {
            Hep3Vector posAtEcal = TrackUtils.getTrackPositionAtEcal(tracks.get(0));
            Cluster cand_clust = findClosestCluster(posAtEcal, clusters);
            if(cand_clust!=null) {
                if(Math.abs( posAtEcal.x() - cand_clust.getPosition()[0])<30.0 && 
                        Math.abs( posAtEcal.y() - cand_clust.getPosition()[1])<30.0) 
                {
                    selectedTracks.add(track);
                }
            }
        }

        // remove old track collection
        event.remove("MatchedTracks");
        
        // Put the tracks back into the event
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put("MatchedTracks", selectedTracks, Track.class, flag);

        
        
        
        /*
                
        if (event.hasCollection(Cluster.class, "EcalClusters")) {
            List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
            int top = 0;
            int bottom = 0;
            for (Cluster cluster : clusters) {
                if (cluster.getPosition()[1] > 0) top++;
                else bottom++;
            }
            
            boolean passTop = true;
            if(top < minEcalClustersInHalf || top > maxEcalClustersInHalf) {
                passTop = false;
            }
            boolean passBottom = true;
            if(bottom < minEcalClustersInHalf || bottom > maxEcalClustersInHalf) {
                passBottom = false;
            }
            
            logger.info(Integer.toString(bottom) + "(" + Integer.toString(top) + ")" + " ecal clusters in bottom (top)");
            
            if( !passTop ) skipEvent();
            
            if( !passBottom ) {
                skipEvent();
            }
            
            
        } else {
                logger.info("no ECal clusters in event");
            
            skipEvent();
        }
        
        logger.info("Passed ecal cluster selection");
        
        if(event.hasCollection(RawTrackerHit.class, "SVTRawTrackerHits")) {
            
            List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
                
            logger.info("Event has " + Integer.toString(rawHits.size()) + " SVTRawTrackerHits");
            
            if( rawHits.size() < minRawHits) {
                skipEvent();
            }
            
        } else {
            
            logger.info("no SVTRawTrackerHits in event");
            
            skipEvent();
        }

        logger.info("Passed raw tracker hit selection");
        
        
        
        
        
        if(event.hasCollection(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D")) {
            
            List<SiTrackerHitStrip1D> stripClusters = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
  
            logger.fine("Event has " + Integer.toString(stripClusters.size()) + " StripClusterer_SiTrackerHitStrip1D");

            Map<HpsSiSensor, Integer> nClustersPerSensor = new HashMap<HpsSiSensor , Integer>();
            Map<HpsSiSensor, Double> stripHitsIso = new HashMap<HpsSiSensor , Double>();
            int top = 0;
            int bottom = 0;
            for(SiTrackerHitStrip1D stripCluster : stripClusters) {
                HpsSiSensor sensor = (HpsSiSensor) stripCluster.getSensor();
                if(!nClustersPerSensor.containsKey(sensor)) {
                    nClustersPerSensor.put(sensor, 1);
                } 
                nClustersPerSensor.put(sensor, nClustersPerSensor.get(sensor)+1);
                logger.fine("strip on " + sensor.getName() + " with " + Integer.toString(stripCluster.getRawHits().size()) + " raw hits");
                if(sensor.isTopLayer()) top++;
                else bottom++;
                
                
                SiTrackerHitStrip1D local = stripCluster.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                
                double stripIsoMin = 9999.9;
                for (SiTrackerHitStrip1D stripHitOther : stripClusters) {
                    if(stripHitOther.equals(stripCluster)) {
                        continue;
                    }
                    
                    HpsSiSensor sensorOther = (HpsSiSensor) stripHitOther.getRawHits().get(0).getDetectorElement();
                    //System.out.println(sensor.getName() + " c.f. " + sensorOther.getName());
                    if(sensorOther.equals(sensor)) {
                        SiTrackerHitStrip1D localOther = stripHitOther.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                        double d = Math.abs(local.getPosition()[0] - localOther.getPosition()[0]);
                        //System.out.println(sensor.getName() + " d " + Double.toString(d));
                        if (d < stripIsoMin && d > 0) {
                            stripIsoMin = d;
                        }
                    }
                }
                stripHitsIso.put(sensor, stripIsoMin);
                
                
            }
            boolean passTop = true;
            if(top < minStripClustersInHalf || top > maxStripClustersInHalf) {
                passTop = false;
            }
            boolean passBottom = true;
            if(bottom < minStripClustersInHalf || bottom > maxStripClustersInHalf) {
                passBottom = false;
            }
            
            logger.info(Integer.toString(bottom) + "(" + Integer.toString(top) + ")" + " strip clusters in bottom (top)");
            
            if( !passTop && !passBottom ) {
                skipEvent();
            }
            
            for(Map.Entry<HpsSiSensor, Integer> entry : nClustersPerSensor.entrySet()) {
                int n = entry.getValue();
                if(n < minStripClustersPerSensor || n > maxStripClustersPerSensor) {
                    skipEvent();
                }
            
                if(stripHitsIso.get(entry.getKey()) < minStripIsolation ) {
                    skipEvent();
                }
            
            }
            
            
            
            
            
            
            
        } else {

            logger.info("no StripClusterer_SiTrackerHitStrip1D in event");

            skipEvent();
        }
        
        logger.info("Passed cluster hit selection");
        */
        incrementEventPassed();
        
    }
    
    
    private Cluster findClosestCluster(Hep3Vector posonhelix, List<Cluster> clusters) {
        Cluster closest = null;
        double minDist = 9999;
        for (Cluster cluster : clusters) {
            double[] clPos = cluster.getPosition();
            double clEne = cluster.getEnergy();
            double dist = Math.sqrt(Math.pow(clPos[0] - posonhelix.x(), 2) + Math.pow(clPos[1] - posonhelix.y(), 2)); //coordinates!!!
            if (dist < minDist && clEne > 0.4) {
                closest = cluster;
                minDist = dist;
            }
        }
        return closest;
    }
       
}
