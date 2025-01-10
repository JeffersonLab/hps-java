package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.event.Track;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.util.Driver;
import org.lcsim.event.ReconstructedParticle;
/**
 *
 * @author mgraham created 1/29/24
 * 
 * Extension of SlopeBasedTrackHitKiller to work on Kalman Tracks
 * This driver will remove 1d strip clusters from the
 * "StripClusterer_SiTrackerHitStrip1D" (default)
 * collection based on a track-slope efficiency file (obtained from L1/no L1 WAB events)
 * mg...this only works for L1 module at the moment
 * mg...the official hit killing as of 2024 is StripHitKiller.java...
 * mg...this is just included for posterity
 * 
 */
public class KalmanSlopeBasedTrackHitKiller extends SlopeBasedTrackHitKiller {
   
    String trackCollectionName = "KalmanFullTracks";


    public KalmanSlopeBasedTrackHitKiller() {
    }

    @Override
    public void process(EventHeader event) {
        
        if (event.hasItem(stripHitInputCollectionName))
            siClusters = (List<TrackerHit>) event.get(stripHitInputCollectionName);
        else {
            System.out.println("KalmanSlopeBasedTrackHitKiller::process No Input Collection Found?? " + stripHitInputCollectionName);
            return;
        }
              
        List<Track> tracks = event.get(Track.class, trackCollectionName);                
        Map<Track,Double> trkNewSlopeMap = new HashMap<Track, Double>();

        if (_correctForDisplacement) {
            if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName)) {
                if (_debug)
                    System.out.println("KalmanSlopeBasedTrackHitKiller::process No Input Collection Found?? " + unconstrainedV0CandidatesColName);
                return;
            }
            List<ReconstructedParticle> unconstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
            if (_debug)
                System.out.println("This events has " + unconstrainedV0List.size() + " unconstrained V0s");
            trkNewSlopeMap = getUniqueTracksFromV0List(unconstrainedV0List);
            System.out.println("# of tracks in map = " + trkNewSlopeMap.size());

        }

        List<TrackerHit> tmpClusterList = new ArrayList<TrackerHit>(siClusters);
        int oldClusterListSize = siClusters.size();

        for (TrackerHit siCluster : siClusters) {
            for (ModuleSlopeMap modToKill : _modulesToKill) {
                if (modToKill.getLayer() != layerToModule(((RawTrackerHit) siCluster.getRawHits().get(0)).getLayerNumber()))
                    continue;
                double lambda = -666;
                if (_correctForDisplacement) {
                    lambda = adjustedSlopeFromMap(trkNewSlopeMap,siCluster);
                    if (_debug)
                        System.out.println("corrected lambda= "+lambda);
                }else{
                    Track trk=getTrackWithHit(tracks,siCluster);
                    if (trk==null)
                        continue;
                    lambda = trk.getTrackStates().get(0).getTanLambda(); 
                }

               
                double ratio = modToKill.getInefficiency(lambda);
                if (ratio == -666)
                    continue;
                if (_useSqrtKillFactor) {
                    double eff = Math.sqrt(1-ratio);
                    ratio = 1-eff;
                }
                double killFactor = ratio*modToKill.getScaleKillFactor();
                double random = Math.random(); //throw a random number to see if this hit should be rejected
                if (_debug)
                    System.out.println("ratio = " + ratio + "; killFactor = " + killFactor + "; random # = " + random);
                if (random < killFactor) {                
                    tmpClusterList.remove(siCluster);
                }
            }
        }

        if (_debug) {
            System.out.println("New Cluster List Has " + tmpClusterList.size() + "; old List had " + oldClusterListSize);
            System.out.println("");
        }
        int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.        
        event.put(this.stripHitInputCollectionName, tmpClusterList, SiTrackerHitStrip1D.class, 0, toString());
        if (_debug)
            System.out.println("Clearing hit relational table caches");
        TrackUtils.clearCaches();

    }

    @Override
    public void endOfData() {
      
    }
    /*
     * mg...7/5/20...
     * the strips in the SiCluster list are type: org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D
     * while from the track list (and presumably linked in HTH) are: org.hps.recon.tracking.SiTrackerHitStrip1D
     * and they apparently are actually separate in memory...
     * ...just checking if the SiCluster TrackerHit "hit" is in the hitsontrack collection doesn't work..
     * that's why I go back to the raw hits and compare these lists.  
     */
    public Track getTrackWithHit(List<Track> tracks, TrackerHit hit) {
        for(Track trk: tracks) {
        List<TrackerHit> hitsontrack=trk.getTrackerHits();      
            for (TrackerHit hot: hitsontrack) {
                List<RawTrackerHit> rawTrkHits = (List<RawTrackerHit>)( hot.getRawHits());
                List<RawTrackerHit> rawHitHits = (List<RawTrackerHit>)( hit.getRawHits());
                if (rawHitHits.equals(rawTrkHits)) {
                    if (_debug)
                        System.out.println("found match to SiCluster in track");
                    return trk;
                }
            }
        }                
        return null;
    }
   
    public double adjustedSlopeFromMap(Map<Track,Double> trkmap, TrackerHit hit) {
        
        for (Map.Entry<Track,Double> entry : trkmap.entrySet())  {
            Track trk = entry.getKey();
            double newSlope = entry.getValue();
            List<TrackerHit> hitsontrack = trk.getTrackerHits();
            for (TrackerHit hot: hitsontrack) {
                List<RawTrackerHit> rawTrkHits = (List<RawTrackerHit>)(hot.getRawHits());
                List<RawTrackerHit> rawHitHits = (List<RawTrackerHit>)(hit.getRawHits());
                if (rawHitHits.equals(rawTrkHits)) {
                    if (_debug)
                        System.out.println("found match to SiCluster in track");
                    return newSlope;
                }
            }
        }                
        return -666;
    }
}
