package org.hps.users.omoreno;

import java.util.HashMap;
import java.util.List; 
import java.util.ArrayList; 
import java.util.Map;

import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.util.Driver;

/**
 * Driver used to relate a Track to an Ecal scoring plane hit.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 */
public class EcalScoringPlaneDriver extends Driver {

	boolean verbose = false;
	
	// Collection Names
	String ecalScoringPlaneHitsCollectionName = "TrackerHitsECal";
	String tracksCollectionName = "MatchedTracks";
	String trackToScoringPlaneHitRelationsName = "TrackToEcalScoringPlaneHitRelations";
	String trackToMCParticleRelationsName = "TrackToMCParticleRelations";
	
	/**
	 * Enable/disable verbose mode
	 * 
	 * @param verbose : set true to enable, false otherwise
	 */
	public void setVerbose(boolean verbose){
		this.verbose = verbose; 
	}
	
	@Override
	protected void process(EventHeader event){
		
		// If the event doesn't have a collection of Tracks, skip it
		if(!event.hasCollection(Track.class, tracksCollectionName)) return;
		
		// If the event doesn't have a collection of Ecal scoring plane hits, 
		// skip it
		if(!event.hasCollection(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName)) return;
		
		// Get the collection of tracks from the event
		List<Track> tracks = event.get(Track.class, tracksCollectionName);
		
		// Get the collection of Ecal scoring plane hits from the event
		List<SimTrackerHit> scoringPlaneHits = event.get(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName);
		
		// Create a collection to hold the scoring plane hits that were found to match
		// a track
		List<SimTrackerHit> matchedScoringPlaneHits = new ArrayList<SimTrackerHit>(); 
		
		// Create a collection of LCRelations between a track and the scoring plane hit
		List<LCRelation> trackToScoringPlaneHitRelations = new ArrayList<LCRelation>();
	
		// Create a collection of LCRelations between a track and its corresponding MC particle
		List<LCRelation> trackToMCParticleRelations = new ArrayList<LCRelation>();
	
		MCParticle particle = null;
		for(Track track : tracks){
		
			// Get the MC particle associated with this track
			particle = this.getMCParticleAssociatedWithTrack(track);
			// If the MC particle is null, then the hits associated with the
			// track did not have an MC particle associated with them
			// TODO: Find out why some hits don't have any MC particles associated with them
			if(particle == null) continue;
		
			// Add an LCRelation between the track and the corresponding MC particle
			trackToMCParticleRelations.add(new BaseLCRelation(track, particle));
			
			// Loop over all of the scoring plane hits and check if the associated MC particle
			// matches the one from the track
			for(SimTrackerHit scoringPlaneHit : scoringPlaneHits){
	
				// If the MC particles don't match, move on to the next particle
				if(!(scoringPlaneHit.getMCParticle() == particle)) continue; 
					
				this.printVerbose("Found a match between a track and a scoring plane hit.");
				
				// If a match is found, add the scoring plane hit to the list of matched hits and
				// an LCRelation between the track and the scoring plane.
				matchedScoringPlaneHits.add(scoringPlaneHit);
				trackToScoringPlaneHitRelations.add(new BaseLCRelation(track, scoringPlaneHit));
				
				// Once a match is found, there is no need to loop through the rest of the list
				break;
			}
		}
		
		// Store all of the collections in the event
		event.put(ecalScoringPlaneHitsCollectionName, matchedScoringPlaneHits, SimTrackerHit.class, 0);
		event.put(trackToScoringPlaneHitRelationsName, trackToScoringPlaneHitRelations, LCRelation.class, 0);
		event.put(trackToMCParticleRelationsName, trackToMCParticleRelations, LCRelation.class, 0);
	}
	
	/**
	 * Print a message if verbose has been enabled.
	 *  
	 * @param message : message to print.
	 */
	private void printVerbose(String message){
		if(verbose)
			System.out.println(this.getClass().getSimpleName() + ": " + message);
	}
	
	/**
	 * Get the MC particle associated with a track.
	 * 
	 * @param track : Track to get the MC particle for
	 * @return The MC particle associated with the track
	 */
	private MCParticle getMCParticleAssociatedWithTrack(Track track){
		
		Map <MCParticle, int[]>mcParticleMultiplicity = new HashMap<MCParticle, int[]>();
		MCParticle particle;
		for(TrackerHit hit : track.getTrackerHits()){
		
				// If one of the tracker hits doesn't have any MC particles associated
				// with it, return null for now.
				if(((HelicalTrackHit) hit).getMCParticles().size() == 0){
					this.printVerbose("HelicalTrackHit is not associated with any MC particles.");
					return null;
				}
				
				particle = ((HelicalTrackHit) hit).getMCParticles().get(0);
				if(!mcParticleMultiplicity.containsKey(particle)){
					mcParticleMultiplicity.put(particle, new int[1]);
					mcParticleMultiplicity.get(particle)[0] = 0;
				}
				
				mcParticleMultiplicity.get(particle)[0]++;
				
		}
		
		// Look for the MC particle that occurs the most of the track
		int maxValue = 0;
		particle = null;
		for(Map.Entry<MCParticle, int[]> entry : mcParticleMultiplicity.entrySet()){
			if(maxValue < entry.getValue()[0]){
				particle = entry.getKey();
				maxValue = entry.getValue()[0];
			}
		}
		
		return particle;
	}
}
