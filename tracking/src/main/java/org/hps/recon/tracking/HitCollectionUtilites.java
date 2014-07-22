package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;

/**
 * Some methods which manipulate hit collection in various ways
 * --right now, uses HelicalTrackHits everywhere since I plan to
 * use it for tracking...probably can use TrackerHits to make
 * it more general
 * @author mgraham  -- data created 7/9/2014
 */
public class HitCollectionUtilites {
    /* return a list of hits sorted by there distance from the beam (in y) */

    public static List<HelicalTrackHit> SortHitsByDistanceFromBeam(List<HelicalTrackHit> hits) {
        List<HelicalTrackHit> sortedHits = new ArrayList<>();
        while (!hits.isEmpty()) {
            double minPosition = 999;
            HelicalTrackHit minHit = null;
            for (HelicalTrackHit hit : hits)
                if (Math.abs(hit.getCorrectedPosition().y()) < minPosition) {
                    minHit = hit;
                    minPosition = hit.getCorrectedPosition().y();
                }
            if (minHit != null) {
                sortedHits.add(minHit);
                hits.remove(minHit);
            }
        }
        return sortedHits;
    }

    /*
     *  split the hit collection into top and bottom hit collections    
     */
    public static List<List<HelicalTrackHit>> SplitTopBottomHits(List<HelicalTrackHit> hits) {
        List<List<HelicalTrackHit>> splitLists = new ArrayList<>();
        List<HelicalTrackHit> topHits = new ArrayList<>();
        List<HelicalTrackHit> bottomHits = new ArrayList<>();
        for (HelicalTrackHit hit : hits)
            if (hit.getPosition()[1] > 0)
                topHits.add(hit);
            else
                bottomHits.add(hit);
        splitLists.add(topHits);
        splitLists.add(bottomHits);
        
        return splitLists;
    }

    /*
     *  get the hits from a certain layer    
     */
    public static List<HelicalTrackHit> GetHitsFromLayer(List<HelicalTrackHit> hits, int nlayer) {
        List<HelicalTrackHit> hitList = new ArrayList<>();
        for (HelicalTrackHit hit : hits)           
            if (hit.Layer() == nlayer)
                hitList.add(hit);
        return hitList;
    }
    
     /*
     *  get the hits from a certain layer sorted by distance from beamline  
     */
    public static List<HelicalTrackHit> GetSortedHits(List<HelicalTrackHit> hits, int nlayer) {
       return SortHitsByDistanceFromBeam(GetHitsFromLayer(hits,nlayer));
    }
    
}
