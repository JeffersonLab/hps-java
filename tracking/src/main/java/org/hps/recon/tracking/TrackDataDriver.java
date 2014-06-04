package org.hps.recon.tracking;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
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
	String trackResidualsCollectionName = "TrackResiduals";

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
		
		// Create a collection to hold the track residuals
		List<TrackResidualsData> trackResidualsCollection = new ArrayList<TrackResidualsData>();
		
		double totalT0 = 0;
		double totalHits = 0;
		double trackTime = 0;
		double t0Residual = 0;
		double xResidual = 0;
		double yResidual = 0;
		float trackerVolume = -1; 
		
		
		boolean isFirstHit = true;
		
		HpsSiSensor sensor = null; 
		Hep3Vector stereoHitPosition = null;
		Hep3Vector trackPosition = null;
		
		List<Double>  t0Residuals = new ArrayList<Double>(); 
		List<Double>  trackResidualsX = new ArrayList<Double>();
		List<Float>   trackResidualsY = new ArrayList<Float>();
		List<Integer> sensorLayers = new ArrayList<Integer>();
		List<Integer> stereoLayers = new ArrayList<Integer>(); 
		
		
		// Loop over all the tracks in the event
		for (Track track : tracks) {
						
			totalT0 = 0; 
			totalHits = 0;
			t0Residuals.clear();
			sensorLayers.clear();
			trackResidualsX.clear();
			trackResidualsY.clear();
			stereoLayers.clear();
			
			//
			// Calculate the track time and track residuals
			//
			
			// Loop over all stereo hits comprising a track
			for (TrackerHit stereoHit : track.getTrackerHits()) {
			
				// Add the stereo layer number associated with the track residual
				stereoLayers.add(((HelicalTrackHit) stereoHit).Layer());
				
				// Extrapolate the track to the stereo hit position and calculate 
				// track residuals
				stereoHitPosition = ((HelicalTrackHit) stereoHit).getCorrectedPosition();
				System.out.println("Stereo Hit Position: " + stereoHitPosition.toString());
				trackPosition = TrackUtils.extrapolateTrack(track, stereoHitPosition.x());
				System.out.println("Track Position: " + trackPosition.toString());
				xResidual = trackPosition.x() - stereoHitPosition.y();
				yResidual = trackPosition.y() - stereoHitPosition.z(); 
				trackResidualsX.add(xResidual);
				trackResidualsY.add((float) yResidual);
				
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
					sensorLayers.add(sensor.getLayerNumber()); 
					
					// Find the t0 residual and add it to the list of residuals
					t0Residual = trackTime - cluster.time(); 
					// Apply correction to t0 residual
					t0Residual /= Math.sqrt((totalHits - 1)/totalHits);
					t0Residuals.add(t0Residual);
				}
			}
		
			timeDataCollection.add(new TrackTimeData(trackerVolume, trackTime, sensorLayers, t0Residuals));
			trackResidualsCollection.add(new TrackResidualsData((int) trackerVolume, stereoLayers, trackResidualsX, trackResidualsY));
		
		}
		
		event.put(trackTimeDataCollectionName, timeDataCollection, TrackTimeData.class, 0);
		event.put(trackResidualsCollectionName, trackResidualsCollection, TrackResidualsData.class, 0);
	}
}
