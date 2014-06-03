package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

import org.hps.recon.tracking.TrackTimeData;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 * 
 */
public class TrackDataDriver extends Driver {

	// Collection Names
	String trackCollectionName = "MatchedTracks";
	// TODO: Change this to match whatever track name is decided on
	String trackTimeDataCollectionName = "TrackTimeData";

	public TrackDataDriver() {}

	protected void detectorChanged(Detector detector){
		
		// TODO: Add plots of all track data variables
		
	}
	
	protected void process(EventHeader event) {

		// If the event doesn't contain a collection of tracks, skip it.
		if (!event.hasCollection(Track.class, trackCollectionName))
			return;

		// Get the collection of tracks from the event
		List<Track> tracks = event.get(Track.class, trackCollectionName);

		// Create a collection to hold the track time and t0 residual data
		List<TrackTimeData> timeDataCollection = new ArrayList<TrackTimeData>(); 
		
		double totalT0 = 0;
		double totalHits = 0;
		double trackTime = 0;
		double t0Residual = 0;
		float trackerVolume = -1; 
		int layer;
		
		
		boolean isFirstHit = true;
		
		HpsSiSensor sensor = null; 
		TrackTimeData timeData = null; 
		
		List<Double> t0Residuals = new ArrayList<Double>(); 
		List<Integer> layers = new ArrayList<Integer>();
		
		// Loop over all the tracks in the event
		for (Track track : tracks) {
			
			totalT0 = 0; 
			totalHits = 0;
			t0Residuals.clear();
			layers.clear();
			
			//
			// Calculate the track time
			//
			
			// Loop over all stereo hits comprising a track
			for (TrackerHit stereoHit : track.getTrackerHits()) {
				// Loop over the clusters comprising the stereo hit
				for(HelicalTrackStrip cluster : ((HelicalTrackCross) stereoHit).getStrips()){
					
					totalT0 += cluster.time();
					totalHits++;
				}
			}
			
			// The track time is the mean t0 of hits on a track
			trackTime = totalT0/totalHits;
	
			
			//
			// Calculate the t0 residuals
			//
			
			isFirstHit = true;
			// Loop over all stereo hits comprising a track
			for (TrackerHit stereoHit : track.getTrackerHits()) {
				// Loop over the clusters comprising the stereo hit
				for(HelicalTrackStrip cluster : ((HelicalTrackCross) stereoHit).getStrips()){

					if(isFirstHit){
						sensor = (HpsSiSensor) ((RawTrackerHit) cluster.rawhits().get(0)).getDetectorElement(); 
						if(sensor.isTopLayer()) trackerVolume = 0; 
						else if(sensor.isBottomLayer()) trackerVolume = 1;
					}
				
					// Add the layer number associated with this residual to the list of layers
					layers.add(sensor.getLayerNumber()); 
					
					// Find the t0 residual and add it to the list of residuals
					t0Residual = trackTime - cluster.time(); 
					// Apply correction to t0 residual
					t0Residual /= Math.sqrt((totalHits - 1)/totalHits);
					t0Residuals.add(t0Residual);
				}
			}
		
			timeDataCollection.add(new TrackTimeData(trackerVolume, trackTime, layers, t0Residuals));
		}
		
		event.put(trackTimeDataCollectionName, timeDataCollection, TrackTimeData.class, 0);
	}
}
