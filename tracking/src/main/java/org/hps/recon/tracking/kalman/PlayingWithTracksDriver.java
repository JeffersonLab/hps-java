/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.recon.tracking.kalman;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrack2DHit;
import org.lcsim.fit.helicaltrack.HelicalTrack3DHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.util.Driver;

/**
 *
 * @author ecfine
 */
public class PlayingWithTracksDriver extends Driver{

    public PlayingWithTracksDriver() {
    }

    
    public void process(EventHeader event){
        
        System.out.println("event number " + event.getEventNumber());
        System.out.println("event detector name: " +event.getDetectorName());
        List<MCParticle> mcparticles = event.getMCParticles();
        System.out.println("number of mcparticles: " + mcparticles.size());
        MCParticle firstParticle = mcparticles.get(0);
        System.out.println("energy of first particle: " + firstParticle.getEnergy());
        if (event.hasItem("MatchedTracks")) {
            List<Track> trklist = (List<Track>) event.get("MatchedTracks");
            System.out.println("number of tracks: " + trklist.size());
            for (int i = 0; i < trklist.size(); i ++){
                Track trk = trklist.get(i);
                for (int k = 0; k < trk.getTrackerHits().size(); k++){
                    TrackerHit hit = (TrackerHit) trk.getTrackerHits().get(k);
                    if (hit instanceof HelicalTrack2DHit){
                        System.out.println("HelicalTrack2DHit");
                    } else
                    if (hit instanceof HelicalTrack3DHit){
                        System.out.println("HelicalTrack3DHit");
                    } else if(hit instanceof HelicalTrackCross){
                        System.out.println("HelicalTrackCross");
                    } else
                    if (hit instanceof HelicalTrackHit){
                        System.out.println("HelicalTrackHit");
                    } else {
                        System.out.println("Don't know this kind of track...");
                    }
                }
            }
        } else {
            System.out.println("No tracks!");
        }
    }
}
