package org.hps.users.spaul;

import java.util.List;

import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;

public class BestTrack {
	static boolean shareHits(Track t1, Track t2){
		for(TrackerHit hit : t1.getTrackerHits()){
			for(TrackerHit hit2 : t2.getTrackerHits()){
				if(equals(hit, hit2))
					return true;
			}
		}
		return false;
	}
	
	static double threshold = 3;
	static boolean equals(TrackerHit hit1, TrackerHit hit2){
		return Math.hypot(hit1.getPosition()[0]-hit2.getPosition()[0],
					hit1.getPosition()[1]-hit2.getPosition()[1])< threshold;
	}
	
}
