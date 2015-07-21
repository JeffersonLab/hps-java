package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class TridentTrackDriver extends Driver {
	private String finalStateCollectionName = "FinalStateParticles";
	private String candidateCollectionName = "UnconstrainedV0Candidates";
	
	private int tracksCandidate = 0;
	private int tracksFinalState = 0;
	private int tracksCandidateCluster = 0;
	private int tracksFinalStateCluster = 0;
	
	private static final int ANY_CLUSTER = 0;
	private static final int HAS_CLUSTER = 1;
	
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D[] tracks = new IHistogram1D[2];
	private IHistogram1D[] posTracks = new IHistogram1D[2];
	private IHistogram1D[] negTracks = new IHistogram1D[2];
	private IHistogram1D[] posMomentum = new IHistogram1D[2];
	private IHistogram1D[] negMomentum = new IHistogram1D[2];
	private IHistogram1D[] energySum = new IHistogram1D[2];
	private IHistogram1D[] invariantMass = new IHistogram1D[2];
	private IHistogram2D[] energySum2D = new IHistogram2D[2];
	private IHistogram2D[] position = new IHistogram2D[2];
	
	@Override
	public void startOfData() {
		// Instantiate the "any cluster status" plots.
		tracks[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Tracks in Event (All)", 20, 0, 20);
		posTracks[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Tracks in Event (Positive)", 20, 0, 20);
		negTracks[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Tracks in Event (Negative)", 20, 0, 20);
		posMomentum[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Momentum (Positive)", 110, 0, 1.1);
		negMomentum[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Momentum (Negative)", 110, 0, 1.1);
		energySum[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Energy Sum", 220, 0, 2.2);
		invariantMass[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Invariant Mass", 240, 0.000, 0.120);
		energySum2D[ANY_CLUSTER] = aida.histogram2D("Trident Analysis/All/2D Energy Sum", 55, 0, 1.1, 55, 0, 1.1);
		position[ANY_CLUSTER] = aida.histogram2D("Trident Analysis/All/Track Cluster Position", 46, -23, 23, 11, -5.5, 5.5);
		
		// Instantiate the "has a cluster" plots.
		tracks[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Tracks in Event (All)", 20, 0, 20);
		posTracks[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Tracks in Event (Positive)", 20, 0, 20);
		negTracks[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Tracks in Event (Negative)", 20, 0, 20);
		posMomentum[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Momentum (Positive)", 110, 0, 1.1);
		negMomentum[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Momentum (Negative)", 110, 0, 1.1);
		energySum[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Energy Sum", 220, 0, 2.2);
		invariantMass[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Invariant Mass", 240, 0.000, 0.120);
		energySum2D[HAS_CLUSTER] = aida.histogram2D("Trident Analysis/Cluster/2D Energy Sum", 55, 0, 1.1, 55, 0, 1.1);
		position[HAS_CLUSTER] = aida.histogram2D("Trident Analysis/Cluster/Track Cluster Position", 46, -23, 23, 11, -5.5, 5.5);
	}
	
	@Override
	public void endOfData() {
		System.out.printf("Tracks (Candidate)           :: %d%n", tracksCandidate);
		System.out.printf("Tracks (Final State)         :: %d%n", tracksFinalState);
		System.out.printf("Cluster Tracks (Candidate)   :: %d%n", tracksCandidateCluster);
		System.out.printf("Cluster Tracks (Final State) :: %d%n", tracksFinalStateCluster);
	}
	
	@Override
	public void process(EventHeader event) {
		// Check for final state particles.
		if(event.hasCollection(ReconstructedParticle.class, finalStateCollectionName)) {
			// Get the final state particles.
			List<ReconstructedParticle> trackList = event.get(ReconstructedParticle.class, finalStateCollectionName);
			
			// Get the number of tracks.
			tracksFinalState += trackList.size();
			
			// Store the positive and negative tracks.
			List<ReconstructedParticle> posTrackList = new ArrayList<ReconstructedParticle>();
			List<ReconstructedParticle> negTrackList = new ArrayList<ReconstructedParticle>();
			
			// Store the same tracks, but limited to those with clusters.
			List<ReconstructedParticle> allClusterTrackList = new ArrayList<ReconstructedParticle>();
			List<ReconstructedParticle> posClusterTrackList = new ArrayList<ReconstructedParticle>();
			List<ReconstructedParticle> negClusterTrackList = new ArrayList<ReconstructedParticle>();
			
			// Iterate over the tracks and populate the lists.
			for(ReconstructedParticle track : trackList) {
				if(track.getCharge() > 0) {
					// Increment the counters and populate the momentum plots.
					posTrackList.add(track);
					posMomentum[ANY_CLUSTER].fill(track.getMomentum().magnitude());
					
					// Repeat for the "has clusters" plots if necessary.
					if(track.getClusters().size() > 0) {
						// Increment the counters and populate the
						// momentum plot.
						posClusterTrackList.add(track);
						allClusterTrackList.add(track);
						posMomentum[HAS_CLUSTER].fill(track.getMomentum().magnitude());
						
						// Populate the cluster position plot.
						int ix = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
						int iy = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
						position[HAS_CLUSTER].fill(ix, iy);
					}
				} else if(track.getCharge() < 0) {
					// Increment the counters and populate the momentum plots.
					negTrackList.add(track);
					negMomentum[ANY_CLUSTER].fill(track.getMomentum().magnitude());
					
					// Repeat for the "has clusters" plots if necessary.
					if(track.getClusters().size() > 0) {
						// Increment the counters and populate the
						// momentum plot.
						negClusterTrackList.add(track);
						allClusterTrackList.add(track);
						negMomentum[HAS_CLUSTER].fill(track.getMomentum().magnitude());
						
						// Populate the cluster position plot.
						int ix = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
						int iy = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
						position[HAS_CLUSTER].fill(ix, iy);
					}
				} else {
					if(track.getClusters().size() > 0) {
						// Increment the counter.
						allClusterTrackList.add(track);
						
						// Populate the cluster position plot.
						int ix = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
						int iy = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
						position[HAS_CLUSTER].fill(ix, iy);
					}
				}
				
				// Track the number of cluster tracks.
				if(!track.getClusters().isEmpty()) {
					tracksFinalStateCluster++;
				}
			}
			
			// Populate the tracks per event plots.
			tracks[ANY_CLUSTER].fill(trackList.size());
			tracks[HAS_CLUSTER].fill(allClusterTrackList.size());
			posTracks[ANY_CLUSTER].fill(posTrackList.size());
			posTracks[HAS_CLUSTER].fill(posClusterTrackList.size());
			negTracks[ANY_CLUSTER].fill(negTrackList.size());
			negTracks[HAS_CLUSTER].fill(negClusterTrackList.size());
			
			/// Store track pairs.
			List<ReconstructedParticle[]> pairList = new ArrayList<ReconstructedParticle[]>();
			List<ReconstructedParticle[]> pairClusterList = new ArrayList<ReconstructedParticle[]>();
			
			// Form track pairs for all tracks.
			for(ReconstructedParticle posTrack : posTrackList) {
				for(ReconstructedParticle negTrack : negTrackList) {
					pairList.add(new ReconstructedParticle[] { posTrack, negTrack });
				}
			}
			
			// Form track pairs for cluster tracks.
			for(ReconstructedParticle posTrack : posClusterTrackList) {
				for(ReconstructedParticle negTrack : negClusterTrackList) {
					pairClusterList.add(new ReconstructedParticle[] { posTrack, negTrack });
				}
			}
			
			// Populate the track pair plots.
			for(ReconstructedParticle[] pair : pairList) {
				energySum[ANY_CLUSTER].fill(pair[0].getEnergy() + pair[1].getEnergy());
				energySum2D[ANY_CLUSTER].fill(pair[0].getEnergy(), pair[1].getEnergy());
			}
			
			// Populate the cluster track pair plots.
			for(ReconstructedParticle[] pair : pairClusterList) {
				energySum[HAS_CLUSTER].fill(pair[0].getEnergy() + pair[1].getEnergy());
				energySum2D[HAS_CLUSTER].fill(pair[0].getEnergy(), pair[1].getEnergy());
			}
		}
		
		// Check for V0 candidates.
		if(event.hasCollection(ReconstructedParticle.class, candidateCollectionName)) {
			// Get the candidate particles.
			List<ReconstructedParticle> trackList = event.get(ReconstructedParticle.class, candidateCollectionName);
			
			// Increment the counter.
			tracksCandidate += trackList.size();
			
			// Increment the counter for cluster tracks.
			for(ReconstructedParticle track : trackList) {
				// Populate the invariant mass plot.
				invariantMass[ANY_CLUSTER].fill(track.getMass());
				
				// Check for a cluster track.
				if(track.getClusters().size() > 0) {
					tracksCandidateCluster++;
					invariantMass[HAS_CLUSTER].fill(track.getMass());
				}
			}
		}
	}
}
