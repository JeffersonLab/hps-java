package org.hps.users.kmccarty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;

public class TrackableParticleSelectionDriver extends Driver {
	private String svtHitCollectionName = "TrackerHits";
	private String particleCollectionName = "MCParticles";
	private String outputCollectionName = "TrackableMCParticles";
	
	@Override
	public void process(EventHeader event) {
		// Get the list of SVT hits. If there are none, just add an
		// empty "trackable particles" list to the event.
		List<SimTrackerHit> svtHits = null;
		if(event.hasCollection(SimTrackerHit.class, svtHitCollectionName)) {
			svtHits = event.get(SimTrackerHit.class, svtHitCollectionName);
		} else {
			event.put(outputCollectionName, new ArrayList<MCParticle>(0), MCParticle.class, 1);
			return;
		}
		
		// Get the list of MCParticles. If there are none, just add
		// an empty "trackable particles" list to the event.
		List<MCParticle> particles = null;
		if(event.hasCollection(MCParticle.class, particleCollectionName)) {
			particles = event.get(MCParticle.class, particleCollectionName);
		} else {
			event.put(outputCollectionName, new ArrayList<MCParticle>(0), MCParticle.class, 1);
			return;
		}
		
		// Iterate over the SVT hits and track whether a particle has
		// passed through a given layer of the SVT.
		Map<MCParticle, boolean[]> layerMap = new HashMap<MCParticle, boolean[]>();
		for(SimTrackerHit svtHit : svtHits) {
			if(layerMap.containsKey(svtHit.getMCParticle())) {
				boolean[] layers = layerMap.get(svtHit.getMCParticle());
				layers[getSVTLayer(svtHit) - 1] = true;
			} else {
				boolean[] layers = new boolean[6];
				layers[getSVTLayer(svtHit) - 1] = true;
				layerMap.put(svtHit.getMCParticle(), layers);
			}
		}
		
		// Create a list of only those particles that pass through at
		// least 5 layers of the SVT.
		//List<MCParticle> trackableParticles = new ArrayList<MCParticle>();
		List<MCParticle> trackableParticles = new ArrayList<MCParticle>();
		for(MCParticle particle : particles) {
			if(layerMap.containsKey(particle)) {
				int layersTraversed = getInstancesTrue(layerMap.get(particle));
				if(layersTraversed >= 5) { trackableParticles.add(particle); }
			} else {
				continue;
			}
		}
		
		/*
		if(!trackableParticles.isEmpty()) {
			System.out.print("Event " + event.getEventNumber() + " has " + trackableParticles.size() + " trackable particle");
			if(trackableParticles.size() == 1) { System.out.println("."); }
			else { System.out.println("s."); }
		}
		*/
		
		// Put the list of trackable particles into the data stream.
		event.put(outputCollectionName, trackableParticles, MCParticle.class, 1);
	}
	
	private static final int getInstancesTrue(boolean[] array) {
		int veritates = 0;
		for(boolean b : array) {
			if(b) { veritates++; }
		}
		return veritates;
	}
	
	private static final int getSVTLayer(SimTrackerHit svtHit) {
		if(svtHit.getLayer() == 1 || svtHit.getLayer() == 2) {
			return 1;
		} else if(svtHit.getLayer() == 3 || svtHit.getLayer() == 4) {
			return 2;
		} else if(svtHit.getLayer() == 5 || svtHit.getLayer() == 6) {
			return 3;
		} else if(svtHit.getLayer() == 7 || svtHit.getLayer() == 8) {
			return 4;
		} else if(svtHit.getLayer() == 9 || svtHit.getLayer() == 10) {
			return 5;
		} else if(svtHit.getLayer() == 11 || svtHit.getLayer() == 12) {
			return 6;
		} else {
			return -1;
		}
	}
	
	public void setOutputCollectionName(String collection) {
		outputCollectionName = collection;
	}
	
	public void setSvtHitCollectionName(String collection) {
		svtHitCollectionName = collection;
	}
	
	public void setParticleCollectionName(String collection) {
		particleCollectionName = collection;
	}
}